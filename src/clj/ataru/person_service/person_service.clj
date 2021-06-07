(ns ataru.person-service.person-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.person-service.birth-date-converter :as bd-converter]
            [ataru.person-service.oppijanumerorekisteri-person-extract :as orpe]
            [ataru.person-service.person-client :as person-client]
            [ataru.util :as util]
            [clojure.string :as cs]
            [com.stuartsierra.component :as component]
            [ataru.date :as date]))

(defprotocol PersonService
  (create-or-find-person [this person]
    "Create or find a person in Oppijanumerorekisteri.")

  (get-persons [this oids]
    "Find multiple persons from Oppijanumerorekisteri.")

  (get-person [this oid]
    "Find a person from ONR.")

  (linked-oids [this oids]))

(defn- person-info-from-application [application]
  (let [answers (util/answers-by-key (:answers application))
        birth-date (-> answers :birth-date :value)]
    (merge {:first-name     (-> answers :first-name :value)
            :preferred-name (-> answers :preferred-name :value)
            :last-name      (-> answers :last-name :value)
            :birth-date     birth-date
            :nationality    (-> answers :nationality :value)}
           (when-not (cs/blank? birth-date)
             (let [minor (date/minor? birth-date)]
               (when (boolean? minor)
                 {:minor (date/minor? birth-date)})))
           (when-not (cs/blank? (-> answers :ssn :value))
             {:ssn (-> answers :ssn :value)})
           (when-not (cs/blank? (-> answers :gender :value))
             {:gender (-> answers :gender :value)})
           (when-not (cs/blank? (-> answers :language :value))
             {:language (-> answers :language :value)}))))

(defn- parse-onr-aidinkieli [person]
  (some-> person :aidinkieli :kieliKoodi cs/upper-case))

(defn- person-info-from-onr-person [person]
  (merge {:first-name     (:etunimet person)
          :preferred-name (:kutsumanimi person)
          :last-name      (:sukunimi person)
          :nationality    (->> (-> person :kansalaisuus)
                               (mapv #(vector (get % :kansalaisuusKoodi "999"))))}
         (let [birth-date (:syntymaaika person)]
           (when-not (cs/blank? birth-date)
             (let [finnish-birth-date (bd-converter/convert-to-finnish-format birth-date)
                   minor (date/minor? finnish-birth-date)]
               (cond-> {:birth-date finnish-birth-date}
                       (boolean? minor) (assoc :minor minor)))))
         (when-not (cs/blank? (:hetu person))
           {:ssn (:hetu person)})
         (when-not (cs/blank? (-> person :sukupuoli))
           {:gender (-> person :sukupuoli)})
         (let [aidinkieli (parse-onr-aidinkieli person)]
           (when-not (cs/blank? aidinkieli)
             {:language aidinkieli}))))

(defn parse-person [application person-from-onr]
  (let [yksiloity   (or (-> person-from-onr :yksiloity)
                        (-> person-from-onr :yksiloityVTJ))
        person-info (if yksiloity
                      (person-info-from-onr-person person-from-onr)
                      (person-info-from-application application))]
    (merge person-info
           (when (some? (:person-oid application))
             {:oid         (:person-oid application)
              :turvakielto (-> person-from-onr :turvakielto boolean)
              :yksiloity   (boolean yksiloity)}))))

(defrecord IntegratedPersonService [henkilo-cache
                                    oppijanumerorekisteri-cas-client]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  PersonService

  (create-or-find-person [_ application]
    (person-client/create-or-find-person
     oppijanumerorekisteri-cas-client
     (orpe/extract-person-from-application application)))

  (get-persons [_ oids] (cache/get-many-from henkilo-cache oids))

  (get-person [_ oid] (cache/get-from henkilo-cache oid))

  (linked-oids [_ oids]
    (person-client/linked-oids oppijanumerorekisteri-cas-client oids)))

(def fake-person-from-creation {:personOid    "1.2.3.4.5.6"
                  :firstName    "Foo"
                  :lastName     "Bar"
                  :email        "foo.bar@mailinator.com"
                  :idpEntitys   []})

(def fake-onr-person {:oidHenkilo   "1.2.3.4.5.6"
                      :hetu         "020202A0202"
                      :etunimet     "Testi"
                      :kutsumanimi  "Testi"
                      :sukunimi     "Ihminen"
                      :syntymaaika  "1941-06-16"
                      :sukupuoli    "2"
                      :kansalaisuus [{:kansalaisuusKoodi "246"}]
                      :aidinkieli   {:id          "742310"
                                     :kieliKoodi  "fi"
                                     :kieliTyyppi "suomi"}
                      :turvakielto  false
                      :yksiloity    false
                      :yksiloityVTJ false})

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (create-or-find-person [this person] fake-person-from-creation)

  (get-persons [this oids]
    (reduce #(assoc %1 %2 (.get-person this %2))
            {}
            oids))

  (get-person [this oid]
    (condp = oid
      "2.2.2" (merge fake-onr-person
                     {:oidHenkilo "2.2.2"
                      :turvakielto true
                      :yksiloity   true
                      :etunimet    "Ari"
                      :kutsumanimi "Ari"
                      :sukunimi    "Vatanen"
                      :hetu         "141196-933S"})
      (merge fake-onr-person
             {:oidHenkilo oid})))

  (linked-oids [this oids]
    {}))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (map->IntegratedPersonService {})))
