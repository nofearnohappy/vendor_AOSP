#define LOG_TAG "flicker_hal.cpp"
#include <stdlib.h>
#include <stdio.h>
#include <common/include/common.h>
#include <client/mtk_metadata_tag.h>
#include <cutils/properties.h>
#include <camera_custom_nvram.h>
#include <ispdrv_mgr.h>
#include <isp_tuning_mgr.h>
#include <tuning_mgr.h>
#include <camera_custom_cam_cal.h>
#include <cam_cal_drv.h>
#include <IHalSensor.h>
#include <ctype.h>
#include <sys/stat.h>
#include <aaa_sensor_mgr.h>
#include <time.h>
#include <kd_camera_feature.h>
#include <isp_mgr.h>
#include <nvram_drv.h>
#include <nvram_drv_mgr.h>
#include <vector>

using namespace NS3Av3;
#include "Flicker_type.h"
#include <utils/threads.h>
#include <cutils/log.h>
#include "awb_param.h"
#include "ae_param.h"
#include "af_param.h"
#include "camera_custom_AEPlinetable.h"
#include "sequential_testing.h"
#include "FlickerDetection.h"
#include "camera_custom_flicker.h"
#include "imem_buffer.h"
#include <iopipe/CamIO/INormalPipe.h>
#include "flicker_hal.h"
#include "flicker_util.h"
#include "camera_custom_flicker_para.h"
#include "ae_feature.h"
#include <string.h>
#include <cutils/xlog.h>
#if 1//iopipe2.0
#define IOPIPE_SET_MODUL_REG(handle,RegName,Value)
#define IOPIPE_SET_MODUL_REGS(handle, StartRegName, size, ValueArray)
namespace NSImageio {
namespace NSIspio {
enum EModule
{
    //raw
    EModule_OB          = 00,
    EModule_BNR         = 05,
    EModule_LSC         = 10,
    EModule_RPG         = 15,
    EModule_AE          = 20,
    EModule_AWB         = 25,
    EModule_SGG1        = 30,
    EModule_FLK         = 35,
    EModule_AF          = 40,
    EModule_SGG2        = 45,
    EModule_SGG3        = 46,
    EModule_EIS         = 50,
    EModule_LCS         = 55,
    EModule_BPCI        = 60,
    EModule_LSCI        = 65,
    EModule_AAO         = 70,
    EModule_ESFKO       = 75,
    EModule_AFO         = 80,
    EModule_EISO        = 85,
    EModule_LCSO        = 90,
    EModule_iHDR        = 95,
    EModule_CAMSV_IMGO  = 100,
    //raw_d
    EModule_OB_D        = 1000,
    EModule_BNR_D         = 1005,
    EModule_LSC_D       = 1010,
    EModule_RPG_D       = 1015,
    EModule_BPCI_D      = 1020,
    EModule_LSCI_D      = 1025,
    EModule_AE_D        = 1030,
    EModule_AWB_D       = 1035,
    EModule_SGG1_D      = 1040,
    EModule_AF_D        = 1045,
    EModule_LCS_D       = 1050,
    EModule_AAO_D       = 1055,
    EModule_AFO_D       = 1060,
    EModule_LCSO_D      = 1065,
    EModule_iHDR_D      = 1070
};
enum EPIPECmd {

    //  IPECmd_SET_SENSOR_DEV             = 0x1001,
    //  EPIPECmd_SET_SENSOR_GAIN            = 0x1002,
    //  EPIPECmd_SET_SENSOR_EXP             = 0x1003,
    //  EPIPECmd_SET_CAM_MODE               = 0x1004,
        EPIPECmd_SET_SCENE_MODE             = 0x1005,
        EPIPECmd_SET_ISO                    = 0x1006,
        EPIPECmd_SET_FLUORESCENT_CCT        = 0x1007,
        EPIPECmd_SET_SCENE_LIGHT_VALUE      = 0x1008,
        EPIPECmd_VALIDATE_FRAME             = 0x1009,
        EPIPECmd_SET_OPERATION_MODE         = 0x100A,
        EPIPECmd_SET_EFFECT                 = 0x100B,
        EPIPECmd_SET_ZOOM_RATIO             = 0x100C,
        EPIPECmd_SET_BRIGHTNESS             = 0x100D,
        EPIPECmd_SET_CONTRAST               = 0x100E,
        EPIPECmd_SET_EDGE                   = 0x100F,
        EPIPECmd_SET_HUE                    = 0x1010,
        EPIPECmd_SET_SATURATION             = 0x1011,
        EPIPECmd_SEND_TUNING_CMD            = 0x1012,
        EPIPECmd_DECIDE_OFFLINE_CAPTURE     = 0x1013,
        EPIPECmd_LOCK_REG                   = 0x1014,
        EPIPECmd_SET_SHADING_IDX            = 0x1018,

        EPIPECmd_SET_RRZ                    = 0x101A,
        EPIPECmd_SET_P1_UPDATE              = 0x101B,
        EPIPECmd_SET_IMGO                   = 0x101C,

        EPIPECmd_SET_BASE_ADDR              = 0x1102,
        EPIPECmd_SET_CQ_CHANNEL             = 0x1103,
        EPIPECmd_SET_CQ_TRIGGER_MODE        = 0x1104,
        EPIPECmd_AE_SMOOTH                  = 0x1105,
        EPIPECmd_SET_FMT_EN                 = 0x1107,
        EPIPECmd_SET_GDMA_LINK_EN           = 0x1108,
        EPIPECmd_SET_FMT_START              = 0x1109,
        EPIPECmd_SET_CAM_CTL_DBG            = 0x110A,
        EPIPECmd_SET_IMG_PLANE_BY_IMGI      = 0x110B,
        EPIPECmd_SET_P2_QUEUE_CONTROL_STATE = 0x110C,

        EPIPECmd_GET_TG_OUT_SIZE            = 0x110F,
        EPIPECmd_GET_RMX_OUT_SIZE           = 0x1110,
        EPIPECmd_GET_HBIN_INFO              = 0x1111,
        EPIPECmd_GET_EIS_INFO               = 0x1112,
        EPIPECmd_GET_SUGG_BURST_QUEUE_NUM   = 0x1114,

        EPIPECmd_GET_TWIN_INFO              = 0x1116,

        EPIPECmd_SET_EIS_CBFP               = 0X1117,
        EPIPECmd_SET_LCS_CBFP               = 0X1118,
        EPIPECmd_SET_SGG2_CBFP              = 0X1119,

        EPIPECmd_GET_CUR_FRM_STATUS         = 0x111D,
        EPIPECmd_GET_CUR_SOF_IDX            = 0x111E,

