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
#ifndef _I_LSC_MGR_H_
#define _I_LSC_MGR_H_

#include <aaa_types.h>
#include <isp_tuning.h>
#include <Local.h>

#include <ILscBuf.h>
#include <ILscTbl.h>

namespace NSIspTuning {
#define EN_LSC_LOG_UPDATE       (0x1 << 0)
#define EN_LSC_LOG_GET_CT       (0x1 << 1)
#define EN_LSC_LOG_SET_CT       (0x1 << 2)
#define EN_LSC_LOG_SET_TABLE    (0x1 << 3)
#define EN_LSC_LOG_THREAD       (0x1 << 4)
#define EN_LSC_LOG_TSF_SET_PROC (0x1 << 5)
#define EN_LSC_LOG_TSF_RUN      (0x1 << 6)
#define EN_LSC_LOG_TSF_BATCH    (0x1 << 7)
#define EN_LSC_LOG_TSF_DUMP     (0x1 << 8)
#define EN_LSC_LOG_TSF_REINIT   (0x1 << 9)

/*******************************************************************************
 * Interface of LSC Manager
 *******************************************************************************/
class ILscMgr
{ 
public:
    typedef enum
    {
        E_TSF_CMD_IDLE      = 0,
        E_TSF_CMD_INIT      = 1,
        E_TSF_CMD_RUN       = 2,
        E_TSF_CMD_BATCH     = 3,
        E_TSF_CMD_BATCH_CAP = 4,
        E_TSF_CMD_CHG       = 5,
    } E_TSF_CMD_T;
    
    typedef struct 
    {    
        MUINT32 m_u4CCT;
        MINT32  m_i4LV;
        MINT32  m_RGAIN;
        MINT32  m_GGAIN;
        MINT32  m_BGAIN;
        MINT32  m_FLUO_IDX;
        MINT32  m_DAY_FLUO_IDX;
    } TSF_AWB_INFO;

    typedef struct 
    {
        E_TSF_CMD_T     eCmd;
        MUINT32         u4FrmId;
        TSF_AWB_INFO    rAwbInfo;
        MUINT32         u4SizeAwbStat;
        MUINT8*         prAwbStat;
    } TSF_INPUT_INFO_T;

    typedef enum 
    {
        LSC_SCENARIO_PRV   = 0,    //     ESensorMode_Preview,
        LSC_SCENARIO_CAP   = 1,    //     ESensorMode_Capture,    
        LSC_SCENARIO_VDO   = 2,    //     ESensorMode_Video,
        LSC_SCENARIO_SLIM1 = 3,    //     ESensorMode_SlimVideo1,
        LSC_SCENARIO_SLIM2 = 4,    //     ESensorMode_SlimVideo2,
        LSC_SCENARIO_CUST1 = 5,    //     ESensorMode_Custom1,
        LSC_SCENARIO_CUST2 = 6,    //     ESensorMode_Custom2,
        LSC_SCENARIO_CUST3 = 7,    //     ESensorMode_Custom3,
        LSC_SCENARIO_CUST4 = 8,    //     ESensorMode_Custom4,
        LSC_SCENARIO_CUST5 = 9,    //     ESensorMode_Custom5,
        LSC_SCENARIO_NUM
    } ELscScenario_T;

    static ILscMgr*                 createInstance(
                                        ESensorDev_T const eSensorDev,
                                        MINT32 i4SensorIdx);

    static ILscMgr*                 getInstance(ESensorDev_T sensor);

    virtual MBOOL                   destroyInstance() = 0;

    virtual MBOOL                   init() = 0;
    virtual MBOOL                   uninit() = 0;

    virtual MBOOL                   setSensorMode(ESensorMode_T eSensorMode, MUINT32 u4Width, MUINT32 u4Height, MBOOL fgForce=MFALSE) = 0;
    virtual ESensorMode_T           getSensorMode() const = 0;
    virtual const ILscTbl::TransformCfg_T& getTransformCfg(ESensorMode_T eSensorMode) const = 0;

    virtual MUINT32                 getCTIdx() = 0;
    virtual MBOOL                   setCTIdx(MUINT32 const u4CTIdx) = 0;
    virtual MVOID                   setOnOff(MBOOL fgOnOff) = 0;
    virtual MBOOL                   getOnOff() const = 0;
    virtual MBOOL                   setRatio(MUINT32 u4Ratio) = 0;
    virtual MUINT32                 getRatio() const = 0;

    virtual ILscBuf*                createBuf(const char* name) = 0;
    virtual MBOOL                   destroyBuf(ILscBuf* pBuf) = 0;
    virtual MBOOL                   syncBuf(ILscBuf* pBuf) = 0;
    virtual MBOOL                   syncTbl(const ILscTbl& rTbl) = 0;
//    virtual ESensorDev_T            getSensorDev() const = 0;

    virtual const ILscTbl*          getLut(ESensorMode_T eSensorMode, MUINT32 u4CtIdx) const = 0;
//    virtual MUINT32                 getTotalLutSize(ELscScenario_T eLscScn) const = 0;
//    virtual MUINT32                 getPerLutSize(ELscScenario_T eLscScn) const = 0;

//    virtual MBOOL                   readNvramTbl(MBOOL fgForce) = 0;
//    virtual MBOOL                   writeNvramTbl(void) = 0;

    virtual MINT32                  getGainTable(MUINT32 u4Bayer, MUINT32 u4GridNumX, MUINT32 u4GridNumY, MFLOAT* pGainTbl) = 0;
//    virtual MINT32                  setGainTable(MUINT32 u4GridNumX, MUINT32 u4GridNumY, MUINT32 u4Width, MUINT32 u4Height, float* pGainTbl) = 0;
//    virtual MINT32                  setCoefTable(const MUINT32* pCoefTbl) = 0;

    virtual MINT32                  getDebugInfo(SHADING_DEBUG_INFO_T &rShadingDbgInfo) = 0;
    virtual MINT32                  getDebugTbl(DEBUG_SHAD_ARRAY_INFO_T &rShadingDbgTbl, DEBUG_SHAD_ARRAY_2_T& rShadRestTbl) = 0;
    
    virtual MINT32                  CCTOPSetSdblkFileCfg(MBOOL fgSave, const char* filename) = 0;
//    virtual MINT32                  CCTOPSetBypass123(MBOOL fgBypass) = 0;
//    virtual MINT32                  setTsfForceAwb(MBOOL fgForce) = 0;

    virtual MVOID                   updateLsc() = 0;
    virtual MVOID                   updateTsf(const TSF_INPUT_INFO_T& rInputInfo) = 0;
    virtual MVOID                   setTsfOnOff(MBOOL fgOnOff) = 0;
    virtual MBOOL                   getTsfOnOff() const = 0;

protected:
    virtual                         ~ILscMgr(){}
};

};
#endif // _I_LSC_MGR_H_
