package com.mediatek.floatmenu;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class FloatMenuService extends Service {
    private static final String TAG = FloatMenuService.class.getSimpleName();
    public static final String KEY_CONNECTION = "wfd_connection";
    private static final long DEFAULT_TIME_OUT = 3000;
    private WindowManager mWm = null;
    private WindowManager.LayoutParams mWmParams = null;
    private LinearLayout mMenuView = null;
    private ImageView mKeyMark = null;
    private LinearLayout mKeyLayout = null;
    private KeyButtonView mKeyBack = null;
    private KeyButtonView mKeyHome = null;
    private int mTouchSlop;
    private float mTouchStartX = 0f;
    private float mTouchStartY = 0f;
    private float mInitX = 0;
    private float mInitY = 0;
    private float mX;
    private float mY;
    private int mDirection = Gravity.RIGHT;
    private boolean mInitial;
    private int mOrientation;
    private int mSystemUiVisibility;
    private Runnable mCheckRunable = new Runnable() {

        @Override
        public void run() {
            if (!mInitial) {
                return;
            }
            shrinkMenu();
            Drawable bg = mMenuView.getBackground();
            if (bg != null) {
                bg.setAlpha(153);
            }
        }

    };

    private BroadcastReceiver mChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                int newOrientatin = getResources().getConfiguration().orientation;
                if (newOrientatin != mOrientation) {
                    orientationChanged(newOrientatin);
                    mOrientation = newOrientatin;
                }
            }
        }

    };

    private void orientationChanged(int newOrientatin) {
        removePendingCallback();
        mCheckRunable.run();
        Rect rect = new Rect();
        mMenuView.getWindowVisibleDisplayFrame(rect);
        mWmParams.x = rect.width();
        mWmParams.y = rect.height() / 2;
        mDirection = Gravity.RIGHT;
        updateViewAside();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMenus();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mChangedReceiver, filter);
    }

    private void initMenus() {
        Log.v("@M_" + TAG, "initMenus");
        if (mMenuView == null) {
            mMenuView = (LinearLayout) LayoutInflater.from(this).inflate(
                    R.layout.menu_view, null);
            mKeyMark = (ImageView) mMenuView.findViewById(R.id.menu_key_mark);
            mKeyLayout = (LinearLayout) mMenuView
                    .findViewById(R.id.menu_key_layout);
            mKeyBack = (KeyButtonView) mMenuView
                    .findViewById(R.id.menu_key_back);
            mKeyHome = (KeyButtonView) mMenuView
                    .findViewById(R.id.menu_key_home);
            mKeyBack.setKeyCode(KeyEvent.KEYCODE_BACK);
            mKeyHome.setKeyCode(KeyEvent.KEYCODE_HOME);
            mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            MenuTouchListener listener = new MenuTouchListener();
            mMenuView.setOnTouchListener(listener);
            mKeyBack.setTouchedCallback(listener);
            mKeyHome.setTouchedCallback(listener);
            mMenuView.removeAllViews();
            mMenuView.setBackgroundResource(0);
            mMenuView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            Log.v("@M_" + TAG, "onSystemUiVisibilityChange: "
                                    + visibility);
                            mSystemUiVisibility = visibility;
                            if (mInitial) {
                                updateViewWhenSystemUiChange(visibility);
                            }
                        }
                    });
        }
        if (mWmParams == null) {
            mWmParams = new WindowManager.LayoutParams();
            mWmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            mWmParams.flags |= LayoutParams.FLAG_NOT_FOCUSABLE
                    | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | LayoutParams.FLAG_NOT_TOUCH_MODAL;
            mWmParams.gravity = Gravity.LEFT | Gravity.TOP;
            mWmParams.hasSystemUiListeners = true;
            Rect rect = new Rect();
            mMenuView.getWindowVisibleDisplayFrame(rect);
            mWmParams.x = rect.width();
            mWmParams.y = rect.height() / 2;
            mDirection = Gravity.RIGHT;
            mWmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mWmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            mWmParams.format = PixelFormat.RGBA_8888;
            mOrientation = getResources().getConfiguration().orientation;
        }
        getWindowManager().addView(mMenuView, mWmParams);
        checkForTimeout(DEFAULT_TIME_OUT);
    }

    private void updateViewWhenSystemUiChange(int visibility) {
        if (visibility != 0) {
            mMenuView.removeAllViews();
            mMenuView.setBackgroundResource(0);
            removePendingCallback();
        } else {
            mMenuView.addView(mKeyMark);
            mMenuView.setBackgroundResource(R.drawable.floating_menu_bg);
            checkForTimeout(DEFAULT_TIME_OUT);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getBooleanExtra(KEY_CONNECTION, false)) {
                Log.v("@M_" + TAG, "update systemui");
                mInitial = true;
                updateViewWhenSystemUiChange(mSystemUiVisibility);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.v("@M_" + TAG, "onDestroy");
        mInitial = false;
        unregisterReceiver(mChangedReceiver);
        removePendingCallback();
        if (mMenuView != null) {
            getWindowManager().removeView(mMenuView);
        }
        super.onDestroy();
    }

    private WindowManager getWindowManager() {
        if (mWm == null) {
            mWm = (WindowManager) getApplicationContext().getSystemService(
                    Context.WINDOW_SERVICE);
        }
        return mWm;
    }

    private class MenuTouchListener implements OnTouchListener,
            KeyButtonView.TouchedCallback {
        private boolean mDragged = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mX = event.getRawX();
            mY = event.getRawY();
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitX = event.getRawX();
                mInitY = event.getRawY();
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                mDragged = true;
                removePendingCallback();
                break;
            case MotionEvent.ACTION_MOVE:
                double dist = Math.hypot(mX - mInitX, mY - mInitY);
                if (dist >= mTouchSlop) {
                    if (isMenuExpanded()) {
                        shrinkMenu();
                        mDragged = false;
                    }
                    if (mDragged) {
                        updateViewPos();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                double distance = Math.hypot(mX - mInitX, mY - mInitY);
                if (distance < mTouchSlop) {
                    performClick();
                } else {
                    updateViewAside();
                }
                mInitX = 0;
                mInitY = 0;
                mTouchStartX = 0;
                mTouchStartY = 0;
                mDragged = false;
                checkForTimeout(DEFAULT_TIME_OUT);
                break;
            case MotionEvent.ACTION_CANCEL:
                mInitX = 0;
                mInitY = 0;
                mTouchStartX = 0;
                mTouchStartY = 0;
                mDragged = false;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                if (isMenuExpanded()) {
                    shrinkMenu();
                }
                break;
            default:
                break;
            }
            return true;
        }

        @Override
        public void onTouched(int action) {
            Log.v("@M_" + TAG, "onTouched");
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                removePendingCallback();
                break;
            case MotionEvent.ACTION_UP:
                checkForTimeout(DEFAULT_TIME_OUT);
                break;
            default:
                break;
            }
        }

    }

    public void performClick() {
        Log.v("@M_" + TAG, "perform click");
        if (isMenuExpanded()) {
            shrinkMenu();
        } else {
            expandMenu();
        }
    }

    private void checkForTimeout(long timeOut) {
        mMenuView.postDelayed(mCheckRunable, timeOut);
    }

    private void removePendingCallback() {
        Drawable bg = mMenuView.getBackground();
        if (bg != null) {
            bg.setAlpha(255);
        }
        mMenuView.removeCallbacks(mCheckRunable);
    }

    private void expandMenu() {
        Log.v("@M_" + TAG, "expand menu: " + mDirection);
        switch (mDirection) {
        case Gravity.TOP:
        case Gravity.LEFT:
            mMenuView.addView(mKeyLayout, 0);
            break;
        case Gravity.RIGHT:
        case Gravity.BOTTOM:
            mMenuView.addView(mKeyLayout);
            break;
        default:
            break;
        }
        mMenuView.postInvalidate();
        mWmParams.flags &= ~(LayoutParams.FLAG_NOT_TOUCH_MODAL);
        updateView();
    }

    private void shrinkMenu() {
        Log.v("@M_" + TAG, "shrink menu");
        mMenuView.removeView(mKeyLayout);
        mWmParams.flags |= LayoutParams.FLAG_NOT_TOUCH_MODAL;
        updateView();
    }

    private boolean isMenuExpanded() {
        int index = mMenuView.indexOfChild(mKeyLayout);
        Log.v("@M_" + TAG, "index: " + index);
        return index >= 0;
    }

    public void updateViewPos() {
        Rect rect = new Rect();
        mMenuView.getWindowVisibleDisplayFrame(rect);
        mWmParams.x = (int) (mX - mTouchStartX);
        mWmParams.y = (int) (mY - mTouchStartY - mKeyMark.getHeight() / 2);
        updateView();
    }

    public void updateView() {
        getWindowManager().updateViewLayout(mMenuView, mWmParams);
    }

    public void updateViewAside() {
        Log.v("@M_" + TAG, "updateViewAside");
        Rect rect = new Rect();
        mMenuView.getWindowVisibleDisplayFrame(rect);

        int bakX = mWmParams.x + mMenuView.getWidth() / 2;
        int bakY = mWmParams.y + mMenuView.getHeight() / 2;
        int area = 1;
        if (bakX > rect.width() / 2) {
            if (bakY > rect.height() / 2) {
                area = 4;
            } else {
                area = 2;
            }
        } else {
            if (bakY > rect.height() / 2) {
                area = 3;
            } else {
                area = 1;
            }
        }
        switch (area) {
        case 4:
            if ((rect.width() - bakX) < (rect.height() - bakY)) {
                mWmParams.x = rect.width() * 2;
            } else {
                mWmParams.y = rect.height() * 2;
            }
            break;
        case 3:
            if (bakX < rect.height() - bakY) {
                mWmParams.x = 0;
            } else {
                mWmParams.y = rect.height() * 2;
            }
            break;
        case 2:
            if (bakY < rect.width() - bakX) {
                mWmParams.y = 0;
            } else {
                mWmParams.x = rect.width() * 2;
            }
            break;
        case 1:
            if (bakY < bakX) {
                mWmParams.y = 0;
            } else {
                mWmParams.x = 0;
            }
            break;
        default:
            break;
        }
        if (mWmParams.x == 0) {
            updateLayout(Gravity.LEFT);
        } else if (mWmParams.y == 0) {
            updateLayout(Gravity.TOP);
        } else if (mWmParams.x == rect.width() * 2) {
            updateLayout(Gravity.RIGHT);
        } else if (mWmParams.y == rect.height() * 2) {
            updateLayout(Gravity.BOTTOM);
        }
        updateView();
    }

    private void updateLayout(int direction) {
        Log.v("@M_" + TAG, "updateLayout: " + direction);
        switch (direction) {
        case Gravity.TOP:
            mKeyMark.setRotation(90f);
            mMenuView.setOrientation(LinearLayout.VERTICAL);
            mKeyLayout.setOrientation(LinearLayout.VERTICAL);
            break;
        case Gravity.BOTTOM:
            mKeyMark.setRotation(270f);
            mMenuView.setOrientation(LinearLayout.VERTICAL);
            mKeyLayout.setOrientation(LinearLayout.VERTICAL);
            break;
        case Gravity.LEFT:
            mKeyMark.setRotation(0f);
            mMenuView.setOrientation(LinearLayout.HORIZONTAL);
            mKeyLayout.setOrientation(LinearLayout.HORIZONTAL);
            break;
        case Gravity.RIGHT:
            mKeyMark.setRotation(180f);
            mMenuView.setOrientation(LinearLayout.HORIZONTAL);
            mKeyLayout.setOrientation(LinearLayout.HORIZONTAL);
            break;
        default:
            break;
        }
        mMenuView.postInvalidate();
        mDirection = direction;
    }

}
