/******************************************************************************
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                  Copyright (C) 2014 by Dolby Laboratories.
 *                            All rights reserved.
 ******************************************************************************/

/** @file */

#ifndef DAP_CPDP_CQMF_H
#define DAP_CPDP_CQMF_H

#include "dap_cpdp.h"

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

#define DAP_CPDP_CQMF_MAX_BLOCKS	(24)
#define DAP_CPDP_CQMF_MAX_ELEMENTS	(64)

/** @defgroup dap-prepare-cqmf Prepare DAP CPDP with CQMF data.
 *  @{*/
#define DAP_CPDP_CQMF_BLOCK_MULTIPLE (4)
/**  dap_cpdp_prepare_cqmf()
 *
 * This may be called instead of calling dap_cpdp_prepare() to input data
 * which has already been processed by a CQMF analysis bank.
 * In this case, the parameters are mostly identical to the parameters
 * which would be passed to dap_cpdp_prepare(). The differences are:
 * - p_input is a clvec_buffer instead of a dlb_buffer. The constraints are:
 *    - p_input->num_channels has the same meaning as the nchannels element
 *      of the old dlb_buffer argument.
 *    - p_input->max_num_channels must be >= p_input->num_channels
 *    - p_input->num_blocks must be a multiple of DAP_CPDP_CQMF_BLOCK_MULTIPLE,
 *      greater than 0 and no greater than DAP_CPDP_MAX_BLOCKS *
 *      DAP_CPDP_CQMF_BLOCK_MULTIPLE
 *    - p_input->max_num_blocks must be >= p_input->num_blocks
 *    - p_input->num_elements must be 64
 *    - p_input->max_num_elements must be >= p_input->num_elements
 *    - p_input->data[ch][blk] is the DLB_CLVEC corresponding to the output
 *      from a CQMF filter bank for channel 'ch' on block 'blk'.
 *    - The CQMF filter bank used must have the same center frequencies as
 *      the one used when feeding dap_cpdp with non-CQMF data.
 *    - The pointer given must remain valid until dap_cpdp_process() has
 *      returned. The data pointed to must not change in this time.
 * - The p_object_metadata, object_metadata_offset, p_metadata_in,
 *   p_downmix_data and b_height_channels flags should be given with 288
 *   samples of latency. This is the amount of latency introduced by the
 *   CQMF analysis, so this means that the values for these parameters at the
 *   time of the CQMF analysis should be passed directly to dap_cpdp_prepare()
 *   and do not need to be buffered.
 *
 * There are constraints about when you can call dap_cpdp_process(),
 * dap_cpdp_prepare() and dap_cpdp_prepare_cqmf(). These can be explained
 * by a state transition diagram:
 *
 * @verbatim
 * +---------------+                                   +-----------------+
 * |               |---> dap_cpdp_prepare() ---------> | PCM Input state |->-+
 * | Initial State |                                   +-----------------+   |
 * |               |--+                                                      V
 * +---------------+  |                                +------------------+  |
 *        ^           +-> dap_cpdp_prepare_cqmf() ---> | CQMF Input State |--+
 *        |                                            +------------------+  |
 *        |                                                                  V
 *        +-<<------------<< dap_cpdp_process() <<----------------------<<---+
 * @endverbatim
 *
 * i.e. after each call to dap_cpdp_prepare*(), you must call
 *      dap_cpdp_process(). It is valid to switch between CQMF input and PCM
 *      input, however the audio may be discontinuous during the transition.
 *
 * ### LATENCY
 * The latency reported by dap_cpdp_get_latency() is the latency including
 * the latency which would have been introduced by the CQMF analysis step.
 * This is done even though this latency is not introduced by this library.
 *
 * ### ERRORS
 * In addition to the relevant error conditions of dap_cpdp_prepare(), this
 * function will also return 0 in the case where p_input->num_blocks is
 * not a multiple of DAP_CPDP_CQMF_BLOCK_MULTIPLE.
 */
unsigned
dap_cpdp_prepare_cqmf
    (dap_cpdp_state          *p_dap_cpdp
    ,const clvec_buffer      *p_input
    ,const oamdi             *p_object_metadata
    ,unsigned                 object_metadata_offset
    ,const dap_cpdp_metadata *p_metadata_in
    ,const dap_cpdp_mix_data *p_downmix_data
    ,int                      b_height_channels
    );
/**@}*/

#endif
