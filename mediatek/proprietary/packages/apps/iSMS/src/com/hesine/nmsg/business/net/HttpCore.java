package com.hesine.nmsg.business.net;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.common.MLog;

public class HttpCore extends AsyncTask<Object, Object, Object> {

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 5000 * 10;
    public static final int BUFFER_SIZE = 2 * 1024;
    public static final byte[] BUFFER = new byte[BUFFER_SIZE];
    private static final int RETRY_TIME_LIMIT = 3;
    private boolean httpRuning = false;
    private RequestTask task = null;
    private Pipe listener = null;
    private int retryCount = RETRY_TIME_LIMIT;

    public static final int CODE_SUCCESS = 1;
    public static final int CODE_FAILED = 0;
    public static final int CODE_TIMEOUT = -1;
    private int code = CODE_SUCCESS;

    public HttpCore() {
    }

    public void start(RequestTask task, Pipe listener) {
        this.task = task;
        this.listener = listener;
//        execute();
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public RequestTask getTask() {
        return task;
    }

    void get(RequestTask task) {
        try {
            retryCount--;
            code = CODE_SUCCESS;           
            HttpGet httpGet = new HttpGet(task.getUrl());            
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                    CONNECT_TIMEOUT );
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
                    READ_TIMEOUT);
            
            HttpResponse httpResp = httpClient.execute(httpGet);
            int code = httpResp.getStatusLine().getStatusCode();
            if(code != 200){
                MLog.error("http resule is error code : " + code);
            }else{
                task.setParseData(EntityUtils.toByteArray(httpResp.getEntity()));
            }            
        } catch (SocketTimeoutException e) {
            code = CODE_TIMEOUT;
            MLog.error(MLog.getStactTrace(e));
        } catch (ConnectTimeoutException e) {
            code = CODE_TIMEOUT;
            MLog.error(MLog.getStactTrace(e));
        } catch (IOException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (IndexOutOfBoundsException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (NullPointerException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (Exception e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } finally {
            if ((code == CODE_FAILED || code == CODE_TIMEOUT) && retryCount > 0) {
                get(task);
            } else {
                retryCount = RETRY_TIME_LIMIT;
            }
        }
    }

    void post(RequestTask task) {
        retryCount--;
        code = CODE_SUCCESS;
        try {            
            HttpPost httpPost = new HttpPost(task.getUrl());
            httpPost.setEntity(new ByteArrayEntity(task.getConstructData()));
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                    CONNECT_TIMEOUT );
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
                    READ_TIMEOUT );
            
            HttpResponse httpResp = httpClient.execute(httpPost);
            int code = httpResp.getStatusLine().getStatusCode();
            if(code != 200){
                MLog.error("httppost result is error " + code);
            }else{
                task.setParseData(EntityUtils.toByteArray(httpResp.getEntity()));   
            }
            
        } catch (SocketTimeoutException e) {
            code = CODE_TIMEOUT;
            MLog.error(MLog.getStactTrace(e));
        } catch (ConnectTimeoutException e) {
            code = CODE_TIMEOUT;
            MLog.error(MLog.getStactTrace(e));
        } catch (IOException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (IndexOutOfBoundsException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (NullPointerException e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } catch (Exception e) {
            code = CODE_FAILED;
            MLog.error(MLog.getStactTrace(e));
        } finally {
            if ((code == CODE_FAILED || code == CODE_TIMEOUT) && retryCount > 0) {
                post(task);
            } else {
                retryCount = RETRY_TIME_LIMIT;
            }
        }
    }

    protected void cancel() {
        if (httpRuning ) {
            try {
                this.cancel(true);
                this.onCancelled();
            } catch (Exception e) {
                MLog.error(MLog.getStactTrace(e));
            }
        }
    }

    protected void onCancelled(Object result) {
        super.onCancelled();
        httpRuning = false;
        listener.complete(task, this, 1);
    }

    @Override
    protected Object doInBackground(Object... params) {
        if (isCancelled()) {
            return null;
        }
        if (task.getMethod().equals("POST")) {
            post(task);
        } else if (task.getMethod().equals("GET")) {
            get(task);
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        httpRuning = true;
    }

    @Override
    protected void onProgressUpdate(Object... values) {
    }

    @Override
    protected void onPostExecute(Object result) {
        cancel();
        httpRuning = false;
        int ret = Pipe.NET_SUCCESS;
        if (code == CODE_FAILED) {
            ret = Pipe.NET_FAIL;
        } else if (code == CODE_TIMEOUT) {
            ret = Pipe.NET_TIMEOUT;
        }
        listener.complete(task, this, ret);
    }
}
