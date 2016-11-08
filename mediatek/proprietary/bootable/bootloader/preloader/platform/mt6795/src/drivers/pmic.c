#include <typedefs.h>
#include <platform.h>
#include <pmic_wrap_init.h>
#include <pmic.h>
#ifdef MTK_BQ24160_SUPPORT
#include <bq24160.h>
#endif
#include <wdt.h>
#include <leds.h>

#define SYSTEM_ON_VOLTAGE 3400

extern int PMIC_IMM_GetVbat(int deCount);
//==============================================================================
// PMIC access API
//==============================================================================
U32 pmic_read_interface (U32 RegNum, U32 *val, U32 MASK, U32 SHIFT)
{
    U32 return_value = 0;    
    U32 pmic_reg = 0;
    U32 rdata;    

    //mt_read_byte(RegNum, &pmic_reg);
    return_value= pwrap_wacs2(0, (RegNum), 0, &rdata);
    pmic_reg=rdata;
    if(return_value!=0)
    {   
        print("[pmic_read_interface] Reg[%x]= pmic_wrap read data fail\n", RegNum);
        return return_value;
    }
    //print("[pmic_read_interface] Reg[%x]=0x%x\n", RegNum, pmic_reg);
    
    pmic_reg &= (MASK << SHIFT);
    *val = (pmic_reg >> SHIFT);    
    //print("[pmic_read_interface] val=0x%x\n", *val);

    return return_value;
}

U32 pmic_config_interface (U32 RegNum, U32 val, U32 MASK, U32 SHIFT)
{
    U32 return_value = 0;    
    U32 pmic_reg = 0;
    U32 rdata;

    //1. mt_read_byte(RegNum, &pmic_reg);
    return_value= pwrap_wacs2(0, (RegNum), 0, &rdata);
    pmic_reg=rdata;    
    if(return_value!=0)
    {   
        print("[pmic_config_interface] Reg[%x]= pmic_wrap read data fail\n", RegNum);
        return return_value;
    }
    //print("[pmic_config_interface] Reg[%x]=0x%x\n", RegNum, pmic_reg);
    
    pmic_reg &= ~(MASK << SHIFT);
    pmic_reg |= (val << SHIFT);

    //2. mt_write_byte(RegNum, pmic_reg);
    return_value= pwrap_wacs2(1, (RegNum), pmic_reg, &rdata);
    if(return_value!=0)
    {   
        print("[pmic_config_interface] Reg[%x]= pmic_wrap read data fail\n", RegNum);
        return return_value;
    }
    //print("[pmic_config_interface] write Reg[%x]=0x%x\n", RegNum, pmic_reg);    

#if 0
    //3. Double Check    
    //mt_read_byte(RegNum, &pmic_reg);
    return_value= pwrap_wacs2(0, (RegNum), 0, &rdata);
    pmic_reg=rdata;    
    if(return_value!=0)
    {   
        print("[pmic_config_interface] Reg[%x]= pmic_wrap write data fail\n", RegNum);
        return return_value;
    }
    print("[pmic_config_interface] Reg[%x]=0x%x\n", RegNum, pmic_reg);
#endif    

    return return_value;
}

//==============================================================================
// PMIC-Charger Type Detection
//==============================================================================
CHARGER_TYPE g_ret = CHARGER_UNKNOWN;
int g_charger_in_flag = 0;
int g_first_check=0;

extern void Charger_Detect_Init(void);
extern void Charger_Detect_Release(void);

void pmic_lock(void){    
}

void pmic_unlock(void){    
}

void mt6332_upmu_set_rg_bc12_vsrc_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_VSRC_EN_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_VSRC_EN_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_vref_vth(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_VREF_VTH_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_VREF_VTH_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_ipu_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_IPU_EN_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_IPU_EN_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_ipd_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_IPD_EN_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_IPD_EN_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_cmp_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_CMP_EN_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_CMP_EN_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_bias_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_BIAS_EN_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_BIAS_EN_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_bb_ctrl(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CORE_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_BB_CTRL_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_BB_CTRL_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_rst(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CHR_CON16),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_RST_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_RST_SHIFT)
	                         );
  pmic_unlock();
}

void mt6332_upmu_set_rg_bc12_chrdet_rst(kal_uint32 val)
{
  kal_uint32 ret=0;

  pmic_lock();
  ret=pmic_config_interface( (kal_uint32)(MT6332_CHR_CON16),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_CHRDET_RST_MASK),
                             (kal_uint32)(MT6332_PMIC_RG_BC12_CHRDET_RST_SHIFT)
	                         );
  pmic_unlock();
}

kal_uint32 mt6332_upmu_get_rgs_bc12_cmp_out(void)
{
  kal_uint32 ret=0;
  kal_uint32 val=0;

  pmic_lock();
  ret=pmic_read_interface( (kal_uint32)(MT6332_STA_CON0),
                           (&val),
                           (kal_uint32)(MT6332_PMIC_RGS_BC12_CMP_OUT_MASK),
                           (kal_uint32)(MT6332_PMIC_RGS_BC12_CMP_OUT_SHIFT)
	                       );
  pmic_unlock();

  return val;
}

kal_uint32 mt6332_upmu_get_swcid(void)
{
  kal_uint32 ret=0;
  kal_uint32 val=0;

  pmic_lock();
  ret=pmic_read_interface( (kal_uint32)(MT6332_SWCID),
                           (&val),
                           (kal_uint32)(MT6332_PMIC_SWCID_MASK),
                           (kal_uint32)(MT6332_PMIC_SWCID_SHIFT)
	                       );
  pmic_unlock();

  return val;
}

unsigned int get_pmic_mt6332_cid(void)
{
    return mt6332_upmu_get_swcid();
}

#if defined(CONFIG_POWER_EXT) || defined(CONFIG_MTK_FPGA)

int hw_charging_get_charger_type(void)
{
    return STANDARD_HOST;
}

