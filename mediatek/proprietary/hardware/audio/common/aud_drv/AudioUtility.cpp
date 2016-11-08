#include "AudioUtility.h"

#include "AudioLock.h"
#include "audio_custom_exp.h"

extern "C" {
#include  "bli_exp.h"
}

namespace android
{
unsigned long long TimeDifference(struct timespec time1, struct timespec time2)
{
    unsigned long long diffns = 0;
    struct timespec tstemp1 = time1;
    struct timespec tstemp2 = time2;

    //    ALOGD("TimeStampDiff time1 sec= %ld, nsec=%ld, time2 sec=%ld, nsec=%ld" ,tstemp1.tv_sec, tstemp1.tv_nsec, tstemp2.tv_sec, tstemp2.tv_nsec);

    if (tstemp1.tv_sec > tstemp2.tv_sec)
    {
        if (tstemp1.tv_nsec >= tstemp2.tv_nsec)
        {
            diffns = ((tstemp1.tv_sec - tstemp2.tv_sec) * 1000000000) + tstemp1.tv_nsec - tstemp2.tv_nsec;
        }
        else
        {
            diffns = ((tstemp1.tv_sec - tstemp2.tv_sec - 1) * 1000000000) + tstemp1.tv_nsec + 1000000000 - tstemp2.tv_nsec;
        }
    }
    else if (tstemp1.tv_sec == tstemp2.tv_sec)
    {
        if (tstemp1.tv_nsec >= tstemp2.tv_nsec)
        {
            diffns = tstemp1.tv_nsec - tstemp2.tv_nsec;
        }
        else
        {
            diffns = tstemp2.tv_nsec - tstemp1.tv_nsec;
        }
    }
    else
    {
        if (tstemp2.tv_nsec >= tstemp1.tv_nsec)
        {
            diffns = ((tstemp2.tv_sec - tstemp1.tv_sec) * 1000000000) + tstemp2.tv_nsec - tstemp1.tv_nsec;
        }
        else
        {
            diffns = ((tstemp2.tv_sec - tstemp1.tv_sec - 1) * 1000000000) + tstemp2.tv_nsec + 1000000000 - tstemp1.tv_nsec;
        }
    }
    //    ALOGD("TimeDifference time1 sec= %ld, nsec=%ld, time2 sec=%ld, nsec=%ld, diffns=%lld" ,tstemp1.tv_sec, tstemp1.tv_nsec, tstemp2.tv_sec,tstemp2.tv_nsec,diffns);
    return diffns;
}


//---------- implementation of ringbuffer--------------


// function for get how many data is available

/**
* function for get how many data is available
* @return how many data exist
*/

int RingBuf_getDataCount(const RingBuf *RingBuf1)
{
    /*
    ALOGD("RingBuf1->pBase = 0x%x RingBuf1->pWrite = 0x%x  RingBuf1->bufLen = %d  RingBuf1->pRead = 0x%x",
        RingBuf1->pBufBase,RingBuf1->pWrite, RingBuf1->bufLen,RingBuf1->pRead);
        */
    int count = RingBuf1->pWrite - RingBuf1->pRead;
    if (count < 0) { count += RingBuf1->bufLen; }
    return count;
}

/**
*  function for get how free space available
* @return how free sapce
*/

int RingBuf_getFreeSpace(const RingBuf *RingBuf1)
{
    /*
    ALOGD("RingBuf1->pBase = 0x%x RingBuf1->pWrite = 0x%x  RingBuf1->bufLen = %d  RingBuf1->pRead = 0x%x",
        RingBuf1->pBufBase,RingBuf1->pWrite, RingBuf1->bufLen,RingBuf1->pRead);*/
    int count = 0;

    if (RingBuf1->pRead > RingBuf1->pWrite)
    {
        count = RingBuf1->pRead - RingBuf1->pWrite - 8;
    }
    else // RingBuf1->pRead <= RingBuf1->pWrite
    {
        count = RingBuf1->pRead - RingBuf1->pWrite + RingBuf1->bufLen - 8;
    }

    return (count > 0) ? count : 0;
}

/**
* copy count number bytes from ring buffer to buf
* @param buf buffer copy from
* @param RingBuf1 buffer copy to
* @param count number of bytes need to copy
*/

void RingBuf_copyToLinear(char *buf, RingBuf *RingBuf1, int count)
{
    /*
    ALOGD("RingBuf1->pBase = 0x%x RingBuf1->pWrite = 0x%x  RingBuf1->bufLen = %d  RingBuf1->pRead = 0x%x",
        RingBuf1->pBufBase,RingBuf1->pWrite, RingBuf1->bufLen,RingBuf1->pRead);*/
    if (RingBuf1->pRead <= RingBuf1->pWrite)
    {
        memcpy(buf, RingBuf1->pRead, count);
        RingBuf1->pRead += count;
    }
    else
    {
        char *end = RingBuf1->pBufBase + RingBuf1->bufLen;
        int r2e = end - RingBuf1->pRead;
        if (count <= r2e)
        {
            //ALOGD("2 RingBuf_copyToLinear r2e= %d count = %d",r2e,count);
            memcpy(buf, RingBuf1->pRead, count);
            RingBuf1->pRead += count;
            if (RingBuf1->pRead == end)
            {
                RingBuf1->pRead = RingBuf1->pBufBase;
            }
        }
        else
        {
            //ALOGD("3 RingBuf_copyToLinear r2e= %d count = %d",r2e,count);
            memcpy(buf, RingBuf1->pRead, r2e);
            memcpy(buf + r2e, RingBuf1->pBufBase, count - r2e);
            RingBuf1->pRead = RingBuf1->pBufBase + count - r2e;
        }
    }
}

/**
* copy count number bytes from buf to RingBuf1
* @param RingBuf1 ring buffer copy from
* @param buf copy to
* @param count number of bytes need to copy
*/
void RingBuf_copyFromLinear(RingBuf *RingBuf1, const char *buf, int count)
{
    int spaceIHave;
    char *end = RingBuf1->pBufBase + RingBuf1->bufLen;

    // count buffer data I have
    spaceIHave = RingBuf1->bufLen - RingBuf_getDataCount(RingBuf1) - 8;
    //spaceIHave = RingBuf_getDataCount(RingBuf1);

    // if not enough, assert
    ASSERT(spaceIHave >= count);

    if (RingBuf1->pRead <= RingBuf1->pWrite)
    {
        int w2e = end - RingBuf1->pWrite;
        if (count <= w2e)
        {
            memcpy(RingBuf1->pWrite, buf, count);
            RingBuf1->pWrite += count;
            if (RingBuf1->pWrite == end)
            {
                RingBuf1->pWrite = RingBuf1->pBufBase;
            }
        }
        else
        {
            memcpy(RingBuf1->pWrite, buf, w2e);
            memcpy(RingBuf1->pBufBase, buf + w2e, count - w2e);
            RingBuf1->pWrite = RingBuf1->pBufBase + count - w2e;
        }
    }
    else
    {
        memcpy(RingBuf1->pWrite, buf, count);
        RingBuf1->pWrite += count;
    }

}

/**
* fill count number zero bytes to RingBuf1
* @param RingBuf1 ring buffer fill from
* @param count number of bytes need to copy
*/
void RingBuf_fillZero(RingBuf *RingBuf1, int count)
{
    int spaceIHave;
    char *end = RingBuf1->pBufBase + RingBuf1->bufLen;

    // count buffer data I have
    spaceIHave = RingBuf1->bufLen - RingBuf_getDataCount(RingBuf1) - 8;
    //spaceIHave = RingBuf_getDataCount(RingBuf1);

    // if not enough, assert
    ASSERT(spaceIHave >= count);

    if (RingBuf1->pRead <= RingBuf1->pWrite)
    {
        int w2e = end - RingBuf1->pWrite;
        if (count <= w2e)
        {
            memset(RingBuf1->pWrite, 0, sizeof(char)*count);
            RingBuf1->pWrite += count;
            if (RingBuf1->pWrite == end)
            {
                RingBuf1->pWrite = RingBuf1->pBufBase;
            }
        }
        else
        {
            memset(RingBuf1->pWrite, 0, sizeof(char)*w2e);
            memset(RingBuf1->pBufBase, 0, sizeof(char) * (count - w2e));
            RingBuf1->pWrite = RingBuf1->pBufBase + count - w2e;
        }
    }
    else
    {
        memset(RingBuf1->pWrite, 0, sizeof(char)*count);
        RingBuf1->pWrite += count;
    }

}


/**
* copy ring buffer from RingBufs(source) to RingBuft(target)
* @param RingBuft ring buffer copy to
* @param RingBufs copy from copy from
*/

void RingBuf_copyEmpty(RingBuf *RingBuft, RingBuf *RingBufs)
{
    if (RingBufs->pRead <= RingBufs->pWrite)
    {
        RingBuf_copyFromLinear(RingBuft, RingBufs->pRead, RingBufs->pWrite - RingBufs->pRead);
        //RingBufs->pRead = RingBufs->pWrite;
        // no need to update source read pointer, because it is read to empty
    }
    else
    {
        char *end = RingBufs->pBufBase + RingBufs->bufLen;
        RingBuf_copyFromLinear(RingBuft, RingBufs->pRead, end - RingBufs->pRead);
        RingBuf_copyFromLinear(RingBuft, RingBufs->pBufBase, RingBufs->pWrite - RingBufs->pBufBase);
    }
}


/**
* copy ring buffer from RingBufs(source) to RingBuft(target) with count
* @param RingBuft ring buffer copy to
* @param RingBufs copy from copy from
*/
int RingBuf_copyFromRingBuf(RingBuf *RingBuft, RingBuf *RingBufs, int count)
{
    int cntInRingBufs = RingBuf_getDataCount(RingBufs);
    int freeSpaceInRingBuft = RingBuf_getFreeSpace(RingBuft);
    ASSERT(count <= cntInRingBufs && count <= freeSpaceInRingBuft);

    if (RingBufs->pRead <= RingBufs->pWrite)
    {
        RingBuf_copyFromLinear(RingBuft, RingBufs->pRead, count);
        RingBufs->pRead += count;
        // no need to update source read pointer, because it is read to empty
    }
    else
    {
        char *end = RingBufs->pBufBase + RingBufs->bufLen;
        int cnt2e = end - RingBufs->pRead;
        if (cnt2e >= count)
        {
            RingBuf_copyFromLinear(RingBuft, RingBufs->pRead, count);
            RingBufs->pRead += count;
            if (RingBufs->pRead == end)
            {
                RingBufs->pRead = RingBufs->pBufBase;
            }
        }
        else
        {
            RingBuf_copyFromLinear(RingBuft, RingBufs->pRead, cnt2e);
            RingBuf_copyFromLinear(RingBuft, RingBufs->pBufBase, count - cnt2e);
            RingBufs->pRead = RingBufs->pBufBase + count - cnt2e;
        }
    }
    return count;
}

/**
* write bytes size of count woith value
* @param RingBuf1 ring buffer copy to
* @value value put into buffer
* @count bytes ned to put.
*/
void RingBuf_writeDataValue(RingBuf *RingBuf1, const int value, const int count)
{
    int spaceIHave;

    // count buffer data I have
    spaceIHave = RingBuf1->bufLen - RingBuf_getDataCount(RingBuf1) - 8;

    // if not enough, assert
    ASSERT(spaceIHave >= count);

    if (RingBuf1->pRead <= RingBuf1->pWrite)
    {
        char *end = RingBuf1->pBufBase + RingBuf1->bufLen;
        int w2e = end - RingBuf1->pWrite;
        if (count <= w2e)
        {
            memset(RingBuf1->pWrite, value, count);
            RingBuf1->pWrite += count;
        }
        else
        {
            memset(RingBuf1->pWrite, value, w2e);
            memset(RingBuf1->pBufBase, value, count - w2e);
            RingBuf1->pWrite = RingBuf1->pBufBase + count - w2e;
        }
    }
    else
    {
        memset(RingBuf1->pWrite, value, count);
        RingBuf1->pWrite += count;
    }

}


//---------end of ringbuffer implemenation------------------------------------------------------


//--------pc dump operation

struct BufferDump
{
    void *pBufBase;
    int ssize_t;
};

#if defined(PC_EMULATION)
HANDLE hPCMDumpThread = NULL;
HANDLE PCMDataNotifyEvent = NULL;
#else
pthread_t hPCMDumpThread = NULL;
pthread_cond_t  PCMDataNotifyEvent;
pthread_mutex_t PCMDataNotifyMutex;
#endif

AudioLock mPCMDumpMutex; // use for PCM buffer dump
KeyedVector<FILE *, Vector<BufferDump *> *> mDumpFileHandleVector; // vector to save current recording client
uint32_t mSleepTime = 2;

void *PCMDumpThread(void *arg);

int AudiocheckAndCreateDirectory(const char *pC)
{
    char tmp[PATH_MAX];
    int i = 0;
    while (*pC)
    {
        tmp[i] = *pC;
        if (*pC == '/' && i)
        {
            tmp[i] = '\0';
            if (access(tmp, F_OK) != 0)
            {
                if (mkdir(tmp, 0770) == -1)
                {
                    ALOGE("AudioDumpPCM: mkdir error! %s\n", (char *)strerror(errno));
                    return -1;
                }
            }
            tmp[i] = '/';
        }
        i++;
        pC++;
    }
    return 0;
}

bool bDumpStreamOutFlg = false;
bool bDumpStreamInFlg = false;

FILE *AudioOpendumpPCMFile(const char *filepath, const char *propty)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;
    property_get(propty, value, "0");
    int bflag = atoi(value);

