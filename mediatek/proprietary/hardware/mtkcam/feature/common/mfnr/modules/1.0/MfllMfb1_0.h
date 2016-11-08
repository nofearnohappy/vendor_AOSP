#ifndef __MFLLMFB1_0_H__
#define __MFLLMFB1_0_H__

#include "IMfllMfb.h"
#include "IMfllImageBuffer.h"

#include <pthread.h>

using android::sp;

namespace mfll {
class MfllMfb1_0 : public IMfllMfb {
public:
    MfllMfb1_0(void);
    virtual ~MfllMfb1_0(void);

/* implementations */
public:
    virtual enum MfllErr init(int sensorId);
    virtual enum MfllErr blend(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out, IMfllImageBuffer *wt_in, IMfllImageBuffer *wt_out);
    virtual enum MfllErr mix(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out);
    virtual enum MfllErr setWeightingBuffer(sp<IMfllImageBuffer> input, sp<IMfllImageBuffer> output);
    virtual sp<IMfllImageBuffer> getWeightingBufferInput(void);
    virtual sp<IMfllImageBuffer> getWeightingBufferOutput(void);
    virtual enum MfllErr setSyncPrivateData(void *data, size_t size);
    virtual enum MfllErr encodeRawToYuv(IMfllImageBuffer *input, IMfllImageBuffer *output);

/* attributes */
private:
    /* thread-safe protector */
    pthread_mutex_t m_mutex;
    inline void lock(void) { pthread_mutex_lock(&m_mutex); }
    inline void unlock(void) { pthread_mutex_unlock(&m_mutex); }
private:
    int m_sensorId;
    enum MfllMode m_shotMode;
    enum NoiseReductionType m_nrType;
    sp<IMfllImageBuffer> m_spWeightingInput;
    sp<IMfllImageBuffer> m_spWeightingOutput;
    void *m_syncPrivateData;
    size_t m_syncPrivateDataSize;
};
};/* namespace mfll */
#endif//__MFLLMFB1_0_H__

