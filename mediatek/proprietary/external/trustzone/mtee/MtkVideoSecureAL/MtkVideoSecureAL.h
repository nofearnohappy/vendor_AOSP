#ifndef __MTK_VIDEO_SECURE_AL__
#define __MTK_VIDEO_SECURE_AL__
typedef unsigned long long  ULONG_DUMMY_T;
//#define BIN_SEC_SELF_TEST 1

typedef enum
{
    MTK_SECURE_AL_FAIL      = 0,
    MTK_SECURE_AL_SUCCESS   = 1,
} MTK_SECURE_AL_RESULT;

#if 0
typedef struct _VdecH264SecInitStruct {
    void* mem;
    int size;
} VdecH264SecInitStruct;
#else
typedef struct _VdecH264SecMemInfoStruct {
    void* mem;
    int size;
} VdecH264SecMemInfoStruct, VideoH264SecMemInfoStruct;

typedef struct _VdecH265SecMemInfoStruct {
    void* mem;
    int size;
} VdecH265SecMemInfoStruct, VideoH265SecMemInfoStruct;

typedef struct _VdecH264SecInitStruct {
    unsigned long vdec_h264_drv_data_share_handle;
    unsigned long vdec_h264_instanceTemp_share_handle;
    unsigned long h264_DEC_PRM_DataInst_share_handle;
    unsigned long bitstream_va_share_handle;   // UT only
    unsigned long bitstream_secure_handle;
    unsigned long bitstream_length;   // valid size of bitstream
} VdecH264SecInitStruct;

// test only
typedef struct _VdecFrameDumpStruct {
    unsigned long frame_secure_handle;
    unsigned long frame_va_share_handle;
} VdecFrameDumpStruct;
#endif


/*
    void* mem1;    // pVdec_H264_InstanceInst
    int size1;          // sizeof(Vdec_H264_InstanceInst)

    void* mem2;   // pH264_DEC_PRM_DataInst
    int size2;         // sizeof(H264_DEC_PRM_DataInst)
*/
typedef struct _VdecH264SecDecStruct {
    unsigned long pVdec_H264_InstanceTemp_share_handle;   // share memory handle of pVdec_H264_InstanceTemp
    unsigned long pH264_DEC_PRM_DataInst_share_handle;    // share memory handle of pH264_DEC_PRM_DataInst

    unsigned long va1;         // pBitstream
    unsigned long handle1;  // handle of pBitstream
    unsigned long bitstream_length;   // valid size of bitstream

    unsigned long va2;         // pFrame
    unsigned long handle2;  // handle of pFrame

    unsigned long va3;    //  &pVdec_H264_InstanceInst->mCurrentBitstream

    unsigned long va4;         //  pVdec_H264_InstanceInst->pCurrBitstream
    unsigned long handle4;  // handle of pVdec_H264_InstanceInst->pCurrBitstream

    unsigned long handle5;  // ringbuffer VA share handle, for UT only
    unsigned long handle6;  // framebuffer VA share handle, for UT only
} VdecH264SecDecStruct;


typedef struct _VdecH264SecDeinitStruct {
    unsigned long pVdec_H264_InstanceTemp_share_handle;   // share memory handle of pVdec_H264_InstanceTemp
    unsigned long pH264_DEC_PRM_DataInst_share_handle;    // share memory handle of pH264_DEC_PRM_DataInst
    unsigned long pVdec_H264_Drv_dataInst_share_handle;    // share memory handle of pVdec_H264_Drv_dataInst
    unsigned long pDecStruct2_share_handle;                            // share memory handle of pDecStruct2
} VdecH264SecDeinitStruct;


typedef struct _VdecH265SecInitStruct {
    uint32_t vdec_h265_drv_data_share_handle;
    uint32_t vdec_h265_instanceTemp_share_handle;
    uint32_t h265_DEC_PRM_DataInst_share_handle;
    uint32_t bitstream_va_share_handle;
    uint32_t bitstream_secure_handle;
    uint32_t bitstream_length;
} VdecH265SecInitStruct;

typedef struct _VdecH265SecDecStruct {
    uint32_t pVdec_H265_InstanceTemp_share_handle; // share memory handle of pVdec_H265_InstanceTemp
    uint32_t pH265_DEC_PRM_DataInst_share_handle;  // share memory handle of pH265_DEC_PRM_DataInst

    uint32_t va1;              // pBitstream
    uint32_t handle1;          // handle of pBitstream
    uint32_t bitstream_length; // valid size of bitstream

    uint32_t va2;      // pFrame
    uint32_t handle2;  // handle of pFrame

    uint32_t va3;      // &pVdec_H264_InstanceInst->mCurrentBitstream

    uint32_t va4;      //  pVdec_H265_InstanceInst->pCurrBitstream
    uint32_t handle4;  // handle of pVdec_H265_InstanceInst->pCurrBitstream

    uint32_t handle5;  // ringbuffer VA share handle, for UT only
    uint32_t handle6;  // framebuffer VA share handle, for UT only
} VdecH265SecDecStruct;

