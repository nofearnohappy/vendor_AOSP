package com.mediatek.systemui.plugin;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.mediatek.systemui.ext.DataType;

public class Op07StatusBarPluginTest extends InstrumentationTestCase {

    private static Op07StatusBarPlugin mStatusBarPlugin = null
    private Context context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = this.getInstrumentation().getContext();
        //mStatusBarPlugin = (Op07StatusBarPlugin) PluginManager.createPluginObject(context, "com.mediatek.systemui.ext.IStatusBarPlugin");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
       // mStatusBarPlugin = null;
    }

}
