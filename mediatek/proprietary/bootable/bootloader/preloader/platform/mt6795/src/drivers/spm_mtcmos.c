/* XXX: only in kernel
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/spinlock.h>
#include <linux/delay.h>    //udelay

#include <mach/mt_typedefs.h>
#include <mach/mt_spm.h>
#include <mach/mt_spm_mtcmos.h>
#include <mach/hotplug.h>
#include <mach/mt_clkmgr.h>
*/
#include <platform.h>
#include <spm.h>
#include <spm_mtcmos.h>
#include <spm_mtcmos_internal.h>
#include <timer.h>  //udelay
#include <pll.h>
#include <mt_ptp2.h>

/**************************************
 * for CPU MTCMOS
 **************************************/
/* XXX: only in kernel
static DEFINE_SPINLOCK(spm_cpu_lock);


void spm_mtcmos_cpu_lock(unsigned long *flags)
{
    spin_lock_irqsave(&spm_cpu_lock, *flags);
}

void spm_mtcmos_cpu_unlock(unsigned long *flags)
{
    spin_unlock_irqrestore(&spm_cpu_lock, *flags);
}
*/
#define spm_mtcmos_cpu_lock(x)
#define spm_mtcmos_cpu_unlock(x)


int spm_mtcmos_ctrl_cpu0(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN)
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA7_CPU0_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU0_L1_PDN, spm_read(SPM_CA7_CPU0_L1_PDN) | L1_PDN);
        while ((spm_read(SPM_CA7_CPU0_L1_PDN) & L1_PDN_ACK) != L1_PDN_ACK);
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA7_CPU0_PWR_CON, (spm_read(SPM_CA7_CPU0_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU0) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU0) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU0) != CA7_CPU0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU0) != CA7_CPU0));
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA7_CPU0_L1_PDN, spm_read(SPM_CA7_CPU0_L1_PDN) & ~L1_PDN);
        while ((spm_read(SPM_CA7_CPU0_L1_PDN) & L1_PDN_ACK) != 0);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_CPU0_PWR_CON, spm_read(SPM_CA7_CPU0_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu1(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN)
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA7_CPU1_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU1_L1_PDN, spm_read(SPM_CA7_CPU1_L1_PDN) | L1_PDN);
        while ((spm_read(SPM_CA7_CPU1_L1_PDN) & L1_PDN_ACK) != L1_PDN_ACK);
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA7_CPU1_PWR_CON, (spm_read(SPM_CA7_CPU1_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU1) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU1) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU1) != CA7_CPU1) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU1) != CA7_CPU1));
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA7_CPU1_L1_PDN, spm_read(SPM_CA7_CPU1_L1_PDN) & ~L1_PDN);
        while ((spm_read(SPM_CA7_CPU1_L1_PDN) & L1_PDN_ACK) != 0);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_CPU1_PWR_CON, spm_read(SPM_CA7_CPU1_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu2(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA7_CPU2_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU2_L1_PDN, spm_read(SPM_CA7_CPU2_L1_PDN) | L1_PDN);
        while ((spm_read(SPM_CA7_CPU2_L1_PDN) & L1_PDN_ACK) != L1_PDN_ACK);
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA7_CPU2_PWR_CON, (spm_read(SPM_CA7_CPU2_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU2) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU2) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU2) != CA7_CPU2) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU2) != CA7_CPU2));
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA7_CPU2_L1_PDN, spm_read(SPM_CA7_CPU2_L1_PDN) & ~L1_PDN);
        while ((spm_read(SPM_CA7_CPU2_L1_PDN) & L1_PDN_ACK) != 0);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_CPU2_PWR_CON, spm_read(SPM_CA7_CPU2_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu3(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA7_CPU3_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU3_L1_PDN, spm_read(SPM_CA7_CPU3_L1_PDN) | L1_PDN);
        while ((spm_read(SPM_CA7_CPU3_L1_PDN) & L1_PDN_ACK) != L1_PDN_ACK);
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA7_CPU3_PWR_CON, (spm_read(SPM_CA7_CPU3_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU3) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU3) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPU3) != CA7_CPU3) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPU3) != CA7_CPU3));
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA7_CPU3_L1_PDN, spm_read(SPM_CA7_CPU3_L1_PDN) & ~L1_PDN);
        while ((spm_read(SPM_CA7_CPU3_L1_PDN) & L1_PDN_ACK) != 0);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_CPU3_PWR_CON, spm_read(SPM_CA7_CPU3_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu4(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA15_CPU0_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU0_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU0_CA15_L1_PDN_ACK) != CPU0_CA15_L1_PDN_ACK);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU0_CA15_L1_PDN_ISO);
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA15_CPU0_PWR_CON, (spm_read(SPM_CA15_CPU0_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU0) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU0) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
        
        if (!(spm_read(SPM_PWR_STATUS) & (CA15_CPU1 | CA15_CPU2 | CA15_CPU3)) && 
            !(spm_read(SPM_PWR_STATUS_2ND) & (CA15_CPU1 | CA15_CPU2 | CA15_CPU3)))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
    } 
    else /* STA_POWER_ON */
    {
        if (!(spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) &&
            !(spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU0) != CA15_CPU0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU0) != CA15_CPU0));
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU0_CA15_L1_PDN_ISO);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU0_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU0_CA15_L1_PDN_ACK) != 0);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu5(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN)
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA15_CPU1_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU1_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU1_CA15_L1_PDN_ACK) != CPU1_CA15_L1_PDN_ACK);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU1_CA15_L1_PDN_ISO);
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA15_CPU1_PWR_CON, (spm_read(SPM_CA15_CPU1_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU1) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU1) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
        
        if (!(spm_read(SPM_PWR_STATUS) & (CA15_CPU0 | CA15_CPU2 | CA15_CPU3)) && 
            !(spm_read(SPM_PWR_STATUS_2ND) & (CA15_CPU0 | CA15_CPU2 | CA15_CPU3)))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
    } 
    else /* STA_POWER_ON */
    {
        if (!(spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) &&
            !(spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU1) != CA15_CPU1) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU1) != CA15_CPU1));
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU1_CA15_L1_PDN_ISO);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU1_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU1_CA15_L1_PDN_ACK) != 0);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu6(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA15_CPU2_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU2_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU2_CA15_L1_PDN_ACK) != CPU2_CA15_L1_PDN_ACK);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU2_CA15_L1_PDN_ISO);
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA15_CPU2_PWR_CON, (spm_read(SPM_CA15_CPU2_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU2) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU2) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
        
        if (!(spm_read(SPM_PWR_STATUS) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU3)) && 
            !(spm_read(SPM_PWR_STATUS_2ND) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU3)))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
    } 
    else /* STA_POWER_ON */
    {
        if (!(spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) &&
            !(spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU2) != CA15_CPU2) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU2) != CA15_CPU2));
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU2_CA15_L1_PDN_ISO);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU2_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU2_CA15_L1_PDN_ACK) != 0);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpu7(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA15_CPU3_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU3_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU3_CA15_L1_PDN_ACK) != CPU3_CA15_L1_PDN_ACK);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) | CPU3_CA15_L1_PDN_ISO);
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA15_CPU3_PWR_CON, (spm_read(SPM_CA15_CPU3_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU3) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU3) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
        
        if (!(spm_read(SPM_PWR_STATUS) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU2)) && 
            !(spm_read(SPM_PWR_STATUS_2ND) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU2)))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
    } 
    else /* STA_POWER_ON */
    {
        if (!(spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) &&
            !(spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP))
            spm_mtcmos_ctrl_cpusys1(state, chkWfiBeforePdn);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPU3) != CA15_CPU3) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPU3) != CA15_CPU3));
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_CLK_DIS);
        
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU3_CA15_L1_PDN_ISO);
        spm_write(SPM_CA15_L1_PWR_CON, spm_read(SPM_CA15_L1_PWR_CON) & ~CPU3_CA15_L1_PDN);
        while ((spm_read(SPM_CA15_L1_PWR_CON) & CPU3_CA15_L1_PDN_ACK) != 0);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_dbg0(int state)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA7_DBG_PWR_CON, (spm_read(SPM_CA7_DBG_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_DBG) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_DBG) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_DBG) != CA7_DBG) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_DBG) != CA7_DBG));
        
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~PWR_CLK_DIS);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~SRAM_CKISO);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_DBG_PWR_CON, spm_read(SPM_CA7_DBG_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}

