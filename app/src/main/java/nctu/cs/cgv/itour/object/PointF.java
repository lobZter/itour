package nctu.cs.cgv.itour.object;

import android.widget.ImageView;

/**
 * Created by lobst3rd on 2017/7/7.
 */

public class PointF {

    public float x;
    public float y;
    public ImageView nodeImage;

    public PointF() {
        this.x = 0;
        this.y = 0;
        this.nodeImage = null;
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
        this.nodeImage = null;
    }
}
