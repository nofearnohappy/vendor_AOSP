#include "MfllMfb1_0.h"
#include "IMfllCore.h"
#include "MfllLog.h"

#include <mtkcam/iopipe/PostProc/IHalPostProcPipe.h>
#include <mtkcam/iopipe/PostProc/INormalStream.h>
#include <mtkcam/iopipe/PostProc/IFeatureStream.h>
#include <mtkcam/iopipe/PostProc/IPortEnum.h>
#include <mtkcam/featureio/IHal3A.h>
#include <mtkcam/hwutils/HwMisc.h>


using namespace mfll;
using namespace NSCam;
using namespace NSCam::Utils::Format;
using namespace NSCam::NSIoPipe::NSPostProc;
using namespace NS3A;

using android::sp;
using NSCam::IImageBuffer;

/* make sure pass 2 is thread-safe ... */
static pthread_mutex_t gMutexPass2Lock = PTHREAD_MUTEX_INITIALIZER;
static void pass2_lock(void)
{
    pthread_mutex_lock(&gMutexPass2Lock);
}

static void pass2_unlock(void)
{
    pthread_mutex_unlock(&gMutexPass2Lock);
}

/* a helper function to set 3A profile */
static MBOOL set_isp_profile(const EIspProfile_T &profile, int sensorId)
{
    MBOOL bRet = MTRUE;
    IHal3A *p3A = NULL;
    p3A = IHal3A::createInstance(IHal3A::E_Camera_1, sensorId, MFLL_LOG_KEYWORD);
    if (p3A == NULL) {
        mfllLogE("%s: create IHal3A instance failed", __FUNCTION__);
        return MFALSE;
    }
    mfllLogD("%s: create IHal3A instance ok", __FUNCTION__);

    ParamIspProfile_T _3A_profile(profile, 0 /* magic num */, MFALSE, ParamIspProfile_T::EParamValidate_P2Only);
    p3A->setIspProfile(_3A_profile);
    p3A->destroyInstance(MFLL_LOG_KEYWORD);
    return MTRUE;
}

IMfllMfb* IMfllMfb::createInstance(void)
{
    return (IMfllMfb*)new MfllMfb1_0();
}

MfllMfb1_0::MfllMfb1_0(void)
{
    m_mutex = PTHREAD_MUTEX_INITIALIZER;
    m_spWeightingInput = NULL;
    m_spWeightingOutput = NULL;
    m_syncPrivateData = NULL;
    m_syncPrivateDataSize = 0;
}

MfllMfb1_0::~MfllMfb1_0(void)
{
}

enum MfllErr MfllMfb1_0::init(int sensorId)
{
    m_sensorId = sensorId;
    return MfllErr_Ok;
}

enum MfllErr MfllMfb1_0::blend(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out, IMfllImageBuffer *wt_in, IMfllImageBuffer *wt_out)
{
    enum MfllErr err = MfllErr_Ok;
    MBOOL bRet = MTRUE;

    /* get member resource here */
    lock();
    enum NoiseReductionType nrType = m_nrType;
    EIspProfile_T profile = EIspProfile_MFB_PostProc_EE_Off;
    int sensorId = m_sensorId;
    void *privateData = m_syncPrivateData;
    size_t privateDataSize = m_syncPrivateDataSize;
    unlock();


    IImageBuffer *ptrBase = (IImageBuffer*)base->getPhysicalImageBuffer();
    IImageBuffer *ptrRef = (IImageBuffer*)ref->getPhysicalImageBuffer();
    IImageBuffer *ptrOut = (IImageBuffer*)out->getPhysicalImageBuffer();
    IImageBuffer *ptrWeightingIn = NULL;
    if (wt_in)
        ptrWeightingIn = (IImageBuffer*)wt_in->getPhysicalImageBuffer();

    IImageBuffer *ptrWeightingOut = (IImageBuffer*)wt_out->getPhysicalImageBuffer();

