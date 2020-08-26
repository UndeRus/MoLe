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

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedLedgerAccount {
    private List<ParsedBalance> aebalance;
    private List<ParsedBalance> aibalance;
    private String aname;
    private int anumpostings;
    public ParsedLedgerAccount() {
    }
    public int getAnumpostings() {
        return anumpostings;
    }
    public void setAnumpostings(int anumpostings) {
        this.anumpostings = anumpostings;
    }
    public List<ParsedBalance> getAebalance() {
        return aebalance;
    }
    public List<ParsedBalance> getAibalance() {
        return aibalance;
    }
    public void setAebalance(List<ParsedBalance> aebalance) {
        this.aebalance = aebalance;
    }
    public void setAibalance(List<ParsedBalance> aibalance) {
        this.aibalance = aibalance;
    }
    public String getAname() {
        return aname;
    }
    public void setAname(String aname) {
        this.aname = aname;
    }

}
