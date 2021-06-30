(ns ataru.hakija.application-form-components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common
             :as application-field
             :refer
             [answer-key
              required-hint
              scroll-to-anchor
              is-required-field?
              markdown-paragraph
              belongs-to-hakukohde-or-ryhma?]]
            [ataru.application.option-visibility :as option-visibility]
            [ataru.hakija.application-hakukohde-component :as hakukohde]
            [ataru.hakija.pohjakoulutusristiriita :as pohjakoulutusristiriita]
            [ataru.util :as util]
            [reagent.core :as r]
            [clojure.string :as string]
            [ataru.translations.translation-util :as tu]
            [ataru.feature-config :as fc]
            [ataru.hakija.components.form-field-label-component :as form-field-label-component]
            [ataru.hakija.components.dropdown-component :as dropdown-component]
            [ataru.hakija.components.generic-label-component :as generic-label-component]
            [ataru.hakija.components.info-text-component :as info-text-component]
            [ataru.hakija.components.question-hakukohde-names-component :as hakukohde-names-component]
            [ataru.hakija.arvosanat.arvosanat-render :as arvosanat]
            [ataru.hakija.render-generic-component :as generic-component]))

(defonce autocomplete-off "new-password")

(declare render-field)

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- email-verify-field-change [field-descriptor value verify-value]
  (dispatch [:application/set-email-verify-field field-descriptor value verify-value]))

(defn- textual-field-change [field-descriptor value]
  (dispatch [:application/set-repeatable-application-field field-descriptor nil nil value]))

(defn textual-field-blur
  [field-descriptor]
  (dispatch [:application/run-rules (:blur-rules field-descriptor)]))

(defn- multi-value-field-change [field-descriptor question-group-idx value]
  (dispatch [:application/set-repeatable-application-field field-descriptor question-group-idx nil value]))

(defn- validation-error
  [errors]
  (let [languages @(subscribe [:application/default-languages])]
    (when (not-empty errors)
      [:div.application__validation-error-dialog
       (doall
         (map-indexed (fn [idx error]
                        (with-meta (util/non-blank-val error languages)
                                   {:key (str "error-" idx)}))
                      errors))])))

(defn info-element [field-descriptor _]
  (let [languages              (subscribe [:application/default-languages])
        application-identifier (subscribe [:application/application-identifier])
        header                 (util/non-blank-val (:label field-descriptor) @languages)
        text                   (util/non-blank-val (:text field-descriptor) @languages)]
    [:div.application__form-info-element.application__form-field
     (when (not-empty header)
       [:label.application__form-field-label [:span header]])
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [hakukohde-names-component/question-hakukohde-names field-descriptor :info-for-hakukohde])
     [markdown-paragraph text (-> field-descriptor :params :info-text-collapse) @application-identifier]]))

(defn- event->value [handler]
  (fn [evt]
    (let [value (-> evt .-target .-value)]
      (handler value))))

(defn email-field [field-descriptor idx]
      (let [id            (keyword (:id field-descriptor))
            size          (get-in field-descriptor [:params :size])
            size-class    (text-field-size->class size)
            languages     @(subscribe [:application/default-languages])
            form-field-id (application-field/form-field-id field-descriptor idx)
            local-state   (r/atom {:focused? false :focused-verify? false :value nil :value-verify nil})
            lang          @(subscribe [:application/form-language])
            text-main     (tu/get-hakija-translation :email-info-text lang)
            text-verify   (tu/get-hakija-translation :verify-email lang)
            cannot-view?  @(subscribe [:application/cannot-view? id])]
           (fn [field-descriptor idx]
               (let [answer        @(subscribe [:application/answer id idx nil])
                     on-change     #(if idx
                                        (multi-value-field-change field-descriptor idx %)
                                        (textual-field-change field-descriptor %))
                     show-error?   @(subscribe [:application/show-validation-error-class? id idx nil])
                     value         (cond cannot-view?
                                         "***********"
                                         (:focused? @local-state)
                                         (:value @local-state)
                                         :else
                                         (:value answer))]
                    [:div.application__form-field
                     [form-field-label-component/form-field-label field-descriptor form-field-id]
                     [:div.application__form-info-element
                      [markdown-paragraph text-main false nil]]
                     [:input.application__form-text-input
                      (merge {:id            form-field-id
                              :type          "text"
                              :placeholder   (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                                       (util/non-blank-val input-hint languages))
                              :class         (str size-class
                                                  (if show-error?
                                                      " application__form-field-error"
                                                      " application__form-text-input--normal"))
                              :on-blur       (event->value (fn [value]
                                                               (swap! local-state assoc
                                                                      :focused? false)
                                                               (on-change (clojure.string/trim (or value "")))
                                                               (textual-field-blur field-descriptor)))
                              :on-change    (event->value (fn [value]
                                                              (swap! local-state assoc
                                                                   :focused? true
                                                                   :value value)
                                                              (on-change value)))
                              :required      (is-required-field? field-descriptor)
                              :aria-invalid  (not (:valid answer))
                              :autoComplete  autocomplete-off
                              :value         value
                              :default-value (if @(subscribe [:application/cannot-view? id])
                                                 "***********"
                                                 (:value answer))
                              :on-paste      (fn [event]
                                                 (.preventDefault event))
                              :data-test-id  "email-input"}
                             (when @(subscribe [:application/cannot-edit? id])
                                   {:disabled true}))]
                     [validation-error (:errors answer)]
                     (let [id           :verify-email
                           get-verify-value (fn [] (cond cannot-view?
                                                         "***********"
                                                         (:focused-verify? @local-state)
                                                         (:value-verify @local-state)
                                                         :else
                                                         (:verify answer)))]
                          [:div
                           [:label.application__form-field-label.label.application__form-field-label--verify-email
                            {:id  "application-form-field-label-verify-email"
                             :for id}
                            [:span (str text-verify (required-hint field-descriptor))]]
                           [:input.application__form-text-input
                            {:id           id
                             :type         "text"
                             :required     true
                             :on-blur      (fn [_]
                                             (swap! local-state assoc
                                                    :focused-verify? false)
                                             (email-verify-field-change field-descriptor (:value answer)
                                                                          (clojure.string/trim (or (get-verify-value) ""))))

                             :on-paste     (fn [event]
                                               (.preventDefault event))
                             :on-change    (event->value (fn [value]
                                                             (swap! local-state assoc
                                                                    :focused-verify? true
                                                                    :value-verify value)
                                                             (email-verify-field-change field-descriptor (:value answer) (get-verify-value))))
                             :value        (get-verify-value)
                             :class        (str size-class
                                                (if show-error?
                                                    " application__form-field-error"
                                                    " application__form-text-input--normal"))
                             :aria-invalid (not (:valid answer))
                             :autoComplete autocomplete-off
                             :data-test-id "verify-email-input"}]])]))))

