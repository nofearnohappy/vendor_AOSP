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

#include "bootrom_stage.h"
#include "da_stage.h"
#include "download_images.h"
#include "interface.h"
#include "_External/include/DOWNLOAD.H"
#if defined(__GNUC__)
#include "GCC_Utility.h"
#endif

static STATUS_E bootrom_stage(COM_HANDLE com_handle,
                                const struct image *download_agent,
                                const struct image *download_agent_TCM,
                                const struct image *download_EPP,
                                const ExternalMemorySetting *externalMemorySetting,
                                int isUSB, int isNFB)
{
    STATUS_E status;
	unsigned int response;
	unsigned int size = 0;
    unsigned int mode = 0;
    unsigned short platoform = 0;

    log_output("Connecting to BootROM... com_handle(%d)\n", com_handle);
    status = bootrom_connect(com_handle);


    if (status != S_DONE)
    {
        return status;
    }

    set_progress(15);

    //Todo: detect bootROM connection or bootloader connection
    //Use BROM USB below
#if defined(_MSC_VER)
	if (com_change_timeout(com_handle, 500, 5000))
	{
		return S_COM_PORT_SET_TIMEOUT_FAIL;
	}
#endif


    log_output("Disabling watchdog..\n");

	status = bootrom_disable_watchdog(com_handle);
	if (status != S_DONE)
	{
		if (com_recv_dword(com_handle, &response) != COM_STATUS_DONE)
		{
			return status;
		}
	}

    log_output("Get BB Code\n");
    status = bootrom_read_platform_code(com_handle, &platoform);
    if (status != S_DONE)
	{
		return status;
	}


	log_output("Latching powerkey...\n");
	status = bootrom_latch_powerkey(com_handle);
	if (status != S_DONE)
	{
		return status;
	}

	//SetRemap for BROM
	log_output("Set Remap...\n");
    if(externalMemorySetting->flashInfo.flashType == FLASHType_SERIAL_NOR_FLASH)
    {
        log_output("XIP\n");
        mode = 1;
    }
    else
    {
        log_output("Non-XIP\n");
        mode = 0; // normal mode
    }
	status = bootrom_SetRemap(com_handle, mode);
	if (status != S_DONE)
	{
		return status;
	}

    set_progress(20);

    if(platoform == 0x6260) // Only MT6260 has to do EPP flow. (MT6261 doesn't)
    {
    	log_output("Send EPP...\n");
        if(download_EPP->len == 0)
        {
            log_output("ERROR : Send EPP fail, EPP image is not loading yet\n");
            return S_BROM_DOWNLOAD_EPP_FAIL;
        }
    	status = bootrom_SendEPP(com_handle, download_EPP, externalMemorySetting, isNFB);
    	if (status != S_DONE)
    	{
            log_output("ERROR : Send EPP fail, status code = %d\n", status);
    		return S_BROM_DOWNLOAD_EPP_FAIL;
    	}
    }

	status = bootrom_SetRemap(com_handle, mode);
	if (status != S_DONE)
	{
		return status;
	}

    set_progress(22);

	log_output("Sending DA1...\n");
	status = bootrom_send_download_agent(com_handle, download_agent);
	if (status != S_DONE)
	{
		return status;
	}
    if(download_agent_TCM->buf != NULL)
    {
        set_progress(25);
		log_output("Sending DA2...\n");
		status = bootrom_send_download_agent(com_handle, download_agent_TCM);
		if (status != S_DONE)
		{
			return status;
		}
    }

    set_progress(28);

	log_output("Transferring control to DA...\n");
	status = bootrom_jump_to_download_agent(com_handle, download_agent);
	if (status != S_DONE)
	{
		return status;
	}

    log_output("BootROM stage .... Done\n");
    return S_DONE;
}

