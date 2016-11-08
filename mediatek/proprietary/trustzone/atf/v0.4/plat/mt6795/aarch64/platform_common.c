/*
 * Copyright (c) 2013-2014, ARM Limited and Contributors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of ARM nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include <arch.h>
#include <arch_helpers.h>
#include <assert.h>
#include <bl_common.h>
#include <cci400.h>
#include <debug.h>
#include <mmio.h>
#include <platform.h>
#include <xlat_tables.h>
#include "../plat_def.h"
//#include <stdio.h>    for printf


/*******************************************************************************
 * This array holds the characteristics of the differences between the three
 * MTK_platform platforms (Base, A53_A57 & Foundation). It will be populated during cold
 * boot at each boot stage by the primary before enabling the MMU (to allow cci
 * configuration) & used thereafter. Each BL will have its own copy to allow
 * independent operation.
 ******************************************************************************/
static unsigned long mt_config[CONFIG_LIMIT];

/*
 * Table of regions to map using the MMU.
 * This doesn't include TZRAM as the 'mem_layout' argument passed to
 * configure_mmu_elx() will give the available subset of that,
 */

const mmap_region_t mt_mmap[] = {
    /* for ATF text, RO, RW and boot argument */
    {(TZRAM_BASE & PAGE_ADDR_MASK), 
     ((TZRAM_SIZE & ~(PAGE_SIZE_MASK)) + PAGE_SIZE), MT_MEMORY | MT_RW | MT_SECURE},

    /* for T-OS, but we do not load T-OS in ATF. do not need it */
/*    { TZDRAM_BASE,	TZDRAM_SIZE,	MT_MEMORY | MT_RW | MT_SECURE },    */

    /* UART address mapping */
    { MTK_DEVICE_BASE,	MTK_DEVICE_SIZE,	MT_DEVICE | MT_RW | MT_SECURE },

    /* GIC + CCI 400 address mapping */
    {(MT_DEV_BASE & PAGE_ADDR_MASK),MT_DEV_SIZE, MT_DEVICE | MT_RW | MT_SECURE},

    /* For TRNG and Clock Control address mapping */
    // including in MT_DEV_BASE and MT_DEV_SIZE, limit 4 XLAT_TABLES
//    {TRNG_BASE_ADDR, TRNG_BASE_SIZE, MT_DEVICE | MT_RW | MT_SECURE},        
//    {TRNG_PDN_BASE_ADDR, TRNG_PDN_BASE_SIZE, MT_DEVICE | MT_RW | MT_SECURE},

	/* Top-level Reset Generator - WDT */
    { MTK_WDT_BASE, MTK_WDT_SIZE, MT_DEVICE | MT_RW | MT_SECURE },

    /* 2nd GB as device for now...*/
    /* TZC-400 setting, we use Device-APC instead, do not use it yet */
/*    { DRAM1_BASE,	DRAM1_SIZE,	MT_MEMORY | MT_RW | MT_NS },    */
	{0}
};


/*******************************************************************************
 * Macro generating the code for the function setting up the pagetables as per
 * the platform memory map & initialize the mmu, for the given exception level
 ******************************************************************************/
#define DEFINE_CONFIGURE_MMU_EL(_el)					\
	void mt_configure_mmu_el##_el(unsigned long total_base,		\
				   unsigned long total_size,		\
				   unsigned long ro_start,		\
				   unsigned long ro_limit,		\
				   unsigned long coh_start,		\
				   unsigned long coh_limit)		\
	{								\
		mmap_add_region(total_base,				\
				total_size,				\
				MT_MEMORY | MT_RW | MT_SECURE);		\
		mmap_add_region(ro_start, ro_limit - ro_start,		\
				MT_MEMORY | MT_RO | MT_SECURE);		\
		mmap_add_region(coh_start, coh_limit - coh_start,	\
				MT_DEVICE | MT_RW | MT_SECURE);		\
		mmap_add(mt_mmap);					\
		init_xlat_tables();					\
									\
		enable_mmu_el##_el();					\
	}

/* Define EL1 and EL3 variants of the function initialising the MMU */
DEFINE_CONFIGURE_MMU_EL(1)
DEFINE_CONFIGURE_MMU_EL(3)