(defn- options-satisfying-condition [field-descriptor answer-value options]
  (filter (option-visibility/visibility-checker field-descriptor answer-value) options))

(defn- get-visible-followups [field-descriptor answer-value options]
  (->> options
       (options-satisfying-condition field-descriptor answer-value)
       (map :followups)
       flatten
       (filterv #(deref (subscribe [:application/visible? (keyword (:id %))])))))

(defn- text-field-followups-container [field-descriptor options answer-value question-group-idx]
  (let [followups (get-visible-followups field-descriptor answer-value options)]
    (when (not-empty followups)
      (into [:div.application__form-multi-choice-followups-container.animated.fadeIn
             {:data-test-id "tekstikenttä-lisäkysymykset"}]
            (for [followup followups]
              ^{:key (:id followup)}
              [render-field followup question-group-idx])))))

(defn text-field [field-descriptor _]
  (let [id           (keyword (:id field-descriptor))
        size         (get-in field-descriptor [:params :size])
        size-class   (text-field-size->class size)
        languages    (subscribe [:application/default-languages])
        disabled?    (subscribe [:application/disabled? id])
        cannot-view? (subscribe [:application/cannot-view? id])
        cannot-edit? (subscribe [:application/cannot-edit? id])
        local-state  (r/atom {:focused? false :value nil})]
    (fn [field-descriptor idx]
      (let [languages        @languages
            disabled?        @disabled?
            {:keys [value
                    valid
                    errors]} @(subscribe [:application/answer id idx nil])
            options          @(subscribe [:application/visible-options field-descriptor])
            cannot-view?     @cannot-view?
            cannot-edit?     @cannot-edit?
            show-error?      @(subscribe [:application/show-validation-error-class? id idx nil])
            on-change        (if idx
                               (partial multi-value-field-change field-descriptor idx)
                               (partial textual-field-change field-descriptor))
            on-blur          (fn [_] (textual-field-blur field-descriptor))
            form-field-id    (application-field/form-field-id field-descriptor idx)
            data-test-id     (if (some #{id} [:first-name
                                              :preferred-name
                                              :last-name
                                              :ssn
                                              :phone
                                              :address
                                              :postal-code
                                              :postal-office])
                               (-> id
                                   name
                                   (str "-input"))
                               "tekstikenttä-input")]
        [:div.application__form-field
         [form-field-label-component/form-field-label field-descriptor form-field-id]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [hakukohde-names-component/question-hakukohde-names field-descriptor])
         [:div.application__form-text-input-info-text
          [info-text-component/info-text field-descriptor]]
         [:input.application__form-text-input
          (merge {:id           form-field-id
                  :type         "text"
                  :placeholder  (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                  (util/non-blank-val input-hint languages))
                  :class        (str size-class
                                     (if show-error?
                                       " application__form-field-error"
                                       " application__form-text-input--normal"))
                  :on-blur      (fn [evt]
                                  (swap! local-state assoc
                                         :focused? false)
                                  (on-blur evt))
                  :on-change    (event->value (fn [value]
                                                (swap! local-state assoc
                                                       :focused? true
                                                       :value value)
                                                (on-change value)))
                  :required     (is-required-field? field-descriptor)
                  :aria-invalid (not valid)
                  :tab-index    "0"
                  :autoComplete autocomplete-off
                  :value        (cond cannot-view?
                                      "***********"
                                      (:focused? @local-state)
                                      (:value @local-state)
                                      :else
                                      value)
                  :data-test-id data-test-id}
                 (when (or disabled? cannot-edit?)
                   {:disabled true}))]
         [validation-error errors]
         (when (not (or (string/blank? value)
                        show-error?))
           [text-field-followups-container field-descriptor options value idx])]))))

(defn- repeatable-text-field-row
  [{:keys [field-descriptor]}]
  (let [id           (keyword (:id field-descriptor))
        size-class   (text-field-size->class (get-in field-descriptor [:params :size]))
        cannot-edit? (subscribe [:application/cannot-edit? id])
        local-state  (r/atom {:focused? false :value nil})]
    (fn [{:keys [field-descriptor field-label-id last? question-group-idx repeatable-idx]}]
      (let [padded?                      (or (zero? repeatable-idx) last?)
            first-is-empty?              (empty? (:value @(subscribe [:application/answer id question-group-idx 0])))
            {:keys [value
                    valid]} @(subscribe [:application/answer id question-group-idx repeatable-idx])
            show-validation-error-class? @(subscribe [:application/show-validation-error-class? id question-group-idx repeatable-idx])
            cannot-edit?                 @cannot-edit?
            remove-field                 (fn [_]
                                           (swap! local-state assoc
                                                  :focused? false)
                                           (dispatch [:application/remove-repeatable-application-field-value
                                                      field-descriptor
                                                      question-group-idx
                                                      repeatable-idx]))
            on-blur                      (fn [evt]
                                           (let [value (-> evt .-target .-value)]
                                             (swap! local-state assoc
                                                    :focused? false)
                                             (when (and (empty? value) (not last?))
                                               (dispatch [:application/remove-repeatable-application-field-value
                                                          field-descriptor
                                                          question-group-idx
                                                          repeatable-idx]))))
            on-change                    (fn [evt]
                                           (let [value (-> evt .-target .-value)]
                                             (swap! local-state assoc
                                                    :focused? true
                                                    :value value)
                                             (dispatch [:application/set-repeatable-application-field
                                                        field-descriptor
                                                        question-group-idx
                                                        repeatable-idx
                                                        value])))
            lang                         @(subscribe [:application/form-language])]
        [:div.application__form-repeatable-text-wrap
         {:class (when padded? "application__form-repeatable-text-wrap--padded")}
         [:input.application__form-text-input
          {:type            "text"
           :class           (str size-class
                                 (if show-validation-error-class?
                                   " application__form-field-error"
                                   " application__form-text-input--normal")
                                 (when last?
                                   " application__form-text-input--disabled"))
           :value           (cond last?
                                  nil
                                  (:focused? @local-state)
                                  (:value @local-state)
                                  :else
                                  value)
           :placeholder     (when last? (tu/get-hakija-translation :add-more lang))
           :disabled        (or cannot-edit? (and last? first-is-empty?))
           :aria-invalid    (not valid)
           :aria-labelledby field-label-id
           :tab-index       "0"
           :autoComplete    autocomplete-off
           :on-blur         on-blur
           :on-change       on-change}]
         (when (and (not cannot-edit?) (not last?))
           [:a.application__form-repeatable-text--addremove
            [:i.zmdi.zmdi-close.zmdi-hc-lg
             {:on-click remove-field}]])]))))

(defn repeatable-text-field
  [field-descriptor idx]
  (let [id            (keyword (:id field-descriptor))
        cannot-edit?  @(subscribe [:application/cannot-edit? id])
        answer-count  @(subscribe [:application/repeatable-answer-count id idx])]
    [:div.application__form-field
     [generic-label-component/generic-label field-descriptor idx]
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [hakukohde-names-component/question-hakukohde-names field-descriptor])
     [:div.application__form-text-input-info-text
      [info-text-component/info-text field-descriptor]]
     (doall
       (map (fn [repeatable-idx]
              ^{:key (str id "-" repeatable-idx)}
              [repeatable-text-field-row
               {:field-descriptor   field-descriptor
                :field-label-id     (generic-label-component/id-for-label field-descriptor idx)
                :last?              (= repeatable-idx answer-count)
                :question-group-idx idx
                :repeatable-idx     repeatable-idx}])
            (range (if cannot-edit?
                     answer-count
                     (inc answer-count)))))]))

