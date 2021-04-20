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

package net.ktnx.mobileledger.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "options", primaryKeys = {"profile_id", "name"})
public class Option {
    public static final String OPT_LAST_SCRAPE = "last_scrape";
    @ColumnInfo(name = "profile_id")
    private long profileId;
    @NonNull
    @ColumnInfo
    private String name;
    @ColumnInfo
    private String value;
    public Option(long profileId, @NotNull String name, String value) {
        this.profileId = profileId;
        this.name = name;
        this.value = value;
    }
    public long getProfileId() {
        return profileId;
    }
    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }
    @NonNull
    public String getName() {
        return name;
    }
    public void setName(@NonNull String name) {
        this.name = name;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
