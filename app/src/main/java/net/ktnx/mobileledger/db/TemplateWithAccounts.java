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

package net.ktnx.mobileledger.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

public class TemplateWithAccounts {
    @Embedded
    public TemplateHeader header;
    @Relation(parentColumn = "id", entityColumn = "template_id")
    public List<TemplateAccount> accounts;

    public static TemplateWithAccounts from(TemplateWithAccounts o) {
        TemplateWithAccounts result = new TemplateWithAccounts();
        result.header = new TemplateHeader(o.header);
        result.accounts = new ArrayList<>();
        for (TemplateAccount acc : o.accounts) {
            result.accounts.add(new TemplateAccount(acc));
        }

        return result;
    }
    public Long getId() {
        return header.getId();
    }
    public TemplateWithAccounts createDuplicate() {
        TemplateWithAccounts result = new TemplateWithAccounts();
        result.header = header.createDuplicate();
        result.accounts = new ArrayList<>();
        for (TemplateAccount acc : accounts) {
            result.accounts.add(acc.createDuplicate(result.header));
        }

        return result;
    }
}
