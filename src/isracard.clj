(ns isracard
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn- handle-errors [{{:keys [Header] :as body} :body, :as response}]
  (if (= (:Status Header) "-2")
    (throw (ex-info (:Message Header) body))
    response))

(def ^:private request (comp handle-errors http/request))

(defn- base-request [reqName]
  {:url "https://digital.isracard.co.il/services/ProxyRequestHandler.ashx"
   :method :get
   :query-params {:reqName reqName}
   :as :json})

(defn authenticate [{:keys [MisparZihuy Sisma cardSuffix]}]
  (let [{:keys [body]
         :as res} (-> (base-request "performLogonI")
                      (assoc :method :post
                             :query-params {:reqName "performLogonI"}
                             :content-type :application/x-www-form-urlencoded
                             :body (json/generate-string
                                    {:MisparZihuy MisparZihuy
                                     :Sisma Sisma
                                     :cardSuffix cardSuffix}))
                      http/request)]
    (if-let [message (:message body)]
      (throw (ex-info message body))
      res)))

(defn cards-list []
  (-> (base-request "CardsList_102Digital")
      (merge (select-keys auth-res [:cookies]))
      request))

(defn card-transactions [params]
  (-> (base-request "CardsTransactionsList")
      (merge (select-keys auth-res [:cookies]))
      (update :query-params merge params)
      request))

(comment
  (def auth-res (authenticate {:MisparZihuy ""
                               :Sisma ""
                               :cardSuffix ""}))
  (cards-list)
  (card-transactions {:year 2019 :month "02"}))
