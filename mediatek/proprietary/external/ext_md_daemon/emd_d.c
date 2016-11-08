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

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*****************************************************************************
 *
 * Filename:
 * ---------
 *   emd_d.c
 *
 * Project:
 * --------
 *   ALPS
 *
 * Description:
 * ------------
 *   Dual Talk
 *
 * Author:
 * -------
 *   Chao Song
 *
 ****************************************************************************/

#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <unistd.h>
#include <cutils/properties.h>
#include <android/log.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <errno.h>
#include <pthread.h>
#include <hardware_legacy/power.h>
#include <hardware/ccci_intf.h>
#ifdef EMD_HAVE_AEE_FEATURE
#include "../aee/binary/inc/aee.h"
#endif // EMD_HAVE_AEE_FEATURE

#define EMD_UART_CFIFO_DEV   "/dev/emd_cfifo0"
//#define EMD_UART_DEV         "/dev/ttyMT1" 
//#define EMD_MONITOR_DEV      "/dev/emd_ctl0"

#define MUXD_NAME_FOR_EMD "gsm0710muxdmd2"
#define RILD_NAME_FOR_EMD "mtkrildmd2"
#define PPPD_NAME_FOR_EMD "pppd_gprs"

#define EMD_WAKE_LOCK_MAIN "emd_d"
#define EMD_MAIN_WAKE_LOCK() acquire_wake_lock(PARTIAL_WAKE_LOCK, EMD_WAKE_LOCK_MAIN)
#define EMD_MAIN_WAKE_UNLOCK() release_wake_lock(EMD_WAKE_LOCK_MAIN)

#define EMD_WAKE_LOCK_READER "emd_d_reader"
#define EMD_READER_WAKE_LOCK() acquire_wake_lock(PARTIAL_WAKE_LOCK, EMD_WAKE_LOCK_READER)
#define EMD_READER_WAKE_UNLOCK() release_wake_lock(EMD_WAKE_LOCK_READER)

#define EMD_WAKE_LOCK_WRITER "emd_d_writer"
#define EMD_WRITER_WAKE_LOCK() acquire_wake_lock(PARTIAL_WAKE_LOCK, EMD_WAKE_LOCK_WRITER)
#define EMD_WRITER_WAKE_UNLOCK() release_wake_lock(EMD_WAKE_LOCK_WRITER)


enum{
    SRV_INIT=0,
    SRV_STARTING,
    SRV_STARTED,
    SRV_STOPPING,
    SRV_STOPPED,    
    SRV_ERROR,
};
typedef struct{
    char* name;
    int type; //0 need start, 1 no need start
    int status;//0 stoped, 1 starting, 2 run, 3 stopping,     
}emd_service_t;
static emd_service_t srv_mgr[]={
    {MUXD_NAME_FOR_EMD, 0, SRV_INIT},
    {RILD_NAME_FOR_EMD, 1, SRV_INIT},
    {PPPD_NAME_FOR_EMD, 1, SRV_INIT},    
};

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "emd_d",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "emd_d",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "emd_d",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "emd_d",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "emd_d",__VA_ARGS__)

/* MD Message, this is for user space deamon use */
enum {
    EMD_MSG_READY = 0xF0A50000,
    EMD_MSG_REQUEST_RST,
    EMD_MSG_WAIT_DONE,
    EMD_MSG_ENTER_FLIGHT_MODE,
    EMD_MSG_LEAVE_FLIGHT_MODE,
};

/* MD Status, this is for user space deamon use */
enum {
    EMD_STA_RST = 0,
    EMD_STA_RDY = 1,
    EMD_STA_WAIT = 2,
    EMD_STA_FLIGHT_MODE = 3,
};

static int curr_md_sta = EMD_STA_RST;
static int emd_monitor_fd=-1;
static int emd_uart_fd=-1;
static int emd_uart_cfifo=-1;
static int emd_dump_eanble = 1;

static int is_meta_mode(void)
{
#define BOOT_MODE_FILE            "/sys/class/BOOT/BOOT/boot/boot_mode" // controlled by mtxxxx_boot.c
#define META_MODE                '1'
    int fd, ret;
    size_t s;
    volatile char data[20];
    fd = open(BOOT_MODE_FILE, O_RDONLY);
    if (fd < 0) {
        LOGD("fail to open %s: ", BOOT_MODE_FILE);
        perror("");
        return 0;
    }
    s = read(fd, (void *)data, sizeof(char) * 3);
    if (s <= 0) {
        LOGD("fail to read %s", BOOT_MODE_FILE);
        perror("");
        ret = 0;
    } else {
        if (data[0] == META_MODE) {
            ret = 1;
        } else {
            ret = 0;
        }
    }
    close(fd);
    return ret;
}

