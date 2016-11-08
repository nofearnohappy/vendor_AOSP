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
#define LOG_TAG "tsf_dft"
#ifndef ENABLE_MY_LOG
#define ENABLE_MY_LOG           (1)
#define GLOBAL_ENABLE_MY_LOG    (1)
#endif

//#define LSC_DBG

#include "TsfDft.h"
#include <LscUtil.h>

#include <ILscNvram.h>
#include <awb_param.h>
#include <awb_tuning_custom.h>

#include <sys/prctl.h>
#include <sys/resource.h>
#include <v1/config/PriorityDefs.h>

using namespace NSIspTuning;

#define TSF_SCN_DFT ESensorMode_Preview

#if 0 //(CAM3_3ATESTLVL > CAM3_3ASTTUT)
#define TSF_BUILD
#endif
#define TSF_THREAD_BUILD

#ifdef TSF_THREAD_BUILD
#define TSF_LOCK() ::pthread_mutex_lock(&m_Mutex)
#define TSF_UNLOCK() ::pthread_mutex_unlock(&m_Mutex)
#else
#define TSF_LOCK() /*::pthread_mutex_lock(&m_Mutex)*/
#define TSF_UNLOCK() /*::pthread_mutex_unlock(&m_Mutex)*/
#endif
/*******************************************************************************
 * TsfDft::TSF_INPUT_STAT_T
 *******************************************************************************/
MBOOL
TsfDft::TSF_INPUT_STAT_T::
dump(const char* filename) const
{   
    FILE* fptr = fopen(filename, "wb");
    
    if (fptr)
    {
        fwrite(vecStat.data(), vecStat.size(), 1, fptr);
        fwrite(&rAwbInfo, sizeof(ILscMgr::TSF_AWB_INFO), 1, fptr);
        fclose(fptr);
        return MTRUE;
    }
    return MFALSE;
}

MBOOL
TsfDft::TSF_INPUT_STAT_T::
put(const ILscMgr::TSF_INPUT_INFO_T& rInStat, MUINT8 fgOpt)
{
    MBOOL fgRet = MTRUE;

    // AWB info
    u4FrmId = rInStat.u4FrmId;
    rAwbInfo = rInStat.rAwbInfo;
    //u4Ratio = m_pLsc->getRatio();

    // AWB STAT preprocessing
    if (rInStat.prAwbStat)
    {
        MINT32 i4NumY = 90; //m_rTsfEnvInfo.ImgHeight;
        MINT32 i4NumX = 120;  //m_rTsfEnvInfo.ImgWidth;
        MINT32 i4AwbLineSize = i4NumX * 4;
        MINT32 i4AeSize      = ((i4NumX + 3)/4) * 4;                        // in byte
        MINT32 i4HdrSize     = (fgOpt & 0x4) ? ((i4NumX*4 + 31)/32)*4 : 0;  // in byte
        MINT32 i4AeOverSize  = (fgOpt & 0x2) ? ((i4NumX*8 + 31)/32)*4 : 0;  // in byte
        MINT32 i4TsfSize     = (fgOpt & 0x1) ? ((i4NumX*16 + 31)/32)*4 : 0; // in byte
        MINT32 i4LineSize    = i4AwbLineSize + i4AeSize + i4HdrSize + i4AeOverSize + i4TsfSize;
        const MUINT8* pSrc   = reinterpret_cast<const MUINT8*>(rInStat.prAwbStat);
#if STAT16BIT
        vecStat.resize(i4NumX * i4NumY * 4);
        MUINT16* pDst = vecStat.data();
        if (fgOpt & 0x1)    // c4 is available
        {
            for (MINT32 y = 0; y < i4NumY; y++)
            {
                const MUINT8*  pTmpSrc = pSrc;
                const MUINT32* pTsfSrc = (const MUINT32*)((const MUINT8*)pSrc + i4AeSize + i4AeOverSize + i4TsfSize);
                MINT32 x = i4NumX;
                while (x >= 8)
                {
                    MUINT16 c8;
                    MUINT16 c4;
                    MUINT32 res = *pTsfSrc++;
                    #define EXTRACT\
                        c8 = *pTmpSrc++;\
                        c4 = (res & 0xF); res >>= 4;\
                        *pDst++ = ((c4 & 0x8) ? (((c8-1)<<4)|c4) : ((c8<<4)|c4));
                    
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    EXTRACT
                    x -= 8;
                }
                MUINT32 last_res = *pTsfSrc;
                while (x)
                {
                    MUINT16 c8;
                    MUINT16 c4;
                    c8 = *pTmpSrc++;
                    c4 = (last_res >>(28-(x*4)) & 0xF);
                    *pDst++ = ((c4 & 0x8) ? (((c8-1)<<4)|c4) : ((c8<<4)|c4));
                    x--;
                }
                pSrc += i4LineSize;
            }
        }
        else    // c4 is not available
        {
            for (MINT32 y = 0; y < i4NumY; y++)
            {
                const MUINT8*  pTmpSrc = pSrc;
                MINT32 x = i4NumX;
                while (x >= 8)
                {
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    *pDst++ = ((*pTmpSrc++) << 4);
                    x -= 8;
                }
                while (x)
                {
                    *pDst++ = ((*pTmpSrc++) << 4);             
                    x--;
                }
                pSrc += i4LineSize;
            }
        }
#else
        vecStat.resize(i4AwbLineSize * i4NumY);
        MUINT8* pDst = vecStat.begin();
    #if 1
        for (MINT32 y = 0; y < i4NumY; y++) 
        {
            ::memcpy(pDst, pSrc, i4AwbLineSize);
            pDst += i4AwbLineSize;
            pSrc += i4LineSize;
        }
    #else
        ::memset(pDst, 0x80, (i4AwbLineSize * i4NumY));
    #endif
#endif
    }

    return fgRet;
}

