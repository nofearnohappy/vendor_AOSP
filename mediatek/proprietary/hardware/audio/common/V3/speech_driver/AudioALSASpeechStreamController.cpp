
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
#define MTK_LOG_ENABLE 1
#include <unistd.h>
#include <sched.h>
#include <sys/prctl.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <media/AudioSystem.h>
#include <cutils/properties.h>
#include <cutils/log.h>
#include "AudioALSAHardwareResourceManager.h"

#include "SpeechDriverInterface.h"
#include "SpeechDriverFactory.h"
#include "AudioALSAStreamOut.h"
#include "AudioALSAStreamIn.h"
#include "AudioALSASpeechStreamController.h"
#define LOG_TAG "AudioALSASpeechStreamController"

#define PROCESS_BLOCK_SIZE 512
#define READ_STREAM_LENGTH (PROCESS_BLOCK_SIZE * 2)
namespace android
{

/*==============================================================================
 *                     Property keys
 *============================================================================*/
AudioALSASpeechStreamController *AudioALSASpeechStreamController::UniqueInstance = NULL;
AudioALSASpeechStreamController *AudioALSASpeechStreamController::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (UniqueInstance == NULL)
    {
        UniqueInstance = new AudioALSASpeechStreamController();
    }
    ASSERT(UniqueInstance != NULL);
    return UniqueInstance;
}

AudioALSASpeechStreamController::AudioALSASpeechStreamController()
{
    int ret;
    ALOGD("%s()", __FUNCTION__);
    
    m_bEnabled = false;
    m_bThreadExit = false;
    mOutputDevices = AUDIO_DEVICE_NONE;
    
    mAudioMtkStreamManager = AudioALSAStreamManager::getInstance();
    
    ret = pthread_mutex_init(&mSpeechStreamMutex, NULL);
    if (ret != 0)
    {
        SLOGE("Failed to initialize mSpeechStreamMutex!");
    }

    ret = pthread_cond_init(&mSphStream_Cond, NULL);
    if (ret != 0)
    {
        SLOGE("Failed to initialize mSphStream_Cond!");
    }
}

AudioALSASpeechStreamController::~AudioALSASpeechStreamController()
{
    ALOGD("%s()", __FUNCTION__);
    pthread_cond_destroy(&mSphStream_Cond);
}

void *AudioALSASpeechStreamController::SpeechStreamThread(void *arg)
{
    SLOGD("%s() +", __FUNCTION__);
    uint32_t device = AUDIO_DEVICE_IN_BUILTIN_MIC;
    status_t status = 0;
    int format = AUDIO_FORMAT_PCM_16_BIT;
    uint32_t channel = AUDIO_CHANNEL_IN_STEREO;
    uint32_t sampleRate = 48000;
    short readBuffer[PROCESS_BLOCK_SIZE];//for record
    char *pReadBuffer = (char *)readBuffer;
    int nRead = 0;
    memset(pReadBuffer, 0, sizeof(short)*PROCESS_BLOCK_SIZE);
    android_audio_legacy::AudioStreamIn *streamInput = NULL;
    android_audio_legacy::AudioStreamOut *streamOutput = NULL;
    AudioALSASpeechStreamController *pSphStrmCtrl = static_cast<AudioALSASpeechStreamController *>(arg);
    if(pSphStrmCtrl == NULL) {
        SLOGE("SpeechStreamThread pSphStrmCtrl = NULL arg = %x", arg);
        return 0;
    }
    pthread_mutex_lock(&pSphStrmCtrl->mSpeechStreamMutex);
    pSphStrmCtrl->m_bThreadExit = false;
    // Adjust thread priority
    prctl(PR_SET_NAME, (unsigned long)"SpeechStreamThread", 0, 0, 0);
    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_AUDIO);

    // ----start the loop --------
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    streamInput = pSphStrmCtrl->getStreamManager()->openInputStream(device, &format, &channel, &sampleRate, &status, (android_audio_legacy::AudioSystem::audio_in_acoustics)0);
    ASSERT(streamInput != NULL);
    streamInput->setParameters(String8("MOD_DAI_INPUT=1"));
    device = AUDIO_DEVICE_OUT_SPEAKER;
    channel = AUDIO_CHANNEL_OUT_STEREO;
    streamOutput = pSphStrmCtrl->getStreamManager()->openOutputStream(device, &format, &channel, &sampleRate, &status);
    ASSERT(streamOutput != NULL);
    streamOutput->setParameters(String8("SPH_DL_OUTPUT=1"));
    
    SLOGD("pthread_cond_signal(&pSpkMonitor->mSpkMonitor_Cond)");
    pthread_cond_signal(&pSphStrmCtrl->mSphStream_Cond); // wake all thread
    pthread_mutex_unlock(&pSphStrmCtrl->mSpeechStreamMutex);
    SLOGD("%s() loop start", __FUNCTION__);
    streamOutput->write(pReadBuffer, READ_STREAM_LENGTH);//Write silent data
    //streamOutput->write(pReadBuffer, READ_STREAM_LENGTH);
    //streamOutput->write(pReadBuffer, READ_STREAM_LENGTH);
    //streamOutput->write(pReadBuffer, READ_STREAM_LENGTH);
    
    while(pSphStrmCtrl->m_bEnabled && pSphStrmCtrl->m_bThreadExit == false) {
        if(streamInput!= NULL)
        {
            nRead = streamInput->read(pReadBuffer, READ_STREAM_LENGTH);
            SLOGD("streamin read %d", nRead);
            if(streamOutput != NULL && pSphStrmCtrl->m_bEnabled == true && pSphStrmCtrl->m_bThreadExit == false)
            {
                streamOutput->write(pReadBuffer, nRead);
            }
        }
    }
    SLOGD("%s() loop end", __FUNCTION__);
     
    //exit thread
    if(streamOutput != NULL)
    {
        streamOutput->standby();
        pSphStrmCtrl->getStreamManager()->closeOutputStream(streamOutput);
        streamOutput = NULL;
    }
    if(streamInput != NULL)
    {
        streamInput->standby();
        pSphStrmCtrl->getStreamManager()->closeInputStream(streamInput);
        streamInput = NULL;
    }
    
    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    //exit thread
    pthread_mutex_lock(&pSphStrmCtrl->mSpeechStreamMutex);
    SLOGD("pthread_cond_signal(&pSphStrmCtrl->mSpkMonitor_Cond)");
    pthread_cond_signal(&pSphStrmCtrl->mSphStream_Cond); // wake all thread
    pthread_mutex_unlock(&pSphStrmCtrl->mSpeechStreamMutex);
    pthread_exit(NULL);
    return 0;
}

