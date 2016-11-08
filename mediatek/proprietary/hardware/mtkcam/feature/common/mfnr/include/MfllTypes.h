#ifndef __MFLLTYPES_H__
#define __MFLLTYPES_H__

#include "MfllDefs.h"

#include <pthread.h> // pthread_mutex_t

namespace mfll {

    /* Mfll error code */
    enum MfllErr {
        MfllErr_Ok = 0,
        MfllErr_Shooted,
        MfllErr_AlreadyExist,
        MfllErr_NotInited,
        MfllErr_BadArgument,
        MfllErr_IllegalBlendFrameNum,
        MfllErr_IllegalCaptureFrameNum,
        MfllErr_NullPointer,
        MfllErr_NotImplemented,
        MfllErr_NotSupported,
        /* This error code indicates to instance creation is failed */
        MfllErr_CreateInstanceFailed,
        /* Load image failed */
        MfllErr_LoadImageFailed,
        /* Save image failed */
        MfllErr_SaveImageFailed,
        /* Others error will be categoried here */
        MfllErr_UnexpectedError,
        /* indicates to size only */
        MfllErr_Size,
    };

    /* Mfll mode */
    enum MfllMode {
        MfllMode_NormalMfll = 0,
        MfllMode_ZsdMfll,
        MfllMode_NormalAis,
        MfllMode_ZsdAis,
        /* indicates to size only */
        MfllMode_Size,
    };

    /* Image format that Mfll will invoke */
    enum ImageFormat {
        ImageFormat_Yuy2 = 0,
        ImageFormat_Raw10,
        ImageFormat_Yv16,
        ImageFormat_Y8,
        /* size */
        ImageFormat_Size
    };

    enum NoiseReductionType {
        NoiseReductionType_None = 0,
        NoiseReductionType_SWNR,
        NoiseReductionType_HWNR,
        /* size */
        NoiseReductionType_Size
    };

    /**
     *  MFLL provides an event classes, these are event that MFLL will invoked.
     *  If not specified param1 or param2, it will be 0.
     */
    enum EventType {
        EventType_Init = 0,
        EventType_AllocateRawBuffer, /* param1: integer, index of buffer */
        EventType_AllocateQyuvBuffer, /* param1: integer, index of buffer */
        EventType_AllocateYuvBase,
        EventType_AllocateYuvGolden,
        EventType_AllocateYuvWorking,
        EventType_AllocateYuvMixing,
        EventType_AllocateWeighting, /* param1: integer, index of weighting table */
        EventType_AllocateMemc, /* param1: integer, index of memc working buffer */
        EventType_AllocatePostview,
        EventType_AllocateThumbnail,
        EventType_AllocateJpeg,
        EventType_Capture, /* invoke when start capturing frame and all frames are captured */
        EventType_CaptureRaw, /* param1: integer, index of captured RAW */
        EventType_CaptureYuvQ, /* param1: integer, index of captured Q size Yuv */
        EventType_CaptureEis, /* param1: integer, index of captured Eis info */
        EventType_Bss,
        EventType_EncodeYuvBase,
        EventType_EncodeYuvGolden,
        EventType_MotionEstimation, /* param1: integer, index of buffer */
        EventType_MotionCompensation,
        EventType_Blending,
        EventType_Mixing,
        EventType_NoiseReduction,
        EventType_Postview,
        EventType_Thumbnail,
        EventType_EncodeJpeg,
        EventType_Destroy,
        /* size */
        EventType_Size,
    };

    enum MemcMode {
        MemcMode_Sequential = 0,
        MemcMode_Parallel,
        /* size */
        MemcMode_Size
    };

    /* RWB sensor support mode */
    enum RwbMode {
        RwbMode_None = 0,
        RebMode_Mdp,
        RwbMode_GPU,
        /* size */
        RwbMode_Size
    };

    /* Memory Reduce Plan mode */
    enum MrpMode {
        MrpMode_BestPerformance = 0,
        MrpMode_LowestMemory,
        MrpMode_Balance,
        /* size */
        MrpMode_Size
    };

    /* Describes Mfll feature options */
    typedef struct MfllFeatureOpt {
        enum MrpMode mrp_mode;
        enum RwbMode rwb_mode;
        enum MemcMode memc_mode;
        int dualphase_mode; // only works with Zsd
        MfllFeatureOpt (void)
        : mrp_mode ((enum MrpMode)MFLL_MRP_SUPPORT_MODE)
        , rwb_mode ((enum RwbMode)MFLL_RWB_SUPPORT_MODE)
        , memc_mode ((enum MemcMode)MFLL_MEMC_SUPPORT_MODE)
        , dualphase_mode ((int)MFLL_DUALPHASE_SUPPORT_MODE)
        {
        }
    } MfllFeatureOpt_t;

    /* sync object */
    typedef struct MfllSyncObj {
        pthread_mutex_t trigger;
        pthread_mutex_t done;
        MfllSyncObj(void)
        : trigger(PTHREAD_MUTEX_INITIALIZER)
        , done(PTHREAD_MUTEX_INITIALIZER)
        {
        }
    } MfllSyncObj_t;

