package com.mediatek.rcs.pam.model;

import android.text.TextUtils;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.util.Utils;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MenuEntry implements SanityCheck {
    public String commandId;
    public String title;
    public int type;
    public int priority; // priority in parent menu
    public final List<MenuEntry> subMenuItems;

    public static class MenuItemComparator implements Comparator<MenuEntry> {
        @Override
        public int compare(MenuEntry l, MenuEntry r) {
            return l.priority - r.priority;
        }
    }

    public static final MenuItemComparator COMPARATOR = new MenuItemComparator();

    public MenuEntry() {
        subMenuItems = new LinkedList<MenuEntry>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"MenuItem\", commandId:\"")
          .append(commandId)
          .append("\", title:\"")
          .append(title)
          .append("\", type:")
          .append(type)
          .append(", priority:")
          .append(priority)
          .append(", subMenuItems:[");
        for (int i = 0; i < subMenuItems.size(); ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(subMenuItems.get(i).toString());
        }
        sb.append("]}");
        return sb.toString();
    }

    public boolean hasSubMenu() {
        return !subMenuItems.isEmpty();
    }

    @Override
    public void checkSanity() throws PAMException {
        if (hasSubMenu()) {
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                    (commandId != null || type != Constants.MENU_TYPE_INVALID));
        } else {
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                    TextUtils.isEmpty(commandId));
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    (type > Constants.MAX_MENU_TYPE || type < Constants.MIN_MENU_TYPE));
        }

        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                TextUtils.isEmpty(title));

        for (MenuEntry item : subMenuItems) {
            item.checkSanity();
        }
    }

    /**
     * Sort by priority.
     */
    public void sortSubMenus() {
        Collections.sort(subMenuItems, COMPARATOR);
    }

    public void buildMenuItemString(XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        buildMenuItemStringRecursively(serializer, this);
    }

    private void buildMenuItemStringRecursively(XmlSerializer serializer, MenuEntry item)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer
            .startTag(null, CommonXmlTags.MENU)
            .startTag(null, CommonXmlTags.COMMANDID)
            .text(item.commandId == null ? "" : item.commandId)
            .endTag(null, CommonXmlTags.COMMANDID)
            .startTag(null, CommonXmlTags.TITLE)
            .text(item.title)
            .endTag(null, CommonXmlTags.TITLE)
            .startTag(null, CommonXmlTags.TYPE)
            .text(item.type == Constants.INVALID ? "" : Integer.toString(item.type))
            .endTag(null, CommonXmlTags.TYPE)
            .startTag(null, CommonXmlTags.PRIORITY)
            .text(Integer.toString(item.priority))
            .endTag(null, CommonXmlTags.PRIORITY);
        if (item.subMenuItems.size() > 0) {
            serializer.startTag(null, CommonXmlTags.SUBMENU);
            for (MenuEntry subItem : item.subMenuItems) {
                buildMenuItemStringRecursively(serializer, subItem);
            }
            serializer.endTag(null, CommonXmlTags.SUBMENU);
        }
        serializer.endTag(null, CommonXmlTags.MENU);
    }

}
