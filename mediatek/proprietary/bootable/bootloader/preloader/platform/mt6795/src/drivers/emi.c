// 6595_DDR.cpp : Defines the entry point for the console application.
//

#include <emi.h>
#include <typedefs.h>
#include "dramc_common.h"
#include "dramc_register.h"
#include "dramc_pi_api.h"
#include "emi.h"
#include "platform.h"
#include "custom_emi.h"
#include <stdlib.h>

#define DRAM_BASE 0x40000000ULL

extern u32 seclib_get_devinfo_with_index(u32 index);

DRAMC_CTX_T *psCurrDramCtx;

DRAMC_CTX_T DramCtx_LPDDR3 =
{
	CHANNEL_A,		// DRAM_CHANNEL
	TYPE_LPDDR3,	// DRAM_DRAM_TYPE_T
	PACKAGE_POP,	// DRAM_PACKAGE_T
	DATA_WIDTH_32BIT,		// DRAM_DATA_WIDTH_T
	DEFAULT_TEST2_1_CAL, 	// test2_1;
	DEFAULT_TEST2_2_CAL,	// test2_2;
	TEST_XTALK_PATTERN,	// test_pattern;
#ifdef DUAL_FREQ_K
	DUAL_FREQ_LOW,
#else
    #ifdef DDR_667
        333,
    #elif defined (DDR_800)
        400,
    #elif defined (DDR_1066)
    	533,
    #elif defined (DDR_1333)
    	666,
    #elif defined (DDR_1600)
    	800,
    #elif defined (DDR_1780)
    	890,
    #elif defined (DDR_1866)
    	933,
    #elif defined (DDR_2000)
    	1000,
    #elif defined (DDR_2133)
    	1066,
    #elif defined (DDR_1420)
    	710,
    #elif defined (DDR_2400)
    	1200,
    #elif defined (DDR_1792)
	896,
    #else		
    	890,
    #endif	
#endif
	
	533,			// frequency_low;
	DISABLE,		// fglow_freq_write_en;
	DISABLE,	// ssc_en;
	DISABLE		// en_4bitMux;
};

DRAMC_CTX_T DramCtx_PCDDR3 =
{
	CHANNEL_A,		// DRAM_CHANNEL
	TYPE_PCDDR3,		// DRAM_DRAM_TYPE_T
	PACKAGE_SBS,	// DRAM_PACKAGE_T
	DATA_WIDTH_32BIT,		// DRAM_DATA_WIDTH_T
	DEFAULT_TEST2_1_CAL, 	// test2_1;
	DEFAULT_TEST2_2_CAL,	// test2_2;
	TEST_XTALK_PATTERN,	// test_pattern; Audio or Xtalk.
	900, // frequency;	
	533,			// frequency_low;
	ENABLE,		// fglow_freq_write_en;
	DISABLE,	// ssc_en;
	DISABLE		// en_4bitMux;
};

#ifndef COMBO_MCP

#ifdef DDRTYPE_LPDDR2
#define    EMI_CONA_VAL     LPDDR2_EMI_CONA
#endif

#ifdef DDRTYPE_LPDDR3
//#define    EMI_CONA_VAL         0x2A3AE
#define    EMI_CONA_VAL     LPDDR3_EMI_CONA
#endif

#ifdef LPDDR3_EMI_CONH
    #define    EMI_CONH_VAL     LPDDR3_EMI_CONH
#else
    #define    EMI_CONH_VAL     0
#endif
      
#endif //ifndef COMBO_MCP

static int enable_combo_dis = 0;
extern int num_of_emi_records;
extern EMI_SETTINGS emi_settings[];
#define TIMEOUT 3
extern unsigned int g_ddr_reserve_enable;
extern unsigned int g_ddr_reserve_success;

#ifdef COMBO_MCP
void EMI_Init(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
void EMI_Init(DRAMC_CTX_T *p)
#endif
{
	//----------------EMI Setting--------------------
	*(volatile unsigned *)(EMI_APB_BASE+0x00000028)=0x08420000;  //Yi-chih's command
	*(volatile unsigned *)(EMI_APB_BASE+0x00000060)=0x40000500;  //disable EMI top DCM
	*(volatile unsigned *)(EMI_APB_BASE+0x00000140)=0x20406188;//0x12202488;   // 83 for low latency
	*(volatile unsigned *)(EMI_APB_BASE+0x00000144)=0x20406188;//0x12202488; //new add
	/* 
	*(volatile unsigned *)(EMI_APB_BASE+0x00000100)=0x40107a06;//0x40105808; //m0 cpu ori:0x8020783f
	// *(volatile unsigned *)(EMI_APB_BASE+0x00000108)=0x808070ea;//0xa0a05028; //m1 ori:0x80200000 // ultra can over limit
	*(volatile unsigned *)(EMI_APB_BASE+0x00000110)=0x808070d5; m2
	*(volatile unsigned *)(EMI_APB_BASE+0x00000118)=0x0810784a;//0x030fd80d; // ???????????????????????????????????????????????????????????//m3 mdmcu ori:0x80807809 ori:0x07007010 bit[12]:enable bw limiter
	*(volatile unsigned *)(EMI_APB_BASE+0x00000120)=0x30407042; //m4 fcore ori:0x8080781a
	*(volatile unsigned *)(EMI_APB_BASE+0x00000128)=0x808070d5; //m5 MM
	*(volatile unsigned *)(EMI_APB_BASE+0x00000130)=0x80807045; //m6 vcodec ori:0x8080381a

	*/
	*((UINT32P)(EMI_APB_BASE+0x00000100))= 0x7f077a49;
	*((UINT32P)(EMI_APB_BASE+0x00000110))= 0xa0a070dd;
	*((UINT32P)(EMI_APB_BASE+0x00000118))= 0x07007046;
	*((UINT32P)(EMI_APB_BASE+0x00000120))= 0x40407046;
	*((UINT32P)(EMI_APB_BASE+0x00000128))= 0xa0a070c6;
	*((UINT32P)(EMI_APB_BASE+0x00000130))= 0xffff7047;

	*(volatile unsigned *)(EMI_APB_BASE+0x00000148)=0x9719595e;//0323 chg, ori :0x00462f2f
	*(volatile unsigned *)(EMI_APB_BASE+0x0000014c)=0x9719595e; // new add
//	*(volatile unsigned *)(EMI_APB_BASE+0x00000000)=0x20202027; // DUAL CHANEEL

#ifdef COMBO_MCP
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000)=emi_set->EMI_CONA_VAL; 
#else
	if (p->dram_type==TYPE_LPDDR3)
	{    
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000)=LPDDR3_EMI_CONA; 
	}
	else
	{	    
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000)=PCDDR3_EMI_CONA;	 
	}
#endif	
	*(volatile unsigned *)(EMI_APB_BASE+0x000000f8)=0x00000000;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000400)=0x00ff0001;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000008)=0x17283544;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000010)=0x0a1a0b1a;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000018)=0x00000000; //SMI threthold
	*(volatile unsigned *)(EMI_APB_BASE+0x00000020)=0xFFFF0848;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000030)=0x2b2b2a38;
#ifdef COMBO_MCP
    *(volatile unsigned *)(EMI_APB_BASE+0x00000038)=emi_set->EMI_CONH_VAL; 
#else	
	#ifdef LPDDR3_EMI_CONH
	*(volatile unsigned *)(EMI_APB_BASE+0x00000038)=LPDDR3_EMI_CONH;
	#else
	*(volatile unsigned *)(EMI_APB_BASE+0x00000038)=0x00000000;
	#endif
