/*********************************************************************************************
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

#define LOG_TAG "n3d_hal"

#include <stdlib.h>     // for rand()

#include <Log.h>
#include "n3d_hal_imp.h"         // For N3D_HAL class.

#include <IHalSensor.h>
#include <nvbuf_util.h>
#include <Hal3/IHal3A.h>
#include <hal/inc/camera_custom_stereo.h>  // For CUST_STEREO_* definitions.
#include <ui/GraphicBuffer.h>
#include <math.h>

#include <vsdof/hal/rapidjson/writer.h>
#include <vsdof/hal/rapidjson/stringbuffer.h>
#include <vsdof/hal/rapidjson/document.h>     // rapidjson's DOM-style API
#include <vsdof/hal/rapidjson/prettywriter.h> // for stringify JSON
#include <vsdof/hal/rapidjson/filewritestream.h>
#include <vsdof/hal/rapidjson/writer.h>

#define N3D_HAL_DEBUG

#ifdef N3D_HAL_DEBUG    // Enable debug log.

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
#endif  // N3D_HAL_DEBUG

using namespace NSCam;
using namespace android;
using namespace rapidjson;
using namespace std;
using namespace StereoHAL;
using android::Mutex;           // For android::Mutex in stereo_hal.h.

/**************************************************************************
 *                      D E F I N E S / M A C R O S                       *
 **************************************************************************/
#define HAS_HEFE    1
#define HAS_HEFM    2
#define HW_GEO_SETTING (HAS_HEFE|HAS_HEFM)

#define PROPERTY_ENABLE_VERIFY  STEREO_PROPERTY_PREFIX"enable_verify"
#define PERPERTY_ENABLE_CC      STEREO_PROPERTY_PREFIX"enable_cc"
#define PROPERTY_ALGO_BEBUG     STEREO_PROPERTY_PREFIX"dbgdump"

#define MODULE_ROTATE 0         //Should align to StereoSettigProvider::getModuleRotation()

const MFLOAT RRZ_CAPIBILITY = 0.25f;

/**************************************************************************
 *     E N U M / S T R U C T / T Y P E D E F    D E C L A R A T I O N     *
 **************************************************************************/

/**************************************************************************
 *                 E X T E R N A L    R E F E R E N C E S                 *
 **************************************************************************/

/**************************************************************************
 *                         G L O B A L    D A T A                         *
 **************************************************************************/

/**************************************************************************
 *       P R I V A T E    F U N C T I O N    D E C L A R A T I O N        *
 **************************************************************************/
Mutex N3D_HAL_IMP::mLock;

/**************************************************************************
 *       Public Functions                                                 *
 **************************************************************************/

N3D_HAL *
N3D_HAL::createInstance()
{
    return new N3D_HAL_IMP();
}

N3D_HAL_IMP::N3D_HAL_IMP()
    : m_pWorkBuf(NULL)
    , m_eScenario(eSTEREO_SCENARIO_UNKNOWN)
    , m_pStereoDrv(NULL)
    , m_main1Mask(NULL)
//    , m_pAFTable(NULL)
    , m_stereoExtraData(NULL)
{
    m_pStereoDrv = MTKStereoKernel::createInstance();
}

N3D_HAL_IMP::~N3D_HAL_IMP()
{
    if(m_main1Mask) {
        delete [] m_main1Mask;
        m_main1Mask = NULL;
    }

    if(m_main2Mask) {
        delete [] m_main2Mask;
        m_main2Mask = NULL;
    }
}

bool
N3D_HAL_IMP::N3DHALInit(N3D_HAL_INIT_PARAM &n3dInitParam)
{
    m_eScenario = n3dInitParam.eScenario;
    switch(n3dInitParam.eScenario) {
        case eSTEREO_SCENARIO_PREVIEW:
            m_algoInitInfo.scenario = STEREO_KERNEL_SCENARIO_IMAGE_PREVIEW;
            break;
        case eSTEREO_SCENARIO_CAPTURE:
            m_algoInitInfo.scenario = STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF;
            break;
        case eSTEREO_SCENARIO_RECORD:
            m_algoInitInfo.scenario = STEREO_KERNEL_SCENARIO_VIDEO_RECORD;
            break;
        default:
            break;
    }

    //init m_main1Mask
    StereoArea areaMask = StereoSizeProvider::getInstance()->getBufferSize(E_MASK_M_Y, m_eScenario);
    MUINT32 length = areaMask.size.w * areaMask.size.h;
    if(length > 0) {
        if(NULL == m_main1Mask) {
            m_main1Mask = new MUINT8[length];
            ::memset(m_main1Mask, 0, sizeof(MUINT8)*length);
            MUINT8 *startPos = m_main1Mask+areaMask.startPt.x;
            const MUINT32 END_Y = areaMask.size.h - areaMask.startPt.y;
            const MUINT32 CONTENT_W = areaMask.contentSize().w * sizeof(MUINT8);
            for(int y = areaMask.startPt.y; y < END_Y; y++) {
                ::memset(startPos, 0xFF, CONTENT_W);
                startPos += areaMask.size.w;
            }
        }
    } else {
        MY_LOGE("Size of MASK_M_Y is 0");
        return false;
    }

    // ALGORITHM INPUT and SbS OUTPUT
    Pass2SizeInfo pass2SizeInfo;
    StereoSizeProvider::getInstance()->getPass2SizeInfo(PASS2A_2, m_eScenario, pass2SizeInfo);
    m_algoInitInfo.algo_source_image_width  = pass2SizeInfo.areaWDMA.size.w;
    m_algoInitInfo.algo_source_image_height = pass2SizeInfo.areaWDMA.size.h;

    MSize szOutput = StereoSizeProvider::getInstance()->getBufferSize(E_MV_Y, m_eScenario);
    m_algoInitInfo.algo_output_image_width  = szOutput.w;
    m_algoInitInfo.algo_output_image_height = szOutput.h;
    // HWFE INPUT - the actual size for HWFE (after SRZ)
    m_algoInitInfo.geo_level  = n3dInitParam.fefmRound;   //N3D_HAL_INIT_PARAM.fefmRound
    m_algoInitInfo.is_geo_hw  = HW_GEO_SETTING;           // 2bits, NONE, HWFE, HWFM, 0, 1, 3 (no SWFE+HWFM)
    _initN3DGeoInfo(m_algoInitInfo.geo_img);              //FEFM setting

    // COLOR CORRECTION INPUT
    _initCCImgInfo(m_algoInitInfo.pho_img);       //settings of main = auxi

    // Learning
    StereoSettingProvider::getStereoCameraFOV(m_algoInitInfo.hori_fov_main, m_algoInitInfo.hori_fov_auxi);
    m_algoInitInfo.stereo_baseline      = STEREO_BASELINE;
    m_algoInitInfo.sensor_config        = StereoSettingProvider::getSensorRelativePosition();
    m_algoInitInfo.module_orientation   = ( eRotate_90  == StereoSettingProvider::getModuleRotation() ||
                                            eRotate_270 == StereoSettingProvider::getModuleRotation() );
    m_algoInitInfo.is_output_warping    = 1;

    //Get min/max dac
    const char *HAL3A_QUERY_NAME = "MTKStereoCamera";
    int32_t main1Idx, main2Idx;
    StereoSettingProvider::getStereoSensorIndex(main1Idx, main2Idx);
    IHal3A *pHal3A = IHal3A::createInstance(IHal3A::E_Camera_1, main1Idx, HAL3A_QUERY_NAME);
//    pHal3A->send3ACtrl(E3ACtrl_GetDAFTBL, (MUINTPTR)&m_pAFTable, 0);
//    MY_LOGD("m_pAFTable %p", &m_pAFTable);
//
//    //Since af_mgr::init may run later, we have to wait for it
//    for(int nTimes = 10; nTimes > 0; nTimes--) {
//        m_algoInitInfo.af_dac_start = m_pAFTable->af_dac_min;
//        if (0 == m_algoInitInfo.af_dac_start) {
//            MY_LOGD("Waiting for af_dac_min...");
//            usleep(20 * 1000);
//        } else {
//            break;
//        }
//    }

    if (0 == m_algoInitInfo.af_dac_start) {
        MY_LOGE("Cannot get af_dac_min");
    }
    pHal3A->destroyInstance(HAL3A_QUERY_NAME);

    // WARPING / CROPING
    m_algoInitInfo.enable_cc        = 1;
    // Only change if the property is set
    int nCheckCC = checkStereoProperty(PERPERTY_ENABLE_CC, false);
    if(nCheckCC >= 0) {
        MY_LOGD("Override CC setting: %d", nCheckCC);
        m_algoInitInfo.enable_cc = nCheckCC;
    }
    m_algoInitInfo.enable_gpu       = 1;
    m_algoInitInfo.enable_ac        = 1;
    m_algoInitInfo.enable_learning  = 1;
    m_algoInitInfo.enable_verify = (checkStereoProperty(PROPERTY_ENABLE_VERIFY) > 0) ? 1 : 0;

    // Learning Coordinates RE-MAPPING
    _getStereoRemapInfo(m_algoInitInfo.remap_main, m_algoInitInfo.remap_auxi, m_eScenario);

    MRESULT err = m_pStereoDrv->StereoKernelInit(&m_algoInitInfo);
    if (err) {
        MY_LOGE("Init N3D algo failed(%d)", err);
        return false;
    }

    // OUTPUT after Initialization
    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_WORK_BUF_INFO, NULL,
                                                &m_algoInitInfo.working_buffer_size);
    if(err) {
        MY_LOGE("Fail to get working buffer size");
        return false;
    }

    if(!_initWorkingBuffer(m_algoInitInfo.working_buffer_size)) {
        return false;
    }

    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_DEFAULT_TUNING, NULL, &m_algoTuningInfo);
    if (err) {
        MY_LOGE("StereoKernelFeatureCtrl(GET_DEFAULT_TUNING) fail. error code: %d.", err);
        return false;
    } else {
        m_algoInitInfo.ptuning_para = &m_algoTuningInfo;
    }

    _dumpInitInfo(m_algoInitInfo);

    return true;
}

