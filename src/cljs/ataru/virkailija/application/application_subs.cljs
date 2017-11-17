(ns ataru.virkailija.application.application-subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as re-frame]
            [ataru.util :as u]))

(defn- from-multi-lang [text]
  (some #(get text %) [:fi :sv :en]))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku       (get-in db [:application :selected-haku])
         selected-hakukohde  (get-in db [:application :selected-hakukohde])
         selected-form-key   (get-in db [:application :selected-form-key])
         forms               (get-in db [:application :forms])
         applications        (get-in db [:application :applications])]
     (or (:name (get forms selected-form-key))
         (from-multi-lang (:name selected-hakukohde))
         (from-multi-lang (:name selected-haku))
         (if (sequential? applications) (str "Löytyi " (count applications) " hakemusta"))))))

(re-frame/reg-sub
  :application/application-list-selected-by
  (fn [db]
    (let [db-application (:application db)]
      (cond
        (:selected-form-key db-application) :selected-form-key
        (:selected-haku db-application) :selected-haku
        (:selected-hakukohde db-application) :selected-hakukohde))))

(re-frame/reg-sub
 :application/application-list-belongs-to-haku?
 (fn [db]
   (boolean
    (or
     (get-in db [:application :selected-haku])
     (get-in db [:application :selected-hakukohde])
     (get-in db [:application :selected-form-key])))))

