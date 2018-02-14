(ns ataru.tarjonta-service.hakuaika
  (:require
   [clj-time.core :as time]
   [clj-time.coerce :as time-coerce]))

(defn- time-within?
  ([instant {:keys [alkuPvm loppuPvm]}]
   (time-within? instant alkuPvm loppuPvm))
  ([instant start end]
   (time/within?
    (time/interval (time-coerce/from-long start)
                   (time-coerce/from-long end))
    instant)))

(defn- find-current-or-last-hakuaika
  [hakuaikas]
  (let [now              (time/now)
        current-hakuaika (first (filter (partial time-within? now) hakuaikas))]
    (or
      current-hakuaika
      (last hakuaikas))))

(defn- jatkuva-haku? [haku]
  (clojure.string/starts-with? (:hakutapaUri haku) "hakutapa_03#"))

(defn hakuaika-on [start end]
      (cond
        (and start end (time-within? (time/now) start end))
        true

        ;; "Jatkuva haku"
        (and start (not end) (time/after? (time/now) (time-coerce/from-long start)))
        true

        :else
        false))

(defn parse-hakuaika-from-hakukohde [hakukohde]
      (if-let [start (:hakuaikaAlkuPvm hakukohde)]
              (let [end (:hakuaikaLoppuPvm hakukohde)]
                   {:start (:hakuaikaAlkuPvm hakukohde)
                    :end   (:hakuaikaLoppuPvm hakukohde)
                    :on (hakuaika-on start end)})))

(defn- parse-haku-hakuaika
  "Hakuaika from hakuaika can override hakuaika from haku. Haku may have multiple hakuaikas defined."
  [haku]
  (let [this-haku-hakuaika (find-current-or-last-hakuaika (:hakuaikas haku))]
               {:start (:alkuPvm this-haku-hakuaika)
                :end   (:loppuPvm this-haku-hakuaika)}))

(defn get-hakuaika-info [haku ohjausparametrit]
  (as-> (parse-haku-hakuaika haku) {:keys [start end] :as interval}
                (assoc interval :on (hakuaika-on start end))
                (assoc interval
                       :attachment-modify-grace-period-days
                       (-> ohjausparametrit :PH_LMT :value))
                (assoc interval :jatkuva-haku? (jatkuva-haku? haku))
                (assoc interval :hakukierros-end (-> ohjausparametrit :PH_HKP :date))))
