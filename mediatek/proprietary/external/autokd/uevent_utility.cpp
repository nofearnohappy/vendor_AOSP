#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <linux/rtc.h>
#include <sys/mman.h>
#include <utils/Log.h>
#include <string.h>
#include <poll.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <pthread.h>
#include "autok.h"
#define UEVENT_MSG_LEN  1024
struct uevent {
    char subsystem[128];
    int host_id;
    char what[128];
};

static int open_uevent_socket(void)
{
    struct sockaddr_nl addr;
    int sz = 2*UEVENT_MSG_LEN; // XXX larger? udev uses 
    int on = 1;
    int s;

    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_pid = getpid();
    addr.nl_groups = 0xffffffff;

    s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if(s < 0)
        return -1;

    setsockopt(s, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz));
    setsockopt(s, SOL_SOCKET, SO_PASSCRED, &on, sizeof(on));

    if(bind(s, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        close(s);
        return -1;
    }

    return s;
}

static int parse_event(char *msg, struct uevent *uevent)
{
    const char subsystem_key[] = "SUBSYSTEM=";
    const char host_key[] = "HOST=";
    const char what_key[] = "WHAT=";
    const char query_key[] = "mmc_host";
    
    uevent->host_id = -1;
    /* currently ignoring SEQNUM */
    while(*msg) {
        if(!strncmp(msg, subsystem_key, strlen(subsystem_key))) {
            msg += strlen(subsystem_key);
            memset(uevent->subsystem, 0, sizeof(uevent->subsystem)/sizeof(uevent->subsystem[0])); 
            strcpy(uevent->subsystem, msg);
        } else if(!strncmp(msg, host_key, strlen(host_key))) {
            msg += strlen(host_key);
            uevent->host_id = atoi(msg);    
        } else if(!strncmp(msg, what_key, strlen(what_key))) {
            msg += strlen(what_key);
            memset(uevent->what, 0, sizeof(uevent->what)/sizeof(uevent->what[0]));
            strcpy(uevent->what, msg);    
        }
            /* advance to after the next \0 */
        while(*msg++)
            ;
    }
    
    if(!strncmp(uevent->subsystem, query_key, strlen(query_key)))
        return 0;
    else
        return -1;
    
}

static void* handle_device_fd(void *file_desc, struct uevent *uevent)
{   
    int fd = *((int*)file_desc);
    char msg[UEVENT_MSG_LEN+2*sizeof(char)];
    char cred_msg[CMSG_SPACE(sizeof(struct ucred))];
    
    memset(msg, 0, UEVENT_MSG_LEN+2*sizeof(char));
    memset(cred_msg, 0, CMSG_SPACE(sizeof(struct ucred)));
    for(;;) {
        struct iovec iov = {msg, sizeof(msg)};
        struct sockaddr_nl snl;
        struct msghdr hdr = {&snl, sizeof(snl), &iov, 1, cred_msg, sizeof(cred_msg), 0};

        ssize_t n = recvmsg(fd, &hdr, 0);
        if (n <= 0) {
            break;
        }
        if ((snl.nl_groups != 1) || (snl.nl_pid != 0)) {
            /* ignoring non-kernel netlink multicast message */
            continue;
        }
        struct cmsghdr * cmsg = CMSG_FIRSTHDR(&hdr);
        if (cmsg == NULL || cmsg->cmsg_type != SCM_CREDENTIALS) {
            /* no sender credentials received, ignore message */
            continue;
        }
        struct ucred * cred = (struct ucred *)CMSG_DATA(cmsg);
        if (cred->uid != 0) {
            /* message from non-root user, ignore */
            continue;
        }
        if(n >= UEVENT_MSG_LEN) /* overflow -- discard */
            continue;

        msg[n] = '\0';
        msg[n+1] = '\0';
        
        if(!parse_event(msg, uevent)){
            return NULL;
        }
    }
    return NULL;
}

void *force_re_k_thread(void *para)
{
    char data_buf[BUF_LEN]="";
    int data_count = 0;
    
    // Do not run too fast and blocking the next autok uevent    
    sleep(1);
    // Remove autok calibrated data
    system("rm -rf /data/autok_*");
    system("rm -rf /data/nvram/APCFG/APRDCL/SDIO");
    
    // Reset SDIO Device
    data_count = snprintf(data_buf, BUF_LEN, "%s", "1");
    set_node_data(PARAM_COUNT_DEVNODE, data_buf, data_count);
    
    // Re-K
    //set_ready(3);
    
    return ((void *)0);
}


int wait_sdio_uevent(int *id, const char *keyword)
{
    static int fd = 0;
    pthread_t uevent_tid;
    struct uevent event;
    int ret = 0;
    if(fd <= 0){
        fd = open_uevent_socket();
        if (fd < 0) {
            LOGE("Open socket error!\n");
            return -1;
        }
    }
    
    while(1){
        handle_device_fd(&fd, &event);
        if(strncmp(event.what, keyword, strlen(keyword))==0){
            LOGI("test_event { %s, %d, %s }\n", event.subsystem, event.host_id, event.what);
            *id = event.host_id;
            break;
        }
        // For debug use        
        if(strncmp(event.what, "bus_error", strlen("bus_error"))==0){
            LOGI("test_event { %s, %d, %s }\n", event.subsystem, event.host_id, event.what);
            ret = pthread_create(&uevent_tid, NULL, force_re_k_thread, NULL);
            if(ret != 0){
                LOGE("can't create thread: %d\n",ret);
            }
        }
    }

    return 0;
}

