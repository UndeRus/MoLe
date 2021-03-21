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

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Logger;

@SuppressLint("Registered")
public class ProfileThemedActivity extends CrashReportingActivity {
    protected MobileLedgerProfile mProfile;
    private boolean themeSetUp = false;
    private boolean mIgnoreProfileChange;
    protected void setupProfileColors() {
        final int themeHue = (mProfile == null) ? -1 : mProfile.getThemeHue();

        Colors.setupTheme(this, themeHue);

        if (themeSetUp) {
            Logger.debug("prf-thm-act",
                    "setupProfileColors(): theme already set up, recreating activity");
            this.recreate();
        }
        themeSetUp = true;

        Colors.profileThemeId = Data.retrieveCurrentThemeIdFromDb();
    }
    @Override
    protected void onStart() {
        super.onStart();
        Colors.refreshColors(getTheme());
    }
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initProfile();
        setupProfileColors();

        mIgnoreProfileChange = true;
        Data.observeProfile(this, profile -> {
            if (!mIgnoreProfileChange) {
                mProfile = profile;
                setupProfileColors();
            }

            mIgnoreProfileChange = false;
        });

        super.onCreate(savedInstanceState);
    }
    protected void initProfile() {
        mProfile = Data.initProfile();
    }
}
