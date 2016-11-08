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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>
#include <sys/ioctl.h>

#include <time.h>
#include <math.h>

#include "common.h"
#include "miniui.h"
#include "ftm.h"

#ifdef FEATURE_FTM_IRTX_LED

/*#define debug_data*/

#define TAG                 "[IrTx/LED] "

/* IOCTO */
#define IRTX_IOC_SET_IRTX_LED_OUT        _IOW('R', 10, unsigned int)
#define IRTX_IOC_SET_CARRIER_FREQ        _IOW('R', 0, unsigned int)

static int decode_data[] = {8900, 4500, 500, 600, 500, 600, 500, 1750, 500, 600,
				500, 600, 500, 600, 500, 600, 500, 650, 450, 1750,
				500, 1700, 500, 600, 500, 1750, 500, 1700, 500, 1700,
				500, 1750, 500, 1700, 500, 600, 500, 600, 500, 650,
				500, 1700, 500, 600, 500, 600, 500, 650, 450, 650,
				500, 1700, 500, 1700, 500, 1750, 500, 600, 500, 1700,
				500, 1750, 500, 1700, 500, 1700, 500}; /*--B501 TV data --*/

enum {
    ITEM_ENABLE_IRTX_LED,
    ITEM_DISABLE_IRTX_LED,
    ITEM_PASS,
    ITEM_FAIL,
};

// Manual test
static item_t irtx_led_test_items[] = {
    item(ITEM_ENABLE_IRTX_LED, uistr_info_irtx_led_enable),
    item(ITEM_DISABLE_IRTX_LED, uistr_info_irtx_led_disable),
#ifndef FEATURE_FTM_TOUCH_MODE
    item(ITEM_PASS,   uistr_pass),
    item(ITEM_FAIL,   uistr_fail),
#endif /* #ifndef FEATURE_FTM_TOUCH_MODE */
    item(-1, NULL),
};

// Auto test
static item_t irtx_led_auto_test_items[] = {
    item(-1, NULL),
};

struct irtx_led_stu {    
    char   info[1024];
    text_t title;
    text_t text;
    struct ftm_module *mod;
    struct itemview *iv;
    pthread_t update_thread;
    bool   exit_thread;
    bool   test_done;
    int    test_case;  // 1: enable led, 2: disable led
};

#define mod_to_irtx_led_stu(p)     (struct irtx_led_stu*)((char*)(p) + sizeof(struct ftm_module))

static int test_steps = 0;

