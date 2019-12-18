(ns ataru.tarjonta-service.kouta.kouta-client
  (:require [ataru.cache.cache-service :as cache-service]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.schema.form-schema :as form-schema]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(def haku-checker (s/checker form-schema/Haku))
(def hakukohde-checker (s/checker form-schema/Hakukohde))
(def hakus-by-checker (s/checker [s/Str]))

(defn- parse-date-time
  [s]
  (let [tz  (t/time-zone-for-id "Europe/Helsinki")
        fmt (f/formatter "yyyy-MM-dd'T'HH:mm" tz)]
    (t/to-time-zone (f/parse fmt s) tz)))

(defn- parse-haku
  [haku]
  (let [hakuajat (mapv (fn [hakuaika]
                         {:hakuaika-id "kouta-hakuaika-id"
                          :start       (parse-date-time (:alkaa hakuaika))
                          :end         (parse-date-time (:paattyy hakuaika))})
                       (:hakuajat haku))]
    (merge
     {:can-submit-multiple-applications           true
      :hakuajat                                   hakuajat
      :hakukausi-vuosi                            (->> hakuajat
                                                       (map #(t/year (:start %)))
                                                       (apply max))
      :hakukohteet                                (:hakukohteet haku)
      :hakutapa-uri                               (:hakutapaKoodiUri haku)
      :haun-tiedot-url                            (url-helper/resolve-url :kouta-app.haku (:oid haku))
      :kohdejoukko-uri                            (:kohdejoukkoKoodiUri haku)
      :name                                       (:nimi haku)
      :oid                                        (:oid haku)
      :prioritize-hakukohteet                     false
      :sijoittelu                                 false
      :yhteishaku                                 (clojure.string/starts-with?
                                                   (:hakutapaKoodiUri haku)
                                                   "hakutapa_01#")
      :ylioppilastutkinto-antaa-hakukelpoisuuden? false}
     (when (some? (:hakulomakeAtaruId haku))
       {:ataru-form-key (:hakulomakeAtaruId haku)}))))

(defn- parse-hakukohde-tila
  [hakukohde]
  (case (:tila hakukohde)
    "tallennettu" :luonnos
    "julkaistu"   :julkaistu
    "arkistoitu"  :julkaistu
    (throw
     (new RuntimeException
          (str "Unknown hakukohteen tila " (:tila hakukohde)
               " in hakukohde " (:oid hakukohde))))))

(defn- parse-hakukohde
  [hakukohde tarjoajat]
  (merge
   {:oid                                                         (:oid hakukohde)
    :tila                                                        (parse-hakukohde-tila hakukohde)
    :haku-oid                                                    (:hakuOid hakukohde)
    :koulutus-oids                                               [(:toteutusOid hakukohde)]
    :name                                                        (:nimi hakukohde)
    :tarjoaja-name                                               (or (:name (first tarjoajat)) {})
    :tarjoaja-oids                                               (mapv :oid tarjoajat)
    :ryhmaliitokset                                              []
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
  (some-> :kouta-internal.haku
          (url-helper/resolve-url haku-oid)
          (get-result cas-client)
          parse-haku))

(s/defn ^:always-validate get-hakus-by :- (s/maybe [s/Str])
  [cas-client
   query :- {(s/optional-key :form-key)     s/Str
             (s/optional-key :tarjoaja-oid) s/Str}]
  (-> :kouta-internal.haku-search
      (url-helper/resolve-url
       (cond-> {}
               (contains? query :form-key)
               (assoc "ataruId" (:form-key query))
               (contains? query :tarjoaja-oid)
               (assoc "hakukohteenTarjoaja" (:tarjoaja-oid query))))
      (get-result cas-client)
      ((fn [result] (mapv :oid result)))))

(s/defn ^:always-validate get-hakukohde :- (s/maybe form-schema/Hakukohde)
  [hakukohde-oid :- s/Str
   organization-service
   cas-client]
  (let [hakukohde (some-> :kouta-internal.hakukohde
                          (url-helper/resolve-url hakukohde-oid)
                          (get-result cas-client))
        toteutus  (some-> :kouta-internal.toteutus
                          (url-helper/resolve-url (:toteutusOid hakukohde))
                          (get-result cas-client))
        tarjoajat (some->> (or (seq (:tarjoajat hakukohde))
                               (seq (:tarjoajat toteutus)))
                           (organization-service/get-organizations-for-oids
                            organization-service))]
    (parse-hakukohde hakukohde tarjoajat)))

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

(defrecord HakukohdeCacheLoader [cas-client organization-service]
  cache-service/CacheLoader

  (load [_ hakukohde-oid]
    (get-hakukohde hakukohde-oid organization-service cas-client))

  (load-many [this hakukohde-oids]
    (cache-service/default-load-many this hakukohde-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-checker response)))

(defrecord HakusByFormKeyCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ form-key]
    (get-hakus-by cas-client {:form-key form-key}))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakus-by-checker response)))

(defrecord HakusByHakukohteenTarjoajaCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ tarjoaja-oid]
    (get-hakus-by cas-client {:tarjoaja-oid tarjoaja-oid}))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakus-by-checker response)))