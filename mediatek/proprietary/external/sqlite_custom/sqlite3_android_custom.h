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

#ifndef SQLITE3_CUSTOM_H
#define SQLITE3_CUSTOM_H

#include <sqlite3.h>

namespace android {

/**
 * add for CT NEW FEATURE to show items follow order 09-004
 */
#define DS_FULL_INITIALS_MATCH          20
#define DS_FULL_FULLSPEL_MATCH          16

#define DS_INITIALS_MATCH               17
#define DS_FULLSPELL_MATCH              15
#define DS_NUMBER_MATCH                 13
#define DS_MIXED_MATCH                  11
#define DS_NOT_MATCH                    0

/**
* STATIC VARIABLE
*    NAME_MATCH_DEFAULT_TYPE
*
* DESCRIPTION
*   Flags indicate default name matching types which can be chosed by customer
*                       The valid values are 0,1,2.
*                       If 0, the origin match method
*                       If 1, an accurate matching type
*                       If 2, inputs with mixed order also match contacts
*/
#define NAME_MATCH_TYPE_ORIGIN          0
#define NAME_MATCH_TYPE_MIX_ORDER       2

/**
* STATIC VARIABLE
*    NUMBER_MATCH_DEFAULT_TYPE
*
* DESCRIPTION
*   Flags indicate default number matching types which can be chosed by customer
*                       The valid values are 0,1.
*                       If 0, a number matching function from the begginning
*                       If 1, a number matching function with middle match
*/
#define NUMBER_MATCH_TYPE_MIDDLE        1


/**
 * FUNCTION POINTER
 *  FUNC_MATCH
 *
 * DESCRIPTION
 *  Function pointer for dialer search algorithm. It is used to do matching and
 *  get the matched positions and matched count, which are used to highlight
 *  matched characters.
 *  Search resource can be names or numbers.
 *  The matching rules should be consistent with the rules used in FUNC_FILTER.
 *
 * PARAMETERS:
 *  searchKey           [IN]  user input string.
 *  matchText          	[IN]  search key contain contact name or phone number
 *  matchOffset         [IN]  name character offset in the corresponding string
 *  						  NULL if the matchType is DS_SEARCH_TYPE_NUMBER.
 *  matchType         	[IN]  A flag indicates the search is to query names or numbers.
 *                            Its value should be DS_SEARCH_TYPE_NAME or DS_SEARCH_TYPE_NUMBER,
 *                            which are defined in DialerSearchUtils.h.
 *  res      			[OUT] matched positions sequence
 *  resLen				[OUT] this count of matched substrings
 *
 * RETURNS:
 *  DS_RET_OK           Defined in DialerSearchUtils.h, its values is 0.
 *                      If get matched results successfully, return DS_RET_OK.
 *  DS_RET_ERR          Defined in DialerSearchUtils.h, its values is -1.
 *                      If error happens when to get matched results, return DS_RET_ERR.
 *  DS_RET_NO_RESULT    Defined in DialerSearchUtils.h, its values is -2.
 *                      If there is no matched result, return this DS_RET_NO_RESULT.
 */
typedef int (*FUNC_MATCH) (const char *searchKey, \
		const char *matchText, \
		const char *matchOffset, \
		int matchType,  \
		char *res, \
		int *resLen);
/**
 * FUNCTION POINTER
 *  FUNC_FILTER
 *
 * DESCRIPTION
 *  Function pointer for dialer search algorithm. It is used to do matching and
 *  filter the un-matched results.
 *  Search resource can be names or numbers.
 *
 * PARAMETERS:
 *  searchKey           [IN]  user input string.
 *  matchText          	[IN]  search key contain contact name or phone number
 *  matchOffset         [IN]  name character offset in the corresponding string
 *  						  NULL if the matchType is DS_SEARCH_TYPE_NUMBER.
 *  matchType         	[IN]  A flag indicates the search is to query names or numbers.
 *                            Its value should be DS_SEARCH_TYPE_NAME or DS_SEARCH_TYPE_NUMBER,
 *                            which are defined in DialerSearchUtils.h.
 * RETURNS:
 *  DS_RET_OK           Defined in DialerSearchUtils.h, its values is 0.
 *                      If get matched results successfully, return DS_RET_OK.
 *  DS_RET_ERR          Defined in DialerSearchUtils.h, its values is -1.
 *                      If error happens when to get matched results, return DS_RET_ERR.
 *  DS_RET_NO_RESULT    Defined in DialerSearchUtils.h, its values is -2.
 *                      If there is no matched result, return this DS_RET_NO_RESULT.
 */
typedef int (*FUNC_FILTER) (const char *searchKey, \
		const char *matchText, \
		const char *matchOffset, \
		int matchType);
/**
 * FUNCTION POINTER
 *  FUNC_NAME_MATCH_PTR
 *
 * DESCRIPTION
 *  Function pointer for dialer search algorithm. It is used to do matching on
 *  contacts' names.
 *
 * PARAMETERS:
 *  searchKey           [IN]  user input string.
 *  matchText          	[IN]  search key contain contact name or phone number
 *  matchOffset         [IN]  name character offset in the corresponding string
 *  						  NULL if the matchType is DS_SEARCH_TYPE_NUMBER.
 *  matchType         	[IN]  A flag indicates the search is to query names or numbers.
 *                            Its value should be DS_SEARCH_TYPE_NAME or DS_SEARCH_TYPE_NUMBER,
 *                            which are defined in DialerSearchUtils.h.
 *  needResult			[IN]  A flag indicates whether this fuction should return matched info.
 *  						  If needResult is true, res and resLen must be filled.
 *  res      			[OUT] matched positions sequence
 *  resLen				[OUT] this count of matched substrings
 *
 *  RETURNS:
 *  DS_RET_OK           Defined in DialerSearchUtils.h, its values is 0.
 *                      If get matched results successfully, return DS_RET_OK.
 *  DS_RET_ERR          Defined in DialerSearchUtils.h, its values is -1.
 *                      If error happens when to get matched results, return DS_RET_ERR.
 *  DS_RET_NO_RESULT    Defined in DialerSearchUtils.h, its values is -2.
 *                      If there is no matched result, return this DS_RET_NO_RESULT.
 */
typedef int (*FUNC_NAME_MATCH_PTR)(const char *searchKey, \
		const char *matchText, \
		const char *matchOffset, \
		char *res, \
		int *resLen, \
		bool needResult);
/**
 * FUNCTION POINTER
 *  FUNC_NUMBER_MATCH_PTR
 *
 * DESCRIPTION
 *  Function pointer for dialer search algorithm. It is used to do matching on
 *  contacts' phone numbers.
 *
 * PARAMETERS:
 *  searchKey           [IN]  user input string.
 *  matchText          	[IN]  search key contain contact name or phone number
 *  needResult			[IN]  A flag indicates whether this fuction should return matched info.
 *  						  If needResult is true, res and resLen must be filled.
 *  res      			[OUT] matched positions sequence
 *  resLen				[OUT] this count of matched substrings
 *
 * RETURNS:
 *  DS_RET_OK           Defined in DialerSearchUtils.h, its values is 0.
 *                      If get matched results successfully, return DS_RET_OK.
 *  DS_RET_ERR          Defined in DialerSearchUtils.h, its values is -1.
 *                      If error happens when to get matched results, return DS_RET_ERR.
 *  DS_RET_NO_RESULT    Defined in DialerSearchUtils.h, its values is -2.
 *                      If there is no matched result, return this DS_RET_NO_RESULT.
 */
typedef int (*FUNC_NUMBER_MATCH_PTR)(const char *searchKey, \
		const char *matchText, \
		char *res, \
		int *resLen, \
		bool needResult);

/**
* STATIC VARIABLE
*    NAME_MATCH_DEFAULT_TYPE
*
* DESCRIPTION
*   Flags indicate default name matching types which can be chosed by customer
*                       The valid values are 0,1,2.
*                       If 0, the origin match method
*                       If 1, an accurate matching type
*                       If 2, inputs with mixed order also match contacts
*/
//static int NAME_MATCH_TYPE_ORIGIN = 0;
//static int NAME_MATCH_TYPE_ACCURATE = 1;
//static int NAME_MATCH_TYPE_MIX_ORDER = 2;


/**
* STATIC VARIABLE
*    NUMBER_MATCH_DEFAULT_TYPE
*
* DESCRIPTION
*   Flags indicate default number matching types which can be chosed by customer
*                       The valid values are 0,1.
*                       If 0, a number matching function from the begginning
*                       If 1, a number matching function with middle match
*/
//static int NUMBER_MATCH_TYPE_ACCURATE = 0;
//static int NUMBER_MATCH_TYPE_MIDDLE = 1;


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
int get_name_match_default_type();

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
int get_number_match_default_type();

/**
 * FUNCTION
 *  get_name_match_function
 *
 * DESCRIPTION
 * Return the custom defined name_matching function.
 *
 * PARAMETERS:
 * void
 *
 * RETURNS:
 *  FUNC_NAME_MATCH_PTR the PTR of the name mutch function
 */
FUNC_NAME_MATCH_PTR get_name_match_function();

/**
 * FUNCTION
 *  get_number_match_function
 *
 * DESCRIPTION
 * Return the custom defined number_matching function.
 * Define the function as doCustomerNumberMatchExample().
 *
 * PARAMETERS:
 * void
 *
 * RETURNS:
 *  FUNC_NUMBER_MATCH_PTR the PTR of the number mutch function
 */
FUNC_NUMBER_MATCH_PTR get_number_match_function();

/**
 * FUNCTION
 *  get_dialer_search_match_function
 *
 * DESCRIPTION
 *  It is a higher level function than "get_name_match_function",
 *  which define the match rules of dialer search, including Number match and Name match rules.
 *
 * PARAMETERS:
 *  void
 *
 * RETURNS:
 *  FUNC_MATCH
 */
FUNC_MATCH get_dialer_search_match_function();

/**
 * FUNCTION
 *  get_dialer_search_filter_function
 *
 * DESCRIPTION
 *  It is a higher level function than "get_name_match_function",
 *  which return the match offsets of the dialer search,
 *  including Number matched offsets and Name matched offsets.
 *
 * PARAMETERS:
 *  void
 *
 * RETURNS:
 *  FUNC_FILTER
 */
FUNC_FILTER get_dialer_search_filter_function();


/**
 * FUNCTION
 *  register_customer_dialer_search_functions
 *
 * DESCRIPTION
 *  This function is used to register original SQLite3 functions for dialer search.
 *  which should be sync with the high level java code.
 *
 * PARAMETERS:
 *  handle           [IN] A pinter connects to a SQLite3 instance.
 *
 * RETURNS:
 *  int
 */
int  register_customer_dialer_search_functions(sqlite3 * handle);

}//android namespace

#endif