#if 0 //There is no dbgsys wrapper in ca15 cpusys
int spm_mtcmos_ctrl_dbg1(int state)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~SRAM_ISOINT_B);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | PWR_ISO);
        spm_write(SPM_MP1_DBG_PWR_CON, (spm_read(SPM_MP1_DBG_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~PWR_ON);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & MP1_DBG) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & MP1_DBG) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & MP1_DBG) != MP1_DBG) || ((spm_read(SPM_PWR_STATUS_2ND) & MP1_DBG) != MP1_DBG));
        
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~PWR_CLK_DIS);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~SRAM_CKISO);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_MP1_DBG_PWR_CON, spm_read(SPM_MP1_DBG_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
    }
    
    return 0;
}
#endif //There is no dbgsys wrapper in ca15 cpusys

int spm_mtcmos_ctrl_cpusys0(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        //TODO: add per cpu power status check?
        
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA7_CPUTOP_STANDBYWFI) == 0);
        
        //if (((spm_read(SPM_PWR_STATUS) & MP1_DBG) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & MP1_DBG) != 0))
        spm_mtcmos_ctrl_dbg0(state);
        
        spm_topaxi_prot(CA7_PDN_REQ, 1);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~SRAM_ISOINT_B);
    #if 1
        spm_write(SPM_CA7_CPUTOP_L2_PDN, spm_read(SPM_CA7_CPUTOP_L2_PDN) | L2_SRAM_PDN);
        while ((spm_read(SPM_CA7_CPUTOP_L2_PDN) & L2_SRAM_PDN_ACK) != L2_SRAM_PDN_ACK);
    #else
        spm_write(SPM_CA7_CPUTOP_L2_SLEEP, spm_read(SPM_CA7_CPUTOP_L2_SLEEP) & ~L2_SRAM_SLEEP_B);
        while ((spm_read(SPM_CA7_CPUTOP_L2_SLEEP) & L2_SRAM_SLEEP_B_ACK) != 0);
    #endif
        
        /***********************************************************************
         * 20140123 
         * Due to there is no bus protect for CA7 L2 share SRAM ADB,
         * adjust the CA7 CPUTOP power off sequence
         **********************************************************************/
        //XXX: original sequence
        //spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_ISO);
        //spm_write(SPM_CA7_CPUTOP_PWR_CON, (spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_CLK_DIS);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~PWR_RST_B);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_ISO);
        
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPUTOP) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPUTOP) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA7_CPUTOP) != CA7_CPUTOP) || ((spm_read(SPM_PWR_STATUS_2ND) & CA7_CPUTOP) != CA7_CPUTOP));
        
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~PWR_CLK_DIS);
        
    #if 1
        spm_write(SPM_CA7_CPUTOP_L2_PDN, spm_read(SPM_CA7_CPUTOP_L2_PDN) & ~L2_SRAM_PDN);
        while ((spm_read(SPM_CA7_CPUTOP_L2_PDN) & L2_SRAM_PDN_ACK) != 0);
    #else
        spm_write(SPM_CA7_CPUTOP_L2_SLEEP, spm_read(SPM_CA7_CPUTOP_L2_SLEEP) | L2_SRAM_SLEEP_B);
        while ((spm_read(SPM_CA7_CPUTOP_L2_SLEEP) & L2_SRAM_SLEEP_B_ACK) != L2_SRAM_SLEEP_B_ACK);
    #endif
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA7_CPUTOP_PWR_CON, spm_read(SPM_CA7_CPUTOP_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
        
        spm_topaxi_prot(CA7_PDN_REQ, 0);
        
        spm_mtcmos_ctrl_dbg0(state);
    }
    
    return 0;
}

