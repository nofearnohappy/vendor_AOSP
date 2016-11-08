/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2009
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

/*******************************************************************************
 *
 * Filename:
 * ---------
 * AudioCompFltCustParam.cpp
 *
 * Project:
 * --------
 *   Android
 *
 * Description:
 * ------------
 *   This file implements customized parameter handling
 *
 * Author:
 * -------
 *   Tina Tsai (mtk01981)
 *
 *------------------------------------------------------------------------------
 * $Revision: #2 $
 * $Modtime:$
 * $Log:$
 *
 *******************************************************************************/

/*=============================================================================
 *                              Include Files
 *===========================================================================*/
#define LOG_TAG "AudioCompFltCustParam"
#if defined(PC_EMULATION)
#include "windows.h"
#else
#include "unistd.h"
#include "pthread.h"
#endif

#include <utils/Log.h>
#include <utils/String8.h>

#include "CFG_AUDIO_File.h"
#include "Custom_NvRam_LID.h"
#include "libnvram.h"
#include "AudioCompensationFilter.h"
#include "CFG_Audio_Default.h"
#include <cutils/properties.h>
#include "AudioCustParam.h"

#define MTK_AUDIO_TUNING_TOOL_V2_PHASE_THIS_REV (2)
#if (MTK_AUDIO_TUNING_TOOL_V2_PHASE >= MTK_AUDIO_TUNING_TOOL_V2_PHASE_THIS_REV)
#include "AudioParamParser.h"
#include <media/AudioSystem.h>
#endif

#ifdef MTK_BASIC_PACKAGE
#define USE_DEFAULT_CUST_TABLE //For BringUp usage
#endif

#ifndef ASSERT
#define ASSERT(x)
#endif

