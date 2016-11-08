#include <typedefs.h>
#include "timer.h"

#include <upmu_hw.h>
#include "pmic.h"
//==============================================================================
// Extern
//==============================================================================
extern int g_R_BAT_SENSE;
extern int g_R_I_SENSE;
extern int g_bat_init_flag;

extern U32 pmic_read_interface (U32 RegNum, U32 *val, U32 MASK, U32 SHIFT);
extern U32 pmic_config_interface (U32 RegNum, U32 val, U32 MASK, U32 SHIFT);
extern U32 get_mt6332_pmic_chip_version (void);
//==============================================================================
// PMIC-AUXADC related define
//==============================================================================
#define VOLTAGE_FULL_RANGE     	3200
#define ADC_PRECISE         	4096 	// 12 bits

//==============================================================================
// PMIC-AUXADC global variable
//==============================================================================
kal_int32 count_time_out=100;

//==============================================================================
// PMIC-AUXADC 
//==============================================================================
int PMIC_IMM_GetVbat(int deCount)
{
#if 1
	kal_int32 ret;    
	kal_int32 count=0;
	kal_int32 u4Sample_times = 0;
	kal_int32 u4channel=0;    
	kal_int32 adc_result_temp=0;  
	kal_int32 adc_result=0;
	kal_uint32 ready_bit = 0, output_val = 0;

	/*
		MT6331
		0 : NA
		1 : NA
		2 : NA 
		3 : NA
		4 : TSENSE_PMIC_31
		5 : VACCDET
		6 : VISMPS_1
		7 : AUXADCVIN0
		8 : NA    
		9 : HP
		11-15: Shared
		
		MT6332
		0 : BATSNS
		1 : ISENSE
		2 : VBIF 
		3 : BATON
		4 : TSENSE_PMIC_32
		5 : VCHRIN
		6 : VISMPS_2
		7 : VUSB/ VADAPTOR
		8 : M3_REF    
		9 : SPK_ISENSE
		10: SPK_THR_V
		11: SPK_THR_I
		12-15: shared 
	*/
	
		// for batses, isense
	if(get_mt6332_pmic_chip_version()==PMIC6332_E1_CID_CODE) {
		pmic_config_interface( (kal_uint32)(MT6332_AUXADC_CON10), 1, 0x1, 5);
		pmic_config_interface( (kal_uint32)(MT6332_AUXADC_CON10), 1, 0x1, 6);
	}
	
	// set average smaple number = 16
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON2), 0, 0xFFF, 0);
	pmic_config_interface( (kal_uint32)(MT6332_AUXADC_CON2), 0, 0xFFF, 0);
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON1), 3, 0x7, 0);
	pmic_config_interface( (kal_uint32)(MT6332_AUXADC_CON1), 3, 0x7, 0);
	
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON0), 0, 0x1, 15);
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON0), 0, 0x1, 14);
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON0), 0, 0x1, 13);
	pmic_config_interface( (kal_uint32)(MT6331_AUXADC_CON7), 3, 0x3, 1);	

	do
	{
		count=0;

		ret=pmic_config_interface( (kal_uint32)(MT6332_AUXADC_RQST0_CLR), 0x1, 0x1, 0);	//clear
		ret=pmic_config_interface( (kal_uint32)(MT6332_AUXADC_RQST0_CLR), 0x0, 0x1, 0);
		ret=pmic_config_interface( (kal_uint32)(MT6332_AUXADC_RQST0_SET), 0x1, 0x1, 0);	//set
		
		udelay(1);
	        
	        pmic_read_interface(MT6332_AUXADC_ADC0, &ready_bit,0x8000,0x0);
                while( ( ready_bit >>15 ) != 1 )
                {
	            udelay(1);
	            if( (count++) > count_time_out)
	            {
			print("Power/PMIC", "[IMM_GetOneChannelValue_PMIC] BATSNS Time out!\n");
			break;
	            }            
                }
                ret = pmic_read_interface(MT6332_AUXADC_ADC0,&output_val,0x0FFF,0x0);             
		ret = pmic_config_interface( (kal_uint32)(MT6332_AUXADC_RQST0_CLR), 0x1, 0x1, 0);	//clear
		ret = pmic_config_interface( (kal_uint32)(MT6332_AUXADC_RQST0_CLR), 0x0, 0x1, 0);
	        u4channel += output_val;
	        u4Sample_times++;
	}while (u4Sample_times < deCount);
	/* Value averaging  */ 
	adc_result_temp = u4channel / deCount;
	adc_result = (adc_result_temp * 2 * VOLTAGE_FULL_RANGE)/ADC_PRECISE;

	if (ret == 0)
		return adc_result;
	else
		return -1;
#else
	return 0;
#endif   
}


