const observer = new MutationObserver(mutations => {

  let search = document.querySelector("ytmusic-nav-bar[is-mweb]:not([user-logged-in]) .center-content.ytmusic-nav-bar");
  if(!!search && search.style.display !== "block"){
    search.style.display = "block";
  }
  let author = document.querySelector("yt-simple-endpoint style-scope yt-formatted-string:nth-child(2)");
  let track = document.querySelector("yt-formatted-string.ytmusic-player-controls:nth-child(1)");
  if (!!author && !!track) {
      console.log("Playing : " + author.title + " - " + track.title);
  } else {
    let authDesktop = document.querySelector(".byline.style-scope.ytmusic-player-bar.complex-string").title.split("•")[0];
    let titleDesktop = document.querySelector(".byline.style-scope.ytmusic-player-bar.complex-string").title.split("•")[1];
    if (!!authDesktop && !!titleDesktop) {
        console.log("Playing : " + authDesktop + " - " + titleDesktop);
    }
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
