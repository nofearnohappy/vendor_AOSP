/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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
#define TAG                  "[LENS] "
#define LOG_TAG "flash_mgr.cpp"

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>

extern "C" {
#include "common.h"
#include "miniui.h"
#include "ftm.h"
}

#include <sys/ioctl.h>
#include <acdk/MdkIF.h>
#include <cutils/properties.h>


//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
#define DEBUG_FTM_CAMERA
#ifdef DEBUG_FTM_CAMERA
#define FTM_CAMERA_DBG(fmt, arg...) LOGD(fmt, ##arg)
#define FTM_CAMERA_ERR(fmt, arg...)  LOGE(fmt, ##arg)
#else
#define FTM_CAMERA_DBG(a,...)
#define FTM_CAMERA_ERR(a,...)
#endif
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
enum
{
    ITEM_NULL,
    ITEM_PASS,
    ITEM_FAIL,
    ITEM_LENS_TEST,
};
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
struct camera
{
    char  info[1024];
    int isExit;
    int cmd;
    int isTestDone;
    text_t    title;
    text_t    text;
    struct ftm_module *mod;
    struct textview tv;
    struct itemview *iv;
};
#define mod_to_camera(p)     (struct camera*)((char*)(p) + sizeof(struct ftm_module))
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
static item_t menu_items[5];

#define CAM_MAIN    0x1
#define CAM_SUB    0x2
#define CAM_MAIN2    0x8

static MUINT32 srcDev = 0; // 0x1:main , 0x2:sub , 0x8:main2

//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

typedef struct {
    unsigned char uMotorName[32];
} mcuMotorName;



#define mcuIOC_T_MOVETO       _IOW('A',1,unsigned int)
#define mcuIOC_S_SETDRVNAME   _IOW('A',10,mcuMotorName)

#ifdef LENS_SEARCH_SUPPORT
struct LENS_LIST {
    unsigned int Enable;
    char LensDrvFile[32];
};

#define LENS_LIST_MAX    5

struct LENS_LIST len_drv_list[LENS_LIST_MAX] = {
    {1, "/dev/DW9714AF"},
    {1, "/dev/LC898122AF"},
    {1, "/dev/DW9714AF"},
    {1, "/dev/AD5820AF"},
};
#endif

static void *lens_test_thread(void *priv)
{
    FTM_CAMERA_DBG("lens_test_thread %d\n",__LINE__);
    struct camera *stb = (struct camera *)priv;
    while(!stb->isExit)
    {
        //FTM_CAMERA_DBG("lens_test_thread %d\n",__LINE__);
        if(stb->cmd==ITEM_LENS_TEST)
        {
            //FTM_CAMERA_DBG("lens_test_thread %d\n",__LINE__);
            #ifdef LENS_SEARCH_SUPPORT

            int i;
            for( i = 0; i < LENS_LIST_MAX; i++ )
            {
                int temp = open(len_drv_list[i].LensDrvFile, O_RDWR);
                FTM_CAMERA_DBG("[LENS_TEST]Device : %s", len_drv_list[i].LensDrvFile);
                if( temp < 0 )
                    FTM_CAMERA_DBG("[LENS_TEST]Device error opening : %s", strerror(errno));

                if( temp > 0 )
                {
                    int err = ioctl(temp, mcuIOC_T_MOVETO, (unsigned long)0);
                    close(temp);
                    FTM_CAMERA_DBG("[LENS_TEST]Device : %s(%d)", len_drv_list[i].LensDrvFile, err);
                    if( err == 0 )
                    {
                        FTM_CAMERA_DBG("[LENS_TEST]Find Device : %s(%d)", len_drv_list[i].LensDrvFile, err);
                        break;
                    }
                }
            }

            FTM_CAMERA_DBG("[LENS_TEST]Find Device : %s(%d)", len_drv_list[i].LensDrvFile, i);
            int m_fd_Lens = open(len_drv_list[i].LensDrvFile,O_RDWR);

            #else

            int m_fd_Lens = 0;
            int err = 0;

            mcuMotorName motorName;
            char driverName[32];

            switch(srcDev)
            {
                case CAM_MAIN:
                    m_fd_Lens = open("/dev/MAINAF",O_RDWR);
                    #if defined(FEATURE_FTM_MAIN_LENS_LC898212XDAF)
                    sprintf(driverName, "%s", "LC898212XDAF");
                    #elif defined(FEATURE_FTM_MAIN_LENS_BU6429AF)
                    sprintf(driverName, "%s", "BU6429AF");
                    #else
                    sprintf(driverName, "%s", "BU6429AF");
                    #endif
                    memcpy(motorName.uMotorName, (unsigned char*)driverName, 32);
                    ioctl(m_fd_Lens, mcuIOC_S_SETDRVNAME, &motorName);
                    break;
                case CAM_MAIN2:
                    m_fd_Lens = open("/dev/MAIN2AF",O_RDWR);
                    break;
                case CAM_SUB:
                    m_fd_Lens = open("/dev/SUBAF",O_RDWR);
                    #if defined(FEATURE_FTM_SUB_LENS_DW9714AF)
                    sprintf(driverName, "%s", "DW9714AF");
                    #elif defined(FEATURE_FTM_MAIN_LENS_BU6424AF)
                    sprintf(driverName, "%s", "BU6424AF");
                    #else
                    sprintf(driverName, "%s", "BU6424AF");
                    #endif
                    memcpy(motorName.uMotorName, (unsigned char*)driverName, 32);
                    ioctl(m_fd_Lens, mcuIOC_S_SETDRVNAME, &motorName);
                    break;
                default:
                    FTM_CAMERA_DBG("Error : srcDev");
                    break;
            }

            #endif

            FTM_CAMERA_DBG("[LENS_TEST]Start");
            if (m_fd_Lens < 0)
            {
                FTM_CAMERA_DBG("[LENS_TEST]Device error opening : %s", strerror(errno));
            }

            stb->isTestDone = 1;

            while(!stb->isExit && stb->cmd==ITEM_LENS_TEST)
            {
                if(stb->isExit) break;

                if( m_fd_Lens > 0 )
                    err = ioctl(m_fd_Lens,mcuIOC_T_MOVETO,(unsigned long)1023);

                if (err < 0) {
                    FTM_CAMERA_DBG("[LENS_TEST][moveMCU] ioctl - LC898212AFIOC_T_MOVETO, error %s",  strerror(errno));
                }

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;

                if( m_fd_Lens > 0 )
                    err = ioctl(m_fd_Lens,mcuIOC_T_MOVETO,(unsigned long)0);

                if (err < 0) {
                    FTM_CAMERA_DBG("[LENS_TEST][moveMCU] ioctl - LC898212AFIOC_T_MOVETO, error %s",  strerror(errno));
                }

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
                usleep(50000);

                if(stb->isExit) break;
            }

            if( m_fd_Lens > 0 )
            {
                close(m_fd_Lens);
            }
        }

    }
    FTM_CAMERA_DBG("lens_test_thread %d End\n",__LINE__);
    return NULL;
}

