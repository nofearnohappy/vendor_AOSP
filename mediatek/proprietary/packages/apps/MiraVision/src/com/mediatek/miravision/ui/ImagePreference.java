package com.mediatek.miravision.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImagePreference extends PreferenceCategory {

    public ImagePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_image);
    }

    public ImagePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_image);
    }

    public ImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_image);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        setLayoutResource(R.layout.preference_image);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        new LoadPicture(view).execute(R.drawable.picture_mode);
        return view;
    }

    private class LoadPicture extends AsyncTask<Integer, Void, Drawable> {

        private View mView;

        public LoadPicture(View view) {
            mView = view;
        }

        @Override
        protected Drawable doInBackground(Integer... params) {
            Context context =  ImagePreference.this.getContext();
            return context.getResources().getDrawable(params[0]);
        }

        protected void onPostExecute(Drawable result) {
            ImageView view = (ImageView) mView.findViewById(R.id.percentage_image);
            if (view != null) {
                view.setImageDrawable(result);
            }
        }
    }
}
