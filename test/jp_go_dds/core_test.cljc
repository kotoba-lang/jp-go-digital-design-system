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

(deftest notification-banner-markup
  (testing "上流 markup に忠実: root の data-style/data-type、heading/icon/body"
    (let [b (html/->html
             (dds/notification-banner
              {:type :warning :heading "申込前にお読みください"}
              [:p "本文"]))]
      (is (str/includes? b "<div class=\"dads-notification-banner\" data-style=\"standard\" data-type=\"warning\">"))
      (is (str/includes? b "<h2 class=\"dads-notification-banner__heading\">"))
      (is (str/includes? b "dads-notification-banner__icon"))
      (is (str/includes? b "aria-label=\"警告\""))
      ;; 上流の icon path をそのまま出す(fill="Canvas" 込み)
      (is (str/includes? b "M1 21 12 2l11 19H1Z"))
      (is (str/includes? b "fill=\"Canvas\""))
      (is (str/includes? b "dads-notification-banner__heading-text\">申込前にお読みください"))
      (is (str/includes? b "<div class=\"dads-notification-banner__body\"><p>本文</p></div>"))))
  (testing "type ごとに icon と aria-label が変わる"
    (doseq [[t label] {:success "成功" :error "エラー" :info-1 "インフォメーション"}]
      (is (str/includes? (html/->html (dds/notification-banner {:type t :heading "h"}))
                         (str "aria-label=\"" label "\"")))))
  (testing "閉じるボタンは既定で出ない(静的ページに動かない UI を置かない)"
    (is (not (str/includes? (html/->html (dds/notification-banner {:heading "h"}))
                            "dads-notification-banner__close")))
    (let [c (html/->html (dds/notification-banner {:heading "h" :closable? true
                                                   :close-id "x-close"}))]
      (is (str/includes? c "aria-labelledby=\"x-close\""))
      (is (str/includes? c "id=\"x-close\" class=\"dads-notification-banner__close-label\">閉じる"))))
  (testing "timestamp / actions は与えた時だけ出る"
    (let [plain (html/->html (dds/notification-banner {:heading "h"}))]
      (is (not (str/includes? plain "__timestamp")))
      (is (not (str/includes? plain "__actions"))))
    (let [full (html/->html
                (dds/notification-banner
                 {:heading "h"
                  :timestamp {:datetime "2024-07-01" :text "2024年7月1日"}
                  :actions [(dds/button "詳細" {:type :outline})]}))]
      (is (str/includes? full "<time datetime=\"2024-07-01\">2024年7月1日</time>"))
      (is (str/includes? full "dads-notification-banner__actions"))
      (is (str/includes? full "dads-button")))))

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
