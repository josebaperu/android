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

if (clickSkip(".next-button") || clickSkip("[aria-label='Next']")) {
    // clicked
} else {
    var bar = document.querySelector("ytmusic-player-bar");
    if (bar && typeof bar.onNextButtonTap_ === "function") {
        bar.onNextButtonTap_();
    } else {
        var api = bar && (bar.playerApi || bar.playerApi_);
        if (api && typeof api.nextVideo === "function") {
            api.nextVideo();
        }
    }
}
