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
 * Copyright (c) 2009, MediaTek, Inc.
 * All rights reserved.
 *
 * Unauthorized use, practice, perform, copy, distribution, reproduction,
 * or disclosure of this information in whole or in part is prohibited.
 *-----------------------------------------------------------------------------
 *
 *
 *---------------------------------------------------------------------------*/

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------

#include "cli.h"

//-----------------------------------------------------------------------------
// Configurations
//-----------------------------------------------------------------------------

extern INT32 _AutoDefault(INT32 i4Argc, const CHAR **szArgv);
extern INT32 _FsgDefault(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arAutoTestCmdTbl [] =
{
	{ (CHAR*)"dev" , (CHAR*)"dev" , _AutoDefault , NULL, (CHAR*)"enter device test mode", CLI_GUEST },
	{ (CHAR*)"u3i" , (CHAR*)"u3i" , _AutoDefault , NULL, (CHAR*)"initialize phy module", CLI_GUEST },
	{ (CHAR*)"u3w" , (CHAR*)"u3w" , _AutoDefault , NULL, (CHAR*)"write phy register", CLI_GUEST },
	{ (CHAR*)"u3r" , (CHAR*)"u3r" , _AutoDefault , NULL, (CHAR*)"read phy register", CLI_GUEST },
	{ (CHAR*)"u3d" , (CHAR*)"u3d" , _AutoDefault , NULL, (CHAR*)"PIPE phase scan", CLI_GUEST },
	{ (CHAR*)"link" , (CHAR*)"link" , _AutoDefault , NULL, (CHAR*)"u3 device link up", CLI_GUEST },
	{ (CHAR*)"eyeinit" , (CHAR*)"eyeinit" , _AutoDefault , NULL, (CHAR*)"initialize eyescan", CLI_GUEST },	
	{ (CHAR*)"eyescan" , (CHAR*)"eyescan" , _AutoDefault , NULL, (CHAR*)"eyescan", CLI_GUEST },	
	{ (CHAR*)"stop" , (CHAR*)"stop" , _AutoDefault , NULL, (CHAR*)"Call TS_AUTO_TEST_STOP", CLI_GUEST },
	{ (CHAR*)"otg" , (CHAR*)"otg" , _AutoDefault , NULL, (CHAR*)"call otg_top", CLI_GUEST },
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rAutoTestCmdTbl =
{
    (CHAR*)"auto test management", (CHAR*)"auto", NULL, _arAutoTestCmdTbl, (CHAR*)"Auto test configuration", CLI_GUEST,
};

CLI_EXEC_T* GetAutoTestCmdTbl(void){
    return &_rAutoTestCmdTbl;
}



//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arFsgTestCmdTbl [] =
{
	{ (CHAR*)"init" , (CHAR*)"init" , _FsgDefault , NULL, (CHAR*)"start to run normal driver", CLI_GUEST },
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};



CLI_EXEC_T _rFsgTestCmdTbl =
{
    (CHAR*)"musb driver initialization", (CHAR*)"fsg", NULL, _arFsgTestCmdTbl, (CHAR*)"Start musb driver", CLI_GUEST,
};

CLI_EXEC_T* GetFsgTestCmdTbl(void){
    return &_rFsgTestCmdTbl;
}



//-----------------------------------------------------------------------------
// Macro definitions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// extern variables
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
// extern functions
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------- 
/** 
 *  @param  
 *  @retval   
 */
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// public functions
//-----------------------------------------------------------------------------
// 
