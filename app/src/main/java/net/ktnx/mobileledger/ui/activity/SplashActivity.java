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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

public class SplashActivity extends CrashReportingActivity {
    private static final long keepActiveForMS = 400;
    private long startupTime;
    private boolean running = true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_default);
        setContentView(R.layout.splash_activity_layout);
        Logger.debug("splash", "onCreate()");

        MobileLedgerDatabase.initComplete.setValue(false);
        MobileLedgerDatabase.initComplete.observe(this, this::onDbInitDoneChanged);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Logger.debug("splash", "onStart()");
        running = true;

        startupTime = System.currentTimeMillis();

        AsyncTask<Void, Void, Void> dbInitTask = new DatabaseInitTask();
        dbInitTask.execute();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Logger.debug("splash", "onPause()");
        running = false;
    }
    @Override
    protected void onResume() {
        super.onResume();
        Logger.debug("splash", "onResume()");
        running = true;
    }
    private void onDbInitDoneChanged(Boolean done) {
        if (!done) {
            Logger.debug("splash", "DB not yet initialized");
            return;
        }

        Logger.debug("splash", "DB init done");
        long now = System.currentTimeMillis();
        if (now > startupTime + keepActiveForMS)
            startMainActivity();
        else {
            new Handler().postDelayed(this::startMainActivity,
                    keepActiveForMS - (now - startupTime));
        }
    }
    private void startMainActivity() {
        if (running) {
            Logger.debug("splash", "still running, launching main activity");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_slowly, R.anim.fade_out_slowly);
        }
        else {
            Logger.debug("splash", "Not running, finish and go away");
            finish();
        }
    }
    private static class DatabaseInitTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            MobileLedgerProfile.loadAllFromDB(null);

            String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
            MobileLedgerProfile startupProfile = Data.getProfile(profileUUID);
            if (startupProfile != null)
                Data.setCurrentProfile(startupProfile);
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            Logger.debug("splash", "DatabaseInitTask::onPostExecute()");
            super.onPostExecute(aVoid);
            MobileLedgerDatabase.initComplete.setValue(true);
        }
    }
}
