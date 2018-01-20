package nctu.cs.cgv.itour.object;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lobZter on 2018/1/20.
 */

public class CheckinNode extends ImageNode{

    public boolean onSpot;
    public List<Checkin> checkinList;

    public CheckinNode(float x, float y) {
        super(x, y);
        onSpot = false;
        checkinList = new ArrayList<>();
    }

    public CheckinNode(float x, float y, View icon) {
        super(x, y, icon);
        onSpot = false;
        checkinList = new ArrayList<>();
    }
}
