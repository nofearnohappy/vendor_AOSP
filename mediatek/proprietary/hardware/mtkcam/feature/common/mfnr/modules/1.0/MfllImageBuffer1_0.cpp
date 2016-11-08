#include "MfllImageBuffer1_0.h"
#include "MfllLog.h"

#include <mtkcam/UITypes.h>
#include <mtkcam/ImageFormat.h>
#include <mtkcam/utils/Format.h>
#include <mtkcam/utils/ImageBufferHeap.h>
#include <mtkcam/utils/imagebuf/IIonImageBufferHeap.h>

#include <stdio.h> // FILE

#define BUFFER_USAGE_SW (eBUFFER_USAGE_SW_READ_OFTEN | \
                         eBUFFER_USAGE_SW_WRITE_OFTEN | \
                         eBUFFER_USAGE_HW_CAMERA_READWRITE)

#define ALIGN(w, a) (((w + (a-1)) / a) * a)

using namespace mfll;
using namespace NSCam;
using namespace NSCam::Utils::Format;
using android::sp;
using NSCam::IImageBuffer;

IMfllImageBuffer* IMfllImageBuffer::createInstance(void)
{
    return (IMfllImageBuffer*)new MfllImageBuffer1_0;
}

MfllImageBuffer1_0::MfllImageBuffer1_0(void)
{
    m_imgBuffer = NULL;
    m_spEventDispatcher = NULL;
    m_width = 0;
    m_height = 0;
    m_alignedWidth = 2;
    m_alignedHeight = 2;
    m_bufferSize = 0;
    m_locker = PTHREAD_MUTEX_INITIALIZER;
}

MfllImageBuffer1_0::~MfllImageBuffer1_0(void)
{
    releaseBuffer();
}

enum MfllErr MfllImageBuffer1_0::setResolution(unsigned int w, unsigned int h)
{
    enum MfllErr err = MfllErr_Ok;
    lock();
    if (m_imgBuffer.get()) {
        err = MfllErr_Shooted;
        goto lbExit;
    }
    m_width = w;
    m_height = h;
lbExit:
    unlock();
    return err;
}

enum MfllErr MfllImageBuffer1_0::setAligned(unsigned int aligned_w, unsigned int aligned_h)
{
    enum MfllErr err = MfllErr_Ok;
    lock();
    if (m_imgBuffer.get()) {
        err = MfllErr_Shooted;
        goto lbExit;
    }
    m_alignedWidth = aligned_w;
    m_alignedHeight = aligned_h;
lbExit:
    unlock();
    return err;
}

enum MfllErr MfllImageBuffer1_0::setImageFormat(enum ImageFormat f)
{
    enum MfllErr err = MfllErr_Ok;
    lock();
    if (m_imgBuffer.get()) {
        err = MfllErr_Shooted;
        goto lbExit;
    }
    m_format = f;
lbExit:
    unlock();
    return err;
}

enum MfllErr MfllImageBuffer1_0::getResolution(unsigned int &w, unsigned int &h)
{
    w = m_width;
    h = m_height;
    return MfllErr_Ok;
}

enum MfllErr MfllImageBuffer1_0::getAligned(unsigned int &w, unsigned int &h)
{
    w = m_alignedWidth;
    h = m_alignedHeight;
    return MfllErr_Ok;
}

enum ImageFormat MfllImageBuffer1_0::getImageFormat(void)
{
    return m_format;
}

enum MfllErr MfllImageBuffer1_0::initBuffer(void)
{
    enum MfllErr err = MfllErr_Ok;
    mfllFunctionIn();

    lock();

    /* release first */
    if (m_imgBuffer.get()) {
        err = MfllErr_Shooted;
        goto lbExit;
    }

