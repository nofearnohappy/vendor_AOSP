/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

#define LOG_TAG "EMCPUSTRESS"
#define MTK_LOG_ENABLE 1
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <cutils/log.h>
#include <chip_support.h>
#include <semaphore.h>
#include "ModuleCpuStress.h"
#include "RPCClient.h"

#ifndef SCHED_IDLE
#define SCHED_IDLE 5
#endif

static force_core_test_files_t cpu_force_core_test_files[] = {
    {FILE_CPU0_SCAL, FILE_CPU0_ONLINE},
    {FILE_CPU1_SCAL, FILE_CPU1_ONLINE},
    {FILE_CPU2_SCAL, FILE_CPU2_ONLINE},
    {FILE_CPU3_SCAL, FILE_CPU3_ONLINE},
    {FILE_CPU4_SCAL, FILE_CPU4_ONLINE},
    {FILE_CPU5_SCAL, FILE_CPU5_ONLINE},
    {FILE_CPU6_SCAL, FILE_CPU6_ONLINE},
    {FILE_CPU7_SCAL, FILE_CPU7_ONLINE},
};

static char* backup_buffers[] = {backup_first, backup_second, 
    backup_third, backup_fourth,
    backup_5, backup_6, 
    backup_7, backup_8}; 


static int read_int_val(RPCClient *msg_client) {
    int type, len;
    type = msg_client->ReadInt();
    if (type != PARAM_TYPE_INT) {
        ALOGD("FAIL TO read_int_val: INVALID TYPE");
        return -1;
    }
    len = msg_client->ReadInt();
    return msg_client->ReadInt();
}

static void echo(const char* value, const char* file) {
    char command[CPUTEST_RESULT_SIZE] = { 0 };
    snprintf(command, CPUTEST_RESULT_SIZE, "echo %s > %s", value, file);
    ALOGD("command: %s", command);
    system(command);
}

static void echo(char buf[CPUTEST_RESULT_SIZE>>1], const char* file) {
    char command[CPUTEST_RESULT_SIZE] = { 0 };
    strcpy(command, "echo ");
    strcat(command, buf);
    strcat(command, " > ");
    strcat(command, file);
    ALOGD("command: %s", command);
    system(command);
}

static void echo_int(int val, const char *file) {
    char buffer[SMALL_BUFFER_SIZE] = {0};
    snprintf(buffer, sizeof(buffer), "%d", val);
    echo(buffer, file);
}

static void sleepms(int milliSec) {
    struct timeval delay;
    if (milliSec <= 0) {
        return;
    }
    delay.tv_sec = (milliSec / 1000);
    delay.tv_usec = (milliSec % 1000) * 1000;
    select(0, NULL, NULL, NULL, &delay);
}

static void trimstrtail(char * buffer) {
    if (buffer == NULL) {
        return;
    }
    int index = 0;
    index = strlen(buffer) - 1;
    while (index >= 0) {
        if (buffer[index] == 32 || buffer[index] == '\n' || buffer[index] == '\r') {
            index--;
        } else {
            break;
        }
    }
    buffer[index + 1] = 0;
}

static int cat(char *buffer, int bsize, const char* file) {
    char command[CPUTEST_RESULT_SIZE] = { 0 };
    int ret = 0;
    int index = 0;
    strcpy(command, "cat ");
    strcat(command, file);
    ALOGD("command: %s",command);
    FILE * fp = popen(command, "r");
    if (fp == NULL) {
        ALOGE("cat popen fail, errno: %d, %s", errno, strerror(errno));
        return -1;
    }
    if (fgets(buffer, bsize, fp) != NULL) {
        trimstrtail(buffer);
    }

    ALOGD("cat: >%s, errno:%d", buffer, errno);
    pclose(fp);
    return 0;
}

static int readfile(char *buffer, int bsize, const char* file) {
    FILE* fp = fopen(file, "r");
    int ret = 0;
    int len = 0;
    int index = 0;
    if (fp == NULL) {
        ALOGD("fopen fail: errno:%d, %s", errno, strerror(errno));
        return -1;
    }
    ret = fread(buffer, 1, bsize - 1, fp);
    if (ret > 0 && ferror(fp) == 0) {
        buffer[ret] = 0;
        trimstrtail(buffer);
    }
    fclose(fp);
    ALOGD("File content >%s", buffer);
    return 0;
}


static int parse_cpu_test_request(RPCClient *msg_client, cpu_test_request_t *request) {

    request->cpu_info_len = read_int_val(msg_client);
    request->test_data_len = read_int_val(msg_client);
    if (request->cpu_info_len > 0) {
        int cpuinfos = read_int_val(msg_client);
        request->core_num = (cpuinfos & 0xFF);
        request->core_bit_len = ((cpuinfos >> 8) & 0xFF);
        request->cpu_arch = ((cpuinfos >> 16) & 0xFF);
        request->cpu_bits = ((cpuinfos >> 24) & 0xFF);
        if (request->core_bit_len > 0) {
            int n = 32 / request->core_bit_len;
            int test_core_len = request->core_num / n + ((request->core_num % n > 0) ? 1 : 0);
            request->test_cores = (int *)malloc(sizeof(int) * test_core_len);
            if (request->test_cores == NULL) {
                ALOGD("parse_cpu_test_request; malloc fail for test_cores");
                return -1;
            }
            for (int i = 0; i < test_core_len; i++) {
                request->test_cores[i] = read_int_val(msg_client);
            }
        }
    }

    if (request->test_data_len > 0) {
        request->test_data = (int *)malloc(sizeof(int) * request->test_data_len);
        if (request->test_data == NULL) {
            ALOGD("parse_cpu_test_request; malloc fail for test_data");
            return -1;
        }
        for (int i = 0; i < request->test_data_len; i++) {
            request->test_data[i] = read_int_val(msg_client);
        }
    }
    return 0;
    
}

