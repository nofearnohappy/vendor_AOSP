#ifndef __MFLLCAPTURER_H__
#define __MFLLCAPTURER_H__

#include "IMfllCapturer.h"


using android::sp;

namespace mfll {
class MfllCapturer : public IMfllCapturer {
public:
    MfllCapturer(void);
    virtual ~MfllCapturer(void);

/* Implementations */
public:
    virtual enum MfllErr captureFrames(
        unsigned int captureFrameNum,
        std::vector< sp<IMfllImageBuffer> > &raws,
        std::vector< sp<IMfllImageBuffer> > &qyuvs,
        std::vector< MfllMotionVector_t > &gmvs
    );
    virtual unsigned int getCapturedFrameNum(void);
    virtual enum MfllErr setCaptureMode(const enum MfllMode &mode);
    virtual enum MfllErr registerEventDispatcher(const sp<IMfllEvents> &e);

private:
    unsigned int m_frameNum;
    enum MfllMode m_captureMode; // indicates to capture mode.
    sp<IMfllEvents> m_spEventDispatcher;

}; /* class mfll */
}; /* namespace mfll */
#endif /* __MFLLCAPTURER_H__ */
