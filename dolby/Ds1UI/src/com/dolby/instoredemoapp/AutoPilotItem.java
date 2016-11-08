/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.instoredemoapp;

public class AutoPilotItem {
	private static String UNSET = "unset";

	private int mId;
	private String mTimeStamp;
	private TextInfo mDisplayText;
	private String mMasterControl;
	private String mProfileControl;
	private String mSurroundVirtualizer;
	private String mDialogEnhancer;
	private String mVolumeLeveler;
	private String mIntelligentEq;

	public AutoPilotItem(int id, String timestamp, TextInfo text,
			String mastercontrol, String profilecontrol,
			String surroundvirtualizer, String dialogenhancer,
			String volumeleveler, String intelligenteq) {
		mId = id;
		mTimeStamp = timestamp;
		mDisplayText = text;
		mMasterControl = mastercontrol;
		mProfileControl = profilecontrol;
		mSurroundVirtualizer = surroundvirtualizer;
		mDialogEnhancer = dialogenhancer;
		mVolumeLeveler = volumeleveler;
		mIntelligentEq = intelligenteq;
	}

	public AutoPilotItem() {
		super();
		mId = 0;
		mTimeStamp = UNSET;
		mDisplayText = new TextInfo();
		mDisplayText.text = "";
		mMasterControl = UNSET;
		mProfileControl = UNSET;
		mSurroundVirtualizer = UNSET;
		mDialogEnhancer = UNSET;
		mVolumeLeveler = UNSET;
		mIntelligentEq = UNSET;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public String getTimeStamp() {
		return mTimeStamp;
	}

	public void setTimeStamp(String timestamp) {
		mTimeStamp = timestamp;
	}

	public TextInfo getDisplayText() {
		return mDisplayText;
	}

	public void setDisplayText(TextInfo displaytext) {
		mDisplayText = displaytext;
	}

	public String getMasterControlValue() {
		return mMasterControl;
	}

	public void setMasterControlValue(String mastercontrol) {
		mMasterControl = mastercontrol;
	}

	public String getProfileControlValue() {
		return mProfileControl;
	}

	public void setProfileControlValue(String profilecontrol) {
		mProfileControl = profilecontrol;
	}

	public String getSurroundVirtualizerValue() {
		return mSurroundVirtualizer;
	}

	public void setSurroundVirtualizerValue(String surroundvirtualizer) {
		mSurroundVirtualizer = surroundvirtualizer;
	}

	public String getDialogEnahancerValue() {
		return mDialogEnhancer;
	}

	public void setDialogEnhancerValue(String dialogenhancer) {
		mDialogEnhancer = dialogenhancer;
	}

	public String getVolumeLevelerValue() {
		return mVolumeLeveler;
	}

	public void setVolumeLevelerValue(String volumeleveler) {
		mVolumeLeveler = volumeleveler;
	}

	public String getIntelligenEqValue() {
		return mIntelligentEq;
	}

	public void setIntelligentEqValue(String intelligenteq) {
		mIntelligentEq = intelligenteq;
	}

	public String toString() {
		String ret = new String();
		ret =  "id = " + ((Integer)mId).toString() + "\n"
				+ "timestamp = " + mTimeStamp + "\n" 
		        + "textinfo = " + mDisplayText + "\n" 
				+ "master_control = " + mMasterControl + "\n"
				+ "profile_control = " + mProfileControl + "\n"
				+ "surround_virtualizer = " + mSurroundVirtualizer + "\n"
				+ "dialog_enhancer = " + mDialogEnhancer + "\n"
				+ "volume_leveler = " + mVolumeLeveler + "\n"
				+ "intelligent_eq = " + mIntelligentEq + "\n";
		return ret;
	}
}