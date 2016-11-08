//
//  DRAMC_COMMON.H    
//

#ifndef _DRAMC_COMMON_H_
#define _DRAMC_COMMON_H_

/***********************************************************************/
/*                  Public Types                                       */
/***********************************************************************/
#if 1
#include <typedefs.h>
#else
typedef unsigned char   UCHAR;
typedef unsigned char   UINT8;
typedef unsigned short  UINT16;
typedef signed char     INT8;
typedef signed int    INT16;

typedef signed char            S8;
typedef signed short           S16;
typedef signed int            S32;
typedef unsigned char   U8;
typedef unsigned short  U16;
typedef unsigned int   U32;

#ifndef UINT32P
typedef volatile unsigned int* UINT32P; 
#endif //UINT32P
#endif

/*------------------------------------------------------------*/
/*                  macros, defines, typedefs, enums          */
/*------------------------------------------------------------*/
/************************ Bit Process *************************/
#define mcBITL(b)               (1L << (b))
//#define mcBIT(b)                (1 << (b))
#define mcBIT(b)                mcBITL(b)
#define mcMASK(w)               (mcBIT(w) - 1)
#define mcMASKS(w, b)           (mcMASK(w) << (b))
//#define mcCLR_BIT_C(a)          ((-1 - (Bit_#a)) & 0xFFFF)
#define mcCLR_BIT_C(a)          ((~Bit_##a) & 0xFFFF)

#define mcSET_MASK(a, b)        ((a) |= (b))
#define mcCLR_MASK(a, b)        ((a) &= (~(b)))
//#define mcCLR_SET_MASK(a, b, c)  ((a) = ((a) & (~(b))) | (c))
//          \_ same as mcCLR_MASK + mcSET_MASK at optimized code
#define mcSET_BIT(a, b)         mcSET_MASK(a, mcBIT(b))
#define mcCLR_BIT(a, b)         mcCLR_MASK(a, mcBIT(b))
#define mcCHK_BIT1(a, b)        ((a) & mcBIT(b))
#define mcCHK_BITM(a, b, m)     (((a) >> (b)) & (m))
#define mcCHK_BITS(a, b, w)     mcCHK_BITM(a, b, mcMASK(w))
//#define mcTEST_BIT(a, b)        mcCHK_BITM(a, b, 1)
#define mcTEST_BIT(a, b)        mcCHK_BIT1(a, b)
#define mcCHG_BIT(a, b)         ((a) ^= mcBIT(b))

#define mcSET_FIELD0(var, value, mask, pos) mcSET_MASK(var, ((value) << (pos)) & (mask))

#define mcSET_FIELD(var, value, mask, pos)  \
{                                           \
    mcCLR_MASK(var, mask);                  \
    mcSET_FIELD0(var, value, mask, pos);    \
}

#define mcGET_FIELD(var, mask, pos)     (((var) & (mask)) >> (pos))

#ifndef BIT0
#define BIT0                    mcBIT(0)
#define BIT1                    mcBIT(1)
#define BIT2                    mcBIT(2)
#define BIT3                    mcBIT(3)
#define BIT4                    mcBIT(4)
#define BIT5                    mcBIT(5)
#define BIT6                    mcBIT(6)
#define BIT7                    mcBIT(7)
#endif

#define mcLOW_MASK(u16)         ((UCHAR) ((u16) & mcMASK(8)))
#define mcHIGH_MASK(u16)        ((UCHAR) ((u16) & ~mcMASK(8)))
#define mcLOW2HIGH(u16)         (((UINT16) (u16)) << 8)
/* (1234, 5678) -> 7834 */
#define mc2BYTE(ch1L, ch2H)     (mcLOW_MASK(ch1L) | mcLOW2HIGH(ch2H))
/* (1234) -> 0034 */
//#define mcLOW_BYTE(u16)         ((UCHAR)(u16))
#define mcLOW_BYTE(u16)         mcLOW_MASK(u16)
/* (1234) -> 0012 */
#define mcHIGH_BYTE(u16)        ((UCHAR) (((UINT16) (u16)) >> 8))

