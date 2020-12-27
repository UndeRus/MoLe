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

import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.model.LedgerTransaction;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public abstract class TransactionListParser {
    public static TransactionListParser forApiVersion(SendTransactionTask.API apiVersion,
                                                      InputStream input) throws IOException {
        switch (apiVersion) {
            case v1_14:
                return new net.ktnx.mobileledger.json.v1_14.TransactionListParser(input);
            case v1_15:
                return new net.ktnx.mobileledger.json.v1_15.TransactionListParser(input);
            case v1_19_1:
                return new net.ktnx.mobileledger.json.v1_19_1.TransactionListParser(input);
            default:
                throw new RuntimeException("Unsupported version " + apiVersion.toString());
        }

    }
    abstract public LedgerTransaction nextTransaction() throws ParseException;
}
