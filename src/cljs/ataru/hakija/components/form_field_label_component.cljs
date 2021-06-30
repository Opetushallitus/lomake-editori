(ns ataru.hakija.components.form-field-label-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]
            [re-frame.core :as re-frame :refer [subscribe]]
            [ataru.hakija.components.hakukohde-details-component :refer [hakukohde-details-component]]))

(defn form-field-label [_ _]
  (let [languages  (re-frame/subscribe [:application/default-languages])]
    (fn [field-descriptor form-field-id]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)
            is-duplicate-question (not (nil? (:duplikoitu-kysymys-hakukohde-oid field-descriptor)))
            duplicate-question-class (if is-duplicate-question "application__form-field-label application__form-field-label--duplicate-question" "application__form-field-label")]
        [:label
         {:class duplicate-question-class
          :for form-field-id}
         (when-not is-duplicate-question
           [:span (str label (application-field/required-hint field-descriptor))])
         (when is-duplicate-question
           [hakukohde-details-component field-descriptor])
         [application-field/scroll-to-anchor field-descriptor]]))))