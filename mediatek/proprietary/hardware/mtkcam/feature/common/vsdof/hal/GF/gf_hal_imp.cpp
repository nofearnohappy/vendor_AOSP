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
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/

#include "gf_hal_imp.h"
#include <vsdof/hal/stereo_common.h>
#include <vsdof/hal/stereo_size_provider.h>
#include <vsdof/hal/stereo_setting_provider.h>
#include <vsdof/hal/stereo_tuning_provider.h>

using namespace StereoHAL;

#define GF_HAL_DEBUG

#ifdef GF_HAL_DEBUG    // Enable debug log.

#undef __func__
#define __func__ __FUNCTION__

#define MY_LOGD(fmt, arg...)    ALOGD("[%s]" fmt, __func__, ##arg)
#define MY_LOGI(fmt, arg...)    ALOGI("[%s]" fmt, __func__, ##arg)
#define MY_LOGW(fmt, arg...)    ALOGW("[%s] WRN(%5d):" fmt, __func__, __LINE__, ##arg)
#define MY_LOGE(fmt, arg...)    ALOGE("[%s] %s ERROR(%5d):" fmt, __func__,__FILE__, __LINE__, ##arg)

#else   // Disable debug log.
#define MY_LOGD(a,...)
#define MY_LOGI(a,...)
#define MY_LOGW(a,...)
#define MY_LOGE(a,...)
#endif  // GF_HAL_DEBUG

const bool LOG_ENABLED = StereoSettingProvider::isLogEnabled(PERPERTY_BOKEH_NODE_LOG);
const bool BENCHMARK_ENABLED = StereoSettingProvider::isProfileLogEnabled();

GF_HAL *
GF_HAL::createInstance()
{
    return new GF_HAL_IMP();
}

GF_HAL_IMP::GF_HAL_IMP()
{
    m_pGfDrv = MTKGF::createInstance(DRV_GF_OBJ_SW);
    //Init GF
    ::memset(&m_initInfo, 0, sizeof(GFInitInfo));
    //=== Init sizes ===
    MSize inputSize = StereoSizeProvider::getInstance()->getBufferSize(E_MY_S); //scenario doesn't matter
    m_initInfo.inputWidth  = inputSize.w;
    m_initInfo.inputHeight = inputSize.h;
    //
    MSize outputSize = StereoSizeProvider::getInstance()->getBufferSize(E_DMW);   //scenario doesn't matter
    m_initInfo.outputWidth  = outputSize.w;
    m_initInfo.outputHeight = outputSize.h;
    //=== Init tuning info ===
    m_initInfo.pTuningInfo = new GFTuningInfo();
    m_initInfo.pTuningInfo->coreNumber = 1;
    m_pGfDrv->Init((void *)&m_initInfo, NULL);
    //Get working buffer size
    m_pGfDrv->FeatureCtrl(GF_FEATURE_GET_WORKBUF_SIZE, NULL, &m_initInfo.workingBuffSize);

    //Allocate working buffer and set to GF
    if(m_initInfo.workingBuffSize > 0) {
        m_initInfo.workingBuffAddr = new MUINT8[m_initInfo.workingBuffSize];
    }

    if(m_initInfo.workingBuffAddr) {
        m_pGfDrv->FeatureCtrl(GF_FEATURE_SET_WORKBUF_ADDR, &m_initInfo, 0);
    }

    _dumpInitData();
};

GF_HAL_IMP::~GF_HAL_IMP()
{
    //Delete working buffer
    if(m_initInfo.pTuningInfo) {
        delete m_initInfo.pTuningInfo;
        m_initInfo.pTuningInfo = NULL;
    }

    if(m_initInfo.workingBuffAddr) {
        delete [] m_initInfo.workingBuffAddr;
        m_initInfo.workingBuffAddr = NULL;
    }

    if(m_pGfDrv) {
        m_pGfDrv->Reset();
        m_pGfDrv->destroyInstance(m_pGfDrv);
        m_pGfDrv = NULL;
    }
}

