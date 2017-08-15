package nctu.cs.cgv.itour.object;

import android.view.View;

import java.util.ArrayList;

/**
 * Created by lobst3rd on 2017/8/15.
 */

public class MergedCheckinNode {

    public float x;
    public float y;
    public View icon;
    public ArrayList<ImageNode> checkinList;

    public MergedCheckinNode() {
        this.x = 0;
        this.y = 0;
        this.icon = null;
        this.checkinList = new ArrayList<>();
    }

    public MergedCheckinNode(float x, float y) {
        this.x = x;
        this.y = y;
        this.icon = null;
        this.checkinList = new ArrayList<>();
    }

}
