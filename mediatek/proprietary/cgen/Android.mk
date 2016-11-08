# FIXME
include device/mediatek/build/build/Makefile
ifeq ($(strip $(MTK_BASE_PROJECT)),)
  MTK_PROJECT_NAME := $(subst full_,,$(TARGET_PRODUCT))
else
  MTK_PROJECT_NAME := $(MTK_BASE_PROJECT)
endif
MTK_PROJECT := $(MTK_PROJECT_NAME)
ifeq ($(MTK_INTERNAL),yes)
ifneq ($(wildcard vendor/mediatek/proprietary/buildinfo/label.ini),)
  include vendor/mediatek/proprietary/buildinfo/label.ini
  ifeq ($(MTK_BUILD_VERNO),ALPS.W10.24.p0)
    MTK_BUILD_VERNO := $(MTK_INTERNAL_BUILD_VERNO)
  endif
  ifeq ($(MTK_WEEK_NO),W10.24)
    MTK_WEEK_NO := $(MTK_INTERNAL_WEEK_NO)
  endif
endif
endif


MTK_INCLUDE_CHECK_VARIABLE := MTK_PROJECT MTK_CHIP_VER MTK_WEEK_NO
MTK_INCLUDE_CHECK_VARIABLE += TARGET_DEVICE
MTK_PREGEN_TEMP := $(strip $(foreach v,$(MTK_INCLUDE_CHECK_VARIABLE),$(if $($(v)),,$(v))))
ifneq ($(MTK_PREGEN_TEMP),)
  $(warning *** These variables are not defined:)
  $(warning *** $(MTK_PREGEN_TEMP))
  $(error Please check makefile inclusion)
endif

MTK_CGEN_BIN_DIR := vendor/mediatek/proprietary/cgen
MTK_CGEN_OUT_DIR := $(TARGET_OUT_INTERMEDIATES)/CGEN
MTK_CGEN_CUSTOM_DIR := vendor/mediatek/proprietary/custom/$(strip $(MTK_PROJECT))
MTK_CGEN_CUSTOM_PROJECT_DIR := vendor/mediatek/proprietary/custom/$(strip $(MTK_PROJECT))/cgen
MTK_CGEN_CUSTOM_PLATFORM_DIR := vendor/mediatek/proprietary/custom/$(subst M,m,$(subst T,t,$(strip $(MTK_PLATFORM))))/cgen
MTK_CGEN_CUSTOM_COMMON_DIR := vendor/mediatek/proprietary/custom/common/cgen

MTK_CGEN_COMPILE_OPTION := $(strip $(MTK_INTERNAL_CDEFS))
MTK_CGEN_COMPILE_OPTION += -I$(MTK_CGEN_OUT_DIR)/inc -I$(MTK_CGEN_BIN_DIR)/apeditor

MTK_CGEN_APDB_SourceFile := $(MTK_CGEN_OUT_DIR)/apeditor/app_parse_db.c
MTK_CGEN_AP_Temp_CL	:= $(MTK_CGEN_OUT_DIR)/apeditor/app_temp_db

MTK_CGEN_TARGET_CFG	:= $(MTK_CGEN_BIN_DIR)/cgencfg/tgt_cnf
MTK_CGEN_HOST_CFG	:= $(MTK_CGEN_BIN_DIR)/cgencfg/pc_cnf
MTK_CGEN_EXECUTABLE	:= $(MTK_CGEN_BIN_DIR)/Cgen

MTK_CGEN_cfg_module_file_dir	:= $(MTK_CGEN_CUSTOM_PLATFORM_DIR)/cfgfileinc $(MTK_CGEN_CUSTOM_COMMON_DIR)/cfgfileinc
MTK_CGEN_cfg_module_default_dir	:= $(MTK_CGEN_CUSTOM_PLATFORM_DIR)/cfgdefault $(MTK_CGEN_CUSTOM_COMMON_DIR)/cfgdefault
MTK_CGEN_custom_cfg_module_file_dir     := $(MTK_CGEN_CUSTOM_PROJECT_DIR)/cfgfileinc
MTK_CGEN_custom_cfg_default_file_dir    := $(MTK_CGEN_CUSTOM_PROJECT_DIR)/cfgdefault

