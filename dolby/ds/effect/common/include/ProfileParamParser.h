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

#ifndef DOLBY_PROFILE_PARAM_PARSER_H_
#define DOLBY_PROFILE_PARAM_PARSER_H_

#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include "DapParams.h"
#include "EffectParamParser.h"

namespace dolby {

using namespace android;

enum DsProfileId
{
    PROFILE_INVALID = -1,
    PROFILE_MOVIE = 0,
    PROFILE_MUSIC,
    PROFILE_GAME,
    PROFILE_VOICE,
    PROFILE_CUSTOM_1,
    PROFILE_CUSTOM_2,
    PROFILE_OFF
};

enum DsOffType
{
    DS_OFF_TYPE_BYPASSED = 0,
    DS_OFF_TYPE_PARAMETERIZED = 1
};

class ProfileParamParser
{
public:
    ProfileParamParser(void *buffer, int size);

    bool validate();
    bool begin();
    bool next();

    int numberOfParams() const
    { return mNumParams; }

    DsProfileId profileId() const
    { return static_cast<DsProfileId>(mProfileId); }

    DapParameterId paramId() const
    { return static_cast<DapParameterId>(mCurrentParamId); }

    int length() const
    { return mCurrentParamLen; }

    const dap_param_value_t *values() const
    { return mCurrentParamData; }

protected:
    ParserBuffer mPbuf;
    int32_t mProfileId;
    int32_t mNumParams;
    int32_t mCurrentParamIdx;
    int32_t mCurrentParamId;
    int32_t mCurrentParamLen;
    dap_param_value_t *mCurrentParamData;
};

}
#endif //DOLBY_PROFILE_PARAM_PARSER_H_
