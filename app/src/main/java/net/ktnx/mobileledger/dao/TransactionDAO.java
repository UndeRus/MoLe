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

import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Transaction;
import net.ktnx.mobileledger.db.TransactionAccount;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    @Query("SELECT tr.id, tr.profile_id, tr.ledger_id, tr.description, tr.data_hash, tr.comment, " +
           "tr.year, tr.month, tr.day, tr.generation from transactions tr JOIN " +
           "transaction_accounts t_a ON t_a.transaction_id = tr.id WHERE tr.description = " +
           ":description AND t_a.account_name LIKE '%'||:accountTerm||'%' ORDER BY year desc, " +
           "month desc, day desc, tr.ledger_id desc LIMIT 1")
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
    public abstract int purgeOldTransactionsSync(long profileId, long currentGeneration);

    @Query("DELETE FROM transaction_accounts WHERE EXISTS (SELECT 1 FROM transactions tr WHERE tr" +
           ".id=transaction_accounts.transaction_id AND tr.profile_id=:profileId) AND generation " +
           "<> :currentGeneration")
    public abstract int purgeOldTransactionAccountsSync(long profileId, long currentGeneration);
    public void storeTransactionsSync(List<TransactionWithAccounts> list, long profileId) {
        long generation = getGenerationSync(profileId) + 1;

        for (TransactionWithAccounts tr : list) {
            tr.transaction.setGeneration(generation);
            tr.transaction.setProfileId(profileId);

            storeSync(tr);
        }

        Logger.debug("Transaction", "Purging old transactions");
        int removed = purgeOldTransactionsSync(profileId, generation);
        Logger.debug("Transaction", String.format(Locale.ROOT, "Purged %d transactions", removed));

        removed = purgeOldTransactionAccountsSync(profileId, generation);
        Logger.debug("Transaction",
                String.format(Locale.ROOT, "Purged %d transaction accounts", removed));
    }
    private void storeSync(TransactionWithAccounts rec) {
        TransactionAccountDAO trAccDao = DB.get()
                                           .getTransactionAccountDAO();

        Transaction transaction = rec.transaction;
        Transaction existing = getByLedgerId(transaction.getProfileId(), transaction.getLedgerId());
        if (existing != null) {
            existing.copyDataFrom(transaction);
            updateSync(existing);
            transaction = existing;
        }
        else
            transaction.setId(insertSync(transaction));

        for (TransactionAccount trAcc : rec.accounts) {
            trAcc.setTransactionId(transaction.getId());
            trAcc.setGeneration(transaction.getGeneration());
            TransactionAccount existingAcc =
                    trAccDao.getByOrderNoSync(trAcc.getTransactionId(), trAcc.getOrderNo());
            if (existingAcc != null) {
                existingAcc.copyDataFrom(trAcc);
                trAccDao.updateSync(trAcc);
                trAcc = existingAcc;
            }
            else
                trAcc.setId(trAccDao.insertSync(trAcc));
        }
    }
    @Query("SELECT * FROM transactions where profile_id = :profileId AND ledger_id = :ledgerId")
    public abstract Transaction getByLedgerId(long profileId, long ledgerId);
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
