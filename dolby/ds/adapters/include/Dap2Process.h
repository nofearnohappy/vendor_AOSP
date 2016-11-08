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
#ifndef DOLBY_DAP2_PROCESS_H_
#define DOLBY_DAP2_PROCESS_H_

#include <utils/Vector.h>
extern "C" {
#include "dap_cpdp.h"
#include "oamdi/include/oamdi.h"
}
#include "IDlbProcess.h"

#define OAMD_MAX_NUM_OBJECTS  (16)
#define AOBS_MAX_NUM_TUNING_CHANNELS DAP_CPDP_MAX_NUM_OUTPUT_CHANNELS

namespace dolby {

using namespace android;

class Dap2Process : public IDlbProcess
{
public:
    Dap2Process();
    virtual ~Dap2Process();

    virtual status_t init();
    virtual void setEnabled(bool) { }

    virtual status_t configure(int bufferSize, int sampleRate, audio_format_t format,
        audio_channel_mask_t inChannels, audio_channel_mask_t outChannels);

    virtual status_t getParam(DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t setParam(DapParameterId param, const dap_param_value_t* values, int length);

    virtual status_t process(BufferProvider *inBuffer, BufferProvider *outBuffer);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);

protected:
    int open();
    void close();

    int loadLicense();
    int validateLicense();

    dap_cpdp_state*      mDapCpDpStatePtr;
    unsigned long        mSampleRate;
    void*                mPersistentMemoryPtr;
    void*                mScratchMemoryPtr;

    dap_cpdp_metadata    mMIMetadata;
    dap_cpdp_mix_data*   mMixDataPtr;

    int                  mProcessingMode;
    bool                 mVirtualizerOn;

    bool                 mHasVisualizerData;
    Vector<unsigned int> mVisBandCenterFreqs;
};

} // namespace dolby

#endif//DOLBY_DAP2_PROCESS_H_
