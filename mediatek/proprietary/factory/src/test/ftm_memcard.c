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

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <errno.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <cutils/properties.h>
#include "common.h"
#include "miniui.h"
#include "ftm.h"

#ifdef FEATURE_FTM_MEMCARD

#define TAG                 "[MCARD] "

/* should be moved to customized part */
#define MAX_NUM_SDCARDS     (3)
#define MIN_SDCARD_IDX      (0)
#define MAX_SDCARD_IDX      (MAX_NUM_SDCARDS + MIN_SDCARD_IDX - 1)

enum {
#if defined(MTK_EMMC_SUPPORT) && !defined(MTK_SHARED_SDCARD)
    ITEM_FORMAT_EMMC_FAT,
#endif
    ITEM_PASS,
    ITEM_FAIL,
};

static item_t mcard_items[] = {
#if defined(MTK_EMMC_SUPPORT) && !defined(MTK_SHARED_SDCARD)
    //item(ITEM_FORMAT_EMMC_FAT,    uistr_info_emmc_format_item),
#endif
    //item(ITEM_PASS,   uistr_pass),
    //item(ITEM_FAIL,   uistr_fail),
    item(-1, NULL),
};

static item_t mcard_items_manu[] = {
#if defined(MTK_EMMC_SUPPORT) && !defined(MTK_SHARED_SDCARD)
    //item(ITEM_FORMAT_EMMC_FAT,    uistr_info_emmc_format_item),
#endif
    item(ITEM_PASS,   uistr_pass),
    item(ITEM_FAIL,   uistr_fail),
    item(-1, NULL),
};

int g_test_result = 0;

struct mcard {
    int          id;
    char         dev_path[512];
    char         sys_path[512];
    char         info[1024];
    char        *mntpnt;
    bool         mounted;
    bool         avail;
    int          blocknum;
    unsigned int checksum;
    const char  *format_stat;
};
struct mcard_array{
    struct mcard* mcard[1];
    char   info[2048];
    char* ptr_step;
    int mcard_no;

    text_t title;
    text_t text;
    bool isFormatting;
    bool exit_thd;
    pthread_t update_thd;
    struct ftm_module *mod;
    struct itemview *iv;
};

#define mod_to_mcard_array(p)  (struct mcard_array*)((char*)(p) + sizeof(struct ftm_module))

#define FREEIF(p)   do { if(p) free(p); (p) = NULL; } while(0)

extern sp_ata_data return_data;

static unsigned int mcard_mkcksum(const char *name, int maxlen)
{
    unsigned int cksum = 0;
    const char *p = name;

    while (*p != '\0' && maxlen) {
        cksum += *p++;
        maxlen--;
    }

    return cksum;
}

static unsigned int mcard_checksum(char *path)
{
    DIR *dp;
    struct dirent *dirp;
    unsigned int cksum = 0;

    if (NULL == (dp = opendir(path)))
        return -1;

    while (NULL != (dirp = readdir(dp))) {
        cksum += mcard_mkcksum(dirp->d_name, 256);
    }

    closedir(dp);

    return cksum;
}

static int mcard_statfs(char *mntpnt, struct statfs *stat)
{
    return statfs(mntpnt, stat);
}

static int mcard_mount(char *devpath, char *mntpnt, bool remount)
{

    int flags, rc;

    flags = MS_NODEV | MS_NOEXEC | MS_NOSUID | MS_DIRSYNC;

    LOGD(TAG "%s, mntpnt=%s, devpath=%s\n", __FUNCTION__, mntpnt, devpath);

    if (remount)
        flags |= MS_REMOUNT;

    /*
     * The mount masks restrict access so that:
     * 1. The 'system' user cannot access the SD card at all -
     *    (protects system_server from grabbing file references)
     * 2. Group users can RWX
     * 3. Others can only RX
     */
    rc = mount(devpath, mntpnt, "vfat", flags,
            "utf8,uid=1000,gid=1015,fmask=702,dmask=702,shortname=mixed");

    if (rc && errno == EROFS) {
        flags |= MS_RDONLY;
        rc = mount(devpath, mntpnt, "vfat", flags,
            "utf8,uid=1000,gid=1015,fmask=702,dmask=702,shortname=mixed");
    }

    if (rc) {
        LOGE(TAG "%s: mount fail, %d (%s)\n", __FUNCTION__, errno, strerror(errno));
    }

    return rc;
}

