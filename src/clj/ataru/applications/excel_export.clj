(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row VerticalAlignment]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook XSSFCell XSSFCellStyle])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.util.language-label :as label]
            [ataru.application.review-states :refer [application-review-states]]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.files.file-store :as file-store]
            [ataru.util :as util]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy debug]]))

(def tz (t/default-time-zone))

(def ^:private modified-time-format
  (f/formatter "yyyy-MM-dd HH:mm:ss" tz))

(def ^:private filename-time-format
  (f/formatter "yyyy-MM-dd_HHmm" tz))

(defn time-formatter
  ([date-time formatter]
   (f/unparse formatter date-time))
  ([date-time]
    (time-formatter date-time modified-time-format)))

(defn state-formatter [state]
  (or (get application-review-states state) "Tuntematon"))

(def ^:private form-meta-fields
  [{:label "Nimi"
    :field :name}
   {:label "Id"
    :field :id}
   {:label "Tunniste"
    :field :key}
   {:label "Viimeksi muokattu"
    :field :created-time
    :format-fn time-formatter}
   {:label "Viimeinen muokkaaja"
    :field :created-by}])

(def ^:private application-meta-fields
  [{:label "Id"
    :field :key}
   {:label     "Lähetysaika"
    :field     :created-time
    :format-fn time-formatter}
   {:label     "Tila"
    :field     :state
    :format-fn state-formatter}
   {:label     "Hakijan henkilö-OID"
    :field     :person-oid
    :format-fn str}])

(def ^:private review-headers ["Muistiinpanot" "Pisteet"])

(defn- indexed-meta-fields
  [fields]
  (map-indexed (fn [idx field] (merge field {:column idx})) fields))

(defn- set-cell-style [cell workbook]
  (let [cell-style (.createCellStyle workbook)]
    (.setWrapText cell-style true)
    (.setVerticalAlignment cell-style VerticalAlignment/TOP)
    (.setCellStyle cell cell-style)
    cell))

(defn- update-row-cell! [sheet row column value workbook]
  (when-let [v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet row)
            (.createRow sheet row))
        (.getCell column Row/CREATE_NULL_AS_BLANK)
        (set-cell-style workbook)
        (.setCellValue v)))
  sheet)

(defn- make-writer [sheet row-offset workbook]
  (fn [row column value]
    (update-row-cell!
      sheet
      (+ row-offset row)
      column
      value workbook)
    [sheet row-offset row column value]))

(defn- write-form-meta!
  [writer form applications fields]
  (doseq [meta-field fields]
    (let [col        (:column meta-field)
          value-from (case (:from meta-field)
                       :applications (first applications)
                       form)
          value      ((:field meta-field) value-from)
          formatter  (or (:format-fn meta-field) identity)]
      (writer 0 col (formatter value)))))

(defn- write-headers! [writer headers meta-fields]
  (doseq [meta-field meta-fields]
    (writer 0 (:column meta-field) (:label meta-field)))
  (doseq [header headers]
    (writer 0 (+ (:column header) (count meta-fields)) (:decorated-header header))))

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (get-in koodi [:label lang])))

