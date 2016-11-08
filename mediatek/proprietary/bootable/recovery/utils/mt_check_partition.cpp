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

#include <errno.h>
#include <getopt.h>
#include <limits.h>
#include <linux/input.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/reboot.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/vfs.h>
#include <stdarg.h>
#include <libgen.h>
#include <byteswap.h>
#include <endian.h>
#include <ctype.h>
#include <sys/mount.h>
#include <dirent.h>
#include <stdarg.h>
#include <fcntl.h>
#include <mtd/mtd-user.h>
#include <stdint.h>
#include <fs_mgr.h>
#include "bootloader.h"
#include "common.h"
#include "cutils/properties.h"
#include "install.h"
#include "minui/minui.h"
#include "minzip/DirUtil.h"
#include "minzip/Zip.h"
#include "roots.h"
#include "mt_check_partition.h"
#include "minzip/SysUtil.h"

#include "mtdutils/mounts.h"
#include "mt_common_tk.h"
#include "mt_partition.h"
#include "mt_gpt.h"
#include "mt_pmt.h"
#include "mt_sepol.h"
#include "mt_header.h"
#include "mt_backup_restore_image.h"

int usrdata_changed;

//tonykuo 2013-07-23
int from_mota = 0;
int isResizeFail = 0;
uint64_t resizeFailDataSize=0;
int part_size_changed = 0;

void clear_force_upgrade(void)
{
   from_mota = 0;
}

void set_force_upgrade(void)
{
   from_mota = 1;
}

int check_force_upgrade(void)
{
    return from_mota;
}

/* Short term solution to alter user that use OTA in normal mode instead of SIU */
/* 0 suceess , 1 error */

int force_upgrade(bool is_gpt, part_info_t *part_scatter[], part_info_t *part_mtd[]  ,int part_num)
{
   int userdata_index = 0;

   if(is_gpt)
      userdata_index = get_partition_index_by_name(part_scatter, part_num,"userdata");
   else
      userdata_index = get_partition_index_by_name(part_scatter, part_num,"usrdata");

   if((userdata_index > 0) && (userdata_index != part_num))
   {
        if(part_mtd[userdata_index]->offset != part_scatter[userdata_index]->offset)
        {
           if(!check_force_upgrade())
           {
              ui->Print("Phone is encrypt, check for force upgrade\n");
              return 1;
           }
           else
           {
              printf("Phone is encrypt, so data partition will be format\n");
              return 0;
           }
        }
   }
   else
     printf("Fail: can NOT find userdata partition\n");

   return 0;
}

int get_part_size_changed(void)
{
  return part_size_changed;
}

char backup_path[PATH_MAX];

/**
 * @brief backup user data
 *
 * This function is used to perform user data backup
 * @param [in]backup_size size of data partition to be backuped. 0: while partition backup, otherwise: size to be backuped
 * @return 0: success, otherwise: error code
 */
