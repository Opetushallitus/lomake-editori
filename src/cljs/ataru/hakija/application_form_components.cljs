(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                           required-hint
                                                           textual-field-value
                                                           scroll-to-anchor]]
            [ataru.hakija.application-validators :as validator]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- field-value-valid?
  [field-data value]
  (if (not-empty (:validators field-data))
    (every? true? (map #(validator/validate % value) (:validators field-data)))
    true))

(defn- textual-field-change [text-field-data evt]
  (let [value  (-> evt .-target .-value)
        valid? (field-value-valid? text-field-data value)]
    (do
      (dispatch [:application/set-application-field (answer-key text-field-data) {:value value :valid valid?}])
      (when-let [rules (not-empty (:rules text-field-data))]
        (dispatch [:application/run-rule rules])))))

(defn- init-dropdown-value
  [dropdown-data this]
  (let [select (-> (r/dom-node this) (.querySelector "select"))
        value  (or (first
                     (eduction
                       (comp (filter :default-value)
                             (map (comp :fi :label)))
                       (:options dropdown-data)))
                   (-> select .-value))
        valid  (field-value-valid? dropdown-data value)]
    (do
      (dispatch [:application/set-application-field (answer-key dropdown-data) {:value value :valid valid}])
      (when-let [rules (not-empty (:rules dropdown-data))]
        (dispatch [:application/run-rule rules])))))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(defn- label [field-descriptor & [size-class]]
  (let [id     (keyword (:id field-descriptor))
        valid? (subscribe [:state-query [:application :answers id :valid]])
        value  (subscribe [:state-query [:application :answers id :value]])]
    (fn [field-descriptor & [size-class]]
      [:label.application__form-field-label
       [:span (str (get-in field-descriptor [:label :fi]) (required-hint field-descriptor))]
       [scroll-to-anchor field-descriptor]
       (when (and
               (not @valid?)
               (some #(= % "required") (:validators field-descriptor))
               (validator/validate "required" @value))
         [:span.application__form-field-error "Tarkista muoto"])])))

(defn text-field [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])
        id (keyword (:id field-descriptor))
        valid? (subscribe [:state-query [:application :answers id :valid]])]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      (let [size-class (text-field-size->class (get-in field-descriptor [:params :size]))]
        [div-kwd
         [label field-descriptor size-class]
         [:input.application__form-text-input
          {:type      "text"
           :placeholder (when-let [input-hint (-> field-descriptor :params :placeholder)]
                          (:fi input-hint))
           :class     (str size-class (if @valid?
                                          " application__form-text-input--normal"
                                          " application__form-field-error"))
           :value     (textual-field-value field-descriptor @application)
           :on-change (partial textual-field-change field-descriptor)}]]))))

(defn- text-area-size->class [size]
  (match size
         "S" "application__form-text-area__size-small"
         "M" "application__form-text-area__size-medium"
         "L" "application__form-text-area__size-large"
         :else "application__form-text-area__size-medium"))

(defn text-area [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])]
    (fn [field-descriptor]
      [div-kwd
       [label field-descriptor "application__form-text-area"]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         :value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)}]])))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:div.application__wrapper-heading
    [:h2 (-> field-descriptor :label :fi)]
    [scroll-to-anchor field-descriptor]]
   (into [:div.application__wrapper-contents]
         (for [child children]
           [render-field child]))])

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
        (for [child children]
          [render-field child :div-kwd :div.application__row-field.application__form-field])))

(defn dropdown
  [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])]
    (r/create-class
      {:component-did-mount (partial init-dropdown-value field-descriptor)
       :reagent-render      (fn [field-descriptor]
                              [div-kwd
                               {:on-change (partial textual-field-change field-descriptor)}
                               [label field-descriptor "application__form-select-label"]
                               [:div.application__form-select-wrapper
                                [:span.application__form-select-arrow]
                                [:select.application__form-select
                                 {:value (textual-field-value field-descriptor @application)}
                                 (for [option (:options field-descriptor)]
                                   ^{:key (:value option)}
                                   [:option (get-in option [:label :fi])])]]])})))

(defn render-field
  [field-descriptor & args]
  (let [ui (subscribe [:state-query [:application :ui]])
        visible? (fn [id]
                   (get-in @ui [(keyword id) :visible?] true))]
    (fn [field-descriptor & args]
      (cond-> (match field-descriptor
                     {:fieldClass "wrapperElement"
                      :fieldType  "fieldset"
                      :children   children} [wrapper-field field-descriptor children]
                     {:fieldClass "wrapperElement"
                      :fieldType  "rowcontainer"
                      :children   children} [row-wrapper children]
                     {:fieldClass "formField"
                      :id (_ :guard (complement visible?))} [:div]

                     {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor]
                     {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
                     {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor])
        (and (empty? (:children field-descriptor))
             (visible? (:id field-descriptor))) (into args)))))

(defn editable-fields [form-data]
  (when form-data
    (into [:div] (for [content (:content form-data)]
                   [render-field content]))))
