/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds;

import android.media.AudioSystem;
import android.media.audiofx.AudioEffect;
import android.util.Log;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.UUID;

import com.dolby.api.DsLog;
import com.dolby.api.DsConstants;

public class DsEffect
{
    private static final String LOG_TAG = "DsEffect";
    public static final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
    //These are legitimate, randomly calculated values and can be used permanently
    public static final UUID EFFECT_TYPE_DS = UUID.fromString("46d279d9-9be7-453d-9d7c-ef937f675587");
    public static final UUID EFFECT_DS = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");

    protected static final int
        EFFECT_PARAM_SET_VALUES = 0,
        EFFECT_PARAM_VISUALIZER_ENABLE = 1,
        EFFECT_PARAM_VISUALIZER_DATA = 2,
        EFFECT_PARAM_VERSION = 3,
        EFFECT_PARAM_OFF_TYPE = 4,
        EFFECT_PARAM_DEFINE_PROFILE = 5;
        // Add more effect params here if required.

    protected AudioEffect audioEffect = null;
    private int audioSessionId_;

    // The byte size for the elements packed into the data array.
    private static final int activeDeviceSize_ = 4;
    private static final int paramCountSize_ = 4;
    private static final int deviceIdSize_ = 4;
    private static final int paramIdSize_ = 4;
    private static final int paramValueCountSize_ = 4;
    private static final int paramValueSize_ = 4;
    private static final int profileIndexSize_ = 4;
    /**
    * Endpoint enumeration values for DAP 'endp' parameter.
    */
    public static final int ENDP_INT_SPEAKERS = 0;
    public static final int ENDP_EXT_SPEAKERS = 1;
    public static final int ENDP_HEADPHONES   = 2;
    public static final int ENDP_HDMI         = 3;
    public static final int ENDP_SPDIF        = 4;
    public static final int ENDP_DLNA         = 5;
    public static final int ENDP_ANALOG       = 6;

    private static final String[] overrideParamNames = new String[] { "endp", "dvli", "dvlo", "vmb" };

    private static final int[][] overrideParams = new int[][]
    {
        // Output device                                    endp                        dvli     dvlo     vmb
        { AudioSystem.DEVICE_OUT_EARPIECE,                  ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_SPEAKER,                   ENDP_INT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_WIRED_HEADSET,             ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_WIRED_HEADPHONE,           ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_SCO,             ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_SCO_HEADSET,     ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_SCO_CARKIT,      ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP,            ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES, ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER,    ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_AUX_DIGITAL,               ENDP_HDMI,         -496,    -496,      0 },
        { AudioSystem.DEVICE_OUT_ANLG_DOCK_HEADSET,         ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_DGTL_DOCK_HEADSET,         ENDP_HEADPHONES,   -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_USB_ACCESSORY,             ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_USB_DEVICE,                ENDP_EXT_SPEAKERS, -320,    -320,    144 },
        { AudioSystem.DEVICE_OUT_REMOTE_SUBMIX,             ENDP_HDMI,         -496,    -496,      0 },
    };

    private void sendDapOverrides()
    {
        DsLog.log1 (LOG_TAG, "Sending parameter overrides to effect");
        //
        // Send EFFECT_CMD_SET_PARAM
        // EFFECT_PARAM_SET_VALUES
        //
        // Calculate the total array length
        int nParams = overrideParams.length * overrideParamNames.length;
        byte[] baValue = new byte[activeDeviceSize_ + paramCountSize_ + (deviceIdSize_ + paramIdSize_ + paramValueCountSize_ + paramValueSize_) * nParams];
        int index = 0;
        // Active audio device.
        index += int32ToByteArray(AudioSystem.DEVICE_OUT_DEFAULT, baValue, index);
        // Number of parameters.
        index += int32ToByteArray(nParams, baValue, index);
        for (int[] overrides : overrideParams)
        {
            for (int i = 0; i < overrideParamNames.length; ++i)
            {
                // Device ID to which the parameter will be applied.
                index += int32ToByteArray(overrides[0], baValue, index);
                // Parameter ID.
                int paramId = DsAkSettings.getAkParamId(overrideParamNames[i]);
                index += int32ToByteArray(paramId, baValue, index);
                // Parameter value count.
                index += int32ToByteArray(1, baValue, index);
                // Parameter value.
                index += int32ToByteArray(overrides[i + 1], baValue, index);
            }
        }
        int status = setParameter(EFFECT_PARAM_SET_VALUES, baValue);
        if (status != AudioEffect.SUCCESS)
        {
            Log.e(LOG_TAG, "sendOverrides: Error in sending override parameters" + status);
        }
    }

