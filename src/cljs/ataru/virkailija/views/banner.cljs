(ns ataru.virkailija.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]]
            [cljs.core.async.macros :refer [go]])
  (:require [ataru.virkailija.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs.core.async :as a :refer  [<! timeout]]
            [taoensso.timbre :refer-macros [spy debug]]
            [clojure.string :as string]))

(def logo
  [:div.logo
   [:a {:href "https://virkailija.opintopolku.fi/"}
    [:img {:src "/lomake-editori/images/opintopolku_large-fi.png"
           :height "40px"}]]])

(def panels
  {:editor      {:text "Lomakkeet" :href #(str "/lomake-editori/editor/" %)}
   :application {:text "Hakemukset" :href #(str "/lomake-editori/applications/" %)}})

(def right-labels {:form-edit "Lomakkeen muokkaus"
                   :view-applications "Hakemusten katselu"
                   :edit-applications "Hakemusten muokkaus"})

(def active-section-arrow [:span.active-section-arrow {:dangerouslySetInnerHTML {:__html "&#x2304;"}}])

(defn section-link [panel-kw]
  (let [active-panel     (subscribe [:active-panel])
        active?          (reaction (= @active-panel panel-kw))
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])]
    (fn []
      [:div.section-link {:class (name panel-kw)}
       (if @active?
         [:span.active-section
          active-section-arrow
          (-> panels panel-kw :text)]
         [:a {:on-click (partial routes/navigate-to-click-handler (str ((-> panels panel-kw :href ) @selected-form-key)))}
          (-> panels panel-kw :text)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     [:div.divider]
     [section-link :application]]))

(defn create-org-labels [organizations]
  (map
   (fn [org]
     (str (get-in org [:name :fi]) " (" (string/join ", " (map #(get right-labels (keyword %)) (:rights org))) ")"))
   organizations))

(defn profile []
  (let [user-info (subscribe [:state-query [:editor :user-info]])]
    (fn []
      (when @user-info
        (let [org-count      (count (:organizations @user-info))
              org-labels     (create-org-labels (:organizations @user-info))
              joint-orgs-str (string/join " \n" org-labels)
              org-str        (cond
                               (= 0 org-count) "Ei organisaatiota"
                               (< 1 org-count) "Useita organisaatioita"
                               :else           (get-in (first (:organizations @user-info)) [:name :fi]))]
          [:div.profile
           [:div
            [:p (:username @user-info)]
            [:p.tooltip-indicator {:title joint-orgs-str} org-str]]
           [:div.divider]
           [:div
            [:a {:href "/lomake-editori/auth/logout"} "Kirjaudu ulos"]]])))))

(defn status []
  (let [flash    (subscribe [:state-query [:flash]])
        loading? (subscribe [:state-query [:flash :loading?]])
        expired? (subscribe [:state-query [:flash :expired?]])]
    (fn []
      [:div
       (when @flash
         (match [@loading? @expired? @flash]
                [false _ {:error-type :concurrent-edit
                        :message message}]
                [:div.flasher.concurrent-edit-error.animated.flash
                 [:span message]]

                [false _ {:detail detailed-error
                        :message message}]
                [:div.flasher
                 [:span message]]

                [_ false {:message (message :guard some?)}]
                [:div.flasher
                 [:span message]]

                [_ true {:message (message :guard some?)}]
                [:div.flasher.animated.fadeOut
                 [:span message]]

                :else
                [:div]))])))

(defn top-banner []
  [:div.top-banner [:div.tabs logo [title]] [status] [profile]])
