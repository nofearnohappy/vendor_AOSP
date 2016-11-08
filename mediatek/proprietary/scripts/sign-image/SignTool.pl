#!usr/bin/perl
# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.

my $base_prj = $ARGV[0];
my $prj = $ARGV[1];
my $custom_dir = $ARGV[2];
my $secro_ac = $ARGV[3];
my $nand_page_size = $ARGV[4];
my $fb_signature = "FB_SIG";
my $dir = $ARGV[5];
my $out_dir = $ARGV[6];
my $cfg_dir = "vendor/mediatek/proprietary/custom/$base_prj/security/image_auth";
my $cfg_def = "IMG_AUTH_CFG.ini";
my $cfg = "$cfg_dir/$cfg_def";
my $key = "$cfg_dir/IMG_AUTH_KEY.ini";
my $oemkey = "$cfg_dir/VERIFIED_BOOT_IMG_AUTH_KEY.ini";
my $BUILD_UBOOT = "no";
$ENV{PROJECT}=$prj;
my $secro_type = "GMP";

##########################################################
# Dump Parameter
##########################################################
print "\n\n";
print "********************************************\n";
print " Dump Paramter \n";
print "********************************************\n";
print " Base Project     : $base_prj\n";
print " Project          : $prj\n";
print " Custom Directory : $custom_dir\n";
print " SECRO AC         : $secro_ac\n";
print " NAND Page Size   : $nand_page_size\n";
print " PRODUCT_OUT      : $dir\n";
print " OUT_DIR          : $out_dir\n";
print " BUILD_UBOOT      : $BUILD_UBOOT\n";

##########################################################
# Create Folder
##########################################################
print "\n\n";
print "********************************************\n";
print " Create Folder \n";
print "********************************************\n";
`mkdir $dir/signed_bin` if ( ! -d "$dir/signed_bin" );
`mkdir $dir/sig` if ( ! -d "$dir/sig" );
print "Image Dir '$dir'\n";
my $command = "vendor/mediatek/proprietary/scripts/sign-image/SignTool.sh";
#tool for signing image with SHA256 + RSA2048 for verified boot
my $command_2048 = "vendor/mediatek/proprietary/scripts/sign-image/SignTool_2048";

##########################################################
# File Check
##########################################################
my @imgs_need_sign_raw = ("lk.bin", "logo.bin", "secro.img", "boot.img", "recovery.img");
my @imgs_need_sign = ("system.img", "userdata.img", "efuse.img");
if(${BUILD_UBOOT} eq "yes") {
        push (@imgs_need_sign_raw, "uboot_${prj}.bin");
        push (@imgs_need_sign, "uboot_${prj}.bin");
}

# ProjectConfig.mk settings is not imported here, and try all the possible names
push (@imgs_need_sign, "trustzone.bin");
push (@imgs_need_sign, "mobicore.bin");
push (@imgs_need_sign, "tz.img");

# Does not check whether all images in the list exist. Sign all the images which can be found.

##########################################################
# BACKUP SECRO
##########################################################
my $secro_out = "$dir/secro.img";
my $secro_bak = "$dir/secro_bak.img";
system("cp -rf $secro_out $secro_bak") == 0 or die "backup SECRO fail\n";

##########################################################
# SECRO POST PROCESS
##########################################################
print "\n\n";
print "********************************************\n";
print " SecRo Post Processing \n";
print "********************************************\n";

my $cust_root_dir = "vendor/mediatek/proprietary/custom";
my $secro_def_cfg = "$cust_root_dir/common/secro/SECRO_DEFAULT_LOCK_CFG.ini";
if (${secro_type} eq "GMP") {
   $secro_def_cfg = "$cust_root_dir/common/secro/SECRO_GMP.ini";
}
my $secro_def_out = "$dir/secro.img";
my $secro_script = "vendor/mediatek/proprietary/scripts/secroimage/secro_post.pl";

if (${secro_ac} eq "yes")
{
	system("./$secro_script $secro_def_cfg $base_prj $custom_dir $secro_ac $secro_def_out $secro_type $prj $out_dir") == 0 or die "SECRO post process return error\n";
}

