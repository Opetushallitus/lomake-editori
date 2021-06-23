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
            [taoensso.timbre :as log]
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

(defn- editing-allowed-by-custom-deadline? [now field]
  (some->> (custom-deadline field)
           (f/parse deadline-format)
           (time/before? now)))

(defn- editing-allowed-by-hakuaika?
  [now field hakuajat application-in-processing-state?]
  (let [hakuaika            (hakuaika/select-hakuaika-for-field now field hakuajat)
        hakuaika-start      (some-> hakuaika :start t/from-long)
        hakuaika-end        (some-> hakuaika :end t/from-long)
        attachment-edit-end (hakuaika/attachment-edit-end hakuaika)
        hakukierros-end     (some-> hakuaika :hakukierros-end t/from-long)
        after?              (fn [t] (or (nil? t)
                                        (time/after? now t)))
        before?             (fn [t] (and (some? t)
                                         (time/before? now t)))]
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
  [now field hakuajat roles application-in-processing-state? field-deadline]
  (not (and (or (and (form-role/virkailija? roles)
                     (not (form-role/with-henkilo? roles)))
                (not (contains? editing-forbidden-person-info-field-ids (keyword (:id field)))))
            (or (form-role/virkailija? roles)
                (cond (some? field-deadline)
                      (time/before? now (:deadline field-deadline))
                      (custom-deadline field)
                      (editing-allowed-by-custom-deadline? now field)
                      :else
                      (editing-allowed-by-hakuaika? now field hakuajat application-in-processing-state?)))
            (or (form-role/virkailija? roles)
                (not (and (empty? (:uniques hakuajat))
                          application-in-processing-state?))))))

(defn flag-uneditable-and-unviewable-field
  [now hakuajat roles application-in-processing-state? field-deadlines field]
  (if (= "formField" (:fieldClass field))
    (let [cannot-view? (and (contains? viewing-forbidden-person-info-field-ids
                                       (keyword (:id field)))
                            (not (form-role/virkailija? roles)))
          cannot-edit? (or cannot-view?
                           (uneditable?
                            now
                            field
                            hakuajat
                            roles
                            application-in-processing-state?
                            (get field-deadlines (:id field))))]
      (assoc field
             :cannot-view cannot-view?
             :cannot-edit cannot-edit?))
    field))

(s/defn ^:always-validate flag-uneditable-and-unviewable-fields :- s/Any
  [form :- s/Any
   now :- s/Any
   hakuajat :- s/Any
   roles :- [form-role/FormRole]
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}]
  (update form :content (partial util/map-form-fields
                                 (partial flag-uneditable-and-unviewable-field
                                          now
                                          hakuajat
                                          roles
                                          application-in-processing-state?
                                          field-deadlines))))

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
   form-by-id-cache :- s/Any
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}]
  (let [now      (time/now)
        hakuajat (hakuaika/index-hakuajat hakukohteet)]
    (when-let [form (cache/get-from form-by-id-cache (str id))]
      (when (not (:deleted form))
        (-> (koodisto/populate-form-koodisto-fields koodisto-cache form)
            (remove-required-hakija-validator-if-virkailija roles)
            (populate-attachment-deadlines now hakuajat field-deadlines)
            (flag-uneditable-and-unviewable-fields now hakuajat roles application-in-processing-state? field-deadlines))))))

(s/defn ^:always-validate fetch-form-by-key :- s/Any
  [key :- s/Any
   roles :- [form-role/FormRole]
   form-by-id-cache :- s/Any
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}]
  (when-let [latest-id (form-store/latest-id-by-key key)]
    (fetch-form-by-id latest-id
                      roles
                      form-by-id-cache
                      koodisto-cache
                      hakukohteet
                      application-in-processing-state?
                      field-deadlines)))

(s/defn ^:always-validate fetch-form-by-haku-oid-and-id :- s/Any
  [form-by-id-cache :- s/Any
   tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   haku-oid :- s/Any
   id :- s/Int
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   roles :- [form-role/FormRole]]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid)
        hakukohteet   (get-in tarjonta-info [:tarjonta :hakukohteet])
        priorisoivat  (:ryhmat (hakukohderyhmat/priorisoivat-hakukohderyhmat tarjonta-service haku-oid))
        rajaavat      (:ryhmat (hakukohderyhmat/rajaavat-hakukohderyhmat haku-oid))
        form          (fetch-form-by-id
                       id
                       roles
                       form-by-id-cache
                       koodisto-cache
                       hakukohteet
                       application-in-processing-state?
                       field-deadlines)]
    (when (and (some? form) (some? tarjonta-info))
      (-> form
          (merge tarjonta-info)
          (assoc? :priorisoivat-hakukohderyhmat priorisoivat)
          (assoc? :rajaavat-hakukohderyhmat rajaavat)
          (populate-hakukohde-answer-options tarjonta-info)
          (populate-can-submit-multiple-applications tarjonta-info)))))

(s/defn ^:always-validate fetch-form-by-haku-oid :- s/Any
  [form-by-id-cache :- s/Any
   tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   haku-oid :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   roles :- [form-role/FormRole]]
  (when-let [latest-id (some-> (tarjonta/get-haku tarjonta-service haku-oid)
                               :ataru-form-key
                               form-store/latest-id-by-key)]
    (fetch-form-by-haku-oid-and-id form-by-id-cache
                                   tarjonta-service
                                   koodisto-cache
                                   organization-service
                                   ohjausparametrit-service
                                   haku-oid
                                   latest-id
                                   application-in-processing-state?
                                   field-deadlines
                                   roles)))

(s/defn ^:always-validate fetch-form-by-haku-oid-str-cached :- s/Any
  [form-by-haku-oid-str-cache :- s/Any
   haku-oid :- s/Str
   roles :- [form-role/FormRole]]
  (cache/get-from form-by-haku-oid-str-cache
                  (apply str
                         haku-oid
                         "#" false ;; TODO remove with care, keys linger in Redis
                         (sort (map #(str "#" (name %)) roles)))))

(s/defn ^:always-validate fetch-form-by-hakukohde-oid-str-cached :- s/Any
  [tarjonta-service :- s/Any
   form-by-haku-oid-str-cache :- s/Any
   hakukohde-oid :- s/Str
   roles :- [form-role/FormRole]]
  (when-let [hakukohde (tarjonta/get-hakukohde tarjonta-service hakukohde-oid)]
    (fetch-form-by-haku-oid-str-cached form-by-haku-oid-str-cache
                                       (:haku-oid hakukohde)
                                       roles)))

(def form-coercer (sc/coercer! form-schema/FormWithContentAndTarjontaMetadata
                               coerce/json-schema-coercion-matcher))

(defrecord FormByHakuOidStrCacheLoader [form-by-id-cache
                                        koodisto-cache
                                        ohjausparametrit-service
                                        organization-service
                                        tarjonta-service]
  cache/CacheLoader
  (load [_ key]
    (let [[haku-oid aips? & roles] (clojure.string/split key #"#")] ;; TODO remove aips? with care, keys linger in Redis
      (when-let [form (fetch-form-by-haku-oid form-by-id-cache
                                              tarjonta-service
                                              koodisto-cache
                                              organization-service
                                              ohjausparametrit-service
                                              haku-oid
                                              false
                                              {}
                                              (map keyword roles))]
        (json/generate-string (form-coercer form)))))
  (load-many [this keys]
    (into {} (keep #(when-let [v (cache/load this %)] [% v]) keys)))
  (load-many-size [_] 1)
  (check-schema [_ _] nil))
