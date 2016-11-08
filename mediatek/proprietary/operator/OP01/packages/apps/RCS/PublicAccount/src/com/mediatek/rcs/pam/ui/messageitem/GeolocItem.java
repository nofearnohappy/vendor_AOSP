package com.mediatek.rcs.pam.ui.messageitem;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.util.GeoLocUtils;
import com.mediatek.rcs.pam.util.GeoLocXmlParser;

public class GeolocItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX + "GeolocItem";
    protected View mGeolocView;

    public GeolocItem(ViewGroup layout) {
        super(layout);

        mGeolocView = (View) mLayout.findViewById(R.id.ip_geoloc);
        mGeolocView.setVisibility(View.VISIBLE);
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);
    }

    @Override
    public void unbind() {
        mGeolocView.setVisibility(View.GONE);
        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {

    }

    @Override
    public void onMessageListItemClick() {
        if (mMessageData == null) {
            Log.d(TAG, "onMessageListItemClick():Message item is null !");
            return;
        }

        String mediaPath = mMessageData.getMessageContent().mediaPath;
        if (TextUtils.isEmpty(mediaPath)) {
            Toast.makeText(mLayout.getContext(), "mediaPath is null",
                    Toast.LENGTH_LONG).show();
            return;
        }
        GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(mediaPath);
        double latitude = parser.getLatitude();
        double longitude = parser.getLongitude();
        Log.d(TAG, "parseGeoLocXml: latitude=" + latitude + ", longtitude="
                + longitude);

        if (latitude != 0.0 || longitude != 0.0) {
            Uri uri = Uri.parse("geo:" + latitude + "," + longitude);
            Intent locIntent = new Intent(Intent.ACTION_VIEW, uri);
            mLayout.getContext().startActivity(locIntent);
        } else {
            Toast.makeText(mLayout.getContext(), "parse geoloc info fail",
                    Toast.LENGTH_LONG).show();
        }
    }
}
