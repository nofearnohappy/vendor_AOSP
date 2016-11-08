#ifndef _LEDS_H
#define _LEDS_H

enum led_color {
	LED_RED,
	LED_GREEN,
	LED_BLUE,
};

enum led_brightness {
	LED_OFF		= 0,
	LED_HALF	= 127,
	LED_FULL	= 255,
};

typedef enum{      
	ISINK_PWM_MODE = 0,      
	ISINK_BREATH_MODE = 1,      
	ISINK_REGISTER_MODE = 2
} MT65XX_PMIC_ISINK_MODE;

typedef enum{  
    ISINK_0 = 0,  //4mA
    ISINK_1 = 1,  //8mA
    ISINK_2 = 2,  //12mA
    ISINK_3 = 3,  //16mA
    ISINK_4 = 4,  //20mA
    ISINK_5 = 5   //24mA
} MT65XX_PMIC_ISINK_STEP;

typedef enum{  
	//32K clock
    ISINK_1KHZ = 0,  
    ISINK_200HZ = 4,  
    ISINK_5HZ = 199,  
    ISINK_2HZ = 499,  
    ISINK_1HZ = 999,  
    ISINK_05HZ = 1999,  
    ISINK_02HZ = 4999,
    ISINK_01HZ = 9999,
    //2M clock
    ISINK_2M_20KHZ = 2,
    ISINK_2M_1KHZ = 61,
    ISINK_2M_200HZ = 311,
    ISINK_2M_5HZ = 12499, 
    ISINK_2M_2HZ = 31249,
    ISINK_2M_1HZ = 62499
} MT65XX_PMIC_ISINK_FSEL;

enum mt65xx_led_type
{
    MT65XX_LED_TYPE_RED = 0,
    MT65XX_LED_TYPE_GREEN,
    MT65XX_LED_TYPE_BLUE,
	MT65XX_LED_TYPE_TOTAL,
};

enum mt65xx_led_mode                                                       
{                                                                          
    MT65XX_LED_MODE_NONE,                                                  
    MT65XX_LED_MODE_PMIC,     
};

enum mt65xx_led_pmic      
{   
    MT65XX_LED_PMIC_NLED_ISINK0,
    MT65XX_LED_PMIC_NLED_ISINK1,
    MT65XX_LED_PMIC_NLED_ISINK2,
    MT65XX_LED_PMIC_NLED_ISINK3,
};

struct cust_mt65xx_led {
    char                 *name;
    enum mt65xx_led_mode  mode;
    int                   data;
};

extern int mt65xx_leds_brightness_set(enum mt65xx_led_type type, enum led_brightness level);
extern void leds_battery_low_charging(void);

#endif // _LEDS_H
