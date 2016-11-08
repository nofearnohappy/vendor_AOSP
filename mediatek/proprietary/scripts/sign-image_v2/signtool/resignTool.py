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


IMG_HDR_MAGIC = 0x58881688
IMG_HEADER_SIZE = 0x200



# tool path


#string format
cert1_replace_taget 	= "(\s)*pubk2"
cert2_replace_hash 	= "(\s)*imgHash EXTERNAL_BITSTRING"
cert2_replace_headerhash 	= "(\s)*imgHdrHash EXTERNAL_BITSTRING"
swId_replace_target 	="(\s)*swID INTEGER"
imgVer_replace_target 	="(\s)*imgVer INTEGER"

cert1md_replace_taget = "(\s)*pubkHash EXTERNAL_BITSTRING"




global swId 
global imgVer
global hdr_size

#typedef union
#{
#    struct
#    {
#        unsigned int magic;     /* always IMG_MAGIC */
#        unsigned int dsize;     /* image size, image header is incldued, padding is not included */
#        char name[IMG_NAME_SIZE];
#        unsigned int maddr;     /* image load address in RAM */
#        unsigned int mode;      /* maddr is counted from the beginning or end of RAM */
#        /* extension */
#        unsigned int ext_magic;    /* always EXT_MAGIC */
#        unsigned int hdr_size;     /* header size is 512 bytes currently, but may extend in the future */
#        unsigned int hdr_version;  /* see HDR_VERSION */
#        unsigned int img_type;     /* please refer to #define beginning with IMG_TYPE_ */
#        unsigned int img_list_end; /* end of image list? 0: this image is followed by another image 1: end */
#        unsigned int align_size;   /* image size alignment setting in bytes, 16 by default for AES encryption */
#        unsigned int dsize_extend; /* high word of image size for 64 bit address support */
#        unsigned int maddr_extend; /* high word of image load address in RAM for 64 bit address support */
#    } info;
#    unsigned char data[IMG_HDR_SIZE];
#} IMG_HDR_T;

#mkimage_header = "<2I 32c 2I 8I"
mkimage_header = "<2I 32s 2I 8I"
def parseMultiBin(binFile,target_img_name):
	#parse bin
	imgSize = 0
	index = 0
	f = open(binFile)
	last_pos = 0
	pre_pos = 0
	
	binArray = []
	sizeArray = []
	offsetArray = []
	imgNameArray = []
	
	file_size = os.path.getsize(binFile)
	print "file_size: "+ str(hex(file_size))+"\n"
	
	firstImage = 1
	finalSize =0
	matchTarget =0
	img_name =0
	
	while True:
		#print "lastpos:"+str(hex(last_pos))
		f.seek(last_pos)
		header_size = struct.calcsize(mkimage_header)
		fin = f.read(header_size)
		unpack_array = struct.unpack(mkimage_header,fin)
		f.seek(last_pos)
			
		magic_number = unpack_array[0]
		dsize		=  unpack_array[1]
		hdr_size    = unpack_array[6]
		align_size  =  unpack_array[10]
		img_type    =  unpack_array[8]
		
		if( ~ cmp(magic_number,int(IMG_HDR_MAGIC)) == 0):	
			print "wrong image header magic"
			sys.exit()
			
		# include header + image+ padding size to 16 bytes align
		imgSize = (dsize+ hdr_size+(align_size-1))/align_size*align_size
		print "img-"+str(index)+" size:" + hex(imgSize)	
		
		img_type_byte0 = img_type & 0xFF
		img_type_byte3 = (img_type >> 24) & 0xFF
			
		if(img_type_byte3 != 2):
			# image not cert
			pre_img_name = img_name
			img_name = unpack_array[2].rstrip('\t\r\n\0')
			if(target_img_name == img_name):
				print "Target image, remove cert for replace"
				isTarget = 1
				matchTarget = 1
			else:
				print "Not target image, retain cert"
				isTarget = 0
			isRaw = 1
		else:
			#image is cert
			isRaw = 0
			
		if (isRaw and firstImage == 0):
			print "add index"+str(index) +" image"
			img_str = bin_tmp_path+"tmp_"+str(index)+".bin"
			binArray.append(img_str)
			sizeArray.append(finalSize)
			offsetArray.append(pre_pos)
			imgNameArray.append(pre_img_name)
			pre_pos = last_pos
			finalSize = 0
			index+=1
		
		firstImage = 0
	
		if(isTarget):
			if(isRaw):
				finalSize = imgSize
			else:
				print "is cert, discard it"
		else:
			finalSize += imgSize
			
		last_pos += imgSize
		
		if(last_pos >= file_size):
			print "add index"+ str(index) +" image, this is final image"
			img_str = bin_tmp_path+"tmp_"+str(index)+".bin"
			binArray.append(img_str)
			sizeArray.append(finalSize)
			offsetArray.append(pre_pos)
			imgNameArray.append(img_name)
			pre_pos = last_pos
			finalSize = 0
			break
		
	f.close()
	if(matchTarget == 0):
		print "img name not match,exit!!"
		sys.exit()
	return binArray,sizeArray,offsetArray,imgNameArray


