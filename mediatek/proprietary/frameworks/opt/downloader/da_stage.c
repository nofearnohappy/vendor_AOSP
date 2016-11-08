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

#include "da_stage.h"
#include "_External/include/DOWNLOAD.H"
#include "image.h"
#include "_External/include/mtk_mcu.h"
#include "_External/include/flash_dev_tbl.h"
#include "_External/include/nand_dev_tbl.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#if defined(__GNUC__)
#include "GCC_Utility.h"
#endif



static unsigned short ComputeChecksum(const unsigned char *buf,
                                      unsigned int bufSize,
                                      unsigned short initialChecksum)
{
	unsigned int numWords;
	unsigned short *p;
	unsigned int wordIndex;
	unsigned short checksum;

    if(buf == NULL) return 0;
    if((bufSize % 2) != 0) return 0;

    numWords = bufSize / 2;
    p = (unsigned short *)buf;
    checksum = initialChecksum;

    for (wordIndex=0; wordIndex<numWords; ++wordIndex)
    {
        checksum ^= *(p + wordIndex);
    }


    return checksum;
}

static unsigned short GetImageChecksum(const unsigned char *p_temp,
                                       const unsigned int pkg_len)
{
    unsigned short init = 0;
    const unsigned char * first = p_temp;
    const unsigned char * last = p_temp + pkg_len;
    while ( first!=last )
    {
        init = init + *first++;
    }
    return init;
}

static unsigned short GetYaffsImageChecksum(const unsigned char *p_temp,
                                            const unsigned int pkg_len,
                                            const uint16 page_size,
                                            const uint16 spare_size)
{
	unsigned short check_sum = 0;
	unsigned int pagespare_len = page_size + spare_size;

    unsigned int j;
	for (j = 0; j < pkg_len; j += pagespare_len) {
	    unsigned int i;
		for (i = 0; i < page_size; i++) {
			check_sum += *(p_temp+i);
		}
		p_temp += pagespare_len;
	}
    return check_sum;

}

static STATUS_E detect_flash(COM_HANDLE com_handle,
                             const struct image *flash_table,
                             unsigned int entry_size)
{
    const unsigned int table_size = flash_table->len / entry_size;
    unsigned char response = 0;
    unsigned int i;

    if (com_send_dword(com_handle, table_size) != COM_STATUS_DONE)
    {
        return 300;
    }

    if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 301;
    }

    if (response != ACK)
    {
        return 302;
    }

    for (i=0; i<table_size; ++i)
    {
        if (com_send_data(com_handle, &flash_table->buf[i*entry_size],
			entry_size) != COM_STATUS_DONE)
        {
            return 303;
        }

        if (com_recv_data_chk_len(com_handle, &response,1) != COM_STATUS_DONE)
        {
            return 304;
        }

        if (response == ACK)
        {
            break;
        }
    }

    return S_DONE;
}

static STATUS_E get_CBRinfo(COM_HANDLE com_handle, struct GFH_CBR_INFO_v2* p_cbrVersionInfo)
{
    unsigned int ret = 0;
	//CBR information
	unsigned int startBlockOfCBR = 0;
    unsigned int numNormalBlock = 0;
    unsigned int numFaskBlock = 0;
    unsigned int numSpareBlock = 0;
    unsigned int numMaxRecordInBlock = 0;
    unsigned int numTotalBlock = 0;
    unsigned char bReadOnlyMode = 1;
    unsigned int target_cbr_version=0;

	//Get Setting of CBR
	if( 0 != (ret = SV5_CMD_GetSettingOfCBR(com_handle, &startBlockOfCBR,&numNormalBlock, &numFaskBlock, &numSpareBlock,
		&numMaxRecordInBlock, &numTotalBlock, (unsigned char *)&bReadOnlyMode, &target_cbr_version)))
	{
		const unsigned int numMinSpareBlock = p_cbrVersionInfo->m_cbr_spare_block_num;
        const unsigned int numMaxSpareBlock = p_cbrVersionInfo->m_cbr_spare_block_num;
		unsigned int load_cbr_version = (p_cbrVersionInfo->m_gfh_hdr.m_magic_ver>>24);
		numNormalBlock = p_cbrVersionInfo->m_cbr_normal_block_num;
        numFaskBlock = 0;
        numMaxRecordInBlock = 16;
        bReadOnlyMode = 0;

		if(ret != S_DA_CBR_NOT_FOUND)
        {
			return ret;
		}


		if(0 == numNormalBlock)
        {
            // ex: MT6251 may not have CBR
            //LOG_WARNING("No CBR is created.");
        }
        else
        {
			if( 0 != (ret = SV5_CMD_CreateCBR(com_handle, numNormalBlock,
                            numFaskBlock, numMinSpareBlock,
                            numMaxSpareBlock, numMaxRecordInBlock,
                            bReadOnlyMode, load_cbr_version) ) )
            {
                return ret;
            }
		}

	}
    else
    {
		// CBR exists. Therefore, we should compare the setting
        // between the flash and Primary MAUI image.
		if((p_cbrVersionInfo->m_gfh_hdr.m_magic_ver>>24) != target_cbr_version)
        {
			return S_DA_CBR_VERSION_NOT_MATCHED;
		}

		if ((p_cbrVersionInfo->m_cbr_normal_block_num != numNormalBlock) ||
                        (p_cbrVersionInfo->m_cbr_spare_block_num != numSpareBlock))
        {
            return S_DA_CBR_COMPARE_FAILED;
        }
	}
    return S_DONE;
}


STATUS_E da_connect(COM_HANDLE com_handle,
                    const struct image *nor_flash_table,
                    const struct image *nand_flash_table,
                    unsigned int bmt_address)
{
	unsigned char chip_type = 0; int i=0;
    DWORD start_time, cur_time;

    Sleep(300);    // Wait for DA to initialize itself

    // Get SYNC_CHAR
    start_time = GetTickCount();  // setup start timestamp
    while (1)
    {
        COM_STATUS com_status;
        unsigned char response = 0;
        log_output("Get SYNC_CHAR ");
        com_status = com_recv_byte_without_retry(com_handle, &response);
        log_output("%02X ", response);

        if (com_status == COM_STATUS_DONE)
        {
            if (response == SYNC_CHAR)
            {
                break;
            }
            else
            {
                continue;
            }
        }
        else if (com_status == COM_STATUS_READ_TIMEOUT)
        {
            continue;
        }
        else
        {
            return S_FT_DA_INIT_SYNC_ERROR;
        }

        cur_time = GetTickCount();  // get cur timestamp

        if( (cur_time-start_time) >= TIMEOUT_10S) {
            log_output("[ERROR] Wait DA response timeout!!\n");
            return COM_STATUS_READ_TIMEOUT;
        }
    }

    // Get DA version
    {
        unsigned char version[2] = { 0 };

        if (com_recv_data(com_handle, version, 2) != COM_STATUS_DONE)
        {
            return S_FT_DA_INIT_SYNC_ERROR;
        }

        if ((version[0] != 0x03) || (version[1] != 0x02))
        {
            return S_FT_DA_VERSION_INCORRECT;
        }
    }

    // Get BB chip type
    {
        if (com_recv_byte(com_handle, &chip_type) != COM_STATUS_DONE)
        {
            return 601;
        }

        log_output("DA return BB = %d \n", chip_type);

        // This BROM Lite is restricted for MT6280
        if (chip_type != MT6260 && chip_type != MT6261)
        {
            return S_UNKNOWN_TARGET_BBCHIP;
        }
    }

	// Trigger BAT_ON check for USBDL W/O Battery
	//send NAK
	if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
    {
        return 602;
    }

    // Send BootROM version and bootloader version
    {
        // send brom version
        if (com_send_byte(com_handle, 0x05) != COM_STATUS_DONE)
        {
            return 603;
        }
        // send BLOADER version
        if (com_send_byte(com_handle, 0xFE) != COM_STATUS_DONE)
        {
            return 604;
        }
    }

    // Send chip select
    {
        if (com_send_byte(com_handle, CS0) != COM_STATUS_DONE)
        {
            return 605;
        }

        if (com_send_byte(com_handle, CS_WITH_DECODER) != COM_STATUS_DONE)
        {
            return 606;
        }

        if (com_send_byte(com_handle, CS0) != COM_STATUS_DONE)
        {
            return 607;
        }
    }

    // Send NFI_ACCCON setting
    {
		if (com_send_dword(com_handle, 0x7007ffff) != COM_STATUS_DONE) //for MT6235B
		{
			return 608;
		}
    }

    // Send clock setting
    {
        if (com_send_byte(com_handle, EXT_26M) != COM_STATUS_DONE)
        {
            return 609;
        }
    }


    // Detect NOR flash
    log_output("DA starts to detect NOR/SF flash \n");
    {
        int ret = 0;
        const unsigned int entry_size = sizeof(NOR_Device_S);
        if ( (ret = detect_flash(com_handle, nor_flash_table, entry_size)) != S_DONE)
        {
            return ret;
        }
    }

    // Detect NAND flash
	{
        unsigned char response = 0;
        unsigned int bmt_result = 0;

        if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
        {
            return 610;
        }

        if (response == ACK)
        {
            const unsigned int entry_size = sizeof(NAND_Device_S);

            log_output("DA starts to detect NAND flash \n");

            // Send the begin address of BMT Pool for linux partition download
            if (com_send_dword(com_handle, bmt_address) != COM_STATUS_DONE)
            {
                return 611;
            }

            if (detect_flash(com_handle, nand_flash_table,
				entry_size) != S_DONE)
            {
                return 612;
            }

            //Read Init BMT result
            if (com_recv_dword(com_handle, &bmt_result) != COM_STATUS_DONE)
            {
                return 613;
            }


        }
    }

    log_output("Get DA report from target. \n");

    //Sleep(3000); // Wait for DA to detect external DRAM size

    // Get HW detection report
    {
        DA_REPORT_T report = { 0 };
        unsigned char tmp[2] = { 0 };
        int i=0; //counter

        //
        // NOR flash report
        //
        if (com_recv_dword(com_handle, &report.m_nor_ret))
        {
            return 614;
        }

        if (com_recv_data(com_handle, tmp, 2))
        {
            return 615;
        }

        report.m_nor_chip_select[0] = tmp[0];
        report.m_nor_chip_select[1] = tmp[1];

        if (com_recv_word(com_handle,
			&report.m_nor_flash_id) != COM_STATUS_DONE)
        {
            return 616;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_size) != COM_STATUS_DONE)
        {
            return 617;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_size_die1) != COM_STATUS_DONE)
        {
            return 618;
        }



        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_1) != COM_STATUS_DONE)
        {
            return 619;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_2) != COM_STATUS_DONE)
        {
            return 620;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_3) != COM_STATUS_DONE)
        {
            return 621;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_4) != COM_STATUS_DONE)
        {
            return 622;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_otp_status) != COM_STATUS_DONE)
        {
            return 623;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_otp_size) != COM_STATUS_DONE)
        {
            return 624;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_id_die2) != COM_STATUS_DONE)
        {
            return 625;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_size_die2) != COM_STATUS_DONE)
        {
            return 626;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_1_die2) != COM_STATUS_DONE)
        {
            return 627;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_2_die2) != COM_STATUS_DONE)
        {
            return 628;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_3_die2) != COM_STATUS_DONE)
        {
            return 629;
        }

        if (com_recv_word(com_handle,
			&report.m_nor_flash_dev_code_4_die2) != COM_STATUS_DONE)
        {
            return 630;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_otp_status_die2) != COM_STATUS_DONE)
        {
            return 631;
        }

        if (com_recv_dword(com_handle,
			&report.m_nor_flash_otp_size_die2) != COM_STATUS_DONE)
        {
            return 632;
        }

        //
        // NAND flash report
        //
        if (com_recv_dword(com_handle, &report.m_nand_ret))
        {
            return 633;
        }

        if (com_recv_byte(com_handle, &tmp[0]))
        {
            return 634;
        }

        report.m_nand_chip_select = tmp[0];

        if (com_recv_word(com_handle,
			&report.m_nand_flash_id) != COM_STATUS_DONE)
        {
            return 635;
        }

        if (com_recv_dword(com_handle,
			&report.m_nand_flash_size) != COM_STATUS_DONE)
        {
            return 636;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_1) != COM_STATUS_DONE)
        {
            return 637;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_2) != COM_STATUS_DONE)
        {
            return 638;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_3) != COM_STATUS_DONE)
        {
            return 639;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_4) != COM_STATUS_DONE)
        {
            return 640;
        }

		//add second NAND info
		if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_1_part2) != COM_STATUS_DONE)
        {
            return 641;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_2_part2) != COM_STATUS_DONE)
        {
            return 642;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_3_part2) != COM_STATUS_DONE)
        {
            return 643;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_flash_dev_code_4_part2) != COM_STATUS_DONE)
        {
            return 644;
        }


        if (com_recv_word(com_handle,
			&report.m_nand_pagesize) != COM_STATUS_DONE)
        {
            return 645;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_sparesize) != COM_STATUS_DONE)
        {
            return 646;
        }

        if (com_recv_word(com_handle,
			&report.m_nand_pages_per_block) != COM_STATUS_DONE)
        {
            return 647;
        }

        if (com_recv_data(com_handle, tmp, 2))
        {
            return 648;
        }

        report.m_nand_io_interface = tmp[0];
        report.m_nand_addr_cycle = tmp[1];

        //detect EMMC for SV5
        if (com_recv_dword(com_handle,
			&report.m_emmc_ret) != COM_STATUS_DONE)
        {
            return 649;
        }

        if (com_recv_byte(com_handle,
			&report.m_emmc_manufacture_id) != COM_STATUS_DONE)
        {
            return 650;
        }

		for(i = 0 ; i < 6 ; i ++)
		{
            if (com_recv_byte(com_handle,
                &report.m_emmc_product_name[i]) != COM_STATUS_DONE)
            {
                return 651;
            }

		}
        report.m_emmc_product_name[6] = '\0';

        if (com_recv_byte(com_handle,
			&report.m_emmc_partitioned) != COM_STATUS_DONE)
        {
            return 652;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_boot1_size) != COM_STATUS_DONE)
        {
            return 653;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_boot2_size) != COM_STATUS_DONE)
        {
            return 654;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_rpmb_size) != COM_STATUS_DONE)
        {
            return 655;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_gp1_size) != COM_STATUS_DONE)
        {
            return 656;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_gp2_size) != COM_STATUS_DONE)
        {
            return 657;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_gp3_size) != COM_STATUS_DONE)
        {
            return 658;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_gp4_size) != COM_STATUS_DONE)
        {
            return 659;
        }
        if (com_recv_dword(com_handle,
			&report.m_emmc_ua_size) != COM_STATUS_DONE)
        {
            return 660;
        }
        // SV3
	    //report.m_emmc_ret = S_DA_EMMC_FLASH_NOT_FOUND; // for SV3, no EMMC support
		//report.m_emmc_partitioned = 0;

        //
        // Internal RAM report
        //
        if (com_recv_dword(com_handle,
			&report.m_int_sram_ret) != COM_STATUS_DONE)
        {
            return 661;
        }

        if (com_recv_dword(com_handle,
			&report.m_int_sram_size) != COM_STATUS_DONE)
        {
            return 662;
        }

        //
        // External RAM report
        //
        if (com_recv_dword(com_handle,
			&report.m_ext_ram_ret) != COM_STATUS_DONE)
        {
            return 663;
        }

        if (com_recv_data(com_handle, tmp, 2))
        {
            return 664;
        }

        report.m_ext_ram_type = tmp[0];
        report.m_ext_ram_chip_select = tmp[1];

        if (com_recv_dword(com_handle,
			&report.m_ext_ram_size) != COM_STATUS_DONE)
        {
            return 665;
        }

        //for SV5 read SF_DetectionTable
		for(i=0;i<12;i++)
		{
			if (com_recv_data(com_handle, tmp, 1))
			{
				return 666;
			}
		}

        // read ACK for check all data is sent
        if (com_recv_byte(com_handle, &tmp[0]) != COM_STATUS_DONE)
        {
            return 667;
        }
    }

    log_output("Get DA report done. \n");

    // Wait for DA to scan flash content
    {
        unsigned char response = 0;


        if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
            {
            return 668;
            }

        if (response != ACK)
            {
                return 668;
            }
        }

    // Get download status
    {
        unsigned int download_status = 0;

        if (com_recv_dword(com_handle, &download_status) != COM_STATUS_DONE)
        {
            return 669;
        }
    }

    // Get boot style
    {
        unsigned int boot_style = 0;

        if (com_recv_dword(com_handle, &boot_style) != COM_STATUS_DONE)
        {
            return 670;
        }
    }

    // Get SOC check result
    {
        unsigned char soc_check_result = 0;

        if (com_recv_byte(com_handle, &soc_check_result) != COM_STATUS_DONE)
        {
            return 671;
        }
    }

    log_output("Sync with DA is done. \n");
    return S_DONE;
}


