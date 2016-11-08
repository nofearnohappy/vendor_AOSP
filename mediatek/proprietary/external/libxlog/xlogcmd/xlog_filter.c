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

#define _GNU_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "../include/xlogutils.h"
#include "xlogcmd.h"

static unsigned int s2level(const char *level, unsigned int default_level)
{
    if ((strcmp(level, "verbose") == 0) || (strcmp(level, "on") == 0)) {
        return ANDROID_LOG_VERBOSE;
    }
    else if (!strcmp(level, "debug")) {
        return ANDROID_LOG_DEBUG;
    }
    else if (!strcmp(level, "info")) {
        return ANDROID_LOG_INFO;
    }
    else if (!strcmp(level, "warn")) {
        return ANDROID_LOG_WARN;
    }
    else if (!strcmp(level, "error")) {
        return ANDROID_LOG_ERROR;
    }
    else if (!strcmp(level, "assert")) {
        return ANDROID_LOG_FATAL;
    }
    else if (!strcmp(level, "off")) {
        return ANDROID_LOG_SILENT;
    }
    return default_level;
}

static unsigned int layer2level(const char *level_string, uint32_t default_level)
{
    char lstr[256];
    uint32_t xlog_level, log_level;

    strcpy(lstr, level_string);
    char *sep = strchr(lstr, '+');

    if (sep != NULL) {
        *sep = 0;
        xlog_level = s2level(lstr, (default_level >> 4) & 0xf);
        log_level = s2level(sep + 1, default_level & 0xf);
    }
    else {
        xlog_level = s2level(level_string, (default_level >> 4) & 0xf);
        log_level = s2level(level_string, default_level & 0xf);
    }

    return (xlog_level << 4) | log_level;
}

unsigned int xlog_string2level(const char *level)
{
    if (level != NULL && strlen(level) < 48) {
        char lstr[48];
        strcpy(lstr, level);

        char *sep = strchr(lstr, '/');
        if (sep != NULL) {
            char *sep1 = strchr(sep + 1, '/');
            if (sep1 != NULL) {
                *sep = *sep1 = 0;

                unsigned int jlevel = layer2level(lstr, (ANDROID_LOG_VERBOSE << 4) | ANDROID_LOG_VERBOSE);
                unsigned int nlevel = layer2level(sep + 1, (ANDROID_LOG_DEBUG << 4) | ANDROID_LOG_VERBOSE);
                unsigned int klevel = layer2level(sep1 + 1, (ANDROID_LOG_VERBOSE << 4) | ANDROID_LOG_VERBOSE);

                return (jlevel << 16) | (nlevel << 8) | (klevel);
            }
        }
	else {
            uint32_t level = layer2level(lstr, (ANDROID_LOG_VERBOSE << 4) | ANDROID_LOG_VERBOSE);
            return (level << 16) | (level << 8) | level;
	}
           
    }
    /* unknown level fall back */
    return XLOG_FILTER_DEFAULT_LEVEL;
}

static char *getline_without_eol(FILE *fp)
{
    char *str = NULL;

    if (!feof(fp)) {
        str = malloc(256);
        if (fgets(str, 256, fp) != NULL) {
            size_t len = strlen(str);
            if (len > 0) {
                size_t current_len = len;
                while (current_len) {
                    current_len--;
                    if ((str[current_len] != '\r') && (str[current_len] != '\n')) {
                        break;
                    }
                    str[current_len] = 0;
                }
            }
        }
        else {
            free(str);
            str = NULL;
        }
    }
    return str;
}

struct xlog_filter_list *xlog_read_filters(FILE *ffp)
{
    struct xlog_filter_list *flist = malloc(sizeof(struct xlog_filter_list));

    if(flist == NULL) return NULL;
    memset(flist, 0, sizeof(struct xlog_filter_list));
    flist->default_level = XLOG_FILTER_DEFAULT_LEVEL;

    if (ffp != NULL) {
        char *bufptr = NULL;

        /* Check filter header */
        bufptr = getline_without_eol(ffp);
        if (bufptr == NULL) {
                goto error;
        }
        if (strcmp(bufptr, XLOGCMD_TAG) != 0){
                free(bufptr);
                goto error; 
	}
        free(bufptr);

        while (1) {
            bufptr = getline_without_eol(ffp);
            if (bufptr != NULL) {
                const char *cmd = strtok(bufptr, "\t");
                if (cmd != NULL) {
                    const char *arg1 = strtok(NULL, "\t");
                    const char *arg2 = NULL;
                    if (arg1 != NULL) {
                        arg2 = strtok(NULL, "\t");
                    }

                    if (strcmp(cmd, "ALL") == 0) {
                        flist->default_level = xlog_string2level(arg1);
                    }
                    else if (strcmp(cmd, "TAG") == 0) {
                        if ((arg1 != NULL) && (arg2 != NULL)) {
                            unsigned int tag_level = xlog_string2level(arg2);
                        
                            xlog_filter_set(flist, arg1, tag_level);
                        }
                    }
                    else {
                        /* FIXME: ? */
                    }
                }
                free(bufptr);
            }
            else {
                break;
            }
        }
        
    }