static STATUS_E da_stage(COM_HANDLE com_handle,
                         const struct image *nor_flash_table,
                         const struct image *nand_flash_table,
                         Region_images *boot_region_images,
                         Region_images *main_region_images,
						 int isUSB,
						 int isNFB,
						 unsigned int bmt_address)
{
    STATUS_E status;

    // To enable hardware flow contrl, you have to
    // (1) properly configure CTS/RTS of the target hardware,
    // (2) use hardware-flow-control-enabled Download Agent, and
    // (3) uncomment the following lines

    //if (com_enable_hardware_flow_control(com_handle) != COM_STATUS_DONE)
    //{
    //    log_output("Failed to enable HW flow control "
    //               "on the communication port\n");
    //    return S_UNDEFINED_ERROR;
    //}

    unsigned char response = 0;
    unsigned int errorCode =0;
    int eraseHB = 0;

    log_output("Connecting to DA...\n");
    status = da_connect(com_handle, nor_flash_table, nand_flash_table, bmt_address);

    if (status != S_DONE)
    {
        return status;
    }

    set_progress(32);

	//CMD_GetFATRanges
	if(!isUSB)
	{
		log_output("Changing baudrate...\n");
		status = da_change_baudrate_phase1(com_handle);

		if (status != S_DONE)
		{
			return status;
		}

		// Wait for target's UART TX/RX to become stable
		Sleep(1000);

		if (com_change_baudrate(com_handle, 921600) != COM_STATUS_DONE)
		{
			log_output("Failed to change baudrate of the communication port\n");
			return S_UNDEFINED_ERROR;
		}
        log_output("Changing baudrate phase2...\n");
		status = da_change_baudrate_phase2(com_handle);

		if (status != S_DONE)
		{
			return status;
		}
	}

    //Format CBR before writing
    log_output("Formatting CBR...\n");
    status = SV5_CMD_FormatCBR(com_handle);
    if(status != 0)
    {
        return status;
    }

    set_progress(35);

	//Write Boot loader
	//Assume bootloader full download if bootLoader != NULL
	//       or no bootloader download
	if(boot_region_images->num_images != 0)
	{
		log_output("Write Boot loader...\n");
		status = da_write_boot_loader(  com_handle,
                                        boot_region_images,
                                        isNFB);
		if (status != S_DONE)
		{
			return status;
		}
	}
    else
    {
        // Bypass check boot loader feature
        // only check target is not empty
	    log_output("Get boot loader feature...\n");
		status = SV5_CMD_CheckBootLoaderFeature_CheckLoadType(com_handle, &(boot_region_images->region_images[0]), FEATURE_CHECK_WITH_ARM_BL);
		if (status != S_DONE)
		{
			return status;
		}

	}

    set_progress(50);

    if(isNFB)
    {

//======================================================
        //Send NFB write image
		if( main_region_images->num_images > 0)
		{
    		log_output("Write NFB images...\n");
    		status = da_write_NFB_images(com_handle,
                                main_region_images,
                                4096);
    		if (status != S_DONE)
    		{
    			return status;
    		}
		}
        /*
        status = da_FormatFAT(com_handle,
                                HW_STORAGE_NAND,
                                0,
                                NULL,
                                NULL);
		if (status != S_DONE)
		{
			return status;
		}*/
//======================================================
/*
//======================================================
        if(num_linux_images > 0)
        {
            // if both the ARM BL and EXT BL are downloaded, erase the HB
            if(bootLoader->buf != NULL && extBootLoader->buf != NULL)
            {
                eraseHB = 1;
            }

    		//Send NFB write linux partition image
    		log_output("Write linux partition images...\n");
    		status = da_write_linux_images( com_handle,
                                            linux_images ,
                                            num_linux_images,
                                            eraseHB); // 1 for eraseHB
    		if (status != S_DONE)
    		{
    			return status;
    		}
        }
//======================================================
*/

/*
//======================================================
        //Send total format
        status = da_FormatFlash(com_handle,
                                HW_STORAGE_NAND,
                                NUTL_ERASE,
                                0,
                                0x00000000,
                                0x08000000);
//======================================================
*/
	}
    else
    {
        //Send NOR write image
		if( main_region_images->num_images > 0)
		{
    		log_output("Write NOR images...\n");
    		status = da_write_NOR_images(com_handle,
                                main_region_images,
                                4096);
    		if (status != S_DONE)
    		{
    			return status;
    		}
		}
	}

    // In 6595 DSDA project, Native downloader doesn't need to reset Modem.
    // Because the caller (android init flow) who call downloader will do that.
    // Android init flow sets kcol0& reset modem before it calls native downloader.
    // After native download is done, init flow resets modem again.

	//log_output("Enable Watch Dog...\n");
	//da_EnableWatchDog(com_handle, (unsigned short)(1000/15.6));

	//if (status != S_DONE)
    //{
    //    return status;
    //}

    return S_DONE;
}
STATUS_E download_images(const struct image *download_agent,
						 const struct image *download_agent_TCM,
                         const struct image *nor_flash_table,
                         const struct image *nand_flash_table,
						 const struct image *download_EPP,
                         Region_images *boot_region_images,
                         Region_images *main_region_images,
                         const struct ExternalMemorySetting *externalMemorySetting,
                         const unsigned int bmt_address,
						 int isUSB,
						 int isNFB,
						 unsigned int comPortNum)
{

    COM_HANDLE com_handle = INVALID_COM_HANDLE;
    STATUS_E status;
    int i = 0;

	if(isUSB)
    {

		while(1)
		{
			Sleep(100);
			if (com_open(&com_handle, 115200, comPortNum) == COM_STATUS_DONE)
			{
                log_output("handel (%d)\n", com_handle);
				break;
			}else{
				com_close(&com_handle);
			}

            if(i++ > 200)
            {
                log_output("ERROR : Failed to open the communication port, Timeout (20 sec)\n");
                return S_COM_PORT_OPEN_FAIL;
            }
		}

	}
    else
    {
		if (com_open(&com_handle, 115200, comPortNum) != COM_STATUS_DONE)
		{
			log_output("Failed to open the communication port\n");
			return S_COM_PORT_OPEN_FAIL;
		}
	}

    set_progress(10);
    status = bootrom_stage( com_handle,
                            download_agent,
                            download_agent_TCM,
                            download_EPP,
                            externalMemorySetting,
                            isUSB,
                            isNFB);

    if (status != S_DONE)
    {
        log_output("Download failed in BootROM stage: error=%u\n", status);
        com_close(&com_handle);
        return status;
    }

    set_progress(30);
    status = da_stage(  com_handle,
                        nor_flash_table,
                        nand_flash_table,
		                boot_region_images,
		                main_region_images,
		                isUSB,
		                isNFB,
		                bmt_address);

    if (status != S_DONE)
    {
        log_output("Download failed in DA stage: error=%u\n", status);
        com_close(&com_handle);
        return status;
    }

    if (com_close(&com_handle) != COM_STATUS_DONE)
    {
        log_output("Failed to close the communication port\n");
    }

    return S_DONE;
}
