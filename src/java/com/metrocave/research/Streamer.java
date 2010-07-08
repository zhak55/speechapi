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
package com.metrocave.research;


import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;

import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;

import org.red5.server.net.rtmp.event.VideoData;
import org.slf4j.Logger;

import com.xuggle.red5.io.Red5Message;
import com.xuggle.red5.io.Red5StreamingQueue;
import com.xuggle.red5.io.Red5Message.Type;

import com.xuggle.xuggler.IPacket;


import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

/**
 * Experimental class to send red5 audio stream directly to the recognizer.
 * <p>
 */
public class Streamer implements Runnable {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	// private final Log log = LogFactory.getLog( this.getClass() );
	private final EtmMonitor profiler = EtmManager.getEtmMonitor();


	private final IBroadcastStream mInputStream;

	private final Red5StreamingQueue mInputQueue;

	private final IStreamListener mInputListener;


	private volatile boolean mIsRunning = false;

	private volatile boolean mKeepRunning = true;


	private PipedInputStream pis;
	private PipedOutputStream pos = null;


	/**
	 * Create a new streamer object.
	 * 
	 * All listeners are set to null.
	 * 
	 * @param aInputStream
	 *            The stream to get input packets from.
	 */
	public Streamer(IBroadcastStream aInputStream, PipedInputStream aPis, PipedOutputStream aPos) {

		if (aInputStream == null)
			throw new IllegalArgumentException("must pass input stream");
		pis = aPis;
		pos = aPos;
		mInputStream = aInputStream;

		mInputQueue = new Red5StreamingQueue();



		// Make FFMPEG return back if a read takes longer than 2 seconds. Unfortunately
		// it means FFMPEG will think the stream has ended, but them's the breaks.
		// mInputQueue.setReadTimeout(new TimeValue(2, TimeUnit.SECONDS));
		mInputListener = new IStreamListener() {
			public void packetReceived(IBroadcastStream aStream, IStreamPacket aPacket) {
				EtmPoint point = profiler.createPoint(this.getClass().getName() + "#packetReceived");
				try {

					IoBuffer buf = aPacket.getData();
					// ByteBuffer buf = aPacket.getData();
					if (buf != null)
						buf.rewind();
					if (buf == null || buf.remaining() == 0) {
						log.debug("skipping empty packet with no data");
						return;
					}

					if (aPacket instanceof AudioData) {
						log.debug("  adding packet type: {}; ts: {}; on stream: {}", new Object[] { "AUDIO",
						        aPacket.getTimestamp(), aStream.getPublishedName() });
						mInputQueue.put(new Red5Message(Red5Message.Type.AUDIO, (AudioData) aPacket));
					} else if (aPacket instanceof VideoData) {
						Red5Message.Type type = Red5Message.Type.INTERFRAME;
						VideoData dataPacket = (VideoData) aPacket;
						switch (dataPacket.getFrameType()) {
						case DISPOSABLE_INTERFRAME:
							type = Red5Message.Type.DISPOSABLE_INTERFRAME;
							break;
						case INTERFRAME:
							type = Red5Message.Type.INTERFRAME;
							break;
						case KEYFRAME:
						case UNKNOWN:
							type = Red5Message.Type.KEY_FRAME;
							break;
						}
						if (type != Red5Message.Type.DISPOSABLE_INTERFRAME) // The FFMPEG FLV decoder doesn't
																			// handle disposable frames
						{
							log.debug("  adding packet type: {}; ts: {}; on stream: {}", new Object[] {
							        dataPacket.getFrameType(), aPacket.getTimestamp(),
							        aStream.getPublishedName() });
							mInputQueue.put(new Red5Message(type, dataPacket));
						}
					} else if (aPacket instanceof IRTMPEvent) {
						log.debug("  adding packet type: {}; ts: {}; on stream: {}", new Object[] { "OTHER",
						        aPacket.getTimestamp(), aStream.getPublishedName() });
						Red5Message.Type type = Red5Message.Type.OTHER;
						IRTMPEvent dataPacket = (IRTMPEvent) aPacket;
						mInputQueue.put(new Red5Message(type, dataPacket));
					} else {
						log.debug("dropping packet type: {}; ts: {}; on stream: {}", new Object[] {
						        "UNKNOWN", aPacket.getTimestamp(), aStream.getPublishedName() });
					}
				} catch (InterruptedException ex) {
					log.error("exception: {}", ex);
				} finally {
					point.collect();
				}
			}

		};

	}

