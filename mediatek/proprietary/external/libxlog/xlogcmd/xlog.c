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
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <sys/time.h> 
#include <sys/stat.h>
#include <private/android_filesystem_config.h>
#include <android/log.h>
#include <cutils/properties.h>
#include <cutils/xlog.h>

#ifdef HAVE_AEE_FEATURE
#include "../../../../external/aee/binary/inc/aee.h"
#endif
#include "../include/xlogutils.h"
#include "xlogcmd.h"

#define AEE_MODE_MTK_ENG        1
#define AEE_MODE_MTK_USER       2
#define AEE_MODE_CUSTOMER_ENG   3
#define AEE_MODE_CUSTOMER_USER  4

#define	LOGD_FILTER		"persist.log.tag"
static void system_filter_set(const struct xlog_filter_list *flist)
{
    /* xlog_filter_dump(flist); */
	int i;

    xlogf_set_level(flist->default_level);
	switch (flist->default_level&0xF) {

	case ANDROID_LOG_DEBUG:
		property_set(LOGD_FILTER, "D");
		break;

	case ANDROID_LOG_INFO:
		property_set(LOGD_FILTER, "I");
		break;

	case ANDROID_LOG_WARN:
		property_set(LOGD_FILTER, "W");
		break;

	case ANDROID_LOG_ERROR:
		property_set(LOGD_FILTER, "E");
		break;

	case ANDROID_LOG_FATAL:
		property_set(LOGD_FILTER, "A");
		break;

	case ANDROID_LOG_SILENT:
		property_set(LOGD_FILTER, "S");
		break;

	case ANDROID_LOG_VERBOSE:
		property_set(LOGD_FILTER, "V");
		break;

	default:
		property_set(LOGD_FILTER, "V");
		break;
	}

    for (i = 0; i < flist->count; i++) {
        xlogf_tag_set_level(flist->filters[i].tag, flist->filters[i].level);
    }
}

static struct xlog_filter_list *filter_load_default(void)
{
    struct xlog_filter_list *flist;
    FILE *infp = fopen(XLOG_DEFAULT_FILTER_PATH, "r");
    if (infp != NULL) {
        flist = xlog_read_filters(infp);
        fclose(infp);
    }
    else {
        flist = xlog_read_filters(NULL);
    }

    return flist;
}

static struct xlog_filter_list *filter_load(void)
{
    struct xlog_filter_list *flist;
    FILE *infp = fopen(XLOGCMD_SAVE_PATH, "r");
    if (infp == NULL) {
	infp = fopen(XLOG_DEFAULT_FILTER_PATH, "r");
    }
    if (infp != NULL) {
        flist = xlog_read_filters(infp);
        fclose(infp);
    }
    else {
        flist = xlog_read_filters(NULL);
    }

    return flist;
}

static void filter_save(const struct xlog_filter_list *flist)
{
	if(access(XLOGCMD_SAVE_DIR,F_OK) != 0){
		mkdir(XLOGCMD_SAVE_DIR, 0775);
	}
    FILE *outfp = fopen(XLOGCMD_SAVE_PATH, "w");
    if (outfp != NULL) {
        xlog_filter_write(outfp, flist);
        fclose(outfp);

	system_filter_set(flist);
    }
    else {
        printf("Error: Can't save filter to %s (%s)\n", XLOGCMD_SAVE_PATH, strerror(errno));
    }
}

static void filter_delete(const char *tag)
{
    struct xlog_filter_list *flist = filter_load();
    if (tag != NULL) {
        xlog_filter_delete(flist, tag);
    }
    else {
        flist->count = 0;
    }
    filter_save(flist);
    free(flist);
}

static void filter_dump()
{
    struct xlog_filter_list *flist = filter_load();
    xlog_filter_dump(flist);
    free(flist);
}

static void filter_all_set(int level)
{
    struct xlog_filter_list *flist = filter_load();
    flist->default_level = level;
    filter_save(flist);
    free(flist);
}

static void filter_export(FILE *fp)
{
    struct xlog_filter_list *flist = filter_load();
    xlog_filter_write(fp, flist);
    free(flist);
}

static void filter_import(FILE *fp)
{
    struct xlog_filter_list *flist = xlog_read_filters(fp);
    filter_save(flist);
    free(flist);
}

static void filter_single_set(const char *tag, const char *level)
{
    struct xlog_filter_list *flist = filter_load();
    xlog_filter_set(flist, tag, xlog_string2level(level));
    filter_save(flist);
    free(flist);
}

static int is_log_enabled(int aee_mode)
{
    char pval[PROPERTY_VALUE_MAX];

    property_get("init.svc.adbd", pval, "stopped");
    if (strcmp(pval, "running") == 0) {
        return 1;
    }

    if ((aee_mode == AEE_MODE_MTK_ENG) || (aee_mode == AEE_MODE_CUSTOMER_ENG)) {
        return 1;
    }

    return 0;
}

static void debug_changed(void)
{
    int uart_disabled, user_built = 0;
#ifdef HAVE_AEE_FEATURE 
    int aee_mode = aee_get_mode();
#else
    int aee_mode = AEE_MODE_MTK_ENG;
#endif    
    printf("%s: aee mode %d\n", __func__, aee_mode);

    switch (aee_mode) {
    case AEE_MODE_MTK_ENG:
        //uart_disabled = 0;
        break;

    case AEE_MODE_MTK_USER:
       // uart_disabled = 1;
        user_built = 1;
        break;
        
    case AEE_MODE_CUSTOMER_ENG:
       // uart_disabled = 0;
        break;

    case AEE_MODE_CUSTOMER_USER:
       // uart_disabled = 1;
        user_built = 1;
        break;

    default:
	//uart_disabled = 1;
	user_built = 1;
	break;
    };


    struct xlog_filter_list *flist = NULL;

    int enabled = is_log_enabled(aee_mode);
    if (enabled) {
      flist = filter_load();
    }
    else {
      flist = xlog_read_filters(NULL);
      flist->default_level = xlog_string2level("error");
      system_filter_set(flist);
    }
    system_filter_set(flist);
    free(flist);
}