bool
N3D_HAL_IMP::N3DHALRun(N3D_HAL_PARAM &n3dParams, N3D_HAL_OUTPUT &n3dOutput)
{
    _setN3DParams(n3dParams, n3dOutput);
    _runN3D(n3dOutput);
    return true;
}

bool
N3D_HAL_IMP::N3DHALRun(N3D_HAL_PARAM_CAPTURE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput)
{
    _setN3DCaptureParams(n3dParams, n3dOutput);
    _runN3DCapture(n3dOutput);
    return true;
}

bool
N3D_HAL_IMP::N3DHALRun(N3D_HAL_PARAM_CAPTURE_SWFE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput)
{
    _setN3DSWFECaptureParams(n3dParams, n3dOutput);
    _runN3DCapture(n3dOutput);
    return true;
}

char *
N3D_HAL_IMP::getStereoExtraData()
{
    //Only support capture
    if(eSTEREO_SCENARIO_CAPTURE != m_eScenario) {
        return NULL;
    }

    if(NULL == m_stereoExtraData) {
        _prepareStereoExtraData();
    }

    return m_stereoExtraData;
}
/**************************************************************************
 *       Private Functions                                                *
 **************************************************************************/
bool
N3D_HAL_IMP::_getFEOInputInfo(ENUM_PASS2_ROUND pass2Round,
                              ENUM_STEREO_SCENARIO eScenario,
                              STEREO_KERNEL_IMG_INFO_STRUCT &result)
{
    result.depth    = 1;    //pixel depth, YUV:1, RGB: 3, RGBA: 4
    result.format   = 0;    //YUV:0, RGB: 1

    Pass2SizeInfo pass2Info;
    StereoSizeProvider::getInstance()->getPass2SizeInfo(pass2Round, eScenario, pass2Info);

    result.width        = pass2Info.areaFEO.size.w;
    result.height       = pass2Info.areaFEO.size.h;
    result.stride       = result.width;
    result.act_width    = pass2Info.areaFEO.size.w - pass2Info.areaFEO.padding.w;
    result.act_height   = pass2Info.areaFEO.size.h - pass2Info.areaFEO.padding.h;

    if(pass2Round <= PASS2A_3) {
        result.offset_x = 0;
        result.offset_y = 0;
    } else {
        result.offset_x = (result.width - result.act_width)>>1;
        result.offset_y = (result.height - result.act_height)>>1;
    }

    return true;
}

bool
N3D_HAL_IMP::_initN3DGeoInfo(STEREO_KERNEL_GEO_INFO_STRUCT geo_img[])
{
    if(MAX_GEO_LEVEL > 0) {
        geo_img[0].block_size = StereoSettingProvider::fefmBlockSize(1);    //16
        _getFEOInputInfo(PASS2A,        m_eScenario,    geo_img[0].img_main);
        _getFEOInputInfo(PASS2A_P,      m_eScenario,    geo_img[0].img_auxi);
    }

    if(MAX_GEO_LEVEL > 1) {
        geo_img[1].block_size = StereoSettingProvider::fefmBlockSize(1);    //16
        _getFEOInputInfo(PASS2A_2,      m_eScenario,    geo_img[1].img_main);
        _getFEOInputInfo(PASS2A_P_2,    m_eScenario,    geo_img[1].img_auxi);
    }

    if(MAX_GEO_LEVEL > 2) {
        geo_img[2].block_size = StereoSettingProvider::fefmBlockSize(2);    //8
        _getFEOInputInfo(PASS2A_3,      m_eScenario,    geo_img[2].img_main);
        _getFEOInputInfo(PASS2A_P_3,    m_eScenario,    geo_img[2].img_auxi);
    }

    return true;
}

bool
N3D_HAL_IMP::_initCCImgInfo(STEREO_KERNEL_IMG_INFO_STRUCT &ccImgInfo)
{
    Pass2SizeInfo pass2Info;
    StereoSizeProvider::getInstance()->getPass2SizeInfo(PASS2A_3, m_eScenario, pass2Info);
    MSize szCCImg = pass2Info.szIMG2O;
    //
    ccImgInfo.width         = szCCImg.w;
    ccImgInfo.height        = szCCImg.h;
    ccImgInfo.depth         = 1;            //pixel depth, YUV:1, RGB: 3, RGBA: 4
    ccImgInfo.stride        = szCCImg.w;
    ccImgInfo.format        = 0;            //YUV:0, RGB: 1
    ccImgInfo.act_width     = szCCImg.w;
    ccImgInfo.act_height    = szCCImg.h;
    ccImgInfo.offset_x      = 0;
    ccImgInfo.offset_y      = 0;
    //
    return true;
}

MUINT8 *
N3D_HAL_IMP::_initWorkingBuffer(const MUINT32 BUFFER_SIZE)
{
    if (m_pWorkBuf) {
        free(m_pWorkBuf);
    }
    m_pWorkBuf = NULL;

    const MUINT32 BUFFER_LEN = sizeof(MUINT8) * BUFFER_SIZE;
    m_pWorkBuf = (MUINT8*)malloc(BUFFER_LEN) ;
    ::memset(m_pWorkBuf, 0, BUFFER_LEN);
    MY_LOGD("Working buffer size: %d addr %p. ", BUFFER_SIZE, m_pWorkBuf);
    MBOOL bResult = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.
    // Allocate working buffer.
    //     Allocate memory
    //     Set WorkBufInfo
    m_algoWorkBufInfo.ext_mem_size       = BUFFER_SIZE;
    m_algoWorkBufInfo.ext_mem_start_addr = m_pWorkBuf;

    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SET_WORK_BUF_INFO,
                                                &m_algoWorkBufInfo, NULL);
    if (err)
    {
        MY_LOGE("StereoKernelFeatureCtrl(SET_WORK_BUF_INFO) fail. error code: %d.", err);
        return NULL;
    }

    return m_pWorkBuf;
}


