
;; -- Namespace ---------------------------------------------------------------
;; ----------------------------------------------------------------------------

(ns re-frame.interceptors
    (:require [re-frame.core :as core]))



;; -- Redirects ---------------------------------------------------------------
;; ----------------------------------------------------------------------------

; re-frame.core
(def ->interceptor core/->interceptor)
