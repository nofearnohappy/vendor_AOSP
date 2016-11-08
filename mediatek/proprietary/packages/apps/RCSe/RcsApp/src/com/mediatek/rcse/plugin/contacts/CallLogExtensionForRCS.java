/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcse.plugin.contacts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.rcs.R;
import com.mediatek.rcse.settings.RcsSettings;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultRCSeCallLogExtension;
import com.mediatek.rcse.plugin.contacts.CallLogExtention.Action;

@PluginImpl(interfaceName="com.mediatek.dialer.ext.IRCSeCallLogExtension")
public class CallLogExtensionForRCS extends DefaultRCSeCallLogExtension {
	private static final String TAG = "CallLogExtensionForRCS";
	private CallLogExtention mCallLogPlugin;
	Context mContext;
	private Action[] mRcsActions;
	// private Action [] RCSActions;
	public static final String RCS_DISPLAY_NAME = "rcs_display_name";
	public static final String RCS_PHONE_NUMBER = "rcs_phone_number";
	public static final String RCS_FT_SUPPORTED = "rcs_ft_supported";
	public static final int RCS_ICON_ID = 100;
	public static final int RCS_DETAIL_ID = 100;
	//public static final String COMMD_FOR_RCS = "ExtenstionForRCS";
	private Activity mActivity;
	// private ImageView mRCSIcon;
	private Drawable mExtenstionIcon;

	public CallLogExtensionForRCS(Context context) {
		mContext = context;
		mCallLogPlugin = new CallLogExtention(context);
		RcsSettings.createInstance(mContext);
	}

	public void bindPluginViewForCallLogList(Context context, ViewGroup viewGroup, String number) {
        log("bindPluginViewForCallLogList number:"+number);
        Drawable rcsIcon = null;
        boolean canSetRCSIcon = false;
        if (mCallLogPlugin != null && number != null) {
			rcsIcon = mCallLogPlugin.getContactPresence(number);
			boolean isEnabled = mCallLogPlugin.isEnabled();
			Log.i(TAG, "[bindPluginViewForCallLogList] isEnabled : " + isEnabled );
			if ((rcsIcon != null) && isEnabled ) {
				canSetRCSIcon = true;
			} else {
				Log.i(TAG, "[bindPluginViewForCallLogList] icon : " + rcsIcon + " |isEnabled : " + isEnabled);
				canSetRCSIcon = false;
			}
		} else {
			Log.e(TAG, "[bindPluginViewForCallLogList] mCallLogPlugin : " + mCallLogPlugin);
			canSetRCSIcon = false;
		}
        
        //canSetRCSIcon true means contact is rcs contact, need to add imageview to viewgroup
        View view = viewGroup.findViewById(RCS_ICON_ID);
        if(canSetRCSIcon && (view == null))
        {
	        LayoutInflater mInflater;
	        mInflater = LayoutInflater.from(mContext);
	        View iconLayout = mInflater.inflate(R.layout.rcs_icon_plugin, null);
	        iconLayout.setId(RCS_ICON_ID);
	        ImageView rcsIconImageView = (ImageView)iconLayout.findViewById(R.id.rcs_icon);
	        rcsIconImageView.setVisibility(View.VISIBLE);
	        rcsIconImageView.setImageDrawable(mCallLogPlugin.getAppIcon());
	        viewGroup.addView(iconLayout,2);
	        
        }
    }
	
