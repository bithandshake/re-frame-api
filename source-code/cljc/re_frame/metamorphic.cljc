
(ns re-frame.metamorphic
    (:require [candy.api             :refer [return]]
              [map.api               :as map]
              [re-frame.effects-map  :as effects-map]
              [re-frame.event-vector :as event-vector]
              [re-frame.types        :as types]
              [vector.api            :as vector]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn metamorphic-handler->handler-f
  ; @param (metamorphic-event) n
  ;
  ; @example
  ; (metamorphic-handler->handler-f [...])
  ; =>
  ; (fn [_ _] {:dispatch [...]})
  ;
  ; @example
  ; (metamorphic-handler->handler-f {:dispatch [...]})
  ; =>
  ; (fn [_ _] {:dispatch [...]})
  ;
  ; @example
  ; (metamorphic-handler->handler-f (fn [_ _] ...))
  ; =>
  ; (fn [_ _] ...})
  ;
  ; @return (function)
  [n]
  (cond (map?    n) (effects-map/effects-map->handler-f   n)
        (vector? n) (event-vector/event-vector->handler-f n)
        :return  n))

(defn metamorphic-event->effects-map
  ; @param (metamorphic-event) n
  ;
  ; @example
  ; (metamorphic-event->effects-map [:my-event])
  ; =>
  ; {:dispatch [:my-event]}
  ;
  ; @example
  ; (metamorphic-event->effects-map {:dispatch [:my-event])
  ; =>
  ; {:dispatch [:my-event]}
  ;
  ; @example
  ; (metamorphic-event->effects-map (fn [] {:dispatch [:my-event]))
  ; =>
  ; {:dispatch [:my-event]}
  ;
  ; @return (map)
  [n]
  (cond (vector? n) (event-vector/event-vector->effects-map n)
        (map?    n) (return                                 n)
        (fn?     n) (metamorphic-event->effects-map        (n))))

(defn metamorphic-event<-params
  ; @param (metamorphic-event) n
  ; @param (list of *) params
  ;
  ; @example
  ; (metamorphic-event<-params [:my-event] "My param" "Your param")
  ; =>
  ; [:my-event "My param" "Your param"]
  ;
  ; @example
  ; (metamorphic-event<-params {:dispatch [:my-event]} "My param" "Your param")
  ; =>
  ; {:dispatch [:my-event "My param" "Your param"]}
  ;
  ; @return (metamorphic-event)
  [n & params]
  ; A metamorphic event could be a vector ...
  ; ... as an event-vector:         [:my-event ...]
  ; ... as a dispatch-later vector: [{:ms   500 :dispatch [:my-event ...]}]
  ; ... as a dispatch-tick vector:  [{:tick 500 :dispatch [:my-event ...]}]
  (cond (types/event-vector? n) (vector/concat-items n params)
        (vector?             n) (vector/->items      n #(apply metamorphic-event<-params % params))
        (map?                n) (map/->values        n #(apply metamorphic-event<-params % params))
        :return              n))
