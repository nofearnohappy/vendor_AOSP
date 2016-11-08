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

#define LOG_TAG "DlbQdspAlsaDriver"

#include "DlbLog.h"
#include <stdlib.h>
#include <platform.h>
#include <tinyalsa/asoundlib.h>
#include "QdspAlsaDriver.h"
#include "ProfileParamParser.h"

#define min(x, y) (((x) < (y)) ? (x) : (y))

namespace dolby {

// Note: Keep the control name definitions consistent with kcontrol.
static const char* kSetParamControl = "DS1 DAP Set Param"; // set parameter ALSA mixer control name.
static const char* kGetParamControl = "DS1 DAP Get Param"; // get parameter ALSA mixer control name.
static const char* kGetVisualizerControl = "DS1 DAP Get Visualizer"; // get visualizer data ALSA mixer control name.

using namespace android;

static inline bool isParamExcluded(int paramId)
{
    return (paramId == QDSP_DAP_PARAM_DVLI ||
            paramId == QDSP_DAP_PARAM_DVLO ||
            paramId == QDSP_DAP_PARAM_VMB ||
            paramId == QDSP_DAP_PARAM_ENDP);
}

QdspDriver::QdspDriver()
{
    ALOGI("%s()", __FUNCTION__);
    mMixer = NULL;
    mMixerSetParamCtl = NULL;
    mMixerGetParamCtl = NULL;
    mMixerGetVisualizerCtl = NULL;
}

QdspDriver::~QdspDriver()
{
    ALOGI("%s()", __FUNCTION__);
    close();
}

status_t QdspDriver::init()
{
    ALOGI("%s()", __FUNCTION__);
    return open();
}

status_t QdspDriver::setEnabled(bool enable)
{
    ALOGD("%s(): enable %d", __FUNCTION__, enable);
    for (DapParamCache::Iterator iter = (enable ? mActiveProfile.getIterator() : mOffProfile.getIterator());
        !iter.finished();
        iter.next())
    {
        QdspParameterId qparam = qdspParamIdForParam(iter.param());
        if (!isParamExcluded(qparam))
            applyParam(AUDIO_DEVICE_OUT_DEFAULT, qparam, iter.values()->data(), iter.values()->length());
        else
            ALOGV("%s(): paramId %d is skipped", __FUNCTION__, qparam);
    }
    commitChangedParams(AUDIO_DEVICE_OUT_DEFAULT);
    return NO_ERROR;
}

status_t QdspDriver::setParam(audio_devices_t device, QdspParameterId param, const dap_param_value_t* values, int length)
{
    ALOGD("%s(): device 0x%8X, QDSP param 0x%5X, length %i", __FUNCTION__, device, param, length);
    // This interface does not support setting parameters for multiple output devices
    if (device != AUDIO_DEVICE_OUT_DEFAULT)
    {
        ALOGV("%s(): Unsupported device, the parameter is ignored", __FUNCTION__);
        return NO_ERROR;
    }
    if (isParamExcluded(param))
    {
        ALOGV("%s(): paramId %d is skipped", __FUNCTION__, param);
        return NO_ERROR;
    }
    // Update the active profile parameter cache.
    DapParameterId dapParam = paramIdForQdspId(param);
    mActiveProfile.set(dapParam, values, length);
    ALOGV("%s(): The active profile parameter cache received parameter %s", __FUNCTION__,
        dapParamNameValue(dapParam, values, length).string());
    return applyParam(device, param, values, length);
}

status_t QdspDriver::getParam(audio_devices_t device, QdspParameterId param, dap_param_value_t* values, int* length)
{
    ALOGVV("%s()", __FUNCTION__);

    int data[QDSP_PARAM_TOTAL_LEN];
    int offset = 0;
    int len = *length;
    data[QDSP_PARAM_HEADER_DEVICE_IDX] = static_cast<int>(device);
    data[QDSP_PARAM_HEADER_PARAM_IDX] = param;
    while (len > 0)
    {
        data[QDSP_PARAM_HEADER_OFFSET_IDX] = offset;
        int packet_length = min(len, QDSP_PARAM_PAYLOAD_LEN);
        data[QDSP_PARAM_HEADER_LENGTH_IDX] = packet_length;
        int count = QDSP_PARAM_HEADER_LEN + packet_length;

        status_t status = setGetParamCtl(data, count);
        if (status != NO_ERROR)
        {
            return status;
        }

        status = getGetParamCtl(data, count);
        if (status != NO_ERROR)
        {
            return status;
        }

        for (int i = 0; i < packet_length; ++i)
        {
            values[offset + i] = static_cast<dap_param_value_t>(data[QDSP_PARAM_VALUES_START_IDX + i]);
        }

        len -= packet_length;
        offset += packet_length;
    }
    *length = offset;
    return NO_ERROR;
}

status_t QdspDriver::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);

    int visualizer[QDSP_VISUALIZER_TOTAL_LEN];
    status_t status = getGetVisualizerCtl(visualizer, QDSP_VISUALIZER_TOTAL_LEN);

    int length = visualizer[QDSP_VISUALIZER_LENGTH_IDX];
    if (length > (2 * (*bands)))
    {
        ALOGE("%s() Expected %d bands received %d", __FUNCTION__, *bands, length/2);
        return BAD_VALUE;
    }

    for (int i = 0; i < length; ++i)
    {
        data[i] = static_cast<dap_param_value_t>(visualizer[QDSP_VISUALIZER_VALUES_IDX + i]);
    }

    return NO_ERROR;
}

