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
#ifndef _STEREODEPTH_HAL_BASE_H_
#define _STEREODEPTH_HAL_BASE_H_

// Need to include following header file if you want to use this .h.
//#include "MTKWarp.h"            // Must put before #include "stereodepth_hal_base.h". For WarpImageExtInfo struct.
//#include "MTKStereoKernel.h"    // Must put before #include "stereodepth_hal_base.h". For STEREO_KERNEL_GET_WIN_REMAP_INFO_STRUCT struct.

using namespace NSCam;

/**************************************************************************
 *                      D E F I N E S / M A C R O S                       *
 **************************************************************************/

/**************************************************************************
 *     E N U M / S T R U C T / T Y P E D E F    D E C L A R A T I O N     *
 **************************************************************************/
// Follow STEREODEPTH_TUNING_PARAM_STRUCT in MTKStereoDepth.h
typedef struct
{
    MFLOAT  *pNvRamDataArray;
    MFLOAT  *pCoordTransParam;
    MFLOAT  stereo_fov_main;    // in degree
    MFLOAT  stereo_fov_main2;   // in degree
    MFLOAT  stereo_baseline;    // in cm
    MUINT32 stereo_pxlarr_width;
    MUINT32 stereo_pxlarr_height;
    MUINT16 stereo_main12_pos;  // 0:main(left), main1(right) ; 1: else

} STEREODEPTH_HAL_INIT_PARAM_STRUCT, *P_STEREODEPTH_HAL_INIT_PARAM_STRUCT;


typedef struct
{
    MUINT af_win_start_x;
    MUINT af_win_start_y;
    MUINT af_win_end_x;
    MUINT af_win_end_y;

} AF_WIN_COORDINATE_STRUCT, *P_AF_WIN_COORDINATE_STRUCT;

#if 0   // Obesolete.
typedef struct
{
    // TOUCH_bound_xL, TOUCH_bound_xR, TOUCH_bound_yT, TOUCH_bound_yD - definition of touch panel
    //             yD (-1000)
    //          ---------------
    //         |               |
    //   xL    |               |  xR
    // (-1000) |    +(X,Y)     |(1000)
    //          ---------------
    //            yT ( 1000)
    MINT32 TOUCH_bound_xL;
    MINT32 TOUCH_bound_xR;
    MINT32 TOUCH_bound_yT;
    MINT32 TOUCH_bound_yD;

    // MDP
    MINT32 MDP_offset_x;
    MINT32 MDP_offset_y;
    MINT32 MDP_in_size_w;
    MINT32 MDP_in_size_h;
    MINT32 MDP_out_size_w;
    MINT32 MDP_out_size_h;
    MINT32 MDP_crop_w;
    MINT32 MDP_crop_h;

    // GPU
    WarpImageExtInfo *pGPU_Grid; // defined in MTKWarp.h.

    // ALGO IN to SENSOR
    STEREO_KERNEL_GET_WIN_REMAP_INFO_STRUCT gStereoKernelGetWinRemapInfo ; // defined in MTKStereoKernel.h

} COORDINATE_REMAPPING_PARAM_STRUCT;
#endif  // Obesolete.

/**************************************************************************
 *                 E X T E R N A L    R E F E R E N C E S                 *
 **************************************************************************/

/**************************************************************************
 *        P U B L I C    F U N C T I O N    D E C L A R A T I O N         *
 **************************************************************************/

/**************************************************************************
 *                   C L A S S    D E C L A R A T I O N                   *
 **************************************************************************/

class StereoDepthHalBase
{
public:
    static StereoDepthHalBase* createInstance();
    virtual void destroyInstance() = 0;
    virtual bool init() = 0;
    virtual bool uninit() = 0;
    virtual ~StereoDepthHalBase() {};

    virtual bool StereoDepthInit(P_STEREODEPTH_HAL_INIT_PARAM_STRUCT pstStereodepthHalInitParam) = 0;
    virtual bool StereoDepthSetParams(P_STEREODEPTH_HAL_INIT_PARAM_STRUCT pstStereodepthHalInitParam) = 0;
    virtual bool StereoDepthRunLearning(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate) = 0;

    //Deprecate in the future
    virtual bool StereoDepthRunQuerying(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate) = 0;

    virtual bool StereoDepthQuery(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate) = 0;
    virtual bool StereoDepthUninit(void) = 0;
//    virtual bool CoordinateRemapping(void) = 0;   // Obsolete
    virtual void StereoDepthPrintDafTable(void) = 0;

protected:

private:

};

#endif  // _STEREODEPTH_HAL_BASE_H_

