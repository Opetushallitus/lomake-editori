(ns ataru.virkailija.editor.component
  (:require
   [ataru.application-common.application-field-common :refer [copy-link]]
   [ataru.cljs-util :as util]
   [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
   [ataru.virkailija.editor.components.followup-question :as followup-question]
   [ataru.component-data.person-info-module :as pm]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
   [cljs.core.match :refer-macros [match]]
   [clojure.string :as string]
   [goog.string :as s]
   [cljs-time.core :as t]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :as r]
   [ataru.component-data.module.module-spec :as module-spec]
   [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
   [ataru.virkailija.editor.components.component-content :as component-content]
   [ataru.virkailija.editor.components.info-addon-component :as info-addon-component]
   [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
   [ataru.virkailija.editor.components.input-field-component :as input-field-component]
   [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]
   [ataru.virkailija.editor.components.repeater-checkbox-component :as repeater-checkbox-component]
   [ataru.virkailija.editor.components.text-component :as text-component]
   [ataru.virkailija.editor.components.text-header-component :as text-header-component]
   [ataru.virkailija.editor.components.validator-checkbox-component :as validator-checkbox-component]))

(defn- required-disabled [initial-content]
  (contains? (-> initial-content :validators set) "required-hakija"))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- koodisto-field [component idx lang]
  (let [value (get-in component [:label lang])]
    [:div.editor-form__koodisto-field-container
     {:key (str "option-" lang "-" idx)}
     [:div.editor-form__koodisto-field
      {:on-drop prevent-default
       :data-test-id "editor-form__koodisto-field"}
      value]]))

(defn- koodisto-fields-with-lang [_ _]
  (fn [languages option-path]
    (let [component           @(subscribe [:editor/get-component-value option-path])]
      [:div
       {:title (clojure.string/join ", " (map (fn [lang] (get-in component [:label lang])) languages))}
       (map-indexed (partial koodisto-field component)
                    languages)])))

(defn text-field [initial-content followups path]
  [text-component/text-component initial-content followups path
   :header-label @(subscribe [:editor/virkailija-translation :text-field])
   :size-label @(subscribe [:editor/virkailija-translation :text-field-size])])

(defn text-area [initial-content followups path]
  [text-component/text-component initial-content followups path
   :header-label @(subscribe [:editor/virkailija-translation :text-area])
   :size-label @(subscribe [:editor/virkailija-translation :text-area-size])])

(defn- remove-dropdown-option-button [path option-index disabled? parent-key option-value question-group-element?]
  [:div.editor-form__multi-options-remove--cross
   [copy-link (str parent-key "_" (when question-group-element? "groupN_") option-value) :answer? true :shared-use-warning? false]
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (.preventDefault evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index])))
     :class    (when disabled? "editor-form__multi-options-remove--cross--disabled")}]])

(defn- dropdown-option
  [option-index _ _ _ path _ show-followups _ _ _ & _]
  (let [component-locked?        (subscribe [:editor/component-locked? path])
        on-click            (fn [up? event]
                              (when-not @component-locked?
                                (.preventDefault event)
                                (reset! show-followups nil)
                                (dispatch [(if up?
                                             :editor/move-option-up
                                             :editor/move-option-down) path option-index])))
        selection-limit?    (subscribe [:editor/selection-limit? path])
        only-numbers        (fn [value]
                              (let [n (string/replace value #"\D" "")]
                                (if (empty? n)
                                  nil
                                  (js/parseInt n))))]
    (fn [option-index option-count option-path followups path languages show-followups parent-key option-value question-group-element? &
         {:keys [editable?]
          :or   {editable? true}}]
      [:div
       [:div.editor-form__multi-options-wrapper-outer
        [:div
         [:div.editor-form__multi-options-arrows-container
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--up
           {:on-click (partial on-click true)
            :class    (when @component-locked? "editor-form__multi-options-arrow--disabled")}]
          [:div.editor-form__multi-options-arrows--stretch]
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--down
           {:on-click (partial on-click false)
            :class    (when @component-locked? "editor-form__multi-options-arrow--disabled")}]]]
        [:div.editor-form__multi-options-wrapper-inner
         {:key (str "options-" option-index)}
         (if editable?
           (input-fields-with-lang-component/input-fields-with-lang
            (fn [lang]
              [input-field-component/input-field {:path        option-path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch [:editor/set-dropdown-option-value
                                                                           (-> % .-target .-value)
                                                                           option-path :label lang])}])
            languages)
           [koodisto-fields-with-lang languages option-path])]
        (when @selection-limit?
          [:div.editor-form__selection-limit
           [input-field-component/input-field {:path        option-path
                                               :lang        :dont-care
                                               :dispatch-fn #(dispatch [:editor/set-dropdown-option-selection-limit
                                                                        (only-numbers (-> % .-target .-value))
                                                                        option-path :selection-limit])
                                               :placeholder @(subscribe [:editor/virkailija-translation :selection-limit-input])
                                               :class       "editor-form__text-field--selection-limit"
                                               :value-fn    (fn [v] (:selection-limit v))}]])
        [followup-question/followup-question option-index followups show-followups]
        [belongs-to-hakukohteet-component/belongs-to-hakukohteet-option parent-key option-index option-path]
        (when editable?
          [remove-dropdown-option-button path option-index (or @component-locked? (< option-count 3)) parent-key option-value question-group-element?])]
       [followup-question/followup-question-overlay option-index followups path show-followups]])))

