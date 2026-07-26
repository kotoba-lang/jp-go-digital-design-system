(ns jp-go-dds.tokens-test
  "`jp-go-dds.tokens` の契約テスト。

  一番大事なのは `every-bridged-primitive-exists-in-vendored-css`:
  橋渡しの右辺は全て vendor 済み DADS CSS が実際に発行している custom
  property でなければならない。存在しない変数を参照しても CSS は**エラーを
  出さず、その宣言だけが黙って無効になる** — つまり `--hig-color-tint` が
  何色にもならず、それを使っている SVG が不可視になる、という形でしか
  表面化しない。上流 CSS を `scripts/vendor.cljs` で更新して primitive 名が
  変わったときに、ここで落ちてほしい。"
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [clojure.string :as str]
            [jp-go-dds.tokens :as tokens]))

(def ^:private vendored-css
  "vendor 済み dds.css。JVM からは slurp、nbb からは node:fs。
  ライブラリ本体は純関数のまま（css は呼び出し側が読む）なので、
  読み込みはテスト側の責任。"
  #?(:clj (slurp "resources/jp_go_dds/dds.css")
     :cljs (let [fs (js/require "fs")]
             (str (.readFileSync fs "resources/jp_go_dds/dds.css")))))

(defn- declared-custom-properties
  "CSS が `--name:` として宣言している custom property の集合。"
  [css]
  (into #{} (map #(str/replace % #":$" ""))
        (re-seq #"--[a-zA-Z0-9-]+:" css)))

(defn- referenced-vars
  "`var(--x)` / `var(--x, fallback)` として参照されている名前の集合。"
  [s]
  (into #{} (map second) (re-seq #"var\(\s*(--[a-zA-Z0-9-]+)" s)))

;; ───────────────── 橋渡しの右辺が実在すること ─────────────────

(deftest every-bridged-primitive-exists-in-vendored-css
  (let [declared (declared-custom-properties vendored-css)
        referenced (referenced-vars (str/join " " (vals tokens/hig->dads)))
        missing (sort (remove declared referenced))]
    (is (empty? missing)
        (str "vendor 済み DADS CSS が発行していない custom property を"
             "橋渡しが参照している(その宣言は黙って無効になる): "
             (pr-str missing)))
    (is (seq referenced) "橋渡しが1つも DADS primitive を参照していない")))

(deftest bridge-never-hardcodes-a-hex-colour
  (testing "palette 値は var() 参照のみ — hex を焼くと vendor 更新から取り残される"
    (doseq [[k v] tokens/hig->dads
            :when (str/includes? k "color")]
      (is (str/starts-with? v "var(")
          (str k " が var() 参照でない: " v))
      (is (not (re-find #"#[0-9a-fA-F]{3,8}\b" v))
          (str k " に raw hex が焼かれている: " v))))

  (testing "唯一の raw 値は font-mono の system stack"
    (let [raw (remove #(str/starts-with? (val %) "var(") tokens/hig->dads)]
      (is (= #{"--hig-font-mono"} (set (map key raw)))
          (str "想定外の raw 値: " (pr-str (map key raw)))))))

;; ───────────────── 左辺が --hig-* 契約であること ─────────────────

(deftest bridge-only-redefines-the-hig-contract
  (testing "DADS 自身の変数も class も上書きしない — skin を戻せる条件"
    (doseq [k (keys tokens/hig->dads)]
      (is (str/starts-with? k "--hig-")
          (str "--hig-* 以外を再定義している: " k))))

  (testing "bridge-css は :root 宣言 1 本で、selector を持たない"
    (is (str/starts-with? tokens/bridge-css ":root{"))
    (is (str/ends-with? tokens/bridge-css "}"))
    (is (= 1 (count (re-seq #"\{" tokens/bridge-css)))
        "宣言ブロックが複数ある = DADS の class に触れている疑い")
    (is (not (str/includes? tokens/bridge-css ".dads-"))
        "橋渡しが DADS の class を触っている")))

(deftest bridge-css-emits-every-mapping
  (doseq [[k v] tokens/hig->dads]
    (is (str/includes? tokens/bridge-css (str k ":" v ";"))
        (str "bridge-css に " k " が出ていない"))))

;; ───────────────── a11y 補正 ─────────────────

(deftest a11y-css-covers-the-three-audit-findings
  (testing "color-scheme の CSS 宣言(light 固定でも減点対象)"
    (is (str/includes? tokens/a11y-css "color-scheme:light")))
  (testing "tap target 44px"
    (is (str/includes? tokens/a11y-css "min-height:44px"))
    (is (str/includes? tokens/a11y-css ".dads-button")))
  (testing "safe-area は左右下の全辺"
    (doseq [side ["left" "right" "bottom"]]
      (is (str/includes? tokens/a11y-css (str "env(safe-area-inset-" side))
          (str side " が safe-area 未対応")))))

(deftest skin-css-is-bridge-then-a11y
  (testing "順序: 橋渡しが先、補正が後。アプリの :app-css はさらに後で後勝ちする"
    (is (= tokens/skin-css (str tokens/bridge-css tokens/a11y-css)))
    (is (< (str/index-of tokens/skin-css "--hig-color-tint")
           (str/index-of tokens/skin-css "min-height:44px")))))

;; ───────────────── ヘルパ ─────────────────

(deftest hig-var-accepts-both-spellings
  (is (= "var(--hig-color-tint)" (tokens/hig-var "--hig-color-tint")))
  (is (= "var(--hig-color-tint)" (tokens/hig-var "color-tint")))
  (is (= "var(--hig-color-tint)" (tokens/hig-var :color-tint))))

(deftest bridged?-tells-an-app-whether-a-token-survives-this-skin
  (is (true? (tokens/bridged? "--hig-color-tint")))
  (is (true? (tokens/bridged? "color-tint")))
  (testing "橋渡しの無い token は false — kotoba-ui skin でしか出ない"
    (is (false? (tokens/bridged? "--hig-color-nonexistent")))
    (is (false? (tokens/bridged? "--hig-material-thick")))))

;; ───────────── 実資産に対する回帰: itad の art.cljc が使う token ─────────────

(deftest covers-the-tokens-the-first-consumer-actually-uses
  (testing "ai-gftd-itad/art.cljc が参照する --hig-* は全て橋渡し済み"
    ;; この一覧は art.cljc の実際の参照（`grep -o 'var(--hig-[a-z-]*)'`）。
    ;; itad が app CSS に手書きしていた橋渡しをライブラリへ持ち上げた以上、
    ;; ここが落ちる = itad の SVG が無色になる、を意味する。
    (doseq [t ["--hig-color-label" "--hig-color-secondary-label"
               "--hig-color-tint" "--hig-palette-green" "--hig-palette-red"
               "--hig-palette-orange" "--hig-color-secondary-system-fill"
               "--hig-color-quaternary-system-fill" "--hig-color-system-background"
               "--hig-font-text" "--hig-font-mono"]]
      (is (tokens/bridged? t) (str t " の橋渡しが無い")))))
