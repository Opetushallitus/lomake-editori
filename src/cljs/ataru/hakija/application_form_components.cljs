(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [cemerick.url :as url]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       textual-field-value
                                                                       scroll-to-anchor]]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(declare render-field)

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- non-blank-val [val default]
  (if-not (clojure.string/blank? val)
    val
    default))

(defn- field-value-valid?
  [field-data value]
  (if (not-empty (:validators field-data))
    (every? true? (map #(validator/validate % value)
                    (:validators field-data)))
    true))

(defn- textual-field-change [text-field-data evt]
  (let [value  (-> evt .-target .-value)
        valid? (field-value-valid? text-field-data value)]
    (do
      ; dispatch-sync because we really really want the value to change NOW. Is a minor UI speed boost.
      (dispatch-sync [:application/set-application-field (answer-key text-field-data) {:value value :valid valid?}])
      (when-let [rules (not-empty (:rules text-field-data))]
        (dispatch [:application/run-rule rules])))))

(defn- init-dropdown-value
  [dropdown-data lang secret this]
  (let [select (-> (r/dom-node this) (.querySelector "select"))
        value  (or (first
                     (eduction
                       (comp (filter :default-value)
                         (map :value))
                       (:options dropdown-data)))
                 (-> select .-value))
        valid? (field-value-valid? dropdown-data value)]
    (if-not (some? secret)
      (dispatch [:application/set-application-field (answer-key dropdown-data) {:value value :valid valid?}]))
    (when-let [rules (not-empty (:rules dropdown-data))]
      (dispatch [:application/run-rule rules]))))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(defn- label [field-descriptor]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [field-descriptor]
      (let [label (non-blank-val (get-in field-descriptor [:label @lang])
                                 (get-in field-descriptor [:label @default-lang]))]
        [:label.application__form-field-label
         [:span (str label (required-hint field-descriptor))]
         [scroll-to-anchor field-descriptor]]))))

