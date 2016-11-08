/******************************************************************************
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                  Copyright (C) 2012-2014 by Dolby Laboratories.
 *                            All rights reserved.
 ******************************************************************************/

#define LOG_TAG "Dap2Stub"
#include <cutils/log.h>

#include "dap_cpdp.h"
#include "oamdi/include/oamdi.h"
#include "oamdi/include/oamdi_dec.h"
#include "dlb_bitbuf/include/dlb_bitbuf.h"
#include "dlb_bitbuf/include/dlb_bitbuf_read.h"

typedef struct
{
    unsigned num_channels;
    unsigned num_blocks;
    unsigned num_elements;

    void **data;

    unsigned max_num_channels;
    unsigned max_num_blocks;
    unsigned max_num_elements;
}clvec_buffer;

struct dap_cpdp_state_s
{
    const dlb_buffer *in_buf;
    size_t sample_count;
};

const char * dap_cpdp_get_version(void)
{ return DAP_CPDP_VERSION; }

size_t dap_cpdp_query_memory(const dap_cpdp_init_info *p_info)
{
    return sizeof(dap_cpdp_state);
}

size_t dap_cpdp_query_scratch(const dap_cpdp_init_info *p_info)
{
    return 1;
}

dap_cpdp_state *dap_cpdp_init(const dap_cpdp_init_info *p_info, void *p_mem)
{
    dap_cpdp_state *inst = (dap_cpdp_state*)p_mem;
    inst->in_buf = NULL;
    inst->sample_count = 0;
    return inst;
}

void dap_cpdp_shutdown(dap_cpdp_state *p_dap_cpdp)
{
    p_dap_cpdp->in_buf = NULL;
    p_dap_cpdp->sample_count = 0;
}

unsigned dap_cpdp_prepare(dap_cpdp_state *p_dap_cpdp, unsigned sample_count_on_256,
    const dlb_buffer *p_input, const oamdi *p_object_metadata,
    unsigned object_metadata_offset, const dap_cpdp_metadata *p_metadata_in,
    const dap_cpdp_mix_data *p_downmix_data, int b_height_channels)
{
    LOG_FATAL_IF(p_object_metadata != NULL,
        "%s() Metadata not supported i stub.", __FUNCTION__);
    p_dap_cpdp->in_buf = p_input;
    p_dap_cpdp->sample_count = sample_count_on_256 * 256;
    return p_input->nchannel;
}

unsigned
dap_cpdp_prepare_cqmf
    (dap_cpdp_state          *p_dap_cpdp
    ,const clvec_buffer      *p_input
    ,const oamdi             *p_object_metadata
    ,unsigned                 object_metadata_offset
    ,const dap_cpdp_metadata *p_metadata_in
    ,const dap_cpdp_mix_data *p_downmix_data
    ,int                      b_height_channels
    )
{
	/*stubs can't support CQMF input*/
	return 0;
}


dap_cpdp_metadata dap_cpdp_process(dap_cpdp_state *p_dap_cpdp, const dlb_buffer *p_output, void *scratch)
{
    unsigned i;
    const dlb_buffer *p_input = p_dap_cpdp->in_buf;
    LOG_FATAL_IF(p_input == NULL,
        "%s() dap_cpdp_prepare() must be called before this function.", __FUNCTION__);
    LOG_FATAL_IF(p_input->nchannel != p_output->nchannel,
        "%s() Can not change the number of channels in stub.", __FUNCTION__);
    LOG_FATAL_IF(p_input->data_type != p_output->data_type,
        "%s() Can not change the data type in stub.", __FUNCTION__);
    int sizeof_data_type = 0;
    switch (p_input->data_type)
    {
    case DLB_BUFFER_SHORT_16:
        sizeof_data_type = sizeof(int16_t);
        break;
    case DLB_BUFFER_INT_LEFT:
    case DLB_BUFFER_LONG_32:
        sizeof_data_type = sizeof(int32_t);
        break;
    case DLB_BUFFER_FLOAT:
        sizeof_data_type = sizeof(float);
        break;
    case DLB_BUFFER_DOUBLE:
        sizeof_data_type = sizeof(double);
        break;
    default:
        LOG_FATAL("%s() Unknown dlb_buffer data type %d", __FUNCTION__, p_input->data_type);
    };
    ptrdiff_t stride = p_input->nstride * sizeof_data_type;
    for (i = 1; i < p_input->nchannel; ++i) {
        LOG_FATAL_IF(((((uint8_t*)p_input->ppdata[i-1]) + stride) != p_input->ppdata[i]),
            "%s() Input buffer not allocated on contiguous memory", __FUNCTION__);
        LOG_FATAL_IF(((((uint8_t*)p_output->ppdata[i-1]) + stride) != p_output->ppdata[i]),
            "%s() Output buffer not allocated on contiguous memory", __FUNCTION__);
    }
    memcpy(p_output->ppdata[0], p_input->ppdata[0], stride * p_dap_cpdp->sample_count);
    p_dap_cpdp->in_buf = NULL;
    p_dap_cpdp->sample_count = 0;

    dap_cpdp_metadata ret = { 0 };
    return ret;
}

