(ns jp-go-dds.kotoba-dark-mirror-parity-test
  "W6 slice 2 for jp-go-dds: the dark ramp mirror as a Kotoba rule.

  `jp-go-dds.dark` derives a dark palette by walking each upstream ramp
  backwards — one rule, `i → n-1-i`, applied uniformly so that no ramp needs an
  exception table. `kotoba/dark_mirror.kotoba` owns that rule; this gate runs it
  against every ramp actually present in the vendored `dds.css` and requires it
  to agree with `dark/mirror` step for step.

  Scanning the stylesheet stays in `.cljc` — see the module header. The input
  here is what the scan produced, not the stylesheet.

  Consumer APIs are unchanged; `kotoba-lang/compiler` is a test-only dep."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [jp-go-dds.dark :as dark]
            [jp-go-dds.tokens :as tokens]
            [kotoba.compiler.core :as compiler]
            [kotoba.kir :as ir]))

(def module (slurp "kotoba/dark_mirror.kotoba"))

(def ^:private fuel 262144)

(def ^:private chunk-size
  "kotoba-kir `value/document-container-item-limit`."
  32)

(def ^:private dds-css (slurp "resources/jp_go_dds/dds.css"))

(def ^:private literals (dark/light-literals dds-css))
(def ^:private ramps (dark/ramps literals))

(defn- kotoba-literal [s]
  (str \" (-> s (str/replace "\\" "\\\\") (str/replace "\"" "\\\"")) \"))

(defn- steps-form [steps]
  (str "(document-vector "
       (str/join " " (map #(str "(document-i64 " % ")") (sort steps)))
       ")"))

(defn- compile-and-run [cases]
  (let [defs (for [[name body] cases]
               (str "(defn " name " [] :string " body ")"))
        kir (:kir (compiler/compile-source
                   (str module "\n" (str/join "\n" defs))
                   :js-kotoba-v1))]
    (into {} (map (fn [[name _]]
                    [name (ir/execute kir (symbol name) [] {:fuel fuel})])
                  cases))))

(defn- ok-or-err [expr]
  (str "(match-result " expr " [:result :string :string] (ok t t) (err m m))"))

(defn- cljc-pairs
  "`dark/mirror` rendered the same way `mirror-pairs` renders it."
  [steps]
  (let [m (dark/mirror steps)]
    (str/join " " (map (fn [s] (str s "->" (get m s))) (sort steps)))))

;; --- the rule -------------------------------------------------------------

(deftest mirror-matches-cljc-for-every-vendored-ramp
  (is (seq ramps) "sanity: the vendored stylesheet really has ramps")
  (let [named (map-indexed (fn [i [base steps]] [(str "r" i) base steps])
                           (sort-by key ramps))
        out (compile-and-run
             (into {} (for [[case-name _ steps] named]
                        [case-name (str "(mirror-pairs " (steps-form steps) ")")])))]
    (doseq [[case-name base steps] named]
      (is (= (cljc-pairs steps) (get out case-name))
          (str "mirror parity for " base)))
    (testing "the ramps really are long enough for the rule to say something"
      (is (<= 12 (apply max (map (comp count last) named)))
          "DADS publishes 13-step colour ramps; a 1-step ramp would prove nothing")
      (is (every? #(<= (count (last %)) chunk-size) named)
          "a ramp must fit one document value, or this gate is not covering it"))))

(deftest odd-length-ramps-fix-their-midpoint
  (testing "the midpoint of a monotone ramp survives reversal"
    (let [out (compile-and-run
               {"odd" (str "(mirror-pairs " (steps-form [50 600 1200]) ")")
                "even" (str "(mirror-pairs " (steps-form [100 200 300 400]) ")")})]
      (is (= "50->1200 600->600 1200->50" (get out "odd")))
      (is (= "100->400 200->300 300->200 400->100" (get out "even")))
      (is (= (cljc-pairs [50 600 1200]) (get out "odd"))
          "and the .cljc agrees about the fixed point"))))

(deftest mirror-is-an-involution
  (testing "mirroring twice is identity, which is why light values must be saved"
    (let [steps (->> ramps (sort-by key) first val sort vec)
          out (compile-and-run
               (into {} (for [i (range (count steps))]
                          [(str "twice" i)
                           (str "(string-from-i64 (step-at " (steps-form steps)
                                " (mirror-index " (count steps)
                                " (mirror-index " (count steps) " " i "))))")])))]
      (doseq [i (range (count steps))]
        (is (= (str (nth steps i)) (get out (str "twice" i)))
            "mirror ∘ mirror = identity")))))

;; --- the escape hatch's name ----------------------------------------------

(defn- space-join-forms
  "Nest `string-concat` so one case covers a whole chunk of names."
  [forms]
  (reduce (fn [acc f]
            (str "(string-concat " acc " (string-concat \" \" " f "))"))
          (first forms)
          (rest forms)))

(deftest light-name-matches-cljc-for-every-literal
  (let [names (vec (sort (keys literals)))
        chunks (vec (partition-all chunk-size names))
        out (compile-and-run
             (into {} (for [[i chunk] (map-indexed vector chunks)]
                        [(str "n" i)
                         (space-join-forms
                          (map #(str "(light-name " (kotoba-literal %) ")")
                               chunk))])))]
    (is (seq names) "sanity: literals were extracted")
    (is (= 156 (count names))
        "every vendored literal is covered, not a sample")
    (doseq [[i chunk] (map-indexed vector chunks)]
      (is (= (str/join " " (map dark/->light-name chunk)) (get out (str "n" i)))
          (str "light-name parity for chunk " i)))))

(deftest light-name-checked-fails-closed
  (let [out (compile-and-run
             {"good" (ok-or-err "(light-name-checked \"--color-primitive-red-800\")")
              "wrong-prefix" (ok-or-err "(light-name-checked \"--dads-primitive-red-800\")")
              "too-short" (ok-or-err "(light-name-checked \"--c\")")})]
    (is (= "--dds-light-primitive-red-800" (get out "good")))
    (is (= (dark/->light-name "--color-primitive-red-800") (get out "good")))
    (is (= "not a --color- name" (get out "wrong-prefix")))
    (is (= "not a --color- name" (get out "too-short"))
        "a name shorter than the prefix must not be sliced")))

;; --- the two constraints this slice ran into ------------------------------

(deftest measured-input-bounds
  (testing "the whole sheet is out of reach; the :root block is not"
    (is (< 65536 (count (.getBytes dds-css "UTF-8")))
        "dds.css exceeds string-value-byte-limit, so no guest function can take it whole")
    (is (> 65536 (count (.getBytes (tokens/root-css dds-css) "UTF-8")))
        "the :root block would fit — size is not what keeps the scan on the host")
    (is (every? #(<= (count %) chunk-size) (vals ramps))
        "every ramp fits one document value")))
