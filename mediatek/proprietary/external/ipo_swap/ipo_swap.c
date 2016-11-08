#include <sys/types.h>
#include <sys/stat.h>
#include <sys/swap.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include "mtd/ubi-user.h"
#include <syslog.h>
#include <stdio.h>
#include <stdlib.h>


#define MAX_MTD_PARTITIONS 20

static struct {
    char name[16];
    int number;
} mtd_part_map[MAX_MTD_PARTITIONS];

static int mtd_part_count = -1;

//#define DEBUG

#ifdef DEBUG
#define LOG syslog
#else
#define LOG
#endif

static void find_mtd_partitions(void)
{
    int fd;
    char buf[1024];
    char *pmtdbufp;
    ssize_t pmtdsize;
    int r;

    fd = open("/proc/mtd", O_RDONLY);
    if (fd < 0)
        return;

    buf[sizeof(buf) - 1] = '\0';
    pmtdsize = read(fd, buf, sizeof(buf) - 1);
    pmtdbufp = buf;
    while (pmtdsize > 0) {
        int mtdnum, mtdsize, mtderasesize;
        char mtdname[16];
        mtdname[0] = '\0';
        mtdnum = -1;
        r = sscanf(pmtdbufp, "mtd%d: %x %x %15s",
                   &mtdnum, &mtdsize, &mtderasesize, mtdname);
        if ((r == 4) && (mtdname[0] == '"')) {
            char *x = strchr(mtdname + 1, '"');
            if (x) {
                *x = 0;
            }
            LOG(LOG_INFO,"mtd partition %d, %s", mtdnum, mtdname + 1);
            if (mtd_part_count < MAX_MTD_PARTITIONS) {
                strcpy(mtd_part_map[mtd_part_count].name, mtdname + 1);
                mtd_part_map[mtd_part_count].number = mtdnum;
                mtd_part_count++;
            } else {
                LOG(LOG_ERR, "too many mtd partitions");
            }
        }
        while (pmtdsize > 0 && *pmtdbufp != '\n') {
            pmtdbufp++;
            pmtdsize--;
        }
        if (pmtdsize > 0) {
            pmtdbufp++;
            pmtdsize--;
        }
    }
    close(fd);
}

int mtd_name_to_number(const char *name)
{
    int n;
    if (mtd_part_count < 0) {
        mtd_part_count = 0;
        find_mtd_partitions();
    }
    for (n = 0; n < mtd_part_count; n++) {
        if (!strcmp(name, mtd_part_map[n].name)) {
            return mtd_part_map[n].number;
        }
    }
    return -1;
}


int remap(void)
{
	struct ubi_map_req req;
	int ret,i, fd, ubi_vol_cdev;
	char path[255], tmp[255];
	int leb_cnt = 0;
	int mtd_num = mtd_name_to_number("userdata");
	int ubi_dev = -1;

	for(i=0;i<2;i++) {
		sprintf(path, "/sys/class/ubi/ubi%d/mtd_num", i);
		fd = open(path, O_RDONLY);
		read(fd, tmp, 255);
		close(fd);
		if(mtd_num == atoi(tmp)) {
			ubi_dev = i;
			break;
		}
	}

	for(i=0;i<3;i++) {
		sprintf(path, "/sys/class/ubi/ubi%d_%d/name", ubi_dev, i);
		fd = open(path, O_RDONLY);
		read(fd, tmp, 255);
		close(fd);
		if(memcmp(tmp, "ipoh", 4) == 0) {
			sprintf(path, "/sys/class/ubi/ubi%d_%d/reserved_ebs", ubi_dev, i);
			fd = open(path, O_RDONLY);
			read(fd, tmp, 255);
			close(fd);
			leb_cnt = atoi(tmp);
			break;
		}
	}
	sprintf(path, "/dev/ubi%d_%d", ubi_dev, i);
	LOG(LOG_INFO, "dev %s leb count %d", path, leb_cnt);

	ubi_vol_cdev = open(path , O_RDWR);
	if(ubi_vol_cdev == -1) {
		LOG(LOG_ERR, "open %s fail errno %d", path, errno);
		return -1;
	}
	for(i=0;i<leb_cnt;i++) {
		ret = ioctl(ubi_vol_cdev, UBI_IOCEBISMAP, &i);
		LOG(LOG_INFO, "LEN %d ismap %d errno %d", i, ret, errno);
		if(ret == 1) { 
			ret = ioctl(ubi_vol_cdev, UBI_IOCEBUNMAP, &i);
			if(ret != 0)
				LOG(LOG_ERR, "ioctl UBI_IOCEBUNMAP %d errno %d fail", i, errno);
		}
		req.lnum=i;
		ret = ioctl(ubi_vol_cdev, UBI_IOCEBMAP, &req);
		if(ret != 0)
			LOG(LOG_ERR, "ioctl UBI_IOCEBMAP %d errno %d fail", i, errno);
		ret = ioctl(ubi_vol_cdev, UBI_IOCEBISMAP, &i);
		LOG(LOG_INFO, "LEN %d ismap %d errno %d", i, ret, errno);
	}
	close(ubi_vol_cdev);
	return 0;
}

int main(int argc, char *argv[])
{
	int ret=0;
	char cmd[255];
	int mtd_num = mtd_name_to_number("ipoh");
	if(mtd_num == -1) {
        	LOG(LOG_ERR, "can't found ipoh parttion");
		return -1;
	}
#ifdef DEBUG
	openlog("ipo_swap", LOG_CONS, LOG_USER);
#endif
	remap();

	snprintf(cmd, 255, "mkswap /dev/block/mtdblock%d", mtd_num);
	system(cmd);
	
#ifdef DEBUG
	closelog();
#endif
	return ret;
}
