package nctu.cs.cgv.itour.object;

import android.widget.ImageView;

/**
 * Created by lobst3rd on 2017/7/7.
 */

public class ImageNode {

    public float x;
    public float y;
    public ImageView icon;

    public ImageNode() {
        this.x = 0;
        this.y = 0;
        this.icon = null;
    }

    public ImageNode(float x, float y) {
        this.x = x;
        this.y = y;
        this.icon = null;
    }
}
