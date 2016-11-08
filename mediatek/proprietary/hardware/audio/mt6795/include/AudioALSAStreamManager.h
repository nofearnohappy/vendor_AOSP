#ifndef ANDROID_AUDIO_ALSA_STREAM_MANAGER_H
#define ANDROID_AUDIO_ALSA_STREAM_MANAGER_H

#include <utils/threads.h>
#include <utils/KeyedVector.h>

//#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware_legacy/AudioMTKHardwareInterface.h>
#include <hardware_legacy/AudioSystemLegacy.h>

#include "AudioType.h"
#include "AudioLock.h"
#include "AudioMTKFilter.h"
#include "AudioPolicyParameters.h"
#include "AudioSpeechEnhanceInfo.h"


namespace android
{

class AudioALSAStreamOut;
class AudioALSAStreamIn;

class AudioALSAPlaybackHandlerBase;
class AudioALSACaptureHandlerBase;

class AudioALSASpeechPhoneCallController;
class AudioALSAFMController;

class AudioALSAVolumeController;

class AudioALSAVoiceWakeUpController;

class SpeechDriverFactory;

class AudioALSAStreamManager
{
    public:
        virtual ~AudioALSAStreamManager();
        static AudioALSAStreamManager *getInstance();


        /**
         * open/close ALSA output stream
         */
        android_audio_legacy::AudioStreamOut *openOutputStream(
            uint32_t devices,
            int *format,
            uint32_t *channels,
            uint32_t *sampleRate,
            status_t *status,
            uint32_t output_flag=0);

        void closeOutputStream(android_audio_legacy::AudioStreamOut *out);


        /**
         * open/close ALSA input stream
         */
        android_audio_legacy::AudioStreamIn *openInputStream(
            uint32_t devices,
            int *format,
            uint32_t *channels,
            uint32_t *sampleRate,
            status_t *status,
            android_audio_legacy::AudioSystem::audio_in_acoustics acoustics,
            uint32_t input_flag=0);

        void closeInputStream(android_audio_legacy::AudioStreamIn *in);


        /**
         * create/destroy ALSA playback/capture handler
         */
        AudioALSAPlaybackHandlerBase *createPlaybackHandler(stream_attribute_t *stream_attribute_source);
        AudioALSACaptureHandlerBase  *createCaptureHandler(stream_attribute_t *stream_attribute_target);

        status_t destroyPlaybackHandler(AudioALSAPlaybackHandlerBase *pPlaybackHandler);
        status_t destroyCaptureHandler(AudioALSACaptureHandlerBase   *pCaptureHandler);


        /**
         * volume related functions
         */
        status_t setVoiceVolume(float volume);
        float getMasterVolume(void);
        status_t setMasterVolume(float volume);
        status_t setFmVolume(float volume);

        status_t setMicMute(bool state);
        bool     getMicMute();

        /**
         * mode / routing related functions
         */
        status_t setMode(audio_mode_t new_mode);
        status_t routingOutputDevice(AudioALSAStreamOut *pAudioALSAStreamOut, const audio_devices_t current_output_devices, audio_devices_t output_devices);
        status_t routingInputDevice(const audio_devices_t current_input_device, audio_devices_t input_device);

        // check if headset has changed
        bool CheckHeadsetChange(const audio_devices_t current_input_device, audio_devices_t input_device);


        /**
         * FM radio related opeation // TODO(Harvey): move to FM Controller later
         */
        status_t setFmEnable(const bool enable, bool bForceControl = false, bool bForce2DirectConn = false);
        bool     getFmEnable();


        /**
         * suspend/resume all input/output stream
         */
        status_t setAllOutputStreamsSuspend(const bool suspend_on, const bool setModeRequest = false);
        status_t setAllInputStreamsSuspend(const bool suspend_on, const bool setModeRequest = false);
        status_t setAllStreamsSuspend(const bool suspend_on, const bool setModeRequest = false);