static int emd_open_uart(char* dev_name)
{
    int fd,fdflags;
    speed_t speed;
    int status;
    int cmux_port_speed = 5;
    speed_t baud_bits[] = {
        0, B9600, B19200, B38400, B57600, B115200, B230400, B460800, B921600, B1500000
    };
    struct termios uart_cfg_opt;
    fd = open(dev_name, O_RDWR | O_NOCTTY | O_NONBLOCK);
    fdflags = fcntl(fd, F_GETFL);
    fcntl(fd, F_SETFL, fdflags & ~O_NONBLOCK);
    tcgetattr(fd, &uart_cfg_opt);
    tcflush(fd, TCIOFLUSH);

    /*set standard buadrate setting*/
    speed = baud_bits[cmux_port_speed];
    cfsetospeed(&uart_cfg_opt, speed);
    cfsetispeed(&uart_cfg_opt, speed);

    /* Raw data */
    uart_cfg_opt.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
    uart_cfg_opt.c_iflag &= ~(INLCR | IGNCR | ICRNL | IXON | IXOFF);
    uart_cfg_opt.c_oflag &=~(INLCR|IGNCR|ICRNL);
    uart_cfg_opt.c_oflag &=~(ONLCR|OCRNL);

    /* Non flow control */
    uart_cfg_opt.c_cflag |= CRTSCTS;                /*clear flags for hardware flow control*/
    uart_cfg_opt.c_iflag &= ~(IXON | IXOFF | IXANY); /*clear flags for software flow control*/        

    // Set time out
    uart_cfg_opt.c_cc[VMIN] = 1;
    uart_cfg_opt.c_cc[VTIME] = 0;

    /* Apply new settings */
    tcsetattr(fd, TCSANOW, &uart_cfg_opt);

    status = TIOCM_DTR | TIOCM_RTS;
    ioctl(fd, TIOCMBIS, &status);
    return fd;

}

static void emd_sig_alrm(int signo)
{
	LOGD("stop dump raw data\n");
	emd_dump_eanble = 0;
}

static void emd_dump(char* str,void* msg_buf, int len)
{
#if 0
    unsigned int *int_p = (unsigned int *)msg_buf;
    LOGD("emd_dump:%s, len:%d, data[0]:0x%x, data[1]:0x%x\n", str, len, *int_p, *(int_p + 1));
#else
    unsigned char *char_ptr = (unsigned char *)msg_buf;
	unsigned int *int_p = (unsigned int *)msg_buf;
    int _16_fix_num = len/16;
    int tail_num = len%16;
    char *buf;
    int i,j;

	if(!emd_dump_eanble)
		goto short_log;
	buf = (char *)malloc(len+1);
	if(!buf)
		goto short_log;
	
    for(i=0;i<len;i++)
    {
        if((32<=char_ptr[i] && char_ptr[i]<=126)
            ||char_ptr[i]==0x0A) // only show readable char and LF
        {
            buf[i]=char_ptr[i];
        }
        else
        {
            buf[i]='?';
        }
    }
    buf[i]='\0';
    LOGD("emd_dump:%s len=%d\n", str,len);
    LOGD("%s\n",buf);
	free(buf);
	return;

short_log:
	LOGD("emd_dump:%s, len:%d, data[0]:0x%x, data[1]:0x%x\n", str, len, *int_p, *(int_p + 1));
	return;
#endif
}
//#define EMD_UT_TEST

