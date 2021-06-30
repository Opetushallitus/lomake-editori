(ns ataru.hakija.subs
  (:require [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.application-common.application-field-common :as afc]
            [ataru.hakija.application :as autil]
            [ataru.hakija.application-validators :as validators]
            [ataru.hakija.person-info-fields :as person-info-fields]
            [clojure.set :as cset]
            [clojure.string :as cstr]
            [cemerick.url :as url]))

(defonce attachment-modify-grace-period-days
  (get (js->clj js/config) "attachment-modify-grace-period-days" 14))

(re-frame/reg-sub
  :application/form
  (fn [db _]
    (:form db)))

(re-frame/reg-sub
  :application/flat-form-content
  (fn [db _]
    (:flat-form-content db)))

(re-frame/reg-sub
  :application/form-field
  (fn [_ _]
    (re-frame/subscribe [:application/flat-form-content]))
  (fn [flat-form-content [_ id]]
    (first (filter #(= (keyword id) (keyword (:id %))) flat-form-content))))

(re-frame/reg-sub
  :application/application
  (fn [db _]
    (:application db)))

(re-frame/reg-sub
  :application/application-identifier
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:application-identifier application)))

(re-frame/reg-sub
  :application/editing?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:editing? application)))

(re-frame/reg-sub
  :application/answers
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:answers application)))

(re-frame/reg-sub
  :application/excluded-attachment-ids-when-yo-and-jyemp
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:excluded-attachment-ids-when-yo-and-jyemp application)))

(re-frame/reg-sub
  :application/person
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:person application)))

(defn- value-from-person
  [person id question-group-idx repeatable-idx]
  (cond (some? question-group-idx)
        (get-in person [id question-group-idx (or repeatable-idx 0)])
        (some? repeatable-idx)
        (get-in person [id repeatable-idx])
        :else
        (get person id)))

(re-frame/reg-sub
  :application/visible-options
  (fn [[_ field-description] _]
    (re-frame/subscribe [:application/ui-of (keyword (:id field-description))]))
  (fn [ui-of [_ field-description]]
    (keep-indexed (fn [index option]
                    (when (get-in ui-of [index :visible?] true)
                      option))
                  (:options field-description))))

(re-frame/reg-sub
  :application/visible?
  (fn [[_ id] _]
    (re-frame/subscribe [:application/ui-of id]))
  (fn [ui _]
    (:visible? ui true)))

(re-frame/reg-sub
  :application/answer
  (fn [[_ id _ _] _]
    [(re-frame/subscribe [:application/answers])
     (re-frame/subscribe [:application/person])
     (re-frame/subscribe [:application/cannot-edit? id])])
  (fn [[answers person cannot-edit?] [_ id question-group-idx repeatable-idx]]
    (let [id (keyword id)]
      (cond-> (cond (some? question-group-idx)
                    (get-in answers [id :values question-group-idx (or repeatable-idx 0)])
                    (some? repeatable-idx)
                    (get-in answers [id :values repeatable-idx])
                    :else
                    (get answers id))
              (and cannot-edit?
                   (not= :have-finnish-ssn id)
                   (contains? person-info-fields/editing-forbidden-person-info-field-ids id))
              (assoc :value (value-from-person person id question-group-idx repeatable-idx)
                     :valid true)))))

(re-frame/reg-sub
  :application/tarjonta-hakukohteet
  (fn [db _]
    (-> db :form :tarjonta :hakukohteet)))

(re-frame/reg-sub
  :application/tarjonta-hakukohteet-by-oid
  (fn [_ _]
    (re-frame/subscribe [:application/tarjonta-hakukohteet]))
  (fn [hakukohteet _]
    (util/group-by-first :oid hakukohteet)))

(re-frame/reg-sub
  :application/attachment-deadline
  (fn [_ _]
    (re-frame/subscribe [:application/selected-language]))
  (fn [selected-language [_ field]]
    (-> field :params :deadline-label selected-language)))

(re-frame/reg-sub
  :application/haku-end-time
  (fn [_ _]
    [(re-frame/subscribe [:state-query [:form :tarjonta :hakuaika :label :end-time]])
     (re-frame/subscribe [:application/selected-language])])
  (fn [[label selected-language] [_ _]]
    (when label
      (get label selected-language))))

