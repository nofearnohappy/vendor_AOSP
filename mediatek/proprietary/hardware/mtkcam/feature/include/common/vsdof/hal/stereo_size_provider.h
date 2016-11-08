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
#ifndef _STEREO_SIZE_PROVIDER_H_
#define _STEREO_SIZE_PROVIDER_H_

#include <cutils/atomic.h>
#include <imageio/ispio_utility.h>
#include <imageio/ispio_pipe_ports.h>
#include "stereo_common.h"
#include "pass2_size_provider_base.h"
#include "stereo_setting_provider.h"

#define STEREO_SIZE_PROVIDER_DEBUG

#ifdef STEREO_SIZE_PROVIDER_DEBUG    // Enable debug log.

#undef __func__
#define __func__ __FUNCTION__

#define SIZE_PROVIDER_LOGD(fmt, arg...)    ALOGD("[%s]" fmt, __func__, ##arg)
#define SIZE_PROVIDER_LOGI(fmt, arg...)    ALOGI("[%s]" fmt, __func__, ##arg)
#define SIZE_PROVIDER_LOGW(fmt, arg...)    ALOGW("[%s] WRN(%5d):" fmt, __func__, __LINE__, ##arg)
#define SIZE_PROVIDER_LOGE(fmt, arg...)    ALOGE("[%s] %s ERROR(%5d):" fmt, __func__,__FILE__, __LINE__, ##arg)

#else   // Disable debug log.
#define SIZE_PROVIDER_LOGD(a,...)
#define SIZE_PROVIDER_LOGI(a,...)
#define SIZE_PROVIDER_LOGW(a,...)
#define SIZE_PROVIDER_LOGE(a,...)
#endif  // STEREO_SIZE_PROVIDER_DEBUG

enum ENUM_BUFFER_NAME
{
    //N3D Output
    E_MV_Y,
    E_MASK_M_Y,
    E_SV_Y,
    E_MASK_S_Y,
    E_LDC,

    //N3D before MDP for capture
    E_MV_Y_LARGE,
    E_MASK_M_Y_LARGE,
    E_SV_Y_LARGE,
    E_MASK_S_Y_LARGE,

    //DPE Output
    E_DMP_H,
    E_CFM_H,
    E_RESPO,

    //OCC Output
    E_MY_S,
    E_DMH,

    //WMF Output
    E_DMW,
    E_DEPTH_MAP,

    //Output
    E_DMG,
    E_DMBG,

    //Bokeh Output
    E_BOKEH_WROT, //VSDOF image
    E_BOKEH_WDMA, //Clean image
};

//using namespace NSImageio;
using namespace NSImageio::NSIspio;
using namespace StereoHAL;

class StereoSizeProvider
{
public:
    static StereoSizeProvider *getInstance();
    static void destroyInstance();

    //For Pass 1
    bool getPass1Size( ENUM_STEREO_SENSOR sensor,
                       EImageFormat format,
                       EPortIndex port,
                       ENUM_STEREO_SCENARIO scenario,
                       //below are outputs
                       MSize &size,
                       MUINT32 &strideInBytes ) const;

    //For Pass 2, will rotate according to module orientation and ratio inside
    bool getPass2SizeInfo(ENUM_PASS2_ROUND round, ENUM_STEREO_SCENARIO eScenario, Pass2SizeInfo &pass2SizeInfo);

    //For rests, will rotate according to module orientation and ratio inside
    StereoArea getBufferSize(ENUM_BUFFER_NAME eName, ENUM_STEREO_SCENARIO eScenario = eSTEREO_SCENARIO_UNKNOWN) const;

protected:
    StereoSizeProvider() {}
    virtual ~StereoSizeProvider();

private:
    static Mutex            mLock;
    static volatile MINT32  mUsers;

    static StereoSizeProvider *_instance;
};

#endif