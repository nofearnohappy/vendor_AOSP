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

extern INT32 _DbgReadPCI(INT32 i4Argc, const CHAR **szArgv);
extern INT32 _DbgReadXHCI(INT32 i4Argc, const CHAR **szArgv);
extern INT32 _DbgDumpXHCIRegs(INT32 i4Argc, const CHAR **szArgv);

//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------


CLI_EXEC_T _arDbgCmdTbl [] =
{
	{ (CHAR*)"read_pci" , (CHAR*)"rpci" , _DbgReadPCI , NULL, (CHAR*)"read PCI memory", CLI_GUEST },
	{ (CHAR*)"read_xhci" , (CHAR*)"r" , _DbgReadPCI , NULL, (CHAR*)"read XHCI memory", CLI_GUEST },
	{ (CHAR*)"dump_xhci_regs" , (CHAR*)"dr" , _DbgDumpXHCIRegs , NULL, (CHAR*)"dump XHCI registers", CLI_GUEST },
	{ (CHAR*)"printportstatus" , (CHAR*)"portstatus" , _DbgDumpXHCIRegs , NULL, (CHAR*)"print current attached port status", CLI_GUEST },
	{ (CHAR*)"debug_slt_ctx" , (CHAR*)"dbgslt" , _DbgDumpXHCIRegs , NULL, (CHAR*)"print slot output context", CLI_GUEST },	
	{ (CHAR*)"printhccparams" , (CHAR*)"hccparams" , _DbgDumpXHCIRegs , NULL, (CHAR*)"print HCCPARAMS", CLI_GUEST }, 
	{ (CHAR*)"scheduling1" , (CHAR*)"sch1" , _DbgDumpXHCIRegs , NULL, (CHAR*)"Test NEC scheduling", CLI_GUEST }, 
	{ (CHAR*)"scheduling2" , (CHAR*)"sch2" , _DbgDumpXHCIRegs , NULL, (CHAR*)"Test NEC scheduling", CLI_GUEST }, 
	{ (CHAR*)"scheduling3" , (CHAR*)"sch3" , _DbgDumpXHCIRegs , NULL, (CHAR*)"Test NEC scheduling", CLI_GUEST }, 
	{ (CHAR*)"setpls" , (CHAR*)"setpls" , _DbgDumpXHCIRegs , NULL, (CHAR*)"set port pls value", CLI_GUEST }, 	
	{ (CHAR*)"setped" , (CHAR*)"setped" , _DbgDumpXHCIRegs , NULL, (CHAR*)"set port ped bit", CLI_GUEST }, 		
	{ (CHAR*)"portreset" , (CHAR*)"portreset" , _DbgDumpXHCIRegs , NULL, (CHAR*)"hot reset or warm reset a port", CLI_GUEST },
	{ (CHAR*)"mdelay" , (CHAR*)"mdelay" , _DbgDumpXHCIRegs , NULL, (CHAR*)"delay msecs", CLI_GUEST }, 			
	{ (CHAR*)"sifinit" , (CHAR*)"sifinit" , _DbgDumpXHCIRegs , NULL, (CHAR*)"init sif", CLI_GUEST }, 			
	{ (CHAR*)"u3w" , (CHAR*)"u3w" , _DbgDumpXHCIRegs , NULL, (CHAR*)"write phy register", CLI_GUEST }, 			
	{ (CHAR*)"u3r" , (CHAR*)"u3r" , _DbgDumpXHCIRegs , NULL, (CHAR*)"read phy register", CLI_GUEST }, 				
	{ (CHAR*)"u3i" , (CHAR*)"u3i" , _DbgDumpXHCIRegs , NULL, (CHAR*)"init u3 A60802 phy setting", CLI_GUEST },	
	{ (CHAR*)"u3ic" , (CHAR*)"u3ic" , _DbgDumpXHCIRegs , NULL, (CHAR*)"init u3 C60802 phy setting", CLI_GUEST },		
	{ (CHAR*)"u3" , (CHAR*)"u3" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u3 A60802 phy calibration", CLI_GUEST },		
	{ (CHAR*)"u3c" , (CHAR*)"u3c" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u3 C60802 phy calibration", CLI_GUEST },			
	{ (CHAR*)"u3eyescan" , (CHAR*)"u3eyescan" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u3 phy eyescan", CLI_GUEST },			
	{ (CHAR*)"mw" , (CHAR*)"mw" , _DbgDumpXHCIRegs , NULL, (CHAR*)"memory write", CLI_GUEST },	
	{ (CHAR*)"mr" , (CHAR*)"mr" , _DbgDumpXHCIRegs , NULL, (CHAR*)"memory read", CLI_GUEST },	
	{ (CHAR*)"keyboard" , (CHAR*)"kb" , _DbgDumpXHCIRegs , NULL, (CHAR*)"keyboard class test", CLI_GUEST }, 
	{ (CHAR*)"sch" , (CHAR*)"sch" , _DbgDumpXHCIRegs , NULL, (CHAR*)"verify scheduler algorithm", CLI_GUEST }, 
	{ (CHAR*)"ewe" , (CHAR*)"ewe" , _DbgDumpXHCIRegs , NULL, (CHAR*)"verify ewe register", CLI_GUEST }, 	
	{ (CHAR*)"u2t" , (CHAR*)"u2t" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u2 test mode", CLI_GUEST }, 	
	{ (CHAR*)"u2ct" , (CHAR*)"u2ct" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u2 electric compliance test", CLI_GUEST }, 		
	{ (CHAR*)"u3lect" , (CHAR*)"u3lect" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u3 compliance test - lecroy", CLI_GUEST }, 	
	{ (CHAR*)"u3elct" , (CHAR*)"u3elct" , _DbgDumpXHCIRegs , NULL, (CHAR*)"u3 compliance test - ellisys", CLI_GUEST }, 		
	{ (CHAR*)"ll" , 	(CHAR*)"ll" , 	_DbgDumpXHCIRegs , NULL, (CHAR*)"debug log level", CLI_GUEST }, 	
    { (CHAR*)NULL , (CHAR*)NULL , NULL, NULL, (CHAR*) NULL, CLI_GUEST }
};

CLI_EXEC_T _rDbgCmdTbl =
{
    (CHAR*)"debug commands", (CHAR*)"dbg", NULL, _arDbgCmdTbl, (CHAR*)"debug commands for driver development", CLI_GUEST,
};

CLI_EXEC_T* GetDbgCmdTbl(void){
    return &_rDbgCmdTbl;
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
