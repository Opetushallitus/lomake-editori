(ns ataru.tarjonta-service.tarjonta-client
  (:require
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.schema.form-schema :as schema]
   [ataru.util.http-util :as http-util]
   [cheshire.core :as json]
   [clojure.string :as string]
   [schema.core :as s])
  (:import [org.joda.time DateTime DateTimeZone]))

(def koulutus-checker (s/checker schema/Koulutus))
(def hakukohde-checker (s/checker schema/Hakukohde))
(def haku-checker (s/checker schema/Haku))
(def hakukohde-search-checker (s/checker [s/Str]))

(defn- localized-names
  ([names]
   (localized-names names identity))
  ([names key-fn]
   (into {}
         (for [[input-k output-k] [[:kieli_fi :fi]
                                   [:kieli_sv :sv]
                                   [:kieli_en :en]]
               :let               [val (key-fn (or (input-k names) (output-k names)))]
               :when              (not (clojure.string/blank? val))]
           [output-k val]))))

(defn- parse-koulutuskoodi
  [koulutus]
  (-> koulutus
      :koulutuskoodi
      :meta
      (localized-names :nimi)))

(defn- parse-tutkintonimikes
  [koulutus]
  (mapv (fn [[_ name]]
          (-> name
              :meta
              (localized-names :nimi)))
        (-> koulutus :tutkintonimikes :meta)))

(defn- parse-koulutusohjelma
  [koulutus]
  (-> koulutus
      :koulutusohjelma
      :tekstis
      localized-names))

(defn- parse-koulutus
  [response]
  (cond-> {:oid                  (:oid response)
           :koulutuskoodi-name   (parse-koulutuskoodi response)
           :tutkintonimike-names (parse-tutkintonimikes response)
           :koulutusohjelma-name (parse-koulutusohjelma response)}
          (not (clojure.string/blank? (:tarkenne response)))
          (assoc :tarkenne (:tarkenne response))))

(defn- parse-can-be-applied-to?
  [hakukohde]
  (case (:tila hakukohde)
    "POISTETTU"     false
    "LUONNOS"       false
    "VALMIS"        true
    "JULKAISTU"     true
    "PERUTTU"       false
    "KOPIOITU"      false
    "PUUTTEELLINEN" false
    (throw
     (new RuntimeException
          (str "Unknown hakukohteen tila " (:tila hakukohde)
               " in hakukohde " (:oid hakukohde))))))

(defn parse-hakukohde
  [hakukohde]
  (merge {:oid                                                         (:oid hakukohde)
          :hakukohteen-tiedot-url                                      (resolve-url :tarjonta-app.hakukohde (:oid hakukohde))
          :can-be-applied-to?                                          (parse-can-be-applied-to? hakukohde)
          :haku-oid                                                    (:hakuOid hakukohde)
          :koulutus-oids                                               (map :oid (:koulutukset hakukohde))
          :name                                                        (localized-names (:hakukohteenNimet hakukohde))
          :tarjoaja-name                                               (localized-names (:tarjoajaNimet hakukohde))
          :tarjoaja-oids                                               (:tarjoajaOids hakukohde)
          :ryhmaliitokset                                              (some->> (:ryhmaliitokset hakukohde)
                                                                                (map :ryhmaOid)
                                                                                (distinct))
          :koulutustyypit                                              []
          :hakukelpoisuusvaatimus-uris                                 (:hakukelpoisuusvaatimusUris hakukohde)
          :ylioppilastutkinto-antaa-hakukelpoisuuden?                  (boolean (:ylioppilastutkintoAntaaHakukelpoisuuden hakukohde))
          :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? (boolean (:josYoEiMuitaLiitepyyntoja hakukohde))}
         (if (:kaytetaanHakukohdekohtaistaHakuaikaa hakukohde)
           {:hakuajat [(merge {:start (new DateTime
                                           (:hakuaikaAlkuPvm hakukohde)
                                           (DateTimeZone/forID "Europe/Helsinki"))}
                              (when (some? (:hakuaikaLoppuPvm hakukohde))
                                {:end (new DateTime
                                           (:hakuaikaLoppuPvm hakukohde)
                                           (DateTimeZone/forID "Europe/Helsinki"))}))]}
           {:hakuaika-id (:hakuaikaId hakukohde)})))