namespace android
{

/*=============================================================================
 *                             Public Function
 *===========================================================================*/
#if 0 //Call checkNvramReady of audiocustparam
bool checkNvramReady(void)
{
#if defined(USE_DEFAULT_CUST_TABLE)
    return true;
#endif

    int read_nvram_ready_retry = 0;
    int ret = 0;
    char nvram_init_val[PROPERTY_VALUE_MAX];
    while (read_nvram_ready_retry < MAX_RETRY_COUNT)
    {
        read_nvram_ready_retry++;
        property_get("service.nvram_init", nvram_init_val, NULL);
        if (strcmp(nvram_init_val, "Ready") == 0 ||
            strcmp(nvram_init_val, "Pre_Ready") == 0)
        {
            ret = true;
            break;
        }
        else
        {
            usleep(500 * 1000);
        }
    }
    ALOGD("Get nvram restore ready retry cc=%d\n", read_nvram_ready_retry);
    if (read_nvram_ready_retry >= MAX_RETRY_COUNT)
    {
        ALOGW("Get nvram restore ready faild !!!\n");
        ret = false;
    }
    return ret;
}
#endif

int getDefaultAudioCompFltParam(AudioCompFltType_t eFLTtype, AUDIO_ACF_CUSTOM_PARAM_STRUCT *audioParam)
{
    int dDataSize = 0;
    if (AUDIO_COMP_FLT_AUDIO == eFLTtype)
    {
        memcpy((void *)audioParam, (void *) & (audio_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
    else if (AUDIO_COMP_FLT_HEADPHONE == eFLTtype)
    {
        memcpy((void *)audioParam, (void *) & (audio_hcf_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
    else if (AUDIO_COMP_FLT_AUDENH == eFLTtype)
    {
        memset((void *)audioParam, 0x00, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
    else if (AUDIO_COMP_FLT_VIBSPK == eFLTtype)
    {
        memcpy((void*)audioParam, (void*)&(audio_vibspk_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)&& defined(MTK_STEREO_SPK_ACF_TUNING_SUPPORT) //means :92 above support
    else if (AUDIO_COMP_FLT_AUDIO_SUB == eFLTtype)
    {
        memcpy((void *)audioParam, (void *) & (audiosub_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
#endif
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    else if (AUDIO_COMP_FLT_DRC_FOR_MUSIC == eFLTtype)
    {
        memcpy((void *)audioParam, (void *) & (audio_musicdrc_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
    else if (AUDIO_COMP_FLT_DRC_FOR_RINGTONE == eFLTtype)
    {
        memcpy((void *)audioParam, (void *) & (audio_ringtonedrc_custom_default), sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        dDataSize = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
    }
#endif
    else//Shouldn't happen
    {
        ;
        ASSERT(0);
    }

    return dDataSize;
}

#if (MTK_AUDIO_TUNING_TOOL_V2_PHASE >= MTK_AUDIO_TUNING_TOOL_V2_PHASE_THIS_REV)
const char *FltAudioTypeFileName[] = {"PlaybackACF", "PlaybackHCF", "","","","PlaybackDRC","PlaybackDRC"};
const char *FltCategoryName[] = {"Profile,Speaker", "Profile,Headset", "","","","Volume type,Music","Volume type,Ring"};
#define BES_LOUDNESS_L_HFP_FC "bes_loudness_L_hpf_fc"
#define BES_LOUDNESS_L_HPF_ORDER "bes_loudness_L_hpf_order"
#define BES_LOUDNESS_L_LPF_FC "bes_loudness_L_lpf_fc"
#define BES_LOUDNESS_L_LPF_ORDER "bes_loudness_L_lpf_order"
#define BES_LOUDNESS_L_BPF_FC "bes_loudness_L_bpf_fc"
#define BES_LOUDNESS_L_BPF_BW "bes_loudness_L_bpf_bw"
#define BES_LOUDNESS_L_BPF_GAIN "bes_loudness_L_bpf_gain"

#define BES_LOUDNESS_R_HFP_FC "bes_loudness_R_hpf_fc"
#define BES_LOUDNESS_R_HPF_ORDER "bes_loudness_R_hpf_order"
#define BES_LOUDNESS_R_LPF_FC "bes_loudness_R_lpf_fc"
#define BES_LOUDNESS_R_LPF_ORDER "bes_loudness_R_lpf_order"
#define BES_LOUDNESS_R_BPF_FC "bes_loudness_R_bpf_fc"
#define BES_LOUDNESS_R_BPF_BW "bes_loudness_R_bpf_bw"
#define BES_LOUDNESS_R_BPF_GAIN "bes_loudness_R_bpf_gain"

#define BES_LOUDNESS_SEP_LR_FILTER "bes_loudness_Sep_LR_Filter"
#define BES_LOUDNESS_WS_GAIN_MAX "bes_loudness_WS_Gain_Max"
#define BES_LOUDNESS_WS_GAIN_MIN "bes_loudness_WS_Gain_Min"
#define BES_LOUDNESS_FILTER_FIRST "bes_loudness_Filter_First"
#define BES_LOUDNESS_NUM_BANDS "bes_loudness_Num_Bands"
#define BES_LOUDNESS_FLT_BANK_ORDER "bes_loudness_Flt_Bank_Order"
#define BES_LOUDNESS_CROSS_FREQ "bes_loudness_Cross_Freq"
#define DRC_TH "DRC_Th"
#define DRC_GN "DRC_Gn"
#define SB_GN "SB_Gn"
#define SB_MODE "SB_Mode"
#define DRC_DELAY "DRC_Delay"
#define ATT_TIME "Att_Time"
#define REL_TIME "Rel_Time"
#define HYST_TH "Hyst_Th"
#define LIM_TH "Lim_Th"
#define LIM_GN "Lim_Gn"
#define LIM_CONST "Lim_Const"
#define LIM_DELAY "Lim_Delay"
#define SWIPREV "SWIPRev"

uint16_t sizeByteParaData(DATA_TYPE dataType, uint16_t arraySize)
{
    uint16_t sizeUnit = 4;

    switch (dataType)
    {
        case TYPE_INT:
            sizeUnit = 4;
            break;
        case TYPE_UINT:
            sizeUnit = 4;
            break;
        case TYPE_FLOAT:
            sizeUnit = 4;
            break;
        case TYPE_BYTE_ARRAY:
            sizeUnit = arraySize;
            break;
        case TYPE_USHORT_ARRAY:
        case TYPE_SHORT_ARRAY:
            sizeUnit = arraySize << 1;
            break;
        case TYPE_UINT_ARRAY:
        case TYPE_INT_ARRAY:
            sizeUnit = arraySize << 2;
            break;
        default:
            sizeUnit = 4;
    }

    //ALOGD("-%s(), arraySize=%d, sizeUnit=%d", __FUNCTION__, arraySize, sizeUnit);

    return sizeUnit;
}

int getPlaybackPostProcessParameterFromXML(AudioCompFltType_t eFLTtype, AUDIO_ACF_CUSTOM_PARAM_STRUCT *audioParam)
{
    int dReturnValue = 0;

    if (eFLTtype != AUDIO_COMP_FLT_AUDIO
        && eFLTtype != AUDIO_COMP_FLT_HEADPHONE
        && eFLTtype != AUDIO_COMP_FLT_DRC_FOR_MUSIC
        && eFLTtype != AUDIO_COMP_FLT_DRC_FOR_RINGTONE)
        {
            ALOGE("Error %s Line %d eFLTtype %d", __FUNCTION__, __LINE__, eFLTtype);
            return -1;
        } else {
            ALOGD("%s Type/Name [%d]/[%s]", __FUNCTION__, eFLTtype, FltAudioTypeFileName[eFLTtype]);

            AppHandle *pAppHandle = appHandleGetInstance();
            if (NULL == pAppHandle) {
                ALOGE("Error %s %d",__FUNCTION__,__LINE__);
                return -1;
            }
            AudioType *pAudioType = appHandleGetAudioTypeByName(pAppHandle, FltAudioTypeFileName[eFLTtype]);
            if (NULL == pAudioType) {
                ALOGE("Error %s %d",__FUNCTION__,__LINE__);
                return -1;
            }

            audioTypeReadLock(pAudioType, __FUNCTION__);
            // Load data
            do {
                Param  *pParamInfo;
                uint16_t sizeByteParam;
                ParamUnit* pParamUnit = audioTypeGetParamUnit(pAudioType, FltCategoryName[eFLTtype]);
                if (NULL == pParamUnit) {
                    dReturnValue = -1;
                    ALOGE("Error %s %d",__FUNCTION__,__LINE__);
                    break;
                }
/*L Filter*/
                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_HFP_FC);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_L_hpf_fc = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_L_hpf_fc = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_L_hpf_fc);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_HPF_ORDER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_L_hpf_order = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_L_hpf_order = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_L_hpf_order);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_LPF_FC);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_L_lpf_fc = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_L_lpf_fc = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_L_lpf_fc);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_LPF_ORDER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_L_lpf_order = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_L_lpf_order = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_L_lpf_order);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_BPF_FC);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_L_bpf_fc), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_BPF_BW);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_L_bpf_bw), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_L_BPF_GAIN);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_L_bpf_gain), pParamInfo->data, sizeByteParam);
/*R Filter*/
                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_HFP_FC);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_R_hpf_fc = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_R_hpf_fc = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_R_hpf_fc);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_HPF_ORDER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_R_hpf_order = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_R_hpf_order = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_R_hpf_order);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_LPF_FC);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_R_lpf_fc = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_R_lpf_fc = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_R_lpf_fc);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_LPF_ORDER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_f_param.V5F.bes_loudness_R_lpf_order = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_R_lpf_order = %d",audioParam->bes_loudness_f_param.V5F.bes_loudness_R_lpf_order);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_BPF_FC);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_R_bpf_fc), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_BPF_BW);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_R_bpf_bw), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_R_BPF_GAIN);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_f_param.V5F.bes_loudness_R_bpf_gain), pParamInfo->data, sizeByteParam);
/*DRC*/
                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_SEP_LR_FILTER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_Sep_LR_Filter = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_Sep_LR_Filter = %d",audioParam->bes_loudness_Sep_LR_Filter);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_WS_GAIN_MAX);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_WS_Gain_Max = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_WS_Gain_Max = %d",audioParam->bes_loudness_WS_Gain_Max);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_WS_GAIN_MIN);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_WS_Gain_Min = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_WS_Gain_Min = %d",audioParam->bes_loudness_WS_Gain_Min);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_FILTER_FIRST);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_Filter_First = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_Filter_First = %d",audioParam->bes_loudness_Filter_First);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_NUM_BANDS);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_Num_Bands = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_Num_Bands = %d",audioParam->bes_loudness_Num_Bands);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_FLT_BANK_ORDER);
                ASSERT(pParamInfo!=NULL);
                audioParam->bes_loudness_Flt_Bank_Order = *((unsigned int*)pParamInfo->data);
                ALOGD("bes_loudness_Flt_Bank_Order = %d",audioParam->bes_loudness_Flt_Bank_Order);

                pParamInfo = paramUnitGetParamByName(pParamUnit, BES_LOUDNESS_CROSS_FREQ);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->bes_loudness_Cross_Freq), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, DRC_TH);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->DRC_Th), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, DRC_GN);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->DRC_Gn), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, SB_GN);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->SB_Gn), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, SB_MODE);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->SB_Mode), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, DRC_DELAY);
                ASSERT(pParamInfo!=NULL);
                audioParam->DRC_Delay = *((unsigned int*)pParamInfo->data);
                ALOGD("DRC_Delay = %d",audioParam->DRC_Delay);

                pParamInfo = paramUnitGetParamByName(pParamUnit, ATT_TIME);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->Att_Time), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, REL_TIME);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->Rel_Time), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, HYST_TH);
                ASSERT(pParamInfo!=NULL);
                sizeByteParam = sizeByteParaData((DATA_TYPE)pParamInfo->paramInfo->dataType, pParamInfo->arraySize);
                memcpy(&(audioParam->Hyst_Th), pParamInfo->data, sizeByteParam);

                pParamInfo = paramUnitGetParamByName(pParamUnit, LIM_TH);
                ASSERT(pParamInfo!=NULL);
                audioParam->Lim_Th = *((int*)pParamInfo->data);
                ALOGD("Lim_Th = %d",audioParam->Lim_Th);

                pParamInfo = paramUnitGetParamByName(pParamUnit, LIM_GN);
                ASSERT(pParamInfo!=NULL);
                audioParam->Lim_Gn = *((int*)pParamInfo->data);
                ALOGD("Lim_Gn = %d",audioParam->Lim_Gn);

                pParamInfo = paramUnitGetParamByName(pParamUnit, LIM_CONST);
                ASSERT(pParamInfo!=NULL);
                audioParam->Lim_Const = *((unsigned int*)pParamInfo->data);
                ALOGD("Lim_Const = %d",audioParam->Lim_Const);

                pParamInfo = paramUnitGetParamByName(pParamUnit, LIM_DELAY);
                ASSERT(pParamInfo!=NULL);
                audioParam->Lim_Delay = *((unsigned int*)pParamInfo->data);
                ALOGD("Lim_Delay = %d",audioParam->Lim_Delay);

                pParamInfo = paramUnitGetParamByName(pParamUnit, SWIPREV);
                ASSERT(pParamInfo!=NULL);
                audioParam->SWIPRev = *((int*)pParamInfo->data);
                ALOGD("SWIPRev = 0x%x",audioParam->SWIPRev);
            } while (0);
            audioTypeUnlock(pAudioType);
        }
        ALOGD("%s Parameter %d dReturnValue %d", __FUNCTION__, eFLTtype,dReturnValue);
        return dReturnValue;
}

