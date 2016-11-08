/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
 *
 * Filename:
 * ---------
 *   MtkVideoSecureAL.cpp
 *
 * Project:
 * --------
 *   MT65xx/MT8135
 *
 * Description:
 * ------------
 *   MTK secure video abstraction layer
 *   This file includes two parts:
 *   1) MtkVideoSecureMemoryAllocator
 *   2) MtkVideoSecureAL
 *
 * Author:
 * -------
 *   Morris Yang (mtk03147)
 *
 ****************************************************************************/


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <tz_cross/ta_mem.h>
#include <tz_cross/ta_vdec.h>
#ifdef MTK_SEC_VIDEO_PATH_SUPPORT
#include <tz_cross/ta_venc.h>
#endif
#include "MtkVideoSecureAL.h"
#include <cutils/log.h>
#undef LOG_TAG
#define LOG_TAG "MtkVideoSecureAL"
//#define LOG_ENALABLE
#ifdef LOG_ENALABLE
#define ALOGI  ALOGE
#define ALOGD  ALOGE
#else
#define ALOGI(...)
#endif
/* Begin added by mtk09845 UT_TEST_DEV  */
#if 0
#undef ALOGD
#undef ALOGE
#define ALOGD printf
#define ALOGE printf
#endif

#define UT_H265_DEBUG_
#ifdef UT_H265_DEBUG_
#define printfInfo printf
#else
#define printfInfo
#endif
/* End added by mtk09845 UT_TEST_DEV  */

#ifdef MMPROFILE_VIDEO_SEC_AL
#include <linux/mmprofile.h>
MMP_Event MMP_VDEC_SEC_AL = 0;
#endif

#define UNREGISTER_SHARE_MEM 1

UREE_SESSION_HANDLE gMemSession = 0;
UREE_SESSION_HANDLE gVdecSession = 0;

#ifdef MTK_SEC_VIDEO_PATH_SUPPORT
UREE_SESSION_HANDLE gVencSession = 0;
#endif

MTK_SECURE_AL_RESULT MtkVideoSecureMemAllocatorInit() {
    TZ_RESULT ret;
    ALOGE ("+MtkVideoSecureMemAllocatorInit");

    if (gMemSession != 0) {
        ALOGE ("Mem Session already exists, line:%d", __LINE__);
        return MTK_SECURE_AL_SUCCESS;
    }

    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &gMemSession);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("Mem CreateSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ALOGE ("-MtkVideoSecureMemAllocatorInit gMemSession (%d)", gMemSession);
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVideoSecureMemAllocatorDeinit() {
    TZ_RESULT ret;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ret = UREE_CloseSession(gMemSession);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("CloseSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    gMemSession = 0;
    return MTK_SECURE_AL_SUCCESS;
}

unsigned long MtkVideoAllocateSecureBuffer(int size, int align) {
    TZ_RESULT ret;
    UREE_SECUREMEM_HANDLE memHandle = 0;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return 0;
    }

    // Alloc secure memory
    ret = UREE_AllocSecuremem (gMemSession, &memHandle, align, size);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_AllocSecuremem Error: %d, line:%d", ret, __LINE__);
    }

    return memHandle;
}


MTK_SECURE_AL_RESULT MtkVideoFreeSecureBuffer(unsigned long memHandle) {
    TZ_RESULT ret;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists");
        return MTK_SECURE_AL_FAIL;
    }

    // Free/Unreference secure memory
    ret = UREE_UnreferenceSecuremem (gMemSession, memHandle);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_UnReferenceSecureMem Error: %d, line:%d", ret, __LINE__);
    }

    return MTK_SECURE_AL_SUCCESS;
}

/* Begin added by mtk09845 UT_TEST_DEV  */
unsigned long MtkVideoAllocateSecureBitstreamBuffer(int size, int align)
{
    ALOGD("+[ %s : %d ]\n", __FUNCTION__, __LINE__);
    TZ_RESULT ret;
    UREE_SECUREMEM_HANDLE memHandle = 0;

    if (gMemSession == 0)
    {
        ALOGE ("ERROR: No session exists, line:%d\n", __LINE__);
        return 0;
    }
    ret = UREE_AllocSecurechunkmem (gMemSession, &memHandle, align, size);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_AllocSecurechunkmem Error: %d, line:%d\n", ret, __LINE__);
    }

    return memHandle;
}

/**
 * write bitstream from normal to secure
 * param bs_sec_handle : secure handle to memory allocatec in secure world
 * param bs_va         : bit stream virtal address
 * param size          : bit stream size
 **/
