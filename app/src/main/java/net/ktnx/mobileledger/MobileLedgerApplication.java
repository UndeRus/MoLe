/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import static net.ktnx.mobileledger.ui.activity.SettingsActivity.PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS;

public class MobileLedgerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        updateColorValues();
        MLDB.init(this);
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        Data.optShowOnlyStarred.set(p.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));
        SharedPreferences.OnSharedPreferenceChangeListener handler = (preference, value) -> {
            Data.optShowOnlyStarred
                    .set(preference.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));
        };
        p.registerOnSharedPreferenceChangeListener(handler);
    }
    @Override
    public void onTerminate() {
        MLDB.done();
        super.onTerminate();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateColorValues();
    }
    private void updateColorValues() {
        Resources rm = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Resources.Theme theme = getTheme();
            Globals.tableRowOddBG = rm.getColor(R.color.table_row_odd_bg, theme);
            Globals.tableRowEvenBG = rm.getColor(R.color.table_row_even_bg, theme);
            Globals.primaryDark = rm.getColor(R.color.design_default_color_primary_dark, theme);
            Globals.defaultTextColor = rm.getColor(android.R.color.tab_indicator_text, theme);
        }
        else {
            Globals.tableRowOddBG = rm.getColor(R.color.table_row_odd_bg);
            Globals.tableRowEvenBG = rm.getColor(R.color.table_row_even_bg);
            Globals.primaryDark = rm.getColor(R.color.design_default_color_primary_dark);
            Globals.defaultTextColor = rm.getColor(android.R.color.tab_indicator_text);
        }
    }
}
