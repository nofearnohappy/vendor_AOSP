#ifndef __PQ_SERVICE_H__
#define __PQ_SERVICE_H__

#include <utils/threads.h>
#include "IPQService.h"

// HAL
#include <hardware/hardware.h>
#include <hardware/lights.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

namespace android
{
class PQService :
        public BinderService<PQService>,
        public BnPQService,
        public Thread
{
    friend class BinderService<PQService>;
public:
    PQService();
    ~PQService();

    static char const* getServiceName() { return "PQ"; }

    typedef enum PQ_SCENARIO_TYPE_ENUM
    {
        SCENARIO_UNKNOWN,
        SCENARIO_VIDEO,
        SCENARIO_PICTURE,
        SCENARIO_ISP_PREVIEW
    } PQ_SCENARIO_TYPE_ENUM;

    // IPQService interface

    virtual status_t dump(int fd, const Vector<String16>& args);
    virtual status_t setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y);
    virtual status_t getColorRegion(DISP_PQ_WIN_PARAM *win_param);
    virtual status_t setPQMode(int32_t mode);
    virtual status_t getTDSHPFlag(int32_t *TDSHPFlag);
    virtual status_t setTDSHPFlag(int32_t TDSHPFlag);
    virtual status_t getMappedColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode);
    virtual status_t getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode);
    virtual status_t getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index);
    virtual status_t getPQDSIndex(DISP_PQ_DS_PARAM *dsparam);
    virtual status_t setPQDCIndex(int32_t level, int32_t index);
    virtual status_t getColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode);
    virtual status_t getTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode);
    virtual status_t setPQIndex(int32_t level, int32_t  scenario, int32_t  tuning_mode, int32_t index);
    virtual status_t setDISPScenario(int32_t scenario);
    virtual status_t getColorCapInfo(MDP_COLOR_CAP *param);
    virtual status_t getTDSHPReg(MDP_TDSHP_REG *param);
    virtual status_t setFeatureSwitch(IPQService::PQFeatureID id, uint32_t value);
    virtual status_t getFeatureSwitch(IPQService::PQFeatureID id, uint32_t *value);


private:

    status_t enableDisplayColor(uint32_t value);
    status_t enableContentColorVideo(uint32_t value);
    status_t enableSharpness(uint32_t value);
    status_t enableDynamicContrast(uint32_t value);
    status_t enableDynamicSharpness(uint32_t value);
    status_t enableDisplayGamma(uint32_t value);
    status_t enableDisplayOverDrive(uint32_t value);

    int _getLcmIndexOfGamma();
    void _setGammaIndex(int index);
    void configGamma(int picMode);
    status_t configCcorrCoef(int32_t coefTableIdx);

    // The value should be the same as the PQFeatureID enum in IPQService
    enum PQFeatureID {
        DISPLAY_COLOR,
        CONTENT_COLOR,
        CONTENT_COLOR_VIDEO,
        SHARPNESS,
        DYNAMIC_CONTRAST,
        DYNAMIC_SHARPNESS,
        DISPLAY_GAMMA,
        DISPLAY_OVER_DRIVE,
        PQ_FEATURE_MAX,
    };

    virtual void onFirstRef();
    virtual status_t readyToRun();
    bool initDriverRegs();
    virtual bool threadLoop();
    unsigned int remapCcorrIndex(unsigned int ccorrindex);
    mutable Mutex mLock;
    unsigned int mEventFlags;
    bool mCcorrDebug;
    DISP_PQ_WIN_PARAM mdp_win_param;
    DISP_PQ_PARAM pqparam;    /* for temp pqparam calculation or ioctl */
    DISP_PQ_DC_PARAM pqdcparam;
    DISP_PQ_DS_PARAM pqdsparam;
    DISPLAY_PQ_T pqindex;
    DISPLAY_TDSHP_T tdshpindex;
    gamma_entry_t m_CustGamma[GAMMA_LCM_MAX][GAMMA_INDEX_MAX];
    int32_t PQMode;
    int32_t PQScenario;
    int32_t MDP_TDSHP_FLAG;
    int32_t m_drvID;
    uint32_t m_bFeatureSwitch[PQ_FEATURE_MAX];

};
};

#endif

