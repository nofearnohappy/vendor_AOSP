#include "unistd.h"
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
//#include <linux/delay.h>
#include "msdc_register.h"
#include "x_typedef.h"
#include <string.h>
#include <android/log.h>
#include "ett_core.h"


#include <cutils/properties.h> //ccyeh


#define TARGET_CLOCK (200 * 1000 * 1000)
#define CLI_MAGIC 'L'
#define IOCTL_READ _IOR(CLI_MAGIC, 0, int)
#define IOCTL_WRITE _IOW(CLI_MAGIC, 1, int)
#define  LOG_TAG    "sdio_ett"

char ett_test_init[]="dev.ETT_test_init";
char ett_test_exit[]="dev.ETT_test_exit";
char ett_test_mode_change[]="dev.ETT_test_mode_change";
char ett_test_cmd[]="dev.ETT_test_cmd";
char ett_test_read[]="dev.ETT_test_read";
char ett_test_write[]="dev.ETT_test_write";
char ett_test_recovery[]="dev.ETT_test_recovery ";


//int msdc_tune_fd = -1;
int msdc_debug_fd = -1;
int mt65x2_pmic_fd = -1;
FILE * ett_result_fd;
int TEST_MSDC_PORT  = 3;
extern struct ETT_DATA g_ettdata;
/*-----------------------------------------------------------------
  Following definition is provieded by designer PVT simulation result
  The data is used to calculating the auto-K stage1 range spec
-------------------------------------------------------------------*/
/*Following definition is provided by spec 3.0*/
#define DIV_CEIL_FUNC(_n,_d) ((_n)/(_d)+(((_n)%(_d)==0)?0:1))
#define MAX_DELAY_VARIATION_DUE_TO_TEMPERATURE_IN_PS (2600) /*-25degC~~+125degC*/
#define F208M_CYCLE_IN_PS (4808)

#define MIN_CLK_GEN_DELAY_IN_PS (12123)
#define MAX_CLK_GEN_DELAY_IN_PS (32726)
#define MIN_PAD_DELAY_IN_PS (3111) /*bc_RCcbest : FF/LT/HV*/
#define MAX_PAD_DELAY_IN_PS (8618) /*wcl_RCcworst SS/LT/LV*/
#define SCALE_OF_CLK_GEN_2_PAD_DELAY (MIN_CLK_GEN_DELAY_IN_PS/MIN_PAD_DELAY_IN_PS)

#define MAX_SCORE_OF_PAD_DELAY_AGAINST_TEMP_VAR \
       (DIV_CEIL_FUNC((SCALE_PAD_TUNE_CMDRDLY*MAX_DELAY_VARIATION_DUE_TO_TEMPERATURE_IN_PS), MIN_PAD_DELAY_IN_PS))

#define MIN_SCORE_OF_PAD_DELAY_AGAINST_TEMP_VAR \
       (DIV_CEIL_FUNC((SCALE_PAD_TUNE_CMDRDLY*MAX_DELAY_VARIATION_DUE_TO_TEMPERATURE_IN_PS), MAX_PAD_DELAY_IN_PS))

/*CMD*/
#define SCALE_CMD_RSP_TA_CNTR         (8)
#define SCALE_CMD_RSP_DLY             (32)
#define SCALE_CMD_IOCON_RSPL          (2)
#define SCALE_CKGEN_MSDC_DLY_SEL      (32)
#define SCALE_PAD_TUNE_CMDRDLY        (32)
/*READ*/
#define SCALE_DATA_DRIVING            (8)
#define SCALE_INT_DAT_LATCH_CK_SEL    (8)
#define SCALE_IOCON_RDSPL             (2)
#define SCALE_PAD_TUNE_DATRDDLY       (32)
/*WRITE*/
#define SCALE_WRDAT_CRCS_TA_CNTR      (8)
#define SCALE_IOCON_WDSPL            (2)
#define SCALE_PAD_TUNE_DATWRDLY       (32)

#define TUNING_INACCURACY (2)
#define AUTOK_TUNING_INACCURACY     2
#define REAL_PASS_WINDOW         (MAX_SCORE_OF_PAD_DELAY_AGAINST_TEMP_VAR)
#define PASS_WINDOW  (MIN_SCORE_OF_PAD_DELAY_AGAINST_TEMP_VAR-TUNING_INACCURACY)


/* Message verbosity: lower values indicate higher urgency */
#define ETT_DBG_OFF                             0
#define ETT_DBG_ERROR                           1
#define ETT_DBG_RESULT                          2
#define ETT_DBG_WARN                            3
#define ETT_DBG_INFO                            4
#define ETT_DBG_LOUD                            5


typedef struct
{
    u32 reg;
    u32 offset;
    u32 len;
    u32 threshold;
    u8  scale;
    u8 sel;
    u8 range[2];
    BOOL bRange;
    char* name;
}S_SDETT_TUNE;

typedef struct
{
    u8  cmd_resp_gear;
    u8  ta_gear;
    u8  int_data_latch_gear;
    u8  spl_gear;
    u8  fIntDatLatchCKChng;
    u8  IntDatLatchCK;
    u32 raw;
    u32 score;
}S_ETT_TUNE_XDATA;

typedef struct
{
    unsigned int RawData;

    unsigned int BoundReg1_S;
    unsigned int BoundReg1_E;
    unsigned int Reg1Cnt;
    unsigned int BoundReg2_S;
    unsigned int BoundReg2_E;
    unsigned int Reg2Cnt;

    unsigned char fInvalidCKGEN;
    unsigned char CurCKGEN;

}AUTOK_RAWD_SCAN_T, *P_AUTOK_RAWD_SCAN_T;

typedef enum
{
    TUNE_INIT = 0,
    TUNE_CMD,
    TUNE_READ,
    TUNE_WRITE,
    TUNE_RESULT_SHOW,
    TUNE_FAIL,
    TUNE_DONE
}E_ETT_STATE;

typedef enum
{
    E_RESULT_PASS = 0,
    E_RESULT_CMD_CRC = 1,
    E_RESULT_W_CRC = 2,
    E_RESULT_R_CRC = 3,
    E_RESULT_ERR = 4,
    E_RESULT_START = 5,
    E_RESULT_PW_SMALL = 6,
    E_RESULT_KEEP_OLD = 7,
    E_RESULT_TO = 8,
    E_RESULT_CMP_ERR = 9,
    E_RESULT_MAX
}E_RESULT_TYPE;

typedef enum
{
    RD_SCAN_NONE,
    RD_SCAN_PAD_BOUND_S,
    RD_SCAN_PAD_BOUND_E,
    RD_SCAN_PAD_BOUND_S_2,
    RD_SCAN_PAD_BOUND_E_2,
    RD_SCAN_PAD_MARGIN,

}AUTOK_RAWD_SCAN_STA_E;

typedef enum
{
/*CMD*/
    MSDC_PAD_TUNE_CMDRRDLY = 0,
    MSDC_PATCH_BIT1_CMD_RSP,
    MSDC_IOCON_RSPL,
    MSDC_PATCH_BIT0_CKGENDLSEL,
    MSDC_PAD_TUNE_CMDRDLY,
/*READ*/
    MSDC_PATCH_BIT0_INTCKSEL,

    MSDC_IOCON_DSPL,
    MSDC_PAD_TUNE_DATRRDLY,
/*WRITE*/
    MSDC_PATCH_BIT1_WRDAT_CRCS,
    MSDC_IOCON_WDSPL,
    MSDC_PAD_TUNE_DATWRDLY,
    MSDC_ETT_TUNE_PARM_MAX,
/*Following the parameter we had to set*/
    MSDC_IOCON_DDLSEL = MSDC_ETT_TUNE_PARM_MAX,
    MSDC_IOCON_DSPLSEL,
    MSDC_IOCON_WDSPLSEL,
    MSDC_ETT_ALL_PARAM
}E_MSDC_ETT_PARAMETER;