    if (!bflag)
    {
        if(!strcmp(propty,streamout_propty) && bDumpStreamOutFlg)
            bflag = 1;
        else if(!strcmp(propty,streamin_propty) && bDumpStreamInFlg)
            bflag = 1;
    }

    if (bflag)
    {
        ret = AudiocheckAndCreateDirectory(filepath);
        if (ret < 0)
        {
            ALOGE("AudioOpendumpPCMFile dumpPCMData checkAndCreateDirectory() fail!!!");
        }
        else
        {
            FILE *fp = fopen(filepath, "wb");
            if (fp != NULL)
            {

                mPCMDumpMutex.lock();

                Vector<BufferDump *> *pBD = new Vector<BufferDump *>;
                //ALOGD("AudioOpendumpPCMFile file=%p, pBD=%p",fp, pBD);
                mDumpFileHandleVector.add(fp, pBD);
                /*for (size_t i = 0; i < mDumpFileHandleVector.size() ; i++)
                {
                    ALOGD("AudioOpendumpPCMFile i=%d, handle=%p, %p",i,mDumpFileHandleVector.keyAt(i),mDumpFileHandleVector.valueAt(i));
                }*/

                //ALOGD("AudioOpendumpPCMFile hPCMDumpThread=%p",hPCMDumpThread);
                if (hPCMDumpThread == NULL)
                {
#if defined(PC_EMULATION)
                    PCMDataNotifyEvent = CreateEvent(NULL, TRUE, FALSE, "PCMDataNotifyEvent");
                    hPCMDumpThread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)PCMDumpThread, NULL, 0, 0);
                    if (hPCMDumpThread == 0)
                    {
                        ALOGE("hPCMDumpThread create fail!!!");
                    }
                    else
                    {
                        ALOGD("hPCMDumpThread=%p created", hPCMDumpThread);
                    }
#else
                    //create PCM data dump thread here
                    int ret;
                    ret = pthread_create(&hPCMDumpThread, NULL, PCMDumpThread, NULL);
                    if (ret != 0)
                    {
                        ALOGE("hPCMDumpThread create fail!!!");
                    }
                    else
                    {
                        ALOGD("hPCMDumpThread=%p created", hPCMDumpThread);
                    }
                    ret = pthread_cond_init(&PCMDataNotifyEvent, NULL);
                    if (ret != 0)
                    {
                        ALOGE("PCMDataNotifyEvent create fail!!!");
                    }

                    ret = pthread_mutex_init(&PCMDataNotifyMutex, NULL);
                    if (ret != 0)
                    {
                        ALOGE("PCMDataNotifyMutex create fail!!!");
                    }
#endif
                }
                mPCMDumpMutex.unlock();
                return fp;
            }
            else
            {
                ALOGE("AudioFlinger AudioOpendumpPCMFile %s fail", propty);
            }
        }
    }
    return NULL;
}

