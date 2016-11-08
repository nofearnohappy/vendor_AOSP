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

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arAutoTestCmdTbl [] =
{
	{ (CHAR*)"handshake" , (CHAR*)"handshake" , _AutoDefault , NULL, (CHAR*)"Init handshake a test case", CLI_GUEST },
	{ (CHAR*)"getresult" , (CHAR*)"getresult" , _AutoDefault , NULL, (CHAR*)"Get result after test case", CLI_GUEST },		
	{ (CHAR*)"quit" , (CHAR*)"quit" , _AutoDefault , NULL, (CHAR*)"Quit autotest", CLI_GUEST },			
	{ (CHAR*)"u3handshake" , (CHAR*)"u3handshake" , _AutoDefault , NULL, (CHAR*)"u3 auto test POC", CLI_GUEST },			
	{ (CHAR*)"u3getresult" , (CHAR*)"u3getresult" , _AutoDefault , NULL, (CHAR*)"Quit autotest", CLI_GUEST },			
	{ (CHAR*)"u3quit" , (CHAR*)"u3quit" , _AutoDefault , NULL, (CHAR*)"Quit autotest", CLI_GUEST },	
	{ (CHAR*)"lbctrl" , (CHAR*)"lbctrl" , _AutoDefault , NULL, (CHAR*)"Ctrl transfer loopback", CLI_GUEST },	
	{ (CHAR*)"lb" , (CHAR*)"lb" , _AutoDefault , NULL, (CHAR*)"BULK/INTR/ISOC transfer loopback", CLI_GUEST },	
	{ (CHAR*)"lbscan" , (CHAR*)"lbscan" , _AutoDefault , NULL, (CHAR*)"BULK/INTR/ISOC transfer loopback scan all parameters", CLI_GUEST },	
	{ (CHAR*)"lbsg" , (CHAR*)"lbsg" , _AutoDefault , NULL, (CHAR*)"BULK transfer scatter-gather loopback", CLI_GUEST },	
	{ (CHAR*)"lbsgscan" , (CHAR*)"lbsgscan" , _AutoDefault , NULL, (CHAR*)"BULK transfer scatter-gather loopback scan all parameters", CLI_GUEST },	
	{ (CHAR*)"devrandomstop" , (CHAR*)"devrandomstop" , _AutoDefault , NULL, (CHAR*)"test device random stop function", CLI_GUEST },	
	{ (CHAR*)"randomsuspend" , (CHAR*)"randomsuspend" , _AutoDefault , NULL, (CHAR*)"randomly suspend for n times", CLI_GUEST },	
	{ (CHAR*)"randomwakeup" , (CHAR*)"randomwakeup" , _AutoDefault , NULL, (CHAR*)"randomly remote wakeup for n times", CLI_GUEST },		
	{ (CHAR*)"stress" , (CHAR*)"stress" , _AutoDefault , NULL, (CHAR*)"stress test", CLI_GUEST },			
	{ (CHAR*)"isofrm" , (CHAR*)"isofrm" , _AutoDefault , NULL, (CHAR*)"test iso transfer with frameid instead of SIA", CLI_GUEST },				
	{ (CHAR*)"conresume" , (CHAR*)"conresume" , _AutoDefault , NULL, (CHAR*)"concurrent resume", CLI_GUEST },					
	{ (CHAR*)"conu1u2" , (CHAR*)"conu1u2" , _AutoDefault , NULL, (CHAR*)"concurrent u1/u2 enter", CLI_GUEST }, 				
	{ (CHAR*)"conu1u2exit" , (CHAR*)"conu1u2exit" , _AutoDefault , NULL, (CHAR*)"concurrent u1/u2 exit", CLI_GUEST }, 					
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
