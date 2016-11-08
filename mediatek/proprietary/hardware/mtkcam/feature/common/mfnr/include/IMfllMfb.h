#ifndef __IMFLLMFB_H__
#define __IMFLLMFB_H__

#include "MfllDefs.h"
#include "MfllTypes.h"
#include "IMfllEvents.h"
#include "IMfllImageBuffer.h"

#include <utils/RefBase.h> // android::RefBase

namespace mfll {
class IMfllMfb : public android::RefBase {
public:
    static IMfllMfb* createInstance(void);

public:
    /**
     *  Init IMfllMfb module
     *
     *  @note               - You don't need to to unint this module it will be unit in destructor.
     */
    virtual enum MfllErr init(int sensorId) = 0;

    /**
     *  Do image blending.
     *  Before invokes this method, you could prepare weighting memory for IMfllMfb, or
     *  IMfllMfb will invoke IMfllImageBuffer::createInstance to create. And IMfllImageBuffer::initBuffer
     *  is also invoked before use it.
     *
     *  @param base         - Base frame as input image.
     *  @param ref          - Reference frame as input image.
     *  @param out          - Blended image.
     *  @param wt_in        - Weighting table input, the first time to blend, set this argument as NULL
     *  @param wt_out       - Weighting table output.
     *  @note               - This function must be thread-safe.
     */
    virtual enum MfllErr blend(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out, IMfllImageBuffer *wt_in, IMfllImageBuffer *wt_out) = 0;

    /**
     *  Do image mixing.
     *  Before invokes this method, you could prepare weighting memory for IMfllMfb, or
     *  IMfllMfb will invoke IMfllImageBuffer::createInstance to create. And IMfllImageBuffer::initBuffer
     *  is also invoked before use it.
     *
     *  @param base         - Base frame as input image.
     *  @param ref          - Reference frame as input image.
     *  @param out          - Mixed frame image.
     *  @note               - This function must be thread-safe.
     */
    virtual enum MfllErr mix(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out) = 0;

    /**
     *  Caller could prepare weighting buffer for blending. Invokes this function for preparing buffers.
     *
     *  @param input        - Two buffers for weighting table with image format ImageFormat_Y
     *  @note               - The dimension of these buffers are supposed to be the same as input base frame.
     */
    virtual enum MfllErr setWeightingBuffer(sp<IMfllImageBuffer> input, sp<IMfllImageBuffer> output) = 0;

    /**
     *  Set sync data for pass 1 and pass 2. This data should be get by IspSyncControl
     *
     *  @param *data        - Private data
     *  @param size         - Data size
     *  @return             - Returns MfllErr_Ok if ok.
     */
    virtual enum MfllErr setSyncPrivateData(void *data, size_t size) = 0;

    /**
     *  Retrieve input weighting buffer reference.
     *
     *  @return             - A strong pointer of weighting buffer reference.
     */
    virtual sp<IMfllImageBuffer> getWeightingBufferInput(void) = 0;

    /**
     *  Retrieve output weighting buffer reference.
     *
     *  @return             - A strong pointer of weighting buffer reference.
     */
    virtual sp<IMfllImageBuffer> getWeightingBufferOutput(void) = 0;

    /**
     *  Encode RAW10 image to YUV serial image using pass 2 driver, like YUV2, YV16
     *
     *  @param input        - Input buffer
     *  @param output       - Output buffer
     *  @return             - Returns MfllErr_Ok if ok.
     *  @note               - The size of image must be the same
     */
    virtual enum MfllErr encodeRawToYuv(IMfllImageBuffer *input, IMfllImageBuffer *output) = 0;

protected:
    virtual ~IMfllMfb(void){};
};
}; /* namespace mfll */

#endif//__IMFLLMFB_H__

