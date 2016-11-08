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
#ifndef DOLBY_I_DLB_PROCESS_H_
#define DOLBY_I_DLB_PROCESS_H_

#include <utils/Errors.h>
#include <system/audio.h>
#include <hardware/audio_effect.h>
#include "DapParams.h"
#include "DlbBufferProvider.h"

#define NUM_PCM_SAMPLES_PER_BLOCK (256)

namespace dolby {

using namespace android;

class IDlbProcess
{
public:
    virtual ~IDlbProcess() { };
    virtual status_t init() = 0;
    virtual void setEnabled(bool enable) = 0;
    virtual status_t configure(int bufferSize, int sampleRate, audio_format_t format,
        audio_channel_mask_t inChannels, audio_channel_mask_t outChannels) = 0;

    virtual status_t getParam(DapParameterId param, dap_param_value_t* values, int* length) = 0;
    virtual status_t setParam(DapParameterId param, const dap_param_value_t* values, int length) = 0;

    virtual status_t process(BufferProvider *inBuffer, BufferProvider *outBuffer) = 0;
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands) = 0;
    virtual status_t deviceChanged() { return NO_ERROR; };
};

} // namespace dolby

#endif//DOLBY_I_DLB_PROCESS_H_
