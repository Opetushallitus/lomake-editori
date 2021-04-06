(ns ataru.organization-service.organization-client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [clojure.string :as s]
            [taoensso.timbre :as log]))

(def oph-organization "1.2.246.562.10.00000000001")

(defn- read-body
  [resp]
  (-> resp :body (json/parse-string true)))

(defn- org-node->map [org-node] {:name (:nimi org-node)
                                 :oid  (:oid org-node)
                                 :type :organization})

(defn- group->map [group] {:name            (:nimi group)
                           :oid             (:oid group)
                           :type            :group
                           :hakukohderyhma? (-> (set (:ryhmatyypit group))
                                                (some ["hakukohde" "hakukohderyhma"])
                                                string?)
                           :active?         (= "AKTIIVINEN" (:status group))})

(defn get-all-organizations-as-seq
  "Flattens hierarchy and includes all suborganizations"
  [hierarchy]
  (letfn [(recur-orgs [org-node]
            (if (< 0 (count (:children org-node)))
              (into [(org-node->map org-node)]
                    (map #(recur-orgs %) (:children org-node)))
              (org-node->map org-node)))]
    (flatten (map #(recur-orgs %) (:organisaatiot hierarchy)))))

(defn- get-organization-from-remote-service [organization-oid]
  (let [response (http-util/do-get (resolve-url :organisaatio-service.name organization-oid))]
    (if (= 200 (:status response))
      (let [parsed-response (read-body response)
            org-count       (:numHits parsed-response)]
        (cond
          (< 1 org-count)
          ; Should not happen ever, but let's make the failure very explicit and do some moves if it actually occurs:
          (throw (Exception. (str "Got wrong number of organizations for unique oid query " org-count)))

          (= 0 org-count)
          nil

          :else
          (org-node->map (first (:organisaatiot parsed-response)))))
      (throw (Exception. (str "Got status code " (:status response) " While reading single organization"))))))

(defn get-organization [organization-oid]
  (if (= organization-oid oph-organization)
    ;; the remote organization service  (organisaatiopalvelu) doesn't support
    ;; fetching data about the root OPH organization, so we'll hard-code it here:
    {:oid oph-organization :name {:fi "OPH"} :type :organization}
    (get-organization-from-remote-service organization-oid)))

(defn get-organizations
  "Returns a sequence of {:name <org-name> :oid <org-oid>} maps containing all suborganizations
   The root organization is the first element"
  [root-organization-oid]
  (let [url      (if (s/blank? root-organization-oid)
                   (resolve-url :organisaatio-service.root-hierarchy)
                   (resolve-url :organisaatio-service.plain-hierarchy root-organization-oid))
        response (http-util/do-get url)]
    (if (= 200 (:status response))
      (-> response read-body get-all-organizations-as-seq)
      (throw (Exception. (str "Got status code " (:status response) " While reading organizations"))))))

(defn get-groups
  "returns a sequence of {:name <group-name> :oid <group-oid>} maps containing all the
   groups within organization service"
  []
  (let [response (http-util/do-get (resolve-url :organisaatio-service.groups))]
    (if (= 200 (:status response))
      (->> response
           read-body
           (reduce #(assoc %1 (:oid %2) (group->map %2)) {}))
      (throw (Exception. (str "Got status code " (:status response) " While reading groups"))))))

(defn get-organization-by-oid-or-number
  "Get organization by oid or number."
  [oid-or-number]
  (let [url (resolve-url :organisaatio-service.get-by-oid oid-or-number)
        {:keys [status body]} (http-util/do-get url)]
    (if (= 200 status)
      (json/parse-string body true)
      (log/error (str "Couldn't fetch organization by number from url: " url)))))

(defn fake-hakukohderyhma [index]
  (group->map {:oid         (format "1.2.246.562.28.0000000000%d" index)
               :nimi        {:fi (format "Testihakukohderyhma %d" index)}
               :ryhmatyypit ["hakukohde"]}))
