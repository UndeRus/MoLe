/*
 * Copyright © 2018 Damyan Ivanov.
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

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {
    private MessageDigest digest;
    public Digest(String type) throws NoSuchAlgorithmException {
        digest = MessageDigest.getInstance(type);
    }
    public static char[] hexDigitsFor(byte x) {
        return hexDigitsFor(((x < 0) ? 256 + x : x));
    }
    public static char[] hexDigitsFor(int x) {
        if ((x < 0) || (x > 255)) throw new ArithmeticException(
                String.format("Hex digits must be between 0 and 255 (argument: %d)", x));
        char[] result = new char[]{0, 0};
        result[0] = hexDigitFor(x / 16);
        result[1] = hexDigitFor(x % 16);

        return result;
    }
    public static char hexDigitFor(int x) {
        if (x < 0) throw new ArithmeticException(
                String.format("Hex digits can't be negative (argument: %d)", x));
        if (x < 10) return (char) ('0' + x);
        if (x < 16) return (char) ('a' + x - 10);
        throw new ArithmeticException(
                String.format("Hex digits can't be greater than 15 (argument: %d)", x));
    }
    public void update(byte input) {
        digest.update(input);
    }
    public void update(byte[] input, int offset, int len) {
        digest.update(input, offset, len);
    }
    public void update(byte[] input) {
        digest.update(input);
    }
    public void update(ByteBuffer input) {
        digest.update(input);
    }
    public byte[] digest() {
        return digest.digest();
    }
    public int digest(byte[] buf, int offset, int len) throws DigestException {
        return digest.digest(buf, offset, len);
    }
    public byte[] digest(byte[] input) {
        return digest.digest(input);
    }
    public String digestToHexString() {
        byte[] digest = digest();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < getDigestLength(); i++) {
            result.append(hexDigitsFor(digest[i]));
        }
        return result.toString();
    }
    public void reset() {
        digest.reset();
    }
    public int getDigestLength() {
        return digest.getDigestLength();
    }
    @Override
    public Object clone() throws CloneNotSupportedException {
        return digest.clone();
    }
}
