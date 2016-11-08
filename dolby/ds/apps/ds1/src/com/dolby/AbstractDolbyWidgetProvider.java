/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby;

import java.util.HashSet;
import java.util.Set;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import android.dolby.DsCommon;

public abstract class AbstractDolbyWidgetProvider extends AppWidgetProvider {

    private static final Set<Class<?>> mWidgetSmallClasses = new HashSet<Class<?>>();

    private static final Set<Class<?>> mWidgetExtraLargeClasses = new HashSet<Class<?>>();

    private static DS1Application mContext;

    protected static boolean mDsOn = false;
    protected static boolean mModified = false;
    protected static int mSelectedProfile = 0;
    protected static String mSelectedProfileName = "";

    protected static void addWidgetSmallClass(Class<?> cls) {
        mWidgetSmallClasses.add(cls);
    }

    protected static void addWidgetExtraLargeClass(Class<?> cls) {
        mWidgetExtraLargeClasses.add(cls);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Log.d(Tag.WIDGET, "Widget.onUpdate " + this);

        sendInitIntent(this.getClass().getName());

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            Log.d(Tag.WIDGET, "appWidgetId: " + appWidgetId);

            RemoteViews rv = populateRemoteViews(context, null, false, isExtraLargeWidget(this));
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @SuppressWarnings("unused")
    private static void updateAllWidgets() {
        Log.d(Tag.WIDGET, "updateAllWidgets");

        ensureInitState();

        final AppWidgetManager manager = AppWidgetManager.getInstance(mContext);

        for (final Class<?> cls : mWidgetSmallClasses) {
            updateWidgets(manager, cls, false, false);
        }

        for (final Class<?> cls : mWidgetExtraLargeClasses) {
            updateWidgets(manager, cls, true, true);
        }
    }

    private static void updateWidgets(AppWidgetManager manager,
            Class<?> widgetClass, boolean large, boolean extraLarge) {
        final ComponentName cn = new ComponentName(mContext, widgetClass);
        final int[] widgetIds = manager.getAppWidgetIds(cn);
        if (widgetIds != null && widgetIds.length != 0) {
            RemoteViews rv = populateRemoteViews(mContext, null, large, extraLarge);
            manager.updateAppWidget(widgetIds, rv);
        }
    }

    protected static void updateWidgets(Class<?> widgetClass, boolean large,
            boolean extraLarge) {
        ensureInitState();
        final AppWidgetManager manager = AppWidgetManager.getInstance(mContext);
        updateWidgets(manager, widgetClass, large, extraLarge);
    }

    private static boolean isExtraLargeWidget(Object o) {
        Class<?> cls = o.getClass();
        while (cls != null) {
            for (Class<?> wcls : mWidgetExtraLargeClasses) {
                if (cls.equals(wcls)) {
                    return true;
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static RemoteViews populateRemoteViews(Context context,
            RemoteViews rv, boolean large, boolean extraLarge) {
        if (rv == null) {
        	if (extraLarge) {
        		rv = new RemoteViews(context.getPackageName(), R.layout.widget_profile_layout);
        	} else {
        		rv = new RemoteViews(context.getPackageName(), R.layout.widget_status_layout);
        	}
        }

        final boolean on = mDsOn;
        final int profile = mSelectedProfile;
        final int anames[]= {R.string.movie, R.string.music, R.string.game, R.string.voice, R.string.preset_1, R.string.preset_2};
        final int aimgon[]= {R.drawable.movieon, R.drawable.musicon, R.drawable.gameon, R.drawable.voiceon, R.drawable.preset1on, R.drawable.preset2on};
        final int aimgoff[]= {R.drawable.movieoff, R.drawable.musicoff, R.drawable.gameoff, R.drawable.voiceoff, R.drawable.preset1off, R.drawable.preset2off};
        final int aimgdis[]={R.drawable.moviedis, R.drawable.musicdis, R.drawable.gamedis, R.drawable.voicedis, R.drawable.preset1dis, R.drawable.preset2dis};
        final int aprofiles[]= {R.id.profile_1, R.id.profile_2, R.id.profile_3, R.id.profile_4, R.id.profile_5, R.id.profile_6};

        // on/off buttons
        rv.setViewVisibility(R.id.powerButtonOff, !on ? View.VISIBLE : View.INVISIBLE);
        rv.setViewVisibility(R.id.powerButtonOn, on ? View.VISIBLE : View.INVISIBLE);

        // DS logo
        rv.setImageViewResource(R.id.dsLogo, on ? R.drawable.dslogo : R.drawable.dslogodis);

        PendingIntent pendingIntent;

        pendingIntent = createWidgetIntent(context, DsCommon.CODE_DS_ON);
        rv.setOnClickPendingIntent(R.id.powerButtonOff, pendingIntent);

        pendingIntent = createWidgetIntent(context, DsCommon.CODE_DS_OFF);
        rv.setOnClickPendingIntent(R.id.powerButtonOn, pendingIntent);

        pendingIntent = createWidgetIntent(context, DsCommon.CODE_LAUNCH_APP);
        rv.setOnClickPendingIntent(R.id.dsLogo, pendingIntent);

        if (large || extraLarge) {
            final String setBgMethod = "setBackgroundResource";
            if (on) {
                // profile buttons
            	int i=0;
            	for (int prof : aprofiles){
                    rv.setImageViewResource(prof, profile == i ? aimgon[i] : aimgoff[i]);
                    rv.setInt(prof, setBgMethod, profile == i ? R.drawable.topselectedbackground : 0);
                    i++;
            	}
                // set click intents
                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_0);
                rv.setOnClickPendingIntent(R.id.profile_1, pendingIntent);

                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_1);
                rv.setOnClickPendingIntent(R.id.profile_2, pendingIntent);

                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_2);
                rv.setOnClickPendingIntent(R.id.profile_3, pendingIntent);

                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_3);
                rv.setOnClickPendingIntent(R.id.profile_4, pendingIntent);

                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_4);
                rv.setOnClickPendingIntent(R.id.profile_5, pendingIntent);

                pendingIntent = createWidgetIntent(context, DsCommon.CODE_SET_PROFILE_5);
                rv.setOnClickPendingIntent(R.id.profile_6, pendingIntent);

            } else {
            	int i=0;
            	for (int prof : aprofiles){
                    rv.setImageViewResource(prof, aimgdis[i]);
                    rv.setInt(prof, setBgMethod, 0);
                    rv.setOnClickPendingIntent(prof, null);
                    i++;
            	}
            }
        } else {

            final String setBgMethod = "setBackgroundResource";
            final String setCBgMethod = "setBackgroundColor";

            if (4 > profile) {
                rv.setTextViewText(R.id.name, context.getString(anames[profile]));
            } else{
                if (mModified) {
                    rv.setTextViewText(R.id.name, mSelectedProfileName);
                } else {
                    rv.setTextViewText(R.id.name, context.getString(anames[profile]));
                }
            }

            if (on) {
            	rv.setImageViewResource(R.id.profile_1, aimgon[profile]);
                rv.setInt(R.id.widget_bottom1, setBgMethod, R.drawable.topselectedbackgroundwsmall);
                pendingIntent = createWidgetIntent(context, DsCommon.CODE_LAUNCH_APP);
                rv.setOnClickPendingIntent(R.id.widget_bottom1, pendingIntent);
                rv.setTextColor(R.id.name, Color.WHITE);
            } else {
            	rv.setImageViewResource(R.id.profile_1, aimgdis[profile]);
            	rv.setInt(R.id.widget_bottom1, setCBgMethod, 0x80000000);
            	rv.setOnClickPendingIntent(R.id.widget_bottom1, null);
            	rv.setTextColor(R.id.name, Color.rgb(0x41, 0x72, 0x9b));
            }

            // Not setting type font because of:
            // http://stackoverflow.com/questions/3535164/how-to-change-the-font-in-android-widgets-to-user-defined-fonts-in-assets-fold

        }
        return rv;
    }

