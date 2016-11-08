#define LOG_TAG "AudioSetParam"

#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <signal.h>
#include <binder/IPCThreadState.h>
#include <binder/MemoryBase.h>
#include <media/AudioSystem.h>
#include <media/mediaplayer.h>
#include <system/audio_policy.h>
#include <hardware/audio_policy.h>
#include <hardware_legacy/AudioPolicyInterface.h>
#include <hardware_legacy/AudioSystemLegacy.h>
#include <hardware/hardware.h>
#include <system/audio.h>

using namespace android;

static void usage(const char* name) {
    fprintf(stderr, "Usage: %s [-s parameter=value] [-g parameter]\n", name);
    fprintf(stderr, "    -s    set parameter\n");
    fprintf(stderr, "    -g    get parameter\n");
    fprintf(stderr, "If no options, it will run in the command line mode.'\n");
}

void getCommands()
{
    char cmd[1024];
    while (true)
    {
        printf("\nplease enter command, ex: 'GET_XXX_ENABLE', 'SET_XXX_ENABLE=0', 'SET_XXX_ENABLE=1', and '0' for exit\n\n");
        if (fgets(cmd, 1024, stdin) == NULL)
        {
            break;
        }

        if (cmd[0] == '0') // exit
        {
            break;
        }
        else if (strrchr(cmd, '=') != NULL)   // has '=', it's set function
        {
            status_t ret = AudioSystem::setParameters(0, String8(cmd));
            if ( ret == NO_ERROR )
            {
                fprintf(stderr, "set parameter %s succeed.\n", cmd);
            }
            else
            {
                fprintf(stderr, "set parameter %s error:%d!!\n", cmd, ret );
            }
        }
        else   // get function
        {
            printf("%s\n", AudioSystem::getParameters(0, String8(cmd)).string());
        }
    }
}

int main(int argc, char* argv[])
{
   ProcessState::self()->startThreadPool();
    sp<ProcessState> proc(ProcessState::self());
    
    const char* const progname = argv[0];

    if ( argc == 1 )
    {
        getCommands();
    }
    else
    {
        for (int ch; (ch = getopt(argc, argv, "s:g:")) != -1;) {
            switch (ch) {
            case 's':
                if (strrchr(optarg, '=') != NULL)   // has '=', it's set function
                {
                    status_t ret = AudioSystem::setParameters(0, String8(optarg));
                    if ( ret == NO_ERROR )
                    {
                        fprintf(stderr, "set parameter %s succeed.\n", optarg);
                    }
                    else
                    {
                        fprintf(stderr, "set parameter %s error:%d!!\n", optarg, ret );
                    }
                }
                else
                {
                    usage(progname);
                    goto EXIT;
                }
                break;
            case 'g':
                printf("%s\n", AudioSystem::getParameters(0, String8(optarg)).string());
                break;
            default:
                usage(progname);
                goto EXIT;
                break;
            }
        }
    }

EXIT:
    return 0;
}


