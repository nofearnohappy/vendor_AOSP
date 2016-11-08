#define LOG_NDEBUG 0
#define LOG_TAG "SHF_HAL"
#include <utils/Log.h>

#include "shf_debug.h"

#ifdef SHF_DEBUG_MODE
void shf_debug_print_bytes(void* buffer, size_t size) {
    if (!buffer) {
        ALOGW("print_bytes: no data!");
        return;
    }

    uint8_t* data = (uint8_t*)buffer;
    ALOGV("print_bytes: size=%d ", size);
    for (size_t i = 0; i < size; i++) {
        ALOGV("0x%.2x ", *(data + i));
    }
}
#endif
