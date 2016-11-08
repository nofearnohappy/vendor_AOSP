#ifndef SHF_KERNEL_H
#define SHF_KERNEL_H

#include "shf_define.h"
#include "shf_types.h"

typedef struct ipi_data {
    uint8_t data[SHF_IPI_PROTOCOL_BYTES];
    size_t size;
} ipi_data_t;

typedef struct ipi_buffer {
    size_t head;
    size_t tail;
    size_t size;//data count
    ipi_data_t* data;
} ipi_buffer_t;

#define SHF_IOW(num, dtype)     _IOW('S', num, dtype)
#define SHF_IOR(num, dtype)     _IOR('S', num, dtype)
#define SHF_IOWR(num, dtype)    _IOWR('S', num, dtype)
#define SHF_IO(num)             _IO('S', num)

#define SHF_IPI_SEND            SHF_IOW(1, ipi_data_t)
#define SHF_IPI_POLL            SHF_IOR(2, ipi_data_t)
#define SHF_GESTURE_ENABLE      SHF_IOW(3, int)

#endif//SHF_KERNEL_H