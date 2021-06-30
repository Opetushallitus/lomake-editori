(ns ataru.hakija.components.hakukohde-details-component
  (:require [ataru.util :as util]
            [re-frame.core :as re-frame]
            [re-frame.core :as re-frame :refer [subscribe]]))

(defn hakukohde-details-component [_ _]
  (let [lang  @(re-frame/subscribe [:application/form-language])]
    (fn [field-descriptor]
      (let [hakukohde @(re-frame/subscribe [:application/get-hakukohde (:duplikoitu-kysymys-hakukohde-oid field-descriptor)])
            name          (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
            tarjoaja-name (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]
           [:span (str name " " tarjoaja-name)]))))