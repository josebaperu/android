        let toggle = document.querySelector('yt-icon-button.play-pause-button > button:nth-child(1)');
        let toggleWeb = document.querySelector("#play-pause-button-mweb");
        let toggleWebDesktop = document.querySelector("#play-pause-button");

        if(!!toggle) {
            toggle.click();
         }else if(!!toggleWeb){
            toggleWeb.click();
         } else if(!!toggleWebDesktop){
            toggleWebDesktop.click();
         }
