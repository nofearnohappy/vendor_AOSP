/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#ifndef UDC_USER_H
#define UDC_USER_H

/**** Module Error Codes ****/
/**** NOTE: In order to avoid collisions with OS-specific error codes, the following
            numbers should not be used:
            126-127 (conflict with Cygwin internal codes)
            Any number >= 256 (they cannot be captured with the common Perl module)
 ****/
#define ERR_NO_ERROR                          0
#define	ERR_INVALID_PCM_WORD_SIZE             1
#define	ERR_INVALID_KARAOKE_MODE              2
#define	ERR_INVALID_DBG_ARG                   3
#define	ERR_INVALID_DBGINFO_ARG               4
#define	ERR_INVALID_DBGB4B_ARG                5
#define	ERR_INVALID_COMPR_MODE                6
#define	ERR_INVALID_LFE_MODE                  7
#define	ERR_INVALID_OUTPUT_MODE               8
#define	ERR_INVALID_NOUTPUT_CHANS             9
#define	ERR_INVALID_PCM_SCL_FACTOR            10
#define ERR_INVALID_INPUTMODE                 11
#define	ERR_INVALID_STEREO_MODE               13
#define	ERR_INVALID_DUAL_MONO_MODE            14
#define	ERR_INVALID_DRC_CUT                   15
#define	ERR_INVALID_DRC_BOOST                 16
#define	ERR_INVALID_COMMAND_LINE              17
#define	ERR_DISPLAY_USAGE_ONLY                18
#define	ERR_INVALID_OUTFILE_ARG               19
#define	ERR_INVALID_VERBOSE_MODE              20
#define	ERR_UNKNOWN_INPUTFILE                 21
#define	ERR_UNKNOWN_OUTMETADATAFILE           22
#define	ERR_UNKNOWN_OUTPCMFILE                23
#define	ERR_INVALID_FILESUPPRESS_ARG          24
#define ERR_TOO_FEW_OUTPUT_FILES			  25
#define ERR_TOO_MANY_OUTPUT_PARAMS            26
#define ERR_INVALID_EXTCHAN_ROUTE_ARG         27
#define	ERR_ILLEGAL_CHAN_ROUTE_ARRAY          28
#define	ERR_INVALID_PCMCONCEAL_REPEAT_COUNT   29
#define	ERR_INVALID_PCMCONCEAL_ARG            30
#define	ERR_UNKNOWN_RUNNING_MODE              31
#define	ERR_OPERATING_MODE_MISMATCH	          32
#define	ERR_INVALID_OPERATING_MODE_ARG        33
#define	ERR_INVALID_ASSOC_STREAMID_ARG        34
#define	ERR_INVALID_WAVEOUTPUT_MODE           35
#define ERR_SEPARATOR_USAGE                   36
#define ERR_INCONSISTENT_NUM_OUTPUTS          37
#define ERR_ASSOC_ID_MISSING                  38
#define ERR_INVALID_MDCTBANDLIMIT             40
#define ERR_PROCESSING                        41
#define ERR_INVALID_QUIET_MODE                42
#define ERR_INVALID_ROUTE_ARG                 43
#define ERR_INVALID_MIX_BALANCE               44
#define ERR_PARSE_CSV_ERROR                   45
#define ERR_MEMORY                            46
#define ERR_QUERY_FAILED                      47
#define ERR_OPEN_FAILED                       48
#define ERR_FILE                              49
#define ERR_UNKNOWN                           50
#define ERR_PROCESS_OUTPUT                    51
#define	ERR_UNKNOWN_LICENSE_FILE              52
#define	ERR_READING_LICENSE_FILE              53
#define	ERR_DUALMONO_ASSOCIATED               54
#define ERR_INVALID_HARD_CLIPPING_MODE        55


/**** Module Equates ****/

#define	    END_OF_STREAM	    (-1)    /* used for setting default frame region */

#define     DEFAULTOUTMODE      (7)     /* default output mode */
#define	    DEFAULTSCALEFACTOR  (100)   /* default scale factor */
#define	    MINSCALEFACTOR      (0)     /* minimum scale factor */
#define	    MAXSCALEFACTOR      (100)   /* maximum scale factor */
#define	    DEFAULTSUBSTREAMID	(0)
#define     CHAN_NOT_SET        (-1)    /* Used for routing array initialization */
/* The following two values (representing dB) are used for validation of
   usermixbalance.  Positive values (1 to 31) attenuate the main audio
   (32 = full mute main); negative values (-1 to -31) attenuate the
   associated (-32 = full mute associated).  The default value is 0, which
   indicates no adjustment. */
