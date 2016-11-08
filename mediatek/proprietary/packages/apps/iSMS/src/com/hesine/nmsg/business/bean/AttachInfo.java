package com.hesine.nmsg.business.bean;

import java.io.File;

import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.common.MLog;

public class AttachInfo {

    public static final int ATTACH_NONE = 0;
    public static final int ATTACH_PIC = 1;
    public static final int ATTACH_AUDIO = 2;
    public static final int ATTACH_VIDEO = 3;

    public static final String SAVE_DIR = File.separator + EnumConstants.ROOT_DIR + File.separator
            + "attach/";

    private String type = null;
    private String name = null;
    private String size = null;
    private String url = null;
    private String attachment = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static String getAttachmentAbsPath(AttachInfo ai) {
        String ret = null;
        String dir = null;
        if (null != ai) {
            String sType = ai.getType();
            int type = ATTACH_NONE;
            if (null != sType && sType.length() > 0) {
                try {
                    type = Integer.parseInt(sType);
                } catch (NumberFormatException e) {
                    MLog.error(e.toString());
                }
            }
            switch (type) {
                case ATTACH_PIC:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "picture/";
                    }
                    break;
                case ATTACH_AUDIO:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "audio/";
                    }
                    break;
                case ATTACH_VIDEO:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "video/";
                    }
                    break;
                default:
                    break;
            }
        }
        if (null != dir) {
            ret = dir + ai.getName();
        }
        return ret;
    }

    public static String getAttachmentAbsPathByUrl(AttachInfo ai) {
        String ret = null;
        String dir = null;
        if (null != ai) {
            int type = Integer.parseInt(ai.getType());
            switch (type) {
                case ATTACH_PIC:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "picture/";
                    }
                    break;
                case ATTACH_AUDIO:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "audio/";
                    }
                    break;
                case ATTACH_VIDEO:
                    if (FileEx.getSDCardStatus()) {
                        dir = FileEx.getSDCardPath() + SAVE_DIR + "video/";
                    }
                    break;
                default:
                    break;
            }
        }
        if (null != dir) {
            ret = dir + ai.getUrl().substring(ai.getUrl().lastIndexOf('/') + 1);
        }
        return ret;
    }

    public static boolean saveAttachment(AttachInfo ai) {
        boolean ret = false;
        // String absPath = getAttachmentAbsPath(ai);
        // if(null != absPath) {
        // boolean isAttachmentExisted = FileEx.ifFileExisted(absPath);
        // if(!isAttachmentExisted) {
        // try {
        // String data = Utility.base64Decode(ai.getAttachment());
        // FileEx.write(absPath, data.getBytes("ISO-8859-1"));
        // } catch (Exception e) {
        // MLog.error(e.toString());
        // }
        // ret = true;
        // }
        // }
        return ret;
    }
}