    MSize resolution = ptrBase->getImgSize();

    /* input ports */
    NSCam::NSIoPipe::NSPostProc::Input minput_imgi;     //base frame
    NSCam::NSIoPipe::NSPostProc::Input minput_vipi;     //reference frame
    NSCam::NSIoPipe::NSPostProc::Input minput_vip3i;    //weighting map

    /* output ports */
    NSCam::NSIoPipe::NSPostProc::Output moutput_mfbo;   //weighting map
    NSCam::NSIoPipe::NSPostProc::Output moutput_img3o;  //output frame

    IFeatureStream* pIMfbBlending = NULL;
    IHal3A *p3A = NULL;

    QParams params;
    QParams dequeParams;
    const MINT64 timeout = 5000000000;  //5s,unit is nsec

    pIMfbBlending = IFeatureStream::createInstance(
        "MFB_Blending",
        (IMfllCore::isZsdMode(m_shotMode) ? EFeatureStreamTag_MFB_Bld_Vss : EFeatureStreamTag_MFB_Bld),
        sensorId
    );

    if (pIMfbBlending == NULL) {
        mfllLogE("%s: create IFeatureStream instance failed", __FUNCTION__);
        goto lbExit;
    }

    bRet = pIMfbBlending->init();
    if (bRet != MTRUE) {
        mfllLogE("%s: init IFeatureStream instance fail", __FUNCTION__);
        goto lbExit;
    }

    /**
     *  P A S S 2
     *
     *  Configure input parameters
     */
    params.mvIn.clear();

    // - imgi = yuv base frame
    minput_imgi.mBuffer = ptrBase;
    minput_imgi.mPortID.inout = 0; //in
    minput_imgi.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_IMGI;
    minput_imgi.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    minput_imgi.mCropRect.p_integral.x = 0;
    minput_imgi.mCropRect.p_integral.y = 0;
    minput_imgi.mCropRect.p_fractional.x = 0;
    minput_imgi.mCropRect.p_fractional.y = 0;
    minput_imgi.mCropRect.s.w = resolution.w;
    minput_imgi.mCropRect.s.h = resolution.h;
    params.mvIn.push_back(minput_imgi);
    mfllLogD("%s: minput_imgi.mBuffer = %p", __FUNCTION__, minput_imgi.mBuffer);

    // - vipi = reference frame
    minput_vipi.mBuffer = ptrRef;
    minput_vipi.mPortID.inout = 0; //in
    minput_vipi.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_VIPI;
    minput_vipi.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    minput_vipi.mCropRect.p_integral.x = 0;
    minput_vipi.mCropRect.p_integral.y = 0;
    minput_vipi.mCropRect.p_fractional.x = 0;
    minput_vipi.mCropRect.p_fractional.y = 0;
    minput_vipi.mCropRect.s.w = ptrRef->getImgSize().w;
    minput_vipi.mCropRect.s.h = ptrRef->getImgSize().h;
    params.mvIn.push_back(minput_vipi);
    mfllLogD("%s: minput_vipi.mBuffer = %p", __FUNCTION__, minput_vipi.mBuffer);

    /* for not the first blending, we need to give weighting map */
    if(ptrWeightingIn) {
        minput_vip3i.mBuffer = ptrWeightingIn;
        minput_vip3i.mPortID.inout = 0; //in
        minput_vip3i.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_VIP3I;
        minput_vip3i.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
        minput_vip3i.mCropRect.p_integral.x = 0;
        minput_vip3i.mCropRect.p_integral.y = 0;
        minput_vip3i.mCropRect.p_fractional.x = 0;
        minput_vip3i.mCropRect.p_fractional.y = 0;
        minput_vip3i.mCropRect.s.w = ptrWeightingIn->getImgSize().w;
        minput_vip3i.mCropRect.s.h = ptrWeightingIn->getImgSize().h;
        params.mvIn.push_back(minput_vip3i);
        mfllLogD("minput_vip3i.mBuffer = %p", minput_vip3i.mBuffer);
    }

