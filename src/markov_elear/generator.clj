(ns markov-elear.generator
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [overtone.at-at :as overtone]
            [twitter.api.restful :as twitter]
            [twitter.oauth :as twitter-oauth]
            [environ.core :refer [env]])
  (:gen-class))

(defn word-chain [word-transitions]
  (reduce (fn [r t] (merge-with set/union r
                               (let [[a b c] t]
                                 {[a b] (if c #{c} #{})})))
          {}
          word-transitions))

(defn walk-chain [prefix chain result]
  (let [suffixes (get chain prefix)]
    (if (or (> (count (apply str result)) 100) (empty? suffixes))
      result
      (let [n (rand-int (count suffixes))
            suffix (nth (seq suffixes) n)
            new-prefix [(last prefix) suffix]]
        (recur new-prefix chain (conj result suffix))))))


(defn text->word-chain [s]
  (let [words (clojure.string/split s #"[\s|\n]")
        word-transitions (partition-all 3 1 words)]
    (word-chain word-transitions)))

(defn generate-text [start-phrase word-chain]
  (let [prefix (clojure.string/split start-phrase #" ")]
    (apply str (interpose " "
                          (walk-chain prefix word-chain prefix)))))

(defn generate-text
  [start-phrase word-chain]
  (let [prefix (clojure.string/split start-phrase #" ")]
    (apply str (interpose " "
                          (walk-chain prefix word-chain prefix)))))

(defn process-file [fname]
  (text->word-chain
   (slurp (io/resource fname))))

(def files ["quangle-wangle.txt" "monad.txt" "clojure.txt" "functional.txt"
            "jumblies.txt" "pelican.txt" "pobble.txt"])
(def functional-leary (apply merge-with clojure.set/union (map process-file files)))

(def prefix-list ["On the" "They went" "And all" "We think"
                  "For every" "No other" "To a" "And every"
                  "We, too," "For his" "And the" "But the"
                  "Are the" "The Pobble" "For the" "When we"])

(def my-pool (overtone/mk-pool))

(defn tweet-text []
  (generate-text (-> prefix-list shuffle first) functional-leary))


(def my-creds (twitter-oauth/make-oauth-creds (env :app-consumer-key)
                                              (env :app-consumer-secret)
                                              (env :user-access-token)
                                              (env :user-access-secret)))

(defn status-update []
  (twitter/statuses-update :oauth-creds my-creds
                 :params {:status (tweet-text)}))

(defn -main [& args]
  ;; every 8 hours
  (println "Started up")
  (println (tweet-text))
  (overtone/every (* 1000 60 60 8) #(println (status-update)) my-pool))

