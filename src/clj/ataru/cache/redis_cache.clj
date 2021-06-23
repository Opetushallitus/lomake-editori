(ns ataru.cache.redis-cache
  (:require [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.locks :as car-locks]
            [ataru.cache.cache-service :as cache]
            [clojure.string :as s])
  (:import [java.util.concurrent
            ArrayBlockingQueue
            ThreadPoolExecutor
            ThreadPoolExecutor$DiscardPolicy
            TimeUnit]))

(defn- ->cache-key
  [name key]
  (str "ataru:cache:item:" name ":" key))

(defn- ->low-priority-queue
  [name]
  (str "ataru:cache:low-priority-update-queue:" name))

(defn- ->high-priority-queue
  [name]
  (str "ataru:cache:high-priority-update-queue:" name))

(defn- ->notify-key
  [name key]
  (str "ataru:cache:notify:" name ":" key))

(defn- ->notify-pattern
  [name]
  (->notify-key name "*"))

(defn- notify-key->key
  [name notify-key]
  (s/replace notify-key (->notify-key name "") ""))

(defn- ->lock-key
  [name key]
  (str "ataru:cache:lock:" name ":" key))

(defn- redis-scan
  [redis name cursor]
  (wcar (:connection-opts redis)
        (car/scan cursor :match (->cache-key name "*"))))

(defn- redis-get
  [cache key]
  (let [cache-key   (->cache-key (:name cache) key)
        [value ttl] (wcar (:connection-opts (:redis cache)) :as-pipeline
                          (car/get cache-key)
                          (car/pttl cache-key))]
    (when (some? value)
      (if-let [e (cache/check-schema (:loader cache) value)]
        (do (log/warn e "Schema check for" key "failed")
            nil)
        [value ttl]))))

