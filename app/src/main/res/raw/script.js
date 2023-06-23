function set_cookie(name, value) {
  document.cookie = name +'='+ value +'; Path=/;';
}
function delete_cookie(name) {
  document.cookie = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}

setInterval (() => {
    const add = document.querySelector("[data-testid='npv-header-title']");
    const play = document.querySelector("[data-testid='play-button']");
    const shuffle = document.querySelector("[data-testid='shuffle-button']");

    if(!!add  &&'Advertisement' === add.textContent) {
		set_cookie("reload","true");
        window.location.reload();
    }
    if(document.cookie.includes("reload=true")) {

		if(!!play) {
			play.click();
			set_cookie("reload","false");
		} else if(!!shuffle) {
			shuffle.click();
			set_cookie("reload","false");
		}
    }
}, 1000);