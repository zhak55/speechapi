/*
* speechapi - Javascript frontend for use in on-line speech-to-text and text-to-speech.
*
* Copyright (C) 2010 Speechapi - http://www.speechapi.com
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*
* Contact: spencer@speechapi.com
* Contact: elias.majic@speechapi.com
*
*/

var speechapi = {
	username: null,
	password: null,
	recognizerSetup : false,
	documentReady: false,
	onResultCallback: null,
	onTtsCallback: null,
	onLoadedCallback: null,
	containerID: null,
	server: null,
	automatic: false,
	initNotCalled: true,
	ruleIndex : 1,
	grammars : {},

	setup: function(username, password, result, tts, onLoaded, containerID) {
		speechapi.username = username;
		speechapi.password = password;

		speechapi.recognizerSetup = false;	
		speechapi.documentReady = false;
	
		speechapi.onLoadedCallback = onLoaded;
		speechapi.onResultCallback = result;
		speechapi.initNotCalled=true;

		speechapi.onFinishTTSCallback = tts;
	
		speechapi.containerID = containerID;	
	
		speechapi.automatic=false;
	},

	setup2: function(username, password, result, tts, onLoaded, containerID, altContainerID, server,swffile) {
		var flashvars = {speechServer : server};
        	var params = {allowscriptaccess : "always"};
		var attributes = {};
		attributes.id =containerID; 
		swfobject.embedSWF(swffile, altContainerID, "215", "138", "9.0.28", false,flashvars, params, attributes);
		speechapi.setup(username,password,onResult,onFinishTTS, onLoaded, containerID); 
	},
			

	result: function(result) {
        	if (result == null) {
           		result =new Object();
           		result.text = "Recognition Error";
           		result.rCode="Error";
           		result.rMessage="Null result received from server";
        	} 
		//var jsonResult = eval('(' + result + ')');
		if(speechapi.onResultCallback != null)
			speechapi.onResultCallback(result);
	},

	setOnResult: function(obj) {
		if (eval("typeof " + obj + " == 'function'")) {
			speechapi.onResultCallback = obj;
		} else {
			alert('setOnResult needs to have a callback function that exists!');
		}
	},

	finishTTS: function() {
		if(speechapi.onFinishTTSCallback != null)
			speechapi.onFinishTTSCallback();
	},

	setOnFinishTTS: function(obj) {
		if (eval("typeof " + obj + " == 'function'"))
		{
			speechapi.onFinishTTSCallback = obj;
		}
		else
		{
			alert('setOnFinishTTS needs to have a callback function that exists!');
		}	
	},


	loaded: function () {
	        document.getElementById(speechapi.containerID).initFS(speechapi.username, speechapi.password, 'speechapi.result', 'speechapi.finishTTS');
		if(speechapi.onLoadedCallback != null)
			speechapi.onLoadedCallback();
	},

	setOnLoaded: function(obj) {
		if (eval("typeof " + obj + " == 'function'")) {
			speechapi.onLoadedCallback = obj;
		} else {
			alert('setOnResult needs to have a callback function that exists!');
		}
	},


	startVxmlAppUrl: function(appUrl,callback) {
		document.getElementById(speechapi.containerID).startVxmlAppUrl(appUrl,callback);
	},


	startVxmlAppText: function(app,callback) {
		document.getElementById(speechapi.containerID).startVxmlAppText(app,callback);
	},


	setupRecognition: function(grammarType, grammar, automatic) {

		if (typeof automatic === 'undefined') automatic = speechapi.automatic;
	
		if(typeof grammarType == 'string' && typeof grammar == 'string') {
                	gType = grammarType.toUpperCase();
                	if ((gType == 'SIMPLE') || (gType == 'JSGF')) {
                    		if (grammar.length > 0) {
                        		speechapi.recognizerSetup = true;
		        		document.getElementById(speechapi.containerID).setupRecognition(grammarType,grammar, automatic)
                		} else {
		       			alert('Empty grammar string');
				}
                	} else {
		   		alert('Invalid grammar type '+grammarType);
                	}
		} else {
			alert('Grammar mode and/or string must be a string.');
		}	
	},

	startRecognition: function() {
        	if (speechapi.recognizerSetup) {
  	   		document.getElementById(speechapi.containerID).startRecognition();
        	} else {
			alert('Setup Grammars with setupRecognition() method before calling startRecognition()');
        	}
	},

	stopRecognition: function() {
		document.getElementById(speechapi.containerID).stopRecognition();
	},

	speak: function(text, speaker) {
		document.getElementById(speechapi.containerID).speak(text, speaker);
	},



	processRule: function(rulename, tag) {
		if (speechapi.grammars[rulename]) {
			speechapi.grammars[rulename].callback(speechapi.grammars[rulename].text, tag);
		} else {
			alert("unhandled rulename/tag "+rulename+"/"+tag);
		}
	},


	resetGrammar: function() {
		speechapi.grammars = {};
	},

	// similar to Array.join() but joins the keys of an
	// associative array instead of the array values
	joinKeys: function(map, separator) {
		var result = "";
		var count = 0;

		for (var x in map) {
			if (count++ > 0)
				result += separator;
			result += x;
		}

		return result;
	},

	constructGrammar: function() {
		var grammar = "#JSGF V1.0;\n";
		grammar += "grammar mashup;\n";

		//grammar += "public <command> = [<pre>] (<";
		grammar += "public <command> =  (<";
		grammar += speechapi.joinKeys(speechapi.grammars, "> | <");
		grammar += ">);\n";

		for (var rulename in speechapi.grammars) {
			grammar += ( "<" + rulename + "> = " + speechapi.grammars[rulename].text + ";\n" );
		}
		//alert(grammar);
		return grammar;
	},

	addJsgfGrammar: function(text, callback) {
		var rulename = 'id' + speechapi.ruleIndex++;
		speechapi.grammars[rulename] = {"text" : text, "callback" : callback};
		return rulename;
	}

};

