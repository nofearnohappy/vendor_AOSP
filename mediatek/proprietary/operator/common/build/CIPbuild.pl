use Getopt::Long;

my $addtionalOption = "";
my $project = "";
my $configFile = "vendor/mediatek/proprietary/operator/common/build/CIPconfig.ini";
my $operator = "ALL";

&GetOptions(
  "ini=s" => \$configFile,
  "o=s" => \$addtionalOption,
  "p=s" => \$project,
  "op=s" => \$operator,
);

#if($configFile =~ /^\s*$/){
#  print "configFile is empty $configFile\n";
#  &usage();
#}
#elsif(!-e $configFile){
#  print "Can not find configFile=$configFile\n";
#  &usage();
#}

if(!-e $configFile){
  $configFile = "vendor/mediatek/proprietary/operator/common/build/CIPconfig.ini";
}

if(!-e $configFile){
  print "Can not find configFile=$configFile\n";
  &usage();
}

if($project =~ /^\s*$/){
  my $file = (split /\//, $configFile)[-1];
  $project = (split /\./, $file)[0];
  #print "default project: $project\n";
}

print "configFile: $configFile\n";
print "addtionalOption: $addtionalOption\n";
print "project: $project\n";
print "operator: $operator\n";


my @AllCIPSupportOP;
my %feature2value;
my $firstMeetCIPSupport = 1;
open CONFIGFILE,"<$configFile" or die "Can not open configFile=$configFile\n";
while(<CONFIGFILE>){
  my $line = $_;
  chomp $line;
  next if($line =~ /^#/);
  next if($line =~ /^\s*$/);
  if($line =~ /^\s*CIP_SUPPORT\s*:=\s*(.+?)\s*$/){
    my $CIPSupportOP = $1;
    chomp $CIPSupportOP;
    die "Error: Find \"CIP_SUPPORT:=\" again\n" if(! $firstMeetCIPSupport);
    print "CIP_SUPPORT = $CIPSupportOP\n";
    @AllCIPSupportOP = split /\s+/,$CIPSupportOP;
  }
  elsif($line =~ /^\s*(\S+?)\s*:=\s*(.+?)\s*$/){
    $feature2value{$1}=$2;
  }
  else{
    print "Warning: Unknow input line:$line\n";
  }
}
close CONFIGFILE;

my $cmd = "mv out/target/product/$project/custom.img out/target/product/$project/custom_bak.img";
my $buildNumber = 0;
system("$cmd") && &errorFlow("Backup custom.img fail");
for my $CIPSupportOP (@AllCIPSupportOP){
  if ($CIPSupportOP eq $operator || $operator eq "ALL"){
    my $featureValue = $feature2value{$CIPSupportOP};
    print $CIPSupportOP." = ".$featureValue."\n";
    my $command = "make clean-customimage && make $addtionalOption $featureValue customimage";
    print "$command\n";
    system("$command") && &errorFlow("Build customimage fail");
    &backup($CIPSupportOP);
    $buildNumber ++;
  }
}

$cmd = "mv out/target/product/$project/custom_bak.img out/target/product/$project/custom.img";
system("$cmd") && &errorFlow("Restore custom.img fail");

if ($buildNumber == 0) {
	&errorFlow("$operator custom.img build fail")
}
exit 0;

sub backup{
  my $backupName = $_[0];
  my $cmd = "mv out/target/product/$project/custom.img out/target/product/$project/custom${backupName}.img";
  print $cmd."\n";
  system("$cmd") && &errorFlow("Backup custom.img fail");
}

sub errorFlow{
  $errorMessage = $_[0];
  print "$errorMessage\n";
  exit 2;
}

sub usage{
  warn << "__END_OF_USAGE";
Usage: perl vendor/mediatek/proprietary/operator/common/build/CIPbuild.pl [options]

Options:
  -ini      : CIP config file.
  -p        : Project to build.
  -o        : Pass extra arguments for each build process.

Example:
  perl vendor/mediatek/proprietary/operator/common/build/CIPbuild.pl -ini=vendor/mediatek/proprietary/operator/common/build/CIPconfig.ini -p=mt6582_phone

__END_OF_USAGE

  exit 1;

}
