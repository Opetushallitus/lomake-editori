; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.virkailija.views.virkailija-readonly
  (:require [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       get-value
                                                                       render-paragraphs
                                                                       replace-with-option-label
                                                                       scroll-to-anchor
                                                                       copy-link]]
            [ataru.application.option-visibility :as option-visibility]
            [ataru.component-data.component-util :refer [answer-to-always-include?]]
            [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.set :as set]
            [clojure.string :as string]
            [ataru.application_common.comparators :as comparators]
            [cljs.core.match :refer-macros [match]]
            [goog.string :as s]))

(declare field)

(def exclude-always-included #(not (answer-to-always-include? %)))

(defn- from-multi-lang [text lang]
  (util/non-blank-val text [lang :fi :sv :en]))

(defn- ylioppilastutkinto? [application]
  (boolean (some #(or (= "pohjakoulutus_yo" %)
                      (= "pohjakoulutus_yo_ammatillinen" %)
                      (= "pohjakoulutus_yo_kansainvalinen_suomessa" %)
                      (= "pohjakoulutus_yo_ulkomainen" %))
                 (get-in application [:answers :higher-completed-base-education :value]))))

(defn- selected-hakukohteet [application]
  (get-in application [:answers :hakukohteet :value]))

(defn- selected-hakukohteet-and-ryhmat-from-application [application hakukohteet]
  (let [selected-hakukohteet                   (set (selected-hakukohteet application))
        selected-hakukohteet-tarjonta          (vals (select-keys hakukohteet selected-hakukohteet))
        selected-hakukohderyhmat               (set (mapcat :ryhmaliitokset selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet-tarjonta (set (remove :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?
                                                            selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohderyhmat      (set (mapcat :ryhmaliitokset selected-ei-jyemp-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet          (set (map :oid selected-ei-jyemp-hakukohteet-tarjonta))]
    [(set/union selected-hakukohteet selected-hakukohderyhmat)
     (set/union selected-ei-jyemp-hakukohteet selected-ei-jyemp-hakukohderyhmat)]))

(defn- visible? [field-descriptor application hakukohteet-and-ryhmat]
  (let [[selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat] hakukohteet-and-ryhmat
        jyemp? (and (ylioppilastutkinto? application)
                    (contains? (:excluded-attachment-ids-when-yo-and-jyemp application) (:id field-descriptor)))
        belongs-to (set (concat (:belongs-to-hakukohderyhma field-descriptor)
                                (:belongs-to-hakukohteet field-descriptor)))]
    (and (not (get-in field-descriptor [:params :hidden] false))
         (not= "infoElement" (:fieldClass field-descriptor))
         (not (:exclude-from-answers field-descriptor))
         (or (not jyemp?) (not (empty? selected-ei-jyemp-hakukohteet-and-ryhmat)))
         (or (and (empty? (:belongs-to-hakukohteet field-descriptor))
                  (empty? (:belongs-to-hakukohderyhma field-descriptor)))

             (not (empty? (set/intersection
                            belongs-to
                            (if jyemp?
                              selected-ei-jyemp-hakukohteet-and-ryhmat
                              selected-hakukohteet-and-ryhmat)))))
         (or (empty? (:children field-descriptor))
             (some #(visible? % application hakukohteet-and-ryhmat) (:children field-descriptor))))))

(defn- text-form-field-nested-container [selected-options lang application hakukohteet-and-ryhmat question-group-idx]
  [:div.application-handling__nested-container--top-level
   {:data-test-id "tekstikenttä-lisäkysymykset"}
   (doall
     (for [option selected-options]
       ^{:key (:value option)}
       [:div.application-handling__nested-container-option
        (when (some #(visible? % application hakukohteet-and-ryhmat) (:followups option))
          [:div.application-handling__nested-container
           (for [followup (:followups option)]
             ^{:key (:id followup)}
             [field followup application hakukohteet-and-ryhmat lang question-group-idx false])])]))])

(defn- text-form-field-values [id values]
  [:div.application__form-field-value
   {:data-test-id "tekstikenttä-vastaus"}
   (cond (and (sequential? values) (< 1 (count values)))
         [:ul.application__form-field-list
          (map-indexed
            (fn [i value]
              ^{:key (str id i)}
              [:li (render-paragraphs value)])
            values)]
         (sequential? values)
         (render-paragraphs (first values))
         :else
         (render-paragraphs values))])

(defn- text-form-field-label [id field-descriptor lang]
  [:label.application__form-field-label
   [:span
    (str (from-multi-lang (:label field-descriptor) lang)
         (required-hint field-descriptor))
    [copy-link id :shared-use-warning? false :include? exclude-always-included]]])

(defn text [field-descriptor application hakukohteet-and-ryhmat lang group-idx]
  (let [id               (keyword (:id field-descriptor))
        use-onr-info?    (contains? (:person application) id)
        values           (replace-with-option-label (if use-onr-info?
                                                      (-> application :person id)
                                                      (get-value (-> application :answers id) group-idx))
                                                    (:options field-descriptor)
                                                    lang)
        visible?         (option-visibility/visibility-checker field-descriptor values)
        options          (filter visible? (:options field-descriptor))
        followups?       (some (comp not-empty :followups) options)
        highlight-field? (subscribe [:application/field-highlighted? id])]
    [:div.application__form-field
     {:class (when @highlight-field? "highlighted")
      :id    id}
     [text-form-field-label id field-descriptor lang]
     [text-form-field-values id values]
     (when followups?
      [text-form-field-nested-container options lang application hakukohteet-and-ryhmat group-idx])]))

(defn- attachment-item [file-key virus-scan-status virus-status-elem text]
  [:div.application__virkailija-readonly-attachment-area
   (if (= virus-scan-status "done")
     [:a {:href (str "/lomake-editori/api/files/content/" file-key)}
      text]
     text)
   virus-status-elem])

(defn- attachment-list [attachments]
  [:div.application-handling__nested-container
   (doall
     (map-indexed (fn attachment->link [idx {file-key :key filename :filename size :size virus-scan-status :virus-scan-status}]
                    (let [text              (str filename " (" (util/size-bytes->str size) ")")
                          component-key     (str "attachment-div-" idx)
                          virus-status-elem (case virus-scan-status
                                              "not_started" [:span.application__virkailija-readonly-attachment-virus-status-not-started
                                                             (s/format "| %s..." @(subscribe [:editor/virkailija-translation :checking]))]
                                              "virus_found" [:span.application__virkailija-readonly-attachment-virus-status-virus-found
                                                             (s/format "| %s" @(subscribe [:editor/virkailija-translation :virus-found]))]
                                              "failed" [:span.application__virkailija-readonly-attachment-virus-status-failed
                                                        (s/format "| %s" @(subscribe [:editor/virkailija-translation :virus-scan-failed]))]
                                              "done" nil
                                              [:span.application__virkailija-readonly-attachment-virus-status-failed
                                               (s/format "| %s" @(subscribe [:editor/virkailija-translation :error]))])]
                      [:div.application__virkailija-readonly-attachment
                       {:key component-key}
                       [attachment-item file-key virus-scan-status virus-status-elem text]]))
                  (filter identity attachments)))])

(defn attachment [field-descriptor application lang group-idx]
  (let [id         (:id field-descriptor)
        answer-key (keyword (answer-key field-descriptor))
        values     (cond-> (get-in application [:answers answer-key :values])
                           (some? group-idx)
                           (nth group-idx))]
    [:div.application__form-field
     [:label.application__form-field-label
      [:span
       (str (from-multi-lang (:label field-descriptor) lang)
            (required-hint field-descriptor))
       [copy-link id :shared-use-warning? false :include? exclude-always-included]]]
     [attachment-list values]]))

(defn wrapper [content application hakukohteet-and-ryhmat lang children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:div.application__wrapper-heading
    [:h2 (from-multi-lang (:label content) lang)]
    [scroll-to-anchor content]]
   (into [:div.application__wrapper-contents]
         (for [child children]
           [field child application hakukohteet-and-ryhmat lang nil false]))])

(defn row-container [_ _ _ _ group-idx person-info-field?]
  (fn [application hakukohteet-and-ryhmat lang children]
    (into [:div] (for [child children]
                   [field child application hakukohteet-and-ryhmat lang group-idx person-info-field?]))))

(defn- fieldset-answer-table [answers]
  [:tbody
   (doall
     (for [[idx values] (map vector (range) answers)]
       (into
         [:tr {:key (str idx "-" (apply str values))}]
         (for [value values]
           [:td (str value)]))))])

(defn- get-answer-value [application child group-idx]
  (if (some? group-idx)
    (get-in application [:answers (keyword (:id child)) :value group-idx])
    (get-in application [:answers (keyword (:id child)) :value])))

(defn fieldset [field-descriptor application lang children group-idx]
  (let [fieldset-answers (some->> (not-empty children)
                                  (map #(get-answer-value application % group-idx))
                                  (apply map vector))]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (from-multi-lang (:label field-descriptor) lang)
           (required-hint field-descriptor))]
     [:table.application__readonly-adjacent
      [:thead
       (into [:tr]
             (for [child children]
               [:th.application__readonly-adjacent--header
                (str (from-multi-lang (:label child) lang)
                     (required-hint field-descriptor))]))]
      [fieldset-answer-table fieldset-answers]]]))

(defn- selectable [content application hakukohteet-and-ryhmat lang question-group-idx]
  [:div.application__form-field
   [:div.application__form-field-label--selectable
    [:div.application__form-field-label
     [:span
      (from-multi-lang (:label content) lang)
      [copy-link (:id content) :shared-use-warning? false :include? exclude-always-included]]]
    (let [values           (-> (cond-> (get-in application [:answers (keyword (:id content)) :value])
                                       (some? question-group-idx)
                                       (nth question-group-idx nil))
                               vector
                               flatten
                               set)
          selected-options (filter #(contains? values (:value %))
                                   (:options content))
          values-wo-option (remove (fn [value]
                                     (some #(= value (:value %))
                                           selected-options))
                                   values)]
      [:div.application-handling__nested-container
       (doall
        (for [option selected-options]
          ^{:key (:value option)}
          [:div
           [:p.application__text-field-paragraph
            (from-multi-lang (:label option) lang)]
           (when (some #(visible? % application hakukohteet-and-ryhmat) (:followups option))
             [:div.application-handling__nested-container
              (for [followup (:followups option)]
                ^{:key (:id followup)}
                [field followup application hakukohteet-and-ryhmat lang question-group-idx false])])]))
       (doall
        (for [value values-wo-option]
          ^{:key (str "unknown-option-" value)}
          [:div
           [:p.application__text-field-paragraph
            (if value
              (str @(subscribe [:editor/virkailija-translation :unknown-option]) " " value)
              (str @(subscribe [:editor/virkailija-translation :empty-option])))]]))])]])

(defn- haku-row [haku-oid]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper
    [:div.application-handling__review-area-haku-heading
     (str @(subscribe [:application/haku-name haku-oid]) " ")
     (when-let [url @(subscribe [:application/haun-tiedot-url haku-oid])]
       [:a.editor-form__haku-admin-link
        {:href   url
         :target "_blank"}
        [:i.zmdi.zmdi-open-in-new]])]]])

(defn- hakukohteet-list-row [hakukohde-oid]
  (let [selected-hakukohde-oids (set @(subscribe [:state-query [:application :selected-review-hakukohde-oids]]))
        selected?               (contains? selected-hakukohde-oids hakukohde-oid)]
    [:div.application__form-field
     [:div.application-handling__hakukohde-wrapper.application-handling__hakukohde--selectable
      {:class    (when selected?
                   "application-handling__hakukohde--selected")
       :on-click (fn [_]
                   (dispatch [:application/select-review-hakukohde hakukohde-oid]))}
      (when @(subscribe [:application/prioritize-hakukohteet?])
        [:p.application__hakukohde-priority-number-readonly
         @(subscribe [:application/hakukohde-priority-number hakukohde-oid])])
      [:div
       [:div.application-handling__review-area-hakukohde-heading
        (str @(subscribe [:application/hakukohde-label hakukohde-oid]) " ")
        [:a.editor-form__haku-admin-link
         {:href   @(subscribe [:application/hakukohteen-tiedot-url hakukohde-oid])
          :target "_blank"}
         [:i.zmdi.zmdi-open-in-new]]]
       [:div.application-handling__review-area-koulutus-heading
        @(subscribe [:application/hakukohde-description hakukohde-oid])]]]]))

(defn- hakukohteet [content]
  (when-let [hakukohteet (seq @(subscribe [:application/hakutoiveet]))]
    [:div.application__wrapper-element.application__wrapper-element--border
     [:div.application__wrapper-heading
      [:h2 @(subscribe [:application/hakukohteet-header])]
      [scroll-to-anchor content]]
     [:div.application__wrapper-contents
      {:class (when @(subscribe [:application/field-highlighted? :hakukohteet])
                "highlighted")
       :id    "hakukohteet"}
      [haku-row @(subscribe [:state-query [:application :selected-application-and-form :application :haku]])]
      (for [hakukohde-oid hakukohteet]
        ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
        [hakukohteet-list-row hakukohde-oid])]]))

(defn- person-info-module [content application hakukohteet-and-ryhmat lang]
  [:div.application__person-info-wrapper.application__wrapper-element
   [:div.application__wrapper-element.application__wrapper-element--border
    [:div.application__wrapper-heading
     [:h2 (from-multi-lang (:label content) lang)]
     (when (-> application :person :turvakielto)
       [:p.security-block
        [:i.zmdi.zmdi-account-o]
        "Henkilöllä turvakielto!"])
     [scroll-to-anchor content]]
    (into [:div.application__wrapper-contents]
          (for [child (:children content)
                :when (not (:exclude-from-answers child))]
            [field child application hakukohteet-and-ryhmat lang nil true]))]])

(defn- repeat-count
  [application question-group-children]
  (util/reduce-form-fields
   (fn [max-count child]
     (max max-count
          (count (get-in application [:answers (keyword (:id child)) :value]))))
   0
   question-group-children))

(defn- question-group [content application hakukohteet-and-ryhmat lang children]
  [:div.application__question-group
   [:h3.application__question-group-heading
    (from-multi-lang (:label content) lang)]
   (for [idx (range (repeat-count application children))]
     ^{:key (str "question-group-" (:id content) "-" idx)}
     [:div.application__question-group-repeat
      (for [child children]
        ^{:key (str "question-group-" (:id content) "-" idx "-" (:id child))}
        [field child application hakukohteet-and-ryhmat lang idx false])])])

(defn- nationality-field [field-descriptor application lang children]
  (let [field            (first children)
        id               (keyword (:id field-descriptor))
        values           (flatten (replace-with-option-label (-> application :person :nationality)
                                                             (:options field)
                                                             lang))
        highlight-field? (subscribe [:application/field-highlighted? id])]
    [:div.application__form-field
     {:class (when @highlight-field? "highlighted")
      :id    id}
     [:label.application__form-field-label
      (str (from-multi-lang (:label field) lang)
           (required-hint field))]
     [:div.application__form-field-value
      [:p.application__text-field-paragraph
       (string/join ", " values)]]]))

(defn field
  [content application hakukohteet-and-ryhmat lang group-idx person-info-field?]
  (when (visible? content application hakukohteet-and-ryhmat)
    (match content
      {:module "person-info"} [person-info-module content application hakukohteet-and-ryhmat lang]
      {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application hakukohteet-and-ryhmat lang children]
      {:fieldClass "questionGroup" :fieldType "fieldset" :children children}
           (if person-info-field?
             (nationality-field content application lang children)
             [question-group content application hakukohteet-and-ryhmat lang children])
      {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application hakukohteet-and-ryhmat lang children group-idx person-info-field?]
      {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children group-idx]
      {:fieldClass "formField" :fieldType (:or "dropdown" "multipleChoice" "singleChoice")}
      (if person-info-field?
        (text content application hakukohteet-and-ryhmat lang group-idx)
        [selectable content application hakukohteet-and-ryhmat lang group-idx])
      {:fieldClass "formField" :fieldType (:or "textField" "textArea")} (text content application hakukohteet-and-ryhmat lang group-idx)
      {:fieldClass "formField" :fieldType "attachment"} [attachment content application lang group-idx]
      {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet content]
      {:fieldClass "pohjakoulutusristiriita"} nil)))

(defn- application-language [{:keys [lang]}]
  (when (some? lang)
    (-> lang
        clojure.string/lower-case
        keyword)))

(defn readonly-fields [form application hakukohteet]
  (when form
    (let [lang (or (:selected-language form)                ; languages is set to form in the applicant side
                   (application-language application)       ; language is set to application when in officer side
                   :fi)
          hakukohteet-and-ryhmat (selected-hakukohteet-and-ryhmat-from-application application hakukohteet)
          selected-hakukohteet (get-in application [:answers :hakukohteet :value])]
      (into [:div.application__readonly-container]
        (for [content (:content form)]
          (if (:per-hakukohde content)
            [:div.readonly__per-question-wrapper
             [:div.application__form-field-label.application__form-field__original-question
              [:span
               (from-multi-lang (:label content) lang)]]
             (for [duplicate-field (sort (comparators/duplikoitu-kysymys-hakukohde-comparator selected-hakukohteet)(map #(-> content
                                         (dissoc :per-hakukohde)
                                         (assoc :id (:key %)
                                                :original-question (:original-question %)
                                                :duplikoitu-kysymys-hakukohde-oid (:duplikoitu-kysymys-hakukohde-oid %)))
                                        (filter #(= (:original-question %) (:id content)) (vals (:answers application)))))]
               ^{:key (str "duplicate-" (:id duplicate-field))}
               [:section
                [:div.application__per-hakukohde.application__form-field
                 (str @(subscribe [:application/hakukohde-label (:duplikoitu-kysymys-hakukohde-oid duplicate-field)]) " ")]
                [selectable duplicate-field application hakukohteet-and-ryhmat lang nil]])]
          [field content application hakukohteet-and-ryhmat lang nil]))))))