        EPIPECmd_ALLOC_UNI                  = 0x1200,
        EPIPECmd_DEALLOC_UNI                = 0x1201,
        //EPIPECmd_ALLOC_FLK_PATH             = 0x1200,
        //EPIPECmd_DEALLOC_FLK_PATH           = 0x1201,
        //EPIPECmd_ALLOC_HDS_PATH             = 0x1202,
        //EPIPECmd_DEALLOC_HDS_PATH           = 0x1203,

        EPIPECmd_SET_NR3D_EN                = 0x1300,
        EPIPECmd_SET_NR3D_DMA_SEL           = 0x1301,
        EPIPECmd_SET_CRZ_EN                 = 0x1302,
        EPIPECmd_SET_JPEG_CFG               = 0x1303,
        EPIPECmd_SET_JPEG_WORKBUF_SIZE      = 0x1304,

        EPIPECmd_SET_MODULE_EN              = 0x1401, //phase out
        EPIPECmd_SET_MODULE_SEL             = 0x1402, //phase out
        EPIPECmd_SET_MODULE_CFG             = 0x1403, //phase out
        EPIPECmd_GET_MODULE_HANDLE          = 0x1404, //phase out
        EPIPECmd_SET_MODULE_CFG_DONE        = 0x1405, //phase out
        EPIPECmd_RELEASE_MODULE_HANDLE      = 0x1406, //phase out
        EPIPECmd_SET_MODULE_DBG_DUMP        = 0x1407, //phase out


        EPIPECmd_GET_SENSOR_PRV_RANGE       = 0x2001,
        EPIPECmd_GET_SENSOR_FULL_RANGE      = 0x2002,
        EPIPECmd_GET_RAW_DUMMY_RANGE        = 0x2003,
        EPIPECmd_GET_SENSOR_NUM             = 0x2004,
        EPIPECmd_GET_SENSOR_TYPE            = 0x2005,
        EPIPECmd_GET_RAW_INFO               = 0x2006,
        EPIPECmd_GET_EXIF_DEBUG_INFO        = 0x2007,
        EPIPECmd_GET_SHADING_IDX            = 0x2008,
        EPIPECmd_GET_ATV_DISP_DELAY         = 0x2009,
        EPIPECmd_GET_SENSOR_DELAY_FRAME_CNT = 0x200A,
        EPIPECmd_GET_CAM_CTL_DBG            = 0x200B,
        EPIPECmd_GET_FMT                    = 0x200C,
        EPIPECmd_GET_GDMA                   = 0x200D,
        EPIPECmd_GET_NR3D_GAIN              = 0x200E,
        EPIPECmd_ISP_RESET                  = 0x4001,
        EPIPECmd_MAX                        = 0xFFFF

};
};
};
#endif

#if 1
    #define ISP_READ_BITS_NOPROTECT(IspDrvPtr,RegName,FieldName,...)    (0)
    #define ISP_READ_REG_NOPROTECT(IspDrvPtr,RegName,...)   (0)
#endif


using namespace NSCamCustom;
//=====================================================
// define
//=====================================================
#define FLIKCER_DEBUG 0
#define FLICKER_MAX_LENG  4096
#define MHAL_FLICKER_WORKING_BUF_SIZE (FLICKER_MAX_LENG*4*3)    // flicker support max size
#define FLICKER_SUPPORT_MAX_SIZE (FLICKER_MAX_LENG*2*3)

#define DEF_PROP_DEF_HZ "z.flk_def_hz"  //50, 60
#define DEF_PROP_VERBOSE_EN "z.flk_verbose_en"

//log
#define logI(fmt, arg...) ALOGD(fmt, ##arg)
#define logV(fmt, arg...) {if(logv_en) logI(fmt, ##arg);}
#define logE(fmt, arg...) ALOGD("MError: func=%s line=%d: " fmt, __FUNCTION__, __LINE__, ##arg)
//=====================================================
// variable
//=====================================================
static int g_frmCntPre=0;
FLICKER_EXT_PARA g_flickerExtPara;
static int g_maxDetExpUs=70000;
static int g_flickerHz=0;
static int logv_en=0;
//=====================================================
void debugSetting()
{
#if FLIKCER_DEBUG
    logv_en = 1;
#else
    logv_en = FlickerUtil::getPropInt(DEF_PROP_VERBOSE_EN,0);
#endif
}

MBOOL FlickerHal::pipeSendCommand(MINT32 cmd, MUINTPTR arg1, MUINTPTR arg2, MUINTPTR arg3)
{
    int ret;
    INormalPipe*   pPipe;//iopipe2.0
    pPipe = INormalPipe::createInstance(m_sensorId,"flicker");//iopipe2.0
    ret = pPipe->sendCommand(cmd, arg1, arg2, arg3);
    pPipe->destroyInstance("flicker");
    return ret;
}