static S_SDETT_TUNE g_ett_tune[MSDC_ETT_TUNE_PARM_MAX] =
{
/*CMD*/
    {MSDC_PAD_TUNE,     OFFSET_MSDC_PAD_TUNE_CMDRRDLY,         LEN_MSDC_PAD_TUNE_CMDRRDLY,        0,           SCALE_CMD_RSP_DLY,        10,{0,0},    FALSE,  (char*)"PAD_CMD_RESP_RXDLY"},
    {MSDC_PATCH_BIT1,   OFFSET_MSDC_PATCH_BIT1_CMD_RSP,        LEN_MSDC_PATCH_BIT1_CMD_RSP,       0,       SCALE_CMD_RSP_TA_CNTR,         3,{0,0},    FALSE,  (char*)"CMD_RSP_TA_CNTR"},
    {MSDC_IOCON,        OFFSET_MSDC_IOCON_RSPL,                LEN_MSDC_IOCON_RSPL,               0,        SCALE_CMD_IOCON_RSPL,         0,{0,0},    FALSE,  (char*)"R_SMPL"},
    {MSDC_PATCH_BIT0,   OFFSET_MSDC_PATCH_BIT0_CKGENDLSEL,     LEN_MSDC_PATCH_BIT0_CKGENDLSEL,    0,    SCALE_CKGEN_MSDC_DLY_SEL,         2,{0,0},    FALSE,  (char*)"CKGEN_MSDC_DLY_SEL"},
    {MSDC_PAD_TUNE,     OFFSET_MSDC_PAD_TUNE_CMDRDLY,          LEN_MSDC_PAD_TUNE_CMDRDLY,         0,      SCALE_PAD_TUNE_CMDRDLY,        15,{0,0},    FALSE,  (char*)"PAD_CMD_RXDLY"},
/*READ*/
    {MSDC_PATCH_BIT0,   OFFSET_MSDC_PATCH_BIT0_INTCKSEL,       LEN_MSDC_PATCH_BIT0_INTCKSEL,      0,  SCALE_INT_DAT_LATCH_CK_SEL,         0,{0,0},    FALSE,  (char*)"INT_DAT_LATCH_CK_SEL"},
    {MSDC_IOCON,        OFFSET_MSDC_IOCON_DSPL,                LEN_MSDC_IOCON_DSPL,               0,           SCALE_IOCON_RDSPL,         0,{0,0},    FALSE,  (char*)"R_D_SMPL"},
    {MSDC_PAD_TUNE,     OFFSET_MSDC_PAD_TUNE_DATRRDLY,         LEN_MSDC_PAD_TUNE_DATRRDLY,        0,     SCALE_PAD_TUNE_DATRDDLY,        15,{0,0},    FALSE,  (char*)"PAD_DATA_RD_RXDLY"},
/*WRITE*/
    {MSDC_PATCH_BIT1,   OFFSET_MSDC_PATCH_BIT1_WRDAT_CRCS,     LEN_MSDC_PATCH_BIT1_WRDAT_CRCS,    0,    SCALE_WRDAT_CRCS_TA_CNTR,         1,{0,0},    FALSE,  (char*)"WRDAT_CRCS_TA_CNTR"},
    {MSDC_IOCON,        OFFSET_MSDC_IOCON_WDSPL,               LEN_MSDC_IOCON_WDSPL,              0,           SCALE_IOCON_WDSPL,         0,{0,0},    FALSE,  (char*)"W_D_SMPL"},
    {MSDC_PAD_TUNE,     OFFSET_MSDC_PAD_TUNE_DATWRDLY,         LEN_MSDC_PAD_TUNE_DATWRDLY,        0,     SCALE_PAD_TUNE_DATWRDLY,        15,{0,0},    FALSE,  (char*)"PAD_DATA_WR_RXDLY"},
};

//static u32 g_cmdrrdlySel[SCALE_CMD_RSP_DLY];
static u32 g_ta_raw[SCALE_CMD_RSP_TA_CNTR];
static u32 g_ta_score[SCALE_CMD_RSP_TA_CNTR];

static S_ETT_TUNE_XDATA g_cmd_clkgen_data[SCALE_CKGEN_MSDC_DLY_SEL];
static u32 g_cmd_score[SCALE_CKGEN_MSDC_DLY_SEL*SCALE_CMD_RSP_TA_CNTR][SCALE_CMD_IOCON_RSPL];
static u32 g_cmd_tune[SCALE_CKGEN_MSDC_DLY_SEL][SCALE_CMD_IOCON_RSPL];


static S_ETT_TUNE_XDATA g_read_clkgen_data[SCALE_CKGEN_MSDC_DLY_SEL];
static u32 g_read_data_tune[SCALE_CKGEN_MSDC_DLY_SEL][SCALE_INT_DAT_LATCH_CK_SEL][SCALE_IOCON_RDSPL];
static u32 g_read_data_score[SCALE_CKGEN_MSDC_DLY_SEL][SCALE_INT_DAT_LATCH_CK_SEL][SCALE_IOCON_RDSPL];

static S_ETT_TUNE_XDATA g_write_clkgen_data[SCALE_CKGEN_MSDC_DLY_SEL];
static u32 g_write_data_tune[SCALE_CKGEN_MSDC_DLY_SEL][SCALE_WRDAT_CRCS_TA_CNTR][SCALE_IOCON_WDSPL];
static u32 g_write_data_score[SCALE_CKGEN_MSDC_DLY_SEL][SCALE_WRDAT_CRCS_TA_CNTR][SCALE_IOCON_WDSPL];


static unsigned int g_ett_backup_param[MSDC_ETT_ALL_PARAM];

static u32 g_cmd_ta_start = 1;
static u32 g_wrcrc_ta_start = 1;
static char g_tune_result_str[33];
u32 cmd_times = 1;
u32 rdata_times = 1;
u32 wdata_times = 1;

char g_platform[PROPERTY_VALUE_MAX] = {0}; //ccyeh

extern int msdc_register_write(unsigned int reg, unsigned int offset, unsigned int len, unsigned int value);
static E_RESULT_TYPE sd30_tune_cmd(u32 tst_times);
static E_RESULT_TYPE sd30_tune_read(u32 tst_mode, u32 tst_times);
static E_RESULT_TYPE sd30_tune_write(u32 tst_times);
static E_RESULT_TYPE sd30_tune_result_show(void);


