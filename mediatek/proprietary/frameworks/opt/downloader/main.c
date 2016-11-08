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

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "download_images.h"
#include "cfg_reader.h"
#include "interface.h"

#if defined(__GNUC__)
#include "errno.h"
#include "GCC_Utility.h"
#include <getopt.h>
#endif


#define BR_ENUM_TO_CASE_STRING(case_id)\
case case_id:\
	return #case_id;


static const char *DEFAULT_CFG_FILE_PATH = "/system/etc/firmware/EXT_MODEM_BB.cfg";
static const char *DEFAULT_DA_FILE_PATH ="/system/etc/firmware/downloader_da_6261/";


/************************ Define DA address in target *****************************************/
static const unsigned int DOWNLOAD_AGENT_LOAD_ADDR = 0x70007000;
static const unsigned int DOWNLOAD_AGENT_TCM_LOAD_ADDR = 0x10020000;
static const unsigned int DOWNLOAD_EPP_LOAD_ADDR = 0x70008000; // for MT6260 only


const char *GetPartitionTypeName(PartitionType part_type)
{
    switch(part_type) {

        BR_ENUM_TO_CASE_STRING(RAW)
        BR_ENUM_TO_CASE_STRING(YAFFS)
        default:
            return "UNKNOW";
    }
}

const char *GFH_GetFileTypeName(GFH_FILE_TYPE file_type)
{
    switch(file_type) {

        BR_ENUM_TO_CASE_STRING(GFH_FILE_NONE)
        BR_ENUM_TO_CASE_STRING(ARM_BL)
        BR_ENUM_TO_CASE_STRING(ARM_EXT_BL)
        BR_ENUM_TO_CASE_STRING(DUALMAC_DSP_BL)
        BR_ENUM_TO_CASE_STRING(SCTRL_CERT)
        BR_ENUM_TO_CASE_STRING(TOOL_AUTH)
        BR_ENUM_TO_CASE_STRING(FILE_MTK_RESERVED1)
        BR_ENUM_TO_CASE_STRING(EPP)
        BR_ENUM_TO_CASE_STRING(FILE_MTK_RESERVED2)
        BR_ENUM_TO_CASE_STRING(FILE_MTK_RESERVED3)
        BR_ENUM_TO_CASE_STRING(ROOT_CERT)
        BR_ENUM_TO_CASE_STRING(AP_BL)

        BR_ENUM_TO_CASE_STRING(PRIMARY_MAUI)
        BR_ENUM_TO_CASE_STRING(SECONDARY_MAUI)
        BR_ENUM_TO_CASE_STRING(ON_DEMAND_PAGING)
        BR_ENUM_TO_CASE_STRING(THIRD_ROM)
        BR_ENUM_TO_CASE_STRING(DSP_ROM)
        BR_ENUM_TO_CASE_STRING(CACHED_DSP_ROM)
        BR_ENUM_TO_CASE_STRING(FIRST_FACTORY_MAUI)
        BR_ENUM_TO_CASE_STRING(SECONDARY_FACTORY_MAUI)
        BR_ENUM_TO_CASE_STRING(VIVA)
        BR_ENUM_TO_CASE_STRING(TINY_SYS_ROM1)
        BR_ENUM_TO_CASE_STRING(TINY_SYS_ROM2)
        BR_ENUM_TO_CASE_STRING(LTE_DSP_ROM)

        BR_ENUM_TO_CASE_STRING(CUSTOM_PACK)
        BR_ENUM_TO_CASE_STRING(LANGUAGE_PACK)
        BR_ENUM_TO_CASE_STRING(JUMP_TABLE)

        BR_ENUM_TO_CASE_STRING(FOTA_UE)

        BR_ENUM_TO_CASE_STRING(SECURE_RO_S)
        BR_ENUM_TO_CASE_STRING(SECURE_RO_ME)

        BR_ENUM_TO_CASE_STRING(CARD_DOWNLOAD_PACKAGE)
        BR_ENUM_TO_CASE_STRING(CONFIDENTIAL_BINARY)

        BR_ENUM_TO_CASE_STRING(FILE_SYSTEM)

        BR_ENUM_TO_CASE_STRING(BOOT_CERT)
        BR_ENUM_TO_CASE_STRING(BOOT_CERT_DUMMY_BIN)

        default:
            return "UNKNOW";
    }
}

