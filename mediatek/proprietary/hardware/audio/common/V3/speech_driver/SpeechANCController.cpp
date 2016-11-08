#include "SpeechANCController.h"
#include "AudioALSAHardwareResourceManager.h"
#include "audio_custom_exp.h"
#include "AudioCustParam.h"
#include "AudioType.h"
#include "AudioALSADriverUtility.h"
#include "AudioALSAHardware.h"
#include <pthread.h>
#include <linux/rtpm_prio.h>
#include <sys/prctl.h>
#include "AudioALSADeviceParser.h"

#define LOG_TAG "SpeechANCController"
#define param_anc_add
//#define FAKE_GROUPANC

namespace android
{

static const char     kPrefixOfANCFileName[] = "/sdcard/mtklog/audio_dump/ANCLog";
static const uint32_t kSizeOfPrefixOfANCFileName = sizeof(kPrefixOfANCFileName) - 1;
static const uint32_t kReadBufferSize = 0x2000; // 8k
static const uint32_t kMaxSizeOfANCFileName = 128;
static FILE *mDumpFile_MOD = NULL;
static FILE *mDumpFile_IO2 = NULL;
static FILE *mDumpFile_ADC2 = NULL;

/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

SpeechANCController *SpeechANCController::UniqueSpeechANCController = NULL;


SpeechANCController *SpeechANCController::getInstance()
{
    static Mutex mGetInstanceLock;
    Mutex::Autolock _l(mGetInstanceLock);
    ALOGD("%s()", __FUNCTION__);

    if (UniqueSpeechANCController == NULL)
    {
        UniqueSpeechANCController = new SpeechANCController();
    }
    ASSERT(UniqueSpeechANCController != NULL);
    return UniqueSpeechANCController;
}
/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

SpeechANCController::SpeechANCController()
{
    ALOGD("%s()", __FUNCTION__);
    mEnabled       = false;
    mGroupANC      = false;
#if defined(MTK_ACTIVE_NOISE_CANCELLATION_SUPPORT)
    Init();
#endif
}

SpeechANCController::~SpeechANCController()
{
    ALOGD("%s()", __FUNCTION__);
#if defined(MTK_ACTIVE_NOISE_CANCELLATION_SUPPORT)
    if (mFd)
    {
        ::close(mFd);
        mFd = 0;
    }
#endif
}

/*==============================================================================
 *                     AudioANCControl Imeplementation
 *============================================================================*/
bool SpeechANCController::GetANCSupport(void)
{
    ALOGD("%s(), GetANCSupport:%d", __FUNCTION__, mApply);
    //TODO(Tina): return by project config

#if defined(MTK_ACTIVE_NOISE_CANCELLATION_SUPPORT)
    return true;
#else
    return false;
#endif

}

#if defined(MTK_ACTIVE_NOISE_CANCELLATION_SUPPORT)

void SpeechANCController::Init()
{
    ALOGD("%s()", __FUNCTION__);
    mFd            = ::open(kANCDeviceName, O_RDWR);
    if (mFd < 0)
    {
        ALOGE("%s() fail to open %s", __FUNCTION__, kANCDeviceName);
    }
    else
    {
        ALOGD("%s() open %s success!", __FUNCTION__, kANCDeviceName);

        ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_Init);
    }
    mPcmIn_MOD = NULL;
    mPcmIn_IO2 = NULL;
    mPcmIn_ADC2 = NULL;
    mDumpFile_MOD = NULL;
    mDumpFile_IO2 = NULL;
    mDumpFile_ADC2 = NULL;

    mEnable_ANCLog_MOD = false;
    mEnable_ANCLog_IO2 = false;
    mEnable_ANCLog_ADC2 = false;
#ifdef param_anc_add
    AUDIO_ANC_CUSTOM_PARAM_STRUCT pSphParamAnc;
    Mutex::Autolock _l(mMutex);
    GetANCSpeechParamFromNVRam(&pSphParamAnc);
    mLogEnable     = pSphParamAnc.ANC_log;
    mLogDownSample = pSphParamAnc.ANC_log_downsample;
    mApply         = pSphParamAnc.ANC_apply;

    SetCoefficients(pSphParamAnc.ANC_para);
#else
    mLogEnable     = false;
    mLogDownSample = false;
    mApply         = false;

#endif
}


void SpeechANCController::SetCoefficients(void *buf)
{
    ALOGD("%s(), SetCoefficients:%d", __FUNCTION__);
    ::ioctl(mFd, SET_ANC_PARAMETER, buf);
}

