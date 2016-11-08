#ifndef ANDROID_SPEECH_DRIVER_LAD_H
#define ANDROID_SPEECH_DRIVER_LAD_H

#include "SpeechDriverInterface.h"

#include "CFG_AUDIO_File.h"

namespace android
{
    typedef struct _AUDIO_CUSTOM_HAC_SPEECH_PARAM_STRUCT
    {
        /* speech enhancement */
        unsigned short speech_hac_mode_nb_para[16];
        /* WB speech enhancement */
        unsigned short speech_hac_mode_wb_para[16];    
        /* speech input FIR */
        short sph_hac_in_fir[45];
        /* speech output FIR */
        short sph_hac_out_fir[45];
        
        /* WB speech input FIR */
        short sph_hac_wb_in_fir[90];
        /* WB speech output FIR */
        short sph_hac_wb_out_fir[90];
        /* mic volume setting */    
        
    } AUDIO_CUSTOM_HAC_SPEECH_PARAM_STRUCT;

class SpeechMessengerInterface;

class SpeechDriverLAD : public SpeechDriverInterface
{
    public:
        virtual ~SpeechDriverLAD();


        /**
         * get instance's pointer
         */
        static SpeechDriverLAD *GetInstance(modem_index_t modem_index);


        /**
         * speech control
         */
        virtual speech_mode_t GetSpeechModeByOutputDevice(const audio_devices_t output_device); // only available for LAD
        virtual status_t SetSpeechMode(const audio_devices_t input_device, const audio_devices_t output_device);
        virtual status_t SpeechOn();
        virtual status_t SpeechOff();
        virtual status_t VideoTelephonyOn();
        virtual status_t VideoTelephonyOff();
        virtual status_t SpeechRouterOn();
        virtual status_t SpeechRouterOff();

        virtual status_t setMDVolumeIndex(int stream, int device, int index);

        /**
         * record control
         */
        virtual status_t RecordOn();
        virtual status_t RecordOff();
        virtual status_t RecordOn(record_type_t type_record);
        virtual status_t RecordOff(record_type_t type_record);
        virtual status_t SetPcmRecordType(record_type_t type_record);

        virtual status_t VoiceMemoRecordOn();
        virtual status_t VoiceMemoRecordOff();

        virtual uint16_t GetRecordSampleRate() const;
        virtual uint16_t GetRecordChannelNumber() const;


        /**
         * background sound control
         */
        virtual status_t BGSoundOn();
        virtual status_t BGSoundConfig(uint8_t ul_gain, uint8_t dl_gain);
        virtual status_t BGSoundOff();


        /**
         * pcm 2 way
         */
        virtual status_t PCM2WayPlayOn();
        virtual status_t PCM2WayPlayOff();
        virtual status_t PCM2WayRecordOn();
        virtual status_t PCM2WayRecordOff();
        virtual status_t PCM2WayOn(const bool wideband_on);
        virtual status_t PCM2WayOff();
        virtual status_t DualMicPCM2WayOn(const bool wideband_on, const bool record_only);
        virtual status_t DualMicPCM2WayOff();



        /**
         * tty ctm control
         */
        virtual status_t TtyCtmOn(ctm_interface_t ctm_interface);
        virtual status_t TtyCtmOff();
        virtual status_t TtyCtmDebugOn(bool tty_debug_flag);


        /**
         * acoustic loopback
         */
        virtual status_t SetAcousticLoopback(bool loopback_on);
        virtual status_t SetAcousticLoopbackBtCodec(bool enable_codec);

        virtual status_t SetAcousticLoopbackDelayFrames(int32_t delay_frames);


        /**
         * volume control
         */
        virtual status_t SetDownlinkGain(int16_t gain);
        virtual status_t SetEnh1DownlinkGain(int16_t gain);
        virtual status_t SetUplinkGain(int16_t gain);
        virtual status_t SetDownlinkMute(bool mute_on);
        virtual status_t SetUplinkMute(bool mute_on);
        virtual status_t SetUplinkSourceMute(bool mute_on);
        virtual status_t SetSidetoneGain(int16_t gain);
        virtual status_t SetDSPSidetoneFilter(const bool dsp_stf_on);


        /**
         * device related config
         */
        virtual status_t SetModemSideSamplingRate(uint16_t sample_rate);


        /**
         * speech enhancement control
         */
        virtual status_t SetSpeechEnhancement(bool enhance_on);
        virtual status_t SetSpeechEnhancementMask(const sph_enh_mask_struct_t &mask);

        virtual status_t SetBtHeadsetNrecOn(const bool bt_headset_nrec_on);


        /**
         * speech enhancement parameters setting
         */
        virtual status_t SetAllSpeechEnhancementInfoToModem(); // only available for LAD

        virtual status_t SetVariousKindsOfSpeechParameters(const void *param, const uint16_t data_length, const uint16_t ccci_message_id); // only available for LAD
        virtual status_t SetNBSpeechParameters(const AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB);
//#if defined(MTK_DUAL_MIC_SUPPORT)
        virtual status_t SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic);
//#endif
        virtual status_t SetMagiConSpeechParameters(const AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT *pSphParamMagiCon);
        virtual status_t SetHACSpeechParameters(const AUDIO_CUSTOM_HAC_PARAM_STRUCT *pSphParamHAC);
//#if defined(MTK_WB_SPEECH_SUPPORT)
        virtual status_t SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB);
//#endif
        //#if defined(MTK_VIBSPK_SUPPORT)
        virtual status_t GetVibSpkParam(void *eVibSpkParam);
        virtual status_t SetVibSpkParam(void *eVibSpkParam);
        //#endif

        virtual status_t GetNxpSmartpaParam(void *eParamNxpSmartpa);
        virtual status_t SetNxpSmartpaParam(void *eParamNxpSmartpa);
        virtual status_t SetDynamicSpeechParameters(const int type, const void* param_arg);

        /**
         * check whether modem is ready.
         */
        virtual bool     CheckModemIsReady();

        virtual bool QueryModemRFInfo();

        /**
         * debug info
         */
        virtual status_t ModemDumpSpeechParam();



    protected:
        SpeechDriverLAD() {}
        SpeechDriverLAD(modem_index_t modem_index);


        /**
         * recover status (speech/record/bgs/vt/p2w/tty)
         */
        virtual void RecoverModemSideStatusToInitState();


        /**
         * Clean gain value and mute status
         */
        virtual status_t CleanGainValueAndMuteStatus();


        /**
         * CCCI Messenger
         */
        SpeechMessengerInterface *pCCCI;


        /**
         * Speech Mode
         */
        speech_mode_t mSpeechMode;

        int16_t          mVolumeIndex;


    private:
        /**
         * singleton pattern
         */
        static SpeechDriverLAD *mLad1;
        static SpeechDriverLAD *mLad2;
        static SpeechDriverLAD *mLad3;
};

} // end namespace android

#endif // end of ANDROID_SPEECH_DRIVER_LAD_H
