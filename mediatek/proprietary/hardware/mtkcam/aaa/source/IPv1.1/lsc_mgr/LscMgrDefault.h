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
#ifndef _LSC_MGR_DEFAULT_H_
#define _LSC_MGR_DEFAULT_H_

#include <ILscMgr.h>
#include <ILscBuf.h>
#include <ILscTbl.h>
#include <ILscTsf.h>
#include "ILscNvram.h"
#include <camera_custom_nvram.h>
#include <list>
#include <string>

namespace NSIspTuning
{
class LscMgrDefault : public ILscMgr
{
public:
    static ILscMgr*                 createInstance(
                                        ESensorDev_T const eSensorDev,
                                        MINT32 i4SensorIdx);

    static ILscMgr*                 getInstance(ESensorDev_T sensor);

    virtual MBOOL                   destroyInstance(){return MTRUE;}

    virtual MBOOL                   init();
    virtual MBOOL                   uninit();

    virtual MBOOL                   setSensorMode(ESensorMode_T eSensorMode, MUINT32 u4Width, MUINT32 u4Height, MBOOL fgForce=MFALSE);
    virtual ESensorMode_T           getSensorMode() const;
    virtual const ILscTbl::TransformCfg_T& getTransformCfg(ESensorMode_T eSensorMode) const;

    virtual MUINT32                 getCTIdx();
    virtual MBOOL                   setCTIdx(MUINT32 const u4CTIdx);
    virtual MVOID                   setOnOff(MBOOL fgOnOff);
    virtual MBOOL                   getOnOff() const;
    virtual MBOOL                   setRatio(MUINT32 u4Ratio);
    virtual MUINT32                 getRatio() const;

    virtual ILscBuf*                createBuf(const char* name);
    virtual MBOOL                   destroyBuf(ILscBuf* pBuf);
    virtual MBOOL                   syncBuf(ILscBuf* pBuf);
    virtual MBOOL                   syncTbl(const ILscTbl& rTbl);

//    virtual ESensorDev_T            getSensorDev() const;

    virtual const ILscTbl*          getLut(ESensorMode_T eSensorMode, MUINT32 u4CtIdx) const;
//    virtual MUINT32                 getTotalLutSize(ELscScenario_T eLscScn) const;
//    virtual MUINT32                 getPerLutSize(ELscScenario_T eLscScn) const;

//    virtual MBOOL                   readNvramTbl(MBOOL fgForce);
//    virtual MBOOL                   writeNvramTbl(void);

    virtual MINT32                  getGainTable(MUINT32 u4Bayer, MUINT32 u4GridNumX, MUINT32 u4GridNumY, MFLOAT* pGainTbl);
//    virtual MINT32                  setGainTable(MUINT32 u4GridNumX, MUINT32 u4GridNumY, MUINT32 u4Width, MUINT32 u4Height, float* pGainTbl);
//    virtual MINT32                  setCoefTable(const MUINT32* pCoefTbl);

    virtual MINT32                  getDebugInfo(SHADING_DEBUG_INFO_T &rShadingDbgInfo);
    virtual MINT32                  getDebugTbl(DEBUG_SHAD_ARRAY_INFO_T &rShadingDbgTbl, DEBUG_SHAD_ARRAY_2_T& rShadRestTbl);
    
    virtual MINT32                  CCTOPSetSdblkFileCfg(MBOOL fgSave, const char* filename);
//    virtual MINT32                  CCTOPSetBypass123(MBOOL fgBypass);
//    virtual MINT32                  setTsfForceAwb(MBOOL fgForce);

    virtual MVOID                   updateLsc();
    virtual MVOID                   updateTsf(const TSF_INPUT_INFO_T& rInputInfo);
    virtual MVOID                   setTsfOnOff(MBOOL fgOnOff);
    virtual MBOOL                   getTsfOnOff() const;

protected:
    #define RING_BUF_NUM 3
                                    LscMgrDefault(ESensorDev_T eSensorDev, MINT32 i4SensorIdx);
    virtual                         ~LscMgrDefault();

    typedef struct
    {
        MUINT32 u4SensorPreviewWidth;
        MUINT32 u4SensorPreviewHeight;
        MUINT32 u4SensorCaptureWidth;
        MUINT32 u4SensorCaptureHeight;
        MUINT32 u4SensorVideoWidth;
        MUINT32 u4SensorVideoHeight;
        MUINT32 u4SensorVideo1Width;
        MUINT32 u4SensorVideo1Height;
        MUINT32 u4SensorVideo2Width;
        MUINT32 u4SensorVideo2Height;        
        MUINT32 u4SensorCustom1Width;   // new for custom
        MUINT32 u4SensorCustom1Height;
        MUINT32 u4SensorCustom2Width;
        MUINT32 u4SensorCustom2Height;
        MUINT32 u4SensorCustom3Width;
        MUINT32 u4SensorCustom3Height;
        MUINT32 u4SensorCustom4Width;
        MUINT32 u4SensorCustom4Height;
        MUINT32 u4SensorCustom5Width;
        MUINT32 u4SensorCustom5Height; 
    } SENSOR_RESOLUTION_INFO_T;
    
