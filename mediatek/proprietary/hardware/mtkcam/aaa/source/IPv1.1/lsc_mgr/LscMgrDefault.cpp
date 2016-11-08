/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
 TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/

#define LOG_TAG "lsc_mgr_dft"
#ifndef ENABLE_MY_LOG
#define ENABLE_MY_LOG           (1)
#define GLOBAL_ENABLE_MY_LOG    (1)
#endif

#include "LscMgrDefault.h"
#include <mtkcam/common/include/IHalSensor.h>
#ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
#include <drv/src/isp/mt6797/iopipe/CamIO/FakeSensor.h>
#endif
#include <LscUtil.h>

#include "shading_tuning_custom.h"
#include "tsf_tuning_custom.h"

#include <isp_mgr.h>

using namespace NSIspTuning;
using namespace NSIspTuningv3;
using namespace NSCam;

static LscMgrDefault* _pLscMain;
static LscMgrDefault* _pLscMain2;
static LscMgrDefault* _pLscSub;

ILscMgr* 
LscMgrDefault::
createInstance(ESensorDev_T const eSensorDev, MINT32 i4SensorIdx)
{
    LSC_LOG_BEGIN("eSensorDev(0x%02x)", (MUINT32)eSensorDev);

    switch (eSensorDev)
    {
    default:
    case ESensorDev_Main:       //  Main Sensor
        static LscMgrDefault singleton_main(ESensorDev_Main, i4SensorIdx);
        _pLscMain = &singleton_main;
        LSC_LOG_END("ESensorDev_Main(0x%08x), i4SensorIdx(%d)", _pLscMain, i4SensorIdx);
        return _pLscMain;
    case ESensorDev_MainSecond: //  Main Second Sensor
        static LscMgrDefault singleton_mainsecond(ESensorDev_MainSecond, i4SensorIdx);
        _pLscMain2 = &singleton_mainsecond;
        LSC_LOG_END("ESensorDev_MainSecond(0x%08x), i4SensorIdx(%d)", _pLscMain2, i4SensorIdx);
        return _pLscMain2;
    case ESensorDev_Sub:        //  Sub Sensor
        static LscMgrDefault singleton_sub(ESensorDev_Sub, i4SensorIdx);
        _pLscSub = &singleton_sub;
        LSC_LOG_END("ESensorDev_Sub(0x%08x), i4SensorIdx(%d)", _pLscSub, i4SensorIdx);
        return _pLscSub;
    }
}

ILscMgr* 
LscMgrDefault::
getInstance(ESensorDev_T eSensorDev)
{
    switch (eSensorDev)
    {
    default:
    case ESensorDev_Main:       //  Main Sensor
        return _pLscMain;
    case ESensorDev_MainSecond: //  Main Second Sensor
        return _pLscMain2;
    case ESensorDev_Sub:        //  Sub Sensor
        return _pLscSub;
    }
}

LscMgrDefault::
LscMgrDefault(ESensorDev_T eSensorDev, MINT32 i4SensorIdx)
    : m_eSensorDev(eSensorDev)
    , m_i4SensorIdx(i4SensorIdx)
    , m_eSensorMode(ESensorMode_Preview)
    , m_ePrevSensorMode(ESensorMode_NUM)
    , m_u4NumSensorModes(0)
    , m_fgOnOff(MTRUE)
    , m_fgInit(MFALSE)
    , m_fgSensorCropInfoNull(MFALSE)
    , m_fgBypass1to3(MFALSE)
    , m_e1to3Flag(E_LSC_123_USE_CCT)
    , m_u4CTIdx(0)
    , m_u4Rto(32)
    , m_u4BufIdx(0)
    , m_u4LogEn(0)
    , m_pNvramOtp(ILscNvram::getInstance(eSensorDev))
    , m_pTsf(NULL)
    , m_bDumpSdblk(MFALSE)
{
    LSC_LOG("Enter LscMgrDefault");
}

LscMgrDefault::
~LscMgrDefault()
{
    m_pTsf->destroyInstance();
    LSC_LOG("Exit LscMgrDefault");
}

MBOOL
LscMgrDefault::
init()
{
    LSC_LOG_BEGIN("SensorDev(%d), byp123(%d)", m_eSensorDev, m_fgBypass1to3);
    MUINT32 u4Type = 0;
    GET_PROP("debug.lsc_mgr.log", "0", m_u4LogEn);
    GET_PROP("debug.lsc_mgr.type", "0", u4Type);

    // create buffers
    MINT32 i = 0;
    for (i = 0; i < SHADING_SUPPORT_CT_NUM; i++)
    {
        m_pLscBuf[i] = createBuf("");
    }
    for (i = 0; i < RING_BUF_NUM; i++)
    {
        ILscBuf* pBuf = createBuf("RingBuf");
        m_prLscBufRing[i] = pBuf;
        if (!pBuf)
        {
            LSC_ERR("Fail to create buffer");
        }
    }

    // resolution and reset tables
    if (0 == m_u4NumSensorModes)
    {
        // get resolution at the 1st time
        LSC_LOG("The 1st time, getSensorResolution");
        getSensorResolution();
        resetLscTbl();
    }
    else
    {
        LSC_LOG("Not the 1st time, show info only");
        showResolutionInfo();
    }

    // transformation flow
    loadTableFlow(MFALSE);

    // TSF
    m_pTsf = ILscTsf::createInstance(m_eSensorDev, static_cast<ILscTsf::E_LSC_TSF_TYPE_T>(u4Type));
    m_pTsf->init();
    m_pTsf->setOnOff(m_pNvramOtp->getTsfNvram()->TSF_CFG.isTsfEn ? MTRUE : MFALSE);

    // enable LSC
    setOnOff(getOnOff());

    m_fgInit = MFALSE;

    LSC_LOG_END();

    return MTRUE;
}

MBOOL
LscMgrDefault::
uninit()
{
    LSC_LOG_BEGIN("SensorDev(%d)", m_eSensorDev);

    m_pTsf->uninit();

    MINT32 i = 0;

    // destroy buffer pool.
    std::list<ILscBuf*>::iterator iter;
    for (iter = m_rBufPool.begin(); iter != m_rBufPool.end(); iter++)
    {
        if (*iter)
        {
            delete (*iter);
            *iter = NULL;
        }
    }
    m_rBufPool.clear();

    LSC_LOG_END("SensorDev(%d)", m_eSensorDev);

    return MTRUE;
}

