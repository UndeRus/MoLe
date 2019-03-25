/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.utils;

import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ObservableList<T> extends Observable implements List<T> {
    private List<T> list;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public ObservableList(List<T> list) {
        this.list = list;
    }
    private void forceNotify() {
        setChanged();
        notifyObservers();
    }
    private void forceNotify(int index) {
        setChanged();
        notifyObservers(index);
    }
    public int size() {
        try (LockHolder lh = lockForReading()) {
            return list.size();
        }
    }
    public boolean isEmpty() {
        try (LockHolder lh = lockForReading()) {
            return list.isEmpty();
        }
    }
    public boolean contains(@Nullable Object o) {
        try (LockHolder lh = lockForReading()) {
            return list.contains(o);
        }
    }
    @NonNull
    public Iterator<T> iterator() {
        throw new RuntimeException("Iterators break encapsulation and ignore locking");
//        return list.iterator();
    }
    public Object[] toArray() {
        try (LockHolder lh = lockForReading()) {
            return list.toArray();
        }
    }
    public <T1> T1[] toArray(@Nullable T1[] a) {
        try (LockHolder lh = lockForReading()) {
            return list.toArray(a);
        }
    }
    public boolean add(T t) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.add(t);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    public boolean remove(@Nullable Object o) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.remove(o);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    public T removeQuietly(int index) {
        return list.remove(index);
    }
    public boolean containsAll(@NonNull Collection<?> c) {
        try (LockHolder lh = lockForReading()) {
            return list.containsAll(c);
        }
    }
    public boolean addAll(@NonNull Collection<? extends T> c) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.addAll(c);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.addAll(index, c);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    public boolean addAllQuietly(int index, Collection<? extends T> c) {
        return list.addAll(index, c);
    }
    public boolean removeAll(@NonNull Collection<?> c) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.removeAll(c);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    public boolean retainAll(@NonNull Collection<?> c) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.retainAll(c);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void replaceAll(@NonNull UnaryOperator<T> operator) {
        try (LockHolder lh = lockForWriting()) {
            list.replaceAll(operator);
            lh.downgrade();
            forceNotify();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sort(@Nullable Comparator<? super T> c) {
        try (LockHolder lh = lockForWriting()) {
            lock.writeLock().lock();
            list.sort(c);
            lh.downgrade();
            forceNotify();
        }
    }
    public void clear() {
        try (LockHolder lh = lockForWriting()) {
            boolean wasEmpty = list.isEmpty();
            list.clear();
            lh.downgrade();
            if (!wasEmpty) forceNotify();
        }
    }
    public T get(int index) {
        try (LockHolder lh = lockForReading()) {
            return list.get(index);
        }
    }
    public T set(int index, T element) {
        try (LockHolder lh = lockForWriting()) {
            T result = list.set(index, element);
            lh.downgrade();
            forceNotify();
            return result;
        }
    }
    public void add(int index, T element) {
        try (LockHolder lh = lockForWriting()) {
            list.add(index, element);
            lh.downgrade();
            forceNotify();
        }
    }
    public T remove(int index) {
        try (LockHolder lh = lockForWriting()) {
            T result = list.remove(index);
            lh.downgrade();
            forceNotify();
            return result;
        }
    }
    public int indexOf(@Nullable Object o) {
        try (LockHolder lh = lockForReading()) {
            return list.indexOf(o);
        }
    }
    public int lastIndexOf(@Nullable Object o) {
        try (LockHolder lh = lockForReading()) {
            return list.lastIndexOf(o);
        }
    }
    @NotNull
    public ListIterator<T> listIterator() {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Iterators break encapsulation and ignore locking. Write-lock first");
        return list.listIterator();
    }
    @NotNull
    public ListIterator<T> listIterator(int index) {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Iterators break encapsulation and ignore locking. Write-lock first");
        return list.listIterator(index);
    }
    @NotNull
    public List<T> subList(int fromIndex, int toIndex) {
        try (LockHolder lh = lockForReading()) {
            return list.subList(fromIndex, toIndex);
        }
    }
    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Spliterator<T> spliterator() {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Iterators break encapsulation and ignore locking. Write-lock first");
        return list.spliterator();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean removeIf(Predicate<? super T> filter) {
        try (LockHolder lh = lockForWriting()) {
            boolean result = list.removeIf(filter);
            lh.downgrade();
            if (result) forceNotify();
            return result;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Stream<T> stream() {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Iterators break encapsulation and ignore locking. Write-lock first");
        return list.stream();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Stream<T> parallelStream() {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Iterators break encapsulation and ignore locking. Write-lock first");
        return list.parallelStream();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void forEach(Consumer<? super T> action) {
        try (LockHolder lh = lockForReading()) {
            list.forEach(action);
        }
    }
    public List<T> getList() {
        if (!lock.isWriteLockedByCurrentThread()) throw new RuntimeException(
                "Direct list access breaks encapsulation and ignore locking. Write-lock first");
        return list;
    }
    public void setList(List<T> aList) {
        try (LockHolder lh = lockForWriting()) {
            list = aList;
            lh.downgrade();
            forceNotify();
        }
    }
    public void triggerItemChangedNotification(T item) {
        try (LockHolder lh = lockForReading()) {
            int index = list.indexOf(item);
            if (index == -1) {
                Log.d("ObList", "??? not sending notifications for item not found in the list");
                return;
            }
            Log.d("ObList", "Notifying item change observers");
            triggerItemChangedNotification(index);
        }
    }
    public void triggerItemChangedNotification(int index) {
        forceNotify(index);
    }
    public LockHolder lockForWriting() {
        ReentrantReadWriteLock.WriteLock wLock = lock.writeLock();
        wLock.lock();

        ReentrantReadWriteLock.ReadLock rLock = lock.readLock();
        rLock.lock();

        return new LockHolder(rLock, wLock);
    }
    public LockHolder lockForReading() {
        ReentrantReadWriteLock.ReadLock rLock = lock.readLock();
        rLock.lock();
        return new LockHolder(rLock);
    }
}