(defn- parse-hakuaika
  [hakuaika]
  (cond-> {:hakuaika-id (:hakuaikaId hakuaika)
           :start       (new DateTime
                             (:alkuPvm hakuaika)
                             (DateTimeZone/forID "Europe/Helsinki"))}
          (contains? hakuaika :loppuPvm)
          (assoc :end (new DateTime
                           (:loppuPvm hakuaika)
                           (DateTimeZone/forID "Europe/Helsinki")))))

(defn parse-haku
  [haku]
  (merge
   {:oid                                        (:oid haku)
    :name                                       (localized-names (:nimi haku))
    :hakukohteet                                (:hakukohdeOids haku)
    :ylioppilastutkinto-antaa-hakukelpoisuuden? (boolean (:ylioppilastutkintoAntaaHakukelpoisuuden haku))
    :kohdejoukko-uri                            (:kohdejoukkoUri haku)
    :hakutapa-uri                               (:hakutapaUri haku)
    :hakukausi-vuosi                            (:hakukausiVuosi haku)
    :yhteishaku                                 (= (:hakutapaUri haku) "hakutapa_01#1")
    :prioritize-hakukohteet                     (boolean (:usePriority haku))
    :can-submit-multiple-applications           (boolean (:canSubmitMultipleApplications haku))
    :sijoittelu                                 (boolean (:sijoittelu haku))
    :hakuajat                                   (mapv parse-hakuaika (:hakuaikas haku))
    :haun-tiedot-url                            (resolve-url :tarjonta-app.haku (:oid haku))}
   (when (some? (:ataruLomakeAvain haku))
     {:ataru-form-key (:ataruLomakeAvain haku)})
   (when (and (some? (:maxHakukohdes haku))
              (pos? (:maxHakukohdes haku)))
     {:max-hakukohteet (:maxHakukohdes haku)})))

(defn- parse-search-result
  [search-result]
  (->> (:tulokset search-result)
       (mapcat :tulokset)
       (map :oid)
       seq))

(defn- get-result
  [url]
  (let [{:keys [status body]} (http-util/do-get url)]
    (when (not= 200 status)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))
    (let [{:keys [status result]} (json/parse-string body true)]
      (case status
        "OK"        result
        "NOT_FOUND" nil
        (throw (new RuntimeException (str "Could not get " url ", "
                                          "status: " status ", "
                                          "body: " body)))))))

(s/defn ^:always-validate get-hakukohde :- (s/maybe schema/Hakukohde)
  [hakukohde-oid :- s/Str]
  (some-> :tarjonta-service.hakukohde
          (resolve-url hakukohde-oid)
          get-result
          parse-hakukohde))

(s/defn ^:always-validate hakukohde-search :- (s/maybe [s/Str])
  [haku-oid :- s/Str
   organization-oid :- (s/maybe s/Str)]
  (-> :tarjonta-service.hakukohde.search
      (resolve-url (merge {"hakuOid" haku-oid}
                          (when (some? organization-oid)
                            {"organisationOid" organization-oid})))
      get-result
      parse-search-result))

(s/defn ^:always-validate get-haku :- (s/maybe schema/Haku)
  [haku-oid :- s/Str]
  (some-> :tarjonta-service.haku
          (resolve-url haku-oid)
          get-result
          parse-haku))

(s/defn ^:always-validate get-koulutus :- (s/maybe schema/Koulutus)
  [koulutus-oid :- s/Str]
  (some-> :tarjonta-service.koulutus
          (resolve-url koulutus-oid)
          get-result
          parse-koulutus))

(defn get-forms-in-use
  [organization-oid]
  (-> :tarjonta-service.forms-in-use
      (resolve-url {"oid" organization-oid})
      get-result))
