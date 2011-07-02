(defproject bsp "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]

                 [matchure "0.10.1"]
                 [org.cloudhoist/thread-expr "1.1.1-SNAPSHOT"]

                 ;[gloss "0.2.0-alpha2-SNAPSHOT"]
                 [gloss-b "0.0.1"]
                 [penumbra "0.6.0-SNAPSHOT"]]

  :native-dependencies [[penumbra/lwjgl "2.4.2"]]

  :dev-dependencies [[native-deps "1.0.5"]
                     [swank-clojure "1.2.1"]]
    :main core)