static int get_test_core_val(int core_index, cpu_test_request_t * request) {
    if (request->core_bit_len <= 0 || core_index >= request->core_num || core_index < 0) {
        ALOGD("get_test_core_val invalid argument: core_index:%d, core_num:%d", core_index, request->core_num);
        return -1;
    }
    int n = 32 / request->core_bit_len;
    int index = core_index / n;
    int location = core_index % n;
    int mask = 0xFFFFFFFF >> (32 - request->core_bit_len);
    int val = (request->test_cores[index] >> (location * request->core_bit_len)) & mask;
    return val;
}

static cpu_test_request_t * new_test_request() {
    cpu_test_request_t * request = (cpu_test_request_t *)malloc(sizeof(cpu_test_request_t));
    if (request != NULL) { 
        memset(request, 0, sizeof(cpu_test_request_t));
    }
    return request;
}

static int free_test_request(cpu_test_request_t * request) {
    if (request == NULL) {
        return -1;
    }
    free(request->test_cores);
    free(request->test_data);
    free(request);
    return 0;
}

static char* get_hotplug_file() {
    int chip = em_jni_get_chip_id();
    if (chip >= MTK_6595_SUPPORT) {
        return FILE_HOTPLUG_VER2;
    }
    return FILE_HOTPLUG;
}

void * apmcu_test(void * argvoid) {
    struct thread_params_t * arg = (struct thread_params_t *) argvoid;
    int fd = -1;
    char value[10] = { 0 };
    size_t s = 0;
    do {
        fd = open(arg->file, O_RDWR);
        ALOGD("open file: %s", arg->file);
        if (fd < 0) {
            snprintf(arg->result, sizeof(arg->result), "%s",
                    "fail to open device");
            ALOGE("fail to open device");
            break;
        }
        snprintf(value, sizeof(value), "%d", 1);
        write(fd, value, strlen(value));
        lseek(fd, 0, SEEK_SET);
        s = read(fd, arg->result, sizeof(arg->result));
        if (s <= 0) {
            snprintf(arg->result, sizeof(arg->result), "%s",
                    "could not read response");
            break;
        }
    } while (0);
        if (fd >= 0) {
            close(fd);
        }
    ALOGD("apmcu_test result : %s", arg->result);
    pthread_exit(NULL);
    return NULL;
}

void * swcodec_test(void * argvoid) {
    struct thread_params_t * arg = (struct thread_params_t *) argvoid;
    int tid = gettid();
    ALOGD("tid: %d, Enter swcodec_test: file: %s", tid, arg->file);
    FILE * fp;
    struct timeval timeout;
    struct timeval delay;
    delay.tv_sec = 0;
    delay.tv_usec = 100 * 1000;
    do {
        pthread_mutex_lock(&lock);
        fp = popen(arg->file, "r");
        pthread_mutex_unlock(&lock);
        select(0, NULL, NULL, NULL, &delay);
        if (fp == NULL) {
            ALOGE("popen fail: %s, errno: %d", arg->file, errno);
            strcpy(arg->result, "POPEN FAIL\n");
            break;
        }
        char *ret;
        while(1) {
            pthread_mutex_lock(&lock);
            ret = fgets(arg->result, sizeof(arg->result), fp);
            pthread_mutex_unlock(&lock);
            select(0, NULL, NULL, NULL, &delay);
            if (ret == NULL) {
                ALOGD("tid: %d, get result is null", tid);
                break;
            }
        }
    } while(0);
    if (fp != NULL) {
        pthread_mutex_lock(&lock);
        int closeRet = pclose(fp);
        pthread_mutex_unlock(&lock);
        select(0, NULL, NULL, NULL, &delay);
        while (closeRet == -1) {
            pthread_mutex_lock(&lock);
            closeRet = pclose(fp);
            pthread_mutex_unlock(&lock);
            select(0, NULL, NULL, NULL, &delay);
        }
        ALOGD("after pclose, tid: %d, errno: %d", tid, errno);
    }
    pthread_exit(NULL);
    return NULL;
}