#define MSDC_SET_FIELD(_reg, _field, _val) \
{ \
    if( (_reg == 0xec) && (strcmp(g_platform,"MT6735")==0) ) { \
        msdc_register_write(0xf0, (unsigned int)OFFSET_##_field, (unsigned int)LEN_##_field, (unsigned int)_val); \
    } else { \    	
        msdc_register_write((unsigned int)_reg, (unsigned int)OFFSET_##_field, (unsigned int)LEN_##_field, (unsigned int)_val); } \
    g_ett_backup_param[_field] = _val; \
}

#define MSDC_RESTORE_REG(_reg, _field)  \
        msdc_register_write(_reg, OFFSET_##_field, LEN_##_field, g_ett_backup_param[_field]);

#define MSDC_SET_APPLY(_logLvl, _reg, _field, _val) { \
    g_ett_tune[_field].sel = (_val); \
    MSDC_SET_FIELD(_reg, _field, (_val)); \
    ETT_DBGPRINT(_logLvl, "%s = %d \r\n", g_ett_tune[_field].name, g_ett_tune[_field].sel); \
}

static u32 g_ett_debug_level = ETT_DBG_WARN;

#define ETT_DBGPRINT(_level, _fmt, _args ...)		\
({												\
	if (g_ett_debug_level >= _level){		\
		__android_log_print(_level, LOG_TAG, _fmt, _args) ;							\
		fprintf(ett_result_fd, _fmt, _args);           \
		fflush(ett_result_fd);	\
	}											\
})

#define ETT_DBG_RAW_PRINT(_level, _fmt)		\
({												\
	if (g_ett_debug_level >= _level){		\
		__android_log_print(_level, LOG_TAG,_fmt) ;							\
		fprintf(ett_result_fd, _fmt);           \
		fflush(ett_result_fd);	\
	}											\
})

int msdc_register_write(unsigned int reg, unsigned int offset, unsigned int len, unsigned int value)
{
    char msdc_reg_wr_buf[50];

    sprintf(msdc_reg_wr_buf,"5 2 %x %x %x %x %x\n", TEST_MSDC_PORT, reg, offset, len, value);
    write(msdc_debug_fd, msdc_reg_wr_buf, strlen(msdc_reg_wr_buf));
    //printf("The clock mode is 0x%x & div is 0x%x now. \n", mode, div);
    return 0;
}



int write_cmd_no_output(char *buf)
{
	//negative value is returned if there is problem
	char xbuf[256];
    int result = 0;
    int length;
    char path[512];
    const char TNODE[] = "/sys/mtk_sdio/test";
    int i;
    //printf("PreCurrent command:%s\n", buf);
    if(strstr(buf, ett_test_init)!=NULL){
        //echo 4314 3 1 > /proc/msdc_tune
        int ett_init_fd = -1;
        memset(path, 0, sizeof(path)/sizeof(char));
        sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "CMD");
        if ((ett_init_fd = open(path, O_RDWR)) < 0){
            printf("Can't open %s", path);
        }
        sprintf(xbuf,"1");
        write(ett_init_fd, xbuf, strlen(xbuf));
        close(ett_init_fd);
    } else if(strstr(buf, ett_test_exit)!=NULL){
        //echo 4314 3 1 > /proc/msdc_tune
        int ett_exit_fd = -1;
        memset(path, 0, sizeof(path)/sizeof(char));
        sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "CMD");
        if ((ett_exit_fd = open(path, O_RDWR)) < 0){
            printf("Can't open %s", path);
        }
        sprintf(xbuf,"0");
        write(ett_exit_fd, xbuf, strlen(xbuf));
        close(ett_exit_fd);
    } else if(strstr(buf, ett_test_cmd)!=NULL){
        //cat /sys/mtk_sdio/test/[ID]/CMD
        int test_cmd_fd = -1;
        memset(path, 0, sizeof(path)/sizeof(char));
        sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "CMD");
        for(i=0; i<cmd_times; i++){
        	if ((test_cmd_fd = open(path, O_RDWR)) < 0)
        	{
        		printf("open %s fail\n", path);
        		return -1;
        	}
        	memset(xbuf, 0, 256);
        	if ((length = read(test_cmd_fd, xbuf, 256)) == -1) {
        		printf("Can't read %s\n", path);
        	}
        	close(test_cmd_fd);
        	sscanf(xbuf, "%d\n", &result);
        	if(result!=0)
        		break;
        }
    } else if(strstr(buf, ett_test_read)!=NULL){
        //cat /sys/mtk_sdio/test/[ID]/DATA_READ
    	int data_read_fd = -1;
    	memset(path, 0, sizeof(path)/sizeof(char));
    	sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "DATA_READ");
    	for(i=0; i<rdata_times; i++){
    		if ((data_read_fd = open(path, O_RDWR)) < 0)
    		{
    			printf("open %s fail\n", path);
    			return -1;
    		}
    		memset(xbuf, 0, 256);
    		if ((length = read(data_read_fd, xbuf, 256)) == -1) {
    			printf("Can't read %s\n", path);
    		}
    		close(data_read_fd);
    		sscanf(xbuf, "%d\n", &result);
    		if(result!=0)
    			break;
    	}
    } else if(strstr(buf, ett_test_write)!=NULL){
        //cat /sys/mtk_sdio/test/[ID]/DATA_WRITE
    	int data_write_fd = -1;
    	memset(path, 0, sizeof(path)/sizeof(char));
    	sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "DATA_WRITE");
    	for(i=0; i<wdata_times; i++){
			if ((data_write_fd = open(path, O_RDWR)) < 0)
			{
				printf("open %s fail\n", path);
				return -1;
			}
			memset(xbuf, 0, 256);
			if ((length = read(data_write_fd, xbuf, 256)) == -1) {
				printf("Can't read %s\n", path);
			}
			close(data_write_fd);
			sscanf(xbuf, "%d\n", &result);
			if(result!=0)
				break;
		}
    } else if(strstr(buf, ett_test_recovery)!=NULL){
        //echo 1 > /sys/mtk_sdio/meta/[ID]
      int ett_recovery_fd = -1;
      memset(path, 0, sizeof(path)/sizeof(char));
      sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "DATA_WRITE");
      if ((ett_recovery_fd = open(path, O_RDWR)) < 0){
        printf("Can't open %s", path);
      }
      sprintf(xbuf,"1");
      write(ett_recovery_fd, xbuf, strlen(xbuf));
      close(ett_recovery_fd);
    } else if(strstr(buf, ett_test_mode_change)!=NULL){
        //echo 0 > /sys/mtk_sdio/meta/[ID]
      int ett_mc_fd = -1;
      memset(path, 0, sizeof(path)/sizeof(char));
      sprintf(path, "%s/%d/%s", TNODE, TEST_MSDC_PORT, "DATA_READ");
      if ((ett_mc_fd = open(path, O_RDWR)) < 0){
        printf("Can't open %s", path);
      }
      sprintf(xbuf,"1");
      write(ett_mc_fd, xbuf, strlen(xbuf));
      close(ett_mc_fd);
    }
    //printf("Current command:%s\n", buf);
    return result;
}

int change_msdc_clock(unsigned int target_clk)
{
    //write mask:        echo 5 2 [host_id] [register_offset] [start_bit] [len] [value] > msdc_debug
    unsigned int div;
    unsigned int result_clk;
    unsigned int source_clk = 200000000;
    unsigned int mode;
    char clock_chg_buf[50];

    if (target_clk == 0) {
        // 0 means no need to change clock
        printf("Do not change source clock, Keep the Clock setting same as now. \n");
        return 0;
    }

    if (target_clk >= source_clk) {
        mode = 0x1; /* no divisor */
        div  = 0;
        result_clk = source_clk;
    } else {
        mode = 0x0; /* use divisor */
        if (target_clk >= (source_clk >> 1)) {
            div  = 0;         /* mean div = 1/2 */
            result_clk = source_clk >> 1; /* sclk = clk / 2 */
        } else {
            div  = (source_clk + ((target_clk << 2) - 1)) / (target_clk << 2);
            result_clk = (source_clk >> 2) / div;
        }
    }

    sprintf(clock_chg_buf,"5 2 %x %x %x %x %x\n", TEST_MSDC_PORT, MSDC_CFG, OFFSET_MSDC_CFG_CKDIV, (LEN_MSDC_CFG_CKDIV+LEN_MSDC_CFG_CKMOD), ((mode<<8) | div));
    write(msdc_debug_fd, clock_chg_buf, strlen(clock_chg_buf));
    usleep(100000);

    printf("The clock mode is 0x%x & div is 0x%x now. \n", mode, div);
    printf("The source_clk is %d MHz, So result_clk is %d MHz now. \n", source_clk, result_clk);

    return 0;
}

unsigned int str2int(char *str)
{
	unsigned int ret_int = 0 , tmp = 0;
	unsigned int idx = 0 , len = 0;

	len = strlen(str);
	//KAL_RAWPRINT(KERN_ERR "[%s] str len = %d \n", __FUNCTION__, len);
	for (idx = 0 ; idx < len ; idx ++) {
		tmp = str[idx] - 0x30;
		if (idx != 0) {
			ret_int = ret_int * 10;
		}
		ret_int += tmp;
	}
	//KAL_RAWPRINT(KERN_ERR "[%s] value = %d \n", __FUNCTION__, ret_int);

	return ret_int;
}

static void sd30_tuning_parameter_init(void)
{
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_DDLSEL, 0);
    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATRRDLY, 0x0);

    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_DSPLSEL, 0);
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_DSPL, 0);

    /* cmd response delay selection value */
    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRRDLY, 0);

    /* cmd line delay selection value */
    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY, 0);

    /* data sample selection */
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_WDSPLSEL, 0);
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_WDSPL, 0);

    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATWRDLY, 0x0);

    /* sample cmd line with clock's rising or falling edge */
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_RSPL, 0);

    /* cmd response turn around reriod, just for UHS104 mode */
    MSDC_SET_FIELD(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_CMD_RSP, 1);
    MSDC_SET_FIELD(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_WRDAT_CRCS, 1);

    /* read data latch clock selection */
    MSDC_SET_FIELD(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_INTCKSEL, 0);

    /* ckbuf in ckgen delay selection  for read tuning, 32 stages */
    MSDC_SET_FIELD(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_CKGENDLSEL, 0);

}

