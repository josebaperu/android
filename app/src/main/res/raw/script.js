if (window.adSkipTimer != null) {
	window.clearInterval(window.adSkipTimer);
}
let sleepInterval = 100;
let adSkipperRepeatInterval = sleepInterval * 3.5;
let sleep = function () { var now = new Date().getTime(); while ( new Date().getTime() < now + sleepInterval ) {} };
const PLAYING = "Playing : ";

window.adSkipTimer = window.setInterval(function() {
	let nowPlayingLink = document.querySelector(".now-playing a");
	let theresAnAd = !!nowPlayingLink && nowPlayingLink.hostname !== location.hostname;
    let playPauseButton = document.querySelector("button[title='Play'], button[title='Pause']");
	if (!!theresAnAd) {
		playPauseButton.click();
		setTimeout(() => {
			let playButton = document.querySelector("button[title='Play']");
			playButton && playButton.click();
		}, adSkipperRepeatInterval);
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


}, adSkipperRepeatInterval);