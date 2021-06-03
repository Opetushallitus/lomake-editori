(ns ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu_service_spec
  (:require [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hk-service]
            [speclj.core :refer [describe it should=]]
            [clj-http.client :as http]
            [ataru.cas.client :as cas]))


(def fake-hakukohderyhmapalvelu-response
  {:status 200 :body "[\"1.2.246.562.28.12341\",\"1.2.246.562.28.12342\"]"})

(def expected-result
  ["1.2.246.562.28.12341" "1.2.246.562.28.12342"])

(describe "HakukohderyhmapalveluService"
          (it "Return list of hakukohderyhmas for hakukohde"
              (with-redefs [http/request (constantly fake-hakukohderyhmapalvelu-response)]
                (let [cas-client (cas/new-cas-client "ataru-test")
                      cas (cas/map->CasClientState {:client              cas-client
                                                    :params              nil
                                                    :session-cookie-name "ring-session"
                                                    :session-id          (atom "fake-session")})
                      hk-service-instance (hk-service/->IntegratedHakukohderyhmapalveluService cas)
                      result (hk-service/get-hakukohderyhma-oids-for-hakukohde hk-service-instance "1.2.3.4.5.6")]
                  (should= expected-result result)))))
