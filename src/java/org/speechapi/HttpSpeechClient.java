/*
 * Copyright (C) 2010 speechapi.com
 * 
 *   This file is part of speechapi flashspeak
 * 
 *   Speechapi flashspeak is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Speechapi flashspeak is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Speechapi flashspeak.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package org.speechapi;

import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.io.IOException;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFileFormat.Type;
import java.io.File;

import com.google.gson.Gson;
import com.metrocave.FlexComponent;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.client.HttpRecognizer;
import com.spokentech.speechdown.client.SpeechEventListenerDecorator;

import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.ExternalTriggerEndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.common.Utterance.OutputFormat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import com.spokentech.speechdown.client.HttpSynthesizer;
import org.tritonus.share.sampled.Encodings;

public class HttpSpeechClient {

	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	private static AudioFormat desiredFormat;
	private static int sampleRate = 8000;
	private static boolean signed = true;
	private static boolean bigEndian = true;
	private static int channels = 1;
	private static int sampleSizeInBits = 16;
	private static int audioBufferSize = 160000;
	private static int msecPerRead = 10;
	private static int frameSizeInBytes;

	private HttpRecognizer recog;
	private HttpSynthesizer synth;


	String wav = "audio/x-wav";
	String s4feature = "audio/x-s4feature";
	String s4audio = "audio/x-s4audio";

	String id;
	
	private FlexComponent flex;
	private String mStreamName;

	private boolean recognizing;
	private SpeechEventListener _speechEventListener = null;
	private ExternalTriggerEndPointer ep;

	private String userId = null;
	private String devId = null;
	private String key = null;
	private SpeechSession sc;
	private Gson gson;

	public HttpSpeechClient(FlexComponent flex, String streamName, HttpRecognizer recog, HttpSynthesizer synth, SpeechSession sc) {
		this.sc = sc;
		this.recog = recog;
		this.synth=synth;
		this.flex = flex;
		this.mStreamName = streamName;
		gson = new Gson();

	}

	/**
	 * @return the recognizing
	 */
	public boolean isRecognizing() {
		return recognizing;
	}

	/**
	 * @param recognizing
	 *            the recognizing to set
	 */
	public void setRecognizing(boolean recognizing) {
		this.recognizing = recognizing;
	}

	public void triggerStop() {
		if (ep != null)
			ep.triggerEnd();
	}

	public void triggerStart() {
		if (ep != null)
			ep.triggerStart();
	}

	public void cancelRecognition() {
		if (recog != null)
			recog.cancel(id);
	}

	protected void setUp() {

		// why is this not set -- and passed in on constructor....
		// recog = new HttpRecognizer();
		// recog.setService(service);



	}

	public String speak(String text, String gender) {

		AudioFormat format2;
		format2 = new AudioFormat(Encodings.getEncoding("MPEG1L3"), 11025,16, 1, 2, 11025, true);

		AudioFormat format;
		format = new AudioFormat(44100, 32, channels, signed, bigEndian);

		String voice;
		String outFileName;
		InputStream stream;

		if (gender.toUpperCase().compareTo("MALE") == 0) {
			voice = "bdl-arctic";
		} else {
			voice = "hmm-slt";

		}
		wav = "audio/mpeg";
		text = text;

		stream = synth.synthesize(userId, text, format2, wav, voice);

		String rand = Math.random() + ".mp3";
		outFileName = sc.getStreamsFolder() + "/" + rand;
		if (stream != null) {
			writeStreamToFile(stream, outFileName);
		}
		return rand;
	}

	public void writeStreamToFile(InputStream inStream, String fileName) {
		try {

			File f = new File(fileName);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

			BufferedInputStream in = new BufferedInputStream(inStream);

			byte[] buffer = new byte[256];
			while (true) {
				int bytesRead = in.read(buffer);
				// _logger.trace("Read "+ bytesRead + "bytes.");
				if (bytesRead == -1)
					break;
				out.write(buffer, 0, bytesRead);
			}
			in.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void recognize(boolean automatic, InputStream pi) {
		boolean lmflg = false;
		Type type = null;
		long timeout = 10000;

		SpeechEventListener eventListener = null;
		if (sc.isVxmlMode()) {
			eventListener = sc.getVxmlSessionProcessor().getListener();
		}
		log.debug("vxml mode: " + sc.isVxmlMode() + " " + eventListener);
		MySpeechEventListener listener = new MySpeechEventListener(eventListener);

		if (automatic) {
			log.debug("Starting recognition in auto mode");

			StreamEndPointingInputStream epStream = null;
			S4EndPointer ep = new S4EndPointer();
			epStream = new StreamEndPointingInputStream(ep);
			epStream.setMimeType(s4audio);
			try {
				pi.reset();
			} catch (IOException e) {
			}
			AudioFormat format = getAudioFormat();
			AFormat f = FormatUtils.covertToNeutral(format);
			epStream.setupStream(pi, f);
			try {
				id = recog.recognizeAsynch(devId, key, userId, sc.getGrammar(), epStream, false, true,
				        OutputFormat.json, timeout, listener);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (ThreadDeath e) {
				e.printStackTrace();
				return;
			}
		} else {
			log.debug("Starting recognition in press to speak  mode");

			StreamEndPointingInputStream epStream = null;
			ep = new ExternalTriggerEndPointer();
			epStream = new StreamEndPointingInputStream(ep);
			epStream.setMimeType(wav);
			try {
				pi.reset();
			} catch (IOException e) {
			}
			AudioFormat format = getAudioFormat();
			AFormat f = FormatUtils.covertToNeutral(format);
			// AFormat af = new AFormat("PCM",8000,16,1,false,true,8,2,2);
			epStream.setupStream(pi, f);


			try {
				id = recog.recognizeAsynch(devId, key, userId, sc.getGrammar(), epStream, false, true,
				        OutputFormat.json, timeout, listener);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (ThreadDeath e) {
				e.printStackTrace();
				return;
			}

		}

	}

	private static AudioFormat getAudioFormat() {
		float sampleRate = 8000.0F;
		// 8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		// 8,16
		int channels = 1;
		// 1,2
		boolean signed = true;
		// true,false
		boolean bigEndian = false;
		// true,false
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	public void setDevCredentials(String[] info) {
		devId = info[0];
		key = info[1];
	}

	public HttpRecognizer getHttpRecognizer() {
		return recog;
	}

	public HttpSynthesizer getHttpSynthesizer() {
		return synth;
	}

	private class MySpeechEventListener extends SpeechEventListenerDecorator {

		/**
		 * TODOC.
		 * 
		 * @param speechEventListener
		 *            the speech event listener
		 */
		public MySpeechEventListener(SpeechEventListener speechEventListener) {
			super(speechEventListener);
		}

		public void recognitionComplete(Utterance r) {
			log.debug("Recognition complete: " + r.getText());
			String u = gson.toJson(r, Utterance.class);
			flex.passRecogResults(sc.getId(), u);
			super.recognitionComplete(r);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.spokentech.speechdown.client.SpeechEventListener#speechEnded()
		 */
		public void speechEnded() {
			log.debug("Speech Ended");
			super.speechEnded();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.spokentech.speechdown.client.SpeechEventListener#speechStarted()
		 */
		public void speechStarted() {
			log.debug("Speech Started");
			super.speechStarted();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.spokentech.speechdown.client.SpeechEventListener#noInputTimeout()
		 */
		@Override
		public void noInputTimeout() {
			log.debug("No Inmput Timeout");
			super.noInputTimeout();
		}
	}

}