FlickerHal* FlickerHal::getInstance(int sensorDev)
{
    static FlickerHal singletonMain(ESensorDev_Main);
    static FlickerHal singletonSub(ESensorDev_Sub);
    static FlickerHal singletonMain2(ESensorDev_MainSecond);
    if(sensorDev==ESensorDev_Main)
        return &singletonMain;
    else if(sensorDev==ESensorDev_Sub)
        return &singletonSub;
    else  //if(sensorDev==ESensorDev_MainSecond)
        return &singletonMain2;
}
//=====================================================
int FlickerHal::setFlickerModeSub(int mode)
{
    switch(mode)
    {
    case MTK_CONTROL_AE_ANTIBANDING_MODE_60HZ:
        logI("setflickermode 60");
        m_flickerMode = LIB3A_AE_FLICKER_MODE_60HZ;
        break;
    case MTK_CONTROL_AE_ANTIBANDING_MODE_50HZ:
        logI("setflickermode 50");
        m_flickerMode = LIB3A_AE_FLICKER_MODE_50HZ;
        break;
    case MTK_CONTROL_AE_ANTIBANDING_MODE_AUTO:
        logI("setflickermode auto");
        m_flickerMode = LIB3A_AE_FLICKER_MODE_AUTO;
        break;
    case MTK_CONTROL_AE_ANTIBANDING_MODE_OFF:
        logI("setflickermode off");
        m_flickerMode = LIB3A_AE_FLICKER_MODE_OFF;
        break;
    default:
        logI("setflickermode others (error mode)");
        m_flickerMode = LIB3A_AE_FLICKER_MODE_50HZ;
        break;
    }
    return 0;
}
//=====================================================
MINT32 FlickerHal::createBufSub()
{
    if(m_pVectorData1 != 0)
        logE("m_pVectorData1 is not empty. The release buffer should be called first!");
    m_pVectorData1 = (MINT32*)malloc(MHAL_FLICKER_WORKING_BUF_SIZE); //vector data, sw buffer
    if(m_pVectorData1 == NULL)
    {
        logE("memory1 is not enough");
        return -1;
    }

    m_pVectorData2 = (MINT32*)malloc(MHAL_FLICKER_WORKING_BUF_SIZE);
    if(m_pVectorData2 == NULL)
    {
        logE("memory2 is not enough");
        return -1;
    }
#if 0

    mpIMemDrv = IMemDrv::createInstance();
    if(mpIMemDrv == NULL)
    {
        logE("mpIMemDrv is NULL");
        return -1;
    }

    for(int i = 0; i < 2; i++)
    {
        flkbufInfo[i].size = flkbufInfo[i].virtAddr = flkbufInfo[i].phyAddr = 0;
        flkbufInfo[i].useNoncache = 0;   // improve the performance
        flkbufInfo[i].memID = -1;
    }

    if(!mpIMemDrv->init())
    {
        logE(" mpIMemDrv->init() error");
        return -1;
    }

    for(int i = 0; i < 2; i++)
    {
        flkbufInfo[i].size = FLICKER_SUPPORT_MAX_SIZE;
        if(mpIMemDrv->allocVirtBuf(&flkbufInfo[i]) < 0)
        {
            logE("mpIMemDrv->allocVirtBuf(): id=%d", i);
            return -1;

        }

        if(mpIMemDrv->mapPhyAddr(&flkbufInfo[i]) < 0)
        {
            logE("mapPhyAddr id=%d",i);
            if (mpIMemDrv->freeVirtBuf(&flkbufInfo[i]) < 0)
                logE("freeVirtBuf, id=%d",i);
            return -1;
        }
    }
#endif	
    //isp
    #if 0
    m_pIspDrv2 = IspDrv::createInstance();
    #endif
    if (!m_pIspDrv2) {
        logE("createInstance IspDrv fail");
        return -1;
    }
    if( m_pIspDrv2->init("flicker")<0)
    {
	   	logE("ISP init fail");
        return -1;
    }
    return 0;
}
MVOID FlickerHal::releaseBufSub()
{
     if (m_pIspDrv2)
    {
		m_pIspDrv2->uninit("flicker");
		m_pIspDrv2->destroyInstance();
		m_pIspDrv2 = NULL;
    }
    if(m_pVectorData1 != NULL)
    {
        free(m_pVectorData1);
        m_pVectorData1 = NULL;
    }
    if(m_pVectorData2 != NULL)
    {
        free(m_pVectorData2);
        m_pVectorData2 = NULL;
    }
#if 0	
    if(mpIMemDrv)
    {
        for(MINT32 i = 0; i < 2; ++i)
        {
            if(flkbufInfo[i].virtAddr!=0)
            {
                if(mpIMemDrv->unmapPhyAddr(&flkbufInfo[i]) < 0)
                {
                    logE("unmapPhyAddr id=%d",i);
                }

                if (mpIMemDrv->freeVirtBuf(&flkbufInfo[i]) < 0)
                {
                    logE("freeVirtBuf id=%d",i);
                }
            }
        }
        mpIMemDrv->uninit();
        mpIMemDrv->destroyInstance();
    }
#endif	
}
//=====================================================
FlickerHal::FlickerHal(int sensorDev)
{
    logI("func=%s line=%d",__FUNCTION__, __LINE__);
    mIsEnable=0;
    m_pVectorData1 = 0;
    m_pVectorData2 = 0;
    m_u4SensorPixelClkFreq = 0;
    m_flkAlgStatus = INCONCLUSIVE;
    m_u4FlickerFreq = HAL_FLICKER_AUTO_50HZ;
    m_u4FlickerWidth = 0;
    m_u4FlickerHeight = 0;
    m_flickerMode=LIB3A_AE_FLICKER_MODE_AUTO;
    m_sensorDev =sensorDev;
    mSensorMode=SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
    mIsInited=0;
}
//=====================================================
FlickerHal::~FlickerHal()
{
    logI("func=%s line=%d",__FUNCTION__, __LINE__);
}
//=====================================================
void setFlickerStateHal(EV_TABLE val)
{
    set_flicker_state(val);
    if(val==Hz50)
        g_flickerHz=50;
    else
        g_flickerHz=60;
}
//=====================================================
MINT32 FlickerHal::init(MINT32 i4SensorOpenIndex)
{
    int sensorId=-1;
    logI("func=%s line=%d mUsers=%d",__FUNCTION__, __LINE__,mUsers);
    int iniFilerHz;
    //1: cust init hz
    cust_getFlickerHalPara(&iniFilerHz, &g_maxDetExpUs);
    //2: last detected hz
    if(g_flickerHz==50)
        iniFilerHz=50;
    else if(g_flickerHz==60)
        iniFilerHz=60;
    else
        ;
    //3: property hz
    int propDefFlicker;
    propDefFlicker = FlickerUtil::getPropInt(DEF_PROP_DEF_HZ,0);
    if(propDefFlicker==50)
        iniFilerHz = 50;
    else if(propDefFlicker==60)
        iniFilerHz = 60;
    else
        ;
    //ini flicker hz
    logI("init flicker Hz: %d",iniFilerHz);
    if(iniFilerHz == 50)
    {
        m_u4FlickerFreq = HAL_FLICKER_AUTO_50HZ;
        setFlickerStateHal(Hz50);
    }
    else
    {
        m_u4FlickerFreq = HAL_FLICKER_AUTO_60HZ;
        setFlickerStateHal(Hz60);
    }
    reset_flicker_queue();
    MINT32 err = 0;
    if (mUsers > 0)
    {
        logE("mUsers %d has created", mUsers);
        android_atomic_inc(&mUsers);
        return 0;
    }
    //sensor
    IHalSensorList* const pIHalSensorList = IHalSensorList::get();
    IHalSensor* pHalSensorObj;
    pHalSensorObj = pIHalSensorList->createSensor("flicker", i4SensorOpenIndex);
    if(pHalSensorObj == NULL)
    {
        err=-1;
        logI("error pHalSensorObj=0");
        goto create_fail_exit;
    }


    err = pHalSensorObj->sendCommand(m_sensorDev,SENSOR_CMD_GET_PIXEL_CLOCK_FREQ, (MUINTPTR)&m_u4SensorPixelClkFreq, 0, 0);
    if(err != 0)
    {
        logI("No plck. \n");
    }
    logI("[Flicker Hal]init - m_u4SensorPixelClkFreq: %d \n", m_u4SensorPixelClkFreq);

    err = pHalSensorObj->sendCommand(m_sensorDev,SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM, (MUINTPTR)&m_u4PixelsInLine, 0, 0);
    if(err != 0)
    {
        logI("No pixels per line. \n");
    }
    m_u4PixelsInLine &= 0x0000FFFF;
    android_atomic_inc(&mUsers);
    m_u4FlickerWidth = 0;
    m_u4FlickerHeight = 0;

    //configure flicker window , it can be a fixed window
    int blkH, blkW;
    setWindowInfo(&blkH, &blkW);

    logI("Flicker parameter config read_freq=%d pixel_line=%d column_length=%d\n", (int)m_u4SensorPixelClkFreq, (int)m_u4PixelsInLine, (int)m_u4FlickerHeight);
    logI("flicker_init %d %d %d %d\n",m_u4PixelsInLine, blkH*3, blkW, (int)m_u4SensorPixelClkFreq);
    int ta;
    int tb;
    FLICKER_CUST_PARA para;
    //--------------------------------------------
    // get sensor info
    SensorStaticInfo rSensorStaticInfo;
    logI("ln=%d %d", __LINE__, m_sensorDev);
    switch  ( m_sensorDev )
    {
    case ESensorDev_Main:
        logI("ln=%d %d", __LINE__, m_sensorDev);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN, &rSensorStaticInfo);
        break;
    case ESensorDev_Sub:
        logI("ln=%d %d", __LINE__, m_sensorDev);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_SUB, &rSensorStaticInfo);
        break;
    case ESensorDev_MainSecond:
        logI("ln=%d %d", __LINE__, m_sensorDev);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN_2, &rSensorStaticInfo);
        break;
    default:    //  Shouldn't happen.
        logI("ln=%d %d", __LINE__, m_sensorDev);
        logE("Invalid sensor device: %d, id=%d", m_sensorDev, sensorId);
        break;
        // return MFALSE;
    }
    logI("ln=%d %d", __LINE__, rSensorStaticInfo.sensorDevID);
    sensorId=rSensorStaticInfo.sensorDevID;
    //--------------------------------------------
    logI("preview");
    int sensorMode;
    if(mSensorMode==SENSOR_SCENARIO_ID_NORMAL_PREVIEW)
        sensorMode = e_sensorModePreview;
    else if(mSensorMode==SENSOR_SCENARIO_ID_NORMAL_CAPTURE)
        sensorMode = e_sensorModeCapture;
    else if(mSensorMode==SENSOR_SCENARIO_ID_NORMAL_VIDEO)
        sensorMode = e_sensorModeVideoPreview;
    else if(mSensorMode==SENSOR_SCENARIO_ID_SLIM_VIDEO1)
        sensorMode = e_sensorModeVideo1;
    else //if(mSensorMode==SENSOR_SCENARIO_ID_SLIM_VIDEO2   )
        sensorMode = e_sensorModeVideo2;

    nvGetFlickerPara(sensorId, sensorMode, &para);

    g_flickerExtPara.flickerFreq[0]=para.flickerFreq[0];
    g_flickerExtPara.flickerFreq[1]=para.flickerFreq[1];
    g_flickerExtPara.flickerFreq[2]=para.flickerFreq[2];
    g_flickerExtPara.flickerFreq[3]=para.flickerFreq[3];
    g_flickerExtPara.flickerFreq[4]=para.flickerFreq[4];
    g_flickerExtPara.flickerFreq[5]=para.flickerFreq[5];
    g_flickerExtPara.flickerFreq[6]=para.flickerFreq[6];
    g_flickerExtPara.flickerFreq[7]=para.flickerFreq[7];
    g_flickerExtPara.flickerFreq[8]=para.flickerFreq[8];
    g_flickerExtPara.flickerGradThreshold=para.flickerGradThreshold;
    g_flickerExtPara.flickerSearchRange=para.flickerSearchRange;
    g_flickerExtPara.minPastFrames=para.minPastFrames;
    g_flickerExtPara.maxPastFrames=para.maxPastFrames;
    g_flickerExtPara.EV50_L50.m=para.EV50_L50.m;
    g_flickerExtPara.EV50_L50.b_l=para.EV50_L50.b_l;
    g_flickerExtPara.EV50_L50.b_r=para.EV50_L50.b_r;
    g_flickerExtPara.EV50_L50.offset=para.EV50_L50.offset;

    g_flickerExtPara.EV50_L60.m=para.EV50_L60.m;
    g_flickerExtPara.EV50_L60.b_l=para.EV50_L60.b_l;
    g_flickerExtPara.EV50_L60.b_r=para.EV50_L60.b_r;
    g_flickerExtPara.EV50_L60.offset=para.EV50_L60.offset;

    g_flickerExtPara.EV60_L50.m=para.EV60_L50.m;
    g_flickerExtPara.EV60_L50.b_l=para.EV60_L50.b_l;
    g_flickerExtPara.EV60_L50.b_r=para.EV60_L50.b_r;
    g_flickerExtPara.EV60_L50.offset=para.EV60_L50.offset;

    g_flickerExtPara.EV60_L60.m=para.EV60_L60.m;
    g_flickerExtPara.EV60_L60.b_l=para.EV60_L60.b_l;
    g_flickerExtPara.EV60_L60.b_r=para.EV60_L60.b_r;
    g_flickerExtPara.EV60_L60.offset=para.EV60_L60.offset;
    g_flickerExtPara.EV50_thresholds[0]=para.EV50_thresholds[0];
    g_flickerExtPara.EV50_thresholds[1]=para.EV50_thresholds[1];
    g_flickerExtPara.EV60_thresholds[0]=para.EV60_thresholds[0];
    g_flickerExtPara.EV60_thresholds[1]=para.EV60_thresholds[1];
    g_flickerExtPara.freq_feature_index[0]=para.freq_feature_index[0];
    g_flickerExtPara.freq_feature_index[1]=para.freq_feature_index[1];
    flicker_setExtPara(&g_flickerExtPara);

    ta = FlickerUtil::getMs();
    flicker_init(m_u4PixelsInLine, blkH*3, blkW, m_u4SensorPixelClkFreq);
    tb = FlickerUtil::getMs();
    logI("flk algn init time = %d ms",(int)(tb-ta));

    if( pHalSensorObj )
        pHalSensorObj->destroyInstance("flicker");
    mIsInited=1;
    return err;

