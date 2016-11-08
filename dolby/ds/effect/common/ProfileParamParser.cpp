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

#define LOG_TAG "DlbProfileParamParser"
#include "ProfileParamParser.h"

namespace dolby {

using namespace android;

/**
    Create a streaming parser for profile parameters.
*/
ProfileParamParser::ProfileParamParser(void *buffer, int size)
    : mPbuf(buffer, size)
{
    ALOGV("%s()", __FUNCTION__);
    mProfileId = 0;
    mNumParams = 0;
    mCurrentParamIdx = 0;
    mCurrentParamId = 0;
    mCurrentParamLen = 0;
    mCurrentParamData = NULL;
}

/**
    Returns true if profile definition data is in correct format.
*/
bool ProfileParamParser::validate()
{
    ALOGV("%s()", __FUNCTION__);

    // If begin returns false then header is not correct.
    if (!begin())
    {
        ALOGE("%s() EFFECT_PARAM_DEFINE_PROFILE header is incorrect.", __FUNCTION__);
        return false;
    }
    ALOGV("%s() Found profile id %x with %d parameters",
        __FUNCTION__, mProfileId, mNumParams);
    // Iterate through all parameters
    while (next())
    {
        ALOGV("%s() Found for parameter %s", __FUNCTION__,
            dapParamNameValue(paramId(), values(), length()).string());
    }

    ALOGE_IF((mCurrentParamIdx != mNumParams),
        "%s() Expected %d parameters, found %d parameters.",
        __FUNCTION__, mNumParams, mCurrentParamIdx);
    // Return true if iteration was performed over all parameters.
    return (mCurrentParamIdx == mNumParams);
}

/**
    Consumes the header of profile parameter data and returns true on success.
*/
bool ProfileParamParser::begin()
{
    ALOGV("%s()", __FUNCTION__);

    mPbuf.reset();
    mNumParams = 0;
    mCurrentParamIdx = 0;
    // Active device Id & number of parameters
    if (!mPbuf.extract(&mProfileId))
    {
        ALOGE("%s() failed to extract profile id", __FUNCTION__);
        return false;
    }
    if(!mPbuf.extract(&mNumParams))
    {
        ALOGE("%s() failed to extract number of parameters", __FUNCTION__);
        return false;
    }
    return true;
}

/**
    Consumes one profile parameter and returns true on success.
*/
bool ProfileParamParser::next()
{
    ALOGV("%s()", __FUNCTION__);
    if (mCurrentParamIdx >= mNumParams)
    {
        return false;
    }
    ++mCurrentParamIdx;
    if (!mPbuf.extract(&mCurrentParamId))
    {
        ALOGE("%s() failed to extract parameter id for parameter #%d",
            __FUNCTION__, mCurrentParamIdx);
        return false;
    }
    if (!mPbuf.extract(&mCurrentParamLen))
    {
        ALOGE("%s() failed to extract number of values for parameter #%d",
            __FUNCTION__, mCurrentParamIdx);
        return false;
    }
    if (!mPbuf.consume(&mCurrentParamData, mCurrentParamLen))
    {
        ALOGE("%s() failed to extract %d values for parameter #%d",
            __FUNCTION__, mCurrentParamLen, mCurrentParamIdx);
        return false;
    }
    return true;
}

}
