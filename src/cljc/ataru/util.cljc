(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])
            #?(:clj  [taoensso.timbre :refer [spy debug]]
               :cljs [taoensso.timbre :refer-macros [spy debug]]))
  (:import #?(:clj [java.util UUID])))

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
    (for [field fields]
      (match
        field
        {:fieldClass "wrapperElement"
         :children   children}
        (flatten-form-fields children)

        :else field))))

(defn answers-by-key [answers]
  (group-by-first (comp keyword :key) answers))

(defn group-answers-by-wrapperelement [wrapper-fields answers]
  (let [answers-by-key (answers-by-key answers)]
    (into {}
      (for [{:keys [id children] :as field} wrapper-fields
            :let [top-level-children children]]
        {id (map answers-by-key
              (loop [acc []
                    [{:keys [id children] :as field} & rest-of-fields] top-level-children]
                (if (not-empty children)
                  (recur acc children)
                  ; this is the ANSWER id, NOT wrapper id
                  (if id
                    (recur (conj acc id) (spy rest-of-fields))
                    acc))))}))))