void SpeechANCController::SetApplyANC(bool apply)
{
    //if mmi selected, set flag and enable/disable anc
    ALOGD("%s(), SetApply:%d", __FUNCTION__, apply);

    if (apply ^ mApply)
    {
#ifdef param_anc_add
        AUDIO_ANC_CUSTOM_PARAM_STRUCT pSphParamAnc;
        Mutex::Autolock _l(mMutex);
        GetANCSpeechParamFromNVRam(&pSphParamAnc);
        pSphParamAnc.ANC_apply = apply;
        SetANCSpeechParamToNVRam(&pSphParamAnc);
#endif
        mApply = apply;
    }
}

bool SpeechANCController::GetApplyANC(void)
{
    //get compile option and return
    ALOGD("%s(), mApply:%d", __FUNCTION__, mApply);
    return mApply;
}

void SpeechANCController::SetEanbleANCLog(bool enable, bool downsample)
{
    ALOGD("%s(), enable:%d, downsample(%d)", __FUNCTION__, enable, downsample);
    if (enable ^ mLogEnable || mLogDownSample ^ downsample)
    {
#ifdef param_anc_add
        AUDIO_ANC_CUSTOM_PARAM_STRUCT pSphParamAnc;
        Mutex::Autolock _l(mMutex);
        GetANCSpeechParamFromNVRam(&pSphParamAnc);
        pSphParamAnc.ANC_log = enable;
        pSphParamAnc.ANC_log_downsample = downsample;
        SetANCSpeechParamToNVRam(&pSphParamAnc);
#endif
        mLogEnable = enable;
        mLogDownSample = downsample;
        if (enable)
        {
            ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_EnableLog);
        }
        else
        {
            ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_DisableLog);
        }
    }
}

bool SpeechANCController::GetEanbleANCLog(void)
{
    ALOGD("%s(), mLogEnable:%d", __FUNCTION__, mLogEnable);
    return mLogEnable;
}

bool SpeechANCController::GetEanbleANCLogDownSample(void)
{
    ALOGD("%s(), mLogDownSample:%d", __FUNCTION__, mLogDownSample);
    return mLogDownSample;
}

bool SpeechANCController::EanbleANC(bool enable)
{
    int ret;

    ALOGD("%s(), mEnabled(%d), enable(%d)", __FUNCTION__, mEnabled, enable);


    if (!mGroupANC)
    {
        ALOGD("%s(), EnableError, Not ANC group", __FUNCTION__);
        return false;
    }
    if (enable ^ mEnabled)
    {
        Mutex::Autolock _l(mMutex);
        if (enable)
        {
            ret = ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_Enable);
        }
        else
        {
            ret = ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_Disable);
        }
        if (ret == -1)
        {
            ALOGD("%s(), EnableFail:%d", __FUNCTION__, ret);
            return false;
        }
        mEnabled = enable;
    }
    return true;
}

bool SpeechANCController::CloseANC(void)
{
    int ret;
    ALOGD("%s()", __FUNCTION__);
    if (!mGroupANC)
    {
        ALOGD("%s(), CloseError, Not ANC group", __FUNCTION__);
        return false;
    }
    Mutex::Autolock _l(mMutex);
    ret = ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_Close);
    if (ret == -1)
    {
        ALOGD("%s(), EnableFail:%d", __FUNCTION__, ret);
        return false;
    }
    mEnabled = false;
    return true;
}

bool SpeechANCController::SwapANC(bool swap2anc)
{
    int ret;
    ALOGD("%s(), mGroupANC(%d), swap2anc(%d)", __FUNCTION__, mGroupANC, swap2anc);
    if (mGroupANC ^ swap2anc)
    {
        if (swap2anc)
        {
            ret = ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_SwapToANC);
        }
        else
        {
            ret = ::ioctl(mFd, SET_ANC_CONTROL, ANCControlCmd_SwapFromANC);
        }

#ifdef FAKE_GROUPANC
        mGroupANC = swap2anc;
#endif

        if (ret == -1)
        {
            ALOGD("%s(), SWAPFail:%d", __FUNCTION__, ret);
            return false;
        }
        mGroupANC = swap2anc;
    }
    return true;
}

