        let PLAYING_NEXT = "Playing : ";
        let floatingBarNext = document.querySelector("[data-testid='floating-now-playing-bar']");
        let playButtonTop = document.querySelector("[data-testid='play-button']");
        if(!!floatingBarNext) {
            floatingBarNext.click();
            setTimeout(function(){/* Look mah! No name! */
                let nextt = document.querySelector("[data-testid='npv-skip-forward-button']");
                nextt.click();
            },500);
        } else if(!!playButtonTop) {
            playButtonTop.click();
        }
