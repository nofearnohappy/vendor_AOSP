/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
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
 * MtkAudioLoud.cpp
 *
 * Project:
 * --------
 *   Android
 *
 * Description:
 * ------------
 *   This file implements Mtk Audio Loudness
 *
 * Author:
 * -------
 *   JY Huang (mtk01352)
 *
 *------------------------------------------------------------------------------
 * $Revision: #2 $
 * $Modtime:$
 * $Log:$
 *
 * 08 08 2013 kh.hung
 * Clear loudness parameter.
 *
 * 08 07 2013 kh.hung
 * Add 32 bits version.
 *
 *******************************************************************************/

#include <string.h>
#include <stdint.h>
#include <sys/types.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sched.h>
#include <fcntl.h>
#include <assert.h>

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG  "MtkAudioLoud"

#include <sys/ioctl.h>
#include <utils/Log.h>
#include <utils/String8.h>
#include <assert.h>

#include "AudioCompFltCustParam.h"
#include "MtkAudioLoud.h"

//#define ENABLE_LOG_AUDIO_LOUD
#ifdef ENABLE_LOG_AUDIO_LOUD
#undef ALOGV
#define ALOGV(...) ALOGD(__VA_ARGS__)
#endif

#define BLOCK_SIZE 512
#define DRC_NOISEFILTER_MIN 80 // means -80db/-75db
#define NOISE_FILTER_BASE -256
#define NOISE_FILTER_STEP 5

#ifdef FLT_PROFILING
#include <sys/time.h>
#endif

#define HAVE_SWIP
#define AUDIO_ACF_PARAM_USE_CACHE
#include <utils/CallStack.h>
#define CALLSTACK() \
{ \
    ALOGD("CALL STACK : - %s", __FUNCTION__); \
    android::CallStack stack; \
    stack.update(); \
    String8 strtemp = stack.toString(""); \
    ALOGD("\t%s", strtemp.string()); \
}

