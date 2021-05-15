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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.utils.Logger;

import java.util.Locale;

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

        DB.initComplete.setValue(false);
        DB.initComplete.observe(this, this::onDbInitDoneChanged);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Logger.debug("splash", "onStart()");
        running = true;

        startupTime = System.currentTimeMillis();

        DatabaseInitThread dbInitThread = new DatabaseInitThread();
        Logger.debug("splash", "starting dbInit task");
        dbInitThread.start();
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
            final long delay = keepActiveForMS - (now - startupTime);
            Logger.debug("splash",
                    String.format(Locale.ROOT, "Scheduling main activity start in %d milliseconds",
                            delay));
            new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, delay);
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
    private static class DatabaseInitThread extends Thread {
        @Override
        public void run() {
            long ignored = DB.get()
                             .getProfileDAO()
                             .getProfileCountSync();

            DB.initComplete.postValue(true);
        }
    }
}
