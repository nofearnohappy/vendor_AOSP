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

#include <string.h>
#include "bootrom_stage.h"
#include "image.h"

#include "download_images.h"
#if defined(__GNUC__)
#include "GCC_Utility.h"
#endif

#define BOOT_ROM_WRITE_CMD      0xA1
#define BOOT_ROM_READ_CMD       0xA2
#define BOOT_ROM_CHECKSUM_CMD   0xA4
#define BOOT_ROM_JUMP_CMD       0xA8
#define BOOT_ROM_WRITE_NO_ECHO_CMD			0xAD
#define BOOT_ROM_WRITE32_CMD				0xAE
#define BOOT_ROM_READ32_CMD					0xAF

#define BL_CMD_WRITE16         0xA1
#define BL_CMD_READ16          0xA2
#define BL_CMD_CHKSUM16        0xA4
#define BL_CMD_JUMP_DA         0xA8
#define BL_CMD_WRITE16_NO_ECHO 0xAD
#define BL_CMD_WRITE32         0xAE
#define BL_CMD_READ32          0xAF
#define BL_CMD_JUMP_MAUI       0xB7

#define BROM_CMD_READ16       0xD0
#define BROM_CMD_READ32       0xD1
#define BROM_CMD_WRITE16      0xD2
#define BROM_CMD_WRITE32      0xD4
#define BROM_CMD_JUMP_DA      0xD5
#define BROM_CMD_JUMP_BL      0xD6
#define BROM_CMD_SEND_DA      0xD7
#define BROM_CMD_ENV_PREPARE  0xD9

// Boot-loader Command
#define	BOOT_LOADER_GET_FW_VER				0xBF
#define BOOT_LOADER_GET_VER_CMD				0xFE


int DumpDRAMSetting(DRAMSetting_U dramSetting_u, unsigned int version)
{
    switch(version)
    {
        case 5:
        {
            const DRAMSetting_v05 *dramSetting = &dramSetting_u.v05;

            log_output(" Version[%d] type[%d]\n", version , dramSetting->ramType);
            log_output(" EMI_CONI[%08x]*\n", dramSetting->EMI_CONI_Value );
            log_output(" EMI_CONJ[%08x]\n", dramSetting->EMI_CONJ_Value );
            log_output(" EMI_CONK[%08x]\n", dramSetting->EMI_CONK_Value );
            log_output(" EMI_CONL[%08x]\n", dramSetting->EMI_CONL_Value );
            log_output(" EMI_CONN[%08x]\n", dramSetting->EMI_CONN_Value );
            log_output(" EMI_DRVA[%08x]*\n", dramSetting->EMI_DRVA_Value );
            log_output(" EMI_DRVB[%08x]*\n", dramSetting->EMI_DRVB_Value );
            log_output(" EMI_ODLA[%08x]\n", dramSetting->EMI_ODLA_Value );
            log_output(" EMI_ODLB[%08x]\n", dramSetting->EMI_ODLB_Value );
            log_output(" EMI_ODLC[%08x]*\n", dramSetting->EMI_ODLC_Value );
            log_output(" EMI_ODLD[%08x]*\n", dramSetting->EMI_ODLD_Value );
            log_output(" EMI_ODLE[%08x]*\n", dramSetting->EMI_ODLE_Value );
            log_output(" EMI_ODLG[%08x]*\n", dramSetting->EMI_ODLG_Value );
            log_output(" EMI_DUTA[%08x]\n", dramSetting->EMI_DUTA_Value );
            log_output(" EMI_DUTB[%08x]\n", dramSetting->EMI_DUTB_Value );
            log_output(" EMI_DUTC[%08x]\n", dramSetting->EMI_DUTC_Value );
            log_output(" EMI_DUCA[%08x]\n", dramSetting->EMI_DUCA_Value );
            log_output(" EMI_DUCB[%08x]\n", dramSetting->EMI_DUCB_Value );
            log_output(" EMI_DUCE[%08x]\n", dramSetting->EMI_DUCE_Value );
            log_output(" EMI_IOCL[%08x]\n", dramSetting->EMI_IOCL_Value );
        }
        break;
        case 6:
        {
            const DRAMSetting_v06 *dramSetting = &dramSetting_u.v06;

            log_output(" Version[%d] type[%d]\n", version , dramSetting->ramType);
            log_output(" EMI_CONI[%08x]*\n", dramSetting->EMI_CONI_Value );
            log_output(" EMI_CONJ[%08x]\n", dramSetting->EMI_CONJ_Value );
            log_output(" EMI_CONK[%08x]\n", dramSetting->EMI_CONK_Value );
            log_output(" EMI_CONL[%08x]\n", dramSetting->EMI_CONL_Value );
            log_output(" EMI_CONN[%08x]\n", dramSetting->EMI_CONN_Value );
            log_output(" EMI_DRVA[%08x]*\n", dramSetting->EMI_DRVA_Value );
            log_output(" EMI_DRVB[%08x]*\n", dramSetting->EMI_DRVB_Value );
            //log_output(" EMI_ODLA[%08x]\n", dramSetting->EMI_ODLA_Value );
            //log_output(" EMI_ODLB[%08x]\n", dramSetting->EMI_ODLB_Value );
            //log_output(" EMI_ODLC[%08x]*\n", dramSetting->EMI_ODLC_Value );
            //log_output(" EMI_ODLD[%08x]*\n", dramSetting->EMI_ODLD_Value );
            log_output(" EMI_ODLE[%08x]*\n", dramSetting->EMI_ODLE_Value );
            log_output(" EMI_ODLG[%08x]*\n", dramSetting->EMI_ODLG_Value );
            log_output(" EMI_ODLH[%08x]\n", dramSetting->EMI_ODLH_Value );
            log_output(" EMI_ODLI[%08x]\n", dramSetting->EMI_ODLI_Value );
            log_output(" EMI_ODLJ[%08x]\n", dramSetting->EMI_ODLJ_Value );
            log_output(" EMI_ODLK[%08x]\n", dramSetting->EMI_ODLK_Value );
            log_output(" EMI_ODLL[%08x]\n", dramSetting->EMI_ODLL_Value );
            log_output(" EMI_ODLM[%08x]\n", dramSetting->EMI_ODLM_Value );
            log_output(" EMI_ODLN[%08x]\n", dramSetting->EMI_ODLN_Value );

            log_output(" EMI_DUTA[%08x]\n", dramSetting->EMI_DUTA_Value );
            log_output(" EMI_DUTB[%08x]\n", dramSetting->EMI_DUTB_Value );
            log_output(" EMI_DUTC[%08x]\n", dramSetting->EMI_DUTC_Value );
            log_output(" EMI_DUCA[%08x]\n", dramSetting->EMI_DUCA_Value );
            log_output(" EMI_DUCB[%08x]\n", dramSetting->EMI_DUCB_Value );
            log_output(" EMI_DUCE[%08x]\n", dramSetting->EMI_DUCE_Value );
            log_output(" EMI_IOCL[%08x]\n", dramSetting->EMI_IOCL_Value );
        }
        break;
        default:
        {
            const DRAMSetting_v03 *dramSetting = &dramSetting_u.v03;

            log_output(" Version[%d] type[%d]\n", version , dramSetting->ramType);
            log_output(" EMI_CONI[%08x]*\n", dramSetting->EMI_CONI_Value );
            log_output(" EMI_CONJ[%08x]\n", dramSetting->EMI_CONJ_Value );
            log_output(" EMI_CONK[%08x]\n", dramSetting->EMI_CONK_Value );
            log_output(" EMI_CONL[%08x]\n", dramSetting->EMI_CONL_Value );
            log_output(" EMI_CONN[%08x]\n", dramSetting->EMI_CONN_Value );
            log_output(" EMI_DQSA[%08x]*\n", dramSetting->EMI_DQSA_Value );
            log_output(" EMI_DRVA[%08x]*\n", dramSetting->EMI_DRVA_Value );
            log_output(" EMI_DRVB[%08x]*\n", dramSetting->EMI_DRVB_Value );
            log_output(" EMI_ODLA[%08x]\n", dramSetting->EMI_ODLA_Value );
            log_output(" EMI_ODLB[%08x]\n", dramSetting->EMI_ODLB_Value );
            log_output(" EMI_ODLC[%08x]*\n", dramSetting->EMI_ODLC_Value );
            log_output(" EMI_ODLD[%08x]*\n", dramSetting->EMI_ODLD_Value );
            log_output(" EMI_ODLE[%08x]*\n", dramSetting->EMI_ODLE_Value );
            log_output(" EMI_ODLG[%08x]*\n", dramSetting->EMI_ODLG_Value );
        }
    }

    return S_DONE;
}



