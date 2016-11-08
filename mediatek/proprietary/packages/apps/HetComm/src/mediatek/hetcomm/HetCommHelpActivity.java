package com.mediatek.hetcomm;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Help UI activity for HetComm.
 *
 * @hide
 */
public class HetCommHelpActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hetcomm_help_activity);

        final Resources r = this.getBaseContext().getResources();
        TextView helpText = (TextView) findViewById(R.id.help_text);
        helpText.setMovementMethod(LinkMovementMethod.getInstance());
        helpText.setText(Html.fromHtml(r.getString(R.string.hetcomm_help)));

    }

}