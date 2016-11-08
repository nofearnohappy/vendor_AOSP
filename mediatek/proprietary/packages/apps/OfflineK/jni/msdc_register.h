/*
 * msdc_register.h
 *
 *  Created on: 2014/3/5
 *      Author: MTK04314
 */

#ifndef MSDC_REGISTER_H_
#define MSDC_REGISTER_H_


/* ===================================== */
/*        register define                */
/* ===================================== */
/* common register */

// #define MSDC_CFG                (0x00)
// #define MSDC_IOCON              (0x04)
// #define MSDC_PS                 (0x08)
// #define MSDC_INT                (0x0c)
// #define MSDC_INTEN              (0x10)
// #define MSDC_FIFOCS             (0x14)
// #define MSDC_TXDATA             (0x18)
// #define MSDC_RXDATA             (0x1c)
//
// /* sdmmc register */
// #define SDC_CFG                 (0x30)
// #define SDC_CMD                 (0x34)
// #define SDC_ARG                 (0x38)
// #define SDC_STS                 (0x3c)
// #define SDC_RESP0               (0x40)
// #define SDC_RESP1               (0x44)
// #define SDC_RESP2               (0x48)
// #define SDC_RESP3               (0x4c)
// #define SDC_BLK_NUM             (0x50)
// #define SDC_CSTS                (0x58)
// #define SDC_CSTS_EN             (0x5c)
// #define SDC_DCRC_STS            (0x60)
//
// /* emmc register*/
// #define EMMC_CFG0               (0x70)
// #define EMMC_CFG1               (0x74)
// #define EMMC_STS                (0x78)
// #define EMMC_IOCON              (0x7c)
//
// /* auto command register */
// #define SDC_ACMD_RESP           (0x80)
// #define SDC_ACMD19_TRG          (0x84)
// #define SDC_ACMD19_STS          (0x88)
//
// /* dma register */
// #define MSDC_DMA_SA             (0x90)
// #define MSDC_DMA_CA             (0x94)
// #define MSDC_DMA_CTRL           (0x98)
// #define MSDC_DMA_CFG            (0x9c)
//
// /* pad ctrl register */
// #define MSDC_PAD_CTL0           (0xe0)
// #define MSDC_PAD_CTL1           (0xe4)
// #define MSDC_PAD_CTL2           (0xe8)
//
// /* data read delay */
// #define MSDC_DAT_RDDLY0         (0xf0)
// #define MSDC_DAT_RDDLY1         (0xf4)
//
// /* debug register */
// #define MSDC_DBG_SEL            (0xa0)
// #define MSDC_DBG_OUT            (0xa4)
//
// /* misc register */
// #define MSDC_PATCH_BIT          (0xb0)
// #define MSDC_PAD_TUNE           (0xec)
// #define MSDC_HW_DBG             (0xf8)
// //#define MSDC_VERSION
// //#define MSDC_ECO_VER
//


/*--------------------------------------------------------------------------*/
/* Register Offset                                                          */
/*--------------------------------------------------------------------------*/
#define MSDC_CFG         (0x0)
#define MSDC_IOCON       (0x04)
#define MSDC_PS          (0x08)
#define MSDC_INT         (0x0c)
#define MSDC_INTEN       (0x10)
#define MSDC_FIFOCS      (0x14)
#define MSDC_TXDATA      (0x18)
#define MSDC_RXDATA      (0x1c)
#define SDC_CFG          (0x30)
#define SDC_CMD          (0x34)
#define SDC_ARG          (0x38)
#define SDC_STS          (0x3c)
#define SDC_RESP0        (0x40)
#define SDC_RESP1        (0x44)
#define SDC_RESP2        (0x48)
#define SDC_RESP3        (0x4c)
#define SDC_BLK_NUM      (0x50)
#define SDC_CSTS         (0x58)
#define SDC_CSTS_EN      (0x5c)
#define SDC_DCRC_STS     (0x60)
#define EMMC_CFG0        (0x70)
#define EMMC_CFG1        (0x74)
#define EMMC_STS         (0x78)
#define EMMC_IOCON       (0x7c)
#define SDC_ACMD_RESP    (0x80)
#define SDC_ACMD19_TRG   (0x84)
#define SDC_ACMD19_STS   (0x88)
#define MSDC_DMA_SA      (0x90)
#define MSDC_DMA_CA      (0x94)
#define MSDC_DMA_CTRL    (0x98)
#define MSDC_DMA_CFG     (0x9c)
#define MSDC_DBG_SEL     (0xa0)
#define MSDC_DBG_OUT     (0xa4)
#define MSDC_DMA_LEN     (0xa8)
#define MSDC_PATCH_BIT0  (0xb0)
#define MSDC_PATCH_BIT1  (0xb4)
//#ifdef MTK_SDIO30_TEST_MODE_SUPPORT
#define DAT0_TUNE_CRC    (0xc0)
#define DAT1_TUNE_CRC    (0xc4)
#define DAT2_TUNE_CRC    (0xc8)
#define DAT3_TUNE_CRC    (0xcc)
#define CMD_TUNE_CRC     (0xd0)
#define SDIO_TUNE_WIND   (0xd4)
//#endif	// MTK_SDIO30_TEST_MODE_SUPPORT
#define MSDC_PAD_CTL0    (0xe0)
#define MSDC_PAD_CTL1    (0xe4)
#define MSDC_PAD_CTL2    (0xe8)
#define MSDC_PAD_TUNE    (0xec)
#define MSDC_DAT_RDDLY0  (0xf0)
#define MSDC_DAT_RDDLY1  (0xf4)
#define MSDC_HW_DBG      (0xf8)
#define MSDC_VERSION     (0x100)
#define MSDC_ECO_VER     (0x104)




