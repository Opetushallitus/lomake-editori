(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :as re-frisk]
            [ataru.cljs-util :as cljs-util]
            [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.application-view :refer [form-view]]
            [ataru.hakija.application-handlers]               ;; required although no explicit dependency
            [ataru.hakija.application-hakukohde-handlers]     ;; required although no explicit dependency
            [ataru.hakija.application-hakukohde-2nd-handlers] ;; required although no explicit dependency
            [ataru.hakija.subs]                               ;; required although no explicit dependency
            [ataru.application-common.fx]                     ; ataru.application-common.fx must be required to have common fx handlers enabled
            [ataru.hakija.component-handlers.dropdown-component-handlers]
            [ataru.util :as u]
            [cemerick.url :as url]
            [goog.string :as gstring]
            [clojure.string :as string]
            [ataru.schema-validation :as schema-validation]))

(enable-console-print!)

(defn- path-match
  [path re]
  (when-let [re-match (re-matches re path)]
    (nth re-match 1)))

(defn- dispatch-form-load
  []
  (let [path              (cljs-util/get-path)
        hakukohde-oid     (path-match path #"/hakemus/hakukohde/(.+)/?")
        haku-oid          (path-match path #"/hakemus/haku/(.+)/?")
        form-key          (path-match path #"/hakemus/(.+)/?")
        query-params      (cljs-util/extract-query-params)
        hakija-secret     (:modify query-params)
        virkailija-secret (:virkailija-secret query-params)
        hakukohteet       (string/split (:hakukohteet query-params) #",")]
    (cljs-util/unset-query-param "modify")
    (cljs-util/unset-query-param "virkailija-secret")
    (cond
      (u/not-blank? hakukohde-oid)
      (re-frame/dispatch [:application/get-latest-form-by-hakukohde hakukohde-oid virkailija-secret])

      (u/not-blank? haku-oid)
      (re-frame/dispatch [:application/get-latest-form-by-haku haku-oid hakukohteet virkailija-secret])

      (u/not-blank? form-key)
      (re-frame/dispatch [:application/get-latest-form-by-key form-key virkailija-secret])

      (u/not-blank? hakija-secret)
      (re-frame/dispatch [:application/get-application-by-hakija-secret hakija-secret])

      (u/not-blank? virkailija-secret)
      (re-frame/dispatch [:application/get-application-by-virkailija-secret virkailija-secret]))))

(defn mount-root []
  (schema-validation/enable-schema-fn-validation)
  (re-frame/clear-subscription-cache!)
  (reagent/render [form-view]
                  (.getElementById js/document "app")))

(defn- re-frisk-environment?
  []
  (let [cfg (js->clj js/config)]
    (or (get cfg "enable-re-frisk")
        (= (get cfg "environment-name") "luokka"))))


(defn network-listener []
  (.addEventListener js/window "online" (fn [] (re-frame/dispatch [:application/network-online])))
  (.addEventListener js/window "offline" (fn [] (re-frame/dispatch [:application/network-offline]))))

(re-frame/reg-event-db
  :application/handle-client-error
  (fn [db _] db))

(defn ^:export init []
  (cljs-util/set-global-error-handler! #(ajax/http {:method    :post
                                                    :post-data %
                                                    :url       "/hakemus/api/client-error"
                                                    :handler   [:application/handle-client-error]})
                                       #(let [form @(re-frame/subscribe [:application/form])]
                                          (gstring/format "Hakija form=%s, id=%s, haku-oid=%s, lang=%s, application-key=%s"
                                                          (:key form)
                                                          (:id form)
                                                          (get-in form [:tarjonta :haku-oid])
                                                          (:selected-language form)
                                                          (-> (.. js/window -location -href)
                                                              (url/url)
                                                              (get-in [:query "application-key"])))))
  (network-listener)
  (mount-root)
  (re-frame/dispatch-sync [:application/initialize-db])
  (when (re-frisk-environment?)
    (re-frisk/enable-re-frisk!))
  (dispatch-form-load))
