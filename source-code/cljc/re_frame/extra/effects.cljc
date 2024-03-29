
(ns re-frame.extra.effects
    (:require [fruits.vector.api  :as vector]
              [re-frame.core      :as core]
              [re-frame.tools.api :as re-frame.tools]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(core/reg-event-fx :dispatch-metamorphic-event
  ; @param (metamorphic-event) metamorphic-event
  ;
  ; @usage
  ; [:dispatch-metamorphic-event [:my-event]]
  ;
  ; @usage
  ; [:dispatch-metamorphic-event {:dispatch [:my-event]}]
  (fn [_ [_ metamorphic-event]] (re-frame.tools/metamorphic-event->effect-map metamorphic-event)))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(core/reg-event-fx :dispatch-tick
  ; @param (maps in vector) effect-map-list
  ; [(map) effect-map
  ;   {:dispatch (metamorphic-event)(opt)
  ;    :dispatch-f (function)(opt)
  ;    :dispatch-later (maps in vector)(opt)
  ;    :dispatch-n (metamorphic-events in vector)(opt)
  ;    :dispatch-once (map)(opt)
  ;    :dispatch-tick (maps in vector)(opt)
  ;    :fx (vector)(opt)
  ;    :fx-n (vectors in vector)(opt)
  ;    :tick (integer
  ;    ...}]
  ;
  ; @usage
  ; [:dispatch-tick [{:tick 420 :dispatch [:my-event]}]]
  (fn [_ [_ effect-map-list]]
      (letfn [(f0 [merged-effect-map effect-map]
                  (cond (-> effect-map :tick nil?)                                                                       ; <- No 'tick' value is provided.
                        (-> merged-effect-map (re-frame.tools/merge-effect-maps effect-map))                             ; <- Dispatching without delay.
                        (-> effect-map :tick zero?)                                                                      ; <- Delay is elapsed.
                        (-> merged-effect-map (re-frame.tools/merge-effect-maps effect-map))                             ; <- Dispatching now.
                        (-> effect-map :tick integer?)                                                                   ; <- Delay hasn't elapsed.
                        (-> merged-effect-map (update :dispatch-tick vector/conj-item (update effect-map :tick dec)))))] ; <- Dispatching later.
             (reduce f0 {} effect-map-list))))
