function clickSkip(selector) {
    var bar = document.querySelector("ytmusic-player-bar") || document;
    var hosts = bar.querySelectorAll(selector);
    for (var i = 0; i < hosts.length; i++) {
        var host = hosts[i];
        var btn = host.querySelector("button")
            || (host.shadowRoot && host.shadowRoot.querySelector("button"))
            || host;
        if (btn.getAttribute && btn.getAttribute("aria-disabled") === "true") {
            continue;
        }
        btn.click();
        return true;
    }
    return false;
}

if (clickSkip(".previous-button") || clickSkip("[aria-label='Previous']")) {
    // clicked
} else {
    var bar = document.querySelector("ytmusic-player-bar");
    if (bar && typeof bar.onPreviousButtonTap_ === "function") {
        bar.onPreviousButtonTap_();
    } else {
        var api = bar && (bar.playerApi || bar.playerApi_);
        if (api && typeof api.previousVideo === "function") {
            api.previousVideo();
        }
    }
}
