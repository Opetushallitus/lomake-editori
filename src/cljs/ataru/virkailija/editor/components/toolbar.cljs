(ns ataru.virkailija.editor.components.toolbar
  (:require [ataru.component-data.component :as component]
            [ataru.component-data.base-education-module :as base-education-module]
            [ataru.component-data.higher-education-base-education-module :as kk-base-education-module]
            [ataru.feature-config :as fc]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [ataru.component-data.arvosanat-module :as arvosanat]))

(defn- toolbar-elements []
  [[:form-section component/form-section]
   [:single-choice-button component/single-choice-button]
   [:single-choice-button-koodisto (fn [metadata]
                                     (assoc (component/single-choice-button metadata)
                                            :koodisto-source {:uri "" :title "" :version 1}
                                            :options []))]
   [:dropdown component/dropdown]
   [:dropdown-koodisto (fn [metadata]
                         (assoc (component/dropdown metadata)
                                :koodisto-source {:uri "" :title "" :version 1}
                                :options []))]
   [:multiple-choice component/multiple-choice]
   [:multiple-choice-koodisto (fn [metadata]
                                (assoc (component/multiple-choice metadata)
                                       :koodisto-source {:uri "" :title "" :version 1}
                                       :options []))]
   [:text-field component/text-field {:data-test-id "component-toolbar-tekstikenttä"}]
   [:text-area component/text-area]
   [:adjacent-fieldset component/adjacent-fieldset]
   [:attachment component/attachment]
   [:question-group component/question-group]
   [:info-element component/info-element]
   [:base-education-module base-education-module/module]
   [:kk-base-education-module kk-base-education-module/module]
   [:pohjakoulutusristiriita component/pohjakoulutusristiriita]
   [:lupa-sahkoiseen-asiointiin component/lupa-sahkoiseen-asiointiin]
   [:lupatiedot component/lupatiedot]
   [:guardian-contact-information component/huoltajan-yhteystiedot]])

(def followup-toolbar-element-names
  #{:text-field
    :text-area
    :single-choice-button
    :single-choice-button-koodisto
    :dropdown
    :dropdown-koodisto
    :multiple-choice
    :multiple-choice-koodisto
    :info-element
    :attachment
    :adjacent-fieldset
    :question-group})

(def question-group-toolbar-element-names
  #{:text-field
    :text-area
    :single-choice-button
    :single-choice-button-koodisto
    :dropdown
    :dropdown-koodisto
    :multiple-choice
    :multiple-choice-koodisto
    :info-element
    :attachment
    :adjacent-fieldset})

(defn- followup-toolbar-elements []
  (filter
    (fn [[el-name _]] (contains? followup-toolbar-element-names el-name))
    (toolbar-elements)))

(defn- question-group-toolbar-elements []
  (filter
    (fn [[el-name _]] (contains? question-group-toolbar-element-names el-name))
    (toolbar-elements)))

(def ^:private adjacent-fieldset-toolbar-elements
  {:text-field (comp (fn [text-field] (assoc text-field :params {:adjacent true}))
                    component/text-field)})

(defn- component-toolbar [_ _ _]
  (fn [path elements generator]
    (let [base-education-module-exists?   (subscribe [:editor/base-education-module-exists?])
          pohjakoulutusristiriita-exists? (subscribe [:editor/pohjakoulutusristiriita-exists?])]
      (into [:ul.form__add-component-toolbar--list]
            (for [[component-name generate-fn {:keys [data-test-id]}] elements
                  :when (and (not (and (vector? path)
                                       (= :children (second path))
                                       (= :form-section component-name)))
                             (not (and @base-education-module-exists?
                                       (contains? #{:base-education-module :kk-base-education-module} component-name)))
                             (not (and @pohjakoulutusristiriita-exists?
                                       (= :pohjakoulutusristiriita component-name))))]
              [:li.form__add-component-toolbar--list-item
               [:a {:on-click (fn [evt]
                                (.preventDefault evt)
                                (generator generate-fn))
                    :data-test-id data-test-id}
                @(subscribe [:editor/virkailija-translation component-name])]])))))


(defn custom-add-component [_ _ _]
  (let [mouse-over?  (r/atom false)
        form-locked? (subscribe [:editor/form-locked?])]
    (fn [toolbar path generator]
      [:div.editor-form__add-component-toolbar
       {:class          (when @form-locked? "disabled")
        :on-mouse-enter #(reset! mouse-over? true)
        :on-mouse-leave #(reset! mouse-over? false)
        :data-test-id   "component-toolbar"}
       (cond @form-locked?
             [:div.plus-component.plus-component--disabled [:span "+"]]
             @mouse-over?
             [component-toolbar path toolbar generator]
             :else
             [:div.plus-component [:span "+"]])])))

(defn add-component [path root-level-add-component?]
  (let [elements (cond-> (toolbar-elements)
                         (and root-level-add-component?
                              (fc/feature-enabled? :arvosanat))
                         (conj [:arvosanat-peruskoulu arvosanat/arvosanat-peruskoulu {:data-test-id "component-toolbar-arvosanat"}]
                               [:arvosanat-lukio arvosanat/arvosanat-lukio]))]
    [custom-add-component elements path
     (fn [generate-fn]
       (dispatch [:generate-component generate-fn path]))]))

(defn followup-toolbar [option-path generator]
  [custom-add-component (followup-toolbar-elements) option-path generator])

(defn question-group-toolbar [option-path generator]
  [custom-add-component (question-group-toolbar-elements) option-path generator])

(defn adjacent-fieldset-toolbar [path generator]
  [custom-add-component adjacent-fieldset-toolbar-elements path generator])
