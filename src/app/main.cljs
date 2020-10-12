
(ns app.main (:require [cirru-edn.core :as cirru-edn] ["fs" :as fs]))

(defn replace-node [node]
  (case (:type node)
    :leaf (dissoc node :id)
    :expr
      (do
       (if (contains? node :author) (js/console.warn "find outdated field author"))
       (if (contains? node :timestamp) (js/console.warn "find outdated field timestamp"))
       (-> node
           (dissoc :id)
           (update
            :data
            (fn [children] (->> children (map (fn [[k v]] [k (replace-node v)])) (into {}))))))
    (throw (js/Error. (str "Unexpected node: " (pr-str node))))))

(defn replace-file [file-info]
  (-> file-info
      (update :ns replace-node)
      (update
       :defs
       (fn [defs] (->> defs (map (fn [[k v]] [k (replace-node v)])) (into {}))))
      (update :proc replace-node)))

(defn main! []
  (println "Started.")
  (let [content (fs/readFileSync "calcit.cirru" "utf8")
        data (cirru-edn/parse content)
        new-data (-> data
                     (update-in
                      [:ir :files]
                      (fn [files]
                        (->> files (map (fn [[k v]] [k (replace-file v)])) (into {})))))]
    (fs/writeFileSync "calcit.cirru" (cirru-edn/write new-data)))
  (println "File wrote!"))

(defn reload! [] (.clear js/console) (println "Reloaded, rerunning main!") (main!))
