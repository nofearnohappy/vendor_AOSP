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

#include <arch_helpers.h>
#include <assert.h>
#include <bakery_lock.h>
#include <cci400.h>
#include <scu.h>
#include <mmio.h>
#include <platform.h>
#include <console.h>
#include <debug.h>
#include <platform_def.h>
#include <psci.h>
#include <power_tracer.h>
#include <stdio.h>
#include "plat_def.h"
#include "plat_private.h"
#include "aarch64/plat_helpers.h"

#include "mt_cpuxgpt.h" //  generic_timer_backup()

static struct _el3_dormant_data {
        /* unsigned long mp0_l2actlr_el1; */
        unsigned long mp0_l2ectlr_el1;
        unsigned long mp0_l2rstdisable;
        /* unsigned long storage[32]; */
} el3_dormant_data[1] = {{ .mp0_l2ectlr_el1 = 0xDEADDEAD }};


/*******************************************************************************
 * MTK_platform handler called when an affinity instance is about to enter standby.
 ******************************************************************************/
int mt_affinst_standby(unsigned int power_state)
{
	unsigned int target_afflvl;

	/* Sanity check the requested state */
	target_afflvl = psci_get_pstate_afflvl(power_state);

	/*
	 * It's possible to enter standby only on affinity level 0 i.e. a cpu
	 * on the MTK_platform. Ignore any other affinity level.
	 */
	if (target_afflvl != MPIDR_AFFLVL0)
		return PSCI_E_INVALID_PARAMS;

	/*
	 * Enter standby state
	 * dsb is good practice before using wfi to enter low power states
	 */
	dsb();
	wfi();

	return PSCI_E_SUCCESS;
}

/*******************************************************************************
 * MTK_platform handler called when an affinity instance is about to be turned on. The
 * level and mpidr determine the affinity instance.
 ******************************************************************************/
int mt_affinst_on(unsigned long mpidr,
		   unsigned long sec_entrypoint,
		   unsigned long ns_entrypoint,
		   unsigned int afflvl,
		   unsigned int state)
{
	int rc = PSCI_E_SUCCESS;
	unsigned long linear_id;
	mailbox_t *mt_mboxes;

	/*
	 * It's possible to turn on only affinity level 0 i.e. a cpu
	 * on the MTK_platform. Ignore any other affinity level.
	 */
	if (afflvl != MPIDR_AFFLVL0)
		goto exit;

	linear_id = platform_get_core_pos(mpidr);
	mt_mboxes = (mailbox_t *) (MBOX_OFF);
	mt_mboxes[linear_id].value = sec_entrypoint;
	flush_dcache_range((unsigned long) &mt_mboxes[linear_id],
			   sizeof(unsigned long));

	extern void bl31_on_entrypoint(void);
	if (linear_id >= 4) {
		mmio_write_32(MP1_MISC_CONFIG_BOOT_ADDR(linear_id-4), (unsigned long)bl31_on_entrypoint);
		printf("mt_on, entry %x\n", mmio_read_32(MP1_MISC_CONFIG_BOOT_ADDR(linear_id-4)));
	} else {
		mmio_write_32(MP0_MISC_CONFIG_BOOT_ADDR(linear_id), (unsigned long)bl31_on_entrypoint);
		printf("mt_on, entry %x\n", mmio_read_32(MP0_MISC_CONFIG_BOOT_ADDR(linear_id)));
	}

exit:
	return rc;
}

/*******************************************************************************
 * MTK_platform handler called when an affinity instance is about to be turned off. The
 * level and mpidr determine the affinity instance. The 'state' arg. allows the
 * platform to decide whether the cluster is being turned off and take apt
 * actions.
 *
 * CAUTION: This function is called with coherent stacks so that caches can be
 * turned off, flushed and coherency disabled. There is no guarantee that caches
 * will remain turned on across calls to this function as each affinity level is
 * dealt with. So do not write & read global variables across calls. It will be
 * wise to do flush a write to the global to prevent unpredictable results.
 ******************************************************************************/
