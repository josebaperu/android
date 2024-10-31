setInterval(function() {
    let author = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(2)");
    let track = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
    if (!!author && !!track) {
        console.log("Playing : " + author.title + " - " + track.title);
    }
    let ad = document.querySelector('.ad-showing');
    let video = document.querySelector('video');
    let skipButtons = document.querySelectorAll(".ytp-ad-skip-button");
    let overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");
    let search = document.querySelector("ytmusic-nav-bar[is-mweb]:not([user-logged-in]) .center-content.ytmusic-nav-bar");

    let muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
    let dismissBtn = document.querySelector('yt-button-renderer.dismiss-button.style-scope.ytmusic-mealbar-promo-renderer > yt-button-shape > button > div');
    let movie_player = document.querySelector('ytmusic-player#player');
    let player_container = document.querySelector('#main-panel.ytmusic-player-page');
    if (ad) {

        if (video) {
            video.currentTime = video.duration;

            for (let skipButton of skipButtons) {
                skipButton.click();
            }
        }
    }
    for (let overlayAd of overlayAds) {
        if (overlayAd.style.visibility !== "hidden") {
            overlayAd.style.visibility = "hidden";
        }
    }
    if (!!search && search.style.display !== "block") {
        search.style.display = "block";
    }

    if (!!muteButton && muteButton.style.display === '') {
        muteButton.click();
    }
    if (!!dismissBtn) {
        dismissBtn.click();
    }

    if (!!movie_player && movie_player.style.display !== 'none') {
        movie_player.style.display = 'none';
    }


    if (!!player_container && player_container.style.height !== 'unset') {
        player_container.style.height = 'unset';
    }


}, 500);