(ns jp-go-dds.tokens
  "DADS を『このワークスペース共通の skin』として使うための token 面。

  ## なぜこれが要るのか

  このワークスペースの view / SVG / audit は **`--hig-*` という token 契約**を
  既に共通言語として話している:

  - `kotoba-lang/shitsuke` の HIG semantic layer がその契約の発行元
    （ADR-2607122200）。
  - アプリ側の資産（例: `gftdcojp/ai-gftd-itad` の `art.cljc` は色を全て
    `var(--hig-color-*)` / `var(--hig-palette-*)` で書いた SVG）も同じ契約。
  - `kotoba-lang/design-quality` の決定論 audit も、この契約が満たされている
    前提でスコアを出す。

  DADS（デジタル庁デザインシステム）が発行する CSS custom property は
  `--color-key-*` / `--color-neutral-*` / `--color-primitive-*` / `--font-family-*`
  であって `--hig-*` ではない。つまり **skin を DADS に替えると、token 契約に
  従って正しく書かれた資産ほど色を失う**。

  itad はこれを app CSS に手書きした橋渡し（`ai-gftd-itad.lp/app-css` の
  `:root{--hig-color-label:var(--color-neutral-solid-gray-800); …}`）で回避
  していた。**それを2つ目のアプリが再導出した瞬間に token 契約は壊れる**
  （同じ `--hig-color-tint` が repo ごとに別の DADS 色に落ちる）ので、
  ここに 1 箇所だけ置く。

  ## 契約

  `bridge-css` は `--hig-*` を DADS primitive の上に再定義するだけで、
  DADS 自身の変数も class も**一切上書きしない**。したがって:

  - `--hig-*` で書かれた view/SVG は**無改造で** DADS palette に追従する。
  - `dads-*` class で書かれたコンポーネントは影響を受けない。
  - skin を kotoba-ui に戻すときは、この CSS を出さないだけでよい
    （`shitsuke.hig` が本来の `--hig-*` を発行し直す）。

  ## light 固定であることの帰結

  DADS 上流に dark palette は無い（`jp-go-dds.page` が `color-scheme: light`
  を明示する理由）。`--hig-*` は本来 light/dark 両対応の契約なので、この橋を
  使う限り **dark は表現できない**。これは欠陥ではなく skin の選択であり、
  dark が要るアプリは DADS ではなく kotoba-ui skin を選ぶ、が正しい分岐。"
  (:require [clojure.string :as str]
            [css.core :as css]
            #?(:clj [clojure.java.io])))

(def hig->dads
  "`--hig-*` 契約 → DADS primitive の対応表。

  値は**必ず `var(--dads-primitive)` 参照**にする（raw hex を書かない）。
  こうしておくと上流 CSS を `scripts/vendor.cljs` で再 vendor したときに
  palette の更新が自動で伝わる — ここに hex を焼くと vendor 更新から
  取り残される。

  対応が無い `--hig-*` は**意図的に空欄にせず、この map に載せない**。
  載せなければ `shitsuke.hig` の既定値がそのまま効く（= 未定義参照で色が
  消えるのではなく、HIG 既定にフォールバックする）。"
  {;; --- ラベル / テキスト -------------------------------------------------
   "--hig-color-label" "var(--color-neutral-solid-gray-800)"
   "--hig-color-secondary-label" "var(--color-neutral-solid-gray-600)"
   "--hig-color-tertiary-label" "var(--color-neutral-solid-gray-500)"
   "--hig-color-quaternary-label" "var(--color-neutral-solid-gray-420)"
   ;; --- 面 / 背景 ---------------------------------------------------------
   "--hig-color-system-background" "var(--color-neutral-white)"
   "--hig-color-secondary-system-background" "var(--color-neutral-solid-gray-50)"
   "--hig-color-secondary-system-fill" "var(--color-neutral-solid-gray-200)"
   "--hig-color-tertiary-system-fill" "var(--color-neutral-solid-gray-100)"
   "--hig-color-quaternary-system-fill" "var(--color-neutral-solid-gray-100)"
   "--hig-color-separator" "var(--color-neutral-solid-gray-300)"
   ;; --- アクセント ---------------------------------------------------------
   ;; DADS の key color（デジタル庁ブルー）。HIG の :tint に相当する。
   "--hig-color-tint" "var(--color-key-900)"
   ;; --- semantic palette ---------------------------------------------------
   "--hig-palette-blue" "var(--color-key-900)"
   "--hig-palette-green" "var(--color-primitive-green-700)"
   "--hig-palette-red" "var(--color-primitive-red-800)"
   "--hig-palette-orange" "var(--color-primitive-orange-800)"
   ;; --- 罫 / 塗り（サイト実測で使用頻度の高い順）---------------------------
   ;; `--hig-hairline` は itonami.cloud で 25 箇所参照されていて、色トークンの
   ;; 中でも影響が大きい。separator と同じ階調に寄せる。
   "--hig-hairline" "var(--color-neutral-solid-gray-300)"
   "--hig-color-opaque-separator" "var(--color-neutral-solid-gray-300)"
   "--hig-color-system-fill" "var(--color-neutral-solid-gray-200)"
   "--hig-color-placeholder-text" "var(--color-neutral-solid-gray-500)"
   ;; grouped background は iOS のグループ表の概念で DADS に対応物が無い。
   ;; 面としては通常背景と secondary 背景に潰すのが素直。
   "--hig-color-system-grouped-background" "var(--color-neutral-solid-gray-50)"
   "--hig-color-secondary-system-grouped-background" "var(--color-neutral-white)"
   "--hig-color-tertiary-system-grouped-background" "var(--color-neutral-solid-gray-50)"
   "--hig-color-tertiary-system-background" "var(--color-neutral-solid-gray-50)"
   ;; --- タイポグラフィ -----------------------------------------------------
   "--hig-font-text" "var(--font-family-sans)"
   ;; 見出しも本文と同じ family。DADS は display 用の別 family を持たない
   ;; ——持たないものを埋めるより、DADS の実際の姿に合わせる。
   "--hig-font-display" "var(--font-family-sans)"
   ;; DADS は mono family を発行しない。HIG 側も具体名を持つのはここだけなので
   ;; system stack を書く（唯一の raw 値。font family は palette と違い vendor
   ;; 更新で変わらない種類の値なので、ここに置いても取り残されない）。
   "--hig-font-mono" "ui-monospace,SFMono-Regular,Menlo,monospace"})

(def bridge-rules
  "`hig->dads` を `[selector decls]` の EDN 1 本にしたもの。
  **生 CSS 記法は書かない** — 文字列化は kotoba-lang/css に任せる。

  宣言は map ではなく **pair のベクタ**にする — map に入れ直すとハッシュ順に
  なり `sort-by key` の決定論的な並びが消えるため(css.core/declarations は
  pair の seq をそのまま受ける)。"
  [[":root" (vec (sort-by key hig->dads))]])

(def bridge-css
  "`bridge-rules` を CSS にしたもの。`page` の `:app-css` より **前**に出すこと
  （アプリが個別に上書きしたいときに後勝ちにするため）。"
  (css/css {:rules bridge-rules}))

(def a11y-css
  "DADS 素のままでは `kotoba-lang/design-quality` の決定論 audit が落とす
  3 点の補正。ADR-2607141915 の実測: 素の DADS が 81.97 で、この補正を当てて
  100.00。**アプリ側に毎回書かせない**ためにライブラリへ持ち上げた。

  1. `color-scheme` — light 固定でも「宣言が無い」ことが減点対象。
     `jp-go-dds.page` は meta では出すが、CSS 宣言としても要る。
  2. tap target 44px — DADS の button/summary/input は既定でこれを下回る。
  3. safe-area — 上下左右いずれかが未対応だと減点。ここでは body の
     padding として全辺に効かせる（header などが独自に上書きしてよい）。"
  (css/css
   {:rules
    [[":root" {:color-scheme "light"}]
     [(str ".dads-button,.dads-accordion__summary,.dads-input-text__input,"
           ".dads-textarea__textarea,.dads-checkbox")
      {:min-height 44}]
     ["body" {:padding-left "env(safe-area-inset-left,0px)"
              :padding-right "env(safe-area-inset-right,0px)"
              :padding-bottom "env(safe-area-inset-bottom,0px)"}]]}))

(def skin-css
  "アプリが実際に足すべきもの: `bridge-css` + `a11y-css`。
  `jp-go-dds.page/->page` の `:app-css` の前に連結する。"
  (str bridge-css a11y-css))

(defn- ->custom-property
  "`:color-tint` / `\"color-tint\"` / `\"--hig-color-tint\"` を全て
  `\"--hig-color-tint\"` に正規化する。keyword は `name` を通す — 素の `str`
  だと `\":color-tint\"` になり、先頭のコロンごと変数名に混ざって
  `var(--hig-:color-tint)` という決して解決しない参照を作る（実測）。"
  [token]
  (let [t (if (keyword? token) (name token) (str token))]
    (if (str/starts-with? t "--") t (str "--hig-" t))))

(defn hig-var
  "`--hig-*` を `var(...)` 参照として書くための小さなヘルパ。
  view / SVG は raw hex を書かずこれを通す（kotoba-uiux 規約 2）。

  skin に依存しない — kotoba-ui skin でも DADS skin でも同じ文字列を返し、
  どちらの `--hig-*` が実際に効くかは出している CSS が決める。"
  [token]
  (str "var(" (->custom-property token) ")"))

(defn bridged?
  "`token` がこの skin で DADS に橋渡しされているか。
  橋渡しが無い token を使う view は kotoba-ui skin でしか正しく出ないので、
  アプリのテストからこれを assert できるようにしてある。"
  [token]
  (contains? hig->dads (->custom-property token)))

;; ── primitive-only subset ────────────────────────────────────────────────────

(defn root-css
  "vendored `dds.css` の先頭 `:root { … }` ブロック（DADS primitive の定義）
  だけを切り出す。

  `bridge-css` は `--color-neutral-*` 等の **DADS primitive が既に定義されて
  いる前提**で `--hig-*` を再定義する。したがって bridge を使う側は primitive
  も出さねばならないが、`dads-*` の markup を一切使わないアプリ（既存の
  kotoba-ui ページを DADS の色に寄せるだけの場合）にとって、component CSS を
  含む 72KB 全部を各ページに焼くのは無駄が大きい。実測でこのブロックは 8.6KB。

  **切り出しであって写しではない**のが要点。ここに token を書き写すと
  `scripts/vendor.cljs` の再 vendor から取り残される（このファイル冒頭が
  raw hex を禁じているのと同じ理由）。引数で CSS 文字列を受けるのは cljs/nbb
  からも使えるようにするため。"
  [dds-css]
  (let [start (str/index-of dds-css ":root {")
        end (when start (str/index-of dds-css "\n}" start))]
    (when-not (and start end)
      (throw (ex-info "dds.css に :root ブロックが見つからない — vendor 形式が変わった可能性"
                      {:found-start (some? start)})))
    (subs dds-css start (+ end 2))))

#?(:clj
   (defn root-css-resource
     "`root-css` を vendored resource に対して適用した JVM 向け便宜版。"
     []
     (root-css (slurp (clojure.java.io/resource "jp_go_dds/dds.css")))))
