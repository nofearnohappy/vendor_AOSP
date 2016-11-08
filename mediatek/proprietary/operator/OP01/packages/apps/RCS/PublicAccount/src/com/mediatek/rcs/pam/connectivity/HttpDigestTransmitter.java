package com.mediatek.rcs.pam.connectivity;

import android.content.Context;
import android.util.Log;

import com.android.okhttp.Authenticator;
import com.android.okhttp.Challenge;
import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.okhttp.ResponseBody;
import com.mediatek.rcs.pam.CommonHttpHeader;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.client.PAMClient.Response;
import com.mediatek.rcs.pam.client.PAMClient.Transmitter;

import org.apache.commons.io.IOUtils;

import com.android.okhttp.internal.http.MessageDigestAlgorithm;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

public class HttpDigestTransmitter implements Transmitter {
    public static final String TAG = "PAM/HttpDigestTransmitter";
    private static final String WWW_AUTH = "WWW-Authenticate";

    private String mServerUrl;
    private String mPassword;
    private Context mContext;

    public HttpDigestTransmitter(Context context, String serverUrl, String password) {
        mServerUrl = serverUrl;
        mPassword = password;
        mContext = context;
    }

    @Override
    public Response sendRequest(String msgname, String content, boolean postOrGet)
            throws IOException {
        Log.d(TAG, "sendRequest: " + msgname);
        Log.d(TAG, content);
        Response result = new Response();
        try {
            OkHttpClient client = new OkHttpClient();
            final String method = HttpTransmitter.POST;

            client.setAuthenticator(new Authenticator() {

                @Override
                public Request authenticateProxy(Proxy proxy, com.android.okhttp.Response response)
                        throws IOException {
                    return null;
                }

                @Override
                public Request authenticate(Proxy proxy, com.android.okhttp.Response response)
                        throws IOException {
                    Log.d(TAG, "response = " + response);
                    Map<String, String> values = parseAuth(response.header(WWW_AUTH));
                    Challenge challenge = new Challenge(values.get("scheme"), values.get("realm"),
                            values.get("nonce"), values.get("stale"), "auth", values.get("opaque"));

                    String userName = PlatformManager.getInstance().getUserId(mContext);
                    String url = "/";
                    String nc_value = "00000001";
                    String cnonce = "c4ed7d77e50ebf05cae26e998b5d09d3";
                    String sresponse = MessageDigestAlgorithm.calculateResponse("MD5", userName,
                            challenge.getRealm(), mPassword, challenge.getNonce(), nc_value,
                            cnonce, method, url, null, challenge.getQop());
                    String header = " username=\"" + userName + "\", realm=\""
                            + challenge.getRealm() + "\", nonce=\"" + challenge.getNonce()
                            + "\", uri=\"" + url + "\", response=\"" + sresponse + "\", qop="
                            + challenge.getQop() + ", nc=" + nc_value + ", cnonce=\"" + cnonce
                            + "\", algorithm=MD5" + "\", opaque=\"" + challenge.getOpaque() + "\"";

                    String credential = "Digest " + header;
                    return response.request().newBuilder().header("Authorization", credential)
                            .build();
                }
            });

            Request request = null;
            MediaType contentType = MediaType.parse("text/xml");
            RequestBody resuestBody = RequestBody.create(contentType, content);

            request = new Request.Builder()
                    .url(mServerUrl)
                    .method(method, resuestBody)
                    .header(CommonHttpHeader.ACCEPT_ENCODING,
                            CommonHttpHeader.ACCEPT_ENCODING_VALUE)
                    .header(CommonHttpHeader.X_3GPP_INTENDED_IDENTITY,
                            PlatformManager.getInstance().getIdentity(mContext)).build();
            Log.d(TAG, "request: " + request);
            com.android.okhttp.Response httpResponse = client.newCall(request).execute();
            ResponseBody responseBody = httpResponse.body();
            result.result = httpResponse.code();
            result.content = IOUtils.toString(responseBody.byteStream(), "UTF-8");
            Log.d(TAG, "httpResponse: " + httpResponse);
            Log.d(TAG, "Status Code: " + result.result);
            Log.d(TAG, "Response Content: " + result.content);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
        return result;
    }

    private Map<String, String> parseAuth(String auth) {
        Log.d(TAG, "auth: " + auth);
        auth += ",";
        Map<String, String> result = new HashMap<String, String>();

        String key = null;
        StringBuilder build = new StringBuilder();
        int count = 0;
        for (int i = 0; i < auth.length(); i++) {
            char c = auth.charAt(i);
            if (c == '"') {
                count++;
                continue;
            } else if (c == ',') {
                if (count % 2 == 0) {
                    result.put(key, build.toString());
                    build.delete(0, build.length());
                    continue;
                }
            } else if (c == '=') {
                if (count % 2 == 0) {
                    key = build.toString();
                    build.delete(0, build.length());
                    continue;
                }
            }
            build.append(c);
        }
        Log.d(TAG, "result: " + result);
        return result;
    }
}
