#define LOG_TAG "PQ"
#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <cutils/properties.h>


/*PQService */
#include <dlfcn.h>
#include <math.h>
#include <utils/SortedVector.h>
#include <binder/PermissionCache.h>
#include <android/native_window.h>
#include <gui/ISurfaceComposer.h>
#include <gui/SurfaceComposerClient.h>
#include <ui/DisplayInfo.h>
#include <cutils/memory.h>

 /*PQService */
#include <linux/disp_session.h>

#include "ddp_drv.h"
#include "cust_gamma.h"
#include "cust_color.h"
#include "cust_tdshp.h"
#include <dlfcn.h>
/*PQService */
#include <binder/BinderService.h>
#include <PQService.h>
/*PQService */
namespace android {

#define DISP_DEV_NODE_PATH "/dev/mtk_disp_mgr"

#define PQ_LOGD(fmt, arg...) ALOGD(fmt, ##arg)
#define PQ_LOGE(fmt, arg...) ALOGE(fmt, ##arg)

#define max( a, b )            (((a) > (b)) ? (a) : (b))

/*cust PQ default setting*/


static DISP_PQ_MAPPING_PARAM pqparam_mapping =
{
    image:80,
    video:100,
    camera:20,
} ;

static DISP_PQ_PARAM pqparam_table[PQ_PARAM_TABLE_SIZE]=
{
    //std_image
    {
        u4SHPGain:2,
        u4SatGain:4,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{0,6,10,10},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //std_video
    {
        u4SHPGain:3,
        u4SatGain:4,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{0,6,12,12},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //std_camera
    {
        u4SHPGain:2,
        u4SatGain:4,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{0,6,10,10},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //viv_image
    {
        u4SHPGain:2,
        u4SatGain:9,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{16,16,16,16},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //viv_video
    {
        u4SHPGain:3,
        u4SatGain:9,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{16,16,18,18},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //viv_camera
    {
        u4SHPGain:2,
        u4SatGain:4,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{0,6,10,10},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    },

    //pqparam_usr
    {
        u4SHPGain:2,
        u4SatGain:9,
        u4PartialY:0,
        u4HueAdj:{9,9,12,12},
        u4SatAdj:{16,16,16,16},
        u4Contrast:4,
        u4Brightness:4,
        u4Ccorr:0
    }
};
PQService::PQService()
{
    PQ_LOGD("[PQ_SERIVCE] PQService constructor");

    m_drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ_SERVICE] open device fail!!");
    }

    // set all feature default off
    memset(m_bFeatureSwitch, 0, sizeof(uint32_t) * PQ_FEATURE_MAX);

    // update it by each feature
#ifndef DISP_COLOR_OFF
    m_bFeatureSwitch[DISPLAY_COLOR] = 1;
#endif
#ifdef MDP_COLOR_ENABLE
    m_bFeatureSwitch[CONTENT_COLOR] = 1;
#endif
    m_bFeatureSwitch[DYNAMIC_SHARPNESS] = 1;

}

PQService::~PQService()
{
    close(m_drvID);
}

status_t PQService::getColorRegion(DISP_PQ_WIN_PARAM *win_param)
{
    Mutex::Autolock _l(mLock);
    //for MDP split demo window, MDP should update this per frame???
    win_param->split_en = mdp_win_param.split_en;
    win_param->start_x = mdp_win_param.start_x;
    win_param->start_y = mdp_win_param.start_y;
    win_param->end_x = mdp_win_param.end_x;
    win_param->end_y = mdp_win_param.end_y;
    return NO_ERROR;

}

status_t PQService::setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y)
{
    Mutex::Autolock _l(mLock);
#ifndef DISP_COLOR_OFF

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ_SERVICE] open device fail!!");
        return UNKNOWN_ERROR ;
    }
    mdp_win_param.split_en = split_en;
    mdp_win_param.start_x = start_x;
    mdp_win_param.start_y = start_y;
    mdp_win_param.end_x = end_x;
    mdp_win_param.end_y = end_y;
    ioctl(m_drvID, DISP_IOCTL_PQ_SET_WINDOW, &mdp_win_param);

    //for MDP split demo window, MDP should update this per frame???
#endif
    return NO_ERROR;

}

status_t PQService::setPQMode(int32_t mode)
{
    Mutex::Autolock _l(mLock);
    PQMode = mode;
    char value[PROPERTY_VALUE_MAX];
    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", mode);
    property_set(PQ_PIC_MODE_PROPERTY_STR, value);
    PQ_LOGD("[PQ_SERVICE] property set... picture mode[%d]", mode);
#ifndef DISP_COLOR_OFF
    int i ;
    int percentage = pqparam_mapping.image;  //default scenario = image

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ_SERVICE] open device fail!!");
        return UNKNOWN_ERROR ;
    }


    if (mode == PQ_PIC_MODE_STANDARD || mode == PQ_PIC_MODE_VIVID)
    {

        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4SatGain, pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4Contrast, pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4Brightness);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4HueAdj[0], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4HueAdj[1], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4HueAdj[2], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4HueAdj[3]);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4SatAdj[0], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4SatAdj[1], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4SatAdj[2], pqparam_table[(mode) * PQ_SCENARIO_COUNT + 1].u4SatAdj[3]);

        pqparam.u4SatGain = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SatGain;
        pqparam.u4PartialY = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4PartialY;
        pqparam.u4HueAdj[0] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[0];
        pqparam.u4HueAdj[1] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[1];
        pqparam.u4HueAdj[2] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[2];
        pqparam.u4HueAdj[3] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[3];
        pqparam.u4SatAdj[0] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[1];
        pqparam.u4SatAdj[2] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[2];
        pqparam.u4SatAdj[3] = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[3];
        pqparam.u4Contrast = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4Contrast;
        pqparam.u4Brightness = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4Brightness;
        pqparam.u4SHPGain = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4SHPGain;
        pqparam.u4Ccorr = pqparam_table[(mode) * PQ_SCENARIO_COUNT + 0].u4Ccorr;
        ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        configCcorrCoef(pqparam.u4Ccorr);


    }
    else if (mode == PQ_PIC_MODE_USER_DEF)
    {
        // USER MODE
        //memcpy(&pqparam_user_def, &pqparam_usr, sizeof(pqparam_usr));   // default value from standard setting.

        property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... tdshp[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SHPGain = i;

        property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... gsat[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatGain = i;

        property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... contrast[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Contrast = i;

        property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... pic bright[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Brightness = i;

        pqparam.u4SatGain   = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatGain * percentage / 100;
        pqparam.u4PartialY  = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4PartialY;
        pqparam.u4HueAdj[0] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[0];
        pqparam.u4HueAdj[1] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[1];
        pqparam.u4HueAdj[2] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[2];
        pqparam.u4HueAdj[3] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[3];
        pqparam.u4SatAdj[0] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[1] * percentage / 100;
        pqparam.u4SatAdj[2] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[2] * percentage / 100;
        pqparam.u4SatAdj[3] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[3] * percentage / 100;
        pqparam.u4Contrast = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Contrast * percentage / 100;
        pqparam.u4Brightness = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Brightness * percentage / 100;
        pqparam.u4SHPGain = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SHPGain * percentage / 100;
        pqparam.u4Ccorr = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Ccorr;
        ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam );
        configCcorrCoef(pqparam.u4Ccorr);

        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, shp[%d], gsat[%d], cont[%d], bri[%d] ", pqparam.u4SHPGain, pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);

    }
    else
    {
        PQ_LOGE("[PQ_SERVICE] unknown picture mode!!");
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_table[0].u4SatGain, pqparam_table[0].u4Contrast, pqparam_table[0].u4Brightness);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_table[0].u4HueAdj[0], pqparam_table[0].u4HueAdj[1], pqparam_table[0].u4HueAdj[2], pqparam_table[0].u4HueAdj[3]);
        PQ_LOGD("[PQ_SERVICE] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_table[0].u4SatAdj[0], pqparam_table[0].u4SatAdj[1], pqparam_table[0].u4SatAdj[2], pqparam_table[0].u4SatAdj[3]);

        pqparam.u4SatGain   = pqparam_table[0].u4SatGain;
        pqparam.u4PartialY  = pqparam_table[0].u4PartialY;
        pqparam.u4HueAdj[0] = pqparam_table[0].u4HueAdj[0];
        pqparam.u4HueAdj[1] = pqparam_table[0].u4HueAdj[1];
        pqparam.u4HueAdj[2] = pqparam_table[0].u4HueAdj[2];
        pqparam.u4HueAdj[3] = pqparam_table[0].u4HueAdj[3];
        pqparam.u4SatAdj[0] = pqparam_table[0].u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam_table[0].u4SatAdj[1];
        pqparam.u4SatAdj[2] = pqparam_table[0].u4SatAdj[2];
        pqparam.u4SatAdj[3] = pqparam_table[0].u4SatAdj[3];
        pqparam.u4Contrast = pqparam_table[0].u4Contrast;
        pqparam.u4Brightness = pqparam_table[0].u4Brightness;
        pqparam.u4SHPGain = pqparam_table[0].u4SHPGain;
        pqparam.u4Ccorr = pqparam_table[0].u4Ccorr;
        ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam );
        configCcorrCoef(pqparam.u4Ccorr);
    }
#endif



    return NO_ERROR;
}

