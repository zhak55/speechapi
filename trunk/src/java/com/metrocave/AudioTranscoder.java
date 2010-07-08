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

import org.slf4j.Logger;
import org.speechapi.HttpSpeechClient;
import org.speechapi.SpeechSession;
import org.speechapi.SpeechSessionManager;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.stream.BroadcastScope;
import org.red5.server.stream.IBroadcastScope;
import org.red5.server.stream.IProviderService;

import com.xuggle.red5.io.BroadcastStream;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.SimpleMediaFile;

import com.metrocave.Transcoder;

import com.spokentech.speechdown.client.HttpRecognizer;
import com.spokentech.speechdown.client.HttpSynthesizer;

/**
 * This demo application takes a stream broadcasted from a Flash player and decodes, re-samples, and
 * re-encodes the audio on that stream to produce 5.5KHZ stero PCM audio. It drops all the video.
 * <p>
 * To use install the application to a Red5 server and connect to the "audiotranscoder" application.
 * </p>
 * <p>
 * <b> YOU MUST MAKE SURE YOU HAVE INSTALLED XUGGLER ON THE MACHINE RUNNING YOUR RED5 SERVER.
 * 
 * See http://www.xuggle.com/xuggler </b>
 * </p>
 * <p>
 * 
 * Then publish (you can use the "demos/publisher/publisher.html" application) an audio stream with a unique
 * name (e.g. "my_stream").
 * </p>
 * <p>
 * To hear your transcoded audio stream, connect to the same application and then playback a stream that has
 * the same unique name, but with "xuggle_" appended to it (e.g. "xuggle_my_stream").
 * </p>
 * <p>
 * You should hear the audio you are broadcasting, just re-encoded at the new parameters. Please note that
 * while you will hear a latency in this audio, approximately 3-msec is added by our transcoder -- the rest is
 * coming from your network, from red5, and from the fact that (probably) you've set your buffer time on your
 * flash NetStream object to 2 seconds. To see exactly how fast transcoding is, check out the performance
 * metrics that this application outputs every few seconds.
 * </p>
 */
public class AudioTranscoder {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	private HttpRecognizer recog;
	private HttpSynthesizer synth;

	// configuration parameters (injected)
	private String streamPrefix;

	private FlexComponent flexComponent;

	private String speechToTextUrl = "http://www.speechapi.com:8080/speechcloud/SpeechUploadServlet";

	private String textToSpeechUrl = "http://www.speechapi.com:8080/speechcloud/SpeechDownloadServlet";

	private String streamsFolder = null;

	private boolean mEmbedded = true;

	private int poolSize;

	/**
	 * Create a new resampler demo object.
	 * 
	 * @param streamPrefix
	 *            The prefix to attach to the name of the stream copies we make.
	 */
	public AudioTranscoder(boolean embeddedSpeechEngines, int poolSize, String textToSpeechUrl,
	        String speechToTextUrl, String streamFolder, String streamPrefix, FlexComponent flexComponent) {

		this.mEmbedded = embeddedSpeechEngines;
		this.poolSize = poolSize;
		this.textToSpeechUrl = textToSpeechUrl;
		this.speechToTextUrl = speechToTextUrl;
		this.streamsFolder = streamFolder;
		this.streamPrefix = streamPrefix;
		this.flexComponent = flexComponent;

	}

	protected void setUp() {
		log.info("Creating the http recognizer, poolsize: " + poolSize);
		recog = new HttpRecognizer("test", "test");
		recog.setService(speechToTextUrl);
		recog.enableAsynchMode(poolSize);
		synth = new HttpSynthesizer("test", "test");
		synth.setService(textToSpeechUrl);

	}

