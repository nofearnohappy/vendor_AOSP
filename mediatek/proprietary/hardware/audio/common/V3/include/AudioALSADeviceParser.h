#ifndef ANDROID_AUDIO_MTK_DEVICE_PARSER_H
#define ANDROID_AUDIO_MTK_DEVICE_PARSER_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Mutex.h>
#include <utils/String8.h>
#include <media/AudioSystem.h>
#include "AudioType.h"
#include <utils/KeyedVector.h>
#include "AudioALSADeviceString.h"

namespace android
{

class AudioDeviceDescriptor
{
    public:
        AudioDeviceDescriptor()
        {
            mCardindex = 0;
            mPcmindex = 0;
            mplayback = 0;
            mRecord = 0;
        };
        String8 mStreamName;
        String8 mCodecName;
        unsigned int mCardindex;
        unsigned int mPcmindex;
        unsigned int mplayback;
        unsigned int mRecord;
};

class AudioALSADeviceParser
{
    public:
        unsigned int GetPcmIndexByString(String8 stringpair);
        unsigned int GetCardIndexByString(String8 stringpair);
        static AudioALSADeviceParser *getInstance();
        void dump();

        unsigned int GetCardIndex(){return mCardIndex;}
    private:
        static AudioALSADeviceParser *UniqueAlsaDeviceInstance;
        AudioALSADeviceParser();
        void GetAllPcmAttribute(void);
        void AddPcmString(char *InputBuffer);
        void SetPcmCapability(AudioDeviceDescriptor *Descriptor , char  *Buffer);
        void ParseCardIndex();
        /**
         * Audio Pcm vector
         */
        Vector <AudioDeviceDescriptor *> mAudioDeviceVector;

        unsigned int mCardIndex;
};

}

#endif
