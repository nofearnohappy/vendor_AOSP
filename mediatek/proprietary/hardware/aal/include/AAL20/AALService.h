#ifndef __AAL_SERVICE_H__
#define __AAL_SERVICE_H__

#include <utils/threads.h>

// HAL
#include <hardware/hardware.h>
#include <hardware/lights.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include "IAALService.h"


class CAALFW;
struct AALInitReg;
struct AALInput;
struct AALOutput;

namespace android
{

class CustFunctions;
class AALLightSensor;
class AdaptFields;

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
    
    virtual status_t setSmartBacklightStrength(int32_t level);
    virtual status_t setSmartBacklightRange(int32_t level);
    virtual status_t setReadabilityLevel(int32_t level);
    virtual status_t setLowBLReadabilityLevel(int32_t level);
    virtual status_t getParameters(AALParameters *outParam);
    virtual status_t custInvoke(int32_t cmd, int64_t arg);
    virtual status_t readField(uint32_t field, uint32_t *value);
    virtual status_t writeField(uint32_t field, uint32_t value);

    virtual status_t setAdaptField(IAALService::AdaptFieldId field, void *data, int32_t size, uint32_t *serial);
    virtual status_t getAdaptSerial(IAALService::AdaptFieldId field, uint32_t *value);
    virtual status_t getAdaptField(IAALService::AdaptFieldId field, void *data, int32_t size, uint32_t *serial);
    
private:
    virtual void onFirstRef();
    virtual status_t readyToRun();
    bool initDriverRegs();
    virtual bool threadLoop();
    status_t enableAALEvent(bool enable);
    status_t debugDump(unsigned int debugLevel);
    void initAdapt();
    void deinitAdapt();

    void onBacklightChanged(int32_t level_1024);
    static void onALIChanged(void *obj, int32_t ali);

    void updateDebugInfo(sp<SurfaceControl> surface);
    void clearDebugInfo(sp<SurfaceControl> surface);

    // hardware
    light_device_t *mLight;
    int mDispFd;

    mutable Mutex mLock;
    bool mEventEnabled;
    volatile bool mToEnableEvent;
    volatile bool mUseExternalAli;

    int mScrWidth;
    int mScrHeight;
    int mALI;
    int mPmsScrState; // Screen state of power manager
    int mPrevScrState;
    int mCurrScrState;
    int mBacklight;
    int mTargetBacklight;
    int mLongTermBacklight;
    int mOutBacklight;
    int mOutCabcGain;
    unsigned int mPrevFuncFlags;
    unsigned int mFuncFlags; // bit-set of AALFunction
    int mUserBrightness;

    // Store in member variable to debug
    AALInitReg *mAALInitReg;
    AALInput *mAALInput;
    AALOutput *mAALOutput;

    CAALFW *mAALFW;
    CustFunctions *mCustFunc;
    AALLightSensor *mLightSensor;
    AdaptFields *mAdaptFields;

    unsigned int mDebugLevel;
    struct {
        bool panelOn;
        bool panelDisplayed;
        bool alwaysEnable;
        int overwriteALI;
        int overwriteBacklight;
        int overwriteCABC;
        int overwriteDRE;
    } mDebug;

    unsigned int mEventFlags;
    
    void setEvent(unsigned int event) {
        mEventFlags |= event;
    }

    bool isEventSet(unsigned int event) {
        return ((mEventFlags & event) > 0);
    }

    void clearEvents(unsigned int events = 0xffff) {
        mEventFlags &= ~events;
    }

    void unitTest();
};
};

#endif
