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
#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <cutils/xlog.h>
#include <cutils/logger.h>
#include <cutils/log.h>
#include "../include/xlogutils.h"
#include "xlog-filter.h"

#define XLOG_CACHE_SIZE 10

static int cachehead = 0, cachecount = 0;

static struct tag_level_cache {
    char name[XLOG_MODULE_NAME_MAX_LEN];
    uint32_t *offset;
} cache[XLOG_CACHE_SIZE];

struct _parcel {
    char name[XLOG_MODULE_NAME_MAX_LEN];
    int offset;
};
extern int (*write_to_log)(log_id_t, struct iovec *vec, size_t nr);


int xlogf_set_level(uint32_t level) 
{
    int retval = -1;
    int fd = open(XLOG_DEVICE, O_RDONLY);
    if (fd >= 0) {
        if (ioctl(fd, XLOG_SET_LEVEL, level) == 0) {
            retval = 0;
        }
        close(fd);
    }
    return retval;
}

uint32_t xlogf_get_level(void) 
{
    uint32_t level = XLOG_FILTER_DEFAULT_LEVEL;
    int fd = open(XLOG_DEVICE, O_RDONLY);
    if (fd >= 0) {
        ioctl(fd,XLOG_GET_LEVEL, &level);
        close(fd);
    }

    return level;
}

static uint32_t xlogf_get_module_ptr(const char *name) 
{
    int fd, i;
    static uint32_t *map = (uint32_t *)-1;
    struct _parcel parcel;
    char buf[24];
    int nfd;

    if (name == NULL)
        name = "XLOG_NULL_TAG";

    for(i=1;i<=cachecount;i++) {
        if(!strcmp(cache[(XLOG_CACHE_SIZE + cachehead - i) % XLOG_CACHE_SIZE].name, name)) {
        		if (cache[(XLOG_CACHE_SIZE + cachehead - i) % XLOG_CACHE_SIZE].offset == 0)
        			return XLOG_FILTER_DEFAULT_LEVEL;
            return *cache[(XLOG_CACHE_SIZE + cachehead - i) % XLOG_CACHE_SIZE].offset;
        }
    }

    strncpy(parcel.name, name, XLOG_MODULE_NAME_MAX_LEN - 1);

    if ((fd = open(XLOG_DEVICE, O_RDONLY)) < 0) 
        return XLOG_FILTER_DEFAULT_LEVEL;

    if (map == (uint32_t *)-1) { 
        map = (uint32_t *)mmap(0, 4096, PROT_READ, MAP_SHARED, fd, 0);
        if (map == (uint32_t *)-1) {
            close(fd);
            return XLOG_FILTER_DEFAULT_LEVEL;
        }
    }

    if (ioctl(fd,XLOG_FIND_MODULE, &parcel) == -1) {
        close(fd);
        return XLOG_FILTER_DEFAULT_LEVEL;
    }
    close(fd);

    //if return -1 then exit
    if(parcel.offset == -1) {
        return XLOG_FILTER_DEFAULT_LEVEL;
    }

    strncpy(cache[cachehead].name, parcel.name, XLOG_MODULE_NAME_MAX_LEN-1);
    cache[cachehead].offset = (uint32_t *)&map[parcel.offset];

    cachehead = ((cachehead + 1) % XLOG_CACHE_SIZE);
    if (cachecount < XLOG_CACHE_SIZE) 
        cachecount++;

    return map[parcel.offset];
}

int xlogf_native_tag_is_on(const char *name, int level) 
{
    uint32_t level_setting = (xlogf_get_module_ptr(name) >> 8) & 0xf;
    return (level >= level_setting) ? 1 : 0;
}

int xlogf_java_tag_is_on(const char *name, int level) 
{
    uint32_t level_setting = (xlogf_get_module_ptr(name) >> 16) & 0xf;
    return (level >= level_setting) ? 1 : 0;
}

int xlogf_native_xtag_is_on(const char *name, int level) 
{
    uint32_t level_setting = (xlogf_get_module_ptr(name) >> 12) & 0xf;
    return (level >= level_setting) ? 1 : 0;
}

int xlogf_java_xtag_is_on(const char *name, int level) 
{
    uint32_t level_setting = (xlogf_get_module_ptr(name) >> 20) & 0xf;
    return (level >= level_setting) ? 1 : 0;
}

int xlogf_tag_set_level(const char *name, uint32_t level) 
{
    int retval = -1;
    int fd = open(XLOG_DEVICE, O_RDONLY);
    if (fd >= 0) {
        struct xlog_entry ent;

        memset(&ent, 0, sizeof(sizeof(struct xlog_entry)));
        strlcpy(ent.name, name, XLOG_MODULE_NAME_MAX_LEN);
        ent.level = level;
        if (ioctl(fd, XLOG_SET_TAG_LEVEL, &ent) == 0) {
            retval = 0;
        }
        close(fd);
    }
    return retval;
}

#define LOG_BUF_SIZE	1024

int __xlog_buf_printf(int bufid, const struct xlog_record *rec, ...)
{
    const char *tag = rec->tag_str;
    if (!tag)
        tag = "";
    if(!xlogf_native_xtag_is_on(tag, rec->prio)) 
        return -1;
    int prio = rec->prio | LOGGER_ALE_MSG_RAW;
    
    va_list ap;
    char buf[LOG_BUF_SIZE];
    
    va_start(ap, rec);
    vsnprintf(buf, LOG_BUF_SIZE, rec->fmt_str, ap);
    va_end(ap);
    
    struct iovec vec[3];
    vec[0].iov_base   = (unsigned char *) &prio;
    vec[0].iov_len    = 1;
    vec[1].iov_base   = (void *) tag;
    vec[1].iov_len    = strlen(tag) + 1;
    vec[2].iov_base   = (void *) buf;
    vec[2].iov_len    = strlen(buf) + 1;
    
    int ret = write_to_log(bufid, vec, 3);

    if (rec->prio == ANDROID_LOG_FATAL) {
        abort();
    }
    return ret;
}


