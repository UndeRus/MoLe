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
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.utils.Misc;

import java.util.List;

@Dao
public abstract class TemplateHeaderDAO {
    @Insert()
    public abstract long insertSync(TemplateHeader item);

    public void insertAsync(@NonNull TemplateHeader item, @Nullable Runnable callback) {
        BaseDAO.runAsync(() -> {
            insertSync(item);
            if (callback != null)
                Misc.onMainThread(callback);
        });
    }

    @Update
    public abstract void updateSync(TemplateHeader... items);

    @Delete
    public abstract void deleteSync(TemplateHeader item);

    public void deleteAsync(@NonNull TemplateHeader item, @NonNull Runnable callback) {
        BaseDAO.runAsync(() -> {
            deleteSync(item);
            Misc.onMainThread(callback);
        });
    }

    @Query("SELECT * FROM templates ORDER BY is_fallback, UPPER(name)")
    public abstract LiveData<List<TemplateHeader>> getTemplates();

    @Query("SELECT * FROM templates WHERE id = :id")
    public abstract LiveData<TemplateHeader> getTemplate(Long id);

    @Query("SELECT * FROM templates WHERE id = :id")
    public abstract TemplateHeader getTemplateSync(Long id);

    public void getTemplateAsync(@NonNull Long id,
                                 @NonNull AsyncResultCallback<TemplateHeader> callback) {
        LiveData<TemplateHeader> resultReceiver = getTemplate(id);
        resultReceiver.observeForever(new Observer<TemplateHeader>() {
            @Override
            public void onChanged(TemplateHeader h) {
                if (h == null)
                    return;

                resultReceiver.removeObserver(this);
                callback.onResult(h);
            }
        });
    }

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    public abstract LiveData<TemplateWithAccounts> getTemplateWithAccounts(@NonNull Long id);

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    public abstract TemplateWithAccounts getTemplateWithAccountsSync(@NonNull Long id);

    @Transaction
    @Query("SELECT * FROM templates WHERE uuid = :uuid")
    public abstract TemplateWithAccounts getTemplateWithAccountsByUuidSync(String uuid);

    @Transaction
    @Query("SELECT * FROM templates")
    public abstract List<TemplateWithAccounts> getAllTemplatesWithAccountsSync();

    @Transaction
    public void insertSync(TemplateWithAccounts templateWithAccounts) {
        long template_id = insertSync(templateWithAccounts.header);
        for (TemplateAccount acc : templateWithAccounts.accounts) {
            acc.setTemplateId(template_id);
            DB.get()
              .getTemplateAccountDAO()
              .insertSync(acc);
        }
    }

    public void getTemplateWithAccountsAsync(@NonNull Long id, @NonNull
            AsyncResultCallback<TemplateWithAccounts> callback) {
        LiveData<TemplateWithAccounts> resultReceiver = getTemplateWithAccounts(id);
        resultReceiver.observeForever(new Observer<TemplateWithAccounts>() {
            @Override
            public void onChanged(TemplateWithAccounts result) {
                if (result == null)
                    return;

                resultReceiver.removeObserver(this);
                callback.onResult(result);
            }
        });
    }
    public void insertAsync(@NonNull TemplateWithAccounts item, @Nullable Runnable callback) {
        BaseDAO.runAsync(() -> {
            insertSync(item);
            if (callback != null)
                Misc.onMainThread(callback);
        });
    }
    public void duplicateTemplateWitAccounts(@NonNull Long id, @Nullable
            AsyncResultCallback<TemplateWithAccounts> callback) {
        BaseDAO.runAsync(() -> {
            TemplateWithAccounts src = getTemplateWithAccountsSync(id);
            TemplateWithAccounts dup = src.createDuplicate();
            dup.header.setName(dup.header.getName());
            dup.header.setId(insertSync(dup.header));
            TemplateAccountDAO accDao = DB.get()
                                          .getTemplateAccountDAO();
            for (TemplateAccount dupAcc : dup.accounts) {
                dupAcc.setTemplateId(dup.header.getId());
                dupAcc.setId(accDao.insertSync(dupAcc));
            }
            if (callback != null)
                Misc.onMainThread(() -> callback.onResult(dup));
        });
    }

}