(defn- text-area-size->class [size]
  (match size
         "S" "application__form-text-area__size-small"
         "M" "application__form-text-area__size-medium"
         "L" "application__form-text-area__size-large"
         :else "application__form-text-area__size-medium"))

(defn- parse-max-length [field]
  (let [max-length (-> field :params :max-length)]
    (when-not (or (empty? max-length) (= "0" max-length))
      max-length)))

(defn text-area [field-descriptor _]
  (let [id           (keyword (:id field-descriptor))
        size         (-> field-descriptor :params :size)
        size-class   (text-area-size->class size)
        max-length   (parse-max-length field-descriptor)
        cannot-edit? (subscribe [:application/cannot-edit? id])
        local-state  (r/atom {:focused? false :value nil})]
    (fn [field-descriptor idx]
      (let [{:keys [value
                    valid]} @(subscribe [:application/answer id idx nil])
            form-field-id   (application-field/form-field-id field-descriptor idx)
            cannot-edit?    @cannot-edit?
            on-change       (if idx
                              (partial multi-value-field-change field-descriptor idx)
                              (partial textual-field-change field-descriptor))]
        [:div.application__form-field
         [form-field-label-component/form-field-label field-descriptor form-field-id]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [hakukohde-names-component/question-hakukohde-names field-descriptor])
         [:div.application__form-text-area-info-text
          [info-text-component/info-text field-descriptor]]
         [:textarea.application__form-text-input.application__form-text-area
          (merge {:id           form-field-id
                  :class        size-class
                  :maxLength    max-length
                  :value        (if (:focused? @local-state)
                                  (:value @local-state)
                                  value)
                  :on-blur      (fn [_]
                                  (swap! local-state assoc
                                         :focused? false))
                  :on-change    (event->value (fn [value]
                                                (swap! local-state assoc
                                                       :focused? true
                                                       :value value)
                                                (on-change value)))
                  :required     (is-required-field? field-descriptor)
                  :aria-invalid (not valid)
                  :tab-index    "0"}
                 (when cannot-edit?
                   {:disabled true}))]
         (when max-length
           [:span.application__form-textarea-max-length (str (count value) " / " max-length)])]))))