#endif	
	*(volatile unsigned *)(EMI_APB_BASE+0x00000158)=0x00010800;// ???????????????????????0x08090800; 
	*(volatile unsigned *)(EMI_APB_BASE+0x00000078)=0x80030303;// ???????????0x00030F4d; 
	*(volatile unsigned *)(EMI_APB_BASE+0x0000015c)=0x80030303;// ??????????????????????????? 0x00030F4d;

	*(volatile unsigned *)(EMI_APB_BASE+0x00000150)=0x64f3fc79;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000154)=0x64f3fc79;




	//==============Scramble address==========================
	//============== Defer WR threthold
	*(volatile unsigned *)(EMI_APB_BASE+0x000000f0)=0x38470000;
	//============== Reserve bufer
	//
	//MDMCU don't always ULTRA, but small age
	#ifdef SBR
	*(volatile unsigned *)(EMI_APB_BASE+0x00000078)=0x3422cc3f;// defer ultra excpt MDMCU
	*(volatile unsigned *)(EMI_APB_BASE+0x000000f8)=0x00006000;// LPDDR3
	#else
	*(volatile unsigned *)(EMI_APB_BASE+0x00000078)=0x34220c3f;// defer ultra excpt MDMCU
	#endif
	#ifdef SBR
		#ifdef PBC_MASK
	        *(volatile unsigned *)(EMI_APB_BASE+0x000000e8)=0x00060124;// LPDDR3
	        #else
	        *(volatile unsigned *)(EMI_APB_BASE+0x000000e8)=0x00060324;// LPDDR3
		#endif
	#else
	*(volatile unsigned *)(EMI_APB_BASE+0x000000e8)=0x00060124;// LPDDR3
	#endif
	// Turn on M1 Ultra and all port DRAMC hi enable
	*(volatile unsigned *)(EMI_APB_BASE+0x00000158)=0xff03ff00;// ???????????????????????0x08090800; 
	// RFF)_PBC_MASK; [9] decrease noSBR push to DRAMC

	// Page hit is high 
	*(volatile unsigned *)(EMI_APB_BASE+0x00000060)=0x400005ff;
	*(volatile unsigned *)(EMI_APB_BASE+0x000000d0)=0xCCCCCCCC;//R/8 W/8 outstanding
	*(volatile unsigned *)(EMI_APB_BASE+0x000000d8)=0xcccccccc;//R/8 W/8 outstanding

	// check RESP error 
	//*((UINT32P)(EMI_APB_BASE+0x000001c0))=0x10000000;
	//*((UINT32P)(EMI_APB_BASE+0x000001c8))=0x10000000;
	//*((UINT32P)(EMI_APB_BASE+0x000001d0))=0x10000000;
	//*((UINT32P)(EMI_APB_BASE+0x00000200))=0x10000000;

        //*(volatile unsigned *)(EMI_APB_BASE+0x100)=0x7F007A49;
	//===========END===========================================	
#ifdef SBR_TEST
    *(volatile unsigned *)(EMI_APB_BASE+0x78)=0x3422fc3f;
    *(volatile unsigned *)(EMI_APB_BASE+0xf8)=0x6000;
    *(volatile unsigned *)(EMI_APB_BASE+0xe8)=0x603a7;
    *(volatile unsigned *)(EMI_APB_BASE+0x158)=0xff03ff00;
    *(volatile unsigned *)(EMI_APB_BASE+0x100)=0x00007845;
    *(volatile unsigned *)(EMI_APB_BASE+0x110)=0xa0a070d3;
    *(volatile unsigned *)(EMI_APB_BASE+0x128)=0xa0a070d3;
    *(volatile unsigned *)(EMI_APB_BASE+0x130)=0xffff704b;
#endif
}

void CHA_HWGW_Print(DRAMC_CTX_T *p)
{
	static U8 LowFreq_Min_R0_DQS[4] = {0xff, 0xff, 0xff, 0xff};
	static U8 LowFreq_Max_R0_DQS[4] = {0x00, 0x00, 0x00, 0x00};
	static U8 HighFreq_Min_R0_DQS[4] = {0xff, 0xff, 0xff, 0xff};
	static U8 HighFreq_Max_R0_DQS[4] = {0x00, 0x00, 0x00, 0x00};
	U8 ucstatus = 0, R0_DQS[4], Count;
	U32 u4value, u4value1;

	p->channel = CHANNEL_A;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x374), &u4value);
	R0_DQS[0] = (u4value >> 0) & 0x7f;
	R0_DQS[1] = (u4value >> 8) & 0x7f;
	R0_DQS[2] = (u4value >> 16) & 0x7f;
	R0_DQS[3] = (u4value >> 24) & 0x7f;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x94), &u4value1);
	
	mcSHOW_DBG_MSG(("[Channel %d]Clock=%d,Reg.94h=%xh,Reg.374h=%xh\n", p->channel, p->frequency, u4value1, u4value));

#ifdef DUAL_FREQ_TEST
	if (p->frequency == DUAL_FREQ_LOW)
	{
		for (Count=0; Count<4; Count++)
		{
			if (R0_DQS[Count] < LowFreq_Min_R0_DQS[Count])
			{
				LowFreq_Min_R0_DQS[Count] = R0_DQS[Count];
			}
			if (R0_DQS[Count]  > LowFreq_Max_R0_DQS[Count])
			{
				LowFreq_Max_R0_DQS[Count] = R0_DQS[Count];
			}
		}
		
		mcSHOW_DBG_MSG(("[Channel %d]Clock=%d,DQS0=(%d, %d),DQS1=(%d, %d),DQS2=(%d, %d),DQS3=(%d, %d)\n", 
			p->channel, p->frequency, 
			LowFreq_Min_R0_DQS[0], LowFreq_Max_R0_DQS[0], LowFreq_Min_R0_DQS[1], LowFreq_Max_R0_DQS[1], 
			LowFreq_Min_R0_DQS[2], LowFreq_Max_R0_DQS[2], LowFreq_Min_R0_DQS[3], LowFreq_Max_R0_DQS[3]));
	}
	else
	{
		for (Count=0; Count<4; Count++)
		{
			if (R0_DQS[Count] < HighFreq_Min_R0_DQS[Count])
			{
				HighFreq_Min_R0_DQS[Count] = R0_DQS[Count];
			}
			if (R0_DQS[Count]  > HighFreq_Max_R0_DQS[Count])
			{
				HighFreq_Max_R0_DQS[Count] = R0_DQS[Count];
			}
		}	
		mcSHOW_DBG_MSG(("[Channel %d]Clock=%d,DQS0=(%d, %d),DQS1=(%d, %d),DQS2=(%d, %d),DQS3=(%d, %d)\n", 
			p->channel, p->frequency, 
			HighFreq_Min_R0_DQS[0], HighFreq_Max_R0_DQS[0], HighFreq_Min_R0_DQS[1], HighFreq_Max_R0_DQS[1], 
			HighFreq_Min_R0_DQS[2], HighFreq_Max_R0_DQS[2], HighFreq_Min_R0_DQS[3], HighFreq_Max_R0_DQS[3]));
		
	}
#else
	for (Count=0; Count<4; Count++)
	{
		if (R0_DQS[Count] < LowFreq_Min_R0_DQS[Count])
		{
			LowFreq_Min_R0_DQS[Count] = R0_DQS[Count];
		}
		if (R0_DQS[Count]  > LowFreq_Max_R0_DQS[Count])
		{
			LowFreq_Max_R0_DQS[Count] = R0_DQS[Count];
		}
	}
	mcSHOW_DBG_MSG(("[Channel %d]Clock=%d,DQS0=(%d, %d),DQS1=(%d, %d),DQS2=(%d, %d),DQS3=(%d, %d)\n", 
		p->channel, p->frequency, 
		LowFreq_Min_R0_DQS[0], LowFreq_Max_R0_DQS[0], LowFreq_Min_R0_DQS[1], LowFreq_Max_R0_DQS[1], 
		LowFreq_Min_R0_DQS[2], LowFreq_Max_R0_DQS[2], LowFreq_Min_R0_DQS[3], LowFreq_Max_R0_DQS[3]));
	
#endif

#ifdef TEMP_SENSOR_ENABLE
	p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("[CHA] MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
	p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("[CHB] MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
#endif

}

static void Dump_Registers(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;
	U32 uiAddr;
	U32 u4value;

	if (p->channel == CHANNEL_A)
	{
		mcSHOW_DBG_MSG2(("Channel A registers dump...\n"));
	}
	else
	{
		mcSHOW_DBG_MSG2(("Channel B registers dump...\n"));
	}
	
	mcSHOW_DBG_MSG2(("EMI_CONA=%x\n",*(volatile unsigned *)(EMI_APB_BASE+0x00000000)));

	for (uiAddr=0; uiAddr<=0x690; uiAddr+=4)
	{
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(uiAddr), &u4value);
		mcSHOW_DBG_MSG2(("addr:%x, value:%x\n", uiAddr, u4value));
	
	}
}

static void Dump_EMI_Registers(void)
{
	U8 ucstatus = 0;
	U32 uiAddr;
	U32 u4value;

	for (uiAddr=0; uiAddr<=0x160; uiAddr+=8)
	{
	    u4value = 	(*(volatile unsigned int *)(EMI_APB_BASE + (uiAddr)));
		mcSHOW_DBG_MSG2(("addr:%x, value:%x\n", uiAddr, u4value));	
	}
}

