#ifndef __IMFLLCORE_H__
#define __IMFLLCORE_H__

#include "MfllDefs.h"
#include "MfllTypes.h"
#include "IMfllCapturer.h"
#include "IMfllEventListener.h"
#include "IMfllEvents.h"

#include <utils/RefBase.h> // android::RefBase
#include <vector>

using android::sp;
using std::vector;

namespace mfll {

class IMfllCore : public android::RefBase {
public:
    /**
     *  Create an IMfllCore instance.
     *
     *  @return             - An IMfllCore instance, caller has responsibility to manager it's lifetime.
     */
    static IMfllCore* createInstance(void);

    /**
     *  Get capture information to determine if using MFLL or not
     *
     *  @param *pCfgIn      - pointer of input Mfll configuration
     *  @param *pCfgOut     - output value for containing suggested configuration.
     *  @return             - If ok returns MfllErr_Ok.
     */
    static enum MfllErr getCaptureInfo(const struct MfllConfig *pCfgIn, struct MfllConfig *pCfgOut);

    /**
     *  Get EventType name in const char*
     *
     *  @param e            - EventType to be retrieved.
     *  @return             - Name of EventType.
     */
    static const char* getEventTypeName(enum EventType e);

    /**
     *  Check if the mode is ZSD or note.
     *
     *  @param m            - Mode to check
     *  @return             - If the mode is ZSD mode, returns true
     */
    static bool isZsdMode(const enum MfllMode &m);

public:
    /**
     *  Run MFLL with default feature option.
     *  MFLL is one shoot mechanism, it means an instance only can be
     *  shooted only one times, if you want run MFLL again, re-create an instance.
     *  @return             - If ok returns MfllErr_Ok
     *  @retval MfllErr_Shooted - This instance is out of date.
     *  @notice             - This operation is thread-safe.
     */
    virtual enum MfllErr doMfll(void) = 0;

    /**
     *  Run MFLL with specified feature option.
     *  @param featureOpts  - Feature combinations
     *  @return             - If ok returns MfllErr_Ok.
     *  @retval MfllErr_Shooted - This instance is out of date and cannot be triggered again.
    */
    virtual enum MfllErr doMfll(MfllFeatureOpt_t featureOpts) = 0;

    /**
     *  Get specified blending frame number.
     *  @return             - Frame number to blend
     */
    virtual unsigned int getBlendFrameNum(void) = 0;

    /**
     *  Get specified capture frame number.
     *  @return             - Frame number to capture
     */
    virtual unsigned int getCaptureFrameNum(void) = 0;

    /**
     *  Get instance num of Memc object.
     *
     *  @return             - Number of Memc instance.
     */
    virtual unsigned int getMemcInstanceNum(void) = 0;

    /**
     *  Get the i-th raw buffer.
     *
     *  @param index        - The index of RAW buffer to retrieve.
     *  @return             - Pointer of the specified RAW buffer. If out of range returns NULL.
     */
    virtual IMfllImageBuffer* getRawBuffer(unsigned int index) = 0;
    /**
     *  Get all raw buffers.
     *  @return             - The all RAW buffers in vector.
     *  @note               - Use vector::size(void) to retrieve size of RAW buffers.
     */
    virtual vector<IMfllImageBuffer*> getRawBuffers(void) = 0;

    /**
     *  Get sensor ID
     *
     *  @return             - Sensor ID
     *  @note               - If not inited returns -1.
     */
    virtual int getSensorId(void) = 0;

    /**
     *  Get the i-th QYuv buffer
     *
     *  @param index        - The index of QYUV buffer to retrieve.
     *  @return             - Pointer of the specified QYUV buffer. If out of range returns NULL.
     */
    virtual IMfllImageBuffer* getQYuvBuffer(unsigned int index)= 0;

    /**
     *  Get all QYUV buffers.
     *
     *  @return             - The all QYUV buffers in vector.
     *  @note               - Use vector::size(void) to retrieve size of QYUV buffers.
     */
    virtual vector<IMfllImageBuffer*> getQYuvBuffers(void) = 0;

