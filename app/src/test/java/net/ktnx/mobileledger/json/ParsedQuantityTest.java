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

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ParsedQuantityTest {
    @Test
    public void fromString() {
        ParsedQuantity pq = new ParsedQuantity("-22");
        assertEquals(0, pq.getDecimalPlaces());
        assertEquals(-22, pq.getDecimalMantissa());

        pq = new ParsedQuantity("-123.45");
        assertEquals(2, pq.getDecimalPlaces());
        assertEquals(-12345, pq.getDecimalMantissa());
    }
}