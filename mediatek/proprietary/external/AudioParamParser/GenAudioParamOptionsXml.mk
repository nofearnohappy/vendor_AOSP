###########################################################
## Options to be translated into XML nodes.
###########################################################
_audio_param_parser_FO_list := \
  MTK_WB_SPEECH_SUPPORT \
  MTK_AUDIO_HD_REC_SUPPORT \
  MTK_DUAL_MIC_SUPPORT \
  MTK_HANDSFREE_DMNR_SUPPORT \
  DMNR_TUNNING_AT_MODEMSIDE \
  MTK_VOIP_ENHANCEMENT_SUPPORT \
  MTK_TB_WIFI_3G_MODE \
  MTK_DISABLE_EARPIECE \
  MTK_ASR_SUPPORT \
  MTK_VOIP_NORMAL_DMNR \
  MTK_VOIP_HANDSFREE_DMNR \
  MTK_INCALL_NORMAL_DMNR \
  MTK_VOICE_UNLOCK_SUPPORT \
  MTK_VOICE_UI_SUPPORT \
  MTK_ACF_AUTO_GEN_SUPPORT \
  MTK_SPEAKER_MONITOR_SUPPORT \
  MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV \
  MTK_MAGICONFERENCE_SUPPORT \
  MTK_HAC_SUPPORT \
  MTK_AUDIO_SPH_LPBK_PARAM \
  MTK_AUDIO_GAIN_TABLE_BT \
  MTK_AUDIO_BT_NREC_WO_ENH_MODE \
  MTK_AUDIO_TUNING_TOOL_V2_PHASE \
  MATV_AUDIO_SUPPORT \
  MTK_FM_SUPPORT

###########################################################
## Complex option customization are defined here.
##
## Prefix temporary variables with "_aupapa_" to prevent
## the temporary variable from messing up global namespace.
## The string will be stripped while assembling the XML.
##
## Finally add the variable to _audio_param_parser_FO_list.
###########################################################
# MTK_WIFI_ONLY_SUPPORT
ifeq ($(MTK_TB_WIFI_3G_MODE),WIFI_ONLY)
  _aupapa_VIR_WIFI_ONLY_SUPPORT := yes
else
  _aupapa_VIR_WIFI_ONLY_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_WIFI_ONLY_SUPPORT

# MTK_3G_DATA_ONLY_SUPPORT
ifneq ($(filter 3GDATA_SMS 3GDATA_ONLY,$(MTK_TB_WIFI_3G_MODE)),)
  _aupapa_VIR_3G_DATA_ONLY_SUPPORT := yes
else
  _aupapa_VIR_3G_DATA_ONLY_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_3G_DATA_ONLY_SUPPORT

# SUPPORT_ASR
ifeq ($(MTK_ASR_SUPPORT),yes)
  ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
      _aupapa_VIR_ASR_SUPPORT := yes
    else
      _aupapa_VIR_ASR_SUPPORT := no
    endif
else
    _aupapa_VIR_ASR_SUPPORT:=no
endif
_audio_param_parser_FO_list += _aupapa_VIR_ASR_SUPPORT

# SUPPORT_VOIP_NORMAL_DMNR
ifneq ($(MTK_DISABLE_EARPIECE),yes)
  ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
    ifeq ($(MTK_VOIP_NORMAL_DMNR),yes)
      ifeq ($(MTK_VOIP_ENHANCEMENT_SUPPORT),yes)
        _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT := yes
      else
        _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT := no
      endif
    else
      _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT := no
    endif
  else
    _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT := no
  endif
else
  _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_VOIP_NORMAL_DMNR_SUPPORT

# SUPPORT_VOIP_HANDSFREE_DMNR
ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
  ifeq ($(MTK_VOIP_HANDSFREE_DMNR),yes)
    ifeq ($(MTK_VOIP_ENHANCEMENT_SUPPORT),yes)
      _aupapa_VIR_VOIP_HANDSFREE_DMNR_SUPPORT := yes
    else
      _aupapa_VIR_VOIP_HANDSFREE_DMNR_SUPPORT := no
    endif
  else
    _aupapa_VIR_VOIP_HANDSFREE_DMNR_SUPPORT := no
  endif
else
  _aupapa_VIR_VOIP_HANDSFREE_DMNR_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_VOIP_HANDSFREE_DMNR_SUPPORT

# NO_SPEECH
_aupapa_VIR_NO_SPEECH := no
ifeq ($(_aupapa_VIR_WIFI_ONLY_SUPPORT),yes)
  _aupapa_VIR_NO_SPEECH := yes
endif
ifeq ($(_aupapa_VIR_3G_DATA_ONLY_SUPPORT),yes)
  _aupapa_VIR_NO_SPEECH := yes
endif
_audio_param_parser_FO_list += _aupapa_VIR_NO_SPEECH

# SUPPORT_INCALL_NORMAL_DMNR
ifneq ($(MTK_DISABLE_EARPIECE),yes)
  ifneq ($(_aupapa_VIR_NO_SPEECH),yes)
    ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
      ifneq ($(MTK_INCALL_NORMAL_DMNR),no)
        _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT := yes
      else
        _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT := no
      endif
    else
      _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT := no
    endif
  else
    _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT := no
  endif
