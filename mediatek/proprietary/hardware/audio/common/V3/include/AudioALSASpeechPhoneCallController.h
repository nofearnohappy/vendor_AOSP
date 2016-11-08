#ifndef ANDROID_AUDIO_ALSA_SPEECH_PHONE_CALL_CONTROLLER_H
#define ANDROID_AUDIO_ALSA_SPEECH_PHONE_CALL_CONTROLLER_H

#include <tinyalsa/asoundlib.h> // TODO(Harvey): move it

#include "AudioType.h"
#include "SpeechType.h"

#include "AudioLock.h"
#include "AudioVolumeInterface.h"

#ifdef SPEECH_PMIC_RESET
#include <pthread.h>
#include <utils/threads.h>
#include <linux/rtpm_prio.h>
#include <sys/prctl.h>
#endif

namespace android
{

enum tty_mode_t
{
    AUD_TTY_OFF  =  0,
    AUD_TTY_FULL =  1,
    AUD_TTY_VCO  =  2,
    AUD_TTY_HCO  =  4,
    AUD_TTY_ERR  = -1
};

class AudioALSAHardwareResourceManager;
class SpeechDriverFactory;
class AudioBTCVSDControl;
class AudioALSAVolumeController;

class AudioALSASpeechPhoneCallController
{
    public:
        virtual ~AudioALSASpeechPhoneCallController();

        static AudioALSASpeechPhoneCallController *getInstance();

        virtual audio_devices_t getInputDeviceForPhoneCall(const audio_devices_t output_devices);

        virtual status_t        open(const audio_mode_t audio_mode, const audio_devices_t output_devices, const audio_devices_t input_device);
        virtual status_t        close();
        virtual status_t        routing(const audio_devices_t new_output_devices, const audio_devices_t new_input_device);

        virtual bool            checkTtyNeedOn() const;
        virtual bool            checkSideToneFilterNeedOn(const audio_devices_t output_device) const;

        inline tty_mode_t       getTtyMode() const { return mTtyMode; }
        virtual status_t        setTtyMode(const tty_mode_t tty_mode);
        inline audio_devices_t  getRoutingForTty() const { return mRoutingForTty; }
        inline void             setRoutingForTty(const audio_devices_t new_device) { mRoutingForTty = new_device; }
        virtual void            setTtyInOutDevice(audio_devices_t routing_device);

        virtual void            setVtNeedOn(const bool vt_on);
        virtual void            setMicMute(const bool mute_on);

        virtual void            setBTMode(const int mode);
        virtual void            setDlMute(const bool mute_on);
        virtual void            setUlMute(const bool mute_on);
        virtual void            getRFInfo();


    protected:
        AudioALSASpeechPhoneCallController();

        /**
         * init audio hardware
         */
        virtual status_t init();

        inline uint32_t         calculateSampleRate(const bool bt_device_on)
        {
            return (bt_device_on == false) ? 16000 : (mBTMode == 0) ? 8000 : 16000;
        }



        AudioALSAHardwareResourceManager *mHardwareResourceManager;
        AudioVolumeInterface *mAudioALSAVolumeController;

        SpeechDriverFactory    *mSpeechDriverFactory;
        AudioBTCVSDControl     *mAudioBTCVSDControl;

        AudioLock               mLock;
#ifdef SPEECH_PMIC_RESET
        AudioLock               mThreadLock;
#endif

        audio_mode_t            mAudioMode;

        bool                    mMicMute;
        bool                    mDlMute;
        bool                    mUlMute;
        bool                    mVtNeedOn;

        tty_mode_t              mTtyMode;
        audio_devices_t         mRoutingForTty;

        int                     mBTMode; // BT mode, 0:NB, 1:WB


        struct pcm_config mConfig; // TODO(Harvey): move it to AudioALSAHardwareResourceManager later

        struct pcm *mPcmIn; // TODO(Harvey): move it to AudioALSAHardwareResourceManager later
        struct pcm *mPcmOut; // TODO(Harvey): move it to AudioALSAHardwareResourceManager later
        uint16_t mRfInfo, mRfMode, mASRCNeedOn;


    private:
        static AudioALSASpeechPhoneCallController *mSpeechPhoneCallController; // singleton
#ifdef SPEECH_PMIC_RESET
        bool    mEnable_PMIC_Reset;
        bool            StartPMIC_Reset();
        static void *thread_PMIC_Reset(void *arg);
        pthread_t hThread_PMIC_Reset;
#endif

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_SPEECH_PHONE_CALL_CONTROLLER_H
