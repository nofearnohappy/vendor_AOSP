package com.hesine.nmsg.business.bo;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.ImageInfo;
import com.hesine.nmsg.business.bean.PicInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.thirdparty.Statistics;

public class GetData
        extends
        com.hesine.nmsg.business.bo.Base<com.hesine.nmsg.business.bean.request.Base, 
                                    com.hesine.nmsg.business.bean.response.GetData> {

    private String serviceAccount = null;

    @Override
    public com.hesine.nmsg.business.bean.request.Base contentObject() {
        com.hesine.nmsg.business.bean.request.Base obj = new com.hesine.nmsg.business.bean.request.Base();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_GET_DATA);
        obj.getActionInfo().setServiceAccount(serviceAccount);
        return obj;
    }

    @Override
    public com.hesine.nmsg.business.bean.response.GetData parseObject() {
        return new com.hesine.nmsg.business.bean.response.GetData();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.GetData parseData, int success) {
        if (success == Pipe.NET_SUCCESS) {
            ServiceInfo serviceInfo = parseData.getServiceInfo();
            PicInfo picInfo = parseData.getPicInfo();
            serviceInfo.setAccount(serviceAccount);
            String account = serviceInfo.getAccount();
            if (!DBUtils.ifServiceAccountExisted(account)) {
                long id = DBUtils.insertServiceInfo(serviceInfo, picInfo);
                if (id >= 0) {
                    Statistics.getInstance().accountStatus(account, String.valueOf(1));
                    NmsgApiProvider.accounts.add(account);
                    serviceInfo.setIcon(PicInfo.getPicPath(picInfo));
                    if (!CommonUtils.isExistSystemContactViaAccount(serviceInfo, false)) {
                        CommonUtils.addContactInPhonebook(serviceInfo);
                    }
                }
            } else {
                DBUtils.updateServiceInfo(serviceInfo, picInfo);
                if (CommonUtils.isExistSystemContactViaAccount(serviceInfo, false)) {
                    CommonUtils.updateContact(serviceInfo);
                }
            }
            getAccountAvatar(picInfo);
        }
    }

    public void getAccountAvatar(PicInfo pi) {
        if (FileEx.isFileExisted(PicInfo.getPicPath(pi))) {
            FileEx.deleteFile(PicInfo.getPicPath(pi));
        }
        ImageWorker mImageWorker = new ImageWorker();
        mImageWorker.setListener(new Pipe() {
            @Override
            public void complete(Object owner, Object data, int success) {
                CommonUtils.updateContactPhoto(DBUtils.getServiceInfo(serviceAccount));
            }
        });
        ImageInfo ii = new ImageInfo();
        ii.setPath(PicInfo.getPicPath(pi));
        ii.setUrl(pi.getUrl());
        mImageWorker.setImageInfo(ii);
        mImageWorker.request();
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }
}
