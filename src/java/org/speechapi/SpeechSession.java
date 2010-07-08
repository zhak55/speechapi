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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;

import org.slf4j.Logger;

import com.metrocave.Application;
import com.metrocave.Transcoder;
import com.xuggle.red5.io.BroadcastStream;

public class SpeechSession {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	private String id;

	private BroadcastStream outputStream;

	private Transcoder transcoder;

	private HttpSpeechClient recognizer;

	//private PipedInputStream pipedInputStream;

	//private PipedOutputStream pipedOutputStream;

	private IConnection connection;

	private IScope ascope;

	private String developerId;

	private String developerkey;

	private String grammar;

	private Application app;

	private String streamsFolder;

	private String downloadService;

	private boolean vxmlMode;

	private VoiceXmlSessionProcessor vxmlSessionProcessor;

	public SpeechSession(String id) {
		super();
		this.id = id;
	}

	public SpeechSession(String id, BroadcastStream outputStream, Transcoder transcoder,
	        HttpSpeechClient recognizer, PipedInputStream pipedInputStream,
	        PipedOutputStream pipedOutputStram, IConnection connection) {
		super();
		this.id = id;
		this.outputStream = outputStream;
		this.transcoder = transcoder;
		this.recognizer = recognizer;
		//this.pipedInputStream = pipedInputStream;
		//this.pipedOutputStream = pipedOutputStram;
		this.connection = connection;
	}

	/**
	 * @return the ascope
	 */
	public IScope getAscope() {
		return ascope;
	}

	/**
	 * @param ascope
	 *            the ascope to set
	 */
	public void setAscope(IScope ascope) {
		this.ascope = ascope;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the connections
	 */
	public IConnection getConnection() {
		return connection;
	}

	/**
	 * @param connections
	 *            the connections to set
	 */
	public void setConnection(IConnection connection) {
		this.connection = connection;
	}

	/**
	 * @return the outputStream
	 */
	public BroadcastStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @param outputStream
	 *            the outputStream to set
	 */
	public void setOutputStream(BroadcastStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * @return the transcoder
	 */
	public Transcoder getTranscoder() {
		return transcoder;
	}

	/**
	 * @param transcoder
	 *            the transcoder to set
	 */
	public void setTranscoder(Transcoder transcoder) {
		this.transcoder = transcoder;
	}

	/**
	 * @return the recognizer
	 */
	public HttpSpeechClient getRecognizer() {
		return recognizer;
	}

	/**
	 * @param recognizer
	 *            the recognizer to set
	 */
	public void setRecognizer(HttpSpeechClient recognizer) {
		this.recognizer = recognizer;
	}



	public void close() {

		if (transcoder != null) {
			transcoder.closePipe();
			transcoder.stop();
		} else {
			log.warn("transcoder was not in session " + id);
		}

		if (outputStream != null) {
			outputStream.stop();
		} else {
			log.warn("outputstream was not in session " + id);
		}

		if (recognizer != null) {
			// poor solution, need to add it to the client to support sudden disconnections
			try {
				recognizer.cancelRecognition();
				;
				recognizer.cancelRecognition();
				recognizer = null;
			} catch (ThreadDeath td) {
				td.printStackTrace();
			}
		} else {
			log.warn("sphinx was not in session " + id);
		}


		// stop vxml session (if there is one)
		if (vxmlSessionProcessor != null) {
			vxmlSessionProcessor.stop();
			vxmlSessionProcessor = null;
		}

	}

	public void setCredentials(String[] info) {
		developerId = info[0];
		developerkey = info[1];

	}

	public void setGrammar(String jsgf) {
		grammar = jsgf;

	}

	public String getGrammar() {

		return grammar;
	}

	public void setApplication(Application app) {
		this.app = app;

	}

	/**
	 * @return the app
	 */
	public Application getApp() {
		return app;
	}

	public String getStreamsFolder() {
		return this.streamsFolder;
	}

	public void setStreamsFolder(String streamsFolder) {
		this.streamsFolder = streamsFolder;
	}

	public String getDownloadService() {
		return this.downloadService;
	}

	public void setDownloadService(String downloadService) {
		this.downloadService = downloadService;
	}

	public void setVxmlMode(boolean b) {
		this.vxmlMode = b;
	}

	public boolean isVxmlMode() {
		return this.vxmlMode;
	}

	public void setVxmlSessionProcessor(VoiceXmlSessionProcessor vxml) {
		this.vxmlSessionProcessor = vxml;

	}

	/**
	 * @return the vxmlSessionProcessor
	 */
	public VoiceXmlSessionProcessor getVxmlSessionProcessor() {
		return vxmlSessionProcessor;
	}

}
