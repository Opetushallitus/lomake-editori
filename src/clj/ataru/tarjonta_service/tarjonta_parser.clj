(ns ataru.tarjonta-service.tarjonta-parser
  (:require [clj-time.core :as t]
            [taoensso.timbre :as log]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.koodisto.koodisto :refer [get-koodisto-options]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol]))

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
    {:oid                                                         (:oid hakukohde)
     :name                                                        (:name hakukohde)
     :can-be-applied-to?                                          (:can-be-applied-to? hakukohde)
     :hakukohderyhmat                                             (filter #(contains? hakukohderyhmat %) (:ryhmaliitokset hakukohde))
     :kohdejoukko-korkeakoulu?                                    (clojure.string/starts-with?
                                                                   (:kohdejoukko-uri haku)
                                                                   "haunkohdejoukko_12#")
     :tarjoaja-name                                               (:tarjoaja-name hakukohde)
     :form-key                                                    (:ataru-form-key haku)
     :koulutukset                                                 (mapv #(or (get tarjonta-koulutukset %)
                                                                             (throw (new RuntimeException (str "Koulutus " % " not found"))))
                                                                        (:koulutus-oids hakukohde))
     :koulutustyypit                                              (:koulutustyypit hakukohde)
     :hakuaika                                                    (hakuaika/hakukohteen-hakuaika now haku ohjausparametrit hakukohde)
     :applicable-base-educations                                  (mapcat pohjakoulutukset-by-vaatimus (:hakukelpoisuusvaatimus-uris hakukohde))
     :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? (boolean (:jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? hakukohde))}))

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
                                                                    1
                                                                    false))
           tarjonta-hakukohteet              (tarjonta-protocol/get-hakukohteet tarjonta-service
                                                                                included-hakukohde-oids)
           tarjonta-koulutukset              (->> tarjonta-hakukohteet
                                                  (mapcat :koulutus-oids)
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
                                                  tarjonta-hakukohteet)]
       (println "HAKUKOHTEET 12 " hakukohteet)

       (when (not-empty hakukohteet)
         {:tarjonta
          {:hakukohteet                      hakukohteet
           :haku-oid                         haku-oid
           :haku-name                        (:name haku)
           :prioritize-hakukohteet           (:prioritize-hakukohteet haku)
           :max-hakukohteet                  (:max-hakukohteet haku)
           :hakuaika                         (hakuaika/haun-hakuaika now haku ohjausparametrit)
           :can-submit-multiple-applications (:can-submit-multiple-applications haku)
           :kohdejoukko-uri                  (:kohdejoukko-uri haku)
           :yhteishaku                       (:yhteishaku haku)}}))))
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid]
   (when haku-oid
     (parse-tarjonta-info-by-haku koodisto-cache
                                  tarjonta-service
                                  organization-service
                                  ohjausparametrit-service
                                  haku-oid
                                  (or (->> haku-oid
                                           (tarjonta-protocol/get-haku tarjonta-service)
                                           :hakukohteet)
                                      [])))))
