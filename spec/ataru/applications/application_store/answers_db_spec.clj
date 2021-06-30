(ns ataru.applications.application-store.answers-db-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.db.db :as db]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after describe it should= tags]]))

(def ^:private test-form-id (atom nil))
(def ^:private test-application-id (atom nil))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- delete-form [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/delete! conn :forms ["id = ?" id])))

(defn- delete-application! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/delete! conn :application_hakukohde_attachment_reviews
                  ["application_key = (select key from applications where id = ?)" id])
    (jdbc/delete! conn :applications
                  ["id = ?" id])))

(defn- reset-database! []
  (println "Tyhjennetään hakemuksia tietokannasta.")
  (delete-application! @test-application-id)
  (delete-form @test-form-id))

(def test-form
  {:name             {:fi "Lomake"}
   :content          []
   :created-by       "Testaaja"
   :languages        ["fi"]
   :organization-oid "1.2.246.562.10.00000000001"
   :deleted          nil
   :locked           nil
   :locked-by        nil})

(def test-application
  {:lang    "fi"
   :answers []})

(defn- add-application-with-answers [answers]
  (let [form-id        (-> (form-store/create-new-form! test-form)
                           :key
                           form-store/latest-id-by-key)
        application-id (-> test-application
                           (assoc :form form-id)
                           (assoc :answers answers)
                           (store/add-application
                            []
                            test-form
                            {}
                            audit-logger))]
    (reset! test-form-id form-id)
    (reset! test-application-id application-id)
    application-id))

(defn- create-answer [with-properties]
  (merge {:key       (util/component-id)
          :value     ""
          :fieldType "textField"
          :original-question nil
          :duplikoitu-kysymys-hakukohde-oid nil}
         with-properties))

(describe "get application answers:"
  (tags :unit :database)

  (after
   (reset-database!))

  (describe "basic fields:"
    (it "should get answers containing a string"
      (let [answers           [(create-answer {:value "vastaus"})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application))))

    (it "should get answers containing a null value"
      (let [answers           [(create-answer {:value nil})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application)))))

  (describe "question group:"
    (it "should get answers containing values"
      (let [answers           [(create-answer {:value [["liite-id-0-0" "liite-id-0-1"] ["liite-id-1-0"]]})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application))))

    (it "should get answers containing a null value"
      (let [answers           [(create-answer {:value [["liite-id"] nil]})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application))))

    (it "should get answers containing an empty value"
      (let [answers           [(create-answer {:value [[] ["liite-id"]]})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application)))))

  (describe "repeatable fields:"
    (it "should get an non empty answer value"
      (let [answers           [(create-answer {:value ["vastaus 1" "vastaus 2"]})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application))))

    (it "should get an empty answer value"
      (let [answers           [(create-answer {:value []})]
            application-id    (add-application-with-answers answers)
            found-application (store/get-application application-id)]
        (should= answers (:answers found-application))))))
