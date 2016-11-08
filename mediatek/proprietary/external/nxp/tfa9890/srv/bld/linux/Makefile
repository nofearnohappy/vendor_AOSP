#
# Hardware Abstraction Layer  
#

SRV=../..
HAL=../../../hal
TFA=../../../tfa
HALLIB=$(HAL)/lib
TFALIB=$(TFA)/lib

VPATH=../../src ../../src/lxScribo

# source file
SRC =nxpTfa98xx.c \
		tfa98xxRuntime.c \
        tfa98xxCalibration.c \
        tfa98xxLiveData.c \
        tfa98xxDiagnostics.c
	
OBJ = $(SRC:.c=.o)

OUT = ../../lib/libsrv.a 

# include directories
CPPFLAGS = -I. -I$(HAL)/inc -I$(TFA)/inc -I$(SRV)/inc

# include library search path
LIBPATH = -L. -L$(HALLIB) -L$(TFALIB)

# C++ compiler flags (-g -O2 -Wall)
CFLAGS = -g -O2 -Wall -fPIC

# compiler
CC = gcc

# library paths
LIBS = -ltfa_hal -ltfa -lm 

# compile flags
LDFLAGS = -g

default: $(OUT)

$(OUT): $(OBJ)
	$(AR) rcs $(OUT) $(OBJ)
	ranlib $(OUT)

clean:
	rm -f $(OBJ) $(OUT) Makefile.bak 
