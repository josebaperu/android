    let PLAYING = "Playing : ";
    let PROCESSING = false;
    window.setInterval (() => {
        if(localStorage.getItem("reload") === "true" && PROCESSING === false) {
        let floatingBar = document.querySelector("[data-testid='floating-now-playing-bar']");
            if(!!floatingBar) {
                floatingBar.click();
                    setTimeout(function() {
                        let play = document.querySelector("[data-testid='npv-play-button']");
                        let closeBtn = document.querySelector("button[aria-label='Close']");
                        if(!!play){
                            PROCESSING = true;
                            play.click();
                            closeBtn.click();
                            console.log(PLAYING + "AD SKIPPED IF");
                            localStorage.removeItem("reload");
                            PROCESSING = false;
                        }
                    }, 1800);

            } else if(!floatingBar){
                setTimeout(function() {
                    let playButton = document.querySelector("[data-testid='play-button']");
                    if(!!playButton) {
                        PROCESSING = true;
                        playButton.click();
                        console.log(PLAYING + "AD SKIPPED ELSE IF");
                        localStorage.removeItem("reload");
                        PROCESSING = false;
                    }

                }, 1800);

            } else {
            	console.log(PLAYING + "VOID");
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