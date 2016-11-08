#ifndef ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_BT_CVSD_H
#define ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_BT_CVSD_H

#include "AudioALSACaptureDataProviderBase.h"

namespace android
{

class WCNChipController;
class AudioBTCVSDControl;

class AudioALSACaptureDataProviderBTCVSD : public AudioALSACaptureDataProviderBase
{
    public:
        virtual ~AudioALSACaptureDataProviderBTCVSD();

        static AudioALSACaptureDataProviderBTCVSD *getInstance();

        /**
         * open/close pcm interface when 1st attach & the last detach
         */
        status_t open();
        status_t close();



    protected:
        AudioALSACaptureDataProviderBTCVSD();


        uint32_t readDataFromBTCVSD(void *linear_buffer);



    private:
        /**
         * singleton pattern
         */
        static AudioALSACaptureDataProviderBTCVSD *mAudioALSACaptureDataProviderBTCVSD;

        WCNChipController *mWCNChipController;
        AudioBTCVSDControl *mAudioBTCVSDControl;

        /**
         * pcm read thread
         */
        static void *readThread(void *arg);
        pthread_t hReadThread;
        status_t GetCaptureTimeStamp(time_info_struct_t *Time_Info, size_t read_size);
    
        uint32_t mReadBufferSize;
        int mFd2;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_BT_CVSD_H
