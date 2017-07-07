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
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String lat = remoteMessage.getData().get("lat");
            String lng = remoteMessage.getData().get("lng");
            Log.d(TAG, lat + ", " + lng);
            sendMessage(lat, lng);
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

    }

    private void sendMessage(String lat, String lng) {
        Intent intent = new Intent("my-event");
        // add data
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
