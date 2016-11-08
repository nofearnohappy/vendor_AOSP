#include "shf_configurator.h"
#include "shf_communicator.h"
#include "shf_define.h"
#include "shf_hal.h"

#include <utils/Log.h>
#include <utils/Timers.h>
#include <errno.h>
#include <pthread.h>
#include <sys/time.h>
#include <time.h>

pthread_cond_t m_event_cond;
pthread_mutex_t m_event_mutex;

void event_init()
{
    if (pthread_cond_init(&m_event_cond, NULL)) {
        ALOGE("pthread_cond_init error %s!", strerror(errno));
    }
	if (pthread_mutex_init(&m_event_mutex, NULL)) {
        ALOGE("pthread_mutex_init error %s!", strerror(errno));
    }
}

void event_destroy()
{
    if (pthread_cond_destroy(&m_event_cond)) {
        ALOGE("pthread_cond_destroy errno %d, error %s!", errno, strerror(errno));
    }
	if (pthread_mutex_destroy(&m_event_mutex)) {
        ALOGE("pthread_mutex_destroy error %d, error %s!", errno, strerror(errno));
    }
}

void event_lock()
{
    pthread_mutex_lock(&m_event_mutex);
}

void event_unlock()
{
    pthread_mutex_unlock(&m_event_mutex);
}

int event_wait_timeout(uint64_t reltime)
{
    struct timespec ts;
#if defined(HAVE_POSIX_CLOCKS)
    clock_gettime(CLOCK_REALTIME, &ts);
#else // HAVE_POSIX_CLOCKS
    // we don't support the clocks here.
    struct timeval t;
    gettimeofday(&t, NULL);
    ts.tv_sec = t.tv_sec;
    ts.tv_nsec= t.tv_usec*1000;
#endif // HAVE_POSIX_CLOCKS
    ts.tv_sec += reltime/1000000000;
    ts.tv_nsec+= reltime%1000000000;
    if (ts.tv_nsec >= 1000000000) {
        ts.tv_nsec -= 1000000000;
        ts.tv_sec  += 1;
    }

    status_t err = pthread_cond_timedwait(&m_event_cond, &m_event_mutex, &ts);
    if(err) {
        ALOGE("%s: wait errno %d, error %s", __func__, err, strerror(errno));
    }
    return err;
}

void event_notify()
{
    if (pthread_cond_signal(&m_event_cond)) {
        ALOGE("%s: signal errno %d, error %s", __func__, errno, strerror(errno));
    }
}

static uint8_t waiter_data[SHF_AP_BUFFER_BYTES];
static size_t waiter_size;
static uint8_t waiter_sid;

uint8_t session_id_get(uint8_t* buffer) {
    return buffer[1];
}

uint8_t sender_sid;//session id for sender
uint8_t session_id_create() {
    return sender_sid++;
}

bool build_trigger_data(sensor_trigger_data_t* trigger_data, uint8_t* data, size_t size)
{
    //data structure is set up by wrap_condition method of shf_action.c:
    //SHF_AP_TRIGGER chipno cid aid1 aid2 aid3 aid4 status dataIndexCount data1[index type currnet_value last_value] ...
    if (size < TRIGGER_SLOT_RESERVED) {
        ALOGE("build_trigger_data: fail! size=%d < required=%d!", size, TRIGGER_SLOT_RESERVED);	
        return false;
    }
    memset(trigger_data, 0, sizeof(sensor_trigger_data_t));
    memcpy(trigger_data, data + 2, (1 + SHF_CONDITION_ACTION_SIZE));
    size_t item_size = data[TRIGGER_SLOT_SIZE];
    size_t used = TRIGGER_SLOT_RESERVED;
    for (size_t i = 0; i < item_size; i++) {
        if (size < (used + 2)) {
            ALOGE("build_trigger_data: fail! size=%d < required=%d!", size, (used + 2));			
            return false;
        }
        trigger_data_t* tdata = trigger_data->data + i;
        tdata->index = data[used];
        tdata->type = data[used + 1];
        used += 2;
        switch(tdata->type) {
        case SHF_DATA_TYPE_UINT32: {
            if (size < (used + 8)) {
                ALOGE("build_trigger_data: fail! size=%d < required=%d!", size, (used + 8));
                return false;
            }
            tdata->value.duint32 = convert_uint8_2_uint32(data + used);
            tdata->last.duint32 = convert_uint8_2_uint32(data + used + 4);
            used += 8;
        }
            break;
        case SHF_DATA_TYPE_UINT64: {
            if (size < (used + 16)) {
                ALOGE("build_trigger_data: fail! size=%d < required=%d!", size, (used + 16));
                return false;
            }
            tdata->value.dtime = convert_uint8_2_uint64(data + used);
            tdata->last.dtime = convert_uint8_2_uint64(data + used + 8);
            used += 16;
        }
            break;
        default:
			ALOGW("build_trigger_data: invalid datatype %d!", tdata->type);
            break;
        }
    }
    ALOGV("build_trigger_data: succeed. count=%d, used=%d, size=%d", item_size, used, size);	
    return true;
}