unsigned dap_cpdp_get_latency(dap_cpdp_state *p_dap_cpdp)
{
    return 0;
}

void dap_cpdp_output_mode_set(dap_cpdp_state *p_dap_cpdp,
                              int             processing_mode,
                              unsigned        nb_output_channels,
                              const int      *p_mix_matrix)
{

}

void dap_cpdp_process_optimizer_enable_set(dap_cpdp_state *p_dap_cpdp, int b_enable)
{

}

void dap_cpdp_process_optimizer_bands_set(dap_cpdp_state *p_dap_cpdp,
                                          unsigned        nb_bands,
                                          const unsigned *p_center_frequencies,
                                          const int      *p_gains)
{

}

void dap_cpdp_pregain_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_postgain_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_system_gain_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_surround_decoder_enable_set(dap_cpdp_state *p_dap_cpdp, int enable)
{

}

void dap_cpdp_virtualizer_front_speaker_angle_set(dap_cpdp_state *p_dap_cpdp, unsigned angle)
{

}

void dap_cpdp_virtualizer_surround_speaker_angle_set(dap_cpdp_state *p_dap_cpdp, unsigned angle)
{

}

void dap_cpdp_virtualizer_height_speaker_angle_set(dap_cpdp_state *p_dap_cpdp, unsigned angle)
{

}

void dap_cpdp_height_filter_mode_set(dap_cpdp_state *p_dap_cpdp, int mode)
{

}

void dap_cpdp_bass_extraction_enable_set(dap_cpdp_state *p_dap_cpdp, int enable)
{

}

void dap_cpdp_bass_extraction_cutoff_frequency_set(dap_cpdp_state *p_dap_cpdp, unsigned cutoff_freq)
{

}

void dap_cpdp_surround_boost_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_mi2ieq_steering_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_mi2dv_leveler_steering_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_mi2dialog_enhancer_steering_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_mi2surround_compressor_steering_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_calibration_boost_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_leveler_amount_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_leveler_in_target_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_leveler_out_target_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_leveler_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_modeler_calibration_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volume_modeler_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_ieq_bands_set(dap_cpdp_state *p_dap_cpdp, unsigned int nb_bands,
    const unsigned int *p_band_centers, const int *p_band_targets)
{

}

void dap_cpdp_ieq_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_ieq_amount_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_de_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_de_amount_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_de_ducking_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_volmax_boost_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_graphic_equalizer_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_graphic_equalizer_bands_set(dap_cpdp_state *p_dap_cpdp, unsigned int nb_bands,
    const unsigned int *p_freq, const int *p_gains)
{

}

void dap_cpdp_audio_optimizer_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_audio_optimizer_bands_set(dap_cpdp_state *p_dap_cpdp, unsigned int nb_bands,
    const unsigned int *p_freq, int* const ap_gains[DAP_CPDP_MAX_NUM_OUTPUT_CHANNELS])
{
}

void dap_cpdp_bass_enhancer_enable_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_bass_enhancer_boost_set(dap_cpdp_state *p_dap_cpdp, int boost)
{
}

void dap_cpdp_bass_enhancer_cutoff_frequency_set(dap_cpdp_state *p_dap_cpdp, unsigned cutoff_freq)
{
}

void dap_cpdp_bass_enhancer_width_set(dap_cpdp_state  *p_dap_cpdp, int width)
{
}

void dap_cpdp_vis_bands_get(dap_cpdp_state *p_dap_cpdp, unsigned int *p_nb_bands,
    unsigned int p_band_centers[DAP_CPDP_VIS_NB_BANDS_MAX],
    int p_band_gains[DAP_CPDP_VIS_NB_BANDS_MAX],
    int p_band_excitations[DAP_CPDP_VIS_NB_BANDS_MAX])
{
}

void dap_cpdp_vis_custom_bands_get(dap_cpdp_state *p_dap_cpdp, unsigned int nb_bands,
    const unsigned int *p_band_centers, int *p_band_gains, int *p_band_excitation)
{
}

