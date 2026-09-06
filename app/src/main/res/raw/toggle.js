var SELECTORS = [
    "#play-pause-button",
    "#play-pause-button-mweb",
    "yt-icon-button.play-pause-button button",
    ".play-pause-button"
];
for (var i = 0; i < SELECTORS.length; i++) {
    var button = document.querySelector(SELECTORS[i]);
    if (button !== null && button.offsetParent !== null) {
        button.click();
        break;
    }
}
