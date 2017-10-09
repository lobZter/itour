package nctu.cs.cgv.itour.object;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lobZter on 2017/7/4.
 */

public class Checkin {

    public String lng;
    public String lat;
    public String location;
    public String description;
    public String photo;
    public String audio;
    public Map<String, Boolean> type;
    public String uid;
    public String username;
    public String timestamp;

    public String key;
    public Map<String, Boolean> like = new HashMap<>();

    public Checkin() {
    }

    public Checkin(String lng,
                   String lat,
                   String location,
                   String description,
                   String photo,
                   String audio,
                   Map<String, Boolean> type,
                   String uid,
                   String username,
                   String timestamp) {

        this.lng = lng;
        this.lat = lat;
        this.location = location;
        this.description = description;
        this.photo = photo;
        this.audio = audio;
        this.type = type;
        this.uid = uid;
        this.username = username;
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("lng", lng);
        result.put("lat", lat);
        result.put("location", location);
        result.put("description", description);
        result.put("photo", photo);
        result.put("audio", audio);
        result.put("type", type);
        result.put("uid", uid);
        result.put("username", username);
        result.put("timestamp", timestamp);

        return result;
    }
}
