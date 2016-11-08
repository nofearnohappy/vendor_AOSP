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
#define LOG_TAG "DlbDap2Process"
//#define DAP_PROFILING
//<<MTK_added
#include <stdlib.h>
//MTK_added>>
#include <inttypes.h>
#include <utils/SystemClock.h>
#include <utils/String8.h>
#include <hardware/audio_effect.h>
#include <cutils/properties.h>
#include "DlbLog.h"
#include "Dap2Process.h"

static char cEndpointProp[] = "dolby.audio.sink.info";
static const int Speaker51VirtualizerMatrix[] = {16384, 0, 0, 16384,  11583, 11583, 0, 0, 16384, 0, 0, 16384};

namespace dolby {

using namespace android;

Dap2Process::Dap2Process()
{
    ALOGI("%s()", __FUNCTION__);
    mDapCpDpStatePtr     = NULL;
    mSampleRate          = 44100;
    mPersistentMemoryPtr = NULL;
    mScratchMemoryPtr    = NULL;
    mMixDataPtr          = NULL;
    mProcessingMode      = DAP_CPDP_PROCESS_2;
    mVirtualizerOn       = false;
    mHasVisualizerData   = false;
    mVisBandCenterFreqs.clear();
}

Dap2Process::~Dap2Process()
{
    ALOGI("%s()", __FUNCTION__);
    close();
}

int Dap2Process::loadLicense()
{
    ALOGI("%s() no license check!", __FUNCTION__);
    return NO_ERROR;
}

int Dap2Process::validateLicense()
{
    ALOGI("%s() no license check!", __FUNCTION__);
    return NO_ERROR;
}

int Dap2Process::open()
{
    ALOGI("%s()", __FUNCTION__);
    size_t persistent_size;
    size_t scratch_size;

    /* The sample rate is fixed at initialisation time. If you want to change
     * sample rate, you will need to instantiate a new library. */
    dap_cpdp_init_info initInfo;
    initInfo.sample_rate = mSampleRate;
    initInfo.license_data = (unsigned char*)"full\n1589474320,0\n";
    initInfo.license_size = strlen((char*)initInfo.license_data) + 1;
    initInfo.manufacturer_id = 0;
    initInfo.mode = DAP_CPDP_MODE_FULL_SUPPORT;
    /* De-activate media intelligence on initialization */
    initInfo.mi_process_disable = 1;
    /* Activate virtual bass on initialization */
    initInfo.virtual_bass_process_enable = 0;
    /* The maximum object number DAP could accept */
    initInfo.max_num_objects = OAMD_MAX_NUM_OBJECTS;
    ALOGV("%s(): sample rate %lu, mi_process_disable %i, virtual_bass_process_enable %i",
          __FUNCTION__, initInfo.sample_rate, initInfo.mi_process_disable,
          initInfo.virtual_bass_process_enable);

    persistent_size = dap_cpdp_query_memory(&initInfo);
    mPersistentMemoryPtr = malloc(persistent_size);
    if (mPersistentMemoryPtr == NULL)
    {
        ALOGE("Fail to allocate persistent memory!");
        return NO_MEMORY;
    }

    /* The dap_cpdp_init_info struct passed here must be the same as the one
     * passed to dap_cpdp_query_memory(). */
    mDapCpDpStatePtr = dap_cpdp_init(&initInfo, mPersistentMemoryPtr);

    /* Allocate the scratch memory. */
    scratch_size = dap_cpdp_query_scratch(&initInfo);
    mScratchMemoryPtr = malloc(scratch_size);
    if (mScratchMemoryPtr == NULL)
    {
        ALOGE("Fail to allocate scratch memory!");
        dap_cpdp_shutdown(mDapCpDpStatePtr);
        free(mPersistentMemoryPtr);
        mPersistentMemoryPtr = NULL;
        return NO_MEMORY;
    }

    return NO_ERROR;
}

void Dap2Process::close()
{
    ALOGI("%s()", __FUNCTION__);
    mVisBandCenterFreqs.clear();
    if (mScratchMemoryPtr != NULL)
    {
        free(mScratchMemoryPtr);
        mScratchMemoryPtr = NULL;
    }
    if (mMixDataPtr != NULL)
    {
        free(mMixDataPtr);
        mMixDataPtr = NULL;
    }
    if (mDapCpDpStatePtr != NULL)
    {
        dap_cpdp_shutdown(mDapCpDpStatePtr);
        mDapCpDpStatePtr = NULL;
    }
    if (mPersistentMemoryPtr != NULL)
    {
        free(mPersistentMemoryPtr);
        mPersistentMemoryPtr = NULL;
    }
}

status_t Dap2Process::init()
{
    ALOGI("%s()", __FUNCTION__);
    if ((open() != NO_ERROR) ||
        (loadLicense() != NO_ERROR) ||
        (validateLicense() != NO_ERROR))
    {
        return INVALID_OPERATION;
    }
    ALOGI("%s() DAP opened", __FUNCTION__);
    IF_ALOGI()
    {
        ALOGI("DAP library version: %s", dap_cpdp_get_version());
    }

    return NO_ERROR;
}

status_t Dap2Process::configure(int /*bufferSize*/, int sampleRate, audio_format_t format, audio_channel_mask_t inChannels, audio_channel_mask_t outChannels)
{
    ALOGD("%s(sampleRate=%d, format=%d, inChannels=%d, outChannels=%d)",
        __FUNCTION__, sampleRate, format, inChannels, outChannels);

    // TODO: Revisit the code here once more sample rates are supported someday.
    if (sampleRate != 44100 && sampleRate != 48000)
    {
        ALOGE("%s: Un-supported sample rate %i", __FUNCTION__, sampleRate);
        return BAD_VALUE;
    }
    // As audio sample rate is an init-time parameter, DAP must be re-initialized if there's a sample rate change.
    bool isSampleRateChanged = false;
    if (mSampleRate != (unsigned long)sampleRate)
    {
        close();
        mSampleRate = sampleRate;
        status_t status = init();
        if (status != NO_ERROR)
        {
            ALOGE("Can not configure DAP with sample rate %d (error %d)", sampleRate, status);
            return BAD_VALUE;
        }
        // Detect a sample rate change, and return NO_INIT, leaving it to EffectDap
        // to re-apply all the DAP parameters cached.
        isSampleRateChanged = true;
    }

    int processingMode = DAP_CPDP_PROCESS_2;

    switch(inChannels)
    {
        case AUDIO_CHANNEL_OUT_STEREO:
            break;
#if 0
        // TODO: We won't activate the multichannel processing before we have a thorough test.
        case AUDIO_CHANNEL_OUT_5POINT1:
            break;
        case AUDIO_CHANNEL_OUT_7POINT1:
            break;
#endif
        default:
            ALOGE("Input channel configuration %d is not supported", inChannels);
            return BAD_VALUE;
    }

    switch(outChannels)
    {
        case AUDIO_CHANNEL_OUT_STEREO:
            processingMode = DAP_CPDP_PROCESS_2;
            break;
#if 0
        // TODO: We won't activate the multichannel processing before we have a thorough test.
        case AUDIO_CHANNEL_OUT_5POINT1:
            processingMode = DAP_CPDP_PROCESS_5_1;
            break;
        case AUDIO_CHANNEL_OUT_7POINT1:
            processingMode = DAP_CPDP_PROCESS_7_1;
            break;
#endif
        default:
            ALOGE("Output channel configuration %d is not supportd", outChannels);
            return BAD_VALUE;
    }

    if (mVirtualizerOn && processingMode == DAP_CPDP_PROCESS_2)
    {
        char cEndpoint[PROPERTY_VALUE_MAX];
        property_get(cEndpointProp, cEndpoint, "invalid");
        if (strcmp(cEndpoint, "speaker") == 0)
        {
            processingMode = DAP_CPDP_PROCESS_5_1_SPEAKER;
            dap_cpdp_output_mode_set(mDapCpDpStatePtr, processingMode, 2, Speaker51VirtualizerMatrix);
            mProcessingMode = DAP_CPDP_PROCESS_5_1_SPEAKER;
        }
        else if (strcmp(cEndpoint, "headset") == 0)
        {
            processingMode = DAP_CPDP_PROCESS_2_HEADPHONE_HEIGHT;
        }
    }

    if (processingMode != DAP_CPDP_PROCESS_5_1_SPEAKER)
    {
        status_t status = setParam(DAP2_PARAM_DOM, &processingMode, 1);
        if (status != NO_ERROR)
        {
            ALOGE("Can not set output mode %d (error %d)", processingMode, status);
            return BAD_VALUE;
        }
    }

    return isSampleRateChanged ? NO_INIT : NO_ERROR;
}

status_t Dap2Process::process(BufferProvider *inBuffer, BufferProvider *outBuffer)
{
    ALOGVV("%s() start", __FUNCTION__);
    int numSamples = inBuffer->capacity();
    unsigned int sample_count_on_256 = numSamples / DAP_CPDP_PCM_SAMPLES_PER_BLOCK;

    if ( (numSamples == 0) ||
         (numSamples % DAP_CPDP_PCM_SAMPLES_PER_BLOCK))
    {
        ALOGE("%s error numSamples %d", __FUNCTION__, numSamples);
        return INVALID_OPERATION;;
    }

#ifdef DAP_PROFILING
    int64_t startTime = elapsedRealtimeNano();
#endif

    // TODO: As of now the oamd data is ignored, so we pass NULL and 0 to the 4th and 5th argument.
    //       We may revisit the code here if the effect needs oamd data someday.
    int nChannels = dap_cpdp_prepare(mDapCpDpStatePtr, sample_count_on_256, inBuffer->buffer(),
                                     NULL, 0, NULL, mMixDataPtr, 0);
    if (nChannels != outBuffer->channels())
    {
        ALOGE("%s dap_cpdp_prepare failed! nChannels %i, expected %i", __FUNCTION__,
              nChannels, outBuffer->channels());
        return INVALID_OPERATION;
    }

    mMIMetadata = dap_cpdp_process(mDapCpDpStatePtr, outBuffer->buffer(), mScratchMemoryPtr);

#ifdef DAP_PROFILING
    int64_t duration_us = (elapsedRealtimeNano() - startTime) / 1000;
    ALOGI("Time for dap_cpdp_process() %lld microseconds", duration_us);
#endif

    mHasVisualizerData = true;

    ALOGVV("%s() end", __FUNCTION__);
    return NO_ERROR;
}

/**
 * Apply the specified DAP parameter.
 *
 * @param param   The DAP parameter ID.
 * @param values  The pointer to the DAP parameter values.
 * @param length  The number of the DAP parameter values.
 *
 * @return NO_ERROR on success or BAD_VALUE on failure.
 */
status_t Dap2Process::setParam(DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGD("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, length).string());

    if ((length <= 0) || (values == NULL))
    {
        ALOGE("%s: Invalid input argument!", __FUNCTION__);
        return BAD_VALUE;
    }

    status_t status = NO_ERROR;
    switch (param)
    {
        case DAP_PARAM_VOL:
            dap_cpdp_system_gain_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_PSTG:
            dap_cpdp_postgain_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_PREG:
            dap_cpdp_pregain_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_PLB:
            dap_cpdp_calibration_boost_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_NGON:
            dap_cpdp_surround_decoder_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DEON:
            dap_cpdp_de_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DEA:
            dap_cpdp_de_amount_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DED:
            dap_cpdp_de_ducking_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_VMB:
            dap_cpdp_volmax_boost_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVME:
            dap_cpdp_volume_modeler_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVMC:
            dap_cpdp_volume_modeler_calibration_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVLE:
            dap_cpdp_volume_leveler_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVLA:
            dap_cpdp_volume_leveler_amount_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVLI:
            dap_cpdp_volume_leveler_in_target_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DVLO:
            dap_cpdp_volume_leveler_out_target_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_IEON:
            dap_cpdp_ieq_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_IEA:
            dap_cpdp_ieq_amount_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_IEBS:
            if (length != (values[0] * 2 + 1))
            {
                ALOGE("Invalid values length %i for parameter %s with nBands %u!",
                      length, dapParamName(param).string(), (unsigned int)values[0]);
                status = BAD_VALUE;
            }
            else
            {
                unsigned int nBands = (unsigned int)values[0];
                const unsigned int *pBandFreqs = (const unsigned int *)&values[1];
                const int *pBandTargets = (const int *)&values[1 + nBands];
                dap_cpdp_ieq_bands_set(mDapCpDpStatePtr, nBands, pBandFreqs, pBandTargets);
            }
            break;
        case DAP2_PARAM_ARON:
            dap_cpdp_regulator_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_ARDE:
            dap_cpdp_regulator_speaker_distortion_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_ARRA:
            dap_cpdp_regulator_relaxation_amount_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_AROD:
            dap_cpdp_regulator_overdrive_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_ARTP:
            dap_cpdp_regulator_timbre_preservation_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_ARBS:
            if (length != (values[0] * 4 + 1))
            {
                ALOGE("Invalid values length %i for parameter %s with nBands %u!",
                      length, dapParamName(param).string(), (unsigned int)values[0]);
                status = BAD_VALUE;
            }
            else
            {
                unsigned int nBands = (unsigned int)values[0];
                const unsigned int *pBandFreqs = (const unsigned int *)&values[1];
                const int *pLowThresholds  = (const int *)&values[1 + nBands];
                const int *pHighThresholds = (const int *)&values[1 + nBands * 2];
                const int *pIsolatedBands  = (const int *)&values[1 + nBands * 3];
                dap_cpdp_regulator_tuning_set(mDapCpDpStatePtr, nBands, pBandFreqs,
                                              pLowThresholds, pHighThresholds, pIsolatedBands);
            }
            break;
        case DAP2_PARAM_VTON:
            mVirtualizerOn = (*values != 0);
            if (mVirtualizerOn)
            {
                char cEndpoint[PROPERTY_VALUE_MAX];
                property_get(cEndpointProp, cEndpoint, "invalid");
                if (strcmp(cEndpoint, "speaker") == 0)
                {
                    mProcessingMode = DAP_CPDP_PROCESS_5_1_SPEAKER;
                    dap_cpdp_output_mode_set(mDapCpDpStatePtr, mProcessingMode, 2, Speaker51VirtualizerMatrix);
                }
                else if (strcmp(cEndpoint, "headset") == 0)
                {
                    mProcessingMode = DAP_CPDP_PROCESS_2_HEADPHONE_HEIGHT;
                    dap_cpdp_output_mode_set(mDapCpDpStatePtr, mProcessingMode, 2, NULL);
                }
            }
            else if (mProcessingMode == DAP_CPDP_PROCESS_2_HEADPHONE_HEIGHT ||
                     mProcessingMode == DAP_CPDP_PROCESS_5_1_SPEAKER)
            {
                dap_cpdp_output_mode_set(mDapCpDpStatePtr, DAP_CPDP_PROCESS_2, 2, NULL);
                mProcessingMode = DAP_CPDP_PROCESS_2;
            }
            break;
        case DAP2_PARAM_DOM:
            // nb_output_channels (the 3rd argument) is ignored when p_mix_matrix
            // (the 4th argument) is NULL.
            dap_cpdp_output_mode_set(mDapCpDpStatePtr, *values, 2, NULL);
            mProcessingMode = *values;
            break;
        case DAP2_PARAM_DSB:
            dap_cpdp_surround_boost_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_DSSA:
            dap_cpdp_virtualizer_surround_speaker_angle_set(mDapCpDpStatePtr, *values);
            break;
        case DAP_PARAM_GEON:
            dap_cpdp_graphic_equalizer_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_GEBS:
            if (length != (values[0] * 2 + 1))
            {
                ALOGE("Invalid values length %i for parameter %s with nBands %u!",
                      length, dapParamName(param).string(), (unsigned int)values[0]);
                status = BAD_VALUE;
            }
            else
            {
                unsigned int nBands = (unsigned int)values[0];
                const unsigned int *pBandFreqs = (const unsigned int *)&values[1];
                const int *pBandGains = (const int *)&values[1 + nBands];
                dap_cpdp_graphic_equalizer_bands_set(mDapCpDpStatePtr, nBands,
                                                     pBandFreqs, pBandGains);
            }
            break;
        case DAP_PARAM_VCNB:
            break;
        case DAP_PARAM_VCBF:
            mVisBandCenterFreqs.clear();
			mVisBandCenterFreqs.appendArray((unsigned int *)values, length);
            break;
        case DAP2_PARAM_BEON:
            dap_cpdp_bass_enhancer_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_BEB:
            dap_cpdp_bass_enhancer_boost_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_BECF:
            dap_cpdp_bass_enhancer_cutoff_frequency_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_BEW:
            dap_cpdp_bass_enhancer_width_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_VBM:
            dap_cpdp_virtual_bass_mode_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_VBSF:
            if (length != 2)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                dap_cpdp_virtual_bass_src_freqs_set(mDapCpDpStatePtr, values[0], values[1]);
            }
            break;
        case DAP2_PARAM_VBOG:
            dap_cpdp_virtual_bass_overall_gain_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_VBSG:
            dap_cpdp_virtual_bass_slope_gain_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_VBHG:
            /* Virtual Bass subgains can be set only for the 2nd, 3rd and 4th harmonics. */
            if (length > 3)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                dap_cpdp_virtual_bass_subgains_set(mDapCpDpStatePtr, length, values);
            }
            break;
        case DAP2_PARAM_VBMF:
            if (length != 2)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                dap_cpdp_virtual_bass_mix_freqs_set(mDapCpDpStatePtr, values[0], values[1]);
            }
            break;
        case DAP_PARAM_AOON:
            dap_cpdp_audio_optimizer_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_AOBS:
            if (length != (values[0] * (AOBS_MAX_NUM_TUNING_CHANNELS + 1) + 1))
            {
                ALOGE("Invalid values length %i for parameter %s with nBands %u!",
                      length, dapParamName(param).string(), (unsigned int)values[0]);
                status = BAD_VALUE;
            }
            else
            {
                unsigned int nBands = (unsigned int)values[0];
                const unsigned int *pBandFreqs = (const unsigned int *)&values[1];
                int *ptrs[AOBS_MAX_NUM_TUNING_CHANNELS];
                for (int i = 0; i < AOBS_MAX_NUM_TUNING_CHANNELS; i++)
                {
                    ptrs[i] = (int *const)&values[nBands * (i + 1) + 1];
                }
                dap_cpdp_audio_optimizer_bands_set(mDapCpDpStatePtr, nBands, pBandFreqs, ptrs);
            }
            break;
        case DAP2_PARAM_MSCE:
            dap_cpdp_mi2surround_compressor_steering_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_MIEE:
            dap_cpdp_mi2ieq_steering_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_MDLE:
            dap_cpdp_mi2dv_leveler_steering_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_MDEE:
            dap_cpdp_mi2dialog_enhancer_steering_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_DMC:
            if (length*(sizeof(int)) != sizeof(dap_cpdp_mix_data))
            {
                ALOGE("Invalid values length %i for parameter %i!", length, param);
                status = BAD_VALUE;
            }
            else
            {
                dap_cpdp_mix_data *mix = (dap_cpdp_mix_data *)values;
                if (mMixDataPtr == NULL)
                {
                    // TODO: Reset mMixDataPtr to NULL once the downmix metadata doesn't apply.
                    mMixDataPtr = (dap_cpdp_mix_data *)malloc(sizeof(dap_cpdp_mix_data));
                    if (mMixDataPtr == NULL)
                    {
                        ALOGE("Memory allocation for downmix configuration failure!");
                        status = BAD_VALUE;
                        break;
                    }
                }
                mMixDataPtr->cmixlev_q14   = mix->cmixlev_q14;
                mMixDataPtr->surmixlev_q14 = mix->surmixlev_q14;
            }
            break;
        case DAP2_PARAM_POON:
            dap_cpdp_process_optimizer_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_POBS:
            if (length != (values[0] * (DAP_CPDP_MAX_NUM_PROCESS_CHANNELS + 1) + 1))
            {
                ALOGE("Invalid values length %i for parameter %s with nBands %u!",
                      length, dapParamName(param).string(), (unsigned int)values[0]);
                status = BAD_VALUE;
            }
            else
            {
                unsigned int nBands = (unsigned int)values[0];
                const unsigned int *pBandFreqs = (const unsigned int *)&values[1];
                const int *pGains = (const int *)&values[nBands + 1];
                dap_cpdp_process_optimizer_bands_set(mDapCpDpStatePtr, nBands, pBandFreqs, pGains);
            }
            break;
        case DAP2_PARAM_DFSA:
            dap_cpdp_virtualizer_front_speaker_angle_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_DHSA:
            dap_cpdp_virtualizer_height_speaker_angle_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_DHFM:
            dap_cpdp_height_filter_mode_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_BEXE:
            dap_cpdp_bass_extraction_enable_set(mDapCpDpStatePtr, *values);
            break;
        case DAP2_PARAM_BEXF:
            dap_cpdp_bass_extraction_cutoff_frequency_set(mDapCpDpStatePtr, *values);
            break;
        default:
            ALOGE("parameter %s is NOT settable!", dapParamName(param).string());
            status = BAD_VALUE;
            break;
    }
    return status;
}

