(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])
            #?(:clj  [taoensso.timbre :refer [spy debug]]
               :cljs [taoensso.timbre :refer-macros [spy debug]])
            #?(:cljs [goog.string :as gstring])
            #?(:clj  [clj-time.core :as time]
               :cljs [cljs-time.core :as time])
            #?(:clj  [clj-time.coerce :refer [from-long]]
               :cljs [cljs-time.coerce :refer [from-long]]))
  (:import #?(:clj [java.util UUID])))

(defn gender-int-to-string [gender]
  (case gender
    "1" "mies"
    "2" "nainen"
    nil))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn group-by-first [kw m]
  (-> (group-by kw m)
      (map-kv first)))

(defn component-id []
  #?(:cljs (util/new-uuid)
     :clj  (str (UUID/randomUUID))))

(defn flatten-form-fields [fields]
  (flatten
    (for [field fields
          :when (not= "infoElement" (:fieldClass field))]
      (match
       field
       {:fieldClass (:or "wrapperElement" "questionGroup")
        :children   children}
       (flatten-form-fields children)

       {:fieldType (:or "dropdown" "multipleChoice" "singleChoice")
        :options   options}
       (cons field
         (->> options
              (mapcat :followups)
              flatten-form-fields
              (map #(assoc % :followup? true))))
       :else field))))

(defn form-fields-by-id [form]
  (->> form
       :content
       flatten-form-fields
       (group-by-first (comp keyword :id))))

(defn- get-readable-koodi-value
  [koodisto value]
  (-> (filter #(= value (:value %)) koodisto)
      first
      :label
      :fi
      (or "")))

(defn populate-answer-koodisto-values
  [values field get-koodisto-options]
  (if (:koodisto-source field)
    (let [koodisto (get-koodisto-options (-> field :koodisto-source :uri)
                                         (-> field :koodisto-source :version))]
      (cond
        (and (sequential? values)
             (every? sequential? values))
        (mapv (fn [value]
               (mapv #(get-readable-koodi-value koodisto %) value))
             values)

        (sequential? values)
        (mapv #(get-readable-koodi-value koodisto %) values)

        :else
        (get-readable-koodi-value koodisto values)))
    values))

(defn reduce-form-fields [f init [field & fs :as fields]]
  (if (empty? fields)
    init
    (recur f
           (f init field)
           (concat fs
                   (:children field)
                   (mapcat :followups (:options field))))))

(defn answers-by-key [answers]
  (group-by-first (comp keyword :key) answers))

(defn application-answers-by-key [application]
  (-> application :content :answers answers-by-key))

(defn group-answers-by-wrapperelement [wrapper-fields answers-by-key]
  (into {}
    (for [{:keys [id children] :as field} wrapper-fields
          :let [top-level-children children
                section-id id]]
      {id (loop [acc []
                 [{:keys [id children] :as field} & rest-of-fields] top-level-children]
            (if (not-empty children)
              (recur acc (concat children rest-of-fields))
              ; this is the ANSWER id, NOT section/wrapperElement id
              (if id
                (recur (conj acc
                         {(keyword id)
                          (get answers-by-key (keyword id))})
                  rest-of-fields)
                acc)))})))

(defn followups? [dropdown-options]
  (some some? (mapcat :followups dropdown-options)))

(defn resolve-followups [dropdown-options value]
  (and
    value
    (->> dropdown-options
        (filter (comp (partial = value) :value))
      (mapcat :followups))))

(def ^:private b-limit 1024)
(def ^:private kb-limit 102400)

(defn size-bytes->str [bytes]
  #?(:cljs (condp > bytes
             b-limit (str bytes "B")
             kb-limit (gstring/format "%.01fkB" (/ bytes 1024))
             (gstring/format "%.01fMB" (/ bytes 1024000)))
     :clj (condp > bytes
            b-limit (str bytes " B")
            kb-limit (format "%.2f kB" (float (/ bytes 1024)))
            (format "%.2f MB" (float (/ bytes 1024000))))))

(defn remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn in?
  [vec item]
  (some #(= item %) vec))

(defn not-blank? [s]
  (not (clojure.string/blank? s)))

(defn application-in-processing? [application-hakukohde-reviews]
  (some #(and (= "processing-state" (:requirement %))
              (not (contains? #{"unprocessed" "information-request"}
                              (:state %))))
        application-hakukohde-reviews))

(defn- seq-to-map
  [s key]
  (into
    {}
    (map (fn [x] [(keyword (key x)) x]))
    s))