const char* strUpdateFLT[]={"UpdateACFHCFParameters=0","UpdateACFHCFParameters=1","","","UpdateACFHCFParameters=2","UpdateACFHCFParameters=3","UpdateACFHCFParameters=4"};

void CallbackAudioCompFltCustParamXmlChanged(AppHandle *appHandle, const char *audioTypeName)
{
    ALOGD("+%s(), audioType = %s", __FUNCTION__, audioTypeName);
    // reload XML file
    if (appHandleReloadAudioType(appHandle, audioTypeName) == APP_ERROR)
    {
        ALOGE("%s(), Reload xml fail!(audioType = %s)", __FUNCTION__, audioTypeName);
    }
    else
    {
        if (strcmp(audioTypeName, FltAudioTypeFileName[AUDIO_COMP_FLT_AUDIO]) == 0)
        {
            //"PlaybackACF"
            ALOGD("PlaybackACF:UpdateACFHCFParameters=0 +");
            AudioSystem::setParameters(0, String8(strUpdateFLT[AUDIO_COMP_FLT_AUDIO]/*"UpdateACFHCFParameters=0"*/));
            ALOGD("PlaybackACF:UpdateACFHCFParameters=0 -");
        }
        else if (strcmp(audioTypeName, FltAudioTypeFileName[AUDIO_COMP_FLT_HEADPHONE]) == 0)
        {
            //"PlaybackHCF"
            ALOGD("PlaybackHCF:UpdateACFHCFParameters=1 +");
            AudioSystem::setParameters(0, String8(strUpdateFLT[AUDIO_COMP_FLT_HEADPHONE]/*"UpdateACFHCFParameters=0"*/));
            ALOGD("PlaybackHCF:UpdateACFHCFParameters=1 -");
        }
        else if (strcmp(audioTypeName, FltAudioTypeFileName[AUDIO_COMP_FLT_DRC_FOR_MUSIC]) == 0)
        {
            //"PlaybackDRC"
            ALOGD("PlaybackDRC:UpdateACFHCFParameters=3 +");
            AudioSystem::setParameters(0, String8(strUpdateFLT[AUDIO_COMP_FLT_DRC_FOR_MUSIC]/*"UpdateACFHCFParameters=0"*/));
            ALOGD("PlaybackDRC:UpdateACFHCFParameters=3 -");
        }
    }
    ALOGD("-%s(), audioType = %s", __FUNCTION__, audioTypeName);
}

