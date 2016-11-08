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
#define LOG_TAG "DlbDapParamCache"

#include "DlbLog.h"
#include "DapParamCache.h"

namespace dolby {

using namespace android;

DapParamCache::Values::Values()
{
    ALOGV("%s()", __FUNCTION__);
    mLength = 0;
    mData = NULL;
}

DapParamCache::Values::~Values()
{
    ALOGV("%s()", __FUNCTION__);
    delete[] mData;
}

void DapParamCache::Values::set(const dap_param_value_t* values, int len)
{
    ALOGV("%s(values=%p, len=%d)", __FUNCTION__, values, len);

    if (len > mLength)
    {
        delete[] mData;
        mData = new dap_param_value_t[len];
    }
    memcpy(mData, values, len * sizeof(dap_param_value_t));
    mLength = len;
}

int DapParamCache::Values::get(dap_param_value_t* values, int len) const
{
    ALOGV("%s(values=%p, len=%d)", __FUNCTION__, values, len);

    if (mLength < len)
    {
        len = mLength;
    }
    memcpy(values, mData, len * sizeof(dap_param_value_t));
    return len;
}

DapParamCache::~DapParamCache()
{
    ALOGV("%s()", __FUNCTION__);

    size_t size = mParams.size();
    for (size_t i = 0; i < size; ++i)
    {
        delete mParams.valueAt(i);
    }
}

status_t DapParamCache::set(DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, length).string());

    Cache* cache = get(param);
    if (cache == NULL)
    {
        cache = new Cache();
        mParams.add(param, cache);
    }
    cache->values.set(values, length);
    cache->modified = true;
    return NO_ERROR;
}

status_t DapParamCache::get(DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGV("%s(%s)", __FUNCTION__, dapParamNameValue(param, values, *length).string());

    Cache* cache = get(param);
    if (cache == NULL)
    {
        return NAME_NOT_FOUND;
    }
    *length = cache->values.get(values, *length);
    return NO_ERROR;
}

DapParamCache::Cache* DapParamCache::get(DapParameterId param)
{
    ALOGV("%s(param=%s)", __FUNCTION__, dapParamName(param).string());

    int idx = mParams.indexOfKey(param);
    if (idx < 0)
    {
        return NULL;
    }
    return mParams.valueAt(idx);
}

}
