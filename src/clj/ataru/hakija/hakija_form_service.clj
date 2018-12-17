(ns ataru.hakija.hakija-form-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.forms.hakukohderyhmat :as hakukohderyhmat]
            [ataru.hakija.person-info-fields :refer [viewing-forbidden-person-info-field-ids
                                                     editing-forbidden-person-info-field-ids
                                                     editing-allowed-person-info-field-ids]]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options populate-attachment-deadlines]]
            [taoensso.timbre :refer [warn]]
            [clj-time.core :as time]
            [clj-time.coerce :as t]
            [clj-time.format :as f]
            [cheshire.core :as json]
            [schema.core :as s]
            [schema.coerce :as sc]
            [ring.swagger.coerce :as coerce]
            [ataru.schema.form-schema :as form-schema]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.hakija.form-role :as form-role]
            [ataru.component-data.component :as component]
            [medley.core :refer [find-first]]
            [ataru.util :as util :refer [assoc?]]))

(defn- set-can-submit-multiple-applications-and-yhteishaku
  [multiple? yhteishaku? haku-oid field]
  (-> field
      (assoc-in [:params :can-submit-multiple-applications] multiple?)
      (assoc-in [:params :yhteishaku] yhteishaku?)
      (cond-> (not multiple?) (assoc-in [:params :haku-oid] haku-oid))))

(defn- map-if-ssn-or-email
  [f field]
  (if (or (= "ssn" (:id field))
          (= "email" (:id field)))
    (f field)
    field))

(defn set-submit-multiple-and-yhteishaku-if-ssn-or-email-field
  [multiple? yhteishaku? haku-oid]
  (partial map-if-ssn-or-email
           (partial set-can-submit-multiple-applications-and-yhteishaku
                    multiple? yhteishaku? haku-oid)))