extern int g_ett_progress;
INT32 sd30_ett_tune(INT32 i4Argc, const CHAR **szArgv)
{
    /*[Arg] clock(MHz), data_mode(0: 32bit 1:64bit) ,cmd test times, read data test times, write data test times*/
    E_ETT_STATE state=TUNE_INIT;
    E_RESULT_TYPE res;
    E_MSDC_ETT_PARAMETER parm;
    u32 clock;
    u32 mode;
    char set_command_buf[50];
    
    if(i4Argc <7) {
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "[ERROR] input argument error!!\r\n");
        return CLI_UNKNOWN_CMD;
    }
    clock = str2int((char*)szArgv[2]);
    mode = str2int((char*)szArgv[3]);
    if(clock>208) {
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR,"[ERROR] input clock not in the range [0~208]\r\n");
        return CLI_UNKNOWN_CMD;
    }

    if(mode>1) {
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR,"[ERROR] input data_mode not in the range [0,1]\r\n");
        return CLI_UNKNOWN_CMD;
    }

    cmd_times = 1;
    rdata_times = 1;
    wdata_times = 1;

    cmd_times = str2int((char*)szArgv[4]);
    rdata_times = str2int((char*)szArgv[5]);
    wdata_times = str2int((char*)szArgv[6]);

    property_get("ro.mediatek.platform", g_platform, NULL);	
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG,"platform: %s\n",g_platform);
    
    while(state != TUNE_DONE && state!= TUNE_FAIL) {
        switch(state) {
            case TUNE_INIT:
                g_tune_result_str[32]='\0';
                sprintf(set_command_buf, "%s %d \n", ett_test_init, mode);
                write_cmd_no_output(set_command_buf);
                sd30_tuning_parameter_init();
                clock = clock * 1000 * 1000;
                g_ett_progress = 5;
                change_msdc_clock(clock);
                g_ett_progress = 8;
                usleep(1000000);

                state = TUNE_CMD;
                break;
            case TUNE_CMD:

                res = sd30_tune_cmd(cmd_times);

                g_ett_progress = 30;
                if(res == E_RESULT_PASS) {
                    state = TUNE_READ;
                }
                else {
                    state = TUNE_FAIL;
                }
                break;
            case TUNE_READ:
                res = sd30_tune_read(mode, rdata_times);
                g_ett_progress = 60;
                if(res == E_RESULT_PASS) {
                    state = TUNE_WRITE;
                }
                else if (res == E_RESULT_CMD_CRC) {
                #ifdef ETT_TUNE_DEBUG
                    state = TUNE_FAIL;
                #else
                    state = TUNE_CMD;
                #endif
                }
                else {
                    state = TUNE_FAIL;
                }
                break;
            case TUNE_WRITE:
                res = sd30_tune_write(wdata_times);
                g_ett_progress = 90;
                if(res == E_RESULT_PASS) {
                    state = TUNE_RESULT_SHOW;
                }
                else if (res == E_RESULT_CMD_CRC) {
                #ifdef ETT_TUNE_DEBUG
                    state = TUNE_FAIL;
                #else
                    state = TUNE_CMD;
                #endif
                }
                else {
                    state = TUNE_FAIL;
                }
                break;
            case TUNE_RESULT_SHOW:
                res = sd30_tune_result_show();
                g_ett_progress = 98;
                if(res == E_RESULT_PASS) {
                    state = TUNE_DONE;
                }
                else {
                    state = TUNE_FAIL;
                }
                break;
            default:
                break;
        }
    }

    sprintf(set_command_buf, "%s %d \n", ett_test_exit, 0);
    write_cmd_no_output(set_command_buf);

    if(state == TUNE_FAIL) {
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "[ERROR] Need to contact SA to\r\n"
               "[1] Check connection\r\n"
               "[2] Refine PASS WINDOW range\r\n"
               "[3] Change IO driving\r\n");
    }

    return CLI_UNKNOWN_CMD;
}

