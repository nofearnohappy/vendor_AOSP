/*
 * Header files for UREE RPMB related functions.
 */

#ifndef __UREE_RPMB_H__
#define __UREE_RPMB_H__

#include "tz_cross/trustzone.h"

#ifdef __cplusplus
extern "C" {
#endif
TZ_RESULT UREE_RpmbKeyToProgram(UREE_SESSION_HANDLE session,
                                const unsigned char *key, int keylen);
TZ_RESULT UREE_RpmbInit(UREE_SESSION_HANDLE session,
                        unsigned char *cid, int cidlen);
TZ_RESULT UREE_RpmbGetWriteCounter(UREE_SESSION_HANDLE session,
                                   unsigned char *pkt, uint32_t size);
TZ_RESULT UREE_RpmbFeedbackWriteCounter(UREE_SESSION_HANDLE session,
                                        const unsigned char *pkt,
                                        uint32_t size);
TZ_RESULT UREE_RpmbRegWorkBuf(UREE_SESSION_HANDLE session,
                              UREE_SHAREDMEM_HANDLE pkt_buf_handle,
                              uint32_t pkt_buf_size);
TZ_RESULT UREE_RpmbWaitForCommand(UREE_SESSION_HANDLE session,
                                  uint32_t *cmd);
TZ_RESULT UREE_RpmbFeedbackResult(UREE_SESSION_HANDLE session,
                                  uint32_t cmd);
TZ_RESULT UREE_RpmbTestWriteRead(UREE_SESSION_HANDLE session,
                                 UREE_SHAREDMEM_HANDLE write_buf_handle,
                                 UREE_SHAREDMEM_HANDLE read_buf_handle,
                                 unsigned int size);
#ifdef __cplusplus
}
#endif

#endif /* __UREE_RPMB_H__ */