int mt_affinst_off(unsigned long mpidr,
		    unsigned int afflvl,
		    unsigned int state)
{
	int rc = PSCI_E_SUCCESS;
	unsigned int gicc_base, ectlr;
	unsigned long cpu_setup, cci_setup;

	switch (afflvl) {
	case MPIDR_AFFLVL3:
	case MPIDR_AFFLVL2:
                break;
	case MPIDR_AFFLVL1:
		if (state == PSCI_STATE_OFF) {
			/*
			 * Disable coherency if this cluster is to be
			 * turned off
			 */
			cci_setup = mt_get_cfgvar(CONFIG_HAS_CCI);
			if (cci_setup) {
				cci_disable_coherency(mpidr);
			}
			disable_scu(mpidr);

			trace_power_flow(mpidr, CLUSTER_DOWN);
		}
		break;

	case MPIDR_AFFLVL0:
		if (state == PSCI_STATE_OFF) {

			/*
			 * Take this cpu out of intra-cluster coherency if
			 * the MTK_platform flavour supports the SMP bit.
			 */
			cpu_setup = mt_get_cfgvar(CONFIG_CPU_SETUP);
			if (cpu_setup) {
				ectlr = read_cpuectlr();
				ectlr &= ~CPUECTLR_SMP_BIT;
				write_cpuectlr(ectlr);
			}

			/*
			 * Prevent interrupts from spuriously waking up
			 * this cpu
			 */
                        //gic_cpu_save();
			gicc_base = mt_get_cfgvar(CONFIG_GICC_ADDR);
			gic_cpuif_deactivate(gicc_base);

			trace_power_flow(mpidr, CPU_DOWN);
		}
		break;

	default:
		assert(0);
	}

	return rc;
}

/*******************************************************************************
 * MTK_platform handler called when an affinity instance is about to be suspended. The
 * level and mpidr determine the affinity instance. The 'state' arg. allows the
 * platform to decide whether the cluster is being turned off and take apt
 * actions.
 *
 * CAUTION: This function is called with coherent stacks so that caches can be
 * turned off, flushed and coherency disabled. There is no guarantee that caches
 * will remain turned on across calls to this function as each affinity level is
 * dealt with. So do not write & read global variables across calls. It will be
 * wise to do flush a write to the global to prevent unpredictable results.
 ******************************************************************************/
int mt_affinst_suspend(unsigned long mpidr,
			unsigned long sec_entrypoint,
			unsigned long ns_entrypoint,
			unsigned int afflvl,
			unsigned int state)
{
	int rc = PSCI_E_SUCCESS;
	unsigned int gicc_base, ectlr;
	unsigned long cpu_setup, cci_setup, linear_id;
	mailbox_t *mt_mboxes;

	switch (afflvl) {
        case MPIDR_AFFLVL2: 
                if (state == PSCI_STATE_OFF) {
                        struct _el3_dormant_data *p = &el3_dormant_data[0];

                        /* p->mp0_l2actlr_el1 = read_l2actlr(); */
                        p->mp0_l2ectlr_el1 = read_l2ectlr();

                        //backup L2RSTDISABLE and set as "not disable L2 reset"
                        p->mp0_l2rstdisable = mmio_read_32(MP0_CA7L_CACHE_CONFIG);
                        mmio_write_32(MP0_CA7L_CACHE_CONFIG, 
                                      mmio_read_32(MP0_CA7L_CACHE_CONFIG) & ~L2RSTDISABLE);
                        //backup generic timer
                        //printf("[ATF_Suspend]read_cntpct_el0()=%lu\n", read_cntpct_el0());
                        generic_timer_backup();

                        gic_dist_save();
                }
                break;

	case MPIDR_AFFLVL1:
		if (state == PSCI_STATE_OFF) {
			/*
			 * Disable coherency if this cluster is to be
			 * turned off
			 */
			cci_setup = mt_get_cfgvar(CONFIG_HAS_CCI);
			if (cci_setup) {
				cci_disable_coherency(mpidr);
			}
			disable_scu(mpidr);

			trace_power_flow(mpidr, CLUSTER_SUSPEND);
		}
		break;

	case MPIDR_AFFLVL0:
		if (state == PSCI_STATE_OFF) {
                        //set cpu0 as aa64 for cpu reset
                        mmio_write_32(MP0_MISC_CONFIG3, mmio_read_32(MP0_MISC_CONFIG3) | (1<<12));
			/*
			 * Take this cpu out of intra-cluster coherency if
			 * the MTK_platform flavour supports the SMP bit.
			 */
			cpu_setup = mt_get_cfgvar(CONFIG_CPU_SETUP);
			if (cpu_setup) {
				ectlr = read_cpuectlr();
				ectlr &= ~CPUECTLR_SMP_BIT;
				write_cpuectlr(ectlr);
			}

			/* Program the jump address for the target cpu */
			linear_id = platform_get_core_pos(mpidr);
			mt_mboxes = (mailbox_t *) (MBOX_OFF);
			mt_mboxes[linear_id].value = sec_entrypoint;
			flush_dcache_range((unsigned long) &mt_mboxes[linear_id],
					   sizeof(unsigned long));

			/*
			 * Prevent interrupts from spuriously waking up
			 * this cpu
			 */
                        //gic_cpu_save();
			gicc_base = mt_get_cfgvar(CONFIG_GICC_ADDR);
			gic_cpuif_deactivate(gicc_base);
			trace_power_flow(mpidr, CPU_SUSPEND);
		}
		break;

	default:
		assert(0);
	}

	return rc;
}