MBOOL
LscMgrDefault::
resetLscTbl()
{
    // load table from nvram to tbl obj
    MUINT32 i, j;
    MINT32 i4W = m_rSensorCropWin[ESensorMode_Capture].u4W;
    MINT32 i4H = m_rSensorCropWin[ESensorMode_Capture].u4H;
    MINT32 i4GridX = m_pNvramOtp->getLscNvram()->GridXNum;
    MINT32 i4GridY = m_pNvramOtp->getLscNvram()->GridYNum;
    for (i = 0; i < m_u4NumSensorModes; i++)
    {
        for (j = 0; j < SHADING_SUPPORT_CT_NUM; j++)
        {
            ILscTbl& rTbl = m_rLscTbl[i][j];
            rTbl.setConfig(i4W, i4H, i4GridX, i4GridY);
            rTbl.setData(m_pNvramOtp->getLut((ESensorMode_T) i, j), rTbl.getSize());

            if (i == ESensorMode_Capture)
            {
                char dbgFile[256];
                sprintf(dbgFile, "/sdcard/shading/sensor_%d_cap_%d.log", m_eSensorDev, j);
                rTbl.dump(dbgFile);
                
                //sprintf(dbgFile, "/sdcard/shading/sensor_%d_cap_%d_gain.log", m_eSensorDev, j);
                //ILscTbl rToGain(ILscTbl::GAIN_FLOAT);
                //rTbl.convert(rToGain);
                //rToGain.dump(dbgFile);
            }
        }
    }
    return MTRUE;
}

const ILscTbl*
LscMgrDefault::
getLut(ESensorMode_T eSensorMode, MUINT32 u4CtIdx) const
{
    if (eSensorMode < ESensorMode_NUM && u4CtIdx < SHADING_SUPPORT_CT_NUM)
    {
        return &m_rLscTbl[eSensorMode][u4CtIdx];
    }
    else
    {
        return NULL;
    }
}

MBOOL
LscMgrDefault::
loadTableFlow(MBOOL fgForceRestore)
{
    MINT32 i4Opt123 = 0;

    GET_PROP("debug.lsc_mgr.opt123", "0", i4Opt123);

    LSC_LOG_BEGIN("SensorDev(%d), byp123(%d), i4Opt123(%d)", m_eSensorDev, m_fgBypass1to3, i4Opt123);

    if (i4Opt123 & 0x1 || fgForceRestore || m_fgBypass1to3)
    {
        resetLscTbl();
    }

    MBOOL fgBypass123 = (i4Opt123 & 0x2) ? MTRUE : MFALSE;
    if ((m_e1to3Flag != E_LSC_123_NO_OTP_OK) && (m_e1to3Flag != E_LSC_123_WITH_MTK_OTP_OK))
    {
        if (fgBypass123 || m_fgBypass1to3)
        {
            LSC_LOG("LSC 1-to-3 is bypassed for debug purpose");
            m_e1to3Flag = E_LSC_123_USE_CCT;
        }
        else
        {
            switch (m_pNvramOtp->getOtpState())
            {
            case ILscNvram::E_LSC_NO_OTP:
                if (1)
                {
                    LSC_LOG("No OTP: Do 1-to-3 from capture.");
                    // new path: only calibration in capture mode, and transform to the others.
                    if (doShadingTrfm())
                    {
                        //copyRawLscToNvram(CAL_DATA_LOAD+1);
                        m_e1to3Flag = E_LSC_123_NO_OTP_OK;
                    }
                    else
                    {
                        m_e1to3Flag = E_LSC_123_NO_OTP_ERR;
                    }
                }
                else
                {
                    LSC_LOG("No OTP: Use CCT calibrated table");
                    m_e1to3Flag = E_LSC_123_USE_CCT;
                }
                
                break;
            case ILscNvram::E_LSC_WITH_MTK_OTP:
                LSC_LOG("MTK OTP: Do 1-to-3 align + transform");
                if (doShadingAlign())
                {
                    if (doShadingTrfm())
                    {
                        //copyRawLscToNvram(CAL_DATA_LOAD);
                        m_e1to3Flag = E_LSC_123_WITH_MTK_OTP_OK;
                    }
                    else
                    {
                        m_e1to3Flag = E_LSC_123_WITH_MTK_OTP_ERR2;
                    }
                }
                else
                {
                    m_e1to3Flag = E_LSC_123_WITH_MTK_OTP_ERR1;
                }
                
                break;
            default:
            case ILscNvram::E_LSC_OTP_ERROR:
                m_e1to3Flag = E_LSC_123_OTP_ERR;
                break;
            }
        }
    }
    else
    {
        LSC_LOG("123 Tables already exist in LscMgrDefault (0x%08x)", m_e1to3Flag);
    }
    
    LSC_LOG_END();

    return MTRUE;
}

