(ns jp-go-dds.kotoba-table-parity-test
  "W6 slice 7 for jp-go-dds: `table`'s semantic cell decisions.

  The parity boundary is tag + attributes.  Cell content and the fixed table
  nest are deliberately outside it; the rule being moved is which cells are
  column/row headers and the `scope` relationship each header exposes.

  Consumer APIs are unchanged; `kotoba-lang/compiler` is a test-only dep."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [jp-go-dds.core :as core]
            [kotoba.compiler.core :as compiler]
            [kotoba.kir :as ir]))

(def module (slurp "kotoba/table.kotoba"))

(def ^:private fuel 262144)

(defn- compile-and-run [cases]
  (let [defs (for [[name body] cases]
               (str "(defn " name " [] :string " body ")"))
        kir (:kir (compiler/compile-source
                   (str module "\n" (str/join "\n" defs))
                   :js-kotoba-v1))]
    (into {} (map (fn [[name _]]
                    [name (ir/execute kir (symbol name) [] {:fuel fuel})])
                  cases))))

(defn- rows [table-hiccup]
  (->> (tree-seq vector? seq table-hiccup)
       (filter #(and (vector? %) (= :tr (first %))))))

(defn- cells [row]
  (filter #(and (vector? %) (#{:th :td} (first %))) (drop 1 row)))

(defn- describe-cell [cell]
  (let [attrs (if (map? (second cell)) (second cell) {})]
    {:tag (name (first cell))
     :attrs (str/join " "
                      (for [k [:class :scope]
                            :let [v (get attrs k)]
                            :when (some? v)]
                        (str (name k) "=\"" v "\"")))}))

(defn- body-form [row-header? index suffix]
  (let [cell (str "(record-new [:ref :tbl/cell] " row-header? " " index ")")]
    [(str "tag" suffix) (str "(body-cell-tag " cell ")")
     (str "attrs" suffix) (str "(body-cell-attrs " cell ")")]))

(deftest column-headers-match-cljc
  (let [h (core/table {:headers ["Name" "Value"] :rows []})
        header-row (first (rows h))
        expected (map describe-cell (cells header-row))
        out (compile-and-run {"tag" "(header-tag)"
                              "attrs" "(header-attrs)"})]
    (is (= 2 (count expected)) "the fixture has more than one column")
    (doseq [cell expected]
      (is (= (:tag cell) (get out "tag")))
      (is (= (:attrs cell) (get out "attrs"))))
    (is (= "th" (get out "tag")))
    (is (str/includes? (get out "attrs") "scope=\"col\"")
        "column ownership is exposed to assistive technology")))

(deftest body-cell-semantics-match-cljc
  (let [cases [{:row-header? false :rows [["A" "B" "C"]]}
               {:row-header? true :rows [["A" "B" "C"] ["D" "E" "F"]]}]
        named (for [[case-index c] (map-indexed vector cases)
                    [row-index row] (map-indexed vector (:rows c))
                    cell-index (range (count row))]
                [(str case-index "_" row-index "_" cell-index) c row-index cell-index])
        forms (into {}
                    (mapcat (fn [[suffix c _ cell-index]]
                              (map vec
                                   (partition 2
                                              (body-form (if (:row-header? c) "true" "false")
                                                         cell-index suffix))))
                            named))
        out (compile-and-run forms)]
    (doseq [[suffix c row-index cell-index] named]
      (let [body-rows (if (seq (:headers c)) (rest (rows (core/table c)))
                          (rows (core/table c)))
            expected (describe-cell (nth (vec (cells (nth (vec body-rows) row-index)))
                                         cell-index))]
        (is (= (:tag expected) (get out (str "tag" suffix)))
            (str "tag parity " suffix))
        (is (= (:attrs expected) (get out (str "attrs" suffix)))
            (str "attrs parity " suffix))))
    (testing "only column zero becomes a row header"
      (is (= "th" (get out "tag1_0_0")))
      (is (str/includes? (get out "attrs1_0_0") "scope=\"row\""))
      (is (= "td" (get out "tag1_0_1")))
      (is (= "" (get out "attrs1_0_1"))))
    (testing "without :row-header? even column zero stays a data cell"
      (is (= "td" (get out "tag0_0_0")))
      (is (= "" (get out "attrs0_0_0"))))))