#if 1 //defined(CONFIG_ARM_ERRATA_826319)
int workaround_826319(unsigned long mpidr)
{
        unsigned long l2actlr;

        /** only apply on 1st CPU of each cluster **/
        if (mpidr & MPIDR_CPU_MASK)
                return 0;

        /** CONFIG_ARM_ERRATA_826319=y (for 6595/6752)
         * Prog CatB Rare,
         * System might deadlock if a write cannot complete until read data is accepted	
         * worksround: (L2ACTLR[14]=0, L2ACTLR[3]=1).
         * L2ACTLR must be written before MMU on and any ACE, CHI or ACP traffic.
         **/
        l2actlr = read_l2actlr();
        l2actlr = (l2actlr & ~(1<<14)) | (1<<3);
        write_l2actlr(l2actlr);

        return 0;
}
#else //#if defined(CONFIG_ARM_ERRATA_826319)
#define workaround_826319() do {} while(0)
#endif //#if defined(CONFIG_ARM_ERRATA_826319)

#if 1 //defined(CONFIG_ARM_ERRATA_836870)
int workaround_836870(unsigned long mpidr)
{
        unsigned long cpuactlr;

        /** CONFIG_ARM_ERRATA_836870=y (for 6595/6752/6735, prior to r0p4)
         * Prog CatC,
         * Non-allocating reads might prevent a store exclusive from passing
         * worksround: set the CPUACTLR.DTAH bit.
         * The CPU Auxiliary Control Register can be written only when the system 
         * is idle. ARM recommends that you write to this register after a powerup 
         * reset, before the MMU is enabled, and before any ACE or ACP traffic 
         * begins.
         **/
        cpuactlr = read_cpuactlr();
        cpuactlr = cpuactlr | (1<<24);
        write_cpuactlr(cpuactlr);

        return 0;
}
#else //#if defined(CONFIG_ARM_ERRATA_836870)
#define workaround_836870() do {} while(0)
#endif //#if defined(CONFIG_ARM_ERRATA_836870)

int clear_cntvoff(unsigned long mpidr)
{
    unsigned int scr_val, val;

    /**
     * Clear CNTVOFF in ATF for ARMv8 platform
     **/
    val = 0;

    /* set NS_BIT */
    scr_val = read_scr();
    write_scr(scr_val | SCR_NS_BIT);

    write_cntvoff_el2(val);

    /* write back the original value */
    write_scr(scr_val);	

    //printf("[0x%X] cntvoff_el2=0x%x\n",mpidr, read_cntvoff_el2());
    return val;
}

/*******************************************************************************
 * MTK_platform handler called when an affinity instance has just been powered on after
 * being turned off earlier. The level and mpidr determine the affinity
 * instance. The 'state' arg. allows the platform to decide whether the cluster
 * was turned off prior to wakeup and do what's necessary to setup it up
 * correctly.
 ******************************************************************************/