MBOOL
LscMgrDefault::
getSensorResolution()
{
#ifdef USING_MTK_LDVT
    IHalSensorList*const pIHalSensorList = TS_FakeSensorList::getTestModel();
#else
    IHalSensorList*const pIHalSensorList = IHalSensorList::get();
#endif

    SensorStaticInfo rSensorStaticInfo;
    IHalSensor* pIHalSensor = pIHalSensorList->createSensor("lsc_mgr", m_i4SensorIdx);
    pIHalSensorList->querySensorStaticInfo(m_eSensorDev, &rSensorStaticInfo);

#ifdef USING_MTK_LDVT
    MINT32 u4NumSensorModes = 2;
#else
    MINT32 u4NumSensorModes = MIN(rSensorStaticInfo.SensorModeNum, (MINT32)ESensorMode_NUM);
#endif
    m_rSensorResolution.u4SensorPreviewWidth  = rSensorStaticInfo.previewWidth;
    m_rSensorResolution.u4SensorPreviewHeight = rSensorStaticInfo.previewHeight;
    m_rSensorResolution.u4SensorCaptureWidth  = rSensorStaticInfo.captureWidth;
    m_rSensorResolution.u4SensorCaptureHeight = rSensorStaticInfo.captureHeight;
    m_rSensorResolution.u4SensorVideoWidth    = rSensorStaticInfo.videoWidth;
    m_rSensorResolution.u4SensorVideoHeight   = rSensorStaticInfo.videoHeight;
    m_rSensorResolution.u4SensorVideo1Width   = rSensorStaticInfo.video1Width;
    m_rSensorResolution.u4SensorVideo1Height  = rSensorStaticInfo.video1Height;
    m_rSensorResolution.u4SensorVideo2Width   = rSensorStaticInfo.video2Width;
    m_rSensorResolution.u4SensorVideo2Height  = rSensorStaticInfo.video2Height;
    m_rSensorResolution.u4SensorCustom1Width  = rSensorStaticInfo.SensorCustom1Width; 
    m_rSensorResolution.u4SensorCustom1Height = rSensorStaticInfo.SensorCustom1Height;
    m_rSensorResolution.u4SensorCustom2Width  = rSensorStaticInfo.SensorCustom2Width;
    m_rSensorResolution.u4SensorCustom2Height = rSensorStaticInfo.SensorCustom2Height;
    m_rSensorResolution.u4SensorCustom3Width  = rSensorStaticInfo.SensorCustom3Width;
    m_rSensorResolution.u4SensorCustom3Height = rSensorStaticInfo.SensorCustom3Height;
    m_rSensorResolution.u4SensorCustom4Width  = rSensorStaticInfo.SensorCustom4Width;
    m_rSensorResolution.u4SensorCustom4Height = rSensorStaticInfo.SensorCustom4Height;
    m_rSensorResolution.u4SensorCustom5Width  = rSensorStaticInfo.SensorCustom5Width;
    m_rSensorResolution.u4SensorCustom5Height = rSensorStaticInfo.SensorCustom5Height;

    LSC_LOG("u4NumSensorModes(%d)", u4NumSensorModes);
    LSC_LOG("i4SensorPreviewWidth (%d)", m_rSensorResolution.u4SensorPreviewWidth );
    LSC_LOG("i4SensorPreviewHeight(%d)", m_rSensorResolution.u4SensorPreviewHeight);
    LSC_LOG("i4SensorCaptureWidth (%d)", m_rSensorResolution.u4SensorCaptureWidth );
    LSC_LOG("i4SensorCaptureHeight(%d)", m_rSensorResolution.u4SensorCaptureHeight);
    LSC_LOG("i4SensorVideoWidth   (%d)", m_rSensorResolution.u4SensorVideoWidth   );
    LSC_LOG("i4SensorVideoHeight  (%d)", m_rSensorResolution.u4SensorVideoHeight  );
    LSC_LOG("i4SensorVideo1Width  (%d)", m_rSensorResolution.u4SensorVideo1Width  );    
    LSC_LOG("i4SensorVideo1Height (%d)", m_rSensorResolution.u4SensorVideo1Height );
    LSC_LOG("i4SensorVideo2Width  (%d)", m_rSensorResolution.u4SensorVideo2Width  );
    LSC_LOG("i4SensorVideo2Height (%d)", m_rSensorResolution.u4SensorVideo2Height );
    LSC_LOG("i4SensorCustom1Width (%d)", m_rSensorResolution.u4SensorCustom1Width );
    LSC_LOG("i4SensorCustom1Height(%d)", m_rSensorResolution.u4SensorCustom1Height);
    LSC_LOG("i4SensorCustom2Width (%d)", m_rSensorResolution.u4SensorCustom2Width );
    LSC_LOG("i4SensorCustom2Height(%d)", m_rSensorResolution.u4SensorCustom2Height);
    LSC_LOG("i4SensorCustom3Width (%d)", m_rSensorResolution.u4SensorCustom3Width );
    LSC_LOG("i4SensorCustom3Height(%d)", m_rSensorResolution.u4SensorCustom3Height);
    LSC_LOG("i4SensorCustom4Width (%d)", m_rSensorResolution.u4SensorCustom4Width );
    LSC_LOG("i4SensorCustom4Height(%d)", m_rSensorResolution.u4SensorCustom4Height);
    LSC_LOG("i4SensorCustom5Width (%d)", m_rSensorResolution.u4SensorCustom5Width );
    LSC_LOG("i4SensorCustom5Height(%d)", m_rSensorResolution.u4SensorCustom5Height);

    MINT32 i;
    for (i = 0; i < u4NumSensorModes; i++)
    {
        MUINT32 scenario = i;
        SensorCropWinInfo rSensorCropInfo;

        ::memset(&rSensorCropInfo, 0, sizeof(SensorCropWinInfo));

        MINT32 err = pIHalSensor->sendCommand(m_eSensorDev,
                SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,
                (MUINTPTR)&scenario,
                (MUINTPTR)&rSensorCropInfo,
                0);
                
        if (err != 0 || rSensorCropInfo.full_w == 0)
        {
            LSC_LOG("SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO is not implemented well (%d), use predefined rule.", err);
            m_fgSensorCropInfoNull = MTRUE;
            break;
        }

        SensorCropInfo_T rCrop;
        rCrop.w0 = rSensorCropInfo.full_w;
        rCrop.h0 = rSensorCropInfo.full_h;
        rCrop.x1 = rSensorCropInfo.x0_offset;
        rCrop.y1 = rSensorCropInfo.y0_offset;
        rCrop.w1 = rSensorCropInfo.w0_size;
        rCrop.h1 = rSensorCropInfo.h0_size;
        rCrop.w1r= rSensorCropInfo.scale_w;
        rCrop.h1r= rSensorCropInfo.scale_h;
        rCrop.x2 = rSensorCropInfo.x1_offset + rSensorCropInfo.x2_tg_offset;
        rCrop.y2 = rSensorCropInfo.y1_offset + rSensorCropInfo.y2_tg_offset;
        rCrop.w2 = rSensorCropInfo.w2_tg_size;
        rCrop.h2 = rSensorCropInfo.h2_tg_size;
        rCrop.u4W= rSensorCropInfo.w2_tg_size;
        rCrop.u4H= rSensorCropInfo.h2_tg_size;
        LSC_LOG("SensorMode(%d), full_0(%d,%d) crop1(%d,%d,%d,%d) resize(%d,%d) crop2(%d,%d,%d,%d) final size(%d,%d)",
            scenario, rCrop.w0, rCrop.h0, rCrop.x1, rCrop.y1, rCrop.w1, rCrop.h1, rCrop.w1r, rCrop.h1r,
            rCrop.x2, rCrop.y2, rCrop.w2, rCrop.h2, rCrop.u4W, rCrop.u4H);
        m_rSensorCropWin[i] = rCrop;
    }

    pIHalSensor->destroyInstance("lsc_mgr");

    m_rSensorCropWin[ESensorMode_Preview].u4W    = rSensorStaticInfo.previewWidth;
    m_rSensorCropWin[ESensorMode_Preview].u4H    = rSensorStaticInfo.previewHeight;
    m_rSensorCropWin[ESensorMode_Capture].u4W    = rSensorStaticInfo.captureWidth;
    m_rSensorCropWin[ESensorMode_Capture].u4H    = rSensorStaticInfo.captureHeight;
    m_rSensorCropWin[ESensorMode_Video].u4W      = rSensorStaticInfo.videoWidth;
    m_rSensorCropWin[ESensorMode_Video].u4H      = rSensorStaticInfo.videoHeight;
    m_rSensorCropWin[ESensorMode_SlimVideo1].u4W = rSensorStaticInfo.video1Width;
    m_rSensorCropWin[ESensorMode_SlimVideo1].u4H = rSensorStaticInfo.video1Height;
    m_rSensorCropWin[ESensorMode_SlimVideo2].u4W = rSensorStaticInfo.video2Width;
    m_rSensorCropWin[ESensorMode_SlimVideo2].u4H = rSensorStaticInfo.video2Height; 
    m_rSensorCropWin[ESensorMode_Custom1].u4W    = rSensorStaticInfo.SensorCustom1Width;
    m_rSensorCropWin[ESensorMode_Custom1].u4H    = rSensorStaticInfo.SensorCustom1Height;
    m_rSensorCropWin[ESensorMode_Custom2].u4W    = rSensorStaticInfo.SensorCustom2Width;
    m_rSensorCropWin[ESensorMode_Custom2].u4H    = rSensorStaticInfo.SensorCustom2Height;
    m_rSensorCropWin[ESensorMode_Custom3].u4W    = rSensorStaticInfo.SensorCustom3Width;
    m_rSensorCropWin[ESensorMode_Custom3].u4H    = rSensorStaticInfo.SensorCustom3Height;
    m_rSensorCropWin[ESensorMode_Custom4].u4W    = rSensorStaticInfo.SensorCustom4Width;
    m_rSensorCropWin[ESensorMode_Custom4].u4H    = rSensorStaticInfo.SensorCustom4Height;
    m_rSensorCropWin[ESensorMode_Custom5].u4W    = rSensorStaticInfo.SensorCustom5Width;
    m_rSensorCropWin[ESensorMode_Custom5].u4H    = rSensorStaticInfo.SensorCustom5Height; 

    MUINT32 u4GridX = m_pNvramOtp->getLscNvram()->GridXNum;
    MUINT32 u4GridY = m_pNvramOtp->getLscNvram()->GridYNum;
    for (i = 0; i < u4NumSensorModes; i++)
    {
        convertSensorCrop(!m_fgSensorCropInfoNull, m_rSensorCropWin[ESensorMode_Capture], m_rSensorCropWin[i], m_rTransformCfg[i]);
        m_rTransformCfg[i].u4GridX = u4GridX;
        m_rTransformCfg[i].u4GridY = u4GridY;
    }

    m_u4NumSensorModes = u4NumSensorModes;
    
    return MTRUE;
}