MTK_CGEN_cfg_module_file	:= $(MTK_CGEN_OUT_DIR)/inc/cfg_module_file.h
MTK_CGEN_cfg_module_default	:= $(MTK_CGEN_OUT_DIR)/inc/cfg_module_default.h
MTK_CGEN_custom_cfg_module_file		:= $(MTK_CGEN_OUT_DIR)/inc/custom_cfg_module_file.h
MTK_CGEN_custom_cfg_default_file	:= $(MTK_CGEN_OUT_DIR)/inc/custom_cfg_module_default.h

MTK_CGEN_AP_Editor_BRANCH := $(strip $(patsubst ro.mediatek.version.branch=%,%,$(firstword $(filter ro.mediatek.version.branch=%,$(PRODUCT_PROPERTY_OVERRIDES)))))
ifeq ($(MTK_CGEN_AP_Editor_BRANCH),)
$(warning ro.mediatek.version.branch is not found in PRODUCT_PROPERTY_OVERRIDES, use MTK_BRANCH=$(MTK_BRANCH) instead)
MTK_CGEN_AP_Editor_BRANCH := $(MTK_BRANCH)
endif
MTK_CGEN_AP_Editor_DB		:= $(MTK_CGEN_OUT_DIR)/APDB_$(MTK_PLATFORM)_$(MTK_CHIP_VER)_$(MTK_CGEN_AP_Editor_BRANCH)_$(MTK_WEEK_NO)
MTK_CGEN_AP_Editor2_Temp_DB	:= $(MTK_CGEN_OUT_DIR)/.temp_APDB_$(MTK_PLATFORM)_$(MTK_CHIP_VER)_$(MTK_CGEN_AP_Editor_BRANCH)_$(MTK_WEEK_NO)
MTK_CGEN_AP_Editor_DB_Enum_File	:= $(MTK_CGEN_OUT_DIR)/APDB_$(MTK_PLATFORM)_$(MTK_CHIP_VER)_$(MTK_CGEN_AP_Editor_BRANCH)_$(MTK_WEEK_NO)_ENUM

ifneq ($(strip $(TARGET_BOARD_PLATFORM)),)
  MTK_CGEN_hardWareVersion := $(subst m,M,$(subst t,T,$(strip $(TARGET_BOARD_PLATFORM))))_$(MTK_CHIP_VER)
else
  MTK_CGEN_hardWareVersion := $(MTK_PLATFORM)_$(MTK_CHIP_VER)
endif
MTK_CGEN_SWVersion := $(MTK_BUILD_VERNO)

