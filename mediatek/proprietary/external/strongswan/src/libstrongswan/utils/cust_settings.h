#ifndef __WO_CFG_H__
#define __WO_CFG_H__

#include <stdbool.h>

typedef union cust_value_t {
	int  integer;
	char *str;
	bool boolean;
} cust_value_t;


typedef struct cust_setting_t {
	char* system_property_key;
	cust_value_t default_value;
} cust_setting_t;

typedef enum cust_setting_type_t {
	SETTING_START,
	IS_CUST_PCSCF,
	CUST_PCSCF_IP4_VALUE,
	CUST_PCSCF_IP6_VALUE,
	SETTING_END
} cust_setting_type_t;

static cust_setting_t cust_settings[SETTING_END] = {
	[CUST_PCSCF_IP4_VALUE] = {"persist.net.wo.cust_pcscf_4",  20},
	[CUST_PCSCF_IP6_VALUE] = {"persist.net.wo.cust_pcscf_6",  21}
};

int get_cust_setting(cust_setting_type_t type, char *value);
bool get_cust_setting_bool(cust_setting_type_t type);
int get_cust_setting_int(cust_setting_type_t type);

static inline const char* get_key(cust_setting_type_t type);
static inline cust_value_t get_default(cust_setting_type_t type);

#endif