#define     MIN_USERMIXBALANCE    (-32)
#define     MAX_USERMIXBALANCE     (32)

#define     FIO_MAXFILENAMELEN 1024

#define     MAX_DYNAMIC_PARAS   50
#define     MAX_SINGLE_OPTION_LENGTH 20

/* maximum license file size in bytes */
#define		MAX_LICENSE_FILE_SIZE (1024)

/* metadata output defines */
#define     MDAT_MASK_TSI         0x00000001
#define     MDAT_MASK_BSI         0x00000002
#define     MDAT_MASK_AUDBLK      0x00000004
#define     MDAT_MASK_AUXDATA     0x00000008
#define     MDAT_MASK_MIXING      0x00000010
#define     MDAT_MASK_REENCODE    0x00000020
#define     MDAT_MASK_CHANMAPOUT  0x00000040

/* output file suppression options */
typedef enum
{
    FILENOTSUPPRESSED,
    FILESUPPRESSED
} FILESUPPRESSIONOPTIONS;

/* define verbose mode levels */
typedef enum
{
    VERBOSELEVEL0,
    VERBOSELEVEL1,
    VERBOSELEVEL2
} VERBOSEMODELEVEL;

/* define decoder operating mode */
typedef enum
{
	OPERATING_MODE_DCV = 0, /* Decoder operate as decoder converter,
								   the 7.1 or 5.1 output is controlled by output channel configuration.
								   the converter on or off is controlled by running mode */
	OPERATING_MODE_DDC		/* Decoder operate as dual decoder converter */
} OPERATING_MODE;

/* The substream id for the associated substream */
typedef enum
{
	SUBSTREAMID_1 = 1,
	SUBSTREAMID_2 = 2,
    SUBSTREAMID_3 = 3,
} SUBSTREAMID;

/* Define the JOC output object file format */
typedef enum
{
    JOC_SINGLE_MULTI_CHAN_FILE = 0,
    JOC_MULTIPLE_MONO_FILES = 1,
    JOC_OBJ_FILE_FORMAT_MASK = 2,
    JOC_OBJ_FILE_CHANNEL_MASK = ~3
} JOC_OBJ_FILE_FORMAT;

/**** Module Structures ****/

typedef struct
{
    char p_ddpinfilename[DDPI_UDC_PROG_COUNT][FIO_MAXFILENAMELEN];          
                                            /* input file name for main and associated stream   */
    char p_pcmoutfilename[DDPI_UDC_MAX_PCMOUT_COUNT][FIO_MAXFILENAMELEN];	    
                                            /* multiple output PCM file name for main stream	*/
    char p_mdatoutfilename[DDPI_UDC_PROG_COUNT][FIO_MAXFILENAMELEN];          
                                            /* metadata output file name for main and associated stream   */
    char p_evooutfilename[DDPI_UDC_PCMOUT_COUNT][FIO_MAXFILENAMELEN];          
                                            /* evolution metadata output file name for main and associated stream   */
    char p_evoquick_outfilename[1][FIO_MAXFILENAMELEN];
                                            /* evolution metadata quick access output file name for main and associated stream   */
    char p_configfilename[FIO_MAXFILENAMELEN];
                                            /* config file name */
    const char *p_ddoutfilename;            /* output DD file name              */
    const char *p_licensefilename;          /* input license file name          */

    int waveoutput;                         /* Microsoft Wave output files (*.wav) or not (*.pcm) */
    int alt71wavrouting;                    /* Use [L R C LFE x1 x2 Ls Rs] routing for Wave output */
    int pcmwordtype;                        /* output PCM file word type        */
    int timeslicesizeheader;                /* Use timeslice size header        */
    int timeslicestart;                     /* timeslice region start           */
    int timesliceend;                       /* timeslice region end             */
    int verbose;                            /* verbose mode                     */
    int outnchans[DDPI_UDC_PCMOUT_COUNT];   /* number of output channels        */
    int chanrouting[DDPI_UDC_PCMOUT_COUNT][DDPI_UDC_MAXPCMOUTCHANS];        
                                            /* channel routing array            */
    int channelmask[DDPI_UDC_PCMOUT_COUNT]; /* wave output channel mask         */
    int quitonerr;                          /* if 0, continue on process err    */
    unsigned int mdatflags;                 /* metadata output flags            */
    unsigned int evo_out_to_file_flag;      /* evolution metadata output flags  */
    unsigned int evo_quickaccess_out_to_file_flag;      /* evolution metadata output flags  */

    int numofddpin;                         /* number of DD+ inputs (1 or 2)    */
    int numofddout;                         /* number of DD outputs (0 or 1)    */
    int numofpcmout;                        /* number of PCM outputs            */
    int numofmainpcmout;                    /* num. of main program PCM outputs */
    int use_config;							/*!< \inout:  use config file flag */
    int	switching_frame;					/*!< \inout:  the index of frame, to which the switching parameter will apply */
    int parameter_ready_for_switch;			/*!< \inout:  parameters switch flag */
    int  dynamic_argc;                      /* Number of arguments dynamically changed */
    char dynamic_argv[MAX_DYNAMIC_PARAS][MAX_SINGLE_OPTION_LENGTH]; /* Argument dynamically changed */
    FILE *fp;								/*!< \inout:  config file pointer */
    int is_interleaved_flag;	            /* Output data interleaved or non-interleaved flag */
    int data_type;                          /* The data type of the dlb_buffer for the PCM output */
    int joc_num_outchannels[DDPI_JOCD_MAX_CHANNELS];               /* Used for JOC mode, the number of channels for one single file */
} EXECPARAMS;

