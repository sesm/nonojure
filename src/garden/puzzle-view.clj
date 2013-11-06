 (require '[garden.units :refer [px px*]])

(def inactive-color "#888")
(def active-color "#000")
(def light-green "#c4faaf")
(def light-yellow "#fffbcc")

[:#puzzle

    [:#puzzle-table {:border-collapse "collapse"}

        [:td :th {:cursor "pointer"}]

        [:.solved.num :.num-clicked {:background-color light-green}]]

    [:.containter
        {:padding "30px"
         :margin "0px 60px 50px 50px"}]

    [:.center
        {:display "block"
         :margin "0 auto"
         :text-align "center"}]

    [:table
        {:margin-right "auto"
         :margin-left "auto"}

        [:td
            {:border "1px solid black"
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

    [:.filled
        {:background-image "url(\"/static/img/clicked.png\")"
         :background-repeat "no-repeat"
         :background-position "50% 50%"
         :background-size "20px 20px"}]

    [:.crossed
        {:background-image "url(\"/static/img/cross.png\")"
         :background-repeat "no-repeat"
         :background-position "50% 50%"
         :background-size "25px 25px"}]

    [:.button-container {:text-align "center"}

        [:.button
             {:font-size "16px"
              :color inactive-color
              :display "inline-block"
              :margin "0px 20px 15px 0px"}

             [:&:hover
                  {:color active-color
                   :cursor "pointer"}]]]

    (for [dir ["top" "bottom" "left" "right"]]
      [(str ".thick-" dir)
       {(str "border-" dir) "2px solid black"}])

    [:td.hide {:border "1px solid white"}

        [:&.has-right {:border-right "1px solid black"}]

        [:&.has-left {:border-left "1px solid black"}]]

    [":not(.footer).last"
        [:td.hide {:border-bottom "1px solid black"}]]

    [:.footer.first
        [:td.hide {:border-top "1px solid black"}]]

    [:td.highlighted {:background-color light-yellow}]]

    [:.no-puzzle
        {:text-align "center"
         :font-size "23px"
         :margin-top "30px"}]

[:#dialog

    [:#solved
        {:text-align "center"
         :display "inline-block"}

        [:p {:margin "0px"}]

        [:.solved-caption
            {:font-size "30px"
             :margin "10px 0px"}]

        [:.invitation
            {:font-size "20px"
             :margin-bottom "5px"}]

        [:.choices

            [:p
                {:display "inline-block"
                 :font-size "20px"
                 :margin "0px 5px"
                 :color inactive-color}

                [:&:hover
                 {:color active-color
                  :cursor "pointer"}]]]]]
