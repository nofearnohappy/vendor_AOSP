#ifndef __IMFLLCAPTURER_H__
#define __IMFLLCAPTURER_H__

#include "MfllDefs.h"
#include "MfllTypes.h"
#include "IMfllEvents.h"
#include "IMfllImageBuffer.h"

#include <utils/RefBase.h> // android::RefBase
#include <vector>

namespace mfll {
class IMfllCapturer : public android::RefBase {
public:
    static IMfllCapturer* createInstance(void);

    /**
     *  Capture frames
     *
     *  @param[out] raws             - A container will contain captured frames if returns MfllErr_Ok.
     *                                 Caller have to prepare IMfllImageBuffer with the same width and
     *                                 height to capture.
     *  @param[out] qyuvs            - A container will contain captured framees in QYUV format.
     *  @param[out] gmvs             - A container will contain global motion vector information.
     *  @return                      - If succeed returns MfllErr_Ok.
     *  @note                        - After invoked this method and returns Ok, the frame number will
     *                                 be updated. Invoke getCapturedFrameNum to retrieve frame number.
     *                                 All vector size is supposed to be the same but size of parameter
     *                                 "gmvs" should be (n-1).
     *                                 Notice that, capturer may invoke IMfllImageBuffer::createInstance if
     *                                 the pointer of IMfllImageBuffer is NULL. Futhermore, IMfllImageBuffer::initBuffer
     *                                 is also invoked too.
     */
    virtual enum MfllErr captureFrames(
        unsigned int captureFrameNum,
        std::vector< sp<IMfllImageBuffer> > &raws,
        std::vector< sp<IMfllImageBuffer> > &qyuvs,
        std::vector< MfllMotionVector_t > &gmvs
    ) = 0;

    /**
     *  Retrieve captured frame number.
     *
     *  @return                  - This method will be avaiable when frames are captured, or retruns
     *                             0 as initial state.
     */
    virtual unsigned int getCapturedFrameNum(void) = 0;

    /**
     *  Set capture mode to IMfllCapturer
     *
     *  @param mode             - Mode indicates to normal/ZSD MFLL/AIS.
     *  @return                 - If this function works greate, returns MfllErr_Ok.
     */
    virtual enum MfllErr setCaptureMode(const enum MfllMode &mode) = 0;

    /**
     *  MfllCapturer will dispatch events therefore the IMfllEvents is necessary to be set.
     *  @param e                 - A strong pointer of IMfllEvents reference.
     *  @return                  - If succeed returns MfllErr_Ok.
     */
    virtual enum MfllErr registerEventDispatcher(const sp<IMfllEvents> &e) = 0;

protected:
    virtual ~IMfllCapturer(void){};
};
}; /* namespace mfll */
#endif//__IMFLLCAPTURER_H__

