/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

#ifndef MT_COMMON_TK_H_
#define MT_COMMON_TK_H_

#include <sys/types.h>
#include <string.h>
#include "ui.h"

#define MAXPD 512

#define PACKED __attribute__ ((__packed__))
#define ALIGN1 __attribute__((aligned(1)))
#define ALIGN2 __attribute__((aligned(2)))

#define recovery_error(error) do {\
  ui->Print(("%s %s, %d, %d, %s\n",__FILE__, __FUNCTION__,__LINE__, error, strerror(error)));\
}while(0)

extern RecoveryUI *ui;

int open_or_warn(const char *pathname, int flags);
ssize_t safe_read(int fd, void *buf, size_t count);
void *mtk_malloc(size_t size);
void *xzalloc(size_t size);

void reset_total_files(void);
void add_total_files(void);
void set_total_files(int set_num);
void reset_count_files(void);
#endif
