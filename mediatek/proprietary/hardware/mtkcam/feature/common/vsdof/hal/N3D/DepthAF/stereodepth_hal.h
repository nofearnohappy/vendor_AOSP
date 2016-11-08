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
#ifndef _STEREODEPTH_HAL_H_
#define _STEREODEPTH_HAL_H_

//#include <stdlib.h>
//#include <utils/threads.h>
//#include <cutils/atomic.h>  // Mutex.
//#include "common.h"
//#include "MTKWarp.h"            // Must put before #include "stereodepth_hal_base.h". For WarpImageExtInfo struct.
//#include "MTKStereoKernel.h"    // Must put before #include "stereodepth_hal_base.h". For STEREO_KERNEL_GET_WIN_REMAP_INFO_STRUCT struct.
//#include "stereodepth_hal_base.h"
//#include "MTKStereoDepth.h" // For MTKStereoDepth.
//#include "mtkcam/featureio/IHal3A.h"    // For IHal3A.
//#include "aaa_hal_common.h"   // For DAF_TBL_STRUCT.

//using android::Mutex;
//using namespace NS3A;
/**************************************************************************
 *                      D E F I N E S / M A C R O S                       *
 **************************************************************************/

/**************************************************************************
 *     E N U M / S T R U C T / T Y P E D E F    D E C L A R A T I O N     *
 **************************************************************************/

/**************************************************************************
 *                 E X T E R N A L    R E F E R E N C E S                 *
 **************************************************************************/

/**************************************************************************
 *        P U B L I C    F U N C T I O N    D E C L A R A T I O N         *
 **************************************************************************/

/**************************************************************************
 *                   C L A S S    D E C L A R A T I O N                   *
 **************************************************************************/
class StereoDepthHal
{

public:
    StereoDepthHal();
    virtual ~StereoDepthHal();

//    virtual void destroyInstance();
//    virtual bool init();
//    virtual bool uninit();
//
//    virtual bool StereoDepthInit(P_STEREODEPTH_HAL_INIT_PARAM_STRUCT pstStereodepthHalInitParam);
//    virtual bool StereoDepthSetParams(P_STEREODEPTH_HAL_INIT_PARAM_STRUCT pstStereodepthHalInitParam);
//    virtual bool StereoDepthRunLearning(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate);
//
//    //Deprecate in the future
//    virtual bool StereoDepthRunQuerying(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate);
//
//    virtual bool StereoDepthQuery(MUINT16 u2NumHwfeMatch, MFLOAT *pfHwfeMatchData, P_AF_WIN_COORDINATE_STRUCT pstAfWinCoordinate);
//    virtual bool StereoDepthUninit(void);
//    virtual void StereoDepthPrintDafTable(void);

private:
//    volatile MINT32         mUsers;
//    mutable Mutex           mLock;
//    bool                    mfRunFirstTime; // A flag used to make StereoDepthSetParams() only run once.
//    //
//    MTKStereoDepth*         m_pStereoDepthDrv;
//    //
//    IHal3A*                 m_pHal3A;
//    //
//    DAF_TBL_STRUCT          *m_prDafTbl;

};

#endif  // _STEREODEPTH_HAL_H_

