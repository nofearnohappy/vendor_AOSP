#include <sys/mman.h>
#include <dlfcn.h>
#include <cutils/log.h>
#include <cutils/atomic.h>
#include <hardware/hardware.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <string.h>
#include <stdlib.h>
#include <sched.h>
#include <sys/resource.h>
#include <linux/fb.h>
#include <wchar.h>
#include <pthread.h>
#include <linux/ion.h>
#include <linux/ion_drv.h>
#include <ion/ion.h>
#include <unistd.h>
#include <linux/mtk_ion.h>
#include <ion.h>

//#pragma GCC optimize ("O0")
#ifdef LOG_TAG
#undef LOG_TAG
#endif
//#define LOG_TAG "ION_TEST"
//#define LogPrint(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, ## __VA_ARGS__)


unsigned int bufsize=1024*1024*8+256;

int ion_custom_ioctl_test()
{
    unsigned int i;
    int ion_fd;
    int ion_test_fd;
    ion_user_handle_t handle;
    int share_fd;
    volatile char* pBuf;
    pid_t pid;

    ion_fd = ion_open();
    if (ion_fd < 0)
    {
        printf("Cannot open ion device.\n");
        return 0;
    }
    if (ion_alloc(ion_fd, bufsize, 0, ION_HEAP_MULTIMEDIA_MASK, 3, &handle))
    {
        printf("IOCTL[ION_IOC_ALLOC] failed!\n");
        return 0;
    }

    if (ion_share(ion_fd, handle, &share_fd))
    {
        printf("IOCTL[ION_IOC_SHARE] failed!\n");
        return 0;
    }

    pBuf = ion_mmap(ion_fd, NULL, bufsize, PROT_READ|PROT_WRITE, MAP_SHARED, share_fd, 0);
    printf("ion_map: pBuf = 0x%x\n", pBuf);
    if (!pBuf)
    {
        printf("Cannot map ion buffer.\n");
        return 0;
    }

    for (i=0; i<bufsize; i+=4)
    {
        *(volatile unsigned int*)(pBuf+i) = i;
    }

    for (i=0; i<bufsize; i+=4)
    {
        if(*(volatile unsigned int*)(pBuf+i) != i)
        {
            printf("ion_test: owner read error !!\n");
        }
    }

    printf("share buffer to child!!\n");
    {
        pid = fork();
        if (pid == 0)
        {   //child
            struct ion_handle *handle;
            ion_fd = open("/dev/ion", O_RDONLY);
            ion_import(ion_fd, share_fd, &handle);
            pBuf = ion_mmap(ion_fd, NULL, bufsize, PROT_READ|PROT_WRITE, MAP_SHARED, share_fd, 0);
            printf("ion_test: map child pBuf = 0x%x\n", pBuf);

            for (i=0; i<bufsize; i+=4)
            {
                if(*(volatile unsigned int*)(pBuf+i) != i)
                {
                    printf("ion_test: child read error 0x%x!=0x%x!!\n", *(volatile unsigned int*)(pBuf+i),i);
                }
            }
            printf("child verify done!\n");

            {
                struct ion_mm_data mm_data;
                mm_data.mm_cmd = ION_MM_CONFIG_BUFFER;
                mm_data.config_buffer_param.handle = handle;
                mm_data.config_buffer_param.eModuleID = 1;
                mm_data.config_buffer_param.security = 0;
                mm_data.config_buffer_param.coherent = 1;
                if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &mm_data))
                {
                    printf("IOCTL[ION_IOC_CUSTOM] Config Buffer failed!\n");
                    return 0;
                }
            }

            {
                struct ion_sys_data sys_data;
                sys_data.sys_cmd = ION_SYS_GET_PHYS;
                sys_data.get_phys_param.handle = handle;
                if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
                {
                    printf("IOCTL[ION_IOC_CUSTOM] Get Phys failed!\n");
                    return 0;
                }
                printf("child Physical address = 0x%x, len = %d\n", sys_data.get_phys_param.phy_addr, sys_data.get_phys_param.len);
            }

            ion_munmap(ion_fd, pBuf, bufsize);
            ion_share_close(ion_fd, share_fd);
            ion_free(ion_fd, handle);
            close(ion_fd);

            printf("ion_test: child exit\n");
            exit(0);
        }

        sleep(2);
        printf("parent process goes...\n");
    }

    {
        struct ion_mm_data mm_data;
        mm_data.mm_cmd = ION_MM_CONFIG_BUFFER;
        mm_data.config_buffer_param.handle = handle;
        mm_data.config_buffer_param.eModuleID = 1;
        mm_data.config_buffer_param.security = 0;
        mm_data.config_buffer_param.coherent = 1;
        if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &mm_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Config Buffer failed!\n");
            return 0;
        }
    }

    {
		struct ion_mm_data set_debug_info_mm_data;
		set_debug_info_mm_data.mm_cmd = ION_MM_SET_DEBUG_INFO;
		set_debug_info_mm_data.buf_debug_info_param.handle = handle;

		strcpy(set_debug_info_mm_data.buf_debug_info_param.dbg_name, "mtk_ion_test");
		set_debug_info_mm_data.buf_debug_info_param.value1 = 1;
		set_debug_info_mm_data.buf_debug_info_param.value2 = 2;
		set_debug_info_mm_data.buf_debug_info_param.value3 = 3;
		set_debug_info_mm_data.buf_debug_info_param.value4 = 4;

		printf("IOCTL[ION_IOC_CUSTOM] set debug info dbg_name = %s, value1 = %d, value2 = %d, value3 = %d, value4 = %d.\n",
				set_debug_info_mm_data.buf_debug_info_param.dbg_name,
				set_debug_info_mm_data.buf_debug_info_param.value1,
				set_debug_info_mm_data.buf_debug_info_param.value2,
				set_debug_info_mm_data.buf_debug_info_param.value3,
				set_debug_info_mm_data.buf_debug_info_param.value4);

		if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &set_debug_info_mm_data)) {
			printf("IOCTL[ION_IOC_CUSTOM] set buffer debug info!\n");
			return 0;
		}

		struct ion_mm_data get_debug_info_mm_data;
		get_debug_info_mm_data.mm_cmd = ION_MM_GET_DEBUG_INFO;
		get_debug_info_mm_data.buf_debug_info_param.handle = handle;
		if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &get_debug_info_mm_data)) {
			printf("IOCTL[ION_IOC_CUSTOM] set buffer debug info!\n");
			return 0;
		}

		printf( "IOCTL[ION_IOC_CUSTOM] get debug info:"
				"dbg_name = %s, value1 = %d, value2 = %d, value3 = %d, value4 = %d.\n",
				get_debug_info_mm_data.buf_debug_info_param.dbg_name,
				get_debug_info_mm_data.buf_debug_info_param.value1,
				get_debug_info_mm_data.buf_debug_info_param.value2,
				get_debug_info_mm_data.buf_debug_info_param.value3,
				get_debug_info_mm_data.buf_debug_info_param.value4);
	}

    {
		struct ion_mm_data set_sf_info_mm_data;
		int i;
		set_sf_info_mm_data.mm_cmd = ION_MM_SET_SF_BUF_INFO;
		set_sf_info_mm_data.sf_buf_info_param.handle = handle;
		for (i = 0; i < ION_MM_SF_BUF_INFO_LEN; i++) {
			set_sf_info_mm_data.sf_buf_info_param.info[i] = i;

			printf("IOCTL[ION_IOC_CUSTOM] set sf info[%d] = %d.\n", i,
					set_sf_info_mm_data.sf_buf_info_param.info[i]);
		}

		if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &set_sf_info_mm_data)) {
			printf("IOCTL[ION_IOC_CUSTOM] set sf buf info!\n");
			return 0;
		}

		struct ion_mm_data get_sf_info_mm_data;
		get_sf_info_mm_data.mm_cmd = ION_MM_GET_SF_BUF_INFO;
		get_sf_info_mm_data.sf_buf_info_param.handle = handle;
		if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &get_sf_info_mm_data)) {
			printf("IOCTL[ION_IOC_CUSTOM] get sf buf info!\n");
			return 0;
		}

		for (i = 0; i < ION_MM_SF_BUF_INFO_LEN; i++) {
			printf("IOCTL[ION_IOC_CUSTOM] get sf info[%d] = %d.\n", i,
					get_sf_info_mm_data.sf_buf_info_param.info[i]);
		}
	}

    {
        struct ion_sys_data sys_data;
        sys_data.sys_cmd = ION_SYS_GET_PHYS;
        sys_data.get_phys_param.handle = handle;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Get Phys failed!\n");
            return 0;
        }
        printf("Physical address=0x%x, len=%d\n", sys_data.get_phys_param.phy_addr, sys_data.get_phys_param.len);
    }

    {
        struct ion_sys_data sys_data;
        sys_data.sys_cmd = ION_SYS_CACHE_SYNC;
        sys_data.cache_sync_param.handle = handle;

        sys_data.cache_sync_param.sync_type = ION_CACHE_CLEAN_BY_RANGE;
        printf("Clean by range.\n");
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
            return 0;
        }
        printf("Invalid by range.\n");
        sys_data.cache_sync_param.sync_type = ION_CACHE_INVALID_BY_RANGE;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
            return 0;
        }
        printf("Flush by range.\n");
        sys_data.cache_sync_param.sync_type = ION_CACHE_FLUSH_BY_RANGE;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
            return 0;
        }


        printf("Clean all.\n");
        sys_data.cache_sync_param.sync_type = ION_CACHE_CLEAN_ALL;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
            return 0;
        }

        printf("Flush all.\n");
        sys_data.cache_sync_param.sync_type = ION_CACHE_FLUSH_ALL;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
            return 0;
        }
    }

    ion_munmap(ion_fd, pBuf, bufsize);

    ion_share_close(ion_fd, share_fd);
    if (ion_free(ion_fd, handle))
    {
        printf("IOCTL[ION_IOC_FREE] failed!\n");
        return 0;
    }
    ion_close(ion_fd);
    printf("ION test done!\n");

    return 0;
}

int main(int argc, char **argv)
{
    ion_custom_ioctl_test();

    return 0;

}


