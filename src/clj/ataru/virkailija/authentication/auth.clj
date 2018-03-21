(ns ataru.virkailija.authentication.auth
  (:require [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.ldap-client :as ldap]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.organization-service.user-rights :as rights]
            [ataru.virkailija.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas]
            [environ.core :refer [env]]
            [medley.core :refer [map-kv]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :refer [info spy error]]
            [yesql.core :as sql])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(defn- redirect-to-logged-out-page []
  (resp/redirect (resolve-url :cas.login)))

(sql/defqueries "sql/virkailija-queries.sql")

(defn cas-login [ticket]
  (fn []
      (when ticket
        (let [cas-client (cas/cas-client (resolve-url :cas-client))
              username   (.run (.validateServiceTicket cas-client (resolve-url :ataru.login-success) ticket))]
          [username ticket]))))

(defn login [login-provider organization-service redirect-url]
  (try
    (if-let [[username ticket] (login-provider)]
      (do
        (cas-store/login ticket)
        (let [virkailija               (ldap/get-virkailija-by-username username)
              user-right-organizations (map-kv (fn [right org-oids]
                                                 [right (organization-service/get-organizations-for-oids organization-service org-oids)])
                                               (ldap/user->right-organization-oids virkailija rights/right-names))]
          (info "username" username "logged in, redirect to" redirect-url)
          (db/exec :db yesql-upsert-virkailija<! {:oid        (:employeeNumber virkailija)
                                                  :first_name (:givenName virkailija)
                                                  :last_name  (:sn virkailija)})
          (audit-log/log {:new       ticket
                          :id        username
                          :operation audit-log/operation-login})
          (-> (resp/redirect redirect-url)
              (assoc :session {:identity {:username                 username
                                          :ticket                   ticket
                                          :user-right-organizations user-right-organizations
                                          :oid                      (:employeeNumber virkailija)}}))))
      (redirect-to-logged-out-page))
    (catch Exception e
      (error e "Error in login ticket handling")
      (redirect-to-logged-out-page))))

(defn logout [session]
  (info "username" (-> session :identity :username) "logged out")
  (cas-store/logout (-> session :identity :ticket))
  (-> (resp/redirect (resolve-url :cas.logout))
      (assoc :session {:identity nil})))

(defn cas-initiated-logout [logout-request]
  (info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (error "Could not parse ticket from CAS request" logout-request)
      (cas-store/logout (.get ticket)))
    (ok)))

(defn logged-in? [request]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket)))