def getSizeFromHeader(binFile):	
	global hdr_size

	f = open(binFile)
	header_size = struct.calcsize(mkimage_header)
	fin = f.read(header_size)
	unpack_array = struct.unpack(mkimage_header,fin)
	f.close()
	dsize		=  unpack_array[1]
	
	return dsize

	
def checkIsRaw(binFile):
	global hdr_size
	isRaw = 0
	isMD = 0

	f = open(binFile)
	header_size = struct.calcsize(mkimage_header)
	fin = f.read(header_size)

	unpack_array = struct.unpack(mkimage_header,fin)
	f.close()
	
	magic_number = unpack_array[0]
	dsize		=  unpack_array[1]
	
	img_type    =  unpack_array[8]
	ext_magic = hex(unpack_array[5])
	
	#print "a"+str(ext_magic)+" "+str(hdr_size)+" "+str(img_type)

	if( cmp(unpack_array[0],int(IMG_HDR_MAGIC)) == 0):
		img_type_byte0 = img_type & 0xFF
		img_type_byte3 = (img_type >> 24) & 0xFF
		#print "05336 " + str(img_type_byte3)+" "+str(img_type_byte0)
		hdr_size =  unpack_array[6]
		
		if(img_type == 0):
			print "Raw IMG"
			isRaw = 1
		elif(img_type_byte3 ==1):
			#print "Is MD IMG"
			if(img_type_byte0 == 0):
				print "MD IMG:LTE"
				isRaw = 1
				isMD = 1
			elif(img_type_byte0 == 1):
				print "MD IMG:C2K"
				isRaw = 1
				isMD =2
			
	else :
		print "Not Raw Img"
		isRaw = 0
	return isRaw, isMD
	

def createOutDir(path):
	dir = os.path.dirname(path)
	print "Create dir:"+dir
	
	if not os.path.exists(dir):
		os.makedirs(dir)

#define BOOT_MAGIC "ANDROID"
#define BOOT_MAGIC_SIZE 8
#define BOOT_NAME_SIZE 16
#define BOOT_ARGS_SIZE 512
#struct boot_img_hdr
#{
#    unsigned char magic[BOOT_MAGIC_SIZE];

#    unsigned kernel_size;  /* size in bytes */
#    unsigned kernel_addr;  /* physical load addr */
#    unsigned ramdisk_size; /* size in bytes */
#    unsigned ramdisk_addr; /* physical load addr */
#    unsigned second_size;  /* size in bytes */
#    unsigned second_addr;  /* physical load addr */
#    unsigned tags_addr;    /* physical addr for kernel tags */
#    unsigned page_size;    /* flash page size we assume */
#    unsigned unused[2];    /* future expansion: should be 0 */

#    unsigned char name[BOOT_NAME_SIZE]; /* asciiz product name */
#    unsigned char cmdline[BOOT_ARGS_SIZE];#
#    unsigned id[8]; /* timestamp / checksum / sha1 / etc */
#};	

BOOT_HDR_MAGIC = "ANDROID"
bootimg_header = "<8c 10I 16c 512c 8I"

def getDmVerityCert(binFile,imgSize):
	hasDmCert = 0
	file_size = os.path.getsize(binFile)
	if(file_size <= imgSize + 4):
		return hasDmCert
	
	
	f1 = open(binFile)
	fin = f1.read(imgSize)
	fin = f1.read(4)
	unpack_array = struct.unpack("<4c",fin)
	
	hasDmCert = 0

	if(ord(unpack_array[0]) == 0x30):
		certSize = (ord(unpack_array[2])<< 8) + ord(unpack_array[3]) + 4
		print hex(certSize)
		f2 = open(dm_cert,'w')
		f1.seek(imgSize)
		f2.write(f1.read(certSize))
		f2.close()
		hasDmCert = 1
	f1.close()
	
	paddingFile(dm_cert,16)
	
	return hasDmCert
		
		