static bool bSendDataToACDK(eACDK_COMMAND	FeatureID,
                                     MUINT8*  pInAddr,
                                     MUINT32  nInBufferSize,
                                     MUINT8*  pOutAddr,
                                     MUINT32  nOutBufferSize,
                                     MUINT32* pRealOutByeCnt)
{
    ACDK_FEATURE_INFO_STRUCT rAcdkFeatureInfo;

    rAcdkFeatureInfo.puParaIn     = pInAddr;
    rAcdkFeatureInfo.u4ParaInLen  = nInBufferSize;
    rAcdkFeatureInfo.puParaOut    = pOutAddr;
    rAcdkFeatureInfo.u4ParaOutLen = nOutBufferSize;
    rAcdkFeatureInfo.pu4RealParaOutLen = pRealOutByeCnt;

    return (Mdk_IOControl(FeatureID, &rAcdkFeatureInfo));
}

static MINT32 camera_preview_test()
{
    FTM_CAMERA_DBG("%s ,Enter\n", __FUNCTION__);

    ACDK_PREVIEW_STRUCT rACDKPrvConfig;
    rACDKPrvConfig.fpPrvCB = NULL;
    rACDKPrvConfig.u4PrvW = 320;
    rACDKPrvConfig.u4PrvH = 240;
    rACDKPrvConfig.u16HDRModeEn = 0;

    rACDKPrvConfig.u16PreviewTestPatEn = 1;
    rACDKPrvConfig.u4OperaType = 0;

    MUINT32 u4RetLen = 0;
    rACDKPrvConfig.eOperaMode    = ACDK_OPT_FACTORY_MODE;

    bool bRet = bSendDataToACDK(ACDK_CMD_PREVIEW_START,
                                (MUINT8 *)&rACDKPrvConfig,
                                sizeof(ACDK_PREVIEW_STRUCT),
                                NULL,
                                0,
                                &u4RetLen);

    if (!bRet)
    {
        FTM_CAMERA_ERR("ACDK_PREVIEW_STRUCT Fail\n");
        return 1;
    }

    FTM_CAMERA_DBG("%s ,Exit\n", __FUNCTION__);

    return 0;
}

static int camera_preview_stop(void)
{
    FTM_CAMERA_DBG("[camera_preview_stop] Stop Camera Preview\n");

    unsigned int u4RetLen = 0;
    bool bRet = bSendDataToACDK(ACDK_CMD_PREVIEW_STOP, NULL, 0, NULL, 0, &u4RetLen);

    if (!bRet)
    {
        return -1;
    }

    FTM_CAMERA_DBG("[camera_preview_stop] X\n");
    return 0;
}