unsigned long MtkVdecFillSecureBuffer(uint32_t bs_sec_handle, void *bs_va, uint32_t size, uint32_t bdirection)
{
    ALOGD("+[ %s : #%d ] \n"
          " \t param1 = 0x%x \t param2 = 0x%x \t param3 = 0x%x \n",
          __FUNCTION__, __LINE__,bs_sec_handle, bs_va, size);
    unsigned char *src = bs_va;
    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SHAREDMEM_HANDLE pBitstream_va_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pFillStuct_share_handle = 0;
    if (gVdecSession != 0)
    {
        ALOGE (" \t Vdec Session already exists, line:%d\n", __LINE__);
    }
    else
    {
        ret = UREE_CreateSession(TZ_TA_VDEC_UUID, &gVdecSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE (" \t Vdec CreateSession Error: %d, line:%d\n", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
    pBitstream_va_share_handle = MtkVideoRegisterSharedMemory(bs_va, size);
    //ALOGD(" \t [ %s ] bit stream share handle 0x%x \t size 0x%x\n", __FUNCTION__, pBitstream_va_share_handle, size);
    VdecFillSecMemStruct *pFillStruct = (VdecFillSecMemStruct*)malloc(sizeof(VdecFillSecMemStruct));
    pFillStruct->bs_secure_handle = bs_sec_handle;
    pFillStruct->bs_share_handle =  pBitstream_va_share_handle;
    pFillStruct->bs_size = size;
    pFillStruct->direction = bdirection;
    pFillStuct_share_handle = MtkVideoRegisterSharedMemory(pFillStruct, sizeof(VdecFillSecMemStruct));

    param[0].memref.handle = pFillStuct_share_handle;
    param[0].memref.size   = sizeof(VdecFillSecMemStruct);
    param[0].memref.offset = 0;
    ret = UREE_TeeServiceCall(gVdecSession, TZCMD_VDEC_FILL_SECMEM, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (pBitstream_va_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pBitstream_va_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
    if (pFillStuct_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pFillStuct_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
    if(pFillStruct)
        free(pFillStruct);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE (" TZCMD_VDEC_FILL_SECMEM failed, ret: 0x%X, line:%d\n", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
    ALOGD("-[ %s : #%d ] \n", __FUNCTION__, __LINE__);
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVdecH265SecInit(VdecH265SecMemInfoStruct* pInitStruct, unsigned int memHandle,
                                       unsigned int* Vdec_H265_InstanceTemp_share_handle,
                                       unsigned int* H265_DEC_PRM_DataInst_share_handle,
                                       unsigned int* H265_Drv_data_share_handle,
                                       unsigned int* DecStruct2_share_handle,
                                       unsigned int* pH265_Sec_session)
{
    ALOGD("+[ %s : %d ] \n"
          " \t param1 = 0x%x \t param2 = 0x%x \t param3 = 0x%x "
          " param4 = 0x%x \t param5 = 0x%x \t param6 = 0x%x \n",
          __FUNCTION__, __LINE__,
          pInitStruct, memHandle, Vdec_H265_InstanceTemp_share_handle,
          H265_DEC_PRM_DataInst_share_handle, H265_Drv_data_share_handle,DecStruct2_share_handle);

#ifdef MMPROFILE_VIDEO_SEC_AL
    MMP_Event  MMP_Player = MMProfileFindEvent(MMP_RootEvent, "Playback");
    if (MMP_Player != 0) {
        MMP_VDEC_SEC_AL = MMProfileRegisterEvent(MMP_Player, "Secure video decoder");
        MMProfileEnableEvent(MMP_VDEC_SEC_AL, 1);
    }
#endif

    TZ_RESULT ret;
    MTEEC_PARAM param[4];

    UREE_SHAREDMEM_HANDLE pDecStruct2_share_handle   = 0;
    //UREE_SHAREDMEM_HANDLE pBitstream_va_share_handle = 0; // for UT only
    UREE_SHAREDMEM_HANDLE pVdec_H265_Drv_data_share_handle     = 0;
    UREE_SHAREDMEM_HANDLE pH265_DEC_PRM_DataInst_share_handle  = 0;
    UREE_SHAREDMEM_HANDLE pVdec_H265_InstanceTemp_share_handle = 0;
#if 0
    if (gVdecSession != 0)
    {
        ALOGE (" Vdec Session already exists, line:%d\n", __LINE__);
    }
    else
#endif
    {
        ret = UREE_CreateSession(TZ_TA_VDEC_UUID, (UREE_SESSION_HANDLE*)pH265_Sec_session);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE (" Vdec CreateSession Error: %d, line:%d\n", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
    pVdec_H265_Drv_data_share_handle        = MtkVideoRegisterSharedMemory(pInitStruct[0].mem, pInitStruct[0].size);
    pVdec_H265_InstanceTemp_share_handle    = MtkVideoRegisterSharedMemory(pInitStruct[1].mem, pInitStruct[1].size);
    pH265_DEC_PRM_DataInst_share_handle     = MtkVideoRegisterSharedMemory(pInitStruct[2].mem, pInitStruct[2].size);
    pDecStruct2_share_handle                = MtkVideoRegisterSharedMemory(pInitStruct[4].mem, pInitStruct[4].size);

    *Vdec_H265_InstanceTemp_share_handle = pVdec_H265_InstanceTemp_share_handle;
    *H265_DEC_PRM_DataInst_share_handle = pH265_DEC_PRM_DataInst_share_handle;
    *H265_Drv_data_share_handle = pVdec_H265_Drv_data_share_handle;
    *DecStruct2_share_handle = pDecStruct2_share_handle;

    VdecH265SecInitStruct* ph265_sec_init_struct = (VdecH265SecInitStruct*)malloc(sizeof(VdecH265SecInitStruct));
    ph265_sec_init_struct->vdec_h265_drv_data_share_handle = pVdec_H265_Drv_data_share_handle;
    ph265_sec_init_struct->vdec_h265_instanceTemp_share_handle = pVdec_H265_InstanceTemp_share_handle;
    ph265_sec_init_struct->h265_DEC_PRM_DataInst_share_handle = pH265_DEC_PRM_DataInst_share_handle;
    ph265_sec_init_struct->bitstream_secure_handle = memHandle;
    ph265_sec_init_struct->bitstream_length = pInitStruct[3].size;

    ALOGD(" \t pVdec_H265_Drv_data_share_handle     = 0x%X \n", pVdec_H265_Drv_data_share_handle);
    ALOGD(" \t pVdec_H265_InstanceTemp_share_handle = 0x%X \n", pVdec_H265_InstanceTemp_share_handle);
    ALOGD(" \t pH265_DEC_PRM_DataInst_share_handle  = 0x%X \n", pH265_DEC_PRM_DataInst_share_handle);
    ALOGD(" \t bitstream_secure_handle              = 0x%X \n", memHandle);

    UREE_SHAREDMEM_HANDLE pVdecH265SecInit_share_handle =
                        MtkVideoRegisterSharedMemory(ph265_sec_init_struct, sizeof(VdecH265SecInitStruct));
    #if 0
    param[0].mem.buffer =  pVdecH265SecInit_share_handle;
    param[0].mem.size   =  sizeof(VdecH265SecInitStruct);
    #else
    param[0].memref.handle = (uint32_t)pVdecH265SecInit_share_handle;
    param[0].memref.size = sizeof(VdecH265SecInitStruct);
    param[0].memref.offset = 0;
    #endif

    ret = UREE_TeeServiceCall((UREE_SESSION_HANDLE)*pH265_Sec_session, TZCMD_VDEC_HEVC_INIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE (" UREE_TeeServiceCall failed, ret: 0x%x, line:%d \n", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

#if UNREGISTER_SHARE_MEM
    if (pVdecH265SecInit_share_handle != 0)
    {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVdecH265SecInit_share_handle))
        {
            ALOGE (" MtkVideoUnregisterSharedMemory failed, line:%d\n", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#if 0
    if (pBitstream_va_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pBitstream_va_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#endif
#endif

    ALOGD ("-[ %s #%d ]\n", __FUNCTION__, __LINE__);
    return MTK_SECURE_AL_SUCCESS;
}
/* End added by mtk09845 UT_TEST_DEV  */


unsigned long MtkVideoAllocateSecureFrameBuffer(int size, int align)
{
    //
    TZ_RESULT ret;
    UREE_SECUREMEM_HANDLE memHandle = 0;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return 0;
    }

    // Alloc secure memory
    ret = UREE_AllocSecurechunkmem (gMemSession, &memHandle, align, size);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_AllocSecurechunkmem Error: %d, line:%d", ret, __LINE__);
    }

    return memHandle;
}

MTK_SECURE_AL_RESULT MtkVideoFreeSecureFrameBuffer(unsigned long memHandle) {
    TZ_RESULT ret;
    ALOGE ("+MtkVideoFreeSecureFrameBuffer (0x%08X)", memHandle);

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists");
        return MTK_SECURE_AL_FAIL;
    }

    // Free/Unreference secure memory
    ret = UREE_UnreferenceSecurechunkmem (gMemSession, memHandle);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_UnreferenceSecurechunkmem Error: %d, line:%d", ret, __LINE__);
    }

    ALOGE ("-MtkVideoFreeSecureFrameBuffer");
    return MTK_SECURE_AL_SUCCESS;
}

unsigned long MtkVideoRegisterSharedMemory(void* buffer, int size) {
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE sharedHandle = 0;
    UREE_SHAREDMEM_PARAM  sharedParam;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists");
        return 0;
    }

    // Register shared memory
    sharedParam.buffer = buffer;
    sharedParam.size = size;
    ret = UREE_RegisterSharedmem (gMemSession, &sharedHandle, &sharedParam);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_RegisterSharedmem Error: %d, line:%d, gMemSession(%d)", ret, __LINE__, gMemSession);
    }
    else
    {
       ALOGI("UREE_RegisterSharedmem: %d, line:%d, gMemSession(%d)", ret, __LINE__, gMemSession);
    }
    return sharedHandle;
}


MTK_SECURE_AL_RESULT MtkVideoUnregisterSharedMemory(unsigned long sharedHandle)
{
    TZ_RESULT ret;

    if (gMemSession == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    // Unregister shared memory
    ret = UREE_UnregisterSharedmem (gMemSession, sharedHandle);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_UnregisterSharedmem Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    return MTK_SECURE_AL_SUCCESS;
}

/*
int test()
{
    UREE_SESSION_HANDLE session;
    TZ_RESULT ret;
    UREE_SECUREMEM_HANDLE memHandle;
    UREE_SHAREDMEM_HANDLE sharedHandle;
    TZ_SHAREDMEM_PARAM sharedParam;

    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &session);
    if (ret != TZ_RESULT_SUCCESS)
    {
        // Should provide strerror style error string in UREE.
        printf("CreateSession Error: %d\n", ret);
        return 1;
    }

    // Register shared memory
    sharedParam.buffer = malloc (512);
    sharedParam.size = 512;
    ret = UREE_RegisterSharedmem (session, &sharedHandle, &sharedParam);
    if (ret != TZ_RESULT_SUCCESS)
    {
        printf("UREE_RegisterSharedmem Error: %d\n", ret);
    }

    // Alloc secure memory
    ret = UREE_AllocSecuremem (session, &memHandle, 1024, 128);
    if (ret != TZ_RESULT_SUCCESS)
    {
        printf("UREE_AllocSecuremem Error: %d\n", ret);
    }

    // Free/Unreference secure memory
    ret = UREE_UnreferenceSecuremem (session, memHandle);
    if (ret != TZ_RESULT_SUCCESS)
    {
        printf("UREE_UnreferenceSecuremem Error: %d\n", ret);
    }

    // Unregister shared memory
    ret = UREE_UnregisterSharedmem (session, sharedHandle);
    if (ret != TZ_RESULT_SUCCESS)
    {
        printf("UREE_UnregisterSharedmem Error: %d\n", ret);
    }
    free (sharedParam.buffer);

    ret = UREE_CloseSession(session);
    if (ret != TZ_RESULT_SUCCESS)
    {
        printf("CloseSeesion Error: %d\n", ret);
    }

    return 0;
}
*/

MTK_SECURE_AL_RESULT MtkVdecH264SecInitTest(VdecFrameDumpStruct* pFrameDumpInfo) {
    ALOGD ("+MtkVdecH264SecInitTest");
#if 0
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE test_sharemem_handle = 0;
    UREE_SHAREDMEM_HANDLE test_sharemem_handle2 = 0;
    VdecH264SecInitStruct* _initStruct;
    UREE_SHAREDMEM_HANDLE pVdecH264SecInit_share_handle = 0;
    MTEEC_PARAM param[4];

    ret = UREE_CreateSession(TZ_TA_VDEC_UUID, &gVdecSession);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("Vdec CreateSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    test_sharemem_handle = MtkVideoRegisterSharedMemory(pMemInfo[0].mem, pMemInfo[0].size);
    ALOGE ("@@ test_sharemem_handle (0x%08X)", test_sharemem_handle);

    test_sharemem_handle2 = MtkVideoRegisterSharedMemory(pMemInfo[1].mem, pMemInfo[1].size);
    ALOGE ("@@ test_sharemem_handle2 (0x%08X)", test_sharemem_handle2);

#if 0  // test 1
    param[0].memref.handle = (uint32_t) test_sharemem_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = size;

    ret = UREE_TeeServiceCall(gVdecSession, TZCMD_VDEC_TEST, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#endif

#if 1  // test 2
    _initStruct = (VdecH264SecInitStruct*)malloc(sizeof(VdecH264SecInitStruct));
    _initStruct->vdec_h264_drv_data_share_handle = test_sharemem_handle;
    _initStruct->vdec_h264_instanceTemp_share_handle = test_sharemem_handle2;

    pVdecH264SecInit_share_handle = MtkVideoRegisterSharedMemory(_initStruct, sizeof(VdecH264SecInitStruct));
    ALOGE ("@@ pVdecH264SecInit_share_handle (0x%08X)", pVdecH264SecInit_share_handle);
    param[0].memref.handle = (uint32_t) pVdecH264SecInit_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH264SecInitStruct);

    ret = UREE_TeeServiceCall(gVdecSession, TZCMD_VDEC_TEST, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#endif
#else  // dump secure frame buffer
    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SHAREDMEM_HANDLE pVdecFrameDump_share_handle = 0;
    pVdecFrameDump_share_handle = MtkVideoRegisterSharedMemory(pFrameDumpInfo, sizeof(VdecFrameDumpStruct));
    ALOGE ("@@ pVdecFrameDump_share_handle (0x%08X)", pVdecFrameDump_share_handle);
    param[0].memref.handle = (uint32_t) pVdecFrameDump_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecFrameDumpStruct);

    ret = UREE_TeeServiceCall(gVdecSession, TZCMD_VDEC_TEST, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

#endif

    ALOGD ("-MtkVdecH264SecInitTest");
    return MTK_SECURE_AL_SUCCESS;
}


MTK_SECURE_AL_RESULT MtkVdecH264SecInit(VdecH264SecMemInfoStruct* pInitStruct, unsigned long memHandle,
                                                                                   unsigned long* Vdec_H264_InstanceTemp_share_handle,
                                                                                   unsigned long* H264_DEC_PRM_DataInst_share_handle,
                                                                                   unsigned long* H264_Drv_data_share_handle,
                                                                                   unsigned long* DecStruct2_share_handle,
                                                                                   unsigned int* pH264_Sec_session) {
    ALOGD ("+MtkVdecH264SecInit memHandle(0x%08X)", memHandle);

#ifdef MMPROFILE_VIDEO_SEC_AL
    MMP_Event  MMP_Player = MMProfileFindEvent(MMP_RootEvent, "Playback");
        if(MMP_Player != 0) {
            MMP_VDEC_SEC_AL = MMProfileRegisterEvent(MMP_Player, "Secure video decoder");
            MMProfileEnableEvent(MMP_VDEC_SEC_AL, 1);
        }
#endif

    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SHAREDMEM_HANDLE pVdec_H264_Drv_data_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVdec_H264_InstanceTemp_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pH264_DEC_PRM_DataInst_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pDecStruct2_share_handle = 0;

    UREE_SHAREDMEM_HANDLE pBitstream_va_share_handle = 0; // for UT only

    VdecH264SecInitStruct* ph264_sec_init_struct = (VdecH264SecInitStruct*)malloc(sizeof(VdecH264SecInitStruct));
    UREE_SHAREDMEM_HANDLE pVdecH264SecInit_share_handle = 0;

    /*if (gVdecSession != 0) {
        ALOGE ("Vdec Session already exists, line:%d", __LINE__);
    }
    else */{   // create a new VDEC session
    ret = UREE_CreateSession(TZ_TA_VDEC_UUID, (UREE_SESSION_HANDLE*)pH264_Sec_session);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("Vdec CreateSession Error: %d, line:%d", ret, __LINE__);
        free(ph264_sec_init_struct);
        return MTK_SECURE_AL_FAIL;
    }
    }

    // TODO: may remove this call later, just in case the alloator didn't init before
    if (MTK_SECURE_AL_SUCCESS != MtkVideoSecureMemAllocatorInit()) {
         ALOGE ("MtkVideoSecureMemAllocatorInit failed, line:%d", __LINE__);
         free(ph264_sec_init_struct);
        return MTK_SECURE_AL_FAIL;
    }

    // a: pVdec_H264_Drv_dataInst   (pVdec_H264_Drv_data)
    // b: pVdec_H264_InstanceTemp (pVdec_H264_Instance)
    // c: pH264_DEC_PRM_DataInst   (pH264_DEC_PRM_Data)
    // d: rSecMemHandle (bitstream)
    pVdec_H264_Drv_data_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[0].mem, pInitStruct[0].size);
    pVdec_H264_InstanceTemp_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[1].mem, pInitStruct[1].size);
    pH264_DEC_PRM_DataInst_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[2].mem, pInitStruct[2].size);
    pDecStruct2_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[4].mem, pInitStruct[4].size);

    // save the shared handle to pVdec_H264_Drv_dataInst
    *Vdec_H264_InstanceTemp_share_handle = pVdec_H264_InstanceTemp_share_handle;
    *H264_DEC_PRM_DataInst_share_handle = pH264_DEC_PRM_DataInst_share_handle;
    *H264_Drv_data_share_handle = pVdec_H264_Drv_data_share_handle;
    *DecStruct2_share_handle = pDecStruct2_share_handle;

    //ALOGE ("@@ [0x%08X][0x%08X][0x%08X]", pVdec_H264_InstanceTemp_share_handle, pH264_DEC_PRM_DataInst_share_handle, pVdec_H264_Drv_data_share_handle);

#if UT_ENABLE
     if (pInitStruct[3].mem != NULL) {
     pBitstream_va_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[3].mem, pInitStruct[3].size);
     }
     else {
        pBitstream_va_share_handle = 0;
     }
     ALOGE ("@@ pBitstream_va_share_handle (ptr: 0x%08X, size=%d)", pInitStruct[3].mem, pInitStruct[3].size);
#else
     pBitstream_va_share_handle = 0;
     ALOGE ("@@ bitstream_length =%d", pInitStruct[3].size);
#endif

    ph264_sec_init_struct->vdec_h264_drv_data_share_handle = pVdec_H264_Drv_data_share_handle;
    ph264_sec_init_struct->vdec_h264_instanceTemp_share_handle = pVdec_H264_InstanceTemp_share_handle;
    ph264_sec_init_struct->h264_DEC_PRM_DataInst_share_handle = pH264_DEC_PRM_DataInst_share_handle;
    ph264_sec_init_struct->bitstream_va_share_handle = pBitstream_va_share_handle;
    ph264_sec_init_struct->bitstream_secure_handle = memHandle;
    ph264_sec_init_struct->bitstream_length = pInitStruct[3].size;
    pVdecH264SecInit_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_init_struct, sizeof(VdecH264SecInitStruct));
    param[0].memref.handle = (uint32_t) pVdecH264SecInit_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH264SecInitStruct);

    ALOGD ("@@ pVdec_H264_Drv_data_share_handle(0x%08X)", pVdec_H264_Drv_data_share_handle);
    ALOGD ("@@ pVdec_H264_InstanceTemp_share_handle(0x%08X)", pVdec_H264_InstanceTemp_share_handle);
    ALOGD ("@@ pH264_DEC_PRM_DataInst_share_handle(0x%08X)", pH264_DEC_PRM_DataInst_share_handle);
    ALOGD ("@@ pDecStruct2_share_handle(0x%08X)", pDecStruct2_share_handle);
    ALOGD ("@@ pBitstream_va_share_handle(0x%08X)", pBitstream_va_share_handle);
    ALOGD ("@@ bitstream_secure_handle(0x%08X)", memHandle);
    ALOGD ("@@ pVdecH264SecInit_share_handle(0x%08X)", pVdecH264SecInit_share_handle);

    ret = UREE_TeeServiceCall((UREE_SESSION_HANDLE)(*pH264_Sec_session), TZCMD_VDEC_AVC_INIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);


/*
    param[0].memref.handle = (uint32_t) pVdec_H264_Drv_data_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = pInitStruct[0].size;

    param[1].memref.handle = (uint32_t) pVdec_H264_InstanceTemp_share_handle;
    param[1].memref.offset = 0;
    param[1].memref.size = pInitStruct[1].size;

    param[2].memref.handle = (uint32_t) pH264_DEC_PRM_DataInst_share_handle;
    param[2].memref.offset = 0;
    param[2].memref.size = pInitStruct[2].size;

    param[3].value.a = memHandle;

    ret = UREE_TeeServiceCall((UREE_SESSION_HANDLE)(*pH264_Sec_session), TZCMD_VDEC_AVC_INIT,
            TZ_ParamTypes4(TZPT_MEMREF_INPUT, TZPT_MEMREF_INPUT, TZPT_MEMREF_INPUT, TZPT_VALUE_INPUT), param);
*/

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret: %d, line:%d", ret, __LINE__);
        free(ph264_sec_init_struct);
        return MTK_SECURE_AL_FAIL;
    }

#if UNREGISTER_SHARE_MEM
    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVdecH264SecInit_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        free(ph264_sec_init_struct);
        return MTK_SECURE_AL_FAIL;
    }

    if (pBitstream_va_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pBitstream_va_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            free(ph264_sec_init_struct);
            return MTK_SECURE_AL_FAIL;
        }
    }
#endif

    free(ph264_sec_init_struct);
    ALOGD ("-MtkVdecH264SecInit");
    return MTK_SECURE_AL_SUCCESS;
}

