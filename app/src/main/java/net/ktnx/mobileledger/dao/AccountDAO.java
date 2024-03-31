/*
 * Copyright © 2024 Damyan Ivanov.
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
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.AccountValue;
import net.ktnx.mobileledger.db.AccountWithAmounts;
import net.ktnx.mobileledger.db.DB;

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertSync(Account item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertSync(List<Account> items);

    @Transaction
    public void insertSync(@NonNull AccountWithAmounts accountWithAmounts) {
        final AccountValueDAO valueDAO = DB.get()
                                           .getAccountValueDAO();
        Account account = accountWithAmounts.account;
        account.setId(insertSync(account));
        for (AccountValue value : accountWithAmounts.amounts) {
            value.setAccountId(account.getId());
            value.setGeneration(account.getGeneration());
            value.setId(valueDAO.insertSync(value));
        }
    }
    @Update
    public abstract void updateSync(Account item);

    @Delete
    public abstract void deleteSync(Account item);

    @Delete
    public abstract void deleteSync(List<Account> items);

    @Query("DELETE FROM accounts")
    public abstract void deleteAllSync();

    @Query("SELECT * FROM accounts WHERE profile_id=:profileId AND IIF(:includeZeroBalances=1, 1," +
           " (EXISTS(SELECT 1 FROM account_values av WHERE av.account_id=accounts.id AND av.value" +
           " <> 0) OR EXISTS(SELECT 1 FROM accounts a WHERE a.parent_name = accounts.name))) " +
           "ORDER BY name")
    public abstract LiveData<List<Account>> getAll(long profileId, boolean includeZeroBalances);

    @Transaction
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND IIF(:includeZeroBalances=1, " +
           "1, (EXISTS(SELECT 1 FROM account_values av WHERE av.account_id=accounts.id AND av" +
           ".value <> 0) OR EXISTS(SELECT 1 FROM accounts a WHERE a.parent_name = accounts.name))" +
           ") ORDER BY name")
    public abstract LiveData<List<AccountWithAmounts>> getAllWithAmounts(long profileId,
                                                                         boolean includeZeroBalances);

    @Query("SELECT * FROM accounts WHERE id=:id")
    public abstract Account getByIdSync(long id);

    //    not useful for now
//    @Transaction
//    @Query("SELECT * FROM patterns")
//    List<PatternWithAccounts> getPatternsWithAccounts();
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    public abstract LiveData<Account> getByName(long profileId, @NonNull String accountName);

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    public abstract Account getByNameSync(long profileId, @NonNull String accountName);

    @Transaction
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    public abstract LiveData<AccountWithAmounts> getByNameWithAmounts(long profileId,
                                                                      @NonNull String accountName);

    @Query("SELECT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, name_upper, rowid ")
    public abstract LiveData<List<AccountNameContainer>> lookupNamesInProfileByName(long profileId,
                                                                                    @NonNull
                                                                                            String term);

    @Query("SELECT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, name_upper, rowid ")
    public abstract List<AccountNameContainer> lookupNamesInProfileByNameSync(long profileId,
                                                                              @NonNull String term);

    @Transaction
    @Query("SELECT * FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY  CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END, name_upper, rowid ")
    public abstract List<AccountWithAmounts> lookupWithAmountsInProfileByNameSync(long profileId,
                                                                                  @NonNull String term);

    @Query("SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%%'||:term||'%%' " + "ORDER BY ordering, name_upper, rowid ")
    public abstract LiveData<List<AccountNameContainer>> lookupNamesByName(@NonNull String term);

    @Query("SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN name_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN name_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%%'||:term||'%%' " + "ORDER BY ordering, name_upper, rowid ")
    public abstract List<AccountNameContainer> lookupNamesByNameSync(@NonNull String term);

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId")
    public abstract List<Account> allForProfileSync(long profileId);

    @Query("SELECT generation FROM accounts WHERE profile_id = :profileId LIMIT 1")
    protected abstract AccountGenerationContainer getGenerationPOJOSync(long profileId);
    public long getGenerationSync(long profileId) {
        AccountGenerationContainer result = getGenerationPOJOSync(profileId);

        if (result == null)
            return 0;
        return result.generation;
    }
    @Query("DELETE FROM accounts WHERE profile_id = :profileId AND generation <> " +
           ":currentGeneration")
    public abstract void purgeOldAccountsSync(long profileId, long currentGeneration);

    @Query("DELETE FROM account_values WHERE EXISTS (SELECT 1 FROM accounts a WHERE a" +
           ".id=account_values.account_id AND a.profile_id=:profileId) AND generation <> " +
           ":currentGeneration")
    public abstract void purgeOldAccountValuesSync(long profileId, long currentGeneration);
    @Transaction
    public void storeAccountsSync(List<AccountWithAmounts> accounts, long profileId) {
        long generation = getGenerationSync(profileId) + 1;

        for (AccountWithAmounts rec : accounts) {
            rec.account.setGeneration(generation);
            rec.account.setProfileId(profileId);
            insertSync(rec);
        }
        purgeOldAccountsSync(profileId, generation);
        purgeOldAccountValuesSync(profileId, generation);
    }

    static public class AccountNameContainer {
        @ColumnInfo
        public String name;
        @ColumnInfo
        public int ordering;
    }

    static class AccountGenerationContainer {
        @ColumnInfo
        long generation;
        public AccountGenerationContainer(long generation) {
            this.generation = generation;
        }
    }
}
