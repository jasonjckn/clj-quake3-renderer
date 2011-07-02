(ns camera
  (:use [cantor]))

(def init
     {;:pos (vec3 -597 -715 -15.0) 
      :pos (vec3 -355.77171588536413, -1319.3424286755305 -720.0146358813008 )
      :accel 0.0
      :vel 0.0})

(defn moving [st dir]
  (-> st (assoc :accel (* dir 400.0)) (assoc :vel (* dir 100.0))))

(defn rot-xyz-matrix [x y z]
  (reduce transform-matrix
          [;(rotation-matrix y 0 1 0)
           (rotation-matrix z 0 0 1)
           (rotation-matrix x 1 0 0)
           ]))

(defn camera-vec3 [mouse mag]
  (transform-vector
   (rot-xyz-matrix (- (.y mouse)) 0 (- (.x mouse)))
   (vec3 0 0 (- mag))))

(defn update [st dt mouse]
  (-> st
      (update-in [:vel] #(min 700.0 (+ % (* dt (:accel st)))))
      (update-in [:pos] #(sub % (camera-vec3 mouse (* dt (:vel st)))))
      ))