        /**
         * standby all input/output stream
         */
        status_t standbyAllOutputStreams(const bool setModeRequest = false);
        status_t standbyAllInputStreams(const bool setModeRequest = false);
        status_t standbyAllStreams(const bool setModeRequest = false);


        /**
         * audio mode status
         */
        inline bool isModeInPhoneCall() { return isModeInPhoneCall(mAudioMode); }
        inline bool isModeInVoipCall()  { return isModeInVoipCall(mAudioMode); }


        // TODO(Harvey): test code, remove it later
        inline uint32_t getStreamOutVectorSize()  { return mStreamOutVector.size(); }
        inline AudioALSAStreamOut *getStreamOut(const size_t i)  { return mStreamOutVector[i]; }
        inline AudioALSAStreamIn *getStreamIn(const size_t i)  { return mStreamInVector[i]; }


        /**
         * stream in related
         */
        virtual size_t getInputBufferSize(uint32_t sampleRate, audio_format_t format, uint32_t channelCount);


        status_t SetMusicPlusStatus(bool bEnable);
        bool GetMusicPlusStatus();
        status_t SetBesLoudnessStatus(bool bEnable);
        bool GetBesLoudnessStatus();
        status_t SetBesLoudnessControlCallback(const BESLOUDNESS_CONTROL_CALLBACK_STRUCT *callback_data);
        status_t UpdateACFHCF(int value);
        status_t SetACFPreviewParameter(void *ptr , int len);
        status_t SetHCFPreviewParameter(void *ptr , int len);

        status_t SetSpeechVmEnable(const int enable);
        status_t SetEMParameter(AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB);
        status_t UpdateSpeechParams(const int speech_band);
        status_t UpdateDualMicParams();
        status_t UpdateMagiConParams();
        status_t UpdateHACParams();
        status_t UpdateSpeechMode();
        status_t UpdateSpeechVolume();
        status_t SetVCEEnable(bool bEnable);
        status_t UpdateSpeechLpbkParams();

        status_t Enable_DualMicSettng(sph_enh_dynamic_mask_t sphMask, bool bEnable);
        status_t Set_LSPK_DlMNR_Enable(sph_enh_dynamic_mask_t sphMask, bool bEnable);
        status_t setSpkOutputGain(int32_t gain, uint32_t ramp_sample_cnt);
        status_t setSpkFilterParam(uint32_t fc, uint32_t bw, int32_t th);
        status_t setVtNeedOn(const bool vt_on);
        status_t setBGSDlMute(const bool mute_on);
        status_t setBGSUlMute(const bool mute_on);
        bool EnableBesRecord(void);

        /**
         * Magic Conference Call
         */
        status_t SetMagiConCallEnable(bool bEnable);        
        bool GetMagiConCallEnable(void);

        /**
         * HAC
         */
        status_t SetHACEnable(bool bEnable);
        bool GetHACEnable(void);

        /**
         * VM Log
         */
        status_t SetVMLogConfig(unsigned short mVMConfig);
        unsigned short GetVMLogConfig(void);

        /**
         * Cust XML
         */
        status_t SetCustXmlEnable(unsigned short enable);
        unsigned short GetCustXmlEnable(void);

        /**
         * BT NREC
         */
        status_t SetBtHeadsetNrec(bool bEnable);
        bool GetBtHeadsetNrecStatus(void);

        /**
         * voice wake up
         */
        status_t setVoiceWakeUpNeedOn(const bool enable);
        bool     getVoiceWakeUpNeedOn();

        /**
         * VoIP dynamic function
         */
        void UpdateDynamicFunctionMask(void);


        /**
         * low latency
         */
        status_t setLowLatencyMode(bool mode);
        /**
         * Bypass DL Post Process Flag
         */
        status_t setBypassDLProcess(bool flag);