    {
        unsigned int imageSize = 0;
        err = createImageBuffer(m_imgBuffer, imageSize, m_width, m_height, m_alignedWidth, m_alignedHeight, m_format);
        if (err == MfllErr_Ok)
            m_bufferSize = imageSize;
    }

lbExit:
    unlock();
    if (err != MfllErr_Ok) {
        if (err == MfllErr_Shooted) {
            mfllLogW("%s: Image buffer has been inited", __FUNCTION__);
            err = MfllErr_Ok; // set MfllErr_Ok.
        }
        else
            mfllLogE("%s: create buffer failed with code %d", __FUNCTION__, (int)err);
    }

    mfllFunctionOut();
    return err;
}

void* MfllImageBuffer1_0::getVa(void)
{
    void *va = NULL;
    mfllFunctionIn();
    lock();
    if (m_imgBuffer.get()) {
        va = (void*)(long)m_imgBuffer->getBufVA(0);
    }
    unlock();
    mfllFunctionOut();
    return va;
}

size_t MfllImageBuffer1_0::getRealBufferSize(void)
{
    return m_bufferSize;
}

void* MfllImageBuffer1_0::getPhysicalImageBuffer(void)
{
    return (void*)m_imgBuffer.get();
}

enum MfllErr MfllImageBuffer1_0::releaseBuffer(void)
{
    enum MfllErr err = MfllErr_Ok;

    mfllFunctionIn();

    lock();

    if (m_imgBuffer.get()) {
        m_imgBuffer->unlockBuf(MFLL_LOG_KEYWORD);
        m_imgBuffer = NULL;
        m_bufferSize = 0;
    }
    else {
        err = MfllErr_NullPointer;
        goto lbExit;
    }

lbExit:
    unlock();
    if (err == MfllErr_NullPointer) {
        mfllLogW("%s: m_imgBuffer is already NULL", __FUNCTION__);
        err = MfllErr_Ok; // set back ok!
    }

    mfllFunctionOut();
    return MfllErr_Ok;
}

enum MfllErr MfllImageBuffer1_0::registerEventDispatcher(const sp<IMfllEvents> &e)
{
    m_spEventDispatcher = e;
    return MfllErr_Ok;
}

enum MfllErr MfllImageBuffer1_0::saveFile(const char *name)
{
    MBOOL bRet;
    enum MfllErr err = MfllErr_Ok;

    lock();

    if (m_imgBuffer.get() == NULL) {
        err = MfllErr_NullPointer;
        goto lbExit;
    }

    bRet = m_imgBuffer->saveToFile(name);

    if (bRet) {
        err = MfllErr_Ok;
    }
    else {
        err = MfllErr_UnexpectedError;
    }

lbExit:
    unlock();

    if (err == MfllErr_NullPointer) {
        mfllLogE("%s: can't save file with NULL pointer", __FUNCTION__);
    }
    else if (err == MfllErr_UnexpectedError) {
        mfllLogE("%s: save file failed with unexpected fail", __FUNCTION__);
    }

    return err;
}

enum MfllErr MfllImageBuffer1_0::loadFile(const char *name)
{
    enum MfllErr err = MfllErr_Ok;
    MBOOL bRet = MTRUE;
    FILE *fp = NULL;
    size_t fileSize = 0;
    size_t resultSize = 0;
    size_t bufferSize = 0;

    lock();

    /* check imgBuf */
    if (m_imgBuffer.get() == NULL) {
        err = MfllErr_NullPointer;
        goto lbExit;
    }

    /* check file */
    fp = fopen(name, "rb");
    if (fp == NULL) {
        err = MfllErr_BadArgument;
        goto lbExit;
    }

    /* tell file size */
    fseek(fp, 0L, SEEK_END);
    fileSize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    bufferSize = getRealBufferSize();

    /* check file size with aligned */
    if (bufferSize != fileSize) {
        err = MfllErr_BadArgument;
        goto lbExit;
    }
    {
        void *addr = (void*)(long)m_imgBuffer->getBufVA(0);
        fread(addr, 1L, bufferSize, fp);
    }

lbExit:
    unlock();

    if (fp)
        fclose(fp);

