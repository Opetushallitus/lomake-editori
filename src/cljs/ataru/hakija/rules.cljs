(ns ataru.hakija.rules
  (:require [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.pohjakoulutusristiriita :as pohjakoulutusristiriita]
            [ataru.preferred-name :as pn]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.date :as date]
            [clojure.string :as string])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- update-value [current-value update-fn]
  (if (vector? current-value)
    [(update-value (first current-value) update-fn)]
    (update-fn current-value)))

(defn- blank-value? [value]
  (or (and (string? value)
           (string/blank? value))
      (and (vector? value)
           (-> value first blank-value?))))

(defn- set-empty-validity
  [a cannot-view? valid?]
  (if (and (blank-value? (:value a))
           (not cannot-view?))
    (-> a
        (assoc :valid valid?)
        (update :values update-value #(assoc % :valid valid?)))
    a))

(defn- hide-field
  ([db id]
   (hide-field db id ""))
  ([db id value]
   (if-let [_ (some #(when (= id (keyword (:id %))) %)
                    (:flat-form-content db))]
     (as-> db db'
           (assoc-in db' [:application :ui id :visible?] false)
           (cond-> db'
                   (-> db' :application :answers id)
                   (update-in [:application :answers id]
                              (fn [answer]
                                (-> answer
                                    (assoc :valid true)
                                    (update :value update-value (constantly value))
                                    (update :values update-value (constantly {:value value :valid true})))))))
     db)))

(defn- show-field
  ([db id]
   (show-field db id false))
  ([db id valid?]
   (if-let [field (some #(when (= id (keyword (:id %))) %)
                        (:flat-form-content db))]
     (as-> db db'
           (assoc-in db' [:application :ui id :visible?] true)
           (let [cannot-view? (and (get-in db [:application :editing?])
                                   (:cannot-view field))]
             (cond-> db'
                     (-> db' :application :answers id)
                     (update-in [:application :answers id] set-empty-validity cannot-view? valid?))))
     db)))

(defn- have-finnish-ssn
  ^{:dependencies [:nationality]}
  [db]
  (let [values (get-in db [:application :answers :nationality :values])]
    (if (empty? (filter (fn [[v & _]] (= (:value v) finland-country-code)) values))
      (-> db
          (update-in [:application :answers :have-finnish-ssn]
                     (fn [a]
                       (if (= "" (:value a))
                         (-> a
                             (merge {:valid true
                                     :value "true"})
                             (update :values merge {:valid true
                                                    :value "true"}))
                         a)))
          (assoc-in [:application :ui :have-finnish-ssn :visible?] true))
      (hide-field db :have-finnish-ssn "true"))))

(defn- ssn
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (show-field db :ssn)
      (hide-field db :ssn))))

(defn- parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (nth ssn 6)
        day          (subs ssn 0 2)
        month        (subs ssn 2 4)
        year         (subs ssn 4 6)
        century      (case century-sign
                       "+" "18"
                       "-" "19"
                       "A" "20")]
    (str day "." month "." century year)))

(defn- parse-gender-from-ssn
  [ssn]
  (if (zero? (mod (js/parseInt (nth ssn 9)) 2))
    "2"                                                     ;; based on koodisto-values
    "1"))

(defn- birth-date-and-gender
  ^{:dependencies [:have-finnish-ssn :ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
        ssn              (get-in db [:application :answers :ssn])
        cannot-view?     (and (get-in db [:application :editing?])
                              (->> (:flat-form-content db)
                                   (filter #(= "ssn" (:id %)))
                                   first
                                   :cannot-view))]
    (if (= "true" have-finnish-ssn)
      (let [[birth-date gender] (cond (and (:valid ssn)
                                           (not-empty (:value ssn)))
                                      [(parse-birth-date-from-ssn (:value ssn))
                                       (parse-gender-from-ssn (:value ssn))]
                                      cannot-view?
                                      [(get-in db [:application :answers :birth-date :value])
                                       (get-in db [:application :answers :gender :value])]
                                      :else
                                      ["" ""])]
        (-> db
            (hide-field :birth-date birth-date)
            (hide-field :gender gender)))
      (-> db
          (show-field :birth-date)
          (show-field :gender)))))

(defn- birth-date
  ^{:dependencies [:have-finnish-ssn :ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
        ssn              (get-in db [:application :answers :ssn])
        cannot-view?     (and (get-in db [:application :editing?])
                              (->> (:flat-form-content db)
                                   (filter #(= "ssn" (:id %)))
                                   first
                                   :cannot-view))]
    (if (= "true" have-finnish-ssn)
      (let [birth-date (cond (and (:valid ssn)
                                  (not-empty (:value ssn)))
                             (parse-birth-date-from-ssn (:value ssn))
                             cannot-view?
                             (get-in db [:application :answers :birth-date :value])
                             :else
                             "")]
        (hide-field db :birth-date birth-date))
      (show-field db :birth-date))))

(defn- passport-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :passport-number)
      (show-field db :passport-number true))))

(defn- national-id-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :national-id-number)
      (show-field db :national-id-number true))))

