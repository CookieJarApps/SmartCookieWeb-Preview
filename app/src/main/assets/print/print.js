function codeToInject() {
    window.print = function() {
        alert("This page called window.print() to attempt to print the page. This feature is currently unsupported in this browser. Please use the built-in print function.")
     };
}

function embed(fn) {
    const script = document.createElement("script");
    script.text = `(${fn.toString()})();`;
    document.getElementsByTagName('head')[0].appendChild(script);
}

embed(codeToInject);

browser.runtime.onMessage.addListener(request => {
  var response = '';
  if(request.req === 'source-code') {
    response = document.documentElement.innerHTML;
  }

  return Promise.resolve({content: response});
});

