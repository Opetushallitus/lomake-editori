(ns ataru.hakija.application-validators
  (:require [clojure.set]
            [clojure.string]
            [ataru.email :as email]
            [ataru.ssn :as ssn]
            [ataru.translations.texts :as texts]
            [ataru.preferred-name :as pn]
            [ataru.number :refer [gte lte numeric-matcher]]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            #?(:clj  [clojure.core.async :as async]
               :cljs [cljs.core.async :as async])
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])))

(defn ^:private required?
  [{:keys [value]}]
  (if (string? value)
    (not (clojure.string/blank? value))
    (not (empty? value))))

(defn- required-valinnainen-oppimaara
  [params]
  (let [answer-key (-> params :field-descriptor :id keyword)
        last-idx   (-> params :answers-by-key :oppiaine-valinnainen-kieli :values count dec)]
    (->> params
         :answers-by-key
         answer-key
         :values
         count
         range
         (every? (fn [value-idx]
                   (or (= value-idx last-idx)
                       (-> params
                           :answers-by-key
                           :oppiaine-valinnainen-kieli
                           :values
                           (get value-idx)
                           first
                           :value
                           (not= "oppiaine-valinnainen-kieli-a"))
                       (required? params)))))))

(defn- required-hakija?
  [{:keys [virkailija?] :as params}]
  (or (required? params)
      virkailija?))

(defn- residence-in-finland?
  [answers-by-key]
  (= (str finland-country-code)
     (str (-> answers-by-key :country-of-residence :value))))

(defn- have-finnish-ssn?
  [answers-by-key]
  (or (= "true" (get-in answers-by-key [:have-finnish-ssn :value]))
      (ssn/ssn? (get-in answers-by-key [:ssn :value]))))

(defn- ssn?
  [{:keys [has-applied value answers-by-key field-descriptor]}]
  (let [multiple?      (get-in field-descriptor
                               [:params :can-submit-multiple-applications]
                               true)
        haku-oid       (get-in field-descriptor
                               [:params :haku-oid])
        preferred-name (:preferred-name answers-by-key)
        original-value (get-in answers-by-key [(keyword (:id field-descriptor)) :original-value])
        modifying?     (some? original-value)]
    (async/go
      (cond (not (ssn/ssn? value))
            [false []]
            (and (not multiple?)
                 (not (and modifying? (= value original-value)))
                 (async/<! (has-applied haku-oid {:ssn value})))
            [false [(texts/ssn-applied-error (when (:valid preferred-name)
                                               (:value preferred-name)))]]
            :else
            [true []]))))

(defn- email?
  [{:keys [has-applied answers-by-key field-descriptor]}]
  (let [multiple?      (get-in field-descriptor [:params :can-submit-multiple-applications] true)
        haku-oid       (get-in field-descriptor [:params :haku-oid])
        this-answer    (get answers-by-key (keyword (:id field-descriptor)))
        preferred-name (:preferred-name answers-by-key)
        original-value (:original-value this-answer)
        modifying?     (some? original-value)
        value          (:value this-answer)
        verify-value   (:verify this-answer)]
    (async/go
      (cond (or (not (email/email? value))
                (and verify-value
                     (not= verify-value value)))
            [false []]
            (and modifying? (= value original-value))
            [true []]
            (and (not multiple?)
                 (async/<! (has-applied haku-oid {:email value})))
            [false
             [((if modifying?
                 texts/email-applied-error-when-modifying
                 texts/email-applied-error) value (when (:valid preferred-name)
                                                    (:value preferred-name)))]]
            :else
            [true []]))))

(defn- email-simple?
  [{:keys [value]}]
  (if (clojure.string/blank? value)
    true
    (email/email? value)))

(defn- selection-limit?
  [{:keys [try-selection value field-descriptor]}]
  (let [id (:id field-descriptor)]
    (async/go
      (async/<! (try-selection id value)))))

(def ^:private postal-code-pattern #"^\d{5}$")

(defn ^:private postal-code?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (and (not (nil? value))
         (not (nil? (re-matches postal-code-pattern value))))
    (not (clojure.string/blank? value))))

