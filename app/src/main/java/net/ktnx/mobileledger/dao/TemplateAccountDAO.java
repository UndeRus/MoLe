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
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import net.ktnx.mobileledger.db.TemplateAccount;

import java.util.List;

@Dao
public interface TemplateAccountDAO {
    @Insert
    Long insertSync(TemplateAccount item);

    @Update
    void updateSync(TemplateAccount... items);

    @Delete
    void deleteSync(TemplateAccount item);

    @Query("SELECT * FROM template_accounts WHERE template_id=:template_id")
    LiveData<List<TemplateAccount>> getTemplateAccounts(Long template_id);

    @Query("SELECT * FROM template_accounts WHERE id = :id")
    LiveData<TemplateAccount> getPatternAccountById(Long id);

    @Query("UPDATE template_accounts set position=-1 WHERE template_id=:templateId")
    void prepareForSave(@NonNull Long templateId);

    @Query("DELETE FROM template_accounts WHERE position=-1 AND template_id=:templateId")
    void finishSave(@NonNull Long templateId);
}
