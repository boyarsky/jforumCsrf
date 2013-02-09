// rolledback to owasp csrf guard 2 since 3 is failing so much in IE


	var tokenName = '%TOKEN_NAME%';
	var tokenValue = '%TOKEN_VALUE%';

	function updateAnchors()
	{
		updateTag('a','href');
	}
	
	function updateLinks()
	{
		updateTag('link', 'href');
	}
	
	function updateAreas()
	{
		updateTag('area', 'href');
	}
	
	function updateFrames()
	{
		updateTag('frame', 'src');
	}
	
	function updateIFrames()
	{
		updateTag('iframe', 'src');
	}
	
	function updateStyles()
	{
		updateTag('style', 'src');
	}
	
	function updateScripts()
	{
		updateTag('script', 'src');
	}
	
	function updateImages()
	{
		updateTag('img', 'src');
	}
	
	function updateForms()
	{
		var pageTokens = {};
		var forms = document.getElementsByTagName('form');
		
		for(i=0; i<forms.length; i++)
		{
			//var html = forms[i].innerHTML;
			
			//html += '<input type=hidden name=' + tokenName + ' value=' + tokenValue + ' />';
			
			//alert('new html: ' + html);
			
			//forms[i].innerHTML = html;
			
			injectTokenAttribute(forms[i], "action", tokenName, tokenValue, pageTokens);
			
			// added action update from CSRF 3 (needed for posting) - with IE friendly change for adding name to node
			
			// hack to test if action is a string since IE returns [object] when action in form and as hidden field
			// if not a string, assume it is our action and add token for now
			var action = forms[i].getAttributeNode("action").nodeValue;
			if(action != null && isValidUrl(action)) {
				var uri = parseUri(action);
				
				// IE hack from http://stackoverflow.com/questions/1650797/setting-name-of-dom-created-element-fails-in-ie-workaround
				var hidden;
			    try {
			    	hidden = document.createElement('<input type="hidden" name="' + tokenName + '" />');
			    } catch(e) {
			    	hidden = document.createElement("input");
			    	hidden.type = "hidden";
			    	hidden.name = tokenName;
			    }
			    hidden.value = (pageTokens[uri] != null ? pageTokens[uri] : tokenValue);
				forms[i].appendChild(hidden);

			}
			
		}
	}
	
	function updateTag(name,attr)
	{
		var links = document.getElementsByTagName(name);
		
		//alert('length: ' + links.length);
		
		for(i=0; i<links.length; i++)
		{
			var src = links[i].getAttribute(attr);
			
			if(src != null && src != '')
			{
				//alert('found ' + src + '!');
			
				if(isHttpLink(src))
				{
					var index = src.indexOf('?');
				
					if(index != -1)
					{
						src = src + '&' + tokenName + '=' + tokenValue;
					}
					else
					{
						src = src + '?' + tokenName + '=' + tokenValue;
					}
					
					//alert('new src ' + src);
					
					links[i].setAttribute(attr, src);
				}
			}
		}
	}
	
	function isHttpLink(src)
	{
		var result = 0;
		
		if(src.substring(0, 4) == 'http' || src.substring(0, 1) == '/' || src.indexOf(':') == -1)
		{
			result = 1;
		}
		
		return result;
	}
	
	// ---------------------------------------------
	// functions used by csrf 3
	/** check if valid domain based on domainStrict **/
	function isValidDomain(current, target) {
		var result = false;
		
		/** check exact or subdomain match **/
		if(current == target) {
			result = true;
		} else if(%DOMAIN_STRICT% == false) {
			if(target.charAt(0) == '.') {
				result = current.endsWith(target);
			} else {
				result = current.endsWith('.' + target);
			}
		}
		
		return result;
	}

	function getHost(url) {
		// ie 8 wasn't recognizing hostname
		if (/\bMSIE/.test(navigator.userAgent) && !window.opera) {
			url = canonicalizeUrl(url); // damn you, ie6 (and ie 8)
		}
		var a =  document.createElement('a');
		a.href = url;
		return a.hostname; // will return hostname without port!
	}
	
	function canonicalizeUrl(url) { // https://gist.github.com/2428561#gistcomment-306549
		var div = document.createElement('div');
		div.innerHTML = "<a></a>";
		div.firstChild.href = url;
		div.innerHTML = div.innerHTML;
		return div.firstChild.href;
	}

	/** determine if uri/url points to valid domain **/
	function isValidUrl(src) {
		var urlHost = getHost(src);
		return isValidDomain(document.domain, urlHost);
	}

	/** parse uri from url **/
	function parseUri(url) {
		var uri = "";
		var token = "://";
		var index = url.indexOf(token);
		var part = "";
		
		/**
		 * ensure to skip protocol and prepend context path for non-qualified
		 * resources (ex: "protect.html" vs
		 * "/Owasp.CsrfGuard.Test/protect.html").
		 */
		if(index > 0) {
			part = url.substring(index + token.length);
		} else if(url.charAt(0) != '/') {
			part = "%CONTEXT_PATH%/" + url;
		} else {
			part = url;
		}
		
		/** parse up to end or query string **/
		var uriContext = (index == -1);
		
		for(var i=0; i<part.length; i++) {
			var character = part.charAt(i);
			
			if(character == '/') {
				uriContext = true;
			} else if(uriContext == true && (character == '?' || character == '#')) {
				uriContext = false;
				break;
			}
			
			if(uriContext == true) {
				uri += character;
			}
		}
		
		return uri;
	}
	
	/** inject tokens as query string parameters into url **/ 
	function injectTokenAttribute(element, attributeName, tokenName, tokenValue, pageTokens) {
		//var location = element.getAttribute(attributeName);
		// hack - getting same error as on action - don't know why but hack to move forward
		var attr = element.getAttributeNode(attributeName);
		var location = null;
		if ( attr != null) {
			location = attr.nodeValue;
		}
		
		if(location != null && isValidUrl(location)) {
			var uri = parseUri(location);
			var value = (pageTokens[uri] != null ? pageTokens[uri] : tokenValue);
			
			
			if(location.indexOf('?') != -1) {
				location = location + '&' + tokenName + '=' + value;
			} else {
				location = location + '?' + tokenName + '=' + value;
			}
			

			try {
				var attr = document.createAttribute(attributeName);
				attr.nodeValue = location;
				element.setAttributeNode(attr);
			} catch (e) {
				// attempted to set/update unsupported attribute
			}
		}
	}


	
	// ---------------------------------------------
	
	// put in function
	function addCsrfAttributes() {
	updateAnchors();
	updateLinks();
	updateAreas();
	updateFrames();
	updateIFrames();
	//updateStyles();
	//updateScripts();
	//updateImages();
	updateForms();
	}
	
	// i know this is bad coding - trying to get IE working before making it better
	window.onload = addCsrfAttributes;
