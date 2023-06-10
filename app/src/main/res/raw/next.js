        let next = document.querySelector('tp-yt-paper-icon-button.next-button:nth-child(7) > tp-yt-iron-icon:nth-child(1)');
        let nextWeb = document.querySelector("ytmusic-player-bar.style-scope:nth-child(13) > div:nth-child(5) > tp-yt-paper-icon-button:nth-child(5) > tp-yt-iron-icon:nth-child(1)");
        if(!!next && !nextWeb) {
            next.click();
         } else if(!!nextWeb){
            nextWeb.click();
         }