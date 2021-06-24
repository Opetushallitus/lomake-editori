(ns ataru.ssn
  (:require #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            [clojure.string :as string]))

(def ^:private ssn-pattern #"^(\d{2})(\d{2})(\d{2})([-|A])(\d{3})([0-9a-zA-Z])$")

(def ^:private check-chars {0  "0"
                            1  "1"
                            2  "2"
                            3  "3"
                            4  "4"
                            5  "5"
                            6  "6"
                            7  "7"
                            8  "8"
                            9  "9"
                            10 "A"
                            11 "B"
                            12 "C"
                            13 "D"
                            14 "E"
                            15 "F"
                            16 "H"
                            17 "J"
                            18 "K"
                            19 "L"
                            20 "M"
                            21 "N"
                            22 "P"
                            23 "R"
                            24 "S"
                            25 "T"
                            26 "U"
                            27 "V"
                            28 "W"
                            29 "X"
                            30 "Y"})

(defn- ->int [thestr]
  #?(:clj  (Integer/parseInt thestr)
     :cljs (js/parseInt thestr 10)))

(defn valid-year? [year century]
  {:pre [(integer? year)]}
  (let [current-year (c/year (c/now))]
    (and
      ; not (given year between 2000 and current-year)
      (not
        (and
          (= "-" century)
          (-> year (>= 2000))
          (-> year (<= current-year))))
      ; not (given century is A and year in future)
      (not
        (and
          (or (= "A" century) (= "a" century))
          (-> year (>= (+ current-year 1))))))))

(defn ssn?
  [value]
  (when-not (nil? value)
    (when-let [[_ day month year century individual check] (re-matches ssn-pattern value)]
      (let [check-str  (str day month year individual)
            check-num  (->int check-str)
            check-mod  (mod check-num 31)
            check-char (get check-chars check-mod)]
        (and
          (valid-year? (+ 2000 (->int year)) century)
          (= (clojure.string/upper-case check) check-char))))))

(defn- parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (subs ssn 6 7)
        day          (subs ssn 0 2)
        month        (subs ssn 2 4)
        year         (subs ssn 4 6)
        century      (case century-sign
                       "+" "18"
                       "-" "19"
                       "A" "20")]
    (str day "." month "." century year)))

(defn ssn->birth-date [ssn]
  (when-not (string/blank? ssn)
    (parse-birth-date-from-ssn ssn)))