STATUS_E SV5_CMD_CheckBootLoaderFeature_CheckLoadType(COM_HANDLE com_handle,
										const struct image *img,
										Feature_Check_Type checkType)
{
	unsigned int status;
	unsigned int target_feature_combination;
    unsigned int target_bl_maui_paired_ver;

	// 1. Send command
    if (com_send_byte(com_handle, DA_BL_FEATURE_CHECK_CMD) != COM_STATUS_DONE)
    {
        return 950;
    }
	// 2. send checking method: get feature from target's BL or MAUI
	if (com_send_dword(com_handle, checkType) != COM_STATUS_DONE)
    {
        return 951;
    }
	//
    //  3. Wait for response
    //
	if (com_recv_dword(com_handle, &status) != COM_STATUS_DONE)
	{
		return 952;
	}
	if (status != S_DONE)
    {
        //Feature info not found %s, target flash is empty
        log_output("Feature info(%d) not found, target flash is empty \n",checkType);
		return S_DONE;
	}
	// 4. Get feature from target


    if (com_recv_dword(com_handle, &target_feature_combination) != COM_STATUS_DONE)
	{
		return 953;
	}

    if (com_recv_dword(com_handle, &target_bl_maui_paired_ver) != COM_STATUS_DONE)
	{
		return 954;
	}

    //Skip to get feature info from PC's ARM_BL
    //No check features between PC's and target's ARM_BL

    return S_DONE;
}

STATUS_E da_write_boot_loader(COM_HANDLE com_handle,
							  Region_images *boot_region_images,
							  const int isNFB)
{
    const unsigned int packet_len = 4096;
    unsigned char response = 0;
    unsigned int da_ret = 0;
    unsigned int bl_status = 0;
    unsigned int i, j;
	unsigned int num_bytes_sent;
	unsigned int num_of_bl = 0;
    unsigned int num_of_download_bl = 0;
	unsigned int offset;
    unsigned char flashtype = 0;
    unsigned char token = ACK;
    //struct image *bootLoader = NULL;
	//struct image *extBootLoader = NULL;

	GFH_FILE_INFO_v1 bootLoader_gfh_file_info[4];
	GFH_BL_INFO_v1	bootLoader_gfh_bl_info[4];

    if(boot_region_images->num_images > 4)
    {
        // currently, Native downloader doesn't support that number of boot region images is over 4
        return 800;
    }

    // find BL, EXT_BL image from boot_region_images, and parse GFH for each images
    for(i = 0 ; i < boot_region_images->num_images ; i ++)
    {
        /*if(boot_region_images->region_images[i].gfh_file_type == ARM_BL)
        {
            bootLoader = &(boot_region_images->region_images[i]);
        }

        if(boot_region_images->region_images[i].gfh_file_type == ARM_EXT_BL)
        {
            extBootLoader = &(boot_region_images->region_images[i]);
        }*/

        // Parse GFH from boot region images
        offset =0;
        memcpy(&(bootLoader_gfh_file_info[i]), boot_region_images->region_images[i].buf, sizeof(GFH_FILE_INFO_v1));
    	//Get GFH_BL_INFO_v1
    	for(j=0;j<10;j++)
    	{
    		memcpy(&(bootLoader_gfh_bl_info[i]), boot_region_images->region_images[i].buf+offset, sizeof(GFH_BL_INFO_v1));

    		if(bootLoader_gfh_bl_info[i].m_gfh_hdr.m_type == GFH_BL_INFO)
    		{
    			break;
    		}else{
    			offset += bootLoader_gfh_bl_info[i].m_gfh_hdr.m_size;
    			if(j==9)
    			{
    				return 801;
    			}
    		}
    	}
    }


    num_of_bl = boot_region_images->num_images;


    // Parse GFH from boot region
    /*if(bootLoader != NULL)
    {
        offset =0;
        memcpy(&bootLoader_gfh_file_info[0], bootLoader->buf, sizeof(GFH_FILE_INFO_v1));
    	//Get GFH_BL_INFO_v1
    	for(i=0;i<10;i++)
    	{
    		memcpy(&bootLoader_gfh_bl_info[0], bootLoader->buf+offset, sizeof(GFH_BL_INFO_v1));

    		if(bootLoader_gfh_bl_info[0].m_gfh_hdr.m_type == GFH_BL_INFO)
    		{
    			break;
    		}else{
    			offset += bootLoader_gfh_bl_info[0].m_gfh_hdr.m_size;
    			if(i==9)
    			{
    				return 800;
    			}
    		}
    	}
        num_of_bl = 1;
        num_of_download_bl ++;
    }
    if(extBootLoader != NULL)
    {
        offset =0;
        memcpy(&bootLoader_gfh_file_info[1], extBootLoader->buf, sizeof(GFH_FILE_INFO_v1));
    	for(i=0;i<10;i++)
    	{
    		memcpy(&bootLoader_gfh_bl_info[1], extBootLoader->buf+offset, sizeof(GFH_BL_INFO_v1));

    		if(bootLoader_gfh_bl_info[1].m_gfh_hdr.m_type == GFH_BL_INFO)
    		{
    			break;
    		}else{
    			offset += bootLoader_gfh_bl_info[1].m_gfh_hdr.m_size;
    			if(i==9)
    			{
    				return 801;
    			}
    		}
    	}
        num_of_bl = 2;
        num_of_download_bl ++;
    }*/

	// send Format HB command
    log_output("da_write_boot_loader:: send Format HB command\n");
    if (com_send_byte(com_handle, DA_FORMAT_HB) != COM_STATUS_DONE)
    {
        return 802;
    }
    log_output("da_write_boot_loader:: send Flash Dev\n");
	// send Flash Dev
	// 0x07 for SF
	// 0x09 for SPI NAND
	if(isNFB)
	{
        log_output("Set flash type: SPI NAND\n");
        flashtype = 0x09;
    }
    else
    {
        log_output("Set flash type: SF\n");
        flashtype = 0x07;
    }
	if (com_send_byte(com_handle, flashtype) != COM_STATUS_DONE)
    {
        return 803;
    }
    log_output("da_write_boot_loader 0x%x\n", flashtype);
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 804;
    }
	log_output("da_write_boot_loader com_recv_byte response 0x%0x\n",response);
    if (response != ACK)
    {
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 805;
        }
		log_output("da_write_boot_loader com_recv_byte response 0x%04x\n",da_ret);
        log_output("[WARNING]Format HB error!!! err(%d)\n", da_ret);
    }


	// send DA_DOWNLOAD_BLOADER_CMD
	log_output("da_write_boot_loader::DA_DOWNLOAD_BLOADER_CMD(0x%02X).\n",DA_DOWNLOAD_BLOADER_CMD);
    if (com_send_byte(com_handle, DA_DOWNLOAD_BLOADER_CMD) != COM_STATUS_DONE)
    {
        return 806;
    }
	log_output("da_write_boot_loader::  check code region access right..");
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 807;
    }
	log_output("da_write_boot_loader DA_DOWNLOAD_BLOADER_CMD response 0x%0x\n",response);
    if (response != ACK)
    {
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 808;
        }
        return da_ret;
    }

	// send packet length
	log_output("da_write_boot_loader:: send PACKET SIZE(0x%08X)\n",packet_len);
	if (com_send_dword(com_handle, packet_len) != COM_STATUS_DONE)
    {
        return 809;
    }

	//- 1.2. send flash_dev type
    log_output("da_write_boot_loader send Flash Device Type(0x%02X)\n",flashtype);
	if (com_send_byte(com_handle, flashtype) != COM_STATUS_DONE)
    {
        return 810;
    }
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 811;
    }
	log_output("da_write_boot_loader  check Flash Device Type... response 0x%x\n",response);
    if (response != ACK)
    {
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 812;
        }
	log_output("da_write_boot_loader send 0x%x com_recv_dword 0x%4x\n",flashtype, da_ret);
        return da_ret;
    }
    //check Header Block...
    log_output("da_write_boot_loader:: check Header Block..\n");
	if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 813;
    }
	  log_output("da_write_boot_loader check Header Block response 0x%x\n",response);
    if (response == ACK)
	{
	  log_output("da_write_boot_loader:: 1st download ...\n");
		//TRACE("DA_cmd::CMD_DownloadBootLoader(): 1st download ... \n");
	}else if (response == CONT_CHAR)
	{
	 log_output("da_write_boot_loader:: updating ... \n");
		//TRACE("DA_cmd::CMD_DownloadBootLoader(): updating ... \n");
	}else
    {
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 814;
        }
        return da_ret;
    }

	//2. Send num of BLs
    log_output("da_write_boot_loader: send BL Numers(0x%08X).\n",num_of_bl);
	if (com_send_dword(com_handle, num_of_bl) != COM_STATUS_DONE)
    {
        return 815;
    }

    //3. Send DA need format/check HB in target or not
    token = ACK;
    log_output("da_write_boot_loader ask DA to format HB (0x%02X)\n",token);
	if (com_send_byte(com_handle, token) != COM_STATUS_DONE)
    {
        return 810;
    }
    token = NACK;
    log_output("da_write_boot_loader ask DA to check HB exist or not (0x%02X)\n",token);
	if (com_send_byte(com_handle, token) != COM_STATUS_DONE)
    {
        return 810;
    }
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 811;
    }
	log_output("da_write_boot_loader  DA check HB... response 0x%x\n",response);
    if (response != ACK)
    {
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 812;
        }
	    log_output("da_write_boot_loader DA check HB... response error (com_recv_dword 0x%4x)\n", da_ret);
        return da_ret;
    }

     set_progress(38);
	//Sleep(1000);

	//4. Get BL Profiles
	for(i=0; i<num_of_bl; i++)
	{
		// 4.1 BL_EXIST_MAGIC
                log_output("da_write_boot_loader : send BL[%d] - BL_EXIST_MAGIC(0x%02X).\n", i, ACK);
		if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
		{
			return 816;
		}

		// 4.2 BL_DEV
                log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - BL_DEV(0x%02X).\n", i, flashtype);
		if (com_send_byte(com_handle, flashtype) != COM_STATUS_DONE)
		{
			return 817;
		}
		// 4.3 BL_TYPE
                log_output("da_write_boot_loader send BL[%d] - BL_TYPE(0x%04X).\n", i, bootLoader_gfh_file_info[i].m_file_type);//ARM_EXT_BL_BACKUP
		if (com_send_word(com_handle, bootLoader_gfh_file_info[i].m_file_type) != COM_STATUS_DONE)
		{
			return 818;
		}
		// 4.4 BL_BEGIN_DEV_ADDR
                log_output("da_write_boot_loader:: send BL[%d] - BL_BEGIN_DEV_ADDR(load addr)(0x%08X).\n", i, bootLoader_gfh_file_info[i].m_load_addr);
		if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_load_addr) != COM_STATUS_DONE)
		{
			return 819;
		}

		if(bootLoader_gfh_file_info[i].m_max_size == 0xFFFFFFFF)
		{
                        log_output("da_write_boot_loader:: send BL[%d] - BL_BOUNDARY_DEV_ADDR(load addr+file_len)(0x%08X).\n", i, bootLoader_gfh_file_info[i].m_load_addr+bootLoader_gfh_file_info[i].m_file_len);
			if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_load_addr+bootLoader_gfh_file_info[i].m_file_len) != COM_STATUS_DONE)
			{
				return 820;
			}
		}else{
                        log_output("da_write_boot_loader:: send BL[%d] - BL_BOUNDARY_DEV_ADDR(load addr+file_len)(0x%08X).\n", i, bootLoader_gfh_file_info[i].m_load_addr+bootLoader_gfh_file_info[i].m_max_size);
			if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_load_addr+bootLoader_gfh_file_info[i].m_max_size) != COM_STATUS_DONE)
			{
				return 821;
			}
		}

		// 4.6 BL_ATTRIBUTE
		log_output("da_write_boot_loader::: send BL[%d] - BL_ATTRIBUTE(0x%08X)\n.", i, bootLoader_gfh_bl_info[i].m_bl_attr);
		if (com_send_dword(com_handle, bootLoader_gfh_bl_info[i].m_bl_attr) != COM_STATUS_DONE)
		{
			return 822;
		}

		// 4.7 BL_MAX_SIZE
		log_output("da_write_boot_loader:::  send BL[%d] - BL_MAX_SIZE(0x%08X).\n", i,bootLoader_gfh_file_info[i].m_max_size);
		if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_max_size) != COM_STATUS_DONE)
		{
			return 823;
		}

		// 4.8 AC_C_Enable
		 log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - AC_C enable (0).\n", i);
		if (com_send_byte(com_handle, 0) != COM_STATUS_DONE)
		{
			return 824;
		}
		// 4.9 AC Offset
		log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - AC Final Offset (0) = FILE_CONTENT_OFFSET (0x%08X) + AC Offset (0).\n", i, bootLoader_gfh_file_info[i].m_content_offset + 0);
		if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_content_offset + 0) != COM_STATUS_DONE)
		{
			return 825;
		}
		// 4.10 AC Length
		log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - AC Length (0).\n", i);
		if (com_send_dword(com_handle, 0) != COM_STATUS_DONE)
		{
			return 826;
		}

		//----- EXT INFO ----
        // 4.11 BL_SIZE
                log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - BL_SIZE(0x%08X).", i, bootLoader_gfh_file_info[i].m_file_len);//p_bl_ext_descriptor->m_bl_size);
		if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_file_len) != COM_STATUS_DONE)
		{
			return 827;
		}
		// 4.12 BL_RESERVED_BLOCK
		log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - BL_RESERVED_BLOCK(0).\n", i);//p_bl_ext_descriptor->m_bl_reserved);
		if (com_send_byte(com_handle, 0) != COM_STATUS_DONE)
		{
			return 828;
		}
		// 4.13 BL_ALIGN_TYPE
		log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - BL_ALIGN_TYPE(0).", i);//p_bl_ext_descriptor->m_bl_align_type);
		if (com_send_byte(com_handle, 0) != COM_STATUS_DONE)
		{
			return 829;
		}
		// 4.14 BL_FILEINFO_ATTR
		 log_output("DA_cmd::CMD_DownloadBootLoader(): send BL[%d] - BL_FILEINFO_ATTR(0x%02X).\n", i, bootLoader_gfh_file_info[i].m_attr);
		if (com_send_dword(com_handle, bootLoader_gfh_file_info[i].m_attr) != COM_STATUS_DONE)
		{
			return 830;
		}

		//---- Check result ----
        // read ack
                log_output("DA_cmd::CMD_DownloadBootLoader(): wait for ACK.");
		if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
		{
			return 831;
		}
		if (response != ACK)
		{
                    if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
                    {
                        return 832;
                    }
                    return da_ret;

		}
		log_output("DA_cmd::CMD_DownloadBootLoader(): ACK(0x%02X) OK!", response);
	}

	//5. Set BL Profiles
	// read ack
	log_output("DA_cmd::CMD_DownloadBootLoader(): Set BL Profiles - wait for ACK.");
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
	{
		return 833;
	}
	if (response != ACK)
	{
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 834;
        }
        return da_ret;

	}
	 log_output("DA_cmd::CMD_DownloadBootLoader(): Set BL Profiles - ACK(0x%02X) OK!\n",response);
	// 5.5 wait for BL Self update
    // read ack

	 log_output("DA_cmd::CMD_DownloadBootLoader(): BL Self Update Check - wait for ACK.\n");
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
	{
		return 835;
	}
	if (response != ACK)
	{
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 836;
        }
        return da_ret;
	}
	log_output("DA_cmd::CMD_DownloadBootLoader(): BL Self Update Check - ACK(0x%02X) OK!\n", response);


    set_progress(40);
	//6. Send BL
	//send boot loader
	for(i=0; i<num_of_bl; i++)
	{
		num_bytes_sent = 0;
		while (num_bytes_sent < boot_region_images->region_images[i].len)
		{
			unsigned int j;
			unsigned short checksum;
			unsigned int num_bytes_to_send;
			unsigned short dummy_checksum = 0;
            DWORD start_time, cur_time;

			checksum = 0;

			if (packet_len > (boot_region_images->region_images[i].len - num_bytes_sent))
			{
				num_bytes_to_send = boot_region_images->region_images[i].len - num_bytes_sent;
			}
			else
			{
				num_bytes_to_send = packet_len;
			}

			if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
			{
				return 837;
			}

			if (com_send_data(com_handle, boot_region_images->region_images[i].buf + num_bytes_sent,
				num_bytes_to_send) != COM_STATUS_DONE)
			{
				return 838;
			}

			// calculate checksum
			for(j=0; j<num_bytes_to_send; j++) {
				// WARNING: MUST make sure it unsigned value to do checksum
				checksum += boot_region_images->region_images[i].buf[num_bytes_sent+j];
                //log_output("%x ", blImage->buf[num_bytes_sent+j]);
			}

			if (com_send_word(com_handle, checksum) != COM_STATUS_DONE)
			{
				return 839;
			}

            start_time = GetTickCount();    // setup start timestamp
			while (1)
			{
				// TODO: error handling
				if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
				{
					continue;
				}

				if (response == CONT_CHAR)
				{
					break;
				}
				else if((response == NACK)||(response == STOP_CHAR))
				{
					return 840;
				}

                cur_time = GetTickCount();  // get cur timestamp

                // check if exceed timeout value
                if( (cur_time-start_time) >= TIMEOUT_10S) {
                    log_output("[ERROR] Wait DA response timeout in write BLs!!\n");
                    return COM_STATUS_READ_TIMEOUT;
                }
			}

			num_bytes_sent += num_bytes_to_send;
		}

        set_progress(40+ (8/num_of_bl)*(i+1));
	}

	//7. download finish
    // read ack
        log_output("DA_cmd::CMD_DownloadBootLoader(): wait for post-process of download bootloader.\n");
	if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
	{
		return 841;
	}
	 log_output("da_write_boot_loader download finish response is 0x%0x\n",response);
	if (response != ACK)
	{
    	if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
        {
            return 842;
        }
        log_output("DA_cmd::CMD_DownloadBootLoader(): error code: (%d). ",da_ret);
        return da_ret;

	}
    else
    {
    	if (com_recv_dword(com_handle, &bl_status) != COM_STATUS_DONE)
        {
            return 843;
        }
        log_output("da_write_boot_loader download finish response status is 0x%04x\n",bl_status);
    }
	return S_DONE;
}


