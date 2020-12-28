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

package net.ktnx.mobileledger.json.v1_19_1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.ktnx.mobileledger.model.LedgerTransaction;

public class Gateway extends net.ktnx.mobileledger.json.Gateway {
    @Override
    public String transactionSaveRequest(LedgerTransaction ledgerTransaction)
            throws JsonProcessingException {
        net.ktnx.mobileledger.json.v1_19_1.ParsedLedgerTransaction jsonTransaction =
                net.ktnx.mobileledger.json.v1_19_1.ParsedLedgerTransaction.fromLedgerTransaction(
                        ledgerTransaction);
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer =
                mapper.writerFor(net.ktnx.mobileledger.json.v1_19_1.ParsedLedgerTransaction.class);
        return writer.writeValueAsString(jsonTransaction);
    }
}