#else
static void hw_bc12_init(void)
{
    Charger_Detect_Init();
        
    //RG_bc12_BIAS_EN=1    
    mt6332_upmu_set_rg_bc12_bias_en(0x1);
    //RG_bc12_VSRC_EN[1:0]=00
    mt6332_upmu_set_rg_bc12_vsrc_en(0x0);
    //RG_bc12_VREF_VTH = [1:0]=00
    mt6332_upmu_set_rg_bc12_vref_vth(0x0);
    //RG_bc12_CMP_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_cmp_en(0x0);
    //RG_bc12_IPU_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_ipu_en(0x0);
    //RG_bc12_IPD_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_ipd_en(0x0);
    //bc12_RST=1
    mt6332_upmu_set_rg_bc12_rst(0x1);
    //bc12_BB_CTRL=1
    mt6332_upmu_set_rg_bc12_bb_ctrl(0x1);

    //msleep(10);
    mdelay(50);

}
 
 
static U32 hw_bc12_DCD(void)
{
    U32 wChargerAvail = 0;

    //RG_bc12_IPU_EN[1.0] = 10
    if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_ipu_en(0x1);
    else                                            mt6332_upmu_set_rg_bc12_ipu_en(0x2);
    
    //RG_bc12_IPD_EN[1.0] = 01
    if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_ipd_en(0x2);
    else                                            mt6332_upmu_set_rg_bc12_ipd_en(0x1); 
    
    //RG_bc12_VREF_VTH = [1:0]=01
    mt6332_upmu_set_rg_bc12_vref_vth(0x1);
    
    //RG_bc12_CMP_EN[1.0] = 10
    if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_cmp_en(0x1);
    else                                            mt6332_upmu_set_rg_bc12_cmp_en(0x2);

    //msleep(20);
    mdelay(80);

    wChargerAvail = mt6332_upmu_get_rgs_bc12_cmp_out();
    
    //RG_bc12_IPU_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_ipu_en(0x0);
    //RG_bc12_IPD_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_ipd_en(0x0);
    //RG_bc12_CMP_EN[1.0] = 00
    mt6332_upmu_set_rg_bc12_cmp_en(0x0);
    //RG_bc12_VREF_VTH = [1:0]=00
    mt6332_upmu_set_rg_bc12_vref_vth(0x0);

    return wChargerAvail;
}
 
 
static U32 hw_bc12_stepA1(void)
{
   U32 wChargerAvail = 0;
     
   //RG_bc12_IPD_EN[1.0] = 01
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_ipd_en(0x2);
   else                                            mt6332_upmu_set_rg_bc12_ipd_en(0x1);
   
   //RG_bc12_VREF_VTH = [1:0]=00
   mt6332_upmu_set_rg_bc12_vref_vth(0x0);
   
   //RG_bc12_CMP_EN[1.0] = 01
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_cmp_en(0x2);
   else                                            mt6332_upmu_set_rg_bc12_cmp_en(0x1);

   //msleep(80);
   mdelay(80);

   wChargerAvail = mt6332_upmu_get_rgs_bc12_cmp_out();

   //RG_bc12_IPD_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_ipd_en(0x0);
   //RG_bc12_CMP_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_cmp_en(0x0);

   return  wChargerAvail;
}
 
 
static U32 hw_bc12_stepA2(void)
{
   U32 wChargerAvail = 0;
     
   //RG_bc12_VSRC_EN[1.0] = 10 
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_vsrc_en(0x1);
   else                                            mt6332_upmu_set_rg_bc12_vsrc_en(0x2);
   
   //RG_bc12_IPD_EN[1:0] = 01
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_ipd_en(0x2);
   else                                            mt6332_upmu_set_rg_bc12_ipd_en(0x1);
   
   //RG_bc12_VREF_VTH = [1:0]=00
   mt6332_upmu_set_rg_bc12_vref_vth(0x0);
   
   //RG_bc12_CMP_EN[1.0] = 01
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_cmp_en(0x2);
   else                                            mt6332_upmu_set_rg_bc12_cmp_en(0x1);

   //msleep(80);
   mdelay(80);

   wChargerAvail = mt6332_upmu_get_rgs_bc12_cmp_out();

   //RG_bc12_VSRC_EN[1:0]=00
   mt6332_upmu_set_rg_bc12_vsrc_en(0x0);
   //RG_bc12_IPD_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_ipd_en(0x0);
   //RG_bc12_CMP_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_cmp_en(0x0);

   return  wChargerAvail;
}
 
 
static U32 hw_bc12_stepB2(void)
{
   U32 wChargerAvail = 0;

   //RG_bc12_IPU_EN[1:0]=10
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_ipu_en(0x1);
   else                                            mt6332_upmu_set_rg_bc12_ipu_en(0x2);
   
   //RG_bc12_VREF_VTH = [1:0]=01
   mt6332_upmu_set_rg_bc12_vref_vth(0x1);
   
   //RG_bc12_CMP_EN[1.0] = 01
   if(get_pmic_mt6332_cid()==PMIC6332_E1_CID_CODE) mt6332_upmu_set_rg_bc12_cmp_en(0x2);
   else                                            mt6332_upmu_set_rg_bc12_cmp_en(0x1);

   //msleep(80);
   mdelay(80);

   wChargerAvail = mt6332_upmu_get_rgs_bc12_cmp_out();

   //RG_bc12_IPU_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_ipu_en(0x0);
   //RG_bc12_CMP_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_cmp_en(0x0);
   //RG_bc12_VREF_VTH = [1:0]=00
   mt6332_upmu_set_rg_bc12_vref_vth(0x0);

   return  wChargerAvail;
}
 
 
static void hw_bc12_done(void)
{
   //RG_bc12_VSRC_EN[1:0]=00
   mt6332_upmu_set_rg_bc12_vsrc_en(0x0);
   //RG_bc12_VREF_VTH = [1:0]=0
   mt6332_upmu_set_rg_bc12_vref_vth(0x0);
   //RG_bc12_CMP_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_cmp_en(0x0);
   //RG_bc12_IPU_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_ipu_en(0x0);
   //RG_bc12_IPD_EN[1.0] = 00
   mt6332_upmu_set_rg_bc12_ipd_en(0x0);
   //RG_bc12_BIAS_EN=0
   mt6332_upmu_set_rg_bc12_bias_en(0x0); 

   Charger_Detect_Release();
     
}

CHARGER_TYPE hw_charger_type_detection(void)
{
    CHARGER_TYPE charger_tye = CHARGER_UNKNOWN;
    
    /********* Step initial  ***************/         
    hw_bc12_init();
 
    /********* Step DCD ***************/  
    if(1 == hw_bc12_DCD())
    {
         /********* Step A1 ***************/
         if(1 == hw_bc12_stepA1())
         {             
             charger_tye = APPLE_2_1A_CHARGER;
             printf("step A1 : Apple 2.1A CHARGER!\r\n");
         }
         else
         {
             charger_tye = NONSTANDARD_CHARGER;
              printf("step A1 : Non STANDARD CHARGER!\r\n");
         }
    }
    else
    {
         /********* Step A2 ***************/
         if(1 == hw_bc12_stepA2())
         {
             /********* Step B2 ***************/
             if(1 == hw_bc12_stepB2())
             {
                 charger_tye = STANDARD_CHARGER;
                  printf("step B2 : STANDARD CHARGER!\r\n");
             }
             else
             {
                 charger_tye = CHARGING_HOST;
                  printf("step B2 :  Charging Host!\r\n");
             }
         }
         else
         {
              charger_tye = STANDARD_HOST;
              printf("step A2 : Standard USB Host!\r\n");
         }
 
    }
 
    /********* Finally setting *******************************/
    hw_bc12_done();
    
    return charger_tye;
}
#endif

CHARGER_TYPE mt_charger_type_detection(void)
{
    if( g_first_check == 0 )
    {
        g_first_check = 1;
        g_ret = hw_charger_type_detection();
    }
    else
    {
        printf("[mt_charger_type_detection] Got data !!, %d, %d\r\n", g_charger_in_flag, g_first_check);
    }

    return g_ret;
}

//==============================================================================
// PMIC Usage APIs
//==============================================================================
U32 get_mt6331_pmic_chip_version (void)
{
    U32 ret=0;
    U32 val=0;

    ret=pmic_read_interface( (kal_uint32)(MT6331_SWCID),
                           (&val),
                           (kal_uint32)(MT6331_PMIC_SWCID_MASK),
                           (kal_uint32)(MT6331_PMIC_SWCID_SHIFT)
	                       );                           

    return val;
}

U32 get_mt6332_pmic_chip_version (void)
{
    U32 ret=0;
    U32 val=0;

    ret=pmic_read_interface( (kal_uint32)(MT6332_SWCID),
                           (&val),
                           (kal_uint32)(MT6332_PMIC_SWCID_MASK),
                           (kal_uint32)(MT6332_PMIC_SWCID_SHIFT)
	                       );                           

    return val;
}

int pmic_detect_powerkey(void)
{
    U32 ret=0;
    U32 val=0;

    ret=pmic_read_interface( (U32)(MT6331_TOPSTATUS),
                             (&val),
                             (U32)(MT6331_PMIC_PWRKEY_DEB_MASK),
                             (U32)(MT6331_PMIC_PWRKEY_DEB_SHIFT)
                             );

    if (val==1){     
        printf("pl pmic powerkey Release\n");
        return 0;
    }else{
        printf("pl pmic powerkey Press\n");
        return 1;
    }
}

int pmic_detect_homekey(void)
{
    U32 ret=0;
    U32 val=0;

    ret=pmic_read_interface( (U32)(MT6331_TOPSTATUS),
                             (&val),
                             (U32)(MT6331_PMIC_HOMEKEY_DEB_MASK),
                             (U32)(MT6331_PMIC_HOMEKEY_DEB_SHIFT)
                             );

                             
    if (val==1){     
        printf("pl pmic FCHRKEY Release\n");
        return 0;
    }else{
        printf("pl pmic FCHRKEY Press\n");
        return 1;
    }
}

U32 pmic_IsUsbCableIn (void) 
{    
    U32 ret=0;
    U32 val=0;

    #ifdef SLT
    val = 1; // for bring up
    printf("[pmic_IsUsbCableIn] have CFG_EVB_PLATFORM, %d\n", val);
    #else
    pmic_config_interface(0x10A, 0x1,  0xF,  8);
    pmic_config_interface(0x10A, 0x17, 0xFF, 0);
    pmic_read_interface(0x108, &val, 0x1,  1);
    printf("[pmic_IsUsbCableIn] %d\n", val);
    #endif    

    if(val)
        return PMIC_CHRDET_EXIST;
    else
        return PMIC_CHRDET_NOT_EXIST;
}    

