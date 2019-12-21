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

package net.ktnx.mobileledger.json.v1_14;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedQuantity {
    private long decimalMantissa;
    private int decimalPlaces;
    public ParsedQuantity() {
    }
    public ParsedQuantity(String input) {
        parseString(input);
    }
    public long getDecimalMantissa() {
        return decimalMantissa;
    }
    public void setDecimalMantissa(long decimalMantissa) {
        this.decimalMantissa = decimalMantissa;
    }
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
    public float asFloat() {
        return (float) (decimalMantissa * Math.pow(10, -decimalPlaces));
    }
    public void parseString(String input) {
        int pointPos = input.indexOf('.');
        if (pointPos >= 0) {
            String integral = input.replace(".", "");
            decimalMantissa = Long.valueOf(integral);
            decimalPlaces = input.length() - pointPos - 1;
        }
        else {
            decimalMantissa = Long.valueOf(input);
            decimalPlaces = 0;
        }
    }
}