static int acdkIFInit()
{
    FTM_CAMERA_DBG("%s : Open ACDK\n",__FUNCTION__);

    //====== Local Variable ======

    ACDK_FEATURE_INFO_STRUCT rAcdkFeatureInfo;
    bool bRet;
    unsigned int u4RetLen;

    //====== Create ACDK Object ======
    if (Mdk_Open() == MFALSE)
    {
        FTM_CAMERA_ERR("Mdk_Open() Fail \n");
        return -1;
    }

    //====== Select Camera Sensor ======
    MUINT8 srcDev = CAM_SUB;
    rAcdkFeatureInfo.puParaIn = (MUINT8 *)&srcDev;
    rAcdkFeatureInfo.u4ParaInLen = sizeof(int);
    rAcdkFeatureInfo.puParaOut = NULL;
    rAcdkFeatureInfo.u4ParaOutLen = 0;
    rAcdkFeatureInfo.pu4RealParaOutLen = &u4RetLen;

    FTM_CAMERA_DBG("%s : srcDev:%d\n",__FUNCTION__,srcDev);
    bRet = Mdk_IOControl(ACDK_CMD_SET_SRC_DEV, &rAcdkFeatureInfo);
    if (!bRet)
    {
        FTM_CAMERA_DBG("ACDK_FEATURE_SET_SRC_DEV Fail: %d\n",srcDev);
        return -1;
    }

    //====== Initialize ACDK ======

    FTM_CAMERA_DBG("%s : Init ACDK\n",__FUNCTION__);
    if(Mdk_Init() == MFALSE)
    {
        return -1;
    }

    //====== Preview Initialization =====
    //camera_preview_test();

    return 0;
}

//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
int lens_entry(struct ftm_param *param, void *priv)
{
    FTM_CAMERA_DBG("lens_entry %d\n",__LINE__);
    int chosen;
    bool exit = false;
    struct camera *cam = (struct camera *)priv;
    struct textview *tv = NULL ;
    struct itemview *iv = NULL ;
    static int isTestDone = 0;

    init_text(&cam ->title, param->name, COLOR_YELLOW);
    init_text(&cam ->text, &cam->info[0], COLOR_YELLOW);
    if (!cam->iv)
    {
        iv = ui_new_itemview();
        if (!iv)
        {
            FTM_CAMERA_DBG("No memory");
            return -1;
        }
        cam->iv = iv;
    }

    iv = cam->iv;
    iv->set_title(iv, &cam->title);

    //menu_items[0].id = ITEM_LENS_TEST;
    //menu_items[0].name = uistr_strobe_test;
    //menu_items[1].id = -1;
    //menu_items[1].name = NULL;


    #ifndef FEATURE_FTM_TOUCH_MODE
    menu_items[0].id = ITEM_PASS;
    menu_items[0].name = uistr_pass;
    menu_items[1].id = ITEM_FAIL;
    menu_items[1].name = uistr_fail;
    menu_items[2].id = -1;
    menu_items[2].name = NULL;
    #else
    menu_items[0].id = -1;
    menu_items[0].name = NULL;
    #endif

    iv->set_items(iv, menu_items, 0);
    iv->set_text(iv, &cam->text);

    #ifdef FEATURE_FTM_TOUCH_MODE
    text_t lbtn ;
    text_t cbtn ;
    text_t rbtn ;
    init_text(&lbtn, uistr_key_fail, COLOR_YELLOW);
    init_text(&cbtn, uistr_key_back, COLOR_YELLOW);
    init_text(&rbtn, uistr_key_pass, COLOR_YELLOW);
    iv->set_btn(iv, &lbtn, &cbtn, &rbtn);
    #endif

    FTM_CAMERA_DBG("%s : srcDev:%d\n",__FUNCTION__,srcDev);

    if( acdkIFInit() != 0 )
    {
        FTM_CAMERA_DBG("acdkIFInit() Fail\n");
    }

    pthread_t lensTestTh;
    pthread_create(&lensTestTh, NULL, lens_test_thread, cam);
    cam->cmd = ITEM_NULL;
    cam->isExit = 0;
    cam->isTestDone = isTestDone;

    if(isTestDone == 0)
    {
        cam->cmd = ITEM_LENS_TEST;
        isTestDone=1;
    }

    while(1)
    {
        chosen = iv->run(iv, &exit);
        switch (chosen)
        {
            #ifndef FEATURE_FTM_TOUCH_MODE
            case ITEM_PASS:
                cam->cmd = ITEM_NULL;
                if(cam->isTestDone)
                {
                    cam->mod->test_result = FTM_TEST_PASS;
                    exit = true;
                }
                else
                {
                    memset(cam->info, 0, 1024);
                    sprintf(cam->info, "Not test done !! \n");
                    iv->set_text(iv, &cam->text);
                    iv->redraw(iv);
                }
                break;
            case ITEM_FAIL:
                cam->cmd = ITEM_NULL;
                if(cam->isTestDone)
                {
                    cam->mod->test_result = FTM_TEST_FAIL;
                    exit = true;
                }
                else
                {
                    memset(cam->info, 0, 1024);
                    sprintf(cam->info, "Not test done !! \n");
                    iv->set_text(iv, &cam->text);
                    iv->redraw(iv);
                }
                break;
            #endif

            #ifdef FEATURE_FTM_TOUCH_MODE
            case L_BTN_DOWN:
                cam->cmd = ITEM_NULL;
                if(cam->isTestDone)
                {
                    cam->mod->test_result = FTM_TEST_FAIL;
                    exit = true;
                }
                else
                {
                    memset(cam->info, 0, 1024);
                    sprintf(cam->info, "Not test done !! \n");
                    iv->set_text(iv, &cam->text);
                    iv->redraw(iv);
                }
                break;
            case C_BTN_DOWN:
                cam->cmd = ITEM_NULL;
                exit = true;
                break;
            case R_BTN_DOWN:
                cam->cmd = ITEM_NULL;
                if(cam->isTestDone)
                {
                    cam->mod->test_result = FTM_TEST_PASS;
                    exit = true;
                }
                else
                {
                    memset(cam->info, 0, 1024);
                    sprintf(cam->info, "Not test done !! \n");
                    iv->set_text(iv, &cam->text);
                    iv->redraw(iv);
                }
                break;
                #endif
        }
        if(exit)
        {
            isTestDone = cam->isTestDone;
            cam->cmd = ITEM_NULL;
            cam->isExit = true;
            isTestDone = 0;
            //camera_preview_stop();
            Mdk_DeInit();
            Mdk_Close();
            break;
        }
        usleep(30000);
    }


FTM_CAMERA_DBG("lens_entry %d\n",__LINE__);
    pthread_join(lensTestTh, NULL);
FTM_CAMERA_DBG("lens_entry %d\n",__LINE__);
    return 0;
}

