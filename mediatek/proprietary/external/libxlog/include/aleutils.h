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

#if !defined( __ALEUTILS_H__)
#define __ALEUTILS_H__

#include <stdint.h>

#if defined(__cplusplus)
extern "C" {
#endif

#define ALE_C_PRINTF 0
#define ALE_KERNEL_PRINTF 1
#define ALE_JAVA_PRINTF 2

#include <stdarg.h>

struct ale_map;

union ale_printf_arg {
    int8_t s8_val;
    uint8_t u8_val;

    int16_t s16_val;
    uint16_t u16_val;

    int32_t s32_val;
    uint32_t u32_val;

    int64_t s64_val;
    uint64_t u64_val;

    char *string_val;
    void *ptr_val;

    double double_val;
    long double long_double_val;
};

struct ale_tag_entry {
    int count;
    const char *layer;
    int level;
    char *tag;

    char *filename;
};

struct ale_tag_stat {
    int count;
    
    struct ale_tag_entry *entries;
};

struct ale_map *ale_map_open(const char *filename);

int ale_map_translate(char **tag, char **msg, const struct ale_map *map, int type, const void *buf, int buflen);

void ale_map_close(struct ale_map *map);

void ale_map_dump_all(const struct ale_map *map);

void ale_map_dump_tag_all(const struct ale_map *map);

struct ale_tag_stat *ale_map_tag_stat(const struct ale_map *map);

void ale_tag_stat_free(struct ale_tag_stat *stat);

void ale_map_dump_message(const struct ale_map *map, const char *message_id);

int ale_b64_decode(char const *src, unsigned char *target, int targsize);

char *ale_unquote_string(const char *fmt);

char *arm_le_printf(char *(*ale_printf)(const char *, const union ale_printf_arg *), 
		    const char *fmt, const char *arglist, const void *argbuf, 
		    int argbuf_len);

char *ale_c_printf(const char *fmt0, const union ale_printf_arg *args);

char *ale_java_printf(const char *fmt0, const union ale_printf_arg *args);

char *ale_kernel_printf(const char *fmt0, const union ale_printf_arg *args);

char *ale_dtoa(double d, int mode, int ndigits, int *decpt, int *sign, char **rve);

const char *ale_convert_level_to_string(int level);

#if defined(__cplusplus)
};
#endif

#endif /* __ALEUTILS_H__ */