// /* ===================================== */
// /*        register field                 */
// /* ===================================== */
// /* MSDC_CFG mask */
// #define MSDC_CFG_MODE           (0x1  << 0)     /* RW */
// #define MSDC_CFG_CKPDN          (0x1  << 1)     /* RW */
// #define MSDC_CFG_RST            (0x1  << 2)     /* RW */
// #define MSDC_CFG_PIO            (0x1  << 3)     /* RW */
// #define MSDC_CFG_CKDRVEN        (0x1  << 4)     /* RW */
// #define MSDC_CFG_BV18SDT        (0x1  << 5)     /* RW */
// #define MSDC_CFG_BV18PSS        (0x1  << 6)     /* R  */
// #define MSDC_CFG_CKSTB          (0x1  << 7)     /* R  */
// #define MSDC_CFG_CKDIV          (0xff << 8)     /* RW */
// #define MSDC_CFG_CKMOD          (0x3  << 16)    /* RW */
//
// /* MSDC_IOCON mask */
// #define MSDC_IOCON_SDR104CKS    (0x1  << 0)     /* RW */
// #define MSDC_IOCON_RSPL         (0x1  << 1)     /* RW */
// #define MSDC_IOCON_DSPL         (0x1  << 2)     /* RW */
// #define MSDC_IOCON_DDLSEL       (0x1  << 3)     /* RW */
// //#define MSDC_IOCON_DDR50CKD     (0x1  << 4)     /* RW */   /* not in document! [Fix me] */
// //#define MSDC_IOCON_DSPLSEL      (0x1  << 5)     /* RW */
// //#define MSDC_IOCON_D0SPL        (0x1  << 16)    /* RW */
// //#define MSDC_IOCON_D1SPL        (0x1  << 17)    /* RW */
// //#define MSDC_IOCON_D2SPL        (0x1  << 18)    /* RW */
// //#define MSDC_IOCON_D3SPL        (0x1  << 19)    /* RW */
// //#define MSDC_IOCON_D4SPL        (0x1  << 20)    /* RW */
// //#define MSDC_IOCON_D5SPL        (0x1  << 21)    /* RW */
// //#define MSDC_IOCON_D6SPL        (0x1  << 22)    /* RW */
// //#define MSDC_IOCON_D7SPL        (0x1  << 23)    /* RW */
// #define MSDC_IOCON_RISCSZ       (0x3  << 24)    /* RW */
//
// /* MSDC_PS mask */
// #define MSDC_PS_CDEN            (0x1  << 0)     /* RW */
// #define MSDC_PS_CDSTS           (0x1  << 1)     /* R  */
// #define MSDC_PS_CDDEBOUNCE      (0xf  << 12)    /* RW */
// #define MSDC_PS_DAT             (0xff << 16)    /* R  */
// #define MSDC_PS_DAT0            (0x1  << 16)    /* R  */
// #define MSDC_PS_CMD             (0x1  << 24)    /* R  */
// #define MSDC_PS_WP              (0x1UL<< 31)    /* R  */
//
// /* MSDC_INT mask */
// #define MSDC_INT_MMCIRQ         (0x1  << 0)     /* W1C */
// #define MSDC_INT_CDSC           (0x1  << 1)     /* W1C */
// #define MSDC_INT_ACMDRDY        (0x1  << 3)     /* W1C */
// #define MSDC_INT_ACMDTMO        (0x1  << 4)     /* W1C */
// #define MSDC_INT_ACMDCRCERR     (0x1  << 5)     /* W1C */
// #define MSDC_INT_DMAQ_EMPTY     (0x1  << 6)     /* W1C */
// #define MSDC_INT_SDIOIRQ        (0x1  << 7)     /* W1C */
// #define MSDC_INT_CMDRDY         (0x1  << 8)     /* W1C */
// #define MSDC_INT_CMDTMO         (0x1  << 9)     /* W1C */
// #define MSDC_INT_RSPCRCERR      (0x1  << 10)    /* W1C */
// #define MSDC_INT_CSTA           (0x1  << 11)    /* R */
// #define MSDC_INT_XFER_COMPL     (0x1  << 12)    /* W1C */
// #define MSDC_INT_DXFER_DONE     (0x1  << 13)    /* W1C */
// #define MSDC_INT_DATTMO         (0x1  << 14)    /* W1C */
// #define MSDC_INT_DATCRCERR      (0x1  << 15)    /* W1C */
// #define MSDC_INT_ACMD19_DONE    (0x1  << 16)    /* W1C */
//
// /* MSDC_INTEN mask */
// #define MSDC_INTEN_MMCIRQ       (0x1  << 0)     /* RW */
// #define MSDC_INTEN_CDSC         (0x1  << 1)     /* RW */
// #define MSDC_INTEN_ACMDRDY      (0x1  << 3)     /* RW */
// #define MSDC_INTEN_ACMDTMO      (0x1  << 4)     /* RW */
// #define MSDC_INTEN_ACMDCRCERR   (0x1  << 5)     /* RW */
// #define MSDC_INTEN_DMAQ_EMPTY   (0x1  << 6)     /* RW */
// #define MSDC_INTEN_SDIOIRQ      (0x1  << 7)     /* RW */
// #define MSDC_INTEN_CMDRDY       (0x1  << 8)     /* RW */
// #define MSDC_INTEN_CMDTMO       (0x1  << 9)     /* RW */
// #define MSDC_INTEN_RSPCRCERR    (0x1  << 10)    /* RW */
// #define MSDC_INTEN_CSTA         (0x1  << 11)    /* RW */
// #define MSDC_INTEN_XFER_COMPL   (0x1  << 12)    /* RW */
// #define MSDC_INTEN_DXFER_DONE   (0x1  << 13)    /* RW */
// #define MSDC_INTEN_DATTMO       (0x1  << 14)    /* RW */
// #define MSDC_INTEN_DATCRCERR    (0x1  << 15)    /* RW */
// #define MSDC_INTEN_ACMD19_DONE  (0x1  << 16)    /* RW */
//
// /* MSDC_FIFOCS mask */
// #define MSDC_FIFOCS_RXCNT       (0xff << 0)     /* R */
// #define MSDC_FIFOCS_TXCNT       (0xff << 16)    /* R */
// #define MSDC_FIFOCS_CLR         (0x1UL<< 31)    /* RW */
//
// /* SDC_CFG mask */
// //#define SDC_CFG_SDIOINTWKUP     (0x1  << 0)     /* RW */   /* not in document! [Fix me] */
// #define SDC_CFG_INSWKUP         (0x1  << 1)     /* RW */
// #define SDC_CFG_BUSWIDTH        (0x3  << 16)    /* RW */
// #define SDC_CFG_SDIO            (0x1  << 19)    /* RW */
// //#define SDC_CFG_SDIOIDE         (0x1  << 20)    /* RW */
// #define SDC_CFG_INTATGAP        (0x1  << 21)    /* RW */
// #define SDC_CFG_DTOC            (0xffUL << 24)  /* RW */
//
// /* SDC_CMD mask */
// #define SDC_CMD_OPC             (0x3f << 0)     /* RW */
// #define SDC_CMD_BRK             (0x1  << 6)     /* RW */
// #define SDC_CMD_RSPTYP          (0x7  << 7)     /* RW */
// #define SDC_CMD_DTYP            (0x3  << 11)    /* RW */
// #define SDC_CMD_RW              (0x1  << 13)    /* RW */
// #define SDC_CMD_STOP            (0x1  << 14)    /* RW */
// #define SDC_CMD_GOIRQ           (0x1  << 15)    /* RW */
// #define SDC_CMD_BLKLEN          (0xfff<< 16)    /* RW */
// #define SDC_CMD_AUTOCMD         (0x3  << 28)    /* RW */
// #define SDC_CMD_VOLSWTH         (0x1  << 30)    /* RW */
//
// /* SDC_STS mask */
// #define SDC_STS_SDCBUSY         (0x1  << 0)     /* RW */
// #define SDC_STS_CMDBUSY         (0x1  << 1)     /* RW */
// #define SDC_STS_SWR_COMPL       (0x1  << 31)    /* RW */
//
// /* SDC_DCRC_STS mask */
// #define SDC_DCRC_STS_NEG        (0xf  << 8)     /* RO */
// #define SDC_DCRC_STS_POS        (0xff << 0)     /* RO */
//
// /* EMMC_CFG0 mask */
// #define EMMC_CFG0_BOOTSTART     (0x1  << 0)     /* W */
// #define EMMC_CFG0_BOOTSTOP      (0x1  << 1)     /* W */
// #define EMMC_CFG0_BOOTMODE      (0x1  << 2)     /* RW */
// #define EMMC_CFG0_BOOTACKDIS    (0x1  << 3)     /* RW */
// #define EMMC_CFG0_BOOTWDLY      (0x7  << 12)    /* RW */
// #define EMMC_CFG0_BOOTSUPP      (0x1  << 15)    /* RW */
//
// /* EMMC_CFG1 mask */
// #define EMMC_CFG1_BOOTDATTMC    (0xfffff << 0)  /* RW */
// #define EMMC_CFG1_BOOTACKTMC    (0xfffUL << 20) /* RW */
//
// /* EMMC_STS mask */
// #define EMMC_STS_BOOTCRCERR     (0x1  << 0)     /* W1C */
// #define EMMC_STS_BOOTACKERR     (0x1  << 1)     /* W1C */
// #define EMMC_STS_BOOTDATTMO     (0x1  << 2)     /* W1C */
// #define EMMC_STS_BOOTACKTMO     (0x1  << 3)     /* W1C */
// #define EMMC_STS_BOOTUPSTATE    (0x1  << 4)     /* R */
// #define EMMC_STS_BOOTACKRCV     (0x1  << 5)     /* W1C */
// #define EMMC_STS_BOOTDATRCV     (0x1  << 6)     /* R */
//
// /* EMMC_IOCON mask */
// #define EMMC_IOCON_BOOTRST      (0x1  << 0)     /* RW */
//
// /* SDC_ACMD19_TRG mask */
// #define SDC_ACMD19_TRG_TUNESEL  (0xf  << 0)     /* RW */
//
// /* MSDC_DMA_CTRL mask */
// #define MSDC_DMA_CTRL_START     (0x1  << 0)     /* W */
// #define MSDC_DMA_CTRL_STOP      (0x1  << 1)     /* W */
// #define MSDC_DMA_CTRL_RESUME    (0x1  << 2)     /* W */
// #define MSDC_DMA_CTRL_MODE      (0x1  << 8)     /* RW */
// #define MSDC_DMA_CTRL_LASTBUF   (0x1  << 10)    /* RW */
// #define MSDC_DMA_CTRL_BRUSTSZ   (0x7  << 12)    /* RW */
// #define MSDC_DMA_CTRL_XFERSZ    (0xffffUL << 16)/* RW */
//
// /* MSDC_DMA_CFG mask */
// #define MSDC_DMA_CFG_STS        (0x1  << 0)     /* R */
// #define MSDC_DMA_CFG_DECSEN     (0x1  << 1)     /* RW */
// #define MSDC_DMA_CFG_BDCSERR    (0x1  << 4)     /* R */
// #define MSDC_DMA_CFG_GPDCSERR   (0x1  << 5)     /* R */
//
// /* MSDC_PATCH_BIT mask */
// #define MSDC_PATCH_BIT_WFLSMODE (0x1  << 0)     /* RW */
// #define MSDC_PATCH_BIT_ODDSUPP  (0x1  << 1)     /* RW */
// #define MSDC_PATCH_BIT_IODSSEL  (0x1  << 16)    /* RW */
// #define MSDC_PATCH_BIT_IOINTSEL (0x1  << 17)    /* RW */
// #define MSDC_PATCH_BIT_BUSYDLY  (0xf  << 18)    /* RW */
// #define MSDC_PATCH_BIT_WDOD     (0xf  << 22)    /* RW */
// #define MSDC_PATCH_BIT_IDRTSEL  (0x1  << 26)    /* RW */
// #define MSDC_PATCH_BIT_CMDFSEL  (0x1  << 27)    /* RW */
// #define MSDC_PATCH_BIT_INTDLSEL (0x1  << 28)    /* RW */
// #define MSDC_PATCH_BIT_SPCPUSH  (0x1  << 29)    /* RW */
// #define MSDC_PATCH_BIT_DECRCTMO (0x1  << 30)    /* RW */
//
// /* MSDC_PAD_CTL0 mask */
// #define MSDC_PAD_CTL0_CLKDRVN   (0x7  << 0)     /* RW */
// #define MSDC_PAD_CTL0_CLKDRVP   (0x7  << 4)     /* RW */
// #define MSDC_PAD_CTL0_CLKSR     (0x1  << 8)     /* RW */
// #define MSDC_PAD_CTL0_CLKPD     (0x1  << 16)    /* RW */
// #define MSDC_PAD_CTL0_CLKPU     (0x1  << 17)    /* RW */
// #define MSDC_PAD_CTL0_CLKSMT    (0x1  << 18)    /* RW */
// #define MSDC_PAD_CTL0_CLKIES    (0x1  << 19)    /* RW */
// #define MSDC_PAD_CTL0_CLKTDSEL  (0xf  << 20)    /* RW */
// #define MSDC_PAD_CTL0_CLKRDSEL  (0xffUL<< 24)   /* RW */
//
// /* MSDC_PAD_CTL1 mask */
// #define MSDC_PAD_CTL1_CMDDRVN   (0x7  << 0)     /* RW */
// #define MSDC_PAD_CTL1_CMDDRVP   (0x7  << 4)     /* RW */
// #define MSDC_PAD_CTL1_CMDSR     (0x1  << 8)     /* RW */
// #define MSDC_PAD_CTL1_CMDPD     (0x1  << 16)    /* RW */
// #define MSDC_PAD_CTL1_CMDPU     (0x1  << 17)    /* RW */
// #define MSDC_PAD_CTL1_CMDSMT    (0x1  << 18)    /* RW */
// #define MSDC_PAD_CTL1_CMDIES    (0x1  << 19)    /* RW */
// #define MSDC_PAD_CTL1_CMDTDSEL  (0xf  << 20)    /* RW */
// #define MSDC_PAD_CTL1_CMDRDSEL  (0xffUL<< 24)   /* RW */
//
// /* MSDC_PAD_CTL2 mask */
// #define MSDC_PAD_CTL2_DATDRVN   (0x7  << 0)     /* RW */
// #define MSDC_PAD_CTL2_DATDRVP   (0x7  << 4)     /* RW */
// #define MSDC_PAD_CTL2_DATSR     (0x1  << 8)     /* RW */
// #define MSDC_PAD_CTL2_DATPD     (0x1  << 16)    /* RW */
// #define MSDC_PAD_CTL2_DATPU     (0x1  << 17)    /* RW */
// #define MSDC_PAD_CTL2_DATIES    (0x1  << 19)    /* RW */
// #define MSDC_PAD_CTL2_DATSMT    (0x1  << 18)    /* RW */
// #define MSDC_PAD_CTL2_DATTDSEL  (0xf  << 20)    /* RW */
// #define MSDC_PAD_CTL2_DATRDSEL  (0xffUL<< 24)   /* RW */
//
// /* MSDC_PAD_TUNE mask */
// #define MSDC_PAD_TUNE_DATWRDLY  (0x1F << 0)     /* RW */
// #define MSDC_PAD_TUNE_DATRRDLY  (0x1F << 8)     /* RW */
// #define MSDC_PAD_TUNE_CMDRDLY   (0x1F << 16)    /* RW */
// #define MSDC_PAD_TUNE_CMDRRDLY  (0x1FUL << 22)  /* RW */
// #define MSDC_PAD_TUNE_CLKTXDLY  (0x1FUL << 27)  /* RW */
//
// /* MSDC_DAT_RDDLY0/1 mask */
// #define MSDC_DAT_RDDLY0_D0      (0x1F << 0)     /* RW */
// #define MSDC_DAT_RDDLY0_D1      (0x1F << 8)     /* RW */
// #define MSDC_DAT_RDDLY0_D2      (0x1F << 16)    /* RW */
// #define MSDC_DAT_RDDLY0_D3      (0x1F << 24)    /* RW */
//
// #define MSDC_DAT_RDDLY1_D4      (0x1F << 0)     /* RW */
// #define MSDC_DAT_RDDLY1_D5      (0x1F << 8)     /* RW */
// #define MSDC_DAT_RDDLY1_D6      (0x1F << 16)    /* RW */
// #define MSDC_DAT_RDDLY1_D7      (0x1F << 24)    /* RW */
//







