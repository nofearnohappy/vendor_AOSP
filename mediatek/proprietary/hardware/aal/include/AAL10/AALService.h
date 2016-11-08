#ifndef __AAL_SERVICE_H__
#define __AAL_SERVICE_H__

#include <utils/threads.h>

// HAL
#include <hardware/hardware.h>
#include <hardware/lights.h>


#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

#include "AAL.h"
#include "IAALService.h"


#define AAL_STATISTICS_BUF_NUM 2


namespace android
{

class AALLightSensor;

class AALService : 
        public BinderService<AALService>, 
        public BnAALService,
        public Thread
{
    friend class BinderService<AALService>;
public:

    AALService();
    ~AALService();
    
    static char const* getServiceName() { return "AAL"; }
    
    // IAALServic interface
    virtual status_t setFunction(uint32_t func_bitset);
    virtual status_t setLightSensorValue(int32_t value);
    virtual status_t setScreenState(int32_t state, int32_t brightness);

    virtual status_t dump(int fd, const Vector<String16>& args);
    
    status_t setSmartBacklightLevel(int32_t level);
    status_t setToleranceRatioLevel(int32_t level);
    status_t setReadabilityLevel(int32_t level);
private:
    virtual void onFirstRef();
    virtual status_t readyToRun();
    virtual bool threadLoop();

    status_t enableAALEvent(int enable);

    status_t loadCalData();
    status_t loadCfgData();

    status_t loadConfig();

    status_t setFunctionNoLock(uint32_t func_bitset);
    status_t onBacklightChanged(int32_t level);
    static void onALSChanged(void *obj, int ali);

    status_t debugDump();

    void updateDebugInfo(sp<SurfaceControl> surface);
    void clearDebugInfo(sp<SurfaceControl> surface);

    // hardware
    int mFd;

    mutable Mutex mLock;
    bool mIsCalibrated;
    int mEnableEvent;
    volatile bool mToEnableEvent;
    volatile bool mUseExternalAli;
    int mState;
    int mInBacklight;
    unsigned long mOutBacklight;
    unsigned int mFuncFlags;
    AAL *mAALFW;
    AALLightSensor *mLightSensor;
    
    // ALS
    int mALI;

    // platform dependent
    int mIdx;
    DISP_AAL_STATISTICS mAALStatistic[AAL_STATISTICS_BUF_NUM]; // ring buffer to keep history
    DISP_AAL_PARAM mAALParams;

    unsigned int mFramePixels;

    // for AAL debug information
    bool mAALDebugOn;
    bool mAALDebugOnScreen;
};
};
#endif
