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

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedPosting {
    private Void pbalanceassertion;
    private String pstatus = "Unmarked";
    private String paccount;
    private List<ParsedAmount> pamount;
    private String pdate = null;
    private String ptype = "RegularPosting";
    private String pcomment = "";
    private List<List<String>> ptags = new ArrayList<>();
    private String poriginal = null;
    public ParsedPosting() {
    }
    public String getPdate() {
        return pdate;
    }
    public void setPdate(String pdate) {
        this.pdate = pdate;
    }
    public String getPtype() {
        return ptype;
    }
    public void setPtype(String ptype) {
        this.ptype = ptype;
    }
    public String getPcomment() {
        return pcomment;
    }
    public void setPcomment(String pcomment) {
        this.pcomment = pcomment;
    }
    public List<List<String>> getPtags() {
        return ptags;
    }
    public void setPtags(List<List<String>> ptags) {
        this.ptags = ptags;
    }
    public String getPoriginal() {
        return poriginal;
    }
    public void setPoriginal(String poriginal) {
        this.poriginal = poriginal;
    }
    public String getPstatus() {
        return pstatus;
    }
    public void setPstatus(String pstatus) {
        this.pstatus = pstatus;
    }
    public Void getPbalanceassertion() {
        return pbalanceassertion;
    }
    public void setPbalanceassertion(Void pbalanceassertion) {
        this.pbalanceassertion = pbalanceassertion;
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