#define MAX_TRY_TIMES 1
int irtx_led_test_pre_set(void *priv)
{
	struct irtx_led_stu *irtxledstu = (struct irtx_led_stu *)priv;
	int ret = 0;
	int enable = (irtxledstu->test_case == ITEM_ENABLE_IRTX_LED) ? 1 : 0;

#if 1
	int fd = -1;
	int irtx_set = enable;
	int try_times = MAX_TRY_TIMES;
	int total_time = 0; /* micro-seconds */
	long i,j;
	int buffer_len;

	unsigned int *wave_buffer;
	int int_ptr = 0;
	int bit_ptr = 0;
	char current_level = 1; /* start with high level */
	int carrier_freq = 38008;

	int p_len = sizeof(decode_data) / sizeof(decode_data[0]);

	carrier_freq = 38008;

	LOGD(TAG "%s: Start enable:%d\n", __FUNCTION__, enable); 

	test_steps = 0;
	while (try_times-- > 0) {
		fd = open("/dev/irtx", O_RDWR, 0);

		if (fd >= 0) {
			test_steps = 1;
			sprintf(irtxledstu->info + strlen(irtxledstu->info), "%s %s: %s \n"
			, uistr_info_irtx_led_test, uistr_info_irtx_open_device, uistr_info_pass);

			if( enable ) { /* tv_power_data_out */

				for (i = 0; i < p_len; i++)
					total_time += decode_data[i];
				/* simulate the time spent transmitting by sleeping */
					LOGD("transmit for %d uS at %d Hz\n", total_time, carrier_freq);

					LOGD("data_len:%d, decode_data[]:\n", p_len);

#ifdef debug_data    /* out put log for debug: start */

			for (i=0; i < (p_len >> 3); i++) {
				int ii = (i << 3);
				LOGD("&decode_data[%d]:0x%p, %d, %d, %d, %d, %d, %d, %d, %d\n", ii, (decode_data + ii)
				, decode_data[ii + 0], decode_data[ii + 1], decode_data[ii + 2], decode_data[ii + 3]
				, decode_data[ii + 4], decode_data[ii + 5], decode_data[ii + 6], decode_data[ii + 7]);
			}
			i = (i << 3);
			for (; i < (p_len ); i++) {
				LOGD("0x%x, ", decode_data[i]);
			}
			LOGD("\n");

#endif  /* out put log for debug: end */

				ret = ioctl(fd, IRTX_IOC_SET_CARRIER_FREQ, &carrier_freq);
				if(ret)
					LOGE(TAG "%s: ioctl call fail! err:%d \n", __FUNCTION__, errno);
				else
					LOGE(TAG "%s: ioctl call pass! \n", __FUNCTION__);

				buffer_len = ceil(total_time/(float)32); /* number of integers, one bit for one micro-seconds */
				wave_buffer = malloc(buffer_len * 4);    /* number of bytes */
				LOGD("U32 number=%d\n", buffer_len);

				memset(wave_buffer, 0, buffer_len * 4);
				for (i = 0; i < p_len; i++) {
					for(j=0; j<decode_data[i]; j++) {
						if(current_level)
							*(wave_buffer+int_ptr) |= (1<<bit_ptr);
						else
							*(wave_buffer+int_ptr) &= ~(1<<bit_ptr);
						bit_ptr++;
						if(bit_ptr==32) {
							bit_ptr = 0;
							int_ptr++;
						}
					}
					current_level = !current_level;
				}

				if (write(fd, (char *)wave_buffer, buffer_len * 4))
					ret = 0;
				else
					ret = 1;

			LOGD("converted len:%d, data:\n", buffer_len);

#ifdef debug_data    /* out put log for debug: start */

			for (i=0; i < (buffer_len >> 3); i++) {
				int ii = (i << 3);
				LOGD("&wave_buffer[%d]%p, 0x%x, 0x%x, 0x%x, 0x%x, 0x%x, 0x%x, 0x%x, 0x%x\n", ii, &(wave_buffer[ii])
					, wave_buffer[ii + 0], wave_buffer[ii + 1], wave_buffer[ii + 2], wave_buffer[ii + 3]
					, wave_buffer[ii + 4], wave_buffer[ii + 5], wave_buffer[ii + 6], wave_buffer[ii + 7]);
			}
			i = (i << 3);
			for (; i < buffer_len; i++) {
				LOGD("0x%x, ", wave_buffer[i]);
			}
			LOGD("==\n");

#endif	/* out put log for debug: end */

			} else {   /* for disable using*/
				ret = ioctl(fd, IRTX_IOC_SET_IRTX_LED_OUT, &enable);
			}

			close(fd);

			if (ret) {
				sprintf(irtxledstu->info + strlen(irtxledstu->info), "%s %s: %s \n"
				, uistr_info_irtx_led_test, uistr_info_irtx_call_ioctl, uistr_info_fail);
				LOGE(TAG "%s: write call fail! err:%d, tried times:%d\n", __FUNCTION__, errno, (MAX_TRY_TIMES - try_times));
			} else {
				test_steps = 2;
				LOGE(TAG "%s: write call pass! tried times:%d\n", __FUNCTION__, (MAX_TRY_TIMES - try_times));
				sprintf(irtxledstu->info + strlen(irtxledstu->info), "%s %s: %s \n"
				, uistr_info_irtx_led_test, uistr_info_irtx_call_ioctl, uistr_info_pass);
				break;
			}
		} else { /* open device fail */
			ret = -1;
			sprintf(irtxledstu->info + strlen(irtxledstu->info), "%s %s: %s \n"
			, uistr_info_irtx_led_test, uistr_info_irtx_open_device, uistr_info_fail);
			LOGE(TAG "%s: Can't open /dev/irtx!! fd:%d, err:%d, tried times:%d\n", __FUNCTION__, fd, errno, (MAX_TRY_TIMES - try_times));
		}

		usleep(100000);
	}

	LOGD(TAG "%s: result:%d\n", __FUNCTION__, ret); 
#endif
	return ret;
}

static int aaa;
static void irtx_led_test_update_info(struct irtx_led_stu *irtxledstu, char *info)
{
#if 0
    char *ptr;

    /* preare text view info */
    ptr  = info;

    sprintf(ptr, "%s %s: %s \n", uistr_info_irtx_led_test, uistr_info_irtx_open_device, (test_steps > 0) ? uistr_info_pass:uistr_info_fail);

    if (test_steps > 0)  {
        ptr += sprintf(ptr, "%s %s: %s \n", uistr_info_irtx_led_test, uistr_info_irtx_call_ioctl, (test_steps > 1) ? uistr_info_pass:uistr_info_fail);
    }

    LOGD(TAG "%s: %s\n", __FUNCTION__, info);
#endif
    sprintf(info, "%s %d \n", info, aaa++);

    return;
}


