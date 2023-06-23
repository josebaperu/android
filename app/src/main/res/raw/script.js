    function set_cookie(name, value) {
      document.cookie = name +'='+ value +'; Path=/;';
    }
    function delete_cookie(name) {
      document.cookie = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    }

    window.setInterval (() => {
        const ad = document.querySelector("[data-testid='npv-header-title']");
        const play = document.querySelector("[data-testid='play-button']");
        const playp = document.querySelector("[data-test-id='play-pause-button']");
        const shuffle = document.querySelector("[data-testid='shuffle-button']");

        console.log("cookie " + document.cookie.includes("reload=true"));

        if(document.cookie.includes("reload=true")) {
            if(!!playp) {
                console.log(".playp");
                playp.click();
            } else if(!!play) {
                console.log(".play");
                play.click();
            } else if (!!shuffle) {
                console.log(".shuffle");
                shuffle.click();
            }
            set_cookie("reload","false");
        }

        if(!!ad  &&'Advertisement' === ad.textContent) {
            console.log(".if");
            set_cookie("reload","true");
            window.location.href = "https://www.reloadthispage.com";
            window.clearInterval(this);
        }
    }, 1000);

