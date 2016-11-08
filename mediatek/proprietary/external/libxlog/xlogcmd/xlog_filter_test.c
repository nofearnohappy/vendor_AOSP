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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "../include/xlogutils.h"
#include "xlogcmd.h"

#define TESTCASE_1                                                \
    "XLOG-FILTER-V2\n"                                            \
    "ALL\tverbose/info/debug\n"                                   \
    "TAG\tHelloWorld/1\tverbose/debug/info\n"                     \
    "TAG\tHelloWorld/2\tdebug/info/warn\n"                        \
    "TAG\tHelloWorld/3\tinfo/warn/error\n"                        \
    "TAG\tHelloWorld/4\twarn/error/assert\n"                      \
    "TAG\tHelloWorld/5\terror/assert/off\n"                       \
    "TAG\tHelloWorld/6\tassert/off/verbose\n"                     \
    "TAG\tHelloWorld/7\toff/verbose/debug\n"

#define TESTCASE_2                                                      \
    "XLOG-FILTER-V2\n"                                                  \
    "ALL\tdebug/verbose/error\n"					\
    "TAG\tHelloWorld/1\tverbose/debug/info\n"                           \
    "TAG\tHelloWorld/2\tdebug/info/warn\n"                              \
    "TAG\tHelloWorld/3\tinfo/warn/error\n"                              \
    "TAG\tHelloWorld/5\terror/assert/off\n"                             \
    "TAG\tHelloWorld/6\tassert/off/verbose\n"                           \
    "TAG\tHelloWorld/7\toff/verbose/debug\n"

#define TESTCASE_3                                                      \
    "XLOG-FILTER-V2\n"							\
    "ALL\tdebug\n"							\
    "TAG\tHelloWorld/0\ton\n"						\
    "TAG\tHelloWorld/1\tverbose\n"					\
    "TAG\tHelloWorld/2\tdebug\n"					\
    "TAG\tHelloWorld/3\tinfo\n"						\
    "TAG\tHelloWorld/4\twarn\n"						\
    "TAG\tHelloWorld/5\terror\n"					\
    "TAG\tHelloWorld/6\tassert\n"					\
    "TAG\tHelloWorld/7\toff\n"

#define TESTCASE_4                                                      \
    "XLOG-FILTER-V2\n"							\
    "ALL\tverbose/debug+verbose/verbose\n"				\
    "TAG\tHelloWorld/0\ton\n"						\


void filter_testcase(const char *input)
{
    FILE *ffp = fmemopen((void *)input, strlen(input), "r");
    struct xlog_filter_list *flist = xlog_read_filters(ffp);
    printf("input:\n%s\ndump:\n", input);
    xlog_filter_dump(flist);

    free(flist);
    fclose(ffp);
}

void filter_test_add(void)
{
    FILE *ffp = fmemopen((void *)TESTCASE_2, strlen(TESTCASE_2), "r");
    struct xlog_filter_list *flist = xlog_read_filters(ffp);
    xlog_filter_set(flist, "HelloWorld/4", ANDROID_LOG_VERBOSE);
    xlog_filter_set(flist, "HelloWorld/8", ANDROID_LOG_DEBUG);
    xlog_filter_set(flist, "HelloWorld/0", ANDROID_LOG_INFO);

    printf("input:\n%s\ndump:\n", TESTCASE_2);
    xlog_filter_dump(flist);
    free(flist);
    fclose(ffp);
}

int main(int argc, char *argv[])
{
    filter_testcase(TESTCASE_1);

    filter_test_add();

    filter_testcase(TESTCASE_3);

    filter_testcase(TESTCASE_4);

    return 0;
}
