(ns ataru.koodisto.koodisto
  (:require [ataru.util :as util]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]
            [clojure.core.cache :as cache]
            [ataru.cache.cache-service :as cache-service])
  (:import java.time.ZonedDateTime))

(defn encode-koodisto-key [{:keys [uri version]}]
  (str uri "#" version))

(defn- valid-koodi
  [valid-at koodi]
  (when-not (or (and (contains? (:valid koodi) :start)
                     (.isBefore valid-at (:start (:valid koodi))))
                (and (contains? (:valid koodi) :end)
                     (.isAfter valid-at (:end (:valid koodi)))))
    (cond-> koodi
            (contains? koodi :within)
            (update :within (partial keep (partial valid-koodi valid-at))))))

(defn get-koodisto-options
  [koodisto-cache uri version allow-invalid?]
  (cond->> (cache-service/get-from koodisto-cache (encode-koodisto-key {:uri uri :version version}))
           (not allow-invalid?)
           (keep (partial valid-koodi (ZonedDateTime/now)))))

(defn- populate-form-koodisto-field
  [koodisto-cache field]
  (if (and (:koodisto-source field)
           (or (= "multipleChoice" (:fieldType field))
               (= "dropdown" (:fieldType field))))
    (let [{:keys [uri version default-option]} (:koodisto-source field)
          empty-option                         {:value "" :label {:fi "" :sv "" :en ""}}
          koodis                               (map (fn [koodi] (select-keys koodi [:value :label]))
                                                    (get-koodisto-options koodisto-cache uri version (:allow-invalid? (:koodisto-source field))))
          koodis-with-default-option           (cond->> koodis
                                                        (some? default-option)
                                                        (map (fn [option]
                                                               (cond-> option
                                                                       (= default-option (-> option :label :fi))
                                                                       (assoc :default-value true)))))
          koodis-with-followups                (update-options-while-keeping-existing-followups koodis-with-default-option (:options field))]
      (assoc field :options (if (= (:fieldType field) "dropdown")
                              (->> koodis-with-followups
                                   (remove (comp (partial = "") :value))
                                   (cons empty-option))
                              koodis-with-followups)))
    field))

(defn populate-form-koodisto-fields
  [koodisto-cache form]
  (update form :content (partial util/map-form-fields
                                 (partial populate-form-koodisto-field
                                          koodisto-cache))))

(defn get-postal-office-by-postal-code
  [koodisto-cache postal-code]
  (->> (get-koodisto-options koodisto-cache "posti" 1 false)
       (filter #(= postal-code (:value %)))
       (first)))

(defn get-koulutustyypit
  [koodisto-cache]
  (get-koodisto-options koodisto-cache "koulutustyyppi" 2 false))

(defn all-koodisto-values
  [koodisto-cache uri version allow-invalid?]
  (->> (get-koodisto-options koodisto-cache uri version allow-invalid?)
       (map :value)
       (into #{})))
