package nctu.cs.cgv.itour.service;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by lobst3rd on 2017/6/20.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = "MyFirebaseMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String postId = remoteMessage.getData().get("postId");
            String lat = remoteMessage.getData().get("lat");
            String lng = remoteMessage.getData().get("lng");
//            sendMessage(postId, lat, lng);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void sendMessage(String postId, String lat, String lng) {
        // send message to activities by broadcasting
        Intent intent = new Intent("checkinIcon");
        intent.putExtra("postId", postId);
        intent.putExtra("lat", Float.valueOf(lat));
        intent.putExtra("lng", Float.valueOf(lng));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
