(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]
            [ataru.application-common.application-field-common :refer [required-validators pad sanitize-value]]
            [clojure.core.match :refer [match]]))

(defn- initial-valid-status [flattened-form-fields preselected-hakukohteet]
  (->> flattened-form-fields
       (filter util/answerable?)
       (map-indexed
        (fn [_ field]
          (match [field]
            [{:id      "hakukohteet"
              :label   label
              :options options}]
            (let [values (cond (= 1 (count options))
                               [{:value (:value (first options))
                                 :valid true}]
                               (some? preselected-hakukohteet)
                               (mapv (fn [oid] {:value oid :valid true}) preselected-hakukohteet)
                               :else
                               [])]
              [:hakukohteet {:valid  (not (empty? values))
                             :label  label
                             :value  (mapv :value values)
                             :values values}])
            [{:id    "pohjakoulutusristiriita"
              :label label}]
            [:pohjakoulutusristiriita {:valid  true
                                       :label  label
                                       :value  nil
                                       :values {:value nil
                                                :valid true}}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  (:or "textField" "textArea")
              :label      label
              :params     {:question-group-id _}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  [[""]]
                             :values [[{:value ""
                                        :valid (not required?)}]]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "textField"
              :label      label
              :params     {:adjacent-field-id _}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  [""]
                             :values [{:value ""
                                       :valid (not required?)}]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "textField"
              :label      label
              :params     {:repeatable true}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  [""]
                             :values [{:value ""
                                       :valid (not required?)}]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  (:or "textField" "textArea")
              :label      label}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  ""
                             :values {:value ""
                                      :valid (not required?)}}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "dropdown"
              :label      label
              :params     {:question-group-id _}
              :options    options}]
            (let [value     (some #(when (:default-value %) (:value %)) options)
                  required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (or (some? value) (not required?))
                             :label  label
                             :value  [[(or value "")]]
                             :values [[{:value (or value "")
                                        :valid (or (some? value) (not required?))}]]}])
            [{:id         id
              :fieldClass "formField"
              :fieldType  "dropdown"
              :label      label
              :options    options}]
            (let [value     (some #(when (:default-value %) (:value %)) options)
                  required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (or (some? value) (not required?))
                             :label  label
                             :value  (or value "")
                             :values {:value (or value "")
                                      :valid (or (some? value)
                                                    (or (not required?)
                                                    (boolean (:per-hakukohde field))))}}])
            [{:id         id
              :fieldClass "formField"
              :fieldType  "singleChoice"
              :label      label
              :params     {:question-group-id _}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :value  [[nil]]
                             :values [[{:value nil
                                        :valid (not required?)}]]
                             :label  label}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "singleChoice"
              :label      label}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (or (not required?)
                                         (boolean (:per-hakukohde field)))
                             :value  nil
                             :values {:value nil
                                      :valid (or (not required?)
                                                 (boolean (:per-hakukohde field)))}
                             :label  label}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "multipleChoice"
              :label      label
              :params     {:question-group-id _}}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                              (:validators field)))
                           :value  [[]]
                           :values [[]]
                           :label  label}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  "multipleChoice"
              :label      label}]
            [(keyword id) {:valid  (or (not (some #(contains? required-validators %)
                                             (:validators field)))
                                  (boolean (:per-hakukohde field)))
                      :value  []
                      :values []
                      :label  label}]
            [{:id         id
              :fieldClass "formField"
              :fieldType  "attachment"
              :label      label
              :params     {:question-group-id _}}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                              (:validators field)))
                           :value  [[]]
                           :values [[]]
                           :label  label}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  "attachment"
              :label      label}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                              (:validators field)))
                           :value  []
                           :values []
                           :label  label}])))
       (into {})))

(defn create-initial-answers
  "Create initial answer structure based on form structure.
  Validity, dropdown default value and default hakukohde for now."
  [flat-form-content preselected-hakukohde-oids]
  (initial-valid-status flat-form-content preselected-hakukohde-oids))

(defn contains-id? [content id]
  (contains? (set (map :id content)) id))