static char* get_apmcu_test_file(int index, int core_index, cpu_test_request_t* request) {
    const char* test_files[][CORE_NUMBER_8] = {
        {FILE_VFP_0,    FILE_VFP_1,    FILE_VFP_2,    FILE_VFP_3,    FILE_VFP_4,    FILE_VFP_5,    FILE_VFP_6,    FILE_VFP_7},
        {FILE_CA7_0,    FILE_CA7_1,    FILE_CA7_2,    FILE_CA7_3,    FILE_CA7_4,    FILE_CA7_5,    FILE_CA7_6,    FILE_CA7_7},
        {FILE_DHRY_0,   FILE_DHRY_1,   FILE_DHRY_2,   FILE_DHRY_3,   FILE_DHRY_4,   FILE_DHRY_5,   FILE_DHRY_6,   FILE_DHRY_7},
        {FILE_MEMCPY_0, FILE_MEMCPY_1, FILE_MEMCPY_2, FILE_MEMCPY_3, FILE_MEMCPY_4, FILE_MEMCPY_5, FILE_MEMCPY_6, FILE_MEMCPY_7},
        {FILE_FDCT_0,   FILE_FDCT_1,   FILE_FDCT_2,   FILE_FDCT_3,   FILE_FDCT_4,   FILE_FDCT_5,   FILE_FDCT_6,   FILE_FDCT_7},
        {FILE_IMDCT_0,  FILE_IMDCT_1,  FILE_IMDCT_2,  FILE_IMDCT_3,  FILE_IMDCT_4,  FILE_IMDCT_5,  FILE_IMDCT_6,  FILE_IMDCT_7},
        {FILE_MAX_POWER64_0, FILE_MAX_POWER64_1, FILE_MAX_POWER64_2, FILE_MAX_POWER64_3, FILE_MAX_POWER64_4, FILE_MAX_POWER64_5, FILE_MAX_POWER64_6, FILE_MAX_POWER64_7},
        {FILE_DHRYSTONE64_0, FILE_DHRYSTONE64_1, FILE_DHRYSTONE64_2, FILE_DHRYSTONE64_3, FILE_DHRYSTONE64_4, FILE_DHRYSTONE64_5, FILE_DHRYSTONE64_6, FILE_DHRYSTONE64_7},
        {FILE_SAXPY_0, FILE_SAXPY_1, FILE_SAXPY_2, FILE_SAXPY_3, FILE_SAXPY_4, FILE_SAXPY_5, FILE_SAXPY_6, FILE_SAXPY_7},
        {FILE_SAXPY64_0, FILE_SAXPY64_1, FILE_SAXPY64_2, FILE_SAXPY64_3, FILE_SAXPY64_4, FILE_SAXPY64_5, FILE_SAXPY64_6, FILE_SAXPY64_7},
        {FILE_ADV_SIM_0, FILE_ADV_SIM_1, FILE_ADV_SIM_2, FILE_ADV_SIM_3, FILE_ADV_SIM_4, FILE_ADV_SIM_5, FILE_ADV_SIM_6, FILE_ADV_SIM_7},
        {FILE_IDLE2MAX_0, FILE_IDLE2MAX_1, FILE_IDLE2MAX_2, FILE_IDLE2MAX_3, FILE_IDLE2MAX_4, FILE_IDLE2MAX_5, FILE_IDLE2MAX_6, FILE_IDLE2MAX_7},      
    };
    int test_count = sizeof(test_files) / sizeof(test_files[0]);
    int core_count = sizeof(test_files[0]) / sizeof(test_files[0][0]);
    if (index < 0 || index >= test_count || core_index < 0 || core_index >= core_count) {
        ALOGE("get_apmcu_test_file: Invalid argument, index:%d, core_index:%d", index, core_index);
        return NULL;
    }

    int chip = em_jni_get_chip_id();
    if (chip == MTK_6575_SUPPORT || chip == MTK_6577_SUPPORT
            || chip == MTK_6573_SUPPORT || chip == MTK_6516_SUPPORT) {
        test_files[0][0] = FILE_NEON_0;
        test_files[0][1] = FILE_NEON_1;
        test_files[0][2] = FILE_NEON_2;
        test_files[0][3] = FILE_NEON_3;
        test_files[1][0] = FILE_CA9_0;
        test_files[1][1] = FILE_CA9_1;
        test_files[1][2] = FILE_CA9_2;
        test_files[1][3] = FILE_CA9_3;
    }

    if ((request->cpu_info_len > 0) && (request->cpu_arch == CPU_ARCH_BIG_LITTLE)) {
        test_files[1][4] = FILE_CA15_4;
        test_files[1][5] = FILE_CA15_5;
        test_files[1][6] = FILE_CA15_6;
        test_files[1][7] = FILE_CA15_7;
    }

    return (char *)test_files[index][core_index];
}


void * test_idle2max(void* arg) {
    void ** params = (void **)arg;
    void * thread_param = params[1];
    sem_t* sync_sem = (sem_t *)params[0];
    sem_post(sync_sem);
    return apmcu_test(thread_param);
}

void doApMcuTest(RPCClient* msgSender, cpu_test_request_t* request, int mode) {
    int index, test_core_num;
    char result[CPUTEST_RESULT_SIZE] = { 0 };

    struct thread_status_t test_threads[CORE_NUMBER_8] = {
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
    };
    
    ALOGD("test_threads size is %d", sizeof(test_threads));

    sched_param param;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setschedpolicy(&attr, SCHED_IDLE);
    param.sched_priority = 0;
    pthread_attr_setschedparam(&attr, &param);

    if (mode == MODE_RADIO_BACKUP_RESTORE) {
        if (request->test_data_len < 3) {
            ALOGD("doApMcuTest MODE_RADIO_BACKUP_RESTORE, invalid test_data");
            msgSender->PostMsg("INVALID_TEST_DATA");
            return;
        }
        index = request->test_data[1];
        test_core_num = request->test_data[2];
        
        for (int i = 0; i < test_core_num; i++) {
            char* test_file = get_apmcu_test_file(index, i, request);
            if (test_file != NULL) {
                strcpy(test_threads[i].param.file, test_file);
                test_threads[i].create_result = pthread_create(&test_threads[i].pid,
                        &attr, apmcu_test, (void *) &test_threads[i].param);
            }
        }
        for (int i = 0; i < test_core_num; i++) {
            if (test_threads[i].pid) {
                pthread_join(test_threads[i].pid, NULL);
            }
        }
        for (int i = 0; i < test_core_num; i++) {
            strncat(result, test_threads[i].param.result, strlen(test_threads[i].param.result)-1);
            strncat(result, ";", 1);
        }
        result[(int) strlen(result) - 1] = 0;
    } else if (mode == MODE_CHECK_CUSTOM || mode == MODE_CUSTOM_V2) {
        int core_num = request->core_num;
        if (core_num > CORE_NUMBER_MAX || core_num <= 0) {
            ALOGD("doApMcuTest, invalid core_num:%d", core_num);
            msgSender->PostMsg("INVALID_CORE_NUM");
            return;
        }
        if (request->test_data_len < 2) {
            ALOGD("MODE_CHECK_CUSTOM, invalid test_data");
            msgSender->PostMsg("INVALID_TEST_DATA");
            return;
        }
        index = request->test_data[1];
        if (index == INDEX_TEST_IDLE2MAX) {
            cpu_set_t backup_cpuset;
            cpu_set_t cpuset;
            CPU_ZERO(&backup_cpuset);
            int ret = sched_getaffinity(0, sizeof(cpu_set_t), &backup_cpuset);
            if (ret < 0) {
                ALOGD("fail to get original affinity");
                msgSender->PostMsg("FAIL_TO_BACKUP_AFFINITY");
                return;
            }
            CPU_ZERO(&cpuset);
            CPU_SET(0, &cpuset);
            ret = sched_setaffinity(0, sizeof(cpu_set_t), &cpuset);
            if (ret < 0) {
                ALOGD("fail to set affinity to cpu0");
                msgSender->PostMsg("FAIL_TO_BIND_TO_CPU0");
                return;
            }
            sem_t sync_sem;
            sem_init(&sync_sem, 0, 0);
            for (int i = 0; i < core_num; i++) {
                int val = get_test_core_val(i, request);
                if (val == FORCE_CORE_RUN) {
                    char * test_file = get_apmcu_test_file(index, i, request);
                    strcpy(test_threads[i].param.file, test_file);
                    void* params[2];
                    params[0] = &sync_sem;
                    params[1] = &test_threads[i].param;
                    test_threads[i].create_result = pthread_create(&test_threads[i].pid,
                            &attr, test_idle2max, (void *) params);
                    sem_wait(&sync_sem);
                }
            }
            sem_destroy(&sync_sem);

            for (int i = 0; i < core_num; i++) {
                int val = get_test_core_val(i, request);
                if (val == FORCE_CORE_RUN) {
                    if (test_threads[i].pid) {
                        pthread_join(test_threads[i].pid, NULL);
                    }
                }
            }
            
            ret = sched_setaffinity(0, sizeof(cpu_set_t), &backup_cpuset);
            if (ret < 0) {
                ALOGD("fail to restore process affinity with cpu");
            }

            
        } else {
            for (int i = 0; i < core_num; i++) {
                int val = get_test_core_val(i, request);
                if (val == FORCE_CORE_RUN) {
                    char * test_file = get_apmcu_test_file(index, i, request);
                    if (test_file == NULL) {
                        ALOGD("get null apmcu test file, index:%d, i:%d", index, i);
                        continue;
                    }
                    strcpy(test_threads[i].param.file, test_file);
                    test_threads[i].create_result = pthread_create(&test_threads[i].pid,
                            &attr, apmcu_test, (void *) &test_threads[i].param);
                }
            }

            for (int i = 0; i < core_num; i++) {
                int val = get_test_core_val(i, request);
                if (val == FORCE_CORE_RUN) {
                    if (test_threads[i].pid) {
                        pthread_join(test_threads[i].pid, NULL);
                    }
                }
            }
        }
        
        for (int i = 0; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            if (val == FORCE_CORE_RUN) {
                strncat(result, test_threads[i].param.result, strlen(test_threads[i].param.result) - 1);
                strncat(result, ";", 1);
            } else {
                strcat(result, "$[NO_TEST)#;");
            }

        }
        result[(int) strlen(result) - 1] = 0;
    } else {
        ALOGD("Unknown mode:%d", mode);
        msgSender->PostMsg("UNKNOWN_MODE");
        return;
    }

    ALOGD("apmcu result is %s", result);
    msgSender->PostMsg(result);
}