int spm_mtcmos_ctrl_cpusys1(int state, int chkWfiBeforePdn)
{
    unsigned long flags;
    
    /* enable register control */
    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
    
    if (state == STA_POWER_DOWN) 
    {
        spm_topaxi_prot(CA15_PDN_REQ, 1);
        
        if (chkWfiBeforePdn)
            while ((spm_read(SPM_SLEEP_TIMER_STA) & CA15_CPUTOP_STANDBYWFI) == 0);
        
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | SRAM_CKISO);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~SRAM_ISOINT_B);
    #if 1
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) | CA15_L2_PDN);
        while ((spm_read(SPM_CA15_L2_PWR_CON) & CA15_L2_PDN_ACK) != CA15_L2_PDN_ACK);
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) | CA15_L2_PDN_ISO);
    #else
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) & ~L2_SRAM_SLEEP_B);
        while ((spm_read(SPM_CA15_L2_PWR_CON) & L2_SRAM_SLEEP_B_ACK) != 0);
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) | CA15_L2_SLEEPB_ISO);
    #endif
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | PWR_ISO);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, (spm_read(SPM_CA15_CPUTOP_PWR_CON) | PWR_CLK_DIS) & ~PWR_RST_B);
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~PWR_ON);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) != 0) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP) != 0));
        
        spm_mtcmos_cpu_unlock(&flags);
    } 
    else /* STA_POWER_ON */
    {
        spm_mtcmos_cpu_lock(&flags);
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | PWR_ON);
        udelay(1);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | PWR_ON_2ND);
        while (((spm_read(SPM_PWR_STATUS) & CA15_CPUTOP) != CA15_CPUTOP) || ((spm_read(SPM_PWR_STATUS_2ND) & CA15_CPUTOP) != CA15_CPUTOP));
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~PWR_CLK_DIS);
        
    #if 1
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) & ~CA15_L2_PDN_ISO);
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) & ~CA15_L2_PDN);
        while ((spm_read(SPM_CA15_L2_PWR_CON) & CA15_L2_PDN_ACK) != 0);
    #else
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) & ~CA15_L2_SLEEPB_ISO);
        spm_write(SPM_CA15_L2_PWR_CON, spm_read(SPM_CA15_L2_PWR_CON) | L2_SRAM_SLEEP_B);
        while ((spm_read(SPM_CA15_L2_PWR_CON) & L2_SRAM_SLEEP_B_ACK) != L2_SRAM_SLEEP_B_ACK);
    #endif
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | SRAM_ISOINT_B);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~SRAM_CKISO);
        
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_CA15_CPUTOP_PWR_CON, spm_read(SPM_CA15_CPUTOP_PWR_CON) | PWR_RST_B);
        
        spm_mtcmos_cpu_unlock(&flags);
        
        spm_topaxi_prot(CA15_PDN_REQ, 0);
    }
    
    return 0;
}

