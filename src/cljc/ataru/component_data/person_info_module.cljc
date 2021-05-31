(ns ataru.component-data.person-info-module
  (:require [ataru.component-data.component :as component]
            [ataru.util :as util]
            [ataru.translations.texts :refer [person-info-module-texts general-texts]]
            [clojure.walk]
            [com.rpl.specter :refer [select walker]]))

; validators defined in ataru.hakija.application-validators

(defn ^:private text-field
  [labels & {:keys [size id validators rules params blur-rules exclude-from-answers-if-hidden metadata] :or
                   {size "M" validators [:required] rules {} params {} blur-rules {} exclude-from-answers-if-hidden false}}]
  (-> (component/text-field metadata)
      (assoc :id id)
      (assoc :rules rules)
      (assoc :label labels)
      (assoc :validators validators)
      (assoc-in [:params :size] size)
      (cond-> (not (empty? params)) (assoc :params params))
      (cond-> (not (empty? blur-rules)) (assoc :blur-rules blur-rules))
      (cond-> exclude-from-answers-if-hidden (assoc :exclude-from-answers-if-hidden true))))

(defn ^:private first-name-component
  [metadata]
  (text-field (:forenames person-info-module-texts)
              :id :first-name
              :metadata metadata
              :blur-rules {:prefill-preferred-first-name :main-first-name}))

(defn ^:private preferred-name-component
  [metadata]
  (text-field (:main-forename person-info-module-texts)
              :size "S"
              :id :preferred-name
              :metadata metadata
              :validators [:required :main-first-name]))

(defn ^:private first-name-section
  [metadata]
  (component/row-section [(first-name-component metadata)
                          (preferred-name-component metadata)]
    metadata))

(defn ^:private last-name-component
  [metadata]
  (text-field (:surname person-info-module-texts) :id :last-name :metadata metadata))

(defn ^:private dropdown-option
  [value labels & {:keys [default-value] :or {default-value false}}]
  (-> (component/dropdown-option "0")
      (merge {:value value :label labels}
             (when default-value
               {:default-value default-value}))))

(defn ^:private nationality-component
  [metadata gender?]
  (-> (component/question-group metadata)
      (merge {:children [(merge (dissoc (component/dropdown metadata) :validators)
                                {:label           (:nationality person-info-module-texts)
                                 :options         []
                                 :id              :nationality
                                 :validators      [:required]
                                 :rules           (if gender?
                                                    {:toggle-ssn-based-fields nil}
                                                    {:toggle-ssn-based-fields-without-gender nil})
                                 :koodisto-source {:uri "maatjavaltiot2" :version 2 :default-option "Suomi"}})]})))

(defn- country-of-residence-component
  [metadata]
  (-> (dissoc (component/dropdown metadata) :options)
      (merge {:label (:country-of-residence person-info-module-texts)
              :validators [:required]
              :rules {:change-country-of-residence nil}
              :id :country-of-residence
              :koodisto-source {:uri "maatjavaltiot2" :version 2 :default-option "Suomi"}})))

(defn- have-finnish-ssn-component
  [metadata gender?]
  (-> (component/dropdown metadata)
      (merge {:label (:have-finnish-ssn person-info-module-texts)
              :rules (if gender?
                       {:toggle-ssn-based-fields nil}
                       {:toggle-ssn-based-fields-without-gender nil})
              :no-blank-option true
              :exclude-from-answers true
              :id :have-finnish-ssn})
      (assoc :options [(dropdown-option "true" (:yes general-texts) :default-value true)
                       (dropdown-option "false" (:no general-texts))])))

(defn ^:private ssn-component
  [metadata gender?]
  (assoc (text-field (:ssn person-info-module-texts) :size "S" :id :ssn :metadata metadata)
         :rules (if gender?
                  {:toggle-ssn-based-fields nil}
                  {:toggle-ssn-based-fields-without-gender nil})
         :validators [:ssn :required]))

(defn ^:private gender-section
  [metadata]
  (-> (dissoc (component/dropdown metadata) :options)
      (merge {:label           (:gender person-info-module-texts)
              :validators      [:required]
              :id              :gender
              :koodisto-source {:uri "sukupuoli" :version 1}})))

(defn ^:private birthdate-and-gender-component
  [metadata]
  (component/row-section
    [(merge-with merge
                 (text-field
                   (:birth-date person-info-module-texts)
                   :size "S"
                   :id :birth-date
                   :metadata metadata
                   :validators [:past-date :required])
                 {:params {:placeholder (:date-formats person-info-module-texts)}
                  :rules {:toggle-birthdate-based-fields nil}})
     (gender-section metadata)]
    metadata))

