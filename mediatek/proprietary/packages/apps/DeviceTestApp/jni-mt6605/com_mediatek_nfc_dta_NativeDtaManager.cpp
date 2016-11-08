
#include <stdint.h>

#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

///*** DTA JNI Version ****
#define DTA_VERSION  '1','5','0','1','2','0','0','1'
///*********************

static jmethodID com_mediatek_nfc_dta_notifyMessageListener;
static jmethodID com_mediatek_nfc_dta_switchTestState;

namespace android {

struct nfc_jni_native_data *nat;

static jint com_mediatek_nfc_dta_DtaManager_initialize(JNIEnv *env, jobject thiz)
{
    int retVal = 0;
    //struct nfc_jni_native_data *nat = NULL;
    jclass cls;
    jobject obj;
    jfieldID f;

    LOGD("%s:", __FUNCTION__);

    if( DtaInit() < 0)
    {
        retVal = -1;
        goto end;
    }

    DtaSetDeviceDtaMode(1);

    nat = (nfc_jni_native_data*)malloc(sizeof(struct nfc_jni_native_data));
    if(nat == NULL)
    {
       LOGD("malloc of nfc_jni_native_data failed");
       return -1;
    }

    memset(nat, 0, sizeof(*nat));
    env->GetJavaVM(&(nat->vm));
    nat->env_version = env->GetVersion();
    nat->manager = env->NewGlobalRef(thiz);

    cls = env->GetObjectClass(thiz);
    f = env->GetFieldID(cls, "mNative", "I");
    env->SetIntField(thiz, f, (intptr_t)nat);

    /* Initialize native cached references */
     com_mediatek_nfc_dta_notifyMessageListener = env->GetMethodID(cls, "notifyMessageListener",
                     "()V");
     com_mediatek_nfc_dta_switchTestState = env->GetMethodID(cls, "switchTestState",
                     "()V");

end:

    LOGD("%s: Result(%d)", __FUNCTION__, retVal);
    return retVal;

}

static jint com_mediatek_nfc_dta_DtaManager_deinitialize(JNIEnv *env, jobject thiz)
{
    int retVal = 0;

    LOGD("%s:", __FUNCTION__);

    DtaSetDeviceDtaMode(0);

    DtaDeinit();
end:
    return retVal;

}


static jint com_mediatek_nfc_dta_DtaManager_enableDiscovery(JNIEnv *env, jobject thiz, jint patternNumber, jint testType)
{
    int retVal = -1;

    //Jni
    jfieldID fid = NULL;
    jclass nativeClass = NULL;

    //value
    int swio;
    int uidLevel;
    int didSupport;
    int fsci;

    LOGD("%s: pattern number(0x%04X), test type(0x%04X)", __FUNCTION__, patternNumber, testType);

    nativeClass = env->GetObjectClass(thiz);

    ///swio
    fid = env->GetFieldID(nativeClass, "mListenSwio", "I");
    swio = env->GetIntField(thiz , fid);
    LOGD("mListenSwio = %d", swio);

    ///uid level
    fid = env->GetFieldID(nativeClass, "mListenUidLevel", "I");
    uidLevel = env->GetIntField(thiz , fid);
    LOGD("mListenUidLevel = %d", uidLevel);

    ///did support
    fid = env->GetFieldID(nativeClass, "mListenDidSupport", "I");
    didSupport = env->GetIntField(thiz , fid);
    LOGD("mListenDidSupport = %d", didSupport);

    ///fsci
    fid = env->GetFieldID(nativeClass, "mListenFsci", "I");
    fsci = env->GetIntField(thiz , fid);
    LOGD("mListenFsci = %d", fsci);

    //assign listen config
    if(DtaSetListenConfig(swio, uidLevel, didSupport, fsci) < 0)
    {
        retVal = -1;
        goto end;
    }

    //assign current DTA config parameters
    if(DtaSetConfig(patternNumber) < 0)
    {
        retVal = -1;
        goto end;
    }

    if(DtaSetType((DTA_TEST_TYPE)testType) < 0)
    {
        retVal = -1;
        goto end;
    }

    if(DtaEnableDiscovery() < 0)
    {
        retVal = -1;
        goto end;
    }

end:
    return retVal;

}


static jint com_mediatek_nfc_dta_DtaManager_disableDiscovery(JNIEnv *env, jobject thiz)
{
    int retVal = -1;

    LOGD("%s:", __FUNCTION__);

    retVal = DtaDisableDiscovery();

end:
    return retVal;

}

static jint com_mediatek_nfc_dta_DtaManager_reset(JNIEnv *env, jobject thiz)
{
    //DtaResetDevice();
    return 0;
}

static jint com_mediatek_nfc_dta_DtaManager_setDtaMode (JNIEnv *env, jobject thiz, jint mode)
{
    return DtaSetDeviceDtaMode(mode);
}

static jint com_mediatek_nfc_dta_DtaManager_setDtaQuickMode (JNIEnv *env, jobject thiz, jint mode)
{
    return DtaSetDeviceDtaQuickMode(mode);
}

static jint com_mediatek_nfc_dta_DtaManager_setPatternNumber (JNIEnv *env, jobject thiz, jint patternNumber)
{
    LOGD("%s: pattern number %d", __FUNCTION__, patternNumber);

    return DtaNormalFlowSetPatternNum(patternNumber);
}


static jstring com_mediatek_nfc_dta_DtaManager_getDtaVersion (JNIEnv *env, jobject thiz)
{
    char buffer[10] = {DTA_VERSION,'\0'};
    LOGD("%s: dta JNI version %s", __FUNCTION__, buffer);
    //snprintf(buffer, sizeof(buffer), "%s", );
    return env->NewStringUTF(buffer);
}

static jint com_mediatek_nfc_dta_DtaManager_setDtaConfigPath (JNIEnv *env, jobject thiz, jstring path)
{
    unsigned char* ConfigPath = NULL;
    unsigned int ConfigPathLength = 0;

    ConfigPath = (unsigned char*)env->GetStringUTFChars(path, NULL);
    ConfigPathLength = (unsigned int)env->GetStringUTFLength(path);

    LOGD("%s: dta config path %s, len, %d", __FUNCTION__, ConfigPath, ConfigPathLength);

    return DtaSetConfigPath(ConfigPath, ConfigPathLength);
}


//void transferMessageToJava(JNIEnv *env, nfc_jni_native_data *nat, jstring message)
void transferMessageToJava(char * message)
{
    JavaVM * javaVM = getJavaVM();
    JNIEnv * env = NULL;
    bool attached = false;
    jstring str;
    jfieldID f;
    jclass cls;

    if (javaVM == NULL) {
        LOGD("Java VM is NULL");
        return;
    }

    javaVM->GetEnv( (void **)&env, JNI_VERSION_1_6);
    if (env == NULL) {
        attached = true;
        JavaVMAttachArgs thread_args;
        thread_args.name = "transferMessageToJava";
        thread_args.version = JNI_VERSION_1_6;
        thread_args.group = NULL;
        javaVM->AttachCurrentThread(&env, &thread_args);
    }

    if (env != NULL) {

        str = env->NewStringUTF(message);

        cls = env->GetObjectClass(nat->manager);

        if (cls == NULL) {
            LOGD("failed to find the object");
            return;
        }

        f = env->GetFieldID(cls, "mMessage", "Ljava/lang/String;" );
    	if (f == NULL) {
    		LOGD("failed to find the field");
    		return;
    	}

        env->SetObjectField(nat->manager, f, str);

        env->CallVoidMethod(nat->manager, com_mediatek_nfc_dta_notifyMessageListener);

        env->DeleteLocalRef(str);
    }

    if (attached == true) {
        javaVM->DetachCurrentThread();
    }
    return;
}



void switchTestState()
{
    JavaVM * javaVM = getJavaVM();
    JNIEnv * env = NULL;
    bool attached = false;
    jstring str;
    jfieldID f;
    jclass cls;

    if (javaVM == NULL) {
        LOGD("Java VM is NULL");
        return;
    }

    javaVM->GetEnv( (void **)&env, JNI_VERSION_1_6);
    if (env == NULL) {
        attached = true;
        JavaVMAttachArgs thread_args;
        thread_args.name = "switchTestState";
        thread_args.version = JNI_VERSION_1_6;
        thread_args.group = NULL;
        javaVM->AttachCurrentThread(&env, &thread_args);
    }

    if (env != NULL) {

        env->CallVoidMethod(nat->manager, com_mediatek_nfc_dta_switchTestState);

    }

    if (attached == true) {
        javaVM->DetachCurrentThread();
    }
    return;
}


//---------------------------------------------------------------------------------------//
//    JNI Define
//---------------------------------------------------------------------------------------//
static JNINativeMethod gMethods[] =
{
   {"doInitialize", "()I",
        (void *)com_mediatek_nfc_dta_DtaManager_initialize},
   {"doDeinitialize", "()I",
        (void *)com_mediatek_nfc_dta_DtaManager_deinitialize},
   {"doEnableDiscovery", "(II)I",
        (void *)com_mediatek_nfc_dta_DtaManager_enableDiscovery},
   {"doDisableDiscovery", "()I",
        (void *)com_mediatek_nfc_dta_DtaManager_disableDiscovery},
   {"doReset", "()I",
        (void *)com_mediatek_nfc_dta_DtaManager_reset},
   {"doSetDtaMode", "(I)I",
        (void *)com_mediatek_nfc_dta_DtaManager_setDtaMode},
   {"doSetDtaQuickMode", "(I)I",
        (void *)com_mediatek_nfc_dta_DtaManager_setDtaQuickMode},
   {"doSetPatternNumber", "(I)I",
        (void *)com_mediatek_nfc_dta_DtaManager_setPatternNumber},
   {"doGetDtaVersion", "()Ljava/lang/String;",
        (void *)com_mediatek_nfc_dta_DtaManager_getDtaVersion},
   {"doSetDtaConfigPath", "(Ljava/lang/String;)I",
        (void *)com_mediatek_nfc_dta_DtaManager_setDtaConfigPath},
};


int register_com_mediatek_nfc_dta_NativeDtaManager(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/mediatek/nfc/dta/NativeDtaManager",
      gMethods, NELEM(gMethods));
}

} //namespace android
