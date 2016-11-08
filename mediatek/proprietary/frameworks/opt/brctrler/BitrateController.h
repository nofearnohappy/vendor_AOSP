#ifndef BITRATECTRLR_H_

#define BITRATECTRLR_H_


#include <stdlib.h>
#include <utils/Vector.h>
#include <media/stagefright/foundation/ABase.h>

namespace android {

struct BitrateController {
    BitrateController();
    ~BitrateController();

    enum ParaType {
        EXPECTED_BITRATE = 0,
        CURRENT_BITRATE = 1,
        SKIPRATE = 2,
        MAX = 3
    };
    int setOneFrameBits(int bits, bool isI);
    bool checkSkip();
    int updownLevel(int upDownNum);
    int getStatus(int type);
    int setTolerantBitrate(int bitrate);

private:
    int mMaxLevel;
    int mMinLevel;
    int mLevelNow;
    int mMaxBitrateK;

    Vector<int> mBitrateVector;
    Vector<bool> mFrameTypeVector;

    int mWindowSize;
    int mFPS;
    int mTolerantBitrateK;

    int mSingleITimes;
    int mSinglePTimes;
    int mContTimes;
    int mContFrames;

    int mSingleIThreshold;
    int mSinglePThreshold;
    int mContFrameThreshold;

    int mFrameCount;
    int mSkipFrameCount;
    int mCurrentBitrate;

    Vector<int> mSegFrameCount;
    Vector<int> mSegSkipFrameCount;
    int mSegSize;

    bool mDbgMode;

    int updateThreshold(void);
    DISALLOW_EVIL_CONSTRUCTORS(BitrateController);
};

}  // namespace android

#endif  // BITRATECTRLR_H_