(defn wrapper-field
  [field-descriptor]
  (let [label (util/non-blank-val (:label field-descriptor) @(subscribe [:application/default-languages]))]
    [:div.application__wrapper-element
     [:div.application__wrapper-heading
      [:h2 label]
      [scroll-to-anchor field-descriptor]]
     (into [:div.application__wrapper-contents]
           (for [child (:children field-descriptor)
                 :when @(subscribe [:application/visible? (keyword (:id child))])]
             ^{:key (:id child)}
             [render-field child nil]))]))

(defn- remove-question-group-button [field-descriptor idx]
  (let [mouse-over?   (subscribe [:application/mouse-over-remove-question-group-button
                                  field-descriptor
                                  idx])
        on-mouse-over (fn [_]
                        (dispatch [:application/remove-question-group-mouse-over
                                   field-descriptor
                                   idx]))
        on-mouse-out  (fn [_]
                        (dispatch [:application/remove-question-group-mouse-out
                                   field-descriptor
                                   idx]))
        on-click      (fn [_]
                        (dispatch [:application/remove-question-group-row
                                   field-descriptor
                                   idx]))]
    (fn [_ _]
      [(if @mouse-over?
         :i.zmdi.zmdi-close.application__remove-question-group-row.application__remove-question-group-row-mouse-over
         :i.zmdi.zmdi-close.application__remove-question-group-row)
       {:on-mouse-over on-mouse-over
        :on-mouse-out  on-mouse-out
        :on-click      on-click}])))

(defn- question-group-row [field-descriptor idx can-remove?]
  (let [mouse-over? (subscribe [:application/mouse-over-remove-question-group-button
                                field-descriptor
                                idx])]
    [(if @mouse-over?
       :div.application__question-group-row.application__question-group-row-mouse-over
       :div.application__question-group-row)
     (into [:div.application__question-group-row-content]
           (for [child (:children field-descriptor)
                 :when @(subscribe [:application/visible? (keyword (:id child))])]
             ^{:key (str (:id child) "-" idx)}
             [render-field child idx]))
     (when can-remove?
       [remove-question-group-button field-descriptor idx])]))

(defn question-group [field-descriptor _]
  (let [languages     (subscribe [:application/default-languages])
        label         (util/non-blank-val (:label field-descriptor) @languages)
        row-count     @(subscribe [:application/question-group-row-count (:id field-descriptor)])
        cannot-edits? (map #(subscribe [:application/cannot-edit? (keyword (:id %))])
                           (util/flatten-form-fields (:children field-descriptor)))
        lang          @(subscribe [:application/form-language])]
    [:div.application__question-group
     (when-not (string/blank? label)
       [:h3.application__question-group-heading label])
     [scroll-to-anchor field-descriptor]
     [:div
      (doall
       (for [idx (range (or row-count 1))]
         ^{:key (str "question-group-row-" idx)}
         [question-group-row
          field-descriptor
          idx
          (and (< 1 row-count) (not (some deref cannot-edits?)))]))]
     (when (not (some deref cannot-edits?))
       [:div.application__add-question-group-row
        [:a {:href     "#"
             :on-click (fn add-question-group-row [event]
                         (.preventDefault event)
                         (dispatch [:application/add-question-group-row field-descriptor]))}
         [:span.zmdi.zmdi-plus-circle.application__add-question-group-plus-sign]
         (tu/get-hakija-translation :add lang)]])]))

(defn row-wrapper [field-descriptor _]
  (into [:div.application__row-field-wrapper]
        (for [child (:children field-descriptor)
              :when @(subscribe [:application/visible? (keyword (:id child))])]
          ^{:key (:id child)}
          [render-field child nil])))

(defn- multi-choice-followups [followups question-group-idx]
  [:div.application__form-multi-choice-followups-outer-container
   [:div.application__form-multi-choice-followups-indicator]
   (into [:div.application__form-multi-choice-followups-container.animated.fadeIn]
         (for [followup followups]
           ^{:key (:id followup)}
           [render-field followup question-group-idx]))])

(defn- multiple-choice-option [field-descriptor option _ _]
  (let [languages    (subscribe [:application/default-languages])
        label        (util/non-blank-option-label option @languages)
        value        (:value option)
        option-id    (util/component-id)
        cannot-edit? (subscribe [:application/cannot-edit? (keyword (:id field-descriptor))])]
    (fn [field-descriptor option parent-id question-group-idx]
      (let [on-change (fn [_]
                        (dispatch [:application/toggle-multiple-choice-option field-descriptor question-group-idx option]))
            checked?  (subscribe [:application/multiple-choice-option-checked? parent-id value question-group-idx])
            followups (filterv #(deref (subscribe [:application/visible? (keyword (:id %))]))
                               (:followups option))]
        [:div {:key option-id}
         [:input.application__form-checkbox
          (merge {:id        option-id
                  :type      "checkbox"
                  :checked   @checked?
                  :value     value
                  :on-change on-change
                  :role      "option"}
                 (when @cannot-edit? {:disabled true}))]
         [:label
          (merge {:for option-id}
                 (when @cannot-edit? {:class "disabled"}))
          label]
         (when (and @checked?
                    (not-empty followups))
           (multi-choice-followups followups question-group-idx))]))))

