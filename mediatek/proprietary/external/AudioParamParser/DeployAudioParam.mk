DEFAULT_AUDIO_PARAM_DIR := $(TARGET_OUT_ETC)/audio_param
DEFAULT_AUDIO_PARAM_FILE := default.audio_param

EXTRACT_FILE_LIST := "*_AudioParam.xml" "*_ParamUnitDesc.xml" "*_ParamTreeView.xml"

#$(warning INSTALL_AUDIO_PARAM_DIR_LIST = $(INSTALL_AUDIO_PARAM_DIR_LIST))
#$(warning INSTALL_AUDIO_PARAM_FILE_LIST = $(INSTALL_AUDIO_PARAM_FILE_LIST))

$(shell mkdir -p $(DEFAULT_AUDIO_PARAM_DIR))

# Deploy these files in INSTALL_AUDIO_PARAM_DIR_LIST to DEFAULT_AUDIO_PARAM_DIR
$(foreach i,$(INSTALL_AUDIO_PARAM_DIR_LIST), \
    $(shell cp -f $(i)/*.xml $(DEFAULT_AUDIO_PARAM_DIR)/) \
)

# Deploy these files in INSTALL_AUDIO_PARAM_FILE_LIST to DEFAULT_AUDIO_PARAM_FILE
$(foreach i,$(INSTALL_AUDIO_PARAM_FILE_LIST), \
    $(shell cp -f $(i) $(TARGET_OUT_ETC)/$(DEFAULT_AUDIO_PARAM_FILE)) \
)

# Check if the audio_param exist, uncompress & delete it
HAS_AUDIO_PARAM_FILE := $(shell test -e $(TARGET_OUT_ETC)/$(DEFAULT_AUDIO_PARAM_FILE) && echo yes || echo no)
ifeq ($(HAS_AUDIO_PARAM_FILE),yes)
exec_ := $(shell unzip -o $(TARGET_OUT_ETC)/$(DEFAULT_AUDIO_PARAM_FILE) $(EXTRACT_FILE_LIST) -d $(DEFAULT_AUDIO_PARAM_DIR))
#$(shell rm -f $(TARGET_OUT_ETC)/$(DEFAULT_AUDIO_PARAM_FILE))
endif
