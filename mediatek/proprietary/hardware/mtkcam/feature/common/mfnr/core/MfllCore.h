#ifndef __MFLLCORE_H__
#define __MFLLCORE_H__

/**
 *  Reversion Infomation
 */
#define MFLL_CORE_REVERSION_MAJOR       MFLL_CORE_VERSION_MAJOR
#define MFLL_CORE_REVERSION_MINOR       MFLL_CORE_VERSION_MINOR
#define MFLL_CORE_REVERSION_BUGFIX      0

#include "MfllDefs.h"
#include "MfllTypes.h"

/* interfaces */
#include "IMfllCore.h"
#include "IMfllImageBuffer.h"
#include "IMfllCapturer.h"
#include "IMfllEventListener.h"
#include "IMfllEvents.h"
#include "IMfllMfb.h"

/* standard libs */
#include <semaphore.h>
#include <pthread.h> // pthread_t, pthread_mutex_t
#include <vector>
#include <string>
#include <utils/RefBase.h> // android::RefBase

using android::sp; // use android::sp as sp
using std::vector; // use std::vector as vector

namespace mfll {

class MfllCore : public IMfllCore {
public:
    MfllCore(void);
    virtual ~MfllCore(void);

/* related modules */
public:
    /* Event dispatcher */
    sp<IMfllEvents> m_event;

public:
    /* module to provide captured frames */
    sp<IMfllCapturer>   m_spCapturer;
    sp<IMfllMfb>        m_spMfb;
#if 0 // not implement now...
    sp<MfllBlender>     *m_pBlender;   // Module to blend frames
    sp<MfllMixer>       *m_pMixer;     // Module to mix frames
    sp<MfllMemc>        *m_pMemc;      // Module to do ME/MC
    sp<MfllBss>         *m_pBss;       // Module to do Best Shot Selection(BSS)
#endif

/* attributes */
protected:
    bool            m_bShooted;
    pthread_mutex_t m_mutexShoot; // protect shoot, makes MfllCore thread-safe
    MfllConfig_t    m_cfg;
    int             m_sensorId; // sensor Id
    unsigned int    m_frameNum; // blending frame amount
    unsigned int    m_frameNumCaptured; // captured frame number
    unsigned int    m_memcInstanceNum; // represents how many MfllMemc to be create
    enum MfllMode   m_shotMode; // shotMode
    enum RwbMode    m_eRwbMode;
    enum MrpMode    m_eMrpMode;
    unsigned int    m_width; // full size
    unsigned int    m_height; // full size
    unsigned int    m_qwidth; // for 1/4 size YUV
    unsigned int    m_qheight; // for 1//4 size YUV
    unsigned int    m_outputWidth; // for output JPEG & YUV
    unsigned int    m_outputHeight;  // for output JPEG & YUV
    unsigned int    m_widthPostview; // for postview width
    unsigned int    m_heightPostview; // for postview height
    unsigned int    m_widthThumbnail; // for thumbnail width
    unsigned int    m_heightThumbnail; // for thumbnail width

    /* sync private data, which is provided by IspSyncControl */
    void            *m_privateData;
    size_t          m_privateDataSize;

    /* Global motion vector retrieved by IMfllCapturer */
    MfllMotionVector_t  m_globalMv[MFLL_MAX_FRAMES];

/* public attributes */
public:
    /*  Bypass option can let you bypass any operation but conditions are still necessary */
    MfllBypassOption_t  m_bypass; // MfllBypassOption_t

    /* used image buffers */
    /* phiscal buffer, costs memory */
    sp<IMfllImageBuffer> m_imgRaws[MFLL_MAX_FRAMES];
    sp<IMfllImageBuffer> m_imgQYuvs[MFLL_MAX_FRAMES]; // may save 1/4 size yuv
    sp<IMfllImageBuffer> m_imgYuvBase;
    sp<IMfllImageBuffer> m_imgYuvGolden;
    sp<IMfllImageBuffer> m_imgYuvBlended;
    sp<IMfllImageBuffer> m_imgWeighting[2]; // ping-pong buffers, takes 2
    sp<IMfllImageBuffer> m_imgMemc[MFLL_MAX_FRAMES]; // working buffer for MEME

