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

package com.dolby.api;

import android.util.Log;

public enum DsParams
{
    /* Parameters used by DAP1, begin */
    /**
     * Specifies the preferential enable for the Dolby Headphone feature.
     * numeric range: [0-2]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     *     2 - DS1_FEATURE_AUTO
     */
    DolbyHeadphoneVirtualizerControl(DsCommon.DAP1_PARAM_ID_START + 1),
    /**
     * Specifies the preferential enable for the Dolby Virtual Speaker feature.
     * numeric range: [0-2]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     *     2 - DS1_FEATURE_AUTO
     */
    DolbyVirtualSpeakerVirtualizerControl(DsCommon.DAP1_PARAM_ID_START + 2),
    /**
     * Specifies the preferential enable for the Dolby Volume Leveler feature. 
     * numeric range: [0-1]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     */
    DolbyVolumeLevelerEnable(DsCommon.DAP1_PARAM_ID_START + 3),
    /**
     * Specifies the preferential enable for the Dolby Volume Modeler feature. 
     * numeric range: [0-1]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     */
    DolbyVolumeModelerEnable(DsCommon.DAP1_PARAM_ID_START + 4),
    /**
     * Specifies the preferential enable for Next Gen Surround feature.
     * numeric range: [0-2]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     *     2 - DS1_FEATURE_AUTO
     */
    NextGenSurroundEnable(DsCommon.DAP1_PARAM_ID_START + 5),
    /**
     * Specifies the preferential enable for the Intelligent Equalizer feature. 
     * numeric range: [0-1]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     */
    IntelligentEqualizerEnable(DsCommon.DAP1_PARAM_ID_START + 6),
    /**
     * Specifies the preferential enable for the Dialog Enhancement feature.
     * numeric range: [0-1]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     */
    DialogEnhancementEnable(DsCommon.DAP1_PARAM_ID_START + 7),
    /**
     * Specifies the preferential enable for the Graphic Equalizer feature.
     * numeric range: [0-1]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     */
    GraphicEqualizerEnable(DsCommon.DAP1_PARAM_ID_START + 8),
    /**
     * Amount of boost applied to the surround channels when Dolby Headphone is enabled as the virtualizer, 
     * or when the Surround Compressor is active and no virtualizer is active.
     * numeric range: [0-1] (which means [0.00,6.00] dB)
     */
    DolbyHeadphoneSurroundBoost(DsCommon.DAP1_PARAM_ID_START + 9),
    /**
     * Amount of reverberation to add to the signal when Dolby Headphone is being used as the virtualizer.
     * numeric range: [-2080,192] (which means [-130.00,12.00] dB)
     */
    DolbyHeadphoneReverberationGain(DsCommon.DAP1_PARAM_ID_START + 10),
    /**
     * Amount of boost applied to the surround channels when Dolby Virtual Speaker is enabled as the virtualizer.
     * numeric range: [0-1] (which means [0.00,6.00] dB)
     */
    DolbyVirtualSpeakerSurroundBoost(DsCommon.DAP1_PARAM_ID_START + 11),
    /**
     * Informs Dolby Virtual Speaker of the physical angular separation of your loudspeakers.
     * numeric range: [5-30] degree
     */
    DolbyVirtualSpeakerAngle(DsCommon.DAP1_PARAM_ID_START + 12),
    /**
     * Frequencies below this value will not get virtualized.
     * numeric range: [20-20000] Hz
     */
    DolbyVirtualSpeakerStartFrequency(DsCommon.DAP1_PARAM_ID_START + 13),
    /**
     * Sets how much the leveler adjusts the loudness to normalize different audio content.
     * numeric range: [0-10]
     */
    DolbyVolumeLevelingAmount(DsCommon.DAP1_PARAM_ID_START + 14),
    /**
     * Specifies the band target levels for the Intelligent Equalizer.
     * numeric range: [-480,480] (which means [-30.00,30.00] dBFS)
     */
    IntelligentEqualizerBandTargets(DsCommon.DAP1_PARAM_ID_START + 15),
    /**
     * Specifies the strength of the Intelligent Equalizer effect to apply.
     * numeric range: [0,16] (which means [0.00,1.00])
     */
    IntelligentEqualizerAmount(DsCommon.DAP1_PARAM_ID_START + 16),
    /**
     * Specifies the strength of the Dialog Enhancement effect.
     * numeric range: [0,16] (which means [0.00,1.00])
     */
    DialogEnhancementAmount(DsCommon.DAP1_PARAM_ID_START + 17),
    /**
     * Controls the degree of suppresion of channels that don't contain dialog.
     * numeric range: [0,16] (which means [0.00,1.00])
     */
    DialogEnhancementDucking(DsCommon.DAP1_PARAM_ID_START + 18),
    /**
     * The band gains for the Graphic Equalizer.
     * numeric range: [-576,576] (which means [-36.00,36.00] dB)
     */
    GraphicEqualizerBandGains(DsCommon.DAP1_PARAM_ID_START + 19),
    /**
     * Specifies the preferential enable for the Audio Optimizer feature.
     * numeric range: [0-2]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     *     2 - DS1_FEATURE_AUTO
     */
    AudioOptimizerEnable(DsCommon.DAP1_PARAM_ID_START + 20),
    /**
     * Specifies a limited gain to be applied to the signal.
     * numeric range: [0,192] (which means [0.00,12.00] dB)
     */
    PeakLimiterBoost(DsCommon.DAP1_PARAM_ID_START + 21),
    /**
     * Controls the state of the Peak Limiter and Audio Regulator.
     * numeric range: [0-4]
     *     0 - DS1_PLMD_DISABLE_ALL
     *     1 - DS1_PLMD_PEAK_ONLY
     *     2 - DS1_PLMD_REGULATED_PEAK
     *     3 - DS1_PLMD_REGULATED_DISTORTION
     *     4 - DS1_PLMD_AUTO
     */
    PeakLimitingProtectionMode(DsCommon.DAP1_PARAM_ID_START + 22),
    /**
     * Specifies the preferential enable for the Volume Maximizer feature.
     * numeric range: [0-2]
     *     0 - DS1_FEATURE_OFF
     *     1 - DS1_FEATURE_ON
     *     2 - DS1_FEATURE_AUTO
     */
    VolumeMaximizerEnable(DsCommon.DAP1_PARAM_ID_START + 23),
    /**
     * The boost to be applied by Volume Maximizer.
     * numeric range: [0,192] (which means [0.00,12.00] dB)
     */
    VolumeMaximizerBoost(DsCommon.DAP1_PARAM_ID_START + 24),
    /**
     * Sets the target average loudness level of the Dolby Volume Leveler.
     * numeric range: [-640,0] (which means [-40.00,0.00] dB)
     */
    DolbyVolumeLevelerInputTarget(DsCommon.DAP1_PARAM_ID_START + 25),
    /**
     * The Leveler Output Target is adjusted at manufacture to calibrate the system to a reference playback sound pressure level.
     * numeric range: [-640,0] (which means [-40.00,0.00] dB)
     */
    DolbyVolumeLevelerOutputTarget(DsCommon.DAP1_PARAM_ID_START + 26),
    /**
     * Use this parameter to fine-tune the manufacturer calibrated reference level to the listening environment. 
     * numeric range: [-320,320] (which means [-20.00,20.00] dB)
     */
    DolbyVolumeModelerCalibration(DsCommon.DAP1_PARAM_ID_START + 27),
    /**
     * Specifies the number of target band gains and center frequencies that will be provided to the Intelligent Equalizer.
     * numeric range: [1,40]
     */
    IntelligentEqualizerBandCount(DsCommon.DAP1_PARAM_ID_START + 28),
    /**
     * Specifies the band center frequencies for the Intelligent Equalizer target levels.
     * numeric range: [20-20000] Hz
     */
    IntelligentEqualizerBandFrequencies(DsCommon.DAP1_PARAM_ID_START + 29),
    /**
     * The number of band gains that will be supplied to the Graphic Equalizer.
     * numeric range: [1,40]
     */
    GraphicEqualizerBandCount(DsCommon.DAP1_PARAM_ID_START + 30),
    /**
     * The band center frequencies for the Graphic Equalizer.
     * numeric range: [20-20000] Hz
     */
    GraphicEqualizerBandFrequencies(DsCommon.DAP1_PARAM_ID_START + 31),
    /**
     * The number of frequency bands per channel that will be provided to the Audio Optimizer.
     * numeric range: [1,40]
     */
    AudioOptimizerBandCount(DsCommon.DAP1_PARAM_ID_START + 32),
    /**
     * The center frequencies of the Audio Optimizer bands.
     * numeric range: [20-20000] Hz
     */
    AudioOptimizerBandFrequencies(DsCommon.DAP1_PARAM_ID_START + 33),
    /**
     * The Audio Optimizer gains for each band for each channel.
     * numeric range: [-480,480] (which means [-30.00,30.00] dB)
     */
    AudioOptimizerBandGains(DsCommon.DAP1_PARAM_ID_START + 34),
    /**
     * Specifies the number of frequency bands that will be supplied to the Audio Regulator.
     * numeric range: [1,40]
     */
    AudioRegulatorBandCount(DsCommon.DAP1_PARAM_ID_START + 35),
    /**
     * Specifies the center frequencies of the audio regulator bands.
     * numeric range: [20-20000] Hz
     */
    AudioRegulatorBandFrequencies(DsCommon.DAP1_PARAM_ID_START + 36),
    /**
     * The maximum number of channels of configuration data that will be provided to the Audio Optimizer.
     * numeric range: [0,8]
     */
    AudioOptimizerChannelCount(DsCommon.DAP1_PARAM_ID_START + 37),
    /**
     * Specifies each Audio regulator band as isolated or not.
     * numeric range: [0-1]
     *     0 - Not Isolated
     *     1 - Isolated
     */
    AudioRegulatorBandIsolates(DsCommon.DAP1_PARAM_ID_START + 38),
    /**
     * Sets the low onset point of the compression curve for each band.
     * numeric range: [-2080,0] (which means [-130.00,0.00] dBFS)
     */
    AudioRegulatorBandLowThresholds(DsCommon.DAP1_PARAM_ID_START + 39),
    /**
     * Sets the high limiting point of the compression curve for each band.
     * numeric range: [-2080,0] (which means [-130.00,0.00] dBFS)
     */
    AudioRegulatorBandHighThresholds(DsCommon.DAP1_PARAM_ID_START + 40),
    /**
     * The amount the signal is allowed to push past the distortion thresholds.
     * numeric range: [0,192] (which means [0.00,12.00] dB)
     */
    AudioRegulatorOverdrive(DsCommon.DAP1_PARAM_ID_START + 41),
    /**
     * Controls the compromise between loudness and timbre preservation at high volume levels.
     * numeric range: [0,16] (which means [0.00,1.00] )
     */
    AudioRegulatorTimbrePreservationAmount(DsCommon.DAP1_PARAM_ID_START + 42);
    /* Parameters used by DAP1, finish */

