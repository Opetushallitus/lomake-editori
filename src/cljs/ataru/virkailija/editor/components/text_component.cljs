(ns ataru.virkailija.editor.components.text-component
  (:require
    [ataru.application-common.application-field-common :refer [copy-link]]
    [ataru.cljs-util :as util]
    [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
    [ataru.virkailija.editor.components.component-content :as component-content]
    [ataru.virkailija.editor.components.followup-question :as followup-question]
    [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
    [ataru.virkailija.editor.components.input-field-component :as input-field-component]
    [ataru.virkailija.editor.components.info-addon-component :as info-addon-component]
    [ataru.virkailija.editor.components.repeater-checkbox-component :as repeater-checkbox-component]
    [ataru.virkailija.editor.components.text-header-component :as text-header-component]
    [ataru.virkailija.editor.components.validator-checkbox-component :as validator-checkbox-component]
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as string]
    [reagent.core :as r]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(defn- required-disabled [initial-content]
  (contains? (-> initial-content :validators set) "required-hakija"))

(defn- get-val [event]
  (-> event .-target .-value))

(defn- numeerisen-kentän-muoto [{:keys [path]}]
  (let [decimal-places    (subscribe [:editor/get-component-value path :params :decimals])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [{:keys [component-id path allow-decimals?]}]
      [:div.editor-form__additional-params-container
       [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :shape])]
       [:select.editor-form__decimal-places-selector
        {:value     (or @decimal-places "")
         :disabled  @component-locked?
         :on-change (fn [e]
                      (let [new-val (get-val e)
                            value   (when (not-empty new-val)
                                      (js/parseInt new-val))]
                        (dispatch [:editor/set-decimals-value component-id value path])))}
        [:option {:value "" :key 0} @(subscribe [:editor/virkailija-translation :integer])]
        (doall
          (for [i (range 1 5)]
            [:option {:value i :key i :disabled (not allow-decimals?)}
             (str i " " @(subscribe [:editor/virkailija-translation :decimals]))]))]])))

(defn- numeerisen-kentän-arvoalueen-rajaus [component-id path]
  (let [component-locked? (subscribe [:editor/component-locked? path])
        min-value         (subscribe [:editor/get-range-value component-id :min-value path])
        max-value         (subscribe [:editor/get-range-value component-id :max-value path])
        min-invalid?      (subscribe [:state-query [:editor :ui component-id :min-value :invalid?]])
        max-invalid?      (subscribe [:state-query [:editor :ui component-id :max-value :invalid?]])
        min-id            (util/new-uuid)
        max-id            (util/new-uuid)
        format-range      (fn [value]
                            (string/replace (string/trim (or value "")) "." ","))]
    (fn [component-id path]
      [:div.editor-form__additional-params-container
       [:label.editor-form__range-label
        {:for   min-id
         :class (when @component-locked? "editor-form__checkbox-label--disabled")}
        @(subscribe [:editor/virkailija-translation :numeric-range])]
       [:input.editor-form__range-input
        {:type      "text"
         :id        min-id
         :class     (when @min-invalid? "editor-form__text-field--invalid")
         :disabled  @component-locked?
         :value     @min-value
         :on-blur   #(dispatch [:editor/set-range-value component-id :min-value (format-range (-> % .-target .-value)) path])
         :on-change #(dispatch [:editor/set-range-value component-id :min-value (-> % .-target .-value) path])}]
       [:span "—"]
       [:input.editor-form__range-input
        {:type      "text"
         :id        max-id
         :class     (when @max-invalid? "editor-form__text-field--invalid")
         :disabled  @component-locked?
         :value     @max-value
         :on-blur   #(dispatch [:editor/set-range-value component-id :max-value (format-range (-> % .-target .-value)) path])
         :on-change #(dispatch [:editor/set-range-value component-id :max-value (-> % .-target .-value) path])}]])))

(defn- lomakeosion-piilottaminen-arvon-perusteella [_]
  (let [id (util/new-uuid)]
    (fn [{:keys [component-locked?
                 decimals-in-use?
                 section-visibility-conditions?
                 path
                 repeatable?]}]
      (let [checked? (or section-visibility-conditions? false)
            disabled? (or component-locked?
                          decimals-in-use?
                          section-visibility-conditions?
                          repeatable?)]
        [:div.editor-form__text-field-additional-params-container
         [:input.editor-form__text-field-checkbox
          {:id           id
           :type         "checkbox"
           :checked      checked?
           :disabled     disabled?
           :on-change    (fn [evt]
                           (when-not disabled?
                             (.preventDefault evt)
                             (when (-> evt .-target .-checked)
                               (dispatch [:editor/lisää-tekstikentän-arvon-perusteella-osion-piilottamis-ehto path]))))
           :data-test-id "tekstikenttä-valinta-lomakeosion-piilottaminen-arvon-perusteella"}]
         [:label.editor-form__text-field-checkbox-label
          {:for   id
           :class (when disabled? "editor-form__text-field-checkbox-label--disabled")}
          "Toisen lomakeosion piilottaminen arvon perusteella"] ;@(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella])
         (when checked?
           [:div.editor-form__text-field-checkbox-add-condition
            [:span " | "]
            (let [teksti @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-lisaa-ehto])]
              (if component-locked?
                [:span.editor-form__text-field-checkbox-add-condition-disabled teksti]
                [:a
                 {:on-click (fn [] (dispatch [:editor/lisää-tekstikentän-arvon-perusteella-osion-piilottamis-ehto path]))}
                 teksti]))])]))))

(defn- lisäkysymys-arvon-perusteella [_]
  (let [id (util/new-uuid)]
    (fn [{:keys [component-locked?
                 decimals-in-use?
                 followups?
                 options-with-condition?
                 options-without-condition?
                 path
                 repeatable?]}]
      (let [checked?     options-with-condition?
            disabled?    (or component-locked?
                             decimals-in-use?
                             followups?
                             options-without-condition?
                             repeatable?)]
        [:div.editor-form__text-field-additional-params-container
         [:input.editor-form__text-field-checkbox
          {:id           id
           :type         "checkbox"
           :checked      checked?
           :disabled     disabled?
           :on-change    (fn [evt]
                           (when-not disabled?
                             (.preventDefault evt)
                             (when (-> evt .-target .-checked)
                               (dispatch [:editor/lisää-tekstikentän-arvon-perusteella-optio path]))))
           :data-test-id "tekstikenttä-valinta-lisäkysymys-arvon-perusteella"}]
         [:label.editor-form__text-field-checkbox-label
          {:for   id
           :class (when disabled? "editor-form__text-field-checkbox-label--disabled")}
          @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella])]
         (when checked?
           [:div.editor-form__text-field-checkbox-add-condition
            [:span " | "]
            (let [teksti @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-lisaa-ehto])]
              (if (or component-locked?
                      options-without-condition?)
                [:span.editor-form__text-field-checkbox-add-condition-disabled teksti]
                [:a
                 {:on-click (fn [] (dispatch [:editor/lisää-tekstikentän-arvon-perusteella-optio path]))}
                 teksti]))])]))))

(defn- kenttään-vain-numeroita [{:keys [component-id path] :as props}]
  [:div.editor-form__text-field-kenttään-vain-numeroita
   [numeerisen-kentän-muoto props]
   [numeerisen-kentän-arvoalueen-rajaus component-id path]
   (when (not (:adjacent-text-field? props))
     [lisäkysymys-arvon-perusteella props])
   (when (not (:adjacent-text-field? props))
     [lomakeosion-piilottaminen-arvon-perusteella props])])

(defn text-component-type-selector [_ path _]
  (let [id                (util/new-uuid)
        checked?          (subscribe [:editor/get-component-value path :params :numeric])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [component-id path props]
      [:div
       [:div.editor-form__checkbox-container
        [:input.editor-form__checkbox
         {:type         "checkbox"
          :id           id
          :checked      (or @checked? false)
          :disabled     (or @component-locked?
                            (:cannot-change-type? props))
          :on-change    (fn [event]
                          (let [checked-now? (-> event .-target .-checked)]
                            (dispatch [:editor/set-component-value checked-now? path :params :numeric])
                            (dispatch [(if checked-now?
                                         :editor/add-validator
                                         :editor/remove-validator) "numeric" path])
                            (when-not checked-now?
                              (dispatch [:editor/set-decimals-value component-id nil path]))))
          :data-test-id "tekstikenttä-valinta-kenttään-vain-numeroita"}]
        [:label.editor-form__checkbox-label
         {:for   id
          :class (when @component-locked? "editor-form__checkbox-label--disabled")}
         @(subscribe [:editor/virkailija-translation :only-numeric])]]
       (when @checked?
         [kenttään-vain-numeroita (merge props {:component-id component-id
                                                :path         path})])])))

(defn- button-label-class
  [button-name component-locked?]
  (let [button-class (match button-name
                            "S" "editor-form__button--left-edge"
                            "L" "editor-form__button--right-edge"
                            :else nil)]
    (str (when component-locked? "editor-form__button-label--disabled ") button-class)))

(defn- remove-option [{:keys [disabled? option-index path]}]
  [:div.editor-form__text-field-remove-option
   {:class (when disabled? "editor-form__text-field-remove-option--disabled")}
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (.preventDefault evt)
                   (dispatch [:editor/poista-tekstikentän-arvon-perusteella-optio (conj path option-index)])))}]])

