#ifndef ANDROID_SPEECH_DRIVER_INTERFACE_H
#define ANDROID_SPEECH_DRIVER_INTERFACE_H

#include <utils/threads.h>

#include "AudioType.h"
#include "SpeechType.h"

#include "CFG_AUDIO_File.h"

namespace android
{
class SpeechDriverInterface
{
    public:
        virtual ~SpeechDriverInterface() {}

        /**
         * speech control
         */
        virtual status_t SetSpeechMode(const audio_devices_t input_device, const audio_devices_t output_device) = 0;
        virtual status_t SpeechOn() = 0;
        virtual status_t SpeechOff() = 0;
        virtual status_t VideoTelephonyOn() = 0;
        virtual status_t VideoTelephonyOff() = 0;
        virtual status_t SpeechRouterOn() = 0;
        virtual status_t SpeechRouterOff() = 0;
        virtual status_t setMDVolumeIndex(int value, int device, int index) = 0;


        /**
         * record control
         */
        virtual status_t RecordOn() = 0;
        virtual status_t RecordOff() = 0;

        virtual status_t VoiceMemoRecordOn() = 0;
        virtual status_t VoiceMemoRecordOff() = 0;

        virtual uint16_t GetRecordSampleRate() const = 0;
        virtual uint16_t GetRecordChannelNumber() const = 0;
        
        virtual status_t RecordOn(record_type_t type_record) = 0;
        virtual status_t RecordOff(record_type_t type_record) = 0;

        virtual status_t SetPcmRecordType(record_type_t type_record) = 0;

        /**
         * background sound control
         */
        virtual status_t BGSoundOn() = 0;
        virtual status_t BGSoundConfig(uint8_t ul_gain, uint8_t dl_gain) = 0;
        virtual status_t BGSoundOff() = 0;


        /**
         * pcm 2 way
         */
        virtual status_t PCM2WayPlayOn() = 0;
        virtual status_t PCM2WayPlayOff() = 0;
        virtual status_t PCM2WayRecordOn() = 0;
        virtual status_t PCM2WayRecordOff() = 0;
        virtual status_t PCM2WayOn(const bool wideband_on) = 0;
        virtual status_t PCM2WayOff() = 0;
        virtual status_t DualMicPCM2WayOn(const bool wideband_on, const bool record_only) = 0;
        virtual status_t DualMicPCM2WayOff() = 0;

        /**
         * tty ctm control
         */
        virtual status_t TtyCtmOn(ctm_interface_t ctm_interface) = 0;
        virtual status_t TtyCtmOff() = 0;
        virtual status_t TtyCtmDebugOn(bool tty_debug_flag) = 0;


        /**
         * acoustic loopback
         */
        virtual status_t SetAcousticLoopback(bool loopback_on) = 0;
        virtual status_t SetAcousticLoopbackBtCodec(bool enable_codec) = 0;

        virtual status_t SetAcousticLoopbackDelayFrames(int32_t delay_frames) = 0;

        /**
         * volume control
         */
        virtual status_t SetDownlinkGain(int16_t gain) = 0;
        virtual status_t SetEnh1DownlinkGain(int16_t gain) = 0;
        int16_t GetDownlinkGain(void) {return mDownlinkGain;}
        int16_t GetEnh1DownlinkGain(void) {return mDownlinkenh1Gain;}
        virtual status_t SetUplinkGain(int16_t gain) = 0;
        virtual status_t SetDownlinkMute(bool mute_on) = 0;
        virtual status_t SetUplinkMute(bool mute_on) = 0;
        virtual status_t SetUplinkSourceMute(bool mute_on) = 0;
        virtual status_t SetSidetoneGain(int16_t gain) = 0;
        virtual status_t SetDSPSidetoneFilter(const bool dsp_stf_on) { return NO_INIT; }


        /**
         * device related config
         */
        virtual status_t SetModemSideSamplingRate(uint16_t sample_rate) = 0;


        /**
         * speech enhancement control
         */
        virtual void     SetForceDisableSpeechEnhancement(bool force_disable_on) { mForceDisableSpeechEnhancement = force_disable_on; }
        virtual status_t SetSpeechEnhancement(bool enhance_on) = 0;
        virtual status_t SetSpeechEnhancementMask(const sph_enh_mask_struct_t &mask) = 0;

        virtual status_t SetBtHeadsetNrecOn(const bool bt_headset_nrec_on) = 0;


        /**
         * speech enhancement parameters setting
         */
        virtual status_t SetNBSpeechParameters(const AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB) = 0;
//#if defined(MTK_DUAL_MIC_SUPPORT)
        virtual status_t SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic) = 0;
//#endif
        virtual status_t SetMagiConSpeechParameters(const AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT *pSphParamMagiCon) = 0;
        virtual status_t SetHACSpeechParameters(const AUDIO_CUSTOM_HAC_PARAM_STRUCT *pSphParamHAC) = 0;
//#if defined(MTK_WB_SPEECH_SUPPORT)
        virtual status_t SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB) = 0;
