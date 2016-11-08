/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "DlbEffectParamParser"

#include "DlbLog.h"
#include "EffectParamParser.h"

namespace dolby {

/**
    Initialize a parse buffer object with given buffer.

    If the provide buffer is NULL, value for size is forced
    to zero.
*/
ParserBuffer::ParserBuffer(void *buffer, int size)
{
    mBuffer = reinterpret_cast<uint8_t*>(buffer);
    mSize = (buffer == NULL) ? 0 : size;
    reset();
}

/**
    Skip given number of bytes in the stream.

    This function will try to skip given number of bytes in the input stream.
    If this puts mIndex beyond the end of buffer then no action is taken
    and false is returned.
*/
bool ParserBuffer::skip(int numBytes)
{
    int next_index = mIndex + numBytes;
    if (next_index > mSize)
    {
        return false;
    }
    mIndex = next_index;
    return true;
}

/**
    Initialize a EffectParamParser object with given buffer.
*/
EffectParamParser::EffectParamParser(void *buffer, int size)
    : mPbuf(buffer, size)
{
    ALOGV("%s()", __FUNCTION__);

    mActiveDeviceId = 0;
    mNumParams = 0;
    mCurrentParamIdx = 0;
    mCurrentDeviceId = 0;
    mCurrentParamId = 0;
    mCurrentParamLen = 0;
    mCurrentParamData = NULL;
}

/**
    Returns true if input buffer has properly formatted data.

    This function iterates over the entire buffer and verifies that
    the header and all parameters are specified correctly. This function
    must be called before starting iteration since it will reset the
    iteration sequence.
*/
bool EffectParamParser::validate()
{
    ALOGV("%s()", __FUNCTION__);
    // If begin returns false then header is not correct.
    if (!begin())
    {
        ALOGE("%s() EFFECT_PARAM_SET_VALUES header is incorrect.", __FUNCTION__);
        return false;
    }
    ALOGV("%s() Found active device %x with %d parameters",
        __FUNCTION__, mActiveDeviceId, mNumParams);
    // Iterate through all parameters
    while (next())
    {
        ALOGV("%s() Found for device %x parameter %s", __FUNCTION__,
            deviceId(), dapParamNameValue(paramId(), values(), length()).string());
    }

    ALOGE_IF((mCurrentParamIdx != mNumParams),
        "%s() Expected %d parameters, found %d parameters.",
        __FUNCTION__, mNumParams, mCurrentParamIdx);
    // Return true if iteration was performed over all parameters.
    return (mCurrentParamIdx == mNumParams);
}

/**
    Starts a new iteration.

    Returns true if the message header is correct. This function resets
    iteration to the beginning of stream. Function next() must be called
    after this function returns to get to the first parameter.
*/
bool EffectParamParser::begin()
{
    ALOGV("%s()", __FUNCTION__);
    mPbuf.reset();
    mNumParams = 0;
    mCurrentParamIdx = 0;
    // Active device Id & number of parameters
    if (!mPbuf.extract(&mActiveDeviceId))
    {
        ALOGE("%s() failed to extract active device id", __FUNCTION__);
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
    Go to next available parameter return false if no more parameters are available.
*/
bool EffectParamParser::next()
{
    ALOGV("%s()", __FUNCTION__);
    if (mCurrentParamIdx >= mNumParams)
    {
        return false;
    }
    ++mCurrentParamIdx;
    if (!mPbuf.extract(&mCurrentDeviceId))
    {
        ALOGE("%s() failed to extract device id for parameter #%d",
            __FUNCTION__, mCurrentParamIdx);
        return false;
    }
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
