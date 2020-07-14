(ns ataru.virkailija.application.application-list.virkailija-application-list-handlers
  (:require [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.db :as initial-db]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/toggle-filter
  (fn [db [_ filter-id state]]
    (update-in db [:application :filters-checkboxes filter-id state] not)))

(reg-event-fx
  :application/apply-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] (get-in db [:application :filters-checkboxes]))
                   (assoc-in [:application :ensisijaisesti?] (get-in db [:application :ensisijaisesti?-checkbox]))
                   (assoc-in [:application :rajaus-hakukohteella] (get-in db [:application :rajaus-hakukohteella-value]))
                   (assoc-in [:application :attachment-review-states] (get-in db [:application :attachment-review-states-value])))
     :dispatch [:application/reload-applications]}))

(reg-event-fx
  :application/remove-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] initial-db/default-filters)
                   (assoc-in [:application :filters-checkboxes] initial-db/default-filters)
                   (assoc-in [:application :ensisijaisesti?] false)
                   (assoc-in [:application :ensisijaisesti?-checkbox] false)
                   (assoc-in [:application :rajaus-hakukohteella] nil)
                   (assoc-in [:application :rajaus-hakukohteella-value] nil)
                   (assoc-in [:application :attachment-review-states] {})
                   (assoc-in [:application :attachment-review-states-value] {}))
     :dispatch [:application/reload-applications]}))

(defn- set-rajaus-hakukohteella
  [db hakukohde-oid]
  (cljs-util/update-url-with-query-params {:rajaus-hakukohteella hakukohde-oid})
  (assoc-in db [:application :rajaus-hakukohteella-value] hakukohde-oid))

(defn- set-ensisijaisesti
  [db ensisijaisesti?]
  (cljs-util/update-url-with-query-params {:ensisijaisesti ensisijaisesti?})
  (cond-> (assoc-in db [:application :ensisijaisesti?-checkbox] ensisijaisesti?)
          (not ensisijaisesti?)
          (set-rajaus-hakukohteella nil)))

(reg-event-db
  :application/set-ensisijaisesti
  (fn [db [_ ensisijaisesti?]] (set-ensisijaisesti db ensisijaisesti?)))

(reg-event-db
  :application/set-rajaus-hakukohteella
  (fn [db [_ hakukohde-oid]] (set-rajaus-hakukohteella db hakukohde-oid)))

(defn undo-filters
  [db]
  (-> db
      (assoc-in [:application :filters-checkboxes] (get-in db [:application :filters]))
      (set-ensisijaisesti (get-in db [:application :ensisijaisesti?]))
      (set-rajaus-hakukohteella (get-in db [:application :rajaus-hakukohteella]))
      (assoc-in [:application :attachment-review-states-value] (get-in db [:application :attachment-review-states]))))

(reg-event-db
  :application/undo-filters
  (fn [db _] (undo-filters db)))

(reg-event-db
  :application/toggle-shown-time-column
  (fn [db _]
    (update-in db [:application :selected-time-column] #(if (= "created-time" %)
                                                          "submitted"
                                                          "created-time"))))

(reg-event-fx
  :application/update-sort
  (fn [{:keys [db]} [_ column-id]]
    {:db       (update-in db [:application :sort]
                          #(if (= column-id (:order-by %))
                             (update % :order {"desc" "asc" "asc" "desc"})
                             (assoc % :order-by column-id)))
     :dispatch [:application/reload-applications]}))

(reg-event-db
  :application/remove-filter-attachment
  (fn [db [_ field-id]]
    (update-in db [:application :attachment-review-states-value] dissoc field-id)))

(reg-event-db
  :application/set-filter-attachment-state
  (fn [db [_ field-id state value]]
    (assoc-in db [:application :attachment-review-states-value field-id state] value)))