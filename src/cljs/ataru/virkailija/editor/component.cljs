(ns ataru.virkailija.editor.component
  (:require [ataru.component-data.component :as component]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [ataru.virkailija.editor.components.followup-question :refer [followup-question followup-question-overlay]]
            [ataru.virkailija.temporal :as temporal]
            [ataru.cljs-util :as util :refer [cljs->str str->cljs new-uuid]]
            [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

; IE only allows this data attribute name for drag event dataTransfer
; http://stackoverflow.com/questions/26213011/html5-dragdrop-issue-in-internet-explorer-datatransfer-property-access-not-pos
(def ^:private ie-compatible-drag-data-attribute-name "Text")

(defn- required-checkbox
  [path initial-content]
  (let [id        (util/new-uuid)
        required? (true? (some? ((set (map keyword (:validators initial-content))) :required)))]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   required?
                                    :on-change (fn [event]
                                                 (dispatch [(if (-> event .-target .-checked)
                                                              :editor/add-validator
                                                              :editor/remove-validator) "required" path]))}]
     [:label.editor-form__checkbox-label {:for id} "Pakollinen tieto"]]))

(defn- repeater-checkbox
  [path initial-content]
  (let [id       (util/new-uuid)
        checked? (-> initial-content :params :repeatable)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable]))}]
     [:label.editor-form__checkbox-label {:for id} "Vastaaja voi lisätä useita vastauksia"]]))

(defn- selectable-list-item
  [path id hakukohde selected-hakukohteet get-name on-click-add on-click-remove]
  (let [
        parts (subscribe [:editor/name-parts id (get-name hakukohde)])]
    (fn [path id hakukohde selected-hakukohteet]
      (let [selected? (contains? (set selected-hakukohteet) (:oid hakukohde))]
        [(keyword
           (str "li"
             ".belongs-to-hakukohteet-modal__hakukohde-list-item"
             (when selected?
               ".belongs-to-hakukohteet-modal__hakukohde-list-item--selected")))
         {:on-click (if selected? (partial on-click-remove hakukohde) (partial on-click-add hakukohde))}
         (map-indexed (fn [i [part highlight?]]
                        ^{:key (str i)}
                        [(keyword
                           (str "span"
                             ".belongs-to-hakukohteet-modal__hakukohde-label"
                             (when selected?
                               ".belongs-to-hakukohteet-modal__hakukohde-label--selected")
                             (when highlight?
                               ".belongs-to-hakukohteet-modal__hakukohde-label--highlighted")))
                         part])
           @parts)]))))

(defn- hakukohderyhma-list-item
  [path id hakukohderyhmat selected-hakukohderyhmat]
  [:li.belongs-to-hakukohteet-modal__haku-list-item
   [:ul.belongs-to-hakukohteet-modal__hakukohde-list
    (let [on-click-add    (fn [hakukohderyhma _] (dispatch [:editor/add-to-belongs-to-hakukohderyhma
                                                       path
                                                       (:oid hakukohderyhma)]))
          on-click-remove (fn [hakukohderyhma _] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                                       path (:oid hakukohderyhma)]))
          get-name (fn [hakukohderyhma] @(subscribe [:editor/get-some-name hakukohderyhma]))]
    (for [hakukohderyhma @hakukohderyhmat]
      ^{:key (:oid hakukohderyhma)}
      [selectable-list-item path id hakukohderyhma selected-hakukohderyhmat get-name on-click-add on-click-remove]))]])

(defn- haku-list-item
  [path id haku selected-hakukohteet]
  (let [name (subscribe [:editor/get-some-name haku])
        on-click-add    (fn [hakukohde _] (dispatch [:editor/add-to-belongs-to-hakukohteet
                                           path
                                           (:oid hakukohde)]))
        on-click-remove (fn [hakukohde _] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                           path (:oid hakukohde)]))
        get-name (fn [hakukohde] @(subscribe [:editor/get-hakukohde-name hakukohde]))
        show-at-most @(subscribe [:editor/belongs-to-hakukohteet-modal-show-more-value id (:oid haku)])]
    (fn [path id haku selected-hakukohteet]
      [:li.belongs-to-hakukohteet-modal__haku-list-item
       [:span.belongs-to-hakukohteet-modal__haku-label
        @name]
       [:ul.belongs-to-hakukohteet-modal__hakukohde-list
        (for [hakukohde (first (split-at show-at-most (:hakukohteet haku)))]
          ^{:key (:oid hakukohde)}
          [selectable-list-item path id hakukohde selected-hakukohteet get-name on-click-add on-click-remove])
        (when (< show-at-most (count (:hakukohteet haku)))
          [:li.belongs-to-hakukohteet-modal__hakukohde-list-item--show-more
           {:on-click #(dispatch [:editor/belongs-to-hakukohteet-modal-show-more id (:oid haku)])}
           [:span.belongs-to-hakukohteet-modal__hakukohde-label
            "Näytä lisää.."]])]])))

