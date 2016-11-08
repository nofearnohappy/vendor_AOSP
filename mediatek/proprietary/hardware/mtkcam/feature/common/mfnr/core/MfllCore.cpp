#include "MfllCore.h"
#include "MfllLog.h"

#include "MfllFeatureMrp.h"

#include <utils/threads.h> // ANDROID_PRIORITY_FOREGROUND
#include <cutils/xlog.h> // XLOG
#include <sys/time.h>
#include <vector> // std::vector
#include <map>

using namespace mfll;
using android::sp;
using std::vector;
using std::map;

static map<enum EventType, const char*> EVENTTYPE_NAME = {
    {EventType_Init,                        "EventType_Init"},
    {EventType_AllocateRawBuffer,           "EventType_AllocateRawBuffer"},
    {EventType_AllocateQyuvBuffer,          "EventType_AllocateQyuvBuffer"},
    {EventType_AllocateYuvBase,             "EventType_AllocateYuvBase"},
    {EventType_AllocateYuvGolden,           "EventType_AllocateYuvGolden"},
    {EventType_AllocateYuvWorking,          "EventType_AllocateYuvWorking"},
    {EventType_AllocateYuvMixing,           "EventType_AllocateYuvMixing"},
    {EventType_AllocateWeighting,           "EventType_AllocateWeighting"},
    {EventType_AllocateMemc,                "EventType_AllocateMemc"},
    {EventType_AllocatePostview,            "EventType_AllocatePostview"},
    {EventType_AllocateThumbnail,           "EventType_AllocateThumbnail"},
    {EventType_AllocateJpeg,                "EventType_AllocateJpeg"},
    {EventType_Capture,                     "EventType_Capture"},
    {EventType_CaptureRaw,                  "EventType_CaptureRaw"},
    {EventType_CaptureYuvQ,                 "EventType_CaptureYuvQ"},
    {EventType_CaptureEis,                  "EventType_CaptureEis"},
    {EventType_Bss,                         "EventType_Bss"},
    {EventType_EncodeYuvBase,               "EventType_EncodeYuvBase"},
    {EventType_EncodeYuvGolden,             "EventType_EncodeYuvGolden"},
    {EventType_MotionEstimation,            "EventType_MotionEstimation"},
    {EventType_MotionCompensation,          "EventType_MotionCompensation"},
    {EventType_Blending,                    "EventType_Blending"},
    {EventType_Mixing,                      "EventType_Mixing"},
    {EventType_NoiseReduction,              "EventType_NoiseReduction"},
    {EventType_Postview,                    "EventType_Postview"},
    {EventType_Thumbnail,                   "EventType_Thumbnail"},
    {EventType_EncodeJpeg,                  "EventType_EncodeJpeg"},
    {EventType_Destroy,                     "EventType_Destroy"}
};

const pthread_attr_t PTHREAD_DEFAULT_ATTR = {
    0, NULL, 1024 * 1024, 4096, SCHED_OTHER, ANDROID_PRIORITY_FOREGROUND
};

/* This thread will allocate all buffers sequentially */
static void* thread_allocate_memory(void *arg)
{
    void *ret = 0;
    mfllFunctionIn();

    MfllCore *pCore = (MfllCore*)arg;
    int frameNum = (int)pCore->getBlendFrameNum();

    /* allocate raw buffers frames */
    for(int i = 0; i < frameNum; i++)
        pCore->doAllocRawBuffer((void*)(long)i);

    /* allocate QYUV buffers */
    for(int i = 0; i < frameNum; i++)
        pCore->doAllocQyuvBuffer((void*)(long)i);

    /* ME/MC working buffer */
    for(int i = 0; i < frameNum - 1; i++)
        pCore->doAllocMemcWorking((void*)(long)i);

    /* After BSS done, we can convert RAW to YUV base */
    pCore->doAllocYuvBase(NULL);

    /* YUV working buffer for ME/MC */
    pCore->doAllocYuvWorking(NULL);

    /* Allocate weighting table */
    pCore->doAllocWeighting((void*)(long)0);
    pCore->doAllocWeighting((void*)(long)1);

    /* After BSS done, we can convert RAW to YUV golden too (is not urgent) */
    pCore->doAllocYuvGolden(NULL);

    /* Postview / thumbnail / JPEG related buffers */
    pCore->doAllocPostview(NULL);
    pCore->doAllocThumbnail(NULL);
    pCore->doAllocJpeg(NULL);

lbExit:
    pCore->decStrong(pCore);
    mfllFunctionOut();
    return ret;
}

/**
 *  Thread phase1 has responsibility to capture frames, execute BSS, and encode
 *  YUV base/golden frames.
 */
static void* thread_phase1(void *arg)
{
    void *ret = 0;
    mfllFunctionIn();
    MfllCore *c = (MfllCore*)arg;

    c->doCapture(NULL);
    c->doBss(NULL);
    c->doEncodeYuvBase(NULL);
    c->doEncodeYuvGolden(NULL);

    c->decStrong(c);
    mfllFunctionOut();
    return ret;
}

/**
 *  Since our hardware only provides that only one a pass2 driver, all the
 *  operation invokes pass2 driver will be processed in this thread.
 *
 *  Due to pass2 driver is a thread-safe and FIFO mechanism, every operation
 *  can be dispatched as a sub thread to ask pass2 driver for operation using
 *  greedy algorithm.
 */
static void* thread_phase2(void *arg)
{
    void *ret = 0;
    mfllFunctionIn();
    MfllCore *c = (MfllCore*)arg;
    int times = (int)c->getBlendFrameNum() - 1;

    for (int i = 0; i < times; i++)
        c->doBlending((void*)(long)i);

    c->doMixing(NULL);
    c->doNoiseReduction(NULL);
    c->doCreatePostview(NULL);
    c->doEncodeThumbnail(NULL);
    c->doEncodeJpeg(NULL);
    c->decStrong(c);
    mfllFunctionOut();
    return ret;
}

/**
 *  Thread motion estimation will be invoked parallelly. There will be
 *  (m_frameNum - 1) mfll_thread_me
 */