void
N3D_HAL_IMP::_setN3DCommonParams(N3D_HAL_PARAM_COMMON &n3dParams, N3D_HAL_OUTPUT &n3dOutput, STEREO_KERNEL_SET_PROC_INFO_STRUCT &result)
{
    result.warp_src_addr_main  = n3dParams.rectifyImgMain1;
    result.warp_dst_addr_main  = n3dOutput.rectifyImgMain1;
    result.warp_msk_addr_main  = n3dOutput.maskMain1;

    result.warp_dst_addr_auxi  = n3dOutput.rectifyImgMain2; //Only for preview/record
    result.warp_msk_addr_auxi  = n3dOutput.maskMain2;       //Only for preview/record

    //TODO: assign ldcMain1

    // for Photometric Correction
    result.pho_src_addr_main   = n3dParams.ccImgMain1;
    result.pho_src_addr_auxi   = n3dParams.ccImgMain2;
    // HWFE
    for(int i = 0; i < MAX_GEO_LEVEL; i++) {
        result.geo_data_main[i] = n3dParams.hwfefmData.geoDataMain1[i];
        result.geo_data_auxi[i] = n3dParams.hwfefmData.geoDataMain2[i];
        result.geo_data_ltor[i] = n3dParams.hwfefmData.geoDataLeftToRight[i];
        result.geo_data_rtol[i] = n3dParams.hwfefmData.geoDataRightToLeft[i];
    }
    // AF INFO, get from af_mgr
    result.af_dac_index ;
    result.af_valid ;
    result.af_confidence ;
    result.af_win_str_x_remap ;
    result.af_win_str_y_remap ;
    result.af_win_end_x_remap ;
    result.af_win_end_y_remap ;
}

void
N3D_HAL_IMP::_setN3DParams(N3D_HAL_PARAM &n3dParams, N3D_HAL_OUTPUT &n3dOutput)
{
    STEREO_KERNEL_SET_PROC_INFO_STRUCT setProcInfo;
    _setN3DCommonParams(n3dParams, n3dOutput, setProcInfo);
    setProcInfo.warp_src_addr_auxi  = n3dOutput.rectifyImgMain2;

    // EIS INFO.
    if(n3dParams.eisData.isON) {
        setProcInfo.eis_vec_x   = n3dParams.eisData.eisOffset.x;
        setProcInfo.eis_vec_y   = n3dParams.eisData.eisOffset.y;
        setProcInfo.eis_width   = n3dParams.eisData.eisImgSize.w;
        setProcInfo.eis_height  = n3dParams.eisData.eisImgSize.h;
    } else {
        setProcInfo.eis_vec_x   = 0;
        setProcInfo.eis_vec_y   = 0;
        setProcInfo.eis_width   = 0;
        setProcInfo.eis_height  = 0;
    }
}

void
N3D_HAL_IMP::_setN3DCaptureParams(N3D_HAL_PARAM_CAPTURE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput)
{
    m_captureOrientation = n3dParams.captureOrientation;
    //
    STEREO_KERNEL_SET_PROC_INFO_STRUCT setProcInfo;
    _setN3DCommonParams(n3dParams, n3dOutput, setProcInfo);
    setProcInfo.src_gb.mGraphicBuffer = (void *)&n3dParams.rectifyGBMain2;
    if(NULL == m_outputGBMain2.get()) {
        StereoArea imgArea = StereoSizeProvider::getInstance()->getBufferSize(E_SV_Y_LARGE);
        allocImageBuffer(eImgFmt_RGBA8888, imgArea.size, ALLOC_GB, m_outputGBMain2);
    }
    setProcInfo.dst_gb.mGraphicBuffer = (void *)m_outputGBMain2.get();

//    sp<GraphicBuffer>* srcGBArray[1];
//    sp<GraphicBuffer>* dstGBArray[1];
//    srcGBArray[0] = (sp<GraphicBuffer>*)&setProcInfo.src_gb;
//    dstGBArray[0] = (sp<GraphicBuffer>*)&setProcInfo.dst_gb;
//    setProcInfo.InputGB   = (void*)&srcGBArray;
//    setProcInfo.OutputGB  = (void*)&dstGBArray;
}

bool
N3D_HAL_IMP::_runN3DCommon(N3D_HAL_OUTPUT &n3dOutput)
{
    bool bResult = true;

    MINT32 err = 0; // 0: no error. other value: error.

    MY_LOGD("StereoKernelMain +");
    err = m_pStereoDrv->StereoKernelMain();
    MY_LOGD("StereoKernelMain -");
    if (err) {
        MY_LOGE("StereoKernelMain() fail. error code: %d.", err);
        bResult = MFALSE;
    }

    // Get result.
    if(!err) {
        err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_RESULT, NULL, &m_algoResult);
        if (err)
        {
            MY_LOGE("StereoKernelFeatureCtrl(GET_RESULT) fail. error code: %d.", err);
            bResult = MFALSE;
        }
    }

    if(!err) {
        //Use MDP to centerize main1 image
        StereoArea imgArea = StereoSizeProvider::getInstance()->getBufferSize(E_MV_Y);
        MUINT32 LENGTH = imgArea.size.w * imgArea.size.h;
        MUINT32 offset = 0;
        sp<IImageBuffer> srcImg;
        sp<IImageBuffer> dstImg;
        //1. Use MDP to resize and centerize main1 image, n3dOutput.rectifyImgMain1
        {
            // Create src image buffer
            allocImageBuffer(eImgFmt_YV12, imgArea.size, !ALLOC_GB, srcImg);
            // Cpoy data to dst image buffer
            MUINT32 offset = 0;
            ::memcpy((void*)srcImg.get()->getBufVA(0), m_algoResult.warp_main, LENGTH);
            offset += LENGTH;
            ::memcpy((void*)srcImg.get()->getBufVA(1), m_algoResult.warp_main+offset, LENGTH>>2);
            offset += (LENGTH>>2);
            ::memcpy((void*)srcImg.get()->getBufVA(2), m_algoResult.warp_main+offset, LENGTH>>2);
            // Create dst image buffer
            allocImageBuffer(eImgFmt_YV12, imgArea.size, !ALLOC_GB, dstImg);
            // rotate by MDP
            DpRect *roi = new DpRect(imgArea.startPt.x, imgArea.startPt.y, imgArea.size.w, imgArea.size.h);  //no need to delete?
            transformImage(srcImg.get(), dstImg.get(), StereoSettingProvider::getModuleRotation(), roi);
            delete roi;

            // Copy rotated image from dst to n3dOutput.rectifyImgMain1
            offset = 0;
            ::memcpy(n3dOutput.rectifyImgMain1,         (MUINT8*)dstImg.get()->getBufVA(0), LENGTH);
            offset += LENGTH;
            ::memcpy(n3dOutput.rectifyImgMain1+offset,  (MUINT8*)dstImg.get()->getBufVA(1), LENGTH>>2);
            offset += (LENGTH>>2);
            ::memcpy(n3dOutput.rectifyImgMain1+offset,  (MUINT8*)dstImg.get()->getBufVA(2), LENGTH>>2);

            freeImageBuffer(srcImg);
            freeImageBuffer(dstImg);
        }

        //Copy mask main1
        StereoArea areaMask = StereoSizeProvider::getInstance()->getBufferSize(E_MASK_M_Y, m_eScenario);
        MUINT32 length = areaMask.size.w * areaMask.size.h;
        memcpy(n3dOutput.maskMain1, m_main1Mask, length * sizeof(MUINT8));
    }

    return bResult;
}

bool
N3D_HAL_IMP::_runN3D(N3D_HAL_OUTPUT &n3dOutput)
{
    bool bResult = true;
    bResult = _runN3DCommon(n3dOutput);

    //TODO: run depth-AF

    return bResult;
}