static int bootrom_exchange_byte(COM_HANDLE com_handle, unsigned char data)
{
    unsigned char response;

    if (com_send_byte(com_handle, data) != COM_STATUS_DONE)
    {
        return 401;
    }

#if defined(_MSC_VER)
	if (!FlushFileBuffers(com_handle))
    {
        return COM_STATUS_ERROR;
    }
#endif

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 402;
    }

    if (response != data)
    {
        return 403;
    }

    return 0;
}


static int bootrom_exchange_word(COM_HANDLE com_handle, unsigned short data)
{
    unsigned short response;

    if (com_send_word(com_handle, data) != COM_STATUS_DONE)
    {
        return 404;
    }

    if (com_recv_word(com_handle, &response) != COM_STATUS_DONE)
    {
        return 405;
    }

    if (response != data)
    {
        return 406;
    }

    return 0;
}


static int bootrom_exchange_dword(COM_HANDLE com_handle, unsigned int data)
{
    unsigned int response;

    if (com_send_dword(com_handle, data) != COM_STATUS_DONE)
    {
        return 407;
    }

    if (com_recv_dword(com_handle, &response) != COM_STATUS_DONE)
    {
        return 408;
    }

    if (response != data)
    {
        return 409;
    }

    return 0;
}


// Implement simplified BootROM Write16 command => len always equals 1
static STATUS_E bootrom_write16_cmd(COM_HANDLE com_handle,
                                    unsigned int addr, unsigned short data)
{
	unsigned short dummy;

    if (bootrom_exchange_byte(com_handle, BROM_CMD_WRITE16) != 0)
    {
        return 410;
    }

    if (bootrom_exchange_dword(com_handle, addr) != 0)
    {
        return 411;
    }

    if (bootrom_exchange_dword(com_handle, 1) != 0)
    {
        return 412;
    }

	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 413;
    }

    if (bootrom_exchange_word(com_handle, data) != 0)
    {
        return 414;
    }

	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 415;
    }

    return S_DONE;
}

