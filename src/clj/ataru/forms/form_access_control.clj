(ns ataru.forms.form-access-control
  (:require
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.virkailija.user.organization-client :refer [oph-organization]]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [taoensso.timbre :refer [warn]]))

(defn form-allowed-by-key? [form-key session organization-service right]
  (session-orgs/organization-allowed?
   session
   organization-service
   (fn [] (form-store/get-organization-oid-by-key form-key))
   [right]))

(defn form-allowed-by-id?
  "id identifies a version of the form"
  [form-id session organization-service right]
  (session-orgs/organization-allowed?
   session
   organization-service
   (fn [] (form-store/get-organization-oid-by-id form-id))
   [right]))

(defn get-organizations-with-edit-rights [session]
  (-> session
      :identity
      :user-right-organizations
      :form-edit))

(defn- check-edit-authorization [form session organization-service do-fn]
  (let [user-name     (-> session :identity :username)
        organizations (get-organizations-with-edit-rights session)]
    (cond
      (and
       (:id form) ; Updating, since form already has id
       (not (form-allowed-by-id? (:id form) session organization-service :form-edit)))
      (throw (user-feedback-exception
               (str "Ei oikeutta lomakkeeseen "
                    (:id form)
                    " organisaatioilla "
                    (vec organizations))))

      (not (:organization-oid form))
      (throw (user-feedback-exception (str "Lomaketta ei ole kytketty organisaatioon"
                                           (when-not (empty? organizations)
                                             (str " " (vec organizations))))))

      ;The potentially new organization for form is not allowed for user
      (not (session-orgs/organization-allowed?
            session
            organization-service
            (:organization-oid form)
            [:form-edit]))
      (throw (user-feedback-exception
              (str "Ei oikeutta organisaatioon "
                   (:organization-oid form)
                   " käyttäjän organisaatioilla "
                   (vec organizations))))

      :else
      (do-fn))))

(defn post-form [form session organization-service]
  (let [organization-oids (map :oid (get-organizations-with-edit-rights session))
        first-org-oid     (first organization-oids)
        form-with-org     (assoc form :organization-oid (or (:organization-oid form) first-org-oid))]
    (check-edit-authorization
     form-with-org
     session
     organization-service
     (fn []
       (form-store/create-form-or-increment-version!
        (assoc
         form-with-org
         :created-by (-> session :identity :username)))))))

(defn delete-form [form-id session organization-service]
  (let [form (form-store/fetch-latest-version form-id)]
    (check-edit-authorization form session organization-service
      (fn []
        (form-store/create-form-or-increment-version!
         (assoc form :deleted true))))))

(defn- application-count->form [{:keys [key] :as form}]
  (assoc form :application-count (application-store/get-application-count-with-deleteds-by-form-key key)))

(defn- deleted-with-applications? [{:keys [application-count deleted]}]
  (or (not deleted)
      (> application-count 0)))

(defn get-forms-for-editor [session organization-service]
  {:forms (->> (session-orgs/run-org-authorized
                session
                organization-service
                [:form-edit]
                vector
                #(form-store/get-forms false %)
                #(form-store/get-all-forms false)))})

(defn get-forms-for-application-listing [session organization-service]
  {:forms (->> (session-orgs/run-org-authorized
                session
                organization-service
                [:view-applications]
                vector
                #(form-store/get-forms true %)
                #(form-store/get-all-forms true))
               (map #(application-count->form %))
               (filter deleted-with-applications?))})
