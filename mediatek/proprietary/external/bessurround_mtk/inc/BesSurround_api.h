#ifndef VS_API_H
#define VS_API_H

typedef enum
{
    HEADPHONE = 0,
    MONO_SPEAKER,
    SPEAKER,
    OTHER
} MTK_VS_DEVICE;

typedef enum
{
    VS_MOVIE = 0,
    VS_MUSIC,
    NUM_MODE
} MTK_VS_MODE;

typedef enum
{
    WIDE = 0,
    MID,
    NARROW,
    NUM_REV_DIS
} MTK_VS_REV_DIS;

typedef enum
{
    MIDDLE = 0,
    HIGH,
    ALL
} MTK_VS_REV_BAND;

typedef enum
{
    REV_LPF_ON = 0,
    REV_LPF_OFF
} MTK_VS_REV_LPF;

typedef enum
{
    SUR_ENH_OFF = 0,
    SUR_ENH_ON = 1
} MTK_VS_SUR_ENH;

typedef enum
{
    NORMAL = 0,
    BYPASS_REV,
    BYPASS_HRTF,
    BYPASS_REV_HRTF,
    BYPASS_ALL
} MTK_VS_DEBUG;

typedef struct
{
    // for reverb
    short rev_weight_f[NUM_MODE];
    short rev_weight_s[NUM_MODE];
    MTK_VS_REV_DIS rev_dis[NUM_MODE];
    MTK_VS_REV_BAND rev_band[NUM_MODE];
    MTK_VS_REV_LPF rev_lpf[NUM_MODE];

    // for up/down mix
    MTK_VS_SUR_ENH sur_enh[NUM_MODE];
    short sur_weight[NUM_MODE];
    short ori_weight[NUM_MODE];
    short output_gain[NUM_MODE];

    // for debug
    MTK_VS_DEBUG debug;

} MTK_VS_PARAM;

typedef struct
{
    // common info
    int channel_mask;
    int sampling_rate;
    int valid_bit;

    // device info
    MTK_VS_DEVICE device;

    // param
    MTK_VS_PARAM param;

} MTK_VS_INFO;

typedef enum
{
    SUCCESS = 0,
    FAIL
} MTK_VS_RESULT;

typedef enum
{
    RAMP_DOWN,
    RAMP_NORMAL,
    RAMP_UP
} MTK_VS_RAMP_STATUS;


MTK_VS_RESULT mtk_vs_get_memsize(int *hdl_size, int *temp_buffer_size);
MTK_VS_RESULT mtk_vs_init(void *vs_hdl, MTK_VS_INFO *vs_info, void *temp_buffer);
int mtk_vs_process(void *vs_hdl, int *in_buffer, int *out_buffer, int num_sample);
MTK_VS_RESULT mtk_vs_set_mode(void *vs_hdl, MTK_VS_MODE mode);
MTK_VS_RESULT mtk_vs_reset(void *vs_hdl);
MTK_VS_RESULT mtk_vs_ramp_down(void *vs_hdl);
MTK_VS_RESULT mtk_vs_ramp_up(void *vs_hdl);
MTK_VS_RAMP_STATUS mtk_vs_get_ramp_status(void *vs_hdl);
MTK_VS_RESULT mtk_vs_query_channel_support(unsigned int channel_mask);

#endif