int ModuleCpuStress::init(RPCClient* msgSender) {
    int paramNum = msgSender->ReadInt();
    ALOGD("CPU STRESS INIT");
    system(COMMAND_DISABLE_PVLK_ALARM);
    msgSender->PostMsg("0");
    return 0;
}

int ModuleCpuStress::ApMcu(RPCClient* msgSender) {
    int paraNum = msgSender->ReadInt();
    int mode = 0;
    int ret = 0;
    cpu_test_request_t* request = new_test_request();
    ret = parse_cpu_test_request(msgSender, request);
    if (ret < 0) {
        ALOGD("parse_cpu_test_request fail");
        goto err_out;    
    }
    if (request->test_data_len < 1) {
        ALOGD("ApMcu: fail to get mode; invalid test_data_len:%d", request->test_data_len);
        goto err_out;
    }
    mode = request->test_data[0];
    doApMcuTest(msgSender, request, mode);
    goto out;
err_out:
    msgSender->PostMsg((char*)ERROR);
out:
    free_test_request(request);
    return ret;

}

static char* get_swcodec_test_command(int core_index) {
    const char* test_cmds[] = {
        COMMAND_SWCODEC_TEST_CORE_0,
        COMMAND_SWCODEC_TEST_CORE_1,
        COMMAND_SWCODEC_TEST_CORE_2,
        COMMAND_SWCODEC_TEST_CORE_3,
        COMMAND_SWCODEC_TEST_CORE_4,
        COMMAND_SWCODEC_TEST_CORE_5,
        COMMAND_SWCODEC_TEST_CORE_6,
        COMMAND_SWCODEC_TEST_CORE_7,};
    int len = sizeof(test_cmds) / sizeof(test_cmds[0]);
    if (core_index < 0 || core_index >= len) {
        ALOGD("get_swcodec_test_command INVALID core_index:%d", core_index);
        return NULL;
    }
    int chip = em_jni_get_chip_id();
    ALOGD("chip id: %d", chip);
    if (chip == MTK_6575_SUPPORT) {
        test_cmds[0] = COMMAND_SWCODEC_TEST_SINGLE_6575;
    } else if (chip == MTK_6577_SUPPORT) {
        test_cmds[0] = COMMAND_SWCODEC_TEST_DUAL_0_6577;
        test_cmds[1] = COMMAND_SWCODEC_TEST_DUAL_1_6577;
    }
    
    return (char *)test_cmds[core_index];
}

