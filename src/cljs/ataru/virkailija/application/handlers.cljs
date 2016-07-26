(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(register-handler
  :application/select-application
  (fn [db [_ application-id]]
    (dispatch [:application/fetch-application application-id])
    (assoc-in db [:application :selected-id] application-id)))

(register-handler
  :application/fetch-applications
  (fn [db [_ form-id]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/list?formId=" form-id)
      (fn [db aplications-response]
        (assoc-in db [:application :applications] (:applications aplications-response))))
    db))

(defn answers-indexed
  "Convert the rest api version of application to a version which application
  readonly-rendering can use (answers are indexed with key in a map)"
  [application]
  (let [answers    (:answers application)
        answer-map (into {} (map (fn [answer] [(keyword (:key answer)) answer])) answers)]
    (assoc application :answers answer-map)))

(register-handler
  :application/fetch-application
  (fn [db [_ application-id]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/" application-id)
      (fn [db application]
        (assoc-in db [:application :selected-application] (answers-indexed application))))
    db))