typedef struct _memc_attr {
    MfllCore *pCore;
    vector<int> indexQueue;
} memc_attr_t;

static void* thread_memc(void *arg_memc_attr)
{
    enum MfllErr err = MfllErr_Ok;
    memc_attr_t *pMemcAttr = (memc_attr_t*)arg_memc_attr;

    MfllCore *c = pMemcAttr->pCore;

    mfllFunctionIn();

    for (size_t i = 0; i < pMemcAttr->indexQueue.size(); i++ )
    {
        void *void_index = (void*)(long)pMemcAttr->indexQueue[i];
        err = c->doMotionEstimation(void_index);
        if (err != MfllErr_Ok) {
            mfllLogW("%s: ME(%d) failed, ignore MC and blending(%d)", __FUNCTION__, i, i);
            c->m_bypass.bypassMotionCompensation[i] = 1;
            c->m_bypass.bypassBlending[i] = 1;
        }

        err = pMemcAttr->pCore->doMotionCompensation(void_index); 
        if (err != MfllErr_Ok) {
            mfllLogW("%s: MC(%d) failed, ignore blending(%d)", __FUNCTION__, i, i);
            c->m_bypass.bypassMotionCompensation[i] = 1;
        }
    }

lbExit:
    mfllFunctionOut();
    return (void*)(long)err;

}

static void* thread_memc_parallel(void *arg)
{
    void *ret = 0;
    mfllFunctionIn();
    MfllCore *c = (MfllCore*)arg;

    int times = (int)c->getBlendFrameNum() - 1;
    int instanceNum = (int)c->getMemcInstanceNum();
    int threadsNum = (times <= instanceNum ? times : instanceNum);

    mfllLogD("%s: times to blend(%d), MEMC instanceNum(%d), threadsNum(%d)", __FUNCTION__, times, instanceNum, threadsNum);

    memc_attr_t *attrs = new memc_attr_t[threadsNum]; 
    pthread_t *pThreads = new pthread_t[threadsNum];
    pthread_attr_t pthreadAttr = PTHREAD_DEFAULT_ATTR;

    /* create threadsNum threads for executing ME/MC */
    for (int i = 0; i < times; i++) {
        attrs[i % threadsNum].indexQueue.push_back(i);
        attrs[i % threadsNum].pCore = c;
    }

    for (int i = 0; i < threadsNum; i++) {
        pthread_create((pThreads + i), &pthreadAttr, thread_memc, (void*)&attrs[i]);
    }

    /* sync threads */
    for (int i = 0; i < threadsNum; i++) {
        void *r;
        pthread_join(*(pThreads + i), (void**)&r);
    }

lbExit:
    delete [] pThreads;
    delete [] attrs;

    c->decStrong(c);
    mfllFunctionOut();
    return ret;
}

/* this thread will process ME/MC sequentially */
static void* thread_memc_seq(void *arg)
{
    enum MfllErr err = MfllErr_Ok;

    mfllFunctionIn();

    MfllCore *c = (MfllCore*)arg;
    int times = (int)c->getBlendFrameNum() - 1;

    memc_attr_t memcAttr;
    memcAttr.pCore = c;
    for (int i = 0; i < times; i++) {
        memcAttr.indexQueue.push_back(i);
    }

    err = (enum MfllErr)(long)thread_memc((void*)&memcAttr);

    c->decStrong(c);
    mfllFunctionOut();

    return (void*)(long)err;
}

IMfllCore* IMfllCore::createInstance(void)
{
    return (IMfllCore*)new MfllCore;
}

enum MfllErr IMfllCore::getCaptureInfo(const struct MfllConfig *pCfgIn, struct MfllConfig *pCfgOut)
{
    return MfllErr_NotImplemented;
}

const char* IMfllCore::getEventTypeName(enum EventType e)
{
    unsigned int index = (unsigned int)e;
    if (e < 0 || e >= (unsigned int)EventType_Size)
        return "Unknown_EventType";
    return EVENTTYPE_NAME[e];
}

bool IMfllCore::isZsdMode(const enum MfllMode &m)
{
    return (m == MfllMode_ZsdMfll || m == MfllMode_ZsdAis) ? true : false;
}

/**
 *  MfllCore
 */
MfllCore::MfllCore(void)
{
    mfllFunctionIn();

    /* modules */
    m_spCapturer = NULL;
    /* event dispatcher */
    m_event = IMfllEvents::createInstance();
    /* attributes */
    m_frameNum = MFLL_BLEND_FRAME;
    m_frameNumCaptured = MFLL_CAPTURE_FRAME;
    m_memcInstanceNum = 1;
    m_bShooted = false;
    m_sensorId = -1;
    m_mutexShoot = PTHREAD_MUTEX_INITIALIZER;
    m_privateData = NULL;
    m_privateDataSize = 0;

    /* set image buffer smart pointers to NULL */
    m_imgYuvBase = NULL;
    m_imgYuvGolden = NULL;
    m_imgYuvBlended = NULL;
    m_imgWeighting[0] = NULL;
    m_imgWeighting[1] = NULL;

    /* pointers of IMfllImageBuffers */
    IMfllImageBuffer *m_ptrImgYuvBase = NULL;
    IMfllImageBuffer *m_ptrImgYuvRef = NULL;
    IMfllImageBuffer *m_ptrImgYuvGolden = NULL;
    IMfllImageBuffer *m_ptrImgYuvBlended = NULL;
    IMfllImageBuffer *m_ptrImgYuvMixed = NULL;
    IMfllImageBuffer *m_ptrImgWeightingIn = NULL;
    IMfllImageBuffer *m_ptrImgWeightingOut = NULL;
    IMfllImageBuffer *m_ptrImgWeightingFinal = NULL;


    /* sync objects, lock as default */
    lockSyncObject(&m_syncCapture);
    lockSyncObject(&m_syncEncodeYuvBase);
    lockSyncObject(&m_syncEncodeYuvGolden);
    lockSyncObject(&m_syncBss);
    lockSyncObject(&m_syncMixing);
    lockSyncObject(&m_syncNoiseReduction);
    lockSyncObject(&m_syncPostview);
    lockSyncObject(&m_syncThumbnail);
    lockSyncObject(&m_syncEncJpeg);

    for (unsigned int i = 0; i < MFLL_MAX_FRAMES; i++) {
        /* set image buffer smart pointers = NULL */
        m_imgRaws[i] = NULL;
        m_imgQYuvs[i] = NULL;
        m_imgMemc[i] = NULL;
        /* sync object for operation */
        lockSyncObject(&m_syncCapturedRaw[i]);
        lockSyncObject(&m_syncCapturedYuvQ[i]);
        lockSyncObject(&m_syncMotionEstimation[i]);
        lockSyncObject(&m_syncMotionCompensation[i]);
        lockSyncObject(&m_syncBlending[i]);
    }

    mfllLogD("Create MfllCore version: %s",((std::string)getReversionString()).c_str());
    mfllFunctionOut();
}

