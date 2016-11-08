#include <errno.h>
#include <libgen.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/types.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdbool.h>

#include "mtdutils/mtdutils.h"
#include "common.h"
#include "bootimg.h"
#include "mt_partition.h"

#define PART_NAME_LEN 128
#define MTK_BOOT_SIG_LEN  256

static inline int get_boot_img_phy_size(boot_img_hdr *header){
  int kernel_page_cnt;
  int ramdisk_page_cnt;
  int second_page_cnt;
  off64_t partion_end_offset;

  kernel_page_cnt = (header->kernel_size+header->page_size-1)/header->page_size;
  ramdisk_page_cnt = (header->ramdisk_size+header->page_size-1)/header->page_size;
  second_page_cnt = (header->second_size+header->page_size-1)/header->page_size;
  /* plus 1 page count for boot image header itself */
  partion_end_offset = (off64_t)(kernel_page_cnt+ramdisk_page_cnt+second_page_cnt+1)*header->page_size;

  printf("kernel_page_cnt:%d ramdisk_page_cnt:%d second_page_cnt:%d partion_end_offset:0x%llx\n",
    kernel_page_cnt, ramdisk_page_cnt, second_page_cnt, partion_end_offset);

  return partion_end_offset;
}

int applysignature_buf(char *sigfile_buf, int sigfile_buf_size, const char *partition_name) {
    int result = 0;
    char part_dev_path[PART_NAME_LEN];
    char *dev_path;
    int storage_type;
    bool success;
    struct boot_img_hdr hdr;
    int partion_end_offset;

    storage_type = mt_get_phone_type();
    dev_path = get_partition_path(partition_name);
    if(EMMC_TYPE == storage_type){
    /* EMMC */
        int fd = open(dev_path, O_RDWR | O_SYNC);
        if (fd != -1) {
            while(read(fd, &hdr, sizeof(boot_img_hdr)) != sizeof(boot_img_hdr)){
                lseek64(fd, 0, SEEK_SET);
            }
            partion_end_offset = get_boot_img_phy_size(&hdr);

            /* move FD pointer to bootimg partition tail */
            if(lseek64(fd, partion_end_offset, SEEK_SET) != partion_end_offset) {
               printf("fail to lseek %d error is %s\n", partion_end_offset, strerror(errno));
               result = -1;
               close(fd);
               sync();
               goto done;
            }

            /* in case boot image is signed by boot signer , skip cert of boot signer */
            /* Read boot sig */
            #define BOOT_SIG_HDR_SZ 16
            #define ASN_ID_SEQUENCE  0x30
            unsigned char boot_sig_hdr[BOOT_SIG_HDR_SZ] = {0};

            if(read(fd, (unsigned char*)&boot_sig_hdr, BOOT_SIG_HDR_SZ) <= 0) {
               printf("Error: boot sig read error  is %s\n", strerror(errno));
            }

            if (boot_sig_hdr[0] == ASN_ID_SEQUENCE) {
               /* boot signature exists */
               unsigned boot_sig_offset = 0;
               unsigned len = 0;
               unsigned len_size = 0;
               unsigned boot_sig_size = 0;
               if (boot_sig_hdr[1] & 0x80) {
                  /* multi-byte length field */
                  unsigned int i = 0;
                  len_size = 1 + (boot_sig_hdr[1] & 0x7f);
                  for (i = 0; i < len_size - 1; i++) {
                      len = (len << 8) | boot_sig_hdr[2 + i];
                  }
               }
               else {
                        /* single-byte length field */
                        len_size = 1;
                        len = boot_sig_hdr[1];
               }

                boot_sig_size = 1 + len_size + len;
                partion_end_offset += boot_sig_size;
                //printf("Debug boot_sig_size = %d, partion_end_offset = %x\n",boot_sig_size,partion_end_offset);

            }

            /* move FD pointer to bootimg partition tail */
            if(lseek64(fd, partion_end_offset, SEEK_SET) != partion_end_offset) {
               printf("fail to lseek %d error is %s\n", partion_end_offset, strerror(errno));
               result = -1;
               close(fd);
               sync();
               goto done;
            }

            if (write(fd, sigfile_buf, sigfile_buf_size) == -1) {
                printf("fail to write %s\n", dev_path);
                result = -1;
            }
            close(fd);
            sync();
        } else {
            printf("open %s fail\n", dev_path);
            result = -1;
        }
    }
    else if(NAND_TYPE == storage_type){
        /* NAND */
        const MtdPartition* mtd;
        MtdReadContext *r_ctx;
        int read = sigfile_buf_size;
        int wrote = 0;
        int erase_offset = 0;
        size_t erase_size = 0;
        size_t page_buf_size = 0;
        char *page_buf = NULL;

        mtd_scan_partitions();
        mtd = mtd_find_partition_by_name(dev_path);
        if (mtd == NULL) {
            printf("%s: no mtd partition named \"%s\"\n", __func__, dev_path);
            result = -1;
            goto done;
        }
        /* Read boot.img header */
        r_ctx = mtd_read_partition(mtd);
        if(mtd_read_data(r_ctx, (char *)&hdr, sizeof(boot_img_hdr)) != sizeof(boot_img_hdr)){
            printf("%s: fail to read %s\n", __func__, dev_path);
            result = -1;
            mtd_read_close(r_ctx);
            goto done;
        }
        partion_end_offset = get_boot_img_phy_size(&hdr);
        mtd_read_close(r_ctx);

        MtdWriteContext* ctx = mtd_write_partition(mtd);
        if (ctx == NULL) {
            printf("%s: can't write mtd partition \"%s\"\n",
                    __func__, dev_path);
            result = -1;
            goto done;
        }
        success = true;

        // if sig offset is not a page start, just read back last page and wirte whole page with appeneding sig
        mtd_partition_info(mtd, NULL, &erase_size, NULL);

        if(erase_size == 0) {
           printf("mtd_partition_info get erase_size fail\n");
           result = -1;
           mtd_write_close(ctx);
           goto done;
        }

        erase_offset = (partion_end_offset / erase_size) * erase_size;
        printf("erase_offset: %d(erase_size: %zu), partion_end_offset: %d\n", erase_offset, erase_size, partion_end_offset);
        if (erase_offset != partion_end_offset) {
            page_buf_size = erase_size + sigfile_buf_size;
            page_buf = malloc(page_buf_size);
            memset(page_buf, 0, page_buf_size);
            // read back last page
            r_ctx = mtd_read_partition(mtd);
            int rsize = mtd_read_data_ex(r_ctx, page_buf, partion_end_offset-erase_offset, erase_offset);
            if(rsize != partion_end_offset-erase_offset){
                printf("%s: fail to read %s, size:%d, off:%d, readback:%d\n", __func__, dev_path, partion_end_offset-erase_offset, partion_end_offset, rsize);
                result = -1;
                mtd_read_close(r_ctx);
                mtd_write_close(ctx);
                free(page_buf);
                goto done;
            }
            memcpy(page_buf+rsize, sigfile_buf, sigfile_buf_size);
            mtd_read_close(r_ctx);
            read = rsize + sigfile_buf_size;
            wrote = mtd_write_data_ex(ctx, page_buf, read, erase_offset);
            free(page_buf);
        } else {
            wrote = mtd_write_data_ex(ctx, sigfile_buf, read, partion_end_offset);
        }

        success = success && (wrote == read);

        if (!success) {
            printf("mtd_write_data to %s failed: %s\n",
                    dev_path, strerror(errno));
        }

        if (mtd_erase_blocks(ctx, -1) == (off64_t)-1) {
            printf("%s: error erasing blocks of %s\n", __func__, dev_path);
        }
        if (mtd_write_close(ctx) != 0) {
            printf("%s: error closing write of %s\n", __func__, dev_path);
        }

        printf("%s %s partition\n",
                success ? "wrote" : "failed to write", dev_path);
        if(success)
            sync();
#if 0   // for debug, flash tool can not readback partition data of nand device
        char buf[256];
        r_ctx = mtd_read_partition(mtd);
        int read_size = mtd_read_data_ex(r_ctx, buf, sizeof(buf), partion_end_offset);
        printf("read size = %d\n", read_size);
        int i = 0;
        for (i = 0; i < sizeof(buf); i++) {
            if (i && (i % 16 == 0))
                printf("\n");
            printf("%02x ", buf[i]);
        }
        printf("\nend\n");
        mtd_read_close(r_ctx);
#endif
        result = success ? 0 : -1;
    }
done:
    free(dev_path);
    return result;
}

int applysignature(const char *filename, const char *partition_name){
  char *rbuf = NULL;
  int result = 0;
  int in_fd;
  struct stat f_buf;

  in_fd = open(filename, O_RDONLY);
  if (in_fd != -1) {
    if(fstat(in_fd, &f_buf) == -1) {
      printf("fstat fail in applysignature error is %s\n",strerror(errno));
      close(in_fd);
      return -1;
    }
    rbuf = malloc(f_buf.st_size);
    if(rbuf == NULL) {
      printf("Fail to malloc in applysignature\n");
      close(in_fd);
      return -1;
    }
    while(read(in_fd, rbuf, f_buf.st_size) != f_buf.st_size){
      lseek64(in_fd, 0, SEEK_SET);
    }
    result = applysignature_buf(rbuf, f_buf.st_size, partition_name);
  }
  else{
    printf("Fail to open %s, %s", filename, strerror(errno));
    result = -1;
  }

  if(in_fd>=0) close(in_fd);
  free(rbuf);
  return result;
}
