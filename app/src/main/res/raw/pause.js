// Pause playback. Idempotent: pausing what is already paused is a no-op, so a
// duplicate PAUSE from the system cannot restart anything.
var videos = document.querySelectorAll("video");

for (var i = 0; i < videos.length; i++) {
    videos[i].pause();
}
