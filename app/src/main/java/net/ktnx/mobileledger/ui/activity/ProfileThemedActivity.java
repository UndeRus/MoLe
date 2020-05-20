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

package net.ktnx.mobileledger.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.MLDB;

@SuppressLint("Registered")
public class ProfileThemedActivity extends CrashReportingActivity {
    protected MobileLedgerProfile mProfile;
    protected void setupProfileColors() {
        Colors.setupTheme(this, mProfile);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Colors.refreshColors(getTheme());
    }
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initProfile();
        setupProfileColors();
        super.onCreate(savedInstanceState);


        Data.profile.observe(this, mobileLedgerProfile -> {
            mProfile = mobileLedgerProfile;
            setupProfileColors();
        });
    }
    protected void initProfile() {
        mProfile = Data.profile.getValue();
        if (mProfile == null) {
            String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
            MobileLedgerProfile startupProfile;


            startupProfile = Data.getProfile(profileUUID);
            Data.setCurrentProfile(startupProfile);
            mProfile = startupProfile;
        }
    }
}