(defn ^:private birthdate-component
  [metadata]
  (component/row-section
    [(merge-with merge
       (text-field
         (:birth-date person-info-module-texts)
         :size "S"
         :id :birth-date
         :metadata metadata
         :validators [:past-date :required])
       {:params {:placeholder (:date-formats person-info-module-texts)}
        :rules {:toggle-birthdate-based-fields nil}})]
    metadata))

(defn- ssn-birthdate-gender-wrapper
  [metadata]
  (assoc
   (component/row-section
     [(ssn-component metadata true)
      (birthdate-and-gender-component metadata)]
     metadata)
   :child-validator :birthdate-and-gender-component))

(defn- ssn-birthdate-wrapper
  [metadata]
  (assoc
   (component/row-section
     [(ssn-component metadata false)
      (birthdate-component metadata)]
     metadata)
   :child-validator :ssn-or-birthdate-component))

(defn- passport-number
  [metadata]
  (text-field (:passport-number person-info-module-texts)
              :size "M" :id :passport-number  :validators [] :metadata metadata))

(defn- national-id-number
  [metadata]
  (text-field (:national-id-number person-info-module-texts)
              :size "M" :id :national-id-number :validators [] :metadata metadata))

(defn- birthplace
  [metadata]
  (text-field (:birthplace person-info-module-texts)
              :size "M" :id :birthplace :validators [:birthplace] :metadata metadata))

(defn ^:private email-component
  [metadata]
  (text-field (:email person-info-module-texts) :id :email :validators [:required :email] :metadata metadata))

(defn ^:private phone-component
  [metadata]
  (text-field (:phone person-info-module-texts) :id :phone :validators [:required :phone] :metadata metadata))

(defn ^:private street-address-component
  [metadata]
  (text-field (:address person-info-module-texts) :size "M" :id :address :metadata metadata))

(defn ^:private home-town-component
  [metadata]
  (merge (dissoc (component/dropdown metadata) :options)
         {:label (:home-town person-info-module-texts)
          :id :home-town
          :validators [:home-town]
          :koodisto-source {:uri "kunta" :version 1}
          :exclude-from-answers-if-hidden true}))

(defn- city-component
  [metadata]
  (text-field (:city person-info-module-texts)
              :id :city
              :metadata metadata
              :validators [:city]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-code-component
  [metadata]
  (text-field
    (:postal-code person-info-module-texts)
    :size "S"
    :id :postal-code
    :metadata metadata
    :rules {:select-postal-office-based-on-postal-code :postal-office}
    :validators [:postal-code]))

(defn ^:private postal-office-component
  [metadata]
  (text-field (:postal-office person-info-module-texts)
              :id :postal-office
              :metadata metadata
              :validators [:postal-office]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-office-section
  [metadata]
  (component/row-section [(postal-code-component metadata)
                          (postal-office-component metadata)]
    metadata))

(defn ^:private native-language-section
  [metadata]
  (-> (dissoc (component/dropdown metadata) :options)
      (merge {:label (:language person-info-module-texts)
              :validators [:required]
              :id :language
              :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}
              :rules {:toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet nil}})))

(defn onr-person-info-module [metadata]
  [(first-name-section metadata)
   (last-name-component metadata)
   (nationality-component metadata true)
   (have-finnish-ssn-component metadata true)
   (ssn-birthdate-gender-wrapper metadata)
   (birthplace metadata)
   (passport-number metadata)
   (national-id-number metadata)
   (email-component metadata)
   (phone-component metadata)
   (country-of-residence-component metadata)
   (street-address-component metadata)
   (postal-office-section metadata)
   (home-town-component metadata)
   (city-component metadata)
   (native-language-section metadata)])

(defn muu-person-info-module [metadata]
  [(first-name-section metadata)
   (last-name-component metadata)
   (nationality-component metadata false)
   (have-finnish-ssn-component metadata false)
   (ssn-birthdate-wrapper metadata)
   (email-component metadata)
   (phone-component metadata)
   (country-of-residence-component metadata)
   (street-address-component metadata)
   (postal-office-section metadata)
   (home-town-component metadata)
   (city-component metadata)])

(defn person-info-module
  ([]
   (person-info-module :onr))
  ([version]
  (let [metadata {:created-by  {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}
                  :modified-by {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}}]
    (merge (component/form-section metadata)
           {:label           (:label person-info-module-texts)
            :label-amendment (:label-amendment person-info-module-texts)
            :id              (name version)
            :children        (if (= version :muu)
                               (muu-person-info-module metadata)
                               (onr-person-info-module metadata))
            :module          :person-info}))))


(def person-info-questions
  (->> (person-info-module)
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(defn muu-person-info-module?
  [form]
  (->> (select (walker #(= (:module %) "person-info")) form)
       (first)
       :id
       (= "muu")))