MfllCore::~MfllCore(void)
{
    MfllErr err;
    int iResult;
    MfllEventStatus_t status;

    mfllFunctionIn();
    m_event->onEvent(EventType_Destroy, status, (void*)this);

    /* release sync data */
    if (m_privateData) {
        delete m_privateData;
        m_privateDataSize = 0;
    }

    m_event->doneEvent(EventType_Destroy, status, (void*)this);
    mfllFunctionOut();
}

enum MfllErr MfllCore::init(const MfllConfig_t *pCfg /* = 0 */)
{
    MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();

    pthread_mutex_lock(&m_mutexShoot);
    if (m_bShooted) {
        err = MfllErr_Shooted;
        goto lbExit;
    }

    m_event->onEvent(EventType_Init, status, (void*)this);

    /* assign pointers to real buffer */
    m_ptrImgYuvBase = m_imgYuvBase.get();
    m_ptrImgYuvGolden = m_imgYuvGolden.get();
    m_ptrImgYuvBlended = m_imgYuvBlended.get();

    /* check parameter is ok or not */
    if (getBlendFrameNum() <= 0) {
        err = MfllErr_IllegalBlendFrameNum;
        mfllLogE("%s: blending frame number is <= 0", __FUNCTION__);
        goto lbExit;
    }

    if (getCaptureFrameNum() <= 0 || getCaptureFrameNum() < getBlendFrameNum()) {
        err = MfllErr_IllegalCaptureFrameNum;
        mfllLogE("%s: catpure frame num is illegal.", __FUNCTION__);
        goto lbExit;
    }

lbExit:
    pthread_mutex_unlock(&m_mutexShoot);

    status.err = err;
    m_event->doneEvent(EventType_Init, status, (void*)this);

    if (err == MfllErr_Shooted)
        mfllLogW("%s: MFLL has shooted, cannot init MFLL anymore", __FUNCTION__);

    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doMfll(void)
{
    MfllFeatureOpt_t opts;
    return doMfll(opts);
}

enum MfllErr MfllCore::doMfll(MfllFeatureOpt_t featureOpts)
{
    MfllErr err = MfllErr_Ok;
    std::vector<pthread_t> vThreadToJoin;
    pthread_t pThread;
    pthread_t pThreadPhase1;
    void *ret;
    std::vector<pthread_t>::iterator itr;


    /* function pointer to memc */
    void* (*ptr_thread_memc)(void*) = NULL;

    /* only Dual Phase may invoke Mfll async */
    bool bIsAsync = (featureOpts.dualphase_mode != 0 ? true : false);

    mfllFunctionIn();

    /* init first */
    init();

    /* use mutex to protect operating shoot state */
    pthread_mutex_lock(&m_mutexShoot);
    if (m_bShooted) {
        pthread_mutex_unlock(&m_mutexShoot);
        mfllLogW("MFLL has been shooted, cancel this shoot");
        err = MfllErr_Shooted;
        goto lbExit;
    }
    else {
        m_bShooted = true;
        pthread_mutex_unlock(&m_mutexShoot);
    }

    /**
     *  M F L L    S T A R T
     */

    /* register Memory Reduce Plan feature */
    //if (featureOpts.mrp_mode != MrpMode_BestPerformance) {
    if (1) {
        registerEventListenerNoLock(new MfllFeatureMrp);
    }

    /* assign ME/MC thread */
    switch(featureOpts.memc_mode){
        case MemcMode_Sequential:
            ptr_thread_memc = &thread_memc_seq;
            break;
        case MemcMode_Parallel:
            m_memcInstanceNum = MFLL_MEMC_THREADS_NUM;
            ptr_thread_memc = &thread_memc_parallel;
            break;
        default:
            mfllLogE("%s: memc_mode(%d) is out of range. Use memc_seq as default", __FUNCTION__, (int)featureOpts.memc_mode);
            ptr_thread_memc = &thread_memc_seq;
            break;
    }

    /* thread to allocate buffers sequentially */
    createThread(&pThread, thread_allocate_memory);
    vThreadToJoin.push_back(pThread);
    /* thread for capture image->bss->raw2yuv */
    createThread(&pThreadPhase1, thread_phase1);
    /* thread to do ME&MC */
    createThread(&pThread, (*ptr_thread_memc));
    vThreadToJoin.push_back(pThread);
    /* thread for blending->mixing->NR->Postview->JPEG */
    createThread(&pThread, thread_phase2);
    vThreadToJoin.push_back(pThread);

    /**
     *  Ok, here we need to sync threads.
     */

    /* Phase1 should be always joined. */
    err = joinThread(&pThreadPhase1);