(defn- belongs-to-hakukohteet-modal
  [path id selected-hakukohteet selected-hakukohderyhmat]
  (let [search-term     (subscribe [:editor/belongs-to-hakukohteet-modal-search-term-value id])
        fetching?       (subscribe [:editor/fetching-haut?])
        used-by-haku?   (subscribe [:editor/used-by-haku?])
        haut            (subscribe [:editor/filtered-haut id])
        hakukohderyhmat (subscribe [:editor/filtered-hakukohderyhmat id])
        on-click        (fn [_] (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        on-change       (fn [e] (dispatch [:editor/on-belongs-to-hakukohteet-modal-search-term-change
                                           id (.-value (.-target e))]))]
    (fn [path id selected-hakukohteet selected-hakukohderyhmat]
      [:div.belongs-to-hakukohteet-modal
       [:div.belongs-to-hakukohteet-modal__arrow-up]
       (if @used-by-haku?
         [:div.belongs-to-hakukohteet-modal__box
          [:div.belongs-to-hakukohteet-modal__input-row
           [:div.belongs-to-hakukohteet-modal__search-container
            [:input.belongs-to-hakukohteet-modal__search
             {:value     @search-term
              :on-change on-change}]]
           [:button.belongs-to-hakukohteet-modal__hide
            {:on-click on-click}
            [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
          (if @fetching?
            [:div.belongs-to-hakukohteet-modal__spinner
             [:i.zmdi.zmdi-spinner.spin]]
            [:ul.belongs-to-hakukohteet-modal__haku-list
             [hakukohderyhma-list-item path id hakukohderyhmat selected-hakukohderyhmat]
             (for [[_ haku] @haut]
               ^{:key (:oid haku)}
                 [haku-list-item path id haku selected-hakukohteet])])]
         [:div.belongs-to-hakukohteet-modal__box
          [:div.belongs-to-hakukohteet-modal__no-haku-row
           [:p.belongs-to-hakukohteet-modal__no-haku
            "Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."]
           [:button.belongs-to-hakukohteet-modal__hide
            {:on-click on-click}
            [:i.zmdi.zmdi-close.zmdi-hc-lg]]]])])))

(defn- belongs-to-hakukohderyhma
  [path oid]
  (let [name      (subscribe [:editor/belongs-to-hakukohderyhma-name oid])
        fetching? (subscribe [:editor/fetching-haut?])
        on-click  (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                     path oid]))]
    (fn [_ _]
      [:li.belongs-to-hakukohteet__hakukohde-list-item.animated.fadeIn
       [:span.belongs-to-hakukohteet__hakukohde-label
        (if @fetching?
          [:i.zmdi.zmdi-spinner.spin]
          @name)]
       [:button.belongs-to-hakukohteet__hakukohde-remove
        {:on-click on-click}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]])))

(defn- belongs-to-hakukohde
  [path oid]
  (let [name      (subscribe [:editor/belongs-to-hakukohde-name oid])
        fetching? (subscribe [:editor/fetching-haut?])
        on-click  (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                     path oid]))]
    (fn [_ _]
      [:li.belongs-to-hakukohteet__hakukohde-list-item.animated.fadeIn
       [:span.belongs-to-hakukohteet__hakukohde-label
        (if @fetching?
          [:i.zmdi.zmdi-spinner.spin]
          @name)]
       [:button.belongs-to-hakukohteet__hakukohde-remove
        {:on-click on-click}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]])))

(defn- belongs-to-hakukohteet
  [path initial-content]
  (let [id              (:id initial-content)
        on-click-show   (fn [_]
                          (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide   (fn [_]
                          (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?     (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        modal-toggle-id (util/new-uuid)]
    (fn [path initial-content]
      (let [visible-to (:belongs-to-hakukohteet initial-content)
            visible-to-hakukohderyhmat (:belongs-to-hakukohderyhma initial-content)]
        [:div.belongs-to-hakukohteet
         [:label.belongs-to-hakukohteet__modal-toggle-label
          {:for modal-toggle-id}
          "Näkyvyys lomakkeella: "]
         [:button.belongs-to-hakukohteet__modal-toggle
          {:id       modal-toggle-id
           :on-click (if @show-modal? on-click-hide on-click-show)}
          (if (empty? visible-to)
            "näkyy kaikille"
            "vain valituille hakukohteille")]
         (when @show-modal?
           [belongs-to-hakukohteet-modal path (:id initial-content) visible-to visible-to-hakukohderyhmat])
         [:ul.belongs-to-hakukohteet__hakukohde-list
          (for [oid visible-to]
            ^{:key oid}
            [belongs-to-hakukohde path oid])
          (for [oid visible-to-hakukohderyhmat]
            ^{:key oid}
            [belongs-to-hakukohderyhma path oid])]]))))

(defn- on-drag-start
  [path]
  (fn [event]
    (-> event .-dataTransfer (.setData ie-compatible-drag-data-attribute-name (util/cljs->str path)))))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- fade-out-effect
  [path]
  (reaction (case @(subscribe [:state-query [:editor :forms-meta path]])
              :fade-out "fade-out"
              :fade-in "animated fadeInUp"
              nil)))

(defn- remove-component-button [component-wrapped? path]
  (case @(subscribe [:editor/remove-component-button-state path])
    :active
    [:button.editor-form__remove-component-button
     {:on-click #(dispatch [:editor/start-remove-component path])}
     "Poista"]
    :confirm
    [:button.editor-form__remove-component-button--confirm.editor-form__remove-component-button
     {:on-click (fn [event]
                  (let [target (-> event
                                 .-target
                                 (gdom/getAncestorByClass
                                   (if component-wrapped?
                                     "editor-form__section_wrapper"
                                     "editor-form__component-wrapper")))]
                    (set! (.-height (.-style target)) (str (.-offsetHeight target) "px"))
                    (dispatch [:editor/confirm-remove-component path])))}
     "Vahvista poisto"]
    :disabled
    [:button.editor-form__remove-component-button--disabled.editor-form__remove-component-button
     {:disabled true}
     "Vahvista poisto"]))

(defn copy [id]
  (let [copy-container (.getElementById js/document "editor-form__copy-question-id-container")]
    (set! (.-value copy-container) id)
    (.select copy-container)
    (.execCommand js/document "copy")))

(defn- copy-link [id]
  [:a.editor-form__copy-question-id
   {:data-tooltip "Kopioi kysymyksen tunniste leikepöydälle"
    :on-mouse-down #(copy id)}
   "id"])

(defn- header-metadata
  [metadata]
  [:span.editor-form__component-main-header-metadata
   "Luonut " (-> metadata :created-by :name)
   ", viimeksi muokannut " (when-not (= (-> metadata :created-by :oid)
                                       (-> metadata :modified-by :oid))
                             (-> metadata :modified-by :name))
   " " (-> metadata :modified-by :date temporal/str->googdate temporal/time->date)])

(defn- text-header
  [label path metadata & {:keys [component-wrapped?
                        draggable
                        sub-header
                        on-fold-click]
                 :or   {draggable true}}]
  [:div.editor-form__header-wrapper
   {:draggable     draggable
    :on-drag-start (on-drag-start path)
    :on-drag-over  prevent-default}
   [:header.editor-form__component-header
    (when (some? on-fold-click)
      [:button.editor-form__component-fold-button
       {:on-click on-fold-click}
       (if (some? sub-header)
         [:i.zmdi.zmdi-chevron-down]
         [:i.zmdi.zmdi-chevron-up])])
    [:span.editor-form__component-main-header
     label]
    (when metadata (header-metadata metadata))
    [:span.editor-form__component-sub-header
     {:class (if (some? sub-header)
               "editor-form__component-sub-header-visible"
               "editor-form__component-sub-header-hidden")}
     (when (some? sub-header)
       (->> [:fi :sv :en]
            (map (partial get sub-header))
            (remove clojure.string/blank?)
            (clojure.string/join " - ")))]]
   [remove-component-button component-wrapped? path]])

(defn markdown-help []
  [:div.editor-form__markdown-help
   [:div
    [:div.editor-form__markdown-help-arrow-left]
    [:div.editor-form__markdown-help-content
     [:span "# otsikko (# ylin - ###### alin)"]
     [:br]
     [:span "**lihavoitava sisältö**"]
     [:br]
     [:span "*kursivoitava sisältö*"]
     [:br]
     [:span "[linkin teksti](http://linkin osoite)"]
     [:br]
     [:a {:href          "https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet"
          :target        "_blank"
          :on-mouse-down (fn [evt]
                           (let [url (.getAttribute (-> evt .-target) "href")]
                             (.open js/window url "_blank")))}
      "Lisää muotoiluohjeita"]]]])

(defn input-field [path lang dispatch-fn {:keys [class value-fn tag]
                                          :or   {tag :input}}]
  (let [component (subscribe [:editor/get-component-value path])
        focus?    (subscribe [:state-query [:editor :ui (:id @component) :focus?]])
        value     (or
                    (when value-fn
                      (reaction (value-fn @component)))
                    (reaction (get-in @component [:label lang])))
        languages (subscribe [:editor/languages])]
    (r/create-class
      {:component-did-mount (fn [component]
                              (when (cond-> @focus?
                                      (> (count @languages) 1)
                                      (and (= (first @languages) lang)))
                                (let [dom-node (r/dom-node component)]
                                  (.focus dom-node))))
       :reagent-render      (fn [_ _ _ _]
                              [tag
                               {:class     (str "editor-form__text-field " (when-not (empty? class) class))
                                :value     @value
                                :on-change dispatch-fn
                                :on-drop   prevent-default}])})))

(defn- add-multi-lang-class [field-spec]
  (let [multi-lang-class "editor-form__text-field-wrapper--with-label"]
    (if (map? (last field-spec))
      (assoc-in field-spec [(dec (count field-spec)) :class] multi-lang-class)
      (conj field-spec {:class multi-lang-class}))))

(defn- input-fields-with-lang [field-fn languages & {:keys [header?] :or {header? false}}]
  (let [multiple-languages? (> (count languages) 1)]
    (map-indexed (fn [idx lang]
                   (let [field-spec (field-fn lang)]
                     ^{:key (str "option-" lang "-" idx)}
                     [:div.editor-form__text-field-container
                      (when-not header?
                        {:class "editor-form__multi-option-wrapper"})
                      (cond-> field-spec
                        multiple-languages? add-multi-lang-class)
                      (when multiple-languages?
                        [:div.editor-form__text-field-label (-> lang name clojure.string/upper-case)])]))
      languages)))

(defn- koodisto-field [path lang]
  (let [component (subscribe [:editor/get-component-value path])
        value     (get-in @component [:label lang])]
    [:div.editor-form__koodisto-field
     {:on-drop prevent-default}
     value]))

(defn- koodisto-fields-with-lang [languages option-path]
  (let [multiple-languages? (> (count languages) 1)
        component           @(subscribe [:editor/get-component-value option-path])]
    [:div
     {:title (clojure.string/join ", " (map (fn [lang] (get-in component [:label lang])) languages))}
     (map-indexed (fn [idx lang]
                    (let [field-spec (koodisto-field option-path lang)]
                      ^{:key (str "option-" lang "-" idx)}
                      [:div.editor-form__koodisto-field-container
                       field-spec])) languages)]))

(defn info-addon
  "Info text which is added to an existing component"
  [path]
  (let [id        (util/new-uuid)
        checked?  (reaction (some? @(subscribe [:editor/get-component-value path :params :info-text :label])))
        languages (subscribe [:editor/languages])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       [:div.editor-form__info-addon-checkbox
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :on-change (fn [event]
                              (dispatch [:editor/set-component-value
                                         (if (-> event .-target .-checked) {:fi "" :sv "" :en ""} nil)
                                         path :params :info-text :label]))}]
        [:label {:for id} "Kysymys sisältää ohjetekstin"]]
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang
                 (fn [lang]
                   [input-field
                    (concat path [:params :info-text])
                    lang
                    #(dispatch-sync [:editor/set-component-value
                                     (-> % .-target .-value)
                                     path :params :info-text :label lang])
                    {:tag :textarea}])
                 @languages)
            (map (fn [field]
                   (into field [[:div.editor-form__info-addon-markdown-anchor
                                 (markdown-help)]]))))])])))