int userdata_backup(uint64_t backup_size)
{
    time_t t = time(NULL);
    struct tm *tmp = localtime(&t);
    struct statfs s, d;
    volatile uint64_t sd_free, data_used;
    FILE *fp;
    int val;
    my_header_t my_header;
    unsigned char plan[HEADER_SIZE];
    int phone_encrypted = 0;
    struct phone_encrypt_state phone_enc_state;
    //tonykuo 2013-08-06
    int num_raw_files = 1;
    int i;
    char tmp_backup_path[PATH_MAX];
    int force_raw_backup = 0;
    int child_pid_num = 0;

    memset(backup_path, 0, PATH_MAX);
    memset(&s, 0, sizeof(s));

    reset_total_files();
    reset_count_files();

    //wschen 2012-04-11 need to check if data is encrypted or not
    //if encrypted, not to mount data
    if (get_phone_encrypt_state(&phone_enc_state) != 0) {
        ui->Print("Error: Cannot get phone encrypt state\n");
        return INSTALL_ERROR;
    } else {
        if (phone_enc_state.state == PHONE_ENCRYPTED) {
            phone_encrypted = 1;
        }
    }

    if (ensure_path_mounted("/sdcard") != 0) {
        ui->Print("Error: No SD-Card.\n");
        return INSTALL_NO_SDCARD;
    }

    if (statfs("/sdcard", &s) != 0) {
        ui->Print("Error: Statfs /sdcard error.\n");
        return INSTALL_ERROR;
    }

    if (recovery_is_permissive() == false) {
        force_raw_backup = 1;
        printf("use raw data backup\n");
    }

    //if phone is encrypted, don't mount data
    if ((phone_encrypted == 0) && (!force_raw_backup)) {
        if (ensure_path_mounted("/data") != 0) {
            ensure_path_unmounted("/sdcard");
            ui->Print("Error: Mount /data error.\n");
            return INSTALL_ERROR;
        }

        if (statfs("/data", &d) != 0) {
            ui->Print("Error: Statfs /data error.\n");
            ensure_path_unmounted("/sdcard");
            ensure_path_unmounted("/data");
            return INSTALL_ERROR;
        }
    }

    //wschen 2011-04-28 remove uncompleted backup file
    fp = fopen("/sdcard/.data_backup_name", "r");
    if (fp) {
        if (fgets(backup_path, sizeof(backup_path), fp) == NULL) {
            printf("get backup_name fail\n");
        } else {
            if (!strncmp(backup_path, "/sdcard", strlen("/sdcard")) && (access(backup_path, R_OK) == 0))    // only unlink backup_path which exists and starts with /sdcard
                unlink(backup_path);
        }
        fclose(fp);
        unlink("/sdcard/.data_backup_name");
        sync();
    }

    sd_free = ((uint64_t) (s.f_bavail * s.f_bsize));
    //wschen 2012-04-11 need to check if data is encrypted or not
    //if encrypted, need to calculate the whole block device size
    if (phone_encrypted || force_raw_backup) {
        if (mt_is_support_gpt()) {
            data_used = get_partition_size("userdata");
        } else {
            data_used = get_partition_size("usrdata");
        }
        if (force_raw_backup && backup_size)
            data_used = backup_size;
    } else {
        data_used = ((uint64_t) ((d.f_blocks - d.f_bavail) * d.f_bsize));
    }
    if (data_used == 0) {
        ui->Print("Cannot get data space.\n");
        ensure_path_unmounted("/sdcard");
        return INSTALL_ERROR;
    }
    ui->Print("SD card free space: %lluMB\n", sd_free >> 20);
    ui->Print("User data allocated: %lluMB\n", data_used >> 20);

    if (sd_free <= data_used) {
        ui->Print("SD card did not have enough free space.\n");
        ensure_path_unmounted("/sdcard");
        ensure_path_unmounted("/data");
        return INSTALL_ERROR;
    }

    if (tmp == NULL) {
        struct timeval tp;
        gettimeofday(&tp, NULL);
        sprintf(backup_path, "/sdcard/userdata_%d.backup", (int) tp.tv_sec);
    } else {
        strftime(backup_path, PATH_MAX, "/sdcard/userdata_%Y%m%d_%H%M%S.backup", tmp);
    }

    fp = fopen(backup_path, "w");

    ui->Print("backup file: %s\n", basename(backup_path));
    if (!fp) {
        ui->Print("SD card is write protected.\n");
        ensure_path_unmounted("/sdcard");
        ensure_path_unmounted("/data");
        return INSTALL_ERROR;
    } else {
        fclose(fp);
        if (remove(backup_path) < 0)
            printf("Can not remove %s (%s)\n", backup_path, strerror(errno));
    }

    fp = fopen("/sdcard/.data_backup_name", "w");
    if (fp) {
        fprintf(fp, "%s", backup_path);
        fclose(fp);
        sync();
    }

    Volume* v = volume_for_path("/data");

    if (get_nand_type() == 0) {
        if (ensure_path_mounted("/data") != 0) {
            ui->Print("Error:userdata_backup Mount user data fail\n");
            return INSTALL_ERROR;
        }
    }

    if (do_raw_backup_restore(v->blk_device, backup_path, data_used, ACTION_BACKUP) < 0) {
        ui->Print("Error: backup fail.\n");
        if (remove(backup_path) < 0)
            printf("Can not remove %s (%s)\n", backup_path, strerror(errno));
        chdir("/");

        ensure_path_unmounted("/sdcard");
        ensure_path_unmounted("/data");
        return INSTALL_ERROR;
    }

    ui->SetProgress((float) 1.0);
    unlink("/sdcard/.data_backup_name");
    sync();

    chdir("/");

    ensure_path_unmounted("/sdcard");
    ensure_path_unmounted("/data");
    return INSTALL_SUCCESS;
}


#if 1 //tonykuo 2014-01-17
pt_resident_nand pmt_nand[PART_MAX_COUNT];
int pmt_read = 0;
#endif



