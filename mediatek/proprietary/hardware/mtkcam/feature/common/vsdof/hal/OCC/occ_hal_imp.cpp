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

#include "occ_hal_imp.h"
#include <vsdof/hal/stereo_common.h>
#include <vsdof/hal/stereo_size_provider.h>
#include <vsdof/hal/stereo_setting_provider.h>

using namespace StereoHAL;

#define OCC_HAL_DEBUG

#ifdef OCC_HAL_DEBUG    // Enable debug log.

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
#endif  // OCC_HAL_DEBUG

const bool LOG_ENABLED = StereoSettingProvider::isLogEnabled(PERPERTY_DEPTHMAP_NODE_LOG);
const bool BENCHMARK_ENABLED = StereoSettingProvider::isProfileLogEnabled();

OCC_HAL *
OCC_HAL::createInstance()
{
    return new OCC_HAL_IMP();
}

OCC_HAL_IMP::OCC_HAL_IMP()
{
    //Create OCC instance
    m_pOccDrv = MTKOcc::createInstance(DRV_OCC_OBJ_SW); //TODO: check this parameter
    if(NULL == m_pOccDrv) {
        MY_LOGE("Cannot create instance of OCC");
        return;
    }

    //Init OCC
    ::memset(&m_initInfo, 0, sizeof(OccInitInfo));
    //=== Init sizes ===
    MSize inputSize = StereoSizeProvider::getInstance()->getBufferSize(E_DMP_H); //scenario doesn't matter
    m_initInfo.inputWidth = inputSize.w;
    m_initInfo.inputHeight = inputSize.h;
    //
    MSize outputSize = StereoSizeProvider::getInstance()->getBufferSize(E_DMH);   //scenario doesn't matter
    m_initInfo.outputWidth = outputSize.w;
    m_initInfo.outputHeight = outputSize.h;
    //
    m_initInfo.outputX = (inputSize.w-outputSize.w)>>1;
    m_initInfo.outputY = (inputSize.h-outputSize.h)>>1;
    //=== Init tuning info ===
    m_initInfo.pTuningInfo = new OccTuningInfo();
    m_initInfo.pTuningInfo->mainCamPos = (StereoSettingProvider::getSensorRelativePosition() == 0)
                                         ? OCC_MAINCAM_POS_ON_LEFT : OCC_MAINCAM_POS_ON_RIGHT;
    m_initInfo.pTuningInfo->coreNumber = 1;
    m_pOccDrv->Init((void *)&m_initInfo, NULL);
    //Get working buffer size
    m_pOccDrv->FeatureCtrl(OCC_FEATURE_GET_WORKBUF_SIZE, NULL, &m_initInfo.workingBuffSize);

    //Allocate working buffer and set to OCC
    if(m_initInfo.workingBuffSize > 0) {
        m_initInfo.workingBuffAddr = new MUINT8[m_initInfo.workingBuffSize];
    }

    if(m_initInfo.workingBuffAddr) {
        m_pOccDrv->FeatureCtrl(OCC_FEATURE_SET_WORKBUF_ADDR, &m_initInfo, 0);
    }

    _dumpInitData();
}

OCC_HAL_IMP::~OCC_HAL_IMP()
{
    if(m_initInfo.pTuningInfo) {
        delete m_initInfo.pTuningInfo;
        m_initInfo.pTuningInfo = NULL;
    }

    if(m_initInfo.workingBuffAddr) {
        delete [] m_initInfo.workingBuffAddr;
        m_initInfo.workingBuffAddr = NULL;
    }

    if(m_pOccDrv) {
        m_pOccDrv->Reset();
        m_pOccDrv->destroyInstance(m_pOccDrv);
        m_pOccDrv = NULL;
    }
}

bool
OCC_HAL_IMP::OCCHALRun(OCC_HAL_PARAMS &occHalParam, OCC_HAL_OUTPUT &occHalOutput)
{
    bool bResult = true;
    _setOCCParams(occHalParam);
    _runOCC(occHalOutput);
    return bResult;
}

