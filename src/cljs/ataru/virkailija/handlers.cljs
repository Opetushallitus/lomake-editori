(ns ataru.virkailija.handlers
    (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
              [ataru.virkailija.autosave :as autosave]
              [ataru.virkailija.editor.handlers :refer [clear-copy-component]]
              [ataru.virkailija.db :as db]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
  :set-state
  (fn [db [_ path args]]
    (assert (or (vector? path)
                (seq? path)))
    (if (map? args)
      (update-in db path merge args)
      (assoc-in db path args))))

(reg-event-db
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(reg-event-fx
  :state-update-fx
  (fn [cofx [_ f]]
    (or (f cofx)
      (dissoc cofx :event))))

(reg-event-db
  :set-active-panel
  (fn [db [_ active-panel]]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (cond-> db
            (get-in db [:editor :copy-component :copy-component-cut?] false)
            (clear-copy-component)
            true
            (assoc :active-panel active-panel))))

(reg-event-fx
  :flasher
  (fn [{:keys [db]} [_ flash]]
    (let [template-editor-visible? (get-in db [:editor :ui :template-editor-visible?])]
      (if (not template-editor-visible?)
        (-> {:db db}
            (assoc :delayed-dispatch
                   {:dispatch-vec [:state-update (fn [db]
                                                   (if (= flash (dissoc (:flash db) :expired?))
                                                     (update db :flash assoc :expired? true)))]
                    :timeout      16})
            (assoc-in [:db :flash] (assoc flash :expired? false)))))))

(reg-event-db
  :snackbar-message
  (fn [db [_ message]]
    (assoc db :snackbar-message message)))