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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorLong;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.HueRing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import static java.lang.Math.abs;
import static net.ktnx.mobileledger.utils.Logger.debug;

public class Colors {
    public static final int DEFAULT_HUE_DEG = 261;
    public static final int THEME_HUE_STEP_DEG = 5;
    private static final float blueLightness = 0.665f;
    private static final float yellowLightness = 0.350f;
    private static final int[][] EMPTY_STATES = new int[][]{new int[0]};
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
    private static int[] themeIDs =
            {R.style.AppTheme_NoActionBar_000, R.style.AppTheme_NoActionBar_005,
             R.style.AppTheme_NoActionBar_010, R.style.AppTheme_NoActionBar_015,
             R.style.AppTheme_NoActionBar_020, R.style.AppTheme_NoActionBar_025,
             R.style.AppTheme_NoActionBar_030, R.style.AppTheme_NoActionBar_035,
             R.style.AppTheme_NoActionBar_040, R.style.AppTheme_NoActionBar_045,
             R.style.AppTheme_NoActionBar_050, R.style.AppTheme_NoActionBar_055,
             R.style.AppTheme_NoActionBar_060, R.style.AppTheme_NoActionBar_065,
             R.style.AppTheme_NoActionBar_070, R.style.AppTheme_NoActionBar_075,
             R.style.AppTheme_NoActionBar_080, R.style.AppTheme_NoActionBar_085,
             R.style.AppTheme_NoActionBar_090, R.style.AppTheme_NoActionBar_095,
             R.style.AppTheme_NoActionBar_100, R.style.AppTheme_NoActionBar_105,
             R.style.AppTheme_NoActionBar_110, R.style.AppTheme_NoActionBar_115,
             R.style.AppTheme_NoActionBar_120, R.style.AppTheme_NoActionBar_125,
             R.style.AppTheme_NoActionBar_130, R.style.AppTheme_NoActionBar_135,
             R.style.AppTheme_NoActionBar_140, R.style.AppTheme_NoActionBar_145,
             R.style.AppTheme_NoActionBar_150, R.style.AppTheme_NoActionBar_155,
             R.style.AppTheme_NoActionBar_160, R.style.AppTheme_NoActionBar_165,
             R.style.AppTheme_NoActionBar_170, R.style.AppTheme_NoActionBar_175,
             R.style.AppTheme_NoActionBar_180, R.style.AppTheme_NoActionBar_185,
             R.style.AppTheme_NoActionBar_190, R.style.AppTheme_NoActionBar_195,
             R.style.AppTheme_NoActionBar_200, R.style.AppTheme_NoActionBar_205,
             R.style.AppTheme_NoActionBar_210, R.style.AppTheme_NoActionBar_215,
             R.style.AppTheme_NoActionBar_220, R.style.AppTheme_NoActionBar_225,
             R.style.AppTheme_NoActionBar_230, R.style.AppTheme_NoActionBar_235,
             R.style.AppTheme_NoActionBar_240, R.style.AppTheme_NoActionBar_245,
             R.style.AppTheme_NoActionBar_250, R.style.AppTheme_NoActionBar_255,
             R.style.AppTheme_NoActionBar_260, R.style.AppTheme_NoActionBar_265,
             R.style.AppTheme_NoActionBar_270, R.style.AppTheme_NoActionBar_275,
             R.style.AppTheme_NoActionBar_280, R.style.AppTheme_NoActionBar_285,
             R.style.AppTheme_NoActionBar_290, R.style.AppTheme_NoActionBar_295,
             R.style.AppTheme_NoActionBar_300, R.style.AppTheme_NoActionBar_305,
             R.style.AppTheme_NoActionBar_310, R.style.AppTheme_NoActionBar_315,
             R.style.AppTheme_NoActionBar_320, R.style.AppTheme_NoActionBar_325,
             R.style.AppTheme_NoActionBar_330, R.style.AppTheme_NoActionBar_335,
             R.style.AppTheme_NoActionBar_340, R.style.AppTheme_NoActionBar_345,
             R.style.AppTheme_NoActionBar_350, R.style.AppTheme_NoActionBar_355,
             };
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
    public static @ColorInt
    int hslColor(float hueRatio, float saturation, float lightness) {
        return 0xff000000 | hslTriplet(hueRatio, saturation, lightness);
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

        if (h < 1 || h == 6)
            return tupleToColor(c + m, x + m, 0 + m);
        if (h < 2)
            return tupleToColor(x + m, c + m, 0 + m);
        if (h < 3)
            return tupleToColor(0 + m, c + m, x + m);
        if (h < 4)
            return tupleToColor(0 + m, x + m, c + m);
        if (h < 5)
            return tupleToColor(x + m, 0 + m, c + m);
        if (h < 6)
            return tupleToColor(c + m, 0 + m, x + m);

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
        if (y < 0)
            y += 360;
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
        final int themeHue = (profile == null) ? -1 : profile.getThemeHue();
        setupTheme(activity, themeHue);
    }
    public static void setupTheme(Activity activity, int themeHue) {
        int themeId = -1;
        if (themeHue == 360)
            themeHue = 0;
        if ((themeHue >= 0) && (themeHue < 360)) {
            int index;
            if ((themeHue % HueRing.hueStepDegrees) != 0) {
                Logger.warn("profiles",
                        String.format(Locale.US, "Adjusting unexpected hue %d", themeHue));
                index = Math.round(1f * themeHue / HueRing.hueStepDegrees);
            }
            else
                index = themeHue / HueRing.hueStepDegrees;

            themeId = themeIDs[index];
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

    public static @NonNull
    ColorStateList getColorStateList() {
        return getColorStateList(profileThemeId);
    }
    public static @NonNull
    ColorStateList getColorStateList(int hue) {
        return new ColorStateList(EMPTY_STATES, getColors(hue));
    }
    public static int[] getColors() {
        return getColors(profileThemeId);
    }
    public static int[] getColors(int hue) {
        int[] colors = new int[]{0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 6; i++, hue = (hue + 60) % 360) {
            colors[i] = getPrimaryColorForHue(hue);
        }
        return colors;
    }
    public static int getNewProfileThemeHue(ArrayList<MobileLedgerProfile> profiles) {
        if ((profiles == null) || (profiles.size() == 0))
            return DEFAULT_HUE_DEG;

        int chosenHue;

        if (profiles.size() == 1) {
            int opposite = profiles.get(0)
                                   .getThemeHue() + 180;
            opposite %= 360;
            chosenHue = opposite;
        }
        else {
            ArrayList<Integer> hues = new ArrayList<>();
            for (MobileLedgerProfile p : profiles) {
                int hue = p.getThemeHue();
                if (hue == -1)
                    hue = DEFAULT_HUE_DEG;
                hues.add(hue);
            }
            Collections.sort(hues);
            hues.add(hues.get(0));

            int lastHue = -1;
            int largestInterval = 0;
            ArrayList<Integer> largestIntervalStarts = new ArrayList<>();

            for (int h : hues) {
                if (lastHue == -1) {
                    lastHue = h;
                    continue;
                }

                int interval;
                if (h > lastHue)
                    interval = h - lastHue;     // 10 -> 20 is a step of 10
                else
                    interval = h + (360 - lastHue);    // 350 -> 20 is a step of 30

                if (interval > largestInterval) {
                    largestInterval = interval;
                    largestIntervalStarts.clear();
                    largestIntervalStarts.add(lastHue);
                }
                else if (interval == largestInterval) {
                    largestIntervalStarts.add(lastHue);
                }

                lastHue = h;
            }

            final int chosenIndex = (int) (Math.random() * largestIntervalStarts.size());
            int chosenIntervalStart = largestIntervalStarts.get(chosenIndex);

            if (largestInterval % 2 != 0)
                largestInterval++;    // round up the middle point

            chosenHue = (chosenIntervalStart + (largestInterval / 2)) % 360;
        }

        final int mod = chosenHue % THEME_HUE_STEP_DEG;
        if (mod != 0) {
            if (mod > THEME_HUE_STEP_DEG / 2)
                chosenHue += (THEME_HUE_STEP_DEG - mod); // 13 += (5-3) = 15
            else
                chosenHue -= mod;       // 12 -= 2 = 10
        }

        return chosenHue;
    }
}