#if 0
MTK_SECURE_AL_RESULT MtkVdecH265SecInit(VdecH265SecMemInfoStruct* pInitStruct, unsigned long memHandle,
                                                                                   unsigned long* Vdec_H265_InstanceTemp_share_handle,
                                                                                   unsigned long* H265_DEC_PRM_DataInst_share_handle,
                                                                                   unsigned long* H265_Drv_data_share_handle,
                                                                                   unsigned long* DecStruct2_share_handle,
                                                                                   unsigned int* pH265_Sec_session) {
    //ALOGD ("+MtkVdecH265SecInit memHandle(0x%08X)", memHandle);

#ifdef MMPROFILE_VIDEO_SEC_AL
    MMP_Event  MMP_Player = MMProfileFindEvent(MMP_RootEvent, "Playback");
    if (MMP_Player != 0) {
        MMP_VDEC_SEC_AL = MMProfileRegisterEvent(MMP_Player, "Secure video decoder");
        MMProfileEnableEvent(MMP_VDEC_SEC_AL, 1);
    }
#endif

    TZ_RESULT ret;
    MTEEC_PARAM param[4];

    UREE_SHAREDMEM_HANDLE pDecStruct2_share_handle   = 0;
    //UREE_SHAREDMEM_HANDLE pBitstream_va_share_handle = 0; // for UT only
    UREE_SHAREDMEM_HANDLE pVdec_H265_Drv_data_share_handle     = 0;
    UREE_SHAREDMEM_HANDLE pH265_DEC_PRM_DataInst_share_handle  = 0;
    UREE_SHAREDMEM_HANDLE pVdec_H265_InstanceTemp_share_handle = 0;

    VdecH265SecInitStruct* ph265_sec_init_struct = (VdecH265SecInitStruct*)malloc(sizeof(VdecH265SecInitStruct));
    UREE_SHAREDMEM_HANDLE pVdecH265SecInit_share_handle = 0;

    /*if (gVdecSession != 0) {
        ALOGE ("Vdec Session already exists, line:%d", __LINE__);
    }
    else */{
        ret = UREE_CreateSession(TZ_TA_VDEC_UUID, (UREE_SESSION_HANDLE*)pH265_Sec_session);
        if (ret != TZ_RESULT_SUCCESS) {
            ALOGE ("Vdec CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoSecureMemAllocatorInit()) {
        ALOGE ("MtkVideoSecureMemAllocatorInit failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    pVdec_H265_Drv_data_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[0].mem, pInitStruct[0].size);
    pVdec_H265_InstanceTemp_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[1].mem, pInitStruct[1].size);
    pH265_DEC_PRM_DataInst_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[2].mem, pInitStruct[2].size);
    pDecStruct2_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[4].mem, pInitStruct[4].size);

    *Vdec_H265_InstanceTemp_share_handle = pVdec_H265_InstanceTemp_share_handle;
    *H265_DEC_PRM_DataInst_share_handle = pH265_DEC_PRM_DataInst_share_handle;
    *H265_Drv_data_share_handle = pVdec_H265_Drv_data_share_handle;
    *DecStruct2_share_handle = pDecStruct2_share_handle;
    //pBitstream_va_share_handle = 0;

    ph265_sec_init_struct->vdec_h265_drv_data_share_handle = pVdec_H265_Drv_data_share_handle;
    ph265_sec_init_struct->vdec_h265_instanceTemp_share_handle = pVdec_H265_InstanceTemp_share_handle;
    ph265_sec_init_struct->h265_DEC_PRM_DataInst_share_handle = pH265_DEC_PRM_DataInst_share_handle;
    //ph265_sec_init_struct->bitstream_va_share_handle = pBitstream_va_share_handle;
    ph265_sec_init_struct->bitstream_secure_handle = memHandle;
    ph265_sec_init_struct->bitstream_length = pInitStruct[3].size;

    pVdecH265SecInit_share_handle = MtkVideoRegisterSharedMemory(ph265_sec_init_struct, sizeof(VdecH265SecInitStruct));
    param[0].memref.handle = (uint32_t)pVdecH265SecInit_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH265SecInitStruct);

    ret = UREE_TeeServiceCall((UREE_SESSION_HANDLE)*pH265_Sec_session, TZCMD_VDEC_HEVC_INIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE ("UREE_TeeServiceCall failed, ret: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

#if UNREGISTER_SHARE_MEM
    if (pVdecH265SecInit_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVdecH265SecInit_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#if 0
    if (pBitstream_va_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pBitstream_va_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#endif
#endif

    //ALOGD ("-MtkVdecH265SecInit");
    return MTK_SECURE_AL_SUCCESS;
}
#endif

MTK_SECURE_AL_RESULT MtkVdecH264SecDeinit(unsigned int H264_Sec_session, VdecH264SecDeinitStruct* pDeinitStruct) {
    ALOGD ("+MtkVdecH264SecDeinit");
    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SHAREDMEM_HANDLE pDeinitStruct_share_handle = 0;

    pDeinitStruct_share_handle = MtkVideoRegisterSharedMemory(pDeinitStruct, sizeof(VdecH264SecDeinitStruct));

    param[0].memref.handle = (uint32_t) pDeinitStruct_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH264SecDeinitStruct);

    if (H264_Sec_session == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ret = UREE_TeeServiceCall(H264_Sec_session, TZCMD_VDEC_AVC_DEINIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#if UNREGISTER_SHARE_MEM
     //ALOGE ("@@ [0x%08X][0x%08X][0x%08X]", pDeinitStruct->pVdec_H264_InstanceTemp_share_handle, pDeinitStruct->pH264_DEC_PRM_DataInst_share_handle, pDeinitStruct->pVdec_H264_Drv_dataInst_share_handle);

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pVdec_H264_InstanceTemp_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pH264_DEC_PRM_DataInst_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pVdec_H264_Drv_dataInst_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pDecStruct2_share_handle)) {
    ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#endif

#if 1
    ret = UREE_CloseSession(H264_Sec_session);
    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE ("Vdec UREE_CloseSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
    H264_Sec_session = 0;
#endif

    ALOGD ("-MtkVdecH264SecDeinit");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVdecH265SecDeinit(unsigned int H265_Sec_session, VdecH265SecDeinitStruct* pDeinitStruct) {
    //ALOGD ("+MtkVdecH265SecDeinit");

    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SHAREDMEM_HANDLE pDeinitStruct_share_handle = 0;

    pDeinitStruct_share_handle = MtkVideoRegisterSharedMemory(pDeinitStruct, sizeof(VdecH265SecDeinitStruct));

    param[0].memref.handle = (uint32_t)pDeinitStruct_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH264SecDeinitStruct);

    if (0 == H265_Sec_session)
    {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ret = UREE_TeeServiceCall(H265_Sec_session, TZCMD_VDEC_HEVC_DEINIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

#if UNREGISTER_SHARE_MEM
    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pVdec_H265_InstanceTemp_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pH265_DEC_PRM_DataInst_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pVdec_H265_Drv_dataInst_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct->pDecStruct2_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDeinitStruct_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#endif

    ret = UREE_CloseSession(H265_Sec_session);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("Vdec UREE_CloseSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    H265_Sec_session = 0;
    //ALOGD ("-MtkVdecH265SecDeinit");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVdecH264SecDecode(unsigned int H264_Sec_session, VdecH264SecMemInfoStruct* pDecStruct1, unsigned long pDecStruct2_share_handle) {
    //ALOGD ("+MtkVdecH264SecDecode");

#ifdef MMPROFILE_VIDEO_SEC_AL
    MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagStart, 0, 0,"MtkVdecH264SecDecode+");
#endif

    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    //UREE_SHAREDMEM_HANDLE pDecStruct2_share_handle = 0;
#if UT_ENABLE   // share ring buffer VA to TEE
    UREE_SHAREDMEM_HANDLE pBitstream_va_share_handle = 0;
#endif

    if (H264_Sec_session == 0) {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    //pDecStruct2_share_handle = MtkVdecRegisterSharedMemory(pDecStruct2, sizeof(VdecH264SecDecStruct));
    //ALOGD ("@@ pDecStruct2_share_handle(0x%08X)", pDecStruct2_share_handle);

#if UT_ENABLE   // share ring buffer VA to TEE
    if (pDecStruct1[0].mem != NULL) {
        pBitstream_va_share_handle = MtkVideoRegisterSharedMemory(pDecStruct1[0].mem, pDecStruct1[0].size);
        pDecStruct2->handle5 = pBitstream_va_share_handle;
    }
    else {
        pBitstream_va_share_handle = 0;
        pDecStruct2->handle5 = 0;
    }

    ALOGD ("MtkVdecH264SecDecode handle6(0x%08X)", pDecStruct2->handle6);
#endif

    param[0].memref.handle = (uint32_t) pDecStruct2_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH264SecDecStruct);


#ifdef MMPROFILE_VIDEO_SEC_AL
    //MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagStart, 0, 0,"TZCMD_VDEC_AVC_DECODE+");
#endif

    ret = UREE_TeeServiceCall(H264_Sec_session, TZCMD_VDEC_AVC_DECODE, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

#ifdef MMPROFILE_VIDEO_SEC_AL
    //MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagEnd, 0, 0,"TZCMD_VDEC_AVC_DECODE-");
#endif

#if UNREGISTER_SHARE_MEM
#if 0
    if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pDecStruct2_share_handle)) {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
#endif

#if UT_ENABLE
    if (pBitstream_va_share_handle != 0) {
        if (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pBitstream_va_share_handle)) {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#endif
#endif

#ifdef MMPROFILE_VIDEO_SEC_AL
    MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagEnd, 0, 0,"MtkVdecH264SecDecode-");
#endif


    ALOGD ("-MtkVdecH264SecDecode");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVdecH265SecDecode(unsigned int H265_Sec_session, VdecH265SecMemInfoStruct* pDecStruct1, unsigned long pDecStruct2_share_handle)
{
    #if 0
    ALOGD("+[ %s : %d ] \n"
          " \t param1 = 0x%p \t param2 = 0x%x \n",
          __FUNCTION__, __LINE__,
          pDecStruct1, pDecStruct2_share_handle);
    #endif

    #ifdef MMPROFILE_VIDEO_SEC_AL
    //MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagStart, 0, 0, "MtkVdecH265SecDecode+");
    #endif

    TZ_RESULT ret;
    MTEEC_PARAM param[4];

    if (0 == H265_Sec_session) /* ??? */
    {
        ALOGE ("ERROR: No session exists, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    param[0].memref.handle = (uint32_t) pDecStruct2_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VdecH265SecDecStruct);

    ret = UREE_TeeServiceCall(H265_Sec_session, TZCMD_VDEC_HEVC_DECODE, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("UREE_TeeServiceCall failed, ret:%d, #%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    #ifdef MMPROFILE_VIDEO_SEC_AL
    //MMProfileLogMetaStringEx(MMP_VDEC_SEC_AL, MMProfileFlagEnd, 0, 0,"MtkVdecH265SecDecode-");
    #endif

    //ALOGD ("-MtkVdecH265SecDecode\n");
    return MTK_SECURE_AL_SUCCESS;
}

#ifdef MTK_SEC_VIDEO_PATH_SUPPORT
MTK_SECURE_AL_RESULT MtkVencH264SecInit(VideoH264SecMemInfoStruct* pInitStruct, unsigned long* H264_Drv_data_share_handle)
{
    ALOGD ("+MtkVencH264SecInit");
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = 0;
    MTEEC_PARAM param[4] = {0};
    UREE_SHAREDMEM_HANDLE pVencH264SecInit_share_handle = 0;
    VencH264SecInitStruct* ph264_sec_init_struct = (VencH264SecInitStruct*)malloc(sizeof(VencH264SecInitStruct));
    memset(ph264_sec_init_struct, 0, sizeof(VencH264SecInitStruct));
    if (gVencSession != 0) {
        ALOGE ("Venc Session already exists, line:%d", __LINE__);
    }
    else {   // create a new VDEC session
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
        if(gVencSession ==0)
        {
            ALOGE("Venc CreateSession failed gVencSession : %d \b", gVencSession);
            return MTK_SECURE_AL_FAIL;
        }
         ALOGI("Venc CreateSession gVencSession : %d \b", gVencSession);
    }

     // TODO: may remove this call later, just in case the alloator didn't init before
    if (MTK_SECURE_AL_SUCCESS != MtkVideoSecureMemAllocatorInit()) {
        ALOGE ("MtkVideoSecureMemAllocatorInit failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }


    pVenc_H264_Drv_data_share_handle = MtkVideoRegisterSharedMemory(pInitStruct[0].mem, pInitStruct[0].size);
    ALOGI ("@@ pVenc_H264_Drv_data_share_handle (0x%08X)\n", pVenc_H264_Drv_data_share_handle);
    *H264_Drv_data_share_handle = pVenc_H264_Drv_data_share_handle;

    ph264_sec_init_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    pVencH264SecInit_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_init_struct, sizeof(VencH264SecInitStruct));
    param[0].memref.handle = (uint32_t) pVencH264SecInit_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecInitStruct);

    ALOGD ("@@ pVenc_H264_Drv_data_share_handle(0x%08X)", pVenc_H264_Drv_data_share_handle);
    ALOGD ("@@ pVencH264SecInit_share_handle(0x%08X)", pVencH264SecInit_share_handle);

    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_INIT, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret: 0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
    else
    {
        ALOGI("UREE_TeeServiceCall succes, ret: 0x%x, line:%d", ret, __LINE__);
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecInit_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    free(ph264_sec_init_struct);

    ALOGD ("-MtkVencH264SecInit");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecDeinit(unsigned long H264_Drv_data_share_handle)
{
    ALOGD ("+MtkVencH264SecDeinit");
    MTEEC_PARAM param[4] ={0};
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_DEINIT, TZ_ParamTypes1(TZPT_VALUE_INPUT), param);

    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret:0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_Drv_data_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ret = UREE_CloseSession(gVencSession);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE ("Vdec UREE_CloseSession Error: %d, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }
    gVencSession = 0;

    ALOGD ("-MtkVencH264SecDeinit");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecEncode(unsigned long H264_Drv_data_share_handle, VideoH264SecMemInfoStruct* pEncStruct, VideoH264SecMemInfoStruct* pParStruct, unsigned long EncodeOption, unsigned char fgSecBuffer)
{
    ALOGD ("+MtkVencH264SecEncode fgSecBuffer= %d\n",fgSecBuffer);
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Frame_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Bitstream_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Encresult_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_FrameBuf_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_BitstreamBuf_share_handle = 0;

    MTEEC_PARAM param[4] = {0};
    UREE_SHAREDMEM_HANDLE pVencH264SecEnc_share_handle = 0;
    VencH264SecEncStruct* ph264_sec_enc_struct = (VencH264SecEncStruct*)malloc(sizeof(VencH264SecEncStruct));
    memset(ph264_sec_enc_struct, 0, sizeof(VencH264SecEncStruct));
    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
    ALOGI ("+MtkVencH264SecEncode pEncStruct[0].mem=[0x%x]\n",pEncStruct[0].mem);
    if(pEncStruct[0].mem)
    {
        pVenc_H264_Frame_share_handle = MtkVideoRegisterSharedMemory(pEncStruct[0].mem, pEncStruct[0].size);  //Frame Buffer Structure
        ALOGI ("+MtkVencH264SecEncode pVenc_H264_Frame_share_handle[0x%x]\n",pVenc_H264_Frame_share_handle);
    }
    pVenc_H264_Bitstream_share_handle = MtkVideoRegisterSharedMemory(pEncStruct[1].mem, pEncStruct[1].size);    //Bitstream Buffer Structure
    ALOGI ("+MtkVencH264SecEncode pVenc_H264_Bitstream_share_handle[0x%x]\n",pVenc_H264_Bitstream_share_handle);
    pVenc_H264_Encresult_share_handle = MtkVideoRegisterSharedMemory(pEncStruct[2].mem, pEncStruct[2].size);  //EncResult Structure
    ALOGI ("+MtkVencH264SecEncode pVenc_H264_Encresult_share_handle[0x%x]\n",pVenc_H264_Encresult_share_handle);
    if(fgSecBuffer)
    {
#ifdef BIN_SEC_SELF_TEST  //Only for BIN_SELF_TEST
         if(pParStruct[0].mem)
         {
           ALOGI ("+MtkVencH264SecEncode BIN_SELF_TEST starg got pParStruct[0].mem= 0x%x\n",pParStruct[0].mem);
           pVenc_H264_FrameBuf_share_handle = MtkVideoRegisterSharedMemory(pParStruct[0].mem, pParStruct[0].size); //Real Bitstream Buffer VA
           ALOGI ("+MtkVencH264SecEncode BIN_SELF_TEST  pVenc_H264_FrameBuf_share_handle[0x%x] SELF_TEST\n",pVenc_H264_Bitstream_share_handle);
         }
#else
        pVenc_H264_FrameBuf_share_handle = pParStruct[0].mem;  //Frame Buffer Secure Handle
         ALOGI ("+MtkVencH264SecEncode  pVenc_H264_FrameBuf_share_handle[0x%x]\n",pVenc_H264_FrameBuf_share_handle);
#endif

#ifdef BIN_SEC_SELF_TEST  //Only for BIN_SELF_TEST
        pVenc_H264_BitstreamBuf_share_handle = MtkVideoRegisterSharedMemory(pParStruct[1].mem, pParStruct[1].size); //Real Bitstream Buffer VA
        ALOGI ("+MtkVencH264SecEncode  BIN_SELF_TEST  pVenc_H264_BitstreamBuf_share_handle[0x%x] SELF_TEST\n",pVenc_H264_Bitstream_share_handle);
#else
        pVenc_H264_BitstreamBuf_share_handle = pParStruct[1].mem;  //Bitstream Buffer Secure Handle
        ALOGI ("+MtkVencH264SecEncode  pVenc_H264_BitstreamBuf_share_handle[0x%x]\n",pVenc_H264_Bitstream_share_handle);
#endif
    }
    else
    {
        ALOGI ("+MtkVencH264SecEncode pParStruct[0].mem=[0x%x]\n",pParStruct[0].mem);
        if(pParStruct[0].mem)
        {
            pVenc_H264_FrameBuf_share_handle = MtkVideoRegisterSharedMemory(pParStruct[0].mem, pParStruct[0].size); //Real Frame Buffer VA
            ALOGI ("+MtkVencH264SecEncode pVenc_H264_FrameBuf_share_handle[0x%x]\n",pVenc_H264_FrameBuf_share_handle);
        }
        pVenc_H264_BitstreamBuf_share_handle = MtkVideoRegisterSharedMemory(pParStruct[1].mem, pParStruct[1].size); //Real Bitstream Buffer VA
        ALOGI ("+MtkVencH264SecEncode pVenc_H264_BitstreamBuf_share_handle[0x%x]\n",pVenc_H264_BitstreamBuf_share_handle);
    }
    ph264_sec_enc_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    ph264_sec_enc_struct->encode_option = EncodeOption;
    ph264_sec_enc_struct->handle1 = pVenc_H264_Frame_share_handle;  //Frame Buffer Structure
    ph264_sec_enc_struct->va1 = pVenc_H264_FrameBuf_share_handle;   //Real Frame Buffer VA or Secure Handle
    ph264_sec_enc_struct->handle2 = pVenc_H264_Bitstream_share_handle;  //Bitstream Buffer Structure
    ph264_sec_enc_struct->va2 = pVenc_H264_BitstreamBuf_share_handle;   //Real Bitstream Buffer VA or Secure Handle
    ph264_sec_enc_struct->handle3 = pVenc_H264_Encresult_share_handle;  //EncResult Structure

    pVencH264SecEnc_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_enc_struct, sizeof(VencH264SecEncStruct));
    ALOGI ("+MtkVencH264SecEncode pVencH264SecEnc_share_handle[0x%x]\n",pVencH264SecEnc_share_handle);
    param[0].memref.handle = (uint32_t) pVencH264SecEnc_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecEncStruct);

    ALOGI ("@@ pVenc_H264 Frame Struct Handle(0x%08X)", ph264_sec_enc_struct->handle1);
    ALOGI ("@@ pVenc_H264 Frame Buffer VA Handle(0x%08X)", ph264_sec_enc_struct->va1);
    ALOGI ("@@ pVenc_H264 Bitstream Struct Handle(0x%08X)", ph264_sec_enc_struct->handle2);
    ALOGI ("@@ pVenc_H264 Bitstream Buffer VA handle(0x%08X)", ph264_sec_enc_struct->va2);
    ALOGI ("@@ pVenc_H264 Result Struct Handle(0x%08X)", ph264_sec_enc_struct->handle3);

    if(fgSecBuffer)
    {
        ALOGI ("+MtkVencH264SecEncode fgSecBuffer %d,TZCMD_VENC_AVC_ENCODE\n",fgSecBuffer );
        ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_ENCODE, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    }
    else
    {
        ALOGI ("+MtkVencH264SecEncode fgSecBuffer %d,TZCMD_VENC_AVC_ENCODE\n",fgSecBuffer );
        ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_ENCODE_NS, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    }

    ALOGI("TZCMD_VENC_AVC_ENCODE(s) done, line:%d", __LINE__);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret: 0x%x, line:0x%x", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_Encresult_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_Bitstream_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(pVenc_H264_Frame_share_handle && (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_Frame_share_handle)))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecEnc_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(!fgSecBuffer)
    {
        if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_BitstreamBuf_share_handle))
        {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }

        if(pVenc_H264_FrameBuf_share_handle && (MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_FrameBuf_share_handle)))
        {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

#ifdef BIN_SEC_SELF_TEST //Only for BIN_SELF_TEST   SELF TEST, keep input buffer is normal buffer
        if(fgSecBuffer && pVenc_H264_FrameBuf_share_handle)
        {
            if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_FrameBuf_share_handle))
            {
                ALOGE ("pVenc_H264_FrameBuf_share_handle failed, line:%d", __LINE__);
                return MTK_SECURE_AL_FAIL;
            }
        }
#endif


#ifdef BIN_SEC_SELF_TEST  //Only for BIN_SELF_TEST  //SELF TEST, keep output buffers are normal buffer
    if(fgSecBuffer && pVenc_H264_BitstreamBuf_share_handle)
    {
        if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_BitstreamBuf_share_handle))
        {
            ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }
#endif

    free(ph264_sec_enc_struct);

    ALOGD ("-MtkVencH264SecEncode");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecAllocWorkBuf(unsigned long H264_Drv_data_share_handle)
{
    ALOGD ("+MtkVencH264SecAllocWorkBuf");
    MTEEC_PARAM param[4];
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    UREE_SHAREDMEM_HANDLE pVencH264SecCmd_share_handle = 0;
    VencH264SecCmdStruct* ph264_sec_cmd_struct = (VencH264SecCmdStruct*)malloc(sizeof(VencH264SecCmdStruct));
    memset(ph264_sec_cmd_struct, 0, sizeof(VencH264SecCmdStruct));

    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    ph264_sec_cmd_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    pVencH264SecCmd_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_cmd_struct, sizeof(VencH264SecCmdStruct));
    param[0].memref.handle = (uint32_t) pVencH264SecCmd_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecCmdStruct);

    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_ALLOC_WORK_BUF, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret:0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecCmd_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    free(ph264_sec_cmd_struct);

    ALOGD ("-MtkVencH264SecAllocWorkBuf");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecFreeWorkBuf(unsigned long H264_Drv_data_share_handle)
{
    ALOGD ("+MtkVencH264SecFreeWorkBuf");
    MTEEC_PARAM param[4];
    TZ_RESULT ret;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    UREE_SHAREDMEM_HANDLE pVencH264SecCmd_share_handle = 0;
    VencH264SecCmdStruct* ph264_sec_cmd_struct = (VencH264SecCmdStruct*)malloc(sizeof(VencH264SecCmdStruct));
    memset(ph264_sec_cmd_struct, 0, sizeof(VencH264SecCmdStruct));

    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    ph264_sec_cmd_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    pVencH264SecCmd_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_cmd_struct, sizeof(VencH264SecCmdStruct));
    param[0].memref.handle = (uint32_t) pVencH264SecCmd_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecCmdStruct);

    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_FREE_WORK_BUF, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret:0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecCmd_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    free(ph264_sec_cmd_struct);

    ALOGD ("-MtkVencH264SecFreeWorkBuf");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecShareWorkBuf(unsigned long H264_Drv_data_share_handle, VideoH264SecMemInfoStruct* pCmdStruct, unsigned long* pShareHandle)
{
    ALOGD ("+MtkVencH264SecShareWorkBuf");
    MTEEC_PARAM param[4] = {0};
    TZ_RESULT ret =0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RCInfo_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RCCode_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RecLuma_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RecChroma_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RefLuma_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_RefChroma_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_MVInfo1_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_MVInfo2_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_PPSTemp_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_IDRTemp_share_handle = 0;
    UREE_SHAREDMEM_HANDLE pVencH264SecCmd_share_handle = 0;
    VencH264SecShareWorbufStruct* ph264_sec_cmd_struct = (VencH264SecShareWorbufStruct*)malloc(sizeof(VencH264SecShareWorbufStruct));
    memset(ph264_sec_cmd_struct, 0, sizeof(VencH264SecShareWorbufStruct));

    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    //pVenc_H264_Drv_data_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[0].mem, pCmdStruct[0].size);
    pVenc_H264_RCInfo_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[0].mem, pCmdStruct[0].size);
    pVenc_H264_RCCode_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[1].mem, pCmdStruct[1].size);
    pVenc_H264_RecLuma_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[2].mem, pCmdStruct[2].size);
    pVenc_H264_RecChroma_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[3].mem, pCmdStruct[3].size);
    pVenc_H264_RefLuma_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[4].mem, pCmdStruct[4].size);
    pVenc_H264_RefChroma_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[5].mem, pCmdStruct[5].size);
    pVenc_H264_MVInfo1_handle = MtkVideoRegisterSharedMemory(pCmdStruct[6].mem, pCmdStruct[6].size);
    pVenc_H264_MVInfo2_handle = MtkVideoRegisterSharedMemory(pCmdStruct[7].mem, pCmdStruct[7].size);
    pVenc_H264_PPSTemp_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[8].mem, pCmdStruct[8].size);
    pVenc_H264_IDRTemp_share_handle = MtkVideoRegisterSharedMemory(pCmdStruct[9].mem, pCmdStruct[9].size);

    ph264_sec_cmd_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    pShareHandle[0] = ph264_sec_cmd_struct->venc_H264_RCInfo_share_handle = pVenc_H264_RCInfo_share_handle;;
    pShareHandle[1] = ph264_sec_cmd_struct->venc_H264_RCCode_share_handle = pVenc_H264_RCCode_share_handle;
    pShareHandle[2] = ph264_sec_cmd_struct->venc_H264_RecLuma_share_handle = pVenc_H264_RecLuma_share_handle;
    pShareHandle[3] = ph264_sec_cmd_struct->venc_H264_RecChroma_share_handle = pVenc_H264_RecChroma_share_handle;
    pShareHandle[4] = ph264_sec_cmd_struct->venc_H264_RefLuma_share_handle = pVenc_H264_RefLuma_share_handle;
    pShareHandle[5] = ph264_sec_cmd_struct->venc_H264_RefChroma_share_handle = pVenc_H264_RefChroma_share_handle;
    pShareHandle[6] = ph264_sec_cmd_struct->venc_H264_MVInfo1_handle = pVenc_H264_MVInfo1_handle;
    pShareHandle[7] = ph264_sec_cmd_struct->venc_H264_MVInfo2_handle = pVenc_H264_MVInfo2_handle;
    pShareHandle[8] = ph264_sec_cmd_struct->venc_H264_PPSTemp_share_handle = pVenc_H264_PPSTemp_share_handle;
    pShareHandle[9] = ph264_sec_cmd_struct->venc_H264_IDRTemp_share_handle = pVenc_H264_IDRTemp_share_handle;

    pVencH264SecCmd_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_cmd_struct, sizeof(VencH264SecShareWorbufStruct));
    param[0].memref.handle = (uint32_t) pVencH264SecCmd_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecShareWorbufStruct);

    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_SHARE_WORK_BUF, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret:0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecCmd_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    free(ph264_sec_cmd_struct);

    ALOGD ("-MtkVencH264SecShareWorkBuf");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecUnshareWorkBuf(unsigned long H264_Drv_data_share_handle, unsigned long* pUnsharBufHandle)
{
    ALOGD ("+MtkVencH264SecUnshareWorkBuf");

    TZ_RESULT ret;

    UREE_SHAREDMEM_HANDLE pVenc_H264_RCInfo_share_handle = pUnsharBufHandle[0];
    UREE_SHAREDMEM_HANDLE pVenc_H264_RCCode_share_handle = pUnsharBufHandle[1];
    UREE_SHAREDMEM_HANDLE pVenc_H264_RecLuma_share_handle = pUnsharBufHandle[2];
    UREE_SHAREDMEM_HANDLE pVenc_H264_RecChroma_share_handle = pUnsharBufHandle[3];
    UREE_SHAREDMEM_HANDLE pVenc_H264_RefLuma_share_handle = pUnsharBufHandle[4];
    UREE_SHAREDMEM_HANDLE pVenc_H264_RefChroma_share_handle = pUnsharBufHandle[5];
    UREE_SHAREDMEM_HANDLE pVenc_H264_MVInfo1_handle = pUnsharBufHandle[6];
    UREE_SHAREDMEM_HANDLE pVenc_H264_MVInfo2_handle = pUnsharBufHandle[7];
    UREE_SHAREDMEM_HANDLE pVenc_H264_PPSTemp_share_handle = pUnsharBufHandle[8];
    UREE_SHAREDMEM_HANDLE pVenc_H264_IDRTemp_share_handle = pUnsharBufHandle[9];

    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RCInfo_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RCCode_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RecLuma_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RecChroma_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RefLuma_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_RefChroma_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_MVInfo1_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_MVInfo2_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_PPSTemp_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVenc_H264_IDRTemp_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    ALOGD ("-MtkVencH264SecUnshareWorkBuf");
    return MTK_SECURE_AL_SUCCESS;
}