static int vbat_status = PMIC_VBAT_NOT_DROP;
static void pmic_DetectVbatDrop (void) 
{    
	U32 ret=0;
	U32 just_rst=0;

	pmic_read_interface( MT6331_STRUP_CON9, (&just_rst), MT6331_PMIC_JUST_PWRKEY_RST_MASK, MT6331_PMIC_JUST_PWRKEY_RST_SHIFT );
	pmic_config_interface(MT6331_STRUP_CON9, 1, MT6331_PMIC_CLR_JUST_RST_MASK, MT6331_PMIC_CLR_JUST_RST_SHIFT);

	printf("just_rst = %d\n", just_rst);
	if(just_rst)
		vbat_status = PMIC_VBAT_DROP;
	else
		vbat_status = PMIC_VBAT_NOT_DROP;
}

int pmic_IsVbatDrop(void)
{
   return vbat_status;	
}

void hw_set_cc(int cc_val)
{
#if !(CFG_EVB_PLATFORM) || !CFG_FPGA_PLATFORM

    #ifdef MTK_BQ24160_SUPPORT
    bq24160_config_interface(bq24160_CON0, 0x1, 0x1, 7); // wdt reset
    bq24160_config_interface(bq24160_CON7, 0x1, 0x3, 5); // Safty timer
    #else
    //MT6332 SWCHR
    pmic_config_interface(MT6332_CHR_CON8,0x1,MT6332_PMIC_RG_ICH_SEL_SWEN_MASK,MT6332_PMIC_RG_ICH_SEL_SWEN_SHIFT);
    #endif

    if(cc_val==70) // USB current limit at 100mA
    {
        #ifdef MTK_BQ24160_SUPPORT
        bq24160_config_interface(bq24160_CON2, 0x0, 0x7, 4); 
        #else
        //MT6332 SWCHR
        pmic_config_interface(MT6332_CHR_CON10,0x0,MT6332_PMIC_RG_ICH_SEL_MASK,MT6332_PMIC_RG_ICH_SEL_SHIFT); //100mA
        #endif
    }
    else if(cc_val==450) // USB current limit at 450mA 
    {
        #ifdef MTK_BQ24160_SUPPORT
        bq24160_config_interface(bq24160_CON2, 0x2, 0x7, 4); 
        #else
        //MT6332 SWCHR        
        pmic_config_interface(MT6332_CHR_CON10,0x4,MT6332_PMIC_RG_ICH_SEL_MASK,MT6332_PMIC_RG_ICH_SEL_SHIFT); //450mA
        #endif
    }
    else if(cc_val==900) // USB current limit at 900mA 
    {
        #ifdef MTK_BQ24160_SUPPORT
        bq24160_config_interface(bq24160_CON2, 0x4, 0x7, 4); 
        #else
        //MT6332 SWCHR
        pmic_config_interface(MT6332_CHR_CON10,0x9,MT6332_PMIC_RG_ICH_SEL_MASK,MT6332_PMIC_RG_ICH_SEL_SHIFT); //900mA
        #endif
    }
    else
    {
    }

    #ifdef MTK_BQ24160_SUPPORT
    bq24160_dump_register();
    #endif

#endif    
}

int hw_check_battery(void)
{
    #ifdef MTK_DISABLE_POWER_ON_OFF_VOLTAGE_LIMITATION
        printf("ignore bat check !\n");
        return 1;
    #else
        #if CFG_EVB_PLATFORM
            printf("ignore bat check\n");
            return 1;
        #else
            U32 val=0;

            pmic_config_interface(0x1E,0x0,0x1,11); // [11:11]: RG_TESTMODE_SWEN;
            pmic_config_interface(0x8C20,0x0,0x1,11); // [11:11]: RG_TESTMODE_SWEN;    
            
            pmic_config_interface(MT6332_BATON_CON0, 0x1, MT6332_PMIC_RG_BATON_EN_MASK, MT6332_PMIC_RG_BATON_EN_SHIFT);
            pmic_config_interface(MT6332_TOP_CKPDN_CON0_CLR, 0x80C0, 0xFFFF, 0); //enable BIF clock            
            pmic_config_interface(MT6332_LDO_CON2, 0x1, MT6332_PMIC_RG_VBIF28_EN_MASK, MT6332_PMIC_RG_VBIF28_EN_SHIFT);
            
            mdelay(1);
            
            pmic_read_interface(MT6332_BIF_CON31, &val, MT6332_PMIC_BIF_BAT_LOST_MASK, MT6332_PMIC_BIF_BAT_LOST_SHIFT);
            if(val==0)
            {
                printf("bat is exist\n");
                return 1;
            }
            else
            {
                printf("bat NOT exist\n");
                return 0;
            }
        #endif
    #endif
}

void pl_charging(int en_chr)
{
    //no charger feature
}

void pl_kick_chr_wdt(void)
{
#if !(CFG_EVB_PLATFORM) || !CFG_FPGA_PLATFORM

    #ifdef MTK_BQ24160_SUPPORT
    //
    #else
    pmic_config_interface(MT6332_CHRWDT_CON0,0x1,MT6332_PMIC_RG_CHRWDT_EN_MASK,MT6332_PMIC_RG_CHRWDT_EN_SHIFT);
    pmic_config_interface(MT6332_CHRWDT_CON0,0x1,MT6332_PMIC_RG_CHRWDT_WR_MASK,MT6332_PMIC_RG_CHRWDT_WR_SHIFT);
    #endif
    
#endif
}

void pl_close_pre_chr_led(void)
{
    //no charger feature
}

U32 upmu_get_reg_value(kal_uint32 reg)
{
    U32 ret=0;
    U32 reg_val=0;
    
    ret=pmic_read_interface(reg, &reg_val, 0xFFFF, 0x0);
    
    return reg_val;
}

#if !(CFG_EVB_PLATFORM) || !CFG_FPGA_PLATFORM
bool is_in_low_bat(void)
{
    int bat_vol = 0;
#ifdef MTK_BQ24160_SUPPORT
    if (is_in_minsys_mode()==0) {
        bq24160_config_interface(bq24160_CON2, 0x1, CON2_HZ_MODE_MASK, CON2_HZ_MODE_SHIFT);
        bat_vol = PMIC_IMM_GetVbat(5);
        bq24160_config_interface(bq24160_CON2, 0x0, CON2_HZ_MODE_MASK, CON2_HZ_MODE_SHIFT);
        print("[is_in_low_bat] bat_vol = %d\n", bat_vol);
        if ((bat_vol < SYSTEM_ON_VOLTAGE))
            return 1;
        else {
            return 0;
        }
    } else {
        return 1;
    }
#else
    kal_uint32 is_m3_en = 0;

    //MT6332 SWCHR
    bat_vol = PMIC_IMM_GetVbat(5);	
    print("[is_in_low_bat] bat_vol = %d vs %d\n", bat_vol, SYSTEM_ON_VOLTAGE);
    if ((bat_vol < SYSTEM_ON_VOLTAGE))
    {
        return 1;
    }
    else 
    {
        pmic_read_interface(0x805E, &is_m3_en, 0x1, 2); //RGS_M3_EN
        if(is_m3_en==1)
        {
            print("[is_in_low_bat] M3 already enable\n");

            #if (CFG_HIGH_BATTERY_VOLTAGE_SUPPORT) // set in custom\project_name\preloader\cust_bldr.mak
            //set CV_VTH=Vsys=4.35V
            pmic_config_interface(MT6332_CHR_CON11,0x5,MT6332_PMIC_RG_CV_SEL_MASK,MT6332_PMIC_RG_CV_SEL_SHIFT);
            pmic_config_interface(MT6332_CHR_CON13,0x5,MT6332_PMIC_RG_CV_PP_SEL_MASK,MT6332_PMIC_RG_CV_PP_SEL_SHIFT);
            #else
            //set CV_VTH=Vsys=4.2V
            pmic_config_interface(MT6332_CHR_CON11,0x8,MT6332_PMIC_RG_CV_SEL_MASK,MT6332_PMIC_RG_CV_SEL_SHIFT);
            pmic_config_interface(MT6332_CHR_CON13,0x8,MT6332_PMIC_RG_CV_PP_SEL_MASK,MT6332_PMIC_RG_CV_PP_SEL_SHIFT);
            #endif

            print("[is_in_low_bat] Reg[0x%x]=0x%x,Reg[0x%x]=0x%x,Reg[0x%x]=0x%x\n", 
                MT6332_CHR_CON11, upmu_get_reg_value(MT6332_CHR_CON11),
                MT6332_CHR_CON13, upmu_get_reg_value(MT6332_CHR_CON13),
                0x816A, upmu_get_reg_value(0x816A)
                );
        }
        else
        {
            //set CV_VTH=Vsys=3.4V
            pmic_config_interface(MT6332_CHR_CON11,0x18,MT6332_PMIC_RG_CV_SEL_MASK,MT6332_PMIC_RG_CV_SEL_SHIFT);
            pmic_config_interface(MT6332_CHR_CON13,0x18,MT6332_PMIC_RG_CV_PP_SEL_MASK,MT6332_PMIC_RG_CV_PP_SEL_SHIFT);
            print("[is_in_low_bat] set CV_VTH=Vsys=3.4V\n");
            //enable M3
            pmic_config_interface(MT6332_CHR_CON24,0x1,MT6332_PMIC_RG_PRECC_M3_EN_MASK,MT6332_PMIC_RG_PRECC_M3_EN_SHIFT);        
            print("[is_in_low_bat] enable M3\n");
        }        
        return 0;
    }
#endif
}
#endif

