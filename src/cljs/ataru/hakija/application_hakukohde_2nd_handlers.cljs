(ns ataru.hakija.application-hakukohde-2nd-handlers
  (:require [clojure.string :as string]
            [re-frame.core :refer [subscribe reg-event-db reg-fx reg-event-fx dispatch]]
            [ataru.hakija.application-hakukohde-handlers :refer [query-hakukohteet hakukohteet-field]]
            [ataru.hakija.application-handlers :refer [set-field-visibilities
                                                       set-validator-processing
                                                       check-schema-interceptor]]))





(reg-event-db
  :application/set-active-hakukohde-search
  (fn [db [_ active-search-idx]]
    (assoc-in db [:application :active-hakukohde-search] active-search-idx)))