/*******************************************************************************
*
********************************************************************************/
int lens_main_entry(struct ftm_param *param, void *priv)
{
    srcDev = CAM_MAIN; //main sensor
    return lens_entry(param,priv);
}

/*******************************************************************************
*
********************************************************************************/
int lens_main2_entry(struct ftm_param *param, void *priv)
{
    srcDev = CAM_MAIN2; //main2 sensor
    return lens_entry(param,priv);
}


/*******************************************************************************
*
********************************************************************************/
int lens_sub_entry(struct ftm_param *param, void *priv)
{
    srcDev = CAM_SUB; //sub sensor
    return lens_entry(param,priv);
}
//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
extern "C" int lens_main_init(void)
{
    FTM_CAMERA_DBG("lens_init %d\n",__LINE__);
    int ret = 0;
    struct ftm_module *mod;
    struct camera *cam;

    mod = ftm_alloc(ITEM_MAIN_LENS, sizeof(struct camera));
    cam  = mod_to_camera(mod);
    memset(cam, 0x0, sizeof(struct camera));
    /*NOTE: the assignment MUST be done, or exception happens when tester press Test Pass/Test Fail*/
    cam->mod = mod;
    if (!mod)
        return -ENOMEM;

    ret = ftm_register(mod, lens_main_entry, (void*)cam);
    return ret;
}

extern "C" int lens_main2_init(void)
{
    FTM_CAMERA_DBG("lens_init %d\n",__LINE__);
    int ret = 0;
    struct ftm_module *mod;
    struct camera *cam;

    mod = ftm_alloc(ITEM_MAIN2_LENS, sizeof(struct camera));
    cam  = mod_to_camera(mod);
    memset(cam, 0x0, sizeof(struct camera));
    /*NOTE: the assignment MUST be done, or exception happens when tester press Test Pass/Test Fail*/
    cam->mod = mod;
    if (!mod)
        return -ENOMEM;

    ret = ftm_register(mod, lens_main2_entry, (void*)cam);
    return ret;
}

extern "C" int lens_sub_init(void)
{
    FTM_CAMERA_DBG("lens_init %d\n",__LINE__);
    int ret = 0;
    struct ftm_module *mod;
    struct camera *cam;

    mod = ftm_alloc(ITEM_SUB_LENS, sizeof(struct camera));
    cam  = mod_to_camera(mod);
    memset(cam, 0x0, sizeof(struct camera));
    /*NOTE: the assignment MUST be done, or exception happens when tester press Test Pass/Test Fail*/
    cam->mod = mod;
    if (!mod)
        return -ENOMEM;

    ret = ftm_register(mod, lens_sub_entry, (void*)cam);
    return ret;
}