#define DEFAULT_CMD_SMPL (0)
static int check_score_withZeroNum(u32 result, u32 *pNumOfzero)
{
    u32 bit = 0;
    u32 num = 0;
    u32 old = 0;

    *pNumOfzero = 0;
    // maybe result is 0
    if (0 == result) {
        strcpy(g_tune_result_str,"OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        *pNumOfzero = 32;
        return 32;
    }

    if (0xFFFFFFFF == result) {
        strcpy(g_tune_result_str,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        return 0;
    }

    /* calc continue zero number */
    while (bit < 32) {
        if (result & (1 << bit)) { // failed
            g_tune_result_str[bit]='X';
            bit++;
            if (num > old)
                old = num;
            num = 0;
            continue;
        }
        g_tune_result_str[bit]='O';

        bit++;
        num++;
        *pNumOfzero=*pNumOfzero+1;
    }

    if (num > old)
        old = num;

    return old;
}

static int check_one_num(u32 result)
{
    u32 num = 0;
    u32 bit = 0;

    // maybe result is 0
    if (0 == result) {
        return 0;
    }

    if (0xFFFFFFFF == result) {
        return 32;
    }
    while (bit < 32) {
        if (result & (1 << bit)) {
            num++;
        }
        bit++;
    }
    return num;
}

static int check_score(u32 result)
{
    u32 bit = 0;
    u32 num = 0;
    u32 old = 0;

    // maybe result is 0
    if (0 == result) {
        strcpy(g_tune_result_str,"OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        return 32;
    }

    if (0xFFFFFFFF == result) {
        strcpy(g_tune_result_str,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        return 0;
    }

    /* calc continue zero number */
    while (bit < 32) {
        if (result & (1 << bit)) { // failed
            g_tune_result_str[bit]='X';
            bit++;
            if (old < num)
                old = num;
            num = 0;
            continue;
        }
        g_tune_result_str[bit]='O';
        bit++;
        num++;
    }

    if (num > old)
        old = num;

    return old;
}

static int ta_sel(
    u32 *raw_list, u32 start, u32 end, u8 *pmid)
{
    int n;
    u8 r_start, r_stop;
    unsigned int inaccuracy;
    unsigned int raw, TaOfmaxNum = 0;
    unsigned int NumOfZero = 0, maxNum = 0;

    *pmid = 0;
    r_start = end;
    r_stop = start;
    inaccuracy = TUNING_INACCURACY;

    for (n = start; n <= end; n++) {
        check_score_withZeroNum(raw_list[n], &NumOfZero);

        if (NumOfZero > maxNum) {
            maxNum = NumOfZero;
            TaOfmaxNum = n;
        }
    }

    raw = raw_list[TaOfmaxNum];
    ETT_DBGPRINT(ETT_DBG_RESULT, "The maximum offset is %d\r\n", TaOfmaxNum);


find:
    for (n = start; n <= end; n++) {
        if (check_one_num(raw_list[n]^raw) <= inaccuracy) {
            r_start = n;
            break;
        }
    }

    for (n = (int)end; n >= (int)start; n--) {
        if (check_one_num(raw_list[n]^raw) <= inaccuracy) {
            r_stop = n;
            break;
        }
    }

    /*
     * At least get the TA of which the margin has
     * either left 1T and right 1T
     */
    if ((r_start+2) <= r_stop)
        *pmid = (r_start+r_stop)/2;
    else {
        inaccuracy++;

        if (inaccuracy < 5) {
        	ETT_DBGPRINT(ETT_DBG_RESULT, "Enlarge the inaccuracy[%d]\r\n", inaccuracy);
            goto find;
        }
    }

    if (*pmid) {
    	ETT_DBGPRINT(ETT_DBG_RESULT, "Find suitable range[%d %d], TA_sel=%d\r\n",
            r_start, r_stop, *pmid);
    }
    else {
        *pmid = TaOfmaxNum;
        ETT_DBGPRINT(ETT_DBG_RESULT, "Un-expected pattern, pls check!, TA_sel=%d\r\n", *pmid);
    }

    return 0;
}
static E_RESULT_TYPE sd30_tune_cmd(u32 tst_times)
{
    int j,k,m,n,x;
    u32 raw,score,numOfZero;
    u32 max_score,max_numZero,max_raw;
    u8 sel=0;
    char set_command_buf[50];

    /*init data*/
    memset(g_cmd_score, 0, sizeof(u32)*SCALE_CKGEN_MSDC_DLY_SEL*SCALE_CMD_RSP_TA_CNTR*SCALE_CMD_IOCON_RSPL);
    memset(g_cmd_clkgen_data, 0, sizeof(S_ETT_TUNE_XDATA)*SCALE_CKGEN_MSDC_DLY_SEL);
    sprintf(set_command_buf, "%s %d \n", ett_test_cmd, tst_times);
    
    ETT_DBG_RAW_PRINT(ETT_DBG_RESULT, "Find cmd TA\r\n");
    ETT_DBGPRINT(ETT_DBG_RESULT, "%s \t %s \r\n",g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].name, g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].name);
    max_score = 0;
    max_raw = 0;

    for (n = g_cmd_ta_start; n < g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].scale; n++) {
        g_ta_raw[n]=0;
        MSDC_SET_FIELD(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_CMD_RSP, n);
        for (m = 0; m < g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].scale; m++) {
            MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY, m);
            if (write_cmd_no_output(set_command_buf) != 0) {
                // 0 means pass
                g_ta_raw[n] |= (1 << m);
            }
        }
        g_ta_score[n] = check_score(g_ta_raw[n]);
        ETT_DBGPRINT(ETT_DBG_RESULT, "%d \t %02d \t %s\r\n",n,g_ta_score[n],g_tune_result_str);
        if(g_ta_score[n] > max_score) {
            max_score = g_ta_score[n];
            max_raw= g_ta_raw[n];
            g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].sel = n;
        }
    }

    /* Select the suitable TA */
    ta_sel(g_ta_raw, g_cmd_ta_start, g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].scale-1, &sel);
    g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].sel = sel;

    ETT_DBGPRINT(ETT_DBG_RESULT, "suitable cmd TA=%d\r\n",g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].sel);
    MSDC_SET_FIELD(MSDC_PATCH_BIT1,  MSDC_PATCH_BIT1_CMD_RSP, g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].sel);


    ETT_DBG_RAW_PRINT(ETT_DBG_RESULT, "Find cmd internal delay\r\n");
    /*Use rising edge to sample*/
    MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_RSPL, DEFAULT_CMD_SMPL);
    ETT_DBGPRINT(ETT_DBG_RESULT, "%s \t %s \t %s \r\n", \
                 g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, g_ett_tune[MSDC_PAD_TUNE_CMDRRDLY].name, g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].name);
    for (k = 0; k < g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].scale; k++) {
        MSDC_SET_FIELD(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_CKGENDLSEL, k);
        max_score = 0;
        for (x=0;x<g_ett_tune[MSDC_PAD_TUNE_CMDRRDLY].scale; x++){
            raw=0;
            MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRRDLY, x);
            for (m = 0; m < g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].scale; m++) {
                MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY, m);
                if (write_cmd_no_output(set_command_buf) != 0) {
                    // 0 means pass
                    raw |= (1 << m);
                }
            }
            score = check_score_withZeroNum(raw, &numOfZero);
            ETT_DBGPRINT(ETT_DBG_RESULT, "%02d \t %02d \t %02d %s\r\n",k,x,score,g_tune_result_str);
            if(score > max_score) {
                max_score = score;
                max_numZero = numOfZero;
                g_cmd_clkgen_data[k].cmd_resp_gear = x;
                g_cmd_tune[k][DEFAULT_CMD_SMPL] = raw;
                g_cmd_score[k][DEFAULT_CMD_SMPL] = score;
            }
            else if(score == max_score) {
                if(numOfZero > max_numZero) {
                    max_numZero = numOfZero;
                    g_cmd_clkgen_data[k].cmd_resp_gear = x;
                    g_cmd_tune[k][DEFAULT_CMD_SMPL] = raw;
                    g_cmd_score[k][DEFAULT_CMD_SMPL] = score;
                }
            }
            /*TODO: need to re-scan all of it with the same max score to decide middle pos*/
        }
        ETT_DBGPRINT(ETT_DBG_RESULT, "Suitable internal delay: %02d in clkgen(%02d)\r\n", g_cmd_clkgen_data[k].cmd_resp_gear,k);
    }


    /* title */
    ETT_DBGPRINT(ETT_DBG_RESULT, "%s \t %s \t %s \r\n", \
                 g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, g_ett_tune[MSDC_IOCON_RSPL].name, g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].name);

    for (k = 0; k < g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].scale; k++) {
        MSDC_SET_FIELD(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_CKGENDLSEL, k);
        MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRRDLY, g_cmd_clkgen_data[k].cmd_resp_gear);
        for (j = 0; j < g_ett_tune[MSDC_IOCON_RSPL].scale; j++) {
            if(j != DEFAULT_CMD_SMPL) {
                MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_RSPL, j);
                g_cmd_tune[k][j] = 0;
                for (m = 0; m < g_ett_tune[MSDC_PAD_TUNE_CMDRDLY].scale; m++) {
                    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY, m);
                    if (write_cmd_no_output(set_command_buf) != 0) {
                        // 0 means pass
                        g_cmd_tune[k][j] |= (1 << m);
                    }
                }
            }
            g_cmd_score[k][j] = check_score(g_cmd_tune[k][j]);
            ETT_DBGPRINT(ETT_DBG_RESULT, "%02d \t %d \t %02d %s\r\n",k,j,g_cmd_score[k][j],g_tune_result_str);
            if(g_cmd_score[k][j] < PASS_WINDOW)
                g_cmd_score[k][j] = 0;

            if(g_cmd_clkgen_data[k].score < g_cmd_score[k][j]) {
               g_cmd_clkgen_data[k].spl_gear = j;
               g_cmd_clkgen_data[k].score = g_cmd_score[k][j];
               g_cmd_clkgen_data[k].raw = g_cmd_tune[k][j];

            }
        }

    }

    return E_RESULT_PASS;
}
static E_RESULT_TYPE check_array_result_simple(u32 result, u8 *sel)
{
    u8 start = 0;
    u8 end = 0;  // we need ten 0.
    u8 bit = 0;
    u8 max_start = 0;
    u8 max_end = 0;
    u8 max_score = 0;

    // maybe result is 0
    if (result == 0) {
        start = 0; end = 31;
        goto end;
    }

    if(result == 0xFFFFFFFF) {
        *sel = 0;
        return E_RESULT_PW_SMALL;
    }

find:
    start = end = 0;
    while (bit < 32) {
        if (result & (1 << bit)) { // failed
            bit++; continue;
        }
        start = end = bit;
        bit++;
        break;
    }

    while (bit < 32) {
        if (result & (1 << bit)){ // failed
            bit++;
            if((end -start + 1) > max_score) {
                max_score = end - start + 1;
                max_start = start;
                max_end = end;
            }
            goto find;

        } else {
            end = bit; bit++;
            //printf("end<%d>\n", end);
        }
    }
end:
    if((end -start + 1) > max_score) {
        max_score = end - start + 1;
        max_start = start;
        max_end = end;
    }
    *sel = (max_end + max_start)/2;
    if(max_score >= PASS_WINDOW) {
        return E_RESULT_PASS;
    }

    return E_RESULT_PW_SMALL;

}

static E_RESULT_TYPE cmd_param_get(u8 clk_gen_sel, u8 logLvl)
{
    E_RESULT_TYPE res;
    u8 sel=0;
    u8 i;
    u32 tmpScore[SCALE_CMD_RSP_TA_CNTR],tmpRaw[SCALE_CMD_RSP_TA_CNTR];

    g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].sel= clk_gen_sel;
    MSDC_SET_FIELD(MSDC_PATCH_BIT0,  MSDC_PATCH_BIT0_CKGENDLSEL, g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].sel);
    ETT_DBGPRINT(logLvl, "%s = %d cmd score = %d\r\n", \
               g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, clk_gen_sel, g_cmd_clkgen_data[clk_gen_sel].score);

    MSDC_SET_APPLY(logLvl, MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRRDLY, g_cmd_clkgen_data[clk_gen_sel].cmd_resp_gear);

    MSDC_SET_APPLY(logLvl, MSDC_IOCON, MSDC_IOCON_RSPL, g_cmd_clkgen_data[clk_gen_sel].spl_gear);

    res = check_array_result_simple(g_cmd_clkgen_data[clk_gen_sel].raw, &sel);
    MSDC_SET_APPLY(logLvl, MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY, sel);

    return res;
}

