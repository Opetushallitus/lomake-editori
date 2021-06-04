(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login cas-login cas-initiated-logout]]
            [ataru.config.url-helper :refer [resolve-url]]
            [clj-ring-db-session.session.session-client :as session-client]
            [ataru.config.core :refer [config]]
            [clj-ring-db-session.authentication.login :as crdsa-login]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [clojure.string :as string]))

(defn- rewrite-url-for-environment
  "Ensure that https is used when available (due to https termination on
   non-development environments)"
  [url-from-session]
  (if (:dev? env)
    url-from-session
    (string/replace url-from-session #"^http://" "https://")))

(defn- fake-login-provider [ticket]
  (fn []
      (let [username      (if (= ticket "USER-WITH-HAKUKOHDE-ORGANIZATION")
                            "1.2.246.562.11.22222222222"
                            "1.2.246.562.11.11111111111")
            unique-ticket (str (System/currentTimeMillis) "-" (rand-int (Integer/MAX_VALUE)))]
        [username unique-ticket])))

(defn auth-routes [{:keys [login-cas-client
                           kayttooikeus-service
                           person-service
                           organization-service
                           audit-logger
                           session-store
                           cas-logout]}]
  (api/context "/auth" []
    (api/middleware [session-client/wrap-session-client-headers]
                    (api/undocumented
                      (api/GET "/cas" [ticket :as request]
                        (let [redirect-url   (if-let [url-from-session (get-in request [:session :original-url])]
                                               (rewrite-url-for-environment url-from-session)
                                               (get-in config [:public-config :virkailija :service_url]))
                              login-provider (if (-> config :dev :fake-dependencies)
                                               (fake-login-provider ticket)
                                               (cas-login login-cas-client ticket))]
                          (login login-provider
                                 kayttooikeus-service
                                 person-service
                                 organization-service
                                 audit-logger
                                 redirect-url
                                 (:session request))))
                      (api/POST "/cas" [logoutRequest]
                        (cas-initiated-logout logoutRequest session-store cas-logout))
                      (api/GET "/logout" {session :session}
                        (crdsa-login/logout session (resolve-url :cas.logout)))))))
