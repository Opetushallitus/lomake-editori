(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row VerticalAlignment Row$MissingCellPolicy]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook XSSFSheet XSSFCell XSSFCellStyle])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.component-data.base-education-module :as bem]
            [ataru.component-data.component :as component]
            [ataru.component-data.higher-education-base-education-module :as hebem]
            [ataru.component-data.person-info-module :as pim]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.files.file-store :as file-store]
            [ataru.util :as util]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.translations.texts :refer [excel-texts virkailija-texts]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy debug]]
            [ataru.application.review-states :as review-states]
            [ataru.application.application-states :as application-states]))

(def higher-education-base-education-questions
  (->> (hebem/module {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(def base-education-questions
  (->> (bem/module {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(def person-info-questions
  (->> (pim/person-info-module)
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(def lupatiedot-questions
  (->> (component/lupatiedot {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(def answer-to-always-include?
  (clojure.set/union
   #{"hakukohteet"}
   higher-education-base-education-questions
   base-education-questions
   person-info-questions
   lupatiedot-questions))

(def max-value-length 5000)

(def tz (t/time-zone-for-id "Europe/Helsinki"))

(def ^:private modified-time-format
  (f/formatter "yyyy-MM-dd HH:mm:ss" tz))

(def ^:private filename-time-format
  (f/formatter "yyyy-MM-dd_HHmm" tz))

(defn time-formatter
  ([date-time formatter]
   (f/unparse formatter date-time))
  ([date-time]
    (time-formatter date-time modified-time-format)))

(defn application-state-formatter
  [state lang]
  (or
    (lang (->> review-states/application-review-states
               (filter #(= (first %) state))
               first
               second))
    "Tuntematon"))

(defn hakukohde-review-formatter
  [requirement-name application-hakukohde-reviews lang]
  (let [reviews     (filter
                      #(= (:requirement %) requirement-name)
                      application-hakukohde-reviews)
        requirement (last
                      (first
                        (filter #(= (first %) (keyword requirement-name)) review-states/hakukohde-review-types)))
        get-label   (partial application-states/get-review-state-label-by-name requirement)]
    (if (= 1 (count reviews))
      (get-label (-> (first reviews) :state) lang)
      (clojure.string/join
        "\n"
        (map
          (fn [{:keys [hakukohde hakukohde-name state]}]
            (format "%s (%s): %s"
                    hakukohde-name
                    hakukohde
                    (get-label state lang)))
          reviews)))))

(defn- application-review-notes-formatter
  ([review-notes]
    (application-review-notes-formatter nil review-notes))
  ([state-to-select review-notes]
   (->> (if state-to-select
          (filter #(= state-to-select (:state-name %)) review-notes)
          (remove #(some? (:state-name %)) review-notes))
        (map (fn [{:keys [created-time notes hakukohde first-name last-name]}]
               (str
                 (time-formatter created-time)
                 " "
                 (when (and first-name last-name)
                   (str
                     first-name
                     " "
                     last-name))
                 (when hakukohde
                   (str " (hakukohde " hakukohde ")"))
                 ": "
                 notes)))
        (clojure.string/join ",\n"))))

(def ^:private form-meta-fields
  [{:label     (:name excel-texts)
    :field     :name
    :format-fn #(some (partial get %) [:fi :sv :en])}
   {:label (:id excel-texts)
    :field :id}
   {:label (:key excel-texts)
    :field :key}
   {:label     (:created-time excel-texts)
    :field     :created-time
    :format-fn time-formatter}
   {:label (:created-by excel-texts)
    :field :created-by}])

(def ^:private application-meta-fields
  [{:label     (:id excel-texts)
    :field     [:application :key]}
   {:label     (:sent-at excel-texts)
    :field     [:application :created-time]
    :format-fn time-formatter}
   {:label     (:application-state excel-texts)
    :field     [:application :state]
    :lang?     true
    :format-fn application-state-formatter}
   {:label     (:hakukohde-handling-state excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "processing-state")}
   {:label     (:kielitaitovaatimus excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "language-requirement")}
   {:label     (:tutkinnon-kelpoisuus excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "degree-requirement")}
   {:label     (:hakukelpoisuus excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "eligibility-state")}
   {:label     (:eligibility-set-automatically virkailija-texts)
    :field     [:application :eligibility-set-automatically]
    :format-fn #(clojure.string/join "\n" %)}
   {:label     (:ineligibility-reason virkailija-texts)
    :field     [:application-review-notes]
    :format-fn (partial application-review-notes-formatter "eligibility-state")}
   {:label     (:maksuvelvollisuus excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "payment-obligation")}
   {:label     (:valinnan-tila excel-texts)
    :field     [:application :application-hakukohde-reviews]
    :lang?     true
    :format-fn (partial hakukohde-review-formatter "selection-state")}
   {:label     (:pisteet excel-texts)
    :field     [:application-review :score]}
   {:label     (:applicant-oid excel-texts)
    :field     [:application :person-oid]}
   {:label     (:turvakielto excel-texts)
    :field     [:person :turvakielto]
    :format-fn (fnil (fn [turvakielto] (if turvakielto "kyllä" "ei")) false)}
   {:label     (:notes excel-texts)
    :field     [:application-review-notes]
    :format-fn application-review-notes-formatter}])

(defn- create-cell-styles
  [workbook]
  {:style (doto (.createCellStyle workbook)
            (.setWrapText true)
            (.setVerticalAlignment VerticalAlignment/TOP))
   :quote-prefix-style
   (doto (.createCellStyle workbook)
     (.setQuotePrefixed true)
     (.setWrapText true)
     (.setVerticalAlignment VerticalAlignment/TOP))})

(defn- create-workbook-and-styles
  []
  (let [workbook (XSSFWorkbook.)]
    [workbook (create-cell-styles workbook)]))

(defn- indexed-meta-fields
  [fields]
  (map-indexed (fn [idx field] (merge field {:column idx})) fields))

(defn- set-cell-style ^XSSFCell [^XSSFCell cell value styles]
  (if (and (string? value)
           (contains? #{\= \+ \- \@} (first value)))
    (.setCellStyle cell (:quote-prefix-style styles))
    (.setCellStyle cell (:style styles)))
  cell)

(defn- update-row-cell! [styles ^XSSFSheet sheet row column value]
  (when-let [^String v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet (int row))
            (.createRow sheet (int row)))
        (.getCell (int column) Row$MissingCellPolicy/CREATE_NULL_AS_BLANK)
        (set-cell-style value styles)
        (.setCellValue v)))
  sheet)

(defn- make-writer [styles sheet row-offset]
  (fn [row column value]
    (update-row-cell!
      styles
      sheet
      (+ row-offset row)
      column
      value)
    [sheet row-offset row column value]))

(defn- write-form-meta!
  [writer form applications fields lang]
  (doseq [meta-field fields]
    (let [col        (:column meta-field)
          value-from (case (:from meta-field)
                       :applications (first applications)
                       form)
          value      ((:field meta-field) value-from)
          format-fn  (:format-fn meta-field)
          value      (if (some? format-fn)
                       (if (:lang? meta-field)
                         (format-fn value lang)
                         (format-fn value))
                       value)]
      (writer 0 col value))))

(defn- write-headers! [writer headers meta-fields lang]
  (doseq [meta-field meta-fields]
    (writer 0 (:column meta-field) (-> meta-field :label lang)))
  (doseq [header headers]
    (writer 0 (+ (:column header) (count meta-fields)) (:decorated-header header))))

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (util/non-blank-val (:label koodi) [lang :fi :sv :en])))

(defn- raw-values->human-readable-value [field-descriptor {:keys [lang]} get-koodisto-options value]
  (let [lang (-> lang clojure.string/lower-case keyword)
        koodisto-source (:koodisto-source field-descriptor)
        options (:options field-descriptor)]
    (cond (some? koodisto-source)
          (let [koodisto (get-koodisto-options (:uri koodisto-source) (:version koodisto-source))
                koodi-uri->label (partial get-label koodisto lang)]
            (->> (clojure.string/split value #"\s*,\s*")
                 (mapv koodi-uri->label)
                 (interpose ",\n")
                 (apply str)))
          (= (:fieldType field-descriptor) "attachment")
          (try
            (let [[{:keys [filename size]}] (file-store/get-metadata [value])]
              (str filename " (" (util/size-bytes->str size) ")"))
            (catch Exception e
              (util/non-blank-val (:internal-server-error virkailija-texts)
                                  [lang :fi :sv :en])))
          (not (empty? options))
          (some (fn [option]
                  (when (= value (:value option))
                    (or (util/non-blank-val (:label option) [lang :fi :sv :en])
                        value)))
                options)
          :else
          value)))

(defn- all-answers-sec-or-vec? [answers]
  (every? sequential? answers))

(defn- kysymysryhma-answer? [value-or-values]
  (and (sequential? value-or-values)
       (all-answers-sec-or-vec? value-or-values)))

(defn- write-application! [writer application
                           application-review
                           application-review-notes
                           person
                           headers
                           application-meta-fields
                           form-fields-by-key
                           get-koodisto-options
                           lang]
  (doseq [meta-field application-meta-fields]
    (let [format-fn  (:format-fn meta-field)
          field      (get-in {:application        application
                              :application-review application-review
                              :application-review-notes application-review-notes
                              :person             person}
                             (:field meta-field))
          meta-value (if (some? format-fn)
                       (if (:lang? meta-field)
                         (format-fn field lang)
                         (format-fn field))
                       field)]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [answer-key             (:key answer)
          field-descriptor       (get form-fields-by-key answer-key)
          column                 (:column (first (filter #(= answer-key (:id %)) headers)))
          value-or-values        (get person (keyword answer-key) (:value answer))
          ->human-readable-value (partial raw-values->human-readable-value field-descriptor application get-koodisto-options)
          value                  (cond
                                   (kysymysryhma-answer? value-or-values)
                                   (->> value-or-values
                                        (map #(clojure.string/join "," %))
                                        (map ->human-readable-value)
                                        (map-indexed #(format "#%s: %s,\n" %1 %2))
                                        (apply str))

                                   (sequential? value-or-values)
                                   (->> value-or-values
                                        (map ->human-readable-value)
                                        (interpose ",\n")
                                        (apply str))

                                   :else
                                   (->human-readable-value value-or-values))
          value-length           (count value)
          value-truncated        (if (< max-value-length value-length)
                                   (str
                                    (subs value 0 (- max-value-length 100))
                                    "—— [ vastaus liian pitkä Excel-vientiin, poistettu "
                                    (- value-length max-value-length -100) " merkkiä]")
                                   value)]
      (when (and value-truncated column)
        (writer 0 (+ column (count application-meta-fields)) value-truncated)))))

(defn- pick-header
  [form-fields-by-id form-field]
  (str (match form-field
         {:params      {:adjacent true}
          :children-of parent-id}
         (str (util/non-blank-val (get-in form-fields-by-id [parent-id :label])
                                  [:fi :sv :en])
              " - ")
         {:fieldType "attachment"}
         "Liitepyyntö: "
         :else
         "")
       (util/non-blank-val (:label form-field) [:fi :sv :en])))

(defn- belongs-to-other-hakukohde?
  [selected-oids form-field]
  (and (not-empty selected-oids)
       (let [belongs-to (set (concat (:belongs-to-hakukohderyhma form-field)
                                     (:belongs-to-hakukohteet form-field)))]
         (and (not-empty belongs-to)
              (empty? (clojure.set/intersection selected-oids belongs-to))))))

(defn- headers-from-form
  [form-fields form-fields-by-id skip-answers? included-ids selected-oids]
  (->> form-fields
       (remove #(or (:exclude-from-answers %)
                    (and (if (not-empty included-ids)
                           (not (included-ids (:id %)))
                           skip-answers?)
                         (not (answer-to-always-include? (:id %))))
                    (belongs-to-other-hakukohde? selected-oids %)))
       (map #(vector (:id %) (pick-header form-fields-by-id %)))))

(defn- headers-from-applications
  [form-fields-by-id skip-answers? applications]
  (->> applications
       (mapcat :answers)
       (remove #(or (contains? form-fields-by-id (:key %))
                    (and skip-answers?
                         (not (answer-to-always-include? (:key %))))))
       (map #(vector (:key %) (util/non-blank-val (:label %) [:fi :sv :en])))))

(defn- extract-headers
  [applications form selected-oids skip-answers? included-ids]
  (let [form-fields       (util/flatten-form-fields (:content form))
        form-fields-by-id (util/group-by-first :id form-fields)]
    (map-indexed (fn [idx [id header]]
                   {:id               id
                    :decorated-header (or header "")
                    :column           idx})
                 (concat (headers-from-form form-fields
                                            form-fields-by-id
                                            skip-answers?
                                            included-ids
                                            selected-oids)
                         (headers-from-applications form-fields-by-id
                                                    skip-answers?
                                                    applications)))))

(defn- create-form-meta-sheet [workbook styles meta-fields lang]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer styles sheet 0)]
    (doseq [meta-field meta-fields
            :let [column (:column meta-field)
                  label  (-> meta-field :label lang)]]
      (writer 0 column label))
    sheet))

(def ^:private invalid-char-matcher #"[\\/\*\[\]:\?]")

(defn- sheet-name [{:keys [id name]}]
  {:pre [(some? id)
         (some? name)]}
  (let [name (str id "_" (clojure.string/replace (some (partial get name) [:fi :sv :en]) invalid-char-matcher "_"))]
    (cond-> name
      (> (count name) 30)
      (subs 0 30))))

(defn set-column-widths [^XSSFWorkbook workbook]
  (doseq [n (range (.getNumberOfSheets workbook))
          :let [sheet (.getSheetAt workbook (int n))]
          y (range (.getLastCellNum (.getRow sheet 0)))]
    (.autoSizeColumn sheet (short y))))

(defn- update-hakukohteet-for-legacy-applications [application]
  (let [hakukohteet (-> application :answers :hakukohteet)
        hakukohde   (:hakukohde application)]
    (if (or hakukohteet
            (and (not hakukohteet) (not hakukohde)))
      application
      (update application :answers conj
        {:key "hakukohteet" :fieldType "hakukohteet" :value (:hakukohde application) :label {:fi "Hakukohteet"}}))))

(defn- get-hakukohde-name [get-hakukohde lang-s haku-oid hakukohde-oid]
  (let [lang (keyword lang-s)]
    (when-let [hakukohde (get-hakukohde haku-oid hakukohde-oid)]
      (str (util/non-blank-val (:name hakukohde) [lang :fi :sv :en]) " - "
           (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])))))

(defn- add-hakukohde-name [get-haku get-hakukohde lang hakukohde-answer haku-oid]
  (update hakukohde-answer :value
          (partial map-indexed
                   (fn [index oid]
                     (let [name           (get-hakukohde-name get-hakukohde lang haku-oid oid)
                           priority-index (when (:prioritize-hakukohteet (:tarjonta (get-haku haku-oid)))
                                            (str "(" (inc index) ") "))]
                       (if name
                         (str priority-index name " (" oid ")")
                         (str priority-index oid)))))))

(defn- add-hakukohde-names [get-haku get-hakukohde application]
  (update application :answers
          (partial map (fn [answer]
                         (if (= "hakukohteet" (:key answer))
                           (add-hakukohde-name get-haku get-hakukohde (:lang application) answer (:haku application))
                           answer)))))

(defn- add-all-hakukohde-reviews
  [get-hakukohde selected-hakukohde-oids application]
  (let [active-hakukohteet     (set (or
                                     (not-empty
                                       (:hakukohde application))
                                     ["form"]))
        all-reviews            (->> (application-states/get-all-reviews-for-all-requirements
                                      application
                                      selected-hakukohde-oids)
                                    (filter
                                      #(contains? active-hakukohteet (:hakukohde %))))
        all-reviews-with-names (map
                                (fn [{:keys [hakukohde] :as review}]
                                    (assoc review
                                           :hakukohde-name
                                           (get-hakukohde-name
                                             get-hakukohde
                                             (:lang application)
                                             (:haku application)
                                             hakukohde)))
                                all-reviews)]
    (assoc application :application-hakukohde-reviews all-reviews-with-names)))

(defn- filter-eligibility-set-automatically
  [selected-hakukohde-oids application]
  (if (empty? selected-hakukohde-oids)
    application
    (update application :eligibility-set-automatically
            (partial filter (set selected-hakukohde-oids)))))

(defn hakukohde-to-hakukohderyhma-oids [all-hakukohteet selected-hakukohde]
  (some->> (first (filter #(= selected-hakukohde (:oid %)) @all-hakukohteet))
           :hakukohderyhmat))

(defn hakukohderyhma-to-hakukohde-oids [all-hakukohteet selected-hakukohderyhma]
  (->> @all-hakukohteet
       (filter #(contains? (set (:hakukohderyhmat %)) selected-hakukohderyhma))
       (map :oid)))

(defn export-applications
  [applications
   application-reviews
   application-review-notes
   selected-hakukohde
   selected-hakukohderyhma
   skip-answers?
   included-ids
   lang
   tarjonta-service
   koodisto-cache
   organization-service
   ohjausparametrit-service]
  (let [[^XSSFWorkbook workbook styles] (create-workbook-and-styles)
        form-meta-fields             (indexed-meta-fields form-meta-fields)
        form-meta-sheet              (create-form-meta-sheet workbook styles form-meta-fields lang)
        application-meta-fields      (indexed-meta-fields application-meta-fields)
        get-form-by-id               (memoize form-store/fetch-by-id)
        get-latest-form-by-key       (memoize form-store/fetch-by-key)
        get-koodisto-options         (memoize (fn [uri version]
                                                (koodisto/get-koodisto-options koodisto-cache uri version)))
        get-tarjonta-info            (memoize (fn [haku-oid]
                                                  (tarjonta-parser/parse-tarjonta-info-by-haku
                                                   koodisto-cache
                                                   tarjonta-service
                                                   organization-service
                                                   ohjausparametrit-service
                                                   haku-oid)))
        get-hakukohde                (memoize (fn [haku-oid hakukohde-oid]
                                                  (->> (get-tarjonta-info haku-oid)
                                                       :tarjonta
                                                       :hakukohteet
                                                       (some #(when (= hakukohde-oid (:oid %)) %)))))
        all-hakukohteet              (delay (->> (group-by :haku applications)
                                                 (mapcat (fn [[haku applications]]
                                                             (map #(get-hakukohde haku %) (set (mapcat :hakukohde applications)))))))
        selected-hakukohde-oids      (or (some-> selected-hakukohde vector)
                                         (and selected-hakukohderyhma
                                              (hakukohderyhma-to-hakukohde-oids all-hakukohteet selected-hakukohderyhma)))
        selected-hakukohderyhma-oids (or (some-> selected-hakukohderyhma vector)
                                         (and selected-hakukohde
                                              (hakukohde-to-hakukohderyhma-oids all-hakukohteet selected-hakukohde)))
        selected-oids                (clojure.set/union (set selected-hakukohde-oids) (set selected-hakukohderyhma-oids))]
    (->> applications
         (map update-hakukohteet-for-legacy-applications)
         (map (partial add-hakukohde-names get-tarjonta-info get-hakukohde))
         (map (partial add-all-hakukohde-reviews get-hakukohde selected-hakukohde-oids))
         (map (partial filter-eligibility-set-automatically selected-hakukohde-oids))
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
         (map-indexed (fn [sheet-idx {:keys [^String sheet-name form applications]}]
                        (let [applications-sheet (.createSheet workbook sheet-name)
                              headers            (extract-headers applications form selected-oids skip-answers? included-ids)
                              meta-writer        (make-writer styles form-meta-sheet (inc sheet-idx))
                              header-writer      (make-writer styles applications-sheet 0)
                              form-fields-by-key (reduce #(assoc %1 (:id %2) %2)
                                                         {}
                                                         (util/flatten-form-fields (:content form)))]
                          (write-form-meta! meta-writer form applications form-meta-fields lang)
                          (write-headers! header-writer headers application-meta-fields lang)
                          (->> applications
                               (sort-by :created-time)
                               (reverse)
                               (map #(merge % (get-tarjonta-info (:haku %))))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer                   (make-writer styles applications-sheet (inc row-idx))
                                                    application-review           (get application-reviews (:key application))
                                                    review-notes-for-application (get application-review-notes (:key application))
                                                    person                       (:person application)]
                                                (write-application! row-writer
                                                                    application
                                                                    application-review
                                                                    review-notes-for-application
                                                                    person
                                                                    headers
                                                                    application-meta-fields
                                                                    form-fields-by-key
                                                                    get-koodisto-options
                                                                    lang))))
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

(defn create-filename [identifying-part]
  {:pre [(some? identifying-part)]}
  (str
   (sanitize-name identifying-part)
   "_"
   (time-formatter (t/now) filename-time-format)
   ".xlsx"))