        /**
         * [TMP] stream out routing related // TODO(Harvey)
         */
        virtual status_t setParametersToStreamOut(const String8 &keyValuePairs);
        virtual status_t setParameters(const String8 &keyValuePairs, int IOport);


#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
        /**
         * Enable/Disable speech Strm
         */
        status_t DisableSphStrm(const audio_mode_t new_mode);
        status_t EnableSphStrm(const audio_mode_t new_mode);
        status_t DisableSphStrm(audio_devices_t output_devices);
        status_t EnableSphStrm(audio_devices_t output_devices);
        /**
         * Update Stream out filter
         */
        bool isModeInPhoneCallSupportEchoRef(const audio_mode_t audio_mode);
        status_t UpdateStreamOutFilter(android_audio_legacy::AudioStreamOut *out, int format, uint32_t channels, uint32_t sampleRate);
        bool IsSphStrmSupport(void);
#endif


    protected:
        AudioALSAStreamManager();

        inline bool isModeInPhoneCall(const audio_mode_t audio_mode)
        {
            return (audio_mode == AUDIO_MODE_IN_CALL ||
                    audio_mode == AUDIO_MODE_IN_CALL_2 ||
                    audio_mode == AUDIO_MODE_IN_CALL_EXTERNAL);
        }

        inline bool isModeInVoipCall(const audio_mode_t audio_mode)
        {
            return (audio_mode == AUDIO_MODE_IN_COMMUNICATION);
        }


        void SetInputMute(bool bEnable);

    private:
        /**
         * singleton pattern
         */
        static AudioALSAStreamManager *mStreamManager;


        /**
         * stream manager lock
         */
        AudioLock mStreamVectorLock; // used in setMode & open/close input/output stream
        AudioLock mPlaybackHandlerVectorLock;
        AudioLock mLock;


        /**
         * stream in/out vector
         */
        KeyedVector<uint32_t, AudioALSAStreamOut *> mStreamOutVector;
        KeyedVector<uint32_t, AudioALSAStreamIn *>  mStreamInVector;
        uint32_t mStreamOutIndex;
        uint32_t mStreamInIndex;


        /**
         * stream playback/capture handler vector
         */
        KeyedVector<uint32_t, AudioALSAPlaybackHandlerBase *> mPlaybackHandlerVector;
        KeyedVector<uint32_t, AudioALSACaptureHandlerBase *>  mCaptureHandlerVector;
        uint32_t mPlaybackHandlerIndex;
        uint32_t mCaptureHandlerIndex;


        /**
         * speech phone call controller
         */
        AudioALSASpeechPhoneCallController *mSpeechPhoneCallController;


        /**
         * FM radio
         */
        AudioALSAFMController *mFMController;


        /**
         * volume controller
         */
        AudioALSAVolumeController *mAudioALSAVolumeController;
        SpeechDriverFactory *mSpeechDriverFactory;


        /**
         * volume related variables
         */
        bool mMicMute;


        /**
         * audio mode
         */
        audio_mode_t mAudioMode;


        /**
         * Loopback related
         */
        bool mLoopbackEnable; // TODO(Harvey): move to Loopback Controller later

        /**
         * stream in/out vector
         */
        KeyedVector<uint32_t, AudioMTKFilterManager *> mFilterManagerVector;
        uint32_t mFilterManagerNumber;

        bool mBesLoudnessStatus;

        void (*mBesLoudnessControlCallback)(void *data);

        /**
         * Speech EnhanceInfo Instance
         */
        AudioSpeechEnhanceInfo *mAudioSpeechEnhanceInfoInstance;

        /**
         * headphone change flag
         */
        bool mHeadsetChange;

        /**
         * voice wake up
         */
        AudioALSAVoiceWakeUpController *mAudioALSAVoiceWakeUpController;
        bool mVoiceWakeUpNeedOn;
        bool mForceDisableVoiceWakeUpForSetMode;

        /**
        * Bypass DL Post Process Flag
        */
        bool mBypassPostProcessDL;

        /**
        * Bgs UL/DL Gain
        */
        uint8_t mBGSDlGain;
        uint8_t mBGSUlGain;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_STREAM_MANAGER_H