// Note: windows doesn't support getopt().  Linux is support in getopt.h
#if defined(_MSC_VER)
char	*optarg;		// global argument pointer
int		optind = 0; 	// global argv index
int getopt(int argc, char *argv[], char *optstring)
{
	static char *next = NULL;
    char c ;
	char *cp = NULL;

    if (optind == 0)
		next = NULL;

	optarg = NULL;

	if (next == NULL || *next == L'\0')
	{
		if (optind == 0)
			optind++;

		if (optind >= argc || argv[optind][0] != L'-' || argv[optind][1] == L'\0')
		{
			optarg = NULL;
			if (optind < argc)
				optarg = argv[optind];
			return EOF;
		}

		if (strcmp(argv[optind], "--") == 0)
		{
			optind++;
			optarg = NULL;
			if (optind < argc)
				optarg = argv[optind];
			return EOF;
		}

		next = argv[optind];
		next++;		// skip past -
		optind++;
	}

	c = *next++;
	cp = strchr(optstring, c);

	if (cp == NULL || c == ':')
		return '?';

	cp++;
	if (*cp == ':')
	{
		if (*next != '\0')
		{
			optarg = next;
			next = NULL;
		}
		else if (optind < argc)
		{
            if(argv[optind][0] == '-')
            {
                optarg = NULL;
            }
            else
            {
			    optarg = argv[optind];
			    optind++;
            }
		}
		else
		{
			optarg = NULL;
		}
	}

	return c;
}
#endif

int ParseArguments(int argc, char* argv[], char cfg_file_path[], char da_file_path[], unsigned int *comPortNum)
{
    int arg = 0;

	//
	// default value init
	//
	*comPortNum = 0;
    memset(cfg_file_path, 0 ,MAX_FILE_PATH_LEN);
    memset(da_file_path, 0 ,MAX_FILE_PATH_LEN);
	//
	// Parse command arguments
	//
    while ((arg = getopt(argc, argv, "i:c:a:")) != EOF)
    {
        switch (arg)
        {
            case 'i':
            {
                if(optarg != NULL)
                {
                    strncpy(cfg_file_path, optarg, MAX_FILE_PATH_LEN);
                }
                break;
            }
            case 'a':
            {
                if(optarg != NULL)
                {
                    strncpy(da_file_path, optarg, MAX_FILE_PATH_LEN);
                }
                break;
            }
            case 'c':
            {
                if(optarg != NULL)
                {
                    *comPortNum = (unsigned int)strtoul(optarg, NULL, 10);
                }
                break;
            }
        default:
            log_output("ERROR : ParseArguments() : Invalid argument flag. flag = (%c)\n",arg);
            return 1;
        }
    }

    if (strlen(cfg_file_path) == 0) {
    	strncpy(cfg_file_path, DEFAULT_CFG_FILE_PATH ,MAX_FILE_PATH_LEN);
    	log_output("ParseArguments() : no CFG path. def = [%s]\n",cfg_file_path);
    }

    if (strlen(da_file_path) == 0) {
    	strncpy(da_file_path, DEFAULT_DA_FILE_PATH, MAX_FILE_PATH_LEN);
        log_output("ParseArguments() : no da path. def = [%s]\n",da_file_path);
    }

    if(strlen(cfg_file_path) == 0 || strlen(da_file_path) == 0 )
    {
        log_output("ERROR : ParseArguments() : Program parameters is defect. Please make sure you have setup -i, -a, -c options.\n");
        return 2;
    }

    log_output("ParseArguments() : CFG path   = [%s]\n",cfg_file_path);
    log_output("ParseArguments() : DA  path   = [%s]\n",da_file_path);
    log_output("ParseArguments() : COM number = %d\n",*comPortNum);

    return S_DONE;
}


