#define LOG_NDEBUG 0
#define LOG_TAG "SHF_HAL"
#include <utils/Log.h>

#include "shf_communicator.h"
#include "shf_debug.h"
#include "shf_kernel.h"

#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>

static uint16_t checksum_get(void* data, size_t size)
{
    uint16_t chksum = 0;
    uint8_t* p = (uint8_t*)data;
    for (size_t i = 0; i < size; i++, p++) {
        chksum += *p;
    }
    return chksum;
}

static bool checksum_check(void* data, size_t size, uint16_t chksum)
{
    return chksum == checksum_get(data, size);
}

#define BUFFER_STATE_IDLE           (0x00)
#define BUFFER_STATE_STARTED        (0x01)
#define BUFFER_STATE_COMPLETED      (0x02)

typedef struct {
    uint8_t* buffer; //buffer address
    uint8_t offset; //current filled offset
    uint8_t claim; //claim size in data
    uint8_t size; //allocated buffer size
    uint8_t state; //buffer data filled state
} protocol_buffer_t;

#define ESC (0xAA)
#define BOM (0xF0)
#define EOM (0x0F)
#define HEADER_SIZE (0x08)

//TODO define here to reduce stack size.
//But this will add data size and assume this function is called in the same thread.
uint8_t buffer[SHF_PROTOCOL_SEND_BUFFER_BYTES];

typedef status_t (*protocol_send_handler)(void* data, size_t size);
static status_t protocol_send_message(void* data, size_t size, const size_t partition,
        protocol_send_handler handler)
{
#ifdef SHF_DEBUG_MODE
    shf_debug_print_bytes(data, size);
#endif
    const size_t total = size + HEADER_SIZE;
    //Note: array size is required to be constant value in MD32 compiler.
    //The best buffer size should be total.
    //uint8_t buffer[SHF_PROTOCOL_SEND_BUFFER_BYTES];    // = {0xAA, 0xF0, size, 0x00};
    buffer[0] = ESC;
    buffer[1] = BOM;
    buffer[2] = total - 6;
    buffer[3] = 0x00;
    memcpy(buffer + 4, data, size);
    //check Size_L + Size_H + data
    save_uint16_2_uint8(checksum_get(buffer + 2, total - 6), buffer + total - 4);

    buffer[total - 2] = ESC;
    buffer[total - 1] = EOM;
    size_t count = (total%partition == 0) ? total/partition : total/partition + 1;
    size_t index = 0;
    while (index < count) {
        size_t length = (total - partition * index) >= partition ? partition : (total % partition);
        status_t result = handler(buffer + index * partition, length);
        if (result != NO_ERROR) {
            ALOGW("protocol_send_message: fail! index=%d, size=%d", index, size);
            return result;
        }
        ALOGV("protocol_send_message: succeed. index=%d, size=%d", index, size);
        index++;
    }
    return NO_ERROR;
}

static void protocol_receive_message(protocol_buffer_t* buf, uint8_t* rec_buf, uint8_t rec_size,
                                     communicator_handler_t handler)
{
    ALOGV("protocol_receive_message>>>rec_size=%d, esc=%d, bom=%d, size=%d\n", 
        rec_size, *rec_buf, *(rec_buf + 1), *(rec_buf + 2));
#ifdef SHF_DEBUG_MODE
    shf_debug_print_bytes(rec_buf, rec_size);
#endif
    if (rec_size >= 3 && *rec_buf == ESC && *(rec_buf + 1) == BOM
            && *(rec_buf + 2) >= 5 // 2 for size, 1 for msgid, 1 for sessionId, 1 for other info
            && *(rec_buf + 2) <= buf->size) { //head
        if ((buf->offset != (buf->claim + 6)) && (buf->state != BUFFER_STATE_IDLE)) {
            ALOGW("protocol_receive_message: msg is not completed! offset=%d, claim=%d", buf->offset, buf->claim);
        }

        if (buf->state == BUFFER_STATE_COMPLETED) {
        	//last cached data hasn't been processed, so lose current message.
            ALOGW("protocol_receive_message: buffer hasn't been processed. state=%d", buf->state);
            return;
        }
        buf->state = BUFFER_STATE_STARTED;
        buf->claim = *(rec_buf + 2);
        buf->offset = rec_size;
        memcpy(buf->buffer, rec_buf, rec_size); //can be avoided if buf_length == buf_offset
    } else if (buf->offset && buf->offset + rec_size <= buf->claim + 6) { //middle or tail, claim + 6 = all size
        if (buf->state != BUFFER_STATE_STARTED) {
            ALOGW("protocol_receive_message: middle content but state=%d, offset=%d", buf->state, buf->offset);
            return;
        }
        //append received message to last buffer
        memcpy(buf->buffer + buf->offset, rec_buf, rec_size);
        buf->offset += rec_size;
    }
    if (buf->state == BUFFER_STATE_STARTED && buf->offset && (buf->offset == buf->claim + 6)) { //enough
        if (*(buf->buffer + buf->offset - 2) == ESC && *(buf->buffer + buf->offset - 1) == EOM) { //tail
            //checksum only check size + user data
            uint16_t chksum = convert_uint8_2_uint16(buf->buffer + 2 + buf->claim);
            if (checksum_check(buf->buffer + 2, buf->claim, chksum)) {
                //Here we process the buffer imediately.
                buf->state = BUFFER_STATE_COMPLETED;
                handler(buf->buffer + 4, buf->claim - 2);
                buf->state = BUFFER_STATE_IDLE;
            } else {
                ALOGW("protocol_receive_message: checksum is wrong!");
            }
        } else {
            ALOGW("protocol_receive_message: cannot find tail!");
        }
    }
    ALOGV("protocol_receive_message<<<state=%d\n", buf->state);
}

