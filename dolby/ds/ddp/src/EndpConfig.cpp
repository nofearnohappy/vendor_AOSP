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
#define LOG_TAG "EndpConfig"
#include "DlbLog.h"
#include <string.h>
#include <cutils/properties.h>

#include "EndpConfig.h"
#include "dap_cpdp.h"
#include "udc_api.h"

namespace android {


enum dapFeatureEnable
{
    DISABLE = 0,
    ENABLE
};


static const struct endpConfig endpConfigTable[] = 
{
//endp       stereo mode                 DRC setting           udcMaxChOut    jocOutputMode      nonJocOutputMode  jocDialogEn  nonJocDialogEn      VL_enable VL_In  VL_out VL_boost
{"hdmi2", {{{DDPI_UDC_STEREOMODE_STEREO,    DDPI_UDC_COMP_LINE,         2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,   DISABLE,             DISABLE, -496,  -496,   0}}, // DAP_OFF
           {{DDPI_UDC_STEREOMODE_SRND,      DDPI_UDC_COMP_LINE,         2},  {DAP_CPDP_PROCESS_2_LTRT, DAP_CPDP_PROCESS_2_LTRT, DISABLE,   DISABLE,   DISABLE, -496,  -496,   0}} }, // DAP_ON
},

{"hdmi6", {{{DDPI_UDC_STEREOMODE_AUTO,      DDPI_UDC_COMP_LINE,         6},  {DAP_CPDP_PROCESS_5_1, DAP_CPDP_PROCESS_5_1, DISABLE,   DISABLE,              DISABLE, -496,  -496,   0}}, // DAP_OFF
           {{DDPI_UDC_STEREOMODE_AUTO,      DDPI_UDC_COMP_LINE,         6},  {DAP_CPDP_PROCESS_5_1, DAP_CPDP_PROCESS_5_1, DISABLE,   DISABLE,              DISABLE, -496,  -496,   0}}}, // DAP_ON
},

{"hdmi8", {{{DDPI_UDC_STEREOMODE_AUTO,      DDPI_UDC_COMP_LINE,         8},  {DAP_CPDP_PROCESS_7_1, DAP_CPDP_PROCESS_7_1, DISABLE,   DISABLE,              DISABLE, -496, -496,    0}}, // DAP_OFF
           {{DDPI_UDC_STEREOMODE_AUTO,      DDPI_UDC_COMP_LINE,         8},  {DAP_CPDP_PROCESS_7_1, DAP_CPDP_PROCESS_7_1, DISABLE,   DISABLE,              DISABLE, -496, -496,    0}}}, // DAP_ON
},

#ifdef DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD
{"headset", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,   DISABLE,              ENABLE, -496,   -224,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_SRND,    DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2_HEADPHONE, DAP_CPDP_PROCESS_2_LTRT, ENABLE, DISABLE,  ENABLE, -496,   -176,   0}}}, // DAP_ON
},

#ifdef DOLBY_MONO_SPEAKER
{"speaker", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,  0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,  0}}}, // DAP_ON
},
#else
{"speaker", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_SRND,    DDPI_UDC_COMP_PORTABLE_L14, 8},  {DAP_CPDP_PROCESS_2_LTRT, DAP_CPDP_PROCESS_2_LTRT, DISABLE,  DISABLE,     ENABLE, -496,   -224,   0}}}, // DAP_ON
},
#endif

#else
{"headset", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,   DISABLE,              ENABLE, -496,   -224,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_SRND,    DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2_HEADPHONE, DAP_CPDP_PROCESS_2_LTRT, ENABLE, DISABLE,  ENABLE, -496,   -176,   0}}}, // DAP_ON
},

#ifdef DOLBY_MONO_SPEAKER
{"speaker", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,  0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,  0}}}, // DAP_ON
},
#else
{"speaker", {{{DDPI_UDC_STEREOMODE_STEREO,  DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,  DISABLE,               ENABLE, -496,   -224,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_SRND,    DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2_LTRT, DAP_CPDP_PROCESS_2_LTRT, DISABLE,  DISABLE,     ENABLE, -496,   -224,   0}}}, // DAP_ON
},
#endif
#endif // DOLBY_END

/*{"bluetooth", {{{DDPI_UDC_STEREOMODE_STEREO,   DDPI_UDC_COMP_PORTABLE_L14, 2},{DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE, DISABLE,             DISABLE, -496,  -496,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_SRND,     DDPI_UDC_COMP_PORTABLE_L14, 2},{DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE,     DISABLE,             DISABLE, -496,  -496,   0}}}, // DAP_ON
},*/

