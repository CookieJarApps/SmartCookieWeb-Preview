function parseQuery(queryString) {
    if (queryString[0] === '?') {
        queryString = queryString.substr(1);
    }
    const query = Object.fromEntries(new URLSearchParams(queryString).entries());
    injectValues(query);
    updateShowSSL(query);
};


function injectValues(queryMap) {
    document.title = queryMap.title;
    document.getElementById('title').innerHTML = queryMap.title;
    document.getElementById('desc').innerHTML = queryMap.description;

    document.getElementById('advancedButton').innerHTML = queryMap.badCertAdvanced;
    document.getElementById('certDesc').innerHTML = queryMap.badCertTechInfo;
    document.getElementById('openAnyway').innerHTML = queryMap.badCertAcceptTemporary;

    document.getElementById('goBack').innerHTML = queryMap.badCertGoBack;
    document.getElementById('tryAgain').innerHTML = queryMap.button;
};

let dropdownOpen = false;

function updateShowSSL(queryMap) {
    const showSSL = queryMap.showSSL;
    if (typeof document.addCertException === 'undefined') {
        document.getElementById('advancedButton').style.display='none';
    } else {
        if (showSSL === 'true') {
            document.getElementById('advancedButton').style.display='block';
        } else {
            document.getElementById('advancedButton').style.display='none';
        }
    }
};

function toggleAdvancedAndScroll() {
    const advancedPanel = document.getElementById('dropdownPanel');
    if (dropdownOpen) {
        advancedPanel.style.display='none';
    } else {
        advancedPanel.style.display='block';
    }
    dropdownOpen = !dropdownOpen;

    const horizontalLine = document.getElementById("hr");
    const advancedPanelAcceptButton = document.getElementById(
        "openAnyway"
    );
    const dropdownPanel = document.getElementById(
        "dropdownPanel"
    );

    if (dropdownPanel.style.display === "block") {
        horizontalLine.hidden = false;
        advancedPanelAcceptButton.scrollIntoView({
            behavior: "smooth",
            block: "center",
            inline: "nearest",
       });
    } else {
        horizontalLine.hidden = true;
    }
};

async function acceptAndContinue(temporary) {
    try {
        await document.addCertException(temporary);
        location.reload();
    } catch (error) {
        console.error("Unexpected error: " + error);
    }
};

document.addEventListener('DOMContentLoaded', function () {
    if (window.history.length == 1) {
        document.getElementById('goBack').style.display = 'none';
    } else {
        document.getElementById('goBack').addEventListener('click', () => window.history.back());
    }

    document.getElementById('tryAgain').addEventListener('click', () => window.location.reload());

    document.getElementById('advancedButton').addEventListener('click', toggleAdvancedAndScroll);
    document.getElementById('openAnyway').addEventListener('click', () => acceptAndContinue(true));
});

parseQuery(document.documentURI);
