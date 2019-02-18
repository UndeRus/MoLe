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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ObservableList<T> extends Observable {
    private List<T> list;
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
        return list.size();
    }
    public boolean isEmpty() {
        return list.isEmpty();
    }
    public boolean contains(@Nullable Object o) {
        return list.contains(o);
    }
    public Iterator<T> iterator() {
        return list.iterator();
    }
    public Object[] toArray() {
        return list.toArray();
    }
    public <T1> T1[] toArray(@Nullable T1[] a) {
        return list.toArray(a);
    }
    public boolean add(T t) {
        boolean result = list.add(t);
        if (result) forceNotify();
        return result;
    }
    public boolean remove(@Nullable Object o) {
        boolean result = list.remove(o);
        if (result) forceNotify();
        return result;
    }
    public boolean containsAll(@NonNull Collection<?> c) {
        return list.containsAll(c);
    }
    public boolean addAll(@NonNull Collection<? extends T> c) {
        boolean result = list.addAll(c);
        if (result) forceNotify();
        return result;
    }
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        boolean result = list.addAll(index, c);
        if (result) forceNotify();
        return result;
    }
    public boolean removeAll(@NonNull Collection<?> c) {
        boolean result = list.removeAll(c);
        if (result) forceNotify();
        return result;
    }
    public boolean retainAll(@NonNull Collection<?> c) {
        boolean result = list.retainAll(c);
        if (result) forceNotify();
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void replaceAll(@NonNull UnaryOperator<T> operator) {
        list.replaceAll(operator);
        forceNotify();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sort(@Nullable Comparator<? super T> c) {
        list.sort(c);
        forceNotify();
    }
    public void clear() {
        boolean wasEmpty = list.isEmpty();
        list.clear();
        if (!wasEmpty) forceNotify();
    }
    public T get(int index) {
        return list.get(index);
    }
    public T set(int index, T element) {
        T result = list.set(index, element);
        forceNotify();
        return result;
    }
    public void add(int index, T element) {
        list.add(index, element);
        forceNotify();
    }
    public T remove(int index) {
        T result = list.remove(index);
        forceNotify();
        return result;
    }
    public int indexOf(@Nullable Object o) {
        return list.indexOf(o);
    }
    public int lastIndexOf(@Nullable Object o) {
        return list.lastIndexOf(o);
    }
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean removeIf(Predicate<? super T> filter) {
        boolean result = list.removeIf(filter);
        if (result) forceNotify();
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Stream<T> stream() {
        return list.stream();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Stream<T> parallelStream() {
        return list.parallelStream();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void forEach(Consumer<? super T> action) {
        list.forEach(action);
    }
    public List<T> getList() {
        return list;
    }
    public void setList(List<T> aList) {
        list = aList;
        forceNotify();
    }
    public void triggerItemChangedNotification(T item) {
        int index = list.indexOf(item);
        if (index == -1) {
            Log.d("ObList", "??? not sending notifications for item not found in the list");
            return;
        }
        Log.d("ObList", "Notifying item change observers");
        triggerItemChangedNotification(index);
    }
    public void triggerItemChangedNotification(int index) {
        forceNotify(index);
    }
}