status_t PQService::setPQIndex(int32_t level, int32_t  scenario, int32_t  tuning_mode, int32_t index)
{
    Mutex::Autolock _l(mLock);
    char value[PROPERTY_VALUE_MAX];
    int percentage;
    DISP_PQ_PARAM *pqparam_image_ptr;
    DISP_PQ_PARAM *pqparam_video_ptr;

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ_SERVICE] open device fail!!");
        return UNKNOWN_ERROR ;
    }

    PQScenario =scenario;

    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){
        pqparam_image_ptr=&pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0];
        pqparam_video_ptr=&pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1];
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        pqparam_image_ptr=&pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT];
        pqparam_video_ptr=&pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT];
    }
    else{
        PQ_LOGE("[PQ_SERIVCE] PQService : Unknown PQMode\n");
        pqparam_image_ptr=&pqparam_table[0];
        pqparam_video_ptr=&pqparam_table[1];
    }

    // assume scenario is image mode, DISP is always on
    if(PQScenario == SCENARIO_PICTURE){
        percentage = pqparam_mapping.image;
    }
    else  if(PQScenario == SCENARIO_VIDEO){
        percentage = pqparam_mapping.video;
    }
    else  if(PQScenario == SCENARIO_ISP_PREVIEW){
        percentage = pqparam_mapping.camera;
    }
    else{
        percentage = pqparam_mapping.image;
        PQ_LOGE("[PQ_SERIVCE] PQService : getColorIndex, invalid scenario\n");
    }
    PQ_LOGD("[PQ_SERIVCE] PQService : PQMode = %d, PQScenario = %d, level = %d, percentage = %d\n",PQMode,PQScenario,level,percentage);

    ioctl(m_drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    switch (index) {
        case SET_PQ_SHP_GAIN:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4SHPGain = level * percentage / 100;
                }
                else{
                    pqparam.u4SHPGain = level;
                }
                pqparam_image_ptr->u4SHPGain = level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_SHP_GAIN...[%d]\n", pqparam.u4SHPGain);
            }
            break;
        case SET_PQ_SAT_GAIN:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4SatGain = level * percentage / 100;
                }
                else{
                    pqparam.u4SatGain = level;
                }
                pqparam_image_ptr->u4SatGain = level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_SAT_GAIN...[%d]\n", pqparam.u4SatGain);
            }
            break;
        case SET_PQ_LUMA_ADJ:
            {
                pqparam.u4PartialY= (level);
                pqparam_image_ptr->u4PartialY= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_LUMA_ADJ...[%d]\n",pqparam.u4PartialY);
            }
            break;
        case  SET_PQ_HUE_ADJ_SKIN:
            {
                pqparam.u4HueAdj[1]= (level) ;
                pqparam_image_ptr->u4HueAdj[1]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_HUE_ADJ_SKIN...[%d]\n",pqparam.u4HueAdj[1]);
            }
            break;
        case  SET_PQ_HUE_ADJ_GRASS:
            {
                pqparam.u4HueAdj[2] = level;
                pqparam_image_ptr->u4HueAdj[2]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_HUE_ADJ_GRASS...[%d]\n",pqparam.u4HueAdj[2]);
            }
            break;
        case  SET_PQ_HUE_ADJ_SKY:
            {
                pqparam.u4HueAdj[3] = level;
                pqparam_image_ptr->u4HueAdj[3]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_HUE_ADJ_SKY...[%d]\n",pqparam.u4HueAdj[3]);
            }
            break;
        case SET_PQ_SAT_ADJ_SKIN:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4SatAdj[1]= (level) * percentage / 100;
                }
                else{
                    pqparam.u4SatAdj[1] = level;
                }
                pqparam_image_ptr->u4SatAdj[1]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_SAT_ADJ_SKIN...[%d]\n",pqparam.u4SatAdj[1]);
            }
            break;
        case SET_PQ_SAT_ADJ_GRASS:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4SatAdj[2]= (level) * percentage / 100;
                }
                else{
                    pqparam.u4SatAdj[2] = level;
                }
                pqparam_image_ptr->u4SatAdj[2]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_SAT_ADJ_GRASS...[%d]\n",pqparam.u4SatAdj[2] );
            }
            break;
        case SET_PQ_SAT_ADJ_SKY:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4SatAdj[3]= (level) * percentage / 100;
                }
                else{
                    pqparam.u4SatAdj[3] = level;
                }
                pqparam_image_ptr->u4SatAdj[3]= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_SAT_ADJ_GRASS...[%d]\n",pqparam.u4SatAdj[3] );
            }
            break;
        case SET_PQ_CONTRAST:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4Contrast= (level) * percentage / 100 ;
                }
                else{
                    pqparam.u4Contrast= level;
                }
                pqparam_image_ptr->u4Contrast= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_CONTRAST...[%d]\n", pqparam.u4Contrast);
            }
            break;
        case SET_PQ_BRIGHTNESS:
            {
                if (PQMode == PQ_PIC_MODE_USER_DEF){
                    pqparam.u4Brightness= (level) * percentage / 100 ;
                }
                else{
                    pqparam.u4Brightness= level;
                }
                pqparam_image_ptr->u4Brightness= level;
                PQ_LOGD("[PQ_SERIVCE] setPQIndex SET_PQ_BRIGHTNESS...[%d]\n", pqparam.u4Brightness);
            }
            break;
        default:
            PQ_LOGD("[PQ_SERIVCE] setPQIndex default case...\n");
    }
    // if in Gallery PQ tuning mode, sync video param with image param
    if(tuning_mode == TDSHP_FLAG_TUNING)
    {
        *pqparam_video_ptr=*pqparam_image_ptr;
    }
    ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);

    return NO_ERROR;
}

