// Resume playback. Idempotent: if something is already rolling this does nothing,
// so a duplicate PLAY from the system cannot flip the track back off.
var videos = document.querySelectorAll("video");
for (var i = 0; i < videos.length; i++) {
    if (!videos[i].paused && !videos[i].ended) {
        return;
    }
}
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
        return;
    }
}
var target = document.querySelector("ytmusic-player video") || document.querySelector("video");
if (target !== null && typeof target.play === "function") {
    target.play();
}