static void
check_rawd_style(
    P_AUTOK_RAWD_SCAN_T prAutok_raw_scan,
    unsigned char isRDat
    )
{
    unsigned int bit;
    unsigned char fInvalidCKGEN = 0;
    unsigned int filter = 2;
    AUTOK_RAWD_SCAN_STA_E RawScanSta = RD_SCAN_NONE;

    for (bit = 0; bit < 32; bit++) {
        if (prAutok_raw_scan->RawData & (1 << bit)) {
            switch (RawScanSta) {
                case RD_SCAN_NONE:
                    RawScanSta = RD_SCAN_PAD_BOUND_S;
                    prAutok_raw_scan->BoundReg1_S = 0;
                    prAutok_raw_scan->Reg1Cnt++;
                break;

                case RD_SCAN_PAD_MARGIN:
                    RawScanSta = RD_SCAN_PAD_BOUND_S;
                    prAutok_raw_scan->BoundReg1_S = bit;
                    prAutok_raw_scan->Reg1Cnt++;
                break;

                case RD_SCAN_PAD_BOUND_E:
                    if (filter) {
                        if (/*(prAutok_raw_scan->Reg1Cnt <= AUTOK_TUNING_INACCURACY) &&*/
                            ((bit - prAutok_raw_scan->BoundReg1_E) <= AUTOK_TUNING_INACCURACY)) {

                            //AUTOK_PRINT("[WARN] Try to filter the holes on raw data when CKGEN=%d\r\n",
                            //    prAutok_raw_scan->CurCKGEN);

                            RawScanSta = RD_SCAN_PAD_BOUND_S;

                            prAutok_raw_scan->Reg1Cnt += 2;
                            prAutok_raw_scan->BoundReg1_E = 0;
                            prAutok_raw_scan->BoundReg2_S = 0;

                            filter--;
                        }
                        else {
                            RawScanSta = RD_SCAN_PAD_BOUND_S_2;
                            prAutok_raw_scan->BoundReg2_S = bit;
                            prAutok_raw_scan->Reg2Cnt++;
                        }
                    }
                    else {
                        RawScanSta = RD_SCAN_PAD_BOUND_S_2;
                        prAutok_raw_scan->BoundReg2_S = bit;
                        prAutok_raw_scan->Reg2Cnt++;
                    }
                break;

                /* We do NOT hope to see the 3rd boundary region */
                case RD_SCAN_PAD_BOUND_E_2:
                    /*
                     * Before we confirm the thing, just filter the fail
                     * point that might be cause by accident
                     */
                    if (filter) {
                        filter--;
                        RawScanSta = RD_SCAN_PAD_BOUND_S_2;

                        /* In case of the hole locates near region2 */
                        if ((bit - prAutok_raw_scan->BoundReg2_E) <= 2) {
                            if ((bit - prAutok_raw_scan->BoundReg2_E) >= 1)
                                prAutok_raw_scan->Reg2Cnt += (bit - prAutok_raw_scan->BoundReg2_E);

                            prAutok_raw_scan->BoundReg2_E = 0;
                        }
                        /* In case of the hole locates near region1 */
                        else if ((prAutok_raw_scan->BoundReg2_S - prAutok_raw_scan->BoundReg1_E) <= 2){
                            /* Update count of region1 */
                            if ((prAutok_raw_scan->BoundReg2_S - prAutok_raw_scan->BoundReg1_E) >= 1)
                                prAutok_raw_scan->Reg1Cnt +=
                                    (prAutok_raw_scan->BoundReg2_E - prAutok_raw_scan->BoundReg1_E);

                            /* Update region1 */
                            prAutok_raw_scan->BoundReg1_E = prAutok_raw_scan->BoundReg2_E;

                            /* Update region2 */
                            prAutok_raw_scan->BoundReg2_S = bit;
                            prAutok_raw_scan->BoundReg2_E = 0;
                            prAutok_raw_scan->Reg2Cnt = 1;
                        }
                        else {
                            //AUTOK_PRINT("[ERR] Find holes on raw data when CKGEN=%d, but can NOT filter! \r\n",
                            //    prAutok_raw_scan->CurCKGEN);

                            fInvalidCKGEN = 1;
                            goto exit;
                        }

                        //AUTOK_PRINT("[WARN] Try to filter the holes on raw data when CKGEN=%d\r\n",
                        //    prAutok_raw_scan->CurCKGEN);
                    }
                    else {
                        //AUTOK_PRINT("[WARN] Find too much fail regions when CKGEN=%d(Invalid)\r\n",
                        //    prAutok_raw_scan->CurCKGEN);

                        fInvalidCKGEN = 1;
                        goto exit;
                    }
                break;

                case RD_SCAN_PAD_BOUND_S:
                    prAutok_raw_scan->Reg1Cnt++;
                break;

                case RD_SCAN_PAD_BOUND_S_2:
                    prAutok_raw_scan->Reg2Cnt++;
                break;

                default:
                break;
            }
        }
        else {
            switch (RawScanSta) {
                case RD_SCAN_NONE:
                    RawScanSta = RD_SCAN_PAD_MARGIN;
                break;

                case RD_SCAN_PAD_BOUND_S:
                    RawScanSta = RD_SCAN_PAD_BOUND_E;
                    prAutok_raw_scan->BoundReg1_E = bit - 1;
                break;

                case RD_SCAN_PAD_BOUND_S_2:
                    RawScanSta = RD_SCAN_PAD_BOUND_E_2;
                    prAutok_raw_scan->BoundReg2_E = bit - 1;
                break;

                case RD_SCAN_PAD_MARGIN:
                case RD_SCAN_PAD_BOUND_E:
                case RD_SCAN_PAD_BOUND_E_2:
                default:
                break;
            }
        }
    }

    /*
     * Another abnormal case, found 2 regions,
     * but they are too close, maybe it would NOT happen
     */
    if (isRDat) {
        /*
        if (prAutok_raw_scan->Reg1Cnt && prAutok_raw_scan->BoundReg2_E) {
            if ((prAutok_raw_scan->BoundReg2_S - prAutok_raw_scan->BoundReg1_E) > 1) {
                fInvalidCKGEN = 1;
                goto exit;
            }
            else if ((prAutok_raw_scan->BoundReg2_E - prAutok_raw_scan->BoundReg1_S) < 15) {
                prAutok_raw_scan->BoundReg2_S = 0;
                prAutok_raw_scan->BoundReg1_E = prAutok_raw_scan->BoundReg2_E;
                prAutok_raw_scan->BoundReg2_E = 0;
            }
        }
        */

        if (prAutok_raw_scan->Reg1Cnt == 32) {
            fInvalidCKGEN = 1;
            goto exit;
        }

        if ((32 - (prAutok_raw_scan->Reg1Cnt + prAutok_raw_scan->Reg2Cnt)) <=
            (AUTOK_TUNING_INACCURACY+1))
            fInvalidCKGEN = 1;
    }

exit:
    if (fInvalidCKGEN) {
        prAutok_raw_scan->fInvalidCKGEN = 1;
    }
}

static void dev_restore(void)
{
    /*write(onoff_fd, sdio_off_cmd, strlen(sdio_off_cmd));

    usleep(100*1000);
    write(onoff_fd, sdio_pow_off_cmd, strlen(sdio_pow_off_cmd));

    usleep(100*1000);
    write(onoff_fd, sdio_pow_on_cmd, strlen(sdio_pow_on_cmd));

    usleep(100*1000);
    write(onoff_fd, sdio_on_cmd, strlen(sdio_on_cmd));

    usleep(1000*1000);
*/
    change_msdc_clock(TARGET_CLOCK);
    usleep(1000000);

    MSDC_RESTORE_REG(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRRDLY);
    MSDC_RESTORE_REG(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_CMD_RSP);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_RSPL);
    MSDC_RESTORE_REG(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_CKGENDLSEL);
    MSDC_RESTORE_REG(MSDC_PAD_TUNE, MSDC_PAD_TUNE_CMDRDLY);
    MSDC_RESTORE_REG(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_INTCKSEL);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_DSPL);
    MSDC_RESTORE_REG(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATRRDLY);

    MSDC_RESTORE_REG(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_WRDAT_CRCS);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_WDSPL);
    MSDC_RESTORE_REG(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATWRDLY);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_DDLSEL);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_DSPLSEL);
    MSDC_RESTORE_REG(MSDC_IOCON, MSDC_IOCON_WDSPLSEL);

}

