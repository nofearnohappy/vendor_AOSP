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

extern INT32 _SlotDefault(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arSlotTestCmdTbl [] =
{
//	{ (CHAR*)"address" , (CHAR*)"add" , _SlotDefault , NULL, (CHAR*)"attach and address device", CLI_GUEST },
	{ (CHAR*)"configure" , (CHAR*)"config" , _SlotDefault , NULL, (CHAR*)"attach and configure device", CLI_GUEST },
	{ (CHAR*)"disconnect" , (CHAR*)"discon" , _SlotDefault , NULL, (CHAR*)"disconnect, disable slot", CLI_GUEST },
	{ (CHAR*)"configep" , (CHAR*)"configep" , _SlotDefault , NULL, (CHAR*)"config a bulk endpoint", CLI_GUEST },
	{ (CHAR*)"resetslot" , (CHAR*)"reset" , _SlotDefault , NULL, (CHAR*)"reset device slot", CLI_GUEST },
	{ (CHAR*)"resetport" , (CHAR*)"resetp" , _SlotDefault , NULL, (CHAR*)"reset port", CLI_GUEST },
	{ (CHAR*)"reconfigslot" , (CHAR*)"reconfig" , _SlotDefault , NULL, (CHAR*)"reconfig slot", CLI_GUEST },
	{ (CHAR*)"enableslot" , (CHAR*)"enable" , _SlotDefault , NULL, (CHAR*)"attache, reset and enable slot", CLI_GUEST },
	{ (CHAR*)"disableslot" , (CHAR*)"disable" , _SlotDefault , NULL, (CHAR*)"disable slot", CLI_GUEST },
	{ (CHAR*)"addressslot" , (CHAR*)"addslt" , _SlotDefault , NULL, (CHAR*)"address an enabled slot", CLI_GUEST },	
	{ (CHAR*)"getdescriptor" , (CHAR*)"getdesc" , _SlotDefault , NULL, (CHAR*)"Do get_descriptor ctrl transfer", CLI_GUEST },	
	{ (CHAR*)"getbos" , (CHAR*)"getbos" , _SlotDefault , NULL, (CHAR*)"Do get_descriptor ctrl request of BOS descriptor", CLI_GUEST },	
	{ (CHAR*)"setconfiguration" , (CHAR*)"setconf" , _SlotDefault , NULL, (CHAR*)"Do set configuration", CLI_GUEST },		
	{ (CHAR*)"setu1u2enable" , (CHAR*)"setu1u2" , _SlotDefault , NULL, (CHAR*)"set feature - u1 enable, u2 enable", CLI_GUEST },			
	{ (CHAR*)"getdevstatus" , (CHAR*)"devstat" , _SlotDefault , NULL, (CHAR*)"Get status - device status", CLI_GUEST },	
	{ (CHAR*)"evaluatecontext" , (CHAR*)"evalctx" , _SlotDefault , NULL, (CHAR*)"evaluate context test", CLI_GUEST },				
	{ (CHAR*)"ped" , (CHAR*)"ped" , _SlotDefault , NULL, (CHAR*)"test port enable/disable(ped) register", CLI_GUEST },					
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rSlotTestCmdTbl =
{
    (CHAR*)"slog_management", (CHAR*)"slt", NULL, _arSlotTestCmdTbl, (CHAR*)"slot managements", CLI_GUEST,
};

CLI_EXEC_T* GetSlotTestCmdTbl(void){
    return &_rSlotTestCmdTbl;
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
