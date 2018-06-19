// URL of server
var serverURL = "http://localhost:9455";

// If we're on Chrome, set browser to Chrome
if (typeof browser == "undefined") {
	var browser = chrome;
}

// Storage area
var storage = browser.storage.local;

// Sends a POST request using XMLHttpRequest. ASYNCHRONOUS!
function postRequest(url, params) {
	return new Promise(function(resolve, reject) {
		// Create XHR
		var xhr = new XMLHttpRequest();
		// Create promise resolution handlers
		xhr.onreadystatechange = function(e) {
			if (xhr.readyState === 4) {
				if (xhr.status === 200) {
					resolve(xhr.response);
				} else {
					reject(xhr.status);
				}
			}
		}
		xhr.ontimeout = function() {
			reject('timeout');
		}
		// Open POST and send params
		xhr.timeout = 300000; // This has to be long for ADD. Here's 5 minutes.
		xhr.open("POST", url, true);
		xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
		xhr.send(params);
	});
}

// Gets the symmetric key. ASYNCHRONOUS!
function getSymmetricKey() {
	return new Promise(function(resolve, reject) {
		storage.get("key", function(result) {
			try {
				resolve(base64js.toByteArray(result.key.data));
			} catch (e) {
				resolve(null);
			}
		});
	});
}

// Updates the symmetric key using keyform.
function updateSymmetricKey() {
	return new Promise(function(resolve, reject) {
		storage.set({"key": {"data": document.getElementById("keyformkey").value}}, function(result) {
			resolve(result);
		});
	});
}

// Gets what account the current site is bound with. ASYNCHRONOUS!
function getSiteBind(currentTab) {
	return new Promise(function(resolve, reject) {
		var bindName = "site_bind_" + encodeURIComponent(currentTab.url.split("?")[0]);
		storage.get(bindName, function(result) {
			try {
				resolve(result[bindName].value);
			} catch (e) {
				resolve(null);
			}
		});
	});
}

// Binds an account to a URL. ASYNCHRONOUS!
function bindToURL(currentTab) {
	return new Promise(function(resolve, reject) {
		// Set site_bind_[url] to account to bind to, then re-show form
		var bindName = "site_bind_" + encodeURIComponent(currentTab.url.split("?")[0]);
		var bindObj = {};
		bindObj[bindName] = {"value": document.getElementById("bindformaccount").value}
		storage.set(bindObj, function(result) {
			showForm(currentTab);
			resolve(result);
		});
	});
}

// Concatentates two uint8arrays
function concatArrays(a, b) {
	var c = new Uint8Array(a.length+b.length);
	c.set(a);
	c.set(b, a.length);
	return c;
}

// Show keyform if we don't have a key yet, bindform if our site is not yet bound, otherwise show getaddform
async function showForm(currentTab) {
	// Get forms and make them all invisible
	var keyform = document.getElementById("keyform");
	var bindform = document.getElementById("bindform");
	var getaddform = document.getElementById("getaddform");
	bindform.style.display = "none";
	getaddform.style.display = "none";
	keyform.style.display = "none";
	// Get symmetric key and site binding, check if they aren't undefined, and display form accordingly.
	var symkey = await getSymmetricKey();
	var bindsite = await getSiteBind(currentTab);
	if (!symkey) {
		keyform.style.display = "block";
	} else if (!bindsite) {
		bindform.style.display = "block";
	} else {
		getaddform.style.display = "block";
	}
}

// Show addonly if our action is add
function checkShowAddOnly() {
	var actionSelect = document.getElementById("action");
	var addOnly = document.getElementById("addonly");
	if (actionSelect.options[actionSelect.selectedIndex].value == "put") {
		addOnly.style.display = "block";
	} else {
		addOnly.style.display = "none";
	}
}

// Run get or add. Only runs GET at the moment. (note: I need somehow to get the key from the user). ASYNCHRONOUS!
function runGetOrAdd() {
	return new Promise(function(resolve, reject) {
		// Get arguments, create JSON to send and encrypt it
		browser.tabs.query({currentWindow: true, active: true}, async function(tabs) {
			var currentTab = tabs[0];
			var accname = await getSiteBind(currentTab);
			var key = await getSymmetricKey();
			var jsonToSend = JSON.stringify({command: "get", acc: accname});
			var nonce = nacl.randomBytes(nacl.secretbox.nonceLength);
			var misckey = nacl.hash(key).slice(0, 32);
			var encryptedJSON = concatArrays(nonce, nacl.secretbox(base64js.toByteArray(btoa(jsonToSend)), nonce, misckey));
			// Create request
			var requestResult = await postRequest(serverURL, "args=" + encodeURIComponent(base64js.fromByteArray(encryptedJSON)));
			// Decode and decrypt request, then parse it
			var resultRaw = base64js.toByteArray(requestResult);
			var nonce = resultRaw.slice(0, nacl.secretbox.nonceLength);
			var jsonReceived = nacl.secretbox.open(resultRaw.slice(nacl.secretbox.nonceLength), nonce, misckey);
			var parsedJSON = JSON.parse(atob(base64js.fromByteArray(jsonReceived)));
			// Copy stuff!
			if (parsedJSON.status == 0) { // Success
				document.body.innerHTML = "Result: " + parsedJSON.message + "<br/>Account:" + parsedJSON.acc + "<br/>Password:" + parsedJSON.pass;
				resolve({message: parsedJSON.message, acc: parsedJSON.acc, pass: parsedJSON.pass});
			} else { // Fail
				document.body.innerHTML = "Error: " + parsedJSON.message;
				reject({message: parsedJSON.message});
			}
		});
	});
}

// Event listeners for each form
document.getElementById("keyform").addEventListener("submit", function(e) {
	e.preventDefault();
	updateSymmetricKey().then(function() {
		browser.tabs.query({currentWindow: true, active: true}, function(tabs) {
			showForm(tabs[0]);
		});
	});
});
document.getElementById("bindform").addEventListener("submit", function(e) {
	e.preventDefault();
	browser.tabs.query({currentWindow: true, active: true}, function(tabs) {
		bindToURL(tabs[0]).then(function() {
			showForm(tabs[0]);
		});
	});
});
document.getElementById("getaddform").addEventListener("submit", function(e) {
	e.preventDefault();
	browser.tabs.query({currentWindow: true, active: true}, function(tabs) {
		runGetOrAdd().then(function() {
			showForm(tabs[0]);
		});
	});
});
// Show form
browser.tabs.query({currentWindow: true, active: true}, function(tabs) {
	showForm(tabs[0]);
});