static E_RESULT_TYPE sd30_tune_read(u32 tst_mode, u32 tst_times)
{
    E_RESULT_TYPE res = E_RESULT_PASS;
    int i,j,q;
    int n;
    BOOL bStop= TRUE;
    char set_command_buf[50];

    memset(g_read_clkgen_data, 0, sizeof(S_ETT_TUNE_XDATA)*SCALE_CKGEN_MSDC_DLY_SEL);

    ETT_DBGPRINT(ETT_DBG_RESULT, "%s \t %s \t %s \t %s\r\n", g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, \
                 g_ett_tune[MSDC_PATCH_BIT0_INTCKSEL].name, g_ett_tune[MSDC_IOCON_DSPL].name, g_ett_tune[MSDC_PAD_TUNE_DATRRDLY].name);

    for(j = 0; j < g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].scale; j++) {
        if( (res = cmd_param_get(j, 4)) != E_RESULT_PASS)
            return res;

        if(j == 0) {
            sprintf(set_command_buf, "%s %d \n", ett_test_mode_change, tst_mode);
            if (write_cmd_no_output(set_command_buf) != 0) {
            	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "[ERROR] CMD CRC error in test mode change!!\r\n");
                return E_RESULT_ERR;
            }
            sprintf(set_command_buf, "%s %d \n", ett_test_read, tst_times);
        }

        for (q = 0; q < g_ett_tune[MSDC_PATCH_BIT0_INTCKSEL].scale; q++) {
            MSDC_SET_FIELD(MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_INTCKSEL, q);
            for (i = 0; i < g_ett_tune[MSDC_IOCON_DSPL].scale; i++) {
                MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_DSPL, i);
                g_read_data_tune[j][q][i] = 0;
                bStop = FALSE;
                for (n = 0; n < g_ett_tune[MSDC_PAD_TUNE_DATRRDLY].scale && bStop==FALSE; n++) {
                    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATRRDLY, n);

                    if ((res = (E_RESULT_TYPE)write_cmd_no_output(set_command_buf)) != E_RESULT_PASS) {
                        g_read_data_tune[j][q][i] |= (1 << n);
                        if (write_cmd_no_output(ett_test_recovery)) {
                            /* set all failed */
                            bStop = TRUE;
                            g_read_data_tune[j][q][i] = 0xFFFFFFFF;
                            ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "[ERROR]tune read : Fail to bring to tranfer status\n");
                            /* TO-DO re-init card for next para */
                            dev_restore();
                        }
                        if (res == E_RESULT_CMD_CRC) {
                            /*CMD_CRC error*/
                        	ETT_DBGPRINT(ETT_DBG_WARN, "[WARN] CMD CRC error in tuning read[%d %d %d %d], need to tune command again!!\r\n",j,q,i,n);
                            return res;
                        }
                    }
                }

                g_read_data_score[j][q][i] = check_score(g_read_data_tune[j][q][i]);

                ETT_DBGPRINT(ETT_DBG_RESULT, "%02d \t %d \t %d \t %02d %s\r\n",j,q,i,g_read_data_score[j][q][i],g_tune_result_str);

                if(g_read_data_score[j][q][i] < PASS_WINDOW)
                    g_read_data_score[j][q][i] = 0;


                if(g_read_data_score[j][q][i] > g_read_clkgen_data[j].score) {
                    g_read_clkgen_data[j].spl_gear = i;
                    g_read_clkgen_data[j].int_data_latch_gear = q;
                    g_read_clkgen_data[j].score = g_read_data_score[j][q][i];
                    g_read_clkgen_data[j].raw = g_read_data_tune[j][q][i];
                }
            }
        }
    }

    /* Double check for INT_DAT_LATCH_CK */
    if (g_read_clkgen_data[0].int_data_latch_gear == 0) {
        AUTOK_RAWD_SCAN_T RawScanCK0, RawScanCK1;

        /* Check CK0 raw first */
        memset(&RawScanCK0, 0, sizeof(RawScanCK0));
        RawScanCK0.RawData = g_read_data_tune[0][0][0];
        check_rawd_style(&RawScanCK0, 1);

        /* Only change LATCH_CK for this case */
        if (RawScanCK0.Reg1Cnt && !RawScanCK0.BoundReg1_S) {
            /* Then check CK1 raw data */
            memset(&RawScanCK1, 0, sizeof(RawScanCK1));
            RawScanCK1.RawData = g_read_data_tune[0][1][0];
            check_rawd_style(&RawScanCK1, 1);

            if (RawScanCK1.Reg1Cnt && RawScanCK1.BoundReg1_S) {
                /* Select 1 for INT_DAT_LATCH_CK */
                g_read_clkgen_data[0].fIntDatLatchCKChng = 1;
                g_read_clkgen_data[0].IntDatLatchCK = 1;
            }
        }
    }
    else if (g_read_clkgen_data[0].int_data_latch_gear == 1) {

        /* Maybe it is the misjudgement caused by inaccuracy */
        if (check_one_num(g_read_data_tune[0][0][0]^
                g_read_data_tune[0][1][0]) <= TUNING_INACCURACY) {

            g_read_clkgen_data[0].fIntDatLatchCKChng = 1;
            g_read_clkgen_data[0].IntDatLatchCK = 0;
        }
    }

    return E_RESULT_PASS;

}

static E_RESULT_TYPE read_param_get(u8 clk_gen_sel, u8 logLvl)
{
    E_RESULT_TYPE res;
    u8 sel=0;

    ETT_DBGPRINT(logLvl, "%s = %d read score = %d\r\n", \
               g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, clk_gen_sel, g_read_clkgen_data[clk_gen_sel].score);

    if (g_read_clkgen_data[0].fIntDatLatchCKChng) {
    	ETT_DBG_RAW_PRINT(logLvl, "INT_DAT_LATCH_CK is changed by double check!\r\n");
        MSDC_SET_APPLY(logLvl, MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_INTCKSEL,
            g_read_clkgen_data[0].IntDatLatchCK);
    }
    else {
        MSDC_SET_APPLY(logLvl, MSDC_PATCH_BIT0, MSDC_PATCH_BIT0_INTCKSEL,
            g_read_clkgen_data[0].int_data_latch_gear);
    }

    MSDC_SET_APPLY(logLvl, MSDC_IOCON, MSDC_IOCON_DSPL, g_read_clkgen_data[clk_gen_sel].spl_gear);

    res = check_array_result_simple(g_read_clkgen_data[clk_gen_sel].raw, &sel);
    MSDC_SET_APPLY(logLvl, MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATRRDLY, sel);
    return res;
}

