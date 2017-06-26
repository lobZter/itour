package nctu.cs.cgv.itour.config;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by lobZter on 2017/6/21.
 */

public class Utility {
    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }
}
