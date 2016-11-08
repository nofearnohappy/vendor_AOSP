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

#define LOG_TAG "DlbQdspHalDriver"

#include "DlbLog.h"
#include "QdspHalDriver.h"

namespace dolby {

using namespace android;

QdspDriver::QdspDriver()
{
    ALOGD("%s()", __FUNCTION__);
    mHandle = NULL;
}

QdspDriver::~QdspDriver()
{
    ALOGD("%s()", __FUNCTION__);
    if (mHandle != NULL)
    {
        dap_close(mHandle);
    }
}

status_t QdspDriver::init()
{
    ALOGV("%s()", __FUNCTION__);
    mHandle = dap_open();
    ALOGD("%s() -> %p", __FUNCTION__, mHandle);
    if (mHandle == NULL)
    {
        return NO_INIT;
    }
    return dap_command(mHandle, AUDIO_DEVICE_OUT_DEFAULT, DAP_CMD_USE_CACHE_FOR_INIT, true);
}

status_t QdspDriver::setEnabled(bool enable)
{
    ALOGD("%s(enable=%d", __FUNCTION__, enable);
    return dap_command(mHandle, AUDIO_DEVICE_OUT_DEFAULT, DAP_CMD_SET_BYPASS, !enable);
}

status_t QdspDriver::setParam(audio_devices_t device, QdspParameterId param, const dap_param_value_t* values, int length)
{
    ALOGD("%s(device=0x%08x, param=0x%08x, values=%s)", __FUNCTION__, device, param,
        dapParamValue(values, length).string());
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    return dap_set_param(mHandle, device, param, values, length);
}

status_t QdspDriver::getParam(audio_devices_t device, QdspParameterId param, dap_param_value_t* values, int* length)
{
    ALOGD("%s(device=0x%08x, param=0x%08x, values=%p, length=%d)", __FUNCTION__, device, param, values, *length);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    int ret = dap_get_param(mHandle, device, param, values, length);
    if (ret < 0)
    {
        return ret;
    }
    return NO_ERROR;
}

status_t QdspDriver::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    int length = 2 * (*bands);
    int32_t buf[length];
    int ret = dap_get_visualizer(mHandle, buf, &length);
    if (ret < 0)
    {
        return ret;
    }
    *bands = length / 2;
    for (int i =0; i < length; ++i)
    {
        data[i] = static_cast<dap_param_value_t>(buf[i]);
    }
    return NO_ERROR;
}

status_t QdspDriver::commitChangedParams(audio_devices_t device)
{
    ALOGD("%s(device=%08x)", __FUNCTION__, device);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    return dap_command(mHandle, device, DAP_CMD_COMMIT_CHANGED, 0);
}

status_t QdspDriver::commitAllParams(audio_devices_t device)
{
    ALOGD("%s(device=%08x)", __FUNCTION__, device);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    return dap_command(mHandle, device, DAP_CMD_COMMIT_ALL, 0);
}

status_t QdspDriver::setDevice(audio_devices_t device)
{
    ALOGD("%s(device=%08x)", __FUNCTION__, device);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    return dap_command(mHandle, device, DAP_CMD_SET_ACTIVE_DEVICE, device);
}

///
/// Set up the parameters cache for off profile.
///
/// @param ppp The profile parameter parser instance.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::defineOffProfile(ProfileParamParser ppp)
{
    ALOGV("%s()", __FUNCTION__);
    LOG_FATAL_IF(mHandle == NULL, "%s() called without init().", __FUNCTION__);
    // Create a new parameter cache for profile or use existing cache.
    ppp.begin();
    ALOGD("%s(profile=%d, nParams=%d)", __FUNCTION__, ppp.profileId(), ppp.numberOfParams());

    // Add all parameters to off profile parameter cache
    while (ppp.next())
    {
        ALOGD("%s() Sending off profile parameter %s", __FUNCTION__,
            dapParamNameValue(ppp.paramId(), ppp.values(), ppp.length()).string());
        QdspParameterId qparam = qdspParamIdForParam(ppp.paramId());
        int ret = setParam(AUDIO_DEVICE_NONE, qparam, ppp.values(), ppp.length());
        if (ret < 0)
        {
            ALOGE("%s() Failed to set parameter %s (0x%08x)", __FUNCTION__,
                dapParamName(ppp.paramId()).string(), qparam);
            return BAD_VALUE;
        }
    }
    ALOGI("%s() Defined %d parameters for off profile.", __FUNCTION__, ppp.numberOfParams());
    return NO_ERROR;
}
};  // namespace dolby
