#include <arch.h>
#include <power_tracer.h>
#include <stdio.h>

#if 0
#define trace_log(...)	printf("psci: " __VA_ARGS__)
#else
#define trace_log(...)
#endif

void trace_power_flow(unsigned long mpidr, unsigned char mode) {
	switch(mode) {
	case CPU_UP:
		trace_log("core %d:%d ON\n",
				(mpidr & MPIDR_CLUSTER_MASK) >> MPIDR_AFFINITY_BITS,
				(mpidr & MPIDR_CPU_MASK));
		break;
	case CPU_DOWN:
		trace_log("core %d:%d OFF\n",
				(mpidr & MPIDR_CLUSTER_MASK) >> MPIDR_AFFINITY_BITS,
				(mpidr & MPIDR_CPU_MASK));
		break;
	case CPU_SUSPEND:
		trace_log("core %d:%d SUSPEND\n",
				(mpidr & MPIDR_CLUSTER_MASK) >> MPIDR_AFFINITY_BITS,
				(mpidr & MPIDR_CPU_MASK));
		break;
	case CLUSTER_UP:
		trace_log("cluster %d ON\n", (mpidr & MPIDR_CLUSTER_MASK) >> MPIDR_AFFINITY_BITS);
		break;
	case CLUSTER_DOWN:
		trace_log("cluster %d OFF\n", (mpidr & MPIDR_CLUSTER_MASK) >> MPIDR_AFFINITY_BITS);
		break;
	case CLUSTER_SUSPEND:
		trace_log("cluster %d SUSPEND\n", (mpidr & MPIDR_CPU_MASK));
		break;
	default:
		trace_log("unknown power mode\n");
		break;
	}
}