bool
GF_HAL_IMP::GFHALRun(GF_HAL_IN_DATA &inData, GF_HAL_OUT_DATA &outData)
{
    bool bResult = true;
    _setGFParams(inData);
    _runGF(outData);
    return bResult;
}

void
GF_HAL_IMP::_setGFParams(GF_HAL_IN_DATA &gfHalParam)
{
    m_procInfo.touchTrigger = gfHalParam.isTouch;
    gfHalParam.otROI;   //TODO: assign to set proc

    m_procInfo.touchX = gfHalParam.touchPoint.x;
    m_procInfo.touchY = gfHalParam.touchPoint.y;
    m_procInfo.dof = gfHalParam.dofLevel;
//    m_procInfo.depthValue;
    m_procInfo.clearRange = StereoTuningProvider::getGFTuningInfo();

    m_procInfo.numOfBuffer = 2; //depthMap and DS

    MSize size = StereoSizeProvider::getInstance()->getBufferSize(E_MY_S);
    m_procInfo.bufferInfo[0].type       = GF_BUFFER_TYPE_DEPTH;
    m_procInfo.bufferInfo[0].format     = GF_IMAGE_YONLY;
    m_procInfo.bufferInfo[0].width      = size.w;
    m_procInfo.bufferInfo[0].height     = size.h;
    m_procInfo.bufferInfo[0].planeAddr0 = (PEL*)gfHalParam.depthMap;
    m_procInfo.bufferInfo[0].planeAddr1 = NULL;
    m_procInfo.bufferInfo[0].planeAddr2 = NULL;
    m_procInfo.bufferInfo[0].planeAddr3 = NULL;

    m_procInfo.bufferInfo[1].type       = GF_BUFFER_TYPE_DS;
    m_procInfo.bufferInfo[1].format     = GF_IMAGE_YV12;
    m_procInfo.bufferInfo[1].width      = size.w;
    m_procInfo.bufferInfo[1].height     = size.h;
    m_procInfo.bufferInfo[1].planeAddr0 = (PEL*)gfHalParam.downScaledImg->getBufVA(0);
    m_procInfo.bufferInfo[1].planeAddr1 = (PEL*)gfHalParam.downScaledImg->getBufVA(1);
    m_procInfo.bufferInfo[1].planeAddr2 = (PEL*)gfHalParam.downScaledImg->getBufVA(2);
    m_procInfo.bufferInfo[1].planeAddr3 = NULL;

    m_pGfDrv->FeatureCtrl(GF_FEATURE_SET_PROC_INFO, &m_procInfo, 0);

    _dumpSetProcData();
}

void
GF_HAL_IMP::_runGF(GF_HAL_OUT_DATA &gfHalOutput)
{
    //================================
    //  Run GF
    //================================
    struct timespec t_start, t_end, t_result;
    clock_gettime(CLOCK_MONOTONIC, &t_start);

    m_pGfDrv->Main();

    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    if(LOG_ENABLED && BENCHMARK_ENABLED) {
        MY_LOGD("GF Running Time: %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);
    }

    //================================
    //  Get result
    //================================
    ::memset(&m_resultInfo, 0, sizeof(GFResultInfo));
    m_pGfDrv->FeatureCtrl(GF_FEATURE_GET_RESULT, &m_resultInfo, 0);

    //================================
    //  Copy result to GF_HAL_OUT_DATA
    //================================
    for(int i = 0; i < m_resultInfo.numOfBuffer; i++) {
        if(GF_BUFFER_TYPE_BMAP == m_resultInfo.bufferInfo[i].type) {
            ::memcpy(gfHalOutput.bmap, m_resultInfo.bufferInfo[i].planeAddr0, m_resultInfo.bufferInfo[i].width*m_resultInfo.bufferInfo[i].height);
        }
    }
}

void
GF_HAL_IMP::_dumpInitData()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= GF Init Info =========");
    //m_initInfo
}

void
GF_HAL_IMP::_dumpSetProcData()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= GF Proc Data =========");
    //m_procInfo
}

void
GF_HAL_IMP::_dumpGFResult()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= GF Result =========");
    //m_resultInfo
}
