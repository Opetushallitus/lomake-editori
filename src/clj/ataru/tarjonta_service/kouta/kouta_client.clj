(ns ataru.tarjonta-service.kouta.kouta-client
  (:require [ataru.cache.cache-service :as cache-service]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hakukohderyhmapalvelu-service]
            [ataru.schema.form-schema :as form-schema]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string]
            [schema.core :as s]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def haku-checker (s/checker form-schema/Haku))
(def hakukohde-checker (s/checker form-schema/Hakukohde))
(def toteutus-checker (s/checker form-schema/Koulutus))
(def hakus-by-checker (s/checker [s/Str]))
(def hakukohde-search-checker (s/checker [s/Str]))

(defn- parse-date-time
  [s]

  (let [tz (t/time-zone-for-id "Europe/Helsinki")
        fmt-with-seconds (f/formatter "yyyy-MM-dd'T'HH:mm:ss" tz)
        fmt (f/formatter "yyyy-MM-dd'T'HH:mm" tz)]
    (try
      (t/to-time-zone (f/parse fmt-with-seconds s) tz)
      (catch Exception _
        (t/to-time-zone (f/parse fmt s) tz)))))

(defn- parse-haku
  [haku hakukohteet ohjausparametrit]
  (let [hakuajat (mapv (fn [hakuaika]
                         (merge
                           {:hakuaika-id "kouta-hakuaika-id"
                            :start (parse-date-time (:alkaa hakuaika))}
                           (when-let [paattyy (:paattyy hakuaika)]
                             {:end (parse-date-time paattyy)})))
                       (:hakuajat haku))]
    (merge
     {:can-submit-multiple-applications           (get ohjausparametrit :useitaHakemuksia false)
      :hakuajat                                   hakuajat
      :hakukohteet                                (mapv :oid hakukohteet)
      :hakutapa-uri                               (:hakutapaKoodiUri haku)
      :haun-tiedot-url                            (url-helper/resolve-url :kouta-app.haku (:oid haku))
      :kohdejoukko-uri                            (:kohdejoukkoKoodiUri haku)
      :name                                       (:nimi haku)
      :oid                                        (:oid haku)
      :prioritize-hakukohteet                     (get ohjausparametrit :jarjestetytHakutoiveet false)
      :sijoittelu                                 (get ohjausparametrit :sijoittelu false)
      :yhteishaku                                 (string/starts-with?
                                                   (:hakutapaKoodiUri haku)
                                                   "hakutapa_01#")
      :ylioppilastutkinto-antaa-hakukelpoisuuden? false}
     (when (get ohjausparametrit :hakutoiveidenMaaraRajoitettu false)
       {:max-hakukohteet (get ohjausparametrit :hakutoiveidenEnimmaismaara 1)})
     (when (seq hakuajat)
       {:hakukausi-vuosi (->> hakuajat
                              (map #(t/year (:start %)))
                              (apply max))})
     (when (some? (:hakulomakeAtaruId haku))
       {:ataru-form-key (:hakulomakeAtaruId haku)}))))

(defn- parse-can-be-applied-to?
  [hakukohde]
  (case (:tila hakukohde)
    "tallennettu" false
    "julkaistu"   true
    "arkistoitu"  false
    (throw
     (new RuntimeException
          (str "Unknown hakukohteen tila " (:tila hakukohde)
               " in hakukohde " (:oid hakukohde))))))

(defn- parse-hakukohde
  [hakukohde tarjoajat hakukohderyhmas]
  (merge
   {:oid                                                         (:oid hakukohde)
    :hakukohteen-tiedot-url                                      (url-helper/resolve-url :kouta-app.hakukohde (:oid hakukohde))
    :can-be-applied-to?                                          (parse-can-be-applied-to? hakukohde)
    :haku-oid                                                    (:hakuOid hakukohde)
    :koulutus-oids                                               [(:toteutusOid hakukohde)]
    :name                                                        (:nimi hakukohde)
    :tarjoaja-name                                               (or (:name (first tarjoajat)) {})
    :tarjoaja-oids                                               (mapv :oid tarjoajat)
    :ryhmaliitokset                                              hakukohderyhmas
    :hakukelpoisuusvaatimus-uris                                 (:pohjakoulutusvaatimusKoodiUrit hakukohde)
    :ylioppilastutkinto-antaa-hakukelpoisuuden?                  false
    :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? false}
   (if (:kaytetaanHaunAikataulua hakukohde)
     {:hakuaika-id "kouta-hakuaika-id"}
     {:hakuajat (mapv (fn [hakuaika]
                        (merge
                         {:start (parse-date-time (:alkaa hakuaika))}
                         (when-let [paattyy (:paattyy hakuaika)]
                           {:end (parse-date-time paattyy)})))
                      (:hakuajat hakukohde))})))

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

(s/defn ^:always-validate get-haku :- (s/maybe form-schema/Haku)
  [haku-oid :- s/Str
   cas-client]
  (when-let [haku (some-> :kouta-internal.haku
                          (url-helper/resolve-url haku-oid)
                          (get-result cas-client))]
    (let [hakukohteet      (some-> :kouta-internal.hakukohde-search
                                   (url-helper/resolve-url {"haku" haku-oid})
                                   (get-result cas-client))
          ohjausparametrit (ohjausparametrit-client/get-ohjausparametrit haku-oid)]
      (parse-haku haku hakukohteet ohjausparametrit))))

(s/defn ^:always-validate get-hakus-by-form-key :- [s/Str]
  [cas-client form-key]
  (some-> :kouta-internal.haku-search
          (url-helper/resolve-url {"ataruId" form-key})
          (get-result cas-client)
          ((fn [result] (mapv :oid result)))))

(s/defn ^:always-validate get-hakukohde :- (s/maybe form-schema/Hakukohde)
  [hakukohde-oid :- s/Str
   organization-service
   hakukohderyhmapalvelu-service
   cas-client]
  (when-let [hakukohde (some-> :kouta-internal.hakukohde
                               (url-helper/resolve-url hakukohde-oid)
                               (get-result cas-client))]
    (let [tarjoajat (some->> (seq (:tarjoajat hakukohde))
                             (organization-service/get-organizations-for-oids
                              organization-service))
          hakukohderyhmas (hakukohderyhmapalvelu-service/get-hakukohderyhma-oids-for-hakukohde
                            hakukohderyhmapalvelu-service hakukohde-oid)]
      (parse-hakukohde hakukohde tarjoajat hakukohderyhmas))))

(s/defn ^:always-validate get-hakukohdes-by :- (s/maybe [s/Str])
  [cas-client
   query :- {:haku-oid                      s/Str
             (s/optional-key :tarjoaja-oid) s/Str}]
  (some-> :kouta-internal.hakukohde-search
          (url-helper/resolve-url
           (cond-> {"haku" (:haku-oid query)}
                   (contains? query :tarjoaja-oid)
                   (assoc "tarjoaja" (:tarjoaja-oid query))))
          (get-result cas-client)
          ((fn [result] (mapv :oid result)))))

(s/defn ^:always-validate get-toteutus :- (s/maybe form-schema/Koulutus)
  [toteutus-oid :- s/Str]
  {:oid                  toteutus-oid
   :koulutuskoodi-name   {}
   :koulutusohjelma-name {}
   :tutkintonimike-names []})

(defrecord CacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ haku-oid]
    (get-haku haku-oid cas-client))

  (load-many [this haku-oids]
    (cache-service/default-load-many this haku-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (haku-checker response)))

(defrecord HakukohdeCacheLoader [cas-client organization-service hakukohderyhmapalvelu-service]
  cache-service/CacheLoader

  (load [_ hakukohde-oid]
    (get-hakukohde hakukohde-oid organization-service hakukohderyhmapalvelu-service cas-client))

  (load-many [this hakukohde-oids]
    (cache-service/default-load-many this hakukohde-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-checker response)))

(defrecord HakusByFormKeyCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ form-key]
    (get-hakus-by-form-key cas-client form-key))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakus-by-checker response)))

(defrecord HakukohdeSearchCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ key]
    (let [[haku-oid organization-oid] (string/split key #"#")]
      (get-hakukohdes-by
       cas-client
       (cond-> {:haku-oid haku-oid}
               (some? organization-oid)
               (assoc :tarjoaja-oid organization-oid)))))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-search-checker response)))