//mode 1: from partition changed upgrade
int userdata_restore(char *filename, int mode)
{

    unsigned char my_buf[HEADER_SIZE];
    FILE *fp;
    my_header_t my_header;
    struct statfs d;
    uint8_t *hash_value;
    char *plan;
//    char key_data[16];
    struct phone_encrypt_state phone_enc_state;
    //tonykuo 2013-08-06
    int file_processed = 0;
    char tmp_filename[PATH_MAX];
    int force_raw_backup = 0;
    int child_pid_num = 0;
    int val;

    if (get_phone_encrypt_state(&phone_enc_state) != 0) {
        ui->Print("Error: Cannot get phone encrypt state\n");
        return INSTALL_ERROR;
    }

    if (recovery_is_permissive() == false) {
        force_raw_backup = 1;
    }

    if (((phone_enc_state.state == PHONE_ENCRYPTED) || (filename[0] == 0)) && mode) {
        //restore from partition size changed, and we do not backup user data, return OK
        if (phone_enc_state.state == PHONE_ENCRYPTED) {
            phone_enc_state.state = 0;
            set_phone_encrypt_state(&phone_enc_state);
            sync();
        }
        return INSTALL_SUCCESS;
    }

    reset_total_files();
    reset_count_files();

    ui->SetBackground(RecoveryUI::INSTALLING_UPDATE);
    ui->SetProgressType(RecoveryUI::EMPTY);
    ui->ShowProgress(1.0, 0);

    if (get_nand_type() == 0)   // do sync for nand project to get rid of restore attach fail after poewr lose
        sync();

    sprintf(tmp_filename, "%s", filename);

    do {
        printf("tmp_filename=%s\n", tmp_filename);

        fp = fopen(tmp_filename, "r");
        if (fp) {
            if (fread(my_buf, sizeof(my_buf), 1, fp) == 0) {
                printf("read backup header of %s fail (%s)!\n", tmp_filename, strerror(errno));
                fclose(fp);
                return INSTALL_CORRUPT;
            }
            fclose(fp);
        } else {
            ui->Print("Error: Open %s fail\n", tmp_filename);
            return INSTALL_CORRUPT;
        }

        get_encrypted_custom_header(my_buf,sizeof(my_buf),&my_header);
        //check checksum
        hash_value = hash_file(tmp_filename);

        if (hash_value != NULL) {
            if (memcmp(hash_value, my_header.checksum, COMPARE_NUM) != 0) {
                printf("Error: Checksum compare fail\n");
                ui->Print("The backuped file is out of date.\nPlease re-backup your data\n");
                free(hash_value);
                return INSTALL_CORRUPT;
            }
            free(hash_value);
        } else {
            ui->Print("Error: Checksum fail\n");
            return INSTALL_CORRUPT;
        }

        if (!mode) {
            uint64_t data_size = 0;
            if (mt_is_support_gpt()) {
                data_size = get_partition_size("userdata");
            } else {
                data_size = get_partition_size("usrdata");
            }
            //check size
            printf("backup size=%ju part_size=%ju\n", my_header.size, data_size);
            printf("count=%d\n", my_header.file_count);
            if (my_header.encrypt == 0) {
                if (my_header.size > data_size) {
                    ui->Print("User data did not have enough space to restore.\n");
                    return INSTALL_ERROR;
                }
            } else {

                if (my_header.size != data_size) {
                    ui->Print("User data did not match space to restore.\n");
                    return INSTALL_ERROR;
                }
            }
        }

        set_total_files(my_header.file_count);
        file_processed++;
        sprintf(tmp_filename, "%s%d", filename, file_processed);
    } while (file_processed < my_header.raw_file_count);

    ui->Print("Formatting user data...");

    //make sure umount success
    chdir("/");

    ensure_path_unmounted("/data");
    if (format_volume("/data") != 0) {
        ui->Print("\nFormatting user data fail\n");
        return INSTALL_CORRUPT;
    }
    ui->Print(" Done.\n");
    ui->Print("Restoring user data...\n");

    Volume* v = volume_for_path("/data");
    if (get_nand_type() == 0)
    {
        ui->Print("Trying to mount ubifs /data \n");
        if (ensure_ubi_attach("/data") < 0)
            return INSTALL_ERROR;
    }

    if (do_raw_backup_restore(filename, v->blk_device, my_header.size, ACTION_RESTORE) < 0) {
        ui->Print("Error: Restore user data fail\n");
        if (format_volume("/data") < 0)
            printf("Error: format /data\n");
        phone_enc_state.state = 0;
        set_phone_encrypt_state(&phone_enc_state);
        sync();

        return INSTALL_ERROR;
    }

    chdir("/");
    ensure_path_unmounted("/data");
    return INSTALL_SUCCESS;
}



typedef struct kernel_mtd_t {
    uint64_t size;
    uint64_t offset;
} kernel_mtd_t;

static int open_update_package(ZipArchive *zip, const char *zip_path, MemMapping* pMap,bool needs_mount)
{
    /* Before checking partition size  will close the zip, so re-open the zip file */
    int err = 0;
    if(needs_mount)
    {
         if (zip_path[0] == '@') {
            if (ensure_path_mounted(zip_path+1) != 0) {
                LOGE("Can't mount %s\n", zip_path);
                return INSTALL_CORRUPT;
            }
         } else {
            if (ensure_path_mounted(zip_path) != 0) {
                LOGE("Can't mount %s\n", zip_path);
                return INSTALL_CORRUPT;
            }
        }
    }
    if (sysMapFile(zip_path, pMap) != 0) {
        ui->Print("failed to map file in check_part_size \n");
        return INSTALL_CORRUPT;
    }

    err = mzOpenZipArchive(pMap->addr, pMap->length, zip);
    if (err != 0) {
        LOGE("Can't open %s\n(%s)\n", pMap->addr, err != -1 ? strerror(err) : "bad");
        return INSTALL_CORRUPT;
    }

    return INSTALL_SUCCESS;
}

static void close_update_package(ZipArchive *zip, MemMapping* pMap)
{
    /* Close the zip and release mapping before checking partition size */
       mzCloseZipArchive(zip);
       sysReleaseMap(pMap);
}