#ifdef EMD_UT_TEST
#define MUXD_CFIFO_DEV    "/dev/emd_cfifo1"
#define MUXD_MONITOR_DEV  "/dev/emd_ctl1"
static int muxd_cfifo_fd=-1;
static int muxd_monitor_fd=-1;
static int muxd_quit = 0;
#define MUXD_BUF_SIZE 1024
static char muxd_read_buf[MUXD_BUF_SIZE];
static pthread_t muxd_tester;
void *muxd_tester_func(void* argv)
{
    int size=0,ret =0;
    int arg;
    muxd_monitor_fd=  open(MUXD_MONITOR_DEV, O_RDWR);
    if (muxd_monitor_fd < 0) {
        LOGD("muxd fail to open %s: ", MUXD_MONITOR_DEV);
        goto __ERR;

    }
    muxd_cfifo_fd=open(MUXD_CFIFO_DEV, O_RDWR);
    if (muxd_cfifo_fd < 0) {
        LOGD("muxd fail to open %s: \n", MUXD_CFIFO_DEV);
        goto __ERR;
    }

    while(!muxd_quit)
    {
        size = read(muxd_cfifo_fd, (void *)muxd_read_buf, MUXD_BUF_SIZE);
        if (size<=0) {
            LOGD("muxd read error ret=%ld,erro=%d.\n",size,errno); 
            continue;
        } 
        emd_dump("muxd_tester_func",muxd_read_buf,size);
        /*size = write(muxd_cfifo_fd, (void *)muxd_read_buf, size);
        if (size<=0) {
            LOGD("muxd write error ret=%ld.\n",size); 
            continue;
        }        
        switch(muxd_read_buf[0])
        {
            
            case '1':
                LOGD("CCCI_IOC_MD_RESET:\n");
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_MD_RESET, NULL);
                if (ret < 0) {
                    LOGD("CCCI_IOC_MD_RESET failed,ret=%d!\n",ret);
                }
                break;
            case'2':
                LOGD("CCCI_IOC_ENTER_DEEP_FLIGHT:\n");
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_ENTER_DEEP_FLIGHT, NULL);
                if (ret < 0) {
                    LOGD("CCCI_IOC_ENTER_DEEP_FLIGHT failed,ret=%d!\n",ret);
                }
                break;
            case'3':
                LOGD("CCCI_IOC_LEAVE_DEEP_FLIGHT:\n");
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_LEAVE_DEEP_FLIGHT, NULL);
                if (ret < 0) {
                    LOGD("CCCI_IOC_LEAVE_DEEP_FLIGHT failed,ret=%d!\n",ret);
                }
                break;
            case'4':
                LOGD("CCCI_IOC_GET_MD_STATE:\n");
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_GET_MD_STATE, &arg);
                if (ret < 0) {
                    LOGD("CCCI_IOC_GET_MD_STATE failed,ret=%d!\n",ret);
                }
                LOGD("CCCI_IOC_GET_MD_STATE,state=%d,ret=%d!\n",arg,ret);
                break;
            case'5':
                arg= 1<<16;
                arg|=1;
                LOGD("CCCI_IOC_SIM_SWITCH:arg=0x%x\n",arg);
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_SIM_SWITCH, &arg);
                if (ret < 0) {
                    LOGD("CCCI_IOC_SIM_SWITCH failed,ret=%d!\n",ret);
                }
                break;
            case'6':
                arg= 1<<16;
                arg|=2;
                LOGD("CCCI_IOC_SIM_SWITCH:arg=0x%x\n",arg);
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_SIM_SWITCH, &arg);
                if (ret < 0) {
                    LOGD("CCCI_IOC_SIM_SWITCH failed,ret=%d!\n",ret);
                }
                break;
            case'7':
                arg= 1<<16;
                arg|=3;
                LOGD("CCCI_IOC_SIM_SWITCH:arg=0x%x\n",arg);
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_SIM_SWITCH, &arg);
                if (ret < 0) {
                    LOGD("CCCI_IOC_SIM_SWITCH failed,ret=%d!\n",ret);
                }
                break;  
            case'8':
                arg= 1<<16;
                arg|=4;
                LOGD("CCCI_IOC_SIM_SWITCH:arg=0x%x\n",arg);
                ret = ioctl(muxd_monitor_fd, CCCI_IOC_SIM_SWITCH, &arg);
                if (ret < 0) {
                    LOGD("CCCI_IOC_SIM_SWITCH failed,ret=%d!\n",ret);
                }
                break;   
            default:
                LOGD("unknow failed,muxd_read_buf[0]=%d!\n",muxd_read_buf[0]);
                break;
        }*/
    }
__ERR:    
    if(muxd_monitor_fd!=-1)
    {
        close(muxd_monitor_fd);
        muxd_monitor_fd=-1;
    }
    if(muxd_cfifo_fd!=-1)
    {
        close(muxd_cfifo_fd);
        muxd_cfifo_fd=-1;
    }
    muxd_quit=0;
    LOGD("muxd_tester_func leave!\n");
    return NULL;
}
#endif