status_t QdspDriver::commitChangedParams(audio_devices_t device)
{
    ALOGD("%s()", __FUNCTION__);
    int data[] = { static_cast<int>(device), DS1_USR_COMMIT_TO_DSP, 0, 0 };
    int count = sizeof(data) / sizeof(data[0]);
    return setSetParamCtl(data, count);
}

status_t QdspDriver::commitAllParams(audio_devices_t device)
{
    ALOGD("%s()", __FUNCTION__);
    int data[] = { static_cast<int>(device), DS1_USR_COMMIT_ALL_TO_DSP, 0, 0 };
    int count = sizeof(data) / sizeof(data[0]);
    return setSetParamCtl(data, count);
}

///
/// Called to open an ALSA mixer control.
///
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::open()
{
    ALOGI("%s()", __FUNCTION__);

    mMixer = mixer_open(MIXER_CARD);
    if (mMixer == NULL)
    {
        ALOGE("Fail to open ALSA mixer!");
        return BAD_VALUE;
    }

    mMixerSetParamCtl = mixer_get_ctl_by_name(mMixer, kSetParamControl);
    mMixerGetParamCtl = mixer_get_ctl_by_name(mMixer, kGetParamControl);
    mMixerGetVisualizerCtl = mixer_get_ctl_by_name(mMixer, kGetVisualizerControl);
    if (mMixerSetParamCtl == NULL || mMixerGetParamCtl == NULL || mMixerGetVisualizerCtl == NULL)
    {
        ALOGE("Fail to open ALSA mixer control!");
        return BAD_VALUE;
    }
    else
    {
        ALOGD("Opened %s as %p, %s as %p and %s as %p",
            kSetParamControl, mMixerSetParamCtl,
            kGetParamControl, mMixerGetParamCtl,
            kGetVisualizerControl, mMixerGetVisualizerCtl);
    }
    return NO_ERROR;
}

///
/// Called to close an ALSA mixer control.
///
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::close()
{
    ALOGI("%s()", __FUNCTION__);

    if (mMixer != NULL)
    {
        mixer_close(mMixer);
        mMixer = NULL;
        ALOGD("Closed %s, %s and %s", kSetParamControl, kGetParamControl, kGetVisualizerControl);
    }
    mMixerSetParamCtl = NULL;
    mMixerGetParamCtl = NULL;
    mMixerGetVisualizerCtl = NULL;
    return NO_ERROR;
}

/// @internal
/// Convert an array of integers to string for logging.
/// This functions assumes that combined length of string
/// representations of maximum index and value is less than 10.
///
/// @param data Pointer to array of integers
/// @param length Number of elements in array pointed by data
/// @return String containing all values in the input.
///
static char *valuesToString(int *data, int length)
{
    // If no data is specified then return "-"
    if (data == NULL || length == 0)
    {
        char *no_data = new char[2];
        no_data[0] = '-';
        no_data[1] = '\0';
        return no_data;
    }
    int capacity = length * 18;
    char *values = new char[capacity];
    char *curPos = values;
    int i;
    for (i = 0; (capacity > 0) && (i < length - 1); i++)
    {
        int bytesWritten = snprintf(curPos, capacity, "Value[%d]=%d, ", i, data[i]);
        curPos += bytesWritten;
        capacity -= bytesWritten;
    }
    snprintf(curPos, capacity, "Value[%d]=%d", i, data[i]);
    return values;
}

