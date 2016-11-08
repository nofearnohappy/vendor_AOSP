#ifndef _SPM_MTCMOS_
#define _SPM_MTCMOS_

/* XXX: only in kernel
#include <linux/kernel.h>
*/

#define STA_POWER_DOWN  0
#define STA_POWER_ON    1

/*
 * 1. for CPU MTCMOS: CPU0, CPU1, CPU2, CPU3, DBG0, CPU4, CPU5, CPU6, CPU7, DBG1, CPUSYS1
 * 2. call spm_mtcmos_cpu_lock/unlock() before/after any operations
 */
extern void spm_mtcmos_cpu_lock(unsigned long *flags);
extern void spm_mtcmos_cpu_unlock(unsigned long *flags);

extern int spm_mtcmos_ctrl_cpu0(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu1(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu2(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu3(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu4(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu5(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu6(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpu7(int state, int chkWfiBeforePdn);

extern int spm_mtcmos_ctrl_dbg0(int state);

extern int spm_mtcmos_ctrl_cpusys0(int state, int chkWfiBeforePdn);
extern int spm_mtcmos_ctrl_cpusys1(int state, int chkWfiBeforePdn);

extern bool spm_cpusys0_can_power_down(void);
extern bool spm_cpusys1_can_power_down(void);

typedef enum
{
    E1_LITTLE_STAGE_1 = 0,
    E1_BIG_STAGE_1,
    E1_BIG_STAGE_2,
    E1_BIG_STAGE_3,
    E2_STAGE_1,
    E2_STAGE_2,
} CPUSYS_INIT_STAGE;
extern void spm_mtcmos_ctrl_cpusys_init(CPUSYS_INIT_STAGE stage);


/*
 * 1. for non-CPU MTCMOS: VDEC, VENC, ISP, DISP, MFG, INFRA, DDRPHY, MDSYS1, MDSYS2
 * 2. call spm_mtcmos_noncpu_lock/unlock() before/after any operations
 */
extern int spm_topaxi_prot(int bit, int en);

#endif //#ifndef _SPM_MTCMOS_
