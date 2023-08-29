    let PLAYING = "Playing : ";
    window.setInterval (() => {
        let play = document.querySelector("[data-testid='play-button']");
        if(localStorage.getItem("reload") === "true") {
            if(!!play) {
                setTimeout(function() {
                    play.click();
 				    console.log(PLAYING + "AD SKIPPED");
				    localStorage.removeItem("reload");
                }, 1800);
            } else  {
				console.log(PLAYING + "NULL");
            }
        } else {
            let ad = document.querySelector("[data-testid='npv-header-title']");

            if(!!ad  &&'Advertisement' === ad.textContent) {
                localStorage.setItem("reload", "true");
                window.location.reload();
                window.clearInterval(this);
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