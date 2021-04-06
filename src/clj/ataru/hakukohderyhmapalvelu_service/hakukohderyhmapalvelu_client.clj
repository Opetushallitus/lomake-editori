(ns ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-client
  (:require [schema.core :as s]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]
            [taoensso.timbre :as log]))

(defn- get-result
  [url cas-client]
  (log/debug "get-result" url)
  (let [{:keys [status body]} (cas-client/cas-authenticated-get
                                cas-client
                                url)]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

(s/defn ^:always-validate get-hakukohderyhma-oids-for-hakukohde-oid :- (s/maybe [s/Str])
  [hakukohde-oid :- s/Str
   cas-client]
  (some-> :hakukohderyhmapalvelu.hakukohderyhmas-for-hakukohde
          (url-helper/resolve-url hakukohde-oid)
          (get-result cas-client)))
