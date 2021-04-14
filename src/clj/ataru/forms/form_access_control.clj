(ns ataru.forms.form-access-control
  (:require
   [ataru.forms.form-store :as form-store]
   [ataru.schema.form-schema :as form-schema]
   [clojure.walk :refer [prewalk]]
   [ataru.virkailija.editor.form-diff :as form-diff]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]))

(defn- form-allowed-by-id?
  [authorized-organization-oids form-id]
  (contains? authorized-organization-oids
             (form-store/get-organization-oid-by-id form-id)))

(defn- authorized-by-tarjoaja?
  [authorized-organization-oids hakukohde]
  (let [tarjoajat       (set (:tarjoaja-oids hakukohde))
        hakukohderyhmat (set (:ryhmaliitokset hakukohde))]
    (boolean (some authorized-organization-oids
                   (concat tarjoajat hakukohderyhmat)))))

(defn- form-allowed-by-haku?
  [tarjonta-service authorized-organization-oids form-key]
  (->> form-key
       (tarjonta-protocol/hakus-by-form-key tarjonta-service)
       (mapcat :hakukohteet)
       (tarjonta-protocol/get-hakukohteet tarjonta-service)
       (some #(authorized-by-tarjoaja? authorized-organization-oids %))
       boolean))

(defn get-organizations-with-edit-rights
  [session]
  (if (:selected-organization session)
    (if (or (-> session :identity :superuser)
            (contains? (set (-> session :selected-organization :rights)) "form-edit"))
      [(:selected-organization session)]
      [])
    (-> session
        :identity
        :user-right-organizations
        :form-edit)))

(defn- check-lock-authorization [{:keys [key]} session tarjonta-service]
  (when-not (-> session :identity :superuser)
    (let [haut (tarjonta-protocol/hakus-by-form-key tarjonta-service key)]
      (when (some :yhteishaku haut)
        (throw (user-feedback-exception
                (format "Lukitseminen ja avaaminen yhteishaussa vain rekisterinpitäjän oikeuksilla")))))))

(defn- check-edit-authorization [form session tarjonta-service organization-service do-fn]
  (when (not (:organization-oid form))
    (throw
     (user-feedback-exception "Lomaketta ei ole kytketty organisaatioon")))
  (session-orgs/run-org-authorized
   session
   organization-service
   [:form-edit]
   (fn []
     (throw
      (user-feedback-exception "Käyttäjällä ei lomakkeen muokkausoikeutta")))
   (fn [org-oids]
     (if-let [form-id (:id form)]
       (when (not (or (form-allowed-by-id? org-oids form-id)
                      (form-allowed-by-haku? tarjonta-service org-oids (:key form))))
         (throw
          (user-feedback-exception
           (str "Käyttäjällä ei oikeutta muokata lomaketta "
                (:key form)))))
       (when (not (contains? org-oids (:organization-oid form)))
         (throw
          (user-feedback-exception
           (str "Käyttäjällä ei oikeutta luoda lomaketta organisaatiolle "
                (:organization-oid form))))))
     (do-fn))
   (fn []
     (do-fn))))

(defn- check-form-field-id-duplicates
  [form]
  (let [form-element-ids (atom [])]
    (prewalk
      (fn [x]
        (when (and (map-entry? x)
                   (= :id (key x)))
          (swap! form-element-ids conj (val x)))
        x)
      (:content form))
    (when (and
            (not-empty @form-element-ids)
            (not (apply distinct? @form-element-ids)))
      (throw (Exception. (str "Duplicate element id in form (" (:key form) "): " (pr-str (keep #(when (< 1 (second %)) (first %))
                                                                             (frequencies @form-element-ids)))))))))

(defn post-form [form session tarjonta-service organization-service audit-logger]
  (let [organization-oids (map :oid (get-organizations-with-edit-rights session))
        first-org-oid     (first organization-oids)
        form-with-org     (assoc form :organization-oid (or (:organization-oid form) first-org-oid))]
    (check-form-field-id-duplicates form)
    (check-edit-authorization
     form-with-org
     session
     tarjonta-service
     organization-service
     (fn []
       (form-store/create-form-or-increment-version!
        (assoc
         form-with-org
         :created-by (-> session :identity :username))
        session
        audit-logger)))))

(defn edit-form-with-operations
  [id operations session tarjonta-service organization-service audit-logger]
  (let [latest-version (form-store/fetch-form id)]
    (if (:locked latest-version)
      (throw (user-feedback-exception "Lomakkeen muokkaus on estetty."))
      (let [coerced-form (form-schema/form-coercer latest-version)
            updated-form (form-diff/apply-operations coerced-form operations)]
        (post-form updated-form session tarjonta-service organization-service audit-logger)))))

(defn delete-form [form-id session tarjonta-service organization-service audit-logger]
  (let [form (form-store/fetch-latest-version form-id)]
    (if (> (:application-count form) 0)
      (throw (user-feedback-exception "Lomakkeen poisto estetty. Lomakkeella on hakemuksia."))
      (check-edit-authorization
        form
        session
        tarjonta-service
        organization-service
        (fn []
          (form-store/create-form-or-increment-version!
            (assoc form :deleted true)
            session
            audit-logger))))))

(defn- get-forms-as-ordinary-user
  [tarjonta-service authorized-organization-oids hakukohderyhma-oid]
  (filter (fn [form]
            (or (contains? authorized-organization-oids (:organization-oid form))
                (form-allowed-by-haku? tarjonta-service authorized-organization-oids (:key form))))
          (form-store/get-all-forms hakukohderyhma-oid)))

(defn get-forms-for-editor [session tarjonta-service organization-service hakukohderyhma-oid]
  {:forms (session-orgs/run-org-authorized
           session
           organization-service
           [:form-edit]
           (fn [] [])
           (fn [org-oids]
             (map #(dissoc % :organization-oid)
                  (get-forms-as-ordinary-user tarjonta-service org-oids hakukohderyhma-oid)))
           (fn []
             (map #(dissoc % :organization-oid)
                  (form-store/get-all-forms hakukohderyhma-oid))))})

(defn update-form-lock [form-id operation session tarjonta-service organization-service audit-logger]
  (let [latest-version  (form-store/fetch-form form-id)
        previous-locked (:locked latest-version)
        lock?           (= "close" operation)
        updated-form    (merge latest-version
                               (if lock?
                                 {:locked "now()" :locked-by (-> session :identity :oid)}
                                 {:locked nil :locked-by nil}))]
    (check-lock-authorization latest-version session tarjonta-service)
    (if (or (and lock? (some? previous-locked))
            (and (not lock?) (nil? previous-locked)))
      (throw (user-feedback-exception "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen."))
      (select-keys (post-form updated-form session tarjonta-service organization-service audit-logger)
                   [:locked :id]))))
