#include <stdlib.h>

#include "mtk_nfc_dynamic_load.h"
#include "com_mediatek_nfc_dta_dynamicload.h"

jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
   JNIEnv *e;

   ALOGD("MTK NFC Dynamic Load : loading mtknfc_dynamic_load_jni\n");

   // Check JNI version
   if(jvm->GetEnv((void **)&e, JNI_VERSION_1_6))
      return JNI_ERR;

   android::vm = jvm;

   if (android::register_com_mediatek_nfc_dta_dynamicload_NativeDynamicLoad(e) == -1)
      return JNI_ERR;

   return JNI_VERSION_1_6;
}

namespace android {

JavaVM *vm;

JavaVM * getJavaVM()
{
    return vm;
}
/*
 * JNI Utils
 */
JNIEnv *nfc_get_env()
{
    JNIEnv *e;
    if (vm->GetEnv((void **)&e, JNI_VERSION_1_6) != JNI_OK) {
        ALOGD("Current thread is not attached to VM");
        abort();
    }
    return e;
}
    
}

