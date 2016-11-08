include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	src/libevent/event_sig.c \
	src/libevent/event.c \
	src/libevent/event_tcp.c \
	src/libevent/hash.c

LOCAL_CFLAGS := -DUNIX -DLINUX
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += $(log_shared_libraries)

LOCAL_MODULE := libpppoeevent

include $(BUILD_STATIC_LIBRARY)
