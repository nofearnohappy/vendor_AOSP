#include "dram_buffer.h"
#include "typedefs.h"
#include "emi.h"

#define MOD "[Dram_Buffer]"

dram_buf_t* g_dram_buf = 0;

void init_dram_buffer(){
	u32 structure_size = sizeof(dram_buf_t);

	print("%s dram_buf_t size: 0x%x \n" ,MOD, structure_size);
	print("%s part_hdr_t size: 0x%x \n" ,MOD, sizeof(part_hdr_t));
	print("%s sizeof(boot_arg_t): 0x%x \n" ,MOD, sizeof(boot_arg_t));
	/*allocate dram_buf*/
#ifdef SLT
  g_dram_buf = CFG_DRAM_ADDR + (2*1024*1024*1024)-(5*1024*1024);
#else
  g_dram_buf = CFG_DRAM_ADDR + 0x2000000;
#endif
    // init boot argument
    memset((void *)&(g_dram_buf->bootarg), 0, sizeof(boot_arg_t));
    
	//make sure dram_buffer if move to the head of memory
	print("%s g_dram_buf start addr: 0x%x \n" ,MOD, g_dram_buf);
	//make sure msdc_gpd_pool and msdc_bd_pool is 64 bytes alignment
	print("%s g_dram_buf->msdc_gpd_pool start addr: 0x%x \n" ,MOD, g_dram_buf->msdc_gpd_pool);
	print("%s g_dram_buf->msdc_bd_pool start addr: 0x%x \n" ,MOD, g_dram_buf->msdc_bd_pool);
}