ccci_header = "<8c 10I 16c 512c 8I"
def checkIsMD(binFile):
	#parse bin
	imgSize = 0
	isMD = 0
	f = open(binFile)
	header_size = struct.calcsize(ccci_header)
	#print len("<8c 10I 16c 512c")
	fin = f.read(header_size)
	unpack_array = struct.unpack(ccci_header,fin)
	f.close()
	magic_str=""
	for i in range(0,7):
		magic_str=magic_str + unpack_array[i]

	return 0
	
	
def checkIsBoot(binFile):
	global hdr_size
	#parse bin
	imgSize = 0
	isBoot = 0
	f = open(binFile)
	header_size = struct.calcsize(bootimg_header)

	fin = f.read(header_size)
	unpack_array = struct.unpack(bootimg_header, fin)
	f.close()
	magic_str=""
	for i in range(0,7):
		magic_str=magic_str + unpack_array[i]
	
	if (cmp(magic_str,BOOT_HDR_MAGIC) == 0):
		print "Is Boot Img"
		page_size = unpack_array[15]
		hdr_size = page_size
		kernel_size = ((unpack_array[8]/page_size)+1) * page_size
		ramdisk_size = ((unpack_array[10]/page_size)+1) * page_size
		print "Header size:"+ `hex(header_size)`
		print "Kernel size:"+ `hex(kernel_size)`
		print "Ramdisk size:"+ `hex(ramdisk_size)`
		print "page size:"+ `hex(unpack_array[15])`
		imgSize = (kernel_size + ramdisk_size + hdr_size + (16 - 1)) / 16 * 16
		print "Total Size(include header and pading):"+`hex(imgSize)`
		print hex(imgSize)
		isBoot = 1
	else :
		print "Not Boot Img"
	
	return isBoot,imgSize
	
def getSHA(binFile):
	#use Linux shell command to get sha value
	sh_command = 'sha256sum '+ binFile
	b = commands.getoutput(sh_command)
	sha_str = b.strip().split(' ')[0]
	print "Hash:"+sha_str
	
	return sha_str
def hash_gen(in_file, hash_file):
    cmd = 'openssl dgst -binary -sha256'
    cmd += ' ' + in_file
    cmd += ' > ' + hash_file
    ret = subprocess.call([cmd], shell=True)
    return ret

def fillCertConfig(certPath,replaceStr,targetLinePattern):
	sh_command = "chmod 777 "+certPath
	subprocess.call(sh_command, shell=True)
	
	f1 = open(certPath, 'r')
	lines = f1.readlines()
	f1.close()
	
	f2 = open(certPath, 'w')
	
	format1 = re.compile(targetLinePattern)
	for l in lines:
		if(format1.match(l)):
			print l
			l2 = l.split("::=")[0]+"::= "+replaceStr+"\n"
			print l2
			f2.write(l2)
		else:
			f2.write(l)


	
def genCert(cert_config,privk_key,cert_der):
		sh_command = 'python '+cert_tool+' '+ cert_config +' '+privk_key+' '+cert_der
		subprocess.call(sh_command, shell=True)

def paddingFile(inFile,align_num):
	filesize = os.stat(inFile).st_size
	f= open(inFile, 'a+')
	padding = filesize % align_num
	if(padding !=0):
		padding = align_num - padding
		for x in range(padding):
			f.write("\x00")
	f.close();
	
	
def appendFile(binFile,certFile):
	#padding bin file to 16 byte alignment
	paddingFile(binFile,16)
	#padding cert file to 16 byte alignment
	#paddingFile(certFile,16)
	#append cert to binFile
	f1 = open(binFile, 'a+')
	f2 = open(certFile,'r')
	f1.write(f2.read())
	f1.close()	
	f2.close()



	

def getPureImg(binFile,binArrary,sizeArray,offsetArray):
	index = 0
	f2 = open(binFile,'r')
	for bin in binArrary:
		imgSize = sizeArray[index]
		offset = offsetArray[index]
		f2.seek(offset)
		f1 = open(bin,'w+')
		f1.write(f2.read(imgSize))
		f1.close()
		index +=1

	f2.close()