static STATUS_E bootrom_write16_cmd_forWDT(COM_HANDLE com_handle,
										   unsigned int addr, unsigned short data)
{
	Sleep(500);
    if (bootrom_exchange_byte(com_handle, BOOT_ROM_WRITE_CMD) != 0)
    {
        return 420;
    }
	Sleep(500);
    if (bootrom_exchange_dword(com_handle, addr) != 0)
    {
        return 421;
    }
	Sleep(500);
    if (bootrom_exchange_dword(com_handle, 1) != 0)
    {
        return 422;
    }
	Sleep(500);
    if (bootrom_exchange_word(com_handle, data) != 0)
    {
        return 423;
    }

    return S_DONE;
}

/**
* Write data to the specified memory address.
*
* @param hCOM handle to a COM port
* @param ulBaseAddr base memory address
* @param data data to be written
* @param num_of_dword number of DWORDs (32-bit) to write
*
* @return zero for success or non-zero for failure
*/
static STATUS_E bootrom_WriteCmd32_cmd(COM_HANDLE com_handle,
									   unsigned int ulBaseAddr, unsigned int *data, unsigned int num_of_dword)
{
	unsigned int offset;
	unsigned short dummy;

    if (bootrom_exchange_byte(com_handle, BROM_CMD_WRITE32) != 0)
    {
        return 424;
    }

	if (bootrom_exchange_dword(com_handle, ulBaseAddr) != 0)
    {
        return 425;
    }

	if (bootrom_exchange_dword(com_handle, num_of_dword) != 0)
    {
        return 426;
    }

	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 427;
    }

	for(offset=0; offset<num_of_dword; offset++) {
		if (bootrom_exchange_dword(com_handle, data[offset]) != 0)
		{
			return 428;
		}
	}
	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 429;
    }

    return S_DONE;
}
int bootrom_WriteCmd_cmd(COM_HANDLE com_handle, unsigned int ulBaseAddr, unsigned short *data, unsigned int num_of_word)
{
	unsigned int offset;
	int ret;

	if(bootrom_exchange_byte(com_handle, BOOT_ROM_WRITE_CMD))
		return 434;

	if((ret=bootrom_exchange_dword(com_handle, ulBaseAddr))) {
		return 435;
	}

	if((ret=bootrom_exchange_dword(com_handle, num_of_word))) {
		return 436;
	}

	for(offset=0; offset<num_of_word; offset++) {
		if (bootrom_exchange_word(com_handle, data[offset]) != COM_STATUS_DONE)
		{
			return 437;
		}
	}
	return 0;
}

// Implement simplified BootROM Read16 command => len always equals 1
static STATUS_E bootrom_read16_cmd(COM_HANDLE com_handle,
                                   unsigned int addr, unsigned short *data)
{
    if (bootrom_exchange_byte(com_handle, BOOT_ROM_READ_CMD) != 0)
    {
        return 438;
    }

    if (bootrom_exchange_dword(com_handle, addr) != 0)
    {
        return 439;
    }

    if (bootrom_exchange_dword(com_handle, 1) != 0)
    {
        return 440;
    }

    if (com_recv_word(com_handle, data) != COM_STATUS_DONE)
    {
        return 441;
    }

    return 0;
}

/**
* Read data from the specified memory address.
*
* @param hCOM handle to a COM port
* @param ulBaseAddr base memory address
* @param data [out] buffer to receive data
* @param num_of_dword number of DWORDs (32-bit) to read
*
* @return zero for success or non-zero for failure
*/
static STATUS_E bootrom_ReadCmd32_cmd(COM_HANDLE com_handle,
									  unsigned int ulBaseAddr, unsigned int *data, unsigned int num_of_dword)
{

	unsigned int offset;
	unsigned short dummy;

    if (bootrom_exchange_byte(com_handle, BROM_CMD_READ32) != 0)
    {
        return 446;
    }

	if (bootrom_exchange_dword(com_handle, ulBaseAddr) != 0)
    {
        return 447;
    }

	if (bootrom_exchange_dword(com_handle, num_of_dword) != 0)
    {
        return 448;
    }
	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 449;
    }

	for(offset=0; offset<num_of_dword; offset++)	{

		if (com_recv_dword(com_handle, &data[offset]) != 0)
		{
			return 450;
		}
	}
	if (com_recv_word(com_handle, &dummy) != 0)
    {
        return 451;
    }
    return S_DONE;
}

int bootrom_ReadCmd_cmd(COM_HANDLE com_handle, unsigned int ulBaseAddr, unsigned short *data, unsigned int num_of_word)
{
	unsigned int offset;
	int ret;

	if(bootrom_exchange_byte(com_handle, BOOT_ROM_READ_CMD ))
		return 456;

	if((ret=bootrom_exchange_dword(com_handle, ulBaseAddr))) {
		return 457;
	}

	if((ret=bootrom_exchange_dword(com_handle, num_of_word))) {
		return 458;
	}

	for(offset=0; offset<num_of_word; offset++)	{

		if (com_recv_word(com_handle, &data[offset]) != 0)
		{
			return 459;
		}
	}

	return 0;
}

