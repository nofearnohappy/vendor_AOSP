#ifndef _VOICE_UNLOCK_H
#define _VOICE_UNLOCK_H

//#include "int64_t.h"
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int getVoiceUnlockVersion();

#ifndef ___callback_function_def_
#define ___callback_function_def_
typedef void (*Func_t)(void*, const short*, int64_t);
#endif

typedef struct __VoiceUnlockCustomParameters
{
    unsigned char Cust_Para[8][32];

    short  *voiceFIRCoefMic1;
    short  *voiceFIRCoefMic2;
    int  voiceGainMic1;
    int  voiceGainMic2;
    int  micNumFlag;

} VOICE_UNLOCK_CUSTOM_INFO;

/* struct definition */
typedef struct __UnlockTrainingParameters
{
    //str1: For training, save the trained speaker model here; For testing, path of the already trained Speaker model;
    char *str1;
    char *str2;       //str2: UBM directory

	int id;            //command_id
	int fd1;           //save the trained speaker model here!
	int fd2;           //empty

	Func_t Callback;   //training write wave file
	void *me;          //

} UNLOCK_TRAIN_INFO;

typedef struct __UnlockTestParameters
{
    char *str1;     //str1: pattern directory str2:UBM directory
    char *str2;

} UNLOCK_TEST_INFO;

typedef struct __WakeUpPara
{    
		int Aini;
    int Bini;
    int Adef;
    int Bdef;
    int Timer;
    
} WAKE_UP_PHASE1_PARA ;

/* Initialization and Release function */

int TrainingInit(UNLOCK_TRAIN_INFO *unlockTrainInfo,VOICE_UNLOCK_CUSTOM_INFO *voiceCustomInfo);
int TestingInit(UNLOCK_TEST_INFO *unlockTestInfo, VOICE_UNLOCK_CUSTOM_INFO *voiceCustomInfo);
int DLInit(int downlink_latency);
int TrainingRelease();
int TestingRelease();

/* voice unlock/wakeup utility function */
int endPointDetection (short *pMicBuf1, short *pMicBuf2, short *pDLBuf); // return 1: stop  2: no speech in 5 seconds
int PCMDiagonosis(int *confidence);
int onTraining();
int onTesting(int *command_id);

/*voice wake-up only utility function*/
int setVoiceWakeUpMode (int mode);  // mode --> 0:unlock(default) 1: wake-up (keyword) 2: wake-up (keyword+speaker reocognition) (call this function before TrainingInit and TestingInit)
int wakeupInfoDir (const char *infoDir); // can be read and written by SWIP (call this function before TrainingInit and TestingInit)
//int wakeupEndpointDetection(short *pMicBuf1);  //call this function when Testing and mode=1 or 2
int getNewPhase1Para(WAKE_UP_PHASE1_PARA *p_wakeup_para ); // Call this function before TestingRelease
int setContinueButtonState (int buttonMode); // set buttonMode=1 only when user press "continue" button
int setTargetType(int targetType); //targetType--> 0: phone(default), 1: tablet
#ifdef __cplusplus
}
#endif
#endif
