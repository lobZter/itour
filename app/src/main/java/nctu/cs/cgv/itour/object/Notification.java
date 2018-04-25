package nctu.cs.cgv.itour.object;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lobZter on 2017/12/25.
 */

public class Notification {

    public String postId;
    public String uid;
    public String targetUid;
    public String title;
    public String msg;
    public String photo;
    public String location;
    public String lat;
    public String lng;
    public long timestamp;

    public Notification() {
    }

    public Notification(String postId,
                        String uid,
                        String targetUid,
                        String title,
                        String msg,
                        String photo,
                        String location,
                        String lat,
                        String lng,
                        long timestamp) {
        this.postId = postId;
        this.uid = uid;
        this.targetUid = targetUid;
        this.title = title;
        this.msg = msg;
        this.photo = photo;
        this.location = location;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("postId", postId);
        result.put("uid", uid);
        result.put("targetUid", targetUid);
        result.put("title", title);
        result.put("msg", msg);
        result.put("photo", photo);
        result.put("location", location);
        result.put("lat", lat);
        result.put("lng", lng);
        result.put("timestamp", timestamp);
        return result;
    }
}
