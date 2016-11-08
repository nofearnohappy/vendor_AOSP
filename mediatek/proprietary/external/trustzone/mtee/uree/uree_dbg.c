
#include <stdio.h>
#include <uree/system.h>
#include <uree/dbg.h>
#include <tz_cross/ta_dbg.h>

/* #define DBG_UREE_DBG */

TZ_RESULT UREE_GetSecurememinfo(UREE_SESSION_HANDLE session,
                                UREE_SECUREMEM_INFO *info) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    if ((info == NULL)) {
        return TZ_RESULT_ERROR_BAD_PARAMETERS;
    }

    ret = UREE_TeeServiceCall(session, TZCMD_DBG_SECUREMEM_INFO,
            TZ_ParamTypes2(TZPT_VALUE_OUTPUT, TZPT_VALUE_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        printf("[uree] UREE_GetSecurememinfo Error: %d\n", ret);
        return ret;
    }

    info->used_byte = p[0].value.a;
    info->max_free_cont_mem_sz = p[0].value.b;
    info->total_byte = p[1].value.a;

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_GetSecurechunkmeminfo(UREE_SESSION_HANDLE session,
                                     UREE_SECUREMEM_INFO *info) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    if ((info == NULL)) {
        return TZ_RESULT_ERROR_BAD_PARAMETERS;
    }

    ret = UREE_TeeServiceCall(session, TZCMD_DBG_SECURECM_INFO,
            TZ_ParamTypes2(TZPT_VALUE_OUTPUT, TZPT_VALUE_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        printf("[uree] UREE_GetSecurechunkmeminfo Error: %d\n", ret);
        return ret;
    }

    info->used_byte = p[0].value.a;
    info->max_free_cont_mem_sz = p[0].value.b;
    info->total_byte = p[1].value.a;

    return TZ_RESULT_SUCCESS;
}




