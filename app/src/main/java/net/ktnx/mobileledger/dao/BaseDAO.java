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

import net.ktnx.mobileledger.utils.Misc;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseDAO<T> {
    private final static Executor asyncRunner = Executors.newSingleThreadExecutor();
    public static void runAsync(Runnable runnable) {
        asyncRunner.execute(runnable);
    }
    abstract long insertSync(T item);
    public void insert(T item) {
        asyncRunner.execute(() -> insertSync(item));
    }
    public void insert(T item, @NonNull OnInsertedReceiver receiver) {
        asyncRunner.execute(() -> {
            long id = insertSync(item);
            Misc.onMainThread(() -> receiver.onInsert(id));
        });
    }

    abstract void updateSync(T item);
    public void update(T item) {
        asyncRunner.execute(() -> updateSync(item));
    }
    public void update(T item, @NonNull Runnable onDone) {
        asyncRunner.execute(() -> {
            updateSync(item);
            Misc.onMainThread(onDone);
        });
    }
    abstract void deleteSync(T item);
    public void delete(T item) {
        asyncRunner.execute(() -> deleteSync(item));
    }
    public void delete(T item, @NonNull Runnable onDone) {
        asyncRunner.execute(() -> {
            deleteSync(item);
            Misc.onMainThread(onDone);
        });
    }
    interface OnInsertedReceiver {
        void onInsert(long id);
    }
}
