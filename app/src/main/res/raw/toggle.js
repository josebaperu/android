        let toggle = document.querySelector('yt-icon-button.play-pause-button > button:nth-child(1)');
        let toggleWeb = document.querySelector("#play-pause-button-mweb");
        if(!!toggle && !toggleWeb) {
            toggle.click();
         }else if(!!toggleWeb){
            toggleWeb.click();
         }
