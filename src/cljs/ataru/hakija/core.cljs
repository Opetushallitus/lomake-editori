(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [lomake-editori.handlers]
            [lomake-editori.subs]
            [lomake-editori.editor.handlers]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.hakija.form-view :refer [form-view]]
            [ataru.hakija.application-handlers]
            [clojure.string :as str]))

(enable-console-print!)

(defn- form-id-from-url []
  (last (str/split (-> js/window .-location .-pathname) #"/")))

(defn mount-root []
  (reagent/render [form-view]
                  (.getElementById js/document "app")))


(defn ^:export init []
  (mount-root)
  (re-frame/dispatch [:application/get-form (form-id-from-url)]))