/* Simple routine which returns a configuration variable value */
unsigned long mt_get_cfgvar(unsigned int var_id)
{
	assert(var_id < CONFIG_LIMIT);
	return mt_config[var_id];
}

/*******************************************************************************
 * A single boot loader stack is expected to work on both the Foundation MTK_platform
 * models and the two flavours of the Base MTK_platform models (AEMv8 & Cortex). The
 * SYS_ID register provides a mechanism for detecting the differences between
 * these platforms. This information is stored in a per-BL array to allow the
 * code to take the correct path.Per BL platform configuration.
 ******************************************************************************/
int mt_config_setup(void)
{
	unsigned int rev, hbi, bld, arch, sys_id, midr_pn;

	sys_id = mmio_read_32(VE_SYSREGS_BASE + V2M_SYS_ID);
	rev = (sys_id >> SYS_ID_REV_SHIFT) & SYS_ID_REV_MASK;
	hbi = (sys_id >> SYS_ID_HBI_SHIFT) & SYS_ID_HBI_MASK;
	bld = (sys_id >> SYS_ID_BLD_SHIFT) & SYS_ID_BLD_MASK;
	arch = (sys_id >> SYS_ID_ARCH_SHIFT) & SYS_ID_ARCH_MASK;

    arch = ARCH_MODEL;  //FIXME, bypass this stage
    bld = BLD_GIC_A53A57_MMAP;  //FIXME, bypass this stage
    hbi = HBI_MT_BASE;  //FIXME, bypass this stage

	if ((rev != REV_MT) || (arch != ARCH_MODEL))
		panic();

	/*
	 * The build field in the SYS_ID tells which variant of the GIC
	 * memory is implemented by the model.
	 */
	switch (bld) {
#if 0
	case BLD_GIC_VE_MMAP:
		mt_config[CONFIG_GICD_ADDR] = VE_GICD_BASE;
		mt_config[CONFIG_GICC_ADDR] = VE_GICC_BASE;
		mt_config[CONFIG_GICH_ADDR] = VE_GICH_BASE;
		mt_config[CONFIG_GICV_ADDR] = VE_GICV_BASE;
		break;
#endif
	case BLD_GIC_A53A57_MMAP:
		mt_config[CONFIG_GICD_ADDR] = BASE_GICD_BASE;
		mt_config[CONFIG_GICC_ADDR] = BASE_GICC_BASE;
		mt_config[CONFIG_GICH_ADDR] = BASE_GICH_BASE;
		mt_config[CONFIG_GICV_ADDR] = BASE_GICV_BASE;
		break;
	default:
		assert(0);
	}

	/*
	 * The hbi field in the SYS_ID is 0x020 for the Base MTK_platform & 0x010
	 * for the Foundation MTK_platform.
	 */
	switch (hbi) {
	case HBI_FOUNDATION:
		mt_config[CONFIG_MAX_AFF0] = 4;
		mt_config[CONFIG_MAX_AFF1] = 1;
		mt_config[CONFIG_CPU_SETUP] = 0;
		mt_config[CONFIG_BASE_MMAP] = 0;
		mt_config[CONFIG_HAS_CCI] = 0;
		mt_config[CONFIG_HAS_TZC] = 0;
		break;
	case HBI_MT_BASE:
		midr_pn = (read_midr() >> MIDR_PN_SHIFT) & MIDR_PN_MASK;
		if ((midr_pn == MIDR_PN_A57) || (midr_pn == MIDR_PN_A53))
			mt_config[CONFIG_CPU_SETUP] = 1;
		else
			mt_config[CONFIG_CPU_SETUP] = 0;

		mt_config[CONFIG_MAX_AFF0] = 4;
		mt_config[CONFIG_MAX_AFF1] = 2;
		mt_config[CONFIG_BASE_MMAP] = 1;
		mt_config[CONFIG_HAS_CCI] = 1;
		mt_config[CONFIG_HAS_TZC] = 0;
		break;
	default:
		assert(0);
	}

	return 0;
}

unsigned long plat_get_ns_image_entrypoint(void)
{
//	return NS_IMAGE_OFFSET;
    return BL33_START_ADDRESS;

}