void AudioCloseDumpPCMFile(FILE  *file)
{
    if (file != NULL)
    {
        mPCMDumpMutex.lock();
        //ALOGD("AudioCloseDumpPCMFile file=%p, HandleCount=%d",file,mDumpFileHandleVector.size());
        if (mDumpFileHandleVector.size())
        {
            for (size_t i = 0; i < mDumpFileHandleVector.size() ; i++)
            {
                //ALOGD("AudioCloseDumpPCMFile i=%d, handle=%p",i,mDumpFileHandleVector.keyAt(i));
                if (file == mDumpFileHandleVector.keyAt(i))
                {
                    FILE *key = mDumpFileHandleVector.keyAt(i);
                    while ((* mDumpFileHandleVector.valueAt(i)).size() != 0)
                    {
                        free((* mDumpFileHandleVector.valueAt(i))[0]->pBufBase);
                        delete(* mDumpFileHandleVector.valueAt(i))[0];
                        (* mDumpFileHandleVector.valueAt(i)).removeAt(0);
                    }
                    delete mDumpFileHandleVector.valueAt(i);
                    mDumpFileHandleVector.removeItem(key);
                }
            }
        }

        mPCMDumpMutex.unlock();
        fclose(file);
        file = NULL;
    }
    else
    {
        ALOGE("AudioCloseDumpPCMFile file== NULL");
    }
}

