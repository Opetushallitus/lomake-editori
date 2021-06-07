(ns ataru.email.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.field-deadline :as field-deadline]
            [ataru.background-job.email-job :as email-job]
            [ataru.background-job.job :as job]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.email.email-store :as email-store]
            [ataru.forms.form-store :as forms]
            [ataru.tarjonta-service.hakukohde :as hakukohde]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.translations.texts :refer [email-default-texts]]
            [ataru.util :as util]
            [ataru.date :as date]
            [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [clojure.set]
            [clojure.string]
            [markdown.core :as md]
            [medley.core :refer [find-first]]
            [selmer.parser :as selmer]
            [taoensso.timbre :as log])
  (:import [org.owasp.html HtmlPolicyBuilder ElementPolicy]))

(def languages #{:fi :sv :en})
(def languages-map {:fi nil :sv nil :en nil})

(def submit-email-subjects
  {:fi "Opintopolku: hakemuksesi on vastaanotettu"
   :sv "Studieinfo: Din ansökan har mottagits"
   :en "Studyinfo: Your application has been received"})

(def edit-email-subjects
  {:fi "Opintopolku: Muutokset hakemukseesi on tallennettu"
   :sv "Studieinfo: Dina ändringar har lagrats i din ansökan"
   :en "Studyinfo: The changes to your application have been saved"})

(def refresh-secret-email-subjects
  {:fi "Opintopolku: uusi hakemuslinkkisi"
   :sv "Studieinfo: ny länk till din ansökan"
   :en "Studyinfo: your new application link"})

(def from-address "no-reply@opintopolku.fi")

(defn- ->string-array
  [& elements]
  (into-array String elements))

(def add-style-to-links
  (proxy [ElementPolicy] []
    (apply [element-name attrs]
      (doto attrs
        (.add "target")
        (.add "_blank")
        (.add "style")
        (.add "color: #0093C4;"))
      element-name)))

(def html-policy
  (as-> (HtmlPolicyBuilder.) hpb
        (.allowElements hpb (->string-array "p" "span" "div" "h1" "h2" "h3" "h4" "h5" "ul" "ol" "li" "br" "strong" "em"))
        (.allowElements hpb add-style-to-links (->string-array "a"))
        (.allowUrlProtocols hpb (->string-array "http" "https"))
        (.onElements (.allowAttributes hpb (->string-array "href" "target")) (->string-array "a"))
        (.toFactory hpb)))

(defn- submit-email-template-filename
  [lang]
  (str "templates/email_submit_confirmation_template_" (name lang) ".html"))

(defn- modify-link [secret]
  (-> config
      (get-in [:public-config :applicant :service_url])
      (str "/hakemus?modify=" secret)))

(defn ->safe-html
  [content]
  (.sanitize html-policy (md/md-to-html-string content)))

(defn- hakukohde-names [tarjonta-info lang application]
  (when (:haku application)
    (let [tarjonta-hakukohteet (util/group-by-first :oid (:hakukohteet tarjonta-info))
          hakukohteet          (keep #(get tarjonta-hakukohteet %) (:hakukohde application))]
      (when-let [missing-oids (seq (clojure.set/difference
                                    (set (:hakukohde application))
                                    (set (map :oid hakukohteet))))]
        (throw (new RuntimeException
                    (str "Hakukohteet " (clojure.string/join ", " missing-oids)
                         " not found"))))
      (map-indexed (fn [i {:keys [name tarjoaja-name]}]
                     (str (when (:prioritize-hakukohteet tarjonta-info)
                            (str (inc i) ". "))
                          (util/non-blank-val name [lang :fi :sv :en])
                          " - "
                          (util/non-blank-val tarjoaja-name [lang :fi :sv :en])))
                   hakukohteet))))

(defn- add-blank-templates [templates]
  (as-> templates x
        (util/group-by-first (comp keyword :lang) x)
        (merge languages-map x)
        (map (fn [el]
                 (let [lang     (first el)
                       template (second el)]
                   {:lang           (name lang)
                    :subject        (get template :subject (get-in email-default-texts [:email-submit-confirmation-template :submit-email-subjects lang]))
                    :content        (get template :content "")
                    :content-ending (get template :content_ending (get-in email-default-texts [:email-submit-confirmation-template :without-application-period lang]))
                    :signature      (get template :signature (get-in email-default-texts [:email-submit-confirmation-template :signature lang]))}))
             x)))


(defn preview-submit-email
  [lang subject content content-ending signature]
  {:from           from-address
   :subject        subject
   :content        content
   :content-ending content-ending
   :lang           lang
   :signature      signature
   :body           (-> (submit-email-template-filename lang)
                       (selmer/render-file {:lang            lang
                                            :hakukohteet     ["Hakukohde 1" "Hakukohde 2" "Hakukohde 3"]
                                            :attachments-without-answer [{:label "Liite 1"
                                                                          :deadline (get (hakuaika/date-time->localized-date-time (t/date-time 2000 12 31)) (keyword lang))}
                                                                         {:label "Liite 2"
                                                                          :deadline ""}
                                                                         {:label "Liite 3"
                                                                          :deadline ""}]
                                            :application-url "https://opintopolku.fi/hakemus/01234567890abcdefghijklmn"
                                            :application-oid "1.2.246.562.11.00000000000000000000"
                                            :content         (->safe-html content)
                                            :content-ending  (->safe-html content-ending)
                                            :signature       (->safe-html signature)}))})

(defn get-email-templates
  [form-key]
  (as-> (email-store/get-email-templates form-key) x
        (add-blank-templates x)
        (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content-ending %) (:signature %)) x)))

(defn- attachment-with-deadline [_ lang field]
  (let [attachment {:label (-> field
                               :label
                               (util/non-blank-val [lang :fi :sv :en]))}]
    (assoc attachment :deadline (-> field :params :deadline-label lang))))

(defn- create-email ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service subject template-name application-id]
                     (create-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service subject template-name application-id false))
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service subject template-name application-id guardian?]
   (let [now                             (t/now)
         application                     (application-store/get-application application-id)
         tarjonta-info                   (:tarjonta
                                           (tarjonta-parser/parse-tarjonta-info-by-haku
                                             koodisto-cache
                                             tarjonta-service
                                             organization-service
                                             ohjausparametrit-service
                                             (:haku application)
                                             (:hakukohde application)))
         answers-by-key                  (-> application :answers util/answers-by-key)
         hakuajat                        (hakuaika/index-hakuajat (:hakukohteet tarjonta-info))
         field-deadlines                 (->> (:key application)
                                              field-deadline/get-field-deadlines
                                              (map #(dissoc % :last-modified))
                                              (util/group-by-first :field-id))
         form                            (-> (forms/fetch-by-id (:form application))
                                             (hakukohde/populate-attachment-deadlines now hakuajat field-deadlines))
         lang                            (keyword (:lang application))
         attachment-keys-without-answers (->> (application-store/get-application-attachment-reviews (:key application))
                                              (map :attachment-key)
                                              (filter #(not (contains? answers-by-key (keyword %))))
                                              set)
         attachments-without-answer      (->> form
                                              :content
                                              util/flatten-form-fields
                                              (filter #(contains? attachment-keys-without-answers (:id %)))
                                              (map #(attachment-with-deadline application lang %)))
         email-template                  (find-first #(= (:lang application) (:lang %)) (get-email-templates (:key form)))
         content                         (-> email-template
                                             :content
                                             (->safe-html))
         content-ending                  (-> email-template
                                             :content-ending
                                             (->safe-html))
         signature                       (-> email-template
                                             :signature
                                             (->safe-html))
         minor?                          (date/minor? (get-in answers-by-key [:birth-date :value]))
         applier-recipients              (->> (:answers application)
                                              (filter #(= "email" (:key %)))
                                              (map :value))
         guardian-recipients             (->> (:answers application)
                                              (filter (fn [answer]
                                                        (#{"guardian-email" "guardian-email-secondary"} (:key answer))))
                                              (mapcat :value))
         recipients                      (if-not (and minor? guardian?) applier-recipients guardian-recipients)
         subject                         (if subject (subject lang) (email-template :subject))
         application-url                 (modify-link (:secret application))
         template-params                 (cond-> {:hakukohteet                (hakukohde-names tarjonta-info lang application)
                                                  :application-oid            (:key application)
                                                  :application-url            application-url
                                                  :content                    content
                                                  :content-ending             content-ending
                                                  :attachments-without-answer attachments-without-answer
                                                  :signature                  signature}
                                                 (and minor? guardian?) (dissoc :application-url :content-ending))
         body                            (selmer/render-file
                                           (template-name lang)
                                           template-params)]
     (when (seq recipients)
       {:from       from-address
        :recipients recipients
        :subject    subject
        :body       body}))))


(defn- create-submit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id guardian?]
  (create-email koodisto-cache tarjonta-service
                organization-service
                ohjausparametrit-service
                nil
                submit-email-template-filename
                application-id
                guardian?))

(defn preview-submit-emails [previews]
  (map
   #(let [lang           (:lang %)
          subject        (:subject %)
          content        (:content %)
          content-ending (:content-ending %)
          signature      (:signature %)]
      (preview-submit-email lang subject content content-ending signature)) previews))


(defn- create-edit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id guardian?]
  (create-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                edit-email-subjects
                #(str "templates/email_edit_confirmation_template_"
                      (name %)
                      ".html")
                application-id
                guardian?))

(defn- create-refresh-secret-email
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id]
  (create-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                refresh-secret-email-subjects
                #(str "templates/email_refresh_secret_template_" (name %) ".html")
                application-id))

(defn start-email-job [job-runner email]
  (let [job-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                 (job/start-job job-runner
                                connection
                                (:type email-job/job-definition)
                                email))]
    (log/info "Started application confirmation email job (to viestintäpalvelu) with job id" job-id ":")
    (log/info email)))

(defn start-email-submit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-submit-email koodisto-cache tarjonta-service
                                                   organization-service
                                                   ohjausparametrit-service
                                                   application-id
                                                   false))
  (when-let [email (create-submit-email koodisto-cache tarjonta-service
                                        organization-service
                                        ohjausparametrit-service
                                        application-id
                                        true)]
    (start-email-job job-runner email)))

(defn start-email-edit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-edit-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                 application-id
                                                 false))
  (when-let [email (create-edit-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                      application-id
                                      true)]
    (start-email-job job-runner email)))

(defn start-email-refresh-secret-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-refresh-secret-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                           application-id)))

(defn store-email-templates
  [form-key session templates]
  (let [stored-templates (mapv #(email-store/create-or-update-email-template
                                  form-key
                                  (:lang %)
                                  (-> session :identity :oid)
                                  (:subject %)
                                  (:content %)
                                  (:content-ending %)
                                  (:signature %))
                           templates)]
    (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content_ending %) (:signature %)) stored-templates)))