(defn- show-text-field-error-class?
  [field-descriptor value valid?]
  (and
    (not valid?)
    (some #(= % "required") (:validators field-descriptor))
    (validator/validate "required" value)))

(defn- link-detected-paragraph [text]
  (let [words   (for [word (clojure.string/split text #"\s")
                      :let [as-url (url/url word)]]
                  (if (not-empty (:host as-url))
                    [:a {:key  (str as-url)
                         :href (str as-url)}
                     (:host as-url)]
                    (if (empty? word)
                      [:br]
                      word)))
        reducer (fn [acc word-or-url]
                  (if (string? word-or-url)
                    (update acc :words str
                      (str word-or-url " "))
                    (->
                      (update acc :result concat [(:words acc) word-or-url " "])
                      (dissoc :words))))]
    (vec
      (cons :p.no-margin
        (->
          (reduce reducer {} words)
          (as-> reduction
              [(:result reduction) (:words reduction)]))))))


(defn info-text [field-descriptor]
  (let [language (subscribe [:application/form-language])]
    (fn [field-descriptor]
      (when-let [info (@language (some-> field-descriptor :params :info-text :label))]
        [:div.application__form-info-text [link-detected-paragraph info]]))))

(defn text-field [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [id           (keyword (:id field-descriptor))
        value        (subscribe [:state-query [:application :answers id :value]])
        valid?       (subscribe [:state-query [:application :answers id :valid]])
        lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        size-class (text-field-size->class (get-in field-descriptor [:params :size]))]
    (fn [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
      [div-kwd
       [label field-descriptor]
       [:div.application__form-text-input-info-text
        [info-text field-descriptor]]
       [:input.application__form-text-input
        (merge {:id          id
                :type        "text"
                :placeholder (when-let [input-hint (-> field-descriptor :params :placeholder)]
                               (non-blank-val (get input-hint @lang)
                                              (get input-hint @default-lang)))
                :class       (str size-class (if (show-text-field-error-class? field-descriptor @value @valid?)
                                               " application__form-field-error"
                                               " application__form-text-input--normal"))
                :value       @value
                :on-change   (partial textual-field-change field-descriptor)}
          (when disabled {:disabled true}))]])))

(defn repeatable-text-field [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [id         (keyword (:id field-descriptor))
        values     (subscribe [:state-query [:application :answers id :values]])
        size-class (text-field-size->class (get-in field-descriptor [:params :size]))
        lang       (subscribe [:application/form-language])
        on-change  (fn [idx evt]
                     (let [value (some-> evt .-target .-value)
                           valid (field-value-valid? field-descriptor value)]
                       (dispatch [:application/set-repeatable-application-field field-descriptor id idx {:value value :valid valid}])))]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      (into  [div-kwd
              [label field-descriptor]
              [:div.application__form-text-input-info-text
               [info-text field-descriptor]]]
        (cons
          (let [{:keys [value valid]} (first @values)]
            [:div
             [:input.application__form-text-input
              {:type      "text"
               :class     (str size-class (if (show-text-field-error-class? field-descriptor value valid)
                                            " application__form-field-error"
                                            " application__form-text-input--normal"))
               :value     value
               :on-blur   #(when (empty? (-> % .-target .-value))
                             (dispatch [:application/remove-repeatable-application-field-value id 0]))
               :on-change (partial on-change 0)}]])
          (map-indexed
            (let [first-is-empty? (empty? (first (map :value @values)))]
              (fn [idx {:keys [value last?]}]
                (let [clicky #(dispatch [:application/remove-repeatable-application-field-value id (inc idx)])]
                  [:div.application__form-repeatable-text-wrap
                   [:input.application__form-text-input
                    (merge
                      {:type      "text"
                       ; prevent adding second answer when first is empty
                       :disabled  (and last? first-is-empty?)
                       :class     (str
                                    size-class " application__form-text-input--normal"
                                    (when-not value " application__form-text-input--disabled"))
                       :value     value
                       :on-blur   #(when (and
                                           (not last?)
                                           (empty? (-> % .-target .-value)))
                                     (clicky))
                       :on-change (partial on-change (inc idx))}
                      (when last?
                        {:placeholder
                         (case @lang
                           :en "Add more..."
                           :sv "Lägg till..."
                           ;fi
                           "Lisää...")}))]
                   (when value
                     [:a.application__form-repeatable-text--addremove
                      {:on-click clicky}
                      [:i.zmdi.zmdi-close.zmdi-hc-lg]])])))
            (concat (rest @values)
              [{:value nil :valid true :last? true}])))))))

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
       [label field-descriptor]
       [:div.application__form-text-area-info-text
        [info-text field-descriptor]]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         ; default-value because IE11 will "flicker" on input fields. This has side-effect of NOT showing any
         ; dynamically made changes to the text-field value.
         :default-value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)
         :value (textual-field-value field-descriptor @application)}]])))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [field-descriptor children]
      (let [label (non-blank-val (get-in field-descriptor [:label @lang])
                                 (get-in field-descriptor [:label @default-lang]))]
        [:div.application__wrapper-element.application__wrapper-element--border
         [:div.application__wrapper-heading
          [:h2 label]
          [scroll-to-anchor field-descriptor]]
         (into [:div.application__wrapper-contents]
           (for [child children]
             [render-field child lang]))]))))

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
        ; flatten fields here because 'rowcontainer' may
        ; have nested fields because
        ; of validation (for example :one-of validator)
        (for [child (util/flatten-form-fields children)]
          [render-field child :div-kwd :div.application__row-field.application__form-field])))

(defn dropdown-followup [lang value field-descriptor]
  (let [prev (r/atom @value)
        resolve-followup (partial util/resolve-followup (:options field-descriptor))]
    (r/create-class
      {:component-did-update (fn []
                               (let [previous @prev]
                                 (when-not (= previous (reset! prev @value))
                                   (let [previous-followup (resolve-followup previous)
                                         current-followup  (resolve-followup @value)]
                                     (dispatch [:state-update
                                                (fn [db]
                                                  (->
                                                    (update-in db [:application :ui (answer-key previous-followup)] assoc :visible? false)
                                                    (update-in [:application :ui (answer-key current-followup)] assoc :visible? true)))])))))
       :reagent-render       (fn [lang value field-descriptor]
                               (when-let [followup (resolve-followup @value)]
                                 [:div.application__form-dropdown-followup
                                  [render-field followup]]))})))

