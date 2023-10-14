document.addEventListener("DOMContentLoaded", function(event) {
  // START of copy button java script
  // Source: https://remarkablemark.org/blog/2021/06/01/add-copy-code-to-clipboard-button-to-jeyll-site/
  var codeBlocks = document.querySelectorAll('pre');

  codeBlocks.forEach(function (codeBlock) {
    var copyButton = document.createElement('button');
    copyButton.className = 'copy';
    copyButton.type = 'button';
    copyButton.ariaLabel = 'Copy code to clipboard';
    copyButton.innerText = 'Copy';

    codeBlock.append(copyButton);

    copyButton.addEventListener('click', function () {
      var code = codeBlock.querySelector('code').innerText.trim();
      if(window.navigator.clipboard && window.isSecureContext) {
        window.navigator.clipboard.writeText(code);
      } else {
        // Source: https://stackoverflow.com/questions/51805395/navigator-clipboard-is-undefined
        // text area method
        let textArea = document.createElement("textarea");
        textArea.value = code;
        // make the textarea out of viewport
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        // here the magic happens
        document.execCommand('copy')
        textArea.remove();
      }

      copyButton.innerText = 'Copied';
      var fourSeconds = 4000;

      setTimeout(function () {
        copyButton.innerText = 'Copy';
      }, fourSeconds);
    });
  });
  // END start of copy button java script
});
