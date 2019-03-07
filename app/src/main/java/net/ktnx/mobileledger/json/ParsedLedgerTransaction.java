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

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.utils.Globals;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedLedgerTransaction {
    private String tdate, tdate2, tdescription, tcomment;
    private int tindex;
    private List<ParsedPosting> tpostings;
    public ParsedLedgerTransaction() {
    }
    public String getTdate() {
        return tdate;
    }
    public void setTdate(String tdate) {
        this.tdate = tdate;
    }
    public String getTdate2() {
        return tdate2;
    }
    public void setTdate2(String tdate2) {
        this.tdate2 = tdate2;
    }
    public String getTdescription() {
        return tdescription;
    }
    public void setTdescription(String tdescription) {
        this.tdescription = tdescription;
    }
    public String getTcomment() {
        return tcomment;
    }
    public void setTcomment(String tcomment) {
        this.tcomment = tcomment;
    }
    public int getTindex() {
        return tindex;
    }
    public void setTindex(int tindex) {
        this.tindex = tindex;
    }
    public List<ParsedPosting> getTpostings() {
        return tpostings;
    }
    public void setTpostings(List<ParsedPosting> tpostings) {
        this.tpostings = tpostings;
    }
    public LedgerTransaction asLedgerTransaction() throws ParseException {
        Date date = Globals.parseIsoDate(tdate);
        LedgerTransaction tr = new LedgerTransaction(tindex, date, tdescription);

        List<ParsedPosting> postings = tpostings;

        if (postings != null) {
            for (ParsedPosting p : postings) {
                tr.addAccount(p.asLedgerAccount());
            }
        }
        return tr;
    }
}
