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

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.ktnx.mobileledger.db.Profile;

import java.util.List;

@Dao
public abstract class ProfileDAO extends BaseDAO<Profile> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSync(Profile item);

    @Transaction
    public long insertLastSync(Profile item) {
        int count = getProfileCountSync();
        item.setOrderNo(count + 1);
        return insertSync(item);
    }
    public void insertLast(Profile item, OnInsertedReceiver onInsertedReceiver) {
        AsyncTask.execute(() -> {
            long id = insertLastSync(item);
            if (onInsertedReceiver != null)
                onInsertedReceiver.onInsert(id);
        });
    }

    @Update
    abstract void updateSync(Profile item);

    @Delete
    public abstract void deleteSync(Profile item);

    @Query("select * from profiles where id = :profileId")
    public abstract Profile getByIdSync(long profileId);

    @Query("SELECT * FROM profiles WHERE id=:profileId")
    public abstract LiveData<Profile> getById(long profileId);

    @Query("SELECT * FROM profiles ORDER BY order_no")
    public abstract List<Profile> getAllOrderedSync();

    @Query("SELECT * FROM profiles ORDER BY order_no")
    public abstract LiveData<List<Profile>> getAllOrdered();

    @Query("SELECT * FROM profiles LIMIT 1")
    public abstract Profile getAnySync();

    @Query("SELECT * FROM profiles WHERE uuid=:uuid")
    public abstract LiveData<Profile> getByUuid(String uuid);

    @Query("SELECT * FROM profiles WHERE uuid=:uuid")
    public abstract Profile getByUuidSync(String uuid);

    @Query("SELECT MAX(order_no) FROM profiles")
    public abstract int getProfileCountSync();
    public void updateOrderSync(List<Profile> list) {
        if (list == null)
            list = getAllOrderedSync();
        int order = 1;
        for (Profile p : list) {
            p.setOrderNo(order++);
            updateSync(p);
        }
    }
    public void updateOrder(List<Profile> list, Runnable onDone) {
        AsyncTask.execute(() -> {
            updateOrderSync(list);
            if (onDone != null)
                onDone.run();

        });
    }
}
