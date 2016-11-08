/*
$License:
Copyright (C) 2014 InvenSense Corporation, All Rights Reserved.
$
*/

#undef MPL_LOG_NDEBUG
#define MPL_LOG_NDEBUG 0 /* Use 0 to turn on MPL_LOGV output */
#undef MPL_LOG_TAG
#define MPL_LOG_TAG "Sensor"

#include <string.h>
#include <stdio.h>
#include <dirent.h>
#include <ctype.h>
#include <log.h>
#include "ml_command_generator.h"

int get_activate_command(struct fifo_command_t* cmd, int id, int enable)
{
	if (cmd == NULL)
		return -1;
    
    MPL_LOGV("get_activate_command sensor id=%d enable=%d\n", id, enable);
    
	cmd->command = ((id & 0x00FF) << 8) | (!!enable);
	cmd->parameter = NULL;
	return 0;  // 0 is success?
}

int get_batch_command_and_parameter(struct fifo_command_t* cmd, int id,
	int64_t delay, int64_t timeout)
{
    if (cmd == NULL)
            return -1;
	if (delay < 0)
		return -1; 
   
    MPL_LOGV("get_batch_command sensor id=%d delay=%lld timeout=%lld\n", id, delay, timeout);

	if (timeout == 0) // Set delay 
	{
		cmd->command = ((id & 0x00FF) << 8) | CMD_SET_DELAY; 
		cmd->parameter = (unsigned char*)calloc(1, sizeof(uint16_t));
		memcpy(cmd->parameter, (void*)(&timeout), sizeof(uint16_t));

	}
	else // Batch On
	{
        //LOGV_IF(1, "get_batch_command_and_parameter 2\n");
		cmd->command = ((id & 0x00FF) << 8) | CMD_BATCH_ON; 
        //LOGV_IF(1, "get_batch_command_and_parameter 3\n");
		cmd->parameter = (unsigned char*)calloc(1, sizeof(uint16_t));
        //LOGV_IF(1, "get_batch_command_and_parameter 4\n");
		memcpy(cmd->parameter, (void*)(&timeout), sizeof(uint16_t)); 
        //LOGV_IF(1, "get_batch_command_and_parameter 5\n");
	}

	return 0;
}

/* TBD */
int get_delay_command_and_parameter(struct fifo_command_t* cmd, int id,
	int64_t delay, int64_t timeout)
{
	return 0;
}

int get_flush_command(struct fifo_command_t* cmd, int id)
{
	cmd->command = ((id & 0x00FF) << 8) | CMD_FLUSH;
	cmd->parameter = NULL;
	return 0;  
}

// TODO : Is there any define of get meta data?
int get_meta_data_command(struct fifo_command_t* cmd, int id)
{
	return 0;
}

int get_calibration_offset_command(struct fifo_command_t* cmd, unsigned char* calData, int id)
{
	cmd->command = ((id & 0x00FF) << 8) | CMD_GET_CALIB_OFFSETS;
	cmd->parameter = (unsigned char*)calloc(1, 3 * sizeof(uint32_t));
	memcpy(cmd->parameter, (void*)calData, 3 * sizeof(uint32_t)); 
	return 0; 
}

int get_set_calibration_offset_command(struct fifo_command_t* cmd, unsigned char* calData, int id)
{
	cmd->command = ((id & 0x00FF) << 8) | CMD_SET_CALIB_OFFSETS;
	cmd->parameter = (unsigned char*)calloc(1, 3 * sizeof(uint32_t));
	memcpy(cmd->parameter, (void*)calData, 3 * sizeof(uint32_t)); 
	return 0; 
}

int get_calibration_gain_command(struct fifo_command_t* cmd, unsigned char* calData, int id)
{
	cmd->command = ((id & 0x00FF) << 8) | CMD_GET_CALIB_GAINS;
	cmd->parameter = (unsigned char*)calloc(1, 9 * sizeof(uint32_t));
	memcpy(cmd->parameter, (void*)calData, 9 * sizeof(uint32_t)); 
	return 0; 
}

int get_set_calibration_gain_command(struct fifo_command_t* cmd, unsigned char* calData, int id)
{
	cmd->command = ((id & 0x00FF) << 8) | CMD_SET_CALIB_GAINS;
	cmd->parameter = (unsigned char*)calloc(1, 9 * sizeof(uint32_t));
	memcpy(cmd->parameter, (void*)calData, 9 * sizeof(uint32_t)); 
	return 0; 
}

int get_set_power_state_command(struct fifo_command_t* cmd, int state_on)
{
	cmd->command = /*((id & 0x00FF) << 8) | */CMD_SET_POWER;
	cmd->parameter = (unsigned char*)calloc(1, sizeof(uint16_t));
	memcpy(cmd->parameter, (void*)(&state_on), sizeof(uint16_t));
	return 0;
}

//TBD
int get_firmware_info_command(struct fifo_command_t* cmd)
{
	return 0;
}

//TBD
int get_flash_status_command(struct fifo_command_t* cmd)
{
	return 0;
}