/*******************************************************************************
 * TsfDft
 *******************************************************************************/
ILscTsf*
TsfDft::
createInstance(MUINT32 u4SensorDev)
{
    switch (u4SensorDev)
    {
    default:
    case ESensorDev_Main:
        static TsfDft singleton_main(static_cast<MUINT32>(ESensorDev_Main));
        return &singleton_main;
    case ESensorDev_MainSecond:
        static TsfDft singleton_main2(static_cast<MUINT32>(ESensorDev_MainSecond));
        return &singleton_main2;
    case ESensorDev_Sub:
        static TsfDft singleton_sub(static_cast<MUINT32>(ESensorDev_Sub));
        return &singleton_sub;
    }
}

void
TsfDft::
destroyInstance()
{}

TsfDft::
TsfDft(MUINT32 u4SensorDev)
    : m_u4SensorDev(u4SensorDev)
    , m_eSensorMode(ESensorMode_Capture)
    , m_u4LogEn(0)
    , m_bTSF(MFALSE)
    , m_u4TblIdx(0)
    , m_pLsc(NULL)
{
    LSC_LOG("Enter Type 0: Default Cycle");
}

TsfDft::
~TsfDft()
{
    LSC_LOG("Exit Type 0: Default Cycle");
}

MBOOL
TsfDft::
init()
{
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);

    GET_PROP("debug.lsc_mgr.log", "0", m_u4LogEn);
    
    MINT32 i = 0;
    m_pLsc = ILscMgr::getInstance(static_cast<ESensorDev_T>(m_u4SensorDev));
    ILscTbl::Config rCapCfg = m_pLsc->getLut(ESensorMode_Capture, 2)->getConfig();
    m_u4FullW = rCapCfg.i4ImgWd;
    m_u4FullH = rCapCfg.i4ImgHt;
    for (i = 0; i < RING_TBL_NUM; i++)
    {
        m_prLscTbl[i] = new ILscTbl(ILscTbl::HWTBL);
    }

    createTsf();
    createThread();

    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
    return MTRUE;
}

MBOOL
TsfDft::
uninit()
{
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);
    destroyThread();
    destroyTsf();
    MINT32 i = 0;
    for (i = 0; i < RING_TBL_NUM; i++)
    {
        if (m_prLscTbl[i]) delete m_prLscTbl[i];
    }
    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
    return MTRUE;
}

MBOOL
TsfDft::
loadOtpDataForTsf()
{
    ILscNvram* pNvram = ILscNvram::getInstance(static_cast<ESensorDev_T>(m_u4SensorDev));
    const ILscTbl* pGolden = pNvram->getGolden();
    const ILscTbl* pUnit = pNvram->getUnit();
    
    // Golden/Unit
    // for TSF
    GAIN_TBL& rTsfGainGolden    = m_rTsfEnvInfo.ShadingTbl.Golden;
    GAIN_TBL& rTsfGainUnit      = m_rTsfEnvInfo.ShadingTbl.Unit;
    const ILscTbl::Config& rUnitCfg = pUnit->getConfig();
    rTsfGainUnit.bayer        = (MTK_BAYER_ORDER_ENUM)pUnit->getBayer();
    rTsfGainUnit.offset_x     = 0;
    rTsfGainUnit.offset_y     = 0;
    rTsfGainUnit.crop_width   = rUnitCfg.i4ImgWd;
    rTsfGainUnit.crop_height  = rUnitCfg.i4ImgHt;
    rTsfGainUnit.grid_x       = rUnitCfg.i4GridX;
    rTsfGainUnit.grid_y       = rUnitCfg.i4GridY;
    rTsfGainGolden = rTsfGainUnit;

    MUINT32 u4GainTblSize = pUnit->getSize();
    rTsfGainGolden.Tbl          = new MUINT32[u4GainTblSize/4];
    rTsfGainUnit.Tbl            = new MUINT32[u4GainTblSize/4];
    ::memcpy(rTsfGainGolden.Tbl, pGolden->getData(), u4GainTblSize);
    ::memcpy(rTsfGainUnit.Tbl, pUnit->getData(), u4GainTblSize);

    LSC_LOG("Golden(%p), Unit(%p)", rTsfGainGolden.Tbl, rTsfGainUnit.Tbl);
    LSC_LOG("Bayer(%d), Crop(%d,%d,%d,%d), Grid(%d,%d)",
        rTsfGainUnit.bayer, rTsfGainGolden.offset_x, rTsfGainGolden.offset_y, rTsfGainGolden.crop_width, rTsfGainGolden.crop_height,
        rTsfGainGolden.grid_x, rTsfGainGolden.grid_y);    
#if 0
    switch( pNvram->getOtpData()->TableRotation )
    {
        default:
        case 0:
            m_rTsfEnvInfo.afn = MTKTSF_AFN_R0D;
        break;          
        case 1:
            m_rTsfEnvInfo.afn = MTKTSF_AFN_R180D;
        break;    
        case 2:
            m_rTsfEnvInfo.afn = MTKTSF_AFN_MIRROR;
        break;    
        case 3:
            m_rTsfEnvInfo.afn = MTKTSF_AFN_FLIP;
        break;    
    }
#endif
    return MTRUE;
}