    /* Parameters used by DAP2 */
/*    
    DAP_CPDP_SYSTEM_GAIN(DsCommon.DAP_CPDP_PARAM_ID_START + 1),
    DAP_CPDP_POSTGAIN(DsCommon.DAP_CPDP_PARAM_ID_START + 2),
    DAP_CPDP_PREGAIN(DsCommon.DAP_CPDP_PARAM_ID_START + 3),
    DAP_CPDP_CALIBRATION_BOOST(DsCommon.DAP_CPDP_PARAM_ID_START + 4),
    DAP_CPDP_SURROUND_DECODER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 5),
    DAP_CPDP_DIALOG_ENHANCER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 6),
    DAP_CPDP_DIALOG_ENHANCER_AMOUNT(DsCommon.DAP_CPDP_PARAM_ID_START + 7),
    DAP_CPDP_DIALOG_ENHANCER_DUCKING(DsCommon.DAP_CPDP_PARAM_ID_START + 8),
    DAP_CPDP_DIALOG_VOLMAX_BOOST(DsCommon.DAP_CPDP_PARAM_ID_START + 9),
    DAP_CPDP_VOLUME_MODELER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 10),
    DAP_CPDP_VOLUME_MODELER_CALIBRATION(DsCommon.DAP_CPDP_PARAM_ID_START + 11),
    DAP_CPDP_VOLUME_LEVELER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 12),
    DAP_CPDP_VOLUME_LEVELER_AMOUNT(DsCommon.DAP_CPDP_PARAM_ID_START + 13),
    DAP_CPDP_VOLUME_LEVELER_IN_TARGET(DsCommon.DAP_CPDP_PARAM_ID_START + 14),
    DAP_CPDP_VOLUME_LEVELER_OUT_TARGET(DsCommon.DAP_CPDP_PARAM_ID_START + 15),
    DAP_CPDP_IEQ_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 16),
    DAP_CPDP_IEQ_AMOUNT(DsCommon.DAP_CPDP_PARAM_ID_START + 17),
    DAP_CPDP_IEQ_BANDS(DsCommon.DAP_CPDP_PARAM_ID_START + 18),
    DAP_CPDP_REGULATOR_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 19),
    DAP_CPDP_REGULATOR_SPEAKER_DIST_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 20),
    DAP_CPDP_REGULATOR_RELAXATION_AMOUNT(DsCommon.DAP_CPDP_PARAM_ID_START + 21),
    DAP_CPDP_REGULATOR_OVERDRIVE(DsCommon.DAP_CPDP_PARAM_ID_START + 22),
    DAP_CPDP_REGULATOR_TIMBRE_PRESERVATION(DsCommon.DAP_CPDP_PARAM_ID_START + 23),
    DAP_CPDP_REGULATOR_TUNING(DsCommon.DAP_CPDP_PARAM_ID_START + 24),
    DAP_CPDP_OUTPUT_MODE(DsCommon.DAP_CPDP_PARAM_ID_START + 25),
    DAP_CPDP_SURROUND_BOOST(DsCommon.DAP_CPDP_PARAM_ID_START + 26),
    DAP_CPDP_VIRTUALIZER_SPEAKER_START_FREQ(DsCommon.DAP_CPDP_PARAM_ID_START + 27),
    DAP_CPDP_VIRTUALIZER_SPEAKER_ANGLE(DsCommon.DAP_CPDP_PARAM_ID_START + 28),
    DAP_CPDP_VIRTUALIZER_HEADPHONE_REVERB_GAIN(DsCommon.DAP_CPDP_PARAM_ID_START + 29),
    DAP_CPDP_GRAPHIC_EQUALIZER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 30),
    DAP_CPDP_GRAPHIC_EQUALIZER_BANDS(DsCommon.DAP_CPDP_PARAM_ID_START + 31),
    DAP_CPDP_BASS_ENHANCER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 32),
    DAP_CPDP_BASS_ENHANCER_BOOST(DsCommon.DAP_CPDP_PARAM_ID_START + 33),
    DAP_CPDP_BASS_ENHANCER_CUTOFF_FREQUENCY(DsCommon.DAP_CPDP_PARAM_ID_START + 34),
    DAP_CPDP_BASS_ENHANCER_WIDTH(DsCommon.DAP_CPDP_PARAM_ID_START + 35),
    DAP_CPDP_AUDIO_OPTIMIZER_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 36),
    DAP_CPDP_AUDIO_OPTIMIZER_BANDS(DsCommon.DAP_CPDP_PARAM_ID_START + 37),
    DAP_CPDP_MI_SURROUND_COMPRESSOR_STEERING_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 38),
    DAP_CPDP_MI_IEQ_STEERING_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 39),
    DAP_CPDP_MI_DV_LEVELER_STEERING_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 40),
    DAP_CPDP_MI_DIALOG_ENHANCER_STEERING_ENABLE(DsCommon.DAP_CPDP_PARAM_ID_START + 41),
    DAP_CPDP_VIS_BANDS(DsCommon.DAP_CPDP_PARAM_ID_START + 42),
    DAP_CPDP_VIS_CUSTOM_BANDS(DsCommon.DAP_CPDP_PARAM_ID_START + 43),
    DAP_CPDP_REGULATOR_TUNING_INFO(DsCommon.DAP_CPDP_PARAM_ID_START + 44),
    DAP_CPDP_MI_METADATA(DsCommon.DAP_CPDP_PARAM_ID_START + 45),
    DAP_CPDP_DOWNMIX_CONFIG(DsCommon.DAP_CPDP_PARAM_ID_START + 46),
    DAP_CPDP_VERSION(DsCommon.DAP_CPDP_PARAM_ID_START + 47);
*/
    private int id_;
    private DsParams(int id) {this.id_ = id;}

