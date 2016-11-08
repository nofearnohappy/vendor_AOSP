#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := user

LOCAL_SRC_FILES:= \
   agc.c \
   az_lsp.c \
   a_refl.c \
   bgnscd.c \
   bits2prm.c \
   b_cn_cod.c \
   c1035pf.c \
   c2_11pf.c \
   c2_9pf.c \
   c3_14pf.c \
   c4_17pf.c \
   c8_31pf.c \
   calc_cor.c \
   calc_en.c \
   cbsearch.c \
   cl_ltp.c \
   coder.c \
   cod_amr.c \
   convolve.c \
   copy.c \
   cor_h.c \
   c_g_aver.c \
   d1035pf.c \
   d2_11pf.c \
   d2_9pf.c \
   d3_14pf.c \
   d4_17pf.c \
   d8_31pf.c \
   dec_amr.c \
   dec_gain.c \
   dec_lag3.c \
   dec_lag6.c \
   dtx_dec.c \
   dtx_enc.c \
   d_gain_c.c \
   d_gain_p.c \
   d_homing.c \
   d_plsf.c \
   d_plsf_3.c \
   d_plsf_5.c \
   ec_gains.c \
   ex_ctrl.c \
   e_homing.c \
   gain_q.c \
   gc_pred.c \
   gmed_n.c \
   g_adapt.c \
   g_pitch.c \
   hp_max.c \
   inter_36.c \
   int_lpc.c \
   int_lsf.c \
   lpc.c \
   lsp.c \
   lsp_avg.c \
   lsp_az.c \
   lsp_lsf.c \
   ol_ltp.c \
   oper_32b.c \
   ph_disp.c \
   pitch_fr.c \
   pitch_ol.c \
   post_pro.c \
   pow2.c \
   pred_lt.c \
   preemph.c \
   pre_big.c \
   pre_proc.c \
   pstfilt.c \
   p_ol_wgh.c \
   qgain475.c \
   qgain795.c \
   q_gain_c.c \
   q_gain_p.c \
   q_plsf.c \
   q_plsf_3.c \
   q_plsf_5.c \
   residu.c \
   s10_8pf.c \
   set_sign.c \
   set_zero.c \
   sid_sync.c \
   spreproc.c \
   spstproc.c \
   sp_dec.c \
   sp_enc.c \
   sqrt_l.c \
   syn_filt.c \
   tab_bitno.c \
   tab_c2_9pf.c \
   tab_gains.c \
   tab_gray.c \
   tab_lsf_3.c \
   tab_lsf_5.c \
   tab_lsp.c \
   tab_qgain475.c \
   tab_qua_gain.c \
   tab_window.c \
   ton_stab.c \
   vad1.c \
   weight_a.c

LOCAL_MODULE := libamrvt

LOCAL_PRELINK_MODULE:=false 

LOCAL_ARM_MODE := arm

LOCAL_SHARED_LIBRARIES := \
	libnativehelper \
	libcutils \
	libutils
	
include $(BUILD_SHARED_LIBRARY)
