(ns core
  (:use
   [penumbra opengl]
   [util]
   [pallet thread-expr]
   [penumbra.opengl.core :only [gl-import]]
   [cantor]
   [clojure.contrib [io :only [file]]]
   [matchure :only [cond-match]])
  (:require
   [camera :as cam]
   [penumbra.app :as app]
   [penumbra.text :as text])
  (:require [bsp :as bsp]))

(def light-ambient [0.5 0.5 0.5 1])
(def light-diffuse [1 1 1 1])
(def light-position [0 0 -150 1])

(gl-import glClearDepth clear-depth)

(defn post-process [bsp]
  (let [{:keys [faces vertexes textures] :as es} (:entries bsp)

        strip-nulls (fn [s] (apply str (filter #(not= (char 0) %) s)))

        alter-texture (fn-st (update-in [:name] strip-nulls))

        exists? (fn [path] (.exists (file path)))

        find-texture (fn [name]
                       (let [prefix (str "res/quarea51/" name)
                             search-paths (map str (repeat prefix) [".jpg" ".tga"])
                             valid-paths (filter exists? search-paths)]
                         (when-first [v valid-paths] v)))

        load-texture (fn [desc]
                       #_ (-> desc 
                           (when-let-> [path (find-texture (:name desc))]
                                       (assoc :data (load-texture-from-file path))))
                       (try
                         (assoc desc :data (load-texture-from-file
                                            (find-texture (:name desc))))
                         (catch Exception e
                           (println desc)
                           desc)))
        textures* (doall (->> textures (map alter-texture) (map load-texture)))

        normalize-rgba (fn [rgba] (map #(/ (float %) 255.0) rgba))
        alter-vertex (fn-st (update-in [:color] normalize-rgba))
        vertexes* (map alter-vertex vertexes)

        lookup-vertex (fn [v] (take (:count v) (drop (:index v) vertexes*)))
        lookup-texture (fn [idx] (nth textures* idx))
        alter-face (fn-st
                    (update-in [:texture] lookup-texture)
                    (update-in [:vertex] lookup-vertex))
        faces* (map alter-face faces)]

    (-> es
        (assoc :textures textures*)
        (assoc :vertexes vertexes*)
        (assoc :faces faces*))))

(defnl simple-state []
  {:mouse  (vec2 -391.0 -87.0)  ;(vec2 90 -90) 
   :camera cam/init})

(defnl heavy-state []
  (let [bsp (post-process (bsp/parse "res/quarea51/maps/quarea51.bsp"))]

    (-> (simple-state)
        (assoc :bsp bsp)
        (assoc :map (create-display-list
                     (doseq [f (:faces bsp)]
                       (when (= :polygon (:type f))
                         (with-texture (:data (:texture f))
                           (draw-triangle-fan
                            (doseq [v (:vertex f)]
                              (apply texture (:surface-uv v))
                              (apply color (:color v))
                              (apply vertex (:pos v))))))))))))
(defnl settings []
  (app/title! "Quake 3 Map Renderer")
  (app/vsync! false)

  (clear-depth 1)
  (enable :depth-test)
  (depth-test :lequal)

  (clear-color 0 0 0 0.5)
  (enable :texture-2d)
  (shade-model :smooth)
  (hint :perspective-correction-hint :nicest)

   (light 1
         :ambient light-ambient
         :diffuse light-diffuse
         :position light-position)
   (enable :light1)
   (enable :lighting)

  (app/key-repeat! true)
  )

(defnl init [st]
  (settings)
  (heavy-state))

(defn reshape [[x y width height] st]
  (frustum-view 60.0 (/ (double width) height) 1.0 100000.0)
  (load-identity)
  st)

(defn mouse-drag [[dx dy] _ _ st]
  (-> st
      (update-in [:mouse] #(add % (vec2 dx dy)))))


(defnl key-press [key {:keys [mouse vel] :as st}]
  (condp = key
      "r" (init {})
      "R" (do (settings) (merge st  (simple-state))) 
      "w" (update-in st [:camera] #(cam/moving % 1.0))
      "s" (update-in st [:camera] #(cam/moving % -1.0))
      st))

(defnl key-release [key {:keys [mouse vel] :as st}]
  (condp = key
          "w" (update-in st [:camera] #(cam/moving % 0.0))
          "s" (update-in st [:camera] #(cam/moving % 0.0))
          st))

(defnl key-type [key {:keys [mouse camera] :as st}]
  ;(println key " mouse=> "  mouse)
  ;(println key " camera=> "  camera)
  st)

(def cur-st (atom {})) ;; DEBUG

  
(defnl update [[dt time] {:keys [vel mouse] :as st}]
  (swap! cur-st (fn [_] st)) ;; DEBUG
  (-> st
      (update-in [:camera] #(cam/update % dt mouse))))

(defnl display [[dt time] st]
  (birds-eye-view)
  (draw-ui)
  (call-display-list (:map st))
  (app/repaint!)

  :where
  [draw-ui #(text/write-to-screen (format "%d FPS" (int (/ 1 dt))) 3 1)
   birds-eye-view (fn []
                    (rotate (.y (:mouse st)) 1 0 0)
                    (rotate (.x (:mouse st)) 0 0 1)
                    (apply translate (:pos (:camera st)))
                    )])

(defn -main []
  (app/start 
   (map-vals
    wrap-exc
    {:display #'display, :reshape #'reshape, :init #'init,
     :update #'update :key-press #'key-press
     :key-release #'key-release :key-type #'key-type :mouse-drag #'mouse-drag})
   {}))

#_ (-main)
