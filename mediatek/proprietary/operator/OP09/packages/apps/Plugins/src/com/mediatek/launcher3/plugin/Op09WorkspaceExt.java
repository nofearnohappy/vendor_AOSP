package com.mediatek.launcher3.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.common.PluginImpl;
import com.mediatek.launcher3.ext.DefaultWorkspaceExt;
import com.mediatek.launcher3.ext.LauncherLog;
import com.mediatek.op09.plugin.R;

/**
 * OP09 IWorkspaceExt implements for Launcher3.
 */
@PluginImpl(interfaceName = "com.mediatek.launcher3.ext.IWorkspaceExt")
public class Op09WorkspaceExt extends DefaultWorkspaceExt {
    private static final String TAG = "Op09WorkspaceExt";

    private static final int WORKSPACE_ICON_TEXT_LINENUM = 2;
    private static final int WORKSPACE_ICON_TEXT_SIZE_SP = 12;
    private static final int WORKSPACE_SCREEN_NUMBER = 5;

    /**
     * Constructs a new Op09WorkspaceExt instance.
     * @param context A Context object
     */
    public Op09WorkspaceExt(Context context) {
        super(context);
    }

    @Override
    public boolean supportEditAndHideApps() {
        return true;
    }

    @Override
    public boolean supportAppListCycleSliding() {
        return true;
    }

    @Override
    public void customizeWorkSpaceIconText(TextView tv, float orgTextSize) {
        LauncherLog.d(TAG, "customizeWorkSpaceIconText: orgTextSize = " + orgTextSize
                + ", maxlines = " + WORKSPACE_ICON_TEXT_LINENUM
                + ", workspaceTextSize = " + WORKSPACE_ICON_TEXT_SIZE_SP);

        tv.setSingleLine(false);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxLines(WORKSPACE_ICON_TEXT_LINENUM);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, WORKSPACE_ICON_TEXT_SIZE_SP);
    }

    @Override
    public void customizeCompoundPaddingForBubbleText(TextView tv, int orgPadding) {
        LauncherLog.d(TAG, "customizeCompoundPaddingForBubbleText: orgPadding = " + orgPadding);
        tv.setCompoundDrawablePadding(0);
    }

    @Override
    public void customizeFolderNameLayoutParams(LayoutParams lp, int iconSizePx,
            int iconDrawablePaddingPx) {
        LauncherLog.d(TAG, "customizeFolderPreviewLayoutParams: iconSizePx = "
                + iconSizePx + ", iconDrawablePaddingPx = " + iconDrawablePaddingPx);

        lp.topMargin = iconSizePx;
    }

    @Override
    public int customizeFolderPreviewOffsetY(int orgPreviewOffsetY, int folderBackgroundOffset) {
        LauncherLog.d(TAG, "customizeFolderPreviewOffsetX: orgPreviewOffsetY = "
                + orgPreviewOffsetY + ", folderBackgroundOffset = " + folderBackgroundOffset);

        return orgPreviewOffsetY + folderBackgroundOffset / 2;
    }

    @Override
    public void customizeFolderPreviewLayoutParams(FrameLayout.LayoutParams lp) {
        final Resources res = mContext.getResources();
        final int bottomMargin = (int) res.getDimension(
                R.dimen.launcher3_folder_icon_preview_margin_bottom);

        LauncherLog.d(TAG, "customizeFolderPreviewLayoutParams: orgBottomMargin = "
                + lp.bottomMargin + ", bottomMargin = " + bottomMargin);

        // lp.bottomMargin = bottomMargin;
    }

    @Override
    public int customizeFolderCellHeight(int orgHeight) {
        final Resources res = mContext.getResources();
        final int height = (int) res.getDimension(
                R.dimen.launcher3_folder_cell_height);

        LauncherLog.d(TAG, "customizeFolderCellHeight: orgHeight = " + orgHeight
                + ", height = " + height);

        return height;
    }

    // for limited screen
    @Override
    public boolean exceedLimitedScreen(int size) {
        LauncherLog.d(TAG, "exceedLimitedScreen: size = " + size);
        return size >= WORKSPACE_SCREEN_NUMBER;
    }

    @Override
    public void customizeOverviewPanel(ViewGroup overviewPanel, View[] overviewButtons) {
        LauncherLog.d(TAG, "customizeOverviewPanel()");

        final View wallpaperButton = overviewButtons[0];
        final View widgetButton = overviewButtons[1];
        final View settingsButton = overviewButtons[2];

        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)
                overviewPanel.getLayoutParams();
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
        lp.height = FrameLayout.LayoutParams.MATCH_PARENT;

        final LinearLayout overviewPanelLayout = (LinearLayout) View.inflate(mContext,
                R.layout.launcher_overview_panel, null);
        final View editAppsButton = overviewPanelLayout.findViewById(R.id.edit_app_button);
        final View hideAppsButton = overviewPanelLayout.findViewById(R.id.hide_app_button);

        overviewPanelLayout.removeView(editAppsButton);
        overviewPanelLayout.removeView(hideAppsButton);

        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                wallpaperButton.getLayoutParams();
        editAppsButton.setLayoutParams(new LinearLayout.LayoutParams(params));
        hideAppsButton.setLayoutParams(new LinearLayout.LayoutParams(params));

        overviewPanel.addView(editAppsButton);
        overviewPanel.addView(hideAppsButton);

        overviewButtons[3] = editAppsButton;
        overviewButtons[4] = hideAppsButton;
    }
}
