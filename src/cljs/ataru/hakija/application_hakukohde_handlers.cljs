(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe reg-event-db reg-fx reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.application-validators :as validator]
    [ataru.hakija.application-handlers :refer [set-field-visibilities
                                               set-validator-processing
                                               check-schema-interceptor]]))

(defn hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- toggle-hakukohde-search
  [db]
  (update-in db [:application :show-hakukohde-search] not))

(defn query-hakukohteet [hakukohde-query lang virkailija? hakukohteet hakukohteet-field]
  (let [order-by-hakuaika             (if virkailija?
                                        #{}
                                        (->> hakukohteet
                                             (remove #(:on (:hakuaika %)))
                                             (map :oid)
                                             set))
        order-by-name                 #(util/non-blank-val (:label %) [lang :fi :sv :en])
        hakukohde-options             (->> hakukohteet-field
                                           :options
                                           (sort-by (juxt (comp order-by-hakuaika :value)
                                                          order-by-name)))
        query-parts                   (map string/lower-case (string/split hakukohde-query #"\s+"))
        results                       (if (or (string/blank? hakukohde-query)
                                              (< (count hakukohde-query) 2))
                                        (map :value hakukohde-options)
                                        (->> hakukohde-options
                                             (filter
                                               (fn [option]
                                                 (let [haystack (string/lower-case
                                                                  (str (get-in option [:label lang] (get-in option [:label :fi] ""))
                                                                       (get-in option [:description lang] "")))]
                                                   (every? #(string/includes? haystack %) query-parts))))
                                             (map :value)))
        [hakukohde-hits rest-results] (split-at 15 results)]
    {:hakukohde-hits  hakukohde-hits
     :rest-results    rest-results
     :hakukohde-query hakukohde-query}))

(reg-event-db
  :application/hakukohde-search-toggle
  [check-schema-interceptor]
  (fn [db _] (toggle-hakukohde-search db)))

(reg-event-db
  :application/hakukohde-query-process
  [check-schema-interceptor]
  (fn hakukohde-query-process [db [_ hakukohde-query-atom filter-by-koulutustyyppi?]]
    ;;TODO: tee logiikka filter-by-koulutustyyppi?
    (let [hakukohde-query @hakukohde-query-atom
          lang (-> db :form :selected-language)
          virkailija? (some? (get-in db [:application :virkailija-secret]))
          hakukohteet-field (hakukohteet-field db)
          hakukohteet (get-in db [:form :tarjonta :hakukohteet])
          {:keys [hakukohde-query
                  hakukohde-hits
                  rest-results]} (query-hakukohteet hakukohde-query lang virkailija? hakukohteet hakukohteet-field)]
      (println "HAKUKOHTEET " (js/console.log (clj->js (first hakukohteet))))
      (println "HAKUKOHDE HITS " hakukohde-hits)
      (println "REST RESULTS " rest-results)
      (println "HAKUKOHDE QUERY " hakukohde-query)
      (-> db
          (assoc-in [:application :hakukohde-query] hakukohde-query)
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (assoc-in [:application :hakukohde-hits] hakukohde-hits)))))

(reg-event-fx
  :application/hakukohde-query-change
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-query-atom filter-by-koulutustyyppi?]]
    {:dispatch-debounced {:timeout  500
                          :id       :hakukohde-query
                          :dispatch [:application/hakukohde-query-process
                                     hakukohde-query-atom
                                     (boolean filter-by-koulutustyyppi?)]}}))

(reg-event-db
  :application/show-more-hakukohdes
  [check-schema-interceptor]
  (fn [db _]
    (let [remaining-results (-> db :application :remaining-hakukohde-search-results)
          [more-hits rest-results] (split-at 15 remaining-results)]
      (-> db
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (update-in [:application :hakukohde-hits] concat more-hits)))))

(reg-event-fx
  :application/set-hakukohde-valid
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ valid?]]
    {:db       (assoc-in db [:application :answers :hakukohteet :valid] valid?)
     :dispatch [:application/set-validator-processed :hakukohteet]}))

(reg-event-fx
  :application/validate-hakukohteet
  [check-schema-interceptor]
  (fn [{db :db} _]
    {:db                 (set-validator-processing db :hakukohteet)
     :validate-debounced {:value                        (get-in db [:application :answers :hakukohteet :value])
                          :tarjonta-hakukohteet         (get-in db [:form :tarjonta :hakukohteet])
                          :rajaavat-hakukohderyhmat     (get-in db [:form :rajaavat-hakukohderyhmat])
                          :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                          :answers-by-key               (get-in db [:application :answers])
                          :field-descriptor             (hakukohteet-field db)
                          :editing?                     (get-in db [:application :editing?])
                          :virkailija?                  (contains? (:application db) :virkailija-secret)
                          :on-validated                 (fn [[valid? errors]]
                                                          (dispatch [:application/set-hakukohde-valid
                                                                     valid?]))}}))

(reg-event-fx
  :application/hakukohde-add-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid idx]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (vec (get-in db [:application :answers :hakukohteet :values]))
          not-yet-selected?    (every? #(not= hakukohde-oid (:value %))
                                 selected-hakukohteet)
          add-hakukohde-fn     (fn [hakukohteet]
                                 (let [hakukohde {:valid true :value hakukohde-oid}
                                       default {:valid false :value nil}]
                                   (->> (range (max (inc idx) (count hakukohteet)))
                                        (map (fn [cur-idx]
                                               (println "CUR-idx" cur-idx idx (nth hakukohteet cur-idx nil))
                                               (let [cur (nth hakukohteet cur-idx nil)]
                                                 (if (= idx cur-idx)
                                                   hakukohde
                                                   (or cur default))))))))
          new-hakukohde-values (cond-> selected-hakukohteet
                                       not-yet-selected?
                                       add-hakukohde-fn)
          max-hakukohteet      (get-in field-descriptor [:params :max-hakukohteet] nil)
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   set-field-visibilities)]
      {:db                 (cond-> db
                                   (and (some? max-hakukohteet)
                                        (<= max-hakukohteet (count new-hakukohde-values)))
                                   (assoc-in [:application :show-hakukohde-search] false))
       :dispatch [:application/validate-hakukohteet]})))

