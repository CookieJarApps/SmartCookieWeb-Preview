function parseQuery(queryString) {
    if (queryString[0] == '?') {
        queryString = queryString.substr(1);
    }
    const query = Object.fromEntries(new URLSearchParams(queryString).entries());
    injectValues(query);
};

function injectValues(queryMap) {
    document.title = queryMap.title;
    document.getElementById('title').innerHTML = queryMap.title;
    document.getElementById('description').innerHTML = queryMap.description;
    document.getElementById('tryAgain').innerHTML = queryMap.button;
};

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('tryAgain').addEventListener('click', () => window.location.reload());
});

parseQuery(document.documentURI);
