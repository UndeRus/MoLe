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
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts",
        indices = {@Index(name = "un_account_name", unique = true, value = {"profile_id", "name"}),
                   @Index(name = "fk_account_profile", value = "profile_id")
        }, foreignKeys = {
        @ForeignKey(entity = Profile.class, parentColumns = "id", childColumns = "profile_id",
                    onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
})
public class Account {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    long id;
    @ColumnInfo(name = "profile_id")
    long profileId;
    @ColumnInfo
    int level;
    @ColumnInfo
    @NonNull
    private String name;
    @NonNull
    @ColumnInfo(name = "name_upper")
    private String nameUpper;
    @ColumnInfo(name = "parent_name")
    private String parentName;
    @ColumnInfo(defaultValue = "1")
    private boolean expanded = true;
    @ColumnInfo(name = "amounts_expanded", defaultValue = "0")
    private boolean amountsExpanded = false;
    @ColumnInfo(defaultValue = "0")
    private long generation;
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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
    @NonNull
    public String getNameUpper() {
        return nameUpper;
    }
    public void setNameUpper(@NonNull String nameUpper) {
        this.nameUpper = nameUpper;
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public String getParentName() {
        return parentName;
    }
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
    public boolean isExpanded() {
        return expanded;
    }
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    public boolean isAmountsExpanded() {
        return amountsExpanded;
    }
    public void setAmountsExpanded(boolean amountsExpanded) {
        this.amountsExpanded = amountsExpanded;
    }
    public long getGeneration() {
        return generation;
    }
    public void setGeneration(long generation) {
        this.generation = generation;
    }
    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