void AudioDumpPCMData(void *buffer , uint32_t bytes, FILE  *file)
{
    if (hPCMDumpThread != NULL)
    {
        mPCMDumpMutex.lock();
        if (mDumpFileHandleVector.size())
        {
            for (size_t i = 0; i < mDumpFileHandleVector.size() ; i++)
            {
                if (file == mDumpFileHandleVector.keyAt(i))
                {
                    FILE *key = mDumpFileHandleVector.keyAt(i);
                    //ALOGD("AudioDumpPCMData find!! i=%d, key=%p, value=%p",i,mDumpFileHandleVector.keyAt(i),mDumpFileHandleVector.valueAt(i));
                    BufferDump *newInBuffer = new BufferDump;
                    newInBuffer->pBufBase = (short *) malloc(bytes);
                    memcpy(newInBuffer->pBufBase, buffer, bytes);
                    newInBuffer->ssize_t = bytes;
                    (* mDumpFileHandleVector.valueAt(i)).add(newInBuffer);

                    if (mSleepTime == -1) //need to send event
                    {
#if defined(PC_EMULATION)
                        SetEvent(PCMDataNotifyEvent);
#else
                        pthread_mutex_lock(&PCMDataNotifyMutex);
                        pthread_cond_signal(&PCMDataNotifyEvent);
                        pthread_mutex_unlock(&PCMDataNotifyMutex);
#endif
                    }
                }
            }
        }
        mPCMDumpMutex.unlock();
    }
    else    //if no dump thread, just write the data
    {
        fwrite((void *)buffer, sizeof(char), bytes, file);
    }

}