/* ===================================== */
/*       OFFSET               */
/* ===================================== */
/* MSDC_CFG mask */
#define OFFSET_MSDC_CFG_MODE               (0)     /* RW */
#define OFFSET_MSDC_CFG_CKPDN              (1)     /* RW */
#define OFFSET_MSDC_CFG_RST                (2)     /* RW */
#define OFFSET_MSDC_CFG_PIO                (3)     /* RW */
#define OFFSET_MSDC_CFG_CKDRVEN            (4)     /* RW */
#define OFFSET_MSDC_CFG_BV18SDT            (5)     /* RW */
#define OFFSET_MSDC_CFG_BV18PSS            (6)     /* R  */
#define OFFSET_MSDC_CFG_CKSTB              (7)     /* R  */
#define OFFSET_MSDC_CFG_CKDIV              (8)     /* RW */
#define OFFSET_MSDC_CFG_CKMOD              (16)    /* RW */

/* MSDC_IOCON mask */
#define OFFSET_MSDC_IOCON_SDR104CKS        (0)     /* RW */
#define OFFSET_MSDC_IOCON_RSPL             (1)     /* RW */
#define OFFSET_MSDC_IOCON_DSPL             (2)     /* RW */
#define OFFSET_MSDC_IOCON_DDLSEL           (3)     /* RW */
#define OFFSET_MSDC_IOCON_DDR50CKD         (4)     /* RW */   /* not in document! [Fix me] */
#define OFFSET_MSDC_IOCON_DSPLSEL          (5)     /* RW */