int AudioComFltCustParamInit(void)
{
    ALOGD("AudioComFltCustParamInit + ");
    AppHandle *pAppHandle = appHandleGetInstance();
    if (NULL == pAppHandle) {
        ALOGE("Error %s %d",__FUNCTION__,__LINE__);
        return -1;
    }
    appHandleRegXmlChangedCb(pAppHandle, CallbackAudioCompFltCustParamXmlChanged);
    ALOGD("AudioComFltCustParamInit - CallbackAudioCompFltCustParamXmlChanged");
    return 0;
}
#else
int AudioComFltCustParamInit(void)
{
    ALOGD("AudioComFltCustParamInit Do nothing");
    return 0;
}
#endif

int  GetAudioCompFltCustParamFromNV(AudioCompFltType_t eFLTtype, AUDIO_ACF_CUSTOM_PARAM_STRUCT *audioParam)
{
    int result = 0;

#if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultAudioCompFltParam(eFLTtype, audioParam);
#else
    F_ID audio_nvram_fd;
    int file_lid;
    int i = 0, rec_sizem, rec_size, rec_num;
    if (AUDIO_COMP_FLT_AUDIO == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_COMPFLT_LID;
    }
    else if (AUDIO_COMP_FLT_HEADPHONE == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_HEADPHONE_COMPFLT_LID;
    }
    else if (AUDIO_COMP_FLT_VIBSPK == eFLTtype)
    {
       file_lid = AP_CFG_RDCL_FILE_VIBSPK_COMPFLT_LID;
    }
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)&&defined(MTK_STEREO_SPK_ACF_TUNING_SUPPORT) //means :92 above support
    else if (AUDIO_COMP_FLT_AUDIO_SUB == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIOSUB_COMPFLT_LID;
    }
