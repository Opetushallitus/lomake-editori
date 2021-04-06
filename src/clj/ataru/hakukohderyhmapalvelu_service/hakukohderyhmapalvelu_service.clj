(ns ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service
  (:require [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-client :as client]
            [ataru.config.core :refer [config]]))

(defprotocol HakukohderyhmapalveluServiceProtocol
  (get-hakukohderyhma-oids-for-hakukohde [this hakukohde-oid]
    "Gets all oids of hakukohderyhmas hakukohde belongs to"))

(defrecord IntegratedHakukohderyhmapalveluService [hakukohderyhmapalvelu-cas-client]
  HakukohderyhmapalveluServiceProtocol

  (get-hakukohderyhma-oids-for-hakukohde [_ hakukohde-oid]
    (client/get-hakukohderyhma-oids-for-hakukohde-oid hakukohde-oid hakukohderyhmapalvelu-cas-client)))

(defrecord FakeHakukohderyhmapalveluService []
  HakukohderyhmapalveluServiceProtocol

  (get-hakukohderyhma-oids-for-hakukohde [_ _]
    ["1.2.246.562.28.12345" "1.2.246.562.28.12346"]))

(defn new-hakukohderyhmapalvelu-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeHakukohderyhmapalveluService)
    (->IntegratedHakukohderyhmapalveluService nil)))