static int load_image_and_set_address(struct image *image, const char *file_path,
                       unsigned int load_addr)
{
    FILE *file;

    assert(image != NULL);
    if(file_path[0] == '\0')
    {
        image->buf = NULL;
        image->len = 0;
        image->load_addr = load_addr;
        return S_INVALID_ARGUMENTS;
    }

    file = fopen(file_path, "rb");
    if(file == NULL)
    {
        log_output("load_image_and_set_address() : File not found. path = (%s), errno = %d \n",file_path, errno);
        return S_FTHND_FILE_LOAD_FAIL;
    }

#if defined(_MSC_VER)
    image->len = _filelength(fileno(file));
#elif defined(__GNUC__)
    {
        fseek(file, 0L, SEEK_END);
        image->len = ftell(file);
        fseek(file, 0L, SEEK_SET);
    }
#endif
    assert(image->len != 0);

    image->buf = malloc(image->len);
    assert(image->buf != NULL);

    fread(image->buf, 1, image->len, file);
    fclose(file);

    image->load_addr = load_addr;

    return S_DONE;
}

static int load_image(struct image *image, const char *file_path)
{
    FILE *file;

    assert(image != NULL);
    if(file_path[0] == '\0')
    {
        image->buf = NULL;
        image->len = 0;
        image->load_addr = 0;
        return S_INVALID_ARGUMENTS;
    }

    file = fopen(file_path, "rb");
    if(file == NULL)
    {
        log_output("load_image() : File not found. path = (%s). errno = %d. \n",file_path, errno);
        return S_FTHND_FILE_LOAD_FAIL;
    }

#if defined(_MSC_VER)
    image->len = _filelength(fileno(file));
#elif defined(__GNUC__)
    {
        fseek(file, 0L, SEEK_END);
        image->len = ftell(file);
        fseek(file, 0L, SEEK_SET);
    }
#endif
    assert(image->len != 0);

    image->buf = malloc(image->len);
    assert(image->buf != NULL);

    fread(image->buf, 1, image->len, file);
    fclose(file);

    return S_DONE;
}

static int load_image_info(struct image *image, PartitionType type)
{
    GFH_FILE_INFO_v1 *fileInfo = NULL;

    // parse GFH_FILE_INFO to get load address, file type
    fileInfo = (GFH_FILE_INFO_v1 *)image->buf;

    if((fileInfo->m_gfh_hdr.m_magic_ver & 0x00FFFFFF) != GFH_HDR_MAGIC)
    {
        // check GFH_FILE_INFO_v1 is valid
        return 1;
    }

    image->load_addr = fileInfo->m_load_addr;
    image->gfh_file_type = fileInfo->m_file_type;
    image->type= type;
    strncpy(image->name, GFH_GetFileTypeName(fileInfo->m_file_type), MAX_PARTITION_NAME_LEN - 1);

    log_output("load_image_info(): File Type = [%s], load address = 0x%08x , format = %s\n",GFH_GetFileTypeName(fileInfo->m_file_type), image->load_addr, GetPartitionTypeName(type));

    return S_DONE;
}

static void release_image(struct image *image)
{
    assert(image != NULL);
    if(NULL != image->buf)
    {
        free(image->buf);
        image->buf = NULL;
    }
}

static int acquire_region_images( Region_images *region_images, RegionSetting region_setting)
{
    unsigned int i;
    STATUS_E ret = S_DONE;

    assert(region_images != NULL);

    region_images->num_images = region_setting.romNumber;

    if(region_images->num_images == 0)
    {
        return S_DONE; // no images
    }
    if(region_images->num_images > MAX_ROM_FILE_NUM)
    {
        // error images number
        return 1;
    }

    region_images->region_images = malloc(sizeof(struct image) * (region_images->num_images));
    assert(region_images->region_images != NULL);
    memset(region_images->region_images, 0, sizeof(struct image) * (region_images->num_images));

    for (i = 0; i < region_images->num_images; ++i)
    {
        struct image *image_i = &(region_images->region_images[i]);

        ret = load_image(image_i, region_setting.romFiles[i]);
        if(ret != S_DONE)
        {
            return ret;
        }

        ret = load_image_info(image_i, RAW);
        if(ret != S_DONE)
        {
            return ret;
        }
    }

    return S_DONE;
}

