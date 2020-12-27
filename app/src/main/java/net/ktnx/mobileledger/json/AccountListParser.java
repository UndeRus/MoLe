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

package net.ktnx.mobileledger.json;

import com.fasterxml.jackson.databind.MappingIterator;

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.model.LedgerAccount;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static net.ktnx.mobileledger.utils.Logger.debug;

abstract public class AccountListParser {
    protected MappingIterator<net.ktnx.mobileledger.json.ParsedLedgerAccount> iterator;
    public static AccountListParser forApiVersion(SendTransactionTask.API version,
                                                  InputStream input) throws IOException {
        switch (version) {
            case v1_14:
                return new net.ktnx.mobileledger.json.v1_14.AccountListParser(input);
            case v1_15:
                return new net.ktnx.mobileledger.json.v1_15.AccountListParser(input);
            case v1_19_1:
                return new net.ktnx.mobileledger.json.v1_19_1.AccountListParser(input);
            default:
                throw new RuntimeException("Unsupported version " + version.toString());
        }

    }
    public abstract SendTransactionTask.API getApiVersion();
    public LedgerAccount nextAccount(RetrieveTransactionsTask task,
                                     HashMap<String, LedgerAccount> map) {
        if (!iterator.hasNext())
            return null;

        LedgerAccount next = iterator.next()
                                     .toLedgerAccount(task, map);

        if (next.getName()
                .equalsIgnoreCase("root"))
            return nextAccount(task, map);

        debug("accounts", String.format("Got account '%s' [%s]", next.getName(),
                getApiVersion().getDescription()));
        return next;
    }

}
