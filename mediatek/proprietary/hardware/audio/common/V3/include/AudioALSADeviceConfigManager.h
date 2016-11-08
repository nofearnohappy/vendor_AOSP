#ifndef ANDROID_AUDIO_MTK_DEVICE_CONFIG_MANAGER_H
#define ANDROID_AUDIO_MTK_DEVICE_CONFIG_MANAGER_H

#include <stdint.h>
#include <sys/types.h>
#include <utils/Mutex.h>
#include <utils/String8.h>
#include "AudioType.h"
#include <utils/Log.h>
#include <cutils/properties.h>
#include <sys/types.h>
#include <cutils/config_utils.h>
#include <cutils/misc.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <tinyxml.h>

#define AUDIO_DEVICE_TURNON                   "turnon"
#define AUDIO_DEVICE_TURNOFF                 "turnoff"
#define AUDIO_DEVICE_SETTING                  "setting"

#define AUDIO_DEVICE_HEADPHONE           "headphone_output"
#define AUDIO_DEVICE_SPEAKER                 "speaker_output"
#define AUDIO_DEVICE_2IN1_SPEAKER       "two_in_one_speaker_output"
#define AUDIO_DEVICE_RECEIVER                "receiver_output"
#define AUDIO_DEVICE_EARPHONE_SPEAKER "headphoneSpeaker_output"
#define AUDIO_DEVICE_EXT_SPEAKER         "ext_speaker_output"

#define AUDIO_DEVICE_BUILTIN_MIC_MIC1  "builtin_Mic_Mic1"
#define AUDIO_DEVICE_BUILTIN_MIC_MIC2  "builtin_Mic_Mic2"
#define AUDIO_DEVICE_BUILTIN_MIC_MIC3  "builtin_Mic_Mic3"

#define AUDIO_DEVICE_BUILTIN_MIC_MIC1_INVERSE  "builtin_Mic_Mic1_Inverse"
#define AUDIO_DEVICE_BUILTIN_MIC_MIC2_INVERSE  "builtin_Mic_Mic2_Inverse"
#define AUDIO_DEVICE_BUILTIN_MIC_MIC3_INVERSE  "builtin_Mic_Mic3_Inverse"


#define AUDIO_DEVICE_BUILTIN_SINGLE_MIC  "builtin_Mic_SingleMic"
#define AUDIO_DEVICE_BUILTIN_DUAL_MIC  "builtin_Mic_DualMic"
#define AUDIO_DEVICE_BUILTIN_BACK_MIC  "builtin_Mic_BackMic"
#define AUDIO_DEVICE_BUILTIN_BACK_MIC_INVERSE  "builtin_Mic_BackMic_Inverse"

#define AUDIO_DEVICE_HEADSET_MIC        "headset_mic_input"
#define AUDIO_DEVICE_SIDETONE        "sidetone_switch"


#define AUDIOMIC1_TYPE_ACCMODE                    "Mic1TypeACCMode"
#define AUDIOMIC1_TYPE_DCCMODE                    "Mic1TypeDCCMode"
#define AUDIOMIC1_TYPE_DMICMODE                  "Mic1TypeDMICMode"
#define AUDIOMIC1_TYPE_DCCECMDIFFMODE    "Mic1TypeDCCECMDIFFMode"
#define AUDIOMIC1_TYPE_DCCECMSINGLEMODE    "Mic1TypeDCCECMSINGLEMode"

#define AUDIOMIC2_TYPE_ACCMODE                       "Mic2TypeACCMode"
#define AUDIOMIC2_TYPE_DCCMODE                       "Mic2TypeDCCMode"
#define AUDIOMIC2_TYPE_DMICMODE                     "Mic2TypeDMICMode"
#define AUDIOMIC2_TYPE_DCCECMDIFFMODE       "Mic2TypeDCCECMDIFFMode"
#define AUDIOMIC2_TYPE_DCCECMSINGLEMODE    "Mic2TypeDCCECMSINGLEMode"

#define AUDIOMIC3_TYPE_ACCMODE                       "Mic3TypeACCMode"
#define AUDIOMIC3_TYPE_DCCMODE                       "Mic3TypeDCCMode"
#define AUDIOMIC3_TYPE_DMICMODE                     "Mic3TypeDMICMode"
#define AUDIOMIC3_TYPE_DCCECMDIFFMODE       "Mic3TypeDCCECMDIFFMode"
#define AUDIOMIC3_TYPE_DCCECMSINGLEMODE    "Mic3TypeDCCECMSINGLEMode"

#define AUDIOMIC4_TYPE_ACCMODE                       "Mic4TypeACCMode"
#define AUDIOMIC4_TYPE_DCCMODE                       "Mic4TypeDCCMode"
#define AUDIOMIC4_TYPE_DMICMODE                     "Mic4TypeDMICMode"
#define AUDIOMIC4_TYPE_DCCECMDIFFMODE       "Mic4TypeDCCECMDIFFMode"
#define AUDIOMIC4_TYPE_DCCECMSINGLEMODE    "Mic4TypeDCCECMSINGLEMode"

#define AUDIO_MIC_INVERSE                                      "Mic_Setting_Inverse"
#define AUDIO_MIC_NOINVERSE                                 "Mic_Setting_NoInverse"

namespace android
{

class DeviceCtlDescriptor
{
    public:
        DeviceCtlDescriptor();
        String8 mDevicename;
        Vector<String8> mDeviceCltonVector;
        Vector<String8> mDeviceCltoffVector;
        Vector<String8> mDeviceCltsettingVector;
        int DeviceStatusCounter;
};

class DeviceCtlControlSeq
{
    public:
        Vector<String8> mDeviceCltNameVector;
        Vector<String8> mDeviceCltValueVector;
};

class AudioALSADeviceConfigManager
{
    public:
        static AudioALSADeviceConfigManager *getInstance();

        /**
          * LoadAudioConfig file
          */
        status_t LoadAudioConfig(const char *path);

        /**
          * config file related function
          */
        bool  SupportConfigFile(void);


        /**
          *  check device path exist
          */
        bool CheckDeviceExist(const char *path) ;

        /**
          * dump for all config file content
          */
        void dump(void);

          /**
          * apply turn on / off sequence by string
          */
        status_t ApplyDeviceTurnonSequenceByName(const char *DeviceName) ;
        status_t ApplyDeviceTurnoffSequenceByName(const char *DeviceName) ;

        /**
        * apply setting sequence
        */
        status_t ApplyDeviceSettingByName(const char *DeviceName) ;

    private:
        static AudioALSADeviceConfigManager *UniqueAlsaDeviceConfigParserInstance;
        AudioALSADeviceConfigManager();

        /**
          *  load config file
          */
        status_t GetVersion(TiXmlElement *root);
        status_t ParseInitSequence(TiXmlElement *root);
        status_t ParseDeviceSequence(TiXmlElement *root);
        /**
          *     query for device
          */
        DeviceCtlDescriptor *GetDeviceDescriptorbyname(const char *devicename);

        Vector<DeviceCtlDescriptor *> mDeviceVector;
        DeviceCtlControlSeq mDeviceCtlSeq;
        String8 VersionControl;
        bool mConfigsupport;
        bool mInit;

        /**
         * mixer controller
         */
        struct mixer *mMixer;
};

}

#endif
