 setInterval( () => {
 
 
  	let mainDocument = document.getElementsByClassName(
        "video-ads ytp-ad-module"
    );
    let playerOverlay = document.getElementsByClassName(
        "ytp-ad-player-overlay"
    );
    let imageOverlay = document.getElementsByClassName(
        "ytp-ad-image-overlay"
    );

    let skipBtn = document.getElementsByClassName(
        "ytp-ad-skip-button ytp-button"
    );

    let newSkipBtn = document.getElementsByClassName(
        "ytp-ad-skip-button-modern ytp-button"
    );

    let videoDocument = document.getElementsByClassName(
        "video-stream html5-main-video"
    );

    let textOverlay = document.getElementsByClassName("ytp-ad-text-overlay");

    let playerAds = document.getElementById("player-ads");

    function handleSkipBtn() {
        if (skipBtn.length > 0) {
            skipBtn[0].click();
        }
    }

    function handleNewSkipBtn() {
        if (newSkipBtn.length > 0) {
            newSkipBtn[0].click();
        }
    }

    if (mainDocument.length > 0) {
        handleSkipBtn();
        handleNewSkipBtn();
        if (playerOverlay.length > 0) {
            playerOverlay[0].style.visibility = "hidden";
            for (let i = 0; i < videoDocument.length; i++) {
                if (videoDocument[i] && videoDocument[i].duration) {
                    videoDocument[i].currentTime = videoDocument[i].duration;
                }
            }
            handleSkipBtn();
            handleNewSkipBtn();
        }
        if (imageOverlay.length > 0) {
            imageOverlay[0].style.visibility = "hidden";
        }
    }

    if (playerAds) {
        playerAds.style.display = "none";
    }

    if (textOverlay.length > 0) {
        textOverlay[0].style.display = "none";
    }
 
	console.log("running");
 
 
 
 
 }, 1000); 