static E_RESULT_TYPE sd30_tune_write(u32 tst_times)
{
    int i,j,m,n;
    u32 cmp_score = 0;
    u32 max_score = 0;
    u8 clkgen_sel = 0;
    u8 sel = 0;
    E_RESULT_TYPE res = E_RESULT_PASS;
    BOOL bStop= TRUE;
    char set_command_buf[50];

    sprintf(set_command_buf, "%s %d \n", ett_test_write, tst_times);
    memset(g_write_clkgen_data, 0, sizeof(S_ETT_TUNE_XDATA)*SCALE_CKGEN_MSDC_DLY_SEL);

    /* title */
    ETT_DBGPRINT(ETT_DBG_RESULT, "%s \t %s \t %s \t %s\r\n", g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, \
                 g_ett_tune[MSDC_PATCH_BIT1_WRDAT_CRCS].name, g_ett_tune[MSDC_IOCON_WDSPL].name, \
                 g_ett_tune[MSDC_PAD_TUNE_DATWRDLY].name);

    for (m = 0; m < g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].scale; m++) {
        /*Apply the new gear*/
        if( (res = cmd_param_get(m, ETT_DBG_INFO)) != E_RESULT_PASS)
            return res;
        if( (res = read_param_get(m, ETT_DBG_INFO)) != E_RESULT_PASS)
            return res;

        for (n = g_wrcrc_ta_start; n < g_ett_tune[MSDC_PATCH_BIT1_WRDAT_CRCS].scale; n++) {
            MSDC_SET_FIELD(MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_WRDAT_CRCS, n);
            for (i = 0; i < g_ett_tune[MSDC_IOCON_WDSPL].scale; i++) {
                MSDC_SET_FIELD(MSDC_IOCON, MSDC_IOCON_WDSPL, i);
                bStop = FALSE;
                g_write_data_tune[m][n][i] = 0;
                for (j = 0; j < g_ett_tune[MSDC_PAD_TUNE_DATWRDLY].scale && bStop==FALSE; j++) {
                    MSDC_SET_FIELD(MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATWRDLY, j);
                    if ((res = (E_RESULT_TYPE)write_cmd_no_output(set_command_buf)) != E_RESULT_PASS) {
                        g_write_data_tune[m][n][i] |= (1 << j);
                        if (write_cmd_no_output(ett_test_recovery)) {
                            /* set all failed */
                            bStop= TRUE;
                            g_write_data_tune[m][n][i] = 0xFFFFFFFF;
                            ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "[ERROR]tune write : Fail to bring to tranfer status===\n");
                            /* TODO re-init card for next para */
                            dev_restore();
                        }

                        if (res == E_RESULT_CMD_CRC) {
                            /*CMD_CRC error*/
                        	ETT_DBGPRINT(ETT_DBG_WARN, "[WARN] CMD CRC error in tuning write[%d %d,%d,%d], need to tune command again!!\r\n",m,n,i,j);
                            return res;
                        }
                    }

                }
                g_write_data_score[m][n][i] = check_score(g_write_data_tune[m][n][i]);

                ETT_DBGPRINT(ETT_DBG_RESULT, "%02d \t %d \t %d \t %02d %s\r\n",m,n,i,g_write_data_score[m][n][i],g_tune_result_str);

                if(g_write_data_score[m][n][i] < PASS_WINDOW)
                    g_write_data_score[m][n][i] = 0;


                if(g_write_data_score[m][n][i] > g_write_clkgen_data[m].score) {
                    g_write_clkgen_data[m].spl_gear = i;
                    g_write_clkgen_data[m].ta_gear = n;
                    g_write_clkgen_data[m].score = g_write_data_score[m][n][i];
                    g_write_clkgen_data[m].raw = g_write_data_tune[m][n][i];
                }
            }
        }

    }

    return E_RESULT_PASS;

}

static E_RESULT_TYPE sd30_tune_result_show(void)
{
    u8 clkgen_sel = 0;
    u32 cmp_score = 0;
    u32 max_score = 0;
    E_RESULT_TYPE res=E_RESULT_PASS;
    u8 sel=0;
    u8 i;
    u32 tmpScore[SCALE_WRDAT_CRCS_TA_CNTR],tmpRaw[SCALE_WRDAT_CRCS_TA_CNTR];

    /*Find the max total score as clkgen gear*/
    for(i=0;i<g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].scale;i++) {
        if(g_cmd_clkgen_data[i].score == 0 || g_read_clkgen_data[i].score == 0 || g_write_clkgen_data[i].score == 0)
            continue;

        cmp_score = g_cmd_clkgen_data[i].score + g_read_clkgen_data[i].score + g_write_clkgen_data[i].score;
        if(cmp_score > max_score) {
            clkgen_sel = i;
            max_score = cmp_score;
        }
        else if (cmp_score == max_score){
            /*The same score: priority: read, cmd, write*/
            if(g_read_clkgen_data[clkgen_sel].score < g_read_clkgen_data[i].score) {
                clkgen_sel = i;
            }
            else if(g_read_clkgen_data[clkgen_sel].score == g_read_clkgen_data[i].score) {
                if(g_cmd_clkgen_data[clkgen_sel].score < g_cmd_clkgen_data[i].score) {
                    clkgen_sel = i;
                }
            }
        }
    }

    ETT_DBG_RAW_PRINT(ETT_DBG_RESULT, "======ETT TUNING RESULT======\r\n");

    ETT_DBGPRINT(ETT_DBG_RESULT, "%s =%d\r\n",g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].name,g_ett_tune[MSDC_PATCH_BIT1_CMD_RSP].sel);

    /*Apply the new clk_gear*/
    if( (res = cmd_param_get(clkgen_sel, ETT_DBG_RESULT)) != E_RESULT_PASS)
        return res;
    if( (res = read_param_get(clkgen_sel, ETT_DBG_RESULT)) != E_RESULT_PASS)
        return res;

    ETT_DBGPRINT(ETT_DBG_RESULT, "%s = %d write score = %d\r\n", \
               g_ett_tune[MSDC_PATCH_BIT0_CKGENDLSEL].name, clkgen_sel, g_write_clkgen_data[clkgen_sel].score);

    /*Collect all information about WCRC TA*/
    for(i=g_wrcrc_ta_start;i<g_ett_tune[MSDC_PATCH_BIT1_WRDAT_CRCS].scale;i++) {
        tmpRaw[i] = g_write_data_tune[clkgen_sel][i][g_write_clkgen_data[clkgen_sel].spl_gear];
        tmpScore[i] = check_score(tmpRaw[i]);
        ETT_DBGPRINT(ETT_DBG_ERROR, "%d \t %02d \t %s\r\n", i, tmpScore[i], g_tune_result_str);
    }

    #if 0
    if(check_max_range(g_write_clkgen_data[clkgen_sel].score, g_write_clkgen_data[clkgen_sel].raw, \
                        tmpScore, tmpRaw, g_wrcrc_ta_start, g_ett_tune[MSDC_PATCH_BIT1_WRDAT_CRCS].scale, \
                        &sel) != 0) {
        /*Can not find range, use default TA*/
        sel = g_write_clkgen_data[clkgen_sel].ta_gear;
    }
    #endif

    ta_sel(tmpRaw, g_wrcrc_ta_start, g_ett_tune[MSDC_PATCH_BIT1_WRDAT_CRCS].scale-1, &sel);

    MSDC_SET_APPLY(ETT_DBG_RESULT, MSDC_PATCH_BIT1, MSDC_PATCH_BIT1_WRDAT_CRCS, sel);
    MSDC_SET_APPLY(ETT_DBG_RESULT, MSDC_IOCON, MSDC_IOCON_WDSPL, g_write_clkgen_data[clkgen_sel].spl_gear);

    /*PAD_TUNE_DATWRDLY*/
    res = check_array_result_simple(g_write_clkgen_data[clkgen_sel].raw, &sel);

    MSDC_SET_APPLY(ETT_DBG_RESULT, MSDC_PAD_TUNE, MSDC_PAD_TUNE_DATWRDLY, sel);


    return E_RESULT_PASS;
}

int algo_init(int argc, const char ** argv)
{
#if 1
	if ((ett_result_fd = fopen(/*"/sdcard/sdio_ETT_result.txt"*/g_ettdata.mLogPath, "w+")) == NULL)
	{
		__android_log_print(ETT_DBG_ERROR, LOG_TAG,"cannot open /data/sdio_ETT_result.txt") ;
		return -1;
	}
    /*if ((msdc_tune_fd = open("/proc/msdc_tune", O_RDWR)) < 0)
	{
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "cannot open /proc/msdc_tune");
		return -1;
	}*/
    if ((msdc_debug_fd = open("/proc/msdc_debug", O_RDWR)) < 0)
	{
    	ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "cannot open /proc/msdc_debug");
		return -1;
	}
#endif

#if 1
    if (g_ett_debug_level >= ETT_DBG_ERROR){
    	for(int i=0; i<1000; i++)
    		__android_log_print(ETT_DBG_ERROR, LOG_TAG, "cannot open %s", g_ettdata.mLogPath) ;
    	fprintf(ett_result_fd, "cannot open %s", g_ettdata.mLogPath);
    	fflush(ett_result_fd);
    }

    //ETT_DBG_RAW_PRINT(ETT_DBG_ERROR, "open /data/sdio_ETT_result.txt");
#if 1
    TEST_MSDC_PORT = str2int((char*)argv[1]);
    sd30_ett_tune(argc, argv);
#endif
    //printf("argc:%d, argv:[%s][%s][%s]", argc, argv[0], argv[1], argv[2]);
#endif
    fclose(ett_result_fd);
#if 1
    close(msdc_debug_fd);
    //close(msdc_tune_fd);
#endif

	return 0;
}