(re-frame/reg-sub
  :application/haku-aika
  (fn [db _]
    (-> db :form :tarjonta :hakuaika)))

(re-frame/reg-sub
  :application/repeatable-answer-count
  (fn [_ _]
    (re-frame/subscribe [:application/answers]))
  (fn [answers [_ id question-group-idx]]
    (max 1 (count
            (if (some? question-group-idx)
              (get-in answers [id :values question-group-idx])
              (get-in answers [id :values]))))))

(re-frame/reg-sub
  :application/submitted?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (= :submitted (:submit-status application))))

(re-frame/reg-sub
  :application/cannot-edit-because-in-processing?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:cannot-edit-because-in-processing application)))

(re-frame/reg-sub
  :application/virkailija?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (some? (:virkailija-secret application))))

(re-frame/reg-sub
  :application/ui
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:ui application)))

(re-frame/reg-sub
  :application/ui-of
  (fn [_ _]
    (re-frame/subscribe [:application/ui]))
  (fn [ui [_ id]]
    (get ui (keyword id))))

(re-frame/reg-sub
  :state-query
  (fn [db [_ path default]]
    (get-in db (remove nil? path) default)))

(re-frame/reg-sub
  :application/valid-status
  (fn [_ _]
    [(re-frame/subscribe [:application/answers])
     (re-frame/subscribe [:application/ui])
     (re-frame/subscribe [:application/flat-form-content])])
  (fn [[answers ui flat-form-content] _]
    (autil/answers->valid-status answers ui flat-form-content)))

(re-frame/reg-sub
  :application/invalid-fields?
  (fn [_ _]
    (re-frame/subscribe [:application/valid-status]))
  (fn [valid-status _]
    (not (empty? (:invalid-fields valid-status)))))

(re-frame/reg-sub
  :application/can-apply?
  (fn [_ _]
    [(re-frame/subscribe [:application/tarjonta-hakukohteet])
     (re-frame/subscribe [:application/virkailija?])])
  (fn [[hakukohteet virkailija?] _]
    (or (empty? hakukohteet)
        virkailija?
        (some #(get-in % [:hakuaika :on]) hakukohteet))))

(re-frame/reg-sub
  :application/selected-language
  (fn [_ _]
    (re-frame/subscribe [:application/form]))
  (fn [form _]
    (:selected-language form)))

(re-frame/reg-sub
  :application/languages
  (fn [_ _]
    (re-frame/subscribe [:application/form]))
  (fn [form _]
    (:languages form)))

(re-frame/reg-sub
  :application/form-language
  (fn [_ _]
    (re-frame/subscribe [:application/selected-language]))
  (fn [selected-language _]
    ;; When user lands on the page, there isn't any language set until the
    ;; form is loaded
    (or selected-language :fi)))

(re-frame/reg-sub
  :application/selected-hakukohteet
  (fn [_ _]
    (re-frame/subscribe [:application/answer :hakukohteet nil nil]))
  (fn [hakukohteet-answer _]
    (map :value (:values hakukohteet-answer))))

(re-frame/reg-sub
  :application/ylioppilastutkinto?
  (fn [_ _]
    (re-frame/subscribe [:application/answer :higher-completed-base-education nil nil]))
  (fn [pohjakoulutus-answer _]
    (boolean (some #(or (= "pohjakoulutus_yo" %)
                        (= "pohjakoulutus_yo_ammatillinen" %)
                        (= "pohjakoulutus_yo_kansainvalinen_suomessa" %)
                        (= "pohjakoulutus_yo_ulkomainen" %))
                   (:value pohjakoulutus-answer)))))

(re-frame/reg-sub
  :application/selected-hakukohteet-for-field
  (fn [_ _]
    [(re-frame/subscribe [:application/tarjonta-hakukohteet-by-oid])
     (re-frame/subscribe [:application/selected-hakukohteet])
     (re-frame/subscribe [:application/ylioppilastutkinto?])
     (re-frame/subscribe [:application/excluded-attachment-ids-when-yo-and-jyemp])])
  (fn [[hakukohteet selected-hakukohteet ylioppilastutkinto? excluded-attachment-ids-when-yo-and-jyemp] [_ field]]
    (when-let [ids (some-> (concat (get field :belongs-to-hakukohderyhma)
                                   (get field :belongs-to-hakukohteet))
                           seq
                           set)]
      (let [jyemp? (and ylioppilastutkinto?
                        (contains? excluded-attachment-ids-when-yo-and-jyemp (:id field)))]
        (->> (cond->> (map hakukohteet selected-hakukohteet)
                      jyemp?
                      (remove :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?))
             (remove #(empty? (cset/intersection
                               ids
                               (conj (set (:hakukohderyhmat %)) (:oid %))))))))))

(re-frame/reg-sub
  :application/cannot-view?
  (fn [[_ id] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/editing?])])
  (fn [[field editing?] _]
    (and editing? (:cannot-view field))))

