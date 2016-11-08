#
# common file for all make file parameters
#

# source files.
TFA=../..
HAL=../../../hal
HALLIB=../../../hal/lib
TFALIB=../../lib

VPATH=../../src
SRC = $(wildcard $(VPATH)/*.c) 

OBJ = $(SRC:.c=.o)

OUT = ../../lib/libtfa.a 

# include directories
CPPFLAGS = -I. -I$(HAL)/inc -I$(TFA)/inc 

# include library search path
LIBPATH = -L. -L$(HALLIB) -L$(TFALIB)

# compiler
CC = gcc

# include library
LIBS = -ltfa_hal -lm 

# compile flags
LDFLAGS = -g

# C++ compiler flags (-g -O2 -Wall)
CFLAGS := $(CFLAGS) -g -O0 -Wall

default: $(OUT)

$(OUT): $(OBJ)
	$(AR) rcs $(OUT) $(OBJ)
	ranlib $(OUT)

clean:
	rm -f $(OBJ) $(OUT) Makefile.bak 

