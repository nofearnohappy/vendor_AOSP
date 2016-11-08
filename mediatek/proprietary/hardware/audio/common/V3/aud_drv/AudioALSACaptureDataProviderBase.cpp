#include "AudioALSACaptureDataProviderBase.h"

#include <utils/threads.h>

#include "AudioType.h"
#include "AudioLock.h"

#include "AudioALSACaptureDataClient.h"



#define LOG_TAG "AudioALSACaptureDataProviderBase"

namespace android
{
int AudioALSACaptureDataProviderBase::mDumpFileNum = 0;

AudioALSACaptureDataProviderBase::AudioALSACaptureDataProviderBase() :
    mEnable(false),
    mOpenIndex(0),
    mCaptureDataClientIndex(0),
    mPcm(NULL),
    mCaptureDataProviderType(CAPTURE_PROVIDER_BASE)
{
    ALOGD("%s(), %p", __FUNCTION__, this);

    mCaptureDataClientVector.clear();

    memset((void *)&mPcmReadBuf, 0, sizeof(mPcmReadBuf));

    memset((void *)&mConfig, 0, sizeof(mConfig));

    memset((void *)&mStreamAttributeSource, 0, sizeof(mStreamAttributeSource));

    mPCMDumpFile = NULL;
}

AudioALSACaptureDataProviderBase::~AudioALSACaptureDataProviderBase()
{
    ALOGD("%s(), %p", __FUNCTION__, this);
}

status_t AudioALSACaptureDataProviderBase::openPcmDriver(const unsigned int device)
{
    ALOGD("+%s(), pcm device = %d", __FUNCTION__, device);

    ASSERT(mPcm == NULL);
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
    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderBase::closePcmDriver()
{
    ALOGD("+%s(), mPcm = %p", __FUNCTION__, mPcm);

    if (mPcm != NULL)
    {
        pcm_stop(mPcm);
        pcm_close(mPcm);
        mPcm = NULL;
    }

    ALOGD("-%s(), mPcm = %p", __FUNCTION__, mPcm);
    return NO_ERROR;
}

void AudioALSACaptureDataProviderBase::attach(AudioALSACaptureDataClient *pCaptureDataClient)
{
    ALOGD("%s(), %p", __FUNCTION__, this);
    AudioAutoTimeoutLock _l(mClientLock);

    pCaptureDataClient->setIdentity(mCaptureDataClientIndex);
    ALOGD("%s(), mCaptureDataClientIndex=%d, mCaptureDataClientVector.size()=%d, Identity=%d", __FUNCTION__, mCaptureDataClientIndex, mCaptureDataClientVector.size(),
          pCaptureDataClient->getIdentity());
    mCaptureDataClientVector.add(pCaptureDataClient->getIdentity(), pCaptureDataClient);
    mCaptureDataClientIndex++;

    // open pcm interface when 1st attach
    if (mCaptureDataClientVector.size() == 1)
    {
        mOpenIndex++;
        open();
    }
    ALOGD("-%s()", __FUNCTION__);
}

void AudioALSACaptureDataProviderBase::detach(AudioALSACaptureDataClient *pCaptureDataClient)
{
    ALOGD("%s(),%p, Identity=%d, mCaptureDataClientVector.size()=%d,mCaptureDataProviderType=%d, %p", __FUNCTION__, this, pCaptureDataClient->getIdentity(), mCaptureDataClientVector.size(),
          mCaptureDataProviderType, pCaptureDataClient);
    AudioAutoTimeoutLock _l(mClientLock);

    mCaptureDataClientVector.removeItem(pCaptureDataClient->getIdentity());
    // close pcm interface when there is no client attached
    if (mCaptureDataClientVector.size() == 0)
    {
        close();
        ALOGD("%s(), close finish", __FUNCTION__);
    }
    ALOGD("-%s()", __FUNCTION__);
}

void AudioALSACaptureDataProviderBase::provideCaptureDataToAllClients(const uint32_t open_index)
{
    ALOGV("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mClientLock);

    if (open_index != mOpenIndex)
    {
        ALOGD("%s(), open_index(%d) != mOpenIndex(%d), return", __FUNCTION__, open_index, mOpenIndex);
        return;
    }

    AudioALSACaptureDataClient *pCaptureDataClient = NULL;

    WritePcmDumpData();
    for (size_t i = 0; i < mCaptureDataClientVector.size(); i++)
    {
        pCaptureDataClient = mCaptureDataClientVector[i];
        pCaptureDataClient->copyCaptureDataToClient(mPcmReadBuf);
    }

    ALOGV("-%s()", __FUNCTION__);
}

bool AudioALSACaptureDataProviderBase::HasLowLatencyCapture(void)
{
    ALOGD("+%s()", __FUNCTION__);
    //AudioAutoTimeoutLock _l(mClientLock);
    bool bRet = false;
    AudioALSACaptureDataClient *pCaptureDataClient = NULL;

    for (size_t i = 0; i < mCaptureDataClientVector.size(); i++)
    {
        pCaptureDataClient = mCaptureDataClientVector[i];
        if (pCaptureDataClient->IsLowLatencyCapture())
        {
            bRet = true;
            break;
        }
    }
    ALOGD("-%s(), bRet=%d", __FUNCTION__,bRet);
    return bRet;
}

void AudioALSACaptureDataProviderBase::OpenPCMDump(const char *class_name)
{
    ALOGV("%s(), mCaptureDataProviderType=%d", __FUNCTION__, mCaptureDataProviderType);
    char mDumpFileName[128];
    sprintf(mDumpFileName, "%s%d.%s.pcm", streamin, mDumpFileNum, class_name);

    mPCMDumpFile = NULL;
    mPCMDumpFile = AudioOpendumpPCMFile(mDumpFileName, streamin_propty);

    if (mPCMDumpFile != NULL)
    {
        ALOGD("%s DumpFileName = %s", __FUNCTION__, mDumpFileName);

        mDumpFileNum++;
        mDumpFileNum %= MAX_DUMP_NUM;
    }
}

void AudioALSACaptureDataProviderBase::ClosePCMDump()
{
    if (mPCMDumpFile)
    {
        AudioCloseDumpPCMFile(mPCMDumpFile);
        ALOGD("%s(), mCaptureDataProviderType=%d", __FUNCTION__, mCaptureDataProviderType);
        mPCMDumpFile = NULL;
    }
}

void  AudioALSACaptureDataProviderBase::WritePcmDumpData(void)
{
    if (mPCMDumpFile)
    {
        //ALOGD("%s()", __FUNCTION__);
        AudioDumpPCMData((void *)mPcmReadBuf.pBufBase , mPcmReadBuf.bufLen - 1, mPCMDumpFile);
    }
}

//echoref+++
void AudioALSACaptureDataProviderBase::provideEchoRefCaptureDataToAllClients(const uint32_t open_index)
{
    ALOGV("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mClientLock);

    if (open_index != mOpenIndex)
    {
        ALOGD("%s(), open_index(%d) != mOpenIndex(%d), return", __FUNCTION__, open_index, mOpenIndex);
        return;
    }

    AudioALSACaptureDataClient *pCaptureDataClient = NULL;

    WritePcmDumpData();
    for (size_t i = 0; i < mCaptureDataClientVector.size(); i++)
    {
        pCaptureDataClient = mCaptureDataClientVector[i];
        pCaptureDataClient->copyEchoRefCaptureDataToClient(mPcmReadBuf);
    }

    ALOGV("-%s()", __FUNCTION__);
}
//echoref---


status_t AudioALSACaptureDataProviderBase::GetCaptureTimeStamp(time_info_struct_t *Time_Info, unsigned int read_size)
{
    ALOGV("%s()", __FUNCTION__);
    ASSERT(mPcm != NULL);

    long ret_ns;
    size_t avail;
    Time_Info->timestamp_get.tv_sec  = 0;
    Time_Info->timestamp_get.tv_nsec = 0;
    Time_Info->frameInfo_get = 0;
    Time_Info->buffer_per_time = 0;
    Time_Info->kernelbuffer_ns = 0;

    //ALOGD("%s(), Going to check pcm_get_htimestamp", __FUNCTION__);
    if (pcm_get_htimestamp(mPcm, &Time_Info->frameInfo_get, &Time_Info->timestamp_get) == 0)
    {
        Time_Info->buffer_per_time = pcm_bytes_to_frames(mPcm, read_size);
        Time_Info->kernelbuffer_ns = 1000000000 / mStreamAttributeSource.sample_rate * (Time_Info->buffer_per_time + Time_Info->frameInfo_get);
        ALOGV("%s pcm_get_htimestamp sec= %ld, nsec=%ld, frameInfo_get = %u, buffer_per_time=%u, ret_ns = %lu\n",
              __FUNCTION__, Time_Info->timestamp_get.tv_sec, Time_Info->timestamp_get.tv_nsec, Time_Info->frameInfo_get,
              Time_Info->buffer_per_time, Time_Info->kernelbuffer_ns);
#if 0
        if ((TimeStamp->tv_nsec - ret_ns) >= 0)
        {
            TimeStamp->tv_nsec -= ret_ns;
        }
        else
        {
            TimeStamp->tv_sec -= 1;
            TimeStamp->tv_nsec = 1000000000 + TimeStamp->tv_nsec - ret_ns;
        }

        ALOGD("%s calculate pcm_get_htimestamp sec= %ld, nsec=%ld, avail = %d, ret_ns = %ld\n",
              __FUNCTION__, TimeStamp->tv_sec, TimeStamp->tv_nsec, avail, ret_ns);
#endif
    }
    else
    {
        ALOGE("%s pcm_get_htimestamp fail %s\n", __FUNCTION__, pcm_get_error(mPcm));
    }
    return NO_ERROR;
}


} // end of namespace android

