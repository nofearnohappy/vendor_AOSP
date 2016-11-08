package com.mediatek.camera.util.json;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {
    private final static String TAG = "mtkGallery2/JsonParser";
    private JSONObject mJsonObject;
    private final static int INVALID_VALUE = -1;

    public JsonParser(String jsonString) {
        try {
            mJsonObject = new JSONObject(jsonString);
        } catch (JSONException exception) {
            Log.e(TAG, "<JsonParser> exception", exception);
        }
    }

    public JsonParser(byte[] jsonBuffer) {
        try {
            mJsonObject = new JSONObject(new String(jsonBuffer));
        } catch (JSONException exception) {
            Log.e(TAG, "<JsonParser> exception", exception);
        }
    }

    public int getValueIntFromObject(String objectName,
            String subObjectName, String propertyName) {
        int value = INVALID_VALUE;
        if (mJsonObject == null || objectName == null || propertyName == null) {
            Log.d(TAG, "<getValueIntFromObject> error!!");
            return INVALID_VALUE;
        }
        try {
            JSONObject object = mJsonObject.getJSONObject(objectName);
            object = subObjectName == null ? object : object.getJSONObject(subObjectName);
            if (object != null) {
                value = object.getInt(propertyName);
            }
            return value;
        } catch (JSONException exception) {
            Log.e(TAG, "<getValueIntFromObject> exception", exception);
            return INVALID_VALUE;
        }
    }

    public int[][] getInt2DArrayFromObject(String objectName, String arrayName) {
        int[][] array = null;
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getInt2DArrayFromObject> error!!");
            return null;
        }
        try {
            JSONObject object = objectName == null ? mJsonObject : mJsonObject
                    .getJSONObject(objectName);
            JSONArray jsonArray = object.getJSONArray(arrayName);
            if (jsonArray != null) {
                int len = jsonArray.length();
                JSONArray jsonSubArray = jsonArray.getJSONArray(0);
                int subArrayLen = jsonSubArray.length();
                array = new int[len][subArrayLen];
                for (int i = 0; i < len; i++) {
                    jsonSubArray = jsonArray.getJSONArray(i);
                    if (jsonSubArray != null) {
                        for (int j = 0; j < subArrayLen; j++) {
                            array[i][j] = jsonSubArray.getInt(j);
                        }
                    }
                }
            }
            return array;
        } catch (JSONException exception) {
            Log.e(TAG, "<getInt2DArrayFromObject> exception", exception);
            return null;
        }
    }

    public int[] getIntArrayFromObject(String objectName, String arrayName) {
        int[] array = null;
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getIntArrayFromObject> error!!");
            return null;
        }
        try {
            JSONObject object = objectName == null ? mJsonObject : mJsonObject
                    .getJSONObject(objectName);
            if (object != null) {
                JSONArray jsonArray = object.getJSONArray(arrayName);
                if (jsonArray != null) {
                    int len = jsonArray.length();
                    array = new int[len];
                    for (int i = 0; i < len; i++) {
                        array[i] = jsonArray.getInt(i);
                    }
                }
            }
            return array;
        } catch (JSONException exception) {
            Log.e(TAG, "<getIntArrayFromObject> exception", exception);
            return null;
        }
    }

    public int getObjectPropertyValueFromArray(String arrayName,
            int index, String propertyName) {
        if (mJsonObject == null || arrayName == null || propertyName == null) {
            Log.d(TAG, "<getObjectPropertyValueFromArray> error!!");
            return -1;
        }
        try {
            JSONArray jsonArray = mJsonObject.getJSONArray(arrayName);
            if (jsonArray != null) {
                int len = jsonArray.length();
                if (index < 0 || index > len) {
                    Log.d(TAG, "<getObjectPropertyValueFromArray> index error: " + index);
                    return -1;
                }
                JSONObject object = jsonArray.getJSONObject(index);
                if (object != null) {
                    return object.getInt(propertyName);
                }
            }
            return -1;
        } catch (JSONException exception) {
            Log.e(TAG, "<getObjectPropertyValueFromArray> exception", exception);
            return -1;
        }
    }

    public int getArrayLength(String arrayName) {
        if (mJsonObject == null || arrayName == null) {
            Log.d(TAG, "<getArrayLength> error!!");
            return -1;
        }
        try {
            JSONArray jsonArray = mJsonObject.getJSONArray(arrayName);
            if (jsonArray != null) {
                return jsonArray.length();
            }
            return -1;
        } catch (JSONException exception) {
            Log.e(TAG, "<getArrayLength> exception", exception);
            return -1;
        }
    }
}
