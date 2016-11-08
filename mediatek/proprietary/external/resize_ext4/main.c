#include <utils/Log.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <linux/fs.h>
#include <linux/loop.h>
#include <private/android_filesystem_config.h>
#include <cutils/android_reboot.h>
#include <cutils/partition_utils.h>
#include <cutils/properties.h>
#include <logwrap/logwrap.h>
#include "ext2fs/ext2fs.h"
#include <cutils/klog.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "RESIZE_EXT4"
#define STATS_RESIZED 1
#define RESERVE_SIZE (1 * 1024 * 1024)
#define RESIZE_EXT4   "/system/bin/resize2fs"
#define KEY_IN_FOOTER  "footer"
#define ARRAY_SIZE(a) (sizeof(a) / sizeof(*(a)))

#define INFO(x...)    KLOG_INFO("resize", x)
#define ERROR(x...)   KLOG_ERROR("resize", x)
struct resize_stats
{
    int state;
    unsigned long long size;
};

static long long get_block_size(char *path);
static errcode_t adjust_fs_size(ext2_filsys fs, long long *new_size);
static int get_resize_stats(struct resize_stats *stats, char *key_loc, char *blk_device);

int main(int argc, char* agrv[])
{
    struct resize_stats stats = {0, 0};
    int status;
    int ret;
    char *blk_device;
    char *key_loc;

    if(argc != 3) {
        ERROR("The number of argument must be 3.\n");
        ret = -1;
        goto error;
    }
    
    blk_device = agrv[1];
    key_loc =  agrv[2];
    
    if ((ret = get_resize_stats(&stats, key_loc, blk_device)) < 0) {
        ERROR("Failed to get resize status of %s.\n", blk_device);
        goto error;
    }

    if (STATS_RESIZED == stats.state) {
        ERROR("Partition has been resized,so nothing to do!\n");
    }else {
        char *num_kb;
        char *resize_ext4_argv[] = {
                RESIZE_EXT4,
                "-f",
                blk_device,
                NULL
        };

        if (asprintf(&num_kb, "%lldK", (unsigned long long)(stats.size / 1024)) <= 0) {
            ERROR("failed to create command for %s\n", blk_device);
            ret = -1;
            goto error;
        }

        resize_ext4_argv[3] = num_kb;

        ERROR("Running %s on %s\n", RESIZE_EXT4, blk_device);

        ret = android_fork_execvp_ext(ARRAY_SIZE(resize_ext4_argv), resize_ext4_argv,
                &status, true, LOG_NONE,
                false, NULL);
        free(num_kb);
        if (ret < 0) {
            /* No need to check for error in fork, we can't really handle it now */
            ERROR("Failed trying to run %s\n", RESIZE_EXT4);
            goto error;
        }
    }
error:
    ERROR("Resize ext4 return %d\n", ret);
    return ret;
}

static long long get_block_size(char *path)
{
    int fd;
    int ret;
    unsigned long long block_size = 0;
    if ((fd = open(path, O_RDONLY)) < 0) {
        ERROR("Open block fail:%s.\n", (char*)strerror(errno));
        return -1;
    }

    ret = ioctl(fd, BLKGETSIZE64, &block_size);
    close(fd);

    if (ret)
        return -1;

    return block_size;
}

static errcode_t adjust_fs_size(ext2_filsys fs, long long *new_size)
{
    errcode_t	retval;
    int    overhead = 0;
    int    rem;

    fs->super->s_blocks_count = (unsigned int)(*new_size / fs->blocksize);

retry:
    fs->group_desc_count = ext2fs_div_ceil(fs->super->s_blocks_count -
			       fs->super->s_first_data_block, EXT2_BLOCKS_PER_GROUP(fs->super));
    if (fs->group_desc_count == 0)
        return EXT2_ET_TOOSMALL;
    fs->desc_blocks = ext2fs_div_ceil(fs->group_desc_count, EXT2_DESC_PER_BLOCK(fs->super));

    /*
    * Overhead is the number of bookkeeping blocks per group.  It
    * includes the superblock backup, the group descriptor
    * backups, the inode bitmap, the block bitmap, and the inode
    * table.
    */
    overhead = (int) (2 + fs->inode_blocks_per_group);

    if (ext2fs_bg_has_super(fs, fs->group_desc_count - 1))
        overhead += 1 + fs->desc_blocks + fs->super->s_reserved_gdt_blocks;

    /*
    * See if the last group is big enough to support the
    * necessary data structures.  If not, we need to get rid of
    * it.
    */
    rem = (fs->super->s_blocks_count - fs->super->s_first_data_block) % fs->super->s_blocks_per_group;
    if ((fs->group_desc_count == 1) && rem && (rem < overhead))
        return EXT2_ET_TOOSMALL;

    if (rem && (rem < overhead+50)) {
        fs->super->s_blocks_count -= rem;
        goto retry;
    }

    *new_size = ((long long)fs->super->s_blocks_count * (long long)fs->blocksize);
    return 0;
}

static int get_resize_stats(struct resize_stats *stats, char *key_loc, char *blk_device)
{
    errcode_t	retval;
    ext2_filsys	fs = NULL;
    ext2_filsys	dup_fs;
    io_manager	io_ptr;
    long long data_partition_size = 0;
    io_ptr = unix_io_manager;

    retval = ext2fs_open2(blk_device, NULL , 0, 0, 0, io_ptr, &fs);
    if (retval) {
        ERROR("Couldn't find valid filesystem superblock.\n");
	if (fs)
		free(fs);
        return -1;	
    }

    retval = ext2fs_dup_handle(fs, &dup_fs);
    if (retval) {
        ERROR("Couldn't duplicate filesys.\n");
        ext2fs_close(fs);
        return -1;	
    }

    if((data_partition_size = get_block_size(blk_device)) < 0) {
        ERROR("Get %s partition size fail.\n", blk_device);
        ext2fs_close(fs);
        ext2fs_free(dup_fs);
        return -1;
    }
    ERROR("Size for partition(%s) is %lluK.\n", blk_device, data_partition_size / 1024);
    ERROR("Size in superblock is %lluK.\n", (unsigned long long)fs->super->s_blocks_count * fs->blocksize / 1024);

    if (!strcmp(key_loc, KEY_IN_FOOTER)){
        data_partition_size -= RESERVE_SIZE;
        ERROR("There is key in footer of partition(%s).\n", blk_device);   
    }

    //adjust partition size to meet resizefs rule.
    //resizefs will adjust partition size in some case.
    retval = adjust_fs_size(dup_fs, &data_partition_size);
    if (retval) {
        ERROR("Couldn't adjust partition size.\n");
        ext2fs_close(fs);
        ext2fs_free(dup_fs);
        return -1;	
    }
    ERROR("Size will (maybe) resize to(after adjust) is %lluK.\n", data_partition_size / 1024);
    if((unsigned int)(data_partition_size / fs->blocksize) == (unsigned int)(fs->super->s_blocks_count)) {
        ERROR("The size of data already meet the request(size in superblock = size after adjust).\n");
        stats->state = STATS_RESIZED;
    }
    stats->size = data_partition_size;
    ext2fs_close(fs);
    ext2fs_free(dup_fs);
    return 0;
}