MVOID
TsfDft::
createTsf()
{
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);

    m_u4PerFrameStep = 0;
    m_prTsf = MTKTsf::createInstance();

    // create tsf instance
    if (!m_prTsf)
    {
        LSC_ERR("NULL TSF instance");
        m_fgThreadLoop = 0;
    }
    else
    {
        AWB_STAT_PARAM_T rAwbStatParma;

        switch (m_u4SensorDev)
        {
        default:
        case ESensorDev_Main:
            rAwbStatParma = getAWBStatParam<ESensorDev_Main>();
            break;
        case ESensorDev_Sub:
            rAwbStatParma = getAWBStatParam<ESensorDev_Sub>();
            break;
        case ESensorDev_MainSecond:
            rAwbStatParma = getAWBStatParam<ESensorDev_MainSecond>();
            break;
        }

        MTK_TSF_GET_ENV_INFO_STRUCT rTsfGetEnvInfo;
        m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_GET_ENV_INFO, 0, &rTsfGetEnvInfo);
        LSC_LOG("MTKTSF_FEATURE_GET_ENV_INFO, buffer size(%d)", rTsfGetEnvInfo.WorkingBuffSize);

        // allocate working buffer
        MUINT8* gWorkinBuffer = new MUINT8[rTsfGetEnvInfo.WorkingBuffSize];
        ::memset(gWorkinBuffer, 0, rTsfGetEnvInfo.WorkingBuffSize);

        m_rTsfEnvInfo.ImgWidth    = rAwbStatParma.i4WindowNumX;
        m_rTsfEnvInfo.ImgHeight   = rAwbStatParma.i4WindowNumY;
        m_rTsfEnvInfo.BayerOrder  = MTK_BAYER_B;    
        m_rTsfEnvInfo.WorkingBufAddr = (MUINT32*)gWorkinBuffer;

        ILscNvram* pNvram = ILscNvram::getInstance(static_cast<ESensorDev_T>(m_u4SensorDev));

        // tsf tuning para/data
        const CAMERA_TSF_TBL_STRUCT* pTsfCfg = pNvram->getTsfNvram();
        m_rTsfEnvInfo.Para        = (MUINT32*)pTsfCfg->TSF_DATA;
        m_rTsfEnvInfo.pTuningPara = (MINT32*)pTsfCfg->TSF_PARA;
        m_rTsfEnvInfo.TS_TS       = 1;
        m_rTsfEnvInfo.MA_NUM      = 5;

        // golden/unit alignment
        ILscNvram::E_LSC_OTP_T eOtpState = pNvram->getOtpState();
        m_rTsfEnvInfo.WithOTP = (
            (/*(m_fg1to3 == E_LSC_123_WITH_MTK_OTP_OK) && */
             (ILscNvram::E_LSC_WITH_MTK_OTP == eOtpState)) 
              ? MTRUE : MFALSE);

        if (m_rTsfEnvInfo.WithOTP)
        {
            loadOtpDataForTsf();
        }
        else
        {
            LSC_LOG("No OTP Data");
            //m_rTsfEnvInfo.afn = MTKTSF_AFN_R0D;
            m_rTsfEnvInfo.ShadingTbl.Golden.Tbl = NULL;
            m_rTsfEnvInfo.ShadingTbl.Unit.Tbl = NULL;
        }
        LSC_LOG("WithOTP(%d)", m_rTsfEnvInfo.WithOTP);

        // AWB NVRAM
        m_rTsfEnvInfo.EnableORCorrection = 0;
        #warning "FIXME"
        #if 0
        const NVRAM_CAMERA_3A_STRUCT* pNvram3A = pNvram->get3ANvram();
        const AWB_ALGO_CAL_T& rAlgoCalParam = pNvram3A->rAWBNVRAM[AWB_NVRAM_IDX_NORMAL].rAlgoCalParam;
        m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4R   = rAlgoCalParam.rCalData.rUnitGain.i4R; // 512;
        m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4G   = rAlgoCalParam.rCalData.rUnitGain.i4G; // 512;
        m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4B   = rAlgoCalParam.rCalData.rUnitGain.i4B; // 512;
        m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4R = rAlgoCalParam.rCalData.rGoldenGain.i4R; //512;
        m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4G = rAlgoCalParam.rCalData.rGoldenGain.i4G; //512;
        m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4B = rAlgoCalParam.rCalData.rGoldenGain.i4B; //512;
        m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4R    = rAlgoCalParam.rPredictorGain.rAWBGain_LSC.i4R; //809;
        m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4G    = rAlgoCalParam.rPredictorGain.rAWBGain_LSC.i4G; //512;
        m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4B    = rAlgoCalParam.rPredictorGain.rAWBGain_LSC.i4B; //608;
        LSC_LOG("AwbNvramInfo: UnitGain(%d, %d, %d), GoldenGain(%d, %d, %d), D65Gain(%d, %d, %d)",
            m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4R  ,
            m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4G  ,
            m_rTsfEnvInfo.AwbNvramInfo.rUnitGain.i4B  ,
            m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4R,
            m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4G,
            m_rTsfEnvInfo.AwbNvramInfo.rGoldenGain.i4B,
            m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4R   ,
            m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4G   ,
            m_rTsfEnvInfo.AwbNvramInfo.rD65Gain.i4B   );    
        #endif
        m_bTSFInit = MFALSE;
    }
    
    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
}

MVOID
TsfDft::
destroyTsf()
{
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);

    m_prTsf->TsfExit();
    m_prTsf->destroyInstance(m_prTsf);

    delete [] (MUINT8*)m_rTsfEnvInfo.WorkingBufAddr;

    if (m_rTsfEnvInfo.ShadingTbl.Golden.Tbl)
    {
        delete [] m_rTsfEnvInfo.ShadingTbl.Golden.Tbl;
        m_rTsfEnvInfo.ShadingTbl.Golden.Tbl = NULL;
    }

    if (m_rTsfEnvInfo.ShadingTbl.Unit.Tbl)
    {
        delete [] m_rTsfEnvInfo.ShadingTbl.Unit.Tbl;
        m_rTsfEnvInfo.ShadingTbl.Unit.Tbl = NULL;
    }

    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
}

