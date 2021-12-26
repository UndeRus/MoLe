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

package net.ktnx.mobileledger.json;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.ktnx.mobileledger.model.LedgerTransaction;

abstract public class Gateway {
    public static Gateway forApiVersion(API apiVersion) {
        switch (apiVersion) {
            case v1_14:
                return new net.ktnx.mobileledger.json.v1_14.Gateway();
            case v1_15:
                return new net.ktnx.mobileledger.json.v1_15.Gateway();
            case v1_19_1:
                return new net.ktnx.mobileledger.json.v1_19_1.Gateway();
            case v1_23:
                return new net.ktnx.mobileledger.json.v1_23.Gateway();
            default:
                throw new RuntimeException(
                        "JSON API version " + apiVersion + " save implementation missing");
        }
    }
    public abstract String transactionSaveRequest(LedgerTransaction ledgerTransaction)
            throws JsonProcessingException;
}
