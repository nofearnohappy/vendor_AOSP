//[Sensor]
//name = ov16825mipiraw
//
//[Preview]
//read_freq = 80000000
//pixel_line = 1520
//column_length = 1703
//flickerGradThreshold = 45
//
//[Video]
//read_freq = 80000000
//pixel_line = 1520
//column_length = 1703
//flickerGradThreshold = 45
//
//[Capture]
//read_freq = 160000000
//pixel_line = 1910
//column_length = 3431
//flickerGradThreshold = 45
//
//
//[Video1]
//read_freq = 160000000
//pixel_line = 1206
//column_length = 1055
//flickerGradThreshold = 45
//
//[Video2]
//read_freq = 80000000
//pixel_line = 2412
//column_length = 1055
//flickerGradThreshold = 45
#define MTK_LOG_ENABLE 1
#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_ov16825mipiraw.h"
#include "camera_info_ov16825mipiraw.h"
#include "camera_custom_AEPlinetable.h"
//#include "camera_custom_flicker_table.h"
#include "camera_custom_flicker_para.h"
#include <cutils/log.h>


static void get_flicker_para_by_Preview(FLICKER_CUST_PARA* para)
{
  int i;
  int freq[9] = { 70, 80, 100, 110, 120, 140, 170, 210, 230};
  FLICKER_CUST_STATISTICS EV50_L50 = {-194, 4721, 381, -766};
  FLICKER_CUST_STATISTICS EV50_L60 = {1246, 373, 697, -366};
  FLICKER_CUST_STATISTICS EV60_L50 = {1485, 393, 1037, -440};
  FLICKER_CUST_STATISTICS EV60_L60 = {-162, 2898, 247, -642};
  for(i=0;i<9;i++)
  {
    para->flickerFreq[i]=freq[i];
  }
  para->flickerGradThreshold=26;
  para->flickerSearchRange=28;
  para->minPastFrames=3;
  para->maxPastFrames=14;
  para->EV50_L50=EV50_L50;
  para->EV50_L60=EV50_L60;
  para->EV60_L50=EV60_L50;
  para->EV60_L60=EV60_L60;
  para->EV50_thresholds[0]=-30;
  para->EV50_thresholds[1]=12;
  para->EV60_thresholds[0]=-30;
  para->EV60_thresholds[1]=12;
  para->freq_feature_index[0]=4;
  para->freq_feature_index[1]=2;
}

static void get_flicker_para_by_Video(FLICKER_CUST_PARA* para)
{
  int i;
  int freq[9] =  { 70, 80, 100, 110, 120, 140, 170, 210, 230};
  FLICKER_CUST_STATISTICS EV50_L50 = {-194, 4721, 381, -766};
  FLICKER_CUST_STATISTICS EV50_L60 = {1246, 373, 697, -366};
  FLICKER_CUST_STATISTICS EV60_L50 = {1485, 393, 1037, -440};
  FLICKER_CUST_STATISTICS EV60_L60 = {-162, 2898, 247, -642};
  
  for(i=0;i<9;i++)
  {
    para->flickerFreq[i]=freq[i];
  }
  para->flickerGradThreshold=25;
  para->flickerSearchRange=28;
  para->minPastFrames=3;
  para->maxPastFrames=14;
  para->EV50_L50=EV50_L50;
  para->EV50_L60=EV50_L60;
  para->EV60_L50=EV60_L50;
  para->EV60_L60=EV60_L60;
  para->EV50_thresholds[0]=-30;
  para->EV50_thresholds[1]=12;
  para->EV60_thresholds[0]=-30;
  para->EV60_thresholds[1]=12;
  para->freq_feature_index[0]=4;
  para->freq_feature_index[1]=2;
}

static void get_flicker_para_by_Capture(FLICKER_CUST_PARA* para)
{
  int i;
  int freq[9] =  { 80, 90, 100, 110, 120, 130, 140, 160, 230};
  FLICKER_CUST_STATISTICS EV50_L50 = {-194, 4721, 381, -766};
  FLICKER_CUST_STATISTICS EV50_L60 = {1076, 432, 807, -403};
  FLICKER_CUST_STATISTICS EV60_L50 = {1283, 455, 1201, -478};
  FLICKER_CUST_STATISTICS EV60_L60 = {-162, 2898, 247, -642};
  
  for(i=0;i<9;i++)
  {
    para->flickerFreq[i]=freq[i];
  }
  para->flickerGradThreshold=24;
  para->flickerSearchRange=56;
  para->minPastFrames=3;
  para->maxPastFrames=14;
  para->EV50_L50=EV50_L50;
  para->EV50_L60=EV50_L60;
  para->EV60_L50=EV60_L50;
  para->EV60_L60=EV60_L60;
  para->EV50_thresholds[0]=-30;
  para->EV50_thresholds[1]=12;
  para->EV60_thresholds[0]=-30;
  para->EV60_thresholds[1]=12;
  para->freq_feature_index[0]=4;
  para->freq_feature_index[1]=2;
}

