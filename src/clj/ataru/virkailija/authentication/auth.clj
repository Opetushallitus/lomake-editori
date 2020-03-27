(ns ataru.virkailija.authentication.auth
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.db.db :as db]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.user-rights :as rights]
            [ataru.person-service.person-service :as person-service]
            [ataru.virkailija.authentication.cas-ticketstore :as cas-store]
            [medley.core :refer [map-kv]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :as log]
            [yesql.core :as sql])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(defn- redirect-to-login-failed-page []
  (resp/redirect (resolve-url :cas.failure)))

(sql/defqueries "sql/virkailija-queries.sql")

(defn cas-login [cas-client ticket]
  (fn []
    (when ticket
      [(.run (.validateServiceTicket cas-client (resolve-url :ataru.login-success) ticket))
       ticket])))

(defn- user-right-organizations->organization-rights
  "Takes map keyed by right with list of organizations as values, outputs map keyed by organization oid with list of rights as values"
  [user-right-organizations]
  (reduce
    (fn [acc [right organizations]]
      (reduce
        (fn [acc organization]
          (let [existing-rights (get-in acc [(:oid organization) :rights] #{})]
            (assoc acc (:oid organization) (assoc organization :rights (conj existing-rights right)))))
        acc
        organizations))
    {}
    user-right-organizations))

(defn login [login-provider
             kayttooikeus-service
             person-service
             organization-service
             audit-logger
             redirect-url
             session]
  (try
    (if-let [[username ticket] (login-provider)]
      (do
        (cas-store/login ticket)
        (let [virkailija                (kayttooikeus-service/virkailija-by-username kayttooikeus-service username)
              henkilo                   (person-service/get-person person-service (:oidHenkilo virkailija))
              right-organization-oids   (rights/virkailija->right-organization-oids virkailija rights/right-names)
              organization-oids         (-> (vals right-organization-oids) (flatten) (set))
              oph-organization-member?  (contains? organization-oids organization-client/oph-organization)
              user-right-organizations  (map-kv
                                         (fn [right org-oids]
                                           [right (organization-service/get-organizations-for-oids organization-service org-oids)])
                                         right-organization-oids)
              organizations-with-rights (->> user-right-organizations
                                             (map-kv (fn [right organizations]
                                                       [right (organization-service/get-all-organizations organization-service organizations)]))
                                             (user-right-organizations->organization-rights))]
          (log/info "user" username "logged in")
          (db/exec :db yesql-upsert-virkailija<! {:oid        (:oidHenkilo henkilo)
                                                  :first_name (:kutsumanimi henkilo)
                                                  :last_name  (:sukunimi henkilo)})
          (audit-log/log audit-logger
                         {:new       {:ticket ticket}
                          :id        {:henkiloOid (:oidHenkilo henkilo)}
                          :session   session
                          :operation audit-log/operation-login})
          (-> (resp/redirect redirect-url)
              (assoc :session {:identity {:username                 username
                                          :first-name               (:kutsumanimi henkilo)
                                          :last-name                (:sukunimi henkilo)
                                          :oid                      (:oidHenkilo henkilo)
                                          :lang                     (or (some #{(-> henkilo :asiointiKieli :kieliKoodi)}
                                                                              ["fi" "sv" "en"])
                                                                        "fi")
                                          :ticket                   ticket
                                          :user-right-organizations user-right-organizations
                                          :superuser                oph-organization-member?
                                          :organizations            organizations-with-rights}}))))
      (redirect-to-login-failed-page))
    (catch Exception e
      (log/error e "Error in login ticket handling")
      (redirect-to-login-failed-page))))

(defn logout [session]
  (log/info "username" (-> session :identity :username) "logged out")
  (cas-store/logout (-> session :identity :ticket))
  (-> (resp/redirect (resolve-url :cas.logout))
      (assoc :session {:identity nil})))

(defn cas-initiated-logout [logout-request]
  (log/info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (log/info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (log/error "Could not parse ticket from CAS request" logout-request)
      (cas-store/logout (.get ticket)))
    (ok)))

(defn logged-in? [request]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket)))