/**
 * Retrieve the specified DAP parameter.
 *
 * @param param   The DAP parameter ID.
 * @param values  The pointer to save the DAP parameter values.
 * @param length  The pointer to save the number of the DAP parameter values retrieved.
 *
 * @return NO_ERROR on success or BAD_VALUE on failure.
 */
status_t Dap2Process::getParam(DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGD("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, *length).string());

    if ((length == NULL) || (values == NULL))
    {
        ALOGE("%s: Invalid input argument!", __FUNCTION__);
        return BAD_VALUE;
    }

    status_t status = NO_ERROR;
    switch (param)
    {
        case DAP2_PARAM_VNBS:
        {
            const int vnbsLen = DAP_CPDP_VIS_NB_BANDS_MAX * 3 + 1;
            if (*length != vnbsLen)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      *length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                unsigned int *pNbBands         = (unsigned int *)values;
                unsigned int *pBandCenterFreqs = (unsigned int *)&values[1];
                int *pGains       = (int *)&values[DAP_CPDP_VIS_NB_BANDS_MAX + 1];
                int *pExcitations = (int *)&values[DAP_CPDP_VIS_NB_BANDS_MAX * 2 + 1];
                dap_cpdp_vis_bands_get(mDapCpDpStatePtr, pNbBands, pBandCenterFreqs,
                                       pGains, pExcitations);
            }
            break;
        }
        case DAP2_PARAM_ARTI:
        {
            const int artiLen = DAP_CPDP_REGULATOR_TUNING_INFO_NB_BANDS_MAX * 2 + 1;
            if (*length != artiLen)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      *length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                unsigned int *pNbBands = (unsigned int *)values;
                int *pGains       = (int *)&values[1];
                int *pExcitations = (int *)&values[DAP_CPDP_REGULATOR_TUNING_INFO_NB_BANDS_MAX + 1];
                dap_cpdp_regulator_tuning_info_get(mDapCpDpStatePtr, pNbBands, pGains, pExcitations);
            }
            break;
        }
        case DAP_PARAM_VER:
        {
            const char *pVersion = dap_cpdp_get_version();
            if (*length * sizeof(dap_param_value_t) < strlen(pVersion) + 1)
            {
                ALOGE("Invalid values length %i for parameter %s!",
                      *length, dapParamName(param).string());
                status = BAD_VALUE;
            }
            else
            {
                strcpy((char *)values, pVersion);
            }
            break;
        }
        default:
            ALOGE("parameter %s is NOT retrievable!", dapParamName(param).string());
            status = BAD_VALUE;
            break;
    }
    return status;
}

