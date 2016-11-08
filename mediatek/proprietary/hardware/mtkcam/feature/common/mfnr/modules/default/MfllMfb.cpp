#include "MfllMfb.h"

using namespace mfll;
using android::sp;

IMfllMfb* IMfllMfb::createInstance(void)
{
    return (IMfllMfb*)new MfllMfb();
}

MfllMfb::MfllMfb(void)
{
}

MfllMfb::~MfllMfb(void)
{
}

enum MfllErr MfllMfb::init(int sensorId)
{
    return MfllErr_Ok;
}

enum MfllErr MfllMfb::blend(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out, IMfllImageBuffer *wt_in, IMfllImageBuffer *wt_out)
{
    return MfllErr_Ok;
}

enum MfllErr MfllMfb::mix(IMfllImageBuffer *base, IMfllImageBuffer *ref, IMfllImageBuffer *out)
{
    return MfllErr_Ok;
}

enum MfllErr MfllMfb::setWeightingBuffer(sp<IMfllImageBuffer> input, sp<IMfllImageBuffer> output)
{
    return MfllErr_Ok;
}

enum MfllErr MfllMfb::setSyncPrivateData(void *data, size_t size)
{
    return MfllErr_Ok;
}

sp<IMfllImageBuffer> MfllMfb::getWeightingBufferInput(void)
{
    return sp<IMfllImageBuffer>(NULL);
}

sp<IMfllImageBuffer> MfllMfb::getWeightingBufferOutput(void)
{
    return sp<IMfllImageBuffer>(NULL);
}

enum MfllErr MfllMfb::encodeRawToYuv(IMfllImageBuffer *input, IMfllImageBuffer *output)
{
    return MfllErr_Ok;
}
