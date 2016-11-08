#ifndef __FEATURE_TUNING_PARA_H__
#define __FEATURE_TUNING_PARA_H__

{ 
    max_frame_number            :   4,
    bss_clip_th                 :   8,
    memc_bad_mv_range           :   32,
    memc_bad_mv_rate_th         :   48,
    //
    mfll_iso_th                 :   400,
    //
    ais_exp_th                  :   33000,
    ais_advanced_tuning_en      :   1,
    ais_advanced_max_iso        :   2410,
    ais_advanced_max_exposure   :   66000,
    //
    reserved                    :   {
                                    33000,  //ais_exp_th0, default=16667 us, must < ais_exp_th
                                    1200,   //ais_iso_th0, default=1200, must < ais_advanced_max_iso    
                                    },  
},
{
    0,
    0,
    0,
    64,
    64,
    0
},
#endif // __FEATURE_TUNING_PARA_H__