    private void sendDap2Dummy()
    {
        DsLog.log1 (LOG_TAG, "Sending parameter overrides to effect");
        //
        // Send EFFECT_CMD_SET_PARAM
        // EFFECT_PARAM_SET_VALUES
        //
        int nParams = Ds2ConstParam.singleParamArr.size() + 4;
   
        // Active audio device.
        byte[] baValue = audioEffect.intToByteArray(AudioSystem.DEVICE_OUT_DEFAULT);
        // Number of parameters.
        baValue = audioEffect.concatArrays(baValue, audioEffect.intToByteArray(nParams));

        // Start to involve Parameter
        for(int i = 0; i < Ds2ConstParam.singleParamArr.size(); i++) 
        {
            byte[] devIdBa = audioEffect.intToByteArray(AudioSystem.DEVICE_OUT_DEFAULT);
            byte[] paramIdBa = audioEffect.intToByteArray(Ds2ConstParam.singleParamArr.keyAt(i));
            byte[] paramSizeBa = audioEffect.intToByteArray(1);
            byte[] paramValBa = audioEffect.intToByteArray((int)((Integer)Ds2ConstParam.singleParamArr.valueAt(i)));
            baValue = audioEffect.concatArrays(baValue, devIdBa, paramIdBa, paramSizeBa, paramValBa);
        }

        baValue = audioEffect.concatArrays(baValue, 
                   Ds2ConstParam.multiByteArray[0], 
                   Ds2ConstParam.multiByteArray[1], 
                   Ds2ConstParam.multiByteArray[2], 
                   Ds2ConstParam.multiByteArray[3]);
        // Parameter end
        int status = audioEffect.setParameter(EFFECT_PARAM_SET_VALUES, baValue);
        if (status != AudioEffect.SUCCESS)
        {
            Log.e(LOG_TAG, "sendOverrides: Error in sending override parameters" + status);
        }
    }

    /**
     * Constructs a Ds OpenSL-ES AudioEffect.
     *
     * This implementation will formally call the AudioEffect super class
     * once Ds formally extends from AudioEffect.
     *
     * @param audioSessionId The audio session Id to be passed to the
     * AudioEffect constructor.
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @throws RuntimeException
     */
    public DsEffect(int audioSessionId) throws IllegalArgumentException, UnsupportedOperationException, RuntimeException
    {
        // Create the OpenSL-ES effect.
        audioEffect = new AudioEffect(EFFECT_TYPE_NULL, EFFECT_DS, 0, audioSessionId);
        DsLog.log2 (LOG_TAG, "Created DS AudioEffect successfully");

        // Let's find the descriptor of what we created.
        AudioEffect.Descriptor e = audioEffect.getDescriptor();
        DsLog.log1 (LOG_TAG, "CREATED EFFECT Implementor:\"" + e.implementor + "\"\n" +
                " name:\"" + e.name + "\"\n" +
                " connectMode:\"" + e.connectMode + "\"\n" +
                " type:\"" + e.type.toString() + "\"\n" +
                " uuid:\"" + e.uuid.toString() + "\"\n" +
                " sessionID:\"" + audioSessionId + "\"");

        audioSessionId_ = audioSessionId;
        // Send the override parameters to effect
        sendDapOverrides();
    }

    /**
     * Calls AudioEffect.release on the underlying AudioEffect.
     *
     * This implementation will not be required once Ds formally
     * extends from AudioEffect.
     */
    public void release()
    {
        audioEffect.release();
    }

    /**
     * Calls AudioEffect.setEnabled on the underlying AudioEffect.
     *
     * This implementation will not be required once Ds formally
     * extends from AudioEffect.
     *
     * @param enabled The new enabled state of the audio effect.
     * @return The value returned from AudioEffect.setEnabled.
     * @throws IllegalStateException
     */
    public int setEnabled(boolean enabled) throws IllegalStateException
    {
        return audioEffect.setEnabled(enabled);
    }

    /**
     * Calls AudioEffect.getEnabled on the underlying AudioEffect.
     *
     * This implementation will not be required once Ds formally
     * extends from AudioEffect.
     *
     * @return The enabled state of the AudioEffect.
     * @throws IllegalStateException
     */
    public boolean getEnabled() throws IllegalStateException
    {
        return audioEffect.getEnabled();
    }

    /**
     * Checks if this AudioEffect object is controlling the effect engine.
     *
     * This implementation will not be required once Ds formally
     * extends from AudioEffect.
     *
     * @return true if this instance has control of effect engine, false
     *         otherwise.
     * @throws IllegalStateException
     */
    public boolean hasControl() throws IllegalStateException
    {
        return audioEffect.hasControl();
    }