#define RESIZE_PARAMETER_BUF_LEN    64
char resize_unit_ch[4] = {'s', 'K', 'M', 'G'};
int do_resize2fs(const char *dev_name, uint64_t new_part_size)
{
    char new_size_str[RESIZE_PARAMETER_BUF_LEN];
    int unit = 0;
    const char** args = (const char**)malloc(sizeof(char*) * 5);
    char command[RESIZE_PARAMETER_BUF_LEN];
    pid_t pid;
    int status;
    int system_mount_change = 0;
    int data_mount_change = 0;
    const MountedVolume* vol;
    if (args == NULL) {
        printf("%s malloc fail!\n", __FUNCTION__);
        return -1;
    }

    printf("do_resize2fs new_part_size = %ju\n", new_part_size);
    if (new_part_size == 0) {
        printf("incorrect new partition size %ju\n", new_part_size);
        free(args);
        return -1;
    }
    if ((new_part_size % 4096) != 0) {
        printf("new partition size %ju is not 4k aligned\n", new_part_size);
        free(args);
        return -1;
    }

    while ((new_part_size % 1024) == 0) {
        unit++;
        new_part_size = new_part_size >> 10;
        if (unit >= (int) (sizeof(resize_unit_ch) - 1))
            break;
    }
    snprintf(new_size_str, RESIZE_PARAMETER_BUF_LEN, "%ju%c", new_part_size, resize_unit_ch[unit]);
    printf("do_resize2fs new_size_str = %s\n", new_size_str);

    scan_mounted_volumes();
    vol = find_mounted_volume_by_mount_point("/system");
    if (vol == NULL)
        system_mount_change = 1;
    vol = find_mounted_volume_by_mount_point("/data");
    if (vol)
        data_mount_change = 1;

    if (system_mount_change) {
        if (ensure_path_mounted("/system") != 0) {
            ui->Print("Mount /system error.\n");
            free(args);
            return -1;
        }
    }

    if (data_mount_change) {
        if (ensure_path_unmounted("/data") != 0) {
            ui->Print("Unmount /data error.\n");
            free(args);
            return -1;
        }
    }

    // do e2fsck first
    sprintf(command, "/system/bin/e2fsck");
    args[0] = command;
    args[1] = "-pf";
    args[2] = dev_name;
    args[3] = NULL;
    printf("%s -pf %s\n", command, dev_name);

    pid = fork();
    if (pid == 0) {
        execvp(command, (char* const*)args);
        fprintf(stdout, "E:Can't run %s (%s)\n", command, strerror(errno));
        _exit(-1);
    }
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        if (WEXITSTATUS(status) > 1) {
            printf("Error for %s(Status %d)\n", command, WEXITSTATUS(status));
            if (system_mount_change)
                ensure_path_unmounted("/system");
            if (data_mount_change)
                ensure_path_mounted("/data");
            free(args);
            return -1;
        }
    }

    // do resize2fs
    sprintf(command, "/system/bin/resize2fs");
    args[0] = command;
    args[1] = "-f";
    args[2] = dev_name;
    args[3] = new_size_str;
    args[4] = NULL;
    printf("%s -f %s %s\n", command, dev_name, new_size_str);

    pid = fork();
    if (pid == 0) {
        umask(022);
        execvp(command, (char* const*)args);
        fprintf(stdout, "E:Can't run %s (%s)\n", command, strerror(errno));
        _exit(-1);
    }


    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        if (WEXITSTATUS(status) != 0) {
            printf("Error for %s(Status %d)\n", command, WEXITSTATUS(status));
            if (system_mount_change)
                ensure_path_unmounted("/system");
            if (data_mount_change)
                ensure_path_mounted("/data");
            free(args);
            return -1;
        }
    }

    if (system_mount_change) {
        if (ensure_path_unmounted("/system") != 0) {
            ui->Print("Unmount /system error.\n");
            free(args);
            return -1;
        }
    }
    if (data_mount_change) {
        if (ensure_path_mounted("/data") != 0) {
            ui->Print("Mount /data error.\n");
            free(args);
            return -1;
        }
    }

    printf("resize %s to %s done\n", dev_name, new_size_str);

    free(args);
    return 0;
}

