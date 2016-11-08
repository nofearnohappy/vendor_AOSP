package com.mediatek.vendoremail;

import com.android.email.policy.EmailPolicy;
import com.android.email.policy.R;
import com.mediatek.vendoremail.utils.ProviderListInfoUtils;

import android.os.Bundle;
import android.app.Activity;

/**
 * This activity is just for test only EmailPolicy's function.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EmailPolicy.getPolicy(this, "", Bundle.EMPTY);
        ProviderListInfoUtils.findProviderForDomain(getApplicationContext(), "163.com");
    }

}
