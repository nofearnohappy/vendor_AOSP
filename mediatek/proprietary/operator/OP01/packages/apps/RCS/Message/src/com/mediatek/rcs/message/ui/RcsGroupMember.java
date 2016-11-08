package com.mediatek.rcs.message.ui;

import android.app.Activity;
//import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
//import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
//import android.widget.GridView;
//import android.widget.ImageView.ScaleType;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
//import com.mediatek.rcs.message.ui.PortraitService.Portrait;
//import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.message.R;

//import java.util.Arrays;
import java.util.ArrayList;
//import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RcsGroupMember extends Preference {
    private LinearLayout mAccountContainer = null;
    private Context mContext = null;
    private static final String TAG = "Rcs/RcsGroupMemeber";
    private RcsGroupManagementGridView mGridView;
    public static final int REQUEST_CODE_ADD_CONTACT  = 1;
    public static final int REQUEST_CODE_VIEW_CONTACT = 2;
    public static final int REQUEST_CODE_ADD_TO_CONTACT = 3;
    public static final int REQUEST_CODE_REMOVE_CONTACT = 4;

    public static final String ACTION_CONTACT_SELECTION
        = "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String PICK_CONTACT_NUMBER_BALANCE = "NUMBER_BALANCE";
    private String mGroupChatId;
    private Handler mUiHandler;
    //private boolean mIsMeChairmen;

    private String mMyNumber;
    boolean mRemoveState = false;
    PortraitService mPortraitService;
    PortraitUpdateListener mPortraitUpdateListener;
    public RcsGroupMember(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.group_management_fragment);
        mPortraitService = PortraitService.getInstance(
                ((Activity) mContext).getApplicationContext(),
                R.drawable.ic_contact_picture,
                R.drawable.contact_blank_avatar);
        mPortraitUpdateListener = new PortraitUpdateListener();
        mPortraitService.addListener(mPortraitUpdateListener);
        mGroupChatId = ((RcsGroupManagementSetting)context).getChatId();
        //mIsMeChairmen = ((RcsGroupManagementSetting)context).isMeChairmen();
        //mChairmen = ((RcsGroupManagementSetting)context).getChairmen();
        mUiHandler = ((RcsGroupManagementSetting)context).getUiHandler();
        mMyNumber = RCSServiceManager.getInstance().getMyNumber();
        Log.d(TAG, "RcsGroupMember() mGroupChatId:" + mGroupChatId);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        mGridView = (RcsGroupManagementGridView)view.findViewById(R.id.myGrid);
        mGridView.setExpanded(true);
        Set<String> numberSet = mPortraitService.updateGroup(mGroupChatId, false);
        ArrayList<GroupMember> data = new ArrayList<GroupMember>();
        for (String number : numberSet) {
            data.add(new GroupMember(number));
        }

        WindowManager windowM = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display defDisplay = windowM.getDefaultDisplay();
        int w = defDisplay.getWidth();
        int h = defDisplay.getHeight();
        Log.d(TAG, "w:" + w + ",h:" + h);
        if (w > h) {
            mGridView.setNumColumns(w / (h / 4 + h % 4));
        }
        //  else {
        //     mGridView.setNumColumns(4);
        // }

        mGridView.setAdapter(new GroupMemberAdapter(mContext, data));
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d(TAG, "OnItemClickListener()" + "arg2:" + arg2 + ",arg3:" + arg3);
                GroupMemberAdapter adapter = (GroupMemberAdapter) mGridView.getAdapter();
                if (mRemoveState) {
                    if (arg2 >= adapter.getSize()) {
                        mRemoveState = !mRemoveState;
                        adapter.notifyDataSetChanged();
                    } else {
                        //if (arg2 != getChairManPosition()) {
                        String chairMen = ((RcsGroupManagementSetting)mContext).getChairmen();
                        String number = ((GroupMember)adapter.getItem(arg2)).getNumber();
                        if (!chairMen.contains(number) && !isParticipantInviting(number)) {
                            //cannot remove chairMen and inviting member
                            //adapter.remove(arg2);
                            Message msg = mUiHandler.obtainMessage(REQUEST_CODE_REMOVE_CONTACT,
                                                                   number);
                            mUiHandler.sendMessage(msg);
                        }
                    }
                } else {
                    // if (arg2 == adapter.getAddPosition()) {
                    //     try {
                    //         Intent intent = new Intent(ACTION_CONTACT_SELECTION);
                    //         int pickCount = 10;
                    //         intent.setType(Phone.CONTENT_TYPE);

                    //         int existSize = adapter.getSize();
                    //         String[] existNumbers = new String[existSize];
                    //         for (int i = 0; i < existSize; i++) {
                    //             existNumbers[i] = ((GroupMember)adapter.getItem(i)).getNumber();
                    //         }
                    //         intent.putExtra("ExistNumberArray", existNumbers);
                    //         intent.putExtra("Group", true);

                    //         intent.putExtra(PICK_CONTACT_NUMBER_BALANCE, pickCount);
                    //         ((Activity)mContext).startActivityForResult(intent, REQUEST_CODE_ADD_CONTACT);
                    //     } catch (ActivityNotFoundException e) {
                    //         Log.e(TAG, e.getMessage());
                    //         Toast.makeText(mContext,
                    //                        mContext.getString(R.string.no_application_response),
                    //                        Toast.LENGTH_SHORT).show();
                    //     }
                    // } else if (arg2 == adapter.getRemovePosition()) {
                    //     mRemoveState = true;
                    //     adapter.notifyDataSetChanged();
                    // } else
                    if (arg2 < adapter.getSize()) {
                        try {
                            String numberClicked = ((GroupMember)adapter.getItem(arg2)).getNumber();
                            Uri uri = getContactUriForPhoneNumbers(numberClicked);
                            if (PhoneNumberUtils.compare(mMyNumber, numberClicked)) {
                                Intent i = new Intent("android.intent.action.view.profile");
                                ((Activity)mContext).startActivity(i);
                            } else if (uri != null) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                ((Activity)mContext).startActivityForResult(intent,
                                    REQUEST_CODE_VIEW_CONTACT);
                            } else {
                                mPortraitService.requestMemberPortrait(
                                    mGroupChatId, numberClicked, true);
                                //Intent.ACTION_INSERT_OR_EDIT);
                                /*
                                Intent intent = new Intent(Intent.ACTION_INSERT,
                                                           ContactsContract.Contacts.CONTENT_URI);
                                intent.setType(Contacts.CONTENT_ITEM_TYPE);
                                intent.putExtra(ContactsContract.Intents.Insert.PHONE, numberClicked);
                                intent.putExtra(ContactsContract.Intents.Insert.NAME,
                                                ((GroupMember)adapter.getItem(arg2)).getName());
                                intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                */
                                Uri createUri = Uri.fromParts("tel", numberClicked, null);
                                final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT,
                                    createUri);
                                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ((Activity)mContext).startActivityForResult(intent,
                                    REQUEST_CODE_ADD_TO_CONTACT);
                            }
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, e.getMessage());
                            Toast.makeText(mContext,
                                           mContext.getString(R.string.no_application_response),
                                           Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d(TAG, "onItemLongClick()" + "arg2:" + arg2 + ",arg3:" + arg3);
                if (((RcsGroupManagementSetting)mContext).isMeChairmen()) {
                    GroupMemberAdapter adapter = (GroupMemberAdapter) mGridView.getAdapter();
                    int size = adapter.getSize();
                    if (arg2 < size && adapter.getShowRemove()) {
                        mRemoveState = true;
                        adapter.notifyDataSetChanged();
                    }
                    //  else if (arg2 < adapter.getCount()) {
                    //     if (mRemoveState) {
                    //         mRemoveState = false;
                    //     }
                    // }


                    //adapter.notifyDataSetChanged();
                }
                return true;
            }
        });

        mGridView.setOnNoItemClickListener(new RcsGroupManagementGridView.OnNoItemClickListener() {
            @Override
            public void onNoItemClick() {
                Log.d(TAG, "onNoItemClick() mRemoveState:" + mRemoveState);
                if (mRemoveState) {
                    mRemoveState = false;
                    GroupMemberAdapter adapter = (GroupMemberAdapter) mGridView.getAdapter();
                    adapter.notifyDataSetChanged();
                }
            }
        });


    }

    // ==================================================
    private int mTouchedPosition;
    public class GroupMemberAdapter extends BaseAdapter {
        private int mDataSize;
        private ArrayList<GroupMember> mData;
        private boolean mShowRemoveIcon;

        private void initilizeAddRemove() {
            mData.add(new GroupMember(""));
            if (((RcsGroupManagementSetting)mContext).isMeChairmen()) {
                Log.d(TAG, "initilizeAddRemove(), add remove member icon");
                mData.add(new GroupMember(""));
            }
            mShowRemoveIcon = canRemoveParticipant();
        }

        public void updateData(ArrayList<GroupMember> data) {
            mData = data;
            mDataSize = data.size();
            initilizeAddRemove();
            notifyDataSetChanged();
        }

        public void updateDataItemName(int i, String name) {
            if (i < getSize()) {
                ((GroupMember)getItem(i)).setName(name);
                notifyDataSetChanged();
            }
        }

        public int getAddPosition() {
            return mDataSize;
        }

        public int getRemovePosition() {
            return ((RcsGroupManagementSetting)mContext).isMeChairmen() ? (mDataSize + 1) : -1;
        }

        public void remove(int position) {
            if (position >= 0 && position < mDataSize) {
                mData.remove(position);
                mDataSize--;
                mShowRemoveIcon = canRemoveParticipant();
                this.notifyDataSetChanged();
            }
        }

        public void remove(String number) {
            Log.d(TAG, "remove() number:" + number);
            for (int i = 0; i < mData.size(); i++) {
                if (mData.get(i).getNumber().equals(number)) {
                    mData.remove(i);
                    mDataSize--;
                    mShowRemoveIcon = canRemoveParticipant();
                    this.notifyDataSetChanged();
                    break;
                }
            }
        }

        public void add(String number) {
            this.add(number, null);
        }

        public void add(String number, String name) {
            for (int i = 0; i < mData.size(); i++) {
                if (mData.get(i).getNumber().equals(number)) {
                    this.notifyDataSetChanged(); // need update name from inviting to real name
                    return; // donot add again
                }
            }
            mData.add(mDataSize, new GroupMember(number, name));
            mDataSize++;
            mShowRemoveIcon = canRemoveParticipant();
            this.notifyDataSetChanged();
        }

        public int getSize() {
            return mDataSize;
        }

        public boolean getShowRemove() {
            return mShowRemoveIcon;
        }

        public GroupMemberAdapter(Context context, ArrayList<GroupMember> data) {
            Log.d(TAG, "GroupMemberAdapter() data.size():" + data.size());
            mContext = context;
            mData = data;
            mDataSize = data.size();
            initilizeAddRemove();
        }

        private boolean canRemoveParticipant() {
            if (!((RcsGroupManagementSetting)mContext).isMeChairmen()) {
                return false;
            }
            int size = getSize();
            int invitingCount = 0;
            for (int i = 0; i < size; i++) {
                GroupMember member = (GroupMember)getItem(i);
                if (isParticipantInviting(member.getNumber())) {
                    invitingCount++;
                }
            }
            Log.d(TAG, "canRemoveParticipant,size:" + size + ",invitingCount:" + invitingCount);
            // except inviting member, need include chairman, so need +1
            if (size > (invitingCount + 1)) {
                return true;
            } else {
                return false;
            }
        }

        public int getCount() {
            int count;
            if (((RcsGroupManagementSetting)mContext).isMeChairmen() && !mShowRemoveIcon) {
                // if me is chairmen and only myself in group
                // or if me is chairmen and all others are inviting
                // don't show remove icon
                Log.d(TAG, "getCount() hide the remove icon");
                count = mData.size() - 1;
            } else {
                count = mData.size();
            }
            Log.d(TAG, "getCount():" + count);
            return count;
        }

        public Object getItem(int position) {
            return mData.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                                              Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.group_management_grid_item, null);
            }

            ImageView imageView = (ImageView) convertView.findViewById(R.id.grid_item_image);
            TextView textView = (TextView) convertView.findViewById(R.id.grid_item_label);
            View deleteImg = convertView.findViewById(R.id.deleteLayout);
            View chairmanImg = convertView.findViewById(R.id.chairmanLayout);

            GroupMember item = mData.get(position);
            String itemNumber = item.getNumber();
            String chairMen = ((RcsGroupManagementSetting)mContext).getChairmen();
            boolean isInviting = false;
            //imageView.setAdjustViewBounds(true);
            //imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            //imageView.setImageResource(item.getImageId());
            //imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
            //imageView.setLayoutParams(android.view.ViewGroup$LayoutParams(200, 200))
            //(new GridView.LayoutParams(200, 200));

            if (position < getSize()) {
                Portrait portrait = mPortraitService.getMemberPortrait(mGroupChatId, itemNumber);
                isInviting = isParticipantInviting(itemNumber);
                Log.d(TAG, "getView() chairMen:" + chairMen + ",itemNumber:" + itemNumber
                + ",isInviting:" + isInviting + ",position:" + position + ",size:" + getSize());

                imageView.setImageBitmap(PortraitService.decodeString(portrait.mImage));
                if (itemNumber.equals(mMyNumber)) {
                    String myNickName = ((RcsGroupManagementSetting)mContext).getMyNickName();
                    if ((myNickName != null) && !(myNickName.equals(""))) {
                        item.setName(myNickName);
                    } else {
                        item.setName(portrait.mName);
                    }
                } else if (isInviting) {
                    item.setName(mContext.getString(R.string.inviting));
                } else {
                    item.setName(portrait.mName);
                }
                textView.setText(item.getName());

            } else if (position == getSize()) {
                imageView.setImageResource(R.drawable.ic_rcs_group_add_member);
            } else {
                imageView.setImageResource(R.drawable.ic_rcs_group_remove_member);
            }

            //int dataSize = getCount();
            //if (position < dataSize) {
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(position < getSize() ? View.VISIBLE : View.INVISIBLE);

            if (mRemoveState) {
                if (position == getAddPosition() ||
                    position == getRemovePosition()) {
                    //imageView.setVisibility(View.INVISIBLE);
                    //textView.setVisibility(View.INVISIBLE);
                    //deleteImg.setVisibility(View.INVISIBLE);
                    imageView.setVisibility(View.GONE);
                    textView.setVisibility(View.GONE);
                    deleteImg.setVisibility(View.GONE);
                    //} else if (position == getChairManPosition()) {
                } else if (chairMen.contains(itemNumber)) {
                    deleteImg.setVisibility(View.GONE);
                } else if (isInviting) {
                    deleteImg.setVisibility(View.GONE);
                } else {
                    deleteImg.setVisibility(View.VISIBLE);
                }
            } else {
                deleteImg.setVisibility(View.INVISIBLE);
                //convertView.setVisibility(View.VISIBLE);
            }

            if (chairMen.contains(itemNumber) && position < getSize()) {
            //if (position == getChairManPosition()) {
                chairmanImg.setVisibility(View.VISIBLE);
            } else {
                chairmanImg.setVisibility(View.INVISIBLE);
            }

            if (position == getAddPosition()) {
                //imageView.setLongClickable(false);
                imageView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View view, MotionEvent event) {
                        // float currentXPosition = event.getX();
                        // float currentYPosition = event.getY();
                        // int position = mGridView.pointToPosition((int)currentXPosition, (int)currentYPosition);
                        // Log.d(TAG, "onTouch() X:" + currentXPosition + ",Y:" +
                        // currentYPosition + ",position:" + position + ",Action:" + event.getAction());
                        Log.d(TAG, "onTouch() Action:" + event.getAction());
                        ImageView imageView = (ImageView) view;
                        if (event.getAction() == MotionEvent.ACTION_DOWN
                            /* || event.getAction() == MotionEvent.ACTION_POINTER_DOWN */) {
                            imageView.setImageResource(R.drawable.ic_rcs_group_add_member_touched);
                        } else if (event.getAction() == MotionEvent.ACTION_UP ||
                                   event.getAction() == MotionEvent.ACTION_CANCEL ||
                                   event.getAction() == MotionEvent.ACTION_POINTER_UP) {
                            imageView.setImageResource(R.drawable.ic_rcs_group_add_member);
                            //imageView.performClick();
                            try {
                                Intent intent = new Intent(ACTION_CONTACT_SELECTION);
                                int pickCount = 10;
                                intent.setType(Contacts.CONTENT_TYPE);
                                int existSize = getSize();
                                String[] existNumbers = new String[existSize];
                                for (int i = 0; i < existSize; i++) {
                                    existNumbers[i] = ((GroupMember)getItem(i)).getNumber();
                                }
                                intent.putExtra("ExistNumberArray", existNumbers);
                                intent.putExtra("Group", true);
                                intent.putExtra(PICK_CONTACT_NUMBER_BALANCE, pickCount);
                                ((Activity)mContext).startActivityForResult(intent,
                                    REQUEST_CODE_ADD_CONTACT);
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, e.getMessage());
                                Toast.makeText(mContext,
                                               mContext.getString(R.string.no_application_response),
                                               Toast.LENGTH_SHORT).show();
                            }
                        }

                        return true;
                    }
                });
            } else if (position == getRemovePosition()) {
                //imageView.setLongClickable(false);
                imageView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View view, MotionEvent event) {
                        // float currentXPosition = event.getX();
                        // float currentYPosition = event.getY();
                        // int position = mGridView.pointToPosition((int)currentXPosition, (int)currentYPosition);
                        // Log.d(TAG, "onTouch() X:" + currentXPosition +
                        // ",Y:" + currentYPosition + ",position:" + position + ",Action:" + event.getAction());
                        Log.d(TAG, "onTouch() Action:" + event.getAction());
                        if (((RcsGroupManagementSetting)mContext).isMeChairmen()) {
                            ImageView imageView = (ImageView) view;
                            if (event.getAction() == MotionEvent.ACTION_DOWN
                                /* || event.getAction() == MotionEvent.ACTION_POINTER_DOWN */) {
                                imageView.setImageResource(R.drawable.ic_rcs_group_remove_member_touched);
                            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                                       event.getAction() == MotionEvent.ACTION_CANCEL ||
                                       event.getAction() == MotionEvent.ACTION_POINTER_UP) {
                                imageView.setImageResource(R.drawable.ic_rcs_group_remove_member);
                                //imageView.performClick();
                                mRemoveState = true;
                                notifyDataSetChanged();
                            }
                        }

                        return true;
                    }
                });
            } else {
                imageView.setOnTouchListener(null);
            }
            //}

            return convertView;
        }
    }

    public class GroupMember {
        private String mName;
        private String mPhoneNumber;

        GroupMember(String number) {
            mPhoneNumber = number;
        }

        GroupMember(String number, String name) {
            mPhoneNumber = number;
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getNumber() {
            return mPhoneNumber;
        }
    }

    // ==================================================
    public void onAddDone(String contact) {
        Log.d(TAG, "onAddDone() contact:" + contact);
        if (mGridView == null) {
            Log.d(TAG, "onAddDone() mGridView is null");
            return;
        }
        GroupMemberAdapter adapter = (GroupMemberAdapter) mGridView.getAdapter();
        if (adapter == null) {
            Log.d(TAG, "onAddDone() adapter is null");
            return;
        }
        if (contact != null) {
            adapter.add(contact);
        } else {
            mPortraitService.updateGroup(mGroupChatId, false);
            //adapter.notifyDataSetChanged();
        }
    }

    public void onRemoveDone(String contact) {
        Log.d(TAG, "onRemoveDone()contact:" + contact);
        if (mGridView == null) {
            Log.d(TAG, "onRemoveDone()mGridView is null");
            return;
        }

        GroupMemberAdapter adapter = (GroupMemberAdapter) mGridView.getAdapter();
        if (contact != null && adapter != null) {
            adapter.remove(contact);
            if (adapter.getSize() <= 1) {
                if (mRemoveState) {
                    mRemoveState = false;
                    adapter.notifyDataSetChanged();
                }
            }
        } else {
            mPortraitService.updateGroup(mGroupChatId, false);
            //adapter.notifyDataSetChanged();
        }
    }

    public void onChairmenTransferred(String newChairmen, boolean isMeChairmen) {
        if (mGridView == null) {
            Log.d(TAG, "onChairmenTransferred()mGridView is null");
            return;
        }

        GroupMemberAdapter adapter = (GroupMemberAdapter)mGridView.getAdapter();
        int size = adapter.getSize();
        ArrayList<GroupMember> data = new ArrayList<GroupMember>(size);
        for (int i = 0; i < size; i++) {
            data.add((GroupMember)adapter.getItem(i));
        }

        adapter.updateData(data);
    }

    public void onSelfNickNameModified(String nickName) {
        if (mGridView == null) {
            Log.d(TAG, "onSelfNickNameModified()mGridView is null");
            return;
        }

        GroupMemberAdapter adapter = (GroupMemberAdapter)mGridView.getAdapter();
        int size = adapter.getSize();
        for (int i = 0; i < size; i++) {
            String number = ((GroupMember)adapter.getItem(i)).getNumber();
            if (number.equals(mMyNumber)) {
                adapter.updateDataItemName(i, nickName);
                break;
            }
        }
    }

    public ArrayList<GroupMember> getGroupMember() {
        ArrayList<GroupMember> data = new ArrayList<GroupMember>();
        if (mGridView != null) {
            GroupMemberAdapter adapter = (GroupMemberAdapter)mGridView.getAdapter();
            int size = adapter.getSize();
            for (int i = 0; i < size; i++) {
                if (!isParticipantInviting(((GroupMember)adapter.getItem(i)).getNumber())) {
                    data.add((GroupMember)adapter.getItem(i));
                }
            }
        } else {
            Log.d(TAG, "getGroupMember()mGridView is null");
        }

        return data;
    }

    public void onResume() {
        if (mPortraitService != null) {
            mPortraitService.updateGroup(mGroupChatId, false);
        }
    }

    public void onDestroy() {
        if (mPortraitService != null && mPortraitUpdateListener != null) {
            mPortraitService.removeListener(mPortraitUpdateListener);
        }
    }

    // ==================================================
    private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;
    private static final String[] CALLER_ID_PROJECTION = new String[] {
        Phone._ID,                      // 0
        Phone.NUMBER,                   // 1
        Phone.LABEL,                    // 2
        Phone.DISPLAY_NAME,             // 3
        Phone.CONTACT_ID,               // 4
        Phone.CONTACT_PRESENCE,         // 5
        Phone.CONTACT_STATUS,           // 6
        //Phone.NORMALIZED_NUMBER,        // 7
        Contacts.SEND_TO_VOICEMAIL      // 8
    };

    private static final int PHONE_ID_COLUMN = 0;
    private static final int PHONE_NUMBER_COLUMN = 1;
    private static final int PHONE_LABEL_COLUMN = 2;
    private static final int CONTACT_NAME_COLUMN = 3;
    private static final int CONTACT_ID_COLUMN = 4;
    private static final int CONTACT_PRESENCE_COLUMN = 5;
    private static final int CONTACT_STATUS_COLUMN = 6;
    private static final int PHONE_NORMALIZED_NUMBER = 7;
    private static final int SEND_TO_VOICEMAIL = 8;


    // ==================================================
    // ==================================================
    private Uri getContactUriForPhoneNumbers(String number) {
        Uri uri = null;
        if (number != null) {
            Uri tmpuri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = mContext.getContentResolver()
                .query(tmpuri, new String[]{Phone._ID}, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                            cursor.getString(PHONE_ID_COLUMN));
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            } else {
                uri = null;
            }
        }

        return uri;
    }

    private boolean isParticipantInviting(String number) {
        Participant parti;
        List<Participant> participants = ((RcsGroupManagementSetting)mContext).getParticipants();
        int size = participants.size();

        for (int i = 0; i < size; i++) {
            parti = participants.get(i);
            //Log.d(TAG, "isParticipantInviting,number:" + parti.getContact() + ",state=" + parti.getState());
            if (parti.getState() == GroupMemberData.STATE.STATE_PENDING
                && PhoneNumberUtils.compare(number, parti.getContact())) {
                Log.d(TAG, "isParticipantInviting,number:" + number + ",is inviting, size:" + size);
                return true;
            }
        }
        return false;
    }


    // private boolean isChairMan(String chatId) {
    //     //int config = SystemProperties.getInt("persist.rcs.member.ischairman", "0");
    //     String config = System.getProperty("persist.rcs.member.ischairman", "0");
    //     Log.d(TAG, "isChairMan() config:" + config);
    //     //return config.equals(mGroupChatId) ? true : false;
    //     return true;
    // }

    // private int getChairManPosition() {
    //     return 0;
    // }

    // private Uri getContactUriForContactName(String name) {
    //     Uri uri = null;
    //     if (name != null) {
    //         final String whereClause = Phone.DISPLAY_NAME + " =" + name;
    //         Cursor cursor = mContext.getContentResolver()
    //             .query(PHONES_WITH_PRESENCE_URI, CALLER_ID_PROJECTION, whereClause, null, null);
    //         if (cursor != null) {
    //             try {
    //                 while (cursor.moveToNext()) {
    //                     uri = Uri.withAppendedPath
    //                     (ContactsContract.Contacts.CONTENT_URI, cursor.getString(PHONE_ID_COLUMN));
    //                     break;
    //                 }
    //             } finally {
    //                 cursor.close();
    //             }
    //         }
    //     }

    //     return uri;
    // }

    // ==================================================
    // ==================================================
    private class PortraitUpdateListener implements PortraitService.UpdateListener {
        public void onPortraitUpdate(Portrait p, String chatId) {
            if (mGridView == null) {
                Log.d(TAG, "onPortraitUpdate() mGridView is null");
                return;
            }
            Log.d(TAG, "onPortraitUpdate():p.mNumber" + p.mNumber + ",p.mName:" + p.mName);
            GroupMemberAdapter adapter = (GroupMemberAdapter)mGridView.getAdapter();
            int size = adapter.getSize();
            for (int i = 0; i < size; i++) {
                GroupMember member = (GroupMember)adapter.getItem(i);
                if (member.getNumber().equals(p.mNumber)) {
                    View gridChild = mGridView.getChildAt(i);
                    ImageView imageView = (ImageView) gridChild.findViewById(R.id.grid_item_image);
                    TextView textView = (TextView) gridChild.findViewById(R.id.grid_item_label);
                    if (imageView != null) {
                        imageView.setImageBitmap(PortraitService.decodeString(p.mImage));
                        Log.d(TAG, "onPortraitUpdate() update imageView");
                    }

                    if (textView != null && !isParticipantInviting(p.mNumber)) {
                        // donot modify inviting member name
                        Log.d(TAG, "onPortraitUpdate() update p.mName");
                        textView.setText(p.mName);
                    }
                    break;
                }
            }

            //((GroupMemberAdapter)mGridView.getAdapter()).notifyDataSetChanged();
        }

        // after query group info done, notify caller the group member number set
        public void onGroupUpdate(String chatId, Set<String> numberSet) {
            if (mGridView == null || !(mGroupChatId.equals(chatId))) {
                return;
            }

            ArrayList<GroupMember> data = new ArrayList<GroupMember>();
            for (String number : numberSet) {
                data.add(new GroupMember(number));
            }
            ((GroupMemberAdapter)mGridView.getAdapter()).updateData(data);
            Log.d(TAG, "onGroupUpdate()data.size():" + data.size());

            //((GroupMemberAdapter)grid.getAdapter()).notifyDataSetChanged();
        }

        public void onGroupThumbnailUpdate(String chatId, Bitmap thumbnail) {
        }
    }

}