#ifdef COMBO_MCP
void do_calib(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set, int skip_dual_freq_k)
#else
void do_calib(DRAMC_CTX_T *p, int skip_dual_freq_k)
#endif
{
	U8 ucstatus = 0;
    U32 u4value;
       
#if defined(DDR_INIT_TIME_PROFILING)
    /* enable ARM CPU PMU */
    asm volatile(
        "MRC p15, 0, %0, c9, c12, 0\n"
        "BIC %0, %0, #1 << 0\n"   /* disable */
        "ORR %0, %0, #1 << 2\n"   /* reset cycle count */
        "BIC %0, %0, #1 << 3\n"   /* count every clock cycle */
        "MCR p15, 0, %0, c9, c12, 0\n"
        : "+r"(temp)
        :
        : "cc"
    );
    asm volatile(
        "MRC p15, 0, %0, c9, c12, 0\n"
        "ORR %0, %0, #1 << 0\n"   /* enable */
        "MCR p15, 0, %0, c9, c12, 0\n"
        "MRC p15, 0, %0, c9, c12, 1\n"
        "ORR %0, %0, #1 << 31\n"
        "MCR p15, 0, %0, c9, c12, 1\n"
        : "+r"(temp)
        :
        : "cc"
    );

    mcDELAY_US(100);

	/* get CPU cycle count from the ARM CPU PMU */
	asm volatile(
	    "MRC p15, 0, %0, c9, c12, 0\n"
	    "BIC %0, %0, #1 << 0\n"   /* disable */
	    "MCR p15, 0, %0, c9, c12, 0\n"
	    "MRC p15, 0, %0, c9, c13, 0\n"
	    : "+r"(temp)
	    :
	    : "cc"
	);
	opt_print(" mcDELAY_US(100) takes %d CPU cycles\n\r", temp);
#endif

    // not necessary, marked
    //DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
		    			
#if defined(DDR_INIT_TIME_PROFILING)
    /* enable ARM CPU PMU */
    asm volatile(
        "MRC p15, 0, %0, c9, c12, 0\n"
        "BIC %0, %0, #1 << 0\n"   /* disable */
        "ORR %0, %0, #1 << 2\n"   /* reset cycle count */
        "BIC %0, %0, #1 << 3\n"   /* count every clock cycle */
        "MCR p15, 0, %0, c9, c12, 0\n"
        : "+r"(temp)
        :
        : "cc"
    );
    asm volatile(
        "MRC p15, 0, %0, c9, c12, 0\n"
        "ORR %0, %0, #1 << 0\n"   /* enable */
        "MCR p15, 0, %0, c9, c12, 0\n"
        "MRC p15, 0, %0, c9, c12, 1\n"
        "ORR %0, %0, #1 << 31\n"
        "MCR p15, 0, %0, c9, c12, 1\n"
        : "+r"(temp)
        :
        : "cc"
    );
#endif

// DDR_CHANNEL_INIT:

    p->channel = CHANNEL_A;
    DramcSwImpedanceCal((DRAMC_CTX_T *) p, 1);
    //DramcHwImpedanceCal((DRAMC_CTX_T *) p);			

#ifdef DUAL_FREQ_K
#if 0
    // No need for preloader because should already 1.125V here.
	#ifdef DUAL_FREQ_DIFF_VOLTAGE
	// LV 1.0V in low freq
	pmic_Vcore1_adjust(7);
	pmic_Vcore2_adjust(7);
	#else
	// HV 1.125V in low freq.
	pmic_Vcore1_adjust(6);
	pmic_Vcore2_adjust(6);
	pmic_voltage_read();
        #endif
#endif
#endif
	// Run again here for different voltage. For preloader, if the following code is executed after voltage change, no need.
	#ifdef SPM_CONTROL_AFTERK
	TransferToRegControl();
	#endif	        
	
	p->channel = CHANNEL_A;
	DramcEnterSelfRefresh(p, 1); 
	p->channel = CHANNEL_B;
	DramcEnterSelfRefresh(p, 1); 
	MemPllPreInit(p);	
	MemPllInit(p);		
	mcDELAY_US(1);
	mt_mempll_cali(p);
	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
	p->channel = CHANNEL_A;
	DramcEnterSelfRefresh(p, 0); // exit self refresh
	p->channel = CHANNEL_B;
	DramcEnterSelfRefresh(p, 0); // exit self refresh	

#ifdef DUAL_FREQ_K
DDR_CALI_START:	
    p->channel = CHANNEL_A;
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), &u4value);
    mcSET_FIELD(u4value, 0x0, MASK_ACTIM1_REFRCNT, POS_ACTIM1_REFRCNT);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), u4value);    
    p->channel = CHANNEL_B;
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), &u4value);
    mcSET_FIELD(u4value, 0x0, MASK_ACTIM1_REFRCNT, POS_ACTIM1_REFRCNT);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), u4value);        
#endif
	
	{
#ifdef MATYPE_ADAPTATION	
		// Backup here because Reg.04h may be modified based on different column address of different die or channel.
		// Default value should be the smallest number.
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x04), &u4Backup_Reg_04);
#endif

		// Calibration
#ifdef CA_WR_ENABLE    
	    p->channel = CHANNEL_A;
		DramcCATraining((DRAMC_CTX_T *) p);
	    p->channel = CHANNEL_B;
	    DramcCATraining((DRAMC_CTX_T *) p);

#ifdef COMBO_MCP
	    p->channel = CHANNEL_A;               
	    DramcWriteLeveling((DRAMC_CTX_T *) p, emi_set);
	    p->channel = CHANNEL_B;               
		DramcWriteLeveling((DRAMC_CTX_T *) p, emi_set);
#else
	    p->channel = CHANNEL_A;
	    DramcWriteLeveling((DRAMC_CTX_T *) p);
	    p->channel = CHANNEL_B;
		DramcWriteLeveling((DRAMC_CTX_T *) p);
#endif
#endif   
	#ifdef DUAL_RANKS
		if (uiDualRank) 
		{
		    p->channel = CHANNEL_A;
		    DualRankDramcRxdqsGatingCal((DRAMC_CTX_T *) p);
		    p->channel = CHANNEL_B;
			DualRankDramcRxdqsGatingCal((DRAMC_CTX_T *) p);
		}
		else
		{
		    p->channel = CHANNEL_A;
		    DramcRxdqsGatingCal((DRAMC_CTX_T *) p);	
		    p->channel = CHANNEL_B;
			DramcRxdqsGatingCal((DRAMC_CTX_T *) p);		
		}
	#else
            p->channel = CHANNEL_A;
	        DramcRxdqsGatingCal((DRAMC_CTX_T *) p);	
	        p->channel = CHANNEL_B;		
		DramcRxdqsGatingCal((DRAMC_CTX_T *) p);		
	#endif

		if (((DRAMC_CTX_T *) p)->fglow_freq_write_en==ENABLE)
		{
		    mcSHOW_DBG_MSG2(("**********************NOTICE*************************\n"));
			mcSHOW_DBG_MSG2(("Low speed write and high speed read calibration...\n"));
		    mcSHOW_DBG_MSG2(("*****************************************************\n"));
			// change low frequency and use test engine2 to write data, after write, recover back to the original frequency

		    // do channel A & B low frequency write simultaneously
		    CurrentRank = 0;
			DramcLowFreqWrite((DRAMC_CTX_T *) p);   
	#ifdef DUAL_RANKS			
		    if (uiDualRank) 
		    {
			    CurrentRank = 1;
			    // Swap CS0 and CS1.
			    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &u4value);
			    u4value = u4value |0x08;
			    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), u4value);
                
			    // do channel A & B low frequency write simultaneously
			    DramcLowFreqWrite((DRAMC_CTX_T *) p);	
                
			    // Swap CS back.
			    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &u4value);
			    u4value = u4value & (~0x08);
			    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), u4value);
			    CurrentRank = 0;
		    }
    #endif		   
        }
	#ifdef DUAL_RANKS			          		           
	        if (uiDualRank) 
	        {
		        p->channel = CHANNEL_A;
		        DramcDualRankRxdatlatCal((DRAMC_CTX_T *) p);
		        p->channel = CHANNEL_B;
		        DramcDualRankRxdatlatCal((DRAMC_CTX_T *) p);
	        }
	        else
	        {
		        p->channel = CHANNEL_A;
		        DramcRxdatlatCal((DRAMC_CTX_T *) p);
		        p->channel = CHANNEL_B;
		        DramcRxdatlatCal((DRAMC_CTX_T *) p);
	        }
	#else
		    p->channel = CHANNEL_A;
		    DramcRxdatlatCal((DRAMC_CTX_T *) p);
		    p->channel = CHANNEL_B;
		    DramcRxdatlatCal((DRAMC_CTX_T *) p);	
	#endif        
	
	#ifdef RX_DUTY_CALIBRATION
	p->channel = CHANNEL_A;
	DramcClkDutyCal(p);
	p->channel = CHANNEL_B;
	DramcClkDutyCal(p);
	#endif

	p->channel = CHANNEL_A;
    DramcRxWindowPerbitCal((DRAMC_CTX_T *) p);
	p->channel = CHANNEL_B;
	DramcRxWindowPerbitCal((DRAMC_CTX_T *) p);


	p->channel = CHANNEL_A;
	DramcTxWindowPerbitCal((DRAMC_CTX_T *) p);
	p->channel = CHANNEL_B;
    DramcTxWindowPerbitCal((DRAMC_CTX_T *) p);