MBOOL
LscMgrDefault::
convertSensorCrop(MBOOL fgWithSensorCropInfo, const SensorCropInfo_T& rFullInfo, const SensorCropInfo_T& rCropInfo, ILscTbl::TransformCfg_T& rCropCfg)
{            
    if (!fgWithSensorCropInfo)
    {
        MFLOAT fScale = 1.0f;
        
        // fit horizontal
        if (rFullInfo.u4W < rFullInfo.u4H || rCropInfo.u4W < rCropInfo.u4H)
        {
            LSC_ERR("Impossible Case.");
        }
        
        LSC_LOG("No sensor crop info, use magic rule");
        fScale = (MFLOAT) rCropInfo.u4W / rFullInfo.u4W;
        if (rCropInfo.u4W*10 > rFullInfo.u4W*7)
        {
            // crop case
            LSC_LOG("Crop case, scale(%3.3f)", fScale);
            rCropCfg.u4ResizeW  = rFullInfo.u4W;
            rCropCfg.u4ResizeH  = rFullInfo.u4H;
            rCropCfg.u4W        = rCropInfo.u4W;
            rCropCfg.u4H        = rCropInfo.u4H;
            rCropCfg.u4X        = (rCropCfg.u4ResizeW >= rCropCfg.u4W) ? ((rCropCfg.u4ResizeW - rCropCfg.u4W) / 2) : 0;
            rCropCfg.u4Y        = (rCropCfg.u4ResizeH >= rCropCfg.u4H) ? ((rCropCfg.u4ResizeH - rCropCfg.u4H) / 2) : 0;
        }
        else
        {
            // resize case   
            LSC_LOG("Resize case, scale(%3.3f)", fScale);
            rCropCfg.u4ResizeW = rCropInfo.u4W;
            rCropCfg.u4ResizeH = rFullInfo.u4H*fScale;
            rCropCfg.u4W       = rCropInfo.u4W;
            rCropCfg.u4H       = rCropInfo.u4H;
            rCropCfg.u4X       = 0;
            rCropCfg.u4Y       = (rCropCfg.u4ResizeH >= rCropCfg.u4H) ? ((rCropCfg.u4ResizeH - rCropCfg.u4H) / 2) : 0;
        }
    }
    else
    {
        // calculate crop with respect to capture
        // rw = w1r/w1, rh = h1r/h1,
        // x2'= x1*r+x2
        // x2_cap'' = x2c' * rw_scn / rw_cap
        MFLOAT rwc = (MFLOAT) rFullInfo.w1r / rFullInfo.w1;
        MFLOAT rhc = (MFLOAT) rFullInfo.h1r / rFullInfo.h1;
        MFLOAT x2c_ = (MFLOAT) rFullInfo.x1*rwc + rFullInfo.x2;
        MFLOAT y2c_ = (MFLOAT) rFullInfo.y1*rhc + rFullInfo.y2;
        LSC_LOG("rwc(%3.3f), rhc(%3.3f), x2c_(%3.3f), y2c_(%3.3f)", rwc, rhc, x2c_, y2c_);

        MFLOAT rws = (MFLOAT) rCropInfo.w1r / rCropInfo.w1;
        MFLOAT rhs = (MFLOAT) rCropInfo.h1r / rCropInfo.h1;
        MFLOAT x2s_ = (MFLOAT) rCropInfo.x1*rws + rCropInfo.x2;
        MFLOAT y2s_ = (MFLOAT) rCropInfo.y1*rhs + rCropInfo.y2;
        LSC_LOG("rws(%3.3f), rhs(%3.3f), x2s_(%3.3f), y2s_(%3.3f)", rws, rhs, x2s_, y2s_);

        MFLOAT x2c__ = x2c_ * rws / rwc;
        MFLOAT y2c__ = y2c_ * rhs / rhc;
       
        MFLOAT x_sc = x2s_ - x2c__;
        MFLOAT y_sc = y2s_ - y2c__;
        MFLOAT out_w = (MFLOAT) rFullInfo.w2 * rws / rwc;
        MFLOAT out_h = (MFLOAT) rFullInfo.h2 * rhs / rhc;
        MFLOAT w2s = rCropInfo.w2;
        MFLOAT h2s = rCropInfo.h2;

        LSC_LOG("x2c__(%3.3f), y2c__(%3.3f), x_sc(%3.3f), y_sc(%3.3f), out_w(%3.3f), out_h(%3.3f)",
            x2c__, y2c__, x_sc, y_sc, out_w, out_h);

        if (x_sc < 0.0f)
        {
            LSC_LOG("Negative x_sc(%3.3f)", x_sc);
            x_sc = 0.0f;
        }
        
        if ((MFLOAT)x_sc + w2s > out_w)
        {
            LSC_LOG("Exceed right side, x_sc(%3.3f), w2s(%d), out_w(%3.3f)", x_sc, w2s, out_w);
            //w2s = out_w - x_sc;
            x_sc = 0.0f;
            w2s = out_w;
        }

        if (y_sc < 0.0f)
        {
            LSC_LOG("Negative y_sc(%3.3f)", y_sc);
            y_sc = 0.0f;
        }
        
        if ((MFLOAT)y_sc + h2s > out_h)
        {
            LSC_LOG("Exceed bottom side, y_sc(%3.3f), h2s(%d), out_h(%3.3f)", y_sc, h2s, out_h);
            //h2s = out_h - y_sc;
            y_sc = 0.0f;
            h2s = out_h;
        }

        rCropCfg.u4ResizeW = ROUND(out_w);
        rCropCfg.u4ResizeH = ROUND(out_h);
        rCropCfg.u4W       = w2s;
        rCropCfg.u4H       = h2s;
        rCropCfg.u4X       = x_sc;
        rCropCfg.u4Y       = y_sc;
    }
    
    LSC_LOG("Crop CFG for lib ImgWH(%d,%d), outWH(%d,%d), Crop(%d,%d,%d,%d)",
        rFullInfo.w2, rFullInfo.h2, rCropCfg.u4ResizeW, rCropCfg.u4ResizeH,
        rCropCfg.u4X, rCropCfg.u4Y, rCropCfg.u4W, rCropCfg.u4H);

    return MTRUE;
}