STATUS_E SV5_CMD_GetSettingOfCBR(COM_HANDLE com_handle, unsigned int *startBlockOfCBR,
                                    unsigned int *numNormalBlock, unsigned int *numFastlBlock,
                                    unsigned int *numSpareBlock, unsigned int *numMaxRecordInBlock,
	                                        unsigned int *numTotalBlock, unsigned char *bReadOnlyMode,
	                                        unsigned int *cbr_version)
{
	unsigned char response = 0;
	unsigned int errorCode;

	if (com_send_byte(com_handle, DA_GET_CBR_SETTTING_CMD) != COM_STATUS_DONE)
    {
        return 940;
    }
	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 941;
    }
    if (response != ACK)
    {

		if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
		{
			return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
		}
        return errorCode;
    }

	if (com_recv_dword(com_handle, startBlockOfCBR) != COM_STATUS_DONE)
	{
		return 942;
	}
	if (com_recv_dword(com_handle, numNormalBlock) != COM_STATUS_DONE)
	{
		return 943;
	}
	if (com_recv_dword(com_handle, numFastlBlock) != COM_STATUS_DONE)
	{
		return 944;
	}
	if (com_recv_dword(com_handle, numSpareBlock) != COM_STATUS_DONE)
	{
		return 945;
	}
	if (com_recv_dword(com_handle, numMaxRecordInBlock) != COM_STATUS_DONE)
	{
		return 946;
	}
	if (com_recv_dword(com_handle, numTotalBlock) != COM_STATUS_DONE)
	{
		return 947;
	}
	if (com_recv_byte(com_handle, bReadOnlyMode) != COM_STATUS_DONE)
	{
		return 948;
	}
	if (com_recv_dword(com_handle, cbr_version) != COM_STATUS_DONE)
	{
		return 949;
	}

	return 0;
}

STATUS_E SV5_CMD_CreateCBR(COM_HANDLE com_handle, unsigned int numNormalBlock,
                                    unsigned int numFastlBlock, unsigned int numMinSpareBlock,
                                    unsigned int numMaxSpareBlock, unsigned int numMaxRecordInBlock,
	                                        unsigned char bReadOnlyMode,  unsigned int cbr_version)
{
	unsigned char response = 0;
	unsigned int errorCode;

	if (com_send_byte(com_handle, DA_CREATE_CBR_CMD) != COM_STATUS_DONE)
    {
        return 1;
    }

	if (com_send_dword(com_handle, numNormalBlock) != COM_STATUS_DONE)
	{
		return 4;
	}
	if (com_send_dword(com_handle, numFastlBlock) != COM_STATUS_DONE)
	{
		return 5;
	}
	if (com_send_dword(com_handle, numMinSpareBlock) != COM_STATUS_DONE)
	{
		return 6;
	}
	if (com_send_dword(com_handle, numMaxSpareBlock) != COM_STATUS_DONE)
	{
		return 7;
	}
	if (com_send_dword(com_handle, numMaxRecordInBlock) != COM_STATUS_DONE)
	{
		return 8;
	}
	if (com_send_byte(com_handle, bReadOnlyMode) != COM_STATUS_DONE)
	{
		return 10;
	}
	if (com_send_dword(com_handle, cbr_version) != COM_STATUS_DONE)
	{
		return 11;
	}

	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 12;
    }
    if (response != ACK)
    {
		if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
		{
			return 13;
		}
    }

	return 0;
}
#define NUM_OF_ROM 4
STATUS_E da_write_NFB_images(COM_HANDLE com_handle,
                             Region_images *main_region_images,
                             const unsigned int packet_length)
{
    unsigned char response = 0;
    unsigned int i,j;
	unsigned int m_chksum;
	unsigned int num_bytes_sent;
	int ret = S_DONE;
	unsigned int errorCode = 0;
	unsigned int offset = 0;

    //Flash Layout
    unsigned int regionCount = 0;
    unsigned int binaryType = 0;
    unsigned int startPage = 0;
    unsigned int boundPage = 0;



	GFH_CBR_INFO_v2 cbrVersionInfo;

    memset(&cbrVersionInfo, 0, sizeof(GFH_CBR_INFO_v2));

	for(i=0; i<main_region_images->num_images; i++)
	{
        if(main_region_images->region_images[i].gfh_file_type ==  PRIMARY_MAUI)
        {
            GFH_FILE_INFO_v1 gfh_file_info;
            offset =0;
            j = 0;
            //Get GFH file info
            while(1)
            {
                memcpy(&gfh_file_info, main_region_images->region_images[i].buf + offset, sizeof(GFH_FILE_INFO_v1));
                //Get CBR version info from main ROM bin.
    		    if(gfh_file_info.m_gfh_hdr.m_type == GFH_CBR_INFO)
    		    {
                    memcpy(&cbrVersionInfo, main_region_images->region_images[i].buf + offset, sizeof(GFH_CBR_INFO_v2));
    			    break;
    		    }
                else
                {
    			    offset += gfh_file_info.m_gfh_hdr.m_size;
    			    if(j==9)
    			    {
    				    return 900;
    			    }
    		    }
                j++;
            }

        }
	}


	//1. Format CBR Record( done at beginning)

	// 2. Download MAUI

	//Get Setting of CBR
    if( 0 != (ret = get_CBRinfo(com_handle, &cbrVersionInfo) ) )
    {
        return ret;
    }


	// 2. set image list
	// set image list
	if (com_send_byte(com_handle, DA_NAND_IMAGE_LIST_CMD) != COM_STATUS_DONE)
    {
        return 902;
    }
	// send enable MBA feature
	if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
    {
        return 903;
    }
	if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
    {
        return 904;
    }

	// send enable FOTA feature
	if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
    {
        return 905;
    }


	//IMAGE_LIST_IMAGE_COUNT
	if (com_send_byte(com_handle, (unsigned char)main_region_images->num_images) != COM_STATUS_DONE)
    {
        return 906;
    }
	//IMAGE_LIST_ENABLE_COUNT
	if (com_send_byte(com_handle, (unsigned char)main_region_images->num_images) != COM_STATUS_DONE)
    {
        return 907;
    }

	for(i=0; i<main_region_images->num_images; i++)
	{
        GFH_FILE_INFO_v1 gfh_file_info;
		unsigned int beginAddr = 0;
        unsigned int endAddr = 0;
        unsigned int binType = 0;
        unsigned int maxSize = 0;
        unsigned char enable = 0;

        memcpy(&gfh_file_info, main_region_images->region_images[i].buf, sizeof(GFH_FILE_INFO_v1));
		beginAddr = gfh_file_info.m_load_addr;
        endAddr = beginAddr + gfh_file_info.m_file_len - 1;
        binType = gfh_file_info.m_file_type;
        maxSize = gfh_file_info.m_max_size;
        enable = 1;

		// send enable flag
		if (com_send_byte(com_handle, enable) != COM_STATUS_DONE)
		{
			return 908;
		}
		// send begin addr, high byte first
		if (com_send_dword(com_handle, beginAddr) != COM_STATUS_DONE)
		{
			return 909;
		}
		// send end addr, high byte first
		if (com_send_dword(com_handle, endAddr) != COM_STATUS_DONE)
		{
			return 910;
		}
		// send max size
		if (com_send_dword(com_handle, maxSize) != COM_STATUS_DONE)
		{
			return 911;
		}
		// send Type
		if (com_send_dword(com_handle, binType) != COM_STATUS_DONE)
		{
			return 912;
		}
		//read ack
		if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
		{
			return 913;
		}
		if (response != ACK)
		{
			unsigned int errorCode;
			if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
			{
				return 914;
			}
            return errorCode;

		}
	}

	// 3. download image: write to nand flash by memory block
	if (com_send_byte(com_handle, DA_NFB_WRITE_IMAGE_CMD) != COM_STATUS_DONE)
    {
        return 915;
    }

	// wait for command allowance check
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			unsigned int errorCode;
			if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
			{
				return 916;
			}
            return errorCode;

		}
	}

	// send packet length
	if (com_send_dword(com_handle, packet_length) != COM_STATUS_DONE)
    {
        return 917;
    }

	// if accuracy is ACCURACY_AUTO, auto calculate accuracy by baudrate
	//??

	// S1. wait for DA to read boot loader header
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 918;
		}
	}


	// S2. send all rom files

	//send ROM image
	for(i=0; i<main_region_images->num_images;i++)
	{
		num_bytes_sent = 0;
		while (num_bytes_sent < main_region_images->region_images[i].len)
		{
			unsigned int j;
			unsigned short checksum;
			unsigned int num_bytes_to_send;
			unsigned short dummy_checksum = 0;
re_transmission:
			checksum = 0;

#if defined(__GNUC__)
            fflush(stdout);
#endif
			log_output(".");

			if (packet_length> (main_region_images->region_images[i].len - num_bytes_sent))
			{
				num_bytes_to_send = main_region_images->region_images[i].len - num_bytes_sent;
			}
			else
			{
				num_bytes_to_send = packet_length;
			}

			if (com_send_data(com_handle, main_region_images->region_images[i].buf + num_bytes_sent,
				num_bytes_to_send) != COM_STATUS_DONE)
			{
				return 919;
			}

			// calculate checksum
			for(j=0; j<num_bytes_to_send; j++) {
				// WARNING: MUST make sure it unsigned value to do checksum
				checksum += main_region_images->region_images[i].buf[num_bytes_sent+j];
			}

			if (com_send_word(com_handle, checksum) != COM_STATUS_DONE)
			{
				return 920;
			}

			while (1)
			{
				// TODO: error handling
				if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
				{
					continue;
				}

				if (response == CONT_CHAR)
				{
					break;
				}
				else if((response == NACK)||(response == STOP_CHAR))
				{
                    unsigned int errorCode;
                    if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
                    {
                        return 921;
                    }
                    // wait for DA clean RX buffer
                    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
                    {
                        return 922;
                    }
                    if(response != ACK)
                    {
                        return 923;
                    }
                    // send CONT_CHAR to wakeup DA to start recieving again
        			if (com_send_byte(com_handle, CONT_CHAR) != COM_STATUS_DONE)
        			{
        				return 924;
        			}
					goto re_transmission;
				}
			}

			num_bytes_sent += num_bytes_to_send;
		}
	}
    log_output("\n");
    /* Get Flash Layout */
	if (com_recv_dword(com_handle, &regionCount) != COM_STATUS_DONE)
	{
		return 925;
	}
    for(i = 0; i< regionCount; i++)
    {
        if (com_recv_dword(com_handle, &binaryType) != COM_STATUS_DONE)
        {
            return 926;
        }
        if (com_recv_dword(com_handle, &startPage) != COM_STATUS_DONE)
        {
            return 927;
        }
        if (com_recv_dword(com_handle, &boundPage) != COM_STATUS_DONE)
        {
            return 928;
        }
        log_output("Region[%d] {Binary Type: 0x%X, Start Page: 0x%X, Bound Page: 0x%X}\n",
                        i, binaryType, startPage, boundPage);

    }

	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 929;
		}
	}

	for(i=0; i<main_region_images->num_images;i++)
	{

		//calculate checksum
		m_chksum = 0;
		for(j=0; j<main_region_images->region_images[i].len; j++) {
			m_chksum += main_region_images->region_images[i].buf[j];
		}
		if (com_send_word(com_handle, (unsigned short)m_chksum) != COM_STATUS_DONE)
		{
			return 930;
		}
		while (1)
		{
			if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
			{
				continue;
			}

			if (response == ACK)
			{
				break;
			}
			else
			{
				return 931;
			}
		}
	}
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 932;
		}
	}
	return S_DONE;
}