(defn- select-koodisto-dropdown
  [path]
  (let [id                (util/new-uuid)
        selected-koodisto (subscribe [:editor/get-component-value path :koodisto-source])]
    (fn [path]
      [:div.editor-form__koodisto-options
       [:label.editor-form__select-koodisto-dropdown-label
        {:for id}
        @(subscribe [:editor/virkailija-translation :koodisto])]
       [:div.editor-form__select-koodisto-dropdown-wrapper
        [:select.editor-form__select-koodisto-dropdown
         {:id        id
          :class     (if (string/blank? (:uri @selected-koodisto))
                       "editor-form__select-koodisto-dropdown--invalid"
                       "editor-form__select-koodisto-dropdown--regular")
          :value     (:uri @selected-koodisto)
          :on-change #(dispatch [:editor/select-koodisto-options (.-value (.-target %)) path (:allow-invalid? @selected-koodisto)])
          :data-test-id "editor-form__select-koodisto-dropdown"}
         (when (= (:uri @selected-koodisto) "")
           [:option {:value "" :disabled true} ""])
         (for [{:keys [uri title]} koodisto-whitelist/koodisto-whitelist]
           ^{:key (str "koodisto-" uri)}
           [:option {:value uri} title])]
        [:div.editor-form__select-koodisto-dropdown-arrow
         [:i.zmdi.zmdi-chevron-down]]]])))

(defn- custom-answer-options [_ _ _ _ _ _ show-followups parent-key]
  (fn [languages options followups path question-group-element? editable? _ _]
    (let [option-count (count options)]
      (when (or (nil? @show-followups)
                (not (= (count @show-followups) option-count)))
        (reset! show-followups (vec (replicate option-count false))))
      [:div.editor-form__multi-options-container
       {:data-test-id "editor-form__multi-options-container"}
       (doall (map-indexed (fn [idx option]
                             ^{:key (str "options-" idx)}
                             [dropdown-option
                              idx
                              option-count
                              (conj path :options idx)
                              (nth followups idx)
                              path
                              languages
                              show-followups
                              parent-key
                              (:value option)
                              question-group-element?
                              :editable? editable?])
                           options))])))

(defn koodisto-answer-options [_ _ _ _ parent-key]
  (let [opened? (r/atom false)]
    (fn [_ followups path question-group-element?]
      (let [languages             @(subscribe [:editor/languages])
            value                 @(subscribe [:editor/get-component-value path])
            editable?             false
            hide-koodisto-options (fn [_]
                                    (reset! opened? false))
            show-koodisto-options (fn [_]
                                    (reset! opened? true))
            show-followups        (r/atom nil)]
        (if (not @opened?)
          [:div.editor-form__show-koodisto-values
           [:a
            {:on-click show-koodisto-options
             :data-test-id "editor-form__show_koodisto-values__link"}
            [:i.zmdi.zmdi-chevron-down] (str " " @(subscribe [:editor/virkailija-translation :show-options]))]]
          [:div
           [:div.editor-form__show-koodisto-values
            [:a
             {:on-click hide-koodisto-options}
             [:i.zmdi.zmdi-chevron-up] (str " " @(subscribe [:editor/virkailija-translation :hide-options]))]]
           [custom-answer-options languages (:options value) followups path question-group-element? editable? show-followups parent-key]])))))

