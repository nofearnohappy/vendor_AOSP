/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

#include <sys/types.h>
#include <sys/stat.h>
#include <setjmp.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include "mt_common_tk.h"

int total_files;
int count_files;

void reset_count_files(void)
{
   count_files = 0;
}

void reset_total_files(void)
{
   total_files = 0;
}

void add_total_files(void)
{
   total_files++;
}

void set_total_files(int set_num)
{
   total_files = set_num;
}

int open_or_warn(const char *pathname, int flags)
{
    int ret;

    ret = open(pathname, flags, 0666);

    if (ret < 0) {
        ui->Print("Error: can't open '%s'\n", pathname);
    }
    return ret;
}

ssize_t safe_read(int fd, void *buf, size_t count)
{
    ssize_t n;

    do {
        n = read(fd, buf, count);
    } while (n < 0 && errno == EINTR);

    return n;
}

void *mtk_malloc(size_t size)
{
    void *ptr = malloc(size);
    if (ptr == NULL && size != 0) {
        ui->Print("Error: function:%s line:%d Out of memory\n",__FUNCTION__,__LINE__);
    }
    return ptr;
}

void *xzalloc(size_t size)
{
    void *ptr = mtk_malloc(size);
    memset(ptr, 0, size);
    return ptr;
}
