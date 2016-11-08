/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mediatek.rcse.plugin.phone;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import org.gsma.joyn.vsh.IVideoRendererListener;
import org.gsma.joyn.vsh.VideoCodec;
import org.gsma.joyn.vsh.VideoRenderer;
import com.mediatek.rcse.plugin.phone.VideoSurfaceView;

import android.graphics.Bitmap;
import android.os.RemoteException;
import android.os.SystemClock;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

import com.mediatek.rcse.rtp.AndroidDatagramConnection;
import com.mediatek.rcse.rtp.DummyPacketGenerator;
import com.mediatek.rcse.rtp.MediaRegistry;
import com.mediatek.rcse.rtp.VideoRtpReceiver;
import com.mediatek.rcse.rtp.codec.video.h264.decoder.NativeH264Decoder;
import com.mediatek.rcse.rtp.format.video.CameraOptions;
import com.mediatek.rcse.rtp.format.video.Orientation;
import com.mediatek.rcse.rtp.format.video.VideoFormat;
import com.mediatek.rcse.rtp.format.video.VideoOrientation;
import com.mediatek.rcse.rtp.media.MediaOutput;
import com.mediatek.rcse.rtp.media.MediaSample;
import com.mediatek.rcse.rtp.media.VideoSample;
import com.mediatek.rcse.rtp.stream.RtpInputStream;
import com.mediatek.rcse.rtp.stream.RtpStreamListener;
import com.mediatek.rcse.rtp.DatagramConnection;
import com.mediatek.rcse.rtp.CodecsUtils;
import com.mediatek.rcse.rtp.NetworkRessourceManager;
import com.mediatek.rcse.rtp.Logger;

/**
 * Video RTP renderer. Only the H264 QCIF format is supported.
 * 
 * @author jexa7410
 */
