(ns ataru.virkailija.views.hakukohde-and-hakukohderyhma-search
  (:require [ataru.util :as util]
            [ataru.cljs-util :as cljs-util]
            [re-frame.core :as re-frame]
            [reagent.core :as r]))

(defn- search-input->search-terms
  [search-input]
  (clojure.string/split search-input #"\s+"))

(defn- hakukohderyhma->name
  [lang hakukohderyhma]
  (util/non-blank-val (:name hakukohderyhma)
                      [lang :fi :sv :en]))

(defn- hakukohde->hakukohde-name
  [lang hakukohde]
  (let [hakukohde-name (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
        tarjoaja-name  (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]
    (str hakukohde-name " - " tarjoaja-name)))

(defn- hakukohderyhma->hakukohderyhma-hit
  [lang search-terms hakukohderyhma]
  (let [parts (util/match-text (hakukohderyhma->name lang hakukohderyhma)
                               search-terms)]
    {:id          (:oid hakukohderyhma)
     :label-parts parts
     :match?      (or (not-empty (rest parts))
                      (:hilight (first parts)))}))

(defn- hakukohde->hakukohde-hit
  [lang search-terms hakukohde]
  (let [parts (util/match-text (hakukohde->hakukohde-name lang hakukohde)
                               search-terms)]
    {:id          (:oid hakukohde)
     :label-parts parts
     :match?      (or (not-empty (rest parts))
                      (:hilight (first parts)))}))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/search-input
  (fn [db [_ id]]
    (get-in db [:hakukohde-and-hakukohderyhma id :search-input] "")))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/hakukohderyhma-hits
  (fn [db [_ id hakukohderyhmat]]
    (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
          search-terms (search-input->search-terms
                        (get-in db [:hakukohde-and-hakukohderyhma id :search-input] ""))]
      (if (some util/should-search? search-terms)
        (get-in db [:hakukohde-and-hakukohderyhma id :hakukohderyhma-hits])
        (map (partial hakukohderyhma->hakukohderyhma-hit
                      lang
                      search-terms)
             hakukohderyhmat)))))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/hakukohde-hits
  (fn [db [_ id haku]]
    (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
          search-terms (search-input->search-terms
                        (get-in db [:hakukohde-and-hakukohderyhma id :search-input] ""))]
      (if (some util/should-search? search-terms)
        (get-in db [:hakukohde-and-hakukohderyhma id :hakukohde-hits (:oid haku)])
        (map (partial hakukohde->hakukohde-hit
                      lang
                      search-terms)
             (:hakukohteet haku))))))

(re-frame/reg-event-db
  :hakukohde-and-hakukohderyhma/set-search-input
  (fn [db [_ id haut hakukohderyhmat hakukohde-selected? hakukohderyhma-selected? search-input]]
    (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
          search-terms (search-input->search-terms search-input)
          did-search?  (some util/should-search? search-terms)]
      (-> db
          (assoc-in [:hakukohde-and-hakukohderyhma id :search-input] search-input)
          (assoc-in [:hakukohde-and-hakukohderyhma id :hakukohderyhma-hits]
                    (->> hakukohderyhmat
                         (map (partial hakukohderyhma->hakukohderyhma-hit
                                       lang
                                       search-terms))
                         (filter #(or (hakukohderyhma-selected? (:id %))
                                      (and did-search? (:match? %))))))
          (assoc-in [:hakukohde-and-hakukohderyhma id :hakukohde-hits]
                    (reduce (fn [hits haku]
                              (assoc hits (:oid haku)
                                     (->> (:hakukohteet haku)
                                          (map (partial hakukohde->hakukohde-hit
                                                        lang
                                                        search-terms))
                                          (filter #(or (hakukohde-selected? (:id %))
                                                       (and did-search? (:match? %)))))))
                            {}
                            haut))))))

(defn- list-item
  [selected? on-select on-unselect {:keys [id label-parts]}]
  ^{:key (str "list-item-" id)}
  [:li.hakukohde-and-hakukohderyhma-category-list-item
   {:class    (when (selected? id)
                "hakukohde-and-hakukohderyhma-category-list-item--selected")
    :on-click (if (selected? id) #(on-unselect id) #(on-select id))}
   (map-indexed (fn [i {:keys [text hilight]}]
                  ^{:key (str i)}
                  [:span.hakukohde-and-hakukohderyhma-list-item-label
                   {:class
                    (str (when (selected? id)
                           "hakukohde-and-hakukohderyhma-list-item-label--selected")
                         (when hilight
                           " hakukohde-and-hakukohderyhma-list-item-label--highlighted"))}
                   text])
                label-parts)])

(defn- category-listing
  [category-name items selected? on-select on-unselect]
  (let [show-n (r/atom 10)]
    (fn [category-name items selected? on-select on-unselect]
      [:li.hakukohde-and-hakukohderyhma-category-listing
       [:span.hakukohde-and-hakukohderyhma-category-name
        category-name]
       [:ul.hakukohde-and-hakukohderyhma-category-list
        (->> items
             (take @show-n)
             (map (partial list-item selected? on-select on-unselect))
             doall)
        (when (< @show-n (count items))
          [:li.hakukohde-and-hakukohderyhma-category-list-item.hakukohde-and-hakukohderyhma-category-list-item--show-more
           {:on-click #(swap! show-n + 10)}
           [:span.hakukohde-and-hakukohderyhma-show-more
            (cljs-util/get-virkailija-translation :show-more)]])]])))

(defn search-input
  [{:keys [id
           haut
           hakukohderyhmat
           hakukohde-selected?
           hakukohderyhma-selected?]}]
  [:input.hakukohde-and-hakukohderyhma-search-input
   {:value       @(re-frame/subscribe
                   [:hakukohde-and-hakukohderyhma/search-input id])
    :on-change   #(re-frame/dispatch
                   [:hakukohde-and-hakukohderyhma/set-search-input
                    id
                    haut
                    hakukohderyhmat
                    hakukohde-selected?
                    hakukohderyhma-selected?
                    (.-value (.-target %))])
    :placeholder (cljs-util/get-virkailija-translation :search-hakukohde-placeholder)}])

(defn search-listing
  [{:keys [id
           haut
           hakukohderyhmat
           hakukohde-selected?
           hakukohderyhma-selected?
           on-hakukohde-select
           on-hakukohde-unselect
           on-hakukohderyhma-select
           on-hakukohderyhma-unselect]}]
  (let [lang @(re-frame/subscribe [:editor/virkailija-lang])]
    [:ul.hakukohde-and-hakukohderyhma-search-listing
     (when-let [hits (seq @(re-frame/subscribe
                            [:hakukohde-and-hakukohderyhma/hakukohderyhma-hits
                             id
                             hakukohderyhmat]))]
       [category-listing
        (cljs-util/get-virkailija-translation :hakukohderyhmat)
        hits
        hakukohderyhma-selected?
        on-hakukohderyhma-select
        on-hakukohderyhma-unselect])
     (doall
      (for [haku  haut
            :let  [hits @(re-frame/subscribe
                          [:hakukohde-and-hakukohderyhma/hakukohde-hits
                           id
                           haku])]
            :when (seq hits)]
        ^{:key (str "category-" (:oid haku))}
        [category-listing
         (util/non-blank-val (:name haku) [lang :fi :sv :en])
         hits
         hakukohde-selected?
         on-hakukohde-select
         on-hakukohde-unselect]))]))