status_t PQService::getMappedColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    Mutex::Autolock _l(mLock);

    PQScenario = scenario;
    int percentage = 0;
    //scenario will be passed from MDP, return corresponding param of scenario automatically

    PQ_LOGD("[PQ_SERIVCE] PQService : PQScenario = %d, PQMode = %d\n",PQScenario,PQMode);
    // mdp_pqparam_standard/vivid will be added

    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){

        if(PQScenario ==  SCENARIO_PICTURE){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_VIDEO){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 2], sizeof(DISP_PQ_PARAM));
        }
        else{
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedColorIndex, invalid scenario\n");
        }
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        memcpy(&pqparam, &pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT], sizeof(DISP_PQ_PARAM));
        if(PQScenario ==  SCENARIO_PICTURE){
            percentage = pqparam_mapping.image;
        }
        else  if(PQScenario == SCENARIO_VIDEO){
            percentage = pqparam_mapping.video;
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
            percentage = pqparam_mapping.camera;
        }
        else{
        percentage = pqparam_mapping.image;
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedColorIndex, invalid scenario\n");
        }
    }
    else{
        memcpy(&pqparam, &pqparam_table[0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedColorIndex, invalid mode\n");
    }

    if (PQMode == PQ_PIC_MODE_USER_DEF){
        index->u4SatGain = pqparam.u4SatGain * percentage / 100;
        index->u4PartialY = pqparam.u4PartialY;
        index->u4HueAdj[0]= (pqparam.u4HueAdj[0]);
        index->u4HueAdj[1]= (pqparam.u4HueAdj[1]) ;
        index->u4HueAdj[2]= (pqparam.u4HueAdj[2]) ;
        index->u4HueAdj[3]= (pqparam.u4HueAdj[3]) ;
        index->u4SatAdj[0] = pqparam.u4SatAdj[0];
        index->u4SatAdj[1] = pqparam.u4SatAdj[1] * percentage / 100;
        index->u4SatAdj[2] = pqparam.u4SatAdj[2] * percentage / 100;
        index->u4SatAdj[3] = pqparam.u4SatAdj[3] * percentage / 100;
        index->u4Contrast = pqparam.u4Contrast * percentage / 100;
        index->u4Brightness = pqparam.u4Brightness * percentage / 100;
    }
    else{
        index->u4SatGain= pqparam.u4SatGain ;
        index->u4PartialY = pqparam.u4PartialY;
        index->u4HueAdj[0]= (pqparam.u4HueAdj[0]);
        index->u4HueAdj[1]= (pqparam.u4HueAdj[1]);
        index->u4HueAdj[2]= (pqparam.u4HueAdj[2]);
        index->u4HueAdj[3]= (pqparam.u4HueAdj[3]);
        index->u4SatAdj[0] = pqparam.u4SatAdj[0];
        index->u4SatAdj[1] = pqparam.u4SatAdj[1];
        index->u4SatAdj[2] = pqparam.u4SatAdj[2];
        index->u4SatAdj[3] = pqparam.u4SatAdj[3];
        index->u4Contrast = pqparam.u4Contrast ;
        index->u4Brightness = pqparam.u4Brightness ;
    }

    return NO_ERROR;
}

status_t PQService::getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{

    Mutex::Autolock _l(mLock);
    PQScenario = scenario;
    int percentage = 0;

    PQ_LOGD("[PQ_SERIVCE] PQService : PQScenario = %d, PQMode = %d\n",PQScenario,PQMode);

    //scenario will be passed from MDP, return corresponding param of scenario automatically
    // mdp_pqparam_standard/vivid will be added
    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){

        if(PQScenario ==  SCENARIO_PICTURE){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_VIDEO){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 2], sizeof(DISP_PQ_PARAM));
        }
        else{
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedTDSHPIndex, invalid scenario\n");
        }
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        memcpy(&pqparam, &pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT], sizeof(DISP_PQ_PARAM));
        if(PQScenario ==  SCENARIO_PICTURE){
            percentage = pqparam_mapping.image;
        }
        else  if(PQScenario == SCENARIO_VIDEO){
            percentage = pqparam_mapping.video;
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
            percentage = pqparam_mapping.camera;
        }
        else{
        percentage = pqparam_mapping.image;
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedTDSHPIndex, invalid scenario\n");
        }
    }
    else{
        memcpy(&pqparam, &pqparam_table[0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getMappedTDSHPIndex, invalid mode\n");
    }

    if (PQMode == PQ_PIC_MODE_USER_DEF){
        index->u4SHPGain= pqparam.u4SHPGain * percentage / 100;
    }
    else{
        index->u4SHPGain= pqparam.u4SHPGain;
    }

    return NO_ERROR;
}

status_t PQService::setPQDCIndex(int32_t level, int32_t index)
{
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] open /proc/mtk_mira fail...");
        return UNKNOWN_ERROR;
    }

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_DC_PARAM, &pqdcparam);

    pqdcparam.param[index] = level;

    ioctl(m_drvID, DISP_IOCTL_PQ_SET_DC_PARAM, &pqdcparam);

    return NO_ERROR;
}

