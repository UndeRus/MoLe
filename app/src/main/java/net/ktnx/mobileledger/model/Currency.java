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

package net.ktnx.mobileledger.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.utils.Misc;

public class Currency {
    public static final DiffUtil.ItemCallback<Currency> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Currency>() {
                @Override
                public boolean areItemsTheSame(@NonNull Currency oldItem,
                                               @NonNull Currency newItem) {
                    return oldItem.id == newItem.id;
                }
                @Override
                public boolean areContentsTheSame(@NonNull Currency oldItem,
                                                  @NonNull Currency newItem) {
                    return oldItem.name.equals(newItem.name) &&
                           oldItem.position.equals(newItem.position) &&
                           (oldItem.hasGap == newItem.hasGap);
                }
            };
    private int id;
    private String name;
    private Position position;
    private boolean hasGap;
    public Currency(int id, String name) {
        this.id = id;
        this.name = name;
        position = Position.after;
        hasGap = true;
    }
    public Currency(int id, String name, Position position, boolean hasGap) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.hasGap = hasGap;
    }
    public Currency(MobileLedgerProfile profile, String name, Position position, boolean hasGap) {
        SQLiteDatabase db = App.getDatabase();

        try (Cursor c = db.rawQuery("select max(rowid) from currencies", null)) {
            c.moveToNext();
            this.id = c.getInt(0) + 1;
        }
        db.execSQL("insert into currencies(id, name, position, has_gap) values(?, ?, ?, ?)",
                new Object[]{this.id, name, position.toString(), hasGap});

        this.name = name;
        this.position = position;
        this.hasGap = hasGap;
    }
    public static Currency loadByName(String name) {
        MobileLedgerProfile profile = Data.getProfile();
        return profile.loadCurrencyByName(name);
    }
    static public boolean equal(Currency left, Currency right) {
        if (left == null) {
            return right == null;
        }
        else
            return left.equals(right);
    }
    static public boolean equal(Currency left, String right) {
        right = Misc.emptyIsNull(right);
        if (left == null) {
            return right == null;
        }
        else {
            String leftName = Misc.emptyIsNull(left.getName());
            if (leftName == null) {
                return right == null;
            }
            else
                return leftName.equals(right);
        }
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Position getPosition() {
        return position;
    }
    public void setPosition(Position position) {
        this.position = position;
    }
    public boolean hasGap() {
        return hasGap;
    }
    public void setHasGap(boolean hasGap) {
        this.hasGap = hasGap;
    }
    public enum Position {
        before(-1), after(1), unknown(0), none(-2);
        private int value;
        Position(int value) {
            this.value = value;
        }
        static Position valueOf(int value) {
            switch (value) {
                case -1:
                    return before;
                case +1:
                    return after;
                case 0:
                    return unknown;
                case -2:
                    return none;
                default:
                    throw new IllegalStateException(String.format("Unexpected value (%d)", value));
            }
        }
    }
}
