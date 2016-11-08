/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#define LOG_TAG "JPGEffectHal"
#include <cutils/log.h>
#include "include/JPGEffectHal.h"

/******************************************************************************
 *
 ******************************************************************************/
#define FUNCTION_LOG_START      ALOGD("[%s] - E.", __FUNCTION__)
#define FUNCTION_LOG_END        ALOGD("[%s] - X.", __FUNCTION__)

using namespace NSCam;
using namespace android;

JPGEffectHal::
JPGEffectHal() :
    mpisabort(MFALSE),
    mpSDKJPG(NULL)
{
}

JPGEffectHal::
~JPGEffectHal()
{
}

bool
JPGEffectHal::
allParameterConfigured()
{
    FUNCTION_LOG_START;
    //@todo implement this
    FUNCTION_LOG_END;
    return true;
}


status_t
JPGEffectHal::
initImpl()
{
    FUNCTION_LOG_START;
    //@todo implement this

    mpInputFrame = NULL;
    mpOutputFrame = NULL;
    mpSDKJPG  = NULL;

    mpisabort = MFALSE;

    mpSDKJPG = new SDK_jpg();

    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
uninitImpl()
{
    FUNCTION_LOG_START;
    //@todo implement this
    delete  mpSDKJPG;
    mpSDKJPG = NULL;
    FUNCTION_LOG_END;
    return OK;
}


//non-blocking
status_t
JPGEffectHal::
prepareImpl()
{
    FUNCTION_LOG_START;
    //@todo implement this
    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
releaseImpl()
{
    FUNCTION_LOG_START;
    //@todo implement this
    FUNCTION_LOG_END;
    return OK;
}

status_t
JPGEffectHal::
getNameVersionImpl(EffectHalVersion &nameVersion) const
{
    FUNCTION_LOG_START;
    //@todo implement this
    nameVersion.effectName = "Effect_HAL_JPG";
    nameVersion.major = 1;
    nameVersion.minor = 0;
    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
getCaptureRequirementImpl(EffectParameter *inputParam, Vector<EffectCaptureRequirement> &requirements) const
{
    FUNCTION_LOG_START;
    //@todo implement this
    requirements.clear();
    requirements.setCapacity(1);
    EffectCaptureRequirement req;
    char str[32];
    ALOGD("[%s] picture size %dx%d",__FUNCTION__,mParameter->getInt("picture-width"),mParameter->getInt("picture-height") );
    sprintf(str, "%dx%d", mParameter->getInt("picture-height"), mParameter->getInt("picture-width"));
    req.set("picture-size", str);
    req.set("picture-format", eImgFmt_YV12);
    requirements.push_back(req);
    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
setParameterImpl(android::String8 &key, android::String8 &object)
{
    FUNCTION_LOG_START;
    //@todo implement this
    FUNCTION_LOG_END;
    return OK;
}

status_t
JPGEffectHal::
setParametersImpl(android::sp<EffectParameter> parameter)
{
    FUNCTION_LOG_START;
    parameter->dump();
    mParameter = parameter;
    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
startImpl(uint64_t *uid)
{
    FUNCTION_LOG_START;
    //@todo implement this
    mpisabort = MFALSE;

    mpSDKJPG->init();
    setCallback();

    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
abortImpl(EffectResult &result, EffectParameter const *parameter)
{
    FUNCTION_LOG_START;
    //@todo implement this
    mpisabort = MTRUE;

    if(mpInputFrame != NULL)
    {
      ALOGD("[%s] release mpInputFrame", __FUNCTION__);
      mpInputFrame->unlockBuf( "addInputFrame");
      mpInputFrame = NULL;
    }

    if(mpOutputFrame != NULL)
    {
      ALOGD("[%s] release mpOutputFrame", __FUNCTION__);
      mpOutputFrame->unlockBuf( "addInputFrame");
      mpOutputFrame = NULL;
    }

    if(mOutputParameter != NULL)
    {
      ALOGD("[%s] release mOutputParameter", __FUNCTION__);
      mOutputParameter = NULL;
    }

    if(mInputParameter != NULL)
    {
      ALOGD("[%s] release mInputParameter", __FUNCTION__);
      mInputParameter = NULL;
    }

    if(mpRequest != NULL)
    {
      ALOGD("[%s] release mpRequest", __FUNCTION__);
      mpRequest = NULL;
    }

    result.set("onAborted", 1);
    FUNCTION_LOG_END;
    return OK;
}

status_t
JPGEffectHal::
doJPG()
{
    FUNCTION_LOG_START;

    if (!mpSDKJPG->JPG_apply(mpInputFrame.get(), mpOutputFrame.get()))
    {
        ALOGE("[%s] JPG_apply Error!, directly return.", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    JPGSDKCbFunc();

    FUNCTION_LOG_END;
    return OK;
}

status_t
JPGEffectHal::
setCallback()
{
    return OK;
}

status_t
JPGEffectHal::
JPGSDKCbFunc()
{
    FUNCTION_LOG_START;

    mpInputFrame->unlockBuf( "addInputFrame");
    mpOutputFrame->unlockBuf( "addOutputFrame");
    mpOutputFrame = NULL;
    mpInputFrame = NULL;
    mOutputParameter = NULL;
    mInputParameter = NULL;


    sp<EffectResult> iFrameResult = new EffectResult();
    sp<EffectResult> oFrameResult = new EffectResult();
    sp<EffectResult> requestResult = new EffectResult();
    android::sp<EffectFrameInfo> iFrameInfo = mpRequest->vInputFrameInfo.valueAt(0);
    android::sp<EffectFrameInfo> oFrameInfo = mpRequest->vOutputFrameInfo.valueAt(0);

    iFrameResult->set("addInputFrameImpl", "1");
    iFrameInfo->setFrameResult(iFrameResult);
    iFrameInfo->mpOnFrameProcessed(iFrameInfo->mpTag, iFrameInfo);


    oFrameResult->set("addOutputFrameImpl", "1");
    oFrameInfo->setFrameResult(oFrameResult);
    oFrameInfo->mpOnFrameProcessed(oFrameInfo->mpTag, oFrameInfo);

    if(mpisabort)
    {
        ALOGD("[%s] AP will call abort before doing CFB", __FUNCTION__);
        mpisabort = MFALSE;

    } else {
        ALOGD("[%s] call start done after doing CFB", __FUNCTION__);

        requestResult->set("onCompleted", 1);
        mpRequest->setRequestResult(requestResult);
        mpRequest->mpOnRequestProcessed(mpRequest->mpTag, String8("onCompleted"), mpRequest);
    }
    FUNCTION_LOG_END;
    return OK;
}


status_t
JPGEffectHal::
updateEffectRequestImpl(const android::sp<EffectRequest> request)
{
    FUNCTION_LOG_START;
    status_t ret = OK;
    mpRequest = request;
    if (request->vOutputFrameInfo.size() == 0 || request->vInputFrameInfo.size() == 0)
    {
        ALOGD("[%s], request input or output frame empty, directly return", __FUNCTION__);
        return OK;
    }
    android::sp<EffectFrameInfo> oFrameInfo = request->vOutputFrameInfo.valueAt(0);
    android::sp<EffectFrameInfo> iFrameInfo = request->vInputFrameInfo.valueAt(0);
    if (oFrameInfo != NULL && iFrameInfo != NULL)
    {
        ALOGD("[%s] oFrameInfo != NULL && iFrameInfo != NULL", __FUNCTION__);
        ret = iFrameInfo->getFrameBuffer(mpInputFrame);
        ret = oFrameInfo->getFrameBuffer(mpOutputFrame);
        if (mpOutputFrame != NULL && mpInputFrame != NULL)
        {

            if ((mpInputFrame->getImgFormat() != eImgFmt_I422) &&
                (mpInputFrame->getImgFormat() != eImgFmt_YV12) &&
                (mpInputFrame->getImgFormat() != eImgFmt_RGBA8888) &&
                (mpInputFrame->getImgFormat() != eImgFmt_BLOB))
            {
                ALOGE("[%s] InputFrameFmt = %d, Bad value, return", __FUNCTION__, mpInputFrame->getImgFormat());
                return BAD_VALUE;
            }
            if ((mpOutputFrame->getImgFormat() != eImgFmt_I422) &&
                (mpOutputFrame->getImgFormat() != eImgFmt_YV12) &&
                (mpOutputFrame->getImgFormat() != eImgFmt_RGBA8888) &&
                (mpOutputFrame->getImgFormat() != eImgFmt_BLOB))
            {
                ALOGE("[%s] OutputFrameFmt = %d, Bad value, return", __FUNCTION__, mpOutputFrame->getImgFormat());
                return BAD_VALUE;
            }


            mOutputParameter = oFrameInfo->getFrameParameter();
            mInputParameter = iFrameInfo->getFrameParameter();
            ALOGD("[%s] mpOutputFrame != NULL && mpInputFrame != NULL", __FUNCTION__);
            FUNCTION_LOG_END;
            return doJPG();
        }
    }


    FUNCTION_LOG_END;
    return OK;
}
