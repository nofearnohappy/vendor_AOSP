/******************************************************************************
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2013-2014 by Dolby International AB.
 *                            All rights reserved.
 *
 *    Module:     Object Audio Metadata Interface (OAMDI)
 *
 *    File:       oamdi.h
 ******************************************************************************/

/**
 * @defgroup OAMDI_API Object Audio Metadata Interface API Reference
 *
 * OAMDI is a shared software component that serves as an interface 
 * for the processing of object audio metadata between a common 
 * metadata structure and serialized payloads for the Evolution Framework. 
 * It is used across the object audio ecosystem and operates on 
 * the content creation side as well as on the playback side 
 *
 * ## Usage Recommendations
 * The component provides functions for querying the memory size
 * needed to allocate an instance of #oamdi. It also provides 
 * functions to initialize and duplicate an instance.
 *
 * ### Initialization 
 *
 *         \code
 *          oamdi           *p_oamdi = NULL;
 *          void            *p_mem   = NULL;   
 *          oamdi_init_info  oamdi_config;
 *          size_t           size = 0;
 *          ...
 *          oamdi_config.frame_size         = frame_size;
 *          oamdi_config.max_num_objs       = max_num_objs;
 *          oamdi_config.max_num_md_updates = max_num_md_updates;
 *
 *          size  = oamdi_query_mem(&oamdi_config);
 *          p_mem = (unsigned char *)malloc(size);
 *          if (p_mem)
 *          {
 *             p_oamdi = oamdi_init(&oamdi_config, (void *)p_mem);              
 *          }
 *         \endcode
 *
 * ### Duplication
 * To copy an instance of #oamdi, you can use oamdi_duplicate()
 *
 *         \code
 *          oamdi           *p_oamdi_src = NULL;
 *          oamdi           *p_oamdi_dst = NULL; 
 *          oamdi_init_info  oamdi_config_dst;
 *          size_t           size = 0;
 *          void            *p_mem_dst = NULL;
 *          ...
 *          oamdi_get_init_info(p_oamdi_src, &oamdi_config_dst);
 *          size        = oamdi_query_mem(&oamdi_config_dst);
 *          p_mem_dst   = malloc(size);
 *          if (p_mem_dst)
 *          {
 *              p_oamdi_dst = oamdi_duplicate(&oamdi_config_dst, p_mem_dst, p_oamdi_src); 
 *          }
 *         \endcode
 *
 * or memcpy and oamdi_validate_after_copy()
 * 
 *         \code
 *          memcpy((void *)p_oamdi_dst, (void *)p_oamdi_src, size);
 *          oamdi_validate_after_copy(p_oamdi_dst);
 *         \endcode
 *
 * ### Serialization
 * Before serialization, oamdi structure MUST be properly initialized.
 * User MUST set program assignment and metadata update info.
 * User SHOULD set metadat in object info blocks of desired objects.
 * Only objects indicated in program assignment will be serialized.
 * Only object info blocks indicated by num_obj_info_blks in md_update_info structure.
 * will be serialized.
 *
 *         \code
 *          oamdi_prog_assign     prog_assign;
 *          oamdi_md_update_info  md_update;
 *          oamdi_obj_info_blk    info_blk;
 *          size_t                size = 0;
 *          unsigned char        *p_bs = NULL;
 *          oamdi                 p_oamdi = NULL;
 *          ...
 *          // Set program assignment 
 *          prog_assign.num_dyn_objs      = num_dyn_objs;
 *          prog_assign.num_bed_instances = num_bed_instances;
 *          prog_assign.a_beds[0] = OAMDI_BIT_MASK_BED_L
 *                                | OAMDI_BIT_MASK_BED_R
 *                                | OAMDI_BIT_MASK_BED_C
 *                                | OAMDI_BIT_MASK_BED_LFE
 *                                | OAMDI_BIT_MASK_BED_LS
 *                                | OAMDI_BIT_MASK_BED_RS;
 *          ...
 *          oamdi_set_prog_assign(p_oamdi_src, &prog_assign); 
 * 
 *          // Set metadata update info
 *          md_update.sample_offset                          = sample_offset;
 *          md_update.num_obj_info_blks                      = num_obj_info_blks;
 *          md_update.a_blk_update_info[0].blk_offset_factor = blk_offset_factor0;
 *          md_update.a_blk_update_info[0].ramp_duration     = ramp_duration0;
 *          ...
 *          oamdi_set_md_update_info(p_oamdi_src, &md_update);
 *          
 *          // Set basic (and render if dynamic object) info blocks' metadata
 *          info_blk.basic_info.gain            = gain;
 *          info_blk.basic_info.priority        = priority;
 *          ...
 *          oamdi_set_obj_info_blk(p_oamdi_src, OBJECT_ID, INFO_BLK_IDX, &info_blk);
 *         
 *          size = oamdi_get_bitstream_size(p_oamdi_src);
 *          p_bs = (unsigned char *)malloc(size);
 *          if (p_bs)
 *          {
 *              size = oamdi_to_bitstream(p_oamdi, size, p_bs);
 *          }
 *         \endcode
 *
 *### Deserialization
 * Deserialization MIGHT be preceded with a call to oamdi_get_init_info_from_bitstream()
 * to get minimal requirements for the oamdi instance data are deserialized to.
 * 
 *         \code
 *          // p_bs points to valid oamdi bitstream
 *          // of bs_size size 
 *          ...
 *          // This call is optional
 *          err = oamdi_get_init_info_from_bitstream
 *                (p_bs
 *                ,size                      
 *                ,&num_objs          
 *                ,&num_obj_info_blks 
 *                ); 
 * 
 *          // oamdi initialization if necessary
 *          ...
 *          // Deserialization
 *          err = oamdi_from_bitstream(p_oamdi, bs_size, p_bs);
 *         \endcode       
 */