MBOOL
TsfDft::
tsfResetTbl(ESensorMode_T eLscScn)
{
    // reset proc shading table and result shading table
    const ILscTbl* pBaseLsc = m_pLsc->getLut(eLscScn, 2);
    if (pBaseLsc)
    {
        m_rLscBaseTbl = *pBaseLsc;
        
        MINT32 i;
        for (i = 0; i < RING_TBL_NUM; i++)
        {
            *m_prLscTbl[i] = m_rLscBaseTbl;
        }
        return MTRUE;
    }

    return MFALSE;
}

MBOOL
TsfDft::
tsfSetTbl(const ILscTbl& rTbl)
{
    return m_pLsc->syncTbl(rTbl);
}
#if 0
MBOOL
TsfDft::
tsfSetSL2(const MTK_TSF_SL2_PARAM_STRUCT& rSL2)
{
    if (getTsfOnOff())
    {
        m_rSl2Cfg.cen.bits.SL2_CENTR_X = rSL2.SL2_CENTR_X;
        m_rSl2Cfg.cen.bits.SL2_CENTR_Y = rSL2.SL2_CENTR_Y;
        m_rSl2Cfg.rr_con0.bits.SL2_R_0 = rSL2.SL2_RR_0;
        m_rSl2Cfg.rr_con0.bits.SL2_R_1 = rSL2.SL2_RR_1;
        m_rSl2Cfg.rr_con1.bits.SL2_R_2 = rSL2.SL2_RR_2;
    }
    return MTRUE;
}
#endif

MBOOL
TsfDft::
tsfInit()
{
    MRESULT ret = S_TSF_OK;
    
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);

    m_prTsf->TsfExit();

    // SensorCrop    
    // 0: full 4:3 FOV
    // 1: full 16:9 FOV (full horizontal FOV, cropped vertical FOV)
    // 2: general cropping case
    m_rTsfEnvInfo.Raw16_9Mode = 2;
    #if 1
    ILscTbl::TransformCfg_T rCropCfg = m_pLsc->getTransformCfg(m_eSensorMode);
    m_rTsfEnvInfo.SensorCrop.full_width	    = m_u4FullW;
    m_rTsfEnvInfo.SensorCrop.full_height	= m_u4FullH;
    m_rTsfEnvInfo.SensorCrop.resize_width	= rCropCfg.u4ResizeW;
    m_rTsfEnvInfo.SensorCrop.resize_height	= rCropCfg.u4ResizeH;
    m_rTsfEnvInfo.SensorCrop.crop_width	    = rCropCfg.u4W;
    m_rTsfEnvInfo.SensorCrop.crop_height	= rCropCfg.u4H;
    m_rTsfEnvInfo.SensorCrop.crop_hor_offs	= rCropCfg.u4X;
    m_rTsfEnvInfo.SensorCrop.crop_ver_offs	= rCropCfg.u4Y;
    #else
    m_rTsfEnvInfo.SensorCrop.full_width	    = m_rTsfEnvInfo.pLscConfig->raw_wd;
    m_rTsfEnvInfo.SensorCrop.full_height	= m_rTsfEnvInfo.pLscConfig->raw_ht;
    m_rTsfEnvInfo.SensorCrop.resize_width	= m_rTsfEnvInfo.pLscConfig->raw_wd;
    m_rTsfEnvInfo.SensorCrop.resize_height	= m_rTsfEnvInfo.pLscConfig->raw_ht;
    m_rTsfEnvInfo.SensorCrop.crop_width	    = m_rTsfEnvInfo.pLscConfig->raw_wd;
    m_rTsfEnvInfo.SensorCrop.crop_height	= m_rTsfEnvInfo.pLscConfig->raw_ht;
    m_rTsfEnvInfo.SensorCrop.crop_hor_offs	= 0;
    m_rTsfEnvInfo.SensorCrop.crop_ver_offs	= 0;
    #endif
    LSC_LOG("SensorCrop(%d): Full(%d,%d), Resize(%d,%d), Crop(%d,%d,%d,%d)",
        m_eSensorMode,
        m_rTsfEnvInfo.SensorCrop.full_width	  ,
        m_rTsfEnvInfo.SensorCrop.full_height  ,
        m_rTsfEnvInfo.SensorCrop.resize_width ,
        m_rTsfEnvInfo.SensorCrop.resize_height,
        m_rTsfEnvInfo.SensorCrop.crop_hor_offs,
        m_rTsfEnvInfo.SensorCrop.crop_ver_offs,
        m_rTsfEnvInfo.SensorCrop.crop_width   ,
        m_rTsfEnvInfo.SensorCrop.crop_height  );

    MINT32 i4TsfSL2En = 0;
    GET_PROP("debug.lsc_mgr.sl2", "-1", i4TsfSL2En);

    if (i4TsfSL2En == -1)
    {
        // 0:disable, 1:TSF's SL2, 2:NVRAM default
        i4TsfSL2En = 0; //isEnableNSL2(m_eSensorDev);
        LSC_LOG("TSF set SL2 default mode(%d)", i4TsfSL2En);
    }
    else
    {
        LSC_LOG("TSF set SL2 mode(%d)", i4TsfSL2En);
    }

    m_rTsfEnvInfo.EnableSL2      = i4TsfSL2En;
    m_rTsfEnvInfo.pLscConfig     = &m_rTsfLscParam;
    m_rTsfEnvInfo.BaseShadingTbl = (MINT32*) m_rLscBaseTbl.getData();

    LSC_LOG("ImgWidth(%d), ImgHeight(%d), BayerOrder(%d), BaseShadingTbl(%p), Raw16_9Mode(%d), EnableSL2(%d), pLscConfig(%p)",
        m_rTsfEnvInfo.ImgWidth, m_rTsfEnvInfo.ImgHeight, m_rTsfEnvInfo.BayerOrder,
        m_rTsfEnvInfo.BaseShadingTbl, m_rTsfEnvInfo.Raw16_9Mode, m_rTsfEnvInfo.EnableSL2, m_rTsfEnvInfo.pLscConfig);

    // init
    ret = m_prTsf->TsfInit(&m_rTsfEnvInfo, NULL);

    m_prTsf->TsfReset();

    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
    
    return MTRUE;
}

