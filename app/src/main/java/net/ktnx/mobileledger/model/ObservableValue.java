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
    public void notifyObservers(Object arg) {
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
    private class ObservableValueImpl<T> extends Observable {
        protected T value;
        public void setValue(T newValue) {
            setValue(newValue, true);
        }
        private synchronized void setValue(T newValue, boolean notify) {
            if (newValue.equals(value)) return;

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