status_t PQService::getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index)
 {
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] open /proc/mtk_mira fail...");
        return UNKNOWN_ERROR ;
    }

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_DC_PARAM, &pqdcparam);

    memcpy(dcparam, &pqdcparam, sizeof(pqdcparam));

    return NO_ERROR;
}
status_t PQService::getColorCapInfo(MDP_COLOR_CAP *param)
 {
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] open /proc/mtk_mira fail...");
        return UNKNOWN_ERROR ;
    }

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_MDP_COLOR_CAP, param);


    return NO_ERROR;
}

status_t PQService::getTDSHPReg(MDP_TDSHP_REG *param)
 {
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] open /proc/mtk_mira fail...");
        return UNKNOWN_ERROR ;
    }

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_MDP_TDSHP_REG, param);

    return NO_ERROR;
}

status_t PQService::getPQDSIndex(DISP_PQ_DS_PARAM *dsparam)
 {
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] open /proc/mtk_mira fail...");
        return UNKNOWN_ERROR ;
    }

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_DS_PARAM, &pqdsparam);

    memcpy(dsparam, &pqdsparam, sizeof(pqdsparam));

    //PQ_LOGD("[PQ_SERIVCE] dsparam.param[DS_en] %d\n",dsparam->param[DS_en]);
    //PQ_LOGD("[PQ_SERIVCE] dsparam.param[iCorThr_clip0] %d\n",dsparam->param[iCorThr_clip0]);
    //PQ_LOGD("[PQ_SERIVCE] dsparam.param[iGain_clip0] %d\n",dsparam->param[iGain_clip0]);


    return NO_ERROR;
}
status_t PQService::getColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    Mutex::Autolock _l(mLock);
// the statements in #ifndef DISP_COLOR_OFF are used when DISP only


    PQScenario = scenario;
    int percentage;
    //scenario will be passed from MDP, return corresponding param of scenario automatically

    // mdp_pqparam_standard/vivid will be added

    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){

        if(PQScenario ==  SCENARIO_PICTURE){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_VIDEO){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 2], sizeof(DISP_PQ_PARAM));
        }
        else{
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getColorIndex, invalid scenario\n");
        }
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        memcpy(&pqparam, &pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT], sizeof(DISP_PQ_PARAM));
    }
    else{
        memcpy(&pqparam, &pqparam_table[0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getColorIndex, invalid mode\n");
    }

    index->u4SatGain= pqparam.u4SatGain;
    index->u4PartialY = pqparam.u4PartialY;
    index->u4HueAdj[0]= pqparam.u4HueAdj[0];
    index->u4HueAdj[1]= pqparam.u4HueAdj[1];
    index->u4HueAdj[2]= pqparam.u4HueAdj[2];
    index->u4HueAdj[3]= pqparam.u4HueAdj[3];
    index->u4SatAdj[0] = pqparam.u4SatAdj[0];
    index->u4SatAdj[1] = pqparam.u4SatAdj[1];
    index->u4SatAdj[2] = pqparam.u4SatAdj[2];
    index->u4SatAdj[3] = pqparam.u4SatAdj[3];
    index->u4Contrast = pqparam.u4Contrast;
    index->u4Brightness = pqparam.u4Brightness;

    return NO_ERROR;

}

status_t PQService::getTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{

    Mutex::Autolock _l(mLock);
    PQScenario = scenario;
    //scenario will be passed from MDP, return corresponding param of scenario automatically
    // mdp_pqparam_standard/vivid will be added
    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){

        if(PQScenario ==  SCENARIO_PICTURE){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_VIDEO){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 2], sizeof(DISP_PQ_PARAM));
        }
        else{
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getTDSHPIndex, invalid scenario\n");
        }
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        memcpy(&pqparam, &pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT], sizeof(DISP_PQ_PARAM));
    }
    else{
        memcpy(&pqparam, &pqparam_table[0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : getTDSHPIndex, invalid mode\n");
    }

    index->u4SHPGain= pqparam.u4SHPGain;

    return NO_ERROR;
}
status_t PQService::getTDSHPFlag(int32_t *TDSHPFlag)
{
    Mutex::Autolock _l(mLock);

    ioctl(m_drvID, DISP_IOCTL_PQ_GET_TDSHP_FLAG, TDSHPFlag);
    PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_PQ_GET_TDSHP_FLAG()... tuning flag[%d]", *TDSHPFlag);

    return NO_ERROR;
}

status_t PQService::setTDSHPFlag(int32_t TDSHPFlag)
{
    Mutex::Autolock _l(mLock);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ] setTDSHPFlag(), open dev fail...");
    }

    PQ_LOGD("[PQ_SERIVCE] setTuningFlag[%d]", TDSHPFlag);

    ioctl(m_drvID, DISP_IOCTL_PQ_SET_TDSHP_FLAG, &TDSHPFlag);

    return NO_ERROR;
}

