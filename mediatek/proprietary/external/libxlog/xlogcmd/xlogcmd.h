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

#if !defined(_XLOGCMD_H)
#define _XLOGCMD_H

#define XLOG_TAG_LIST_PATH "/system/etc/xlog-filter-tags"
#define XLOG_DEFAULT_FILTER_PATH "/system/etc/xlog-filter-default"
#define XLOGCMD_SAVE_PATH "/data/misc/xlog/xlog-filter"
#define XLOGCMD_SAVE_DIR "/data/misc/xlog"

#define XLOGCMD_VER "2.00"
#define XLOGCMD_TAG "XLOG-FILTER-V2"

#define FILTER_MAX 1024

#define XLOG_DEFAULT_MASK (XLOG_VERBOSE_MASK | XLOG_DEBUG_MASK | XLOG_INFO_MASK | XLOG_WARN_MASK | XLOG_ERROR_MASK | XLOG_ASSERT_MASK)
#define XLOG_DEFAULT_STRING "debug"

struct xlog_filter_list {
    int count;

    unsigned int default_level;

    struct {
        char tag[32];
	unsigned int level;
    } filters[FILTER_MAX];
};

unsigned int xlog_level2mask(const char *level);

const char *xlog_mask2str(char *maskstr, unsigned int mask);

struct xlog_filter_list *xlog_read_filters(FILE *ffp);

void xlog_filter_set(struct xlog_filter_list *flist, const char *tag, unsigned int mask);

void xlog_filter_dump(const struct xlog_filter_list *flist);

void xlog_filter_write(FILE *fp, const struct xlog_filter_list *flist);

void xlog_filter_delete(struct xlog_filter_list *flist, const char *tag);

#define DEBUG_LOG

#endif
