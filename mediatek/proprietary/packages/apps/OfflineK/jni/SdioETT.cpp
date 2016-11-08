#include <jni.h>
#include <com_mtk_offlinek_NativeLoader.h>
#include <cstdio>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <syslog.h>
#include <ett_core.h>
#include <cstdlib>
#include <cstring>
#define READY_DEVNODE "/sys/autok/ready"
#define FORCE_TEST 0
//int get_node_data(const char *filename, char **data, int *len);

struct ETT_DATA g_ettdata;
int g_ett_progress;
JNIEXPORT void JNICALL Java_com_mtk_offlinek_NativeLoader_init
  (JNIEnv *env, jobject obj)
{
	g_ettdata.mPort = NULL;
	g_ettdata.mModule = NULL;
	g_ettdata.mVoltage = NULL;
	g_ettdata.mFrequency = NULL;
	g_ettdata.mCmdTimes = NULL;
	g_ettdata.mRDataTimes = NULL;
	g_ettdata.mWDataTimes = NULL;
	g_ettdata.mLogPath = NULL;
}

void freeETTData(){
	if(g_ettdata.mPort != NULL){
		free(g_ettdata.mPort);
		free(g_ettdata.mFrequency);
		free(g_ettdata.mVoltage);
		free(g_ettdata.mModule);
		free(g_ettdata.mCmdTimes);
		free(g_ettdata.mRDataTimes);
		free(g_ettdata.mWDataTimes);
		free(g_ettdata.mLogPath);
		g_ettdata.mPort = NULL;
		g_ettdata.mFrequency = NULL;
		g_ettdata.mVoltage = NULL;
		g_ettdata.mModule = NULL;
		g_ettdata.mCmdTimes = NULL;
		g_ettdata.mRDataTimes = NULL;
		g_ettdata.mWDataTimes = NULL;
		g_ettdata.mLogPath = NULL;
	}
}

JNIEXPORT jstring JNICALL Java_com_mtk_offlinek_NativeLoader_doETT
  (JNIEnv *env, jobject obj)
{
	char data_buf[] = "Juju Test Autok";
	char **argv;
	int data_count;
	int argc = 7, i;
	const int argv_length = 256;

	argv = (char**)malloc(sizeof(char*)*argc);
	g_ett_progress  = 0;

#if FORCE_TEST
	for(i=0; i<argc; i++){
		argv[i] = (char*)malloc(sizeof(char)*argv_length);
	}
	strcpy(argv[0], "cli_tuning");
	strcpy(argv[1], "2");	// Port
	strcpy(argv[2], "200");	// Frequency
	strcpy(argv[3], "1");	// [Test/Normal Mode]Useless
	strcpy(argv[4], "1");	// Cmd_test_times
	strcpy(argv[5], "1");	// rdata_times
	strcpy(argv[6], "1");	// wdata_times
#else
	argv[0] = (char*)malloc(sizeof(char)*argv_length);
	strcpy(argv[0], "cli_tuning");
	argv[1] = g_ettdata.mPort;	// Port
	argv[2] = g_ettdata.mFrequency;	// Frequency
	argv[3] = (char*)malloc(sizeof(char)*argv_length);
	strcpy(argv[3], "1");	// [Test/Normal Mode]Useless
	argv[4] = g_ettdata.mCmdTimes;	// Cmd_test_times
	argv[5] = g_ettdata.mRDataTimes;	// rdata_times
	argv[6] = g_ettdata.mWDataTimes;	// wdata_times
#endif
#if 1
	algo_init(argc, (const char**)argv);
#endif
	//syslog(LOG_ERR, "[JUJU]JNI CALL");
	//get_node_data(READY_DEVNODE, &data_buf, &data_count);
#if FORCE_TEST
	for(i=0; i<argc; i++){
		free(argv[i]);
	}
#else
	free(argv[0]);
	free(argv[3]);
#endif
	free(argv);
	freeETTData();
	g_ett_progress = 100;
	return env->NewStringUTF(data_buf);
}

