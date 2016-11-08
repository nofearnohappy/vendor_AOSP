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
 *
 *---------------------------------------------------------------------------*/

/** @file cli_modulecmd.c
 *  Add your description here.
 */

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------
#include "cli.h"
#include "assert.h"

//-----------------------------------------------------------------------------
// Configurations
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

// cli module table
extern CLI_EXEC_T* GetHcdTestCmdTbl(void);
extern CLI_EXEC_T* GetSlotTestCmdTbl(void);
extern CLI_EXEC_T* GetLoopTestCmdTbl(void);
extern CLI_EXEC_T* GetPowerTestCmdTbl(void);
extern CLI_EXEC_T* GetAutoTestCmdTbl(void);
extern CLI_EXEC_T* GetRingTestCmdTbl(void);
extern CLI_EXEC_T* GetStressTestCmdTbl(void);
extern CLI_EXEC_T* GetHubTestCmdTbl(void);
extern CLI_EXEC_T* GetDbgCmdTbl(void);
extern CLI_EXEC_T* GetDevTestCmdTbl(void);
extern CLI_EXEC_T* GetOtgTestCmdTbl(void);

CLI_GET_CMD_TBL_FUNC _pfCliGetTbl[] =
{
	GetHcdTestCmdTbl,
	GetSlotTestCmdTbl,
	GetLoopTestCmdTbl,
	GetPowerTestCmdTbl,
	GetAutoTestCmdTbl,
	GetRingTestCmdTbl,
	GetStressTestCmdTbl,
	GetDbgCmdTbl,
	GetHubTestCmdTbl,
	GetDevTestCmdTbl,
	GetOtgTestCmdTbl
};

#define CLI_MOD_NS (sizeof(_pfCliGetTbl)/sizeof(CLI_GET_CMD_TBL_FUNC))

static CLI_EXEC_T _rNullTbl = {NULL, NULL, NULL, NULL, NULL, CLI_SUPERVISOR};
static CLI_EXEC_T _arUserCmdTbl[CLI_MOD_NS + 1];

//-----------------------------------------------------------------------------
// Macro definitions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Static variables
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// global variables
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
// Imported variables
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Imported functions
//-----------------------------------------------------------------------------



//-----------------------------------------------------------------------------
// Static functions
//-----------------------------------------------------------------------------




//----------------------------------------------------------------------------- 
/** 
 * 
 *  @param  
 *  @retval   
 */
//-----------------------------------------------------------------------------



//-----------------------------------------------------------------------------
// public functions
//-----------------------------------------------------------------------------
// 
void CLI_ModuleCmd_Install()
{
    UINT32 u4Idx;
    UINT32 u4CmdIdx;
    CLI_EXEC_T* prModCmdTbl;

    // initialize module command table
    for (u4Idx = 0; u4Idx < (UINT32)(CLI_MOD_NS + 1); u4Idx++)
    {
        _arUserCmdTbl[u4Idx] = _rNullTbl;
    }

    // install module command table
    u4CmdIdx = 0;
    for (u4Idx = 0; u4Idx < (UINT32)CLI_MOD_NS; u4Idx++)
    {
//        ASSERT(_pfCliGetTbl[u4Idx] != NULL);
        assert(_pfCliGetTbl[u4Idx] != NULL);
        prModCmdTbl = _pfCliGetTbl[u4Idx]();

        if ((prModCmdTbl != NULL) &&
            (prModCmdTbl->pszCmdStr != NULL) &&
            ((prModCmdTbl->pfExecFun != NULL) || (prModCmdTbl->prCmdNextLevel != NULL)))
        {
            _arUserCmdTbl[u4CmdIdx] = *prModCmdTbl;
            u4CmdIdx++;
        }
    }

    CLI_CmdTblAttach(_arUserCmdTbl);
}
