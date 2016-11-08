package com.mediatek.selfregister.utils;

import android.util.Base64;
import android.util.Log;

import com.mediatek.selfregister.Const;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Utils {
    private static final String TAG = Const.TAG_PREFIX + "Utils";

    private static final String SERVER_URL = "http://zzhc.vnet.cn";
    private static final String CONTENT_TYPE = "application/encrypted-json";

    private static final String JSON_RESULT_CODE = "resultCode";
    private static final String JSON_RESULT_DESC = "resultDesc";

    public static JSONObject httpSend(String data) {
        return httpSend(SERVER_URL, data);
    }

    /**
     * Send message to the server.
     * @param url CT's server.
     * @param data The message data to be sent.
     * @return JSONObject Response from server or null if error.
     */
    private static JSONObject httpSend(String url, String data) {
        Log.d(TAG, "Enter httpSend()...");
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        JSONObject result = null;

        try {
            StringEntity entity = new StringEntity(data);
            entity.setContentType(CONTENT_TYPE);
            post.setEntity(entity);

            Log.d(TAG, "httpSend(), ===Before execute()");
            HttpResponse response = httpClient.execute(post);
            Log.d(TAG, "httpSend(), ===After execute()");

            int statusCode = response.getStatusLine().getStatusCode();
            Log.d(TAG, "httpSend(), Status code: " + statusCode);

            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity respEntity = response.getEntity();
                String charset = EntityUtils.getContentCharSet(respEntity);
                result = new JSONObject(EntityUtils.toString(respEntity, charset));
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in httpSend()!");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in httpSend()!");
            e.printStackTrace();
        }

        return result;
    }

    public static boolean checkRegisterResult(JSONObject response) {
        if (response == null) {
            Log.e(TAG, "checkRegisterResult(), response is null!");
            return false;
        }

        int resultCode = -1;
        String resultDesc = null;

        try {
            resultCode = response.getInt(JSON_RESULT_CODE);
            resultDesc = response.getString(JSON_RESULT_DESC);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Result code " + resultCode + ", desc " + resultDesc);

        if (resultCode == 0) {
            return true;
        }

        return false;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");

        if (src == null || src.length <= 0) {
            return null;
        }

        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }

        return stringBuilder.toString();
    }

    public static String encodeBase64(String data) {
        byte[] encodeByte = Base64.encode(data.getBytes(), Base64.DEFAULT);
        return new String(encodeByte);
    }
}
