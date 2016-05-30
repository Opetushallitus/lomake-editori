(ns ataru.schema.clj-schema
  (:require [ataru.schema :as schema]
            [schema.core :as s]
            [schema-tools.core :as st]
            [oph.soresu.form.schema :as soresu]))

;        __.,,------.._
;     ,'"   _      _   "`.
;    /.__, ._  -=- _ "`    Y
;   (.____.-.`      ""`   j
;    VvvvvvV`.Y,.    _.,-'       ,     ,     ,
;        Y    ||,   '"\         ,/    ,/    ./
;        |   ,'  ,     `-..,'_,'/___,'/   ,'/   ,
;   ..  ,;,,',-'"\,'  ,  .     '     ' ""' '--,/    .. ..
; ,'. `.`---'     `, /  , Y -=-    ,'   ,   ,. .`-..||_|| ..
;ff\\`. `._        /f ,'j j , ,' ,   , f ,  \=\ Y   || ||`||_..
;l` \` `.`."`-..,-' j  /./ /, , / , / /l \   \=\l   || `' || ||...
; `  `   `-._ `-.,-/ ,' /`"/-/-/-/-"'''"`.`.  `'.\--`'--..`'_`' || ,
;            "`-_,',  ,'  f    ,   /      `._    ``._     ,  `-.`'//         ,
;          ,-"'' _.,-'    l_,-'_,,'          "`-._ . "`. /|     `.'\ ,       |
;        ,',.,-'"          \=) ,`-.         ,    `-'._`.V |       \ // .. . /j
;        |f\\               `._ )-."`.     /|         `.| |        `.`-||-\\/
;        l` \`                 "`._   "`--' j          j' j          `-`---'
;         `  `                     "`_,-','/       ,-'"  /
;                                 ,'",__,-'       /,, ,-'
;                                 Vvv'            VVv'
; memoized function, runs only once -
; it overwrites some of soresus schemas with little changes
((memoize
   (fn []
     (intern 'oph.soresu.form.schema
             'LocalizedString
             {:fi           s/Str
              (s/optional-key :sv) s/Str
              (s/optional-key :en) s/Str})
     nil)))

(soresu/create-form-schema [] [] [])

(s/defschema Form schema/Form)

(s/defschema FormWithContent
  (merge Form
         {:content [(s/if (comp some? :children) soresu/WrapperElement soresu/FormField)]}))