uint32_t SpeechANCController::ConfigPCM(String8 stringPCM, int *buffer_size)
{
    struct pcm_params *params;
    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(stringPCM);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(stringPCM);
    params = pcm_params_get(cardindex, pcmindex,  PCM_IN);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    *buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    pcm_params_free(params);
    //    ALOGD("%s(), pcmindex(%d), cardindex(%d), buffersizemax(%d)", __FUNCTION__, pcmindex, cardindex, *buffer_size);
    ALOGD("%s(), %s = pcmindex(%d), cardindex(%d), buffersizemax(%d)", __FUNCTION__, stringPCM.string(), pcmindex, cardindex, *buffer_size);

    return pcmindex;
}


bool SpeechANCController::StartPCMIn(char mTypePCM, uint32_t device, pcm_config mConfig)
{
    ALOGD("+%s(), pcm device = %d", __FUNCTION__, device);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    pcm *mPcm;

    //ASSERT(mPcm == NULL);
    mPcm = pcm_open(AudioALSADeviceParser::getInstance()->GetCardIndex(), device, PCM_IN, &mConfig);
    if (mPcm == NULL)
    {
        ALOGE("%s(), mPcm == NULL!!", __FUNCTION__);
    }
    else if (pcm_is_ready(mPcm) == false)
    {
        ALOGE("%s(), pcm_is_ready(%p) == false due to %s, close pcm.", __FUNCTION__, mPcm, pcm_get_error(mPcm));
        pcm_close(mPcm);
        mPcm = NULL;
    }
    else
    {
        pcm_start(mPcm);
    }

    ALOGD("-%s(), mPcm = %p", __FUNCTION__, mPcm);
    ASSERT(mPcm != NULL);

    switch (mTypePCM)
    {
        case 0:
            mPcmIn_MOD = mPcm ;
            break;
        case 1:
            mPcmIn_IO2 = mPcm;
            break;
        case 2:
            mPcmIn_ADC2 = mPcm;
            break;
    }

    return true;

}

FILE *SpeechANCController::OpenFile(const char *mType_ANC)
{
    FILE *mDumpFile = NULL;

    char ANC_file_path[kMaxSizeOfANCFileName];

    char Time_file_path[kMaxSizeOfANCFileName];
    memset((void *)ANC_file_path, 0, kMaxSizeOfANCFileName);

    time_t rawtime;
    time(&rawtime);
    struct tm *timeinfo = localtime(&rawtime);
    ALOGV("%s(), mType_ANC: \"%s\"", __FUNCTION__, mType_ANC);
    strftime(Time_file_path, kMaxSizeOfANCFileName , "_%Y_%m_%d_%H%M%S.pcm", timeinfo);
    ALOGV("%s(), Time_file_path: \"%s\"", __FUNCTION__, Time_file_path);
    sprintf(ANC_file_path, "%s_%s%s", kPrefixOfANCFileName, mType_ANC, Time_file_path);
    ALOGD("%s(), ANC_file_path: \"%s\"", __FUNCTION__, ANC_file_path);

    // check vm_file_path is valid
    int ret = AudiocheckAndCreateDirectory(ANC_file_path);
    if (ret < 0)
    {
        ALOGE("%s(), AudiocheckAndCreateDirectory(%s) fail!!", __FUNCTION__, ANC_file_path);
        return NULL;
    }

    // open file
    mDumpFile = fopen(ANC_file_path, "wb");
    if (mDumpFile == NULL)
    {
        ALOGE("%s(), fopen(%s) fail!!", __FUNCTION__, ANC_file_path);
        return NULL;
    }

    return mDumpFile;
}


//call by speech driver
bool SpeechANCController::StartANCLog()
{
    if (!mGroupANC)
    {
        ALOGD("%s(), EnableError, Not ANC group", __FUNCTION__);
        return false;
    }

    ALOGD("%s()", __FUNCTION__);
    if (mLogEnable)
    {

        // create 3 reading thread
        if (mEnable_ANCLog_MOD != true)
        {
            mDumpFile_MOD = OpenFile("MOD");
            //ANC Log_MOD: I14-> O19
            mEnable_ANCLog_MOD = true;
            int ret = pthread_create(&hReadThread_ANCLog_MOD, NULL, SpeechANCController::readThread_ANCLog_MOD, (void *)this);
            if (ret != 0)
            {
                ALOGE("%s() create thread MOD fail!!", __FUNCTION__);
                return UNKNOWN_ERROR;
            }
        }

        if (!mEnable_ANCLog_IO2)
        {
            mDumpFile_IO2 = OpenFile("IO2");
            //ANC Log_IO2: IO2->O11
            mEnable_ANCLog_IO2 = true;
            int ret2 = pthread_create(&hReadThread_ANCLog_IO2, NULL, SpeechANCController::readThread_ANCLog_IO2, (void *)this);
            if (ret2 != 0)
            {
                ALOGE("%s() create thread IO2 fail!!", __FUNCTION__);
                return UNKNOWN_ERROR;
            }
        }

        if (!mEnable_ANCLog_ADC2)
        {
            mDumpFile_ADC2 = OpenFile("ADC2");
            //ANC Log3: ADC2->O5O6
            mEnable_ANCLog_ADC2 = true;
            int ret3 = pthread_create(&hReadThread_ANCLog_ADC2, NULL, SpeechANCController::readThread_ANCLog_ADC2, (void *)this);
            if (ret3 != 0)
            {
                ALOGE("%s() create thread ADC2 fail!!", __FUNCTION__);
                return UNKNOWN_ERROR;
            }
        }

    }
    return true;
}


