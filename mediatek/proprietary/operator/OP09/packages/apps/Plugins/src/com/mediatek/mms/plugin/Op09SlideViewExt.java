package com.mediatek.mms.plugin;

import com.mediatek.mms.ext.DefaultOpSlideViewExt;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.widget.ImageViewEx;

public class Op09SlideViewExt extends DefaultOpSlideViewExt {

    private Context mContext;

    public Op09SlideViewExt(Context base) {
        super(base);
        mContext = base;
    }

    @Override
    public void setImage(ImageViewEx imageView, ViewGroup viewGroup) {
        if (imageView == null) {
            /// M: OP09 Feature: Unsupported Files; @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                Op09MmsUnSupportedFilesExt.getIntance(mContext)
                            .initUnsupportedViewForImage(viewGroup);
            }
            /// @}
        }
    }

    @Override
    public void initAudioInfoView(boolean conformanceMode, LinearLayout audioInfoView,
                                LinearLayout viewPort, ViewGroup viewGroup) {
        if (conformanceMode) {
            /// M: OP09 Feature: Unsupported Files; @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                Op09MmsUnSupportedFilesExt.getIntance(mContext)
                    .setAudioUnsupportedIcon(audioInfoView);
                Op09MmsUnSupportedFilesExt.getIntance(mContext)
                    .initUnsupportedViewForAudio(viewPort);
            }
            /// @}
        } else {
            /// M: OP09 Feature: Unsupported Files; @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                Op09MmsUnSupportedFilesExt.getIntance(mContext)
                    .setAudioUnsupportedIcon(audioInfoView);
                Op09MmsUnSupportedFilesExt.getIntance(mContext)
                    .initUnsupportedViewForAudio(viewGroup);
            }
            /// @}
        }
    }

    @Override
    public void displayAudioInfo() {
        /// M: OP09 Feature: Unsupported Files;@{
        if (MessageUtils.isUnsupportedFilesOn()) {
            Op09MmsUnSupportedFilesExt.getIntance(mContext)
                .setUnsupportedViewVisibilityForAudio(true);
        }
        /// @}
    }

    @Override
    public void hideAudioInfo() {
        /// M: OP09 Feature: Unsupported Files;@{
        if (MessageUtils.isUnsupportedFilesOn()) {
            Op09MmsUnSupportedFilesExt.getIntance(mContext)
                .setUnsupportedViewVisibilityForAudio(false);
        }
        /// @}
    }

    @Override
    public void setAudio(Uri audio) {
        /// M: OP09 Feature: Unsupported Files;@{
        if (MessageUtils.isUnsupportedFilesOn()) {
            Op09MmsUnSupportedFilesExt.getIntance(mContext).setAudioUri(audio);
        }
        /// @}
    }

    @Override
    public void videoViewMatchStatusbar(View view, ViewGroup viewGroup) {
        /// M: OP09 Feature: Unsupported Files; @{
        if (MessageUtils.isUnsupportedFilesOn() && view instanceof ImageViewEx) {
            Op09MmsUnSupportedFilesExt.getIntance(mContext)
                                      .initUnsupportedViewForImage(viewGroup);
        }
        /// @}
    }


}