	/*public void updatePluginViewForCallDetailControls(Context context, ViewGroup baseViewGroup,
            String name, String number) {
        log("updatePluginViewForCallDetailControls name:"+name+" number:"+number+"mCallLogPlugin:"+mCallLogPlugin);
        Drawable rcsIcon = null;
        String chat = null;
		Drawable rcsActionIcon = null;
		mActivity = (Activity)context;
		// if it has im and file transfer function the values is true.
		boolean hasIM = false;
		boolean hasFT = false;
		boolean isEnable = false;
		String[] contactInfo = {
				number, name
		};
		
		if (mCallLogPlugin != null) {
			chat = mCallLogPlugin.getChatString();
			rcsIcon = mCallLogPlugin.getContactPresence(number);
			isEnable = mCallLogPlugin.isEnabled();
			mRcsActions = mCallLogPlugin.getContactActions(number);
			if (mRcsActions[0] != null && mRcsActions[1] != null) {
				if (null != mRcsActions[0].intentAction) {
					hasIM = true;
				}
				if (null != mRcsActions[1].intentAction) {
					hasFT = true;
				}
			}
			Log.i(TAG, "[updatePluginViewForCallDetailControls] rcsIcon : " + (rcsIcon != null)
					+ " | isEnable : " + isEnable + " | hasIM , hasFT : " + hasIM + " , " + hasFT);
		}
		if (mRcsActions[1] != null) {
			rcsActionIcon = mRcsActions[1].icon;
		}
		boolean result = ((rcsIcon != null) && isEnable);
		
        //result true means need to show Joyn messaging/FT , so add view and set the properties
        View detailView = baseViewGroup.findViewById(RCS_DETAIL_ID);
        Log.i(TAG, "[updatePluginViewForCallDetailControls] detailView:"+detailView+" result:"+result);
        if(result && (detailView == null))
        {
        	LayoutInflater mInflater;
	        mInflater = LayoutInflater.from(mContext);
	        View detailLayout = mInflater.inflate(R.layout.rcs_detail_plugin, null);
	        detailLayout.setId(RCS_DETAIL_ID);
	        
	        //set the properties and onclicklistener
	        View rcsContainer = detailLayout;
			View separator03 = baseViewGroup.findViewById(com.android.dialer.R.id.separator03);
			View convertView3 = detailLayout.findViewById(R.id.RCS);
			View rcsAction = convertView3.findViewById(R.id.RCS_action);
	        
			RelativeLayout.LayoutParams relativeLpRCSContainer= new RelativeLayout.LayoutParams(
	                RelativeLayout.LayoutParams.MATCH_PARENT,
	                RelativeLayout.LayoutParams.MATCH_PARENT);
			int marginBottom = (int)mActivity.getResources().getDimension(com.android.dialer.R.dimen.call_detail_button_spacing);
			relativeLpRCSContainer.setMargins(0, 0, 0, marginBottom);
			rcsContainer.setLayoutParams(relativeLpRCSContainer);
			
			FrameLayout.LayoutParams frameLpRCS= new FrameLayout.LayoutParams(
	                FrameLayout.LayoutParams.MATCH_PARENT,
	                (int)mActivity.getResources().getDimension(com.android.dialer.R.dimen.call_log_list_item_height));
			convertView3.setLayoutParams(frameLpRCS);
			
			int paddingLeftRCSAction = (int)mActivity.getResources().getDimension(com.android.dialer.R.dimen.call_log_indent_margin);
			rcsAction.setPadding(paddingLeftRCSAction, 0, 0, 0);
			
			
			String rcsTextValue = chat + " " + number;
			Log.i(TAG, "[updatePluginViewForCallDetailControls] chat = " + chat + " | rcsTextValue : "
					+ rcsTextValue);
			if (!hasIM) {
				rcsTextValue = number;
			}
			rcsAction.setTag(contactInfo);
			TextView rcsText = (TextView) convertView3.findViewById(R.id.RCS_text);
			rcsText.setText(rcsTextValue);
			ImageView icon = (ImageView) convertView3.findViewById(R.id.RCS_icon);
			icon.setOnClickListener(mRcsTransforActionListener);
			icon.setTag(contactInfo);
			int paddingLeftIcon = (int)mActivity.getResources().getDimension(com.android.dialer.R.dimen.call_log_inner_margin);
			int paddingRightIcon = (int)mActivity.getResources().getDimension(com.android.dialer.R.dimen.call_log_outer_margin);
			icon.setPadding(paddingLeftIcon, 0, paddingRightIcon, 0);
			View dividerView = convertView3.findViewById(R.id.RCS_divider);
			rcsAction.setOnClickListener(mRcsTextActionListener);
			icon.setImageDrawable(rcsActionIcon);
			
			//rcsContainer.setVisibility(result ? View.VISIBLE : View.GONE);
			icon.setVisibility(result ? View.VISIBLE : View.GONE);
			dividerView.setVisibility(result ? View.VISIBLE : View.GONE);
			separator03.setVisibility(result ? View.VISIBLE : View.GONE);
			rcsAction.setVisibility(result ? View.VISIBLE : View.GONE);
			if (hasIM && !hasFT) {
				icon.setVisibility(View.GONE);
				dividerView.setVisibility(View.GONE);
			} else if (!hasIM && hasFT) {
				rcsAction.setClickable(false);
			} else if (!hasIM && !hasFT) {
				//rcsContainer.setVisibility(View.GONE);
				icon.setVisibility(View.GONE);
				dividerView.setVisibility(View.GONE);
				separator03.setVisibility(View.GONE);
				rcsAction.setVisibility(View.GONE);
			}
			
	        //add view to baseview
			RelativeLayout.LayoutParams relativeParam = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			relativeParam.addRule(RelativeLayout.BELOW, separator03.getId());
			baseViewGroup.addView(detailLayout, relativeParam);
        }
        
        
    }*/
	
