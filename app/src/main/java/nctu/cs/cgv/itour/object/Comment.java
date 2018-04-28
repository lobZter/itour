package nctu.cs.cgv.itour.object;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Comment {
    public String msg;
    public String uid;
    public String username;
    public long timestamp;

    public Comment() {
    }

    public
    Comment(String msg, String uid, String username, long timestamp) {
        this.msg = msg;
        this.uid = uid;
        this.username = username;
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("msg", msg);
        result.put("uid", uid);
        result.put("username", username);
        result.put("timestamp", timestamp);
        return result;
    }
}