void
OCC_HAL_IMP::_setOCCParams(OCC_HAL_PARAMS &occHalParam)
{
    m_procInfo.dispL = (DVEC_HW*)occHalParam.disparityLeftToRight;
    m_procInfo.dispL = (DVEC_HW*)occHalParam.disparityRightToLEft;

    MUINT32 bufferIndex = 0;
    //================================
    //  Set input data
    //================================
    //OCC input buffer has the same size
    MSize size = StereoSizeProvider::getInstance()->getBufferSize(E_MV_Y);

    //Main1 image
    OccBufferInfo main1ImgInfo;
    main1ImgInfo.type           = OCC_BUFFER_TYPE_DS_L;
    main1ImgInfo.format         = OCC_IMAGE_YV12;
    main1ImgInfo.width          = size.w;
    main1ImgInfo.height         = size.h;
    main1ImgInfo.planeAddr0     = (PEL*)occHalParam.imageMain1->getBufVA(0);
    main1ImgInfo.planeAddr1     = (PEL*)occHalParam.imageMain1->getBufVA(1);
    main1ImgInfo.planeAddr2     = (PEL*)occHalParam.imageMain1->getBufVA(2);
    main1ImgInfo.planeAddr3     = NULL;
    m_procInfo.bufferInfo[bufferIndex++] = main1ImgInfo;

    //Main2 image
    OccBufferInfo main2ImgInfo;
    main2ImgInfo.type           = OCC_BUFFER_TYPE_DS_R;
    main2ImgInfo.format         = OCC_IMAGE_YV12;
    main2ImgInfo.width          = size.w;
    main2ImgInfo.height         = size.h;
    main2ImgInfo.planeAddr0     = (PEL*)occHalParam.imageMain2->getBufVA(0);
    main2ImgInfo.planeAddr1     = (PEL*)occHalParam.imageMain2->getBufVA(1);
    main2ImgInfo.planeAddr2     = (PEL*)occHalParam.imageMain2->getBufVA(2);
    main2ImgInfo.planeAddr3     = NULL;
    m_procInfo.bufferInfo[bufferIndex++] = main2ImgInfo;

    //LDC
    OccBufferInfo ldcInfo;
    ldcInfo.type                = OCC_BUFFER_TYPE_LDC;
    ldcInfo.format              = OCC_IMAGE_YONLY;
    ldcInfo.width               = size.w;
    ldcInfo.height              = size.h;
    ldcInfo.planeAddr0          = (PEL*)occHalParam.ldcMain1;
    ldcInfo.planeAddr1          = NULL;
    ldcInfo.planeAddr2          = NULL;
    ldcInfo.planeAddr3          = NULL;
    m_procInfo.bufferInfo[bufferIndex++] = ldcInfo;

    //Confidence map
    OccBufferInfo confidenceMap;
    confidenceMap.type          = OCC_BUFFER_TYPE_CONF_IN;
    confidenceMap.format        = OCC_IMAGE_YONLY;
    confidenceMap.width         = size.w;
    confidenceMap.height        = size.h;
    confidenceMap.planeAddr0    = (PEL*)occHalParam.confidenceMap;
    confidenceMap.planeAddr1    = NULL;
    confidenceMap.planeAddr2    = NULL;
    confidenceMap.planeAddr3    = NULL;
    m_procInfo.bufferInfo[bufferIndex++] = confidenceMap;

    m_procInfo.numOfBuffer = bufferIndex;
    //================================
    //  Set to OCC
    //================================
    m_pOccDrv->FeatureCtrl(OCC_FEATURE_SET_PROC_INFO, &m_procInfo, 0);
    _dumpSetProcData();
}

