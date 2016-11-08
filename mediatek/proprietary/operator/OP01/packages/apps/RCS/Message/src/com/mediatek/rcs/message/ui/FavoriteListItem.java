package com.mediatek.rcs.message.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cmcc.ccs.chat.ChatMessage;
import com.cmcc.ccs.chat.ChatService;
import com.mediatek.rcs.message.plugin.EmojiImpl;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.FavoriteDataItem.Constants;
import com.mediatek.rcs.message.ui.FavoriteDataItem.IFavoriteData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.MmsData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.MusicData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.PictureData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.TextData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.VcardData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.VemoticonData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.VideoData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.GeolocationData;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * Favorite list adaptor item view.
 * @author mtk81368
 */
public class FavoriteListItem extends RelativeLayout {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/FavoriteListItem";

    /**
     * Construction method.
     * @param context It used to be inflate layout.
     */
    public FavoriteListItem(Context context) {
        super(context);
        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.favorites_view_list_item, this);
        getBaseView();
    }

    private int mType = -1;
    private int mMsgId;
    protected Context mContext;

    private ImageView mAvatar;
    private TextView mFrom;
    private TextView mDate;
    public CheckBox mCheckbox;
    private View mTypeView;

    private void getBaseView() {
        mAvatar = (ImageView) findViewById(R.id.avatar);
        mFrom = (TextView) findViewById(R.id.from);
        mDate = (TextView) findViewById(R.id.date);
        mCheckbox = (CheckBox) findViewById(R.id.checkbox);
    }

    /**
     * Set data base info to adapter view.
     * @param item The data will be add to the view.
     */
    public void setBaseData(FavoriteDataItem item) {
        mAvatar.setImageBitmap(item.getImage());
        mFrom.setText(item.getFrom());
        String time = RcsMessageUtils.formatTimeStampStringExtend(mContext, item.getDate());
        mDate.setText(time);
        mMsgId = item.getMsgId();
    }

    /**
     * The method is call by adapter getView.
     * @param bShow False is means not show checkbox in adaptor.
     */
    public void showCheckBox(boolean bShow) {
        Log.i(TAG, "showCheckBox  = " + bShow);
        if (mCheckbox == null) {
            Log.e(TAG, "showCheckBox  = null ");
            return;
        }
        if (bShow) {
            Log.i(TAG, "setVisibility(VISIBLE)");
            mCheckbox.setVisibility(View.VISIBLE);
        } else {
            mCheckbox.setVisibility(View.GONE);
        }
    }

    /**
     * If the item is checked, the method will be called.
     * @param isCheck Set the check box status.
     */
    public void setChecked(boolean isCheck) {
        Log.i(TAG, "setChecked isCheck = " + isCheck);
        mCheckbox.setChecked(isCheck);
    }

    /**
     * Every item is relevent to a msgid, so can get the msg by msgid.
     * @return mMsgId The msgid of the item.
     */
    public int getMsgId() {
        return mMsgId;
    }

    /**
     * Every data has itself type and view,  when the adaptor getView must
     * decide which view will be load.
     * @param type Data type.
     */
    public void setType(int type) {
        mType = type;
        if (mTypeView != null) {
            mTypeView.setVisibility(View.GONE);
        }
        Log.d(TAG, "setType type = " + type);

        switch (type) {
        case (Constants.MSG_TYPE_VEMOTICON):
            Log.d(TAG, "setType Constants.TYPE_EMOTICON_MSG ");
            mTypeView = (View) this.findViewById(R.id.sms_info);
            break;

        case (ChatService.SMS):
            Log.d(TAG, "setType Constants.MSG_TYPE_SMS ");
            mTypeView = (View) this.findViewById(R.id.sms_info);
            break;

        case (ChatService.IM):
            Log.d(TAG, "setType Constants.MSG_TYPE_IPTEXT ");
            mTypeView = (View) this.findViewById(R.id.sms_info);
            break;

        case (Constants.MSG_TYPE_PUBLICACCOUNT):
            Log.d(TAG, "setType Constants.MSG_TYPE_IPTEXT ");
            mTypeView = (View) this.findViewById(R.id.sms_info);
            break;

        case (Constants.MSG_TYPE_PICTURE):
            mTypeView = (ImageView) this.findViewById(R.id.picture_info);
            break;

        case (ChatService.MMS):
            mTypeView = this.findViewById(R.id.has_icon_info);
            break;

        case (Constants.MSG_TYPE_MUSIC):
            Log.d(TAG, "setType Msg_Type_Music ");
            mTypeView = this.findViewById(R.id.has_icon_info);
            break;

        case (Constants.MSG_TYPE_VIDEO):
            mTypeView = this.findViewById(R.id.vedio_info);
            break;

        case (Constants.MSG_TYPE_VCARD):
            mTypeView = this.findViewById(R.id.has_icon_info);
            break;

        case (Constants.MSG_TYPE_GELOCATION):
            mTypeView = (View) this.findViewById(R.id.geolo_info);
            break;

        default:
            Log.e(TAG, "setType error! type = " + type);
            return;
        }

        if (mTypeView != null) {
            mTypeView.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "setType type view is null ");
        }
        return;
    }

    /**
     * Get the checkbox status.
     * @return isChecked Checkbox status.
     */
    public boolean isItemChecked() {
        return mCheckbox.isChecked();
    }

    /**
     * Add data to the view.
     * @param item The data to be add view.
     * @param typeItem Data type.
     */
    public void setTypeData(FavoriteDataItem item, IFavoriteData typeItem) {
        Log.i(TAG, "setData MmsData");
        if (typeItem instanceof MmsData) {
            ImageView icon = (ImageView) findViewById(R.id.icon);
            TextView subject = (TextView) findViewById(R.id.name_subject);
            TextView size = (TextView) findViewById(R.id.size);

            Bitmap bitmap = BitmapFactory
                    .decodeResource(mContext.getResources(), R.drawable.ic_mms);
            icon.setImageBitmap(bitmap);
            subject.setText(((MmsData) typeItem).getSubject());
            String msgSize = mContext.getString(R.string.message_size_label);
            size.setText(msgSize +item.getSize());
            return;
        }

        if (typeItem instanceof TextData) {
            TextView body = (TextView) findViewById(R.id.sms_info);
            EmojiImpl emojiImpl = EmojiImpl.getInstance(mContext);
            String bodyStr = ((TextData) typeItem).getContent();
            CharSequence bodyChars = null;
            if (bodyStr != null) {
                bodyChars = emojiImpl.getEmojiExpression(bodyStr, true);
            }
            body.setText(bodyChars);
            return;
        }

        if (typeItem instanceof VemoticonData) {
            Log.i(TAG, "setData VemoticonData");
            TextView body = (TextView) findViewById(R.id.sms_info);
            String bodyStr = ((VemoticonData) typeItem).getContent();
            body.setText(bodyStr);
            return;
        }

        if (typeItem instanceof MusicData) {
            Log.i(TAG, "setData MusicData");
            ImageView icon = (ImageView) findViewById(R.id.icon);
            TextView name = (TextView) findViewById(R.id.name_subject);
            TextView size = (TextView) findViewById(R.id.size);

            Bitmap bitmap = BitmapFactory
                    .decodeResource(mContext.getResources(), R.drawable.ic_mms_music);
            icon.setImageBitmap(bitmap);
            name.setText(((MusicData) typeItem).getMusicName());
            String msgSize = mContext.getString(R.string.message_size_label);
            size.setText(msgSize + item.getSize());
            return;
        }

        if (typeItem instanceof PictureData) {
            Log.i(TAG, "setData PictureData");
            ImageView content = (ImageView) findViewById(R.id.picture_info);
            content.setImageBitmap(((PictureData) typeItem).getContentImage());
            return;
        }

        if (typeItem instanceof VideoData) {
            Log.i(TAG, "setData VideoData");
            ImageView vedioContent = (ImageView) findViewById(R.id.vedio_content);
            vedioContent.setImageBitmap(((VideoData) typeItem).getContentImage());
            return;
        }

        if (typeItem instanceof VcardData) {
            ImageView icon = (ImageView) findViewById(R.id.icon);
            TextView name = (TextView) findViewById(R.id.name_subject);
            TextView size = (TextView) findViewById(R.id.size);

            Bitmap bitmap = BitmapFactory
                    .decodeResource(mContext.getResources(), R.drawable.ic_vcard);
            icon.setImageBitmap(bitmap);
            name.setText(((VcardData) typeItem).getName());
            String msgSize = mContext.getString(R.string.message_size_label);
            size.setText(msgSize + item.getSize());
            return;
        }

        if (typeItem instanceof VideoData) {
            Log.i(TAG, "setData VideoData");
            ImageView vedioContent = (ImageView) findViewById(R.id.vedio_content);
            vedioContent.setImageBitmap(((VideoData) typeItem).getContentImage());
            return;
        }

        if (typeItem instanceof GeolocationData) {
            Log.i(TAG, "setData GeolocationData");
            ImageView content = (ImageView) findViewById(R.id.geolo_icon);
            content.setImageBitmap(((GeolocationData) typeItem).getContentImage());
            TextView name = (TextView) findViewById(R.id.name_type);
            name.setText(((GeolocationData) typeItem).getTypeName());
            return;
        }
    }

    protected void updateView(Bitmap decodeString, String name) {
        mAvatar.setImageBitmap(decodeString);
        mFrom.setText(name);
    }

}