{"invalid", {{{DDPI_UDC_STEREOMODE_STEREO,   DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE, DISABLE,               DISABLE, -496,  -496,   0}}, // DAP_OFF
             {{DDPI_UDC_STEREOMODE_STEREO,   DDPI_UDC_COMP_PORTABLE_L14, 2},  {DAP_CPDP_PROCESS_2, DAP_CPDP_PROCESS_2, DISABLE, DISABLE,               DISABLE, -496,  -496,   0}}}, // DAP_ON
},

};

static const int kEndpoints = sizeof (endpConfigTable) / sizeof (endpConfigTable[0]);
static char cEndpointProp[] = "dolby.audio.sink.info";
static char cDsStateProp[] = "dolby.ds.state";

/*
 *      Resets the current endpoint index.
 *
 */
EndpConfigTable::EndpConfigTable()
{
    mCurrEndpIdx = -1;
    mInitTimeEndpIdx = -1;
    mCurrDapState = -1;
    mReconfigOnEndpChange = false;
}

/*
.* Checks if endpoint and DapState are changed and returns true if atleast one of them changed.
.*/
bool EndpConfigTable::isConfigChanged()
{
    bool endpChanged = checkAndUpdateEndp();
    bool globalDapStateChanged = checkAndUpdateDapState();

    return (endpChanged || globalDapStateChanged);
}

/*
 *      Reads the current active endpoint supported by UDC and updates
 *      the endpoint index.
 *      Returns true if there is a change otherwise false.
 *
 */
bool EndpConfigTable::checkAndUpdateEndp()
{
    char cEndpoint[PROPERTY_VALUE_MAX];

    property_get(cEndpointProp, cEndpoint, "invalid");
    int currEndpIdx = getEndpIndex(cEndpoint);

    if (currEndpIdx != mCurrEndpIdx)
    {
        mCurrEndpIdx = currEndpIdx;

        if (mInitTimeEndpIdx == -1)
        {
            // Get hold of endpoint index of decoder component at the time of
            // initilization. In this way we can configure DAP output mode 
            // based on AudioTrack's output channel count.
            mInitTimeEndpIdx = mCurrEndpIdx;
        }       
        return true;
    }

    return false;
}

/*
 *      Reads the current DAP state from system property and returns true if the state is modified.
 *
 */
bool EndpConfigTable::checkAndUpdateDapState()
{
    char cDsState[PROPERTY_VALUE_MAX];

    property_get(cDsStateProp, cDsState, "invalid");

    int activeDapState = (strcmp(cDsState, "on") == 0) ? DAP_STATE_ON : DAP_STATE_OFF;

    if (activeDapState != mCurrDapState)
    {
        ALOGI("DAP state has changed from %d to %d", mCurrDapState, activeDapState);
        mCurrDapState = activeDapState;
        return true;
    }

    return false;
}

/*
 *      Get the index of the table for the given endpoint.
 *      Defaulting to "invalid" endpoint
 */
int EndpConfigTable::getEndpIndex(const char * endp)
{
    int endpIdx = (kEndpoints - 1); //default to invalid

    for (int i = 0; i < kEndpoints; i++)
    {
        if (strcmp (endp, endpConfigTable[i].endp) == 0)
        {
            endpIdx = i;
            break;
        }
    }

    return endpIdx;
}

bool EndpConfigTable::isHpVirtualizerOn(bool isJocOutput)
{
    struct dapConfig dapConf = endpConfigTable[mCurrEndpIdx].jocConf[mCurrDapState].dapConf;
    int outputMode = isJocOutput ? dapConf.jocOutputMode : dapConf.nonJocOutputMode;
    return (outputMode == DAP_CPDP_PROCESS_2_HEADPHONE);
}

/* Get DAP output mode based on channel count.
 * 
 * This method is expected to be called in non JOC case
 * and DAPv1 to take care virtualization.
 *
 */
int EndpConfigTable::getDapOutModeFromChannelCount(int channel)
{
    int outMode = DAP_CPDP_PROCESS_2;
    switch (channel)
    {
        case 6:
            outMode = DAP_CPDP_PROCESS_5_1;
            break;
        case 8:
            outMode = DAP_CPDP_PROCESS_7_1;
            break;
        case 2:
        default:
            outMode = (mCurrDapState == DAP_STATE_ON) ? DAP_CPDP_PROCESS_2_LTRT : DAP_CPDP_PROCESS_2;
            break;
    }
    return outMode;
}