int mt_affinst_on_finish(unsigned long mpidr,
			  unsigned int afflvl,
			  unsigned int state)
{
	int rc = PSCI_E_SUCCESS;
	unsigned long linear_id, cpu_setup;
	mailbox_t *mt_mboxes;
	unsigned int gicd_base, gicc_base,  ectlr;

	switch (afflvl) {

	case MPIDR_AFFLVL2:
                if (state == PSCI_STATE_OFF) {
//                        __asm__ __volatile__ ("1: b 1b \n\t");
                }

		gicd_base = mt_get_cfgvar(CONFIG_GICD_ADDR);
		gic_pcpu_distif_setup(gicd_base);

                break;


	case MPIDR_AFFLVL1:
		/* Enable coherency if this cluster was off */
		if (state == PSCI_STATE_OFF) {
                        //L2ACTLR must be written before MMU on and any ACE, CHI or ACP traffic
                        workaround_826319(mpidr);  
			enable_scu(mpidr);
			mt_cci_setup();
			trace_power_flow(mpidr, CLUSTER_UP);
		}
		break;

	case MPIDR_AFFLVL0:
		/*
		 * Ignore the state passed for a cpu. It could only have
		 * been off if we are here.
		 */

                workaround_836870(mpidr);
		/*
	 * clear CNTVOFF, for slave cores
	 */
	clear_cntvoff(mpidr);

	/*
		 * Turn on intra-cluster coherency if the MTK_platform flavour supports
		 * it.
		 */
		cpu_setup = mt_get_cfgvar(CONFIG_CPU_SETUP);
		if (cpu_setup) {
			ectlr = read_cpuectlr();
			ectlr |= CPUECTLR_SMP_BIT;
			write_cpuectlr(ectlr);
		}

		/* Zero the jump address in the mailbox for this cpu */
		mt_mboxes = (mailbox_t *) (MBOX_OFF);
		linear_id = platform_get_core_pos(mpidr);
		mt_mboxes[linear_id].value = 0;
		flush_dcache_range((unsigned long) &mt_mboxes[linear_id],
				   sizeof(unsigned long));

		gicc_base = mt_get_cfgvar(CONFIG_GICC_ADDR);
		/* Enable the gic cpu interface */
		gic_cpuif_setup(gicc_base);

                //gic_cpu_restore();

#if 0 //fixme, 
		/* Allow access to the System counter timer module */
		reg_val = (1 << CNTACR_RPCT_SHIFT) | (1 << CNTACR_RVCT_SHIFT);
		reg_val |= (1 << CNTACR_RFRQ_SHIFT) | (1 << CNTACR_RVOFF_SHIFT);
		reg_val |= (1 << CNTACR_RWVT_SHIFT) | (1 << CNTACR_RWPT_SHIFT);
		mmio_write_32(SYS_TIMCTL_BASE + CNTACR_BASE(0), reg_val);
		mmio_write_32(SYS_TIMCTL_BASE + CNTACR_BASE(1), reg_val);

		reg_val = (1 << CNTNSAR_NS_SHIFT(0)) |
			(1 << CNTNSAR_NS_SHIFT(1));
		mmio_write_32(SYS_TIMCTL_BASE + CNTNSAR, reg_val);
#endif

		enable_ns_access_to_cpuectlr();

		trace_power_flow(mpidr, CPU_UP);
		break;

	default:
		assert(0);
	}

	return rc;
}

/*******************************************************************************
 * MTK_platform handler called when an affinity instance has just been powered on after
 * having been suspended earlier. The level and mpidr determine the affinity
 * instance.
 * TODO: At the moment we reuse the on finisher and reinitialize the secure
 * context. Need to implement a separate suspend finisher.
 ******************************************************************************/
int mt_affinst_suspend_finish(unsigned long mpidr,
			       unsigned int afflvl,
			       unsigned int state)
{
	int rc = PSCI_E_SUCCESS;

	switch (afflvl) {
	case MPIDR_AFFLVL2:
                if (state == PSCI_STATE_OFF) {
                        struct _el3_dormant_data *p = &el3_dormant_data[0];

                        if (p->mp0_l2ectlr_el1==0xDEADDEAD)
                                panic();
                        else 
                                write_l2ectlr(p->mp0_l2ectlr_el1);
                        /* write_l2actlr(p->mp0_l2actlr_el1); */

                        //restore L2RSTDIRSABLE
                        mmio_write_32(MP0_CA7L_CACHE_CONFIG, 
                                      (mmio_read_32(MP0_CA7L_CACHE_CONFIG) & ~L2RSTDISABLE) 
                                      | (p->mp0_l2rstdisable & L2RSTDISABLE));

                        gic_setup();
                        gic_dist_restore();
                }

                break;

	case MPIDR_AFFLVL1:
	case MPIDR_AFFLVL0:
                return mt_affinst_on_finish(mpidr, afflvl, state);

	default:
		assert(0);
	}

	return rc;
}


/*******************************************************************************
 * Export the platform handlers to enable psci to invoke them
 ******************************************************************************/
static const plat_pm_ops_t mt_plat_pm_ops = {
	mt_affinst_standby,
	mt_affinst_on,
	mt_affinst_off,
	mt_affinst_suspend,
	mt_affinst_on_finish,
	mt_affinst_suspend_finish,
};

/*******************************************************************************
 * Export the platform specific power ops & initialize 
 * the mtk_platform power controller
 ******************************************************************************/
int platform_setup_pm(const plat_pm_ops_t **plat_ops)
{
	*plat_ops = &mt_plat_pm_ops;
	return 0;
}
