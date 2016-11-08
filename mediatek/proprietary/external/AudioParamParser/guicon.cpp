#include "AudioParamParserPriv.h"

#ifdef WIN32
#include <windows.h>
#include <io.h>
#include <iostream>
#include <fstream>
#include <fcntl.h>
#include <stdio.h>

#ifndef _USE_OLD_IOSTREAMS
using namespace std;
#endif

#else
#include <binder/ProcessState.h>
#include <media/AudioSystem.h>
#include "AudioCustParam.h"

using namespace android;
#endif


#ifndef WIN32

void initProcessState()
{
    static int processStateInited = 0;

    /* Init ProcessState, */
    if (!processStateInited)
    {
        ProcessState::self()->startThreadPool();
        sp<ProcessState> proc(ProcessState::self());
        processStateInited = 1;
    }
}

EXPORT int isCustXmlEnable(void)
{
    AUDIO_CUSTOM_AUDIO_FUNC_SWITCH_PARAM_STRUCT eParaAudioFuncSwitch;
    GetAudioFuncSwitchParamFromNV(&eParaAudioFuncSwitch);
    return (eParaAudioFuncSwitch.cust_xml_enable == 1);
}

#else   /* WIN32 */

// maximum mumber of lines the output console should have
static const WORD MAX_CONSOLE_LINES = 500;

/* For Tuning Tool show the debug message */
EXPORT void redirectIOToConsole()
{
    int hConHandle;
    long lStdHandle;

    CONSOLE_SCREEN_BUFFER_INFO coninfo;
    FILE *fp;

    // allocate a console for this app
    AllocConsole();

    // set the screen buffer to be big enough to let us scroll text
    GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE),
                               &coninfo);
    coninfo.dwSize.Y = MAX_CONSOLE_LINES;
    SetConsoleScreenBufferSize(GetStdHandle(STD_OUTPUT_HANDLE), coninfo.dwSize);

    // redirect unbuffered STDOUT to the console
    lStdHandle = (long)GetStdHandle(STD_OUTPUT_HANDLE);
    hConHandle = _open_osfhandle(lStdHandle, _O_TEXT);
    fp = _fdopen(hConHandle, "w");
    *stdout = *fp;
    setvbuf(stdout, NULL, _IONBF, 0);

    // redirect unbuffered STDIN to the console
    lStdHandle = (long)GetStdHandle(STD_INPUT_HANDLE);
    hConHandle = _open_osfhandle(lStdHandle, _O_TEXT);
    fp = _fdopen(hConHandle, "r");
    *stdin = *fp;
    setvbuf(stdin, NULL, _IONBF, 0);

    // redirect unbuffered STDERR to the console
    lStdHandle = (long)GetStdHandle(STD_ERROR_HANDLE);
    hConHandle = _open_osfhandle(lStdHandle, _O_TEXT);
    fp = _fdopen(hConHandle, "w");
    *stderr = *fp;
    setvbuf(stderr, NULL, _IONBF, 0);
    ios::sync_with_stdio();
}
#endif
