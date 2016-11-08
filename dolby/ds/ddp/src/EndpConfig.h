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
#ifndef _ENDP_CONFIG_
#define _ENDP_CONFIG_

namespace android {

/* For QMF, attenuation factor in UDC is -24dB. This is separate from -6dB headroom. 
  Headroom is compensated by DAP internally and hence we should inform 
  DAP only about this attenuation factor.*/
#define UDC_QMF_OUTPUT_LEVEL    (-24 * 16)
#define DAP_QMF_SYSTEM_GAIN     (UDC_QMF_OUTPUT_LEVEL * -1)

enum dapState
{
    DAP_STATE_OFF = 0,
    DAP_STATE_ON,
    DAP_STATE_COUNT
};

struct udcConfig
{
    int stereoMode;
    int drcMode;
    int udcMaxOutChannel;
};

struct dapConfig
{
    int jocOutputMode;
    int nonJocOutputMode;
    int jocDiaEnEnable;
    int nonJocDiaEnEnable;
    int volLevelerEnable;
    int volLevelerInTarget;
    int volLevelerOutTarget;
    int volMaxBoost;
};

struct jocConfig
{
    struct udcConfig    udcConf;
    struct dapConfig    dapConf; 
};

typedef struct endpConfig
{
    const char* endp;
    struct jocConfig jocConf[DAP_STATE_COUNT];
}endpConfig;

class EndpConfigTable
{
private:
    int mCurrEndpIdx;
    int mInitTimeEndpIdx;
    int mCurrDapState;
    bool mReconfigOnEndpChange;
   
    int  getEndpIndex(const char *endp);
    bool checkAndUpdateEndp();
    bool checkAndUpdateDapState();
    int  getChannelCountFromDapOutMode(int dapOutMode);
    int  getDapOutModeFromChannelCount(int channel);

public:
    EndpConfigTable();
    bool isConfigChanged();
    int getMaxOutChannel(bool isDapEnabled, bool isJocOutput, int actualChannelCount);
    int getDRCmode();
    int getStereoMode();
    struct dapConfig getDapEndpConfig(bool isJocOutput, int actualChannelCount);
    bool isHpVirtualizerOn(bool isJocOutput);
    int  getJocForceDownmixMode();
    inline void setReconfigOnEndpChange(bool activateReconfig) { mReconfigOnEndpChange = activateReconfig; }
    inline bool isReconfigOnEndpChange() { return mReconfigOnEndpChange == true; }
};


} // namespace android

#endif // _ENDP_CONFIG_

