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

package net.ktnx.mobileledger.json.v1_15;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedStyle {
    private int asprecision;
    private char asdecimalpoint;
    private char ascommodityside;
    private int digitgroups;
    private boolean ascommodityspaced;
    public ParsedStyle() {
    }
    public int getAsprecision() {
        return asprecision;
    }
    public void setAsprecision(int asprecision) {
        this.asprecision = asprecision;
    }
    public char getAsdecimalpoint() {
        return asdecimalpoint;
    }
    public void setAsdecimalpoint(char asdecimalpoint) {
        this.asdecimalpoint = asdecimalpoint;
    }
    public char getAscommodityside() {
        return ascommodityside;
    }
    public void setAscommodityside(char ascommodityside) {
        this.ascommodityside = ascommodityside;
    }
    public int getDigitgroups() {
        return digitgroups;
    }
    public void setDigitgroups(int digitgroups) {
        this.digitgroups = digitgroups;
    }
    public boolean isAscommodityspaced() {
        return ascommodityspaced;
    }
    public void setAscommodityspaced(boolean ascommodityspaced) {
        this.ascommodityspaced = ascommodityspaced;
    }
}
