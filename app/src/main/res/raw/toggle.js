        let PLAYING_TOGGLE = "Playing : ";
        let floatingBar = document.querySelector("[data-testid='floating-now-playing-bar']");
        let playButton = document.querySelector("[data-testid='play-button']");

        if(!!floatingBar) {
            floatingBar.click();
            setTimeout(function(){/* Look mah! No name! */
                let play = document.querySelector("[data-testid='npv-play-button']");
                play.click();
            },500);
         } else if(!floatingBar && !!playButton){
             playButton.click();
         }