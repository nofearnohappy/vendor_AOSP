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
#define LOG_TAG "StereoTuningProvider"

#include <vsdof/hal/stereo_tuning_provider.h>
#include <vsdof/hal/stereo_common.h>
#include <isp_reg.h>

using namespace StereoHAL;

#define PROPERTY_TUNING_PREFIX  STEREO_PROPERTY_PREFIX"tuning."
#define PROPERTY_DPE_TUNING     PROPERTY_TUNING_PREFIX"dpe"     //debug.STEREO.tuning.dpe
                                                                //0: not to read tuning file
                                                                //1: to read/write tuning file
#define PROPERTY_WMF_TUNING     PROPERTY_TUNING_PREFIX"wmf"     //debug.STEREO.tuning.wmf,  1 to read/write tuning file
#define PROPERTY_GF_TUNING      PROPERTY_TUNING_PREFIX"gf"      //debug.STEREO.tuning.gf,   value of 0, 1, 2

#define DPE_TUNING_FILE_PATH    "/sdcard/dpe_params"    //0: Not to read tuning file
#define WMF_TUNING_FILE_PATH    "/sdcard/wmf_params"

#define STEREO_TUNING_PROVIDER_DEBUG

#ifdef STEREO_TUNING_PROVIDER_DEBUG    // Enable debug log.

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
#endif  // STEREO_TUNING_PROVIDER_DEBUG

bool
StereoTuningProvider::getDPETuningInfo(DVEConfig *tuningBuffer)
{
    if(NULL == tuningBuffer) {
        return false;
    }

    static bool useDefault = true;

    int propVal = checkStereoProperty(PROPERTY_DPE_TUNING);
    if(propVal <= 0) {
        //Disable reading file

        if(useDefault) {
            DVEConfig config;
            ::memcpy(tuningBuffer, &config, sizeof(config));    //recover defaul value

            tuningBuffer->Dve_Org_l_Bbox.DVE_ORG_BBOX_LEFT      = 0;
            tuningBuffer->Dve_Org_l_Bbox.DVE_ORG_BBOX_BOTTOM    = 0;
            tuningBuffer->Dve_Org_l_Bbox.DVE_ORG_BBOX_TOP       = 1920;
            tuningBuffer->Dve_Org_l_Bbox.DVE_ORG_BBOX_RIGHT     = 1088;
            tuningBuffer->Dve_Org_r_Bbox.DVE_ORG_BBOX_LEFT      = 0;
            tuningBuffer->Dve_Org_r_Bbox.DVE_ORG_BBOX_BOTTOM    = 0;
            tuningBuffer->Dve_Org_r_Bbox.DVE_ORG_BBOX_TOP       = 1920;
            tuningBuffer->Dve_Org_r_Bbox.DVE_ORG_BBOX_RIGHT     = 1088;
            tuningBuffer->Dve_Cand_0.DVE_CAND_SEL   = 0xB;
            tuningBuffer->Dve_Cand_0.DVE_CAND_TYPE  = 0x1;
            tuningBuffer->Dve_Cand_1.DVE_CAND_SEL   = 0x12;
            tuningBuffer->Dve_Cand_1.DVE_CAND_TYPE  = 0x2;
            tuningBuffer->Dve_Cand_2.DVE_CAND_SEL   = 0x7;
            tuningBuffer->Dve_Cand_2.DVE_CAND_TYPE  = 0x1;
            tuningBuffer->Dve_Cand_3.DVE_CAND_SEL   = 0x1F;
            tuningBuffer->Dve_Cand_3.DVE_CAND_TYPE  = 0x6;
            tuningBuffer->Dve_Cand_4.DVE_CAND_SEL   = 0x19;
            tuningBuffer->Dve_Cand_4.DVE_CAND_TYPE  = 0x3;
            tuningBuffer->Dve_Cand_5.DVE_CAND_SEL   = 0x1A;
            tuningBuffer->Dve_Cand_5.DVE_CAND_TYPE  = 0x3;
            tuningBuffer->Dve_Cand_6.DVE_CAND_SEL   = 0x1C;
            tuningBuffer->Dve_Cand_6.DVE_CAND_TYPE  = 0x4;
            tuningBuffer->Dve_Cand_7.DVE_CAND_SEL   = 0x1C;
            tuningBuffer->Dve_Cand_7.DVE_CAND_TYPE  = 0x4;
            tuningBuffer->DVE_VERT_GMV  = 0;
            tuningBuffer->DVE_HORZ_GMV  = 0;
        }
    } else {
        useDefault = false;
        //if DPE_TUNING_FILE_PATH exists
        struct stat st;
        if(0 == stat(DPE_TUNING_FILE_PATH, &st)) {
            //  read the file
            MY_LOGD("Read DPE parameters...");
            DVEConfig config;
            FILE *fp = fopen(DPE_TUNING_FILE_PATH, "r");
            if(fp) {
                size_t size = fread(&config, sizeof(DVEConfig), 1, fp);
                if(size == sizeof(DVEConfig)) {
                    ::memcpy(tuningBuffer, &config, sizeof(config));
                } else {
                    MY_LOGW("Incorrect DPE parameters size");
                }

                fclose(fp);
                MY_LOGD("Read DPE parameters...done");
            } else {
                MY_LOGW("Cannot read DPE parameters");
            }
        } else {
            //  dump config to file
            MY_LOGD("Dump DPE parameters...");
            FILE *fp = fopen(DPE_TUNING_FILE_PATH, "w+");
            if(fp) {
                fwrite(tuningBuffer, sizeof(DVEConfig), 1, fp);
                fflush(fp);
                fclose(fp);
            } else {
                MY_LOGW("Cannot create DPE tuning file %s", DPE_TUNING_FILE_PATH);
            }
            MY_LOGD("Dump DPE parameters...done");
        }
    }

    return true;
}

