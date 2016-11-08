#define LOG_NDEBUG 0
#define LOG_TAG "SensorHubService_JNI"
#include "utils/Log.h"

#include <SensorCondition.h>
#include <SensorAction.h>
#include <SensorContext.h>
#include <SensorData.h>
#include <SensorHubManager.h>

#include "jni.h"
#include "JNIHelp.h"
#include <android_runtime/AndroidRuntime.h>

namespace android {

struct DataOffsets
{
    jfieldID    index;
    jfieldID    last;
    jfieldID    dataType;
    jfieldID    intValue;
    jfieldID    longValue;
    jfieldID    floatValue;

    jclass      clazz;
    jmethodID   init;
} gDataOffsets;

struct ItemOffsets
{
    jfieldID    bracketLeft;
    jfieldID    bracketRight;
    jfieldID    combine;
    jfieldID    index1;
    jfieldID    last1;
    jfieldID    index2;
    jfieldID    last2;
    jfieldID    operation;
    jfieldID    dataType;
    jfieldID    intValue;
    jfieldID    longValue;
    jfieldID    floatValue;
    jfieldID    conditionType;

    jclass      clazz;
} gItemOffsets;

struct ConditionOffsets
{
    jmethodID   toArray;
    jclass      clazz;
} gConditionOffsets;

struct ActionOffsets
{
    jfieldID    action;
    jfieldID    repeatable;
    jfieldID    checkLast;

    jclass      clazz;
} gActionOffsets;

struct ServiceOffsets {
    jfieldID    context;
    jfieldID    listener_context;	
    jmethodID   post_event;
	
} gServiceOffsets;

// ----------------------------------------------------------------------------
// ref-counted object for callbacks
class JNISensorHubListener: public SensorTriggerListener
{
public:
    JNISensorHubListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    virtual ~JNISensorHubListener();
    virtual void onTrigger(int msg, int ext1, int ext2, const Parcel* obj);
private:
    JNISensorHubListener(){};

    void triggerJavaDataMethod(int msg, int ext1, int ext2, Vector<SensorData>& v);

