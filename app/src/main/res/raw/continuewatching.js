const yesButton = document.querySelector("button.yt-spec-button-shape-next.yt-spec-button-shape-next--text.yt-spec-button-shape-next--mono.yt-spec-button-shape-next--size-m[aria-label='Yes']");
setInterval(() => {
    if(document.querySelectorAll("ytm-button-renderer")[5]?.childNodes[0]?.textContent === "Yes" && !!yesButton) {
        yesButton.click();
    }
}, 100);