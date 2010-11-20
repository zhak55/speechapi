/*
* speechapi - Flash frontend for use in on-line speech-to-text and text-to-speech.
*
* Copyright (C) 20010 Spencer Lord
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 3 of the License, or
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
*
*/

package com.spokentech {

	import mx.core.UIComponent;
	import mx.managers.PopUpManager;
	import flash.display.Sprite;
	import flash.external.ExternalInterface;
	import com.spokentech.HttpSpeech;
        import flash.system.SecurityDomain;
	import flash.system.Security;
	import flash.media.Microphone;
	import com.adobe.serialization.json.JSON;
	import org.osflash.thunderbolt.Logger;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.net.Responder;
	import flash.utils.Timer;
	import flash.utils.*;
	import flash.media.SoundTransform;

	//import flash.display.*;
	//import flash.display.MovieClip;
	import flash.display.SimpleButton;
	import flash.events.AsyncErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.external.*;
	//import flash.media.Camera;
	import flash.media.Microphone;
	//import flash.media.Video;
	import flash.system.Security;
	import flash.system.SecurityDomain;
	//import flash.text.*;
	import mx.controls.Alert;
	import mx.core.UIComponent;
	import mx.managers.PopUpManager;
	import flash.events.ActivityEvent;
	import flash.events.Event;
	import flash.events.StatusEvent;
	import flash.events.TimerEvent;
	
	public class Speech  {
	
		private var username:String = null;
		private var password:String = null;

		private var recCallback:String;
		private var ttsCallback:String;
		private var vxmlCallback:String;
	        private var recCallbackAir:Function;
		private var ttsCallbackAir:Function ;

		private var inSpeech:Boolean = false;
		private var http:Boolean = true;

		private var httpSpeech:HttpSpeech;

		private var speechServer:String =null;
		private var automatic:Boolean;
		private var speechTimer:Timer; // 1 second
			
		private var nc:NetConnection;	
		private var ns:NetStream;
		private var ns2:NetStream;
		private var streamName:String = "";

		private var mic:Microphone = Microphone.getMicrophone();
		private var recognizerReady:Boolean = true;

		//TODO: Do we really need separate callbacks?  maybe one will do.
	        private var micActivityCallback:Function;
	        private var micActivityCallbackHttp:Function;

		public function init(speechServer: String, micActivityCallback:Function,micActivityCallbackHttp:Function):void {
			//get the connection info passed in as a flash param
			this.speechServer = speechServer;
			Logger.info("Init method",this.speechServer); 
			this.micActivityCallback = micActivityCallback;
			this.micActivityCallbackHttp = micActivityCallbackHttp;

			if (!this.speechServer.indexOf("http")) {
			//if (this.speechServer.substr(0,3) == "http" ) {
				http = true;
				initHttp();
			} else {                   //default to rtmp
				http=false;
				initRtmp();
			}
		}	


		private function initHttp():void {
	
			//Security.loadPolicyFile('http://spokentech.net/static/crossdomain.xml');
			//Security.allowDomain('*');
			//Security.allowInsecureDomain('*');

			httpSpeech = new HttpSpeech();

			if (ExternalInterface.available) {
				ExternalInterface.addCallback("initFS", initFS);
				ExternalInterface.addCallback("speak", speak);
				ExternalInterface.addCallback("setupRecognition", setupRecognition);
				ExternalInterface.addCallback("startRecognition", startRecognition);
				ExternalInterface.addCallback("stopRecognition", stopRecognition);
			} else {
				trace("External if not available");
			}

			//This is called on js side after all is initialized		
			//TODO: Find a better way.  Don't like that the js mehod is hardcoded.
			if (ExternalInterface.available) {
				ExternalInterface.call("speechapi.loaded");
			}

		}
	  

		private function initRtmp():void {
			//Security.allowDomain("*");	
			nc = new NetConnection();
			nc.client=this;
			nc.connect(this.speechServer);
			nc.addEventListener(NetStatusEvent.NET_STATUS, netStatusHandler);
			nc.call("getStreamName", new Responder(streamNameResult, null));
		}

		public function streamNameResult(message:String):void {
			streamName = message;
			publishRtmpStream(message);

			if (ExternalInterface.available) {
				ExternalInterface.addCallback("initFS", initFS);
        			ExternalInterface.addCallback("setupRecognition", setupRecognition);
        			ExternalInterface.addCallback("startRecognition", startRecognition);
        			ExternalInterface.addCallback("stopRecognition", stopRecognition);
       				ExternalInterface.addCallback("speak", speak);
       				ExternalInterface.addCallback("startVxmlAppUrl", startVxmlAppUrl);
        			ExternalInterface.addCallback("startVxmlAppText", startVxmlAppText);

				ExternalInterface.call("speechapi.loaded");
			} else {
              			setTimeout(streamNameResult, 500, message);
			}
		}	
				
		public function publishRtmpStream(streamName:String):void {
			try {
				mic.rate = 8;
				mic.gain=80;
				mic.addEventListener(ActivityEvent.ACTIVITY, micActivityCallback);
				mic.setLoopBack(true);
				mic.setSilenceLevel(10,1000);
				//mic.addEventListener(StatusEvent.STATUS, status);
				//mic.addEventListener(Event.ACTIVATE, active);
				//mic.addEventListener(Event.ENTER_FRAME, updateVolLevel);
				mic.setUseEchoSuppression(true);
				var tf:SoundTransform = new SoundTransform(0);
				mic.soundTransform = tf;
				ns.attachAudio(mic);
				ns.publish(streamName, "live");	
			} catch(err:Error) {
				Alert.show( err.toString() ); 
			}
		}

		//this method gets called by javascript during the initialization sequence after the flash is loaded
		//TODO:  Document the sequence of events at init time.
		public  function initFS(username:String, password:String, recCallback:String, ttsCallback:String):void { 
	        	this.recCallback = recCallback;
			this.ttsCallback = ttsCallback;
			initCommon(username,password);
		}

		// 
		public  function initSal(username:String, password:String, recCallback:Function, ttsCallback:Function,
                                         micActivityCallback:Function,micActivityCallbackHttp:Function):void { 
	        	this.recCallbackAir = recCallback;
			this.ttsCallbackAir = ttsCallback;
			initCommon(username,password);						//init part two (common part)
		}

		// initAir is combined to one method, no waiting required.
		public  function initAir(username:String, password:String, recCallback:Function, ttsCallback:Function,
                                         speechServer: String, micActivityCallback:Function,micActivityCallbackHttp:Function):void { 
	        	this.recCallbackAir = recCallback;
			this.ttsCallbackAir = ttsCallback;
                        init(speechServer,micActivityCallback,micActivityCallbackHttp);		//init part one all common with flex
			initCommon(username,password);						//init part two (common part)
		}


		private function initCommon(username:String, password:String):void {
			this.username=username;
	        	this.password = password;
			if (http) {
				try {
	        	  		httpSpeech.configure(username,password,recognitionComplete,ttsComplete,speechServer,micActivityCallbackHttp);
        			} catch(err:Error) {
			  		Logger.info("initFS: ",err.message);
				}
			} else {

			}

		}


		public function startVxmlAppUrl(vurl:String,callback:String):void {
			if (http) {
				Logger.info("vxml not implemented");
			} else {
				this.vxmlCallback = callback;
				nc.call("startVxmlAppUrl", null, streamName, vurl);
			}
		}
		
		public function startVxmlAppText(vxml:String,callback:String):void {
			if (http) {
				Logger.info("vxml not implemented");
			} else {
				this.vxmlCallback = callback;
				nc.call("startVxmlAppText", null, streamName, vxml);
			}
		}
			
		public function stopVxmlApp():void {
			if (http) {
				Logger.info("vxml not implemented");
			} else {
				Alert.show("stopping vxml app");
				nc.call("stopVxmlApp", null, streamName );
			}
		}

		public function sendDtmf(dtmf:String):void {
			if (http) {
				Logger.info("vxml not implemented");
			} else {
				Alert.show(dtmf);
				nc.call("sendDtmf", null, streamName, dtmf);
			}
		}
	

		public  function speak(text:String, speaker:String):void {
			if (http) {
				httpSpeech.playAudio(text,speaker);
			} else {
				nc.call("speak", new Responder(speakResult, null),streamName, text, speaker);
			}
		}

		public  function cancelSpeak():void {
			if (http) {
				//TODO: add http cancel method here.  but speak is non-blocking so maybe not worth
				//it till http speak becomes non-blocking`
				//httpSpeech.playAudio(text,speaker);
			} else {
				nc.call("stopSpeaking", null,streamName);
			}
		}


		public function setupRecognition(gmode:String,grammar:String, auto:Boolean):void {
			if (http) {
				//Logger.info("setupRecog method ",grammar); 
	    			httpSpeech.setGmode(gmode)
	    			httpSpeech.setGrammar(grammar)
	    			if(auto) {
					this.automatic=true;
					startRecognition()
	    			}
			} else {
				var credentials:Array = new Array();
				credentials[0]=username;
				credentials[1]=password;
				var grammarArray:Array = new Array();
				grammarArray[0]=grammar;
				grammarArray[1]=gmode;
				this.automatic=auto;
				nc.call("initializeSettings", null, credentials, grammarArray, auto, streamName);			
				if(automatic) {
					automatic=true;
					startRecognition()
				}
			}
        	}

		public function startRecognition():void {
			if (http) {
	    			//Logger.info("startRecognition","hello"); 
				httpSpeech.startMicRecording();
	    			speechTimer = new Timer(15000, 1);
	    			speechTimer.addEventListener(TimerEvent.TIMER, timeOutRecRequest);
	    			speechTimer.start();
			} else {
				nc.call("startRecognition", null, streamName, false);
				speechTimer = new Timer(15000, 1);
				speechTimer.addEventListener(TimerEvent.TIMER, timeOutRecRequest);
				speechTimer.start();
			}
        	}

		public function stopRecognition():void {
			if (http) {
				//Logger.info("stopRecognition","hello"); 
	    			httpSpeech.stopMicRecording();
	    			speechTimer.stop();
			} else {
				nc.call("stopRecognition", null, streamName);
				speechTimer.stop();
			}
		}


		private function timeOutRecRequest(event:TimerEvent):void  {
			stopRecognition();
		}

		public function recognitionComplete(results:String):void {

			var resultObj:Object = JSON.decode(results) ;
			if (resultObj.rCode != "Success") {
		      	resultObj.text = "Recognition Error";
			}
	    		if (ExternalInterface.available) {
				ExternalInterface.call(recCallback, resultObj); 
			}
			if (recCallbackAir != null) 
			     recCallbackAir(resultObj);
        	}

		public function ttsComplete():void {
	    		if (ExternalInterface.available) {
	       			ExternalInterface.call(ttsCallback); 
	    		}
			if (ttsCallbackAir != null) 
			     ttsCallbackAir();
        	}

		public function updateVolLevel(event:Event):void {
			return;
		} 


		private function asyncErrorHandler(event:AsyncErrorEvent):void  {
			trace(event.text);
		}

		private function soundCompleteHandler(event:Event):void  {
			//var position = 0;  /huh?
		}

		//necessary red5 method
		public function onBWDone():void  {}


		
		//*******************************************************************************************************
		// These methods are invoked from the Red5 RTMP server.  They either take some action upon the netstream
		// or call the js code using ExternalInterface.call() method
		//*******************************************************************************************************

		//TODO: Remove (if not needed -- don think it is)
		public function flashReady():void {
			if (ExternalInterface.available) {
				ExternalInterface.call("flashReady");
			}
		}
			
		public  function playAudio(filename:String):void {
			if (ns2 == null) {
				ns2 = new NetStream(nc);       
				ns2.addEventListener(AsyncErrorEvent.ASYNC_ERROR, asyncErrorHandler);
				ns2.addEventListener( NetStatusEvent.NET_STATUS, netStatusHandler );
			}
			ns2.play("mp3:" + filename);
		}

		public function passResults(result:String):void {
			var resultObj:Object = JSON.decode(result) ;
			if (resultObj.rCode != "Success") {
		      		resultObj.text = "Recognition Error";
			}

			if (ExternalInterface.available) {
				ExternalInterface.call(recCallback, resultObj); 
			}
			if (recCallbackAir != null) {
				recCallbackAir(resultObj);
			}
		}

		private function netStatusHandler(e:NetStatusEvent):void {
			var code:String = e.info.code;
			if(code == "NetStream.Buffer.Empty") {
				if (ExternalInterface.available) {
					ExternalInterface.call(ttsCallback); 	
				}
			} else if(code == "NetConnection.Connect.Success") {
				ns = new NetStream(nc);        //plus other stuff if you need.
				ns.addEventListener( NetStatusEvent.NET_STATUS, netStatusHandler );
			} else {
				trace(code);
			}
		}

		public  function speakResult(message:String):void {
			if (ns2 == null) {
				ns2 = new NetStream(nc);       
				ns2.addEventListener(AsyncErrorEvent.ASYNC_ERROR, asyncErrorHandler);
				ns2.addEventListener( NetStatusEvent.NET_STATUS, netStatusHandler );
				//var customClient:Object = new Object();
			}
			ns2.play("mp3:" + message);
		}

		public  function stopSpeaking():void {
			if (ns2 != null) {
				ns2.close();
			}
		}

		public  function vxmlComplete(app:String,status:String):void {
			if (ExternalInterface.available) {
				ExternalInterface.call(vxmlCallback,app,status); 	
			}
		}
	}
}