#define OFFSET_MSDC_IOCON_WDSPL            (8)     /* RW */
#define OFFSET_MSDC_IOCON_WDSPLSEL         (9)     /* RW */
#define OFFSET_MSDC_IOCON_WD0SPL           (10)     /* RW */

#define OFFSET_MSDC_IOCON_D0SPL            (16)    /* RW */
#define OFFSET_MSDC_IOCON_D1SPL            (17)    /* RW */
#define OFFSET_MSDC_IOCON_D2SPL            (18)    /* RW */
#define OFFSET_MSDC_IOCON_D3SPL            (19)    /* RW */
#define OFFSET_MSDC_IOCON_D4SPL            (20)    /* RW */
#define OFFSET_MSDC_IOCON_D5SPL            (21)    /* RW */
#define OFFSET_MSDC_IOCON_D6SPL            (22)    /* RW */
#define OFFSET_MSDC_IOCON_D7SPL            (23)    /* RW */
#define OFFSET_MSDC_IOCON_RISCSZ           (24)    /* RW */

/* MSDC_PS mask */
#define OFFSET_MSDC_PS_CDEN                (0)     /* RW */
#define OFFSET_MSDC_PS_CDSTS               (1)     /* R  */
#define OFFSET_MSDC_PS_CDDEBOUNCE          (12)    /* RW */
#define OFFSET_MSDC_PS_DAT                 (16)    /* R  */
#define OFFSET_MSDC_PS_DAT0                (16)    /* R  */
#define OFFSET_MSDC_PS_CMD                 (24)    /* R  */
#define OFFSET_MSDC_PS_WP                  (31)    /* R  */