int EndpConfigTable::getChannelCountFromDapOutMode(int dapOutMode)
{
    int channelCount = 2;
    switch (dapOutMode)
    {
        case DAP_CPDP_PROCESS_2:
        case DAP_CPDP_PROCESS_2_LTRT:
        case DAP_CPDP_PROCESS_2_HEADPHONE:
            channelCount = 2;
            break;
        case DAP_CPDP_PROCESS_5_1:
            channelCount = 6;
            break;
        case DAP_CPDP_PROCESS_7_1:
            channelCount = 8;
            break;
        default:
            channelCount = 2;
            break;
    }
    ALOGV("%s channelCount %d", __FUNCTION__, channelCount);
    return channelCount;
}

/* Get the maximum supported output channel count for the current endpoint
 *
 */
int EndpConfigTable::getMaxOutChannel(bool isDapEnabled, bool isJocOutput, int actualChannelCount)
{
    int maxOutChannel = 0;
    if (!isDapEnabled)
    {
        maxOutChannel = endpConfigTable[mCurrEndpIdx].jocConf[mCurrDapState].udcConf.udcMaxOutChannel;
    }
    else
    {
        struct dapConfig dapConf = getDapEndpConfig(isJocOutput, actualChannelCount);
        maxOutChannel = isJocOutput ? getChannelCountFromDapOutMode(dapConf.jocOutputMode) :
                                      getChannelCountFromDapOutMode(dapConf.nonJocOutputMode);                            
    }
    return maxOutChannel;                   
}

/* Get the desired DRC value for the current endpoint
 *
 */
int EndpConfigTable::getDRCmode()
{
    return endpConfigTable[mCurrEndpIdx].jocConf[mCurrDapState].udcConf.drcMode;
}

/* Get the desired Stereo mode for the current endpoint
 *
 */
int EndpConfigTable::getStereoMode()
{
    return endpConfigTable[mCurrEndpIdx].jocConf[mCurrDapState].udcConf.stereoMode;
}


/* Get the DAP configuration for the current endpoint
 *
 */
struct dapConfig EndpConfigTable::getDapEndpConfig(bool isJocOutput, int actualChannelCount)
{
    struct dapConfig dapConf = endpConfigTable[mCurrEndpIdx].jocConf[mCurrDapState].dapConf;
    
    if (!isReconfigOnEndpChange() &&  mInitTimeEndpIdx != -1)
    {
        // Intention is not to change output channel count during run time.
        struct dapConfig prevDapConf = endpConfigTable[mInitTimeEndpIdx].jocConf[mCurrDapState].dapConf;
        int prevJocOutChannelCount = getChannelCountFromDapOutMode(prevDapConf.jocOutputMode);
        int prevNonJocOutChannelCount = getChannelCountFromDapOutMode(prevDapConf.nonJocOutputMode);

        // Get the channel count for current output mode. If the count is different from init time, update the 
        // current output mode with init time value. Reason is if Audio Tear Down event does not occur on certain
        // endpoint change then AudioTrack expects data in the same order it was created. For example, if Headphone
        // is plugged-out while HDMI6 is connected, Audio Tear Down event does not occur. In this case we have to 
        // output data in 2 channels instead of current endpoint format.
        if (prevJocOutChannelCount != getChannelCountFromDapOutMode(dapConf.jocOutputMode))
        {
            dapConf.jocOutputMode = prevDapConf.jocOutputMode;            
        }

        if (prevNonJocOutChannelCount != getChannelCountFromDapOutMode(dapConf.nonJocOutputMode))
        {   
            dapConf.nonJocOutputMode = prevDapConf.nonJocOutputMode;
        }
    }

    if (!isJocOutput)
    {
        // In case of non-JOC output, chances are channel count in input stream may be less than output mode
        // configuration. In this case we have to re-configure output mode to match the channel count in input stream.
        int expectedChannelCount = getChannelCountFromDapOutMode(dapConf.nonJocOutputMode);
        if (actualChannelCount < expectedChannelCount)
        {
            ALOGV("%s actualChannelCount %d is less than expectedChannelCount %d", __FUNCTION__, actualChannelCount, expectedChannelCount);
            dapConf.nonJocOutputMode = getDapOutModeFromChannelCount(actualChannelCount);
        }
    }

    return dapConf;
}

/* Get JOC force downmix mode for the current endpoint
 * 
 */
int EndpConfigTable::getJocForceDownmixMode()
{
    if (strcmp(endpConfigTable[mCurrEndpIdx].endp, "headset") == 0)
    {
        if (mCurrDapState == DAP_STATE_ON)
        {
            // Disable Force downmix in case of headset and 
            // DAP_CPDP state is ON
            return 0;
        }
    }
    return 1;
}

}

