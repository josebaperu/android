// Resume playback. Idempotent: if something is already rolling this does nothing,
// so a duplicate PLAY from the system cannot flip the video back off.
var videos = document.querySelectorAll("video");
var alreadyPlaying = false;
var target = null;

for (var i = 0; i < videos.length; i++) {
    var candidate = videos[i];
    if (!candidate.paused && !candidate.ended) {
        alreadyPlaying = true;
    }
    // YouTube keeps muted preview <video> elements in feeds, so the watch player
    // is the largest one on the page, not necessarily the first in the DOM.
    if (target === null ||
        candidate.clientWidth * candidate.clientHeight >
        target.clientWidth * target.clientHeight) {
        target = candidate;
    }
}

if (!alreadyPlaying && target !== null) {
    target.play();
}