//#endif
        //#if defined(MTK_VIBSPK_SUPPORT)
        virtual status_t GetVibSpkParam(void *eVibSpkParam) = 0;
        virtual status_t SetVibSpkParam(void *eVibSpkParam) = 0;
        //#endif
        virtual status_t SetDynamicSpeechParameters(const int type, const void* param_arg) = 0;


        /**
         * check whether modem is ready.
         */
        virtual bool     CheckModemIsReady() = 0;


        /**
         * debug info
         */
        virtual status_t ModemDumpSpeechParam() = 0;


        /**
         * get AP side modem function status
         */
        inline bool      GetApSideModemStatus(const modem_status_mask_t modem_status_mask) const
        {
            return ((mApSideModemStatus & modem_status_mask) > 0);
        }


        /**
         * speech driver synchronization
         */
        void             WaitUntilSignaledOrTimeout(unsigned milisecond) { mMutex.lock(); mCondition.waitRelative(mMutex, milliseconds(milisecond)); mMutex.unlock(); }
        void             Signal() { mMutex.lock(); mCondition.signal(); mMutex.unlock();}
        virtual void SetWarningTone(int toneid) { return; }
        virtual void StopWarningTone() {return;}
    protected:
        SpeechDriverInterface()
        {
            mDownlinkGain   = 0x8000;
            mDownlinkenh1Gain = 0x8000;
            mUplinkGain     = 0;
            mSideToneGain   = 0;
            mUplinkMuteOn   = false;
            mUplinkSourceMuteOn   = false;
            mDownlinkMuteOn = false;
            mPCM2WayState = 0;
            mApSideModemStatus = 0;
            mRecordSampleRateType = RECORD_SAMPLE_RATE_08K;
            mRecordChannelType    = RECORD_CHANNEL_MONO;
            mBtHeadsetNrecOn = true;
            mAcousticLoopbackDelayFrames = 0;
            mForceDisableSpeechEnhancement = false;
            mRecordType = RECORD_TYPE_MIX;
            mUseBtCodec = 1;
            mModemIndex = MODEM_1;
        }


        /**
         * recover modem side status (speech/record/bgs/vt/p2w/tty)
         */
        virtual void     RecoverModemSideStatusToInitState() = 0;


        /**
         * set/reset AP side modem function status
         */
        inline void      SetApSideModemStatus(const modem_status_mask_t modem_status_mask)
        {
            ASSERT(GetApSideModemStatus(modem_status_mask) == false);
            mApSideModemStatus |= modem_status_mask;
        }
        inline void      ResetApSideModemStatus(const modem_status_mask_t modem_status_mask)
        {
            ASSERT(GetApSideModemStatus(modem_status_mask) == true);
            mApSideModemStatus &= (~modem_status_mask);
        }


        /**
         * check AP side modem function status
         */
        inline void      CheckApSideModemStatusAllOffOrDie();


        /**
         * class variables
         */
        modem_index_t    mModemIndex;

        int16_t          mDownlinkGain;
        int16_t          mDownlinkenh1Gain;
        int16_t          mUplinkGain;
        int16_t          mSideToneGain;

        bool             mDownlinkMuteOn;
        bool             mUplinkMuteOn;
        bool             mUplinkSourceMuteOn;

        uint32_t         mPCM2WayState; // value |= pcmnway_format_t

        // Modem function status : not the modem real status but AP side control status
        uint32_t         mApSideModemStatus; // value |= modem_status_mask_t

        bool             mForceDisableSpeechEnhancement;

        Mutex            mMutex;
        Condition        mCondition;

        // Record capability
        record_sample_rate_t mRecordSampleRateType;
        record_channel_t     mRecordChannelType;
        record_type_t        mRecordType;

        //for BT SW BT CVSD loopback test
        bool mUseBtCodec;

        // BT Headset NREC
        bool mBtHeadsetNrecOn;

        // loopback delay frames (1 frame = 20 ms)
        uint32_t mAcousticLoopbackDelayFrames;
};


void SpeechDriverInterface::CheckApSideModemStatusAllOffOrDie()
{
    if (mApSideModemStatus != 0)
    {
        ASSERT(GetApSideModemStatus(SPEECH_STATUS_MASK)   != true);
        ASSERT(GetApSideModemStatus(RECORD_STATUS_MASK)   != true);
        ASSERT(GetApSideModemStatus(BGS_STATUS_MASK)      != true);
        ASSERT(GetApSideModemStatus(P2W_STATUS_MASK)      != true);
        ASSERT(GetApSideModemStatus(TTY_STATUS_MASK)      != true);
        ASSERT(GetApSideModemStatus(VT_STATUS_MASK)       != true);
        ASSERT(GetApSideModemStatus(LOOPBACK_STATUS_MASK) != true);
        ASSERT(GetApSideModemStatus(RAW_RECORD_STATUS_MASK)   != true);
    }
}


} // end namespace android

#endif // end of ANDROID_SPEECH_DRIVER_INTERFACE_H