(def ^:private integer-matcher #"([+-]?)(0|[1-9][0-9]*)")

(defn- clean-format [value]
  (string/replace (string/trim (or value "")) "." ","))

(defn- valid-integer? [value]
  (let [clean (clean-format value)]
    (or
      (empty? clean)
      (some? (re-matches integer-matcher clean)))))

(defn- text-field-option-condition [{:keys [path]}]
  (let [component-locked? (subscribe [:editor/component-locked? path])
        id                (util/new-uuid)
        local-state       (r/atom {:focused? false
                                   :valid?   true
                                   :value    ""})]
    (fn [{:keys [condition option-index path]}]
      (let [initialize-state (fn [value]
                               (swap! local-state
                                      assoc
                                      :valid? true
                                      :value value))
            on-blur-fn       (fn [path event]
                               (swap! local-state
                                      assoc
                                      :focused? false)
                               (when (:valid? @local-state)
                                 (dispatch [:editor/aseta-lisäkysymys-arvon-perusteella-vertailuarvo
                                            path
                                            option-index
                                            (get-val event)])))
            on-change-fn     (fn [event] (swap! local-state
                                                assoc
                                                :focused? true
                                                :valid? (valid-integer? (get-val event))
                                                :value (get-val event)))]
        (when (not (:focused? @local-state))
          (let [value (str (-> condition :answer-compared-to))]
            (initialize-state value)))
        [:div.editor-form__text-field-option-condition
         [:label.editor-form__text-field-option-condition-label
          {:for   id
           :class (when @component-locked? "editor-form__textfield-option-condition--disabled")}
          @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-ehto])]
         [:select.editor-form__text-field-option-condition-comparison-operator
          {:disabled     @component-locked?
           :on-change    (fn [event]
                           (dispatch [:editor/aseta-lisäkysymys-arvon-perusteella-operaattori
                                      path
                                      option-index
                                      (get-val event)]))
           :value        (-> condition :comparison-operator)
           :data-test-id "tekstikenttä-lisäkysymys-arvon-perusteella-ehto-operaattori"}
          [:option {:value "<"} @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-ehto-pienempi])]
          [:option {:value "="} @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-ehto-yhtasuuri])]
          [:option {:value ">"} @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-ehto-suurempi])]]
         [:input.editor-form__text-field-option-condition-answer-compared-to
          {:disabled     @component-locked?
           :class        (when (not (:valid? @local-state))
                           "editor-form__text-field-option-condition-answer-compared-to--invalid")
           :id           id
           :on-blur      (partial on-blur-fn path)
           :on-change    on-change-fn
           :type         "text"
           :value        (:value @local-state)
           :data-test-id "tekstikenttä-lisäkysymys-arvon-perusteella-ehto-vertailuarvo"}]]))))

