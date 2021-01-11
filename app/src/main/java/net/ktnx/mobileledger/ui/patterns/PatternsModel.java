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

package net.ktnx.mobileledger.ui.patterns;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.model.PatternEntry;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PatternsModel {
    private static final MutableLiveData<Integer> patternCount = new MutableLiveData<>(0);
    private static final Map<Integer, WeakReference<PatternEntry>> cachedEntries =
            new HashMap<Integer, WeakReference<PatternEntry>>() {};
    public synchronized static void retrievePatterns(PatternsRecyclerViewAdapter modelAdapter) {
        final ArrayList<PatternEntry> idList = new ArrayList<>();
        MLDB.queryInBackground("select id, name from patterns", null, new MLDB.CallbackHelper() {
            @Override
            public void onDone() {
                modelAdapter.setPatterns(idList);
                Logger.debug("patterns",
                        String.format(Locale.US, "Pattern list loaded from db with %d entries",
                                idList.size()));
            }
            @Override
            public boolean onRow(@NonNull Cursor cursor) {
                final PatternEntry entry = new PatternEntry(cursor.getInt(0));
                entry.setName(cursor.getString(1));
                idList.add(entry);
                return true;
            }
        });
    }
}
