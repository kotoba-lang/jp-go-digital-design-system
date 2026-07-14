(ns jp-go-dds.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [html.core :as html]
            [jp-go-dds.core :as dds]
            [jp-go-dds.page :as page]))

(deftest button-markup
  (testing "上流 markup に忠実: class + data-type + data-size"
    (is (= "<button class=\"dads-button\" data-type=\"solid-fill\" data-size=\"md\" type=\"button\">申込</button>"
           (html/->html (dds/button "申込"))))
    (is (str/includes? (html/->html (dds/button "詳細" {:type :outline :href "#f"}))
                       "<a class=\"dads-button\" data-type=\"outline\""))
    (is (str/includes? (html/->html (dds/button "送信" {:submit? true})) "type=\"submit\""))))

(deftest heading-and-accordion
  (is (= "<h1 class=\"dads-heading\" data-size=\"45\">見出し</h1>"
         (html/->html (dds/heading 1 "見出し"))))
  (let [a (html/->html (dds/accordion "質問" [:p "回答"] {:open? true}))]
    (is (str/includes? a "<details class=\"dads-accordion\" open>"))
    (is (str/includes? a "dads-accordion__summary"))
    (is (str/includes? a "dads-accordion__content"))))

(deftest form-controls
  (let [f (html/->html
           (dds/form-field {:label "会社名" :for "f-company" :status "必須"}
                           (dds/input-text {:id "f-company" :required true})))]
    (is (str/includes? f "dads-form-control-label"))
    (is (str/includes? f "dads-form-control-label__status\">必須"))
    (is (str/includes? f "dads-input-text__input"))
    (is (str/includes? f "required")))
  (is (str/includes? (html/->html (dds/checkbox "ノートPC" {:id "dt-laptop"}))
                     "dads-checkbox__input"))
  (is (str/includes? (html/->html (dds/textarea {:id "f-note"})) "dads-textarea__textarea")))

(deftest table-markup
  (let [t (html/->html (dds/table {:caption "料金" :headers ["項目" "価格"]
                                   :rows [["回収" "0円〜"]] :row-header? true}))]
    (is (str/includes? t "dads-table__col-header"))
    (is (str/includes? t "<th class=\"dads-table__row-header\" scope=\"row\">回収</th>"))
    (is (str/includes? t "<caption>料金</caption>"))))

(deftest page-is-light-fixed
  (let [p (page/->page {:title "t" :css ":root{--x:1}"} [:p "hi"])]
    (testing "light 固定(meta color-scheme light + theme-color 白)"
      (is (str/includes? p "name=\"color-scheme\" content=\"light\""))
      (is (str/includes? p "content=\"#ffffff\"")))
    (testing "既定で外部リクエストなし(google fonts は opt-in)"
      (is (not (str/includes? p "fonts.googleapis"))))
    (testing "vendored css + ext css が inline される"
      (is (str/includes? p ":root{--x:1}"))
      (is (str/includes? p "dds-ext-container")))))
