#ifndef __COM_MEDIATEK_NFC_DTA_H__
#define __COM_MEDIATEK_NFC_DTA_H__

#include <JNIHelp.h>
#include <jni.h>

#include <stdlib.h>
#include <stdio.h>
#include <getopt.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <pthread.h>
#include <ctype.h>


#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>
#undef LOG_TAG
#define LOG_TAG "DTA"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG,__VA_ARGS__)

#ifdef __cplusplus
} // extern c
#endif

struct nfc_jni_native_data
{
   int running;
   /* Our VM */
   JavaVM *vm;
   int env_version;
   /* Reference to the NFCManager instance */
   jobject manager;

};

namespace android {

extern JavaVM *vm;

int register_com_mediatek_nfc_dta_NativeDtaManager(JNIEnv *e);
JavaVM * getJavaVM();
JNIEnv *nfc_get_env();
void transferMessageToJava(char * message);
void switchTestState(void);

}

#endif  //__COM_MEDIATEK_NFC_DTA_H__