/**
 * Retrieve the visualizer data.
 *
 * @param data  The pointer to save the retrieved visualizer data.
 * @param bands The pointer to the frequency band number.
 *
 * @return NO_ERROR on success or BAD_VALUE on failure.
 */
status_t Dap2Process::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s(data=%p, bands=%d)", __FUNCTION__, data, *bands);
    if (mVisBandCenterFreqs.size() == 0)
    {
        ALOGE("%s() Band number or band center frequencies NOT initialized.", __FUNCTION__);
        return NO_INIT;
    }
    if (!mHasVisualizerData)
    {
        ALOGVV("%s() No audio processed for visualizer data.", __FUNCTION__);
        return NO_INIT;
    }
    if (*bands < (int)mVisBandCenterFreqs.size())
    {
        ALOGE("%s() Band number %i less than the expected %i.",
              __FUNCTION__, *bands, mVisBandCenterFreqs.size());
        return BAD_VALUE;
    }
    mHasVisualizerData = false;
    *bands = mVisBandCenterFreqs.size();

    int *pGains       = (int *)data;
    int *pExcitations = (int *)&data[mVisBandCenterFreqs.size()];
    dap_cpdp_vis_custom_bands_get(mDapCpDpStatePtr, mVisBandCenterFreqs.size(), mVisBandCenterFreqs.array(),
                                  pGains, pExcitations);

    return NO_ERROR;
}

} // namespace dolby
