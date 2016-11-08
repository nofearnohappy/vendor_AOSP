#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <sys/file.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <utils/Log.h>
#include <cutils/log.h>
#include <sys/types.h>
#include <utils/String16.h>
#include <binder/BinderService.h>
#include <binder/Parcel.h>

#define FEATURE_MD_DIAG

#define ACTION "android.intent.action.THERMAL_WARNING"
#ifdef FEATURE_MD_DIAG
#define MDWarninig "android.intent.action.THERMAL_DIAG"
#endif
#define TYPE "type"

#ifdef FEATURE_MD_DIAG
/*
 * use "si_code" for Action identify
 */
enum {
/* TMD_Alert_ShutDown = 1, */
   TMD_Alert_ULdataBack = 2,
   TMD_Alert_NOULdata = 3
};
#endif

static int debug_on = 0;

#define TM_LOG_TAG "thermald"
#define TM_DBG_LOG(_fmt_, args...) \
    do { \
        if (1 == debug_on) { \
            LOG_PRI(ANDROID_LOG_INFO, TM_LOG_TAG, _fmt_, ##args); \
        } \
    } while(0)

#define TM_INFO_LOG(_fmt_, args...) \
    do { LOG_PRI(ANDROID_LOG_INFO, TM_LOG_TAG, _fmt_, ##args); } while(0)

#define PROCFS_MTK_CL_SD_PID "/proc/driver/thermal/clsd_pid"
#ifdef FEATURE_MD_DIAG
#define PROCFS_MTK_CLMUTT_TMD_PID "/proc/driver/thermal/clmutt_tmd_pid"
#endif

using namespace android;

bool sendBroadcastMessage(String16 action, int value)
{
    TM_INFO_LOG("sendBroadcastMessage(): Action: %s, Value: %d ", action.string(), value);
    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> am = sm->getService(String16("activity"));
    if (am != NULL) {
        Parcel data, reply;
        data.writeInterfaceToken(String16("android.app.IActivityManager"));
        data.writeStrongBinder(NULL);
        // intent begin
        data.writeString16(action); // action
        data.writeInt32(0); // URI data type
        data.writeString16(NULL, 0); // type
        data.writeInt32(0); // flags
        data.writeString16(NULL, 0); // package name
        data.writeString16(NULL, 0); // component name
        data.writeInt32(0); // source bound - size
        data.writeInt32(0); // categories - size
        data.writeInt32(0); // selector - size
        data.writeInt32(0); // clipData - size
        data.writeInt32(-2); // contentUserHint: -2 -> UserHandle.USER_CURRENT
        data.writeInt32(-1); // bundle extras length
        data.writeInt32(0x4C444E42); // 'B' 'N' 'D' 'L'
        int oldPos = data.dataPosition();
        data.writeInt32(1);  // size
        // data.writeInt32(0); // VAL_STRING, need to remove because of analyze common intent
        data.writeString16(String16(TYPE));
        data.writeInt32(1); // VAL_INTEGER
        data.writeInt32(value);
        int newPos = data.dataPosition();
        data.setDataPosition(oldPos - 8);
        data.writeInt32(newPos - oldPos); // refill bundle extras length
        data.setDataPosition(newPos);
        // intent end
        data.writeString16(NULL, 0); // resolvedType
        data.writeStrongBinder(NULL); // resultTo
        data.writeInt32(0); // resultCode
        data.writeString16(NULL, 0); // resultData
        data.writeInt32(-1); // resultExtras
        data.writeString16(NULL, 0); // permission
        data.writeInt32(0); // appOp
        data.writeInt32(-1); // option
        data.writeInt32(1); // serialized: != 0 -> ordered
        data.writeInt32(0); // sticky
        data.writeInt32(-2); // userId: -2 -> UserHandle.USER_CURRENT

        status_t ret = am->transact(IBinder::FIRST_CALL_TRANSACTION + 13, data, &reply); // BROADCAST_INTENT_TRANSACTION
        if (ret == NO_ERROR) {
            int exceptionCode = reply.readExceptionCode();
            if (exceptionCode) {
                TM_INFO_LOG("sendBroadcastMessage(%s) caught exception %d\n",
                        	action.string(), exceptionCode);
                return false;
            }
        } else {
            return false;
        }
    } else {
        TM_INFO_LOG("getService() couldn't find activity service!\n");
        return false;
    }
    return true;
}

static void signal_handler(int signo, siginfo_t *si, void *uc)
{
    switch(si->si_signo) {
    // Add more signo or code to expand thermald
      case SIGIO:
        if (1 == si->si_code) {
              //system("am start com.mediatek.thermalmanager/.ShutDownAlertDialogActivity");
              sendBroadcastMessage(String16(ACTION), 1);
              TM_INFO_LOG("thermal shutdown signal received, si_signo=%d, si_code=%d\n", si->si_signo, si->si_code);
        }
#ifdef FEATURE_MD_DIAG
        else if (TMD_Alert_ULdataBack == si->si_code) {
               sendBroadcastMessage(String16(MDWarninig), 0);
               TM_INFO_LOG("thermald signal received (TMD_Alert_ULdataBack), \
                           si_signo=%d, si_code=%d\n", si->si_signo, si->si_code);
              }
              else if (TMD_Alert_NOULdata == si->si_code) {
                 sendBroadcastMessage(String16(MDWarninig), 1);
                 TM_INFO_LOG("thermald signal received (TMD_Alert_NOULdata), \
                                            si_signo=%d, si_code=%d\n", si->si_signo, si->si_code);
              }
#endif
         break;
       default:
         TM_INFO_LOG("what!!!\n");
         break;
       }
}

int main(int argc, char *argv[])
{
    int fd = open(PROCFS_MTK_CL_SD_PID, O_RDWR);
#ifdef FEATURE_MD_DIAG
    int Mfd = open(PROCFS_MTK_CLMUTT_TMD_PID, O_RDWR);
#endif
    int pid = getpid();
    int ret = 0;
    char pid_string[32] = {0};

    struct sigaction act;

    TM_INFO_LOG("START+++++++++ %d", getpid());

    /* Create signal handler */
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_SIGINFO;
    //act.sa_handler = signal_handler;
    act.sa_sigaction = signal_handler;
    sigemptyset(&act.sa_mask);
    sigaction(SIGIO, &act, NULL);

    /* Write pid to procfs */
    sprintf(pid_string, "%d", pid);

    if(fd >=0 ){
      ret = write(fd, pid_string, sizeof(char) * strlen(pid_string));
       if (ret <= 0){
            TM_INFO_LOG("Fail to write %d to %s %x\n", pid, PROCFS_MTK_CL_SD_PID, ret);
          }
          else {
                 TM_INFO_LOG("Success to write %d to %s\n", pid, PROCFS_MTK_CL_SD_PID);
          }
          close(fd);
       }
#ifdef FEATURE_MD_DIAG
    if(Mfd >=0 ){
      ret = write(Mfd, pid_string, sizeof(char) * strlen(pid_string));
        if (ret <= 0) {
          TM_INFO_LOG("Fail to write %d to %s %x\n", pid, PROCFS_MTK_CLMUTT_TMD_PID, ret);
        }
        else {
          TM_INFO_LOG("Success to write %d to %s\n", pid, PROCFS_MTK_CLMUTT_TMD_PID);
        }
        close(Mfd);
    }
#endif

    TM_INFO_LOG("Enter infinite loop");

    while(1) {
     sleep(100);
    }

    TM_INFO_LOG("END-----------");

    return 0;
}