    /* if not async call, we have to join these threads */
    for( itr = vThreadToJoin.begin(); itr != vThreadToJoin.end(); itr++) {
        pthread_t t = (*itr);

        if (bIsAsync == false) {
            pthread_join(t, &ret);
        }
        else {
            pthread_detach(t);
        }
    }

lbExit:
    mfllFunctionOut();
    return err;
}

IMfllImageBuffer* MfllCore::getRawBuffer(unsigned int index)
{
    if (index >= getCaptureFrameNum())
        return NULL;
    return m_imgRaws[index].get();
}

vector<IMfllImageBuffer*> MfllCore::getRawBuffers(void)
{
    vector<IMfllImageBuffer*> v;
    for (int i = 0; i < (int)getCaptureFrameNum(); i++) {
        v.push_back(m_imgRaws[i].get());
    }
    return v;
}

int MfllCore::getSensorId(void)
{
    return m_sensorId;
}

IMfllImageBuffer* MfllCore::getQYuvBuffer(unsigned int index)
{
    if (index >= getCaptureFrameNum())
        return NULL;
    return m_imgQYuvs[index].get();
}

vector<IMfllImageBuffer*> MfllCore::getQYuvBuffers(void)
{
    vector<IMfllImageBuffer*> v;
    for (int i = 0; i < (int)getCaptureFrameNum(); i++) {
        v.push_back(m_imgQYuvs[i].get());
    }
    return v;
}


unsigned int MfllCore::getReversion(void)
{
    unsigned int v0;
    unsigned int v1;
    unsigned int v2;

    v0 = MFLL_CORE_REVERSION_BUGFIX;
    v1 = MFLL_CORE_REVERSION_MINOR;
    v2 = MFLL_CORE_REVERSION_MAJOR;

    return (v2 << 20) | (v1 << 12) | v0;
}

std::string MfllCore::getReversionString(void)
{
    std::string s;
    s += std::to_string(MFLL_CORE_REVERSION_MAJOR);
    s += ".";
    s += std::to_string(MFLL_CORE_REVERSION_MINOR);
    s += ".";
    s += std::to_string(MFLL_CORE_REVERSION_BUGFIX);
    return s;
}

bool MfllCore::isShooted(void)
{
    bool b = false;
    pthread_mutex_lock(&m_mutexShoot);
    b = m_bShooted;
    pthread_mutex_unlock(&m_mutexShoot);
    return b;
}

enum MfllErr MfllCore::registerEventListener(const sp<IMfllEventListener> &e)
{
    bool b = false;

    pthread_mutex_lock(&m_mutexShoot);
    b = m_bShooted;
    pthread_mutex_unlock(&m_mutexShoot);

    if (b) {
        mfllLogW("%s: Mfll is shooted, ignored.", __FUNCTION__);
        return MfllErr_Shooted;
    }

    return registerEventListenerNoLock(e);
}

enum MfllErr MfllCore::registerEventListenerNoLock(const sp<IMfllEventListener> &e)
{
    mfllFunctionIn();

    vector<enum EventType> t = e->getListenedEventTypes();
    for (size_t i = 0; i < t.size(); i++) {
        m_event->registerEventListener(t[i], e);
    }

    mfllFunctionOut();
    return MfllErr_Ok;
}


enum MfllErr MfllCore::removeEventListener(IMfllEventListener *e)
{
    bool b = false;

    pthread_mutex_lock(&m_mutexShoot);
    b = m_bShooted;
    pthread_mutex_unlock(&m_mutexShoot);

    if (b) {
        mfllLogW("%s: Mfll is shooted, ignored.", __FUNCTION__);
        return MfllErr_Shooted;
    }

    vector<enum EventType> t = e->getListenedEventTypes();
    for (size_t i = 0; i < t.size() ; i++) {
        m_event->removeEventListener(t[i], e);
    }
    return MfllErr_Ok;
}

enum MfllErr MfllCore::setBypassOption(const MfllBypassOption_t &b)
{
    bool shooted = false;

    pthread_mutex_lock(&m_mutexShoot);
    shooted = m_bShooted;
    pthread_mutex_unlock(&m_mutexShoot);

    if (shooted) {
        mfllLogW("%s: Mfll is shooted, ignored.", __FUNCTION__);
        return MfllErr_Shooted;
    }

    m_bypass = b;

    return MfllErr_Ok;
}

enum MfllErr MfllCore::setCaptureResolution(unsigned int width, unsigned int height)
{
    enum MfllErr err = MfllErr_Ok;

    pthread_mutex_lock(&m_mutexShoot);
    if (m_bShooted) {
        err = MfllErr_Shooted;
        goto lbExit;
    }

    m_width = width;
    m_height = height;

lbExit:
    pthread_mutex_unlock(&m_mutexShoot);
    if (err == MfllErr_Shooted)
        mfllLogW("MFLL has shooted, cannot set resolution");
    return err;
}

enum MfllErr MfllCore::setCaptureQResolution(unsigned int qwidth, unsigned int qheight)
{
    enum MfllErr err = MfllErr_Ok;

    pthread_mutex_lock(&m_mutexShoot);
    if (m_bShooted) {
        err = MfllErr_Shooted;
        goto lbExit;
    }

    m_qwidth = qwidth;
    m_qheight = qheight;

lbExit:
    pthread_mutex_unlock(&m_mutexShoot);
    if (err == MfllErr_Shooted)
        mfllLogW("MFLL has shooted, cannot set Q resolution");
    return err;
}

enum MfllErr MfllCore::setCapturer(const sp<IMfllCapturer> &capturer)
{
    enum MfllErr err = MfllErr_Ok;

    pthread_mutex_lock(&m_mutexShoot);
    if (m_bShooted) {
        err = MfllErr_Shooted;
        goto lbExit;
    }

