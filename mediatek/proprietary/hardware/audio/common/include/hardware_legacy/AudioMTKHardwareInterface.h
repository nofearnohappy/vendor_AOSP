#ifndef ANDROID_AUDIO_MTK_HARDWARE_INTERFACE_H
#define ANDROID_AUDIO_MTK_HARDWARE_INTERFACE_H

//#include <SpeechControlInterface.h>

//!  AudioMTKHardwareInterface interface
/*!
  this class is hold extension of android default hardwareinterface
*/
#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware/audio.h>
#include <hardware/AudioCustomVolume.h>


namespace android_audio_legacy
{

class AudioMTKHardwareInterface:public android_audio_legacy::AudioHardwareInterface
{
    public:
        virtual             ~AudioMTKHardwareInterface() { }      
        /**
        *  SetEMParamete, set em parameters to audioahrdware
        * @param ptr
        * @param len
        * @return status
        */
        virtual status_t SetEMParameter(void *ptr , int len) = 0;

        /**
        *  GetEMParameter, get em parameters to audioahrdware
        * @param ptr
        * @param len
        * @return status
        */
        virtual status_t GetEMParameter(void *ptr , int len) = 0;

        /**
        *  SetAudioCommand, base on par1 and par2
        * @param par1
        * @param par2
        * @return status
        */
        virtual status_t SetAudioCommand(int par1, int par2) = 0;

        /**
        *  GetAudioCommand, base on par1
        * @param par1
        * @return status
        */
        virtual status_t GetAudioCommand(int par1) = 0;

        /**
        *  SetAudioData
        * @param par1
        * @param len
        * @param ptr
        * @return status
        */
        virtual status_t SetAudioData(int par1, size_t len, void *ptr) = 0;

        /**
        *  GetAudioData
        * @param par1
        * @param len
        * @param ptr
        * @return status
        */
        virtual status_t GetAudioData(int par1, size_t len, void *ptr) = 0;

        /**
        *  set ACF Preview parameter , thoiis function only temporary replace coefficient
        * @param ptr
        * @param len
        * @return status
        */
        virtual status_t SetACFPreviewParameter(void *ptr , int len) = 0;

        /**
        *  set HCF Preview parameter , thoiis function only temporary replace coefficient
        * @param ptr
        * @param len
        * @return status
        */
        virtual status_t SetHCFPreviewParameter(void *ptr , int len) = 0;
        // for open output stream with flag
        #if 1
        virtual AudioStreamOut* openOutputStreamWithFlags(
                                uint32_t devices,
                                audio_output_flags_t flags=(audio_output_flags_t)0,
                                int *format=0,
                                uint32_t *channels=0,
                                uint32_t *sampleRate=0,
                                status_t *status=0) = 0;// = 0;
        #endif

        // for open input stream with flag
        virtual AudioStreamIn* openInputStreamWithFlags(
                                uint32_t devices,
                                int *format,
                                uint32_t *channels,
                                uint32_t *sampleRate,
                                status_t *status,
                                android_audio_legacy::AudioSystem::audio_in_acoustics acoustics,
                                audio_input_flags_t flags=(audio_input_flags_t)0)
        {
            return openInputStream(devices, format, channels, sampleRate, status, acoustics);
        }
        /////////////////////////////////////////////////////////////////////////
        //    for PCMxWay Interface API
        /////////////////////////////////////////////////////////////////////////
        virtual int xWayPlay_Start(int sample_rate) = 0;
        virtual int xWayPlay_Stop(void) = 0;
        virtual int xWayPlay_Write(void *buffer, int size_bytes) = 0;
        virtual int xWayPlay_GetFreeBufferCount(void) = 0;
        virtual int xWayRec_Start(int sample_rate) = 0;
        virtual int xWayRec_Stop(void) = 0;
        virtual int xWayRec_Read(void *buffer, int size_bytes) = 0;

        //added by wendy
        virtual int ReadRefFromRing(void *buf, uint32_t datasz, void *DLtime) = 0;
        virtual int GetVoiceUnlockULTime(void *DLtime) = 0;
        virtual int SetVoiceUnlockSRC(uint outSR, uint outChannel) = 0;
        virtual bool startVoiceUnlockDL() = 0;
        virtual bool stopVoiceUnlockDL() = 0;
        virtual void freeVoiceUnlockDLInstance() = 0;
        virtual int GetVoiceUnlockDLLatency() = 0;
        virtual bool getVoiceUnlockDLInstance() = 0;

        virtual int createAudioPatch(unsigned int num_sources,
                               const struct audio_port_config *sources,
                               unsigned int num_sinks,
                               const struct audio_port_config *sinks,
                               audio_patch_handle_t *handle) = 0;

        virtual int releaseAudioPatch(audio_patch_handle_t handle) = 0;

        virtual int getAudioPort(struct audio_port *port) = 0;

        virtual int setAudioPortConfig(const struct audio_port_config *config) = 0;

        static AudioMTKHardwareInterface* create();

        virtual status_t GetAudioCustomVol(android::AUDIO_CUSTOM_VOLUME_STRUCT *pAudioCustomVol,int dStructLen)=0;
};

class AudioMTKStreamOutInterface:public AudioStreamOut
{
    public:
        virtual             ~AudioMTKStreamOutInterface()= 0;   
        virtual status_t    setCallBack(stream_callback_t callback, void *cookie)= 0;
        virtual status_t    pause() = 0;
        virtual status_t    resume()= 0;
        virtual int         drain(audio_drain_type_t type) = 0;

        virtual status_t    flush()= 0;

        //Force below two functions to pure virtual
        virtual status_t    getNextWriteTimestamp(int64_t *timestamp)= 0;
        virtual status_t    getPresentationPosition(uint64_t *frames, struct timespec *timestamp)= 0;
};


extern "C" AudioMTKHardwareInterface* createMTKAudioHardware(void);

}

typedef android_audio_legacy::AudioMTKHardwareInterface* create_AudioMTKHw(void);

#endif