(defn- get-val [event]
  (-> event .-target .-value))

(defn- decimal-places-selector [path]
  (let [decimal-places (subscribe [:editor/get-component-value path :params :decimals])]
    (fn [path]
      [:div.editor-form__additional-params-container
       [:header.editor-form__component-item-header "Muoto:"]
       [:select.editor-form__decimal-places-selector
        {:value     (or @decimal-places "")
         :on-change (fn [e]
                      (let [new-val (get-val e)
                            value   (when (not-empty new-val)
                                      (js/parseInt new-val))]
                        (dispatch [:editor/set-component-value value path :params :decimals])))}
        [:option {:value "" :key 0} "kokonaisluku"]
        (for [i (range 1 5)]
          [:option {:value i :key i} (str i " desimaalia")])]])))

(defn- text-component-type-selector [path radio-group-id]
  (let [id       (util/new-uuid)
        checked? (subscribe [:editor/get-component-value path :params :numeric])]
    (fn [path radio-group-id]
      [:div
       [:div.editor-form__checkbox-container
        [:input.editor-form__checkbox
         {:type      "checkbox"
          :id        id
          :checked   (or @checked? false)
          :on-change (fn [event]
                       (let [checked-now? (-> event .-target .-checked)]
                         (dispatch [:editor/set-component-value checked-now? path :params :numeric])
                         (dispatch [(if checked-now?
                                      :editor/add-validator
                                      :editor/remove-validator) "numeric" path])
                         (when-not checked-now?
                           (dispatch [:editor/set-component-value nil path :params :decimals]))))}]
        [:label.editor-form__checkbox-label
         {:for id}
         "Kenttään voi täyttää vain numeroita"]]
       (when @checked?
         [decimal-places-selector path])])))

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages         (subscribe [:editor/languages])
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
        animation-effect  (fade-out-effect path)]
    (fn [initial-content path & {:keys [header-label size-label]}]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header header-label path (:metadata initial-content)]
       [:div.editor-form__component-row-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Kysymys"
          [copy-link (:id initial-content)]]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (get-val %) path :label lang])])
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
                      :on-change (fn [] (size-change btn-name))}]
                    [:label
                     {:for   btn-id
                      :class (match btn-name
                               "S" "editor-form__button--left-edge"
                               "L" "editor-form__button--right-edge"
                               :else nil)}
                     btn-name]]))]
         (when text-area?
           [:div.editor-form__max-length-container
            [:header.editor-form__component-item-header "Max. merkkimäärä"]
            [:input.editor-form__text-field.editor-form__text-field-auto-width
             {:value     @max-length
              :on-change #(max-length-change (get-val %))}]])]
        [:div.editor-form__checkbox-wrapper
         [required-checkbox path initial-content]
         (when-not text-area?
           [repeater-checkbox path initial-content])
         (when-not text-area?
           [text-component-type-selector path radio-group-id])]
        [belongs-to-hakukohteet path initial-content]]
       [info-addon path]])))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän koko"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen koko"])

