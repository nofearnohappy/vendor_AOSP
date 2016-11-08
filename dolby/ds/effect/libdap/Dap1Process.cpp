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
#define LOG_TAG "DlbDap1Process"
//#define DAP_PROFILING
//<<MTK_added
#include <stdlib.h>
#include <string.h>
//MTK_added>>

#include <inttypes.h>
#include <utils/SystemClock.h>
#include <utils/String8.h>
#include <hardware/audio_effect.h>
#include "DlbLog.h"
#include "Dap1Process.h"

#define DS1_OCF_STEREO 0
#define DS1_OCF_5DOT1 1
#define DS1_OCF_7DOT1 2

namespace dolby {

using namespace android;

Dap1Process::Dap1Process()
{
    ALOGI("%s()", __FUNCTION__);
    mAk = NULL;
    mScratch = NULL;
    mHasVisualizerData = false;
}

Dap1Process::~Dap1Process()
{
    ALOGI("%s()", __FUNCTION__);
    stop();
    close();
}

#ifdef DOLBY_DAP_LICENSE

static char *mLicense = NULL;
static int mLicenseSize = 0;

int Dap1Process::loadLicense()
{
    ALOGI("%s()", __FUNCTION__);
    // If license buffer has not been initialized, read the license.
    if (mLicense == NULL)
    {
        // Open license file
        const char *kLicenseFilePath = "/data/dolby/ds-ak.lic";
        FILE* fpLic = fopen("/data/dolby/ds-ak.lic", "rb");
        if (fpLic == NULL)
        {
            ALOGE("Unable to open license file %s", kLicenseFilePath);
            return AK_ERR_HANDLE;
        }

        // Get the size of file
        fseek(fpLic, 0, SEEK_END);
        mLicenseSize = static_cast<int>(ftell(fpLic));
        if (mLicenseSize <= 0)
        {
            ALOGE("Unable to get license file size.");
            fclose(fpLic);
            return AK_ERR_HANDLE;
        }
        fseek(fpLic, 0, SEEK_SET);

        // Allocate buffer and read the file contents
        mLicense = new char[mLicenseSize];
        int bytesRead = fread(mLicense, mLicenseSize, 1, fpLic);
        if (bytesRead != mLicenseSize)
        {
            ALOGE("Unable to read license file.");
            fclose(fpLic);
            return AK_ERR_MEMORY;
        }
        fclose(fpLic);
        ALOGI("%s() License file loaded");
    }
    int initHandle = ak_find(mAk, AK_ROOT, AK_N("init"));
    int lcszHandle = ak_find(mAk, initHandle, AK_N("lcsz"));
    int set_val = ak_set(mAk, lcszHandle, 0, mLicenseSize);
    if (set_val != mLicenseSize) {
        ALOGE("%s() License size is not valid!", __FUNCTION__);
        return AK_ERR_SIZE;
    }
    return AK_OK;
}

int Dap1Process::validateLicense()
{
    ALOGI("%s()", __FUNCTION__);
    if (mLicense != NULL)
    {
        int lcptHandle = ak_find(mAk, AK_ROOT, AK_N("lcpt"));
        return ak_set_bulk(mAk, lcptHandle, 0, mLicenseSize, AK_DATATYPE_CHAR, mLicense);
    }
    return AK_ERR_HANDLE;
}

#else

int Dap1Process::loadLicense()
{
    ALOGI("%s() no license check!", __FUNCTION__);
    return AK_OK;
}

int Dap1Process::validateLicense()
{
    ALOGI("%s() no license check!", __FUNCTION__);
    return AK_OK;
}

#endif

int Dap1Process::open()
{
    ALOGI("%s()", __FUNCTION__);
    mAk = static_cast<ak_instance*>(malloc(ak_size(NULL)));
    int status = ak_open(mAk);
    if (status != AK_OK)
    {
        ALOGE("Call to ak_open() failed with error %d", status);
        free(mAk);
        mAk = NULL;
    }
    return status;
}

void Dap1Process::close()
{
    ALOGI("%s()", __FUNCTION__);
    if (mAk != NULL)
    {
        ak_close(mAk);
        free(mAk);
        mAk = NULL;
    }
}

int Dap1Process::start()
{
    ALOGI("%s()", __FUNCTION__);
    // All init pools are allocated after the scratch pool.
    int numPools = ak_num_pools() + 1;

    mScratch = reinterpret_cast<ak_memory_pool*>(calloc(numPools, sizeof(ak_memory_pool)));
    if (mScratch == NULL)
    {
        ALOGE("Unable to allocate memory for pools");
        return AK_ERR_MEMORY;
    }

    // Get needs of the init pools.
    int status = ak_needs(mAk, AK_ROOT, mScratch + 1);
    if (status != AK_OK)
    {
        ALOGE("Call to ak_needs() failed with error %d", status);
        goto err_cleanup;
    }

    // Get needs of the scratch pool.
    status = ak_scratch(mAk, AK_ROOT, mScratch);
    if (status != AK_OK)
    {
        ALOGE("Call to ak_scratch() failed with error %d", status);
        goto err_cleanup;
    }

    // Allocate memory for each pool.
    for (int i = 0; i < numPools; ++i)
    {
        int alignment =  1 << mScratch[i].alignment;
        mScratch[i].pmem = memalign(alignment, mScratch[i].size);
        if (mScratch[i].pmem == NULL)
        {
            ALOGE("Unable to allocate memory for pool");
            status = AK_ERR_MEMORY;
            goto err_cleanup;
        }
        ALOGV("%s allocated memory %p with size %d and alignment %d for pool %d",
            __FUNCTION__, mScratch[i].pmem, mScratch[i].size, alignment, i);
    }

    // Start the AK instance.
    status = ak_start(mAk, mScratch + 1, NULL, NULL);
    if (status != AK_OK)
    {
        ALOGE("Call to ak_start() failed with error %d", status);
        goto err_cleanup;
    }
    return status;

err_cleanup:
    // Release all pools and free memory
    for (int i = 0; i < numPools; ++i)
    {
        free(mScratch[i].pmem);
    }
    free(mScratch);
    mScratch = NULL;
    return status;
}

void Dap1Process::stop()
{
    ALOGI("%s()", __FUNCTION__);
    if (mScratch != NULL)
    {
        ak_stop(mAk);
        int numPools = ak_num_pools() + 1;
        for (int i = 0; i < numPools; ++i)
        {
            free(mScratch[i].pmem);
        }
        free(mScratch);
        mScratch = NULL;
    }
}

status_t Dap1Process::init()
{
    ALOGI("%s()", __FUNCTION__);
    if ((open() != AK_OK) ||
        (loadLicense() != AK_OK) ||
        (start() != AK_OK) ||
        (validateLicense() != AK_OK))
    {
        return INVALID_OPERATION;
    }
    ALOGI("%s() DAP opened, started and license verified", __FUNCTION__);
    IF_ALOGI()
    {
        int ver_len = 4;
        dap_param_value_t ver[ver_len];
        get(AK_N("ver"), ver, &ver_len);
        String8 ver_info;
        ver_info.appendFormat("%d.%d.%d.%d", ver[0], ver[1], ver[2], ver[3]);
        ALOGI("AK library version: %s", ver_info.string());
    }

    return NO_ERROR;
}

int Dap1Process::get(AK_NAME name, dap_param_value_t *data, int *length)
{
    ALOGV("%s()", __FUNCTION__);

    int handle = ak_find(mAk, AK_ROOT, name);
    if (handle == 0)
    {
        ALOGE("%s() no handle found for parameter %s",
            __FUNCTION__, dapParamName(static_cast<DapParameterId>(name)).string());
        return AK_ERR_HANDLE;
    }
    // Don't use bulk functions for single length parameter
    if (*length == 1)
    {
        *data = ak_get(mAk, handle, 0);
        return AK_OK;
    }
    // Verify that we are not fetching more values than possible
    int max_len = ak_get_length(mAk, handle);
    int len = (*length > max_len) ? max_len : *length;

    // Get the values from DAP
    int status = ak_get_bulk(mAk, handle, 0, len, AK_DATATYPE_INT32, data);
    if (status == AK_OK)
    {
        *length = len;
        ALOGVV("%s() Fetched %s", __FUNCTION__,
            dapParamNameValue(static_cast<DapParameterId>(name), data, len).string());
    }
    return status;
}

int Dap1Process::set(AK_NAME name, const dap_param_value_t *data, int length)
{
    ALOGV("%s()", __FUNCTION__);
    int handle = ak_find(mAk, AK_ROOT, name);
    if (handle == 0)
    {
        ALOGE("%s() no handle found for parameter %s",
            __FUNCTION__, dapParamName(static_cast<DapParameterId>(name)).string());
        return AK_ERR_HANDLE;
    }
    if (length == 1)
    {
        int set_val = ak_set(mAk, handle, 0, *data);
        ALOGW_IF(set_val != *data,
            "%s() Value for parameter %s clamped to %d expected %hd.", __FUNCTION__,
            dapParamName(static_cast<DapParameterId>(name)).string(), set_val, *data);
        return AK_OK;
    }
    else
    {
        return ak_set_bulk(mAk, handle, 0, length, AK_DATATYPE_INT32, data);
    }
}

status_t Dap1Process::configure(int bufferSize, int sampleRate, audio_format_t format, audio_channel_mask_t inChannels, audio_channel_mask_t outChannels)
{
    ALOGD("%s(sampleRate=%d, format=%d, inChannels=%d, outChannels=%d)",
        __FUNCTION__, sampleRate, format, inChannels, outChannels);

    static const char kChannelMapStereo[] =  {
        AK_CHAN_L, AK_CHAN_R
    };
    static const char kChannelMap5_1[] = {
        AK_CHAN_L, AK_CHAN_R, AK_CHAN_C, AK_CHAN_LFE, AK_CHAN_Ls, AK_CHAN_Rs
    };
    static const char kChannelMap7_1[] = {
        AK_CHAN_L, AK_CHAN_R, AK_CHAN_C, AK_CHAN_LFE, AK_CHAN_Ls, AK_CHAN_Rs, AK_CHAN_Lb, AK_CHAN_Rb
    };

    int numChannels = 0;
    const char *channelMap = NULL;
    dap_param_value_t outChannelFormat = 0;

    switch(inChannels)
    {
    case AUDIO_CHANNEL_OUT_STEREO:
        channelMap = kChannelMapStereo;
        numChannels = 2;
        break;
#if 0
    // TODO: We won't activate the multichannel processing before we have a thorough test.
    case AUDIO_CHANNEL_OUT_5POINT1:
        channelMap = kChannelMap5_1;
        numChannels = 6;
        break;
    case AUDIO_CHANNEL_OUT_7POINT1:
        channelMap = kChannelMap7_1;
        numChannels = 8;
        break;
#endif
    default:
        ALOGE("Input channel configuration %d is not supported", inChannels);
        return BAD_VALUE;
    }

    switch(outChannels)
    {
    case AUDIO_CHANNEL_OUT_STEREO:
        outChannelFormat = DS1_OCF_STEREO;
        break;
#if 0
    // TODO: We won't activate the multichannel processing before we have a thorough test.
    case AUDIO_CHANNEL_OUT_5POINT1:
        outChannelFormat = DS1_OCF_5DOT1;
        break;
    case AUDIO_CHANNEL_OUT_7POINT1:
        outChannelFormat = DS1_OCF_7DOT1;
        break;
#endif
    default:
        ALOGE("Output channel configuration %d is not supportd", outChannels);
        return BAD_VALUE;
    }

    int status = ak_set_input_config(mAk, AK_ROOT, sampleRate, NUM_PCM_SAMPLES_PER_BLOCK, numChannels, channelMap);
    if (status != AK_OK)
    {
        ALOGE("Can not configure DAP with sample rate %d, num channels %d (error %d)",
            sampleRate, numChannels, status);
        return BAD_VALUE;
    }
    status = set(AK_N("ocf"), &outChannelFormat, 1);
    if (status != AK_OK)
    {
        ALOGE("Can not set output format %d (error %d)", outChannelFormat, status);
        return BAD_VALUE;
    }
    return NO_ERROR;
}

status_t Dap1Process::process(BufferProvider *inBuffer, BufferProvider *outBuffer)
{
    ALOGVV("%s() start", __FUNCTION__);
    int numSamples = inBuffer->capacity();

    int status = ak_set_input_buffer(mAk, AK_ROOT, inBuffer->buffer(), 0, numSamples);
    if (status != AK_OK)
    {
        ALOGE("Error %d while setting input buffer", status);
        return INVALID_OPERATION;
    }

#ifdef DAP_PROFILING
    int64_t startTime = elapsedRealtimeNano();
#endif

    status = ak_process(mAk, mScratch->pmem);

#ifdef DAP_PROFILING
    int64_t duration_us = (elapsedRealtimeNano() - startTime) / 1000;
    ALOGI("Time for ak_process() %lld microseconds", duration_us);
#endif

    if (status != AK_OK)
    {
        ALOGE("Error %d while processing DAP", status);
        return INVALID_OPERATION;
    }
    mHasVisualizerData = true;

    status = ak_get_output_buffer(mAk, AK_ROOT, outBuffer->buffer(), 0, numSamples);
    if (status != AK_OK)
    {
        ALOGE("Error %d while getting output buffer", status);
        return INVALID_OPERATION;
    }
    ALOGVV("%s() end", __FUNCTION__);
    return NO_ERROR;
}

status_t Dap1Process::setParam(DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGD("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, length).string());

    int status = set(param, values, length);
    if (status != AK_OK)
    {
        ALOGE("%s() Error %d while setting parameter %s",
            __FUNCTION__, status, dapParamName(param).string());
        return BAD_VALUE;
    }
    return NO_ERROR;
}

status_t Dap1Process::getParam(DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGD("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, *length).string());

    int status = get(param, values, length);
    if (status != AK_OK)
    {
        ALOGE("%s() Error %d while getting parameter %s",
            __FUNCTION__, status, dapParamName(param).string());
        return BAD_VALUE;
    }

    if (param == DAP_PARAM_VER)
    {
        String8 ver_info;
        ver_info.appendFormat("%d.%d.%d.%d", values[0], values[1], values[2], values[3]);
        strcpy((char *)values, ver_info.string());
        *length = ver_info.size();
    }
    return NO_ERROR;
}

status_t Dap1Process::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s(data=%p, bands=%d)", __FUNCTION__, data, *bands);
    if (!mHasVisualizerData)
    {
        ALOGVV("%s() No audio processed for visualizer data.", __FUNCTION__);
        return NO_INIT;
    }
    mHasVisualizerData = false;

    int status = get(DAP1_PARAM_VCBG, data, bands);
    if (status != AK_OK)
    {
        ALOGE("%s() Error %d getting VCBG data.", __FUNCTION__, status);
        return BAD_VALUE;
    }

    status = get(DAP1_PARAM_VCBE, (data + *bands), bands);
    if (status != AK_OK)
    {
        ALOGE("%s() Error %d getting VCBE data.", __FUNCTION__, status);
        return BAD_VALUE;
    }

    return NO_ERROR;
}

} // namespace dolby