(defn- text-field-section-visibility-conditions-header [{:keys [component-locked?
                                                                condition
                                                                section-name
                                                                option-index
                                                                path]}]
  (let [hideable-sections @(subscribe [:editor/current-editor-sections])]
    [:div.editor-form__text-field-option-followups-header
     [text-field-option-condition {:condition    condition
                                   :option-index option-index
                                   :path         path}]
     [:div
      "Valitse piilotettava osio tästä"
      [:select
       {:disabled     component-locked?
        :on-change    (fn [event]
                        (dispatch [:editor/lisää-tekstikentän-arvon-perusteella-piilotettavan-osion-nimi
                                   path
                                   option-index
                                   (get-val event)]))
        :value        (or section-name (first hideable-sections))
        :data-test-id "tekstikenttä-arvon-perusteella-piilotettavan-osion-nimi"}
       (for [form-section hideable-sections]
         [:option {:value (:id form-section)} (-> form-section :label :fi)])]]                 ;TODO use transl
     [remove-option {:disabled?    component-locked?
                     :option-index option-index
                     :path         path}]]))

(defn- text-field-option-followups-header [{:keys [component-locked?
                                                   condition
                                                   followups
                                                   option-index
                                                   path
                                                   show-followups]}]
  [:div.editor-form__text-field-option-followups-header
   [text-field-option-condition {:condition    condition
                                 :option-index option-index
                                 :path         path}]
   [followup-question/followup-question option-index followups show-followups]
   [remove-option {:disabled?    component-locked?
                   :option-index option-index
                   :path         path}]])