MBOOL
TsfDft::
convert(const TSF_INPUT_STAT_T& rInputStat, const ILscTbl& rInputTbl, MTK_TSF_SET_PROC_INFO_STRUCT& rProcInfo) const
{
    MBOOL fgLogEn = 1;//(m_u4LogEn & EN_LSC_LOG_TSF_SET_PROC) ? MTRUE : fgDump;
#if 0
    const MINT32* pAwbForceParam = m_rTsfCfgTbl.TSF_CFG.rAWBInput;

    if (m_bTsfForceAwb && pAwbForceParam)
    {
        rProcInfo.ParaL       	= pAwbForceParam[0];
        rProcInfo.ParaC       	= pAwbForceParam[1];
        rProcInfo.FLUO_IDX		= pAwbForceParam[2];
        rProcInfo.DAY_FLUO_IDX	= pAwbForceParam[3];
    }
    else
#endif
    {
        rProcInfo.ParaL       	= rInputStat.rAwbInfo.m_i4LV;
        rProcInfo.ParaC       	= rInputStat.rAwbInfo.m_u4CCT;
        rProcInfo.FLUO_IDX		= rInputStat.rAwbInfo.m_FLUO_IDX;
        rProcInfo.DAY_FLUO_IDX	= rInputStat.rAwbInfo.m_DAY_FLUO_IDX;
    }
    rProcInfo.Gain.i4R = rInputStat.rAwbInfo.m_RGAIN;
    rProcInfo.Gain.i4G = rInputStat.rAwbInfo.m_GGAIN;
    rProcInfo.Gain.i4B = rInputStat.rAwbInfo.m_BGAIN;

    // AWB stat
    rProcInfo.ShadingTbl = (MINT32*)rInputTbl.getData();
#if STAT16BIT
    rProcInfo.ImgAddr = (MUINT16*)rInputStat.vecStat.data();
#else
    rProcInfo.ImgAddr = (MUINT8*)rInputStat.vecStat.data();
#endif
    rProcInfo.LscRA = 32;

    MY_LOG_IF(fgLogEn, "[%s] L(%d), C(%d), F(%d), DF(%d), R(%d), G(%d), B(%d)\n",
        __FUNCTION__, rProcInfo.ParaL, rProcInfo.ParaC, rProcInfo.FLUO_IDX, rProcInfo.DAY_FLUO_IDX, rProcInfo.Gain.i4R, rProcInfo.Gain.i4G, rProcInfo.Gain.i4B);

    return MTRUE;
}

MBOOL
TsfDft::
tsfSetProcInfo(const TSF_INPUT_STAT_T& rInputStat, const ILscTbl& rInputTbl)
{
    MRESULT ret = S_TSF_OK;
    MTK_TSF_SET_PROC_INFO_STRUCT rProcInfo;
    convert(rInputStat, rInputTbl, rProcInfo);
    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_SET_PROC_INFO, &rProcInfo, NULL);
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): MTKTSF_FEATURE_SET_PROC_INFO", ret);
        m_prTsf->TsfReset();
        m_u4PerFrameStep = 0;
    }

    return (ret == S_TSF_OK);
}

TsfDft::E_TSF_STATE_T
TsfDft::
tsfMain()
{
    MRESULT ret = S_TSF_OK;
    E_TSF_STATE_T eState = E_TSF_NOT_READY;

    ret = m_prTsf->TsfMain();
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): TsfMain", ret);
        m_prTsf->TsfReset();
        m_u4PerFrameStep = 0;
        eState = E_TSF_FAIL;
        goto lbExit;
    }

    MTK_TSF_GET_PROC_INFO_STRUCT rTsfGetProc;
    m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_GET_PROC_INFO, 0, &rTsfGetProc);
    if (rTsfGetProc.TsfState == MTKTSF_STATE_READY)
    {
        eState = E_TSF_READY;
    }
    else if (rTsfGetProc.TsfState == MTKTSF_STATE_OPT_DONE)
    {
        eState = E_TSF_OPT_DONE;
    }

lbExit:
    return eState;
}

MBOOL
TsfDft::
tsfGetResult(ILscTbl& rOutputTbl)
{
    MRESULT ret = S_TSF_OK;

    m_rTsfResult.ShadingTbl = static_cast<MUINT32*>(rOutputTbl.editData());
    m_rTsfResult.u4TblSize = rOutputTbl.getSize();
    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_GET_RESULT, NULL, &m_rTsfResult);
    return (ret == S_TSF_OK);
}

MBOOL
TsfDft::
tsfCfgChg()
{
    MRESULT ret = S_TSF_OK;
    
    // this is for identical sized tables, but block numbers are different.
    // no cropping support.
    // ex. 10x10 -> 16x16
    LSC_LOG_BEGIN("Sensor(%d)", m_u4SensorDev);

    MTK_TSF_TBL_STRUCT rTsfTbl;
    rTsfTbl.pLscConfig = &m_rTsfLscParam;
    rTsfTbl.ShadingTbl = (MINT32*)m_rLscBaseTbl.getData();
    // convert current gain table from nxn to mxm (ex. 10x10 to 16x16)
    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_SET_TBL_CHANGE, &rTsfTbl, NULL);
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): MTKTSF_FEATURE_SET_TBL_CHANGE", ret);
        m_prTsf->TsfReset();
        goto lbExit;
    }

