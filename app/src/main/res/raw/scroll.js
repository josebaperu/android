let val = false;

setInterval (() => {
    if(!val) {
        window.scrollBy(0, 1);
        val = true;

    }else {
        window.scrollBy(0, -1);
        val = false;
    }
}, 60000);