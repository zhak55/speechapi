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
package com.metrocave;

import java.io.FileOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.red5.logging.Red5LoggerFactory;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.Red5; 
import org.red5.server.api.service.IServiceCapableConnection; 
import org.slf4j.Logger;
import org.speechapi.HttpSpeechClient;
import org.speechapi.SpeechSession;
import org.speechapi.SpeechSessionManager;
import org.springframework.context.ApplicationContext;


import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.renderer.SimpleTextRenderer;
import etm.core.timer.Java15NanoTimer;

import java.util.Timer;
import java.util.TimerTask;


public class Application extends MultiThreadedApplicationAdapter implements FlexComponent,
        IStreamAwareScopeHandler {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	private AudioTranscoder audioStreamer; // = new AudioTranscoder("metro_", this);

	private EtmMonitor profiler = EtmManager.getEtmMonitor();

	private Timer profilerRenderer = new Timer("profilerRenderer", true);

	private int mProfilerFrequency = 0;

	private int streamCounter = 0;

	private IScope appScope;

	private IServerStream serverStream;

	final private Map<String, IConnection> mConnections = new HashMap<String, IConnection>();

	private ApplicationContext context;

	public void setProfilerFrequency(int seconds) {
		mProfilerFrequency = 0;
	}

	public void init() {

		log.info("Speech Application has started. {}", this.getClass().getName());
		BasicEtmConfigurator.configure(true, new Java15NanoTimer());
		profiler.start();

		// very simple timer here that spits out profiling data every 5 seconds
		if (mProfilerFrequency > 0) {
			profilerRenderer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("Printing Statistics forz: " + this.getClass().getName());
					profiler.render(new SimpleTextRenderer());
				}
			}, mProfilerFrequency * 1000, mProfilerFrequency * 1000);
		}
	}

	@Override
	public void streamPublishStart(IBroadcastStream stream) {
		log.info("streamPublishStart: ", stream, stream.getPublishedName());
		log.debug("streamPublishStart: {}; {}", stream, stream.getPublishedName());
		super.streamPublishStart(stream);
		audioStreamer.startTranscodingStream(stream, Red5.getConnectionLocal().getScope());
	}

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		log.info("streamBroadcastClose: {}; {}", stream, stream.getPublishedName() + " "
		        + Red5.getConnectionLocal().getScope());
		audioStreamer.stopTranscodingStream(stream, Red5.getConnectionLocal().getScope());
		super.streamBroadcastClose(stream);
	}

	public boolean appStart(IScope app) {
		super.appStart(app);
		log.info("Red5Speech.appStart");
		appScope = app;
		context = app.getContext().getApplicationContext();
		return true;
	}

	public boolean appConnect(IConnection conn, Object[] params) {

		String id = conn.getClient().getId();
		mConnections.put(id, conn);
		log.info("appConnect " + id);
		return super.appConnect(conn, params);

	}

	// Red5.getConnectionLocal().getClient()

	public void appDisconnect(IConnection conn) {
		String id = conn.getClient().getId();
		log.info("appDisconnect " + id);
		if (appScope == conn.getScope() && serverStream != null) {
			serverStream.close();
		}
		super.appDisconnect(conn);
	}

	public void playAudio(String name, String filelocation) {
		log.debug("Playing audio " + name + " " + filelocation);
		IServiceCapableConnection sc = (IServiceCapableConnection) mConnections.get(name);
		if (sc != null) {
			sc.invoke("playAudio", new Object[] { filelocation });
		}
	}

	public void speakResults(String name, String filelocation) {
		log.debug("Speaking results " + name + " " + filelocation);
		IServiceCapableConnection sc = (IServiceCapableConnection) mConnections.get(name);
		if (sc != null) {
			sc.invoke("speakResult", new Object[] { filelocation });
		}
	}

	public void stopSpeaking(String name) {
		log.debug("Stop speaking results ");
		IServiceCapableConnection sc = (IServiceCapableConnection) mConnections.get(name);
		if (sc != null) {
			sc.invoke("stopSpeaking", new Object[] {});
		}
	}

	public void passRecogResults(String name, String result) {
		log.debug("Passing results " + name + " " + result);
		IServiceCapableConnection sc = (IServiceCapableConnection) mConnections.get(name);
		if (sc != null) {
			// IServiceCapableConnection sc = (IServiceCapableConnection) conn2;
			sc.invoke("passResults", new Object[] { result });
		}
	}

	public String getStreamName() {
		String id = Red5.getConnectionLocal().getClient().getId();
		log.info("getting a stream name " + id + " " + streamCounter);
		streamCounter++;
		return id;
	}

	public boolean initializeSettings(String[] credentials, String[] grammar, boolean auto, String name) {
		log.debug("Init settings2 " + credentials[0] + " " + name + "\n" + grammar[0]);
		String grammarText = grammar[0];
		String grammarType = grammar[1];

		// not used so why?
		// String userID = credentials[0];
		// String password=credentials[1];

		String JSGF = null;
		if (grammarType.toUpperCase().equals("SIMPLE")) {
			JSGF = "#JSGF V1.0;\ngrammar simplegram;\npublic <" + credentials[0] + "> = ";
			String[] temp = null;
			if ((grammarText != null) && (grammarText.length() > 0)) {
				temp = grammarText.split(",");
				for (int i = 0; i < temp.length; i++) { // d gets successively each value in ar.
					JSGF += temp[i];
					if (i < temp.length - 1) {
						JSGF += " | ";
					} else {
						JSGF += ";\n";
					}
				}
			} else {
				JSGF += "empty grammar;\n";
			}
		} else {
			JSGF = grammarText;
		}
		SpeechSessionManager sman = SpeechSessionManager.getInstance();
		SpeechSession sc = sman.getSession(name);
		if (sc == null) {
			sc = new SpeechSession(name);
			sman.newSession(sc);
		}
		sc.setCredentials(credentials);
		sc.setGrammar(JSGF);
		return false;
	}

	public boolean startRecognition(String name, boolean automatic) {
		log.debug("Start recognition " + name + " " + automatic);
		// get the session object
		SpeechSessionManager sman = SpeechSessionManager.getInstance();
		SpeechSession session = sman.getSession(name);

		// get the speech client from the session
		// TODO: Why is the speech client in the session (isn't it thread safe?)
		HttpSpeechClient speechClient = session.getRecognizer();

		// if in vxml mode, then just trigger start (the recognition requests are all done from within the
		// vxml app)
		// if javascript mode, then start the recognition request AND trigger start)
		if (session.isVxmlMode()) { // vxml mode
			// send start speech event to vxml processor
			log.debug("trigger start speech");
			speechClient.triggerStart();
		} else { // javascript mode
			// start a recognition request using JavaScript defined grammar
			if (speechClient != null) {
				if (!speechClient.isRecognizing()) {
					speechClient.setRecognizing(true);
					Transcoder transcoder = session.getTranscoder();
					if (transcoder != null) {

						// create a new set of pipes (output for transcoder to write new audio to, and input
						// for recognizer to read)
						// TODO: Use a buffer or FIFO queue instead of a pipe
						PipedOutputStream pos = null;
						PipedInputStream pis = new PipedInputStream();
						try {
							log.debug("  Create a new output pipe ");
							pos = new PipedOutputStream(pis);
						} catch (Exception e) {
							e.printStackTrace();
						}

						// tell the transcoder to send audio to in the output side of the pipe
						transcoder.setOutputStream(pos);

						// start recognizer (with the input stream)
						speechClient.recognize(automatic, pis);

						// trigger start
						speechClient.triggerStart();
					} else {
						log.warn("Could not start rec, transcoder  was null " + name);
					}
				} else {
					log.warn("Could not start rec, recognizer is busy... " + name);
				}
			} else {
				log.warn("Could not start rec, recognizer was null " + name);
			}
		}
		return true;
	}

	public boolean stopRecognition(String name) {
		log.debug("Stop recognition on stream: " + name);

		// get the session
		SpeechSessionManager sman = SpeechSessionManager.getInstance();
		SpeechSession sc = sman.getSession(name);

		// get the recognizer from the thread
		HttpSpeechClient speechClient = sc.getRecognizer();
		if (speechClient == null) {
			log.warn("Null recognizer for " + name);
		} else {

			// tell the transcoder to stop
			Transcoder tn = sc.getTranscoder();
			try {
				log.debug("Closing the transcoder");
				tn.closer();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// trigger stop
			speechClient.triggerStop();

			// Set the recognition state, so that another request can follow.
			speechClient.setRecognizing(false);
		}
		return true;
	}

	public boolean startVxmlAppUrl(String name, String appUrl) {
		log.debug("Start vxml app (url) " + name + " " + appUrl+ "not implemented");

		return true;
	}

	public boolean startVxmlAppText(String name, String app) {
		log.debug("Start vxml app (text) " + name + " " + app+" not implemented");
		
		return true;
	}

	public String speak(String name, String text, String gender) {
		log.debug("Speak " + name + " " + text + " " + gender);
		// TODO: use real embedded property (need to get access to it)
		boolean mEmbedded = false;
		String filePath = null;
		if ((text == null) || (text.length() == 0)) {
			log.warn("No text to synthesize");
		} else {
			SpeechSessionManager sman = SpeechSessionManager.getInstance();
			SpeechSession sc = sman.getSession(name);
			// RemoteRecognizer sphinx = mSphinxThreads.get(name);
			HttpSpeechClient sphinx = sc.getRecognizer();

			String filename = null;
			if (sphinx != null) {
				filename = sphinx.speak(text, gender);
			}
			filePath = filename;
		}
		return filePath;

	}

	// setter/getter used for dependency injection

	/**
	 * @return the audioStreamer
	 */
	public AudioTranscoder getAudioStreamer() {
		return audioStreamer;
	}

	/**
	 * @param audioStreamer
	 *            the audioStreamer to set
	 */
	public void setAudioStreamer(AudioTranscoder audioStreamer) {
		this.audioStreamer = audioStreamer;
	}

}
