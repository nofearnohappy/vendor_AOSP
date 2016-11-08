
#ifndef __PQ_SERVICE_CLIENT_H__
#define __PQ_SERVICE_CLIENT_H__

#include <stdint.h>
#include <sys/types.h>

#include <binder/Binder.h>
#include <utils/Singleton.h>
#include <utils/StrongPointer.h>
#include "IPQService.h"

namespace android {

class IPQService;
class PQClient : public Singleton<PQClient>
{
    friend class Singleton<PQClient>;

public:

    typedef enum PQ_SCENARIO_TYPE_ENUM
    {
        SCENARIO_UNKNOWN,
        SCENARIO_VIDEO,
        SCENARIO_PICTURE,
        SCENARIO_ISP_PREVIEW
    } PQ_SCENARIO_TYPE_ENUM;

    status_t getColorCapInfo(MDP_COLOR_CAP *param);
    status_t getTDSHPReg(MDP_TDSHP_REG *param);
    status_t getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index );
    status_t setPQDCIndex(int32_t level, int32_t index );
    status_t getPQDSIndex(DISP_PQ_DS_PARAM *dsparam);
    status_t getColorIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t  mode);
    status_t getTDSHPIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t mode);
    status_t getMappedColorIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t  mode);
    status_t getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t  scenario, int32_t mode);
    status_t setPQIndex(int32_t level, int32_t scenario, int32_t tuning_mode, int32_t index);
    status_t setPQMode(int32_t mode);
    status_t setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y);
    status_t getColorRegion(DISP_PQ_WIN_PARAM *win_param);
    status_t getTDSHPFlag(int32_t *TDSHPFlag);
    status_t setTDSHPFlag(int32_t TDSHPFlag);
    status_t setDISPScenario(int32_t scenario);
    status_t setFeatureSwitch(IPQService::PQFeatureID id, uint32_t value);
    status_t getFeatureSwitch(IPQService::PQFeatureID id, uint32_t *value);

private:
    PQClient();

    // DeathRecipient interface
    void serviceDied();

    status_t assertStateLocked() const;

    mutable Mutex mLock;
    mutable sp<IPQService> mPQService;
    mutable sp<IBinder::DeathRecipient> mDeathObserver;
};

};

#endif