  error:
    return flist;
}

static const char *level2s(uint32_t level, const char *default_string)
{
    switch (level & 0xf) {

    case ANDROID_LOG_DEFAULT:
    case ANDROID_LOG_VERBOSE:
        return "verbose";

    case ANDROID_LOG_DEBUG:
        return "debug";

    case ANDROID_LOG_INFO:
        return "info";

    case ANDROID_LOG_WARN:
        return "warn";

    case ANDROID_LOG_ERROR:
        return "error";

    case ANDROID_LOG_FATAL:
        return "assert";

    case ANDROID_LOG_SILENT:
        return "off";

    default:
        return default_string;
    }
}

static const char *xlog_sublevel2str(char *s, uint32_t level, const char *xlog_def, const char *log_def) 
{
    uint32_t xlog_level = (level >> 4) & 0xf, log_level = level & 0xf;
    if (xlog_level == log_level) {
        sprintf(s, "%s", level2s(log_level, log_def));
    }
    else {
        sprintf(s, "%s+%s", level2s(xlog_level, xlog_def), level2s(log_level, log_def));
    }
    return s;
}

const char *xlog_level2str(char *levelstr, uint32_t level)
{
    char jl[32], nl[32], kl[32];

    xlog_sublevel2str(jl, (level >> 16) & 0xff, "verbose", "verbose");
    xlog_sublevel2str(nl, (level >> 8) & 0xff, "debug", "verbose");
    xlog_sublevel2str(kl, level & 0xff, "verbose", "verbose");

    if ((strcmp(jl, nl) == 0) && (strcmp(nl, kl) == 0)) {
        sprintf(levelstr, "%s", jl);
    }
    else {
        sprintf(levelstr, "%s/%s/%s", jl, nl, kl);
    }
    return levelstr;
}

void xlog_filter_write(FILE *ffp, const struct xlog_filter_list *flist)
{
    char levelstr[128];

    fprintf(ffp, "%s\nALL\t%s\n", XLOGCMD_TAG, xlog_level2str(levelstr, flist->default_level));
    int i;
    for (i = 0; i < flist->count; i++) {
        fprintf(ffp, "TAG\t%s\t%s\n", flist->filters[i].tag, xlog_level2str(levelstr, flist->filters[i].level));
    }
}

void xlog_filter_dump(const struct xlog_filter_list *flist)
{
    char levelstr[128];

    printf("default level %08x,%s\n", flist->default_level, xlog_level2str(levelstr, flist->default_level));

    printf("count %d\n", flist->count);
    int index;
    for (index = 0; index < flist->count; index++) {
	printf("\ttag:\"%s\" level:%08x,%s\n", flist->filters[index].tag, flist->filters[index].level, xlog_level2str(levelstr, flist->filters[index].level));
    }
}

void xlog_filter_delete(struct xlog_filter_list *flist, const char *tag)
{
    int i;
    for (i = 0; i < flist->count; i++) {
        int cmp = strcmp(flist->filters[i].tag, tag);
        if (cmp == 0) {
            memmove(&flist->filters[i], &flist->filters[i + 1], (flist->count - i - 1) * sizeof(flist->filters[0]));
            flist->count--;
        }
    }
}

void xlog_filter_set(struct xlog_filter_list *flist, const char *tag, uint32_t level)
{
    int i;
    for (i = 0; i < flist->count; i++) {
        int cmp = strcmp(flist->filters[i].tag, tag);
        if (cmp == 0) {
            flist->filters[i].level = level;
            return;
        }
        if (cmp > 0) {
            /* FIXME: Check out of range */
            memmove(&flist->filters[i + 1], &flist->filters[i], (flist->count - i) * sizeof(flist->filters[0]));
			if(strlen(tag) > sizeof(flist->filters[i].tag)) {
				strncpy(flist->filters[i].tag, tag, sizeof(flist->filters[i].tag) -1);
			} else {
				strcpy(flist->filters[i].tag, tag);
			}
            flist->filters[i].level = level;
            flist->count++;
            return;
        }
    }
    /* Add to tail */
	if(strlen(tag) > sizeof(flist->filters[flist->count].tag)) {
		strncpy(flist->filters[flist->count].tag, tag, sizeof(flist->filters[flist->count].tag) -1);
	} else {
		strcpy(flist->filters[flist->count].tag, tag);
	}
    flist->filters[flist->count].level = level;
    flist->count++;
    
}

