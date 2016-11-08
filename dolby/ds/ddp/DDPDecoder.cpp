/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "DDP_JOCDecoder"
#include "DlbLog.h"

#include "DDPDecoder.h"
#include "ddpdec_client.h"
#include "udc_api.h"
#include "ds_config.h"

#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <cutils/properties.h>
#include <OMX_AudioExt.h>
#include <OMX_IndexExt.h>

#define DEFAULT_SAMPLERATE      (48000ul)
#define DEFAULT_OUT_CHANNELS    (2)
#define CHANBIT(A)              (((unsigned)0x8000) >> (A))
/* DDPDEC_OUT_BUF_SIZE is a fixed value defining the number of bytes of PCM
 * data that will be written on a successful frame decode, assuming a maximal
 * number of decoded channels
 *
 *IF this is a 2-channel only system, and multichannel PCM will never be
 *required, then the number of output chans to create a buffer for can be
 *reduced to 2 by replacing MAX_PCM_CHANS with 2. MAX_PCM_CHANS will
 *enumerate to 8.
 */
static const int DDPDEC_OUT_BUF_SIZE = GBL_BLKSIZE * GBL_MAXBLOCKS * DEC_MAX_OUT_OBJECTS * GBL_BYTESPERWRD;

/*
 * The MAXFRAMESINOUTPUTBUFFER value defines the maximum number of audio
 * frames that there will be room for in the output PCM buffer. This must be
 * bounded by the size of the AudioTrack's buffer.
 *
 * Where buffer->size() = DDPDEC_OUT_BUF_SIZE * MAXFRAMESINOUTPUTBUFFER
 */
static const int MAXFRAMESINOUTPUTBUFFER = 3;

/* FRAMERBUFFERSIZE is defined by assuming that the maximum amount of data that
 * could be carried over to the next buffer read cycle is 1 frame. It is set
 * to 2 frames for safety. This value is the
 * 2 * ( size of DD+ word (larger than DD) * num. bytes/word)
 */
static const int FRAMERBUFFERSIZE = 2* (GBL_MAXDDPFRMWRDS * GBL_BYTESPERWRD);

