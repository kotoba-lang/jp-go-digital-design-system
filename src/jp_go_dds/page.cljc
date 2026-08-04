(ns jp-go-dds.page
  "完全な HTML 文書の SSR。既定は light 固定(上流 DADS に dark palette は無い)。

  `:dark? true` を渡すと `jp-go-dds.dark` の反転層を差し込み、`color-scheme`
  と `theme-color` を両モード分に切り替える。**knob は 1 つだけ**にしてある
  —— CSS を出すかどうかと meta をどう書くかを別々のフラグにすると、
  「dark の CSS は入っているが meta は light」というページが必ず生まれる
  （ブラウザ UI だけ白いまま、スクロール端で地色が破綻する）。

  css 文字列(resources/jp_go_dds/dds.css)は I/O を持たない純関数を保つため
  呼び出し側が読んで渡す。"
  (:require [html.core :as html]
            [jp-go-dds.core :as dds]
            [jp-go-dds.dark :as dark]))

(defn page
  "opts: :title :description :lang(既定 \"ja\") :css(vendored dds.css 文字列、必須)
  :app-css(追加 CSS) :head(追加 head hiccup) :google-fonts?(既定 false —
  外部リクエストゼロを既定にし、true で Noto Sans JP を読み込む)
  :dark?(既定 false — true で prefers-color-scheme と [data-theme] に対応する)"
  [{:keys [title description lang css app-css head google-fonts? dark?]
    :or {lang "ja"}} & body]
  (let [;; 値を焼かずに引く。dark の地は `--color-neutral-white` が dark で
        ;; 解決した先そのものなので、palette が再 vendor で動けばここも動く。
        surface (when dark? (dark/resolve-dark css "--color-neutral-white"))]
    [:html {:lang lang}
     (into
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1, viewport-fit=cover"}]
       [:meta {:name "color-scheme" :content (if dark? "light dark" "light")}]
       ;; theme-color は media 付きで 2 本出す。1 本だけだと OS が dark の
       ;; ときにブラウザ chrome だけ light のまま残る。
       (if dark?
         (list [:meta {:name "theme-color" :content "#ffffff"
                       :media "(prefers-color-scheme: light)"}]
               [:meta {:name "theme-color" :content surface
                       :media "(prefers-color-scheme: dark)"}])
         [:meta {:name "theme-color" :content "#ffffff"}])
       [:title (or title "")]
       (when description [:meta {:name "description" :content description}])
       (when google-fonts?
         (list [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
               [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
               [:link {:rel "stylesheet"
                       :href "https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400..700&display=swap"}]))
       ;; html.core は style/script を raw-text tag として無エスケープ出力する —
       ;; 子は素の文字列で渡す(:hiccup/raw で包むとベクタごと文字列化され CSS が壊れる。実測)
       ;;
       ;; 順序: 上流 → ext → dark → app。dark を app の前に置くのは、アプリが
       ;; 個別に上書きしたいときに後勝ちにするため(tokens/bridge-css と同じ規約)。
       [:style (str css "\n" dds/ext-css "\n"
                    (when dark? (str (dark/dark-css css) "\n"))
                    (or app-css ""))]]
      (or head []))
     (into [:body] body)]))

(defn ->page
  "hiccup → 完全な HTML 文書文字列(doctype 付き)。"
  [opts & body]
  (str "<!DOCTYPE html>\n" (html/->html (apply page opts body))))