void
OCC_HAL_IMP::_runOCC(OCC_HAL_OUTPUT &occHalOutput)
{
    //================================
    //  Run OCC
    //================================
    struct timespec t_start, t_end, t_result;
    clock_gettime(CLOCK_MONOTONIC, &t_start);

    m_pOccDrv->Main();

    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    if(LOG_ENABLED && BENCHMARK_ENABLED) {
        MY_LOGD("OCC Running Time: %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);
    }

    //================================
    //  Get result
    //================================
    ::memset(&m_resultInfo, 0, sizeof(OccResultInfo));
    m_pOccDrv->FeatureCtrl(OCC_FEATURE_GET_RESULT, &m_resultInfo, 0);

    //================================
    //  Rotate image and depth if needed,
    //  then copy result
    //================================
    MSize szResult;
    ENUM_ROTATION rotation = StereoSettingProvider::getModuleRotation();
    if( eRotate_90  == rotation ||
        eRotate_270 == rotation )
    {
        for(MUINT32 i = 0; i < m_resultInfo.numOfBuffer; i++) {
            szResult = MSize(m_resultInfo.bufferInfo[i].width, m_resultInfo.bufferInfo[i].height);
            if( szResult.w != StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.w ||
                szResult.h != StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.h )
            {
                MY_LOGW("Not expected size: expect(%d x %d), result(%d x %d)",
                         StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.w,
                         StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.h,
                         szResult.w, szResult.h);
            }

            if(OCC_BUFFER_TYPE_DS == m_resultInfo.bufferInfo[i].type) {
                //=== Rotate image ===
                //Create dst image buffer
                sp<IImageBuffer> tmpBuf;
                if(allocImageBuffer(eImgFmt_YV12, szResult, !ALLOC_GB, tmpBuf)) {
                    //Copy DS to image buffer
                    ::memcpy((void*)tmpBuf.get()->getBufVA(0), m_resultInfo.bufferInfo[i].planeAddr0, szResult.w*szResult.h);
                    ::memcpy((void*)tmpBuf.get()->getBufVA(1), m_resultInfo.bufferInfo[i].planeAddr1, (szResult.w*szResult.h)>>2);
                    ::memcpy((void*)tmpBuf.get()->getBufVA(2), m_resultInfo.bufferInfo[i].planeAddr2, (szResult.w*szResult.h)>>2);

                    //Rotate by MDP
                    transformImage(tmpBuf.get(), occHalOutput.downScaledImg, rotation);
                }

                freeImageBuffer(tmpBuf);
            } else if(OCC_BUFFER_TYPE_DEPTH == m_resultInfo.bufferInfo[i].type) {
                //=== Rotate depth map ===
                //Allocate dst depth map
                sp<IImageBuffer> tmpBuf;
                if(allocImageBuffer(eImgFmt_Y8, szResult, !ALLOC_GB, tmpBuf)) {
                    //Copy Depth map to image buffer
                    ::memcpy((void*)tmpBuf.get()->getBufVA(0), m_resultInfo.bufferInfo[i].planeAddr0, szResult.w*szResult.h);

                    //Rotate by MDP
                    rotateBuffer(m_resultInfo.bufferInfo[i].planeAddr0, (MUINT8*)occHalOutput.depthMap, szResult, rotation);
                }
                freeImageBuffer(tmpBuf);
            }
        }
    } else {
        for(MUINT32 i = 0; i < m_resultInfo.numOfBuffer; i++) {
            szResult = MSize(m_resultInfo.bufferInfo[i].width, m_resultInfo.bufferInfo[i].height);
            if( szResult.w != StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.w ||
                szResult.h != StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.h )
            {
                MY_LOGW("Not expected size: expect(%d x %d), result(%d x %d)",
                         StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.w,
                         StereoSizeProvider::getInstance()->getBufferSize(E_MY_S).size.h,
                         szResult.w, szResult.h);
            }

            if(OCC_BUFFER_TYPE_DS == m_resultInfo.bufferInfo[i].type) {
                //Copy DS
                ::memcpy((void*)occHalOutput.downScaledImg->getBufVA(0), m_resultInfo.bufferInfo[i].planeAddr0, szResult.w*szResult.h);
                ::memcpy((void*)occHalOutput.downScaledImg->getBufVA(1), m_resultInfo.bufferInfo[i].planeAddr1, (szResult.w*szResult.h)>>2);
                ::memcpy((void*)occHalOutput.downScaledImg->getBufVA(2), m_resultInfo.bufferInfo[i].planeAddr2, (szResult.w*szResult.h)>>2);
            } else if(OCC_BUFFER_TYPE_DEPTH == m_resultInfo.bufferInfo[i].type) {
                //Copy Depth map
                ::memcpy(occHalOutput.depthMap, m_resultInfo.bufferInfo[i].planeAddr0, szResult.w*szResult.h);
            }
        }
    }

    _dumpOCCResult();
}

void
OCC_HAL_IMP::_dumpInitData()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= OCC Init Info =========");
    //m_initInfo
}

void
OCC_HAL_IMP::_dumpSetProcData()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= OCC Set Proc Info =========");
    //m_procInfo
}

void
OCC_HAL_IMP::_dumpOCCResult()
{
    if(!LOG_ENABLED) {
        return;
    }

    MY_LOGD("========= OCC Output Info =========");
    //m_resultInfo
}