(ns ataru.component-data.base-education-module
  (:require [ataru.util :as u]
            [ataru.translations.texts :refer [base-education-module-texts general-texts]]
            [ataru.component-data.component :as component]))

(defn module [metadata]
  (merge (component/form-section metadata)
         {:label    (:title base-education-module-texts)
          :children [{:id         "completed-base-education"
                      :label      (:completed-education base-education-module-texts)
                      :params     {}
                      :metadata   metadata
                      :options    [{:label     (:higher-education-qualification base-education-module-texts)
                                    :value     "Högskoleexamen som avlagts i Finland"
                                    :followups [{:id         "higher-education-qualification-in-finland"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :metadata   metadata
                                                 :children   [{:id              "higher-education-qualification-in-finland-level"
                                                               :label           (:qualification-level base-education-module-texts)
                                                               :params          {}
                                                               :metadata        metadata
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :validators      ["required"]
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         "higher-education-qualification-in-finland-year-and-date"
                                                               :label      (:completion-year-and-date base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              "higher-education-qualification-in-finland-qualification"
                                                               :label           (:qualification base-education-module-texts)
                                                               :params          {}
                                                               :metadata        metadata
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :validators      ["required"]
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "tutkinto", :title "Tutkinto", :version 2}}
                                                              {:id         "higher-education-qualification-in-finland-institution"
                                                               :label      (:institution base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     (:studies-required base-education-module-texts)
                                    :value     "Studier som högskolan kräver vid en öppen högskola"
                                    :followups [{:id         "studies-required-by-higher-education"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :metadata   metadata
                                                 :children   [{:id         "studies-required-by-higher-education-field"
                                                               :label      (:field base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-study-module"
                                                               :label      (:module base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-scope"
                                                               :label      (:scope base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-institution"
                                                               :label      (:institution base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     (:higher-education-outside-finland base-education-module-texts)
                                    :value     "Högskoleexamen som avlagts annanstans än i Finland"
                                    :followups [{:id         "higher-education-qualification-outside-finland"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :metadata   metadata
                                                 :children   [{:id              "higher-education-qualification-outside-finland-level"
                                                               :label           (:qualification-level base-education-module-texts)
                                                               :params          {}
                                                               :metadata        metadata
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :validators      ["required"]
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         "higher-education-qualification-outside-finland-year-and-date"
                                                               :label      (:completion-year-and-date base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "higher-education-qualification-outside-finland-qualification"
                                                               :label      (:qualification base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "higher-education-qualification-outside-finland-institution"
                                                               :label      (:institution base-education-module-texts)
                                                               :params     {}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              "higher-education-qualification-outside-finland-country"
                                                               :label           (:qualification-country base-education-module-texts)
                                                               :params          {}
                                                               :metadata        metadata
                                                               :validators      ["required"]
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 2}}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     (:other-eligibility base-education-module-texts)
                                    :value     "Övrig högskolebehörighet"
                                    :followups [{:id         "other-eligibility-question-group"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :metadata   metadata
                                                 :children   [{:id         "other-eligibility-year-of-completion"
                                                               :label      (:year-of-completion general-texts)
                                                               :params     {:size "S"}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "other-eligibility-description"
                                                               :label      (:describe-eligibility base-education-module-texts)
                                                               :params     {:size "M"}
                                                               :metadata   metadata
                                                               :validators ["required"]
                                                               :fieldType  "textArea"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}]
                      :fieldType  "multipleChoice"
                      :fieldClass "formField"
                      :validators ["required"]}
                     (merge (component/single-choice-button metadata)
                            {:label      (:have-you-completed base-education-module-texts)
                             :id         "upper-secondary-school-completed"
                             :params     {},
                             :options    [{:label     (:yes general-texts)
                                           :value     "Ja",
                                           :followups [{:id              "upper-secondary-school-completed-country",
                                                        :label           {:en "Choose country", :fi "Valitse suoritusmaa", :sv " Välj land"},
                                                        :params          {:info-text {:label (:choose-country base-education-module-texts)}},
                                                        :metadata        metadata
                                                        :options         [{:label {:fi "", :sv ""}, :value ""}],
                                                        :fieldType       "dropdown",
                                                        :fieldClass      "formField",
                                                        :validators      ["required"]
                                                        :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 2}}]}
                                          {:label (:have-not general-texts), :value "Nej"}],
                             :validators ["required"]})]}))

(def base-education-questions
  (->> (module {})
       :children
       u/flatten-form-fields
       (map (comp name :id))
       set))