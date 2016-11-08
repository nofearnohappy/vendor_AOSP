#-------------------------------------------------------------------------------
# Name:        cert_gen
# Purpose:
#
# Author:      MTK02464
#
# Created:     08/06/2015
# Copyright:   (c) MTK02464 2015
# Licence:     <your licence>
#-------------------------------------------------------------------------------
import base64
import os
import sys
import subprocess

class asn1_obj:
    def __init__(self):
        self.name= ""
        self.type = ""
        self.encoded_type = ""
        self.size = 0
        self.encoded_size = ""
        self.data = ""
        self.encoded_data = ""

def asn1_is_container(data):
    if data == "{":
        return True
    else:
        return False

def asn1_is_external(type):
    external_obj_list = ["EXTERNAL_PEM"]
    if type in external_obj_list:
        return True
    else:
        return False

def asn1_int_encode(data):
    bits = []
    out_data_list = []
    int_data = int(data)
    while True:
        out_data = int_data & 0xff
        bits.append(out_data)
        int_data >>= 8
        if int_data == 0:
            break;
    if bits[-1] & 0x80:
        bits.append(0x0)
    for chunk in reversed(bits):
        out_data_list.append(chunk)
    return out_data_list

def asn1_bitstring_encode(data):
    out_data_list = []
    out_data_list.append(0x0)
    data_size = len(data)
    if data_size % 2:
        data_size = ((len(data) - 1) / 2 + 1) * 2
        data.zfill(data_size)
    for idx in range(len(data)):
        if idx % 2 == 0:
            out_data_list.append(int(data[idx] + data[idx + 1]))
    return out_data_list

def asn1_octetstring_encode(data):
    out_data_list = []
    data_size = len(data)
    if data_size % 2:
        data_size = ((len(data) - 1) / 2 + 1) * 2
        data.zfill(data_size)
    for idx in range(len(data)):
        if idx % 2 == 0:
            out_data_list.append(int(data[idx] + data[idx + 1]))
    return out_data_list

def asn1_oid_encode(data):
    oid_line_list = data.split('.')
    oid_line_inter_list = oid_line_list[2:]
    out_data_list = list()
    #first two nodes are treated as special case
    int_data = int(oid_line_list[0]) * 40 + int(oid_line_list[1])
    out_data_list.append(int_data)
    #process the following nodes
    for oid_node in oid_line_inter_list:
        int_data = int(oid_node)
        if oid_node < 128:
            out_data_list.append(int_data)
        else:
            #split into chunks with each 7 bits
            bits = []
            while int_data:
                bits.append(int_data & 0x7f)
                int_data >>= 7
            bits[1:] = [x | 0x80 for x in bits[1:]]
            for chunk in reversed(bits):
                out_data_list.append(chunk)
    return out_data_list

def asn1_utf8string_encode(data):
    out_data_list = []
    for idx in range(len(data)):
        out_data_list.append(data[idx])
    out_data_list = map(ord, out_data_list)
    return out_data_list

def asn1_printablestring_encode(data):
    out_data_list = []
    for idx in range(len(data)):
        out_data_list.append(data[idx])
    out_data_list = map(ord, out_data_list)
    return out_data_list

def asn1_boolean_encode(data):
    out_data_list = []
    if data == "True":
        out_data_list.append(0xff)
    else:
        out_data_list.append(0x00)
    return out_data_list

def asn1_utctime_encode(data):
    time_data_list = data.split('-')
    out_data_list = []
    out_data_list.append((ord(time_data_list[0][2])))
    out_data_list.append((ord(time_data_list[0][3])))
    for idx in range(1, len(time_data_list)):
        for char_idx in range(len(time_data_list[idx])):
            out_data_list.append((ord(time_data_list[idx][char_idx])))
    out_data_list.append(0x5a)
    return out_data_list

def asn1_external_pem_encode(data):
    #data is actually file path to external pem file
    in_pem_file = open(data, 'r')
    in_pem_file_content = ""
    out_data_list = []
    while True:
      line = in_pem_file.readline()
      if line == '':
          break
      if line[0] != '-':
          in_pem_file_content = in_pem_file_content + line

    out_der_file_content = base64.standard_b64decode(in_pem_file_content)
    out_data_list.append(out_der_file_content)
    in_pem_file.close()
    return out_data_list

def asn1_external_bitstring_encode(data):
    #data is file path to external binary file
    in_bin_file = open(data, 'rb')
    out_data_list = []
    out_data_list.append(0x0)
    byte = in_bin_file.read(1)
    while byte != "":
        out_data_list.append(int(ord(byte)))
        byte = in_bin_file.read(1)

    in_bin_file.close()
    return out_data_list

