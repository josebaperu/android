if (window.adSkipTimer != null) {
	window.clearInterval(window.adSkipTimer);
}
var sleepInterval = 100;
var adSkipperRepeatInterval = sleepInterval * 2;
var sleep = function () { var now = new Date().getTime(); while ( new Date().getTime() < now + sleepInterval ) {} };

window.adSkipTimer = window.setInterval(function() {
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

        for (const skipButton of skipButtons) {
          skipButton.click();
        }
      }, 10)
    }
  }

  let overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");

  for (let overlayAd of overlayAds) {
    overlayAd.style.visibility = "hidden";
  }

}, adSkipperRepeatInterval);