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

#ifndef DOLBY_DAP_BUFFER_ADAPTER_H_
#define DOLBY_DAP_BUFFER_ADAPTER_H_

#include "IDlbProcess.h"
#include "DapParamCache.h"
#include "DlbBufferProvider.h"

namespace dolby {

using namespace android;

class DapBufferAdapter : public IDlbProcess
{
public:
    DapBufferAdapter(IDlbProcess *dap);
    ~DapBufferAdapter();

    virtual status_t init();
    virtual void setEnabled(bool enabled);

    virtual status_t configure(int bufferSize, int sampleRate, audio_format_t format,
        audio_channel_mask_t inChannels, audio_channel_mask_t outChannels);

    virtual status_t getParam(DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t setParam(DapParameterId param, const dap_param_value_t* values, int length);

    virtual status_t process(BufferProvider *inBuffer, BufferProvider *outBuffer);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);

protected:
    status_t updateEnabled();

    IDlbProcess *mDap;
    DlbBufferProvider mInDlbBuf;
    BufferSink mInDlbSink;
    DlbBufferProvider mOutDlbBuf;
    BufferSource mOutDlbSource;
};

} // namespace dolby
#endif//DOLBY_DAP_BUFFER_ADAPTER_H_