int DetectRAMSize(COM_HANDLE com_handle, const unsigned int ram_baseaddr, const unsigned int ram_max_size, const unsigned int ram_step_uint, unsigned int *p_size)
{
#define RAMSIZE_BEGIN_PATTERN	"<<<RAM_BEGIN>>>"
#define RAMSIZE_END_PATTERN		"<<<RAM_END>>>"
#define PATTERN_MAX_SIZE	16

	char ramsize_begin_pattern[PATTERN_MAX_SIZE+1] = {0};
	char ramsize_end_pattern[PATTERN_MAX_SIZE+1] = {0};
	char pattern_buffer[PATTERN_MAX_SIZE+1] = {0};
	unsigned int pppp[PATTERN_MAX_SIZE/4] = {0};
	unsigned int val32_1, val32_2, val32_3;

	unsigned int ram_addr;
	unsigned int i;

	sprintf(&ramsize_begin_pattern[0],"%s",RAMSIZE_BEGIN_PATTERN);
	sprintf(&ramsize_end_pattern[0],"   %s",RAMSIZE_END_PATTERN);

	// 1. write pattern into ram (assume the wait state is enough)
	if(bootrom_WriteCmd32_cmd(com_handle, ram_baseaddr, (unsigned int *)&ramsize_begin_pattern[0], PATTERN_MAX_SIZE/4))
		return 460;

	// 2. use address wrap to calculate ram size
	for( ram_addr = ram_baseaddr + ram_step_uint; ram_addr < (ram_baseaddr + ram_max_size); ram_addr += ram_step_uint ){
		if(bootrom_ReadCmd32_cmd(com_handle, ram_addr, (unsigned int *)&pattern_buffer[0], PATTERN_MAX_SIZE/4))
			return 461;

		if( !memcmp(&pattern_buffer[0],&ramsize_begin_pattern[0],PATTERN_MAX_SIZE) )
			break;
	}

	// 3. backward check if the address is writable
	while( ram_addr > ram_baseaddr ) {

		if(bootrom_WriteCmd32_cmd(com_handle, ram_addr-PATTERN_MAX_SIZE, (unsigned int *)&ramsize_end_pattern[0], PATTERN_MAX_SIZE/4))
			return 462;

		if(bootrom_ReadCmd32_cmd( com_handle, ram_addr-PATTERN_MAX_SIZE, (unsigned int *)&pattern_buffer[0], PATTERN_MAX_SIZE/4))
			return 463;

		if( !memcmp(&pattern_buffer[0],&ramsize_end_pattern[0],PATTERN_MAX_SIZE) )	{
			// writable, it's valid ram address
			break;
		}
		else{
			// can not be written, backward to previous block
			ram_addr -= ram_step_uint;
		}

	}

	// 4. can not detect ram size, ram might be un-accessable or floating
	if( (ram_addr<=ram_baseaddr) || ((ram_baseaddr+ram_max_size)<=ram_addr) ) {

		// floating test
		for( i = 0; i < 5; i++){
			if(bootrom_ReadCmd32_cmd( com_handle, ram_baseaddr, &val32_1, 1))
				return 464;
			if(bootrom_ReadCmd32_cmd( com_handle, ram_baseaddr, &val32_2, 1))
				return 465;

			if( val32_1 != val32_2 ) {
				// data floating
				return S_DA_RAM_FLOARTING;
			}
		}

		// un-accessable test
		if(bootrom_ReadCmd32_cmd( com_handle, ram_baseaddr, &val32_1, 1))
			return 457;
		val32_2 = ~val32_1;

		// floating test
		for( i = 0; i < 5; i++){
			if(bootrom_WriteCmd32_cmd( com_handle, ram_baseaddr, &val32_2, 1))
				return 458;
			if(bootrom_ReadCmd32_cmd( com_handle, ram_baseaddr, &val32_3, 1))
				return 459;

			if( val32_1 != val32_3 ) {
				// data is accessable, it's unknown error
				return S_DA_RAM_ERROR;
			}
		}

		// un-accessable, data is still
		return S_DA_RAM_UNACCESSABLE;
	}

	if( NULL != p_size ) {
		*p_size = (ram_addr-ram_baseaddr);
	}

	return S_DONE;

}


STATUS_E bootrom_connect(COM_HANDLE com_handle)
{
    const unsigned char BOOTROM_START_COMMAND[] = { 0xA0, 0x0A, 0x50, 0x05 };
    const unsigned int NUM_BOOTROM_START_COMMANDS =
		sizeof(BOOTROM_START_COMMAND) / sizeof(BOOTROM_START_COMMAND[0]);
    unsigned int i;
    DWORD start_time, cur_time;

    // setup start timestamp
    start_time = GetTickCount();

    for (i=0; i<NUM_BOOTROM_START_COMMANDS;)
    {
        const unsigned char command = BOOTROM_START_COMMAND[i];
        const unsigned char expected_response = ~command;
        unsigned char response;

        log_output("send 0x%x \n", command);
        if (com_send_byte(com_handle, command) != COM_STATUS_DONE)
        {
            log_output("ERROR : bootrom_connect() start command send failed.\n");
            return S_BROM_CMD_STARTCMD_FAIL;
        }

        // [Caution]
        //   This cannot be a blocking read operation because we have to
        //   have a chance to reset the target and retry the whole
        //   synchronization flow.
        if (com_recv_byte(com_handle, &response) == COM_STATUS_DONE)
        {
            log_output("recv 0x%x \n", response);
            if (response == expected_response)
            {
                ++i;
            }
            else
            {
                i = 0;
                gsm_reset();
            }
        }
        else
        {
        	//Ruoyao : no need retry while no receive by suggetion of bootrom owner
            //i = 0;
            //gsm_reset();
            log_output("ERROR : bootrom_connect() recv byte fail.\n");
            return S_BROM_CMD_STARTCMD_FAIL;
        }

        // get cur timestamp
        cur_time = GetTickCount();

        // check if exceed timeout value
        // 6261: from 20000 to 10000
        if( (cur_time-start_time) >= TIMEOUT_10S ) { // 20s
            log_output("ERROR : bootrom_connect() start command timeout.\n");
            return S_BROM_CMD_STARTCMD_TIMEOUT;
        }
    }

    return S_DONE;
}


