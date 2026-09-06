// Reports playback state to PlaybackBridge so the activity knows when a video is
// rolling and PiP auto-enter can be armed.
//
// Injected at document start and again on every onPageFinished; the guard keeps
// the listeners from stacking up.

if (!window.__ytPlaybackHook) {
    window.__ytPlaybackHook = true;

    function reportState(isPlaying) {
        try {
            if (window.AndroidPlayback && window.AndroidPlayback.setPlaying) {
                window.AndroidPlayback.setPlaying(!!isPlaying);
            }
        } catch (e) {
        }
    }

    // "Playing" here means the user wants playback, so buffering still counts -
    // otherwise auto-enter would disarm every time the video stalls.
    function isPlaying(video) {
        return !!video && !video.paused && !video.ended;
    }

    // A YouTube page holds more than one <video> (feed previews), so state comes
    // from all of them: pausing a preview must not report the watch video as stopped.
    function anyPlaying() {
        var videos = document.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
            if (isPlaying(videos[i])) {
                return true;
            }
        }
        return false;
    }

    function onMediaEvent(event) {
        var target = event.target;
        if (target && target.tagName === 'VIDEO') {
            reportState(anyPlaying());
        }
    }

    // Media events do not bubble, but they do capture, so a single listener on the
    // document sees every <video> - including the ones YouTube swaps in on
    // navigation, with no need to re-attach anything.
    var MEDIA_EVENTS = ['play', 'playing', 'pause', 'ended', 'emptied', 'abort'];
    for (var i = 0; i < MEDIA_EVENTS.length; i++) {
        document.addEventListener(MEDIA_EVENTS[i], onMediaEvent, true);
    }

    // Safety net: catches the video element being torn out of the page without a
    // final event, and covers the case where the bridge was not ready at start.
    // The Java side ignores repeats, so re-reporting the same state is free.
    setInterval(function () {
        try {
            reportState(anyPlaying());
        } catch (e) {
        }
    }, 2000);

    reportState(anyPlaying());
}
