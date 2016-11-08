/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
#ifndef DOLBY_CROSSFADE_PROCESS_H_
#define DOLBY_CROSSFADE_PROCESS_H_

#include "IDlbProcess.h"
#include "DlbBufferProvider.h"

namespace dolby {

using namespace android;

class SampleCounter
{
public:
    SampleCounter(int numSamples)
    { mCount = 0; mTarget = numSamples; }

    void reset()
    { mCount = 0; }

    void preset()
    { mCount = mTarget; }

    float ratio() const
    { return ((float)mCount) / ((float)mTarget); };

    bool atStart() const
    { return mCount <= 0; }

    bool atEnd() const
    { return mCount >= mTarget; }

    void increment(int numSamples)
    { mCount = min(mTarget, mCount + numSamples); }

    void decrement(int numSamples)
    { mCount = max(0, mCount - numSamples); }

protected:
    int mTarget;
    int mCount;
};

class CrossfadeProcess : public IDlbProcess
{
public:
    enum CrossfadeState
    {
        PREROLL_ACTIVE,
        FADE_TO_ACTIVE,
        DAP_ACTIVE,
        FADE_TO_BYPASS,
        DAP_BYPASSED,
    };

    CrossfadeProcess(IDlbProcess *dap);
    virtual ~CrossfadeProcess();

    virtual status_t init();
    virtual void setEnabled(bool enable);

    virtual status_t configure(int bufferSize, int sampleRate, audio_format_t format,
        audio_channel_mask_t inChannels, audio_channel_mask_t outChannels);

    virtual status_t getParam(DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t setParam(DapParameterId param, const dap_param_value_t* values, int length);

    virtual status_t process(BufferProvider *inBuffer, BufferProvider *outBuffer);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);
    virtual status_t deviceChanged();

protected:
    void updateDapEnabled();
    status_t updateState(int numSamples, status_t processStaus);
    void crossfade(BufferProvider *inBuffer, BufferProvider *outBuffer, bool increment);
    void accumulate(int16_t *out_buf, int16_t *in_buf, int16_t *dap_buf, int num_samples, float in_coeff, float dap_coeff);
    void accumulate(int32_t *out_buf, int32_t *in_buf, int32_t *dap_buf, int num_samples, float in_coeff, float dap_coeff);

protected:
    IDlbProcess *mDap;
    CrossfadeState mState;
    SampleCounter mCrossfadeCounter;
    SampleCounter mDapPrerollCounter;
    DlbBufferProvider mCrossfadeBuffer;
};

} // namespace dolby
#endif//DOLBY_CROSSFADE_PROCESS_H_
