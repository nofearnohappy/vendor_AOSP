#ifndef SHF_HAL_H_
#define SHF_HAL_H_

#include "shf_define.h"

typedef struct {
    shf_data_index_t index;
    shf_data_type_t type;
    shf_data_value_t value;
    shf_data_value_t last;
} trigger_data_t;
typedef struct {
    uint8_t cid;
    uint8_t aid[SHF_CONDITION_ACTION_SIZE];
    trigger_data_t data[SHF_CONDITION_ITEM_SIZE * 2];//each item has 2 indices
} sensor_trigger_data_t;

status_t shf_open();
status_t shf_close();

shf_action_index_t shf_condition_action_add(shf_condition_index_t condition_index, shf_action_id_t action);
status_t shf_condition_action_remove(shf_condition_index_t condition_index, shf_action_index_t action_index);
//status_t shf_condition_action_update(shf_condition_index_t condition_index,
//        shf_action_index_t action_index, shf_action_id_t action);

//allocate a new condition and add action to slot 0
shf_condition_index_t shf_condition_add(const shf_condition_t* const condition);
//status_t shf_condition_remove(shf_condition_index_t condition_index);
status_t shf_condition_update(shf_condition_index_t condition_index,
        const shf_condition_t* const condition);

bool shf_action_valid(shf_action_id_t);
bool shf_data_valid(shf_data_index_t);//means context avaliable
bool shf_data_get(shf_data_index_t, shf_data_value_t*);
shf_data_type_t shf_data_type_get(shf_data_index_t);

status_t shf_enable_gesture(bool enable);

size_t shf_data_poll(sensor_trigger_data_t* data, size_t size);

#endif /* SHF_HAL_H_ */
