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

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedBalance {
    private ParsedQuantity aquantity;
    private String acommodity;
    private ParsedStyle astyle;
    public ParsedBalance() {
    }
    public ParsedQuantity getAquantity() {
        return aquantity;
    }
    public void setAquantity(ParsedQuantity aquantity) {
        this.aquantity = aquantity;
    }
    @NonNull
    public String getAcommodity() {
        return (acommodity == null) ? "" : acommodity;
    }
    public void setAcommodity(String acommodity) {
        this.acommodity = acommodity;
    }
    public ParsedStyle getAstyle() {
        return astyle;
    }
    public void setAstyle(ParsedStyle astyle) {
        this.astyle = astyle;
    }
}