///
/// Apply the parameter by sending its values to QDSP via the ALSA mixer control.
///
/// @param device The device ID.
/// @param param  The QDSP ID for a DAP parameter.
/// @param values The parameter values.
/// @param length The number of values to be applied.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::applyParam(audio_devices_t device, QdspParameterId param, const dap_param_value_t* values, int length)
{
    int data[QDSP_PARAM_TOTAL_LEN];
    int offset = 0;

    data[QDSP_PARAM_HEADER_DEVICE_IDX] = static_cast<int>(device);
    data[QDSP_PARAM_HEADER_PARAM_IDX] = param;

    while (length > 0)
    {
        data[QDSP_PARAM_HEADER_OFFSET_IDX] = offset;
        int packet_length = min(length, QDSP_PARAM_PAYLOAD_LEN);
        data[QDSP_PARAM_HEADER_LENGTH_IDX] = packet_length;
        for (int i = 0; i < packet_length; ++i)
        {
            data[QDSP_PARAM_VALUES_START_IDX + i] = values[offset + i];
        }

        int count = QDSP_PARAM_HEADER_LEN + packet_length;
        status_t status = setSetParamCtl(data, count);
        if (status != NO_ERROR)
        {
            return status;
        }
        length -= packet_length;
        offset += packet_length;
    }
    return NO_ERROR;
}

///
/// Set multiple values on set parameter ALSA mixer control.
///
/// @param data Array of integer values.
/// @param count The number of values to be set.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::setSetParamCtl(int *data, int count)
{
    if (mMixerSetParamCtl == NULL || data == NULL)
    {
        return BAD_VALUE;
    }

    IF_ALOGV()
    {
        int paramId = data[QDSP_PARAM_HEADER_PARAM_IDX];

        if (paramId == DS1_USR_COMMIT_TO_DSP)
        {
            ALOGV("%s: mixer_ctl_set_array<DeviceID=%d, ParamID=DS1_USR_COMMIT_TO_DSP, Offset=%d, Length=%d>",
                kSetParamControl, data[QDSP_PARAM_HEADER_DEVICE_IDX],
                data[QDSP_PARAM_HEADER_OFFSET_IDX], data[QDSP_PARAM_HEADER_LENGTH_IDX]);
        }
        else if (paramId == DS1_USR_COMMIT_ALL_TO_DSP)
        {
            ALOGV("%s: mixer_ctl_set_array<DeviceID=%d, ParamID=DS1_USR_COMMIT_ALL_TO_DSP, Offset=%d, Length=%d>",
                kSetParamControl, data[QDSP_PARAM_HEADER_DEVICE_IDX],
                data[QDSP_PARAM_HEADER_OFFSET_IDX], data[QDSP_PARAM_HEADER_LENGTH_IDX]);
        }
        else
        {
            char *values = valuesToString(data + 4, data[3]);
            ALOGV("%s: mixer_ctl_set_array<DeviceID=%d, ParamID=%d, Offset=%d, Length=%d, %s>",
                kSetParamControl, data[QDSP_PARAM_HEADER_DEVICE_IDX], paramId,
                data[QDSP_PARAM_HEADER_OFFSET_IDX], data[QDSP_PARAM_HEADER_LENGTH_IDX], values);
            delete[] values;
        }
    }

    int ret_val = mixer_ctl_set_array(mMixerSetParamCtl, data, count);
    if (ret_val < 0)
    {
        ALOGE("%s() Call to mixer_ctl_set_array() failed with error %d", __FUNCTION__, ret_val);
        return BAD_VALUE;
    }
    return NO_ERROR;
}

