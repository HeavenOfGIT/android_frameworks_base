/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.du;

import android.Manifest;
import android.app.ActivityManagerNative;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;

import java.util.ArrayList;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static OverlayManager mOverlayService;

    // Check to see if a package is installed
    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    public static boolean isAvailableApp(String packageName, Context context) {
       Context mContext = context;
       final PackageManager pm = mContext.getPackageManager();
       try {
           pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
           int enabled = pm.getApplicationEnabledSetting(packageName);
           return enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
               enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
       } catch (NameNotFoundException e) {
           return false;
       }
    }

    // Check to see if device supports the Fingerprint scanner
    public static boolean hasFingerprintSupport(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(
                Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(
                Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected());
    }

    // Check to see if device not only supports the Fingerprint scanner but also if is enrolled
    public static boolean hasFingerprintEnrolled(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(
                Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(
                Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected() &&
                        fingerprintManager.hasEnrolledFingerprints());
    }

    // Check to see if device has a camera
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Check to see if device supports NFC
    public static boolean hasNFC(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    // Check to see if device supports Wifi
    public static boolean hasWiFi(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    // Check to see if device supports Bluetooth
    public static boolean hasBluetooth(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    // Check to see if device supports an alterative ambient display package
    public static boolean hasAltAmbientDisplay(Context context) {
        return context.getResources().getBoolean(com.android.internal.R.bool.config_alt_ambient_display);
    }

    // Check to see if device supports A/B (seamless) system updates
    public static boolean isABdevice(Context context) {
        return SystemProperties.getBoolean("ro.build.ab_update", false);
    }

    // Check for Chinese language
    public static boolean isChineseLanguage() {
       return Resources.getSystem().getConfiguration().locale.getLanguage().startsWith(
               Locale.CHINESE.getLanguage());
    }

    public static boolean deviceHasFlashlight(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    // Method to check if task is in lock task mode
    public static boolean isInLockTaskMode() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    // Returns today's passed time in Millisecond
    public static long getTodayMillis() {
        final long passedMillis;
        Time time = new Time();
        time.set(System.currentTimeMillis());
        passedMillis = ((time.hour * 60 * 60) + (time.minute * 60) + time.second) * 1000;
        return passedMillis;
    }

    public static boolean hasNotch(Context context) {
        int result = 0;
        int resid;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        resid = context.getResources().getIdentifier("config_fillMainBuiltInDisplayCutout",
                "bool", "android");
        if (resid > 0) {
            return context.getResources().getBoolean(resid);
        }
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = 24 * (metrics.densityDpi / 160f);
        return result > Math.round(px);
    }

    public static void sendKeycode(int keycode) {
        long when = SystemClock.uptimeMillis();
        final KeyEvent evDown = new KeyEvent(when, when, KeyEvent.ACTION_DOWN, keycode, 0,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        final KeyEvent evUp = KeyEvent.changeAction(evDown, KeyEvent.ACTION_UP);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                InputManager.getInstance().injectInputEvent(evDown,
                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputManager.getInstance().injectInputEvent(evUp,
                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
        }, 20);
    }

    public static boolean shouldShowGestureNav(Context context) {
        int navbarWidth = Settings.System.getIntForUser(context.getContentResolver(),
            Settings.System.NAVIGATION_HANDLE_WIDTH, 1, UserHandle.USER_CURRENT);
        boolean setNavbarHeight = ((navbarWidth != 0) ? true : false);
        boolean twoThreeButtonEnabled = Utils.isThemeEnabled("com.android.internal.systemui.navbar.twobutton") ||
                Utils.isThemeEnabled("com.android.internal.systemui.navbar.threebutton");
        return setNavbarHeight || twoThreeButtonEnabled;
    }

    // Method to detect whether an overlay is enabled or not
    public static boolean isThemeEnabled(String packageName) {
        if (mOverlayService == null) {
            mOverlayService = new OverlayManager();
        }
        try {
            ArrayList<OverlayInfo> infos = new ArrayList<OverlayInfo>();
            infos.addAll(mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId()));
            infos.addAll(mOverlayService.getOverlayInfosForTarget("com.android.systemui",
                    UserHandle.myUserId()));
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).packageName.equals(packageName)) {
                    return infos.get(i).isEnabled();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }

    // Method to detect whether the system dark theme is enabled or not
    public static boolean isDarkTheme(Context context) {
        UiModeManager mUiModeManager =
                context.getSystemService(UiModeManager.class);
        if (mUiModeManager == null) return false;
        int mode = mUiModeManager.getNightMode();
        return (mode == UiModeManager.MODE_NIGHT_YES);
    }

    // Method to detect navigation bar is in use
    public static boolean hasNavigationBar(Context context) {
        boolean hasNavbar = false;
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            hasNavbar = wm.hasNavigationBar(context.getDisplayId());
        } catch (RemoteException ex) {
        }
        return hasNavbar;
    }

    public static boolean deviceSupportNavigationBar(Context context) {
        return deviceSupportNavigationBarForUser(context, UserHandle.USER_CURRENT);
    }

    public static boolean deviceSupportNavigationBarForUser(Context context, int userId) {
        final boolean showByDefault = context.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        final int hasNavigationBar = Settings.System.getIntForUser(
                context.getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, -1, userId);

        if (hasNavigationBar == -1) {
            String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                return false;
            } else if ("0".equals(navBarOverride)) {
                return true;
            } else {
                return showByDefault;
            }
        } else {
            return hasNavigationBar == 1;
        }
    }
}
