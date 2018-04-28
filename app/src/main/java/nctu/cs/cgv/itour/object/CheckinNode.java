package nctu.cs.cgv.itour.object;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lobZter on 2018/1/20.
 */

public class CheckinNode extends ImageNode{

    public float lat;
    public float lng;
    public boolean onSpot;
    public List<Checkin> checkinList;

    public CheckinNode(float x, float y, float lat, float lng) {
        super(x, y);
        this.lat = lat;
        this.lng = lng;
        this.onSpot = false;
        this.checkinList = new ArrayList<>();
    }

    public CheckinNode(float x, float y, float lat, float lng, View icon) {
        super(x, y, icon);
        this.lat = lat;
        this.lng = lng;
        this.onSpot = false;
        this.checkinList = new ArrayList<>();
    }
}
