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

@Entity(tableName = "description_history", primaryKeys = {"description"})
public class DescriptionHistory {
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    @NonNull
    private String description;
    @ColumnInfo(name = "description_upper")
    @NonNull
    private String descriptionUpper;
    @ColumnInfo(defaultValue = "0")
    private int generation = 0;
    @NonNull
    public String getDescription() {
        return description;
    }
    public void setDescription(@NonNull String description) {
        this.description = description;
    }
    @NonNull
    public String getDescriptionUpper() {
        return descriptionUpper;
    }
    public void setDescriptionUpper(@NonNull String descriptionUpper) {
        this.descriptionUpper = descriptionUpper;
    }
    public int getGeneration() {
        return generation;
    }
    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
