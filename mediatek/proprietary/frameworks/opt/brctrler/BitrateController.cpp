

// #define LOG_NDEBUG 0
#define LOG_TAG "BitCtrl"
#include <utils/Log.h>

#include "BitrateController.h"

#include <cutils/properties.h>

namespace android {

BitrateController::BitrateController()
    : mMaxLevel(5),
      mMinLevel(0),
      mLevelNow(5),
      mMaxBitrateK(4800),
      mWindowSize(30),  // if no other concern, it should be equal to FPS
      mFPS(30),
      mTolerantBitrateK(4800),
      mSingleITimes(12),
      mSinglePTimes(8),
      mContTimes(1),
      mContFrames(5),
      mFrameCount(0),
      mSkipFrameCount(0),
      mCurrentBitrate(0),
      mSegSize(4),
      mDbgMode(0)
{
    char value[PROPERTY_VALUE_MAX];
    property_get("mtk.wfd.skipbr", value, "0");
    if (atoi(value))
    {
        mTolerantBitrateK = atoi(value);
        ALOGI("mTolerantBitrateK change to %d", mTolerantBitrateK);
    }
    property_get("mtk.wfd.skipsItimes", value, "0");
    if (atoi(value))
    {
        mSingleITimes = atoi(value);
        ALOGI("mSingleITimes change to %d", mSingleITimes);
    }
    property_get("mtk.wfd.skipsPtimes", value, "0");
    if (atoi(value))
    {
        mSinglePTimes = atoi(value);
        ALOGI("mSinglePTimes change to %d", mSinglePTimes);
    }
    property_get("mtk.wfd.skipctimes", value, "0");
    if (atoi(value))
    {
        mContTimes = atoi(value);
        ALOGI("mContTimes change to %d", mContTimes);
    }
    property_get("mtk.wfd.skipcframes", value, "0");
    if (atoi(value))
    {
        mContFrames = atoi(value);
        ALOGI("mContFrames change to %d", mContFrames);
    }
    property_get("mtk.wfd.skiplevel", value, "0");
    if (atoi(value))
    {
        mLevelNow = atoi(value);
        ALOGI("mLevelNow change to %d", mLevelNow);
        updownLevel(0);
    }
    property_get("mtk.wfd.skipseq", value, "0");
    if (atoi(value))
    {
        mSegSize = atoi(value);
        ALOGI("mSegSize change to %d", mSegSize);
    }
    updateThreshold();
    mSegFrameCount.push(0);
    mSegSkipFrameCount.push(0);

    property_get("mtk.wfd.skipdbg", value, "0");
    if (atoi(value))
    {
        mDbgMode = atoi(value);
        ALOGI("mDbgMode change to %d", mDbgMode);
    }

    ALOGI("BitrateController con");
}

BitrateController::~BitrateController()
{
    ALOGI("~BitrateController des");
}
#if 0
int BitrateController::inputOneFrame()
{
    ++mFrameCount;
    return 1;
}
#endif//0
int BitrateController::setOneFrameBits(int bits, bool isI)
{
    ALOGI("set one bits %d, isI: %d, w size:%d, v size:%d", bits, isI, mWindowSize, mBitrateVector.size());
    ++mFrameCount;
    ++mSegFrameCount.editItemAt(mSegFrameCount.size()-1);
    if (mBitrateVector.size() == mWindowSize)
    {
        mCurrentBitrate -= mBitrateVector[0];
        mFrameTypeVector.removeAt(0);
        mBitrateVector.removeAt(0);
    }
    mCurrentBitrate += bits;
    mBitrateVector.push(bits);
    mFrameTypeVector.push(isI);
    return 1;
}

int BitrateController::updownLevel(int updownNum)
{
    mLevelNow += updownNum;
    ALOGI("updownLevel: in %d, l %d", updownNum, mLevelNow);
    if (mLevelNow > mMaxLevel) mLevelNow = mMaxLevel;
    if (mLevelNow < mMinLevel) mLevelNow = mMinLevel;

    if (mSegFrameCount.size() == mSegSize)
    {
        mSegFrameCount.removeAt(0);
        mSegSkipFrameCount.removeAt(0);
    }
    mSegFrameCount.push(0);
    mSegSkipFrameCount.push(0);

    if (mLevelNow == 0)//0
    {
        mTolerantBitrateK = 1;
    }
    else if(mLevelNow >= mMaxLevel-1)//5, 4
    {
        mTolerantBitrateK = (mMaxBitrateK/(mMaxLevel-1))*(mMaxLevel-1);
    }
    else//3, 2, 1
    {
        mTolerantBitrateK = (mMaxBitrateK/(mMaxLevel-1))*mLevelNow;
    }

    updateThreshold();
    return 1;
}

bool BitrateController::checkSkip()
{
    if (mLevelNow >= mMaxLevel)
    {
        ALOGI("check pass 1, l = %d, fc:%d, v size:%d", mLevelNow, mFrameCount, mBitrateVector.size());
        return false;
    }

    if (mLevelNow == -1)
    {
        ALOGI("check do skip 1, l = %d, fc:%d, v size:%d", mLevelNow, mFrameCount, mBitrateVector.size());
        ++mSkipFrameCount;
        ++mSegSkipFrameCount.editItemAt(mSegSkipFrameCount.size()-1);
        return true;
    }

    if (mCurrentBitrate > mTolerantBitrateK*1024)
    {
        ALOGI("check do skip 2, finally %d, %d, l = %d, fc:%d, v size:%d",
                mCurrentBitrate, mTolerantBitrateK*1024, mLevelNow, mFrameCount, mBitrateVector.size());
        ++mSkipFrameCount;
        ++mSegSkipFrameCount.editItemAt(mSegSkipFrameCount.size()-1);
        return true;
    }

    if (mBitrateVector.size() < mContFrames)//caution if don't do this, below condition may SIGV
    {
        ALOGI("check pass 2, l = %d, fc:%d, v size:%d", mLevelNow, mFrameCount, mBitrateVector.size());
        return false;
    }

    if (mFrameTypeVector[mFrameTypeVector.size()-1])
    {
        if (mBitrateVector[mBitrateVector.size()-1] > mSingleIThreshold)
        {
            ALOGI("check do skip 3, b:%d, th:%d, fc:%d, v size:%d",
                    mBitrateVector[mBitrateVector.size()-1], mSingleIThreshold, mFrameCount, mBitrateVector.size());
            ++mSkipFrameCount;
            ++mSegSkipFrameCount.editItemAt(mSegSkipFrameCount.size()-1);
            return true;
        }
    }
    else
    {
        if (mBitrateVector[mBitrateVector.size()-1] > mSinglePThreshold)
        {
            ALOGI("check do skip 4, b:%d, th:%d, fc:%d, v size:%d",
                    mBitrateVector[mBitrateVector.size()-1], mSinglePThreshold, mFrameCount, mBitrateVector.size());
            ++mSkipFrameCount;
            ++mSegSkipFrameCount.editItemAt(mSegSkipFrameCount.size()-1);
            return true;
        }
    }

    int i, iTmpBits=0;
    for(i=(int)(mBitrateVector.size()-1);i>=(int)(mBitrateVector.size()-mContFrames);--i)
    {
        iTmpBits += mBitrateVector[i];
    }
    if (iTmpBits > mContFrameThreshold)
    {
        ALOGI("check do skip 5, b:%d, th:%d, fc:%d, v size:%d",
                iTmpBits, mContFrameThreshold, mFrameCount, mBitrateVector.size());
        ++mSkipFrameCount;
        ++mSegSkipFrameCount.editItemAt(mSegSkipFrameCount.size()-1);
        return true;
    }

    ALOGI("check pass, s b:%d, th:%d, th:%d, c b:%d, c th:%d, fc:%d, v size:%d",
            mBitrateVector[mBitrateVector.size()-1], mSingleIThreshold, mSinglePThreshold,
            iTmpBits, mContFrameThreshold, mFrameCount, mBitrateVector.size());
    return false;
}

int BitrateController::getStatus(int type)
{
    int iRet;
    switch(type)
    {
        case EXPECTED_BITRATE:
            if (mDbgMode)
            {
                iRet = mTolerantBitrateK;
                return (mTolerantBitrateK);
            }
            else
            {
                iRet = ((mTolerantBitrateK)/(mMaxBitrateK/(mMaxLevel-1)));
                //ALOGI("%d %d %d", mTolerantBitrateK, (mMaxBitrateK/(mMaxLevel-1)), ((mTolerantBitrateK)/(mMaxBitrateK/(mMaxLevel-1))));
            }
            ALOGI("Give Expected bitrate:%d, dbg:%d", iRet, mDbgMode);
            return iRet;
            break;
        case CURRENT_BITRATE:
            {
                int CurrentBitRate = 0;
#if 0
                for(int i=0;i<mBitrateVector.size();i++)
                {
                    CurrentBitRate += mBitrateVector[i];
                }
#else
                CurrentBitRate = mCurrentBitrate;
#endif//0
                if (mDbgMode)
                {
                    iRet = CurrentBitRate>>10;
                    ALOGI("Give Current bitrate:%d (%d), dbg:%d", iRet, mCurrentBitrate>>10, mDbgMode);
                }
                else
                {
                    //ALOGI("%d %d %d", CurrentBitRate, ((mMaxBitrateK/(mMaxLevel-1)) << 10), CurrentBitRate/((mMaxBitrateK/(mMaxLevel-1)) << 10));
                    if (mLevelNow == mMinLevel) iRet = 0;
                    else iRet = CurrentBitRate/((mMaxBitrateK/(mMaxLevel-1)) << 10);
                    ALOGI("Give Current bitrate:%d, dbg:%d", iRet, mDbgMode);
                }
                return iRet;
                break;
            }
        case SKIPRATE:
            {
                float SkipRate;
                int SkipFrameCount=0, FrameCount=0;
                char dbgBuf1[32]={0}, dbgBuf2[32]={0};
                char *p1=dbgBuf1, *p2=dbgBuf2;
                for(int i=0;i<mSegFrameCount.size();i++)
                {
                    FrameCount += mSegFrameCount[i];
                    SkipFrameCount += mSegSkipFrameCount[i];
                    //ALOGI("%d %d", mSegFrameCount[i], mSegSkipFrameCount[i]);
                    if (mDbgMode)
                    {
                        p1 += sprintf(p1, "%d ", mSegFrameCount[i]);
                        p2 += sprintf(p2, "%d ", mSegSkipFrameCount[i]);
                    }
                }
                if (mDbgMode)
                {
                    ALOGI("%s", dbgBuf1);
                    ALOGI("%s", dbgBuf2);
                }
                if (FrameCount != 0)
                {
                    SkipRate = ((float)SkipFrameCount)/FrameCount;
                }
                else
                {
                    SkipRate = 0;
                }
                if (mDbgMode)
                {
                    iRet = (100 - (int)(SkipRate*100));
                    //ALOGI("Give SkipRate:%f, %d", mSkipRate, (int)(mSkipRate*100));
                    ALOGI("Give Flu Rate:%f, %d, dbg:%d", 1 - SkipRate, 100 - (int)(SkipRate*100), mDbgMode);
                }
                else
                {
                    iRet = -1;
                    ALOGI("Give Flu Rate:%d, dbg:%d", iRet, mDbgMode);
                }
                return iRet;
                break;
            }
        default:
            ALOGE("Unknown parameter type:%d", type);
            break;
    }
    return 0;
}

int BitrateController::setTolerantBitrate(int bitrate)
{
    mMaxBitrateK = bitrate/1024;
    if (mLevelNow == 0)//0
    {
        mTolerantBitrateK = 1;
    }
    else if(mLevelNow >= mMaxLevel-1)//5, 4
    {
        mTolerantBitrateK = (mMaxBitrateK/(mMaxLevel-1))*(mMaxLevel-1);
    }
    else//3, 2, 1
    {
        mTolerantBitrateK = (mMaxBitrateK/(mMaxLevel-1))*mLevelNow;
    }

    updateThreshold();
    ALOGD("set bitreate:%d", bitrate);
    return 1;
}

int BitrateController::updateThreshold(void)
{
    mSingleIThreshold = (mTolerantBitrateK/mFPS)*mSingleITimes*1000;
    mSinglePThreshold = (mTolerantBitrateK/mFPS)*mSinglePTimes*1000;
    mContFrameThreshold = (mTolerantBitrateK/mFPS)*mContFrames*mContTimes*1000;
    return 1;
}

}  // namespace android

