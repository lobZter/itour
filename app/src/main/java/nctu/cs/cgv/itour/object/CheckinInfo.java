package nctu.cs.cgv.itour.object;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by lobZter on 2017/7/4.
 */

public class CheckinInfo {

//    public ImageView icon;
    public String lat;
    public String lng;
    public String location;
    public String description;
    public String filename;
    public String type;

    public CheckinInfo() {
//        this.icon = new ImageView(context);
        this.lat = "0";
        this.lng = "0";
        this.location = "title";
        this.description = "...";
        this.filename = "";
        this.type = "";
    }

    public CheckinInfo(String lat, String lng, String location, String description, String filename, String type) {
        this.lat = lat;
        this.lng = lng;
        this.location = location;
        this.description = description;
        this.filename = filename;
        this.type = type;
    }
}