#define IPI_PICK_TIMEOUT_MAX_NS             (2000000000) //2000ms
#define IPI_PICK_TIMEOUT_INTERVAL_NS        (500000000)  //500ms

bool pick_buffer(uint8_t msgid, uint8_t sid)//TODO: msgid is not used. Is it needed?
{
    nsecs_t last = systemTime(SYSTEM_TIME_MONOTONIC);
    nsecs_t now = last;
    while(now - last < IPI_PICK_TIMEOUT_MAX_NS) {
        event_lock();
        if (session_id_get(waiter_data) == sid) {
            event_unlock();
            return true;
        }
        event_wait_timeout(IPI_PICK_TIMEOUT_INTERVAL_NS);
        event_unlock();
        now = systemTime(SYSTEM_TIME_MONOTONIC);
    }
    return false;
}

shf_action_index_t shf_condition_action_add(shf_condition_index_t condition_index, shf_action_id_t action)
{
    uint8_t sid = session_id_create();
    uint8_t buffer[] = {SHF_AP_CONDITION_ACTION_ADD, sid, condition_index, 0x01/*actionSize*/, action};
    status_t status = shf_communicator_send_message(SHF_DEVICE_SCP, (void*)buffer, sizeof(buffer));
    ALOGD("shf_condition_action_add: sid=%d, size=%d, status=%d, cid=%d, action=%d", 
        sid, sizeof(buffer), status, condition_index, action);	
    if (NO_ERROR != status) {
        ALOGW("shf_condition_action_add: fail!! cid=%d, action=%d", condition_index, action);
        return SHF_CONDITION_ACTION_SIZE;
	}

    if (pick_buffer(SHF_AP_CONDITION_ACTION_ADD, sid)) {
        //out: opId, sessionId, AID array size, [AID]
        ALOGD("shf_condition_action_add: succeed. aid=%d", waiter_data[3]);
        return waiter_data[3];
    } else {
        ALOGW("shf_condition_action_add: timeout!! cid=%d, action=%d", condition_index, action);
    }
    return SHF_CONDITION_ACTION_SIZE;
}

status_t shf_condition_action_remove(shf_condition_index_t condition_index, shf_action_index_t action_index) {
    uint8_t sid = session_id_create();
    uint8_t buffer[] = {SHF_AP_CONDITION_ACTION_REMOVE, sid, condition_index, 0x01/*actionSize*/, action_index};
    status_t status = shf_communicator_send_message(SHF_DEVICE_SCP, (void*)buffer, sizeof(buffer));
    ALOGD("shf_condition_action_remove: sid=%d, size=%d, status=%d, cid=%d, aid=%d", 
        sid, sizeof(buffer), status, condition_index, action_index);
	if (NO_ERROR != status) {
        ALOGW("shf_condition_action_remove: fail!! cid=%d, aid=%d", condition_index, action_index);
		return status;
	}

    if (pick_buffer(SHF_AP_CONDITION_ACTION_REMOVE, sid)) {
        //out: opId, sessionId, status_t array size, [status_t]
        ALOGD("shf_condition_action_remove: succeed. cid=%d, aid=%d, status=%d", condition_index, action_index, waiter_data[3]);
        return waiter_data[3];
    } else {
        ALOGW("shf_condition_action_remove: timeout!! cid=%d, aid=%d", condition_index, action_index);
    }
    return SHF_STATUS_ERROR;
}

