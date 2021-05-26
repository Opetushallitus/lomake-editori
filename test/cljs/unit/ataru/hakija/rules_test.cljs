(ns ataru.hakija.rules-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.util :as util]
            [ataru.hakija.rules :as rules]))


(deftest rule-runner
  (let [rule-fn                 (fn [db argument]
                                    (is (= argument [:argument :a :b :c]))
                                    (is (= (:this-is-a-test-db db) :test-db))
                                    (assoc db :rule-fn-ran? true))
        rule-fn2                (fn [db argument]
                                    (is (= argument [:foo]))
                                    (assoc db :rule-fn2-ran? true))
        rule-to-fn              (fn [rule]
                                  (case rule
                                    :test-rule   rule-fn
                                    :test-rule-2 rule-fn2))
        {:keys [rule-fn-ran?
                rule-fn2-ran?]} (rules/run-rules {:this-is-a-test-db :test-db}
                                                 {:test-rule   [:argument :a :b :c]
                                                  :test-rule-2 [:foo]}
                                                 rule-to-fn)]
    (is (= true rule-fn-ran?))
    (is (= true rule-fn2-ran?))))

(def test-content
  [{:fieldClass "wrapperElement",
  :id "f6d7e75d-a4cc-42ed-b8b9-5d0f7e8c637f",
  :fieldType "fieldset",
  :children
  [{:fieldClass "wrapperElement",
    :id "d52e9946-ffaa-4bc9-9526-cc57c04b7cb5",
    :fieldType "rowcontainer",
    :children
    [{:label {:fi "Kansalaisuus", :sv "Nationalitet"},
      :validators ["required"],
      :fieldClass "formField",
      :id "nationality",
      :rules {:swap-ssn-birthdate-based-on-nationality ["ssn" "birth-date"]},
      :params {},
      :options
      [{:value "", :label {:fi "", :sv ""}}
       {:value "fi", :label {:fi "Suomi", :sv "Finland"}}
       {:value "sv", :label {:fi "Ruotsi", :sv "Sverige"}}],
      :fieldType "dropdown"}
     {:label {:fi "Henkilötunnus", :sv "Personnummer"},
      :validators ["ssn" "required"],
      :fieldClass "formField",
      :id "ssn",
      :params {:size "S"},
      :fieldType "textField"}
     {:label {:fi "Syntymäaika", :sv "Födelsedag"},
      :validators ["past-date" "required"],
      :fieldClass "formField",
      :id "birth-date",
      :params {:size "S"},
      :fieldType "textField"}],
    :params {}}
   {:label {:fi "Sukupuoli", :sv "Kön"},
    :validators ["required"],
    :fieldClass "formField",
    :id "gender",
    :params {},
    :rules {:test-rule-for-test []},
    :options
    [{:value "", :label {:fi "", :sv ""}}
     {:value "1", :label {:fi "Mies", :sv "Människa"}}
     {:value "2", :label {:fi "Nainen", :sv "Kvinna"}}],
    :fieldType "dropdown"}
   {:fieldClass "wrapperElement",
    :id "fe860cb8-588f-41ab-a8d7-15f835d9aac5",
    :fieldType "rowcontainer",
    :children
    [{:label {:fi "Kotikunta", :sv "Bostadsort"},
      :validators ["required"],
      :fieldClass "formField",
      :id "municipality",
      :params {:size "M"},
      :rules {:test-rule-for-test-duplicate []},
      :fieldType "textField"}
     {:label {:fi "Postinumero", :sv "Postnummer"},
      :validators ["postal-code" "required"],
      :fieldClass "formField",
      :id "postal-code",
      :params {:size "S"},
      :rules {:test-rule-for-test-duplicate []},
      :fieldType "textField"}],
    :params {}}],
  :params {},
  :label {:fi "Henkilötiedot", :sv "Personlig information"},
  :module "person-info"}])

(deftest test-run-all-rules
  (let [expected [:swap-ssn-birthdate-based-on-nationality
                  :test-rule-for-test
                  :test-rule-for-test-duplicate
                  :test-rule-for-test-duplicate]]
    (with-redefs [rules/run-rules (fn [db rules]
                                    (update db :rules-that-have-ran conj rules))]
      (is (= expected
             (flatten
              (map keys
                   (:rules-that-have-ran
                    (rules/run-all-rules {:form                {:content test-content}
                                          :rules-that-have-ran []}
                                         (util/flatten-form-fields test-content))))))))))