    m_spCapturer = capturer;

lbExit:
    pthread_mutex_unlock(&m_mutexShoot);
    if (err == MfllErr_Shooted)
        mfllLogW("MFLL has shooted, cannot set Q resolution");
    return err;
}

/**
 *  ---------------------------------------------------------------------------
 *  MFLL OPERATIONS
 *
 *  These functions are operations that MFLL will invoke. Function name template
 *  is do{$action} where {$action} is the meaniful name for the operation
 *  -----------------------------------------------------------------------------
 */

/**
 *  The first operation of MFLL is to capture frames, but it's necessary to wait
 *  RAW buffers and QYUV buffers for capture are ready.
 *
 *  We have to capture getBlendFrameNum() RAW and QYUV frames by IMfllCapturer
 */
enum MfllErr MfllCore::doCapture(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;
    mfllFunctionIn();

    /* invokes events */
    m_event->onEvent(EventType_Capture, status, this);

    if (m_bypass.bypassCapture || status.ignore) {
        mfllLogD("%s: Bypass capture operation", __FUNCTION__);
        usleep(500 * 1000); // assume 500 ms
        status.ignore = 1;
    }
    else {
        /* aquire buffers first */
        for (int i = 0; i < (int)getCaptureFrameNum(); i++) {
            err = doAllocRawBuffer((void*)(long)i);
            if (err != MfllErr_Ok) {
                mfllLogE("%s: allocate RAW buffer(%d) failed", __FUNCTION__, i);
                goto lbExit;
            }

            err = doAllocQyuvBuffer((void*)(long)i);
            if (err != MfllErr_Ok) {
                mfllLogE("%s: allocate QYUV buffer (%d) failed", __FUNCTION__, i);
                goto lbExit;
            }
        }

        /* check if IMfllCapturer has been assigned */
        if (m_spCapturer.get() == NULL) {
            mfllLogD("%s: create MfllCapturer", __FUNCTION__);
            m_spCapturer = IMfllCapturer::createInstance();
            if (m_spCapturer.get() == NULL) {
                mfllLogE("%s: create MfllCapturer instance", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
        }

        vector< sp<IMfllImageBuffer> > raws;
        vector< sp<IMfllImageBuffer> > qyuvs;
        vector<MfllMotionVector_t> gmvs;

        /* prepare IMfllImageBuffer to IMfllCapturer */
        for (int i = 0; i < (int)getCaptureFrameNum(); i++) {
            raws.push_back(m_imgRaws[i]);
            qyuvs.push_back(m_imgQYuvs[i]);
            gmvs.push_back(MfllMotionVector_t());
        }

        /* register event dispatcher */
        err = m_spCapturer->registerEventDispatcher(m_event);

        if (err != MfllErr_Ok) {
            mfllLogE("%s: MfllCapture::registerEventDispatcher failed with code %d", __FUNCTION__, err);
            goto lbExit;
        }

        /* Catpure frames */
        err = m_spCapturer->captureFrames(getCaptureFrameNum(), raws, qyuvs, gmvs);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: MfllCapture::captureFrames failed with code %d", __FUNCTION__, err);
            goto lbExit;
        }

        /* copy global motion vector back */
        for(int i = 0; i < (int)getCaptureFrameNum() - 1; i++) {
            m_globalMv[i] = gmvs[i];
        }
    }

lbExit:
    handleError(err);
    status.err = err;
    m_event->doneEvent(EventType_Capture, status, this);
    syncAnnounceDone(&m_syncCapture);
    mfllFunctionOut();
    return err;
}

/**
 *  Best Shot Selection should takes captured images
 */
enum MfllErr MfllCore::doBss(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncCapture);
    /* trigger events */
    m_event->onEvent(EventType_Bss, status, this);

    if (m_bypass.bypassBss || status.ignore) {
        mfllLogD("%s: Bypass bss", __FUNCTION__);
        usleep(40 * 1000);
        status.ignore = 1;
    }
    else {
    }

lbExit:
    handleError(err);
    status.err = err;
    m_event->doneEvent(EventType_Bss, status, this);
    syncAnnounceDone(&m_syncBss);
    mfllFunctionOut();
    return err;
}

/**
 *  To encode base RAW buffer to YUV base buffer. This operation must be invoked
 *  after doBss() has done and YUV base buffer is available to use. Hence the
 *  conditions are:
 *
 *  1. Buffer of YUV base is ready
 *  2. BSS has been done
 */
enum MfllErr MfllCore::doEncodeYuvBase(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncBss);
    /* invokes events */
    m_event->onEvent(EventType_EncodeYuvBase, status, this);

    if (m_bypass.bypassEncodeYuvBase || status.ignore) {
        mfllLogD("%s: Bypass encoding YUV base", __FUNCTION__);
        usleep(50 * 1000);
        status.ignore = 1;
    }
    else {
        err = doAllocYuvBase(NULL);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: allocate YUV base failed", __FUNCTION__);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }

        // TODO: encode base RAW to YUV base
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_EncodeYuvBase, status, this);
    syncAnnounceDone(&m_syncEncodeYuvBase);
    mfllFunctionOut();
    return err;
}

/**
 *  To encode base RAW buffer to YUV golden buffer. This operation must be invoked
 *  after doBss() has done and YUV golden buffer is available to use.
 *
 *  1. Buffer of YUV golden is ready
 *  2. BSS has been done
 */
enum MfllErr MfllCore::doEncodeYuvGolden(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncBss);
    /* invokes events */
    m_event->onEvent(EventType_EncodeYuvGolden, status, this); // invokes events

    if (m_bypass.bypassEncodeYuvGolden || status.ignore) {
        mfllLogD("%s: Bypass encoding YUV golden", __FUNCTION__);
        usleep(50 * 1000); // 50 ms
        status.ignore = 1;
    }
    else {
        err = doAllocYuvGolden(NULL);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: allocate YUV golden failed", __FUNCTION__);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }

        // TODO: encode base RAW to YUV golden
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_EncodeYuvGolden, status, this);
    syncAnnounceDone(&m_syncEncodeYuvGolden);
    mfllFunctionOut();
    return err;
}

/**
 *  Calculate motion estimation using MfllMemc. This operation must be invoked
 *  while these conditions are matched:
 *
 *  1. BSS has been done
 *  2. memory for MfllMemc should be ready too.
 */
enum MfllErr MfllCore::doMotionEstimation(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionInInt(index);
    /* conditions */
    syncWaitDone(&m_syncBss);
    /* invokes event */
    m_event->onEvent(EventType_MotionEstimation, status, this, (void*)(long)index);

    if (m_bypass.bypassMotionEstimation[index] || status.ignore) {
        mfllLogD("%s: Bypass motion estimation(%d)", __FUNCTION__, index);
        usleep(100 * 1000); // 100 ms
        status.ignore = 1;
    }
    else {
        err = doAllocMemcWorking(void_index);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: allocate MEMC working buffer(%d) failed", __FUNCTION__, index);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }

        // TODO: do motion estimation
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_MotionEstimation, status, this, (void*)(long)index);
    syncAnnounceDone(&m_syncMotionEstimation[index]);
    mfllFunctionOut();
    return err;
}

