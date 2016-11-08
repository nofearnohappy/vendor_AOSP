#ifndef __MFLLIMAGEBUFFER_H__
#define __MFLLIMAGEBUFFER_H__

#include "IMfllImageBuffer.h"

namespace mfll {

class MfllImageBuffer : public IMfllImageBuffer {
public:
    MfllImageBuffer(void);
    virtual ~MfllImageBuffer(void);

/* inlines function */
public:
    virtual enum MfllErr setResolution(unsigned int w, unsigned int h) { return MfllErr_Ok; }
    virtual enum MfllErr setAligned(unsigned int w, unsigned int h) { return MfllErr_Ok; }
    virtual enum MfllErr setImageFormat(enum ImageFormat f) { return MfllErr_Ok; }
    virtual enum MfllErr getResolution(unsigned int &w, unsigned int &h) { return MfllErr_Ok; }
    virtual enum MfllErr getAligned(unsigned int &w, unsigned int &h) { return MfllErr_Ok; }
    virtual enum ImageFormat getImageFormat(void) { return ImageFormat_Yv16; }

public:
    virtual enum MfllErr initBuffer(void);
    virtual void* getVa(void);
    virtual size_t getRealBufferSize(void);
    virtual void* getPhysicalImageBuffer(void);
    virtual enum MfllErr releaseBuffer(void);
    virtual enum MfllErr registerEventDispatcher(const sp<IMfllEvents> &e);
    virtual enum MfllErr saveFile(const char *name);
    virtual enum MfllErr loadFile(const char *name);

}; /* MfllImageBuffer */
}; /* namespace mfll */
#endif /* __MFLLIMAGEBUFFER_H__ */
