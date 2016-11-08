#define LOG_TAG "AALTOOL_JNI"

#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <utils/Log.h>
#include <linux/sensors_io.h>
#include <AALClient.h>

using namespace android;

#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jint JNICALL Java_com_mediatek_aaltool_AALALSCalibration_nGetALSRawData(JNIEnv * env, jobject jobj)
{
    int err = 0;
    int als = 0;
    int fd = open("/dev/als_ps", O_RDONLY);
    
    if (fd < 0) 
    {
        ALOGE("Fail to open alsps device (error: %s)\n", strerror(errno));
        return -1;
    }
    
    if ((err = ioctl(fd, ALSPS_GET_ALS_RAW_DATA, &als)))
    {
        ALOGE("ioctl ALSPS_GET_ALS_RAW_DATA error: %d\n", err);
        close(fd);
        return -1;
    }
    
    ALOGD("als = %d\n", als);
    close(fd);
    return als;
}


JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetALI2BLICurve(JNIEnv * env, jobject jobj, jintArray jcurve)
{
#ifdef MTK_AAL_RUNTIME_TUNING_SUPPORT
    jsize len = env->GetArrayLength(jcurve);

    int *buffer = new int[len];

    jint *curve = env->GetIntArrayElements(jcurve, 0);
    for (jsize i = 0; i < len; i++) {
        buffer[i] = curve[i];
    }
    env->ReleaseIntArrayElements(jcurve, curve, 0);
    
    uint32_t serial;
    if (android::AALClient::getInstance().setAdaptField(
            IAALService::ALI2BLI_CURVE, buffer, sizeof(buffer[0]) * len, &serial) != NO_ERROR)
    {
        ALOGE("fail to set ALI2BLI curve");
        return false;
    }

    delete [] buffer;

    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}


bool getAdaptFieldInt(IAALService::AdaptFieldId field, int &value)
{
#ifdef MTK_AAL_RUNTIME_TUNING_SUPPORT
    status_t ret;
    unsigned int serial;
    ret = AALClient::getInstance().getAdaptField(field, &value, sizeof(value), &serial);
    if (ret != NO_ERROR)
        value = 0;
    return (ret == NO_ERROR);
#else
    value = 0;
    return false;
#endif
}


bool setAdaptFieldInt(IAALService::AdaptFieldId field, int value)
{
#ifdef MTK_AAL_RUNTIME_TUNING_SUPPORT
    unsigned int serial;
    return (AALClient::getInstance().setAdaptField(field, &value, sizeof(value), &serial) == NO_ERROR);
#else
    return false;
#endif
}


JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetRampRateDarken(JNIEnv * env, jobject jobj, jint rate)
{
    return (setAdaptFieldInt(IAALService::BLI_RAMP_RATE_DARKEN, rate) ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetRampRateBrighten(JNIEnv * env, jobject jobj, jint rate)
{
    return (setAdaptFieldInt(IAALService::BLI_RAMP_RATE_BRIGHTEN, rate) ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetSmartBacklightStrength(JNIEnv * env, jobject jobj, jint level)
{
    ALOGD("SmartBacklight level = %d", level);
    if (AALClient::getInstance().setSmartBacklightStrength(level) != 0)
    {
        ALOGE("fail to set SmartBacklight strength");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetSmartBacklightRange(JNIEnv * env, jobject jobj, jint level)
{
    ALOGD("SmartBacklight level = %d", level);
    if (AALClient::getInstance().setSmartBacklightRange(level) != 0)
    {
        ALOGE("fail to set SmartBacklight range");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetReadabilityLevel(JNIEnv * env, jobject jobj, jint level)
{
    ALOGD("Readability level = %d", level);
    if (AALClient::getInstance().setReadabilityLevel(level) != 0)
    {
        ALOGE("fail to set Readability level");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nSetLowBLReadabilityLevel(JNIEnv * env, jobject jobj, jint level)
{
    ALOGD("LowBLReadability level = %d", level);
    if (AALClient::getInstance().setLowBLReadabilityLevel(level) != 0)
    {
        ALOGE("fail to set Readability level");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_mediatek_aaltool_AALTuning_nEnableFunctions(JNIEnv * env, jobject jobj)
{
    ALOGD("Enable AAL functions");
    if (AALClient::getInstance().setFunction(IAALService::FUNC_CABC | IAALService::FUNC_DRE) != 0)
    {
        ALOGE("fail to set AAL function");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}



class JavaObjectRef {
private:
    JNIEnv *mJEnv;
    jclass mClass;
    jobject mObj;
    
public:
    JavaObjectRef(JNIEnv *env, const char *className, jobject obj) {
        mJEnv = env;
        mClass = mJEnv->FindClass(className);
        mObj = obj;

        if (mClass == NULL)
            ALOGE("ERROR: class %s not found", className);
    }

    inline void set(const char *fieldName, jint value) {
        if (mClass != NULL) {
            jfieldID field = mJEnv->GetFieldID(mClass, fieldName, "I");
            if (field != NULL)
                mJEnv->SetIntField(mObj, field, value);
            else
                ALOGE("ERROR: field %s not found", fieldName);
        }
    }
};


JNIEXPORT void JNICALL Java_com_mediatek_aaltool_AALTuning_nGetParameters(JNIEnv *env, jobject jobj, jobject jparam)
{
    AALParameters param;

    if (AALClient::getInstance().getParameters(&param) == NO_ERROR) {
        JavaObjectRef jParamRef(env, "com/mediatek/aaltool/AALTuning$AALParameters", jparam);
        
        if (param.readabilityLevel >= 0)
            jParamRef.set("readabilityLevel", param.readabilityLevel);
        if (param.lowBLReadabilityLevel >= 0)
            jParamRef.set("lowBLReadabilityLevel", param.lowBLReadabilityLevel);
        if (param.smartBacklightStrength >= 0)
            jParamRef.set("smartBacklightStrength", param.smartBacklightStrength);
        if (param.smartBacklightRange >= 0)
            jParamRef.set("smartBacklightRange", param.smartBacklightRange);

        int rate;
        if (getAdaptFieldInt(IAALService::BLI_RAMP_RATE_DARKEN, rate))
            jParamRef.set("darkeningSpeedLevel", rate);
        if (getAdaptFieldInt(IAALService::BLI_RAMP_RATE_BRIGHTEN, rate))
            jParamRef.set("brighteningSpeedLevel", rate);

    } else {
        ALOGE("fail to get AAL parameters");
    }
}


#ifdef __cplusplus
}
#endif