static void filter_boot(void)
{
    struct xlog_filter_list *flist = filter_load();

    if(flist == NULL) return;
    system_filter_set(flist);

    if (access(XLOGCMD_SAVE_PATH, R_OK | W_OK) != 0) {
        FILE *outfp = fopen(XLOGCMD_SAVE_PATH, "w");
        if (outfp != NULL) {
            xlog_filter_write(outfp, flist);
            fclose(outfp);
        }
        chown(XLOGCMD_SAVE_PATH, AID_SHELL, AID_SHELL);
        chmod(XLOGCMD_SAVE_PATH, S_IRUSR | S_IWUSR);
    }

	free(flist);
    debug_changed();
}

static void filter_reset(void)
{
    struct xlog_filter_list *flist = filter_load_default();
    /* If we cannot update current xlog configuration, then don't set live filter */
    if(flist == NULL) return;
    FILE *outfp = fopen(XLOGCMD_SAVE_PATH, "w");
    if (outfp != NULL) {
      xlog_filter_write(outfp, flist);
      fclose(outfp);
    }
    system_filter_set(flist);
	free(flist);
}

static void tag_list(void)
{
    FILE *fp = fopen(XLOG_TAG_LIST_PATH, "r");
    if (fp != NULL) {
        while (!feof(fp)) {
            char buf[1024];
            if (fgets(buf, sizeof(buf), fp) != NULL) {
                printf("%s", buf);
            }
        }
        fclose(fp);
    }
}

void usage(const char *progname)
{
    printf("xlog - module log filter manipulation tool\nUsage\n");
    printf("\txlog filter-add <module-name> <level>\n");
    printf("\txlog filter-delete <module-name>\n");
    printf("\txlog filter-delete-all\n\t\tDelete all tag filters setting\n");
    printf("\txlog filter-dump\n\t\tList current filter settings\n");
    printf("\txlog filter-export <filename>\n\t\tExport xlog filter\n");
    printf("\txlog filter-import <filename>\n\t\tImport xlog filter\n");
    printf("\txlog filter-reset\n\t\tReset to default xlog filters setting\n");
    printf("\txlog filter-set <level>\n");
    printf("\txlog tag-list\n");
    printf("\txlog version\n\n");
    printf("available level:\n");
    printf("       verbose\n");
    printf("       debug\n");
    printf("       info\n");
    printf("       warn\n");
    printf("       error\n");
    printf("       assert\n");
    printf("       on\n");
    printf("       off\n\n");
    printf("* Example *\n");
    printf("xlog filter-add ExceptionLog info\n");
    printf("       turn log for ExceptionLog on for all level>=info\n");
    printf("xlog filter-set off\n");
    printf("       turn off all logs for all level in all modules\n");
}

int main(int argc, char *argv[]) 
{
    if (argc < 2) {
        usage(argv[0]);
        return 1;
    }

    const char *name = argv[1];
    if (!strcmp(name, "boot")) {
        filter_boot();
    }
    else if (strcmp(name, "debug-changed") == 0) {
        debug_changed();
    }
    else if (!strcmp(name, "filter-add")) {
        if (argc == 4) {
            filter_single_set(argv[2], argv[3]);
        } 
        else {
            printf("Error: filter command invalid argument\n");
        }
    } 
    else if (!strcmp(name, "filter-delete")) {
        if (argc == 3) {
            filter_delete(argv[2]);
        }
        else {
            printf("Error: filter-delete command invalid argument\n");
        }
    }
    else if (!strcmp(name, "filter-delete-all")) {
        filter_delete(NULL);
    }
    else if (!strcmp(name, "filter-dump")) {
        filter_dump();
    }
    else if (strcmp(name, "filter-export") == 0) {
        switch (argc) {

        case 2: {
            filter_export(stdout);
            break;
        }

        case 3: {
            FILE *ffp = fopen(argv[2], "w");
            if (ffp != NULL) {
                filter_export(ffp);
                fclose(ffp);
            }
            else {
                printf("Error: filter-export cannot open file %s\n", argv[2]);
            }
            break;
        }

        default:
            printf("Error: filter-export Invalid argument\n");
            break;

        }
    }
    else if (strcmp(name, "filter-import") == 0) {
        switch (argc) {

        case 2: {
            filter_import(stdin);
            break;
        }

        case 3: {
            FILE *ffp = fopen(argv[2], "r");
            if (ffp != NULL) {
                filter_import(ffp);
                fclose(ffp);
            }
            else {
                printf("Error: filter-import cannot open file %s\n", argv[2]);
            }
            break;
        }

        default:
            printf("Error: filter-import Invalid argument\n");
            break;

        }
    }
    else if (strcmp(name, "filter-set") == 0) {
        if (argc == 3) {
            filter_all_set(xlog_string2level(argv[2]));
        }
        else {
            printf("Error: filter-set invalid argument\n");
        }
    }
    else if (strcmp(name, "filter-reset") == 0) {
        filter_reset();
    }
    else if (strcmp(name, "tag-list") == 0) {
        tag_list();
    }
    else if (strcmp(name, "version") == 0) {
	printf(XLOGCMD_VER "\n");
    }
    else {
        usage(argv[0]);
    }
    return 0;
}