/*
		if (((DRAMC_CTX_T *) p)->fglow_freq_write_en==ENABLE)
		{
			// after TX calibration, use high speed write to to RX DQS per bit calibration again
			((DRAMC_CTX_T *) p)->fglow_freq_write_en = DISABLE;
			if (((DRAMC_CTX_T *) p)->test_pattern==TEST_AUDIO_PATTERN)
			{
				DramcRxWindowPerbitCal((DRAMC_CTX_T *) p);
			}

               // Enable "Low frequency write" for channel B calibration
		if (((DRAMC_CTX_T *) p)->channel == CHANNEL_A)
		{
		    ((DRAMC_CTX_T *) p)->fglow_freq_write_en = ENABLE;
		}
	} */

	// Set here in order to save for frequency jump.
	p->channel = CHANNEL_A;
	DramcRANKINCTLConfig(p);
	p->channel = CHANNEL_B;
	DramcRANKINCTLConfig(p);

#ifdef DUAL_FREQ_K
	//p->channel = CHANNEL_A;
        //print_DBG_info();
	//p->channel = CHANNEL_B;
        //print_DBG_info();

  if(skip_dual_freq_k != 1)
  {        
	if (p->frequency == DUAL_FREQ_LOW)
	{
		DramcSaveFreqSetting(p);

#ifdef DUAL_FREQ_DIFF_VOLTAGE
		// HV 1.125V in high freq
		pmic_Vcore1_adjust(6);
		pmic_Vcore2_adjust(6);
		pmic_voltage_read();
#endif		

#if !defined(FREQ_BY_CHIP)		
		p->frequency = DUAL_FREQ_HIGH;
#else
        p->frequency = mt_get_dram_freq_setting();
        if(p->frequency == DUAL_FREQ_LOW)
            goto DDR_CALI_END;
#endif		
		DramcSwitchFreq(p, 1);

	#if defined(DUAL_FREQ_DIFF_ACTIMING) || defined(DUAL_FREQ_DIFF_RLWL)
	    #ifdef COMBO_MCP
	        p->channel = CHANNEL_B;
		    DramcPreInit((DRAMC_CTX_T *) p, emi_set);
		    p->channel = CHANNEL_A;
		    DramcPreInit((DRAMC_CTX_T *) p, emi_set);
            
		    DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
            
		    p->channel = CHANNEL_B;
		    DramcInit((DRAMC_CTX_T *) p, emi_set);		
		    p->channel = CHANNEL_A;
		    DramcInit((DRAMC_CTX_T *) p, emi_set);
	    #else
		    p->channel = CHANNEL_B;
		    DramcPreInit((DRAMC_CTX_T *) p);
		    p->channel = CHANNEL_A;
		    DramcPreInit((DRAMC_CTX_T *) p);
            
		    DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
            
		    p->channel = CHANNEL_B;
		    DramcInit((DRAMC_CTX_T *) p);		
		    p->channel = CHANNEL_A;
		    DramcInit((DRAMC_CTX_T *) p);
		#endif		
	/*#else
		#ifdef DUAL_FREQ_DIFF_RLWL
		p->channel = CHANNEL_B;
		DramcPreInit((DRAMC_CTX_T *) p);
		p->channel = CHANNEL_A;
		DramcPreInit((DRAMC_CTX_T *) p);
		DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
		#endif*/
	#endif
		goto DDR_CALI_START;
	} 
	else
	{
		DramcSaveFreqSetting(p);

 	}	
	//DramcDumpFreqSetting(p);
  } //!skip_dual_freq_k	

#endif
#ifdef MATYPE_ADAPTATION	
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x04), u4Backup_Reg_04);
#endif
	}
DDR_CALI_END:	
#if defined(DDR_INIT_TIME_PROFILING)
	/* get CPU cycle count from the ARM CPU PMU */
	asm volatile(
	    "MRC p15, 0, %0, c9, c12, 0\n"
	    "BIC %0, %0, #1 << 0\n"   /* disable */
	    "MCR p15, 0, %0, c9, c12, 0\n"
	    "MRC p15, 0, %0, c9, c13, 0\n"
	    : "+r"(temp)
	    :
	    : "cc"
	);
	opt_print("DRAMC calibration takes %d CPU cycles\n\r", temp);
#endif


	p->channel = CHANNEL_A;
	DramcRunTimeConfig(p);
	p->channel = CHANNEL_B;
	DramcRunTimeConfig(p);

#ifdef SPM_CONTROL_AFTERK
	TransferToSPMControl();
#endif

}

#ifdef COMBO_MCP
void Init_DRAM(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
void Init_DRAM(DRAMC_CTX_T *p)
#endif
{
	U8 ucstatus = 0;
    U32 u4value;
#ifdef MATYPE_ADAPTATION	
	U32 u4Backup_Reg_04;
#endif

    pmic_config_interface(0x8004, 0x0, 0x1, 7); // 0x8004 bit7 = 1'b0
#ifdef VBIASN_02V
    pmic_config_interface(0x544, 0x1 , 0x1F, 11); //0.2V
#else
    pmic_config_interface(0x544, 0x0, 0x1F, 11); //Set VbiasN to 0V
#endif    
    //Set for Vref at 0.6V when power on
    pmic_config_interface(0x8006, 0x007D, 0xFFFF, 0);
    pmic_config_interface(0x8008, 0x007D, 0xFFFF, 0);
    pmic_config_interface(0x800A, 0x007D, 0xFFFF, 0);
    mcDELAY_MS(25);	// According to ACD spec, need to delay 25ms in normal operation (DS1,DS0)=(1,0).

#ifdef COMBO_MCP
    EMI_Init(p, emi_set);   	

    p->channel = CHANNEL_B;
    DramcPreInit((DRAMC_CTX_T *) p, emi_set);
    p->channel = CHANNEL_A;
    DramcPreInit((DRAMC_CTX_T *) p, emi_set);

    DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

    p->channel = CHANNEL_B;
    DramcInit((DRAMC_CTX_T *) p, emi_set);		
    p->channel = CHANNEL_A;
    DramcInit((DRAMC_CTX_T *) p, emi_set);
#else
    EMI_Init(p);   	

    p->channel = CHANNEL_B;
    DramcPreInit((DRAMC_CTX_T *) p);
    p->channel = CHANNEL_A;
    DramcPreInit((DRAMC_CTX_T *) p);

    DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

    p->channel = CHANNEL_B;
    DramcInit((DRAMC_CTX_T *) p);		
    p->channel = CHANNEL_A;
    DramcInit((DRAMC_CTX_T *) p);		
#endif

#ifdef FTTEST_ZQONLY
    while (1)
    {
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_10);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);		// tZQINIT>=1us
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_10);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);		// tZQINIT>=1us
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    }
#endif
}

void release_dram(void)
{
#ifdef DDR_RESERVE_MODE  
    int counter = TIMEOUT;
    rgu_release_rg_dramc_conf_iso();
    rgu_release_rg_dramc_iso();
    rgu_release_rg_dramc_sref();
    while(counter)
    {
      if(rgu_is_dram_slf() == 0) /* expect to exit dram-self-refresh */
        break;
      counter--;
    }
    if(counter == 0)
    {
      if(g_ddr_reserve_enable==1 && g_ddr_reserve_success==1)
      {
        print("[DDR Reserve] release dram from self-refresh FAIL!\n");
        g_ddr_reserve_success = 0;
      }
    }
#endif    
}

void check_ddr_reserve_status(void)
{
#ifdef DDR_RESERVE_MODE  
    int counter = TIMEOUT;
    if(rgu_is_reserve_ddr_enabled())
    {
      g_ddr_reserve_enable = 1;
      if(rgu_is_reserve_ddr_mode_success())
      {
        while(counter)
        {
          if(rgu_is_dram_slf())
          {
            g_ddr_reserve_success = 1;
            break;
          }
          counter--;
        }
        if(counter == 0)
        {
          print("[DDR Reserve] ddr reserve mode success but DRAM not in self-refresh!\n");
          g_ddr_reserve_success = 0;
        }
      }
    else
      {
        print("[DDR Reserve] ddr reserve mode FAIL!\n");
        g_ddr_reserve_success = 0;
      }
      
      /* release dram, no matter success or failed */
      release_dram();
    }
    else
    {
      print("[DDR Reserve] ddr reserve mode not be enabled yet\n");
      g_ddr_reserve_enable = 0;
    }
#endif    
}