(defn dropdown [_ _ path _]
  (let [languages                (subscribe [:editor/languages])
        options-koodisto         (subscribe [:editor/get-component-value path :koodisto-source])
        koodisto-ordered-by-user (subscribe [:editor/get-component-value path :koodisto-ordered-by-user])
        value                    (subscribe [:editor/get-component-value path])
        support-selection-limit? (subscribe [:editor/dropdown-with-selection-limit? path])
        selected-form-key        (subscribe [:editor/selected-form-key])
        koodisto-ordered-id      (util/new-uuid)
        component-locked?        (subscribe [:editor/component-locked? path])
        allow-invalid-koodis-id  (util/new-uuid)]
    (fn [initial-content followups path {:keys [question-group-element?]}]
      (let [languages      @languages
            field-type     (:fieldType @value)
            show-followups (r/atom nil)]
        [:div.editor-form__component-wrapper
         {:data-test-id "editor-form__dropdopwn-component-wrapper"}
         (let [header (case field-type
                        "dropdown"       (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :dropdown-koodisto])
                                           @(subscribe [:editor/virkailija-translation :dropdown]))
                        "singleChoice"   (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :single-choice-button-koodisto])
                                           @(subscribe [:editor/virkailija-translation :single-choice-button]))
                        "multipleChoice" (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :multiple-choice-koodisto])
                                           @(subscribe [:editor/virkailija-translation :multiple-choice])))
               data-test-id (str "editor-form__" field-type "-component-main")]
           [text-header-component/text-header (:id initial-content) header path (:metadata initial-content)
            :sub-header (:label @value) :data-test-id data-test-id])
         [component-content/component-content
          path
          [:div
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-question-wrapper
             [:div.editor-form__text-field-wrapper
              {:data-test-id (str "editor-form__" field-type "-component-question-wrapper")}
              [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
               [copy-link (:id initial-content)]]
              (input-fields-with-lang-component/input-fields-with-lang
               (fn [lang]
                 [input-field-component/input-field {:path        path
                                                     :lang        lang
                                                     :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                   (-> % .-target .-value)
                                                                                   path :label lang])}])
               languages
               :header? true)]
             [:div.editor-form__checkbox-wrapper
              [validator-checkbox-component/validator-checkbox path initial-content :required (required-disabled initial-content)]
              (when @support-selection-limit?
                [validator-checkbox-component/validator-checkbox path initial-content :selection-limit nil
                 #(dispatch [:editor/set-selection-group-id (when % @selected-form-key) path])])
              (when (some? @options-koodisto)
                [:div.editor-form__checkbox-container
                 [:input.editor-form__checkbox
                  {:id        koodisto-ordered-id
                   :type      "checkbox"
                   :checked   (not @koodisto-ordered-by-user)
                   :disabled  @component-locked?
                   :on-change (fn [event]
                                (dispatch [:editor/set-ordered-by-user (-> event .-target .-checked) path]))}]
                 [:label.editor-form__checkbox-label
                  {:for koodisto-ordered-id}
                  @(subscribe [:editor/virkailija-translation :alphabetically])]])
              (when (some? @options-koodisto)
                [:div.editor-form__checkbox-container
                 [:input.editor-form__checkbox
                  {:id        allow-invalid-koodis-id
                   :type      "checkbox"
                   :checked   (boolean (:allow-invalid? @options-koodisto))
                   :disabled  @component-locked?
                   :on-change #(dispatch [:editor/select-koodisto-options
                                          (:uri @options-koodisto)
                                          path
                                          (not (:allow-invalid? @options-koodisto))])}]
                 [:label.editor-form__checkbox-label
                  {:for allow-invalid-koodis-id}
                  @(subscribe [:editor/virkailija-translation :allow-invalid-koodis])]])]
             [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]
           [info-addon-component/info-addon path initial-content]
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-options_wrapper
             (if (some? @options-koodisto)
               [select-koodisto-dropdown path]
               [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :options])])
             (if (nil? @options-koodisto)
               [custom-answer-options languages (:options @value) followups path question-group-element? true show-followups (:id initial-content)]
               [koodisto-answer-options (:id @value) followups path question-group-element? (:id initial-content)])
             (when (nil? @options-koodisto)
               [:div.editor-form__add-dropdown-item
                [:a
                 {:on-click (fn [evt]
                              (when-not @component-locked?
                                (.preventDefault evt)
                                (reset! show-followups nil)
                                (dispatch [:editor/add-dropdown-option path])))
                  :class    (when @component-locked? "editor-form__add-dropdown-item--disabled")}
                 [:i.zmdi.zmdi-plus-square] (str " " @(subscribe [:editor/virkailija-translation :add]))]])]]]]]))))