/**
 *  Motion compensation is a pure software algorithm, the i-th motion compensation
 *  should be executed if only if:
 *
 *  1. The i-th motion estimation has been done.
 *  2. The (i-1)-th compensation has been done (if i > 1)
 */
enum MfllErr MfllCore::doMotionCompensation(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionInInt(index);
    /* conditions */
    syncWaitDone(&m_syncMotionEstimation[index]);
    if (index > 0)
        syncWaitDone(&m_syncMotionCompensation[index - 1]);
    /* trigger events */
    m_event->onEvent(EventType_MotionCompensation, status, this, (void*)(long)index);

    if (m_bypass.bypassMotionCompensation[index] || status.ignore) {
        mfllLogD("%s: Bypass motion compensation(%d)", __FUNCTION__, index);
        usleep(50 * 1000); // 50 ms
        status.ignore = 1;
    }
    else {
        // TODO: do motion compensation.
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_MotionCompensation, status, this, (void*)(long)index);
    syncAnnounceDone(&m_syncMotionCompensation[index]);
    mfllFunctionOut();
    return err;
}

/**
 *  Blending using MTK hardware, furthurmore, operation blending depends on that
 *  the previous blended output. Therefore, the conditions of this operaion are:
 *
 *  1. YUV working buffer for blending is ready.
 *  2. The i-th motion compensation has been done.
 *  3. The (i-1)-th blending has been done.
 */
enum MfllErr MfllCore::doBlending(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionInInt(index);
    /* conditions */
    syncWaitDone(&m_syncMotionCompensation[index]);
    if (index > 0)
        syncWaitDone(&m_syncBlending[index - 1]);
    /* trigger events */
    m_event->onEvent(EventType_Blending, status, this, (void*)(long)index);

    if (m_bypass.bypassBlending[index] || status.ignore) {
        mfllLogD("%s: Bypass blending(%d)", __FUNCTION__, index);
        usleep(50 * 1000);
        status.ignore = 1;
    }
    else {
        err = doAllocYuvWorking(NULL);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: allocate YUV working buffer failed", __FUNCTION__);
            goto lbExit;
        }

        if (m_spMfb.get() == NULL) {
            mfllLogD("%s: create IMfllMfb instance", __FUNCTION__);
            m_spMfb = IMfllMfb::createInstance();
            if (m_spMfb.get() == NULL) {
                mfllLogE("%s: m_spMfb is NULL", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
        }

        err = m_spMfb->init(m_sensorId); // TODO: pass in the correct sensor ID
        err = m_spMfb->setSyncPrivateData(m_privateData, m_privateDataSize);

        if (err != MfllErr_Ok) {
            mfllLogE("%s: m_spMfb init failed with code %d", __FUNCTION__, (int)err);
            goto lbExit;
        }

        /* prepare weighting buffers */
        if (index % 2 == 0) {
            m_ptrImgWeightingIn = m_imgWeighting[0].get();
            m_ptrImgWeightingOut = m_imgWeighting[1].get();
        }
        else {
            m_ptrImgWeightingIn = m_imgWeighting[1].get();
            m_ptrImgWeightingOut = m_imgWeighting[0].get();
        }


        err = m_spMfb->blend(m_ptrImgYuvBase, m_ptrImgYuvRef, m_ptrImgYuvBlended, m_ptrImgWeightingIn, m_ptrImgWeightingOut);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: Mfb failed with code %d", __FUNCTION__, (int)err);
            goto lbExit;
        }
        else {
            /* save for mixing */
            m_ptrImgWeightingFinal = m_ptrImgWeightingOut;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_Blending, status, this, (void*)(long)index);
    syncAnnounceDone(&m_syncBlending[index]);
    mfllFunctionOut();
    return err;
}

/**
 *  Mixing a blended frame and a golden frame. Therefore, we must have a blended
 *  frame which means that this operation must wait until blending done. Hence
 *  the conditions are
 *
 *  1. YUV mixing output frame buffer ready
 *  2. All blending has been done.
 */
enum MfllErr MfllCore::doMixing(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    for (int i = 0; i < (int)getBlendFrameNum() - 1; i++)
        syncWaitDone(&m_syncBlending[i]);
    /* trigger events */
    m_event->onEvent(EventType_Mixing, status, this);

    if (m_bypass.bypassMixing || status.ignore) {
        mfllLogD("%s: Bypass mixing", __FUNCTION__);
        usleep(50 * 1000);
        status.ignore = 1;
    }
    else {
        if (m_spMfb.get() == NULL) {
            m_spMfb = IMfllMfb::createInstance();
            if (m_spMfb.get() == NULL) {
                mfllLogE("%s: create MFB instance failed", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
        }

        /* set sensor ID and private data for sync */
        err = m_spMfb->init(m_sensorId);
        err = m_spMfb->setSyncPrivateData(m_privateData, m_privateDataSize);

        if (err != MfllErr_Ok) {
            mfllLogE("%s: init MFB instance failed with code %d", __FUNCTION__, (int)err);
            goto lbExit;
        }

        err = m_spMfb->setWeightingBuffer(m_imgWeighting[0], m_imgWeighting[1]);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: set weighting buffers failed with code %d", __FUNCTION__, (int)err);
            goto lbExit;
        }

        m_ptrImgYuvMixed = m_ptrImgYuvBase; // re-use YUV base frame as output.
        err = m_spMfb->mix(m_ptrImgYuvBlended, m_ptrImgYuvGolden, m_ptrImgYuvMixed);
        if (err != MfllErr_Ok) {
            mfllLogE("%s: mix failed with code %d", __FUNCTION__, err);
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_Mixing, status, this);
    syncAnnounceDone(&m_syncMixing);
    mfllFunctionOut();
    return err;
}

/**
 *  Noise reduction is processed after mixing has been done. So the conditions
 *  are
 *
 *  1. YUV mixing has been done
 */
enum MfllErr MfllCore::doNoiseReduction(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncMixing);
    /* trigger events */
    m_event->onEvent(EventType_NoiseReduction, status, this);

    if (m_bypass.bypassNoiseReduction || status.ignore) {
        mfllLogD("%s: Bypass noise reduction", __FUNCTION__);
        usleep(170 * 1000);
        status.ignore = 1;
    }
    else {
        // TODO: do NR
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_NoiseReduction, status, this);
    syncAnnounceDone(&m_syncNoiseReduction);
    mfllFunctionOut();
    return err;
}

/**
 *  After all post-processings are done, creating Postview for application.
 *  We notic that the last post-processing is Noise Reduction and which cannot
 *  be processed parallelly therefore we can assume the conditions for this
 *  funcion are
 *
 *  1. Noise Reduction has been done.
 *  2. Postview related buffers are ready.
 */
enum MfllErr MfllCore::doCreatePostview(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncNoiseReduction);
    /* trigger events */
    m_event->onEvent(EventType_Postview, status, this);

    if (m_bypass.bypassPostview || status.ignore) {
        mfllLogD("%s: Bypass create postview", __FUNCTION__);
        usleep(10 * 1000);
        status.ignore = 1;
    }
    else {
        // TODO: create postview
    }

lbExit:
    handleError(err);
    status.err = err;
    m_event->doneEvent(EventType_Postview, status, this);
    syncAnnounceDone(&m_syncPostview);
    mfllFunctionOut();
    return err;
}

/**
 *  Thumbnail can be created after all the post-processings are done, and buffers
 *  for Thumbnail are ready. Hence conditions are
 *
 *  1. Noise Reduction has been done.
 *  2. Thumbnail related buffers are ready.
 */
enum MfllErr MfllCore::doEncodeThumbnail(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncNoiseReduction);
    /* trigger events */
    m_event->onEvent(EventType_Thumbnail, status, this);

    if (m_bypass.bypassThumbnail || status.ignore) {
        mfllLogD("%s: Bypass create thumbnail", __FUNCTION__);
        usleep(25 * 1000);
        status.ignore = 1;
    }
    else {
        // TODO: create thumbnail.
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_Thumbnail, status, this);
    syncAnnounceDone(&m_syncThumbnail);
    mfllFunctionOut();
    return err;
}