void doSwCodecTest(RPCClient* msgSender, cpu_test_request_t* request, int mode) {
    ALOGD("Enter doSwCodecTest");
    char result[CPUTEST_RESULT_SIZE] = { 0 };
    struct thread_status_t test_threads[] = {
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
        { pid : 0, create_result : -1, },
    };
    if (mode == MODE_RADIO_BACKUP_RESTORE) {
        if (request->test_data_len < 3) {
            ALOGD("doSwCodecTest MODE_RADIO_BACKUP_RESTORE, invalid test_data");
            msgSender->PostMsg("INVALID_TEST_DATA");
            return;
        }
        int test_core_number = request->test_data[1];
        int iteration = request->test_data[2];
        for (int i = 0; i < test_core_number; i++) {
            strcpy(test_threads[i].param.file, get_swcodec_test_command(i));
            test_threads[i].create_result = pthread_create(&test_threads[i].pid,
                    NULL, swcodec_test, (void *) &test_threads[i].param);
        }
        for (int i = 0; i < test_core_number; i++) {
            if (test_threads[i].pid) {
                pthread_join(test_threads[i].pid, NULL);
            }
        }

        for (int i = 0; i < test_core_number; i++) {
            strncat(result, test_threads[i].param.result, strlen(test_threads[i].param.result)-1);
            strncat(result, ";", 1);
        }
        result[(int) strlen(result) - 1] = 0;
    } else if (mode == MODE_CHECK_CUSTOM || mode == MODE_CUSTOM_V2) {
        int core_num = request->core_num;
        if (core_num > CORE_NUMBER_MAX || core_num <= 0) {
            ALOGD("doSwCodecTest, invalid core_num:%d", core_num);
            msgSender->PostMsg("INVALID_CORE_NUM");
            return;
        }
        for (int i = 0; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            if (val == FORCE_CORE_RUN) {
                strcpy(test_threads[i].param.file, get_swcodec_test_command(i));
                test_threads[i].create_result = pthread_create(&test_threads[i].pid,
                        NULL, swcodec_test, (void *) &test_threads[i].param);
            }
        }
        for (int i = 0; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            if (val == FORCE_CORE_RUN) {
                if (test_threads[i].pid) {
                    pthread_join(test_threads[i].pid, NULL);
                }
            }
        }
        for (int i = 0; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            if (val == FORCE_CORE_RUN) {
                strncat(result, test_threads[i].param.result, strlen(test_threads[i].param.result)-1);
                strncat(result, ";", 1);
            } else {
                strcat(result, "$[NO_TEST)#;");
            }
        }
        result[(int) strlen(result) - 1] = 0;
    } else {
        ALOGD("doSwCodecTest Unknown mode:%d", mode);
        msgSender->PostMsg("UNKNOWN_MODE");
        return;
    }
    ALOGD("doSwCodecTest result>%s", result);
    msgSender->PostMsg(result);
}

int ModuleCpuStress::SwCodec(RPCClient* msgSender) {
    int paraNum = msgSender->ReadInt();
    int mode = 0;
    int ret = 0;
    cpu_test_request_t* request = new_test_request();
    ret = parse_cpu_test_request(msgSender, request);
    if (ret < 0) {
        ALOGD("parse_cpu_test_request fail");
        goto err_out;    
    }
    if (request->test_data_len < 1) {
        ALOGD("ApMcu: fail to get mode; invalid test_data_len:%d", request->test_data_len);
        goto err_out;
    }
    mode = request->test_data[0];
    doSwCodecTest(msgSender, request, mode);
    goto out;
err_out:
    msgSender->PostMsg((char*)ERROR);
out:
    free_test_request(request);
    return ret;

}


static void backup(char buf[CPUTEST_RESULT_SIZE>>1], const char* file) {
    char command[CPUTEST_RESULT_SIZE] = { 0 };
    strcpy(command, "cat ");
    FILE * fp = popen(strcat(command, file), "r");
    if (fp == NULL) {
        ALOGE("INDEX_TEST_BACKUP popen fail, errno: %d", errno);
        return;
    }
    fgets(buf, CPUTEST_RESULT_SIZE>>1, fp);
    ALOGD("backup: %s", buf);
    pclose(fp);
}