#if 0
    // convert transformed table to HW coef table.
    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_GEN_CAP_TBL, &m_rTsfTbl, &m_rTsfResult);
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): MTKTSF_FEATURE_GEN_CAP_TBL", ret);
        m_prTsf->TsfReset();
    }
    else
    {
        tsfSetTbl(m_rTsfResult.ShadingTbl);
        //tsfSetSL2(m_rTsfResult.SL2Para);
    }
#endif
    tsfSetTbl(m_rLscBaseTbl);

lbExit:    
    LSC_LOG_END("Sensor(%d)", m_u4SensorDev);
    
    return ret == S_TSF_OK;
}

MBOOL
TsfDft::
tsfBatch(const TSF_INPUT_STAT_T& rInputStat, const ILscTbl& rInputTbl, ILscTbl& rOutputTbl)
{
    MRESULT ret = S_TSF_OK;
    MBOOL fgDump = m_u4LogEn & EN_LSC_LOG_TSF_DUMP ? MTRUE : MFALSE;

    LSC_LOG_BEGIN("Sensor(%d), #(%d)", m_u4SensorDev, rInputStat.u4FrmId);

    MTK_TSF_SET_PROC_INFO_STRUCT rProcInfo;
    convert(rInputStat, rInputTbl, rProcInfo);

    m_rTsfResult.ShadingTbl = static_cast<MUINT32*>(rOutputTbl.editData());
    m_rTsfResult.u4TblSize = rOutputTbl.getSize();

    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_BATCH, &rProcInfo, &m_rTsfResult);
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): MTKTSF_FEATURE_BATCH", ret);
        m_prTsf->TsfReset();
    }
    else
    {
        tsfSetTbl(rOutputTbl);
        //tsfSetSL2(m_rTsfResult.SL2Para);
        if (fgDump)
        {
            char strFile[512] = {'\0'};
            sprintf(strFile, "/sdcard/tsf/%04d_tsfInStat_bat.bin", rInputStat.u4FrmId);
            rInputStat.dump(strFile);
            sprintf(strFile, "/sdcard/tsf/%04d_tsfInput_bat.tbl", rInputStat.u4FrmId);
            rInputTbl.dump(strFile);
            sprintf(strFile, "/sdcard/tsf/%04d_tsfOutput_bat.tbl", rInputStat.u4FrmId);
            rOutputTbl.dump(strFile);
        }
    }

    LSC_LOG_END("Sensor(%d), #(%d)", m_u4SensorDev, rInputStat.u4FrmId);
    
    return ret == S_TSF_OK;
}

MBOOL
TsfDft::
tsfBatchCap(const TSF_INPUT_STAT_T& rInputStat, const ILscTbl& rInputTbl, ILscTbl& rOutputTbl)
{
    MRESULT ret = S_TSF_OK;
    MBOOL fgDump = m_u4LogEn & EN_LSC_LOG_TSF_DUMP ? MTRUE : MFALSE;

    LSC_LOG_BEGIN("Sensor(%d), #(%d)", m_u4SensorDev, rInputStat.u4FrmId);

    MTK_TSF_SET_PROC_INFO_STRUCT rProcInfo;
    convert(rInputStat, rInputTbl, rProcInfo);

    m_rTsfResult.ShadingTbl = static_cast<MUINT32*>(rOutputTbl.editData());
    m_rTsfResult.u4TblSize = rOutputTbl.getSize();

    // disable temporal smooth
    m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_CONFIG_SMOOTH, (void*)0, 0);
    // batch
    ret = m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_BATCH, &rProcInfo, &m_rTsfResult);
    if (ret != S_TSF_OK)
    {
        LSC_ERR("Error(0x%08x): MTKTSF_FEATURE_BATCH", ret);
        m_prTsf->TsfReset();
    }
    else
    {
        tsfSetTbl(rOutputTbl);
        //tsfSetSL2(m_rTsfResult.SL2Para);
        if (fgDump)
        {
            char strFile[512] = {'\0'};
            sprintf(strFile, "/sdcard/tsf/%04d_tsfInStat_cap.bin", rInputStat.u4FrmId);
            rInputStat.dump(strFile);
            sprintf(strFile, "/sdcard/tsf/%04d_tsfInput_cap.tbl", rInputStat.u4FrmId);
            rInputTbl.dump(strFile);
            sprintf(strFile, "/sdcard/tsf/%04d_tsfOutput_cap.tbl", rInputStat.u4FrmId);
            rOutputTbl.dump(strFile);
        }
    }
    // enable temporal smooth
    m_prTsf->TsfFeatureCtrl(MTKTSF_FEATURE_CONFIG_SMOOTH, (void*)1, 0);

    LSC_LOG_END("Sensor(%d), #(%d)", m_u4SensorDev, rInputStat.u4FrmId);
    
    return ret == S_TSF_OK;
}