(defn- birthplace
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :birthplace)
      (show-field db :birthplace))))

(defn- guardian-contact
  ^{:dependencies [:birth-date]}
  [db]
  (let [fields [:guardian-contact-information
                :guardian-contact-minor
                :guardian-contact-minor-secondary
                :guardian-name
                :guardian-phone
                :guardian-email
                :guardian-name-secondary
                :guardian-phone-secondary
                :guardian-email-secondary]
        editing? (get-in db [:application :editing?])
        minor? (if editing?
                 (get-in db [:application :person :minor])
                 (date/minor? (get-in db [:application :answers :birth-date :value])))
        all-empty (every? (fn [field]
                            (let [value (get-in db [:application :answers field :value])]
                              (cond
                                (vector? value) (every? string/blank? value)
                                :else (string/blank? value)))) fields)]
    (if (or (not all-empty) minor?)
      (reduce (fn [db' field] (show-field db' field true)) db fields)
      (reduce (fn [db' field] (hide-field db' field)) db fields))))

(defn- toggle-birth-date-based-fields [db]
  (guardian-contact db))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- update-gender-and-birth-date-based-on-ssn
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- toggle-ssn-based-fields
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- toggle-ssn-based-fields-without-gender
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date
      toggle-birth-date-based-fields))

(defn- postal-office
  ^{:dependencies [:country-of-residence :postal-code]}
  [db]
  (let [answers     (-> db :application :answers)
        country     (-> answers :country-of-residence :value)
        is-finland? (or (= country finland-country-code)
                        (string/blank? country))
        postal-code (-> answers :postal-code)
        auto-input? (and is-finland?
                         (= 5 (count (:value postal-code))))]
    (when auto-input?
      (ajax/http {:method        :get
                  :url           (str "/hakemus/api/postal-codes/" (:value postal-code))
                  :handler       [:application/handle-postal-code-input]
                  :error-handler [:application/handle-postal-code-error]}))
    (-> db
        (update-in [:application :answers :postal-office]
                   merge {:valid (not is-finland?)
                          :value ""})
        (update-in [:application :answers :postal-office :values]
                   merge {:valid (not is-finland?)
                          :value ""})
        (assoc-in [:application :ui :postal-office :visible?] is-finland?)
        (assoc-in [:application :ui :postal-office :disabled?] auto-input?))))

(defn- home-town-and-city
  ^{:dependencies [:country-of-residence]}
  [db]
  (let [country     (get-in db [:application :answers :country-of-residence :value])
        is-finland? (or (= country finland-country-code)
                        (string/blank? country))]
    (if is-finland?
      (-> db
          (show-field :home-town)
          (hide-field :city))
      (-> db
          (hide-field :home-town)
          (show-field :city)))))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (postal-office db))

