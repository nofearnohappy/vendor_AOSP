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

extern INT32 _OtgDefault(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

CLI_EXEC_T _arOtgTestCmdTbl [] =
{
 { (CHAR*)"deva" , (CHAR*)"deva" , _OtgDefault , NULL, (CHAR*)"OTG host attach as A device", CLI_GUEST },
 { (CHAR*)"devb" , (CHAR*)"devb" , _OtgDefault , NULL, (CHAR*)"OTG host attach as B device", CLI_GUEST },
 { (CHAR*)"srp" , (CHAR*)"srp" , _OtgDefault , NULL, (CHAR*)"OTG test SRP", CLI_GUEST },
 { (CHAR*)"hnpa" , (CHAR*)"hnpa" , _OtgDefault , NULL, (CHAR*)"OTG host is hnp to become device", CLI_GUEST },
 { (CHAR*)"hnpabackh" , (CHAR*)"hnpabackh" , _OtgDefault , NULL, (CHAR*)"OTG host is back", CLI_GUEST },
 { (CHAR*)"hnpb" , (CHAR*)"hnpb" , _OtgDefault , NULL, (CHAR*)"OTG b device hnp to become host", CLI_GUEST },
 { (CHAR*)"hnpbbackd" , (CHAR*)"hnpbbackd" , _OtgDefault , NULL, (CHAR*)"OTG device is back", CLI_GUEST },
	{ (CHAR*)"uuta" , (CHAR*)"uuta" , _OtgDefault , NULL, (CHAR*)"OTG OPT UUT-A", CLI_GUEST },
	{ (CHAR*)"uutb" , (CHAR*)"uutb" , _OtgDefault , NULL, (CHAR*)"OTG OPT UUT-B", CLI_GUEST },
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rOtgTestCmdTbl =
{
    (CHAR*)"Otg test", (CHAR*)"otg", NULL, _arOtgTestCmdTbl, (CHAR*)"OTG test cases", CLI_GUEST,
};

CLI_EXEC_T* GetOtgTestCmdTbl(void){
    return &_rOtgTestCmdTbl;
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