#define BUF_SIZE (4096)
#define RETRY_TIMES 100
static char read_buf[BUF_SIZE];
static char write_buf[BUF_SIZE];
static pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
void* emd_uart_reader(void* arg)
{
    ssize_t size;
    ssize_t wrote_size =0;
    int ret=0,retry=0;
    while(1)
    {
        pthread_mutex_lock(&mtx);
        while(emd_uart_cfifo==-1 || emd_uart_fd ==-1)
        {
            LOGD("emd_uart_reader: wait ready,emd_uart_cfifo=%d,emd_uart_fd=%d\n",emd_uart_cfifo,emd_uart_fd);
            pthread_cond_wait(&cond, &mtx);
        }
        pthread_cond_signal(&cond);
        pthread_mutex_unlock(&mtx);
        size = read(emd_uart_fd, (void *)read_buf, BUF_SIZE);
        if (size<=0) {
            LOGD("read error ret=%d,errno=%d.\n",size,errno); 
            continue;
        }
        EMD_READER_WAKE_LOCK();
        emd_dump("EMD_uart_reader",read_buf,size);
        //write into uart
        ret=0;
        wrote_size=0;
        retry=0;
        do{
            ret = write(emd_uart_cfifo, (void *)read_buf+wrote_size, size-wrote_size);
            if(ret>0)
            {
                wrote_size+=ret;
                retry=0;
            }
            else
            {
                retry++;
                if(retry%RETRY_TIMES==0)
                {
                    LOGD("Write data into cfifo retry %d, fail %d\n",RETRY_TIMES, errno);
                    retry=1;
					break;
                }
            }
        }while(wrote_size!=size);
        LOGD("Write cfifo0 %d\n",size);
        EMD_READER_WAKE_UNLOCK();
    }
    return NULL;
}
void* emd_uart_writer(void* arg)
{
    ssize_t size;
    ssize_t wrote_size =0;
    int ret=0,retry=0;
    while(1)
    {
        pthread_mutex_lock(&mtx);
        while(emd_uart_cfifo==-1 || emd_uart_fd ==-1)
        {
            LOGD("emd_uart_writer:wait ready,emd_uart_cfifo=%d,emd_uart_fd=%d\n",emd_uart_cfifo,emd_uart_fd);
            pthread_cond_wait(&cond, &mtx);
        }
        pthread_cond_signal(&cond);
        pthread_mutex_unlock(&mtx);
        size = read(emd_uart_cfifo, (void *)write_buf, BUF_SIZE);
        if (size<=0) {
            LOGD("read error ret=%d,errno=%d.\n",size,errno); 
            continue;
        }
        EMD_WRITER_WAKE_LOCK();
        emd_dump("EMD_uart_writer",write_buf,size);
        //write into uart
        ret=0;  
        wrote_size=0;
        retry=0;
        do{
            ret = write(emd_uart_fd, (void *)write_buf+wrote_size, size-wrote_size);
            if(ret>0)
            {
                wrote_size+=ret;
                retry=0;
            }
            else
            {
                retry++;
                if(retry%(RETRY_TIMES*100)==0)
                {
                    LOGD("Write data into uart retry %d, fail\n",(RETRY_TIMES*100));
                    retry=1;
                }
            }
        }while(wrote_size!=size);
        LOGD("Write uart %d\n",size);
        EMD_WRITER_WAKE_UNLOCK();
    }

    return NULL;
}
int emd_open_connection(char* uart_dev_name)
{
    int ret=0;
    pthread_mutex_lock(&mtx);
    emd_uart_fd=emd_open_uart(uart_dev_name);
    if (emd_uart_fd < 0) {
        LOGD("fail to open uart %s: \n", uart_dev_name);
        ret = -1;
        goto __ERR;
    }    
    emd_uart_cfifo=open(EMD_UART_CFIFO_DEV, O_RDWR);
    if (emd_uart_cfifo < 0) {
        LOGD("fail to open cfifo %s: \n", EMD_UART_CFIFO_DEV);
        ret = -2;
        close(emd_uart_fd);
        emd_uart_fd = -1;
        goto __ERR;
    }
    LOGD("open connection:emd_uart_fd=%d,emd_uart_cfifo=%d \n", emd_uart_fd,emd_uart_cfifo);    
__ERR:
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mtx);
    return ret;
}
int emd_close_connection(void)
{
    int fd;
    LOGD("emd_close_connection enter\n");

    pthread_mutex_lock(&mtx);
    if(emd_uart_fd!=-1)
    {
        LOGD("emd_close_connection emd_uart_fd\n");
        close(emd_uart_fd);
        emd_uart_fd=-1;
    }
    if(emd_uart_cfifo!=-1)
    {
        fd = emd_uart_cfifo;
        emd_uart_cfifo=-1;
        LOGD("emd_close_connection emd_uart_cfifo\n");
        close(fd);
    }
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mtx);
    LOGD("emd_close_connection leave\n");
    return 0;
}
void emd_change_connection_baudrate(speed_t speed)
{
    struct termios uart_cfg_opt;

	if (emd_uart_fd < 0) {
		LOGD("fail to open uart for changing baudrate\n");
		return;
	}
	
    tcgetattr(emd_uart_fd, &uart_cfg_opt);
    cfsetospeed(&uart_cfg_opt, speed);
    cfsetispeed(&uart_cfg_opt, speed);
    tcsetattr(emd_uart_fd, TCSANOW, &uart_cfg_opt);

	LOGD("emd_change_connection_baudrate %d\n", speed);
}