STATUS_E da_write_NOR_images(COM_HANDLE com_handle,
                         Region_images *main_region_images,
                         const unsigned int packet_length)

{
    unsigned char response = 0;
    unsigned int i, j;
	int ret = S_DONE;
	unsigned int errorCode = 0;
	unsigned int offset = 0;

    //
    unsigned char num_of_unchanged_data_blocks = 0;


	GFH_CBR_INFO_v2 cbrVersionInfo;

    memset(&cbrVersionInfo, 0, sizeof(GFH_CBR_INFO_v2));

	for(i=0; i<main_region_images->num_images; i++)
	{
        if(main_region_images->region_images[i].gfh_file_type ==  PRIMARY_MAUI)
        {
            GFH_FILE_INFO_v1 gfh_file_info;
            offset =0; j = 0;
            //Get GFH file info
            while(1)
            {
                memcpy(&gfh_file_info, main_region_images->region_images[i].buf + offset, sizeof(GFH_FILE_INFO_v1));
                //Get CBR version info from main ROM bin.
    		    if(gfh_file_info.m_gfh_hdr.m_type == GFH_CBR_INFO)
    		    {
                    memcpy(&cbrVersionInfo, main_region_images->region_images[i].buf + offset, sizeof(GFH_CBR_INFO_v2));
    			    break;
    		    }
                else
                {
    			    offset += gfh_file_info.m_gfh_hdr.m_size;
    			    if(j==9)
    			    {
    				    return 410;
    			    }
    		    }
                j++;
            }

        }
	}

	//1. Format CBR Record( done at beginning)

	// 2. Download MAUI

	//Get Setting of CBR
	log_output("get_CBRinfo...\n");
    if( 0 != (ret = get_CBRinfo(com_handle, &cbrVersionInfo) ) )
    {
        return ret;
    }


	// 2. set memory block
	log_output("SV5_CMD_SetMemBlock...\n");
    if( 0 != (ret = SV5_CMD_SetMemBlock(com_handle,
                    main_region_images,
                    &num_of_unchanged_data_blocks) ) )
    {
        return ret;
    }



    // erase the ambiguous data
    log_output("SV5_CMD_ERASE_MAUI_INFO...\n");
    if( 0 != (ret = SV5_CMD_ERASE_MAUI_INFO(com_handle) ) )
    {
        return ret;
    }

    set_progress(60);

    //SV5_CMD_WriteData
    if( 0 != (ret = SV5_CMD_WriteData(com_handle,
                            main_region_images,
                            packet_length,
                            num_of_unchanged_data_blocks) ) )
    {
        return ret;
    }


	return S_DONE;
}

STATUS_E SendDataWithRetransmission(COM_HANDLE com_handle,
									const char *data,
									unsigned int dataLength,
									unsigned int packetLength,
									unsigned int sendChecksum)
{
	const unsigned int numPackets = ((dataLength - 1) / packetLength) + 1;
	unsigned int numPacketsSent = 0;

	while (numPacketsSent < numPackets)
	{
		int isLastPacket;
        const unsigned int startOffset = numPacketsSent * packetLength;
        unsigned int currentPacketLength;
		unsigned short checksum = 0;
		unsigned int i;
		unsigned char response;

		if(numPacketsSent == (numPackets - 1))
		{
			isLastPacket = 1;
		}else{
			isLastPacket = 0;
		}

		if(isLastPacket){
			currentPacketLength = dataLength - startOffset;
		}else{
			currentPacketLength = packetLength;
		}

		if (sendChecksum)
        {
			for (i=0; i<currentPacketLength; ++i)
			{
				checksum += (unsigned char) data[startOffset+i];
			}
		}

		while (1)
        {
            if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
			{
				return S_FT_DOWNLOAD_FAIL;
			}

			if (com_send_data(com_handle, data + startOffset,
				currentPacketLength) != COM_STATUS_DONE)
			{
				return S_FT_DOWNLOAD_FAIL;
			}

            if (isLastPacket)
            {
                // Send padding
                for (i=0; i<packetLength-currentPacketLength; ++i)
                {
                    if (com_send_byte(com_handle, 0) != 0)
                    {
                        return 420;
                    }
                }
            }

			if (sendChecksum)
            {
				if (com_send_word(com_handle, checksum) != 0)
				{
					return 421;
				}
			}

            //
            // FIXME (This is only a workaround)
            //
            //   The first attempt to read the CONT_CHAR may not succeed,
            //   so we set a larger timeout value to let ReadData8() keep
            //   trying.
            //
			while (1)
			{
				// TODO: error handling
				if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
				{
					continue;
				}else{
					break;
				}
			}

            if (response == CONT_CHAR)
            {
                break;
            }

            if (response == NACK)
            {
			/*unsigned int errorCode;

			  if (ReadData32(com.GetHandle(), errorCode) != 0)
			  {
			  return 6;
			  }

                MTRACE_ERR(g_hBROM_DEBUG,
				"DA_cmd::SendDataWithRetransmission(): \"%s\"",
				StatusToString(errorCode));

				  if (errorCode == S_DA_NAND_EXCEED_CONTAINER_LIMIT)
				  {
				  return errorCode;
				  }
				  else if (errorCode == S_DA_NAND_REACH_END_OF_FLASH)
				  {
				  return errorCode;
			}*/
				return 422;
            }

			//!! target forcely stop
			if (response == STOP_CHAR)
			{
				//			MTRACE_WARN(g_hBROM_DEBUG, "DA_cmd::SendDataWithRetransmission(): STOP!");

				return 423;
			}
        }

		++numPacketsSent;
	}

	return S_DONE;
}