static int mcard_umount(char *mntpnt)
{
    pid_t pid;
    int child_stat = 0;

    LOGD(TAG "%s: mntpnt=%s\n", __FUNCTION__, mntpnt);
    if ((pid = fork()) < 0) {
        LOGE(TAG "%s, fork fails: %d (%s)\n", __FUNCTION__, errno, strerror(errno));
        return (-1);
    } else if (pid == 0) {
    	/*child process*/
        int err;
        err = execl("/system/bin/superumount", "superumount", mntpnt, NULL);
        exit(-2) ;
    } else {
    	/*parent process*/
        waitpid(pid, &child_stat, 0) ;
        if (WIFEXITED(child_stat)) {
            LOGE(TAG "%s: terminated by exit(%d)\n", __FUNCTION__, WEXITSTATUS(child_stat));
            return WEXITSTATUS(child_stat);
        } else {
            LOGE(TAG "%s: execl error, %d (%s)\n", __FUNCTION__, errno, strerror(errno));
            return -1;
        }
    }
    return -1;
}

static bool mcard_avail(struct mcard *mc)
{
    char name[20];
    char *ptr;
    DIR *dp;
    struct dirent *dirp;

    if (mc->id < MIN_SDCARD_IDX || mc->id > MAX_SDCARD_IDX)
        return false;

    sprintf(name, "mmc%d", mc->id - MIN_SDCARD_IDX);

    ptr  = &mc->sys_path[0];
    ptr += sprintf(ptr, "/sys/class/mmc_host/%s", name);

    if (NULL == (dp = opendir(mc->sys_path)))
        goto error;

    while (NULL != (dirp = readdir(dp))) {
        if (strstr(dirp->d_name, name)) {
            ptr += sprintf(ptr, "/%s", dirp->d_name);
            break;
        }
    }

    closedir(dp);

    if (!dirp)
        goto error;

    return true;

error:
    return false;
}

static void mcard_update_info(struct mcard *mc, char *info)
{
    struct statfs stat;
    char *ptr;
    int rc;
    bool old_avail = mc->avail;

    g_test_result = 0;
    return_data.memcard.sd1_total_size=0;
    return_data.memcard.sd1_free_size=0;

    mc->avail = mcard_avail(mc);

    unsigned int nr_sec;
    unsigned int sd_total_size = 0;
    int fd;
    mc->avail=1;
    if ((fd = open("/dev/block/mmcblk1", O_RDONLY, 0644)) < 0) {
        mc->avail=0;
    } else {
        if ((ioctl(fd, BLKGETSIZE, &nr_sec)) == -1) {
			LOGD("BLKGETSIZE fail \n");
        } else {
            sd_total_size = (nr_sec /2048);
        }
        close(fd);
    }

    /* prepare info */
    ptr  = info;

    ptr += sprintf(ptr, "%s: \n",uistr_info_sd);
    
    if(mc->avail)
    ptr += sprintf(ptr, "%s: %s\n", uistr_info_emmc_sd_avail,uistr_info_emmc_sd_yes );
    else
    ptr += sprintf(ptr, "%s: %s\n", uistr_info_emmc_sd_avail, uistr_info_emmc_sd_no);
    
    ptr += sprintf(ptr, "%s: %d MB\n",uistr_info_emmc_sd_total_size,
        sd_total_size);

    return_data.memcard.sd1_total_size=sd_total_size;
    return_data.memcard.sd1_free_size=0;

    if(mc->avail && sd_total_size > 0)
        g_test_result = 1;
    else
        g_test_result = 0;
    return;
}


