#ifndef __PQDSIMPL_H__
#define __PQDSIMPL_H__


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*
* header files
*/

#define DSHP_ANDROID_PLATFORM

#define DYN_SHARP_VERSION 0

typedef unsigned int uint32_t;
typedef unsigned short uint16_t;
typedef unsigned char uint8_t;
typedef signed int int32_t;

// Initial register values to DS HW
struct DSInitReg {
    uint32_t useless; // To pass compilation
};

struct DSHWReg {
    uint16_t tdshp_gain_high;
    uint16_t tdshp_gain_mid;

    uint16_t tdshp_coring_zero;
    uint16_t tdshp_coring_thr;
    uint16_t tdshp_softcoring_gain;
    uint16_t tdshp_coring_value;
    uint16_t tdshp_gain;
#if DYN_SHARP_VERSION == 0
#else
    uint16_t tdshp_high_coring_zero;
    uint16_t tdshp_high_coring_thr;
    uint16_t tdshp_high_softcoring_gain;
    uint16_t tdshp_high_coring_value;
    uint16_t tdshp_mid_coring_zero;
    uint16_t tdshp_mid_coring_thr;
    uint16_t tdshp_mid_softcoring_gain;
    uint16_t tdshp_mid_coring_value;

    uint16_t edf_flat_th;
    uint16_t edf_detail_rise_th;
#endif
};

// Fields collected from DS HW
struct DSInput {
    uint16_t SrcWidth;
    uint16_t SrcHeight;
    uint16_t DstWidth;
    uint16_t DstHeight;

    uint8_t VideoImgSwitch; // 0: Video, 1: Image

    uint16_t inISOSpeed;
    struct DSHWReg iHWReg;
};

// Fields which will be set to HW registers
struct DSOutput {
    struct DSHWReg iHWReg;
};

// DS FW registers
struct DSReg {
    int32_t DS_en;

    int32_t iUpSlope;        // Range from -64 to 63
    int32_t iUpThreshold;    // Range from 0 to 4095
    int32_t iDownSlope;      // Range from -64 to 63
    int32_t iDownThreshold;  // Range from 0 to 4095

    uint16_t iISO_en;
    uint16_t iISO_thr1;
    uint16_t iISO_thr0;
    uint16_t iISO_thr3;
    uint16_t iISO_thr2;
    uint16_t iISO_IIR_alpha;

    int32_t iCorZero_clip2; //Range from -255 to 255
    int32_t iCorZero_clip1; //Range from -255 to 255
    int32_t iCorZero_clip0; //Range from -255 to 255
    int32_t iCorThr_clip2; //Range from -255 to 255
    int32_t iCorThr_clip1; //Range from -255 to 255
    int32_t iCorThr_clip0; //Range from -255 to 255
    int32_t iCorGain_clip2; //Range from -255 to 255
    int32_t iCorGain_clip1; //Range from -255 to 255
    int32_t iCorGain_clip0; //Range from -255 to 255
    int32_t iGain_clip2; //Range from -255 to 255
    int32_t iGain_clip1; //Range from -255 to 255
    int32_t iGain_clip0; //Range from -255 to 255
#if DYN_SHARP_VERSION == 0
#else
    int32_t iHighCorZero_clip2; //Range from -255 to 255
    int32_t iHighCorZero_clip1; //Range from -255 to 255
    int32_t iHighCorZero_clip0; //Range from -255 to 255
    int32_t iHighCorThr_clip2; //Range from -255 to 255
    int32_t iHighCorThr_clip1; //Range from -255 to 255
    int32_t iHighCorThr_clip0; //Range from -255 to 255
    int32_t iHighCorGain_clip2; //Range from -255 to 255
    int32_t iHighCorGain_clip1; //Range from -255 to 255
    int32_t iHighCorGain_clip0; //Range from -255 to 255

    int32_t iMidCorZero_clip2; //Range from -255 to 255
    int32_t iMidCorZero_clip1; //Range from -255 to 255
    int32_t iMidCorZero_clip0; //Range from -255 to 255
    int32_t iMidCorThr_clip2; //Range from -255 to 255
    int32_t iMidCorThr_clip1; //Range from -255 to 255
    int32_t iMidCorThr_clip0; //Range from -255 to 255
    int32_t iMidCorGain_clip2; //Range from -255 to 255
    int32_t iMidCorGain_clip1; //Range from -255 to 255
    int32_t iMidCorGain_clip0; //Range from -255 to 255

    int32_t i_edf_flat_th_clip2; //Range from -255 to 255
    int32_t i_edf_flat_th_clip1; //Range from -255 to 255
    int32_t i_edf_flat_th_clip0; //Range from -255 to 255
    int32_t i_edf_detail_rise_th_clip2; //Range from -255 to 255
    int32_t i_edf_detail_rise_th_clip1; //Range from -255 to 255
    int32_t i_edf_detail_rise_th_clip0; //Range from -255 to 255
#endif
};


///////////////////////////////////////////////////////////////////////////////
// DS FW Processing class
///////////////////////////////////////////////////////////////////////////////
class CPQDSFW
{

    /* ........Dynamic Sharpness Process, functions......... */
public:

#ifdef DSHP_ANDROID_PLATFORM
    void onCalculate(const DSInput *input, DSOutput *output);
    void onInitPlatform(void);
#else
    void vDrvDSProc_int(const DSInput * input, DSOutput * output);
#endif

    void DSInitialize(void);
    int32_t IsoAdpGetReg(
                  int32_t in_iso,
                  int32_t in_value,
                  int32_t reg_iso_thr0,
                  int32_t reg_iso_thr1,
                  int32_t reg_iso_thr2,
                  int32_t reg_iso_thr3,
                  int32_t reg_clip1,
                  int32_t reg_clip2,
                  int32_t reg_clip3,
                  int32_t reg_min,
                  int32_t reg_max
                  );
    int32_t IsoAdpGetCorValue(int32_t zero_thr, int32_t coring_thr, int32_t gain, int32_t cor_gain);
    int32_t IsoAdpAlphaBlending(int32_t data1, int32_t data2, int32_t alpha, int32_t bits);

    CPQDSFW();

    ~CPQDSFW();

private:

    /* ........Dynamic Sharpness Process, variables......... */
public:
    DSReg * pDSReg;

private:
    DSInput oldInput;
    DSOutput oldOutput;

    uint16_t iISOSpeedIIR;
};

#endif //__PQDSIMPL_H__