STATUS_E bootrom_disable_watchdog(COM_HANDLE com_handle)
{
#define REG_RGU_WDT_MODE     0xA0030000

    return bootrom_write16_cmd(com_handle, REG_RGU_WDT_MODE, 0x2200);
}

STATUS_E bootrom_read_platform_code(COM_HANDLE com_handle, unsigned short *platform_code)
{
#define D_HW_CODE_REG     0x80000008

    return bootrom_read16_cmd(com_handle, D_HW_CODE_REG, platform_code);
}



STATUS_E bootrom_latch_powerkey(COM_HANDLE com_handle)
{
#define REG_RTC_BBPU        0xA0710000
#define REG_RTC_IRQ_EN      0xA0710008
#define REG_RTC_CCI_EN      0xA071000C
#define REG_RTC_AL_MASK     0xA0710010
#define REG_RTC_POWERKEY1   0xA0710050
#define REG_RTC_POWERKEY2   0xA0710054
#define REG_RTC_PROT        0xA0710068
#define REG_RTC_WRTGR       0xA0710074

    unsigned short powerkey1 = 0;
    unsigned short powerkey2 = 0;
    int powerkey_matched = 0;
    DWORD start_time, cur_time;

    start_time = GetTickCount(); // setup start timestamp
	while (1)
    {
        unsigned short data = 0;

        if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
        {
            return 492;
        }

        if ((data & 0x0040) == 0)
        {
            break;
        }

        cur_time = GetTickCount(); // get cur timestamp

        // check if exceed timeout value
        if( (cur_time-start_time) >= TIMEOUT_10S) {
            log_output("[ERROR]RTC polling timeout [492]!!\n");
            return 492;
        }
    }

    // Issue the following Boot ROM commands to retrieve
    // POWERKEY1 and POWERKEY2:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_POWERKEY1, 0x1)
    //   16-bit Read -- (Base address, Length) = (REG_RTC_POWERKEY2, 0x1)
    //
    // Record the power key state:
    //   PowerKeyMatched = ((POWERKEY1 == 0xA357) && (POWERKEY2 == 0x67D2))
    {
        if (bootrom_read16_cmd(com_handle,
			REG_RTC_POWERKEY1, &powerkey1) != S_DONE)
        {
            return 493;
        }

        if (bootrom_read16_cmd(com_handle,
			REG_RTC_POWERKEY2, &powerkey2) != S_DONE)
        {
            return 494;
        }

        powerkey_matched =
            ((powerkey1 == 0x0A357) && (powerkey2 == 0x67D2)) ? 1 : 0;
    }

    // If PowerKeyMatched is false, issue the following Boot ROM commands:
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_AL_MASK, 0x1, 0x0)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_IRQ_EN, 0x1, 0x0)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_CCI_EN, 0x1, 0x0)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_WRTGR, 0x1, 0x1)
    if (!powerkey_matched)
    {
        if (bootrom_write16_cmd(com_handle, REG_RTC_AL_MASK, 0x0) != S_DONE)
        {
            return 495;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_IRQ_EN, 0x0) != S_DONE)
        {
            return 496;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_CCI_EN, 0x0) != S_DONE)
        {
            return 497;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_WRTGR, 0x1) != S_DONE)
        {
            return 498;
        }
    }

    // If PowerKeyMatched is false, repeatedly issue the following Boot ROM
    // command until the read value's bit 6 becomes 0:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_BBPU, 0x1)
    if (!powerkey_matched)
    {
        start_time = GetTickCount(); // setup start timestamp
        while (1)
        {
            unsigned short data = 0;

            if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
            {
                return 499;
            }

            if ((data & 0x0040) == 0)
            {
                break;
            }

            cur_time = GetTickCount(); // get cur timestamp

            // check if exceed timeout value
            if( (cur_time-start_time) >= TIMEOUT_10S) {
                log_output("[ERROR]RTC polling timeout [499]!!\n");
                return 499;
            }
        }
    }

    // If PowerKeyMatched is false, issue the following Boot ROM commands:
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_POWERKEY1, 0x1, 0xA357)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_POWERKEY2, 0x1, 0x67D2)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_WRTGR, 0x1, 0x1)
    if (!powerkey_matched)
    {
        if (bootrom_write16_cmd(com_handle,	REG_RTC_POWERKEY1, 0xA357) != S_DONE)
        {
            return 500;
        }

        if (bootrom_write16_cmd(com_handle,	REG_RTC_POWERKEY2, 0x67D2) != S_DONE)
        {
            return 501;
        }
        if (bootrom_write16_cmd(com_handle, REG_RTC_WRTGR, 0x1) != S_DONE)
        {
            return 502;
        }
    }

    // If PowerKeyMatched is false, repeatedly issue the following Boot ROM
    // command until the read value's bit 6 becomes 0:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_BBPU, 0x1)
    if (!powerkey_matched)
    {
        while (1)
        {
            unsigned short data = 0;
            start_time = GetTickCount(); // setup start timestamp

            if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
            {
                return 503;
            }

            if ((data & 0x0040) == 0)
            {
                break;
            }

            cur_time = GetTickCount(); // get cur timestamp

            // check if exceed timeout value
            if( (cur_time-start_time) >= TIMEOUT_10S) {
                log_output("[ERROR]RTC polling timeout [503]!!\n");
                return 503;
            }
        }
    }

    // Issue the following Boot ROM commands:
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_PROT, 0x1, 0x586A)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_WRTGR, 0x1, 0x1)
    {
        if (bootrom_write16_cmd(com_handle, REG_RTC_PROT, 0x586A) != S_DONE)
        {
            return 504;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_WRTGR, 0x1) != S_DONE)
        {
            return 505;
        }
    }

    // Repeatedly issue the following Boot ROM command until
    // the read value's bit 6 becomes 0:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_BBPU, 0x1)
    {
        while (1)
        {
            unsigned short data = 0;
            start_time = GetTickCount(); // setup start timestamp

            if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
            {
                return 506;
            }

            if ((data & 0x0040) == 0)
            {
                break;
            }

            cur_time = GetTickCount(); // get cur timestamp

            // check if exceed timeout value
            if( (cur_time-start_time) >= TIMEOUT_10S) {
                log_output("[ERROR]RTC polling timeout [506]!!\n");
                return 506;
            }
        }
    }

    // Issue the following Boot ROM commands:
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_PROT, 0x1, 0x9136)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_WRTGR, 0x1, 0x1)
    {
        if (bootrom_write16_cmd(com_handle, REG_RTC_PROT, 0x9136) != S_DONE)
        {
            return 507;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_WRTGR, 0x1) != S_DONE)
        {
            return 508;
        }
    }

    // Repeatedly issue the following Boot ROM command until
    // the read value's bit 6 becomes 0:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_BBPU, 0x1)
    while (1)
    {
        unsigned short data = 0;
        start_time = GetTickCount(); // setup start timestamp

        if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
        {
            return 509;
        }

        if ((data & 0x0040) == 0)
        {
            break;
        }

        cur_time = GetTickCount(); // get cur timestamp

        // check if exceed timeout value
        if( (cur_time-start_time) >= TIMEOUT_10S ) {
            log_output("[ERROR]RTC polling timeout [509]!!\n");
            return 509;
        }
    }

    // Issue the following Boot ROM commands:
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_BBPU, 0x430E)
    //   16-bit Write -- (Base address, Length, Data) = (REG_RTC_WRTGR, 0x1, 0x1)
    {
        if (bootrom_write16_cmd(com_handle, REG_RTC_BBPU, 0x430E) != S_DONE)
        {
            return 510;
        }

        if (bootrom_write16_cmd(com_handle, REG_RTC_WRTGR, 0x1) != S_DONE)
        {
            return 511;
        }
    }

    // Repeatedly issue the following Boot ROM command until
    // the read value's bit 6 becomes 0:
    //   16-bit Read -- (Base address, Length) = (REG_RTC_BBPU, 0x1)
    while (1)
    {
        unsigned short data = 0;
        start_time = GetTickCount(); // setup start timestamp

        if (bootrom_read16_cmd(com_handle, REG_RTC_BBPU, &data) != S_DONE)
        {
            return 512;
        }

        if ((data & 0x0040) == 0)
        {
            break;
        }

        cur_time = GetTickCount(); // get cur timestamp

        // check if exceed timeout value
        if( (cur_time-start_time) >= TIMEOUT_10S) {
            log_output("[ERROR]RTC polling timeout  [512]!!\n");
            return 512;
        }
    }

    return S_DONE;
}


