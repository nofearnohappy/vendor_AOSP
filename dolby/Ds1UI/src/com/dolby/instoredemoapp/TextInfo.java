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

public class TextInfo {
	public String text;
	public String textColor;
	public String textFont;
	public String textPos;
	
	public TextInfo(){
		super();
		text = "";
		textColor = "unset";
		textFont = "unset";
		textPos = "unset";	
	}
	
	public TextInfo(String txt, String color, String font, String pos){
		text = txt;
		textColor = color;
		textFont = font;
		textPos = pos;
	}
	
	public String toString() {
		String str = "TextInfo:" + "\n" 
				+ "    text = " + text + "\n"
				+ "    textColor = " + textColor + "\n"
				+ "    font = " + textFont + "\n" 
				+ "    position = " + textPos + "\n";
		return str;
	}
}