typedef struct _VdecH265SecDeinitStruct {
    uint32_t pVdec_H265_InstanceTemp_share_handle;   // share memory handle of pVdec_H262_InstanceTemp
    uint32_t pH265_DEC_PRM_DataInst_share_handle;    // share memory handle of pH262_DEC_PRM_DataInst
    uint32_t pVdec_H265_Drv_dataInst_share_handle;   // share memory handle of pVdec_H262_Drv_dataInst
    uint32_t pDecStruct2_share_handle;               // share memory handle of pDecStruct2
} VdecH265SecDeinitStruct;

// secure memory allocation related apis
MTK_SECURE_AL_RESULT MtkVideoSecureMemAllocatorInit();
MTK_SECURE_AL_RESULT MtkVideoSecureMemAllocatorDeinit();
unsigned long MtkVideoAllocateSecureBuffer(int size, int align);
MTK_SECURE_AL_RESULT MtkVideoFreeSecureBuffer(unsigned long memHandle);
unsigned long MtkVideoAllocateSecureFrameBuffer(int size, int align);
MTK_SECURE_AL_RESULT MtkVideoFreeSecureFrameBuffer(unsigned long memHandle);
unsigned long MtkVideoRegisterSharedMemory(void* buffer, int size);
MTK_SECURE_AL_RESULT MtkVideoUnregisterSharedMemory(unsigned long sharedHandle);

/* Begin added by mtk09845 UT_TEST_DEV  */
typedef struct _VdecFillSecMemStruct
{
    uint32_t bs_share_handle;
    uint32_t bs_secure_handle;
    uint32_t bs_size;
    uint32_t direction; //0: to mtee, 1: from mtee
} VdecFillSecMemStruct;

unsigned long MtkVideoAllocateSecureBitstreamBuffer(int size, int align);
unsigned long MtkVdecFillSecureBuffer(uint32_t bs_sec_handle, void *bs_va, uint32_t size, uint32_t bdirection);
/* End added by mtk09845 UT_TEST_DEV  */

// video decoding related apis
MTK_SECURE_AL_RESULT MtkVdecH264SecInit(VdecH264SecMemInfoStruct* pInitStruct, unsigned long bitstreamMemHandle, unsigned long* Vdec_H264_InstanceTemp_share_handle, unsigned long* H264_DEC_PRM_DataInst_share_handle, unsigned long* H264_Drv_data_share_handle, unsigned long* DecStruct2_share_handle, unsigned int* pH264_Sec_session);
MTK_SECURE_AL_RESULT MtkVdecH264SecDeinit(unsigned int H264_Sec_session, VdecH264SecDeinitStruct* pDeinitStruct);

MTK_SECURE_AL_RESULT MtkVdecH264SecDecode(unsigned int H264_Sec_session, VdecH264SecMemInfoStruct* pDecStruct1, unsigned long pDecStruct2_share_handle);

MTK_SECURE_AL_RESULT MtkVdecH265SecInit(VdecH265SecMemInfoStruct* pInitStruct, unsigned int bitstreamMemHandle, unsigned int* Vdec_H264_InstanceTemp_share_handle, unsigned int* H264_DEC_PRM_DataInst_share_handle, unsigned int* H264_Drv_data_share_handle, unsigned int* DecStruct2_share_handle, unsigned int* pH265_Sec_session);

MTK_SECURE_AL_RESULT MtkVdecH265SecDeinit(unsigned int H265_Sec_session, VdecH265SecDeinitStruct* pDeinitStruct);

MTK_SECURE_AL_RESULT MtkVdecH265SecDecode(unsigned int H265_Sec_session, VdecH265SecMemInfoStruct* pDecStruct1, unsigned long pDecStruct2_share_handle);

// test
MTK_SECURE_AL_RESULT MtkVdecH264SecInitTest(VdecFrameDumpStruct* pFrameDumpInfo);

//#if defined(MTK_IN_HOUSE_TEE_SUPPORT) && defined(MTK_SEC_VIDEO_PATH_SUPPORT)