(defn filter-haku-seq [haku-seq incomplete-eq]
  (filter #(incomplete-eq (:incomplete %) 0) haku-seq))

(defn filter-haut [haut incomplete-eq]
  (-> haut
      (assoc :direct-form-haut (filter-haku-seq (:direct-form-haut haut) incomplete-eq))
      (assoc :tarjonta-haut (filter-haku-seq (:tarjonta-haut haut) incomplete-eq))))

(defn sort-haku-seq-by-unprocessed [haku-seq]
  (sort-by :unprocessed #(compare %2 %1) haku-seq))

(defn sort-haku-seq-by-name [haku-seq]
  (sort-by (fn [haku]
             (if (string? (:name haku))
               (:name haku)
               (from-multi-lang (:name haku))))
           #(compare (clojure.string/lower-case %1) (clojure.string/lower-case %2))
           haku-seq))

(defn sort-hakukohteet [tarjonta-haut sort-haku-seq-fn]
  (map #(update % :hakukohteet sort-haku-seq-fn) tarjonta-haut))

(defn sort-haut [haut sort-haku-seq-fn]
  (-> haut
      (assoc :direct-form-haut (sort-haku-seq-fn (:direct-form-haut haut)))
      (assoc :tarjonta-haut (->
                             (:tarjonta-haut haut)
                             sort-haku-seq-fn
                             (sort-hakukohteet sort-haku-seq-fn)))))

(defn when-haut [db handle-haut-fn]
  (when-let [haut (get-in db [:application :haut])]
     (handle-haut-fn haut)))

(defn count-haut [haut]
  (+ (count (:tarjonta-haut haut)) (count (:direct-form-haut haut))))

(re-frame/reg-sub
 :application/incomplete-haut
 (fn [db]
   (when-haut
       db
       #(-> %
            (filter-haut >)
            (sort-haut sort-haku-seq-by-unprocessed)))))

(re-frame/reg-sub
 :application/incomplete-haku-count
 (fn [db]
   (when-haut
       db
       #(-> %
            (filter-haut >)
            count-haut))))

(re-frame/reg-sub
 :application/complete-haut
 (fn [db]
   (when-haut
       db
       #(->
         %
         (filter-haut =)
         (sort-haut sort-haku-seq-by-name)))))

(re-frame/reg-sub
 :application/complete-haku-count
 (fn [db]
   (when-haut
       db
       #(-> %
            (filter-haut =)
            count-haut))))

(re-frame/reg-sub
 :application/search-control-all-page-view?
 (fn [db]
   (let [show-search-control (get-in db [:application :search-control :show])]
     (boolean (some #{show-search-control} [:complete :incomplete])))))

(re-frame/reg-sub
  :application/get-i18n-text
  (fn [db [_ translations]]
    (get translations (keyword (get-in db [:application
                                           :selected-application-and-form
                                           :application
                                           :lang]
                                       "fi")))))

(re-frame/reg-sub
  :application/hakukohteet-field
  (fn [db _]
    (first
     (filter #(= "hakukohteet" (:id %))
             (get-in db [:application
                         :selected-application-and-form
                         :form
                         :content])))))

(re-frame/reg-sub
  :application/hakukohde-options-by-oid
  (fn [db _]
    (->> @(re-frame/subscribe [:application/hakukohteet-field])
         :options
         (map (juxt :value identity))
         (into {}))))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [db _]
    @(re-frame/subscribe [:application/get-i18n-text
                          (:label @(re-frame/subscribe [:application/hakukohteet-field]))])))
(re-frame/reg-sub
  :application/hakukohde-label
  (fn [db [_ hakukohde-oid]]
    @(re-frame/subscribe [:application/get-i18n-text
                          (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
                                  [hakukohde-oid :label])])))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [db [_ hakukohde-oid]]
    @(re-frame/subscribe [:application/get-i18n-text
                          (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
                                  [hakukohde-oid :description])])))

(re-frame/reg-sub
  :application/hakukohteet
  (fn [db _]
    (get-in db [:application
                :selected-application-and-form
                :application
                :answers
                :hakukohteet
                :value])))

(re-frame/reg-sub
  :application/information-request-submit-enabled?
  (fn [db _]
    (let [request-state (-> db :application :information-request :state)]
      (and (-> db :application :information-request :subject u/not-blank?)
           (-> db :application :information-request :message u/not-blank?)
           (nil? request-state)))))

(defn- event-and-information-request-comparator [a b]
  (let [time-a (or (:time a) (:created-time a))
        time-b (or (:time b) (:created-time b))]
    (if (t/before? time-a time-b)
      -1
      1)))

(re-frame/reg-sub
  :application/events-and-information-requests
  (fn [db _]
    (->> (concat (-> db :application :events)
                 (-> db :application :information-requests))
         (sort event-and-information-request-comparator))))

(defn- show-email-icon-for-application? [application]
  (and (-> application :new-application-modifications (> 0))
       (-> application :state (= "information-request"))))

(re-frame/reg-sub
  :application/show-state-email-icon?
  (fn [db [_ application-key]]
    (->> db
         :application
         :applications
         (filter (comp (partial = application-key) :key))
         (first)
         (show-email-icon-for-application?))))

(re-frame/reg-sub
  :application/resend-modify-application-link-enabled?
  (fn [db _]
    (-> db :application :modify-application-link :state nil?)))

(re-frame/reg-sub
  :application/filtered-applications
  (fn [db _]
    (let [applications                (-> db :application :applications)
          states-to-include           (-> db :application :filter set)
          selection-states-to-include (-> db :application :selection-filter set)]
      (filter
       (fn [application]
         (let [selection-states (->> (:application-hakukohde-reviews application)
                                     (filter #(= "selection-state" (:requirement %)))
                                     (map :state))]
           (and
            (contains? states-to-include (:state application))
            (or
             (not (empty? (clojure.set/intersection
                           selection-states-to-include
                           (set selection-states))))
             (and
              (contains? selection-states-to-include "incomplete")
              (or
               (empty? selection-states)
               (< (count selection-states)
                  (count (:hakukohde application)))))))))
       applications))))

(re-frame/reg-sub
  :application/review-state-setting-enabled?
  (fn [db [_ setting-kwd]]
    (if-some [enabled-in-state? (-> db :application :review-settings :config setting-kwd)]
      enabled-in-state?
      true)))

(re-frame/reg-sub
  :application/review-state-setting-disabled?
  (fn [db [_ setting-kwd]]
    (-> db :application :review-settings :config setting-kwd (= :updating))))

(re-frame/reg-sub
  :application/review-list-visible?
  (fn [db [_ list-kwd]]
    (-> db :application :ui/review list-kwd)))
