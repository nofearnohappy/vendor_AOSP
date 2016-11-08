package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mediatek.mms.callback.ISlideshowModelCallback;
import com.mediatek.mms.callback.ISlideshowEditorCallback;
import com.mediatek.mms.callback.ISlideshowEditActivityCallback;

public class DefaultOpSlideshowEditActivityExt extends ContextWrapper implements
        IOpSlideshowEditActivityExt {

    public DefaultOpSlideshowEditActivityExt(Context base) {
        super(base);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu, ISlideshowModelCallback slideshow,
            int menuAdd) {
        return false;
    }

    public boolean adjustAddSlideVisibility(Menu optionsMenu, ISlideshowModelCallback slideshow,
            View addSlideItem) {
        return false;
    }

    public void onOptionsItemSelected(Context context, MenuItem item,
            ISlideshowEditorCallback slideshowEditor,
            ISlideshowEditActivityCallback slideshowEditActivity) {

    }
}