status_t PQService::configCcorrCoef(int32_t coefTableIdx)
{
#ifndef CCORR_OFF
    DISP_CCORR_COEF_T ccorr;
    int ret = -1;
    unsigned int coef_sum = 0;
    PQ_LOGD("ccorr table index=%d", coefTableIdx);
    ccorr.hw_id = DISP_CCORR0;

    if (mCcorrDebug == true) { /* scenario debug mode */
        for (int y = 0; y < 3; y += 1) {
            for (int x = 0; x < 3; x += 1) {
                ccorr.coef[y][x] = 0;
            }
        }
        int index = PQMode;
        if (PQScenario ==  SCENARIO_PICTURE)
            index += 0;
        else if (PQScenario ==  SCENARIO_VIDEO)
            index += 1;
        else
            index += 2;
        index = index % 3;
        ccorr.coef[index][index] = 1024;
        coef_sum = 1024;

    } else { /* normal mode */
        for (int y = 0; y < 3; y += 1) {
            for (int x = 0; x < 3; x += 1) {
                ccorr.coef[y][x] = pqindex.CCORR_COEF[coefTableIdx][y][x];
                coef_sum += ccorr.coef[y][x];
            }
        }
    }

    if (coef_sum == 0) { /* zero coef protect */
        ccorr.coef[0][0] = 1024;
        ccorr.coef[1][1] = 1024;
        ccorr.coef[2][2] = 1024;
        PQ_LOGD("ccorr coef all zero, prot on");
    }

    ret = ioctl(m_drvID, DISP_IOCTL_SET_CCORR, &ccorr);
    if (ret == -1)
        PQ_LOGD("ccorr ioctl fail");

#endif
    return NO_ERROR;
}
 status_t  PQService::setDISPScenario(int32_t scenario)
{
    Mutex::Autolock _l(mLock);
    PQScenario = scenario;
#ifndef DISP_COLOR_OFF
    int percentage = 0;

    if (m_drvID < 0)
        return UNKNOWN_ERROR;

    PQ_LOGD("[PQ_SERIVCE] PQService : PQScenario = %d, PQMode = %d\n",PQScenario,PQMode);
    // mdp_pqparam_standard/vivid will be added

    if(PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID){

        if(PQScenario ==  SCENARIO_PICTURE){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_VIDEO){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 1], sizeof(DISP_PQ_PARAM));
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 2], sizeof(DISP_PQ_PARAM));
        }
        else{
             memcpy(&pqparam, &pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : setDISPScenario, invalid scenario\n");
        }
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF){
        memcpy(&pqparam, &pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT], sizeof(DISP_PQ_PARAM));
        if(PQScenario ==  SCENARIO_PICTURE){
            percentage = pqparam_mapping.image;
        }
        else  if(PQScenario == SCENARIO_VIDEO){
            percentage = pqparam_mapping.video;
        }
        else  if(PQScenario == SCENARIO_ISP_PREVIEW){
            percentage = pqparam_mapping.camera;
        }
        else{
        percentage = pqparam_mapping.image;
        PQ_LOGD("[PQ_SERIVCE] PQService : setDISPScenario, invalid scenario\n");
        }
    }
    else{
        memcpy(&pqparam, &pqparam_table[0], sizeof(DISP_PQ_PARAM));
        PQ_LOGD("[PQ_SERIVCE] PQService : setDISPScenario, invalid mode\n");
    }

    if (PQMode == PQ_PIC_MODE_USER_DEF){
        pqparam.u4SatGain= pqparam.u4SatGain * percentage / 100;
        pqparam.u4PartialY = pqparam.u4PartialY;
        pqparam.u4HueAdj[0]= (pqparam.u4HueAdj[0]);
        pqparam.u4HueAdj[1]= (pqparam.u4HueAdj[1]) ;
        pqparam.u4HueAdj[2]= (pqparam.u4HueAdj[2]) ;
        pqparam.u4HueAdj[3]= (pqparam.u4HueAdj[3]) ;
        pqparam.u4SatAdj[0] = pqparam.u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam.u4SatAdj[1] * percentage / 100;
        pqparam.u4SatAdj[2] = pqparam.u4SatAdj[2] * percentage / 100;
        pqparam.u4SatAdj[3] = pqparam.u4SatAdj[3] * percentage / 100;
        pqparam.u4Contrast = pqparam.u4Contrast * percentage / 100;
        pqparam.u4Brightness = pqparam.u4Brightness * percentage / 100;
        pqparam.u4SHPGain = pqparam.u4SHPGain * percentage / 100;
        pqparam.u4Ccorr= pqparam.u4Ccorr;
    }

    ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    configCcorrCoef(pqparam.u4Ccorr);
#endif

    return NO_ERROR;
}



status_t PQService::setFeatureSwitch(IPQService::PQFeatureID id, uint32_t value)
{
    Mutex::Autolock _l(mLock);

    uint32_t featureID = static_cast<PQFeatureID>(id);
    status_t ret = NO_ERROR;


    PQ_LOGD("[PQ_SERVICE] setFeatureSwitch(), feature[%d], value[%d]", featureID, value);

    switch (featureID) {
    case DISPLAY_COLOR:
        enableDisplayColor(value);
        break;
    case CONTENT_COLOR:
        // not implemented
        PQ_LOGE("[PQ_SERVICE] setFeatureSwitch(), CONTENT_COLOR not implemented!!");
        ret = UNKNOWN_ERROR;
        break;
    case CONTENT_COLOR_VIDEO:
        enableContentColorVideo(value);
        break;
    case SHARPNESS:
        enableSharpness(value);
        break;
    case DYNAMIC_CONTRAST:
        enableDynamicContrast(value);
        break;
    case DYNAMIC_SHARPNESS:
        enableDynamicSharpness(value);
        break;
    case DISPLAY_GAMMA:
        enableDisplayGamma(value);
        break;
    case DISPLAY_OVER_DRIVE:
        enableDisplayOverDrive(value);
        break;

    default:
        PQ_LOGE("[PQ_SERVICE] setFeatureSwitch(), feature[%d] is not implemented!!", id);
        ret = UNKNOWN_ERROR;
        break;
    }
    return ret;
}

