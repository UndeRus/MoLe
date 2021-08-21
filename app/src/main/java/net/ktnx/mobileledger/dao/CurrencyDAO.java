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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.ktnx.mobileledger.db.Currency;

import java.util.List;

@Dao
public abstract class CurrencyDAO extends BaseDAO<Currency> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSync(Currency item);

    @Update
    abstract void updateSync(Currency item);

    @Delete
    public abstract void deleteSync(Currency item);

    @Query("SELECT * FROM currencies")
    public abstract LiveData<List<Currency>> getAll();

    @Query("SELECT * FROM currencies")
    public abstract List<Currency> getAllSync();

    @Query("SELECT * FROM currencies WHERE id = :id")
    abstract LiveData<Currency> getById(long id);

    @Query("SELECT * FROM currencies WHERE id = :id")
    public abstract Currency getByIdSync(long id);

    @Query("SELECT * FROM currencies WHERE name = :name")
    public abstract LiveData<Currency> getByName(String name);

    @Query("SELECT * FROM currencies WHERE name = :name")
    public abstract Currency getByNameSync(String name);

//    not useful for now
//    @Transaction
//    @Query("SELECT * FROM patterns")
//    List<PatternWithAccounts> getPatternsWithAccounts();
}
