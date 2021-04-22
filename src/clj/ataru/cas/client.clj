(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [clj-okhttp.core :as ok-http]
            [java-time :as t])
  (:import [fi.vm.sade.javautils.cas CasHttpClient]))

(def opts {:read-timeout 1000})

(defrecord CasClientState [client session-cookie-name session-id])

(defn new-cas-client [service security-uri-suffix session-cookie-name caller-id]
  (new CasHttpClient
       (ok-http/create-client opts)
       caller-id
       session-cookie-name
       service
       security-uri-suffix
       (resolve-url :cas-client)
       (get-in config [:cas :username])
       (get-in config [:cas :password])
       (t/duration 30 :minutes)))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [cas-client (new-cas-client service security-uri-suffix session-cookie-name caller-id)]
    (map->CasClientState {:client              cas-client
                          :session-cookie-name session-cookie-name
                          :session-id          (atom nil)})))

(defn- request-with-json-body [request body]
  (-> request
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :body (json/generate-string body))))

(defn- create-params [session-cookie-name cas-session-id body]
  (cond-> {:cookies           {session-cookie-name {:value @cas-session-id :path "/"}}
           :redirect-strategy :none
           :throw-exceptions  false}
          (some? body) (request-with-json-body body)))

(defn- cas-http [client method url opts-fn & [body]]
  (let [cas-client (:client client)
        session-cookie-name (:session-cookie-name client)
        cas-session-id (:session-id client)
        ;TODO request!!!
        request (ok-http/request client nil)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.call cas-client request))))
    (let [resp (http-util/do-request (merge {:url url :method method}
                                            (opts-fn)
                                            (create-params session-cookie-name cas-session-id body)))]
      (if (or (= 401 (:status resp))
              (= 302 (:status resp)))
        (do
          (reset! cas-session-id (.run (.call cas-client request)))
          (http-util/do-request (merge {:url url :method method}
                                       (opts-fn)
                                       (create-params session-cookie-name cas-session-id body))))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url (constantly {})))

(defn cas-authenticated-delete [client url]
  (cas-http client :delete url (constantly {})))

(defn cas-authenticated-post [client url body]
  (cas-http client :post url (constantly {}) body))

(defn cas-authenticated-multipart-post [client url opts-fn]
  (cas-http client :post url opts-fn nil))

(defn cas-authenticated-get-as-stream [client url]
  (cas-http client :get url (constantly {:as :stream}) nil))