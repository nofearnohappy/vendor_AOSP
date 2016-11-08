#include "shf_types.h"

bool contain(const uint8_t* const list, uint8_t size, uint8_t find)
{
    for (uint8_t i = 0; i < size; i++) {
        if (*(list + i) == find) {
            return true;
        }
    }
    return false;
}

bool unique_add(uint8_t* const list, uint8_t max, uint8_t* size, uint8_t find)
{
    if (!contain(list, *size, find) && *size < max) {
        list[(*size)++] = find;
        return true;
    } else {
        return false;
    }
}

uint64_t timestamp_get(timestamp_t* timestamp)
{
    return convert_uint8_2_uint64((uint8_t*)timestamp);
    //return *(uint64_t*) timestamp;
//    uint64_t value = timestamp->time_h;
//    value <<= 32;
//    value += timestamp->time_l;
//    return value;
}

void timestamp_set(timestamp_t* timestamp, uint64_t value)
{
    save_uint64_2_uint8(value, (uint8_t*)timestamp);
    //*(uint64_t*) timestamp = value;
//    timestamp->time_l = value;
//    timestamp->time_h = value >> 32;
}

uint16_t convert_uint8_2_uint16(uint8_t* buf)
{
    return (((uint16_t) buf[0]) & 0x00FF) + ((((uint16_t) buf[1]) & 0x00FF) << 8);
}

uint32_t convert_uint8_2_uint32(uint8_t* buf)
{
    return (((uint32_t) buf[0])  & 0x000000FF) +
           ((((uint32_t) buf[1]) & 0x000000FF) << 8) +
           ((((uint32_t) buf[2]) & 0x000000FF) << 16) +
           ((((uint32_t) buf[3]) & 0x000000FF) << 24);
}

uint64_t convert_uint8_2_uint64(uint8_t* buf)
{
    return (((uint64_t) buf[0]) & 0x00000000000000FF) +
           ((((uint64_t) buf[1]) & 0x00000000000000FF) << 8) +
           ((((uint64_t) buf[2]) & 0x00000000000000FF) << 16) +
           ((((uint64_t) buf[3]) & 0x00000000000000FF) << 24) +
           ((((uint64_t) buf[4]) & 0x00000000000000FF) << 32) +
           ((((uint64_t) buf[5]) & 0x00000000000000FF) << 40) +
           ((((uint64_t) buf[6]) & 0x00000000000000FF) << 48) +
           ((((uint64_t) buf[7]) & 0x00000000000000FF) << 56);
}

size_t save_uint16_2_uint8(uint16_t v, uint8_t* buf)
{
    buf[0] = ((v >> 0) & 0x00FF);
    buf[1] = ((v >> 8) & 0x00FF);
    return 2;
}

size_t save_uint32_2_uint8(uint32_t v, uint8_t* buf)
{
    buf[0] = ((v >> 0) & 0x000000FF);
    buf[1] = ((v >> 8) & 0x000000FF);
    buf[2] = ((v >> 16) & 0x000000FF);
    buf[3] = ((v >> 24) & 0x000000FF);
    return 4;
}

size_t save_uint64_2_uint8(uint64_t v, uint8_t* buf)
{
    buf[0] = ((v >> 0) & 0x00000000000000FF);
    buf[1] = ((v >> 8) & 0x00000000000000FF);
    buf[2] = ((v >> 16) & 0x00000000000000FF);
    buf[3] = ((v >> 24) & 0x00000000000000FF);
    buf[4] = ((v >> 32) & 0x00000000000000FF);
    buf[5] = ((v >> 40) & 0x00000000000000FF);
    buf[6] = ((v >> 48) & 0x00000000000000FF);
    buf[7] = ((v >> 56) & 0x00000000000000FF);
    return 8;
}

size_t save_float_2_uint8(float v, uint8_t* buf)
{
    uint32_t tv = *((uint32_t*)(&v));
    buf[0] = ((tv >> 0) & 0x000000FF);
    buf[1] = ((tv >> 8) & 0x000000FF);
    buf[2] = ((tv >> 16) & 0x000000FF);
    buf[3] = ((tv >> 24) & 0x000000FF);
    return 4;
}