#define SLAVE1_MAGIC_REG (SRAMROM_BASE + 0x38)
#define SLAVE2_MAGIC_REG (SRAMROM_BASE + 0x38)
#define SLAVE3_MAGIC_REG (SRAMROM_BASE + 0x38)

#define SLAVE1_MAGIC_NUM 0x534C4131
#define SLAVE2_MAGIC_NUM 0x4C415332
#define SLAVE3_MAGIC_NUM 0x41534C33

#define SLAVE_JUMP_REG  (SRAMROM_BASE + 0x34)

extern void cpu_wake_up_forever_wfi(void);

void spm_mtcmos_ctrl_cpusys_init(CPUSYS_INIT_STAGE stage)
{
    //FIXME: check with minhsien about function address physical or virtual
    spm_write(SLAVE_JUMP_REG, cpu_wake_up_forever_wfi);
    
    switch (stage)
    {
        case E2_STAGE_1:
            /* enable register control */
            spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
            spm_write(SPM_CA15_CPU3_PWR_CON, CA15_CPU_PWR_CON_DEF_OFF);
            spm_write(SPM_CA15_CPU2_PWR_CON, CA15_CPU_PWR_CON_DEF_OFF);
            spm_write(SPM_CA15_CPU1_PWR_CON, CA15_CPU_PWR_CON_DEF_OFF);
            spm_write(SPM_CA15_CPU0_PWR_CON, CA15_CPU_PWR_CON_DEF_OFF);
            spm_write(SPM_CA15_L1_PWR_CON, CA15_L1_PWR_CON_DEF_OFF);
            ptp2_init();
            turn_on_LO();
            turn_on_FBB();
            //XXX: check with kev -> can we spm_topaxi_prot here? no!
            //spm_topaxi_prot(CA15_PDN_REQ, 1);
            spm_write(SPM_CA15_CPUTOP_PWR_CON, CA15_CPU_PWR_CON_DEF_OFF);
            spm_write(SPM_CA15_L2_PWR_CON, CA15_L2_PWR_CON_DEF_OFF);
            break;
        case E2_STAGE_2:
            spm_write(SLAVE3_MAGIC_REG, SLAVE3_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu3(STA_POWER_DOWN, 1);
            spm_write(SLAVE2_MAGIC_REG, SLAVE2_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu2(STA_POWER_DOWN, 1);
            spm_write(SLAVE1_MAGIC_REG, SLAVE1_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu1(STA_POWER_DOWN, 1);
            //XXX: check with kev, cory, james -> VCA15_PWR_ISO here? Yes!
            spm_write(SPM_SLEEP_DUAL_VCORE_PWR_CON, spm_read(SPM_SLEEP_DUAL_VCORE_PWR_CON) & ~VCA15_PWR_ISO);
            break;
        case E1_LITTLE_STAGE_1:
            /* enable register control */
            spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
            spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_RST_B);
            ptp2_init();
            turn_on_LO();
            turn_on_FBB();
            spm_mtcmos_ctrl_cpusys1(STA_POWER_ON, 1);
            dump_ptp2_ctrl_regs();
            spm_mtcmos_ctrl_cpu7(STA_POWER_DOWN, 0);
            spm_mtcmos_ctrl_cpu6(STA_POWER_DOWN, 0);
            spm_mtcmos_ctrl_cpu5(STA_POWER_DOWN, 0);
            spm_mtcmos_ctrl_cpu4(STA_POWER_DOWN, 0);
            spm_write(SLAVE3_MAGIC_REG, SLAVE3_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu3(STA_POWER_DOWN, 1);
            spm_write(SLAVE2_MAGIC_REG, SLAVE2_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu2(STA_POWER_DOWN, 1);
            spm_write(SLAVE1_MAGIC_REG, SLAVE1_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu1(STA_POWER_DOWN, 1);
            break;

        case E1_BIG_STAGE_1:
            /* enable register control */
            spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));
            spm_write(SPM_CA15_CPU0_PWR_CON, spm_read(SPM_CA15_CPU0_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU1_PWR_CON, spm_read(SPM_CA15_CPU1_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU2_PWR_CON, spm_read(SPM_CA15_CPU2_PWR_CON) & ~PWR_RST_B);
            spm_write(SPM_CA15_CPU3_PWR_CON, spm_read(SPM_CA15_CPU3_PWR_CON) & ~PWR_RST_B);
            ptp2_init();
            turn_on_LO();
            turn_on_FBB();
            spm_mtcmos_ctrl_cpusys1(STA_POWER_ON, 1);
            dump_ptp2_ctrl_regs();
            spm_mtcmos_ctrl_cpu7(STA_POWER_DOWN, 0);
            spm_mtcmos_ctrl_cpu6(STA_POWER_DOWN, 0);
            spm_mtcmos_ctrl_cpu5(STA_POWER_DOWN, 0);
            spm_write(SLAVE3_MAGIC_REG, SLAVE3_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu3(STA_POWER_DOWN, 1);
            spm_write(SLAVE2_MAGIC_REG, SLAVE2_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu2(STA_POWER_DOWN, 1);
            spm_write(SLAVE1_MAGIC_REG, SLAVE1_MAGIC_NUM);
            spm_mtcmos_ctrl_cpu1(STA_POWER_DOWN, 1);
            break;

        case E1_BIG_STAGE_2:
            spm_mtcmos_ctrl_cpu4(STA_POWER_ON, 1);
            break;

        case E1_BIG_STAGE_3:
            spm_mtcmos_ctrl_cpu0(STA_POWER_DOWN, 1);
            //becuase preloader run on cpusys0 L2, so we can't turn off cpusys0 in prealoder and postpone to kernel
            //spm_mtcmos_ctrl_cpusys0(STA_POWER_DOWN, 1);
            break;

        default:
            break;
    } 
}

bool spm_cpusys0_can_power_down(void)
{
    return !(spm_read(SPM_PWR_STATUS) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU2 | CA15_CPU3 | CA15_CPUTOP | CA7_CPU1 | CA7_CPU2 | CA7_CPU3)) &&
           !(spm_read(SPM_PWR_STATUS_2ND) & (CA15_CPU0 | CA15_CPU1 | CA15_CPU2 | CA15_CPU3 | CA15_CPUTOP | CA7_CPU1 | CA7_CPU2 | CA7_CPU3));
}

bool spm_cpusys1_can_power_down(void)
{
    return !(spm_read(SPM_PWR_STATUS) & (CA7_DBG | CA7_CPU0 | CA7_CPU1 | CA7_CPU2 | CA7_CPU3 | CA7_CPUTOP | CA15_CPU1 | CA15_CPU2 | CA15_CPU3)) &&
           !(spm_read(SPM_PWR_STATUS_2ND) & (CA7_DBG | CA7_CPU0 | CA7_CPU1 | CA7_CPU2 | CA7_CPU3 | CA7_CPUTOP | CA15_CPU1 | CA15_CPU2 | CA15_CPU3));
}


/**************************************
 * for non-CPU MTCMOS
 **************************************/
int spm_topaxi_prot(int bit, int en)
{
    if (en == 1) {
        spm_write(TOPAXI_PROT_EN, spm_read(TOPAXI_PROT_EN) | (1<<bit));
        while ((spm_read(TOPAXI_PROT_STA1) & (1<<bit)) != (1<<bit)) {
        }
    } else {
        spm_write(TOPAXI_PROT_EN, spm_read(TOPAXI_PROT_EN) & ~(1<<bit));
        while (spm_read(TOPAXI_PROT_STA1) & (1<<bit)) {
        }
    }    

    return 0;
}
