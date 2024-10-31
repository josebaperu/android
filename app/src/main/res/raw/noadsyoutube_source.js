const observer = new MutationObserver(mutations => {
  var ad = document.querySelector('.ad-showing');
  if (ad) {
    var video = document.querySelector('video');

    if (video) {
		video.currentTime = video.duration;

        var skipButtons = document.querySelectorAll(".ytp-ad-skip-button");
		for (var skipButton of skipButtons) {
		  skipButton.click();
		}
    }
  }
  var overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");
  for (var overlayAd of overlayAds) {
    overlayAd.style.visibility = "hidden";
  }
});
observer.observe(document, { subtree: true, childList: true });


setTimeout(() => {
    var search = document.querySelector("ytmusic-nav-bar[is-mweb]:not([user-logged-in]) .center-content.ytmusic-nav-bar");
    var author = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(2)");
    var track = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
    var muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
    var dismissBtn = document.querySelector('yt-button-renderer.dismiss-button.style-scope.ytmusic-mealbar-promo-renderer > yt-button-shape > button > div');
    var movie_player = document.querySelector('ytmusic-player#player');
    var player_container = document.querySelector('#main-panel.ytmusic-player-page');
      if(!!search){
        search.style.display = "block";
      }

      if (!!author && !!track) {
          console.log("Playing : " + author.title + " - " + track.title);
      }
      if(!!muteButton && muteButton.style.display === '') {
          muteButton.click();
      }
      if(!!dismissBtn){
        dismissBtn.click();
      }

      if(!!movie_player) {
          movie_player.style.display = 'none';
      }


      if(!!player_container){
          player_container.style.height = 'unset';
      }

}, 500);
