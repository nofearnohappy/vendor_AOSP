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

#ifndef _DAP2JOCPROCESS_H_
#define _DAP2JOCPROCESS_H_

#include <utils/Errors.h>
#include <sys/types.h>
#ifdef __cplusplus
extern "C" {
#include "dap_cpdp_cqmf.h"
}
#endif
#include "Dap2Process.h"

namespace dolby {

using namespace android;

class Dap2JocProcess : public Dap2Process
{

/* The audio bit depth is a compile-time decision. */
#ifdef DOLBY_DAP_16BIT_AUDIO
#define IO_DATUM int16_t
#define DAP_CPDP_DLB_BUFFER_TYPE    DLB_BUFFER_SHORT_16

#elif defined(DOLBY_DAP_32BIT_AUDIO_LEFT_ALIGNED)
#define IO_DATUM int32_t
#define DAP_CPDP_DLB_BUFFER_TYPE    DLB_BUFFER_INT_LEFT

#else
#error "DOLBY_DAP audio bit depth not defined"

#endif // DOLBY_DAP_16BIT_AUDIO

#define DAP_CPDP_MAX_IN_CHANNELS  (16)
#define DAP_CPDP_MAX_OUT_CHANNELS (8)
public:
    Dap2JocProcess(unsigned long sampleRate);
    virtual ~Dap2JocProcess();

    void process(int inChannel, int outChannel, unsigned int mdOffset,
                        oamdi *oamdiPtr, void *in, void *out,
                        int sampleCount, bool isDataInCQMF);

private:
    clvec_buffer         mInClvecBuffer;

    dlb_buffer           mAudioInDlbBuffer;
    dlb_buffer           mAudioOutDlbBuffer;
    void*                pAudioInChannelData[DAP_CPDP_MAX_IN_CHANNELS];
    void*                pAudioOutChannelData[DAP_CPDP_MAX_OUT_CHANNELS];
};

}; // namespace dolby

#endif //_DAP2JOCPROCESS_H_
