#include "icusb_util.h"
#include "icusb_storage.h"

static char *vdc_cmd="/system/bin/vdc" ;

static char *unmount_stor1_argv[]={"vdc", "volume", "unmount", "icusb1","force_and_revert", 0};

static char *unmount_stor2_argv[]={"vdc", "volume", "unmount", "icusb2","force_and_revert", 0};

int isMountpointMounted(const char *path)
{
    char device[256];
    char mount_path[256];
    char rest[256];
    FILE *fp;
    char line[1024];

    if (!(fp = fopen("/proc/mounts", "r"))) {
		icusb_print(PRINT_WARN, "[ICUSB][INFO] ====> no /proc/mounts .\n");
        return 0;
    }

    while(fgets(line, sizeof(line), fp)) {
        line[strlen(line)-1] = '\0';
		
        sscanf(line, "%255s %255s %255s\n", device, mount_path, rest);		
        if (!strcmp(mount_path, path)) {
            fclose(fp);
            return 1;
        }
    }

    fclose(fp);
    return 0;
}

int icusb_storage_do_unmomunt()
{

	int ret = 0;

	if(isMountpointMounted("/mnt/udisk/folder1"))
	{
		icusb_print(PRINT_WARN, "[ICUSB][INFO] ====> icusb_storage_do_unmomunt1 .\n");	
	   ret = icusb_do_exec(vdc_cmd, unmount_stor1_argv) ;
	}

	icusb_print(PRINT_WARN, "[ICUSB][INFO] unmomunt icusb_storage1, return %d \n", ret);
	if(isMountpointMounted("/mnt/udisk/folder2"))
	{	
		icusb_print(PRINT_WARN, "[ICUSB][INFO] ====> icusb_storage_do_unmomunt2 .\n");	
		if(ret == 0)
		ret = icusb_do_exec(vdc_cmd, unmount_stor2_argv) ;
	}
	icusb_print(PRINT_WARN, "[ICUSB][INFO] unmomunt icusb_storage2, return %d \n", ret);
	
	icusb_print(PRINT_WARN, "[ICUSB][INFO] <==== icusb_storage_do_unmomunt\n");
	
	return ret ; 

}

