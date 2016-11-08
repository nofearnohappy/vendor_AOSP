#include "pmic.h"
#include "leds.h"


#define  MOD_TAG	"[LEDS]"
int isLedsDebug = 1;

#define LEDS_LOG(_format, ...) do{  \
    if (isLedsDebug != 0)\
	    print(MOD_TAG " " #_format "\n", ##__VA_ARGS__);\
    } while(0)

struct cust_mt65xx_led cust_led_list[MT65XX_LED_TYPE_TOTAL] = {
    {"red",    MT65XX_LED_MODE_PMIC, MT65XX_LED_PMIC_NLED_ISINK0 },
    {"green",  MT65XX_LED_MODE_NONE, 		-1},            
    {"blue",   MT65XX_LED_MODE_NONE, 		-1},                      
};

/*******************   break ******************************/

static int g_lastlevel[MT65XX_LED_TYPE_TOTAL] = {-1, -1, -1};

static int brightness_set_pmic(enum mt65xx_led_pmic pmic_type, enum led_brightness level)
{
	int tmp_level = level;	
		//static bool backlight_init_flag[4] = {false, false, false, false};

	if(pmic_type == MT65XX_LED_PMIC_NLED_ISINK0)
	{
		LEDS_LOG("[%s] set the ISINK0 regs \n",__func__);
		mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down  
		mt6331_upmu_set_rg_drv_isink0_ck_pdn(0);
    	mt6331_upmu_set_rg_drv_isink0_ck_cksel(0);
		mt6331_upmu_set_isink_ch0_mode(ISINK_PWM_MODE);
	    mt6331_upmu_set_isink_ch0_step(ISINK_3);//16mA
    	mt6331_upmu_set_isink_dim0_duty(15);
		mt6331_upmu_set_isink_dim0_fsel(ISINK_1KHZ);//1KHz
		if (level) 
		{
            mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down            
            mt6331_upmu_set_isink_ch0_en(0x1); // Turn on ISINK Channel 0
			
		}
		else 
		{
            mt6331_upmu_set_isink_ch0_en(0x0); // Turn off ISINK Channel 0
		}
		return 0;
	}
	#if 0
	else if(pmic_type == MT65XX_LED_PMIC_NLED_ISINK1)
	{
        mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down  
		mt6331_upmu_set_rg_drv_isink1_ck_pdn(0);
    	mt6331_upmu_set_rg_drv_isink1_ck_cksel(0);
		mt6331_upmu_set_isink_ch1_mode(ISINK_PWM_MODE);
	    mt6331_upmu_set_isink_ch1_step(ISINK_3);//16mA
    	mt6331_upmu_set_isink_dim1_duty(15);
		mt6331_upmu_set_isink_dim1_fsel(ISINK_1KHZ);//1KHz
		if (level) 
		{
            mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down            
            mt6331_upmu_set_isink_ch1_en(0x1); // Turn on ISINK Channel 0
			
		}
		else 
		{
            mt6331_upmu_set_isink_ch1_en(0x0); // Turn off ISINK Channel 0
		}
		return 0;
	}
	else if(pmic_type == MT65XX_LED_PMIC_NLED_ISINK2)
	{
        mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down  
		mt6331_upmu_set_rg_drv_isink2_ck_pdn(0);
    	mt6331_upmu_set_rg_drv_isink2_ck_cksel(0);
		mt6331_upmu_set_isink_ch2_mode(ISINK_PWM_MODE);
	    mt6331_upmu_set_isink_ch2_step(ISINK_3);//16mA
    	mt6331_upmu_set_isink_dim2_duty(15);
		mt6331_upmu_set_isink_dim2_fsel(ISINK_1KHZ);//1KHz
		if (level) 
		{
            mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down            
            mt6331_upmu_set_isink_ch2_en(0x1); // Turn on ISINK Channel 0
			
		}
		else 
		{
            mt6331_upmu_set_isink_ch2_en(0x0); // Turn off ISINK Channel 0
		}
		return 0;
	}
    else if(pmic_type == MT65XX_LED_PMIC_NLED_ISINK3)
	{
        mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down  
		mt6331_upmu_set_rg_drv_isink3_ck_pdn(0);
    	mt6331_upmu_set_rg_drv_isink3_ck_cksel(0);
		mt6331_upmu_set_isink_ch3_mode(ISINK_PWM_MODE);
	    mt6331_upmu_set_isink_ch3_step(ISINK_3);//16mA
    	mt6331_upmu_set_isink_dim3_duty(15);
		mt6331_upmu_set_isink_dim3_fsel(ISINK_1KHZ);//1KHz
		if (level) 
		{
            mt6331_upmu_set_rg_drv_32k_ck_pdn(0x0); // Disable power down            
            mt6331_upmu_set_isink_ch3_en(0x1); // Turn on ISINK Channel 0
			
		}
		else 
		{
            mt6331_upmu_set_isink_ch3_en(0x0); // Turn off ISINK Channel 0
		}
		return 0;
	}
	#endif
	return -1;
}

static int mt65xx_led_set_cust(struct cust_mt65xx_led *cust, int level)
{
	if (level > LED_FULL)
		level = LED_FULL;
	else if (level < 0)
		level = 0;
  
	switch (cust->mode) {
		case MT65XX_LED_MODE_PMIC:
			return brightness_set_pmic(cust->data, level);
		case MT65XX_LED_MODE_NONE:
		default:
			break;
	}
	return -1;
}

/****************************************************************************
 * external functions
 ***************************************************************************/
int mt65xx_leds_brightness_set(enum mt65xx_led_type type, enum led_brightness level)
{
    //struct cust_mt65xx_led *cust_led_list = cust_led_list;
	LEDS_LOG("[%s] the led_type is: %d , level is %d \n",__func__,type,level);

    if (type >= MT65XX_LED_TYPE_TOTAL)
        return -1;

    if (level > LED_FULL)
        level = LED_FULL;

    if (g_lastlevel[type] != (int)level) {
        g_lastlevel[type] = level;
        //dprintf(CRITICAL,"[LEDS]LK: %s level is %d \n\r", cust_led_list[type].name, level);
        return mt65xx_led_set_cust(&cust_led_list[type], level);
    }
    else {
        return -1;
    }

}


// API for preloader
void leds_battery_low_charging(void)
{
	mt65xx_leds_brightness_set(MT65XX_LED_TYPE_RED, LED_FULL);
	mt65xx_leds_brightness_set(MT65XX_LED_TYPE_GREEN, LED_OFF);
	mt65xx_leds_brightness_set(MT65XX_LED_TYPE_BLUE, LED_OFF);
}