# $(1): $dir_path, the searched directory
# $(2): $fileName, the generated header file name
# $(3): $out_dir
define mtk-cgen-AutoGenHeaderFile
$(2): $$(foreach d,$(1),$$(sort $$(wildcard $$(d)/*.h))) $(4)
	@echo Cgen: $$@
	$(hide) rm -f $$@
	$(hide) mkdir -p $$(dir $$@)
	$(hide) for x in $$(foreach d,$(1),$$(sort $$(wildcard $$(d)/*.h))); do echo "#include \"$(3)$$$$x\"" >>$$@; done;
	$(hide) for x in $(4); do echo "#include \"$$$$x\"" >>$$@; done;
endef

define mtk-cgen-PreprocessFile
-include $$(basename $(2)).d
$(2): $(1)
	@echo Cgen: $$@
	@mkdir -p $$(dir $(2))
	$(hide) gcc $(3) -E $(1) -o $(2) -MP -MMD -MF $$(basename $(2)).d -MQ '$(2)'
endef


$(eval $(call mtk-cgen-AutoGenHeaderFile,$(MTK_CGEN_cfg_module_file_dir),$(MTK_CGEN_cfg_module_file)))
$(eval $(call mtk-cgen-AutoGenHeaderFile,$(MTK_CGEN_cfg_module_default_dir),$(MTK_CGEN_cfg_module_default)))

$(eval $(call mtk-cgen-AutoGenHeaderFile,$(MTK_CGEN_custom_cfg_module_file_dir),$(MTK_CGEN_custom_cfg_module_file)))
$(eval $(call mtk-cgen-AutoGenHeaderFile,$(MTK_CGEN_custom_cfg_default_file_dir),$(MTK_CGEN_custom_cfg_default_file)))

$(eval $(call mtk-cgen-PreprocessFile,$(MTK_CGEN_APDB_SourceFile),$(MTK_CGEN_AP_Temp_CL),$(MTK_CGEN_COMPILE_OPTION) -I. -I$(MTK_CGEN_CUSTOM_PROJECT_DIR)/inc -I$(MTK_CGEN_CUSTOM_PLATFORM_DIR)/inc -I$(MTK_CGEN_CUSTOM_COMMON_DIR)/inc))

$(MTK_CGEN_APDB_SourceFile):
	@echo Cgen: $@
	$(hide) rm -f $@
	$(hide) mkdir -p $(dir $@)
	$(hide) echo \#include \"tst_assert_header_file.h\"	>$@
	$(hide) echo \#include \"ap_editor_data_item.h\"	>>$@
	$(hide) echo \#include \"Custom_NvRam_data_item.h\"	>>$@

$(MTK_CGEN_AP_Temp_CL): $(MTK_CGEN_cfg_module_file)
$(MTK_CGEN_AP_Temp_CL): $(MTK_CGEN_cfg_module_default)
$(MTK_CGEN_AP_Temp_CL): $(MTK_CGEN_custom_cfg_module_file)
$(MTK_CGEN_AP_Temp_CL): $(MTK_CGEN_custom_cfg_default_file)

$(MTK_CGEN_AP_Editor2_Temp_DB): $(MTK_CGEN_EXECUTABLE) $(MTK_CGEN_TARGET_CFG) $(MTK_CGEN_HOST_CFG) $(MTK_CGEN_AP_Temp_CL)
	@echo Cgen: $@
	$(hide) $(MTK_CGEN_EXECUTABLE) -c $(MTK_CGEN_AP_Temp_CL) $(MTK_CGEN_TARGET_CFG) $(MTK_CGEN_HOST_CFG) $(MTK_CGEN_AP_Editor2_Temp_DB) $(MTK_CGEN_AP_Editor_DB_Enum_File) $(MTK_CGEN_hardWareVersion) $(MTK_CGEN_SWVersion)

$(MTK_CGEN_AP_Editor_DB): $(MTK_CGEN_EXECUTABLE) $(MTK_CGEN_AP_Editor2_Temp_DB) $(MTK_CGEN_AP_Temp_CL)
	@echo Cgen: $@
	$(hide) $(MTK_CGEN_EXECUTABLE) -cm $(MTK_CGEN_AP_Editor_DB) $(MTK_CGEN_AP_Editor2_Temp_DB) $(MTK_CGEN_AP_Temp_CL) $(MTK_CGEN_AP_Editor_DB_Enum_File) $(MTK_CGEN_hardWareVersion) $(MTK_CGEN_SWVersion)

.PHONY: cgen
cgen:
ifneq ($(MTK_EMULATOR_SUPPORT), yes)
ifneq ($(PROJECT),generic)
cgen: $(MTK_CGEN_AP_Editor_DB)
endif
endif


MTK_BTCODEGEN_ROOT := vendor/mediatek/proprietary/protect/external/bluetooth/blueangel/_bt_scripts
MTK_BTCODEGEN_OUT_DIR := $(MTK_CGEN_OUT_DIR)
MTK_BTCODEGEN_catcher_ps_db_path := $(MTK_CGEN_OUT_DIR)/database/BTCatacherDB
MTK_BTCODEGEN_dbfolder := $(MTK_CGEN_OUT_DIR)/database
MTK_BTCODEGEN_parsedb := $(MTK_BTCODEGEN_ROOT)/database/parse_db.c
MTK_BTCODEGEN_pridb := $(MTK_BTCODEGEN_dbfolder)/msglog_db/parse.db
MTK_BTCODEGEN_pstrace_db_path := $(MTK_BTCODEGEN_dbfolder)/pstrace_db
MTK_BTCODEGEN_catcher_db_folder := $(MTK_CGEN_OUT_DIR)/database_win32
MTK_BTCODEGEN_ps_trace_h_catcher_path := $(MTK_BTCODEGEN_catcher_db_folder)/ps_trace.h
MTK_BTCODEGEN_ps_trace_file_list := $(MTK_BTCODEGEN_ROOT)/settings/ps_trace_file_list.txt
MTK_BTCODEGEN_pc_cnf := $(MTK_BTCODEGEN_ROOT)/database/Pc_Cnf
MTK_BTCODEGEN_tgt_cnf :=
MTK_BTCODEGEN_version := 1.0
MTK_BTCODEGEN_verno := W0949
ifneq ($(wildcard $(MTK_BTCODEGEN_ROOT)),)
  MTK_BTCODEGEN_TraceListAry := $(shell perl -p -e "s/\#.*$$//g" $(MTK_BTCODEGEN_ps_trace_file_list))
  MTK_BTCODEGEN_Options_ini := $(shell cat $(MTK_BTCODEGEN_ROOT)/settings/GCC_Options.ini)
endif
MTK_BTCODEGEN_GCC_Options := $(foreach item,$(MTK_BTCODEGEN_Options_ini),$(patsubst -I%,-I$(MTK_BTCODEGEN_ROOT)/%,$(item)))


$(eval $(call mtk-cgen-PreprocessFile,$(MTK_BTCODEGEN_parsedb),$(MTK_BTCODEGEN_pridb),$(MTK_CGEN_COMPILE_OPTION) $(MTK_BTCODEGEN_GCC_Options) -DGEN_FOR_CPARSER -DGEN_FOR_PC))

$(MTK_BTCODEGEN_dbfolder)/BPGUInfo: $(MTK_BTCODEGEN_pridb)
	@echo BTCodegen: $@
	$(hide) $(MTK_CGEN_EXECUTABLE) -c $(MTK_BTCODEGEN_pridb) $(MTK_CGEN_TARGET_CFG) $(MTK_CGEN_HOST_CFG) $(MTK_BTCODEGEN_dbfolder)/BPGUInfo $(MTK_BTCODEGEN_dbfolder)/enumFile MoDIS $(MTK_BTCODEGEN_verno)

MTK_BTCODEGEN_PTR_LIST :=
$(foreach file,$(MTK_BTCODEGEN_TraceListAry),\
  $(eval $(call mtk-cgen-PreprocessFile,$(MTK_BTCODEGEN_ROOT)/$(file),$(MTK_BTCODEGEN_pstrace_db_path)/$(notdir $(basename $(file))).ptr,$(MTK_CGEN_COMPILE_OPTION) $(MTK_BTCODEGEN_GCC_Options) -DGEN_FOR_PC)) \
  $(eval MTK_BTCODEGEN_PTR_LIST += $(MTK_BTCODEGEN_pstrace_db_path)/$(notdir $(basename $(file))).ptr) \
)

$(MTK_BTCODEGEN_catcher_ps_db_path): $(MTK_BTCODEGEN_dbfolder)/BPGUInfo $(MTK_BTCODEGEN_PTR_LIST)
	@echo BTCodegen: $@
	$(hide) rm -f $@
	$(hide) mkdir -p $(dir $(MTK_BTCODEGEN_ps_trace_h_catcher_path)) $(dir $(MTK_BTCODEGEN_catcher_ps_db_path))
	$(hide) $(MTK_CGEN_EXECUTABLE) -ps $(MTK_BTCODEGEN_catcher_ps_db_path) $(MTK_BTCODEGEN_dbfolder)/BPGUInfo $(MTK_BTCODEGEN_pstrace_db_path) $(MTK_BTCODEGEN_ps_trace_h_catcher_path)

.PHONY: btcodegen
btcodegen:
ifneq ($(MTK_EMULATOR_SUPPORT), yes)
ifneq ($(PROJECT),generic)
  ifneq ($(MTK_BTCODEGEN_SUPPORT),no)
    ifeq ($(MTK_BT_SUPPORT), yes)
      ifneq ($(wildcard $(MTK_BTCODEGEN_ROOT)/BTCodegen.pl),)
btcodegen: $(MTK_BTCODEGEN_catcher_ps_db_path)
      else # partial source building
$(info BT database auto-gen process disabled due to BT_DB_AUTO_GEN_SCRIPTS_PATH is not exist.)
      endif
    else
$(info BT database auto-gen process disabled due to Bluetooth is turned off.)
    endif
  endif
endif
endif

droidcore: cgen btcodegen
