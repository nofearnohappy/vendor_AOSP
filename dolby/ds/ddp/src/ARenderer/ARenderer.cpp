/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2014 by Dolby Laboratories,
 *                            All rights reserved.
 *
 */
#define LOG_TAG "ARenderer"
#include "DlbLog.h"
#include "ARenderer.h"
#include "oamdi/include/oamdi_dec.h"
//<<MTK_added
#include <stdlib.h>
//MTK_added>>

namespace dolby {

using namespace android;

/* Initiliaze the underlying DAP and OAMDI modules
 *
 * @param sampleRate    Input data sample rate
 *
 * @return NO_ERROR on success or error value
 */
status_t ARenderer::init(int sampleRate)
{
    ALOGI("%s sampleRate %d", __FUNCTION__, sampleRate);
    mDap2JocPtr = new Dap2JocProcess(sampleRate);
    if (mDap2JocPtr == NULL)
    {
        ALOGE("Couldn't open DAP2 instance.");
        return NO_INIT;
    }

    char version[16];
    int  length = 16;
    mDap2JocPtr->getParam(DAP_PARAM_VER, (int *)version, &length);
    ALOGI("DAP version: %s", version);

    mEvoHandlePtr = evo_parser_init();
    if (mEvoHandlePtr == NULL)
    {
        ALOGE("%s evo_parser_init failed", __FUNCTION__);
        return NO_INIT;
    }

    status_t status = initOamdi();
    return status;
}

/* De-initiliaze DAP and frees the corresponding memory
 *
 */
void ARenderer::deinit()
{
    ALOGI("%s", __FUNCTION__);

    if (mEvoHandlePtr != NULL)
    {
        evo_parser_close(mEvoHandlePtr);
        mEvoHandlePtr = NULL;
    }
    if (mOamdiMemoryPtr != NULL)
    {
        free(mOamdiMemoryPtr);
        mOamdiMemoryPtr = NULL;
        mOamdiPtr = NULL;
        mOamdiSize = 0;
    }
    delete mDap2JocPtr;
    mDap2JocPtr = NULL;
}

/**
 * Initalize the oamd information.
 * @internal
 *
 * @return NO_ERROR on success, and NO_MEMORY on failure.
 */
status_t ARenderer::initOamdi()
{
    status_t status = NO_ERROR;

    /*Initialize the oamdi structure with maximum values. */
    oamdi_init_info oamdiInitInfo;
    oamdiInitInfo.frame_size         = 4096;
    oamdiInitInfo.max_num_objs       = OAMD_MAX_NUM_OBJECTS;
    oamdiInitInfo.max_num_md_updates = OAMDI_MAX_OBJ_INFO_BLKS;
    mOamdiSize = oamdi_query_mem(&oamdiInitInfo);
    mOamdiMemoryPtr = malloc(mOamdiSize);
    if (mOamdiMemoryPtr != NULL)
    {
        mOamdiPtr = oamdi_init(&oamdiInitInfo, mOamdiMemoryPtr);
    }
    else
    {
        ALOGE("Fail to allocate memory for oamdi!");
        status = NO_MEMORY;
    }

    return status;
}

/* Configure DAP parameters based on endpoint
 *
 * @param config  DAP2 runtime parameters which depend on endpoint.
 *
 * @return NO_ERROR on success
 */
status_t ARenderer::configure(struct dapConfig config, bool isJocContent)
{
    if (mDap2JocPtr == NULL)
    {
        ALOGE("%s: No DAP instance yet!", __FUNCTION__);
        return NO_INIT;
    }
    dap_param_value_t value;
    int length = 1;
    ALOGV("%s() output_mode %d dialog_enhancer %d vl_enable %d vl_in_level %d vl_out_level %d vol_max_boost %d", __FUNCTION__,
            isJocContent ? config.jocOutputMode : config.nonJocOutputMode,
            isJocContent ? config.jocDiaEnEnable : config.nonJocDiaEnEnable,
            config.volLevelerEnable,
            config.volLevelerInTarget,
            config.volLevelerOutTarget,
            config.volMaxBoost);

    // Config output mode
    value = isJocContent ? config.jocOutputMode : config.nonJocOutputMode;
    if (mDap2JocPtr->setParam(DAP2_PARAM_DOM, &value, length) != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP output mode", __FUNCTION__);
    }

    // Config dialog enhancer
    value = isJocContent ? config.jocDiaEnEnable : config.nonJocDiaEnEnable;
    if (mDap2JocPtr->setParam(DAP_PARAM_DEON, &value, length) != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP dialog enhancer enabled state", __FUNCTION__);
    }

    // Config volume leveler enabled state
    value = config.volLevelerEnable;
    if (mDap2JocPtr->setParam(DAP_PARAM_DVLE, &value, length) != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP volume leveler enabled state", __FUNCTION__);
    }

    // Config volume lever input target level
    value = config.volLevelerInTarget;
    if (mDap2JocPtr->setParam(DAP_PARAM_DVLI, &value, length) != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP volume leveler input target", __FUNCTION__);
    }

    // Config volume leveler output target level
    value = config.volLevelerOutTarget;
    if (mDap2JocPtr->setParam(DAP_PARAM_DVLO, &value, length) != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP volume leveler output target", __FUNCTION__);
    }

    // Config volume maximizer boost
    value = config.volMaxBoost;
    if (mDap2JocPtr->setParam(DAP_PARAM_VMB, &value, length)  != NO_ERROR)
    {
        ALOGE("%s Failed on setting DAP volume maximizer boost", __FUNCTION__);
    }

    return NO_ERROR;
}

/* Set DAP_CPDP pregain parameter
 *
 * @param value    Pregain value
 *
 */
status_t ARenderer::setPregain(int value)
{
    ALOGV("%s: pregain: %d", __FUNCTION__, value);
    status_t status = NO_ERROR;
    if (mDap2JocPtr != NULL)
    {
        int length = 1;
        if (mDap2JocPtr->setParam(DAP_PARAM_PREG, &value, length) != NO_ERROR)
        {
            ALOGE("%s Failed on setting DAP pregain", __FUNCTION__);
            status = BAD_VALUE;
        }
    }
    else
    {
        ALOGE("%s: No DAP2 instance yet", __FUNCTION__);
        status = NO_INIT;
    }

    return status;
}

status_t ARenderer::setSystemGain(int value)
{
    ALOGV("%s: system gain %d", __FUNCTION__, value);
    status_t status = NO_ERROR;
    if (mDap2JocPtr != NULL)
    {
        int length = 1;
        if (mDap2JocPtr->setParam(DAP_PARAM_VOL, &value, length) != NO_ERROR)
        {
            ALOGE("%s Failed on setting DAP system gain", __FUNCTION__);
            status = BAD_VALUE;
        }
    }
    else
    {
        ALOGE("%s: No DAP2 instance yet", __FUNCTION__);
        status = NO_INIT;
    }

    return status;
}

/**
 * Update the oamd information with the evo frame.
 * @internal
 *
 * @param evo_frame_data   EVO frame data.
 * @param evo_frame_size   The size of EVO frame data.
 * @return NO_ERROR on success, and negative on failure.
 */
status_t ARenderer::setOamdi(void *evo_frame_data, unsigned int evo_frame_size)
{
    if (evo_frame_data == NULL || evo_frame_size == 0)
    {
        ALOGV("%s: Invalid EVO frame data %p or size %u", __FUNCTION__, evo_frame_data, evo_frame_size);
        return BAD_VALUE;
    }

    status_t status;
    mOamdiOffset = 0;
    /* parse the evo frame to extract the OAMD payload */
    status = get_oamd_pd_from_evo(mEvoHandlePtr, evo_frame_data, evo_frame_size, &mOamdiOffset);
    if (status != NO_ERROR)
    {
        ALOGE("%s get_oamd_pd_from_evo returns error %d", __FUNCTION__, status);
    }
    else
    {
        char oamdiPre[mOamdiSize];
        memcpy((void *)oamdiPre, (void *)mOamdiPtr, mOamdiSize);
        /* parse OAMD payload and fill OAMDI struct */
        status = oamdi_from_bitstream(mOamdiPtr, mEvoHandlePtr->oamdPdSize, mEvoHandlePtr->oamdPdData);
        if (status != NO_ERROR)
        {
            ALOGE("%s oamdi_from_bitstream returns error %d", __FUNCTION__, status);
            // On an error, take the previous oamdi back
            memcpy((void *)mOamdiPtr, (void *)oamdiPre, mOamdiSize);
            oamdi_validate_after_copy(mOamdiPtr);
        }
        else
        {
            unsigned int num_updates = oamdi_get_num_obj_info_blks(mOamdiPtr);
            unsigned int num_objects = oamdi_get_obj_count(mOamdiPtr);
            for (unsigned int update = 0; update < num_updates; update++)
            {
                for (unsigned int object = 0; object < num_objects; object++)
                {
                    const oamdi_obj_info_blk *info_block = oamdi_get_obj_info_blk(mOamdiPtr, object, update);
                    oamdi_obj_info_blk zero_block = *info_block;
                    zero_block.render_info.width_mode = OAMDI_WIDTH_NO_SPREADING;
                    oamdi_set_obj_info_blk(mOamdiPtr, object, update, &zero_block);
                }
            }
            ALOGV("%s: OAMDI num_obj %u, mdOffset %u",
                   __FUNCTION__, oamdi_get_obj_count(mOamdiPtr), mOamdiOffset);
        }
    }

    return status;
}

/**
 * Check if the input channel count is 1/2/6/8 for legacy DD/DD+.
 * @internal
 *
 * @param inChannel   Input Channel Count
 * @return true on legacy channel count, and false otherwise.
 *
 */
bool ARenderer::isLegacyChanCount(int inChannel)
{
    switch(inChannel)
    {
        case 1:
        case 2:
        case 6:
        case 8:
            return true;
    }
    return false;
}

/* Process the objects based audio input to channel based output. Also parses the evo frame
 * and construct the oamdi struct which is passed as input to DAP.
 *
 * @param inChannel                     Input Channel Count
 * @param outChannel                    Output Channel Count
 * @param sampleRate                    Stream sample rate
 * @param in                            Input data to be processed
 * @param out                           Output channel based data
 * @param evo_frame_data                Evolution frame data
 * @param evo_frame_size                Evolution frame size
 * @param sampleCount                   Samples per channel count
 * @param isDataInCQMF                  Input data in CQMF format or not
 */
void ARenderer::process(int inChannel, int outChannel, int sampleRate,
                        void *in, void *out, void *evo_frame_data, unsigned int evo_frame_size,
                        int sampleCount, bool isDataInCQMF)
{
    status_t     status;
    oamdi        *oamdiPtr = NULL;

    status = setOamdi(evo_frame_data, evo_frame_size);
    oamdiPtr = ((status != NO_ERROR) && isLegacyChanCount(inChannel)) ? NULL : mOamdiPtr;
    mDap2JocPtr->process(inChannel, outChannel, mOamdiOffset, oamdiPtr, in, out, sampleCount, isDataInCQMF);
}

} // namespace dolby