static size_t wrap_condition(const shf_condition_t* const condition, uint8_t* buffer) {
    size_t item_count = 0;
    for (size_t i = 0; i < SHF_CONDITION_ITEM_SIZE; i++) {
        if (condition->item[i].op) {//for offset data, index is 0, but op is MOD_OFFSET
            item_count++;
            ALOGD("wrap_condition: i=%d, index1Addr=0x%x, combineAddr=0x%x, valueAddr=0x%x", 
                i, &condition->item[i].dindex1, &condition->item[i].combine, &condition->item[i].value);			
        } else {
            break;
        }
    }
    size_t action_count = 0;
    for (size_t i = 0; i < SHF_CONDITION_ACTION_SIZE; i++) {
        if (condition->action[i]) {
            action_count++;
        } else {
            break;
        }
    }
    size_t item_size = sizeof(shf_condition_item_t) * item_count;
    size_t action_size = sizeof(shf_action_id_t) * action_count;
    size_t total = item_size + action_size + 2;
    ALOGD("wrap_condition: itemCount=%d, itemSize=%d, actionCount=%d, actionSize=%d, total=%d", 
        item_count, item_size, action_count, action_size, total);
    memset(buffer, 0, total);
    buffer[0] = item_count;
    memcpy(buffer + 1, condition->item, item_size);
    buffer[1 + item_size] = action_count;
    memcpy(buffer + item_size + 2, condition->action, action_size);
    return total;
}

//allocate a new condition and add action to slot 0
shf_condition_index_t shf_condition_add(const shf_condition_t* const condition) {
    uint8_t sid = session_id_create();
    uint8_t buffer[SHF_AP_BUFFER_BYTES] = {SHF_AP_CONDITION_ADD, sid};
    size_t size = wrap_condition(condition, buffer + 2);
    status_t status = shf_communicator_send_message(SHF_DEVICE_SCP, buffer, size + 2);
    ALOGD("shf_condition_add: sid=%d, size=%d, status=%d", sid, (size + 2), status);
	if (NO_ERROR != status) {
        ALOGW("shf_condition_add: fail!!");
		return status;
	}

    if (pick_buffer(SHF_AP_CONDITION_ADD, sid)) {
        //out: opid, sessionId, CID
        ALOGV("shf_condition_add: succeed. cid=%d", waiter_data[2]);
        return waiter_data[2];
    } else {
        ALOGW("shf_condition_add: timeout!!");
    }
    return 0;
}

status_t shf_condition_update(shf_condition_index_t condition_index,
        const shf_condition_t* const condition) {
    uint8_t sid = session_id_create();
    uint8_t buffer[SHF_AP_BUFFER_BYTES] = {SHF_AP_CONDITION_UPDATE, sid, condition_index};
    size_t size = wrap_condition(condition, buffer + 3);
    status_t status = shf_communicator_send_message(SHF_DEVICE_SCP, buffer, size + 3);
    ALOGD("shf_condition_update: sid=%d, size=%d, status=%d, cid=%d", sid, (size + 3), status, condition_index);	
	if (NO_ERROR != status) {
        ALOGW("shf_condition_update: fail!! cid=%d", condition_index);
		return status;
	}

    if (pick_buffer(SHF_AP_CONDITION_UPDATE, sid)) {
        //out: opId, sessionId, status_t
        ALOGV("shf_condition_update: succeed. cid=%d, status=%d", condition_index, waiter_data[2]);
        return waiter_data[2];
    } else {
        ALOGW("shf_condition_update: timeout!! cid=%d!", condition_index);
    }
    return SHF_STATUS_ERROR;
}

status_t shf_enable_gesture(bool enable) {
    status_t status = shf_communicator_enable_gesture(enable);
    ALOGD("shf_enable_gesture: enable=%d, status=%d", enable, status);	
	return status ? SHF_STATUS_ERROR : SHF_STATUS_OK;
}

bool shf_action_valids[] = {
    false,//SHF_ACTION_ID_UNKNOWN
    true,//SHF_ACTION_ID_AP_WAKEUP
    true,//SHF_ACTION_ID_TOUCH_ACTIVE
    true,//SHF_ACTION_ID_TOUCH_DEACTIVE
    true,//SHF_ACTION_ID_CONSYS_WAKEUP
};
bool shf_action_valid(shf_action_id_t action) {
    if (action >= SHF_ACTION_SIZE) {
        return false;
    }
    return shf_action_valids[action];
}

