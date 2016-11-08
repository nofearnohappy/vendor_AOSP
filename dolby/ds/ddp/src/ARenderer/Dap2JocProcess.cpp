/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2013-2014 by Dolby Laboratories,
 *                            All rights reserved.
 *
 */

#define LOG_TAG "Dap2JocProcess"
#include "DlbLog.h"
#include "Dap2JocProcess.h"

namespace dolby {

using namespace android;

Dap2JocProcess::Dap2JocProcess(unsigned long sampleRate)
{
    mSampleRate = sampleRate;

    // Initiliaze static values in dlb_buffer
    mAudioInDlbBuffer.ppdata        = pAudioInChannelData;
    mAudioOutDlbBuffer.ppdata       = pAudioOutChannelData;

    mAudioInDlbBuffer.data_type     = DAP_CPDP_DLB_BUFFER_TYPE;
    mAudioOutDlbBuffer.data_type    = DAP_CPDP_DLB_BUFFER_TYPE;

    mInClvecBuffer.num_channels = 0;
    mInClvecBuffer.num_blocks   = DAP_CPDP_CQMF_MAX_BLOCKS;
    mInClvecBuffer.num_elements = DAP_CPDP_CQMF_MAX_ELEMENTS;
    mInClvecBuffer.data         = NULL;
    mInClvecBuffer.max_num_channels = DAP_CPDP_MAX_IN_CHANNELS;
    mInClvecBuffer.max_num_blocks   = DAP_CPDP_CQMF_MAX_BLOCKS;
    mInClvecBuffer.max_num_elements = DAP_CPDP_CQMF_MAX_ELEMENTS;

    // Initialize a DAP instance with the specified sample rate
    status_t status = init();
    LOG_FATAL_IF(status != NO_ERROR, "DAP initialization failure!");
}

Dap2JocProcess::~Dap2JocProcess()
{
    ALOGI("%s()", __FUNCTION__);
    close();
}

/**
 * Process the audio.
 * Notes: The audio bit depth is determined by sizeof(IO_DATUM).
 *        This method must be called with sampleCount in multiples of DAP_CPDP_PCM_SAMPLES_PER_BLOCK.
 *        In case of any error this method mutes the audio.
 *
.* @param inChannel           Input Channel Count
 * @param outChannel          Output Channel Count
 * @param mdOffset            Metadata offset value
 * @param oamdi               Pointer to OAMDI data
 * @param in                  Pointer to the input data buffer.
 * @param out                 Pointer to the output data buffer.
 * @param sampleCount         The number of samples to be processed.
 * @param isDataInCQMF        Input data in CQMF format or not
 */
void Dap2JocProcess::process(int inChannel, int outChannel, unsigned int mdOffset,
                    oamdi *oamdiPtr, void *in, void *out,
                    int sampleCount,
                    bool isDataInCQMF)
{
    ALOGV("%s inChannel %d outChannel %d mdOffset %d oamdiPtr %p inPtr %p outPtr %p sampleCount %d isDataInCQMF %d", __FUNCTION__, inChannel, outChannel,
          mdOffset, oamdiPtr, in, out, sampleCount, isDataInCQMF);

    unsigned int sample_count_on_256 = sampleCount / DAP_CPDP_PCM_SAMPLES_PER_BLOCK;

    if ( (sampleCount == 0) ||
         (sampleCount % DAP_CPDP_PCM_SAMPLES_PER_BLOCK) ||
         (oamdiPtr != NULL && (int) mdOffset > sampleCount))
    {
        ALOGE("%s error sampleCount %d, mdOffset %d", __FUNCTION__, sampleCount, mdOffset);
        goto fail;
    }

    if (isDataInCQMF)
    {
        mInClvecBuffer.data = (void **)in;
        mInClvecBuffer.num_channels = inChannel;
		mInClvecBuffer.num_blocks = sample_count_on_256 * DAP_CPDP_CQMF_BLOCK_MULTIPLE;
        mAudioOutDlbBuffer.nchannel = dap_cpdp_prepare_cqmf(mDapCpDpStatePtr, &mInClvecBuffer, oamdiPtr, mdOffset, NULL, mMixDataPtr, 0);
    }
    else
    {
        mAudioInDlbBuffer.nchannel  = inChannel;
        mAudioInDlbBuffer.nstride   = DAP_CPDP_MAX_IN_CHANNELS;

        for (unsigned int c = 0; c < mAudioInDlbBuffer.nchannel; c++)
        {
            mAudioInDlbBuffer.ppdata[c] = (IO_DATUM *) in + c;
        }
        mAudioOutDlbBuffer.nchannel = dap_cpdp_prepare(mDapCpDpStatePtr, sample_count_on_256,
                                            &mAudioInDlbBuffer, oamdiPtr, mdOffset, NULL, mMixDataPtr, 0);
    }

    if (mAudioOutDlbBuffer.nchannel == 0)
    {
        ALOGE("%s dap_cpdp_prepare failed!", __FUNCTION__);
        goto fail;
    }
    mAudioOutDlbBuffer.nstride = mAudioOutDlbBuffer.nchannel;

    for (unsigned int c = 0; c < mAudioOutDlbBuffer.nchannel; c++)
    {
        mAudioOutDlbBuffer.ppdata[c]  = (IO_DATUM *) out + c;
    }

    mMIMetadata = dap_cpdp_process(mDapCpDpStatePtr, &mAudioOutDlbBuffer, mScratchMemoryPtr);
    return;

fail:
    memset(out, 0, sampleCount * sizeof (IO_DATUM) * outChannel);

}

} // namespace dolby