static void *irtx_led_test_update_thread(void *priv)
{
    struct irtx_led_stu *irtxledstu = (struct irtx_led_stu *)priv;

    LOGD(TAG "%s: Start1\n", __FUNCTION__);
    
    while (1) {
        LOGD(TAG "%s: in while, exit_thread:%d\n", __FUNCTION__, (irtxledstu->exit_thread) ? 1 : 0);

        usleep(500000);
        if (irtxledstu->exit_thread) {
            LOGD(TAG "%s: Exit. irtxledstu->exit_thread:%d\n", __FUNCTION__, irtxledstu->exit_thread);
            break;
        }

        if (!irtxledstu->test_done) {
            irtxledstu->test_done = true;
            irtx_led_test_pre_set(priv);
        }
    }
    
    LOGD(TAG "%s: Exit\n", __FUNCTION__);
    pthread_exit(NULL);
    
    return NULL;
}

int irtx_led_entry(struct ftm_param *param, void *priv)
{
    int chosen = -1;
    bool exit = false;
    struct irtx_led_stu *irtxledstu = (struct irtx_led_stu *)priv;
    struct itemview *iv;
    bool auto_pass = false;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    strcpy(irtxledstu->info, "");
    init_text(&irtxledstu->title, param->name, COLOR_YELLOW);
    init_text(&irtxledstu->text, &irtxledstu->info[0], COLOR_YELLOW);

    if (!irtxledstu->iv) {
        iv = ui_new_itemview();
        if (!iv) {
            LOGE(TAG "%s - ui_new_itemview fail! err:%d\n", __FUNCTION__, errno);
            return -1;
        }
        irtxledstu->iv = iv;
    }
    
    iv = irtxledstu->iv;
    iv->set_title(iv, &irtxledstu->title);
    iv->set_text(iv, &irtxledstu->text);

    LOGE(TAG "param->test_type:%d\n", param->test_type);
    if (FTM_MANUAL_ITEM == param->test_type) {
        iv->set_items(iv, irtx_led_test_items, 0);
    } else {
        iv->set_items(iv, irtx_led_auto_test_items, 0);
        iv->start_menu(iv,0);
        iv->redraw(iv);
    }

    LOGE(TAG "%s - do\n", __FUNCTION__);

    irtxledstu->exit_thread = false;
    irtxledstu->test_done = true;
    pthread_create(&irtxledstu->update_thread, NULL, irtx_led_test_update_thread, priv);

    LOGE(TAG "%s - do1\n", __FUNCTION__);
    if (FTM_MANUAL_ITEM == param->test_type) {
#ifdef FEATURE_FTM_TOUCH_MODE
        text_t lbtn;
        text_t cbtn;
        text_t rbtn;
        init_text(&lbtn, uistr_key_fail, COLOR_YELLOW);
        init_text(&cbtn, uistr_key_back, COLOR_YELLOW);
        init_text(&rbtn, uistr_key_pass, COLOR_YELLOW);
        iv->set_btn(iv, &lbtn, &cbtn, &rbtn);
#endif
        do {
            LOGE(TAG "%s - do3\n", __FUNCTION__);

            chosen = iv->run(iv, &exit);
            LOGD(TAG "%s -chosen = %d\n", __FUNCTION__, chosen);

            irtxledstu->mod->test_result = FTM_TEST_FAIL;
            
            switch (chosen) {
                case ITEM_ENABLE_IRTX_LED:
                    // irtx_led_test_pre_set(true);
                    LOGD(TAG "%s: ITEM_ENABLE_IRTX_LED\n", __FUNCTION__);
                    // irtx_led_test_update_info(irtxledstu, irtxledstu->info);
                    // iv->redraw(iv);
                    irtxledstu->test_case = ITEM_ENABLE_IRTX_LED;
                    irtxledstu->test_done = false;
                    break;

                case ITEM_DISABLE_IRTX_LED:
                    // irtx_led_test_pre_set(false);
                    LOGD(TAG "%s: ITEM_DISABLE_IRTX_LED\n", __FUNCTION__);
                    // irtx_led_test_update_info(irtxledstu, irtxledstu->info);
                    // iv->redraw(iv);
                    irtxledstu->test_case = ITEM_DISABLE_IRTX_LED;
                    irtxledstu->test_done = false;
                    break;

#ifdef FEATURE_FTM_TOUCH_MODE
                case C_BTN_DOWN:
                    if(irtxledstu->test_done)
                    {
                        irtxledstu->exit_thread = true;
                        irtxledstu->test_done = true;
                        exit = true;
                    }
                    else
                    {
                        memset(irtxledstu->info, 0, 1024);
                        sprintf(irtxledstu->info, "Not test done !! \n");
                        iv->set_text(iv, &irtxledstu->text);
                        iv->redraw(iv);
                    }
                    break;
#endif

#ifndef FEATURE_FTM_TOUCH_MODE
                case ITEM_PASS:
#else   /* #ifndef FEATURE_FTM_TOUCH_MODE */
                case R_BTN_DOWN:
#endif  /* #ifndef FEATURE_FTM_TOUCH_MODE */
                    irtxledstu->mod->test_result = FTM_TEST_PASS;

#ifndef FEATURE_FTM_TOUCH_MODE
                case ITEM_FAIL:
#else   /* #ifndef FEATURE_FTM_TOUCH_MODE */
                case L_BTN_DOWN:
#endif  /* #ifndef FEATURE_FTM_TOUCH_MODE */
                default:
                    irtxledstu->exit_thread = true;
                    irtxledstu->test_done = true;
                    exit = true;
                    break;
            }

            if (exit) {
                irtxledstu->exit_thread = true;
                LOGD(TAG "%s -irtxledstu->exit_thread is true.\n", __FUNCTION__);
                break;
            }
            usleep(1000000);
        }while (1);
    }else if (FTM_AUTO_ITEM == param->test_type) {
        // Auto test
        char *ptmp = NULL;
        memset(irtxledstu->info, 0, sizeof(irtxledstu->info) / sizeof(*(irtxledstu->info)));
        irtxledstu->test_case = ITEM_ENABLE_IRTX_LED;
        irtxledstu->test_done = false;
        while ((strlen(irtxledstu->info) == 0) || (!strstr(irtxledstu->info, "ioctl"))) {
            LOGD(TAG "%s -Auto test ITEM_ENABLE_IRTX_LED test_steps:%d\n", __FUNCTION__, test_steps);
            usleep(200000);
        }

        LOGD(TAG "%s -Auto test ITEM_ENABLE_IRTX_LED test result:%d\n", __FUNCTION__, test_steps);
        LOGD(TAG "%s -begin redraw\n", __FUNCTION__);
        iv->redraw(iv);
        LOGD(TAG "%s -end redraw\n", __FUNCTION__);

	if (test_steps == 2) {
            irtxledstu->test_case = ITEM_DISABLE_IRTX_LED;
            irtxledstu->test_done = false;
            ptmp = strstr(irtxledstu->info, "ioctl"); // first ioctl
            ptmp += strlen("ioctl");

            while ((!strstr(ptmp, "ioctl"))) {
                LOGD(TAG "%s -Auto test ITEM_DISABLE_IRTX_LED test_steps:%d\n", __FUNCTION__, test_steps);
                usleep(200000);
                ptmp = strstr(irtxledstu->info, "ioctl"); // first ioctl
                ptmp += strlen("ioctl");
            }
            
            LOGD(TAG "%s -Auto test ITEM_DISABLE_IRTX_LED test result:%d\n", __FUNCTION__, test_steps);
            LOGD(TAG "%s -begin redraw\n", __FUNCTION__);
            iv->redraw(iv);
            usleep(1000000);
            LOGD(TAG "%s -end redraw\n", __FUNCTION__);

            if (test_steps == 2) {
                auto_pass == true;
            }
        }

        irtxledstu->exit_thread = true;
        irtxledstu->test_done = true;
    }

    pthread_join(irtxledstu->update_thread, NULL);

    if (FTM_AUTO_ITEM == param->test_type) {
        if (auto_pass) {
            irtxledstu->mod->test_result = FTM_TEST_PASS;
        } else {
            irtxledstu->mod->test_result = FTM_TEST_FAIL;
        }
    }

    LOGD(TAG "%s Exist.\n", __FUNCTION__);
     return 0;
}

int irtx_led_init(void)
{
    int ret = 0;
    int e = 0;
    struct ftm_module *mod;
    struct irtx_led_stu *irtxledstru;

    LOGD(TAG "%s: Start\n", __FUNCTION__);
    e = system("setenforce 0");
    LOGD(TAG "%s: setenforce 0. ret:%d\n", __FUNCTION__, e);

    mod = ftm_alloc(ITEM_IRTX_LED_TEST, sizeof(struct irtx_led_stu));
    if (!mod) {
        LOGE(TAG "%s - ftm_alloc error! err:%d\n", __FUNCTION__, errno);
        return -ENOMEM;
    }

    irtxledstru = mod_to_irtx_led_stu(mod);

    /* init */
    irtxledstru->mod = mod;    

    ret = ftm_register(mod, irtx_led_entry, (void*)irtxledstru);

    if (ret) {
        LOGE(TAG "%s -register irtx_led_entry fail!! ret:%d\n", __FUNCTION__, ret);
    }

    return ret;
}

#endif

