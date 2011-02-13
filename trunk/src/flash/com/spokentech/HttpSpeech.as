/*
*
* Copyright (C) 2010  Spokentech http://www.spokentech.coom
* 
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* 
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*
* Contact: spencer@spokentech.com
*
*/

package com.spokentech {

	import com.adobe.audio.format.WAVWriter;

	import flash.display.SimpleButton;
	import flash.events.SampleDataEvent;
	import flash.media.Microphone;
	import flash.media.Sound;
	import flash.utils.ByteArray;
	import flash.events.Event;
        import flash.events.ActivityEvent;
        import flash.events.DataEvent;
        import flash.events.StatusEvent;
	import flash.net.*;
	import flash.media.SoundChannel;
	import org.osflash.thunderbolt.Logger;

        import flash.events.ProgressEvent;
        import flash.events.IOErrorEvent;

	import flash.system.Security;
	import flash.system.SecurityPanel;
	import mx.controls.Alert;

        import ru.inspirit.net.MultipartURLLoader;
 
	public class HttpSpeech  {


		//[Bindable] 
		private var microphoneList:Array;
	  	protected var microphone:Microphone;
	        protected var isRecording:Boolean = false;
	        protected var position:int;
	 	protected var soundRecording:ByteArray;
	 	protected var soundOutput:Sound;

	        private var username:String;
		private var password:String;	

	        private var recCallback:Function;
		private var ttsCallback:Function;
		private var micActivityCallback:Function;
	
	        private var gMode:String = "SIMPLE";
		private var grammar:String;	
		private var gData:ByteArray;	
	        private var autoRecMode:Boolean = false;
		private var oog:Boolean = false;
		private var oogBranchProb:String = "1e-25";
		private var phoneProb:String = "1e-20";
	        private var bufferCount:int= 0;

		private var recUrl:String = "http://spokentech.net:8000/speechcloud/SpeechUploadServlet";
		private var ttsUrl:String = "http://spokentech.net:8000/speechcloud/SpeechDownloadServlet";

		public function HttpSpeech():void {
			setupMicrophoneList();
			setupMicrophone(0);
		}	

		public function configure(username:String,password:String,recCallback:Function,ttsCallback:Function, serviceNamePort:String,micActivityCallback:Function):void {
	 		//Logger.info("configure method",serviceNamePort); 

			this.username = username;
			this.password = password;
			this.recCallback = recCallback;
			this.ttsCallback = ttsCallback;
			this.micActivityCallback = micActivityCallback;

			setupMicrophoneList();
			setupMicrophone(0);

			if (serviceNamePort != null) {
				recUrl = serviceNamePort+"/SpeechUploadServlet";
				ttsUrl = serviceNamePort+"/SpeechDownloadServlet";
                        }
		}	

		public function setGmode(gMode:String):void {
			this.gMode = gMode;
		}


		public function setAutoRecMode(autoRecMode:Boolean):void {
			this.autoRecMode = autoRecMode;
                        if (autoRecMode) {
				//why do i need this in order to get the activity events?  maybe the start data event is needed?
                                startMicRecording();
                        }
		}
			
		public function setOog(oog:Boolean):void {
			this.oog = oog;
		}

	        public function setOogParams(oogBranchProb:String, phoneProb:String):void {
			this.oogBranchProb =oogBranchProb  ;
			this.phoneProb =phoneProb  ;
		}


		public function setGrammar(grammar:String):void {
			this.grammar = grammar;
                        this.gData = new ByteArray();
                        this.gData.writeUTFBytes(this.grammar);
		}

			
		protected function setupMicrophoneList():void {
			microphoneList = Microphone.names;
			//trace(microphoneList);
		}
			
		protected function setupMicrophone(selectedMic:int):void {
			//microphone = Microphone.getMicrophone(comboMicList.selectedIndex);
			microphone = Microphone.getMicrophone(selectedMic);
            		Security.showSettings(SecurityPanel.PRIVACY);

			microphone.rate = 16;
			
			//addEventListener(Event.ENTER_FRAME, drawLines)
			//micTimer.addEventListener(TimerEvent.TIMER, ShowMicActivity);

			//var deviceArray:Array = Microphone.names; 
			//trace("Available sound input devices:"); 
			//for (var i:int = 0; i < deviceArray.length; i++)  {
    				//trace(" " + deviceArray[i]); 
			//} 
				
			microphone.gain = 60; 
			microphone.setUseEchoSuppression(true); 
			microphone.setLoopBack(false); 
			microphone.setSilenceLevel(5, 1000); 
     
			microphone.addEventListener(ActivityEvent.ACTIVITY, onMicActivity); 
			microphone.addEventListener(StatusEvent.STATUS, onMicStatus); 
     
			var micDetails:String = "Sound input device name: " + microphone.name + '\n'; 
			micDetails += "Gain: " + microphone.gain + '\n'; 
			micDetails += "Rate: " + microphone.rate + " kHz" + '\n'; 
			micDetails += "Muted: " + microphone.muted + '\n'; 
			micDetails += "Silence level: " + microphone.silenceLevel + '\n'; 
			micDetails += "Silence timeout: " + microphone.silenceTimeout + '\n'; 
			micDetails += "Echo suppression: " + microphone.useEchoSuppression + '\n'; 
			//Alert.show("httpspeech.setupMic()",micDetails); 
			//Logger.info("httpspeech.setupMic()",micDetails); 
		}
 
 
		private function onMicActivity(event:ActivityEvent):void  {
    			 //Logger.info("HttpSpeech.as:","activating=" + event.activating + ", activityLevel=" +  microphone.activityLevel); 

                         if (autoRecMode) {
			        if (event.activating) {
                                   startMicRecording();
                                } else {
                                   stopMicRecording();
			           //TODO: why do i need this in order to get the activity events?  maybe the start data event is needed?
                                   //startMicRecording();
                                }
			    }
			    micActivityCallback(event,microphone);
			} 

 
		private function onMicStatus(event:StatusEvent):void  {
			//Logger.info("HttpSpeech.as", "status: level=" + event.level + ", code=" + event.code); 
		}



                private function activityHandler(event:ActivityEvent):void {
                	trace("activityHandler: " + event);
                }

                private function statusHandler(event:StatusEvent):void {
                	trace("statusHandler: " + event);
                }

			
		public function startMicRecording():void {
    		    //Logger.info("HttpSpeech.as", "start recording");
		        position = 0;
			isRecording = true;
			bufferCount=0;
			soundRecording = new ByteArray();
			microphone.addEventListener(SampleDataEvent.SAMPLE_DATA, gotMicData);
		}
			
		public function stopMicRecording():void {
    	    		//Logger.info("HttpSpeech.as", "stop recording");
			isRecording = false;
			microphone.removeEventListener(SampleDataEvent.SAMPLE_DATA, gotMicData);
			recognize();
		}

		private function gotMicData(micData:SampleDataEvent):void {

			soundRecording.writeBytes(micData.data);
			//Logger.info( "httpspeech.as, gotMicData.soundrecording,  " + soundRecording.length + " bytes");    
		}
			
			
		public function recognize():void  {

			//Alert.show(recUrl);
	 		//Logger.info("recognize method ",recUrl); 
			soundRecording.position=0;
                	var ml:MultipartURLLoader = new MultipartURLLoader();
			ml.addEventListener(Event.COMPLETE, onReady);
			ml.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, recComplete);
			ml.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, recComplete);
 
			function recComplete(e:DataEvent):void {
				//Logger.info("recognize", "rec result: "+e.data);
				recCallback(e.data);
                        }

			function onReady(e:Event):void {
				//Logger.info("recognize", "complete: "+e);
			}

			//ml.addVariable('lmFlag', 'true');
			ml.addVariable('outputMode', 'json');
			ml.addVariable('gMode', gMode);
			ml.addVariable('continuousFlag', 'false');
			ml.addVariable('doEndpointing', 'false');
			ml.addVariable('CmnBatchFlag', 'true');
			ml.addVariable('encoding', 'PCM_SIGNED');
			ml.addVariable('sampleRate', '16000');
			ml.addVariable('bigEndian', 'false');
			ml.addVariable('bytesPerValue', '2');
			ml.addVariable('detectOOG', oog);
			ml.addVariable('oogBranchProb',oogBranchProb );
			ml.addVariable('phoneInsertionProb',phoneProb );
 

			//convert to wave file
			var data:ByteArray = new ByteArray();
			var wavWriter:WAVWriter = new WAVWriter();
			soundRecording.position = 0;  // rewind to the beginning of the sample
			wavWriter.numOfChannels = 1; // set the inital properties of the Wave Writer
			wavWriter.sampleBitRate = 16;
			wavWriter.samplingRate = 16000;
			wavWriter.processSamples(data, soundRecording, 16000, 1, true); 

			//Logger.info( "httpspeech.recognize" , data.length+ " bytes" );    
			if (gMode != "lm") {
				ml.addFile(gData, 'grammar', 'grammar', 'plain/text');
			}
			ml.addFile(data, 'audio', 'audio', 'audio/x-wav');
			ml.load(recUrl)

			//Alert.show(grammar);
		}
 


		public function playAudio(text:String,speaker:String):void {
			//Alert.show(ttsUrl);
			//Alert.show(text);
			//Alert.show(speaker);
	 		//Logger.info("playAudio method ",ttsUrl); 
			var myRequest:URLRequest = new URLRequest(ttsUrl);
			var myVariables:URLVariables = new URLVariables();
                        //var myLoader:URLLoader = new URLLoader();
                        //var streamer:URLStream = new URLStream(  );
                        //var req:URLRequest = new URLRequest("04-AudioTrack 04.mp3");

			//soundRecording = new ByteArray();

                        myVariables.text= text;
			if (speaker == "male") {
                            myVariables.voice = "hmm-jmk";
			}else if (speaker =="female") {
                            myVariables.voice = "hmm-bdl";
                        } else {
                            myVariables.voice = speaker;
                        }
                        myVariables.mimeType= "audio/mpeg";
                        myVariables.encoding= "MPEG1L3";
                        myVariables.sampleRate= "44100";
 	                myVariables.bigEndian='true';
                        myVariables.bytesPerValue= "4";
                        //myVariables.mimeType= "audio/x-wav";
                        //myVariables.encoding= "PCM_SIGNED";
                        //myVariables.sampleRate= "16000";
 	                //myVariables.bigEndian='true';
                        //myVariables.bytesPerValue= "2";

                        myRequest.method = URLRequestMethod.GET;
			myRequest.data = myVariables;
                            
			//var header:URLRequestHeader = new URLRequestHeader("content-type", "multipart/form-data");
			//myRequest.requestHeaders.push(header);


			soundOutput = new Sound();
                        soundOutput.addEventListener(IOErrorEvent.IO_ERROR, errorHandler);
                        soundOutput.addEventListener(ProgressEvent.PROGRESS, progressHandler);
			soundOutput.addEventListener(Event.COMPLETE, onLoadComplete);

			try {
			       soundOutput.load(myRequest);
			       //soundOutput.load(req);
			       soundOutput.play();
                            
			} catch (err:Error) {
				Alert.show(err.message);
                               trace(err.message);
			}


			//this.addChild(statusTextField);
				 
		}

		function onLoadComplete(event:Event):void {
				//Alert.show(event.toString());
		    //var localSound:Sound = event.target as Sound;
		    //localSound.play();
		}

                private function progressHandler(event:ProgressEvent):void {
			var loadTime:Number = event.bytesLoaded / event.bytesTotal;
			var LoadPercent:uint = Math.round(100 * loadTime);

			//Alert.show(event.toString());

			trace("Sound file's size in bytes: " + event.bytesTotal + "\n" 
				+ "Bytes being loaded: " + event.bytesLoaded + "\n" 
				+ "Percentage of sound file that is loaded " + LoadPercent + "%.\n");
                }
 
                private function errorHandler(errorEvent:IOErrorEvent):void {
                            trace("The sound could not be loaded: " + errorEvent.text);
			//Alert.show(errorEvent.toString());
                }
         }
}

