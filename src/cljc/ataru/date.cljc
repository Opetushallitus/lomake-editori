(ns ataru.date
  (:require #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])))

(def birthday-pattern #"^\d{2}.\d{2}.\d{4}$")
(def birthday-formatter (f/formatter "dd.MM.yyyy"))

(defn years-between? [date date' years]
  (let [[day month year] [(c/day date) (c/month date) (c/year date)]
        [day' month' year'] [(c/day date') (c/month date') (c/year date')]]
    (or
      (> (- year' year) years)
      (and (= years (- year' year))
           (> month' month))
      (and (= years (- year' year))
           (= month' month)
           (>= day' day)))))

(defn minor? [birth-date]
  (when (and birth-date (re-matches birthday-pattern birth-date))
    (let [today (c/today)
          born  (f/parse-local-date birthday-formatter birth-date)]
      (not (years-between? born today 18)))))
