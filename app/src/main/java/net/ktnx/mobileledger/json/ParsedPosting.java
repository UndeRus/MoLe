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

package net.ktnx.mobileledger.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.ktnx.mobileledger.model.LedgerTransactionAccount;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedPosting {
    private String paccount;
    private List<ParsedAmount> pamount;
    public ParsedPosting() {
    }
    public String getPaccount() {
        return paccount;
    }
    public void setPaccount(String paccount) {
        this.paccount = paccount;
    }
    public List<ParsedAmount> getPamount() {
        return pamount;
    }
    public void setPamount(List<ParsedAmount> pamount) {
        this.pamount = pamount;
    }
    public LedgerTransactionAccount asLedgerAccount() {
        ParsedAmount amt = pamount.get(0);
        return new LedgerTransactionAccount(paccount, amt.getAquantity().asFloat(),
                amt.getAcommodity());
    }
}
