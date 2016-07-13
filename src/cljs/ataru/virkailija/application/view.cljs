(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])
        selected-key (subscribe [:state-query [:application :selected]])]
    (into [:div.application-list
           [:div.application-list__header.application-list__row
            [:span.application-list__row--applicant "Hakija"]
            [:span.application-list__row--time "Saapunut"]
            [:span.application-list__row--state "Tila"]]]
          (for [application @applications
                :let        [key       (:key application)
                             time      (t/time->str (:modified-time application))
                             applicant (:applicant application)
                             state     (:state application)]]
            [:div.application-list__row
             {:on-click #(dispatch [:application/select-application (:key application)])
              :class    (when (= @selected-key key)
                          "application-list__row--selected")}
             [:span.application-list__row--applicant
              (or applicant [:span.application-list__row--applicant-unknown "Tuntematon"])]
             [:span.application-list__row--time time]
             [:span.application-list__row--state
              (case (:state application)
                "received" "Saapunut"
                "Tuntematon")]]))))

(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div
     [:h1.application__name
      (:name @form)]
     ; Show when we have "henkilötiedot-moduuli", when we can rely on the applicant's name to be there
     ;[applications]
     ]))

(defn form-list-arrow-up [open?-atom]
  [:span.application-handling__form-list-arrow-up
   {:dangerouslySetInnerHTML {:__html "&#x2303;"}
    :on-click #(reset! open?-atom false)}])

(defn form-list-arrow-down [open?-atom]
  [:span.application-handling__form-list-arrow-down
   {:dangerouslySetInnerHTML {:__html "&#x2304;"}
    :on-click #(reset! open?-atom true)}])

(defn form-list-row [form selected?]
  [:div (:name form)])

(defn form-list-opened [forms selected-form-id open?-atom]
  (into [:div.application-handling__form-list-open [form-list-arrow-up open?-atom]]
        (for [[id form] forms
              :let [selected? (= id (:id selected-form-id))]]
          ^{:key id}
          [form-list-row form selected?])))

(defn form-list-closed [selected-form open?-atom]
  [:div.application-handling__form-list-closed
   {:on-click #(reset! open?-atom true)}
   (:name selected-form)
   [form-list-arrow-down open?-atom]])

(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])
        selected-form    (subscribe [:editor/selected-form])
        open?            (r/atom false)]
    (fn []
      (println "open? " @open?)
      (if @open?
        [form-list-opened @forms @selected-form-id open?]
        [form-list-closed @selected-form open?]))))

(defn application []
  [:div
   [:div.editor-form__container.panel-content
    [form-list]
    [:p [:a
         {:href
           (str
             "/lomake-editori/api/applications/"
             @(subscribe [:state-query [:application :form :id]])
             "/excel")}
         "Lataa kaikki hakemukset Excel -tiedostona"]]]])