MBOOL
LscMgrDefault::
showResolutionInfo()
{
    UINT32 i, u4NumSensorModes;
    u4NumSensorModes = m_u4NumSensorModes;

    LSC_LOG("u4NumSensorModes(%d)", u4NumSensorModes);
    LSC_LOG("i4SensorPreviewWidth (%d)", m_rSensorResolution.u4SensorPreviewWidth);
    LSC_LOG("i4SensorPreviewHeight(%d)", m_rSensorResolution.u4SensorPreviewHeight);
    LSC_LOG("i4SensorCaptureWidth (%d)", m_rSensorResolution.u4SensorCaptureWidth);
    LSC_LOG("i4SensorCaptureHeight(%d)", m_rSensorResolution.u4SensorCaptureHeight);
    LSC_LOG("i4SensorVideoWidth   (%d)", m_rSensorResolution.u4SensorVideoWidth);
    LSC_LOG("i4SensorVideoHeight  (%d)", m_rSensorResolution.u4SensorVideoHeight);
    LSC_LOG("i4SensorVideo1Width  (%d)", m_rSensorResolution.u4SensorVideo1Width);    
    LSC_LOG("i4SensorVideo1Height (%d)", m_rSensorResolution.u4SensorVideo1Height);
    LSC_LOG("i4SensorVideo2Width  (%d)", m_rSensorResolution.u4SensorVideo2Width);
    LSC_LOG("i4SensorVideo2Height (%d)", m_rSensorResolution.u4SensorVideo2Height);
    LSC_LOG("i4SensorCustom1Width (%d)", m_rSensorResolution.u4SensorCustom1Width );
    LSC_LOG("i4SensorCustom1Height(%d)", m_rSensorResolution.u4SensorCustom1Height);
    LSC_LOG("i4SensorCustom2Width (%d)", m_rSensorResolution.u4SensorCustom2Width );
    LSC_LOG("i4SensorCustom2Height(%d)", m_rSensorResolution.u4SensorCustom2Height);
    LSC_LOG("i4SensorCustom3Width (%d)", m_rSensorResolution.u4SensorCustom3Width );
    LSC_LOG("i4SensorCustom3Height(%d)", m_rSensorResolution.u4SensorCustom3Height);
    LSC_LOG("i4SensorCustom4Width (%d)", m_rSensorResolution.u4SensorCustom4Width );
    LSC_LOG("i4SensorCustom4Height(%d)", m_rSensorResolution.u4SensorCustom4Height);
    LSC_LOG("i4SensorCustom5Width (%d)", m_rSensorResolution.u4SensorCustom5Width );
    LSC_LOG("i4SensorCustom5Height(%d)", m_rSensorResolution.u4SensorCustom5Height);

    if (!m_fgSensorCropInfoNull)
    {
        for (i = 0; i < u4NumSensorModes; i++)
        {
            const SensorCropInfo_T& rCrop = m_rSensorCropWin[i];
            LSC_LOG("SensorMode(%d), full_0(%d,%d) crop1(%d,%d,%d,%d) resize(%d,%d) crop2(%d,%d,%d,%d) final size(%d,%d)",
                i, rCrop.w0, rCrop.h0, rCrop.x1, rCrop.y1, rCrop.w1, rCrop.h1, rCrop.w1r, rCrop.h1r,
                rCrop.x2, rCrop.y2, rCrop.w2, rCrop.h2, rCrop.u4W, rCrop.u4H);
        }
    }

    for (i = 0; i < u4NumSensorModes; i++)
    {
        const ILscTbl::TransformCfg_T& rCropCfg = m_rTransformCfg[i];
        LSC_LOG("Crop CFG for lib ImgWH(%d,%d), outWH(%d,%d), Crop(%d,%d,%d,%d)",
            m_rSensorCropWin[ESensorMode_Capture].w2, m_rSensorCropWin[ESensorMode_Capture].h2, rCropCfg.u4ResizeW, rCropCfg.u4ResizeH,
        rCropCfg.u4X, rCropCfg.u4Y, rCropCfg.u4W, rCropCfg.u4H);
    }


    return MTRUE;
}

