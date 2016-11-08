#!/usr/bin/perl

use lib "vendor/mediatek/proprietary/scripts/secroimage";
use lib "device/mediatek/build/build/tools";
use pack_dep_gen;
PrintDependModule($0);

##########################################################
# Initialize Variables
##########################################################
my $secro_cfg = $ARGV[0];
my $prj = $ARGV[1];
my $custom_dir = $ARGV[2];
my $secro_ac = $ARGV[3];
my $secro_out = $ARGV[4];
my $secro_type = $ARGV[5];
my $full_prj = $ARGV[6];
my $out_dir = $ARGV[7];
my $host_os = $ARGV[8];

my $SECRO_TOOL_F = "vendor/mediatek/proprietary/scripts/secroimage";
my $SECRO_CONFIG_F = "vendor/mediatek/proprietary/custom/common/secro";
my $secro_tool = "$SECRO_TOOL_F/SECRO_POST";
if (${secro_type} ne "")
{
    $secro_tool = "$SECRO_TOOL_F/SECRO_POST_$secro_type";
}
if (${host_os} eq "darwin")
{
    $secro_tool = "$secro_tool.darwin";
}
my $SEC_CONFIG_FILE_F = "vendor/mediatek/proprietary/custom/$prj/security";
my $MODEM_PRJ_F = "vendor/mediatek/proprietary/custom/$prj/modem";
my $SECRO_PRJ_F = "vendor/mediatek/proprietary/custom/$prj/secro";

if (${custom_dir} ne ${prj}){
	print "${custom_dir}  does noe equal to ${prj}, use ${custom_dir}\n";
	$SEC_CONFIG_FILE_F = "$custom_dir/security";
	$MODEM_PRJ_F = "$custom_dir/modem";
	$SECRO_PRJ_F = "$custom_dir/secro";
}
my $sml_dir = "$SEC_CONFIG_FILE_F/sml_auth";
my $secro_ini = "$out_dir/target/product/$full_prj/SECRO_WP.ini";

print "******************************************************\n";
print "*********************** SETTINGS *********************\n";
print "******************************************************\n";
print " ARGV[0] (SECRO_CFG) = $secro_cfg\n";
print " ARGV[1] (PROJECT) = $prj\n";
print " ARGV[2] (CUSTOM_PRJ) = $custom_dir\n";
print " ARGV[3] (SECRO_AC) = $secro_ac\n";
print " ARGV[4] (SECRO_OUT) = $secro_out\n";
print " ARGV[5] (SECRO_TYPE) = $secro_type\n";
print " ARGV[6] (FULL PROJECT NAME) = $full_prj\n";
print " ARGV[7] (OUT DIR) = $out_dir\n";

if (index(${prj}, 'x86') != -1)
{
	print " no need to generate secro for x86 projects\n";
	exit 0;
}

##########################################################
# SecRo Post Processing
##########################################################
my $ac_region = "$SECRO_PRJ_F/AC_REGION";
my $and_secro = "$SECRO_PRJ_F/AND_SECURE_RO";
my $md_secro = "$MODEM_PRJ_F/$CUSTOM_MODEM/SECURE_RO";
my $md2_secro = "$MODEM_PRJ_F/$CUSTOM_MODEM/SECURE_RO_sys2";

if (${secro_ac} eq "yes")
{
	if ( ! -e $md_secro )
	{
		print "this modem does not has modem specific SECRO image, use common SECRO\n";
		$md_secro = "$SECRO_CONFIG_F/SECURE_RO";
	}

        if ( ! -e $md2_secro )
        {
                print "this modem2 does not has modem2 specific SECRO image, use common SECRO\n";
                $md2_secro = "$SECRO_CONFIG_F/SECURE_RO_sys2";
        }
}
else
{
	$md_secro = "$SECRO_CONFIG_F/SECURE_RO";
	$md2_secro = "$SECRO_CONFIG_F/SECURE_RO_sys2";
}

##########################################################
# Check the Existence of each Region
##########################################################
if ( ! -e $ac_region )
{
	$ac_region = "$SECRO_CONFIG_F/AC_REGION";
	print "does not has aggregate specific AC_REGION image, use common AC_REGION\n";
}
print " ac_region = $ac_region\n";

if ( ! -e $and_secro )
{
        $and_secro = "$SECRO_CONFIG_F/AND_SECURE_RO";
	print "does not has AP specific AC_REGION_RO image, use common AC_REGION_RO\n";
}
print " and_secro = $and_secro\n";

if ( ! -e $md_secro )
{
        $md_secro = "$SECRO_CONFIG_F/SECURE_RO";
	print "does not has MODEM specific SECURE_RO image, use common SECURE_RO\n";
}
print " md_secro = $md_secro\n";

if ( ! -e $md2_secro )
{
    $md2_secro = "$SECRO_CONFIG_F/SECURE_RO_sys2";
	print "does not has MODEM specific SECURE_RO image, use common SECURE_RO_sys2\n";
}
print " md2_secro = $md2_secro\n";


open(SECRO_FH, ">$secro_ini") or die "open file error $secro_ini\n";

print SECRO_FH "SECRO_CFG = $secro_cfg\n";
print SECRO_FH "AND_SECRO = $and_secro\n";
print SECRO_FH "AC_REGION = $ac_region\n";
PrintDependency($secro_cfg);
PrintDependency($and_secro);
PrintDependency($ac_region);

my $count = 0;
PrintDependency("mediatek/custom/out/$prj/modem/$file");
print SECRO_FH "SECRO[$count] = $md_secro\n";
$count++;
print SECRO_FH "SECRO[$count] = $md_secro\n";
$count++;

if($count>=10)
{
	die "Maximum support of SECRO for world phone is 10, but current is $count\n";
}

while($count<=9)
{
	print SECRO_FH "SECRO[$count] = $SECRO_CONFIG_F/SECURE_RO\n";
	$count++;
}

close(SECRO_FH);

#system("chmod 777 $ac_region") == 0 or die "can't configure $ac_region as writable";
print "MTK_SEC_SECRO_AC_SUPPORT = $secro_ac\n";
if (${secro_ac} eq "yes")
{		
	PrintDependency($secro_ini);
	PrintDependency("$sml_dir/SML_ENCODE_KEY.ini");
	PrintDependency($secro_tool);
	system("./$secro_tool $secro_ini $sml_dir/SML_ENCODE_KEY.ini $secro_out") == 0 or die "SECRO POST Tool return error\n";
}

unlink($secro_ini);
