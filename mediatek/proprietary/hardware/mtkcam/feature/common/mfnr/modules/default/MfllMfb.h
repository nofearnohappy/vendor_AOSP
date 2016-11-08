#ifndef __MFLLMFB_H__
#define __MFLLMFB_H__

#include "IMfllMfb.h"

namespace mfll {
class MfllMfb : public IMfllMfb {
public:
    MfllMfb(void);
    virtual ~MfllMfb(void);

public:
    virtual enum MfllErr init(int sensorId);
    virtual enum MfllErr blend(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out, IMfllImageBuffer *wt_in, IMfllImageBuffer *wt_out);
    virtual enum MfllErr mix(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out);
    virtual enum MfllErr setWeightingBuffer(sp<IMfllImageBuffer> input, sp<IMfllImageBuffer> output);
    virtual enum MfllErr setSyncPrivateData(void *data, size_t size);
    virtual sp<IMfllImageBuffer> getWeightingBufferInput(void);
    virtual sp<IMfllImageBuffer> getWeightingBufferOutput(void);
    virtual enum MfllErr encodeRawToYuv(IMfllImageBuffer *input, IMfllImageBuffer *output);
};
}; /* namespace mfll */

#endif//__MFLLMFB_H__