/* MSDC_INT mask */
#define OFFSET_MSDC_INT_MMCIRQ             (0)     /* W1C */
#define OFFSET_MSDC_INT_CDSC               (1)     /* W1C */
#define OFFSET_MSDC_INT_ACMDRDY            (3)     /* W1C */
#define OFFSET_MSDC_INT_ACMDTMO            (4)     /* W1C */
#define OFFSET_MSDC_INT_ACMDCRCERR         (5)     /* W1C */
#define OFFSET_MSDC_INT_DMAQ_EMPTY         (6)     /* W1C */
#define OFFSET_MSDC_INT_SDIOIRQ            (7)     /* W1C */
#define OFFSET_MSDC_INT_CMDRDY             (8)     /* W1C */
#define OFFSET_MSDC_INT_CMDTMO             (9)     /* W1C */
#define OFFSET_MSDC_INT_RSPCRCERR          (10)    /* W1C */
#define OFFSET_MSDC_INT_CSTA               (11)    /* R */
#define OFFSET_MSDC_INT_XFER_COMPL         (12)    /* W1C */
#define OFFSET_MSDC_INT_DXFER_DONE         (13)    /* W1C */
#define OFFSET_MSDC_INT_DATTMO             (14)    /* W1C */
#define OFFSET_MSDC_INT_DATCRCERR          (15)    /* W1C */
#define OFFSET_MSDC_INT_ACMD19_DONE        (16)    /* W1C */

/* MSDC_INTEN mask */
#define OFFSET_MSDC_INTEN_MMCIRQ           (0)     /* RW */
#define OFFSET_MSDC_INTEN_CDSC             (1)     /* RW */
#define OFFSET_MSDC_INTEN_ACMDRDY          (3)     /* RW */
#define OFFSET_MSDC_INTEN_ACMDTMO          (4)     /* RW */
#define OFFSET_MSDC_INTEN_ACMDCRCERR       (5)     /* RW */
#define OFFSET_MSDC_INTEN_DMAQ_EMPTY       (6)     /* RW */
#define OFFSET_MSDC_INTEN_SDIOIRQ          (7)     /* RW */
#define OFFSET_MSDC_INTEN_CMDRDY           (8)     /* RW */
#define OFFSET_MSDC_INTEN_CMDTMO           (9)     /* RW */
#define OFFSET_MSDC_INTEN_RSPCRCERR        (10)    /* RW */
#define OFFSET_MSDC_INTEN_CSTA             (11)    /* RW */
#define OFFSET_MSDC_INTEN_XFER_COMPL       (12)    /* RW */
#define OFFSET_MSDC_INTEN_DXFER_DONE       (13)    /* RW */
#define OFFSET_MSDC_INTEN_DATTMO           (14)    /* RW */
#define OFFSET_MSDC_INTEN_DATCRCERR        (15)    /* RW */
#define OFFSET_MSDC_INTEN_ACMD19_DONE      (16)    /* RW */

/* MSDC_FIFOCS mask */
#define OFFSET_MSDC_FIFOCS_RXCNT           (0)     /* R */
#define OFFSET_MSDC_FIFOCS_TXCNT           (16)    /* R */
#define OFFSET_MSDC_FIFOCS_CLR             (31)    /* RW */

/* SDC_CFG mask */
#define SDC_CFG_SDIOINTWKUP                (0)     /* RW */   /* not in document! [Fix me] */
#define OFFSET_SDC_CFG_INSWKUP             (1)     /* RW */
#define OFFSET_SDC_CFG_BUSWIDTH            (16)    /* RW */
#define OFFSET_SDC_CFG_SDIO                (19)    /* RW */
#define SDC_CFG_SDIOIDE                    (20)    /* RW */
#define OFFSET_SDC_CFG_INTATGAP            (21)    /* RW */
#define OFFSET_SDC_CFG_DTOC                (24)    /* RW */

/* SDC_CMD mask */
#define OFFSET_SDC_CMD_OPC                 (0)     /* RW */
#define OFFSET_SDC_CMD_BRK                 (6)     /* RW */
#define OFFSET_SDC_CMD_RSPTYP              (7)     /* RW */
#define OFFSET_SDC_CMD_DTYP                (11)    /* RW */
#define OFFSET_SDC_CMD_RW                  (13)    /* RW */
#define OFFSET_SDC_CMD_STOP                (14)    /* RW */
#define OFFSET_SDC_CMD_GOIRQ               (15)    /* RW */
#define OFFSET_SDC_CMD_BLKLEN              (16)    /* RW */
#define OFFSET_SDC_CMD_AUTOCMD             (28)    /* RW */
#define OFFSET_SDC_CMD_VOLSWTH             (30)    /* RW */

/* SDC_SOFFSET_TS mask */
#define OFFSET_SDC_STS_SDCBUSY             (0)     /* RW */
#define OFFSET_SDC_STS_CMDBUSY             (1)     /* RW */
#define OFFSET_SDC_STS_SWR_COMPL           (31)    /* RW */

/* SDC_DCRC_STS mask */
#define OFFSET_SDC_DCRC_STS_NEG            (8)     /* RO */
#define OFFSET_SDC_DCRC_STS_POS            (0)     /* RO */

/* EMMC_CFG0 mask */
#define OFFSET_EMMC_CFG0_BOOTSTART         (0)     /* W */
#define OFFSET_EMMC_CFG0_BOOTSTOP          (1)     /* W */
#define OFFSET_EMMC_CFG0_BOOTMODE          (2)     /* RW */
#define OFFSET_EMMC_CFG0_BOOTACKDIS        (3)     /* RW */
#define OFFSET_EMMC_CFG0_BOOTWDLY          (12)    /* RW */
#define OFFSET_EMMC_CFG0_BOOTSUPP          (15)    /* RW */

/* EMMC_CFG1 mask */
#define OFFSET_EMMC_CFG1_BOOTDATTMC        (0)  /* RW */
#define OFFSET_EMMC_CFG1_BOOTACKTMC        (20) /* RW */

/* EMMC_STS mask */
#define OFFSET_EMMC_STS_BOOTCRCERR         (0)     /* W1C */
#define OFFSET_EMMC_STS_BOOTACKERR         (1)     /* W1C */
#define OFFSET_EMMC_STS_BOOTDATTMO         (2)     /* W1C */
#define OFFSET_EMMC_STS_BOOTACKTMO         (3)     /* W1C */
#define OFFSET_EMMC_STS_BOOTUPSTATE        (4)     /* R */
#define OFFSET_EMMC_STS_BOOTACKRCV         (5)     /* W1C */
#define OFFSET_EMMC_STS_BOOTDATRCV         (6)     /* R */

