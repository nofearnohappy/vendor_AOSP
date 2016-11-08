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

import java.io.InputStream;
import java.util.ArrayList;
import android.util.Log;

public class DlbApInfoExtractor {
	private static final String TAG = "DlbApInfoExtractor";
	
	private IAPMetadataParser mApParser;
	private InputStream mApInfoStream = null;
	
	public DlbApInfoExtractor(){
		super();
		mApParser = new DlbAPMetadataParser();
		Log.d(TAG, "Constructor");
	}

	public void setApInfoFile(InputStream apstream){
		if (!apstream.equals(mApInfoStream)){
		    mApInfoStream = apstream;
		    mApParser.parseFile(mApInfoStream);
		}
	}
	
	public String getTechInfo() {
		
		return mApParser.getTechInfo();
	}

	public String getFormatVersion() {
		
		return mApParser.getFormatVersion();
	}

	public ArrayList<AutoPilotItem> getAutoPilotMetadata() {
		
		return mApParser.getAutoPilotMetadata();
	}
}