STATUS_E da_NAND_WriteBlock(COM_HANDLE com_handle,
							unsigned int blockIndex,
							unsigned int pageSize,
							unsigned int spareSize,
							unsigned int numPagesPerBlock,
							const unsigned char *pageBuffer,
							const unsigned char *spareBuffer,
							unsigned short checksum)
{
	unsigned char response = 0;

	//unsigned int packetLength = pageSize + spareSize;
    //unsigned int numPagesPerPacket = 1;
	const unsigned int packetLength = 16896;
    unsigned int numPagesPerPacket;

	unsigned char *packetBuffer;
	unsigned int i, j;
	STATUS_E status;

	unsigned short checksumFromDA = 0;

	log_output("Write Block num = %u\n", blockIndex);

	if (com_send_byte(com_handle, DA_NAND_WRITE_BLOCK) != COM_STATUS_DONE)
    {
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	if (com_send_dword(com_handle, blockIndex) != COM_STATUS_DONE)
    {
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	if (com_send_dword(com_handle, pageSize) != COM_STATUS_DONE)
    {
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	if (com_send_dword(com_handle, spareSize) != COM_STATUS_DONE)
    {
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	if (com_send_dword(com_handle, numPagesPerBlock) != COM_STATUS_DONE)
    {
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	while (1)
    {
        if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
        {
            continue;
        }

        if (response == ACK)
        {

            break;
        }
        else
        {
			unsigned int errorCode;
			if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
			{
				return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
			}
            return errorCode;
        }
    }

	//
    // Send page data and spare data
    //
    if (pageSize == 512)
    {
        //numPagesPerPacket = 8;
		//packetLength *= numPagesPerPacket;

		// (512 + 16) * 32 == 16896
        numPagesPerPacket = 32;
    }
    else if (pageSize == 2048)
    {
        //numPagesPerPacket = 2;
		//packetLength *= numPagesPerPacket;

		// (2048 + 64) * 8 == 16896
        numPagesPerPacket = 8;
    }else
    {
        //MTRACE_ERR(g_hBROM_DEBUG, "DA_cmd::CMD_NAND_WriteBlock(): "
        //           "Unsupported page size: %u", pageSize);
        return S_FT_SET_DOWNLOAD_BLOCK_FAIL;
    }

	packetBuffer = malloc(packetLength);
	for (i=0; i<numPagesPerBlock; i+=numPagesPerPacket)
    {
        for (j=0; j<numPagesPerPacket; ++j)
        {
			memcpy(packetBuffer+(j*(pageSize+spareSize)),
				pageBuffer+(i * pageSize + j * pageSize),
				pageSize
				);
			memcpy(packetBuffer+(pageSize + j*(pageSize+spareSize)),
				spareBuffer+(i * spareSize + j * spareSize),
				spareSize
				);
        }

        status = SendDataWithRetransmission(
			com_handle, packetBuffer,
			packetLength, packetLength,0);

        if (status == S_DONE)
        {
            continue;
        }

        if (status == S_DA_NAND_BAD_BLOCK)
        {
			free(packetBuffer);
			return status;
        }

		free(packetBuffer);
        return 9;
    }

	//
    // Compare checksum for page data
    // (TODO: spare data)
    //
	while (1)
	{
		// TODO: error handling
		if (com_recv_word(com_handle, &checksumFromDA) != COM_STATUS_DONE)
		{
			continue;
		}else{
			break;
		}
	}

    if (checksum != checksumFromDA)
    {
        //MTRACE_ERR(g_hBROM_DEBUG, "DA_cmd::CMD_NAND_WriteBlock(): "
		// "Checksum mismatch => PC(0x%04X) DA(0x%04X)",
		//          checksum, checksumFromDA);
        return 12;
    }

	free(packetBuffer);
	return S_DONE;
}

STATUS_E da_NAND_WritePagesWithinBlock(COM_HANDLE com_handle,
									   unsigned int blockIndex,
									   unsigned int nonemptyPageIndicesSize,
									   unsigned int *nonemptyPageIndicesPageIndex,
									   unsigned int pageSize,
									   unsigned int spareSize,
									   unsigned int numPagesPerBlock,
									   const unsigned char *pageBuffer,
									   const unsigned char *spareBuffer,
									   unsigned short checksum)
{
	unsigned char response = 0;

	unsigned int packetLength = 16896;
    unsigned int numPagesPerPacket;

	unsigned char *packetBuffer;
	unsigned int i;
	STATUS_E status;

	unsigned short checksumFromDA = 0;

	log_output("Write Block num = %u (WritePagesWithinBlock)\n", blockIndex);

	if (com_send_byte(com_handle, DA_NAND_WRITE_PAGES_WITHIN_BLOCK) != COM_STATUS_DONE)
    {
        return 430;
    }

	if (com_send_dword(com_handle, blockIndex) != COM_STATUS_DONE)
    {
        return 431;
    }

	if (com_send_dword(com_handle, nonemptyPageIndicesSize) != COM_STATUS_DONE)
    {
        return 432;
    }

	if (com_send_dword(com_handle, pageSize) != COM_STATUS_DONE)
    {
        return 433;
    }

	if (com_send_dword(com_handle, spareSize) != COM_STATUS_DONE)
    {
        return 434;
    }

	if (com_send_dword(com_handle, numPagesPerBlock) != COM_STATUS_DONE)
    {
        return 435;
    }

	while (1)
    {
        if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
        {
            continue;
        }

        if (response == ACK)
        {

            break;
        }
        else
        {
			unsigned int errorCode;
			if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
			{
				return 436;
			}
            return errorCode;
        }
    }

	//
    // Send page data and spare data
    //
    if (pageSize == 512)
    {
        //numPagesPerPacket = 8;
		//packetLength *= numPagesPerPacket;

		// (512 + 16) * 32 == 16896
        numPagesPerPacket = 32;
    }
    else if (pageSize == 2048)
    {
        //numPagesPerPacket = 2;
		//packetLength *= numPagesPerPacket;

		// (2048 + 64) * 8 == 16896
        numPagesPerPacket = 8;
    }else
    {
        //MTRACE_ERR(g_hBROM_DEBUG, "DA_cmd::CMD_NAND_WriteBlock(): "
        //           "Unsupported page size: %u", pageSize);
        return 437;
    }

	packetLength = pageSize + spareSize;
	numPagesPerPacket = 1;

	packetBuffer = malloc(packetLength);
	//for (i=0; i<numPagesPerBlock; i+=numPagesPerPacket)
	for (i=0; i<nonemptyPageIndicesSize; i++)
    {
		if (com_send_dword(com_handle, nonemptyPageIndicesPageIndex[i]) != COM_STATUS_DONE)
		{
        free(packetBuffer);
			return 438;
		}

		//for (j=0; j<numPagesPerPacket; ++j)
        {
			memcpy(packetBuffer,
				pageBuffer+(nonemptyPageIndicesPageIndex[i] * pageSize),
				pageSize
				);
			memcpy(packetBuffer+pageSize,
				spareBuffer+(nonemptyPageIndicesPageIndex[i] * spareSize),
				spareSize
				);
        }

        status = SendDataWithRetransmission(
			com_handle, packetBuffer,
			packetLength, packetLength,0);

        if (status == S_DA_NAND_BAD_BLOCK)
        {
			free(packetBuffer);
			return status;
        }

		//
		// Compare checksum for page data
		// (TODO: spare data)
		//
		while (1)
		{
			// TODO: error handling
			if (com_recv_word(com_handle, &checksumFromDA) != COM_STATUS_DONE)
			{
				continue;
			}else{
				break;
			}
		}

		/*if (checksum != checksumFromDA)
		{
		//MTRACE_ERR(g_hBROM_DEBUG, "DA_cmd::CMD_NAND_WriteBlock(): "
		// "Checksum mismatch => PC(0x%04X) DA(0x%04X)",
		//          checksum, checksumFromDA);
		return 12;
	}*/

		if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
		{
			return 439;
		}

		if (status == S_DONE)
        {
            continue;
        }


		free(packetBuffer);
        return 440;
    }


	free(packetBuffer);
	return S_DONE;
}

STATUS_E da_NAND_ReadBlock(COM_HANDLE com_handle,
						   unsigned int startAddr,
						   unsigned int blockSize,
						   NUTL_ReadFlag_E flag,
						   unsigned char *Buffer,
						   unsigned int *buf_len)
{
	unsigned char val;
	unsigned int total_read_bytes;
	unsigned int current_read_bytes;
	const unsigned int	max_pagesize=2048;
	const unsigned int	max_pagesparesize=2112;
	unsigned int page_size, spare_size;
	unsigned int	packet_length=0;
	unsigned int unit_of_page_read_bytes;
	const unsigned int m_packet_length = 4096;

	//unsigned char *buf;
	unsigned char response;

	unsigned int	pages_of_packet;
	unsigned int	retry_count;

	// limit packet_length to max_pagesparesize as minmimum value
	if(m_packet_length < max_pagesparesize ) {
		packet_length = max_pagesparesize;		// max page+spare size
	}
	else {
		packet_length = max_pagesparesize*(m_packet_length/max_pagesize);	// page+spare alignment
	}

	// check if read addr is pagesize alignment
	if( 0 != (startAddr%max_pagesize) ) {
		return 450;
	}

	// check if read length is pagesize alignment
	if( 0 != (blockSize%max_pagesize) ) {
		return 451;
	}

	// send NAND read page commnad and readflag
	if (com_send_byte(com_handle, DA_NAND_READPAGE_CMD) != COM_STATUS_DONE)
	{
		return 452;
	}
	// send readback FAT flag
	val = flag;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return 453;
	}

	// send readback address
	if (com_send_dword(com_handle, startAddr) != COM_STATUS_DONE)
	{
		return 454;
	}
	// send readback length
	if (com_send_dword(com_handle, blockSize) != COM_STATUS_DONE)
	{
		return 455;
	}
	// Send container length
	if (com_send_dword(com_handle, 0) != COM_STATUS_DONE)
	{
		return 456;
	}

	// wait for addr and len check
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 457;
		}
	}

	// get Unit of Page Read
	if (com_recv_dword(com_handle, &page_size))
	{
		return 458;
	}
	if (com_recv_dword(com_handle, &spare_size))
	{
		return 459;
	}

	// get Unit of Page Read
	if (com_recv_dword(com_handle, &unit_of_page_read_bytes))
	{
		return 460;
	}

	// send pages_per_packet
	pages_of_packet = packet_length/(page_size+spare_size);
	if (com_send_dword(com_handle, pages_of_packet) != COM_STATUS_DONE)
	{
		return 461;
	}

	// get total read back length
	if (com_recv_dword(com_handle, &total_read_bytes) != COM_STATUS_DONE)
	{
		return 462;
	}
    *buf_len = total_read_bytes;
	current_read_bytes = 0;
	retry_count = 10;
	while( current_read_bytes < total_read_bytes ) {
		unsigned short	buf_checksum;
		unsigned short	packet_checksum;
		unsigned int	j;
		unsigned int	packet_bytes = 0;
		unsigned int	re_transmission = 0;
		//unsigned int result;

		// read data
		for(j=0; j<1000;j++)
		{
			//if (com_recv_data(com_handle, Buffer+(current_read_bytes), pages_of_packet * unit_of_page_read_bytes) == COM_STATUS_DONE)
			if (com_recv_data_chk_len(com_handle, Buffer+(current_read_bytes), pages_of_packet * unit_of_page_read_bytes) == COM_STATUS_DONE)
			{
				break;
			}else{

			}
		}
		//result = com_recv_data(com_handle, Buffer+(current_read_bytes), pages_of_packet * unit_of_page_read_bytes);

		if(re_transmission==1){
			if(retry_count==0)
			{
				com_send_byte(com_handle, NACK);
				return S_DA_NAND_PAGE_READ_FAILED;
			}else{
				if (com_send_byte(com_handle, CONT_CHAR) != COM_STATUS_DONE)
				{
					return 463;
				}
				retry_count--;
			}
			continue;
		}


		packet_bytes += (pages_of_packet * unit_of_page_read_bytes);

		// read chksum
		if(com_recv_word(com_handle, &packet_checksum) != COM_STATUS_DONE)
		{
			return 464;
		}

		// if empty packet, jump to sending ack step
		if( 0 == packet_bytes ) {
			// send ACK
			if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
			{
				return 465;
			}
			continue;
		}

		// calculate buffer checksum
		buf_checksum = 0;
		for(j=0; j<packet_bytes; j++) {
			// WARNING: MUST make sure it unsigned value to do checksum
			buf_checksum += Buffer[current_read_bytes+j];
		}

		// compare checksum
		if( buf_checksum != packet_checksum ) {
			// send ACK
			if(retry_count==0)
			{
				com_send_byte(com_handle, NACK);
				return S_DA_NAND_PAGE_READ_FAILED;
			}else{
				if (com_send_byte(com_handle, CONT_CHAR) != COM_STATUS_DONE)
				{
					return 466;
				}
				retry_count--;
				continue;
			}
			return 467;
		}
		current_read_bytes += packet_bytes;

		// send ACK
		if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
		{
			return 468;
		}

	}

	return S_DONE;
}


STATUS_E SV5_CMD_SetMemBlock(COM_HANDLE com_handle,
                            Region_images *main_region_images,
                            unsigned char *get_num_of_unchanged_data_blocks)
{

    unsigned int bin_byte[20]; // assume total bin number doesn't exceed 20
    unsigned int i,j;
    unsigned char buf[4];
    memset(bin_byte, 0, 20*sizeof(unsigned int));

    // send mem block command
    buf[0] = DA_MEM_CMD;
    if(com_send_byte(com_handle, DA_MEM_CMD) != COM_STATUS_DONE)
    {
        return 470;
    }

    // send Image Attributes, NACK: disable FOTA feature
    if(com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
    {
        return 471;
    }

    // send mem block count
    buf[0] = main_region_images->num_images;
    if(com_send_byte(com_handle, buf[0]) != COM_STATUS_DONE)
    {
        return 472;
    }

    // send MEM begin addr, end addr
    for (i=0; i<main_region_images->num_images; ++i)
    {
        const unsigned int beginAddr = main_region_images->region_images[i].load_addr % 0x08000000;
        const unsigned int endAddr = beginAddr + main_region_images->region_images[i].len - 1;
        const unsigned int binType = main_region_images->region_images[i].gfh_file_type;

        bin_byte[i] = endAddr - beginAddr;
        log_output("send MEM_BEGIN_ADDR(0x%08X)\n", beginAddr);
        if(com_send_dword(com_handle, beginAddr) != COM_STATUS_DONE)
        {
            return 473;
        }
        log_output("send MEM_END_ADDR(0x%08X)\n", endAddr);
        if(com_send_dword(com_handle, endAddr) != COM_STATUS_DONE)
        {
            return 474;
        }
        log_output("send IMAGE_TYPE(0x%08X)\n", binType);
        if(com_send_dword(com_handle, binType) != COM_STATUS_DONE)
        {
            return 475;
        }

        // read ack
        if(com_recv_byte(com_handle, buf) != COM_STATUS_DONE)
        {
            return 476;
        }

        if (ACK != buf[0])
        {
            unsigned int da_ret = 0;
            if (com_recv_dword(com_handle, &da_ret)!= COM_STATUS_DONE)
            {
                return 477;
            }
            return da_ret;
        }
    }

    // read the number of the unchanged data blocks
    if(com_recv_byte(com_handle, buf) != COM_STATUS_DONE)
    {
        return 478;
    }
    *get_num_of_unchanged_data_blocks = buf[0];
    log_output("UNCHANED_DATA_BLOCKS=(0x%02X)\n", *get_num_of_unchanged_data_blocks);

	// read the ACK/NACK to determinate do format flash firstly or not
    if(com_recv_byte(com_handle, buf) != COM_STATUS_DONE)
    {
		return 479;
	}

	if(buf[0] == ACK) // do format firstly, show yellow progress bar
	{
        // send MEM begin addr, end addr
        for (i=0; i<main_region_images->num_images; ++i)
        {
            unsigned int format_times = 1;

            // get format times for this bin
            if(com_recv_dword(com_handle, &format_times) != COM_STATUS_DONE)
            {
                    return 480;
            }
            log_output("bin index = %d format time = %d\n", i , format_times);
            for(j = 0 ; j < format_times ; j++)
            {
                log_output("Wait bin[%d] format time %d/%d\n",i,j,format_times);
                // get return ACK for each format
                if(com_recv_byte(com_handle, buf) != COM_STATUS_DONE)
                {
                        return 481;
                }
            }
    	}
    }

    // read the Pre-Erase
    if(com_recv_data_chk_len(com_handle, buf, 1) != COM_STATUS_DONE)
    {
		return 482;
	}

    return S_DONE;
}

STATUS_E SV5_CMD_ERASE_MAUI_INFO(COM_HANDLE com_handle)
{
    unsigned int target_MauiInfo_addr = 0x0;
    unsigned int target_rom_addr = 0x0;
    unsigned int target_blk_size = 0x0;
    unsigned int pc_rom_addr = 0x0;
    unsigned char response = 0;
    //
    // Send DA command
    //
    log_output("Send DA_ERASE_MAUI_INFO(0x%02X).\n",DA_ERASE_MAUI_INFO);


    //  Send command
    if (com_send_byte(com_handle, DA_ERASE_MAUI_INFO) != 0)
    {
        return S_FT_PROTOCOL_EXCEPTION;
    }

    //
    //  Wait for response
    //
    // Read data
    if(com_recv_dword(com_handle, &target_MauiInfo_addr) != COM_STATUS_DONE)
    {
        return 490;
    }

    log_output("get the target_MauiInfo_addr: 0x%x\n", target_MauiInfo_addr);


    if(com_recv_dword(com_handle, &target_rom_addr) != COM_STATUS_DONE)
    {
        return 491;
    }
    log_output("get the target_rom_addr: 0x%x\n", target_rom_addr);

    if(com_recv_dword(com_handle, &target_blk_size) != COM_STATUS_DONE)
    {
        return 492;
    }

    log_output("get the target_blk_addr: 0x%x\n", target_blk_size);

    if(target_rom_addr  == 0)
    {
        log_output("Target address is zero. Do nothing!\n");
        return S_DONE;
    }



    if(com_recv_dword(com_handle, &pc_rom_addr) != COM_STATUS_DONE)
    {
        return 493;
    }
    log_output("get the pc_rom_addr: 0x%x\n", pc_rom_addr);


    // check the status
    if(com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 494;
    }

    if(response == ACK)
    {
        if(com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
        {
            return 495;
        }

        if(response != ACK)
        {
            return 496;
        }
        log_output("ERASE DONE!\n");
    }

    return S_DONE;
}

STATUS_E SV5_CMD_WriteData(COM_HANDLE com_handle,
                            Region_images *main_region_images,
                            unsigned int packet_length,
                            unsigned char num_of_unchanged_data_blocks)
{
    unsigned char response = 0;
    unsigned int i;
    unsigned char buf[5];
    unsigned int TotalImageSize = 0;
    unsigned int TotalSendBytes = 0;
    //
    // Send DA command
    //
    log_output("Send DA_WRITE_CMD(0x%02X).\n",DA_WRITE_CMD);


    //  Send command
    if (com_send_byte(com_handle, DA_WRITE_CMD) != 0)
    {
        return 550;
    }

    //
    //  Wait for response
    //
    // Read data
    if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 551;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 552;
        }
        return errorCode;
    }

    //Download Methodology is Sequential Erase (0x1). (0x0) for Best-Effort Erase
    if(com_send_byte(com_handle, 0x1) != COM_STATUS_DONE)
    {
        return 553;
    }
    log_output("Packet Length: %d\n", packet_length);

    if(com_send_dword(com_handle, packet_length) != COM_STATUS_DONE)
    {
        return 554;
    }

    log_output("wait for DA to save unchanged data\n");

    if(num_of_unchanged_data_blocks)
    {
        // delay_time = N x (32x1024)x(70ns) = N x (32x1024)x7/100000 ms
        // N -> number of unchanged data blocks
        // 32x1024 -> each sector has max 32K word data
        // 70ns -> 70x10-9 the single word read time
        unsigned int delay_time = num_of_unchanged_data_blocks*32*1024*7/100000;
        log_output("Sleep(%lums)=UNCHANED_DATA_BLOCKS(%d)x32KWx70ns.\n", delay_time, num_of_unchanged_data_blocks);
        Sleep(delay_time);
    }


    if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 555;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 556;
        }
        return errorCode;
    }
    // wait for 1st sector erase done
    if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 557;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 558;
        }
        return errorCode;
    }

    set_progress(70);

    // count total image size
    for (i=0; i<main_region_images->num_images; ++i)
    {
        TotalImageSize += main_region_images->region_images[i].len;
    }


    // S1. send all rom files
    for (i=0; i<main_region_images->num_images; ++i)
    {
        const unsigned char *imageBuf = main_region_images->region_images[i].buf;
        const unsigned int imageSize = main_region_images->region_images[i].len;
        unsigned int sent_bytes = 0;
        unsigned int retry_count=0;

        while( sent_bytes < imageSize )
        {
            unsigned int j;
            unsigned short checksum;
            unsigned int frame_bytes;

re_transmission:

            // reset the frame checksum
            checksum = 0;

            // if the last frame is less then PACKET_LENGTH bytes
            if( packet_length > (imageSize-sent_bytes) ) {
                frame_bytes = imageSize - sent_bytes;
            }
            else {
                // the normal frame
                frame_bytes = packet_length;
            }
            // Send NACK to make DA stop
            // else ACK to continue
            if(com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
            {
                log_output("Fail to Send ACK !!\n");
                return 559;
            }

            // calculate checksum
            for(j=0; j<frame_bytes; j++)
            {
                // WARNING: MUST make sure it unsigned value to do checksum
                checksum += imageBuf[sent_bytes+j];
            }
            // send frame
            if(com_send_data(com_handle, imageBuf+sent_bytes, frame_bytes) != COM_STATUS_DONE)
            {
                log_output("Fail to Send Packet Frame !!\n");
                goto read_cont_char;
            }
#if defined(__GNUC__)
            fflush(stdout);
#endif
            //log_output(".");  // remove for DSDA porting in android platofmr. This is console behavior.

            // send 2 bytes checksum, high byte first
            buf[0] = (unsigned char)((checksum>> 8)&0x000000FF);
            buf[1] = (unsigned char)((checksum)    &0x000000FF);
            if(com_send_data(com_handle, buf, 2) != COM_STATUS_DONE)
            {
                log_output("Fail to Send Packet checksum !!\n");
                goto read_cont_char;
            }

read_cont_char:
            // read CONT_CHAR
            if(com_recv_data_chk_len(com_handle, buf, 1) != COM_STATUS_DONE)
            {
                log_output("Fail to Read CONT_CHAR !!\n");
                return 560;
            }
            if( CONT_CHAR == buf[0] )
            {
                // sent ok!, reset retry_count
                retry_count = 0;
            }
            else
            {
                unsigned int da_ret, ret;
                // get error code
                if(com_recv_dword(com_handle, &da_ret)) {
                    log_output("Fail to Read Error code from DA !!\n");
                    return 561;
                }
                switch(da_ret)
                {
                case S_DA_UART_RX_BUF_FULL:
                    // target RX buffer is full, add delay to wait for flash erase done
                    //Sleep(DA_FLASH_ERASE_WAITING_TIME);
                    do{
                        if( 0 != (ret=com_recv_data_chk_len(com_handle, buf, 1)) )
                        {
                            log_output("fail, Err(%d)\n", ret);
                        }
                        //MTRACE(g_hBROM_DEBUG, "DA_cmd::CMD_WriteData(): FLUSH BUFFER: 0x%x", buf[0]);

                    }while(FLUSH_CONT == buf[0]);

                case S_DA_UART_DATA_CKSUM_ERROR:
                case S_DA_UART_GET_DATA_TIMEOUT:
                case S_DA_UART_GET_CHKSUM_LSB_TIMEOUT:
                case S_DA_UART_GET_CHKSUM_MSB_TIMEOUT:
                    // check retry times
                    if( PACKET_RE_TRANSMISSION_TIMES > retry_count )
                    {
                        retry_count++;
                        log_output("Retry(%u): (%d): start to re-transmit.\n", retry_count, da_ret);
                    }
                    else
                    {
                        // fail to re-transmission
                        // send NACK to wakeup DA to stop
                        buf[0] = NACK;
                        log_output("Retry(%u): send NACK(0x%02X) to wakeup DA to stop!\n", retry_count, NACK);
                        if(com_send_data(com_handle, buf, 1) != COM_STATUS_DONE)
                        {
                            log_output("Retry(%u): %lu bytes sent.\n", retry_count, sent_bytes);
                        }
                        return 562;
                    }

                    // wait for DA clean RX buffer
                    log_output("Retry(%u): wait for DA clean it's RX buffer\n", retry_count);
                    if( 0 != (ret=com_recv_data_chk_len(com_handle, buf, 1)) )
                    {
                        log_output("fail, Err(%d)\n", ret);
                        return 563;
                    }

                    if( ACK != buf[0] )
                    {
                        log_output("Retry(%u): wrong ack(0x%02X) return!", retry_count, buf[0]);
                        return 564;
                    }

                    // send CONT_CHAR to wakeup DA to start recieving again
                    buf[0] = CONT_CHAR;
                    log_output("send CONT_CHAR to wakeup DA to start recieving again.\n");
                    if(com_send_data(com_handle, buf, 1) != COM_STATUS_DONE)
                    {
                        log_output("Fail to Send CONT_CHAR !!\n");
                        return 565;
                    }

                    // re-transmission this frame
                    log_output("Retry(%u): re-transmission this frame, offset(%lu).", retry_count, sent_bytes);
                    goto re_transmission;
                    break;
                default:
                    // abort transmission
                    log_output("(%d): abort transmission!", da_ret);
                    return da_ret;
                }
            }

            // update progress state
            sent_bytes += frame_bytes;
            TotalSendBytes += frame_bytes;

            set_progress(70 + (25* ((float)TotalSendBytes/TotalImageSize)));
        }
        log_output("Image[%d]: %lu bytes sent\n", i, sent_bytes);
    }
    //wait for DA to write all data to flash.
    log_output("wait for DA to write all data to flash\n");
    if( com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE )
    {
        return 566;
    }

    // S2. wait for recovery done ack
    log_output("wait for DA to perform unchanged data recovery.\n");
    if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 567;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 568;
        }
        return errorCode;
    }
    // wait for pre-ProcessInfo done ack
    log_output("wait for DA to perform ProcessInfo.\n");
    if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 569;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 570;
        }
        log_output("DA ProcessInfo fail, da_ret = %d.\n", errorCode);
        return errorCode;
    }

    set_progress(98);

    //SV5_VerifyImageChecksum
    log_output("verify image checksum. for normal chip\n");

    for(i = 0; i < main_region_images->num_images; i++)
    {
        unsigned short checksum = 0;
        if (main_region_images->region_images[i].gfh_file_type == BOOT_CERT)
        {
            continue;
        }

        checksum = GetImageChecksum(main_region_images->region_images[i].buf, main_region_images->region_images[i].len);

        log_output("Verifying %s\n", main_region_images->region_images[i].name);
        log_output("Sending ckecksum (0x%04X)\n", checksum);

        if (com_send_word(com_handle, checksum) != COM_STATUS_DONE)
        {
            return 571;
        }

        if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
        {
            log_output("Get checksum from DA failed !!\n");
            return 572;
        }

        if (response != ACK)
        {
            unsigned short checksumOnFlash = 0;
            log_output("Checksum mismatched! res = 0x%02x\n",response);

            if (com_recv_word(com_handle, &checksumOnFlash) != COM_STATUS_DONE)
            {
                return 573;
            }
            log_output("[ERROR]checksumOnFlash = 0x%04X\n",checksumOnFlash);
            return S_FT_DOWNLOAD_CHKSUM_MISMATCH;
        }

        log_output("Checksum matched (image index = %d)\n", i);

    }
    log_output("write data OK\n");
    return S_DONE;
}


