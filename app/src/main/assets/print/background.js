browser.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    // If the tab the content script was run on is selected, tell the content script that it can send the page source to the app
    browser.tabs.query({active: true, currentWindow: true})
        .then(tabs => {
            for (const tab of tabs) {
                browser.tabs.sendMessage(tab.id, tab.title);
            }
        });
});