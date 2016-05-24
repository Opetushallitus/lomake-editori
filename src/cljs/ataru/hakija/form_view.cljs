(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]))

(defn text-field [content]
  [:div.application__form-field
   [:label.application_form-field-label (-> content :label :fi)]
   [:input.application__form-text-input {:type "text"}]])

(declare render-field)

(defn wrapper-field [children]
  (into [:div.application__wrapper-element [:h2.application__wrapper-heading "Lomakeosio"]] (mapv render-field children)))

(defn render-field
  [content]
   (match [content]
          [{:fieldClass "wrapperElement"
            :children   children}] [wrapper-field children]
          [{:fieldClass "formField" :fieldType "textField"}] [text-field content]))

(defn render-fields [form-data]
  (if form-data
    (mapv render-field (:content form-data))
    nil))

(defn application-header []
  [:h1.application__header "Lomakkeen nimi"])

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    (fn [] (into [:div.application__form-content-area [application-header]] (render-fields @form)))))

(defn form-view []
  [:div
   [banner]
   [application-contents]])