//IPI only transfers 48 bytes one time,
//so buffer it until all data are sent completely.
//Assume:
//1. max buffer is 48 * 2 = 96 bytes
//2. all data are sent in sequence
//3. header is at buffer address 0
uint8_t shf_in_buf[SHF_AP_BUFFER_BYTES]; //for merging in data
protocol_buffer_t shf_protocol_buffer = {shf_in_buf, 0, 0, SHF_AP_BUFFER_BYTES, 0};
int shf_fd = -1;

static status_t shf_protocol_send_handler(void* data, size_t size)
{
    if (shf_fd >= 0) {
        ipi_data_t temp_data;
        memset(&temp_data, 0, sizeof(ipi_data_t));
        memcpy(temp_data.data, data, size);
        temp_data.size = size;
        status_t ret = ioctl(shf_fd, SHF_IPI_SEND, &temp_data);
        if (ret) {
            ALOGW("shf_protocol_send_handler: failed. size=%d, error=%s", size, strerror(errno));
        }
        return ret;
    } else {
        ALOGW("shf_protocol_send_handler: device was not inited!");
        return NO_INIT;
    }
    return NO_INIT;
}

status_t shf_communicator_send_message(shf_device_t device, void* data, size_t size)
{
    switch (device) {
    case SHF_DEVICE_SCP:
        return protocol_send_message(data, size, SHF_IPI_PROTOCOL_BYTES, shf_protocol_send_handler);
    default:
        ALOGW("shf_communicator_send_message: unsupported device %u!", device);
        return SHF_STATUS_ERROR;
    }
}

status_t shf_communicator_receive_message(shf_device_t device, communicator_handler_t handler)
{
    switch (device) {
    case SHF_DEVICE_SCP:
        if (shf_fd >= 0) {
            ipi_data_t temp_data;
            memset(&temp_data, 0, sizeof(ipi_data_t));
            ALOGV("shf_communicator_receive_message: polling...");
            status_t ret = ioctl(shf_fd, SHF_IPI_POLL, &temp_data);
            if (ret) {         
                ALOGW("shf_communicator_receive_message: poll failed! error=%s", strerror(errno));
                return ret;
            }
            protocol_receive_message(&shf_protocol_buffer, temp_data.data, temp_data.size, handler);
        } else {
            //ALOGW("shf_communicator_receive_message: device was not inited!");
            return NO_INIT;
        }
        break;
    default:
        ALOGW("shf_communicator_receive_message: unsupported device %d!", device);
        break;
    }
    return NO_ERROR;
}

status_t shf_communicator_enable_gesture(bool enable) {
    if (shf_fd >= 0) {
        status_t ret = ioctl(shf_fd, SHF_GESTURE_ENABLE, enable);
        if (ret) {
            ALOGW("shf_communicator_enable_gesture: failed! error=%s", strerror(errno));
        }
        return ret;
    } else {
        ALOGW("shf_communicator_enable_gesture: failed due to no init!");
        return NO_INIT;
    }
}

status_t shf_communicator_init()
{
    shf_fd = open(SENSORSHUB_DEVICE, O_RDONLY);
    if (shf_fd < 0) {
        ALOGW("shf_communicator_init: failed! error=%s", strerror(errno));
        return NO_INIT;
    }
    return NO_ERROR;
}

void shf_communicator_release()
{
    if (shf_fd >= 0) {
        close(shf_fd);
    }
}
