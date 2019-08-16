(ns ataru.applications.application-access-control
  (:require
   [ataru.hakija.hakija-form-service :as hakija-form-service]
   [ataru.log.audit-log :as audit-log]
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.odw.odw-service :as odw-service]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
   [ataru.util :as util]))

(defn authorized-by-form?
  [authorized-organization-oids application]
  (boolean (authorized-organization-oids (:organization-oid application))))

(defn authorized-by-tarjoajat?
  [authorized-organization-oids application]
  (let [tarjoajat       (->> (:hakukohde application)
                             (mapcat :tarjoaja-oids)
                             set)
        hakukohderyhmat (->> (:hakukohde application)
                             (mapcat :ryhmaliitokset)
                             set)]
    (boolean (some authorized-organization-oids
                   (concat tarjoajat hakukohderyhmat)))))

(defn- populate-applications-hakukohteet
  [tarjonta-service applications]
  (let [hakukohteet (->> applications
                         (mapcat :hakukohde)
                         distinct
                         (tarjonta-service/get-hakukohteet tarjonta-service)
                         (reduce #(assoc %1 (:oid %2) %2) {}))]
    (map #(update % :hakukohde (partial mapv (fn [oid] (get hakukohteet oid {:oid oid}))))
         applications)))

(defn- depopulate-application-hakukohteet
  [application]
  (update application :hakukohde (partial mapv :oid)))

(defn- remove-organization-oid [application]
  (dissoc application :organization-oid))

(defn filter-authorized
  [tarjonta-service predicate applications]
  (->> applications
       (populate-applications-hakukohteet tarjonta-service)
       (filter predicate)
       (map depopulate-application-hakukohteet)
       (map remove-organization-oid)))

(defn applications-access-authorized?
  [organization-service tarjonta-service session application-keys rights]
  (session-orgs/run-org-authorized
   session
   organization-service
   rights
   (constantly false)
   #(->> (application-store/applications-authorization-data application-keys)
         (populate-applications-hakukohteet tarjonta-service)
         (every? (some-fn (partial authorized-by-form? %)
                          (partial authorized-by-tarjoajat? %))))
   (constantly true)))

(defn- can-edit-application?
  [organization-service session application]
  (assoc application
         :can-edit?
         (session-orgs/run-org-authorized
           session
           organization-service
           [:edit-applications]
           (constantly false)
           (fn [orgs]
             (or (authorized-by-form? orgs application)
                 (authorized-by-tarjoajat? orgs application)))
           (constantly true))))

(defn get-latest-application-by-key
  [organization-service tarjonta-service session application-key]
  (let [application (session-orgs/run-org-authorized
                     session
                     organization-service
                     [:view-applications :edit-applications]
                     (constantly nil)
                     #(some->> (application-store/get-latest-application-by-key application-key)
                               vector
                               (populate-applications-hakukohteet tarjonta-service)
                               (filter (some-fn (partial authorized-by-form? %)
                                                (partial authorized-by-tarjoajat? %)))
                               (map (partial can-edit-application? organization-service session))
                               (map depopulate-application-hakukohteet)
                               (map remove-organization-oid)
                               first)
                     #(remove-organization-oid
                       (can-edit-application? organization-service
                                              session
                                              (application-store/get-latest-application-by-key application-key))))]
    (when (some? application)
      (audit-log/log {:new       (dissoc application :answers)
                      :id        {:applicationOid application-key}
                      :session   session
                      :operation audit-log/operation-read}))
    application))

(defn- populate-hakukohde
  [external-application]
  (assoc external-application
         :hakukohde (map :hakukohdeOid (:hakutoiveet external-application))))

(defn- remove-hakukohde
  [external-application]
  (dissoc external-application :hakukohde))

(defn external-applications
  [organization-service tarjonta-service session haku-oid hakukohde-oid hakemus-oids]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    #(->> (application-store/get-external-applications haku-oid
                                                       hakukohde-oid
                                                       hakemus-oids)
          (map populate-hakukohde)
          (filter-authorized tarjonta-service
                             (some-fn (partial authorized-by-form? %)
                                      (partial authorized-by-tarjoajat? %)))
          (map remove-hakukohde))
    #(map remove-organization-oid (application-store/get-external-applications
                                   haku-oid
                                   hakukohde-oid
                                   hakemus-oids))))

(defn hakurekisteri-applications [organization-service session haku-oid hakukohde-oids person-oids modified-after]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-hakurekisteri-applications
       haku-oid
       hakukohde-oids
       person-oids
       modified-after)))

(defn application-key-to-person-oid [organization-service session haku-oid hakukohde-oids]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-person-and-application-oids
     haku-oid
     hakukohde-oids)))

(defn omatsivut-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
     person-oid)))

(defn onr-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/onr-applications person-oid)))

(defn get-applications-for-odw [organization-service session person-service tarjonta-service from-date limit offset]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(odw-service/get-applications-for-odw person-service tarjonta-service from-date limit offset)))

(defn get-applications-for-tilastokeskus [organization-service session haku-oid hakukohde-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-application-info-for-tilastokeskus haku-oid hakukohde-oid)))

(defn get-applications-for-valintalaskenta [organization-service session hakukohde-oid application-keys]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-applications-for-valintalaskenta hakukohde-oid application-keys)))

(defn siirto-applications
  [tarjonta-service organization-service session hakukohde-oid application-keys]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(->> (application-store/siirto-applications hakukohde-oid application-keys)
         (map (fn [a] (assoc a :hakukohde (:hakutoiveet a))))
         (filter-authorized tarjonta-service
                            (some-fn (partial authorized-by-form? %)
                                     (partial authorized-by-tarjoajat? %)))
         (map (fn [a] (dissoc a :hakukohde)))
         (map remove-organization-oid))
   #(->> (application-store/siirto-applications hakukohde-oid application-keys)
         (map remove-organization-oid))))

(defn valinta-ui-applications
  [organization-service tarjonta-service session query]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(filter-authorized tarjonta-service
                       (partial authorized-by-tarjoajat? %)
                       (application-store/valinta-ui-applications query))
   #(filter-authorized tarjonta-service
                       (constantly true)
                       (application-store/valinta-ui-applications query))))

(defn viewable-attachment-keys-of-application
  [form-by-haku-oid-and-id-cache
   koodisto-cache
   form-roles
   application]
  (let [form      (cond (some? (:haku application))
                        (hakija-form-service/fetch-form-by-haku-oid-and-id-cached
                         form-by-haku-oid-and-id-cache
                         (:haku application)
                         (:form application)
                         false
                         form-roles)
                        (some? (:form application))
                        (hakija-form-service/fetch-form-by-id
                         (:form application)
                         form-roles
                         koodisto-cache
                         nil
                         false))
        can-view? (->> (:content form)
                       util/flatten-form-fields
                       (filter #(= "attachment" (:fieldType %)))
                       (remove :cannot-view)
                       (map :id)
                       set)]
    (reduce (fn [viewable-attachment-keys answer]
              (cond-> viewable-attachment-keys
                      (can-view? (:key answer))
                      (into (flatten (:value answer)))))
            #{}
            (:answers application))))

(defn viewable-attachment-keys
  [organization-service
   tarjonta-service
   form-by-haku-oid-and-id-cache
   koodisto-cache
   session
   application-key]
  (if-let [application (get-latest-application-by-key
                        organization-service
                        tarjonta-service
                        session
                        application-key)]
    (viewable-attachment-keys-of-application
     form-by-haku-oid-and-id-cache
     koodisto-cache
     [:virkailija]
     application)
    #{}))