def backUpFile(tmpFile,targetFile,backupFile):
	if(backupFile !=""):
		sh_command = 'mv '+targetFile+' '+ backupFile
		subprocess.call(sh_command, shell=True)
	
	sh_command = 'mv '+tmpFile+' '+targetFile
	subprocess.call(sh_command, shell=True)

def catBin(binArray,finalBin):

	f1 = open(finalBin,'w')
	index = 0;
	for bin in binArray:
		#print str(index)+"\n"		
		f2 = open(bin,'r')
		f1.write(f2.read())
		f2.close()
		index +=1
	f1.close()

def copyFile(path1,path2):
	sh_command = 'cp '+path1+' '+ path2
	subprocess.call(sh_command, shell=True)
	
def moveFile(path1,path2):
	sh_command = 'mv '+path1+' '+ path2
	subprocess.call(sh_command, shell=True)	

def addMkimageHeader(cert_name,imgListEnd,img_type,img_name):
	#change config
	#chmod for tool
	#sh_command = "chmod 777 "+mkimage_config
	#subprocess.call(sh_command, shell=True)

	f1 = open(mkimage_config,'r')
	f2 = open(mkimage_config_out,'w+')
	format1 = re.compile("IMG_LIST_END")
	format2 = re.compile("IMG_TYPE")
	format3 = re.compile("NAME")
	for l in f1:
		if(format1.match(l)):
			#print l
			end= l.split("=")[1]
			l2 =l.replace(end,str(imgListEnd))
			#print l2
			f2.write(l2+"\n")
		elif(format2.match(l)):
			#print l
			end= l.split("=")[1]
			l2 =l.replace(end,str(img_type))
			#print l2
			f2.write(l2+"\n")
		elif(format3.match(l)):
			#print l
			end= l.split("=")[1]
			l2 =l.replace(end,str(img_name))
			#print l2
			f2.write(l2+"\n")
		else:
			f2.write(l)
			
	f1.close();
	f2.close();
	
	#chmod for tool
	#sh_command = "chmod 777 "+mkimage_path
	#subprocess.call(sh_command, shell=True)
	#call mkimage
	sh_command = './'+mkimage_path+' '  +cert_name+'  '+mkimage_config_out+' > '+tmpcert_name
	subprocess.call(sh_command, shell=True)
	
	sh_command = 'mv '+tmpcert_name+' '+ cert_name
	subprocess.call(sh_command, shell=True)

def getMDKey(mdImage, isMD):
	#remove mkImage Header
	#tmpBin = mdImage+'_tmp'
	#f1 = open(mdImage,'r')
	#f2 = open(tmpBin,'w+')
	#fin = f1.read(IMG_HEADER_SIZE)
	#f2.write(f1.read())
	#f1.close()
	#f2.close()
	
	md_key_path = cert1md_hash_path +"md_key.bin"
	

	#sh_command = "chmod 777 "+mdGfhParser_path
	#subprocess.call(sh_command, shell=True)
	#get Gfh
	cmd =  './'+mdGfhParser_path+' '+str(isMD)+' '+ mdImage+' '+ md_key_path

	retcode = subprocess.call(cmd, shell=True) 
	if retcode != 0: sys.exit(retcode)
	
	return md_key_path


	
def bin_split(bin , split_path):
	
	split_header = split_path + "header.bin" 
	split_image =  split_path + "image.bin"
	
	f1 = open(bin,'r')
	f2 = open(split_header,'w+')
	f2.write(f1.read(hdr_size))
	f2.close()
	
	f2 = open(split_image,'w+')
	f2.write(f1.read())
	f2.close()
	f1.close()
	
	return split_header,split_image


	
