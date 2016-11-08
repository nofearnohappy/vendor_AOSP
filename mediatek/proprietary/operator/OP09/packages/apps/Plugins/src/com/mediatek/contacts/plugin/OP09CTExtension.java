package com.mediatek.contacts.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.contacts.ext.DefaultCtExtension;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;

import java.util.List;

/**
 * OP09CTExtension class.
 */
@PluginImpl(interfaceName = "com.mediatek.contacts.ext.ICtExtension")
public class OP09CTExtension extends DefaultCtExtension {
    private static final String TAG = "OP09CTExtension";

    private static Bitmap sCtSim1Resourceid;
    private static Bitmap sCtSim2Resourceid;

    private Context mPluginContext;

    /**
     * for op09 from old API:OP09CTExtension.
     *
     * @param context context
     */
    public OP09CTExtension(Context context) {
        mPluginContext = context;
    }

    @Override
    public Drawable getPhotoDrawableBySub(Resources res, int subId,
            Drawable photoDrawable) {
        // TODO Auto-generated method stub
        Log.i(TAG, "getPhotoDrawableBySub");
//        return res.getDrawable(SubscriptionManager
//                .getSubInfoForSubscriber(subId).simIconRes[0]);
        return new BitmapDrawable(getSimIconBySubId(subId));
    }

    @Override
    public void loadSimCardIconBitmap(Resources res) {
        Log.i(TAG, "loadSimCardIconBitmap");
        List<SubscriptionInfo> sublist = getActivatedSubInfoList();
        if (null == sublist || sublist.size() < 1) {
            Log.w(TAG, "[loadSimCardIconBitmap] sublist size is wrong");
            return;
        }
        if (sublist.size() == 1) {
            int slotId = sublist.get(0).getSimSlotIndex();
            Log.i(TAG, "loadSimCardIconBitmap slotId = " + slotId);
            if (slotId == 0) {
//                sCtSim1Resourceid = BitmapFactory.decodeResource(res,
//                        SubscriptionManager.getSubInfoForSubscriber(sublist
//                                .get(0).subId).simIconRes[0]);
                sCtSim1Resourceid = getSimIconBySlotId(slotId);
            } else if (slotId == 1) {
//                sCtSim2Resourceid = BitmapFactory.decodeResource(res,
//                        SubscriptionManager.getSubInfoForSubscriber(sublist
//                                .get(0).subId).simIconRes[0]);
                sCtSim2Resourceid = getSimIconBySlotId(slotId);
            }

        } else if (sublist.size() == 2) {
//            sCtSim1Resourceid = BitmapFactory.decodeResource(res,
//                    SubscriptionManager.getSubInfoForSubscriber(sublist
//                            .get(0).subId).simIconRes[0]);
//            sCtSim2Resourceid = BitmapFactory.decodeResource(res,
//                    SubscriptionManager.getSubInfoForSubscriber(sublist
//                            .get(1).subId).simIconRes[0]);
            sCtSim1Resourceid = getSimIconBySlotId(0);
            sCtSim2Resourceid = getSimIconBySlotId(1);
        }
        Log.i(TAG, "loadSimCardIconBitmap sCtSim1Resourceid = " + sCtSim1Resourceid);
        Log.i(TAG, "loadSimCardIconBitmap sCtSim2Resourceid = " + sCtSim2Resourceid);
    }

    @Override
    public int showAlwaysAskIndicate(int defaultValue) {
        // in cdma mode, return 0.
        return 0;
    }

    private List<SubscriptionInfo> getActivatedSubInfoList() {
        return SubscriptionManager.from(mPluginContext).getActiveSubscriptionInfoList();
    }

    private int getSlotIdBySubId(int subId) {
        SubscriptionInfo subscriptionInfo = getSubInfoUsingSubId(subId);
        return subscriptionInfo == null ? -1 : subscriptionInfo.getSimSlotIndex();
    }

    private SubscriptionInfo getSubInfoUsingSubId(int subId) {

        List<SubscriptionInfo> subscriptionInfoList = getActivatedSubInfoList();
        if (subscriptionInfoList != null && subscriptionInfoList.size() > 0) {
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                if (subscriptionInfo.getSubscriptionId() == subId) {
                    return subscriptionInfo;
                }
            }
        }
        Log.w(TAG, "[getSubInfoUsingSubId] there has some error," +
                    " return null subId : " + subId);
        return null;
    }

    private Bitmap getSimIconBySubId(int subId) {
        SubscriptionInfo info = getSubInfoUsingSubId(subId);
        if (null == info) {
            Log.w(TAG, "[getSimIconBySubId] there has some error,"
                    + " return null subId : " + subId);
            return null;
        }
        Bitmap icon = info.createIconBitmap(mPluginContext);
        return icon;
    }

    private Bitmap getSimIconBySlotId(int slotId) {
        SubscriptionInfo info = getSubscriptionInfoBySlotId(slotId);
        if (null == info) {
            Log.w(TAG, "[getSimIconBySlotId] there has some error,"
                    + " return null slotId : " + slotId);
            return null;
        }
        Bitmap icon = info.createIconBitmap(mPluginContext);
        return icon;
    }

    private SubscriptionInfo getSubscriptionInfoBySlotId(int slotId) {
        return SubscriptionManager.from(mPluginContext).getActiveSubscriptionInfoForSimSlotIndex(
                slotId);
    }
}