void *PCMDumpThread(void *arg)
{
    ALOGD("PCMDumpThread");
    bool bHasdata = false;
    int iNoDataCount = 0;
    while (1)
    {
        mPCMDumpMutex.lock();
        bHasdata = false;
        //ALOGV( "PCMDumpThread mDumpFileHandleVector.size()=%d",mDumpFileHandleVector.size());
        for (size_t i = 0; i < mDumpFileHandleVector.size() ; i++)
        {

            if ((* mDumpFileHandleVector.valueAt(i)).size() > 0)
            {
                bHasdata = true;
                fwrite((* mDumpFileHandleVector.valueAt(i))[0]->pBufBase, (* mDumpFileHandleVector.valueAt(i))[0]->ssize_t, 1, mDumpFileHandleVector.keyAt(i));
                free((* mDumpFileHandleVector.valueAt(i))[0]->pBufBase);
                delete(* mDumpFileHandleVector.valueAt(i))[0];
                (* mDumpFileHandleVector.valueAt(i)).removeAt(0);
            }
        }
        mPCMDumpMutex.unlock();
        if (!bHasdata)
        {
            iNoDataCount++;
            if (iNoDataCount >= 1000)
            {
                mSleepTime = -1;
                ALOGD("PCMDumpThread, wait for new data dump\n");
#if defined(PC_EMULATION)
                WaitForSingleObject(PCMDataNotifyEvent, INFINITE);
                ResetEvent(PCMDataNotifyEvent);
#else
                pthread_mutex_lock(&PCMDataNotifyMutex);
                pthread_cond_wait(&PCMDataNotifyEvent, &PCMDataNotifyMutex);
                pthread_mutex_unlock(&PCMDataNotifyMutex);
                ALOGD("PCMDumpThread, PCM data dump again\n");
#endif
            }
            else
            {
                mSleepTime = 10;
                usleep(mSleepTime * 1000);
            }
        }
        else
        {
            iNoDataCount = 0;
            mSleepTime = 2;
            usleep(mSleepTime * 1000);
        }
        /*
                if(mDumpFileHandleVector.size()==0)
                {
                    ALOGD( "PCMDumpThread exit, no dump handle real");
                    hPCMDumpThread = NULL;
                    pthread_exit(NULL);
                    return 0;
                }*/

    }

    ALOGD("PCMDumpThread exit hPCMDumpThread=%p", hPCMDumpThread);
    hPCMDumpThread = NULL;
    pthread_exit(NULL);
    return 0;
}