static int wait_property_ready(int wait_status, int waitmsec)
{
#define PROPERTY_WAIT_TIME 60
    int idx;
    int ret;
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    char *desired_value;
    int wait_done_cnt=0;
    char key_name[PROPERTY_KEY_MAX];
    int retry_times = (waitmsec+PROPERTY_WAIT_TIME-1)/PROPERTY_WAIT_TIME;
    if(wait_status == SRV_STOPPED)
    {
        desired_value="stopped";
    }
    else if(wait_status == SRV_STARTED)
    {
        desired_value="running";
    }else{
        return -1;
    }
    
    while(retry_times>0)
    {
        wait_done_cnt=0;
        for(idx=0;idx < sizeof(srv_mgr)/sizeof(emd_service_t);idx++)
        {
            if(srv_mgr[idx].status!=wait_status-1)
            {
                if(++wait_done_cnt>=sizeof(srv_mgr)/sizeof(emd_service_t))
                {
                    LOGD("wait all service %s done\n",desired_value);
                    return 0;
                }else
                    continue;
            }
            snprintf(key_name, PROPERTY_KEY_MAX, "%s%s\0", "init.svc.", srv_mgr[idx].name);
            ret = property_get(key_name, value, "");
            if (ret > 0) 
            {
                if (desired_value == NULL || strcmp(value, desired_value) == 0) 
                {
                    LOGD("wait_for_property() ready for name:%s\n",key_name);
                    srv_mgr[idx].status=wait_status;
                }
            }
            else if (ret == 0) 
            {
                LOGD("wait_for_property() no property of :%s\n", key_name);
                srv_mgr[idx].status=SRV_ERROR;
            } 
            else 
            { 
                LOGD("wait_for_property() error returned:%d, errno:%d\n", ret, errno);
                srv_mgr[idx].status=SRV_ERROR;
            }
        }
        if(--retry_times) usleep(PROPERTY_WAIT_TIME * 1000);       
    }
    return -2;
}
static void emd_stop_services(void)
{   int ret=0;
    int idx=0;
#ifdef EMD_UT_TEST    
    muxd_quit=1;
    while(muxd_quit)
    {        
        LOGD("muxd stopping\n");
        usleep(2000*1000);
    }
    LOGD("muxd stopped\n");
#endif
    for(idx=sizeof(srv_mgr)/sizeof(emd_service_t)-1;idx>=0;idx--)
    {
         property_set("ctl.stop", srv_mgr[idx].name);
         srv_mgr[idx].status = SRV_STOPPING;
    }
    ret = wait_property_ready(SRV_STOPPED,300);
    LOGD("emd_stop_services,ret=%d\n",ret);
}

static void emd_start_services(void)
{
    int idx=0,ret;
    
#ifdef EMD_UT_TEST
    pthread_create(&muxd_tester, NULL, muxd_tester_func, NULL);
#endif
    for(idx=0;idx<sizeof(srv_mgr)/sizeof(emd_service_t);idx++)
    {
        if(srv_mgr[idx].type==1)
            continue;
        else
        {
            property_set("ctl.start", srv_mgr[idx].name);
            srv_mgr[idx].status =SRV_STARTING;
        }
    }
    ret=wait_property_ready(SRV_STARTED,300);
    LOGD("emd_start_services,ret=%d\n",ret);
	alarm(15); // disable data dump after 15 seconds
}

static void emd_ready_state_handler(int message)
{
    int ret;
    switch(message)
    {
    case EMD_MSG_ENTER_FLIGHT_MODE:
        LOGD("EMD_MSG_ENTER_FLIGHT_MODE\n");
        curr_md_sta = EMD_STA_FLIGHT_MODE; 
        break;
    default:
        LOGD("Default @ready, msg=%08x\n", message);
        break;
    }
}

static void emd_reset_state_handler(int message)
{
    switch(message)
    {
    case EMD_MSG_READY:
        LOGD("Got md ready mesage @reset\n");
        curr_md_sta = EMD_STA_RDY;
        emd_start_services();
        break;
    default:
        LOGD(" Default @ reset, msg=%08x\n", message);
        break;
    }
}

