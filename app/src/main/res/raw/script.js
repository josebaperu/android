const observer = new MutationObserver(mutations => {
    if(Array.from(document.querySelectorAll("ytm-button-renderer")).filter( x => !!x.childNode && x.childNode[0] === "Yes")) {
        let yesButton = document.querySelector("button.yt-spec-button-shape-next.yt-spec-button-shape-next--text.yt-spec-button-shape-next--mono.yt-spec-button-shape-next--size-m[aria-label='Yes']");
        if(!!yesButton) {
            yesButton.click();
        }
    }
    let muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
    if(!!muteButton && muteButton.style.display === '') {
        muteButton.click();
    }
    let ad = document.querySelector('.ad-showing');
      if (ad) {
        let video = document.querySelector('video');

        if (video) {
          video.currentTime = video.duration;

          setTimeout(() => {
            let skipButtons = document.querySelectorAll(".ytp-ad-skip-button");

            for (let skipButton of skipButtons) {
              skipButton.click();
            }
          }, 10)
        }
      }

    let overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");
    if(overlayAds.length > 0) {
    for (let overlayAd of overlayAds) {
        overlayAd.style.visibility = "hidden";
      }
    }

});
observer.observe(document, { subtree: true, childList: true });