#define CVSD_LOOPBACK_BUFFER_SIZE (180*1000)//BTSCO_CVSD_RX_FRAME*SCO_RX_PCM8K_BUF_SIZE * 10
static uint8_t cvsd_temp_buffer[CVSD_LOOPBACK_BUFFER_SIZE]; //temp buf only for dump to file
static uint32_t cvsd_temp_w = 0;
static uint32_t cvsd_temp_r = 0;
const static uint32_t cvsd_temp_size = CVSD_LOOPBACK_BUFFER_SIZE;

void CVSDLoopbackGetWriteBuffer(uint8_t **buffer, uint32_t *buf_len)  // in bytes
{
    int32_t count;

    if (cvsd_temp_r > cvsd_temp_w)
    {
        count = cvsd_temp_r - cvsd_temp_w - 8;
    }
    else
    {
        count = cvsd_temp_size + cvsd_temp_r - cvsd_temp_w - 8;
    }

    *buffer = (uint8_t *)&cvsd_temp_buffer[cvsd_temp_w];
    *buf_len = (count > 0) ? count : 0;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackGetWriteBuffer: buf_len: %d", *buf_len);
}

void CVSDLoopbackGetReadBuffer(uint8_t **buffer, uint32_t *buf_len)  // in bytes
{
    int32_t count;

    if (cvsd_temp_w >= cvsd_temp_r)
    {
        count = cvsd_temp_w - cvsd_temp_r;
    }
    else
    {
        count = cvsd_temp_size - cvsd_temp_r;
    }

    *buffer = (uint8_t *)&cvsd_temp_buffer[cvsd_temp_r];
    *buf_len = count;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackGetReadBuffer: buf_len: %d", count);
}