MTK_SECURE_AL_RESULT MtkVencH264SecCopyWorkBuf(unsigned long H264_Drv_data_share_handle)
{
    ALOGD ("+MtkVencH264SecCopyWorkBuf");
    MTEEC_PARAM param[4] ={0};
    TZ_RESULT ret =0;
    UREE_SHAREDMEM_HANDLE pVenc_H264_Drv_data_share_handle = H264_Drv_data_share_handle;
    UREE_SHAREDMEM_HANDLE pVencH264SecCmd_share_handle = 0;
    VencH264SecCmdStruct* ph264_sec_cmd_struct = (VencH264SecCmdStruct*)malloc(sizeof(VencH264SecCmdStruct));
    memset(ph264_sec_cmd_struct, 0, sizeof(VencH264SecCmdStruct));

    if (gVencSession == 0)
    {
        //ALOGE ("Venc Session create by UUID: %s", TZ_TA_VENC_UUID);
        ALOGE ("[Warning] Venc Session not exists, line:%d", __LINE__);
        ret = UREE_CreateSession(TZ_TA_VENC_UUID, &gVencSession);
        if (ret != TZ_RESULT_SUCCESS)
        {
            ALOGE ("Venc CreateSession Error: %d, line:%d", ret, __LINE__);
            return MTK_SECURE_AL_FAIL;
        }
    }

    ph264_sec_cmd_struct->venc_h264_drv_data_share_handle = pVenc_H264_Drv_data_share_handle;
    pVencH264SecCmd_share_handle = MtkVideoRegisterSharedMemory(ph264_sec_cmd_struct, sizeof(VencH264SecCmdStruct));
    param[0].memref.handle = (uint32_t) pVencH264SecCmd_share_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = sizeof(VencH264SecCmdStruct);

    ret = UREE_TeeServiceCall(gVencSession, TZCMD_VENC_AVC_COPY_WORK_BUF, TZ_ParamTypes1(TZPT_MEMREF_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS)
    {
        ALOGE("UREE_TeeServiceCall failed, ret:0x%x, line:%d", ret, __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    if(MTK_SECURE_AL_SUCCESS != MtkVideoUnregisterSharedMemory(pVencH264SecCmd_share_handle))
    {
        ALOGE ("MtkVideoUnregisterSharedMemory failed, line:%d", __LINE__);
        return MTK_SECURE_AL_FAIL;
    }

    free(ph264_sec_cmd_struct);

    ALOGD ("-MtkVencH264SecCopyWorkBuf");
    return MTK_SECURE_AL_SUCCESS;
}

#endif