(defn find-parent-field-descriptor [parent content id]
  (if (contains-id? content id)
    parent
    (let [wrappers (->> content
                        (filter :children))]
      (->> wrappers
           (map (fn [wrapper] (find-parent-field-descriptor wrapper (:children wrapper) id)))
           (filter some?)
           first))))

(defn answers->valid-status [all-answers ui flat-form-content]
  {:invalid-fields (for [field flat-form-content
                         :let  [key (keyword (:id field))
                                answer (get all-answers key)]
                         :when (and (some? answer)
                                    (not (:valid answer))
                                    (get-in ui [key :visible?] true))]
                     {:key   key
                      :label (:label answer)})})

(defn- sanitize-attachment-value-by-state [value values]
  (when (not= :deleting (:status values))
    value))

(defn- sanitize-attachment-values-by-states [value values]
       (if (and (nil? values)
                (nil? value))
           nil
           (filterv identity (map-indexed (fn [idx value]
                                              (sanitize-attachment-value-by-state value (get values idx))) value))))

(defn- sanitize-attachment-value [value values question-group-highest-dimension]
  (if (vector? value)
    (if (or (vector? (first value)) (nil? (first value)))
      (pad (or question-group-highest-dimension 0)
           (map-indexed (fn [idx value]
                          (sanitize-attachment-values-by-states value (get values idx))) value) nil)
      (sanitize-attachment-values-by-states value values))
    (sanitize-attachment-value-by-state value values)))

(defn- question-group-shared-answers [ans-key answers flat-form-map]
  (if-let [question-group-id (-> (get flat-form-map ans-key) :params :question-group-id)]
    (->> flat-form-map
         (filter (fn [[_ v]] (= (-> v :params :question-group-id) question-group-id)))
         (keep (fn [[k _]] (get answers k))))))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (util/form-fields-by-id form)]
    (for [[ans-key {:keys [value values]}] answers
          :let
          [field-descriptor (get flat-form-map ans-key)
           question-group-highest-dimension (->> (question-group-shared-answers ans-key answers flat-form-map)
                                                 (map #(count (:value %)))
                                                 (distinct)
                                                 (sort (comp - compare))
                                                 (first))
           original-question (:original-question field-descriptor)
           duplikoitu-kysymys-hakukohde-oid (:duplikoitu-kysymys-hakukohde-oid field-descriptor)]
          :when
          (and (or (= :birth-date ans-key)
                   (= :gender ans-key)
                   (get-in ui [ans-key :visible?] true))
               (not (:exclude-from-answers field-descriptor)))]
      (cond->
        {:key       (:id field-descriptor)
         :value     (cond (#{"attachment"} (:fieldType field-descriptor))
                          (sanitize-attachment-value value values question-group-highest-dimension)

                          :else
                          (sanitize-value field-descriptor value question-group-highest-dimension))
         :fieldType (:fieldType field-descriptor)
         :label     (:label field-descriptor)}
        (some? original-question) (assoc :original-question original-question)
        (some? duplikoitu-kysymys-hakukohde-oid) (assoc :duplikoitu-kysymys-hakukohde-oid duplikoitu-kysymys-hakukohde-oid)))))

(defn create-application-to-submit [application form lang strict-warnings-on-unchanged-edits?]
  (let [{secret :secret virkailija-secret :virkailija-secret} application]
    (cond-> {:form      (:id form)
             :strict-warnings-on-unchanged-edits? (if (nil? strict-warnings-on-unchanged-edits?)
                                                    true
                                                    (boolean strict-warnings-on-unchanged-edits?))
             :lang      lang
             :haku      (-> form :tarjonta :haku-oid)
             :hakukohde (map :value (get-in application [:answers :hakukohteet :values] []))

             :answers   (create-answers-to-submit (:answers application) form (:ui application))}

            (some? (get application :selection-id)) (assoc :selection-id (get application :selection-id))

            (some? secret) (assoc :secret secret)

            (some? virkailija-secret) (assoc :virkailija-secret virkailija-secret))))

(defn extract-wrapper-sections [form]
  (->> (:content form)
       (filter #(and (= (:fieldClass %) "wrapperElement")
                     (not= (:fieldType %) "adjacentfieldset")))
       (map #(select-keys % [:id :label :children]))))

