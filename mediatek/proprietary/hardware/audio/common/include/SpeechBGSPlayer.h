#ifndef ANDROID_SPEECH_BACKGROUND_SOUND_PLAYER_H
#define ANDROID_SPEECH_BACKGROUND_SOUND_PLAYER_H

#include <pthread.h>
#include "AudioType.h"
#include "AudioUtility.h"

extern "C" {
#include "MtkAudioSrc.h"
}

namespace android
{
// for debug
//#define DUMP_BGS_DATA
//#define DUMP_BGS_BLI_BUF
//#define BGS_USE_SINE_WAVE

/*=============================================================================
 *                              Class definition
 *===========================================================================*/

class BGSPlayer;
class SpeechDriverInterface;

class BGSPlayBuffer
{
    private:
        BGSPlayBuffer();
        virtual ~BGSPlayBuffer(); // only destroied by friend class BGSPlayer

        friend          class BGSPlayer;

        status_t        InitBGSPlayBuffer(BGSPlayer *playPointer, uint32_t sampleRate, uint32_t chNum, int32_t mFormat);
        uint32_t        Write(char *buf, uint32_t num);

        int32_t         mFormat;

        // ring buffer
        RingBuf         mRingBuf;

        // BLI_SRC
        MtkAudioSrc     *mBliSrc;
        char           *mBliOutputLinearBuffer;

        Mutex           mBGSPlayBufferRuningMutex;
        Mutex           mBGSPlayBufferMutex;
        Condition       mBGSPlayBufferCondition;

        bool            mExitRequest;

//#ifdef DUMP_BGS_BLI_BUF
        FILE           *pOutFile;
//#endif
};

class BGSPlayer
{
    public:
        virtual ~BGSPlayer();

        static BGSPlayer       *GetInstance();

        BGSPlayBuffer*          CreateBGSPlayBuffer(uint32_t sampleRate, uint32_t chNum, int32_t format);
        uint32_t                Write(BGSPlayBuffer *pBGSPlayBuffer, void *buf, uint32_t num);
        void                    DestroyBGSPlayBuffer(BGSPlayBuffer *pBGSPlayBuffer);

        bool                    Open(SpeechDriverInterface *pSpeechDriver, uint8_t uplink_gain, uint8_t downlink_gain);
        uint32_t                PutData(BGSPlayBuffer *pBGSPlayBuffer, char *target_ptr, uint16_t num_data_request);
        uint32_t                PutDataToSpeaker(char *target_ptr, uint16_t num_data_request);
        bool                    Close();

        AudioLock               mBGSMutex; // use for create/destroy bgs buffer & ccci bgs data request

    private:
        BGSPlayer();

        static BGSPlayer       *mBGSPlayer; // singleton

        SpeechDriverInterface  *mSpeechDriver;
        SortedVector<BGSPlayBuffer *> mBGSPlayBufferVector;
        char                     *mBufBaseTemp;

        Mutex 					mBGSPlayBufferVectorLock;
        uint16_t 				mCount;

//#ifdef DUMP_BGS_DATA
        FILE                   *pOutFile;
//#endif
};


} // end namespace android

#endif //ANDROID_SPEECH_BACKGROUND_SOUND_PLAYER_H
