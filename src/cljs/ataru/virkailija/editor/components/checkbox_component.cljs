(ns ataru.virkailija.editor.components.checkbox-component
  (:require [ataru.cljs-util :as util]
            [re-frame.core :refer [subscribe dispatch]]))

(defn checkbox
  [path initial-content key]
  (let [id                (util/new-uuid)
        checked?          (-> initial-content key boolean)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-component-value (-> event .-target .-checked) path key]))}]
     [:label.editor-form__checkbox-label
      {:for   id}
      @(subscribe [:editor/virkailija-translation key])]]))
