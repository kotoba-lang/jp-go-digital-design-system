(ns jp-go-dds.css
  "必要な component の CSS だけを合成するための API。

  なぜ必要か: この design system の CSS は各ページの `<style>` に **inline** される
  (外部リクエストゼロが設計方針)。上流 40 component を全部束ねると global 込みで
  約 172KB になり、現行の `dds.css`(global + core 14 component、約 72KB)から
  **+100KB**。cloud-itonami だけで 713 ページあるので、使わない component まで
  常時配ると全ページが太る。

  そこで **上流の全 component を 1 ファイルずつ vendor しておき**
  (`resources/jp_go_dds/components/<name>.css`)、ページごとに使う分だけ
  合成する。`dds.css` は従来どおり global + core の束のまま変えていないので、
  既存ページは 1 バイトも変わらない。

  使い分け:
  - 追加 component が要らない → 従来どおり `dds.css` をそのまま読む
  - 1〜2 個だけ足したい      → `(css-for [:date-picker])` を app-css の前に足す
  - 素の markup を広く塗る   → `jp-go-dds.skin`

  nbb など resource が使えない実行系からは `component-path` でパスだけ取り、
  読み込みは呼び出し側が行う(このライブラリは I/O を持たない純関数を保つ方針)。"
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io])))

(def core-components
  "`dds.css` に既に束ねてある component。これらを `css-for` に渡す必要はない。"
  [:button :heading :accordion :input-text :textarea :checkbox
   :form-control-label :table :chip-label :divider :notification-banner
   :select :link :list])

(def core-component-set (set core-components))

(defn component-path
  "component 名 → resource 相対パス。`io/resource` にも
  `<repo>/resources/` からの相対パスにもそのまま使える。"
  [c]
  (str "jp_go_dds/components/" (name c) ".css"))

(def global-path "jp_go_dds/global.css")

(defn extra-components
  "`components` のうち `dds.css` に入っていないものだけを、渡した順で返す。
  `dds.css` を読んだ上で追加合成する場合はこちらを使う(core を二重に入れない)。"
  [components]
  (vec (remove core-component-set (map keyword components))))

#?(:clj
   (defn- slurp-resource [p]
     (if-let [r (io/resource p)]
       (slurp r)
       (throw (ex-info (str "jp-go-dds: resource が無い: " p
                            " —— 上流に存在しない component 名か、"
                            "scripts/vendor.cljs の再実行が要る")
                       {:path p})))))

#?(:clj
   (defn css-for
     "`components` の CSS を連結して返す(渡した順)。

     `:global? true`(既定 false)で `global.css` を先頭に付ける。`dds.css` を
     別途読んでいるなら global は既に入っているので false のままにすること
     —— 二重に入れると同じ custom property を 2 回定義することになる。

     `dds.css` に既に入っている core component を渡しても黙って落とす
     (二重定義を避けるため)。"
     ([components] (css-for components {}))
     ([components {:keys [global?] :or {global? false}}]
      (let [extras (extra-components components)]
        (str/join "\n"
                  (cond-> []
                    global? (conj (slurp-resource global-path))
                    :always (into (map (comp slurp-resource component-path) extras))))))))

#?(:clj
   (defn all-css
     "global + 上流の全 component。**ページに inline するには重い**(約 172KB)ので、
      ドキュメント生成やカタログ表示など、重さが問題にならない用途にだけ使う。"
     []
     (str (slurp-resource global-path) "\n"
          (str/join "\n" (map (comp slurp-resource component-path)
                              (read-string (slurp-resource "jp_go_dds/components.edn")))))))
