const yesButton = document.querySelector("button.yt-spec-button-shape-next.yt-spec-button-shape-next--text.yt-spec-button-shape-next--mono.yt-spec-button-shape-next--size-m[aria-label='Yes']");
setInterval(() => {
    if(!!yesButton) {
        yesButton.click();
        console.log("unpaused");
    }
}, 100);