// Pause playback. Idempotent: pausing what is already paused is a no-op, so a
// duplicate PAUSE from the system cannot restart anything.
var videos = document.querySelectorAll("video");
var pausedAny = false;
for (var i = 0; i < videos.length; i++) {
    if (!videos[i].paused && !videos[i].ended) {
        videos[i].pause();
        pausedAny = true;
    }
}
if (!pausedAny) {
    var SELECTORS = [
        "#play-pause-button",
        "#play-pause-button-mweb",
        "yt-icon-button.play-pause-button button",
        ".play-pause-button"
    ];
    for (var j = 0; j < SELECTORS.length; j++) {
        var button = document.querySelector(SELECTORS[j]);
        if (button !== null && button.offsetParent !== null) {
            button.click();
            break;
        }
    }
}
