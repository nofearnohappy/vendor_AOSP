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

extern INT32 _PowerDefault(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arPowerTestCmdTbl [] =
{
	{ (CHAR*)"suspend" , (CHAR*)"suspend" , _PowerDefault , NULL, (CHAR*)"Suspend device to U3 state", CLI_GUEST },
	{ (CHAR*)"suspendport" , (CHAR*)"suspendp" , _PowerDefault , NULL, (CHAR*)"Suspend device by port id to U3 state", CLI_GUEST },
	{ (CHAR*)"resume" , (CHAR*)"resume" , _PowerDefault , NULL, (CHAR*)"Resume device to U0 state", CLI_GUEST },
	{ (CHAR*)"resumeport" , (CHAR*)"resumep" , _PowerDefault , NULL, (CHAR*)"Resume device by port id to U0 state", CLI_GUEST },
	{ (CHAR*)"wakeup" , (CHAR*)"wakeup" , _PowerDefault , NULL, (CHAR*)"Remote wakeup", CLI_GUEST },	
	{ (CHAR*)"u1u2" , (CHAR*)"u1u2" , _PowerDefault , NULL, (CHAR*)"Let USB3 device enter U1/U2 step", CLI_GUEST },	
	{ (CHAR*)"fla" , (CHAR*)"fla" , _PowerDefault , NULL, (CHAR*)"Transmit FLA LMP, force low power accept", CLI_GUEST },	
	{ (CHAR*)"occ" , (CHAR*)"occ" , _PowerDefault , NULL, (CHAR*)"test over-current change register", CLI_GUEST },		
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rPowerTestCmdTbl =
{
    (CHAR*)"low power management", (CHAR*)"pw", NULL, _arPowerTestCmdTbl, (CHAR*)"Low power management test", CLI_GUEST,
};

CLI_EXEC_T* GetPowerTestCmdTbl(void){
    return &_rPowerTestCmdTbl;
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
