package nctu.cs.cgv.itour.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.Utility;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.activity.MainActivity.CHECKIN_NOTIFICATION_REQUEST;

public class CheckinNotificationService extends Service {
    private static final String TAG = "CheckinNotificationService";
    private NotificationManager notificationManager;
    private int CUSTOM_ID = 666;
    private long currentTimestamp;
    private BroadcastReceiver messageReceiver;
    private float currentLat = 0.0f;
    private float currentLng = 0.0f;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("notification").child(mapTag);

        currentTimestamp = System.currentTimeMillis() / 1000;

        query.orderByChild("timestamp").startAt(currentTimestamp).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                nctu.cs.cgv.itour.object.Notification notification = dataSnapshot.getValue(nctu.cs.cgv.itour.object.Notification.class);
                if (notification == null) return;
                if (notification.targetUid.equals("all") ||
                        (FirebaseAuth.getInstance().getCurrentUser() != null && notification.targetUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())))
                    checkDistance(notification);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return START_STICKY;
    }

    private void checkDistance(nctu.cs.cgv.itour.object.Notification notification) {
        float dist = Utility.gpsToMeter(currentLat, currentLng, Float.valueOf(notification.lat), Float.valueOf(notification.lng));
        if (dist <= 100f) {
            notification.title += "在你周圍打卡";
            notifyCheckin(notification);
        } else if (300f <= dist && dist <= 600f) {
            notification.title += "在" + notification.location + "打卡了";
            notifyCheckin(notification);
        }
    }

    private void notifyCheckin(nctu.cs.cgv.itour.object.Notification notification) {

        Bitmap icon;
        final File externalCacheDir = getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + notification.photo).exists()) {
            // load photo from storage
            icon = BitmapFactory.decodeFile(externalCacheDir.toString() + "/" + notification.photo);
        } else {
        icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra("checkinNotificationIntent", true);
        notificationIntent.putExtra("lat", notification.lat);
        notificationIntent.putExtra("lng", notification.lng);
        notificationIntent.putExtra("key", notification.postId);
        notificationIntent.putExtra("title", notification.title);
        notificationIntent.putExtra("msg", notification.msg);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), CHECKIN_NOTIFICATION_REQUEST, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setLargeIcon(icon);
        notificationBuilder.setVibrate(new long[] {0, 300, 300, 300, 300});
        notificationBuilder.setContentTitle(notification.title);
        notificationBuilder.setContentText(notification.msg);
        notificationBuilder.setContentIntent(intent);

        Notification builtNotification = notificationBuilder.build();
        builtNotification.flags |= Notification.FLAG_AUTO_CANCEL;

//        notificationManager.cancelAll();
        notificationManager.notify((int)(System.currentTimeMillis() / 1000), builtNotification);
    }

    private void setBroadcastReceiver() {
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "gpsUpdate":
                        currentLat = intent.getFloatExtra("lat", 0);
                        currentLng = intent.getFloatExtra("lng", 0);
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("gpsUpdate");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }
}
