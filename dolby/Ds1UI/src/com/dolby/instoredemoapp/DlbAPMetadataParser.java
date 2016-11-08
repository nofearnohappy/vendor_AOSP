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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;
public class DlbAPMetadataParser implements IAPMetadataParser{
	
	private static final String TAG = "DlbAPMetadataParser";
		
	//tag name definition
	private static final String TECH_INFO_TAG = "technology_info";
	private static final String METADATA_VERSION_TAG = "format_version";
	private static final String ITEM_TAG = "item";
	private static final String ID_ATTRIBUTE = "id";
	private static final String TIMESTAMP_TAG = "timestamp";
	private static final String TEXTINFO_TAG = "textinfo";
	private static final String MASTER_CONTROL_TAG = "master_control";
	private static final String PROFILE_CONTROL_TAG = "profile_control";
	private static final String SURROUND_VIRTUALIZER_TAG = "surround_virtualizer";
	private static final String DIALOG_ENHANCER_TAG = "dialog_enhancer";
	private static final String VOLUME_LEVELER_TAG = "volume_leveler";
	private static final String INTELLIGENCE_EQ_TAG = "intelligent_eq";
	
	private ArrayList<AutoPilotItem> mAPItemList;
	private String mTechInfo;
	private String mFormatVersion;
	
		
	public DlbAPMetadataParser(){
		super();
	}

	public String getTechInfo() {
		
		return mTechInfo;
	}

	public String getFormatVersion() {
		
		return mFormatVersion;
	}

	public ArrayList<AutoPilotItem> getAutoPilotMetadata() {
		
		return mAPItemList;
	}
	
	public void parseFile(InputStream apstream){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		try{
			DocumentBuilder builder = factory.newDocumentBuilder(); 
			Document dom = builder.parse(apstream);
			Element root = dom.getDocumentElement(); 
			getTechInfo(root);
			getMetadataVersion(root);
			getAPItems(root);
			Log.d(TAG,  "mAPItemList.length = " + mAPItemList.size());
		}catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "failed to parse the file");
		}		
	}
	
	private void getTechInfo(Element root){
		NodeList items = root.getElementsByTagName(TECH_INFO_TAG);
		Element node = (Element)items.item(0);
		if (node.getNodeType() == Node.ELEMENT_NODE){
		    mTechInfo = node.getFirstChild().getNodeValue();
		}
		Log.d(TAG, "technology_info = " + mTechInfo);
	}
	
	private void getMetadataVersion(Element root){
		NodeList items = root.getElementsByTagName(METADATA_VERSION_TAG);
		Element node = (Element)items.item(0);
		if (node.getNodeType() == Node.ELEMENT_NODE){
		    mFormatVersion = node.getFirstChild().getNodeValue();
		}
		Log.d(TAG, "format_version = " + mFormatVersion);
	}
	
	private TextInfo getTextInfo(Node node){
		TextInfo ti = new TextInfo();
		NodeList nl = node.getChildNodes();
		for (int j = 0; j < nl.getLength(); ++j){
			Node childnode = nl.item(j);
			if (node.getNodeType() == Node.ELEMENT_NODE){
				if (childnode.getNodeName().equals("text")){
					ti.text = childnode.getFirstChild().getNodeValue();
				} else if (childnode.getNodeName().equals("color")) {
					ti.textColor = childnode.getFirstChild().getNodeValue();
				} else if (childnode.getNodeName().equals("font")) {
					ti.textFont = childnode.getFirstChild().getNodeValue();
				} else if (childnode.getNodeName().equals("position")){
					ti.textPos = childnode.getFirstChild().getNodeValue();
				}
			}
		}
		return ti;
	}
	
	private void getAPItems(Element root){
		if (mAPItemList != null){
			mAPItemList.clear();
			mAPItemList = null;
		}
		mAPItemList = new ArrayList<AutoPilotItem>();
		NodeList items = root.getElementsByTagName(ITEM_TAG);
		Log.d(TAG, "length = " + items.getLength());
		for (int i = 0; i < items.getLength(); ++i){
			AutoPilotItem apitem = new AutoPilotItem();
			Element node = (Element)items.item(i);
			Integer itemid = new Integer(node.getAttribute(ID_ATTRIBUTE));
			Log.d(TAG, "id = " + itemid);
			apitem.setId(itemid);
			NodeList nl = node.getChildNodes();
			for (int j = 0; j < nl.getLength(); ++j){
				Node childnode = nl.item(j);
				if(node.getNodeType() == Node.ELEMENT_NODE){				
					if(childnode.getNodeName().equals(TIMESTAMP_TAG)){
						String timestamp = childnode.getFirstChild().getNodeValue();
						apitem.setTimeStamp(timestamp);
					} else if (childnode.getNodeName().equals(TEXTINFO_TAG)){
					    TextInfo textinfo = getTextInfo(childnode);
						apitem.setDisplayText(textinfo);
					} else if (childnode.getNodeName().equals(MASTER_CONTROL_TAG)){
						String master_control = childnode.getFirstChild().getNodeValue();
						apitem.setMasterControlValue(master_control);
					} else if (childnode.getNodeName().equals(PROFILE_CONTROL_TAG)){
						String profile_control = childnode.getFirstChild().getNodeValue();
						apitem.setProfileControlValue(profile_control);
					} else if (childnode.getNodeName().equals(SURROUND_VIRTUALIZER_TAG)){
						String surround_virtualizer = childnode.getFirstChild().getNodeValue();
						apitem.setSurroundVirtualizerValue(surround_virtualizer);
					} else if (childnode.getNodeName().equals(DIALOG_ENHANCER_TAG)){
						String dialog_enhancer = childnode.getFirstChild().getNodeValue();
						apitem.setDialogEnhancerValue(dialog_enhancer);
					} else if (childnode.getNodeName().equals(VOLUME_LEVELER_TAG)){
						String volume_leveler = childnode.getFirstChild().getNodeValue();
						apitem.setVolumeLevelerValue(volume_leveler);
					} else if (childnode.getNodeName().equals(INTELLIGENCE_EQ_TAG)){
						String intelligent_eq = childnode.getFirstChild().getNodeValue();
						apitem.setIntelligentEqValue(intelligent_eq);
					} 
				}
			}
			Log.d(TAG, apitem.toString());
			mAPItemList.add(apitem);
		}
	}
}