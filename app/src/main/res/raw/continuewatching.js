setInterval(() => {
    if(Array.from(document.querySelectorAll("ytm-button-renderer")).filter( x => !!x.childNode && x.childNode[0] === "Yes")) {
        const yesButton = document.querySelector("button.yt-spec-button-shape-next.yt-spec-button-shape-next--text.yt-spec-button-shape-next--mono.yt-spec-button-shape-next--size-m[aria-label='Yes']");
        if(!!yesButton) {
            yesButton.click();
        }
    }
}, 500);