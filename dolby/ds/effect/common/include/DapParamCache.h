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

#ifndef DOLBY_DAP_PARAM_CACHE_H_
#define DOLBY_DAP_PARAM_CACHE_H_

#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include "DapParams.h"

namespace dolby {

using namespace android;

/**
    This class provides a cache for holding multiple DAP parameters and their values.

    This class can be used to cache a set of DAP parameters with extra metadata
    indicating when the parameter value has changed.
*/
class DapParamCache
{
public:
    class Values
    {
    public:
        Values();
        ~Values();

        int length() const
        { return mLength; }

        const dap_param_value_t *data() const
        { return mData; }

        void set(const dap_param_value_t* values, int len);
        int get(dap_param_value_t* values, int len) const;

    protected:
        int mLength;
        dap_param_value_t *mData;

        Values(const Values&);
        Values& operator=(const Values&);
    };

    struct Cache
    {
        bool modified;
        Values values;
    };

    typedef KeyedVector<DapParameterId, Cache*> Params;

    class Iterator
    {
    public:
        Iterator(Params &params) : mParams(params)
        { mIndex = 0; }

        bool finished()
        { return mIndex >= static_cast<int>(mParams.size()); }

        void next()
        { ++mIndex; }

        DapParameterId param() const
        { return mParams.keyAt(mIndex); }

        bool isModified() const
        { return mParams.valueAt(mIndex)->modified; }

        void clearModified()
        { mParams.valueAt(mIndex)->modified = false; }

        Values *values()
        { return &(mParams.valueAt(mIndex)->values); }

    protected:
        Params &mParams;
        int mIndex;
    };

    ~DapParamCache();

    size_t size()
    { return mParams.size(); }

    status_t set(DapParameterId param, const dap_param_value_t* values, int length);
    status_t get(DapParameterId param, dap_param_value_t* values, int* length);
    Cache* get(DapParameterId param);

    Iterator getIterator()
    { return Iterator(mParams); }

protected:
    Params mParams;
};

}
#endif//DOLBY_DAP_PARAM_CACHE_H_
