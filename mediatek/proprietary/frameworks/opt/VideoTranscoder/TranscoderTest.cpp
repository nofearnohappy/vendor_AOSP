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
 *   TranscoderTest.cpp
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   Video transcoder API client sample code
 *
 * Author:
 * -------
 *   Morris Yang (mtk03147)
 *
 ****************************************************************************/
#include <utils/Log.h>
#include <dlfcn.h>
#include <pthread.h>
#undef LOG_TAG
#define LOG_TAG "VideoTranscoderTest"
#include "MtkVideoTranscoder.h"
#include <cstdlib>
#include <sys/stat.h>
#include <utils/RefBase.h>

#include <powermanager/PowerManager.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include <powermanager/IPowerManager.h>

#define MTK_VIDEO_TRANSCODER_LIB_NAME            "/system/lib/libMtkVideoTranscoder.so"
#define MTK_VIDEO_TRANSCODER_API_TRANSCODE    "mtk_video_transcoder_transcode"
#define MTK_VIDEO_TRANSCODER_API_GET_PROGRESS "mtk_video_transcoder_get_progress"
#define MTK_VIDEO_TRANSCODER_API_CANCEL       "mtk_video_transcoder_cancel"
#define MTK_VIDEO_TRANSCODER_API_INIT         "mtk_video_transcoder_init"
#define MTK_VIDEO_TRANSCODER_API_DEINIT         "mtk_video_transcoder_deinit"

typedef int (*transcoder_api_transcode)(Mtk_VideoTranscoder_Context, Mtk_VideoTranscoder_Params);
typedef unsigned int (*transcoder_api_get_progress)(Mtk_VideoTranscoder_Context);
typedef void (*transcoder_api_cancel)(Mtk_VideoTranscoder_Context);
typedef void (*transcoder_api_init)(Mtk_VideoTranscoder_Context *);
typedef void (*transcoder_api_deinit)(Mtk_VideoTranscoder_Context);


Mtk_VideoTranscoder_Context gContext;

#define PATH_SIZE   256
struct ThreadData
{
    void *func;
    int width;
    int height;
    char input_path[PATH_SIZE];
    char output_path[PATH_SIZE];
};

long GetFileSize(const char *filename)
{
    struct stat stat_buf;
    int rc = stat(filename, &stat_buf);
    return (rc == 0) ? (stat_buf.st_size) : (-1);
}

using namespace android;

class PMDeathRecipient : public IBinder::DeathRecipient
{
    public:
        PMDeathRecipient() {}
        virtual     ~PMDeathRecipient() {}

        virtual     void        binderDied(const wp<IBinder> &who);

    private:
        PMDeathRecipient(const PMDeathRecipient &);
        PMDeathRecipient &operator = (const PMDeathRecipient &);
};

void PMDeathRecipient::binderDied(const wp<IBinder> &)
{
    //well...
}

sp<IPowerManager>       mPowerManager;
sp<IBinder>             mWakeLockToken;
sp<PMDeathRecipient>    mDeathRecipient;

void acquireWakeLock()
{
    if (mPowerManager == 0)
    {
        // use checkService() to avoid blocking if power service is not up yet
        sp<IBinder> binder =
            defaultServiceManager()->checkService(String16("power"));
        if (binder == 0)
        {
            printf("cannot connect to the power manager service");
        }
        else
        {
            mPowerManager = interface_cast<IPowerManager>(binder);
            binder->linkToDeath(mDeathRecipient);
        }
    }
    if (mPowerManager != 0)
    {
        sp<IBinder> binder = new BBinder();
        int64_t token = IPCThreadState::self()->clearCallingIdentity();
        status_t status = mPowerManager->acquireWakeLock(POWERMANAGER_PARTIAL_WAKE_LOCK,
                                                         binder,
                                                         String16("TranscoderTest"),
                                                         String16("media"));
        IPCThreadState::self()->restoreCallingIdentity(token);
        if (status == NO_ERROR)
        {
            mWakeLockToken = binder;
        }
    }
}

void releaseWakeLock()
{
    if (mPowerManager != 0)
    {
        int64_t token = IPCThreadState::self()->clearCallingIdentity();
        mPowerManager->releaseWakeLock(mWakeLockToken, 0);
        IPCThreadState::self()->restoreCallingIdentity(token);
    }
    mWakeLockToken.clear();
}

