#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import sys
import os
import struct
import re
import commands
import subprocess
import fileinput
import ntpath


def executeShell(sh_command):
	subprocess.call(sh_command, shell=True)

def createOutDir(path):
	dir = os.path.dirname(path)
	print "Create dir:"+dir
	
	if not os.path.exists(dir):
		os.makedirs(dir)
	
def setPath():
	global out_path
	global resign_tool_path
	global cert1_dir
	global cert2_key_dir
	global img_list_path
	global img_ver_path
	global bin_tmp_path
	out_path = os.environ.get('OUT')+"/"
	bin_tmp_path = out_path+"resign/bin/multi_tmp/"
	resign_tool_path = "vendor/mediatek/proprietary/scripts/sign-image_v2/signtool/resignTool.py"
	cert1_dir = "vendor/mediatek/proprietary/custom/"+platform+"/security/cert_config/cert1/"
	cert2_key_dir = "vendor/mediatek/proprietary/custom/"+platform+"/security/cert_config/cert2_key/"
	img_list_path = "vendor/mediatek/proprietary/custom/"+platform+"/security/cert_config/img_list.txt"
	img_ver_path  = "vendor/mediatek/proprietary/custom/"+platform+"/security/cert_config/img_ver.txt"
	
	createOutDir(bin_tmp_path)
	
def parseArg(argv):
	global platform
	global project
	platform = argv[1]
	project = argv[2]

def copyFile(path1,path2):
	sh_command = 'cp '+path1+' '+ path2
	subprocess.call(sh_command, shell=True)	
	
def parseImgList():
	isSingleBin = 1
	pattern1= "\[single_bin\]"
	format1 = re.compile(pattern1)
	pattern2= "\[multi_bin\]"
	format2 = re.compile(pattern2)
	f = open(img_list_path,'r')
	singleBinDict ={}
	multiBinDict ={}
	for line in f:
		if( not line.strip()):
			continue
	
		if(format1.match(line)):
			isSingleBin =1
		elif(format2.match(line)):
			isSingleBin = 0
		else:
			if(isSingleBin is 1):
				#print line
				bin_name = line.split("=")[0].strip()
				img_name = line.split("=")[1].strip()
				singleBinDict[bin_name]=img_name
			elif(isSingleBin is 0):
				bin_name = line.split("=")[0].strip()
				img_name = line.split("=")[1].strip()
				multiBinDict[bin_name]=img_name
				
	#print singleBinDict
	#print singleBinDict.keys()
	#print multiBinDict
	f.close()
	return singleBinDict, multiBinDict
	
def getImgVer(img_name):
	img_ver = 0
	targetLine = 0
	
	f = open(img_ver_path,'r')

	pattern1= "\["+img_name+"\]"
	format1 = re.compile(pattern1)
	pattern2= "img_ver*"
	format2 = re.compile(pattern2)
	for line in f:
		if( not line.strip()):
			continue
		#print line
		if(format1.match(line)):
			#print img_name
			targetLine = 1
		elif(format2.match(line)):
			if(targetLine == 1):
				img_ver = line.split("=")[1].strip()
			targetLine = 0
			#print line
			
	f.close()
	#print img_ver
	return img_ver
	
def genCert2(img_path,img_name,binDict):
	cert_name = img_name +"_cert1.der"
	key_name = img_name+"_privk2.pem"
	#img_path = out_path+bin_name
	cert_apth = cert1_dir + cert_name
	key_path = cert2_key_dir + key_name
	#get version
	ver = getImgVer(img_name)

	#check bin exist in out folder
	if(os.path.isfile(img_path)):
		sh_command = "python "+ resign_tool_path+ " type=cert2 img="+img_path+" name="+img_name+" cert1="+cert_apth+" privk="+key_path +" ver="+str(ver)
		#print bin_name
		#print img_name
		print sh_command
		executeShell(sh_command)
		print "--------"
	
def main():

	if(len(sys.argv) < 3):
		print "SignFlow.py <platform> <project>"
		print "e.x SignFlow.py mt6755 evb6755_64"
		sys.exit()
	parseArg(sys.argv)
	
	setPath()
	
	singleBinDict, multiBinDict = parseImgList()
	for bin in singleBinDict.keys():
		img_name = singleBinDict[bin]
		img_path = out_path+bin
		genCert2(img_path,img_name,singleBinDict)
	

	for bin in multiBinDict.keys():
		img_name_list = multiBinDict[bin].split(",")

		multi_tmp_bin_in = bin_tmp_path+bin
		multi_tmp_bin_out =out_path+"resign/bin/"+ bin.split(".")[0]+"-verified."+bin.split(".")[1]
		copyFile(out_path+bin,multi_tmp_bin_in)
		
		for img_name in img_name_list:
			img_name = img_name.strip()
			genCert2(multi_tmp_bin_in,img_name,multiBinDict)
			#print multi_tmp_bin_out
			#print multi_tmp_bin_in
			copyFile(multi_tmp_bin_out,multi_tmp_bin_in)
			

	
	#img_name ="lk"
	#bin_name ="lk.bin"
	#cert_name = img_name +"_cert1.der"
	#key_name = img_name+"_privk2.pem"
	#sh_command = "python "+ resign_tool_path+ " type=cert2 img="+out_path+bin_name+" name="+img_name+" cert1="+cert1_dir+cert_name+" privk="+cert2_key_dir+key_name
	#print sh_command
	sys.exit()
	

if __name__ == '__main__':
	main()