void doBackupRestore(int index) {
    char command[CPUTEST_RESULT_SIZE] = { 0 };
    FILE * fp;
    switch(index) {
        case INDEX_TEST_BACKUP:
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_first, sizeof(backup_first), fp);
            ALOGD("backup_first: %s", backup_first);
            pclose(fp);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_BACKUP: %s", command);
            system(command);
            //system(COMMAND_HOTPLUG_DISABLE);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_TEST:
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_TEST: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU2_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_TEST: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU3_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_TEST: %s", command);
            system(command);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_SINGLE:
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_SINGLE popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_first, sizeof(backup_first), fp);
            ALOGD("backup_first: %s", backup_first);
            pclose(fp);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_BACKUP_SINGLE: %s", command);
            system(command);
            strcpy(command, "echo 0 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_SINGLE: %s", command);
            system(command);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_DUAL:
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_DUAL popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_first, sizeof(backup_first), fp);
            ALOGD("backup_first: %s", backup_first);
            pclose(fp);
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_DUAL popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_second, sizeof(backup_second), fp);
            ALOGD("backup_second: %s", backup_second);
            pclose(fp);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
            system(command);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_TRIPLE:
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_first, sizeof(backup_first), fp);
            ALOGD("backup_first: %s", backup_first);
            pclose(fp);
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_second, sizeof(backup_second), fp);
            ALOGD("backup_second: %s", backup_second);
            pclose(fp);
            fp = popen(strcat(command, FILE_CPU2_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_third, sizeof(backup_third), fp);
            ALOGD("backup_third: %s", backup_third);
            pclose(fp);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU2_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU2_SCAL);
            ALOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
            system(command);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_QUAD:
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_first, sizeof(backup_first), fp);
            ALOGD("backup_first: %s", backup_first);
            pclose(fp);
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_second, sizeof(backup_second), fp);
            ALOGD("backup_second: %s", backup_second);
            pclose(fp);
            fp = popen(strcat(command, FILE_CPU2_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_third, sizeof(backup_third), fp);
            ALOGD("backup_third: %s", backup_third);
            pclose(fp);
            strcpy(command, "cat ");
            fp = popen(strcat(command, FILE_CPU3_SCAL), "r");
            if (fp == NULL) {
                ALOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
                return;
            }
            fgets(backup_fourth, sizeof(backup_fourth), fp);
            ALOGD("backup_fourth: %s", backup_fourth);
            pclose(fp);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU2_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU2_SCAL);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo 1 > ");
            strcat(command, FILE_CPU3_ONLINE);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            strcpy(command, "echo performance > ");
            strcat(command, FILE_CPU3_SCAL);
            ALOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
            system(command);
            echo("0", get_hotplug_file());
            break;
        case INDEX_TEST_BACKUP_OCTA:
            ALOGD("INDEX_TEST_BACKUP_OCTA start");
            backup(backup_first, FILE_CPU0_SCAL);
            echo("performance", FILE_CPU0_SCAL);
            echo("1", FILE_CPU1_ONLINE);
            echo("1", FILE_CPU2_ONLINE);
            echo("1", FILE_CPU3_ONLINE);
            echo("1", FILE_CPU4_ONLINE);
            echo("1", FILE_CPU5_ONLINE);
            echo("1", FILE_CPU6_ONLINE);
            echo("1", FILE_CPU7_ONLINE);
            echo("0", FILE_HOTPLUG);
            echo("0", get_hotplug_file());
            ALOGD("INDEX_TEST_BACKUP_OCTA end");
            break;
        case INDEX_TEST_RESTORE:
            strcpy(command, "echo ");
            strncat(command, backup_first, strlen(backup_first) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_RESTORE: %s", command);
            system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_TEST:
            strcpy(command, "echo 0 > ");
            strcat(command, FILE_CPU1_ONLINE);
            ALOGD("INDEX_TEST_RESTORE_TEST: %s", command);
            system(command);
            strcpy(command, "echo 0 > ");
            strcat(command, FILE_CPU2_ONLINE);
            ALOGD("INDEX_TEST_RESTORE_TEST: %s", command);
            system(command);
            strcpy(command, "echo 0 > ");
            strcat(command, FILE_CPU3_ONLINE);
            ALOGD("INDEX_TEST_RESTORE_TEST: %s", command);
            system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_SINGLE:
            strcpy(command, "echo ");
            strncat(command, backup_first, strlen(backup_first) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_RESTORE_SINGLE: %s", command);
            system(command);
            //strcpy(command, "echo 1 > ");
            //strcat(command, FILE_CPU1_ONLINE);
            //ALOGD("INDEX_TEST_RESTORE_SINGLE: %s", command);
            //system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_DUAL:
            strcpy(command, "echo ");
            strncat(command, backup_first, strlen(backup_first) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_second, strlen(backup_second) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_TRIPLE:
            strcpy(command, "echo ");
            strncat(command, backup_first, strlen(backup_first) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_second, strlen(backup_second) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_third, strlen(backup_third) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU2_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_QUAD:
            strcpy(command, "echo ");
            strncat(command, backup_first, strlen(backup_first) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU0_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_second, strlen(backup_second) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU1_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_third, strlen(backup_third) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU2_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            strcpy(command, "echo ");
            strncat(command, backup_fourth, strlen(backup_fourth) - 1);
            strcat(command, " > ");
            strcat(command, FILE_CPU3_SCAL);
            ALOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
            system(command);
            echo("1", get_hotplug_file());
            break;
        case INDEX_TEST_RESTORE_OCTA:
            ALOGD("INDEX_TEST_RESTORE_OCTA start");
            echo(backup_first, FILE_CPU0_SCAL);
            echo("1", FILE_HOTPLUG);
            echo("1", get_hotplug_file());
            ALOGD("INDEX_TEST_RESTORE_OCTA end");
            break;
        default:
            break;
    }
}

int handle_backup_restore(RPCClient* msgSender, int index) {
    switch (index) {
    case INDEX_TEST_BACKUP:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP");
        break;
    case INDEX_TEST_BACKUP_TEST:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_TEST");
        break;
    case INDEX_TEST_BACKUP_SINGLE:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_SINGLE");
        break;
    case INDEX_TEST_BACKUP_DUAL:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_DUAL");
        break;
    case INDEX_TEST_BACKUP_TRIPLE:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_TRIPLE");
        break;
    case INDEX_TEST_BACKUP_QUAD:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_QUAD");
        break;
    case INDEX_TEST_BACKUP_OCTA:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_OCTA");
        break;
    case INDEX_TEST_RESTORE:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE");
        break;
    case INDEX_TEST_RESTORE_TEST:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_TEST");
        break;
    case INDEX_TEST_RESTORE_SINGLE:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_SINGLE");
        break;
    case INDEX_TEST_RESTORE_DUAL:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_DUAL");
        break;
    case INDEX_TEST_RESTORE_TRIPLE:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_TRIPLE");
        break;
    case INDEX_TEST_RESTORE_QUAD:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_QUAD");
        break;
    case INDEX_TEST_RESTORE_OCTA:
        doBackupRestore(index);
        msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_OCTA");
        break;
    default:
        ALOGE("BackupRestore unknown index: %d", index);
        msgSender->PostMsg((char *)"BackRestore unknown index");
        break;
    }
    return 0;
}

int handle_test_mode_check_change(RPCClient * msgSender, cpu_test_request_t* request, bool checked) {
    const char * val;
    char * retStr;
    if (checked) {
        val = "1";
    } else {
        val = "0";
    }
    for (int i = 1; i < request->core_num; i++) {
        const char* online_file = cpu_force_core_test_files[i].online;
        echo(val, online_file);
    }
    if (checked) {
        val = "0";
        retStr = "CHECKED_TEST_MODE_OK";
    } else {
        val = "1";
        retStr = "UNCHECKED_TEST_MODE_OK";
    }
    echo(val, FILE_HOTPLUG_VER2);
    msgSender->PostMsg(retStr);
    return 0;
}

int handle_custom_mode_check_change(RPCClient * msgSender, cpu_test_request_t* request) {
    int enable_num = 0;
    int disable_num = 0;
    int core_num = request->core_num;
    char* ret_str = NULL;
    int big_num = 0;
    for (int i = 0; i < core_num; i++) {
        int val = get_test_core_val(i, request);
        if (val == FORCE_CORE_DISABLE) {
            disable_num++;
            if (i >= core_num / 2) {
                big_num++;
            }
        } else if (val == FORCE_CORE_ENABLE) {
            enable_num++;
            if (i >= core_num / 2) {
                big_num++;
            }
        }
    }
    if (enable_num > 0 && disable_num > 0) {
        ALOGD("INVALID request; no support enable & disable meanwhile");
        goto err_out;
    }

    if (enable_num > 0) {
        echo("0", FILE_HOTPLUG_VER2);
        for (int i = 1; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            char* online_file = cpu_force_core_test_files[i].online;
            if (val == FORCE_CORE_ENABLE) {
                echo("1", online_file);
            } else {
                echo("0", online_file);
            }
        }

        int idx_arr_len = 1;
        int index_arr[2] = { 0 }; 
        if (big_num > 0) {
            idx_arr_len = 2;
            index_arr[1] = core_num / 2;
        } 

        for (int i = 0; i < idx_arr_len; i++) {
            int index = index_arr[i];
            char* buffer = backup_buffers[index];
            char* scaling_file = cpu_force_core_test_files[index].scaling_governor;
            readfile(buffer, sizeof(backup_first), scaling_file);
        }

        for (int i = 0; i < idx_arr_len; i++) {
            int index = index_arr[i];
            char* buffer = backup_buffers[index];
            char* scaling_file = cpu_force_core_test_files[index].scaling_governor;
            echo("performance", scaling_file);
        }

        ret_str = "ENABLE_CHECK_FORCE_CORE_OK";
    } else if (disable_num > 0) {
        int idx_arr_len = 1;
        int index_arr[2] = { 0 }; 
        if (big_num > 0) {
            idx_arr_len = 2;
            index_arr[1] = core_num / 2;
        }
        for (int i = 0; i < idx_arr_len; i++) {
            int index = index_arr[i];
            char* buffer = backup_buffers[index];
            char* scaling_file = cpu_force_core_test_files[index].scaling_governor;
            echo(buffer, scaling_file);
        }
        
        echo("1", FILE_HOTPLUG_VER2);
        ret_str = "DISABLE_CHECK_FORCE_CORE_OK";
    }
    
    if (ret_str == NULL) {
        ALOGD("ret_str was null");
        goto err_out;
    }
    msgSender->PostMsg(ret_str);
    return 0;
    
err_out:
    msgSender->PostMsg((char *)ERROR);
    return -1;

}

int  handle_check_custom_force_core(RPCClient * msgSender, cpu_test_request_t* request) {
    int ret = 0;
    ALOGD("ENTER handle_check_custom_force_core");
    if (request->cpu_info_len <= 0) {
        ALOGD("[handle_check_custom_force_core] invalid cpu_info_len");
        msgSender->PostMsg((char*)ERROR);
        return -1;
    }

    if (request->test_data_len < 2) {
        ALOGD("[handle_check_custom_force_core] invalid test_data_len");
        msgSender->PostMsg((char*)ERROR);
        return -1;
    }
    int index = request->test_data[1];
    int core_num = request->core_num;
    int test_files_num = sizeof(cpu_force_core_test_files) / sizeof(force_core_test_files_t);
    if (test_files_num < core_num) {
        ALOGD("[handle_check_custom_force_core] test_files_num:%d must be no less than core_num:%d ", core_num, test_files_num);
        msgSender->PostMsg((char*)ERROR);
        return -1;
    }

    switch (index) {
    case INDEX_FORCE_CORE_ENABLE_TEST_MODE:
        ret = handle_test_mode_check_change(msgSender, request, true);
        break;
    case INDEX_FORCE_CORE_DISABLE_TEST_MODE:
        ret = handle_test_mode_check_change(msgSender, request, false);
        break;
    case INDEX_FORCE_CORE_CUSTOM:
        ret = handle_custom_mode_check_change(msgSender, request);
        break;
    default:
        ALOGD("UNKNOWN index:%d", index);
        msgSender->PostMsg((char*)ERROR);
        ret = -1;
        break;
    }
    return ret;

}

int handle_check_test_mode_v2(RPCClient * msgSender, cpu_test_request_t* request, bool checked) {
    char* ret_str = ERROR;
    if (request->cpu_info_len <= 0) {
        ALOGD("handle_check_test_mode_v2, invalid cpu_info_len");
        msgSender->PostMsg(ERROR);
        return -1;
    } 
    int core_num = request->core_num;
    if (checked) {
        char* buffer0 = backup_buffers[0];
        char* scaling0 = cpu_force_core_test_files[0].scaling_governor;
        readfile(buffer0, sizeof(backup_first), scaling0);
        echo("performance", scaling0);
        echo("0", get_hotplug_file());
        for (int i = 1; i < core_num; i++) {
            char* online = cpu_force_core_test_files[i].online;
            echo("1", online);
        }
        ret_str = "ENABLE_TEST_MODE_V2_OK";
    } else {
        char* buffer0 = backup_buffers[0];
        char* scaling0 = cpu_force_core_test_files[0].scaling_governor;
        echo(buffer0, scaling0);
        echo("1", get_hotplug_file());
        ret_str = "DISABLE_TEST_MODE_V2_OK";
    }
    msgSender->PostMsg(ret_str);
    return 0;
}



int handle_custom_setting_changed_v2(RPCClient * msgSender, cpu_test_request_t* request) {
    char* ret_str = NULL;
    int ret = 0;
    if (request->cpu_info_len <= 0) {
        ALOGD("handle_custom_setting_changed_v2 invalid cpu_info_len:%d", request->cpu_info_len);
        msgSender->PostMsg(ERROR);
        return -1;
    }

    int disable_num = 0;
    int enable_core4_num = 0;
    int core_num = request->core_num;
    for (int i = 0; i < core_num; i++) {
        int val = get_test_core_val(i, request);
        if (val == FORCE_CORE_DISABLE) {
            disable_num++;
        } else if (val > FORCE_CORE_OFF && val < FORCE_CORE_DISABLE) {
            if (i >= 4) {
                enable_core4_num++;
            }
        }
    }

    if (disable_num > 0) {
        char* buffer0 = backup_buffers[0];
        char* scaling0 = cpu_force_core_test_files[0].scaling_governor;
        echo(buffer0, scaling0);
        echo("1", get_hotplug_file());
        ret_str = "DISABLE_CUSTOM_SETTING_V2_OK";
    } else {
        char* buffer0 = backup_buffers[0];
        char* scaling0 = cpu_force_core_test_files[0].scaling_governor;
        readfile(buffer0, sizeof(backup_first), scaling0);
        echo("performance", scaling0);
        echo("0", get_hotplug_file());
        bool close_core4 = false;
        for (int i = 1; i < core_num; i++) {
            char* online = cpu_force_core_test_files[i].online;
            echo("0", online);
        }
        for (int i = 1; i < core_num; i++) {
            int val = get_test_core_val(i, request);
            char* online = cpu_force_core_test_files[i].online;
            if (i == 4 && enable_core4_num > 0) {
                if (val == FORCE_CORE_OFF) {
                    echo("1", online);
                    close_core4 = true;
                    continue;
                }
            }
            if (val == FORCE_CORE_RUN) {
                echo("1", online);
            } else if (val == FORCE_CORE_IDLE) {
                char val_buff[SMALL_BUFFER_SIZE] = {0};
                snprintf(val_buff, sizeof(val_buff), "CPU%d", i);
                echo(val_buff, FILE_CPU_IDLE);
            }
        }
        if (close_core4) {
            char* online = cpu_force_core_test_files[4].online;
            echo("0", online);
        }

        ret_str = "ENABLE_CUSTOM_SETTING_V2_OK";
    }
    
    msgSender->PostMsg(ret_str);
    return ret;
}

int handle_custom_force_core_v2(RPCClient * msgSender, cpu_test_request_t* request) {
    char* ret_str = NULL;
    int ret = 0;
    if (request->test_data_len < 2) {
        ALOGD("handle_custom_force_core_v2 invalid test_data_len:%d", request->test_data_len);
        msgSender->PostMsg(ERROR);
        return -1;
    }
    if (request->cpu_info_len <= 0) {
        ALOGD("handle_custom_force_core_v2 invalid cpu_info_len:%d", request->cpu_info_len);
        msgSender->PostMsg(ERROR);
        return -1;
    }
    int index = request->test_data[1];
    int core_num = request->core_num;
    int test_file_num = sizeof(cpu_force_core_test_files) / sizeof(cpu_force_core_test_files[0]);
    if (test_file_num < core_num) {
        ALOGD("handle_custom_force_core_v2 test_file_num must be no less than core_num");
        msgSender->PostMsg(ERROR);
        return -1;
    }
    switch (index) {
    case INDEX_FORCE_CORE_DISABLE_TEST_MODE:
        ret = handle_check_test_mode_v2( msgSender, request, false);
        break;
    case INDEX_FORCE_CORE_ENABLE_TEST_MODE:
        ret = handle_check_test_mode_v2( msgSender, request, true);
        break;
    case INDEX_FORCE_CORE_CUSTOM:
        ret = handle_custom_setting_changed_v2(msgSender, request);
        break;
    default:
        ALOGD("handle_custom_force_core_v2 UNKNOWN index:%d", index);
        ret_str = ERROR;
        ret = -1;
        break;
    }
    msgSender->PostMsg(ret_str);
    return ret;
}

int ModuleCpuStress::BackupRestore(RPCClient* msgSender) {
    int paraNum = msgSender->ReadInt();
    int mode = 0;
    int ret = 0;
    cpu_test_request_t* request = new_test_request();
    ret = parse_cpu_test_request(msgSender, request);
    if (ret < 0) {
        ALOGD("parse_cpu_test_request fail");
        goto err_out;    
    }
    if (request->test_data_len < 1) {
        ALOGD("BackupRestore: fail to get mode; invalid test_data_len:%d", request->test_data_len);
        goto err_out;
    }
    mode = request->test_data[0];
    switch (mode) {
    case MODE_RADIO_BACKUP_RESTORE:
    {
        int index = 0;
        if (request->test_data_len < 2) {
            ALOGD("BackupRestore: fail to get index invalid test_data_len:%d", request->test_data_len);
            goto err_out;
        }
        index = request->test_data[1];
        ret = handle_backup_restore(msgSender, index);
    }
        break;
    case MODE_CHECK_CUSTOM:
        ret = handle_check_custom_force_core(msgSender, request);
        break;
    case MODE_CUSTOM_V2:
        ret = handle_custom_force_core_v2(msgSender, request);
        break;
    default:
        ALOGD("BackupRestore UNKOWN mode:%d", mode);
        goto err_out;
    }
    goto out;
err_out:
    msgSender->PostMsg((char*)ERROR);
out:
    free_test_request(request);
    return ret;
}


int ModuleCpuStress::ThermalUpdate(RPCClient* msgSender) {
    int paraNum = msgSender->ReadInt();
    int index = 0;
    if (paraNum != 1) {
        msgSender->PostMsg((char*) ERROR);
        return -1;
    }
    int T = msgSender->ReadInt();
    if (T != PARAM_TYPE_INT) {
        return -1;
    }
    int L = msgSender->ReadInt();
    index = msgSender->ReadInt();
    switch(index) {
    case INDEX_THERMAL_DISABLE:
        system(THERMAL_DISABLE_COMMAND);
        ALOGD("disable thermal: %s", THERMAL_DISABLE_COMMAND);
        msgSender->PostMsg((char *)"INDEX_THERMAL_DISABLE");
        break;
    case INDEX_THERMAL_ENABLE:
        system(THERMAL_ENABLE_COMMAND);
        ALOGD("enable thermal: %s", THERMAL_ENABLE_COMMAND);
        msgSender->PostMsg((char *)"INDEX_THERMAL_ENABLE");
        break;
    default:
        break;
    }
    return 0;
}

ModuleCpuStress::ModuleCpuStress(void) {
    pthread_mutex_init(&lock, NULL);
}

ModuleCpuStress::~ModuleCpuStress(void) {
    pthread_mutex_destroy(&lock);
}

