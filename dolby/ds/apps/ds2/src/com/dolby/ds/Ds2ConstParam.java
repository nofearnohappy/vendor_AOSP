/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds;
import android.util.SparseArray;
import android.media.AudioSystem;

public class Ds2ConstParam
{

    public static final int ieq_band_centers[] = {65, 136, 223, 332, 467, 634, 841, 1098, 1416, 1812, 2302, 2909, 3663, 4598, 5756, 7194, 8976, 11186, 13927, 17326};
    public static final int ieq_open_targets[] = {117, 133, 188, 176, 141, 149, 175, 185, 185, 200, 236, 242, 228, 213, 182, 132, 110, 68, -27, -240};
    public static final int ieq_rich_targets[] = {67, 95, 172, 163, 168, 201, 189, 242, 196, 221, 192, 186, 168, 139, 102, 57, 35, 9, -55, -235};
    public static final int ieq_focused_targets[] = {-419, -112, 75, 116, 113, 160, 165, 80, 61, 79, 98, 121, 64, 70, 44, -71, -33, -100, -238, -411};
    public static final int geq_band_centers[] = {47, 141, 234, 328, 469, 656, 844, 1031, 1313, 1688, 2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 13875, 19688};
    public static final int geq_open_gains[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int geq_rich_gains[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int geq_focused_gains[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int surround_decoder_enable = 1;
    public static final int graphic_equalizer_enable = 0;
    public static final int ieq_enable = 1;
    public static final int ieq_amount = 10;
    public static final int dialog_enhancer_enable = 1;
    public static final int dialog_enhancer_amount = 5;
    public static final int dialog_enhancer_ducking = 0;
    public static final int volume_leveler_enable = 1;
    public static final int volume_leveler_amount = 7;
    public static final int volmax_boost = 144;
    public static final int calibration_boost = 0;
    public static final int volume_modeler_enable = 0;
    public static final int mi_surround_compressor_steering_enable = 1;
    public static final int mi_dialog_enhancer_steering_enable = 1;
    public static final int mi_dv_leveler_steering_enable = 1;
    public static final int mi_ieq_steering_enable = 1;
    public static final int virtualizer_speaker_start_freq = 200;
    public static final int virtualizer_headphone_reverb_gain = 0;
    public static final int intermediate_profile_partial_virtualizer_enable = 1;
    public static final int intermediate_profile_bass_enhancer_enable = 1;
    public static final int intermediate_profile_audio_optimizer_enable = 1;
    public static final int intermediate_profile_regulator_enable = 1;
    public static final int intermediate_profile_regulator_speaker_dist_enable = 1;
    public static final int surround_boost = 96;
    public static final int volume_leveler_in_target = -320;
    public static final int volume_leveler_out_target = -320;
    public static final  int audio_optimizer_band_centers[] = {47, 141, 234, 328, 469, 656, 844, 1031, 1313, 1688, 2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 13875, 19688};
    public static final int audio_optimizer_band_gains[][] =
    {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };
    public static final  int audio_regulator_band_centers[] = {47, 141, 234, 328, 469, 656, 844, 1031, 1313, 1688, 2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 13875, 19688};
    public static final int audio_regulator_low_thresholds[] = {-192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192, -192};
    public static final int audio_regulator_high_thresholds[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int audio_regulator_isolated_bands[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int regulator_overdrive = 0;
    public static final int regulator_timbre_preservation = 12;
    public static final int regulator_relaxation_amount = 0;
    public static final int bass_enhancer_boost = 192;
    public static final int bass_enhancer_cutoff_frequency = 200;
    public static final int bass_enhancer_width = 16;
    public static final int virtualizer_speaker_angle = 10;
    public static final int volume_modeler_calibration = 0;
    public static final int intermediate_headphone_partial_virtualizer_enable = 1;
    public static final int intermediate_headphone_bass_enhancer_enable = 0;
    public static final int intermediate_headphone_audio_optimizer_enable = 0;
    public static final int intermediate_headphone_regulator_enable = 1;
    public static final int intermediate_headphone_regulator_speaker_dist_enable = 0;

    public static SparseArray singleParamArr = new SparseArray<Integer>();
    public static byte[] multiByteArray[] = new byte[4][];

    static
    {
        //CP
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_NGON, surround_decoder_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_GEON, graphic_equalizer_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_IEON, ieq_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_IEA, ieq_amount);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DEON, dialog_enhancer_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DEA, dialog_enhancer_amount);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DED, dialog_enhancer_ducking);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVLE, volume_leveler_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVLA, volume_leveler_amount);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_VMB, volmax_boost);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_PLB, calibration_boost);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVME, volume_modeler_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_MSCE, mi_surround_compressor_steering_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_MDEE, mi_dialog_enhancer_steering_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_MDLE, mi_dv_leveler_steering_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_MIEE, mi_ieq_steering_enable);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DSSF, virtualizer_speaker_start_freq);
        //singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DHRG, virtualizer_headphone_reverb_gain);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DSB, surround_boost);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVLI, volume_leveler_in_target);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVLO, volume_leveler_out_target);

        //DP
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_AROD, regulator_overdrive);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_ARTP, regulator_timbre_preservation);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_ARRA, regulator_relaxation_amount);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_BEB, bass_enhancer_boost);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_BECF, bass_enhancer_cutoff_frequency);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_BEW, bass_enhancer_width);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DSSA, virtualizer_speaker_angle);
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_DVMC, volume_modeler_calibration);
        //Intermediate parameters
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_VTON, (intermediate_profile_partial_virtualizer_enable 
                                                        & intermediate_headphone_partial_virtualizer_enable));
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_BEON, (intermediate_profile_bass_enhancer_enable 
                                                        & intermediate_headphone_bass_enhancer_enable));
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_AOON, (intermediate_profile_audio_optimizer_enable 
                                                        & intermediate_headphone_audio_optimizer_enable));
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_ARON, (intermediate_profile_regulator_enable 
                                                        & intermediate_headphone_regulator_enable));
        singleParamArr.put(Ds2ParamDefs.DAP2_PARAM_ARDE, (intermediate_profile_regulator_speaker_dist_enable 
                                                        & intermediate_headphone_regulator_speaker_dist_enable));

        // Multiple Parameter
        //IEQ
        Ds2ParamDefs.Ds2MultiParam ieqParam = new Ds2ParamDefs.Ds2MultiParam();
        multiByteArray[0] = ieqParam.setMultiParam(AudioSystem.DEVICE_OUT_DEFAULT, 
                Ds2ParamDefs.DAP2_PARAM_IEBS,
                ieq_band_centers.length,
                ieq_band_centers,
                ieq_open_targets);

        //GEQ
        Ds2ParamDefs.Ds2MultiParam geqParam = new Ds2ParamDefs.Ds2MultiParam();
        multiByteArray[1] = geqParam.setMultiParam(AudioSystem.DEVICE_OUT_DEFAULT, 
                Ds2ParamDefs.DAP2_PARAM_GEBS,
                geq_band_centers.length,
                geq_band_centers,
                geq_open_gains);

        //Regulator
        Ds2ParamDefs.Ds2MultiParam regParam = new Ds2ParamDefs.Ds2MultiParam();
        multiByteArray[2] = regParam.setMultiParam(AudioSystem.DEVICE_OUT_DEFAULT,
                Ds2ParamDefs.DAP2_PARAM_ARBS,
                audio_regulator_band_centers.length,
                audio_regulator_band_centers,
                audio_regulator_low_thresholds,
                audio_regulator_high_thresholds,
                audio_regulator_isolated_bands);

        //Optimizer
        Ds2ParamDefs.Ds2MultiParam optParam = new Ds2ParamDefs.Ds2MultiParam();
        multiByteArray[3] = optParam.setMultiParam(AudioSystem.DEVICE_OUT_DEFAULT,
                Ds2ParamDefs.DAP2_PARAM_AOBS,
                audio_optimizer_band_centers.length,
                audio_optimizer_band_centers,
                (audio_optimizer_band_gains[0]),
                (audio_optimizer_band_gains[1]),
                (audio_optimizer_band_gains[2]),
                (audio_optimizer_band_gains[3]),
                (audio_optimizer_band_gains[4]),
                (audio_optimizer_band_gains[5]),
                (audio_optimizer_band_gains[6]),
                (audio_optimizer_band_gains[7]));
       
    }
}