(defn dropdown
  [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application  (subscribe [:state-query [:application]])
        lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        secret       (subscribe [:state-query [:application :secret]])
        value        (reaction
                       (or (->
                             (:answers @application)
                             (get (answer-key field-descriptor))
                             :value)
                         ""))]
    (r/create-class
      {:component-did-mount (partial init-dropdown-value field-descriptor @lang @secret)
       :reagent-render      (fn [field-descriptor]
                              (let [lang         @lang
                                    default-lang @default-lang]
                                [:div.application__form-field-wrapper
                                 [div-kwd
                                  [label field-descriptor]
                                  [:div.application__form-text-input-info-text
                                   [info-text field-descriptor]]
                                  [:div.application__form-select-wrapper
                                   [:span.application__form-select-arrow]
                                   [:select.application__form-select
                                    {:value @value
                                     :on-change (partial textual-field-change field-descriptor)}
                                    (concat
                                      (when
                                          (and
                                            (nil? (:koodisto-source field-descriptor))
                                            (not (:no-blank-option field-descriptor))
                                            (not= "" (:value (first (:options field-descriptor)))))
                                        [^{:key (str "blank-" (:id field-descriptor))} [:option {:value ""} ""]])
                                      (map-indexed
                                        (fn [idx option]
                                          (let [label        (non-blank-val (get-in option [:label lang])
                                                               (get-in option [:label default-lang]))
                                                option-value (:value option)]
                                            ^{:key idx}
                                            [:option {:value option-value} label]))
                                        (:options field-descriptor)))]]]

                                 [dropdown-followup lang value field-descriptor]]))})))

(defn- multiple-choice-option-checked? [options value]
  (true? (get options value)))

(defn multiple-choice
  [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [multiple-choice-id (answer-key field-descriptor)
        options            (subscribe [:state-query [:application :answers multiple-choice-id :options]])
        lang               (subscribe [:application/form-language])
        default-lang       (subscribe [:application/default-language])]
    (fn [field-descriptor]
      (let [options      @options
            lang         @lang
            default-lang @default-lang]
        [div-kwd
         [label field-descriptor]
         [:div.application__form-text-input-info-text
          [info-text field-descriptor]]
         [:div.application__form-outer-checkbox-container
          [:div ; prevents inner div items from reserving full space of the outer checkbox container
           (map (fn [option]
                  (let [label     (non-blank-val (get-in option [:label lang])
                                                 (get-in option [:label default-lang]))
                        value     (:value option)
                        option-id (util/component-id)]
                    [:div {:key option-id}
                     [:input.application__form-checkbox
                      {:id        option-id
                       :type      "checkbox"
                       :checked   (multiple-choice-option-checked? options value)
                       :value     value
                       :on-change (fn [event]
                                    (let [value (.. event -target -value)]
                                      (dispatch [:application/toggle-multiple-choice-option multiple-choice-id value (:validators field-descriptor)])))}]
                     [:label
                      {:for option-id}
                      label]]))
                (:options field-descriptor))]]]))))

(defn single-choice-button [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [button-id (answer-key field-descriptor)
        lang            (subscribe [:application/form-language])
        default-lang    (subscribe [:application/default-language])
        selected-value  (subscribe [:state-query [:application :answers button-id :value]])]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      (let [lang           @lang
            default-lang   @default-lang
            selected-value @selected-value]
        [div-kwd
         [label field-descriptor]
         [:div.application__form-single-choice-button-outer-container
          (map (fn [option]
                 (let [label        (non-blank-val (get-in option [:label lang])
                                                   (get-in option [:label default-lang]))
                       option-value (:value option)
                       option-id    (util/component-id)]
                   [:div.application__form-single-choice-button-inner-container {:key option-id}
                    [:input.application__form-single-choice-button
                     {:id        option-id
                      :type      "checkbox"
                      :checked   (= option-value selected-value)
                      :value     option-value
                      :on-change (fn [event]
                                   (let [value (.. event -target -value)]
                                     (dispatch [:application/select-single-choice-button button-id value (:validators field-descriptor)])))}]
                    [:label
                     {:for option-id}
                     label]]))
               (:options field-descriptor))]]))))

(defn info-element [field-descriptor]
  (let [language (subscribe [:application/form-language])
        header   (some-> (get-in field-descriptor [:label @language]))
        text     (some-> (get-in field-descriptor [:text @language]))]
    [:div.application__form-info-element.application__form-field
     (when (not-empty header)
       [:label.application__form-field-label [:span header]])
     [link-detected-paragraph text]]))

(defn spawn-children-based-on-answers [children answers]
  (let [child-ids        (map (comp keyword :id) children)
        adjacent         (select-keys answers child-ids)
        count-of-answers (reduce max
                           1 ; by default displays one row
                           (map (comp count :values second) adjacent))]
    (flatten
      (map-indexed
        (fn [index children]
          (map
            (fn [child]
              (let [id (keyword (:id child))]
                {:id          id
                 :value-index index
                 :child       child
                 :value       (get-in adjacent [id :values index :value])
                 :valid?      (boolean (get-in adjacent [id :values index :valid]))}))
            children))
        (repeat count-of-answers children)))))