(defn- prefill-preferred-first-name
  [db _]
  (let [answers        (-> db :application :answers)
        first-name     (-> answers :first-name :value (string/trim) (string/split #" ") first)
        preferred-name (-> answers :preferred-name :value)]
    (cond
      (and first-name (string/blank? preferred-name))
      (-> db
          (update-in [:application :answers :preferred-name] merge
                     {:value first-name
                      :valid true})
          (update-in [:application :answers :preferred-name :values] merge
                     {:valid true
                      :value first-name}))

      (and first-name (not (string/blank? preferred-name)))
      (-> db
          (update-in [:application :answers :preferred-name] merge
                     {:valid (pn/main-first-name? {:value preferred-name :answers-by-key answers})})
          (update-in [:application :answers :preferred-name :values] merge
                     {:valid (pn/main-first-name? {:value preferred-name :answers-by-key answers})}))

      :else db)))

(defn- change-country-of-residence
  [db _]
  (-> db
      home-town-and-city
      postal-office))

(defn- pohjakoulutusristiriita
  [db _]
  (if (empty? (pohjakoulutusristiriita/hakukohteet-wo-applicable-base-education db))
    (assoc-in db [:application :ui :pohjakoulutusristiriita :visible?] false)
    (assoc-in db [:application :ui :pohjakoulutusristiriita :visible?] true)))

(defn- swedish-nationality? [db]
  (-> db
      :application
      :answers
      :language
      :value
      (= "SV")))

(defn- arvosana-rivi-dropdown-without-answer? [db answer-key]
  (let [value (-> db
                  :application
                  :answers
                  answer-key
                  :value)]
    (and (vector? value)
         (-> value first vector?)
         (-> value first first (= "")))))

(defn- hide-kieli-oppiaine-row [db oppiaine]
  (let [arvosana  (keyword (str "arvosana-" (name oppiaine)))
        oppimaara (keyword (str "oppimaara-kieli-" (name oppiaine))) ; oppimäärä viittaa tässä varsinaiseen kieleen, jota opiskellaan
        hide      (fn hide [db]
                    (-> db
                        (hide-field oppiaine)
                        (hide-field arvosana)
                        (hide-field oppimaara)))]
    (cond-> db
            (and (arvosana-rivi-dropdown-without-answer? db arvosana)
                 (arvosana-rivi-dropdown-without-answer? db oppimaara))
            hide)))

(defn- toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
  [db _]
  (if (swedish-nationality? db)
    (-> db
        (show-field :A2)
        (show-field :oppimaara-kieli-A2)
        (show-field :arvosana-A2)
        (hide-kieli-oppiaine-row :B1))
    (-> db
        (show-field :B1)
        (show-field :oppimaara-kieli-B1)
        (show-field :arvosana-B1)
        (hide-kieli-oppiaine-row :A2))))

(defn- set-oppiaine-valinnainen-kieli-value
  [db _]
  (let [last-idx (-> db :application :ui :oppiaineen-arvosanat-valinnaiset-kielet :count dec)]
    (when (>= last-idx 0)
      (reduce (fn [db' answer-key]
                (update-in
                  db'
                  [:application :answers answer-key]
                  (fn [answer]
                    (as-> answer answer'
                          (update answer'
                                  :values
                                  (fn [values]
                                    (->> values
                                         (map-indexed
                                           (fn [values-idx values']
                                             (mapv (fn [value]
                                                     (let [oppiaineen-koodi               (some-> db :application :answers :oppiaine-valinnainen-kieli :values (get values-idx) first :value (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))
                                                           last-valinnainen-oppiaine-row? (= values-idx last-idx)
                                                           value-not-blank?               (-> value :value string/blank? not)
                                                           valid?                         (match [last-valinnainen-oppiaine-row? value-not-blank? oppiaineen-koodi answer-key]
                                                                                                 [true _ _ _]
                                                                                                 true

                                                                                                 [false false "a" :oppimaara-kieli-valinnainen-kieli]
                                                                                                 true

                                                                                                 [false false (_ :guard #(not= % "a")) :oppimaara-a-valinnainen-kieli]
                                                                                                 true

                                                                                                 [false true _ _]
                                                                                                 true

                                                                                                 :else
                                                                                                 false)]
                                                       (assoc value :valid valid?)))
                                                   values')))
                                         (into []))))
                          (assoc answer'
                                 :valid
                                 (every? (comp true? :valid first)
                                         (:values answer')))))))
              db
              [:oppimaara-a-valinnainen-kieli
               :oppimaara-kieli-valinnainen-kieli
               :oppiaine-valinnainen-kieli
               :arvosana-valinnainen-kieli]))))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :prefill-preferred-first-name
    prefill-preferred-first-name
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :update-gender-and-birth-date-based-on-ssn
    update-gender-and-birth-date-based-on-ssn
    :select-postal-office-based-on-postal-code
    select-postal-office-based-on-postal-code
    :toggle-ssn-based-fields
    toggle-ssn-based-fields
    :toggle-birthdate-based-fields
    toggle-birth-date-based-fields
    :toggle-ssn-based-fields-without-gender
    toggle-ssn-based-fields-without-gender
    :change-country-of-residence
    change-country-of-residence
    :pohjakoulutusristiriita
    pohjakoulutusristiriita
    :toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
    toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
    :set-oppiaine-valinnainen-kieli-value
    set-oppiaine-valinnainen-kieli-value))

(defn run-rules
  ([db rules]
   (run-rules db rules hakija-rule-to-fn))
  ([db rules rule-to-fn]
   {:pre  [(map? db) (or (empty? rules) (map? rules))]
    :post [map?]}
   (reduce-kv (fn [db rule arg]
                (or ((rule-to-fn rule) db arg) db))
              db
              rules)))

(defn run-all-rules
  [db flat-form-content]
  (->> flat-form-content
       (map :rules)
       (remove empty?)
       (reduce run-rules db)))

(defn run-pohjakoulutusristiriita-rule
  [db]
  (run-rules db {:pohjakoulutusristiriita nil}))