create_fail_exit:
    if( pHalSensorObj )
        pHalSensorObj->destroyInstance("flicker");

    return err;
}
//=====================================================
MINT32 FlickerHal::uninit()
{
    logI("uninit line=%d mUsers=%d",__LINE__, mUsers);
    MINT32 err = 0;
    if (mUsers <= 0)
    {
        // No more users
        return 0;
    }
    // More than one user
    android_atomic_dec(&mUsers);
    if (mUsers == 0)
    {
        flicker_uninit(); //alg uninit
        enableFlickerDetection(0);
        mIsInited=0;
    }
    else
    {
        logI("Still %d users \n", mUsers);
    }
    return 0;
}
//=====================================================
MINT32 FlickerHal::setFlickerDrv(MBOOL flicker_en)
{
    logI("func=%s line=%d flicker_en=%d",__FUNCTION__, __LINE__,(int)flicker_en);
    int ret = 0;

    MINTPTR handle;
    if (0 == pipeSendCommand(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,NSImageio::NSIspio::EModule_FLK,
                                  (MINTPTR)&handle, (MINTPTR)(&("FlickerHal::setFlickerDrv()"))))
    {
        logE("setFlickerDrv() EPIPECmd_GET_MODULE_HANDLE fail");
        return -1;
    }

    if(flicker_en == 1)
    {
        if (MFALSE==pipeSendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_FLK, 1, 0))
        {
            logE("setFlickerDrv() EPIPECmd_SET_MODULE_EN fail");
            return -1;
        }
    }
    else       //disable flicker
    {
        if (MFALSE==pipeSendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_FLK, 0, 0))
        {
            logE("setFlickerDrv() EPIPECmd_SET_MODULE_EN fail");
            return -1;
        }
    }
	 if (0==pipeSendCommand(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("FlickerHal::setFlickerDrv()")), MNULL))
    {
        logE("setFlickerDrv");
        return -1;
    }
    return ret;
}
//=====================================================
MINT32 FlickerHal::setFlickerWinConfig(FLKWinCFG_T* ptFlkWinCfg)
{
    logI("FFLK func=%s line=%d dev=%d id=%d",__FUNCTION__, __LINE__,m_sensorDev,m_sensorId);
    int ret = 0;
    #if 0
    int retv;
    MINTPTR handle;
    retv = pipeSendCommand(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,
                                NSImageio::NSIspio::EModule_FLK,
                                (MINTPTR)&handle, (MINTPTR)(&("FlickerHal::setFlickerWinConfig()")));
    if(retv==0)
    {
        logE("setFlickerWinConfig");
        return -1;
    }

    logI("setFlickerWinConfig ln=%d nx,ny,sx,sy,ox,oy %d %d %d %d %d %d",__LINE__, ptFlkWinCfg->m_u4NumX, ptFlkWinCfg->m_u4NumY,
         ptFlkWinCfg->m_u4SizeX, ptFlkWinCfg->m_u4SizeY, ptFlkWinCfg->m_u4OffsetX, ptFlkWinCfg->m_u4OffsetY);

    CAM_REG_FLK_NUM reg_num;
    reg_num.Bits.FLK_NUM_X = ptFlkWinCfg->m_u4NumX;
    reg_num.Bits.FLK_NUM_Y = ptFlkWinCfg->m_u4NumY;
    logV("setFlickerWinConfig ln=%d %d",__LINE__,(MUINT32)reg_num.Raw);
    IOPIPE_SET_MODUL_REG(handle,  CAM_FLK_NUM,  (MUINT32)reg_num.Raw);

    CAM_REG_FLK_SIZE reg_size;
    reg_size.Bits.FLK_SIZE_X = ptFlkWinCfg->m_u4SizeX;
    reg_size.Bits.FLK_SIZE_Y = ptFlkWinCfg->m_u4SizeY;
    logV("setFlickerWinConfig ln=%d %d",__LINE__,(MUINT32)reg_size.Raw);
    IOPIPE_SET_MODUL_REG(handle,  CAM_FLK_SIZE,  (MUINT32)reg_size.Raw);

    CAM_REG_FLK_OFST reg_ofst;
    reg_ofst.Bits.FLK_OFST_X = ptFlkWinCfg->m_u4OffsetX;
    reg_ofst.Bits.FLK_OFST_Y = ptFlkWinCfg->m_u4OffsetY;
    logV("setFlickerWinConfig ln=%d %d",__LINE__,(MUINT32)reg_ofst.Raw);
    IOPIPE_SET_MODUL_REG(handle,  CAM_FLK_OFST,  (MUINT32)reg_ofst.Raw);

    logI("setFlickerWinConfig ln=%d",__LINE__);
    // set module config done
    retv = pipeSendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL);
    if(retv==0)
        logE("setFlickerWinConfig");

    retv = pipeSendCommand(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("FlickerHal::setFlickerWinConfig()")), MNULL);
    if(retv==0)
        logE("setFlickerWinConfig");
    #endif
    return ret;
}
//=============================================
MINT32 FlickerHal::setFlickerDMAConfig(
    unsigned long flicker_DMA_address ,MINT32 DMASize )
{
    int ret = 0;
    MINTPTR handle;
    if (0 == pipeSendCommand(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,
                                  NSImageio::NSIspio::EModule_ESFKO,
                                  (MINTPTR)&handle, (MINTPTR)(&("FlickerHal::setFlickerDMAConfig()")))    )
    {
        logE("setFlickerWinConfig");
    }
    else
    {
        IOPIPE_SET_MODUL_REG(handle,  CAM_ESFKO_BASE_ADDR,  flicker_DMA_address);
        IOPIPE_SET_MODUL_REG(handle,  CAM_ESFKO_XSIZE,  DMASize);
        IOPIPE_SET_MODUL_REG(handle,  CAM_ESFKO_OFST_ADDR,  0);
        IOPIPE_SET_MODUL_REG(handle,  CAM_ESFKO_YSIZE,  0);
        IOPIPE_SET_MODUL_REG(handle,  CAM_ESFKO_STRIDE,  DMASize);

        // set module config done
        if (0==pipeSendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
        {
            logE("setFlickerDMAConfig");
        }
    }
    if (0==pipeSendCommand(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("FlickerHal::setFlickerDMAConfig()")), MNULL))
    {
        logE("setFlickerDMAConfig");
    }
    return ret;
}
MUINT32 FlickerHal:: GetFlicker_CurrentDMA()
{
    return (MUINT32)ISP_READ_REG_NOPROTECT(m_pIspDrv2, CAM_ESFKO_BASE_ADDR) ;
}
//=============================================
void focusValueResize(MINT64* inH, MINT64* inV, MINT32* out)
{
    int i;
    for(i=0; i<9; i++)
    {
        int row;
        int col;
        double tmp;
        row=i/3;
        col=i%3;
        tmp	 = inH[(col*2)  +(row*2)*6]+
               inH[(col*2+1)+(row*2)*6]+
               inH[(col*2)  +(row*2+1)*6]+
               inH[(col*2+1)+(row*2+1)*6];
        tmp	 +=inV[(col*2)  +(row*2)*6]+
               inV[(col*2+1)+(row*2)*6]+
               inV[(col*2)  +(row*2+1)*6]+
               inV[(col*2+1)+(row*2+1)*6];
        if(tmp > 0x7fffffff)
            tmp=0x7fffffff;
        out[i]=tmp;
        logV("fvr i=%d, fv=%d",i,out[i]);
    }

}
//=============================================
int FlickerHal::Updated(FlickerInput* in, FlickerOutput* out)
{
    if(mIsInited==0)
        return -1;
    {
        if(m_flickerMode==LIB3A_AE_FLICKER_MODE_AUTO)
        {
            AAASensorMgr::getInstance().setFlickerFrameRateActive(m_sensorDev, 1);
        }
        else
        {
            AAASensorMgr::getInstance().setFlickerFrameRateActive(m_sensorDev, 0);
        }
    }



    int i;
    static int kk=0;
    MINT32 fv3x3[9];

    logI("U %d",kk);
    kk++;
    //---------------------
    // log input
    logV("exp=%d",in->aeExpTime);

    for(i=0; i<6; i++)
    {
        logV("fvH row%d=%lf %lf %lf %lf %lf %lf",i,
             (double)(in->afFullStat.i8StatH[i*6+0]), (double)(in->afFullStat.i8StatH[i*6+1]), (double)(in->afFullStat.i8StatH[i*6+2]),
             (double)(in->afFullStat.i8StatH[i*6+3]), (double)(in->afFullStat.i8StatH[i*6+4]), (double)(in->afFullStat.i8StatH[i*6+5]) );
    }
        for(i=0; i<6; i++)
        {
        logV("fvV row%d=%lf %lf %lf %lf %lf %lf",i,
             (double)(in->afFullStat.i8StatV[i*6+0]), (double)(in->afFullStat.i8StatV[i*6+1]), (double)(in->afFullStat.i8StatV[i*6+2]),
             (double)(in->afFullStat.i8StatV[i*6+3]), (double)(in->afFullStat.i8StatV[i*6+4]), (double)(in->afFullStat.i8StatV[i*6+5]) );
        }
    focusValueResize(in->afFullStat.i8StatH, in->afFullStat.i8StatV, fv3x3);

        logV("fv3x3 %d %d %d", out[0], out[1], out[2]);
        logV("fv3x3 %d %d %d", out[3], out[4], out[5]);
        logV("fv3x3 %d %d %d", out[6], out[7], out[8]);
    //---------------------------
    MINT32	err = 0;
    MINT32	i4DetectedResult = -1;
    g_frmCntPre++;
    if(g_frmCntPre<2)
    {
        goto lbExit;
    }
    //	(3) Analyze the flicker by passing EIS information.
    err = analyzeFlickerFrequency(in->aeExpTime, fv3x3);
    if	(err !=0)
    {
        logI("Updated] mpFlickerHal->analyzeFlickerFrequency() - (err)=(%x)",  err);
        goto lbExit;
    }
    //	(4) Get the flicker result from flicker hal
    err = getFlickerStatus(&i4DetectedResult);
    if	(err !=0)
    {
        logI("[Updated] getFlickerStatus err=%d", err);
        goto lbExit;
    }
    out->flickerResult = i4DetectedResult;

lbExit:
    return	err;
}
//=============================================
MINT32 FlickerHal::analyzeFlickerFrequency(int exp, MINT32 *i4vAFstatisic)
{
    logI("A ln=%d cnt=%d",__LINE__,g_frmCntPre);
    MINT32 i,i4Buff_idx=0;
    MINT32 i4vAFInfo[9];
    MINT32 i4DataLen = 0;
    MINT32 u4Height;
    MINT32 *m_FickerSW_buff_1;
    MINT32 *m_FickerSW_buff_2;

    MINT32* flickerDMAAdr;

    // get the AF statistic information
    i4vAFInfo[0]=i4vAFstatisic[0];
    i4vAFInfo[1]=i4vAFstatisic[1];
    i4vAFInfo[2]=i4vAFstatisic[2];
    i4vAFInfo[3]=i4vAFstatisic[3];
    i4vAFInfo[4]=i4vAFstatisic[4];
    i4vAFInfo[5]=i4vAFstatisic[5];
    i4vAFInfo[6]=i4vAFstatisic[6];
    i4vAFInfo[7]=i4vAFstatisic[7];
    i4vAFInfo[8]=i4vAFstatisic[8];

    if(GetFlicker_CurrentDMA()==flkbufInfo[0].phyAddr)
    {
        i4Buff_idx=0;
        m_FickerSW_buff_1=m_pVectorData1;  //m_FickerSW_buff_1 is n-1 data
        m_FickerSW_buff_2=m_pVectorData2;  // m_FickerSW_buff_1 is n-2 data
#if 0        
        mpIMemDrv->cacheSyncbyRange(IMEM_CACHECTRL_ENUM_INVALID, &flkbufInfo[1]);
#endif		
    }
    else
    {
        i4Buff_idx=1;
        m_FickerSW_buff_1=m_pVectorData2;  //m_FickerSW_buff_1 is n-1 data
        m_FickerSW_buff_2=m_pVectorData1;  //m_FickerSW_buff_2 is n-2 data
#if 0        
        mpIMemDrv->cacheSyncbyRange(IMEM_CACHECTRL_ENUM_INVALID, &flkbufInfo[0]);
#endif		
    }



    flickerDMAAdr = (MINT32 *) (8*((flkbufInfo[(i4Buff_idx+1)%2].virtAddr + 7)/8));

    if(m_u4FlickerHeight > FLICKER_MAX_LENG)
    {
        i4DataLen = 3*FLICKER_MAX_LENG /2 ;
        u4Height = FLICKER_MAX_LENG;
    }
    else
    {
        i4DataLen = 3*m_u4FlickerHeight /2 ;
        u4Height = m_u4FlickerHeight;
    }

    for(i=0; i<i4DataLen; i++)
    {
        m_FickerSW_buff_1[2*i+0] = flickerDMAAdr[i] &0x0000FFFF;
        m_FickerSW_buff_1[2*i+1] =(flickerDMAAdr[i] &0xFFFF0000)>>16;
    }
    for(i=0; i<u4Height; i+=100)
    {
        logV("sta row =%d, %d %d %d",i,m_FickerSW_buff_1[3*i],m_FickerSW_buff_1[3*i+1],m_FickerSW_buff_1[3*i+2]);
    }
    //switch FKO dst add.  to another buffer
    setFlickerDMAConfig(flkbufInfo[(i4Buff_idx+1)%2].phyAddr,FLK_DMA_Size);

    flkSensorInfo	sensorInfo;
    sensorInfo.pixelClock = m_u4SensorPixelClkFreq;
    sensorInfo.fullLineWidth = m_u4PixelsInLine;
    MINT32 win_wd = ((m_u4FlickerWidth / 3)>>1)<<1;
    MINT32 win_ht = ((u4Height / 3)>>1)<<1;
    flkEISVector	EISvector;
    int curExp;
    curExp = exp;
    flkAEInfo AEInfo;
    AEInfo.previewShutterValue=curExp;
    static int sBinOut=-1;
    int binOut;
    int binOut2;
    int afOn;
    binOut = FlickerUtil::getPropInt("z.flk_bin_out",0);
    binOut2 = FlickerUtil::getPropInt("z.flk_bin_out2",0);
    afOn = FlickerUtil::getPropInt("z.flk_af_on",1);

    if(afOn==0)
    {
        logI("AF OFF");
        int pp;
        for(pp=0; pp<9; pp++)
        {
            i4vAFInfo[pp]=0;
        }
    }
    if(binOut!=1)
    {
        sBinOut = binOut;
    }
    else if( (binOut==1 && sBinOut!=1) || binOut2==1)
    {
        sBinOut=binOut;
        int cnt;
        FlickerUtil::getFileCount("/sdcard/flicker_file_cnt.txt", &cnt, 0);
        FlickerUtil::setFileCount("/sdcard/flicker_file_cnt.txt", cnt+1);
        char s[100];
        FlickerUtil::createDir("/sdcard/flickerdata/");
        sprintf(s,"/sdcard/flickerdata/%03d",cnt);
        FlickerUtil::createDir(s);
        int tmp;
        FILE* fp;
        sprintf(s,"/sdcard/flickerdata/%03d/flk.raw",cnt);
        fp = fopen(s, "wb");
        if(fp)
        {
            tmp = i4DataLen;
            fwrite(&tmp, 1, 4, fp);
            fwrite(m_FickerSW_buff_1, 4, i4DataLen*2, fp);
            fwrite(m_FickerSW_buff_2, 4, i4DataLen*2, fp);
            tmp = win_wd;        fwrite(&tmp, 1, 4, fp);
            tmp = win_ht;        fwrite(&tmp, 1, 4, fp);
            tmp = m_u4FlickerFreq;        fwrite(&tmp, 1, 4, fp);
            fwrite(&sensorInfo, 1, sizeof(flkSensorInfo), fp);
            fwrite(&AEInfo, 1, sizeof(flkAEInfo), fp);
            fwrite(i4vAFInfo, 4, 9, fp);
            fclose(fp);
            logI("line=%d", __LINE__);
        }
    }
    if(   curExp < g_maxDetExpUs &&
            ( (curExp > 8200 && m_u4FlickerFreq == HAL_FLICKER_AUTO_60HZ ) || (curExp > 9800 && m_u4FlickerFreq == HAL_FLICKER_AUTO_50HZ ) ) )
    {
        //logI("do flicker Sw %d %d %d %d",win_wd, win_ht, curExp, m_u4FlickerFreq);
        m_flkAlgStatus = detectFlicker_SW(m_FickerSW_buff_1, m_FickerSW_buff_2, 3, 3, win_wd, win_ht, m_u4FlickerFreq, sensorInfo, EISvector, AEInfo, i4vAFInfo);

        int propFlickerSwitch;
        propFlickerSwitch = FlickerUtil::getPropInt("z.flk_switch_en",-1);
        if(propFlickerSwitch==0)
        {
            logI("flicker state not changed due to property fixed");
        }
        else
        {
            if(m_flkAlgStatus == FK100 && m_u4FlickerFreq==HAL_FLICKER_AUTO_60HZ) // if the decision is to change exposure table to 50Hz
            {
                m_u4FlickerFreq = HAL_FLICKER_AUTO_50HZ;
                setFlickerStateHal(Hz50);	// defined in "sequential_testing.cpp", have to call these two functions every time we change the flicker table
                reset_flicker_queue();
            }
            else if (m_flkAlgStatus == FK120 && m_u4FlickerFreq==HAL_FLICKER_AUTO_50HZ) // if the decision is to change exposure table to 60Hz
            {
                m_u4FlickerFreq = HAL_FLICKER_AUTO_60HZ;
                setFlickerStateHal(Hz60);	// defined in "sequential_testing.cpp", have to call these two functions every time we change the flicker table
                reset_flicker_queue();
            }
        }
    }
    else if( curExp >= g_maxDetExpUs)
    {
        logI("The exposure time is too long, skip flicker detection:%d\n", curExp);
    }
    else
    {
        logI("The exposure time is too short, skip flicker detection:%d\n", curExp);
    }
    logI("Status:%d, %d\n",m_u4FlickerFreq,curExp);
    return 0;
}
//=============================================
MINT32 FlickerHal::enableFlickerDetection(MBOOL bEnableFlicker)
{
    MINT32 ret = 0;	// 0: no error.
    logI("enableFlickerDetection() bEnableFlicker= %d\n",bEnableFlicker);
    if(mIsEnable==bEnableFlicker)
    {
        logI("flicker enable state is ame, not set to hardware");
        return 0;
    }
    if(bEnableFlicker)
    {
        if(flkbufInfo[0].virtAddr!=0)
        {
            MVOID * rPhyAddress = NULL;
            rPhyAddress = (MVOID*)flkbufInfo[0].phyAddr;
            /* FLK DMA size:
            2 bytes for per line in one window
            ESFKO_XSIZE = (FLK_WNUM_X * FLK_WNUM_Y * FLK_WSIZE_Y * 2) - 1
            ESFKO_YSIZE = 0 */
            FLK_DMA_Size=(strFlkWinCfg.m_u4NumX*strFlkWinCfg.m_u4NumY*strFlkWinCfg.m_u4SizeY*2)-1;
            setFlickerDMAConfig((MUINTPTR)rPhyAddress ,FLK_DMA_Size);
        }
        else
        {
            logI("!!!UNABLE to update pPhyAddr,pVirAddr!!!\n");
        }
        setFlickerDrv(bEnableFlicker);    // Save the column vector and difference
    }
    else
    {
        setFlickerDrv(bEnableFlicker);    // disable theflicker
    }
    mIsEnable = bEnableFlicker;
    return ret;
}
//=============================================
MINT32 FlickerHal::setWindowInfo(int* blkH, int*blkW)
{
    logI("func=%s line=%d",__FUNCTION__, __LINE__);

	MINT32 tgW;
	MINT32 tgH;
    pipeSendCommand(NSImageio::NSIspio::EPIPECmd_GET_TG_OUT_SIZE, m_sensorId, (MINTPTR)(&tgW), (MINTPTR)(&tgH));
    int imgW;
    int imgH;
    //--------------------------------------------
    // get sensor info
    IHalSensorList* const pIHalSensorList = IHalSensorList::get();
    IHalSensor* pIHalSensor = pIHalSensorList->createSensor("FlickerHal", m_sensorId);
    SensorDynamicInfo rSensorDynamicInfo;
    SensorStaticInfo rSensorStaticInfo;
    switch  ( m_sensorDev )
    {
    case ESensorDev_Main:
        pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_MAIN, &rSensorDynamicInfo);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN, &rSensorStaticInfo);
        break;
    case ESensorDev_Sub:
        pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_SUB, &rSensorDynamicInfo);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_SUB, &rSensorStaticInfo);
        break;
    case ESensorDev_MainSecond:
        pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_MAIN_2, &rSensorDynamicInfo);
        pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN_2, &rSensorStaticInfo);
        break;
    default:    //  Shouldn't happen.
        logE("Invalid sensor device: %d, id=%d", m_sensorDev,m_sensorId);
        return MFALSE;
    }
	logI("sensorWH, pv %d %d, cap %d %d, vd %d %d, v1 %d %d, v2 %d %d",
	rSensorStaticInfo.previewWidth,
	rSensorStaticInfo.previewHeight,
	rSensorStaticInfo.captureWidth,
	rSensorStaticInfo.captureHeight,
	rSensorStaticInfo.videoWidth,
	rSensorStaticInfo.videoHeight,
	rSensorStaticInfo.video1Width,
	rSensorStaticInfo.video1Height,
	rSensorStaticInfo.video2Width,
	rSensorStaticInfo.video2Height);

    //imgW=rSensorStaticInfo.previewWidth;
    //imgH=rSensorStaticInfo.previewHeight;
	imgW = tgW;
	imgH = tgH;


    logI("setFlickerWinConfig TG = %d, w %d, h %d, dev %d, id %d\n", rSensorDynamicInfo.TgInfo, imgW, imgH, m_sensorDev, m_sensorId);
    if(pIHalSensor)   pIHalSensor->destroyInstance("FlickerHal");

    //--------------------------------------------
    int u4ToleranceLine=20;
    imgW-=3;
    imgH-=25;

    m_u4FlickerWidth = imgW;
    m_u4FlickerHeight = imgH;
    //logI("[setWindowInfo] width:%d ,%d height:%d ,%d\n", u4Width, m_u4FlickerWidth, u4Height, m_u4FlickerHeight);
    if(m_u4FlickerHeight > FLICKER_MAX_LENG-6)
    {
        imgH = FLICKER_MAX_LENG-6;
    }
    else
    {
        imgH = m_u4FlickerHeight;
    }
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("slow_motion_debug", value, "0");
//    if(atoi(value))
    {
	    //imgW/=6;
	    //imgH/=6;
    }
    strFlkWinCfg.m_uImageW=imgW;
    strFlkWinCfg.m_uImageH=imgH;
    strFlkWinCfg.m_u4NumX=3;
    strFlkWinCfg.m_u4NumY=3;
    strFlkWinCfg.m_u4OffsetX=0;
    strFlkWinCfg.m_u4OffsetY=0+u4ToleranceLine;

    strFlkWinCfg.m_u4SizeX=((imgW-strFlkWinCfg.m_u4OffsetX)/6)*2;
    strFlkWinCfg.m_u4SizeY=((imgH-strFlkWinCfg.m_u4OffsetY+u4ToleranceLine)/6)*2;

    setFlickerWinConfig(&strFlkWinCfg);

    *blkH = strFlkWinCfg.m_u4SizeY;
    *blkW = strFlkWinCfg.m_u4SizeX;
    logI("blkH=%d, blkW = %d",*blkH, *blkW);
    logI("w,h,oft_x, oft_y, sz_x, sz_y %d %d %d %d %d %d", strFlkWinCfg.m_uImageW, strFlkWinCfg.m_uImageH, strFlkWinCfg.m_u4OffsetX, strFlkWinCfg.m_u4OffsetY, strFlkWinCfg.m_u4SizeX, strFlkWinCfg.m_u4SizeY);
    return 0;
}
MINT32 FlickerHal::getFlickerStatus(MINT32 *a_flickerStatus)
{
    if(m_flickerMode==LIB3A_AE_FLICKER_MODE_60HZ)
    {
        logV("getFlickerStatus 60");
        *a_flickerStatus= HAL_FLICKER_AUTO_60HZ;
    }
    else if(m_flickerMode==LIB3A_AE_FLICKER_MODE_50HZ)
    {
        logV("getFlickerStatus 50");
        *a_flickerStatus= HAL_FLICKER_AUTO_50HZ;
    }
    else if(m_flickerMode==AE_FLICKER_MODE_OFF)
    {
        logV("getFlickerStatus off");
        *a_flickerStatus = HAL_FLICKER_AUTO_OFF;
    }
    else // LIB3A_AE_FLICKER_MODE_AUTO
    {
        logV("getFlickerStatus auto");
        *a_flickerStatus = m_u4FlickerFreq;
    }
    //------------------------------
    logI("getFlickerStatus %d",(int)*a_flickerStatus);
    return 0;
}
int FlickerHal::enable(int en)
{
    logI("func=%s ln=%d",__FUNCTION__, __LINE__);
    enableFlickerDetection(en);
    return 0;
}
//=====================================================
int FlickerHal::setSensorMode(int i4NewSensorMode)
{
    mSensorMode = i4NewSensorMode;
    return 0;
}
int FlickerHal::createBuf()
{
    logI("func=%s ln=%d",__FUNCTION__, __LINE__);
    Mutex::Autolock lock(m_lock);
    return createBufSub();
}
int FlickerHal::releaseBuf()
{
    logI("func=%s ln=%d",__FUNCTION__, __LINE__);
    Mutex::Autolock lock(m_lock);
    releaseBufSub();
    return 0;
}
int FlickerHal::open(int sensorId, int tgInfo)
{
    logI("func=%s ln=%d tgInfo=%d (1,2=%d,%d)",__FUNCTION__, __LINE__,tgInfo, CAM_TG_1, CAM_TG_2);
    debugSetting();
    Mutex::Autolock lock(m_lock);
    m_sensorId = sensorId;
    mTgInfo = tgInfo;
    if(mTgInfo!=CAM_TG_1)
    {
        return -1;
    }
    if (init(sensorId) != 0)
    {
        logI("singleton.init() fail \n");
        return -1;
    }
    enable(1);
    return 0;
}
int FlickerHal::close()
{
    logI("func=%s ln=%d tgInfo=%d",__FUNCTION__, __LINE__,mTgInfo);
    Mutex::Autolock lock(m_lock);
    if(mTgInfo!=CAM_TG_1)
    {
        return -1;
    }
    enable(0);
    uninit();
    return 0;
}
int FlickerHal::update(FlickerInput* in, FlickerOutput* out)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("slow_motion_debug", value, "0");
    if(atoi(value))
    {
	    return 0;
    }
    Mutex::Autolock lock(m_lock);
    if(mTgInfo!=CAM_TG_1)
    {
        return -1;
    }
    return Updated(in, out);
}
int FlickerHal::setFlickerMode(int mode)
{
    Mutex::Autolock lock(m_lock);
    return setFlickerModeSub(mode);
}
