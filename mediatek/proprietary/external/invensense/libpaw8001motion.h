/*
* Pixart motion library
 */

#ifndef PIXARTW8001MOTION_H
#define PIXARTW8001MOTION_H
#ifdef __cplusplus
extern "C" {
#endif

void PxiAlg_Open(int HZ); // 20hz
void PxiAlg_Close(void);
int PxiAlg_GetReadyFlag(void);
int PxiAlg_GetMotionFlag(void); 
int PxiAlg_Version(void);	
int PxiAlg_GetTouchFlag(void); 
int PxiAlg_Process(char * ppg_data, float * mems_data); //first 6~7 sec = 120~140 sample before return valid HRM value
float PxiAlg_GetSigGrade(void);
void PxiAlg_HrGet(float * hr);

#ifdef __cplusplus
}
#endif
#endif //PIXARTW8001MOTION_H

