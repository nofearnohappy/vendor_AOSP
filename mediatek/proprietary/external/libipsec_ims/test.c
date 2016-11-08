#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <linux/capability.h>
#include <pthread.h>
#include <private/android_filesystem_config.h>
#include <sys/prctl.h>
#include <fcntl.h>
#include <dirent.h>
#include "src/setkey/log_setky.h"
#include "src/setkey_fileio/setkey_fileio.h"

/* Set UID to inet w/ CAP_NET_RAW, CAP_NET_ADMIN and CAP_NET_BIND_SERVICE
 * (Android's init.rc does not yet support applying linux capabilities) */
void setkey_set_aid_and_cap() {
 /* We ignore errors here since errors are normal if we
  * are already running as non-root.
  */
       /* gid_t groups[] = { AID_ADB, AID_LOG, AID_INPUT, AID_INET, AID_GRAPHICS,
                       AID_NET_BT, AID_SYSTEM, AID_SDCARD_RW, AID_MOUNT,
                           AID_NET_BW_STATS, AID_NET_RAW };
        setgroups(sizeof(groups)/sizeof(groups[0]), groups);*/
	prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0);
        if(setgid(AID_SYSTEM)==0)
            plog_android(LLV_WARNING,"setgid SYSTEM successfully!");
        else
            plog_android(LLV_WARNING,"setgid SYSTEM failed!");
        if(setuid(AID_SYSTEM)==0)
            plog_android(LLV_WARNING,"setuid SYSTEM successfully!");
        else
            plog_android(LLV_WARNING,"setuid SYSTEM failed!");
	struct __user_cap_header_struct header;
	struct __user_cap_data_struct cap;
	header.version = _LINUX_CAPABILITY_VERSION;
	header.pid = 0;
	cap.effective = cap.permitted = 1 << CAP_NET_RAW |
					1 << CAP_NET_ADMIN |
					1 << CAP_NET_BIND_SERVICE |
                                        1 << CAP_SYS_BOOT;
	cap.inheritable = 0;
	capset(&header, &cap);
}
void test_capability(void)
{
        plog_android(LLV_WARNING,"test Warning: gid:%d,uid:%d,pid:%d !\n",getgid(),getuid(),getpid());
	struct __user_cap_header_struct header;
	struct __user_cap_data_struct cap;
	header.version = _LINUX_CAPABILITY_VERSION;
	header.pid = getpid();
	capget(&header, &cap);
        plog_android(LLV_WARNING, "test Warning: permitted:%x,cap.effective:%x !\n",cap.permitted,cap.effective);
}
void main(argc, argv)
	int argc;
	char **argv;
{
pthread_t 	   mThread_listenevent;
    flush_SA_SP_exist();
setkey_set_aid_and_cap();
pthread_create(&mThread_listenevent, NULL, test_capability, NULL);
     
if(atoi(argv[1]))
{
     /*set SA SP*/
     /*2002:daf9:2f6d:20:e828:9879:354e:6de-->2002:daf9:2f6d:20:208:22ff:feae:d3fb,esp,transport mode,spi:0x12,des-cbc,hmac-sha1*/
     setkey_setSA("2001:0:0:1::1","2001:0:0:1::2","esp","0x3fd787a5","transport", 
                  "3des-cbc","0xe150d1484d84a6c7e129f90721811af8e150d1484d84a6c6","hmac-sha1","0x50d1484d84a6c7e129f90721811af8e100000001",0);
     setkey_SP("2001:0:0:1::1","2001:0:0:1::2",PROTOCOL_ICMP,"any","any","esp","transport", "in",0);
     /*2002:daf9:2f6d:20:208:22ff:feae:d3fb-->2002:daf9:2f6d:20:e828:9879:354e:6de,eso,transport mode,spi:0x34,3des-cbc,hmac-sha1*/             
     setkey_setSA("2001:0:0:1::2","2001:0:0:1::1","esp","0x39982bf","transport", 
                  "3des-cbc","0xe150d1484d84a6c7e129f90721811af8e150d1484d84a6c6",
                  "hmac-sha1","0x50d1484d84a6c7e129f90721811af8e100000001",0);
     /*2002:daf9:2f6d:20:e828:9879:354e:6de[1234]-->2002:daf9:2f6d:20:208:22ff:feae:d3fb[any],only tcp,esp,transport mode,output*/             
     
     /*2002:daf9:2f6d:20:208:22ff:feae:d3fb[any]-->2002:daf9:2f6d:20:e828:9879:354e:6de[1234],only tcp,esp,transport mode,input*/      
     setkey_SP("2001:0:0:1::2","2001:0:0:1::1",PROTOCOL_ICMP,"any","any","esp","transport", "out",0);
     /*2002:daf9:2f6d:20:e828:9879:354e:6de-->2002:daf9:2f6d:20:208:22ff:feae:d3fb,esp,transport mode,spi:0x12,des-cbc,hmac-sha1*/
     setkey_setSA("2001:0:0:1::1","2001:0:0:1::2","esp","0x3fd787a4","transport", 
                  "3des-cbc","0xe150d1484d84a6c7e129f90721811af8e150d1484d84a6c7","hmac-sha1","0x50d1484d84a6c7e129f90721811af8e100000000",0);
     setkey_SP("2001:0:0:1::1","2001:0:0:1::2",PROTOCOL_ICMP,"any","any","esp","transport", "in",0);
     /*2002:daf9:2f6d:20:208:22ff:feae:d3fb-->2002:daf9:2f6d:20:e828:9879:354e:6de,eso,transport mode,spi:0x34,3des-cbc,hmac-sha1*/             
     setkey_setSA("2001:0:0:1::2","2001:0:0:1::1","esp","0x39982bc","transport", 
                  "3des-cbc","0xe150d1484d84a6c7e129f90721811af8e150d1484d84a6c7",
                  "hmac-sha1","0x50d1484d84a6c7e129f90721811af8e100000000",0);
     /*2002:daf9:2f6d:20:e828:9879:354e:6de[1234]-->2002:daf9:2f6d:20:208:22ff:feae:d3fb[any],only tcp,esp,transport mode,output*/             
     
     /*2002:daf9:2f6d:20:208:22ff:feae:d3fb[any]-->2002:daf9:2f6d:20:e828:9879:354e:6de[1234],only tcp,esp,transport mode,input*/      
     setkey_SP("2001:0:0:1::2","2001:0:0:1::1",PROTOCOL_ICMP,"any","any","esp","transport", "out",0);

     setkey_setSA("192.168.20.112","192.168.20.123","esp","0x12","transport", "des-cbc","0x84cc855d68922075","hmac-sha1","0xb48408f4655000f588a1a22cc14697d1a4d259cd",0);
     setkey_setSA("192.168.20.123","192.168.20.112","esp","0x34","transport", "3des-cbc","0x6df8e9fc37255c9ba467be460187abc29e20e808f17591aa","hmac-sha1","0x760c7721c03cf906c7fd70d0c9b9afd5785a1548",1);
     setkey_SP("192.168.20.112","192.168.20.123",PROTOCOL_TCP,"3456","any","esp","transport", "in",0);
     setkey_SP("192.168.20.123","192.168.20.112",PROTOCOL_TCP,"any","3456","esp","transport", "out",1);
     setkey_setSA("192.168.20.125","192.168.20.123","esp","0x56","transport", "des-cbc","0x84cc855d68922075","hmac-sha1","0xb48408f4655000f588a1a22cc14697d1a4d259cd",0);
     setkey_setSA("192.168.20.123","192.168.20.125","esp","0x78","transport", "3des-cbc","0x6df8e9fc37255c9ba467be460187abc29e20e808f17591aa","hmac-sha1","0x760c7721c03cf906c7fd70d0c9b9afd5785a1548",2);
     setkey_SP("192.168.20.125","192.168.20.123",PROTOCOL_TCP,"3455","any","esp","transport", "in",0);
     setkey_SP("192.168.20.123","192.168.20.125",PROTOCOL_TCP,"any","3455","esp","transport", "out",2);

     setkey_setSA("192.168.20.125","192.168.20.123","esp","0x90","transport", "des-cbc","0x84cc855d68922075","hmac-sha1","0xb48408f4655000f588a1a22cc14697d1a4d259cd",0);
     setkey_setSA("192.168.20.123","192.168.20.125","esp","0x14","transport", "3des-cbc","0x6df8e9fc37255c9ba467be460187abc29e20e808f17591aa","hmac-sha1","0x760c7721c03cf906c7fd70d0c9b9afd5785a1548",3);
     setkey_SP("192.168.20.125","192.168.20.123",PROTOCOL_TCP,"3454","any","esp","transport", "in",0);
     setkey_SP("192.168.20.123","192.168.20.125",PROTOCOL_TCP,"any","3454","esp","transport", "out",3);

     dump_setkeySA();
     dump_setkeySP();
     //setkey_deleteSA("2002:daf9:2f6d:20:e828:9879:354e:6de","2002:daf9:2f6d:20:208:22ff:feae:d3fb","esp","0x12");

     //setkey_deleteSA("2002:daf9:2f6d:20:208:22ff:feae:d3fb","2002:daf9:2f6d:20:e828:9879:354e:6de","esp","0x34");
     //dump_setkeySA();
    // dump_setkeySP();
     //setkey_deleteSP("192.168.20.172","192.168.20.165","tcp","1234","any","out");
    // dump_setkeySA();
    // dump_setkeySP();
     //setkey_deleteSP("192.168.20.165","192.168.20.172","tcp","any","1234","in");
     //dump_setkeySA();
    // dump_setkeySP();
}
pthread_exit(&mThread_listenevent);

}
