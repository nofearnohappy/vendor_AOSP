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
#ifndef _STEREO_SETTING_PROVIDER_H_
#define _STEREO_SETTING_PROVIDER_H_

#include <common.h>
#include <utils/include/common.h>                // For property

using namespace NSCam;
using namespace android;

#define PROPERTY_ENABLE_LOG         STEREO_PROPERTY_PREFIX"enable_log"
#define PERPERTY_PASS1_LOG          PROPERTY_ENABLE_LOG".Pass1"
#define PERPERTY_DEPTHMAP_NODE_LOG  PROPERTY_ENABLE_LOG".DepthMapNode"
#define PERPERTY_BOKEH_NODE_LOG     PROPERTY_ENABLE_LOG".BokehNode"
#define PERPERTY_DIT_NODE_LOG       PROPERTY_ENABLE_LOG".DualImageTransformNode"
#define PERPERTY_JENC_NODE_LOG      PROPERTY_ENABLE_LOG".JpegEncodeNode"
#define PERPERTY_VENC_NODE_LOG      PROPERTY_ENABLE_LOG".VideoEncodeNode"
#define PROPERTY_ENABLE_PROFILE_LOG STEREO_PROPERTY_PREFIX"enable_profile_log"

/**************************************************************************
 *                      D E F I N E S / M A C R O S                       *
 **************************************************************************/

/**************************************************************************
 *     E N U M / S T R U C T / T Y P E D E F    D E C L A R A T I O N     *
 **************************************************************************/
enum STEREO_RATIO_E
{
    eRatio_Unknown
    , eRatio_16_9   // 16:9, default 1920x1080
    , eRatio_4_3    //  4:3, default 1600x1200
    , eRatio_Default = eRatio_16_9   //[Note] Put this line at last
};

struct STEREO_PARAMS_T
{
    char *jpsSize;          // stereo picture size
    char *jpsSizesStr;      // supported stereo picture size
    char *refocusSize;      // refocus picture size
    char *refocusSizesStr;  // supported refocus picture size
    MUINT32 previewFps;     // preview frame rate
    MUINT32 captureFps;     // capture frame rate

public:     ////    Operations.
    STEREO_PARAMS_T()  { ::memset(this, 0, sizeof(STEREO_PARAMS_T)); }
};

struct IMAGE_RESOLUTION_INFO_STRUCT
{
    MSize   szMainCam;          //1920x1080 1920x1440
    MSize   szSubCam;           //1600x1200
    MUINT32 uRatioNumerator;    //9         3
    MUINT32 uRatioDenomerator;  //16        4

    IMAGE_RESOLUTION_INFO_STRUCT()
    {
        szMainCam           = MSize(1920, 1080);
        szSubCam            = MSize(1600, 1200);
        uRatioNumerator     = 9;
        uRatioDenomerator   = 16;
    }

    IMAGE_RESOLUTION_INFO_STRUCT(MSize szMain, MSize szSub, MUINT32 d, MUINT32 n)
    {
        szMainCam           = szMain;
        szSubCam            = szSub;
        uRatioNumerator     = n;
        uRatioDenomerator   = d;
    }
};

enum ENUM_ROTATION
{
    //Rotation is defined as clock-wised
    eRotate_0   = 0,
    eRotate_90  = 90,
    eRotate_180 = 180,
    eRotate_270 = 270
};

enum SENSOR_RELATIVE_POS_E
{
    eMain_Main2,
    eMain2_Main
};
/**************************************************************************
 *                 E X T E R N A L    R E F E R E N C E S                 *
 **************************************************************************/

/**************************************************************************
 *        P U B L I C    F U N C T I O N    D E C L A R A T I O N         *
 **************************************************************************/

/**************************************************************************
 *                   C L A S S    D E C L A R A T I O N                   *
 **************************************************************************/

class StereoSettingProvider
{
public:
    /**
     * Returns sensor index of stereo main and main2(index in 0, 1, 2)
     * @param  main1 sensor index
     * @param  main2 sensor index
     * @return true if successfully get both index; otherwise false
     */
    static bool getStereoSensorIndex(int32_t &main1Idx, int32_t &main2Idx);
    /**
     * Returns sensor dev index of stereo main and main2(index in 1, 2, 4)
     * @param  main1 Saves the dev index of main stereo sensor
     * @param  main2 Saves the dev index of another stereo sensor
     * @return true if successfully get both index; otherwise false
     */
    static bool getStereoSensorDevIndex(int32_t &main1DevIdx, int32_t &main2DevIdx);
    //
    static bool setImageRatio(STEREO_RATIO_E ratio);
    static STEREO_RATIO_E &imageRatio() { static STEREO_RATIO_E ratio = eRatio_Default; return ratio; }
    static IMAGE_RESOLUTION_INFO_STRUCT &imageResolution() { static IMAGE_RESOLUTION_INFO_STRUCT resolution; return resolution; }
    //
    static bool hasHWFE();
    static MUINT32 fefmBlockSize(const int FE_MODE);
    static bool getStereoCameraFOV(float &mainFOV, float &main2FOV);
    static float getStereoCameraFOVRatio();  //main2_fov / main1_fov
    static ENUM_ROTATION getModuleRotation() { return eRotate_0; }
    /**
     * Get sensor relative position
     * @return 0: main-main2 (main in L)
     *         1: main2-main (main in R)
     */
    static unsigned int getSensorRelativePosition();

    static unsigned int getJPSTransformFlag();    //return conbination of eTransform_FLIP_H, eTransform_ROT_90, etc

    static bool enableLog();    //Globally enable log, each node can decide to log or not
    static bool enableLog(const char *LOG_PROPERTY_NAME);  //enable log of each node
    static bool disableLog();   //Globally disable log
    static bool isLogEnabled(); //Check if global log is enabled
    static bool isLogEnabled(const char *LOG_PROPERTY_NAME);   //Check log status of each node, refers to global log switch
    static bool isProfileLogEnabled(); //Check if global profile log is enabled
protected:

private:

};

#endif  // _STEREO_SETTING_PROVIDER_H_

