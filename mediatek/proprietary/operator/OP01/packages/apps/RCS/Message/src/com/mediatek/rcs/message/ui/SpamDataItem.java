package com.mediatek.rcs.message.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.plugin.EmojiImpl;

import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.io.File;

/**
 * The favorite and spam data is wraped a FavoriteSpamItem type to
 * load by adaptor.
 */
public class SpamDataItem {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/SpamDataItem";

    protected Context mContext;
    private boolean mChecked;
    private int mType;
    private String mTypeName;
    private ISpamData mTypeData;

    // common property
    protected Bitmap mImage;
    private long mDate;
    private String mFrom;
    private String mAddress;
    protected int mMsgId;
    private String mPath;
    private String mSize;
    private String mCt;
    private static final Uri FT_IPMSG_CONTENT_URI =
            Uri.parse("content://org.gsma.joyn.provider.ft/ft");

    /**
     * construct method.
     * @param context the activity that display data.
     * @param msgId message id.
     */
    public SpamDataItem(Context context, Cursor cursor) {
        mMsgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
        mContext = context;
        mImage = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.ic_default_contact);
        mFrom = mContext.getString(R.string.unknown);
        mSize = mContext.getString(R.string.unknown);
        mDate = System.currentTimeMillis();
    }

    /**
     * Get data from database and wrapping it to a FavoriteSpamItem.
     * @param cursor The data item cursor.
     * @param portraitService It's used to get contact infomation.
     * @return methord excute result.
     */
    public boolean initData(Cursor cursor, PortraitService portraitService) {
        if (cursor == null) {
            Log.e(TAG, "setData error cursor is null");
            return false;
        }
        this.setFrom(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS)));
        this.setDate(cursor.getLong(cursor.getColumnIndex(Constants.COLUMN_NAME_DATE)));

        /**
         * one to one chat message, it is need get group name from db if a muti.
         * chat msg. so group msg need add other branch to do with.
         **/
        mAddress = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS));
        if (portraitService != null) {
            Portrait protrait = portraitService.requestPortrait(mAddress);
            mImage = PortraitService.decodeString(protrait.mImage);
            mFrom = protrait.mName;
        }

        mType = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        mTypeData = createTypeData(mType, cursor);
        if (mTypeData == null) {
            Log.e(TAG, "initData fail");
            return false;
        }
        mTypeData.initTypeData(cursor);
        return true;
    }

    private void analysisFileType(String filePath) {
       if (filePath != null) {
          String mimeType = MediaFile.getMimeTypeForFile(filePath);
          if (mimeType == null) {
              mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                      RCSUtils.getFileExtension(filePath));
          }
          if (mimeType != null) {
              if (mimeType.contains(Constants.FILE_TYPE_IMAGE)) {
                  mCt = Constants.CT_TYPE_IMAGE;
              } else if (mimeType.contains(Constants.FILE_TYPE_AUDIO)
                      || mimeType.contains("application/ogg")) {
                  mCt = Constants.CT_TYPE_AUDIO;
              } else if (mimeType.contains(Constants.FILE_TYPE_VIDEO)) {
                  mCt = Constants.CT_TYPE_VEDIO;
              } else if (filePath.toLowerCase().endsWith(".vcf")) {
                  mCt = Constants.CT_TYPE_VCARD;
              } else if (filePath.toLowerCase().endsWith(".xml")) {
                  mCt = Constants.CT_TYPE_GEOLOCATION;
              } else {
                  Log.d(TAG, "analysisFileType() other type add here!");
              }
          }
      } else {
          Log.w(TAG, "analysisFileType(), file name is null!");
      }
      return;
  }

    private ISpamData createTypeData(int mType2, Cursor cursor) {
        Log.d(TAG, "createTypeData");
        switch (mType2) {
        case SpamMsgData.Type.TYPE_EMOTICON_MSG:
            return new VemoticonData();

        case SpamMsgData.Type.TYPE_SMS:
            return new TextData();

        case SpamMsgData.Type.TYPE_IP_BURN_TEXT_MSG:
        case SpamMsgData.Type.TYPE_IP_BURN_FT_MSG:
            return new BurnTextData();

        case SpamMsgData.Type.TYPE_MMS_PUSH:
            return new MmsData();

        case SpamMsgData.Type.TYPE_IP_TEXT_MSG:
            return new TextData();

        case SpamMsgData.Type.TYPE_IP_FT_MSG:
          //  this.setPath(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY)));

            int ipmsgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_IPMSGID));
            Cursor ftCr = mContext.getContentResolver()
            .query(FT_IPMSG_CONTENT_URI, new String[]{"filename"}, "_id is " + ipmsgId, null, null);
            ftCr.moveToFirst();
            mPath = ftCr.getString(ftCr.getColumnIndex("filename"));
            Log.d(TAG, "mPath = " + mPath);
            if (ftCr != null) {
                ftCr.close();
            }

            this.setPath(mPath);
            if (mPath == null) {
               return new FTData();
             }
            File file = new File(mPath);
            if (file == null || !file.exists()) {
                return new FTData();
            }

            analysisFileType(mPath);
            mSize = composerFileSize(mPath);
            if (mCt != null) {
                if (mCt.equals(Constants.CT_TYPE_VEDIO)) {
                    Log.d(TAG, "createTypeData new VideoData()");
                    return new VideoData();
                } else if (mCt.equals(Constants.CT_TYPE_AUDIO)) {
                    Log.d(TAG, "createTypeData new MusicData()");
                    return new MusicData();
                } else if (mCt.equals(Constants.CT_TYPE_IMAGE)) {
                    Log.d(TAG, "createTypeData new PictureData()");
                    return new PictureData();
                } else if (mCt.equals(Constants.CT_TYPE_VCARD)) {
                    Log.d(TAG, "createTypeData new VcardData()");
                    return new VcardData();
                } else if (mCt.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "createTypeData new GELOCATION");
                    return new GeolocationData();
                }
            }

        default:
            Log.e(TAG, "unknown type = " + mType2);
            return null;
        }
    }

    ISpamData getTypeData() {
        return mTypeData;
    }

    /**
     * @return contact image.
     */
    public Bitmap getImage() {
        return mImage;
    }

    /**
     * @return data.
     */
    public long getDate() {
        return mDate;
    }

    /**
     * @return get contact name.
     */
    public String getFrom() {
        return mFrom;
    }

    /**
     * @param address phone number.
     */
    public void setAddress(String address) {
        mAddress = address;
    }

    private void setDate(long date) {
        Log.d(TAG, "setDate = " + date);
        mDate = date;
    }

    /**
     * @param from name of the pone number.
     */
    public void setFrom(String from) {
        mFrom = from;
    }

    private void setPath(String path) {
        mPath = path;
    }

    /**
     * @return mMsgId This data msgid in the database.
     */
    public int getMsgId() {
        return mMsgId;
    }

    /**
     * @return This data phone number
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * @return return the ip message file size.
     */
    public String getSize() {
        return mSize;
    }

    /**
     * @return ip message file path.
     */
    public String getPath() {
        return mPath;
    }

    /**
     * @param bit contact avator.
     */
    public void setImage(Bitmap bit) {
        mImage = bit;
    }

    /**
     * Must Interface of all the data.
     * And each data is must one of them.
     */
    public interface ISpamData {

        /**
         * It used to get descripte data string.
         * The String will be only show in the spam ui.
         * @return the content string.
         */
        public abstract String getContent();
        /**
         * Init data content.
         * @param cursor used to query data from databse.
         * @return init result.
         */
        public abstract boolean initTypeData(Cursor cursor);
        /**
         * get content type, eg: music,picture, vedio, vcard, etc.
         * @return this item content type.
         */
        public abstract int getType();
    }

    private String composerFileSize(String path) {
        if (path == null) {
            Log.e(TAG, "file path == null");
            return null;
        } else {
            Log.e(TAG, "file path = " + path);
        }

        String retSize = null;
        File file = new File(path);
        if (file != null && file.exists()) {
            long size = file.length();
            Log.d(TAG, "pdu size = " + size);
            retSize = RcsMessageUtils.getDisplaySize(size, mContext);
            Log.d(TAG, "mSize = " + retSize);
        }
        return retSize;
    }

    /**
     * If a ip message is a mms, it will be wrap as a mmsData.
     */
    public class MmsData implements ISpamData {
        private int mType = Constants.Type.TYPE_MMS_PUSH;
        private String mSubject = mContext.getString(R.string.no_subject);
        //It only be used in spam activity, so notes multimedia_notification
        private String mTypeName = mContext.getString(R.string.multimedia_notification);

        @Override
        public int getType() {
            return mType;
        }

        private void setSubject(String subject) {
            Resources res = mContext.getResources();
            String str = res.getString(R.string.subject_label);
            str = str + subject;
            mSubject = str;
        }

        public String getSubject() {
            return mSubject;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            this.setSubject(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY)));
            return true;
        }
    }

    /**
     * If a ip message is a Geolocation, it will be wrap as a map data.
     * and it will be diaplay a map picture on the ui.
     */
    public class GeolocationData implements ISpamData {
        private int mType = Constants.MSG_TYPE_GELOCATION;
        private String mTypeName = mContext.getString(R.string.map_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * @return thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            if (mPath != null) {
                mBody = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ipmsg_geolocation);
            }
            return true;
        }
    }

    /**
     * If a ip message is a audio, it will be wrap as a music data and.
     * display music view in the ui.
     */
    public class MusicData implements ISpamData {
        private int mType = Constants.MSG_TYPE_MUSIC;
        private String mMusicName = mContext.getString(R.string.unknown);
        private String mTypeName = mContext.getString(R.string.music_type_name);

        @Override
        public int getType() {
            return mType;
        }

        public String getMusicName() {
            return mMusicName;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mMusicName = mPath.subSequence(mPath.lastIndexOf(File.separator) + 1,
                    mPath.length()).toString();
            return true;
        }
    }

    /**
     * If a ip message is a picture, it will be wrap as a picture data.
     * and it will be diaplay a thumb nail on the ui.
     */
    public class PictureData implements ISpamData {
        private int mType = Constants.MSG_TYPE_PICTURE;
        private String mTypeName = mContext.getString(R.string.pic_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * @return thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }

        private void setBody(String path) {
            File file = new File(path);
            if (file.exists()) {
                Log.d(TAG, "PictureData file exists");
            } else {
                Log.d(TAG, "PictureData file  no exists");
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            mBody = bitmap;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            if (mPath != null) {
                setBody(mPath);
            }
            return true;
        }
    }

    /**
     * If a ip message is a vemotion, it will be diaplay.
     * content on the ui.
     */
    public class VemoticonData implements ISpamData {
        private int mType = Constants.Type.TYPE_EMOTICON_MSG;
        private String mTypeName = mContext.getString(R.string.emoticons);
        private String mXmlBody = mContext.getString(R.string.emoticons);

        public VemoticonData() {
            Log.d(TAG, "VemoticonData");
        }

        @Override
        public int getType() {
            return mType;
        }

   //     public Bitmap getContent() {
   //         return mXmlBody;
   //     }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }


        private void setBody(String body) {
            Log.d(TAG, "setBody = " + body);
            mXmlBody = body;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mXmlBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            Log.d(TAG, "initTypeData mXmlBody = " + mXmlBody);
            EmojiImpl emojiImpl = EmojiImpl.getInstance(mContext);
            mXmlBody = EmojiShop.parseEmSmsString(mXmlBody);
            return true;

        }
    }

    /**
     * If a ip message is a text or sms, it will be diaplay.
     * content on the ui.
     */
    public class TextData implements ISpamData {
        private int mType = Constants.Type.TYPE_SMS;
        private String mBody;

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public String getContent() {
            return mBody;
        }

        private void setBody(String body) {
            mBody = body;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            return true;
        }
    }

    public class BurnTextData implements ISpamData {
        private int mType = Constants.Type.TYPE_IP_BURN_TEXT_MSG;
        private String mBody;
        private String mDeschription = mContext.getString(R.string.burned_msg);

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public String getContent() {
            return null;
        }

        private void setBody(String body) {
            mBody = body;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            return true;
        }
    }

    public class FTBurnData implements ISpamData {
        private int mType = Constants.Type.TYPE_IP_BURN_FT_MSG;
        private String mDeschription = mContext.getString(R.string.burned_msg);

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            return true;
        }

        @Override
        public String getContent() {
            return null;
        }
    }

   /**
     * If a ip message is a undownload ip ft message
     */
    public class FTData implements ISpamData {
        private int mType = Constants.Type.TYPE_IP_FT_MSG;
        private String mTypeName = mContext.getString(R.string.ip_type_name);

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            return true;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }
    }
   /* /**
     * If a ip message is a text or vcard, it will be wrap as
     * a vcard data, and dispaly a vcard icon and file name on
     * the ui.
     */
    public class VcardData implements ISpamData {
        private int mType = Constants.MSG_TYPE_VCARD;
        private String mTypeName = mContext.getString(R.string.vcard_type_name);
        private String mVcardName = mContext.getString(R.string.unknown);

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mVcardName = mPath.subSequence(mPath.lastIndexOf(File.separator) + 1,
                    mPath.length()).toString();
            return true;
        }

        /**
         * getView get the vcard name.
         * @return vcard file name.
         */
        public String getName() {
            return mVcardName;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }
    }

    /**
     * If a ip message is a text or video, it will be wrap as
     * a videoData. And display Thumbnail on the ui.
     */
    public class VideoData implements ISpamData {
        private int mType = Constants.MSG_TYPE_VIDEO;
        private String mTypeName = mContext.getString(R.string.video_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * getView get the video firt frame.
         * @return video firt frame thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }

        private Bitmap createVideoThumbnail(String filePath) {
            File file = new File(filePath);
            if (file.exists()) {
                Log.d(TAG, "VideoData file exists");
            } else {
                Log.d(TAG, "VideoData file  no exists");
            }

            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MINI_KIND);
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 500, 300);
            if (bitmap == null) {
                Log.d(TAG, "VideoData bitmap = null");
            } else {
                Log.d(TAG, "VideoData bitmap !=  null");
            }

            return bitmap;
        }

        @Override
        public String getContent() {
            return "( " + mTypeName + " )";
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = createVideoThumbnail(mPath);
            return true;
        }
    }

    /**
     * Constants of the favorite and spam part.
     */
    public static class Constants {
        // message type
        public static class Type {
            /**
             *
             */
            public static final int TYPE_SMS            = 0;

            /**
             *
             */
            public static final int TYPE_MMS_PUSH       = 1;

            /**
             *
             */
            public static final int TYPE_IP_TEXT_MSG    = 2;

            /**
             *
             */
            public static final int TYPE_IP_FT_MSG      = 3;

            /**
             *
             */
            public static final int TYPE_IP_BURN_TEXT_MSG = 4;
            /**
             *
             */
            public static final int TYPE_IP_BURN_FT_MSG   = 5;

            /**
             *
             */
            public static final int TYPE_EMOTICON_MSG     = 6;

            /**
             *
             */
            public static final int TYPE_CLOUD_MSG        = 7;
        }

        public static final int MSG_TYPE_MUSIC = 0x1004;
        public static final int MSG_TYPE_PUBLICACCOUNT = 0x1007;
        public static final int MSG_TYPE_VIDEO = 0x1005;
        public static final int MSG_TYPE_VCARD = 0x1006;
        public static final int MSG_TYPE_PICTURE = 0x1008;
        public static final int MSG_TYPE_GELOCATION = 0x1009;
        public static final int MSG_TYPE_UNKOWN = 0x1000;

        public static final String COLUMN_NAME_ID = SpamMsgData.COLUMN_ID;
        public static final String COLUMN_NAME_ADDRESS = SpamMsgData.COLUMN_ADDRESS;
        public static final String COLUMN_NAME_DATE = SpamMsgData.COLUMN_DATE;
        public static final String COLUMN_NAME_TYPE = SpamMsgData.COLUMN_TYPE;
        public static final String COLUMN_NAME_BODY = SpamMsgData.COLUMN_BODY;
        public static final String COLUMN_NAME_IPMSGID = SpamMsgData.COLUMN_IPMSG_ID;


        public static final String CT_TYPE_VEDIO = "video/mp4";
        public static final String CT_TYPE_AUDIO = "audio/mp3";
        public static final String CT_TYPE_IMAGE = "image/jpeg";
        public static final String CT_TYPE_VCARD = "text/x-vcard";
        public static final String CT_TYPE_GEOLOCATION = "xml/*";

        /** Image. */
        public static final String FILE_TYPE_IMAGE = "image";
        /** Audio. */
        public static final String FILE_TYPE_AUDIO = "audio";
        /** Video. */
        public static final String FILE_TYPE_VIDEO = "video";
        /** Text. */
        public static final String FILE_TYPE_TEXT = "text";
        /** Application. */
        public static final String FILE_TYPE_APP = "application";
    }
}