(def ^:private whitespace-pattern #"\s*")
(def ^:private phone-pattern #"^\+?\d{4,}$")
(def ^:private finnish-date-pattern #"^\d{1,2}\.\d{1,2}\.\d{4}$")

(defn ^:private phone?
  [{:keys [value]}]
  (if-not (nil? value)
    (let [parsed (clojure.string/replace value whitespace-pattern "")]
      (not (nil? (re-matches phone-pattern parsed))))
    false))

#?(:clj
   (def parse-date
     (let [formatter (f/formatter "dd.MM.YYYY" (c/time-zone-for-id "Europe/Helsinki"))]
       (fn [d]
         (try
           (when (re-matches finnish-date-pattern d)
             (f/parse formatter d))
           (catch Exception _ nil)))))
   :cljs
   (def parse-date
     (let [formatter (f/formatter "d.M.YYYY")]
       (fn [d]
         ;; Unfortunately cljs-time.format allows
         ;; garbage data (e.g. XXXXX11Y11Z1997BLAH),
         ;; so we have to protect against that with a regexp
         (if (re-matches finnish-date-pattern d)
           (try (f/parse formatter d)
                (catch :default _ nil))
           nil)))))

(defn ^:private date?
  [value]
  (boolean
    (some->>
      value
      parse-date)))

(defn ^:private past-date?
  [{:keys [value]}]
  (boolean
    (and (date? value)
         (some-> (parse-date value)
                 (c/before? (c/today-at-midnight))))))

(defn- postal-office?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- birthplace?
  [{:keys [value answers-by-key]}]
  (if (have-finnish-ssn? answers-by-key)
    (clojure.string/blank? value)
    (not (clojure.string/blank? value))))

(defn- home-town?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- city?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    true
    (not (clojure.string/blank? value))))

(defn- partition-above-and-below
  [match? coll]
  (let [[above below] (split-with (complement match?) coll)]
    [(flatten above) (flatten (rest below))]))

(defn offending-priorization [hakukohde-oid selected priorisoivat-hakukohderyhmat]
  (let [priorities-above-and-below (->> priorisoivat-hakukohderyhmat
                                        (filter (fn [ryhma] (contains? (set (flatten (:prioriteetit ryhma))) hakukohde-oid)))
                                        (map :prioriteetit)
                                        (map
                                         #(partition-above-and-below (fn [j]
                                                                       (contains? (set j) hakukohde-oid)) %)))
        [hk-above hk-below] (partition-above-and-below (fn [h]
                                                         (= hakukohde-oid h)) selected)
        should-be-lower            (->> priorities-above-and-below
                                        (mapcat (fn [[_ below]]
                                                  (clojure.set/intersection (set below)
                                                                            (set hk-above)))))
        should-be-higher           (->> priorities-above-and-below
                                        (mapcat (fn [[above _]]
                                                  (clojure.set/intersection (set above)
                                                                            (set hk-below)))))]
    [should-be-lower should-be-higher]))

(defn limitting-hakukohderyhmat [tarjonta-hakukohteet rajaavat-hakukohderyhmat]
  (let [exceeds-limit? (fn [s hakukohderyhma-oid frequency]
                         (if (not-empty (->> rajaavat-hakukohderyhmat
                                             (filter #(= (:hakukohderyhma-oid %) hakukohderyhma-oid))
                                             (filter #(< (:raja %) frequency))))
                           (cons hakukohderyhma-oid s)
                           s))]
    (some->> tarjonta-hakukohteet
             (mapcat :hakukohderyhmat)
             (frequencies)
             (reduce-kv exceeds-limit? []))))

(defn- hakukohteet?
  [{:keys [value field-descriptor tarjonta-hakukohteet priorisoivat-hakukohderyhmat rajaavat-hakukohderyhmat]}]
  (let [hakukohde-options          (:options field-descriptor)
        num-answers                (count value)
        selected                   value
        selected-set               (set value)
        answers-subset-of-options? (clojure.set/subset? selected-set (set (map :value hakukohde-options)))
        limitting?                 (not-empty (limitting-hakukohderyhmat (->> tarjonta-hakukohteet
                                                                    (filter (fn [{:keys [oid]}] (contains? selected-set oid))))
                                                               rajaavat-hakukohderyhmat))
        offending?                 (first (filter #(seq (flatten (offending-priorization % selected priorisoivat-hakukohderyhmat)))
                                                  selected))]
    (cond
     limitting? false
     offending? false
     (pos? (count hakukohde-options)) (if-let [max-hakukohteet (-> field-descriptor :params :max-hakukohteet)]
                                        (and (< 0 num-answers (inc max-hakukohteet)) answers-subset-of-options?)
                                        (and (pos? num-answers) answers-subset-of-options?))
     :else true)))

(defn- numeric?
  [{:keys [value field-descriptor]}]
  (if (clojure.string/blank? value)
    true
    (let [[_ _ integer-part _ _ decimal-part] (re-matches numeric-matcher value)
          decimal-places                      (-> field-descriptor :params :decimals)
          min-value                           (-> field-descriptor :params :min-value)
          max-value                           (-> field-descriptor :params :max-value)]
      (cond (not integer-part)
            false

            (and decimal-part
                 (not decimal-places))
            false

            (and decimal-part
                 (> (count decimal-part)
                    decimal-places))
            false

            :else
            (and (or (nil? min-value)
                     (gte value min-value))
                 (or (nil? max-value)
                     (lte value max-value)))))))

(def pure-validators {:required                       required?
                      :required-valinnainen-oppimaara required-valinnainen-oppimaara
                      :required-hakija                required-hakija?
                      :postal-code                    postal-code?
                      :postal-office                  postal-office?
                      :phone                          phone?
                      :past-date                      past-date?
                      :main-first-name                pn/main-first-name?
                      :birthplace                     birthplace?
                      :home-town                      home-town?
                      :city                           city?
                      :hakukohteet                    hakukohteet?
                      :numeric                        numeric?
                      :email-simple                   email-simple?})

(def async-validators {:selection-limit selection-limit?
                       :ssn ssn?
                       :email email?})

(defn validate
  [{:keys [validator] :as params}]
  (if-let [pure-validator ((keyword validator) pure-validators)]
    (let [valid? (pure-validator params)]
      (async/go [valid? []]))
    (if-let [async-validator ((keyword validator) async-validators)]
      (async-validator params)
      (async/go [false []]))))
