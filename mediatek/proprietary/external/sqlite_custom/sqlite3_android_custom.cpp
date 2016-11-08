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

#define LOG_TAG "sqlite3_custom"

#include <ctype.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <cutils/log.h>

#include "sqlite3_android_custom.h"

namespace android {

/**
 * FUNCTION
 *  get_name_match_default_type
 *
 * DESCRIPTION
 * Return the default name match type chosed by customer;
 *
 * PARAMETERS:
 * void
 *
 * RETURNS:
 *  int  the  name match type value
 */
int get_name_match_default_type() {return NAME_MATCH_TYPE_ORIGIN;}

/**
 * FUNCTION
 *  get_number_match_default_type
 *
 * DESCRIPTION
 * Return the default number match type chosed by customer;
 *
 * PARAMETERS:
 * void
 *
 * RETURNS:
 *  int  the  number match type value
 */
int get_number_match_default_type() {return NUMBER_MATCH_TYPE_MIDDLE;}

/********Define the Custom Functions As Below********/
/**
 * FUNCTION
 *  doCustomerNumberMatchExample
 * DESCRIPTION
 *  number matching function which supports substring matching
 */
int doCustomerNumberMatchExample(const char *searchKey,
        const char *matchText, char *resMatchOffset, int *count,
        bool needResult) {
    int searchKeyLen = strlen(searchKey);
    int matchTextLen = strlen(matchText);
    int subMatchIdx = 0;
    char * matchStrPos = 0;
    if (matchText == NULL || searchKeyLen > matchTextLen) {
        return -2;
    }
    if (NULL != (matchStrPos = strstr(matchText, searchKey))) {
        if (needResult) {
            //The result should be set correctly, Otherwise the dialersearch highlight word will be wrong.
            subMatchIdx = 0;
            resMatchOffset[0] = (char) DS_NUMBER_MATCH;
            resMatchOffset[3]
                    = (char) subMatchIdx;
            resMatchOffset[4]
                    = (char) subMatchIdx + searchKeyLen - 1;
            resMatchOffset[5]
                    = (char) searchKeyLen;
            *count = 2;
        }
        return 0;
    }
    return -2;
}

/********Define the Custom Functions End********/

/********Return the Custom Functions As Below********/
FUNC_NUMBER_MATCH_PTR get_number_match_function()
{
    //return doCustomerNumberMatchExample;
    return 0;
}

FUNC_NAME_MATCH_PTR get_name_match_function()
{
    return 0;
}

FUNC_MATCH get_dialer_search_match_function()
{
    return 0;
}

FUNC_FILTER get_dialer_search_filter_function()
{
    return 0;
}

/********Define the Original Sqlite Function********/
int register_customer_dialer_search_functions(sqlite3 * handle)
{
    return SQLITE_ERROR;
}

} //android namespace