MBOOL
LscMgrDefault::
getResolution(ESensorMode_T eSensorMode, SensorCropInfo_T& rInfo)
{
    if (eSensorMode >= ESensorMode_NUM)
    {
        LSC_ERR("Sensor Mode(%d) doesn't exist!", eSensorMode);
        rInfo = m_rSensorCropWin[ESensorMode_Preview];
        return MFALSE;
    }
    
    rInfo = m_rSensorCropWin[eSensorMode];
    
    LSC_LOG("eSensorMode(%d), W(%d), H(%d)", eSensorMode, rInfo.u4W, rInfo.u4H);
    
    return MTRUE;
}

MBOOL
LscMgrDefault::
getResolution(ELscScenario_T eScn, SensorCropInfo_T& rInfo)
{
    if (eScn >= LSC_SCENARIO_NUM)
    {
        LSC_ERR("eScn(%d) >= %d", eScn, LSC_SCENARIO_NUM);
        eScn = LSC_SCENARIO_PRV;
    }

    rInfo = m_rSensorCropWin[eScn];
    LSC_LOG("eScn(%d), W(%d), H(%d)", eScn, rInfo.u4W, rInfo.u4H);
    
    return MTRUE;
}

#if 0
ILscMgr::ELscScenario_T
LscMgrDefault::
getLscScenario(ESensorMode_T eSensorMode)
{
    if (eSensorMode >= ESensorMode_NUM)
    {
        LSC_ERR("eSensorMode(%d) >= %d", eSensorMode, ESensorMode_NUM);
        eSensorMode = ESensorMode_Preview;
    }
    
    return static_cast<ILscMgr::ELscScenario_T>(eSensorMode);
}
#endif

MBOOL
LscMgrDefault::
doShadingAlign()
{
    LSC_LOG_BEGIN("MTK 1-to-3 Unit/Golden Align");
 
    MBOOL fgRet = MTRUE;
    MINT32 j = 0;
    const ILscTbl* pGolden = m_pNvramOtp->getGolden();
    const ILscTbl* pUnit = m_pNvramOtp->getUnit();

    for (j = 0; j < SHADING_SUPPORT_CT_NUM; j++)
    {
        MINT32 i4Ret = 0;
        ILscTbl rInput(m_rLscTbl[LSC_SCENARIO_CAP][j]); // make a copy in case of error
        i4Ret = ILscTbl::shadingAlign(*pGolden, *pUnit, rInput, m_rLscTbl[LSC_SCENARIO_CAP][j]);
        if (0 != i4Ret)
        {
            fgRet = MFALSE;
            LSC_ERR("Fail to do shading align (%d), ct(%d), restore table", i4Ret, j);
            m_rLscTbl[LSC_SCENARIO_CAP][j] = rInput;
        }
        else
        {
            // debug dump file
            char dbgFile[256];
            sprintf(dbgFile, "/sdcard/shading/sensor_%d_align_cap_%d.log", m_eSensorDev, j);
            m_rLscTbl[LSC_SCENARIO_CAP][j].dump(dbgFile);
        }
    }

    LSC_LOG_END();

    return fgRet;
}

MBOOL
LscMgrDefault::
doShadingTrfm()
{
    LSC_LOG_BEGIN("MTK 1-to-3 Crop Transform");
    
    ELscScenario_T eLscScn;
    for (MINT32 i4Scn = 0; i4Scn < (MINT32)m_u4NumSensorModes; i4Scn++)
    {
        eLscScn = (ELscScenario_T) i4Scn;
        if (eLscScn == LSC_SCENARIO_CAP)
            continue;

        const ILscTbl::TransformCfg_T& rTrfm = m_rTransformCfg[i4Scn];
        for (MUINT32 j = 0; j < SHADING_SUPPORT_CT_NUM; j++)
        {
            ILscTbl& rCapTbl = m_rLscTbl[LSC_SCENARIO_CAP][j];
            ILscTbl& rTarget = m_rLscTbl[i4Scn][j];
            ILscTbl rTmpTbl = m_rLscTbl[i4Scn][j];  // make a copy in case of error

            LSC_LOG("Input (%d x %d) -> Output (%d x %d) -> X(%d), Y(%d), (%d x %d)",
            rCapTbl.getConfig().i4ImgWd, rCapTbl.getConfig().i4ImgHt, rTrfm.u4ResizeW, rTrfm.u4ResizeH,
            rTrfm.u4X, rTrfm.u4Y, rTrfm.u4W, rTrfm.u4H);

            MBOOL fgSuccess = rCapTbl.cropOut(rTrfm, rTarget);
            if (!fgSuccess)
            {
                LSC_ERR("Fail to do shading transform eScn(%d) ct(%d), restore table", i4Scn, j);
                rTarget = rTmpTbl;
            }
            else
            {
                // debug dump file
                char dbgFile[256];
                sprintf(dbgFile, "/sdcard/shading/sensor_%d_trfm_%d_%d.log", m_eSensorDev, i4Scn, j);
                m_rLscTbl[LSC_SCENARIO_CAP][j].dump(dbgFile);
            }
        }
    }

    LSC_LOG_END();

    return MTRUE;
}

