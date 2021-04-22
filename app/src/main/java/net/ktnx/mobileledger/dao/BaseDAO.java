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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public abstract class BaseDAO<T> {
    abstract long insertSync(T item);
    public void insert(T item) {
        AsyncTask.execute(() -> insertSync(item));
    }
    public void insert(T item, @NonNull OnInsertedReceiver receiver) {
        AsyncTask.execute(() -> {
            long id = insertSync(item);
            new Handler(Looper.getMainLooper()).post(() -> receiver.onInsert(id));
        });
    }

    abstract void updateSync(T item);
    public void update(T item) {
        AsyncTask.execute(() -> updateSync(item));
    }
    public void update(T item, @NonNull Runnable onDone) {
        AsyncTask.execute(() -> {
            updateSync(item);
            new Handler(Looper.getMainLooper()).post(onDone);
        });
    }
    abstract void deleteSync(T item);
    public void delete(T item) {
        AsyncTask.execute(() -> deleteSync(item));
    }
    public void delete(T item, @NonNull Runnable onDone) {
        AsyncTask.execute(() -> {
            deleteSync(item);
            new Handler(Looper.getMainLooper()).post(onDone);
        });
    }
    interface OnInsertedReceiver {
        void onInsert(long id);
    }
}
