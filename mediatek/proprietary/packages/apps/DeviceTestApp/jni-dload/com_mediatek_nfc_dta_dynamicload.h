#ifndef __COM_MEDIATEK_NFC_DYNAMIC_LOAD_H__
#define __COM_MEDIATEK_NFC_DYNAMIC_LOAD_H__

#include <JNIHelp.h>
#include <jni.h>

#include <pthread.h>
#include <sys/queue.h>


#ifdef __cplusplus
extern "C" {
#endif

#include "mtk_nfc_dynamic_load.h"

#ifdef __cplusplus
} // extern c
#endif

namespace android {

extern JavaVM *vm;

int register_com_mediatek_nfc_dta_dynamicload_NativeDynamicLoad(JNIEnv *e);
JavaVM * getJavaVM();
JNIEnv *nfc_get_env();

}

#endif

