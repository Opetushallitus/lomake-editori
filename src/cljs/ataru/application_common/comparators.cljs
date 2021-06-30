(ns ataru.application-common.comparators)

(defn duplikoitu-kysymys-hakukohde-comparator
  [selected-hakukohteet]
  (fn [a b]
    (if (and (:original-question a) (= (:original-question a) (:original-question b)))
      (-  (.indexOf selected-hakukohteet (:duplikoitu-kysymys-hakukohde-oid a))
          (.indexOf selected-hakukohteet (:duplikoitu-kysymys-hakukohde-oid b)))
      0)))