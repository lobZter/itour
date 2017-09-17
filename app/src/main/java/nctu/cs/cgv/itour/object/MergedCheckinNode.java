package nctu.cs.cgv.itour.object;

import android.view.View;

import java.util.ArrayList;

/**
 * Created by lobst3rd on 2017/8/15.
 */

public class MergedCheckinNode extends ImageNode{

    public int checkinNum;
    public boolean onSpot;

    public MergedCheckinNode(float x, float y) {
        super(x, y);
        checkinNum = 1;
        onSpot = false;
    }

}
