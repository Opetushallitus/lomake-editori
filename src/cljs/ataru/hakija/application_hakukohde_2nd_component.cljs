(ns ataru.hakija.application-hakukohde-2nd-component
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.application-common.components.button-component :as button-component]))

(defn- hakukohde-info-row [icon title body]
  [:div.application__hakukohde-2nd__info_row
   [:div.application__hakukohde-2nd__info_row__icon-wrapper
    [:div
     [:i.zmdi.zmdi-hc-2x {:class icon}]]]
   [:div.application__hakukohde-2nd__info_row__title-wrapper
    [:h4 title]
    [:p body]]])

(defn- hakukohde-info []
  [:div
   [hakukohde-info-row
    :zmdi-swap-vertical
    "Hakukohteiden järjestäminen"
    "Aseta valitsemasi hakukohteet järjestykseen, jossa toivot tulevasi niihin hyväksytyksi. Harkitse hakukohdejärjestystä tarkoin, sillä se on sitova, etkä voi muuttaa sitä enää hakuajan päättymisen jälkeen."]
   [hakukohde-info-row
    :zmdi-swap-vertical
    "Valituksi tuleminen"
    "Jos et tule hyväksytyksi ensimmäiseksi asettamaasi hakukohteeseen, tarkistetaan riittääkö valintamenestyksesi seuraavaan asetaamaasi hakukohteeseen. Jos tulet hyväksytyksi johonkin toiseen hakukohteeseen, sitä alemmat hakukohteetu peruuntuvat automattisestii, etkä voi enää tulla valituksi niihin. Ylempiin hakukohteisiin voit kuitenkin vielä tulla myöhemmin valituksi."]])

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

(defn- koulutustyypit-filter [idx]
  (let [is-open (r/atom false)
        koulutustyypit (subscribe [:application/koulutustyypit])
        koulutustyypit-filters (subscribe [:application/active-koulutustyyppi-filters idx])]
    (fn []
      (let [koulutustyypit-filters' @koulutustyypit-filters
            label (str
                    "Rajaa koulutustyypeillä"
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
                        (dispatch [:application/hakukohde-query-process (atom "") idx])
                        (dispatch [:application/set-active-hakukohde-search nil])
                        (dispatch [:application/hakukohde-add-selection hakukohde-oid idx]))}
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       [:span @(subscribe [:application/hakukohde-label hakukohde-oid])]]]]))

(defn- hakukohde-selection [idx hakukohde]
  (let [search-input (r/atom "")
        hakukohde-hits (subscribe [:application/koulutustyyppi-filtered-hakukohde-hits idx])
        active-hakukohde-selection (subscribe [:application/active-hakukohde-search])]
    (fn []
      (prn "@hakukohde-hits" @hakukohde-hits)
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
         :placeholder "Hae koulutusta tai oppilaitosta"
         :value       @search-input}]
       (when (= idx @active-hakukohde-selection)
         [:div.application__hakukohde-2nd-row__hakukohde-hits
          (for [hakukohde-oid @hakukohde-hits]
            ^{:key hakukohde-oid}
            [search-hit-hakukohde-row hakukohde-oid idx])])])))

(defn- selected-hakukohde [idx hakukohde-oid]
    [:div.application__hakukohde-2nd-row__selected-hakukohde
     [:div.application__hakukohde-2nd-row__selected-hakukohde-row

       [:div.application-hakukohde-2nd-row__name-wrapper
         [:span @(subscribe [:application/hakukohde-name-label-by-oid hakukohde-oid])]
         [:span @(subscribe [:application/hakukohde-tarjoaja-name-label-by-oid hakukohde-oid])]]
      [:div.application__hakukohde-2nd-row__selected-hakukohde-link
       (when hakukohde-oid
         [:a {:href "/asdf"}
          [:i.zmdi.zmdi-open-in-new]
          " Lue lisätietoa"])]
      [:div.application__hakukohde-2nd-row__selected-hakukohde-remove
       {:on-click #(dispatch [:application/hakukohde-remove-by-idx idx])}
       "Poista "
       [:i.zmdi.zmdi-delete]]]])

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

(defn- hakukohde-row [idx hakukohde-oid max-hakukohteet]
  (println "hakukohde-row " idx hakukohde-oid)
  [:div.application__hakukohde-2nd-row
   [hakukohde-priority idx hakukohde-oid max-hakukohteet]
   [:div.application__hakukohde-2nd-row__right
    [:div.application__hakukohde-2nd-row__top
     [selected-hakukohde idx hakukohde-oid]]
    [:div.application__hakukohde-2nd-row__bottom
     [koulutustyypit-filter idx]
     [hakukohde-selection idx hakukohde-oid]]
    ]])

(defn- lisaa-hakukohde-button []
  [:button.application_add-hakukohde-row-button
   {:on-click #(dispatch [:application/add-empty-hakukohde-selection])}
   [:i.zmdi.zmdi-plus]
   "Lisää hakukohde"])

(defn- hakukohde-max-amount-reached-message [max-hakukohteet]
  [:span.application__hakukohde-2nd-max-amount-reached
   [:i.zmdi.zmdi-info-outline]
   (str " Valitse enintään " max-hakukohteet " hakukohdetta")])

(defn- add-hakukohde-row [selected-hakukohteet max-hakukohteet]
  (let [hakukohteet-count (count selected-hakukohteet)
        has-space (< hakukohteet-count max-hakukohteet)]
    (if has-space
      [lisaa-hakukohde-button]
      [hakukohde-max-amount-reached-message max-hakukohteet])))

(defn hakukohteet
  [field-descriptor _]
  (let [selected-hakukohteet (subscribe [:application/selected-hakukohteet])
        hakukohteet-count (count @selected-hakukohteet)
        max-hakukohteet (subscribe [:application/max-hakukohteet])]
    [:div.application__wrapper-element
     ;;[hakukohde-selection-header field-descriptor]
     [:div.application__wrapper-contents.application__hakukohde-2nd-contents-wrapper
      [hakukohde-info]
      [:div.application__form-field
       [:div.application__hakukohde-selected-list
        (for [idx (range hakukohteet-count)]
          ^{:key (str "hakukohde-row-" idx)}
          [hakukohde-row idx (nth @selected-hakukohteet idx nil) hakukohteet-count])]
       [add-hakukohde-row @selected-hakukohteet @max-hakukohteet]]]]))