/*@{*/
/**
 * @file   oamdi.h
 * @brief  Object Audio Metadata Interface (OAMDI) API
 */

#ifndef OAMDI_H
#define OAMDI_H

#include <stddef.h> /* size_t */

#ifdef __cplusplus
extern "C"
{
#endif
/* @{ */
/**
 * @name Library version info
 */
#define OAMDI_LIB_V_API  (1) /**< @brief API version. */
#define OAMDI_LIB_V_FCT  (1) /**< @brief Functional change. */
#define OAMDI_LIB_V_MTNC (4) /**< @brief Maintenance release. */
/* @} */
/* @{ */
/**
 * @name Object audio metadata version info
 */
#define OAMDI_MD_V_MAJ (1) /**< @brief Major version. */
#define OAMDI_MD_V_MIN (0) /**< @brief Minor version. */
/* @} */

#define OAMDI_MAX_OBJ_INFO_BLKS     (8)
#define OMADI_MAX_NUM_BED_INSTANCES (9)

/* @{ */
/**
 * @name Bed objects' channel assignment
 */
#define OAMDI_BIT_MASK_BED_L    (0x00001ul) /**< @brief Left channel. */
#define OAMDI_BIT_MASK_BED_R    (0x00002ul) /**< @brief Right channel. */
#define OAMDI_BIT_MASK_BED_C    (0x00004ul) /**< @brief Left channel. */
#define OAMDI_BIT_MASK_BED_LFE  (0x00008ul) /**< @brief Low-frequency effects. */
#define OAMDI_BIT_MASK_BED_LS   (0x00010ul) /**< @brief Left surround. */
#define OAMDI_BIT_MASK_BED_RS   (0x00020ul) /**< @brief Right surround. */
#define OAMDI_BIT_MASK_BED_LRS  (0x00040ul) /**< @brief Left rear surround. */
#define OAMDI_BIT_MASK_BED_RRS  (0x00080ul) /**< @brief Right rear surround. */
#define OAMDI_BIT_MASK_BED_LFH  (0x00100ul) /**< @brief Left front height. */
#define OAMDI_BIT_MASK_BED_RFH  (0x00200ul) /**< @brief Right front height. */
#define OAMDI_BIT_MASK_BED_LTM  (0x00400ul) /**< @brief Left top middle. */
#define OAMDI_BIT_MASK_BED_RTM  (0x00800ul) /**< @brief Right top middle. */
#define OAMDI_BIT_MASK_BED_LRH  (0x01000ul) /**< @brief Left top middle. */
#define OAMDI_BIT_MASK_BED_RRH  (0x02000ul) /**< @brief Right rear height. */
#define OAMDI_BIT_MASK_BED_LFW  (0x04000ul) /**< @brief Left front wide. */
#define OAMDI_BIT_MASK_BED_RFW  (0x08000ul) /**< @brief Right front wide. */
#define OAMDI_BIT_MASK_BED_LFE2 (0x10000ul) /**< @brief Low-frequency effects 2. */
/* @} */

/* @{ */
/**
 * @name Number of objects in the ISF modes
 */
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH03100 (4u)
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH05300 (8u)
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH07300 (10u)
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH09500 (14u)
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH07530 (15u)
#define OAMDI_ISF_OBJ_COUNT_OAMDI_ISF_BH15951 (30u)
/* @} */

/******************************************************************************/
/* Data Types                                                                 */
/******************************************************************************/

/** 
 * @brief Object audio metadata data type. 
 */
typedef struct oamdi_s oamdi;

/** 
 * @brief Object audio metadata bool type. 
 */
typedef unsigned char oamdi_bool;

/** 
 * @brief OAMDI init-time configuration parameters. 
 */
typedef struct oamdi_init_info_s
{
    unsigned max_num_objs;        /**< Valid range: [1, 159]. 
                                   **  Maximum number of objects. */
    unsigned max_num_md_updates;  /**< Valid range: [1, 8]. 
                                   **  Maximum number of metadata updates per codec frame. */
    unsigned frame_size;          /**< This parameter is ignored. */                                          
} oamdi_init_info;

/** 
 * @brief OAMDI library version. 
 */
typedef struct oamdi_lib_version_info_s
{
    int v_api;     /**< API version. */
    int v_fct;     /**< Functional change. */
    int v_mtnc;    /**< Maintenance release. */
} oamdi_lib_version_info;

/** 
 * @brief Object audio metadata version. 
 */
typedef struct oamdi_metadata_version_info_s
{
    int v_major;  /**< OAMD major. */
    int v_minor;  /**< OAMD minor. */
} oamdi_metadata_version_info;

/** 
 * @brief Intermediate spatial format objects' configurations. 
 */
enum 
{
   OAMDI_ISF_NULL = 0, /* No ISF objects present. */
   OAMDI_ISF_BH03100,
   OAMDI_ISF_BH05300,    
   OAMDI_ISF_BH07300,
   OAMDI_ISF_BH09500,
   OAMDI_ISF_BH07530,
   OAMDI_ISF_BH15951,
};
typedef int oamdi_isf_objs;

/** 
 * @brief Object type. 
 */
enum 
{
    OAMDI_BED_OBJECT = 0,
    OAMDI_ISF_OBJECT,
    OAMDI_DYNAMIC_OBJECT
};
typedef int oamdi_obj_type;

/** 
 * @brief Object width. 
 */
enum 
{
    OAMDI_WIDTH_NO_SPREADING = 0,    /**< No object spreading. */
    OAMDI_WIDTH_1D           = 1,    /**< 1-D object spreading (radial). */
    OAMDI_WIDTH_3D           = 2     /**< 3-D object spreading (given by X, Y and Z width). */
};
typedef int oamdi_obj_width_mode;

/** 
 * @brief Channel assignment of bed objects. 
 */
enum 
{
    OAMDI_BED_L    = 0,    /**< Left channel. */
    OAMDI_BED_R    = 1,    /**< Right channel. */
    OAMDI_BED_C    = 2,    /**< Left channel. */
    OAMDI_BED_LFE  = 3,    /**< Low-frequency effects. */
    OAMDI_BED_LS   = 4,    /**< Left surround. */
    OAMDI_BED_RS   = 5,    /**< Right surround. */
    OAMDI_BED_LRS  = 6,    /**< Left rear surround. */
    OAMDI_BED_RRS  = 7,    /**< Right rear surround. */
    OAMDI_BED_LFH  = 8,    /**< Left front height. */
    OAMDI_BED_RFH  = 9,    /**< Right front height. */
    OAMDI_BED_LTM  = 10,   /**< Left top middle. */
    OAMDI_BED_RTM  = 11,   /**< Right top middle. */
    OAMDI_BED_LRH  = 12,   /**< Left top middle. */
    OAMDI_BED_RRH  = 13,   /**< Right rear height. */
    OAMDI_BED_LFW  = 14,   /**< Left front wide. */
    OAMDI_BED_RFW  = 15,   /**< Right front wide. */
    OAMDI_BED_LFE2 = 16    /**< Low-frequency effects 2.*/
};

/** 
 * @brief Bed instance. 
 *
 *  A bit mask describing channels present in a
 *  bed instance. 1 indicates a channel presence.
 *  OAMDI_BIT_MASK_BED defines applicable channels. 
 *
 *  User should always specify a 17-bit 
 *  non-standard channel mask. 
 *  If non-standard channel assignment mask
 *  can be represented by a standard channel mask
 *  it will be converted so during serialization,
 *  and converted back during deserialization. 
 *  If only LFE bit is set in chan_assign
 *  "lfe_only" bitstream flag will be serialized.
 */
typedef unsigned long oamdi_bed_chan_assign_mask; 

/** 
 * @brief Zone mask. 
 */
enum 
{
    OAMDI_ZONE_ALL_A       = 0, /**< All active. */
    OAMDI_ZONE_BACK_D      = 1, /**< Back disabled. */
    OAMDI_ZONE_SIDE_D      = 2, /**< Side disabled. */
    OAMDI_ZONE_CENT_BACK_A = 3, /**< Center back. */
    OAMDI_ZONE_SCREEN_A    = 4, /**< Screen active. */
    OAMDI_ZONE_SURR_A      = 5, /**< Surround active. */    
};
typedef int oamdi_zone_mask;

/** 
 * @brief Program assignment.
 *
 * Total number of objects specified as bed objects, 
 * ISF objects and dynamic objects MUST be less than
 * max_num_objs used for initialization and greater 
 * than 0. 
 */
typedef struct oamdi_prog_assign_s
{
    unsigned short             num_dyn_objs;                        /**< Number of the dynamic objects. Valid range: [0, 159]. */
    oamdi_bool                 bed_object_chan_distr;               /**< Bed objects channel distribution. If 1, 
                                                                         it indicates that the content producer prefers 
                                                                         that channel distribution of the surround, rear surround, 
                                                                         and height bed channels be applied in playback 
                                                                         environments that support it. */
    unsigned short             num_bed_instances;                   /**< Number of bed instances. Valid range: [0, 9]. */
    oamdi_bed_chan_assign_mask a_beds[OMADI_MAX_NUM_BED_INSTANCES]; /**< Channel configuration for each bed instance
                                                                     **  Number of bed instances is defined by num_bed_instances. */
    oamdi_isf_objs             interm_spatial_format;               /**< Intermediate spatial format. If 0, no ISF objects present in 
                                                                         the stream. If set, it indicates the intermediate spatial format 
                                                                         of the associated objects. This field should be set with 
                                                                         the OAMDI_ISF_BHxxxxx enums or left to 0. */
} oamdi_prog_assign;

/** 
 * @brief Block update info. 
 */
typedef struct oamdi_blk_update_info
{
  unsigned short blk_offset_factor; /**< Valid range: [0, 63].
                                     **  Info block offset factor in 32-sample blocks. 
                                     **  This factor describes number of 
                                     **  32-sample blocks away from the sample 
                                     **  offset value that the metadata block applies.
                                     **  Sample offset is defined in oamdi_md_update_info.
                                     **   */
  unsigned short ramp_duration;     /**< Valid range: [0, 2048]. 
                                     **  Ramp duration in samples.  */
} oamdi_blk_update_info;

/** 
 * @brief Metadata update info. 
 */
typedef struct oamdi_md_update_info_s
{
    unsigned              sample_offset;       /**< Valid range: [0, 31].
                                                **  Number of samples relative to the start of 
                                                **  the codec frame where the metadata block offset 
                                                **  value takes effect. 
                                                **  Block offset (factor) is defined in oamdi_blk_update_info.
                                                */
    unsigned short        num_obj_info_blks;   /**< Valid range: [1, 8].
                                                **  Number of info blocks (metadata updates)
                                                **  per object per codec frame. */
    oamdi_blk_update_info a_blk_update_info[OAMDI_MAX_OBJ_INFO_BLKS];   
} oamdi_md_update_info;

/** 
 * @brief Object basic info block. 
 */
typedef struct oamdi_obj_basic_info_s
{
    int             gain;            /**< Q15 value representing object linear gain scaled by 8, ranging [0, 5.623] (before scaling).
                                      **  0x0000 (0.000 Q15, 0.000 linear, -inf dB)
                                      **  0x0203 (0.015 Q15, 0.125 linear, -18 dB)
                                      **  0x1000 (0.125 Q15, 1.000 linear, 0 dB)
                                      **  0x1FEC (0.250 Q15, 2.000 linear, 6 dB)
                                      **  0x59f9 (0.703 Q15, 5.623 linear, 15 dB)
                                      **  During serialization, this value is rounded to nearest  
                                      **  and converted to dB ranging [-inf, 15] and represented on 6 bits.
                                      **  During deserialization this values is converted back
                                      **  to linear value in Q15. */
    int             priority;        /**< Q15 value ranging [0, 1]. 
                                      **  0x7fff for priority == 1.
                                      **  During serialization, this value is truncated and 
                                      **  quantized to 5 bits.
                                      **  During deserialization this values is converted back
                                      **  to Q15. */                                     
} oamdi_obj_basic_info;




/** 
 * @brief Object render info block. 
 */
typedef struct oamdi_obj_render_info_s
{
    int                  pos3d_x;          /**< Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 6 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
    int                  pos3d_y;          /**< Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 6 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
    int                  pos3d_z;          /**< Q15 value ranging [-1, 1]. 0xffff8000 for -1, 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 4 bits + sign bit.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
    oamdi_zone_mask      zone_mask;        /**< oamdi_zone_mask enum defines valid values. */
    oamdi_bool           en_elevation;     /**< Indicates whether the object should be rendered to a height zone. */    
    oamdi_obj_width_mode width_mode;       /**< Specifies the type of width spreading */
    union
    {
        int      w1d;                      /**< if (width_mode == oamdi_WIDTH_1D) 
                                            **  Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 5 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
        struct {                           /**< if (width_mode == oamdi_WIDTH_3D) */
            int      x;                    /**< User specifies value of w1d with range [0, 31]. 
                                            **  Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 5 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
            int      y;                    /**  Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 5 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
            int      z;                    /**  Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 5 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
        } w3d;                             
    } width;
    oamdi_bool     object_use_screen_ref;  /**< Use screen size as a reference for the objects' position. */
    int            object_screen_factor;   /**< Q15 value ranging [0, 1]. 0x7fff for 1.
                                            **  0 - use room as reference (i.e. no scaling)
                                            **  1 - use screen as reference
                                            **  other - x and z are linearly scaled. 
                                            **  During serialization, this value is truncated and 
                                            **  quantized to 3 bits.
                                            **  During deserialization this values is converted back
                                            **  to Q15. */
    int            object_depth_factor;    /**< Valid values [0, 3]. Y-position exponent equals:
                                            **  1/4 for 0,
                                            **  1/2 for 1, 
                                            **  1   for 2,
                                            **  2   for 3.
                                            ** */ 
    oamdi_bool     object_snap;            /**< If 1, the object shall be snapped to the nearest speaker. */
} oamdi_obj_render_info;

/** 
 * @brief Object info block. 
 */
typedef struct oamdi_obj_info_blk_s
{
    oamdi_bool            obj_not_active;  /**< By default, all objects are active. */
    oamdi_obj_basic_info  basic_info;      /**< Basic info block data. */
    oamdi_obj_render_info render_info;     /**< Render info block data. */
} oamdi_obj_info_blk;

/** 
 * @brief Object data. 
 */
typedef struct oamdi_obj_data_s
{
    oamdi_obj_info_blk    a_obj_info_blk[OAMDI_MAX_OBJ_INFO_BLKS]; /**< Info blocks for all the MD updates. */
    oamdi_obj_type        obj_type;                                /**< Object type. This field cannot be set directly. 
                                                                    **  It is updated when program assignment is set with 
                                                                    **  oamdi_set_prog_assign() and after deserialization. */
    unsigned              bed_obj_chan_assign;                     /**< Channel assignment for bed objects. This field cannot be set directly. 
                                                                    **  It is updated when program assignment is set with 
                                                                    **  oamdi_set_prog_assign() and after deserialization. */
} oamdi_obj_data;

/******************************************************************************/
/* Public API                                                                 */
/******************************************************************************/

/** 
 * @brief Returns the memory needed by the OAMDI. 
 * @return Size in bytes of a memory block.
 */
size_t
oamdi_query_mem
    (const oamdi_init_info *p_init_info /**< [in] configuration parameters. */
    );

/** 
 * @brief Instantiate the OAMDI instance. 
 * @return Memory-aligned OAMDI context.
 */
oamdi *
oamdi_init
    (const oamdi_init_info *p_config /**< [in] Configuration parameters. */
    ,      void            *p_mem    /**< [in] Pointer to allocated memory for working buffer. */
    );

/** 
 * @brief Duplicates an OAMDI instance. 
 * @return Duplicated, memory-aligned OAMDI.
 */
oamdi *
oamdi_duplicate
    (const oamdi_init_info *p_config    /**< [in] Configuration parameters. */
    ,      void            *p_mem       /**< [in] Pointer to allocated memory for working buffer. 
                                         **  Memory buffer needs to be large enough to 
                                         **  store all metadata from the source oamdi. 
                                         **  oamdi_get_init_info() can be used to retrieve 
                                         **  initialization parameters of the source oamdi instance. 
                                         **  Size of the required buffer can be calculated 
                                         **  with oamdi_query_mem().*/
    ,const oamdi           *p_oamdi_src /**< [in] Source OAMDI instance. */
    );

/** 
 * @brief Update internal pointers after copying OAMDI with memcpy. 
 */
void
oamdi_validate_after_copy
    (oamdi *p_oamdi /**< [in, out] OAMDI instance. */
    );

/*********************************/
/* Setters/Modifiers             */
/*********************************/

/** 
 * @brief Set program assignment info. 
 */
void
oamdi_set_prog_assign
    (      oamdi             *p_oamdi /**< [in, out] OAMDI instance. */
    ,const oamdi_prog_assign *p_prog  /**< [in]      Program assignment data 
                                       **            to be copied to OAMDI. 
                                       **            This function updates obj_type and 
                                       **            bed_obj_chan_assign fields of 
                                       **            oamdi_obj_data struct. Specifying 
                                       **            more objects in program assignment 
                                       **            than max_num_obj used for initialization, 
                                       **            will result in undefined behaviour. 
                                       */ 
    );

/** 
 * @brief Set metadata update info.
 *
 *  This function is used to set:
 *  a) Sample offset; 
 *  b) Number of info blocks (metadata updates) per object per codec frame;
 *  c) Offset of each info block;
 *  d) Ramp duration of each info block.
 */
void
oamdi_set_md_update_info
    (      oamdi                *p_oamdi          /**< [in, out] OAMDI instance. */
    ,const oamdi_md_update_info *p_md_update_info /**< [in]      Metadata update info to be copied to OAMDI.
                                                   **            Specifying num_obj_info_blks > max_num_md_updates
                                                   **            will result in undefined behaviour.
                                                   */
    );

/** 
 * @brief Set sample offset (a reference) for all info blocks per codec frame. 
 */
void
oamdi_set_sample_offset
    (oamdi       *p_oamdi       /**< [in, out] OAMDI instance. */
    ,unsigned     sample_offset /**< [in]      New offset for all info blocks. */  
    );

/** 
 * @brief Set single info block of an object. 
 */
void
oamdi_set_obj_info_blk
    (      oamdi              *p_oamdi          /**< [in, out] OAMDI instance. */
    ,      unsigned            obj_id           /**< [in]      Object ID. MUST be < max_num_obj used for initialization. */
    ,      unsigned            obj_info_blk_idx /**< [in]      Info block index. MUST be < than max_n_md_updates 
                                                 **            used for initialization. */
    ,const oamdi_obj_info_blk *p_obj_md         /**< [in]      Info block to be copied to p_oamd. */
    );

/** 
 * @brief Set object not active flag. 
 *
 * Object not active flag is set per object info block. 
 */
void
oamdi_set_obj_not_active
    (      oamdi              *p_oamdi          /**< [in, out] OAMDI instance. */
    ,      unsigned            obj_id           /**< [in]      Object ID. MUST be < max_num_obj used for initialization. */
    ,      unsigned            obj_info_blk_idx /**< [in]      Info block index. MUST be < than max_n_md_updates 
                                                 **            used for initialization. */
    ,      oamdi_bool          value            /**< [in]      If 1, object is not active. If 0, object is active. 
                                                 **            By default all objects are active. */
    );

/*********************************/
/* Getters                       */
/*********************************/

/** 
 * @brief Get version of the OAMDI library. 
 * @return Library version. 
 */
const oamdi_lib_version_info *
oamdi_get_lib_ver(void);

/** 
 * @brief Get version of the object audio metadata specification.
 * @return Metadata version.
 */
const oamdi_metadata_version_info *
oamdi_get_md_ver(void);

/** 
 * @brief Get initialization parameters used to instantiate the OAMDI instance.  
 */
void
oamdi_get_init_info
    (const oamdi           *p_oamdi        /**< [in, out] OAMDI instance. */
    ,      oamdi_init_info *p_oamdi_config /**< [in, out] Pointer to structure to be populated. */
    );

/** 
 * @brief Get number of objects in an OAMDI instance based on program assignment data.
 * @return Number of objects according to program assignment. 
 */
unsigned  
oamdi_get_obj_count
    (const oamdi *p_oamdi /**< [in, out] OAMDI instance. */
    );

/** 
 * @brief Get program assignment data. 
 */
const oamdi_prog_assign * /** @return Address of oamdi_prog_assign struct in an OAMDI instance. */ 
oamdi_get_prog_assign
    (const oamdi *p_oamdi /**< OAMDI instance. */
    );

/** 
 * @brief Get metadata for all objects. 
 * @return Address of oamdi_obj_data struct in an OAMDI instance. 
 */
const oamdi_obj_data *
oamdi_get_all_obj_md
    (const oamdi *p_oamdi /**< [in, out] OAMDI instance. */
    );

/** 
 * @brief Get metadata from a single info block of an object. 
 * @return Address of oamdi_obj_info_blk struct in an OAMDI instance.
 */
const oamdi_obj_info_blk *
oamdi_get_obj_info_blk
    (const oamdi    *p_oamdi          /**< [in, out] OAMDI instance. */
    ,      unsigned  obj_id           /**< [in]      Object ID. MUST be < max_num_obj used for initialization. */
    ,      unsigned  obj_info_blk_idx /**< [in]      Info block index. MUST be < than max_n_md_updates 
                                       **            used for initialization. */     
    );

/** 
 * @brief Get metadata update info. 
 * @return Address of oamdi_md_update_info struct in an OAMDI instance.
 */
const oamdi_md_update_info *
oamdi_get_md_update_info
    (const oamdi *p_oamdi  /**< [in, out] OAMDI instance. */
    );

/** 
 * @brief Get sample offset of metadata blocks. 
 * @return Sample offset of first metadata block.
 */
unsigned
oamdi_get_sample_offset
    (const oamdi *p_oamdi  /**< [in, out] OAMDI instance. */
    );

/** 
 * @brief Get number of metadata updates per object per audio frame. 
 * @return Number of object info blocks (metadata updates) per audio frame.
 */
unsigned
oamdi_get_num_obj_info_blks
    (const oamdi *p_oamdi  /**< [in, out] OAMDI instance. */
    );

/** 
 * @brief Get object not active flag. 
 *
 * Object not active flag is set per object info block.  
 *
 * @return If 1, object is not active. If 0, object is active.
 */
unsigned
oamdi_get_obj_not_active
    (const oamdi    *p_oamdi          /**< [in, out] OAMDI instance. */
    ,      unsigned  obj_id           /**< [in]      Object ID. MUST be < max_num_obj used for initialization. */
    ,      unsigned  obj_info_blk_idx /**< [in]      Info block index. MUST be < than max_n_md_updates 
                                       **            used for initialization. */
    );

/*********************************/
/* Convenience functions         */
/*********************************/
/** 
 * @brief Check if given channel assignment mask is a standard channel assignment mask. 
 *
 * Standard channel assignment has either both or neither of symmetric channels, 
 * e.g if L is present and R not, channel assignment is non-standard.
 * 
 * @return 1 if channel assignment is standard, 0 otherwise.
 */
unsigned
oamdi_is_std_chan_assign
    (oamdi_bed_chan_assign_mask bed_chan_assign_mask /**< [in] Channel assignment mask. */
    );

/** 
 * @brief Get number of objects (channels) in a bed instance. 
 * @return Number of channels in a bed instance.
 */
unsigned
oamdi_get_bed_channels_count
    (oamdi_bed_chan_assign_mask bed_chan_assign_mask /**< [in] Channel mask of a bed instance. */
    );

#ifdef OAMDI_USE_ENCODE_API
#include "oamdi/include/oamdi_enc.h"
#endif

#ifdef OAMDI_USE_DECODE_API
#include "oamdi/include/oamdi_dec.h"
#endif

#ifdef __cplusplus
}
#endif
#endif /* OAMDI_H */
/*@}*/
