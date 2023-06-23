
setInterval (() => {
    if('Advertisement' === document.querySelector("[data-testid='npv-header-title']").textContent) {
        document.cookie = "reload=true";
        window.location.reload();
    }
    if(document.cookie === "reload=true") {
        document.cookie = "reload=false";
        document.querySelector("[data-testid='play-button']").click();
    }
}, 500);