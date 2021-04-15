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

import net.ktnx.mobileledger.db.TransactionAccount;

import java.util.List;

@Dao
public abstract class TransactionAccountDAO extends BaseDAO<TransactionAccount> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertSync(TransactionAccount item);

    @Update
    public abstract void updateSync(TransactionAccount item);

    @Delete
    public abstract void deleteSync(TransactionAccount item);

    @Delete
    public abstract void deleteSync(List<TransactionAccount> items);

    @Query("SELECT * FROM transaction_accounts WHERE id = :id")
    public abstract LiveData<TransactionAccount> getById(long id);
}