status_t PQService::getFeatureSwitch(IPQService::PQFeatureID id, uint32_t *value)
{
    Mutex::Autolock _l(mLock);

    uint32_t featureID = static_cast<PQFeatureID>(id);

    if (featureID < PQ_FEATURE_MAX)
    {
        *value = m_bFeatureSwitch[featureID];
        PQ_LOGD("[PQ_SERVICE] getFeatureSwitch(), feature[%d], value[%d]", featureID, *value);

        return NO_ERROR;
    }
    else
    {
        *value = 0;
        PQ_LOGE("[PQ_SERVICE] getFeatureSwitch(), unsupported feature[%d]", featureID);

        return UNKNOWN_ERROR;
    }
}

status_t PQService::enableDisplayColor(uint32_t value)
{
#ifndef DISP_COLOR_OFF
    int bypass;
    PQ_LOGD("[PQ_SERVICE] enableDisplayColor(), enable[%d]", value);

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQ_SERVICE] open device fail!!");
        return UNKNOWN_ERROR ;

    }

    //  set bypass COLOR to disp driver.
    if (value > 0)
    {
        bypass = 0;
        ioctl(m_drvID, DISP_IOCTL_PQ_SET_BYPASS_COLOR, &bypass);
    }
    else
    {
        bypass = 1;
        ioctl(m_drvID, DISP_IOCTL_PQ_SET_BYPASS_COLOR, &bypass);
    }
    PQ_LOGD("[PQService] enableDisplayColor[%d]", value);

    m_bFeatureSwitch[DISPLAY_COLOR] = value;
#endif

    return NO_ERROR;
}

status_t PQService::enableContentColorVideo(uint32_t value)
{
#ifdef MDP_COLOR_ENABLE
    char pvalue[PROPERTY_VALUE_MAX];
    int ret;

    snprintf(pvalue, PROPERTY_VALUE_MAX, "%d\n", value);
    ret = property_set(PQ_MDP_COLOR_EN_STR  , pvalue);
    PQ_LOGD("[PQService] enableContentColorVideo[%d]", value);

    m_bFeatureSwitch[CONTENT_COLOR_VIDEO] = value;
#endif

    return NO_ERROR;
}

status_t PQService::enableSharpness(uint32_t value)
{
    char pvalue[PROPERTY_VALUE_MAX];
    int ret;

    snprintf(pvalue, PROPERTY_VALUE_MAX, "%d\n", value);
    ret = property_set(PQ_DBG_SHP_EN_STR, pvalue);
    PQ_LOGD("[PQService] enableSharpness[%d]", value);

    m_bFeatureSwitch[SHARPNESS] = value;

    return NO_ERROR;
}

status_t PQService::enableDynamicContrast(uint32_t value)
{
    char pvalue[PROPERTY_VALUE_MAX];
    int ret;

    snprintf(pvalue, PROPERTY_VALUE_MAX, "%d\n", value);
    ret = property_set(PQ_ADL_PROPERTY_STR, pvalue);
    PQ_LOGD("[PQService] enableDynamicContrast[%d]", value);

    m_bFeatureSwitch[DYNAMIC_CONTRAST] = value;

    return NO_ERROR;
}

status_t PQService::enableDynamicSharpness(uint32_t value)
{
    char pvalue[PROPERTY_VALUE_MAX];
    int ret;

    snprintf(pvalue, PROPERTY_VALUE_MAX, "%d\n", value);
    ret = property_set(PQ_DBG_DSHP_EN_STR, pvalue);
    PQ_LOGD("[PQService] enableDynamicSharpness[%d]", value);

    m_bFeatureSwitch[DYNAMIC_CONTRAST] = value;

    return NO_ERROR;
}

status_t PQService::enableDisplayGamma(uint32_t value)
{
    if (value > 0)
    {
        char pvalue[PROPERTY_VALUE_MAX];
        char dvalue[PROPERTY_VALUE_MAX];
        int index;

        snprintf(dvalue, PROPERTY_VALUE_MAX, "%d\n", GAMMA_INDEX_DEFAULT);
        property_get(GAMMA_INDEX_PROPERTY_NAME, pvalue, dvalue);
        index = atoi(pvalue);
        _setGammaIndex(index);
    }
    else
    {
        _setGammaIndex(GAMMA_INDEX_DEFAULT);
    }
    PQ_LOGD("[PQService] enableDisplayGamma[%d]", value);

    m_bFeatureSwitch[DISPLAY_GAMMA] = value;

    return NO_ERROR;
}

status_t PQService::enableDisplayOverDrive(uint32_t value)
{
    DISP_OD_CMD cmd;

    if (m_drvID < 0)
    {
        PQ_LOGE("[PQService] enableDisplayOverDrive(), open device fail!!");
        return UNKNOWN_ERROR;
    }

    memset(&cmd, 0, sizeof(cmd));
    cmd.size = sizeof(cmd);
    cmd.type = 2;

    if (value > 0)
    {
        cmd.param0 = 1;
    }
    else
    {
        cmd.param0 = 0;
    }

    ioctl(m_drvID, DISP_IOCTL_OD_CTL, &cmd);

    PQ_LOGD("[PQService] enableDisplayOverDrive[%d]", value);

    m_bFeatureSwitch[DISPLAY_OVER_DRIVE] = value;

    return NO_ERROR;
}

void PQService::onFirstRef()
{
    run("PQServiceMain", PRIORITY_DISPLAY);
    PQ_LOGD("[PQ_SERIVCE] PQService : onFirstRef");
}

status_t PQService::readyToRun()
{
    PQ_LOGD("[PQ_SERIVCE] PQService is ready to run.");
    return NO_ERROR;
}


bool PQService::initDriverRegs()
{
    return NO_ERROR;
}

