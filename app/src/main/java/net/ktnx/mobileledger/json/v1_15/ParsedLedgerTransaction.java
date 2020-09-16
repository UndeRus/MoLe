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

package net.ktnx.mobileledger.json.v1_15;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedLedgerTransaction implements net.ktnx.mobileledger.json.ParsedLedgerTransaction {
    private String tdate;
    private String tdate2 = null;
    private String tdescription;
    private String tcomment;
    private String tcode = "";
    private String tstatus = "Unmarked";
    private String tprecedingcomment = "";
    private int tindex;
    private List<ParsedPosting> tpostings;
    private List<List<String>> ttags = new ArrayList<>();
    private ParsedSourcePos tsourcepos = new ParsedSourcePos();
    public ParsedLedgerTransaction() {
    }
    public static ParsedLedgerTransaction fromLedgerTransaction(LedgerTransaction tr) {
        ParsedLedgerTransaction result = new ParsedLedgerTransaction();
        result.setTcomment(tr.getComment());
        result.setTprecedingcomment("");

        ArrayList<ParsedPosting> postings = new ArrayList<>();
        for (LedgerTransactionAccount acc : tr.getAccounts()) {
            if (!acc.getAccountName()
                    .isEmpty())
                postings.add(ParsedPosting.fromLedgerAccount(acc));
        }

        result.setTpostings(postings);
        SimpleDate transactionDate = tr.getDateIfAny();
        if (transactionDate == null) {
            transactionDate = SimpleDate.today();
        }
        result.setTdate(Globals.formatIsoDate(transactionDate));
        result.setTdate2(null);
        result.setTindex(1);
        result.setTdescription(tr.getDescription());
        return result;
    }
    public String getTcode() {
        return tcode;
    }
    public void setTcode(String tcode) {
        this.tcode = tcode;
    }
    public String getTstatus() {
        return tstatus;
    }
    public void setTstatus(String tstatus) {
        this.tstatus = tstatus;
    }
    public List<List<String>> getTtags() {
        return ttags;
    }
    public void setTtags(List<List<String>> ttags) {
        this.ttags = ttags;
    }
    public ParsedSourcePos getTsourcepos() {
        return tsourcepos;
    }
    public void setTsourcepos(ParsedSourcePos tsourcepos) {
        this.tsourcepos = tsourcepos;
    }
    public String getTprecedingcomment() {
        return tprecedingcomment;
    }
    public void setTprecedingcomment(String tprecedingcomment) {
        this.tprecedingcomment = tprecedingcomment;
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
        if (tpostings != null)
            for (ParsedPosting p : tpostings) {
                p.setPtransaction_(tindex);
            }
    }
    public List<ParsedPosting> getTpostings() {
        return tpostings;
    }
    public void setTpostings(List<ParsedPosting> tpostings) {
        this.tpostings = tpostings;
    }
    public void addPosting(ParsedPosting posting) {
        posting.setPtransaction_(tindex);
        tpostings.add(posting);
    }
    public LedgerTransaction asLedgerTransaction() throws ParseException {
        SimpleDate date = Globals.parseIsoDate(tdate);
        LedgerTransaction tr = new LedgerTransaction(tindex, date, tdescription);
        tr.setComment(Misc.trim(Misc.emptyIsNull(tcomment)));

        List<ParsedPosting> postings = tpostings;

        if (postings != null) {
            for (ParsedPosting p : postings) {
                tr.addAccount(p.asLedgerAccount());
            }
        }

        tr.markDataAsLoaded();
        return tr;
    }
}
