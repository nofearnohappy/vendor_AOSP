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
#define LOG_TAG "lsc_mgr_dft_misc"
#ifndef ENABLE_MY_LOG
#define ENABLE_MY_LOG           (1)
#define GLOBAL_ENABLE_MY_LOG    (1)
#endif

#include "LscMgrDefault.h"
#include <LscUtil.h>

#include <isp_mgr.h>

using namespace NSIspTuning;
using namespace NSIspTuningv3;

MINT32
LscMgrDefault::
CCTOPSetSdblkFileCfg(MBOOL fgSave, const char* filename)
{
    m_bDumpSdblk = fgSave;
    m_strSdblkFile = filename;
    return 0;
}

inline static void setDebugTag(SHADING_DEBUG_INFO_T &a_rCamDebugInfo, MINT32 a_i4ID, MINT32 a_i4Value)
{
    a_rCamDebugInfo.Tag[a_i4ID].u4FieldID = AAATAG(AAA_DEBUG_SHADING_MODULE_ID, a_i4ID, 0);
    a_rCamDebugInfo.Tag[a_i4ID].u4FieldValue = a_i4Value;
}

MINT32 
LscMgrDefault::
getDebugInfo(SHADING_DEBUG_INFO_T &rShadingDbgInfo)
{
    ISP_NVRAM_LSC_T debug;

    LSC_LOG_BEGIN();

    ISP_MGR_LSC_T::getInstance(m_eSensorDev).get(debug);
    MUINT32 u4Addr = ISP_MGR_LSC_T::getInstance(m_eSensorDev).getAddr();

    ::memset(&rShadingDbgInfo, 0, sizeof(rShadingDbgInfo));
    setDebugTag(rShadingDbgInfo, SHAD_TAG_VERSION, (MUINT32)SHAD_DEBUG_TAG_VERSION);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_1TO3_EN, (MUINT32)m_e1to3Flag);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_SCENE_IDX, (MUINT32)m_eSensorMode);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CT_IDX, (MUINT32)m_u4CTIdx);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_CTL_DMA_EN, (MUINT32)m_fgOnOff);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSCI_BASE_ADDR, (MUINT32)u4Addr);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_CTL_EN1, (MUINT32)m_fgOnOff);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_CTL1, (MUINT32)debug.ctl1.val);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_CTL2, (MUINT32)debug.ctl2.val);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_CTL3, (MUINT32)debug.ctl3.val);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_LBLOCK, (MUINT32)debug.lblock.val);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_RATIO, (MUINT32)debug.ratio.val);
    setDebugTag(rShadingDbgInfo, SHAD_TAG_CAM_LSC_GAIN_TH, (MUINT32)0/*debug.gain_th.val*/);

    // Tsf
    const MINT32* pTsfExif = NULL;
    MBOOL fgTsfOnOff = MFALSE;
    if (m_pTsf)
    {
        MUINT32 u4Type = m_pTsf->getType();
        if (ILscTsf::E_LSC_TSF_TYPE_OpenShading != u4Type)
        {
            fgTsfOnOff = m_pTsf->getOnOff();
            pTsfExif = static_cast<const MINT32*>(m_pTsf->getRsvdData());
        }
        setDebugTag(rShadingDbgInfo, SHAD_TAG_SCENE_IDX, (MUINT32)(u4Type<<16)|m_eSensorMode);
    }

    setDebugTag(rShadingDbgInfo, SHAD_TAG_TSF_EN, fgTsfOnOff);
    if (pTsfExif && fgTsfOnOff)
    {
        MINT32 i;
        for (i = SHAD_TAG_CNT1; i < SHAD_TAG_END; i++)
        {
            setDebugTag(rShadingDbgInfo, i, *pTsfExif++);
        }
    }

    // SDBLK dump
    if (m_bDumpSdblk && m_pCurrentBuf)
    {
        if (!m_pCurrentBuf->dump(m_strSdblkFile.c_str()))
        {
            LSC_ERR("Fail to dump %s", m_strSdblkFile.c_str());
        }
    }

    LSC_LOG_END();
    
    return 0;
}