extern const U32 uiLPDDR_PHY_Mapping_POP_CHA[32];
extern const U32 uiLPDDR_PHY_Mapping_POP_CHB[32];
unsigned int DRAM_MRR(int MRR_num)
{
    unsigned int MRR_value = 0x0;
    unsigned int dram_type, ucstatus, u4value;
    DRAMC_CTX_T *p = psCurrDramCtx; 
          
    if ((p->dram_type == TYPE_LPDDR3) || (p->dram_type == TYPE_LPDDR2))
    {
        // set DQ bit 0, 1, 2, 3, 4, 5, 6, 7 pinmux
        if (p->channel == CHANNEL_A)
        {
            if (p->dram_type == TYPE_LPDDR3)
            {
                // refer to CA training pinmux array
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[0], MASK_RRRATE_CTL_BIT0_SEL, POS_RRRATE_CTL_BIT0_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[1], MASK_RRRATE_CTL_BIT1_SEL, POS_RRRATE_CTL_BIT1_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[2], MASK_RRRATE_CTL_BIT2_SEL, POS_RRRATE_CTL_BIT2_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[3], MASK_RRRATE_CTL_BIT3_SEL, POS_RRRATE_CTL_BIT3_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), u4value);            
                
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRR_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[4], MASK_MRR_CTL_BIT4_SEL, POS_MRR_CTL_BIT4_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[5], MASK_MRR_CTL_BIT5_SEL, POS_MRR_CTL_BIT5_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[6], MASK_MRR_CTL_BIT6_SEL, POS_MRR_CTL_BIT6_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[7], MASK_MRR_CTL_BIT7_SEL, POS_MRR_CTL_BIT7_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRR_CTL), u4value);                   
            }
            else // LPDDR2
            {
                //TBD
            }
        }
        else
        {
            if (p->dram_type == TYPE_LPDDR3)
            {
                // refer to CA training pinmux array
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[0], MASK_RRRATE_CTL_BIT0_SEL, POS_RRRATE_CTL_BIT0_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[1], MASK_RRRATE_CTL_BIT1_SEL, POS_RRRATE_CTL_BIT1_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[2], MASK_RRRATE_CTL_BIT2_SEL, POS_RRRATE_CTL_BIT2_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[3], MASK_RRRATE_CTL_BIT3_SEL, POS_RRRATE_CTL_BIT3_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), u4value);

                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRR_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[4], MASK_MRR_CTL_BIT4_SEL, POS_MRR_CTL_BIT4_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[5], MASK_MRR_CTL_BIT5_SEL, POS_MRR_CTL_BIT5_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[6], MASK_MRR_CTL_BIT6_SEL, POS_MRR_CTL_BIT6_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[7], MASK_MRR_CTL_BIT7_SEL, POS_MRR_CTL_BIT7_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRR_CTL), u4value);                
            }
            else // LPDDR2
            {
                //TBD
            }
        }

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), MRR_num);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000002);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        MRR_value = (u4value >> 20) & 0xFF;
    }    

    return MRR_value;
}

#ifdef COMBO_MCP
EMI_SETTINGS emi_setting_default_lpddr3 =
{

        //default
		0x0,		/* sub_version */
		0x0003,		/* TYPE */
		0,		/* EMMC ID/FW ID checking length */
		0,		/* FW length */
		{0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0},		/* NAND_EMMC_ID */
		{0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0},		/* FW_ID */
		//0x20102017,		/* EMI_CONA_VAL */
		0x50535057,
		0x00000000,		/* EMI_CONH_VAL */
		0x77FD474B,		/* DRAMC_ACTIM_VAL */
		0x11000000,		/* DRAMC_GDDR3CTL1_VAL */
		0x00048403,		/* DRAMC_CONF1_VAL */
		0x000053B1,		/* DRAMC_DDR2CTL_VAL */
		0xBFC40401,		/* DRAMC_TEST2_3_VAL */
		0x0000006C,		/* DRAMC_CONF2_VAL */
		0xD1646142,		/* DRAMC_PD_CTRL_VAL */
		0x91001E59,		/* DRAMC_ACTIM1_VAL*/
		0x17000000,		/* DRAMC_MISCTL0_VAL*/
		0x000004F1,		/* DRAMC_ACTIM05T_VAL*/
		0x002145C1,		/* DRAM_CRKCFG_VAL*/
		0x2701110D,		/* DRAMC_TEST2_4_VAL*/
		{0x80000000,0,0,0},		/* DRAM RANK SIZE */
		{0,0,0,0,0,0,0,0,0,0},		/* reserved 10 */
		0x00830001,		/* LPDDR3_MODE_REG1 */
		0x001C0002,		/* LPDDR3_MODE_REG2 */
		0x00020003,		/* LPDDR3_MODE_REG3 */
		0x00000006,		/* LPDDR3_MODE_REG5 */
		0x00FF000A,		/* LPDDR3_MODE_REG10 */
		0x0000003F,		/* LPDDR3_MODE_REG63 */
};

static int mt_get_dram_type_for_dis(void)
{
    int i;
    int type = 2;
    type = (emi_settings[0].type & 0xF);
    for (i = 0 ; i < num_of_emi_records; i++)
    {
      //print("[EMI][%d] type%d\n",i,type);
      if (type != (emi_settings[i].type & 0xF))
      {
          print("It's not allow to combine two type dram when combo discrete dram enable\n");
          ASSERT(0);
          break;
      }
    }
    return type;
}

static int mt_get_dram_density(void)
{
    int value, density, io_width;
    long long size;
    
    value = DRAM_MRR(8);
    io_width = ((value & 0xC0) >> 6)? 2 : 4; //0:32bit(4byte), 1:16bit(2byte)
    //print("[EMI]DRAM IO width = %d bit\n", io_width*8);
        
    density = (value & 0x3C) >> 2;
    switch(density)
    {
        case 0x6:
            size = 0x20000000;  //4Gb
            //print("[EMI]DRAM density = 4Gb\n");
            break;
        case 0xE:
            size = 0x30000000;  //6Gb
            //print("[EMI]DRAM density = 6Gb\n");
            break;
        case 0x7:
            size = 0x40000000;  //8Gb
            //print("[EMI]DRAM density = 8Gb\n");
            break;
        case 0xD:
            size = 0x60000000;  //12Gb
            //print("[EMI]DRAM density = 12Gb\n");
            break;
        case 0x8:
            size = 0x80000000;  //16Gb
            //print("[EMI]DRAM density = 16Gb\n");
            break;
        //case 0x9:
            //size = 0x100000000L; //32Gb
            //print("[EMI]DRAM density = 32Gb\n");
            //break;
        default:
            size = 0; //reserved
     }  
     return size; 
}

#if 0
static char id[22];
static int emmc_nand_id_len=16;
static int fw_id_len;
#endif

