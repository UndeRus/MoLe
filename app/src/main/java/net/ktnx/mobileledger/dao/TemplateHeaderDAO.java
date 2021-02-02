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
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TemplateWithAccounts;

import java.util.List;

@Dao
public interface TemplateHeaderDAO {
    @Insert()
    long insert(TemplateHeader item);

    @Update
    void update(TemplateHeader... items);

    @Delete
    void delete(TemplateHeader item);

    @Query("SELECT * FROM templates ORDER BY UPPER(name)")
    LiveData<List<TemplateHeader>> getTemplates();

    @Query("SELECT * FROM templates WHERE id = :id")
    LiveData<TemplateHeader> getTemplate(Long id);

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    LiveData<TemplateWithAccounts> getTemplateWithAccounts(Long id);
}
