/*----------------------------------------------------------------------------*
 * No Warranty                                                                *
 * Except as may be otherwise agreed to in writing, no warranties of any      *
 * kind, whether express or implied, are given by MTK with respect to any MTK *
 * Deliverables or any use thereof, and MTK Deliverables are provided on an   *
 * "AS IS" basis.  MTK hereby expressly disclaims all such warranties,        *
 * including any implied warranties of merchantability, non-infringement and  *
 * fitness for a particular purpose and any warranties arising out of course  *
 * of performance, course of dealing or usage of trade.  Parties further      *
 * acknowledge that Company may, either presently and/or in the future,       *
 * instruct MTK to assist it in the development and the implementation, in    *
 * accordance with Company's designs, of certain softwares relating to        *
 * Company's product(s) (the "Services").  Except as may be otherwise agreed  *
 * to in writing, no warranties of any kind, whether express or implied, are  *
 * given by MTK with respect to the Services provided, and the Services are   *
 * provided on an "AS IS" basis.  Company further acknowledges that the       *
 * Services may contain errors, that testing is important and Company is      *
 * solely responsible for fully testing the Services and/or derivatives       *
 * thereof before they are used, sublicensed or distributed.  Should there be *
 * any third party action brought against MTK, arising out of or relating to  *
 * the Services, Company agree to fully indemnify and hold MTK harmless.      *
 * If the parties mutually agree to enter into or continue a business         *
 * relationship or other arrangement, the terms and conditions set forth      *
 * hereunder shall remain effective and, unless explicitly stated otherwise,  *
 * shall prevail in the event of a conflict in the terms in any agreements    *
 * entered into between the parties.                                          *
 *---------------------------------------------------------------------------*/
/*-----------------------------------------------------------------------------
 * Copyright (c) 2008, MediaTek, Inc.
 * All rights reserved.
 *
 * Unauthorized use, practice, perform, copy, distribution, reproduction,
 * or disclosure of this information in whole or in part is prohibited.
 *-----------------------------------------------------------------------------
 *
 *---------------------------------------------------------------------------*/

/** @file cli.h
 *  Add your description here.
 */

#ifndef _CLI_H
#define _CLI_H

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------
//#include "kal_release.h"
#include "x_typedef.h"
#include "stdio.h"
#include "string.h"
#include "stdarg.h"
#include "assert.h"
// -----------------------------------------------------------------------------
// Configurations
//-----------------------------------------------------------------------------


#ifdef __cplusplus
extern "C" {
#endif

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------
#define CLI_INPUT_BUF_ROW_NUM					3				// number of DOS key row
//#define CLI_INPUT_BUF_ROW_NUM					1				// number of DOS key row, jackson: now only use one row
#define CLI_INPUT_BUF_SIZE						256				// number of character per row
                                                                //
#define CLI_INPUT_MAX_CMD_TBL_NUM				4				// maximum number of attach command root table

#define CLI_PROMPT_STR							"U_CLI>"			// prompt string of CLI

// ASCII key define
#define ASCII_NULL								0x00
#define ASCII_KEY_BS							0x08
#define ASCII_KEY_NL							0x0A
#define ASCII_ENTER								0x0D
#define ASCII_KEY_CR							0x0D
#define ASCII_KEY_ESC							0x1B
#define ASCII_KEY_SPACE							0x20
#define ASCII_KEY_DBL_QUOTE						0x22
#define ASCII_KEY_QUOTE							0x27
#define ASCII_KEY_DOT							0x2e
#define ASCII_KEY_DOLLAR						0x24
#define ASCII_KEY_UP							0x41
#define ASCII_KEY_DOWN							0x42
#define ASCII_KEY_RIGHT							0x43
#define ASCII_KEY_LEFT							0x44
#define ASCII_KEY_ARROW							0x5B
#define ASCII_KEY_ROOT							0x5c

#define ASCII_KEY_PRINTABLE_MIN					0x20
#define ASCII_KEY_PRINTABLE_MAX					0x7E

#define IsPrintable(c) ((((c) > ASCII_NULL) && ((c) < ASCII_KEY_SPACE)) ? 0 : 1)
#define IsSpace(c) (((c)==' ') || ((c)=='\n') || ((c)=='\t') || ((c)=='\r') || ((c)=='\a'))
#define IsDot(c) (((c) == ASCII_KEY_DOT) ? 1 : 0)
#define IsRoot(c) (((c) == ASCII_KEY_ROOT) ? 1 : 0)

#define CLI_MAX_ARGU							20				// maximum number of argument
#define CLI_MAX_ARGU_LEN						256				//length of each argument, modify 32 to 256, due to argument need fit http url  length
                                                                // length of each argument
#define CLI_MANDA_CMD_TBL_IDX					0				// mandatory command root table index
#define CLI_MAX_CMD_TBL_LEVEL					8				// maximum level of command table
//-----------------------------------------------------------------------------
// Type definitions
//-----------------------------------------------------------------------------
//#define CLI_COMMAND_OK					0
#define CLI_UNKNOWN_CMD					-2147483647

/******************************************************************************
* cli command access right
******************************************************************************/
typedef enum
{
	CLI_SUPERVISOR = 0,
	CLI_ADMIN,
	CLI_GUEST,
	CLI_HIDDEN
} CLI_ACCESS_RIGHT_T;


/******************************************************************************
* cli command structure
******************************************************************************/
typedef struct _CLI_EXEC
{
	CHAR*				pszCmdStr;													// command string
	CHAR*				pszCmdAbbrStr;												// command abbreviation
	INT32				(*pfExecFun) (INT32 i4Argc, const CHAR ** szArgv);			// execution function
	struct _CLI_EXEC	*prCmdNextLevel;											// next level command table
	CHAR*				pszCmdHelpStr;												// command description string
	CLI_ACCESS_RIGHT_T	eAccessRight;												// command access right
} CLI_EXEC_T;

//-----------------------------------------------------------------------------
// Macro definitions
//-----------------------------------------------------------------------------
typedef enum _ENUM_CLI_ERR_CODE_T
{
    CLI_COMMAND_OK = 0,
    //E_CLI_COMMAND_OK = 0,
    E_CLI_ERR_GENERAL = -1,
}
ENUM_CLI_ERR_CODE_T;
//-----------------------------------------------------------------------------
// Public functions
//-----------------------------------------------------------------------------
extern INT32 CLI_CmdTblAttach(CLI_EXEC_T* pTbl);
//jackson temp remove extern CLI_EXEC_T* CLI_GetDefaultCmdTbl(void);


typedef CLI_EXEC_T* (*CLI_GET_CMD_TBL_FUNC)(void);

#define cli_print printf
//#define cli_print(x) printf(x)
//do not porting alias related code
/*
// CLI Alias Related

#define CLI_ALIAS_NUM							0x10			// maximum number of alias support
#define CLI_ALIAS_SIZE							0x10			// maximum number of alias characters

#define CLI_ALIAS_CMD_STR						"alias"
#define CLI_ALIAS_CMD_ABBR_STR					"a"

extern void CLI_AliasInit(void);
extern const CHAR* CLI_AliasCompare(const CHAR* szAlias);
extern INT32 CLI_CmdAlias(INT32 i4Argc, const CHAR ** szArgv);
*/
#endif /* _CLI_H */

#ifdef __cplusplus
}
#endif


