package nctu.cs.cgv.itour.object;

import android.view.View;

/**
 * Created by lobst3rd on 2017/7/7.
 */

public class ImageNode extends Node {

    public View icon;

    public ImageNode(float x, float y) {
        super(x, y);
        this.icon = null;
    }

    public ImageNode(float x, float y, View icon) {
        super(x, y);
        this.icon = icon;
    }
}
