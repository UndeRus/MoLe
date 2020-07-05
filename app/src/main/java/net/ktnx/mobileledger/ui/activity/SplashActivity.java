/*
 * Copyright © 2020 Damyan Ivanov.
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.MLDB;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_default);
        setContentView(R.layout.splash_activity_layout);
    }
    @Override
    protected void onStart() {
        super.onStart();

        MobileLedgerProfile.loadAllFromDB(null);

        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        MobileLedgerProfile startupProfile = Data.getProfile(profileUUID);
        if (startupProfile != null)
            Data.setCurrentProfile(startupProfile);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.dummy);
        }, 250);
    }
}