//android::BitrateController gBitrateCtrler;
//void *gBCObjects[4] = {0};
int *gBCCount = 0;

extern "C"
int InitBC(void **pBcHandle)
{
    android::BitrateController **ppBc = (android::BitrateController**)pBcHandle;
    *ppBc = new android::BitrateController;
    ++gBCCount;
    return 1;
}
extern "C"
int DeInitBC(void *bcHandle)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    delete bc;
    --gBCCount;
    return 1;
}

extern "C"
int SetOneFrameBits(void *bcHandle, int bits, bool isI)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    return bc->setOneFrameBits(bits, isI);
    //return gBitrateCtrler.setOneFrameBits(bits, isI);
}
extern "C"
bool CheckSkip(void *bcHandle)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    return bc->checkSkip();
    //return gBitrateCtrler.checkSkip();
}
extern "C"
int UpdownLevel(void *bcHandle, int upDownNum)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    return bc->updownLevel(upDownNum);
    //return gBitrateCtrler.updownLevel(upDownNum);
}
extern "C"
int GetStatus(void *bcHandle, int type)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    return bc->getStatus(type);
    //return gBitrateCtrler.getStatus(type);
}
extern "C"
int SetTolerantBitrate(void *bcHandle, int bitrate)
{
    android::BitrateController *bc = (android::BitrateController*)bcHandle;
    return bc->setTolerantBitrate(bitrate);
}