static int mt_get_mdl_number (void)
{
    static int found = 0;
    static int mdl_number = -1;
    int i;
    int j;
    int has_emmc_nand = 0;
    int discrete_dram_num = 0;
    int mcp_dram_num = 0;

    unsigned int mode_reg_5, dram_density, dram_channel_nr, dram_rank_nr;
    unsigned int dram_type;
    

    if (!(found))
    {
        int result=0;
        //platform_get_mcp_id (id, emmc_nand_id_len,&fw_id_len);
        for (i = 0 ; i < num_of_emi_records; i++)
        {
            if ((emi_settings[i].type & 0x0F00) == 0x0000) 
            {
                discrete_dram_num ++; 
            }
            else
            {
                mcp_dram_num ++; 
            }
        }

        /*If the number >=2  &&
         * one of them is discrete DRAM
         * enable combo discrete dram parse flow
         * */
        if ((discrete_dram_num > 0) && (num_of_emi_records >= 2))
        {
            /* if we enable combo discrete dram
             * check all dram are all same type and not DDR3
             * */
            enable_combo_dis = 1;
            dram_type = emi_settings[0].type & 0x000F;
            for (i = 0 ; i < num_of_emi_records; i++)
            {
                if (dram_type != (emi_settings[i].type & 0x000F))
                {
                    printf("[EMI] Combo discrete dram only support when combo lists are all same dram type.");
                    ASSERT(0);
                }
                if ((emi_settings[i].type & 0x000F) == TYPE_PCDDR3) 
                {
                    // has PCDDR3, disable combo discrete drame, no need to check others setting 
                    enable_combo_dis = 0; 
                    break;
                }
                dram_type = emi_settings[i].type & 0x000F;
            }
            
        } 
        printf("[EMI] mcp_dram_num:%d,discrete_dram_num:%d,enable_combo_dis:%d\r\n",mcp_dram_num,discrete_dram_num,enable_combo_dis);
        /*
         *
         * 0. if there is only one discrete dram, use index=0 emi setting and boot it.
         * */
        if ((0 == mcp_dram_num) && (1 == discrete_dram_num))
        {
            mdl_number = 0;
            found = 1;
            return mdl_number;
        }
            
#if 0
        /* 1.
         * if there is MCP dram in the list, we try to find emi setting by emmc ID
         * */
        if (mcp_dram_num > 0)
        {
            result = platform_get_mcp_id (id, emmc_nand_id_len,&fw_id_len);
            
            for (i = 0; i < num_of_emi_records; i++)
            {
                if (emi_settings[i].type != 0)
                {
                    if ((emi_settings[i].type & 0xF00) != 0x000)
                    {
                        if (result == 0)
                        {   /* valid ID */
            
                            if ((emi_settings[i].type & 0xF00) == 0x100)
                            {
                                /* NAND */
                                if (memcmp(id, emi_settings[i].ID, emi_settings[i].id_length) == 0){
                                    memset(id + emi_settings[i].id_length, 0, sizeof(id) - emi_settings[i].id_length);                                
                                    mdl_number = i;
                                    found = 1;
                                    break; /* found */
                                }
                            }
                            else
                            {
                                
                                /* eMMC */
                                if (memcmp(id, emi_settings[i].ID, emi_settings[i].id_length) == 0)
                                {
#if 1       
                                    printf("fw id len:%d\n",emi_settings[i].fw_id_length);
                                    if (emi_settings[i].fw_id_length > 0)
                                    {
                                        char fw_id[6];
                                        memset(fw_id, 0, sizeof(fw_id));
                                        memcpy(fw_id,id+emmc_nand_id_len,fw_id_len);
                                        for (j = 0; j < fw_id_len;j ++){
                                            printf("0x%x, 0x%x ",fw_id[j],emi_settings[i].fw_id[j]); 
                                        }
                                        if(memcmp(fw_id,emi_settings[i].fw_id,fw_id_len) == 0)
                                        {
                                            mdl_number = i;
                                            found = 1;
                                            break; /* found */
                                        }
                                        else
                                        {
                                            printf("[EMI] fw id match failed\n");
                                        }
                                    }
                                    else
                                    {
                                        mdl_number = i;
                                        found = 1;
                                        break; /* found */
                                    }
#else       
                                        mdl_number = i;
                                        found = 1;
                                        break; /* found */
#endif      
                                }
                                else{
                                      printf("[EMI] index(%d) emmc id match failed\n",i);
                                }
                                
                            }
                        }
                    }
                }
            }
        }
#endif        
#if 1
        /* 2. find emi setting by MODE register 5
         * */
        // if we have found the index from by eMMC ID checking, we can boot android by the setting
        // if not, we try by vendor ID
        if ((0 == found) && (1 == enable_combo_dis))
        {
            EMI_SETTINGS *emi_set;
            //print_DBG_info();
            //print("-->%x,%x,%x\n",emi_set->DRAMC_ACTIM_VAL,emi_set->sub_version,emi_set->fw_id_length); 
            //print("-->%x,%x,%x\n",emi_setting_default.DRAMC_ACTIM_VAL,emi_setting_default.sub_version,emi_setting_default.fw_id_length); 
            dram_type = mt_get_dram_type_for_dis();
            if (TYPE_LPDDR3 == dram_type)
            {
                print("[EMI] LPDDR3 discrete dram init\r\n");
                emi_set = &emi_setting_default_lpddr3;
                psCurrDramCtx = &DramCtx_LPDDR3;   
#if defined(FREQ_BY_CHIP) && !defined(DUAL_FREQ_K)    
                psCurrDramCtx->frequency = mt_get_dram_freq_setting();
#endif                   
                Init_DRAM(psCurrDramCtx, emi_set);
            }
            else if (TYPE_PCDDR3 == dram_type)
            {
                //TBD
            }
            //print_DBG_info();
            do_calib(psCurrDramCtx, emi_set, 1); //skip dual frequency calibration
     
            unsigned int manu_id = DRAM_MRR(0x5);
            print("[EMI]rank0: MR5:%x\n",manu_id);       
            
            //try to find discrete dram by DDR2_MODE_REG5(vendor ID)
            for (i = 0; i < num_of_emi_records; i++)
            {
                if (TYPE_LPDDR3 == dram_type)
                    mode_reg_5 = emi_settings[i].iLPDDR3_MODE_REG_5; 
                print("emi_settings[%d].MODE_REG_5:%x,emi_settings[%d].type:%x\n",i,mode_reg_5,i,emi_settings[i].type);
                //only check discrete dram type
                if ((emi_settings[i].type & 0x0F00) == 0x0000) 
                {
                    //support for compol discrete dram 
                    if ((mode_reg_5 == manu_id) )
                    {
                        dram_density = mt_get_dram_density();
                        dram_channel_nr = ((emi_settings[i].EMI_CONA_VAL & 0x1) == 0x1)? 2 : 1;
                        print("emi_settings[%d].DRAM_RANK_SIZE[0]:0x%x, dram_density:0x%x, dram_channel_nr:%d\n",i,emi_settings[i].DRAM_RANK_SIZE[0], dram_density, dram_channel_nr);                            
                        if(emi_settings[i].DRAM_RANK_SIZE[0] == dram_density * dram_channel_nr)
                        {  
                            dram_rank_nr = mt_get_rank_number();
                            if(dram_rank_nr == -1)
                            {
                                print("[EMI]Get dram rank number fail\n"); 
                                return mdl_number;
                            }
                            else if(dram_rank_nr == 2)
                            {
                                if((emi_settings[i].DRAM_RANK_SIZE[0] == 0) ||    
                                   (emi_settings[i].DRAM_RANK_SIZE[1] == 0))
                                    continue;
                            }
                            else if(dram_rank_nr == 1)
                            {
                                if((emi_settings[i].DRAM_RANK_SIZE[0] != 0) &&    
                                   (emi_settings[i].DRAM_RANK_SIZE[1] != 0))
                                    continue;
                            }
                            mdl_number = i;
                            found = 1;
                            break;
                        } 
                    }
                }
            }
        }
#endif
        printf("found:%d,i:%d\n",found,i);    
    }
    return mdl_number;
}

#endif //#if 0 

int get_dram_rank_nr (void)
{

    int index;
    int emi_cona;
#ifdef COMBO_MCP    
    index = mt_get_mdl_number ();
    if (index < 0 || index >=  num_of_emi_records)
    {
        return -1;
    }

    emi_cona = emi_settings[index].EMI_CONA_VAL;
#else
    emi_cona = EMI_CONA_VAL;
#if CFG_FPGA_PLATFORM
    return 1;
#endif
#endif

    if ((emi_cona & (1 << 17)) != 0 || //for channel 0  
        (emi_cona & (1 << 16)) != 0 )  //for channel 1
        return 2;
    else
        return 1;


}

int mt_get_dram_type (void)
{
    int n;
#ifdef COMBO_MCP     
   /* if combo discrete is enabled, the dram_type is LPDDR2 or LPDDR4, depend on the emi_setting list*/
    if ( 1 == enable_combo_dis)
    return mt_get_dram_type_for_dis();

    n = mt_get_mdl_number();

    if (n < 0  || n >= num_of_emi_records)
    {
        return 0; /* invalid */
    }

    return (emi_settings[n].type & 0xF);
#else
    //KT: set TYPE_LPDDR3 temporally, should be corrected by enabling combo MCP 
    return TYPE_LPDDR3;
#endif

}

#ifdef DUAL_FREQ_K
extern U32 PLL_LowFreq_RegVal[PLLGRPREG_SIZE], CHA_LowFreq_RegVal[FREQREG_SIZE], CHB_LowFreq_RegVal[FREQREG_SIZE];   
extern U32 PLL_HighFreq_RegVal[PLLGRPREG_SIZE], CHA_HighFreq_RegVal[FREQREG_SIZE], CHB_HighFreq_RegVal[FREQREG_SIZE];

mt_set_vcore_dvfs_info(vcore_dvfs_info_t* vcore_dvfs_info)
{
    vcore_dvfs_info->pll_setting_num = PLLGRPREG_SIZE;    
    vcore_dvfs_info->freq_setting_num = FREQREG_SIZE;      
    vcore_dvfs_info->low_freq_pll_setting_addr = PLL_LowFreq_RegVal;
    vcore_dvfs_info->low_freq_cha_setting_addr = CHA_LowFreq_RegVal;
    vcore_dvfs_info->low_freq_chb_setting_addr = CHB_LowFreq_RegVal;
    vcore_dvfs_info->high_freq_pll_setting_addr = PLL_HighFreq_RegVal;
    vcore_dvfs_info->high_freq_cha_setting_addr = CHA_HighFreq_RegVal;
    vcore_dvfs_info->high_freq_chb_setting_addr = CHB_HighFreq_RegVal;       

    printf("[vcore dvfs][preloader]low_freq_pll_setting_addr = 0x%x\n", vcore_dvfs_info->low_freq_pll_setting_addr);
    printf("[vcore dvfs][preloader]low_freq_cha_setting_addr = 0x%x\n", vcore_dvfs_info->low_freq_cha_setting_addr);
    printf("[vcore dvfs][preloader]low_freq_chb_setting_addr = 0x%x\n", vcore_dvfs_info->low_freq_chb_setting_addr);
    printf("[vcore dvfs][preloader]high_freq_pll_setting_addr = 0x%x\n", vcore_dvfs_info->high_freq_pll_setting_addr);
    printf("[vcore dvfs][preloader]high_freq_cha_setting_addr = 0x%x\n", vcore_dvfs_info->high_freq_cha_setting_addr);
    printf("[vcore dvfs][preloader]high_freq_chb_setting_addr = 0x%x\n", vcore_dvfs_info->high_freq_chb_setting_addr);
    printf("[vcore dvfs][preloader]pll_setting_num = %d\n", vcore_dvfs_info->pll_setting_num);
    printf("[vcore dvfs][preloader]freq_setting_num = %d\n", vcore_dvfs_info->freq_setting_num);
}
#else
mt_set_vcore_dvfs_info(vcore_dvfs_info_t* vcore_dvfs_info)
{
    vcore_dvfs_info->pll_setting_num = 0;    
    vcore_dvfs_info->freq_setting_num = 0;      
    vcore_dvfs_info->low_freq_pll_setting_addr = 0;
    vcore_dvfs_info->low_freq_cha_setting_addr = 0;
    vcore_dvfs_info->low_freq_chb_setting_addr = 0;
    vcore_dvfs_info->high_freq_pll_setting_addr = 0;
    vcore_dvfs_info->high_freq_cha_setting_addr = 0;
    vcore_dvfs_info->high_freq_chb_setting_addr = 0;       
}
#endif

