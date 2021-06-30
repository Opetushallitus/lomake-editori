(ns ataru.virkailija.question-search.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :question-search/data
  (fn [db [_ form-key id]]
    (get-in db [:question-search form-key id])))

(re-frame/reg-sub
  :question-search/search-input
  (fn [[_ form-key id] _]
    (re-frame/subscribe [:question-search/data form-key id]))
  (fn [data _]
    (:search-input data "")))

(re-frame/reg-sub
  :question-search/searching?
  (fn [[_ form-key id] _]
    (re-frame/subscribe [:question-search/data form-key id]))
  (fn [data _]
    (get data :searching? false)))

(re-frame/reg-sub
  :question-search/search-result
  (fn [[_ form-key id] _]
    (re-frame/subscribe [:question-search/data form-key id]))
  (fn [data _]
    (mapv (comp keyword :id) (remove :per-hakukohde (:search-result data)))))