void mt6332_turn_on_charging(void)
{
    //kick WDT
    pmic_config_interface(MT6332_CHRWDT_CON0,0x1,MT6332_PMIC_RG_CHRWDT_EN_MASK,MT6332_PMIC_RG_CHRWDT_EN_SHIFT);
    pmic_config_interface(MT6332_CHRWDT_CON0,0x1,MT6332_PMIC_RG_CHRWDT_WR_MASK,MT6332_PMIC_RG_CHRWDT_WR_SHIFT);

    //set PRECC
    pmic_config_interface(MT6332_CHR_CON8,0x1,MT6332_PMIC_RG_IPRECC_SWEN_MASK,MT6332_PMIC_RG_IPRECC_SWEN_SHIFT);
    pmic_config_interface(MT6332_CHR_CON9,0x3,MT6332_PMIC_RG_IPRECC_MASK,MT6332_PMIC_RG_IPRECC_SHIFT); // 450mA
}

void mt6332_dump_register(void)
{
    print("[MT6332] Reg[0x%x]=0x%x,Reg[0x%x]=0x%x,Reg[0x%x]=0x%x,Reg[0x%x]=0x%x,Reg[0x%x]=0x%x,Reg[0x%x]=0x%x.\n", 
        MT6332_CHRWDT_CON0, upmu_get_reg_value(MT6332_CHRWDT_CON0),
        MT6332_CHR_CON8, upmu_get_reg_value(MT6332_CHR_CON8),
        MT6332_CHR_CON9, upmu_get_reg_value(MT6332_CHR_CON9),
        MT6332_CHR_CON11, upmu_get_reg_value(MT6332_CHR_CON11),
        MT6332_CHR_CON13, upmu_get_reg_value(MT6332_CHR_CON13),
        0x805E, upmu_get_reg_value(0x805E)
        );
}

//==============================================================================
// PMIC 6331 EFUSE
//==============================================================================

void get_pmic_6331_efuse_data(U32 *efuse_data)
{
    U32 ret=0;
    U32 reg_val=0;        
    int i=0;
    
    //1. enable efuse ctrl engine clock
    ret=pmic_config_interface(0x0154, 0x0010, 0xFFFF, 0);
    ret=pmic_config_interface(0x0148, 0x0004, 0xFFFF, 0);
    //2.
    ret=pmic_config_interface(0x0616, 0x1, 0x1, 0);
    for(i=0;i<=0x1F;i++)
    {
        if(i<0xF)
        {
            efuse_data[i]=0;
            //print("[get_pmic_6331_efuse_data] efuse_data[0x%x]=0x%x\n",i, efuse_data[i]);
            continue;
        }    
        //3. set row to read
        ret=pmic_config_interface(0x0600, i, 0x1F, 1);
        //4. Toggle
        ret=pmic_read_interface(0x610, &reg_val, 0x1, 0);
        if(reg_val==0)
            ret=pmic_config_interface(0x610, 1, 0x1, 0);
        else
            ret=pmic_config_interface(0x610, 0, 0x1, 0);

        reg_val=1;
        while(reg_val == 1)
        {
            ret=pmic_read_interface(0x61A, &reg_val, 0x1, 0);
            //print("[get_pmic_6331_efuse_data] polling Reg[0x61A][0]=0x%x\n", reg_val);
        }

		udelay(1000);//Need to delay at least 1ms for 0x61A and than can read 0x618
        //print("5. 6331 delay 1 ms\n");

        //6. read data
        if(i==0xF)
            efuse_data[i] = upmu_get_reg_value(0x063C);
        else
            efuse_data[i] = upmu_get_reg_value(0x0618);
        //print("[get_pmic_6331_efuse_data] efuse_data[0x%x]=0x%x\n", i, efuse_data[i]);
    }
    //7. Disable efuse ctrl engine clock
    ret=pmic_config_interface(0x0146, 0x0004, 0xFFFF, 0);
    ret=pmic_config_interface(0x0152, 0x0010, 0xFFFF, 0); // new add
}

/*
0x0,0,15
0x1,16,31
0x2,32,47
0x3,48,63
0x4,64,79
0x5,80,95
0x6,96,111
0x7,112,127
0x8,128,143
0x9,144,159
0xa,160,175
0xb,176,191
0xc,192,207
0xd,208,223
0xe,224,239
0xf,240,255
0x10,256,271
0x11,272,287
0x12,288,303
0x13,304,319
0x14,320,335
0x15,336,351
0x16,352,367
0x17,368,383
0x18,384,399
0x19,400,415
0x1a,416,431
0x1b,432,447
0x1c,448,463
0x1d,464,479
0x1e,480,495
0x1f,496,511
*/

void pmic_6331_efuse_check(void)
{
    print("Reg[0x%x]=0x%x\n", 0x240, upmu_get_reg_value(0x240));
    print("Reg[0x%x]=0x%x\n", 0x2A2, upmu_get_reg_value(0x2A2));
    print("Reg[0x%x]=0x%x\n", 0x30A, upmu_get_reg_value(0x30A));
    print("Reg[0x%x]=0x%x\n", 0x334, upmu_get_reg_value(0x334));
    print("Reg[0x%x]=0x%x\n", 0x35E, upmu_get_reg_value(0x35E));
    print("Reg[0x%x]=0x%x\n", 0x260, upmu_get_reg_value(0x260));
    print("Reg[0x%x]=0x%x\n", 0x576, upmu_get_reg_value(0x576));
    print("Reg[0x%x]=0x%x\n", 0x52E, upmu_get_reg_value(0x52E));
    print("Reg[0x%x]=0x%x\n", 0x242, upmu_get_reg_value(0x242));
    print("Reg[0x%x]=0x%x\n", 0x510, upmu_get_reg_value(0x510));
    print("Reg[0x%x]=0x%x\n", 0x572, upmu_get_reg_value(0x572));
    print("Reg[0x%x]=0x%x\n", 0x574, upmu_get_reg_value(0x574));
}

