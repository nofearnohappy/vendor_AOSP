package com.mediatek.phone.plugin;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultPhoneMiscExt;

/**
 * CT OP09 Phone misc feature.
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.IPhoneMiscExt")
public class OP09PhoneMiscExt extends DefaultPhoneMiscExt {
  @Override
    public boolean publishBinderDirectly() {
        return true;
    }

    /**
     * Whether need to remove "Ask First" item from call with selection list.
     *
     * @param String[] entryValues entryValues
     * @return true if need to remove it.
     */
    @Override
    public String[] removeAskFirstFromSelectionListIndex(String[] entryValues) {
        int len = entryValues.length;
        int i;
        len = len - 1;
        if (len <= 0) {
            return null;
        }
        String[] entryValues_t = new String[len];
        for(i = 0; i < len; i++) {
            entryValues_t[i] = entryValues[i];
        }

        return entryValues_t;
    }

    /**
      * remove "Ask First" item value from call with selection list.
      *
      * @param String[] entries entries
      * @return entries after remove object.
      */
    @Override
    public CharSequence[] removeAskFirstFromSelectionListValue(CharSequence[] entries) {
        int len = entries.length;
        int i;
        len = len - 1;
        if (len <= 0) {
            return null;
        }

        CharSequence[] entries_t = new CharSequence[len];
        for(i = 0; i < len; i++) {
            entries_t[i] = entries[i];
        }

        return entries_t;
    }

    /**
     * For OP09 Set the selectedIndex to the first one When remove "Ask First".
     *
     * @param selectedIndex the default index
     * @return return the first index of phone account.
     */
    public int getSelectedIndex(int selectedIndex) {
        // return 0 when remove the ask first.
        return 0;
    }
}