(defn- remove-dropdown-option-button [path option-index]
  [:a.editor-form__multi-options-remove--cross {:on-click (fn [evt]
                                                            (.preventDefault evt)
                                                            (dispatch [:editor/remove-dropdown-option path :options option-index]))}
   [:i.zmdi.zmdi-close.zmdi-hc-lg]])



(defn- dropdown-option [option-index option-path path languages & {:keys [header? include-followup? editable?] :or {header? false include-followup? true editable? true} :as opts}]
  (let [multiple-languages? (< 1 (count languages))]
    ^{:key (str "options-" option-index)}
    [:div
     [:div.editor-form__multi-options-wrapper-outer
      [:div
       (cond-> {:key (str "options-" option-index)}
         multiple-languages?
         (assoc :class "editor-form__multi-options-wrapper-inner"))
       (if editable?
         (input-fields-with-lang
           (fn [lang]
             [input-field option-path lang #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) option-path :label lang])])
           languages)
         (koodisto-fields-with-lang languages option-path))]
      (when editable?
        [remove-dropdown-option-button path option-index])
      (when include-followup?
        [followup-question option-path])]
     (when include-followup?
       [followup-question-overlay option-path])]))

(defn- dropdown-multi-options [path options-koodisto]
  (let [dropdown-id                (util/new-uuid)
        custom-button-value        "Omat vastausvaihtoehdot"
        custom-button-id           (str dropdown-id "-custom")
        koodisto-button-value      (reaction (str "Koodisto" (if-let [koodisto-name (:title @options-koodisto)] (str ": " koodisto-name) "")))
        koodisto-button-id         (str dropdown-id "-koodisto")
        koodisto-popover-expanded? (r/atom false)]
    (fn [path options-koodisto]
      [:div.editor-form__button-group
       [:input
        {:type      "radio"
         :class     "editor-form__button editor-form__button--large"
         :value     custom-button-value
         :checked   (nil? @options-koodisto)
         :name      dropdown-id
         :id        custom-button-id
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? false)
                      (dispatch [:editor/select-custom-multi-options path]))}]
       [:label
        {:for   custom-button-id
         :class "editor-form__button--left-edge"}
        custom-button-value]
       [:input
        {:type      "radio"
         :class     "editor-form__button editor-form__button--large"
         :value     @koodisto-button-value
         :checked   (not (nil? @options-koodisto))
         :name      dropdown-id
         :id        koodisto-button-id
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? true))}]
       [:label
        {:for   koodisto-button-id
         :class "editor-form__button--right-edge"}
        @koodisto-button-value]
       (when @koodisto-popover-expanded?
         [:div.editor-form__koodisto-popover
          [:div.editor-form__koodisto-popover-header "Koodisto"
           [:a.editor-form__koodisto-popover-close
            {:on-click (fn [e]
                         (.preventDefault e)
                         (reset! koodisto-popover-expanded? false))}
            [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
          [:ul.editor-form__koodisto-popover-list
           (doall (for [{:keys [uri title version]} koodisto-whitelist/koodisto-whitelist]
                    ^{:key (str "koodisto-" uri)}
                    [:li.editor-form__koodisto-popover-list-item
                     [:a.editor-form__koodisto-popover-link
                      {:on-click (fn [e]
                                   (.preventDefault e)
                                   (reset! koodisto-popover-expanded? false)
                                   (dispatch [:editor/select-koodisto-options uri version title path]))}
                      title]]))]])])))

(defn- custom-answer-options [languages options path question-group-element? editable?]
  [:div.editor-form__multi-options-container
   (map-indexed (fn [idx _]
                  (dropdown-option idx [path :options idx] path languages
                                   :editable? editable?
                                   :include-followup? (not question-group-element?)))
                options)])

(defn koodisto-answer-options [id path selected-koodisto question-group-element?]
  (let [opened? (r/atom false)]
    (fn [id path selected-koodisto question-group-element?]
      (let [languages             @(subscribe [:editor/languages])
            value                 @(subscribe [:editor/get-component-value path])
            field-type            (:fieldType value)
            editable?             false
            hide-koodisto-options (fn [evt]
                                    (reset! opened? false))
            show-koodisto-options (fn [evt]
                                    (swap! opened? (fn [old]
                                                     (if (not old)
                                                       (dispatch [:editor/fetch-koodisto-for-component-with-id id selected-koodisto]))
                                                     true)))]
        (if (not @opened?)
          [:div.editor-form__show-koodisto-values
           [:a
            {:on-click show-koodisto-options}
            [:i.zmdi.zmdi-chevron-down] " Näytä vastausvaihtoehdot"]]
          [:div
           [:div.editor-form__show-koodisto-values
            [:a
             {:on-click hide-koodisto-options}
             [:i.zmdi.zmdi-chevron-up] " Sulje vastausvaihtoehdot"]]
           (custom-answer-options languages (:options value) path question-group-element? editable?)])))))

(defn dropdown [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        options-koodisto (subscribe [:editor/get-component-value path :koodisto-source])
        value            (subscribe [:editor/get-component-value path])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path {:keys [question-group-element?]}]
      (let [languages  @languages
            field-type (:fieldType @value)]
        [:div.editor-form__component-wrapper
         {:class @animation-effect}
         (let [header (case field-type
                        "dropdown" "Pudotusvalikko"
                        "singleChoice" "Painikkeet, yksi valittavissa"
                        "multipleChoice" "Lista, monta valittavissa")]
           [text-header header path (:metadata initial-content)])
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__multi-question-wrapper
           [:div.editor-form__text-field-wrapper
            [:header.editor-form__component-item-header "Kysymys"
             [copy-link (:id initial-content)]]
            (input-fields-with-lang
              (fn [lang]
                [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
              languages
              :header? true)]
           [:div.editor-form__checkbox-wrapper
            [required-checkbox path initial-content]]
           [belongs-to-hakukohteet path initial-content]]]
         [info-addon path initial-content]
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__multi-options_wrapper
           [:div.editor-form--padded
            [:header.editor-form__component-item-header "Vastausvaihtoehdot"]
            (when-not (= field-type "singleChoice") [dropdown-multi-options path options-koodisto])]
           (if (nil? @options-koodisto)
             (custom-answer-options languages (:options @value) path question-group-element? true)
             [koodisto-answer-options (:id @value) path @options-koodisto question-group-element?])
           (when (nil? @options-koodisto)
             [:div.editor-form__add-dropdown-item
              [:a
               {:on-click (fn [evt]
                            (.preventDefault evt)
                            (dispatch [:editor/add-dropdown-option path]))}
               [:i.zmdi.zmdi-plus-square] " Lisää"]])]]]))))