else
  _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_INCALL_NORMAL_DMNR_SUPPORT

# SUPPORT_INCALL_HANDSFREE_DMNR
ifneq ($(_aupapa_VIR_NO_SPEECH),yes)
  ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
    ifeq ($(MTK_INCALL_HANDSFREE_DMNR),yes)
      _aupapa_VIR_INCALL_HANDSFREE_DMNR_SUPPORT := yes
    else
      _aupapa_VIR_INCALL_HANDSFREE_DMNR_SUPPORT := no
    endif
  else
    _aupapa_VIR_INCALL_HANDSFREE_DMNR_SUPPORT := no
  endif
else
  _aupapa_VIR_INCALL_HANDSFREE_DMNR_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_INCALL_HANDSFREE_DMNR_SUPPORT

# SUPPORT_VOICE_UNLOCK
ifeq ($(MTK_VOICE_UNLOCK_SUPPORT),yes)
  _aupapa_VIR_VOICE_UNLOCK_SUPPORT := yes
endif
ifeq ($(MTK_VOICE_UI_SUPPORT),yes)
  _aupapa_VIR_VOICE_UNLOCK_SUPPORT := yes
endif
_audio_param_parser_FO_list += VIR_VOICE_UNLOCK_SUPPORT

# VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V5
ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V5 := yes
else
  _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V5 := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V5

# VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V4
ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
  _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V4 := yes
else
  _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V4 := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_AUDIO_BLOUD_CUSTOMPARAMETER_V4

# SUPPORT_MAGI_CONFERENCE
ifeq ($(MTK_MAGICONFERENCE_SUPPORT),yes)
  ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
    _aupapa_VIR_MAGI_CONFERENCE_SUPPORT := yes
  else
    _aupapa_VIR_MAGI_CONFERENCE_SUPPORT := no
  endif
else
  _aupapa_VIR_MAGI_CONFERENCE_SUPPORT := no
endif
_audio_param_parser_FO_list += _aupapa_VIR_MAGI_CONFERENCE_SUPPORT

# SUPPORT_AUDIO_LAYERED_PARAM
ifneq ($(MTK_AUDIO_TUNING_TOOL_VERSION),)
  ifneq ($(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)),V1)
    _aupapa_MTK_AUDIO_TUNING_TOOL_V2_PHASE := \
      $(shell echo $(MTK_AUDIO_TUNING_TOOL_VERSION) | sed 's/V2\.//g')
    _aupapa_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT := yes
   endif
endif
_audio_param_parser_FO_list += _aupapa_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT
_audio_param_parser_FO_list += _aupapa_MTK_AUDIO_TUNING_TOOL_V2_PHASE

###########################################################
## Assemble XML node string from given option
###########################################################

# $(1): option name
define gen_XML_node_from_option
$(eval _xml_node_name  := $(patsubst _aupapa_%,%,$(strip $(1)))) \
$(eval _xml_node_value := $(strip $($(1)))) \
$(if $(strip $(_xml_node_name)),, \
  $(error AudioParamParser: gen_FO_xml_node: XML node name cannot be empty) \
) \
<Param name="$(_xml_node_name)" value="$(_xml_node_value)" /> \
$(eval _xml_node_name  :=) \
$(eval _xml_node_value :=)
endef

###########################################################
## Generate full XML document
###########################################################

define gen_XML_doc
$(eval _aupapa_xml_doc := <?xml version="1.0" encoding="UTF-8"?>\n) \
$(eval _aupapa_xml_doc := $(_aupapa_xml_doc)<AudioParamOptions>\n) \
$(foreach f,$(_audio_param_parser_FO_list), \
  $(eval _aupapa_xml_doc += $(call gen_XML_node_from_option,$(f))\n) \
) \
$(foreach i,$(AUDIO_PARAM_OPTIONS_LIST), \
  $(eval _aupapa_xml_doc += <Param name="$(firstword $(subst =, ,$(i)))" value="$(or $(word 2,$(subst =, ,$(i))),$(value 2))" />\n) \
) \
$(eval _aupapa_xml_doc := $(_aupapa_xml_doc)</AudioParamOptions>) \
$(strip $(_aupapa_xml_doc))
endef

###########################################################
## Target definition
###########################################################
AUDIOPARAM_XML_INSTALLED := $(TARGET_OUT_ETC)/audio_param/AudioParamOptions.xml
AUDIOPARAM_XML_DEPS := \
  $(MTK_TARGET_PROJECT_FOLDER)/ProjectConfig.mk \
  $(LOCAL_PATH)/Android.mk \
  $(lastword $(MAKEFILE_LIST))

$(AUDIOPARAM_XML_INSTALLED): $(AUDIOPARAM_XML_DEPS)
	@echo $@: $?
	@mkdir -p $(dir $@)
	@echo -e '$(strip $(gen_XML_doc))' > $@

ALL_DEFAULT_INSTALLED_MODULES += $(AUDIOPARAM_XML_INSTALLED)