/*
 * reserve a memory from mblock
 * @mblock_info: address of mblock_info 
 * @size: size of memory
 * @align: alignment, not implemented
 * @limit: address limit. Must higher than return address + size 
 * @rank: preferable rank, the returned address is in rank or lower ranks
 * It returns as high rank and high address as possible. (consider rank first)
 */
u64 mblock_reserve(mblock_info_t *mblock_info, u64 size, u64 align, u64 limit,
		enum reserve_rank rank)
{
	int i, max_rank, target = -1;
	u64 start, sz, max_addr = 0;

	if (size & (0x200000 - 1)) {
		printf("warning: size is not 2MB aligned\n");
	}

	if (rank == RANK0) {
		/* reserve memory from rank 0 */
		max_rank = 0;
	} else {
		/* reserve memory from any possible rank */
		/* mblock_num >= nr_ranks is true */
		max_rank = mblock_info->mblock_num - 1;
	}

	for (i = 0; i < mblock_info->mblock_num; i++) {
		start = mblock_info->mblock[i].start;
		sz = mblock_info->mblock[i].size;
		printf("mblock[%d].start: 0x%llx, sz: 0x%llx, limit: 0x%llx, "
				"max_addr: 0x%llx, max_rank: %d, target: %d, "
				"mblock[].rank: %d\n",
				i, start, sz, limit, max_addr, max_rank,
				target, mblock_info->mblock[i].rank);
		printf("mblock_reserve dbg[%d]: %d, %d, %d, %d\n",
				i, (start + sz <= limit),
				(mblock_info->mblock[i].rank <= max_rank),
				(start + sz > max_addr),
				 (sz >= size));
		if ((start + sz <= limit) &&
			(mblock_info->mblock[i].rank <= max_rank) &&
			(start + sz > max_addr) &&
			(sz >= size)) {
			max_addr = start + sz;
			target = i;
		}
	}

	if (target < 0) {
		printf("mblock_reserve error\n");
		return 0;
	} 

	mblock_info->mblock[target].size -= size;

	printf("mblock_reserve: %llx - %llx from mblock %d\n",
			(mblock_info->mblock[target].start
			+ mblock_info->mblock[target].size),
			(mblock_info->mblock[target].start
			+ mblock_info->mblock[target].size + size),
			target);


	return mblock_info->mblock[target].start + 
		mblock_info->mblock[target].size;
}

/* 
 * setup block correctly, we should hander both 4GB mode and 
 * non-4GB mode.
 */
void setup_mblock_info(mblock_info_t *mblock_info, dram_info_t *orig_dram_info,
		mem_desc_t *lca_reserved_mem)
{
	int i;
	u64 max_dram_size = -1; /* MAX value */
	u64 size = 0;
	u64 total_dram_size = 0;

	for (i = 0; i < orig_dram_info->rank_num; i++) {
		total_dram_size += 
			orig_dram_info->rank_info[i].size;
	}
#ifdef CUSTOM_CONFIG_MAX_DRAM_SIZE
	max_dram_size = CUSTOM_CONFIG_MAX_DRAM_SIZE;
	printf("CUSTOM_CONFIG_MAX_DRAM_SIZE: 0x%llx\n", max_dram_size);
#endif 
	lca_reserved_mem->start = lca_reserved_mem->size = 0;
	memset(mblock_info, 0, sizeof(mblock_info_t));

	/* 
	 * non-4GB mode case 
	 */
	/* we do some DRAM size fixup here base on orig_dram_info */
	for (i = 0; i < orig_dram_info->rank_num; i++) {
		size += orig_dram_info->rank_info[i].size;
		mblock_info->mblock[i].start = 
			orig_dram_info->rank_info[i].start;
		mblock_info->mblock[i].rank = i;	/* setup rank */
		if (size <= max_dram_size) {
			mblock_info->mblock[i].size = 
				orig_dram_info->rank_info[i].size;
		} else {
			/* max dram size reached */
			size -= orig_dram_info->rank_info[i].size;
			mblock_info->mblock[i].size = 
				max_dram_size - size;
			/* get lca_reserved_mem info */
			lca_reserved_mem->start = mblock_info->mblock[i].start
				+ mblock_info->mblock[i].size;
			if (mblock_info->mblock[i].size) {
				mblock_info->mblock_num++;
			}
			break;
		}

		if (mblock_info->mblock[i].size) {
			mblock_info->mblock_num++;
		}
	}
	
	printf("total_dram_size: 0x%llx, max_dram_size: 0x%llx\n",
			total_dram_size, max_dram_size);
	if (total_dram_size > max_dram_size) {
		/* add left unused memory to lca_reserved memory */
		lca_reserved_mem->size = total_dram_size - max_dram_size;
		printf("lca_reserved_mem start: 0x%llx, size: 0x%llx\n",
				lca_reserved_mem->start,
				lca_reserved_mem->size);
	}

	/*
	 * TBD
	 * for 4GB mode, we fixup the start address of every mblock
	 */
}

/* 
 * setup block correctly, we should hander both 4GB mode and 
 * non-4GB mode.
 */
void get_orig_dram_rank_info(dram_info_t *orig_dram_info)
{
	int i, j;
	u64 base = DRAM_BASE;
	unsigned int rank_size[4];

	orig_dram_info->rank_num = get_dram_rank_nr();
	get_dram_rank_size(rank_size);

	orig_dram_info->rank_info[0].start = base;
	for (i = 0; i < orig_dram_info->rank_num; i++) {

		orig_dram_info->rank_info[i].size = (u64)rank_size[i];

		if (i > 0) {
			orig_dram_info->rank_info[i].start =
				orig_dram_info->rank_info[i - 1].start +
				orig_dram_info->rank_info[i - 1].size;
		}
		printf("orig_dram_info[%d] start: 0x%llx, size: 0x%llx\n",
				i, orig_dram_info->rank_info[i].start,
				orig_dram_info->rank_info[i].size);
	}
	
	for(j=i; j<4; j++)
	{
	  		orig_dram_info->rank_info[j].start = 0;
	  		orig_dram_info->rank_info[j].size = 0;	
	}
}

void get_dram_rank_size (unsigned int dram_rank_size[])
{
#ifdef COMBO_MCP
    int index, rank_nr, i;

    index = mt_get_mdl_number();

    if (index < 0 || index >= num_of_emi_records)
    {
        return;
    }

    rank_nr = get_dram_rank_nr();

    for(i = 0; i < rank_nr; i++){
        dram_rank_size[i] = emi_settings[index].DRAM_RANK_SIZE[i];

        printf("%d:dram_rank_size:%x\n",i,dram_rank_size[i]);
    }

    return;
#else

    unsigned col_bit, row_bit, ch0_rank0_size, ch0_rank1_size, ch1_rank0_size, ch1_rank1_size;
    unsigned emi_cona = EMI_CONA_VAL, emi_conh = EMI_CONH_VAL;
 
    dram_rank_size[0] = 0;
    dram_rank_size[1] = 0;
    
    ch0_rank0_size = (emi_conh >> 16) & 0xf;
    ch0_rank1_size = (emi_conh >> 20) & 0xf;
    ch1_rank0_size = (emi_conh >> 24) & 0xf;
    ch1_rank1_size = (emi_conh >> 28) & 0xf;
    
    //Channel 0
    {   
        if(ch0_rank0_size == 0)
        {
            //rank 0 setting
            col_bit = ((emi_cona >> 4) & 0x03) + 9;
            row_bit = ((emi_cona >> 12) & 0x03) + 13;
            dram_rank_size[0] = (1 << (row_bit + col_bit)) * 4 * 8; // 4 byte * 8 banks
        }
        else
        {
            dram_rank_size[0] = (ch0_rank0_size * 256 << 20);
        }
 
        if (0 != (emi_cona &  (1 << 17)))   //rank 1 exist
        {
            if(ch0_rank1_size == 0)
            {
                col_bit = ((emi_cona >> 6) & 0x03) + 9;
                row_bit = ((emi_cona >> 14) & 0x03) + 13;
                dram_rank_size[1] = ((1 << (row_bit + col_bit)) * 4 * 8); // 4 byte * 8 banks
            }
            else
            {
                dram_rank_size[1] = (ch0_rank1_size * 256 << 20);
            }                
        }                                        
    }
    
    if(0 != (emi_cona & 0x01))     //channel 1 exist
    {
        if(ch1_rank0_size == 0)
        {                 
            //rank0 setting
            col_bit = ((emi_cona >> 20) & 0x03) + 9;
            row_bit = ((emi_cona >> 28) & 0x03) + 13;             
            dram_rank_size[0] += ((1 << (row_bit + col_bit)) * 4 * 8); // 4 byte * 8 banks
        }
        else
        {
            dram_rank_size[0] += (ch1_rank0_size * 256 << 20);            
        }
        
        if (0 != (emi_cona &  (1 << 16)))   //rank 1 exist
        {
            if(ch1_rank1_size == 0)
            {            
                col_bit = ((emi_cona >> 22) & 0x03) + 9;
                row_bit = ((emi_cona >> 30) & 0x03) + 13;
                dram_rank_size[1] += ((1 << (row_bit + col_bit)) * 4 * 8); // 4 byte * 8 banks
            }
            else
            {
                dram_rank_size[1] += (ch1_rank1_size * 256 << 20);
            } 
        }            
    }       
    
    printf("DRAM rank0 size:0x%x,\nDRAM rank1 size=0x%x\n", dram_rank_size[0], dram_rank_size[1]);    

    return;        
#endif
}

