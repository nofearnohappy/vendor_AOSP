#include "log_setky.h"

void plog_android(int level, char *format, ...)
{
    if (level >= 0 && level <= 5) {
#ifdef ANDROID_CHANGES
        static int levels[6] = {
            ANDROID_LOG_ERROR, ANDROID_LOG_WARN, ANDROID_LOG_INFO,
            ANDROID_LOG_INFO, ANDROID_LOG_DEBUG, ANDROID_LOG_VERBOSE
        };
        va_list ap;
        va_start(ap, format);
        __android_log_vprint(levels[level], "setkey", format, ap);
        va_end(ap);
#else
        static char *levels = "EWNIDV";
        fprintf(stderr, "%c: ", levels[level]);
        va_list ap;
        va_start(ap, format);
        vfprintf(stderr, format, ap);
        va_end(ap);
#endif
    }
}
