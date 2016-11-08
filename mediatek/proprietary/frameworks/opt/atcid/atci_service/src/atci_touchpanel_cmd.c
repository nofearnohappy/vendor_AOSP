#include "atci_touchpanel_cmd.h"
#include "atci_service.h"
#include "atcid_util.h"
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#define DEV_TOUCH_PATH	"/sys/module/tpd_setting/parameters/tpd_fw_version"
#define TPD_GET_FWVER	(1)


int touchpanel_fwver_handler(char * cmdline, ATOP_t at_op, char *response){

	int fd;
	
	char com_date[10]={'\0'}, value[2];
	char log_info[100] = {'\0'};
	int ret = 0;
	fd = open(DEV_TOUCH_PATH, O_RDWR, 0);
    if(fd < 0) {
        return -1;
    }
	
	ret = read(fd, value, sizeof(value));
	sprintf(com_date, "%s", value);

	switch(at_op){
	        case AT_ACTION_OP:
        	case AT_READ_OP:
				sprintf(log_info, "\r\n%s\r\n", value);
				break;
	        case AT_TEST_OP:
				break;
		case AT_SET_OP:
			if(!strcmp(cmdline,com_date))
				sprintf(log_info,"\r\n\r\ OK\r\n\r\n");
			else
				sprintf(log_info,"\r\n\r\ ERROR\r\n\r\n");
			break;
	default:
			break;
	}
	sprintf(response,"\r\n%s \n\r\n", log_info);
	close(fd);
	return 0;
}
