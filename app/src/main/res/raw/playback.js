// Reports play/pause and track metadata to PlaybackBridge.
//
// Injected at document start and again on every onPageFinished; the guard
// keeps the listeners from stacking up.

if (!window.__ytmPlaybackHook) {
    window.__ytmPlaybackHook = true;

    function reportState(isPlaying) {
        try {
            if (window.AndroidPlayback && window.AndroidPlayback.setPlaying) {
                window.AndroidPlayback.setPlaying(!!isPlaying);
            }
        } catch (e) {
        }
    }

    function reportTrack(author, title) {
        try {
            if (window.AndroidPlayback && window.AndroidPlayback.setTrack) {
                window.AndroidPlayback.setTrack(author || "", title || "");
            }
        } catch (e) {
        }
    }

    function isPlaying(video) {
        return !!video && !video.paused && !video.ended;
    }

    function anyPlaying() {
        var videos = document.querySelectorAll("video");
        for (var i = 0; i < videos.length; i++) {
            if (isPlaying(videos[i])) {
                return true;
            }
        }
        return false;
    }

    function onMediaEvent(event) {
        var target = event.target;
        if (target && target.tagName === "VIDEO") {
            reportState(anyPlaying());
        }
    }

    var MEDIA_EVENTS = ["play", "playing", "pause", "ended", "emptied", "abort"];
    for (var i = 0; i < MEDIA_EVENTS.length; i++) {
        document.addEventListener(MEDIA_EVENTS[i], onMediaEvent, true);
    }

    function currentTrack() {
        var titleEl = document.querySelector("yt-formatted-string.title.ytmusic-player-bar")
            || document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
        var authorEl = document.querySelector("yt-formatted-string.byline.ytmusic-player-bar")
            || document.querySelector("yt-formatted-string.subtitle.ytmusic-player-bar")
            || document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(2)");
        if (!titleEl && !authorEl) {
            return null;
        }
        return {
            title: (titleEl && (titleEl.title || titleEl.textContent)) || "",
            author: (authorEl && (authorEl.title || authorEl.textContent)) || ""
        };
    }

    var lastAuthor = "";
    var lastTitle = "";

    function poll() {
        try {
            reportState(anyPlaying());
            var track = currentTrack();
            if (track && (track.author !== lastAuthor || track.title !== lastTitle)) {
                lastAuthor = track.author;
                lastTitle = track.title;
                reportTrack(track.author, track.title);
            }
        } catch (e) {
        }
    }

    setInterval(poll, 2000);
    poll();
}
