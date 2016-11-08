/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
#ifndef DOLBY_I_EFFECT_DAP_H_
#define DOLBY_I_EFFECT_DAP_H_

#include <utils/Errors.h>
#include <hardware/audio_effect.h>

#include "DapParams.h"
#include "ProfileParamParser.h"

namespace dolby {

using namespace android;
/*
              Track Effect Chain
            +--------------------+
 Track 1 ---| E1 | E2 | ... | En |----+    Session 0 Effect Chain
            +--------------------+    |    +--------------------+
                               Mixer <X>---| E1 | E2 | ... | En |----> HW Module
            +--------------------+    |    +--------------------+
 Track 2 ---| E1 | E2 | ... | En |----+                         ^
            +--------------------+    ^                   Output Buffer
            ^                     Mix Buffer             (== Mix Buffer)
       Track Buffer

The last effect in track chain is always configured in ACCUMULATE mode since its
output buffer is the Mix Buffer which is shared with all tracks on the output.

However, an effect on session 0 will never get ACCUMULATE output mode since mixing
is already done by the mixer on the track.

*/

/**
    Interface to be implemented by all effect implementations.

    This interface defines the boundary between common effect code and
    effect implementations. This is the minimal functionality that each
    effect implementation must provide.
*/
class IEffectDap
{
public:
    /**
        Factory function to instantiate effect implementation.

        The common code does not define this function anywhere. The effect
        implementation must define this function in one of the file to
        create and return a new instance of the implementation. The returned
        instance will be passed to delete at the end of its lifetime.
    */
    static IEffectDap *EffectDapFactory();

    /**
        Dummy virtual destructor to force proper cleanup.
    */
    virtual ~IEffectDap() { };

    /**
        Initialize the effect.

        Any initialization (memory allocation, file operations, etc) needed
        for effect operation must be performed in this function and *not* in
        the constructor. If this function returns an error, it may be called
        at a later time to perform initialization,
    */
    virtual status_t init() = 0;

    /**
        Returns true if the effect implementation is an offloaded effect.
     */
    virtual bool isOffload() const = 0;

    /**
        Processes input buffer and overwrites output buffer with processed data.

        This function should only be implemented in non-offloaded effect. This
        function should not perform any system calls (including memory allocations).
     */
    virtual status_t process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer) = 0;

    /**
        Bypass effect processing if effect is enabled.

        This function is used to stop effect processing without disabling the
        effect. This is used to suspend the effect when an audio stream that
        should not be processed by DAP is being played.
    */
    virtual void setBypass(bool bypass) = 0;

    /**
        Set state of effect to enabled or disabled.

        When effect is disabled, the process() function will continue to be executed
        as long as it returns NO_ERROR. Afterwards, no calls to process() function are
        made until the effect is enabled.
    */
    virtual status_t setEnabled(bool enabled) = 0;

    /**
        Set the output device to which the effect is attached.
    */
    virtual status_t setDevice(audio_devices_t device) = 0;

    /**
        Set audio format and configuration
    */
    virtual status_t setConfig(const buffer_config_t &inCfg, const buffer_config_t &outCfg) = 0;

    /**
        Set value of a DAP parameter for specified device.

        This function should update the value of parameter in a device specific
        cache. If the specified device is AUDIO_DEVICE_OUT_DEFAULT then the parameter
        value should be updated in all device caches.
    */
    virtual status_t setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length) = 0;

    /**
        Get value of a DAP parameter for specified device.

        This function fills the \p values array with retrieved parameter data and
        sets \p length to number of values. This function should return data from
        the DAP instance if the specified device is AUDIO_DEVICE_OUT_DEFAULT.
    */
    virtual status_t getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length) = 0;

    /**
        Commit only changed parameters in the specified device cache to DAP instance.

        This function should send the changes parameters in the device cache to DAP
        instance and mark the parameters as not modified.
    */
    virtual status_t commitChangedParams(audio_devices_t device) = 0;

    /**
        Commit all parameter in a device cache to DAP instance.
    */
    virtual status_t commitAllParams(audio_devices_t device) = 0;

    /**
        Get visualizer data from DAP instance.

        The data parameter must be able to hold 2 * (*bands) elements.
    */
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands) = 0;

    /**
        Helper function to set a parameter with single value.
    */
    status_t setParam(audio_devices_t device, DapParameterId param, dap_param_value_t value)
    {
        return setParam(device, param, &value, 1);
    }

    /**
        Helper function to get a parameter with single value.
    */
    dap_param_value_t getParam(audio_devices_t device, DapParameterId param, status_t *status=NULL)
    {
        dap_param_value_t value = 0;
        int length = 1;
        status_t ret = getParam(device, param, &value, &length);
        if (status != NULL)
        {
            *status = ret;
        }
        return value;
    }

    /**
     * Define the off profile for HW effect that doesn't support effect full bypass.
     */
    virtual status_t defineOffProfile(ProfileParamParser ppp)
    {
        return NO_ERROR;
    }

    /**
     * Get the DS off type adopted by the effect.
     */
    virtual int32_t getOffType()
    {
        return DS_OFF_TYPE_BYPASSED;
    }
};

} // namespace dolby

#endif//DOLBY_I_EFFECT_DAP_H_
