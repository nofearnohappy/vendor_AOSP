#ifndef SHF_DEBUG_H_
#define SHF_DEBUG_H_

#include "shf_define.h"

#ifdef SHF_DEBUG_MODE
extern void shf_debug_print_bytes(void* buffer, size_t size);
#endif

#endif /* SHF_DEBUG_H_ */
