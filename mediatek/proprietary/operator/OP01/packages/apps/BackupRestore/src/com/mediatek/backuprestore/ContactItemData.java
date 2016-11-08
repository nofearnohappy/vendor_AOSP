package com.mediatek.backuprestore;

import android.graphics.drawable.Drawable;
import com.mediatek.backuprestore.utils.Constants.ContactType;

public class ContactItemData {

    private int mSimId;
    private boolean mIsChecked;
    private String mContactName;
    // private int msimIconRes;
    private Drawable mIcon;
    private boolean mIsShow;

    public ContactItemData(int simId, boolean isChecked, String contactName, Drawable simIcon) {
        mSimId = simId;
        mIsChecked = isChecked;
        mContactName = contactName;
        mIcon = simIcon;
        // msimIconRes = simIconRes;
    }

    public ContactItemData(int simId, boolean isChecked, String contactName, Drawable simIcon,
            boolean show) {
        mSimId = simId;
        mIsChecked = isChecked;
        mContactName = contactName;
        // msimIconRes = simIconRes;
        mIcon = simIcon;
        mIsShow = show;
    }

    public int getSimId() {
        return mSimId;
    }

    public String getmContactName() {
        return mContactName;
    }

    public boolean getIsShow() {
        return mIsShow;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }
}