static void release_region_images(Region_images *region_images)
{
    unsigned int i;

    for (i = 0; i < region_images->num_images; ++i)
    {
        struct image *image_i = &(region_images->region_images[i]);
        if(image_i != NULL)
        {
            release_image(image_i);
        }
    }
    if(NULL != region_images->region_images)
    {
        free(region_images->region_images);
        region_images->region_images = NULL;
    }
}

static int acquire_download_agent(struct image *download_agent,const char *file_path, unsigned int load_addr)
{
    return load_image_and_set_address(download_agent, file_path,
		load_addr);
}


static void release_download_agent(struct image *download_agent)
{
    release_image(download_agent);
}


static int acquire_flash_table(struct image *flash_table,
                                const char *file_path)
{
    return load_image_and_set_address(flash_table, file_path, 0);
}

static void release_flash_table(struct image *flash_table)
{
    release_image(flash_table);
}

static int acquire_image(struct image *imageBuf,
						  const char *file_path, unsigned int load_addr)
{
    return load_image_and_set_address(imageBuf, file_path, load_addr);
}

static int load_DA(char da_file_path[], struct image *download_agent, struct image *download_agent_TCM, struct image *download_EPP, struct image *nor_flash_table, struct image *nand_flash_table)
{
    int ret = S_DONE;
    int cfg_base_folder_len = strlen(da_file_path);

    // Get INT_SYSRAM
    strcpy(da_file_path+cfg_base_folder_len, "/INT_SYSRAM");
    //log_output("TEST: load_DA() INT_SYSRAM path = [%s]\n", da_file_path);
    ret = acquire_download_agent(download_agent, da_file_path, DOWNLOAD_AGENT_LOAD_ADDR);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_DA() INT_SYSRAM fail! ret = %d\n", ret);
        return ret;
    }

    // Get EXT_RAM
    strcpy(da_file_path+cfg_base_folder_len, "/EXT_RAM");
    //log_output("TEST: load_DA() EXT_RAM path = [%s]\n", da_file_path);
	ret = acquire_download_agent(download_agent_TCM, da_file_path, DOWNLOAD_AGENT_TCM_LOAD_ADDR);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_DA() EXT_RAM fail! ret = %d\n", ret);
        return ret;
    }

    // Get EPP
    strcpy(da_file_path+cfg_base_folder_len, "/EPP");
    //log_output("TEST: load_DA() EPP path = [%s]\n", da_file_path);
	ret = acquire_download_agent(download_EPP,da_file_path, DOWNLOAD_EPP_LOAD_ADDR);
    if(ret != S_DONE)
    {
        log_output("WARNING: load_DA() EPP fail! ret = %d, It's fine for MT6261 platofrm.\n", ret);
        // 6261 has no EPP.
    }

    // Get NOR_FLASH_TABLE
    strcpy(da_file_path+cfg_base_folder_len, "/NOR_FLASH_TABLE");
    //log_output("TEST: load_DA() NOR_FLASH_TABLE path = [%s]\n", da_file_path);
    ret = acquire_flash_table(nor_flash_table, da_file_path);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_DA() NOR_FLASH_TABLE fail! ret = %d\n", ret);
        return ret;
    }

    // Get NAND_FLASH_TABLE
    strcpy(da_file_path+cfg_base_folder_len, "/NAND_FLASH_TABLE");
    //log_output("TEST: load_DA() NAND_FLASH_TABLE path = [%s]\n", da_file_path);
    ret = acquire_flash_table(nand_flash_table, da_file_path);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_DA() NAND_FLASH_TABLE fail! ret = %d\n", ret);
        return ret;
    }

    return S_DONE;
}