STATUS_E bootrom_SetRemap(COM_HANDLE com_handle, unsigned int mode)
{
#define BOOT_CON       0xA0510000

    unsigned int setting = 0;

    // 6260 move remap register from EMI_GENA to BOOT_CON
    // 0xA0510000 BOOT_CON ARM Boot Setting Register
    //  Bit 1 BootCode: Control whether to return boot code or not
    //          0: 0x0 will return boot code
    //          1: 0x0 directed to AHB slave

    //  Bit 0 REMAP: Control bank0, 1 address remapping
    //          0: bank0 to SFC, bank1 to PSRAM
    //          1: bank0 to PSRAM, bank1 to SFC
    if(mode == 1) //REMAP_MODE_OPTION_XIP
    {
        // 6260 with NOR/SF
        if (bootrom_ReadCmd32_cmd(com_handle, BOOT_CON, &setting, 1))
        {
            return 1;
        }

        setting &= 0xFFFFFFFC;
        // for SF+DDR, DDR is put in Bank 1, SF in bank 0, don't remap it.
        setting |= 0x00000002;

        if (bootrom_WriteCmd32_cmd(com_handle, BOOT_CON, &setting, 1))
        {
            return 2;
        }
    }
    else // NORMAL mode
    {
        // 6260 with SPI-NAND
        if (bootrom_ReadCmd32_cmd(com_handle, BOOT_CON, &setting, 1))
        {
            return 1;
        }

        setting &= 0xFFFFFFFC;
        // for NAND+DDR while DDR is put in Bank 1, so remap it to be bank 0!
        setting |= 0x00000003;

        if (bootrom_WriteCmd32_cmd(com_handle, BOOT_CON, &setting, 1))
        {
            return 2;
        }
    }


    return S_DONE;;
}