JNIEXPORT jint JNICALL Java_com_mtk_offlinek_NativeLoader_setRecordItem
  (JNIEnv *env, jobject callingObj, jobject obj)
{
	const char *frequencyText, *portText, *deviceText,
			*cmdTimesText, *rDataTimesText, *wDataTimesText, *voltageText, *logPathText;
	jclass cls = env->GetObjectClass(obj);
	jfieldID frequencyID = env->GetFieldID(cls, "mFrequency", "Ljava/lang/String;");
	jfieldID portID = env->GetFieldID(cls, "mPort", "Ljava/lang/String;");
	jfieldID deviceID = env->GetFieldID(cls, "mDevice", "Ljava/lang/String;");
	jfieldID cmdTimesID = env->GetFieldID(cls, "mCmdTimes", "Ljava/lang/String;");
	jfieldID rDataTimesID = env->GetFieldID(cls, "mRDataTimes", "Ljava/lang/String;");
	jfieldID wDataTimesID = env->GetFieldID(cls, "mWDataTimes", "Ljava/lang/String;");
	jfieldID voltageID = env->GetFieldID(cls, "mVoltage", "Ljava/lang/String;");
	jfieldID logPathID = env->GetFieldID(cls, "mLogPath", "Ljava/lang/String;");
	jstring frequencyStr = (jstring)env->GetObjectField(obj, frequencyID);
	jstring portStr = (jstring)env->GetObjectField(obj, portID);
	jstring deviceStr = (jstring)env->GetObjectField(obj, deviceID);
	jstring cmdTimesStr = (jstring)env->GetObjectField(obj, cmdTimesID);
	jstring rDataTimesStr = (jstring)env->GetObjectField(obj, rDataTimesID);
	jstring wDataTimesStr = (jstring)env->GetObjectField(obj, wDataTimesID);
	jstring voltageStr = (jstring)env->GetObjectField(obj, voltageID);
	jstring logPathStr = (jstring)env->GetObjectField(obj, logPathID);
	frequencyText = env->GetStringUTFChars(frequencyStr, NULL);
	portText = env->GetStringUTFChars(portStr, NULL);
	deviceText = env->GetStringUTFChars(deviceStr, NULL);
	cmdTimesText = env->GetStringUTFChars(cmdTimesStr, NULL);
	rDataTimesText = env->GetStringUTFChars(rDataTimesStr, NULL);
	wDataTimesText = env->GetStringUTFChars(wDataTimesStr, NULL);
	voltageText = env->GetStringUTFChars(voltageStr, NULL);
	logPathText = env->GetStringUTFChars(logPathStr, NULL);
	freeETTData();
	g_ettdata.mPort = (char*)calloc((strlen(portText)+1),sizeof(char));
	g_ettdata.mFrequency = (char*)calloc((strlen(frequencyText)+1),sizeof(char));
	g_ettdata.mVoltage = (char*)calloc((strlen(voltageText)+1),sizeof(char));
	g_ettdata.mModule = (char*)calloc((strlen(deviceText)+1),sizeof(char));
	g_ettdata.mCmdTimes = (char*)calloc((strlen(cmdTimesText)+1),sizeof(char));
	g_ettdata.mRDataTimes = (char*)calloc((strlen(rDataTimesText)+1),sizeof(char));
	g_ettdata.mWDataTimes = (char*)calloc((strlen(wDataTimesText)+1),sizeof(char));
	g_ettdata.mLogPath = (char*)calloc((strlen(logPathText)+1),sizeof(char));
	strncpy(g_ettdata.mPort, portText, strlen(portText));
	strncpy(g_ettdata.mFrequency, frequencyText, strlen(frequencyText));
	strncpy(g_ettdata.mVoltage, voltageText, strlen(voltageText));
	strncpy(g_ettdata.mModule, deviceText, strlen(deviceText));
	strncpy(g_ettdata.mCmdTimes, cmdTimesText, strlen(cmdTimesText));
	strncpy(g_ettdata.mRDataTimes, rDataTimesText, strlen(rDataTimesText));
	strncpy(g_ettdata.mWDataTimes, wDataTimesText, strlen(wDataTimesText));
	strncpy(g_ettdata.mLogPath, logPathText, strlen(logPathText));
	env->ReleaseStringUTFChars(frequencyStr, frequencyText);
	env->ReleaseStringUTFChars(portStr, portText);
	env->ReleaseStringUTFChars(voltageStr, voltageText);
	env->ReleaseStringUTFChars(deviceStr, deviceText);
	env->ReleaseStringUTFChars(cmdTimesStr, cmdTimesText);
	env->ReleaseStringUTFChars(rDataTimesStr, rDataTimesText);
	env->ReleaseStringUTFChars(wDataTimesStr, wDataTimesText);
	env->ReleaseStringUTFChars(logPathStr, logPathText);
	g_ett_progress  = 0;
	return 0;
}



JNIEXPORT void JNICALL Java_com_mtk_offlinek_NativeLoader_switchVoltage
  (JNIEnv *env, jobject obj)
{

}

JNIEXPORT jint JNICALL Java_com_mtk_offlinek_NativeLoader_getProgress
  (JNIEnv *env, jobject obj)
{
	return g_ett_progress;
}
