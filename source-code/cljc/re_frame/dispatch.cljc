
(ns re-frame.dispatch
    (:require [re-frame.core          :as core]
              [re-frame.event-handler :as event-handler]
              [re-frame.event-vector  :as event-vector]
              [re-frame.metamorphic   :as metamorphic]
              [re-frame.registrar     :as registrar]
              [re-frame.state         :as state]
              [time.api               :as time]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(core/reg-event-fx :dispatch-metamorphic-event
  ; @param (metamorphic-event) n
  ;
  ; @usage
  ; [:dispatch-metamorphic-event [...]]
  ;
  ; @usage
  ; [:dispatch-metamorphic-event {:dispatch [...]}]
  (fn [_ [_ n]] (metamorphic/metamorphic-event->effects-map n)))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn dispatch
  ; @param (metamorphic-event) event-handler
  ;
  ; @usage
  ; (dispatch [:my-event])
  ;
  ; @usage
  ; (dispatch {:dispatch [:my-event]})
  ;
  ; @usage
  ; (dispatch (fn [] {:dispatch [:my-event]}))
  ;
  ; @usage
  ; (dispatch nil)
  [event-handler]
  ; Szerver-oldalon a Re-Frame nem jelez hibát, nem regisztrált esemény meghívásakor.
  ; A szerver-oldalon nem történnek meg a nem regisztrált Re-Frame események, ezért nem lehetséges
  ; interceptor-ban vizsgálni az események regisztráltságát.
  (letfn [(check! [] (let [event-id      (event-vector/event-vector->event-id      event-handler)
                           event-exists? (event-handler/event-handler-registered? :event event-id)]
                          (when-not event-exists? (println "re-frame: no :event handler registered for:" event-id))))]
         (if (vector? event-handler) #?(:clj (check!) :cljs nil))
         (if (vector? event-handler)         (core/dispatch event-handler)
                                             (core/dispatch [:dispatch-metamorphic-event event-handler]))))

; @usage
;  {:dispatch ...}
(registrar/clear-handlers :fx      :dispatch)
(core/reg-fx              :dispatch dispatch)

(defn dispatch-fx
  ; @param (event-vector) event-handler
  ;
  ; @usage
  ; (dispatch-fx [:my-side-effect-event ...])
  [event-handler]
  (dispatch {:fx event-handler}))

(defn dispatch-sync
  ; @param (event-vector) event-handler
  ;
  ; @usage
  ; (dispatch-sync [...])
  ;
  [event-handler]
  ; A dispatch-sync függvény a meghívási sebesség fontossága miatt nem kezeli
  ; a metamorphic-event kezelőket!
  (core/dispatch-sync event-handler))

(defn dispatch-n
  ; @param (metamorphic-events in vector) event-list
  ;
  ; @usage
  ; (dispatch-n [[:event-a]
  ;              {:dispatch [:event-b]}
  ;              (fn [_ _] {:dispatch [:event-c]})])
  [event-list]
  (doseq [event (remove nil? event-list)]
         (dispatch event)))

; @usage
;  {:dispatch-n [[...] [...]}
(registrar/clear-handlers :fx        :dispatch-n)
(core/reg-fx              :dispatch-n dispatch-n)

(defn dispatch-later
  ; @param (maps in vector) effects-map-list
  ;
  ; @usage
  ; (dispatch-later [{:ms 500 :dispatch [...]}
  ;                  {:ms 600 :fx [...]
  ;                           :fx-n       [[...] [...]]
  ;                           :dispatch-n [[...] [...]]}])
  [effects-map-list]
  ; Az eredeti dispatch-later függvény clojure környezetben nem időzíti a dispatch-later eseményeket!
  (doseq [{:keys [ms] :as effects-map} (remove nil? effects-map-list)]
         (if ms (letfn [(f [] (dispatch (dissoc effects-map :ms)))]
                       (time/set-timeout! f ms)))))

; @usage
;  {:dispatch-later [{...} {...}]}
(registrar/clear-handlers :fx            :dispatch-later)
(core/reg-fx              :dispatch-later dispatch-later)

;; -- Low sample-rate dispatch functions --------------------------------------
;; ----------------------------------------------------------------------------

(defn- reg-event-lock
  ; @param (integer) timeout
  ; @param (keyword) event-id
  ;
  ; @return (?)
  [timeout event-id]
  (let [elapsed-time (time/elapsed)
        unlock-time  (+ timeout elapsed-time)]
       (swap! state/EVENT-LOCKS assoc event-id unlock-time)))

(defn- event-unlocked?
  ; @param (keyword) event-id
  ;
  ; @return (boolean)
  [event-id]
  (let [elapsed-time (time/elapsed)
        unlock-time  (get @state/EVENT-LOCKS event-id)]
       (> elapsed-time unlock-time)))

(defn- dispatch-unlocked?!
  ; Dispatch event if it is NOT locked
  ;
  ; @param (event-vector) event-vector
  ;
  ; @return (?)
  [event-vector]
  (let [event-id (event-vector/event-vector->event-id event-vector)]
       (if (event-unlocked? event-id)
           (core/dispatch   event-vector))))

(defn- delayed-try
  ; @param (integer) timeout
  ; @param (event-vector) event-vector
  ;
  ; @return (?)
  [timeout event-vector]
  (let [event-id (event-vector/event-vector->event-id event-vector)]
       (if (event-unlocked? event-id)
           (do (core/dispatch  event-vector)
               (reg-event-lock timeout event-id)))))

(defn dispatch-last
  ; @Warning
  ; The 'dispatch-last' function only handles standard event vectors, because
  ; the metamorphic events don't have unique identifiers!
  ;
  ; @description
  ; The 'dispatch-last' function only fires an event if you stop calling it
  ; at least for the given timeout.
  ; It ignores dispatching the event until the timout elapsed since the last calling.
  ;
  ; @param (integer) timeout
  ; @param (event-vector) event-vector
  ;
  ; @usage
  ; (dispatch-last 500 [:my-event])
  ;
  ; @return (?)
  [timeout event-vector]
  (let [event-id (event-vector/event-vector->event-id event-vector)]
       (reg-event-lock    timeout event-id)
       (letfn [(f [] (dispatch-unlocked?! event-vector))]
              (time/set-timeout! f timeout))))

(defn dispatch-once
  ; @Warning
  ; The 'dispatch-once' function only handles standard event vectors, because
  ; the metamorphic events don't have unique identifiers!
  ;
  ; @description
  ; The 'dispatch-once' function only fires an event once in the given interval.
  ; It ignores dispatching the event except one time per interval.
  ;
  ; @param (integer) interval
  ; @param (event-vector) event-vector
  ;
  ; @usage
  ; (dispatch-once 500 [:my-event])
  ;
  ; @return (?)
  [interval event-vector]
  (let [event-id (event-vector/event-vector->event-id event-vector)]
       (if (event-unlocked? event-id)
           (do (core/dispatch  event-vector)
               (reg-event-lock interval event-id))
           (letfn [(f [] (delayed-try interval event-vector))]
                  (time/set-timeout! f interval)))))