(defn- raw-values->human-readable-value [{:keys [content]} {:keys [lang]} key value]
  (let [field-descriptor (util/get-field-descriptor content key)
        lang             (-> lang clojure.string/lower-case keyword)]
    (if-some [koodisto-source (:koodisto-source field-descriptor)]
      (let [koodisto         (koodisto/get-koodisto-options (:uri koodisto-source) (:version koodisto-source))
            koodi-uri->label (partial get-label koodisto lang)]
        (->> (clojure.string/split value #"\s*,\s*")
             (mapv koodi-uri->label)
             (interpose ",\n")
             (apply str)))
      (if (= (:fieldType field-descriptor) "attachment")
        (let [[{:keys [filename size]}] (file-store/get-metadata [value])]
          (when (and filename size)
            (str filename " (" (util/size-bytes->str size) ")")))
        value))))

(defn- sec-or-vec? [value]
  (or (seq? value) (vector? value)))

(defn- all-answers-sec-or-vec? [answers]
  (every? sec-or-vec? answers))

(defn- kysymysryhma-answer? [value-or-values]
  (and (sec-or-vec? value-or-values)
       (all-answers-sec-or-vec? value-or-values)))

(defn- write-application! [writer application headers application-meta-fields form]
  (doseq [meta-field application-meta-fields]
    (let [meta-value ((or (:format-fn meta-field) identity) ((:field meta-field) application))]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [column          (:column (first (filter #(= (:key answer) (:id %)) headers)))
          value-or-values (:value answer)
          value           (cond
                            (kysymysryhma-answer? value-or-values)
                            (->> value-or-values
                                 (map #(clojure.string/join "," %))
                                 (map (partial raw-values->human-readable-value form application (:key answer)))
                                 (map-indexed #(format "#%s: %s,\n" %1 %2))
                                 (apply str))

                            (sec-or-vec? value-or-values)
                            (->> value-or-values
                                 (map (partial raw-values->human-readable-value form application (:key answer)))
                                 (interpose ",\n")
                                 (apply str))

                            :else
                            (raw-values->human-readable-value form application (:key answer) value-or-values))]
      (when (and value column)
        (writer 0 (+ column (count application-meta-fields)) value))))
  (let [application-review  (application-store/get-application-review (:key application))
        beef-header-count   (- (apply max (map :column headers)) (count review-headers))
        prev-header-count   (+ beef-header-count
                               (count application-meta-fields))
        notes-column        (inc prev-header-count)
        score-column        (inc notes-column)
        notes               (:notes application-review)
        score               (:score application-review)]
    (when notes (writer 0 notes-column notes))
    (when score (writer 0 score-column score))))

(defn- form-label? [form-element]
  (and (not= "infoElement" (:fieldClass form-element))
       (not (:exclude-from-answers form-element))))

(defn- hidden-answer? [form-element]
  (:exclude-from-answers form-element))

(defn- pick-label
  [form-element pick-cond]
  (when (pick-cond form-element)
    [[(:id form-element)
      (label/get-language-label-in-preferred-order (:label form-element))]]))

(defn pick-form-labels
  [form-content pick-cond]
  (->> (reduce
         (fn [acc form-element]
           (let [followups (remove nil? (mapcat :followups (:options form-element)))]
             (cond
               (pos? (count (:children form-element)))
               (into acc (pick-form-labels (:children form-element) pick-cond))

               (pos? (count followups))
               (into (into acc (pick-label form-element pick-cond)) (pick-form-labels followups pick-cond))

               :else
               (into acc (pick-label form-element pick-cond)))))
         []
         form-content)))

(defn- find-parent [element fields]
  (let [contains-element? (fn [children] (some? ((set (map :id children)) (:id element))))
        followup-dropdown (fn [field] (mapcat :followups (:options field)))]
    (reduce
      (fn [parent field]
        (or parent
          (match field
            ((_ :guard contains-element?) :<< :children) field

            ((followups :guard not-empty) :<< followup-dropdown)
            (or
              (when (contains-element? followups)
                field)
              (find-parent element followups))

            ((children :guard not-empty) :<< :children)
            (find-parent element children)

            :else nil)))
      nil
      fields)))

(defn- decorate [flat-fields fields id header]
  (let [element (first (filter #(= (:id %) id) flat-fields))]
    (match element
      {:params {:adjacent true}}
      (if-let [parent-element (find-parent element fields)]
        (str (-> parent-element :label :fi) " - " header)
        header)
      {:fieldType "attachment"}
      (str "Liitepyyntö: " (label/get-language-label-in-preferred-order (:label element)))
      :else header)))

(defn- extract-headers-from-applications [applications form]
  (let [hidden-answers (map first (pick-form-labels (:content form) hidden-answer?))]
    (mapcat (fn [application]
              (->> (:answers application)
                   (filter (fn [answer]
                             (not (some (partial = (:key answer)) hidden-answers))))
                   (mapv (fn [answer]
                           (vals (select-keys answer [:key :label]))))))
            applications)))

(defn- remove-duplicates-by-field-id
  [labels-in-form labels-in-applications]
  (let [form-element-ids (set (map first labels-in-form))]
    (remove (fn [[key _]]
              (contains? form-element-ids key))
            labels-in-applications)))

(defn- extract-headers
  [applications form]
  (let [labels-in-form              (pick-form-labels (:content form) form-label?)
        labels-in-applications      (extract-headers-from-applications applications form)
        labels-only-in-applications (remove-duplicates-by-field-id labels-in-form labels-in-applications)
        all-labels                  (distinct (concat labels-in-form labels-only-in-applications (map vector (repeat nil) review-headers)))
        decorator                   (partial decorate (util/flatten-form-fields (:content form)) (:content form))]
    (for [[idx [id header]] (map vector (range) all-labels)
          :when (string? header)]
      {:id               id
       :decorated-header (decorator id header)
       :header           header
       :column           idx})))

(defn- create-form-meta-sheet [workbook meta-fields]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer sheet 0 workbook)]
    (doseq [meta-field meta-fields
            :let [column (:column meta-field)
                  label  (:label meta-field)]]
      (writer 0 column label))
    sheet))

(def ^:private invalid-char-matcher #"[\\/\*\[\]:\?]")

(defn- sheet-name [{:keys [id name]}]
  {:pre [(some? id)
         (some? name)]}
  (let [name (str id "_" (clojure.string/replace name invalid-char-matcher "_"))]
    (cond-> name
      (> (count name) 30)
      (subs 0 30))))

(defn- inject-haku-info
  [tarjonta-service application]
  (merge application
         (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service (:haku application))))

(defn set-column-widths [workbook]
  (doseq [n (range (.getNumberOfSheets workbook))
          :let [sheet (.getSheetAt workbook n)]
          y (range (.getLastCellNum (.getRow sheet 0)))]
    (.autoSizeColumn sheet (short y))))

(defn- update-hakukohteet-for-legacy-applications [application]
  (let [hakukohteet (-> application :answers :hakukohteet)
        hakukohde   (:hakukohde application)]
    (if (or hakukohteet
            (and (not hakukohteet) (not hakukohde)))
      application
      (update application :answers conj
        {:key "hakukohteet" :fieldType "hakukohteet" :value (:hakukohde application) :label "Hakukohteet"}))))

(defn export-applications [applications tarjonta-service]
  (let [workbook                (XSSFWorkbook.)
        form-meta-fields        (indexed-meta-fields form-meta-fields)
        form-meta-sheet         (create-form-meta-sheet workbook form-meta-fields)
        application-meta-fields (indexed-meta-fields application-meta-fields)
        get-form-by-id          (memoize form-store/fetch-by-id)
        get-latest-form-by-key  (memoize form-store/fetch-by-key)]
    (->> applications
         (map update-hakukohteet-for-legacy-applications)
         (reduce (fn [result {:keys [form] :as application}]
                   (let [form-key (:key (get-form-by-id form))
                         form     (get-latest-form-by-key form-key)]
                     (if (contains? result form-key)
                       (update-in result [form-key :applications] conj application)
                       (let [value {:sheet-name   (sheet-name form)
                                    :form         form
                                    :applications [application]}]
                         (assoc result form-key value)))))
                 {})
         (map second)
         (map-indexed (fn [sheet-idx {:keys [sheet-name form applications]}]
                        (let [applications-sheet (.createSheet workbook sheet-name)
                              headers            (extract-headers applications form)
                              meta-writer        (make-writer form-meta-sheet (inc sheet-idx) workbook)
                              header-writer      (make-writer applications-sheet 0 workbook)]
                          (write-form-meta! meta-writer form applications form-meta-fields)
                          (write-headers! header-writer headers application-meta-fields)
                          (->> applications
                               (sort-by :created-time)
                               (reverse)
                               (map (partial inject-haku-info tarjonta-service))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer (make-writer applications-sheet (inc row-idx) workbook)]
                                                (write-application! row-writer application headers application-meta-fields form))))
                               (dorun))
                          (.createFreezePane applications-sheet 0 1 0 1))))
         (dorun))
    (set-column-widths workbook)
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))

(defn- sanitize-name [name]
  (-> name
      (string/replace #"[\s]+" "-")
      (string/replace #"[^\w-]+" "")))

(defn- create-filename [identifying-part]
  {:pre [(some? identifying-part)]}
  (str
   (sanitize-name identifying-part)
   "_"
   (time-formatter (t/now) filename-time-format)
   ".xlsx"))

(defn filename-by-form
  [form-key]
  {:post [(some? %)]}
  (create-filename (-> form-key form-store/fetch-by-key :name)))

(defn filename-by-hakukohde
  [hakukohde-oid session organization-service tarjonta-service]
  {:post [(some? %)]}
  (create-filename (or (.get-hakukohde-name tarjonta-service hakukohde-oid) hakukohde-oid)))

(defn filename-by-haku
  [haku-oid session organization-service tarjonta-service]
  {:post [(some? %)]}
  (create-filename (or (.get-haku-name tarjonta-service haku-oid) haku-oid)))