(reg-event-db
  :application/add-empty-hakukohde-selection
  [check-schema-interceptor]
  (fn [db _]
    (let [empty-hakukohde {:valid false :value nil}]
      (update-in db [:application :answers :hakukohteet :values] #(conj % empty-hakukohde)))))

(reg-event-fx
  :application/hakukohde-remove-idx
  [check-schema-interceptor]
  (fn [{db :db} [_ idx]]
    (let [selected-hakukohteet (into [] (get-in db [:application :answers :hakukohteet :values] []))
          default              {:valid false :value nil}
          new-hakukohde-values (if (= idx (dec (count selected-hakukohteet)))
                                 (drop-last selected-hakukohteet)
                                 (assoc selected-hakukohteet idx default))
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   set-field-visibilities)]
      {:db db
       :dispatch [:application/validate-hakukohteet]})))


(reg-event-fx
  :application/hakukohde-remove
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (vec (remove #(= hakukohde-oid (:value %)) selected-hakukohteet))
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   ;(update-in [:application :ui :hakukohteet :deleting] remove-hakukohde-from-deleting hakukohde-oid)
                                   set-field-visibilities)]
      {:db                 db
       :dispatch [:application/validate-hakukohteet]})))

(reg-event-fx
  :application/hakukohde-remove-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    {:db             db                                     ;(update-in db [:application :ui :hakukohteet :deleting] (comp set conj) hakukohde-oid)
     :dispatch-later [{:ms       500
                       :dispatch [:application/hakukohde-remove hakukohde-oid]}]}))

(reg-event-fx
  :application/change-hakukohde-priority
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid index-change]]
    (let [hakukohteet     (-> db :application :answers :hakukohteet :values vec)
          current-index   (first (keep-indexed #(when (= hakukohde-oid (:value %2))
                                                  %1)
                                   hakukohteet))
          new-index       (+ current-index index-change)
          new-hakukohteet (assoc hakukohteet
                            current-index (nth hakukohteet new-index)
                            new-index (nth hakukohteet current-index))
          db              (-> db
                              (assoc-in [:application :answers :hakukohteet :values] new-hakukohteet)
                              (assoc-in [:application :answers :hakukohteet :value] (mapv :value new-hakukohteet)))]
      {:db                 db
       :dispatch [:application/validate-hakukohteet]})))

(reg-event-db
  :application/handle-fetch-koulutustyypit
  [check-schema-interceptor]
  (fn [db [_ {koulutustyypit-response-body :body}]]
    (prn "WTF2" koulutustyypit-response-body)
    (let [relevant-koulutustyyyppi-ids #{"1" "2" "4" "5" "35" "40"};TODO yksi id puuttuu tästä "tutkintokoulutukseen valmistava..."
          koulutustyypit (filter #(relevant-koulutustyyyppi-ids (:value %))
                                 koulutustyypit-response-body)]
      (assoc-in db [:application :koulutustyypit] koulutustyypit))))

(reg-event-fx
  :application/fetch-koulutustyypit
  [check-schema-interceptor]
  (fn [_]
    (prn "WTF1")
    {:http {:method    :get
            :url       "/hakemus/api/koulutustyypit"
            :handler   [:application/handle-fetch-koulutustyypit]}}))

(reg-event-db
  :application/toggle-koulutustyyppi-filter
  [check-schema-interceptor]
  (fn [db [_ idx koulutustyyppi-value]]
    (update-in db [:application :hakukohde-koulutustyyppi-filters idx koulutustyyppi-value] not)))
