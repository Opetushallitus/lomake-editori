(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.aws.auth :as aws-auth]
            [ataru.cas.client :as cas]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.background-job.job :as job]
            [ataru.aws.sqs :as sqs]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.http.server :as server]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-client :as person-client]
            [environ.core :refer [env]]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [ataru.config.core :refer [config]]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.temp-file-storage.s3-client :as s3-client]
            [ataru.temp-file-storage.filesystem-temp-file-store :as filesystem-temp-file-store]
            [ataru.temp-file-storage.s3-temp-file-store :as s3-temp-file-store]
            [ataru.valinta-tulos-service.service :as valinta-tulos-service]
            [ataru.applications.application-service :as application-service]
            [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hakukohderyhma-service]))

(defn new-system
  ([audit-logger]
   (new-system
     audit-logger
    (Integer/parseInt (get env :ataru-http-port "8351"))
    (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([audit-logger http-port repl-port]
   (apply
    component/system-map

    :audit-logger audit-logger

    :hakukohderyhmapalvelu-cas-client (cas/new-client "/hakukohderyhmapalvelu"
                                                      "auth/cas"
                                                      "ring-session"
                                                      (-> config :public-config :virkailija-caller-id))

    :hakukohderyhmapalvelu-service (component/using
                                     (hakukohderyhma-service/new-hakukohderyhmapalvelu-service)
                                     [:hakukohderyhmapalvelu-cas-client])

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

    :ohjausparametrit-service (component/using
                               (ohjausparametrit-service/new-ohjausparametrit-service)
                               [:ohjausparametrit-cache])

    :oppijanumerorekisteri-cas-client (cas/new-client "/oppijanumerorekisteri-service" "j_spring_cas_security_check"
                                                      "JSESSIONID" (-> config :public-config :hakija-caller-id))

    :liiteri-cas-client (cas/new-client "/liiteri" "/liiteri/auth/cas"
                                        "ring-session" (-> config :public-config :hakija-caller-id))

    :credentials-provider (aws-auth/map->CredentialsProvider {})

    :amazon-sqs (component/using
                 (sqs/map->AmazonSQS {})
                 [:credentials-provider])

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

    :suoritusrekisteri-cas-client (cas/new-client "/suoritusrekisteri" "j_spring_cas_security_check"
                                                  "JSESSIONID" (-> config :public-config :hakija-caller-id))

    :suoritus-service (component/using
                       (suoritus-service/new-suoritus-service)
                       [:suoritusrekisteri-cas-client])

    :s3-client (component/using
                (s3-client/new-client)
                [:credentials-provider])

    :temp-file-store (if (get-in config [:aws :temp-files])
                       (component/using
                        (s3-temp-file-store/new-store)
                        [:s3-client])
                       (filesystem-temp-file-store/new-store))

    :valinta-tulos-service-cas-client (cas/new-client "/valinta-tulos-service" "auth/login"
                                                      "session" (-> config :public-config :hakija-caller-id))

    :valinta-tulos-service (component/using
                            (valinta-tulos-service/map->RemoteValintaTulosService {})
                            {:cas-client :valinta-tulos-service-cas-client})

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

    :handler (component/using
               (handler/new-handler)
               (into [:tarjonta-service
                      :job-runner
                      :liiteri-cas-client
                      :organization-service
                      :ohjausparametrit-service
                      :person-service
                      :temp-file-store
                      :amazon-sqs
                      :application-service
                      :audit-logger]
                     (map first caches)))

    :server-setup {:port      http-port
                   :repl-port repl-port}

    :server (component/using
             (server/new-server)
             [:server-setup :handler])

    :job-runner (component/using
                 (job/new-job-runner hakija-jobs/job-definitions)
                 [:form-by-id-cache
                  :ohjausparametrit-service
                  :henkilo-cache
                  :koodisto-cache
                  :person-service
                  :tarjonta-service
                  :suoritus-service
                  :audit-logger
                  :liiteri-cas-client])

    :redis (redis/map->Redis {})

    (mapcat identity caches))))
