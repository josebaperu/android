  let search = document.querySelector("ytmusic-nav-bar[is-mweb]:not([user-logged-in]) .center-content.ytmusic-nav-bar");
  let author = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(2)");
  let track = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
  let muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
  let dismissBtn = document.querySelector('yt-button-renderer.dismiss-button.style-scope.ytmusic-mealbar-promo-renderer > yt-button-shape > button > div');
  let movie_player = document.querySelector('ytmusic-player#player');
  let player_container = document.querySelector('#main-panel.ytmusic-player-page');
  let skipButtons = document.querySelectorAll(".ytp-ad-skip-button");
  let overlayAds = document.querySelectorAll(".ytp-ad-overlay-slot");

const observer = new MutationObserver(mutations => {
  !!document.querySelectorAll('.image') && document.querySelectorAll('.image').length > 0 ? document.querySelectorAll('.image').forEach( i => i.style.display = 'none'): '';
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

  let ad = document.querySelector('.ad-showing');

  if (ad) {
    let video = document.querySelector('video');

    if (video) {
		video.currentTime = video.duration;


		for (let skipButton of skipButtons) {
		  skipButton.click();
		}
    }
  }


  for (let overlayAd of overlayAds) {
    overlayAd.style.visibility = "hidden";
  }


});
observer.observe(document, { subtree: true, childList: true });
