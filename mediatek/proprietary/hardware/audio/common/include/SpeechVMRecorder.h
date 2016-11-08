#ifndef ANDROID_SPEECH_VM_RECORDER_H
#define ANDROID_SPEECH_VM_RECORDER_H

//#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware_legacy/AudioMTKHardwareInterface.h>

#include "AudioType.h"
#include "CFG_AUDIO_File.h"
#include "AudioUtility.h"

#include "AudioLock.h"

namespace android
{

class SpeechVMRecorder
{
    public:
        virtual ~SpeechVMRecorder();

        static SpeechVMRecorder *GetInstance();

        status_t Open();
        status_t Close();

        uint16_t CopyBufferToVM(RingBuf ul_ring_buf);

        bool GetVMRecordStatus() const { return mEnable; } // true for open, false for close

        void SetVMRecordCapability(const AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB);        
        void SetVMRecordCapability(const uint16_t mVMConfig);
        void UpdateVMRecordCapability();

        bool GetVMRecordCapability() const; // true for support
        bool GetVMRecordCapabilityForCTM4Way() const; // true for support
        void GetCtmDebugDataFromModem(RingBuf ul_ring_buf, FILE *pFile);

        int  StartCtmDebug();
        int  StopCtmDebug();
        FILE *pCtmDumpFileUlIn;
        FILE *pCtmDumpFileDlIn;
        FILE *pCtmDumpFileUlOut;
        FILE *pCtmDumpFileDlOut;

    protected:
        SpeechVMRecorder();

        status_t OpenFile();
        static void *DumpVMRecordDataThread(void *arg);
        void TriggerVMRecord();

        bool mStarting;
        bool mEnable;

        RingBuf mRingBuf; // Internal Input Buffer for Get VM data
        FILE *mDumpFile;

        pthread_t mRecordThread;
        AudioLock mMutex;
        AudioCondition mExitCond;

        uint16_t mAutoVM;

    private:
        static SpeechVMRecorder *mSpeechVMRecorder; // singleton
        bool   m_CtmDebug_Started;
}; // end of class SpeechVMRecorder

}; // end of namespace android

#endif // end of ANDROID_SPEECH_VM_RECORDER_H
