
#include <stdio.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <tz_cross/ta_rpmb.h>
#include <tz_cross/ta_test.h>
#define LOG_TAG "rpmb"
#include <log/log.h>

TZ_RESULT UREE_RpmbKeyToProgram(UREE_SESSION_HANDLE session, char *key, int keylen) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].mem.buffer = key;
    p[0].mem.size = keylen;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_KEY_TO_PROGRAM,
            TZ_ParamTypes1(TZPT_MEM_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbKeyToProgram Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbInit(UREE_SESSION_HANDLE session, unsigned char *cid, int cidlen) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].mem.buffer = cid;
    p[0].mem.size = cidlen;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_INIT,
            TZ_ParamTypes1(TZPT_MEM_INPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbInit Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbGetWriteCounter(UREE_SESSION_HANDLE session, unsigned char *pkt_buf, uint32_t size) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].mem.buffer = pkt_buf;
    p[0].mem.size = size;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_GET_WRITECOUNTER,
            TZ_ParamTypes1(TZPT_MEM_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbGetWriteCounter Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbFeedbackWriteCounter(UREE_SESSION_HANDLE session, const unsigned char *pkt_buf, uint32_t size) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].mem.buffer = (unsigned char *)pkt_buf;
    p[0].mem.size = size;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_FEEDBACK_WRITECOUNTER,
            TZ_ParamTypes1(TZPT_MEM_INPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbFeedbackWriteCounter Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbRegWorkBuf(UREE_SESSION_HANDLE session,
                              UREE_SHAREDMEM_HANDLE pkt_buf_handle,
                              uint32_t pkt_buf_size) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].memref.handle = pkt_buf_handle;
    p[0].memref.offset = 0;
    p[0].memref.size = pkt_buf_size;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_REG_WORK_BUF,
            TZ_ParamTypes1(TZPT_MEMREF_INOUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbRegWorkBuf Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbWaitForCommand(UREE_SESSION_HANDLE session, uint32_t *cmd) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_WAIT_FOR_COMMAND,
            TZ_ParamTypes1(TZPT_VALUE_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbWaitForCommand Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }

    *cmd = p[0].value.a;

    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbFeedbackResult(UREE_SESSION_HANDLE session, uint32_t cmd) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].value.a = cmd;

    ret = UREE_TeeServiceCall(session, TZCMD_RPMB_FEEDBACK_RESULT,
            TZ_ParamTypes1(TZPT_VALUE_INPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbFeedbackResult Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }


    return TZ_RESULT_SUCCESS;
}

TZ_RESULT UREE_RpmbTestWriteRead(UREE_SESSION_HANDLE session,
                                 UREE_SHAREDMEM_HANDLE write_buf_handle,
                                 UREE_SHAREDMEM_HANDLE read_buf_handle,
                                 unsigned int size) {
    MTEEC_PARAM p[4];
    TZ_RESULT ret;

    p[0].memref.handle = write_buf_handle;
    p[0].memref.offset = 0;;
    p[0].memref.size = size;
    p[1].memref.handle = read_buf_handle;
    p[1].memref.offset = 0;;
    p[1].memref.size = size;

    ret = UREE_TeeServiceCall(session, TZCMD_TEST_RPMB,
            TZ_ParamTypes2(TZPT_MEMREF_INPUT, TZPT_MEMREF_OUTPUT), p);

    if (ret != TZ_RESULT_SUCCESS) {
        ALOGE("[uree] UREE_RpmbTestWriteRead Error: %d (%s)\n", ret, TZ_GetErrorString(ret));
        return ret;
    }


    return TZ_RESULT_SUCCESS;
}
