(ns jp-go-dds.page
  "完全な HTML 文書の SSR。light mode 固定(上流 DADS に dark palette は無い —
  color-scheme light を明示し、OS が dark でも light で描画させる)。
  css 文字列(resources/jp_go_dds/dds.css)は I/O を持たない純関数を保つため
  呼び出し側が読んで渡す。"
  (:require [html.core :as html]
            [jp-go-dds.core :as dds]))

(defn page
  "opts: :title :description :lang(既定 \"ja\") :css(vendored dds.css 文字列、必須)
  :app-css(追加 CSS) :head(追加 head hiccup) :google-fonts?(既定 false —
  外部リクエストゼロを既定にし、true で Noto Sans JP を読み込む)"
  [{:keys [title description lang css app-css head google-fonts?]
    :or {lang "ja"}} & body]
  [:html {:lang lang}
   (into
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1, viewport-fit=cover"}]
     [:meta {:name "color-scheme" :content "light"}]
     [:meta {:name "theme-color" :content "#ffffff"}]
     [:title (or title "")]
     (when description [:meta {:name "description" :content description}])
     (when google-fonts?
       (list [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
             [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
             [:link {:rel "stylesheet"
                     :href "https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400..700&display=swap"}]))
     [:style [:hiccup/raw (str css "\n" dds/ext-css "\n" (or app-css ""))]]]
    (or head []))
   (into [:body] body)])

(defn ->page
  "hiccup → 完全な HTML 文書文字列(doctype 付き)。"
  [opts & body]
  (str "<!DOCTYPE html>\n" (html/->html (apply page opts body))))
