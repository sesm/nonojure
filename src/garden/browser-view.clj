[:#browser
    {:text-align "center"
     :margin-left "auto"
     :margin-right "auto"
     :width "85%"}

    [:.thumbnail
        {:text-align "center"
         :display "inline-block"
         :width "230px"
         :height "230px"
         :margin "10px"
         :padding-bottom "10px"
         :float "left"
         :position "relative"}

        [:&:hover
            {:background-color "#EEE"
             :cursor "pointer"}]

        [:.canvas-holder-inner
            {:display "table-cell"
             :vertical-align "middle"}]

        [:.canvas-holder-outer
            {:display "table"
             :height "230px"
             :width "100%"}]

        [:.description
            {:height "20px"
             :bottom "20px"
             :position "relative"}

            [:.size :.difficulty
                {:margin-right "10px"
                 :display "inline"}]

            [:&:before
                {:display "inline-block"
                 :margin-right "5px"
                 :vertical-align "middle"}]]

        [:&.solved

            [:.description:before {:content "url('/static/img/solved.png')"}]]

        [:&.in-progress

            [:.description:before {:content "url('/static/img/in-progress.png')"}]]]

    [:.filtering
        {:width "500px"
         :text-align "left"
         :margin-left "auto"
         :margin-right "auto"
         :font-size "20px"}

        [:.item
            {:display "inline-block"
             :width "70px"
             :text-align "center"}]

        [:a
            {:color "#888"
             :cursor "pointer"}

            [:&:hover {:border-bottom "2px solid #888"}]

            [:&.selected
                {:border-bottom "2px solid black"
                 :color "black"
                 :font-size "20px"
                 :cursor "default"}]]

        [:.type
            {:width "80px"
             :text-align "right"
             :display "inline-block"}]]]
