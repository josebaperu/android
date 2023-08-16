setInterval(() => {
    window.alert = function() {};
    window.onbeforeunload = null;


    if(Array.from(document.querySelectorAll("ytm-button-renderer")).filter( x => !!x.childNode && x.childNode[0] === "Yes")) {
        const yesButton = document.querySelector("button.yt-spec-button-shape-next.yt-spec-button-shape-next--text.yt-spec-button-shape-next--mono.yt-spec-button-shape-next--size-m[aria-label='Yes']");
        if(!!yesButton) {
            console.log("reload on yes");
            window.location.reload();
        }
    }

}, 500);

/*
    const playButton = document.querySelector('#play-pause-button');
    if(!!playButton && playButton.getAttribute('aria-label') === 'Play') {
        playButton.click();
        console.log("click playPauseButton");
    }
*/