    /**
     *  Get MFLL core reversion
     *  @return             - 8 digitals for representing aa.bbb.ccc
     *                        e.g.: v2.0.1, return value will be
     *                        0x02000001
     */
    virtual unsigned int getReversion(void) = 0;

    /**
     *  Get MFLL core reversion in std::string format
     *  @return             - A std::string object contains reversion info.
     *                        e.g.: 2.0.1
     */
    virtual std::string getReversionString(void) = 0;

    /**
     *  Retrieve if this MFLL instance shooted or notice
     *  @return             - True or false
     *  @notice             - This function is thread-safe.
     */
    virtual bool isShooted(void) = 0;

    /**
     *  MFLL provides that event listener mechanism. You can register event listener
     *  to monitor event and pre-process or post-process operation for the event.
     *  @param e            - A IMfllEventListener object with smart pointer protected
     *                        It means the lifetime of this object will be holded by
     *                        MfllCore.
     *  @return             - If ok returns MfllErr_Ok.
     *  @retval MfllErr_Shooted - The MFLL instance has shooted and cannot execute
     *                            this operation anymore.
     *  @notice             - Operations in event may block MFLL flow.
     *                      - This operation is thread-safe.
     *                      - Lifetime will be extended or managed by strong pointer.
     */
    virtual enum MfllErr registerEventListener(const sp<IMfllEventListener> &e) = 0;

    /**
     *  Remove registered event from MFLL.
     *  @param *e           - Identification of IMfllEventListener.
     *  @return             - If ok returns MfllErr_Ok
     *  @retval MfllErr_Shooted - The MFLL instance has shooted and cannot execute
     *                            this operation anymore.
     *  @notice             - removing events will travelling all registered events
     *                        and find the specified event and remove it. It means
     *                        this operation is high cost, make sure you really
     *                        want to remove event or just can ignore event.
     *                      - This operation is thread-safe.
     */
    virtual enum MfllErr removeEventListener(IMfllEventListener *e) = 0;

    /**
     *  User can set bypass option before calling doMfll.
     *  @param b            - Bypass option instance to describe the bypass operations
     *  @return             - If applied returns MfllErr_Ok.
     *                        If is doing MFLL, this function doesn't work and returns MfllErr_Shooted.
     */
    virtual enum MfllErr setBypassOption(const MfllBypassOption_t &b) = 0;

    /**
     *  Set capture resolution to MFLL
     *  @param width        - Width of capture image
     *  @param height       - Height of capture image
     *  @return             - If ok returns MfllErr_Ok
     *  @retval MfllErr_Shooted - The MFLL instance has shooted and cannot execute
     *                            this operation anymore
     *  @notice             - This operation is thread-safe.
     */
    virtual enum MfllErr setCaptureResolution(unsigned int width, unsigned int height) = 0;

    /**
     *  Set capture resolution of QSize YUV for algorithm
     *  @param qwidth       - Q size width of capture image
     *  @param qheight      - Q size height of capture image
     *  @return             - If ok returns MfllErr_Ok
     *  @retval MfllErr_Shooted - The MFLL instance has shooted and cannot execute
     *                            this operaion anymore
     *  @notice             - This operation is thread-safe.
     */
    virtual enum MfllErr setCaptureQResolution(unsigned int qwidth, unsigned int qheight) = 0;

    /**
     *  User may also set capturer to MfllCore. If capturer hasn't been set, MfllCore will
     *  invoke IMfllCapturer::createInstance to create IMfllCapturer instance when need.
     *
     *  @param capturer     - A strong pointer contains reference of IMfllCapturer instance.
     *  @return             - If ok returns MfllErr_Ok
     *  @notice             - This operation is thread-safe
     *                        Lifetime of capturer should be handled by android::sp.
     */
    virtual enum MfllErr setCapturer(const sp<IMfllCapturer> &capturer) = 0;

protected:
    virtual ~IMfllCore(void) {};

}; /* class IMfllCore */
}; /* namespace mfll */
#endif