bool shf_data_valids[] = {
    false,//UNKNOWN 0
    true,//SHF_DATA_INDEX_ACCELEROMETER_X 1
    true,//SHF_DATA_INDEX_ACCELEROMETER_Y 2
    true,//SHF_DATA_INDEX_ACCELEROMETER_Z 3
    true,//SHF_DATA_INDEX_ACCELEROMETER_TIME 4
    false,// 5
    true,//SHF_DATA_INDEX_LIGHT_VALUE 6
    true,//SHF_DATA_INDEX_LIGHT_TIME 7
    false,// 8
    true,//SHF_DATA_INDEX_PROXIMITY_VALUE 9
    true,//SHF_DATA_INDEX_PROXIMITY_TIME 10
    false,// 11
    true,//SHF_DATA_INDEX_CLOCK_TIME 12
    false,// 13
    true,//SHF_DATA_INDEX_PEDOMETER_LENGTH 14
    true,//SHF_DATA_INDEX_PEDOMETER_FREQUENCY 15
    true,//SHF_DATA_INDEX_PEDOMETER_COUNT 16
    true,//SHF_DATA_INDEX_PEDOMETER_DISTANCE 17
    true,//SHF_DATA_INDEX_PEDOMETER_TIME 18
    false,// 19
    true,//SHF_DATA_INDEX_ACTIVITY_VEHICLE 20
    true,//SHF_DATA_INDEX_ACTIVITY_BIKE 21
    true,//SHF_DATA_INDEX_ACTIVITY_FOOT 22
    true,//SHF_DATA_INDEX_ACTIVITY_STILL 23
    true,//SHF_DATA_INDEX_ACTIVITY_UNKNOWN 24
    true,//SHF_DATA_INDEX_ACTIVITY_TILT 25
    true,//SHF_DATA_INDEX_ACTIVITY_TIME 26
    false,// 27
    true,//SHF_DATA_INDEX_INPOCKET_INPOCKET 28
    true,//SHF_DATA_INDEX_INPOCKET_TIME 29
    false,// 30
    true,//SHF_DATA_INDEX_MPACTIVITY_ACTIVITY 31
    true,//SHF_DATA_INDEX_MPACTIVITY_CONFIDENCE 32
    true,//SHF_DATA_INDEX_MPACTIVITY_TIME 33
    false,// 34
    true,//SHF_DATA_INDEX_SIGNIFICANT_VALUE 35
    true,//SHF_DATA_INDEX_SIGNIFICANT_TIME 36
    false,// 37
    true, //SHF_DATA_INDEX_PICKUP_VALUE 38
    true, //SHF_DATA_INDEX_PICKUP_TIME 39
    false, // 40
    true, //SHF_DATA_INDEX_FACEDOWN_VALUE 41
    true, //SHF_DATA_INDEX_FACEDOWN_TIME 42
    false, // 43
    true, //SHF_DATA_INDEX_SHAKE_VALUE 44
    true, //SHF_DATA_INDEX_SHAKE_TIME 45
    false, // 46
    true, //SHF_DATA_INDEX_GESTURE_VALUE 47
    true, //SHF_DATA_INDEX_GESTURE_TIME 48
    false, // 49    
};
bool shf_data_valid(shf_data_index_t index) {
    if (index >= SHF_POOL_SIZE) {
        return false;
    }
    return shf_data_valids[index];
}