    private static PendingIntent createWidgetIntent(Context context, int code) {
        final ComponentName serviceName = new ComponentName(context, DsService.class);
        Intent intent = new Intent();
        intent.setComponent(serviceName);

        if (code == DsCommon.CODE_DS_ON || code == DsCommon.CODE_DS_OFF) {
            intent.setAction(DsCommon.ONOFF_ACTION);
            intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDONOFF);
        } else if ((code == DsCommon.CODE_SET_PROFILE_0 || code == DsCommon.CODE_SET_PROFILE_1 || code == DsCommon.CODE_SET_PROFILE_2) && (mDsOn == true)) {
            intent.setAction(DsCommon.SELECTPROFILE_ACTION);
            if (code == DsCommon.CODE_SET_PROFILE_0) {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTMOVIE);
            } else if (code == DsCommon.CODE_SET_PROFILE_1) {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTMUSIC);
            } else {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTGAME);
            }
        } else if ((code == DsCommon.CODE_SET_PROFILE_3 || code == DsCommon.CODE_SET_PROFILE_4 || code == DsCommon.CODE_SET_PROFILE_5) && (mDsOn == true)) {
            intent.setAction(DsCommon.SELECTPROFILE_ACTION);
            if (code == DsCommon.CODE_SET_PROFILE_3) {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTVOICE);
            } else if (code == DsCommon.CODE_SET_PROFILE_4) {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTPRESET1);
            } else {
                intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDSELECTPRESET2);
            }
        } else if (code == DsCommon.CODE_LAUNCH_APP) {
            intent.setAction(DsCommon.LAUNCH_DOLBY_APP_ACTION);
        } else {
            // Currently, it should not go to here.
        }

        return PendingIntent.getService(context, code /* no requestCode */, intent, 0 /*
                                                                                       * no
                                                                                       * flags
                                                                                       */);
    }

    protected boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    protected void sendInitIntent(String className) {
        Intent intent = new Intent(DsCommon.INIT_ACTION);
        intent.putExtra(DsCommon.CMDNAME, DsCommon.CMDINIT);
        intent.putExtra(DsCommon.WIDGET_CLASS, className);
        ensureInitState();
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(Tag.WIDGET, "Widget.onEnabled");
        super.onEnabled(context);
        ensureInitState();
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(Tag.WIDGET, "Widget.onDisabled");
        super.onDisabled(context);
        destruct();
    }

    private static void ensureInitState() {
        if (mContext == null) {
            mContext = DS1Application.getStaticContext();
        }
    }

    private void destruct() {
        // do nothing
    }
}
