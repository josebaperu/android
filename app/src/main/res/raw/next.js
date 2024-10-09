        let next = document.querySelector('tp-yt-paper-icon-button.next-button:nth-child(7) > tp-yt-iron-icon:nth-child(1)');
        let nextWeb = document.querySelector("ytmusic-player-bar.style-scope:nth-child(13) > div:nth-child(5) > tp-yt-paper-icon-button:nth-child(5) > tp-yt-iron-icon:nth-child(1)");
        let nextWebDesktop = document.querySelector(".next-button");
        if(!!next) {
            next.click();
        } else if(!!nextWeb){
            nextWeb.click();
        }else if(!!nextWebDesktop){
          nextWebDesktop.click();
        }