// When a page is loaded after the print extension is enabled, message the background script to ask if it's the currently selected tab
browser.runtime.sendMessage({"dummy": "dummy"});

// If this tab is currently selected, send the source code of the page to the app for printing
browser.runtime.onMessage.addListener(function (response, sendResponse) {
    browser.runtime.sendNativeMessage("browser", document.documentElement.innerHTML);
});