    typedef struct MfllMotionVector {
        unsigned int x;
        unsigned int y;
        MfllMotionVector(void)
        : x (0)
        , y (0)
        {
        }
    } MfllMotionVector_t;

    typedef struct MfllEventStatus {
        int ignore;
        enum MfllErr err;

        MfllEventStatus(void)
        : ignore(0)
        , err(MfllErr_Ok)
        {}
    } MfllEventStatus_t;

    /* Mfll debug option */
    typedef struct MfllBypassOption {
        unsigned int bypassAllocRawBuffer[MFLL_MAX_FRAMES];
        unsigned int bypassAllocQyuvBuffer[MFLL_MAX_FRAMES];
        unsigned int bypassAllocYuvBase;
        unsigned int bypassAllocYuvGolden;
        unsigned int bypassAllocYuvWorking;
        unsigned int bypassAllocYuvMixing;
        unsigned int bypassAllocWeighting[2];
        unsigned int bypassAllocMemc[MFLL_MAX_FRAMES];
        unsigned int bypassAllocPostview;
        unsigned int bypassAllocThumbnail;
        unsigned int bypassAllocJpeg;
        unsigned int bypassCapture;
        unsigned int bypassBss;
        unsigned int bypassEncodeYuvBase;
        unsigned int bypassEncodeYuvGolden;
        unsigned int bypassMotionEstimation[MFLL_MAX_FRAMES];
        unsigned int bypassMotionCompensation[MFLL_MAX_FRAMES];
        unsigned int bypassBlending[MFLL_MAX_FRAMES];
        unsigned int bypassMixing;
        unsigned int bypassNoiseReduction;
        unsigned int bypassPostview;
        unsigned int bypassThumbnail;
        unsigned int bypassEncodeJpeg;
        MfllBypassOption(void)
        : bypassAllocYuvBase(MFLL_DEBUG_BYPASS_ALLOCATE_YUV_BASE)
        , bypassAllocYuvGolden(MFLL_DEBUG_BYPASS_ALLOCATE_YUV_GOLDEN)
        , bypassAllocYuvWorking(MFLL_DEBUG_BYPASS_ALLOCATE_YUV_WORKING)
        , bypassAllocYuvMixing(MFLL_DEBUG_BYPASS_ALLOCATE_YUV_MIXING)
        , bypassAllocPostview(MFLL_DEBUG_BYPASS_ALLOCATE_POSTVIEW)
        , bypassAllocThumbnail(MFLL_DEBUG_BYPASS_ALLOCATE_THUMBNAIL)
        , bypassAllocJpeg(MFLL_DEBUG_BYPASS_ALLOCATE_JPEG)
        , bypassCapture(MFLL_DEBUG_BYPASS_CAPTURE)
        , bypassBss(MFLL_DEBUG_BYPASS_BSS)
        , bypassEncodeYuvBase(MFLL_DEBUG_BYPASS_ENCODE_YUV_BASE)
        , bypassEncodeYuvGolden(MFLL_DEBUG_BYPASS_ENCODE_YUV_GOLDEN)
        , bypassMixing(MFLL_DEBUG_BYPASS_MIXING)
        , bypassNoiseReduction(MFLL_DEBUG_BYPASS_NOISE_REDUCTION)
        , bypassPostview(MFLL_DEBUG_BYPASS_POSTVIEW)
        , bypassThumbnail(MFLL_DEBUG_BYPASS_THUMBNAIL)
        , bypassEncodeJpeg(MFLL_DEBUG_BYPASS_ENCODE_JPEG)
        {
            bypassAllocWeighting[0] = MFLL_DEBUG_BYPASS_ALLOCATE_WEIGHTING;
            bypassAllocWeighting[1] = MFLL_DEBUG_BYPASS_ALLOCATE_WEIGHTING;
            for (int i = 0; i < MFLL_MAX_FRAMES; i++) {
                bypassAllocRawBuffer[i] = MFLL_DEBUG_BYPASS_ALLOCATE_RAW_BUFFER;
                bypassAllocQyuvBuffer[i] = MFLL_DEBUG_BYPASS_ALLOCATE_QYUV_BUFFER;
                bypassAllocMemc[i] = MFLL_DEBUG_BYPASS_ALLOCATE_MEMC;
                bypassMotionEstimation[i] = MFLL_DEBUG_BYPASS_MOTION_ESTIMATION;
                bypassMotionCompensation[i] = MFLL_DEBUG_BYPASS_MOTION_COMPENSATION;
                bypassBlending[i] = MFLL_DEBUG_BYPASS_BLENDING;
            }
        }
    } MfllBypassOption_t;

    /* configuration of Mfll */
    typedef struct MfllConfig {
        int             iSensorId;
        int             iExposure;
        int             iIso;
        unsigned int    uiUpdateAe;
        unsigned char   ucMfbNum; // Multi-Frame Blending frame
        enum MfllMode   eMfllMode;
    } MfllConfig_t;

}; /* namespace mfll */
#endif /* __MFLLTYPES_H__ */
