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
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "currencies",
        indices = {@Index(name = "currency_name_idx", unique = true, value = "name")})
public class Currency {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull
    private String name;
    @NonNull
    private String position;
    @NonNull
    @ColumnInfo(name = "has_gap")
    private Boolean hasGap;
    public Currency(long id, @NonNull String name, @NonNull String position,
                    @NonNull Boolean hasGap) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.hasGap = hasGap;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    @NonNull
    public String getName() {
        return name;
    }
    public void setName(@NonNull String name) {
        this.name = name;
    }
    @NonNull
    public String getPosition() {
        return position;
    }
    public void setPosition(@NonNull String position) {
        this.position = position;
    }
    @NonNull
    public Boolean getHasGap() {
        return hasGap;
    }
    public void setHasGap(@NonNull Boolean hasGap) {
        this.hasGap = hasGap;
    }
}
