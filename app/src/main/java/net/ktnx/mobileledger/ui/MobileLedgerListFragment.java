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

package net.ktnx.mobileledger.ui;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListAdapter;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.DimensionUtils;

public class MobileLedgerListFragment extends Fragment {
    public SwipeRefreshLayout refreshLayout;
    public TransactionListAdapter modelAdapter;
    protected RecyclerView root;
    @NonNull
    public MainActivity getMainActivity() {
        return (MainActivity) requireActivity();
    }
    protected void themeChanged(Integer counter) {
        refreshLayout.setColorSchemeColors(Colors.getSwipeCircleColors());
    }
    public void onBackgroundTaskRunningChanged(Boolean isRunning) {
        if (getActivity() == null)
            return;
        if (refreshLayout == null)
            return;
        refreshLayout.setRefreshing(isRunning);
    }
    protected void manageFabOnScroll() {
        final MainActivity mainActivity = getMainActivity();
        int triggerPixels = DimensionUtils.dp2px(mainActivity, 30f);
        root.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            private float upAnchor = -1;
            private float downAnchor = -1;
            private float lastY;
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = upAnchor = downAnchor = e.getAxisValue(MotionEvent.AXIS_Y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        final float currentY = e.getAxisValue(MotionEvent.AXIS_Y);
                        if (currentY > lastY) {
                            // swipe down
                            upAnchor = lastY;

                            mainActivity.fabShouldShow();
                        }
                        else {
                            // swipe up
                            downAnchor = lastY;

                            if (currentY < upAnchor - triggerPixels)
                                mainActivity.fabHide();
                        }

                        lastY = currentY;

                        break;
                }
                return false;
            }
            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }
            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });
    }
}