def genCert2(argDict):

	certType = argDict['type']
	binFile = argDict['img']
	cert1 = argDict['cert1']
	cert_privk = argDict['privk']
	imgName = argDict['name']
	imgVer = argDict['ver']
	isRaw =0
	isBoot = 0

	binArray = []
	sizeArray = []
	offsetArray = []
	imgNameArray = []
	isRaw,isMD = checkIsRaw(binFile)
	if(not isRaw):
		isBoot,bootImgSize = checkIsBoot(binFile)

	if(isBoot):
		hasDmCert = getDmVerityCert(binFile,bootImgSize)
	
	if(isRaw):
		binArray,sizeArray,offsetArray, imgNameArray = parseMultiBin(binFile, imgName)
	elif(isBoot):
		binArray.append(bin_tmp_path+"tmp.bin")
		sizeArray.append(bootImgSize)
		offsetArray.append(0)
		imgNameArray.append(imgName)
	else:
		print "wrong image format"
		return -1
	
	getPureImg(binFile,binArray,sizeArray,offsetArray)
	print "bin_number:"+ str(len(binArray))
	#Get hash from image
	i = 0
	imgListEnd = 0
	for bin in binArray:
		if( i == len(binArray)-1):
			imgListEnd = 1
	
		if(isRaw == 1 or isBoot ==1  or isMD == 1 or isMD == 2):
			#Raw Image
			#if exist DM cert, append it first
			if(isBoot ==1 and hasDmCert==1 ):
				appendFile(bin,dm_cert)
			if(imgNameArray[i] == imgName):
				#parse mkimage heaader and image from bin
				split_header, split_image = bin_split(bin, cert2_hash_path)
				
				image_hash = cert2_hash_path + "image.hash"
				header_hash = cert2_hash_path + "header.hash"
				
				#gen header hash
				hash_gen(split_header, header_hash)
				#gen image+header hash
				#hash_gen(bin, image_hash)
				#gen image hash
				hash_gen(split_image, image_hash)
				
				#cat cert1
				appendFile(bin,cert1)
				
				#fill cert2 config
				copyFile(cert2_config,cert2_config_out)
				fillCertConfig(cert2_config_out, image_hash, cert2_replace_hash)
				fillCertConfig(cert2_config_out, header_hash, cert2_replace_headerhash)
				if(imgVer != 0):
					fillCertConfig(cert2_config_out, imgVer, imgVer_replace_target)
				#gen cert2
				genCert(cert2_config_out ,cert_privk ,cert2_name)
				#add mkimage header on cert2
				cert2_img_type = 0x2 << 24 | 0x2
				addMkimageHeader(cert2_name,imgListEnd,cert2_img_type,"cert2")
				#cat cert2
				appendFile(bin,cert2_name)
				#cat sig file
				sig_file = sig_path+imgName+".sig"
				print imgVer
				print "sig:"+sig_file
				sh_command = "chmod 777 "+cert1
				subprocess.call(sh_command, shell=True)
				copyFile(cert1,sig_file)
				appendFile(sig_file,cert2_name)
				
				
		
		i+=1
	
	binName = ntpath.split(binFile)[1]
	
	#print binName
	
	binName = binName.split(".")[0]+"-verified."+binName.split(".")[1]
	#print tttName
	finalBin = bin_tmp_path + binName
	
	#cat all bin to binArray[0]
	catBin(binArray,finalBin)
	
	#Post Process
	copyFile(finalBin,bin_path+binName)
	copyFile(bin_path+binName,out+binName)
	print "output path:"+out+binName

def endiness_convert(inFile,outFile):

	endian = sys.byteorder
	if endian == "little":
		fd = open(outFile, "wb")
		fs = open(inFile, "rb")
		from array import array
		for i in range(8, 0, -1):
			a = array("B", fs.read(4))		
			a.reverse()
			a.tofile(fd)

		fs.closed	
		fd.closed
	
def genCert1md(argDict):
	binFile = argDict['img']
	cert_privk = argDict['privk']
	
	#check is Raw
	isRaw, isMD = checkIsRaw(binFile)
	
	if(isMD < 1):
		print "Wrong MD image type"
		return
	
	#SV5 Image(MD)
	split_header, split_image = bin_split(binFile, cert1md_hash_path)
	
	key_hash_tmp = cert1md_hash_path + "key_tmp.hash"
	key_hash = cert1md_hash_path + "key.hash"
	header_hash = cert1md_hash_path + "header.hash"

	#get md key
	md_key = getMDKey(split_image, isMD)
	#gen header hash
	hash_gen(split_header, header_hash)
	#gen key hash
	hash_gen(md_key, key_hash_tmp)
	#Endiness conversion
	endiness_convert(key_hash_tmp, key_hash)
	
	#fill config
	copyFile(cert1md_config,cert1md_config_out)
	fillCertConfig(cert1md_config_out, key_hash, cert1md_replace_taget)
	fillCertConfig(cert1md_config_out, argDict['pubk'], cert1_replace_taget)
	
	swId   = argDict['swID']
	if(swId != 0):
		fillCertConfig(cert1_config_out, swId, swId_replace_target)
	#gen cert
	genCert(cert1md_config_out ,cert_privk ,cert1md_name)
	#add mkimage header on cert1md

	cert1md_img_type = 0x2 << 24 | 0x1 
	addMkimageHeader(cert1md_name,0,cert1md_img_type,"cert1md")
	print "output path:"+cert1md_name

	