void CVSDLoopbackReadDataDone(uint32_t len) // in bytes
{
    cvsd_temp_r += len;
    if (cvsd_temp_r == cvsd_temp_size)
    {
        cvsd_temp_r = 0;
    }
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackReadDataDone: len: %d", len);
}

void CVSDLoopbackWriteDataDone(uint32_t len) // in bytes
{
    cvsd_temp_w += len;
    if (cvsd_temp_w == cvsd_temp_size)
    {
        cvsd_temp_w = 0;
    }
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackWriteDataDone: len: %d", len);
}

void CVSDLoopbackResetBuffer(void) // in bytes
{
    memset(cvsd_temp_buffer, 0, CVSD_LOOPBACK_BUFFER_SIZE);
    cvsd_temp_w = 180 * 100; //if 0, deadlock
    cvsd_temp_r = 0;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackResetBuffer");
}

int32_t CVSDLoopbackGetFreeSpace(void)
{
    int32_t count;

    if (cvsd_temp_r > cvsd_temp_w)
    {
        count = cvsd_temp_r - cvsd_temp_w - 8;
    }
    else
    {
        count = cvsd_temp_size + cvsd_temp_r - cvsd_temp_w - 8;
    }

    return (count > 0) ? count : 0; // free size in byte
}

int32_t CVSDLoopbackGetDataCount(void)
{
    return (cvsd_temp_size - CVSDLoopbackGetFreeSpace() - 8);
}

const char PROPERTY_KEY_DMIC_ON[PROPERTY_KEY_MAX]  = "persist.af.feature.dmic";
const char PROPERTY_KEY_2IN1SPK_ON[PROPERTY_KEY_MAX] = "persist.af.feature.2in1spk";
const char PROPERTY_KEY_VIBSPK_ON[PROPERTY_KEY_MAX] = "persist.af.feature.vibspk";


bool IsAudioSupportFeature(int dFeatureOption)
{
    bool bSupportFlg = false;
    char stForFeatureUsage[PROPERTY_VALUE_MAX];

    switch (dFeatureOption)
    {
        case AUDIO_SUPPORT_DMIC:
        {
#ifdef MTK_DIGITAL_MIC_SUPPORT
            property_get(PROPERTY_KEY_DMIC_ON, stForFeatureUsage, "1"); //"1": default on
#else
            property_get(PROPERTY_KEY_DMIC_ON, stForFeatureUsage, "0"); //"0": default off
#endif
            bSupportFlg = (stForFeatureUsage[0] == '0') ? false : true;
            //ALOGD("IsAudioSupportFeature AUDIO_SUPPORT_DMIC [%d]\n",bSupportFlg);

            break;
        }
        case AUDIO_SUPPORT_2IN1_SPEAKER:
        {
#ifdef USING_2IN1_SPEAKER
            property_get(PROPERTY_KEY_2IN1SPK_ON, stForFeatureUsage, "1"); //"1": default on
#else
            property_get(PROPERTY_KEY_2IN1SPK_ON, stForFeatureUsage, "0"); //"0": default off
#endif
            bSupportFlg = (stForFeatureUsage[0] == '0') ? false : true;
            //ALOGD("IsAudioSupportFeature AUDIO_SUPPORT_2IN1_SPEAKER [%d]\n",bSupportFlg);

            break;
        }
        case AUDIO_SUPPORT_VIBRATION_SPEAKER:
        {
#ifdef MTK_VIBSPK_SUPPORT
            property_get(PROPERTY_KEY_VIBSPK_ON, stForFeatureUsage, "1"); //"1": default on
#else
            property_get(PROPERTY_KEY_VIBSPK_ON, stForFeatureUsage, "0"); //"0": default off
#endif
            bSupportFlg = (stForFeatureUsage[0] == '0') ? false : true;
            //ALOGD("IsAudioSupportFeature AUDIO_SUPPORT_VIBRATION_SPEAKER [%d]\n",bSupportFlg);

            break;
        }
        case AUDIO_SUPPORT_EXTERNAL_BUILTIN_MIC:
        {
#ifdef MTK_EXTERNAL_BUILTIN_MIC_SUPPORT
            bSupportFlg = true;
#else
            bSupportFlg = false;
#endif
        }
        case AUDIO_SUPPORT_EXTERNAL_ECHO_REFERENCE:
        {
#ifdef MTK_EXTERNAL_SPEAKER_DAC_SUPPORT
            bSupportFlg = true;
#else
            bSupportFlg = false;
#endif
        }
        default:
            break;
    }

    return bSupportFlg;
}