void pmic_6331_efuse_management(void)
{
    U32 efuse_data[0x20]={0};
    int i=0;
    int is_efuse_trimed=0;

    //get efuse data
    get_pmic_6331_efuse_data(efuse_data);

    //dump efuse data for check
    for(i=0xF;i<0x20;i++)
        print("[6331] efuse_data[0x%x]=0x%x\n", i, efuse_data[i]);

#if 1
    is_efuse_trimed = (efuse_data[0xf]>>15)&0x0001;

    print("[6331] is_efuse_trimed=0x%x\n", is_efuse_trimed);

    if(is_efuse_trimed == 1)
    {
        print("Before apply pmic efuse\n");
        pmic_6331_efuse_check();
        
        pmic_config_interface(0x240,((efuse_data[0x10]>> 0)&0x0001),0x1,4);
        pmic_config_interface(0x240,((efuse_data[0x10]>> 1)&0x0001),0x1,5);
        pmic_config_interface(0x240,((efuse_data[0x10]>> 2)&0x0001),0x1,6);

        pmic_config_interface(0x2A2,((efuse_data[0x10]>> 6)&0x0001),0x1,4);
        pmic_config_interface(0x2A2,((efuse_data[0x10]>> 7)&0x0001),0x1,5);
        pmic_config_interface(0x2A2,((efuse_data[0x10]>> 8)&0x0001),0x1,6);

        pmic_config_interface(0x30A,((efuse_data[0x10]>>12)&0x0001),0x1,4);
        pmic_config_interface(0x30A,((efuse_data[0x10]>>13)&0x0001),0x1,5);
        pmic_config_interface(0x30A,((efuse_data[0x10]>>14)&0x0001),0x1,6);

        pmic_config_interface(0x334,((efuse_data[0x10]>>15)&0x0001),0x1,4);
        pmic_config_interface(0x334,((efuse_data[0x11]>> 0)&0x0001),0x1,5);
        pmic_config_interface(0x334,((efuse_data[0x11]>> 1)&0x0001),0x1,6);

        pmic_config_interface(0x35E,((efuse_data[0x11]>> 2)&0x0001),0x1,4);
        pmic_config_interface(0x35E,((efuse_data[0x11]>> 3)&0x0001),0x1,5);
        pmic_config_interface(0x35E,((efuse_data[0x11]>> 4)&0x0001),0x1,6);

        pmic_config_interface(0x260,((efuse_data[0x12]>>13)&0x0001),0x1,0);
        pmic_config_interface(0x260,((efuse_data[0x12]>>14)&0x0001),0x1,1);
        pmic_config_interface(0x260,((efuse_data[0x12]>>15)&0x0001),0x1,2);

        pmic_config_interface(0x576,((efuse_data[0x13]>> 0)&0x0001),0x1,9);
        pmic_config_interface(0x576,((efuse_data[0x13]>> 1)&0x0001),0x1,10);
        pmic_config_interface(0x576,((efuse_data[0x13]>> 2)&0x0001),0x1,11);

        pmic_config_interface(0x52E,((efuse_data[0x13]>> 3)&0x0001),0x1,8);
        pmic_config_interface(0x52E,((efuse_data[0x13]>> 4)&0x0001),0x1,9);
        pmic_config_interface(0x52E,((efuse_data[0x13]>> 5)&0x0001),0x1,10);
        pmic_config_interface(0x52E,((efuse_data[0x13]>> 6)&0x0001),0x1,11);

        pmic_config_interface(0x242,((efuse_data[0x17]>> 8)&0x0001),0x1,2);
        pmic_config_interface(0x242,((efuse_data[0x17]>> 9)&0x0001),0x1,3);
        pmic_config_interface(0x242,((efuse_data[0x17]>>10)&0x0001),0x1,4);
        pmic_config_interface(0x242,((efuse_data[0x17]>>11)&0x0001),0x1,5);
        pmic_config_interface(0x242,((efuse_data[0x17]>>12)&0x0001),0x1,6);
        pmic_config_interface(0x242,((efuse_data[0x17]>>13)&0x0001),0x1,7);

        pmic_config_interface(0x510,((efuse_data[0x17]>>14)&0x0001),0x1,8);
        pmic_config_interface(0x510,((efuse_data[0x17]>>15)&0x0001),0x1,9);
        pmic_config_interface(0x510,((efuse_data[0x18]>> 0)&0x0001),0x1,10);
        pmic_config_interface(0x510,((efuse_data[0x18]>> 1)&0x0001),0x1,11);

        pmic_config_interface(0x572,((efuse_data[0x18]>> 2)&0x0001),0x1,8);
        pmic_config_interface(0x572,((efuse_data[0x18]>> 3)&0x0001),0x1,9);
        pmic_config_interface(0x572,((efuse_data[0x18]>> 4)&0x0001),0x1,10);
        pmic_config_interface(0x572,((efuse_data[0x18]>> 5)&0x0001),0x1,11);

        pmic_config_interface(0x574,((efuse_data[0x18]>> 6)&0x0001),0x1,8);
        pmic_config_interface(0x574,((efuse_data[0x18]>> 7)&0x0001),0x1,9);
        pmic_config_interface(0x574,((efuse_data[0x18]>> 8)&0x0001),0x1,10);
        pmic_config_interface(0x574,((efuse_data[0x18]>> 9)&0x0001),0x1,11);

        print("After apply pmic efuse\n");
        pmic_6331_efuse_check();
    }
#endif

}

//==============================================================================
// PMIC 6332 EFUSE
//==============================================================================

void get_pmic_6332_efuse_data(U32 *efuse_data)
{
    U32 ret=0;
    U32 reg_val=0;        
    int i=0;
    
    //1. enable efuse ctrl engine clock
    ret=pmic_config_interface(0x80B6, 0x0010, 0xFFFF, 0);
    ret=pmic_config_interface(0x80A4, 0x0004, 0xFFFF, 0);
    //2.
    ret=pmic_config_interface(0x8C6C, 0x1, 0x1, 0);
    for(i=0;i<=0x1F;i++)
    {
        if(i<0xF)
        {
            efuse_data[i]=0;
            //print("[get_pmic_6332_efuse_data] efuse_data[0x%x]=0x%x\n",i, efuse_data[i]);
            continue;
        } 
        //3. set row to read
        ret=pmic_config_interface(0x8C56, i, 0x1F, 1);
        //4. Toggle
        ret=pmic_read_interface(0x8C66, &reg_val, 0x1, 0);
        if(reg_val==0)
            ret=pmic_config_interface(0x8C66, 1, 0x1, 0);
        else
            ret=pmic_config_interface(0x8C66, 0, 0x1, 0);

        reg_val=1;
        while(reg_val == 1)
        {
            ret=pmic_read_interface(0x8C70, &reg_val, 0x1, 0);
            //print("[get_pmic_6332_efuse_data] polling Reg[0x61A][0]=0x%x\n", reg_val);
        }

		udelay(1000);//Need to delay at least 1ms for 0x8C70 and than can read 0x8C6E
        //print("5. 6332 delay 1 ms\n");

        //6. read data
        if(i==0xF)
            efuse_data[i] = upmu_get_reg_value(0x8C92);
        else
            efuse_data[i] = upmu_get_reg_value(0x8C6E);
        //print("[get_pmic_6332_efuse_data] efuse_data[0x%x]=0x%x\n", i, efuse_data[i]);
    }
    //7. Disable efuse ctrl engine clock
    ret=pmic_config_interface(0x80A2, 0x0004, 0xFFFF, 0);
    ret=pmic_config_interface(0x80B4, 0x0010, 0xFFFF, 0); // new add
}

/*
0x0,0,15
0x1,16,31
0x2,32,47
0x3,48,63
0x4,64,79
0x5,80,95
0x6,96,111
0x7,112,127
0x8,128,143
0x9,144,159
0xa,160,175
0xb,176,191
0xc,192,207
0xd,208,223
0xe,224,239
0xf,240,255
0x10,256,271
0x11,272,287
0x12,288,303
0x13,304,319
0x14,320,335
0x15,336,351
0x16,352,367
0x17,368,383
0x18,384,399
0x19,400,415
0x1a,416,431
0x1b,432,447
0x1c,448,463
0x1d,464,479
0x1e,480,495
0x1f,496,511
*/

void pmic_6332_efuse_check(void)
{
    print("Reg[0x%x]=0x%x\n", 0x8C3C, upmu_get_reg_value(0x8C3C));
    print("Reg[0x%x]=0x%x\n", 0x8CE0, upmu_get_reg_value(0x8CE0));
    print("Reg[0x%x]=0x%x\n", 0x802E, upmu_get_reg_value(0x802E));
    print("Reg[0x%x]=0x%x\n", 0x8030, upmu_get_reg_value(0x8030));
    print("Reg[0x%x]=0x%x\n", 0x84F6, upmu_get_reg_value(0x84F6));
    print("Reg[0x%x]=0x%x\n", 0x84F8, upmu_get_reg_value(0x84F8));
    print("Reg[0x%x]=0x%x\n", 0x8528, upmu_get_reg_value(0x8528));
    print("Reg[0x%x]=0x%x\n", 0x852C, upmu_get_reg_value(0x852C));
    print("Reg[0x%x]=0x%x\n", 0x852E, upmu_get_reg_value(0x852E));
    print("Reg[0x%x]=0x%x\n", 0x8530, upmu_get_reg_value(0x8530));
    print("Reg[0x%x]=0x%x\n", 0x849C, upmu_get_reg_value(0x849C));
    print("Reg[0x%x]=0x%x\n", 0x84C8, upmu_get_reg_value(0x84C8));
    print("Reg[0x%x]=0x%x\n", 0x84A4, upmu_get_reg_value(0x84A4));
    print("Reg[0x%x]=0x%x\n", 0x84D0, upmu_get_reg_value(0x84D0));
    print("Reg[0x%x]=0x%x\n", 0x8D10, upmu_get_reg_value(0x8D10));
    print("Reg[0x%x]=0x%x\n", 0x815E, upmu_get_reg_value(0x815E));
}

