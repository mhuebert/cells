(ns cells.cell-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [run!]])
  (:require [reagent.core :as r :refer [cursor]]
            [cells.compiler :as eval]
            [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [put! chan <! >!]]
            [reagent.ratom :refer [dispose!]]
            [cljs.reader :refer [read-string]]
            [cells.compiler]
            [cells.timing :refer [clear-intervals! run-cell!]]
            [cells.state :as state :refer [cells values]]))

(declare cell names alphabet-name number-name little-pony-names)

(defn little-pony-name []
  (-> little-pony-names shuffle first symbol))
(def new-name little-pony-name)

(defn make-id [id-candidate]
  (cond
    (number? id-candidate) (symbol (str state/number-prefix id-candidate))
    (nil? id-candidate) (new-name)
    (get @cells id-candidate) (new-name)
    :else id-candidate))

(def blank-cell {:source ""
                 :width 1
                 :height 1})

(defn new-cell
  ([] (new-cell (new-name) ""))
  ([source] (new-cell (new-name) source))
  ([id source]
   (go
     (let [id (make-id id)
           cell (if (map? source) source (merge blank-cell {:source source}))]
       (swap! cells assoc id (r/atom cell))
       (swap! state/cell-order conj id)
       (if-not (get values id) ;may already exist if another cell is listening
         (swap! values assoc id (r/atom nil)))
       (<! (run-cell! id))
       id))))

