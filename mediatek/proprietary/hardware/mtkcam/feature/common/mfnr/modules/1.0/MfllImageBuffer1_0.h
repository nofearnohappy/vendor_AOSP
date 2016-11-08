#ifndef __MFLLIMAGEBUFFER1_0_H__
#define __MFLLIMAGEBUFFER1_0_H__

#include "IMfllImageBuffer.h"


/* middleware related headers */
#include "IImageBuffer.h"

#include <pthread.h>

using NSCam::IImageBuffer;
using android::sp;

namespace mfll {

class MfllImageBuffer1_0 : public IMfllImageBuffer {
public:
    MfllImageBuffer1_0(void);
    virtual ~MfllImageBuffer1_0(void);

/* Image information */
private:
    unsigned int m_width;
    unsigned int m_height;
    unsigned int m_alignedWidth;
    unsigned int m_alignedHeight;
    unsigned int m_bufferSize;
    enum ImageFormat m_format;
    sp<IMfllEvents> m_spEventDispatcher;

private:
    sp<IImageBuffer> m_imgBuffer;

/* thread-safe mutex */
private:
    pthread_mutex_t m_locker;
    inline void lock(void)      { pthread_mutex_lock(&m_locker); }
    inline void unlock(void)    { pthread_mutex_unlock(&m_locker); }

/* inlines function */
public:
    virtual enum MfllErr setResolution(unsigned int w, unsigned int h);
    virtual enum MfllErr setAligned(unsigned int aligned_w, unsigned int aligned_h);
    virtual enum MfllErr setImageFormat(enum ImageFormat f);
    virtual enum MfllErr getResolution(unsigned int &w, unsigned int &h);
    virtual enum MfllErr getAligned(unsigned int &w, unsigned int &h);
    virtual enum ImageFormat getImageFormat(void);

/* implementations */
public:
    virtual enum MfllErr initBuffer(void);
    virtual void* getVa(void);
    virtual size_t getRealBufferSize(void);
    virtual void* getPhysicalImageBuffer(void);
    virtual enum MfllErr releaseBuffer(void);
    virtual enum MfllErr registerEventDispatcher(const sp<IMfllEvents> &e);
    virtual enum MfllErr saveFile(const char *name);
    virtual enum MfllErr loadFile(const char *name);

public:
    /* this function always create a continous buffer */
    static enum MfllErr createImageBuffer(sp<IImageBuffer>& imgBuf, unsigned int& imageSize, int width, int height, int align_width, int align_height, enum ImageFormat fmt);
    /* mapping mfll::ImageFormat to NSCam::Utils::Format */
    static MUINT32 convertImageFormat(const enum ImageFormat &fmt);

}; /* MfllImageBuffer1_0 */

}; /* namespace mfll */
#endif /* __MFLLIMAGEBUFFER1_0_H__ */
