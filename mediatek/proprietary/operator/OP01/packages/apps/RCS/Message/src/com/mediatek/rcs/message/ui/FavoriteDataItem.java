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

import com.cmcc.ccs.chat.ChatMessage;
import com.cmcc.ccs.chat.ChatService;
import com.mediatek.rcs.common.provider.FavoriteMsgData;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.data.ForwardSendData;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.ui.SpamDataItem.Constants;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.io.File;

/**
 * The favorite and spam data is wraped a FavoriteSpamItem type to
 * load by adaptor.
 */
public class FavoriteDataItem {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/FavoriteItem";

    protected Context mContext;
    private int mType;
    private String mTypeName;
    private IFavoriteData mTypeData;

    // common property
    protected Bitmap mImage;
    private long mDate;
    private String mFrom;
    private String mAddress;
    protected int mMsgId;
    private String mPath;
    private String mSize;
    private String mCt;

    /**
     * construct method.
     * @param context the activity that display data.
     * @param msgId message id.
     */
    public FavoriteDataItem(Context context, Cursor cursor) {
        mMsgId = cursor.getInt(cursor.getColumnIndex(FavoriteMsgData.COLUMN_ID));
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
        this.setFrom(cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_CONTACT)));
        this.setDate(cursor.getLong(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DATE)));
        this.setPath(cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_FILENAME)));
        mSize = composerFileSize(mPath);

        /**
         * one to one chat message, it is need get group name from db if a muti.
         * chat msg. so group msg need add other branch to do with.
         * if the msg is from me, the mAddress will be null.
         **/
        mAddress = cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_CONTACT));
        Log.e(TAG, "mAddress = " + mAddress);

        if (cursor.getInt(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_FLAG)) == ChatMessage.PUBLIC) {
            Log.d(TAG, "this is a public message");
                getPublicNameAndImage(mAddress);
                mTypeData = createPublicTypeData(cursor);
                if (mTypeData == null) {
                    Log.e(TAG, "initData fail");
                    return false;
                }
                mTypeData.initTypeData(cursor);
        } else {
            if (mAddress != null && portraitService != null) {
                Portrait protrait = portraitService.requestPortrait(mAddress);
                mImage = PortraitService.decodeString(protrait.mImage);
                mFrom = protrait.mName;
            }

            if (mAddress == null) {
                mFrom = mContext.getString(R.string.me);
                mAddress = mFrom;
            }
            mType = cursor.getInt(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_TYPE));
            mTypeData = createTypeData(mType);
            if (mTypeData == null) {
                Log.e(TAG, "initData fail");
                return false;
            }
            mTypeData.initTypeData(cursor);
        }
        return true;
    }

    private void getPublicNameAndImage(String address) {
        Cursor paCs = null;
        paCs = mContext.getContentResolver().query(Constants.PUBLIC_URI,
                new String[] { "name", "logo_path" }, Constants.UUID + "=?",
                new String[] { address }, null);
        if (paCs != null && paCs.getCount() > 0) {
            paCs.moveToFirst();
            mFrom = paCs.getString(paCs.getColumnIndexOrThrow("name"));
            String logoPath = paCs.getString(paCs.getColumnIndexOrThrow("logo_path"));
            Log.d(TAG, "public account logoPath = " + logoPath);
            if (logoPath != null) {
                try {
                    mImage = BitmapFactory.decodeFile(logoPath);
                } catch (java.lang.OutOfMemoryError e) {
                    Log.d(TAG, "java.lang.OutOfMemoryError");
                }
            }
        }

        if (paCs != null) {
            paCs.close();
        }
    }

    private IFavoriteData createPublicTypeData(Cursor cursor) {
        Log.d(TAG, "createPublicTypeData ");
        String subject = cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_BODY));
        if (mPath == null) {
            Log.d(TAG, "public account text");
            return new TextData(Constants.MSG_TYPE_PUBLICACCOUNT);
        }

        if (mPath != null && subject != null) {
            Log.d(TAG, "public account artical");
            return new TextData(Constants.MSG_TYPE_PUBLICACCOUNT);
        }

        analysisFileType(mPath);
        if (mCt != null) {
            if (mCt.equals(Constants.CT_TYPE_VEDIO)) {
                Log.d(TAG, "createPublicTypeData createTypeData new VideoData()");
                return new VideoData();
            } else if (mCt.equals(Constants.CT_TYPE_AUDIO)) {
                Log.d(TAG, "createPublicTypeData createTypeData new MusicData()");
                return new MusicData();
            } else if (mCt.equals(Constants.CT_TYPE_IMAGE)) {
                Log.d(TAG, "createPublicTypeData createTypeData new PictureData()");
                return new PictureData();
            } else if (mCt.equals(Constants.CT_TYPE_VCARD)) {
                Log.d(TAG, "createPublicTypeData createTypeData new VcardData()");
                return new VcardData();
            } else if (mCt.equals(Constants.CT_TYPE_GEOLOCATION)) {
                Log.d(TAG, "createPublicTypeData createTypeData new GeolocationData()");
                return new GeolocationData();
            }
        }

        Log.e(TAG, "createPublicTypeData error, return null");
        return null;
    }

    private IFavoriteData createTypeData(int type) {
        Log.d(TAG, "createTypeData mType2 = " + type);
        switch (type) {
        case Constants.MSG_TYPE_VEMOTICON:
            return new VemoticonData();

        case ChatService.SMS:
            return new TextData(ChatService.SMS);

        case ChatService.MMS:
            return new MmsData();

        case ChatService.IM:
            return new TextData(ChatService.IM);

        case ChatService.FT:
            analysisFileType(mPath);
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
                    Log.d(TAG, "createTypeData new GeolocationData()");
                    return new GeolocationData();
                }
            }

        default:
            Log.e(TAG, "unknown type = " + type);
            return null;
        }
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

    /**
     *
     * @return type info
     */
   public IFavoriteData getTypeData() {
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
        Log.d(TAG, "path = " + path);
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
     * @author mtk81368
     */
    public interface IFavoriteData {
        /**
         * It used to get descripte data string.
         * The String will be only show in the spam ui.
         * @return the content string.
         */
        public abstract String getTypeName();
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
            Log.d(TAG, "file size = " + size);
            retSize = RcsMessageUtils.getDisplaySize(size, mContext);
            Log.d(TAG, "mSize = " + retSize);
        }
        return retSize;
    }

    /**
     * If a ip message is a mms, it will be wrap as a mmsData.
     * @author mtk81368
     */
    public class MmsData implements IFavoriteData {
        private int mType = ChatService.MMS;
        private String mSubject = mContext.getString(R.string.no_subject);
        private String mTypeName = mContext.getString(R.string.multimedia_message);

        @Override
        public int getType() {
            return mType;
        }

        private void setSubject(String subject) {
            Resources res = mContext.getResources();
            String str = res.getString(R.string.subject_label);
            if (subject == null) {
                str = mSubject;
            } else {
                str = str + subject;
            }
            mSubject = str;
        }

        public String getSubject() {
            return mSubject;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            this.setSubject(cursor.getString(
                                cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_BODY)));
            return true;
        }

    }

    /**
     * If a ip message is a audio, it will be wrap as a music data and.
     * display music view in the ui.
     * @author mtk81368
     */
    public class MusicData implements IFavoriteData {
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
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mMusicName = mPath.subSequence(mPath.lastIndexOf(File.separator) + 1,
                    mPath.length()).toString();
            return true;
        }

    }

    /**
     * If a ip message is a Geolocation, it will be wrap as a map data.
     * and it will be diaplay a map picture on the ui.
     * @author mtk81368
     */
    public class GeolocationData implements IFavoriteData {
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
        public String getTypeName() {
            return mTypeName;
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
     * If a ip message is a picture, it will be wrap as a picture data.
     * and it will be diaplay a thumb nail on the ui.
     * @author mtk81368
     */
    public class PictureData implements IFavoriteData {
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
            if (file != null && file.exists()) {
                Log.d(TAG, "PictureData file exists");
            } else {
                Log.d(TAG, "PictureData file  no exists, show default picture");
                mBody  = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_photo);
                return;
            }
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeFile(path);
            } catch (java.lang.OutOfMemoryError e) {
                Log.d(TAG, "java.lang.OutOfMemoryError");
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_photo);
            }
            mBody = bitmap;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
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
     * If a ip message is a text or sms, it will be diaplay.
     * content on the ui.
     * @author mtk81368
     */
    public class TextData implements IFavoriteData {
        private int mType;
        private String mTypeName = mContext.getString(R.string.imtext_type_name);
        private String mBody;

        public TextData(int type) {
            Log.d(TAG, "TextData type = " + type);
            mType = type;
            if (mType == ChatService.SMS) {
                mTypeName = mContext.getString(R.string.text_message);
            } else if (mType == Constants.MSG_TYPE_PUBLICACCOUNT) {
                mTypeName = mContext.getString(R.string.public_accounts_msg);
            }
        }

        @Override
        public int getType() {
            return mType;
        }

        public String getContent() {
            return mBody;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        private void setBody(String body) {
            Log.d(TAG, "setBody = " + body);
            mBody = body;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_BODY));
            Log.d(TAG, "initTypeData mBody = " + mBody);
            return true;
        }
    }

    /**
     * If a ip message is a vemotion, it will be diaplay.
     * content on the ui.
     */
    public class VemoticonData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_VEMOTICON;
        private String mTypeName = mContext.getString(R.string.emoticons);
        private String mXmlBody;

        public VemoticonData() {
            Log.d(TAG, "VemoticonData");
        }

        @Override
        public int getType() {
            return mType;
        }

       public String getContent() {
            return mXmlBody;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        private void setBody(String body) {
            Log.d(TAG, "setBody = " + body);
            mXmlBody = body;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mXmlBody = cursor.getString(cursor.getColumnIndex(FavoriteMsgData.COLUMN_DA_BODY));
            Log.d(TAG, "initTypeData mXmlBody = " + mXmlBody);
            mXmlBody = EmojiShop.parseEmSmsString(mXmlBody);
            return true;
        }
    }

    /**
     * If a ip message is a text or vcard, it will be wrap as
     * a vcard data, and dispaly a vcard icon and file name on
     * the ui.
     * @author mtk81368
     */
    public class VcardData implements IFavoriteData {
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
        public String getTypeName() {
            return mTypeName;
        }
    }

    /**
     * If a ip message is a text or video, it will be wrap as
     * a videoData. And display Thumbnail on the ui.
     * @author mtk81368
     */
    public class VideoData implements IFavoriteData {
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
            if (file != null && file.exists()) {
                Log.d(TAG, "VideoData file exists");
            } else {
                Log.d(TAG, "VideoData file  no exists,show default ve");
                return BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_video);
            }

            Bitmap bitmap = null;
            try {
                bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MINI_KIND);
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 500, 300);
            } catch (java.lang.OutOfMemoryError e) {
                Log.d(TAG, "java.lang.OutOfMemoryError");
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_video);
            }
            if (bitmap == null) {
                Log.d(TAG, "VideoData bitmap = null");
            } else {
                Log.d(TAG, "VideoData bitmap !=  null");
            }

            return bitmap;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = createVideoThumbnail(mPath);
            return true;
        }

    }

    /**
     * Constants of the favorite and spam part.
     * @author mtk81368
     */
    public static class Constants {
        public static final int MSG_TYPE_MUSIC = 0x1004;
        public static final int MSG_TYPE_PUBLICACCOUNT = 0x1006;
        public static final int MSG_TYPE_VIDEO = 0x1005;
        public static final int MSG_TYPE_VCARD = 0x1007;
        public static final int MSG_TYPE_PICTURE = 0x1008;
        public static final int MSG_TYPE_GELOCATION = 0x1009;
        public static final int MSG_TYPE_UNKOWN = 0x100;
        public static final int MSG_TYPE_VEMOTICON = 0x6;

        public static final String CT_TYPE_VEDIO = "video/mp4";
        public static final String CT_TYPE_AUDIO = "audio/mp3";
        public static final String CT_TYPE_IMAGE = "image/jpeg";
        public static final String CT_TYPE_VCARD = "text/x-vcard";
        public static final String CT_TYPE_GEOLOCATION = "xml/*";
        public static final String CT_TYPE_IPTEXT = "text/plain";

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
        /** forward sms flag when ue havent config rcs situation */
        public static final int FORWARD_TYPE_SMS = 1;
        /** forward mms flag when ue havent config rcs situation */
        public static final int FORWARD_TYPE_MMS = 2;

        public static final Uri PUBLIC_URI = Uri.parse("content://com.mediatek.publicaccounts/accounts");
        public static final String KEY_WEB_LINK = "key_web_link";
        public static final String KEY_FORWARDABLE = "key_forwardable";
        public static final String UUID = "uuid";
    }
}