bool PQService::threadLoop()
{
    char value[PROPERTY_VALUE_MAX];
    int i;
    int32_t  status;
    int percentage = pqparam_mapping.image;  //default scenario = image

    MDP_TDSHP_FLAG = 0;

    property_get(PQ_PIC_MODE_PROPERTY_STR, value, PQ_PIC_MODE_DEFAULT);
    PQMode = atoi(value);
    PQScenario = SCENARIO_PICTURE;
    mCcorrDebug = false;

    /*load */
    void    *handle;
    DISP_PQ_MAPPING_PARAM *pq_mapping_ptr;
    DISP_PQ_PARAM *pq_param_ptr;
    DISPLAY_PQ_T  *pq_table_ptr;
    DISPLAY_TDSHP_T *tdshp_table_ptr;

    /* open the needed object */
    handle = dlopen("libpq_cust.so", RTLD_LAZY);
    if (!handle) {
        PQ_LOGD("[PQ_SERIVCE] can't open libpq_cust.so\n");
        goto PQCONFIG;
    }
    /* find the address of function and data objects */

    pq_mapping_ptr = (DISP_PQ_MAPPING_PARAM *)dlsym(handle, "pqparam_mapping");
    if (!pq_mapping_ptr) {
        PQ_LOGD("[PQ_SERIVCE] pqparam_mapping is not found in libpq_cust.so\n");
        dlclose(handle);
        goto PQCONFIG;
    }
    memcpy(&pqparam_mapping, pq_mapping_ptr, sizeof(DISP_PQ_MAPPING_PARAM));

    pq_param_ptr = (DISP_PQ_PARAM *)dlsym(handle, "pqparam_table");
    if (!pq_param_ptr) {
        PQ_LOGD("[PQ_SERIVCE] pqparam_table is not found in libpq_cust.so\n");
        dlclose(handle);
        goto PQCONFIG;
    }
    memcpy(&pqparam_table, pq_param_ptr, sizeof(pqparam_table));

#ifdef MDP_COLOR_ENABLE
    pq_table_ptr = (DISPLAY_PQ_T *)dlsym(handle, "secondary_pqindex");
#else
    pq_table_ptr = (DISPLAY_PQ_T *)dlsym(handle, "primary_pqindex");
#endif

    if (!pq_table_ptr) {
        PQ_LOGD("[PQ_SERIVCE] pqindex is not found in libpq_cust.so\n");
        dlclose(handle);
        goto PQCONFIG;
    }
    memcpy(&pqindex, pq_table_ptr, sizeof(DISPLAY_PQ_T));

    tdshp_table_ptr = (DISPLAY_TDSHP_T *)dlsym(handle, "tdshpindex");
    if (!tdshp_table_ptr) {
        PQ_LOGD("[PQ_SERIVCE] tdshpindex is not found in libpq_cust.so\n");
        dlclose(handle);
        goto PQCONFIG;
    }
    memcpy(&tdshpindex, tdshp_table_ptr, sizeof(DISPLAY_TDSHP_T));

    {
        gamma_entry_t* ptr = (gamma_entry_t*)dlsym(handle, "cust_gamma");
        if (!ptr) {
            PQ_LOGD("[PQ_SERIVCE] cust_gamma is not found in libpq_cust.so\n");
            dlclose(handle);
            goto PQCONFIG;
        }
        memcpy(&m_CustGamma[0][0], ptr, sizeof(gamma_entry_t) * GAMMA_LCM_MAX * GAMMA_INDEX_MAX);
        configGamma(PQMode);
    }
    dlclose(handle);

PQCONFIG:

#ifndef DISP_COLOR_OFF

    PQ_LOGD("[PQ_SERIVCE] DISP PQ init start...");

    if (m_drvID < 0)
    {
        PQ_LOGE("PQ device open failed!!");
    }

    // pq index
    ioctl(m_drvID, DISP_IOCTL_SET_PQINDEX, &pqindex);

    if (PQMode == PQ_PIC_MODE_STANDARD || PQMode == PQ_PIC_MODE_VIVID)
    {
        pqparam.u4SatGain = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SatGain;
        pqparam.u4PartialY = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4PartialY;
        pqparam.u4HueAdj[0] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[0];
        pqparam.u4HueAdj[1] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[1];
        pqparam.u4HueAdj[2] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[2];
        pqparam.u4HueAdj[3] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4HueAdj[3];
        pqparam.u4SatAdj[0] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[1];
        pqparam.u4SatAdj[2] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[2];
        pqparam.u4SatAdj[3] = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SatAdj[3];
        pqparam.u4Contrast = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4Contrast;
        pqparam.u4Brightness = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4Brightness;
        pqparam.u4SHPGain = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4SHPGain;
        pqparam.u4Ccorr = pqparam_table[(PQMode) * PQ_SCENARIO_COUNT + 0].u4Ccorr;
        status=ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQPARAM %d...",status);
        status=ioctl(m_drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQ_GAL_PARAM %d...",status);
        property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_STANDARD_DEFAULT);
        property_set(PQ_TDSHP_PROPERTY_STR, value);

        configCcorrCoef(pqparam.u4Ccorr);
    }
    else if (PQMode == PQ_PIC_MODE_USER_DEF)
    {

        // base on vivid
        //memcpy(&pqparam_user_def, &pqparam_usr, sizeof(pqparam_usr));

        property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... tdshp[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SHPGain = i;

        property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... gsat[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatGain = i;

        property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... contrast[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Contrast = i;

        property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
        i = atoi(value);
        PQ_LOGD("[PQ_SERVICE] property get... pic bright[%d]", i);
        pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Brightness = i;

        pqparam.u4SatGain = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatGain * percentage / 100;
        pqparam.u4PartialY = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4PartialY;
        pqparam.u4HueAdj[0] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[0];
        pqparam.u4HueAdj[1] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[1];
        pqparam.u4HueAdj[2] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[2];
        pqparam.u4HueAdj[3] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4HueAdj[3];
        pqparam.u4SatAdj[0] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[0];
        pqparam.u4SatAdj[1] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[1] * percentage / 100;
        pqparam.u4SatAdj[2] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[2] * percentage / 100;
        pqparam.u4SatAdj[3] = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SatAdj[3] * percentage / 100;
        pqparam.u4Contrast = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Contrast * percentage / 100;
        pqparam.u4Brightness = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Brightness * percentage / 100;
        pqparam.u4SHPGain = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4SHPGain * percentage / 100;
        pqparam.u4Ccorr = pqparam_table[PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT].u4Ccorr;

        status = ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQPARAM %d...",status);
        status = ioctl(m_drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQ_GAL_PARAM %d...",status);

        configCcorrCoef(pqparam.u4Ccorr);
    }
    else
    {
        memcpy(&pqparam, &pqparam_table[0], sizeof(pqparam));

        PQ_LOGE("[PQ][main pq] main, property get... unknown pic_mode[%d]", PQMode);
        status = ioctl(m_drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQPARAM %d...",status);
        status = ioctl(m_drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);
        PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQ_GAL_PARAM %d...",status);
    }

    status = ioctl(m_drvID, DISP_IOCTL_SET_PQ_CAM_PARAM, &pqparam_table[2]);
    PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_PQ_CAM_PARAM %d...",status);
    status = ioctl(m_drvID, DISP_IOCTL_SET_TDSHPINDEX, &tdshpindex);
    PQ_LOGD("[PQ_SERIVCE] DISP_IOCTL_SET_TDSHPINDEX %d...",status);
    // write DC property
    property_get(PQ_ADL_PROPERTY_STR, value, PQ_ADL_INDEX_DEFAULT);
    property_set(PQ_ADL_PROPERTY_STR, value);

    configCcorrCoef(pqparam.u4Ccorr);

    PQ_LOGD("[PQ_SERIVCE] DISP PQ init end...");
#endif

    PQ_LOGD("[PQ_SERIVCE] threadLoop config User_Def PQ... end");


    while(1)
    {
        sleep(10);
        //PQ_LOGD("[PQ_SERIVCE] PQService threadloop");

    }

return true;
}

int PQService::_getLcmIndexOfGamma()
{
    static int lcmIdx = -1;

    if (lcmIdx == -1) {
        int ret = ioctl(m_drvID, DISP_IOCTL_GET_LCMINDEX, &lcmIdx);
        if (ret == 0) {
            if (lcmIdx < 0 || GAMMA_LCM_MAX <= lcmIdx) {
                PQ_LOGE("Invalid LCM index %d, GAMMA_LCM_MAX = %d", lcmIdx, GAMMA_LCM_MAX);
                lcmIdx = 0;
            }
        } else {
            PQ_LOGE("ioctl(DISP_IOCTL_GET_LCMINDEX) return %d", ret);
            lcmIdx = 0;
        }
    }

    PQ_LOGD("LCM index: %d/%d", lcmIdx, GAMMA_LCM_MAX);

    return lcmIdx;
}


void PQService::_setGammaIndex(int index)
{
    if (index < 0 || GAMMA_INDEX_MAX <= index)
        index = GAMMA_INDEX_DEFAULT;

    DISP_GAMMA_LUT_T *driver_gamma = new DISP_GAMMA_LUT_T;

    int lcm_id = _getLcmIndexOfGamma();

    const gamma_entry_t *entry = &(m_CustGamma[lcm_id][index]);
    driver_gamma->hw_id = DISP_GAMMA0;
    for (int i = 0; i < DISP_GAMMA_LUT_SIZE; i++) {
        driver_gamma->lut[i] = GAMMA_ENTRY((*entry)[0][i], (*entry)[1][i], (*entry)[2][i]);
    }

    ioctl(m_drvID, DISP_IOCTL_SET_GAMMALUT, driver_gamma);

    delete driver_gamma;
}

void PQService::configGamma(int picMode)
{
#if (GAMMA_LCM_MAX > 0) && (GAMMA_INDEX_MAX > 0)
    int lcmIndex = 0;
    int gammaIndex = 0;
#endif

#if GAMMA_LCM_MAX > 1
    lcmIndex = _getLcmIndexOfGamma();
#endif

#if GAMMA_INDEX_MAX > 1
    // get gamma index from runtime property configuration
    char property[PROPERTY_VALUE_MAX];

    gammaIndex = GAMMA_INDEX_DEFAULT;
    if (picMode == PQ_PIC_MODE_USER_DEF &&
            property_get(GAMMA_INDEX_PROPERTY_NAME, property, NULL) > 0 &&
            strlen(property) > 0)
    {
        gammaIndex = atoi(property);
    }

    if (gammaIndex < 0 || GAMMA_INDEX_MAX <= gammaIndex)
        gammaIndex = GAMMA_INDEX_DEFAULT;

    PQ_LOGD("Gamma index: %d/%d", gammaIndex, GAMMA_INDEX_MAX);
#endif

#if (GAMMA_LCM_MAX > 0) && (GAMMA_INDEX_MAX > 0)
    DISP_GAMMA_LUT_T *driverGamma = new DISP_GAMMA_LUT_T;

    const gamma_entry_t *entry = &(m_CustGamma[lcmIndex][gammaIndex]);
    driverGamma->hw_id = DISP_GAMMA0;
    for (int i = 0; i < DISP_GAMMA_LUT_SIZE; i++) {
        driverGamma->lut[i] = GAMMA_ENTRY((*entry)[0][i], (*entry)[1][i], (*entry)[2][i]);
    }

    ioctl(m_drvID, DISP_IOCTL_SET_GAMMALUT, driverGamma);

    delete driverGamma;
#endif
}

static const String16 sDump("android.permission.DUMP");
status_t PQService::dump(int fd, const Vector<String16>& args)
{
    static const size_t SIZE = 4096;
    char *buffer;
    String8 result;

    buffer = new char[SIZE];
  PQ_LOGD("[PQ_SERIVCE] PQService dump");
    if (!PermissionCache::checkCallingPermission(sDump)) {
        snprintf(buffer, SIZE, "Permission Denial: "
                "can't dump SurfaceFlinger from pid=%d, uid=%d\n",
                IPCThreadState::self()->getCallingPid(),
                IPCThreadState::self()->getCallingUid());
        result.append(buffer);
    } else {
        // Try to get the main lock, but don't insist if we can't
        // (this would indicate AALService is stuck, but we want to be able to
        // print something in dumpsys).
        int retry = 3;
        while (mLock.tryLock() < 0 && --retry >= 0) {
            usleep(500 * 1000);
        }
        const bool locked(retry >= 0);
        if (!locked) {
            snprintf(buffer, SIZE,
                    "PQService appears to be unresponsive, "
                    "dumping anyways (no locks held)\n");
            result.append(buffer);
        } else {
          size_t numArgs = args.size();
            for (size_t argIndex = 0; argIndex < numArgs; ) {
                if (args[argIndex] == String16("--ccorrdebug")) {
                    mCcorrDebug = true;
                    snprintf(buffer, SIZE, "CCORR Debug On");
                } else if (args[argIndex] == String16("--ccorrnormal")) {
                    mCcorrDebug = false;
                    snprintf(buffer, SIZE, "CCORR Debug Off");
                }
                argIndex += 1;
            }
      result.append(buffer);
        }

        if (locked) {
            mLock.unlock();
        }
    }

    write(fd, result.string(), result.size());

    delete [] buffer;

    return NO_ERROR;
}

};