/**
 *  JPEG can be encoded after all the post-processings are done, and buffers
 *  for encoding JPEG are ready. Hence the conditions are
 *
 *  1. Noise Reduction has been done.
 *  2. JPEG encoding related buffers are ready.
 */
enum MfllErr MfllCore::doEncodeJpeg(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* conditions */
    syncWaitDone(&m_syncNoiseReduction);
    /* trigger events */
    m_event->onEvent(EventType_EncodeJpeg, status, this);

    if (m_bypass.bypassEncodeJpeg || status.ignore) {
        mfllLogD("%s: Bypass encoding JPEG", __FUNCTION__);
        usleep(70 * 1000);
        status.ignore = 1;
    }
    else {
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_EncodeJpeg, status, this);
    syncAnnounceDone(&m_syncEncJpeg);
    mfllFunctionOut();
    return MfllErr_NotImplemented;
}

enum MfllErr MfllCore::doAllocRawBuffer(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateRawBuffer, status, this, (void*)(long)index);

    if (m_bypass.bypassAllocRawBuffer[index] || status.ignore) {
        mfllLogD("%s: Bypass allocate raw buffers", __FUNCTION__);
        usleep(50 * 1000);
        status.ignore = 1;
    }
    else {
        /* create IMfllImageBuffer instances */
        IMfllImageBuffer *pImg = m_imgRaws[index].get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create IMfllImageBuffer(%d) failed", __FUNCTION__, index);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
            m_imgRaws[index] = pImg;
        }

        pImg->setImageFormat(ImageFormat_Raw10);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init raw buffer(%d) failed", __FUNCTION__, index);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateRawBuffer, status, this, (void*)(long)index);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocQyuvBuffer(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateQyuvBuffer, status, this, (void*)(long)index);

    if (m_bypass.bypassAllocQyuvBuffer[index] || status.ignore) {
        mfllLogD("%s: Bypass allocate QYUV buffers", __FUNCTION__);
        usleep(50 * 1000);
        status.ignore = 1;
    }
    else {
        IMfllImageBuffer *pImg = m_imgQYuvs[index].get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create QYUV buffer instance (%d) failed", __FUNCTION__, index);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
            m_imgQYuvs[index] = pImg;
        }

        pImg->setImageFormat(ImageFormat_Yuy2);
        pImg->setResolution(m_qwidth, m_qheight);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init QYUV buffer(%d) failed", __FUNCTION__, index);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateQyuvBuffer, status, this, (void*)(long)index);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocYuvBase(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateYuvBase, status, this);

    if (m_bypass.bypassAllocYuvBase || status.ignore) {
        mfllLogD("%s: Bypass allocate YUV base buffer", __FUNCTION__);
        usleep(100 * 1000);
        status.ignore = 1;
    }
    else {
        IMfllImageBuffer *pImg = m_imgYuvBase.get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create YUV base buffer instance failed", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
            m_imgYuvBase = pImg;
        }

        pImg->setImageFormat(ImageFormat_Yuy2);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init YUV base buffer failed", __FUNCTION__);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateYuvBase, status, this);
    mfllFunctionOut();
    return err;
}