def genCert1(argDict):
	#copy cert1 config
	copyFile(cert1_config,cert1_config_out)
	swId   = argDict['swID']
	if(swId != 0):
		fillCertConfig(cert1_config_out, swId, swId_replace_target)
	fillCertConfig(cert1_config_out, argDict['pubk'], cert1_replace_taget)
	#gen cert
	genCert(cert1_config_out ,argDict['privk'] ,cert1_name)
	cert1_img_type = 0x2 << 24 
	addMkimageHeader(cert1_name,0,cert1_img_type,"cert1")
	print "output path:"+cert1_name


def fillArgDict(str,key,argDict):
	prefix = str.split("=")[0]
	format = re.compile(key,re.I)
	if(format.search(prefix)):
		val = str.split("=")[1]
		argDict[key] = val
		print key+": "+val
		#print argDict[key]
	return argDict
	
def parseArg(argv):
	global argDict
	argDict = {'type': 0,'img':0 ,'privk': 0,'pubk': 0,'cert1': 0,'swID': 0,'ver':0, 'name':0}
	for str in argv:
		for key in argDict:
			argDict = fillArgDict(str,key,argDict)
	input_wrong =0
	#check input
	if(argDict['type'] == "cert1"):
		if(argDict['privk'] == "" or argDict['pubk'] == "" ):
			print "wrong cert1 input"
			input_wrong = 1
	elif(argDict['type'] == "cert2"):
		if(argDict['privk'] == "" or argDict['cert1'] == ""  or argDict['img'] == ""  or argDict['name'] == "" ):
			print "wrong cert2 input"
	elif(argDict['type'] == "cert1md"):
		if(argDict['img'] == "" or argDict['privk'] == ""):
			print "wrong cert1md input"
	else:
		print "wrong cert type!"
		input_wrong = 1
	if(input_wrong == 1):
		helpMenu()
		sys.exit()
	return argDict

def createFolder():

	createOutDir(mkimage_config_out)
	createOutDir(bin_path)

	if(argDict['type'] =="cert1md" ):
		createOutDir(cert1md_name)
		createOutDir(cert1md_hash_path)
	elif(argDict['type'] =="cert1"):
		createOutDir(cert1_name)
		createOutDir(cert1_config_out)
	elif(argDict['type'] =="cert2"):
		createOutDir(cert2_name)
		createOutDir(cert2_hash_path)
		createOutDir(sig_path)
		createOutDir(bin_tmp_path)

