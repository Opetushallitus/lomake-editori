(ns ataru.virkailija.virkailija-system
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [ataru.aws.auth :as aws-auth]
            [ataru.aws.sns :as sns]
            [ataru.aws.sqs :as sqs]
            [ataru.http.server :as server]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]]
            [ataru.background-job.job :as job]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [taoensso.timbre :as log])
  (:import java.time.Duration))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8350"))
     (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (apply
     component/system-map

     :organization-service (component/using
                             (organization-service/new-organization-service)
                             [:cache-service])

     :cache-service (component/using {} (mapv (comp keyword :name) caches))

     :virkailija-tarjonta-service (component/using
                                    (tarjonta-service/new-virkailija-tarjonta-service)
                                    [:organization-service :cache-service])

     :tarjonta-service (component/using
                         (tarjonta-service/new-tarjonta-service)
                         [:cache-service])

     :ohjausparametrit-service (component/using
                                 (ohjausparametrit-service/new-ohjausparametrit-service)
                                 [:cache-service])

     :kayttooikeus-service (if (-> config :dev :fake-dependencies)
                             (kayttooikeus-service/->FakeKayttooikeusService)
                             (kayttooikeus-service/->HttpKayttooikeusService nil))

     :person-service (component/using
                       (person-service/new-person-service)
                       [:cache-service])

     :handler (component/using
                (virkailija-routes/new-handler)
                [:organization-service
                 :virkailija-tarjonta-service
                 :tarjonta-service
                 :job-runner
                 :ohjausparametrit-service
                 :cache-service
                 :person-service
                 :kayttooikeus-service])

     :server-setup {:port      http-port
                    :repl-port repl-port}

     :server (component/using
               (server/new-server)
               [:server-setup :handler])

     :job-runner (job/new-job-runner virkailija-jobs/job-definitions)

     :credentials-provider (aws-auth/map->CredentialsProvider {})

     :amazon-sqs (component/using
                  (sqs/map->AmazonSQS {})
                  [:credentials-provider])

     :sns-message-manager (sns/map->SNSMessageManager {})

     :update-person-info-worker (component/using
                                 (person-integration/map->UpdatePersonInfoWorker
                                  {:enabled?      (:enabled? (:henkilo-modified-queue (:aws config)))
                                   :drain-failed? (:drain-failed? (:henkilo-modified-queue (:aws config)))
                                   :queue-url     (:queue-url (:henkilo-modified-queue (:aws config)))
                                   :receive-wait  (Duration/ofSeconds 20)})
                                 [:amazon-sqs
                                  :person-service
                                  :sns-message-manager])

     :redis (redis/map->Redis {})

     (mapcat (fn [cache]
               [(keyword (:name cache)) (component/using cache [:redis])])
             caches))))
