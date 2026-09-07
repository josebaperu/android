package com.webview.monochrome;

/**
 * Page-side playback control for monochrome.tf. Kept as Java source so it
 * does not live under res/raw.
 */
final class PlaybackJs {
    private PlaybackJs() {}

    static final String HOOK = """
            if (!window.__monoCtl) {
                window.__monoCtl = true;

                function mediaNodes() {
                    return document.querySelectorAll('audio, video');
                }

                function isPlaying() {
                    var nodes = mediaNodes();
                    for (var i = 0; i < nodes.length; i++) {
                        if (!nodes[i].paused && !nodes[i].ended) {
                            return true;
                        }
                    }
                    return false;
                }

                function firstMedia() {
                    var nodes = mediaNodes();
                    var fallback = null;
                    for (var i = 0; i < nodes.length; i++) {
                        if (!nodes[i].paused && !nodes[i].ended) {
                            return nodes[i];
                        }
                        if (!fallback) {
                            fallback = nodes[i];
                        }
                    }
                    return fallback;
                }

                function fireSkipKey(isNext) {
                    var active = document.activeElement;
                    if (active && active !== document.body && typeof active.blur === 'function') {
                        active.blur();
                    }
                    var key = isNext ? 'ArrowRight' : 'ArrowLeft';
                    var keyCode = isNext ? 39 : 37;
                    var opts = {
                        key: key,
                        code: key,
                        keyCode: keyCode,
                        which: keyCode,
                        bubbles: true,
                        cancelable: true,
                        composed: true,
                        shiftKey: true,
                        ctrlKey: false,
                        altKey: false,
                        metaKey: false,
                        view: window
                    };
                    var ev = new KeyboardEvent('keydown', opts);
                    try {
                        if (!ev.shiftKey) {
                            Object.defineProperty(ev, 'shiftKey', { value: true });
                        }
                        if ((ev.key || '').toLowerCase() !== key.toLowerCase()) {
                            Object.defineProperty(ev, 'key', { value: key });
                        }
                    } catch (e) {
                    }
                    (document.body || document.documentElement).dispatchEvent(ev);
                }

                window.__monoPlay = function () {
                    if (isPlaying()) {
                        return;
                    }
                    var m = firstMedia();
                    if (m && typeof m.play === 'function') {
                        var p = m.play();
                        if (p && typeof p.catch === 'function') {
                            p.catch(function () {});
                        }
                    }
                };

                window.__monoPause = function () {
                    var nodes = mediaNodes();
                    for (var i = 0; i < nodes.length; i++) {
                        if (!nodes[i].paused && !nodes[i].ended) {
                            nodes[i].pause();
                        }
                    }
                };

                window.__monoToggle = function () {
                    if (isPlaying()) {
                        window.__monoPause();
                    } else {
                        window.__monoPlay();
                    }
                };

                window.__monoNext = function () {
                    fireSkipKey(true);
                };

                window.__monoPrev = function () {
                    fireSkipKey(false);
                };

                function currentTrack() {
                    var titleEl = document.querySelector('.now-playing-bar .title');
                    var authorEl = document.querySelector('.now-playing-bar .artist');
                    var title = titleEl ? (titleEl.textContent || '').trim() : '';
                    var author = authorEl ? (authorEl.textContent || '').trim() : '';
                    if (title === 'Select a song') {
                        title = '';
                    }
                    return { title: title, author: author };
                }

                var lastPlaying = null;
                var lastTitle = '';
                var lastAuthor = '';

                function report() {
                    try {
                        if (!window.AndroidPlayback) {
                            return;
                        }
                        var playing = isPlaying();
                        if (lastPlaying !== playing) {
                            lastPlaying = playing;
                            window.AndroidPlayback.setPlaying(playing);
                        }
                        var track = currentTrack();
                        if (track.title !== lastTitle || track.author !== lastAuthor) {
                            lastTitle = track.title;
                            lastAuthor = track.author;
                            window.AndroidPlayback.setTrack(track.author, track.title);
                        }
                    } catch (e) {
                    }
                }

                var events = ['play', 'playing', 'pause', 'ended', 'emptied', 'abort'];
                for (var e = 0; e < events.length; e++) {
                    document.addEventListener(events[e], report, true);
                }
                setInterval(report, 2000);
                report();
            }
            """;
}
