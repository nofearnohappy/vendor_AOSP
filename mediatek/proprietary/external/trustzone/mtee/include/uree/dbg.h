/*
 * Header files for UREE debug related functions.
 */

#ifndef __UREE_DBG_H__
#define __UREE_DBG_H__

#include "tz_cross/trustzone.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Secure memory/chunk memory information structure
*/
typedef struct {
    uint32_t used_byte;             // used size in bytes
    uint32_t total_byte;            // total size in bytes
    uint32_t max_free_cont_mem_sz;  // maximum free continuous memory size
} UREE_SECUREMEM_INFO;

/**
 * Secure memory get information
 *
 * Get information of secure mmeory.
 *
 * @param session    The session handle.
 * @param  info    The pointer of memory information
 * @param return    return code.
 */ 
TZ_RESULT UREE_GetSecurememinfo(UREE_SESSION_HANDLE session, UREE_SECUREMEM_INFO *info);


/**
 * Secure chunk memory get information
 *
 * Get information of secure chunk mmeory.
 *
 * @param session    The session handle.
 * @param  info    The pointer of memory information
 * @param return    return code.
 */ 
TZ_RESULT UREE_GetSecurechunkmeminfo(UREE_SESSION_HANDLE session, UREE_SECUREMEM_INFO *info);


#ifdef __cplusplus
}
#endif

#endif /* __UREE_DBG_H__ */
