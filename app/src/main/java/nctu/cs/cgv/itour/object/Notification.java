package nctu.cs.cgv.itour.object;

/**
 * Created by lobZter on 2017/12/25.
 */

public class Notification {

    public String postId;
    public String targetUid;
    public String title;
    public String msg;
    public String lng;
    public String lat;
    public String timestamp;

    public Notification () {
    }

    public Notification (String postId,
                         String targetUid,
                         String title,
                         String msg,
                         String lng,
                         String lat,
                         String timestamp) {
        this.postId = postId;
        this.targetUid = targetUid;
        this.title = title;
        this.msg = msg;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

}
