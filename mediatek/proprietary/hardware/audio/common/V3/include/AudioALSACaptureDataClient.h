#ifndef ANDROID_AUDIO_ALSA_CAPTURE_DATA_CLIENT_H
#define ANDROID_AUDIO_ALSA_CAPTURE_DATA_CLIENT_H

#include "AudioType.h"
#include "AudioLock.h"
#include "AudioUtility.h"
#ifdef MTK_BASIC_PACKAGE
#include "AudioTypeExt.h"
#endif

//BesRecord process +++
#include "AudioSpeechEnhLayer.h"
//#include "AudioALSAVolumeController.h"
#include "AudioVolumeInterface.h"
#include "AudioSpeechEnhanceInfo.h"

//BesRecord process ---

//Android Native Preprocess effect +++
#include "AudioPreProcess.h"
//Android Native Preprocess effect ---

namespace android
{

class AudioALSACaptureDataProviderBase;
class MtkAudioSrc;
class AudioALSACaptureDataProviderEchoRef;

/// Observer pattern: Observer
class AudioALSACaptureDataClient
{
    public:
        AudioALSACaptureDataClient(AudioALSACaptureDataProviderBase *pCaptureDataProvider, stream_attribute_t *stream_attribute_target);
        virtual ~AudioALSACaptureDataClient();


        /**
         * set client index
         */
        void         setIdentity(const uint32_t identity);
        inline uint32_t     getIdentity() const { return mIdentity; }


        /**
         * get / process / offer data
         */
        virtual uint32_t    copyCaptureDataToClient(RingBuf pcm_read_buf); // called by capture data provider


        /**
         * read data from audio hardware
         */
        virtual ssize_t     read(void *buffer, ssize_t bytes); // called by capture handler

        //EchoRef+++
        /**
         * get / process / offer data
         */
        virtual uint32_t    copyEchoRefCaptureDataToClient(RingBuf pcm_read_buf); // called by capture data provider

        void AddEchoRefDataProvider(AudioALSACaptureDataProviderBase *pCaptureDataProvider, stream_attribute_t *stream_attribute_target);
        //EchoRef---

        /**
         * Update BesRecord Parameters
         */
        status_t UpdateBesRecParam();

        /**
         * check if the attached client has low latency requirement
         */
        bool	IsLowLatencyCapture(void);

    private:
        AudioALSACaptureDataClient() {}
        AudioALSACaptureDataProviderBase *mCaptureDataProvider;

        status_t ApplyVolume(void *Buffer , uint32_t BufferSize);

        enum channel_remix_operation {
            CHANNEL_REMIX_NOP = 0,
            CHANNEL_STEREO_CROSSMIX_L2R,
            CHANNEL_STEREO_CROSSMIX_R2L,
            CHANNEL_STEREO_DOWNMIX,
            CHANNEL_STEREO_DOWNMIX_L_ONLY,
            CHANNEL_STEREO_DOWNMIX_R_ONLY,
            CHANNEL_MONO_TO_STEREO,
        };

        uint32_t mIdentity; // key for mCaptureDataClientVector
        bool mIsIdentitySet;

        AudioLock mLock;
        AudioCondition mWaitWorkCV;

        /**
         * Speech EnhanceInfo Instance
         */
        AudioSpeechEnhanceInfo *mAudioSpeechEnhanceInfoInstance;

        /**
         * attribute
         */
        const stream_attribute_t *mStreamAttributeSource; // from audio hw
        stream_attribute_t *mStreamAttributeTarget; // to stream in


        /**
         * local ring buffer
         */
        RingBuf             mRawDataBuf;
        RingBuf             mSrcDataBuf;
        RingBuf             mProcessedDataBuf;


        /**
         * Bli SRC
         */
        MtkAudioSrc *mBliSrc;

        bool mMicMute;
        bool mMuteTransition;

        //BesRecord Move to here for temp
        SPELayer    *mSPELayer;

