
(ns re-frame.sub
    (:require [re-frame.core :as core]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

; re-frame.core
(def subscribe core/subscribe)

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn subscribed
  ; @param (subscription-vector) subscriber
  ;
  ; @usage
  ;  (subscribed [:my-subscription])
  ;
  ; @return (*)
  [subscriber]
  (-> subscriber subscribe deref))
