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

package net.ktnx.mobileledger.model;

import net.ktnx.mobileledger.db.TemplateHeader;

import java.util.regex.MatchResult;

public class MatchedTemplate {
    public TemplateHeader templateHead;
    public MatchResult matchResult;
    public MatchedTemplate(TemplateHeader templateHead, MatchResult matchResult) {
        this.templateHead = templateHead;
        this.matchResult = matchResult;
    }
}