(defn- text-field-section-visibility-condition-wrapper [props]
  (when (:condition props)
    [text-field-section-visibility-conditions-header props]))

(defn- text-field-option-followups-wrapper [{:keys [component-locked?
                                                    condition
                                                    followups
                                                    option-index
                                                    path
                                                    show-followups]}]
  [:div.editor-form__text-field-option-followups-wrapper
   {:data-test-id "tekstikenttä-lisäkysymys-lista"}
   (when condition
     [text-field-option-followups-header {:component-locked? component-locked?
                                          :condition         condition
                                          :followups         followups
                                          :option-index      option-index
                                          :path              path
                                          :show-followups    show-followups}])
   [followup-question/followup-question-overlay option-index followups path show-followups]])

(defn- initialize-show-followups [show-followups options]
  (let [option-count                   (count options)
        show-options-without-condition (not (empty? (remove :condition options)))]
    (when (or (nil? @show-followups)
              (not (= (count @show-followups) option-count)))
      (reset! show-followups (vec (replicate option-count show-options-without-condition))))))

(defn- text-field-option-followups
  [_]
  (let [show-followups (r/atom nil)]
    (fn [{:keys [component-locked? followups options path]}]
      (initialize-show-followups show-followups options)
      [:<>
       (doall
         (map-indexed
           (fn [index option]
             (let [followups (nth followups index)]
               ^{:key (str "options-" index)}
               [text-field-option-followups-wrapper {:component-locked? component-locked?
                                                     :condition         (:condition option)
                                                     :followups         followups
                                                     :option-index      index
                                                     :path              (conj path :options)
                                                     :show-followups    show-followups}]))
           options))])))

(defn- text-field-section-visibility-conditions
  [_]
  (fn [{:keys [component-locked? section-visibility-conditions path]}]
    [:<>
     (doall
       (map-indexed
         (fn [index visibility-condition]
           ^{:key (str "visibility-conditions-" index)}
           [text-field-section-visibility-condition-wrapper {:component-locked? component-locked?
                                                             :condition         (:condition visibility-condition)
                                                             :section-name      (:section-name visibility-condition)
                                                             :option-index      index
                                                             :path              (conj path :section-visibility-conditions)}])
         section-visibility-conditions))]))

(defn- text-field-has-an-option [_ _ _ _]
  (let [id (util/new-uuid)]
    (fn [value followups path component-locked?]
      (let [option-index 0
            options-without-condition? (->> (:options value) (remove :condition) empty? not)
            options-with-condition? (->> (:options value) (filter :condition) empty? not)
            repeatable? (-> value :params :repeatable boolean)
            disabled? (or component-locked?
                          options-with-condition?
                                           (not (empty? (first followups)))
                                           repeatable?)]
        [:div.editor-form__text-field-checkbox-container
         [:input.editor-form__text-field-checkbox
          {:id           id
           :type         "checkbox"
           :checked      options-without-condition?
           :disabled     disabled?
           :on-change    (fn [evt]
                           (when-not disabled?
                             (.preventDefault evt)
                             (if (-> evt .-target .-checked)
                               (dispatch [:editor/add-text-field-option path])
                               (dispatch [:editor/remove-text-field-option (conj path :options option-index)]))))
           :data-test-id "tekstikenttä-valinta-lisäkysymys"}]
         [:label.editor-form__text-field-checkbox-label
          {:for   id
           :class (when disabled? "editor-form__text-field-checkbox-label--disabled")}
          @(subscribe [:editor/virkailija-translation :lisakysymys])]]))))

