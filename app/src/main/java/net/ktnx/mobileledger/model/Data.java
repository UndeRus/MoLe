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

package net.ktnx.mobileledger.model;

import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableAtomicInteger;
import net.ktnx.mobileledger.utils.ObservableList;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Data {
    public static ObservableList<TransactionListItem> transactions = new ObservableList<>(new ArrayList<>());
    public static ObservableList<LedgerAccount> accounts = new ObservableList<>(new ArrayList<>());
    public static ObservableAtomicInteger backgroundTaskCount = new ObservableAtomicInteger(0);
    public static ObservableValue<Date> lastUpdateDate = new ObservableValue<>();
    public static ObservableValue<MobileLedgerProfile> profile = new ObservableValue<>();
    public static ObservableList<MobileLedgerProfile> profiles =
            new ObservableList<>(new ArrayList<>());
    public static ObservableValue<Boolean> optShowOnlyStarred = new ObservableValue<>();
    public static void setCurrentProfile(MobileLedgerProfile newProfile) {
        MLDB.setOption(MLDB.OPT_PROFILE_UUID, newProfile.getUuid());
        profile.set(newProfile);
    }
    public static int getProfileIndex(MobileLedgerProfile profile) {
        try (LockHolder lh = profiles.lockForReading()) {
            for (int i = 0; i < profiles.size(); i++) {
                MobileLedgerProfile p = profiles.get(i);
                if (p.equals(profile)) return i;
            }

            return -1;
        }
    }
}
