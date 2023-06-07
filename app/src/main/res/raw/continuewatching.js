function eventFire(el, etype){
  if (el.fireEvent) {
    el.fireEvent('on' + etype);
  } else {
    var evObj = document.createEvent('Events');
    evObj.initEvent(etype, true, false);
    el.dispatchEvent(evObj);
  }
}

function fire() {
  btn = document.getElementById('confirm-button');
  if (btn !== null) {
    dlg = btn.parentElement.parentElement.parentElement.parentElement;
    if (dlg.style["display"] !== "none") {
      eventFire(btn, 'click');
      console.log('vid unpaused, HA-HA YT!');
    }
  }
  setTimeout(fire, 250);
}

setTimeout(fire, 250);