/* xxxHHHHH + LLLLLxxx */
#define mcUNSIGN_2BYTE(u8h, u8l, hmsk, lmsk)                \
    (((mcLOW2HIGH((u8h) & mcMASK(hmsk)) | (u8l))            \
        << (8 - (hmsk))) >> ((8 - (hmsk)) + (8 - (lmsk))))

#define mcSIGN_2BYTE(u8h, u8l, hmsk, lmsk)                  \
    (((INT16) ((mcLOW2HIGH((u8h) & mcMASK(hmsk)) | (u8l))     \
        << (8 - (hmsk)))) >> ((8 - (hmsk)) + (8 - (lmsk))))

#define mcSET_MASKS(par)                mcMASKS(cw##par, cp##par)
#define mcFIELD_CMB(val, par)           mcFIELD(val, cm##par, cp##par)
#define mcGET_FIELD_CMB(var, par)       mcGET_FIELD(var, cm##par, cp##par)
#define mcSET_FIELD_CMB(var, val, par)  mcSET_FIELD(var, val, cm##par, cp##par)

#define CEIL_A_OVER_B(_A, _B)	(((_A)-(_B)*((_A)/(_B))) > 0? (_A)/(_B)+1:(_A)/(_B))

// Edward?? : The following needs to be porting.
/************************** Common Macro *********************/
#define delay_a_while(count) \
    do {                                   \
           register unsigned int delay;        \
           asm volatile ("mov %0, %1\n\t"      \
                         "1:\n\t"              \
                         "subs %0, %0, #1\n\t" \
                         "bne 1b\n\t"          \
                         : "+r" (delay)        \
                         : "r" (count)         \
                         : "cc");              \
        } while (0)

#define dsb() __asm__ __volatile__ ("dsb" : : : "memory")

#if 0
#define mcDELAY_US(x)           delay_a_while((U32) (x*10))
#define mcDELAY_MS(x)           delay_a_while((U32) (x*1000))
#else
#define mcDELAY_US(x)           gpt_busy_wait_us(x)
#define mcDELAY_MS(x)           gpt_busy_wait_us(x*1000)
#endif

// Edward?? : The following needs to be porting.
#ifdef DDR_INIT_TIME_PROFILING    
#define mcSHOW_HW_MSG(_x_)     
#define mcSHOW_DBG_MSG(_x_) 
#define mcSHOW_DBG_MSG2(_x_)   
#define mcSHOW_DBG_MSG3(_x_) 
#define mcSHOW_DBG_MSG4(_x_) 
#define mcSHOW_USER_MSG(_x_)    
#define mcSHOW_DRVAPI_MSG(_x_) 
#define mcSHOW_ERR_MSG(_x_)  
#define mcFPRINTF(_x_)          
#else
    #if defined(TARGET_BUILD_VARIANT_ENG) && !defined(SLT)
        #define mcSHOW_HW_MSG(_x_)     
        #define mcSHOW_DBG_MSG(_x_) print _x_
        #define mcSHOW_DBG_MSG2(_x_)   print _x_
        #define mcSHOW_DBG_MSG3(_x_)   if (RXPERBIT_LOG_PRINT) {print _x_;}
        #define mcSHOW_DBG_MSG4(_x_)   if (RXPERBIT_LOG_PRINT) {print _x_;}
        #define mcSHOW_USER_MSG(_x_)    
        #define mcSHOW_DRVAPI_MSG(_x_)  print _x_ 
        #define mcSHOW_ERR_MSG(_x_) print _x_      
        #define mcFPRINTF(_x_)         
    #else
        #define mcSHOW_HW_MSG(_x_)     
        #define mcSHOW_DBG_MSG(_x_) 
        #define mcSHOW_DBG_MSG2(_x_)   
        #define mcSHOW_DBG_MSG3(_x_) 
        #define mcSHOW_DBG_MSG4(_x_) 
        #define mcSHOW_USER_MSG(_x_)    
        #define mcSHOW_DRVAPI_MSG(_x_) 
        #define mcSHOW_ERR_MSG(_x_)  
        #define mcFPRINTF(_x_)         
    #endif
#endif

#endif   // _DRAMC_COMMON_H_
