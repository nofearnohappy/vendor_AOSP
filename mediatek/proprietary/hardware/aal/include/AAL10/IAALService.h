#ifndef __IAALSERVICE_H__
#define __IAALSERVICE_H__

#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <binder/BinderService.h>

namespace android
{
//
//  Holder service for pass objects between processes.
//
class IAALService : public IInterface 
{
protected:
    enum {
        AAL_SET_FUNCTION = IBinder::FIRST_CALL_TRANSACTION,
        AAL_SET_LIGHT_SENSOR_VALUE,
        AAL_SET_SCREEN_STATE,
        AAL_SET_SMARTBACKLIGHT_LEVEL,
        AAL_SET_TOLERANCE_RATIO_LEVEL,
        AAL_SET_READABILITY_LEVEL
    };

    enum {
        SCREEN_STATE_OFF = 0,
        SCREEN_STATE_DOZE = 1,
        SCREEN_STATE_DIM = 2,
        SCREEN_STATE_ON = 3,
    };

public:

    // screen brightenss mode copy from Settings.System
    enum BrightnessMode {
        /** SCREEN_BRIGHTNESS_MODE value for manual mode. */
        SCREEN_BRIGHTNESS_MODE_MANUAL = 0,
        /** SCREEN_BRIGHTNESS_MODE value for automatic mode. */
        SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
        /** SCREEN_ECO_BRIGHTNESS_MODE value for automatic eco backlight mode. */
        SCREEN_BRIGHTNESS_ECO_MODE_AUTOMATIC,
    };

    enum {
        FUNC_NONE = 0x0,
        FUNC_CABC = 0x2,
        FUNC_DRE  = 0x4
    };

    DECLARE_META_INTERFACE(AALService);

    virtual status_t setFunction(uint32_t funcFlags) = 0;
    virtual status_t setLightSensorValue(int32_t value) = 0;
    virtual status_t setScreenState(int32_t state, int32_t brightness) = 0;
    virtual status_t setSmartBacklightLevel(int32_t level) = 0;
    virtual status_t setToleranceRatioLevel(int32_t level) = 0;
    virtual status_t setReadabilityLevel(int32_t level) = 0;
};

class BnAALService : public BnInterface<IAALService> 
{
    virtual status_t onTransact(uint32_t code,
                                const Parcel& data,
                                Parcel* reply,
                                uint32_t flags = 0);
};    

};

#endif