	/**
	 * Is the main loop running?
	 * 
	 * @see #run()
	 * @return true if the loop is running, false otherwise.
	 */
	public boolean isRunning() {
		return mIsRunning;
	}

	/**
	 * Stop the {@link Streamer} loop if it's running on a separate thread.
	 * <p>
	 * It does this by sending a {@link Red5Message} for the end of stream
	 * 
	 * to the {@link Streamer} and allowing it to exit gracefully.
	 * </p>
	 * 
	 * @see #run()
	 */
	public void stop() {
		try {
			mInputQueue.put(new Red5Message(Red5Message.Type.END_STREAM, null));
		} catch (InterruptedException e) {
			log.error("exception: {}", e);
		}
		mKeepRunning = false;
	}

	/**
	 * Open up all input and ouput containers (files) and being transcoding.
	 * <p>
	 * The {@link Streamer} requires its own thread to do work on, and callers are responsible for allocating
	 * the {@link Thread}.
	 * </p>
	 * <p>
	 * This method does not return unless another thread calls {@link Streamer#stop()}, or it reaches the end
	 * of a Red5 stream. It is meant to be passed as the {@link Runnable#run()} method for a thread.
	 * </p>
	 */
	public void run() {
		try {
			openContainer();
			stream();
		} catch (Throwable e) {
			log.error("uncaught exception: " + e.getMessage());
			e.printStackTrace();
		} finally {
			closeContainer();
		}
	}

	private void stream() {
		synchronized (this) {
			mIsRunning = true;
			notifyAll();
		}

		IPacket iPacket = IPacket.make();
		log.debug("Packets and Audio buffers created");

		while (mKeepRunning) {
			Red5Message message = null; 
			EtmPoint fullLoop = profiler.createPoint(this.getClass().getName() + "#transcode_loop");
			try {
				EtmPoint point = profiler.createPoint(this.getClass().getName() + "#readNextPacket");
				try {
					try {
	                    message = mInputQueue.read();
                    } catch (InterruptedException e) {
	                    // TODO Auto-generated catch block
	                    e.printStackTrace();
                    }
				} finally {
					point.collect();
				}
				Type type = message.getType();


				if (message.getAudioSampleSize() < 0) {
					log.debug("container is empty; exiting transcoding thread");
					mKeepRunning = false;
					break;
				}
				
				log.debug("next packet read");

				if (type == Red5Message.Type.AUDIO) {
					IRTMPEvent data = message.getData();
					AudioData d = (AudioData)data;
					IoBuffer buf = d.getData();
					byte[] rawBytes = buf.array();
					log.info("buffer "+rawBytes.length+" "+d.getTimestamp()+" "+data.getHeader().toString());
		
					if (!bPipeClosed) {
							try {
								pos.write(rawBytes, 0, rawBytes.length);
							} catch (Exception e) {
								e.printStackTrace();
							}
	
					}
				} else {
					log.info(type.toString());
				}

			} finally {
				fullLoop.collect();
			}
		}
	}

	private void openContainer() {
		EtmPoint point = profiler.createPoint(this.getClass().getName() + "#open");
		try {
			// set out thread name
			String threadName = "Transcoder[" + mInputStream.getPublishedName() + "]";
			log.debug("Changing thread name: {}; to {};", Thread.currentThread().getName(), threadName);
			Thread.currentThread().setName(threadName);
			// Register a new listener; should hopefully start getting audio packets immediately
			log.debug("Adding packet listener to stream: {}", mInputStream.getPublishedName());
			mInputStream.addStreamListener(mInputListener);
		} finally {
			point.collect();
		}

	}


	private void closeContainer() {
		EtmPoint point = profiler.createPoint(this.getClass().getName() + "#close");
		try {
			try {
				mInputStream.removeStreamListener(mInputListener);
			} finally {
				synchronized (this) {
					mIsRunning = false;
					notifyAll();
				}
			}
		} finally {
			point.collect();
		}

	}


	private Boolean bPipeClosed = false;

	public void closePipe() {
		bPipeClosed = true;
	}

	public void closer() {
		try {
			pos.flush();
			pos.close();

		} catch (Exception e) {

		}
	}


	public void flushPipes() {
		try {
			pos.flush();
		} catch (Exception e) {
		}
	}

	public void setPipes(PipedInputStream apis, PipedOutputStream apos) {
		pis = apis;
		pos = apos;
	}

}
