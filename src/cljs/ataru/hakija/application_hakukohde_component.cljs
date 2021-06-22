(ns ataru.hakija.application-hakukohde-component
  (:require
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ataru.application-common.application-field-common :refer [scroll-to-anchor]]
    [ataru.util :as util]
    [ataru.translations.translation-util :as translations]
    [reagent.core :as r]
    [ataru.hakija.application-hakukohde-2nd-component :as hakukohde-2nd]))

(defn hilighted-text->span [idx {:keys [text hilight]}]
  [(if hilight
     :span.application__search-hit-hakukohde-row--hilight-span.application__search-hit-hakukohde-row--hilight-span__hilight
     :span.application__search-hit-hakukohde-row--hilight-span)
   {:key (str "hilight-" idx)}
   text])

(defn hilight-text [text hilight-text]
  (if (some? text)
    (map-indexed hilighted-text->span (util/match-text text (clojure.string/split hilight-text #"\s+") false))
    [:span ""]))

(defn- hakukohde-remove-event-handler [e]
  (dispatch [:application/hakukohde-remove-selection (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-select-event-handler [e]
  (dispatch [:application/hakukohde-add-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-search-toggle-event-handler [_]
  (dispatch [:application/hakukohde-search-toggle]))

(defn- selected-hakukohde-row-remove
  [hakukohde-oid disabled?]
  (let [lang @(subscribe [:application/form-language])]
    [:button.application__selected-hakukohde-row--remove
     {:data-hakukohde-oid hakukohde-oid
      :disabled           disabled?
      :on-click           (when (not disabled?)
                            hakukohde-remove-event-handler)}
     (translations/get-hakija-translation :remove lang)]))

(defn- selected-hakukohde-increase-priority
  [hakukohde-oid priority-number disabled?]
  (let [increase-disabled? (= priority-number 1)]
    [:span.application__selected-hakukohde-row--priority-increase
     {:disabled (when disabled? "disabled")
      :class    (when increase-disabled? "disabled")
      :on-click (when-not increase-disabled?
                  #(dispatch [:application/change-hakukohde-priority hakukohde-oid -1]))}]))

(defn- selected-hakukohde-decrease-priority
  [hakukohde-oid priority-number disabled?]
  (let [selected-hakukohteet @(subscribe [:application/selected-hakukohteet])
        decrease-disabled?   (= priority-number (count selected-hakukohteet))]
    [:span.application__selected-hakukohde-row--priority-decrease
     {:disabled (when disabled? "disabled")
      :class    (when decrease-disabled? "disabled")
      :on-click (when-not decrease-disabled?
                  #(dispatch [:application/change-hakukohde-priority hakukohde-oid 1]))}]))

(defn- prioritize-hakukohde-buttons
  ;; TODO: Katso missä käytössä
  [hakukohde-oid disabled?]
  (let [priority-number @(subscribe [:application/hakukohde-priority-number hakukohde-oid])]
    [:div.application__selected-hakukohde-row--priority-changer
     [selected-hakukohde-increase-priority hakukohde-oid priority-number disabled?]
     priority-number
     [selected-hakukohde-decrease-priority hakukohde-oid priority-number disabled?]]))

(defn- offending-priorization [should-be-higher should-be-lower]
  (let [lang @(subscribe [:application/form-language])]
    [:div.application__selected-hakukohde-row--offending-priorization
     [:i.zmdi.zmdi-alert-circle]
     [:div
      [:div.application__selected-hakukohde-row--offending-priorization-heading
       (translations/get-hakija-translation :application-priorization-invalid lang)]
      [:div
       (first (translations/get-hakija-translation :should-be-higher-priorization-than lang))
       [:em (str "\"" @(subscribe [:application/hakukohde-label should-be-higher]) "\"")]
       (last (translations/get-hakija-translation :should-be-higher-priorization-than lang))
       [:em (str "\"" @(subscribe [:application/hakukohde-label should-be-lower]) "\"")]]]]))

(defn- selected-hakukohde-row
  [hakukohde-oid]
  (let [deleting?                          @(subscribe [:application/hakukohde-deleting? hakukohde-oid])
        prioritize-hakukohteet?            @(subscribe [:application/prioritize-hakukohteet?])
        haku-editable?                     @(subscribe [:application/hakukohteet-editable?])
        hakukohde-editable?                @(subscribe [:application/hakukohde-editable? hakukohde-oid])
        [should-be-lower should-be-higher] @(subscribe [:application/hakukohde-offending-priorization? hakukohde-oid])
        rajaavat-hakukohteet               @(subscribe [:application/rajaavat-hakukohteet hakukohde-oid])
        lang                               @(subscribe [:application/form-language])]
    [:div.application__selected-hakukohde-row.animated
     {:class (if deleting? "fadeOut" "fadeIn")}
     (when prioritize-hakukohteet?
       [prioritize-hakukohde-buttons hakukohde-oid (not hakukohde-editable?)])
     [:div.application__selected-hakukohde-row--content
      [:div.application__hakukohde-header
       @(subscribe [:application/hakukohde-label hakukohde-oid])]
      [:div.application__hakukohde-description
       @(subscribe [:application/hakukohde-description hakukohde-oid])]
      (when (not hakukohde-editable?)
        [:div.application__hakukohde-application-period-ended
         [:i.zmdi.zmdi-lock]
         (translations/get-hakija-translation :not-editable-application-period-ended lang)])
      (when (not-empty rajaavat-hakukohteet)
        [:div.application__search-hit-hakukohde-row--limit-reached
         [:h3.application__search-hit-hakukohde-row--limit-reached-heading
          (translations/get-hakija-translation :application-limit-reached-in-hakukohderyhma lang)]
         (doall (for [hakukohde rajaavat-hakukohteet]
                  ^{:key (str "limitting-hakukohde-" (:oid hakukohde))}
                  [:div.application__search-hit-hakukohde-row--limitting-hakukohde
                   @(subscribe [:application/hakukohde-label (:oid hakukohde)])]))])
      (if (seq should-be-higher)
        (offending-priorization (first should-be-higher) hakukohde-oid)
        (if (seq should-be-lower)
          (offending-priorization hakukohde-oid (first should-be-lower))))]
     [:div.application__selected-hakukohde-row--buttons
      (cond (and haku-editable? hakukohde-editable?)
            [selected-hakukohde-row-remove hakukohde-oid false]
            (not hakukohde-editable?)
            [selected-hakukohde-row-remove hakukohde-oid true]
            haku-editable?
            [selected-hakukohde-row-remove hakukohde-oid false])]]))

(defn- search-hit-hakukohde-row
  [hakukohde-oid]
  (let [hakukohde-selected?  @(subscribe [:application/hakukohde-selected? hakukohde-oid])
        search-term          @(subscribe [:application/hakukohde-query])
        aria-header-id       (str "hakukohde-search-hit-header-" hakukohde-oid)
        aria-description-id  (str "hakukohde-search-hit-description-" hakukohde-oid)
        hakukohde-editable?  @(subscribe [:application/hakukohde-editable? hakukohde-oid])
        hakukohteet-full?    @(subscribe [:application/hakukohteet-full? hakukohde-oid])
        rajaavat-hakukohteet @(subscribe [:application/rajaavat-hakukohteet hakukohde-oid])
        lang                 @(subscribe [:application/form-language])]
    [:div.application__search-hit-hakukohde-row
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       (hilight-text @(subscribe [:application/hakukohde-label hakukohde-oid]) search-term)]
      [:div.application__hakukohde-description
       {:id aria-description-id}
       (hilight-text @(subscribe [:application/hakukohde-description hakukohde-oid]) search-term)]
      (when (not hakukohde-editable?)
        [:div.application__hakukohde-application-period-ended
         [:i.zmdi.zmdi-lock]
         (translations/get-hakija-translation :not-selectable-application-period-ended lang)])
      (when (and (not hakukohde-selected?)
                 (not-empty rajaavat-hakukohteet))
        [:div.application__search-hit-hakukohde-row--limit-reached
         [:h3.application__search-hit-hakukohde-row--limit-reached-heading
          (translations/get-hakija-translation :application-limit-reached-in-hakukohderyhma lang)]
         (doall (for [hakukohde rajaavat-hakukohteet]
                  ^{:key (str "limitting-hakukohde-" (:oid hakukohde))}
                  [:div.application__search-hit-hakukohde-row--limitting-hakukohde
                   @(subscribe [:application/hakukohde-label (:oid hakukohde)])]))])]
     [:div.application__search-hit-hakukohde-row--buttons
      (if hakukohde-selected?
        [:i.zmdi.zmdi-check.zmdi-hc-2x.application__search-hit-hakukohde-row--selected-check]
        [:button.application__search-hit-hakukohde-row--select-button
         {:data-hakukohde-oid hakukohde-oid
          :on-click           hakukohde-select-event-handler
          :disabled           (or hakukohteet-full?
                                  (not hakukohde-editable?)
                                  (not-empty rajaavat-hakukohteet))
          :aria-labelledby    aria-header-id
          :aria-describedby   aria-description-id}
         (translations/get-hakija-translation :add lang)])]]))

(defn- hakukohde-selection-search
  []
  (let [hakukohde-hits         (subscribe [:application/hakukohde-hits])
        prioritize-hakukohteet (subscribe [:application/prioritize-hakukohteet?])
        search-input           (r/atom @(subscribe [:application/hakukohde-query]))
        lang                   (subscribe [:application/form-language])]
    (fn []
      [:div.application__hakukohde-selection
       [:div.application__hakukohde-selection-search-arrow-up
        {:class (when @prioritize-hakukohteet
                  "application__hakukohde-selection-search-arrow-up--prioritized")}]
       [:div.application__hakukohde-selection-search-container
        [:div.application__hakukohde-selection-search-close-button
         [:a {:on-click hakukohde-search-toggle-event-handler}
          [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
        [:div.application__hakukohde-selection-search-input.application__form-text-input-box
         [:input.application__form-text-input-in-box
          {:on-change   #(do (reset! search-input (.-value (.-target %)))
                             (dispatch [:application/hakukohde-query-change search-input]))
           :title       (translations/get-hakija-translation :search-application-options @lang)
           :placeholder (translations/get-hakija-translation :search-application-options @lang)
           :value       @search-input}]
         (when (not (empty? @search-input))
           [:div.application__form-clear-text-input-in-box
            [:a
             {:on-click #(do (reset! search-input "")
                             (dispatch [:application/hakukohde-query-change search-input]))}
             [:i.zmdi.zmdi-close]]])]
        [:div.application__hakukohde-selection-search-results
         (if (and
               (empty? @hakukohde-hits)
               (not (clojure.string/blank? @search-input)))
           [:div.application__hakukohde-selection-search-no-hits (translations/get-hakija-translation :no-hakukohde-search-hits @lang)]
           (for [hakukohde-oid @hakukohde-hits]
             ^{:key (str "found-hakukohde-row-" hakukohde-oid)}
             [search-hit-hakukohde-row hakukohde-oid]))]
        (when @(subscribe [:application/show-more-hakukohdes?])
          [:div.application__show_more_hakukohdes_container
           [:span.application__show_more_hakukohdes
            {:on-click #(dispatch [:application/show-more-hakukohdes])}
            (translations/get-hakija-translation :show-more @lang)]])]])))

(defn- hakukohde-selection-header
  [field-descriptor]
  [:div.application__wrapper-heading
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [scroll-to-anchor field-descriptor]])

(defn- select-new-hakukohde-row []
  (let [lang @(subscribe [:application/form-language])]
    (when @(subscribe [:application/hakukohteet-editable?])
      (if @(subscribe [:application/hakukohteet-full?])
        (let [max-hakukohteet @(subscribe [:application/max-hakukohteet])]
          [:span.application__hakukohde-max-selected
           (translations/get-hakija-translation :applications_at_most lang max-hakukohteet)])
        [:div.application__hakukohde-selection-open-search-wrapper
         [:a.application__hakukohde-selection-open-search
          {:on-click hakukohde-search-toggle-event-handler}
          (translations/get-hakija-translation :add-application-option lang)]]))))

(defn hakukohteet
  [field-descriptor _]
  [:div.application__wrapper-element
   [hakukohde-selection-header field-descriptor]
   [:div.application__wrapper-contents.application__hakukohde-contents-wrapper
    [:div.application__form-field
     [:div.application__hakukohde-selected-list
      (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
        ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
        [selected-hakukohde-row hakukohde-oid])]
     [select-new-hakukohde-row]
     (when @(subscribe [:application/show-hakukohde-search])
       [hakukohde-selection-search])]]])

(defn hakukohteet-picker
  [field-descriptor idx]
  (let [toisen-asteen-yhteishaku @(subscribe [:application/toisen-asteen-yhteishaku?])]
    (if toisen-asteen-yhteishaku
      [hakukohde-2nd/hakukohteet field-descriptor idx]
      [hakukohteet field-descriptor idx])))