CHIP_TYPE mt_get_chip_type_by_efuse()
{
    int value;
    
    value = seclib_get_devinfo_with_index(24);
    //print("chip info = 0x%x\n",value);
    
    value = value >> 24;
    value &= 0xF;  //only need bit[3:0]
#ifdef MTK_DISABLE_EFUSE
    value = 6;
#endif     
    if(((value >= 0) && (value <= 5)) || ((value >= 11) && (value <= 15)))  
    {
        return CHIP_6595M;
    }
    else if((value >= 6) && (value <=10))
    {
        return CHIP_6595;
    }
    
    return -1;
}

int mt_get_dram_freq_setting()
{
    unsigned int value, freq;
    CHIP_TYPE chip_type;

#if defined(FREQ_BY_CHIP)
        
    if(mt_get_chip_sw_ver() == CHIP_SW_VER_01)  //MT6595 E1
    {
        freq = 666;
    }
    else
    {     
        //if((strcmp(LCM_WIDTH, "1440") == 0) && (strcmp(LCM_HEIGHT, "2560") == 0))  //WQHD
        if((LCM_WIDTH == 1440) && (LCM_HEIGHT == 2560))  //WQHD  
        {
            freq = 896;
        }
        else
        {   
            chip_type = mt_get_chip_type_by_efuse();
             
            if(chip_type == CHIP_6595)    //MT6595 E2
                freq = 896;
            else if(chip_type == CHIP_6595M)  //MT6595M
                freq = 666;
            else
                ASSERT(0);                
        }
    }
#else
    #ifdef DDR_667
        freq = 333;
    #elif defined (DDR_800)
        freq = 400;
    #elif defined (DDR_1066)
    	freq = 533;
    #elif defined (DDR_1333)
    	freq = 666;
    #elif defined (DDR_1600)
    	freq = 800;
    #elif defined (DDR_1780)
    	freq = 890;
    #elif defined (DDR_1866)
    	freq = 933;
    #elif defined (DDR_2000)
    	freq = 1000;
    #elif defined (DDR_2133)
    	freq = 1066;
    #elif defined (DDR_1420)
    	freq = 710;
    #elif defined (DDR_2400)
    	freq = 1200;
    #elif defined (DDR_1792)
	    freq = 896;
    #else		
    	freq = 890;
    #endif	 
#endif
    //print("mt_get_dram_freq_setting = %d\n", freq);
    return freq;       
}

#ifdef SUPPORT_DA
void mt_set_emi(EMI_SETTINGS *emi_set)
#else
void mt_set_emi(void)
#endif
{
	U32 ii, u4err_value;
	DRAMC_CTX_T *p;

#if 1
    U32 reg_val;
    //VCORE1 force PWM mode
    pmic_config_interface(0x035C,0x1,0x1,8);
    pmic_read_interface(0x035C, &reg_val, 0x1, 8);
    print("0x035C[8] = 0x%x\n", reg_val);
    //VCORE2 force PWM mode 
    pmic_config_interface(0x023E,0x1,0x1,8);
    pmic_read_interface(0x023E, &reg_val, 0x1, 8);
    print("0x023E[8] = 0x%x\n", reg_val);
#endif

#ifdef COMBO_MCP
  #ifndef SUPPORT_DA
    int index = 0;
    EMI_SETTINGS *emi_set;

    index = mt_get_mdl_number ();
    print("[Check]mt_get_mdl_number 0x%x\n",index);
    //print("[EMI] eMMC/NAND ID = %x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x\r\n", id[0], id[1], id[2], id[3], id[4], id[5], id[6], id[7], id[8],id[9],id[10],id[11],id[12],id[13],id[14],id[15]);
    if (index < 0 || index >=  num_of_emi_records)
    {
        print("[EMI] setting failed 0x%x\r\n", index);
        ASSERT(0);
    }
  
    print("[EMI] MDL number = %d\r\n", index);
    emi_set = &emi_settings[index];
  #endif //SUPPORT_DA   
    //print("[EMI] emi_set eMMC/NAND ID = %x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x,%x\r\n", emi_set->ID[0], emi_set->ID[1], emi_set->ID[2], emi_set->ID[3], emi_set->ID[4], emi_set->ID[5], emi_set->ID[6], emi_set->ID[7], emi_set->ID[8],emi_set->ID[9],emi_set->ID[10],emi_set->ID[11],emi_set->ID[12],emi_set->ID[13],emi_set->ID[14],emi_set->ID[15]);
    if ((emi_set->type & 0xF) == TYPE_LPDDR3)
    {
        p = psCurrDramCtx = &DramCtx_LPDDR3;   
#if defined(FREQ_BY_CHIP) && !defined(DUAL_FREQ_K)        
        p->frequency = mt_get_dram_freq_setting();
#endif                  
        Init_DRAM(p, emi_set);
    }
    else if ((emi_set->type & 0xF) == TYPE_PCDDR3)
    {
        p = psCurrDramCtx = &DramCtx_PCDDR3;
#if defined(FREQ_BY_CHIP) && !defined(DUAL_FREQ_K)           
        p->frequency = mt_get_dram_freq_setting();
#endif        
        Init_DRAM(p, emi_set);
    }
    else
    {
        print("The DRAM type is not supported");
        ASSERT(0);
    }

    do_calib(p, emi_set, 0);
    
#else

#ifdef DDRTYPE_LPDDR3
	p = psCurrDramCtx = &DramCtx_LPDDR3;
#endif

#ifdef DDRTYPE_DDR3
	p = psCurrDramCtx = &DramCtx_PCDDR3;
#endif
	
#ifdef defined(FREQ_BY_CHIP) && !defined(DUAL_FREQ_K)    
        p->frequency = mt_get_dram_freq_setting();
#endif  
	
	Init_DRAM(p);
	
    do_calib(p, 0);	
#endif

    //set VCORE1 to auto mode
    pmic_config_interface(0x035C,0x0,0x1,8);
    //set VCORE2 to auto mode 
    pmic_config_interface(0x023E,0x0,0x1,8);
    
	p->channel = CHANNEL_A;
	print("\n\nChannel A setting after calibration ...\n\n");
    Dump_Registers(p);
	p->channel = CHANNEL_B;
	print("\n\nChannel B setting after calibration ...\n\n");
    Dump_Registers(p);
    print("\n\nEMI setting ...\n\n");    
    Dump_EMI_Registers();
    
	// Single rank test.
	for (ii=0; ii<2; ii++) {
		u4err_value = DramcEngine2((DRAMC_CTX_T *) p, TE_OP_WRITE_READ_CHECK, 0x55000000, 0xaa010000, 2, 0, 0, (U8)ii);
		mcSHOW_DBG_MSG(("[A60808_MISC_CMD_TA2_XTALK-%d] err_value=0x%8x\n", ii, u4err_value));
	}
}

void enable_4GB_mode(void)
{
	unsigned int i;
	u32 dram_rank_size[4] = {0,0,0,0};
	u64 total_dram_size = 0;

	get_dram_rank_size(dram_rank_size);

	for(i = 0; i < 4; i++){
		total_dram_size += dram_rank_size[i];
	}

	if(total_dram_size > 0xC0000000ULL) {
		print("[Enable 4GB Support] Total_dram_size = 0x%llx\n", total_dram_size);
		*(volatile unsigned int *)(0x10003208) |= 1 << 15;
		*(volatile unsigned int *)(0x10001f00) |= 1 << 13;
	}
}