shf_data_type_t shf_data_types[SHF_POOL_SIZE + 1] = {
    SHF_DATA_TYPE_INVALID, //0

    SHF_DATA_INDEX_TYPE_ACCELEROMETER_X, // 1
    SHF_DATA_INDEX_TYPE_ACCELEROMETER_Y, // 2
    SHF_DATA_INDEX_TYPE_ACCELEROMETER_Z, // 3
    SHF_DATA_INDEX_TYPE_ACCELEROMETER_TIME, // 4
    SHF_DATA_TYPE_INVALID, //5

    SHF_DATA_INDEX_TYPE_LIGHT_VALUE, //6
    SHF_DATA_INDEX_TYPE_LIGHT_TIME, //7
    SHF_DATA_TYPE_INVALID, //8

    SHF_DATA_INDEX_TYPE_PROXIMITY_VALUE, //9
    SHF_DATA_INDEX_TYPE_PROXIMITY_TIME,//10
    SHF_DATA_TYPE_INVALID, //11

    SHF_DATA_INDEX_TYPE_CLOCK_TIME, //12
    SHF_DATA_TYPE_INVALID, //13

    SHF_DATA_INDEX_TYPE_PEDOMETER_LENGTH, //14
    SHF_DATA_INDEX_TYPE_PEDOMETER_FREQUENCY, //15
    SHF_DATA_INDEX_TYPE_PEDOMETER_COUNT, //16
    SHF_DATA_INDEX_TYPE_PEDOMETER_DISTANCE, //17
    SHF_DATA_INDEX_TYPE_PEDOMETER_TIME, //18
    SHF_DATA_TYPE_INVALID, //19

    SHF_DATA_INDEX_TYPE_ACTIVITY_VEHICLE,//20
    SHF_DATA_INDEX_TYPE_ACTIVITY_BIKE, //21
    SHF_DATA_INDEX_TYPE_ACTIVITY_FOOT, //22
    SHF_DATA_INDEX_TYPE_ACTIVITY_STILL, //23
    SHF_DATA_INDEX_TYPE_ACTIVITY_UNKNOWN, //24
    SHF_DATA_INDEX_TYPE_ACTIVITY_TILT, //25
    SHF_DATA_INDEX_TYPE_ACTIVITY_TIME, //26
    SHF_DATA_TYPE_INVALID,//27

    SHF_DATA_INDEX_TYPE_INPOCKET_VALUE, //28
    SHF_DATA_INDEX_TYPE_INPOCKET_TIME, //29
    SHF_DATA_TYPE_INVALID, //30

    SHF_DATA_INDEX_TYPE_MPACTIVITY_ACTIVITY, //31
    SHF_DATA_INDEX_TYPE_MPACTIVITY_CONFIDENCE, //32
    SHF_DATA_INDEX_TYPE_MPACTIVITY_TIME, //33
    SHF_DATA_TYPE_INVALID, //34

    SHF_DATA_INDEX_TYPE_SIGNIFICANT_VALUE, //35
    SHF_DATA_INDEX_TYPE_SIGNIFICANT_TIME, //36
    SHF_DATA_TYPE_INVALID, //37

    SHF_DATA_INDEX_TYPE_PICKUP_VALUE, //38        
    SHF_DATA_INDEX_TYPE_PICKUP_TIME, //39
    SHF_DATA_TYPE_INVALID, //40

    SHF_DATA_INDEX_TYPE_FACEDOWN_VALUE, //41
    SHF_DATA_INDEX_TYPE_FACEDOWN_TIME, //42
    SHF_DATA_TYPE_INVALID, //43

    SHF_DATA_INDEX_TYPE_SHAKE_VALUE, //44
    SHF_DATA_INDEX_TYPE_SHAKE_TIME, //45
    SHF_DATA_TYPE_INVALID, //46

    SHF_DATA_INDEX_TYPE_GESTURE_VALUE, //47
    SHF_DATA_INDEX_TYPE_GESTURE_TIME, //48
    SHF_DATA_TYPE_INVALID, //49
};
shf_data_type_t shf_data_type_get(shf_data_index_t index) {
    if (index >= SHF_POOL_SIZE) {
        return SHF_DATA_TYPE_INVALID;
    }
    return shf_data_types[index];
}

sensor_trigger_data_t* user_data;
size_t user_data_size;
size_t user_data_filled;
void shf_dispatch_handler(void* data, size_t size)
{
    uint8_t* buf = (uint8_t*)data;
    ALOGD("shf_dispatch_handler: msgId=0x%x, size=%d", *buf, size);
    switch(*buf) {
    case SHF_AP_TRIGGER:
        //should cast to user data
        if (user_data && size) {
            if (build_trigger_data(user_data + user_data_filled, (uint8_t*)data, size)) {
                user_data_filled += 1;
            } else {
                ALOGW("shf_dispatch_handler: failed to build trigger data! size=%d", size);
            }
        }
        break;
    default:
        event_lock();
        memcpy(waiter_data, data, size);
        waiter_size = size;
        event_notify();
        event_unlock();
        break;
    }
}

size_t shf_data_poll(sensor_trigger_data_t* data, size_t size)
{
    user_data = data;
    user_data_size = size;
    user_data_filled = 0;
    status_t r = NO_ERROR;
    while((r = shf_communicator_receive_message(SHF_DEVICE_SCP, shf_dispatch_handler)) == NO_ERROR
        && user_data_filled < user_data_size);
    user_data = NULL;
    user_data_size = 0;
    return user_data_filled;
}

status_t shf_open()
{
    event_init();
    status_t result = shf_communicator_init();
    ALOGV("shf_open: result=%d", result);
    return result;
}
status_t shf_close()
{
    shf_communicator_release();
    event_destroy();
    return NO_ERROR;
}
