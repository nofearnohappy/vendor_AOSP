/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <assert.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdlib.h>

#include <hardware/memtrack.h>

#include "memtrack_mtk.h"

#define LOG_TAG "memtrack_graphic"
#include <utils/Log.h>

char filename[256][256];
char *dir_path = "/d/ion/clients/";
int file_num = 0;

int travel_dir() {
    DIR *d;
    struct dirent *file;

    file_num = 0;
    memset(filename, 0x0, sizeof (filename));

    if (!(d = opendir(dir_path))) {
        ALOGE("open dir: %s.\n", dir_path);
        return -1;
    }

    while((file = readdir(d)) != NULL) {
      if (strncmp(file->d_name, ".", 1) ==0)
          continue;
      strcpy(filename[file_num++], file->d_name);
    }

    closedir(d);
    return 0;
}

int graphic_memtrack_get_memory(pid_t pid, enum memtrack_type type,
                             struct memtrack_record *records,
                             size_t *num_records)
{
    FILE *fp;
    char line[1024];
    char tmp[1024];
    int is_init = false;
    size_t accounted_size = 0;
    size_t unaccounted_size = 0;

    ALOGV("%s pid=%d, type=%d, num_records=%d\n", __func__, pid, type, *num_records);

    assert(type == MEMTRACK_TYPE_GRAPHICS);

#if 0
    snprintf(tmp, sizeof(tmp), "/proc/%d/cmdline", pid);
    fp = fopen(tmp, "r");
    if (fp != NULL) {
        if (fgets(line, sizeof(line), fp)) {
            if (strcmp(line, "/init") == 0)
                is_init = true;
        }
        fclose(fp);
    }
#endif

    if(*num_records == 0) {
        *num_records = 1;
        return 0;
    }

    travel_dir();

#if 0
    if (is_init) {
        snprintf(tmp, sizeof(tmp), "/sys/kernel/debug/ion/heaps/%s_total_in_pool", "ion_mm_heap");
        fp = fopen(tmp, "r");
        if (fp == NULL) {
      ALOGE("%s error to open /sys/kernel/debug/ion/heaps/%s_total_in_pool: %s\n", __func__, strerror(errno), "ion_mm_heap");
            return -errno;
        }

        while (1) {
            size_t size;
            int ret;

            if (fgets(line, sizeof(line), fp) == NULL) {
                break;
            }

            ret = sscanf(line, "%s %16zu\n", tmp, &size);
            if (ret == 2 && strcmp(tmp, "total_in_pool")==0 ) {
    unaccounted_size = size * 4 * 1024;
    ALOGI("%s match %d: %s :  %s %zu\n", __func__, ret, line, tmp, size);
            }
        }

        fclose(fp);

        int i;
        char disp_name[256][256];
        int len = 0;

        for (i = 0; i<file_num; i++) {
            snprintf(tmp, sizeof(tmp), filename[i]);
            strtok(tmp, "-");
            //if (strcmp(tmp, "disp_decouple") ==0 || strcmp(tmp, "display") == 0) {
              if (atoi(tmp) == 0) { //disp_decouple, or display from kernel
                strcpy(disp_name[len++], filename[i]);
            }
        }

        for (i = 0; i < len; i++) {
            snprintf(tmp, sizeof(tmp), "/d/ion/clients/%s", disp_name[i]);
            fp = fopen(tmp, "r");
            if (fp == NULL) {
          ALOGE("%s error to open /sys/kernel/debug/ion/clients/%d: %s\n", __func__, pid, strerror(errno));
                return -errno;
            }

            while (1) {
                pid_t handle_pid;
                size_t size;
                int ret;
                int handle_count;

                if (fgets(line, sizeof(line), fp) == NULL) {
                    break;
                }

                ret = sscanf(line, "%s %d %8zu %d\n", tmp, &handle_pid, &size, &handle_count);
                if (ret == 4 && strcmp(tmp, "ion_mm_heap")==0) {
         unaccounted_size += size / handle_count;
         ALOGI("%s match %d: %s : %d %d %zu %d %d\n", __func__, ret, line, pid, handle_pid, size, handle_count, unaccounted_size);
                }
            }
            fclose(fp);
        }

  if(*num_records > 0) {
      records[0].size_in_bytes = unaccounted_size;
    records[0].flags = MEMTRACK_FLAG_SMAPS_UNACCOUNTED|
                   MEMTRACK_FLAG_SHARED |
                   MEMTRACK_FLAG_SYSTEM |
                   MEMTRACK_FLAG_NONSECURE;
  }

  *num_records = 1;
    } else
#endif
    {
        int i;
        char pid_name[256][256];
        int len = 0;

        for (i = 0; i<file_num; i++) {
            snprintf(tmp, sizeof(tmp), filename[i]);
            strtok(tmp, "-");
            //if (strcmp(tmp, "disp_decouple") ==0 || strcmp(tmp, "display") == 0)
            if (atoi(tmp) == 0) //disp_decouple, or display from kernel
                continue;
            if (pid == atoi(tmp)) {
                strcpy(pid_name[len++], filename[i]);
            }
        }

        for (i = 0; i < len; i++) {
            snprintf(tmp, sizeof(tmp), "/d/ion/clients/%s", pid_name[i]);
            fp = fopen(tmp, "r");
            if (fp == NULL) {
                ALOGE("%s error to open /sys/kernel/debug/ion/clients/%d: %s\n",
                      __func__, pid, strerror(errno));
                return -errno;
            }
            while (1) {
                pid_t handle_pid;
                size_t size;
                int handle_count;
                int ret;
                int count = 0;

                if (fgets(line, sizeof(line), fp) == NULL) {
                    break;
                }

                ret = sscanf(line, "%s %d %8zu %d\n", tmp, &handle_pid, &size, &handle_count);
                if (ret == 4 && strcmp(tmp, "ion_mm_heap")==0) {
                    unaccounted_size += size / handle_count;
                    ALOGI("%s match %d: %s : %d %d %zu %d %d\n", __func__, ret, line,
                          pid, handle_pid, size, handle_count, unaccounted_size);
                }
            }
            fclose(fp);
        }

        if(*num_records > 0) {
            records[0].size_in_bytes = unaccounted_size;
            records[0].flags = MEMTRACK_FLAG_SMAPS_UNACCOUNTED|
                               MEMTRACK_FLAG_SHARED |
                               MEMTRACK_FLAG_SYSTEM |
                               MEMTRACK_FLAG_NONSECURE;
        }

        *num_records = 1;
    }
    return 0;
}

int graphic_memtrack_init()
{
    return 0;
}