MBOOL
TsfDft::
tsfRun(const TSF_INPUT_STAT_T& rInputStat, const ILscTbl& rInputTbl, const ILscTbl& rTblPrior, ILscTbl& rOutputTbl)
{
    MBOOL fgDump = m_u4LogEn & EN_LSC_LOG_TSF_DUMP ? MTRUE : MFALSE;
    MBOOL fgLogEn = (m_u4LogEn & EN_LSC_LOG_TSF_RUN) ? MTRUE : MFALSE;
    MINT32 i4Case = 0;
    MUINT32 u4Step = m_u4PerFrameStep;

    MY_LOG_IF(fgLogEn, "[%s +] Sensor(%d), #(%d), step(%d)", __FUNCTION__, m_u4SensorDev, rInputStat.u4FrmId, u4Step);

    if (m_u4PerFrameStep == 0)
    {
        // only set proc info at the 1st frame.
        if (!tsfSetProcInfo(rInputStat, rInputTbl))
            return MFALSE;
    }
    m_u4PerFrameStep ++;    

    E_TSF_STATE_T eState = tsfMain();
    if (eState == E_TSF_FAIL)
    {
        i4Case = -1;
    }
    else if (eState == E_TSF_READY)
    {
        if (tsfGetResult(rOutputTbl))
        {
            m_u4PerFrameStep = 0;
            tsfSetTbl(rOutputTbl);
            //tsfSetSL2(m_rTsfResult.SL2Para);
            i4Case = 1;
        }
        else
        {
            i4Case = -2;
        }
    }
    else
    {
        i4Case = 0;
        rOutputTbl = rTblPrior;
    }

    if (fgDump)
    {
        char strFile[512] = {'\0'};
        sprintf(strFile, "/sdcard/tsf/%04d_tsfInStat_%d.bin", rInputStat.u4FrmId, u4Step);
        rInputStat.dump(strFile);
        sprintf(strFile, "/sdcard/tsf/%04d_tsfInput_%d.tbl", rInputStat.u4FrmId, u4Step);
        rInputTbl.dump(strFile);
        sprintf(strFile, "/sdcard/tsf/%04d_tsfOutput_%d.tbl", rInputStat.u4FrmId, u4Step);
        rOutputTbl.dump(strFile);
    }

    MY_LOG_IF(fgLogEn, "[%s -] Sensor(%d), #(%d), step(%d), case(%d)", __FUNCTION__, m_u4SensorDev, rInputStat.u4FrmId, u4Step, i4Case);

    return (i4Case >= 0);
}

MBOOL
TsfDft::
createThread()
{
#ifdef TSF_THREAD_BUILD
    LSC_LOG_BEGIN();
    m_fgThreadLoop = MTRUE;
    ::pthread_mutex_init(&m_Mutex, NULL);
    ::sem_init(&m_Sema, 0, 0);
    ::pthread_create(&m_Thread, NULL, threadLoop, this);
    LSC_LOG_END("Create TSF m_Thread(0x%08x)\n", (MUINT32) m_Thread);
#endif
    return MTRUE;
}

MBOOL
TsfDft::
destroyThread()
{
#ifdef TSF_THREAD_BUILD
    LSC_LOG_BEGIN();

    TSF_LOCK();
    m_fgThreadLoop = MFALSE;
    TSF_UNLOCK();
    ::sem_post(&m_Sema);
    ::pthread_join(m_Thread, NULL);
    ::sem_destroy(&m_Sema);
    ::pthread_mutex_destroy(&m_Mutex);

    LSC_LOG_END();
#endif
    return MTRUE;
}

MVOID
TsfDft::
changeThreadSetting()
{
#ifdef TSF_THREAD_BUILD
    // (1) set name 
    ::prctl(PR_SET_NAME, "F858THREAD", 0, 0, 0);

    //
    struct sched_param sched_p;
    ::sched_getparam(0, &sched_p);

    // (2) set policy/priority
#if MTKCAM_HAVE_RR_PRIORITY
    int const policy    = SCHED_RR;
    int const priority  = PRIO_RT_F858_THREAD;
    //  set
    sched_p.sched_priority = priority;  //  Note: "priority" is real-time priority.
    ::sched_setscheduler(0, policy, &sched_p);
    //  get
    ::sched_getparam(0, &sched_p);
#else
    int const policy    = SCHED_OTHER;
    int const priority  = NICE_CAMERA_TSF;
    //  set
    sched_p.sched_priority = priority;  //  Note: "priority" is nice value.
    ::sched_setscheduler(0, policy, &sched_p);
    ::setpriority(PRIO_PROCESS, 0, priority);
    //  get
    sched_p.sched_priority = ::getpriority(PRIO_PROCESS, 0);
#endif
    //
    LSC_LOG(
        "sensor(%d), tid(%d), policy:(expect, result)=(%d, %d), priority:(expect, result)=(0x%08x, 0x%08x)"
        , m_u4SensorDev, ::gettid()
        , policy, ::sched_getscheduler(0)
        , priority, sched_p.sched_priority
    );

#endif
}

MVOID
TsfDft::
doThreadFunc()
{
    LSC_LOG_BEGIN();

    while (m_fgThreadLoop)
    {
        ::sem_wait(&m_Sema);
        if (!m_fgThreadLoop)
            break;
        TSF_LOCK();
        MUINT32 u4TblIdx = (m_u4TblIdxCmd & RING_TBL_MSK);
        const TSF_INPUT_STAT_T& rStat = m_rStat[u4TblIdx];
        tsfRun(rStat, *m_prLscTbl[u4TblIdx], *m_prLscTbl[(u4TblIdx+2)&RING_TBL_MSK], *m_prLscTbl[(u4TblIdx+3)&RING_TBL_MSK]);
        TSF_UNLOCK();
    }
    
    LSC_LOG_END();
}

MVOID*
TsfDft::
threadLoop(void* arg)
{
#ifdef TSF_THREAD_BUILD
    TsfDft* _this = reinterpret_cast<TsfDft*>(arg);

    _this->changeThreadSetting();

    _this->doThreadFunc();

#endif
    return NULL;
}

const MVOID*
TsfDft::
getRsvdData() const
{
    return &m_rTsfResult.ExifData[0];
}

MBOOL
TsfDft::
setOnOff(MBOOL fgOnOff)
{
#if defined(TSF_BUILD)
    LSC_LOG("(%d)", fgOnOff);
    m_bTSF = fgOnOff;
    return MTRUE;
#else
    m_bTSF = MFALSE;
    return MFALSE;
#endif
}

MBOOL
TsfDft::
getOnOff() const
{
    return m_bTSF;
}

