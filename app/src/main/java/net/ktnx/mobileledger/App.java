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

package net.ktnx.mobileledger;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

import static net.ktnx.mobileledger.ui.activity.SettingsActivity.PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS;

public class App extends Application {
    public static App instance;
    private MobileLedgerDatabase dbHelper;
    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        updateMonthNames();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        Data.optShowOnlyStarred.set(p.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));
        SharedPreferences.OnSharedPreferenceChangeListener handler =
                (preference, value) -> Data.optShowOnlyStarred
                        .set(preference.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));
        p.registerOnSharedPreferenceChangeListener(handler);
    }
    private void updateMonthNames() {
        Resources rm = getResources();
        Globals.monthNames = rm.getStringArray(R.array.month_names);
    }
    @Override
    public void onTerminate() {
        if (dbHelper != null) dbHelper.close();
        super.onTerminate();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateMonthNames();
    }
    public static SQLiteDatabase getDatabase() {
        if (instance == null) throw new RuntimeException("Application not created yet");

        return instance.getDB();
    }
    public SQLiteDatabase getDB() {
        if (dbHelper == null) initDb();

        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("pragma case_sensitive_like=ON;");

        return db;
    }
    private synchronized void initDb() {
        if (dbHelper != null) return;

        dbHelper = new MobileLedgerDatabase(this);
    }
}