    if (err == MfllErr_NullPointer) {
        mfllLogE("%s: load file failed with NULL image buffer", __FUNCTION__);
    }
    else if (err == MfllErr_BadArgument) {
        if (bufferSize != fileSize)
            mfllLogE("%s: file size doesn't match, bufferSize = %d, fileSize = %d", __FUNCTION__, bufferSize, fileSize);
        else
            mfllLogE("%s: file %s cannot be opened", __FUNCTION__, name);
    }

    return err;
}

enum MfllErr MfllImageBuffer1_0::createImageBuffer(sp<IImageBuffer> &imgBuf, unsigned int &imageSize, int width, int height, int align_width, int align_height, enum ImageFormat f)
{
    MBOOL bRet = MTRUE;
    enum MfllErr err = MfllErr_Ok;

    IImageBufferAllocator *allocator = NULL;
    sp<IImageBuffer> tempBuf = NULL;
    sp<IImageBuffer> pBuf = NULL;
    sp<IImageBufferHeap> pHeap = NULL;

    MUINT32 imgFmt = convertImageFormat(f);
    MUINT32 planeCount = queryPlaneCount(imgFmt);
    MINT32 bufBoundaryInBytes[3] = {0}; // always be zero
    MUINT32 strideInBytes[3] = {0};
    MUINT32 addrOffset[3] = {0};
    MUINT32 alignedw;
    MUINT32 alignedh;
    MUINT32 bpp = 0;

    /* check image format if valid*/
    if (imgFmt == 0) {
        mfllLogE("%s: Invalid input image format=%d", __FUNCTION__, (int)f);
        return MfllErr_BadArgument;
    }

    /* algin width and height */
    alignedw = ALIGN(width, align_width);
    alignedh = ALIGN(height, align_height);

    /* calculate total size, stride, and address of each planes */
    {
        imageSize = 0; // in bytes
        planeCount = queryPlaneCount(imgFmt);
        for (int i = 0; i < planeCount; i++) {
            unsigned int w = queryPlaneWidthInPixels(imgFmt, i, alignedw);
            unsigned int h = queryPlaneHeightInPixels(imgFmt, i, alignedh);
            unsigned int bitsPerPixel = queryPlaneBitsPerPixel(imgFmt, i);

            strideInBytes[i] = (w * bitsPerPixel) / 8;
            addrOffset[i] = imageSize;

            imageSize += (w * h * bitsPerPixel) / 8; // calcuate in bytes

            mfllLogD("%s: plane %d, stride = %d, addr_offset = %d", __FUNCTION__, i, strideInBytes[i], addrOffset[i]);
        }
    }
    mfllLogD("%s: create buffer with (w,h,alignedw,alignedh,fmt)=(%d,%d,%d,%d,%d)", __FUNCTION__, width, height, alignedw, alignedh, (int)imgFmt);
    mfllLogD("%s: buffer size = %d", __FUNCTION__, imageSize);

#if 0
    if (imgFmt == eImgFmt_YUY2) {
        bpp = 2; // 2 bytes per pixel
        imageSize = alignedw * alignedh * bpp;
        strideInBytes[0] = alignedw * bpp;
    }

    else if (imgFmt == eImgFmt_YV16) {
        bpp = 2; // 2 bytes per pixel
        imageSize = alignedw * alignedh * bpp;

        /* calculate strideInBytes for each plane */
        for (int i = 0; i < 3; i++) {
            /**
             *  notice that, bits per pixel may be smaller than 8(a byte), thereforce, calculate
             *  (bis per pixel) x (width) first, and divide 8 as bytes.
             */
            strideInBytes[i] = (queryPlaneWidthInPixels(imgFmt, i,  alignedw) * queryPlaneBitsPerPixel(imgFmt, i)) / 8;
            mfllLogD("%s: YV16 plane %d stride is %d", __FUNCTION__, i, strideInBytes[i]);
        }

        /* plane start address offset */
        addrOffset[0] = 0;
        addrOffset[1] = alignedw * alignedh; // half of image
        addrOffset[2] = addrOffset[1] + (addrOffset[1] >> 1);
    }
    else if (imgFmt == eImgFmt_Y8) {
        bpp = 1; // a byte per pixel
        imageSize = alignedw * alignedh * bpp;
        strideInBytes[0] = alignedw * bpp;
    }
    else {
        mfllLogE("%s: not support image format %d", __FUNCTION__, (int)imgFmt);
        return MfllErr_NotSupported;
    }
#endif