//path_is_data == 2 -> using block map, can not erase data & cache
int check_part_size(ZipArchive *zip, int path_is_data, const char *zip_path, MemMapping* pMap,bool needs_mount)
{
    const ZipEntry *scatter_entry = mzFindZipEntry(zip, "scatter.txt");
    char *fname = (char *)"/tmp/scatter";
    bool ok;
    int part_num = 0, mtd_cnt = 0;
    char buf[256];
    FILE *fp = NULL;
    part_info_t *part_mtd[MAX_PARTITION_NUM], *part_scatter[MAX_PARTITION_NUM], *part_mtd_ptr = NULL;
    int i;
    int fd;
    uint64_t total_size = 0;
    int upgrade_type = 0;
    const ZipEntry *type_entry;
    char *tname = (char *)"/tmp/type";
    int err = 0;
    int force_raw_backup = 0;
    uint64_t new_data_size = 0;
    int retval = ERROR_CANNOT_UPGRADE;

    bool is_gpt = mt_is_support_gpt();

    part_size_changed = 0;
    memset(part_mtd, 0, sizeof(part_info_t*)*MAX_PARTITION_NUM);
    memset(part_scatter, 0, sizeof(part_info_t*)*MAX_PARTITION_NUM);


    if (scatter_entry == NULL) {
        mzCloseZipArchive(zip);
        ui->Print("Error: Invalid OTA package, missing scatter\n");
        return 1;
    }

    unlink(fname);
    fd = creat(fname, 0755);
    if (fd < 0) {
        mzCloseZipArchive(zip);
        ui->Print("Error: Can't make %s\n", fname);
        return 1;
    }

    if (recovery_is_permissive() == false) {
        force_raw_backup = 1;
        printf("Due to SELinux policy, recovery is enforcing mode\n");
        printf("Only support backup raw user data\n");
    }

    ok = mzExtractZipEntryToFile(zip, scatter_entry, fd);
    close(fd);

    if (!ok) {
        ui->Print("Error: File system fail, scatter\n");
        return 1;
    }

    //check upgrade type
    type_entry = mzFindZipEntry(zip, "type.txt");
    if (type_entry == NULL) {
        mzCloseZipArchive(zip);
        ui->Print("Error: Invalid OTA package, missing type\n");
        unlink(fname);
        return 1;
    }

    unlink(tname);
    fd = creat(tname, 0755);
    if (fd < 0) {
        mzCloseZipArchive(zip);
        ui->Print("Error: Can't make %s\n", tname);
        unlink(fname);
        return 1;
    }

    ok = mzExtractZipEntryToFile(zip, type_entry, fd);
    close(fd);

    if (!ok) {
        ui->Print("Error: File system fail, type\n");
        return 1;
    }

    fp = fopen(tname, "r");
    if (fp) {
        fgets(buf, sizeof(buf), fp);
        upgrade_type = atoi(buf);
        fclose(fp);
        printf("upgrade_type=%d\n", upgrade_type);
        if (upgrade_type == 0) {
            if (ensure_path_mounted("/cache") != 0) {
                printf("Differential OTA mount cache fail\n");
            }
        }
    } else {
        ui->Print("Error: Open type fail\n");
        unlink(fname);
        unlink(tname);
        return 1;
    }

    /* Close the zip and release mapping before checking partition size */
    close_update_package(zip, pMap);

    //check partition size
    if (get_nand_type() == 0)
    {
        i = get_partition_info_mlc(part_mtd, &part_num, &total_size);
    }
    else
    {
        i = get_partition_info(part_mtd, &part_num, &total_size);
    }

    if (i != CHECK_OK) {
        switch (i) {
            case ERROR_FILE_OPEN:
                ui->Print("Error: Get partition info fail\n");
                break;
            case ERROR_OUT_OF_MEMORY:
                ui->Print("Error: function:%s line:%d Out of memory\n",__FUNCTION__,__LINE__);
                break;
        }
        return i;
    }

    if (part_num) {
        mtd_cnt = part_num;
    }

    //check scatter file
    fp = fopen(fname, "r");

    if (fp) {
        char p_name[PTENT_BUF_SIZE];
        char p_offset[PTENT_BUF_SIZE];
        part_num = 0;
        set_scatter_expdb_succeed(0);

        int data_index = 0;

        printf("====== Scatter File:\n");
        while (fgets(buf, sizeof(buf), fp)) {
            printf("%s", buf);
            if (strlen(buf) && (buf[0] != '{') && (buf[0] != '}')) {
                if (sscanf(buf, "%s %s", p_name, p_offset) == 2) {
                    if (is_gpt) {
                        if (!strcmp(p_name, "preloader") || !strcasecmp(p_name, "pgpt")) {
                            //skip PGPT && preloader
                            continue;
                        }

                        if (data_index && (part_num > data_index)) {
                            // next to userdata must be intsd or skip following partition
                            if (strcmp(p_name, "intsd")) {
                                continue;
                            }
                        }
                    }
                    part_mtd_ptr = (part_info_t *)malloc(sizeof(part_info_t));
                    if (part_mtd_ptr != NULL) {
                        part_mtd_ptr->offset = hex2ulong(p_offset + 2);
                        p_name[strlen(p_name)] = 0;
                        snprintf(part_mtd_ptr->name, sizeof(part_mtd_ptr->name), "%s", p_name);
                        part_scatter[part_num] = part_mtd_ptr;
                        if (is_gpt) {
                            if (!strcmp(p_name, "system")) {
                                if (!strcmp(part_scatter[part_num - 1]->name, "expdb")) {
                                    set_scatter_expdb_succeed(1);
                                }
                            } else if (!strcmp(p_name, "userdata")) {
                                data_index = part_num;
                            }
                        } else {
                            if (!strcmp(p_name, "ANDROID")) {
                                if (!strcmp(part_scatter[part_num - 1]->name, "__NODL_EXPDB")) {
                                    set_scatter_expdb_succeed(1);
                                }
                            }
                        }
                        part_num++;
                        if (part_num >= MAX_PARTITION_NUM) {
                            for (i = 0; i < mtd_cnt; i++) {
                                free(part_mtd[i]);
                            }
                            ui->Print("Error: Too many partitions\n");
                            fclose(fp);
                            unlink(fname);
                            unlink(tname);
                            return 1;
                        }
                    } else {
                        for (i = 0; i < mtd_cnt; i++) {
                            free(part_mtd[i]);
                        }
                        ui->Print("Error: function:%s line:%d Out of memory\n",__FUNCTION__,__LINE__);
                        fclose(fp);
                        unlink(fname);
                        unlink(tname);
                        return 1;
                    }
                } else {
                    continue;
                }
            }
        }

        fclose(fp);
    } else {
        for (i = 0; i < mtd_cnt; i++) {
            free(part_mtd[i]);
        }

        ui->Print("Error: Open scatter fail\n");
        unlink(fname);
        unlink(tname);
        return 1;
    }

    if (part_num != mtd_cnt) {
        ui->Print("Error: Partition table not match\n");
        goto fail_out;
    } else {
        //add checking for system/cache/userdata are in the end partition
        if (!check_partition_layout(part_scatter, part_num, is_gpt)) {
            ui->Print("Error: Invalid partition setting (%d)\n", part_num);
            goto fail_out;
        }
        //partition match
        int match_cnt = 0;

        printf("phone_expdb_succeed=%d\n", get_phone_expdb_succeed());
        printf("scatter_expdb_succeed=%d\n", get_scatter_expdb_succeed());

        usrdata_changed = 0;

        //get PMT for MLC tonykuo 2014-01-17
        if (!get_emmc_phone() && get_pmt_version()) {
            memset(pmt_nand, 0, sizeof(pmt_nand));
            if (get_pmt_nand(pmt_nand) != 0) {
                printf("get_pmt_nand fail\n");
                //goto fail_out;
            } else {
                pmt_read = 1;
                copy_part_scatter_dup(part_scatter, mtd_cnt);
            }
        }

        int system_part_idx;
        system_part_idx = get_partition_index_by_name(part_scatter, part_num,"system");
        if ((system_part_idx > 0) && (system_part_idx >= part_num)) {
            system_part_idx = get_partition_index_by_name(part_scatter, part_num,"ANDROID");
            if ((system_part_idx > 0) && (system_part_idx >= part_num)) {
                ui->Print("Invalid partition setting from scatter file\n");
                goto fail_out;
            }
        }

        if (!get_emmc_phone()) {
                //check partition start address, before and include expdb
            for (i = 0; i <= system_part_idx; i++) {
                    if (part_mtd[i]->offset != part_scatter[i]->offset) {
                        if (pmt_read) {
                            if (pmt_nand[i].offset != part_scatter[i]->offset) { //check pmt offset again for MLC
                                ui->Print("Error: Invalid partition setting from PMT\n");
                                ui->Print("%d: %s %llx:%jx\n", i, pmt_nand[i].name, pmt_nand[i].offset, part_scatter[i]->offset);
                                goto fail_out;
                            } else {
                                set_MLC_case(1);
                            }
                        } else {
                            ui->Print("Error: Invalid partition setting from dumchar_info\n");
                            ui->Print("%d: %s %jx:%jx\n", i, part_mtd[i]->name, part_mtd[i]->offset, part_scatter[i]->offset);
                            goto fail_out;
                        }
                    }
                }
                //check if partition size changed, after expdb
            for (i = system_part_idx+1; i < mtd_cnt; i++) {
                if (part_mtd[i]->offset != part_scatter[i]->offset) {
                    if (pmt_read) {
                        if (pmt_nand[i].offset != part_scatter[i]->offset) { //check pmt offset again for MLC
                            if (!strcmp(part_mtd[i]->name,"usrdata")) {
                                    usrdata_changed = 1;
                            }
                            printf("Partition layout change, [%d]: %s %jx %jx\n", i, part_mtd[i]->name, part_mtd[i]->offset, part_scatter[i]->offset);
                            match_cnt++;
                            break;
                        }
                    } else {
                        if (!strcmp(part_mtd[i]->name,"usrdata")) {
                                usrdata_changed = 1;
                        }
                        match_cnt++;
                    }
                }
            }
        } else {
            for (i = 0; i <= system_part_idx; i++) {
                if (part_mtd[i]->offset != part_scatter[i]->offset) {
                    ui->Print("Error: Invalid partition setting\n");
                    ui->Print("%d: %s %jx:%jx\n", i, part_mtd[i]->name, part_mtd[i]->offset, part_scatter[i]->offset);
                    goto fail_out;
                }
            }
            for (i = system_part_idx+1; i < mtd_cnt; i++) {
                if (part_mtd[i]->offset != part_scatter[i]->offset) {
                    if (is_gpt) {
                        if (!strcmp(part_mtd[i]->name,"userdata")) {
                            usrdata_changed = 1;
                        }
                    } else {
                        if (!strcmp(part_mtd[i]->name,"usrdata")) {
                            usrdata_changed = 1;
                        }
                    }
                    printf("Partition layout change, [%d]: %s %jx %jx\n", i, part_mtd[i]->name, part_mtd[i]->offset, part_scatter[i]->offset);
                    match_cnt++;
                }
            }
            int fat_idx = get_partition_index_by_name(part_scatter, part_num,"intsd");
            //FAT
            if ((fat_idx > 0) && (fat_idx < mtd_cnt) && (part_mtd[fat_idx]->offset != part_scatter[fat_idx]->offset)) {
                ui->Print("Error: Invalid partition setting\n");
                ui->Print("%d: %s %jx:%jx\n", fat_idx, part_mtd[fat_idx]->name, part_mtd[fat_idx]->offset, part_scatter[fat_idx]->offset);
                goto fail_out;
            }
        }

        if (match_cnt) {

            uint64_t sd_free = 0, data_used;
            struct statfs s;
            char temp[] ="/sdcard/XXXXXX";
            kernel_mtd_t new_mtd[3];
            int phone_encrypted = 0;
            struct phone_encrypt_state phone_enc_state;

            if (get_emmc_phone()) {
                if (get_phone_encrypt_state(&phone_enc_state) != 0) {
                    ui->Print("Error: Cannot get phone encrypt state\n");
                    goto fail_out;
                } else {
                    if (phone_enc_state.state == PHONE_ENCRYPTED) {
                        phone_encrypted = 1;
                    }
                }
            }


            if (!upgrade_type) {
                ui->Print("Only support full upgrade with size changed.\n");
                goto fail_out;
            }

            part_size_changed = 1;

            //Abort update since /data layout change and it's updating from /data
            if (usrdata_changed && path_is_data) {
                ui->Print("Updating from /data with /data partition layout change.\n");
                goto fail_out;
            }

            if (path_is_data) {
                printf("updating from /data without it's layout change, skip stat SD info.\n");
            } else {
                if (ensure_path_mounted("/sdcard") != 0) {
                    ui->Print("Error: Mount /sdcard error.\n");
                    goto fail_out;
                }

                if (statfs("/sdcard", &s) != 0) {
                    ui->Print("Error: Statfs /sdcard error.\n");
                    goto fail_out;
                }

                //check SD free space
                sd_free = ((uint64_t) (s.f_bavail * s.f_bsize));

                if (is_gpt) {
                    data_used = get_partition_size("userdata");
                } else {
                    data_used = get_partition_size("usrdata");
                }

                if (!phone_encrypted) {
                    //check if write protected
                    fd = mkstemp(temp);
                    if (fd) {
                        close(fd);
                        unlink(temp);
                    } else {
                        ui->Print("SD card is write protected.\n");
                        goto fail_out;
                    }
                    new_data_size = data_used;
                    if (!get_emmc_phone()) {
                        if ((total_size - part_scatter[part_num - 1]->offset) < data_used) {
                            ui->Print("New data partition did not have enough free space.\n");
                            goto fail_out;
                        }
                    } else {
                        if (get_has_fat()) {
                            new_data_size = part_scatter[part_num - 1]->offset - part_scatter[part_num - 2]->offset;
                        } else {
                            new_data_size = total_size - part_scatter[part_num - 1]->offset;
                        }
                    }

                    //Skip userdata backup if updating from /data with no /data layout change
                    if (usrdata_changed) {
                        uint64_t backup_size = data_used;
                        printf("userdata partition sized changed, start to backup user data\n");

                        if (sd_free <= new_data_size) {
                            ui->Print("SD card did not have enough free space.\n");
                            ui->Print("SD free %ju MB, new_data_size %ju MB.\n", sd_free >> 20, new_data_size >> 20);
                            goto fail_out;
                        }
                        if (isResizeFail && (resizeFailDataSize != new_data_size)) {
                            isResizeFail = 0; // might be another ota package because that new data size is different from previous upgrade package
                            resizeFailDataSize = 0;
                        } else if (isResizeFail && (resizeFailDataSize == new_data_size)) { // the same otapackage, check if force upgrade
                            if (!check_force_upgrade()) {
                                ui->Print("Can not update due to new data partition is smaller than current used!\n");
                                goto prompt_out;
                            }
                        }
                        if (get_emmc_phone() && (isResizeFail == 0) && (new_data_size < data_used)) {
                            Volume* v = volume_for_path("/data");
                            if (do_resize2fs(v->blk_device, new_data_size) < 0) {
                                printf("Error: Resize partition fail\n");
                                ui->Print("Error: due to partition size changed, data partition has to be erased to continue upgrade\n");
                                isResizeFail = 1;
                                resizeFailDataSize = new_data_size;
                                if (!check_force_upgrade())
                                    goto prompt_out;
                            }
                            backup_size = new_data_size;
                        }

                        if (isResizeFail == 0) {
                            if (userdata_backup(backup_size)) {
                                goto fail_out;
                            }
                        }
                    }
                    if (ensure_path_unmounted("/data")) {
                        ui->Print("Error: Fail to umount /data\n");
                    }
                }  else { // end of phone_encrypted
                    /* phone is encrypt, short term solution */
                    if(force_upgrade(is_gpt, part_scatter, part_mtd,part_num))
                        goto prompt_out;
                }
            }

            if (ensure_path_unmounted("/system")) {
                ui->Print("Error: Fail to umount /system\n");
            }

            if (ensure_path_unmounted("/cache")) {
                ui->Print("Error: Fail to umount /cache\n");
            }

            if (!get_emmc_phone()) {
                if (get_phone_expdb_succeed() && get_scatter_expdb_succeed()) {
                    kernel_mtd_t nmtd[4];
                    mtd_info_user mtd_info;
                    //update kernel partition table
                    //expdb
                    nmtd[0].size = (part_scatter[part_num - 3]->offset - part_scatter[part_num - 4]->offset);
                    nmtd[0].offset = part_scatter[part_num - 4]->offset;
                    //system
                    nmtd[1].size = (part_scatter[part_num - 2]->offset - part_scatter[part_num - 3]->offset);
                    nmtd[1].offset = part_scatter[part_num - 3]->offset;
                    //cache
                    nmtd[2].size = (part_scatter[part_num - 1]->offset - part_scatter[part_num - 2]->offset);
                    nmtd[2].offset = part_scatter[part_num - 2]->offset;
                    //userdata
                        nmtd[3].size = (total_size - part_scatter[part_num - 1]->offset);
                        nmtd[3].offset = part_scatter[part_num - 1]->offset;
                        //printf("MLC total size = %llx, data size = %llx\n", total_size, nmtd[3].size);

                    fp = fopen("/proc/driver/mtd_change", "w");
                    if (fp) {
                        if (fwrite(nmtd, sizeof(nmtd), 1, fp) != 1) {
                            ui->Print("Error: Write MTD information fail\n");
                            fclose(fp);
                            goto fail_out;
                        }
                        fclose(fp);
                    } else {
                        ui->Print("Error: Open MTD information fail\n");
                        goto fail_out;
                    }

                } else {
                    mtd_info_user mtd_info;
                    //update kernel partition table
                    //system
                    new_mtd[0].size = (part_scatter[part_num - 2]->offset - part_scatter[part_num - 3]->offset);
                    new_mtd[0].offset = part_scatter[part_num - 3]->offset;
                    //cache
                    new_mtd[1].size = (part_scatter[part_num - 1]->offset - part_scatter[part_num - 2]->offset);
                    new_mtd[1].offset = part_scatter[part_num - 2]->offset;
                    //userdata
                        new_mtd[2].size = (total_size - part_scatter[part_num - 1]->offset);
                        new_mtd[2].offset = part_scatter[part_num - 1]->offset;

                    fp = fopen("/proc/driver/mtd_change", "w");
                    if (fp) {
                        if (fwrite(new_mtd, sizeof(new_mtd), 1, fp) != 1) {
                            ui->Print("Error: Write MTD information fail\n");
                            fclose(fp);
                            goto fail_out;
                        }
                        fclose(fp);
                    } else {
                        ui->Print("Error: Open MTD information fail\n");
                        goto fail_out;
                    }
                }
            } else {

                if (is_gpt) {
                    if (update_gpt(part_scatter, part_num)) {
                        ui->Print("Error: Update GPT fail\n");
                        goto fail_out;
                    }
                } else {
                    //tonykuo 2013-07-23
                    if (get_combo_emmc()) {
                        if (update_pmt_combo_emmc(part_scatter, part_num)) {
                            ui->Print("Error: Update PMT fail\n");
                            goto fail_out;
                        }
                    } else {
                        if (update_pmt_emmc(part_scatter, part_num)) {
                            ui->Print("Error: Update PMT fail\n");
                            goto fail_out;
                        }
                    }
                }

                if (phone_encrypted || isResizeFail) {
                    if (usrdata_changed) {
                        ui->Print("Warning: Due to phone is encrypted, erase all data.\n");
                        if (format_volume("/data") != 0) {
                            ui->Print("Error: Formatting data fail\n");
                            goto fail_out;
                        }
                    }
                    memset(backup_path, 0, PATH_MAX);
                }
            }

            if (path_is_data == 2) {
                if (access("/cache/recovery/command", F_OK) == 0) {
                    rename("/cache/recovery/command", "/tmp/command");
                }
                if (access("/cache/recovery/block.map", F_OK) == 0) {
                    rename("/cache/recovery/block.map", "/tmp/block.map");
                }
            }
            //format cache
            if (format_volume("/cache") != 0) {
                ui->Print("Error: Formatting cache fail\n");
                goto fail_out;
            }
            // mount cache and create recovery folder
            ensure_path_mounted("/cache");
            mkdir("/cache/recovery", 0770);

            if (path_is_data == 2) {
                if (access("/tmp/command", F_OK) == 0) {
                    rename("tmp/command", "/cache/recovery/command");
                }
                if (access("/tmp/block.map", F_OK) == 0) {
                    rename("/tmp/block.map", "/cache/recovery/block.map");
                }
            }
        }
    }
    /* Before checking partition size  will close the zip, so re-open the zip file */
     if(open_update_package(zip, zip_path, pMap,needs_mount) != INSTALL_SUCCESS)
        goto fail_out;

    unlink(fname);
    unlink(tname);

    for (i = 0; i < mtd_cnt; i++) {
        free(part_mtd[i]);
    }

    for (i = 0; i < part_num; i++) {
        free(part_scatter[i]);
    }


    return CHECK_OK;
prompt_out:
    retval = ERROR_CANNOT_BACKUP;
fail_out:

    unlink(fname);
    unlink(tname);

    for (i = 0; i < mtd_cnt; i++) {
        free(part_mtd[i]);
    }

    for (i = 0; i < part_num; i++) {
        free(part_scatter[i]);
    }

    return retval;
}
