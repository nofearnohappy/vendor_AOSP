#include "typedefs.h"
#include "tz_init.h"
#include "tz_emi_reg.h"
#include "tz_emi_mpu.h"

#define MOD "[TZ_EMI_MPU]"

#define readl(addr) (__raw_readl(addr))
#define writel(b,addr) __raw_writel(b,addr)
#define IOMEM(reg) (reg)

/*
 * emi_mpu_set_region_protection: protect a region.
 * @start: start address of the region
 * @end: end address of the region
 * @region: EMI MPU region id
 * @access_permission: EMI MPU access permission
 * Return 0 for success, otherwise negative status code.
 */
int emi_mpu_set_region_protection(unsigned long long start, unsigned long long end, int region, unsigned int access_permission)
{
    int ret = 0;
    unsigned int tmp;
    unsigned long long emi_physical_offset;
        
    if((end != 0) || (start !=0)) 
    {
    	/* if not 4GB mode need offset 0x4000000 */
    	if ((*(volatile unsigned int *)(0x10001f00) & 0x2000) == 0)
    		emi_physical_offset = 0x40000000;
    	else
    		emi_physical_offset = 0;
    	  /* printf("preloader emi_physical_offset=%llx\n",emi_physical_offset); */

        /*Address 64KB alignment*/
        start -= emi_physical_offset;
        end -= emi_physical_offset;
        start = (start >> 16) & 0xFFFF;
        end = (end >> 16) & 0xFFFF;

        if (end <= start) 
        {
            return -1;
        }
    }

    
    switch (region) {
    case 0:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUI)) & 0xFFFF0000;
        writel(0, EMI_MPUI);
        writel((start << 16) | end, EMI_MPUA); 
        writel(tmp | access_permission, EMI_MPUI);
        break; 

    case 1:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUI)) & 0x0000FFFF;
        writel(0, EMI_MPUI);
        writel((start << 16) | end, EMI_MPUB);
        writel(tmp | (access_permission << 16), EMI_MPUI);
        break;

    case 2:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUJ)) & 0xFFFF0000;
        writel(0, EMI_MPUJ);
        writel((start << 16) | end, EMI_MPUC);
        writel(tmp | access_permission, EMI_MPUJ);
        break; 

    case 3:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUJ)) & 0x0000FFFF;
        writel(0, EMI_MPUJ);
        writel((start << 16) | end, EMI_MPUD);
        writel(tmp | (access_permission << 16), EMI_MPUJ);
        break;    
        
    case 4:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUK)) & 0xFFFF0000;
        writel(0, EMI_MPUK);
        writel((start << 16) | end, EMI_MPUE);
        writel(tmp | access_permission, EMI_MPUK);
        break; 

    case 5:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUK)) & 0x0000FFFF;
        writel(0, EMI_MPUK);
        writel((start << 16) | end, EMI_MPUF);
        writel(tmp | (access_permission << 16), EMI_MPUK);
        break;    
        
    case 6:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUL)) & 0xFFFF0000;
        writel(0, EMI_MPUL);
        writel((start << 16) | end, EMI_MPUG); 
        writel(tmp | access_permission, EMI_MPUL);
        break; 

    case 7:
        //Marcos: Clear access right before setting MPU address (Mt6582 design)
        tmp = readl(IOMEM(EMI_MPUL)) & 0x0000FFFF;
        writel(0, EMI_MPUL);
        writel((start << 16) | end, EMI_MPUH);
        writel(tmp | (access_permission << 16), EMI_MPUL);
        break;    
    
       default:
        ret = -1;
        break;
    }

    return ret;
}

void tz_emi_mpu_init(u32 start_add, u32 end_addr)
{
    int ret = 0;
    unsigned int sec_mem_mpu_attr;
    unsigned int sec_mem_phy_start, sec_mem_phy_end;

    /* Caculate start/end address */
    sec_mem_phy_start = start_add;
    sec_mem_phy_end = end_addr;

    // For MT6589
    //==================================================================================================================
    //            | Region |  D0(AP)  |  D1(MD0)  |  D2(Conn) |  D3(MD32) |  D4(MM)  |  D5(MD1)  |  D6(MFG)  |  D7(N/A)
    //------------+---------------------------------------------------------------------------------------------------
    // Secure OS  |    0   |RW(S)     |Forbidden  |Forbidden  |Forbidden  |RW(S)     |Forbidden  |Forbidden  |Forbidden
    //------------+---------------------------------------------------------------------------------------------------
    // MD0 ROM    |    1   |RO(S/NS)  |RO(S/NS)   |Forbidden  |Forbidden
    //------------+------------------------------------------------------
    // MD0 R/W+   |    2   |Forbidden |No protect |Forbidden  |Forbidden
    //------------+------------------------------------------------------
    // MD1 ROM    |    3   |RO(S/NS)  |Forbidden  |RO(S/NS)   |Forbidden
    //------------+------------------------------------------------------
    // MD1 R/W+   |    4   |Forbidden |Forbidden  |No protect |Forbidden
    //------------+------------------------------------------------------
    // MD0 Share  |    5   |No protect|No protect |Forbidden  |Forbidden
    //------------+------------------------------------------------------
    // MD1 Share  |    6   |No protect|Forbidden  |No protect |Forbidden
    //------------+------------------------------------------------------
    // AP         |    7   |No protect|Forbidden  |Forbidden  |No protect
    //===================================================================

    sec_mem_mpu_attr = SET_ACCESS_PERMISSON(LOCK, SEC_RW, FORBIDDEN, FORBIDDEN, SEC_RW);

    print("%s MPU [0x%x-0x%x]\n", MOD, sec_mem_phy_start, sec_mem_phy_end);

    ret = emi_mpu_set_region_protection(sec_mem_phy_start,      /*START_ADDR*/                                    
                                        sec_mem_phy_end,      /*END_ADDR*/                                  
                                        sec_mem_mpu_id,       /*region*/                                   
                                        sec_mem_mpu_attr);

    if(ret)
    {
        print("%s MPU error!!\n", MOD);
    }    
}
