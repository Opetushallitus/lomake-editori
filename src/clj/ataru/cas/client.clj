(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.core.match :refer [match]]
            [clojure.core :as c]
            [clojure.string :as s])
  (:import [fi.vm.sade.javautils.nio.cas CasClient CasConfig CasLogout]
           [org.asynchttpclient RequestBuilder]
           (org.asynchttpclient.request.body.multipart InputStreamPart)))

(defrecord CasClientState [client])

;(comment defn create-cas-config [service security-uri-suffix session-cookie-name caller-id request-timeout]
;  (let [
;        username (get-in config [:cas :username])
;        password (get-in config [:cas :password])
;        cas-url (resolve-url :cas-client)
;        service-url (c/str (resolve-url :url-virkailija) service)
;        csrf (s/replace service "/" "")]
;    (if (nil? request-timeout)
;      (do
;        (log/info "Cas client created for service: " service-url)
;        (match session-cookie-name
;               "JSESSIONID" (CasConfig/SpringSessionCasConfig
;                              username
;                              password
;                              cas-url
;                              service-url
;                              csrf
;                              caller-id)
;               "ring-session" (CasConfig/RingSessionCasConfig
;                                username
;                                password
;                                cas-url
;                                service-url
;                                csrf
;                                caller-id)
;               :else (CasConfig/CasConfig
;                       username
;                       password
;                       cas-url
;                       service-url
;                       csrf
;                       caller-id
;                       session-cookie-name
;                       security-uri-suffix)))
;      (do
;        (log/info "Cas client created for service: " service-url "timeout: " request-timeout)
;        (new CasConfig
;           username
;           password
;           cas-url
;           service-url
;           csrf
;           caller-id
;           session-cookie-name
;           security-uri-suffix
;           nil
;           request-timeout))
;      )))

(defn create-cas-config [service security-uri-suffix session-cookie-name caller-id]
  (let [
        username (get-in config [:cas :username])
        password (get-in config [:cas :password])
        cas-url (resolve-url :cas-client)
        service-url (c/str (resolve-url :url-virkailija) service)
        csrf (s/replace service "/" "")]
    (match session-cookie-name
           "JSESSIONID" (CasConfig/SpringSessionCasConfig
                          username
                          password
                          cas-url
                          service-url
                          csrf
                          caller-id)
           "ring-session" (CasConfig/RingSessionCasConfig
                            username
                            password
                            cas-url
                            service-url
                            csrf
                            caller-id)
           :else (CasConfig/CasConfig
                   username
                   password
                   cas-url
                   service-url
                   csrf
                   caller-id
                   session-cookie-name
                   security-uri-suffix))))

(defn cas-logout []
  (new CasLogout))

(defn new-cas-client [service security-uri-suffix session-cookie-name caller-id]
  (new CasClient
       (create-cas-config service security-uri-suffix session-cookie-name caller-id)))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [cas-client (new-cas-client service security-uri-suffix session-cookie-name caller-id) ]
    (map->CasClientState {:client cas-client})))

(defn- cas-http [client method url opts-fn & [body]]
  (let [cas-client (:client client)
        request (match method
                       :get (-> (RequestBuilder.)
                                (.setUrl url)
                                (.setMethod "GET")
                                (.build))
                       :post (-> (RequestBuilder.)
                                 (.setUrl url)
                                 (.setMethod "POST")
                                 (.setHeader "Content-Type" "application/json")
                                 (.setBody (json/generate-string body))
                                 (.build))
                       :post-multipart (let [name (get-in (opts-fn) [:multipart 0 :part-name])
                                             content (get-in (opts-fn) [:multipart 0 :content])
                                             file-name (get-in (opts-fn) [:multipart 0 :name])]
                                         (-> (RequestBuilder.)
                                             (.setUrl url)
                                             (.setRequestTimeout 300000)
                                             (.setReadTimeout 300000)
                                             (.setMethod "POST")
                                             (.setHeader "Content-Type" "multipart/form-data")
                                             (.addBodyPart (new InputStreamPart name content file-name))
                                             (.build)))
                       :delete (-> (RequestBuilder.)
                                   (.setUrl url)
                                   (.setMethod "DELETE")
                                   (.build)))
        ]
    (log/info "REQUEST" request)
    (let [resp (.executeBlocking cas-client request)
          status (.getStatusCode resp)
          body (match method
                      :get-as-stream (.getResponseBodyAsStream resp)
                      :else
                      (.getResponseBody resp))
          response {:status status :body body}]

      (log/info "RESPONSE STATUS: " status)
      response)))

(defn cas-authenticated-get [client url]
  (log/info "cas-authenticated-get")
  (cas-http client :get url (constantly {})))

(defn cas-authenticated-delete [client url]
  (log/info "cas-authenticated-delete")
  (cas-http client :delete url (constantly {})))

(defn cas-authenticated-post [client url body]
  (log/info "cas-authenticated-post")
  (cas-http client :post url (constantly {}) body))

(defn cas-authenticated-multipart-post [client url opts-fn]
  (log/info "cas-authenticated-multipart-post")
  (cas-http client :post-multipart url opts-fn nil))

(defn cas-authenticated-get-as-stream [client url]
  (log/info "cas-authenticated-get-as-stream")
  (cas-http client :get-as-stream url (constantly {:as :stream}) nil))