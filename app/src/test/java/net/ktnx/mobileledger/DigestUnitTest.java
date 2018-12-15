/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger;

import net.ktnx.mobileledger.utils.Digest;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DigestUnitTest {
    @Test
    public void digestToHexString_isCorrect() {
        assertEquals('0', Digest.hexDigitFor(0));
        assertEquals('1', Digest.hexDigitFor(1));
        assertEquals('2', Digest.hexDigitFor(2));
        assertEquals('3', Digest.hexDigitFor(3));
        assertEquals('4', Digest.hexDigitFor(4));
        assertEquals('5', Digest.hexDigitFor(5));
        assertEquals('6', Digest.hexDigitFor(6));
        assertEquals('7', Digest.hexDigitFor(7));
        assertEquals('8', Digest.hexDigitFor(8));
        assertEquals('9', Digest.hexDigitFor(9));
        assertEquals('a', Digest.hexDigitFor(10));
        assertEquals('b', Digest.hexDigitFor(11));
        assertEquals('c', Digest.hexDigitFor(12));
        assertEquals('d', Digest.hexDigitFor(13));
        assertEquals('e', Digest.hexDigitFor(14));
        assertEquals('f', Digest.hexDigitFor(15));
    }
    @Test
    public void hexDigitsFor_isCorrect() {
        assertEquals("00", String.valueOf(Digest.hexDigitsFor(0)));
        assertEquals("10", String.valueOf(Digest.hexDigitsFor(16)));
        assertEquals("ff", String.valueOf(Digest.hexDigitsFor(255)));
        assertEquals("a0", String.valueOf(Digest.hexDigitsFor(160)));
        assertEquals("10", String.valueOf(Digest.hexDigitsFor((byte) 16)));
        assertEquals("ff", String.valueOf(Digest.hexDigitsFor((byte) -1)));
    }
    @Test(expected = ArithmeticException.class)
    public void digestToHexString_throwsOnNegative() {
        Digest.hexDigitFor(-1);
    }
    @Test(expected = ArithmeticException.class)
    public void digestToHexString_throwsOnGreaterThan15() {
        Digest.hexDigitFor(16);
    }
    @Test(expected = ArithmeticException.class)
    public void hexDigitsFor_throwsOnNegative() {
        final char[] chars = Digest.hexDigitsFor(-5);
    }
    @Test(expected = ArithmeticException.class)
    public void hexDigitsFor_throwsOnGreatherThan255() {
        final char[] chars = Digest.hexDigitsFor(256);
    }
}