(defn component-group [content path children]
  (let [id                (:id content)
        languages         @(subscribe [:editor/languages])
        value             @(subscribe [:editor/get-component-value path])
        group-header-text (case (:fieldClass content)
                            "wrapperElement" @(subscribe [:editor/virkailija-translation :wrapper-element])
                            "questionGroup"  @(subscribe [:editor/virkailija-translation :question-group]))
        header-label-text (case (:fieldClass content)
                            "wrapperElement" @(subscribe [:editor/virkailija-translation :wrapper-header])
                            "questionGroup"  @(subscribe [:editor/virkailija-translation :group-header]))]
    [:div.editor-form__component-wrapper
     [text-header-component/text-header id group-header-text path (:metadata content)
      :sub-header (:label value)]
     [component-content/component-content
      path ;id
      [:div
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header header-label-text]
        (input-fields-with-lang-component/input-fields-with-lang
          (fn [lang]
            [input-field-component/input-field {:path        path
                                                :lang        lang
                                                :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                              (-> % .-target .-value)
                                                                              path :label lang])}])
         languages
         :header? true)]
       [:div.editor-form__wrapper-element-well
        children]
       [dnd/drag-n-drop-spacer (conj path :children (count children))]
       (when-not @(subscribe [:editor/component-locked? path])
         (case (:fieldClass content)
           "wrapperElement" [toolbar/add-component (conj path :children (count children)) false]
           "questionGroup"  [toolbar/question-group-toolbar path
                             (fn [generate-fn]
                               (dispatch [:generate-component generate-fn (conj path :children (count children))]))]))]]]))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
              "questionGroup" (map #(recursively-get-labels %) (:children component))
              "wrapperElement" (map #(recursively-get-labels %) (:children component))
              :else (or (-> component :label lang)
                        (-> component :label :fi))))]
    (flatten (recursively-get-labels component))))

(defn hakukohteet-module [_ path]
  (let [virkailija-lang (subscribe [:editor/virkailija-lang])
        value           (subscribe [:editor/get-component-value path])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
        :foldable? false
        :can-cut? true
        :can-copy? false
        :can-remove? false
        :data-test-id "hakukohteet-header"]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
         @(subscribe [:editor/virkailija-translation :hakukohde-info])]]])))

(defn module [content path]
  (let [{:keys [foldable?
                can-cut?
                can-copy?
                can-remove?
                show-child-component-names?
                has-multiple-configurations?]} (-> content :module name module-spec/get-module-spec)
        value             (subscribe [:editor/get-component-value path])
        virkailija-lang   (subscribe [:editor/virkailija-lang])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [content path]
      (let [module-name         (-> content :module keyword)
            data-test-id-prefix (case module-name
                                  :person-info "henkilotietomoduuli"
                                  :arvosanat-peruskoulu "arvosanat-moduuli"
                                  nil)]
        [:div.editor-form__component-wrapper
         [text-header-component/text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
          :foldable? foldable?
          :can-cut? can-cut?
          :can-copy? can-copy?
          :can-remove? can-remove?
          :show-child-component-names? show-child-component-names?
          :data-test-id (some-> data-test-id-prefix (str "-header"))]
         [:div.editor-form__component-content-wrapper
          (when has-multiple-configurations?
            (let [values (set ["onr" "muu"])]
              [:div.editor-form__module-fields
               [:select.editor-form__select
                {:on-change    (fn [event]
                                 (let [version    (keyword (-> event .-target .-value))
                                       new-module (pm/person-info-module version)]
                                   (dispatch-sync [:editor/set-component-value
                                                   new-module path])))
                 :disabled     @component-locked?
                 :value        (or (get values (:id content)) "onr")
                 :data-test-id (some-> data-test-id-prefix (str "-select"))}
                (doall (for [opt values]
                         [:option {:value opt
                                   :key   opt} @(subscribe [:editor/virkailija-translation (keyword (str "person-info-module-" opt))])]))]]))
          (when show-child-component-names?
            [:div.editor-form__module-fields
             [:span.editor-form__module-fields-label
              @(subscribe [:editor/virkailija-translation :contains-fields])]
             " "
             [:span
              {:data-test-id (some-> data-test-id-prefix (str "-fields-label"))}
              (clojure.string/join ", " (get-leaf-component-labels @value @(subscribe [:editor/virkailija-lang])))]])]]))))

