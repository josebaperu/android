const observer = new MutationObserver(mutations => {
  let author = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(2)");
  let track = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
  if (!!author && !!track) {
      console.log("Playing : " + author.title + " - " + track.title);
  }
  let muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
  if(!!muteButton && muteButton.style.display === '') {
      muteButton.click();
  }
  let dismissBtn = document.querySelector('yt-button-renderer.dismiss-button.style-scope.ytmusic-mealbar-promo-renderer > yt-button-shape > button > div');
  if(!!dismissBtn){
    dismissBtn.click();
  }

  let movie_player = document.querySelector('ytmusic-player#player');
  if(!!movie_player) {
      movie_player.style.display = 'none';
  }


  let player_container = document.querySelector('#main-panel.ytmusic-player-page');
  if(!!player_container){
      player_container.style.height = 'unset';
  }

  let ad = document.querySelector('.ad-showing');

  if (ad) {
    let video = document.querySelector('video');

    if (video) {
		video.currentTime = video.duration;

		let skipButtons = document.querySelectorAll(".ytp-ad-skip-button");

		for (let skipButton of skipButtons) {
		  skipButton.click();
		}
    }
  }

  let overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");

  for (let overlayAd of overlayAds) {
    overlayAd.style.visibility = "hidden";
  }


});
observer.observe(document, { subtree: true, childList: true });
