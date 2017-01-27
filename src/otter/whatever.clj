(ns otter.whatever
  (:gen-class)
  (:require [clojure.pprint :refer [pprint]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.core.async :refer [<!! <! >! alts!! timeout chan go go-loop close!]])
  (:import [java.util.concurrent TimeoutException TimeUnit FutureTask]))

(def hi (future
          (println "in future")
          (loop [seconds 1]
            (if (> seconds 10)
              (println "done")
              (do
                (<!! (timeout 1000))
                (println "waited" seconds "seconds")
                (recur (inc seconds)))))
          (println "out future")))

         (let [f-a (future
                     (loop [seconds 1]
                       (if (> seconds 10)
                         (println "done")
                         (do
                           (<!! (timeout 1000))
                           (println "(a waited" seconds "seconds)")
                           (recur (inc seconds))))))
               f-b (future
                     (loop [seconds 1]
                       (if (> seconds 5)
                         (println "done")
                         (do
                           (<!! (timeout 1000))
                           (println "(b waited" seconds "seconds)")
                           (recur (inc seconds))))))])

(defn cancellable
  [f]
  (let [killed? (atom false)]
    (future
      (try
        (let [x (future (f))]
          (loop [s 1]
            (Thread/sleep 1000)
            (println "hey!" @killed?)
            (if @killed?
              (do (println "cancelling..")
                  (.cancel x true)
                  (println "cancelled")) 
              (recur (inc s)))))
        (catch Exception e (println e))))
    killed?))

(def x (cancellable
        (future
          (loop [seconds 1]
            (if (> seconds 10)
              (println "done")
              (do
                (<!! (timeout 1000))
                (println "waited" seconds "seconds")
                (recur (inc seconds))))))))

(def f       (future
              (loop [seconds 1]
                (if (> seconds 10)
                  (println "done")
                  (do
                    (<!! (timeout 1000))
                    (println "waited" seconds "seconds")
                    (recur (inc seconds)))))))


(reset! x true)



           
(defn race-guy
  [interval f-a f-b]
  (go
    
    (loop [seconds 1]
      (Thread/sleep 1000)
      (match [(.isDone f-a) (.isDone f-b)]
        [true false] (do (.cancel f-b true) :a)
        [false true] (do (.cancel f-a true) :b)
        [true true] :both
        [false false] (do (println "<< Recur"seconds ">>")
                          (recur (inc seconds))))))))

(<!! (go
       (let [f-a (future
                  (loop [seconds 1]
                    (if (> seconds 10)
                      (println "done")
                      (do
                        (<!! (timeout 1000))
                        (println "(a waited" seconds "seconds)")
                        (recur (inc seconds))))))
            f-b (future
                  (loop [seconds 1]
                    (if (> seconds 5)
                      (println "done")
                      (do
                        (<!! (timeout 1000))
                        (println "(b waited" seconds "seconds)")
                        (recur (inc seconds))))))]
        (loop [seconds 1]
          (Thread/sleep 1000)
          (match [(.isDone f-a) (.isDone f-b)]
            [true false] (do (.cancel f-b true) :a) 
            [false true] (do (.cancel f-a true) :b)
            [true true] :both
            [false false] (do (println "<< Recur"seconds ">>")
                              (recur (inc seconds))))))))

(.cancel hi true)

(comment
  (def c (go-loop [seconds 1]
           (if (> seconds 10)
             (println "done")
             (do
               (<! (timeout 1000))
               (println "waited" seconds "seconds")
               (recur (inc seconds))))))

  (close! c)
  
  (<!! (go-loop [seconds 1]
         (if (> seconds 10)
           (println "done")
           (do
             (<! (timeout 1000))
             (println "waited" seconds "seconds")
             (recur (inc seconds))))))

  
  
  )



;; http://stackoverflow.com/questions/6694530/executing-a-function-with-a-timeout
(def ^{:doc "Create a map of pretty keywords to ugly TimeUnits"}
  uglify-time-unit
  (into {} (for [[enum aliases] {TimeUnit/NANOSECONDS [:ns :nanoseconds]
                                 TimeUnit/MICROSECONDS [:us :microseconds]
                                 TimeUnit/MILLISECONDS [:ms :milliseconds]
                                 TimeUnit/SECONDS [:s :sec :seconds]}
                 alias aliases]
             {alias enum})))

(defn thunk-timeout
  "Takes a function and an amount of time to wait for thse function to finish
   executing. The sandbox can do this for you. unit is any of :ns, :us, :ms,
   or :s which correspond to TimeUnit/NANOSECONDS, MICROSECONDS, MILLISECONDS,
   and SECONDS respectively."
  ([thunk ms]
     (thunk-timeout thunk ms :ms nil)) ; Default to milliseconds, because that's pretty common.
  ([thunk time unit]
     (thunk-timeout thunk time unit nil))
  ([thunk time unit tg]
     (let [task (FutureTask. thunk)
           thr (if tg (Thread. tg task) (Thread. task))]
       (try
         (.start thr)
         {:status :ok
          :body (.get task time (or (uglify-time-unit unit) unit))}
         (catch TimeoutException e
           (.cancel task true)
           (.stop thr)
           {:status :timeout :time time :unit unit})
         (catch Exception e
           (.cancel task true)
           (.stop thr)
           (throw e))
         (finally (when tg (.stop tg)))))))



(defmacro with-timeout [time & body]
  `(thunk-timeout (fn [] ~@body) ~time))

(with-timeout
  2000
  (Thread/sleep 10000)
)

(defn run-fixture
  [fixture scenarios]
  (letfn [(run-scenario [scenario]
            (let [out (new java.io.StringWriter)]
              (binding [*out* out]
                (let [output (fixture scenario)]
                  {:scenario scenario
                   :output output
                   :log (str out)}))))]
    (pmap run-scenario scenarios)))

(let [fixture (fn [scenario]
                (println "Sleeping for" scenario)
                (Thread/sleep scenario)
                (println "Done at" (f/unparse (f/formatters :hour-minute-second-ms) (t/now)))
                true)]
  (run-fixture fixture (repeatedly 5 #(rand-int 3000))))
;; (f/show-formatters)




(defn -main
  [& args]
  (log/info "*******************")
  (log/info "Starting Allegra...")
  (log/info "*******************")
  (try
    (println "howdy")
    (catch Exception e
      (log/error "Processing failed.")
      (log/error e))))