(re-frame/reg-sub
  :application/cannot-edit?
  (fn [[_ id] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/editing?])])
  (fn [[field editing?] _]
    (and editing? (:cannot-edit field))))

(re-frame/reg-sub
  :application/disabled?
  (fn [[_ id] _]
    (re-frame/subscribe [:application/ui-of id]))
  (fn [ui _]
    (get ui :disabled? false)))

(re-frame/reg-sub
  :application/get-i18n-text
  (fn [_ [_ translations]]
    (get translations @(re-frame/subscribe [:application/form-language]))))

(re-frame/reg-sub
  :application/adjacent-field-row-amount
  (fn [db [_ field-descriptor question-group-idx]]
    (let [child-id   (-> (:children field-descriptor) first :id keyword)
          value-path (cond-> [:application :answers child-id :values]
                       question-group-idx (conj question-group-idx))
          row-amount (-> (get-in db value-path [])
                         count)]
      (if (= row-amount 0)
        1
        row-amount))))

(re-frame/reg-sub
  :application/question-group-row-count
  (fn [[_ id] _]
    (re-frame/subscribe [:application/ui-of id]))
  (fn [ui-of _]
    (:count ui-of 1)))

(re-frame/reg-sub
  :application/attachments
  (fn [_ _]
    (re-frame/subscribe [:application/answers]))
  (fn [answers [_ id question-group-idx]]
    (if (some? question-group-idx)
      (get-in answers [(keyword id) :values question-group-idx])
      (get-in answers [(keyword id) :values]))))

(re-frame/reg-sub
  :application/visible-attachments
  (fn [[_ id question-group-idx] _]
    (re-frame/subscribe [:application/attachments id question-group-idx]))
  (fn [attachments _]
    (doall (filterv (fn [[_ attachment]]
                      (not= :deleting
                            (:status attachment)))
                    (map-indexed (fn [idx attachment]
                                   [idx attachment]) attachments)))))

(re-frame/reg-sub
  :application/multiple-choice-option-checked?
  (fn [_ _]
    (re-frame/subscribe [:application/answers]))
  (fn [answers [_ id option-value question-group-idx]]
    (boolean
     (some #(= option-value %)
           (if (some? question-group-idx)
             (get-in answers [(keyword id) :value question-group-idx])
             (get-in answers [(keyword id) :value]))))))

(re-frame/reg-sub
  :application/single-choice-option-checked?
  (fn [db [_ parent-id option-value question-group-idx]]
    (let [value (get-in db (if question-group-idx
                             [:application :answers parent-id :values question-group-idx 0 :value]
                             [:application :answers parent-id :value]))]
      (= option-value value))))

(re-frame/reg-sub
  :application/single-choice-option-valid?
  (fn [db [_ parent-id question-group-idx]]
    (get-in db (if question-group-idx
                 [:application :answers parent-id :values question-group-idx 0 :valid]
                 [:application :answers parent-id :valid]))))

(defn- hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(re-frame/reg-sub
  :application/hakukohde-options
  (fn [db _]
    (:options (hakukohteet-field db))))

(re-frame/reg-sub
  :application/hakukohde-options-by-oid
  (fn [_ _]
    (into {} (map (juxt :value identity)
                  @(re-frame/subscribe [:application/hakukohde-options])))))

(re-frame/reg-sub
  :application/hakukohteet-editable?
  (fn [_ _]
    (and (< 1 (count @(re-frame/subscribe [:application/hakukohde-options])))
         (not @(re-frame/subscribe [:application/cannot-edit? :hakukohteet])))))

(re-frame/reg-sub
  :application/priorisoivat-hakukohderyhmat
  (fn [db _]
    (-> db :form :priorisoivat-hakukohderyhmat)))

(re-frame/reg-sub
  :application/hakukohde-offending-priorization?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-hakukohteet])
     (re-frame/subscribe [:application/priorisoivat-hakukohderyhmat])])
  (fn [[selected-hakukohteet priorisoivat-hakukohderyhmat] [_ hakukohde-oid]]
    (validators/offending-priorization hakukohde-oid
                                       selected-hakukohteet
                                       priorisoivat-hakukohderyhmat)))