/* EMMC_IOCON mask */
#define OFFSET_EMMC_IOCON_BOOTRST          (0)     /* RW */

/* SDC_ACMD19_TRG mask */
#define OFFSET_SDC_ACMD19_TRG_TUNESEL      (0)     /* RW */

/* MSDC_DMA_CTRL mask */
#define OFFSET_MSDC_DMA_CTRL_START         (0)     /* W */
#define OFFSET_MSDC_DMA_CTRL_STOP          (1)     /* W */
#define OFFSET_MSDC_DMA_CTRL_RESUME        (2)     /* W */
#define OFFSET_MSDC_DMA_CTRL_MODE          (8)     /* RW */
#define OFFSET_MSDC_DMA_CTRL_LASTBUF       (10)    /* RW */
#define OFFSET_MSDC_DMA_CTRL_BRUSTSZ       (12)    /* RW */
#define OFFSET_MSDC_DMA_CTRL_XFERSZ        (16)/* RW */

/* MSDC_DMA_CFG mask */
#define OFFSET_MSDC_DMA_CFG_STS            (0)     /* R */
#define OFFSET_MSDC_DMA_CFG_DECSEN         (1)     /* RW */
#define OFFSET_MSDC_DMA_CFG_BDCSERR        (4)     /* R */
#define OFFSET_MSDC_DMA_CFG_GPDCSERR       (5)     /* R */

/* MSDC_PATCH_BIT0 mask */
#define OFFSET_MSDC_PATCH_BIT0_WFLSMODE     (0)     /* RW */
#define OFFSET_MSDC_PATCH_BIT0_ODDSUPP      (1)     /* RW */
#define OFFSET_MSDC_PATCH_BIT0_IODSSEL      (16)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_IOINTSEL     (17)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_BUSYDLY      (18)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_WDOD         (22)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_IDRTSEL      (26)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_CMDFSEL      (27)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_INTDLSEL     (28)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_SPCPUSH      (29)    /* RW */
#define OFFSET_MSDC_PATCH_BIT0_DECRCTMO     (30)    /* RW */

#define OFFSET_MSDC_PATCH_BIT0_INTCKSEL     (7)     /* RW */
#define OFFSET_MSDC_PATCH_BIT0_CKGENDLSEL   (10)    /* RW */



/* MSDC_PATCH_BIT1 mask */
#define OFFSET_MSDC_PATCH_BIT1_WRDAT_CRCS  (0)
#define OFFSET_MSDC_PATCH_BIT1_CMD_RSP     (3)


/* MSDC_PAD_CTL0 mask */
#define OFFSET_MSDC_PAD_CTL0_CLKDRVN       (0)     /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKDRVP       (4)     /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKSR         (8)     /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKPD         (16)    /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKPU         (17)    /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKSMT        (18)    /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKIES        (19)    /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKTDSEL      (20)    /* RW */
#define OFFSET_MSDC_PAD_CTL0_CLKRDSEL      (24)   /* RW */

/* MSDC_PAD_CTL1 mask */
#define OFFSET_MSDC_PAD_CTL1_CMDDRVN       (0)     /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDDRVP       (4)     /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDSR         (8)     /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDPD         (16)    /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDPU         (17)    /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDSMT        (18)    /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDIES        (19)    /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDTDSEL      (20)    /* RW */
#define OFFSET_MSDC_PAD_CTL1_CMDRDSEL      (24)   /* RW */

/* MSDC_PAD_CTL2 mask */
#define OFFSET_MSDC_PAD_CTL2_DATDRVN       (0)     /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATDRVP       (4)     /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATSR         (8)     /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATPD         (16)    /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATPU         (17)    /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATIES        (19)    /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATSMT        (18)    /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATTDSEL      (20)    /* RW */
#define OFFSET_MSDC_PAD_CTL2_DATRDSEL      (24)   /* RW */

/* MSDC_PAD_TUNE mask */
#define OFFSET_MSDC_PAD_TUNE_DATWRDLY      (0)     /* RW */
#define OFFSET_MSDC_PAD_TUNE_DATRRDLY      (8)     /* RW */
#define OFFSET_MSDC_PAD_TUNE_CMDRDLY       (16)    /* RW */
#define OFFSET_MSDC_PAD_TUNE_CMDRRDLY      (22)  /* RW */
#define OFFSET_MSDC_PAD_TUNE_CLKTXDLY      (27)  /* RW */

/* MSDC_DAT_RDDLY0/1 mask */
#define OFFSET_MSDC_DAT_RDDLY0_D0          (0)     /* RW */
#define OFFSET_MSDC_DAT_RDDLY0_D1          (8)     /* RW */
#define OFFSET_MSDC_DAT_RDDLY0_D2          (16)    /* RW */
#define OFFSET_MSDC_DAT_RDDLY0_D3          (24)    /* RW */

#define OFFSET_MSDC_DAT_RDDLY1_D4          (0)     /* RW */
#define OFFSET_MSDC_DAT_RDDLY1_D5          (8)     /* RW */
#define OFFSET_MSDC_DAT_RDDLY1_D6          (16)    /* RW */
#define OFFSET_MSDC_DAT_RDDLY1_D7          (24)    /* RW */








/* ===================================== */
/*        LENGTH field                 */
/* ===================================== */
/* MSDC_CFG mask */
#define LEN_MSDC_CFG_MODE                   (1)    /* RW */
#define LEN_MSDC_CFG_CKPDN                  (1)    /* RW */
#define LEN_MSDC_CFG_RST                    (1)    /* RW */
#define LEN_MSDC_CFG_PIO                    (1)    /* RW */
#define LEN_MSDC_CFG_CKDRVEN                (1)    /* RW */
#define LEN_MSDC_CFG_BV18SDT                (1)    /* RW */
#define LEN_MSDC_CFG_BV18PSS                (1)    /* R  */
#define LEN_MSDC_CFG_CKSTB                  (1)    /* R  */
#define LEN_MSDC_CFG_CKDIV                  (8)    /* RW */
#define LEN_MSDC_CFG_CKMOD                  (2)    /* RW */