    jclass      mClass;     // Reference to SensorHubService class
    jobject     mObject;    // Weak ref to SensorHubService Java object to call on
    jclass      mDataClass; // Reference to SensorData class
};

JNISensorHubListener::JNISensorHubListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{
    // Hold onto the SensorHubService class for use in calling the static method
    // that posts events to the application thread.
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        ALOGE("Can't find com/mediatek/sensorhub/SensorHubService!");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mClass = (jclass)env->NewGlobalRef(clazz);

    // We use a weak reference so the SensorHubService object can be garbage collected.
    // The reference is only used as a proxy for callbacks.
    mObject = env->NewGlobalRef(weak_thiz);

    jclass dataClass = env->FindClass("com/mediatek/sensorhub/DataCell");
    if (dataClass == NULL) {
        ALOGE("Can't find com/mediatek/sensorhub/DataCell!");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mDataClass = (jclass)env->NewGlobalRef(dataClass);
}

JNISensorHubListener::~JNISensorHubListener()
{
    // remove global references
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
    env->DeleteGlobalRef(mDataClass);
    ALOGV("JNISensorHubListener destructor.");	
}

void JNISensorHubListener::triggerJavaDataMethod(int msg, int ext1, int ext2, Vector<SensorData>& v)
{
    JavaVM* vm = AndroidRuntime::getJavaVM();
    JNIEnv *env;
    jint result = vm->AttachCurrentThread(&env, NULL);
    if (result != JNI_OK)
        ALOGE("ERROR: attach of thread sensorhub failed!");

    //JNIEnv *env = AndroidRuntime::getJNIEnv();
    if (mObject != NULL) {
        size_t index = 0;
        size_t size = v.size();
        ALOGV("triggerJavaDataMethod: size=%d", size);
        jobjectArray ret = NULL;		
        if (size > 0) {
            ret = env->NewObjectArray(size, mDataClass, NULL);
            for (Vector<SensorData>::iterator iter = v.begin(); iter != v.end(); iter++) {
                const DataOffsets& dataOffsets(gDataOffsets);
                jobject dataObj = env->NewObject(mDataClass, dataOffsets.init);
                env->SetIntField(dataObj,       dataOffsets.index,      iter->getDataIndex());
                env->SetBooleanField(dataObj,   dataOffsets.last,       iter->isLast());
                env->SetIntField(dataObj,       dataOffsets.dataType,   iter->getType());
                env->SetIntField(dataObj,       dataOffsets.intValue,   iter->getIntValue());
                env->SetLongField(dataObj,      dataOffsets.longValue,  iter->getLongValue());
                env->SetFloatField(dataObj,     dataOffsets.floatValue, iter->getFloatValue());

                env->SetObjectArrayElement(ret, index, dataObj);
                index++;
            }
        }
        //ALOGV("triggerJavaDataMethod: calling method %p>>>", gServiceOffsets.post_event);
        env->CallStaticVoidMethod(mClass, gServiceOffsets.post_event, mObject, msg, ext1, ext2, ret);
        //ALOGV("triggerJavaDataMethod: calling method<<<"); 

        if (env->ExceptionCheck()) {
            ALOGW("triggerJavaDataMethod: exception when notifying event(%d, %d, %d).", msg, ext1, ext2);
            env->ExceptionClear();
        }
    } else {
        ALOGW("triggerJavaDataMethod: failed due to null object!");
    }
    result = vm->DetachCurrentThread();
    if (result != JNI_OK)
        ALOGE("ERROR: thread detach failed!");

}

void JNISensorHubListener::onTrigger(int msg, int ext1, int ext2, const Parcel* obj)
{
    switch (msg) {
    case SENSOR_TRIGGER_ACTION_DATA: {
        ALOGV("onTrigger: msg=%d, arg1=%d, arg2=%d", msg, ext1, ext2);
        Vector<SensorData> v;
        if (obj) {
            ALOGV("onTrigger: pos=%d, size=%d", obj->dataPosition(), obj->dataSize());
            SensorData::unflattenVector(*obj, v);
        }
        triggerJavaDataMethod(SENSOR_TRIGGER_ACTION_DATA, ext1, ext2, v);
    }
        break;
    default:
        ALOGW("onTrigger: unexpected msg=%d, arg1=%d, arg2=%d", msg, ext1, ext2);		
        break;
    }
}

// ----------------------------------------------------------------------------
static sp<SensorHubManager> getSensorHubManager(JNIEnv* env, jobject thiz)
{
    SensorHubManager* const p = (SensorHubManager*)env->GetLongField(thiz, gServiceOffsets.context);
    return sp<SensorHubManager>(p);
}

static sp<SensorHubManager> setSensorHubManager(JNIEnv* env, jobject thiz, const sp<SensorHubManager>& mgr)
{
    sp<SensorHubManager> old = (SensorHubManager*)env->GetLongField(thiz, gServiceOffsets.context);
    if (mgr.get()) {
        mgr->incStrong((void*)setSensorHubManager);
    }
    if (old != 0) {
        old->decStrong((void*)setSensorHubManager);
    }
    env->SetLongField(thiz, gServiceOffsets.context, (jlong)mgr.get());
    return old;
}

static sp<SensorTriggerListener> getSensorTriggerListener(JNIEnv* env, jobject thiz)
{
    SensorTriggerListener* const p = (SensorTriggerListener*)env->GetLongField(thiz, gServiceOffsets.listener_context);
    return sp<SensorTriggerListener>(p);
}

static sp<SensorTriggerListener> setSensorTriggerListener(JNIEnv* env, jobject thiz, const sp<JNISensorHubListener>& listener)
{
    sp<SensorTriggerListener> old = (SensorTriggerListener*)env->GetLongField(thiz, gServiceOffsets.listener_context);
    if (listener.get()) {
        listener->incStrong((void*)setSensorHubManager);
    }
    if (old != 0) {
        old->decStrong((void*)setSensorHubManager);
    }
    env->SetLongField(thiz, gServiceOffsets.listener_context, (jlong)listener.get());
    return old;
}

static void nativeInit(JNIEnv *env)
{
    ALOGV("nativeInit>>>");
    jclass clazz = env->FindClass("com/mediatek/sensorhub/SensorHubService");
    if (clazz == NULL) {
        ALOGE("nativeInit: failed! No SensorHubService class!");		
        return;
    }
    gServiceOffsets.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (gServiceOffsets.context == NULL) {
        ALOGE("nativeInit: failed! No mNativeContext field!");
        return;
    }
    gServiceOffsets.listener_context = env->GetFieldID(clazz, "mListenerContext", "J");
    if (gServiceOffsets.listener_context == NULL) {
        ALOGE("nativeInit: failed! No mListenerContext field!");
        return;
    }
    gServiceOffsets.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;III[Ljava/lang/Object;)V");
    if (gServiceOffsets.post_event == NULL) {
        ALOGE("nativeInit: failed! No postEventFromNative method!");		
        return;
    }

    jclass dataClass = env->FindClass("com/mediatek/sensorhub/DataCell");
    DataOffsets& dataOffsets = gDataOffsets;
    dataOffsets.index               = env->GetFieldID(dataClass, "mIndex",             "I");
    dataOffsets.last                = env->GetFieldID(dataClass, "mIsPrevious",        "Z");
    dataOffsets.dataType            = env->GetFieldID(dataClass, "mType",              "I");
    dataOffsets.intValue            = env->GetFieldID(dataClass, "mIntValue",          "I");
    dataOffsets.longValue           = env->GetFieldID(dataClass, "mLongValue",         "J");
    dataOffsets.floatValue          = env->GetFieldID(dataClass, "mFloatValue",        "F");
    dataOffsets.init                = env->GetMethodID(dataClass, "<init>",          "()V");
    dataOffsets.clazz               = dataClass;

    jclass actionClass = env->FindClass("com/mediatek/sensorhub/Action");
    ActionOffsets& actionOffsets = gActionOffsets;
    actionOffsets.action            = env->GetFieldID(actionClass, "mActionId",                 "I");
    actionOffsets.repeatable        = env->GetFieldID(actionClass, "mRepeatable",               "Z");
    actionOffsets.checkLast         = env->GetFieldID(actionClass, "mOnConditionChanged",       "Z");
    actionOffsets.clazz             = actionClass;

    jclass itemClass = env->FindClass("com/mediatek/sensorhub/ConditionItem");
    ItemOffsets& itemOffsets = gItemOffsets;
    itemOffsets.bracketLeft         = env->GetFieldID(itemClass, "mBracketLeft",       "Z");
    itemOffsets.bracketRight        = env->GetFieldID(itemClass, "mBracketRight",      "Z");
    itemOffsets.combine             = env->GetFieldID(itemClass, "mCombine",           "I");
    itemOffsets.index1              = env->GetFieldID(itemClass, "mIndex1",            "I");
    itemOffsets.last1               = env->GetFieldID(itemClass, "mIsLast1",           "Z");
    itemOffsets.index2              = env->GetFieldID(itemClass, "mIndex2",            "I");
    itemOffsets.last2               = env->GetFieldID(itemClass, "mIsLast2",           "Z");
    itemOffsets.operation           = env->GetFieldID(itemClass, "mOperation",         "I");
    itemOffsets.dataType            = env->GetFieldID(itemClass, "mDataType",          "I");
    itemOffsets.intValue            = env->GetFieldID(itemClass, "mIntValue",          "I");
    itemOffsets.longValue           = env->GetFieldID(itemClass, "mLongValue",         "J");
    itemOffsets.floatValue          = env->GetFieldID(itemClass, "mFloatValue",        "F");
    itemOffsets.conditionType       = env->GetFieldID(itemClass, "mConditionType",     "I");
    itemOffsets.clazz               = itemClass;

    jclass conditionClass = env->FindClass("com/mediatek/sensorhub/Condition");
    ConditionOffsets& conditionOffsets = gConditionOffsets;
    conditionOffsets.toArray        = env->GetMethodID(conditionClass, "toArray",       
                                              "()[Lcom/mediatek/sensorhub/ConditionItem;");
    conditionOffsets.clazz          = conditionClass;

    ALOGV("nativeInit<<<");
}

static void nativeSetup(JNIEnv *env, jobject thiz, jobject weak_thiz)
{
    ALOGV("nativeSetup>>>");
    sp<SensorHubManager> mgr = new SensorHubManager();
    if (mgr == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }
    setSensorHubManager(env, thiz, mgr);

    // create new listener and give it to SensorHubManager
    sp<JNISensorHubListener> listener = new JNISensorHubListener(env, thiz, weak_thiz);
    if (listener == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }
    setSensorTriggerListener(env, thiz, listener);
    ALOGV("nativeSetup<<<");
}

static void nativeFinalize(JNIEnv *env, jobject thiz)
{
}

static jintArray nativeGetContextList(JNIEnv *env, jobject thiz) {
    sp<SensorHubManager> mgr = getSensorHubManager(env, thiz);
    if (mgr == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    Vector<int> list = mgr->getContextList();
    size_t size = list.size();
    jintArray intArray = env->NewIntArray(size);
    jint* ret = env->GetIntArrayElements(intArray, NULL);
    for (size_t i = 0; i < size; i++) {
        ret[i] = list[i];
    }
    env->ReleaseIntArrayElements(intArray, ret, 0);
    return intArray;
}

static SensorCondition createNativeCondition(JNIEnv *env, jobject obj)
{
    SensorCondition condition;
    jobjectArray objectArray = (jobjectArray)env->CallObjectMethod(obj, gConditionOffsets.toArray);
    size_t size = env->GetArrayLength(objectArray);
    for (size_t i = 0; i < size; i++) {
        jobject item = env->GetObjectArrayElement(objectArray, i);
        
        jboolean bracketLeft = env->GetBooleanField(item, gItemOffsets.bracketLeft);
        jboolean bracketRight = env->GetBooleanField(item, gItemOffsets.bracketRight);
        jint combine = env->GetIntField(item, gItemOffsets.combine);

        jint index1 = env->GetIntField(item, gItemOffsets.index1);
        jboolean last1 = env->GetBooleanField(item, gItemOffsets.last1);
        jint index2 = env->GetIntField(item, gItemOffsets.index2);
        jboolean last2 = env->GetBooleanField(item, gItemOffsets.last2);
        jint operation = env->GetIntField(item, gItemOffsets.operation);
        jint dataType = env->GetIntField(item, gItemOffsets.dataType);
        jint intValue = env->GetIntField(item, gItemOffsets.intValue);
        jlong longValue = env->GetLongField(item, gItemOffsets.longValue);
        jfloat floatValue = env->GetFloatField(item, gItemOffsets.floatValue);
        jint conditionType = env->GetIntField(item, gItemOffsets.conditionType);
        ALOGV("createCondition: index=%d, comb=0x%x, op=0x%x, bl=%d, br=%d, type=%d, ivalue=%d, fvalue=%f, lvalue=%lld", 
            index1, combine, operation, bracketLeft, bracketRight, dataType, intValue, floatValue, longValue);
        SensorConditionItem sci(index1, last1, operation, index2, last2, dataType, intValue, longValue, floatValue,
            bracketLeft, bracketRight, combine, conditionType);
        condition.add(sci);
    }
    return condition;
}

static SensorAction createNativeAction(JNIEnv *env, jobject thiz, jobject action)
{
    //jint actionId = env->GetIntField(action, gActionOffsets.action);
    jboolean repeatable = env->GetBooleanField(action, gActionOffsets.repeatable);
    jboolean checkLast = env->GetBooleanField(action, gActionOffsets.checkLast);
    sp<SensorTriggerListener> listener = getSensorTriggerListener(env, thiz);
    ALOGV("createAction: repeatable=%d, checkLast=%d, listener=%p", repeatable, checkLast, listener.get());	
    SensorAction sa(listener, repeatable, checkLast);
    return sa;
}

static jint nativeRequestAction(JNIEnv *env, jobject thiz, jobject condition, jobject action)
{
    jint rid = 0; 
    sp<SensorHubManager> mgr = getSensorHubManager(env, thiz);
    if (mgr == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return rid;
    }

    rid = mgr->requestAction(createNativeCondition(env, condition), createNativeAction(env, thiz, action));
    ALOGV("nativeRequestAction: rid=%d", rid);
    return rid;
}

static jboolean nativeUpdateCondition(JNIEnv *env, jobject thiz, jint requestId, jobject condition)
{
    jboolean ret = false;
    sp<SensorHubManager> mgr = getSensorHubManager(env, thiz);
    if (mgr == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return ret;
    }

    ret = mgr->updateCondition(requestId, createNativeCondition(env, condition));
    ALOGV("nativeUpdateCondition: rid=%d, ret=%d", requestId, ret);
    return ret;
}

static jboolean nativeCancelAction(JNIEnv *env, jobject thiz, jint requestId)
{
    jboolean ret = false;
    sp<SensorHubManager> mgr = getSensorHubManager(env, thiz);
    if (mgr == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return ret;
    }

    ret = mgr->cancelAction(requestId);
    ALOGV("nativeCancelAction: rid=%d, ret=%d", requestId, ret);
    return ret;
}

static jboolean nativeEnableGestureWakeup(JNIEnv *env, jobject thiz, jboolean enable)
{
    jboolean ret = false;
    sp<SensorHubManager> mgr = getSensorHubManager(env, thiz);
    if (mgr == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return ret;
    }
	
    ret = mgr->enableGestureWakeup(enable);
    ALOGV("nativeEnableGestureWakeup: enable=%d, ret=%d", enable, ret);
    return ret;
}

static JNINativeMethod gMethods[] = {
    {"nativeInit",              "()V",                              (void*)nativeInit },
    {"nativeSetup",             "(Ljava/lang/Object;)V",            (void*)nativeSetup },
    {"nativeFinalize",          "()V",                              (void*)nativeFinalize },
    {"nativeGetContextList",    "()[I",                             (void*)nativeGetContextList },
    {"nativeRequestAction",     "(Lcom/mediatek/sensorhub/Condition;Lcom/mediatek/sensorhub/Action;)I",
                                                                    (void*)nativeRequestAction },
    {"nativeUpdateCondition",   "(ILcom/mediatek/sensorhub/Condition;)Z",
															        (void*)nativeUpdateCondition},
    {"nativeCancelAction",      "(I)Z",                             (void*)nativeCancelAction },
    {"nativeEnableGestureWakeup",       "(Z)Z",                     (void*)nativeEnableGestureWakeup },
};

}; // namespace android

using namespace android;

int register_com_mediatek_sensorhub_SensorHubService(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "com/mediatek/sensorhub/SensorHubService",
            gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_com_mediatek_sensorhub_SensorHubService(env) < 0) {
        ALOGE("ERROR: SensorHubService native registration failed!");
        goto bail;
    }

    ALOGD("JNI_OnLoad: succeed...");
    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
