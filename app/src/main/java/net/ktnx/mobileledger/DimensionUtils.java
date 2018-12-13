package net.ktnx.mobileledger;

import android.content.Context;
import android.util.TypedValue;

public class DimensionUtils {
    public static int dp2px(Context context, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
               context.getResources().getDisplayMetrics()));
    }

}