///
/// Set multiple values on get parameter ALSA mixer control.
///
/// @param data Array of integer values.
/// @param count The number of values to be set.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::setGetParamCtl(int *data, int count)
{
    if (mMixerGetParamCtl == NULL || data == NULL)
    {
        return BAD_VALUE;
    }

    ALOGD("%s: mixer_ctl_set_array<DeviceID=%d, ParamID=%d, Offset=%d, Length=%d>",
        kGetParamControl, data[QDSP_PARAM_HEADER_DEVICE_IDX], data[QDSP_PARAM_HEADER_PARAM_IDX],
        data[QDSP_PARAM_HEADER_OFFSET_IDX], data[QDSP_PARAM_HEADER_LENGTH_IDX]);

    int ret_val = mixer_ctl_set_array(mMixerGetParamCtl, data, count);
    if (ret_val < 0)
    {
        ALOGE("%s() Call to mixer_ctl_set_array() failed with error %d", __FUNCTION__, ret_val);
        return BAD_VALUE;
    }
    return NO_ERROR;
}

///
/// Get multiple values from get parameter ALSA mixer control.
///
/// @param data Array of integers to store retrived values.
/// @param count The number of values to retrive.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::getGetParamCtl(int *data, int count)
{
    ALOGVV("%s()", __FUNCTION__);
    if (mMixerGetParamCtl == NULL || data == NULL)
    {
        return BAD_VALUE;
    }

    int ret_val = mixer_ctl_get_array(mMixerGetParamCtl, data, count);
    if (ret_val < 0)
    {
        ALOGE("%s() Call to mixer_ctl_get_array() failed with error %d", __FUNCTION__, ret_val);
        return BAD_VALUE;
    }

    IF_ALOGVV()
    {
        char *values = valuesToString(data + QDSP_PARAM_VALUES_START_IDX, data[QDSP_PARAM_HEADER_LENGTH_IDX]);
        ALOGVV("%s: mixer_ctl_get_array<DeviceID=%d, ParamID=%d, Offset=%d, Length=%d, %s>",
            kSetParamControl, data[QDSP_PARAM_HEADER_DEVICE_IDX], data[QDSP_PARAM_HEADER_PARAM_IDX],
            data[QDSP_PARAM_HEADER_OFFSET_IDX], data[QDSP_PARAM_HEADER_LENGTH_IDX], values);
        delete[] values;
    }

    return NO_ERROR;
}

///
/// Get multiple values from get visualizer ALSA mixer control.
///
/// @param data Array of integers to store retrived values.
/// @param count The number of values to retrive.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::getGetVisualizerCtl(int *data, int count)
{
    ALOGVV("%s()", __FUNCTION__);
    if (mMixerGetVisualizerCtl == NULL || data == NULL)
    {
        return BAD_VALUE;
    }

    int ret_val = mixer_ctl_get_array(mMixerGetVisualizerCtl, data, count);
    if (ret_val < 0)
    {
        ALOGE("%s() Call to mixer_ctl_get_array() failed with error %d", __FUNCTION__, ret_val);
        return BAD_VALUE;
    }

    IF_ALOGVV()
    {
        int length = data[QDSP_VISUALIZER_LENGTH_IDX];
        int virt_data_len = length / 2;

        char *vcbg_values = valuesToString(data + QDSP_VISUALIZER_VALUES_IDX, virt_data_len);
        char *vcbe_values = valuesToString(data + QDSP_VISUALIZER_VALUES_IDX + virt_data_len, virt_data_len);

        ALOGVV("%s: mixer_ctl_get_array<Length=%d, VCBG(%s), VCBE(%s)>",
            kGetVisualizerControl, length, vcbg_values, vcbe_values);

        delete[] vcbg_values, vcbe_values;
    }

    return NO_ERROR;
}

///
/// Set up the parameters cache for off profile.
///
/// @param ppp The profile parameter parser instance.
/// @return NO_ERROR on success, and BAD_VALUE otherwise.
///
status_t QdspDriver::defineOffProfile(ProfileParamParser ppp)
{
    ALOGD("%s()", __FUNCTION__);
    // Create a new parameter cache for profile or use existing cache.
    ppp.begin();
    ALOGD("%s(profile=%d, nParams=%d)", __FUNCTION__, ppp.profileId(), ppp.numberOfParams());

    // Add all parameters to off profile parameter cache
    while (ppp.next())
    {
        ALOGD("%s() Received parameter %s", __FUNCTION__,
            dapParamNameValue(ppp.paramId(), ppp.values(), ppp.length()).string());
        mOffProfile.set(ppp.paramId(), ppp.values(), ppp.length());
    }
    ALOGI("%s() Added %d parameters for off profile.", __FUNCTION__, ppp.numberOfParams());
    return NO_ERROR;
}
};  // namespace dolby
