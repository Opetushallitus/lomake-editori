(ns ataru.email.email-store
  (:require [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/email-template-queries.sql")

(declare yesql-upsert-email-template!<)
(declare yesql-get-email-templates)

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn create-or-update-email-template
  [form-key lang virkailija-oid subject content content-ending signature]
  (first (exec-db :db yesql-upsert-email-template!< {:form_key       form-key
                                                     :lang           lang
                                                     :virkailija_oid virkailija-oid
                                                     :subject        subject
                                                     :content        content
                                                     :content_ending content-ending
                                                     :signature      signature
                                                     :haku_oid       ""})))

(defn get-email-templates
  [form-key]
  (exec-db :db yesql-get-email-templates {:form_key form-key
                                          :haku_oid ""}))