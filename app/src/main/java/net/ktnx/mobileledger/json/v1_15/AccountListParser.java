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

package net.ktnx.mobileledger.json.v1_15;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountListParser {

    private final MappingIterator<ParsedLedgerAccount> iter;

    public AccountListParser(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(ParsedLedgerAccount.class);

        iter = reader.readValues(input);
    }
    public ParsedLedgerAccount nextAccount() {
        if (!iter.hasNext()) return null;

        ParsedLedgerAccount next = iter.next();

        if (next.getAname().equalsIgnoreCase("root")) return nextAccount();

        debug("accounts", String.format("Got account '%s'", next.getAname()));
        return next;
    }
}