(defn drag-n-drop-spacer [path content]
  (let [expanded? (r/atom false)]
    (fn [path content]
      [:div
       {:on-drop       (fn [event]
                         (.preventDefault event)
                         (reset! expanded? false)
                         (let [source-path (-> event .-dataTransfer (.getData ie-compatible-drag-data-attribute-name) util/str->cljs)]
                           (dispatch [:editor/move-component source-path path])))
        :on-drag-enter (fn [event] (.preventDefault event)) ;; IE needs this, otherwise on-drag-over doesn't occur
        :on-drag-over  (fn [event]
                         (.preventDefault event)
                         (reset! expanded? true)
                         nil)
        :on-drag-leave (fn [event]
                         (.preventDefault event)
                         (reset! expanded? false)
                         nil)
        :class         (if (and
                             (= 1 (count path))
                             (contains? content :children))
                         "editor-form__drag_n_drop_spacer_container_for_component_group"
                         "editor-form__drag_n_drop_spacer_container_for_component")}
       [:div
        {:class (if @expanded?
                  "editor-form__drag_n_drop_spacer--dashbox-visible"
                  "editor-form__drag_n_drop_spacer--dashbox-hidden")}]])))

(defn component-group [content path children]
  (let [languages         (subscribe [:editor/languages])
        value             (subscribe [:editor/get-component-value path])
        folded?           (subscribe [:editor/folded? (:id content)])
        animation-effect  (fade-out-effect path)
        group-header-text (case (:fieldClass content)
                            "wrapperElement" "Lomakeosio"
                            "questionGroup" "Kysymysryhmä")
        header-label-text (case (:fieldClass content)
                            "wrapperElement" "Osion nimi"
                            "questionGroup" "Kysymysryhmän otsikko")]
    (fn [content path children]
      (let [languages @languages
            value     @value]
        (if @folded?
          [:div.editor-form__section_wrapper
           {:class @animation-effect}
           [:div.editor-form__component-wrapper
            [text-header group-header-text path (:metadata content)
             :component-wrapped? true
             :sub-header (:label value)
             :on-fold-click #(dispatch [:editor/unfold (:id content)])]]]
          [:div.editor-form__section_wrapper
           {:class @animation-effect}
           [:div.editor-form__component-wrapper
            [text-header group-header-text path (:metadata content)
             :component-wrapped? true
             :on-fold-click #(dispatch [:editor/fold (:id content)])]
            [:div.editor-form__text-field-wrapper.editor-form__text-field--section
             [:header.editor-form__component-item-header header-label-text]
             (input-fields-with-lang
               (fn [lang]
                 [input-field path lang #(dispatch-sync [:editor/set-component-value
                                                         (-> % .-target .-value)
                                                         path
                                                         :label lang])])
               languages
               :header? true)]]
           children
           [drag-n-drop-spacer (conj path :children (count children))]
           (case (:fieldClass content)
             "wrapperElement" [toolbar/add-component (conj path :children (count children))]
             "questionGroup" [toolbar/question-group-toolbar path
                              (fn [generate-fn]
                                (dispatch [:generate-component generate-fn (conj path :children (count children))]))])])))))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
              "wrapperElement" (map #(recursively-get-labels %) (:children component))
              :else (-> component :label lang)))]
    (flatten (recursively-get-labels component))))

