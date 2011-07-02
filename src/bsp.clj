(ns bsp
  (:use [gloss core io]
        [clojure.contrib
         [io :only [file]]
         [duck-streams :only [to-byte-array]]]
        [clojure walk]))

(defn c-str [len] (string :US-ASCII :length len :as-str true))

(def c-lump (ordered-map :offset :uint32
                         :length :uint32))
(def c-header
     (ordered-map :magic (c-str 4)
                  :version :uint32
                  :lumps (repeat 17 c-lump)))

(def c-vec2f [:float32 :float32])
(def c-vec2i [:int32 :int32])
(def c-vec3f [:float32 :float32 :float32])
(def c-rgba [:ubyte :ubyte :ubyte :ubyte])

(def c-index-count (ordered-map :index :int32 :count :int32))
(def c-rect (ordered-map :top-left c-vec2i :dims c-vec2i))


(def c-texture (ordered-map :name (c-str 64)
                            :flags :int32
                            :contents :int32))

(def c-vertex (ordered-map :pos c-vec3f
                           :surface-uv c-vec2f
                           :lightmap-uv c-vec2f
                           :normal c-vec3f
                           :color c-rgba))

(def c-face-lightmap
     (ordered-map :index :int32
                  :image-rect c-rect
                  :origin c-vec3f
                  :s c-vec3f
                  :t c-vec3f))

(def c-face
     (let [c-type (enum :int32 {:polygon 1 :patch 2 :mesh 3 :billboard 4})]
       (ordered-map :texture :int32
                    :effect :int32
                    :type c-type
                    :vertex c-index-count
                    :meshvert c-index-count
                    :lightmap c-face-lightmap
                    :normal c-vec3f
                    :patch-dims c-vec2i)))

(def lump-id->codec
     {1 [:textures (repeated c-texture)]
      10 [:vertexes (repeated c-vertex)]
      13 [:faces (repeated c-face)]})


(def c-bsp
     (let [mk-entry-desc
           (fn [idx lump-desc]
             (if-let [[kw codec] (lump-id->codec idx)]
               {kw (assoc lump-desc :codec codec)}))

           header->entries-desc
           (fn [{:keys [lumps]}]
             (->> lumps
                  (map-indexed mk-entry-desc)
                  (filter #(not (nil? %))) (apply merge)))]

       (directory c-header header->entries-desc)))


(defn parse [path]
  (->> (file path) (to-byte-array) (to-byte-buffer) (decode c-bsp)))