MBOOL
LscMgrDefault::
setSensorMode(ESensorMode_T eSensorMode, MUINT32 u4Width, MUINT32 u4Height, MBOOL fgForce)
{
    ESensorMode_T ePrevSensorMode;

    ePrevSensorMode = m_eSensorMode;

    if (eSensorMode != ePrevSensorMode || m_fgInit == MFALSE || fgForce)
    {
        m_fgInit = MTRUE;
        m_eSensorMode = eSensorMode;
        m_ePrevSensorMode = ePrevSensorMode;

        // update config
        LSC_LOG("update config for eLscScn(%d), fgForce(%d)", eSensorMode, fgForce);

        // load table to dma buffer
        MINT32 j;
        for (j = 0; j < SHADING_SUPPORT_CT_NUM; j++)
        {
            ILscTbl& rTbl = m_rLscTbl[eSensorMode][j];
            ILscTbl::Config rCfg = rTbl.getConfig();
            LSC_LOG("Grid(%d,%d) WxH(%d,%d)->(%d,%d)", rCfg.i4GridX, rCfg.i4GridY, rCfg.i4ImgWd, rCfg.i4ImgHt, u4Width, u4Height);
            rTbl.setConfig(u4Width, u4Height, rCfg.i4GridX, rCfg.i4GridY);
            rTbl.toBuf(*m_pLscBuf[j]);
        }
#if 0
        // modify trfm cfg due to frontal bin
        SensorCropInfo_T rInfo;
        getResolution(eSensorMode, rInfo);
        m_rTransformCfg[eSensorMode].u4ResizeW = m_rTransformCfg[eSensorMode].u4ResizeW * u4Width / rInfo.u4W;
        m_rTransformCfg[eSensorMode].u4ResizeH = m_rTransformCfg[eSensorMode].u4ResizeH * u4Height / rInfo.u4H;
        m_rTransformCfg[eSensorMode].u4X = m_rTransformCfg[eSensorMode].u4X * u4Width / rInfo.u4W;
        m_rTransformCfg[eSensorMode].u4Y = m_rTransformCfg[eSensorMode].u4Y * u4Height / rInfo.u4H;
        m_rTransformCfg[eSensorMode].u4W = m_rTransformCfg[eSensorMode].u4W * u4Width / rInfo.u4W;
        m_rTransformCfg[eSensorMode].u4H = m_rTransformCfg[eSensorMode].u4H * u4Height / rInfo.u4H;
#endif
        m_pTsf->setConfig(eSensorMode, u4Width, u4Height, fgForce);

#if 0
        calSl2Config(rCrop, m_rSl2Cfg);
#endif
    }
   
    return MTRUE;
}

ESensorMode_T
LscMgrDefault::
getSensorMode() const
{
    return m_eSensorMode;
}

const ILscTbl::TransformCfg_T&
LscMgrDefault::
getTransformCfg(ESensorMode_T eSensorMode) const
{
    return m_rTransformCfg[eSensorMode];
}

MUINT32
LscMgrDefault::
getCTIdx() 
{
    MBOOL fgLogEn = (m_u4LogEn & EN_LSC_LOG_GET_CT) ? MTRUE : MFALSE;
    if (SHADING_SUPPORT_CT_NUM <= m_u4CTIdx)
    {
        LSC_ERR("m_u4CTIdx(%d) exceeds", m_u4CTIdx);
    }

    MY_LOG_IF(fgLogEn, "[%s] CT(%d)\n", __FUNCTION__, m_u4CTIdx);
    return m_u4CTIdx;
}

MBOOL
LscMgrDefault::
setCTIdx(MUINT32 const u4CTIdx) 
{
    MUINT32 _u4CtIdx;
    MINT32 dbg_ct = 0;
    MBOOL fgLogEn = (m_u4LogEn & EN_LSC_LOG_SET_CT) ? MTRUE : MFALSE;
    GET_PROP("debug.lsc_mgr.ct", "-1", dbg_ct);

    if (dbg_ct != -1)
    {
        LSC_LOG("DEBUG CT(%d)", dbg_ct);
        _u4CtIdx = dbg_ct;
    }
    else
    {
        _u4CtIdx = u4CTIdx;
    }

    if (SHADING_SUPPORT_CT_NUM <= _u4CtIdx) 
    {
        LSC_LOG("_u4CtIdx(%d) exceeds", _u4CtIdx);
        return MFALSE;
    }

    m_u4CTIdx = _u4CtIdx;
    MY_LOG_IF(fgLogEn, "[%s] CT(%d)", __FUNCTION__, _u4CtIdx);

    return MTRUE;
}

MBOOL
LscMgrDefault::
setRatio(MUINT32 u4Ratio)
{
    m_u4Rto = u4Ratio > 32 ? 32 : u4Ratio;
    return MTRUE;
}

MUINT32
LscMgrDefault::
getRatio() const
{
    return m_u4Rto;
}

MBOOL
LscMgrDefault::
getOnOff(void) const
{
#if 0
    MINT32 dbg_enable = 0;
    GET_PROP("debug.lsc_mgr.enable", "-1", dbg_enable);   

    if (dbg_enable != -1)
    {
        // overwrite for debug
        m_fgOnOff = dbg_enable == 0 ? MFALSE : MTRUE;
    }
#endif
    return m_fgOnOff;
}

MVOID
LscMgrDefault::
setOnOff(MBOOL fgOnOff)
{
    m_fgOnOff = fgOnOff;
    ISP_MGR_LSC_T::getInstance(m_eSensorDev).enableLsc(fgOnOff);
}