static int load_SW_images(CFG_Images_Name cfgImagesName, Region_images *boot_region_images, Region_images *main_region_images)
{
    int ret = S_DONE;

    // Load boot regin images (ARM_BL, ARM_EXT_BL)
    ret = acquire_region_images(boot_region_images, cfgImagesName.BootRegion);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_SW_images() boot regin images fail! ret = %d\n", ret);
        return ret;
    }

    // Load main regin images (ROM, VIVA)
    ret = acquire_region_images(main_region_images, cfgImagesName.MainRegion);
    if(ret != S_DONE)
    {
        log_output("ERROR: load_SW_images() main regin images fail! ret = %d\n", ret);
        return ret;
    }

    return S_DONE;
}

int main(int argc, char **argv)
{
    // Path string and COM number (get from argv)
    char cfg_file_path[MAX_FILE_PATH_LEN];
    char da_file_path[MAX_FILE_PATH_LEN];
    unsigned int comPortNum = 0;

    // Image buffer
    struct image download_agent = { 0 };
	struct image download_agent_TCM = { 0 };
	struct image download_EPP = { 0 };
    struct image nor_flash_table = { 0 };
    struct image nand_flash_table = { 0 };
    Region_images boot_region_images = { 0 };
    Region_images main_region_images = { 0 };
    CFG_Images_Name cfgImagesName = { 0 };
    ExternalMemorySetting externalmemorysetting = { 0 };
    unsigned int num_images = 0;
    unsigned int bmt_address = 0;
    int i = 0;
    STATUS_E ret = S_DONE;

    log_output("enter main() : argc = [%d]\n", argc);

    set_progress(0);
    // Parse arguments of main()
    if(S_DONE != (ret = ParseArguments(argc, argv, cfg_file_path, da_file_path,&comPortNum)))
    {
        log_output("ERROR: Parse input arguments fail! ParseArguments() ret = %d\n", ret);
        set_error_status(S_INVALID_ARGUMENTS);
        return S_INVALID_ARGUMENTS;
    }

    set_progress(1);
    // Parse CFG file to get SW information (image file name)
    ret = ParseConfigFile(cfg_file_path, &cfgImagesName, &externalmemorysetting);
    if(ret != S_DONE)
    {
        log_output("Native Downloader end, ParseConfigFile() failed ,ret = %d.\n", ret);
        goto end;
    }
    //DumpExternalMemorySetting(&externalmemorysetting);

    set_progress(2);
	// Acquire DAs, flash tables
    ret = load_DA(da_file_path, &download_agent, &download_agent_TCM, &download_EPP, &nor_flash_table, &nand_flash_table);
    if(ret != S_DONE)
    {
        log_output("Native Downloader end, load_DA() failed ,ret = %d.\n", ret);
        goto end;
    }

    set_progress(3);
    // Acquire SW images
    ret = load_SW_images(cfgImagesName, &boot_region_images, &main_region_images);
    if(ret != S_DONE)
    {
        log_output("Native Downloader end, load_SW_images() failed ,ret = %d.\n", ret);
        goto end;
    }

    set_progress(5);
    // start downloader
    ret = download_images(&download_agent,
					&download_agent_TCM,
					&nor_flash_table,
					&nand_flash_table,
					&download_EPP,
					&boot_region_images,
					&main_region_images,
					&externalmemorysetting,
					bmt_address,
					1, /*0:UART, 1:USB*/
					0, /*0:NOR,SF, 1:NAND*/  /* MT6260, 6261 is SF flash platform.*/
					comPortNum);

end:
    // free images buffer
    release_flash_table(&nand_flash_table);
    release_flash_table(&nor_flash_table);
    release_download_agent(&download_agent);
    release_download_agent(&download_agent_TCM);
    release_download_agent(&download_EPP);

    release_cfg_images_name(&cfgImagesName);
    release_region_images(&boot_region_images);
    release_region_images(&main_region_images);


    if(ret == S_DONE)
        set_progress(100);
    else
        set_error_status(ret);

    log_output("Native Downloader end, ret = %d \n", ret);
    return ret;
}
