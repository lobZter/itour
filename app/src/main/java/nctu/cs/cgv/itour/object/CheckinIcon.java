package nctu.cs.cgv.itour.object;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by lobZter on 2017/7/4.
 */

public class CheckinIcon {
    public ImageView icon;
    public float lat;
    public float lng;
    public String location;
    public String description;
    public String filename;
    public String type;

    public CheckinIcon() {
//        this.icon = new ImageView(context);
        this.lat = 0;
        this.lng = 0;
        this.location = "title";
        this.description = "...";
        this.filename = "";
        this.type = "";
    }

    public CheckinIcon(float lat, float lng, String location, String description, String filename, String type) {
        this.lat = lat;
        this.lng = lng;
        this.location = location;
        this.description = description;
        this.filename = filename;
        this.type = type;
    }
}
