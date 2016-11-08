#include <jni.h>
#include <JNIHelp.h>
#include <utils/Log.h>
#include <pthread.h>
#include <unistd.h>
#include <string.h>

#include <mal.h>
#include <entity/rds/rds_if.h>

#define LOG_TAG "WifiOffloadService"
#define LOG_NDEBUG 0

JavaVM* g_vm = 0;
jobject g_obj = 0;

void *g_conn_ptr = NULL;


int handover_callback(rds_ho_status_t *status)
{
    JNIEnv *env = 0;
    if (g_vm == NULL)
    {
        ALOGE("[handover_callback] NULL JVM");
        return 0;
    }
    if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK)
    {
        ALOGE("[handover_callback] AttachCurrentThread failure");
        return 0;
    }
    jclass clazz = (*env)->GetObjectClass(env, g_obj);
    if (clazz == NULL)
    {
        ALOGD("handover_callback() clazz is NULL");
        return 0;
    }
    jmethodID mid = (*env)->GetMethodID(env, clazz, "onHandover", "(II)V");
    int i = 0;
    if (mid != NULL)
    {
        (*env)->CallVoidMethod(env, g_obj, mid, status->ucho_status, status->etarget_ran_type);
    }
    (*g_vm)->DetachCurrentThread(g_vm);
    return 0;
}

void init(JNIEnv *env, jobject obj)
{
    (*env)->GetJavaVM(env, &g_vm);
    g_obj = (*env)->NewGlobalRef(env, obj);
    // rds_get_verno();
    rds_notify_funp_t rds_fp = {0};
    rds_fp.pfrds_ho_status_ind = handover_callback;
    g_conn_ptr = epdgs_rds_conn_init(&rds_fp);
}

jint get_rat_type(JNIEnv *env, jobject obj)
{
    if (g_conn_ptr == NULL)
    {
        ALOGE("get_rat_type conn_ptr is NULL");
        return RDS_RAN_UNSPEC;
    }
    rds_rb_get_demand_req_t req;
    // TODO: set req.u4pdn_id
    rds_rb_get_ran_type_rsp_t rsp = {.i4rds_ret = RDS_FALSE, .u4ran_type = RDS_RAN_UNSPEC};
    rds_get_ran_type(&req, &rsp, g_conn_ptr);
    return rsp.u4ran_type;
}

jobject get_disconnect_cause(JNIEnv *env, jobject obj)
{
    if (g_conn_ptr == NULL)
    {
        ALOGE("get_disconnect_cause conn_ptr is NULL");
        return NULL;
    }
    rds_rb_get_demand_req_t req;
    // TODO: set req.u4pdn_id
    rds_rb_get_last_err_rsp_t rsp = {.i4rds_ret = RDS_FALSE, .i4lasterr = 0, .i4lastsuberr = 0};
    rds_get_last_err(&req, &rsp, g_conn_ptr);
    // new DisconnectCause object
    jclass clazz = (*env)->FindClass(env, "com/mediatek/wfo/DisconnectCause");
    if (clazz == NULL)
    {
        ALOGE("get_disconnect_cause FindClass return NULL");
        return NULL;
    }
    jmethodID ctor = (*env)->GetMethodID(env, clazz, "<init>", "(II)V");
    if (ctor == NULL)
    {
        ALOGE("get_disconnect_cause GetMethodID return NULL");
        return NULL;
    }
    return (*env)->NewObject(env, clazz, ctor, rsp.i4lasterr, rsp.i4lastsuberr);
}

void set_wos_profile(JNIEnv *env, jobject obj, jboolean volte_enabled, jboolean wfc_enabled, jboolean wifi_enabled, jint wifi_mode)
{
    if (g_conn_ptr == NULL)
    {
        ALOGE("get_rat_type conn_ptr is NULL");
        return;
    }
    rds_ru_set_uiparam_req_t req = {
        .fgimsolte_en = volte_enabled,
        .fgwfc_en = wfc_enabled,
        .fgwifiui_en = wifi_enabled,
        .rdspolicy = wifi_mode
    };
    rds_set_ui_param(&req, g_conn_ptr);
}

void set_wifi_state(JNIEnv *env, jobject obj, jboolean wifi_connected, jstring if_name, jstring ipv4, jstring ipv6, jstring mac)
{
    if (g_conn_ptr == NULL)
    {
        ALOGE("set_wifi_state conn_ptr is NULL");
        return;
    }
    rds_ru_set_wifistat_req_t req = {
        .fgwifi_en = wifi_connected,
        .szwifi_ifname = {0},
        .szwifi_ipv4addr = {0},
        .szwifi_ipv6addr = {0},
        .szwifi_macaddr = {0},
    };

    const char *pcIfName = (*env)->GetStringUTFChars(env, if_name, 0);
    const char *pcIpv4 = (*env)->GetStringUTFChars(env, ipv4, 0);
    const char *pcIpv6 = (*env)->GetStringUTFChars(env, ipv6, 0);
    const char *pcMac = (*env)->GetStringUTFChars(env, mac, 0);
    strncpy(req.szwifi_ifname, pcIfName, RDS_STR_LEN - 1);
    strncpy(req.szwifi_ipv4addr, pcIpv4, 2 * RDS_STR_LEN - 1);
    strncpy(req.szwifi_ipv6addr, pcIpv6, 2 * RDS_STR_LEN - 1);
    strncpy(req.szwifi_macaddr, pcMac, 2 * RDS_STR_LEN - 1);
    ALOGD("set_wifi_state req.szwifi_ifname is %s\n", req.szwifi_ifname);
    ALOGD("set_wifi_state req.szwifi_ipv4addr is %s\n", req.szwifi_ipv4addr);
    ALOGD("set_wifi_state req.szwifi_ipv6addr is %s\n", req.szwifi_ipv6addr);
    ALOGD("set_wifi_state req.szwifi_macaddr is %s\n", req.szwifi_macaddr);
    rds_set_wifistat(&req, g_conn_ptr);
    (*env)->ReleaseStringUTFChars(env, if_name, pcIfName);
    (*env)->ReleaseStringUTFChars(env, ipv4, pcIpv4);
    (*env)->ReleaseStringUTFChars(env, ipv6, pcIpv6);
    (*env)->ReleaseStringUTFChars(env, mac, pcMac);
}


static JNINativeMethod method_table[] = {
    { "nativeInit", "()V", (void *)init },
    { "nativeGetRatType", "()I", (void *)get_rat_type },
    { "nativeGetDisconnectCause", "()Lcom/mediatek/wfo/DisconnectCause;", (void *)get_disconnect_cause },
    { "nativeSetWosProfile", "(ZZZI)V", (void *)set_wos_profile },
    { "nativeSetWifiStatus", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void *)set_wifi_state },
};

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_FALSE;
    }

    jniRegisterNativeMethods(env, "com/mediatek/wfo/impl/WifiOffloadService",
            method_table, NELEM(method_table));

    return JNI_VERSION_1_4;
}
