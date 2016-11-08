#include "platform.h"
#include "i2c.h"
#include "gpio.h"
#include "bq24160.h"
#include "cust_i2c.h"
#include "cust_gpio_usage.h"

/**********************************************************
  *   I2C Slave Setting
  *********************************************************/
#define bq24160_SLAVE_ADDR_WRITE   0xD6
#define bq24160_SLAVE_ADDR_Read    0xD7

/**********************************************************
  *   Global Variable 
  *********************************************************/
#ifdef I2C_SWITHING_CHARGER_CHANNEL
#define bq24160_I2C_ID I2C_SWITHING_CHARGER_CHANNEL
#else
#define bq24160_I2C_ID 0//2
#endif

static struct mt_i2c_t bq24160_i2c;

int g_bq24160_driver_ready=0;
int g_bq24160_hw_exist=0;

#define bq24160_print(fmt, args...)   \
do {									\
    printf(fmt, ##args); \
} while(0)

kal_uint8 bq24160_reg[bq24160_REG_NUM] = {0};

/**********************************************************
  *
  *   [I2C Function For Read/Write bq24160] 
  *
  *********************************************************/
kal_uint32 bq24160_write_byte(kal_uint8 addr, kal_uint8 value)
{
    int ret_code = I2C_OK;
    kal_uint8 write_data[2];
    kal_uint16 len;

    write_data[0]= addr;
    write_data[1] = value;

    bq24160_i2c.id = bq24160_I2C_ID;
    /* Since i2c will left shift 1 bit, we need to set bq24160 I2C address to >>1 */
    bq24160_i2c.addr = (bq24160_SLAVE_ADDR_WRITE >> 1);
    bq24160_i2c.mode = ST_MODE;
    bq24160_i2c.speed = 100;
    len = 2;
    
    ret_code = i2c_write(&bq24160_i2c, write_data, len);        
    //bq24160_print("%s: i2c_write: ret_code: %d\n", __func__, ret_code);

    if(ret_code == 0)
        return 1; // ok
    else
        return 0; // fail
}

kal_uint32 bq24160_read_byte (kal_uint8 addr, kal_uint8 *dataBuffer) 
{
    int ret_code = I2C_OK;
    kal_uint16 len;
    *dataBuffer = addr;

    bq24160_i2c.id = bq24160_I2C_ID;
    /* Since i2c will left shift 1 bit, we need to set bq24160 I2C address to >>1 */
    bq24160_i2c.addr = (bq24160_SLAVE_ADDR_WRITE >> 1);
    bq24160_i2c.mode = ST_MODE;
    bq24160_i2c.speed = 100;
    len = 1;

    ret_code = i2c_write_read(&bq24160_i2c, dataBuffer, len, len);    
    //bq24160_print("%s: i2c_read: ret_code: %d\n", __func__, ret_code);

    if(ret_code == 0)
        return 1; // ok
    else
        return 0; // fail
}

/**********************************************************
  *
  *   [Read / Write Function] 
  *
  *********************************************************/
kal_uint32 bq24160_read_interface (kal_uint8 RegNum, kal_uint8 *val, kal_uint8 MASK, kal_uint8 SHIFT)
{
    kal_uint8 bq24160_reg = 0;
    kal_uint32 ret = 0;
    
    //bq24160_print("--------------------------------------------------PL\n");

    ret = bq24160_read_byte(RegNum, &bq24160_reg);
    //bq24160_print("[bq24160_read_interface] Reg[%x]=0x%x\n", RegNum, bq24160_reg);
    
    bq24160_reg &= (MASK << SHIFT);
    *val = (bq24160_reg >> SHIFT);    
    //bq24160_print("[bq24160_read_interface] val=0x%x\n", *val);

    return ret;
}

kal_uint32 bq24160_config_interface (kal_uint8 RegNum, kal_uint8 val, kal_uint8 MASK, kal_uint8 SHIFT)
{
    kal_uint8 bq24160_reg = 0;
    kal_uint32 ret = 0;

    //bq24160_print("--------------------------------------------------PL\n");

    ret = bq24160_read_byte(RegNum, &bq24160_reg);
    //bq24160_print("[bq24160_config_interface] Reg[%x]=0x%x\n", RegNum, bq24160_reg);
    
    bq24160_reg &= ~(MASK << SHIFT);
    bq24160_reg |= (val << SHIFT);

    if(RegNum == bq24160_CON2 && val == 1 && MASK ==CON2_RESET_MASK && SHIFT == CON2_RESET_SHIFT)
    {
        // RESET bit
    }
    else if(RegNum == bq24160_CON2)
    {
        bq24160_reg &= ~0x80;	//RESET bit read returs 1, so clear it
    }

    ret = bq24160_write_byte(RegNum, bq24160_reg);
    //bq24160_print("[bq24160_config_interface] write Reg[%x]=0x%x\n", RegNum, bq24160_reg);

    // Check
    //bq24160_read_byte(RegNum, &bq24160_reg);
    //bq24160_print("[bq24160_config_interface] Check Reg[%x]=0x%x\n", RegNum, bq24160_reg);

    return ret;
}

void bq24160_dump_register(void)
{
    int i=0;

    for (i=0;i<bq24160_REG_NUM;i++)
    {
        bq24160_read_byte(i, &bq24160_reg[i]);
    }

    bq24160_print("[0x0]=0x%x,[0x1]=0x%x,[0x2]=0x%x,[0x3]=0x%x,[0x4]=0x%x,[0x5]=0x%x,[0x6]=0x%x,[0x7]=0x%x\n", 
        bq24160_reg[0], bq24160_reg[1],bq24160_reg[2],bq24160_reg[3],bq24160_reg[4],bq24160_reg[5],bq24160_reg[6],bq24160_reg[7]);
}

kal_uint8 is_in_minsys_mode(void)
{
    kal_uint8 val=0;
    bq24160_read_interface(bq24160_CON6, &val, 0x1, 7);
    return val;
}

void bq24160_turn_on_charging(void)
{
    bq24160_config_interface(bq24160_CON0, 0x1, 0x1, 7); // wdt reset
    bq24160_config_interface(bq24160_CON7, 0x1, 0x3, 5); // Safty timer
    
    bq24160_config_interface(bq24160_CON2, 0x2, 0x7, 4); // USB current limit at 500mA    
        
    bq24160_config_interface(bq24160_CON3, 0x1, 0x1, 1); // IN current limit
    bq24160_config_interface(bq24160_CON5, 0x13,0x1F,3); // ICHG to BAT
    bq24160_config_interface(bq24160_CON5, 0x3, 0x7, 0); // ITERM to BAT           
    bq24160_config_interface(bq24160_CON6, 0x3, 0x7, 3); // VINDPM_USB
    bq24160_config_interface(bq24160_CON6, 0x3, 0x7, 0); // VINDPM_IN
    bq24160_config_interface(bq24160_CON7, 0x0, 0x1, 3); // Thermal sense
    
    bq24160_config_interface(bq24160_CON3, 0x23,0x3F,2); // CV=4.2V    
    bq24160_config_interface(bq24160_CON2, 0x0, 0x1, 1);
}

void bq24160_hw_init(void) 
{       
    bq24160_turn_on_charging();
    bq24160_dump_register();
    
    //--------------------------------------------------------

    #ifdef I2C_SWITHING_CHARGER_CHANNEL
    bq24160_print("[bq24160_driver_probe] PL I2C_SWITHING_CHARGER_CHANNEL=0x%x.\n", I2C_SWITHING_CHARGER_CHANNEL);
    #else
    bq24160_print("[bq24160_driver_probe] PL No I2C_SWITHING_CHARGER_CHANNEL\n");
    #endif
}