#define MIN_MULTI_BAND_COUNT 5
#define MAX_MULTI_BAND_COUNT 10
#define MUSIC_UP_BOUND 11   // mMusicCount > MUSIC_UP_BOUND, use MIN_MULTI_BAND_COUNT
#define MUSIC_LOW_BOUND 5   // mMusicCount < MUSIC_LOW_BOUND, use MAX_MULTI_BAND_COUNT
namespace android {
    Mutex mMusicCountMutex;
    int MtkAudioLoud::mMusicCount = 0;
    int MtkAudioLoud::mMaxMusicCount = 10;

#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)||defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
#if 0 //defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
#include "CFG_Audio_Default.h"
BLOUD_HD_CustomParam audio_musicdrc_fixed_default =
{
    BES_LOUDNESS_WS_GAIN_MAX_MUSICDRC,
    BES_LOUDNESS_WS_GAIN_MIN_MUSICDRC,
    BES_LOUDNESS_FILTER_FIRST_MUSICDRC,

    BES_LOUDNESS_SEP_LR_FILTER_MUSICDRC,
    BES_LOUDNESS_NUM_BANDS_MUSICDRC,
    BES_LOUDNESS_FLT_BANK_ORDER_MUSICDRC,
    BES_LOUDNESS_CROSSOVER_FREQ_MUSICDRC,

    BES_LOUDNESS_GAIN_MAP_IN_MUSICDRC,
    BES_LOUDNESS_GAIN_MAP_OUT_MUSICDRC,
    BES_LOUDNESS_SB_GAIN_MUSICDRC,
    BES_LOUDNESS_SB_MODE_MUSICDRC,
    BES_LOUDNESS_DRC_DELAY_MUSICDRC,
    BES_LOUDNESS_ATT_TIME_MUSICDRC,
    BES_LOUDNESS_REL_TIME_MUSICDRC,
    BES_LOUDNESS_HYST_TH_MUSICDRC,
    BES_LOUDNESS_LIM_TH_MUSICDRC,
    BES_LOUDNESS_LIM_GN_MUSICDRC,
    BES_LOUDNESS_LIM_CONST_MUSICDRC,
    BES_LOUDNESS_LIM_DELAY_MUSICDRC,
};

#endif

MtkAudioCustParamCache *MtkAudioCustParamCache::mAudioCustParamCache = NULL;
MtkAudioCustParamCache *MtkAudioCustParamCache::getInstance()
{
    static Mutex InstanceLock;
    Mutex::Autolock _l(InstanceLock);

    if (mAudioCustParamCache == NULL)
    {
        mAudioCustParamCache = new MtkAudioCustParamCache();
    }
    ASSERT(mAudioCustParamCache != NULL);
    return mAudioCustParamCache;
}

MtkAudioCustParamCache::MtkAudioCustParamCache()
{
    mFilterType = AUDIO_COMP_FLT_AUDENH;
    getDefaultAudioCompFltParam((AudioCompFltType_t)mFilterType, &mAudioParam);
}

ACE_ERRID MtkAudioCustParamCache::LoadParameter(uint32_t FilterType, AUDIO_ACF_CUSTOM_PARAM_STRUCT *AudioParam)
{
    Mutex::Autolock _l(mLock);
    //Use cache instead of repeatly reading NVRAM
    if(FilterType == mFilterType)
    {
        memcpy((void *)AudioParam, (void *)&mAudioParam, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
    }
    else
    {
        if (FilterType == AUDIO_COMP_FLT_AUDENH)
            getDefaultAudioCompFltParam((AudioCompFltType_t)FilterType, AudioParam);
        else
            GetAudioCompFltCustParamFromNV((AudioCompFltType_t)FilterType, AudioParam);
        memcpy((void *)&mAudioParam, (void *)AudioParam, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
        mFilterType = FilterType;
    }
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioCustParamCache::SaveParameter(uint32_t FilterType, AUDIO_ACF_CUSTOM_PARAM_STRUCT *AudioParam)
{
    Mutex::Autolock _l(mLock);
    mFilterType = FilterType;
    memcpy((void *)&mAudioParam, (void *)AudioParam, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
    return ACE_SUCCESS;
}

bool MtkAudioLoud::mAudioCompFltXmlRegCallback = false;

MtkAudioLoud::MtkAudioLoud() :
    mPcmFormat(0), mFilterType(0), mWorkMode(0), mTempBufSize(0), mInternalBufSize(0),
    mTempBufSize_cache(0), mInternalBufSize_cache(0), mpTempBuf(NULL), mpInternalBuf(NULL),
    mIsSepLR_Filter(false), bIsZeroCoeff(false), mNoiseFilter(0)
{
    Init();
}

MtkAudioLoud::MtkAudioLoud(uint32_t eFLTtype) :
    mPcmFormat(0), mFilterType(eFLTtype), mWorkMode(0), mTempBufSize(0), mInternalBufSize(0),
    mTempBufSize_cache(0), mInternalBufSize_cache(0), mpTempBuf(NULL), mpInternalBuf(NULL),
    mIsSepLR_Filter(false), bIsZeroCoeff(false), mNoiseFilter(0)
{
    Init();
    SetParameter(BLOUD_PAR_SET_FILTER_TYPE, (void *)((long)eFLTtype));
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    if ( mFilterType == AUDIO_COMP_FLT_DRC_FOR_MUSIC )
    {
        Mutex::Autolock _l(mMusicCountMutex);
        mMusicCount ++;
        //ALOGD("mMusicCount ++ = %d", mMusicCount );
    }
#endif
}
void MtkAudioLoud::Init()
{
    memset(&mBloudHandle, 0, sizeof(BS_HD_Handle));

    memset(&mInitParam, 0, sizeof(BLOUD_HD_InitParam));

    memset(&mAudioParam, 0, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));

    memset(&mV4ToV5Use, 0, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT_FILTER_PARAM));

    memset(&mParamFormatUse, 0, sizeof(BLOUD_HD_IIR_Design_Param));

    mInitParam.pMode_Param = new BLOUD_HD_ModeParam;
    memset(mInitParam.pMode_Param, 0, sizeof(BLOUD_HD_ModeParam));

    mInitParam.pMode_Param->pFilter_Coef_L = new BLOUD_HD_FilterCoef;
    memset(mInitParam.pMode_Param->pFilter_Coef_L, 0, sizeof(BLOUD_HD_FilterCoef));

    mInitParam.pMode_Param->pFilter_Coef_R = new BLOUD_HD_FilterCoef;
    memset(mInitParam.pMode_Param->pFilter_Coef_R, 0, sizeof(BLOUD_HD_FilterCoef));

    mInitParam.pMode_Param->pCustom_Param = new BLOUD_HD_CustomParam;
    memset(mInitParam.pMode_Param->pCustom_Param, 0, sizeof(BLOUD_HD_CustomParam));

    mState = ACE_STATE_INIT;

    if (mAudioCompFltXmlRegCallback == false)
    {
        mAudioCompFltXmlRegCallback = true;
        AudioComFltCustParamInit();
    }

    ALOGD("MtkAudioLoud Constructor\n");
}

MtkAudioLoud::~MtkAudioLoud()
{
    ALOGD("+%s()\n",__FUNCTION__);
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    if ( mFilterType == AUDIO_COMP_FLT_DRC_FOR_MUSIC )
    {
        Mutex::Autolock _l(mMusicCountMutex);
        mMusicCount --;
        //ALOGD("mMusicCount -- = %d", mMusicCount );
    }
#endif
    if(mInitParam.pMode_Param != NULL)
    {
        if( mInitParam.pMode_Param->pFilter_Coef_L != NULL )
        {
            delete mInitParam.pMode_Param->pFilter_Coef_L;
            mInitParam.pMode_Param->pFilter_Coef_L = NULL;
        }
        if( mInitParam.pMode_Param->pFilter_Coef_R != NULL )
        {
            delete mInitParam.pMode_Param->pFilter_Coef_R;
            mInitParam.pMode_Param->pFilter_Coef_R = NULL;
        }
        if( mInitParam.pMode_Param->pCustom_Param != NULL )
        {
            delete mInitParam.pMode_Param->pCustom_Param;
            mInitParam.pMode_Param->pCustom_Param = NULL;
        }
        delete mInitParam.pMode_Param;
        mInitParam.pMode_Param = NULL;
    }

    if(mpTempBuf != NULL)
    {
        delete[] mpTempBuf;
        mpTempBuf = NULL;
    }
    if(mpInternalBuf != NULL)
    {
        delete[] mpInternalBuf;
        mpInternalBuf = NULL;
    }
    ALOGD("-%s()\n",__FUNCTION__);
}

ACE_ERRID MtkAudioLoud::SetParameter(uint32_t paramID, void *param)
{
    ALOGD("+%s(), paramID %d, param 0x%lx\n",__FUNCTION__, paramID, (long)param);
    Mutex::Autolock _l(mLock);
    uint32_t Curparam = (uint32_t)((long)param);
    //Add constraint to limit the use after open.
    switch (paramID)
    {
        case BLOUD_PAR_SET_FILTER_TYPE:
        {
            mFilterType = Curparam;
            break;
        }
        case BLOUD_PAR_SET_WORK_MODE:
        {
            mWorkMode = Curparam;
            switch(mWorkMode)
            {
                case AUDIO_CMP_FLT_LOUDNESS_BASIC:     // basic Loudness mode
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_LOUD_FLT;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_BASIC;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_ENHANCED:     // enhancement(1) Loudness mode
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_LOUD_FLT;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_ENHANCED;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_AGGRESSIVE:     // enhancement(2) Loudness mode
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_LOUD_FLT;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_AGGRESSIVE;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_LITE:     // Only DRC, no filtering
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_NONE;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_BASIC;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_COMP:     // Audio Compensation Filter mode (No DRC)
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_COMP_FLT;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_NONE;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_COMP_BASIC:     // Audio Compensation Filter mode + DRC
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_COMP_FLT;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_BASIC;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_COMP_HEADPHONE:     //HCF
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_COMP_HDP;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_NONE;
                    break;
                case AUDIO_CMP_FLT_LOUDNESS_COMP_AUDENH:
                    mInitParam.pMode_Param->Filter_Mode   = HD_FILT_MODE_AUD_ENH;
                    mInitParam.pMode_Param->Loudness_Mode = HD_LOUD_MODE_NONE;
                    break;
                default:
                    ALOGW("%s() invalide workmode %d\n",__FUNCTION__, mWorkMode);
                    break;
            }
            break;
        }
        case BLOUD_PAR_SET_SAMPLE_RATE:
        {
            mInitParam.Sampling_Rate = Curparam;
            break;
        }
        case BLOUD_PAR_SET_PCM_FORMAT:
        {
            mPcmFormat = Curparam;
            mInitParam.PCM_Format = mPcmFormat;
            break;
        }
        case BLOUD_PAR_SET_CHANNEL_NUMBER:
        {
            uint32_t chNum = Curparam;
            if(chNum > 0 && chNum < 3)
            {  // chnum should be 1 or 2
                mInitParam.Channel = chNum;
            }
            else
            {
                return ACE_INVALIDE_PARAMETER;
            }
            break;
        }

        case BLOUD_PAR_SET_USE_DEFAULT_PARAM:
        {
#if defined(AUDIO_ACF_PARAM_USE_CACHE)
            MtkAudioCustParamCache::getInstance()->LoadParameter(mFilterType, &mAudioParam);
#else
            if (mFilterType == AUDIO_COMP_FLT_AUDENH)
                getDefaultAudioCompFltParam((AudioCompFltType_t)mFilterType, &mAudioParam);
            else
                GetAudioCompFltCustParamFromNV((AudioCompFltType_t)mFilterType, &mAudioParam);
#endif
            copyParam();
            break;
        }
        case BLOUD_PAR_SET_PREVIEW_PARAM:
        {
#if defined(AUDIO_ACF_PARAM_USE_CACHE)
            MtkAudioCustParamCache::getInstance()->SaveParameter(mFilterType, (AUDIO_ACF_CUSTOM_PARAM_STRUCT *)param);
#endif
            memcpy((void *)&mAudioParam, (void *)param, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
            copyParam();
            break;
        }
        case BLOUD_PAR_SET_USE_DEFAULT_PARAM_FORCE_RELOAD:
        {
            if (mFilterType == AUDIO_COMP_FLT_AUDENH)
                getDefaultAudioCompFltParam((AudioCompFltType_t)mFilterType, &mAudioParam);
            else
                GetAudioCompFltCustParamFromNV((AudioCompFltType_t)mFilterType, &mAudioParam);
#if defined(AUDIO_ACF_PARAM_USE_CACHE)
            MtkAudioCustParamCache::getInstance()->SaveParameter(mFilterType, &mAudioParam);
#endif
            copyParam();
            break;
        }
        case BLOUD_PAR_SET_USE_DEFAULT_PARAM_SUB:
        {

#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
            if (mFilterType == AUDIO_COMP_FLT_AUDIO)
            {
#if defined(AUDIO_ACF_PARAM_USE_CACHE)
                MtkAudioCustParamCache::getInstance()->LoadParameter(AUDIO_COMP_FLT_AUDIO_SUB, &mAudioParam);
#else
                GetAudioCompFltCustParamFromNV((AudioCompFltType_t)AUDIO_COMP_FLT_AUDIO_SUB, &mAudioParam);
#endif
                copyParamSub();
            }
#endif
            break;
        }
        case BLOUD_PAR_SET_PREVIEW_PARAM_SUB:
        {
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)

            if (mFilterType == AUDIO_COMP_FLT_AUDIO)
            {
                memcpy((void *)&mAudioParam, (void *)param, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
                copyParamSub();
            }
#endif
            break;
        }
        case BLOUD_PAR_SET_SEP_LR_FILTER:
        {
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
            mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter = mIsSepLR_Filter = (bool)Curparam;
#endif
            break;
        }
        case BLOUD_PAR_SET_STEREO_TO_MONO_MODE:
        {
            mInitParam.pMode_Param->S2M_Mode = Curparam;
            if(mState == ACE_STATE_OPEN)
            {
                BLOUD_HD_RuntimeParam runtime_param;
                runtime_param.Command = BLOUD_HD_CHANGE_MODE;
                runtime_param.pMode_Param = mInitParam.pMode_Param;
                mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
            }
        }
        case BLOUD_PAR_SET_UPDATE_PARAM_TO_SWIP:
        {
            if(mState != ACE_STATE_OPEN)
                return ACE_INVALIDE_OPERATION;
            BLOUD_HD_RuntimeParam runtime_param;
            runtime_param.Command = BLOUD_HD_CHANGE_MODE;
            runtime_param.pMode_Param = (BLOUD_HD_ModeParam *) mInitParam.pMode_Param;
            mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
            break;
        }
        case BLOUD_PAR_SET_RAMP_UP:
       {
           if (Curparam == 0)
               mInitParam.Initial_State = BLOUD_HD_NORMAL_STATE;
           else
               mInitParam.Initial_State = BLOUD_HD_BYPASS_STATE;

           ALOGD("%s %d mInitParam.Initial_State %d",__FUNCTION__,__LINE__,mInitParam.Initial_State);
           break;
       }
        case BLOUD_PAR_SET_NOISE_FILTER:
        {
            if ( Curparam == 1 )
            {
                mNoiseFilter = DRC_NOISEFILTER_MIN;
            }
            else
            {
                mNoiseFilter = 0;
            }
            break;
        }
        default:
            ALOGD("-%s() Error\n",__FUNCTION__);
            return ACE_INVALIDE_PARAMETER;
    }
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::GetParameter(uint32_t paramID, void *param)
{
    ALOGD("+%s(), paramID %d, param %p\n",__FUNCTION__, paramID, param);
    Mutex::Autolock _l(mLock);
    ALOGD("-%s(), paramID %d, param %p\n",__FUNCTION__, paramID, param);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::Open(void)
{
    int32_t result;

    ALOGD("+%s()\n",__FUNCTION__);
    Mutex::Autolock _l(mLock);
    if(mState != ACE_STATE_INIT)
        return ACE_INVALIDE_OPERATION;
#if defined(HAVE_SWIP)
    BLOUD_HD_SetHandle(&mBloudHandle);
#endif
    mBloudHandle.GetBufferSize(&mInternalBufSize, &mTempBufSize, mPcmFormat);

    ALOGD("AudLdSz %d/%d %d/%d fmt %d",mInternalBufSize,mInternalBufSize_cache,mTempBufSize,mTempBufSize_cache,mPcmFormat);

    if( mInternalBufSize > mInternalBufSize_cache )
    {
        if(mpInternalBuf != NULL)
        {
            ALOGD("AudLdSz Del mpInternalBuf");
            delete[] mpInternalBuf;
        }

        mpInternalBuf = new char[mInternalBufSize];
        mInternalBufSize_cache = mInternalBufSize;
        ALOGD("AudLdSz New mpInternalBuf Pass");
    }

    if( mTempBufSize > mTempBufSize_cache )
    {
        if(mpTempBuf != NULL)
        {
            ALOGD("AudLdSz Del mpTempBuf");
            delete[] mpTempBuf;
        }

        mpTempBuf = new char[mTempBufSize];
        mTempBufSize_cache = mTempBufSize;
        ALOGD("AudLdSz New mpTempBuf Pass");
    }

    //ALOGD("Filter_Mode [%d] Loudness_Mode [%d] Pte [%x]",mInitParam.pMode_Param->Filter_Mode,mInitParam.pMode_Param->Loudness_Mode,(int)mInitParam.pMode_Param->pFilter_Coef_L);

    result = mBloudHandle.Open(&mBloudHandle, mpInternalBuf, (const void *)&mInitParam);
    mState = ACE_STATE_OPEN;
    ALOGD("-%s() result %d\n",__FUNCTION__, result);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::Close(void)
{
    ALOGD("+%s()\n",__FUNCTION__);
    Mutex::Autolock _l(mLock);
    if(mState != ACE_STATE_OPEN)
        return ACE_INVALIDE_OPERATION;
#if 0
    if(mpTempBuf != NULL)
    {
        delete[] mpTempBuf;
        mpTempBuf = NULL;
    }
    if(mpInternalBuf != NULL)
    {
        delete[] mpInternalBuf;
        mpInternalBuf = NULL;
    }
#endif
    mState = ACE_STATE_INIT;
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::ResetBuffer(void)
{
    ALOGD("+%s()\n",__FUNCTION__);
    Mutex::Autolock _l(mLock);
    if(mState != ACE_STATE_OPEN)
        return ACE_INVALIDE_OPERATION;
    BLOUD_HD_RuntimeParam runtime_param;
    runtime_param.Command = BLOUD_HD_RESET;
    mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::SetWorkMode(uint32_t chNum, uint32_t smpRate, uint32_t workMode, bool bRampUpEnable)
{
    ACE_ERRID ret;
    ALOGD("+%s()\n",__FUNCTION__);
    if(mState != ACE_STATE_OPEN)
    {
         ALOGD("%s(), chNum %d, sampleRate %d, workMode %d RampupEnable %d\n",__FUNCTION__, chNum, smpRate, workMode,bRampUpEnable);

        if( (ret = SetParameter(BLOUD_PAR_SET_CHANNEL_NUMBER, (void *)((long)chNum))) != ACE_SUCCESS )
            return ret;
        if( (ret = SetParameter(BLOUD_PAR_SET_SAMPLE_RATE, (void *)((long)smpRate))) != ACE_SUCCESS )
            return ret;
        if( (ret = SetParameter(BLOUD_PAR_SET_WORK_MODE, (void *)((long)workMode))) != ACE_SUCCESS )
            return ret;
         if( (ret = SetParameter(BLOUD_PAR_SET_RAMP_UP, (void *)bRampUpEnable)) != ACE_SUCCESS )
            return ret;
    }
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

int MtkAudioLoud::BLOUD_HD_Get_Sampling_Rate_Index(unsigned int sampling_rate)
{
    int sr_idx;

    switch (sampling_rate)
    {
        case 48000:  sr_idx = 0;   break;
        case 44100:  sr_idx = 1;   break;
        case 32000:  sr_idx = 2;   break;
        case 24000:  sr_idx = 3;   break;
        case 22050:  sr_idx = 4;   break;
        case 16000:  sr_idx = 5;   break;
        case 12000:  sr_idx = 6;   break;
        case 11025:  sr_idx = 7;   break;
        case  8000:  sr_idx = 8;   break;
        default:     sr_idx = -1;  break;
    }

    return sr_idx;
}

int MtkAudioLoud::BLOUD_HD_Filter_V4_to_V5_Conversion(unsigned int sampling_rate, AUDIO_ACF_CUSTOM_PARAM_STRUCT_FILTER_PARAM *p_V4, BLOUD_HD_FilterCoef *p_V5)
{
    int result = 0;
    int sr_idx = BLOUD_HD_Get_Sampling_Rate_Index(sampling_rate);


    memset(p_V5->HPF_COEF, 0, 2 * 5 * sizeof(unsigned int));
    memset(p_V5->LPF_COEF, 0,     3 * sizeof(unsigned int));
    memset(p_V5->BPF_COEF, 0, 8 * 6 * sizeof(unsigned int));

    if (p_V4 == NULL || p_V5 == NULL)
    {
        result = -1;
    }
    else if (sr_idx < 0 || sr_idx > 8)
    {
        result = -2;
    }
    else
    {
        int flt_idx;

        for (flt_idx = 0; flt_idx < 2; flt_idx++)
        {
            memcpy(p_V5->HPF_COEF[flt_idx], p_V4->bes_loudness_hsf_coeff[flt_idx][sr_idx], sizeof(unsigned int) * 5);
        }

        if (sr_idx < 6)
        {
            memcpy(p_V5->LPF_COEF, p_V4->bes_loudness_lpf_coeff[sr_idx], sizeof(unsigned int) * 3);

            for (flt_idx = 0; flt_idx < 8; flt_idx++)
            {
                //memcpy(p_V5->BPF_COEF, p_V4->bes_loudness_bpf_coeff[sr_idx], sizeof(unsigned int) * 3);
                memset(p_V5->BPF_COEF[flt_idx], 0, sizeof(unsigned int) * 6);
                memcpy(p_V5->BPF_COEF[flt_idx], p_V4->bes_loudness_bpf_coeff[flt_idx][sr_idx], sizeof(unsigned int) * 3);
            }
        }
    }

    return result;

}

void MtkAudioLoud::UseDefaultFullband( BLOUD_HD_InitParam *pInitParam )
{
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    if ( pInitParam )
    {
        unsigned int Att_Time_Default[6] = {64,64,64,64,64,64};
        unsigned int Rel_Time_Default[6] = {6400,6400,6400,6400,6400,6400};
        int          Hyst_Th_Default[6]  = {256,256,256,256,256,256};
        int          DRC_Th_Default[5]   = {-15360,-12800,-10240,-7680,0,};
        int          DRC_Gn_Default[5]   = {6144,6144,6144,6144,0};

        pInitParam->pMode_Param->pCustom_Param->Num_Bands = 1;
        pInitParam->pMode_Param->pCustom_Param->SB_Mode[0] = 0;
        pInitParam->pMode_Param->pCustom_Param->SB_Gn[0] = 0;
        pInitParam->pMode_Param->pCustom_Param->Lim_Th = 0x7FFF;
        pInitParam->pMode_Param->pCustom_Param->Lim_Gn = 0x7FFF;
        pInitParam->pMode_Param->pCustom_Param->Lim_Const = 4;
        pInitParam->pMode_Param->pCustom_Param->Sep_LR_Filter = 0;

        memcpy((void *)pInitParam->pMode_Param->pCustom_Param->Att_Time, (void *)Att_Time_Default, 6 * sizeof(unsigned int));
        memcpy((void *)pInitParam->pMode_Param->pCustom_Param->Rel_Time, (void *)Rel_Time_Default, 6 * sizeof(unsigned int));
        memcpy((void *)pInitParam->pMode_Param->pCustom_Param->Hyst_Th,  (void *)Hyst_Th_Default,  6 * sizeof(int));
        memcpy((void *)pInitParam->pMode_Param->pCustom_Param->DRC_Th,   (void *)DRC_Th_Default,   5 * sizeof(int));
        memcpy((void *)pInitParam->pMode_Param->pCustom_Param->DRC_Gn,   (void *)DRC_Gn_Default,   5 * sizeof(int));
    }
#endif
}

void MtkAudioLoud::UseNoiseFilter( BLOUD_HD_InitParam *pInitParam )
{
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    if ( pInitParam )
    {
        int NoiseGap1 = NOISE_FILTER_BASE * mNoiseFilter;
        int NoiseGap2 = NOISE_FILTER_BASE * ( mNoiseFilter - NOISE_FILTER_STEP );

        int DRC_Th_Default[2]   = {NoiseGap1,NoiseGap2};
        int DRC_Gn_Default[1]   = {0};

        for (int i = 0; i < pInitParam->pMode_Param->pCustom_Param->Num_Bands; i++)
        {
            if ( pInitParam->pMode_Param->pCustom_Param->DRC_Th[i][0] > DRC_Th_Default[0] )
            {
                pInitParam->pMode_Param->pCustom_Param->DRC_Th[i][0] = DRC_Th_Default[0];
            }
            if ( pInitParam->pMode_Param->pCustom_Param->DRC_Th[i][1] > DRC_Th_Default[1] )
            {
                pInitParam->pMode_Param->pCustom_Param->DRC_Th[i][1] = DRC_Th_Default[1];
            }

            pInitParam->pMode_Param->pCustom_Param->DRC_Gn[i][0] = DRC_Gn_Default[0];
        }
    }
#endif
}

void MtkAudioLoud::copyParam(void)
{
    bool ZeroFlag = true;
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
    int dCheckStatus;
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Max = mAudioParam.bes_loudness_WS_Gain_Max;
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Min = mAudioParam.bes_loudness_WS_Gain_Min;
    mInitParam.pMode_Param->pCustom_Param->Filter_First = mAudioParam.bes_loudness_Filter_First;
    mInitParam.pMode_Param->pCustom_Param->Num_Bands = mAudioParam.bes_loudness_Num_Bands;
    mInitParam.pMode_Param->pCustom_Param->Flt_Bank_Order = mAudioParam.bes_loudness_Flt_Bank_Order;
    mInitParam.pMode_Param->pCustom_Param->DRC_Delay = mAudioParam.DRC_Delay;
    mInitParam.pMode_Param->pCustom_Param->Lim_Th = mAudioParam.Lim_Th;
    mInitParam.pMode_Param->pCustom_Param->Lim_Gn = mAudioParam.Lim_Gn;
    mInitParam.pMode_Param->pCustom_Param->Lim_Const = mAudioParam.Lim_Const;
    mInitParam.pMode_Param->pCustom_Param->Lim_Delay = mAudioParam.Lim_Delay;
    mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter = mIsSepLR_Filter = mAudioParam.bes_loudness_Sep_LR_Filter;
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->Att_Time, (void *)mAudioParam.Att_Time, 48 * sizeof(unsigned int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->Rel_Time, (void *)mAudioParam.Rel_Time, 48 * sizeof(unsigned int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->Cross_Freq, (void *)mAudioParam.bes_loudness_Cross_Freq, 7 * sizeof(unsigned int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->SB_Mode, (void *)mAudioParam.SB_Mode, 8 * sizeof(unsigned int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->SB_Gn, (void *)mAudioParam.SB_Gn, 8 * sizeof(unsigned int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->Hyst_Th, (void *)mAudioParam.Hyst_Th, 48 * sizeof(int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->DRC_Th, (void *)mAudioParam.DRC_Th, 40 * sizeof(int));
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param->DRC_Gn, (void *)mAudioParam.DRC_Gn, 40 * sizeof(int));

    if ( mFilterType == AUDIO_COMP_FLT_DRC_FOR_MUSIC )
    {
        if ( mNoiseFilter!= 0 )
        {
            // Only VOIP will use noise filter.
            // We suppose the track number will not exceed MUSIC_UP_BOUND.
            UseNoiseFilter( &mInitParam );
        }
        else
        {
            Mutex::Autolock _l(mMusicCountMutex);
            if ( mMusicCount > MUSIC_UP_BOUND )
            {
                mMaxMusicCount = MIN_MULTI_BAND_COUNT;
            }
            else if ( mMusicCount < MUSIC_LOW_BOUND )
            {
                mMaxMusicCount = MAX_MULTI_BAND_COUNT;
            }

            if ( mMusicCount > mMaxMusicCount )
            {
                //ALOGD("mMusicCount = %d", mMusicCount );
                UseDefaultFullband( &mInitParam );
            }
        }
    }

    if (mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_hpf_fc != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_hpf_order != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_lpf_fc != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_lpf_order != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_hpf_fc != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_hpf_order != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_lpf_fc != 0
        || mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_lpf_order != 0
        )
    {
            ZeroFlag = false;
    }

    if (ZeroFlag)
    {
        for (int i=0;i<8;i++)
        {
            if (mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_fc[i] != 0
                ||mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_bw[i] != 0
                ||mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_gain[i] != 0
                ||mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_fc[i] != 0
                ||mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_bw[i] != 0
                ||mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_gain[i] != 0
                )
            {
                ZeroFlag = false;
                break;
            }
        }
    }

    bIsZeroCoeff = ZeroFlag;

    if (mFilterType == AUDIO_COMP_FLT_VIBSPK)
    {
        memset((void *)&mV4ToV5Use,0x00,sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT_FILTER_PARAM));
        memcpy((void *)mV4ToV5Use.bes_loudness_bpf_coeff, (void *)mAudioParam.bes_loudness_f_param.V5ViVSPK.bes_loudness_bpf_coeff, 36 * sizeof(unsigned int));
        dCheckStatus = BLOUD_HD_Filter_V4_to_V5_Conversion(mInitParam.Sampling_Rate,&mV4ToV5Use,mInitParam.pMode_Param->pFilter_Coef_L);
        if(dCheckStatus)
        {
              ALOGE("%s %d Return pFilter_Coef_L %d",__FUNCTION__,__LINE__,dCheckStatus);
        }
        dCheckStatus = BLOUD_HD_Filter_V4_to_V5_Conversion(mInitParam.Sampling_Rate,&mV4ToV5Use,mInitParam.pMode_Param->pFilter_Coef_R);
        if(dCheckStatus)
        {
              ALOGE("%s %d Return pFilter_Coef_R %d",__FUNCTION__,__LINE__,dCheckStatus);
        }
    }
    else
    {
        mParamFormatUse.hpf_fc = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_hpf_fc;
        mParamFormatUse.hpf_order = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_hpf_order;
        mParamFormatUse.lpf_fc = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_lpf_fc;
        mParamFormatUse.lpf_order = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_lpf_order;
        memcpy((void *)mParamFormatUse.bpf_fc, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_fc, 8 * sizeof(unsigned int));
        memcpy((void *)mParamFormatUse.bpf_bw, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_bw, 8 * sizeof(unsigned int));
        memcpy((void *)mParamFormatUse.bpf_gain, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_L_bpf_gain, 8 * sizeof(int));

        dCheckStatus =BLOUD_HD_Filter_Design(mInitParam.pMode_Param->Filter_Mode,mInitParam.Sampling_Rate,&mParamFormatUse,mInitParam.pMode_Param->pFilter_Coef_L);

        if(dCheckStatus)
        {
              ALOGE("%s %d Return pFilter_Coef_L %d",__FUNCTION__,__LINE__,dCheckStatus);
        }

        mParamFormatUse.hpf_fc = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_hpf_fc;
        mParamFormatUse.hpf_order = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_hpf_order;
        mParamFormatUse.lpf_fc = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_lpf_fc;
        mParamFormatUse.lpf_order = mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_lpf_order;
        memcpy((void *)mParamFormatUse.bpf_fc, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_fc, 8 * sizeof(unsigned int));
        memcpy((void *)mParamFormatUse.bpf_bw, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_bw, 8 * sizeof(unsigned int));
        memcpy((void *)mParamFormatUse.bpf_gain, (void *)mAudioParam.bes_loudness_f_param.V5F.bes_loudness_R_bpf_gain, 8 * sizeof(int));

        dCheckStatus =BLOUD_HD_Filter_Design(mInitParam.pMode_Param->Filter_Mode,mInitParam.Sampling_Rate,&mParamFormatUse,mInitParam.pMode_Param->pFilter_Coef_R);

        if(dCheckStatus)
        {
              ALOGE("%s %d Return pFilter_Coef_R %d",__FUNCTION__,__LINE__,dCheckStatus);
        }
    }
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF, (void *)mAudioParam.bes_loudness_hsf_coeff_L, 10 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF, (void *)mAudioParam.bes_loudness_bpf_coeff_L, 24 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->LPF_COEF, (void *)mAudioParam.bes_loudness_lpf_coeff_L, 3 * sizeof(unsigned int));

    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF, (void *)mAudioParam.bes_loudness_hsf_coeff_R, 10 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF, (void *)mAudioParam.bes_loudness_bpf_coeff_R, 24 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_R->LPF_COEF, (void *)mAudioParam.bes_loudness_lpf_coeff_R, 3 * sizeof(unsigned int));



#elif defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    int dCheckStatus;
#if 0
    ALOGD("Temp use for V4");
    memcpy((void *)mInitParam.pMode_Param->pCustom_Param, (void *)&audio_musicdrc_fixed_default, sizeof(BLOUD_HD_CustomParam));
#else
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Max = mAudioParam.bes_loudness_WS_Gain_Max;
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Min = mAudioParam.bes_loudness_WS_Gain_Min;
    mInitParam.pMode_Param->pCustom_Param->Filter_First = mAudioParam.bes_loudness_Filter_First;
    mInitParam.pMode_Param->pCustom_Param->Num_Bands = 0;
    mInitParam.pMode_Param->pCustom_Param->Flt_Bank_Order = 0;
    mInitParam.pMode_Param->pCustom_Param->DRC_Delay = 0;
    mInitParam.pMode_Param->pCustom_Param->Lim_Th = 0;
    mInitParam.pMode_Param->pCustom_Param->Lim_Gn = 0;
    mInitParam.pMode_Param->pCustom_Param->Lim_Const = 0;
    mInitParam.pMode_Param->pCustom_Param->Lim_Delay = 0;
    mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter = mIsSepLR_Filter;
    memset((void *)mInitParam.pMode_Param->pCustom_Param->Att_Time, 0x00, 48 * sizeof(unsigned int));
    memset((void *)mInitParam.pMode_Param->pCustom_Param->Rel_Time, 0x00, 48 * sizeof(unsigned int));
    for (int i = 0; i < 6; i++)
    {
        mInitParam.pMode_Param->pCustom_Param->Att_Time[0][i] = mAudioParam.bes_loudness_Att_Time;
        mInitParam.pMode_Param->pCustom_Param->Rel_Time[0][i] = mAudioParam.bes_loudness_Rel_Time;
    }

    memset((void *)mInitParam.pMode_Param->pCustom_Param->Cross_Freq, 0x00, 7 * sizeof(unsigned int));
    memset((void *)mInitParam.pMode_Param->pCustom_Param->SB_Mode, 0x00, 8 * sizeof(unsigned int));
    memset((void *)mInitParam.pMode_Param->pCustom_Param->Hyst_Th, 0x00, 48 * sizeof(int));

    memset((void *)mInitParam.pMode_Param->pCustom_Param->DRC_Th, 0x00, 40 * sizeof(int));
    memset((void *)mInitParam.pMode_Param->pCustom_Param->DRC_Gn, 0x00, 40 * sizeof(int));
    for (int i = 0; i < 5; i++)
    {
        int tmp_gn_in = (int)mAudioParam.bes_loudness_Gain_Map_In[i];
        int tmp_gn_ou = (int)mAudioParam.bes_loudness_Gain_Map_Out[i];
        // Bound into 255 ~ -128
        tmp_gn_in = tmp_gn_in > 255 ? 255 : tmp_gn_in < -128 ? -128 : tmp_gn_in;
        tmp_gn_ou = tmp_gn_ou > 255 ? 255 : tmp_gn_ou < -128 ? -128 : tmp_gn_ou;
        // Mapping 255 ~ 128 to -1 ~ -128
        tmp_gn_in = tmp_gn_in >= 128 ? tmp_gn_in - 256 : tmp_gn_in;
        tmp_gn_ou = tmp_gn_ou >= 128 ? tmp_gn_ou - 256 : tmp_gn_ou;
        mInitParam.pMode_Param->pCustom_Param->DRC_Th[0][i] = tmp_gn_in << 8;
        mInitParam.pMode_Param->pCustom_Param->DRC_Gn[0][i] = tmp_gn_ou << 8;
    }
#endif
    memcpy((void *)mV4ToV5Use.bes_loudness_hsf_coeff, (void *)mAudioParam.bes_loudness_hsf_coeff, 90 * sizeof(unsigned int));
    memcpy((void *)mV4ToV5Use.bes_loudness_bpf_coeff, (void *)mAudioParam.bes_loudness_bpf_coeff, 144 * sizeof(unsigned int));
    memcpy((void *)mV4ToV5Use.bes_loudness_lpf_coeff, (void *)mAudioParam.bes_loudness_lpf_coeff, 18 * sizeof(unsigned int));
    dCheckStatus = BLOUD_HD_Filter_V4_to_V5_Conversion(mInitParam.Sampling_Rate,&mV4ToV5Use,mInitParam.pMode_Param->pFilter_Coef_L);
    if(dCheckStatus)
    {
          ALOGE("%s %d Return pFilter_Coef_R %d",__FUNCTION__,__LINE__,dCheckStatus);
    }
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF, (void *)mAudioParam.bes_loudness_hsf_coeff, 90 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF, (void *)mAudioParam.bes_loudness_bpf_coeff, 144 * sizeof(unsigned int));
    //memcpy((void *)mInitParam.pMode_Param->pFilter_Coef_L->LPF_COEF, (void *)mAudioParam.bes_loudness_lpf_coeff, 18 * sizeof(unsigned int));
    for(int i=0;i<2;i++)
        for(int j=0;j<9;j++)
            for(int k=0;k<5;k++)
        {
            if (mAudioParam.bes_loudness_hsf_coeff[i][j][k])
            {
                ZeroFlag = false;
                break;
            }
        }

    if (ZeroFlag)
    {
        for(int i=0;i<8;i++)
        for(int j=0;j<6;j++)
        for(int k=0;k<3;k++)
        {
            if (mAudioParam.bes_loudness_bpf_coeff[i][j][k])
            {
                ZeroFlag = false;
                break;
            }
        }
    }

    if (ZeroFlag)
    {
        for(int i=0;i<6;i++)
            for(int j=0;j<3;j++)
        {
            if (mAudioParam.bes_loudness_lpf_coeff[i][j])
            {
                ZeroFlag = false;
                break;
            }
        }
    }

    bIsZeroCoeff = ZeroFlag;
#if 0
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Max = mAudioParam.bes_loudness_WS_Gain_Max;
    mInitParam.pMode_Param->pCustom_Param->WS_Gain_Min = mAudioParam.bes_loudness_WS_Gain_Min;
    mInitParam.pMode_Param->pCustom_Param->Filter_First = mAudioParam.bes_loudness_Filter_First;
    mInitParam.pMode_Param->pCustom_Param->Att_Time = mAudioParam.bes_loudness_Att_Time;
    mInitParam.pMode_Param->pCustom_Param->Rel_Time = mAudioParam.bes_loudness_Rel_Time;
    mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter = mIsSepLR_Filter; //0: Use same filter for both L / R
    memcpy((void*)mInitParam.pMode_Param->pCustom_Param->Gain_Map_In, (void*)mAudioParam.bes_loudness_Gain_Map_In, 5*sizeof(char));
    memcpy((void*)mInitParam.pMode_Param->pCustom_Param->Gain_Map_Out, (void*)mAudioParam.bes_loudness_Gain_Map_Out, 5*sizeof(char));
    memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF, (void*)mAudioParam.bes_loudness_hsf_coeff, 90*sizeof(unsigned int));
    memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF, (void*)mAudioParam.bes_loudness_bpf_coeff, 144*sizeof(unsigned int));
    memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_L->LPF_COEF, (void*)mAudioParam.bes_loudness_lpf_coeff, 18*sizeof(unsigned int));

    if (mIsSepLR_Filter == 0)
    {
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF, 0, 90*sizeof(unsigned int));
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF, 0, 144*sizeof(unsigned int));
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->LPF_COEF, 0, 18*sizeof(unsigned int));
    }
#endif
#endif
    if (mIsSepLR_Filter == 0)
    {
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF, 0, 10*sizeof(unsigned int));
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF, 0, 48*sizeof(unsigned int));
        memset((void*)mInitParam.pMode_Param->pFilter_Coef_R->LPF_COEF, 0, 3*sizeof(unsigned int));
    }
/*
    bool ZeroFlag = true;

    for(int i=0;i<2;i++)
        for(int j=0;j<5;j++)
        {
            if (mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[i][j])
            {
                ZeroFlag = false;
                break;
            }
        }

    if (ZeroFlag)
    {
        for(int i=0;i<8;i++)
        for(int j=0;j<6;j++)
        {
            if (mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[i][j])
            {
                ZeroFlag = false;
                break;
            }
        }
    }

    if (ZeroFlag)
    {
        for(int i=0;i<8;i++)
        {
            if (mInitParam.pMode_Param->pFilter_Coef_L->LPF_COEF[i])
            {
                ZeroFlag = false;
                break;
            }
        }
    }

    if (mIsSepLR_Filter)
    {
        for(int i=0;i<2;i++)
        for(int j=0;j<5;j++)
        {
            if (mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[i][j])
            {
                ZeroFlag = false;
                break;
            }
        }

        if (ZeroFlag)
        {
            for(int i=0;i<8;i++)
            for(int j=0;j<6;j++)
            {
                if (mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[i][j])
                {
                    ZeroFlag = false;
                    break;
                }
            }
        }

        if (ZeroFlag)
        {
            for(int i=0;i<8;i++)
            {
                if (mInitParam.pMode_Param->pFilter_Coef_R->LPF_COEF[i])
                {
                    ZeroFlag = false;
                    break;
                }
            }
        }
    }

    bIsZeroCoeff = ZeroFlag;
    */
    ALOGD("bIsZeroCoeff %d mFilterType %d",bIsZeroCoeff,mFilterType);
    ALOGD("Channel %d",mInitParam.Channel);
    ALOGD("Sampling_Rate %d",mInitParam.Sampling_Rate);
    ALOGD("PCM_Format %d",mInitParam.PCM_Format);

    ALOGD("copyParam mIsSepLR_Filter [%d]",mIsSepLR_Filter);
    ALOGD("LHSF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][0]);
    ALOGD("LHSF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][1]);
    ALOGD("LBPF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][0]);
    ALOGD("LBPF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][1]);
    ALOGD("RHSF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][0]);
    ALOGD("RHSF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][1]);
    ALOGD("RBPF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][0]);
    ALOGD("RBPF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][1]);

    ALOGD("WS_Gain_Max %d",mInitParam.pMode_Param->pCustom_Param->WS_Gain_Max);
    ALOGD("WS_Gain_Min %d",mInitParam.pMode_Param->pCustom_Param->WS_Gain_Min);
    ALOGD("Filter_First %d",mInitParam.pMode_Param->pCustom_Param->Filter_First);
    ALOGD("Num_Bands %d",mInitParam.pMode_Param->pCustom_Param->Num_Bands);
    ALOGD("Flt_Bank_Order %d",mInitParam.pMode_Param->pCustom_Param->Flt_Bank_Order);
    ALOGD("DRC_Delay %d",mInitParam.pMode_Param->pCustom_Param->DRC_Delay);
    ALOGD("Lim_Th %d",mInitParam.pMode_Param->pCustom_Param->Lim_Th);
    ALOGD("Lim_Gn %d",mInitParam.pMode_Param->pCustom_Param->Lim_Gn);
    ALOGD("Lim_Const %d",mInitParam.pMode_Param->pCustom_Param->Lim_Const);
    ALOGD("Lim_Delay %d",mInitParam.pMode_Param->pCustom_Param->Lim_Delay);
    ALOGD("Sep_LR_Filter %d",mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter);

    for (int i = 0; i < 6; i++)
    {
        ALOGD("Att_Time[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->Att_Time[0][i]);
        ALOGD("Rel_Time[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->Rel_Time[0][i]);
    }

    for (int i = 0; i < 5; i++)
    {
        ALOGD("DRC_Th[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->DRC_Th[0][i]);
        ALOGD("DRC_Gn[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->DRC_Gn[0][i]);
    }
}

void MtkAudioLoud::copyParamSub(void)
{

#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    int dCheckStatus;

    if (mIsSepLR_Filter)
    {
//        memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF, (void*)mAudioParam.bes_loudness_hsf_coeff, 90*sizeof(unsigned int));
//        memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF, (void*)mAudioParam.bes_loudness_bpf_coeff, 144*sizeof(unsigned int));
//        memcpy((void*)mInitParam.pMode_Param->pFilter_Coef_R->LPF_COEF, (void*)mAudioParam.bes_loudness_lpf_coeff, 18*sizeof(unsigned int));
        memcpy((void *)mV4ToV5Use.bes_loudness_hsf_coeff, (void *)mAudioParam.bes_loudness_hsf_coeff, 90 * sizeof(unsigned int));
        memcpy((void *)mV4ToV5Use.bes_loudness_bpf_coeff, (void *)mAudioParam.bes_loudness_bpf_coeff, 144 * sizeof(unsigned int));
        memcpy((void *)mV4ToV5Use.bes_loudness_lpf_coeff, (void *)mAudioParam.bes_loudness_lpf_coeff, 18 * sizeof(unsigned int));

        dCheckStatus = BLOUD_HD_Filter_V4_to_V5_Conversion(mInitParam.Sampling_Rate,&mV4ToV5Use,mInitParam.pMode_Param->pFilter_Coef_R);
        if(dCheckStatus)
        {
              ALOGE("%s %d Return pFilter_Coef_R %d",__FUNCTION__,__LINE__,dCheckStatus);
        }
    }

    ALOGD("Channel %d",mInitParam.Channel);
    ALOGD("Sampling_Rate %d",mInitParam.Sampling_Rate);
    ALOGD("PCM_Format %d",mInitParam.PCM_Format);

    ALOGD("copyParamSub mIsSepLR_Filter [%d]",mIsSepLR_Filter);
    ALOGD("LHSF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][0]);
    ALOGD("LHSF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_L->HPF_COEF[0][1]);
    ALOGD("LBPF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][0]);
    ALOGD("LBPF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_L->BPF_COEF[0][1]);
    ALOGD("RHSF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][0]);
    ALOGD("RHSF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_R->HPF_COEF[0][1]);
    ALOGD("RBPF_Coeff [0][0]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][0], &mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][0]);
    ALOGD("RBPF_Coeff [0][1]=0x%x, addr = %p,", mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][1], &mInitParam.pMode_Param->pFilter_Coef_R->BPF_COEF[0][1]);
    ALOGD("WS_Gain_Max %d",mInitParam.pMode_Param->pCustom_Param->WS_Gain_Max);
    ALOGD("WS_Gain_Min %d",mInitParam.pMode_Param->pCustom_Param->WS_Gain_Min);
    ALOGD("Filter_First %d",mInitParam.pMode_Param->pCustom_Param->Filter_First);
    ALOGD("Num_Bands %d",mInitParam.pMode_Param->pCustom_Param->Num_Bands);
    ALOGD("Flt_Bank_Order %d",mInitParam.pMode_Param->pCustom_Param->Flt_Bank_Order);
    ALOGD("DRC_Delay %d",mInitParam.pMode_Param->pCustom_Param->DRC_Delay);
    ALOGD("Lim_Th %d",mInitParam.pMode_Param->pCustom_Param->Lim_Th);
    ALOGD("Lim_Gn %d",mInitParam.pMode_Param->pCustom_Param->Lim_Gn);
    ALOGD("Lim_Const %d",mInitParam.pMode_Param->pCustom_Param->Lim_Const);
    ALOGD("Lim_Delay %d",mInitParam.pMode_Param->pCustom_Param->Lim_Delay);
    ALOGD("Sep_LR_Filter %d",mInitParam.pMode_Param->pCustom_Param->Sep_LR_Filter);

    for (int i = 0; i < 6; i++)
    {
        ALOGD("Att_Time[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->Att_Time[0][i]);
        ALOGD("Rel_Time[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->Rel_Time[0][i]);
    }

    for (int i = 0; i < 5; i++)
    {
        ALOGD("DRC_Th[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->DRC_Th[0][i]);
        ALOGD("DRC_Gn[0][%d] = %d",i,mInitParam.pMode_Param->pCustom_Param->DRC_Gn[0][i]);
    }
#endif
}

        /* Return: consumed input buffer size(byte)                             */
ACE_ERRID MtkAudioLoud::Process(void *pInputBuffer,   /* Input, pointer to input buffer */
                     uint32_t *InputSampleCount,        /* Input, length(byte) of input buffer */
                                                        /* Output, length(byte) left in the input buffer after conversion */
                     void *pOutputBuffer,               /* Input, pointer to output buffer */
                     uint32_t *OutputSampleCount)       /* Input, length(byte) of output buffer */
                                                        /* Output, output data length(byte) */
{
    ALOGV("+%s(), inputCnt %d, outputCnt %d\n",__FUNCTION__, *InputSampleCount, *OutputSampleCount);
    Mutex::Autolock _l(mLock);
    uint32_t dOutputMaxBufSize = *OutputSampleCount;
    uint32_t block_size_byte, offset_bit, loop_cnt, i, totalCnt, TotalConsumedSample = 0, TotalOuputSample = 0, ConsumedSampleCount;
    int32_t result;
    if(mState != ACE_STATE_OPEN)
    {
        ALOGD("Error");
        return ACE_INVALIDE_OPERATION;
    }
    //Simplify handle (BLOCK_SIZE x N) Samples
    if( mPcmFormat == BLOUDHD_IN_Q1P15_OUT_Q1P15 ) // 16 bits
    {
        // 2-byte, mono
        if(mInitParam.Channel == 1)
        {
            offset_bit = 1;
        }// 2-byte, L/R
        else
        {
        offset_bit = 2;
    }
    }
    else //32 bits
    {
        if(mInitParam.Channel == 1)
        {// 4-byte, Mono
            offset_bit = 2;
        }
        else
        {
        // 4-byte, L/R
        offset_bit = 3;
    }
    }
    block_size_byte = BLOCK_SIZE * (1<<offset_bit);
    if( ((*InputSampleCount & (block_size_byte - 1)) != 0) || ((*OutputSampleCount & (block_size_byte - 1)) != 0)/* || (*InputSampleCount != *OutputSampleCount)*/)
    {
        ALOGW("-%s(), inputCnt %d, outputCnt %d block_size_byte %d\n",__FUNCTION__, *InputSampleCount, *OutputSampleCount, block_size_byte);
        ASSERT(0);
    }
    loop_cnt = *InputSampleCount / block_size_byte;
    //ALOGV("+%s(), loop_cnt %d, block_size_byte %d, sample %d %d\n",__FUNCTION__, loop_cnt, block_size_byte, *(int *)pInputBuffer, (*(int *)pInputBuffer)>>16);
    for (i = 0; i < loop_cnt; i++)
    {
        ConsumedSampleCount = block_size_byte;
        *OutputSampleCount = block_size_byte;
        if (dOutputMaxBufSize < TotalConsumedSample+ConsumedSampleCount)
        {
            ALOGW("Warning for input [%d] > output [%d], and skip process",*InputSampleCount,dOutputMaxBufSize);
            CALLSTACK();
            break;
        }
#ifdef ENABLE_PROCESS_PROFILING
        struct timespec systemtime_start, systemtime_end;
        unsigned long total_nano_sec=0;
        int rc;
        rc = clock_gettime(CLOCK_THREAD_CPUTIME_ID, &systemtime_start);
#endif
        result = mBloudHandle.Process( &mBloudHandle,
                             mpTempBuf,
                             (int *)(pInputBuffer+TotalConsumedSample),
                             (int *)&ConsumedSampleCount,
                             (int *)(pOutputBuffer + TotalOuputSample),
                             (int *)OutputSampleCount);
#ifdef ENABLE_PROCESS_PROFILING
        rc = clock_gettime(CLOCK_THREAD_CPUTIME_ID, &systemtime_end);
        total_nano_sec = systemtime_end.tv_nsec - systemtime_start.tv_nsec;
        ALOGD("FLT[%d] nano [%d] SampleCount [%d] Ch [%d] SP [%d] PCM [%d]",mFilterType,total_nano_sec,ConsumedSampleCount,mInitParam.Channel,mInitParam.Sampling_Rate,mInitParam.PCM_Format);
#endif
        ALOGV("result [%d] ConsumedSampleCount [%d] i [%d] loop_cnt [%d]",result,ConsumedSampleCount,i,loop_cnt);
        TotalConsumedSample += ConsumedSampleCount;

        TotalOuputSample += *OutputSampleCount;
    }
    //ALOGV("+%s(), result = %d, loop_cnt %d, block_size_byte %d, sample %d %d\n",__FUNCTION__, result, loop_cnt, block_size_byte, *(int *)pOutputBuffer, (*(int *)pOutputBuffer)>>16);
    *OutputSampleCount = TotalOuputSample;
    *InputSampleCount = TotalConsumedSample;
    ALOGV("-%s(), inputCnt %d, outputCnt %d\n",__FUNCTION__, *InputSampleCount, *OutputSampleCount);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::Change2ByPass(void)
{
    ALOGD("+%s()\n",__FUNCTION__);
    Mutex::Autolock _l(mLock);
    if(mState != ACE_STATE_OPEN)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }

    BLOUD_HD_RuntimeStatus runtime_status;

    if(mBloudHandle.GetStatus(&mBloudHandle, &runtime_status)<0)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }
    else if (runtime_status.State == BLOUD_HD_SWITCHING_STATE)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }

    BLOUD_HD_RuntimeParam runtime_param;
    runtime_param.Command = BLOUD_HD_TO_BYPASS_STATE;
    mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::Change2Normal(void)
{
    ALOGD("+%s()\n",__FUNCTION__);
    Mutex::Autolock _l(mLock);
    if(mState != ACE_STATE_OPEN)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }
    BLOUD_HD_RuntimeStatus runtime_status;

    if(mBloudHandle.GetStatus(&mBloudHandle, &runtime_status)<0)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }
    else if (runtime_status.State == BLOUD_HD_SWITCHING_STATE)
    {
        ALOGW("-%s() Line [%d]\n",__FUNCTION__,__LINE__);
        return ACE_INVALIDE_OPERATION;
    }
    BLOUD_HD_RuntimeParam runtime_param;
    runtime_param.Command = BLOUD_HD_TO_NORMAL_STATE;
    mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
    ALOGD("-%s()\n",__FUNCTION__);
    return ACE_SUCCESS;
}

int MtkAudioLoud::getBesSoundVer(void)
{
#if defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
        const int ParameterVer = 4;
#elif defined(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
        const int ParameterVer = 5;
#else
        const int ParameterVer = -1;
#endif

#if defined(HAVE_SWIP)
        BS_HD_Handle mTempBloudHandle;
        BS_HD_EngineInfo mEngineInfo;
        memset((void*)(&mEngineInfo),0x00,sizeof(BS_HD_EngineInfo));
        BLOUD_HD_SetHandle(&mTempBloudHandle);
        mTempBloudHandle.GetEngineInfo(&mEngineInfo);
        ALOGD("BesSound Ver : 0x%x",mEngineInfo.Version);
        ALOGD("BesSound FlushOutSampleCount : 0x%x",mEngineInfo.FlushOutSampleCount);
        ALOGD("NvRam Format  : V%d",ParameterVer);
        return mEngineInfo.Version;
#else
        ALOGD("BesSound Ver : 0 [Unsupport]");
        ALOGD("NvRam Format  : V%d",ParameterVer);
        return 0;
#endif
}

bool MtkAudioLoud::IsZeroCoeffFilter(void)
{
    return bIsZeroCoeff;
}

ACE_ERRID MtkAudioLoud::SetOutputGain(int32_t gain, uint32_t ramp_sample_cnt)
{
    ALOGD("+%s() gain %d rampeSample %d\n",__FUNCTION__, gain, ramp_sample_cnt);
    Mutex::Autolock _l(mLock);
    if(mState == ACE_STATE_OPEN)
    {
        BLOUD_HD_RuntimeParam runtime_param;
        runtime_param.Command = BLOUD_HD_SET_OUTPUT_GAIN;
        runtime_param.pMode_Param = mInitParam.pMode_Param;
        runtime_param.pMode_Param->pFilter_Coef_L->output_gain = gain;
        runtime_param.pMode_Param->pFilter_Coef_L->ramp_smpl_cnt = ramp_sample_cnt;
        runtime_param.pMode_Param->pFilter_Coef_R->output_gain = gain;
        runtime_param.pMode_Param->pFilter_Coef_R->ramp_smpl_cnt = ramp_sample_cnt;
        mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
    }
    return ACE_SUCCESS;
}

ACE_ERRID MtkAudioLoud::SetNotchFilterParam(uint32_t fc, uint32_t bw, int32_t th)
{
    ALOGD("+%s() fc= %d bw= %d th= %d, state %d\n",__FUNCTION__, fc, bw, th, mState);
    Mutex::Autolock _l(mLock);
    if(mState == ACE_STATE_OPEN)
    {
        BLOUD_HD_RuntimeParam runtime_param;
        runtime_param.Command = BLOUD_HD_CHANGE_NOTCH;
        runtime_param.pMode_Param = mInitParam.pMode_Param;
        runtime_param.pMode_Param->pFilter_Coef_L->notch_fc = fc;
        runtime_param.pMode_Param->pFilter_Coef_L->notch_bw = bw;
        runtime_param.pMode_Param->pFilter_Coef_L->notch_th = th;
        runtime_param.pMode_Param->pFilter_Coef_R->notch_fc = fc;
        runtime_param.pMode_Param->pFilter_Coef_R->notch_bw = bw;
        runtime_param.pMode_Param->pFilter_Coef_R->notch_th = th;
        mBloudHandle.SetParameters(&mBloudHandle, &runtime_param);
    }
    return ACE_SUCCESS;
}

#else
MtkAudioLoud::MtkAudioLoud(){};
MtkAudioLoud::MtkAudioLoud(uint32_t eFLTtype){};
void MtkAudioLoud::Init(){};
MtkAudioLoud::~MtkAudioLoud(){};
ACE_ERRID MtkAudioLoud::SetParameter(uint32_t paramID, void *param){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::GetParameter(uint32_t paramID, void *param){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::Open(void){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::Close(void){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::ResetBuffer(void){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::SetWorkMode(uint32_t chNum, uint32_t smpRate, uint32_t workMode, bool bRampUpEnable){return ACE_SUCCESS;}
int MtkAudioLoud::getBesSoundVer(void){return 0;};
void MtkAudioLoud::copyParam(void){};
void MtkAudioLoud::copyParamSub(void){};
ACE_ERRID MtkAudioLoud::Process(void *pInputBuffer,   /* Input, pointer to input buffer */
                     uint32_t *InputSampleCount,        /* Input, length(byte) of input buffer */
                                                        /* Output, length(byte) left in the input buffer after conversion */
                     void *pOutputBuffer,               /* Input, pointer to output buffer */
                     uint32_t *OutputSampleCount)       /* Input, length(byte) of output buffer */
                                                        /* Output, output data length(byte) */
{return ACE_SUCCESS;}

ACE_ERRID MtkAudioLoud::SetOutputGain(int32_t gain, uint32_t ramp_sample_cnt){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::SetNotchFilterParam(uint32_t fc, uint32_t bw, int32_t th){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::Change2ByPass(void){return ACE_SUCCESS;}
ACE_ERRID MtkAudioLoud::Change2Normal(void){return ACE_SUCCESS;}
int BLOUD_HD_Get_Sampling_Rate_Index(unsigned int sampling_rate){return -1;}
int BLOUD_HD_Filter_V4_to_V5_Conversion(unsigned int sampling_rate, AUDIO_ACF_CUSTOM_PARAM_STRUCT_FILTER_PARAM *p_V4, BLOUD_HD_FilterCoef *p_V5);
int MtkAudioLoud::bIsZeroCoeffFilter(void){return false;}

#endif

}//namespace android

