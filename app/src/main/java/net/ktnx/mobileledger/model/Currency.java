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

package net.ktnx.mobileledger.model;

import net.ktnx.mobileledger.utils.Misc;

public class Currency {
    private final int id;
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
        before, after, unknown, none
    }
}
