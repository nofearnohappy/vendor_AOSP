/*
$License:
Copyright (C) 2014 InvenSense Corporation, All Rights Reserved.
$
*/

/*******************************************************************************
*
* $Id$
*
******************************************************************************/

#ifndef ML_COMMAND_GENERATOR_H__
#define ML_COMMAND_GENERATOR_H__

#ifdef __cplusplus
extern "C" {
#endif

	struct fifo_command_t {
		int16_t command;
		unsigned char *parameter; 
		// Data size of command need dynamic allocation (Calibration related command has different size)
	};

	enum SENSOR_COMMAND {
		CMD_SENSOR_OFF			= 0x00,
		CMD_SENSOR_ON			= 0x01,
		CMD_SET_POWER			= 0x02,
		CMD_BATCH_ON			= 0x03,
		CMD_FLUSH				= 0x04,
		CMD_SET_DELAY			= 0x05,
		CMD_SET_CALIB_GAINS		= 0x06,
		CMD_GET_CALIB_GAINS		= 0x07,
		CMD_SET_CALIB_OFFSETS	= 0x08,
		CMD_GET_CALIB_OFFSETS	= 0x09,
		CMD_SET_REF_FRAME		= 0x0A,
		CMD_GET_FIRMWARE_INFO	= 0x0B,
		CMD_MAX
	};


	int get_activate_command(struct fifo_command_t* cmd, int id, int enable);

	int get_batch_command_and_parameter(struct fifo_command_t* cmd, int id,
		int64_t delay, int64_t timeout);

	int get_delay_command_and_parameter(struct fifo_command_t* cmd, int id,
		int64_t delay, int64_t timeout);

	int get_flush_command(struct fifo_command_t* cmd, int id);

	int get_meta_data_command(struct fifo_command_t* cmd, int id);

	int get_calibration_offset_command(struct fifo_command_t* cmd, unsigned char* calData, int id);

	int get_set_calibration_offset_command(struct fifo_command_t* cmd, unsigned char* calData, int id);

	int get_calibration_gain_command(struct fifo_command_t* cmd, unsigned char* calData, int id);

	int get_set_calibration_gain_command(struct fifo_command_t* cmd, unsigned char* calData, int id);

	int get_set_power_state_command(struct fifo_command_t* cmd, int state_on);

	int get_firmware_info_command(struct fifo_command_t* cmd);

	int get_flash_status_command(struct fifo_command_t* cmd);

#ifdef __cplusplus
}
#endif
#endif	/* ML_COMMAND_GENERATOR_H__ */