        //#ifdef MTK_AUDIO_HD_REC_SUPPORT
        //BesRecord process +++
        //Load BesRecord parameters from NVRam
        void LoadBesRecordParams(void);
        int CheckBesRecordMode(void);
        void ConfigBesRecordParams(void);
        void StartBesRecord(void);
        void StopBesRecord(void);
        uint32_t BesRecordPreprocess(void *buffer , uint32_t bytes);
        int GetBesRecordRoutePath(void);
        int SetCaptureGain(void);
        bool CheckBesRecordBypass(void);
        bool CheckNeedBesRecordSRC(void);
        bool IsVoIPEnable(void);
        bool IsNeedChannelRemix() { return (mChannelRemixOp != CHANNEL_REMIX_NOP); }
        void CheckChannelRemixOp(void);
        ssize_t ApplyChannelRemix(short *buffer, size_t bytes);
        ssize_t ApplyChannelRemixWithRingBuf(RingBuf *srcBuffer, RingBuf *dstBuffer);
        void CheckBesRecordStereoModeEnable(void);

        timespec GetCaptureTimeStamp(void);
        timespec GetEchoRefTimeStamp(void);

        bool CheckDynamicSpeechEnhancementMaskOnOff(const voip_sph_enh_dynamic_mask_t dynamic_mask_type);
        void SetDMNREnable(DMNR_TYPE type, bool enable);
        void CheckDynamicSpeechMask(void);
        void UpdateDynamicFunction(void);

#if ((!defined(MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)) || (MTK_AUDIO_TUNING_TOOL_V2_PHASE == 1))
        AUDIO_HD_RECORD_SCENE_TABLE_STRUCT mBesRecordSceneTable;
        AUDIO_HD_RECORD_PARAM_STRUCT mBesRecordParam;
        AUDIO_VOIP_PARAM_STRUCT mVOIPParam;
        AUDIO_CUSTOM_EXTRA_PARAM_STRUCT mDMNRParam;

        int mBesRecordModeIndex;
        int mBesRecordSceneIndex;
#endif

        AudioVolumeInterface *mAudioALSAVolumeController;
        bool mBesRecordStereoMode;
        bool mBypassBesRecord;
        bool mNeedBesRecordSRC;
        uint32_t dropBesRecordDataSize;

        SPE_MODE mSpeechProcessMode;
        voip_sph_enh_mask_struct_t mVoIPSpeechEnhancementMask;
        uint32_t mChannelRemixOp;
        //Audio tuning tool
        bool mBesRecTuningEnable;
        char m_strTuningFileName[VM_FILE_NAME_LEN_MAX];
        /**
         * Bli SRC
         */
        MtkAudioSrc *mBliSrcHandler1;
        MtkAudioSrc *mBliSrcHandler2;
        int mBesRecSRCSizeFactor;
        int mBesRecSRCSizeFactor2;
        //BesRecord process ---
        //#endif

        //Android Native Preprocess effect +++
        AudioPreProcess *mAudioPreProcessEffect;
        uint32_t NativePreprocess(void *buffer , uint32_t bytes);
        void CheckNativeEffect(void);
        //Android Native Preprocess effect ---

        //EchoRef+++
        AudioALSACaptureDataProviderBase *mCaptureDataProviderEchoRef;

        /**
                 * attribute
             */
        const stream_attribute_t *mStreamAttributeSourceEchoRef; // from audio hw, need the same as DL1 stream out used
        stream_attribute_t *mStreamAttributeTargetEchoRef; // to stream in

        /**
         * local ring buffer
         */
        RingBuf             mEchoRefRawDataBuf;
        RingBuf             mEchoRefSrcDataBuf;

        /**
         * Bli SRC
         */
        MtkAudioSrc *mBliSrcEchoRef;
        MtkAudioSrc *mBliSrcEchoRefBesRecord;
        //EchoRef---
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_DATA_CLIENT_H
