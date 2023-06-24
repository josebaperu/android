
    window.setInterval (() => {
        const play = document.querySelector("[data-testid='play-button']");
        const playp = document.querySelector("[data-test-id='play-pause-button']");
        const shuffle = document.querySelector("[data-testid='shuffle-button']");
		console.log("play " + !!play + " playp " + !!playp + " shuffle " + !!shuffle + " reload " + localStorage.getItem("reload") );
        if(localStorage.getItem("reload") === "true") {

            if(!!playp) {
                playp.click();
				console.log(".playp");
				localStorage.removeItem("reload");

            } else if(!!play) {
                play.click();
 				console.log(".play");
				localStorage.removeItem("reload");

            } else if (!!shuffle) {
				console.log(".shuffle");
                shuffle.click();
				localStorage.removeItem("reload");

            }
        }
        const ad = document.querySelector("[data-testid='npv-header-title']");

        if(!!ad  &&'Advertisement' === ad.textContent) {
            console.log("+++++++++++++++++++++++++++");
            localStorage.setItem("reload", "true");
            window.location.reload();
            window.clearInterval(this);
        }

    }, 1000);

