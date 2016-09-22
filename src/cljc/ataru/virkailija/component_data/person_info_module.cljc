  (ns ataru.virkailija.component-data.person-info-module
    (:require [ataru.virkailija.component-data.component :as component]
              [clojure.walk]))

; validators defined in ataru.hakija.application-validators

(defn ^:private text-field
  [labels & {:keys [size id validators rules] :or {size "M" validators [] rules {}}}]
  (-> (component/text-field)
      (assoc :rules rules)
      (assoc :label labels)
      (assoc :validators (conj validators :required))
      (assoc-in [:params :size] size)
      (assoc :id id)))

(defn ^:private first-name-component
  []
  (text-field {:fi "Etunimet" :sv "Förnamn" :en "First name"} :id :first-name))

(defn ^:private preferred-name-component
  []
  (text-field {:fi "Kutsumanimi" :sv "Smeknamn" :en "Middle name"} :size "S" :id :preferred-name))

(defn ^:private first-name-section
  []
  (component/row-section [(first-name-component)
                          (preferred-name-component)]))

(defn ^:private last-name-component
  []
  (text-field {:fi "Sukunimi" :sv "Efternamn" :en "Surname"} :id :last-name))

(defn ^:private dropdown-option
  [value labels & {:keys [default-value] :or {default-value false}}]
  (-> (component/dropdown-option)
      (merge {:value value :label labels}
             (when default-value
               {:default-value default-value}))
      (dissoc :focus?)))

(defn ^:private nationality-component
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Kansalaisuus" :sv "Nationalitet" :en "Nationality"}
              :validators [:required]
              :rules {:swap-ssn-birthdate-based-on-nationality [:ssn :birth-date]}
              :id :nationality
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- have-finnish-ssn-component
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Onko sinulla suomalainen henkilötunnus?"
                      :sv "Har du en finsk säkerhet personnummer?"
                      :en "Do you have a Finnish social security number?"}
              :rules {:toggle-ssn-based-fields :ssn}
              :no-blank-option true
              :exclude-from-answers true
              :id :have-finnish-ssn})
      (assoc :options [(dropdown-option "true" {:fi "Kyllä" :sv "Ja" :en "Yes"} :default-value true)
                       (dropdown-option "false" {:fi "Ei" :sv "Nej" :en "No"})])))

(defn ^:private ssn-component
  []
  (assoc (text-field {:fi "Henkilötunnus" :sv "Personnummer" :en "Social security number"} :size "S" :id :ssn)
         :rules {:update-gender-and-birth-date-based-on-ssn :gender}
         :validators [:ssn :required]))

(defn ^:private gender-section
  []
  (-> (component/dropdown)
      (merge (component/dropdown) {:label      {:fi "Sukupuoli" :sv "Kön" :en "Gender"}
                                   :validators [:required]
                                   :id         :gender})
      (update :options #(concat % [(dropdown-option "male" {:fi "Mies" :sv "Människa" :en "Male"})
                                   (dropdown-option "female" {:fi "Nainen" :sv "Kvinna" :en "Female"})]))))

(defn ^:private birthdate-and-gender-component
  []
  (component/row-section
    [(merge-with merge
                 (text-field
                   {:fi "Syntymäaika" :sv "Födelsedag" :en "Date of birth"}
                   :size "S"
                   :id :birth-date
                   :validators [:past-date :required])
                 {:params {:placeholder {:fi "pp.kk.vvvv"}}})
     (gender-section)]))

(defn ^:private email-component
  []
  (text-field {:fi "Sähköpostiosoite" :sv "E-postadress" :en "E-mail address"} :id :email :validators [:email]))

(defn ^:private phone-component
  []
  (text-field {:fi "Matkapuhelin" :sv "Mobiltelefonnummer" :en "Mobile phone number"} :id :phone :validators [:phone]))

(defn ^:private street-address-component
  []
  (text-field {:fi "Katuosoite" :sv "Adress" :en "Address"} :size "M" :id :address))

(defn ^:private home-town-component
  []
  (text-field {:fi "Kotikunta" :sv "Bostadsort" :en "Home town"} :id :home-town))

(defn ^:private postal-code-component
  []
  (text-field
    {:fi "Postinumero" :sv "Postnummer" :en "Postal code"}
    :size "S"
    :id :postal-code
    :rules {:select-postal-office-based-on-postal-code :postal-office}
    :validators [:postal-code]))

(defn ^:private postal-office-component
  []
  (text-field {:fi "Postitoimipaikka" :sv "Postkontor" :en "Postal office"} :id :postal-office))

(defn ^:private postal-office-section
  []
  (component/row-section [(postal-code-component)
                          (postal-office-component)]))

(defn ^:private native-language-section
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Äidinkieli" :sv "Modersmål" :en "Native language"}
              :validators [:required]
              :id :language
              :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}})))

(defn person-info-module
  []
  (clojure.walk/prewalk
    (fn [x]
      (if (map? x)
        (dissoc x :focus?)
        x))
    (merge (component/form-section) {:label {:fi "Henkilötiedot"
                                             :sv "Personlig information"
                                             :en "Personal information"}
                                     :label-amendment {:fi "(Osio lisätään automaattisesti lomakkeelle)"
                                                       :sv "Partitionen automatiskt lägga formen"
                                                       :en "The section will be automatically added to the application"}
                                     :children [(first-name-section)
                                                (last-name-component)
                                                (nationality-component)
                                                (have-finnish-ssn-component)
                                                (ssn-component)
                                                (birthdate-and-gender-component)
                                                (email-component)
                                                (phone-component)
                                                (street-address-component)
                                                (postal-office-section)
                                                (home-town-component)
                                                (native-language-section)]
                                     :module :person-info})))
