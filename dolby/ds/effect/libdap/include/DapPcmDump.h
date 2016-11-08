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
#ifndef DOLBY_DAP_PCM_DUMP_H_
#define DOLBY_DAP_PCM_DUMP_H_

#include "DlbBufferProvider.h"
#include "DapParams.h"

#define DOLBY_PCM_DUMP_PROPERTY "dolby.debug.dump_pcm"
#define DOLBY_PCM_IN_DUMP_FILE "/data/dolby/DsPcmInput.pcm"
#define DOLBY_PCM_OUT_DUMP_FILE "/data/dolby/DsPcmOutput.pcm"

namespace dolby {

using namespace android;

class DapBufferDump
{
public:
    DapBufferDump(const char *filepath);
    ~DapBufferDump();

    status_t open();
    void close();
    status_t dump(const BufferProvider &buf);

protected:
    const char *mFilePath;
    FILE *mDumpFile;
};

//
// Usage: Change the volume down to 0 to end the current audio dump session while music is playing.
// And then change the volume 2 clicks up to launch the new audio dump session during music playback.
// As a result, the previous dumped audio will be overwritten.
// The retrieved audio dump will be under /data folder, so first ensure /data folder is writable.
//
class DapPcmDump
{
public:
    enum PcmDumpState {
        PCM_DUMP_STOPPED,
        PCM_DUMP_STARTED,
        PCM_DUMP_ERROR,
    };

    DapPcmDump();
    ~DapPcmDump();

    void dumpInput(const BufferProvider &inBuffer);
    void dumpOutput(const BufferProvider &outBuffer);

protected:
    void dump(const BufferProvider &buffer, DapBufferDump &dumper);
    bool getUserEnable();

    PcmDumpState mState;
    DapBufferDump mInBufferDump;
    DapBufferDump mOutBufferDump;
};

} // namespace dolby
#endif//DOLBY_DAP_PCM_DUMP_H_