MINT32
LscMgrDefault::
getDebugTbl(DEBUG_SHAD_ARRAY_INFO_T &rShadingDbgTbl, DEBUG_SHAD_ARRAY_2_T& rShadRestTbl)
{
    ISP_NVRAM_LSC_T debug;
    
    LSC_LOG_BEGIN();

    ISP_MGR_LSC_T::getInstance(m_eSensorDev).get(debug);

    ::memset(&rShadingDbgTbl, 0, sizeof(DEBUG_SHAD_ARRAY_INFO_T));
    ::memset(&rShadRestTbl, 0, sizeof(DEBUG_SHAD_ARRAY_2_T));
    
    rShadingDbgTbl.hdr.u4KeyID = DEBUG_SHAD_TABLE_KEYID;
    rShadingDbgTbl.hdr.u4ModuleCount = ModuleNum<1, 0>::val;
    rShadingDbgTbl.hdr.u4DbgSHADArrayOffset = sizeof(DEBUG_SHAD_ARRAY_INFO_S::Header);

    rShadingDbgTbl.rDbgSHADArray.u4BlockNumX = debug.ctl2.bits.LSC_SDBLK_XNUM + 1;
    rShadingDbgTbl.rDbgSHADArray.u4BlockNumY = debug.ctl3.bits.LSC_SDBLK_YNUM + 1;

    MUINT32 u4Blocks = 
        rShadingDbgTbl.rDbgSHADArray.u4BlockNumX *
        rShadingDbgTbl.rDbgSHADArray.u4BlockNumY;

    MUINT32 u4RestBlocks = 0;

    if (u4Blocks >= APPN_SHAD_BLOCK_NUM_MAX)
    {
        u4RestBlocks = u4Blocks - APPN_SHAD_BLOCK_NUM_MAX;
        u4Blocks = APPN_SHAD_BLOCK_NUM_MAX;
    }

    rShadingDbgTbl.rDbgSHADArray.u4CountU32 = u4Blocks*4*4;
    const MUINT32* pu4Addr = m_pCurrentBuf->getTable();
    ::memcpy(rShadingDbgTbl.rDbgSHADArray.u4Array, pu4Addr, u4Blocks*4*4*sizeof(MUINT32));

    rShadRestTbl.u4CountU32 = u4RestBlocks*4*4;
    if (u4RestBlocks)
    {
        ::memcpy(rShadRestTbl.u4Array, (MUINT32*) pu4Addr+SHAD_ARRAY_VALUE_SIZE, u4RestBlocks*4*4*sizeof(MUINT32));
    }

    LSC_LOG_END("X(%d),Y(%d),Cnt(%d),Cnt2(%d)",
        rShadingDbgTbl.rDbgSHADArray.u4BlockNumX,
        rShadingDbgTbl.rDbgSHADArray.u4BlockNumY,
        rShadingDbgTbl.rDbgSHADArray.u4CountU32,
        rShadRestTbl.u4CountU32);
    
    return 0;
}
/*
MBOOL
LscMgrDefault::
setSwNr()
{
    ISP_NVRAM_SL2_T rSl2Cfg;
    ISP_MGR_SL2_T::getInstance(m_eSensorDev).get(rSl2Cfg);

    LSC_LOG("SL2 cen(0x%08x), rr0(0x%08x), rr1(0x%08x), rr2(0x%08x)",
        rSl2Cfg.cen, rSl2Cfg.max0_rr, rSl2Cfg.max1_rr, rSl2Cfg.max2_rr);
#if 0       
    SwNRParam::getInstance(m_i4SensorIdx)->setSL2B(
        rSl2Cfg.cen.bits.SL2_CENTR_X,
        rSl2Cfg.cen.bits.SL2_CENTR_Y,
        rSl2Cfg.max0_rr.val,
        rSl2Cfg.max1_rr.val,
        rSl2Cfg.max2_rr.val);
#endif
    return MTRUE;
}
*/
MINT32
LscMgrDefault::
getGainTable(MUINT32 u4Bayer, MUINT32 u4GridNumX, MUINT32 u4GridNumY, MFLOAT* pGainTbl)
{
    #warning "hardcode table"
    *pGainTbl++ = 1.0f;
    *pGainTbl++ = 1.0f;
    *pGainTbl++ = 1.0f;
    *pGainTbl++ = 1.0f;
    return 0;
}