bool
N3D_HAL_IMP::_runN3DCapture(N3D_HAL_OUTPUT_CAPTURE &n3dOutput)
{
    bool bResult = true;
    bResult = _runN3DCommon(n3dOutput);

    if(eSTEREO_SCENARIO_CAPTURE != m_eScenario) {
        MY_LOGW("Wrong scenario, expect %d, fact: %d", eSTEREO_SCENARIO_CAPTURE, m_eScenario);
        return false;
    }

    if( 1 == checkStereoProperty(PROPERTY_ALGO_BEBUG) ) {
        static MUINT snLogCount = 0;
        m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SAVE_LOG, &snLogCount, NULL);
    }

    //=== Split mask ===
    struct timespec t_start, t_end, t_result;
    clock_gettime(CLOCK_MONOTONIC, &t_start);
    //
    _splitMask();
    //
    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    MY_LOGD("[Benchmark] Split mask: %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);

    //=== Transfer data to JSON ===
    clock_gettime(CLOCK_MONOTONIC, &t_start);
    //
    _prepareStereoExtraData();
    //
    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    MY_LOGD("[Benchmark] Encode extra data: %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);

    StereoArea imgArea = StereoSizeProvider::getInstance()->getBufferSize(E_MV_Y_LARGE);
    MUINT32 LENGTH = imgArea.size.w * imgArea.size.h;
    MUINT32 offset = 0;
    sp<IImageBuffer> srcImg;
    sp<IImageBuffer> dstImg;
    //1. Use MDP to resize and centerize main1 image, n3dOutput.rectifyImgMain1
    {
        // Create src image buffer
        allocImageBuffer(eImgFmt_YV12, imgArea.size, !ALLOC_GB, srcImg);
        // Cpoy data to dst image buffer
        MUINT32 offset = 0;
        ::memcpy((void*)srcImg.get()->getBufVA(0), n3dOutput.rectifyImgMain1, LENGTH);
        offset += LENGTH;
        ::memcpy((void*)srcImg.get()->getBufVA(1), n3dOutput.rectifyImgMain1+offset, LENGTH>>2);
        offset += (LENGTH>>2);
        ::memcpy((void*)srcImg.get()->getBufVA(2), n3dOutput.rectifyImgMain1+offset, LENGTH>>2);
        // Create dst image buffer
        allocImageBuffer(eImgFmt_YV12, imgArea.size, !ALLOC_GB, dstImg);
        // rotate by MDP
        DpRect *roi = new DpRect(imgArea.startPt.x, imgArea.startPt.y, imgArea.size.w, imgArea.size.h);  //no need to delete?
        transformImage(srcImg.get(), dstImg.get(), StereoSettingProvider::getModuleRotation(), roi);
        delete roi;

        // Copy rotated image from dst to n3dOutput.rectifyImgMain1
        offset = 0;
        ::memcpy(n3dOutput.rectifyImgMain1, (MUINT8*)dstImg.get()->getBufVA(0), LENGTH);
        offset += LENGTH;
        ::memcpy(n3dOutput.rectifyImgMain1+offset, (MUINT8*)dstImg.get()->getBufVA(1), LENGTH>>2);
        offset += (LENGTH>>2);
        ::memcpy(n3dOutput.rectifyImgMain1+offset, (MUINT8*)dstImg.get()->getBufVA(2), LENGTH>>2);

        freeImageBuffer(srcImg);
        freeImageBuffer(dstImg);
    }

    //2. Use MDP to convert main2 image(m_outputGBMain2) from RGBA to YV12 for JPS, n3dOutput.jpsImgMain2
    {
        //Transform to YV12 by MDP
        allocImageBuffer(eImgFmt_YV12, imgArea.size, !ALLOC_GB, dstImg);
        transformImage(m_outputGBMain2.get(), dstImg.get());

        //Copy data to n3dOutput.jpsImgMain2
        offset = 0;
        ::memcpy(n3dOutput.jpsImgMain2,         (MUINT8*)dstImg.get()->getBufVA(0), LENGTH);
        offset += LENGTH;
        ::memcpy(n3dOutput.jpsImgMain2+offset,  (MUINT8*)dstImg.get()->getBufVA(1), LENGTH>>2);
        offset += (LENGTH>>2);
        ::memcpy(n3dOutput.jpsImgMain2+offset,  (MUINT8*)dstImg.get()->getBufVA(2), LENGTH>>2);

        //3. Use MDP to resize main2 image, n3dOutput.rectifyImgMain2
        sp<IImageBuffer> resizedImg;
        MSize newSize = StereoSizeProvider::getInstance()->getBufferSize(E_SV_Y);
        if(allocImageBuffer(eImgFmt_YV12, newSize, !ALLOC_GB, resizedImg)) {
            transformImage(dstImg.get(), resizedImg.get());

            //Copy data to n3dOutput.rectifyImgMain2
            const MUINT32 RESIZE_LEN = newSize.w * newSize.h;
            offset = 0;
            ::memcpy(n3dOutput.rectifyImgMain2,         (MUINT8*)resizedImg.get()->getBufVA(0), RESIZE_LEN);
            offset += RESIZE_LEN;
            ::memcpy(n3dOutput.rectifyImgMain2+offset,  (MUINT8*)resizedImg.get()->getBufVA(1), RESIZE_LEN>>2);
            offset += (RESIZE_LEN>>2);
            ::memcpy(n3dOutput.rectifyImgMain2+offset,  (MUINT8*)resizedImg.get()->getBufVA(2), RESIZE_LEN>>2);

            freeImageBuffer(resizedImg);
        }

        freeImageBuffer(dstImg);
    }

    //4. Use MDP to resize main2 mask and truccate, n3dOutput.maskMain2
    {
        sp<IImageBuffer> maskImg;
        if(allocImageBuffer(eImgFmt_Y8, imgArea.size, !ALLOC_GB, maskImg)) {
            //Copy mask to maskImg
            ::memcpy((MUINT8*)maskImg.get()->getBufVA(0), m_main2Mask, LENGTH);
        }

        sp<IImageBuffer> resizedMask;
        MSize newSize = StereoSizeProvider::getInstance()->getBufferSize(E_SV_Y);
        const MUINT32 RESIZE_LEN = newSize.w * newSize.h;
        if(allocImageBuffer(eImgFmt_Y8, newSize, !ALLOC_GB, resizedMask)) {
            transformImage(maskImg.get(), resizedMask.get());

            //Copy data to n3dOutput.maskMain2
            const MUINT32 RESIZE_LEN = newSize.w * newSize.h;
            offset = 0;
            ::memcpy(n3dOutput.maskMain2,         (MUINT8*)resizedMask.get()->getBufVA(0), RESIZE_LEN);
            offset += RESIZE_LEN;
            ::memcpy(n3dOutput.maskMain2+offset,  (MUINT8*)resizedMask.get()->getBufVA(1), RESIZE_LEN>>2);
            offset += (RESIZE_LEN>>2);
            ::memcpy(n3dOutput.maskMain2+offset,  (MUINT8*)resizedMask.get()->getBufVA(2), RESIZE_LEN>>2);
        }

        //Truncate
        struct timespec t_start, t_end, t_result;
        clock_gettime(CLOCK_MONOTONIC, &t_start);
        //
        for(MUINT32 i = 0; i < RESIZE_LEN; i++) {
            *(n3dOutput.maskMain2+i) &= 0XFF;
        }
        //
        clock_gettime(CLOCK_MONOTONIC, &t_end);
        t_result = timeDiff(t_start, t_end);
        MY_LOGD("[Benchmark] Truncate mask: %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);

        freeImageBuffer(maskImg);
        freeImageBuffer(resizedMask);
    }

    return bResult;
}

void
N3D_HAL_IMP::_setN3DSWFECaptureParams(N3D_HAL_PARAM_CAPTURE_SWFE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput)
{
    STEREO_KERNEL_SET_PROC_INFO_STRUCT setProcInfo;
    _setN3DCommonParams(n3dParams, n3dOutput, setProcInfo);
//    MUINT8* geo_src_addr_main[MAX_GEO_LEVEL] ;  //SWFEFM_DATA.geo_src_image_main1
//    MUINT8* geo_src_addr_auxi[MAX_GEO_LEVEL] ;  //SWFEFM_DATA.geo_src_image_main2
}

bool
N3D_HAL_IMP::_getStereoRemapInfo(STEREO_KERNEL_COORD_REMAP_INFO_STRUCT &infoMain1,
                                 STEREO_KERNEL_COORD_REMAP_INFO_STRUCT &infoMain2,
                                 ENUM_STEREO_SCENARIO eScenario)
{
    int sensorScenario = getSensorSenario(eScenario);

    MINT32 err = 0;
    int main1SensorIndex, main2SensorIndex;
    StereoSettingProvider::getStereoSensorIndex(main1SensorIndex, main2SensorIndex);

    int main1SensorDevIndex, main2SensorDevIndex;
    StereoSettingProvider::getStereoSensorDevIndex(main1SensorDevIndex, main2SensorDevIndex);

    IHalSensorList* sensorList = IHalSensorList::get();
    IHalSensor* pIHalSensor = NULL;
    SensorCropWinInfo rSensorCropInfo;
    ::memset(&rSensorCropInfo, 0, sizeof(SensorCropWinInfo));

    if(sensorList) {
        MUINT32 junkStride;
        //========= Get main1 size =========
        IHalSensor* pIHalSensor = sensorList->createSensor(LOG_TAG, main1SensorIndex);
        err = pIHalSensor->sendCommand(main1SensorDevIndex, SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,
                                       (MUINTPTR)&sensorScenario, (MUINTPTR)&rSensorCropInfo, 0);

        if(!err) {
            infoMain1.pixel_array_width  = rSensorCropInfo.full_w;
            infoMain1.pixel_array_height = rSensorCropInfo.full_h ;
            infoMain1.sensor_offset_x0   = rSensorCropInfo.x0_offset ;
            infoMain1.sensor_offset_y0   = rSensorCropInfo.y0_offset ;
            infoMain1.sensor_size_w0     = rSensorCropInfo.w0_size ;
            infoMain1.sensor_size_h0     = rSensorCropInfo.h0_size ;
            infoMain1.sensor_scale_w     = rSensorCropInfo.scale_w ;
            infoMain1.sensor_scale_h     = rSensorCropInfo.scale_h ;
            infoMain1.sensor_offset_x1   = rSensorCropInfo.x1_offset ;
            infoMain1.sensor_offset_y1   = rSensorCropInfo.y1_offset ;
            infoMain1.tg_offset_x        = rSensorCropInfo.x2_tg_offset ;
            infoMain1.tg_offset_y        = rSensorCropInfo.y2_tg_offset ;

            infoMain1.rrz_usage_width    = rSensorCropInfo.w2_tg_size;      //sensor out width;
            infoMain1.rrz_usage_height   = (((rSensorCropInfo.w2_tg_size*9/16)>>1)<<1);

            MSize szMain1RRZO;
            StereoSizeProvider::getInstance()->getPass1Size( eSTEREO_SENSOR_MAIN1,
                                                             eImgFmt_FG_BAYER10,
                                                             EPortIndex_RRZO,
                                                             eScenario,
                                                             //below are outputs
                                                             szMain1RRZO,
                                                             junkStride);
            MINT32 uMaxRRZSize = (MUINT32)ceil(infoMain1.rrz_usage_width * RRZ_CAPIBILITY);
            if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
            if(uMaxRRZSize > szMain1RRZO.w) {
                infoMain1.rrz_out_width  = uMaxRRZSize;
            } else {
                infoMain1.rrz_out_width  = szMain1RRZO.w;
            }

            //rrz_out_height must be an even number
            uMaxRRZSize = (MUINT32)ceil(infoMain1.rrz_usage_height * RRZ_CAPIBILITY);
            if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
            if(uMaxRRZSize > szMain1RRZO.h) {
                infoMain1.rrz_out_height  = uMaxRRZSize;
            } else {
                infoMain1.rrz_out_height  = szMain1RRZO.h;
            }

            infoMain1.rrz_offset_x       = ((rSensorCropInfo.w2_tg_size - infoMain1.rrz_usage_width )>>1 ) ;
            infoMain1.rrz_offset_y       = ((rSensorCropInfo.h2_tg_size - infoMain1.rrz_usage_height)>>1 ) ;

            MY_LOGD("main1 cam");
            MY_LOGD("main full_w %d. main_full_h %d",rSensorCropInfo.full_w,rSensorCropInfo.full_h);
            MY_LOGD("x0_offset %d. y0_offset %d. w0_size %d. h0_size %d.",rSensorCropInfo.x0_offset,rSensorCropInfo.y0_offset,rSensorCropInfo.w0_size,rSensorCropInfo.h0_size);
            MY_LOGD("scale_w %d. scale_h %d",rSensorCropInfo.scale_w,rSensorCropInfo.scale_h);
            MY_LOGD("x1_offset %d. y1_offset %d. w1_size %d. h1_size %d.",rSensorCropInfo.x1_offset,rSensorCropInfo.y1_offset,rSensorCropInfo.w1_size,rSensorCropInfo.h1_size);
            MY_LOGD("x2_tg_offset %d. y2_tg_offset %d. w2_tg_size %d. h2_tg_size %d.",rSensorCropInfo.x2_tg_offset,rSensorCropInfo.y2_tg_offset,rSensorCropInfo.w2_tg_size,rSensorCropInfo.h2_tg_size);

            pIHalSensor->destroyInstance(LOG_TAG);
        }

        //========= Get main2 size =========
        pIHalSensor = sensorList->createSensor(LOG_TAG, main2SensorIndex);
        err = pIHalSensor->sendCommand(main2SensorDevIndex, SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,
                                       (MUINTPTR)&sensorScenario, (MUINTPTR)&rSensorCropInfo, 0);

        if(!err) {
            infoMain2.pixel_array_width  = rSensorCropInfo.full_w;
            infoMain2.pixel_array_height = rSensorCropInfo.full_h ;
            infoMain2.sensor_offset_x0   = rSensorCropInfo.x0_offset ;
            infoMain2.sensor_offset_y0   = rSensorCropInfo.y0_offset ;
            infoMain2.sensor_size_w0     = rSensorCropInfo.w0_size ;
            infoMain2.sensor_size_h0     = rSensorCropInfo.h0_size ;
            infoMain2.sensor_scale_w     = rSensorCropInfo.scale_w ;
            infoMain2.sensor_scale_h     = rSensorCropInfo.scale_h ;
            infoMain2.sensor_offset_x1   = rSensorCropInfo.x1_offset ;
            infoMain2.sensor_offset_y1   = rSensorCropInfo.y1_offset ;
            infoMain2.tg_offset_x        = rSensorCropInfo.x2_tg_offset ;
            infoMain2.tg_offset_y        = rSensorCropInfo.y2_tg_offset ;

            infoMain2.rrz_usage_width    = rSensorCropInfo.w2_tg_size;      //sensor out width;
            infoMain2.rrz_usage_height   = (((rSensorCropInfo.w2_tg_size*9/16)>>1)<<1);

            MSize szMain2RRZO;
            StereoSizeProvider::getInstance()->getPass1Size( eSTEREO_SENSOR_MAIN2,
                                                             eImgFmt_FG_BAYER10,
                                                             EPortIndex_RRZO,
                                                             eScenario,
                                                             //below are outputs
                                                             szMain2RRZO,
                                                             junkStride);
            MINT32 uMaxRRZSize = (MUINT32)ceil(infoMain2.rrz_usage_width * RRZ_CAPIBILITY);
            if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
            if(uMaxRRZSize > szMain2RRZO.w) {
                infoMain2.rrz_out_width  = uMaxRRZSize;
            } else {
                infoMain2.rrz_out_width  = szMain2RRZO.w;
            }

            //rrz_out_height must be an even number
            uMaxRRZSize = (MUINT32)ceil(infoMain2.rrz_usage_height * RRZ_CAPIBILITY);
            if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
            if(uMaxRRZSize > szMain2RRZO.h) {
                infoMain2.rrz_out_height  = uMaxRRZSize;
            } else {
                infoMain2.rrz_out_height  = szMain2RRZO.h;
            }

            infoMain2.rrz_offset_x       = ((rSensorCropInfo.w2_tg_size - infoMain2.rrz_usage_width )>>1 ) ;
            infoMain2.rrz_offset_y       = ((rSensorCropInfo.h2_tg_size - infoMain2.rrz_usage_height)>>1 ) ;

            MY_LOGD("main2 cam");
            MY_LOGD("main full_w %d. main_full_h %d",rSensorCropInfo.full_w,rSensorCropInfo.full_h);
            MY_LOGD("x0_offset %d. y0_offset %d. w0_size %d. h0_size %d.",rSensorCropInfo.x0_offset,rSensorCropInfo.y0_offset,rSensorCropInfo.w0_size,rSensorCropInfo.h0_size);
            MY_LOGD("scale_w %d. scale_h %d",rSensorCropInfo.scale_w,rSensorCropInfo.scale_h);
            MY_LOGD("x1_offset %d. y1_offset %d. w1_size %d. h1_size %d.",rSensorCropInfo.x1_offset,rSensorCropInfo.y1_offset,rSensorCropInfo.w1_size,rSensorCropInfo.h1_size);
            MY_LOGD("x2_tg_offset %d. y2_tg_offset %d. w2_tg_size %d. h2_tg_size %d.",rSensorCropInfo.x2_tg_offset,rSensorCropInfo.y2_tg_offset,rSensorCropInfo.w2_tg_size,rSensorCropInfo.h2_tg_size);
        }
    }

    return true;
}

void
N3D_HAL_IMP::_splitMask()
{
    //====================================================================
    //  SPLITER: Split and rotate mask according to module orientation
    //  Result is stored in m_main2Mask(2176x1152)
    //====================================================================
    const int WIDTH  = m_algoInitInfo.algo_output_image_width;
    const int HEIGHT = m_algoInitInfo.algo_output_image_height;
    const int IMAGE_SIZE = WIDTH * HEIGHT;

    // init other memory for save rotate image.
    if(NULL == m_main2Mask) {
        m_main2Mask = new MUINT8[IMAGE_SIZE];
    }
    ::memset(m_main2Mask, 0, IMAGE_SIZE*sizeof(MUINT8));

    //Get mask from graphic buffer & rotate at the same time
    //Mask is 8-bit image, value: 0 or 0xFF

    //No rotation
    int nCol = 0;
    int nRow = 0;
    int nWritePos = 0;

#if MODULE_ROTATE == 90
    //Rotate 90 degree clockwise
    nCol = HEIGHT - 1;
    nRow = 0;
    nWritePos = nCol;

#elif MODULE_ROTATE == 180
    //Rotate 180 degree
    nWritePos = IMAGE_SIZE - 1;

#elif MODULE_ROTATE == 270
    //Rotate 270 degree clockwise
    nCol = 0;
    nRow = WIDTH - 1;
    nWritePos = IMAGE_SIZE - HEIGHT;
#endif

    //Since graphic buffer is non-cacheable, which means direct access to gbuffer is very slow.
    //So we use cache to read and get mask
    //Performance enhancement is about 300ms -> 66 ms
    const MUINT32 CACHE_SIZE = 128; //experimental result, faster than 64, almost the same as 256, CACHE_SIZE % 8 = 0
    const MUINT32 COPY_SIZE = CACHE_SIZE * sizeof(MUINT32);

    MUINT32* pImgCache = new MUINT32[CACHE_SIZE];
    MUINT32* pGraphicBuffer = (MUINT32*)m_outputGBMain2.get();
    MUINT32* pCachePos = NULL;

    int nIndex = 0;
    for(int nRound = IMAGE_SIZE/CACHE_SIZE - 1; nRound >= 0; nRound--)
    {
        //Cache graphic buffer
        ::memcpy(pImgCache, pGraphicBuffer, COPY_SIZE);
        pGraphicBuffer += CACHE_SIZE;
        pCachePos = pImgCache;

        //Get mask from alpha channel and rotate at a time
        for(nIndex = CACHE_SIZE-1; nIndex >= 0; nIndex--) {
            *(m_main2Mask + nWritePos) = (MUINT8)((*pCachePos)>>24);
            ++pCachePos;

#if MODULE_ROTATE == 90
        //Rotate 90 degree clockwise
        nWritePos += HEIGHT;
        ++nRow;
        if(nRow >= WIDTH) {
            nRow = 0;
            --nCol;
            nWritePos = nCol;
        }

#elif MODULE_ROTATE == 180
        //Rotate 180 degree
        nWritePos--;

#elif MODULE_ROTATE == 270
        //Rotate 270 degree clockwise
        nWritePos -= HEIGHT;
        --nRow;
        if(nRow < 0) {
            nRow = WIDTH - 1;
            ++nCol;
            nWritePos = IMAGE_SIZE - HEIGHT + nCol;
        }
#else
        //No rotation
        ++nWritePos;
#endif
        }
    }
    delete [] pImgCache;
}

bool
N3D_HAL_IMP::N3DHALSaveNVRAM()
{
    MBOOL bResult = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.

    int32_t main1DevIdx, main2DevIdx;
    StereoSettingProvider::getStereoSensorDevIndex(main1DevIdx, main2DevIdx);
    err = NvBufUtil::getInstance().write(CAMERA_NVRAM_DATA_GEOMETRY, main1DevIdx);

    if (err) {
        MY_LOGE("Write to NVRAM fail.");
        bResult = MFALSE;
    }

    MY_LOGD("- X. Result: %d.", bResult);

    return bResult;
}

bool
N3D_HAL_IMP::_loadNVRAM()
{
    MBOOL bResult = MTRUE;

    int32_t main1DevIdx, main2DevIdx;
    StereoSettingProvider::getStereoSensorDevIndex(main1DevIdx, main2DevIdx);
    MINT32 err = NvBufUtil::getInstance().getBufAndRead(CAMERA_NVRAM_DATA_GEOMETRY, main1DevIdx, (void*&)m_pVoidGeoData);
    bResult = !err;
    MY_LOGD("- X. Result: %d.", bResult);
    return bResult;
}

void
N3D_HAL_IMP::_compressMask(std::vector<RUN_LENGTH_DATA> &compressedMask)
{
    compressedMask.clear();

    const int IMAGE_SIZE = m_algoInitInfo.algo_output_image_width * m_algoInitInfo.algo_output_image_height;
    MUINT32 len = 0;
    MUINT32 offset = 0;

    const int CMP_LEN = 128;
    MUINT8 *FF_MASK = new MUINT8[CMP_LEN];
    ::memset(FF_MASK, 0xFF, CMP_LEN);

    for(int i = 0; i < IMAGE_SIZE; i += CMP_LEN) {
        if(0 == memcmp(m_main2Mask, FF_MASK, CMP_LEN)) {
            if(0 == len) {
                offset = i;
            }

            len += CMP_LEN;
            m_main2Mask += CMP_LEN;
        } else {
            for(int j = 0; j < CMP_LEN; j++, m_main2Mask++) {
                if(0 != *m_main2Mask) {
                    if(0 != len) {
                        ++len;
                    } else {
                        len = 1;
                        offset = i+j;
                    }
                } else {
                    if(0 != len) {
                        compressedMask.push_back(RUN_LENGTH_DATA(offset, len));
                        len = 0;
                    }
                }
            }
        }
    }

    if(0 != len) {
        compressedMask.push_back(RUN_LENGTH_DATA(offset, len));
    }

    delete [] FF_MASK;
}

const char *
N3D_HAL_IMP::_prepareStereoExtraData()
{
    if(m_stereoExtraData) {
        delete m_stereoExtraData;
        m_stereoExtraData = NULL;
    }

//    "JPS_size": {
//        "width": 4352,
//        "height": 1152
//    },
    Document document;
    document.SetObject();
    Document::AllocatorType& allocator = document.GetAllocator();

    Value JPS_size(kObjectType);
    JPS_size.AddMember("width", m_algoInitInfo.algo_source_image_width*2, allocator);
    JPS_size.AddMember("height", m_algoInitInfo.algo_source_image_height, allocator);
    document.AddMember("JPS_size", JPS_size, allocator);

//    "output_image_size" : {
//        "width": 2176,
//        "height": 1152
//    },
    Value output_image_size(kObjectType);
    output_image_size.AddMember("width", m_algoInitInfo.algo_output_image_width, allocator);
    output_image_size.AddMember("height", m_algoInitInfo.algo_output_image_height, allocator);
    document.AddMember("output_image_size", output_image_size, allocator);

//    "main_cam_align_shift" : {
//        "x": 30,
//        "y": 10
//    },
    Value main_cam_align_shift(kObjectType);
    main_cam_align_shift.AddMember("x", m_algoResult.algo_align_shift_x, allocator);
    main_cam_align_shift.AddMember("y", m_algoResult.algo_align_shift_y, allocator);
    document.AddMember("main_cam_align_shift", main_cam_align_shift, allocator);

//    "input_image_size": {
//        "width": 1920,
//        "height": 1080
//    },
    Value input_image_size(kObjectType);
    input_image_size.AddMember("width",  m_algoInitInfo.algo_source_image_width,  allocator);
    input_image_size.AddMember("height", m_algoInitInfo.algo_source_image_height, allocator);
    document.AddMember("input_image_size", input_image_size, allocator);

//    "capture_orientation": {
//        "orientations_values": ["0: none", "1: flip_horizontal", "2: flip_vertical", "4: 90", "3: 180", "7: 270"],
//        "orientation": 0
//    },
    Value capture_orientation(kObjectType);
    Value orientations_values(kArrayType);
    orientations_values.PushBack(Value("0: none").Move(), allocator);
    orientations_values.PushBack(Value("1: flip_horizontal").Move(), allocator);
    orientations_values.PushBack(Value("2: flip_vertical").Move(), allocator);
    orientations_values.PushBack(Value("4: 90").Move(), allocator);
    orientations_values.PushBack(Value("3: 180").Move(), allocator);
    orientations_values.PushBack(Value("7: 270").Move(), allocator);
    capture_orientation.AddMember("orientations_values", orientations_values, allocator);
    capture_orientation.AddMember("orientation", Value(m_captureOrientation).Move(), allocator);
    document.AddMember("capture_orientation", capture_orientation, allocator);

//    "sensor_relative_position": {
//        "relative_position_values": ["0: main-minor", "1: minor-main"],
//        "relative_position": 0
//    },
    Value sensor_relative_position(kObjectType);
    Value relative_position_values(kArrayType);
    relative_position_values.PushBack(Value("0: main-minor").Move(), allocator);
    relative_position_values.PushBack(Value("1: minor-main").Move(), allocator);
    sensor_relative_position.AddMember("relative_position_values", relative_position_values, allocator);
    sensor_relative_position.AddMember("relative_position", Value(StereoSettingProvider::getSensorRelativePosition()).Move(), allocator);
    document.AddMember("sensor_relative_position", sensor_relative_position, allocator);

//    "focus_roi": {
//        "top": 0,
//        "left": 10,
//        "bottom": 10,
//        "right": 30
//    },
    Value focus_roi(kObjectType);
    focus_roi.AddMember("top",      Value(m_afROI.leftTop().y).Move(), allocator);
    focus_roi.AddMember("left",     Value(m_afROI.leftTop().x).Move(), allocator);
    focus_roi.AddMember("bottom",   Value(m_afROI.rightBottom().y).Move(), allocator);
    focus_roi.AddMember("right",    Value(m_afROI.rightBottom().x).Move(), allocator);
    document.AddMember("focus_roi", focus_roi, allocator);

//    "verify_geo_data": {
//        "quality_level_values": ["PASS","WARN","FAIL"],
//        "quality_level": 0,
//        "statistics": [0,0,0,0,0,0]
//    },
    Value verify_geo_data(kObjectType);
    Value quality_level_values(kArrayType);
    quality_level_values.PushBack(Value("PASS").Move(), allocator);
    quality_level_values.PushBack(Value("WARN").Move(), allocator);
    quality_level_values.PushBack(Value("FAIL").Move(), allocator);
    verify_geo_data.AddMember("quality_level_values", quality_level_values, allocator);
    verify_geo_data.AddMember("quality_level", Value(m_algoResult.verify_geo_quality_level).Move(), allocator);
    Value geo_statistics(kArrayType);
    for(int i = 0; i < 6; i++) {
        geo_statistics.PushBack(Value(m_algoResult.verify_geo_statistics[0]).Move(), allocator);
    }
    verify_geo_data.AddMember("statistics", geo_statistics, allocator);
    document.AddMember("verify_geo_data", verify_geo_data, allocator);

//    "verify_pho_data": {
//        "quality_level_values": ["PASS","WARN","FAIL"],
//        "quality_level": 0,
//        "statistics": [0,0,0,0]
//    },
    Value verify_pho_data(kObjectType);
    Value pho_quality_level_values(kArrayType);
    pho_quality_level_values.PushBack(Value("PASS").Move(), allocator);
    pho_quality_level_values.PushBack(Value("WARN").Move(), allocator);
    pho_quality_level_values.PushBack(Value("FAIL").Move(), allocator);
    verify_pho_data.AddMember("quality_level_values", pho_quality_level_values, allocator);
    verify_pho_data.AddMember("quality_level", Value(m_algoResult.verify_pho_quality_level).Move(), allocator);
    Value pho_statistics(kArrayType);
    for(int i = 0; i < 4; i++) {
        pho_statistics.PushBack(Value(m_algoResult.verify_pho_statistics[0]).Move(), allocator);
    }
    verify_pho_data.AddMember("statistics", pho_statistics, allocator);
    document.AddMember("verify_pho_data", verify_pho_data, allocator);

//    "verify_mtk_cha": {
//        "quality_level_values": ["PASS","WARN","FAIL"],
//        "quality_level": 0,
//        "statistics": [0,0]
//    },

//    "face_detections" : [
//        {
//            "left": 0,
//            "top": 10,
//            "right": 10,
//            "bottom": 30,
//            "rotation-in-plane": 0
//        },
//        {
//            "left": 20,
//            "top": 30,
//            "right": 40,
//            "bottom": 50,
//            "rotation-in-plane": 11
//        }
//    ],

    //FD information will pass by metadata
//    MtkCameraFaceMetadata FaceInfo;
//    MtkCameraFace FBFaceInfo[15];
//    MtkFaceInfo MTKPoseInfo[15];
//    FaceInfo.faces=(MtkCameraFace *)FBFaceInfo;
//    FaceInfo.posInfo=(MtkFaceInfo *)MTKPoseInfo;
//
//    halFDBase* fdobj = halFDBase::createInstance(HAL_FD_OBJ_FDFT_SW);
//    fdobj->halFDGetFaceInfo(&FaceInfo);
//    fdobj->destroyInstance();
//    MY_LOGD("StereoHal FD number %d ",FaceInfo.number_of_faces);
//    if(FaceInfo.number_of_faces > 0){
//        MY_LOGD("StereoHal FD rect %d, %d, %d, %d",FaceInfo.faces[0].rect[0],FaceInfo.faces[0].rect[1],FaceInfo.faces[0].rect[2],FaceInfo.faces[0].rect[3]);
//        Value face_detections(kArrayType);
//        for(int f = 0; f < FaceInfo.number_of_faces; f++) {
//            Value face(kObjectType);
//            face.AddMember("left",      Value(FaceInfo.faces[f].rect[0]).Move(), allocator);
//            face.AddMember("top",       Value(FaceInfo.faces[f].rect[1]).Move(), allocator);
//            face.AddMember("right",     Value(FaceInfo.faces[f].rect[2]).Move(), allocator);
//            face.AddMember("bottom",    Value(FaceInfo.faces[f].rect[3]).Move(), allocator);
//            face.AddMember("rotation-in-plane", Value(FaceInfo.posInfo[f].rip_dir).Move(), allocator);  //0, 1, ...11
//
//            face_detections.PushBack(face.Move(), allocator);
//        }
//
//        document.AddMember("face_detections", face_detections, allocator);
//    }

//    "mask_info" : {
//        "width":2176,
//        "height":1152,
//        "mask description": "Data(0xFF), format: [offset,length]",
//        "mask": [[28,1296],[1372,1296],[2716,1296],...]
//    }
    Value mask_info(kObjectType);
    mask_info.AddMember("width", m_algoInitInfo.algo_output_image_width, allocator);
    mask_info.AddMember("height", m_algoInitInfo.algo_output_image_height, allocator);
    mask_info.AddMember("mask description", "Data(0xFF), format: [offset,length]", allocator);

    struct timespec t_start, t_end, t_result;
    clock_gettime(CLOCK_MONOTONIC, &t_start);

    std::vector<RUN_LENGTH_DATA> runLengthMaskData;
    _compressMask(runLengthMaskData);

    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    MY_LOGD("[Benchmark] Compress mask: %lu.%.9lu(len:%d)", t_result.tv_sec, t_result.tv_nsec, runLengthMaskData.size());

    clock_gettime(CLOCK_MONOTONIC, &t_start);
    Value mask(kArrayType);
    for(std::vector<RUN_LENGTH_DATA>::iterator it = runLengthMaskData.begin(); it != runLengthMaskData.end(); ++it) {
        Value maskData(kArrayType);
        maskData.PushBack(Value(it->offset).Move(), allocator);
        maskData.PushBack(Value(it->len).Move(), allocator);
        mask.PushBack(maskData.Move(), allocator);
    }

    clock_gettime(CLOCK_MONOTONIC, &t_end);
    t_result = timeDiff(t_start, t_end);
    MY_LOGD("mask(non-zero-run-length): %lu.%.9lu", t_result.tv_sec, t_result.tv_nsec);
    mask_info.AddMember("mask", mask, allocator);
    document.AddMember("mask_info", mask_info, allocator);

    StringBuffer sb;
    Writer<StringBuffer> writer(sb);
    document.Accept(writer);    // Accept() traverses the DOM and generates Handler events.
//    MY_LOGD("JSON: %s:", sb.GetString());

//    FILE* fp = fopen("/sdcard/dbg2.json", "wb"); // non-Windows use "w"
//    char writeBuffer[1024];
//    FileWriteStream os(fp, writeBuffer, sizeof(writeBuffer));
//    Writer<FileWriteStream> writer(os);
//    document.Accept(writer);
//    fclose(fp);

    const char *stereoExtraData = sb.GetString();
    if(stereoExtraData) {
        const int STR_LEN = strlen(stereoExtraData);
        if(STR_LEN > 0) {
            m_stereoExtraData = new char[STR_LEN+1];
            strcpy(m_stereoExtraData, stereoExtraData);
        }
    }

    return m_stereoExtraData;
}

// Logger
void
N3D_HAL_IMP::_dumpInitInfo(STEREO_KERNEL_SET_ENV_INFO_STRUCT &initInfo)
{
    if(!StereoSettingProvider::isLogEnabled(PERPERTY_DEPTHMAP_NODE_LOG)) {
        return;
    }

    MY_LOGD("========= N3D Init Info =========");
    MY_LOGD("[scenario] %d", initInfo.scenario);

    // ALGORITHM INPUT and SbS OUTPUT
    MY_LOGD("[algo_source_image_width]  %d", initInfo.algo_source_image_width);
    MY_LOGD("[algo_source_image_height] %d", initInfo.algo_source_image_height);
    MY_LOGD("[algo_output_image_width]  %d", initInfo.algo_output_image_width);
    MY_LOGD("[algo_output_image_height] %d", initInfo.algo_output_image_height);
    MY_LOGD("[algo_input_image_width]   %d", initInfo.algo_input_image_width);
    MY_LOGD("[algo_input_image_height]  %d", initInfo.algo_input_image_height);

    // HWFE INPUT - the actual size for HWFE (after SRZ)
    MY_LOGD("[geo_level]  %d", initInfo.geo_level);
    MY_LOGD("[is_geo_hw]  %d", initInfo.is_geo_hw);

    char logPrefix[32];
    int i = 0;
    for(i = 0; i < MAX_GEO_LEVEL; i++) {
        MY_LOGD("[geo_img][%d][block_size] %d", i, initInfo.geo_img[i].block_size);

        sprintf(logPrefix, "[geo_img][%d][main]", i);
        _dumpImgInfo(logPrefix, initInfo.geo_img[i].img_main);

        sprintf(logPrefix, "[geo_img][%d][auxi]", i);
        _dumpImgInfo(logPrefix, initInfo.geo_img[i].img_auxi);
    }

    // COLOR CORRECTION INPUT
    _dumpImgInfo("[pho_img]", initInfo.pho_img);

    // Learning
    MY_LOGD("[hori_fov_main]        %.1f", initInfo.hori_fov_main);
    MY_LOGD("[hori_fov_auxi]        %.1f", initInfo.hori_fov_auxi);
    MY_LOGD("[stereo_baseline]      %.1f", initInfo.stereo_baseline);
    MY_LOGD("[sensor_config]        %d", initInfo.sensor_config);
    MY_LOGD("[module_orientation]   %d", initInfo.module_orientation);
    MY_LOGD("[is_output_warping]    %d", initInfo.is_output_warping);
    MY_LOGD("[af_dac_start]         %d", initInfo.af_dac_start);

    // WARPING / CROPING
    MY_LOGD("[enable_cc]            %d", initInfo.enable_cc);
    MY_LOGD("[enable_gpu]           %d", initInfo.enable_gpu);
    MY_LOGD("[enable_verify]        %d", initInfo.enable_verify);
    MY_LOGD("[enable_ac]            %d", initInfo.enable_ac);
    MY_LOGD("[enable_learning]      %d", initInfo.enable_learning);

    // Learning Coordinates RE-MAPPING
    _dumpRemapInfo("[remap_main]", initInfo.remap_main);
    _dumpRemapInfo("[remap_auxi]", initInfo.remap_auxi);

    // OUTPUT after Initialization
    MUINT32 working_buffer_size ;
    MY_LOGD("[working_buffer_size]  %d", initInfo.working_buffer_size);
    _dumpTuningInfo("", *(initInfo.ptuning_para));
}

void
N3D_HAL_IMP::_dumpImgInfo(const char *prefix, STEREO_KERNEL_IMG_INFO_STRUCT &imgInfo)
{
    if(!StereoSettingProvider::isLogEnabled(PERPERTY_DEPTHMAP_NODE_LOG)) {
        return;
    }

    MY_LOGD("[%s][width]      %d", prefix, imgInfo.width);
    MY_LOGD("[%s][height]     %d", prefix, imgInfo.height);
    MY_LOGD("[%s][depth]      %d", prefix, imgInfo.depth);
    MY_LOGD("[%s][stride]     %d", prefix, imgInfo.stride);
    MY_LOGD("[%s][format]     %d", prefix, imgInfo.format);
    MY_LOGD("[%s][act_width]  %d", prefix, imgInfo.act_width);
    MY_LOGD("[%s][act_height] %d", prefix, imgInfo.act_height);
    MY_LOGD("[%s][offset_x]   %d", prefix, imgInfo.offset_x);
    MY_LOGD("[%s][offset_y]   %d", prefix, imgInfo.offset_y);
}

void
N3D_HAL_IMP::_dumpRemapInfo(const char *prefix, STEREO_KERNEL_COORD_REMAP_INFO_STRUCT &remapInfo)
{
    if(!StereoSettingProvider::isLogEnabled(PERPERTY_DEPTHMAP_NODE_LOG)) {
        return;
    }

    MY_LOGD("[%s][pixel_array_width]    %d", prefix, remapInfo.pixel_array_width);
    MY_LOGD("[%s][pixel_array_height]   %d", prefix, remapInfo.pixel_array_height);
    MY_LOGD("[%s][sensor_offset_x0]     %d", prefix, remapInfo.sensor_offset_x0);
    MY_LOGD("[%s][sensor_offset_y0]     %d", prefix, remapInfo.sensor_offset_y0);
    MY_LOGD("[%s][sensor_size_w0]       %d", prefix, remapInfo.sensor_size_w0);
    MY_LOGD("[%s][sensor_size_h0]       %d", prefix, remapInfo.sensor_size_h0);
    MY_LOGD("[%s][sensor_scale_w]       %d", prefix, remapInfo.sensor_scale_w);
    MY_LOGD("[%s][sensor_scale_h]       %d", prefix, remapInfo.sensor_scale_h);
    MY_LOGD("[%s][sensor_offset_x1]     %d", prefix, remapInfo.sensor_offset_x1);
    MY_LOGD("[%s][sensor_offset_y1]     %d", prefix, remapInfo.sensor_offset_y1);
    MY_LOGD("[%s][tg_offset_x]          %d", prefix, remapInfo.tg_offset_x);
    MY_LOGD("[%s][tg_offset_y]          %d", prefix, remapInfo.tg_offset_y);
    MY_LOGD("[%s][rrz_offset_x]         %d", prefix, remapInfo.rrz_offset_x);
    MY_LOGD("[%s][rrz_offset_y]         %d", prefix, remapInfo.rrz_offset_y);
    MY_LOGD("[%s][rrz_usage_width]      %d", prefix, remapInfo.rrz_usage_width);
    MY_LOGD("[%s][rrz_usage_height]     %d", prefix, remapInfo.rrz_usage_height);
    MY_LOGD("[%s][rrz_out_width]        %d", prefix, remapInfo.rrz_out_width);
    MY_LOGD("[%s][rrz_out_height]       %d", prefix, remapInfo.rrz_out_height);
}

void
N3D_HAL_IMP::_dumpTuningInfo(const char *prefix, STEREO_KERNEL_TUNING_PARA_STRUCT &tuningInfo)
{
    if(!StereoSettingProvider::isLogEnabled(PERPERTY_DEPTHMAP_NODE_LOG)) {
        return;
    }

    MY_LOGD("[%s][alg_color]        %d", prefix, tuningInfo.alg_color);
    MY_LOGD("[%s][cc_thr]           %d", prefix, tuningInfo.cc_thr);
    MY_LOGD("[%s][cc_protect_gap]   %d", prefix, tuningInfo.cc_protect_gap);
    MY_LOGD("[%s][learn_tolerance]  %d", prefix, tuningInfo.learn_tolerance);
    MY_LOGD("[%s][search_range_xL]  %f", prefix, tuningInfo.search_range_xL);
    MY_LOGD("[%s][search_range_xR]  %f", prefix, tuningInfo.search_range_xR);
    MY_LOGD("[%s][search_range_yT]  %f", prefix, tuningInfo.search_range_yT);
    MY_LOGD("[%s][search_range_yD]  %f", prefix, tuningInfo.search_range_yD);
}