bool
StereoTuningProvider::getWMFTuningInfo(WMFEFILTERSIZE &size, void *tbliBuffer)
{
    size = WMFE_FILTER_SIZE_7x7;

    if(NULL == tbliBuffer) {
        return false;
    }

    const short DEFAULT_TBLI[] = {
        1000,   920,    846,    778,    716,    659,    606,    558,    //  8
        513,    472,    434,    399,    367,    338,    311,    286,    // 16
        263,    242,    223,    205,    188,    173,    159,    147,    // 24
        135,    124,    114,    105,    96,     89,     82,     75,     // 32
        69,     63,     58,     54,     49,     45,     42,     38,     // 40
        35,     32,     30,     27,     25,     23,     21,     19,     // 48
        18,     16,     15,     14,     13,     12,     11,     10,     // 56
        9,      8,      7,      7,      6,      6,      5,      5,      // 64
        4,      4,      4,      3,      3,      3,      2,      2,      // 72
        2,      2,      2,      1,      1,      1,      1,      1,      // 80
        1,      1,      1,      1,      1,      1,      1,      1,      // 88
        1,      1,      1,      1,      1,      1,      1,      1,      // 96
        1,      1,      1,      1,      1,      1,      1,      1,      //104
        1,      1,      1,      1,      1,      1,      1,      1,      //112
        1,      1,      1,      1,      1,      1,      1,      1,      //120
        1,      1,      1,      1,      1,      1,      1,      1       //128
    };

    static bool useDefault = true;

    int propVal = checkStereoProperty(PROPERTY_WMF_TUNING);
    if(propVal <= 0) {
        //Disable reading file

        if(useDefault) {
            ::memcpy(tbliBuffer, DEFAULT_TBLI, sizeof(DEFAULT_TBLI));
        }
    } else {
        useDefault = false;
        //if DPE_TUNING_FILE_PATH exists
        struct stat st;
        if(0 == stat(WMF_TUNING_FILE_PATH, &st)) {
            //  read the file
            MY_LOGD("Read WMF parameters...");
            DVEConfig config;
            FILE *fp = fopen(WMF_TUNING_FILE_PATH, "r");
            if(fp) {
                size_t size = fread(&config, sizeof(DVEConfig), 1, fp);
                if(size == sizeof(DVEConfig)) {
                    ::memcpy(tbliBuffer, &config, sizeof(config));
                } else {
                    MY_LOGW("Incorrect WMF parameters size");
                }

                fclose(fp);
                MY_LOGD("Read WMF parameters...done");
            } else {
                MY_LOGW("Cannot read WMF parameters");
            }
        } else {
            //  dump config to file
            MY_LOGD("Dump WMF parameters...");
            FILE *fp = fopen(WMF_TUNING_FILE_PATH, "w+");
            if(fp) {
                fwrite(tbliBuffer, sizeof(DVEConfig), 1, fp);
                fflush(fp);
                fclose(fp);
            } else {
                MY_LOGW("Cannot create WMF tuning file %s", WMF_TUNING_FILE_PATH);
            }
            MY_LOGD("Dump WMF parameters...done");
        }
    }

    return true;
}

ENUM_CLEAR_REGION
StereoTuningProvider::getGFTuningInfo()
{
    return E_CLEAR_REGION_MEDIUM;
}

bool
StereoTuningProvider::getBokehTuningInfo(void *tuningBuffer, ENUM_BOKEH_STRENGTH eBokehStrength)
{
//    dip_x_reg_t *tuning = (dip_x_reg_t *)tuningBuffer;
//
//    tuning->DIP_X_BOK_CON.BOK_MODE;
//    tuning->DIP_X_BOK_CON.BOK_AP_MODE;
//    tuning->DIP_X_BOK_CON.BOK_FGBG_MODE;
//    tuning->DIP_X_BOK_CON.BOK_FGBG_WT;
//
//    tuning->DIP_X_BOK_TUN.BOK_STR_WT;
//    tuning->DIP_X_BOK_TUN.BOK_WT_GAIN;
//    tuning->DIP_X_BOK_TUN.BOK_INTENSITY;
//    tuning->DIP_X_BOK_TUN.BOK_DOF_M;
//
//    tuning->DIP_X_BOK_OFF.BOK_XOFF;
//    tuning->DIP_X_BOK_OFF.BOK_YOFF;

//    tuning->DIP_X_BOK_RSV1.BOK_RSV1;

    return true;
}