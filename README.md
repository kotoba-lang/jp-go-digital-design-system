# jp-go-digital-design-system

[デジタル庁デザインシステム（DADS）の HTML コンポーネント例](https://github.com/digital-go-jp/design-system-example-components-html)
（MIT License, © 2025 デジタル庁）を **cljc hiccup** で提供するライブラリ。
ADR-2607141915（com-junkawasaki/root）。

- markup / class 名（`dads-*`）は上流の HTML 例に忠実。CSS は
  `resources/jp_go_dds/dds.css` に vendor（先頭コメントに上流 commit と MIT 表記。
  `nbb scripts/vendor.cljs <上流 clone>` で再生成 — 手編集禁止）。
- **既定は light 固定**（上流に dark palette は無い）。`page` に `:dark? true`
  を渡すと `jp-go-dds.dark` の反転層が入り、`prefers-color-scheme` と
  `[data-theme]` の両方に対応する。**上流の dark ではなく、こちらの拡張**
  （下記「dark」節）。
- 上流に無い layout 補助（container / section / grid / stack / card / hero）は
  `dds-ext-*` prefix + `core/ext-css` で明確に区別（上流 class と混ぜない）。
- 既定で**外部リクエストゼロ**（Noto Sans JP の Google Fonts 読み込みは
  `:google-fonts? true` の opt-in）。

## 収録コンポーネント（上流 subset）

button / heading / accordion / input-text / textarea / checkbox /
form-control-label / table / chip-label / divider / notification-banner /
select / link / list

上流 41 エントリのうち **40 が正式コンポーネント**(各 `c/c.css` を持つ)で、
`card` だけがパターン(example CSS のみ)。**40 件すべてを vendor 済み**。

## CSS の配り方 —— `dds.css` と `css-for`

この design system の CSS は各ページの `<style>` に **inline** される(外部リクエスト
ゼロが設計方針)。40 component を全部束ねると global 込みで約 172KB になり、
core だけの 72KB から **+100KB**。cloud-itonami だけで 713 ページあるので、
使わない component まで常時配ると全ページが太る。

そこで 2 段構えにしている:

| 用途 | 使うもの | 大きさ |
|---|---|---|
| 既定(core 14 component) | `resources/jp_go_dds/dds.css` | 約 72KB |
| 追加で 1〜2 個だけ要る | `dds.css` + `(jp-go-dds.css/css-for [:date-picker])` | +数 KB |
| 素の markup を広く塗る | `dds.css` + `jp-go-dds.skin/skin-css` | +4KB |
| 全部(ドキュメント用途) | `(jp-go-dds.css/all-css)` | 約 185KB |

`css-for` は **core に既に入っている component を黙って落とす**(同じ custom
property の二重定義を避けるため)。`resources/jp_go_dds/components/<name>.css` に
1 ファイルずつ置いてあるので、nbb など resource が使えない実行系からは
`jp-go-dds.css/component-path` でパスだけ取って呼び出し側で読む。

**core 14**(= `dds.css` に束ねてある): button / heading / accordion / input-text /
textarea / checkbox / form-control-label / table / chip-label / divider /
notification-banner / select / link / list

**per-component で追加できる 26**: blockquote / breadcrumb / calendar / carousel /
date-picker / description-list / disclosure / drawer / emergency-banner /
file-upload / hamburger-menu-button / horizontal-menu / image /
language-selector / menu-list / menu-list-box / modal-dialog / page-navigation /
progress-indicator / radio / resource-list / search-box / step-navigation /
tab / toc / utility-link

`notification-banner` は `:type` に `:success` / `:error` / `:warning` /
`:info-1` / `:info-2` を取り、icon path は上流 `src/components/
notification-banner/{success,error,warning,info-1}.html` をそのまま写している
（`fill="Canvas"` も上流どおり）。**上流との差分が1点だけある**: 上流の例は常に
閉じるボタンを持つが、閉じる挙動は JS 依存なので既定では出さない（静的 SSR
ページに動かないボタンを置かないため）。上流どおりの markup が要る場合は
`:closable? true`（+ `:close-id`）を渡す。

## token 面 — `jp-go-dds.tokens`（共通化の接ぎ目）

このワークスペースの view / SVG / audit は **`--hig-*` token 契約**を共通言語に
している（`shitsuke.hig` が発行元、ADR-2607122200）。DADS が発行するのは
`--color-key-*` / `--color-neutral-*` / `--color-primitive-*` なので、**skin を
DADS に替えると token 契約に忠実に書かれた資産ほど色を失う**。

`jp-go-dds.tokens/skin-css` はその橋渡し（`--hig-*` を DADS primitive の上に
再定義する `:root` 宣言 1 本）＋ design-quality audit の 3 補正
（`color-scheme` 宣言 / tap target 44px / safe-area）をまとめたもの。
**DADS 自身の変数も `dads-*` class も一切上書きしない**ので、skin を kotoba-ui へ
戻すときはこの CSS を出さないだけでよい。

```clojure
(require '[jp-go-dds.tokens :as tokens])
(page/->page {:title "..." :css dds-css :app-css (str tokens/skin-css my-css)} ...)
```

`tokens/hig-var` は `var(--hig-*)` を書くヘルパ、`tokens/bridged?` は
「その token がこの skin で生き残るか」をアプリのテストから assert するための述語。
橋渡しの右辺が vendor 済み CSS に実在することは
`jp-go-dds.tokens-test/every-bridged-primitive-exists-in-vendored-css` が強制する
（存在しない変数を参照しても CSS はエラーを出さず、その宣言だけが黙って無効に
なるため — SVG が不可視になる形でしか表面化しない）。

**dark は表現できない**（上流に dark palette が無い）。dark が要るアプリは
DADS ではなく kotoba-ui skin を選ぶ、が正しい分岐。

## 使い方

```clojure
(require '[jp-go-dds.core :as dds]
         '[jp-go-dds.page :as page])

(page/->page {:title "..." :css (slurp "resources/jp_go_dds/dds.css")} ; css は呼び出し側が読む(純関数維持)
  (dds/container
    (dds/heading 1 "見出し")
    (dds/button "申し込む" {:type :solid-fill :size "lg"})
    (dds/accordion "よくある質問" [:p "回答"] {})))
```

第一消費者: `gftdcojp/ai-gftd-itad`（itad.gftd.ai の LP — kotoba-ui からの
opt-out はオーナー指示 + 行政手続き系サービスとしての信頼感のため。
ADR-2607141915 に理由を明記）。

## dark (`jp-go-dds.dark`) —— 上流には無い。これは拡張

```clojure
(page/->page {:title "..." :css dds-css :dark? true} …)
```

`:dark? true` だけで CSS・`color-scheme`・`theme-color` が揃う。knob を 1 つに
してあるのは、CSS と meta を別フラグにすると「dark の地に light のブラウザ
chrome」というページが必ず生まれるため。

### 色を作らず、ramp を逆から辿る

デジタル庁デザインシステムに dark palette は無い。しかし**無いのは palette で
あって色ではない** —— 上流は 10 色相 + key を 13 段、neutral grey を 12 段
持っており、どの ramp も単調に暗くなる。dark を「同じ ramp を逆から辿ること」と
定義すれば、新しい色を一つも発明せずに dark が出る。規則は 1 本:

    ramp の i 番目 → 同じ ramp の (n-1-i) 番目

index で鏡映するので、grey の `420` `536` のような半端な段にも例外表が要らない
（数値で `N → 1000-N` を計算すると `50 → 950` が存在しない）。

**反転は primitive 層で行う。** `--color-key-*` も `--color-semantic-*` も
`tokens/hig->dads` の `--hig-*` 契約も primitive を指しているので、primitive を
反転すれば **semantic の dark 版も `--hig-*` の dark bridge も component CSS の
書き換えも一切要らない**。`dads-*` の markup はそのまま dark になる。

唯一の例外が `white ↔ black` で、鏡映すると地が純黒になり面がそれ以上下に
行けなくなるため、地は grey の最暗段に落とす。上流から導けないこちらの判断。

### 実測 contrast（`clojure -M:test` が毎回検算する）

地 `#1a1a1a` に対して:

| token | dark 値 | contrast |
|---|---|---|
| `--color-neutral-solid-gray-800`（本文） | `#e6e6e6` | 13.94:1 |
| `--color-neutral-solid-gray-700` | `#cccccc` | 10.84:1 |
| `--color-neutral-solid-gray-600` | `#b3b3b3` | 8.30:1 |
| `--color-neutral-solid-gray-536` | `#999999` | 6.11:1 |
| `--color-key-900`（primary action） | `#9db7f9` | 8.76:1 |
| `--color-semantic-error-1` | `#ff7171` | 6.51:1 |
| `--color-semantic-success-1` | `#259d63` | 5.04:1 |
| `--color-semantic-warning-orange-1` | `#fb5b01` | 5.47:1 |
| `--color-semantic-warning-yellow-1` | `#ebb700` | 9.37:1 |
| solid-fill button（地 key / 字 white） | — | 8.76:1 |
| **反転しない場合の key blue `#0017c1`** | — | **1.57:1** |

最後の行が dark を設計する理由そのもの。デジタル庁の key blue は暗地の上で
2:1 も出ないので、反転しないと primary action が読めない。

この表は説明ではなく**検査**で、`dark_test` が WCAG の相対輝度から毎回計算して
閾値に当てる。上流の再 vendor で palette が動いたらここが落ちる —— 色の劣化は
落ちるテストにしないと誰も気付かない。

### 実装上の落とし穴、2 つとも塞いである

- **循環参照**: 鏡映は対合なので `red-800: var(--red-400)` と
  `red-400: var(--red-800)` を同じ要素に書くと両方 invalid になり色が全部消える。
  light の実値を vendored CSS から**抽出**して `--dds-light-*` に退避し、dark は
  それだけを参照する（抽出であって書き写しではないので、再 vendor に追従する。
  `dark.cljc` に色の literal は 1 つも無く、テストがそれを固定している）。
- **`@media` は specificity を上げない**: `@media (prefers-color-scheme: dark)`
  の中の `:root` は外の素の `:root` と同じ強さなので、後から出る
  `tokens/a11y-css` の `:root { color-scheme: light }` に順序だけで負け、
  **auto-dark が黙って無効化される**。dark 側を `:root:root`、`[data-theme]` 側を
  `:root:root[data-theme=…]` にして、明示指定 > auto > 素の `:root` を
  順序に依存せず固定してある。

## テスト

```bash
nbb --classpath "src:test:../html/src" test/run_tests.cljs
clojure -X:test   # JVM compat
```

## 互換スキン (`jp-go-dds.skin`)

既存の素朴な markup を **書き換えずに** DADS の見た目へ寄せるためのスキン。
cloud-itonami の operator console(各 repo の `render_html.clj` が実 actor 実行から
生成)や一部の product LP は、意味的な HTML と小さな class 語彙
(`.ok` / `.warn` / `.err` / `.critical` / `.muted` / `.card` / `.badge` / `.banner`
/ `.bar` / `.cta-*` …)だけで書かれていて、1 repo ずつ形が違う。構造を組み直すと
セクション欠落の危険があるので、**markup は触らず `<style>` の中身だけ**を
`dds.css` + `skin-css` に差し替える。

```clojure
(require '[jp-go-dds.skin :as skin])
(str vendored-dds-css "\n" skin/skin-css)   ; skin は dds.css の後ろ
```

`skin-rules` も EDN(`[selector decls]` のベクタ列)。raw hex は書かず、判定色は
DADS の semantic token(`--color-semantic-success-2` / `-warning-yellow-2` /
`-error-1`)に載せている。

## kotoba-uiux 規約との関係

このモノレポの標準 UI スタックは kotoba-ui（ADR-2607122200）。本ライブラリは
「デジタル庁デザインシステムに合わせたい日本の公共・行政文脈サービス」向けの
**明示的な opt-out 先**であり、採用する repo は理由を ADR に書くこと。