static void get_flicker_para_by_Video1(FLICKER_CUST_PARA* para)
{
  int i;
  int freq[9] =  { 70, 100, 120, 140, 160, 170, 190, 210, 230};
  FLICKER_CUST_STATISTICS EV50_L50 = {-194, 4721, 381, -766};
  FLICKER_CUST_STATISTICS EV50_L60 = {326, 1426, 2665, -709};
  FLICKER_CUST_STATISTICS EV60_L50 = {388, 1502, 3967, -783};
  FLICKER_CUST_STATISTICS EV60_L60 = {-162, 2898, 247, -642};
  
  for(i=0;i<9;i++)
  {
    para->flickerFreq[i]=freq[i];
  }
  para->flickerGradThreshold=23;
  para->flickerSearchRange=16;
  para->minPastFrames=3;
  para->maxPastFrames=14;
  para->EV50_L50=EV50_L50;
  para->EV50_L60=EV50_L60;
  para->EV60_L50=EV60_L50;
  para->EV60_L60=EV60_L60;
  para->EV50_thresholds[0]=-30;
  para->EV50_thresholds[1]=12;
  para->EV60_thresholds[0]=-30;
  para->EV60_thresholds[1]=12;
  para->freq_feature_index[0]=2;
  para->freq_feature_index[1]=1;
}

static void get_flicker_para_by_Video2(FLICKER_CUST_PARA* para)
{
  int i;
  int freq[9] =  { 70, 80, 90, 100, 120, 130, 140, 160, 170};
  FLICKER_CUST_STATISTICS EV50_L50 = {-194, 4721, 381, -766};
  FLICKER_CUST_STATISTICS EV50_L60 = {1155, 402, 751, -385};
  FLICKER_CUST_STATISTICS EV60_L50 = {1378, 423, 1119, -459};
  FLICKER_CUST_STATISTICS EV60_L60 = {-162, 2898, 247, -642};
  
  for(i=0;i<9;i++)
  {
    para->flickerFreq[i]=freq[i];
  }
  para->flickerGradThreshold=30;
  para->flickerSearchRange=16;
  para->minPastFrames=3;
  para->maxPastFrames=14;
  para->EV50_L50=EV50_L50;
  para->EV50_L60=EV50_L60;
  para->EV60_L50=EV60_L50;
  para->EV60_L60=EV60_L60;
  para->EV50_thresholds[0]=-30;
  para->EV50_thresholds[1]=12;
  para->EV60_thresholds[0]=-30;
  para->EV60_thresholds[1]=12;
  para->freq_feature_index[0]=4;
  para->freq_feature_index[1]=3;
}

typedef NSFeature::RAWSensorInfo<SENSOR_ID> SensorInfoSingleton_T;
namespace NSFeature {
template <>
UINT32
SensorInfoSingleton_T::
impGetFlickerPara(MINT32 sensorMode, MVOID*const pDataBuf) const
{
	ALOGD("impGetFlickerPara+ mode=%d", sensorMode);
	ALOGD("prv=%d, vdo=%d, cap=%d, zsd=%d",
	    (int)e_sensorModePreview, (int)e_sensorModeVideoPreview, (int)e_sensorModeCapture, (int)e_sensorModeZsd );
	FLICKER_CUST_PARA* para;
	para =  (FLICKER_CUST_PARA*)pDataBuf;
	if(sensorMode == e_sensorModePreview)
		get_flicker_para_by_Preview(para);

	else if(sensorMode == e_sensorModeVideo)
	{
		get_flicker_para_by_Video(para);
	}
	else if(sensorMode == e_sensorModeCapture)
	{
		get_flicker_para_by_Capture(para);		
	}
	else if(sensorMode == e_sensorModeVideo1)
	{
		get_flicker_para_by_Video1(para);		
	}
	else if(sensorMode == e_sensorModeVideo2)
	{
		get_flicker_para_by_Video2(para);		
	}
	else
	{
		ALOGD("impGetFlickerPara ERROR ln=%d", __LINE__);
		return -1;
	}
	ALOGD("impGetFlickerPara-");
	return 0;
}
}