STATUS_E da_FormatFAT(COM_HANDLE com_handle,
						NUTL_EraseFlag_E  flag,
						unsigned int  bValidation,
                         Range *nor_fat_range,
                         Range *nand_fat_range)
{
	unsigned char response = 0;
	unsigned char val;
	unsigned int status;
	unsigned int format_addr,format_length;
	Range norFATRange, nandFATRange, emmcFATRange;

    unsigned int statistics_fmt_begin_addr = 0;
    unsigned int statistics_fmt_length = 0;
    unsigned int statistics_total_blocks = 0;
    unsigned int statistics_bad_blocks = 0;
    unsigned int statistics_fmt_err_blocks = 0;

	if (com_send_byte(com_handle, DA_FORMAT_FAT_CMD) != COM_STATUS_DONE)
	{
		return 390;
	}
	val = (unsigned char)flag;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return 391;
	}
	val = (unsigned char)bValidation;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return 392;
	}

	//
    // Wait for response
    //
	if (com_recv_dword(com_handle, &status) != COM_STATUS_DONE)
	{
		return 393;
	}
	if (status != S_DONE)
    {
        return status;
    }

	if (com_recv_dword(com_handle, &norFATRange.m_start_address) != COM_STATUS_DONE)
	{
		return 394;
	}
	if (com_recv_dword(com_handle, &norFATRange.m_length) != COM_STATUS_DONE)
	{
		return 395;
	}
	if (com_recv_dword(com_handle, &nandFATRange.m_start_address) != COM_STATUS_DONE)
	{
		return 396;
	}
	if (com_recv_dword(com_handle, &nandFATRange.m_length) != COM_STATUS_DONE)
	{
		return 397;
	}
	if (com_recv_dword(com_handle, &emmcFATRange.m_start_address) != COM_STATUS_DONE)
	{
		return 398;
	}
	if (com_recv_dword(com_handle, &emmcFATRange.m_length) != COM_STATUS_DONE)
	{
		return 399;
	}

	if (norFATRange.m_length != 0)
    {
		//type = HW_STORAGE_NOR;
        format_addr			= norFATRange.m_start_address;
        format_length       = norFATRange.m_length;

	}

	if(nandFATRange.m_length != 0)
    {
		//type = HW_STORAGE_NAND;
        format_addr			= nandFATRange.m_start_address;
        format_length       = nandFATRange.m_length;
	}

	if(emmcFATRange.m_length != 0)
    {
		//type = HW_STORAGE_NAND;
        format_addr			= emmcFATRange.m_start_address;
        format_length       = emmcFATRange.m_length;
	}


	// wait for command allowance check
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 400;
		}
	}
	// range checking
	while (1)
	{
		if (com_recv_byte_without_retry(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
			return 401;
		}
	}
	// wait til format is done
	while (1)
	{
		STATUS_E da_ret;
		unsigned char return_progress = 0;
		unsigned int addr32;

		if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
		{
			return 402;
		}

		// check report
		if (da_ret == S_DA_IN_PROGRESS || S_DONE==da_ret)
		{
			if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
			{
				return 403;
			}
		}else{
			// error, read fail address
			if (com_recv_dword(com_handle, &addr32) != COM_STATUS_DONE)
			{
				return 404;
			}
			// get continue flag
			if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
			{
				return 405;
			}
			if (CONT_CHAR == response)
			{
				continue;
			}else{
				return 406;
			}
		}

		if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
		{
			return 407;
		}

		// 100%, format is done
		if (S_DONE == da_ret)
		{
			break;
		}
	}


	if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
	{
		return 408;
	}

	if (response != ACK)
	{
        if (com_recv_dword(com_handle, &statistics_fmt_begin_addr) != COM_STATUS_DONE)
        {
            return 409;
        }
        if (com_recv_dword(com_handle, &statistics_fmt_length) != COM_STATUS_DONE)
        {
            return 409;
        }
        if (com_recv_dword(com_handle, &statistics_total_blocks) != COM_STATUS_DONE)
        {
            return 409;
        }
        if (com_recv_dword(com_handle, &statistics_bad_blocks) != COM_STATUS_DONE)
        {
            return 409;
        }
        if (com_recv_dword(com_handle, &statistics_fmt_err_blocks) != COM_STATUS_DONE)
        {
            return 409;
        }
        log_output("FormatStatisticsReport_T={ range( begin :0x%08X, length:0x%08X), total_blocks(%d), bad_blocks(%d), err_blocks(%d) }.",
                    statistics_fmt_begin_addr,
                    statistics_fmt_length,
                    statistics_total_blocks,
                    statistics_bad_blocks,
                    statistics_fmt_err_blocks);
	}


	return S_DONE;
}

