; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.hakija.hakija-readonly
  (:require [clojure.string :as string]
            [re-frame.core :refer [subscribe]]
            [ataru.util :as util]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :as application-field]
            [ataru.application.option-visibility :as option-visibility]
            [ataru.hakija.arvosanat.arvosanat-render :as arvosanat]
            [ataru.hakija.components.hakukohde-details-component :refer [hakukohde-details-component]]))

(declare field)

(defn- from-multi-lang [text lang]
  (util/non-blank-val text [lang :fi :sv :en]))

(defn- split-if-string [s]
  (if (string? s)
    (string/split s #"\s*,\s*")
    s))

(defn- visible? [ui field-descriptor]
  (and (get-in @ui [(keyword (:id field-descriptor)) :visible?] true)
       (or (empty? (:children field-descriptor))
           (some #(and (visible? ui %)
                       (not= "infoElement" (:fieldClass %)))
                 (:children field-descriptor)))))

(declare child-fields)

(defn- text-nested-container [selected-options application lang group-idx]
  [:div.application-handling__nested-container.application-handling__nested-container--top-level
   {:data-test-id "tekstikenttä-lisäkysymykset"}
   (let [ui (subscribe [:state-query [:application :ui]])]
     (doall
       (for [option selected-options]
         ^{:key (:value option)}
         [:div.application-handling__nested-container-option
          (when (some #(visible? ui %) (:followups option))
            (into [:div.application-handling__nested-container]
                  (child-fields (:followups option) application lang ui group-idx)))])))])

(defn- text-readonly-text [field-descriptor values group-idx]
  [:div.application__readonly-text
   {:aria-labelledby (application-field/id-for-label field-descriptor group-idx)
    :data-test-id    "tekstikenttä-vastaus"}
   (cond (and (sequential? values) (< 1 (count values)))
         [:ul.application__form-field-list
          (map-indexed
            (fn [i value]
              ^{:key (str (:id field-descriptor) i)}
              [:li (application-field/render-paragraphs value)])
            values)]
         (sequential? values)
         (application-field/render-paragraphs (first values))
         :else
         (application-field/render-paragraphs values))])

(defn- text-form-field-label [field-descriptor lang group-idx]
  [:div.application__form-field-label
   {:id (application-field/id-for-label field-descriptor group-idx)}
   (str (from-multi-lang (:label field-descriptor) lang)
        (application-field/required-hint field-descriptor))])

(defn text [field-descriptor application lang group-idx]
  (let [id         (keyword (:id field-descriptor))
        answer     @(subscribe [:application/answer id])
        values     (cond-> (application-field/get-value answer group-idx)
                           (contains? field-descriptor :koodisto-source)
                           split-if-string
                           (application-field/predefined-value-answer? field-descriptor)
                           (application-field/replace-with-option-label (:options field-descriptor) lang))
        visible?   (option-visibility/visibility-checker field-descriptor values)
        options    (filter visible? (:options field-descriptor))
        followups? (some (comp not-empty :followups) options)]
    [:div.application__form-field
     [text-form-field-label field-descriptor lang group-idx]
     (if @(subscribe [:application/cannot-view? id])
       [:div.application__text-field-paragraph "***********"]
       [text-readonly-text field-descriptor values group-idx])
     (when followups?
       [text-nested-container options application lang group-idx])]))

(defn- attachment-list [attachments]
  (let [visible-attachments (filter #(not= (:status %) :deleting) attachments)]
    [:div
     (map (fn [value]
            ^{:key (str "attachment-" (:value value))}
            [:ul.application__form-field-list (str (:filename value) " (" (util/size-bytes->str (:size value)) ")")])
          visible-attachments)]))

(defn attachment [field-descriptor application lang question-group-index]
  (let [answer-key (keyword (application-field/answer-key field-descriptor))
        values     (if question-group-index
                     (-> application
                         :answers
                         answer-key
                         :values
                         (nth question-group-index nil))
                     (-> application :answers answer-key :values))]
    [:div.application__form-field
     [:div.application__form-field-label
      (str (from-multi-lang (:label field-descriptor) lang)
           (application-field/required-hint field-descriptor))]
     [attachment-list values]]))

(defn child-fields [children application lang ui question-group-id]
  (for [child children
        :when (visible? ui child)]
    ^{:key (str (:id child)
                (when question-group-id
                  (str "-" question-group-id)))}
    (if (:per-hakukohde child)
      [:div.readonly__per-question-wrapper
       [:div.application__form-field-label.application__form-field__original-question
        (from-multi-lang (:label child) lang)]
       (for [duplicate-field (filter #(= (:original-question %) (:id child)) children)]
         ^{:key (str "duplicate-" (:id duplicate-field))}
         [:section
           [hakukohde-details-component duplicate-field]
           [field duplicate-field application lang nil]])]
      (when (not (:duplikoitu-kysymys-hakukohde-oid child))
        [field child application lang question-group-id]))))

(defn wrapper [_ _ _ _]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
      [:div.application__wrapper-element
       [:div.application__wrapper-heading
        [:h2 (from-multi-lang (:label content) lang)]
        [application-field/scroll-to-anchor content]]
       (into [:div.application__wrapper-contents]
             (child-fields children application lang ui nil))])))

(defn question-group [_ _ _ _]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
      (let [groups-amount (->> content :id keyword (get @ui) :count)]
        [:div.application__question-group.application__read-only
         [:p.application__read-only-heading-text
          (from-multi-lang (:label content) lang)]
         (into [:div]
               (for [idx (range groups-amount)]
                 ^{:key (str (:id content) "-" idx)}
                 [:div.application__question-group-row
                  (into [:div.application__question-group-row-content.application__form-field]
                        (child-fields children application lang ui idx))]))]))))

(defn row-container [_ _ _ _]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [application lang children question-group-index]
      (into [:div] (child-fields children application lang ui question-group-index)))))

