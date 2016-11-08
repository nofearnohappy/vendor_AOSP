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
#define LOG_TAG "DlbEndpointParamCache"

#include "DlbLog.h"
#include "EndpointParamCache.h"

namespace dolby {

using namespace android;

// @@DOLBY_DAP2_BACKWARD_COMPATIBLE
#ifdef DOLBY_DAP2_BACKWARD_COMPATIBLE

#define DAP2_NUM_CHANNELS_MAX          (10)
#define DAP2_NUM_BANDS_MAX             (20)

#define DAP1_PLMD_DISABLE_ALL          (0)
#define DAP1_PLMD_PEAK_ONLY            (1)
#define DAP1_PLMD_REGULATED_PEAK       (2)
#define DAP1_PLMD_REGULATED_DISTORTION (3)
#define DAP1_PLMD_AUTO                 (4)

static inline bool isHeadphone(audio_devices_t device)
{
    return (device == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
            device == AUDIO_DEVICE_OUT_WIRED_HEADPHONE ||
            device == AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES ||
            device == AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET ||
            device == AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET);
}

static inline bool isSpeaker(audio_devices_t device)
{
    return (device == AUDIO_DEVICE_OUT_SPEAKER);
}
#endif
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE_END

EndpointParamCache::EndpointParamCache(IDlbProcess *dap) : mDap(dap)
{
    ALOGD("%s()", __FUNCTION__);
    mCurrentDevice = AUDIO_DEVICE_NONE;
}

EndpointParamCache::~EndpointParamCache()
{
    ALOGD("%s()", __FUNCTION__);
    size_t size = mCache.size();
    for (size_t i = 0; i < size; ++i)
    {
        delete mCache.valueAt(i);
    }
}

status_t EndpointParamCache::init()
{
    ALOGV("%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t EndpointParamCache::setDevice(audio_devices_t device)
{
    ALOGD("%s(device=0x%08x)", __FUNCTION__, device);
    mCurrentDevice = device;
    status_t status = NO_ERROR;
    DapParamCache *cache = getCache(device);
    if (cache != NULL)
    {
        ALOGD("%s() Applying parameters for new device 0x%08x", __FUNCTION__, device);
        status = doCommit(cache, false);
    }
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE
#ifdef DOLBY_DAP2_BACKWARD_COMPATIBLE
    cache = getCache(AUDIO_DEVICE_OUT_DEFAULT);
    if (cache != NULL)
    {
        commitDap1Param(DAP1_PARAM_VDHE, cache);
        commitDap1Param(DAP1_PARAM_VSPE, cache);
        commitDap1Param(DAP1_PARAM_DHSB, cache);
        commitDap1Param(DAP1_PARAM_DSSB, cache);
        commitDap1Param(DAP1_PARAM_PLMD, cache);
    }
#endif
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE_END
    return status;
}

DapParamCache *EndpointParamCache::getCache(audio_devices_t device)
{
    ALOGVV("%s()", __FUNCTION__);
    int idx = mCache.indexOfKey(device);
    if (idx < 0)
    {
        return NULL;
    }
    return mCache.valueAt(idx);
}

status_t EndpointParamCache::setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s()", __FUNCTION__);
    DapParamCache *params = getCache(device);
    if (params == NULL)
    {
        ALOGD("%s() Creating parameter cache for device 0x%08x", __FUNCTION__, device);
        params = new DapParamCache;
        mCache.add(device, params);
    }
    return params->set(param, values, length);
}

status_t EndpointParamCache::getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGV("%s()", __FUNCTION__);
    if (device == mCurrentDevice || device == AUDIO_DEVICE_OUT_DEFAULT)
    {
        return mDap->getParam(param, values, length);
    }
    else
    {
        DapParamCache *params = getCache(device);
        if (params == NULL)
        {
            ALOGE("%s() No cache defined for device 0x%08x", __FUNCTION__, device);
            return NO_INIT;
        }
        return params->get(param, values, length);
    }
}

status_t EndpointParamCache::commit(audio_devices_t device, bool modifiedOnly)
{
    ALOGV("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    // Only commit parameters if device is either current device or all devices
    if (device == mCurrentDevice || device == AUDIO_DEVICE_OUT_DEFAULT)
    {
        DapParamCache *params = getCache(device);
        if (params == NULL)
        {
            return NO_INIT;
        }
        ALOGD("%s() Applying parameters for device 0x%08x", __FUNCTION__, device);
        status = doCommit(params, modifiedOnly);

        // Make sure that overridden parameters for current device are always applied to DAP
        if (status == NO_ERROR && device == AUDIO_DEVICE_OUT_DEFAULT)
        {
            params = getCache(mCurrentDevice);
            if (params != NULL)
            {
                ALOGD("%s() Overriding parameters for current device 0x%08x", __FUNCTION__, mCurrentDevice);
                status = doCommit(params, false);
            }
        }
    }
    return status;
}

status_t EndpointParamCache::doCommit(DapParamCache *params, bool modifiedOnly)
{
    ALOGV("%s()", __FUNCTION__);
    status_t status = NO_ERROR;
    int paramsCommitted = 0;
    for (DapParamCache::Iterator iter = params->getIterator(); !iter.finished(); iter.next())
    {
        if (modifiedOnly && !iter.isModified())
        {
            continue;
        }
        ALOGV("%s() committing modified=%d %s", __FUNCTION__, iter.isModified(),
            dapParamNameValue(iter.param(), iter.values()->data(), iter.values()->length()).string());
        iter.clearModified();
        status = mDap->setParam(iter.param(), iter.values()->data(), iter.values()->length());
        if (status != NO_ERROR)
        {
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE
#ifdef DOLBY_DAP2_BACKWARD_COMPATIBLE
            // On an error, DAP1 specific parameters still get a chance to be mapped
            // to DAP2 parameter and apply.
            status = commitDap1Param(iter.param(), params);
            if (status != NO_ERROR)
            {
                break;
            }
#else
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE_END
            break;
#endif // @@DOLBY_DAP2_BACKWARD_COMPATIBLE_LINE
        }
        ++paramsCommitted;
    }
    ALOGD("%s() Committed %d parameters out of %d.", __FUNCTION__, paramsCommitted, params->size());
    return status;
}

// @@DOLBY_DAP2_BACKWARD_COMPATIBLE
#ifdef DOLBY_DAP2_BACKWARD_COMPATIBLE
status_t EndpointParamCache::commitDap1Param(DapParameterId param, DapParamCache *params)
{
    ALOGV("%s(param=%s)", __FUNCTION__, dapParamName(param).string());
    status_t status = NO_ERROR;
    switch (param)
    {
        case DAP1_PARAM_IEBT:
        {
            int length = 1;
            const int nValues = DAP2_NUM_BANDS_MAX * 2 + 1;
            dap_param_value_t nBands;
            dap_param_value_t values[nValues];
            status = params->get(DAP1_PARAM_IENB, &nBands, &length);
            ALOGE_IF(status != NO_ERROR, "ienb NOT found in the cache");
            values[0] = length = nBands;
            if (status == NO_ERROR)
            {
                status = params->get(DAP1_PARAM_IEBF, &values[1], &length);
                ALOGE_IF(status != NO_ERROR, "iebf NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                status = params->get(param, &values[1 + length], &length);
                ALOGE_IF(status != NO_ERROR, "iebt NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                length = length * 2 + 1;
                status = mDap->setParam(DAP2_PARAM_IEBS, values, length);
            }
            break;
        }
        case DAP1_PARAM_GEBG:
        {
            int length = 1;
            const int nValues = DAP2_NUM_BANDS_MAX * 2 + 1;
            dap_param_value_t nBands;
            dap_param_value_t values[nValues];
            status = params->get(DAP1_PARAM_GENB, &nBands, &length);
            ALOGE_IF(status != NO_ERROR, "genb NOT found in the cache");
            values[0] = length = nBands;
            if (status == NO_ERROR)
            {
                status = params->get(DAP1_PARAM_GEBF, &values[1], &length);
                ALOGE_IF(status != NO_ERROR, "gebf NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                status = params->get(param, &values[1 + length], &length);
                ALOGE_IF(status != NO_ERROR, "gebg NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                length = length * 2 + 1;
                status = mDap->setParam(DAP2_PARAM_GEBS, values, length);
            }
            break;
        }
        case DAP1_PARAM_VDHE:
        case DAP1_PARAM_DHSB:
        {
            int length = 1;
            dap_param_value_t value;
            DapParameterId dap2Param = (param == DAP1_PARAM_VDHE) ? DAP2_PARAM_VTON : DAP2_PARAM_DSB;
            if (isHeadphone(mCurrentDevice))
            {
                status = params->get(param, &value, &length);
                ALOGE_IF(status != NO_ERROR, "%s NOT found in the cache", dapParamName(param).string());
                status = (status == NO_ERROR) ? mDap->setParam(dap2Param, &value, length) : status;
            }
            else if (isSpeaker(mCurrentDevice))
            {
                ALOGV("param %s is skipped as the current device is speaker",
                      dapParamName(param).string());
            }
            else
            {
                value = 0;
                status = mDap->setParam(dap2Param, &value, length);
            }
            break;
        }
        case DAP1_PARAM_VSPE:
        case DAP1_PARAM_DSSB:
        {
            int length = 1;
            dap_param_value_t value;
            DapParameterId dap2Param = (param == DAP1_PARAM_VSPE) ? DAP2_PARAM_VTON : DAP2_PARAM_DSB;
            if (isSpeaker(mCurrentDevice))
            {
                status = params->get(param, &value, &length);
                ALOGE_IF(status != NO_ERROR, "%s NOT found in the cache", dapParamName(param).string());
                status = (status == NO_ERROR) ? mDap->setParam(dap2Param, &value, length) : status;
            }
            else if (isHeadphone(mCurrentDevice))
            {
                ALOGV("param %s is skipped as the current device is headphone",
                      dapParamName(param).string());
            }
            else
            {
                value = 0;
                status = mDap->setParam(dap2Param, &value, length);
            }
            break;
        }
        case DAP1_PARAM_ARBI:
        {
            int length = 1;
            dap_param_value_t nBands;
            const int nValues = DAP2_NUM_BANDS_MAX * 4 + 1;
            dap_param_value_t values[nValues];
            status = params->get(DAP1_PARAM_ARNB, &nBands, &length);
            ALOGE_IF(status != NO_ERROR, "arnb NOT found in the cache");
            values[0] = length = nBands;
            if (status == NO_ERROR)
            {
                status = params->get(DAP1_PARAM_ARBF, &values[1], &length);
                ALOGE_IF(status != NO_ERROR, "arbf NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                status = params->get(DAP1_PARAM_ARBL, &values[1 + length], &length);
                ALOGE_IF(status != NO_ERROR, "arbl NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                status = params->get(DAP1_PARAM_ARBH, &values[1 + length * 2], &length);
                ALOGE_IF(status != NO_ERROR, "arbh NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                status = params->get(param, &values[1 + length * 3], &length);
                ALOGE_IF(status != NO_ERROR, "arbi NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                length = length * 4 + 1;
                status = mDap->setParam(DAP2_PARAM_ARBS, values, length);
            }
            break;
        }
        case DAP1_PARAM_AOBG:
        {
            int length = 1;
            const int nValues = DAP2_NUM_BANDS_MAX * (DAP2_NUM_CHANNELS_MAX + 1) + 1;
            dap_param_value_t nBands = 0, nChannels = 0;
            dap_param_value_t values[nValues] = {0};
            status = params->get(DAP1_PARAM_AONB, &nBands, &length);
            ALOGE_IF(status != NO_ERROR, "aonb NOT found in the cache");
            values[0] = nBands;
            if (status == NO_ERROR)
            {
                length = nBands;
                status = params->get(DAP1_PARAM_AOBF, &values[1], &length);
                ALOGE_IF(status != NO_ERROR, "aobf NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                length = 1;
                status = params->get(DAP1_PARAM_AOCC, &nChannels, &length);
                ALOGE_IF(status != NO_ERROR, "aocc NOT found in the cache");
            }
            if (status == NO_ERROR)
            {
                length = nChannels * (nBands + 1);
                dap_param_value_t aobg[length];
                status = params->get(param, aobg, &length);
                ALOGE_IF(status != NO_ERROR, "aobg NOT found in the cache");
                for (int i = 0; i < nChannels; i++)
                {
                    memcpy(&values[nBands * (i + 1) + 1], &aobg[i * (nBands + 1) + 1],
                           nBands * sizeof(dap_param_value_t));
                }
            }
            if (status == NO_ERROR)
            {
                length = nBands * (DAP2_NUM_CHANNELS_MAX + 1) + 1;
                status = mDap->setParam(DAP2_PARAM_AOBS, values, length);
            }
            break;
        }
        case DAP1_PARAM_PLMD:
        {
            int length = 1;
            dap_param_value_t plmd, value;
            status = params->get(param, &plmd, &length);
            ALOGE_IF(status != NO_ERROR, "plmd NOT found in the cache");
            if (status != NO_ERROR)
            {
                break;
            }
            if (plmd == DAP1_PLMD_PEAK_ONLY)
            {
                value = 0;
                status = mDap->setParam(DAP2_PARAM_ARON, &value, length);
            }
            else if (plmd == DAP1_PLMD_REGULATED_PEAK)
            {
                value = 1;
                status = mDap->setParam(DAP2_PARAM_ARON, &value, length);
                value = 0;
                status = mDap->setParam(DAP2_PARAM_ARDE, &value, length);
            }
            else if (plmd == DAP1_PLMD_REGULATED_DISTORTION)
            {
                value = 1;
                status = mDap->setParam(DAP2_PARAM_ARON, &value, length);
                value = 1;
                status = mDap->setParam(DAP2_PARAM_ARDE, &value, length);
            }
            else if (plmd == DAP1_PLMD_AUTO)
            {
                value = 1;
                status = mDap->setParam(DAP2_PARAM_ARON, &value, length);
                value = isSpeaker(mCurrentDevice) ? 1 : 0;
                status = mDap->setParam(DAP2_PARAM_ARDE, &value, length);
            }
            else
            {
                value = 0;
                status = mDap->setParam(DAP2_PARAM_ARON, &value, length);
                value = 0;
                status = mDap->setParam(DAP2_PARAM_ARDE, &value, length);
            }
            break;
        }
        // The following DAP1 parameters will be bundled and applied along with other DAP1 parameters
        case DAP1_PARAM_IENB:
        case DAP1_PARAM_IEBF:
        case DAP1_PARAM_GENB:
        case DAP1_PARAM_GEBF:
        case DAP1_PARAM_AONB:
        case DAP1_PARAM_AOBF:
        case DAP1_PARAM_AOCC:
        case DAP1_PARAM_ARNB:
        case DAP1_PARAM_ARBF:
        case DAP1_PARAM_ARBL:
        case DAP1_PARAM_ARBH:
            ALOGV("param %s is skipped since it gets applied along with the other parameters",
                  dapParamName(param).string());
            break;
        // The following DAP1 parameters are removed from DAP2, we ignore them without an error
        case DAP_PARAM_DHRG:
        case DAP_PARAM_DSSF:
        case DAP1_PARAM_VMON:
        case DAP1_PARAM_ENDP:
        case DAP1_PARAM_SCPE:
        case DAP1_PARAM_VEN:
        case DAP1_PARAM_OCF:
            ALOGV("param %s is skipped since it is removed from DAP2", dapParamName(param).string());
            break;
        default:
            status = BAD_VALUE;
            ALOGE("%s: Un-recognized param %s", __FUNCTION__, dapParamName(param).string());
            break;
    }

    return status;
}
#endif
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE_END

status_t EndpointParamCache::commitChangedParams(audio_devices_t device)
{
    ALOGD("%s()", __FUNCTION__);
    return commit(device, true);
}

status_t EndpointParamCache::commitAllParams(audio_devices_t device)
{
    ALOGD("%s()", __FUNCTION__);
    return commit(device, false);
}

} // namespace dolby
