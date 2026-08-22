(ns jp-go-dds.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [html.core :as html]
            [jp-go-dds.core :as dds]
            [jp-go-dds.page :as page]
            [jp-go-dds.skin :as skin]
            [jp-go-dds.css :as dcss]))

(def dds-components
  "vendor 済み component の一覧(生成物 components.edn と同じ内容)。"
  [:accordion :blockquote :breadcrumb :button :calendar :carousel :checkbox
   :chip-label :date-picker :description-list :disclosure :divider :drawer
   :emergency-banner :file-upload :form-control-label :hamburger-menu-button
   :heading :horizontal-menu :image :input-text :language-selector :link :list
   :menu-list :menu-list-box :modal-dialog :notification-banner :page-navigation
   :progress-indicator :radio :resource-list :search-box :select
   :step-navigation :tab :table :textarea :toc :utility-link])

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

(deftest ext-css-is-edn-authored
  (testing "ext-rules は EDN データ(生 CSS 文字列を持ち込まない)"
    (is (vector? dds/ext-rules))
    (is (every? (fn [[sel decls]] (and (string? sel) (map? decls))) dds/ext-rules))
    ;; 値は全て EDN のスカラ/キーワード — CSS の宣言区切り(;)や
    ;; ルール境界({ })が文字列に埋まっていないこと。
    (is (not-any? (fn [[_ decls]]
                    (some #(and (string? %) (re-find #"[{};]" %)) (vals decls)))
                  dds/ext-rules)))
  (testing "順序が保たれる(map だと要素数超過でハッシュ順になりカスケードが壊れる)"
    (let [sels (mapv first dds/ext-rules)]
      (is (< (.indexOf sels ".dds-ext-section")
             (.indexOf sels ".dds-ext-section:first-of-type")))))
  (testing "表は自分の中だけで横スクロールする(ページ全体を横スクロールさせない)"
    ;; 上流 .dads-table は overflow を持たないため ext-rules で封じ込める。
    (is (= {:max-width "100%" :min-width 0 :overflow-x "auto"}
           (some (fn [[sel decls]] (when (= ".dads-table" sel) decls)) dds/ext-rules)))
    (is (str/includes? dds/ext-css "overflow-x: auto")))
  (testing "page がその ext-css を inline する"
    (is (str/includes? (page/->page {:title "t" :css ""} [:p "x"])
                       "overflow-x: auto"))))

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

;; ───────────────── 互換スキン ─────────────────

(deftest skin-is-edn-authored
  (testing "skin-rules も EDN データ(生 CSS 文字列を書かない)"
    (is (vector? skin/skin-rules))
    (is (every? (fn [[sel decls]] (and (string? sel) (map? decls))) skin/skin-rules))
    (is (not-any? (fn [[_ decls]]
                    (some #(and (string? %) (re-find #"[{};]" %)) (vals decls)))
                  skin/skin-rules)))
  (testing "raw hex を書かない(色は DADS token 経由)"
    (is (not-any? (fn [[_ decls]]
                    (some #(and (string? %) (re-find #"#[0-9a-fA-F]{3,8}\b" %)) (vals decls)))
                  skin/skin-rules)))
  (testing "governor の判定色は semantic token に載る"
    (doseq [[sel token] {".ok" "success" ".warn" "warning" ".err,.critical" "error"}]
      (let [decls (some (fn [[s d]] (when (= sel s) d)) skin/skin-rules)]
        (is (some? decls) (str sel " が無い"))
        (is (re-find (re-pattern token) (str (:color decls)))
            (str sel " が semantic " token " token を使っていない")))))
  (testing "横に長い表がページ全体を横スクロールさせない(素の表のみ —
            DADS の table component は自前で overflow を持つので除外する)"
    (let [t (some (fn [[s d]] (when (= "table:not(.dads-table__table)" s) d))
                  skin/skin-rules)]
      (is (= "auto" (:overflow-x t)))
      (is (= "100%" (:max-width t))))))

(deftest select-markup
  (testing "上流 select/with-form-control-label.html に忠実な入れ子"
    (let [s (html/->html (dds/select {:id "f" :name "n" :required true}
                                     [["" "選択してください"] ["1" "足立区"]]))]
      (is (str/includes? s "<span class=\"dads-select\">"))
      (is (str/includes? s "<span class=\"dads-select__control\">"))
      (is (str/includes? s "class=\"dads-select__select\" data-size=\"md\""))
      (is (str/includes? s "dads-select__chevron"))
      ;; chevron の path は上流のものをそのまま
      (is (str/includes? s "M12 17L3 8L4 7L12 15L20 7L21 8L12 17Z"))
      (testing "value 空の項目は placeholder(disabled+selected)"
        (is (str/includes? s "<option value=\"\" disabled selected>選択してください</option>")))
      (is (str/includes? s "<option value=\"1\">足立区</option>"))))
  (testing "error があるときだけ error-text を出す"
    (is (not (str/includes? (html/->html (dds/select {} [["a" "A"]])) "__error-text")))
    (is (str/includes? (html/->html (dds/select {:error "＊必須です"} [["a" "A"]]))
                       "dads-select__error-text"))))

(deftest select-current-value
  (testing ":value に一致する option だけが selected になる"
    (let [s (html/->html (dds/select {:value "18"} [["12" "12mm"] ["18" "18mm"] ["24" "24mm"]]))]
      (is (str/includes? s "<option value=\"18\" selected>18mm</option>"))
      (is (str/includes? s "<option value=\"12\">12mm</option>"))
      (is (str/includes? s "<option value=\"24\">24mm</option>"))
      (is (= 1 (count (re-seq #"selected" s))))))
  (testing "数値と文字列は同じ値として一致する（options は数値を持つことがある）"
    (is (str/includes? (html/->html (dds/select {:value 18} [[18 "18mm"]]))
                       "selected"))
    (is (str/includes? (html/->html (dds/select {:value "18"} [[18 "18mm"]]))
                       "selected")))
  (testing ":value を渡さなければ selected は付かない（＝ブラウザは先頭を選ぶ）"
    ;; これは仕様であって欠陥ではない。ただし既定値を持つフォームで :value を
    ;; 忘れると**黙って先頭が選ばれる**ので、consumer 側が気付けるよう明示する。
    (is (not (str/includes? (html/->html (dds/select {} [["12" "12mm"] ["18" "18mm"]]))
                            "selected"))))
  (testing "placeholder がある場合、:value 指定はそちらを上書きしない"
    (let [s (html/->html (dds/select {:value "1"} [["" "選択してください"] ["1" "足立区"]]))]
      (is (str/includes? s "<option value=\"\" disabled selected>選択してください</option>"))
      (is (str/includes? s "<option value=\"1\" selected>足立区</option>")))))

(deftest language-selector-markup
  (let [markup
        (html/->html
         (dds/language-selector
          {:id-prefix "header-language"
           :current :ja
           :languages [{:code :ja :label "日本語" :href "?lang=ja"}
                       {:code :en :label "English" :href "?lang=en"}]}))]
    (testing "上流3コンポーネントのclassと開閉hookを合成する"
      (is (str/includes? markup "class=\"dads-language-selector\""))
      (is (str/includes? markup "class=\"dads-menu-list-box\""))
      (is (str/includes? markup "class=\"dads-menu-list\""))
      (is (str/includes? markup "id=\"header-language-opener\""))
      (is (str/includes? markup "aria-controls=\"header-language-popup\""))
      (is (str/includes? markup "aria-expanded=\"false\""))
      (is (str/includes? markup "data-language-selector-opener")))
    (testing "オープナーは常にLanguage、言語名は自称"
      (is (str/includes? markup ">Language<"))
      (is (str/includes? markup "lang=\"ja\""))
      (is (str/includes? markup "hreflang=\"ja\""))
      (is (str/includes? markup ">日本語<"))
      (is (str/includes? markup ">English<")))
    (testing "現在言語だけにcurrentと可視化用checkが付く"
      (is (= 1 (count (re-seq #"aria-current=\"true\"" markup))))
      (is (= 2 (count (re-seq #"dads-language-selector__check" markup)))))))

(deftest form-field-requirement-vs-status
  (testing "必須マーカーは __requirement(data-required=true)。__status ではない"
    (let [f (html/->html (dds/form-field {:label "氏名" :requirement "※必須" :required? true} [:i]))]
      (is (str/includes? f "dads-form-control-label__requirement\" data-required=\"true\""))
      (is (not (str/includes? f "__status")))))
  (testing "__status は状態表示用として引き続き使える(別物)"
    (is (str/includes? (html/->html (dds/form-field {:label "x" :status "任意"} [:i]))
                       "dads-form-control-label__status")))
  (testing "required? false なら data-required=false"
    (is (str/includes? (html/->html (dds/form-field {:label "x" :requirement "任意"} [:i]))
                       "data-required=\"false\""))))

(deftest radio-markup
  (testing "上流 radio/playground.html に忠実"
    (let [r (html/->html (dds/radio "ラベル" {:name "g" :value "1" :checked true}))]
      (is (str/includes? r "<label class=\"dads-radio\" data-size=\"md\">"))
      (is (str/includes? r "<span class=\"dads-radio__radio\">"))
      (is (str/includes? r "class=\"dads-radio__input\" type=\"radio\""))
      (is (str/includes? r "name=\"g\" value=\"1\" checked"))
      (is (str/includes? r "dads-radio__label\">ラベル")))))

(deftest css-subsetting
  (testing "core は dds.css に入っているので css-for から落ちる")
  (is (= [:date-picker :radio] (dcss/extra-components [:button :date-picker :table :radio])))
  (testing "全 component が per-component ファイルとして vendor されている"
    (is (= 40 (count dds-components)))
    ;; card は example CSS のみのパターンなので一覧に出ない
    (is (not (contains? (set dds-components) :card)))
    (is (contains? (set dds-components) :select))))