    /**
     *  During blending, MFLL will use two buffers as ping-pong buffers, e.g.:
     *  1st: imgBase    + imgRef = imgBlended    (ping)
     *  2nd: imgBlended + imgRef = imgBase       (pong)
     *  3rd: imgBase    + imgRef = imgBlended    (ping)
     *  ...
     *
     *  Hence here we use these pointers to indicate the REAL meanful buffers.
     */
    IMfllImageBuffer *m_ptrImgYuvBase; // indicates to full size base YUV
    IMfllImageBuffer *m_ptrImgYuvRef; // indicates to any size reference YUV
    IMfllImageBuffer *m_ptrImgYuvGolden; // indicates to golden YUV (noraml YUV)
    IMfllImageBuffer *m_ptrImgYuvBlended; // indicates to blended YUV (same size with YUV base)
    IMfllImageBuffer *m_ptrImgYuvMixed; // indicates to mixed YUV
    IMfllImageBuffer *m_ptrImgWeightingIn; // indicates to input of weighting table during blending
    IMfllImageBuffer *m_ptrImgWeightingOut; // indicates to output of weighting table during blending
    IMfllImageBuffer *m_ptrImgWeightingFinal; // indicates to final weighting for mixing

/**
 *  Synchronization objects
 *
 *  We're using pthread_mutex_t for thread synchronization, here we defined
 *  that the default mutexes state to locked for these operations (blending, mixing or ME/MC).
 *  All the operations have different condition which means
 *  to invoke an operaion, the operation will be executed after conditions matched.
 *  For example: doEncodeYuvBase, which means to encode base RAW frame to YUV frame.
 *  The conditions of this operation will be the base frame has been decided.
 *
 *  So we will invoke just like:
 *
 *  // wait RAW buffers buffers are ready to use
 *  syncWaitDone(&m_syncBss);
 *
 *  to wait conditions matched.
 */
public:
    /* operation sync objects */
    MfllSyncObj_t m_syncCapture; // annonced done while capture is done
    MfllSyncObj_t m_syncCapturedRaw[MFLL_MAX_FRAMES]; // annonced done while a single RAW is captured
    MfllSyncObj_t m_syncCapturedYuvQ[MFLL_MAX_FRAMES]; // annonced done while a single QYUV is captured
    MfllSyncObj_t m_syncEncodeYuvBase;
    MfllSyncObj_t m_syncEncodeYuvGolden;
    MfllSyncObj_t m_syncBss;
    MfllSyncObj_t m_syncMotionEstimation[MFLL_MAX_FRAMES];
    MfllSyncObj_t m_syncMotionCompensation[MFLL_MAX_FRAMES];
    MfllSyncObj_t m_syncBlending[MFLL_MAX_FRAMES];
    MfllSyncObj_t m_syncMixing;
    MfllSyncObj_t m_syncNoiseReduction;
    MfllSyncObj_t m_syncPostview;
    MfllSyncObj_t m_syncThumbnail;
    MfllSyncObj_t m_syncEncJpeg;

/* implementations */
public:
    virtual enum MfllErr init(const MfllConfig_t *pCfg = NULL);
    virtual enum MfllErr doMfll(void);
    virtual enum MfllErr doMfll(MfllFeatureOpt_t featureOpts);
    virtual unsigned int getBlendFrameNum(void) { return m_frameNum; }
    virtual unsigned int getCaptureFrameNum(void) { return m_frameNumCaptured; }
    virtual unsigned int getMemcInstanceNum(void) { return m_memcInstanceNum; }
    virtual IMfllImageBuffer* getRawBuffer(unsigned int index);
    virtual vector<IMfllImageBuffer*> getRawBuffers(void);
    virtual int getSensorId(void);
    virtual IMfllImageBuffer* getQYuvBuffer(unsigned int index);
    virtual vector<IMfllImageBuffer*> getQYuvBuffers(void);
    virtual unsigned int getReversion(void);
    virtual std::string getReversionString(void);
    virtual bool isShooted(void);
    virtual enum MfllErr registerEventListener(const sp<IMfllEventListener> &e);
    virtual enum MfllErr registerEventListenerNoLock(const sp<IMfllEventListener> &e);
    virtual enum MfllErr removeEventListener(IMfllEventListener *e);
    virtual enum MfllErr setBypassOption(const MfllBypassOption_t &b);
    virtual enum MfllErr setCaptureResolution(unsigned int width, unsigned int height);
    virtual enum MfllErr setCaptureQResolution(unsigned int qwidth, unsigned int qheight);
    virtual enum MfllErr setCapturer(const sp<IMfllCapturer> &capturer);

public:
    static enum MfllErr getCaptureInfo(const struct MfllConfig *pCfgIn, struct MfllConfig *pCfgOut);

/* MFLL operations, user should not invoke these functions directly */
public:

