/*
 * Copyright © 2020 Damyan Ivanov.
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

import java.util.Observable;
import java.util.Observer;

public class ObservableValue<T> {
    private final ObservableValueImpl<T> impl = new ObservableValueImpl<>();
    public ObservableValue() {}
    public ObservableValue(T initialValue) {
        impl.setValue(initialValue, false);
    }
    public void set(T newValue) {
        impl.setValue(newValue);
    }
    public T get() {
        return impl.getValue();
    }
    public void addObserver(Observer o) {
        impl.addObserver(o);
    }
    public void deleteObserver(Observer o) {
        impl.deleteObserver(o);
    }
    public void notifyObservers() {
        impl.notifyObservers();
    }
    public void notifyObservers(T arg) {
        impl.notifyObservers(arg);
    }
    public void deleteObservers() {
        impl.deleteObservers();
    }
    public boolean hasChanged() {
        return impl.hasChanged();
    }
    public int countObservers() {
        return impl.countObservers();
    }
    public void forceNotifyObservers() {
        impl.setChanged();
        impl.notifyObservers();
    }
    private static class ObservableValueImpl<T> extends Observable {
        protected T value;
        public void setValue(T newValue) {
            setValue(newValue, true);
        }
        protected void setChanged() {
            super.setChanged();
        }
        private synchronized void setValue(T newValue, boolean notify) {
            if ((newValue == null) && (value == null))
                return;

            if ((newValue != null) && newValue.equals(value)) return;

            T oldValue = value;
            value = newValue;
            setChanged();
            if (notify) notifyObservers(oldValue);
        }
        public T getValue() {
            return value;
        }
    }
}