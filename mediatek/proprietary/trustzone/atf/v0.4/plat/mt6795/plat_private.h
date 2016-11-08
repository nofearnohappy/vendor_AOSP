/*
 * Copyright (c) 2014, ARM Limited and Contributors. All rights reserved.
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

#ifndef __PLAT_PRIVATE_H__
#define __PLAT_PRIVATE_H__

#include <bl_common.h>
#include <platform_def.h>

#define LINUX_KERNEL_32 0
#define LINUX_KERNEL_64 1

typedef volatile struct mailbox {
	unsigned long value
	__attribute__((__aligned__(CACHE_WRITEBACK_GRANULE)));
} mailbox_t;

/*******************************************************************************
 * This structure represents the superset of information that is passed to
 * BL31 e.g. while passing control to it from BL2 which is bl31_params
 * and bl31_plat_params and its elements
 ******************************************************************************/
typedef struct bl2_to_bl31_params_mem {
	bl31_params_t bl31_params;
	image_info_t bl31_image_info;
	image_info_t bl32_image_info;
	image_info_t bl33_image_info;
	entry_point_info_t bl33_ep_info;
	entry_point_info_t bl32_ep_info;
	entry_point_info_t bl31_ep_info;
} bl2_to_bl31_params_mem_t;

struct kernel_info {
    uint64_t pc;
    uint64_t r0;
    uint64_t r1;
    uint64_t r2;
    uint64_t k32_64;
};

/*******************************************************************************
 * Forward declarations
 ******************************************************************************/
struct meminfo;

/*******************************************************************************
 * Function and variable prototypes
 ******************************************************************************/
void mt_configure_mmu_el1(unsigned long total_base,
			   unsigned long total_size,
			   unsigned long,
			   unsigned long,
			   unsigned long,
			   unsigned long);
void mt_configure_mmu_el3(unsigned long total_base,
			   unsigned long total_size,
			   unsigned long,
			   unsigned long,
			   unsigned long,
			   unsigned long);
unsigned long mt_get_cfgvar(unsigned int);
int mt_config_setup(void);

#if RESET_TO_BL31
void mt_get_entry_point_info(unsigned long target_security,
				struct entry_point_info *target_entry_info);
#endif
void mt_cci_setup(void);

/* Declarations for mt_gic.c */
void gic_cpuif_deactivate(unsigned int);
void gic_cpuif_setup(unsigned int);
void gic_pcpu_distif_setup(unsigned int);
void gic_setup(void);

/* Declarations for mt_topology.c */
int mt_setup_topology(void);

/* Declarations for mt_io_storage.c */
void mt_io_setup(void);

/* Declarations for mt_security.c */
void mt_security_setup(void);
uint32_t get_devinfo_with_index(uint32_t);
/* Sets the entrypoint for BL32 */
void mt_set_bl32_ep_info(struct entry_point_info *bl32_ep);

/* Sets the entrypoint for BL33 */
void mt_set_bl33_ep_info(struct entry_point_info *bl33_ep);

void enable_ns_access_to_cpuectlr(void);
//L2ACTLR must be written before MMU on and any ACE, CHI or ACP traffic
int workaround_836870(unsigned long mpidr);
int clear_cntvoff(unsigned long mpidr);

uint64_t get_kernel_k32_64(void);
uint64_t get_kernel_info_pc(void);
uint64_t get_kernel_info_r0(void);
uint64_t get_kernel_info_r1(void);
uint64_t get_kernel_info_r2(void);

#endif /* __PLAT_PRIVATE_H__ */
