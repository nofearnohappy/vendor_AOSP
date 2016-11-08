/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include "mt_common_tk.h"
#include "roots.h"
#include "bootloader.h"
#include "mt_pmt.h"
#include "install.h"

part_info_t part_scatter_dup[MAX_PARTITION_NUM];


static int get_pmt_emmc(pt_resident_emmc *pt)
{
    int fd,ret;

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_READ, pt);
        if ( ret != 0) {
            printf("Error: function: %s line: %d ioctl PMT_READ /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            close(fd);
            return 1;
        }
        close(fd);
    } else {
        printf("Error: function: %s line: %d open /dev/pmt fail\n",__FUNCTION__,__LINE__);
        return 1;
    }
    return 0;
}

static int get_pmt_combo_emmc(pt_resident_combo_emmc *pt)
{
    int fd,ret;

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_READ, pt);
        if (ret != 0) {
            printf("Error: function: %s line: %d ioctl PMT_READ /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            close(fd);
            return 1;
        }
        close(fd);
    } else {
        printf("Error: function: %s line: %d open /dev/pmt fail\n",__FUNCTION__,__LINE__);
        return 1;
    }
    return 0;
}

int update_pmt_combo_emmc(part_info_t *part_scatter[], int part_num)
{
    struct DM_PARTITION_PACKET_x dm;
    unsigned long long preloader_offset = 0;

    int fd;
    int i;
    pt_resident_combo_emmc pt_emmc[PART_MAX_COUNT];
    memset(pt_emmc, 0, sizeof(pt_emmc));

    if (get_pmt_combo_emmc(pt_emmc) != 0) {
        printf("Error: function: %s line: %d get_pmt_combo_emmc fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    memset(&dm, 0, sizeof(dm));

    printf("Updating Combo eMMC PMT...\n");
    for (i = 0; i < part_num; i++) {
        snprintf(dm.part_info[i].part_name, sizeof(dm.part_info[i].part_name), "%s", pt_emmc[i].name);
        //Set offset = 0 for preloader and physical start address otherwise
        if (!strcmp(part_scatter[i]->name, "PRELOADER")) {
            dm.part_info[i].start_addr = preloader_offset;
        } else {
            dm.part_info[i].start_addr = part_scatter[i]->offset;
        }
        printf("partition offset = %llx ...\n", dm.part_info[i].start_addr);

        if (i == (part_num - 1)) {
            if (!strcmp((char const*)pt_emmc[i].name, "FAT")) {
                dm.part_info[i].part_len = 0;
            } else {
                dm.part_info[i].part_len = pt_emmc[i].size + (pt_emmc[i].offset - part_scatter[i]->offset);
            }
        } else {
            //Set preloader length to the value recorded in scatter.txt (part_size)
            if (!strcmp(part_scatter[i]->name, "PRELOADER")) {
                dm.part_info[i].part_len = part_scatter[i]->offset;
            } else {
                dm.part_info[i].part_len = part_scatter[i + 1]->offset - part_scatter[i]->offset;
            }
            printf("partition length = %llx ...\n", dm.part_info[i].part_len);
        }
        if (dm.part_info[i].part_name[0] == '_') {
            dm.part_info[i].visible = 0;
        } else {
            dm.part_info[i].visible = 1;
        }
        dm.part_info[i].dl_selected = 1;

        if (!strcmp(part_scatter[i]->name, "PRELOADER")) {
            dm.part_info[i].part_id = EMMC_PART_BOOT1;  //EMMC_BOOT_1
        } else {
            dm.part_info[i].part_id = EMMC_PART_USER;  //EMMC_USER
        }
    }

    for (i = part_num; i < PART_MAX_COUNT; i++) {
        if (strlen((char const*)pt_emmc[i].name)) {
            snprintf(dm.part_info[i].part_name, sizeof(dm.part_info[i].part_name), "%s", pt_emmc[i].name);
            dm.part_info[i].start_addr = pt_emmc[i].offset;
            dm.part_info[i].part_len = pt_emmc[i].size;
            dm.part_info[i].visible = 0;
            dm.part_info[i].dl_selected = 1;
        }
    }

    fd = open("/proc/sd_upgrade", O_RDWR);
    if (fd != -1) {
        if (write(fd, &dm, sizeof(dm)) != sizeof(dm)) {
            close(fd);
            ui->Print("Error: function: %s line: %d Write PMT fail\n",__FUNCTION__,__LINE__);
            return 1;
        }
        close(fd);
    } else {
        ui->Print("Error: function: %s line: %d Open PMT fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    return 0;
}

int update_pmt_emmc(part_info_t *part_scatter[], int part_num)
{
    DM_PARTITION_INFO_PACKET_x dm;
    int fd;
    int i;

    pt_resident_emmc pt_emmc[PART_MAX_COUNT];
    memset(pt_emmc, 0, sizeof(pt_emmc));

    if (get_pmt_emmc(pt_emmc) != 0) {
        printf("Error: function: %s line: %d get_pmt_emmc fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    memset(&dm, 0, sizeof(dm));

    for (i = 0; i < part_num; i++) {
        snprintf(dm.part_info[i].part_name, sizeof(dm.part_info[i].part_name), "%s", pt_emmc[i].name);
        dm.part_info[i].start_addr = part_scatter[i]->offset;
        if (i == (part_num - 1)) {
            if (!strcmp((char const*)pt_emmc[i].name, "FAT")) {
                dm.part_info[i].part_len = 0;
            } else {
                dm.part_info[i].part_len = pt_emmc[i].size + (pt_emmc[i].offset - part_scatter[i]->offset);
            }
        } else {
            dm.part_info[i].part_len = part_scatter[i + 1]->offset - part_scatter[i]->offset;
        }
        if (dm.part_info[i].part_name[0] == '_') {
            dm.part_info[i].part_visibility = 0;
        } else {
            dm.part_info[i].part_visibility = 1;
        }
        dm.part_info[i].dl_selected = 1;
    }

    for (i = part_num; i < PART_MAX_COUNT; i++) {
        if (strlen((char const*)pt_emmc[i].name)) {
            snprintf(dm.part_info[i].part_name, sizeof(dm.part_info[i].part_name), "%s", pt_emmc[i].name);
            dm.part_info[i].start_addr = pt_emmc[i].offset;
            dm.part_info[i].part_len = pt_emmc[i].size;
            dm.part_info[i].part_visibility = 0;
            dm.part_info[i].dl_selected = 1;
        }
    }

    fd = open("/proc/sd_upgrade", O_RDWR);
    if (fd != -1) {
        if (write(fd, &dm, sizeof(dm)) != sizeof(dm)) {
            close(fd);
            ui->Print("Error: function: %s line: %d Write PMT fail\n",__FUNCTION__,__LINE__);
            return 1;
        }
        close(fd);
    } else {
        ui->Print("Error: function: %s line: %d Open PMT fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    return 0;
}

int get_pmt_nand(pt_resident_nand *pt)
{
    int fd,ret;

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_READ, pt);
        if (ret != 0) {
            close(fd);
            printf("Error: function: %s line: %d ioctl PMT_READ /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            return 1;
        }
        close(fd);
    } else {
        printf("Error: function: %s line: %d open /dev/pmt fail\n",__FUNCTION__,__LINE__);
        return 1;
    }
    return 0;
}


static int get_pmt_nand_32(pt_resident_nand_32 *pt)
{
    int fd,ret;

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_READ, pt);
        if (ret != 0) {
            printf("Error: function: %s line: %d ioctl PMT_READ /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            close(fd);
            return 1;
        }
        close(fd);
    } else {
        printf("Error: function: %s line: %d open /dev/pmt fail\n",__FUNCTION__,__LINE__);
        return 1;
    }
    return 0;
}

int get_pmt_version(void)
{
    //ret 1 = new version
    int fd,error;
    int ret = 0;
    unsigned int sig;

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        error = ioctl(fd, PMT_VERSION, &sig);
        if (error != 0) {
            printf("old PMT_VERSION\n");
            printf("Error: function: %s line: %d ioctl PMT_VERSION /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,error,strerror(errno));
        } else {
            if ((sig == PT_SIG) || (sig == PT_SIG_v3)) {
                ret = 1;
            }
        }
        close(fd);
    } else {
        printf("Error: function: %s line: %d open /dev/pmt fail\n",__FUNCTION__,__LINE__);
    }
    return ret;
}

static int update_pmt_32(void)
{
    FILE *fp;
    char buf[128];
    int i = 0;
    int fd,ret;
    pt_resident_nand_32 pt_nand[PART_MAX_COUNT];
    memset(pt_nand, 0, sizeof(pt_nand));

    if (get_pmt_nand_32(pt_nand) != 0) {
        printf("Error: function: %s line: %d get_pmt_nand fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    DM_PARTITION_INFO_PACKET dm;

    fp = fopen("/proc/mtd", "r");
    if (fp) {
        char m_name[32];
        char p_name[32];
        unsigned int size, esize;
        unsigned int start_addr = 0;

        memset(&dm, 0, sizeof(dm));

        while (fgets(buf, sizeof(buf), fp)) {
            if (strlen(buf) && (strstr(buf, "mtd") == buf)) {
                if (sscanf(buf, "%s %x %x \"%s", m_name, &size, &esize, p_name) == 4) {
                    p_name[strlen(p_name) - 1] = 0;
                    sprintf(dm.part_info[i].part_name, "%s", pt_nand[i].name);
                    dm.part_info[i].start_addr = start_addr;
                    dm.part_info[i].part_len = size;
                    dm.part_info[i].part_visibility = 1;
                    dm.part_info[i].dl_selected = 1;
                    start_addr += size;
                    i++;
                }
            } else {
                continue;
            }
        }

        if (i != 0) {
            for (; i < PART_MAX_COUNT; i++) {
                if (pt_nand[i].size == 0xffffffff) {
                    break;
                }
                sprintf(dm.part_info[i].part_name, "%s", pt_nand[i].name);
                dm.part_info[i].start_addr = pt_nand[i].offset;
                dm.part_info[i].part_len = pt_nand[i].size;
                dm.part_info[i].dl_selected = 0;
            }
        }

        dm.part_num = i;
        fclose(fp);
    } else {
        ui->Print("Error: function: %s line: %d Open MTD fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_WRITE, &dm);
        if (ret != 0) {
            close(fd);
            ui->Print("Error: function: %s line: %d ioctl PMT_WRITE /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            return 1;
        }
        close(fd);
    } else {
        ui->Print("Error: function: %s line: %d Open PMT fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    return 0;
}

void copy_part_scatter_dup(part_info_t *part_scatter[], int mtd_cnt)
{
    int i = 0;
    memcpy(part_scatter_dup, part_scatter, sizeof(part_info_t *)*MAX_PARTITION_NUM);
    for (i = 0; i < mtd_cnt; i++) {
         memcpy(&part_scatter_dup[i], part_scatter[i], sizeof(part_info_t));
         //printf("part_scatter_dup table:\n");
         //printf("%s %llx\n",part_scatter_dup[i].name, part_scatter_dup[i].offset);
    }
}

static int update_pmt(void)
{
    FILE *fp;
    char buf[128];
    int i = 0;
    int fd,ret;
    pt_resident_nand pt_nand[PART_MAX_COUNT];
    memset(pt_nand, 0, sizeof(pt_nand));

    if (get_pmt_nand(pt_nand) != 0) {
        printf("Error: function: %s line: %d get_pmt_nand fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    DM_PARTITION_INFO_PACKET_64 dm;

    fp = fopen("/proc/mtd", "r");
    if (fp) {
        char m_name[32];
        char p_name[32];
        unsigned long long size, esize;
        unsigned long long start_addr = 0;

        memset(&dm, 0, sizeof(dm));

        while (fgets(buf, sizeof(buf), fp)) {
            if (strlen(buf) && (strstr(buf, "mtd") == buf)) {
                if (sscanf(buf, "%s %llx %llx \"%s", m_name, &size, &esize, p_name) == 4) {
                    p_name[strlen(p_name) - 1] = 0;
                    if (!strcasecmp((const char*)pt_nand[i].name, "bmtpool"))
                        break;
                    sprintf(dm.part_info[i].part_name, "%s", pt_nand[i].name);
                    if (get_MLC_case()) { //tonykuo 2014-01-17
                        dm.part_info[i].start_addr = part_scatter_dup[i].offset;
                    } else {
                        dm.part_info[i].start_addr = start_addr;
                    }

                    dm.part_info[i].part_len = size;
                    dm.part_info[i].part_visibility = 1;
                    dm.part_info[i].dl_selected = 1;

                    if (!get_MLC_case()) { //tonykuo 2014-01-17
                        start_addr += size;
                    }

                    i++;
                }
            } else {
                continue;
            }
        }

        if (i != 0) {
            for (; i < PART_MAX_COUNT; i++) {
                if (pt_nand[i].size == 0xffffffffffffffff) {
                    break;
                }
                sprintf(dm.part_info[i].part_name, "%s", pt_nand[i].name);
                dm.part_info[i].start_addr = pt_nand[i].offset;
                dm.part_info[i].part_len = pt_nand[i].size;
                dm.part_info[i].dl_selected = 0;
            }
        }

        dm.part_num = i;
        fclose(fp);
    } else {
        ui->Print("Error: function: %s line: %d Open MTD fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    fd = open("/dev/pmt", O_RDWR);
    if (fd != -1) {
        ret = ioctl(fd, PMT_WRITE, &dm);
        if (ret != 0) {
            close(fd);
            printf("Error: function: %s line: %d ioctl PMT_WRITE /dev/pmt fail error = %d (%s)\n"
            ,__FUNCTION__,__LINE__,ret,strerror(errno));
            return 1;
        }
        close(fd);
    } else {
        ui->Print("Error: function: %s line: %d Open PMT fail\n",__FUNCTION__,__LINE__);
        return 1;
    }

    return 0;
}

int update_pmt_nand(void) {
    if (!get_emmc_phone() && get_part_size_changed()) {
        //tonykuo 2014-01-17
        int ret;
        if (get_pmt_version()) {
            ret = update_pmt();
        } else {
            ret = update_pmt_32();
        }
        if (ret) {
            chdir("/");
            ensure_path_unmounted("/data");

            return INSTALL_ERROR;
        }
    }
    return INSTALL_SUCCESS;
}
