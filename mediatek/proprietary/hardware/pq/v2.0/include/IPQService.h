
#ifndef __IPQSERVICE_H__
#define __IPQSERVICE_H__

#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <binder/BinderService.h>
#include "ddp_drv.h"


// Not all version supports Android runtime tuning.
// Runtime tuning is available only if this macro is defined.
#define MTK_PQ_RUNTIME_TUNING_SUPPORT


namespace android
{


enum
{
    SET_PQ_SHP_GAIN = 0,
    SET_PQ_SAT_GAIN,
    SET_PQ_LUMA_ADJ,
    SET_PQ_HUE_ADJ_SKIN,
    SET_PQ_HUE_ADJ_GRASS,
    SET_PQ_HUE_ADJ_SKY,
    SET_PQ_SAT_ADJ_SKIN,
    SET_PQ_SAT_ADJ_GRASS,
    SET_PQ_SAT_ADJ_SKY,
    SET_PQ_CONTRAST,
    SET_PQ_BRIGHTNESS
};


//
//  Holder service for pass objects between processes.
//
class IPQService : public IInterface
{
protected:
    enum {
        PQ_SET_INDEX = IBinder::FIRST_CALL_TRANSACTION,
        PQ_GET_COLOR_INDEX,
        PQ_GET_TDSHP_INDEX,
        PQ_GET_MAPPED_COLOR_INDEX,
        PQ_GET_MAPPED_TDSHP_INDEX,
        PQ_SET_MODE,
        PQ_SET_COLOR_REGION,
        PQ_GET_COLOR_REGION,
        PQ_GET_TDSHP_FLAG,
        PQ_SET_TDSHP_FLAG,
        PQ_SET_SCENARIO,
        PQ_GET_PQDC_INDEX,
        PQ_SET_PQDC_INDEX,
        PQ_GET_PQDS_INDEX,
        PQ_GET_COLOR_CAP,
        PQ_GET_TDSHP_REG,
        PQ_SET_FEATURE_SWITCH,
        PQ_GET_FEATURE_SWITCH,
    };

public:


    DECLARE_META_INTERFACE(PQService);
    virtual status_t getTDSHPReg(MDP_TDSHP_REG *param) = 0;
    virtual status_t getColorCapInfo(MDP_COLOR_CAP *param) = 0;
    virtual status_t getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index) = 0;
    virtual status_t setPQDCIndex(int32_t level, int32_t index) = 0;
    virtual status_t getPQDSIndex(DISP_PQ_DS_PARAM *dsparam) = 0;
    virtual status_t setTDSHPFlag(int32_t TDSHPFlag) = 0;
    virtual status_t getTDSHPFlag(int32_t *TDSHPFlag) = 0;
    virtual status_t getColorIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t  mode) = 0;
    virtual status_t getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode) = 0;
    virtual status_t getMappedColorIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t  mode) = 0;
    virtual status_t getTDSHPIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t  mode) = 0;
    virtual status_t setPQIndex(int32_t level, int32_t  scenario, int32_t  tuning_mode, int32_t index) = 0;
    virtual status_t setPQMode(int32_t mode) = 0;
    virtual status_t setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y) = 0;
    virtual status_t getColorRegion(DISP_PQ_WIN_PARAM *win_param) = 0;
    virtual status_t setDISPScenario(int32_t scenario) = 0;

    enum PQFeatureID{
        DISPLAY_COLOR,
        CONTENT_COLOR,
        CONTENT_COLOR_VIDEO,
        SHARPNESS,
        DYNAMIC_CONTRAST,
        DYNAMIC_SHARPNESS,
        DISPLAY_CCORR,
        DISPLAY_GAMMA,
        DISPLAY_OVER_DRIVE,
        PQ_FEATURE_MAX,
    };
    virtual status_t setFeatureSwitch(PQFeatureID id, uint32_t value) = 0;
    virtual status_t getFeatureSwitch(PQFeatureID id, uint32_t *value) = 0;

};

class BnPQService : public BnInterface<IPQService>
{
    virtual status_t onTransact(uint32_t code,
                                const Parcel& data,
                                Parcel* reply,
                                uint32_t flags = 0);
};

};

#endif