(defn hakukohteet-module [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      [:div.editor-form__module-wrapper
       [:header.editor-form__module-header
        [:span.editor-form__module-header-label (get-in @value [:label :fi])]
        " "
        [:span (get-in @value [:label-amendment :fi])]]
       [:div.editor-form__module-fields
        "Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."]])))

(defn module [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      [:div.editor-form__module-wrapper
       [:header.editor-form__module-header
        [:span.editor-form__module-header-label (get-in @value [:label :fi])]]
       [:div.editor-form__module-fields
        [:span.editor-form__module-fields-label "Sisältää kentät:"]
        " "
        (clojure.string/join ", " (get-leaf-component-labels @value :fi))]])))

(defn info-element
  "Info text which is a standalone component"
  [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Infoteksti" path (:metadata initial-content)]
       [:div.editor-form__component-row-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Otsikko"]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)
         [:div.infoelement
          [:header.editor-form__component-item-header "Teksti"]
          (->> (input-fields-with-lang
                 (fn [lang]
                   [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :text lang])
                    {:value-fn (fn [component] (get-in component [:text lang]))
                     :tag      :textarea}])
                 @languages
                 :header? true)
            (map (fn [field]
                   (into field [[:div.editor-form__markdown-anchor
                                 (markdown-help)]]))))]]
        [:div.editor-form__checkbox-wrapper]
        [belongs-to-hakukohteet path initial-content]]])))

