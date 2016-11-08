/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */


#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <getopt.h>
#include <limits.h>
#include <linux/input.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/reboot.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <cutils/properties.h>
#include <unistd.h>
#include <pthread.h>


#include "common.h"
#include "ftm.h"
#include "miniui.h"
#include "utils.h"

#include "libnvram.h"
#include "CFG_file_info_custom.h"

#include "item.h"


#define TAG "[FACTORY] "

#define FTM_CUST_FILE1  "/sdcard/factory.ini"
#define FTM_CUST_FILE2  "/etc/factory.ini"

#define MAX_RETRY_COUNT 20

static item_t ftm_menu_items[] = {
    //item(ITEM_MUI_TEST,"Mini-UI Test"),
    item(ITEM_AUTO_TEST, uistr_auto_test),
    item(ITEM_FULL_TEST, uistr_full_test),
    item(ITEM_ITEM_TEST, uistr_item_test),
    item(ITEM_REPORT,    uistr_test_report),
    item(ITEM_DEBUG_TEST,uistr_debug_test),
    #ifdef FEATURE_FTM_CLEARFLASH
        item(ITEM_CLRFLASH,  uistr_clear_flash),
    #endif
    #ifdef FEATURE_FTM_CLEAREMMC
        item(ITEM_CLREMMC,  uistr_clear_emmc),
    #endif
    item(ITEM_VERSION,   uistr_version),
    item(ITEM_REBOOT,    uistr_reboot),
    item(ITEM_MAX_IDS,   NULL),
};

extern item_t ftm_cust_items[ITEM_MAX_IDS];
 bool at_command_processor();
//add for saving test report
enum {
    TEST_REPORT_UNTEST,
    TEST_REPORT_PASS,
	 TEST_REPORT_FAIL,
};

// add for restore test report from testreport.log
int file_exist(char * filename)
{
    return (access(filename, 0) == 0);
}

int restore_item_result(char *item_name, char *result)
{
    item_t *items;
    int item_id;
    struct ftm_module *mod;
    items = get_item_list();
    item_id = get_item_id(items, item_name);
    mod = ftm_get_module(item_id);
    if (mod && mod->visible)
	  {
	    switch (*result)
        {
        case ' ':
            mod->test_result = 0;
            break;
                    
        case 'O':
            mod->test_result = 1;
            break;
              
        case 'X':
            mod->test_result = 2;
            break;
  
        default:
            
            break;
        }   
        
    }
    return 0;
}

int restore_test_report()
{
    FILE *fp = NULL;
    char item_result[32] = {0};
    char *result;
    int filepos = 0;
    LOGD(TAG "Entry %s\n", __FUNCTION__);
    if(file_exist(TEST_REPORT_SAVE_FILE))
    {
        fp = open_file(TEST_REPORT_SAVE_FILE);
        if(fp == NULL)
        {
            LOGD(TAG "Can not open the log file\n");
            return -1;
        }
        fseek(fp, 0, SEEK_END);
        filepos = ftell(fp);
        if(filepos == 0)
        {
            LOGD(TAG "file is null\n");
            fclose(fp);
            return -1;
        }
        rewind(fp);
        while(fp != (FILE *)EOF)
        {
            fgets(item_result, 32, fp);
            result = strchr(item_result, '=');
            if(result == NULL)
            {
                continue;
            }
            else
            {
                *result = 0;
                result++;
                restore_item_result(item_result, result);
            }
        }
        fclose(fp);
    }
    LOGD(TAG "Leave %s\n", __FUNCTION__);
    return 0;
}

