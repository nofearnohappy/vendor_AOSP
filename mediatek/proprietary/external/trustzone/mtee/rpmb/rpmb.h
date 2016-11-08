#ifndef __RPMB_H__
#define __RPMB_H__
/* Standard MMC commands (4.1)           type  argument     response */
   /* class 1 */
#define MMC_GO_IDLE_STATE         0   /* bc                          */
#define MMC_SEND_OP_COND          1   /* bcr  [31:0] OCR         R3  */
#define MMC_ALL_SEND_CID          2   /* bcr                     R2  */
#define MMC_SET_RELATIVE_ADDR     3   /* ac   [31:16] RCA        R1  */
#define MMC_SET_DSR               4   /* bc   [31:16] RCA            */
#define MMC_SLEEP_AWAKE           5   /* ac   [31:16] RCA 15:flg R1b */
#define MMC_SWITCH                6   /* ac   [31:0] See below   R1b */
#define MMC_SELECT_CARD           7   /* ac   [31:16] RCA        R1  */
#define MMC_SEND_EXT_CSD          8   /* adtc                    R1  */
#define MMC_SEND_CSD              9   /* ac   [31:16] RCA        R2  */
#define MMC_SEND_CID             10   /* ac   [31:16] RCA        R2  */
#define MMC_READ_DAT_UNTIL_STOP  11   /* adtc [31:0] dadr        R1  */
#define MMC_STOP_TRANSMISSION    12   /* ac                      R1b */
#define MMC_SEND_STATUS          13   /* ac   [31:16] RCA        R1  */
#define MMC_BUS_TEST_R           14   /* adtc                    R1  */
#define MMC_GO_INACTIVE_STATE    15   /* ac   [31:16] RCA            */
#define MMC_BUS_TEST_W           19   /* adtc                    R1  */
#define MMC_SPI_READ_OCR         58   /* spi                  spi_R3 */
#define MMC_SPI_CRC_ON_OFF       59   /* spi  [0:0] flag      spi_R1 */

  /* class 2 */
#define MMC_SET_BLOCKLEN         16   /* ac   [31:0] block len   R1  */
#define MMC_READ_SINGLE_BLOCK    17   /* adtc [31:0] data addr   R1  */
#define MMC_READ_MULTIPLE_BLOCK  18   /* adtc [31:0] data addr   R1  */
#define MMC_SEND_TUNING_BLOCK    19   /* adtc                    R1  */
#define MMC_SEND_TUNING_BLOCK_HS200     21      /* adtc R1  */

  /* class 3 */
#define MMC_WRITE_DAT_UNTIL_STOP 20   /* adtc [31:0] data addr   R1  */

  /* class 4 */
#define MMC_SET_BLOCK_COUNT      23   /* adtc [31:0] data addr   R1  */
#define MMC_WRITE_BLOCK          24   /* adtc [31:0] data addr   R1  */
#define MMC_WRITE_MULTIPLE_BLOCK 25   /* adtc                    R1  */
#define MMC_PROGRAM_CID          26   /* adtc                    R1  */
#define MMC_PROGRAM_CSD          27   /* adtc                    R1  */

  /* class 6 */
#define MMC_SET_WRITE_PROT       28   /* ac   [31:0] data addr   R1b */
#define MMC_CLR_WRITE_PROT       29   /* ac   [31:0] data addr   R1b */
#define MMC_SEND_WRITE_PROT      30   /* adtc [31:0] wpdata addr R1  */

  /* class 5 */
#define MMC_ERASE_GROUP_START    35   /* ac   [31:0] data addr   R1  */
#define MMC_ERASE_GROUP_END      36   /* ac   [31:0] data addr   R1  */
#define MMC_ERASE                38   /* ac                      R1b */

  /* class 9 */
#define MMC_FAST_IO              39   /* ac   <Complex>          R4  */
#define MMC_GO_IRQ_STATE         40   /* bcr                     R5  */

  /* class 7 */
#define MMC_LOCK_UNLOCK          42   /* adtc                    R1b */

  /* class 8 */
