(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.handlers-util :as handlers-util]
    [ataru.application_common.comparators :as comparators]
    [ataru.hakija.application-handlers :refer [set-field-visibilities
                                               set-validator-processing
                                               check-schema-interceptor]]))

(defn- hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- toggle-hakukohde-search
  [db]
  (update-in db [:application :show-hakukohde-search] not))

(reg-event-db
  :application/hakukohde-search-toggle
  [check-schema-interceptor]
  (fn [db _] (toggle-hakukohde-search db)))

(reg-event-db
  :application/hakukohde-query-process
  [check-schema-interceptor]
  (fn hakukohde-query-process [db [_ hakukohde-query-atom]]
    (let [hakukohde-query               @hakukohde-query-atom
          lang                          (-> db :form :selected-language)
          order-by-hakuaika             (if (some? (get-in db [:application :virkailija-secret]))
                                          #{}
                                          (->> (get-in db [:form :tarjonta :hakukohteet])
                                               (remove #(:on (:hakuaika %)))
                                               (map :oid)
                                               set))
          order-by-name                 #(util/non-blank-val (:label %) [lang :fi :sv :en])
          hakukohde-options             (->> (hakukohteet-field db)
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
      (-> db
          (assoc-in [:application :hakukohde-query] hakukohde-query)
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (assoc-in [:application :hakukohde-hits] hakukohde-hits)))))

(reg-event-fx
  :application/hakukohde-query-change
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-query-atom]]
    {:dispatch-debounced {:timeout  500
                          :id       :hakukohde-query
                          :dispatch [:application/hakukohde-query-process hakukohde-query-atom]}}))

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
  :application/create-questions-per-hakukohde
  (fn [{db :db} [_ hakukohde-oid]]
    (let [questions (get-in db [:form :content])
          selected-hakukohteet (get-in db [:application :answers :hakukohteet :value])
          update-questions (sort (comparators/duplikoitu-kysymys-hakukohde-comparator selected-hakukohteet)
                            (reduce (partial handlers-util/duplicate-questions-for-hakukohde db hakukohde-oid) [] questions))
          updated-answers (handlers-util/fill-missing-answer-for-hakukohde (get-in db [:application :answers]) update-questions)
          flat-form-content (util/flatten-form-fields update-questions)]
      {:db (-> db
               (assoc-in [:form :content] update-questions)
               (assoc-in [:application :answers] updated-answers)
               (assoc :flat-form-content flat-form-content))})))

(reg-event-fx
  :application/hakukohde-add-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (vec (get-in db [:application :answers :hakukohteet :values]))
          not-yet-selected?    (every? #(not= hakukohde-oid (:value %))
                                 selected-hakukohteet)
          new-hakukohde-values (cond-> selected-hakukohteet
                                       not-yet-selected?
                                       (conj {:valid true :value hakukohde-oid}))
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
       :dispatch-n [[:application/validate-hakukohteet] [:application/create-questions-per-hakukohde hakukohde-oid]]})))

(defn- remove-question-duplicates-with-hakukohde
  [hakukohde-oid questions]
  (filter #(not= (:duplikoitu-kysymys-hakukohde-oid %) hakukohde-oid) questions))

(defn- remove-duplicates-with-hakukohde
  [m questions hakukohde-oid]
  (let [duplicate-question-ids (->> questions
                                    (filter #(= (:duplikoitu-kysymys-hakukohde-oid %) hakukohde-oid))
                                    (map #(keyword (:id %))))]
    (apply dissoc m duplicate-question-ids )))

(reg-event-fx
  :application/remove-questions-per-hakukohde
  (fn [{db :db} [_ hakukohde-oid]]
    (let [questions (get-in db [:form :content])
          answers (get-in db [:application :answers])
          ui (get-in db [:application :ui])
          answers-without-duplicates (remove-duplicates-with-hakukohde answers questions hakukohde-oid)
          ui-without-duplicates (remove-duplicates-with-hakukohde ui questions hakukohde-oid)
          update-questions (remove-question-duplicates-with-hakukohde hakukohde-oid questions)
          flat-form-content (util/flatten-form-fields update-questions)]
      {:db (-> db
               (assoc-in [:form :content] update-questions)
               (assoc-in [:application :answers] answers-without-duplicates)
               (assoc-in [:application :ui] ui-without-duplicates)
               (assoc :flat-form-content flat-form-content))})))

(defn- remove-hakukohde-from-deleting
  [hakukohteet hakukohde]
  (remove #(= hakukohde %) hakukohteet))

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
                                   (update-in [:application :ui :hakukohteet :deleting] remove-hakukohde-from-deleting hakukohde-oid)
                                   set-field-visibilities)]
      {:db                 db
       :dispatch-n [[:application/validate-hakukohteet] [:application/remove-questions-per-hakukohde hakukohde-oid]]})))

(reg-event-fx
  :application/hakukohde-remove-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    {:db             (update-in db [:application :ui :hakukohteet :deleting] (comp set conj) hakukohde-oid)
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