    struct SensorCropInfo_T
    {
        // TBD
        MUINT32  w0;    // original full width
        MUINT32  h0;    // original full height
        MUINT32  x1;    // crop_1 x offset from full_0
        MUINT32  y1;    // crop_1 y offset from full_0
        MUINT32  w1;    // crop_1 width from full_0
        MUINT32  h1;    // crop_1 height from full_0
        MUINT32  w1r;   // scaled width from crop_1, w1 * r
        MUINT32  h1r;   // scaled height from crop_1, h1 * r
        MUINT32  x2;    // crop_2 x offset from scaled crop_1
        MUINT32  y2;    // crop_2 y offset from scaled crop_1
        MUINT32  w2;    // crop_2 width from scaled crop_1
        MUINT32  h2;    // crop_2 height from scaled crop_1
        
        MUINT32  u4W;   // input size of LSC, w2*r2, r2 must be 1
        MUINT32  u4H;   // input size of LSC, h2*r2, r2 must be 1
    };

    typedef enum
    {
        E_LSC_123_OTP_ERR           = -1,   // use default table
        E_LSC_123_USE_CCT           = 0,    // use default table
        E_LSC_123_NO_OTP_OK         = 1,    // use transformed table
        E_LSC_123_NO_OTP_ERR        = 2,    // use default table
        E_LSC_123_WITH_MTK_OTP_OK   = 3,    // use transformed table
        E_LSC_123_WITH_MTK_OTP_ERR1 = 4,    // use default table
        E_LSC_123_WITH_MTK_OTP_ERR2 = 5,    // use default table
    } E_LSC_123_FLAG_T;

    virtual MBOOL                   getSensorResolution();
    virtual MBOOL                   getResolution(ESensorMode_T eSensorMode, SensorCropInfo_T& rInfo);
    virtual MBOOL                   getResolution(ELscScenario_T eScn, SensorCropInfo_T& rInfo);
    virtual MBOOL                   showResolutionInfo();

    virtual MBOOL                   convertSensorCrop(MBOOL fgWithSensorCropInfo, const SensorCropInfo_T& rFullInfo, const SensorCropInfo_T& rCropInfo, ILscTbl::TransformCfg_T& rCropCfg);

    virtual MBOOL                   resetLscTbl();
    virtual MBOOL                   loadTableFlow(MBOOL fgForceRestore);
    virtual MBOOL                   doShadingAlign();
    virtual MBOOL                   doShadingTrfm();

    ESensorDev_T                    m_eSensorDev;
    MINT32                          m_i4SensorIdx;

    ESensorMode_T                   m_eSensorMode;
    ESensorMode_T                   m_ePrevSensorMode;
    MUINT32                         m_u4NumSensorModes;

    SENSOR_RESOLUTION_INFO_T        m_rSensorResolution;
    SensorCropInfo_T                m_rCurSensorCrop;
    SensorCropInfo_T                m_rSensorCropWin[ESensorMode_NUM];

    ILscTbl::TransformCfg_T         m_rTransformCfg[ESensorMode_NUM];

    MBOOL                           m_fgOnOff;
    MBOOL                           m_fgInit;
    MBOOL                           m_fgSensorCropInfoNull;

    MBOOL                           m_fgBypass1to3;
    E_LSC_123_FLAG_T                m_e1to3Flag;

    MUINT32                         m_u4CTIdx;
    MUINT32                         m_u4Rto;
    MUINT32                         m_u4BufIdx;
    MUINT32                         m_u4LogEn;

    ILscTbl                         m_rLscTbl[LSC_SCENARIO_NUM][SHADING_SUPPORT_CT_NUM];

    ILscNvram*                      m_pNvramOtp;
    ILscBuf*                        m_pLscBuf[SHADING_SUPPORT_CT_NUM];
    ILscBuf*                        m_prLscBufRing[RING_BUF_NUM];
    ILscBuf*                        m_pCurrentBuf;
    std::list<ILscBuf*>             m_rBufPool;
    ILscTsf*                        m_pTsf;

    MBOOL                           m_bDumpSdblk;
    std::string                     m_strSdblkFile;
};
};

#endif //_LSC_MGR_DEFAULT_H_
