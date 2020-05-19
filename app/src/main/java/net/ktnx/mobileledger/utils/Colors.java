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

import net.ktnx.mobileledger.BuildConfig;
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
    public static final int baseHueStep = 60;
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
    public static int primary, defaultTextColor, defaultTextColorDisabled;
    public static int profileThemeId = -1;
    public static MutableLiveData<Integer> themeWatch = new MutableLiveData<>(0);
    public static int errorTextColor;
    private static int SWIPE_COLOR_COUNT = 6;
    private static int[] themeIDs =
            {R.style.AppTheme_000, R.style.AppTheme_005,
             R.style.AppTheme_010, R.style.AppTheme_015,
             R.style.AppTheme_020, R.style.AppTheme_025,
             R.style.AppTheme_030, R.style.AppTheme_035,
             R.style.AppTheme_040, R.style.AppTheme_045,
             R.style.AppTheme_050, R.style.AppTheme_055,
             R.style.AppTheme_060, R.style.AppTheme_065,
             R.style.AppTheme_070, R.style.AppTheme_075,
             R.style.AppTheme_080, R.style.AppTheme_085,
             R.style.AppTheme_090, R.style.AppTheme_095,
             R.style.AppTheme_100, R.style.AppTheme_105,
             R.style.AppTheme_110, R.style.AppTheme_115,
             R.style.AppTheme_120, R.style.AppTheme_125,
             R.style.AppTheme_130, R.style.AppTheme_135,
             R.style.AppTheme_140, R.style.AppTheme_145,
             R.style.AppTheme_150, R.style.AppTheme_155,
             R.style.AppTheme_160, R.style.AppTheme_165,
             R.style.AppTheme_170, R.style.AppTheme_175,
             R.style.AppTheme_180, R.style.AppTheme_185,
             R.style.AppTheme_190, R.style.AppTheme_195,
             R.style.AppTheme_200, R.style.AppTheme_205,
             R.style.AppTheme_210, R.style.AppTheme_215,
             R.style.AppTheme_220, R.style.AppTheme_225,
             R.style.AppTheme_230, R.style.AppTheme_235,
             R.style.AppTheme_240, R.style.AppTheme_245,
             R.style.AppTheme_250, R.style.AppTheme_255,
             R.style.AppTheme_260, R.style.AppTheme_265,
             R.style.AppTheme_270, R.style.AppTheme_275,
             R.style.AppTheme_280, R.style.AppTheme_285,
             R.style.AppTheme_290, R.style.AppTheme_295,
             R.style.AppTheme_300, R.style.AppTheme_305,
             R.style.AppTheme_310, R.style.AppTheme_315,
             R.style.AppTheme_320, R.style.AppTheme_325,
             R.style.AppTheme_330, R.style.AppTheme_335,
             R.style.AppTheme_340, R.style.AppTheme_345,
             R.style.AppTheme_350, R.style.AppTheme_355,
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
        defaultTextColorDisabled = 0x7f000000 | 0x00ffffff & defaultTextColor;
        theme.resolveAttribute(R.attr.colorAccent, tv, true);
        accent = tv.data;
        theme.resolveAttribute(R.attr.errorTextColor, tv, true);
        errorTextColor = tv.data;

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
    public static float baseHueLightness(int baseHueDegrees) {
        switch (baseHueDegrees % 360) {
            case 0:
                return 0.450f;   // red
            case 60:
                return 0.400f;  // yellow
            case 120:
                return 0.400f;  // green
            case 180:
                return 0.400f;  // cyan
            case 240:
                return 0.750f;  // blue
            case 300:
                return 0.500f;   // magenta
            default:
                throw new IllegalStateException(
                        String.format(Locale.US, "baseHueLightness called with invalid value %d",
                                baseHueDegrees));
        }
    }
    public static float hueLightness(int hueDegrees) {
        int mod = hueDegrees % baseHueStep;
        int x0 = hueDegrees - mod;
        int x1 = x0 + baseHueStep;

        float y0 = baseHueLightness(x0);
        float y1 = baseHueLightness(x1);

        return y0 + (hueDegrees - x0) * (y1 - y0) / (x1 - x0);
    }
    public static @ColorInt
    int getPrimaryColorForHue(int hueDegrees) {
        int result = hslColor(hueDegrees / 360f, 0.845f, hueLightness(hueDegrees));
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
    public static int getThemeIdForHue(int themeHue) {
        int themeId = -1;
        if (themeHue == 360)
            themeHue = 0;
        if ((themeHue >= 0) && (themeHue < 360) && (themeHue != DEFAULT_HUE_DEG)) {
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
            themeId = R.style.AppTheme;
            debug("profiles",
                    String.format(Locale.ENGLISH, "Theme hue %d not supported, using the default",
                            themeHue));
        }

        return themeId;
    }
    public static void setupTheme(Activity activity, int themeHue) {
        int themeId = getThemeIdForHue(themeHue);
        activity.setTheme(themeId);

        refreshColors(activity.getTheme());
    }
    public static @NonNull
    ColorStateList getColorStateList() {
        return getColorStateList(profileThemeId);
    }
    public static @NonNull
    ColorStateList getColorStateList(int hue) {
        return new ColorStateList(EMPTY_STATES, getSwipeCircleColors(hue));
    }
    public static int[] getSwipeCircleColors() {
        return getSwipeCircleColors(profileThemeId);
    }
    public static int[] getSwipeCircleColors(int hue) {
        int[] colors = new int[SWIPE_COLOR_COUNT];
        for (int i = 0; i < SWIPE_COLOR_COUNT; i++, hue = (hue + 360 / SWIPE_COLOR_COUNT) % 360) {
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
            if (BuildConfig.DEBUG) {
                StringBuilder huesSB = new StringBuilder();
                for (int h : hues) {
                    if (huesSB.length() > 0)
                        huesSB.append(", ");
                    huesSB.append(h);
                }
                debug("profiles", String.format("used hues: %s", huesSB.toString()));
            }
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

            debug("profiles",
                    String.format(Locale.US, "Choosing the middle colour between %d and %d",
                            chosenIntervalStart, chosenIntervalStart + largestInterval));

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

        debug("profiles", String.format(Locale.US, "New profile hue: %d", chosenHue));

        return chosenHue;
    }
}
