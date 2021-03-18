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

import net.ktnx.mobileledger.db.Account;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class AccountDAO extends BaseDAO<Account> {
    static public List<String> unbox(List<AccountNameContainer> list) {
        ArrayList<String> result = new ArrayList<>(list.size());
        for (AccountNameContainer item : list) {
            result.add(item.name);
        }

        return result;
    }
    @Insert
    public abstract long insertSync(Account item);

    @Update
    public abstract void updateSync(Account item);

    @Delete
    public abstract void deleteSync(Account item);

    @Query("SELECT * FROM accounts")
    public abstract LiveData<List<Account>> getAll();

    //    not useful for now
//    @Transaction
//    @Query("SELECT * FROM patterns")
//    List<PatternWithAccounts> getPatternsWithAccounts();
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    public abstract LiveData<Account> getByName(long profileId, @NonNull String accountName);

    @Query("SELECT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, name_upper, rowid ")
    public abstract LiveData<List<AccountNameContainer>> lookupInProfileByName(long profileId,
                                                                               @NonNull
                                                                                       String term);

    @Query("SELECT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, name_upper, rowid ")
    public abstract List<AccountNameContainer> lookupInProfileByNameSync(long profileId,
                                                                         @NonNull String term);

    @Query("SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%%'||:term||'%%' " + "ORDER BY ordering, name_upper, rowid ")
    public abstract LiveData<List<AccountNameContainer>> lookupByName(@NonNull String term);

    @Query("SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%%'||:term||'%%' " + "ORDER BY ordering, name_upper, rowid ")
    public abstract List<AccountNameContainer> lookupByNameSync(@NonNull String term);

    static public class AccountNameContainer {
        @ColumnInfo
        public String name;
        @ColumnInfo
        public int ordering;
    }
}
