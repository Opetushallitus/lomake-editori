(ns ataru.application-common.components.button-component
  (:require [schema.core :as s]))

(s/defschema AriaAttributes
  {:aria-haspopup                  (s/eq "listbox")
   :aria-labelledby                s/Str
   (s/optional-key :aria-expanded) (s/eq true)})

(s/defn button
  [{:keys [label
           on-click
           aria-attrs
           data-test-id]} :- {:label                         s/Str
                              :on-click                      s/Any
                              (s/optional-key :aria-attrs)   AriaAttributes
                              (s/optional-key :data-test-id) s/Str}]
  [:button.a-button
   (cond-> {:type         "button"
            :on-click     on-click
            :data-test-id data-test-id}
           (seq aria-attrs)
           (merge aria-attrs))
   [:span
    {:id (:aria-labelledby aria-attrs)}
    label]])
