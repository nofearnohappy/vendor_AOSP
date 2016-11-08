#ifndef __TRACER_H_
#define __TRACER_H_

#define	CPU_UP	0
#define	CPU_DOWN	1
#define	CPU_SUSPEND	2
#define	CLUSTER_UP	3
#define	CLUSTER_DOWN	4
#define	CLUSTER_SUSPEND	5

void trace_power_flow(unsigned long mpidr, unsigned char mode);

#endif
