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

package net.ktnx.mobileledger.utils;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SimpleDateTest {

    @After
    public void tearDown() throws Exception {
    }
    @Test
    public void compareTo() {
        SimpleDate d1 = new SimpleDate(2020, 6, 1);
        SimpleDate d2 = new SimpleDate(2019, 7, 6);

        assertTrue(d1.compareTo(d2) > 0);
        assertTrue(d2.compareTo(d1) < 0);
        assertTrue(d1.compareTo(new SimpleDate(2020, 6, 2)) < 0);
        assertTrue(d1.compareTo(new SimpleDate(2020, 5, 2)) > 0);
        assertTrue(d1.compareTo(new SimpleDate(2019, 5, 2)) > 0);
    }
}