(def text-color-inactive "#888")

[:body
    {:font-family "Intuitive"
     :overflow-y "scroll"}]

[:.number-text
    {:font-family "Colibri"}]

[:.user-area
    {:float "right"}

    [:.button
        {:color text-color-inactive
         :font-size "18px"
         :margin "0px"
         :display "inline-block"}

        [:&:hover
            {:cursor "pointer"
             :border-bottom (str "1px solid "
                                 text-color-inactive)}]]

    [:.email
        {:color text-color-inactive
         :display "inline-block"
         :margin "0px 10px 0px 0px"
         :font-size "16px"}]]

[:.head-div
    {:position "relative"
     :width "200px"
     :margin-left "auto"
     :margin-right "auto"}

    [:#title
        {:text-transform "uppercase"
         :font-size "2.7em"
         :margin "0px"
         :text-align "center"}]]

[:#navigation
    {:margin "20px auto"
     :display "table"}

    [:.tab-button
        {:font-size "25px"
         :color text-color-inactive
         :display "inline-block"
         :margin "10px 20px"
         :border-bottom "3px solid white"}

        [:&.active
            {:color "#000"
             :border-bottom "3px solid black"}

            [:&:hover
                {:color "#000"
                 :border-bottom "3px solid black"
                 :cursor "default"}]]

        [:&:hover
         {:cursor "pointer"
          :border-bottom (str "3px solid "
                              text-color-inactive)}]]]

[:.hidden {:display "none"}]

[:#ajax-indicator {:visibility "hidden"}

    [:&.visible {:visibility "visible"}]]

[:#dialog
    {:position "fixed"
     :top 0
     :right 0
     :left 0
     :bottom 0}

    [:.darkener
        {:background-color "#000"
         :opacity 0.7
         :position "absolute"
         :top 0
         :right 0
         :left 0
         :bottom 0
         :z-index 90}]

    [:.content-holder
        {:position "absolute"
         :text-align "center"
         :top 0
         :right 0
         :left 0
         :bottom 0
         :z-index 100}

         [:.floater
             {:float "left"
              :height "40%"}]

         [:.content
             {:background-color "#FFF"
              :opacity 1
              :clear "both"
              :margin "auto"
              :padding "15px"
              :border-radius "5px"
              :position "relative"}]]

]