audio_devices_t AudioALSASpeechStreamController::GetStreamOutputDevice(void)
{
    return mOutputDevices;
}

status_t AudioALSASpeechStreamController::SetStreamOutputDevice(audio_devices_t OutputDevices)
{
    ALOGD("%s(), %d", __FUNCTION__, OutputDevices);
    mOutputDevices = OutputDevices;
    return NO_ERROR;
}

bool AudioALSASpeechStreamController::IsSpeechStreamThreadEnable(void)
{
    ALOGD("%s(), %d", __FUNCTION__, m_bEnabled);
    return m_bEnabled;
}

status_t AudioALSASpeechStreamController::EnableSpeechStreamThread(bool enable)
{
    struct timeval now;
    struct timespec timeout;
    gettimeofday(&now, NULL);
    timeout.tv_sec = now.tv_sec + 3;
    timeout.tv_nsec = now.tv_usec * 1000;
    int ret;
    ALOGD("%s() %d", __FUNCTION__, enable);
    
    if(enable == true && m_bEnabled == false)
    {
        ALOGD("open SpeechStreamThread");
        pthread_mutex_lock(&mSpeechStreamMutex);
        ret = pthread_create(&mSpeechStreamThreadID, NULL, AudioALSASpeechStreamController::SpeechStreamThread, (void *)this);
        if (ret != 0)
        {
            ALOGE("EnableSpeechStreamThread pthread_create error!!");
        }

        ALOGD("+mSphStream_Cond wait");
        m_bEnabled = true;
        ret = pthread_cond_timedwait(&mSphStream_Cond, &mSpeechStreamMutex, &timeout);
        ALOGD("-mSphStream_Cond receive ret=%d", ret);
        
        pthread_mutex_unlock(&mSpeechStreamMutex);
        
    }
    else if(enable == false && m_bEnabled == true)
    { 
        //stop thread
        ALOGD("close SpeechStreamThread");
        pthread_mutex_lock(&mSpeechStreamMutex);
        if (!m_bThreadExit)
        {
            m_bThreadExit = true;
            ALOGD("+mSphStream_Cond wait");
            ret = pthread_cond_timedwait(&mSphStream_Cond, &mSpeechStreamMutex, &timeout);
            ALOGD("-mSphStream_Cond receive ret=%d", ret);
        }
        m_bEnabled = false;
        pthread_mutex_unlock(&mSpeechStreamMutex);
    }
    return NO_ERROR;
}

} // end of namespace android
#endif //MTK_SPEAKER_MONITOR_SPEECH_SUPPORT