##########################################################
# Process Common Files
##########################################################
print "\n\n";
print "********************************************\n";
print " Sign Common Images \n";
print "********************************************\n";

# for these images, two layers of signature is applied, one for verified boot, one for secure download.
foreach my $img (@imgs_need_sign_raw) {
	if ( ! -e "$dir/$img") {
		warn "the $img is NOT exsit, please check\n";
		next;
	}
	my $signed_verified_img = $img;
	$signed_verified_img =~ s/\./-verified\./;
	my $sig_img = $img;
	$sig_img =~ s/\.bin/\.sig/;
	$sig_img =~ s/\.img/\.sig/;
	print "Sign Image '$dir/$img' with key '$oemkey'... output = '$dir/$signed_verified_img'\n";
	system("$command_2048 $oemkey $dir/$img $dir/$signed_verified_img $dir/sig/$sig_img") == 0 or die "sign image(verified boot) fail";

	my $signed_img = $img;
	$signed_img =~ s/\./-sign\./;

	my $signed_cfg = "$cfg_dir/$img.ini";
	if ( ! -e "$signed_cfg" ) {
		$signed_cfg = $cfg;
	}
	print "Sign Image '$dir/$img' with cfg '$signed_cfg'...\n";
	
	system("$command $key $signed_cfg $dir/$signed_verified_img $dir/signed_bin/$signed_img $nand_page_size $fb_signature") == 0 or die "sign image fail";
}

# these images are signed for secure download only and not covered by verified boot.
foreach my $img (@imgs_need_sign) {
	if ( ! -e "$dir/$img") {
		warn "the $img is NOT exsit, please check\n";
		next;
	}
	my $signed_img = $img;
	$signed_img =~ s/\./-sign\./;
	my $signed_cfg = "$cfg_dir/$img.ini";
	if ( ! -e "$signed_cfg" ) {
		$signed_cfg = $cfg;
	}
	print "Sign Image '$dir/$img' with cfg '$signed_cfg'...\n";
	
	system("$command $key $signed_cfg $dir/$img $dir/signed_bin/$signed_img $nand_page_size $fb_signature") == 0 or die "sign image fail";
}

sub print_system {
	my $command = $_[0];
	my $rslt = system($command);
	print "$command: $rslt\n";
	die "Failed to execute $command" if ($rslt != 0);
}

##########################################################
# Process EMMC Files
##########################################################
print "\n\n";
print "********************************************\n";
print " Sign EMMC Images \n";
print "********************************************\n";

my @imgs_need_sign = ("MBR", "EBR1", "EBR2");

foreach my $img (@imgs_need_sign) {
	if (-e "$dir/$img") {		
		my $signed_cfg = "$cfg_dir/$img.ini";
        	if ( ! -e "$signed_cfg" ) {
                	$signed_cfg = $cfg;
        	}
        	print "Sign Image '$dir/$img' with cfg '$signed_cfg'...\n";
		system("$command $key $signed_cfg $dir/$img $dir/signed_bin/${img}-sign $nand_page_size $fb_signature") == 0 or die "sign EMMC image fail";
	}
}

my @imgs_need_sign = ("cache.img", "custom.img");

foreach my $img (@imgs_need_sign) {
	if (-e "$dir/$img") {		
		my $signed_img = $img;
		$signed_img =~ s/\./-sign\./;
        	my $signed_cfg = "$cfg_dir/$img.ini";
        	if ( ! -e "$signed_cfg" ) {
                	$signed_cfg = $cfg;
        	}
        	print "Sign Image '$dir/$img' with cfg '$signed_cfg'...\n";
		system("$command $key $signed_cfg $dir/$img $dir/signed_bin/$signed_img $nand_page_size $fb_signature") == 0 or die "sign EMMC image fail";
	}
}

##########################################################
# RESTORE SECRO
##########################################################
my $secro_out = "$dir/secro.img";
my $secro_bak = "$dir/secro_bak.img";
system("cp -rf $secro_bak $secro_out") == 0 or die "restore SECRO fail\n";
system("rm -rf $secro_bak") == 0 or die "remove backup SECRO fail\n";

print "remove unused *sig files \n";
system("rm -f $dir/*.sig ");
