
(function() {
document.cookie="VISITOR_INFO1_LIVE=oKckVSqvaGw; path=/; domain=.youtube.com";

if (window.adSkipTimer != null) {
	window.clearInterval(window.adSkipTimer);
}

var sleepInterval = 100;
var adSkipperRepeatInterval = sleepInterval * 2.5;
var sleep = function () { var now = new Date().getTime(); while ( new Date().getTime() < now + sleepInterval ) {} };

window.adSkipTimer = window.setInterval(function() {

        if (document.querySelector(".ytp-ad-skip-button.ytp-button") != null) {
            document.querySelector(".ytp-ad-skip-button.ytp-button").click();
        }if (document.querySelector(".ytp-ad-overlay-close-button") != null) {
            document.querySelector(".ytp-ad-overlay-close-button").click();
        }
        if (document.querySelector(".ytp-ad-button-icon") != null) {
            document.querySelector(".ytp-ad-button-icon").click();
            sleep();
            document.querySelector(".ytp-ad-button.ytp-ad-info-dialog-mute-button.ytp-ad-button-link").click();
            sleep();
            document.querySelectorAll(".ytp-ad-feedback-dialog-reason-input")[2].click();
            sleep();
            document.querySelector(".ytp-ad-feedback-dialog-confirm-button").click();
        }
        if (document.querySelector(".style-scope.ytd-popup-container yt-confirm-dialog-renderer a.yt-simple-endpoint.style-scope.yt-button-renderer") != null) {
            document.querySelector(".style-scope.ytd-popup-container yt-confirm-dialog-renderer a.yt-simple-endpoint.style-scope.yt-button-renderer").click();
        }
        if (document.querySelector(".ytd-mealbar-promo-renderer #dismiss-button a.yt-simple-endpoint.style-scope.ytd-button-renderer") != null) {
            document.querySelector(".ytd-mealbar-promo-renderer #dismiss-button a.yt-simple-endpoint.style-scope.ytd-button-renderer").click();
            document.querySelector(".ytd-mealbar-promo-renderer").parentElement.remove();
        }


	    const skip = document.querySelector('.videoAdUiSkipButtonExperimentalText');
        if (skip) skip.click();
        let ad = document.getElementsByClassName("video-ads ytp-ad-module")[0];
        let vid = document.getElementsByClassName("video-stream html5-main-video")[0];
        if(ad==undefined){
            pbRate = vid.playbackRate;
        }
        let closeAble = document.getElementsByClassName("ytp-ad-overlay-close-button");
        for(let i=0;i<closeAble.length;i++){
            closeAble[i].click();
        }
        if(document.getElementsByClassName("style-scope ytd-watch-next-secondary-results-renderer sparkles-light-cta GoogleActiveViewElement")[0]!==undefined){
            let sideAd=document.getElementsByClassName("style-scope ytd-watch-next-secondary-results-renderer sparkles-light-cta GoogleActiveViewElement")[0];
            sideAd.style.display="none";
        }
        if(document.getElementsByClassName("style-scope ytd-item-section-renderer sparkles-light-cta")[0]!==undefined){
            let sideAd_ = document.getElementsByClassName("style-scope ytd-item-section-renderer sparkles-light-cta")[0];
            sideAd_.style.display="none";
        }
        if(document.getElementsByClassName("ytp-ad-text ytp-ad-skip-button-text")[0]!==undefined){
            let skipBtn=document.getElementsByClassName("ytp-ad-text ytp-ad-skip-button-text")[0];
            skipBtn.click();
        }
        if(document.getElementsByClassName("ytp-ad-message-container")[0]!==undefined){
            let incomingAd=document.getElementsByClassName("ytp-ad-message-container")[0];
            incomingAd.style.display="none";
        }
        if(document.getElementsByClassName("style-scope ytd-companion-slot-renderer")[0]!==undefined){
            document.getElementsByClassName("style-scope ytd-companion-slot-renderer")[0].remove();
        }
        if(ad!==undefined){
            if(ad.children.length>0){
                if(document.getElementsByClassName("ytp-ad-text ytp-ad-preview-text")[0]!==undefined){
                    vid.playbackRate=16;
                }
            }
        }




}, adSkipperRepeatInterval);

})();