    // allocate blob buffer
    IImageBufferAllocator::ImgParam blobParam(imageSize, bufBoundaryInBytes[0]);
    allocator = IImageBufferAllocator::getInstance();

    if (allocator == NULL) {
        mfllLogE("%s: allocator is NULL", __FUNCTION__);
        return MfllErr_UnexpectedError;
    }

    tempBuf = allocator->alloc(MFLL_LOG_KEYWORD, blobParam);
    if (tempBuf.get() == NULL) {
        mfllLogE("%s: allocate blob buffer failed", __FUNCTION__);
        err = MfllErr_UnexpectedError;
        goto lbExit;
    }

    if (!tempBuf->lockBuf(MFLL_LOG_KEYWORD, BUFFER_USAGE_SW)) {
        mfllLogE("%s: lock blob buffer failed", __FUNCTION__);
        err = MfllErr_UnexpectedError;
        goto lbExit;
    }

    /* encapsulate tempBuf into external IImageBuffer */
    {
        IImageBufferAllocator::ImgParam extParam(imgFmt, MSize(width, height), strideInBytes, bufBoundaryInBytes, planeCount);
        MINT32      _memID[3];
        MUINTPTR    _virtAddr[3];
        for (int i = 0; i < 3; i++) {
            _memID[i] = tempBuf->getFD();
            _virtAddr[i] = tempBuf->getBufVA(0) + addrOffset[i];
        }

        if (planeCount > 1) {
            /* non-continuous buffer */
            PortBufInfo_v1 portBufInfo = PortBufInfo_v1(_memID, _virtAddr, planeCount);
            pHeap = ImageBufferHeap::create(MFLL_LOG_KEYWORD, extParam, portBufInfo);
        }
        else {
            /* A plane */
            PortBufInfo_v1 portBufInfo = PortBufInfo_v1(tempBuf->getFD(0), tempBuf->getBufVA(0));
            pHeap = ImageBufferHeap::create(MFLL_LOG_KEYWORD, extParam, portBufInfo);
        }
    }

    if (pHeap.get() == NULL) {
        mfllLogE("%s: create heap of blob failed", __FUNCTION__);
        goto lbExit;
    }

    /* create image buffer */
    pBuf = pHeap->createImageBuffer();

    if (pBuf.get() == NULL) {
        mfllLogE("%s: create image buffer from blob heap fail", __FUNCTION__);
        err = MfllErr_UnexpectedError;
        goto lbExit;
    }

    if (!pBuf->lockBuf(MFLL_LOG_KEYWORD, BUFFER_USAGE_SW)) {
        mfllLogE("%s: lock image buffer fail", __FUNCTION__);
        err = MfllErr_UnexpectedError;
        goto lbExit;
    }

    /* Great! You got an image buffer */
    imgBuf = pBuf;
    mfllLogD("%s: allocated buffer 0x%p with size %d", __FUNCTION__, imgBuf->getBufVA(0), imageSize);

lbExit:
    if (allocator)
        allocator->free(tempBuf.get());
    if (tempBuf.get())
        tempBuf->unlockBuf(MFLL_LOG_KEYWORD);

    return err;
}

MUINT32 MfllImageBuffer1_0::convertImageFormat(const enum ImageFormat &fmt)
{
    MUINT32 f = 0;
    switch(fmt) {
    case ImageFormat_Yuy2:
        f = eImgFmt_YUY2;
        break;
    case ImageFormat_Yv16:
        f = eImgFmt_YV16;
        break;
    case ImageFormat_Raw10:
        f = eImgFmt_BAYER10;
        break;
    case ImageFormat_Y8:
        f = eImgFmt_Y8;
        break;
    default:
        f = 0;
        break;
    }
    return f;
}