public class LiveVideoRenderer extends VideoRenderer implements
		RtpStreamListener {

	private byte[] sps = null;
	private byte[] pps = null;
	private boolean spsinit = false;
	private boolean ppsinit = false;
	/**
	 * List of supported video codecs
	 */
	private VideoCodec[] supportedMediaCodecs = null;

	private NativeH264Decoder decoder = null;

	/**
	 * Selected video codec
	 */
	private VideoCodec selectedVideoCodec = null;

	/**
	 * Video format
	 */
	private VideoFormat videoFormat;

	/**
	 * RtpInputStream shared with the renderer
	 */
	private RtpInputStream rendererRtpInputStream = null;

	/**
	 * Local RTP port
	 */
	private int localRtpPort;

	/**
	 * RTP receiver session
	 */
	private VideoRtpReceiver rtpReceiver = null;

	/**
	 * RTP dummy packet generator
	 */
	private DummyPacketGenerator rtpDummySender = null;

	/**
	 * RTP media output
	 */
	private MediaRtpOutput rtpOutput = null;

	/**
	 * Is player opened
	 */
	private boolean opened = false;

	/**
	 * Is player started
	 */
	private boolean started = false;

	/**
	 * Video start time
	 */
	private long videoStartTime = 0L;

	/**
	 * Video surface
	 */
	private VideoSurfaceView surface = null;

	/**
	 * Media event listeners
	 */
	private Vector<IVideoRendererListener> listeners = new Vector<IVideoRendererListener>();

	/**
	 * Temporary connection to reserve the port
	 */
	private DatagramConnection temporaryConnection = null;

	/**
	 * Orientation header id.
	 */
	private int orientationHeaderId = -1;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public LiveVideoRenderer() {

		decoder = new NativeH264Decoder();
		// Set the local RTP port
		localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
		reservePort(localRtpPort);

		// Init codecs
		supportedMediaCodecs = CodecsUtils.getRendererCodecList();
		if (supportedMediaCodecs.length > 0) {
			setVideoCodec(supportedMediaCodecs[0]);
		}

	}

	/**
	 * Constructor with a list of video codecs
	 * 
	 * @param codecs
	 *            Ordered list of codecs (preferred codec in first)
	 */
	public LiveVideoRenderer(VideoCodec[] codecs) {
		// Set the local RTP port
		decoder = new NativeH264Decoder();

		localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
		reservePort(localRtpPort);

		// Init codecs
		supportedMediaCodecs = codecs;

	}

	/**
	 * Set the surface to render video
	 * 
	 * @param surface
	 *            Video surface
	 */
	public void setVideoSurface(VideoSurfaceView surface) {
		this.surface = surface;
	}

	/**
	 * Return the video start time
	 * 
	 * @return Milliseconds
	 */
	public long getVideoStartTime() {
		return videoStartTime;
	}

	/**
	 * Returns the local RTP port
	 * 
	 * @return Port
	 */
	public int getLocalRtpPort() {
		return localRtpPort;
	}

	/**
	 * Returns the local RTP stream (set after the open)
	 * 
	 * @return RtpInputStream
	 */
	public RtpInputStream getRtpInputStream() {
		return rendererRtpInputStream;
	}

	/**
	 * Reserve a port
	 * 
	 * @param port
	 *            Port to reserve
	 */
	private void reservePort(int port) {
		if (temporaryConnection == null) {
			try {
				temporaryConnection = new AndroidDatagramConnection();
				temporaryConnection.open(port);
			} catch (IOException e) {
				temporaryConnection = null;
			}
		}
	}

	/**
	 * Release the reserved port.
	 */
	private void releasePort() {
		if (temporaryConnection != null) {
			try {
				temporaryConnection.close();
			} catch (IOException e) {
				temporaryConnection = null;
			}
		}
	}

	/**
	 * Is player opened
	 * 
	 * @return Boolean
	 */
	public boolean isOpened() {
		return opened;
	}

	/**
	 * Is player started
	 * 
	 * @return Boolean
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * Open the renderer
	 * 
	 * @param remoteHost
	 *            Remote host
	 * @param remotePort
	 *            Remote port
	 */
	public void open(VideoCodec codec, String remoteHost, int remotePort) {
		if (opened) {
			// Already opened
			return;
		}
		selectedVideoCodec = codec;

		// Check video codec
		if (selectedVideoCodec == null) {
			notifyPlayerEventError("Video Codec not selected");
			return;
		}

		try {
			// Init the video decoder
			int result = decoder.InitDecoder( surface);
			if (result != 0) {
				notifyPlayerEventError("Decoder init failed with error code "
						+ result);
				return;
			}
		} catch (UnsatisfiedLinkError e) {
			notifyPlayerEventError(e.getMessage());
			return;
		}

		try {
			// Init the RTP layer
			releasePort();
			rtpReceiver = new VideoRtpReceiver(localRtpPort);
			rtpDummySender = new DummyPacketGenerator();
			rtpOutput = new MediaRtpOutput();
			rtpOutput.open();
			rtpReceiver.prepareSession(remoteHost, remotePort,
					orientationHeaderId, rtpOutput, videoFormat, this);
			rendererRtpInputStream = rtpReceiver.getInputStream();
			rtpDummySender.prepareSession(remoteHost, remotePort,
					rtpReceiver.getInputStream());
			rtpDummySender.startSession();
		} catch (Exception e) {
			notifyPlayerEventError(e.getMessage());
			return;
		}

		// Player is opened
		opened = true;
		notifyPlayerEventOpened();
	}

	/**
	 * Close the renderer
	 */
	public synchronized void close() {
		if (!opened) {
			// Already closed
			return;
		}

		// Close the RTP layer
		rtpOutput.close();
		rtpReceiver.stopSession();
		rtpDummySender.stopSession();

		try {
			// Close the video decoder
			decoder.DeinitDecoder();
		} catch (UnsatisfiedLinkError e) {
			if (logger.isActivated()) {
				logger.error("Can't close correctly the video decoder", e);
			}
		}

		// Player is closed
		opened = false;
		notifyPlayerEventClosed();
	}

	/**
	 * Start the player
	 */
	public void start() {
		if (!opened) {
			// Player not opened
			return;
		}

		if (started) {
			// Already started
			return;
		}

		// Start RTP layer
		rtpReceiver.startSession();

		// Renderer is started
		videoStartTime = SystemClock.uptimeMillis();
		started = true;
		notifyPlayerEventStarted();
	}

	/**
	 * Stop the renderer
	 */
	public void stop() {
		if (!started) {
			return;
		}

		// Stop RTP layer
		if (rtpReceiver != null) {
			rtpReceiver.stopSession();
		}
		if (rtpDummySender != null) {
			rtpDummySender.stopSession();
		}
		if (rtpOutput != null) {
			rtpOutput.close();
		}

		// Force black screen
		surface.clearImage();

		// Renderer is stopped
		started = false;
		videoStartTime = 0L;
		notifyPlayerEventStopped();
	}

	/**
	 * Add a media event listener
	 * 
	 * @param listener
	 *            Media event listener
	 */
	public void addEventListener(IVideoRendererListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * Add a media event listener
	 * 
	 * @param listener
	 *            Media event listener
	 */
	public void removeEventListener(IVideoRendererListener listener) {
		listeners.removeElement(listener);
	}

	/**
	 * Remove all media event listeners
	 */
	public void removeAllListeners() {
		listeners.removeAllElements();
	}

	/**
	 * Get supported video codecs
	 * 
	 * @return media Codecs list
	 */
	public VideoCodec[] getSupportedCodecs() {
		return supportedMediaCodecs;
	}

	/**
	 * Get video codec
	 * 
	 * @return Video codec
	 */
	public VideoCodec getCodec() {
		if (selectedVideoCodec == null)
			return null;
		else
			return selectedVideoCodec;
	}

	/**
	 * Set video codec
	 * 
	 * @param mediaCodec
	 *            Media codec
	 */
	public void setVideoCodec(VideoCodec mediaCodec) {
		if (mediaCodec.compare(supportedMediaCodecs[0])) {
			selectedVideoCodec = mediaCodec;
			videoFormat = (VideoFormat) MediaRegistry.generateFormat(mediaCodec
					.getEncoding());
		} else {
			notifyPlayerEventError("Codec not supported");
		}
	}

	/**
	 * Notify RTP aborted
	 */
	public void rtpStreamAborted() {
		notifyPlayerEventError("RTP session aborted");
	}

	/**
	 * Set extension header orientation id
	 * 
	 * @param headerId
	 *            extension header orientation id
	 */
	public void setOrientationHeaderId(int headerId) {
		this.orientationHeaderId = headerId;
	}

	/**
	 * Notify player event started
	 */
	private void notifyPlayerEventStarted() {
		if (logger.isActivated()) {
			logger.debug("Player is started");
		}
		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				((IVideoRendererListener) ite.next()).onRendererStarted();
			} catch (RemoteException e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Notify player event resized
	 */
	private void notifyPlayerEventResized(int width, int height) {
		if (logger.isActivated()) {
			logger.debug("The media size has changed");
		}
		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				// ((IVideoRendererListener)ite.next()).mediaResized(width,
				// height);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Notify player event stopped
	 */
	private void notifyPlayerEventStopped() {
		if (logger.isActivated()) {
			logger.debug("Player is stopped");
		}
		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				((IVideoRendererListener) ite.next()).onRendererStopped();
			} catch (RemoteException e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Notify player event opened
	 */
	private void notifyPlayerEventOpened() {
		if (logger.isActivated()) {
			logger.debug("Player is opened");
		}
		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				((IVideoRendererListener) ite.next()).onRendererOpened();
			} catch (RemoteException e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Notify player event closed
	 */
	private void notifyPlayerEventClosed() {
		if (logger.isActivated()) {
			logger.debug("Player is closed");
		}
		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				((IVideoRendererListener) ite.next()).onRendererClosed();
			} catch (RemoteException e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Notify player event error
	 */
	private void notifyPlayerEventError(String error) {
		if (logger.isActivated()) {
			logger.debug("Renderer error: " + error);
		}

		Iterator<IVideoRendererListener> ite = listeners.iterator();
		while (ite.hasNext()) {
			try {
				((IVideoRendererListener) ite.next()).onRendererFailed();
			} catch (RemoteException e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
	}

	/**
	 * Media RTP output
	 */
	private class MediaRtpOutput implements MediaOutput {
		/**
		 * Bitmap frame
		 */
		private Bitmap rgbFrame = null;

		/**
		 * Video orientation
		 */
		private VideoOrientation videoOrientation = new VideoOrientation(
				CameraOptions.BACK, Orientation.NONE);

		/**
		 * Frame dimensions Just 2 - width and height
		 */
		private int decodedFrameDimensions[] = new int[2];

		/**
		 * Constructor
		 */
		public MediaRtpOutput() {
			// Init rgbFrame with a default size
			rgbFrame = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
		}

		/**
		 * Open the renderer
		 */
		public void open() {
			// Nothing to do
		}

		/**
		 * Close the renderer
		 */
		public void close() {
		}

		/**
		 * Write a media sample
		 * 
		 * @param sample
		 *            Sample
		 */
		public void writeSample(MediaSample sample) {
			rtpDummySender.incomingStarted();

			// Init orientation
			VideoOrientation orientation = ((VideoSample) sample)
					.getVideoOrientation();
			if (orientation != null) {
				this.videoOrientation = orientation;
			}

			if (spsinit == false) {
				sps = sample.getData();
				spsinit = true;

				return;
			}
			if (ppsinit == false) {
				pps = sample.getData();
				ppsinit = true;
				decoder.configureDecoder(sps, pps);
				return;
			}

			/*
			 * try { String path = "/data/data/rendererSamples.txt"; File file =
			 * new File(path); FileOutputStream stream = new
			 * FileOutputStream(path, true); try {
			 * stream.write(sample.getData());
			 * 
			 * } catch (java.io.IOException ww) { ww.printStackTrace(); } }
			 * catch (FileNotFoundException e1) { e1.printStackTrace(); }
			 */
			decoder.DecodeAndConvert(sample.getData(), videoOrientation
					.getOrientation().getValue(), decodedFrameDimensions);

		}

	}
}