(re-frame/reg-sub
  :application/hakukohde-editable?
  (fn [_ _]
    [(re-frame/subscribe [:application/tarjonta-hakukohteet-by-oid])
     (re-frame/subscribe [:application/virkailija?])])
  (fn [[hakukohteet virkailija?] [_ hakukohde-oid]]
    (or virkailija?
        (get-in hakukohteet [hakukohde-oid :hakuaika :on]))))

(re-frame/reg-sub
  :application/hakukohde-query
  (fn [db _] (get-in db [:application :hakukohde-query] "")))

(re-frame/reg-sub
  :application/show-more-hakukohdes?
  (fn [db _]
    (let [remaining-hakukohde-results (-> db :application :remaining-hakukohde-search-results count)]
      (> remaining-hakukohde-results 0))))

(re-frame/reg-sub
  :application/hakukohde-hits
  (fn [db _]
    (get-in db [:application :hakukohde-hits])))

(re-frame/reg-sub
  :application/get-hakukohde
  (fn [_ _]
    [(re-frame/subscribe [:application/tarjonta-hakukohteet])])
  (fn [[hakukohteet] [_ hakukohde-oid]]
    (first (filter #(= (:oid %) hakukohde-oid) hakukohteet ))))

(re-frame/reg-sub
  :application/hakukohde-selected?
  (fn [_ _]
    (re-frame/subscribe [:application/selected-hakukohteet]))
  (fn [selected-hakukohteet [_ hakukohde-oid]]
    (some #(= % hakukohde-oid) selected-hakukohteet)))

(re-frame/reg-sub
  :application/hakukohde-deleting?
  (fn [db [_ hakukohde-oid]]
    (some #{hakukohde-oid} (-> db :application :ui :hakukohteet :deleting))))

(re-frame/reg-sub
  :application/max-hakukohteet
  (fn [db _]
    (get-in (hakukohteet-field db)
            [:params :max-hakukohteet]
            nil)))

(re-frame/reg-sub
  :application/rajaavat-hakukohderyhmat
  (fn [db _]
    (get-in db [:form :rajaavat-hakukohderyhmat])))

(re-frame/reg-sub
  :application/rajaavat-hakukohteet
  (fn [_ _]
    [(re-frame/subscribe [:application/rajaavat-hakukohderyhmat])
     (re-frame/subscribe [:application/tarjonta-hakukohteet-by-oid])
     (re-frame/subscribe [:application/selected-hakukohteet])])
  (fn rajaavat-hakukohteet [[rajaavat tarjonta-hakukohteet selected-hakukohteet] [_ hakukohde-oid]]
    (let [hakukohde                     (get tarjonta-hakukohteet hakukohde-oid)
          possibly-limiting-hakukohteet (->> (remove #(= hakukohde-oid %) selected-hakukohteet)
                                             (map tarjonta-hakukohteet)
                                             (remove #(empty? (clojure.set/intersection
                                                               (set (:hakukohderyhmat %))
                                                               (set (map :hakukohderyhma-oid rajaavat))
                                                               (set (:hakukohderyhmat hakukohde))))))
          limiting-hakukohderyhma-oids  (-> (conj possibly-limiting-hakukohteet hakukohde)
                                            (validators/limitting-hakukohderyhmat rajaavat)
                                            set)]
      (remove #(empty? (clojure.set/intersection
                        limiting-hakukohderyhma-oids
                        (set (:hakukohderyhmat %))))
              possibly-limiting-hakukohteet))))

(re-frame/reg-sub
  :application/hakukohteet-full?
  (fn [_ _]
    [(re-frame/subscribe [:application/max-hakukohteet])
     (re-frame/subscribe [:application/selected-hakukohteet])])
  (fn [[max-hakukohteet selected-hakukohteet] _]
    (and (some? max-hakukohteet)
         (<= max-hakukohteet (count selected-hakukohteet)))))

(re-frame/reg-sub
  :application/default-languages
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-language])
     (re-frame/subscribe [:application/languages])])
  (fn [[selected-language languages] _]
    (concat [selected-language]
            languages
            [:fi :sv :en])))

(re-frame/reg-sub
  :application/fetching-selection-limits?
  (fn [db [_ id]]
    (if-let [limited (get db :selection-limited)]
      (and (limited (name id))
           (some #(get-in db [:application :validators-processing (keyword %)]) limited)))))

(re-frame/reg-sub
  :application/selection-over-network-uncertain?
  (fn [db _]
    (get-in db [:application :selection-over-network-uncertain?])))

(re-frame/reg-sub
  :application/limit-reached?
  (fn [db [_ question-id answer-id]]
    (let [original-value (get-in db [:application :answers question-id :original-value])]
      (if-let [limits (get-in db [:application :answers question-id :limit-reached])]
        (and (limits answer-id)
             (not= original-value answer-id))))))

(re-frame/reg-sub
  :application/hakukohde-label
  (fn [_ [_ hakukohde-oid]]
    (util/non-blank-val
     (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
             [hakukohde-oid :label])
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [_ [_ hakukohde-oid]]
    (util/non-blank-val
     (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
             [hakukohde-oid :description])
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [db _]
    (util/non-blank-val
     (:label (hakukohteet-field db))
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/show-hakukohde-search
  (fn [db _]
    (get-in db [:application :show-hakukohde-search])))

(re-frame/reg-sub
  :application/mouse-over-remove-question-group-button
  (fn [db [_ field-descriptor idx]]
    (get-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx])))

(re-frame/reg-sub
  :application/prioritize-hakukohteet?
  (fn [db _]
    (-> db :form :tarjonta :prioritize-hakukohteet)))

(re-frame/reg-sub
  :application/hakukohde-priority-number
  (fn [_ _]
    (re-frame/subscribe [:application/selected-hakukohteet]))
  (fn [selected-hakukohteet [_ hakukohde-oid]]
    (first (keep-indexed #(when (= hakukohde-oid %2) (inc %1))
                         selected-hakukohteet))))

(re-frame/reg-sub
  :application/validators-processing
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:validators-processing application)))

(re-frame/reg-sub
  :application/validator-processing?
  (fn [_ _]
    (re-frame/subscribe [:application/validators-processing]))
  (fn [validators-processing [_ id]]
    (contains? validators-processing (keyword id))))

(re-frame/reg-sub
  :application/show-validation-error-class?
  (fn [[_ id question-group-idx repeatable-idx] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/answer id question-group-idx repeatable-idx])
     (re-frame/subscribe [:application/validator-processing? id])])
  (fn [[field {:keys [value valid]} validator-processing?] _]
    (and (not valid)
         (or (afc/is-required-field? field)
             (-> field :params :numeric))
         (if (string? value)
           (not (cstr/blank? value))
           (not (empty? value)))
         (not validator-processing?))))

(re-frame/reg-sub
  :application/attachments-uploading?
  (fn [db]
    (not-empty (mapcat keys (vals (:attachments-uploading db))))))

(re-frame/reg-sub
  :application/attachment-download-link
  (fn [db [_ attachment-key]]
    (let [secret            (get-in db [:application :secret])
          virkailija-secret (get-in db [:application :virkailija-secret])]
      (cond (some? secret)
            (str "/hakemus/api/files/" attachment-key "?secret=" secret)
            (some? virkailija-secret)
            (str "/hakemus/api/files/" attachment-key "?virkailija-secret=" virkailija-secret)))))

(re-frame/reg-sub
  :application/language-version-link
  (fn [db [_ language]]
    (let [url               (url/url (.. js/window -location -href))
          virkailija-secret (get-in db [:application :virkailija-secret])]
      (-> (cond-> url
                  (some? virkailija-secret)
                  (assoc-in [:query "virkailija-secret"] virkailija-secret))
          (assoc-in [:query "lang"] (name language))
          str))))

(re-frame/reg-sub
  :application/edits?
  (fn [_ _]
    (re-frame/subscribe [:application/answers]))
  (fn [answers _]
      (let [changed? (fn [answer] (or (not= (:original-value answer) (:value answer))
                                      (some? (some #(= :deleting (:status %)) (:values answer)))))]
           (some? (some changed? (vals answers))))))