    public int toInt() { return id_; }
    /**
     * @return A string representing the parameter.
     */
    public String toString() 
    {
        String name;

        if (id_ > DsCommon.DAP1_PARAM_ID_START && id_ < DsCommon.DAP1_PARAM_ID_END)
        {
            name = DAP1_PARAM_NAMES[id_ - DsCommon.DAP1_PARAM_ID_START]; 
        }
        /*else if (id_ > DsCommon.DAP_CPDP_PARAM_ID_START && id_ < DsCommon.DAP_CPDP_PARAM_ID_END)
        {
            name = DAP_CPDP_PARAM_NAMES[id_ - DsCommon.DAP_CPDP_PARAM_ID_START]; 
        }*/
        else
        {
            name = "error";
        }
        return name;
    }
    /**
     * @param i The integer of parameter id.
     * @return The DsParams that represents the parameter.
     */
    public static DsParams FromInt(int i)
    {
        DsParams item;

        if (i > DsCommon.DAP1_PARAM_ID_START && i < DsCommon.DAP1_PARAM_ID_END)
        {
            item = params[i - DsCommon.DAP1_PARAM_ID_START - 1]; 
        }
        /*else if (i > DsCommon.DAP_CPDP_PARAM_ID_START && i < DsCommon.DAP_CPDP_PARAM_ID_END)
        {
            item = params[i - DsCommon.DAP_CPDP_PARAM_ID_START - 1 + DsCommon.DAP1_PARAM_ID_END - 1]; 
        }*/
        else
        {
            item = null;
        }
        return item;
    }
    /**
     * @param name The string of parameter name.
     * @return The DsParams that represents the parameter.
     */
    public static DsParams FromString(String name)
    {
        DsParams item = null;
        boolean isFound = false;
        if (name != null)
        {
            for(int i = DsCommon.DAP1_PARAM_ID_START; i < DsCommon.DAP1_PARAM_ID_END - 1; i++)
            {
                if(DAP1_PARAM_NAMES[i - DsCommon.DAP1_PARAM_ID_START + 1].equalsIgnoreCase(name.trim()))
                {
                    item = params[i - DsCommon.DAP1_PARAM_ID_START];
                    isFound = true;
                    break;
                }
            }

            /*if (!isFound)
            {
                for(int n = DsCommon.DAP_CPDP_PARAM_ID_START; n < DsCommon.DAP_CPDP_PARAM_ID_END - 1; n++)
                {
                    if(DAP_CPDP_PARAM_NAMES[n - DsCommon.DAP_CPDP_PARAM_ID_START + 1].equals(name) )
                    {
                        item = params[n - DsCommon.DAP_CPDP_PARAM_ID_START];
                        break;
                    }
                }
            }*/
        }
        return item;
    }


