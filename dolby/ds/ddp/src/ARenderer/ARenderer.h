/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2014 by Dolby Laboratories,
 *                            All rights reserved.
 *
 */

#ifndef _ARENDERER_H_
#define _ARENDERER_H_

#include "Dap2JocProcess.h"
#include "EndpConfig.h"
#include "evo_parser.h"

namespace dolby {

using namespace android;

class ARenderer
{
public:
    status_t init(int sampleRate);
    void deinit();
    status_t configure(struct dapConfig config, bool isJocContent);
    status_t setPregain(int value);
    status_t setSystemGain(int value);
    void process(int inChannel, int outChannel, int sampleRate,
                    void *in, void *out, void *evo_data, unsigned int evo_size, int sampleCount,
                    bool isDataInCQMF);

private:
    Dap2JocProcess*      mDap2JocPtr;
    evo_handle*          mEvoHandlePtr;
    void*                mOamdiMemoryPtr;
    oamdi*               mOamdiPtr;
    size_t               mOamdiSize;
    unsigned int         mOamdiOffset;
    bool     isLegacyChanCount(int inChannel);
    status_t initOamdi();
    status_t setOamdi(void *evo_frame_data, unsigned int evo_frame_size);
};

} //namespace dolby

#endif // _ARENDERER_H_

