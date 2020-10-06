package android.content.res;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.SystemProperties;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressLint("StaticUtils")
public class AccentUtils {
    private static final String TAG = "AccentUtils";

    private static final String ACCENT_COLOR_PROP = "persist.sys.nad.accent_color";

    static boolean isResourceAccent(String resName) {
        return resName.contains("accent_device_default_light")
                || resName.contains("accent_device_default_dark")
                || resName.contains("material_pixel_blue_dark")
                || resName.contains("material_pixel_blue_bright")
                || resName.contains("colorAccent")
                || resName.contains("dialer_theme_color")
                || resName.contains("dialer_theme_color_dark")
                || resName.contains("dialer_theme_color_20pct");
    }

    public static int getNewAccentColor(int defaultColor) {
        return getAccentColor(defaultColor, ACCENT_COLOR_PROP);
    }

    private static int getAccentColor(int defaultColor, String property) {
        try {
            String colorValue = SystemProperties.get(property, "-1");
            return "-1".equals(colorValue)
                    ? defaultColor : colorValue.equals("ff1a73e8")
                    ? defaultColor : Color.parseColor("#" + colorValue);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set accent: " + e.getMessage() +
                    "\nSetting default: " + defaultColor);
            return defaultColor;
        }
    }
}