(defn adjacent-text-fields [{:keys [children] :as field-descriptor}]
  (let [language              (subscribe [:application/form-language])
        default-lang          (subscribe [:application/default-language])
        header                (some-> (get-in field-descriptor [:label @language]))
        info-text             (some-> (get-in field-descriptor [:params :info-text :label @language]))
        repeatable?           (-> field-descriptor :params :repeatable)
        answers               (subscribe [:state-query [:application :answers]])
        adjacent-field-change (fn [child id idx event]
                                (let [value  (some-> event .-target .-value)
                                      valid? (field-value-valid? child value)]
                                  (dispatch [:application/set-adjacent-field-answer
                                             child
                                             id
                                             idx
                                             {:value value :valid valid?}])))]
    (fn [{:keys [children] :as field-descriptor}]
      [:div.application__form-field
       [label field-descriptor]
       (when-let [info (@language (some-> field-descriptor :params :info-text :label))]
         [:div.application__form-info-text [link-detected-paragraph info]])
       [:div
        (doall (for [[rowcount row]
                     (->> (spawn-children-based-on-answers children @answers)
                          ; counter
                          (map vector (range))
                          ; children grouped into a row
                          (partition (count children))
                          ; rowcount
                          (map vector (range)))]
                 (into
                   ^{:key (->> row second second (select-keys [:value-index :id]) (apply str "-" rowcount))}
                   [:div.application__form-adjacent-text-fields-wrapper
                    (for [[counter {:keys [id value-index child value valid?]}] row
                          :let  [fid (str value-index "-" (:id child))]]
                      ^{:key fid}
                      [:div.application__form-adjacent-row
                       (when (-> counter (< (count children)))
                         [label child])
                       [:input.application__form-text-input
                        {:id        fid
                         :type      "text"
                         :class     (match [valid? ((set (:validators child)) "required")]
                                      [false (_ :guard some?)]
                                      " application__form-field-error"
                                      :else
                                      " application__form-text-input--normal")
                         :value     value
                         :on-change (partial adjacent-field-change child id value-index)}]])
                    (when (pos? rowcount)
                      [:a {:on-click (fn [evt]
                                       (.preventDefault evt)
                                       (dispatch [:application/remove-adjacent-field field-descriptor (first (map (comp :value-index second) row))]))}
                       [:i.zmdi.zmdi-close.zmdi-hc-lg]])])))]
       [:a {:on-click (fn [evt]
                        (.preventDefault evt)
                        (dispatch [:application/add-adjacent-fields field-descriptor]))}
        [:i.zmdi.zmdi-plus-square] " Lisää"]])))

(defn render-field
  [field-descriptor & args]
  (let [ui       (subscribe [:state-query [:application :ui]])
        visible? (fn [id]
                   (get-in @ui [(keyword id) :visible?] true))]
    (fn [field-descriptor & args]
      (let [disabled? (get-in @ui [(keyword (:id field-descriptor)) :disabled?] false)]
        (cond-> (match field-descriptor
                       {:fieldClass "wrapperElement"
                        :fieldType  "fieldset"
                        :children   children} [wrapper-field field-descriptor children]
                       {:fieldClass "wrapperElement"
                        :fieldType  "rowcontainer"
                        :children   children} [row-wrapper children]
                       {:fieldClass "formField"
                        :id         (_ :guard (complement visible?))} [:div]
                       {:fieldClass "formField" :fieldType "textField" :params {:repeatable true}} [repeatable-text-field field-descriptor]
                       {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor :disabled disabled?]
                       {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
                       {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor]
                       {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor]
                       {:fieldClass "formField" :fieldType "singleChoice"} [single-choice-button field-descriptor]
                       {:fieldClass "infoElement"} [info-element field-descriptor]
                       {:fieldClass "wrapperElement" :fieldType "adjacentfieldset"} [adjacent-text-fields field-descriptor])
                (and (empty? (:children field-descriptor))
                     (visible? (:id field-descriptor))) (into args))))))

(defn editable-fields [form-data]
  (when form-data
    (into [:div] (for [content (:content form-data)]
                   [render-field content]))))
