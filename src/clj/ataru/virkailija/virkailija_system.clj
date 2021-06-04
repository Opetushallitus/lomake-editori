(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.aws.auth :as aws-auth]
            [ataru.aws.sns :as sns]
            [ataru.aws.sqs :as sqs]
            [ataru.cas.client :as cas]
            [ataru.cache.redis-cache :as redis-cache]
            [ataru.cache.two-layer-cache :as two-layer-cache]
            [ataru.http.server :as server]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client :as koostepalvelu-client]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service :as koostepalvelu-service]
            [ataru.valintaperusteet.client :as valintaperusteet-client]
            [ataru.valintaperusteet.service :as valintaperusteet-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.valinta-tulos-service.service :as valinta-tulos-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]]
            [ataru.background-job.job :as job]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.applications.application-service :as application-service]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [ataru.db.db :as db])
  (:import java.time.Duration
           [java.util.concurrent TimeUnit]))

(defn new-system
  ([audit-logger]
   (new-system
     audit-logger
    (Integer/parseInt (get env :ataru-http-port "8350"))
    (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([audit-logger http-port repl-port]
   (apply
    component/system-map

    :audit-logger audit-logger

    :organization-service (component/using
                           (organization-service/new-organization-service)
                           [:organizations-hierarchy-cache
                            :all-organization-groups-cache])

    :tarjonta-service (component/using
                       (tarjonta-service/new-tarjonta-service)
                       [:organization-service
                        :forms-in-use-cache
                        :koulutus-cache
                        :kouta-hakus-by-form-key-cache
                        :hakukohde-cache
                        :haku-cache
                        :hakukohde-search-cache])

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-redis-cache
    (component/using
      (redis-cache/map->Cache
        {:name          "valintalaskentakoostepalvelu-hakukohde-valintalaskenta"
         :ttl           [3 TimeUnit/DAYS]
         :refresh-after [1 TimeUnit/DAYS]
         :lock-timeout  [10000 TimeUnit/MILLISECONDS]})
      {:redis  :redis
       :loader :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache-loader})

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
    (component/using
      (two-layer-cache/map->Cache
        {:name                "in-memory-valintalaskentakoostepalvelu-hakukohde-valintalaskenta"
         :size                200000
         :expire-after-access [3 TimeUnit/DAYS]
         :refresh-after       [1 TimeUnit/SECONDS]})
      {:redis-cache :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-redis-cache})

    :valinta-tulos-service-cas-client (cas/new-client "/valinta-tulos-service" "/auth/login"
                                                      "session" (-> config :public-config :virkailija-caller-id))

    :valinta-tulos-service (component/using
                            (valinta-tulos-service/map->RemoteValintaTulosService {})
                            {:cas-client :valinta-tulos-service-cas-client})

    :valintalaskentakoostepalvelu-cas-client (cas/new-client "/valintalaskentakoostepalvelu"
                                                             "j_spring_cas_security_check"
                                                             "JSESSIONID"
                                                             (-> config :public-config :virkailija-caller-id))

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache-loader (component/using
                                                                           (koostepalvelu-client/map->HakukohdeValintalaskentaCacheLoader {})
                                                                           [:valintalaskentakoostepalvelu-cas-client])

    :valintalaskentakoostepalvelu-service (component/using
                                            (koostepalvelu-service/new-valintalaskentakoostepalvelu-service)
                                            [:valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache])

    :valintaperusteet-cas-client (cas/new-client "/valintaperusteet-service" "j_spring_cas_security_check"
                                                 "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :valintatapajono-cache-loader (component/using
                                   (valintaperusteet-client/map->ValintatapajonoCacheLoader {})
                                   [:valintaperusteet-cas-client])

    :valintatapajono-redis-cache (component/using
                                  (redis-cache/map->Cache
                                   {:name          "valintatapajono"
                                    :ttl           [3 TimeUnit/DAYS]
                                    :refresh-after [1 TimeUnit/DAYS]
                                    :lock-timeout  [1 TimeUnit/SECONDS]})
                                  {:redis  :redis
                                   :loader :valintatapajono-cache-loader})

    :valintatapajono-cache (component/using
                            (two-layer-cache/map->Cache
                             {:name                "in-memory-valintatapajono"
                              :size                10000
                              :expire-after-access [3 TimeUnit/DAYS]
                              :refresh-after       [12 TimeUnit/HOURS]})
                            {:redis-cache :valintatapajono-redis-cache})

    :valintaperusteet-service (component/using
                               (valintaperusteet-service/map->CachedValintaperusteetService {})
                               [:valintatapajono-cache])

    :ohjausparametrit-service (component/using
                               (ohjausparametrit-service/new-ohjausparametrit-service)
                               [:ohjausparametrit-cache])

    :kayttooikeus-cas-client (cas/new-client "/kayttooikeus-service" "j_spring_cas_security_check"
                                             "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :kayttooikeus-service (if (-> config :dev :fake-dependencies)
                            (kayttooikeus-service/->FakeKayttooikeusService)
                            (component/using
                             (kayttooikeus-service/->HttpKayttooikeusService nil)
                             [:kayttooikeus-cas-client]))

    :oppijanumerorekisteri-cas-client (cas/new-client "/oppijanumerorekisteri-service" "j_spring_cas_security_check"
                                                      "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :henkilo-cache-loader (component/using
                           (person-client/map->PersonCacheLoader {})
                           [:oppijanumerorekisteri-cas-client])

    :form-by-haku-oid-str-cache-loader (component/using
                                        (hakija-form-service/map->FormByHakuOidStrCacheLoader {})
                                        [:form-by-id-cache
                                         :koodisto-cache
                                         :ohjausparametrit-service
                                         :organization-service
                                         :tarjonta-service])

    :person-service (component/using
                     (person-service/new-person-service)
                     [:henkilo-cache :oppijanumerorekisteri-cas-client])

    :login-cas-client (cas/new-cas-client "/cas/login" "/auth/cas" "ring-session" (-> config :public-config :virkailija-caller-id))

    :cas-logout (cas/cas-logout)

    :liiteri-cas-client (cas/new-client "/liiteri" "/auth/cas"
                                        "ring-session" (-> config :public-config :virkailija-caller-id))

    :application-service (component/using
                           (application-service/new-application-service)
                           [:liiteri-cas-client
                            :organization-service
                            :tarjonta-service
                            :ohjausparametrit-service
                            :audit-logger
                            :person-service
                            :valinta-tulos-service
                            :koodisto-cache
                            :job-runner])

    :session-store (create-session-store (db/get-datasource :db))

    :handler (component/using
              (virkailija-routes/new-handler)
              (vec (concat [:login-cas-client
                            :organization-service
                            :tarjonta-service
                            :liiteri-cas-client
                            :valintalaskentakoostepalvelu-service
                            :valintaperusteet-service
                            :valinta-tulos-service
                            :job-runner
                            :ohjausparametrit-service
                            :person-service
                            :kayttooikeus-service
                            :audit-logger
                            :application-service
                            :session-store
                            :cas-logout]
                           (map first caches))))

    :server-setup {:port      http-port
                   :repl-port repl-port}

    :server (component/using
             (server/new-server)
             [:server-setup :handler])

    :suoritusrekisteri-cas-client (cas/new-client "/suoritusrekisteri" "j_spring_cas_security_check"
                                                  "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :suoritus-service (component/using
                       (suoritus-service/new-suoritus-service)
                       [:suoritusrekisteri-cas-client])

    :job-runner (component/using
                 (job/new-job-runner (merge virkailija-jobs/job-definitions
                                            hakija-jobs/job-definitions))
                 [:form-by-id-cache
                  :ohjausparametrit-service
                  :henkilo-cache
                  :koodisto-cache
                  :person-service
                  :tarjonta-service
                  :suoritus-service
                  :audit-logger
                  :liiteri-cas-client])

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
                                 :job-runner
                                 :sns-message-manager])

    :redis (redis/map->Redis {})

    (mapcat identity caches))))