#endif
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5) //means :95 above support
    else if (AUDIO_COMP_FLT_DRC_FOR_MUSIC == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_MUSIC_DRC_LID;
    }
    else if (AUDIO_COMP_FLT_DRC_FOR_RINGTONE == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_RINGTONE_DRC_LID;
    }
#endif
    else//Shouldn't happen
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_COMPFLT_LID;
        ASSERT(0);
    }
#if ((MTK_AUDIO_TUNING_TOOL_V2_PHASE >= MTK_AUDIO_TUNING_TOOL_V2_PHASE_THIS_REV) && defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5))
        if ( eFLTtype == AUDIO_COMP_FLT_AUDIO
            || eFLTtype == AUDIO_COMP_FLT_HEADPHONE
            || eFLTtype == AUDIO_COMP_FLT_DRC_FOR_MUSIC
            || eFLTtype == AUDIO_COMP_FLT_DRC_FOR_RINGTONE)
        {
            if ( getPlaybackPostProcessParameterFromXML(eFLTtype, audioParam) >= 0) {
                result = sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT);
            } else {
                result = 0;
            }

        } else
#endif
    {
        audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
        result = read(audio_nvram_fd.iFileDesc, audioParam, rec_size * rec_num);
        NVM_CloseFileDesc(audio_nvram_fd);
    }

    if (result != sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT))
    {
        ALOGE("%s(), size wrong, using default parameters,result=%d, struct size=%zu", __FUNCTION__, result, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        result = getDefaultAudioCompFltParam(eFLTtype, audioParam);
    }
#endif
    return result;
}

int  SetAudioCompFltCustParamToNV(AudioCompFltType_t eFLTtype, AUDIO_ACF_CUSTOM_PARAM_STRUCT *audioParam)
{
    int result = 0;
#if defined(USE_DEFAULT_CUST_TABLE)
    result = 0;
#else
    // write to NV ram
    F_ID audio_nvram_fd;
    int file_lid;
    int i = 0, rec_sizem, rec_size, rec_num;

    if (AUDIO_COMP_FLT_AUDIO == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_COMPFLT_LID;
    }
    else if (AUDIO_COMP_FLT_HEADPHONE == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_HEADPHONE_COMPFLT_LID;
    }
    else if (AUDIO_COMP_FLT_VIBSPK == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_VIBSPK_COMPFLT_LID;
    }
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)&& defined(MTK_STEREO_SPK_ACF_TUNING_SUPPORT)//means :92 above support

    else if (AUDIO_COMP_FLT_AUDIO_SUB == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIOSUB_COMPFLT_LID;
    }
#endif
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)//means :95 above support
    else if (AUDIO_COMP_FLT_DRC_FOR_MUSIC == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_MUSIC_DRC_LID;
    }
    else if (AUDIO_COMP_FLT_DRC_FOR_RINGTONE == eFLTtype)
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_RINGTONE_DRC_LID;
    }
#endif
    else//Shouldn't happen
    {
        file_lid = AP_CFG_RDCL_FILE_AUDIO_COMPFLT_LID;
        ASSERT(0);
    }

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    result = write(audio_nvram_fd.iFileDesc, audioParam, rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif
    return result;
}


}; // namespace android
