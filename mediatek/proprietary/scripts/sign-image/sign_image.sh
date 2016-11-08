#!/bin/bash

#######################################
# Initialize variables
#######################################
D_CURR=`pwd`

#######################################
# Specify temporarily folder path
#######################################

function usage() {

	#######################################
	# Dump usage howto
	#######################################

	echo "sign image ..."
	echo "Please source ./build/envsetup.sh and ./mbldenv.sh first, and select correct project"
	echo "Command: ./sign_image.sh <BASE_PROJECT>"
}


#######################################
# Check arguments
#######################################
if [ "$1" == "" ]; then
	source ./build/envsetup.sh
	export MTK_BASE_PROJECT=$(get_build_var MTK_BASE_PROJECT)
	export OUT_DIR=$(get_build_var OUT_DIR)
	export PRODUCT_OUT=$(get_build_var PRODUCT_OUT)
	export MTK_PLATFORM=$(get_build_var MTK_PLATFORM)
	MTK_PLATFORM=${MTK_PLATFORM,,}
else
	export MTK_BASE_PROJECT=$1
fi

v2_file="vendor/mediatek/proprietary/custom/$MTK_PLATFORM/security/cert_config/img_list.txt"

echo -e "MTK_BASE_PROJECT=$MTK_BASE_PROJECT"
echo -e "MTK_PLATFORM=$MTK_PLATFORM"
echo -e "$v2_file"

if [ -f "$v2_file" ]; then
	echo "v2 sign flow"
	python vendor/mediatek/proprietary/scripts/sign-image_v2/SignFlow.py $MTK_PLATFORM $MTK_BASE_PROJECT
else
	echo "v1 sign flow"
	make -f ./vendor/mediatek/proprietary/scripts/sign-image/Android.mk
fi