(defn- update-ssn-and-email-fields-in-person-module
  [multiple? yhteishaku? haku-oid form]
  (when-let [person-module-idx (util/first-index-of #(= (:module %) "person-info") (:content form))]
    (let [person-module     (nth (:content form) person-module-idx)
          new-person-module (update person-module :children (partial clojure.walk/prewalk (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid)))]
      (assoc-in form [:content person-module-idx] new-person-module))))

(defn populate-can-submit-multiple-applications
  [form tarjonta-info]
  (let [multiple?   (get-in tarjonta-info [:tarjonta :can-submit-multiple-applications] true)
        yhteishaku? (get-in tarjonta-info [:tarjonta :yhteishaku] false)
        haku-oid    (get-in tarjonta-info [:tarjonta :haku-oid])]
    (or
      (update-ssn-and-email-fields-in-person-module multiple? yhteishaku? haku-oid form)
      (update form :content
              (fn [content]
                (clojure.walk/prewalk (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid) content))))))

(defn- custom-deadline [field]
  (get-in field [:params :deadline]))

(def deadline-format (f/formatter "dd.MM.yyyy HH:mm" (time/time-zone-for-id "Europe/Helsinki")))

(defn- editing-allowed-by-custom-deadline? [field]
  (some->> (custom-deadline field)
           (f/parse deadline-format)
           (time/before? (time/now))))

(defn- editing-allowed-by-hakuaika?
  [field hakuajat application-in-processing-state?]
  (let [hakuaika            (hakuaika/select-hakuaika-for-field field hakuajat)
        hakuaika-start      (some-> hakuaika :start t/from-long)
        hakuaika-end        (some-> hakuaika :end t/from-long)
        attachment-edit-end (hakuaika/attachment-edit-end hakuaika)
        hakukierros-end     (some-> hakuaika :hakukierros-end t/from-long)
        after?              (fn [t] (or (nil? t)
                                        (time/after? (time/now) t)))
        before?             (fn [t] (and (some? t)
                                         (time/before? (time/now) t)))]
    (or (nil? hakuaika)
        (and (not (and application-in-processing-state? (:jatkuva-haku? hakuaika)))
             (after? hakuaika-start)
             (or (before? hakuaika-end)
                 (and (before? attachment-edit-end)
                      (= "attachment" (:fieldType field)))
                 (and (before? hakukierros-end)
                      (contains? editing-allowed-person-info-field-ids
                        (keyword (:id field)))))))))

(defn- uneditable?
  [field hakuajat roles application-in-processing-state?]
  (not (and (or (and (form-role/virkailija? roles)
                     (not (form-role/with-henkilo? roles)))
                (not (contains? editing-forbidden-person-info-field-ids (keyword (:id field)))))
            (or (form-role/virkailija? roles)
                (if (custom-deadline field)
                  (editing-allowed-by-custom-deadline? field)
                  (editing-allowed-by-hakuaika? field hakuajat application-in-processing-state?)))
            (or (form-role/virkailija? roles)
                (not (and (empty? (:uniques hakuajat))
                          application-in-processing-state?))))))

(defn flag-uneditable-and-unviewable-field
  [hakuajat roles application-in-processing-state? field]
  (if (= "formField" (:fieldClass field))
    (let [cannot-view? (and (contains? viewing-forbidden-person-info-field-ids
                                       (keyword (:id field)))
                            (not (form-role/virkailija? roles)))
          cannot-edit? (or cannot-view?
                           (uneditable? field hakuajat roles application-in-processing-state?))]
      (assoc field
             :cannot-view cannot-view?
             :cannot-edit cannot-edit?))
    field))

(s/defn ^:always-validate flag-uneditable-and-unviewable-fields :- s/Any
  [form :- s/Any
   hakukohteet :- s/Any
   roles :- [form-role/FormRole]
   application-in-processing-state? :- s/Bool]
  (let [hakuajat (hakuaika/index-hakuajat hakukohteet)]
    (update form :content (partial util/map-form-fields
                                   (partial flag-uneditable-and-unviewable-field
                                            hakuajat
                                            roles
                                            application-in-processing-state?)))))

(s/defn ^:always-validate remove-required-hakija-validator-if-virkailija :- s/Any
  [form :- s/Any
   roles :- [form-role/FormRole]]
  (if (form-role/virkailija? roles)
    (update form :content
            (fn [content]
              (clojure.walk/prewalk
                (fn [field]
                  (if (= "formField" (:fieldClass field))
                    (update field :validators (partial remove #{"required-hakija"}))
                    field))
                content)))
    form))

(s/defn ^:always-validate fetch-form-by-id :- s/Any
  [id :- s/Any
   roles :- [form-role/FormRole]
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool]
  (when-let [form (form-store/fetch-by-id id)]
    (when (not (:deleted form))
      (-> (koodisto/populate-form-koodisto-fields-cached koodisto-cache form)
          (remove-required-hakija-validator-if-virkailija roles)
          (populate-attachment-deadlines hakukohteet)
          (flag-uneditable-and-unviewable-fields hakukohteet roles application-in-processing-state?)))))

(s/defn ^:always-validate fetch-form-by-key :- s/Any
  [key :- s/Any
   roles :- [form-role/FormRole]
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool]
  (when-let [latest-id (form-store/latest-id-by-key key)]
    (fetch-form-by-id latest-id
                      roles
                      koodisto-cache
                      hakukohteet
                      application-in-processing-state?)))

(s/defn ^:always-validate fetch-form-by-haku-oid-and-id :- s/Any
  [tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   haku-oid :- s/Any
   id :- s/Int
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid)
        hakukohteet   (get-in tarjonta-info [:tarjonta :hakukohteet])
        priorisoivat  (:ryhmat (hakukohderyhmat/priorisoivat-hakukohderyhmat tarjonta-service haku-oid))
        rajaavat      (:ryhmat (hakukohderyhmat/rajaavat-hakukohderyhmat haku-oid))
        form          (fetch-form-by-id id roles koodisto-cache hakukohteet application-in-processing-state?)]
    (when (not tarjonta-info)
      (throw (Exception. (str "No haku found for haku " haku-oid))))
    (if form
      (-> form
          (merge tarjonta-info)
          (assoc? :priorisoivat-hakukohderyhmat priorisoivat)
          (assoc? :rajaavat-hakukohderyhmat rajaavat)
          (assoc :load-time (System/currentTimeMillis))
          (populate-hakukohde-answer-options tarjonta-info)
          (populate-can-submit-multiple-applications tarjonta-info))
      (warn "could not find local form for haku" haku-oid "with id" id))))

(s/defn ^:always-validate fetch-form-by-haku-oid :- s/Any
  [tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   haku-oid :- s/Any
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (if-let [latest-id (some-> (tarjonta/get-haku tarjonta-service haku-oid)
                             :ataruLomakeAvain
                             form-store/latest-id-by-key)]
    (fetch-form-by-haku-oid-and-id tarjonta-service
                                   koodisto-cache
                                   organization-service
                                   ohjausparametrit-service
                                   haku-oid
                                   latest-id
                                   application-in-processing-state?
                                   roles)
    (throw (RuntimeException. (str "No form found for haku " haku-oid)))))

(s/defn ^:always-validate fetch-form-by-hakukohde-oid :- s/Any
  [tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   hakukohde-oid :- s/Any
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (let [hakukohde (tarjonta/get-hakukohde tarjonta-service hakukohde-oid)]
    (fetch-form-by-haku-oid tarjonta-service
                            koodisto-cache
                            organization-service
                            ohjausparametrit-service
                            (:hakuOid hakukohde)
                            false
                            roles)))

(s/defn ^:always-validate fetch-form-by-haku-oid-and-id-cached :- s/Any
  [form-by-haku-oid-and-id-cache :- s/Any
   haku-oid :- s/Str
   id :- s/Int
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (cache/get-from form-by-haku-oid-and-id-cache
                  (apply str
                         haku-oid
                         "#" id
                         "#" application-in-processing-state?
                         (sort (map #(str "#" (name %)) roles)))))

(s/defn ^:always-validate fetch-form-by-haku-oid-cached :- s/Any
  [tarjonta-service
   form-by-haku-oid-and-id-cache
   haku-oid :- s/Any
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (if-let [latest-id (some-> (tarjonta/get-haku tarjonta-service haku-oid)
                             :ataruLomakeAvain
                             form-store/latest-id-by-key)]
    (fetch-form-by-haku-oid-and-id-cached form-by-haku-oid-and-id-cache
                                          haku-oid
                                          latest-id
                                          application-in-processing-state?
                                          roles)
    (throw (RuntimeException. (str "No form found for haku " haku-oid)))))

(s/defn ^:always-validate fetch-form-by-haku-oid-str-cached :- s/Any
  [form-by-haku-oid-str-cache :- s/Any
   haku-oid :- s/Str
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (cache/get-from form-by-haku-oid-str-cache
                  (apply str
                         haku-oid
                         "#" application-in-processing-state?
                         (sort (map #(str "#" (name %)) roles)))))

(s/defn ^:always-validate fetch-form-by-hakukohde-oid-str-cached :- s/Any
  [tarjonta-service :- s/Any
   form-by-haku-oid-str-cache :- s/Any
   hakukohde-oid :- s/Str
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (let [hakukohde (tarjonta/get-hakukohde tarjonta-service hakukohde-oid)]
    (fetch-form-by-haku-oid-str-cached form-by-haku-oid-str-cache
                                       (:hakuOid hakukohde)
                                       false
                                       roles)))

(defrecord FormByHakuOidAndIdCacheLoader [tarjonta-service
                                          koodisto-cache
                                          organization-service
                                          ohjausparametrit-service]
  cache/CacheLoader
  (load [_ key]
    (let [[haku-oid id aips? & roles] (clojure.string/split key #"#")]
      (fetch-form-by-haku-oid-and-id tarjonta-service
                                     koodisto-cache
                                     organization-service
                                     ohjausparametrit-service
                                     haku-oid
                                     (Integer/valueOf id)
                                     (Boolean/valueOf aips?)
                                     (map keyword roles))))
  (load-many [this keys]
    (into {} (keep #(when-let [v (cache/load this %)] [% v]) keys))))

(def form-coercer (sc/coercer! form-schema/FormWithContentAndTarjontaMetadata
                               coerce/json-schema-coercion-matcher))

(defrecord FormByHakuOidStrCacheLoader [tarjonta-service
                                        koodisto-cache
                                        organization-service
                                        ohjausparametrit-service]
  cache/CacheLoader
  (load [_ key]
    (let [[haku-oid aips? & roles] (clojure.string/split key #"#")]
      (json/generate-string
       (form-coercer
        (fetch-form-by-haku-oid tarjonta-service
                                koodisto-cache
                                organization-service
                                ohjausparametrit-service
                                haku-oid
                                (Boolean/valueOf aips?)
                                (map keyword roles))))))
  (load-many [this keys]
    (into {} (keep #(when-let [v (cache/load this %)] [% v]) keys))))