#define MMC_APP_CMD              55   /* ac   [31:16] RCA        R1  */
#define MMC_GEN_CMD              56   /* adtc [0] RD/WR          R1  */

/* flags: expected response type */
#define MMC_RSP_PRESENT (1 << 0)
#define MMC_RSP_136     (1 << 1)                /* 136 bit response */
#define MMC_RSP_CRC     (1 << 2)                /* expect valid crc */
#define MMC_RSP_BUSY    (1 << 3)                /* card may send busy */
#define MMC_RSP_OPCODE  (1 << 4)                /* response contains opcode */

#define MMC_CMD_MASK    (3 << 5)                /* non-SPI command type */
#define MMC_CMD_AC      (0 << 5)
#define MMC_CMD_ADTC    (1 << 5)
#define MMC_CMD_BC      (2 << 5)
#define MMC_CMD_BCR     (3 << 5)

#define MMC_RSP_SPI_S1  (1 << 7)                /* one status byte */
#define MMC_RSP_SPI_S2  (1 << 8)                /* second byte */
#define MMC_RSP_SPI_B4  (1 << 9)                /* four data bytes */
#define MMC_RSP_SPI_BUSY (1 << 10)              /* card may send busy */

/*
 * These are the native response types, and correspond to valid bit
 * patterns of the above flags.  One additional valid pattern
 * is all zeros, which means we don't expect a response.
 */
#define MMC_RSP_NONE    (0)
#define MMC_RSP_R1      (MMC_RSP_PRESENT|MMC_RSP_CRC|MMC_RSP_OPCODE)
#define MMC_RSP_R1B     (MMC_RSP_PRESENT|MMC_RSP_CRC|MMC_RSP_OPCODE|MMC_RSP_BUSY)
#define MMC_RSP_R2      (MMC_RSP_PRESENT|MMC_RSP_136|MMC_RSP_CRC)
#define MMC_RSP_R3      (MMC_RSP_PRESENT)
#define MMC_RSP_R4      (MMC_RSP_PRESENT)
#define MMC_RSP_R5      (MMC_RSP_PRESENT|MMC_RSP_CRC|MMC_RSP_OPCODE)
#define MMC_RSP_R6      (MMC_RSP_PRESENT|MMC_RSP_CRC|MMC_RSP_OPCODE)
#define MMC_RSP_R7      (MMC_RSP_PRESENT|MMC_RSP_CRC|MMC_RSP_OPCODE)

/*
 * These are the SPI response types for MMC, SD, and SDIO cards.
 * Commands return R1, with maybe more info.  Zero is an error type;
 * callers must always provide the appropriate MMC_RSP_SPI_Rx flags.
 */
#define MMC_RSP_SPI_R1  (MMC_RSP_SPI_S1)
#define MMC_RSP_SPI_R1B (MMC_RSP_SPI_S1|MMC_RSP_SPI_BUSY)
#define MMC_RSP_SPI_R2  (MMC_RSP_SPI_S1|MMC_RSP_SPI_S2)
#define MMC_RSP_SPI_R3  (MMC_RSP_SPI_S1|MMC_RSP_SPI_B4)
#define MMC_RSP_SPI_R4  (MMC_RSP_SPI_S1|MMC_RSP_SPI_B4)
#define MMC_RSP_SPI_R5  (MMC_RSP_SPI_S1|MMC_RSP_SPI_S2)
#define MMC_RSP_SPI_R7  (MMC_RSP_SPI_S1|MMC_RSP_SPI_B4)

int EmmcRpmbProgramKey(int fd, rpmb_pkt_t *ps_rpmb_pkt, rpmb_pkt_t *ps_res_pkt);
int EmmcRpmbReadCounter(int fd, rpmb_pkt_t *ps_rpmb_pkt);
int EmmcRpmbAuthRead(int fd, rpmb_pkt_t *pread_rpmb_pkt, rpmb_pkt_t *result_pkt, unsigned int size);
int EmmcRpmbAuthWrite(int fd, rpmb_pkt_t *pwrite_rpmb_pkt, rpmb_pkt_t *result_pkt);

#endif