def asn1_data_encode(type, data):
    if type == "BOOLEAN":
        encoded_data = asn1_boolean_encode(data)
    if type == "INTEGER":
        encoded_data = asn1_int_encode(data)
    elif type == "BITSTRING":
        encoded_data = asn1_bitstring_encode(data)
    elif type == "OCTETSTRING":
        encoded_data = asn1_octetstring_encode(data)
    elif type == "OID":
        encoded_data = asn1_oid_encode(data)
    elif type == "UTF8STRING":
        encoded_data = asn1_utf8string_encode(data)
    elif type == "PRINTABLESTRING":
        encoded_data = asn1_printablestring_encode(data)
    elif type == "UTCTIME":
        encoded_data = asn1_utctime_encode(data)
    elif type == "EXTERNAL_PEM":
        encoded_data = asn1_external_pem_encode(data)
    elif type == "EXTERNAL_BITSTRING":
        encoded_data = asn1_external_bitstring_encode(data)
    else:
        encoded_data = list()

    return encoded_data

def asn1_type_encode(type):
    encoded_type = []
    type_entry = type.split(":")
    type_table = {"BOOLEAN": 0x01, "INTEGER": 0x02, "BITSTRING": 0x03, "EXTERNAL_BITSTRING": 0x03,"OCTETSTRING": 0x04,"NULL": 0x05, "OID": 0x06, "UTF8STRING": 0x0c, "SEQUENCE": 0x30, "SET": 0x31, "PRINTABLESTRING": 0x13, "UTCTIME": 0x17, "EXPLICIT": 0xa0}
    if type_entry[0] in type_table:
        if type_entry[0] == "EXPLICIT":
            index = int(type_entry[1])
            encoded_type.append(type_table[type_entry[0]] | index)
        else:
            encoded_type.append(type_table[type_entry[0]])
    return encoded_type

def asn1_size_encode(size):
    encoded_size = []
    if size < 128:
        encoded_size.append(size)
    else:
        bits = []
        num_of_size_chunk = 0
        while size:
            size_chunk = size & 0xff
            size >>= 8
            num_of_size_chunk += 1
            bits.append(size_chunk)
        bits.append(num_of_size_chunk | 0x80)
        for chunk in reversed(bits):
            encoded_size.append(chunk)
    return encoded_size

def asn1_create(cert_cfg, cert_obj):
    total_size = 0
    while True:
        line = cert_cfg.readline()
        if line == '':
            return total_size
        cur_asn1_obj = asn1_obj()
        component = line.split()
        if len(component) == 0:
            continue
        elif component[0] == '}':
            return total_size

        cur_asn1_obj.name = component[0]
        if cur_asn1_obj.name == "NULL":
            cur_asn1_obj.type = "NULL"
        else:
            cur_asn1_obj.type = component[1]
            cur_asn1_obj.data = component[3]

        if cur_asn1_obj.type == "EXTERNAL_CFG": # read in external cfg file and ignore current line
            external_cfg = open(cur_asn1_obj.data, 'r')
            total_size += asn1_create(external_cfg, cert_obj)
            external_cfg.close()
            continue

        cur_asn1_obj.encoded_type = asn1_type_encode(cur_asn1_obj.type)
        cert_obj.append(cur_asn1_obj)

        if not asn1_is_container(cur_asn1_obj.data):
            cur_asn1_obj.encoded_data = asn1_data_encode(cur_asn1_obj.type, cur_asn1_obj.data)
            if asn1_is_external(cur_asn1_obj.type):
                # a string of binary data
                cur_asn1_obj.size = len(cur_asn1_obj.encoded_data[0])
                total_size += cur_asn1_obj.size
            else:
                # a list of bytes data, one byte per element
                cur_asn1_obj.size = len(cur_asn1_obj.encoded_data)
                cur_asn1_obj.encoded_size = asn1_size_encode(cur_asn1_obj.size)
                total_size += 1 + len(cur_asn1_obj.encoded_size) + len(cur_asn1_obj.encoded_data)
        else:
            child_cert_obj = list()
            cert_obj.append(child_cert_obj)
            child_size = asn1_create(cert_cfg, child_cert_obj)
            cur_asn1_obj.size = child_size
            cur_asn1_obj.encoded_size = asn1_size_encode(child_size)
            total_size += 1 + len(asn1_size_encode(child_size)) + child_size

def dump_asn1(x509cert):
    for asn1_obj_entry in x509cert:
        if isinstance(asn1_obj_entry, list):
            dump_asn1(asn1_obj_entry)
        elif isinstance(asn1_obj_entry, asn1_obj):
            print "{} = {}".format("container", asn1_is_container(asn1_obj_entry.data))
            print "{} = {}".format("name", asn1_obj_entry.name)
            print "{} = {}".format("type", asn1_obj_entry.type)
            print "{} = {}".format("encoded_type", asn1_obj_entry.encoded_type)
            print "{} = {}".format("size", asn1_obj_entry.size)
            print "{} = {}".format("encoded_size", asn1_obj_entry.encoded_size)
            print "{} = {}".format("data", asn1_obj_entry.data)
            print "{} = {}".format("encoded_data", asn1_obj_entry.encoded_data)
            print ""
        else:
            print "Unknown asn1 object"

