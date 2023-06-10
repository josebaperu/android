setInterval(function() {
    'use strict';
    if (document.getElementsByClassName('line-text style-scope yt-confirm-dialog-renderer').length >= 1) {
        for (let i = 0; i < document.getElementsByClassName('line-text style-scope yt-confirm-dialog-renderer').length; i++) {
            if (document.getElementsByClassName('line-text style-scope yt-confirm-dialog-renderer')[i].innerText == "Video paused. Continue watching?") {
                document.getElementsByClassName('line-text style-scope yt-confirm-dialog-renderer')[i].parentNode.parentNode.parentNode.querySelector('#confirm-button').click()
            }
        }
    }
}, 250)();