    /**
     *  P A S S 2
     *
     *  Configure output parameters
     */
    params.mvOut.clear();

    // - mfbo = weighting map
    moutput_mfbo.mBuffer = ptrWeightingOut;
    moutput_mfbo.mPortID.inout = 1; //out
    moutput_mfbo.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_MFBO;
    moutput_mfbo.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    params.mvOut.push_back(moutput_mfbo);
    mfllLogD("moutput_mfbo.mBuffer = %p", moutput_mfbo.mBuffer);

    // - img3o = blending output
    moutput_img3o.mBuffer = ptrOut;
    moutput_img3o.mPortID.inout = 1; //out
    moutput_img3o.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_IMG3O;
    moutput_img3o.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    params.mvOut.push_back(moutput_img3o);
    mfllLogD("moutput_img3o.mBuffer = %p", moutput_img3o.mBuffer);

    /* determine ISP profile */
    if (IMfllCore::isZsdMode(m_shotMode)) {
        profile = (nrType == NoiseReductionType_SWNR ? EIspProfile_VSS_MFB_Blending_All_Off_SWNR :  EIspProfile_VSS_MFB_Blending_All_Off);
    }
    else {
        profile = (nrType == NoiseReductionType_SWNR ? EIspProfile_MFB_Blending_All_Off_SWNR :  EIspProfile_MFB_Blending_All_Off);
    }

    pass2_lock();

    bRet = set_isp_profile(profile, sensorId);
    if (bRet != MTRUE) {
        mfllLogE("%s: set_isp_profile returns fail", __FUNCTION__);
        goto lbExit_unlcok;
    }

    // enqueue
    //params.mFrameNo = ref;
    params.mFrameNo = 0;
    pIMfbBlending->enque(params);

    // dequeue
    bRet = pIMfbBlending->deque(dequeParams, timeout);
    if (bRet != MTRUE) {
        mfllLogE("%s: pass 2 deque failed", __FUNCTION__);
        goto lbExit_unlcok;
    }

lbExit_unlcok:
    pass2_unlock();

lbExit:
    return err;
}

enum MfllErr MfllMfb1_0::mix(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out)
{
    return MfllErr_NotImplemented;
}

enum MfllErr MfllMfb1_0::setWeightingBuffer(sp<IMfllImageBuffer> input, sp<IMfllImageBuffer> output)
{
    lock();
    m_spWeightingInput = input;
    m_spWeightingOutput = output;
    unlock();
    return MfllErr_Ok;
}

sp<IMfllImageBuffer> MfllMfb1_0::getWeightingBufferInput(void)
{
    return m_spWeightingInput;
}

sp<IMfllImageBuffer> MfllMfb1_0::getWeightingBufferOutput(void)
{
    return m_spWeightingOutput;
}

enum MfllErr MfllMfb1_0::setSyncPrivateData(void *data, size_t size)
{
    lock();
    m_syncPrivateData = data;
    m_syncPrivateDataSize = size;
    unlock();
    return MfllErr_Ok;
}

enum MfllErr MfllMfb1_0::encodeRawToYuv(IMfllImageBuffer *input, IMfllImageBuffer *output)
{
    enum MfllErr err = MfllErr_Ok;
    MBOOL bRet = MTRUE;
    EIspProfile_T profile = EIspProfile_MFB_PostProc_EE_Off;

    lock();
    int sensorId = m_sensorId;
    void *privateData = m_syncPrivateData;
    size_t privateDataSize = m_syncPrivateDataSize;
    IImageBuffer *pInput = (IImageBuffer*)input->getPhysicalImageBuffer();
    IImageBuffer *pOutput = (IImageBuffer*)output->getPhysicalImageBuffer();
    unlock();

    NSCam::NSIoPipe::NSPostProc::Input  minput;
    NSCam::NSIoPipe::NSPostProc::Output moutput;
    QParams params;
    QParams dequeParams;

