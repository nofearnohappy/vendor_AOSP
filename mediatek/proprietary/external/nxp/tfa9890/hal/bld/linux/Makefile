#
# Hardware Abstraction Layer  
#

TFA=../../../tfa

VPATH=../../src ../../src/lxScribo ../../src/lxScribo/scribosrv

# HAL source files.
HALSRC = NXP_I2C_linux.c 
HALOBJ = $(HALSRC:.c=.o)
HALOUT = ../../lib/libiic.a 

# Scribo client source files.
SCRIBOSRC = lxI2c.c \
	lxScribo.c \
	lxScriboSerial.c \
	lxScriboSocket.c 
SCRIBOOBJ = $(SCRIBOSRC:.c=.o)
SCRIBOOUT = ../../lib/libscribo.a 
# Scribo server source files.
SCRIBOSRVSRC = i2cserver.c cmd.c
SCRIBOSRVOBJ = $(SCRIBOSRVSRC:.c=.o)
SCRIBOSRVOUT = ../../lib/libscribosrv.a 

# Dummy source files.
DMYSRC = lxDummy.c
DMYOBJ = $(DMYSRC:.c=.o)
DMYOUT = ../../lib/libdmy.a 

SRC = $(HALSRC) $(SCRIBOSRC) $(SCRIBOSRVSRC) $(DMYSRC) 

OBJ = $(SRC:.c=.o)

OUT = ../../lib/libhal.a 

# include directories
CPPFLAGS = -I. -I../../inc -I../../src/lxScribo -I../../src/lxScribo/scribosrv -I$(TFA)/inc

# C++ compiler flags (-g -O2 -Wall)
CFLAGS = -g -O2 -Wall -fPIC

# compiler
CC = gcc

# library paths
LIBS =

# compile flags
LDFLAGS = -g

default: $(OUT) $(HALOUT) $(SCRIBOOUT) $(SCRIBOSRVOUT) $(DMYOUT)

$(OUT): $(OBJ)
	$(AR) rcs $(OUT) $(OBJ)
	ranlib $(OUT)

$(HALOUT): $(HALOBJ)
	$(AR) rcs $(HALOUT) $(HALOBJ)
	ranlib $(HALOUT)

$(DMYOUT): $(DMYOBJ)
	$(AR) rcs $(DMYOUT) $(DMYOBJ)
	ranlib $(DMYOUT)

$(SCRIBOOUT): $(SCRIBOOBJ)
	$(AR) rcs $(SCRIBOOUT) $(SCRIBOOBJ)
	ranlib $(SCRIBOOUT)

$(SCRIBOSRVOUT): $(SCRIBOSRVOBJ)
	$(AR) rcs $(SCRIBOSRVOUT) $(SCRIBOSRVOBJ)
	ranlib $(SCRIBOSRVOUT)

clean:
	rm -f $(OBJ) $(OUT) $(HALOUT) $(SCRIBOOUT) $(SCRIBOSRVOUT) $(DMYOUT) Makefile.bak 