static void *mcard_update_iv_thread(void *priv)
{
    struct mcard_array *ma = (struct mcard_array *)priv;
    struct itemview *iv = ma->iv;
    struct statfs stat;
    int count = 1, chkcnt = 10;
    int index = 0;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    while (1) {
        usleep(100000);
        chkcnt--;

        if (ma->exit_thd)
            break;

        if (chkcnt > 0)
            continue;

        if (ma->isFormatting)
            continue;

        chkcnt = 10;

        mcard_update_info(ma->mcard[0],ma->info);

        iv->redraw(iv);
        ma->exit_thd = true;
    }
    pthread_exit(NULL);

    LOGD(TAG "%s: Exit\n", __FUNCTION__);

    return NULL;
}

int mcard_entry(struct ftm_param *param, void *priv)
{
    char *ptr;
    int chosen;
    int index = 0;
    bool exit_val = false;
    struct mcard_array *ma = (struct mcard_array *)priv;
    struct itemview *iv;
    struct statfs stat;
    pid_t pid;

    LOGD(TAG "%s\n", __FUNCTION__);

    init_text(&ma->title, param->name, COLOR_YELLOW);
    init_text(&ma->text, &ma->info[0], COLOR_YELLOW);

    mcard_update_info(ma->mcard[0],ma->info);

    ma->isFormatting = false;
    ma->exit_thd = false;

    if (!ma->iv) {
        iv = ui_new_itemview();
        if (!iv) {
            LOGD(TAG "No memory");
            return -1;
        }
        ma->iv = iv;
    }

    iv = ma->iv;
    iv->set_title(iv, &ma->title);
    if (FTM_MANUAL_ITEM == param->test_type){
        iv->set_items(iv, mcard_items_manu, 0);
    } else {
        iv->set_items(iv, mcard_items, 0);
    }
    iv->set_text(iv, &ma->text);
    iv->redraw(iv);

    if (FTM_MANUAL_ITEM == param->test_type){
        do {
            chosen = iv->run(iv, &exit_val);
            switch (chosen) {
                case ITEM_PASS:
                case ITEM_FAIL:
                    if (chosen == ITEM_PASS) {
                        ma->mod->test_result = FTM_TEST_PASS;
                    } else if (chosen == ITEM_FAIL) {
                        ma->mod->test_result = FTM_TEST_FAIL;
                    }

                ma->mcard[0]->format_stat = "Default";

                exit_val = true;
                break;
            }

            if (exit_val) {
                ma->exit_thd = true;
                break;
            }
        } while (1);

    } else if(FTM_AUTO_ITEM == param->test_type){
        pthread_create(&ma->update_thd, NULL, mcard_update_iv_thread, priv);
        pthread_join(ma->update_thd, NULL);
    }

    if (g_test_result > 0) {
        ma->mod->test_result = FTM_TEST_PASS;
    } else {
        ma->mod->test_result = FTM_TEST_FAIL;
    }

    return 0;
}

int mcard_init(void)
{
    int ret = 0;
    struct ftm_module *mod = NULL;
    struct mcard *mc = NULL;
    struct mcard_array *ma = NULL;
    char *env_var;
    struct statfs stat;
    LOGD(TAG "%s\n", __FUNCTION__);

    mod = ftm_alloc(ITEM_MEMCARD, sizeof(struct mcard_array));
    if (!mod)
        return -ENOMEM;
    mc = (struct mcard*)malloc(sizeof(struct mcard));
    if(!mc){
        ftm_free(mod);
        return -ENOMEM;
    }

    ma  = mod_to_mcard_array(mod);
    ma->mod      = mod;
    mc->id       = 0;
    LOGD(TAG "mc->id=%d \n", mc->id);

    mc->mounted  = false;
    mc->avail    = false;
    mc->mntpnt   = NULL;
    mc->checksum = -1;
    mc->blocknum = -1;

    mc->format_stat = "Default";

    ma->mcard[0] = mc;
    ma->mcard_no += 1;

    LOGD(TAG "ma->mcard_no(%d)\n",ma->mcard_no);
    mcard_update_info(ma->mcard[0],ma->info);
    ret = ftm_register(mod, mcard_entry, (void*)ma);
    return ret;
}

#endif