(defn adjacent-fieldset [content path children]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Vierekkäiset tekstikentät" path (:metadata content)]
       [:div.editor-form__component-row-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Otsikko"]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__checkbox-wrapper
         [repeater-checkbox path content]]
        [belongs-to-hakukohteet path content]]
       [info-addon path]
       [:div.editor-form__adjacent-fieldset-container
        children
        (when (-> (count children) (< 3))
          [toolbar/adjacent-fieldset-toolbar
           (concat path [:children])
           (fn [component-fn]
             (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]])))

(defn adjacent-text-field [content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Tekstikenttä" path (:metadata content) :draggable false]
       [:div.editor-form__component-row-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Kysymys"
          [copy-link (:id content)]]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__checkbox-wrapper
         [required-checkbox path content]]
        [belongs-to-hakukohteet path content]]])))

(defn attachment-textarea [path]
  (let [id        (util/new-uuid)
        checked?  (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        languages (subscribe [:editor/languages])]
    (fn [path]
      [:div.editor-form__text-field-wrapper.infoelement
       [:div.editor-form__attachment-info-checkbox-wrapper
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :on-change (fn toggle-attachment-textarea [event]
                              (.preventDefault event)
                              (let [checked? (.. event -target -checked)]
                                (dispatch [:editor/set-component-value checked? path :params :info-text :enabled?])))}]
        [:label
         {:for id}
         "Liitepyyntö sisältää ohjetekstin"]]
       (when @checked?
         (->> (input-fields-with-lang
                (fn attachment-textarea-input [lang]
                  [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :params :info-text :value lang])
                   {:value-fn #(get-in % [:params :info-text :value lang])
                    :tag      :textarea}])
                @languages
                :header? true)
           (map (fn [field]
                  (into field [[:div.editor-form__markdown-anchor
                                (markdown-help)]])))))])))

(defn attachment [content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Liitepyyntö" path (:metadata content)]
       [:div.editor-form__component-row-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Liitteen nimi"
          [copy-link (:id content)]]
         (input-fields-with-lang
           (fn attachment-file-name-input [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__single-checkbox-wrapper
         [required-checkbox path content]]
        [belongs-to-hakukohteet path content]]
       [attachment-textarea path]])))