//Sync with TA venc\h264\encode.h
typedef struct _VencH264SecInitStruct {
    union
      {
         unsigned long venc_h264_drv_data_share_handle;
         ULONG_DUMMY_T venc_h264_drv_data_share_handle_dummy;
      };
    union
      {
         unsigned long bitstream_va_share_handle;   // UT only
         ULONG_DUMMY_T bitstream_va_share_handle_dummy;   // UT only
      };
    union
      {
         unsigned long bitstream_secure_handle;
         ULONG_DUMMY_T bitstream_secure_handle_dummy;
      };
    union
      {
         unsigned long bitstream_length;   // valid size of bitstream
         ULONG_DUMMY_T bitstream_length_dummy;   // valid size of bitstream
      };
    union
      {
         unsigned long frame_va_share_handle;   // UT only
         ULONG_DUMMY_T frame_va_share_handle_dummy;   // UT only
      };
    union
      {
         unsigned long frame_secure_handle;
         ULONG_DUMMY_T frame_secure_handle_dummy;
      };
} VencH264SecInitStruct;

typedef struct _VencH264SecShareWorbufStruct
{
      union
      {
         unsigned long venc_h264_drv_data_share_handle;
         ULONG_DUMMY_T venc_h264_drv_data_share_handle_dummy;
      };

      union
      {
          unsigned long venc_H264_RCInfo_share_handle;
          ULONG_DUMMY_T  venc_H264_RCInfo_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_RCCode_share_handle;
          ULONG_DUMMY_T  venc_H264_RCCode_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_RecLuma_share_handle;
          ULONG_DUMMY_T  venc_H264_RecLuma_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_RecChroma_share_handle;
          ULONG_DUMMY_T  venc_H264_RecChroma_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_RefLuma_share_handle;
          ULONG_DUMMY_T  venc_H264_RefLuma_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_RefChroma_share_handle;
          ULONG_DUMMY_T  venc_H264_RefChroma_share_handle_dummy;
      };
      union
      {
         unsigned long venc_H264_MVInfo1_handle;
         ULONG_DUMMY_T  venc_H264_MVInfo1_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_MVInfo2_handle;
          ULONG_DUMMY_T  venc_H264_MVInfo2_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_PPSTemp_share_handle;
          ULONG_DUMMY_T  venc_H264_PPSTemp_share_handle_dummy;
      };
      union
      {
          unsigned long venc_H264_IDRTemp_share_handle;
          ULONG_DUMMY_T  venc_H264_IDRTemp_share_handle_dummy;
      };
} VencH264SecShareWorbufStruct;

typedef struct _VencH264SecCmdStruct {
     union
      {
         unsigned long venc_h264_drv_data_share_handle;
         ULONG_DUMMY_T venc_h264_drv_data_share_handle_dummy;
      };
} VencH264SecCmdStruct;

typedef struct _VencH264SecEncStruct {
      union
      {
          unsigned long venc_h264_drv_data_share_handle;
          ULONG_DUMMY_T venc_h264_drv_data_share_handle_dummy;
      };
      union
      {
          unsigned long encode_option;
          ULONG_DUMMY_T encode_option_dummy;
      };
      union
      {
          unsigned long va1;         // pFrame
          ULONG_DUMMY_T va1_dummy;
      };
      union
      {
          unsigned long handle1;  // handle of pFrame
          ULONG_DUMMY_T handle1_dummy;
      };
      union
      {
          unsigned long va2;         // pBitstream
          ULONG_DUMMY_T va2_dummy;
      };
      union
      {
          unsigned long handle2;  // handle of pBitstream
          ULONG_DUMMY_T handle2_dummy;
      };
      union
      {
          unsigned long handle3;  // handle of presult
          ULONG_DUMMY_T handle3_dummy;
      };
} VencH264SecEncStruct;

MTK_SECURE_AL_RESULT MtkVencH264SecInit(VideoH264SecMemInfoStruct* pInitStruct, unsigned long* H264_Drv_data_share_handle);
MTK_SECURE_AL_RESULT MtkVencH264SecDeinit(unsigned long H264_Drv_data_share_handle);
MTK_SECURE_AL_RESULT MtkVencH264SecEncode(unsigned long H264_Drv_data_share_handle, VideoH264SecMemInfoStruct* pEncStruct, VideoH264SecMemInfoStruct* pParStruct, unsigned long EncodeOption, unsigned char fgSecBuffer);
MTK_SECURE_AL_RESULT MtkVencH264SecAllocWorkBuf(unsigned long H264_Drv_data_share_handle);
MTK_SECURE_AL_RESULT MtkVencH264SecFreeWorkBuf(unsigned long H264_Drv_data_share_handle);
MTK_SECURE_AL_RESULT MtkVencH264SecShareWorkBuf(unsigned long H264_Drv_data_share_handle, VideoH264SecMemInfoStruct* pCmdStruct, unsigned long* pShareHandle);
MTK_SECURE_AL_RESULT MtkVencH264SecUnshareWorkBuf(unsigned long H264_Drv_data_share_handle, unsigned long* pUnsharBufHandle);
MTK_SECURE_AL_RESULT MtkVencH264SecCopyWorkBuf(unsigned long H264_Drv_data_share_handle);
//#endif

#endif /* __MTK_VDEC_SECURE_AL__ */
