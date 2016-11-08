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

extern INT32 _RingDefault(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arRingTestCmdTbl [] =
{
	{ (CHAR*)"erfull" , (CHAR*)"erfull" , _RingDefault , NULL, (CHAR*)"Test event ring full error", CLI_GUEST },
	{ (CHAR*)"stopcmd" , (CHAR*)"stopcmd" , _RingDefault , NULL, (CHAR*)"Stop command ring", CLI_GUEST },
	{ (CHAR*)"abortcmd" , (CHAR*)"abortcmd" , _RingDefault , NULL, (CHAR*)"Abort command ring", CLI_GUEST },
	{ (CHAR*)"stopep" , (CHAR*)"stopep" , _RingDefault , NULL, (CHAR*)"Stop ep ring", CLI_GUEST },
	{ (CHAR*)"rrd" , (CHAR*)"rrd" , _RingDefault , NULL, (CHAR*)"Add a random ring doorbell thread", CLI_GUEST },
	{ (CHAR*)"rstp" , (CHAR*)"rstp" , _RingDefault , NULL, (CHAR*)"Add a random stop ep thread", CLI_GUEST },	
	{ (CHAR*)"enlarge" , (CHAR*)"enlarge" , _RingDefault , NULL, (CHAR*)"Enlarge a ep ring", CLI_GUEST },
	{ (CHAR*)"shrink" , (CHAR*)"shrink" , _RingDefault , NULL, (CHAR*)"Shrink a ep ring", CLI_GUEST },
	{ (CHAR*)"intrmod" , (CHAR*)"intrmod" , _RingDefault , NULL, (CHAR*)"Test interrupt moderation", CLI_GUEST },
	{ (CHAR*)"bei" , (CHAR*)"bei" , _RingDefault , NULL, (CHAR*)"Test bei of TRB with normal bulk transfer", CLI_GUEST },	
	{ (CHAR*)"idt" , (CHAR*)"idt" , _RingDefault , NULL, (CHAR*)"Test idt of TRB with normal bulk transfer", CLI_GUEST },		
	{ (CHAR*)"noop" , (CHAR*)"noop" , _RingDefault , NULL, (CHAR*)"Test noop transfer trb", CLI_GUEST },		
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rRingTestCmdTbl =
{
    (CHAR*)"ring mgt function", (CHAR*)"ring", NULL, _arRingTestCmdTbl, (CHAR*)"Ring mgt function management", CLI_GUEST,
};

CLI_EXEC_T* GetRingTestCmdTbl(void){
    return &_rRingTestCmdTbl;
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