(defn multiple-choice
  [field-descriptor _]
  (let [id        (answer-key field-descriptor)
        languages (subscribe [:application/default-languages])]
    (fn [field-descriptor idx]
      (let [options @(subscribe [:application/visible-options field-descriptor])]
        [:div.application__form-field
         [generic-label-component/generic-label field-descriptor idx]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [hakukohde-names-component/question-hakukohde-names field-descriptor])
         [:div.application__form-text-input-info-text
          [info-text-component/info-text field-descriptor]]
         [:div.application__form-outer-checkbox-container
          {:aria-labelledby (generic-label-component/id-for-label field-descriptor idx)
           :aria-invalid    (not (:valid @(subscribe [:application/answer id idx nil])))
           :tab-index       "0"
           :role            "listbox"}
          (doall
            (map-indexed (fn [option-idx option]
                           ^{:key (str "multiple-choice-" (:id field-descriptor) "-" option-idx "-" (:value option) (when idx (str "-" idx)))}
                           [multiple-choice-option field-descriptor option id idx])
                         (cond->> options
                                  (and (some? (:koodisto-source field-descriptor))
                                       (not (:koodisto-ordered-by-user field-descriptor)))
                                  (sort-by #(util/non-blank-option-label % @languages)))))]]))))

(defn- single-choice-option [option parent-id field-descriptor question-group-idx languages _ _]
  (let [cannot-edit?   (subscribe [:application/cannot-edit? (keyword (:id field-descriptor))])
        label          (util/non-blank-val (:label option) @languages)
        uncertain?     (subscribe [:application/selection-over-network-uncertain?])
        option-value   (:value option)
        option-id      (str (:id field-descriptor) "-" question-group-idx "-" option-value)
        limit-reached? (subscribe [:application/limit-reached? parent-id option-value])
        valid?         (subscribe [:application/single-choice-option-valid? parent-id question-group-idx])
        lang           (subscribe [:application/form-language])]
    (fn [option parent-id field-descriptor question-group-idx _ use-multi-choice-style? verifying?]
      (let [checked?             @(subscribe [:application/single-choice-option-checked? parent-id option-value question-group-idx])
            followups            (filterv #(deref (subscribe [:application/visible? (keyword (:id %))]))
                                          (:followups option))
            unselectable?        (and (or (not checked?)
                                          (not @valid?))
                                      @limit-reached?)
            disabled?            (or @verifying? @cannot-edit? unselectable?)
            selection-uncertain? @uncertain?
            has-selection-limit? (:selection-limit option)
            sure-if-selected?    (or (not has-selection-limit?) (and (-> field-descriptor :params :selection-group-id) (not selection-uncertain?)))]
        [:div.application__form-single-choice-button-inner-container {:key option-id}
         [:input
          (merge {:id        option-id
                  :type      "checkbox"
                  :checked   (and (not @verifying?) (not unselectable?) sure-if-selected? checked?)
                  :value     option-value
                  :on-change (fn [event]
                               (let [value (.. event -target -value)]
                                 (dispatch [:application/set-repeatable-application-field
                                            field-descriptor
                                            question-group-idx
                                            nil
                                            (when-not checked? value)])))
                  :role      "radio"
                  :class     (if use-multi-choice-style?
                               "application__form-checkbox"
                               "application__form-single-choice-button")}
                 (when disabled? {:disabled true}))]
         [:label
          (merge {:for option-id}
                 (when disabled? {:class "disabled"}))
          (when (and @verifying? checked?)
            [:span.application__form-single-choice-button--verifying
             [:i.zmdi.zmdi-spinner.spin]])
          label
          (when (and (not @verifying?) unselectable?)
            (str " (" (tu/get-hakija-translation :limit-reached @lang) ")"))]
         (when (and @valid? checked? (not-empty followups))
           (if use-multi-choice-style?
             (multi-choice-followups followups question-group-idx)
             [:div.application__form-single-choice-followups-indicator]))]))))

(defn- use-multi-choice-style? [single-choice-field langs]
  (or (< 3 (count (:options single-choice-field)))
      (some (fn [option]
              (let [label (util/non-blank-val (:label option) langs)]
                (< 50 (count label))))
            (:options single-choice-field))))

(defn single-choice-button [field-descriptor _]
  (let [button-id               (answer-key field-descriptor)
        languages               (subscribe [:application/default-languages])
        verifying?              (subscribe [:application/fetching-selection-limits? button-id])
        use-multi-choice-style? (use-multi-choice-style? field-descriptor @languages)]
    (fn [field-descriptor idx]
      (let [answer    @(subscribe [:application/answer button-id idx nil])
            options   @(subscribe [:application/visible-options field-descriptor])
            followups (get-visible-followups field-descriptor (:value answer) options)]
        [:div.application__form-field.application__form-single-choice-button-container
         [generic-label-component/generic-label field-descriptor idx]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [hakukohde-names-component/question-hakukohde-names field-descriptor])
         [:div.application__form-text-input-info-text
          [info-text-component/info-text field-descriptor]]
         [:div.application__form-single-choice-button-outer-container
          {:aria-labelledby (generic-label-component/id-for-label field-descriptor idx)
           :aria-invalid    (not (:valid answer))
           :tab-index       "0"
           :role            "radiogroup"
           :class           (when use-multi-choice-style? "application__form-single-choice-button-container--column")}
          (doall
            (map (fn [option]
                   ^{:key (str "single-choice-" (when idx (str idx "-")) (:id field-descriptor) "-" (:value option))}
                   [single-choice-option option button-id field-descriptor idx languages use-multi-choice-style? verifying?])
                 options))]
         (when (and (not use-multi-choice-style?)
                    (not-empty followups))
           (into [:div.application__form-multi-choice-followups-container.animated.fadeIn]
                 (for [followup followups]
                   ^{:key (:id followup)}
                   [render-field followup idx])))]))))

(defonce max-attachment-size-bytes
         (get (js->clj js/config) "attachment-file-max-size-bytes" (* 10 1024 1024)))

(defn- upload-attachment [field-descriptor question-group-idx event]
  (.preventDefault event)
  (let [file-list (or (some-> event .-dataTransfer .-files)
                      (.. event -target -files))
        files     (->> (.-length file-list)
                       (range)
                       (map #(.item file-list %)))]
    (dispatch [:application/add-attachments field-descriptor question-group-idx files])))

(defn deadline-info [deadline]
  (let [lang @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment--deadline
     (str (tu/get-hakija-translation :deadline-in lang) " " deadline)]))

(defn attachment-upload [field-descriptor component-id attachment-count question-group-idx]
  (let [id   (str component-id (when question-group-idx "-" question-group-idx) "-upload-button")
        lang @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment-container
     [:input.application__form-upload-input
      {:id           id
       :type         "file"
       :multiple     "multiple"
       :key          (str "upload-button-" component-id "-" attachment-count)
       :on-change    (partial upload-attachment field-descriptor question-group-idx)
       :required     (is-required-field? field-descriptor)
       :aria-invalid (not (:valid @(subscribe [:application/answer id question-group-idx nil])))
       :autoComplete autocomplete-off}]
     [:label.application__form-upload-label
      {:for id}
      [:i.zmdi.zmdi-cloud-upload.application__form-upload-icon]
      [:span.application__form-upload-button-add-text (tu/get-hakija-translation :add-attachment lang)]]
     [:span.application__form-upload-button-info
      [:div
       (tu/get-hakija-translation :file-size-info lang (util/size-bytes->str max-attachment-size-bytes))]
      (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
        (deadline-info deadline))]]))

(defn- attachment-filename
  [id question-group-idx attachment-idx show-size?]
  (let [file @(subscribe [:application/answer
                          id
                          question-group-idx
                          attachment-idx])
        link @(subscribe [:application/attachment-download-link (:value file)])]
    [:div
     (if (and (:final file)
              (fc/feature-enabled? :attachment-download-allowed))
       [:a.application__form-attachment-filename
        {:href link}
        (:filename file)]
       [:span.application__form-attachment-filename
        (:filename file)])
     (when (and (some? (:size file)) show-size?)
       [:span (str " (" (util/size-bytes->str (:size file)) ")")])]))

(defn- attachment-remove-button
  [field-descriptor _ _]
  (let [id       (keyword (:id field-descriptor))
        confirm? (r/atom false)]
    (fn [field-descriptor question-group-idx attachment-idx]
      (let [lang         @(subscribe [:application/form-language])
            cannot-edit? @(subscribe [:application/cannot-edit? id])]
        [:div.application__form-attachment-remove-button-container
         (when-not cannot-edit?
           [:button.application__form-attachment-remove-button
            {:on-click #(swap! confirm? not)}
            (if @confirm?
              (tu/get-hakija-translation :cancel-remove lang)
              (tu/get-hakija-translation :remove lang))])
         (when @confirm?
           [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
            {:on-click (fn [_]
                         (reset! confirm? false)
                         (dispatch [:application/remove-attachment
                                    field-descriptor
                                    question-group-idx
                                    attachment-idx]))}
            (tu/get-hakija-translation :confirm-remove lang)])]))))

(defn- cancel-attachment-upload-button
  [_ _ _]
  (let [confirm? (r/atom false)
        lang     (subscribe [:application/form-language])]
    (fn [field-descriptor question-group-idx attachment-idx]
      [:div.application__form-attachment-remove-button-container
       [:button.application__form-attachment-remove-button
        {:on-click #(swap! confirm? not)}
        (if @confirm?
          (tu/get-hakija-translation :cancel-cancel-upload @lang)
          (tu/get-hakija-translation :cancel-upload @lang))]
       (when @confirm?
         [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
          {:on-click (fn [_]
                       (reset! confirm? false)
                       (dispatch [:application/cancel-attachment-upload
                                  field-descriptor
                                  question-group-idx
                                  attachment-idx]))}
          (tu/get-hakija-translation :confirm-cancel-upload @lang)])])))

(defn attachment-view-file [field-descriptor component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__success
    [attachment-filename component-id question-group-idx attachment-idx true]]
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-check-mark-container
    [:i.zmdi.zmdi-check.application__form-attachment-check-mark]]
   [:div.application__form-attachment-list-item-sub-container
    [attachment-remove-button field-descriptor question-group-idx attachment-idx]]])

(defn attachment-view-file-error [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment @(subscribe [:application/answer
                                component-id
                                question-group-idx
                                attachment-idx])
        lang       @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__error
      [attachment-filename component-id question-group-idx attachment-idx true]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-error-container
      (doall
        (map-indexed (fn [i [error params]]
                       ^{:key (str "attachment-error-" i)}
                       [:span.application__form-attachment-error
                        (tu/get-hakija-translation error lang params)])
                     (:errors attachment)))]
     [:div.application__form-attachment-list-item-sub-container
      [attachment-remove-button field-descriptor question-group-idx attachment-idx]]]))

(defn attachment-deleting-file [_ component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
    [attachment-filename component-id question-group-idx attachment-idx true]]])

(defn attachment-uploading-file
  [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment       @(subscribe [:application/answer component-id question-group-idx attachment-idx])
        size             (:size attachment)
        uploaded-size    (:uploaded-size attachment)
        upload-complete? (<= size uploaded-size)
        percent          (int (* 100 (/ uploaded-size size)))
        lang             @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
      [attachment-filename component-id question-group-idx attachment-idx false]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-uploading-container
      [:i.zmdi.zmdi-spinner.application__form-upload-uploading-spinner]
      [:span (str (tu/get-hakija-translation
                    (if upload-complete? :processing-file :uploading)
                    lang)
                  "... ")]
      [:span (str percent " % "
                  "(" (util/size-bytes->str uploaded-size false)
                  "/"
                  (util/size-bytes->str size) ")")]]
     [:div.application__form-attachment-list-item-sub-container
      [cancel-attachment-upload-button field-descriptor question-group-idx attachment-idx]]]))

(defn attachment-row [field-descriptor component-id attachment-idx question-group-idx status]
  [:li.application__attachment-filename-list-item
   [(case status
      :ready attachment-view-file
      :error attachment-view-file-error
      :uploading attachment-uploading-file
      :deleting attachment-deleting-file)
    field-descriptor component-id question-group-idx attachment-idx]])

(defn attachment [{:keys [id] :as field-descriptor} question-group-idx]
  (let [languages              @(subscribe [:application/default-languages])
        text                   (util/non-blank-val (get-in field-descriptor [:params :info-text :value]) languages)
        attachments            @(subscribe [:application/attachments id question-group-idx])
        visible-attachments    @(subscribe [:application/visible-attachments id question-group-idx])
        attachment-count       (count attachments)
        application-identifier @(subscribe [:application/application-identifier])]
    [:div.application__form-field
     [generic-label-component/generic-label field-descriptor question-group-idx]
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [hakukohde-names-component/question-hakukohde-names field-descriptor :liitepyynto-for-hakukohde])
     (when-not (clojure.string/blank? text)
       [markdown-paragraph text (-> field-descriptor :params :info-text-collapse) application-identifier])
     (when (not-empty visible-attachments)
       [:ol.application__attachment-filename-list
        (doall (map (fn [[attachment-idx attachment]]
                      ^{:key (str "attachment-" (when question-group-idx (str question-group-idx "-")) id "-" attachment-idx)}
                      [attachment-row field-descriptor id attachment-idx question-group-idx (:status attachment)])
                    visible-attachments))])
     (if (get-in field-descriptor [:params :mail-attachment?])
       (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
         [:div.application__mail-attachment--deadline
          [deadline-info deadline]])
       (when-not @(subscribe [:application/cannot-edit? (keyword id)])
         [attachment-upload field-descriptor id attachment-count question-group-idx]))]))

(defn- adjacent-field-input [{:keys [field-descriptor]}]
  (let [id          (keyword (:id field-descriptor))
        local-state (r/atom {:focused? false :value nil})]
    (fn [{:keys [field-descriptor labelledby question-group-idx row-idx]}]
      (let [{:keys [value
                    valid]} @(subscribe [:application/answer id question-group-idx row-idx])
            cannot-edit?    @(subscribe [:application/cannot-edit? id])
            show-error?     @(subscribe [:application/show-validation-error-class? id question-group-idx row-idx nil])
            on-blur         (fn [_]
                              (swap! local-state assoc
                                     :focused? false))
            on-change       (fn [evt]
                              (let [value (-> evt .-target .-value)]
                                (swap! local-state assoc
                                       :focused? true
                                       :value value)
                                (dispatch [:application/set-repeatable-application-field
                                           field-descriptor
                                           question-group-idx
                                           row-idx
                                           value])))]
        [:input.application__form-text-input
         {:class           (if show-error?
                             " application__form-field-error"
                             " application__form-text-input--normal")
          :id              (str id "-" row-idx)
          :type            "text"
          :value           (if (:focused? @local-state)
                             (:value @local-state)
                             value)
          :on-blur         on-blur
          :on-change       on-change
          :disabled        cannot-edit?
          :aria-invalid    (not valid)
          :aria-labelledby labelledby
          :tab-index       "0"
          :autoComplete    autocomplete-off}]))))

(defn adjacent-text-fields [field-descriptor _]
  (let [cannot-edits? (map #(subscribe [:application/cannot-edit? (keyword (:id %))])
                           (util/flatten-form-fields (:children field-descriptor)))]
    (fn [field-descriptor question-group-idx]
      (let [row-amount      (subscribe [:application/adjacent-field-row-amount field-descriptor question-group-idx])
            remove-on-click (fn remove-adjacent-text-field [event]
                              (let [row-idx (int (.getAttribute (.-currentTarget event) "data-row-idx"))]
                                (.preventDefault event)
                                (dispatch [:application/remove-adjacent-field
                                           field-descriptor
                                           question-group-idx
                                           row-idx])))
            add-on-click    (fn add-adjacent-text-field [event]
                              (.preventDefault event)
                              (dispatch [:application/add-adjacent-fields
                                         field-descriptor
                                         question-group-idx]))
            lang            @(subscribe [:application/form-language])
            header-label-id (generic-label-component/id-for-label field-descriptor question-group-idx)]
        [:div.application__form-field
         [generic-label-component/generic-label field-descriptor question-group-idx]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [hakukohde-names-component/question-hakukohde-names field-descriptor])
         [info-text-component/info-text field-descriptor]
         [:div
          (->> (range @row-amount)
               (map (fn adjacent-text-fields-row [row-idx]
                      ^{:key (str "adjacent-fields-" row-idx)}
                      [:div.application__form-adjacent-text-fields-wrapper
                       (map-indexed (fn adjacent-text-fields-column [col-idx child]
                                      (let [key            (str "adjacent-field-" row-idx "-" col-idx)
                                            field-label-id (generic-label-component/id-for-label child
                                                                                                 question-group-idx)]
                                        ^{:key key}
                                        [:div.application__form-adjacent-row
                                         [:div (when-not (= row-idx 0)
                                                 {:class "application__form-adjacent-row--mobile-only"})
                                          [generic-label-component/generic-label child question-group-idx]]
                                         [adjacent-field-input
                                          {:field-descriptor   child
                                           :labelledby         (str header-label-id " " field-label-id)
                                           :question-group-idx question-group-idx
                                           :row-idx            row-idx}]]))
                                    (:children field-descriptor))
                       (when (and (pos? row-idx) (not (some deref cannot-edits?)))
                         [:a {:data-row-idx row-idx
                              :on-click     remove-on-click}
                          [:span.application__form-adjacent-row--mobile-only (tu/get-hakija-translation :remove-row lang)]
                          [:i.application__form-adjacent-row--desktop-only.i.zmdi.zmdi-close.zmdi-hc-lg]])]))
               doall)]
         (when (and (get-in field-descriptor [:params :repeatable])
                    (not (some deref cannot-edits?)))
           [:a.application__form-add-new-row
            {:on-click add-on-click}
            [:i.zmdi.zmdi-plus-square] (str " " (tu/get-hakija-translation :add-row lang))])]))))

(defn- render-component [{:keys [field-descriptor
                                 idx]}]
  (match field-descriptor
         {:id         "email"
          :fieldClass "formField"
          :fieldType  "textField"} [email-field field-descriptor idx]
         {:fieldClass "wrapperElement"
          :fieldType  "fieldset"} [wrapper-field field-descriptor idx]
         {:fieldClass "questionGroup"
          :fieldType  "fieldset"} [question-group field-descriptor idx]
         {:fieldClass "wrapperElement"
          :fieldType  "rowcontainer"} [row-wrapper field-descriptor idx]
         {:fieldClass "formField" :fieldType "textField" :params {:repeatable true}} [repeatable-text-field field-descriptor idx]
         {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor idx]
         {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor idx]
         {:fieldClass "formField" :fieldType "dropdown"} [dropdown-component/dropdown field-descriptor idx render-field]
         {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor idx]
         {:fieldClass "formField" :fieldType "singleChoice"} [single-choice-button field-descriptor idx]
         {:fieldClass "formField" :fieldType "attachment"} [attachment field-descriptor idx]
         {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohde/hakukohteet field-descriptor idx]
         {:fieldClass "pohjakoulutusristiriita" :fieldType "pohjakoulutusristiriita"} [pohjakoulutusristiriita/pohjakoulutusristiriita field-descriptor idx]
         {:fieldClass "infoElement"} [info-element field-descriptor idx]
         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset"} [adjacent-text-fields field-descriptor idx]))

(defn render-field [field-descriptor idx]
  (when (and field-descriptor (not (:duplikoitu-kysymys-hakukohde-oid field-descriptor)))
    (let [version (:version field-descriptor)
          render-fn (cond
                      (= "generic" version) generic-component/render-generic-component
                      (= "oppiaineen-arvosanat" version) arvosanat/render-arvosanat-component
                      :else render-component)
          visible?  @(subscribe [:application/visible? (keyword (:id field-descriptor))])]
      (when visible?
        [render-fn
         {:field-descriptor field-descriptor
          :idx              idx
          :render-field     render-field}]))))

(defn render-duplicate-field [field-descriptor idx]
  (when (and field-descriptor (:duplikoitu-kysymys-hakukohde-oid field-descriptor))
    (let [visible?  @(subscribe [:application/visible? (keyword (:id field-descriptor))])]
      (when visible?
        [render-component
         {:field-descriptor field-descriptor
          :idx              idx
          :render-field     render-field}]))))

(defn editable-fields [_]
  (r/create-class
   {:component-did-mount #(dispatch [:application/setup-window-unload])
    :reagent-render      (fn [form-data]
                           (into [:div.application__editable-content.animated.fadeIn]
                                 (for [field (:content form-data)
                                       :when @(subscribe [:application/visible? (keyword (:id field))])]
                                   ^{:key (:id field)}
                                   (if (:per-hakukohde field)
                                     [:div.per-question-wrapper
                                        [form-field-label-component/form-field-label field (application-field/form-field-id field nil)]
                                        (for [duplicate-field (filter #(= (:original-question %) (:id field)) (:content form-data))]
                                          ^{:key (str "duplicate-" (:id duplicate-field))}
                                          [render-duplicate-field duplicate-field nil])]
                                     [render-field field nil]))))}))
