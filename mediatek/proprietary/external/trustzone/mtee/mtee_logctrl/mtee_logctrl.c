#include <stdio.h>
#include <stdlib.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <tz_cross/ta_logctrl.h>

int main(int argc, char *argv[]) {
    TZ_RESULT ret;
    MTEEC_PARAM param[4];
    UREE_SESSION_HANDLE ta_log_ctrl_sess;

    if (2 != argc) {
        printf("usage: mtee_logctrl [value]\n");
        printf("0  -> info\n");
        printf("1  -> debug\n");
        printf("2  -> printf\n");
        printf("3  -> warning\n");
        printf("4  -> bug\n");
        printf("5  -> assert\n");
        printf("15 -> disable\n");
        return 0;
    }

    ret = UREE_CreateSession(TZ_TA_LOG_CTRL_UUID, &ta_log_ctrl_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("mtee_logctrl: fail to creat session for mtee communication %s\n", TZ_GetErrorString(ret));
        return 0;
    }

    param[0].value.a = (uint32_t)atoi(argv[1]);
    ret = UREE_TeeServiceCall(ta_log_ctrl_sess, TZCMD_LOG_CTRL_SET_LVL, TZ_ParamTypes1(TZPT_VALUE_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("mtee_logctrl: fail to change mtee log level %s\n", TZ_GetErrorString(ret));
        return 0;
    }

    ret = UREE_CloseSession(ta_log_ctrl_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("mtee_logctrl: fail to close session for mtee communication %s\n", TZ_GetErrorString(ret));
        return 0;
    }

    printf("Succeed to set log level (%d) for mtee !!!\n", param[0].value.a);
    return 0;
}