STATUS_E da_FormatFlash(COM_HANDLE com_handle,
						HW_StorageType_E  type,
						NUTL_EraseFlag_E  flag,
						unsigned int  bValidation,
						unsigned int address,
						unsigned int length)
{
	unsigned char response = 0;
	unsigned char val;

	if (com_send_byte(com_handle, DA_FORMAT_CMD) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}
	val = type;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}
	val = flag;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}
	val = bValidation;
	if (com_send_byte(com_handle, val) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}

	// send erase address
	if (com_send_dword(com_handle, address) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}
	// send erase length
	if (com_send_dword(com_handle, length) != COM_STATUS_DONE)
	{
		return S_DA_NAND_ERASE_FAILED;
	}

	// wait for command allowance check
	while (1)
	{
		if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
            unsigned int errorCode;
            if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            return errorCode;
		}
	}
	// range checking
	while (1)
	{
		if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
            unsigned int errorCode;
            if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            return errorCode;
		}
	}

	// wait til format is done
	// read report from DA
	while (1)
	{
		STATUS_E da_ret;
		unsigned char return_progress = 0;
		unsigned int addr32;

		if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
		{
			return S_DA_NAND_ERASE_FAILED;
		}

		// check report
		if (da_ret == S_DA_IN_PROGRESS || S_DONE==da_ret)
		{
			if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
			{
				return S_DA_NAND_ERASE_FAILED;
			}
		}else{
			// error, read fail address
			if (com_recv_dword(com_handle, &addr32) != COM_STATUS_DONE)
			{
				return S_DA_NAND_ERASE_FAILED;
			}
			// get continue flag
			if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
			{
				return S_DA_NAND_ERASE_FAILED;
			}
			if (CONT_CHAR == response)
			{
				continue;
			}else{
				return S_DA_NAND_ERASE_FAILED;
			}
		}

		if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
		{
			return S_DA_NAND_ERASE_FAILED;
		}

		// 100%, format is done
		if (S_DONE == da_ret)
		{
			break;
		}
	}

	while (1)
	{
		if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
		{
			continue;
		}

		if (response == ACK)
		{
			break;
		}
		else
		{
            unsigned int fmt_begin_addr = 0;
            unsigned int fmt_length = 0;
            unsigned int total_blocks = 0;
            unsigned int bad_blocks = 0;
            unsigned int err_blocks = 0;
            if (com_recv_dword(com_handle, &fmt_begin_addr) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            if (com_recv_dword(com_handle, &fmt_length) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            if (com_recv_dword(com_handle, &total_blocks) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            if (com_recv_dword(com_handle, &bad_blocks) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            if (com_recv_dword(com_handle, &err_blocks) != COM_STATUS_DONE)
            {
                return S_DA_NAND_ERASE_FAILED;
            }
            log_output("FORMAT: begin address(0x%X) length(0x%X) total blocks(%d) bad blocks(%d) err blocks(%d)\n",
                        fmt_begin_addr,
                        fmt_length,
                        total_blocks,
                        bad_blocks,
                        err_blocks);
            break;
		}
	}

	return S_DONE;
}
STATUS_E SV5_CMD_FormatCBR(COM_HANDLE com_handle)
{
    unsigned char response;

    if (com_send_byte(com_handle, DA_FORMAT_CBR) != COM_STATUS_DONE)
    {
        return 380;
    }
    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 381;
    }
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 382;
        }
        //Format CBR error!!!
        return errorCode;
    }
    return S_DONE;
}

STATUS_E SV5_CMD_BACKUP_AND_ERASE_HB(COM_HANDLE com_handle)
{
	unsigned char status;

	// Send command
    if (com_send_byte(com_handle, DA_BACKUP_AND_ERASE_HB) != COM_STATUS_DONE)
    {
        return 960;
    }

	if (com_recv_byte(com_handle, &status) != COM_STATUS_DONE)
	{
		return 961;
	}
	if (status != ACK)
    {
		return 962;
	}
	// Start to backup and erase header block

    if (com_recv_byte(com_handle, &status) != COM_STATUS_DONE)
	{
		return 963;
	}
	if (status != ACK)
    {
		return 964;
	}
    // Done to backup and erase header block

    return S_DONE;
}

STATUS_E SV5_CMD_RESTORE_HB(COM_HANDLE com_handle)
{
	unsigned char status;

	// Send command
    if (com_send_byte(com_handle, DA_RESTORE_HB) != COM_STATUS_DONE)
    {
        return 970;
    }

	if (com_recv_byte(com_handle, &status) != COM_STATUS_DONE)
	{
		return 971;
	}
	if (status != ACK)
    {
		return 972;
	}
	// Start to restore header block

    if (com_recv_byte(com_handle, &status) != COM_STATUS_DONE)
	{
		return 973;
	}
	if (status != ACK)
    {
		return 974;
	}
    // Done to restore header block

    return S_DONE;
}


STATUS_E SV5_CMD_ReadBMT(COM_HANDLE com_handle, DA_REPORT_T *p_da_report)
{
    bmt_entry* p_bmt_entry = NULL;
    unsigned char bmt_entry_num = 0;
    unsigned char response = 0;
    int idx = 0;
    unsigned int block_size = p_da_report->m_nand_pagesize * p_da_report->m_nand_pages_per_block;

    // send DA_READ_BMT
    if (com_send_byte(com_handle, DA_READ_BMT) != COM_STATUS_DONE)
        return 320;

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
        return 321;

    if (ACK != response)
        return 322;
    if (com_recv_byte(com_handle, &bmt_entry_num) != COM_STATUS_DONE)
        return 323;

    p_bmt_entry = (bmt_entry*)malloc(sizeof(bmt_entry)*bmt_entry_num);
    memset(p_bmt_entry, 0, sizeof(bmt_entry)*bmt_entry_num);

    for (idx = 0; idx < bmt_entry_num; idx++)
    {
        if (com_recv_word(com_handle, &p_bmt_entry[idx].bad_index) != COM_STATUS_DONE)
        {
            free(p_bmt_entry);
            return 324;
        }
        if (com_recv_word(com_handle, &p_bmt_entry[idx].mapped_index) != COM_STATUS_DONE)
        {
            free(p_bmt_entry);
            return 325;
        }
    }
    if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
    {
        free(p_bmt_entry);
        return 326;
    }
    log_output("Read BMT: %02d entries.\n", bmt_entry_num);
    for (idx = 0; idx < bmt_entry_num ;idx++)
    {
        log_output("Read BMT(%02d): %02X -> %02X, %08X -> %08X\n",
                    idx,
                    p_bmt_entry[idx].bad_index,
                    p_bmt_entry[idx].mapped_index,
                    p_bmt_entry[idx].bad_index*block_size,
                    p_bmt_entry[idx].mapped_index*block_size);
    }
    return 0;
}

STATUS_E SV5_CMD_NAND_DL_Partition_Data(COM_HANDLE com_handle,
                                    const struct image *linux_images,
                                    const unsigned int num_linux_images,
                                    unsigned int packetLength)
{
    // the load and target's flash must support 2K packet size
    const unsigned int yaffs_packetLength = 2048+64;
    unsigned int index = 0;
    unsigned int total_bytes = 0;
    unsigned char response = 0;
    log_output("packetlength=%u , yaffs_packetLength=%u\n",
            packetLength,
            yaffs_packetLength);

    //Calculate the total size of all the image
    for(index = 0; index < num_linux_images; index++)
    {
        if(linux_images[index].type == RESERVED ||
            (strcmp(linux_images[index].name, "SecurePartition") == 0))
        {
            continue;
        }
        else
        {
            total_bytes += linux_images[index].len;
        }
    }

    //DA_DL_LINUX_RAW_DATA
    for(index = 0; index < num_linux_images; index++)
    {
        unsigned int partition_size = 0;
        unsigned int send_packet = packetLength; // packetLength is just used to store respected packet length

        //1 Send DA command by partition type
        if(linux_images[index].type == RESERVED )
        {
            log_output("Pass DL Partition %s type(0x%x)\n", linux_images[index].name, linux_images[index].type);
            continue;
        }
        else if(linux_images[index].type == RAW)
        {
            if( strcmp(linux_images[index].name, "SecurePartition") == 0)
            {
                // SV5_CMD_NAND_DL_Encrypt_Partition_Data(hCOM, rom);
                // Do nothing
                continue;
            }

            if (com_send_byte(com_handle, DA_DL_LINUX_RAW_DATA) != COM_STATUS_DONE)
            {
                return 330;
            }
        }
        else if(linux_images[index].type == YAFFS)
        {
            send_packet = yaffs_packetLength;

            if (com_send_byte(com_handle, DA_DL_LINUX_YAFFS_DATA) != COM_STATUS_DONE)
            {
                return 331;
            }
        }
        else
        {
            return 332;
            //Return error of using unsupported type
        }

        if(index < num_linux_images-1)
        {
            //the partition size of the last one will be 0
            partition_size = linux_images[index+1].load_addr - linux_images[index].load_addr;
        }

        log_output("---Write Partition [%s] type(0x%x)---\n",linux_images[index].name, linux_images[index].type);
        log_output("Partition Address 0x%X\n",linux_images[index].load_addr);
        log_output("Partition Size 0x%X\n",partition_size);
        log_output("Image Size 0x%X(%d)\n",linux_images[index].len,linux_images[index].len);
        log_output("Packet Length 0x%X(%d)\n",send_packet,send_packet);

        // Apply same protocol to DA_DL_LINUX_RAW_DATA and DA_DL_LINUX_YAFFS_DATA

        if (com_send_dword(com_handle, linux_images[index].load_addr) != COM_STATUS_DONE)
        {
            return 333;
        }

        if (com_send_dword(com_handle, partition_size) != COM_STATUS_DONE)
        {
            return 334;
        }

        if (com_send_dword(com_handle, linux_images[index].len) != COM_STATUS_DONE)
        {
            return 335;
        }

        // for yaffs: 2048+64 per packet

        if (com_send_dword(com_handle, send_packet) != COM_STATUS_DONE)
        {
            return 336;
        }

        if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
        {
            return 337;
        }

        if (response != ACK)
        {
            unsigned int errorCode;

            if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
            {
                return 338;
            }
            return errorCode;
        }
        //2 send image to DA
        {
            const unsigned char *imageBuf = linux_images[index].buf;
            const unsigned int imageSize = linux_images[index].len;
            unsigned int sent_bytes = 0;
            unsigned int retry_count = 0;

            unsigned char buf[5];
            unsigned int packet_index = 0;
            unsigned int target_bytes = imageSize;
            unsigned int target_sent_bytes = 0;

            STATUS_E    da_ret;
            int ret;
           // send each rom file
            sent_bytes = 0;
            retry_count = 0;
            packet_index = 0;
            log_output("[%s]:%lu bytes, target_sent_bytes=%lu/%lu.\n",
                linux_images[index].name,
                imageSize,
                target_sent_bytes,
                target_bytes);
            while( sent_bytes < imageSize )
            {
                unsigned int j;
                unsigned short checksum;
                unsigned int frame_bytes;
re_transmission:
                // reset the frame checksum
                checksum = 0;
                // if the last frame is less then PACKET_LENGTH bytes
                if( send_packet > (imageSize-sent_bytes) )
                {
                    frame_bytes = imageSize - sent_bytes;
                }
                else
                {
                    // the normal frame
                    frame_bytes = send_packet;
                }
                // send frame
        		if(com_send_data(com_handle, imageBuf+sent_bytes, frame_bytes) != COM_STATUS_DONE)
        		{
        			goto read_cont_char;
        		}
                // calculate checksum
                for(j=0; j<frame_bytes; j++)
                {
                    // WARNING: MUST make sure it unsigned value to do checksum
                    checksum += imageBuf[sent_bytes+j];
                }
                // send 2 bytes checksum, high byte first
                buf[0] = (unsigned char)((checksum>> 8)&0x000000FF);
                buf[1] = (unsigned char)((checksum)    &0x000000FF);
          		if(com_send_data(com_handle, buf, 2) != COM_STATUS_DONE)
        		{
        			goto read_cont_char;
        		}
read_cont_char:

                // read CONT_CHAR
           		if(com_recv_data_chk_len(com_handle, &buf[0], 1) != COM_STATUS_DONE)
        		{
        			return 339;
        		}

                if( CONT_CHAR == buf[0] )
                {
                    // sent ok!, reset retry_count
                    retry_count = 0;
                }
                else
                {
                    // get error code
                    if (com_recv_dword(com_handle, &da_ret) != COM_STATUS_DONE)
                    {
                        return 340;
                    }
                    switch(da_ret)
                    {
                    case S_DA_UART_RX_BUF_FULL:
                        // target RX buffer is full, add delay to wait for flash erase done
                        //Sleep(DA_FLASH_ERASE_WAITING_TIME);
                    case S_DA_UART_DATA_CKSUM_ERROR:
                    case S_DA_UART_GET_DATA_TIMEOUT:
                    case S_DA_UART_GET_CHKSUM_LSB_TIMEOUT:
                    case S_DA_UART_GET_CHKSUM_MSB_TIMEOUT:
                        // check retry times
                        if( PACKET_RE_TRANSMISSION_TIMES > retry_count ) {
                            retry_count++;
                            log_output("PKT[%lu]:Retry(%u):(%d) received, start to re-transmit.\n",
                                    packet_index, retry_count, da_ret);
                        }
                        else {
                            // fail to re-transmission
                            log_output("PKT[%lu]: Retry(%u): stop to re-transmit! retry %u times fail!\n",
                                        packet_index, retry_count, retry_count);
                            // send NACK to wakeup DA to stop
                       		if(com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
                    		{
                    			return 341;
                    		}
                            return 342;
                        }

                        // wait for DA clean RX buffer
                        log_output("PKT[%lu]: Retry(%u): wait for DA clean it's RX buffer.\n", packet_index, retry_count);
                        if( COM_STATUS_DONE != (ret=com_recv_data_chk_len(com_handle, buf, 1)) )
                        {
                            log_output("PKT[%lu]: ReadData(): fail, Err(%d)\n", packet_index, ret);
                            return 343;
                        }
                        if( ACK != buf[0] )
                        {
                            log_output("PKT[%lu]: Retry(%u): wrong ack(0x%02X) return!\n", packet_index, retry_count, buf[0]);
                            return 344;
                        }

                        // send CONT_CHAR to wakeup DA to start recieving again
                        log_output("PKT[%lu]: Retry(%u): send CONT_CHAR to wakeup DA to start recieving again.\n", packet_index, retry_count);
                        if(com_send_byte(com_handle, CONT_CHAR) != COM_STATUS_DONE)
                        {
                            return 345;
                        }

                        // re-transmission this frame
                        log_output("PKT[%lu]: Retry(%u): re-transmission this frame, offset(%lu).\n",
                            packet_index,
                            retry_count,
                            sent_bytes);
                        goto re_transmission;
                        break;

                    default:
                        // flash erase timeout abort transmission
                        log_output("PKT[%lu]: (%d), abort transmission!\n",
                            packet_index,
                            da_ret);
                        return da_ret;
                    }
                }

                // update progress state
                sent_bytes += frame_bytes;
                target_sent_bytes += frame_bytes;
                packet_index++;
            }
        }
        //3 checksum
        {
            uint8 data;
            uint16 page_size;
            uint16 spare_size;
            //Use different checksum for YAFFS or RAW
            unsigned short pc_checksum = 0;
            unsigned short target_checksum = 0;

            if (com_recv_data_chk_len(com_handle, &data, 1) != COM_STATUS_DONE)
            {
                return 346;
            }
            if(data != S_DONE)
            {
                return 347;
            }

            // read page size and spare size from target
            if (com_recv_word(com_handle, &page_size) != COM_STATUS_DONE)
            {
                return 348;
            }
            if (com_recv_word(com_handle, &spare_size) != COM_STATUS_DONE)
            {
                return 349;
            }
            log_output("Get target flash page_size: 0x%x, spare_size: 0x%x\n", page_size, spare_size);

            if(linux_images[index].type == YAFFS)
            {
                log_output("Calculate YAFFS checksum\n");
                pc_checksum = GetYaffsImageChecksum(linux_images[index].buf, linux_images[index].len, page_size, spare_size);
            }
            else // for RAW partition
            {
                pc_checksum = GetImageChecksum(linux_images[index].buf, linux_images[index].len);
            }
            log_output("Sending PC-side ckecksum (0x%04X)\n", pc_checksum);
            if (com_send_word(com_handle, pc_checksum) != COM_STATUS_DONE)
            {
                return 350;
            }
            // Read checksum response < 4mins
            if( com_recv_data_chk_len(com_handle, &data, 1) != COM_STATUS_DONE )
            {
                return 351;
            }

            if (com_recv_word(com_handle, &target_checksum) != COM_STATUS_DONE)
            {
                return 352;
            }
            log_output("Getting target image ckecksum (0x%04X)\n", target_checksum);

            if(data != ACK)
            {
                log_output("Checksum Compare Fail\n");
                return 353;
            }

        }
    }


    return S_DONE;

}
STATUS_E SV5_CMD_NAND_DL_Encrypt_Partition_Data(COM_HANDLE com_handle,
                                const struct image *sec_img)
{
    unsigned char response = 0;
    unsigned short checksum =0;
    unsigned short checksumPC=0;
	unsigned int i=0;


    if (com_send_byte(com_handle, DA_DL_AND_ENCRYPT_LINUX_RAW_DATA) != COM_STATUS_DONE)
    {
        return 360;
    }

    if (com_send_dword(com_handle, sec_img->load_addr) != COM_STATUS_DONE)
    {
        return 361;
    }

    if (com_send_dword(com_handle, sec_img->len) != COM_STATUS_DONE)
    {
        return 362;
    }

    // environment checking
    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 363;
    }
    if (response != ACK)
    {
        if (response == NACK)
        {
            unsigned int errorCode;
            if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
            {
                return 364;
            }
            return errorCode;
        }
        return 365;
    }
    // send data
/*	if(com_send_data(com_handle, sec_img->buf, sec_img->len) != COM_STATUS_DONE)
	{
		return 366;
	}*/
    {
        unsigned int sent_bytes = 0;
        unsigned int frame_bytes = 0;
        while( sent_bytes < sec_img->len )
        {
            if( 4096 > (sec_img->len-sent_bytes) )
            {
                frame_bytes = sec_img->len - sent_bytes;
            }
            else
            {
                // the normal frame
                frame_bytes = 4096;
            }
            // send frame
    		if(com_send_data(com_handle, sec_img->buf+sent_bytes, frame_bytes) != COM_STATUS_DONE)
    		{
    			return 366;
    		}
            sent_bytes += frame_bytes;
        }
    }

    // send data done, calculate checksum
    for(i = 0; i < sec_img->len; i++)
    {
        checksumPC += sec_img->buf[i];
    }

    if (com_send_word(com_handle, checksumPC) != COM_STATUS_DONE)
    {
        return 367;
    }
    // receive response for checksum
	if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
	{
		return 368;
	}
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 369;
        }
        return errorCode;
    }

    // receive response for program
	if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
	{
		return 370;
	}
    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 371;
        }
        return errorCode;
    }
    log_output("Download encrypted data success\n");
    return S_DONE;


}