static void emd_wait_state_handler(int message)
{
    int ret=0;
    int boot_mode=is_meta_mode();
    switch(message)
    {
    case EMD_MSG_WAIT_DONE:
        LOGD("Got waitdone mesage @wait\n");
        curr_md_sta = EMD_STA_RST;
		emd_dump_eanble = 1;
        ret = ioctl(emd_monitor_fd, CCCI_IOC_DO_START_MD, &boot_mode);
        if (ret < 0) {
            LOGD("DO_START_MD failed!\n");
            return ret;
        }
        break;
    default:
        LOGD(" Default @ wait, msg=%08x\n", message);
        break;
    }
}


static void emd_flight_state_handler(int message)
{
    switch(message)
    {
        case EMD_MSG_LEAVE_FLIGHT_MODE:
            curr_md_sta = EMD_STA_WAIT;
            LOGD("leave flight mode at flight mode state\n");
            if (!is_meta_mode()) 
            {
                emd_stop_services();
            }
            break;
        default:
            LOGD("Ignore msg:%08x at flight_state_handler\n", message);
            break;
    }
}
static int check_and_wait_ccci_ready(void)
{
    int fd,retry =100;
    do{
    	fd = open("/sys/kernel/ccci/version", O_RDONLY);
        if(fd > 0)
        {
            char * name;
            close(fd);
            LOGD("CCCI verison is %d\n",ccci_get_version());
            break;
        }
        usleep(100*1000);
        retry--;
    }while(retry>0);
    if(retry==0)
    {
        LOGD("CCCI verison file doesnot exist!\n");
        return 0;
    }
    return 1;
}


#define NATIVE_DOWNLOADER "/system/bin/downloader"
#define PROP_EMD_DL_PROGRESS "persist.sys.extmddlprogress"
char dl_desired_value[] = "100_0000";    // native down over
char st_md_desired_value[] = "110_0000"; // modem boot started
static int emd_check_and_download_md_img(void)
{
#define WAIT_DL_FINISHED_TIME 2*1000*1000
#define CHECK_TIMES           60
    int pid,ret;
    char* native_downloader_params[]={"-p",PROP_EMD_DL_PROGRESS,NULL};
    char err_msg[100];
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    int has_read;
    int retry;
    struct stat buf;

    //check 
    ret = property_get(PROP_EMD_DL_PROGRESS, value, "none");    
    if (ret > 0) {
        if (strcmp(value, st_md_desired_value) == 0) {
            LOGD("No need download ext md image,emd_dl_progress:%s\n", value);
            return 0;
        }else{
            LOGD("Need download ext md image, emd_dl_progress:%s \n", value);
        }
    } else {  /* property_get return negative, it seems impossible */
        LOGD("Need download ext md image,ret=%d!\n",ret);
    }    
    //start to downlaod
    pid=fork();
    if(pid<0)
    {
        LOGD("Error(%d) occured on forking\n",errno);
#ifdef __EMD_HAVE_AEE_FEATURE
        snprintf(err_msg,sizeof(err_msg),"ext_mdinit fork error(%d)\0",errno);
        aee_system_exception("ExtMD", "ext_mdinit", 0,err_msg);
#endif // EMD_HAVE_AEE_FEATURE            
        return -1;
    }
    else if(pid==0)
    {
        char* params[]={"-p",PROP_EMD_DL_PROGRESS,NULL};
        LOGD("Run child[%d]\n",getpid());
        ret = property_get(PROP_EMD_DL_PROGRESS, value, "none");    
        if (ret > 0) {
            if (strcmp(value, dl_desired_value) == 0) {
                LOGD("No need download ext md image,emd_dl_progress:%s, pid:%d\n", value, getpid());
                exit(0);
            }else{
                LOGD("Need download ext md image, emd_dl_progress:%s,  pid:%d\n", value, getpid());
            }
        } else {  /* property_get return negative, it seems impossible */
            LOGD("Need download ext md image,ret=%d, pid:%d!\n",ret, getpid());
        }

        if(execv(NATIVE_DOWNLOADER,params)<0)
        {
            LOGD("execv error1[%d]\n",errno); 
            exit(0xF4);
        }
        else
        {
            LOGD("execv error2[%d]\n",errno); 
            exit(0);
        }
    }
    else
    {
        int status=0;
        ret = ioctl(emd_monitor_fd, CCCI_IOC_ENTER_MD_DL_MODE, NULL);
        if (ret < 0) {
            LOGD("[ERR]enter dl mode failed,so exit!\n");
#ifdef __EMD_HAVE_AEE_FEATURE
            snprintf(err_msg,sizeof(err_msg),"ext_mdinit power on md fail(0x%x)\0",ret);
            aee_system_exception("ExtMD", "external.ext_mdinit", 0,err_msg);
#endif // EMD_HAVE_AEE_FEATURE
            LOGD("[ERR]enter dl mode(0x%x)\n", ret);
            return ret;
        }
        retry =CHECK_TIMES;
        while((ret=waitpid(pid,&status,WNOHANG)) <= 0)
        {
            retry--;
            usleep(WAIT_DL_FINISHED_TIME);
            ret = property_get(PROP_EMD_DL_PROGRESS, value, "none"); 
            if(retry==0)
            {                
                if((ret=kill(pid,SIGKILL))==0)
                {
                    LOGD("[ERR]wait dl timeout, killed child %d, dl=%s\n",value);
                    return -2;
                }else{
                    LOGD("[ERR]waitchid %d exit, dl=%s\n",pid,value);
                    waitpid(pid,&status,0);
                    break;
                }
            }
        }

        if(WIFEXITED(status))
        {
            if(WEXITSTATUS(status)==0xF4)
            {
                #ifdef __EMD_HAVE_AEE_FEATURE
                snprintf(err_msg,sizeof(err_msg),"ext_mdinit execv error(0x%x)\0",WEXITSTATUS(status));
                aee_system_exception("ExtMD", "external.ext_mdinit", 0,err_msg);
                #endif // EMD_HAVE_AEE_FEATURE
                LOGD("ext_mdinit execv error(0x%x)\n", WEXITSTATUS(status));
                return -3;
            }
            else if(WEXITSTATUS(status)==0x0)
            {
                ret = property_get(PROP_EMD_DL_PROGRESS, value, "none"); 
                LOGD("Dowload status=%s.\n",value);
            }
            else
            {
                #ifdef __EMD_HAVE_AEE_FEATURE
                snprintf(err_msg,sizeof(err_msg),"Native downloader error(0x%x)\0",WEXITSTATUS(status));
                aee_system_exception("ExtMD", "Native downloader", 0,err_msg);
                #endif // EMD_HAVE_AEE_FEATUR
                LOGD("Native downloader error(0x%x)\n", WEXITSTATUS(status));
                return -4;
            }
        }
        else
        {
            LOGD("open EMD_VERSION_FILE fail: %d\n", errno);
            return -5;
        }
    }

    return 0;
}

