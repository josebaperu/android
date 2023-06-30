
    window.setInterval (() => {
        const play = document.querySelector("[data-testid='play-button']");
        if(localStorage.getItem("reload") === "true") {
            if(!!play) {
                play.click();
 				console.log(".play");
				localStorage.removeItem("reload");

            } else  {
				console.log(".else");
            }
        }
        const ad = document.querySelector("[data-testid='npv-header-title']");

        if(!!ad  &&'Advertisement' === ad.textContent) {
            localStorage.setItem("reload", "true");
            window.location.reload();
            window.clearInterval(this);
        }
        const floatingPlayer = document.querySelectorAll("[data-testid='floating-now-playing-bar'] span");
        if(!!floatingPlayer && floatingPlayer.length === 2) {
            console.log("Playing : " + floatingPlayer[1].innerText + " - " + floatingPlayer[0].innerText);
        } else {
            const tunePlayer = document.querySelectorAll("[data-testid='npv-metadata-container'] a");
            if(!!tunePlayer && tunePlayer.length === 2) {
                console.log("Playing : " + tunePlayer[1].innerText + " - " + tunePlayer[0].innerText);
            }
        }
    }, 1000);