(defn text-component [_ _ path & {:keys [header-label]}]
  (let [languages         (subscribe [:editor/languages])
        value             (subscribe [:editor/get-component-value path])
        sub-header        (subscribe [:editor/get-component-value path :label])
        size              (subscribe [:editor/get-component-value path :params :size])
        max-length        (subscribe [:editor/get-component-value path :params :max-length])
        radio-group-id    (util/new-uuid)
        radio-buttons     ["S" "M" "L"]
        radio-button-ids  (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        max-length-change (fn [new-val]
                            (dispatch-sync [:editor/set-component-value new-val path :params :max-length]))
        size-change       (fn [new-size]
                            (dispatch-sync [:editor/set-component-value new-size path :params :size]))
        text-area?        (= "Tekstialue" header-label)
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [initial-content followups path & {:keys [header-label _ size-label]}]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) header-label path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content/component-content
        path
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
            [copy-link (:id initial-content)]]
           (input-fields-with-lang-component/input-fields-with-lang
             (fn [lang]
               [input-field-component/input-field {:path         path
                                                   :lang         lang
                                                   :dispatch-fn  #(dispatch-sync [:editor/set-component-value
                                                                                  (get-val %)
                                                                                  path :label lang])
                                                   :data-test-id "tekstikenttä-kysymys"}])
             @languages
             :header? true)]
          [:div.editor-form__button-wrapper
           [:header.editor-form__component-item-header size-label]
           [:div.editor-form__button-group
            (doall (for [[btn-name btn-id] radio-button-ids]
                     ^{:key (str btn-id "-radio")}
                     [:div
                      [:input.editor-form__button
                       {:type      "radio"
                        :value     btn-name
                        :checked   (or
                                     (= @size btn-name)
                                     (and
                                       (nil? @size)
                                       (= "M" btn-name)))
                        :name      radio-group-id
                        :id        btn-id
                        :disabled  @component-locked?
                        :on-change (fn [] (size-change btn-name))}]
                      [:label.editor-form__button-label
                       {:for   btn-id
                        :class (button-label-class btn-name @component-locked?)}
                       btn-name]]))]
           (when text-area?
             [:div.editor-form__max-length-container
              [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :max-characters])]
              [:input.editor-form__text-field.editor-form__text-field-auto-width
               {:value     @max-length
                :disabled  @component-locked?
                :on-change #(max-length-change (get-val %))}]])]
          [:div.editor-form__checkbox-wrapper
           [validator-checkbox-component/validator-checkbox path initial-content :required (required-disabled initial-content)]
           (when-not text-area?
             [repeater-checkbox-component/repeater-checkbox path initial-content])
           (when-not text-area?
             (let [{:keys [options section-visibility-conditions]} @value
                   section-visibility-conditions? (->> section-visibility-conditions (filter :condition) empty? not)
                   options-with-condition? (->> options (filter :condition) empty? not)
                   props {:allow-decimals?                (not options-with-condition?)
                          :cannot-change-type?            (or options-with-condition? section-visibility-conditions?)
                          :component-locked?              @component-locked?
                          :decimals-in-use?               (-> @value :params :decimals pos?)
                          :followups?                     (->> followups (filter empty?) empty? not)
                          :options-with-condition?        options-with-condition?
                          :options-without-condition?     (->> options (remove :condition) empty? not)
                          :repeatable?                    (-> @value :params :repeatable boolean)
                          :section-visibility-conditions? section-visibility-conditions?}]
               [text-component-type-selector (:id initial-content) path props]))]
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]
         [:div.editor-form__text-field-checkbox-wrapper
          [info-addon-component/info-addon path]
          (when-not text-area?
            [text-field-has-an-option @value followups path @component-locked?])]
         (when-not text-area?
           [:div.editor-form__component-row-wrapper
            [text-field-option-followups {:component-locked? @component-locked?
                                          :followups         followups
                                          :options           (:options @value)
                                          :path              path}]
            [text-field-section-visibility-conditions {:component-locked?             @component-locked?
                                                       :section-visibility-conditions (:section-visibility-conditions @value)
                                                       :path                          path}]])]]])))