int write_test_report(item_t *items, FILE *fp)
{

    int i = 0, test_report_len = 0, write_result = -1;
	char test_report[ITEM_MAX_IDS * 40] = {0};
	char *get_test_report = test_report;
	char result[] = { ' ', 'O', 'X' };
	int state = 0;
  if((items == NULL) || (fp == NULL))
  {
      return -1;
  }
    while (i < ITEM_MAX_IDS && items->name) 
    {

//		LOGD(TAG "items.name=%s item.background=%d", items->name, items->background);
		if(items->background == 0)
		{
            state = TEST_REPORT_UNTEST;
		}
		else if (items->background == COLOR_GREEN)
		{
            state = TEST_REPORT_PASS;
		}
		else if (items->background == COLOR_RED)
		{
            state = TEST_REPORT_FAIL;
		}
//		LOGD(TAG "state = %d", state);
        if(strncmp(items->name, uistr_info_test_report_back, strlen(uistr_info_test_report_back)))
        {
		    get_test_report = test_report + test_report_len;
            test_report_len += snprintf(get_test_report, 40, "%s=%c\n", items->name+4,
				result[state]);
        }
//		LOGD(TAG "%s", get_test_report);
//	    LOGD(TAG "%s", test_report);

        i++;
        items++;
    }

    LOGD(TAG "before write");
	LOGD(TAG "%s", test_report);
    write_result = fputs(test_report, fp);
	LOGD(TAG "The result of fputs is %d", write_result);

	return 0;
}
int create_report(item_t *item, item_t *rpt_items, int maxitems, char *buf, int size)
{
    struct ftm_module *mod;
    int i = 0, len = 0;
    char *ptr = buf;
    char result[] = { ' ', 'O', 'X' };
    color_t bgc[] = { 0, COLOR_GREEN, COLOR_RED };
    //handle of testreport.log
	  FILE *fp = NULL;

    if((item == NULL) || (rpt_items == NULL) || (buf == NULL))
    {
        return 0;
    }

    while (i < maxitems && item->name) 
    {
        mod = ftm_get_module(item->id);
        if (mod && mod->visible && len < size) 
        {
            ptr = buf + len;
            len += sprintf(ptr, "[%c] %s ",
                (mod->test_result >= FTM_TEST_MAX) ?
                result[FTM_TEST_UNKNOWN] : result[mod->test_result], item->name);
            ptr[len++] = '\0';
            rpt_items[i].id = mod->id;
            rpt_items[i].name = ptr;
            rpt_items[i].background = (mod->test_result >= FTM_TEST_MAX) ?
                0 : bgc[mod->test_result];
            i++;
        }
        item++;
    }

    //add for saving test report
    fp = open_file(TEST_REPORT_SAVE_FILE);

	if(fp == NULL)
	{
	    LOGD(TAG "TEST_REPORT_SAVE_FILE is null");
	}
	else
	{
	    LOGD(TAG "TEST_REPORT_SAVE_FILE is not null");
        write_test_report(rpt_items, fp);
		fclose(fp);
        fp = NULL;
	}
    //add for saving test report

    if (i < maxitems - 1) 
    {
        rpt_items[i].id   = ITEM_MAX_IDS;
        rpt_items[i].name = uistr_info_test_report_back;
    }
    return ++i;
}

static int item_test_report(item_t *items, char *buf, int bufsz)
{

    int chosen_item = 0;
    int quit = 0;
    struct itemview triv; /* test report item view */
    item_t rpt_items[ITEM_MAX_IDS + 1];
    text_t tr_title;
    struct ftm_param param;

    if((items == NULL) || (buf == NULL))
    {
        return 0;
    }

    init_text(&tr_title, uistr_test_report, COLOR_YELLOW);

    ui_init_itemview(&triv);

    quit = 0;
    memset(rpt_items, 0, sizeof(item_t) * (ITEM_MAX_IDS + 1));
    create_report(items, rpt_items, ITEM_MAX_IDS, buf, bufsz);
    triv.set_title(&triv, &tr_title);
    triv.set_items(&triv, rpt_items, 0);
    while (quit == 0) 
    {
        chosen_item = triv.run(&triv, &quit);
        if(quit == 1)
        break;
        if (chosen_item == ITEM_MAX_IDS)
        break;
        param.name = get_item_name(items, chosen_item);
        param.test_type = get_item_test_type(items, param.name);
        LOGD(TAG "%s, param.type = %d\n", __FUNCTION__ , param.test_type);
        ftm_entry(chosen_item, &param);
        create_report(items, rpt_items, ITEM_MAX_IDS, buf, bufsz);
    }
    return 0;
}