void pmic_6332_efuse_management(void)
{
    U32 efuse_data[0x20]={0};
    int i=0;
    int is_efuse_trimed=0;
    int is_ocs_trim=0;

    //get efuse data
    get_pmic_6332_efuse_data(efuse_data);

    //dump efuse data for check
    for(i=0xF;i<0x20;i++)
        print("[6332] efuse_data[0x%x]=0x%x\n", i, efuse_data[i]);

#if 1
    is_ocs_trim = (efuse_data[0xf]>>14)&0x0001;
    if(is_ocs_trim == 0)
    {           
        print("before Reg[0x%x]=0x%x\n", 0x803C, upmu_get_reg_value(0x803C));    
        pmic_config_interface(0x803C,0x0,0x1, 8);
        pmic_config_interface(0x803C,0x0,0x1, 9);
        pmic_config_interface(0x803C,0x1,0x1,10);
        pmic_config_interface(0x803C,0x0,0x1,11);
        pmic_config_interface(0x803C,0x0,0x1,12);
        pmic_config_interface(0x803C,0x0,0x1,13);
        print("after Reg[0x%x]=0x%x\n", 0x803C, upmu_get_reg_value(0x803C));
    }
#endif    

#if 1
    is_efuse_trimed = (efuse_data[0xf]>>15)&0x0001;

    print("[6332] is_efuse_trimed=0x%x\n", is_efuse_trimed);

    if(is_efuse_trimed == 1)
    {
        print("Before apply pmic efuse\n");
        pmic_6332_efuse_check();
        
        pmic_config_interface(0x8C3C,((efuse_data[0x11]>>12)&0x0001),0x1,10);
        pmic_config_interface(0x8C3C,((efuse_data[0x11]>>13)&0x0001),0x1,11);
        pmic_config_interface(0x8C3C,((efuse_data[0x11]>>14)&0x0001),0x1,12);
        pmic_config_interface(0x8C3C,((efuse_data[0x11]>>15)&0x0001),0x1,13);
        pmic_config_interface(0x8C3C,((efuse_data[0x12]>> 0)&0x0001),0x1,14);
        pmic_config_interface(0x8C3C,((efuse_data[0x12]>> 1)&0x0001),0x1,15);

        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 2)&0x0001),0x1,8);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 3)&0x0001),0x1,9);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 4)&0x0001),0x1,10);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 5)&0x0001),0x1,11);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 6)&0x0001),0x1,12);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 7)&0x0001),0x1,13);        
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 8)&0x0001),0x1,14);
        pmic_config_interface(0x8CE0,((efuse_data[0x12]>> 9)&0x0001),0x1,15);

        pmic_config_interface(0x802E,((efuse_data[0x12]>>10)&0x0001),0x1,2);
        pmic_config_interface(0x802E,((efuse_data[0x12]>>11)&0x0001),0x1,3);
        pmic_config_interface(0x802E,((efuse_data[0x12]>>12)&0x0001),0x1,4);
        pmic_config_interface(0x802E,((efuse_data[0x12]>>13)&0x0001),0x1,5);

        pmic_config_interface(0x802E,((efuse_data[0x12]>>15)&0x0001),0x1,6);
        pmic_config_interface(0x802E,((efuse_data[0x13]>> 0)&0x0001),0x1,7);
        pmic_config_interface(0x802E,((efuse_data[0x13]>> 1)&0x0001),0x1,8);
        pmic_config_interface(0x802E,((efuse_data[0x13]>> 2)&0x0001),0x1,9);
        pmic_config_interface(0x802E,((efuse_data[0x13]>> 3)&0x0001),0x1,10);
        pmic_config_interface(0x802E,((efuse_data[0x13]>> 4)&0x0001),0x1,11);

        pmic_config_interface(0x8030,((efuse_data[0x13]>> 5)&0x0001),0x1,2);
        pmic_config_interface(0x8030,((efuse_data[0x13]>> 6)&0x0001),0x1,3);
        pmic_config_interface(0x8030,((efuse_data[0x13]>> 7)&0x0001),0x1,4);
        pmic_config_interface(0x8030,((efuse_data[0x13]>> 8)&0x0001),0x1,5);
        pmic_config_interface(0x8030,((efuse_data[0x13]>> 9)&0x0001),0x1,6);
        pmic_config_interface(0x8030,((efuse_data[0x13]>>10)&0x0001),0x1,7);
        pmic_config_interface(0x8030,((efuse_data[0x13]>>11)&0x0001),0x1,8);
        pmic_config_interface(0x8030,((efuse_data[0x13]>>12)&0x0001),0x1,9);
        pmic_config_interface(0x8030,((efuse_data[0x13]>>13)&0x0001),0x1,10);
        pmic_config_interface(0x8030,((efuse_data[0x13]>>14)&0x0001),0x1,11);

        pmic_config_interface(0x84F6,((efuse_data[0x13]>>15)&0x0001),0x1,0);
        pmic_config_interface(0x84F6,((efuse_data[0x14]>> 0)&0x0001),0x1,1);
        pmic_config_interface(0x84F6,((efuse_data[0x14]>> 1)&0x0001),0x1,2);
        pmic_config_interface(0x84F6,((efuse_data[0x14]>> 2)&0x0001),0x1,3);
        pmic_config_interface(0x84F6,((efuse_data[0x14]>> 3)&0x0001),0x1,4);

        pmic_config_interface(0x84F8,((efuse_data[0x14]>> 4)&0x0001),0x1,0);
        pmic_config_interface(0x84F8,((efuse_data[0x14]>> 5)&0x0001),0x1,1);
        pmic_config_interface(0x84F8,((efuse_data[0x14]>> 6)&0x0001),0x1,2);
        pmic_config_interface(0x84F8,((efuse_data[0x14]>> 7)&0x0001),0x1,3);
        pmic_config_interface(0x84F8,((efuse_data[0x14]>> 8)&0x0001),0x1,4);

        pmic_config_interface(0x8528,((efuse_data[0x14]>> 9)&0x0001),0x1,0);
        pmic_config_interface(0x8528,((efuse_data[0x14]>>10)&0x0001),0x1,1);
        pmic_config_interface(0x8528,((efuse_data[0x14]>>11)&0x0001),0x1,2);
        pmic_config_interface(0x8528,((efuse_data[0x14]>>12)&0x0001),0x1,3);
        pmic_config_interface(0x8528,((efuse_data[0x14]>>13)&0x0001),0x1,4);

        pmic_config_interface(0x852C,((efuse_data[0x14]>>14)&0x0001),0x1,0);
        pmic_config_interface(0x852C,((efuse_data[0x14]>>15)&0x0001),0x1,1);
        pmic_config_interface(0x852C,((efuse_data[0x15]>> 0)&0x0001),0x1,2);
        pmic_config_interface(0x852C,((efuse_data[0x15]>> 1)&0x0001),0x1,3);
        pmic_config_interface(0x852C,((efuse_data[0x15]>> 2)&0x0001),0x1,4);
        
        pmic_config_interface(0x852E,((efuse_data[0x15]>> 3)&0x0001),0x1,0);
        pmic_config_interface(0x852E,((efuse_data[0x15]>> 4)&0x0001),0x1,1);
        pmic_config_interface(0x852E,((efuse_data[0x15]>> 5)&0x0001),0x1,2);
        pmic_config_interface(0x852E,((efuse_data[0x15]>> 6)&0x0001),0x1,3);
        pmic_config_interface(0x852E,((efuse_data[0x15]>> 7)&0x0001),0x1,4);
        
        pmic_config_interface(0x8530,((efuse_data[0x15]>> 8)&0x0001),0x1,12);
        pmic_config_interface(0x8530,((efuse_data[0x15]>> 9)&0x0001),0x1,13);
        pmic_config_interface(0x8530,((efuse_data[0x15]>>10)&0x0001),0x1,14);
        pmic_config_interface(0x8530,((efuse_data[0x15]>>11)&0x0001),0x1,15);

        pmic_config_interface(0x849C,((efuse_data[0x15]>>12)&0x0001),0x1,8);
        pmic_config_interface(0x849C,((efuse_data[0x15]>>13)&0x0001),0x1,9);
        pmic_config_interface(0x849C,((efuse_data[0x15]>>14)&0x0001),0x1,10);
        pmic_config_interface(0x849C,((efuse_data[0x15]>>15)&0x0001),0x1,11);
        pmic_config_interface(0x849C,((efuse_data[0x16]>> 0)&0x0001),0x1,12);
        pmic_config_interface(0x849C,((efuse_data[0x16]>> 1)&0x0001),0x1,13);

        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 2)&0x0001),0x1,8);
        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 3)&0x0001),0x1,9);
        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 4)&0x0001),0x1,10);
        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 5)&0x0001),0x1,11);
        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 6)&0x0001),0x1,12);
        pmic_config_interface(0x84C8,((efuse_data[0x16]>> 7)&0x0001),0x1,13);

        pmic_config_interface(0x84A4,((efuse_data[0x16]>> 8)&0x0001),0x1,4);
        pmic_config_interface(0x84A4,((efuse_data[0x16]>> 9)&0x0001),0x1,5);
        pmic_config_interface(0x84A4,((efuse_data[0x16]>>10)&0x0001),0x1,6);

        pmic_config_interface(0x84D0,((efuse_data[0x16]>>11)&0x0001),0x1,4);
        pmic_config_interface(0x84D0,((efuse_data[0x16]>>12)&0x0001),0x1,5);
        pmic_config_interface(0x84D0,((efuse_data[0x16]>>13)&0x0001),0x1,6);

        pmic_config_interface(0x8D10,((efuse_data[0x16]>>14)&0x0001),0x1,0);
        pmic_config_interface(0x8D10,((efuse_data[0x16]>>15)&0x0001),0x1,1);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 0)&0x0001),0x1,2);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 1)&0x0001),0x1,3);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 2)&0x0001),0x1,4);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 3)&0x0001),0x1,5);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 4)&0x0001),0x1,6);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 5)&0x0001),0x1,7);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 6)&0x0001),0x1,8);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 7)&0x0001),0x1,9);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 8)&0x0001),0x1,10);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>> 9)&0x0001),0x1,11);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>>10)&0x0001),0x1,12);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>>11)&0x0001),0x1,13);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>>12)&0x0001),0x1,14);
        pmic_config_interface(0x8D10,((efuse_data[0x17]>>13)&0x0001),0x1,15);

        pmic_config_interface(0x815E,((efuse_data[0x17]>>14)&0x0001),0x1,8);
        pmic_config_interface(0x815E,((efuse_data[0x17]>>15)&0x0001),0x1,9);
        pmic_config_interface(0x815E,((efuse_data[0x18]>> 0)&0x0001),0x1,10);

        pmic_config_interface(0x8030,((efuse_data[0x18]>> 1)&0x0001),0x1,0);
        pmic_config_interface(0x8030,((efuse_data[0x18]>> 2)&0x0001),0x1,1);

        print("After apply pmic efuse\n");
        pmic_6332_efuse_check();
    }
