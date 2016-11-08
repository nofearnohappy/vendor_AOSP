/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

public class Ds2ParamDefs {

    //MAX BANDS & CHANS
    public static final int DAP_MAX_NUM_BANDS = 20;
    public static final int DAP_MAX_NUM_CHANS = 8;
    private static final int BYTE_PER_INT = 4;

    public static final int DAP2_PARAM_INVALID = -1;
    public static final int DAP2_PARAM_VOL = 0x006c6f76;
    public static final int DAP2_PARAM_PSTG = 0x67747370;
    public static final int DAP2_PARAM_PREG = 0x67657270;
    public static final int DAP2_PARAM_PLB = 0x00626c70;
    public static final int DAP2_PARAM_NGON = 0x6e6f676e;
    public static final int DAP2_PARAM_DEON = 0x6e6f6564;
    public static final int DAP2_PARAM_DEA = 0x00616564;
    public static final int DAP2_PARAM_DED = 0x00646564;
    public static final int DAP2_PARAM_VMB = 0x00626d76;
    public static final int DAP2_PARAM_DVME = 0x656d7664;
    public static final int DAP2_PARAM_DVMC = 0x636d7664;
    public static final int DAP2_PARAM_DVLE = 0x656c7664;
    public static final int DAP2_PARAM_DVLA = 0x616c7664;
    public static final int DAP2_PARAM_DVLI = 0x696c7664;
    public static final int DAP2_PARAM_DVLO = 0x6f6c7664;
    public static final int DAP2_PARAM_IEON = 0x6e6f6569;
    public static final int DAP2_PARAM_IEA = 0x00616569;
    public static final int DAP2_PARAM_IEBS = 0x73626569;    // dap_geq_ieq_bands_params_t
    public static final int DAP2_PARAM_ARON = 0x6e6f7261;
    public static final int DAP2_PARAM_ARDE = 0x65647261;
    public static final int DAP2_PARAM_ARRA = 0x61727261;
    public static final int DAP2_PARAM_AROD = 0x646f7261;
    public static final int DAP2_PARAM_ARTP = 0x70747261;
    public static final int DAP2_PARAM_ARBS = 0x73627261;    // Dap2_Regulator_Tuning_Params_t
    public static final int DAP2_PARAM_ARTI = 0x69747261;   // Dap_CpDp_Regulator_Tuning_Info_Values
    public static final int DAP2_PARAM_VTON = 0x6e6f7476;
    public static final int DAP2_PARAM_DOM = 0x006d6f64;
    public static final int DAP2_PARAM_DSB = 0x00627364;
    public static final int DAP2_PARAM_DSSF = 0x66737364;
    public static final int DAP2_PARAM_DSSA = 0x61737364;
    public static final int DAP2_PARAM_GEON = 0x6e6f6567;
    public static final int DAP2_PARAM_GEBS = 0x73626567;    // Dap2_Eq_Bands_Param
    public static final int DAP2_PARAM_BEON = 0x6e6f6562;
    public static final int DAP2_PARAM_BEB = 0x00626562;
    public static final int DAP2_PARAM_BECF = 0x66636562;
    public static final int DAP2_PARAM_BEW = 0x00776562;
    public static final int DAP2_PARAM_VBM = 0x006d6276;
    public static final int DAP2_PARAM_DHRG = 0x67726864;
    public static final int DAP2_PARAM_VBSF = 0x66736276;
    public static final int DAP2_PARAM_VBOG = 0x676f6276;
    public static final int DAP2_PARAM_VBSG = 0x67736276;
    public static final int DAP2_PARAM_VBHG = 0x67686276;
    public static final int DAP2_PARAM_VBMF = 0x666d6276;
    public static final int DAP2_PARAM_AOON = 0x6e6f6f61;
    public static final int DAP2_PARAM_AOBS = 0x73626f61;    // Dap2_Audio_Optimizer_Bands_Params
    public static final int DAP2_PARAM_MSCE = 0x6563736d;
    public static final int DAP2_PARAM_MIEE = 0x6565696d;
    public static final int DAP2_PARAM_MDLE = 0x656c646d;
    public static final int DAP2_PARAM_MDEE = 0x6565646d;
    public static final int DAP2_PARAM_VNBS = 0x73626e76;    // Dap2_Vis_Bands_Params
    public static final int DAP2_PARAM_VCBS = 0x73626376;    // Dap2_Vis_Bands_Params
    public static final int DAP2_PARAM_DMC = 0x00636d64;    // dap_cpdp_mix_data
    public static final int DAP2_PARAM_VER = 0x00726576;

    public static byte[] intToByteArray(int value)
    {
        ByteBuffer converter = ByteBuffer.allocate(BYTE_PER_INT);
        converter.order(ByteOrder.nativeOrder());
            converter.putInt(value);
        return converter.array();
    }

    public static byte[] IntArrayToByteArray (int [] src)
    {
        int srcLength = src.length;
        ByteBuffer converter = ByteBuffer.allocate(srcLength * BYTE_PER_INT);
        converter.order(ByteOrder.nativeOrder());
        IntBuffer inputBuffer = converter.asIntBuffer();
        inputBuffer.put(src);
        return converter.array();
    }

    //Class for multiple parameter storing
    public static class Ds2MultiParam
    {
        public int paramId_ = DAP2_PARAM_INVALID;

        public byte[] setMultiParam(int deviceId, int paramId, int bandNum, int[]... paramArray)
        {
            paramId_ = paramId;
            //Device Id + Param Id + Value size
            int size = BYTE_PER_INT * 3;
            int valuecount = 1; // bandNum size
            for (int[] array : paramArray)
            {
                valuecount += array.length;
            }
            // + total array length
            size = size + valuecount * BYTE_PER_INT;

            int offset = 0;
            byte[] outBuffer = new byte[size];

            // copy and combine the buffer
            byte[] byteDevId = intToByteArray(deviceId);
            byte[] byteId = intToByteArray(paramId);
            byte[] byteLen = intToByteArray(valuecount);
            byte[] byteBand = intToByteArray(bandNum);
            System.arraycopy(byteDevId, 0, outBuffer, offset, byteDevId.length);
            offset += byteDevId.length;
            System.arraycopy(byteId, 0, outBuffer, offset, byteId.length);
            offset += byteId.length;
            System.arraycopy(byteLen, 0, outBuffer, offset, byteLen.length);
            offset += byteLen.length;
            System.arraycopy(byteBand, 0, outBuffer, offset, byteBand.length);
            offset += byteBand.length;

            for (int[] array : paramArray)
            {
                byte[] byteValue = IntArrayToByteArray(array);
                System.arraycopy(byteValue, 0, outBuffer, offset, byteValue.length);
                offset += byteValue.length;
            }

            return outBuffer;
        }
    }
}

