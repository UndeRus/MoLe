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

package net.ktnx.mobileledger.json.v1_23;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedLedgerAccount extends net.ktnx.mobileledger.json.ParsedLedgerAccount {
    private List<ParsedBalance> aebalance;
    private List<ParsedBalance> aibalance;
    public ParsedLedgerAccount() {
    }
    public List<ParsedBalance> getAibalance() {
        return aibalance;
    }
    public void setAibalance(List<ParsedBalance> aibalance) {
        this.aibalance = aibalance;
    }
    public List<ParsedBalance> getAebalance() {
        return aebalance;
    }
    public void setAebalance(List<ParsedBalance> aebalance) {
        this.aebalance = aebalance;
    }
    @Override
    public List<SimpleBalance> getSimpleBalance() {
        List<SimpleBalance> result = new ArrayList<SimpleBalance>();
        for (ParsedBalance b : getAibalance()) {
            result.add(new SimpleBalance(b.getAcommodity(), b.getAquantity()
                                                             .asFloat()));
        }

        return result;
    }
}
