
// Establish connection with app
let port = browser.runtime.connectNative("print");

port.onMessage.addListener(response => {
    print();
});

browser.runtime.onMessage.addListener(function(msg, sender, sendResponse) {
    print();
});

function print(){
    browser.tabs.query({active: true, currentWindow: true}).then( tabs => {
        browser.tabs.sendMessage( tabs[0].id, {'req':'source-code'}).then( response => {
          port.postMessage(response.content);
        });
    });
}