def write_asn1(cert_out, x509cert):
    for asn1_obj_entry in x509cert:
        if isinstance(asn1_obj_entry, list):
            write_asn1(cert_out, asn1_obj_entry)
        elif isinstance(asn1_obj_entry, asn1_obj):
            #write into cert_out
            if asn1_is_external(asn1_obj_entry.type):
                if len(asn1_obj_entry.encoded_data[0]) > 0:
                    cert_out.write(asn1_obj_entry.encoded_data[0])
            else:
                for i in range(len(asn1_obj_entry.encoded_type)):
                    cert_out.write(chr(asn1_obj_entry.encoded_type[i]))
                for i in range(len(asn1_obj_entry.encoded_size)):
                    cert_out.write(chr(asn1_obj_entry.encoded_size[i]))
                #data may come from external file
                if len(asn1_obj_entry.encoded_data) > 0:
                    for i in range(len(asn1_obj_entry.encoded_data)):
                        cert_out.write(chr(asn1_obj_entry.encoded_data[i]))
        else:
            print "Unknown asn1 object"

def asn1_gen(cfg_file, out_file, debug):
    cert_cfg = open(cfg_file, 'r')
    cert_out = open(out_file, 'wb')
    asn1_obj = list()
    asn1_create(cert_cfg, asn1_obj)

    if debug == True:
        print ""
        dump_asn1(asn1_obj)
    write_asn1(cert_out, asn1_obj)
    cert_out.close()
    cert_cfg.close()

def gen_pubk_from_prvk(prvk, pubk):
    cmd = 'openssl rsa'
    cmd += ' -in ' + prvk
    cmd += ' -pubout > ' + pubk 
    ret = subprocess.call([cmd], shell=True)
    return ret

def hash_gen(in_file, hash_file):
    cmd = 'openssl dgst -binary -sha256'
    cmd += ' ' + in_file
    cmd += ' > ' + hash_file
    ret = subprocess.call([cmd], shell=True)
    return ret

def sig_gen(in_hash, prvk, out_sig):
    cmd = 'openssl pkeyutl -sign'
    cmd += ' -in ' + in_hash
    cmd += ' -inkey ' + prvk 
    cmd += ' -out ' + out_sig
    cmd += ' -pkeyopt digest:sha256 -pkeyopt rsa_padding_mode:pss -pkeyopt rsa_pss_saltlen:32'
    ret = subprocess.call([cmd], shell=True)
    return ret

def pem_to_der(pem_file_path, der_file_path):
    in_file = open(pem_file_path, 'r')
    in_data = ""
    while True:
        line = in_file.readline()
        if line == '':
            break
        if line[0] != '-':
            in_data = in_data + line

    out_data = base64.standard_b64decode(in_data)

    out_file = open(der_file_path, 'wb')
    out_file.write(out_data)
    out_file.close()
    return

def pubk_der_to_gfh(pubk_der_file_path, gfh_ini_file_path, gfh_file_path):
    tool_path = os.path.abspath(os.path.dirname(__file__))
    cmd = tool_path + "/der_extractor " + pubk_der_file_path + " " + gfh_ini_file_path + " SV5_SBC"
    ret = subprocess.call([cmd], shell=True)
    cmd = tool_path + "/PBP -func keybin -o " + gfh_file_path + " -i " + gfh_ini_file_path 
    ret = subprocess.call([cmd], shell=True)
    return ret

def update_tbs_config_file(config_file_path, pubk_pem_path):
    config_file = open(config_file_path, 'r') 
    lines = config_file.readlines()
    config_file.close()

    out_config_file = open(config_file_path, 'w')
    for line in lines:
        component = line.split()
        if (component[0] == 'pubk') and (component[1] == 'EXTERNAL_PEM'):
            line_regen = "    pubk EXTERNAL_PEM ::= " + pubk_pem_path + '\n'
            out_config_file.write(line_regen)
        elif (component[0] == 'rootpubk') and (component[1] == 'EXTERNAL_BITSTRING'):
            #this is root cert, must generate gfh and oemkey.h
            #pubk is generated from prvk, and we assuem pubk is in out_folder
            intermediate_folder_path, pubk_pem_file = os.path.split(os.path.abspath(pubk_pem_path))

            core_name, pem_extension = os.path.splitext(pubk_pem_file)
            pubk_der_path = intermediate_folder_path + "/" + core_name + ".der"
            pem_to_der(pubk_pem_path, pubk_der_path)
            #generate gfh from pubk_der
            gfh_ini_file_path = intermediate_folder_path + "/" + "CHIP_KEY.ini"
            gfh_file_path = intermediate_folder_path + "/" + "gfh_sec_key.bin"
            pubk_der_to_gfh(pubk_der_path, gfh_ini_file_path, gfh_file_path)
            #generate final path of gfh
            line_regen = "    rootpubk EXTERNAL_BITSTRING ::= " + gfh_file_path + '\n'
            out_config_file.write(line_regen)
        else:
            out_config_file.write(line)

    out_config_file.close()
    return

