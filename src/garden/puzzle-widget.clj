(require '[garden.units :refer [px px*]])

(def inactive-color "#888")
(def active-color "#000")
(def light-green "#c4faaf")
(def light-yellow "#fffbcc")
(def border-color "gray")


[:#puzzle

    [:#puzzle-table {:border-collapse "collapse"}

        [:td :th {:cursor "pointer"}]

        [:.solved.num :.num-clicked {:background-color light-green}]]

    [:.center
        {:display "block"
         :margin "0 auto"
         :text-align "center"}]

    [:table
        {:margin-right "auto"
         :margin-left "auto"}

        [:td
            {:border (str "1px solid " border-color)
             :overflow "hidden"
             :white-space "nowrap"

             :-moz-user-select "-moz-none"
             :-khtml-user-select "none"
             :-webkit-user-select "none"
             :-ms-user-select "none"
             :user-selectnone "none"}]]

    (for [[type size] [[:.small-cells 16]
                       [:.medium-cells 24]
                       [:.large-cells 32]]]
      [type [:td
                {:width (px size)
                 :height (px size)
                 :font-size (px* size 3/4)}]])

    [:#puzzle-view
        {:margin-right "auto"
         :margin-left "auto"}]

    [:&.filled-blot [:.filled
        {:background-image "url(\"/static/img/clicked.png\")"
         :background-repeat "no-repeat"
         :background-position "50% 50%"
         :background-size "100%"}]]

    [:&.filled-square [:.filled
        {:background-color "black"}]]

    [:&.crossed-cross [:.crossed
        {:background-image "url(\"/static/img/cross.png\")"
         :background-repeat "no-repeat"
         :background-position "50% 50%"
         :background-size "100%"}]]

    [:&.crossed-dot [:.crossed
        {:background-image "url(\"/static/img/dot.png\")"
         :background-repeat "no-repeat"
         :background-position "50% 50%"
         :background-size "100%"}]]

    [:.button-container {:text-align "center"}

        [:.button
             {:font-size "16px"
              :color inactive-color
              :display "inline-block"
              :margin "0px 20px 15px 0px"}

             [:&:hover
                  {:color active-color
                   :cursor "pointer"}]]

        [:.image
            {:width "20px"
             :height "20px"
             :vertical-align "top"
             :display "inline-block"}]]

    (doall (for [dir ["top" "bottom" "left" "right"]]
       [(str ".thick-" dir)
        {(str "border-" dir) (str "2px solid " border-color)}]))

    [:td.hide {:border "1px solid white"}

        [:&.has-right {:border-right (str "1px solid " border-color)}]

        [:&.has-left {:border-left (str "1px solid " border-color)}]]

    [":not(.footer).last"
        [:td.hide
            {:border-bottom (str "1px solid " border-color)}]]

    [:.footer.first
        [:td.hide
            {:border-top (str "1px solid " border-color)}]]

    [:td.highlighted {:background-color light-yellow}]

    [:.no-puzzle
        {:text-align "center"
         :font-size "23px"
         :margin-top "30px"}]]
