/*
 * ett_core.h
 *
 *  Created on: 2014/3/5
 *      Author: MTK04314
 */

#ifndef ETT_CORE_H_
#define ETT_CORE_H_


extern int algo_init(int argc, const char ** argv);
struct ETT_DATA{
	char *mPort;
	char *mModule;
	char *mVoltage;
	char *mFrequency;
	char *mCmdTimes;
	char *mRDataTimes;
	char *mWDataTimes;
	char *mLogPath;
};
#endif /* ETT_CORE_H_ */