timespec GetSystemTime(bool print)
{
    struct timespec systemtime;
    int rc;
    rc = clock_gettime(CLOCK_MONOTONIC, &systemtime);
    if (rc != 0)
    {
        systemtime.tv_sec  = 0;
        systemtime.tv_nsec = 0;
        ALOGD("%s() clock_gettime error", __FUNCTION__);
    }
    if (print == true)
    {
        ALOGD("%s(), sec %ld nsec %ld", __FUNCTION__, systemtime.tv_sec, systemtime.tv_nsec);
    }

    return systemtime;
}

static const char *g_phone_mic_propty = "persist.rm.debug.phonemic";
static const char *g_headset_mic_propty = "persist.rm.debug.headsetmic";

uint32_t GetMicDeviceMode(uint32_t mic_category) //0: phonemic, 1: headsetmic
{
    char value[PROPERTY_VALUE_MAX];
    int ret, bflag;
    uint32_t mPhoneMicMode, mHeadsetMicMode;

    if (mic_category == 0)
    {
#ifdef PHONE_MIC_MODE //defined in audio_custom_exp.h
        mPhoneMicMode = PHONE_MIC_MODE;
        ALOGD("PHONE_MIC_MODE defined!, mPhoneMicMode = %d", mPhoneMicMode);
#else
        mPhoneMicMode = AUDIO_MIC_MODE_DCC;
#endif
        // control by setprop
        property_get(g_phone_mic_propty, value, "0");
        bflag = atoi(value);
        if (bflag != 0)
        {
            mPhoneMicMode = bflag;
            ALOGD("mPhoneMicMode getprop, mPhoneMicMode = %d", mPhoneMicMode);
        }
        return mPhoneMicMode;
    }
    else if (mic_category == 1)
    {
#ifdef HEADSET_MIC_MODE //defined in audio_custom_exp.h
        mHeadsetMicMode = HEADSET_MIC_MODE;
        ALOGD("HEADSET_MIC_MODE defined!, mHeadsetMicMode = %d", mHeadsetMicMode);
#else
        mHeadsetMicMode = AUDIO_MIC_MODE_DCC;
#endif
        // control by setprop
        property_get(g_headset_mic_propty, value, "0");
        bflag = atoi(value);
        if (bflag != 0)
        {
            mHeadsetMicMode = bflag;
            ALOGD("mHeadsetMicMode getprop, mHeadsetMicMode = %d", mHeadsetMicMode);
        }
        return mHeadsetMicMode;
    }
    else
    {
        ALOGE("%s() wrong mic_category!!!", __FUNCTION__);
        return 0;
    }
}

}