STATUS_E SV5_FlashTool_CheckPartitionTable(COM_HANDLE com_handle,
                                    const struct image *linux_images,
                                    const unsigned int num_linux_images,
                                    pt_resident* new_part,
                                    pt_info* pi)
{
    unsigned int part_num = 0;
    unsigned int tmp_addr = 0;
    unsigned int index = 0;
    unsigned int change_index = 0;
    unsigned int ret = 0;
    unsigned int count = 0;
    pt_resident original_part[PART_MAX_COUNT];
    int pt_changed = 0;

    for(index = 0;
        index < num_linux_images;
        ++index)
    {
        memcpy(new_part[index].name, linux_images[index].name, MAX_PARTITION_NAME_LEN);
        new_part[index].offset = linux_images[index].load_addr;//dm_part->part_info[part_num].start_addr;
        new_part[index].size = 0;
        new_part[index].mask_flags = 0;
        if (index >0)
        {
            new_part[index-1].size = (linux_images[index].load_addr - new_part[index-1].offset);
        }
    }



    // Start to query the partition info
    // get cout
    // get part info
    ret = SV5_CMD_ReadPartitionInfo(com_handle, original_part, &count, PART_MAX_COUNT);
    if ( S_DONE != ret )
    {
        // If this is the first time to dl, return S_PART_NO_VALID_TABLE
        return ret;
    }

    for (index = 0; index < num_linux_images; index++)
    {
        log_output("ON Host PART[%-2d]: [%-14s], (0x%016X), (0x%016X)\n", index, new_part[index].name, new_part[index].offset, new_part[index].size);
    }

    for(change_index = 0; change_index < count; change_index++)
    {
        if( (new_part[change_index].size != original_part[change_index].size)||
            (new_part[change_index].offset != original_part[change_index].offset))
        {
#if defined(_MSC_VER)
            log_output("Partition %d (%-14s) size changed from 0x%016I64X to 0x%016I64X\n\
            offset changed from 0x%016I64X to 0x%016I64X\n",
#else
log_output("Partition %d (%-14s) size changed from 0x%016llX to 0x%016llX\n\
offset changed from 0x%016llX to 0x%016llX\n",
#endif
            change_index,
            original_part[change_index].name,
            original_part[change_index].size,
            new_part[change_index].size,
            original_part[change_index].offset,
            new_part[change_index].offset);
            pt_changed = 1;
            break;
        }
    }

    if(pt_changed == 1)
    {
        //full linux binaries download
        // the download partition info is different with target's.
        ret = S_FT_PMT_MISMATCH;
    }
    else
    {
        ret = S_DONE;
    }

    return ret;
}

STATUS_E SV5_FlashTool_ReadPartitionCount(COM_HANDLE com_handle, unsigned int* count)
{
    STATUS_E ret = S_DONE;
    pt_resident temp[PART_MAX_COUNT];
    ret = SV5_CMD_ReadPartitionInfo(com_handle, temp, count, PART_MAX_COUNT);
    return ret;
}

STATUS_E SV5_CMD_ReadPartitionInfo(COM_HANDLE com_handle,
                                 pt_resident* part,
                                 unsigned int * part_num,
                                 const unsigned int max_num)
{
    unsigned char response = 0;
    unsigned int index = 0;
    unsigned int total_len, table_size;

    if (com_send_byte(com_handle, DA_READ_PARTITION_TBL_CMD) != COM_STATUS_DONE)
    {
        return 980;
    }

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 981;
    }

    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 982;
        }
        return errorCode;//if this is the first time to dl, DA will return S_PART_NO_VALID_TABLE
    }
    //command is allowed
    if (com_recv_dword(com_handle, &total_len) != COM_STATUS_DONE)
    {
        return 983;
    }

    if (0 != (total_len % sizeof(pt_resident)))
    {
        log_output("PartitionInfo not match: total_len(%d), entry_len(%d).", total_len, sizeof(pt_resident));
        if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
        {
            return 984;
        }
        else
        {
    	    return 985;
        }
    }
    else if (max_num < (total_len/sizeof(pt_resident)))
    {
        log_output("insufficient memory: total_len(%d), entry_len(%d), maxnum(%d)", total_len, sizeof(pt_resident), max_num);
        if (com_send_byte(com_handle, NACK) != COM_STATUS_DONE)
        {
            return 986;
        }
        else
        {
    	    return 987;
        }

    }
    else
    {
        if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
        {
            return 988;
        }
    }

    table_size = total_len/sizeof(pt_resident);
	if (com_recv_data_chk_len(com_handle, (unsigned char*)part, total_len) != COM_STATUS_DONE)
	{
		return 989;
	}

    if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
    {
        return 990;
    }
    // dump partitions
    for(index = 0; index < table_size; ++index)
    {
#if defined(_MSC_VER)
        log_output("ON Target PART[%-2d](%-14s) - offset (0x%016I64X) - size (0x%016I64X) - mask (0x%016I64X)\n",
#else
        log_output("ON Target PART[%-2d](%-14s) - offset (0x%016llX) - size (0x%016llX) - mask (0x%016llX)\n",
#endif
            index,
            part[index].name,
            part[index].offset,
            part[index].size,
            part[index].mask_flags);
    }
    *part_num = table_size;

    {
        unsigned int part_size = 0xffff;
        //There must be one of partitions whose size is 0
        for (index = 0; index < table_size; index++)
        {
            if(part[index].size ==0)
            {
            	part_size = index+1;
                break;
            }
        }
        if (part_size == 0xffff)
        {
            log_output("The table may be corrupted!");
            return S_FT_LAST_PARTITION_NOT_FOUND;
        }
    }
    return S_DONE;
}
STATUS_E SV5_CMD_WritePartitionInfo(COM_HANDLE com_handle,
                                         const struct image *linux_images,
                                         const unsigned int num_linux_images,
                                         pt_resident* new_part,
                                         const unsigned int max_num,
                                         int bIsUpdated)
{
    unsigned char response = 0;
    unsigned int index = 0;
    const int pt_size = sizeof(pt_resident);
    const int pi_size = sizeof(pt_info);

    if (com_send_byte(com_handle, DA_WRITE_PARTITION_TBL_CMD) != COM_STATUS_DONE)
    {
        return 300;
    }

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 301;
    }

    if (response != ACK)
    {
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 302;
        }
        return errorCode;
    }
    //command is allowed
    if (com_send_dword(com_handle, num_linux_images) != COM_STATUS_DONE)
    {
        return 303;
    }
    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 304;
    }
    if (response != ACK)
    {
        return 305;
    }

    // target table size is received.

    for (index = 0; index < num_linux_images; ++index)
    {
        // write
		if(com_send_data(com_handle, (unsigned char *)&new_part[index], pt_size) != COM_STATUS_DONE)
		{
			return 306;
		}
        // receive response
		if(com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
		{
			return 307;
		}
        // found
		if( response == ACK )
		{
			break;
		}
		// not found
		else if( response == CONT_CHAR )
		{
			continue;
		}
		// something wrong
		else
		{
			return 308;
		}
    }

    // Send the table update flag
	if (com_send_dword(com_handle, bIsUpdated))
	{
		return 309;
	}

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return 310;
    }

    if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return 311;
    }
	if( response != ACK )
	{
        unsigned int errorCode;
        if (com_recv_dword(com_handle, &errorCode) != COM_STATUS_DONE)
        {
            return 312;
        }
        return errorCode;

	}
    log_output("(%s) Partition table Done ...", bIsUpdated?"UPDATE":"NEW");
    return S_DONE;
}


STATUS_E da_disconnect(COM_HANDLE com_handle)
{
    unsigned char response = 0;
    unsigned int resetFlag = 0x0; // 0x0 reset for normal mode, 0x1 for download mode

    if (com_send_byte(com_handle, DA_FINISH_CMD) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }

    if (com_send_dword(com_handle, resetFlag) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }
    if (com_recv_data_chk_len(com_handle, &response, 1)!= COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }
    if (response != ACK)
    {
        return S_FT_FINISH_CMD_FAIL;
    }

    return S_DONE;
}

STATUS_E da_change_baudrate_phase1(COM_HANDLE com_handle)
{
    unsigned char response = 0;

    if (com_send_byte(com_handle, DA_SPEED_CMD) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (com_send_byte(com_handle, UART_BAUD_921600) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    // Disable synchronization
    if (com_send_byte(com_handle, 0) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (response != ACK)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    return S_DONE;
}


STATUS_E da_change_baudrate_phase2(COM_HANDLE com_handle)
{
    unsigned char response = 0;

    if (com_send_byte(com_handle, SYNC_CHAR) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (response != SYNC_CHAR)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }
#if defined(_MSC_VER)
    // delay for redundant SYNC_CHAR ack
    Sleep(50);
    // purge com
    if(!PurgeComm(com_handle, PURGE_TXABORT|PURGE_TXCLEAR|PURGE_RXABORT|PURGE_RXCLEAR)) {
        //MTRACE_ERR(g_hBROM_DEBUG, "DA_cmd::CMD_ChangeUartSpeed(): SYNC(%lu): PurgeComm() fail! , Err(%u).", g_BaudrateTable[BaudrateId-1], GetLastError());
        return 8;
    }
#endif

    if (com_send_byte(com_handle, ACK) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    // Wait for DA to purge its RX/TX FIFO
    Sleep(200);


    if (com_recv_byte(com_handle, &response) != COM_STATUS_DONE)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

    if (response != ACK)
    {
        return S_FT_CHANGE_BAUDRATE_FAIL;
    }

//send test bytes

    return S_DONE;
}

STATUS_E da_EnableWatchDog(COM_HANDLE com_handle, unsigned short  ms_timeout_interval)
{
    unsigned char response = 0;
    unsigned int resetFlag = 0x0; // 0x0 reset for normal mode, 0x1 for download mode

    if (com_send_byte(com_handle, DA_ENABLE_WATCHDOG_CMD) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }

    if (com_send_word(com_handle, ms_timeout_interval) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }

    if (com_send_dword(com_handle, resetFlag) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }
    if (com_recv_data_chk_len(com_handle, &response, 1) != COM_STATUS_DONE)
    {
        return S_FT_FINISH_CMD_FAIL;
    }
    if (response != ACK)
    {
        return S_FT_FINISH_CMD_FAIL;
    }

    return S_DONE;
}
