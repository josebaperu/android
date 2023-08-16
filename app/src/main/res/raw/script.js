setInterval(() => {
const muteButton = document.querySelector('button.ytp-unmute.ytp-popup.ytp-button');
    if(!!muteButton && muteButton.style.display === '') {
        muteButton.click();
    }
}, 500);