//call by speech driver
bool SpeechANCController::StopANCLog()
{
    int ret;
    if (!mGroupANC)
    {
        ALOGD("%s(), EnableError, Not ANC group", __FUNCTION__);
        return false;
    }

    ALOGD("%s()", __FUNCTION__);
    if (mLogEnable)
    {
        // exist 3 reading threads
        mEnable_ANCLog_MOD = false;
        mEnable_ANCLog_IO2 = false;
        mEnable_ANCLog_ADC2 = false;
    }
    return true;
}


void *SpeechANCController::readThread_ANCLog_MOD(void *arg)
{
    prctl(PR_SET_NAME, (unsigned long)__FUNCTION__, 0, 0, 0);
    SpeechANCController *pSpeechANCController = (SpeechANCController *)arg;

    // force to set priority
    struct sched_param sched_p;
    sched_getparam(0, &sched_p);
    sched_p.sched_priority = RTPM_PRIO_AUDIO_RECORD + 1;
    if (0 != sched_setscheduler(0, SCHED_RR, &sched_p))
    {
        ALOGE("[%s] failed, errno: %d", __FUNCTION__, errno);
    }
    else
    {
        sched_p.sched_priority = RTPM_PRIO_AUDIO_CCCI_THREAD;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    ssize_t buffer_size, write_bytes = 0;
    pSpeechANCController->mIndexPcmIn_MOD = pSpeechANCController->ConfigPCM(keypcmMODADCI2S, &buffer_size);

    // config attribute 
    memset(&pSpeechANCController->mConfig_MOD, 0, sizeof(pSpeechANCController->mConfig_MOD));
    pSpeechANCController->mConfig_MOD.channels = 1;
    pSpeechANCController->mConfig_MOD.rate = 16000;

    // Buffer size: 2048(period_size) * 2(ch) * 2(byte) * 8(period_count) = 64 kb
    pSpeechANCController->mConfig_MOD.period_count = 2;
    pSpeechANCController->mConfig_MOD.format = PCM_FORMAT_S16_LE;

    pSpeechANCController->mConfig_MOD.start_threshold = 0;
    pSpeechANCController->mConfig_MOD.stop_threshold = 0;
    pSpeechANCController->mConfig_MOD.silence_threshold = 0;

    pSpeechANCController->mConfig_MOD.period_size = (buffer_size / (pSpeechANCController->mConfig_MOD.channels * pSpeechANCController->mConfig_MOD.period_count)) / ((pSpeechANCController->mConfig_MOD.format == PCM_FORMAT_S16_LE) ? 2 : 4);

    pSpeechANCController->StartPCMIn(0, pSpeechANCController->mIndexPcmIn_MOD, pSpeechANCController->mConfig_MOD);

    // read raw data from stream manager
    char *buffer = new char[kReadBufferSize];
    memset(buffer, 0, sizeof(char)*kReadBufferSize);

    while (pSpeechANCController->mEnable_ANCLog_MOD == true)
    {
        if (pSpeechANCController->mEnable_ANCLog_MOD == false)
        {
            break;
        }

        //pcm read
        ASSERT(pSpeechANCController->mPcmIn_MOD != NULL);
        int retval = pcm_read(pSpeechANCController->mPcmIn_MOD, buffer, kReadBufferSize);
        ALOGD("%s(), pcm_read done", __FUNCTION__);

        if (retval != 0)
        {
            ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
        }
        if (mDumpFile_MOD != NULL)
        {
            // ALOGD("%s(),fwrite file=0x%x,  kReadBufferSize=%d", __FUNCTION__, mDumpFile_MOD, kReadBufferSize);
            // write data to sd card
            write_bytes += fwrite(buffer, sizeof(char), kReadBufferSize, mDumpFile_MOD);
        }
        else
        {
            ALOGE("%s(), mDumpFile_MOD == NULL!!!!!!!!!!!!!!!!!!!!!!!!", __FUNCTION__);
        }
    }

    // close file
    if (mDumpFile_MOD != NULL)
    {
        fflush(mDumpFile_MOD);
        fclose(mDumpFile_MOD);
        ALOGD("%s(), fclose", __FUNCTION__);
        mDumpFile_MOD = NULL;
    }

    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    pcm_stop(pSpeechANCController->mPcmIn_MOD);
    ALOGD("%s(), pcm_stop", __FUNCTION__);
    pcm_close(pSpeechANCController->mPcmIn_MOD);
    pSpeechANCController->mPcmIn_MOD = NULL;

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}



void *SpeechANCController::readThread_ANCLog_IO2(void *arg)
{
    prctl(PR_SET_NAME, (unsigned long)__FUNCTION__, 0, 0, 0);
    SpeechANCController *pSpeechANCController = (SpeechANCController *)arg;

    // force to set priority
    struct sched_param sched_p;
    sched_getparam(0, &sched_p);
    sched_p.sched_priority = RTPM_PRIO_AUDIO_RECORD + 1;
    if (0 != sched_setscheduler(0, SCHED_RR, &sched_p))
    {
        ALOGE("[%s] failed, errno: %d", __FUNCTION__, errno);
    }
    else
    {
        sched_p.sched_priority = RTPM_PRIO_AUDIO_CCCI_THREAD;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    ssize_t buffer_size, write_bytes = 0;
    pSpeechANCController->mIndexPcmIn_IO2 = pSpeechANCController->ConfigPCM(keypcmIO2DAI, &buffer_size);

    // config attribute
    memset(&pSpeechANCController->mConfig_IO2, 0, sizeof(pSpeechANCController->mConfig_IO2));
    pSpeechANCController->mConfig_IO2.channels = 2;
    pSpeechANCController->mConfig_IO2.rate = 48000;//actural samplerate 26000

    // Buffer size: 2048(period_size) * 2(ch) * 2(byte) * 8(period_count) = 64 kb
    pSpeechANCController->mConfig_IO2.period_count = 4;
    pSpeechANCController->mConfig_IO2.format = PCM_FORMAT_S16_LE;

    pSpeechANCController->mConfig_IO2.start_threshold = 0;
    pSpeechANCController->mConfig_IO2.stop_threshold = 0;
    pSpeechANCController->mConfig_IO2.silence_threshold = 0;
    pSpeechANCController->mConfig_IO2.period_size = (buffer_size / (pSpeechANCController->mConfig_IO2.channels * pSpeechANCController->mConfig_IO2.period_count)) / ((pSpeechANCController->mConfig_IO2.format == PCM_FORMAT_S16_LE) ? 2 : 4);

    pSpeechANCController->StartPCMIn(1, pSpeechANCController->mIndexPcmIn_IO2, pSpeechANCController->mConfig_IO2);

    // read raw data from stream manager
    char *buffer = new char[kReadBufferSize];
    memset(buffer, 0, sizeof(char)*kReadBufferSize);

    while (pSpeechANCController->mEnable_ANCLog_IO2 == true)
    {
        if (pSpeechANCController->mEnable_ANCLog_IO2 == false)
        {
            break;
        }
        //pcm read
        ASSERT(pSpeechANCController->mPcmIn_IO2 != NULL);
        int retval = pcm_read(pSpeechANCController->mPcmIn_IO2, buffer, kReadBufferSize);
        if (mDumpFile_IO2 != NULL)
        {
            // ALOGD("%s(),fwrite file=0x%x,  kReadBufferSize=%d", __FUNCTION__, mDumpFile_IO2, kReadBufferSize);
            // write data to sd card
            write_bytes += fwrite((void *)buffer, sizeof(char), kReadBufferSize, mDumpFile_IO2);
        }
        else
        {
            ALOGE("%s(),mDumpFile_IO2 == NULL!!!!!!!!!!!!!!!!!!!!!!!!", __FUNCTION__);
        }

        if (retval != 0)
        {
            ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
        }

    }

    // close file
    if (mDumpFile_IO2 != NULL)
    {
        fflush(mDumpFile_IO2);
        fclose(mDumpFile_IO2);
        mDumpFile_IO2 = NULL;
    }

    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    pcm_stop(pSpeechANCController->mPcmIn_IO2);
    pcm_close(pSpeechANCController->mPcmIn_IO2);
    pSpeechANCController->mPcmIn_IO2 = NULL;

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}

void *SpeechANCController::readThread_ANCLog_ADC2(void *arg)
{
    prctl(PR_SET_NAME, (unsigned long)__FUNCTION__, 0, 0, 0);
    SpeechANCController *pSpeechANCController = (SpeechANCController *)arg;

    // force to set priority
    struct sched_param sched_p;
    sched_getparam(0, &sched_p);
    sched_p.sched_priority = RTPM_PRIO_AUDIO_RECORD + 1;
    if (0 != sched_setscheduler(0, SCHED_RR, &sched_p))
    {
        ALOGE("[%s] failed, errno: %d", __FUNCTION__, errno);
    }
    else
    {
        sched_p.sched_priority = RTPM_PRIO_AUDIO_CCCI_THREAD;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());

    ssize_t buffer_size, write_bytes = 0;
    pSpeechANCController->mIndexPcmIn_ADC2 = pSpeechANCController->ConfigPCM(keypcmADC2AWB, &buffer_size);

    // config attribute
    memset(&pSpeechANCController->mConfig_ADC2, 0, sizeof(pSpeechANCController->mConfig_ADC2));
    pSpeechANCController->mConfig_ADC2.channels = 2;
    //pSpeechANCController->mConfig_ADC2.rate = 192000;
    pSpeechANCController->mConfig_ADC2.rate = 48000;//actural samplerate 26000

    // Buffer size: 2048(period_size) * 2(ch) * 2(byte) * 8(period_count) = 64 kb
    pSpeechANCController->mConfig_ADC2.period_count = 4;
    pSpeechANCController->mConfig_ADC2.format = PCM_FORMAT_S16_LE;

    pSpeechANCController->mConfig_ADC2.start_threshold = 0;
    pSpeechANCController->mConfig_ADC2.stop_threshold = 0;
    pSpeechANCController->mConfig_ADC2.silence_threshold = 0;
    pSpeechANCController->mConfig_ADC2.period_size = (buffer_size / (pSpeechANCController->mConfig_ADC2.channels * pSpeechANCController->mConfig_ADC2.period_count)) / ((pSpeechANCController->mConfig_ADC2.format == PCM_FORMAT_S16_LE) ? 2 : 4);

    pSpeechANCController->StartPCMIn(2, pSpeechANCController->mIndexPcmIn_ADC2, pSpeechANCController->mConfig_ADC2);

    // read raw data from stream manager
    char *buffer = new char[kReadBufferSize];
    memset(buffer, 0, sizeof(char)*kReadBufferSize);

    while (pSpeechANCController->mEnable_ANCLog_ADC2 == true)
    {
        if (pSpeechANCController->mEnable_ANCLog_ADC2 == false)
        {
            break;
        }
        //pcm read
        //    bytes_read = StreamInANCLog1->read(buffer, bytes);
        ASSERT(pSpeechANCController->mPcmIn_ADC2 != NULL);
        int retval = pcm_read(pSpeechANCController->mPcmIn_ADC2, buffer, kReadBufferSize);
        if (mDumpFile_ADC2 != NULL)
        {
            // ALOGD("%s(),fwrite file=0x%x,  kReadBufferSize=%d", __FUNCTION__, mDumpFile_ADC2, kReadBufferSize);
            // write data to sd card
            write_bytes += fwrite((void *)buffer, sizeof(char), kReadBufferSize, mDumpFile_ADC2);
        }
        else
        {
            ALOGE("%s(), mDumpFile_ADC2 == NULL!!!!!!!!!!!!!!!!!!!!!!!!", __FUNCTION__);
        }

        if (retval != 0)
        {
            ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
        }

    }

    // close file
    if (mDumpFile_ADC2 != NULL)
    {
        fflush(mDumpFile_ADC2);
        fclose(mDumpFile_ADC2);
        mDumpFile_ADC2 = NULL;
    }

    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    pcm_stop(pSpeechANCController->mPcmIn_ADC2);
    pcm_close(pSpeechANCController->mPcmIn_ADC2);
    pSpeechANCController->mPcmIn_ADC2 = NULL;

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}

#endif

}







//namespace android
