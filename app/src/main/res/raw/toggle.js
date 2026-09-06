// Flip playback state. Only used for an explicit user toggle; the system's
// transport controls send PLAY or PAUSE instead so repeats stay harmless.
var videos = document.querySelectorAll("video");
var anyPlaying = false;
var target = null;

for (var i = 0; i < videos.length; i++) {
    var candidate = videos[i];
    if (!candidate.paused && !candidate.ended) {
        anyPlaying = true;
    }
    // The watch player is the largest video; feeds keep muted previews around.
    if (target === null ||
        candidate.clientWidth * candidate.clientHeight >
        target.clientWidth * target.clientHeight) {
        target = candidate;
    }
}

if (anyPlaying) {
    for (var j = 0; j < videos.length; j++) {
        videos[j].pause();
    }
} else if (target !== null) {
    target.play();
}
