#!/bin/bash

OUT_DIR=doc
SRC_DIR=src
PACKAGE=com.mediatek.mediatekdm.mdm

rm -rf $OUT_DIR

javadoc -sourcepath $SRC_DIR -d $OUT_DIR $PACKAGE
