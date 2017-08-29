package nctu.cs.cgv.itour.object;

import android.content.Context;
import android.widget.ImageView;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lobZter on 2017/7/4.
 */

public class Checkin {

    public String lat;
    public String lng;
    public String location;
    public String description;
    public String filename;
    public String type;
    public String uid;
    public String username;
    public String key;

    public Checkin() {
    }

    public Checkin(String lat, String lng, String location, String description, String filename, String type, String uid, String username) {
        this.lat = lat;
        this.lng = lng;
        this.location = location;
        this.description = description;
        this.filename = filename;
        this.type = type;
        this.uid = uid;
        this.username = username;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("lat", lat);
        result.put("lng", lng);
        result.put("location", location);
        result.put("description", description);
        result.put("filename", filename);
        result.put("type", type);
        result.put("uid", uid);
        result.put("username", username);

        return result;
    }
}