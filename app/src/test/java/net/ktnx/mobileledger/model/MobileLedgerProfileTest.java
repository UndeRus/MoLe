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

package net.ktnx.mobileledger.model;

import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class MobileLedgerProfileTest {
    private List<LedgerAccount> listFromArray(LedgerAccount[] array) {
        ArrayList<LedgerAccount> result = new ArrayList<>();
        Collections.addAll(result, array);

        return result;
    }
    private void aTest(LedgerAccount[] oldList, LedgerAccount[] newList,
                       LedgerAccount[] expectedResult) {
        List<LedgerAccount> result = MobileLedgerProfile.mergeAccountLists(listFromArray(oldList),
                listFromArray(newList));
        assertArrayEquals(expectedResult, result.toArray());
    }
    private void negTest(LedgerAccount[] oldList, LedgerAccount[] newList,
                         LedgerAccount[] expectedResult) {
        List<LedgerAccount> result = MobileLedgerProfile.mergeAccountLists(listFromArray(oldList),
                listFromArray(newList));
        assertThrows(ArrayComparisonFailure.class,
                () -> assertArrayEquals(expectedResult, result.toArray()));
    }
    private LedgerAccount[] emptyArray() {
        return new LedgerAccount[]{};
    }
    @Test
    public void mergeEmptyLists() {
        aTest(emptyArray(), emptyArray(), emptyArray());
    }
    @Test
    public void mergeIntoEmptyLists() {
        LedgerAccount acc1 = new LedgerAccount(null, "Acc1", null);
        aTest(emptyArray(), new LedgerAccount[]{acc1}, new LedgerAccount[]{acc1});
    }
    @Test
    public void mergeEmptyList() {
        LedgerAccount acc1 = new LedgerAccount(null, "Acc1", null);
        aTest(new LedgerAccount[]{acc1}, emptyArray(), emptyArray());
    }
    @Test
    public void mergeEqualLists() {
        LedgerAccount acc1 = new LedgerAccount(null, "Acc1", null);
        aTest(new LedgerAccount[]{acc1}, new LedgerAccount[]{acc1}, new LedgerAccount[]{acc1});
    }
    @Test
    public void mergeFlags() {
        LedgerAccount acc1a = new LedgerAccount(null, "Acc1", null);
        LedgerAccount acc1b = new LedgerAccount(null, "Acc1", null);
        acc1b.setExpanded(true);
        acc1b.setAmountsExpanded(true);
        List<LedgerAccount> merged =
                MobileLedgerProfile.mergeAccountLists(listFromArray(new LedgerAccount[]{acc1a}),
                        listFromArray(new LedgerAccount[]{acc1b}));
        assertArrayEquals(new LedgerAccount[]{acc1b}, merged.toArray());
        assertSame(merged.get(0), acc1a);
        // restore original values, modified by the merge
        acc1a.setExpanded(false);
        acc1a.setAmountsExpanded(false);
        negTest(new LedgerAccount[]{acc1a}, new LedgerAccount[]{acc1b},
                new LedgerAccount[]{new LedgerAccount(null, "Acc1", null)});
    }
}