	/*private final View.OnClickListener mRcsTransforActionListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			String[] contactInfo = (String[]) view.getTag();
			Intent intent = new Intent();
			String number = contactInfo[0];
			String name = contactInfo[1];
			
			//get FT support
			boolean isFtSupported = mCallLogPlugin.getFileTransferSupport(number);
			
			if (mRcsActions[1] != null) {
				intent = mRcsActions[1].intentAction;
				Log.i(TAG, "[mRcsTransforActionListener] intent : " + intent);
			}

			Log.i(TAG, "[mRcsTransforActionListener] name : " + name
					+ " | number : " + number + ",mRcsActions[1] = "
					+ mRcsActions[1]);
			if (TextUtils.isEmpty(name)) {
				name = number;
			}
			intent.putExtra(RCS_DISPLAY_NAME, name);
			intent.putExtra(RCS_PHONE_NUMBER, number);
			intent.putExtra(RCS_FT_SUPPORTED, isFtSupported);

			mActivity.startActivity(intent);
		}
	};*/
	
	/*private final View.OnClickListener mRcsTextActionListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			String[] contactInfo = (String[]) view.getTag();
			int mode = RcsSettings.getInstance().getMessagingUx();
			Intent intent = new Intent();
			String number = contactInfo[0];
			String name = contactInfo[1];
			if (mRcsActions[0] != null) {
				intent = mRcsActions[0].intentAction;
				Log.i(TAG, "[mRcsTextActionListener] intent : " + intent);
			}
			Log.i(TAG, "[mRcsTextActionListener] name : " + name
					+ " | number : " + number + ", mRcsActions[0] = "
					+ mRcsActions[0]);
			if (TextUtils.isEmpty(name)) {
				name = number;
			}
			intent.putExtra(RCS_DISPLAY_NAME, name);
			intent.putExtra(RCS_PHONE_NUMBER, number);
			if(mode ==1)
			{
				//fully integrated
				intent.putExtra("isjoyn", false);
			}
			else
			{
				//converged
			    intent.putExtra("isjoyn", true);
			}
			mActivity.startActivity(intent);
		}
	};*/
	
	/*public void initPluginViewForCallDetailHistory(Context context, ViewGroup header, String number) {
        log("initPluginViewForCallDetailHistory number:"+number);
        boolean result = isEnabled(number);
        View rcsContainer = header.findViewById(com.android.dialer.R.id.header_RCS_container);
        rcsContainer.setVisibility(result ? View.VISIBLE : View.GONE);
    }*/
	
	public boolean isEnabled(String number) {
		if (mCallLogPlugin != null && number != null) {
			Drawable a = mCallLogPlugin.getContactPresence(number);
			boolean isEnabled = mCallLogPlugin.isEnabled();
			Log.i(TAG, "[isEnabled] a is not null and enabled :" + (null != a) + " , " + isEnabled);
			return ((null != a) && isEnabled);
		} else {
			Log.e(TAG, "[isEnabled] mCallLogPlugin or number is null " + mCallLogPlugin + " , "
					+ number);
			return false;
		}
    }
	
	private void log(String msg) {
        Log.d(TAG, msg);
    }
}
