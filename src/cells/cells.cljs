(ns cells.cells
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cells.state :as state :refer [layout sources values compiled-fns]]
            [cljs.core.async :refer [put! chan <! >!]]
            [cells.compiler :refer [def-in-cljs-user declare-in-cljs-user compile-as-fn def-value-in-cljs-user]]
            [cells.refactor.rename :refer [replace-symbol]]
            [cells.refactor.find :refer [find-reactive-symbols]]
            [reagent.core :as r]
            ))

(declare alphabet-name number-name little-pony-names new-name make-id update-subs! clear-intervals!)

(defn new-cell!
  ([] (new-cell! (new-name) nil))
  ([data] (new-cell! (new-name) data))
  ([id data]
   (go
     (let [id (if-let [id (:id data)] (make-id id) (new-name))
           source-atom (r/atom nil)
           compiled-fn-atom (r/atom (js* "function(){}"))
           value-atom (r/atom (:initial-val data))]

       (swap! sources assoc id source-atom)
       (swap! compiled-fns assoc id compiled-fn-atom)
       (swap! values assoc id value-atom)

       (<! (def-value-in-cljs-user id))

       (add-watch source-atom :self-watch
                  (fn [_ _ _ new]
                    (go
                      (when (not= id @state/current-cell)   ;TODO - layout should control when source atom updates
                        (let [f (<! (compile-as-fn new))]
                          (when (not= (.toString f) (.toString @compiled-fn-atom))
                            (reset! compiled-fn-atom f)))))))

       (add-watch compiled-fn-atom :self-watch
                  (fn [_ _ _ fn]
                    (go
                      (update-subs! id)
                      (clear-intervals! id)
                      (binding [cljs.user/self @value-atom
                                cljs.user/self-id id]
                        (reset! value-atom (fn))))))

       (add-watch value-atom :self-watch
                  (fn [_ _ old new]
                    (go
                      (when (not= old new)
                        (<! (def-value-in-cljs-user id))
                        (doseq [s (get @state/dependents id)]
                          (clear-intervals! s)
                          (binding [cljs.user/self @(get @values s)
                                    cljs.user/self-id s]
                            (reset! (get @values s) (try (@(get @compiled-fns s))
                                                         (catch js/Error e [:div {:class-name "cell-error"} (str e)])))))))))

       (reset! source-atom (:source data))                  ;this will start the reactions
       id))))