#endif

}

//==============================================================================
// PMIC Init Code
//==============================================================================
U32 pmic_init (void)
{
    U32 ret_code = PMIC_TEST_PASS;
    int ret_val=0;
    int reg_val=0;
    int plug_out_1, plug_out_2;
    unsigned int code = mt_get_chip_hw_code();

    print("[pmic_init] Preloader Start..................\n");    
    print("[pmic_init] MT6331 CHIP Code = 0x%x\n", get_mt6331_pmic_chip_version());
    print("[pmic_init] MT6332 CHIP Code = 0x%x\n", get_mt6332_pmic_chip_version());
    
    if (!hw_check_battery()) {
        ret_val = pmic_config_interface(0x16, 0, 0x1, 0); // [0:0]: STRUP_PWROFF_SEQ_EN; Ricky
        ret_val = pmic_config_interface(0x16, 0, 0x1, 1); // [1:1]: STRUP_PWROFF_PREOFF_EN; Ricky	   	
    }
    ret_val = pmic_read_interface(0x16, &plug_out_1, 0x1, 0); // [0:0]: STRUP_PWROFF_SEQ_EN; Ricky
    ret_val = pmic_read_interface(0x16, &plug_out_2, 0x1, 1); // [1:1]: STRUP_PWROFF_PREOFF_EN; Ricky	
    print("[pmic_init] plug_out_1 = %d, plug_out_2 = %d\n", plug_out_1, plug_out_2);
	//detect V battery Drop 
	pmic_DetectVbatDrop();
    #if 1    
    //HW pre-init for MT6331 and MT6332
    ret_val = pmic_config_interface(0x1E,0x0,0x1,11); // [11:11]: RG_TESTMODE_SWEN;
    ret_val = pmic_config_interface(0x8C20,0x0,0x1,11); // [11:11]: RG_TESTMODE_SWEN;
    print("[pmic_init] HW pre-init Reg[0x%x]=0x%x\n", 0x1E, upmu_get_reg_value(0x1E));
    print("[pmic_init] HW pre-init Reg[0x%x]=0x%x\n", 0x8C20, upmu_get_reg_value(0x8C20));
    #endif

    #if 1
    //20140619, TS
    print("[before TS] [0x%x]=0x%x,[0x%x]=0x%x\n", 0x576, upmu_get_reg_value(0x576), 0x544, upmu_get_reg_value(0x544));
    ret_val = pmic_config_interface(0x576,0x0,0x3,2); 
    ret_val = pmic_config_interface(0x544,0x0,0x1F,11); 
    print("[after TS] [0x%x]=0x%x,[0x%x]=0x%x\n", 0x576, upmu_get_reg_value(0x576), 0x544, upmu_get_reg_value(0x544));
    #endif

    #if 1
    pmic_6331_efuse_management();
    pmic_6332_efuse_management();
    #endif

    //Enable PMIC RST function (depends on main chip RST function)
    #if 1
    //PMIC Digital reset
    ret_val=pmic_config_interface(MT6331_TOP_RST_MISC_CLR, 0x0002, 0xFFFF, 0); //[1]=0, RG_WDTRSTB_MODE
    ret_val=pmic_config_interface(MT6331_TOP_RST_MISC_SET, 0x0001, 0xFFFF, 0); //[0]=1, RG_WDTRSTB_EN
    print("[pmic_init] Reg[0x%x]=0x%x\n", MT6331_TOP_RST_MISC, upmu_get_reg_value(MT6331_TOP_RST_MISC));    
    ret_val=pmic_config_interface(MT6332_TOP_RST_MISC_CLR, 0x0002, 0xFFFF, 0); //[1]=0, RG_WDTRSTB_MODE
    ret_val=pmic_config_interface(MT6332_TOP_RST_MISC_SET, 0x0001, 0xFFFF, 0); //[0]=1, RG_WDTRSTB_EN
    print("[pmic_init] Reg[0x%x]=0x%x\n", MT6332_TOP_RST_MISC, upmu_get_reg_value(MT6332_TOP_RST_MISC));
    #else
    //PMIC HW Full reset
    ret_val=pmic_config_interface(MT6331_TOP_RST_MISC_SET, 0x0002, 0xFFFF, 0); //[1]=1, RG_WDTRSTB_MODE
    ret_val=pmic_config_interface(MT6331_TOP_RST_MISC_SET, 0x0001, 0xFFFF, 0); //[0]=1, RG_WDTRSTB_EN
    print("[pmic_init] Reg[0x%x]=0x%x\n", MT6331_TOP_RST_MISC, upmu_get_reg_value(MT6331_TOP_RST_MISC));    
    ret_val=pmic_config_interface(MT6332_TOP_RST_MISC_SET, 0x0002, 0xFFFF, 0); //[1]=1, RG_WDTRSTB_MODE
    ret_val=pmic_config_interface(MT6332_TOP_RST_MISC_SET, 0x0001, 0xFFFF, 0); //[0]=1, RG_WDTRSTB_EN
    print("[pmic_init] Reg[0x%x]=0x%x\n", MT6332_TOP_RST_MISC, upmu_get_reg_value(MT6332_TOP_RST_MISC));
    #endif

    #if !(CFG_EVB_PLATFORM) || !CFG_FPGA_PLATFORM
    if( pmic_IsUsbCableIn()==PMIC_CHRDET_EXIST )
    {
        #ifdef MTK_BQ24160_SUPPORT
        bq24160_hw_init();
        //bq24160_config_interface(bq24160_CON2, 0x1, CON2_HZ_MODE_MASK, CON2_HZ_MODE_SHIFT);
        #else
        //MT6332 SWCHR, TBD
        #endif
    
        while( is_in_low_bat()==1 )
        {
            platform_wdt_all_kick();
            if( -1 != mt65xx_leds_brightness_set(MT65XX_LED_TYPE_RED, LED_FULL)){
            	print("[pmic_init] low battery, need turn on LED\n");
            }

            //bq24160_config_interface(bq24160_CON2, 0x0, CON2_HZ_MODE_MASK, CON2_HZ_MODE_SHIFT);
            print("[pmic_init] low battery, need charging, please wait\n");

            #ifdef MTK_BQ24160_SUPPORT
            bq24160_turn_on_charging();
            bq24160_dump_register();
            #else
            //MT6332 SWCHR
            mt6332_turn_on_charging();
            mt6332_dump_register();
            #endif
            mdelay(1000);
            if( pmic_IsUsbCableIn()==PMIC_CHRDET_NOT_EXIST )
                break;
        }
        
        if( -1 != mt65xx_leds_brightness_set(MT65XX_LED_TYPE_RED, LED_OFF)){
            print("[pmic_init] need turn off LED\n");
        }
        print("[pmic_init] finish preloader charging\n");
    }
    #endif

    #if 1    
    if (0x6795 == code) {
        print("[pmic_init] HW setting at PL : 2014-11-20_1\n");
        ret_val = pmic_config_interface(0x1E,0x0,0x1,11); // MT6331 STRUP_CON16[11]: RG_TESTMODE_SWEN = 1・b0
        ret_val = pmic_config_interface(0x8C20,0x0,0x1,11); // MT6332 STRUP_CON14[11]: RG_TESTMODE_SWEN = 1・b0
        ret_val = pmic_config_interface(0xE,0x1,0x1,5); //STRUP_CON8[5]: VCORE2_PG_ENB = 1・b1
        ret_val = pmic_config_interface(0xC,0x1,0x1,4); //STRUP_CON7[4]: VCORE1_PG_H2L_EN = 1・b1
        ret_val = pmic_config_interface(0x24C,0x44,0x7F,0); // [6:0]: VDVFS11_VOSEL; 
        ret_val = pmic_config_interface(0x24E,0x44,0x7F,0); // [6:0]: VDVFS11_VOSEL_ON; 
        ret_val = pmic_config_interface(0x36A,0x44,0x7F,0); // [6:0]: VCORE2_VOSEL; 
        ret_val = pmic_config_interface(0x36C,0x44,0x7F,0); // [6:0]: VCORE2_VOSEL_ON; 
        ret_val = pmic_config_interface(0x534,0x44,0x7F,9); // [15:9]: RG_VSRAM_DVFS1_VOSEL; 
        ret_val = pmic_config_interface(0x264,0x44,0x7F,0); // [6:0]: VSRAM_DVFS1_VOSEL_ON; 
        ret_val = pmic_config_interface(0x524,0x0,0x1,11); // [11:11]: RG_VSRAM_DVFS1_ON_CTRL; YP
        ret_val = pmic_config_interface(0x524,0x1,0x1,10); // [10:10]: RG_VSRAM_DVFS1_EN; 
        ret_val = pmic_config_interface(0x8492,0x40,0x7F,0); // [6:0]: VSRAM_DVFS2_VOSEL_ON;     
        ret_val = pmic_config_interface(0x8CC2,0x40,0x7F,9); // [15:9]: RG_VSRAM_DVFS2_VOSEL;
    }
    else
    {
        print("[pmic_init] HW setting at PL : 2014-03-27\n");
        ret_val = pmic_config_interface(0x24C,0x44,0x7F,0); // [6:0]: VDVFS11_VOSEL; 
        ret_val = pmic_config_interface(0x24E,0x44,0x7F,0); // [6:0]: VDVFS11_VOSEL_ON; 
        ret_val = pmic_config_interface(0x36A,0x44,0x7F,0); // [6:0]: VCORE2_VOSEL; 
        ret_val = pmic_config_interface(0x36C,0x44,0x7F,0); // [6:0]: VCORE2_VOSEL_ON; 
        ret_val = pmic_config_interface(0x534,0x44,0x7F,9); // [15:9]: RG_VSRAM_DVFS1_VOSEL; 
        ret_val = pmic_config_interface(0x264,0x44,0x7F,0); // [6:0]: VSRAM_DVFS1_VOSEL_ON; 
        ret_val = pmic_config_interface(0x524,0x0,0x1,11); // [11:11]: RG_VSRAM_DVFS1_ON_CTRL; YP
        ret_val = pmic_config_interface(0x524,0x1,0x1,10); // [10:10]: RG_VSRAM_DVFS1_EN; 
    }
    //---------------------------------------------------------------------
    if(get_mt6331_pmic_chip_version()>=0x3120)
    {
        pmic_read_interface(0x63C,&reg_val,0x3,13);
        if(reg_val==0x0) {
            pmic_config_interface(0x18,0x1,0x1,5);
        } else if(reg_val==0x1) {
            pmic_config_interface(0x18,0x1,0x1,6);
        } else if(reg_val==0x2) {
            pmic_config_interface(0x18,0x1,0x1,2);
        } else if(reg_val==0x3) {
            pmic_config_interface(0x524,0x1,0x1,10);
        } else {
            print("[pmic_init] wrong reg_val=%d\n", reg_val);
        }
        print("[pmic_init] Reg[0x%x]=0x%x (reg_val=%d)\n", 0x18, upmu_get_reg_value(0x18), reg_val);
    }
    //---------------------------------------------------------------------

    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x1E,  upmu_get_reg_value(0x1E));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x8C20,upmu_get_reg_value(0x8C20));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0xE,   upmu_get_reg_value(0xE));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0xC,   upmu_get_reg_value(0xC));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x24c, upmu_get_reg_value(0x24c));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x24E, upmu_get_reg_value(0x24E));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x36A, upmu_get_reg_value(0x36A));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x36C, upmu_get_reg_value(0x36C));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x534, upmu_get_reg_value(0x534));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x264, upmu_get_reg_value(0x264));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x524, upmu_get_reg_value(0x524));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x63C, upmu_get_reg_value(0x63C));
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x8492,upmu_get_reg_value(0x8492));    
    print("[pmic_init] Reg[0x%x]=0x%x\n", 0x8CC2,upmu_get_reg_value(0x8CC2));
    #endif

    #if 1
    da9210_driver_probe();
    #endif

    #ifdef DUMMY_AP
    //print("[pmic_init for DUMMY_AP]\n");
    #endif

    print("[pmic_init] Done...................\n");

    return ret_code;
}

