package com.mediatek.mediatekdm;

import android.app.Application;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class DmApplication extends Application {
    private static DmApplication sInstance;

    @Override
    public void onCreate() {
        Log.e(TAG.APPLICATION, "DmApplication.onCreate()");
        super.onCreate();
        PlatformManager.getInstance().clearFileWhenSwitch(this);

        createComponents();

        Log.e(TAG.APPLICATION, "Update instance");
        // Initialize the singleton reference after the initialization finished.
        sInstance = this;
    }

    private void createComponents() {
        if (DmFeatureSwitch.CMCC_SPECIFIC) {
            Class<?> componentClass;
            try {
                componentClass = Class
                        .forName("com.mediatek.mediatekdm.operator.cmcc.CMCCComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_FUMO) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.fumo.FumoComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_SCOMO) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.scomo.ScomoComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_LAWMO) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.lawmo.LawmoComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_VOLTE) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.volte.IMSComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
                componentClass = Class.forName("com.mediatek.mediatekdm.volte.XDMComponent");
                componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_WFHS) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.wfhs.WiFiHotSpotComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }

        if (DmFeatureSwitch.DM_ANDSF) {
            Class<?> componentClass;
            try {
                componentClass = Class.forName("com.mediatek.mediatekdm.andsf.ANDSFComponent");
                Constructor<?> componentConstructor = componentClass.getConstructor();
                mComponents.add((IDmComponent) componentConstructor.newInstance());
            } catch (InstantiationException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (IllegalArgumentException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                throw new Error(e);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Return the singleton of DmApplication.
     *
     * @return Reference to the singleton if it has been created (onCreate() invoked), null
     *         otherwise.
     */
    public static DmApplication getInstance() {
        return sInstance;
    }

    public ArrayList<IDmComponent> getComponents() {
        return mComponents;
    }

    public IDmComponent findComponentByName(String name) {
        for (IDmComponent component : DmApplication.getInstance().getComponents()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null;
    }

    /**
     * Convenient method to check whether any component requires silent mode.
     *
     * @return true if any component requires silent mode, false otherwise.
     */
    public boolean forceSilentMode() {
        boolean forceSilentMode = false;
        for (IDmComponent component : DmApplication.getInstance().getComponents()) {
            if (component.forceSilentMode()) {
                forceSilentMode = true;
                break;
            }
        }
        Log.d(TAG.APPLICATION, "forceSilentMode returns " + forceSilentMode);
        return forceSilentMode;
    }

    public boolean checkPrerequisites() {
        for (IDmComponent component : DmApplication.getInstance().getComponents()) {
            if (!component.checkPrerequisites()) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<IDmComponent> mComponents = new ArrayList<IDmComponent>();
}