(defn- update-values [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn update-subs! [id]
  (swap! state/dependents update-values #(disj % id))
  (doseq [s (find-reactive-symbols (-> @state/sources (get id) deref))]
    (swap! state/dependents update s #(conj (or % #{}) id))))

(defn clear-intervals! [id]
  (doseq [i (get @state/interval-ids id)] (js/clearInterval i))
  (swap! state/interval-ids assoc id []))

(defn clear-dependents! [id]
  (swap! state/dependents update-values disj id))

(defn kill-cell!
  [id]
  (clear-intervals! id)
  (clear-dependents! id)
  (swap! state/dependents dissoc id)
  (swap! values dissoc id)                            ;cell value-cache
  (swap! compiled-fns dissoc id)
  (swap! sources dissoc id))

(defn interval
  ([f] (interval f 500))
  ([n f]
   (let [id cljs.user/self-id
         value (get @values id)
         exec #(binding [cljs.user/self @(get @values id)
                         cljs.user/self-id id]
                (reset! value (f cljs.user/self)))]
     (clear-intervals! id)
     (let [interval-id (js/setInterval exec (max 24 n))]
       (swap! state/interval-ids update id #(conj (or % []) interval-id)))
     (f @value))))

(defn rename-symbol! [old-symbol new-symbol]
  (go
    (let [all-vars (set (flatten [(keys @sources)
                                  (keys (ns-interns 'cljs.core))
                                  (keys (ns-interns 'cells.cell-helpers))]))]

      (when (and new-symbol (not= old-symbol new-symbol) (not (all-vars new-symbol)))

        (<! (new-cell! new-symbol
                       {:source      @(get @sources old-symbol)
                        :initial-val @(get @values old-symbol)}))

        (doseq [view (:views @state/layout)]
          (if (= old-symbol (:id @view)) (swap! view assoc :id new-symbol)))

        (doseq [[_ src-atom] @sources]
          (swap! src-atom #(replace-symbol % old-symbol new-symbol)))

        (kill-cell! old-symbol)))))


(defn make-id [id-candidate]
  (if (or (nil? id-candidate) (get @sources id-candidate))
    (new-name)
    id-candidate))

(defn alphabet-name []
  (let [char-index-start 97
        char-index-end 123]
    (loop [i char-index-start
           repetitions 1]
      (let [letter (symbol (apply str (repeat repetitions (char i))))]
        (if-not (contains? @sources letter) letter
                                           (if (= i char-index-end)
                                             (recur char-index-start (inc repetitions))
                                             (recur (inc i) repetitions)))))))

(defn little-pony-name []
  (-> little-pony-names shuffle first symbol))
(def new-name little-pony-name)

(defn number-name []
  (inc (count @state/sources)))

(def little-pony-names (clojure.string/split "applejack\npinkie-pie\naloe\ncheerilee\ncherry-jubilee\ncoco-pommel\ngoldie-delicious\ngranny-smith\nthe-headless-horse\njunebug\nlotus-blossom\nmane\nmayor-mare\nnurse-redheart\nthe-olden-pony\nphoto-finish\nprim-hemline\nroma\nsuri-polomare\nteddie-safari\ntorch-song\ntree-hugger\nconductor\nall-aboard\nbig-mcintosh\nbraeburn\ncheese-sandwich\ndoc-top\ndouble-diamond\ngizmo\nhoity-toity\nrandolph\nearth-pony-royal-guards\nsheriff-silverstar\nsilver-shill\ntrain-conductor\nsteamer\ntoe\ntrouble-shoes\npinkie\ncloudy-quartz\npinkie\nmaud-pie\ngranny-smith\napple-bumpkin\napple-cider\napple-fritter\napple-honey\napple-leaves\naunt-orange\nauntie-applesauce\ncandy-apples\ncaramel-apple\nflorina-tart\ngala-appleby\ngranny-smith\nigneous-rock\njonagold\nlavender-fritter\nmagdalena\npeachy-sweet\npinkie\npokey-oaks\nred-gala\nstinkin\nsundowner\napple-bottoms\napple-cinnamon\napple-strudel\nbushel\ngolden-delicious\nhalf-baked-apple\nhappy-trails\nhayseed-turnip-truck\nprairie-tune\nred-delicious\nuncle-orange\nwensley\naction-shot\namaranthine\nambrosia\ncrystal-chalice-stand-pony\namethyst-gleam\namira\napple-bottom\napricot-bow\nbeauty-brass\nbell-perin\nbella-brella\nbelle-star\nberry-dreams\nberry-frost\nberry-icicle\nberryshine\nbiddy-broomtail\nbig-wig\nbitta-blues\nblue-bonnet\nblue-bows\nblue-cutie\nblue-nile\nbonnie\nbottlecap\nbubblegum-blossom\nbutter-pop\ncandy-mane\ncandy-twirl\ncarlotta\ncharcoal-bakes\ncharged-up\nchelsea-porcelain\ncherry-berry\ncherry-punch\nchilly-puddle\ncobalt-shade\ncoral-shine\ncornflower\ncrescendo\ndainty-dove\ndaisy\ndoseydotes\ndosie-dough\ndry-wheat\neclair-cr\nelphaba-trot\nfiddly-faddle\nflounder\nflurry\nforest-spirit\nginger-gold\ngolden-harvest\ngrace\ngrape-delight\ngreen-jewel\nhazel-harvest\nhinny-of-the-hills\nimmemoria\njoan-pommelway\njubileena\nlady-justice\nlavender-august\nlavender-blush\nlavenderhoof\nlemon-chiffon\nlilac-blossom\nlilac-links\nlily-valley\nlinked-hearts\nlittle-po\nluckette\nlucky-star\nlyrica-lilac\nmajesty\nmango-juice\nwhinnyapolis-delegate\nmarch-gustysnows\nmaribelle\nmarigold\nmaroon-carrot\nmasquerade\nmaybelline\nmidnight-fun\nmillie\nmint-swirl\nmj\nnurse-snowheart\nnurse-sweetheart\nnurse-tenderheart\noakey-doke\nobscurity\noctavia-melody\npaisley-pastel\npampered-pearl\npeachy-cream\npearly-stitch\npeggy-holstein\npetunia\npicture-perfect\npinot-noir\npitch-perfect\nplay-write\npowder-rouge\npretty-vision\npurple-haze\npurple-wave\npursey-pink\nmasseuse-pony\nquake\nreflective-rock\nregal-candent\nrose\nroxie\nruby-splash\nscrewball\nscrewy\nseasong\nserena\nshoeshine\nsilver-frames\nsky-view\nsnappy-scoop\nsoft-spot\nsoigne-folio\nsoot-stain\nspaceage-sparkle\nspring-forward\nspring-water\nstella\nstrawberry-ice\nsun-streak\nsunny-smiles\nsunset-bliss\nsurf\nsweetberry\nsweetie-drops\nswirly-cotton\nsymphony\ntoffee\ntree-sap\ntropical-spring\nturf\nvanilla-sweets\nvera\nvidala-swoon\nwelly\nwildwood-flower\nwilma\nwinter-withers\nace\naffero\napple-bread\napple-slice\nbaritone\nbiff\nbig-top\nprofessor\nbill-neigh\nblack-stone\nmr\nbrindle-young\nburnt-oak\nbusiness-saavy\ncaboose\ncaesar\ncaramel\ncharlie-coal\ncherry-fizzy\ncherry-strudel\nchocolate-haze\nclassy-clover\nclean-sweep\ncloudy-haze\ncobalt\ncoco-crusoe\ncommander-redfeather\nconcerto\ncormano\nancient-beast-dealer\ncratetoss\ncreme-brulee\ncrest-crown\nflashy-pony\ndance-fever\ndavenport\ndirtbound\ndr\npest-control-pony\negon\neiffel\nemerald-beacon\nemerald-green\nevening-star\nfelix\nfrederick-horseshoepin\nfull-steam\nfuzzy-slippers\ngeri\ngingerbread\nglobe-trotter\ngoldengrape\ngrape-crush\nmr\nhaakim\nharry-trotter\nhay-fever\nhaymish\nhercules\nhermes\nhughbert-jellius\nicy-drop\njeff-letrotski\njes\njim-beam\njohn-bull\nkarat\nkazooie\nklein\nduke-of-maretonia\nkyrippos-ii\nleadwing\nlincoln\nsecurity-guard\nlockdown\nlucky-clover\nmatch-game\nmeadow-song\nmelilot\nmoon-dust\nmorton-saltworthy\nastro-pony\nneptunio\nnight-watch\nnoteworthy\nparcel-post\nparish-nandermane\nperfect-pace\npersnickety\npigpen\npine-breeze\npinto-picasso\npipe-down\nraggedy-doctor\nragtime\nrivet\nrogue\nrough-tumble\nroyal-riff\nsam\nsavoir-fare\nsealed-scroll\nshamrock\nshooting-star\nshortround\nsir-pony-moore\nslendermane\nsmokestack\nsourpuss\nsqueaky-clean\nstar-gazer\nsteel-wright\nsterling-silver\nstrawberry-cream\nswanky-hank\ntall-order\ntall-tale\ntemple-chant\ntwilight-sky\nbowling-pony\nwalter\nwelch\nwetzel\nwisp\nwithers\namethyst-star\ncharm\ncloud-kicker\nderpy\ndiamond-mint\nfine-line\nfluttershy\nhelia\nlemon-hearts\nlemony-gem\nlyra-heartstrings\nmerry-may\nminuette\norange-swirl\nparasol\nprimrose\nrainbow-dash\nrarity\nraven\nsapphire-shores\nsassaflash\nsea-swirl\nsprinkle-medley\nstrawberry-sunrise\nsunshower-raindrops\nswan-song\ntwilight-sparkle\ntwinkleshine\nwhite-lightning"
                                             #"\n"))