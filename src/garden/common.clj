[:body
    {:font-family "Intuitive"
     :overflow-y "scroll"}]

[:.number-text
    {:font-family "Colibri"}]

[:.header
    {:text-transform "uppercase"
     :font-size "2.7em"
     :margin "0px"
     :text-align "center"}]

[:.head-div
    {:position "relative"
     :width "200px"
     :margin-left "auto"
     :margin-right "auto"}]

[:#navigation
    {:margin "20px auto"
     :display "table"}

    [:.tab-button
        {:font-size "25px"
         :color "#888"
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
          :border-bottom "3px solid #888"}]]]

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