void dap_cpdp_regulator_tuning_set(dap_cpdp_state *p_dap_cpdp, unsigned nb_bands,
    const unsigned *p_band_centers, const int *p_low_thresholds,
    const int *p_high_thresholds, const int *p_isolated_bands)
{

}

void dap_cpdp_regulator_overdrive_set(dap_cpdp_state *p_dap_cpdp, int overdrive)
{

}

void dap_cpdp_regulator_timbre_preservation_set(dap_cpdp_state *p_dap_cpdp, int timbre_preservation)
{

}

void dap_cpdp_regulator_relaxation_amount_set(dap_cpdp_state *p_dap_cpdp, int relaxation_amount)
{

}

void dap_cpdp_regulator_speaker_distortion_enable_set(dap_cpdp_state *p_dap_cpdp, int speaker_distortion_enable)
{

}

void dap_cpdp_regulator_enable_set(dap_cpdp_state *p_dap_cpdp, int regulator_enable)
{

}

void dap_cpdp_regulator_tuning_info_get(dap_cpdp_state *p_dap_cpdp, unsigned int *p_nb_bands,
    int p_regulator_gains[DAP_CPDP_REGULATOR_TUNING_INFO_NB_BANDS_MAX],
    int p_regulator_excitations[DAP_CPDP_REGULATOR_TUNING_INFO_NB_BANDS_MAX])
{

}

void dap_cpdp_virtual_bass_mode_set(dap_cpdp_state *p_dap_cpdp, int mode)
{

}

void dap_cpdp_virtual_bass_src_freqs_set(dap_cpdp_state *p_dap_cpdp, unsigned low_src_freq,unsigned high_src_freq)
{

}

void dap_cpdp_virtual_bass_overall_gain_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_virtual_bass_slope_gain_set(dap_cpdp_state *p_dap_cpdp, int value)
{

}

void dap_cpdp_virtual_bass_subgains_set(dap_cpdp_state *p_dap_cpdp, unsigned size, const int *p_subgains)
{

}

void dap_cpdp_virtual_bass_mix_freqs_set(dap_cpdp_state *p_dap_cpdp, unsigned low_mix_freq, unsigned high_mix_freq)
{

}

//#ifdef DOLBY_OAMDI_BITBUF_STUBS

static const dlb_bitbuf_version_info kBitbufVer =
{
    0, 0, 0,
    "Stub Bitbuf"
};

const dlb_bitbuf_version_info *dlb_bitbuf_get_version(void)
{
    return &kBitbufVer;
}

void dlb_bitbuf_init(dlb_bitbuf_handle p_bitbuf, DLB_BITBUF_DATATYPE *p_base, unsigned long bitbuf_size)
{

}

int dlb_bitbuf_skip(dlb_bitbuf_handle p_bitbuf, long num_bits)
{
    return 0;
}

int dlb_bitbuf_set_abs_pos(dlb_bitbuf_handle p_bitbuf, unsigned long abs_bit_pos)
{
    return 0;
}


int dlb_bitbuf_align(dlb_bitbuf_handle p_bitbuf)
{
    return 0;
}

unsigned int dlb_bitbuf_get_alignment_bits(dlb_bitbuf_handle p_bitbuf)
{
    return 0;
}

unsigned long dlb_bitbuf_get_abs_pos(dlb_bitbuf_handle p_bitbuf)
{
    return 0;
}

long dlb_bitbuf_get_bits_left(dlb_bitbuf_handle p_bitbuf)
{
    return 0;
}