MBOOL
TsfDft::
setConfig(ESensorMode_T eSensorMode, MUINT32 u4W, MUINT32 u4H, MBOOL fgForce)
{
#if defined(TSF_BUILD) && !defined(LSC_DBG)
    LSC_LOG_BEGIN();

    TSF_LOCK();

    ILscTbl::Config rTblCfg = m_pLsc->getLut(eSensorMode, 2)->getConfig();
    LSC_LOG("Sensor(%d), Scn(%d), WxH(%dx%d), Grid(%dx%d), force(%d)",
        m_u4SensorDev, eSensorMode, rTblCfg.i4ImgWd, rTblCfg.i4ImgHt, rTblCfg.i4GridX, rTblCfg.i4GridY, fgForce);

    MBOOL fgBlkChg = 
        (m_rTsfLscParam.x_grid_num != rTblCfg.i4GridX)||
        (m_rTsfLscParam.y_grid_num != rTblCfg.i4GridY);

    float fRto = (((float)m_rTsfLscParam.raw_wd*rTblCfg.i4ImgHt) / ((float)m_rTsfLscParam.raw_ht*rTblCfg.i4ImgWd));
    float fMax = 1.0f + (1.0f/80.0f);
    float fMin = 1.0f - (1.0f/80.0f);
    MBOOL fgAspectChg = fRto > fMax || fRto < fMin;
    //MBOOL fgAspectChg = (m_rTsfLscParam.raw_wd*u4H != m_rTsfLscParam.raw_ht*u4W);

    MINT32 i = 0;
    // config table
    m_rTsfLscParam.raw_wd        = rTblCfg.i4ImgWd;
    m_rTsfLscParam.raw_ht        = rTblCfg.i4ImgHt;
    m_rTsfLscParam.x_offset      = 0;
    m_rTsfLscParam.y_offset      = 0;
    m_rTsfLscParam.block_wd      = rTblCfg.rCfgBlk.i4BlkW;
    m_rTsfLscParam.block_ht      = rTblCfg.rCfgBlk.i4BlkH;
    m_rTsfLscParam.x_grid_num    = rTblCfg.i4GridX;
    m_rTsfLscParam.y_grid_num    = rTblCfg.i4GridY;
    m_rTsfLscParam.block_wd_last = rTblCfg.rCfgBlk.i4BlkLastW;
    m_rTsfLscParam.block_ht_last = rTblCfg.rCfgBlk.i4BlkLastH;

    LSC_LOG("raw_wd(%d), raw_ht(%d), block_wd(%d), block_ht(%d), xgrid(%d), ygrid(%d), wd_last(%d), ht_last(%d)",
        m_rTsfLscParam.raw_wd,
        m_rTsfLscParam.raw_ht,
        m_rTsfLscParam.block_wd,
        m_rTsfLscParam.block_ht,
        m_rTsfLscParam.x_grid_num,
        m_rTsfLscParam.y_grid_num,
        m_rTsfLscParam.block_wd_last,
        m_rTsfLscParam.block_ht_last);
    m_eSensorMode = eSensorMode;

    //
    if (!m_bTSFInit || fgBlkChg || fgAspectChg || fgForce)
    {
        LSC_LOG("Need to reset table, eSensorMode(%d), init(%d), fgBlkChg(%d), fgAspectChg(%d), fgForce(%d)", eSensorMode, m_bTSFInit, fgBlkChg, fgAspectChg, fgForce);
        m_rTransformCfg = m_pLsc->getTransformCfg(eSensorMode);
        tsfResetTbl(eSensorMode);
        tsfInit();
        tsfCfgChg();
        m_bTSFInit = MTRUE;
    }
    else
    {
        tsfCfgChg();
        LSC_LOG("No need to reset table");
    }

    m_u4PerFrameStep = 0;

    TSF_UNLOCK();
    LSC_LOG_END();
#endif

    return MTRUE;
}

MBOOL
TsfDft::
update(const ILscMgr::TSF_INPUT_INFO_T& rInputInfo)
{
    LSC_LOG_BEGIN("sensor(%d)", m_u4SensorDev);
    TSF_LOCK();
    if (m_bTSFInit && m_bTSF)
    {
        MUINT32 u4TblIdx = (m_u4TblIdx & RING_TBL_MSK);
        TSF_INPUT_STAT_T& rStat = m_rStat[u4TblIdx];

        // AWB stat
        rStat.u4FrmId = rInputInfo.u4FrmId;
        rStat.rAwbInfo = rInputInfo.rAwbInfo;
        rStat.u4Ratio = m_pLsc->getRatio();
        rStat.put(rInputInfo);

    #ifdef TSF_THREAD_BUILD
        if (ILscMgr::E_TSF_CMD_RUN == rInputInfo.eCmd)
        {
            m_u4TblIdxCmd = u4TblIdx;
            ::sem_post(&m_Sema);
        }
    #endif
        else
        {
            switch (rInputInfo.eCmd)
            {
            default:
            case ILscMgr::E_TSF_CMD_BATCH:
                tsfBatch(rStat, *m_prLscTbl[u4TblIdx], *m_prLscTbl[(u4TblIdx+3)&RING_TBL_MSK]);
                break;
            case ILscMgr::E_TSF_CMD_BATCH_CAP:
                tsfBatchCap(rStat, *m_prLscTbl[u4TblIdx], *m_prLscTbl[(u4TblIdx+3)&RING_TBL_MSK]);
                break;
            case ILscMgr::E_TSF_CMD_RUN:
                tsfRun(rStat, *m_prLscTbl[u4TblIdx], *m_prLscTbl[(u4TblIdx+2)&RING_TBL_MSK], *m_prLscTbl[(u4TblIdx+3)&RING_TBL_MSK]);
                break;
            }
        }
        // increment table index
        m_u4TblIdx = ((m_u4TblIdx+1) & RING_TBL_MSK);
    }
    TSF_UNLOCK();
    LSC_LOG_END();
    return MTRUE;
}
