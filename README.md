# jp-go-digital-design-system

[デジタル庁デザインシステム（DADS）の HTML コンポーネント例](https://github.com/digital-go-jp/design-system-example-components-html)
（MIT License, © 2025 デジタル庁）を **cljc hiccup** で提供するライブラリ。
ADR-2607141915（com-junkawasaki/root）。

- markup / class 名（`dads-*`）は上流の HTML 例に忠実。CSS は
  `resources/jp_go_dds/dds.css` に vendor（先頭コメントに上流 commit と MIT 表記。
  `nbb scripts/vendor.cljs <上流 clone>` で再生成 — 手編集禁止）。
- **light mode 固定**（上流に dark palette は無い。`page` が
  `color-scheme: light` を明示するので OS が dark でも light で描画される）。
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
