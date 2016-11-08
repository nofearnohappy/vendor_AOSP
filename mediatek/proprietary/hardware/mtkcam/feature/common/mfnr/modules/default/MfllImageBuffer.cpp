#include "MfllImageBuffer.h"

using namespace mfll;

IMfllImageBuffer* IMfllImageBuffer::createInstance(void)
{
    return (IMfllImageBuffer*)new MfllImageBuffer();
}

MfllImageBuffer::MfllImageBuffer(void)
{
}

MfllImageBuffer::~MfllImageBuffer(void)
{
}

enum MfllErr MfllImageBuffer::initBuffer(void)
{
    return MfllErr_NotImplemented;
}

void* MfllImageBuffer::getVa(void)
{
    return NULL;
}

size_t MfllImageBuffer::getRealBufferSize(void)
{
    return 0;
}

void* MfllImageBuffer::getPhysicalImageBuffer(void)
{
    return NULL;
}

enum MfllErr MfllImageBuffer::releaseBuffer(void)
{
    return MfllErr_NotImplemented;
}

enum MfllErr MfllImageBuffer::registerEventDispatcher(const sp<IMfllEvents> &e)
{
    return MfllErr_Ok;
}

enum MfllErr MfllImageBuffer::saveFile(const char *name)
{
    return MfllErr_NotImplemented;
}

enum MfllErr MfllImageBuffer::loadFile(const char *name)
{
    return MfllErr_NotImplemented;
}

