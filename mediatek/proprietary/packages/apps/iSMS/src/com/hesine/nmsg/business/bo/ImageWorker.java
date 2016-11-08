package com.hesine.nmsg.business.bo;

import java.io.IOException;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ImageInfo;
import com.hesine.nmsg.business.net.Http;
import com.hesine.nmsg.business.net.RequestTask;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.common.MLog;

public class ImageWorker implements Pipe {

    protected boolean isRequesting = false;
    RequestTask task = null;
    protected Pipe listener = null;
    private ImageInfo imageInfo = null;

    public void setListener(Pipe listener) {
        this.listener = listener;
    }

    public boolean isRequesting() {
        return this.isRequesting;
    }

    public void request() {
        if (this.isRequesting) {
            return;
        }
        isRequesting = true;
        task = Http.instance().get(imageInfo.getUrl(), this);
    }

    public static byte[] getData(ImageInfo ii) {
        String path = ii.getPath();
        if (FileEx.isFileExisted(path)) {
            try {
                byte[] buffer = FileEx.readFile(path);
                return buffer;
            } catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
            }
        }
        return null;
    }

    public void cancel() {
        if (null != task) {
            task.setListener(null);
            Http.instance().cancel(task);
            task = null;
        }
        this.isRequesting = false;
    }

    @Override
    public void complete(Object owner, Object data, int success) {
        isRequesting = false;
        RequestTask t = (RequestTask) owner;
        byte[] retData = t.getParseData();
        int retCode = Pipe.NET_FAIL;
        if (null == retData) {
            retCode = Pipe.NET_FAIL;
        } else {
            FileEx.write(imageInfo.getPath(), retData);
            retCode = Pipe.NET_SUCCESS;
        }
        if (null != listener) {
            listener.complete(this, imageInfo, retCode);
        }
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

}
