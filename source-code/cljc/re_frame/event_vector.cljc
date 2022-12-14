
(ns re-frame.event-vector
    (:require [candy.api  :refer [return]]
              [vector.api :as vector]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn event-vector->event-id
  ; @param (vector) event-vector
  ;
  ; @example
  ; (event-vector->event-id [:my-event ...])
  ; =>
  ; :my-event
  ;
  ; @return (vector)
  [event-vector]
  (first event-vector))

(defn event-vector->effects-map
  ; @param (vector) event-vector
  ;
  ; @example
  ; (event-vector->effects-map [...])
  ; =>
  ; {:dispatch [...]}
  ;
  ; @return (map)
  [event-vector]
  {:dispatch event-vector})

(defn event-vector->handler-f
  ; @param (vector) event-vector
  ;
  ; @example
  ; (event-vector->handler-f [...])
  ; =>
  ; (fn [_ _] {:dispatch [...]})
  ;
  ; @return (function)
  [event-vector]
  (fn [_ _] {:dispatch event-vector}))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn event-vector<-params
  ; @param (event-vector) n
  ; @param (list of *) params
  ;
  ; @example
  ; (event-vector<-params [:my-event] "My param" "Your param")
  ; =>
  ; [:my-event "My param" "Your param"]
  ;
  ; @return (event-vector)
  [n & params]
  (vector/concat-items n params))