void *TranscodeThread(void *data)
{
    ALOGD("+TranscodeThread");
    ThreadData *pData = (ThreadData *)data;

    printf("File name %s\n", pData->input_path);
    printf("Target width %d, target height %d\n", pData->width, pData->height);

    transcoder_api_transcode do_transcode = (transcoder_api_transcode)pData->func;

    // transcode
    Mtk_VideoTranscoder_Params params;
    params.target_width = pData->width;
    params.target_height = pData->height;

    params.begin_ts = 0;//10*1000;
    params.end_ts = 0;//40*1000;
    params.target_bit_rate = 512 * 1024;
    params.target_frame_rate = 0;   // set to 0 to use source clip fps
    params.input_path = pData->input_path;
    params.output_path = "/sdcard/output.mp4";

    do_transcode(gContext, params);

    long fileSize = GetFileSize(params.output_path);
    printf("Output file size is %ld\n", fileSize);
    if (fileSize != 3343377)
    {
        printf("Found mismatched size!!!\n");
        while (1) {}
    }

    ALOGD("-TranscodeThread");
    return 0;
}

int main(int argc, char **argv)
{
    // load video transcoder library
    if (argc < 4)
    {
        printf("Incorrect number of arguments(%d)\n", argc);
        return 1;
    }
    ThreadData rThreadData;
    memcpy(rThreadData.input_path, argv[1], PATH_SIZE);
    rThreadData.width = atoi(argv[2]);
    rThreadData.height = atoi(argv[3]);

    mDeathRecipient = new PMDeathRecipient();

    void *pVideoTranscoderLib = NULL;
    pVideoTranscoderLib = dlopen(MTK_VIDEO_TRANSCODER_LIB_NAME, RTLD_NOW);
    if (NULL == pVideoTranscoderLib)
    {
        ALOGE("%s", dlerror());
        return 0;
    }

    // retrieve video transcoder api (DO_TRANSCODE)
    transcoder_api_transcode do_transcode = (transcoder_api_transcode) dlsym(pVideoTranscoderLib, MTK_VIDEO_TRANSCODER_API_TRANSCODE);
    if (NULL == do_transcode)
    {
        ALOGE("%s", dlerror());
        dlclose(pVideoTranscoderLib);
        return 0;
    }

    // retrieve video transcoder api (DO_GET_PROGRESS)
    transcoder_api_get_progress do_get_progress = (transcoder_api_get_progress) dlsym(pVideoTranscoderLib, MTK_VIDEO_TRANSCODER_API_GET_PROGRESS);
    if (NULL == do_get_progress)
    {
        ALOGE("%s", dlerror());
        dlclose(pVideoTranscoderLib);
        return 0;
    }

    // retrieve video transcoder api (DO_CANCEL)
    transcoder_api_cancel do_cancel = (transcoder_api_cancel) dlsym(pVideoTranscoderLib, MTK_VIDEO_TRANSCODER_API_CANCEL);
    if (NULL == do_cancel)
    {
        ALOGE("%s", dlerror());
        dlclose(pVideoTranscoderLib);
        return 0;
    }

    // retrieve video transcoder api (DO_INIT)
    transcoder_api_init do_init = (transcoder_api_init) dlsym(pVideoTranscoderLib, MTK_VIDEO_TRANSCODER_API_INIT);
    if (NULL == do_init)
    {
        ALOGE("%s", dlerror());
        dlclose(pVideoTranscoderLib);
        return 0;
    }

    // retrieve video transcoder api (DO_DEINIT)
    transcoder_api_deinit do_deinit = (transcoder_api_deinit) dlsym(pVideoTranscoderLib, MTK_VIDEO_TRANSCODER_API_DEINIT);
    if (NULL == do_deinit)
    {
        ALOGE("%s", dlerror());
        dlclose(pVideoTranscoderLib);
        return 0;
    }

    // calling init
    do_init(&gContext);

#if 0
    // transcode
    do_transcode(
        "/sdcard/352x288_1520_30.3gp",
        "/sdcard/output.mp4",
        320,
        240,
        0 * 1000,
        11 * 1000);
#else

    acquireWakeLock();
    pthread_t tid;
    int ret;
    rThreadData.func = (void *)do_transcode;
    ret = pthread_create(&tid, NULL, TranscodeThread, (void *)(&rThreadData));
    if (ret != 0)
    {
        ALOGE("pthread_create error");
    }

#if 0 // cancel test
    usleep(3 * 1000 * 1000);
    do_cancel(gContext);
#endif

#if 0 // progress test
    for (int i = 0 ; i < 10 ; i++)
    {
        usleep(1 * 1000 * 1000);
        int percentage = do_get_progress(gContext);
    }
#endif

    pthread_join(tid, NULL);
#endif
    releaseWakeLock();

    // calling deinit
    do_deinit(gContext);

    // clean up
    dlclose(pVideoTranscoderLib);

    return 0;
}
