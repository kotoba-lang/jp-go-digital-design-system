(ns jp-go-dds.behavior-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [jp-go-dds.behavior :as behavior]))

(deftest language-selector-behavior-contract
  (testing "the shared script binds only to the public component hooks"
    (doseq [hook ["data-language-selector]"
                  "data-language-selector-opener]"
                  "data-language-selector-popup]"
                  "data-language-selector-item]"]]
      (is (str/includes? behavior/language-selector-script hook))))
  (testing "keyboard and dismissal behaviour remain part of the component"
    (doseq [key ["Escape" "ArrowDown" "ArrowUp" "Home" "End"]]
      (is (str/includes? behavior/language-selector-script key)))
    (is (str/includes? behavior/language-selector-script "aria-expanded"))
    (is (str/includes? behavior/language-selector-script "popup.hidden"))))
