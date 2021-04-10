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

package net.ktnx.mobileledger.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import net.ktnx.mobileledger.db.DescriptionHistory;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class DescriptionHistoryDAO extends BaseDAO<DescriptionHistory> {
    static public List<String> unbox(List<DescriptionContainer> list) {
        ArrayList<String> result = new ArrayList<>(list.size());
        for (DescriptionContainer item : list) {
            result.add(item.description);
        }

        return result;
    }
    @Insert
    public abstract long insertSync(DescriptionHistory item);

    @Update
    public abstract void updateSync(DescriptionHistory item);

    @Delete
    public abstract void deleteSync(DescriptionHistory item);

    @Delete
    public abstract void deleteSync(List<DescriptionHistory> items);

    @Query("DELETE FROM description_history where not exists (select 1 from transactions tr where" +
           " upper(tr.description)=description_history.description_upper)")
    public abstract void sweepSync();

    @Query("SELECT * FROM description_history")
    public abstract LiveData<List<DescriptionHistory>> getAll();

    @Query("SELECT DISTINCT description, CASE WHEN description_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN description_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN description_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM description_history " +
           "WHERE description_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, description_upper, rowid ")
    public abstract List<DescriptionContainer> lookupSync(@NonNull String term);

    static public class DescriptionContainer {
        @ColumnInfo
        public String description;
        @ColumnInfo
        public int ordering;
    }
}
