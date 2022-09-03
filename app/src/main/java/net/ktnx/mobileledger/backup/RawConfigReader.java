/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.backup;

import android.util.JsonReader;
import android.util.JsonToken;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.backup.ConfigIO.Keys;
import net.ktnx.mobileledger.dao.CurrencyDAO;
import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.db.Currency;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RawConfigReader {
    private final JsonReader r;
    private List<Currency> commodities;
    private List<Profile> profiles;
    private List<TemplateWithAccounts> templates;
    private String currentProfile;
    public RawConfigReader(InputStream inputStream) {
        r = new JsonReader(new BufferedReader(new InputStreamReader(inputStream)));
    }
    public List<Currency> getCommodities() {
        return commodities;
    }
    public List<Profile> getProfiles() {
        return profiles;
    }
    public List<TemplateWithAccounts> getTemplates() {
        return templates;
    }
    public String getCurrentProfile() {
        return currentProfile;
    }
    public void readConfig() throws IOException {
        commodities = null;
        profiles = null;
        templates = null;
        currentProfile = null;
        r.beginObject();
        while (r.hasNext()) {
            String item = r.nextName();
            if (r.peek() == JsonToken.NULL) {
                r.nextNull();
                continue;
            }
            switch (item) {
                case Keys.COMMODITIES:
                    commodities = readCommodities();
                    break;
                case Keys.PROFILES:
                    profiles = readProfiles();
                    break;
                case Keys.TEMPLATES:
                    templates = readTemplates();
                    break;
                case Keys.CURRENT_PROFILE:
                    currentProfile = r.nextString();
                    break;
                default:
                    throw new RuntimeException("unexpected top-level item " + item);
            }
        }
        r.endObject();
    }
    private TemplateAccount readTemplateAccount() throws IOException {
        r.beginObject();
        TemplateAccount result = new TemplateAccount(0L, 0L, 0L);
        while (r.peek() != JsonToken.END_OBJECT) {
            String item = r.nextName();
            if (r.peek() == JsonToken.NULL) {
                r.nextNull();
                continue;
            }
            switch (item) {
                case Keys.NAME:
                    result.setAccountName(r.nextString());
                    break;
                case Keys.NAME_GROUP:
                    result.setAccountNameMatchGroup(r.nextInt());
                    break;
                case Keys.COMMENT:
                    result.setAccountComment(r.nextString());
                    break;
                case Keys.COMMENT_GROUP:
                    result.setAccountCommentMatchGroup(r.nextInt());
                    break;
                case Keys.AMOUNT:
                    result.setAmount((float) r.nextDouble());
                    break;
                case Keys.AMOUNT_GROUP:
                    result.setAmountMatchGroup(r.nextInt());
                    break;
                case Keys.NEGATE_AMOUNT:
                    result.setNegateAmount(r.nextBoolean());
                    break;
                case Keys.CURRENCY:
                    result.setCurrency(r.nextLong());
                    break;
                case Keys.CURRENCY_GROUP:
                    result.setCurrencyMatchGroup(r.nextInt());
                    break;

                default:
                    throw new IllegalStateException("Unexpected template account item: " + item);
            }
        }
        r.endObject();

        return result;
    }
    private TemplateWithAccounts readTemplate(JsonReader r) throws IOException {
        r.beginObject();
        String name = null;
        TemplateHeader t = new TemplateHeader(0L, "", "");
        List<TemplateAccount> accounts = new ArrayList<>();

        while (r.peek() != JsonToken.END_OBJECT) {
            String item = r.nextName();
            if (r.peek() == JsonToken.NULL) {
                r.nextNull();
                continue;
            }
            switch (item) {
                case Keys.UUID:
                    t.setUuid(r.nextString());
                    break;
                case Keys.NAME:
                    t.setName(r.nextString());
                    break;
                case Keys.REGEX:
                    t.setRegularExpression(r.nextString());
                    break;
                case Keys.TEST_TEXT:
                    t.setTestText(r.nextString());
                    break;
                case Keys.DATE_YEAR:
                    t.setDateYear(r.nextInt());
                    break;
                case Keys.DATE_YEAR_GROUP:
                    t.setDateYearMatchGroup(r.nextInt());
                    break;
                case Keys.DATE_MONTH:
                    t.setDateMonth(r.nextInt());
                    break;
                case Keys.DATE_MONTH_GROUP:
                    t.setDateMonthMatchGroup(r.nextInt());
                    break;
                case Keys.DATE_DAY:
                    t.setDateDay(r.nextInt());
                    break;
                case Keys.DATE_DAY_GROUP:
                    t.setDateDayMatchGroup(r.nextInt());
                    break;
                case Keys.TRANSACTION:
                    t.setTransactionDescription(r.nextString());
                    break;
                case Keys.TRANSACTION_GROUP:
                    t.setTransactionDescriptionMatchGroup(r.nextInt());
                    break;
                case Keys.COMMENT:
                    t.setTransactionComment(r.nextString());
                    break;
                case Keys.COMMENT_GROUP:
                    t.setTransactionCommentMatchGroup(r.nextInt());
                    break;
                case Keys.IS_FALLBACK:
                    t.setFallback(r.nextBoolean());
                    break;
                case Keys.ACCOUNTS:
                    r.beginArray();
                    while (r.peek() == JsonToken.BEGIN_OBJECT) {
                        accounts.add(readTemplateAccount());
                    }
                    r.endArray();
                    break;
                default:
                    throw new RuntimeException("Unknown template header item: " + item);
            }
        }
        r.endObject();

        TemplateWithAccounts result = new TemplateWithAccounts();
        result.header = t;
        result.accounts = accounts;
        return result;
    }
    private List<TemplateWithAccounts> readTemplates() throws IOException {
        List<TemplateWithAccounts> list = new ArrayList<>();

        r.beginArray();
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            list.add(readTemplate(r));
        }
        r.endArray();

        return list;
    }
    private List<Currency> readCommodities() throws IOException {
        List<Currency> list = new ArrayList<>();

        r.beginArray();
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            Currency c = new Currency();

            r.beginObject();
            while (r.peek() != JsonToken.END_OBJECT) {
                final String item = r.nextName();
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull();
                    continue;
                }
                switch (item) {
                    case Keys.NAME:
                        c.setName(r.nextString());
                        break;
                    case Keys.POSITION:
                        c.setPosition(r.nextString());
                        break;
                    case Keys.HAS_GAP:
                        c.setHasGap(r.nextBoolean());
                        break;
                    default:
                        throw new RuntimeException("Unknown commodity key: " + item);
                }
            }
            r.endObject();

            if (c.getName()
                 .isEmpty())
                throw new RuntimeException("Missing commodity name");

            list.add(c);
        }
        r.endArray();

        return list;
    }
    private List<Profile> readProfiles() throws IOException {
        List<Profile> list = new ArrayList<>();
        r.beginArray();
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            Profile p = new Profile();
            r.beginObject();
            while (r.peek() != JsonToken.END_OBJECT) {
                String item = r.nextName();
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull();
                    continue;
                }

                switch (item) {
                    case Keys.UUID:
                        p.setUuid(r.nextString());
                        break;
                    case Keys.NAME:
                        p.setName(r.nextString());
                        break;
                    case Keys.URL:
                        p.setUrl(r.nextString());
                        break;
                    case Keys.USE_AUTH:
                        p.setUseAuthentication(r.nextBoolean());
                        break;
                    case Keys.AUTH_USER:
                        p.setAuthUser(r.nextString());
                        break;
                    case Keys.AUTH_PASS:
                        p.setAuthPassword(r.nextString());
                        break;
                    case Keys.API_VER:
                        p.setApiVersion(r.nextInt());
                        break;
                    case Keys.CAN_POST:
                        p.setPermitPosting(r.nextBoolean());
                        break;
                    case Keys.DEFAULT_COMMODITY:
                        p.setDefaultCommodity(r.nextString());
                        break;
                    case Keys.SHOW_COMMODITY:
                        p.setShowCommodityByDefault(r.nextBoolean());
                        break;
                    case Keys.SHOW_COMMENTS:
                        p.setShowCommentsByDefault(r.nextBoolean());
                        break;
                    case Keys.FUTURE_DATES:
                        p.setFutureDates(r.nextInt());
                        break;
                    case Keys.PREF_ACCOUNT:
                        p.setPreferredAccountsFilter(r.nextString());
                        break;
                    case Keys.COLOUR:
                        p.setTheme(r.nextInt());
                        break;


                    default:
                        throw new IllegalStateException("Unexpected profile item: " + item);
                }
            }
            r.endObject();

            list.add(p);
        }
        r.endArray();

        return list;
    }
    public void restoreAll() {
        restoreCommodities();
        restoreProfiles();
        restoreTemplates();
        restoreCurrentProfile();
    }
    private void restoreTemplates() {
        if (templates == null)
            return;

        TemplateHeaderDAO dao = DB.get()
                                  .getTemplateDAO();

        for (TemplateWithAccounts t : templates) {
            if (dao.getTemplateWithAccountsByUuidSync(t.header.getUuid()) == null)
                dao.insertSync(t);
        }
    }
    private void restoreProfiles() {
        if (profiles == null)
            return;

        ProfileDAO dao = DB.get()
                           .getProfileDAO();

        for (Profile p : profiles) {
            if (dao.getByUuidSync(p.getUuid()) == null)
                dao.insert(p);
        }
    }
    private void restoreCommodities() {
        if (commodities == null)
            return;

        CurrencyDAO dao = DB.get()
                            .getCurrencyDAO();

        for (Currency c : commodities) {
            if (dao.getByNameSync(c.getName()) == null)
                dao.insert(c);
        }
    }
    private void restoreCurrentProfile() {
        if (currentProfile == null) {
            Logger.debug("backup", "Not restoring current profile (not present in backup)");
            return;
        }

        ProfileDAO dao = DB.get()
                           .getProfileDAO();

        Profile p = dao.getByUuidSync(currentProfile);

        if (p != null) {
            Logger.debug("backup", "Restoring current profile "+p.getName());
            Data.postCurrentProfile(p);
            App.storeStartupProfileAndTheme(p.getId(), p.getTheme());
        }
        else {
            Logger.debug("backup", "Not restoring profile "+currentProfile+": not found in DB");
        }
    }
}
