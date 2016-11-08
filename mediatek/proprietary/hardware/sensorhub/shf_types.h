#ifndef SHF_TYPES_H_
#define SHF_TYPES_H_

#include <stdint.h>
#include <errno.h>
#include <sys/types.h>
#include <memory.h>
#include <utils/Errors.h>

//primitive types @{
#ifndef status_t
typedef signed int status_t;
#endif

#ifndef NULL
#define NULL 0
#endif

#ifndef NO_ERROR
#define NO_ERROR 0
#endif

#ifndef NO_INIT
#define NO_INIT -ENODEV
#endif
//@}

//TODO should enable it if align 4 bytes for uint64
//otherwise, comment it.
#define ALIGN_UINT64
#ifndef ALIGN_UINT64
typedef struct {
    uint32_t time_l;
    uint32_t time_h;
} timestamp_t;
#else
typedef uint64_t timestamp_t;
#endif

bool contain(const uint8_t* const list, uint8_t size, uint8_t find);
bool unique_add(uint8_t* const list, uint8_t max, uint8_t* size, uint8_t find);

uint64_t timestamp_get(timestamp_t* timestamp);
void timestamp_set(timestamp_t* timestamp, uint64_t value);
void timestamp_set_now(timestamp_t* timestamp);
uint64_t timestamp_get_now();

uint16_t convert_uint8_2_uint16(uint8_t* buf);
uint32_t convert_uint8_2_uint32(uint8_t* buf);
uint64_t convert_uint8_2_uint64(uint8_t* buf);

size_t save_uint16_2_uint8(uint16_t v, uint8_t* buf);
size_t save_uint32_2_uint8(uint32_t v, uint8_t* buf);
size_t save_uint64_2_uint8(uint64_t v, uint8_t* buf);
size_t save_float_2_uint8(float v, uint8_t* buf);

#endif /* SHF_TYPES_H_ */
