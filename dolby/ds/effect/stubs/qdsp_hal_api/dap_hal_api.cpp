/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "DlbQdspDapHal"

#include <utils/Log.h>
#include <utils/String8.h>
#include <system/audio.h>
#include "dap_hal_api.h"

using namespace android;

static int mVisualizerPos = 0;

dap_handle_t dap_open()
{
    ALOGI("%s() => %p", __FUNCTION__, &mVisualizerPos);
    return &mVisualizerPos;
}

void dap_close(dap_handle_t handle)
{
    ALOGI("%s(handle=%p)", __FUNCTION__, handle);
    ALOGE_IF(handle != (dap_handle_t)&mVisualizerPos, "%s() called with invalid handle(%p).", __FUNCTION__, handle);
}

int dap_command(dap_handle_t handle, audio_devices_t device_id, dap_cmd_t command, int32_t data)
{
    ALOGI("%s(handle=%p, device=0x%08x, command=%d, data=%d)", __FUNCTION__, handle, device_id, command, data);
    ALOGE_IF(handle != (dap_handle_t)&mVisualizerPos, "%s() Called with invalid handle(%p).", __FUNCTION__, handle);
    return 0;
}

int dap_set_param(dap_handle_t handle, audio_devices_t device_id, int32_t param_id, const int32_t* data, int32_t length)
{
    String8 values;
    for (int i = 0; i < (length-1); ++i)
    {
        values.appendFormat("%hd, ", data[i]);
    }
    if (length > 0)
    {
        values.appendFormat("%hd", data[length-1]);
    }
    ALOGI("%s(handle=%p, device=0x%08x, param=0x%08x, value=#%d[%s])",
        __FUNCTION__, handle, device_id, param_id, length, values.string());
    ALOGE_IF(handle != (dap_handle_t)&mVisualizerPos, "%s() Called with invalid handle(%p).", __FUNCTION__, handle);
    return 0;
}

int dap_get_param(dap_handle_t handle, audio_devices_t device_id, int32_t param_id, int32_t* data, int32_t* length)
{
    ALOGI("%s(handle=%p, device=0x%08x, param=0x%08x, value=#%d)",
        __FUNCTION__, handle, device_id, param_id, *length);
    for (int i = 0; i < *length; ++i)
    {
        data[i] = i + 1;
    }
    ALOGE_IF(handle != (dap_handle_t)&mVisualizerPos, "%s() Called with invalid handle(%p).", __FUNCTION__, handle);
    return 0;
}

int dap_get_visualizer(dap_handle_t handle, int32_t* data, int32_t* length)
{
    ALOGV("%s(data=%p, length=%d)", __FUNCTION__, data, *length);
    if (handle != (dap_handle_t)&mVisualizerPos)
    {
        ALOGE("%s() Called with invalid handle(%p).", __FUNCTION__, handle);
        return -1;
    }
    // Pattern for smooth sine wave
    const int32_t kPattern[] = {
        100, 124, 147, 164, 176, 180, 176, 164, 147,
        124, 100, 75, 52, 35, 23, 20, 23, 35, 52, 75
    };

    const int kNumSamples = sizeof(kPattern) / sizeof(kPattern[0]);

    // Copy pattern into output array
    int bandlen = *length / 2;
    for (int i = 0, j = bandlen; i < bandlen; ++i, ++j)
    {
        data[i] = kPattern[(mVisualizerPos + i) % kNumSamples];
        data[j] = kPattern[(mVisualizerPos + i + kNumSamples/2) % kNumSamples];
    }

    // Shift pattern position for next cycle
    mVisualizerPos = (mVisualizerPos + 1) % kNumSamples;
    return 0;
}
