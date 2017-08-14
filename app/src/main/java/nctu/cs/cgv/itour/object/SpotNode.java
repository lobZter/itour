package nctu.cs.cgv.itour.object;

import android.view.View;

import java.util.ArrayList;

/**
 * Created by lobst3rd on 2017/8/4.
 */

public class SpotNode {

    public float x;
    public float y;
    public String name;
    public View icon;
    public View checkin;
    public ArrayList<ImageNode> checkinList;

    public SpotNode() {
        this.x = 0;
        this.y = 0;
        this.name = "";
        this.icon = null;
        this.checkin = null;
        this.checkinList = new ArrayList<>();
    }

    public SpotNode(float x, float y) {
        this.x = x;
        this.y = y;
        this.name = "";
        this.icon = null;
        this.checkin = null;
        this.checkinList = new ArrayList<>();
    }

    public SpotNode(float x, float y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.icon = null;
        this.checkin = null;
        this.checkinList = new ArrayList<>();
    }
}
