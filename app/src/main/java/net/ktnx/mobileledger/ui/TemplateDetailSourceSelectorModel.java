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

package net.ktnx.mobileledger.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.model.TemplateDetailSource;

import java.util.ArrayList;
import java.util.List;

public class TemplateDetailSourceSelectorModel extends ViewModel {
    public final MutableLiveData<List<TemplateDetailSource>> groups = new MutableLiveData<>();
    private OnSourceSelectedListener selectionListener;
    public TemplateDetailSourceSelectorModel() {
    }
    void setOnSourceSelectedListener(OnSourceSelectedListener listener) {
        selectionListener = listener;
    }
    void resetOnSourceSelectedListener() {
        selectionListener = null;
    }
    void triggerOnSourceSelectedListener(boolean literal, short group) {
        if (selectionListener != null)
            selectionListener.onSourceSelected(literal, group);
    }
    public void setSourcesList(ArrayList<TemplateDetailSource> mSources) {
        groups.setValue(mSources);
    }
}