(defn kill-cell [id]
  (swap! state/cells dissoc id)
  (swap! state/cell-order #(remove #{id} %)))

(defn replace-cell [old new]
  (prn "replacing cell..."))

(def html #(with-meta % {:hiccup true}))

(defn value [id]
  @(get @values id))

(defn coerce-id [id]
  (if (number? id) (symbol (str state/number-prefix id)) id))

(defn get-val [id]
  (aget js/window "cljs" "user" (name id)))

(defn interval
  ([f] (interval f 500))
  ([n f]
   (let [id cljs.user/self-id
         exec #(binding [cljs.user/self (get-val id)]
                (reset! (get @values id) (f cljs.user/self))
                (eval/def-cell id))]
     (clear-intervals! id)
     (let [interval-id (js/setInterval exec (max 24 n))]
       (swap! state/index update-in [:interval-ids id] #(conj (or % []) interval-id)))
     (f (get-val id)))))


(defn get-json [url]
  (go
    (let [c (chan)]
      (xhr/send url
                #(let [text (-> % .-target .getResponseText)
                       res (try (-> text read-string :js)
                                (catch js/Error e (.log js/console e)))]
                  (put! c (js->clj res)))
                "GET"
                )
      c)))

; put get-json in user namespace, declare it, try it


#_(interval 500
          (fn []
            (.send goog.net.XhrIo "http://time.jsontest.com"
                   #(cell! 2 (first (vals (js->clj (.getResponseJson (.-target %)))))))))

#_(defn source!
    ([id val]
     (let [id (coerce-id id)]
       (reset! (get @cells id) (str val))
       nil)))

#_(defn value! [id val]
    (go (let [id (coerce-id id)]
          (reset! (get @values id) val)
          (<! (eval/def-cell id))
          val)))

(defn alphabet-name []
  (let [char-index-start 97
        char-index-end 123]
    (loop [i char-index-start
           repetitions 1]
      (let [letter (symbol (apply str (repeat repetitions (char i))))]
        (if-not (contains? @cells letter) letter
                                          (if (= i char-index-end)
                                            (recur char-index-start (inc repetitions))
                                            (recur (inc i) repetitions)))))))

(defn number-name []
  (inc (count @state/cells)))



(def little-pony-names (clojure.string/split "applejack\npinkie-pie\naloe\ncheerilee\ncherry-jubilee\ncoco-pommel\ngoldie-delicious\ngranny-smith\nthe-headless-horse\njunebug\nlotus-blossom\nmane\nmayor-mare\nnurse-redheart\nthe-olden-pony\nphoto-finish\nprim-hemline\nroma\nsuri-polomare\nteddie-safari\ntorch-song\ntree-hugger\nconductor\nall-aboard\nbig-mcintosh\nbraeburn\ncheese-sandwich\ndoc-top\ndouble-diamond\ngizmo\nhoity-toity\nrandolph\nearth-pony-royal-guards\nsheriff-silverstar\nsilver-shill\ntrain-conductor\nsteamer\ntoe\ntrouble-shoes\npinkie\ncloudy-quartz\npinkie\nmaud-pie\ngranny-smith\napple-bumpkin\napple-cider\napple-fritter\napple-honey\napple-leaves\naunt-orange\nauntie-applesauce\ncandy-apples\ncaramel-apple\nflorina-tart\ngala-appleby\ngranny-smith\nigneous-rock\njonagold\nlavender-fritter\nmagdalena\npeachy-sweet\npinkie\npokey-oaks\nred-gala\nstinkin\nsundowner\napple-bottoms\napple-cinnamon\napple-strudel\nbushel\ngolden-delicious\nhalf-baked-apple\nhappy-trails\nhayseed-turnip-truck\nprairie-tune\nred-delicious\nuncle-orange\nwensley\naction-shot\namaranthine\nambrosia\ncrystal-chalice-stand-pony\namethyst-gleam\namira\napple-bottom\napricot-bow\nbeauty-brass\nbell-perin\nbella-brella\nbelle-star\nberry-dreams\nberry-frost\nberry-icicle\nberryshine\nbiddy-broomtail\nbig-wig\nbitta-blues\nblue-bonnet\nblue-bows\nblue-cutie\nblue-nile\nbonnie\nbottlecap\nbubblegum-blossom\nbutter-pop\ncandy-mane\ncandy-twirl\ncarlotta\ncharcoal-bakes\ncharged-up\nchelsea-porcelain\ncherry-berry\ncherry-punch\nchilly-puddle\ncobalt-shade\ncoral-shine\ncornflower\ncrescendo\ndainty-dove\ndaisy\ndoseydotes\ndosie-dough\ndry-wheat\neclair-cr\nelphaba-trot\nfiddly-faddle\nflounder\nflurry\nforest-spirit\nginger-gold\ngolden-harvest\ngrace\ngrape-delight\ngreen-jewel\nhazel-harvest\nhinny-of-the-hills\nimmemoria\njoan-pommelway\njubileena\nlady-justice\nlavender-august\nlavender-blush\nlavenderhoof\nlemon-chiffon\nlilac-blossom\nlilac-links\nlily-valley\nlinked-hearts\nlittle-po\nluckette\nlucky-star\nlyrica-lilac\nmajesty\nmango-juice\nwhinnyapolis-delegate\nmarch-gustysnows\nmaribelle\nmarigold\nmaroon-carrot\nmasquerade\nmaybelline\nmidnight-fun\nmillie\nmint-swirl\nmj\nnurse-snowheart\nnurse-sweetheart\nnurse-tenderheart\noakey-doke\nobscurity\noctavia-melody\npaisley-pastel\npampered-pearl\npeachy-cream\npearly-stitch\npeggy-holstein\npetunia\npicture-perfect\npinot-noir\npitch-perfect\nplay-write\npowder-rouge\npretty-vision\npurple-haze\npurple-wave\npursey-pink\nmasseuse-pony\nquake\nreflective-rock\nregal-candent\nrose\nroxie\nruby-splash\nscrewball\nscrewy\nseasong\nserena\nshoeshine\nsilver-frames\nsky-view\nsnappy-scoop\nsoft-spot\nsoigne-folio\nsoot-stain\nspaceage-sparkle\nspring-forward\nspring-water\nstella\nstrawberry-ice\nsun-streak\nsunny-smiles\nsunset-bliss\nsurf\nsweetberry\nsweetie-drops\nswirly-cotton\nsymphony\ntoffee\ntree-sap\ntropical-spring\nturf\nvanilla-sweets\nvera\nvidala-swoon\nwelly\nwildwood-flower\nwilma\nwinter-withers\nace\naffero\napple-bread\napple-slice\nbaritone\nbiff\nbig-top\nprofessor\nbill-neigh\nblack-stone\nmr\nbrindle-young\nburnt-oak\nbusiness-saavy\ncaboose\ncaesar\ncaramel\ncharlie-coal\ncherry-fizzy\ncherry-strudel\nchocolate-haze\nclassy-clover\nclean-sweep\ncloudy-haze\ncobalt\ncoco-crusoe\ncommander-redfeather\nconcerto\ncormano\nancient-beast-dealer\ncratetoss\ncreme-brulee\ncrest-crown\nflashy-pony\ndance-fever\ndavenport\ndirtbound\ndr\npest-control-pony\negon\neiffel\nemerald-beacon\nemerald-green\nevening-star\nfelix\nfrederick-horseshoepin\nfull-steam\nfuzzy-slippers\ngeri\ngingerbread\nglobe-trotter\ngoldengrape\ngrape-crush\nmr\nhaakim\nharry-trotter\nhay-fever\nhaymish\nhercules\nhermes\nhughbert-jellius\nicy-drop\njeff-letrotski\njes\njim-beam\njohn-bull\nkarat\nkazooie\nklein\nduke-of-maretonia\nkyrippos-ii\nleadwing\nlincoln\nsecurity-guard\nlockdown\nlucky-clover\nmatch-game\nmeadow-song\nmelilot\nmoon-dust\nmorton-saltworthy\nastro-pony\nneptunio\nnight-watch\nnoteworthy\nparcel-post\nparish-nandermane\nperfect-pace\npersnickety\npigpen\npine-breeze\npinto-picasso\npipe-down\nraggedy-doctor\nragtime\nrivet\nrogue\nrough-tumble\nroyal-riff\nsam\nsavoir-fare\nsealed-scroll\nshamrock\nshooting-star\nshortround\nsir-pony-moore\nslendermane\nsmokestack\nsourpuss\nsqueaky-clean\nstar-gazer\nsteel-wright\nsterling-silver\nstrawberry-cream\nswanky-hank\ntall-order\ntall-tale\ntemple-chant\ntwilight-sky\nbowling-pony\nwalter\nwelch\nwetzel\nwisp\nwithers\namethyst-star\ncharm\ncloud-kicker\nderpy\ndiamond-mint\nfine-line\nfluttershy\nhelia\nlemon-hearts\nlemony-gem\nlyra-heartstrings\nmerry-may\nminuette\norange-swirl\nparasol\nprimrose\nrainbow-dash\nrarity\nraven\nsapphire-shores\nsassaflash\nsea-swirl\nsprinkle-medley\nstrawberry-sunrise\nsunshower-raindrops\nswan-song\ntwilight-sparkle\ntwinkleshine\nwhite-lightning"
                                 #"\n"))