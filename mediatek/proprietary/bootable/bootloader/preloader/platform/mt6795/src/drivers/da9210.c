#include "platform.h"
#include "i2c.h"
#include "gpio.h"
#include "da9210.h"
#include "cust_i2c.h"
#include "cust_gpio_usage.h"

/**********************************************************
  *   I2C Slave Setting
  *********************************************************/
#define da9210_SLAVE_ADDR_WRITE   0xD0
#define da9210_SLAVE_ADDR_Read    0xD1

/**********************************************************
  *   Global Variable 
  *********************************************************/
#ifdef I2C_EXT_BUCK_CHANNEL
#define da9210_I2C_ID I2C_EXT_BUCK_CHANNEL
#else
#define da9210_I2C_ID I2C1
#endif

#ifdef GPIO_EXT_BUCK_VSEL_PIN
unsigned int g_vproc_vsel_gpio_number = GPIO_EXT_BUCK_VSEL_PIN; 
#else
unsigned int g_vproc_vsel_gpio_number = 0;
#endif

static struct mt_i2c_t da9210_i2c;

int g_da9210_driver_ready=0;
int g_da9210_hw_exist=0;

#define da9210_print(fmt, args...)   \
do {									\
    printf(fmt, ##args); \
} while(0)

/**********************************************************
  *
  *   [I2C Function For Read/Write da9210] 
  *
  *********************************************************/
kal_uint32 da9210_write_byte(kal_uint8 addr, kal_uint8 value)
{
    int ret_code = I2C_OK;
    kal_uint8 write_data[2];
    kal_uint16 len;

    write_data[0]= addr;
    write_data[1] = value;

    da9210_i2c.id = da9210_I2C_ID;
    /* Since i2c will left shift 1 bit, we need to set da9210 I2C address to >>1 */
    da9210_i2c.addr = (da9210_SLAVE_ADDR_WRITE >> 1);
    da9210_i2c.mode = ST_MODE;
    da9210_i2c.speed = 100;
    len = 2;
    
    ret_code = i2c_write(&da9210_i2c, write_data, len);        
    //da9210_print("%s: i2c_write: ret_code: %d\n", __func__, ret_code);

    if(ret_code == 0)
        return 1; // ok
    else
        return 0; // fail
}

kal_uint32 da9210_read_byte (kal_uint8 addr, kal_uint8 *dataBuffer) 
{
    int ret_code = I2C_OK;
    kal_uint16 len;
    *dataBuffer = addr;

    da9210_i2c.id = da9210_I2C_ID;
    /* Since i2c will left shift 1 bit, we need to set da9210 I2C address to >>1 */
    da9210_i2c.addr = (da9210_SLAVE_ADDR_WRITE >> 1);
    da9210_i2c.mode = ST_MODE;
    da9210_i2c.speed = 100;
    len = 1;

    ret_code = i2c_write_read(&da9210_i2c, dataBuffer, len, len);    
    //da9210_print("%s: i2c_read: ret_code: %d\n", __func__, ret_code);

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
kal_uint32 da9210_read_interface (kal_uint8 RegNum, kal_uint8 *val, kal_uint8 MASK, kal_uint8 SHIFT)
{
    kal_uint8 da9210_reg = 0;
    kal_uint32 ret = 0;
    
    //da9210_print("--------------------------------------------------PL\n");

    ret = da9210_read_byte(RegNum, &da9210_reg);
    //da9210_print("[da9210_read_interface] Reg[%x]=0x%x\n", RegNum, da9210_reg);
    
    da9210_reg &= (MASK << SHIFT);
    *val = (da9210_reg >> SHIFT);    
    //da9210_print("[da9210_read_interface] val=0x%x\n", *val);

    return ret;
}

kal_uint32 da9210_config_interface (kal_uint8 RegNum, kal_uint8 val, kal_uint8 MASK, kal_uint8 SHIFT)
{
    kal_uint8 da9210_reg = 0;
    kal_uint32 ret = 0;

    //da9210_print("--------------------------------------------------PL\n");

    ret = da9210_read_byte(RegNum, &da9210_reg);
    //da9210_print("[da9210_config_interface] Reg[%x]=0x%x\n", RegNum, da9210_reg);
    
    da9210_reg &= ~(MASK << SHIFT);
    da9210_reg |= (val << SHIFT);

    ret = da9210_write_byte(RegNum, da9210_reg);
    //da9210_print("[da9210_config_interface] write Reg[%x]=0x%x\n", RegNum, da9210_reg);

    // Check
    //da9210_read_byte(RegNum, &da9210_reg);
    //da9210_print("[da9210_config_interface] Check Reg[%x]=0x%x\n", RegNum, da9210_reg);

    return ret;
}

kal_uint32 da9210_get_reg_value(kal_uint32 reg)
{
    kal_uint32 ret=0;
    kal_uint8 reg_val=0;

    ret=da9210_read_interface( (kal_uint8) reg, &reg_val, 0xFF, 0x0);

    if(ret==0) da9210_print("%d", ret);
    return reg_val;
}

void da9210_dump_register(void)
{
    kal_uint8 i=0;
    //----------------------------------------------------------------
    da9210_print("[da9210] page 0,1: ");   
    da9210_print("[0x%x]=0x%x ", 0x0, da9210_get_reg_value(0x0));
    for (i=0x50;i<=0x5D;i++) {     
        da9210_print("[0x%x]=0x%x ", i, da9210_get_reg_value(i));
    }    
    for (i=0xD0;i<=0xD9;i++) {
        da9210_print("[0x%x]=0x%x ", i, da9210_get_reg_value(i));
    }
    da9210_print("\n");
    //----------------------------------------------------------------
    da9210_print("[da9210] page 2,3: ");    
    for (i=0x05;i<=0x06;i++)
    {
        da9210_config_interface(0x0, 0x2, 0xF, 0); // select to page 2,3
        da9210_print("[0x%x]=0x%x ", i, da9210_get_reg_value(i));
    }
    for (i=0x43;i<=0x4F;i++)
    {
        da9210_config_interface(0x0, 0x2, 0xF, 0); // select to page 2,3
        da9210_print("[0x%x]=0x%x ", i, da9210_get_reg_value(i));
    }
    da9210_print("\n");
    //----------------------------------------------------------------
    da9210_config_interface(0x0, 0x0, 0xF, 0); // select to page 0,1
    //---------------------------------------------------------------- 
}

int get_da9210_i2c_ch_num(void)
{
    return da9210_I2C_ID;
}

int da9210_check_point=0;

void ext_buck_vproc_vsel(int val)
{   
    if(g_vproc_vsel_gpio_number != 0)
    {
        mt_set_gpio_mode(g_vproc_vsel_gpio_number,0); // 0:GPIO mode
        mt_set_gpio_dir(g_vproc_vsel_gpio_number,1);  // dir = output
        mt_set_gpio_out(g_vproc_vsel_gpio_number,val);

        da9210_check_point=1;
    }
    else
    {
        da9210_check_point=2;
    }

    //da9210_print("[ext_buck_vproc_vsel] done. (%d)\n", g_vproc_vsel_gpio_number);
}

void da9210_hw_init(void)
{
   kal_uint32 ret=0;

   //pre-init
   ret = da9210_config_interface(0x5D,0x1, 0x1, 0); // BUCK_EN=1
   ret = da9210_config_interface(0xD8,0x46,0xFF,0); // VSEL=high, 1.0V, Setting VBUCK_A=1.0V
   ret = da9210_config_interface(0xD9,0x46,0xFF,0); // VSEL=low,  1.0V, Setting VBUCK_B=1.0V

   //-----------------------------------------------
   
   da9210_print("[da9210_hw_init] [0x0]=0x%x, [0x58]=0x%x, [0x59]=0x%x, [0x5A]=0x%x, [0x5D]=0x%x, [0xD1]=0x%x, [0xD2]=0x%x, [0xD6]=0x%x, [0xD8]=0x%x, [0xD9]=0x%x\n", 
        da9210_get_reg_value(0x0), 
        da9210_get_reg_value(0x58), da9210_get_reg_value(0x59), 
        da9210_get_reg_value(0x5A), da9210_get_reg_value(0x5D), 
        da9210_get_reg_value(0xD1), da9210_get_reg_value(0xD2), 
        da9210_get_reg_value(0xD6), 
        da9210_get_reg_value(0xD8), da9210_get_reg_value(0xD9)
        );
   da9210_print("[da9210_hw_init] Done (%d)\n", da9210_check_point);  

   if(ret==0) da9210_print("%d", ret);
}

void da9210_hw_component_detect(void)
{
    kal_uint32 ret=0;
    kal_uint8 val=0;

    ret=da9210_config_interface(0x0, 0x1, 0x1, 7); // page reverts to 0 after one access
    ret=da9210_config_interface(0x0, 0x2, 0xF, 0); // select to page 2,3
    
    ret=da9210_read_interface(0x5,&val,0xF,4);
    
    // check default SPEC. value
    if(val==0xD)
    {
        g_da9210_hw_exist=1;        
    }
    else
    {
        g_da9210_hw_exist=0;
    }
    
    da9210_print("[da9210_hw_component_detect] exist=%d, Reg[0x105][7:4]=0x%x\n", g_da9210_hw_exist, val);

    if(ret==0) da9210_print("%d", ret);
}

int is_da9210_sw_ready(void)
{
    da9210_print("g_da9210_driver_ready=%d\n", g_da9210_driver_ready);
    
    return g_da9210_driver_ready;
}

int is_da9210_exist(void)
{
    da9210_print("g_da9210_hw_exist=%d\n", g_da9210_hw_exist);
    
    return g_da9210_hw_exist;
}

int da9210_vosel(unsigned long val)
{
    int ret=1;
    unsigned long reg_val=0;

    //reg_val = ( (val) - 30000 ) / 1000; //300mV~1570mV, step=10mV
    reg_val = ((((val*10)-300000)/1000)+9)/10;

    if(reg_val > 127)
        reg_val = 127;

    ret=da9210_write_byte(0xD8, reg_val);

    da9210_print("[da9210_vosel] val=%ld, reg_val=%ld, Reg[0xD8]=0x%x\n", 
        val, reg_val, da9210_get_reg_value(0xD8));

    return ret;
}

void da9210_driver_probe(void) 
{       
    da9210_hw_component_detect();        
    if(g_da9210_hw_exist==1)
    {
        da9210_hw_init();
        
        #if defined(TARGET_BUILD_VARIANT_ENG)
        da9210_dump_register();
        #endif
    }
    else
    {
        da9210_print("[da9210_driver_probe] PL da9210 is not exist\n");
    }    
    g_da9210_driver_ready=1;
    
    da9210_print("[da9210_driver_probe] PL g_da9210_hw_exist=%d, g_da9210_driver_ready=%d\n", 
        g_da9210_hw_exist, g_da9210_driver_ready);
   
    //--------------------------------------------------------

    #ifdef I2C_EXT_BUCK_CHANNEL
    da9210_print("[da9210_driver_probe] PL I2C_EXT_BUCK_CHANNEL=%d.\n", I2C_EXT_BUCK_CHANNEL);
    #else
    da9210_print("[da9210_driver_probe] PL No I2C_EXT_BUCK_CHANNEL (%d)\n", da9210_I2C_ID);
    #endif

    #ifdef GPIO_EXT_BUCK_VSEL_PIN
    da9210_print("[da9210_driver_probe] PL GPIO_EXT_BUCK_VSEL_PIN=0x%x.\n", GPIO_EXT_BUCK_VSEL_PIN);
    #else
    da9210_print("[da9210_driver_probe] PL No GPIO_EXT_BUCK_VSEL_PIN (0x%x)\n", g_vproc_vsel_gpio_number);
    #endif
}