/* MSDC_IOCON mask */
#define LEN_MSDC_IOCON_SDR104CKS            (1)    /* RW */
#define LEN_MSDC_IOCON_RSPL                 (1)    /* RW */
#define LEN_MSDC_IOCON_DSPL                 (1)    /* RW */
#define LEN_MSDC_IOCON_DDLSEL               (1)    /* RW */
#define LEN_MSDC_IOCON_DDR50CKD             (1)    /* RW */   /* not in document! [Fix me] */
#define LEN_MSDC_IOCON_DSPLSEL              (1)    /* RW */

#define LEN_MSDC_IOCON_WDSPL                (1)    /* RW */
#define LEN_MSDC_IOCON_WDSPLSEL             (1)    /* RW */
#define LEN_MSDC_IOCON_WD0SPL               (1)    /* RW */

#define LEN_MSDC_IOCON_D0SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D1SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D2SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D3SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D4SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D5SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D6SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_D7SPL                (1)    /* RW */
#define LEN_MSDC_IOCON_RISCSZ               (2)    /* RW */

/* MSDC_PS mask */
#define LEN_MSDC_PS_CDEN                    (1)    /* RW */
#define LEN_MSDC_PS_CDSTS                   (1)    /* R  */
#define LEN_MSDC_PS_CDDEBOUNCE              (4)    /* RW */
#define LEN_MSDC_PS_DAT                     (8)    /* R  */
#define LEN_MSDC_PS_DAT0                    (1)    /* R  */
#define LEN_MSDC_PS_CMD                     (1)    /* R  */
#define LEN_MSDC_PS_WP                      (1)    /* R  */

/* MSDC_INT mask */
#define LEN_MSDC_INT_MMCIRQ                 (1)    /* W1C */
#define LEN_MSDC_INT_CDSC                   (1)    /* W1C */
#define LEN_MSDC_INT_ACMDRDY                (1)    /* W1C */
#define LEN_MSDC_INT_ACMDTMO                (1)    /* W1C */
#define LEN_MSDC_INT_ACMDCRCERR             (1)    /* W1C */
#define LEN_MSDC_INT_DMAQ_EMPTY             (1)    /* W1C */
#define LEN_MSDC_INT_SDIOIRQ                (1)    /* W1C */
#define LEN_MSDC_INT_CMDRDY                 (1)    /* W1C */
#define LEN_MSDC_INT_CMDTMO                 (1)    /* W1C */
#define LEN_MSDC_INT_RSPCRCERR              (1)    /* W1C */
#define LEN_MSDC_INT_CSTA                   (1)    /* R */
#define LEN_MSDC_INT_XFER_COMPL             (1)    /* W1C */
#define LEN_MSDC_INT_DXFER_DONE             (1)    /* W1C */
#define LEN_MSDC_INT_DATTMO                 (1)    /* W1C */
#define LEN_MSDC_INT_DATCRCERR              (1)    /* W1C */
#define LEN_MSDC_INT_ACMD19_DONE            (1)    /* W1C */

/* MSDC_INTEN mask */
#define LEN_MSDC_INTEN_MMCIRQ               (1)    /* RW */
#define LEN_MSDC_INTEN_CDSC                 (1)    /* RW */
#define LEN_MSDC_INTEN_ACMDRDY              (1)    /* RW */
#define LEN_MSDC_INTEN_ACMDTMO              (1)    /* RW */
#define LEN_MSDC_INTEN_ACMDCRCERR           (1)    /* RW */
#define LEN_MSDC_INTEN_DMAQ_EMPTY           (1)    /* RW */
#define LEN_MSDC_INTEN_SDIOIRQ              (1)    /* RW */
#define LEN_MSDC_INTEN_CMDRDY               (1)    /* RW */
#define LEN_MSDC_INTEN_CMDTMO               (1)    /* RW */
#define LEN_MSDC_INTEN_RSPCRCERR            (1)    /* RW */
#define LEN_MSDC_INTEN_CSTA                 (1)    /* RW */
#define LEN_MSDC_INTEN_XFER_COMPL           (1)    /* RW */
#define LEN_MSDC_INTEN_DXFER_DONE           (1)    /* RW */
#define LEN_MSDC_INTEN_DATTMO               (1)    /* RW */
#define LEN_MSDC_INTEN_DATCRCERR            (1)    /* RW */
#define LEN_MSDC_INTEN_ACMD19_DONE          (1)    /* RW */

/* MSDC_FIFOCS mask */
#define LEN_MSDC_FIFOCS_RXCNT               (8)    /* R */
#define LEN_MSDC_FIFOCS_TXCNT               (8)    /* R */
#define LEN_MSDC_FIFOCS_CLR                 (1)    /* RW */

/* SDC_CFG mask */
#define LEN_SDC_CFG_SDIOINTWKUP             (1)    /* RW */   /* not in document! [Fix me] */
#define LEN_SDC_CFG_INSWKUP                 (1)    /* RW */
#define LEN_SDC_CFG_BUSWIDTH                (2)    /* RW */
#define LEN_SDC_CFG_SDIO                    (1)    /* RW */
#define LEN_SDC_CFG_SDIOIDE                 (1)    /* RW */
#define LEN_SDC_CFG_INTATGAP                (1)    /* RW */
#define LEN_SDC_CFG_DTOC                    (8)  /* RW */

/* SDC_CMD mask */
#define LEN_SDC_CMD_OPC                     (6)    /* RW */
#define LEN_SDC_CMD_BRK                     (1)    /* RW */
#define LEN_SDC_CMD_RSPTYP                  (3)    /* RW */
#define LEN_SDC_CMD_DTYP                    (2)    /* RW */
#define LEN_SDC_CMD_RW                      (1)    /* RW */
#define LEN_SDC_CMD_STOP                    (1)    /* RW */
#define LEN_SDC_CMD_GOIRQ                   (1)    /* RW */
#define LEN_SDC_CMD_BLKLEN                  (1)    /* RW */
#define LEN_SDC_CMD_AUTOCMD                 (2)    /* RW */
#define LEN_SDC_CMD_VOLSWTH                 (1)    /* RW */

/* SDC_STS mask */
#define LEN_SDC_STS_SDCBUSY                 (1)    /* RW */
#define LEN_SDC_STS_CMDBUSY                 (1)    /* RW */
#define LEN_SDC_STS_SWR_COMPL               (1)    /* RW */

/* SDC_DCRC_STS mask */
#define LEN_SDC_DCRC_STS_NEG                (4)    /* RO */
#define LEN_SDC_DCRC_STS_POS                (8)    /* RO */