    /**
     *  We make all the jobs with the same prototype: void*. Because we want to make some job threads
     *  with job queue to execute these jobs.
     */
    #define JOB_VOID void *__arg
    /* doCapture will output N full size RAW and N 1/4 Size YUV */
    enum MfllErr doCapture(JOB_VOID);
    /* Wait until all captured buffers are ready, using BSS to pick the best base frame */
    enum MfllErr doBss(JOB_VOID);
    /* After the base frame has been selected, we can encode it to YUV */
    enum MfllErr doEncodeYuvBase(JOB_VOID);
    enum MfllErr doEncodeYuvGolden(JOB_VOID);
    enum MfllErr doMotionEstimation(void *void_index);
    enum MfllErr doMotionCompensation(void *void_index);
    enum MfllErr doBlending(void *void_index);
    enum MfllErr doMixing(JOB_VOID);
    enum MfllErr doNoiseReduction(JOB_VOID);
    enum MfllErr doCreatePostview(JOB_VOID);
    enum MfllErr doEncodeThumbnail(JOB_VOID);
    enum MfllErr doEncodeJpeg(JOB_VOID);

/* Buffer allocations */
public:
    enum MfllErr doAllocRawBuffer(void *void_index);
    enum MfllErr doAllocQyuvBuffer(void *void_index);
    enum MfllErr doAllocYuvBase(JOB_VOID);
    enum MfllErr doAllocYuvGolden(JOB_VOID);
    enum MfllErr doAllocYuvWorking(JOB_VOID);
    enum MfllErr doAllocWeighting(void *void_index);
    enum MfllErr doAllocMemcWorking(void *void_index);
    enum MfllErr doAllocPostview(JOB_VOID);
    enum MfllErr doAllocThumbnail(JOB_VOID);
    enum MfllErr doAllocJpeg(JOB_VOID);

protected:
    /* Error handling, makes sure MfllShot won't makes deadlock or something else ... */
    void handleError(enum MfllErr err);

public:
    /**
     *  To create a thread within MfllCore as the argument, and also increase "this"(MfllCore*) lifetime,
     *  caller must to invoke MfllCore::decStrong(void*) to decrease lifetime counter when the thread
     *  goes to the end.
     */
    enum MfllErr createThread(pthread_t *pThread, void *(*routine)(void*), bool bDistach = false);
    enum MfllErr joinThread(pthread_t *pThread);

/* MFLL sync object operations */
public:
    static void lockSyncObject(MfllSyncObj_t *pSyncObj);
    static void unlockSyncObject(MfllSyncObj_t *pSyncObj);
    static void syncWaitTrigger(MfllSyncObj_t *pSyncObj);
    static void syncAnnounceTrigger(MfllSyncObj_t *pSyncObj);
    static void syncWaitDone(MfllSyncObj_t *pSyncObj);
    static void syncAnnounceDone(MfllSyncObj_t *pSyncObj);

/* static methods */
public:
    /**
     *  To create a MFLL default configuration
     *  @return             - A dynamically created struct MfllConfig object.
     *  @notice             - Caller has responsibility to release handle.
     */
    static MfllConfig_t* createMfllConfig(void);

}; /* class MfllCore */
}; /* namespace mfll */

#endif /* __MFLLCORE_H__ */
