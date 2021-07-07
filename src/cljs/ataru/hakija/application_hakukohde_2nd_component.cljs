(ns ataru.hakija.application-hakukohde-2nd-component
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.application-common.components.button-component :as button-component]
            [ataru.translations.translation-util :as translations]))

(defn- koulutustyyppi-filter-row [koulutustyyppi-name is-selected on-change-fn]
  [:div.application__koulutustyypit-filter-row
   {:on-mouse-down #(.preventDefault %)}
   [:input {:id            (str koulutustyyppi-name "-checkbox")
            :aria-label    koulutustyyppi-name
            :type          "checkbox"
            :on-change     on-change-fn
            :checked       is-selected
            :on-mouse-down #(.preventDefault %)}]
   [:span {:on-mouse-down #(.preventDefault %)
           :on-click      on-change-fn
           :aria-hidden   true}
    koulutustyyppi-name]])

(defn- koulutustyyppi-btn [label is-open? on-click-fn on-blur-fn]
  [:div
   [button-component/button {:label label
                             :on-click on-click-fn
                             :on-blur on-blur-fn}]
   (if is-open?
     [:i.zmdi.zmdi-caret-up]
     [:i.zmdi.zmdi-caret-down])])

(defn- koulutustyypit-filter [idx lang]
  (let [is-open (r/atom false)
        koulutustyypit (subscribe [:application/koulutustyypit])
        koulutustyypit-filters (subscribe [:application/active-koulutustyyppi-filters idx])]
    (fn []
      (let [koulutustyypit-filters' @koulutustyypit-filters
            label (str
                    (translations/get-hakija-translation :filter-with-koulutustyypit lang)
                    (when (not-empty koulutustyypit-filters')
                      (str " (" (count koulutustyypit-filters') ")")))
            on-blur-fn #(reset! is-open false)
            on-click-fn #(swap! is-open not)]
        [:div.application__hakukohde-2nd-row__hakukohde-koulutustyyppi
         [koulutustyyppi-btn label @is-open on-click-fn on-blur-fn]
         (when @is-open
           [:div.application__koulutustyypit-filter-wrapper

            (for [{uri :uri :as koulutustyyppi} @koulutustyypit]
              (let [is-selected (boolean (koulutustyypit-filters' uri))
                    on-select #(dispatch [:application/toggle-koulutustyyppi-filter idx uri])]
                ^{:key uri}
                [koulutustyyppi-filter-row (-> koulutustyyppi :label :fi) is-selected on-select]))])])))) ;TODO i18n

(defn- search-hit-hakukohde-row
  [hakukohde-oid idx]
  (let [aria-header-id (str "hakukohde-search-hit-header-" hakukohde-oid)]
    [:div.application__search-hit-hakukohde-row-2nd
     {:on-mouse-down #(.preventDefault %)
      :on-click      #(do
                        (dispatch [:application/hakukohde-query-process (atom "") idx])
                        (dispatch [:application/set-active-hakukohde-search nil])
                        (dispatch [:application/hakukohde-add-selection hakukohde-oid idx]))}
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       [:span @(subscribe [:application/hakukohde-label hakukohde-oid])]]]]))

(defn- hakukohde-selection [idx lang]
  (let [search-input (r/atom "")
        hakukohde-hits (subscribe [:application/koulutustyyppi-filtered-hakukohde-hits idx])
        active-hakukohde-selection (subscribe [:application/active-hakukohde-search])]
    (fn []
      [:div.application__hakukohde-2nd-row__hakukohde
       [:input.application__form-text-input-in-box
        {
         :on-blur #(do
                     (reset! search-input "")
                     (dispatch [:application/hakukohde-query-process search-input])
                     (dispatch [:application/set-active-hakukohde-search nil]))
         :on-change   #(do (reset! search-input (.-value (.-target %)))
                           (dispatch [:application/hakukohde-query-change search-input idx])
                           (dispatch [:application/set-active-hakukohde-search idx]))
         :placeholder (translations/get-hakija-translation :search-for-hakukohde lang)
         :value       @search-input}]
       (when (= idx @active-hakukohde-selection)
         [:div.application__hakukohde-2nd-row__hakukohde-hits
          (for [hakukohde-oid @hakukohde-hits]
            ^{:key hakukohde-oid}
            [search-hit-hakukohde-row hakukohde-oid idx])])])))

(defn- selected-hakukohde [idx hakukohde-oid lang]
  [:div.application__hakukohde-2nd-row__selected-hakukohde
   [:div.application__hakukohde-2nd-row__selected-hakukohde-row

    [:div.application-hakukohde-2nd-row__name-wrapper
     [:span @(subscribe [:application/hakukohde-name-label-by-oid hakukohde-oid])]
     [:span @(subscribe [:application/hakukohde-tarjoaja-name-label-by-oid hakukohde-oid])]]
    [:div.application__hakukohde-2nd-row__selected-hakukohde-utils-wrapper
     [:div.application__hakukohde-2nd-row__selected-hakukohde-link
      (when hakukohde-oid
        [:a {:href "/asdf"}
         [:i.zmdi.zmdi-open-in-new]
         (str
           " "
           (translations/get-hakija-translation :read-further-info lang))])]
     [:div.application__hakukohde-2nd-row__selected-hakukohde-remove
      {:on-click #(dispatch [:application/hakukohde-remove-by-idx idx])}
      (str
        (translations/get-hakija-translation :remove lang)
        " ")
      [:i.zmdi.zmdi-delete]]]]])

(defn- hakukohde-priority [idx hakukohde-oid max-hakukohteet]
  (let [increase-disabled (= idx 0)
        decrease-disabled (= idx (dec max-hakukohteet))]
    [:div.application__hakukohde-2nd-row__hakukohde-order
     [:span
      [:i.zmdi.zmdi-caret-up.zmdi-hc-2x
       (if increase-disabled
         {:class "application__hakukohde-2nd-row__hakukohde-change-order-hidden"}
         {:on-click #(dispatch [:application/change-hakukohde-priority hakukohde-oid -1])})]]
     [:span (inc idx)]
     [:span
      [:i.zmdi.zmdi-caret-down.zmdi-hc-2x
       (if decrease-disabled
         {:class "application__hakukohde-2nd-row__hakukohde-change-order-hidden"}
         {:on-click #(dispatch [:application/change-hakukohde-priority hakukohde-oid 1])})]]]))

(defn- hakukohde-row [idx hakukohde-oid max-hakukohteet lang]
  [:div.application__hakukohde-2nd-row
   [hakukohde-priority idx hakukohde-oid max-hakukohteet]
   [:div.application__hakukohde-2nd-row__right
    [:div.application__hakukohde-2nd-row__top
     [selected-hakukohde idx hakukohde-oid lang]]
    [:div.application__hakukohde-2nd-row__bottom
     [koulutustyypit-filter idx lang]
     [hakukohde-selection idx lang]]
    ]])

(defn- lisaa-hakukohde-button [lang]
  (fn []
    [:button.application_add-hakukohde-row-button
     {:on-click #(dispatch [:application/add-empty-hakukohde-selection])}
     [:i.zmdi.zmdi-plus]
     (translations/get-hakija-translation :add-application-option lang)]))

(defn- hakukohde-max-amount-reached-message [max-hakukohteet lang]
  (let [[text-1 text-2] (translations/get-hakija-translation :choose-n-hakukohde-at-most lang)]
    [:span.application__hakukohde-2nd-max-amount-reached
     [:i.zmdi.zmdi-info-outline]
     (str
       " "
       text-1
       max-hakukohteet
       text-2)]))

(defn- add-hakukohde-row [selected-hakukohteet max-hakukohteet lang]
  (let [hakukohteet-count (count selected-hakukohteet)
        has-space (< hakukohteet-count max-hakukohteet)]
    (if has-space
      [lisaa-hakukohde-button lang]
      [hakukohde-max-amount-reached-message max-hakukohteet lang])))

(defn hakukohteet
  [field-descriptor _]
  (let [selected-hakukohteet (subscribe [:application/selected-hakukohteet])
        hakukohteet-count (count @selected-hakukohteet)
        max-hakukohteet (subscribe [:application/max-hakukohteet])
        lang (subscribe [:application/form-language])]
    [:div.application__wrapper-element
     ;;[hakukohde-selection-header field-descriptor]
     [:div.application__wrapper-contents.application__hakukohde-2nd-contents-wrapper
      [:div.application__form-field
       [:div.application__hakukohde-selected-list
        (for [idx (range hakukohteet-count)]
          ^{:key (str "hakukohde-row-" idx)}
          [hakukohde-row idx (nth @selected-hakukohteet idx nil) hakukohteet-count @lang])]
       [add-hakukohde-row @selected-hakukohteet @max-hakukohteet @lang]]]]))
