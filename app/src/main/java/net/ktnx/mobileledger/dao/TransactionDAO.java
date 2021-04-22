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
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import net.ktnx.mobileledger.db.Transaction;
import net.ktnx.mobileledger.db.TransactionWithAccounts;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class TransactionDAO extends BaseDAO<Transaction> {
    static public List<String> unbox(List<DescriptionContainer> list) {
        ArrayList<String> result = new ArrayList<>(list.size());
        for (DescriptionContainer item : list) {
            result.add(item.description);
        }

        return result;
    }
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertSync(Transaction item);

    @Update
    public abstract void updateSync(Transaction item);

    @Delete
    public abstract void deleteSync(Transaction item);

    @Delete
    public abstract void deleteSync(List<Transaction> items);

    @Query("SELECT * FROM transactions WHERE id = :id")
    public abstract LiveData<Transaction> getById(long id);

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    public abstract LiveData<TransactionWithAccounts> getByIdWithAccounts(long transactionId);

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    public abstract TransactionWithAccounts getByIdWithAccountsSync(long transactionId);

    @Query("SELECT DISTINCT description, CASE WHEN description_upper LIKE :term||'%%' THEN 1 " +
           "               WHEN description_upper LIKE '%%:'||:term||'%%' THEN 2 " +
           "               WHEN description_upper LIKE '%% '||:term||'%%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM description_history " +
           "WHERE description_upper LIKE '%%'||:term||'%%' " +
           "ORDER BY ordering, description_upper, rowid ")
    public abstract List<DescriptionContainer> lookupDescriptionSync(@NonNull String term);

    @androidx.room.Transaction
    @Query("SELECT * from transactions WHERE description = :description ORDER BY year desc, month" +
           " desc, day desc LIMIT 1")
    public abstract TransactionWithAccounts getFirstByDescriptionSync(@NonNull String description);

    @androidx.room.Transaction
    @Query("SELECT * from transactions tr JOIN transaction_accounts t_a ON t_a.transaction_id = " +
           "tr.id WHERE tr.description = :description AND t_a.account_name LIKE " +
           "'%'||:accountTerm||'%' ORDER BY year desc, month desc, day desc, tr.ledger_id desc " +
           "LIMIT 1")
    public abstract TransactionWithAccounts getFirstByDescriptionHavingAccountSync(
            @NonNull String description, @NonNull String accountTerm);

    @Query("SELECT * from transactions WHERE profile_id = :profileId ORDER BY " +
           "year desc, month desc, day desc, ledger_id desc")
    public abstract List<Transaction> allForProfileSync(long profileId);

    @Query("SELECT generation FROM transactions WHERE profile_id = :profileId LIMIT 1")
    protected abstract TransactionGenerationContainer getGenerationPOJOSync(long profileId);

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE profile_id = :profileId")
    public abstract List<TransactionWithAccounts> getAllWithAccountsSync(long profileId);

    @androidx.room.Transaction
    @Query("SELECT distinct(tr.id), tr.ledger_id, tr.profile_id, tr.data_hash, tr.year, tr.month," +
           " tr.day, tr.description, tr.comment, tr.generation FROM transactions tr JOIN " +
           "transaction_accounts ta ON ta.transaction_id=tr.id WHERE ta.account_name LIKE " +
           ":accountName||'%' AND ta.amount <> 0 AND tr.profile_id = :profileId ORDER BY tr.year " +
           "desc, tr.month desc, tr.day desc, tr.ledger_id desc")
    public abstract List<TransactionWithAccounts> getAllWithAccountsFilteredSync(long profileId,
                                                                                 String accountName);

    public long getGenerationSync(long profileId) {
        TransactionGenerationContainer result = getGenerationPOJOSync(profileId);

        if (result == null)
            return 0;
        return result.generation;
    }
    @Query("DELETE FROM transactions WHERE profile_id = :profileId AND generation <> " +
           ":currentGeneration")
    public abstract void purgeOldTransactionsSync(long profileId, long currentGeneration);
    static class TransactionGenerationContainer {
        @ColumnInfo
        long generation;
        public TransactionGenerationContainer(long generation) {
            this.generation = generation;
        }
    }

    static public class DescriptionContainer {
        @ColumnInfo
        public String description;
        @ColumnInfo
        public int ordering;
    }
}
