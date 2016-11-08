/** Commands for TA RPMB **/

#ifndef __TRUSTZONE_TA_RPMB__
#define __TRUSTZONE_TA_RPMB__

#define TZ_TA_RPMB_UUID   "607b363c-5be2-4c38-9205-505ba6c8c7b2"

/* Command for RPMB */
#define TZCMD_RPMB_INIT                   0
#define TZCMD_RPMB_REG_WORK_BUF           1
#define TZCMD_RPMB_GET_WRITECOUNTER       2
#define TZCMD_RPMB_FEEDBACK_WRITECOUNTER  3
#define TZCMD_RPMB_WAIT_FOR_COMMAND       4
#define TZCMD_RPMB_FEEDBACK_RESULT        5
#define TZCMD_RPMB_KEY_TO_PROGRAM         0x1000

/* Early Command for RPMB */
#define TZCMD_RPMB_EARLY_AUTH_READ        0xea01
#define TZCMD_RPMB_EARLY_AUTH_WRITE       0xea02
#define TZCMD_RPMB_EARLY_MAC_TEST         0xea03

/* Normal world RPMB CMD */
#define NS_RPMB_OP_SHIFT (24)
#define NS_RPMB_SIZE_MASK ((1 << NS_RPMB_OP_SHIFT)-1)
#define NS_RPMB_OP_MASK   (~((1 << NS_RPMB_OP_SHIFT)-1))
#define RPMB_READ  (1 << NS_RPMB_OP_SHIFT)
#define RPMB_WRITE (2 << NS_RPMB_OP_SHIFT)
#define RPMB_EAGAIN (0xff << NS_RPMB_OP_SHIFT)

#define NS_RPMB_CMD_PACK(op, size) (op | size)
#define NS_RPMB_OP(cmd) ((cmd) & NS_RPMB_OP_MASK)
#define NS_RPMB_SIZE(cmd) ((cmd) & NS_RPMB_SIZE_MASK)

/* rpmb packet structure */
#define RPMB_NONCE_LENGTH (16)
#define RPMB_DATA_LENGTH (256)
#define RPMB_KEY_LENGTH  (32)
#define RPMB_SIGNATURE_LENGTH (RPMB_KEY_LENGTH)
#define RPMB_STUFF_BYTES (196)

typedef struct rpmb_pkt_s {
    unsigned short u2ReqResp;
    unsigned short u2Result;
    unsigned short u2BlockCount;
    unsigned short u2Address;
    unsigned int   u4WriteCounter;
    unsigned char  au1Nonce[RPMB_NONCE_LENGTH];
    unsigned char  au1Data[RPMB_DATA_LENGTH];
    unsigned char  au1KeyMac[RPMB_KEY_LENGTH];
    unsigned char  au1StuffBytes[RPMB_STUFF_BYTES];
} rpmb_pkt_t;

int TA_Rpmb_AuthRead(uint32_t offset, uint8_t *buf, uint32_t size, uint32_t wait);
int TA_Rpmb_AuthWrite(uint32_t offset, uint8_t *buf, uint32_t size, uint32_t wait);

#endif /* __TRUSTZONE_TA_RPMB__ */
