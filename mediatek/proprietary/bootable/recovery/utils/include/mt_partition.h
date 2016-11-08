/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
#ifndef MT_PARTITION_H_
#define MT_PARTITION_H_

#include <stdint.h>

#define MAX_PARTITION_NUM (33)
#define PTENT_BUF_SIZE 64
#define NAND_TYPE    0
#define EMMC_TYPE    1

#define BLOCK_DEV_PATH "/sys/class/block/mmcblk0p"
#define PARTITION_BLOCK_SIZE 512

typedef struct part_info_t {
    uint64_t offset;
    char name[32];
} part_info_t;

typedef struct part_detail_info {
    uint64_t part_start_addr;
    uint64_t part_size;
    int part_index;
    char part_name[32];
} part_detail_info;

int get_partition_index_by_name(part_info_t *part_scatter[], int part_num, const char *partition_name);
int force_upgrade(bool is_gpt, part_info_t *part_scatter[], part_info_t *part_mtd[]  ,int part_num);

int get_combo_emmc(void);
int get_emmc_phone(void);
int get_has_fat(void);
int get_MLC_case(void);
void set_MLC_case(int value);

uint64_t hex2ulong(char *x);

void set_phone_expdb_succeed(int value);
int get_phone_expdb_succeed(void);

void set_scatter_expdb_succeed(int value);
int get_scatter_expdb_succeed(void);

void get_partition_blk_dev(const char *name, char *blk);
uint64_t get_partition_size(const char *name);

int get_partition_info(part_info_t *part_mtd[], int *part_num, uint64_t *total_size);
int get_partition_info_mlc(part_info_t *part_mtd[], int *part_num, uint64_t *total_size);

int parse_partition_info(const char *p_name, part_detail_info *part_info);
int parse_partition_info_by_index(int part_index, part_detail_info *part_info);

bool check_partition_layout(part_info_t *part_scatter[], int part_num, bool is_gpt);
#ifdef __cplusplus
extern "C" {
#endif
void mt_init_partition_type(void);
int mt_get_phone_type(void);
bool mt_is_support_gpt(void);
char* get_partition_path(const char *partition);
#ifdef __cplusplus
}
#endif
#endif