STATUS_E bootrom_SendEPP(COM_HANDLE com_handle,
                         const struct image *download_EPP,
                         const struct ExternalMemorySetting *externalMemorySetting,
                         const int isNFB)
{
    const unsigned int packet_len = 1024;
    unsigned char out_buf[1024]; // Caution: Is it OK to allocate 2KB on stack?
    unsigned short checksum = 0;
	unsigned short _checksum = 0;
    unsigned int num_bytes_sent = 0;
	unsigned short dummy,checksum_from_bootrom;
	unsigned short targetFlashID[8];
    unsigned int settingIndex = 0;
	int i;
	unsigned char response = 0;
	unsigned int dummy2;
    int ret = 0;

	if (bootrom_exchange_byte(com_handle, BROM_CMD_ENV_PREPARE) != 0)
    {
        return 522;
    }

	if(bootrom_exchange_dword(com_handle, download_EPP->load_addr) != 0)
		return 523;

	if(bootrom_exchange_dword(com_handle, download_EPP->len) != 0)
		return 524;

	//epp Param Addr
	if(bootrom_exchange_dword(com_handle, 0x7000C000 ) != 0)
		return 525;
	//epp Param Len
	if(bootrom_exchange_dword(com_handle, 0x1000) != 0)
		return 526;

	if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 527;
    }

	while (num_bytes_sent < download_EPP->len)
    {
        unsigned int num_bytes_to_send;
        unsigned short data;
        unsigned int i;

        if (packet_len > (download_EPP->len - num_bytes_sent))
        {
            num_bytes_to_send = (download_EPP->len - num_bytes_sent);
        }
        else
        {
            num_bytes_to_send = packet_len;
        }

		for (i=0; i<num_bytes_to_send; i+=2)
        {
            out_buf[i] = download_EPP->buf[num_bytes_sent + i];
            out_buf[i+1] = download_EPP->buf[num_bytes_sent + i + 1];

            data = (((unsigned short) out_buf[i]) << 8) & 0xFF00;
            data |= ((unsigned short) out_buf[i+1]) & 0x00FF;

            checksum ^= data;
        }

        if (com_send_data(com_handle, out_buf,
			num_bytes_to_send) != COM_STATUS_DONE)
        {
            return 528;
        }

        num_bytes_sent += num_bytes_to_send;
    }

	if (com_recv_word(com_handle, &checksum_from_bootrom) != COM_STATUS_DONE)
    {
        return 529;
    }

	_checksum = checksum>>8;
	_checksum |= ((checksum<<8)&0xFF00);

	if (_checksum != checksum_from_bootrom)
    {
        return 530;
    }
	Sleep(1000);

	if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 531;
    }

	//EPP is executing...
	if ( (ret = com_recv_dword(com_handle, &dummy2)) != COM_STATUS_DONE)
    {
        printf("ret = %d, dummy2 = %d \n", ret, dummy2);
        return 532;
    }

	//Send FLASH_TYPE 4bytes 0x00000003 for Serial Flash
	//externalMemorySetting->flashInfo
	if (com_send_dword(com_handle, externalMemorySetting->flashInfo.flashType) != COM_STATUS_DONE)
	{
		return 533;
	}

    if(isNFB)
    {
        // only NAND project, need use combo memory to init DRAM
        //Send NAND_CHIP_SELECT 0x00
    	if (com_send_byte(com_handle, 0) != COM_STATUS_DONE)
    	{
    		return 534;
    	}
        //Send NAND_ACCCON 0x7007FFFF for MT6260
    	if (com_send_dword(com_handle, 0x7007FFFF) != COM_STATUS_DONE)
    	{
    		return 535;
    	}
    }

	//Start get flash ID from EPP
	for(i=0; i<8; i++)
	{
		if (com_recv_word(com_handle, &targetFlashID[i]) != COM_STATUS_DONE)
		{
			return 536;
		}
	}

    // Non-Sip DRAM, use Flash ID to select EMI Setting
    if(externalMemorySetting->ramSetting.ramType > 0 &&
       externalMemorySetting->ramSetting.ramType < 0x1000 )
    {
        if(externalMemorySetting->flashInfo.numValidEntries > 0)  // if there is flashID, compare it
    	{
            // FlashInfo must match DRamSetting
            if(externalMemorySetting->ramSetting.numValidEntries == externalMemorySetting->flashInfo.numValidEntries)
	        {
	            // Compare flash IDs for each setting
	            for(settingIndex = 0; settingIndex < externalMemorySetting->ramSetting.numValidEntries ; settingIndex++)
	            {
	                const flashID *id = &(externalMemorySetting->flashInfo.u.v02[settingIndex].NAND_ID);
                    unsigned int idIndex = 0;
	                if(id->idNumber > 8)
	                {
                        log_output("[ERROR]Too much flash IDs in MAUI bin !!\n");
                        return 537;
	                }

	                for(idIndex = 0 ; idIndex < id->idNumber ; idIndex++)
	                {
	                    if( targetFlashID[idIndex] != id->id[idIndex])
	                        break;
	                }

	                if(idIndex == id->idNumber) // all ID is match
	                {
                        log_output("Find match flash IDs, setting index = %d\n", settingIndex);
	                    break;
	                }
	            }

	            if(settingIndex == externalMemorySetting->ramSetting.numValidEntries)
	            {
	                //LOG_ERROR("BootROMCommandProtocol::SendEPP(): The flash IDs on Target is not match the flash IDs in Load");
	                if(settingIndex == 1)
	                {
                        log_output("[Single Memory] The flash IDs on Target is not match the flash IDs in MAUI bin, there is no suitable EMI setting can init DRAM\n");
                        return 538;
	                }
	                else
	                {
                        log_output("[Combo Memory] The flash IDs on Target is not match the flash IDs in MAUI bin, there is no suitable EMI setting can init DRAM\n");
                        return 539;
                    }
	            }
	        }
	        else
	        {
                log_output("[ERROR]The FlashInfo entry number is not match DRamSetting !!\n");
                return 540;
	        }
        }
    }
    else if(externalMemorySetting->ramSetting.ramType >= 0x1000 &&
            externalMemorySetting->ramSetting.ramType < 0x10000)
            // SIP-DDR , use EFUSE content to select EMI setting
    {
	    // Donot support to get EMI setting index from EMI info in EFUSE
	    // so default = 0
	    settingIndex = 0;
    }


    {
        DRAMSetting_U dramSetting;
        EPP_PARAM epp_param;

        memset (&dramSetting, 0, sizeof(DRAMSetting_U));
        memset(&epp_param, 0, sizeof(EPP_PARAM));

        switch(externalMemorySetting->ramSetting.version)
        {
            case 0:
                //Perhaps cfg file is not loaded
                log_output("[ERROR]Unknow EMI setting version\n");
                return 541;
            case 5:
                dramSetting.v05 = externalMemorySetting->ramSetting.u.v05[settingIndex];
                break;
            case 6:
                dramSetting.v06 = externalMemorySetting->ramSetting.u.v06[settingIndex];
                break;
            default:
                log_output("Old EMI setting version = %d\n",externalMemorySetting->ramSetting.version);
                dramSetting.v03 = externalMemorySetting->ramSetting.u.v03[settingIndex];
                break;

        }

        epp_param.cfg_version = externalMemorySetting->CFGVersion;
        epp_param.m_ExtRamType = externalMemorySetting->ramSetting.ramType;
        epp_param.m_PMIC_Controller = externalMemorySetting->PMICController;
        memcpy(&(epp_param.MTK_DRAM_Setting), &dramSetting, sizeof(DRAMSetting_U));

        log_output("epp_param.cfg_version = %d \n",epp_param.cfg_version);
        log_output("epp_param.m_ExtRamType = %d \n",epp_param.m_ExtRamType);
        log_output("epp_param.m_PMIC_Controller = %d \n",epp_param.m_PMIC_Controller);

        DumpDRAMSetting(epp_param.MTK_DRAM_Setting, externalMemorySetting->ramSetting.version);

    	//Send EPP Param
    	if (com_send_data(com_handle, (unsigned char *)&epp_param, sizeof(EPP_PARAM)) != COM_STATUS_DONE)
        {
            return 538;
        }

    }

	// init ext-ram status Ack(5A) is correct return
    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 539;
    }
	// Detect ext-ram status Ack(5A) is correct return
    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 540;
    }
	// detect Ext-Ram size
	if (com_recv_dword(com_handle, &dummy2) != COM_STATUS_DONE)
    {
        return 541;
    }
	// Boot Rom status
	if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 542;
    }
	// EPP Status
	if (com_recv_dword(com_handle, &dummy2) != COM_STATUS_DONE)
    {
        return 543;
    }

	return S_DONE;;
}