static int full_test_mode(char *buf, int bufsz)
{
    int i = 0;
    item_t *items;
    struct ftm_module *mod;
    struct ftm_param param;
	  item_t rpt_items[ITEM_MAX_IDS + 1];
    int stopmode = 0;
    char *stopprop = ftm_get_prop("FTM.FailStop");

    if(buf == NULL)
    {
        return -1;
    }

    if (stopprop && !strncasecmp(stopprop, "yes", strlen("yes")))
        stopmode = 1;

    LOGD(TAG "full_test_mode: %d", stopmode);

//    items = get_manual_item_list();

    items = ftm_cust_items;

	LOGD(TAG "get_manual_item_list end");

    while (items[i].name)
	{
		LOGD(TAG "name = %s,id = %d,mode=%d",items[i].name,items[i].id,items[i].mode);
		if(items[i].mode != FTM_AUTO_ITEM)
		{
            LOGD(TAG "%s:%d", items[i].name, items[i].id);

        	switch (items[i].id)
			{
                case ITEM_IDLE: /* skip items */
                    break;
                case ITEM_REPORT:
                    item_test_report(items, buf, bufsz);
                    break;
                default:
                    mod = ftm_get_module(items[i].id);
                	if (mod && mod->visible)
    				{
                        param.name = items[i].name;
                        param.test_type = FTM_MANUAL_ITEM;
                        LOGD(TAG "%s, param.type = %d\n", __FUNCTION__ , param.test_type);
                        ftm_entry(items[i].id, &param);
                        if (stopmode && mod->test_result != FTM_TEST_PASS)
                        continue;
                   }
                   break;
            }
         }
        i++;
    }

    //add for saving test report
    create_report(items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
    //add for saving test report

    return 0;
}

static int auto_test_mode(char *buf, int bufsz)
{
    int i = 0;
    item_t *items, *cust_items;
    struct ftm_module *mod;
    struct ftm_param param;

    //add for saving test report
    item_t rpt_items[ITEM_MAX_IDS + 1];
    int stopmode = 0;
    char *stopprop = ftm_get_prop("FTM.FailStop");

    if(buf == NULL)
    {
        return -1;
    }

    if (stopprop && !strncasecmp(stopprop, "yes", strlen("yes")))
        stopmode = 1;

    LOGD(TAG "auto_test_mode: %d", stopmode);

    items = get_auto_item_list();
    //add for saving test report
    cust_items = get_item_list();
    memset(rpt_items, 0, sizeof(item_t) * (ITEM_MAX_IDS + 1));

    while (items[i].name) 
    {
        LOGD(TAG "%s:%d", items[i].name, items[i].id);
        switch (items[i].id) 
        {
            case ITEM_IDLE: /* skip items */
                break;
            case ITEM_REPORT:
                item_test_report(items, buf, bufsz);
                break;
            default:
                mod = ftm_get_module(items[i].id);
                if (mod && mod->visible) 
                {
                    param.name = items[i].name;
                    param.test_type = FTM_AUTO_ITEM;
                    LOGD(TAG "%s, param.type = %d\n", __FUNCTION__ , param.test_type);
                    ftm_entry(items[i].id, &param);
                    if (stopmode && mod->test_result != FTM_TEST_PASS)
                        continue;
                }
                break;
        }
        i++;
    }

    //add for saving testreport.log
    create_report(cust_items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
    //add for saving testreport.log

    return 0;
}

static int item_test_mode(char *buf, int bufsz)
{
    int chosen_item = 0;
    int exit = 0;
    struct itemview itv;  /* item test menu */
    struct ftm_param param;
    text_t  title;
    item_t *items;

    //add for saving test report
	item_t rpt_items[ITEM_MAX_IDS + 1];

    if(buf == NULL)
    {
        return -1;
    }

    LOGD(TAG "item_test_mode");

    items = get_item_list();

    ui_init_itemview(&itv);
    init_text(&title, uistr_item_test, COLOR_YELLOW);

    itv.set_title(&itv, &title);
    itv.set_items(&itv, items, 0);

    while (1) 
    {
        chosen_item = itv.run(&itv, &exit);
        if (exit == 1)
            break;
        switch (chosen_item) 
        {
            case ITEM_REPORT:
                item_test_report(items, buf, bufsz);
                break;
            default:
                param.name = get_item_name(items, chosen_item);
                param.test_type = get_item_test_type(items, param.name);
                LOGD(TAG "%s, param.type = %d\n", __FUNCTION__ , param.test_type);
                ftm_entry(chosen_item, &param);
                LOGD(TAG "ITEM TEST ftm_entry before");
                //add for saving test report
    	        create_report(items, rpt_items, ITEM_MAX_IDS , buf, bufsz);
                //add for saving test report
                break;
        }
    }
    return 0;
}

static int debug_test_mode(char *buf, int bufsz)
{
    int chosen_item = 0;
    int exit = 0;
    struct itemview itv;  /* item test menu */
    struct ftm_param param;
    text_t  title;
    item_t *items;

    if(buf == NULL)
    {
        return -1;
    }

    LOGD(TAG "debug_test_mode");

    items = get_debug_item_list();

    ui_init_itemview(&itv);
    init_text(&title, uistr_item_test, COLOR_YELLOW);

    itv.set_title(&itv, &title);
    itv.set_items(&itv, items, 0);

    while (1) 
    {
        chosen_item = itv.run(&itv, &exit);
        if (exit == 1)
            break;
        switch (chosen_item) 
        {
            default:
    			LOGD(TAG "chosen_item=%d",chosen_item);
                param.name = get_item_name(items, chosen_item);
                ftm_debug_entry(chosen_item, &param);
                break;
        }
    }
    return 0;
}

//MTKBEGIN  [mtk0625][DualTalk]
#if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
#define EXT_MD_IOC_MAGIC			'E'
#define EXT_MD_IOCTL_LET_MD_GO		_IO(EXT_MD_IOC_MAGIC, 1)
#define EXT_MD_IOCTL_REQUEST_RESET	_IO(EXT_MD_IOC_MAGIC, 2)
#define EXT_MD_IOCTL_POWER_ON_HOLD	_IO(EXT_MD_IOC_MAGIC, 3)

int boot_modem(int is_reset)
{
    LOGD(TAG "%s\n", __FUNCTION__);

	int ret;
	int ext_md_ctl_n0, ext_md_ctl_n1;

	ext_md_ctl_n0 = open("/dev/ext_md_ctl0", O_RDWR);
	if(ext_md_ctl_n0 <0) 
    {
        LOGD(TAG "open ext_md_ctl0 fail");
		return	ext_md_ctl_n0;
	}
	ret = ioctl(ext_md_ctl_n0, EXT_MD_IOCTL_POWER_ON_HOLD, NULL);
	if (ret < 0) 
    {
        LOGD(TAG "power on modem fail");
		return	ret;
	}

	ext_md_ctl_n1 = open("/dev/ext_md_ctl1", O_RDWR);
	if(ext_md_ctl_n1 <0) 
    {
        LOGD(TAG "open ext_md_ctl_n1 fail");
		return	ext_md_ctl_n1;
	}
	ret = ioctl(ext_md_ctl_n1, EXT_MD_IOCTL_LET_MD_GO, NULL);
	if (ret < 0) 
    {
        LOGD(TAG "EXT_MD_IOCTL_LET_MD_GO fail");
		return	ret;
	}

	return	ret;
}
#endif  /* MTK_DT_SUPPORT */
//MTKEND    [mtk80625][DualTalk]

int main(int argc, char **argv)
{
    int exit = 0;
    char *buf = NULL;
    // for read back test report
    int first_entry = 1;
    struct ftm_param param;
    struct itemview fiv;  /* factory item menu */
    item_t *items;
    text_t ftm_title;
    int bootMode;
    ui_init();
    /* CHECKME! should add this fuctnion to avoid UI not displayed */
    show_slash_screen(uistr_factory_mode, 1000);
    bootMode = getBootMode();
    if(ATE_FACTORY_BOOT == bootMode)
    {
        ui_print("Enter ATE factory mode...\n");

        ate_signal();
        #ifdef FEATURE_FTM_MEMCARD
        mcard_init();
        #endif
        while(1){}
    }
    else if(FACTORY_BOOT == bootMode)
    {
		 buf = (char *)malloc(BUFSZ);
		 if (NULL == buf)
		 {
		     ui_print("Fail to get memory!\n");
		 }

        ftm_init();
        if (!read_config(FTM_CUST_FILE1))
            read_config(FTM_CUST_FILE2);
        //for ata test
        at_command_processor();
    	LOGD(TAG "pc control stops!\n");

        ui_init_itemview(&fiv);

        init_text(&ftm_title, uistr_factory_mode, COLOR_YELLOW);

        items = ftm_menu_items;
        fiv.set_title(&fiv, &ftm_title);
        fiv.set_items(&fiv, items, 0);

        #if defined(MTK_EXTERNAL_MODEM_SLOT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
        boot_modem(0);
        #endif  /* MTK_DT_SUPPORT */

		while (!exit) 
		{
            int chosen_item = fiv.run(&fiv, NULL);
			switch (chosen_item) 
			{
            case ITEM_FULL_TEST:
                full_test_mode(buf, BUFSZ);
                break;
	        case ITEM_AUTO_TEST:
                auto_test_mode(buf, BUFSZ);
	            item_test_report(get_auto_item_list(), buf, BUFSZ);
                break;
            case ITEM_ITEM_TEST:
                item_test_mode(buf, BUFSZ);
                break;
		    case ITEM_DEBUG_TEST:
    			debug_test_mode(buf, BUFSZ);
                break;
            case ITEM_REPORT:
                // when first entry, try to read back test report
                if(first_entry)
                {
                    restore_test_report();
                    first_entry = 0;
                }
                item_test_report(get_item_list(), buf, BUFSZ);
                break;
            case ITEM_VERSION:
                display_version(0, NULL, false);				
                break;
            case ITEM_REBOOT:
                exit = 1;
                fiv.exit(&fiv);
                break;
            default:
                param.name = get_item_name(items, chosen_item);
                ftm_entry(chosen_item, &param);
                break;
            }
		}//end while

    	if (buf)
        {   
    	    free(buf);
            buf = NULL;
        }

    	ui_printf("Entering factory reset mode...\n");
    	ui_printf("Rebooting...\n");
    	//sync();
    	property_set("sys.powerctl","reboot");

    	return EXIT_SUCCESS;

    }
    else
    {
        LOGD(TAG "Unsupported Factory mode\n");
    }
	
    return EXIT_SUCCESS;
}

#if defined(FEATURE_FTM_WIFI_ONLY)
int get_barcode_from_nvram(char *barcode_result)
{

	int read_nvram_ready_retry = 0;
	F_ID fid;
	int rec_size = 0;
	int rec_num = 0;
	int barcode_lid = AP_CFG_REEB_PRODUCT_INFO_LID;
	PRODUCT_INFO *barcode_struct;
	bool isread = true;
	char nvram_init_val[128] = {0};
	LOGD(TAG "Entry get_barcode_from_nvram");
    if(barcode_result == NULL)
    {
        return 0;
    }
	while(read_nvram_ready_retry < MAX_RETRY_COUNT)
	{
		read_nvram_ready_retry++;
		property_get("service.nvram_init", nvram_init_val, NULL);
		if(strcmp(nvram_init_val, "Ready") == 0)
		{
			break;
		}
		else
		{
			usleep(500*1000);
		}
	}

	if(read_nvram_ready_retry >= MAX_RETRY_COUNT)
	{
		LOGD(TAG "Get nvram restore ready failed!");
		return 0;
	}

	barcode_struct= (PRODUCT_INFO *)malloc(sizeof(PRODUCT_INFO));
	if(barcode_struct == NULL)
	{
		return 0;
	}

	fid = NVM_GetFileDesc(barcode_lid, &rec_size, &rec_num, isread);

	if(fid.iFileDesc < 0)
	{
		LOGD(TAG "fid.iFileDesc < 0");
		return 0;
	}

	if(rec_size != read(fid.iFileDesc, barcode_struct, rec_size))
	{
		free(barcode_struct);
		return 0;
	}
	if(strlen((const char *)barcode_struct->barcode) > 0)
	{
		strcpy(barcode_result, (const char *)barcode_struct->barcode);
	}
    else
	{
		strcpy(barcode_result, "unknown");
	}

	free(barcode_struct);
	if(!NVM_CloseFileDesc(fid))
	{
		return 0;
	}
	LOGD(TAG "The size of barcode_struct:%d\n", sizeof(barcode_struct));
	LOGD(TAG "Barcode is %s\n", barcode_result);
	return 1;
}
#endif

     