	/**
	 * Starts transcoding this stream. This method is a no-op if this stream is already a stream copy created
	 * by this transcoder.
	 * 
	 * @param aStream
	 *            The stream to copy.
	 * @param aScope
	 *            The application scope.
	 */
	synchronized public void startTranscodingStream(IBroadcastStream aStream, IScope aScope) {

		SpeechSessionManager sman = SpeechSessionManager.getInstance();

		SpeechSession sContext = sman.getSession(aStream.getPublishedName());

		if (sContext == null) {
			sContext = new SpeechSession(aStream.getPublishedName());
			sman.newSession(sContext);
		}

		sContext.setDownloadService(textToSpeechUrl);
		sContext.setStreamsFolder(streamsFolder);

		if (aStream.getPublishedName().startsWith(streamPrefix)) {
			log.info("Not making a copy of a copy: {}", aStream.getPublishedName());
			return;
		}

		log.debug("aStream.getPublishedName()=" + aStream.getPublishedName());
		log.debug("getStreamPrefix()=" + streamPrefix);
		log.debug("startTranscodingStream" + aStream.getPublishedName() + "-" + streamPrefix);
		log.debug("Making transcoded version of: {}", aStream.getPublishedName());

		/*
		 * First, we create the meta data information about our stream.
		 * 
		 * Turns out aaffmpeg-red5 provides an object to do that with.
		 */
		ISimpleMediaFile inputStreamInfo = new SimpleMediaFile();

		// now we're going to default to no video, and lots of audio,
		// but we're going to ask AAFFMPEG to figure out as much
		// data as possible. This means AAFFMPEG will have to look at at
		// least ONE audio packet before finishing the IContainer open call.
		inputStreamInfo.setHasVideo(false);
		inputStreamInfo.setHasAudio(true);

		/*
		 * Now, we need to set up the output stream we want to broadcast to. Turns out aaffmpeg-red5 provides
		 * one of those.
		 */
		String outputName = streamPrefix + aStream.getPublishedName();
		BroadcastStream outputStream = new BroadcastStream(outputName);
		outputStream.setPublishedName(outputName);
		outputStream.setScope(aScope);

		IContext context = aScope.getContext();

		IProviderService providerService = (IProviderService) context.getBean(IProviderService.BEAN_NAME);
		if (providerService.registerBroadcastStream(aScope, outputName, outputStream)) {
			IBroadcastScope bsScope = (BroadcastScope) providerService.getLiveProviderInput(aScope,
			        outputName, true);
			bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, outputStream);
		} else {
			log.error("Got a fatal error; could not register broadcast stream");
			throw new RuntimeException("fooey!");
		}
		sContext.setOutputStream(outputStream);
		sContext.setAscope(aScope);
		outputStream.start();

		/**
		 * Now let's give aaffmpeg-red5 some information about what we want to transcode as.
		 */
		ISimpleMediaFile outputStreamInfo = new SimpleMediaFile();
		outputStreamInfo.setAudioSampleRate(8000);
		outputStreamInfo.setAudioChannels(1);
		outputStreamInfo.setAudioBitRate(16000);
		outputStreamInfo.setAudioCodec(ICodec.ID.CODEC_ID_PCM_S16LE);
		outputStreamInfo.setHasVideo(false);

		/**
		 * And finally, let's create out transcoder
		 */

		log.debug("Creating a new remote recognizer " + recog);
		HttpSpeechClient recognizer = null;
		recognizer = new HttpSpeechClient(flexComponent, aStream.getPublishedName(), recog, synth, sContext);
		recognizer.setRecognizing(false);

		sContext.setRecognizer(recognizer);

		Transcoder transcoder = new Transcoder(aStream, outputStream, outputStreamInfo);

		Thread transcoderThread = new Thread(transcoder);
		transcoderThread.setDaemon(true);
		sContext.setTranscoder(transcoder);

		log.debug("Starting transcoding thread for: {}", aStream.getPublishedName());
		transcoderThread.start();
	}

	/**
	 * Stop transcoding a stream.
	 * 
	 * @param aStream
	 *            The stream to stop transcoding.
	 * @param aScope
	 *            The application scope.
	 */
	synchronized public void stopTranscodingStream(IBroadcastStream aStream, IScope aScope) {
		String name = aStream.getPublishedName();
		SpeechSessionManager sman = SpeechSessionManager.getInstance();
		SpeechSession sc = sman.getSession(name);
		sc.close();
		sman.removeSession(sc);

	}

}
