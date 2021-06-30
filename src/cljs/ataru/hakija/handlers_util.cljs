(ns ataru.hakija.handlers-util
  (:require [ataru.application-common.application-field-common :refer [required-validators]]))

(defn- is-hakukohde-in-hakukohderyhma-of-question
       [tarjonta-hakukohteet hakukohde-oid question]
       (let [is-ryhma-in-hakukohderyhmat (fn [hakukohderyhma] (some #(= hakukohderyhma %) (:belongs-to-hakukohderyhma question)))
             selected-hakukohde (some #(when (= (:oid %) hakukohde-oid) %) tarjonta-hakukohteet)]
         (some is-ryhma-in-hakukohderyhmat (:hakukohderyhmat selected-hakukohde))))

(defn duplicate-questions-for-hakukohde
       [db hakukohde-oid questions question]
       (if (and (:per-hakukohde question) (is-hakukohde-in-hakukohderyhma-of-question (get-in db [:form :tarjonta :hakukohteet]) hakukohde-oid question))
         (conj questions question (-> question
                                      (dissoc :per-hakukohde)
                                      (assoc :id (str (:id question) "_" hakukohde-oid)
                                             :duplikoitu-kysymys-hakukohde-oid hakukohde-oid
                                             :original-question (:id question))))
         (conj questions question)))

(defn- duplicate-questions-for-hakukohde-inner
  [tarjonta-hakukohteet hakukohde-oids questions question]
  (if (and (:per-hakukohde question) (some #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids))
    (let [valid-hakukohde-oids (filter #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids)
          questions-to-add (map #(-> question
                                     (dissoc :per-hakukohde)
                                     (assoc :id (str (:id question) "_" %)
                                            :duplikoitu-kysymys-hakukohde-oid %
                                            :original-question (:id question))) valid-hakukohde-oids)]
        (concat questions [question] questions-to-add))
    (concat questions [question])))

(defn duplicate-questions-for-hakukohteet
  [tarjonta-hakukohteet hakukohde-oids questions]
  (let [questions-duplicated (reduce (partial duplicate-questions-for-hakukohde-inner tarjonta-hakukohteet hakukohde-oids) [] questions)]
    questions-duplicated))

(defn- is-duplicated-required
  [question]
  (some #(contains? required-validators %)
        (:validators question)))

(defn fill-missing-answer-for-hakukohde
  [answers questions]
    (let [missing-questions (filter #(and (:original-question %) (not (get answers (keyword (:id %))))) questions)
          get-original-answer (fn [question]
                                (get answers (keyword (:original-question question))))]
      (if (seq missing-questions)
        (reduce (fn [answers question]
                  (assoc answers (keyword (:id question))
                                 (assoc (get-original-answer question) :valid (not (is-duplicated-required question)))))
                answers missing-questions)
        answers)))