void mt6331_upmu_set_rg_drv_32k_ck_pdn(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_TOP_CKPDN_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_32K_CK_PDN_MASK),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_32K_CK_PDN_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_rg_drv_isink0_ck_pdn(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_TOP_CKPDN_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_ISINK0_CK_PDN_MASK),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_ISINK0_CK_PDN_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_rg_drv_isink0_ck_cksel(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_TOP_CKSEL_CON),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_ISINK0_CK_CKSEL_MASK),
                             (kal_uint32)(MT6331_PMIC_RG_DRV_ISINK0_CK_CKSEL_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_isink_ch0_mode(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_ISINK0_CON0),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_MODE_MASK),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_MODE_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_isink_ch0_step(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_ISINK0_CON2),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_STEP_MASK),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_STEP_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_isink_dim0_duty(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_ISINK0_CON2),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_ISINK_DIM0_DUTY_MASK),
                             (kal_uint32)(MT6331_PMIC_ISINK_DIM0_DUTY_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_isink_dim0_fsel(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_ISINK0_CON1),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_ISINK_DIM0_FSEL_MASK),
                             (kal_uint32)(MT6331_PMIC_ISINK_DIM0_FSEL_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}

void mt6331_upmu_set_isink_ch0_en(kal_uint32 val)
{
  kal_uint32 ret=0;

  ret=pmic_config_interface( (kal_uint32)(MT6331_ISINK_EN_CTRL),
                             (kal_uint32)(val),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_EN_MASK),
                             (kal_uint32)(MT6331_PMIC_ISINK_CH0_EN_SHIFT)
                             );
  //if(ret!=0) dprintf(INFO, "%d", ret);
}
