package com.mediatek.rcs.pam.connectivity;

import android.content.Context;
import android.util.Log;

import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.okhttp.ResponseBody;
import com.mediatek.rcs.pam.CommonHttpHeader;
import com.mediatek.rcs.pam.client.PAMClient.Response;
import com.mediatek.rcs.pam.client.PAMClient.Transmitter;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;

public class HttpTransmitter implements Transmitter {
    public static final String TAG = "PAM/HttpTransmitter";
    public static final String POST = "POST";
    public static final String GET = "GET";

    private String mServerUrl;

    public HttpTransmitter(Context context, String serverUrl) {
        mServerUrl = serverUrl;
    }

    @Override
    public Response sendRequest(String msgname, String content, boolean postOrGet) {
        Log.d(TAG, "sendRequest: " + msgname);
        Log.d(TAG, content);
        Response result = new Response();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = null;
            MediaType contentType = MediaType.parse("text/xml");
            RequestBody resuestBody = RequestBody.create(contentType, content);
            String method = POST;
            request = new Request.Builder()
                    .url(mServerUrl)
                    .method(method, resuestBody)
                    .header(CommonHttpHeader.ACCEPT_ENCODING,
                            CommonHttpHeader.ACCEPT_ENCODING_VALUE).build();
            com.android.okhttp.Response httpResponse = client.newCall(request).execute();
            ResponseBody responseBody = httpResponse.body();
            result.result = httpResponse.code();
            result.content = IOUtils.toString(responseBody.byteStream(), "UTF-8");
            Log.d(TAG, "httpResponse: " + httpResponse);
            Log.d(TAG, "Status Code: " + result.result);
            Log.d(TAG, "Response Content: " + result.content);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            result.result = -1;
        } catch (IOException e) {
            e.printStackTrace();
            result.result = -1;
        }
        return result;
    }
}
