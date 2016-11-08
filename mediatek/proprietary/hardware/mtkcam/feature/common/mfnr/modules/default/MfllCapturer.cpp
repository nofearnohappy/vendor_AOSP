#include "MfllCapturer.h"
#include "MfllLog.h"

using namespace mfll;

IMfllCapturer* IMfllCapturer::createInstance(void)
{
    return (IMfllCapturer*)new MfllCapturer;
}

MfllCapturer::MfllCapturer(void)
{
    m_spEventDispatcher = NULL;
    m_frameNum = 0;
    m_captureMode = (enum MfllMode)0;
}

MfllCapturer::~MfllCapturer(void)
{
}

enum MfllErr MfllCapturer::captureFrames(
    unsigned int captureFrameNum,
    std::vector< sp<IMfllImageBuffer> > &raws,
    std::vector< sp<IMfllImageBuffer> > &qyuvs,
    std::vector<MfllMotionVector_t> &gmvs)
{
    MFLL_UNUSED(captureFrameNum);
    MFLL_UNUSED(raws);
    MFLL_UNUSED(qyuvs);
    MFLL_UNUSED(gmvs);

    return MfllErr_NotImplemented;
}

unsigned int MfllCapturer::getCapturedFrameNum(void)
{
    return m_frameNum;
}

enum MfllErr MfllCapturer::setCaptureMode(const enum MfllMode &mode)
{
    m_captureMode = mode;
    return MfllErr_Ok;
}

enum MfllErr MfllCapturer::registerEventDispatcher(const sp<IMfllEvents> &e)
{
    m_spEventDispatcher = e;
    return MfllErr_Ok;
}