def gen_tbs_config_file(config_file, tbs_config_file, pubk_file):
    cmd = 'cp ' + config_file + ' ' + tbs_config_file
    ret = subprocess.call([cmd], shell=True)
    update_tbs_config_file(tbs_config_file, pubk_file)

def update_x509_config_file(x509cert_cfg, tbs_cert_cfg, sig_path):
    config_file = open(x509cert_cfg, 'r') 
    lines = config_file.readlines()
    config_file.close()

    out_config_file = open(x509cert_cfg, 'w')
    for line in lines:
        component = line.split()
        if (component[0] == 'tbsCertificate') and (component[1] == 'EXTERNAL_CFG'):
            line_regen = "    tbsCertificate EXTERNAL_CFG ::= " + tbs_cert_cfg + '\n'
            out_config_file.write(line_regen)
        elif (component[0] == 'sigValue') and (component[1] == 'EXTERNAL_BITSTRING'):
            line_regen = "    sigValue EXTERNAL_BITSTRING ::= " + sig_path + '\n'
            out_config_file.write(line_regen)
        else:
            out_config_file.write(line)

    out_config_file.close()
    return

def gen_x509_config_file(x509cert_template_cfg, x509cert_config_file, tbs_config_file, sig_file):
    cmd = 'cp ' + x509cert_template_cfg + ' ' + x509cert_config_file
    ret = subprocess.call([cmd], shell=True)
    cmd = 'chmod 777 ' + x509cert_config_file
    ret = subprocess.call([cmd], shell=True)
    update_x509_config_file(x509cert_config_file, tbs_config_file, sig_file)

def create_cert_intermediate_folder(out_folder_path):
    intermediate_folder_path = os.path.abspath(out_folder_path + "/cert_intermediate")
    if not os.path.exists(intermediate_folder_path):
        os.makedirs(intermediate_folder_path)

def print_usage():
    print "[Usage] python [cert.cfg] [prvk.pem] [cert.out]"

def main():
    if len(sys.argv) != 4:
        print_usage()
        return

    config_file = sys.argv[1]
    prvk_file = sys.argv[2]
    x509cert_file = sys.argv[3]

    tool_folder = os.path.abspath(os.path.dirname(__file__))
    in_folder, in_file = os.path.split(os.path.abspath(config_file))
    key_folder, key_file = os.path.split(os.path.abspath(prvk_file))
    out_folder, out_file = os.path.split(os.path.abspath(x509cert_file))
    #core name will be used to create hash/sig and other temporary files
    core_name, cfg_extension = os.path.splitext(in_file)

    create_cert_intermediate_folder(out_folder)
    intermediate_folder_path = out_folder + "/cert_intermediate"

    hash_file = intermediate_folder_path + "/" + "tbs_" + core_name + ".hash"
    sig_file = intermediate_folder_path + "/" + "tbs_" + core_name + ".sig"
    tbs_cert_file = intermediate_folder_path + "/" + "tbs_" + core_name + ".der"
    pubk_file = intermediate_folder_path + "/" + core_name + "_pubk.pem"

    #generate public key
    print "Generating public key from private key..."
    gen_pubk_from_prvk(prvk_file, pubk_file)

    print "Generating tbsCertificate..."
    #update public key path in config file
    tbs_config_file = intermediate_folder_path + "/" + "tbs_" + core_name + ".cfg"
    #generate tbs config file
    gen_tbs_config_file(config_file, tbs_config_file, pubk_file)

    asn1_gen(tbs_config_file, tbs_cert_file, False)
    hash_gen(tbs_cert_file, hash_file)
    sig_gen(hash_file, prvk_file, sig_file)

    print "Generating x509cert..."
    #update signature file path in x509 config file
    x509cert_config_file = intermediate_folder_path + "/" + "x509_" + core_name + ".cfg"
    #generate x509 config file
    gen_x509_config_file(tool_folder + '\/x509cert_template.cfg', x509cert_config_file, tbs_config_file, sig_file)

    asn1_gen(x509cert_config_file, x509cert_file, False)
    print "Done"
    return

if __name__ == '__main__':
    main()