STATUS_E bootrom_send_download_agent(COM_HANDLE com_handle,
                                     const struct image *download_agent)
{
    const unsigned int packet_len = 1024;
    unsigned char out_buf[1024]; // Caution: Is it OK to allocate 2KB on stack?
    unsigned short checksum = 0;
	unsigned short _checksum = 0;
    unsigned int num_bytes_sent = 0;
	unsigned short dummy,checksum_from_bootrom;

    if (download_agent == NULL)
    {
        return 543;
    }

	if (bootrom_exchange_byte(com_handle, BROM_CMD_SEND_DA) != 0)
	{
		return 544;
	}

    if (bootrom_exchange_dword(com_handle, download_agent->load_addr) != 0)
    {
        return 545;
    }

    if (bootrom_exchange_dword(com_handle, download_agent->len + 0x0) != 0)
    {
        return 546;
    }
	//Sig len
	if (bootrom_exchange_dword(com_handle, 0x100) != 0)
    {
        return 547;
    }

    if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 548;
    }

    while (num_bytes_sent < download_agent->len)
    {
        unsigned int num_bytes_to_send;
        unsigned short data;
        unsigned int i;

        if (packet_len > (download_agent->len - num_bytes_sent))
        {
            num_bytes_to_send = (download_agent->len - num_bytes_sent);
        }
        else
        {
            num_bytes_to_send = packet_len;
        }

        for (i=0; i<num_bytes_to_send; i+=2)
        {
            out_buf[i] = download_agent->buf[num_bytes_sent + i];
            out_buf[i+1] = download_agent->buf[num_bytes_sent + i+ 1];

            data = (((unsigned short) out_buf[i]) << 8) & 0xFF00;
            data |= ((unsigned short) out_buf[i+1]) & 0x00FF;

            checksum ^= data;
        }

        if (com_send_data(com_handle, out_buf,
			num_bytes_to_send) != COM_STATUS_DONE)
        {
            return 549;
        }

        num_bytes_sent += num_bytes_to_send;
    }

	if (com_recv_word(com_handle, &checksum_from_bootrom) != COM_STATUS_DONE)
    {
        return 550;
    }

	_checksum = checksum>>8;
	_checksum |= ((checksum<<8)&0xFF00);
	if (_checksum != checksum_from_bootrom)
    {
        log_output("[ERROR] : DA checksum mis-match, pc_checksun(0x%04x) != brom_checksum(0x%04x)\n", _checksum, checksum_from_bootrom);
        return 551;
    }
	if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 552;
    }

    return S_DONE;
}


STATUS_E bootrom_jump_to_download_agent(COM_HANDLE com_handle,
                                        const struct image *download_agent)
{
	unsigned short dummy;

    if (bootrom_exchange_byte(com_handle, BROM_CMD_JUMP_DA) != 0)
    {
        return 566;
    }

    if (bootrom_exchange_dword(com_handle, download_agent->load_addr) != 0)
    {
        return 567;
    }
	if (com_recv_word(com_handle, &dummy) != COM_STATUS_DONE)
    {
        return 568;
    }
    return S_DONE;
}