def setPath():
	global out_path
	global bin_path
	global cert1_name
	global cert1_config
	global cert1_config_out
	global cert1md_name
	global cert1md_config
	global cert1md_config_out
	global cert2_name
	global cert2_config
	global cert2_config_out
	global cert_config_tmp
	global tmpcert_name
	global cert_out_name
	global cert1md_hash_path
	global cert2_hash_path
	global dm_cert
	global bin_tmp_path
	global mkimage_config_out
	global sig_path
	global out
	global cert_gen_path
	global cert_tool
	global mkimage_tool_path
	global mkimage_path
	global mkimage_config
	global mdGfhParser_path

	img_name = argDict['name']

	if(os.environ.get('OUT') == None):
		out = "out/"

	else:
		out = os.environ.get('OUT')+"/"
	out_path = out+"resign/"
		
	bin_path = 	out_path + "bin/"
	cert_config_tmp = out_path + "cert/cert_tmp.cfg"
	tmpcert_name 	= out_path + "cert/tmp.der"
	cert_out_name 	= out_path + "cert/cert.der"
	
		
	pwd_split = sys.argv[0].split("signtool/resignTool.py")	

	if(pwd_split[0] =='resignTool.py'):
		pwd ="../"
	elif( len(pwd_split) > 1 and pwd_split[1] == ''):
		pwd = pwd_split[0]
	

	#tool path
	#cert_gen_path = "vendor/mediatek/proprietary/scripts/sign-image_v2/cert_chain/cert_gen/"
	cert_gen_path =  pwd + "cert_chain/cert_gen/"
	cert_tool = cert_gen_path + "cert_gen.py"
	#mkimage_tool_path = "vendor/mediatek/proprietary/scripts/sign-image_v2/mkimage20/"
	mkimage_tool_path = pwd + "mkimage20/"
	mkimage_path 	= 	mkimage_tool_path + "mkimage"
	mkimage_config 	= 	mkimage_tool_path + "img_hdr.cfg"
	#mdGfhParser_path = "vendor/mediatek/proprietary/scripts/sign-image_v2/mdtool/getPublicKey"
	mdGfhParser_path = pwd + "mdtool/getPublicKey"

	if(argDict['type'] =="cert1md" ):
		mkimage_config_out = out_path + "cert/cert1md/intermediate/img_hdr.cfg"
		cert1md_hash_path = out_path + "cert/cert1md/intermediate/hash/"
		cert1md_name 		= 	out_path + "cert/cert1md/cert1md.der"
		cert1md_config 		= 	cert_gen_path + "cert3.cfg"
		cert1md_config_out 	= 	out_path + "cert/cert1md/intermediate/cert3.cfg"
	elif(argDict['type'] =="cert1"):
		mkimage_config_out = out_path + "cert/cert1/intermediate/img_hdr.cfg"
		cert1_name 				= 	out_path + "cert/cert1/cert1.der"
		cert1_config 			= 	cert_gen_path + "cert1.cfg"
		cert1_config_out 		= 	out_path + "cert/cert1/intermediate/cert1.cfg"
	elif(argDict['type'] =="cert2"):
		mkimage_config_out = out_path  +"cert/cert2/"+img_name+"/intermediate/img_hdr.cfg"
		cert2_hash_path = 	out_path + "cert/cert2/"+img_name+"/intermediate/hash/"
		dm_cert 		= 	out_path + "cert/cert2/"+img_name+"/intermediate/dm_cert.der"
		bin_tmp_path 	= 	out_path  +"cert/cert2/"+img_name+"/intermediate/tmp_bin/"
		cert2_name 				= out_path + "cert/cert2/"+img_name+"/cert2.der"
		cert2_config 			= cert_gen_path + "cert2.cfg"
		cert2_config_out 		= out_path + "cert/cert2/"+img_name+"/intermediate/cert2.cfg"
		sig_path				= out+"sig/"
		

def helpMenu():
		print "Gen Cert1:"
		print "	usage: python resignTool.py type=cert1 privk=[cert1_privk.pem] pubk=[cert2_pubk.pem]"
		print "	optional: swID=[number]"
		print "	output: cert1"
		print "Gen Cert2 and append cert1,cert2 to the image:"
		print "	usage: python resignTool.py type=cert2 img=[xxx.bin] name=[img name] cert1=[cert1.der] privk=[cert2_privk.pem]"
		print "	optional:ver=number"
		print "	output: image append with cert1 and cert2"
		print "Gen Cert1md:"
		print "	usage: python resignTool.py type=cert1md img=[xxx.bin] privk=[cert1md_privk.pem] pubk=[cert2_pubk.pem]"
		print "	output: cert1md"
	
	
def main():
	argDict = parseArg(sys.argv)
	setPath()

	if(len(sys.argv) < 3):
		helpMenu()
	elif(argDict['type']=="cert1"): #python resignTool.py type=cert1 privk=key/privk1.pem  pubk=key/pubk2.pem
		
		if(len(sys.argv) < 4):
			print "cert1 gen wrong paramter!"
			return

		createFolder()

		genCert1(argDict)
	elif(argDict['type'] =="cert2"): #python resignTool.py type=cert2 img=lk.bin name=lk cert1=out/cert/cert1/cert1.der  privk=key/privk2.pem
		
		if(len(sys.argv) < 6):
			print "cert2 gen wrong parameter!"
			return
			
		createFolder()

		genCert2(argDict)
		
	elif(argDict['type'] =="cert1md"): #python resignTool.py type=cert1md img=sv5.bin  privk=key/privk2.pem pubk=key/pubk2.pem

		if(len(sys.argv) < 4):
			print "cert1md gen wrong parameter"
			return

		createFolder()
		
		genCert1md(argDict)
	else:
		print "wrong cert type !"


if __name__ == '__main__':
	main()
