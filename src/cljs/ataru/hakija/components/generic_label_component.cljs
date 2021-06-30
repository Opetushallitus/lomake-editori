(ns ataru.hakija.components.generic-label-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn id-for-label
  [field-descriptor idx]
  (application-field/id-for-label field-descriptor idx))

(defn generic-label [_ _]
  (let [languages  (re-frame/subscribe [:application/default-languages])
        lang  @(re-frame/subscribe [:application/form-language])]
    (fn [field-descriptor idx]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)
            is-duplicate-question (not (nil? (:duplikoitu-kysymys-hakukohde-oid field-descriptor)))
            duplicate-question-class (if is-duplicate-question "application__form-field-label application__form-field-label--duplicate-question" "application__form-field-label")
            hakukohde @(re-frame/subscribe [:application/get-hakukohde (:duplikoitu-kysymys-hakukohde-oid field-descriptor)])
            name          (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
            tarjoaja-name (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]
        [:div
         {:id (id-for-label field-descriptor idx) :class duplicate-question-class}
         (when-not is-duplicate-question
           [:span (str label (application-field/required-hint field-descriptor))])
         (when is-duplicate-question
           [:span (str name " " tarjoaja-name)])
         [application-field/scroll-to-anchor field-descriptor]]))))
