/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.dao.BaseDAO;
import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Logger;

import java.util.Locale;

@SuppressLint("Registered")
public class ProfileThemedActivity extends CrashReportingActivity {
    public static final String TAG = "prf-thm-act";
    protected static final String PARAM_PROFILE_ID = "profile-id";
    protected static final String PARAM_THEME = "theme";
    protected Profile mProfile;
    private boolean themeSetUp = false;
    private boolean mIgnoreProfileChange;
    private int mThemeHue;
    protected void setupProfileColors(int newHue) {
        if (themeSetUp && newHue == mThemeHue) {
            Logger.debug(TAG,
                    String.format(Locale.ROOT, "Ignore request to set theme to the same value (%d)",
                            newHue));
            return;
        }

        Logger.debug(TAG,
                String.format(Locale.ROOT, "Changing theme from %d to %d", mThemeHue, newHue));

        mThemeHue = newHue;
        Colors.setupTheme(this, mThemeHue);

        if (themeSetUp) {
            Logger.debug(TAG, "setupProfileColors(): theme already set up, recreating activity");
            this.recreate();
        }
        themeSetUp = true;

        Colors.profileThemeId = mThemeHue;
    }
    @Override
    protected void onStart() {
        super.onStart();
        Colors.refreshColors(getTheme());
    }
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initProfile();

        Data.observeProfile(this, profile -> {
            if (profile == null) {
                Logger.debug(TAG, "No current profile, leaving");
                return;
            }

            mProfile = profile;
            int hue = profile.getTheme();

            if (hue != mThemeHue) {
                storeProfilePref(profile);
                Logger.debug(TAG,
                        String.format(Locale.US, "profile observer calling setupProfileColors(%d)",
                                hue));
                setupProfileColors(hue);
            }
        });

        super.onCreate(savedInstanceState);
    }
    public void storeProfilePref(Profile profile) {
        App.storeStartupProfileAndTheme(profile.getId(), profile.getTheme());
    }
    protected void initProfile() {
        long profileId = App.getStartupProfile();
        int hue = App.getStartupTheme();
        if (profileId == -1)
            mThemeHue = Colors.DEFAULT_HUE_DEG;

        Logger.debug(TAG,
                String.format(Locale.US, "initProfile() calling setupProfileColors(%d)", hue));
        setupProfileColors(hue);

        initProfile(profileId);
    }
    protected void initProfile(long profileId) {
        BaseDAO.runAsync(() -> initProfileSync(profileId));
    }
    private void initProfileSync(long profileId) {
        ProfileDAO dao = DB.get()
                           .getProfileDAO();
        Profile profile = dao.getByIdSync(profileId);

        if (profile == null) {
            Logger.debug(TAG, String.format(Locale.ROOT, "Profile %d not found. Trying any other",
                    profileId));

            profile = dao.getAnySync();
        }

        if (profile == null)
            Logger.debug(TAG, "No profile could be loaded");
        else
            Logger.debug(TAG, String.format(Locale.ROOT, "Profile %d loaded. posting", profileId));
        Data.postCurrentProfile(profile);
    }
}
