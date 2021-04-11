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
import androidx.room.Update;

import net.ktnx.mobileledger.db.Profile;

@Dao
public abstract class ProfileDAO extends BaseDAO<Profile> {
    @Insert
    abstract long insertSync(Profile item);

    @Update
    abstract void updateSync(Profile item);

    @Delete
    public abstract void deleteSync(Profile item);

    @Query("select * from profiles where id = :profileId")
    public abstract Profile getByIdSync(long profileId);

    @Query("SELECT * FROM profiles WHERE id=:profileId")
    public abstract LiveData<Profile> getById(long profileId);

    @Query("SELECT * FROM profiles LIMIT 1")
    public abstract Profile getAnySync();
}
