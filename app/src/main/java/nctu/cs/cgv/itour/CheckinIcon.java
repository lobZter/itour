package nctu.cs.cgv.itour;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by lobZter on 2017/7/4.
 */

public class CheckinIcon {
    public ImageView icon;
    public double latitude;
    public double longitude;
    public String path;
    public String type;

    public CheckinIcon(Context context) {
        this.icon = new ImageView(context);
        this.latitude = 0;
        this.longitude = 0;
        this.path = null;
        this.type = null;
    }

    public CheckinIcon(Context context, double latitude, double longitude, String type, String path) {
        this.icon = new ImageView(context);
        this.latitude = latitude;
        this.longitude = longitude;
        this.path = path;
        this.type = type;
    }
}
