(defproject otter "0.1.1"
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [clj-http "3.0.1"]
                 [clj-time "0.11.0"]
                 [com.taoensso/timbre "4.3.1"]]
  :resource-paths ["resources" "test-resources"]
  :main otter.core
  :aot :all)