    private static final String DAP1_PARAM_NAMES[] = {"null",
                                          "vdhe", "vspe", "dvle", "dvme", "ngon",
                                          "ieon", "deon", "geon", "dhsb", "dhrg",
                                          "dssb", "dssa", "dssf", "dvla", "iebt",
                                          "iea",  "dea",  "ded",  "gebg", "aoon",
                                          "plb",  "plmd", "vmon", "vmb",  "dvli",
                                          "dvlo", "dvmc", "ienb", "iebf", "genb",
                                          "gebf", "aonb", "aobf", "aobg", "arnb",
                                          "arbf", "aocc", "arbi", "arbl", "arbh",
                                          "arod", "artp"};
/*
    private static final String DAP_CPDP_PARAM_NAMES[] = {
        "null",
        "system-gain",
        "postgain",
        "pregain",
        "calibration-boost",
        "surround-decoder-enable",
        "dialog-enhancer-enable",
        "dialog-enhancer-amount",
        "dialog-enhancer-ducking",
        "volmax-boost",
        "volume-modeler-enable",
        "volume-modeler-calibration",
        "volume-leveler-enable",
        "volume-leveler-amount",
        "volume-leveler-in-target",
        "volume-leveler-out-target",
        "ieq-enable",
        "ieq-amount",
        "ieq-bands",
        "regulator-enable",
        "regulator-speaker-dist-enable",
        "regulator-relaxation-amount",
        "regulator-overdrive",
        "regulator-timbre-preservation",
        "regulator-tuning",
        "output-mode",
        "surround-boost",
        "virtualizer-speaker-start-freq",
        "virtualizer-speaker-angle",
        "virtualizer-headphone-reverb-gain",
        "graphic-equalizer-enable",
        "graphic-equalizer-bands",
        "bass-enhancer-enable",
        "bass-enhancer-boost",
        "bass-enhancer-cutoff-frequency",
        "bass-enhancer-width",
        "audio-optimizer-enable",
        "audio-optimizer-bands",
        "mi-surround-compressor-steering-enable",
        "mi-ieq-steering-enable",
        "mi-dv-leveler-steering-enable",
        "mi-dialog-enhancer-steering-enable",
        "vis-bands",
        "vis-custom-bands",
        "regulator-tuning-info",
        "mi-metadata",
        "downmix-config",
        "version"
    };
*/
    static private final DsParams params[] = {    
        DolbyHeadphoneVirtualizerControl,
        DolbyVirtualSpeakerVirtualizerControl,
        DolbyVolumeLevelerEnable,
        DolbyVolumeModelerEnable,
        NextGenSurroundEnable,
        IntelligentEqualizerEnable,
        DialogEnhancementEnable,
        GraphicEqualizerEnable,
        DolbyHeadphoneSurroundBoost,
        DolbyHeadphoneReverberationGain,
        DolbyVirtualSpeakerSurroundBoost,
        DolbyVirtualSpeakerAngle,
        DolbyVirtualSpeakerStartFrequency,
        DolbyVolumeLevelingAmount,
        IntelligentEqualizerBandTargets,
        IntelligentEqualizerAmount,
        DialogEnhancementAmount,
        DialogEnhancementDucking,
        GraphicEqualizerBandGains,
        AudioOptimizerEnable,
        PeakLimiterBoost,
        PeakLimitingProtectionMode,
        VolumeMaximizerEnable,
        VolumeMaximizerBoost,
        DolbyVolumeLevelerInputTarget,
        DolbyVolumeLevelerOutputTarget,
        DolbyVolumeModelerCalibration,
        IntelligentEqualizerBandCount,
        IntelligentEqualizerBandFrequencies,
        GraphicEqualizerBandCount,
        GraphicEqualizerBandFrequencies,
        AudioOptimizerBandCount,
        AudioOptimizerBandFrequencies,
        AudioOptimizerBandGains,
        AudioRegulatorBandCount,
        AudioRegulatorBandFrequencies,
        AudioOptimizerChannelCount,
        AudioRegulatorBandIsolates,
        AudioRegulatorBandLowThresholds,
        AudioRegulatorBandHighThresholds,
        AudioRegulatorOverdrive,
        AudioRegulatorTimbrePreservationAmount
/*        
        DAP_CPDP_SYSTEM_GAIN,
        DAP_CPDP_POSTGAIN,
        DAP_CPDP_PREGAIN,
        DAP_CPDP_CALIBRATION_BOOST,
        DAP_CPDP_SURROUND_DECODER_ENABLE,
        DAP_CPDP_DIALOG_ENHANCER_ENABLE,
        DAP_CPDP_DIALOG_ENHANCER_AMOUNT,
        DAP_CPDP_DIALOG_ENHANCER_DUCKING,
        DAP_CPDP_DIALOG_VOLMAX_BOOST,
        DAP_CPDP_VOLUME_MODELER_ENABLE,
        DAP_CPDP_VOLUME_MODELER_CALIBRATION,
        DAP_CPDP_VOLUME_LEVELER_ENABLE,
        DAP_CPDP_VOLUME_LEVELER_AMOUNT,
        DAP_CPDP_VOLUME_LEVELER_IN_TARGET,
        DAP_CPDP_VOLUME_LEVELER_OUT_TARGET,
        DAP_CPDP_IEQ_ENABLE,
        DAP_CPDP_IEQ_AMOUNT,
        DAP_CPDP_IEQ_BANDS,
        DAP_CPDP_REGULATOR_ENABLE,
        DAP_CPDP_REGULATOR_SPEAKER_DIST_ENABLE,
        DAP_CPDP_REGULATOR_RELAXATION_AMOUNT,
        DAP_CPDP_REGULATOR_OVERDRIVE,
        DAP_CPDP_REGULATOR_TIMBRE_PRESERVATION,
        DAP_CPDP_REGULATOR_TUNING,
        DAP_CPDP_OUTPUT_MODE,
        DAP_CPDP_SURROUND_BOOST,
        DAP_CPDP_VIRTUALIZER_SPEAKER_START_FREQ,
        DAP_CPDP_VIRTUALIZER_SPEAKER_ANGLE,
        DAP_CPDP_VIRTUALIZER_HEADPHONE_REVERB_GAIN,
        DAP_CPDP_GRAPHIC_EQUALIZER_ENABLE,
        DAP_CPDP_GRAPHIC_EQUALIZER_BANDS,
        DAP_CPDP_BASS_ENHANCER_ENABLE,
        DAP_CPDP_BASS_ENHANCER_BOOST,
        DAP_CPDP_BASS_ENHANCER_CUTOFF_FREQUENCY,
        DAP_CPDP_BASS_ENHANCER_WIDTH,
        DAP_CPDP_AUDIO_OPTIMIZER_ENABLE,
        DAP_CPDP_AUDIO_OPTIMIZER_BANDS,
        DAP_CPDP_MI_SURROUND_COMPRESSOR_STEERING_ENABLE,
        DAP_CPDP_MI_IEQ_STEERING_ENABLE,
        DAP_CPDP_MI_DV_LEVELER_STEERING_ENABLE,
        DAP_CPDP_MI_DIALOG_ENHANCER_STEERING_ENABLE,
        DAP_CPDP_VIS_BANDS,
        DAP_CPDP_VIS_CUSTOM_BANDS,
        DAP_CPDP_REGULATOR_TUNING_INFO,
        DAP_CPDP_MI_METADATA,
        DAP_CPDP_DOWNMIX_CONFIG,
        DAP_CPDP_VERSION
*/        
    };
}
