    let PLAYING = "Playing : ";
    let attempts = 5;
    window.setInterval (() => {
        if(localStorage.getItem("reload") === "true") {
        let floatingBar = document.querySelector("[data-testid='floating-now-playing-bar']");
        let playButton = document.querySelector("[data-testid='play-button']");
            if(!!floatingBar) {
                floatingBar.click();
                    setTimeout(function() {
                        let play = document.querySelector("[data-testid='npv-play-button']");
                        if(!!play){
                           play.click();
                            console.log(PLAYING + "AD SKIPPED IF");
                            localStorage.removeItem("reload");
                            attempts = 5;
                        }
                    }, 1800);

            } else if(!!playButton){
                playButton.click();
                setTimeout(function() {
                    let floatingBar = document.querySelector("[data-testid='floating-now-playing-bar']");
                    if(!!floatingBar) {
                        floatingBar.click();
                        console.log(PLAYING + "AD SKIPPED ELSE IF");
                        localStorage.removeItem("reload");
                        attempts = 5;
                    }

                }, 1800);

            }else if(attempts > 0){
                attempts--;
				console.log(PLAYING + "ATTEMPTS " +attempts);
                window.location.reload(true);
            } else {
            	console.log(PLAYING + "ELSE");
            }
        } else {
            let ad = document.querySelector("[data-testid='npv-header-title']");

            if(!!ad  && 'Advertisement' === ad.textContent) {
                localStorage.setItem("reload", "true");
                window.location.reload(true);
            }
            let floatingPlayer = document.querySelectorAll("[data-testid='floating-now-playing-bar'] span");
            if(!!floatingPlayer && floatingPlayer.length === 2) {
                console.log(PLAYING + floatingPlayer[1].innerText + " - " + floatingPlayer[0].innerText);
            } else {
                let tunePlayer = document.querySelectorAll("[data-testid='npv-metadata-container'] a");
                if(!!tunePlayer && tunePlayer.length === 2) {
                    console.log(PLAYING + tunePlayer[1].innerText + " - " + tunePlayer[0].innerText);
                }
            }
        }

    }, 1000);