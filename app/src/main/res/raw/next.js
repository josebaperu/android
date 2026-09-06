// Skip to the next video.
//
// The old single nth-child chain into the mobile control overlay broke on any
// markup change, so try the stable class names first, then the player API, and
// keep the positional selector only as a last resort.
var SELECTORS = [
    ".ytp-next-button",
    "button.ytm-next-button",
    "#player-control-overlay > div > div:nth-child(5) > player-middle-controls > div > div:nth-child(5) > button"
];

var clicked = false;

for (var i = 0; i < SELECTORS.length && !clicked; i++) {
    var button = document.querySelector(SELECTORS[i]);
    // offsetParent is null for a hidden control, which would be a no-op click.
    if (button !== null && button.offsetParent !== null) {
        button.click();
        clicked = true;
    }
}

if (!clicked) {
    var player = document.getElementById("movie_player") ||
        document.querySelector(".html5-video-player");
    if (player !== null && typeof player.nextVideo === "function") {
        player.nextVideo();
    }
}