    private static byte[] int32ArrayToByteArray (int [] src)
    {
        int srcLength = src.length;
        byte[]dst = new byte[srcLength << 2];

        for (int i = 0; i < srcLength; i++)
        {
            int x = src[i];
            int j = i << 2;
            dst[j++] = (byte) ((x >>> 0) & 0xff);
            dst[j++] = (byte) ((x >>> 8) & 0xff);
            dst[j++] = (byte) ((x >>> 16) & 0xff);
            dst[j++] = (byte) ((x >>> 24) & 0xff);
        }
        return dst;
    }

    private static int int32ToByteArray(int value, byte[] dst, int index)
    {
        dst[index++] = (byte)(value & 0xff);
        dst[index++] = (byte)((value >>> 8) & 0xff);
        dst[index++] = (byte)((value >>> 16) & 0xff);
        dst[index] = (byte)((value >>> 24) & 0xff);
        return 4;
    }

    private static int stringToByteArray(String src, byte[] dst, int index) throws IllegalArgumentException
    {
        int len = src.length();
        if (len > 4)
        {
            Log.e(LOG_TAG, "parameter name " + src + " contains more than 4 characters");
            throw new IllegalArgumentException("Wrong parameter name");
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                dst[index++] = (byte)(src.charAt(i));
            }
            if (len < 4)
                dst[index] = (byte)'\0';
        }
        return 4;
    }

    private static int byteArrayToInt32(byte[] ba)
    {
        return ((ba[3] & 0xff) << 24) | ((ba[2] & 0xff) << 16) | ((ba[1] & 0xff) << 8) | (ba[0] & 0xff);
    }

    private static int[] byteArrayToInt32Array(byte[] ba)
    {
        int srcLength = ba.length;
        int destLength = srcLength >> 2;
        int[] dest = new int[destLength];

        for (int i = 0; i < destLength; i++)
        {
            dest[i] = ((ba[i * 4 + 3] & 0xff) << 24) | ((ba[i * 4 + 2] & 0xff) << 16) | ((ba[i * 4 + 1] & 0xff) << 8) | (ba[i * 4] & 0xff);
        }
        return dest;
    }

    private static String byteArrayToString(byte[] ba)
    {
        StringBuilder sb = new StringBuilder(3 + ba.length * 6);
        sb.append("HEX(");
        for (byte b : ba)
        {
            sb.append(Integer.toHexString((int)b));
            sb.append(' ');
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Pass the settings of a specified profile to the underlying effect.
     *
     * @param id      The profile index.
     * @param profile The profile settings.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int defineProfile(int id, DsProfileSettings profile)
    {
        DsLog.log1(LOG_TAG, "defineProfile: profile index " + id);

        //
        // Send EFFECT_CMD_SET_PARAM
        // EFFECT_PARAM_DEFINE_PROFILE
        //
        Object[] params = DsAkSettings.akSettableParamDefinitions.toArray();
        // Calculate the total array length
        int nParams = params.length;
        DsAkSettings akSettings = profile.getAllSettings();
        int index = 0;
        byte[] baValue = new byte[profileIndexSize_ + paramCountSize_ + (paramIdSize_ + paramValueCountSize_) * nParams + akSettings.getValues().length * paramValueSize_];
        // Profile index.
        index += int32ToByteArray(id, baValue, index);
        // Number of parameters.
        index += int32ToByteArray(nParams, baValue, index);
        for (int i = 0; i < nParams; ++i)
        {
            // Parameter ID.
            int paramId = DsAkSettings.getAkParamId((String)params[i]);
            index += int32ToByteArray(paramId, baValue, index);
            // Parameter value count.
            int length = DsAkSettings.getParamArrayLength((String)params[i]);
            index += int32ToByteArray(length, baValue, index);
            for (int offset = 0; offset < length; ++offset)
            {
                index += int32ToByteArray(akSettings.get((String)params[i], offset), baValue, index);
            }
        }
        int status = setParameter(EFFECT_PARAM_DEFINE_PROFILE, baValue);
        if (status != AudioEffect.SUCCESS)
        {
            DsLog.log1(LOG_TAG, "defineProfile: Fail to define profile " + id);
            return status;
        }
        return AudioEffect.SUCCESS;
    }

    /**
     * Set the parameter to the underlying Ds audio processing library.
     *
     * @param param   The integer containing the identifier of the parameter to set.
     * @param baValue The byte array containing the new value for the specified parameter.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int setParameter(int param, byte[] baValue) throws IllegalStateException
    {
        DsLog.log1(LOG_TAG, "setParameter param:" + param + ", baValue:" + byteArrayToString(baValue));
        return audioEffect.setParameter(param, baValue);
    }

    /**
     * Retrieve the parameter from the underlying Ds audio processing library.
     *
     * @param param   The integer containing the identifier of the parameter to get.
     * @param baValue The byte array to contain the value for the specified parameter.
     * @return The number of meaningful bytes retrieved in value array in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int getParameter(int param, byte[] baValue) throws IllegalStateException
    {
        int count = 0;

        count = audioEffect.getParameter(param, baValue);
        DsLog.log3(LOG_TAG, "getParameter param:" + param + ", baValue:" + byteArrayToString(baValue));
        DsLog.log3(LOG_TAG, "getParameter returns " + count);
        return count;
    }

    /**
     * Enable or disable the visualizer.
     * This enables or disables the visualizer in the underlying Ds library.
     * When disabled, visualizer data will not be updated and it therefore should not be retrieved.
     *
     * @param enable The new state of the visualizer.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int setVisualizerOn(boolean enable)
    {
        DsLog.log1(LOG_TAG, "setVisualizerOn");
        int on = enable ? DsAkSettings.AK_DS1_FEATURE_ON : DsAkSettings.AK_DS1_FEATURE_OFF;

        //
        // Send EFFECT_CMD_SET_PARAM
        // EFFECT_PARAM_VISUALIZER_ENABLE
        //
        byte[] baValue = new byte[4];
        int32ToByteArray(on, baValue, 0);
        return setParameter(EFFECT_PARAM_VISUALIZER_ENABLE, baValue);
    }

    /**
     * Get the enabled state of the visualizer.
     *
     * @return true if the visualizer is enabled. false otherwise.
     */
    public boolean getVisualizerOn()
    {
        DsLog.log1(LOG_TAG, "getVisualizerOn");
        boolean enabled = false;
        int count = 0;

        //
        // Send EFFECT_CMD_GET_PARAM
        // EFFECT_PARAM_VISUALIZER_ENABLE
        //
        byte[] baValue = new byte[4];
        count = getParameter(EFFECT_PARAM_VISUALIZER_ENABLE, baValue);
        if (count != 4)
        {
            Log.e(LOG_TAG, "getVisualizerOn: Error in getting the visualizer on/off state!");
        }
        else
        {
            int on = byteArrayToInt32(baValue);
            enabled = (on == DsAkSettings.AK_DS1_FEATURE_ON) ? true : false;
        }

        return enabled;
    }

    /**
     * Get the visualizer data from the underlying Ds audio processing library.
     * The retrieved visualizer data array starts with custom visualizer band gains, and followed by band excitations.
     * Band gains and excitations are concatenated, not interleaved.
     *
     * @return The visualizer data array retrieved on success, and null on failure.
     */
    public int[] getVisualizerData()
    {
        DsLog.log3(LOG_TAG, "getVisualizerData");
        int count = 0;

        //
        // Send EFFECT_CMD_GET_PARAM
        // EFFECT_PARAM_VISUALIZER_DATA
        //
        int numVisualizerData = DsAkSettings.getParamArrayLength("vcbg") + DsAkSettings.getParamArrayLength("vcbe");
        byte[] visualizerData = new byte[numVisualizerData * paramValueSize_];

        count = getParameter(EFFECT_PARAM_VISUALIZER_DATA, visualizerData);
        if (count != visualizerData.length)
        {
            return null;
        }

        return byteArrayToInt32Array(visualizerData);
    }

    /**
     * Set a single value for the specified parameter.
     * Note: The beginning offset of the parameter is always expected to be 0 here, based on the native implementation.
     *
     * The parameter and offset pair must be defined in DsAkSettings. setAllProfileSettings is expected to
     * be called before this method.
     * This behaviour is provided for debugging purposes and should not be relied upon.
     * @param parameter The DS parameter.
     * @param values The new value/values for the parameter setting.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int setSingleSetting(String parameter, int[] values)
    {
        DsLog.log1(LOG_TAG, "setSingleSetting: parameter " + parameter + ", length " + values.length);
        int begin = DsAkSettings.getAkSettingIndex(parameter, 0);
        int end = DsAkSettings.getAkSettingIndex(parameter, values.length - 1);
        if (begin == -1 || end == -1)
        {
            Log.e(LOG_TAG, "Attempt to set disallowed parameter and offset combination");
            return AudioEffect.ERROR_INVALID_OPERATION;
        }
        int paramId = DsAkSettings.getAkParamId(parameter);

        //
        // Send EFFECT_PARAM_SET_VALUES
        //
        byte[] baValue = new byte[activeDeviceSize_ + paramCountSize_ + deviceIdSize_ + paramIdSize_ + paramValueCountSize_ + values.length * paramValueSize_];
        int index = 0;
        // Active audio device.
        // TODO: Now we rely on the EFFECT_CMD_SET_DEVICE command in the native layer to report
        //       the active audio device. We'll revisit here once the endpoint intelligence feature
        //       is ready.
        index += int32ToByteArray(AudioSystem.DEVICE_OUT_DEFAULT, baValue, index);
        // Number of parameters (Only 1 in this case).
        index += int32ToByteArray(1, baValue, index);
        // Device ID to which the parameter will be applied.
        index += int32ToByteArray(AudioSystem.DEVICE_OUT_DEFAULT, baValue, index);
        // Parameter ID.
        index += int32ToByteArray(paramId, baValue, index);
        // Parameter value count.
        index += int32ToByteArray(values.length, baValue, index);
        // Parameter values.
        for (int j = 0; j < values.length; j++)
            index += int32ToByteArray(values[j], baValue, index);
        return setParameter(EFFECT_PARAM_SET_VALUES, baValue);
    }

    /**
     * Set all the profile-only settings.
     *
     * @param settings The new profile settings.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     */
    public int setAllProfileSettings(DsProfileSettings settings)
    {
        DsLog.log1(LOG_TAG, "setAllProfileSettings");
        DsAkSettings akSettings = settings.getAllSettings();

        //
        // Send EFFECT_CMD_SET_PARAM
        // EFFECT_PARAM_SET_VALUES
        //
        Object[] params = DsAkSettings.akSettableParamDefinitions.toArray();
        // Calculate the total array length
        int nParams = params.length;
        byte[] baValue = new byte[activeDeviceSize_ + paramCountSize_ + (deviceIdSize_ + paramIdSize_ + paramValueCountSize_) * nParams + akSettings.getValues().length * paramValueSize_];
        int index = 0;
        // Active audio device.
        index += int32ToByteArray(AudioSystem.DEVICE_OUT_DEFAULT, baValue, index);
        // Number of parameters.
        index += int32ToByteArray(nParams, baValue, index);
        for (int i = 0; i < params.length; ++i)
        {
            // Device ID to which the parameter will be applied.
            index += int32ToByteArray(AudioSystem.DEVICE_OUT_DEFAULT, baValue, index);
            // Parameter ID.
            int paramId = DsAkSettings.getAkParamId((String)params[i]);
            index += int32ToByteArray(paramId, baValue, index);
            // Parameter value count.
            int length = DsAkSettings.getParamArrayLength((String)params[i]);
            index += int32ToByteArray(length, baValue, index);
            for (int offset = 0; offset < length; ++offset)
            {
                index += int32ToByteArray(akSettings.get((String)params[i], offset), baValue, index);
            }
        }
        return setParameter(EFFECT_PARAM_SET_VALUES, baValue);
    }

    /**
     * Get the version of DS1AK library, the format is 4 numbers.
     *
     * @return The version of the Ds audio processing library.
     */
    public String getVersion()
    {
        //
        // Send EFFECT_CMD_GET_PARAM
        //
        int verLen = 32;
        byte[] version = new byte[verLen];
        getParameter(EFFECT_PARAM_VERSION, version);
        String strFull = new String(version);
        // find the '\0' in c/c++ format
        int endPos = strFull.indexOf(0);
        String strVer = new String(version, 0, endPos);
        return strVer;
    }

    /**
     * Get the off type of DAP, off profile or bypass mode.
     *
     * @return The off type (either off profile or bypass) adopted by the effect.
     * @throws IllegalStateException if the DsEffect cannot be used.
     */
    public int getOffType() throws IllegalStateException
    {
        int count = 0;
        int offType = DsConstants.DS_OFF_BYPASSED_TYPE;

        //
        // Send EFFECT_CMD_GET_PARAM
        // EFFECT_PARAM_OFF_TYPE
        //
        byte[] baValue = new byte[4];
        count = getParameter(EFFECT_PARAM_OFF_TYPE, baValue);
        if (count != 4)
        {
            Log.e(LOG_TAG, "getOffType: Error in getting the effect off type!");
            throw new IllegalStateException("Wrong processing of EFFECT_PARAM_OFF_TYPE");
        }
        else
        {
            offType = byteArrayToInt32(baValue);
        }

        return offType;
    }
}
