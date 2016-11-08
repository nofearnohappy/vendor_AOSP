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
#define LOG_TAG "DlbDapPcmDump"

#include "DlbLog.h"
#include "DapPcmDump.h"
#include <stdlib.h>
#include <cutils/properties.h>

namespace dolby {

using namespace android;

DapBufferDump::DapBufferDump(const char *filepath) : mFilePath(filepath)
{
    ALOGV("%s()", __FUNCTION__);
    mDumpFile = NULL;
}

DapBufferDump::~DapBufferDump()
{
    close();
}

status_t DapBufferDump::open()
{
    ALOGVV("%s()", __FUNCTION__);
    LOG_FATAL_IF(mDumpFile != NULL, "%s() Called when file is already opened!", __FUNCTION__);
    mDumpFile = fopen(mFilePath, "wb");

    if (mDumpFile == NULL)
    {
        ALOGE("%s() Error opening file %s. Have you done 'adb remount'?", __FUNCTION__, mFilePath);
        return NO_INIT;
    }

    return NO_ERROR;
}

status_t DapBufferDump::dump(const BufferProvider &buf)
{
    ALOGVV("%s()", __FUNCTION__);
    LOG_FATAL_IF(mDumpFile == NULL, "%s() Called when file is not opened!", __FUNCTION__);

    if (fwrite(buf.raw(), buf.stride(), buf.capacity(), mDumpFile) == 0)
    {
        ALOGE("%s() Error writing to file %s. Check if there is enough free space.", __FUNCTION__, mFilePath);
        close();
        return NO_MEMORY;
    }
    return NO_ERROR;
}

void DapBufferDump::close()
{
    ALOGVV("%s()", __FUNCTION__);
    if (mDumpFile != NULL)
    {
        fclose(mDumpFile);
        mDumpFile = NULL;
    }
}

DapPcmDump::DapPcmDump() :
    mInBufferDump(DOLBY_PCM_IN_DUMP_FILE),
    mOutBufferDump(DOLBY_PCM_OUT_DUMP_FILE)
{
    mState = PCM_DUMP_STOPPED;
    ALOGI("DAP PCM Dump will be available as %s, %s", DOLBY_PCM_IN_DUMP_FILE, DOLBY_PCM_OUT_DUMP_FILE);
}

DapPcmDump::~DapPcmDump()
{
    ALOGV("%s()", __FUNCTION__);
    mInBufferDump.close();
    mOutBufferDump.close();
}

bool DapPcmDump::getUserEnable()
{
    ALOGVV("%s()", __FUNCTION__);
    char dolby_dump_enable_value[PROPERTY_VALUE_MAX];
    property_get(DOLBY_PCM_DUMP_PROPERTY, dolby_dump_enable_value, "0");
    int dolby_dump_enable = atoi(dolby_dump_enable_value);
    return dolby_dump_enable != 0;
}

void DapPcmDump::dump(const BufferProvider &buffer, DapBufferDump &dumper)
{
    ALOGVV("%s()", __FUNCTION__);
    bool userEnable = getUserEnable();

    switch (mState)
    {
    case PCM_DUMP_STOPPED:
        if (userEnable)
        {
            if ((mInBufferDump.open() == NO_ERROR)
                && (mOutBufferDump.open() == NO_ERROR))
            {
                ALOGI("Started DAP PCM dump");
                mState = PCM_DUMP_STARTED;
            }
        }
        break;
    case PCM_DUMP_STARTED:
        if (!userEnable)
        {
            ALOGI("Stopped DAP PCM dump");
            mState = PCM_DUMP_STOPPED;
        }
        break;
    case PCM_DUMP_ERROR:
        if (!userEnable)
        {
            mState = PCM_DUMP_STOPPED;
        }
        break;
    }

    if (mState == PCM_DUMP_STARTED)
    {
        if(dumper.dump(buffer) != NO_ERROR)
        {
            mState = PCM_DUMP_ERROR;
        }
    }
}

void DapPcmDump::dumpInput(const BufferProvider &inBuffer)
{
    ALOGVV("%s()", __FUNCTION__);
    dump(inBuffer, mInBufferDump);
}

void DapPcmDump::dumpOutput(const BufferProvider &outBuffer)
{
    ALOGVV("%s()", __FUNCTION__);
    dump(outBuffer, mOutBufferDump);
}

} // namespace dolby
