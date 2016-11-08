#ifndef _IMFLLIMAGEBUFFER_H__
#define _IMFLLIMAGEBUFFER_H__

#include "MfllDefs.h"
#include "MfllTypes.h"
#include "IMfllEvents.h"

#include <utils/RefBase.h> // android::RefBase

namespace mfll {
class IMfllImageBuffer : public android::RefBase {
public:
    static IMfllImageBuffer* createInstance(void);

public:
    virtual enum MfllErr setResolution(unsigned int w, unsigned int h) = 0;
    virtual enum MfllErr setAligned(unsigned int aligned_w, unsigned int aligned_h) = 0;
    virtual enum MfllErr setImageFormat(enum ImageFormat f) = 0;
    virtual enum MfllErr getResolution(unsigned int &w, unsigned int &h) = 0;
    virtual enum MfllErr getAligned(unsigned int &w, unsigned int &h) = 0;
    virtual enum ImageFormat getImageFormat(void) = 0;

public:
    /**
     *  Acquire physical memory trunk according information created.
     *
     *  @return                 - Returns MfllErr_Ok if ok.
     *  @note                   - This function should be synchronized.
     *                            This function should be thread-safe.
     *                            Duplicated call should not make errors.
     */
    virtual enum MfllErr initBuffer(void) = 0;

    /**
     *  Get virtual address of image buffer
     *
     *  @return                 - If failed or not inited returns NULL.
     *  @note                   - This function should be thread-safe.
     */
    virtual void* getVa(void) = 0;

    /**
     *  Get the real buffer size of the MfllImageBuffer.
     *
     *  @return                 - If the buffer hasn't been allocated, this function will return 0.
     *  @note                   - This function should be thread-safe.
     */
    virtual size_t getRealBufferSize(void) = 0;

    /**
     *  Get physical image buffer handle
     *
     *  @return                 - Get physical image buffer handle
     *  @note                   - You have to now what your implement
     */
    virtual void* getPhysicalImageBuffer(void) = 0;

    /**
     *  Release buffer memory directly, or while the object is being destroyed,
     *  this function will be invoked in destructor
     *
     *  @return                 - Returns MfllErr_Ok if ok.
     *  @note                   - This function should be synchronized.
     *                            This function should be thread-safe.
     *                            Duplicated call should not make errors.
     *                            This function should be invoked while instance is being destroyed.
     *                            We do not suggest caller to invoke this function to release buffer,
     *                            it's better to release buffer by destructor.
     */
    virtual enum MfllErr releaseBuffer(void) = 0;

    /**
     *  Register event dispatcher.
     *
     *  @param e                - A strong pointer of IMfllEvents.
     *  @return                 - Returns MfllErr_Ok if ok.
     */
    virtual enum MfllErr registerEventDispatcher(const sp<IMfllEvents> &e) = 0;

    /**
     *  Save image to file.
     *
     *  @param *name            - File path to save.
     *  @return                 - Returns MfllErr_Ok if ok.
     */
    virtual enum MfllErr saveFile(const char *name) = 0;

    /**
     *  Load image from file.
     *
     *  @param *name            - File path to load.
     *  @return                 - Returns MfllErr_Ok if ok.
     */
    virtual enum MfllErr loadFile(const char *name) = 0;

protected:
    virtual ~IMfllImageBuffer(){};
};
}; /* namespace mfll */
#endif//_IMFLLIMAGEBUFFER_H__

