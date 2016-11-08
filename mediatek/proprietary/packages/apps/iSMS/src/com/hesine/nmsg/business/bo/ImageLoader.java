package com.hesine.nmsg.business.bo;

import java.util.ArrayList;
import java.util.List;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ImageInfo;

public class ImageLoader implements Pipe {

    private List<ImageWorker> mImageQueue = new ArrayList<ImageWorker>();
    private Pipe listener = null;

    public void setListener(Pipe listener) {
        this.listener = listener;
    }

    public boolean isRequestExisted(ImageInfo ii) {
        for (ImageWorker imageWorker : mImageQueue) {
            if (imageWorker.getImageInfo().getUrl().equals(ii.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public void request(ImageInfo ii) {
        if (ii == null || isRequestExisted(ii)) {
            return;
        }
        ImageWorker api = new ImageWorker();
        api.setImageInfo(ii);
        api.setListener(this);
       // if (mImageQueue.size() <= 0) {
            api.request();
       // }
        mImageQueue.add(api);
    }

    @Override
    public void complete(Object owner, Object data, int success) {
        if (null != listener) {
            listener.complete(this, data, success);
        }
        ImageWorker api = (ImageWorker)owner;
        mImageQueue.remove(api);
//        if (mImageQueue.size() > 0) {
//            mImageQueue.get(0).request();
//        }
    }

}