(defn info-element
  "Info text which is a standalone component"
  [_ path]
  (let [languages        (subscribe [:editor/languages])
        collapse-checked (subscribe [:editor/get-component-value path :params :info-text-collapse])
        sub-header       (subscribe [:editor/get-component-value path :label])
        component-locked?     (subscribe [:editor/component-locked? path])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) @(subscribe [:editor/virkailija-translation :info-element]) path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content/component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
           (input-fields-with-lang-component/input-fields-with-lang
            (fn [lang]
              [input-field-component/input-field {:path        path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                (-> % .-target .-value)
                                                                                path :label lang])}])
            @languages
            :header? true)
           [:div.infoelement
            [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :text])]
            (->> (input-fields-with-lang-component/input-fields-with-lang
                  (fn [lang]
                    [input-field-component/input-field {:path        path
                                                        :lang        lang
                                                        :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                      (-> % .-target .-value)
                                                                                      path :text lang])
                                                        :value-fn    (fn [component] (get-in component [:text lang]))
                                                        :tag         :textarea}])
                  @languages
                  :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help-component/markdown-help)]])))
                 doall)]]
          [:div.editor-form__checkbox-wrapper
           (let [collapsed-id (util/new-uuid)]
             [:div.editor-form__checkbox-container
              [:input.editor-form__checkbox {:type      "checkbox"
                                             :id        collapsed-id
                                             :checked   (boolean @collapse-checked)
                                             :disabled  @component-locked?
                                             :on-change (fn [event]
                                                          (dispatch [:editor/set-component-value
                                                                     (-> event .-target .-checked)
                                                                     path :params :info-text-collapse]))}]
              [:label.editor-form__checkbox-label
               {:for   collapsed-id
                :class (when @component-locked? "editor-form__checkbox-label--disabled")}
               @(subscribe [:editor/virkailija-translation :collapse-info-text])]])]
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]]])))

(defn pohjakoulutusristiriita
  [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) (get-in initial-content [:label :fi]) path (:metadata initial-content)]
       [component-content/component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:div.infoelement
            (->> (input-fields-with-lang-component/input-fields-with-lang
                  (fn [lang]
                    [input-field-component/input-field {:path        path
                                                        :lang        lang
                                                        :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                      (-> % .-target .-value)
                                                                                      path :text lang])
                                                        :value-fn    (fn [component] (get-in component [:text lang]))
                                                        :tag         :textarea}])
                  @languages
                  :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help-component/markdown-help)]])))
                 doall)]]]]]])))

(defn adjacent-fieldset [_ path _]
  (let [languages         (subscribe [:editor/languages])
        sub-header        (subscribe [:editor/get-component-value path :label])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :adjacent-fieldset]) path (:metadata content)
        :sub-header @sub-header]
       [component-content/component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
           (input-fields-with-lang-component/input-fields-with-lang
            (fn [lang]
              [input-field-component/input-field {:path        path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                (-> % .-target .-value)
                                                                                path :label lang])}])
            @languages
            :header? true)]
          [:div.editor-form__checkbox-wrapper
           [repeater-checkbox-component/repeater-checkbox path content]]
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]
         [info-addon-component/info-addon path]
         [:div.editor-form__adjacent-fieldset-container
          children
          (when (and (not @component-locked?)
                     (-> (count children) (< 3)))
            [toolbar/adjacent-fieldset-toolbar
             (concat path [:children])
             (fn [component-fn]
               (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]]]])))

(defn adjacent-text-field [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :text-field]) path (:metadata content)
        :foldable? false
        :can-cut? false
        :can-copy? false
        :can-remove? true]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__component-row-wrapper
         [:div.editor-form__text-field-wrapper
          [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
           [copy-link (:id content)]]
          (input-fields-with-lang-component/input-fields-with-lang
           (fn [lang]
             [input-field-component/input-field {:path        path
                                                 :lang        lang
                                                 :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                               (-> % .-target .-value)
                                                                               path :label lang])}])
           @languages
           :header? true)]
         [:div.editor-form__checkbox-wrapper
          [validator-checkbox-component/validator-checkbox path content :required (required-disabled content)]
          [text-component/text-component-type-selector (:id content) path {:adjacent-text-field? true
                                                                           :allow-decimals?      true}]]
        [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]]])))