unsigned int dlb_bitbuf_read(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned long dlb_bitbuf_read_long(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned int dlb_bitbuf_fast_read(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned long dlb_bitbuf_fast_read_long(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

int dlb_bitbuf_safe_read(dlb_bitbuf_handle p_bitbuf, unsigned int n, unsigned int *p_data)
{
    return 0;
}

int dlb_bitbuf_safe_read_long(dlb_bitbuf_handle p_bitbuf, unsigned int n, unsigned long *p_data)
{
    return 0;
}

unsigned int dlb_bitbuf_peek(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned long dlb_bitbuf_peek_long(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned int dlb_bitbuf_fast_peek(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

unsigned long dlb_bitbuf_fast_peek_long(dlb_bitbuf_handle p_bitbuf, unsigned int n)
{
    return 0;
}

int dlb_bitbuf_safe_peek(dlb_bitbuf_handle p_bitbuf, unsigned int n, unsigned int *p_data)
{
    return 0;
}

int dlb_bitbuf_safe_peek_long(dlb_bitbuf_handle p_bitbuf, unsigned int n, unsigned long *p_data)
{
    return 0;
}

int dlb_bitbuf_write(dlb_bitbuf_handle p_bitbuf, unsigned int data, unsigned int n)
{
    return 0;
}

int dlb_bitbuf_write_long(dlb_bitbuf_handle p_bitbuf, unsigned long data, unsigned int n)
{
    return 0;
}

struct oamdi_s
{

};

size_t oamdi_query_mem(const oamdi_init_info *p_init_info)
{
    return sizeof(oamdi);
}

oamdi *oamdi_init(const oamdi_init_info *p_config, void *p_mem)
{
    oamdi *inst = (oamdi*)p_mem;
    return inst;
}

oamdi *oamdi_duplicate(const oamdi_init_info *p_config, void *p_mem, const oamdi *p_oamdi_src)
{
    oamdi *inst = (oamdi*)p_mem;
    *inst = *p_oamdi_src;
    return inst;
}

void oamdi_validate_after_copy(oamdi *p_oamdi)
{

}

void oamdi_set_prog_assign(oamdi *p_oamdi, const oamdi_prog_assign *p_prog)
{

}

void oamdi_set_md_update_info(oamdi *p_oamdi, const oamdi_md_update_info *p_md_update_info)
{

}

void oamdi_set_sample_offset(oamdi *p_oamdi, unsigned sample_offset)
{

}

void oamdi_set_obj_info_blk(oamdi *p_oamdi, unsigned obj_id, unsigned obj_info_blk_idx, const oamdi_obj_info_blk *p_obj_md)
{

}

void oamdi_set_obj_not_active(oamdi *p_oamdi, unsigned obj_id, unsigned obj_info_blk_idx, oamdi_bool value)
{

}

static const oamdi_lib_version_info kOmdiVersion = { 0 };

const oamdi_lib_version_info *oamdi_get_lib_ver(void)
{
    return &kOmdiVersion;
}

static const oamdi_metadata_version_info kOmdiMetaVer = { 0 };
const oamdi_metadata_version_info *oamdi_get_md_ver(void)
{
    return &kOmdiMetaVer;
}

void oamdi_get_init_info(const oamdi *p_oamdi, oamdi_init_info *p_oamdi_config)
{

}

unsigned oamdi_get_obj_count(const oamdi *p_oamdi)
{
    return 0;
}

const oamdi_prog_assign *oamdi_get_prog_assign(const oamdi *p_oamdi)
{
    return NULL;
}

const oamdi_obj_data *oamdi_get_all_obj_md(const oamdi *p_oamdi)
{
    return NULL;
}

const oamdi_obj_info_blk *oamdi_get_obj_info_blk(const oamdi *p_oamdi, unsigned obj_id, unsigned obj_info_blk_idx)
{
    return NULL;
}

const oamdi_md_update_info *oamdi_get_md_update_info(const oamdi *p_oamdi)
{
    return NULL;
}

unsigned oamdi_get_sample_offset(const oamdi *p_oamdi)
{
    return 0;
}

unsigned oamdi_get_num_obj_info_blks(const oamdi *p_oamdi)
{
    return 0;
}

unsigned oamdi_get_obj_not_active(const oamdi *p_oamdi, unsigned  obj_id, unsigned  obj_info_blk_idx)
{
    return 0;
}

unsigned oamdi_is_std_chan_assign(const oamdi_bed_chan_assign_mask bed_chan_assign_mask)
{
    return 0;
}

unsigned oamdi_get_bed_channels_count(const oamdi_bed_chan_assign_mask bed_chan_assign_mask)
{
    return 0;
}

int oamdi_get_init_info_from_bitstream(const unsigned char *p_bs, size_t bs_size, unsigned *p_num_objs, unsigned *p_num_obj_info_blks)
{
    return 0;
}

int oamdi_get_oamd_ver_from_bitstream(const unsigned char *p_bs, size_t bs_size, unsigned *p_oamd_ver)
{
    return 0;
}

int oamdi_from_bitstream(oamdi *p_oamdi, size_t bs_size, const unsigned char *p_bs)
{
    return 0;
}

size_t oamdi_get_bitstream_size(const oamdi *p_oamdi)
{
    return 0;
}

size_t oamdi_get_max_bitstream_size(const oamdi_init_info *p_config)
{
    return 0;
}

size_t oamdi_to_bitstream(const oamdi *p_oamdi, size_t bs_size, unsigned char *p_bs)
{
    return 0;
}

//#endif//DOLBY_OMDI_BITBUF_STUBS
