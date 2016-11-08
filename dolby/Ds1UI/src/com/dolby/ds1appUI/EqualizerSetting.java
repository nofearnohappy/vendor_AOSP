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

package com.dolby.ds1appUI;

public class EqualizerSetting {
    private String mName;
    private int mIconSelected, mIconNormal, mIconDisabled;

    public EqualizerSetting(String name, int iconSelected, int iconNormal, int iconDisabled) {
        mName = name;
        mIconSelected = iconSelected;
        mIconNormal = iconNormal;
        mIconDisabled = iconDisabled;
    }

    public String getName() {
        return mName;
    }

    public int getIcon(boolean selected, boolean enabled) {
        return enabled ? (selected ? mIconSelected : mIconNormal) : mIconDisabled;
    }

}
