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

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import net.ktnx.mobileledger.utils.Misc;

import java.io.FileNotFoundException;
import java.io.IOException;

abstract class ConfigIO extends Thread {
    protected final OnErrorListener onErrorListener;
    protected ParcelFileDescriptor pfd;
    ConfigIO(Context context, Uri uri, OnErrorListener onErrorListener)
            throws FileNotFoundException {
        this.onErrorListener = onErrorListener;
        pfd = context.getContentResolver()
                     .openFileDescriptor(uri, getStreamMode());

        initStream();
    }
    abstract protected String getStreamMode();

    abstract protected void initStream();

    abstract protected void processStream() throws IOException;
    @Override
    public void run() {
        try {
            processStream();
        }
        catch (Exception e) {
            Log.e("cfg-json", "Error processing settings as JSON", e);
            if (onErrorListener != null)
                Misc.onMainThread(() -> onErrorListener.error(e));
        }
        finally {
            try {
                pfd.close();
            }
            catch (Exception e) {
                Log.e("cfg-json", "Error closing file descriptor", e);
            }
        }
    }
    protected static class Keys {
        static final String ACCOUNTS = "accounts";
        static final String AMOUNT = "amount";
        static final String AMOUNT_GROUP = "amountGroup";
        static final String API_VER = "apiVersion";
        static final String AUTH_PASS = "authPass";
        static final String AUTH_USER = "authUser";
        static final String CAN_POST = "permitPosting";
        static final String COLOUR = "colour";
        static final String COMMENT = "comment";
        static final String COMMENT_GROUP = "commentMatchGroup";
        static final String COMMODITIES = "commodities";
        static final String CURRENCY = "commodity";
        static final String CURRENCY_GROUP = "commodityGroup";
        static final String CURRENT_PROFILE = "currentProfile";
        static final String DATE_DAY = "dateDay";
        static final String DATE_DAY_GROUP = "dateDayMatchGroup";
        static final String DATE_MONTH = "dateMonth";
        static final String DATE_MONTH_GROUP = "dateMonthMatchGroup";
        static final String DATE_YEAR = "dateYear";
        static final String DATE_YEAR_GROUP = "dateYearMatchGroup";
        static final String DEFAULT_COMMODITY = "defaultCommodity";
        static final String FUTURE_DATES = "futureDates";
        static final String HAS_GAP = "hasGap";
        static final String IS_FALLBACK = "isFallback";
        static final String NAME = "name";
        static final String NAME_GROUP = "nameMatchGroup";
        static final String NEGATE_AMOUNT = "negateAmount";
        static final String POSITION = "position";
        static final String PREF_ACCOUNT = "preferredAccountsFilter";
        static final String PROFILES = "profiles";
        static final String REGEX = "regex";
        static final String SHOW_COMMENTS = "showCommentsByDefault";
        static final String SHOW_COMMODITY = "showCommodityByDefault";
        static final String TEMPLATES = "templates";
        static final String TEST_TEXT = "testText";
        static final String TRANSACTION = "description";
        static final String TRANSACTION_GROUP = "descriptionMatchGroup";
        static final String URL = "url";
        static final String USE_AUTH = "useAuth";
        static final String UUID = "uuid";
    }

    abstract static public class OnErrorListener {
        public abstract void error(Exception e);
    }
}