(defn- redis-mget
  [cache keys]
  (reduce (fn [acc keys]
            (let [cache-keys (map #(->cache-key (:name cache) %) keys)
                  result     (wcar (:connection-opts (:redis cache)) :as-pipeline
                                   (apply car/mget cache-keys)
                                   (doseq [cache-key cache-keys]
                                     (car/pttl cache-key)))]
              (reduce (fn [acc [key value ttl-left]]
                        (if (some? value)
                          (if-let [e (cache/check-schema (:loader cache) value)]
                            (do (log/warn e "Schema check for" key "failed")
                                acc)
                            (-> acc
                                (assoc-in [:values key] value)
                                (assoc-in [:ttl-left key] ttl-left)))
                          acc))
                      acc
                      (map vector keys (first result) (rest result)))))
          {:values   {}
           :ttl-left {}}
          (partition 5000 5000 nil keys)))

(defn- add-to-queue
  [cache queue-name key]
  (wcar (:connection-opts (:redis cache))
        (car/zadd queue-name :nx (System/currentTimeMillis) key)))

(defn- acquire-locks
  [cache keys]
  (reduce (fn [locks key]
            (let [lock-key (->lock-key (:name cache) key)]
              (try
                (if-let [lock-id (car-locks/acquire-lock
                                  (:connection-opts (:redis cache))
                                  lock-key
                                  (:lock-timeout-ms cache)
                                  5)]
                  (assoc locks key lock-id)
                  locks)
                (catch Exception e
                  (log/error e "Failed to acquire lock" lock-key)
                  locks))))
          {}
          keys))

(defn- release-locks
  [cache locks]
  (doseq [[key lock-id] locks
          :let          [lock-key (->lock-key (:name cache) key)]]
    (try
      (when-not (car-locks/release-lock
                 (:connection-opts (:redis cache))
                 lock-key
                 lock-id)
        (log/warn "Failed to release lock" lock-key lock-id))
      (catch Exception e
        (log/error e "Failed to release lock" lock-key lock-id)))))

(defn- pop-keys
  [cache queue-name]
  (->> (wcar (:connection-opts (:redis cache))
             (car/zpopmin queue-name
                          (cache/load-many-size (:loader cache))))
       (partition 1 2)
       (apply concat)))

(defn- update-keys
  [cache keys]
  (let [locks (acquire-locks cache keys)]
    (try
      (when-let [locked-keys (seq (filter #(contains? locks %) keys))]
        (let [values (cache/load-many (:loader cache) locked-keys)]
          (wcar (:connection-opts (:redis cache))
                (doseq [key locked-keys]
                  (if (contains? values key)
                    (car/set (->cache-key (:name cache) key) (get values key) :px (:ttl-ms cache))
                    (car/del (->cache-key (:name cache) key)))
                  (car/publish (->notify-key (:name cache) key) (get values key))))))
      (finally (release-locks cache locks)))))

(defn- enqueue-update-execution
  [cache]
  (.execute
   (:executor cache)
   (fn []
     (when (try
             (when-let [keys (or (seq (pop-keys cache (->high-priority-queue (:name cache))))
                                 (seq (pop-keys cache (->low-priority-queue (:name cache)))))]
               (update-keys cache keys)
               true)
             (catch Exception e
               (log/error e "Failed to update" (:name cache) "keys")
               true))
       (recur)))))

(defn- fetch-as-high-priority
  [cache keys]
  (let [promises (reduce #(assoc %1 %2 (promise)) {} keys)
        listener (car/with-new-pubsub-listener (:spec (:connection-opts (:redis cache)))
                   {(->notify-pattern (:name cache))
                    (fn [[_ _ notify-key value]]
                      (when-let [key (notify-key->key (:name cache) notify-key)]
                        (when-let [p (get promises key)]
                          (deliver p [value]))))}
                   (car/psubscribe (->notify-pattern (:name cache))))]
    (try
      (doseq [key keys]
        (add-to-queue cache (->high-priority-queue (:name cache)) key))

      (enqueue-update-execution cache)

      (reduce (fn [values [key promise]]
                (if-let [[value] (deref promise (:lock-timeout-ms cache) nil)]
                  (if (some? value)
                    (assoc values key value)
                    values)
                  (throw
                   (new RuntimeException
                        (str "Failed to fetch " (:name cache) " " key)))))
              {}
              promises)
      (finally
        (car/with-open-listener listener
          (car/punsubscribe (->notify-pattern (:name cache))))
        (car/close-listener listener)))))

(defn- update-as-low-priority
  [cache keys]
  (doseq [key keys]
    (add-to-queue cache (->low-priority-queue (:name cache)) key))
  (enqueue-update-execution cache))

(defrecord Cache [redis
                  loader
                  name
                  ttl
                  refresh-after
                  lock-timeout
                  ttl-ms
                  refresh-after-ms
                  lock-timeout-ms
                  executor]
  component/Lifecycle

  (start [this]
    (if (nil? ttl-ms)
      (assoc this
             :ttl-ms (.toMillis (second ttl) (first ttl))
             :refresh-after-ms (when-let [[t unit] refresh-after]
                                 (.toMillis unit t))
             :lock-timeout-ms (.toMillis (second lock-timeout) (first lock-timeout))
             :executor (new ThreadPoolExecutor
                            1
                            (.availableProcessors (Runtime/getRuntime))
                            10 TimeUnit/SECONDS
                            (new ArrayBlockingQueue
                                 (.availableProcessors (Runtime/getRuntime)))
                            (new ThreadPoolExecutor$DiscardPolicy)))
      this))
  (stop [this]
    (when executor
      (.shutdown executor))
    (assoc this
           :ttl-ms nil
           :refresh-after-ms nil
           :lock-timeout-ms nil
           :executor nil))

  cache/Stats
  (stats [_]
    (let [[high-count low-count] (wcar (:connection-opts redis) :as-pipeline
                                       (car/zcount (->high-priority-queue name) "-inf" "+inf")
                                       (car/zcount (->low-priority-queue name) "-inf" "+inf"))]
      {:high-priority-queue-length high-count
       :low-priority-queue-length  low-count
       :executor-pool-size         (.getPoolSize executor)
       :executor-queue-length      (.size (.getQueue executor))}))

  cache/Cache

  (get-from [this key]
    (if-let [[value ttl-left] (redis-get this key)]
      (do (when (and (some? refresh-after-ms)
                     (< refresh-after-ms (- ttl-ms ttl-left)))
            (update-as-low-priority this [key]))
          value)
      (get (fetch-as-high-priority this [key]) key)))

  (get-many-from [this keys]
    (let [result    (redis-mget this keys)
          not-found (remove #(contains? (:values result) %) keys)]
      (when (some? refresh-after-ms)
        (->> keys
             (filter #(< refresh-after-ms (- ttl-ms (get-in result [:ttl-left %] ttl-ms))))
             (update-as-low-priority this)))
      (if (empty? not-found)
        (:values result)
        (merge (:values result)
               (fetch-as-high-priority this not-found)))))

  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (->cache-key name key))))

  (clear-all [_]
    (loop [[cursor keys] (redis-scan redis name 0)]
      (wcar (:connection-opts redis)
            (doseq [key keys] (car/del key)))
      (when (not= "0" cursor)
        (recur (redis-scan redis name cursor))))))