(defn attachment-textarea [path]
  (let [checked?         (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        mail-attachment? (subscribe [:editor/get-component-value path :params :mail-attachment?])
        collapse?        (subscribe [:editor/get-component-value path :params :info-text-collapse])
        languages        (subscribe [:editor/languages])
        component-locked?      (subscribe [:editor/component-locked? path])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       (let [id (util/new-uuid)]
         [:div.editor-form__info-addon-checkbox
          [:input {:id        id
                   :type      "checkbox"
                   :checked   @mail-attachment?
                   :disabled  @component-locked?
                   :on-change (fn toggle-attachment-textarea [event]
                                (.preventDefault event)
                                (let [mail-attachment? (.. event -target -checked)]
                                  (dispatch [:editor/update-mail-attachment mail-attachment? path])))}]
          [:label
           {:for   id
            :class (when @component-locked? "editor-form__checkbox-label--disabled")}
           @(subscribe [:editor/virkailija-translation :mail-attachment-text])]])
       (when-not @mail-attachment?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   @checked?
                     :disabled  @component-locked?
                     :on-change (fn toggle-attachment-textarea [event]
                                  (.preventDefault event)
                                  (let [checked? (.. event -target -checked)]
                                    (dispatch [:editor/set-component-value checked? path :params :info-text :enabled?])))}]
            [:label
             {:for   id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :attachment-info-text])]]))
       (when @checked?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   (boolean @collapse?)
                     :disabled  @component-locked?
                     :on-change (fn [event]
                                  (dispatch [:editor/set-component-value
                                             (-> event .-target .-checked)
                                             path :params :info-text-collapse]))}]
            [:label
             {:for   id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :collapse-info-text])]]))
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang-component/input-fields-with-lang
                (fn attachment-textarea-input [lang]
                  [input-field-component/input-field {:path        path
                                                      :lang        lang
                                                      :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                    (-> % .-target .-value)
                                                                                    path :params :info-text :value lang])
                                                      :value-fn    #(get-in % [:params :info-text :value lang])
                                                      :tag         :textarea}])
                @languages
                :header? true)
               (map (fn [field]
                      (into field [[:div.editor-form__info-addon-markdown-anchor
                                    (markdown-help-component/markdown-help)]])))
               doall)])])))


(def ^:private deadline-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4}) (\d{1,2}):(\d{1,2})$")
(def ^:private deadline-format "%d.%d.%d %02d:%02d")

(defn deadline-date [deadline]
  (when-let [[_ day month year hours minutes] (map js/parseInt (re-matches deadline-pattern deadline))]
    (if-let [dt (t/date-time year month day hours minutes)]
      (when (= [year month day hours minutes]
               [(.getYear dt) (inc (.getMonth dt)) (.getDate dt)
                (.getHours dt)
                (.getMinutes dt)])
        (s/format deadline-format day month year hours minutes)))))

(defn attachment [_ path]
  (let [component        (subscribe [:editor/get-component-value path])
        languages        (subscribe [:editor/languages])
        deadline-value   (r/atom (get-in @component [:params :deadline]))
        valid            (r/atom true)
        mail-attachment? (subscribe [:editor/get-component-value path :params :mail-attachment?])
        format-deadline  (fn [event]
                           (some->> (deadline-date (-> event .-target .-value))
                                    (reset! deadline-value)))
        update-value     (fn [unformatted-value value valid?]
                           (reset! deadline-value unformatted-value)
                           (reset! valid valid?)
                           (dispatch-sync [:editor/set-component-value value path :params :deadline]))
        update-deadline  (fn [event]
                           (let [value    (-> event .-target .-value)
                                 deadline (deadline-date value)]
                             (cond
                              (clojure.string/blank? value) (update-value value nil true)
                              (and value deadline) (update-value value deadline true)
                              :else (update-value value nil false))))]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :attachment]) path (:metadata content)
        :sub-header (:label @component)]
       [component-content/component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :attachment-name])
            [copy-link (:id content)]]
           (input-fields-with-lang-component/input-fields-with-lang
            (fn attachment-file-name-input [lang]
              [input-field-component/input-field {:path        path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                (-> % .-target .-value)
                                                                                path :label lang])}])
            @languages
            :header? true)]
          [:div.editor-form__text-field-wrapper
           [:label.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :attachment-deadline])]
           [:input.editor-form__attachment-deadline-field
            {:type        "text"
             :class       (when-not @valid "editor-form__text-field--invalid")
             :value       @deadline-value
             :on-blur     format-deadline
             :placeholder "pp.kk.vvvv hh:mm"
             :on-change   update-deadline}]]
          (when-not @mail-attachment?
            [:div.editor-form__checkbox-wrapper
             [validator-checkbox-component/validator-checkbox path content :required (required-disabled content)]])
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]
         [attachment-textarea path]]]])))
