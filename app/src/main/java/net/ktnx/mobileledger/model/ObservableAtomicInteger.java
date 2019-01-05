/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.model;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

public class ObservableAtomicInteger extends Observable {
    private AtomicInteger holder;
    ObservableAtomicInteger() {
        super();
        holder = new AtomicInteger();
    }
    ObservableAtomicInteger(int initialValue) {
        this();
        holder.set(initialValue);
    }
    public int get() {
        return holder.get();
    }
    public void set(int newValue) {
//        Log.d("atomic", "set");
        holder.set(newValue);
        forceNotify();
    }
    private void forceNotify() {
        setChanged();
//        Log.d("atomic", String.format("notifying %d observers", countObservers()));
        notifyObservers();
    }
//    public void lazySet(int newValue) {
//        holder.lazySet(newValue);
//        forceNotify();
//    }
    public int getAndSet(int newValue) {
        int result = holder.getAndSet(newValue);
        forceNotify();
        return result;
    }
    public boolean compareAndSet(int expect, int update) {
        boolean result = holder.compareAndSet(expect, update);
        if (result) forceNotify();
        return result;
    }
//    public boolean weakCompareAndSet(int expect, int update) {
//        boolean result = holder.weakCompareAndSet(expect, update);
//        if (result) forceNotify();
//        return result;
//    }
    public int getAndIncrement() {
        int result = holder.getAndIncrement();
        forceNotify();
        return result;
    }
    public int getAndDecrement() {
        int result = holder.getAndDecrement();
        forceNotify();
        return result;
    }
    public int getAndAdd(int delta) {
        int result = holder.getAndAdd(delta);
        forceNotify();
        return result;
    }
    public int incrementAndGet() {
//        Log.d("atomic", "incrementAndGet");
        int result = holder.incrementAndGet();
        forceNotify();
        return result;
    }
    public int decrementAndGet() {
//        Log.d("atomic", "decrementAndGet");
        int result = holder.decrementAndGet();
        forceNotify();
        return result;
    }
    public int addAndGet(int delta) {
        int result = holder.addAndGet(delta);
        forceNotify();
        return result;
    }
    @RequiresApi(Build.VERSION_CODES.N)
    public int getAndUpdate(IntUnaryOperator updateFunction) {
        int result = holder.getAndUpdate(updateFunction);
        forceNotify();
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public int updateAndGet(IntUnaryOperator updateFunction) {
        int result = holder.updateAndGet(updateFunction);
        forceNotify();
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int result = holder.getAndAccumulate(x, accumulatorFunction);
        forceNotify();
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int result = holder.accumulateAndGet(x, accumulatorFunction);
        forceNotify();
        return result;
    }
    public int intValue() {
        return holder.intValue();
    }
    public long longValue() {
        return holder.longValue();
    }
    public float floatValue() {
        return holder.floatValue();
    }
    public double doubleValue() {
        return holder.doubleValue();
    }
    public byte byteValue() {
        return holder.byteValue();
    }
    public short shortValue() {
        return holder.shortValue();
    }
}
