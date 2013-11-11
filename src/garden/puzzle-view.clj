(def inactive-color "#888")
(def active-color "#000")

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
