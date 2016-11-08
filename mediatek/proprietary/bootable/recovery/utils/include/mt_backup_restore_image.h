/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
#ifndef MT_BACKUP_RESTORE_IMAGE_H_
#define MT_BACKUP_RESTORE_IMAGE_H_

#define DATA_UBI_DEVICE "/dev/ubi1_0"
#define RAW_BACKUP_BUF_MAX_SIZE     (16*1024*1024)
#define RAW_BACKUP_BUF_MIN_SIZE     (4*1024)
#define LARGE_FILE_THRESHOLD        (2047<<20)   // 2G - 1M

enum {
    ACTION_BACKUP = 0,
    ACTION_RESTORE,
};

int do_raw_backup_restore(const char *src, const char *dst, uint64_t total_size, int action);
int flush_nand(void);

#endif
