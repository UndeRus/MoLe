/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.TypedValue;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.HueRing;

import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorLong;
import androidx.lifecycle.MutableLiveData;

import static java.lang.Math.abs;
import static net.ktnx.mobileledger.utils.Logger.debug;

public class Colors {
    public static final int DEFAULT_HUE_DEG = 261;
    private static final float blueLightness = 0.665f;
    private static final float yellowLightness = 0.350f;
    public static @ColorInt
    int accent;
    @ColorInt
    public static int tableRowLightBG;
    @ColorInt
    public static int tableRowDarkBG;
    @ColorInt
    public static int primary, defaultTextColor;
    public static int profileThemeId = -1;
    public static MutableLiveData<Integer> themeWatch = new MutableLiveData<>(0);
    public static void refreshColors(Resources.Theme theme) {
        TypedValue tv = new TypedValue();
        theme.resolveAttribute(R.attr.table_row_dark_bg, tv, true);
        tableRowDarkBG = tv.data;
        theme.resolveAttribute(R.attr.table_row_light_bg, tv, true);
        tableRowLightBG = tv.data;
        theme.resolveAttribute(R.attr.colorPrimary, tv, true);
        primary = tv.data;
        theme.resolveAttribute(R.attr.textColor, tv, true);
        defaultTextColor = tv.data;
        theme.resolveAttribute(R.attr.colorAccent, tv, true);
        accent = tv.data;

        // trigger theme observers
        themeWatch.postValue(themeWatch.getValue() + 1);
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
    int hslColor(float hueRatio, float saturation, float lightness) {
        return 0xff000000 | hslTriplet(hueRatio, saturation, lightness);
    }
    public static @ColorInt
    int hsvTriplet(float hue, float saturation, float value) {
        @ColorLong long result;
        int r, g, b;

        if ((hue < -0.00005) || (hue > 1.0000005) || (saturation < 0) || (saturation > 1) ||
            (value < 0) || (value > 1)) throw new IllegalArgumentException(String.format(
                "hue, saturation, value and alpha must all be between 0 and 1. Arguments given: " +
                "hue=%1.5f, sat=%1.5f, val=%1.5f", hue, saturation, value));

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
    int hslTriplet(float hueRatio, float saturation, float lightness) {
        @ColorLong long result;
        float h = hueRatio * 6;
        float c = (1 - abs(2f * lightness - 1)) * saturation;
        float h_mod_2 = h % 2;
        float x = c * (1 - Math.abs(h_mod_2 - 1));
        int r, g, b;
        float m = lightness - c / 2f;

        if (h < 1 || h == 6) return tupleToColor(c + m, x + m, 0 + m);
        if (h < 2) return tupleToColor(x + m, c + m, 0 + m);
        if (h < 3) return tupleToColor(0 + m, c + m, x + m);
        if (h < 4) return tupleToColor(0 + m, x + m, c + m);
        if (h < 5) return tupleToColor(x + m, 0 + m, c + m);
        if (h < 6) return tupleToColor(c + m, 0 + m, x + m);

        throw new IllegalArgumentException(String.format(
                "Unexpected value for h (%1.3f) while converting hsl(%1.3f, %1.3f, %1.3f) to rgb",
                h, hueRatio, saturation, lightness));
    }

    public static @ColorInt
    int tupleToColor(float r, float g, float b) {
        int r_int = Math.round(255 * r);
        int g_int = Math.round(255 * g);
        int b_int = Math.round(255 * b);
        return (r_int << 16) | (g_int << 8) | b_int;
    }
    public static @ColorInt
    int getPrimaryColorForHue(int hueDegrees) {
//        int result = hsvColor(hueDegrees, 0.61f, 0.95f);
        float y = hueDegrees - 60;
        if (y < 0) y += 360;
        float l = yellowLightness + (blueLightness - yellowLightness) *
                                    (float) Math.cos(Math.toRadians(Math.abs(180 - y) / 2f));
        int result = hslColor(hueDegrees / 360f, 0.845f, l);
        debug("colors", String.format(Locale.ENGLISH, "getPrimaryColorForHue(%d) = %x", hueDegrees,
                result));
        return result;
    }
    public static void setupTheme(Activity activity) {
        MobileLedgerProfile profile = Data.profile.getValue();
        setupTheme(activity, profile);
    }
    public static void setupTheme(Activity activity, MobileLedgerProfile profile) {
        final int themeHue = (profile == null) ? -1 : profile.getThemeId();
        setupTheme(activity, themeHue);
    }
    public static void setupTheme(Activity activity, int themeHue) {
        int themeId = -1;
        // Relies that theme resource IDs are sequential numbers
        if (themeHue == 360) themeHue = 0;
        if ((themeHue >= 0) && (themeHue < 360) && ((themeHue % HueRing.hueStepDegrees) == 0)) {
            themeId = R.style.AppTheme_NoActionBar_000 + (themeHue / HueRing.hueStepDegrees);
        }

        if (themeId < 0) {
            activity.setTheme(R.style.AppTheme_NoActionBar);
            debug("profiles",
                    String.format(Locale.ENGLISH, "Theme hue %d not supported, using the default",
                            themeHue));
        }
        else {
            activity.setTheme(themeId);
        }

        refreshColors(activity.getTheme());
    }

}