uint64_t plat_get_syscnt_freq(void)
{
	uint64_t counter_base_frequency;

	/* Read the frequency from Frequency modes table */
	//counter_base_frequency = mmio_read_32(SYS_CNTCTL_BASE + CNTFID_OFF);
    counter_base_frequency = 13000000; //FIXME, 13 MHz

	/* The first entry of the frequency modes table must not be 0 */
	assert(counter_base_frequency != 0);

	return counter_base_frequency;
}

void mt_cci_setup(void)
{
	unsigned long cci_setup;

	/*
	 * Enable CCI-400 for this cluster. No need
	 * for locks as no other cpu is active at the
	 * moment
	 */
	cci_setup = mt_get_cfgvar(CONFIG_HAS_CCI);
	if (cci_setup)
		cci_enable_coherency(read_mpidr());
}


/*******************************************************************************
 * Set SPSR and secure state for BL32 image
 ******************************************************************************/
void mt_set_bl32_ep_info(entry_point_info_t *bl32_ep_info)
{
	SET_SECURITY_STATE(bl32_ep_info->h.attr, SECURE);
	/*
	 * The Secure Payload Dispatcher service is responsible for
	 * setting the SPSR prior to entry into the BL32 image.
	 */
	bl32_ep_info->spsr = 0;
}

/*******************************************************************************
 * Set SPSR and secure state for BL33 image
 ******************************************************************************/
void mt_set_bl33_ep_info(entry_point_info_t *bl33_ep_info)
{
	unsigned long el_status;
	unsigned int mode;
    unsigned int rw, ee;
	unsigned long daif;


	/* Figure out what mode we enter the non-secure world in */
	el_status = read_id_aa64pfr0_el1() >> ID_AA64PFR0_EL2_SHIFT;
	el_status &= ID_AA64PFR0_ELX_MASK;

	if (el_status)
		mode = MODE_EL2;
	else
		mode = MODE_EL1;

	/*
	 * TODO: Consider the possibility of specifying the SPSR in
	 * the FIP ToC and allowing the platform to have a say as
	 * well.
	 */


	/*
	 * Figure out whether the cpu enters the non-secure address space
	 * in aarch32 or aarch64
	 */

/*
    typedef enum {
        BOOT_OPT_64S3 = 0,
        BOOT_OPT_64S1,
        BOOT_OPT_32S3,
        BOOT_OPT_32S1,
        BOOT_OPT_64N2,
        BOOT_OPT_64N1,
        BOOT_OPT_32N2,
        BOOT_OPT_32N1,
        BOOT_OPT_UNKNOWN
    } boot_option_t;
  */  

    //setting SPSR, RW 32 or 64
/*
    if ((BOOT_OPT_32N2 == pl_boot_argument.lk_boot_opt) ||
        (BOOT_OPT_32N1 == pl_boot_argument.lk_boot_opt) ||
        (BOOT_OPT_32S1 == pl_boot_argument.lk_boot_opt) ) {
*/
    if (1){
    	rw = 0;
    }else{
        rw = 1;
    }
	if (0 == rw) {
	    printf("LK is AArch32\n");
	    printf("LK start_addr=x0x%x\n", bl33_ep_info->pc);
    	mode = MODE32_svc;
		ee = 0;
		/*
		 * TODO: Choose async. exception bits if HYP mode is not
		 * implemented according to the values of SCR.{AW, FW} bits
		 */
		daif = DAIF_ABT_BIT | DAIF_IRQ_BIT | DAIF_FIQ_BIT;

		bl33_ep_info->spsr = SPSR_MODE32(mode, 0, ee, daif);

		/*
		 * Pass boot argument to LK
		 * ldr     w4, =pl_boot_argument
		 * ldr     w5, =BOOT_ARGUMENT_SIZE
		 */
		bl33_ep_info->args.arg4=(unsigned long)(uintptr_t)BOOT_ARGUMENT_LOCATION;
		bl33_ep_info->args.arg5=(unsigned long)(uintptr_t)BOOT_ARGUMENT_SIZE; 
	} else {
        printf("LK is AArch64\n");
		bl33_ep_info->spsr = SPSR_64(mode, MODE_SP_ELX, DISABLE_ALL_EXCEPTIONS);
    }
    SET_SECURITY_STATE(bl33_ep_info->h.attr, NON_SECURE);
}
