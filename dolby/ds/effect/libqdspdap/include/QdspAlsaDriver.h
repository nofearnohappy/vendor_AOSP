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

#ifndef DOLBY_QDSP_ALSA_DRIVER_H_
#define DOLBY_QDSP_ALSA_DRIVER_H_

#include <system/audio.h>
#include <utils/Errors.h>
#include "ProfileParamParser.h"
#include "DapParamCache.h"
#include "QdspParams.h"

namespace dolby {

using namespace android;

class QdspDriver
{
public:
    QdspDriver();
    ~QdspDriver();

    status_t init();
    status_t setEnabled(bool enable);
    status_t setParam(audio_devices_t device, QdspParameterId param, const dap_param_value_t* values, int length);
    status_t getParam(audio_devices_t device, QdspParameterId param, dap_param_value_t* values, int* length);
    status_t commitChangedParams(audio_devices_t device);
    status_t commitAllParams(audio_devices_t device);
    status_t getVisualizer(dap_param_value_t *data, int *bands);
    status_t setDevice(audio_devices_t device)
    { return NO_ERROR; }
    status_t defineOffProfile(ProfileParamParser ppp);
    DsOffType getOffType() { return DS_OFF_TYPE_PARAMETERIZED; };

protected:
    status_t open();
    status_t close();
    status_t applyParam(audio_devices_t device, QdspParameterId param, const dap_param_value_t* values, int length);
    status_t setSetParamCtl(int *data, int count);
    status_t setGetParamCtl(int *data, int count);
    status_t getGetParamCtl(int *data, int count);
    status_t getGetVisualizerCtl(int *data, int count);

private:
    struct mixer*     mMixer;    // ALSA mixer to apply the parameter.
    struct mixer_ctl* mMixerSetParamCtl; // ALSA mixer control to set parameter.
    struct mixer_ctl* mMixerGetParamCtl; // ALSA mixer control to get parameter.
    struct mixer_ctl* mMixerGetVisualizerCtl; // ALSA mixer control to get visualizer.
    DapParamCache mOffProfile;
    DapParamCache mActiveProfile;
};

};  // namespace dolby

#endif //DOLBY_QDSP_ALSA_DRIVER_H_
