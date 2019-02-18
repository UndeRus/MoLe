package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorLong;
import android.util.Log;
import android.util.TypedValue;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

public class Colors {
    public static final int DEFAULT_HUE_DEG = 261;
    public static @ColorInt
    int accent;
    @ColorInt
    public static int tableRowLightBG;
    @ColorInt
    public static int tableRowDarkBG;
    @ColorInt
    public static int primary, defaultTextColor;
    public static int profileThemeId = -1;

    public static ObservableValue<Integer> themeWatch = new ObservableValue<>(0);
    public static void refreshColors(Resources.Theme theme) {
        TypedValue tv = new TypedValue();
        theme.resolveAttribute(R.attr.table_row_dark_bg, tv, true);
        tableRowDarkBG = tv.data;
        theme.resolveAttribute(R.attr.table_row_light_bg, tv, true);
        tableRowLightBG = tv.data;
        theme.resolveAttribute(R.attr.colorPrimary, tv, true);
        primary = tv.data;
        theme.resolveAttribute(android.R.color.tab_indicator_text, tv, true);
        defaultTextColor = tv.data;
        theme.resolveAttribute(R.attr.colorAccent, tv, true);
        accent = tv.data;

        // trigger theme observers
        themeWatch.notifyObservers();
    }
    public static @ColorLong
    long hsvaColor(float hue, float saturation, float value, float alpha) {
        if (alpha < 0 || alpha > 1)
            throw new IllegalArgumentException("alpha must be between 0 and 1");

        @ColorLong long rgb = hsvTriplet(hue, saturation, value);

        long a_bits = Math.round(255 * alpha);
        return (a_bits << 24) | rgb;
    }
    public static @ColorInt
    int hsvColor(float hue, float saturation, float value) {
        return 0xff000000 | hsvTriplet(hue, saturation, value);
    }
    public static @ColorInt
    int hsvTriplet(float hue, float saturation, float value) {
        @ColorLong long result;
        int r, g, b;

        if ((hue < 0) || (hue > 1) || (saturation < 0) || (saturation > 1) || (value < 0) ||
            (value > 1)) throw new IllegalArgumentException(
                "hue, saturation, value and alpha must all be between 0 and 1");

        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        switch (h) {
            case 0:
            case 6:
                return tupleToColor(value, t, p);
            case 1:
                return tupleToColor(q, value, p);
            case 2:
                return tupleToColor(p, value, t);
            case 3:
                return tupleToColor(p, q, value);
            case 4:
                return tupleToColor(t, p, value);
            case 5:
                return tupleToColor(value, p, q);
            default:
                throw new RuntimeException(String.format("Unexpected value for h (%d) while " +
                                                         "converting hsv(%1.2f, %1.2f, %1.2f) to " +
                                                         "rgb", h, hue, saturation, value));
        }
    }

    public static @ColorInt
    int tupleToColor(float r, float g, float b) {
        int r_int = Math.round(255 * r);
        int g_int = Math.round(255 * g);
        int b_int = Math.round(255 * b);
        return (r_int << 16) | (g_int << 8) | b_int;
    }
    public static @ColorInt
    int getPrimaryColorForHue(int degrees) {
        return getPrimaryColorForHue(degrees / 360f);
    }
    public static @ColorInt
    int getPrimaryColorForHue(float hue) {
        int result = hsvColor(hue, 0.61f, 0.95f);
        Log.d("colors", String.format("getPrimaryColorForHue(%1.2f) = %x", hue, result));
        return result;
    }
    public static void setupTheme(Activity activity) {
        MobileLedgerProfile profile = Data.profile.get();
        if (profile != null) {
            switch (Data.profile.get().getThemeId()) {
                case 0:
                    activity.setTheme(R.style.AppTheme_NoActionBar_0);
                    break;
                case 15:
                    activity.setTheme(R.style.AppTheme_NoActionBar_15);
                    break;
                case 30:
                    activity.setTheme(R.style.AppTheme_NoActionBar_30);
                    break;
                case 45:
                    activity.setTheme(R.style.AppTheme_NoActionBar_45);
                    break;
                case 60:
                    activity.setTheme(R.style.AppTheme_NoActionBar_60);
                    break;
                case 75:
                    activity.setTheme(R.style.AppTheme_NoActionBar_75);
                    break;
                case 90:
                    activity.setTheme(R.style.AppTheme_NoActionBar_90);
                    break;
                case 105:
                    activity.setTheme(R.style.AppTheme_NoActionBar_105);
                    break;
                case 120:
                    activity.setTheme(R.style.AppTheme_NoActionBar_120);
                    break;
                case 135:
                    activity.setTheme(R.style.AppTheme_NoActionBar_135);
                    break;
                case 150:
                    activity.setTheme(R.style.AppTheme_NoActionBar_150);
                    break;
                case 165:
                    activity.setTheme(R.style.AppTheme_NoActionBar_165);
                    break;
                case 180:
                    activity.setTheme(R.style.AppTheme_NoActionBar_180);
                    break;
                case 195:
                    activity.setTheme(R.style.AppTheme_NoActionBar_195);
                    break;
                case 210:
                    activity.setTheme(R.style.AppTheme_NoActionBar_210);
                    break;
                case 225:
                    activity.setTheme(R.style.AppTheme_NoActionBar_225);
                    break;
                case 240:
                    activity.setTheme(R.style.AppTheme_NoActionBar_240);
                    break;
                case 255:
                    activity.setTheme(R.style.AppTheme_NoActionBar_255);
                    break;
                case 270:
                    activity.setTheme(R.style.AppTheme_NoActionBar_270);
                    break;
                case 285:
                    activity.setTheme(R.style.AppTheme_NoActionBar_285);
                    break;
                case 300:
                    activity.setTheme(R.style.AppTheme_NoActionBar_300);
                    break;
                case 315:
                    activity.setTheme(R.style.AppTheme_NoActionBar_315);
                    break;
                case 330:
                    activity.setTheme(R.style.AppTheme_NoActionBar_330);
                    break;
                case 345:
                    activity.setTheme(R.style.AppTheme_NoActionBar_345);
                    break;
                default:
                    activity.setTheme(R.style.AppTheme_NoActionBar);
            }
        }
        else activity.setTheme(R.style.AppTheme_NoActionBar);

        refreshColors(activity.getTheme());
    }

}
