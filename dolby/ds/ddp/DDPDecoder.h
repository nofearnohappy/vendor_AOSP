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
#ifndef DDP_DECODER_H
#define DDP_DECODER_H

#include "MediaSource.h"
#include "MetaData.h"
#include "SimpleSoftOMXComponent.h"
#include "ARenderer/ARenderer.h"
#include "EndpConfig.h"

struct tDdpDecoderExternal;

namespace android {

using namespace dolby;

struct MediaBufferGroup;

#define DS_UDC_MODE_CHANGE_FIX

struct DDPDecoder : public SimpleSoftOMXComponent {
    DDPDecoder(const char *name,const OMX_CALLBACKTYPE *callbacks,
            OMX_PTR appData,
            OMX_COMPONENTTYPE **component);

    virtual status_t start();
    virtual status_t stop();
    virtual void setMultiChannelPCMOutDownmix();

protected:
    virtual ~DDPDecoder();

    virtual OMX_ERRORTYPE internalGetParameter(
            OMX_INDEXTYPE index, OMX_PTR params);

    virtual OMX_ERRORTYPE internalSetParameter(
            OMX_INDEXTYPE index, const OMX_PTR params);

    virtual void onQueueFilled(OMX_U32 portIndex);
    virtual void onPortFlushCompleted(OMX_U32 portIndex);
    virtual void onPortEnableCompleted(OMX_U32 portIndex, bool enabled);
    virtual void onReset();

    virtual OMX_ERRORTYPE getExtensionIndex(
            const char *name, OMX_INDEXTYPE *index);

private:
    enum {
        kNumInputBuffers = 4,
        kNumOutputBuffers = 4,
    };
    ARenderer *mARenderer;

    tDdpDecoderExternal *mConfig;

    void *mDecoder;

    int64_t mAnchorTimeUs;
    int64_t mNumFramesOutput;
    int mLastChannelCount;
    int mLastSamplingRate;
    int mLastDRCmode;
    bool mPrepareOutputPortChange;
    bool mIsEC3;
    bool mIsJOC;
    bool mSignalledError;
    bool mStarted;
    bool mFadeIn;
    bool mCurJocDapOn;
    bool mLastJocDapOn;
    bool mIsJocOutput;
    bool mUpdateDDPSystemProperty;

    enum {
        NONE,
        AWAITING_DISABLED,
        AWAITING_ENABLED
    } mOutputPortSettingsChange;

    int64_t mLastMediaTimeUs;
    int64_t mLastAdjustedTimeUs;
    EndpConfigTable mEndpConfigTable;

    void init(const char *name);
    void initPorts();

    DISALLOW_EVIL_CONSTRUCTORS(DDPDecoder);
    void fadeInBuffer(void *data, int nrChans, int frameLengthSamplesPerChannel);
    int channelMaptoChannelCount(int chanMap);
    void setMaxPcmOutChannels();
    void configARenderer();
    void closeARenderer();
    void updateDDPSystemProperties();
    void setReconfigOnEndpChange(bool activateReconfig);
    bool isReconfigOnEndpChange();
};

}  // namespace android

#endif  /* DDP_DECODER_H */