    INormalStream *pNormalStream = NULL;
    IHal3A *p3A = NULL;

    MSize srcSize = pInput->getImgSize();
    MSize dstSize = pOutput->getImgSize();
    NSCamHW::Rect srcRect(0, 0, srcSize.w, srcSize.h);
    NSCamHW::Rect dstRect(0, 0, dstSize.w, dstSize.h);
    NSCamHW::Rect cropRect = MtkCamUtils::calCrop(srcRect, dstRect, 100);
    mfllLogD("srcRect xywh(%d,%d,%d,%d)", srcRect.x, srcRect.y, srcRect.w, srcRect.h);
    mfllLogD("dstRect xywh(%d,%d,%d,%d)", dstRect.x, dstRect.y, dstRect.w, dstRect.h);
    mfllLogD("cropRect xywh(%d,%d,%d,%d)", cropRect.x, cropRect.y, cropRect.w, cropRect.h);

    pNormalStream = NSCam::NSIoPipe::NSPostProc::INormalStream::createInstance(
        "mfll_p2iopipe",
        IMfllCore::isZsdMode(m_shotMode) ? ENormalStreamTag_Vss : ENormalStreamTag_Cap,
        sensorId
    );
    if (pNormalStream == NULL) {
        mfllLogE("%s: create INoramlStream instance fail", __FUNCTION__);
        goto lbExit;
    }

    pass2_lock();

    bRet = set_isp_profile(profile, m_sensorId);
    if (bRet != MTRUE) {
        mfllLogE("%s: set_isp_profile returns fail", __FUNCTION__);
        goto lbExit_unlcok;
    }

    bRet = pNormalStream->init();
    if (bRet != MTRUE) {
        mfllLogE("%s: init INormalStream instance fail", __FUNCTION__);
        goto lbExit_unlcok;
    }

    //crop
    {
        MCrpRsInfo crop1;
        crop1.mGroupID    = 1;
        crop1.mCropRect.s = srcSize;
        crop1.mResizeDst  = srcSize;
        //
        MCrpRsInfo crop2;
        crop2.mGroupID    = 2;
        crop2.mCropRect.p_integral.x = cropRect.x; //0
        crop2.mCropRect.p_integral.y = cropRect.y; //0
        crop2.mCropRect.p_fractional.x = 0;
        crop2.mCropRect.p_fractional.y = 0;
        crop2.mCropRect.s.w = cropRect.w;  //mRaw_Width
        crop2.mCropRect.s.h = cropRect.h;  //mRaw_Height
        //crop2.mResizeDst = MSize(0,0);
        params.mvCropRsInfo.push_back(crop1);
        params.mvCropRsInfo.push_back(crop2);
    }

    params.mvIn.clear();
    minput.mBuffer = pInput;
    minput.mPortID.inout = 0;
    minput.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    params.mvIn.push_back(minput);

    params.mvOut.clear();
    moutput.mBuffer = pOutput;
    moutput.mPortID.inout = 1;
    moutput.mPortID.index = NSCam::NSIoPipe::NSPostProc::EPipePortIndex_WDMAO;
    moutput.mPortID.type = NSCam::NSIoPipe::EPortType_Memory;
    params.mvOut.push_back(moutput);

    // set private data to pass2
    params.mpPrivaData = privateData;

    params.mFrameNo = 0;
    pNormalStream->enque(params);

    dequeParams.mFrameNo = 0;
    bRet = pNormalStream->deque(dequeParams, 5000000000);

    if (bRet != MTRUE) {
        mfllLogE("%s: INormalStream::deque failed", __FUNCTION__);
    }
    else {
        mfllLogD("%s: INormalStream deque ok", __FUNCTION__);
    }

    pNormalStream->uninit();
    pNormalStream->destroyInstance(MFLL_LOG_KEYWORD);
    pNormalStream = NULL;

lbExit_unlcok:
    pass2_unlock();

lbExit:
    return err;
}