typedef struct
{
    int runningmode;              /* running mode                     */
    int compmode;                 /* dynamic range compression mode   */
    int outlfe;                   /* output LFE channel present       */
    int outchanconfig;            /* output channel configuration     */
    int pcmscale;                 /* PCM scale factor                 */
    int dynscalehigh;             /* dynamic range compression cut scale factor   */
    int dynscalelow;              /* dynamic range compression boost scale factor */
    int stereomode;               /* stereo output mode               */
    int dualmode;                 /* dual mono reproduction mode      */
    int inputmode;		          /* single or dual input */
    int associdselect;            /* substream ID selector	                        */
    int mdctbandlimit;            /* MDCT bandlimiting mode                         */
    int dec_errorconcealflag;     /* Subroutine PCM output error concealment flag   */
    int dec_errorconcealtype;     /* Subroutine PCM output error concealment method */
    int cnv_errorconcealflag;     /* Subroutine DD output error concealment flag    */
    int drcsuppressmode;
    int converttimeslicestart;    /* converting time slice region start             */
    int converttimesliceend;      /* converting time slice region end               */
    int decodetimeslicestart;     /* decoding time slice region start               */
    int decodetimesliceend;	      /* decoding time slice region end                 */
    unsigned int frm_debugflags;  /* Frame debug flags                              */
    unsigned int dec_debugflags;  /* Decode debug flags                             */
    int decorr_mode;              /* Decorrelator operating mode                    */
    int evohashflag;              /* Evolution decoder operating mode   */
    int misd_substreamidx;	      /* Substream ID of the program, that subparam related */
    int	is_evolution_quickaccess;			 /*!< \inout: evolution quick access flag  */
    int evoquickaccess_substreamid;			 /* quick access evolution substream ID */
    int evoquickaccess_strmtype;			 /* quick access evolution substream type */
    int joc_force_downmix;        /* force joc output downmix content */
} SUBPARAMS;

/**** Module Functions ****/

/*****************************************************************
* displaybanner():
*	This function displays the version info and copyright message,
*	as well as information about the configuration.
*
*	Parameters
*	p_decparams			Output from subroutine query function
*
*	Return value
*	ERR_NO_ERROR (0) if no error, nonzero otherwise
*
*****************************************************************/
int displaybanner(
    const ddpi_udc_query_op  *p_decparams);                /* input  */

/*****************************************************************
* initexecparams():
*	This function initializes variables used by the executive,
*   which can be overridden by the user in the command-line interface.
*
*	Parameters
*	p_execparams		User-specified executive parameters
*
*	Return value
*	ERR_NO_ERROR (0) if no error, nonzero otherwise
*
*****************************************************************/
int initexecparams(
    EXECPARAMS *p_execparams);               /* modify */

/*****************************************************************
* initsubparams():
*	This function initializes variables used by the subroutine,
*   which can be overridden by the user in the command-line interface.
*
*	Parameters
*	p_subparams		User-specified subroutine parameters
*
*	Return value
*	ERR_NO_ERROR (0) if no error, nonzero otherwise
*
*****************************************************************/
int initsubparams(
    SUBPARAMS *p_subparams);                /* output */

/*****************************************************************\
* setsubparams():
*	This function sets parameters in the subroutine.
*
*	Parameters
*	numofoutputs        Number of Outputs
*	p_subparams			User-specified subroutine parameters
*   p_dechdl            Handle to subroutine memory
*
*	Return value
*	ERR_NO_ERROR (0) if no error, nonzero otherwise
*
\*****************************************************************/
int setsubparams(
	int numofoutputs,                /* input  */
    SUBPARAMS *p_subparams,          /* input  */
    void *p_dechdl);                 /* modify */

#endif /* UDC_USER_H */