namespace android {

template<class T>
static void InitOMXParams(T *params) {
    params->nSize = sizeof(T);
    params->nVersion.s.nVersionMajor = 1;
    params->nVersion.s.nVersionMinor = 0;
    params->nVersion.s.nRevision = 0;
    params->nVersion.s.nStep = 0;
}

DDPDecoder::DDPDecoder(
        const char *name,
        const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData,
        OMX_COMPONENTTYPE **component)
    : SimpleSoftOMXComponent(name, callbacks, appData, component),
      mARenderer(NULL),
      mConfig(new tDdpDecoderExternal),
      mDecoder(NULL),
      mAnchorTimeUs(0),
      mNumFramesOutput(0),
      mLastChannelCount(0),
      mLastSamplingRate(0),
      mLastDRCmode(DDPI_UDC_COMP_PORTABLE_L14),
      mPrepareOutputPortChange(false),
      mIsEC3(false),
      mIsJOC(false),
      mSignalledError(false),
      mStarted(false),
      mFadeIn(false),
      mCurJocDapOn(false),
      mLastJocDapOn(false),
      mIsJocOutput(false),
      mUpdateDDPSystemProperty(true),
      mOutputPortSettingsChange(NONE),
      mLastMediaTimeUs(0),
      mLastAdjustedTimeUs(0){

      init(name);
      initPorts();

}

void DDPDecoder::init(const char *name) {
    ALOGI("-> init name %s", name);

    if (!strcmp(name, "OMX.dolby.ec3.decoder")) {
        mIsEC3 = true;
    } else if (!strcmp(name, "OMX.dolby.ec3_joc.decoder")) {
#ifdef DOLBY_UDC_VIRTUALIZE_AUDIO    
        mIsJOC = true;
#else
        mIsEC3 = true;
        ALOGW("%s JOC mode decoding is not supported, defaulting to EAC3!", __FUNCTION__);	
#endif // DOLBY_END
    } else {
        CHECK(!strcmp(name, "OMX.dolby.ac3.decoder"));
    }
#ifdef DOLBY_UDC_FORCE_JOC_OUTPUT
    mIsJOC = true;
#endif // DOLBY_END
    memset(mConfig, 0, sizeof(tDdpDecoderExternal));
    mConfig->nrChans = -1;
    mConfig->nPCMOutMaxChannels = -1;

    if (mIsJOC) {
        mARenderer = new ARenderer();
        mARenderer->init(DEFAULT_SAMPLERATE);
    }
    ALOGI("<- init");
}

void DDPDecoder::closeARenderer()
{
    if(mARenderer) {
        mARenderer->deinit();
        delete mARenderer;
        mARenderer = NULL;
    }
}

DDPDecoder::~DDPDecoder() {
    ALOGI("-> ~DDPDecoder");

    if (mStarted) {
        stop();
    }

    closeARenderer();

    delete mConfig;
    mConfig = NULL;

    ALOGI("<- ~DDPDecoder");
}

void DDPDecoder::initPorts() {
    ALOGD("%s", __FUNCTION__);
    OMX_PARAM_PORTDEFINITIONTYPE def;
    InitOMXParams(&def);

    def.nPortIndex = 0;
    def.eDir = OMX_DirInput;
    def.nBufferCountMin = kNumInputBuffers;
    def.nBufferCountActual = def.nBufferCountMin;
    def.nBufferSize = FRAMERBUFFERSIZE;
    def.bEnabled = OMX_TRUE;
    def.bPopulated = OMX_FALSE;
    def.eDomain = OMX_PortDomainAudio;
    def.bBuffersContiguous = OMX_FALSE;
    def.nBufferAlignment = 1;

    def.format.audio.cMIMEType =
        const_cast<char *>(
                    mIsJOC
                    ? MEDIA_MIMETYPE_AUDIO_EAC3_JOC
                    :
                    mIsEC3
                    ? MEDIA_MIMETYPE_AUDIO_EAC3
                    : MEDIA_MIMETYPE_AUDIO_AC3);

    def.format.audio.pNativeRender = NULL;
    def.format.audio.bFlagErrorConcealment = OMX_FALSE;
    def.format.audio.eEncoding = (OMX_AUDIO_CODINGTYPE)OMX_AUDIO_CodingAndroidAC3;

    addPort(def);

    def.nPortIndex = 1;
    def.eDir = OMX_DirOutput;
    def.nBufferCountMin = kNumOutputBuffers;
    def.nBufferCountActual = def.nBufferCountMin;

    def.nBufferSize = DDPDEC_OUT_BUF_SIZE * MAXFRAMESINOUTPUTBUFFER;
    def.bEnabled = OMX_TRUE;
    def.bPopulated = OMX_FALSE;
    def.eDomain = OMX_PortDomainAudio;
    def.bBuffersContiguous = OMX_FALSE;
    def.nBufferAlignment = 2;

    def.format.audio.cMIMEType = const_cast<char *>("audio/raw");
    def.format.audio.pNativeRender = NULL;
    def.format.audio.bFlagErrorConcealment = OMX_FALSE;
    def.format.audio.eEncoding = OMX_AUDIO_CodingPCM;

    addPort(def);
}

void DDPDecoder::updateDDPSystemProperties() {

    static char propJocDecoding[] = "dolby.audio.ddp.joc_decoding";
    static char propGlobalDapOn[] = "dolby.audio.ddp.glb_dap_bypass";
    static char propQmfOutput[]   = "dolby.audio.ddp.qmf_output";
    static char propHpVirtualizer[] = "dolby.audio.ddp.hp_virtu_on";
       
    property_set(propJocDecoding, mIsJocOutput ? "1" : "0");  
    property_set(propGlobalDapOn, mCurJocDapOn ? "1" : "0");   
    property_set(propQmfOutput, mConfig->isDataInQMF ? "1" : "0");
    property_set(propHpVirtualizer, mEndpConfigTable.isHpVirtualizerOn(mConfig->isJocOutput) ? "1": "0");
}

/* Reconfig on run time endpoint change
 *
 * In AOSP's MediaPlayer context, output channel count should be aligned
 * to AudioTrack's expected channel count. On change in Endpoint there
 * are cases in which Audio tear down event was not happening resulting in 
 * audio distortion as AOSP's AudioPlayer can't handle change in channel count
 * during run-time. At the same time there are other clients capable of handling 
 * change in channel count during run-time. The below method provides 
 * flexibility to change this behaviour in run-time.
 */
void DDPDecoder::setReconfigOnEndpChange(bool activateReconfig)
{ 
    mEndpConfigTable.setReconfigOnEndpChange(activateReconfig);
    ALOGV("%s %d", __FUNCTION__, activateReconfig);
}

bool DDPDecoder::isReconfigOnEndpChange()
{ 
    return mEndpConfigTable.isReconfigOnEndpChange(); 
}

int DDPDecoder::channelMaptoChannelCount(int chanMap)
{
    int count = 0;
    for (unsigned int i = 0; i < sizeof(chanMap) * 8; ++i) {
        unsigned int chanBit = chanMap & CHANBIT(i);
        if (chanBit) {
            unsigned int chanLoc = 1 << i;
            switch (chanLoc) {
                //These Bits indicate a pair of channels.
                case DDPI_UDC_CHANNEL_Cpr:
                case DDPI_UDC_CHANNEL_RSpr:
                case DDPI_UDC_CHANNEL_SDpr:
                case DDPI_UDC_CHANNEL_VHpr:
                case DDPI_UDC_CHANNEL_TSpr:
                    count += 2;
                    break;
                default:
                    count++;
            }
        }
    }
    return count;
}

void DDPDecoder::configARenderer() {

    int actualChannelCount = channelMaptoChannelCount(mConfig->outChannelMap);
    mARenderer->configure(mEndpConfigTable.getDapEndpConfig(mConfig->isJocOutput, actualChannelCount), 
                            mConfig->isJocOutput);

    if (mEndpConfigTable.isHpVirtualizerOn(mConfig->isJocOutput)) {
        mCurJocDapOn = true;
    } else {
        mCurJocDapOn = false;
    }
}

void DDPDecoder::setMaxPcmOutChannels() {
    //set max number of output channels
    int actualChannelCount = channelMaptoChannelCount(mConfig->outChannelMap);
    mConfig->nPCMOutMaxChannels = mEndpConfigTable.getMaxOutChannel(mARenderer != NULL, mConfig->isJocOutput, 
                                    actualChannelCount);
    
    ALOGD("Setting Max Output Channels to %d", mConfig->nPCMOutMaxChannels);
}

void DDPDecoder::setMultiChannelPCMOutDownmix() {

    if (mEndpConfigTable.isConfigChanged()) {

        if (mIsJOC) {
            if (mARenderer != NULL) {
                configARenderer();
                mConfig->jocForceDownmixMode = mEndpConfigTable.getJocForceDownmixMode();
                mConfig->updatedChannelRouting = 1;
                ALOGD("%s mConfig->jocForceDownmixMode %d", __FUNCTION__, mConfig->jocForceDownmixMode);
            }
        } else {
            //set drc mode
            mConfig->drcMode = mEndpConfigTable.getDRCmode();
            ALOGD("Endpoint Switched - Setting DRC Mode to %d", mConfig->drcMode);

            //set desired udc stereo mode
            mConfig->downmixMode = mEndpConfigTable.getStereoMode();
            mConfig->updatedChannelRouting = 1;
            ALOGD("Endpoint Switched - Setting Downmix Config to %d", mConfig->downmixMode);
        }     

        // We are expecting Audio_Tear_Down event to take care of closing the Decoder and re-opening it.
        // As of now there are some instances in which this is not happening (for example when HDMI is plugged-in and
        // Headphone is plugged out) during play-back.
        // The below logic makes sure that output channels are configured only during init time but not in run time as
        // sending OMX_EventPortSettingsChanged during run time causes crash in OMXCodec.cpp due to
        // Audio_Tear_Down event.
        // If reconfig on endpoint change is active then upper layer is capable of handling change in channel count in run-time.
        if (isReconfigOnEndpChange() || !mStarted) {
            setMaxPcmOutChannels();
        }
    }
}

status_t DDPDecoder::start() {
    ALOGI("-> start");

    CHECK(!mStarted);

    mConfig->inputBufferCurrentLength = 0;
    mConfig->inputBufferUsedLength    = 0;

    if (mIsJOC) {
        mConfig->isJocDecodeMode = true;
        mConfig->isJocOutput = true;
    }
    mIsJocOutput = mConfig->isJocOutput;
    mConfig->isDataInQMF = false;
    
    setMultiChannelPCMOutDownmix();

    mDecoder = ddpdec_open(mConfig);
    if (mDecoder == NULL) {
        ALOGE("ddpdec_open() FAIL");
        return UNKNOWN_ERROR;
    }

    if (mARenderer != NULL && mConfig->isDataInQMF == true) {
        mARenderer->setPregain(UDC_QMF_OUTPUT_LEVEL);
        mARenderer->setSystemGain(DAP_QMF_SYSTEM_GAIN);
    }

    mLastDRCmode = mConfig->drcMode;
    mAnchorTimeUs = 0;
    mNumFramesOutput = 0;
    mStarted = true;

    ALOGD("mStarted = %d", mStarted);
    ALOGI("<- start");

    return OK;
}

status_t DDPDecoder::stop() {

    //shutdown cleanly
    ALOGI("-> stop");

    CHECK(mStarted);

    mStarted = false;

    ddpdec_close(mConfig, mDecoder);
    mDecoder = NULL;

    ALOGI("<- stop");

    return OK;
}

OMX_ERRORTYPE DDPDecoder::internalGetParameter(
        OMX_INDEXTYPE index, OMX_PTR params) {
    switch (index) {
        case OMX_IndexParamAudioAndroidAc3:
        {
            OMX_AUDIO_PARAM_ANDROID_AC3TYPE *ac3Params =
                (OMX_AUDIO_PARAM_ANDROID_AC3TYPE*)params;
            if (!mStarted)
            {
                ac3Params->nChannels = DEFAULT_OUT_CHANNELS;
                ac3Params->nSampleRate = DEFAULT_SAMPLERATE;
            }
            else
            {
                ac3Params->nChannels = mConfig->nrChans;
                ac3Params->nSampleRate = mConfig->samplingRate;
            }
            return OMX_ErrorNone;
        }
        case OMX_IndexParamAudioPcm:
        {
            OMX_AUDIO_PARAM_PCMMODETYPE *pcmParams =
                (OMX_AUDIO_PARAM_PCMMODETYPE *)params;

            if (pcmParams->nPortIndex > 1) {
                return OMX_ErrorUndefined;
            }

            pcmParams->eNumData = OMX_NumericalDataSigned;
            pcmParams->eEndian = OMX_EndianBig;
            pcmParams->bInterleaved = OMX_TRUE;
            pcmParams->nBitPerSample = 16;
            pcmParams->ePCMMode = OMX_AUDIO_PCMModeLinear;

            pcmParams->eChannelMapping[0] = OMX_AUDIO_ChannelLF;
            pcmParams->eChannelMapping[1] = OMX_AUDIO_ChannelRF;
            pcmParams->eChannelMapping[2] = OMX_AUDIO_ChannelCF;
            pcmParams->eChannelMapping[3] = OMX_AUDIO_ChannelLFE;
            pcmParams->eChannelMapping[4] = OMX_AUDIO_ChannelLS;
            pcmParams->eChannelMapping[5] = OMX_AUDIO_ChannelRS;
            pcmParams->eChannelMapping[6] = OMX_AUDIO_ChannelLR;
            pcmParams->eChannelMapping[7] = OMX_AUDIO_ChannelRR;

            if (!mStarted) {
                pcmParams->nChannels        = DEFAULT_OUT_CHANNELS;
                pcmParams->nSamplingRate    = DEFAULT_SAMPLERATE;
                //Also set the 'last' parameters to the same value so we don't immediatley ask to reconfigure
                //the port after startup
                mLastSamplingRate = pcmParams->nSamplingRate;
                mLastChannelCount = pcmParams->nChannels;

            } else {
                pcmParams->nChannels = mConfig->nrChans;
                pcmParams->nSamplingRate = mConfig->samplingRate;
            }

            return OMX_ErrorNone;
        }

        case OMX_IndexDolbyReconfigOnEndpChange:
        {
            OMX_BOOL *reconfigOnEndpChangeParam = (OMX_BOOL*)params;
            *reconfigOnEndpChangeParam = isReconfigOnEndpChange() ? OMX_TRUE : OMX_FALSE;
            return OMX_ErrorNone;
        }

        default:
            return SimpleSoftOMXComponent::internalGetParameter(index, params);
    }
}

OMX_ERRORTYPE DDPDecoder::internalSetParameter(
        OMX_INDEXTYPE index, const OMX_PTR params) {
    switch (index) {
        case OMX_IndexParamAudioAndroidAc3:
            //TODO: Use the sample rate & channels to initilize port struct
            return OMX_ErrorNone;
        case OMX_IndexParamStandardComponentRole:
        {
            const OMX_PARAM_COMPONENTROLETYPE *roleParams =
                (const OMX_PARAM_COMPONENTROLETYPE *)params;

            if (strncmp((const char *)roleParams->cRole,
                    mIsJOC ? "audio_decoder.ec3_joc" : mIsEC3 ? "audio_decoder.ec3" : "audio_decoder.ac3",
                    OMX_MAX_STRINGNAME_SIZE - 1)) {
                return OMX_ErrorUndefined;
            }

            return OMX_ErrorNone;
        }

        case OMX_IndexParamAudioPcm:
        {
            const OMX_AUDIO_PARAM_PCMMODETYPE *pcmParams =
                (OMX_AUDIO_PARAM_PCMMODETYPE *)params;

            if (pcmParams->nPortIndex != 1) {
                return OMX_ErrorUndefined;
            }

            if (pcmParams->nChannels < 1 || pcmParams->nChannels > 8) {
                return OMX_ErrorUndefined;
            }

            return OMX_ErrorNone;
        }

        case OMX_IndexDolbyReconfigOnEndpChange:
        {
            const OMX_BOOL *reconfigOnEndpChangeParam = (OMX_BOOL*)params;
            setReconfigOnEndpChange(*reconfigOnEndpChangeParam != OMX_FALSE);
            return OMX_ErrorNone;
        }

        default:
            return SimpleSoftOMXComponent::internalSetParameter(index, params);
    }
}

void DDPDecoder::onQueueFilled(OMX_U32 portIndex) {

    ALOGV("-> onQueueFilled (Port %d)", (uint32_t) portIndex);

    if (mSignalledError || mOutputPortSettingsChange != NONE) {
        ALOGE("<- onQueueFilled: mSignalledError=%d mOutputPortSettingsChange=%d", mSignalledError, mOutputPortSettingsChange);
        return;
    }

    List<BufferInfo *> &inQueue = getPortQueue(0);
    List<BufferInfo *> &outQueue = getPortQueue(1);

    ALOGV("inQueue: %d items, outQueue: %d items", inQueue.size(), outQueue.size());

    if (!mStarted) {
        status_t err = start();
        if (err != OK) {
            mSignalledError = true;
            ALOGE("%s signalling ERROR", __FUNCTION__);
            notify(OMX_EventError, OMX_ErrorUndefined, err, NULL);
            return;
        }
    }

    while (!inQueue.empty() && !outQueue.empty()) {

        BufferInfo *inInfo = *inQueue.begin();
        OMX_BUFFERHEADERTYPE *inHeader = inInfo->mHeader;

        BufferInfo *outInfo = *outQueue.begin();
        OMX_BUFFERHEADERTYPE *outHeader = outInfo->mHeader;

        if (mPrepareOutputPortChange) {
            // Check if all the output buffers have been consumed.
            // If not, we'll hold onto the input buffer and will decode
            // it once the output port has been reconfigured.
            if (outQueue.size() == kNumOutputBuffers) {
                ALOGV("output parameter will be reconfigured");

                mPrepareOutputPortChange = false;
                notify(OMX_EventPortSettingsChanged, 1, 0, NULL);
                mOutputPortSettingsChange = AWAITING_DISABLED;
            }
            return;
        }

        if (inHeader->nFlags & OMX_BUFFERFLAG_EOS) {

            ALOGV("End of input stream");

            // END OF STREAM
            inQueue.erase(inQueue.begin());
            inInfo->mOwnedByUs = false;
            notifyEmptyBufferDone(inHeader);

#ifdef MTK_AOSP_ENHANCEMENT
            outHeader->nTimeStamp = 0;
#endif
            outHeader->nFilledLen = 0;
            outHeader->nFlags = OMX_BUFFERFLAG_EOS;

            outQueue.erase(outQueue.begin());
            outInfo->mOwnedByUs = false;
            notifyFillBufferDone(outHeader);
            return;
        }

        ALOGV("-> Decode loop");
        ALOGV("inHeader nOffset=%d nFilledLen=%d nTimeStamp=%lld    outHeader nOffset=%d nAllocLen=%d", (int) inHeader->nOffset, (int) inHeader->nFilledLen, inHeader->nTimeStamp, (int) outHeader->nOffset, (int) outHeader->nAllocLen);

        if (inHeader->nOffset == 0) {
            mAnchorTimeUs = inHeader->nTimeStamp;
            mNumFramesOutput = 0;
            outHeader->nTimeStamp = mAnchorTimeUs;
        }

        //wrap read data in MediaBuffer object
        MediaBuffer *decoderInputBuffer = new MediaBuffer(inHeader->pBuffer + inHeader->nOffset, inHeader->nFilledLen);
        if (decoderInputBuffer == NULL) {
            ALOGE("Invalid Read");

            // END OF STREAM
            inQueue.erase(inQueue.begin());
            inInfo->mOwnedByUs = false;
            notifyEmptyBufferDone(inHeader);

            outHeader->nFilledLen = 0;
            outHeader->nFlags = OMX_BUFFERFLAG_EOS;

            outQueue.erase(outQueue.begin());
            outInfo->mOwnedByUs = false;
            notifyFillBufferDone(outHeader);
            return;
        }

        ALOGV("Read into decoderInputBuffer");
        ALOGV("post read decoderInputBuffer->range_length() = %d",decoderInputBuffer->range_length() );
        ALOGV("post read decoderInputBuffer->range_offset() = %d",decoderInputBuffer->range_offset() );
        ALOGV("post read decoderInputBuffer->size() = %d",decoderInputBuffer->size() );

        //wrap the output buffer in MediaBuffer object
        MediaBuffer *decoderOutputBuffer = new MediaBuffer(outHeader->pBuffer + outHeader->nOffset, outHeader->nAllocLen);
        if (decoderOutputBuffer == NULL)
        {
            ALOGE("Invalid Write");
            decoderInputBuffer->release();
            decoderInputBuffer = NULL;
            return;
        }
        //make sure the buffer range is set empty
        decoderOutputBuffer->set_range(0, 0);

        //Main process/decode loop
        while ((decoderInputBuffer->range_length() != 0)
            && ((decoderOutputBuffer->size() - decoderOutputBuffer->range_length() - decoderOutputBuffer->range_offset()) >= (size_t) DDPDEC_OUT_BUF_SIZE))
        {

            ALOGV("-> Main decode loop");

            //First check to see if the output requirements for max number of channels
            //(or endpoint) has changed as this will necessitate a change in downmix mode and possible
            //output port reconfiguration
            setMultiChannelPCMOutDownmix();

            //(re)assign /update the input buffer to the decoder
            mConfig->pInputBuffer = (char *)decoderInputBuffer->data() + decoderInputBuffer->range_offset();
            mConfig->inputBufferCurrentLength = decoderInputBuffer->range_length();
            mConfig->inputBufferUsedLength = 0;
            //(re)assign the output buffer
            mConfig->pOutputBuffer = (char *)decoderOutputBuffer->data() + decoderOutputBuffer->range_offset() + decoderOutputBuffer->range_length();

            //Frame decoded here
            ERROR_CODE decoderErr = static_cast<ERROR_CODE>(ddpdec_process(mConfig, mDecoder));

            if (decoderErr == DDPAUDEC_SUCCESS) {
                ALOGV("%s mConfig->nrChans after decoder is %d", __FUNCTION__, mConfig->nrChans);
                if (mIsJOC && (mIsJocOutput != mConfig->isJocOutput)) {
                    ALOGV("%s JocOutput Mode changed from %d to %d", __FUNCTION__, mIsJocOutput, mConfig->isJocOutput);
                    if (mARenderer != NULL) {
                        configARenderer();
                        setMaxPcmOutChannels();
                    }
                    mIsJocOutput = mConfig->isJocOutput;
                    mUpdateDDPSystemProperty = true;
                }

                if ( mARenderer != NULL ) {
                    ALOGV("%s mARenderer in chain", __FUNCTION__);
                    char *p_outbuf_align = (char *) ((((long) mConfig->pOutMem) +(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1)) & ~(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));
                    mARenderer->process(mConfig->nrChans, mConfig->nPCMOutMaxChannels, mConfig->samplingRate,
                                        p_outbuf_align, mConfig->pOutputBuffer, mConfig->pEvoFrameData,
                                        mConfig->evoFrameSize, mConfig->frameLengthSamplesPerChannel,
                                        mConfig->isDataInQMF);
                    mConfig->nrChans = mConfig->nPCMOutMaxChannels;
                }

                // Check on the sampling rate and number of channels to see whether either have changed.
                if (mConfig->samplingRate != mLastSamplingRate) {
                    ALOGI("Sample rate changed - was %d Hz, is now is %d Hz", mLastSamplingRate, mConfig->samplingRate);
                    mLastSamplingRate = mConfig->samplingRate;
                    mPrepareOutputPortChange = true;
                }

                if (mConfig->nrChans != mLastChannelCount) {
                    ALOGI("Number of output channels changed -  was %d, is now %d", mLastChannelCount, mConfig->nrChans);
                    mLastChannelCount = mConfig->nrChans;
                    mPrepareOutputPortChange = true;
                }

                if (mFadeIn) {
                    fadeInBuffer((void *)mConfig->pOutputBuffer, mConfig->nrChans, mConfig->frameLengthSamplesPerChannel);
                    mFadeIn = false;
                }

                if (mConfig->drcMode != mLastDRCmode){
                    ALOGI("DRC Mode changed -  was %d, is now %d", mLastDRCmode, mConfig->drcMode);
                    mLastDRCmode = mConfig->drcMode;

                    // In our current implementation we are setting the new DRC value to decoder only after current frame set. Hence the
                    // first frame after changing the DRC always contains the old value. We decided to do memset of zeros, as we dont want
                    // to see issue with A/V sync.
                    memset(mConfig->pOutputBuffer, 0, mConfig->nrChans * mConfig->frameLengthSamplesPerChannel * GBL_BYTESPERWRD);
                    mFadeIn = true;
                }

                if (mPrepareOutputPortChange) {
                    // if the size of outQueue is equal to the allocated output port buffer count,
                    // it means all the output buffers have been consumed and it is able to
                    // tirgger the port change event.
                    if (outQueue.size() == kNumOutputBuffers) {
                        ALOGD("output parameter will be reconfigured");

                        mPrepareOutputPortChange = false;
                        notify(OMX_EventPortSettingsChanged, 1, 0, NULL);
                        mOutputPortSettingsChange = AWAITING_DISABLED;

                    }
					//remove reference to decoder input and out buffer
                    if (decoderInputBuffer) {
                        decoderInputBuffer->release();
                        decoderInputBuffer = NULL;
                    }
                    if (decoderOutputBuffer) {
                        decoderOutputBuffer->release();
                        decoderOutputBuffer = NULL;
                    }
                    return;
                }

                // Notify OMXCodec if state of JOC has changed
                if(mLastJocDapOn != mCurJocDapOn) {
                        mLastJocDapOn = mCurJocDapOn;
                        mUpdateDDPSystemProperty = true;
                        ALOGI("%s OMX_EventDolbyProcessedAudio %d", __FUNCTION__, mCurJocDapOn);
                        notify(static_cast<OMX_EVENTTYPE>(OMX_EventDolbyProcessedAudio), mCurJocDapOn, 0, NULL);
                }
            } else {
                mConfig->frameLengthSamplesPerChannel = 0;
                mConfig->samplingRate = mLastSamplingRate;
                mConfig->nrChans = mLastChannelCount;
            }

            int numOutBytes = mConfig->nrChans * mConfig->frameLengthSamplesPerChannel * GBL_BYTESPERWRD;

            ALOGV("mConfig->frameLengthSamplesPerChannel = %d", mConfig->frameLengthSamplesPerChannel);
            ALOGV("mConfig->sampling rate = %d", mConfig->samplingRate);
            ALOGV("mConfig->nrChans = %d ", mConfig->nrChans);
            ALOGV("numOutBytes: %d channels, %d samples/chan -> %d numOutBytes", mConfig->nrChans, mConfig->frameLengthSamplesPerChannel, numOutBytes);
            ALOGV("mConfig->inputBufferUsedLength = %d",  mConfig->inputBufferUsedLength);

            // update used space in input buffer
            decoderInputBuffer->set_range((decoderInputBuffer->range_offset() + mConfig->inputBufferUsedLength),(decoderInputBuffer->range_length() - mConfig->inputBufferUsedLength));
            // update used space in output buffer
            decoderOutputBuffer->set_range((decoderOutputBuffer->range_offset()), (decoderOutputBuffer->range_length() + numOutBytes));

            ALOGV("decoderInputBuffer->range_offset() = %d, decoderInputBuffer->range_length() = %d",decoderInputBuffer->range_offset(), decoderInputBuffer->range_length());
            ALOGV("decoderOutputBuffer->range_offset() = %d, decoderOutputBuffer->range_length() = %d",decoderOutputBuffer->range_offset(), decoderOutputBuffer->range_length());
            ALOGV("Space remaining in output buffer: %d", (decoderOutputBuffer->size() - decoderOutputBuffer->range_length()));

            mNumFramesOutput += (mConfig->frameLengthSamplesPerChannel * mConfig->nrChans);

            int64_t keytime = (mAnchorTimeUs + (mNumFramesOutput * 1000000) / (mConfig->samplingRate ? mConfig->samplingRate : 1));
            ALOGV("after meta data set");
            ALOGV("kKeyTime = %lld", keytime);
            ALOGV("manchortimeUs = %lld", mAnchorTimeUs);
            ALOGV("mNumFramesOutput = %lld", mNumFramesOutput);

            if (mUpdateDDPSystemProperty) {
                updateDDPSystemProperties();
                mUpdateDDPSystemProperty = false;
            }

            ALOGV("<- Main decode loop");
        } // end of while loop

        if (mLastMediaTimeUs != inHeader->nTimeStamp) {
            //reset adjustments
            mLastAdjustedTimeUs = 0;
            mLastMediaTimeUs = inHeader->nTimeStamp;
        } else {
            if (mConfig->samplingRate != 0) {
                int64_t delta = (mConfig->frameLengthSamplesPerChannel * 1000000) / mConfig->samplingRate;
                ALOGV("delta = %lld", delta);
                mLastAdjustedTimeUs += delta;
            } else {
                mLastAdjustedTimeUs = 0;
            }
        }
        ALOGV("mLastAdjustedTimeUs = %lld, mLastMediaTimeUs = %lld, wouldBeTime = %lld", mLastAdjustedTimeUs, mLastMediaTimeUs, (mLastMediaTimeUs + mLastAdjustedTimeUs) );

        //we used whole input buffer
        inHeader->nOffset += inHeader->nFilledLen;
        inHeader->nFilledLen = 0;
        inInfo->mOwnedByUs = false;
        inQueue.erase(inQueue.begin());
        inInfo = NULL;
        notifyEmptyBufferDone(inHeader);
        inHeader = NULL;

        //We're done with the input buffer, so release it
        if (decoderInputBuffer != NULL) {
            ALOGV("Release decoderInputBuffer");
            decoderInputBuffer->release();
            decoderInputBuffer = NULL;
        }

        if (decoderOutputBuffer != NULL) {
            if (decoderOutputBuffer->range_length() != 0) {
                //update the output buffer header object
                outHeader->nFilledLen = decoderOutputBuffer->range_length();
                outHeader->nOffset = 0;
                outHeader->nFlags = 0;
                outHeader->nTimeStamp = mLastMediaTimeUs + mLastAdjustedTimeUs;
                outInfo->mOwnedByUs = false;
                outQueue.erase(outQueue.begin());
                outInfo = NULL;
                notifyFillBufferDone(outHeader);
                outHeader = NULL;
            }
            ALOGV("Release decoderOutputBuffer");
            decoderOutputBuffer->release();
            decoderOutputBuffer = NULL;
        }
    }

    ALOGV("<- onQueueFilled");
}

void DDPDecoder::onPortFlushCompleted(OMX_U32 portIndex) {
    ALOGV("onPortFlushCompleted(%d)", (int) portIndex);

    if (portIndex == 0) {
        //clear any existing input/remainder buffers
        mNumFramesOutput = 0;
    }
}

void DDPDecoder::onReset() {
    ALOGV("onReset() called");
}

void DDPDecoder::onPortEnableCompleted(OMX_U32 portIndex, bool enabled) {
    if (portIndex != 1) {
        return;
    }

    switch (mOutputPortSettingsChange) {
        case NONE:
            break;

        case AWAITING_DISABLED:
        {
            CHECK(!enabled);
            mOutputPortSettingsChange = AWAITING_ENABLED;
            break;
        }

        default:
        {
            CHECK_EQ((int)mOutputPortSettingsChange, (int)AWAITING_ENABLED);
            CHECK(enabled);
            mOutputPortSettingsChange = NONE;
            break;
        }
    }
}

void DDPDecoder::fadeInBuffer(void* data, int nrChans, int frameLengthSamplesPerChannel)
{
#ifdef DOLBY_UDC_OUTPUT_TWO_BYTES_PER_SAMPLE
    int16_t *dataPtr = (int16_t *) data;
#else
    int *dataPtr = (int *) data;
#endif
    float fadeFactor = 0.0;
    float fadeStepSize = 1.0 / (mConfig->frameLengthSamplesPerChannel);
    int32_t index;
    ALOGV("Fade Implementation: Fading in buffer 0x%p of size = %d", dataPtr, nrChans * frameLengthSamplesPerChannel * GBL_BYTESPERWRD);
    for (int i = 0; i < mConfig->frameLengthSamplesPerChannel; i++)
    {
        for(int ch = 0; ch < mConfig->nrChans; ch++)
        {
            index = i * mConfig->nrChans + ch;
            dataPtr[index] = ((dataPtr[index] * fadeFactor) + 0.5);
        }
        fadeFactor += fadeStepSize;
    }
}

OMX_ERRORTYPE DDPDecoder::getExtensionIndex(const char *name, OMX_INDEXTYPE *index)
{
    if (!strcmp(name, OMX_EventDolbyProcessedAudioString))
    {
        *index = static_cast<OMX_INDEXTYPE>(OMX_EventDolbyProcessedAudio);
        return OMX_ErrorNone;
    }

    if (!strcmp(name, OMX_IndexDolbyReconfigOnEndpChangeString))
    {
        *index = static_cast<OMX_INDEXTYPE>(OMX_IndexDolbyReconfigOnEndpChange);
        return OMX_ErrorNone;
    }

    return SimpleSoftOMXComponent::getExtensionIndex(name, index);
}

}  // namespace android

android::SoftOMXComponent *createSoftOMXComponent(
        const char *name, const OMX_CALLBACKTYPE *callbacks,
        OMX_PTR appData, OMX_COMPONENTTYPE **component) {
    return new android::DDPDecoder(name, callbacks, appData, component);
}