MVOID
LscMgrDefault::
updateLsc()
{
    MBOOL fgLogEn = (m_u4LogEn & EN_LSC_LOG_UPDATE) ? MTRUE : MFALSE;
    MBOOL fgUserTbl = MFALSE;
    MUINT32 u4PhyAddr, u4CTIdx;
    MUINT8* pVirAddr;

    u4CTIdx = getCTIdx();
    u4CTIdx = u4CTIdx >= 3 ? 3 : u4CTIdx;

    // ratio
    ILscTbl rTmp(ILscTbl::HWTBL);
    m_rLscTbl[m_eSensorMode][u4CTIdx].getRaTbl(m_u4Rto, rTmp);

    // put to isp_mgr buf
    //ISP_MGR_LSC_T::getInstance(m_eSensorDev).enableLsc(getOnOff());
    syncTbl(rTmp);
    //syncBuf(m_pLscBuf[u4CTIdx]);

    // sl2
    ISP_NVRAM_SL2_T rSl2Cfg;
    MBOOL fgSl2OnOff = 0;
    MINT32 i4Idx = 0;  
    MINT32 dbg_enable = 0;
    GET_PROP("debug.lsc_mgr.sl2", "-1", dbg_enable);
    MINT32 i4Case = (dbg_enable != -1) ? dbg_enable : isEnableSL2(m_eSensorDev);
    
    switch (i4Case)
    {
    default:
    case 0:
        fgSl2OnOff = 0;
        i4Idx = m_eSensorMode*SHADING_SUPPORT_CT_NUM+u4CTIdx;
        if (i4Idx >= NVRAM_SL2_TBL_NUM)
        {
            LSC_ERR("0: SL2 idx(%d) exceeds NVRAM_SL2_TBL_NUM", i4Idx);
            return;
        }
        break;
    case 1:
        fgSl2OnOff = 1;
        i4Idx = m_eSensorMode*SHADING_SUPPORT_CT_NUM+u4CTIdx;
        if (i4Idx >= NVRAM_SL2_TBL_NUM)
        {
            LSC_ERR("1: SL2 idx(%d) exceeds NVRAM_SL2_TBL_NUM", i4Idx);
            return;
        }
        break;
    case 2:
        fgSl2OnOff = 1;
        i4Idx = m_eSensorMode*SHADING_SUPPORT_CT_NUM+getTSFD65Idx();
        if (i4Idx >= NVRAM_SL2_TBL_NUM)
        {
            LSC_ERR("2: SL2 idx(%d) exceeds NVRAM_SL2_TBL_NUM", i4Idx);
            return;
        }
        break;
    }

//    if (getTsfOnOff())
//        rSl2Cfg = m_rSl2Cfg;
//    else
        rSl2Cfg = m_pNvramOtp->getIspNvram()->ISPRegs.SL2[i4Idx];

    MY_LOG_IF(fgLogEn, "[%s] SL2 OnOff(%d), Idx(%d)", __FUNCTION__, fgSl2OnOff, i4Idx);
    MY_LOG_IF(fgLogEn, "[%s] SL2 cen(0x%08x), rr0(0x%08x), rr1(0x%08x), gain(0x%08x)",
        __FUNCTION__, rSl2Cfg.cen, rSl2Cfg.rr_con0, rSl2Cfg.rr_con1, rSl2Cfg.gain);
    //if (NSIspTuning::IspTuningMgr::getInstance().getOperMode(m_eSensorDev) != NSIspTuning::EOperMode_Meta)
    {
        ISP_MGR_SL2_T::getInstance(m_eSensorDev).setEnable(fgSl2OnOff);
        //ISP_MGR_SL2_T::getInstance(m_eSensorDev).put(rSl2Cfg);
    }
}

MVOID
LscMgrDefault::
setTsfOnOff(MBOOL fgOnOff)
{
    m_pTsf->setOnOff(fgOnOff);
}

MBOOL
LscMgrDefault::
getTsfOnOff() const
{
    return m_pTsf->getOnOff();
}

MVOID
LscMgrDefault::
updateTsf(const TSF_INPUT_INFO_T& rInputInfo)
{
    m_pTsf->update(rInputInfo);
}

ILscBuf*
LscMgrDefault::
createBuf(const char* name)
{
    MUINT32 u4BufCount = m_rBufPool.size();
    ILscBuf* pBuf = new ILscBuf(m_eSensorDev, u4BufCount, name);
    if (pBuf)
    {
        m_rBufPool.push_back(pBuf);
        LSC_LOG("OK: pBuf(%p), poolsize(%d)", pBuf, m_rBufPool.size());
    }
    else
    {
        LSC_ERR("NG: poolsize(%d)", u4BufCount);
    }
    return pBuf;
}

MBOOL
LscMgrDefault::
destroyBuf(ILscBuf* pBuf)
{
    if (pBuf)
    {
        std::list<ILscBuf*>::iterator iter;
        for (iter = m_rBufPool.begin(); iter != m_rBufPool.end(); iter++)
        {
            if (pBuf == *iter)
            {
                LSC_LOG("OK: pBuf(%p), poolsize(%d)", pBuf, m_rBufPool.size());
                delete pBuf;
                *iter = NULL;
                m_rBufPool.erase(iter);
                return MTRUE;
            }
        }
    }
    return MFALSE;
}

MBOOL
LscMgrDefault::
syncBuf(ILscBuf* pBuf)
{
    ISP_MGR_LSC_T::getInstance(m_eSensorDev).putBuf(*pBuf);
    m_pCurrentBuf = pBuf;
    return MTRUE;
}

MBOOL
LscMgrDefault::
syncTbl(const ILscTbl& rTbl)
{
    ILscBuf* pBuf = m_prLscBufRing[m_u4BufIdx];
    rTbl.toBuf(*pBuf);

    m_u4BufIdx ++;
    m_u4BufIdx = m_u4BufIdx >= RING_BUF_NUM ? 0 : m_u4BufIdx;
    ISP_MGR_LSC_T::getInstance(m_eSensorDev).putBuf(*pBuf);
    m_pCurrentBuf = pBuf;

    return MTRUE;
}
