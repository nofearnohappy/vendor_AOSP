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

#ifndef DOLBY_EFFECT_PARAM_PARSER_H_
#define DOLBY_EFFECT_PARAM_PARSER_H_

#include <system/audio.h>
#include "DapParams.h"

namespace dolby {

/**
    Class to parse a buffer by interpreting contents as data types.

    This class is a safe way to get objects from a raw byte array
    containing memory representation of the object. This class
    represents a memory buffer as a stream containing objects.

    The memory buffer passed to the constructor is not managed by
    this class. So the code that creates an object of this class
    must ensure that the buffer remains valid for the duration of
    the created object.
*/
class ParserBuffer
{
public:
    ParserBuffer(void *buffer, int size);

    void reset()
    { mIndex = 0; }

    int size() const
    { return mSize; }

    int remaining() const
    { return mSize - mIndex; }

    bool finished() const
    { return mIndex >= mSize; }

    bool skip(int numBytes);

    template <typename T>
    bool extract(T* value);

    template <typename T>
    bool consume(T** value, int len);

    template <typename T>
    bool consume(T** value)
    { return consume<T>(value, 1); }

protected:
    uint8_t *mBuffer;
    int mSize;
    int mIndex;
};

/**
    Extract & copy an object of type T from input stream into value.

    Returns true if *value was updated. Since this function copies
    data from stream, it should only be used for primitive types
    (e.g. int, long, bool, etc) where using a pointer is an overhead.
*/
template <typename T>
bool ParserBuffer::extract(T* value)
{
    int cur_index = mIndex;
    if (skip(sizeof(T)))
    {
        *value = *(reinterpret_cast<T*>(mBuffer + cur_index));
        return true;
    }
    return false;
}

/**
    Return a pointer to given number of object of type T from input stream.

    Returns true of *value was updated. This function returns a pointer
    within input stream that is capable of holding \p len number of
    objects of type T.

    The returned pointer becomes invalid when the memory buffer passed to
    the constructor is destroyed. This function should be used to retrieve
    structures and arrays from the input stream.
*/
template <typename T>
bool ParserBuffer::consume(T** value, int len)
{
    int cur_index = mIndex;
    if (skip(sizeof(T[len])))
    {
        *value = reinterpret_cast<T*>(mBuffer + cur_index);
        return true;
    }
    return false;
}

/**
    Unpack the DapParamId parameter values sent by DS Service.

    This class must be kept in sync with the code in DS Service
    responsible for packing DS parameters. The structure of message
    is as follows:
    +--------+-------------+-------------+-----+-------------+
    | Header | Parameter 1 | Parameter 2 | ... | Parameter N |
    +--------+-------------+-------------+-----+-------------+

    The header has following structure:
    |<--- int32 --->|<------ int32 ------>|
    +---------------+---------------------+
    | Active Device | Number Of Parameter |
    +---------------+---------------------+

    Each parameter is packed as follows:
    |<- int32 ->|<-- int32 --->|<----- int32 ---->|<-int16->| ... |<-int16->|
    +-----------+--------------+------------------+---------+-----+---------+
    | Device Id | Parameter Id | Number of Values | Value 1 | ... | Value N |
    +-----------+--------------+------------------+---------+-----+---------+

    This class represents an iterative interface for extracting parameter
    values. Start the iteration by calling begin() and call next() to go
    through each parameter. The next() function will return \p false when
    all the parameters are iterated over.
*/
class EffectParamParser
{
public:
    EffectParamParser(void *buffer, int size);

    bool validate();
    bool begin();
    bool next();

    audio_devices_t activeDevice() const
    { return static_cast<audio_devices_t>(mActiveDeviceId); }

    int numberOfParams() const
    { return mNumParams; }

    audio_devices_t deviceId() const
    { return static_cast<audio_devices_t>(mCurrentDeviceId); }

    DapParameterId paramId() const
    { return static_cast<DapParameterId>(mCurrentParamId); }

    int length() const
    { return mCurrentParamLen; }

    const dap_param_value_t *values() const
    { return mCurrentParamData; }

protected:
    ParserBuffer mPbuf;
    int32_t mActiveDeviceId;
    int32_t mNumParams;
    int32_t mCurrentParamIdx;
    int32_t mCurrentDeviceId;
    int32_t mCurrentParamId;
    int32_t mCurrentParamLen;
    dap_param_value_t *mCurrentParamData;
};

}

#endif//DOLBY_EFFECT_PARAM_PARSER_H_
