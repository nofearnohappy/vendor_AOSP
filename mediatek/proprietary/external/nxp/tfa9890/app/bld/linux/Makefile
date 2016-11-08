#
# Climax and examples makefile
#

# path to other components
APP=../..
SRV=../../../srv
HAL=../../../hal
TFA=../../../tfa
HALLIB=$(HAL)/lib
TFALIB=$(TFA)/lib
SRVLIB=$(SRV)/lib

# include directories
CPPFLAGS = -I. -I$(HAL)/inc -I$(TFA)/inc -I$(SRV)/inc
CFLAGS += -g

# include library search path
LIBPATH = -L. -L$(HALLIB) -L$(TFALIB) -L$(SRVLIB)

# compiler
CC = gcc

# include library
LIBS = -liic -lhal -lscribo -lscribosrv -ltfa -lsrv -lm 

# compile flags
LDFLAGS = -g

default: climax 

all: climax exTfa98xx exTfa98xxStereo 

# for building climax
CLIMAX=../../climax
VPATH=$(CLIMAX)/inc $(CLIMAX)/src $(CLIMAX)/src/cli
SRC = climax.c \
	cliCommands.c cmdline.c 
OBJ = $(SRC:.c=.o)
CPPFLAGS := $(CPPFLAGS) -I$(APP)/climax/inc
CCFLAGS = -O0 -DLXSCRIBO -DLOGERR -Wall -Wno-unused -g
OUT = climax
climax: $(OBJ)
	$(CC) -o $@ $(CPPFLAGS) $(LDFLAGS) $^ $(LIBPATH) -Wl,--start-group $(LIBS) -Wl,--end-group 

arm : CC= arm-linux-gnueabi-gcc

# for building the examples	
exTfa98xx: ../../exTfa98xx/src/main_mono.o 
	$(CC) -o $@ $(CPPFLAGS) $(LDFLAGS) $^ $(LIBPATH) -Wl,--start-group $(LIBS) -Wl,--end-group 
exTfa98xxStereo: ../../exTfa98xx/src/main_stereo.o 
	$(CC) -o $@ $(CPPFLAGS) $(LDFLAGS) $^ $(LIBPATH) -Wl,--start-group $(LIBS) -Wl,--end-group 
    
clean:
	rm -f $(OBJ)  climax Makefile.bak 

cleanall:
	rm -f $(LIB_OBJ) $(OBJ) climax exTfa98xx exTfa98xxStereo Makefile.bak 
