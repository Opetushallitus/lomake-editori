(ns ataru.hakija.application-hakukohde-2nd-component
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.application-common.components.dropdown-component :as dropdown-component]))



(defn- koulutustyyppi []
  [:div.application__hakukohde-2nd-row__hakukohde-koulutustyyppi
   [dropdown-component/dropdown
    {:options               []
     :unselected-label      "Koulutustyyppi"
     :selected-value        nil
     :on-change             println}]])

(defn- search-hit-hakukohde-row
  [hakukohde-oid idx]
  (let [hakukohde-selected?  @(subscribe [:application/hakukohde-selected? hakukohde-oid])
        search-term          @(subscribe [:application/hakukohde-query])
        aria-header-id       (str "hakukohde-search-hit-header-" hakukohde-oid)
        aria-description-id  (str "hakukohde-search-hit-description-" hakukohde-oid)
        hakukohde-editable?  @(subscribe [:application/hakukohde-editable? hakukohde-oid])
        hakukohteet-full?    @(subscribe [:application/hakukohteet-full? hakukohde-oid])
        rajaavat-hakukohteet @(subscribe [:application/rajaavat-hakukohteet hakukohde-oid])
        lang                 @(subscribe [:application/form-language])]
    [:div.application__search-hit-hakukohde-row-2nd
     {:on-mouse-down #(.preventDefault %)
      :on-click      #(do
                        (dispatch [:application/hakukohde-query-process (atom "") false])
                        (dispatch [:application/set-active-hakukohde-search nil])
                        (dispatch [:application/hakukohde-add-selection hakukohde-oid idx]))}
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       [:span @(subscribe [:application/hakukohde-label hakukohde-oid])]]]]))

(defn- hakukohde-selection [idx hakukohde]
  (let [search-input (r/atom "")
        hakukohde-hits (subscribe [:application/hakukohde-hits])
        active-hakukohde-selection (subscribe [:application/active-hakukohde-search])]
    (fn []
      (println "HAKUKOHTEET SUBI " @hakukohde-hits)
      [:div.application__hakukohde-2nd-row__hakukohde
       [:input.application__form-text-input-in-box
        {
         :on-blur #(do
                     (reset! search-input "")
                     (dispatch [:application/hakukohde-query-process search-input false])
                     (dispatch [:application/set-active-hakukohde-search nil]))
         :on-change   #(do (reset! search-input (.-value (.-target %)))
                           (dispatch [:application/hakukohde-query-change search-input true])
                           (dispatch [:application/set-active-hakukohde-search idx]))
         :title       "Otsikko"
         :placeholder "Hakukohde"
         :value       @search-input}]
       (when (= idx @active-hakukohde-selection)
         [:div.application__hakukohde-2nd-row__hakukohde-hits
          (for [hakukohde-oid @hakukohde-hits]
            ^{:key hakukohde-oid}
            [search-hit-hakukohde-row hakukohde-oid idx])])])))

(defn- selected-hakukohde [idx hakukohde-oid]
  [:div.application__hakukohde-2nd-row__selected-hakukohde
   (if hakukohde-oid
     [:div.application__hakukohde-2nd-row__selected-hakukohde-row
      [:div.application__hakukohde-2nd-row__selected-hakukohde-details
       @(subscribe [:application/hakukohde-label hakukohde-oid])]
      [:div.application__hakukohde-2nd-row__selected-hakukohde-link
       [:a {:href "/asdf"}
        [:i.zmdi.zmdi-open-in-new]
        " Lue lisätietoa"]]
      [:div.application__hakukohde-2nd-row__selected-hakukohde-remove
       {:on-click #(dispatch [:application/hakukohde-remove-idx idx])}
       "Poista "
       [:i.zmdi.zmdi-delete]]]
     )])

(defn- hakukohde-priority [idx hakukohde-oid max-hakukohteet]
  (let [increase-disabled (= idx 0)
        decrease-disabled (= idx (dec max-hakukohteet))]
    [:div.application__hakukohde-2nd-row__hakukohde-order
     [:span
      (when-not increase-disabled
        [:i.zmdi.zmdi-caret-up.zmdi-hc-2x
         {:on-click #(dispatch [:application/change-hakukohde-priority hakukohde-oid -1])}])]
     [:span (inc idx)]
     [:span
      (when-not decrease-disabled
        [:i.zmdi.zmdi-caret-down.zmdi-hc-2x
         {:on-click #(dispatch [:application/change-hakukohde-priority hakukohde-oid 1])}])]]))

(defn- hakukohde-row [idx hakukohde-oid max-hakukohteet]
  (println "hakukohde-row " idx hakukohde-oid)
  [:div.application__hakukohde-2nd-row
   [hakukohde-priority idx hakukohde-oid max-hakukohteet]
   [:div.application__hakukohde-2nd-row__right
    [:div.application__hakukohde-2nd-row__bottom
     [selected-hakukohde idx hakukohde-oid]]
    [:div.application__hakukohde-2nd-row__top
     [koulutustyyppi]
     [hakukohde-selection idx hakukohde-oid]]
    ]])

(defn hakukohteet
  [field-descriptor _]
  (let [selected-hakukohteet (subscribe [:application/selected-hakukohteet])
        max-hakukohteet (subscribe [:application/max-hakukohteet])]
    (println "MAX HAKUKOHTEET " @max-hakukohteet)
    (println "VALITUT HAKUKOHTEET " @selected-hakukohteet)
    [:div.application__wrapper-element
     ;;[hakukohde-selection-header field-descriptor]
     [:div.application__wrapper-contents.application__hakukohde-2nd-contents-wrapper
      [:div.application__form-field
       [:div.application__hakukohde-selected-list
        (for [idx (range @max-hakukohteet)]
          ^{:key (str "hakukohde-row-" idx)}
          [hakukohde-row idx (nth @selected-hakukohteet idx nil) @max-hakukohteet])]]]]))