/* EMMC_CFG0 mask */
#define LEN_EMMC_CFG0_BOOTSTART             (1)    /* W */
#define LEN_EMMC_CFG0_BOOTSTOP              (1)    /* W */
#define LEN_EMMC_CFG0_BOOTMODE              (1)    /* RW */
#define LEN_EMMC_CFG0_BOOTACKDIS            (1)    /* RW */
#define LEN_EMMC_CFG0_BOOTWDLY              (3)    /* RW */
#define LEN_EMMC_CFG0_BOOTSUPP              (1)    /* RW */

/* EMMC_CFG1 mask */
#define LEN_EMMC_CFG1_BOOTDATTMC            (1) /* RW */
#define LEN_EMMC_CFG1_BOOTACKTMC            (1) /* RW */

/* EMMC_STS mask */
#define LEN_EMMC_STS_BOOTCRCERR             (1)    /* W1C */
#define LEN_EMMC_STS_BOOTACKERR             (1)    /* W1C */
#define LEN_EMMC_STS_BOOTDATTMO             (1)    /* W1C */
#define LEN_EMMC_STS_BOOTACKTMO             (1)    /* W1C */
#define LEN_EMMC_STS_BOOTUPSTATE            (1)    /* R */
#define LEN_EMMC_STS_BOOTACKRCV             (1)    /* W1C */
#define LEN_EMMC_STS_BOOTDATRCV             (1)    /* R */

/* EMMC_IOCON mask */
#define LEN_EMMC_IOCON_BOOTRST              (1)    /* RW */

/* SDC_ACMD19_TRG mask */
#define LEN_SDC_ACMD19_TRG_TUNESEL          (4)    /* RW */

/* MSDC_DMA_CTRL mask */
#define LEN_MSDC_DMA_CTRL_START             (1)    /* W */
#define LEN_MSDC_DMA_CTRL_STOP              (1)    /* W */
#define LEN_MSDC_DMA_CTRL_RESUME            (1)    /* W */
#define LEN_MSDC_DMA_CTRL_MODE              (1)    /* RW */
#define LEN_MSDC_DMA_CTRL_LASTBUF           (1)    /* RW */
#define LEN_MSDC_DMA_CTRL_BRUSTSZ           (3)    /* RW */
#define LEN_MSDC_DMA_CTRL_XFERSZ            (1)/* RW */

/* MSDC_DMA_CFG mask */
#define LEN_MSDC_DMA_CFG_STS                (1)    /* R */
#define LEN_MSDC_DMA_CFG_DECSEN             (1)    /* RW */
#define LEN_MSDC_DMA_CFG_BDCSERR            (1)    /* R */
#define LEN_MSDC_DMA_CFG_GPDCSERR           (1)    /* R */

/* MSDC_PATCH_BIT mask */
#define LEN_MSDC_PATCH_BIT0_WFLSMODE         (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_ODDSUPP          (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_IODSSEL          (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_IOINTSEL         (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_BUSYDLY          (4)    /* RW */
#define LEN_MSDC_PATCH_BIT0_WDOD             (4)    /* RW */
#define LEN_MSDC_PATCH_BIT0_IDRTSEL          (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_CMDFSEL          (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_INTDLSEL         (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_SPCPUSH          (1)    /* RW */
#define LEN_MSDC_PATCH_BIT0_DECRCTMO         (1)    /* RW */

#define LEN_MSDC_PATCH_BIT0_INTCKSEL         (3)     /* RW */
#define LEN_MSDC_PATCH_BIT0_CKGENDLSEL       (5)    /* RW */


/* MSDC_PATCH_BIT1 mask */
#define LEN_MSDC_PATCH_BIT1_WRDAT_CRCS   (3)
#define LEN_MSDC_PATCH_BIT1_CMD_RSP      (3)


/* MSDC_PAD_CTL0 mask */
#define LEN_MSDC_PAD_CTL0_CLKDRVN           (3)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKDRVP           (3)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKSR             (1)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKPD             (1)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKPU             (1)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKSMT            (1)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKIES            (1)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKTDSEL          (4)    /* RW */
#define LEN_MSDC_PAD_CTL0_CLKRDSEL          (8)   /* RW */

/* MSDC_PAD_CTL1 mask */
#define LEN_MSDC_PAD_CTL1_CMDDRVN           (3)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDDRVP           (3)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDSR             (1)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDPD             (1)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDPU             (1)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDSMT            (1)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDIES            (1)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDTDSEL          (4)    /* RW */
#define LEN_MSDC_PAD_CTL1_CMDRDSEL          (8)   /* RW */

/* MSDC_PAD_CTL2 mask */
#define LEN_MSDC_PAD_CTL2_DATDRVN           (3)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATDRVP           (3)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATSR             (1)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATPD             (1)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATPU             (1)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATIES            (1)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATSMT            (1)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATTDSEL          (4)    /* RW */
#define LEN_MSDC_PAD_CTL2_DATRDSEL          (8)   /* RW */

/* MSDC_PAD_TUNE mask */
#define LEN_MSDC_PAD_TUNE_DATWRDLY          (5)    /* RW */
#define LEN_MSDC_PAD_TUNE_DATRRDLY          (5)    /* RW */
#define LEN_MSDC_PAD_TUNE_CMDRDLY           (5)    /* RW */
#define LEN_MSDC_PAD_TUNE_CMDRRDLY          (5)  /* RW */
#define LEN_MSDC_PAD_TUNE_CLKTXDLY          (5)  /* RW */

/* MSDC_DAT_RDDLY0/1 mask */
#define LEN_MSDC_DAT_RDDLY0_D0              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY0_D1              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY0_D2              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY0_D3              (5)    /* RW */

#define LEN_MSDC_DAT_RDDLY1_D4              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY1_D5              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY1_D6              (5)    /* RW */
#define LEN_MSDC_DAT_RDDLY1_D7              (5)    /* RW */







/* ===================================== */
/*        register read/write            */
/* ===================================== */
static inline unsigned int uffs(unsigned int x)
{
    unsigned int r = 1;

    if (!x)
        return 0;
    if (!(x & 0xffff)) {
        x >>= 16;
        r += 16;
    }
    if (!(x & 0xff)) {
        x >>= 8;
        r += 8;
    }
    if (!(x & 0xf)) {
        x >>= 4;
        r += 4;
    }
    if (!(x & 3)) {
        x >>= 2;
        r += 2;
    }
    if (!(x & 1)) {
        x >>= 1;
        r += 1;
    }
    return r;
}





#endif /* MSDC_REGISTER_H_ */
