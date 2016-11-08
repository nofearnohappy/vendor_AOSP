#ifndef ANDROID_AUDIO_ALSA_SPEAKER_MONITOR_H
#define ANDROID_AUDIO_ALSA_SPEAKER_MONITOR_H

#include <utils/threads.h>
#include <utils/KeyedVector.h>

//#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware_legacy/AudioMTKHardwareInterface.h>
#include <hardware_legacy/AudioSystemLegacy.h>

#include "AudioType.h"
#include "AudioLock.h"
#include "AudioPolicyParameters.h"
#include "AudioALSAStreamManager.h"
#include "AudioLock.h"
//#include "AudioMTKStreamManagerInterface.h"

namespace android
{

class AudioALSAStreamOut;
class AudioALSAStreamIn;

class AudioALSASpeechPhoneCallController;
class AudioALSAVolumeController;

class AudioALSASpeakerMonitor
{
    public:
        virtual ~AudioALSASpeakerMonitor();
        static AudioALSASpeakerMonitor *getInstance();
        AudioALSAStreamManager *getStreamManager() {return mAudioMtkStreamManager;}
        status_t EnableSpeakerMonitorThread(bool enable);
        status_t Activate(void);
        status_t Deactivate(void);
        status_t SetTempLowerBound(short degree);
        status_t SetTempUpperBound(short degree);
        short GetTempLowerBound(void);
        short GetTempUpperBound(void);
        void OpenPCMDump(const char *class_name);
        void ClosePCMDump();
        void WritePcmDumpData(const void *buffer, ssize_t bytes);
        status_t GetFilterParam(unsigned int *center_freq, unsigned int *bw, int *threshold);
        pthread_mutex_t mSpkMonitorMutex;
        pthread_cond_t mSpkMonitor_Cond, mSpkMonitorActivate_Cond;
        bool m_bThreadExit;
        bool m_bActivated;

    protected:
        AudioALSASpeakerMonitor();
        FILE *mPCMDumpFile;
        static uint32_t mDumpFileNum;
    private:
        AudioLock mLock;
        static AudioALSASpeakerMonitor *UniqueInstance;
        pthread_t mSpeakerMonitorThreadID;
        AudioALSAStreamManager *mAudioMtkStreamManager;
        bool m_bEnabled;
        short mTempUpperBound;
        short mTempLowerBound;
        unsigned int mNotchFC;
        unsigned int mNotchBW;
        int mNotchTH;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_SPEAKER_MONITOR_H