enum MfllErr MfllCore::doAllocYuvGolden(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateYuvGolden, status, this);

    if (m_bypass.bypassAllocYuvGolden || status.ignore) {
        mfllLogD("%s: Bypass allocate YUV golen buffer", __FUNCTION__);
        usleep(100 * 1000);
        status.ignore = 1;
    }
    else {
        IMfllImageBuffer *pImg = m_imgYuvGolden.get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create YUV golden instance failed", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }
            m_imgYuvGolden = pImg;
        }

        pImg->setImageFormat(ImageFormat_Yuy2);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init YUV golden buffer failed", __FUNCTION__);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateYuvGolden, status, this);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocYuvWorking(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateYuvWorking, status, this);

    if (m_bypass.bypassAllocYuvWorking || status.ignore) {
        mfllLogD("%s: Bypass allocate YUV working(mixing) buffer", __FUNCTION__);
        usleep(100 * 1000);
        status.ignore = 1;
    }
    else {
        IMfllImageBuffer *pImg = m_imgYuvBlended.get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create YUV blended buffer instance failed", __FUNCTION__);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }

            m_imgYuvBlended = pImg;
        }
        pImg->setImageFormat(ImageFormat_Yuy2);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init YUV blended buffer failed", __FUNCTION__);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateYuvWorking, status, this);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocWeighting(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateWeighting, status, this, (void*)(long)index);

    if (m_bypass.bypassAllocWeighting[index] || status.ignore) {
        mfllLogD("%s: Bypass allocate weighting table(%d)", __FUNCTION__, index);
        usleep(10 * 1000);
        status.ignore = 1;
    }
    else {
        /* index should be smaller than 2 or it's the critical condition */
        if (index >= 2) {
            mfllLogE("%s: index(%d) of weighting table is greater than 2", __FUNCTION__, index);
            abort();
        }

        IMfllImageBuffer *pImg = m_imgWeighting[index].get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create weighting table(%d) buffer instance failed", __FUNCTION__, index);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }

            m_imgWeighting[index] = pImg;
        }
        pImg->setImageFormat(ImageFormat_Y8);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init weighting table(%d) buffer failed", __FUNCTION__, index);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateWeighting, status, this, (void*)(long)index);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocMemcWorking(void *void_index)
{
    unsigned int index = (unsigned int)(long)void_index;
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateMemc, status, this, (void*)(long)index);

    if (m_bypass.bypassAllocMemc[index] || status.ignore) {
        mfllLogD("%s: Bypass allocate MEMC working buffer(%d)", __FUNCTION__, index);
        usleep(20 * 1000);
        status.ignore = 1;
    }
    else {
        IMfllImageBuffer *pImg = m_imgMemc[index].get();
        if (pImg == NULL) {
            pImg = IMfllImageBuffer::createInstance();
            if (pImg == NULL) {
                mfllLogE("%s: create MEMC working buffer(%d) instance failed", __FUNCTION__, index);
                err = MfllErr_CreateInstanceFailed;
                goto lbExit;
            }

            m_imgMemc[index] = pImg;
        }
        pImg->setImageFormat(ImageFormat_Y8);
        pImg->setResolution(m_width, m_height);
        err = pImg->initBuffer();
        if (err != MfllErr_Ok) {
            mfllLogE("%s: init MEMC working buffer(%d) failed", __FUNCTION__, index);
            err = MfllErr_UnexpectedError;
            goto lbExit;
        }
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateMemc, status, this, (void*)(long)index);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocPostview(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocatePostview, status, this);

    if (m_bypass.bypassAllocPostview) {
        usleep(10 * 1000);
        status.ignore = 1;
    }
    else {
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocatePostview, status, this);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocThumbnail(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateThumbnail, status, this);

    if (m_bypass.bypassAllocThumbnail) {
        usleep(10 * 1000);
        status.ignore = 1;
    }
    else {
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateThumbnail, status, this);
    mfllFunctionOut();
    return err;
}

enum MfllErr MfllCore::doAllocJpeg(JOB_VOID)
{
    enum MfllErr err = MfllErr_Ok;
    MfllEventStatus_t status;

    mfllFunctionIn();
    /* trigger events */
    m_event->onEvent(EventType_AllocateJpeg, status, this);

    if (m_bypass.bypassAllocJpeg) {
        usleep(20 * 1000);
        status.ignore = 1;
    }
    else {
    }

lbExit:
    status.err = err;
    m_event->doneEvent(EventType_AllocateJpeg, status, this);
    mfllFunctionOut();
    return err;
}

void MfllCore::handleError(enum MfllErr err)
{
    //TODO: handle errors....
    MFLL_UNUSED(err);
}

enum MfllErr MfllCore::createThread(pthread_t *pThread, void *(*routine)(void*), bool bDistach /* = false */)
{
    pthread_attr_t attr = PTHREAD_DEFAULT_ATTR;
    mfllFunctionIn();

    /* increase lifetime, the routine should invoke decStrong */
    incStrong(this);

    pthread_create(pThread, &attr, routine, (void*)this);
    if (bDistach) {
        pthread_detach(*pThread);
    }
    mfllFunctionOut();
    return MfllErr_Ok;
}

enum MfllErr MfllCore::joinThread(pthread_t *pThread)
{
    enum MfllErr err = MfllErr_Ok;
    void *ret;

    mfllFunctionIn();

    int r = pthread_join(*pThread, &ret);
    if (r == EINVAL) {
        mfllLogE("Join pthread %p failed, perhaps it's not a joinable thread", (void*)pThread);
        err = MfllErr_UnexpectedError;
    }
    mfllFunctionOut();
    return err;
}

void MfllCore::lockSyncObject(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_lock(&pSyncObj->trigger);
    pthread_mutex_lock(&pSyncObj->done);
}

void MfllCore::unlockSyncObject(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_unlock(&pSyncObj->trigger);
    pthread_mutex_unlock(&pSyncObj->done);
}

void MfllCore::syncWaitTrigger(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_lock(&pSyncObj->trigger);
    pthread_mutex_unlock(&pSyncObj->trigger);
}

void MfllCore::syncAnnounceTrigger(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_unlock(&pSyncObj->trigger);
}

void MfllCore::syncWaitDone(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_lock(&pSyncObj->done);
    pthread_mutex_unlock(&pSyncObj->done);
}

void MfllCore::syncAnnounceDone(MfllSyncObj_t *pSyncObj)
{
    pthread_mutex_unlock(&pSyncObj->done);
}

MfllConfig_t* MfllCore::createMfllConfig(void)
{
    MfllConfig_t *cfg = new MfllConfig_t;
    // TODO: create MFLL default MfllConfig_t
    return cfg;
}