(defn- fieldset-answer-table [answers]
  [:tbody
   (doall
     (for [[idx values] (map vector (range) answers)]
       (into
         [:tr {:key (str idx "-" (apply str values))}]
         (for [value values]
           [:td.application__readonly-adjacent-cell (str value)]))))])

(defn fieldset [field-descriptor application lang children question-group-idx]
  (let [fieldset-answers (->> children
                              (map #(if (some? question-group-idx)
                                      (get-in application [:answers (keyword (:id %)) :value question-group-idx])
                                      (get-in application [:answers (keyword (:id %)) :value])))
                              (apply map vector))]
    [:div.application__form-field
     [:div.application__form-field-label
      (str (from-multi-lang (:label field-descriptor) lang)
           (application-field/required-hint field-descriptor))]
     [:table.application__readonly-adjacent
      [:thead
       (into [:tr]
         (for [child children]
           [:th.application__readonly-adjacent--header
            (str (from-multi-lang (:label child) lang)
                 (application-field/required-hint field-descriptor))]))]
      [fieldset-answer-table fieldset-answers]]]))

(defn- selectable [content application lang question-group-idx]
  (let [{:keys [arvosanat-taulukko?]} (:readonly-render-options content)
        {:keys [unselected-label
                data-test-id]} content]
    [:div.application__form-field
     {:class (when arvosanat-taulukko?
               "application__form-field--readonly-arvosanat-taulukko")}
     [:div.application__form-field-label
      {:class (when arvosanat-taulukko?
                "application__form-field-label--readonly-arvosanat-taulukko")}
      (if (and arvosanat-taulukko?
               (seq unselected-label))
        (from-multi-lang unselected-label lang)
        (from-multi-lang (:label content) lang))]
     [:div.application-handling__nested-container
      {:class (when arvosanat-taulukko?
                "application-handling__nested-container--readonly-arvosanat-taulukko")}
      (let [values           (-> @(subscribe [:application/answer (:id content) question-group-idx nil])
                                 :value
                                 vector
                                 flatten
                                 set)
            selected-options (filter #(contains? values (:value %)) (:options content))
            ui               (subscribe [:state-query [:application :ui]])]
        (doall
          (for [option selected-options]
            ^{:key (:value option)}
            [:div
             [:p.application__text-field-paragraph
              {:class        (when arvosanat-taulukko?
                               "application__text-field-paragraph--readonly-arvosanat-taulukko")
               :data-test-id data-test-id}
              (from-multi-lang (:label option) lang)]
             (when (some #(visible? ui %) (:followups option))
               (into [:div.application-handling__nested-container]
                     (child-fields (:followups option) application lang ui question-group-idx)))])))]]))

(defn- multiple-choice [content application lang question-group-idx]
  [:div.application__form-field
   [:div.application__form-field-label
    (from-multi-lang (:label content) lang)]
   [:div.application-handling__nested-container
    (let [selected-options (filterv #(deref (subscribe [:application/multiple-choice-option-checked? (keyword (:id content)) (:value %) question-group-idx]))
                                    (:options content))
          ui               (subscribe [:state-query [:application :ui]])]
      (doall
       (for [option selected-options]
         ^{:key (:value option)}
         [:div
          [:p.application__text-field-paragraph
           (from-multi-lang (:label option) lang)]
          (when (some #(visible? ui %) (:followups option))
            (into [:div.application-handling__nested-container]
                  (child-fields (:followups option) application lang ui question-group-idx)))])))]])

(defn- selected-hakukohde-row
  [hakukohde-oid]
  [:div.application__selected-hakukohde-row
   (when @(subscribe [:application/prioritize-hakukohteet?])
     [:p.application__hakukohde-priority-number-readonly
      @(subscribe [:application/hakukohde-priority-number hakukohde-oid])])
   [:div.application__selected-hakukohde-row--content
    [:div.application__hakukohde-header
     @(subscribe [:application/hakukohde-label hakukohde-oid])]
    [:div.application__hakukohde-description
     @(subscribe [:application/hakukohde-description hakukohde-oid])]]])

(defn- hakukohde-selection-header
  [content]
  [:div.application__wrapper-heading
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [application-field/scroll-to-anchor content]])

(defn- hakukohteet
  [content]
  [:div.application__wrapper-element
   [hakukohde-selection-header content]
   [:div.application__wrapper-contents
    [:div.application__form-field
     [:div.application__hakukohde-selected-list
      (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
        ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
        [selected-hakukohde-row hakukohde-oid])]]]])

(defn- render-component-readonly [{:keys [field-descriptor
                                          application
                                          lang
                                          question-group-index]}]
  (match field-descriptor
         {:fieldClass "wrapperElement" :module "person-info" :children children} [wrapper field-descriptor application lang children]
         {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper field-descriptor application lang children]
         {:fieldClass "questionGroup" :fieldType "fieldset" :children children} [question-group field-descriptor application lang children]
         {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children question-group-index]
         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset field-descriptor application lang children question-group-index]
         {:fieldClass "formField" :exclude-from-answers true} nil
         {:fieldClass "pohjakoulutusristiriita"} nil
         {:fieldClass "infoElement"} nil
         {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor application lang question-group-index]
         {:fieldClass "formField" :fieldType (:or "dropdown" "singleChoice")} [selectable field-descriptor application lang question-group-index]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea")} [text field-descriptor application lang question-group-index]
         {:fieldClass "formField" :fieldType "attachment"} [attachment field-descriptor application lang question-group-index]
         {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet field-descriptor]))

(defn field
  [field-descriptor application lang question-group-index]
  (let [render-fn (case (:version field-descriptor)
                    "oppiaineen-arvosanat" arvosanat/render-arvosanat-component-readonly
                    render-component-readonly)]
    (render-fn {:field-descriptor     field-descriptor
                :application          application
                :lang                 lang
                :render-field         field
                :question-group-index question-group-index})))

(defn- application-language [{:keys [lang]}]
  (when (some? lang)
    (-> lang
        string/lower-case
        keyword)))

(defn readonly-fields [form application]
  (when form
    (let [lang (or (:selected-language form)                ; languages is set to form in the applicant side
                   (application-language application)       ; language is set to application when in officer side
                   :fi)
          ui   (subscribe [:state-query [:application :ui]])]
      (into [:div.application__readonly-container.animated.fadeIn]
            (child-fields (:content form) application lang ui nil)))))
