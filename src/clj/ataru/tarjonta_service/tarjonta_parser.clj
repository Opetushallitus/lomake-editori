(ns ataru.tarjonta-service.tarjonta-parser
  (:require [clj-time.core :as t]
            [taoensso.timbre :as log]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.tarjonta-service.tarjonta-service :refer [yhteishaku?]]
            [ataru.koodisto.koodisto :refer [get-koodisto-options]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol]))

(def ^:private lang-key-renames {:kieli_fi :fi :kieli_en :en :kieli_sv :sv})

(defn- localized-names
  ([names]
   (localized-names names identity))
  ([names key-fn]
  (into {}
        (for [lang [:fi :sv :en]
              :when (contains? names lang)]
          [lang (-> names lang key-fn)]))))

(defn- parse-hakukohde
  [tarjonta-service
   now
   hakukohderyhmat
   haku
   tarjonta-koulutukset
   ohjausparametrit
   pohjakoulutukset-by-vaatimus
   hakukohde]
  (when (:oid hakukohde)
    {:oid                        (:oid hakukohde)
     :name                       (->> (clojure.set/rename-keys (:hakukohteenNimet hakukohde)
                                                               lang-key-renames)
                                      (remove (comp clojure.string/blank? second))
                                      (into {}))
     :hakukohderyhmat            (->> (:ryhmaliitokset hakukohde)
                                      (map :ryhmaOid)
                                      (distinct)
                                      (filter #(contains? hakukohderyhmat %)))
     :kohdejoukko-korkeakoulu?   (clojure.string/starts-with?
                                  (:kohdejoukkoUri haku)
                                  "haunkohdejoukko_12#")
     :tarjoaja-name              (:tarjoajaNimet hakukohde)
     :form-key                   (:ataruLomakeAvain hakukohde)
     :koulutukset                (mapv #(or (get tarjonta-koulutukset (:oid %))
                                            (throw (new RuntimeException (str "Koulutus " (:oid %) " not found"))))
                                       (:koulutukset hakukohde))
     :hakuaika                   (hakuaika/get-hakuaika-info now haku ohjausparametrit hakukohde)
     :applicable-base-educations (mapcat pohjakoulutukset-by-vaatimus (:hakukelpoisuusvaatimusUris hakukohde))}))

(defn- pohjakoulutukset-by-vaatimus
  [pohjakoulutusvaatimuskorkeakoulut]
  (reduce (fn [m {:keys [uri within]}]
            (assoc m uri (->> within
                              (filter #(clojure.string/starts-with? (:uri %) "pohjakoulutuskklomake_"))
                              (map :value))))
          {}
          pohjakoulutusvaatimuskorkeakoulut))

(defn parse-tarjonta-info-by-haku
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid included-hakukohde-oids]
   {:pre [(some? tarjonta-service)
          (some? organization-service)
          (some? ohjausparametrit-service)]}
   (when haku-oid
     (let [now                               (t/now)
           hakukohderyhmat                   (->> (organization-service/get-hakukohde-groups
                                                    organization-service)
                                                  (map :oid)
                                                  (set))
           haku                              (tarjonta-protocol/get-haku
                                               tarjonta-service
                                               haku-oid)
           ohjausparametrit                  (ohjausparametrit-protocol/get-parametri
                                               ohjausparametrit-service
                                               haku-oid)
           pohjakoulutukset-by-vaatimus      (pohjakoulutukset-by-vaatimus
                                              (get-koodisto-options koodisto-cache
                                                                    "pohjakoulutusvaatimuskorkeakoulut"
                                                                    1))
           tarjonta-hakukohteet              (tarjonta-protocol/get-hakukohteet tarjonta-service
                                                                                included-hakukohde-oids)
           tarjonta-koulutukset              (->> tarjonta-hakukohteet
                                                  (mapcat :koulutukset)
                                                  (map :oid)
                                                  distinct
                                                  (tarjonta-protocol/get-koulutukset tarjonta-service))
           hakukohteet                       (map #(parse-hakukohde tarjonta-service
                                                                    now
                                                                    hakukohderyhmat
                                                                    haku
                                                                    tarjonta-koulutukset
                                                                    ohjausparametrit
                                                                    pohjakoulutukset-by-vaatimus
                                                                    %)
                                                  tarjonta-hakukohteet)
           max-hakukohteet                   (:maxHakukohdes haku)]
       (when (not-empty hakukohteet)
         {:tarjonta
          {:hakukohteet                      hakukohteet
           :haku-oid                         haku-oid
           :haku-name                        (-> haku :nimi (clojure.set/rename-keys lang-key-renames) localized-names)
           :prioritize-hakukohteet           (:usePriority haku)
           :max-hakukohteet                  (when (and max-hakukohteet (pos? max-hakukohteet))
                                               max-hakukohteet)
           :hakuaika                         (hakuaika/select-hakuaika now (map :hakuaika hakukohteet))
           :can-submit-multiple-applications (:canSubmitMultipleApplications haku)
           :yhteishaku                       (yhteishaku? haku)}}))))
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid]
   (when haku-oid
     (parse-tarjonta-info-by-haku koodisto-cache
                                  tarjonta-service
                                  organization-service
                                  ohjausparametrit-service
                                  haku-oid
                                  (or (->> haku-oid
                                           (.get-haku tarjonta-service)
                                           :hakukohdeOids)
                                      [])))))
