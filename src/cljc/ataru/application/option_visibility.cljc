(ns ataru.application.option-visibility
  (:require [clojure.string :as string]
            [ataru.number :as number]))

(declare answer-values)

(defn non-blank-answer-satisfies-condition? [value option]
  (and (not (string/blank? value))
       (if-let [condition (:condition option)]
         (let [operator (case (:comparison-operator condition)
                          "<" <
                          "=" =
                          ">" >)]
           (operator (number/->int value) (:answer-compared-to condition)))
         true)))

(defn- non-blank-answer-with-option-condition-satisfied-checker [value]
  (fn [option]
    (boolean
      (some #(non-blank-answer-satisfies-condition? % option)
            (answer-values value)))))

(defn- answer-values [value]
  (cond (and (vector? value) (or (vector? (first value)) (nil? (first value))))
        (set (mapcat identity value))
        (vector? value)
        (set value)
        :else
        #{value}))

(defn- selected-option-checker [value]
  (let [values (answer-values value)]
    (fn selected? [option]
      (contains? values (:value option)))))

(defn visibility-checker [field-descriptor answer-value]
  (cond
    (#{"dropdown" "multipleChoice" "singleChoice"} (:fieldType field-descriptor))
    (selected-option-checker answer-value)

    (= "textField" (:fieldType field-descriptor))
    (non-blank-answer-with-option-condition-satisfied-checker answer-value)

    :else
    (constantly true)))
