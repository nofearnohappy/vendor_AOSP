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
#ifndef DOLBY_DAP1_PROCESS_H_
#define DOLBY_DAP1_PROCESS_H_

extern "C" {
#include "ak.h"
#include "ak_name.h"
}
#include "IDlbProcess.h"

namespace dolby {

using namespace android;

class Dap1Process : public IDlbProcess
{
public:
    Dap1Process();
    virtual ~Dap1Process();

    virtual status_t init();
    virtual void setEnabled(bool enable) { }

    virtual status_t configure(int bufferSize, int sampleRate, audio_format_t format,
        audio_channel_mask_t inChannels, audio_channel_mask_t outChannels);

    virtual status_t getParam(DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t setParam(DapParameterId param, const dap_param_value_t* values, int length);

    virtual status_t process(BufferProvider *inBuffer, BufferProvider *outBuffer);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);

protected:
    int open();
    void close();

    int start();
    void stop();

    int loadLicense();
    int validateLicense();

    int get(AK_NAME name, dap_param_value_t *data, int *length);
    int set(AK_NAME name, const dap_param_value_t *data, int length);

    ak_instance *mAk;
    ak_memory_pool *mScratch;
    bool mHasVisualizerData;
};

} // namespace dolby

#endif//DOLBY_DAP1_PROCESS_H_