static int common_msg_handler(int msg)
{
    int ret = 1;
    switch (msg)
    {
        case EMD_MSG_REQUEST_RST:
            LOGD("Got reset request mesage @ready\n");
            curr_md_sta = EMD_STA_WAIT;
            emd_stop_services();
            break;
        default:
            ret = 0;
            break;
    }
    return ret;
}

int main(int argc, char **argv)
{
    int ret, fd,pid, message;
    ssize_t size;
    pthread_t uart_reader_id,uart_writer_id;
    char* emd_uart_dev=NULL;
    char* emd_monitor_dev=NULL;
    char* native_downloader=NULL;
    int sim_mode=0;
    int boot_mode=is_meta_mode();
	int dl_retry = 5;

#ifdef MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
#endif

    if(argc!=2)
    {
            LOGD("[ERR]wrong parameter(%d),so exit\n",argc);
            return -1;
    }
    if(argv[1])
    {
        emd_uart_dev=argv[1];
        if(emd_uart_dev==NULL || strlen(emd_uart_dev)==0)
        {
            LOGD("[ERR]wrong emd_monitor_dev,so exit!\n");
            return -1;
        }
    }
	if(signal(SIGALRM, emd_sig_alrm) == SIG_ERR) {
		LOGE("[ERR]signal(SIGALRM) fail: %d\n", errno);
	}
    emd_monitor_dev=ccci_get_node_name(USR_CCCI_CTRL,MD_SYS5);
    if (emd_monitor_dev ==NULL) {
        LOGD("[ERR]emd_monitor_dev is NULL\n");
        return -1;
    }    
    emd_monitor_fd = open(emd_monitor_dev, O_RDWR);
    if (emd_monitor_fd < 0) {
        LOGD("[ERR]fail to open %s: \n", emd_monitor_dev);
        return -1;
    }
	
    if(check_and_wait_ccci_ready())
    {
        LOGD("%s ver:0.0",argv[0]);
    }else{
        LOGD("ccci version file not ready!");
        return -1;
    }
    pthread_create(&uart_reader_id, NULL, emd_uart_reader, NULL);
    pthread_create(&uart_writer_id, NULL, emd_uart_writer, NULL);
    ret=emd_open_connection(emd_uart_dev);
    if(ret<0)
    {
        LOGD("[ERR]emd_open_connection failed,so exit!\n");
        return -1;
    }

#ifdef MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
DL_RETRY:
    LOGD("[NATIVE_DOWNLOAD_SUPPORT] Defined! retry=%d\n", dl_retry);
    if((ret=emd_check_and_download_md_img())!=0)
    {
        LOGD("emd_check_and_download_md_img,error=%d\n",ret);
		if(dl_retry--<=1) {
			#ifdef EMD_HAVE_AEE_FEATURE
			char err_msg[100];
		    snprintf(err_msg,sizeof(err_msg),"Native download error %d, retried %d", ret, dl_retry);
		    aee_system_exception("ExtMD", "ext_mdinit", 0, err_msg);
			#endif // EMD_HAVE_AEE_FEATURE         
        	return -1;
		}
    }
	
    ret = property_get(PROP_EMD_DL_PROGRESS, prop_value, "none");	
    if (ret > 0) {
        if (strcmp(st_md_desired_value, prop_value)!=0 && strcmp(dl_desired_value, prop_value)!=0) {
            ret = property_set(PROP_EMD_DL_PROGRESS, "");
            LOGD("Reset property:%s(%s), ret:%d. try again\n", PROP_EMD_DL_PROGRESS, prop_value, ret);
			goto DL_RETRY;
        }
    } else {  /* property_get return negative, it seems impossible */
        LOGD("Can not get property:%s, ret:%d. try again!\n", PROP_EMD_DL_PROGRESS, ret);
		goto DL_RETRY;
    }
#else // MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
    LOGD("[NATIVE_DOWNLOAD_SUPPORT] not Defined!\n");
#endif // MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT

	emd_change_connection_baudrate(B1500000);
	emd_dump_eanble = 1;
    ret = ioctl(emd_monitor_fd, CCCI_IOC_POWER_ON_MD, &boot_mode);
    if (ret < 0) {
        LOGD("[ERR]power on modem failed,so exit!\n");
        return ret;
    }

#ifdef MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
    ret = property_get(PROP_EMD_DL_PROGRESS, prop_value, "none");	
    if (ret > 0) {
         LOGD("Download Over! prop_name:%s, prop_value:%s\n", PROP_EMD_DL_PROGRESS, prop_value);
        if (strcmp(dl_desired_value, prop_value) == 0) {
            LOGD("Download Succeeded! change prop:%s to %s\n", PROP_EMD_DL_PROGRESS, st_md_desired_value);
            ret = property_set(PROP_EMD_DL_PROGRESS, st_md_desired_value);
            LOGD("Change prop:%s to %s, ret:%d\n", PROP_EMD_DL_PROGRESS, st_md_desired_value, ret);
        }else{
            LOGD("Need Change prop:%s, ret:%d \n", PROP_EMD_DL_PROGRESS, ret);
        }
    } else {  /* property_get return negative, it seems impossible */
        LOGD("Can not get property:%s, ret:%d. maybe try again!\n", PROP_EMD_DL_PROGRESS, ret);
    }
#endif  // MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT

    do {
        size = read(emd_monitor_fd, (void *)&message, sizeof(int));
        if (size<=0) {
            LOGD("read error ret=%ld,errno=%d.\n",size,errno); 
            continue;
        } else if (size!= sizeof(int)) {
            LOGD("read ext md message with unexpected size=%d\n",size);
            continue;
        } 
        EMD_MAIN_WAKE_LOCK();

        LOGD("message = 0x%08X\n", message);
        if (common_msg_handler(message)) {
            continue;
        }
        switch(curr_md_sta)
        {
            case EMD_STA_RDY:
                emd_ready_state_handler(message);
                break;
            case EMD_STA_WAIT:
                emd_wait_state_handler(message);
                break;
            case EMD_STA_RST:
                emd_reset_state_handler(message);
                break;
            case EMD_STA_FLIGHT_MODE:
                emd_flight_state_handler(message);
                break;
            default:
                LOGD("Invalid state, should not enter here!!\n");
                break;
        }
        EMD_MAIN_WAKE_UNLOCK();
    } while (1);
    EMD_MAIN_WAKE_UNLOCK();
    return 0;
}

